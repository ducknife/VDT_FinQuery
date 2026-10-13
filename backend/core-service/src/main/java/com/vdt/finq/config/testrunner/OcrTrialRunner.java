package com.vdt.finq.config.testrunner;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.vdt.finq.common.properties.OCRProperties;
import com.vdt.finq.modules.rag.preprocessing.ocr.PdfRenderer;
import com.vdt.finq.modules.rag.preprocessing.ocr.VisionOcrClient;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

// OCR thử một trang rồi so con số với file .md của công cụ cũ; bật bằng ocr.trial.enabled=true
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "ocr.trial.enabled", havingValue = "true")
public class OcrTrialRunner implements CommandLineRunner {

    // dãy số nhóm bằng dấu chấm (8.195.930.808.790): không được sai lệch
    private static final Pattern MONEY = Pattern.compile("\\(?\\d{1,3}(?:\\.\\d{3}){2,}\\)?");

    private static final String PDF_PATH = "../../data/raw/CTR/01_bao_cao_tai_chinh/2019/rieng/BCTC-R-ban-nien-2019.pdf";
    private static final String REFERENCE_PATH = "../../data/ocr/CTR/01_bao_cao_tai_chinh/2019/rieng/BCTC-R-ban-nien-2019/trang_002.md";

    // đánh số như in trên giấy
    private static final int PAGE_NUMBER = 7;

    private final OCRProperties properties;
    private final VisionOcrClient visionOcrClient;

    @Override
    public void run(String... args) throws Exception {
        Path pdf = Path.of(PDF_PATH).toAbsolutePath().normalize();
        Path reference = Path.of(REFERENCE_PATH).toAbsolutePath().normalize();
        // bộ kết xuất đếm trang từ 0
        int pageIndex = PAGE_NUMBER - 1;

        log.info("=== ocr trial: page {} of {} ===", PAGE_NUMBER, pdf.getFileName());

        String fresh;
        try (PdfRenderer renderer = PdfRenderer.open(pdf)) {
            long renderStart = System.currentTimeMillis();
            byte[] jpeg = renderer.render(pageIndex, properties.getDpi(),
                    (float) properties.getJpegQuality());
            long renderMs = System.currentTimeMillis() - renderStart;

            // giữ lại ảnh để mở ra kiểm tra bằng mắt
            Path imageOut = Path.of("trial-page-%03d.jpg".formatted(PAGE_NUMBER));
            Files.write(imageOut, jpeg);
            log.info("rendered in {} ms -> {} KB ({})", renderMs, jpeg.length / 1024, imageOut);
            warnIfImageSizeIsOdd(jpeg.length);

            long ocrStart = System.currentTimeMillis();
            fresh = visionOcrClient.ocr(jpeg);
            log.info("ocr in {} ms -> {} chars",
                    System.currentTimeMillis() - ocrStart, fresh.length());
        }

        Path freshOut = Path.of("trial-page-%03d.md".formatted(PAGE_NUMBER));
        Files.writeString(freshOut, fresh);
        log.info("written to {}", freshOut);

        if (!Files.exists(reference)) {
            log.warn("no reference file at {}, nothing to compare against", reference);
            return;
        }
        compare(Files.readString(reference), fresh);
    }

    // ba phép kiểm tra, xếp theo mức độ nghiêm trọng nếu bị lệch
    private void compare(String reference, String fresh) {
        log.info("--- comparison against the node output ---");

        // 1. con số: sai một chữ số là sai số liệu
        Set<String> referenceMoney = extractMoney(reference);
        Set<String> freshMoney = extractMoney(fresh);
        Set<String> missing = new LinkedHashSet<>(referenceMoney);
        missing.removeAll(freshMoney);
        Set<String> extra = new LinkedHashSet<>(freshMoney);
        extra.removeAll(referenceMoney);

        log.info("numbers: reference {} | fresh {}", referenceMoney.size(), freshMoney.size());
        if (missing.isEmpty() && extra.isEmpty()) {
            log.info("  ok, every number matches");
        } else {
            log.error("  mismatch, missing {} | unexpected {}", missing, extra);
        }

        // 2. số dòng bảng. mất dòng nghĩa là model đã gộp hoặc bỏ sót dòng của bảng
        long referenceRows = countTableRows(reference);
        long freshRows = countTableRows(fresh);
        log.info("table rows: reference {} | fresh {} {}", referenceRows, freshRows,
                referenceRows == freshRows ? "ok" : "<-- differs");

        // 3. độ dài tổng: lệch nhiều là mất một phần trang
        double ratio = (double) fresh.length() / reference.length();
        log.info("length: reference {} | fresh {} (ratio {})",
                reference.length(), fresh.length(), "%.2f".formatted(ratio));
        if (ratio < 0.7 || ratio > 1.4) {
            log.warn("  length is far off, open both files and look");
        }

        firstDifferingLine(reference, fresh);
    }

    // chỉ ra chỗ hai kết quả bắt đầu khác nhau
    private void firstDifferingLine(String reference, String fresh) {
        List<String> a = reference.lines().map(String::strip).filter(s -> !s.isEmpty()).toList();
        List<String> b = fresh.lines().map(String::strip).filter(s -> !s.isEmpty()).toList();
        for (int i = 0; i < Math.min(a.size(), b.size()); i++) {
            if (!a.get(i).equals(b.get(i))) {
                log.info("first difference at line {}:", i + 1);
                log.info("  reference: {}", a.get(i));
                log.info("  fresh    : {}", b.get(i));
                return;
            }
        }
        if (a.size() == b.size()) {
            log.info("every non empty line is identical");
        } else {
            log.info("same prefix, different line counts: {} vs {}", a.size(), b.size());
        }
    }

    private static Set<String> extractMoney(String text) {
        Set<String> found = new LinkedHashSet<>();
        Matcher matcher = MONEY.matcher(text);
        while (matcher.find()) {
            found.add(matcher.group());
        }
        return found;
    }

    private static long countTableRows(String text) {
        return text.lines().filter(line -> line.strip().startsWith("|")).count();
    }

    // 150-400 KB là mức bình thường cho trang A4 ở 170 dpi, ảnh xám
    private static void warnIfImageSizeIsOdd(int bytes) {
        int kb = bytes / 1024;
        if (kb > 1024) {
            log.warn("image is {} KB, dpi or quality is too high and the network will choke", kb);
        } else if (kb < 50) {
            log.warn("image is only {} KB, too low and the model will misread digits", kb);
        }
    }
}
