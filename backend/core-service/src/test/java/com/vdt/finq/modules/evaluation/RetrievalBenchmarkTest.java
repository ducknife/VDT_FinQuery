package com.vdt.finq.modules.evaluation;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.ToDoubleFunction;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.yaml.snakeyaml.Yaml;

import com.vdt.finq.common.properties.RerankProperties;
import com.vdt.finq.common.properties.RetrievalProperties;
import com.vdt.finq.modules.hierarchicalrag.retrieval.ChunkRef;
import com.vdt.finq.modules.hierarchicalrag.retrieval.RetrievalOptions;
import com.vdt.finq.modules.hierarchicalrag.retrieval.RetrievalResult;
import com.vdt.finq.modules.hierarchicalrag.retrieval.RetrievalService;
import com.vdt.finq.modules.hierarchicalrag.retrieval.RetrievedChunk;

// benchmark TRUY HỒI trên bộ câu hỏi data/eval/eval_test.yml, không gọi LLM.
// mỗi cấu hình chạy hết các câu có trang vàng; cấu hình chính chạy thêm câu từ chối (ghi điểm rerank cao nhất).
// kết quả: data/eval/ket-qua/benchmark-truy-hoi_<thời điểm>.md (bảng tổng hợp) và .csv (từng câu)
//
// cần MySQL + Qdrant; tốn ~0,1-0,2 USD mỗi cấu hình. chạy (PowerShell phải bọc ngoặc kép):
//     ./mvnw test "-Dtest=RetrievalBenchmarkTest" "-Dbenchmark=true"
//     ./mvnw test "-Dtest=RetrievalBenchmarkTest" "-Dbenchmark=true" "-Dbenchmark.configs=CHINH,KHONG_LOC"
// bị ngắt giữa chừng thì chạy tiếp vào đúng file cũ (bỏ qua các câu đã có):
//     ./mvnw test "-Dtest=RetrievalBenchmarkTest" "-Dbenchmark=true" "-Dbenchmark.resume=../../data/eval/ket-qua/<file>.csv"
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = { "cleaner.trial.enabled=false", "spring.devtools.restart.enabled=false" })
@EnabledIfSystemProperty(named = "benchmark", matches = "true")
class RetrievalBenchmarkTest {

    private static final Path QUESTIONS = Path.of("../../data/eval/eval_test.yml");
    private static final Path OUT_DIR = Path.of("../../data/eval/ket-qua");
    private static final String MAIN = "CHINH";
    private static final String MODEL_4B = "Qwen/Qwen3-Reranker-4B";
    private static final String MODEL_8B = "Qwen/Qwen3-Reranker-8B";

    @Autowired
    private RetrievalService retrievalService;
    @Autowired
    private RerankProperties rerankProperties;
    @Autowired
    private RetrievalProperties retrievalProperties;

    // một cấu hình cần so; model null = giữ model trong .env
    private record Config(String name, String description, boolean keywords, boolean filter, boolean rerank, boolean normalize,
            String tier1Model, String tier2Model, int topK, int tier1Keep, int finalK) {
    }

    // một câu hỏi đọc từ YAML
    private record Question(String id, String group, String text, List<String> tags, Map<String, Integer> gold,
            Map<String, Set<Integer>> groupsOfPage, int evidenceCount, boolean needAllEvidence, Set<String> traps) {
    }

    // kết quả một câu dưới một cấu hình
    private record Row(String config, Question q, boolean r80, boolean r40, boolean r20, Boolean all20, double ndcg20,
            double ndcgEv, double rr, int firstRank, Boolean fooled, String filterMode, int qdrantHits, Double topScore, long totalMs,
            long rerankMs, double cost, String top1, String error) {
    }

