package com.vdt.finq.modules.rag.retrieval;

import static com.vdt.finq.modules.rag.preprocessing.indexing.vectordb.QdrantConstants.ELEMENT_TYPE;
import static com.vdt.finq.modules.rag.preprocessing.indexing.vectordb.QdrantConstants.KY;
import static com.vdt.finq.modules.rag.preprocessing.indexing.vectordb.QdrantConstants.LOAI;
import static com.vdt.finq.modules.rag.preprocessing.indexing.vectordb.QdrantConstants.NAM;
import static com.vdt.finq.modules.rag.preprocessing.indexing.vectordb.QdrantConstants.NOI_DUNG;
import static com.vdt.finq.modules.rag.preprocessing.indexing.vectordb.QdrantConstants.PHAM_VI;
import static com.vdt.finq.modules.rag.preprocessing.indexing.vectordb.QdrantConstants.PHAN;
import static com.vdt.finq.modules.rag.preprocessing.indexing.vectordb.QdrantConstants.TAI_LIEU_ID;
import static com.vdt.finq.modules.rag.preprocessing.indexing.vectordb.QdrantConstants.TRANG;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import org.springframework.stereotype.Component;

import com.vdt.finq.common.exception.AppException;
import com.vdt.finq.common.properties.QdrantProperties;

import io.qdrant.client.QdrantClient;
import io.qdrant.client.WithPayloadSelectorFactory;
import io.qdrant.client.grpc.JsonWithInt.Value;
import io.qdrant.client.grpc.Points.Filter;
import io.qdrant.client.grpc.Points.ScoredPoint;
import io.qdrant.client.grpc.Points.SearchParams;
import io.qdrant.client.grpc.Points.SearchPoints;

import lombok.extern.slf4j.Slf4j;

// tìm chunk gần nhất trong Qdrant; lọc chạy ngay lúc tìm nên vẫn đủ limit kết quả
@Slf4j
@Component
public class QdrantRetriever {

    // lỗi/quá giờ (máy thiếu RAM) thì thử lại một lần, để agent không nhận lỗi rồi tự đổi câu tìm (B07, 13/10)
    private static final int ATTEMPTS = 2;

    private final QdrantClient client;
    private final String collection;

    public QdrantRetriever(QdrantClient client, QdrantProperties properties) {
        this.client = client;
        this.collection = properties.getCollection();
    }

    // filter null = không lọc
    public List<RetrievedChunk> search(float[] vector, Filter filter, int limit) {
        List<Float> values = new ArrayList<>(vector.length);
        for (float v : vector) {
            values.add(v);
        }
        SearchPoints.Builder request = SearchPoints.newBuilder()
                .setCollectionName(collection)
                .addAllVector(values)
                .setLimit(limit)
                .setWithPayload(WithPayloadSelectorFactory.enable(true))
                // tìm chính xác: kho ~7 nghìn chunk nên nhanh ngang HNSW (8–14 ms) và ổn định giữa các lần chạy;
                // HNSW xấp xỉ có thể đổi tập ứng viên sau khi Qdrant khởi động lại / tối ưu segment
                .setParams(SearchParams.newBuilder().setExact(true).build());
        if (filter != null) {
            request.setFilter(filter);
        }
        for (int attempt = 1;; attempt++) {
            try {
                return client.searchAsync(request.build()).get().stream().map(QdrantRetriever::toChunk).toList();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new AppException(503, "tìm Qdrant bị ngắt");
            } catch (ExecutionException e) {
                if (attempt >= ATTEMPTS) {
                    throw new AppException(503, "không tìm được trên Qdrant: " + e.getCause().getMessage());
                }
                log.warn("tìm Qdrant lỗi lần {}: {}, thử lại", attempt, e.getCause().getMessage());
            }
        }
    }

    private static RetrievedChunk toChunk(ScoredPoint point) {
        Map<String, Value> p = point.getPayloadMap();
        return new RetrievedChunk(
                point.getId().getUuid(),
                text(p, TAI_LIEU_ID),
                (int) p.get(TRANG).getIntegerValue(),
                (int) p.get(PHAN).getIntegerValue(),
                p.containsKey(NAM) ? (int) p.get(NAM).getIntegerValue() : null,
                text(p, KY),
                text(p, PHAM_VI),
                text(p, LOAI),
                text(p, ELEMENT_TYPE),
                text(p, NOI_DUNG),
                point.getScore());
    }

    private static String text(Map<String, Value> payload, String key) {
        Value v = payload.get(key);
        return v == null ? null : v.getStringValue();
    }
}
