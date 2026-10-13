package com.vdt.finq.modules.evaluation;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.vdt.finq.common.properties.RetrievalProperties;
import com.vdt.finq.common.text.EnglishFilter;
import com.vdt.finq.modules.rag.preprocessing.indexing.embedder.Embedding;
import com.vdt.finq.modules.rag.preprocessing.indexing.embedder.EmbeddingClient;
import com.vdt.finq.modules.rag.preretrieval.PreparedQuery;
import com.vdt.finq.modules.rag.preretrieval.QueryPreprocessor;
import com.vdt.finq.modules.rag.retrieval.QdrantFilters;
import com.vdt.finq.modules.rag.retrieval.QdrantRetriever;
import com.vdt.finq.modules.rag.retrieval.RetrievedChunk;
import com.vdt.finq.utils.TableToJson;

// so điểm cosine của một trang với câu hỏi theo các cách embed, cạnh điểm ngưỡng lọt top-k của Qdrant:
//     ./mvnw test "-Dtest=EmbedProbeTest" "-Dembedprobe=true" "-Dembedprobe.q=@<file câu hỏi, cách nhau ||>"
//         "-Dembedprobe.page=@<file nội dung trang>" "-Dembedprobe.title=@<file tên tài liệu>"
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = { "cleaner.trial.enabled=false", "answer.trial.enabled=false", "spring.devtools.restart.enabled=false" })
@EnabledIfSystemProperty(named = "embedprobe", matches = "true")
class EmbedProbeTest {

    @Autowired
    private QueryPreprocessor preprocessor;
    @Autowired
    private EmbeddingClient embeddingClient;
    @Autowired
    private QdrantRetriever retriever;
    @Autowired
    private RetrievalProperties properties;
    @Autowired
    private TableToJson tableToJson;
    @Autowired
    private org.springframework.jdbc.core.JdbcTemplate jdbc;
    @Autowired
    private com.vdt.finq.modules.rag.preretrieval.normalizer.FinancialTextNormalizer normalizer;

    @Test
    void probe() throws Exception {
        String page = prop("embedprobe.page");
        String title = prop("embedprobe.title");
        Map<String, String> variants = new LinkedHashMap<>();
        // page đọc nguyên văn từ payload (giữ \r\n); các biến thể là những cách sửa phía nạp
        String lf = page.replace("\r\n", "\n");
        String heading = prop("embedprobe.heading");
        variants.put("đang index", Embedding.embedText(title, tableToJson.convert(page)));
        variants.put("đổi \\r\\n -> \\n", Embedding.embedText(title, tableToJson.convert(lf)));
        variants.put("bỏ tiếng Anh", Embedding.embedText(title, tableToJson.convert(EnglishFilter.stripEnglish(lf))));
        if (heading != null) {
            variants.put("tên + tiêu đề văn bản", Embedding.embedText(title + " — " + heading, tableToJson.convert(lf)));
        }
        List<String> names = new java.util.ArrayList<>(variants.keySet());
        // embed lẻ từng biến thể (như phía câu hỏi), để so với embed theo lô bên dưới
        List<float[]> pageVectors = new java.util.ArrayList<>();
        for (String text : variants.values()) {
            pageVectors.add(embeddingClient.embed(List.of(text)).get(0));
        }
        // embed theo lô như lúc nạp: trang này nằm chung lô với một trang dài hơn nhiều
        String longer = prop("embedprobe.long");
        if (longer != null) {
            names.add("lô cùng trang dài");
            pageVectors.add(embeddingClient.embed(List.of(longer, variants.values().iterator().next())).get(1));
        }
        System.out.println("[EMB] tên tài liệu trong DB: " + jdbc.queryForList(
                "SELECT ten_hien_thi FROM documents WHERE tai_lieu_id = ?", String.class,
                String.valueOf(System.getProperty("embedprobe.doc", "#").split("#")[0])));

        for (String q : prop("embedprobe.q").split("\\|\\|")) {
            PreparedQuery prepared = preprocessor.prepare(q.strip(), true);
            float[] qv = embeddingClient.embed(List.of("Instruct: " + properties.getQueryInstruction() + "\nQuery:"
                    + prepared.getText())).get(0);
            List<RetrievedChunk> hits = retriever.search(qv, QdrantFilters.strict(prepared.getFilter()), properties.getTopK());
            System.out.printf("[EMB] %s%n[EMB]   ngưỡng: hạng 1 = %.4f, hạng %d = %.4f%n", q.strip(), hits.get(0).getVectorScore(),
                    hits.size(), hits.get(hits.size() - 1).getVectorScore());
            for (int i = 0; i < names.size(); i++) {
                double cos = cosine(qv, pageVectors.get(i));
                long rank = hits.stream().filter(h -> h.getVectorScore() > cos).count() + 1;
                System.out.printf("[EMB]   %-22s cosine %.4f -> hạng ~%s%n", names.get(i), cos,
                        rank > hits.size() ? "ngoài " + hits.size() : String.valueOf(rank));
            }
            // vector đang lưu trong Qdrant của chính trang này (lọc theo tài liệu + trang)
            String doc = System.getProperty("embedprobe.doc");
            if (doc != null) {
                String[] d = doc.split("#");
                var only = io.qdrant.client.grpc.Points.Filter.newBuilder()
                        .addMust(io.qdrant.client.ConditionFactory.matchKeyword("tai_lieu_id", d[0]))
                        .addMust(io.qdrant.client.ConditionFactory.match("trang", Long.parseLong(d[1]))).build();
                for (RetrievedChunk c : retriever.search(qv, only, 5)) {
                    float s = c.getVectorScore();
                    long rank = hits.stream().filter(h -> h.getVectorScore() > s).count() + 1;
                    System.out.printf("[EMB]   %-22s cosine %.4f -> hạng ~%s (phần %d)%n", "vector đang lưu", s,
                            rank > hits.size() ? "ngoài " + hits.size() : String.valueOf(rank), c.getPhan());
                }
            }
        }
    }

