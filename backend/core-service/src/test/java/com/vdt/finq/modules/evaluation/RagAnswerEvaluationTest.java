package com.vdt.finq.modules.evaluation;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.yaml.snakeyaml.Yaml;

import com.vdt.finq.common.properties.LlmProperties;
import com.vdt.finq.modules.agent.AskResult;
import com.vdt.finq.modules.agent.loop.QueryLoop;
import com.vdt.finq.modules.evaluation.baseline.BaselineRunner;
import com.vdt.finq.modules.rag.generation.Citation;

// đánh giá câu trả lời nhánh tài liệu trên eval_test.yml (26 câu rag_* + 3 câu từ chối không có số), cùng model:
//   FINQUERY  agent + RAG phân cấp | RAG_CO_BAN  nguyên câu hỏi -> vector top 20, cùng bước trả lời và bộ kiểm.
// chấm: đúng ý (AnswerJudge, người duyệt lại), trích đúng trang vàng, câu trích có thật, số không có nguồn.
//     ./mvnw test "-Dtest=RagAnswerEvaluationTest" "-Dragevel=true" ["-Dragevel.configs=FINQUERY"] ["-Dragevel.ids=V01,V02"]
//     chạy tiếp: "-Dragevel.resume=../../data/eval/ket-qua/danh-gia-rag_<thời điểm>.csv"; nhãn model: "-Dragevel.label=qwen"
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = { "cleaner.trial.enabled=false", "answer.trial.enabled=false" })
@EnabledIfSystemProperty(named = "ragevel", matches = "true")
class RagAnswerEvaluationTest {

    private static final Path QUESTIONS = Path.of("../../data/eval/eval_test.yml");
    private static final Path OUT_DIR = Path.of("../../data/eval/ket-qua");
    private static final List<String> ALL_CONFIGS = List.of("FINQUERY", "RAG_CO_BAN");
    private static final Set<String> REFUSALS = Set.of("X01", "X02", "X05");
    private static final String[] HEADER = { "cau_hinh", "id", "nhom", "danh_gia", "ly_do", "trang_thai", "luot", "ms",
            "token_vao", "token_ra", "usd", "usd_cham", "trich_dan_dung", "trich_dan", "trung_trang_vang",
            "so_khong_nguon", "trang_trich", "cong_cu", "tra_loi" };

    @Autowired
    private QueryLoop queryLoop;
    @Autowired
    private BaselineRunner baselines;
    @Autowired
    private AnswerJudge judge;
    @Autowired
    private LlmProperties llmProperties;

    private record Row(Map<String, String> v) {
        String get(String k) {
            return v.getOrDefault(k, "");
        }
    }

