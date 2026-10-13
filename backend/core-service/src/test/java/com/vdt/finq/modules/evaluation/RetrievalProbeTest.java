package com.vdt.finq.modules.evaluation;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.vdt.finq.modules.rag.retrieval.ChunkRef;
import com.vdt.finq.modules.rag.retrieval.RetrievalOptions;
import com.vdt.finq.modules.rag.retrieval.RetrievalResult;
import com.vdt.finq.modules.rag.retrieval.RetrievalService;
import com.vdt.finq.modules.rag.retrieval.RetrievedChunk;

// chẩn đoán một câu: trang vàng đứng hạng mấy ở Qdrant / rerank tầng 1 / top cuối (không gọi LLM):
//     ./mvnw test "-Dtest=RetrievalProbeTest" "-Dprobe=true" "-Dprobe.gold=<tai_lieu_id>#<trang>" "-Dprobe.q=<câu 1>||<câu 2>"
// lặp n lần để đo dao động: "-Dprobe.n=5" "-Dprobe.basic=false"
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = { "cleaner.trial.enabled=false", "answer.trial.enabled=false", "spring.devtools.restart.enabled=false" })
@EnabledIfSystemProperty(named = "probe", matches = "true")
class RetrievalProbeTest {

    @Autowired
    private RetrievalService retrievalService;

    @Test
    void probe() {
        String[] gold = System.getProperty("probe.gold").split("#");
        int n = Integer.parseInt(System.getProperty("probe.n", "1"));
        var configs = Boolean.parseBoolean(System.getProperty("probe.basic", "true"))
                ? List.of(Map.entry("FINQUERY", RetrievalOptions.DEFAULT), Map.entry("RAG_CO_BAN", RetrievalOptions.BASIC_RAG))
                : List.of(Map.entry("FINQUERY", RetrievalOptions.DEFAULT));
        for (String q : prop("probe.q").split("\\|\\|")) {
            for (int i = 0; i < n; i++) {
            for (var entry : configs) {
                RetrievalResult r = retrievalService.retrieve(q.strip(), entry.getValue());
                List<ChunkRef> last = r.getChunks().stream().map(ChunkRef::of).toList();
                System.out.printf("[PROBE] %s | %s | lọc %s | Qdrant %s | tầng 1 %s | top cuối %s%n", entry.getKey(), q.strip(),
                        r.getFilterMode(), rank(r.getQdrantRanking(), gold), rank(r.getTier1Ranking(), gold),
                        rank(last, gold));
                System.out.println("[PROBE]   top 5: " + r.getChunks().stream().limit(5)
                        .map(RetrievalProbeTest::label).toList());
            }
            }
        }
    }

    // hạng 1-based của trang vàng, "-" nếu không có
    private static String rank(List<ChunkRef> refs, String[] gold) {
        for (int i = 0; i < refs.size(); i++) {
            ChunkRef c = refs.get(i);
            if (c.getTaiLieuId().equals(gold[0]) && c.getTrang() == Integer.parseInt(gold[1])) {
                return "#" + (i + 1) + "/" + refs.size();
            }
        }
        return "-/" + refs.size();
    }

    private static String label(RetrievedChunk c) {
        return c.getTenHienThi() + " tr." + c.getTrang();
    }

    // tham số -D qua mvnw trên Windows mất dấu tiếng Việt (cp1252): "@<file>" thì đọc chuỗi từ file UTF-8
    private static String prop(String key) {
        String v = System.getProperty(key);
        try {
            return v != null && v.startsWith("@")
                    ? java.nio.file.Files.readString(java.nio.file.Path.of(v.substring(1)), java.nio.charset.StandardCharsets.UTF_8).strip()
                    : v;
        } catch (java.io.IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