    @Test
    void runBenchmark() throws Exception {
        List<Question> questions = loadQuestions();
        List<Config> configs = selectConfigs();
        String originalTier1 = rerankProperties.getTier1Model();
        String originalTier2 = rerankProperties.getTier2Model();
        int[] originalSizes = { retrievalProperties.getTopK(), retrievalProperties.getTier1Keep(),
                retrievalProperties.getFinalK() };

        Files.createDirectories(OUT_DIR);
        List<Row> rows = new ArrayList<>();
        Path csv;
        String stamp;
        // chạy tiếp một lượt bị ngắt: -Dbenchmark.resume=<file csv>; các câu đã có trong file thì bỏ qua
        String resume = System.getProperty("benchmark.resume", "");
        if (!resume.isBlank()) {
            csv = Path.of(resume);
            rows.addAll(readRows(csv, questions));
            stamp = csv.getFileName().toString().replace("benchmark-truy-hoi_", "").replace(".csv", "");
            System.out.println("chạy tiếp " + csv + ": đã có " + rows.size() + " dòng");
        } else {
            stamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmm"));
            csv = OUT_DIR.resolve("benchmark-truy-hoi_" + stamp + ".csv");
            Files.writeString(csv, csvHeader(), StandardCharsets.UTF_8);
        }
        Set<String> done = new HashSet<>();
        rows.forEach(r -> done.add(r.config() + "|" + r.q().id()));

        int consecutiveErrors = 0;
        try {
            for (Config c : configs) {
                apply(c, originalTier1, originalTier2);
                for (Question q : questions) {
                    // câu từ chối không có trang vàng: chỉ chạy ở cấu hình chính để ghi điểm rerank cao nhất
                    if (q.gold().isEmpty() && !c.name().equals(MAIN) || done.contains(c.name() + "|" + q.id())) {
                        continue;
                    }
                    Row row = runOne(c, q);
                    rows.add(row);
                    Files.writeString(csv, csvLine(row), StandardCharsets.UTF_8, java.nio.file.StandardOpenOption.APPEND);
                    System.out.printf("[%s] %s nDCG@20=%.3f R@20=%s %d ms%n", c.name(), q.id(), row.ndcg20(), row.r20(),
                            row.totalMs());
                    // Qdrant hoặc API hỏng thì dừng ngay, không chạy tiếp ra cả bảng số 0
                    consecutiveErrors = row.error() == null ? 0 : consecutiveErrors + 1;
                    if (consecutiveErrors >= 3) {
                        throw new IllegalStateException("3 câu liên tiếp lỗi, dừng benchmark: " + row.error());
                    }
                }
            }
        } finally {
            rerankProperties.setTier1Model(originalTier1);
            rerankProperties.setTier2Model(originalTier2);
            retrievalProperties.setTopK(originalSizes[0]);
            retrievalProperties.setTier1Keep(originalSizes[1]);
            retrievalProperties.setFinalK(originalSizes[2]);
        }

        // báo cáo gồm MỌI cấu hình có dữ liệu trong file (chạy tiếp một cấu hình không làm mất các cấu hình cũ)
        Set<String> present = new HashSet<>();
        rows.forEach(r -> present.add(r.config()));
        List<Config> reported = allConfigs(originalTier1, originalTier2).stream()
                .filter(c -> present.contains(c.name())).toList();
        Path md = OUT_DIR.resolve("benchmark-truy-hoi_" + stamp + ".md");
        Files.writeString(md, report(reported, rows, originalTier1, originalTier2, csv.getFileName().toString()),
                StandardCharsets.UTF_8);
        System.out.println("đã ghi " + md.toAbsolutePath());
    }

    private List<Config> selectConfigs() {
        List<Config> all = allConfigs(rerankProperties.getTier1Model(), rerankProperties.getTier2Model());
        String wanted = System.getProperty("benchmark.configs", "");
        if (wanted.isBlank()) {
            return all;
        }
        Set<String> names = new HashSet<>(Arrays.asList(wanted.split(",")));
        names.add(MAIN);
        return all.stream().filter(c -> names.contains(c.name())).toList();
    }

