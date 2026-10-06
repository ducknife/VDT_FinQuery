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
import com.vdt.finq.modules.hierarchicalrag.preprocessing.ocr.PdfRenderer;
import com.vdt.finq.modules.hierarchicalrag.preprocessing.ocr.VisionOcrClient;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

// runs one page end to end and compares it with the .md the old node tool produced.
// seeing text come back is not proof, the digits have to match.
// off by default, turn on with ocr.trial.enabled=true
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "ocr.trial.enabled", havingValue = "true")
public class OcrTrialRunner implements CommandLineRunner {

    // a run of digits grouped by dots like 8.195.930.808.790, the shape that must
    // not drift
    private static final Pattern MONEY = Pattern.compile("\\(?\\d{1,3}(?:\\.\\d{3}){2,}\\)?");

    private static final String PDF_PATH = "../../data/raw/CTR/01_bao_cao_tai_chinh/2019/rieng/BCTC-R-ban-nien-2019.pdf";
    private static final String REFERENCE_PATH = "../../data/ocr/CTR/01_bao_cao_tai_chinh/2019/rieng/BCTC-R-ban-nien-2019/trang_002.md";

    // as printed on paper
    private static final int PAGE_NUMBER = 7;

    private final OCRProperties properties;
    private final VisionOcrClient visionOcrClient;

    @Override
    public void run(String... args) throws Exception {
        Path pdf = Path.of(PDF_PATH).toAbsolutePath().normalize();
        Path reference = Path.of(REFERENCE_PATH).toAbsolutePath().normalize();
        // the renderer counts from zero
        int pageIndex = PAGE_NUMBER - 1;

        log.info("=== ocr trial: page {} of {} ===", PAGE_NUMBER, pdf.getFileName());

        String fresh;
        try (PdfRenderer renderer = PdfRenderer.open(pdf)) {
            long renderStart = System.currentTimeMillis();
            byte[] jpeg = renderer.render(pageIndex, properties.getDpi(),
                    (float) properties.getJpegQuality());
            long renderMs = System.currentTimeMillis() - renderStart;

            // keep the image so it can be opened and checked by eye
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

    // three checks, ordered by how badly a mismatch would hurt
    private void compare(String reference, String fresh) {
        log.info("--- comparison against the node output ---");

        // 1. digits. one wrong digit is a wrong financial figure, so this matters most.
        // a page can read well and still be useless
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

        // 2. table rows. losing rows means the model merged or dropped table lines
        long referenceRows = countTableRows(reference);
        long freshRows = countTableRows(fresh);
        log.info("table rows: reference {} | fresh {} {}", referenceRows, freshRows,
                referenceRows == freshRows ? "ok" : "<-- differs");

        // 3. overall size. wording may differ between runs, a big gap means a chunk
        // of the page went missing
        double ratio = (double) fresh.length() / reference.length();
        log.info("length: reference {} | fresh {} (ratio {})",
                reference.length(), fresh.length(), "%.2f".formatted(ratio));
        if (ratio < 0.7 || ratio > 1.4) {
            log.warn("  length is far off, open both files and look");
        }

        firstDifferingLine(reference, fresh);
    }

    // show where the two outputs start to diverge
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

    // 150-400 KB is the normal range for a4 at 170 dpi grayscale
    private static void warnIfImageSizeIsOdd(int bytes) {
        int kb = bytes / 1024;
        if (kb > 1024) {
            log.warn("image is {} KB, dpi or quality is too high and the network will choke", kb);
        } else if (kb < 50) {
            log.warn("image is only {} KB, too low and the model will misread digits", kb);
        }
    }
}