    @Test
    void evaluate() throws Exception {
        List<Map<String, Object>> all;
        try (InputStream in = Files.newInputStream(QUESTIONS)) {
            all = new Yaml().load(in);
        }
        List<Map<String, Object>> questions = all.stream()
                .filter(q -> String.valueOf(q.get("nhom")).startsWith("rag") || REFUSALS.contains(q.get("id"))).toList();
        String ids = System.getProperty("ragevel.ids", "");
        if (!ids.isBlank()) {
            List<String> keep = List.of(ids.split(","));
            questions = questions.stream().filter(q -> keep.contains(q.get("id"))).toList();
        }
        String wanted = System.getProperty("ragevel.configs", "");
        List<String> configs = wanted.isBlank() ? ALL_CONFIGS : List.of(wanted.split(","));
        String resume = System.getProperty("ragevel.resume", "");
        String label = System.getProperty("ragevel.label", "");
        Path csv = resume.isBlank() ? OUT_DIR.resolve("danh-gia-rag_"
                + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmm"))
                + (label.isBlank() ? "" : "_" + label) + ".csv") : Path.of(resume);
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
                Row row = run(config, q);
                rows.add(row);
                Files.writeString(csv, csvLine(row), StandardCharsets.UTF_8, StandardOpenOption.APPEND);
                System.out.printf("[%s] %s %s (%s) %s ms trang vàng=%s%n", config, id, row.get("danh_gia"),
                        row.get("ly_do"), row.get("ms"), row.get("trung_trang_vang"));
            }
        }
        Path md = Path.of(csv.toString().replace(".csv", ".md"));
        Files.writeString(md, report(questions, rows, configs, csv.getFileName().toString()), StandardCharsets.UTF_8);
        System.out.println("đã ghi " + md.toAbsolutePath().normalize());
    }

    @SuppressWarnings("unchecked")
    private Row run(String config, Map<String, Object> q) {
        String id = (String) q.get("id");
        String question = (String) q.get("cau_hoi");
        Map<String, String> v = new LinkedHashMap<>();
        v.put("cau_hinh", config);
        v.put("id", id);
        v.put("nhom", String.valueOf(q.get("nhom")));
        AskResult r;
        try {
            r = config.equals("FINQUERY") ? queryLoop.ask(question) : baselines.basicRag(question);
        } catch (RuntimeException e) {
            v.put("danh_gia", "wrong");
            v.put("ly_do", "lỗi: " + e.getMessage());
            return new Row(v);
        }
        v.put("trang_thai", r.getStatus());
        v.put("luot", String.valueOf(r.getToolRounds()));
        v.put("ms", String.valueOf(r.getTimingsMs().get("total")));
        v.put("token_vao", String.valueOf(r.getPromptTokens()));
        v.put("token_ra", String.valueOf(r.getCompletionTokens()));
        v.put("usd", String.format("%.5f", r.getCostUsd().get("total")));
        v.put("trich_dan_dung", String.valueOf(r.getCitations().stream().filter(Citation::isVerified).count()));
        v.put("trich_dan", String.valueOf(r.getCitations().size()));
        v.put("so_khong_nguon", String.valueOf(r.getUnverifiedNumbers().size()));
        v.put("trang_trich", r.getCitations().stream().filter(c -> c.getTaiLieuId() != null)
                .map(c -> c.getTaiLieuId() + "#" + c.getTrang()).distinct().collect(Collectors.joining(" ")));
        v.put("cong_cu", String.join(" || ", r.getToolTrace()));
        v.put("tra_loi", r.getAnswer());

        // trích đúng trang vàng: có ít nhất một trích dẫn rơi vào trang vàng của câu
        Set<String> gold = ((List<Map<String, Object>>) q.getOrDefault("trang_vang", List.of())).stream()
                .map(g -> g.get("tai_lieu_id") + "#" + g.get("trang")).collect(Collectors.toSet());
        boolean hit = r.getCitations().stream().anyMatch(c -> gold.contains(c.getTaiLieuId() + "#" + c.getTrang()));
        v.put("trung_trang_vang", REFUSALS.contains(id) ? "" : hit ? "1" : "0");

        if (REFUSALS.contains(id)) {
            DbAnswerScorer.Verdict verdict = DbAnswerScorer.score(r.getAnswer(), Map.of("kieu", "tu_choi", "so_duoc_phep", List.of()),
                    new DbAnswerScorer.Context(true, r.getStatus(), r.getUnverifiedNumbers().size()));
            v.put("danh_gia", verdict.correct() ? "refuse_ok" : "refuse_wrong");
            v.put("ly_do", verdict.reason());
        } else {
            AnswerJudge.Judgment j = judge.judge(question, String.valueOf(q.get("dap_an")), r.getAnswer());
            v.put("danh_gia", j.verdict());
            v.put("ly_do", j.reason());
            v.put("usd_cham", String.format("%.5f", j.cost()));
        }
        return new Row(v);
    }

    private String report(List<Map<String, Object>> questions, List<Row> rows, List<String> configs, String csvName) {
        StringBuilder s = new StringBuilder("# Đánh giá câu trả lời nhánh tài liệu — ")
                .append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))).append("\n\n")
                .append("Bộ câu: `data/eval/eval_test.yml`, ").append(questions.size())
                .append(" câu (rag_* + X01, X02, X05). Model: `").append(llmProperties.getModel())
                .append("`. Chấm đúng ý: `AnswerJudge` (LLM), **cần người duyệt**. Chi tiết: `").append(csvName)
                .append("`.\n\n- FINQUERY: agent + RAG phân cấp\n- RAG_CO_BAN: nguyên câu hỏi -> vector top 20, cùng bước trả lời\n\n")
                .append("## 1. Tổng hợp\n\n| Cấu hình | Đúng | Một phần | Sai | Điểm (đúng 1, một phần 0,5) | Từ chối đúng | Trích đúng trang vàng | Câu trích ✓ | Số không nguồn | Thời gian TB ± ĐLC | p50 | p95 | Token vào / ra TB | USD/câu |\n")
                .append("|---|---|---|---|---|---|---|---|---|---|---|---|---|---|\n");
        for (String c : configs) {
            List<Row> rs = rows.stream().filter(r -> r.get("cau_hinh").equals(c)).toList();
            if (rs.isEmpty()) {
                continue;
            }
            List<Row> judged = rs.stream().filter(r -> !REFUSALS.contains(r.get("id"))).toList();
            long ok = count(judged, "correct");
            long part = count(judged, "partial");
            long bad = judged.size() - ok - part;
            List<Double> secs = rs.stream().map(r -> num(r.get("ms")) / 1000).toList();
            double mean = secs.stream().mapToDouble(Double::doubleValue).average().orElse(0);
            double sd = Math.sqrt(secs.stream().mapToDouble(x -> (x - mean) * (x - mean)).sum() / Math.max(1, secs.size() - 1));
            s.append(String.format("| %s | %d/%d | %d | %d | **%.1f/%d** | %d/%d | %d/%d | %.0f/%.0f | %.0f | %.1f ± %.1f s | %.1f s | %.1f s | %.0f / %.0f | %.4f |%n",
                    c, ok, judged.size(), part, bad, ok + part * 0.5, judged.size(),
                    rs.stream().filter(r -> r.get("danh_gia").equals("refuse_ok")).count(),
                    rs.stream().filter(r -> REFUSALS.contains(r.get("id"))).count(),
                    judged.stream().filter(r -> r.get("trung_trang_vang").equals("1")).count(), judged.size(),
                    sum(rs, "trich_dan_dung"), sum(rs, "trich_dan"), sum(rs, "so_khong_nguon"), mean, sd,
                    pct(secs, 50), pct(secs, 95), avg(rs, "token_vao"), avg(rs, "token_ra"), avg(rs, "usd")));
        }
        s.append("\n## 2. Theo nhóm câu (đúng / một phần / sai)\n\n| Nhóm | ").append(String.join(" | ", configs)).append(" |\n|---|")
                .append("---|".repeat(configs.size())).append('\n');
        for (String g : List.of("rag_van_ban", "rag_bang", "rag_nhieu_trang")) {
            s.append("| ").append(g);
            for (String c : configs) {
                List<Row> rs = rows.stream().filter(r -> r.get("cau_hinh").equals(c) && r.get("nhom").equals(g)).toList();
                s.append(" | ").append(count(rs, "correct")).append(" / ").append(count(rs, "partial")).append(" / ")
                        .append(count(rs, "wrong"));
            }
            s.append(" |\n");
        }
        s.append("\n## 3. Từng câu\n\n| Câu | Nhóm | ").append(String.join(" | ", configs)).append(" |\n|---|---|")
                .append("---|".repeat(configs.size())).append('\n');
        for (Map<String, Object> q : questions) {
            s.append("| ").append(q.get("id")).append(" | ").append(q.get("nhom"));
            for (String c : configs) {
                Row r = find(rows, c, (String) q.get("id"));
                s.append(" | ").append(r == null ? "" : mark(r.get("danh_gia")) + (r.get("trung_trang_vang").equals("1") ? " · trang vàng" : ""));
            }
            s.append(" |\n");
        }
        // danh sách cần duyệt: mọi câu sai / một phần + 5 câu đúng ngẫu nhiên (hạt giống cố định)
        s.append("\n## 4. Cần người duyệt\n");
        List<Row> review = new ArrayList<>(rows.stream().filter(r -> r.get("danh_gia").equals("wrong")
                || r.get("danh_gia").equals("partial") || r.get("danh_gia").equals("refuse_wrong")).toList());
        List<Row> correct = new ArrayList<>(rows.stream().filter(r -> r.get("danh_gia").equals("correct")).toList());
        Collections.shuffle(correct, new Random(42));
        review.addAll(correct.subList(0, Math.min(5, correct.size())));
        for (Row r : review) {
            Map<String, Object> q = questions.stream().filter(x -> x.get("id").equals(r.get("id"))).findFirst().orElse(Map.of());
            s.append("\n### ").append(r.get("id")).append(" · ").append(r.get("cau_hinh")).append(" · ").append(mark(r.get("danh_gia")))
                    .append("\n\n**Câu hỏi:** ").append(q.get("cau_hoi")).append("  \n**Đáp án vàng:** ").append(q.get("dap_an"))
                    .append("  \n**Lý do chấm:** ").append(r.get("ly_do")).append("  \n**Trang trích:** ").append(r.get("trang_trich"))
                    .append("\n\n").append(r.get("tra_loi").replace(" / ", "\n")).append('\n');
        }
        return s.toString();
    }

    private static String mark(String verdict) {
        return switch (verdict) {
            case "correct" -> "✓ đúng";
            case "partial" -> "◐ một phần";
            case "refuse_ok" -> "✓ từ chối đúng";
            case "refuse_wrong" -> "✗ từ chối sai";
            default -> "✗ sai";
        };
    }

    private static long count(List<Row> rs, String verdict) {
        return rs.stream().filter(r -> r.get("danh_gia").equals(verdict)).count();
    }

    private static double sum(List<Row> rs, String k) {
        return rs.stream().mapToDouble(r -> num(r.get(k))).sum();
    }

    private static double avg(List<Row> rs, String k) {
        return rs.stream().mapToDouble(r -> num(r.get(k))).average().orElse(0);
    }

    private static double pct(List<Double> xs, int p) {
        return RetrievalMetrics.percentile(xs.stream().map(x -> (long) (x * 1000)).toList(), p) / 1000.0;
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
