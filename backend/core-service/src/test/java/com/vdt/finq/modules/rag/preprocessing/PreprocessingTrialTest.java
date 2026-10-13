package com.vdt.finq.modules.rag.preprocessing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vdt.finq.common.properties.EmbeddingProperties;
import com.vdt.finq.common.properties.IndexingProperties;
import com.vdt.finq.common.properties.IndexingProperties.TableFormat;
import com.vdt.finq.modules.document.Document;
import com.vdt.finq.modules.document.DocumentEnums.CheDo;
import com.vdt.finq.modules.document.DocumentEnums.Ky;
import com.vdt.finq.modules.document.DocumentEnums.LoaiTaiLieu;
import com.vdt.finq.modules.document.DocumentEnums.PhamVi;
import com.vdt.finq.modules.financial.FinancialCatalog;
import com.vdt.finq.modules.financial.extraction.DocumentFigureExtractor;
import com.vdt.finq.modules.financial.extraction.ExtractionIssue;
import com.vdt.finq.modules.financial.extraction.FigureCandidate;
import com.vdt.finq.modules.rag.preprocessing.datacleaner.NFCNormalization;
import com.vdt.finq.modules.rag.preprocessing.datacleaner.TextCleaner;
import com.vdt.finq.modules.rag.preprocessing.indexing.chunker.Chunk;
import com.vdt.finq.modules.rag.preprocessing.indexing.chunker.Chunking;
import com.vdt.finq.modules.rag.preprocessing.indexing.chunker.stitcher.TableStitcher;
import com.vdt.finq.modules.rag.preprocessing.indexing.chunker.stitcher.TextStitcher;
import com.vdt.finq.modules.rag.preprocessing.indexing.embedder.EmbeddedChunk;
import com.vdt.finq.modules.rag.preprocessing.indexing.embedder.Embedding;
import com.vdt.finq.modules.rag.preprocessing.indexing.embedder.EmbeddingClient;
import com.vdt.finq.modules.rag.preprocessing.indexing.vectordb.DocumentMetadata;
import com.vdt.finq.modules.rag.preprocessing.indexing.vectordb.QdrantIndexWriter;
import com.vdt.finq.modules.rag.preretrieval.normalizer.FinancialTextNormalizer;
import com.vdt.finq.utils.TableToJson;

import io.qdrant.client.grpc.JsonWithInt.Value;

// chạy thử tiền xử lý (trừ OCR) trên vài trang, ghi từng bước ra trial-preprocessing.md, không ghi DB:
//     ./mvnw test -Dtest=PreprocessingTrialTest [-Dtrial.embed=true]
class PreprocessingTrialTest {

    // BCTC HN quý 4/2023: B01 vắt trang 5-6, B02 trang 7-8 (trang 8 mất tiêu đề), B03 trang 9
    private static final String DUONG_DAN = "01_bao_cao_tai_chinh/2023/hop_nhat/20240130-CTR-BCTC-HN-Quý-4.2023";
    private static final int FIRST_PAGE = 5;
    private static final int LAST_PAGE = 10;

    private static final Path OCR_ROOT = Path.of("../../data/ocr");
    private static final Path REPORT = Path.of("trial-preprocessing.md");

    private final IndexingProperties indexing = new IndexingProperties(4000, 500, 50000, TableFormat.JSON, false);
    private final TextCleaner cleaner = new TextCleaner(new NFCNormalization());
    private final TableStitcher tableStitcher = new TableStitcher();
    private final Chunking chunking = new Chunking(indexing, tableStitcher, new TextStitcher(indexing));
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final TableToJson tableToJson = new TableToJson(objectMapper);

    private final StringBuilder out = new StringBuilder();