    private static List<Config> allConfigs(String t1, String t2) {
        return List.of(
                new Config(MAIN, "từ khoá + bộ lọc + rerank 2 tầng, 80 → 40 → 20", true, true, true, true, t1, t2, 80, 40, 20),
                new Config("KHONG_LOC", "tắt bộ lọc metadata", true, false, true, true, t1, t2, 80, 40, 20),
                new Config("KHONG_TU_KHOA", "tắt trích từ khoá", false, true, true, true, t1, t2, 80, 40, 20),
                new Config("KHONG_RERANK", "chỉ tìm vector, lấy 20 chunk đầu", true, true, false, true, t1, t2, 80, 40, 20),
                new Config("MOT_TANG_4B", "rerank 1 tầng: 4B chấm cả 80 chunk", true, true, true, true, "", MODEL_4B, 80, 40, 20),
                new Config("RUT_GON", "rút gọn 50 → 20 → 10", true, true, true, true, t1, t2, 50, 20, 10),
                new Config("B_4B_8B", "rerank 2 tầng 4B → 8B", true, true, true, true, MODEL_4B, MODEL_8B, 80, 40, 20),
                // đối chứng RAG cơ bản (pham-vi_v1.0): nguyên câu hỏi -> vector top 20; không chuẩn hoá, từ khoá, lọc, rerank
                new Config("RAG_CO_BAN", "RAG cơ bản: nguyên câu hỏi, vector top 20, không lọc, không rerank",
                        false, false, false, false, t1, t2, 80, 40, 20));
    }

    private void apply(Config c, String originalTier1, String originalTier2) {
        rerankProperties.setTier1Model(c.tier1Model() == null ? originalTier1 : c.tier1Model());
        rerankProperties.setTier2Model(c.tier2Model() == null ? originalTier2 : c.tier2Model());
        retrievalProperties.setTopK(c.topK());
        retrievalProperties.setTier1Keep(c.tier1Keep());
        retrievalProperties.setFinalK(c.finalK());
    }

    private Row runOne(Config c, Question q) {
        try {
            RetrievalResult r = retrievalService.retrieve(q.text(),
                    new RetrievalOptions(c.keywords(), c.filter(), c.rerank(), c.normalize()));
            List<ChunkRef> finalRefs = r.getChunks().stream().map(ChunkRef::of).toList();
            List<String> p80 = RetrievalMetrics.topPages(r.getQdrantRanking(), c.topK());
            List<String> p40 = RetrievalMetrics.topPages(r.getTier1Ranking(), c.tier1Keep());
            List<String> p20 = RetrievalMetrics.topPages(finalRefs, 20);
            Double top = r.getChunks().stream()
                    .map(ch -> ch.getTier2Score() != null ? ch.getTier2Score() : ch.getTier1Score())
                    .filter(s -> s != null).max(Double::compare).orElse(null);
            String top1 = r.getChunks().isEmpty() ? "" : describe(r.getChunks().get(0));
            return new Row(c.name(), q,
                    RetrievalMetrics.hit(p80, q.gold()),
                    RetrievalMetrics.hit(p40, q.gold()),
                    RetrievalMetrics.hit(p20, q.gold()),
                    q.needAllEvidence() ? RetrievalMetrics.allEvidence(p20, q.groupsOfPage(), q.evidenceCount()) : null,
                    q.gold().isEmpty() ? 0 : RetrievalMetrics.ndcg(p20, q.gold(), 20),
                    q.gold().isEmpty() ? 0 : RetrievalMetrics.ndcgByEvidence(p20, q.gold(), q.groupsOfPage(),
                            q.evidenceCount(), 20),
                    RetrievalMetrics.reciprocalRank(p20, q.gold()),
                    RetrievalMetrics.firstGoldRank(p20, q.gold()),
                    q.traps().isEmpty() ? null : RetrievalMetrics.fooled(p20, q.gold(), q.traps()),
                    r.getFilterMode(), r.getQdrantHits(), top,
                    r.getTimingsMs().getOrDefault("total", 0L), r.getTimingsMs().getOrDefault("rerank", 0L),
                    r.getRerankCost(), top1, null);
        } catch (RuntimeException e) {
            return new Row(c.name(), q, false, false, false, null, 0, 0, 0, 0, null, "", 0, null, 0, 0, 0, "",
                    e.getMessage());
        }
    }

    private static String describe(RetrievedChunk c) {
        return (c.getTenHienThi() == null ? c.getTaiLieuId() : c.getTenHienThi()) + " tr." + c.getTrang();
    }

    // ---------------------------------------------------------------- đọc bộ câu hỏi

