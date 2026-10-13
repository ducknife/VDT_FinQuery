package com.vdt.finq.modules.rag.preprocessing.datacleaner;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.vdt.finq.common.properties.OCRProperties;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

// endpoint thử luật làm sạch trên toàn kho, chỉ đọc, không ghi file .md.
// bật bằng cleaner.test-api.enabled=true
@Slf4j
@RestController
@RequestMapping("/test/cleaner")
@RequiredArgsConstructor
@ConditionalOnProperty(name = "cleaner.test-api.enabled", havingValue = "true")
public class TestCleanerController {

    private final OCRProperties ocrProperties;
    private final TextCleaner textCleaner;
    private final NFCNormalization nfcNormalization;

    // chạy mọi trang qua cleaner và báo mỗi trang đổi bao nhiêu, không ghi gì
    @GetMapping("/audit")
    public Map<String, Object> audit() throws IOException {
        Path root = Path.of(ocrProperties.getOutputDir()).toAbsolutePath().normalize();
        long start = System.currentTimeMillis();

        List<Map<String, Object>> pages = new ArrayList<>();
        long rawTotal = 0;
        long cleanTotal = 0;
        int untouched = 0;

        try (Stream<Path> walk = Files.walk(root)) {
            for (Path p : walk.filter(Files::isRegularFile)
                    .filter(f -> f.getFileName().toString().startsWith("trang_"))
                    .filter(f -> f.getFileName().toString().endsWith(".md"))
                    .toList()) {

                String raw = Files.readString(p);
                String clean = textCleaner.clean(raw);
                rawTotal += raw.length();
                cleanTotal += clean.length();

                if (raw.strip().equals(clean)) {
                    untouched++;
                    continue;
                }
                pages.add(Map.of(
                        "path", root.relativize(p).toString().replace('\\', '/'),
                        "rawChars", raw.length(),
                        "cleanChars", clean.length(),
                        "removed", raw.length() - clean.length(),
                        "percent", percent(raw.length() - clean.length(), raw.length()),
                        "nbsp", countOf(raw, "&nbsp;"),
                        "fence", countOf(raw, "```"),
                        "wasNfc", nfcNormalization.isNfc(raw)));
            }
        }

        // trang bị cắt bớt nhiều nhất lên đầu, đó là chỗ luật dễ chạy sai nhất
        pages.sort(Comparator.comparingDouble(m -> -(double) m.get("percent")));

        long ms = System.currentTimeMillis() - start;
        log.info("cleaner audit: {} changed, {} untouched, {} ms",
                pages.size(), untouched, ms);

        return Map.of(
                "root", root.toString(),
                "totalPages", pages.size() + untouched,
                "changed", pages.size(),
                "untouched", untouched,
                "rawChars", rawTotal,
                "cleanChars", cleanTotal,
                "elapsedMs", ms,
                "pages", pages);
    }

    // trước và sau của một trang, để mở xem một dòng đáng ngờ trong bảng audit
    @GetMapping("/page")
    public Map<String, Object> page(@RequestParam String path) throws IOException {
        Path root = Path.of(ocrProperties.getOutputDir()).toAbsolutePath().normalize();
        Path file = root.resolve(path).normalize();
        // đường dẫn đến từ trình duyệt, coi là không tin cậy
        if (!file.startsWith(root)) {
            throw new IllegalArgumentException("path escapes the data directory: " + path);
        }
        String raw = Files.readString(file);
        String clean = textCleaner.clean(raw);
        return Map.of(
                "path", path,
                "raw", raw,
                "clean", clean,
                "rawChars", raw.length(),
                "cleanChars", clean.length());
    }

    private static double percent(int removed, int total) {
        return total == 0 ? 0 : Math.round(1000.0 * removed / total) / 10.0;
    }

    private static int countOf(String text, String needle) {
        int n = 0;
        int i = text.indexOf(needle);
        while (i >= 0) {
            n++;
            i = text.indexOf(needle, i + needle.length());
        }
        return n;
    }
}