    @Test
    void runWholePipelineExceptOcr() throws Exception {
        Map<String, String> env = readEnv();
        JdbcTemplate jdbc = jdbc(env);
        assumeTrue(jdbc != null, "MySQL không chạy hoặc thiếu .env");
        Document document = loadDocument(jdbc);
        assumeTrue(document != null && Files.isDirectory(OCR_ROOT.resolve(document.ocrRelativePath())),
                "không có tài liệu mẫu trên máy này");

        out.append("# Chạy thử luồng tiền xử lý (trừ OCR)\n\n");
        out.append("Tài liệu `").append(document.getTaiLieuId()).append("`: ").append(document.getTenHienThi())
                .append(", trang ").append(FIRST_PAGE).append("–").append(LAST_PAGE).append(".\n");
        out.append("Không ghi gì vào MySQL hay Qdrant. Sinh bởi `PreprocessingTrialTest`.\n\n");

        // 1-2. đọc trang OCR và làm sạch; trang ngoài khoảng để rỗng cho đúng số trang
        List<String> raw = new ArrayList<>();
        List<String> clean = new ArrayList<>();
        section("1. Đọc trang OCR và làm sạch (`TextCleaner`)");
        out.append("| Trang | Ký tự gốc | Sau làm sạch | Bỏ đi |\n|---|---|---|---|\n");
        for (int page = 1; page <= LAST_PAGE; page++) {
            String text = "";
            if (page >= FIRST_PAGE) {
                Path file = OCR_ROOT.resolve(document.ocrRelativePath()).resolve("trang_%03d.md".formatted(page));
                text = Files.exists(file) ? Files.readString(file, StandardCharsets.UTF_8) : "";
            }
            raw.add(text);
            String c = cleaner.clean(text);
            clean.add(c);
            if (page >= FIRST_PAGE) {
                out.append("| ").append(page).append(" | ").append(text.length()).append(" | ").append(c.length())
                        .append(" | ").append(text.length() - c.length()).append(" |\n");
            }
        }

        // 3. sửa tiêu đề bảng vắt trang (chunk và trích số dùng chung)
        List<String> repaired = tableStitcher.repairTableHeaders(clean);
        section("2. Sửa tiêu đề bảng vắt trang (`TableStitcher`)");
        for (int i = FIRST_PAGE - 1; i < LAST_PAGE; i++) {
            if (!repaired.get(i).equals(clean.get(i))) {
                out.append("Trang ").append(i + 1).append(" được chép thêm tiêu đề bảng từ trang ").append(i)
                        .append(". 12 dòng đầu sau khi sửa:\n\n```\n")
                        .append(firstLines(repaired.get(i), 12)).append("\n```\n\n");
            }
        }

        // 4. chunk
        List<Chunk> chunks = chunking.chunk(document.getTaiLieuId(), clean);
        section("3. Chunk (`Chunking`)");
        out.append("| # | Trang | Phần | Ký tự | Kiểu | Gối đầu |\n|---|---|---|---|---|---|\n");
        for (int i = 0; i < chunks.size(); i++) {
            Chunk c = chunks.get(i);
            out.append("| ").append(i + 1).append(" | ").append(c.getPage()).append(" | ").append(c.getPart())
                    .append(" | ").append(c.getText().length()).append(" | ")
                    .append(QdrantIndexWriter.elementType(c.getText()).getValue()).append(" | ")
                    .append(stitchLabels(c.getText())).append(" |\n");
        }
        for (int i = 0; i < chunks.size(); i++) {
            Chunk c = chunks.get(i);
            out.append("\n<details><summary>Chunk ").append(i + 1).append(" (trang ").append(c.getPage())
                    .append(", phần ").append(c.getPart()).append(")</summary>\n\n````markdown\n")
                    .append(c.getText()).append("\n````\n</details>\n");
        }

        // 5. văn bản đem embed: tên tài liệu + chunk, bảng đổi sang JSON
        section("4. Văn bản đem embed (`TableToJson` + tên tài liệu)");
        List<String> embedTexts = new ArrayList<>();
        out.append("| # | Markdown | Đem embed (JSON) | Tỉ lệ |\n|---|---|---|---|\n");
        for (int i = 0; i < chunks.size(); i++) {
            String text = Embedding.embedText(document.getTenHienThi(), tableToJson.convert(chunks.get(i).getText()));
            embedTexts.add(text);
            int md = chunks.get(i).getText().length();
            out.append("| ").append(i + 1).append(" | ").append(md).append(" | ").append(text.length())
                    .append(" | ").append("%.2f".formatted((double) text.length() / md)).append(" |\n");
        }
        int sample = firstTableChunk(chunks);
        out.append("\nVăn bản đem embed của chunk ").append(sample + 1).append(" (30 dòng đầu):\n\n```\n")
                .append(firstLines(embedTexts.get(sample), 30)).append("\n```\n");

        // 6. embedding: chỉ gọi API khi được bật
        section("5. Embedding (`Embedding` -> `EmbeddingClient`)");
        List<float[]> vectors = new ArrayList<>();
        if (Boolean.getBoolean("trial.embed") && env.containsKey("LLM_API_KEY")) {
            Embedding embedding = new Embedding(new EmbeddingClient(embeddingProperties(env), objectMapper),
                    embeddingProperties(env), indexing, tableToJson,
                    new FinancialTextNormalizer(new NFCNormalization()));
            for (EmbeddedChunk e : embedding.embedChunks(chunks, document.getTenHienThi())) {
                assertThat(e.getEmbedText()).isEqualTo(embedTexts.get(vectors.size()));
                vectors.add(e.getVector());
            }
            out.append("Model `").append(env.get("EMBEDDING_MODEL")).append("`, ").append(vectors.size())
                    .append(" vector.\n\n| # | Số chiều | Độ dài (norm) | 6 số đầu |\n|---|---|---|---|\n");
            for (int i = 0; i < vectors.size(); i++) {
                out.append("| ").append(i + 1).append(" | ").append(vectors.get(i).length).append(" | ")
                        .append("%.4f".formatted(norm(vectors.get(i)))).append(" | ").append(head(vectors.get(i)))
                        .append(" |\n");
            }
        } else {
            out.append("Bỏ qua (không gọi API). Chạy với `-Dtrial.embed=true` để embed thật.\n");
        }

        // 7. point Qdrant: id cố định + payload, đúng hàm mà QdrantIndexWriter dùng
        section("6. Point Qdrant (`QdrantIndexWriter.payloadOf`), chưa ghi");
        DocumentMetadata metadata = DocumentMetadata.from(document);
        for (int i = 0; i < chunks.size(); i++) {
            Chunk c = chunks.get(i);
            out.append("\n**Point ").append(i + 1).append("** `")
                    .append(QdrantIndexWriter.pointId(document.getTaiLieuId(), c.getPage(), c.getPart())).append("`");
            out.append(vectors.isEmpty() ? " (chưa có vector)\n\n" : " + vector " + vectors.get(i).length + " chiều\n\n");
            out.append("| Trường | Giá trị |\n|---|---|\n");
            QdrantIndexWriter.payloadOf(metadata, c).forEach((k, v) -> out.append("| ").append(k).append(" | ")
                    .append(render(k, v)).append(" |\n"));
        }

        // 8. trích số trên cùng các trang
        FinancialCatalog.Snapshot catalog = new FinancialCatalog(jdbc).load();
        DocumentFigureExtractor.Result result = DocumentFigureExtractor.extract(document,
                document.getCheDo().getValue(), repaired, catalog);
        section("7. Trích số (`DocumentFigureExtractor`), chưa ghi");
        out.append(result.getTables()).append(" bảng mẫu biểu, ").append(result.getCandidates().size())
                .append(" số qua hết các chốt, ").append(result.getIssues().size()).append(" ngoại lệ.\n\n");
        out.append("| Mẫu | Mã | Mã nội bộ | Kỳ | Giá trị | Trang | Cột |\n|---|---|---|---|---|---|---|\n");
        for (FigureCandidate f : result.getCandidates()) {
            out.append("| ").append(f.getMauBieu().getValue()).append(" | ").append(f.getMa()).append(" | ")
                    .append(f.getMaNoiBo()).append(" | ").append(f.getPeriod()).append(" | ")
                    .append("%,d".formatted(f.getGiaTri()).replace(',', '.')).append(" | ").append(f.getTrang())
                    .append(" | ").append(f.isCurrent() ? "kỳ này" : f.isRestated() ? "so sánh, đã điều chỉnh" : "so sánh")
                    .append(" |\n");
        }
        if (!result.getIssues().isEmpty()) {
            out.append("\nNgoại lệ:\n\n| Trang | Loại | Mã | Đọc được | Chi tiết |\n|---|---|---|---|---|\n");
            for (ExtractionIssue issue : result.getIssues()) {
                out.append("| ").append(issue.getTrang()).append(" | ").append(issue.getLoaiLoi().getValue())
                        .append(" | ").append(issue.getMa()).append(" | ").append(issue.getTenDocDuoc()).append(" | ")
                        .append(issue.getChiTiet()).append(" |\n");
            }
        }

        Files.writeString(REPORT, out.toString(), StandardCharsets.UTF_8);
        assertThat(chunks).isNotEmpty();
        assertThat(result.getCandidates()).isNotEmpty();
    }

