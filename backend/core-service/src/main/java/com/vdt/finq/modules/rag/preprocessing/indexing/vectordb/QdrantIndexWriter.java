package com.vdt.finq.modules.rag.preprocessing.indexing.vectordb;

import static io.qdrant.client.ConditionFactory.matchKeyword;
import static io.qdrant.client.PointIdFactory.id;
import static io.qdrant.client.ValueFactory.value;
import static io.qdrant.client.VectorsFactory.vectors;
import static com.vdt.finq.modules.rag.preprocessing.indexing.vectordb.QdrantConstants.*;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import org.springframework.stereotype.Component;

import com.google.common.util.concurrent.ListenableFuture;
import com.vdt.finq.common.properties.EmbeddingProperties;
import com.vdt.finq.common.properties.QdrantProperties;
import com.vdt.finq.modules.rag.preprocessing.indexing.chunker.Chunk;
import com.vdt.finq.modules.rag.preprocessing.indexing.embedder.EmbeddedChunk;
import com.vdt.finq.modules.rag.preprocessing.indexing.vectordb.QdrantEnums.ElementType;

import io.qdrant.client.QdrantClient;
import io.qdrant.client.grpc.Collections.CollectionInfo;
import io.qdrant.client.grpc.Collections.Distance;
import io.qdrant.client.grpc.Collections.PayloadSchemaType;
import io.qdrant.client.grpc.Collections.VectorParams;
import io.qdrant.client.grpc.JsonWithInt.Value;
import io.qdrant.client.grpc.Points.DeletePoints;
import io.qdrant.client.grpc.Points.Filter;
import io.qdrant.client.grpc.Points.PointStruct;
import io.qdrant.client.grpc.Points.PointsSelector;
import io.qdrant.client.grpc.Points.UpsertPoints;
import lombok.extern.slf4j.Slf4j;

// ghi chunk đã embed vào Qdrant: collection, payload index, xoá và upsert point.
// vector để tìm theo nghĩa, payload để lọc và trích dẫn
@Slf4j
@Component
public class QdrantIndexWriter {

    // các trường dùng để lọc thì phải có payload index, Qdrant không tự tạo
    private static final Map<String, PayloadSchemaType> INDEXED_FIELDS = Map.of(
            TAI_LIEU_ID, PayloadSchemaType.Keyword,
            MA_CONG_TY, PayloadSchemaType.Keyword,
            KY, PayloadSchemaType.Keyword,
            PHAM_VI, PayloadSchemaType.Keyword,
            LOAI, PayloadSchemaType.Keyword,
            ELEMENT_TYPE, PayloadSchemaType.Keyword,
            NAM, PayloadSchemaType.Integer,
            LA_BAN_CHINH, PayloadSchemaType.Bool);

    private final QdrantClient client;
    private final String collection;
    private final int dimensions;
    private final int upsertBatchSize;

    public QdrantIndexWriter(QdrantClient client, QdrantProperties qdrantProperties,
            EmbeddingProperties embeddingProperties) {
        this.client = client;
        this.collection = qdrantProperties.getCollection();
        this.dimensions = embeddingProperties.getDimensions();
        this.upsertBatchSize = qdrantProperties.getUpsertBatchSize();
    }

    // tạo collection và payload index nếu chưa có; gọi trước lần nạp đầu tiên, gọi lại vô hại
    public void ensureCollection() {
        if (!await(client.collectionExistsAsync(collection))) {
            VectorParams params = VectorParams.newBuilder()
                    .setSize(dimensions)
                    // vector của model đã chuẩn hoá, cosine là phép đo chuẩn
                    .setDistance(Distance.Cosine)
                    .build();
            await(client.createCollectionAsync(collection, params));
            log.info("đã tạo collection {} ({} chiều, Cosine)", collection, dimensions);
        }

        CollectionInfo info = await(client.getCollectionInfoAsync(collection));
        // số chiều của collection phải khớp model đang dùng, lệch là dấu hiệu đổi model mà quên đổi collection
        long size = info.getConfig().getParams().getVectorsConfig().getParams().getSize();
        if (size != dimensions) {
            throw new IllegalStateException("collection " + collection + " có " + size
                    + " chiều nhưng model embedding trả " + dimensions + " chiều; hãy dùng collection khác");
        }

        // tạo những payload index còn thiếu
        Set<String> existing = info.getPayloadSchemaMap().keySet();
        INDEXED_FIELDS.forEach((field, type) -> {
            if (!existing.contains(field)) {
                await(client.createPayloadIndexAsync(collection, field, type, null, true, null, null));
                log.info("đã tạo payload index {} ({})", field, type);
            }
        });
    }

