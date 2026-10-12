package com.vdt.finq.modules.agent;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.vdt.finq.modules.agent.loop.QueryLoop;
import com.vdt.finq.modules.hierarchicalrag.generation.Citation;

// chạy thật vòng agent, ghi ra data/eval/ket-qua/agent-thu_*.md (cần MySQL + Qdrant, tốn tiền API):
//     ./mvnw test "-Dtest=AgentTrialTest" "-Dagent=true" ["-Dagent.q=..."]
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = { "cleaner.trial.enabled=false", "spring.devtools.restart.enabled=false" })
@EnabledIfSystemProperty(named = "agent", matches = "true")
class AgentTrialTest {

    // câu V02 của bộ đánh giá (nhánh tài liệu); đáp án: công văn giải trình 25/10/2025 trang 2
    private static final String DEFAULT_QUESTION = "Nguyên nhân lãi ròng hợp nhất quý 3/2025 tăng so với cùng kỳ là gì?";

    // bộ 3 câu nhánh tài liệu: "-Dagent.set=true"
    private static final List<String> SET = List.of(
            DEFAULT_QUESTION,
            "Cổ tức năm 2024 được chi trả theo tỷ lệ bao nhiêu và ngày đăng ký cuối cùng là ngày nào?",
            "Kế hoạch lợi nhuận sau thuế hợp nhất năm nay là bao nhiêu?");

    // bộ 8 câu nhánh số liệu: "-Dagent.set=db"
    private static final List<String> DB_SET = List.of(
            "Nợ phải trả hợp nhất tại kỳ báo cáo gần nhất là bao nhiêu?",                                 // D03
            "Doanh thu thuần riêng của quý 2/2026 (chỉ riêng quý, không luỹ kế) là bao nhiêu?",            // D07
            "Doanh thu thuần hợp nhất năm 2019 (theo số liệu đã trình bày lại) là bao nhiêu?",              // D09
            "Tiền mặt hợp nhất cuối quý trước là bao nhiêu?",                                               // D11
            "Doanh thu thuần hợp nhất năm 2025 tăng bao nhiêu phần trăm so với năm 2024?",                  // C01
            "Tỷ lệ nợ phải trả trên tổng tài sản hợp nhất cuối năm 2025 là bao nhiêu?",                     // C03
            "Năm 2025 dòng tiền thuần từ hoạt động kinh doanh hợp nhất gấp bao nhiêu lần lợi nhuận sau thuế?", // C05
            "Doanh thu thuần hợp nhất cả năm 2026 là bao nhiêu?");                                          // X06

    @Autowired
    private QueryLoop queryLoop;

    @Test
    void ask() throws Exception {
        String set = System.getProperty("agent.set", "");
        List<String> questions = "true".equals(set) ? SET : "db".equals(set) ? DB_SET
                : List.of(System.getProperty("agent.q", DEFAULT_QUESTION));
        StringBuilder summary = new StringBuilder();
        for (String q : questions) {
            AskResult r = askOne(q);
            String line = String.format("| %s | %s | %d | %s | %s | %.4f |", q, r.getStatus(), r.getToolRounds(),
                    r.getTimingsMs(), r.getCitations().stream().map(c -> c.isVerified() ? "✓" : "✗").toList(),
                    r.getCostUsd().get("total"));
            System.out.println("TÓM TẮT " + line);
            summary.append(line).append('\n');
        }
        System.out.println("TÓM TẮT\n| Câu hỏi | Trạng thái | Lượt chọn tool | Thời gian (ms) | Câu trích | USD |\n|---|---|---|---|---|---|\n"
                + summary);
    }

    private AskResult askOne(String question) throws Exception {
        AskResult r = queryLoop.ask(question);

        StringBuilder md = new StringBuilder("# Chạy thử agent — ")
                .append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))).append("\n\n")
                .append("**Câu hỏi:** ").append(question).append("\n\n")
                .append("**Trạng thái:** `").append(r.getStatus()).append("` · prompt `").append(r.getPromptVersion())
                .append("` · model `").append(r.getModel()).append("`\n\n")
                .append("## Câu trả lời\n\n").append(r.getAnswer()).append("\n\n");
        if (!r.getMissing().isBlank()) {
            md.append("**Thiếu:** ").append(r.getMissing()).append("\n\n");
        }
        md.append("## Trích dẫn\n\n| Mã | Nguồn | Trang | Câu trích | Kiểm thấy trong nguồn |\n|---|---|---|---|---|\n");
        for (Citation c : r.getCitations()) {
            md.append("| ").append(c.getId()).append(" | ").append(cell(c.getTenHienThi())).append(" | ")
                    .append(c.getTrang()).append(" | ").append(cell(c.getQuote())).append(" | ")
                    .append(c.isVerified() ? "✓" : "✗ KHÔNG").append(" |\n");
        }
        md.append("\n## Kỷ luật bằng chứng\n\n")
                .append("- Số trong câu trả lời không có trong nguồn: ").append(r.getUnverifiedNumbers()).append('\n')
                .append("- Mã trích dẫn không tồn tại: ").append(r.getUnknownIds()).append("\n\n")
                .append("## Các lượt agent\n\n- Số lượt chọn tool: ").append(r.getToolRounds()).append('\n');
        r.getToolTrace().forEach(t -> md.append("- ").append(t).append('\n'));
        md.append("\n## Thời gian (ms) và chi phí (USD)\n\n- Thời gian: ").append(r.getTimingsMs()).append('\n')
                .append("- Chi phí: ").append(r.getCostUsd()).append('\n')
                .append("- Token: vào ").append(r.getPromptTokens()).append(", ra ").append(r.getCompletionTokens())
                .append('\n');

        Path out = Path.of("../../data/eval/ket-qua/agent-thu_"
                + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")) + ".md");
        Files.createDirectories(out.getParent());
        Files.writeString(out, md, StandardCharsets.UTF_8);
        System.out.println("đã ghi " + out.toAbsolutePath());
        return r;
    }

    private static String cell(String s) {
        return s == null ? "" : s.replace("|", "\\|").replace("\n", " ");
    }
}
