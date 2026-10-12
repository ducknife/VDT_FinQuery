package com.vdt.finq.config.testrunner;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import com.vdt.finq.common.properties.OCRProperties;
import com.vdt.finq.modules.hierarchicalrag.preprocessing.datacleaner.TextCleaner;
import com.vdt.finq.modules.hierarchicalrag.preprocessing.indexing.chunker.Chunk;
import com.vdt.finq.modules.hierarchicalrag.preprocessing.indexing.chunker.Chunking;
import com.vdt.finq.modules.hierarchicalrag.preprocessing.indexing.embedder.EmbeddedChunk;
import com.vdt.finq.modules.hierarchicalrag.preprocessing.indexing.embedder.Embedding;
import com.vdt.finq.modules.hierarchicalrag.preprocessing.indexing.vectordb.QdrantIndexWriter;
import com.vdt.finq.modules.hierarchicalrag.preprocessing.ocr.PageStore;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

// thử embedding hai chunk gối đầu, không ghi gì; bật bằng embedding.trial.enabled=true
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "embedding.trial.enabled", havingValue = "true")
public class EmbeddingTrialRunner implements CommandLineRunner {

    // gối văn bản: câu "Vốn chủ sở hữu tăng | từ 867 tỷ lên 1.061 tỷ" bị cắt qua trang 2 -> 3
    private static final String TEXT_DOCUMENT = "03_dai_hoi_dong_co_dong/2021/bao_cao/04.-BC-BDH";
    private static final int TEXT_PAGE = 2;
    private static final String TEXT_MARKER = "(tiếp trang";
    private static final String TEXT_QUESTION = "Vốn chủ sở hữu năm 2020 tăng từ bao nhiêu lên bao nhiêu?";

    // gối tiêu đề bảng: trang 8 có dòng số liệu bị OCR coi là tiêu đề
    private static final String TABLE_DOCUMENT = "01_bao_cao_tai_chinh/2023/hop_nhat/20240130-CTR-BCTC-HN-Quý-4.2023";
    private static final int TABLE_PAGE = 8;
    private static final String TABLE_MARKER = "(tiêu đề bảng từ trang";
    private static final String TABLE_QUESTION = "Chi phí thuế thu nhập doanh nghiệp hiện hành quý 4 năm 2023 là bao nhiêu?";

    // tài liệu trong documents.duong_dan tính từ data/ocr/CTR
    private static final String CORPUS = "CTR";

    // thử một lô đầy đủ: tài liệu này có 33 chunk, lấy 32 chunk đầu cho đúng một lần gọi API
    private static final String BATCH_DOCUMENT = TABLE_DOCUMENT;
    private static final int BATCH_SIZE = 32;

    // ghi cùng chỗ với trial-cleaned.md (thư mục chạy ứng dụng)
    private static final Path BATCH_OUTPUT = Path.of("trial-embedding-32.txt");
    private static final String SEPARATOR = "=".repeat(80);

