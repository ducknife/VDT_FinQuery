package com.vdt.finq.modules.evaluation;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.vdt.finq.common.properties.EmbeddingProperties;
import com.vdt.finq.common.properties.RerankProperties;
import com.vdt.finq.modules.hierarchicalrag.retrieval.RetrievalOptions;
import com.vdt.finq.modules.hierarchicalrag.retrieval.RetrievalResult;
import com.vdt.finq.modules.hierarchicalrag.retrieval.RetrievalService;
import com.vdt.finq.modules.hierarchicalrag.retrieval.rerank.TierStat;

// đo độ trễ truy hồi từng bước trên 10 câu × 2 lượt, ghi ra data/eval/ket-qua/do-tre_*.md:
//     ./mvnw test "-Dtest=LatencyProbeTest" "-Dlatency=true" ["-Dlatency.label=..."]
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = { "cleaner.trial.enabled=false", "spring.devtools.restart.enabled=false" })
@EnabledIfSystemProperty(named = "latency", matches = "true")
class LatencyProbeTest {

    private static final int ROUNDS = 2;
    // đủ các nhóm câu: đoạn văn, bảng, nhiều trang, DB một số, so sánh, không dấu, thời gian tương đối
    private static final List<String> QUESTIONS = List.of(
            "Nguyên nhân lãi ròng hợp nhất quý 3/2025 tăng so với cùng kỳ là gì?",
            "Cổ tức năm 2024 được chi trả theo tỷ lệ bao nhiêu và ngày đăng ký cuối cùng là ngày nào?",
            "Tập đoàn Viettel đang nắm bao nhiêu cổ phần của CTR?",
            "Kế hoạch lợi nhuận sau thuế hợp nhất năm nay là bao nhiêu?",
            "Năm 2025 công ty có hoàn thành kế hoạch lợi nhuận sau thuế hợp nhất không?",
            "Doanh thu thuần hợp nhất năm 2024 của CTR là bao nhiêu?",
            "Lợi nhuận gộp hợp nhất 6 tháng đầu năm 2026 là bao nhiêu?",
            "Doanh thu thuần hợp nhất năm 2025 tăng bao nhiêu phần trăm so với năm 2024?",
            "vi sao loi nhuan sau thue hop nhat nam 2025 cua CTR tang so voi nam 2024",
            "Tiền mặt hợp nhất cuối quý trước là bao nhiêu?");

    @Autowired
    private RetrievalService retrievalService;
    @Autowired
    private RerankProperties rerankProperties;
    @Autowired
    private com.vdt.finq.common.properties.RetrievalProperties retrievalProperties;
    @Autowired
    private EmbeddingProperties embeddingProperties;

