package com.vdt.finq.modules.evaluation;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.yaml.snakeyaml.Yaml;

import com.vdt.finq.modules.agent.AskResult;
import com.vdt.finq.modules.agent.loop.QueryLoop;
import com.vdt.finq.modules.evaluation.baseline.BaselineRunner;
import com.vdt.finq.modules.hierarchicalrag.generation.Citation;

// đánh giá câu trả lời nhánh số liệu trên data/eval/eval_dbquery.yml (như RQ3 của paper FinAI), cùng model:
//   FINQUERY  3 tool SQL mẫu | TEXT_TO_SQL  LLM tự viết SQL | LLM_ONLY  không công cụ
// ghi từng câu ngay khi xong (chạy tiếp được), rồi .md tổng hợp kèm toàn văn câu trả lời để soát:
//     ./mvnw test "-Dtest=DbAnswerEvaluationTest" "-Ddbeval=true" ["-Ddbeval.configs=FINQUERY,TEXT_TO_SQL"]
//     chạy tiếp: "-Ddbeval.resume=../../data/eval/ket-qua/danh-gia-db_<thời điểm>.csv"; chọn câu: "-Ddbeval.ids=D01,C01"
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = { "cleaner.trial.enabled=false", "spring.devtools.restart.enabled=false" })
@EnabledIfSystemProperty(named = "dbeval", matches = "true")
class DbAnswerEvaluationTest {

    private static final Path QUESTIONS = Path.of("../../data/eval/eval_dbquery.yml");
    private static final Path OUT_DIR = Path.of("../../data/eval/ket-qua");
    private static final List<String> ALL_CONFIGS = List.of("FINQUERY", "TEXT_TO_SQL", "LLM_ONLY");
    private static final String[] HEADER = { "cau_hinh", "id", "nhom", "dung", "ly_do", "trang_thai", "luot",
            "ms", "token_vao", "token_ra", "usd", "trich_dan_dung", "trich_dan", "so_khong_nguon", "sql_loi",
            "cong_cu", "tra_loi" };

    @Autowired
    private QueryLoop queryLoop;
    @Autowired
    private BaselineRunner baselines;