    @SuppressWarnings("unchecked")
    private static List<Question> loadQuestions() throws Exception {
        List<Question> out = new ArrayList<>();
        try (InputStream in = Files.newInputStream(QUESTIONS)) {
            List<Map<String, Object>> items = new Yaml().load(in);
            for (Map<String, Object> m : items) {
                Map<String, Integer> gold = new LinkedHashMap<>();
                Map<String, Set<Integer>> groups = new HashMap<>();
                for (Map<String, Object> g : (List<Map<String, Object>>) m.getOrDefault("trang_vang", List.of())) {
                    String key = RetrievalMetrics.pageKey((String) g.get("tai_lieu_id"), (Integer) g.get("trang"));
                    gold.put(key, (Integer) g.get("muc"));
                    groups.put(key, new HashSet<>((List<Integer>) g.getOrDefault("bang_chung_so", List.of(0))));
                }
                Set<String> traps = new HashSet<>();
                for (Map<String, Object> t : (List<Map<String, Object>>) m.getOrDefault("trang_de_nham", List.of())) {
                    traps.add(RetrievalMetrics.pageKey((String) t.get("tai_lieu_id"), (Integer) t.get("trang")));
                }
                int evidence = ((List<Object>) m.getOrDefault("bang_chung", List.of())).size();
                out.add(new Question((String) m.get("id"), (String) m.get("nhom"), (String) m.get("cau_hoi"),
                        (List<String>) m.getOrDefault("bien_the", List.of()), gold, groups, evidence,
                        Boolean.TRUE.equals(m.get("can_du_moi_bang_chung")), traps));
            }
        }
        return out;
    }

    // ---------------------------------------------------------------- CSV

    // đọc lại các dòng đã ghi (để chạy tiếp); câu không còn trong bộ câu hỏi thì bỏ
    private static List<Row> readRows(Path csv, List<Question> questions) throws Exception {
        Map<String, Question> byId = new HashMap<>();
        questions.forEach(q -> byId.put(q.id(), q));
        List<String> lines = Files.readAllLines(csv, StandardCharsets.UTF_8);
        List<String> header = splitCsv(lines.get(0));
        List<Row> out = new ArrayList<>();
        for (String line : lines.subList(1, lines.size())) {
            if (line.isBlank()) {
                continue;
            }
            List<String> cells = splitCsv(line);
            Map<String, String> v = new HashMap<>();
            for (int i = 0; i < header.size() && i < cells.size(); i++) {
                v.put(header.get(i), cells.get(i));
            }
            Question q = byId.get(v.get("id"));
            if (q == null) {
                continue;
            }
            out.add(new Row(v.get("cau_hinh"), q, "1".equals(v.get("recall80")), "1".equals(v.get("recall40")),
                    "1".equals(v.get("recall20")), bool(v.get("du_bang_chung20")), dbl(v.get("ndcg20")),
                    dbl(v.get("ndcg20_bang_chung")), dbl(v.get("rr")), (int) dbl(v.get("hang_trang_dung")),
                    bool(v.get("bi_lua")), v.getOrDefault("che_do_loc", ""), (int) dbl(v.get("qdrant_hits")),
                    blank(v.get("diem_rerank_cao_nhat")) ? null : dbl(v.get("diem_rerank_cao_nhat")),
                    (long) dbl(v.get("tong_ms")), (long) dbl(v.get("rerank_ms")), dbl(v.get("chi_phi_usd")),
                    v.getOrDefault("hang1", ""), blank(v.get("loi")) ? null : v.get("loi")));
        }
        return out;
    }

    // tách một dòng CSV do csvLine ghi (ô chữ bọc ngoặc kép, không có ngoặc kép bên trong)
    private static List<String> splitCsv(String line) {
        List<String> out = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean quoted = false;
        for (char ch : line.toCharArray()) {
            if (ch == '"') {
                quoted = !quoted;
            } else if (ch == ',' && !quoted) {
                out.add(cur.toString());
                cur.setLength(0);
            } else {
                cur.append(ch);
            }
        }
        out.add(cur.toString());
        return out;
    }

    private static boolean blank(String s) {
        return s == null || s.isBlank();
    }