    // vector đang lưu có còn khớp với embedding hiện tại? embed lại mẫu chunk theo đúng công thức nạp rồi so:
    //     "-Dembedprobe.drift=<file JSON [{doc, trang, text, vec}]>" (lấy từ Qdrant scroll)
    @Test
    void drift() throws Exception {
        String file = System.getProperty("embedprobe.drift");
        if (file == null) {
            return;
        }
        var json = new com.fasterxml.jackson.databind.ObjectMapper().readTree(Files.readString(Path.of(file), StandardCharsets.UTF_8));
        for (var p : json) {
            // "-Dembedprobe.title=@<file>": thử một tên khác với tên trong DB
            String title = System.getProperty("embedprobe.title") != null ? prop("embedprobe.title")
                    : jdbc.queryForObject("SELECT ten_hien_thi FROM documents WHERE tai_lieu_id = ?", String.class,
                            p.get("doc").asText());
            float[] stored = new float[p.get("vec").size()];
            for (int i = 0; i < stored.length; i++) {
                stored[i] = (float) p.get("vec").get(i).asDouble();
            }
            float[] now = embeddingClient.embed(List.of(Embedding.embedText(title, tableToJson.convert(p.get("text").asText()))))
                    .get(0);
            System.out.printf("[DRIFT] %s#%d (%d ký tự): cosine(lưu, embed lại) = %.4f%n", p.get("doc").asText(),
                    p.get("trang").asInt(), p.get("text").asText().length(), cosine(stored, now));
        }
    }

    private static double cosine(float[] a, float[] b) {
        double dot = 0, na = 0, nb = 0;
        for (int i = 0; i < a.length; i++) {
            dot += a[i] * b[i];
            na += a[i] * a[i];
            nb += b[i] * b[i];
        }
        return dot / Math.sqrt(na * nb);
    }

    // "@<file>": đọc từ file UTF-8 (tham số -D qua mvnw trên Windows mất dấu tiếng Việt)
    private static String prop(String key) throws Exception {
        String v = System.getProperty(key);
        return v != null && v.startsWith("@") ? Files.readString(Path.of(v.substring(1)), StandardCharsets.UTF_8) : v;
    }
}