    private record Row(Map<String, String> v) {
        String get(String k) {
            return v.getOrDefault(k, "");
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    void evaluate() throws Exception {
        List<Map<String, Object>> questions;
        try (InputStream in = Files.newInputStream(QUESTIONS)) {
            questions = new Yaml().load(in);
        }
        String ids = System.getProperty("dbeval.ids", "");
        if (!ids.isBlank()) {
            List<String> keep = List.of(ids.split(","));
            questions = questions.stream().filter(q -> keep.contains(q.get("id"))).toList();
        }
        String wanted = System.getProperty("dbeval.configs", "");
        List<String> configs = wanted.isBlank() ? ALL_CONFIGS : List.of(wanted.split(","));
        String resume = System.getProperty("dbeval.resume", "");
        Path csv = resume.isBlank() ? OUT_DIR.resolve("danh-gia-db_"
                + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmm")) + ".csv") : Path.of(resume);
        List<Row> rows = new ArrayList<>(Files.exists(csv) ? readCsv(csv) : List.of());
        if (!Files.exists(csv)) {
            Files.writeString(csv, String.join(";", HEADER) + "\n", StandardCharsets.UTF_8);
        }

        for (Map<String, Object> q : questions) {
            for (String config : configs) {
                String id = (String) q.get("id");
                if (rows.stream().anyMatch(r -> r.get("cau_hinh").equals(config) && r.get("id").equals(id))) {
                    continue;
                }
                Row row = run(config, q, (Map<String, Object>) q.get("cham"));
                rows.add(row);
                Files.writeString(csv, csvLine(row), StandardCharsets.UTF_8, StandardOpenOption.APPEND);
                System.out.printf("[%s] %s %s (%s) %s ms %s USD%n", config, id, row.get("dung").equals("1") ? "ĐÚNG" : "SAI",
                        row.get("ly_do"), row.get("ms"), row.get("usd"));
            }
        }
        Path md = Path.of(csv.toString().replace(".csv", ".md"));
        Files.writeString(md, report(questions, rows, configs, csv.getFileName().toString()), StandardCharsets.UTF_8);
        System.out.println("đã ghi " + md.toAbsolutePath().normalize());
    }

    private Row run(String config, Map<String, Object> q, Map<String, Object> cham) {
        String question = (String) q.get("cau_hoi");
        Map<String, String> v = new LinkedHashMap<>();
        v.put("cau_hinh", config);
        v.put("id", (String) q.get("id"));
        v.put("nhom", (String) q.get("nhom"));
        String answer;
        try {
            if (config.equals("LLM_ONLY")) {
                BaselineRunner.PlainAnswer p = baselines.llmOnly(question);
                answer = p.answer();
                v.put("trang_thai", "");
                v.put("luot", "1");
                v.put("ms", String.valueOf(p.millis()));
                v.put("token_vao", String.valueOf(p.promptTokens()));
                v.put("token_ra", String.valueOf(p.completionTokens()));
                v.put("usd", String.format("%.5f", p.cost()));
            } else {
                AskResult r = config.equals("FINQUERY") ? queryLoop.ask(question) : baselines.textToSql(question);
                answer = r.getAnswer();
                v.put("trang_thai", r.getStatus());
                v.put("luot", String.valueOf(r.getToolRounds()));
                v.put("ms", String.valueOf(r.getTimingsMs().get("total")));
                v.put("token_vao", String.valueOf(r.getPromptTokens()));
                v.put("token_ra", String.valueOf(r.getCompletionTokens()));
                v.put("usd", String.format("%.5f", r.getCostUsd().get("total")));
                v.put("trich_dan_dung", String.valueOf(r.getCitations().stream().filter(Citation::isVerified).count()));
                v.put("trich_dan", String.valueOf(r.getCitations().size()));
                v.put("so_khong_nguon", String.valueOf(r.getUnverifiedNumbers().size()));
                v.put("sql_loi", String.valueOf(r.getToolTrace().stream()
                        .filter(t -> t.startsWith("execute_sql LỖI") || t.startsWith("execute_sql BỊ CHẶN")).count()));
                v.put("cong_cu", String.join(" || ", r.getToolTrace()));
            }
        } catch (RuntimeException e) {
            answer = "";
            v.put("trang_thai", "loi: " + e.getMessage());
        }
        DbAnswerScorer.Verdict verdict = DbAnswerScorer.score(answer, cham);
        v.put("dung", verdict.correct() ? "1" : "0");
        v.put("ly_do", verdict.reason());
        v.put("tra_loi", answer);
        return new Row(v);
    }

    private static String report(List<Map<String, Object>> questions, List<Row> rows, List<String> configs, String csvName) {
        StringBuilder s = new StringBuilder("# Đánh giá câu trả lời nhánh số liệu — ")
                .append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))).append("\n\n")
                .append("Bộ câu hỏi: `data/eval/eval_dbquery.yml` (").append(questions.size()).append(" câu). Cùng model, ")
                .append("chấm máy \"đúng và đủ số được hỏi\" (`DbAnswerScorer`), **cần người soát lại**. Chi tiết: `")
                .append(csvName).append("`.\n\n")
                .append("- FINQUERY: 3 tool SQL mẫu, LLM chỉ điền tham số\n- TEXT_TO_SQL: LLM tự viết SQL (tool execute_sql, user chỉ đọc)\n")
                .append("- LLM_ONLY: không công cụ, trả lời từ trí nhớ\n\n## 1. Tổng hợp\n\n")
                .append("| Cấu hình | Đúng | Một số | So sánh | Từ chối | Thời gian TB ± ĐLC (s) | p50 (s) | USD/câu | Token vào TB | Lỗi SQL | Số không có nguồn |\n")
                .append("|---|---|---|---|---|---|---|---|---|---|---|\n");
        for (String c : configs) {
            List<Row> rs = rows.stream().filter(r -> r.get("cau_hinh").equals(c)).toList();
            if (rs.isEmpty()) {
                continue;
            }
            List<Double> secs = rs.stream().map(r -> num(r.get("ms")) / 1000).toList();
            double mean = secs.stream().mapToDouble(Double::doubleValue).average().orElse(0);
            double sd = Math.sqrt(secs.stream().mapToDouble(x -> (x - mean) * (x - mean)).sum() / Math.max(1, secs.size() - 1));
            s.append(String.format("| %s | **%d/%d** | %s | %s | %s | %.1f ± %.1f | %.1f | %.4f | %.0f | %d | %d |%n", c,
                    correct(rs), rs.size(), group(rs, "db_mot_so"), group(rs, "db_so_sanh"), group(rs, "tu_choi"), mean, sd,
                    RetrievalMetrics.percentile(secs.stream().map(x -> (long) (x * 1000)).toList(), 50) / 1000.0,
                    rs.stream().mapToDouble(r -> num(r.get("usd"))).average().orElse(0),
                    rs.stream().mapToDouble(r -> num(r.get("token_vao"))).average().orElse(0),
                    (long) rs.stream().mapToDouble(r -> num(r.get("sql_loi"))).sum(),
                    (long) rs.stream().mapToDouble(r -> num(r.get("so_khong_nguon"))).sum()));
        }
        s.append("\n## 2. Từng câu\n\n| Câu | Nhóm | ").append(String.join(" | ", configs)).append(" |\n|---|---|")
                .append("---|".repeat(configs.size())).append('\n');
        for (Map<String, Object> q : questions) {
            s.append("| ").append(q.get("id")).append(" | ").append(q.get("nhom"));
            for (String c : configs) {
                Row r = find(rows, c, (String) q.get("id"));
                s.append(" | ").append(r == null ? "" : (r.get("dung").equals("1") ? "✓" : "✗ " + r.get("ly_do")));
            }
            s.append(" |\n");
        }
        s.append("\n## 3. Câu trả lời để soát\n");
        for (Map<String, Object> q : questions) {
            s.append("\n### ").append(q.get("id")).append(" — ").append(q.get("cau_hoi")).append("\n\n**Đáp án vàng:** ")
                    .append(q.get("dap_an")).append("\n\n");
            for (String c : configs) {
                Row r = find(rows, c, (String) q.get("id"));
                if (r == null) {
                    continue;
                }
                s.append("- **").append(c).append("** ").append(r.get("dung").equals("1") ? "✓" : "✗").append(" (")
                        .append(r.get("ly_do")).append(", ").append(String.format("%.1f s", num(r.get("ms")) / 1000))
                        .append("): ").append(r.get("tra_loi").replace("\n", " ")).append('\n');
                if (!r.get("cong_cu").isBlank()) {
                    s.append("  - công cụ: ").append(r.get("cong_cu")).append('\n');
                }
            }
        }
        return s.toString();
    }