    private static double dbl(String s) {
        return blank(s) ? 0 : Double.parseDouble(s);
    }

    private static Boolean bool(String s) {
        return blank(s) ? null : "1".equals(s);
    }

    private static String csvHeader() {
        return "cau_hinh,id,nhom,bien_the,recall80,recall40,recall20,du_bang_chung20,ndcg20,ndcg20_bang_chung,rr,hang_trang_dung,"
                + "bi_lua,che_do_loc,qdrant_hits,diem_rerank_cao_nhat,tong_ms,rerank_ms,chi_phi_usd,hang1,loi\n";
    }

    private static String csvLine(Row r) {
        return String.join(",", r.config(), r.q().id(), r.q().group(), q(String.join(" ", r.q().tags())),
                b(r.r80()), b(r.r40()), b(r.r20()), r.all20() == null ? "" : b(r.all20()),
                f(r.ndcg20()), f(r.ndcgEv()), f(r.rr()), String.valueOf(r.firstRank()), r.fooled() == null ? "" : b(r.fooled()),
                r.filterMode(), String.valueOf(r.qdrantHits()), r.topScore() == null ? "" : f(r.topScore()),
                String.valueOf(r.totalMs()), String.valueOf(r.rerankMs()), String.format(Locale.ROOT, "%.6f", r.cost()),
                q(r.top1()), q(r.error() == null ? "" : r.error())) + "\n";
    }

    private static String b(boolean v) {
        return v ? "1" : "0";
    }

    private static String f(double v) {
        return String.format(Locale.ROOT, "%.4f", v);
    }

    private static String q(String s) {
        return "\"" + s.replace("\"", "'") + "\"";
    }

    // ---------------------------------------------------------------- báo cáo markdown