    // ---- tiện ích ----

    private void section(String title) {
        out.append("\n## ").append(title).append("\n\n");
    }

    private static int firstTableChunk(List<Chunk> chunks) {
        for (int i = 0; i < chunks.size(); i++) {
            if (chunks.get(i).getText().contains("|")) {
                return i;
            }
        }
        return 0;
    }

    private static String stitchLabels(String text) {
        List<String> found = new ArrayList<>();
        Matcher m = Pattern.compile("\\((từ|tiếp) trang \\d+\\)").matcher(text);
        while (m.find()) {
            found.add(m.group());
        }
        return found.isEmpty() ? "" : String.join(", ", found);
    }

    private static String firstLines(String text, int n) {
        return String.join("\n", text.lines().limit(n).toList());
    }

    // noi_dung dài nên chỉ hiện độ dài
    private static String render(String key, Value v) {
        String s = switch (v.getKindCase()) {
            case STRING_VALUE -> v.getStringValue();
            case INTEGER_VALUE -> String.valueOf(v.getIntegerValue());
            case BOOL_VALUE -> String.valueOf(v.getBoolValue());
            default -> v.toString();
        };
        return "noi_dung".equals(key) ? "(" + s.length() + " ký tự, markdown)" : s;
    }

    private static double norm(float[] v) {
        double sum = 0;
        for (float x : v) {
            sum += x * x;
        }
        return Math.sqrt(sum);
    }