    private final OCRProperties ocrProperties;
    private final PageStore pageStore;
    private final TextCleaner textCleaner;
    private final Chunking chunking;
    private final Embedding embedding;
    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) throws Exception {
        log.info("=== embedding trial ===");
        Chunk textChunk = pickChunk(TEXT_DOCUMENT, TEXT_PAGE, TEXT_MARKER);
        Chunk tableChunk = pickChunk(TABLE_DOCUMENT, TABLE_PAGE, TABLE_MARKER);
        if (textChunk == null || tableChunk == null) {
            return;
        }

        // mỗi chunk gắn tên tài liệu riêng, nên gọi hai lần; thực tế mỗi tài liệu gọi theo lô 32
        EmbeddedChunk textResult = embedding.embedChunks(List.of(textChunk), titleOf(TEXT_DOCUMENT)).get(0);
        EmbeddedChunk tableResult = embedding.embedChunks(List.of(tableChunk), titleOf(TABLE_DOCUMENT)).get(0);

        report("GỐI VĂN BẢN", textResult, TEXT_MARKER);
        report("GỐI TIÊU ĐỀ BẢNG", tableResult, TABLE_MARKER);

        // vector có tìm đúng chunk không: câu hỏi nào phải giống chunk của nó hơn chunk kia
        log.info("--- độ giống (cosine) giữa câu hỏi và chunk ---");
        float[] q1 = embedding.embedQuery(TEXT_QUESTION);
        float[] q2 = embedding.embedQuery(TABLE_QUESTION);
        log.info("  \"{}\"", TEXT_QUESTION);
        log.info("      chunk văn bản {}   |   chunk bảng {}", score(q1, textResult), score(q1, tableResult));
        log.info("  \"{}\"", TABLE_QUESTION);
        log.info("      chunk văn bản {}   |   chunk bảng {}", score(q2, textResult), score(q2, tableResult));

        batchTrial();
    }

    // embed 32 chunk trong MỘT lần gọi, ghi từng vector kèm nhãn và metadata ra file
    private void batchTrial() throws Exception {
        log.info("");
        log.info("=== thử một lô {} chunk ===", BATCH_SIZE);
        List<String> pages = loadPages(CORPUS + "/" + BATCH_DOCUMENT);
        List<Chunk> chunks = chunking.chunk(BATCH_DOCUMENT, pages);
        List<Chunk> batch = chunks.subList(0, Math.min(BATCH_SIZE, chunks.size()));
        Map<String, Object> meta = documentMetadata(BATCH_DOCUMENT);
        String title = String.valueOf(meta.getOrDefault("ten_hien_thi", titleOf(BATCH_DOCUMENT)));

        long start = System.nanoTime();
        List<EmbeddedChunk> results = embedding.embedChunks(batch, title);
        long millis = (System.nanoTime() - start) / 1_000_000;

        StringBuilder out = new StringBuilder();
        out.append("THỬ EMBEDDING MỘT LÔ\n");
        out.append("tài liệu : ").append(BATCH_DOCUMENT).append('\n');
        out.append("số chunk : ").append(results.size()).append(" / ").append(chunks.size())
           .append(" chunk của tài liệu, gọi API 1 lần, ").append(millis).append(" ms\n");
        out.append("model    : vector ").append(results.isEmpty() ? 0 : results.get(0).getVector().length)
           .append(" chiều, nhận về dạng base64\n\n");

        for (int i = 0; i < results.size(); i++) {
            EmbeddedChunk r = results.get(i);
            Chunk c = r.getChunk();
            float[] v = r.getVector();

            out.append(SEPARATOR).append('\n');
            out.append("VECTOR ").append(i + 1).append(" / ").append(results.size())
               .append("   (trang ").append(c.getPage()).append(", phần ").append(c.getPart()).append(")\n");
            out.append(SEPARATOR).append('\n');

            // metadata: phần của chunk và phần lấy từ bảng documents (sẽ thành payload Qdrant)
            out.append("tai_lieu_id  : ").append(meta.getOrDefault("tai_lieu_id", "?")).append('\n');
            out.append("ten_hien_thi : ").append(title).append('\n');
            out.append("loai         : ").append(meta.getOrDefault("loai", "?"))
               .append(" | nam: ").append(meta.getOrDefault("nam", "?"))
               .append(" | ky: ").append(meta.getOrDefault("ky", "?"))
               .append(" | pham_vi: ").append(meta.getOrDefault("pham_vi", "?"))
               .append(" | la_ban_chinh: ").append(meta.getOrDefault("la_ban_chinh", "?")).append('\n');
            out.append("trang        : ").append(c.getPage()).append(" | phan: ").append(c.getPart())
               .append(" | element_type: ").append(elementType(c.getText())).append('\n');
            out.append("độ dài       : chunk ").append(c.getText().length())
               .append(" ký tự, văn bản đem embed ").append(r.getEmbedText().length()).append(" ký tự\n");
            out.append("gối đầu      : ").append(stitching(c.getText())).append('\n');
            out.append("nội dung     : ").append(preview(c.getText())).append('\n');
            out.append("vector       : ").append(v.length).append(" chiều, độ dài ")
               .append(String.format(Locale.ROOT, "%.4f", norm(v))).append('\n');
            out.append(formatVector(v)).append("\n\n");
        }
        Files.writeString(BATCH_OUTPUT, out.toString(), StandardCharsets.UTF_8);
        log.info("đã embed {} chunk trong 1 lần gọi ({} ms), ghi ra {}",
                results.size(), millis, BATCH_OUTPUT.toAbsolutePath().normalize());
    }

    // metadata của tài liệu trong bảng documents
    private Map<String, Object> documentMetadata(String document) {
        try {
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                    "SELECT tai_lieu_id, ten_hien_thi, loai, nam, ky, pham_vi, la_ban_chinh "
                    + "FROM documents WHERE duong_dan = ?", document);
            if (!rows.isEmpty()) {
                return rows.get(0);
            }
        } catch (Exception e) {
            log.warn("không đọc được bảng documents: {}", e.getMessage());
        }
        return Map.of();
    }

    // dùng chung cách phân loại với QdrantIndexWriter để report khớp payload thật
    private static String elementType(String text) {
        return QdrantIndexWriter.elementType(text).getValue();
    }

    // chunk có phần gối đầu nào
    private static String stitching(String text) {
        List<String> found = new ArrayList<>();
        if (text.contains("(từ trang")) {
            found.add("gối câu từ trang trước");
        }
        if (text.contains("(tiếp trang")) {
            found.add("gối câu từ trang sau");
        }
        if (text.contains("(tiêu đề bảng từ trang")) {
            found.add("gối tiêu đề bảng");
        }
        return found.isEmpty() ? "không" : String.join(", ", found);
    }

    // 200 ký tự đầu, gộp xuống dòng thành dấu cách cho dễ đọc
    private static String preview(String text) {
        String flat = text.replaceAll("\\s+", " ").strip();
        return flat.length() > 200 ? flat.substring(0, 200) + " ..." : flat;
    }

    // in đủ 2560 số, mỗi dòng 8 số
    private static String formatVector(float[] v) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < v.length; i++) {
            if (i > 0) {
                sb.append(i % 8 == 0 ? ",\n " : ", ");
            }
            sb.append(String.format(Locale.ROOT, "%10.7f", v[i]));
        }
        return sb.append(']').toString();
    }

    // đọc cả tài liệu, làm sạch, chunk, rồi lấy phần của trang cần xem có chứa dấu hiệu gối
    private Chunk pickChunk(String document, int page, String marker) throws Exception {
        List<String> pages = loadPages(CORPUS + "/" + document);
        if (pages.isEmpty()) {
            log.warn("không đọc được tài liệu {}", document);
            return null;
        }
        List<Chunk> chunks = chunking.chunk(document, pages);
        for (Chunk c : chunks) {
            if (c.getPage() == page && c.getText().contains(marker)) {
                log.info("{}: {} trang, {} chunk; chọn trang {} phần {}",
                        document, pages.size(), chunks.size(), c.getPage(), c.getPart());
                return c;
            }
        }
        log.warn("trang {} của {} không có dấu hiệu gối \"{}\"", page, document, marker);
        return null;
    }

    // các trang của một tài liệu, đã làm sạch; trang thiếu thay bằng chuỗi rỗng để giữ đúng số trang
    private List<String> loadPages(String relativePath) throws Exception {
        Path dir = Path.of(ocrProperties.getOutputDir()).resolve(relativePath);
        if (!Files.isDirectory(dir)) {
            return List.of();
        }
        int last;
        try (Stream<Path> files = Files.list(dir)) {
            last = files.map(p -> p.getFileName().toString())
                    .filter(n -> n.matches("trang_\\d+\\.md"))
                    .mapToInt(n -> Integer.parseInt(n.replaceAll("\\D", "")))
                    .max().orElse(0);
        }
        List<String> pages = new ArrayList<>();
        for (int n = 1; n <= last; n++) {
            pages.add(pageStore.read(relativePath, n).map(textCleaner::clean).orElse(""));
        }
        return pages;
    }

    // tên hiển thị của tài liệu trong bảng documents, không có thì dùng tên thư mục
    private String titleOf(String document) {
        try {
            List<String> names = jdbcTemplate.queryForList(
                    "SELECT ten_hien_thi FROM documents WHERE duong_dan = ?", String.class, document);
            if (!names.isEmpty()) {
                return names.get(0);
            }
        } catch (Exception e) {
            log.warn("không đọc được bảng documents: {}", e.getMessage());
        }
        return Path.of(document).getFileName().toString();
    }

    private void report(String title, EmbeddedChunk result, String marker) {
        Chunk c = result.getChunk();
        String text = result.getEmbedText();
        float[] v = result.getVector();
        List<Float> av = new ArrayList<>();
        for (float f : v) {
            av.add(f);
        }

        log.info("");
        log.info("=== {} ===", title);
        log.info("tài liệu {} | trang {} | phần {}", c.getDocumentId(), c.getPage(), c.getPart());
        log.info("văn bản đem embed: {} ký tự (nội dung chunk {} ký tự)", text.length(), c.getText().length());
        log.info("--- dòng đầu (tên tài liệu) ---");
        log.info("  {}", text.lines().findFirst().orElse(""));
        log.info("--- quanh chỗ gối ---");
        int at = text.indexOf(marker);
        String around = text.substring(Math.max(0, at - 200), Math.min(text.length(), at + 400));
        around.lines().forEach(line -> log.info("  {}", line));
        log.info("--- vector ---");
        log.info(av.toString());
        log.info("  số chiều {} | độ dài {} | 8 số đầu {}", v.length, "%.4f".formatted(norm(v)), firstValues(v, 8));
    }

    // vector đã chuẩn hoá nên cosine chính là tích vô hướng
    private static String score(float[] query, EmbeddedChunk chunk) {
        float[] v = chunk.getVector();
        double dot = 0;
        for (int i = 0; i < v.length; i++) {
            dot += query[i] * v[i];
        }
        return "%.4f".formatted(dot);
    }

    private static double norm(float[] v) {
        double sum = 0;
        for (float x : v) {
            sum += x * x;
        }
        return Math.sqrt(sum);
    }

    private static String firstValues(float[] v, int n) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < n; i++) {
            sb.append(i == 0 ? "" : ", ").append("%.4f".formatted(v[i]));
        }
        return sb.append(", ...]").toString();
    }
}