    private static String report(List<Config> configs, List<Row> rows, String t1, String t2, String csvName) {
        StringBuilder md = new StringBuilder();
        long scored = rows.stream().filter(r -> r.config().equals(MAIN) && !r.q().gold().isEmpty()).count();
        md.append("# Benchmark truy hồi — ").append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")))
                .append("\n\nBộ câu hỏi: `data/eval/eval_test.yml` (").append(scored)
                .append(" câu có trang vàng; câu từ chối chỉ ghi điểm rerank). Model trong `.env`: tầng 1 `").append(t1)
                .append("`, tầng 2 `").append(t2).append("`. Chi tiết từng câu: `").append(csvName).append("`.\n\n")
                .append("Chỉ số tính theo trang (k chunk đầu đổi về trang, mỗi trang tính một lần). Recall@k = có ít nhất "
                        + "một trang vàng trong k; nDCG@20 có tính mức 2/1; MRR = 1/hạng trang vàng đầu tiên; "
                        + "Đủ bằng chứng = câu nhiều trang lấy đủ mọi bằng chứng trong 20; Bị lừa = trang dễ nhầm xếp trên "
                        + "trang vàng. nDCG@20 theo bằng chứng: mỗi bằng chứng chỉ cần một trang (cùng con số in lại ở "
                        + "nhiều báo cáo thì một trang là đủ trả lời), không phạt việc không lấy hết các bản in lại.\n\n");

        md.append("## 1. Tổng hợp theo cấu hình\n\n")
                .append("| Cấu hình | Mô tả | n | Recall@Qdrant | Recall@tầng 1 | Recall@20 | nDCG@20 | nDCG@20 theo bằng chứng "
                        + "| MRR | Đủ bằng chứng | Bị lừa | p50 ms | p95 ms | USD/câu | Lỗi |\n")
                .append("|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|\n");
        for (Config c : configs) {
            List<Row> rs = scoredRows(rows, c.name());
            if (rs.isEmpty()) {
                continue;
            }
            List<Long> ms = rs.stream().filter(r -> r.error() == null).map(Row::totalMs).toList();
            md.append("| ").append(c.name()).append(" | ").append(c.description()).append(" | ").append(rs.size())
                    .append(" | ").append(pct(rs, r -> r.r80() ? 1 : 0))
                    .append(" | ").append(pct(rs, r -> r.r40() ? 1 : 0))
                    .append(" | ").append(pct(rs, r -> r.r20() ? 1 : 0))
                    .append(" | ").append(avg(rs, Row::ndcg20))
                    .append(" | ").append(avg(rs, Row::ndcgEv))
                    .append(" | ").append(avg(rs, Row::rr))
                    .append(" | ").append(pctNullable(rs, Row::all20))
                    .append(" | ").append(pctNullable(rs, Row::fooled))
                    .append(" | ").append(RetrievalMetrics.percentile(ms, 50))
                    .append(" | ").append(RetrievalMetrics.percentile(ms, 95))
                    .append(" | ").append(String.format(Locale.ROOT, "%.4f", rs.stream().mapToDouble(Row::cost).average().orElse(0)))
                    .append(" | ").append(rs.stream().filter(r -> r.error() != null).count()).append(" |\n");
        }

        md.append("\n## 2. So với cấu hình chính (bootstrap ghép cặp, 10.000 lần, khoảng tin cậy 95%)\n\n")
                .append("Khoảng không chứa 0 thì chênh lệch đáng tin; chứa 0 thì chưa kết luận được với 50 câu.\n\n")
                .append("| Cấu hình | Δ nDCG@20 | KTC 95% | Δ nDCG@20 theo bằng chứng | KTC 95% | Δ Recall@20 | KTC 95% |\n")
                .append("|---|---|---|---|---|---|---|\n");
        Map<String, Row> mainById = new HashMap<>();
        scoredRows(rows, MAIN).forEach(r -> mainById.put(r.q().id(), r));
        for (Config c : configs) {
            if (c.name().equals(MAIN)) {
                continue;
            }
            List<Row> rs = scoredRows(rows, c.name()).stream().filter(r -> mainById.containsKey(r.q().id())).toList();
            if (rs.isEmpty()) {
                continue;
            }
            double[] dn = rs.stream().mapToDouble(r -> r.ndcg20() - mainById.get(r.q().id()).ndcg20()).toArray();
            double[] dr = rs.stream()
                    .mapToDouble(r -> (r.r20() ? 1 : 0) - (mainById.get(r.q().id()).r20() ? 1 : 0)).toArray();
            double[] de = rs.stream().mapToDouble(r -> r.ndcgEv() - mainById.get(r.q().id()).ndcgEv()).toArray();
            double[] cn = RetrievalMetrics.bootstrapMeanCi(dn, 10_000, 42);
            double[] ce = RetrievalMetrics.bootstrapMeanCi(de, 10_000, 42);
            double[] cr = RetrievalMetrics.bootstrapMeanCi(dr, 10_000, 42);
            md.append("| ").append(c.name()).append(" | ").append(signed(cn[0])).append(" | [")
                    .append(signed(cn[1])).append("; ").append(signed(cn[2])).append("] | ")
                    .append(signed(ce[0])).append(" | [").append(signed(ce[1])).append("; ").append(signed(ce[2]))
                    .append("] | ")
                    .append(signed(cr[0])).append(" | [").append(signed(cr[1])).append("; ").append(signed(cr[2]))
                    .append("] |\n");
        }

        List<Row> main = scoredRows(rows, MAIN);
        md.append("\n## 3. Cấu hình chính theo nhóm câu\n\n")
                .append("| Nhóm | n | Recall@20 | nDCG@20 | nDCG@20 theo bằng chứng | MRR |\n|---|---|---|---|---|---|\n");
        Map<String, List<Row>> byGroup = new TreeMap<>();
        main.forEach(r -> byGroup.computeIfAbsent(r.q().group(), k -> new ArrayList<>()).add(r));
        byGroup.forEach((g, rs) -> md.append("| ").append(g).append(" | ").append(rs.size()).append(" | ")
                .append(pct(rs, r -> r.r20() ? 1 : 0)).append(" | ").append(avg(rs, Row::ndcg20)).append(" | ")
                .append(avg(rs, Row::ndcgEv)).append(" | ").append(avg(rs, Row::rr)).append(" |\n"));

        md.append("\n## 4. Cấu hình chính theo ca biên\n\n")
                .append("| Ca biên | n | Recall@20 | nDCG@20 | nDCG@20 theo bằng chứng |\n|---|---|---|---|---|\n");
        Map<String, List<Row>> byTag = new TreeMap<>();
        main.forEach(r -> r.q().tags().forEach(t -> byTag.computeIfAbsent(t, k -> new ArrayList<>()).add(r)));
        byTag.forEach((t, rs) -> md.append("| ").append(t).append(" | ").append(rs.size()).append(" | ")
                .append(pct(rs, r -> r.r20() ? 1 : 0)).append(" | ").append(avg(rs, Row::ndcg20)).append(" | ")
                .append(avg(rs, Row::ndcgEv)).append(" |\n"));

        md.append("\n## 5. Câu cấu hình chính trượt (không có trang vàng trong 20) hoặc trang vàng xếp sau hạng 5\n\n")
                .append("| Câu | Nhóm | Câu hỏi | Hạng trang vàng | Có ở Qdrant? | Có sau tầng 1? | Hạng 1 là | Lọc |\n")
                .append("|---|---|---|---|---|---|---|---|\n");
        main.stream().filter(r -> r.firstRank() == 0 || r.firstRank() > 5).forEach(r -> md.append("| ")
                .append(r.q().id()).append(" | ").append(r.q().group()).append(" | ").append(cell(r.q().text()))
                .append(" | ").append(r.firstRank() == 0 ? "—" : r.firstRank()).append(" | ").append(r.r80() ? "có" : "KHÔNG")
                .append(" | ").append(r.r40() ? "có" : "KHÔNG").append(" | ").append(cell(r.top1())).append(" | ")
                .append(r.filterMode()).append(r.error() == null ? "" : " (lỗi: " + cell(r.error()) + ")").append(" |\n"));

        md.append("\n## 6. Điểm rerank cao nhất: câu từ chối so với câu trả lời được (cấu hình chính)\n\n")
                .append("Nếu câu từ chối có điểm cao nhất thấp hơn rõ rệt thì dùng được làm ngưỡng từ chối.\n\n")
                .append("| Câu | Nhóm | Điểm cao nhất |\n|---|---|---|\n");
        rows.stream().filter(r -> r.config().equals(MAIN) && r.q().group().equals("tu_choi"))
                .forEach(r -> md.append("| ").append(r.q().id()).append(" | ").append(r.q().group()).append(" | ")
                        .append(r.topScore() == null ? "—" : f(r.topScore())).append(" |\n"));
        List<Double> answerable = main.stream().filter(r -> !r.q().group().equals("tu_choi") && r.topScore() != null)
                .map(Row::topScore).sorted().toList();
        if (!answerable.isEmpty()) {
            md.append("| (câu trả lời được) | thấp nhất / trung vị | ").append(f(answerable.get(0))).append(" / ")
                    .append(f(answerable.get(answerable.size() / 2))).append(" |\n");
        }
        return md.toString();
    }

    private static List<Row> scoredRows(List<Row> rows, String config) {
        return rows.stream().filter(r -> r.config().equals(config) && !r.q().gold().isEmpty()).toList();
    }

    private static String pct(List<Row> rs, ToDoubleFunction<Row> f) {
        return String.format(Locale.ROOT, "%.1f%%", 100 * rs.stream().mapToDouble(f).average().orElse(0)).replace('.', ',');
    }

    private static String pctNullable(List<Row> rs, java.util.function.Function<Row, Boolean> f) {
        List<Boolean> vs = rs.stream().map(f).filter(v -> v != null).toList();
        if (vs.isEmpty()) {
            return "—";
        }
        long yes = vs.stream().filter(Boolean::booleanValue).count();
        return yes + "/" + vs.size();
    }

    private static String avg(List<Row> rs, ToDoubleFunction<Row> f) {
        return String.format(Locale.ROOT, "%.3f", rs.stream().mapToDouble(f).average().orElse(0)).replace('.', ',');
    }

    private static String signed(double v) {
        return String.format(Locale.ROOT, "%+.3f", v).replace('.', ',');
    }

    private static String cell(String s) {
        return s == null ? "" : s.replace("|", "\\|").replace("\n", " ");
    }
}
