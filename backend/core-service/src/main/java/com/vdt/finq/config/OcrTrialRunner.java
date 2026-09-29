package com.vdt.finq.config;

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
import com.vdt.finq.modules.hierarchicalrag.preprocessing.ocr.PdfRenderer;
import com.vdt.finq.modules.hierarchicalrag.preprocessing.ocr.VisionOcrClient;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Runs one page end to end and compares it with the .md the old Node tool produced.
 *
 * This is the only honest way to tell whether the Java pipeline works: seeing text
 * come back is not proof, the digits have to match.
 *
 * Off by default. Turn on with:  --ocr.trial.enabled=true
 * Override the page with:        --ocr.trial.page-number=8
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "ocr.trial.enabled", havingValue = "true")
public class OcrTrialRunner implements CommandLineRunner {

    /** A run of digits grouped by dots, e.g. 8.195.930.808.790 - the shape that must not drift. */
    private static final Pattern MONEY = Pattern.compile("\\(?\\d{1,3}(?:\\.\\d{3}){2,}\\)?");

    private static final String PDF_PATH =
            "../../data/raw/CTR/01_bao_cao_tai_chinh/2019/rieng/BCTC-R-ban-nien-2019.pdf";
    private static final String REFERENCE_PATH =
            "../../data/ocr/CTR/01_bao_cao_tai_chinh/2019/rieng/BCTC-R-ban-nien-2019/trang_002.md";
    private static final int PAGE_NUMBER = 2;          // as printed on paper

    private final OCRProperties properties;
    private final VisionOcrClient visionOcrClient;

    @Override
    public void run(String... args) throws Exception {
        Path pdf = Path.of(PDF_PATH).toAbsolutePath().normalize();
        Path reference = Path.of(REFERENCE_PATH).toAbsolutePath().normalize();
        int pageIndex = PAGE_NUMBER - 1;               // renderer counts from zero

        log.info("=== OCR trial: page {} of {} ===", PAGE_NUMBER, pdf.getFileName());

        String fresh;
        byte[] jpeg;
        try (PdfRenderer renderer = PdfRenderer.open(pdf)) {
            long renderStart = System.currentTimeMillis();
            jpeg = renderer.render(pageIndex, properties.getDpi(),
                    (float) properties.getJpegQuality());
            long renderMs = System.currentTimeMillis() - renderStart;

            // Keep the image so it can be opened and checked by eye.
            Path imageOut = Path.of("trial-page-%03d.jpg".formatted(PAGE_NUMBER));
            Files.write(imageOut, jpeg);
            log.info("rendered in {} ms -> {} KB  ({})", renderMs, jpeg.length / 1024, imageOut);
            warnIfImageSizeIsOdd(jpeg.length);

            long ocrStart = System.currentTimeMillis();
            fresh = visionOcrClient.ocr(jpeg);
            log.info("OCR in {} ms -> {} chars", System.currentTimeMillis() - ocrStart,
                    fresh.length());
        }

        Path freshOut = Path.of("trial-page-%03d.md".formatted(PAGE_NUMBER));
        Files.writeString(freshOut, fresh);
        log.info("written to {}", freshOut);

        if (!Files.exists(reference)) {
            log.warn("no reference file at {} - nothing to compare against", reference);
            return;
        }
        compare(Files.readString(reference), fresh);
    }

    /** Three checks, in order of how badly a mismatch would hurt. */
    private void compare(String reference, String fresh) {
        log.info("--- comparison against the Node output ---");

        // 1. Digits. A single wrong digit is a wrong financial figure, so this is the
        //    check that matters most - a page can read well and still be useless.
        Set<String> referenceMoney = extractMoney(reference);
        Set<String> freshMoney = extractMoney(fresh);
        Set<String> missing = new LinkedHashSet<>(referenceMoney);
        missing.removeAll(freshMoney);
        Set<String> extra = new LinkedHashSet<>(freshMoney);
        extra.removeAll(referenceMoney);

        log.info("numbers: reference {} | fresh {}", referenceMoney.size(), freshMoney.size());
        if (missing.isEmpty() && extra.isEmpty()) {
            log.info("  OK - every number matches");
        } else {
            log.error("  MISMATCH - missing {} | unexpected {}", missing, extra);
        }

        // 2. Table rows. Losing rows means the model merged or dropped table lines.
        long referenceRows = countTableRows(reference);
        long freshRows = countTableRows(fresh);
        log.info("table rows: reference {} | fresh {} {}", referenceRows, freshRows,
                referenceRows == freshRows ? "OK" : "<-- differs");

        // 3. Overall size. Wording may differ slightly between runs; a big gap means
        //    a chunk of the page went missing.
        double ratio = (double) fresh.length() / reference.length();
        log.info("length: reference {} | fresh {} (ratio {})",
                reference.length(), fresh.length(), "%.2f".formatted(ratio));
        if (ratio < 0.7 || ratio > 1.4) {
            log.warn("  length is far off - open both files and look");
        }

        firstDifferingLine(reference, fresh);
    }

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
            log.info("every non-empty line is identical");
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

    private static void warnIfImageSizeIsOdd(int bytes) {
        int kb = bytes / 1024;
        if (kb > 1024) {
            log.warn("image is {} KB - DPI or quality is too high, the network will choke", kb);
        } else if (kb < 50) {
            log.warn("image is only {} KB - too low, the model will misread digits", kb);
        }
    }
}