    private static String head(float[] v) {
        StringBuilder s = new StringBuilder();
        for (int i = 0; i < 6; i++) {
            s.append(i == 0 ? "" : ", ").append("%.4f".formatted(v[i]));
        }
        return s.toString();
    }

    private static EmbeddingProperties embeddingProperties(Map<String, String> env) {
        EmbeddingProperties p = new EmbeddingProperties();
        p.setApiUrl(env.get("EMBEDDING_API_URL"));
        p.setApiKey(env.get("LLM_API_KEY"));
        p.setModel(env.get("EMBEDDING_MODEL"));
        p.setDimensions(Integer.parseInt(env.getOrDefault("EMBEDDING_DIMENSIONS", "4096")));
        return p;
    }

    // đọc .env của core-service (cùng file Spring nạp lúc chạy thật)
    private static Map<String, String> readEnv() throws Exception {
        Map<String, String> env = new HashMap<>();
        Path file = Path.of(".env");
        if (!Files.exists(file)) {
            return env;
        }
        for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
            String s = line.strip();
            int eq = s.indexOf('=');
            if (s.isEmpty() || s.startsWith("#") || eq < 0) {
                continue;
            }
            env.put(s.substring(0, eq).strip(), s.substring(eq + 1).strip().replace("\"", ""));
        }
        return env;
    }

    private static JdbcTemplate jdbc(Map<String, String> env) {
        if (!env.containsKey("DB_URL")) {
            return null;
        }
        try {
            JdbcTemplate jdbc = new JdbcTemplate(
                    new DriverManagerDataSource(env.get("DB_URL"), env.get("DB_USERNAME"), env.get("DB_PASSWORD")));
            jdbc.queryForObject("SELECT 1", Integer.class);
            return jdbc;
        } catch (Exception e) {
            return null;
        }
    }

    private static Document loadDocument(JdbcTemplate jdbc) {
        List<Document> found = jdbc.query("SELECT tai_lieu_id, ma_cong_ty, ten_hien_thi, loai, nam, ky, ngay_ket_thuc_ky, "
                + "pham_vi, che_do, tong_so_trang, la_ban_chinh, duong_dan FROM documents WHERE duong_dan = ?",
                (rs, i) -> {
                    Document d = new Document();
                    d.setTaiLieuId(rs.getString("tai_lieu_id"));
                    d.setMaCongTy(rs.getString("ma_cong_ty"));
                    d.setTenHienThi(rs.getString("ten_hien_thi"));
                    d.setLoai(LoaiTaiLieu.fromValue(rs.getString("loai")));
                    d.setNam(rs.getInt("nam"));
                    d.setKy(Ky.fromValue(rs.getString("ky")));
                    d.setNgayKetThucKy(rs.getDate("ngay_ket_thuc_ky").toLocalDate());
                    d.setPhamVi(PhamVi.fromValue(rs.getString("pham_vi")));
                    d.setCheDo(CheDo.fromValue(rs.getString("che_do")));
                    d.setTongSoTrang(rs.getInt("tong_so_trang"));
                    d.setLaBanChinh(rs.getBoolean("la_ban_chinh"));
                    d.setDuongDan(rs.getString("duong_dan"));
                    return d;
                }, DUONG_DAN);
        return found.isEmpty() ? null : found.get(0);
    }
}
