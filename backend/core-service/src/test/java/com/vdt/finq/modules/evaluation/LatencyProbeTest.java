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

// đo độ trễ từ lúc nhận câu hỏi đến khi có 20 chunk cuối, tách theo từng bước, trên 10 câu đại diện × 2 lượt.
// dùng để so nhà cung cấp API (DeepInfra, Alibaba...) trong cùng điều kiện. kết quả: data/eval/ket-qua/do-tre_<thời điểm>.md
//     ./mvnw test "-Dtest=LatencyProbeTest" "-Dlatency=true"
//     ./mvnw test "-Dtest=LatencyProbeTest" "-Dlatency=true" "-Dlatency.label=qwen-alibaba"
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
    private EmbeddingProperties embeddingProperties;

    @Test
    void measure() throws Exception {
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

    private static long tier(RetrievalResult r, String name) {
        return r.getTiers().stream().filter(t -> t.getTier().equals(name)).mapToLong(TierStat::getMillis).sum();
    }
}
