package com.vdt.finq.config.testrunner;

import java.nio.file.Files;
import java.nio.file.Path;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.vdt.finq.modules.rag.preprocessing.datacleaner.NFCNormalization;
import com.vdt.finq.modules.rag.preprocessing.datacleaner.TextCleaner;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

// làm sạch thử một trang và báo thay đổi; bật bằng cleaner.trial.enabled=true
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "cleaner.trial.enabled", havingValue = "true")
public class CleanerTrialRunner implements CommandLineRunner {

    // trang tệ nhất kho: 17.940 ký tự, phần lớn là &nbsp; do model kẹt vòng lặp
    private static final String PAGE_PATH =
            "../../data/ocr/CTR/01_bao_cao_tai_chinh/2019/rieng/BCR-nam-2019/trang_002.md";

    private final TextCleaner textCleaner;
    private final NFCNormalization nfcNormalization;

    @Override
    public void run(String... args) throws Exception {
        Path page = Path.of(PAGE_PATH).toAbsolutePath().normalize();
        if (!Files.exists(page)) {
            log.warn("page not found: {}", page);
            return;
        }

        String raw = Files.readString(page);
        String clean = textCleaner.clean(raw);

        log.info("=== cleaner trial: {} ===", page.getFileName());
        log.info("before : {} chars, {} lines, nfc={}",
                raw.length(), raw.lines().count(), nfcNormalization.isNfc(raw));
        log.info("after  : {} chars, {} lines, nfc={}",
                clean.length(), clean.lines().count(), nfcNormalization.isNfc(clean));
        log.info("removed: {} chars ({}%)",
                raw.length() - clean.length(),
                "%.1f".formatted(100.0 * (raw.length() - clean.length()) / raw.length()));

        log.info("--- what each rule found ---");
        report("&nbsp;", count(raw, "&nbsp;"));
        report("code fence", count(raw, "```"));
        report("trailing whitespace", raw.split("\n", -1).length == 0 ? 0
                : (int) raw.lines().filter(l -> l.length() != l.stripTrailing().length()).count());
        report("inner double space", countPattern(raw));

        Path out = Path.of("trial-cleaned.md");
        Files.writeString(out, clean);
        log.info("written to {}", out);

        log.info("--- first 12 lines after cleaning ---");
        clean.lines().limit(12).forEach(l -> log.info("  {}", l));
    }

    private void report(String rule, int hits) {
        log.info("  {:<22} {} hits".replace("{:<22}", "%-22s".formatted(rule)), hits);
    }

    private static int count(String text, String needle) {
        int n = 0;
        int i = text.indexOf(needle);
        while (i >= 0) {
            n++;
            i = text.indexOf(needle, i + needle.length());
        }
        return n;
    }

    private static int countPattern(String text) {
        return (int) java.util.regex.Pattern.compile("(?<=\\S)[ \\t]{2,}")
                .matcher(text).results().count();
    }
}
