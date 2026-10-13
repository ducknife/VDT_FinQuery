package com.vdt.finq.modules.rag.preprocessing.indexing.embedder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import com.vdt.finq.common.properties.EmbeddingProperties;
import com.vdt.finq.common.properties.IndexingProperties;
import com.vdt.finq.common.properties.IndexingProperties.TableFormat;
import com.vdt.finq.modules.rag.preprocessing.indexing.chunker.Chunk;
import com.vdt.finq.modules.rag.preretrieval.normalizer.FinancialTextNormalizer;
import com.vdt.finq.utils.TableToJson;

import lombok.RequiredArgsConstructor;

// biến chunk thành vector: gắn tên tài liệu, chia lô, gọi EmbeddingClient; toạ độ để ở payload, không embed
@Component
@RequiredArgsConstructor
public class Embedding {

    // Qwen3-Embedding bất đối xứng: câu hỏi có tiền tố nhiệm vụ, tài liệu embed nguyên văn
    private static final String QUERY_INSTRUCTION =
            "Given a question about a Vietnamese company's financial reports, "
            + "retrieve the report pages that answer it";

    private final EmbeddingClient embeddingClient;
    private final EmbeddingProperties properties;
    private final IndexingProperties indexingProperties;
    private final TableToJson tableToJson;
    private final FinancialTextNormalizer normalizer;

    // embed các chunk của một tài liệu; documentTitle là documents.ten_hien_thi
    public List<EmbeddedChunk> embedChunks(List<Chunk> chunks, String documentTitle)
            throws IOException, InterruptedException {
        List<EmbeddedChunk> result = new ArrayList<>(chunks.size());
        int batchSize = properties.getBatchSize();

        for (int start = 0; start < chunks.size(); start += batchSize) {
            List<Chunk> batch = chunks.subList(start, Math.min(chunks.size(), start + batchSize));
            List<String> texts = batch.stream().map(c -> embedText(documentTitle, bodyOf(c))).toList();
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

    // phần thân đem embed: chữ của chunk, bảng đổi JSON nếu cấu hình; payload vẫn giữ markdown
    private String bodyOf(Chunk chunk) {
        String body = chunk.getText();
        if (indexingProperties.getTableFormat() == TableFormat.JSON) {
            body = tableToJson.convert(body);
        }
        // cùng bộ chuẩn hoá với câu hỏi, để hai đầu chung một dạng chuẩn
        if (indexingProperties.isCorpusNormalization()) {
            body = normalizer.normalizeCorpus(body);
        }
        return body;
    }

    // văn bản đem embed = [tên tài liệu] + nội dung chunk (contextual header, không có số trang)
    public static String embedText(String documentTitle, Chunk chunk) {
        return embedText(documentTitle, chunk.getText());
    }

    public static String embedText(String documentTitle, String body) {
        if (documentTitle == null || documentTitle.isBlank()) {
            return body;
        }
        return "[" + documentTitle + "]\n" + body;
    }
}