    @Test
    void measure() throws Exception {
        if (Boolean.getBoolean("latency.ab")) {
            measureEnglishFilterAb();
            return;
        }
        String label = System.getProperty("latency.label", "deepinfra");
        List<Map<String, Long>> samples = new ArrayList<>();
        StringBuilder detail = new StringBuilder("| Lượt | Câu | Tiền xử lý | Embed | Tìm Qdrant | Rerank tầng 1 | Rerank tầng 2 | Tổng |\n"
                + "|---|---|---|---|---|---|---|---|\n");
        for (int round = 1; round <= ROUNDS; round++) {
            for (int i = 0; i < QUESTIONS.size(); i++) {
                RetrievalResult r = retrievalService.retrieve(QUESTIONS.get(i), RetrievalOptions.DEFAULT);
                Map<String, Long> s = new LinkedHashMap<>(r.getTimingsMs());
                s.put("tier1", tier(r, "tier1"));
                s.put("tier2", tier(r, "tier2"));
                samples.add(s);
                detail.append("| ").append(round).append(" | ").append(i + 1).append(" | ").append(s.get("preprocess"))
                        .append(" | ").append(s.get("embed")).append(" | ").append(s.get("search")).append(" | ")
                        .append(s.get("tier1")).append(" | ").append(s.get("tier2")).append(" | ").append(s.get("total"))
                        .append(" |\n");
                System.out.printf("[%s] lượt %d câu %d: %s%n", label, round, i + 1, s);
            }
        }

        StringBuilder md = new StringBuilder("# Độ trễ truy hồi — ").append(label).append(" — ")
                .append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))).append("\n\n")
                .append("Từ lúc nhận câu hỏi đến khi có 20 chunk cuối (80 → 40 → 20). ").append(QUESTIONS.size())
                .append(" câu × ").append(ROUNDS).append(" lượt. Embed: `").append(embeddingProperties.getModel())
                .append("` (").append(embeddingProperties.getApiUrl()).append("); rerank: `")
                .append(rerankProperties.getTier1Model()).append("` → `").append(rerankProperties.getTier2Model())
                .append("` (").append(rerankProperties.getApiUrl()).append(").\n\n")
                .append("| Bước | Trung vị (ms) | p90 (ms) | Lớn nhất (ms) |\n|---|---|---|---|\n");
        for (String[] step : new String[][] { { "preprocess", "Tiền xử lý (chuẩn hoá, từ khoá, bộ lọc)" },
                { "embed", "Embed câu hỏi" }, { "search", "Tìm Qdrant + gắn tên tài liệu" },
                { "tier1", "Rerank tầng 1" }, { "tier2", "Rerank tầng 2" }, { "total", "**Tổng**" } }) {
            List<Long> v = samples.stream().map(s -> s.getOrDefault(step[0], 0L)).toList();
            md.append("| ").append(step[1]).append(" | ").append(RetrievalMetrics.percentile(v, 50)).append(" | ")
                    .append(RetrievalMetrics.percentile(v, 90)).append(" | ")
                    .append(v.stream().mapToLong(Long::longValue).max().orElse(0)).append(" |\n");
        }
        md.append("\n## Từng lần chạy\n\n").append(detail);
        Path out = Path.of("../../data/eval/ket-qua/do-tre_" + label + "_"
                + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmm")) + ".md");
        Files.createDirectories(out.getParent());
        Files.writeString(out, md, StandardCharsets.UTF_8);
        System.out.println("đã ghi " + out.toAbsolutePath());
    }

    // A/B xen kẽ bật/tắt lọc tiếng Anh trên cùng câu, đo cả token gửi rerank:
    //     ./mvnw test "-Dtest=LatencyProbeTest" "-Dlatency=true" "-Dlatency.ab=true"
    private void measureEnglishFilterAb() throws Exception {
        boolean original = retrievalProperties.isDropEnglish();
        Map<Boolean, List<long[]>> arms = new LinkedHashMap<>();
        arms.put(false, new ArrayList<>());
        arms.put(true, new ArrayList<>());
        StringBuilder detail = new StringBuilder("| Lượt | Câu | Lọc | Chunk vào rerank | Token tầng 1 | Token tầng 2 | Embed | Rerank tầng 1 | Rerank tầng 2 | Tổng |\n"
                + "|---|---|---|---|---|---|---|---|---|---|\n");
        try {
            for (int round = 1; round <= ROUNDS; round++) {
                for (int i = 0; i < QUESTIONS.size(); i++) {
                    boolean[] order = round % 2 == 1 ? new boolean[] { false, true } : new boolean[] { true, false };
                    for (boolean drop : order) {
                        retrievalProperties.setDropEnglish(drop);
                        RetrievalResult r = retrievalService.retrieve(QUESTIONS.get(i), RetrievalOptions.DEFAULT);
                        long[] s = { r.getQdrantHits(), tierTokens(r, "tier1"), tierTokens(r, "tier2"),
                                r.getTimingsMs().get("embed"), tier(r, "tier1"), tier(r, "tier2"), r.getTimingsMs().get("total") };
                        arms.get(drop).add(s);
                        detail.append(String.format("| %d | %d | %s | %d | %d | %d | %d | %d | %d | %d |%n", round, i + 1,
                                drop ? "bật" : "tắt", s[0], s[1], s[2], s[3], s[4], s[5], s[6]));
                        System.out.printf("[ab] lượt %d câu %d lọc=%s: %s%n", round, i + 1, drop, java.util.Arrays.toString(s));
                    }
                }
            }
        } finally {
            retrievalProperties.setDropEnglish(original);
        }
        String[] names = { "Chunk vào rerank", "Token tầng 1", "Token tầng 2", "Embed (ms)", "Rerank tầng 1 (ms)",
                "Rerank tầng 2 (ms)", "Tổng (ms)" };
        StringBuilder md = new StringBuilder("# A/B xen kẽ bộ lọc tiếng Anh — ")
                .append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))).append("\n\n")
                .append(QUESTIONS.size()).append(" câu × ").append(ROUNDS)
                .append(" lượt; mỗi câu chạy tắt lọc và bật lọc ngay sau nhau (lượt 2 đảo thứ tự).\n\n")
                .append("| Chỉ số (trung vị) | Tắt lọc | Bật lọc |\n|---|---|---|\n");
        for (int k = 0; k < names.length; k++) {
            final int col = k;
            md.append("| ").append(names[k]);
            for (boolean drop : new boolean[] { false, true }) {
                List<Long> v = arms.get(drop).stream().map(s -> s[col]).toList();
                md.append(" | ").append(RetrievalMetrics.percentile(v, 50));
            }
            md.append(" |\n");
        }
        md.append("\n## Từng lần chạy\n\n").append(detail);
        Path out = Path.of("../../data/eval/ket-qua/do-tre_ab-loc-tieng-anh_"
                + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmm")) + ".md");
        Files.writeString(out, md, StandardCharsets.UTF_8);
        System.out.println("đã ghi " + out.toAbsolutePath());
    }

    private static long tierTokens(RetrievalResult r, String name) {
        return r.getTiers().stream().filter(t -> t.getTier().equals(name)).mapToLong(TierStat::getInputTokens).sum();
    }

    private static long tier(RetrievalResult r, String name) {
        return r.getTiers().stream().filter(t -> t.getTier().equals(name)).mapToLong(TierStat::getMillis).sum();
    }
}