    // xoá mọi point của một tài liệu; gọi trước khi nạp lại để dọn các phần cũ
    public void deleteDocument(String taiLieuId) {
        DeletePoints request = DeletePoints.newBuilder()
                .setCollectionName(collection)
                .setPoints(PointsSelector.newBuilder().setFilter(byDocument(taiLieuId)))
                // chờ Qdrant xoá xong mới trả về
                .setWait(true)
                .build();
        await(client.deleteAsync(request));
    }

    // ghi các chunk của một tài liệu, theo từng lô; trả về số point đã ghi
    public int upsert(DocumentMetadata document, List<EmbeddedChunk> chunks) {
        List<PointStruct> points = new ArrayList<>(chunks.size());
        for (EmbeddedChunk embedded : chunks) {
            points.add(toPoint(document, embedded));
        }
        for (int start = 0; start < points.size(); start += upsertBatchSize) {
            UpsertPoints request = UpsertPoints.newBuilder()
                    .setCollectionName(collection)
                    .addAllPoints(points.subList(start, Math.min(points.size(), start + upsertBatchSize)))
                    // chờ ghi xong, để bước ghi ngay_embed vào MySQL chỉ chạy khi Qdrant đã có dữ liệu
                    .setWait(true)
                    .build();
            await(client.upsertAsync(request));
        }
        return points.size();
    }

    // số point của một tài liệu, dùng để đối chiếu với documents.so_chunk
    public long countDocument(String taiLieuId) {
        return await(client.countAsync(collection, byDocument(taiLieuId), true));
    }

    // một chunk -> một point: id cố định, vector, payload
    private PointStruct toPoint(DocumentMetadata document, EmbeddedChunk embedded) {
        Chunk chunk = embedded.getChunk();
        if (embedded.getVector().length != dimensions) {
            throw new IllegalArgumentException("vector " + embedded.getVector().length
                    + " chiều, collection cần " + dimensions);
        }

        return PointStruct.newBuilder()
                .setId(id(pointId(document.getTaiLieuId(), chunk.getPage(), chunk.getPart())))
                .setVectors(vectors(embedded.getVector()))
                .putAllPayload(payloadOf(document, chunk))
                .build();
    }

    // payload của một point; public để test xem trước đúng thứ sẽ ghi vào Qdrant
    public static Map<String, Value> payloadOf(DocumentMetadata document, Chunk chunk) {
        Map<String, Value> payload = new LinkedHashMap<>();
        payload.put(TAI_LIEU_ID, value(document.getTaiLieuId()));
        payload.put(MA_CONG_TY, value(document.getMaCongTy()));
        payload.put(TRANG, value(chunk.getPage()));
        payload.put(PHAN, value(chunk.getPart()));
        payload.put(NAM, value(document.getNam()));
        payload.put(LOAI, value(document.getLoai().getValue()));
        payload.put(LA_BAN_CHINH, value(document.isLaBanChinh()));
        payload.put(ELEMENT_TYPE, value(elementType(chunk.getText()).getValue()));
        // nội dung gốc, không có tên tài liệu ở đầu, để trích dẫn cho sạch
        payload.put(NOI_DUNG, value(chunk.getText()));
        // trường có thể thiếu thì bỏ hẳn, không ghi null
        if (document.getKy() != null) {
            payload.put(KY, value(document.getKy().getValue()));
        }
        if (document.getPhamVi() != null) {
            payload.put(PHAM_VI, value(document.getPhamVi().getValue()));
        }
        return payload;
    }

    // id cố định từ toạ độ: nạp lại cùng tài liệu thì ghi đè đúng point cũ, không sinh bản trùng
    public static UUID pointId(String taiLieuId, int page, int part) {
        String key = taiLieuId + "|" + page + "|" + part;
        return UUID.nameUUIDFromBytes(key.getBytes(StandardCharsets.UTF_8));
    }

    // dòng bắt đầu bằng | là dòng bảng markdown, còn lại là chữ
    public static ElementType elementType(String text) {
        boolean hasTable = false;
        boolean hasText = false;
        for (String line : text.lines().toList()) {
            if (line.isBlank()) {
                continue;
            }
            if (line.stripLeading().startsWith("|")) {
                hasTable = true;
            } else {
                hasText = true;
            }
        }
        if (!hasTable) {
            return ElementType.TEXT;
        }
        return hasText ? ElementType.MIXED : ElementType.TABLE;
    }

    private static Filter byDocument(String taiLieuId) {
        return Filter.newBuilder().addMust(matchKeyword(TAI_LIEU_ID, taiLieuId)).build();
    }

    // chờ một lời gọi gRPC xong, đổi lỗi thành ngoại lệ không kiểm tra cho gọn
    private static <T> T await(ListenableFuture<T> future) {
        try {
            return future.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("bị ngắt khi chờ Qdrant", e);
        } catch (ExecutionException e) {
            throw new IllegalStateException("Qdrant báo lỗi: " + e.getCause().getMessage(), e.getCause());
        }
    }
}