    private static long correct(List<Row> rs) {
        return rs.stream().filter(r -> r.get("dung").equals("1")).count();
    }

    private static String group(List<Row> rs, String nhom) {
        List<Row> g = rs.stream().filter(r -> r.get("nhom").equals(nhom)).toList();
        return g.isEmpty() ? "" : correct(g) + "/" + g.size();
    }

    private static Row find(List<Row> rows, String config, String id) {
        return rows.stream().filter(r -> r.get("cau_hinh").equals(config) && r.get("id").equals(id)).findFirst().orElse(null);
    }

    private static double num(String s) {
        try {
            return s == null || s.isBlank() ? 0 : Double.parseDouble(s.replace(',', '.'));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    // CSV phân cách ";" , ô bọc ngoặc kép, ngoặc kép bên trong đổi thành nháy đơn, xuống dòng thành " / "
    private static String csvLine(Row r) {
        return java.util.Arrays.stream(HEADER)
                .map(k -> "\"" + r.get(k).replace("\"", "'").replace("\r", "").replace("\n", " / ") + "\"")
                .collect(Collectors.joining(";")) + "\n";
    }

    private static List<Row> readCsv(Path csv) throws Exception {
        List<Row> out = new ArrayList<>();
        List<String> lines = Files.readAllLines(csv, StandardCharsets.UTF_8);
        for (String line : lines.subList(1, lines.size())) {
            if (line.isBlank()) {
                continue;
            }
            String[] cells = line.substring(1, line.length() - 1).split("\";\"", -1);
            Map<String, String> v = new LinkedHashMap<>();
            for (int i = 0; i < HEADER.length && i < cells.length; i++) {
                v.put(HEADER[i], cells[i]);
            }
            out.add(new Row(v));
        }
        return out;
    }
}
