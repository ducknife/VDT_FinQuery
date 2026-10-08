package com.vdt.finq.modules.hierarchicalrag.preprocessing.indexing.embedder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import com.vdt.finq.common.properties.EmbeddingProperties;
import com.vdt.finq.modules.hierarchicalrag.preprocessing.indexing.chunker.Chunk;

import lombok.RequiredArgsConstructor;

// biến chunk thành vector: gắn tên tài liệu, chia lô, gọi EmbeddingClient
//
// nguyên tắc (logs/lap-chi-muc_v1.7.md, mục 3.5): chữ có nghĩa thì đem embed,
// toạ độ (tai_lieu_id, trang, phan) để trong payload của Qdrant, không đem embed
@Component
@RequiredArgsConstructor
public class Embedding {

    // Qwen3-Embedding là model bất đối xứng: câu hỏi phải có tiền tố mô tả nhiệm vụ,
    // tài liệu thì embed nguyên văn
    private static final String QUERY_INSTRUCTION =
            "Given a question about a Vietnamese company's financial reports, "
            + "retrieve the report pages that answer it";

    private final EmbeddingClient embeddingClient;
    private final EmbeddingProperties properties;

    // embed các chunk của một tài liệu; documentTitle là documents.ten_hien_thi
    public List<EmbeddedChunk> embedChunks(List<Chunk> chunks, String documentTitle)
            throws IOException, InterruptedException {
        List<EmbeddedChunk> result = new ArrayList<>(chunks.size());
        int batchSize = properties.getBatchSize();

        for (int start = 0; start < chunks.size(); start += batchSize) {
            List<Chunk> batch = chunks.subList(start, Math.min(chunks.size(), start + batchSize));
            List<String> texts = batch.stream().map(c -> embedText(documentTitle, c)).toList();
            List<float[]> vectors = embeddingClient.embed(texts);
            for (int i = 0; i < batch.size(); i++) {
                result.add(new EmbeddedChunk(batch.get(i), texts.get(i), vectors.get(i)));
            }
        }
        return result;
    }

    // embed một câu hỏi của người dùng, dùng ở phía truy hồi
    public float[] embedQuery(String question) throws IOException, InterruptedException {
        String text = "Instruct: " + QUERY_INSTRUCTION + "\nQuery: " + question;
        return embeddingClient.embed(List.of(text)).get(0);
    }

    // văn bản đem embed = [tên tài liệu] + nội dung chunk (contextual header, không có số trang)
    public static String embedText(String documentTitle, Chunk chunk) {
        if (documentTitle == null || documentTitle.isBlank()) {
            return chunk.getText();
        }
        return "[" + documentTitle + "]\n" + chunk.getText();
    }
}
