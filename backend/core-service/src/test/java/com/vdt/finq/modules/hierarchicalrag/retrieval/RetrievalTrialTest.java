package com.vdt.finq.modules.hierarchicalrag.retrieval;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.vdt.finq.modules.hierarchicalrag.preretrieval.PreparedQuery;
import com.vdt.finq.modules.hierarchicalrag.retrieval.rerank.TierStat;

// chạy thật truy hồi trên vài câu mẫu, ghi ra trial-retrieval.md (cần MySQL + Qdrant):
//     ./mvnw test -Dtest=RetrievalTrialTest -Dtrial.retrieval=true
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = { "cleaner.trial.enabled=false", "spring.devtools.restart.enabled=false" })
@EnabledIfSystemProperty(named = "trial.retrieval", matches = "true")
class RetrievalTrialTest {

    private static final Path REPORT = Path.of("trial-retrieval.md");
    private static final int SHOW = 10;

    private static final List<String> QUESTIONS = List.of(
            "Doanh thu thuần quý 3/2024 của CTR là bao nhiêu?",
            "LNST năm ngoái là bao nhiêu?",
            "Tổng tài sản cuối năm 2023 trên báo cáo riêng",
            "Cổ tức năm 2024 chia bao nhiêu phần trăm?",
            "Vì sao lợi nhuận sau thuế quý 2/2025 thay đổi so với cùng kỳ?",
            "Chiến lược phát triển của công ty trong các năm tới là gì?");

    @Autowired
    private RetrievalService retrievalService;

    @Test
    void retrieveSampleQuestions() throws Exception {
        StringBuilder out = new StringBuilder("# Chạy thử truy hồi\n\n")
                .append("Luồng: chuẩn hoá → từ khoá → bộ lọc → Qdrant 80 → rerank tầng 1 giữ 40 → tầng 2 giữ 20. ")
                .append("Bảng chỉ in ").append(SHOW).append(" chunk đầu.\n\n");
        double totalCost = 0;
        for (String question : QUESTIONS) {
            RetrievalResult r = retrievalService.retrieve(question, RetrievalOptions.DEFAULT);
            assertThat(r.getChunks()).isNotEmpty();
            totalCost += r.getRerankCost();
            write(out, r);
        }
        out.append("---\nTổng tiền rerank cả lượt: ").append(String.format("%.6f", totalCost)).append(" USD\n");
        Files.writeString(REPORT, out, StandardCharsets.UTF_8);
        System.out.println("đã ghi " + REPORT.toAbsolutePath());
    }

    private static void write(StringBuilder out, RetrievalResult r) {
        PreparedQuery p = r.getPrepared();
        out.append("## ").append(r.getQuestion()).append("\n\n")
                .append("| Bước | Kết quả |\n|---|---|\n")
                .append("| Chuẩn hoá | ").append(cell(p.getNormalized().getText())).append(" |\n")
                .append("| Từ khoá | ").append(cell(String.join("; ", p.getKeywords()))).append(" |\n")
                .append("| Bộ lọc | ").append(cell(String.join(", ", p.getFilter().getApplied()))).append(" |\n")
                .append("| Chế độ lọc | ").append(r.getFilterMode()).append(" → Qdrant trả ").append(r.getQdrantHits())
                .append(" chunk |\n");
        for (TierStat t : r.getTiers()) {
            out.append("| Rerank ").append(t.getTier()).append(" | ");
            if (t.getSkipped() != null) {
                out.append("bỏ qua: ").append(cell(t.getSkipped()));
            } else {
                out.append(t.getModel()).append(": ").append(t.getInputCount()).append(" → ").append(t.getKeptCount())
                        .append(", ").append(t.getMillis()).append(" ms, ").append(t.getInputTokens())
                        .append(" token, ").append(String.format("%.6f", t.getCost())).append(" USD");
            }
            out.append(" |\n");
        }
        out.append("| Thời gian (ms) | ").append(r.getTimingsMs()).append(" |\n\n");

        out.append("| # | Tài liệu | Trang | Năm/kỳ | Vector | Tầng 1 | Tầng 2 | Mở đầu chunk |\n")
                .append("|---|---|---|---|---|---|---|---|\n");
        for (int i = 0; i < Math.min(SHOW, r.getChunks().size()); i++) {
            RetrievedChunk c = r.getChunks().get(i);
            out.append("| ").append(i + 1)
                    .append(" | ").append(cell(c.getTenHienThi()))
                    .append(" | ").append(c.getTrang()).append(c.getPhan() > 0 ? "." + c.getPhan() : "")
                    .append(" | ").append(c.getNam()).append(c.getKy() == null ? "" : "/" + c.getKy())
                    .append(" | ").append(String.format("%.3f", c.getVectorScore()))
                    .append(" | ").append(score(c.getTier1Score()))
                    .append(" | ").append(score(c.getTier2Score()))
                    .append(" | ").append(cell(preview(c.getNoiDung()))).append(" |\n");
        }
        out.append("\n");
    }

    private static String score(Double s) {
        return s == null ? "-" : String.format("%.3f", s);
    }

    private static String preview(String text) {
        String flat = text.replaceAll("\\s+", " ").strip();
        return flat.length() > 110 ? flat.substring(0, 110) + "…" : flat;
    }

    private static String cell(String text) {
        return text == null ? "" : text.replace("|", "\\|").replace("\n", " ");
    }
}
