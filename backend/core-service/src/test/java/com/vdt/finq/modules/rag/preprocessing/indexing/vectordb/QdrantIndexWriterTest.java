package com.vdt.finq.modules.rag.preprocessing.indexing.vectordb;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.vdt.finq.common.properties.EmbeddingProperties;
import com.vdt.finq.common.properties.QdrantProperties;
import com.vdt.finq.config.QdrantConfig;
import com.vdt.finq.modules.rag.preprocessing.indexing.chunker.Chunk;
import com.vdt.finq.modules.rag.preprocessing.indexing.embedder.EmbeddedChunk;
import com.vdt.finq.modules.rag.preprocessing.indexing.vectordb.QdrantEnums.ElementType;
import com.vdt.finq.modules.document.DocumentEnums.Ky;
import com.vdt.finq.modules.document.DocumentEnums.LoaiTaiLieu;
import com.vdt.finq.modules.document.DocumentEnums.PhamVi;

import io.qdrant.client.QdrantClient;

// chạy trên Qdrant thật (docker), collection tạm, vector giả 8 chiều: không gọi API embedding
class QdrantIndexWriterTest {

    private static final int DIMS = 8;

    private QdrantClient client;
    private QdrantProperties qdrantProperties;
    private QdrantIndexWriter writer;

    @BeforeEach
    void setUp() {
        assumeTrue(qdrantRunning(), "Qdrant chưa chạy ở localhost:6334");
        qdrantProperties = new QdrantProperties();
        qdrantProperties.setCollection("finq_test_" + UUID.randomUUID().toString().substring(0, 8));
        EmbeddingProperties embeddingProperties = new EmbeddingProperties();
        embeddingProperties.setDimensions(DIMS);
        client = new QdrantConfig().qdrantClient(qdrantProperties);
        writer = new QdrantIndexWriter(client, qdrantProperties, embeddingProperties);
    }

    @AfterEach
    void tearDown() throws Exception {
        if (client != null) {
            // test thuần (không gọi ensureCollection) thì không có collection để xoá
            if (client.collectionExistsAsync(qdrantProperties.getCollection()).get()) {
                client.deleteCollectionAsync(qdrantProperties.getCollection()).get();
            }
            client.close();
        }
    }

    // tạo collection và đủ 8 payload index; gọi lại lần nữa không lỗi
    @Test
    void ensureCollectionCreatesIndexes() throws Exception {
        writer.ensureCollection();
        writer.ensureCollection();

        var info = client.getCollectionInfoAsync(qdrantProperties.getCollection()).get();
        assertThat(info.getConfig().getParams().getVectorsConfig().getParams().getSize()).isEqualTo(DIMS);
        assertThat(info.getPayloadSchemaMap().keySet()).containsExactlyInAnyOrder(
                "tai_lieu_id", "ma_cong_ty", "ky", "pham_vi", "loai", "element_type", "nam", "la_ban_chinh");
    }

    // nạp lại cùng tài liệu thì ghi đè (id cố định), xoá theo tài liệu không đụng tài liệu khác
    @Test
    void upsertIsIdempotentAndDeleteIsPerDocument() {
        writer.ensureCollection();
        DocumentMetadata a = new DocumentMetadata("DOC-A", "CTR", LoaiTaiLieu.BCTC, 2023, Ky.Q4, PhamVi.HOP_NHAT, true);
        DocumentMetadata b = new DocumentMetadata("DOC-B", "CTR", LoaiTaiLieu.NGHI_QUYET, 2024, null, null, true);

        assertThat(writer.upsert(a, chunks("DOC-A", 3))).isEqualTo(3);
        writer.upsert(b, chunks("DOC-B", 2));
        // nạp lại tài liệu A: vẫn 3 point chứ không phải 6
        writer.upsert(a, chunks("DOC-A", 3));

        assertThat(writer.countDocument("DOC-A")).isEqualTo(3);
        assertThat(writer.countDocument("DOC-B")).isEqualTo(2);

        writer.deleteDocument("DOC-A");
        assertThat(writer.countDocument("DOC-A")).isZero();
        assertThat(writer.countDocument("DOC-B")).isEqualTo(2);
    }

    // vector sai số chiều bị chặn trước khi gửi
    @Test
    void rejectsVectorOfWrongSize() {
        writer.ensureCollection();
        DocumentMetadata a = new DocumentMetadata("DOC-A", "CTR", LoaiTaiLieu.BCTC, 2023, Ky.Q4, PhamVi.HOP_NHAT, true);
        EmbeddedChunk wrong = new EmbeddedChunk(new Chunk("DOC-A", 1, 0, "x"), "x", new float[DIMS + 1]);
        assertThatThrownBy(() -> writer.upsert(a, List.of(wrong))).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void elementTypeAndPointId() {
        assertThat(QdrantIndexWriter.elementType("Doanh thu tăng.")).isEqualTo(ElementType.TEXT);
        assertThat(QdrantIndexWriter.elementType("| a | b |\n|---|---|")).isEqualTo(ElementType.TABLE);
        assertThat(QdrantIndexWriter.elementType("Bảng 1\n\n| a | b |")).isEqualTo(ElementType.MIXED);
        assertThat(QdrantIndexWriter.pointId("DOC-A", 5, 0)).isEqualTo(QdrantIndexWriter.pointId("DOC-A", 5, 0));
        assertThat(QdrantIndexWriter.pointId("DOC-A", 5, 0)).isNotEqualTo(QdrantIndexWriter.pointId("DOC-A", 5, 1));
    }

    // vector giả đã chuẩn hoá, mỗi chunk một hướng khác nhau
    private static List<EmbeddedChunk> chunks(String documentId, int count) {
        List<EmbeddedChunk> list = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            float[] v = new float[DIMS];
            v[i % DIMS] = 1f;
            Chunk c = new Chunk(documentId, i + 1, 0, "| Chỉ tiêu | Năm nay |\n|---|---|\n| Doanh thu " + i + " | 100 |");
            list.add(new EmbeddedChunk(c, c.getText(), v));
        }
        return list;
    }

    private static boolean qdrantRunning() {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress("localhost", 6334), 1000);
            return true;
        } catch (IOException e) {
            return false;
        }
    }
}
 