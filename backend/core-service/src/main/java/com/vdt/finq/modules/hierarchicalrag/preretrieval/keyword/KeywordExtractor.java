package com.vdt.finq.modules.hierarchicalrag.preretrieval.keyword;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.Yaml;

import com.vdt.finq.common.text.VietnameseText;
import com.vdt.finq.modules.hierarchicalrag.preretrieval.normalizer.NormalizedQuery;

import lombok.AllArgsConstructor;

// trích từ khoá của câu hỏi theo luật (QĐ-030), thay cho LLM ở bước Keywords Extraction của paper:
//   1. cách gọi đời thường trong keywords.yml -> tên đúng như báo cáo viết ("lãi ròng" -> "lợi nhuận sau thuế ...")
//   2. tên chỉ tiêu có sẵn trong DB (financial_items, financial_item_mappings) xuất hiện nguyên cụm trong câu
// từ khoá được NỐI vào câu hỏi (Q' = chuẩn hoá ∪ từ khoá) để vector gần với cách báo cáo viết
@Component
public class KeywordExtractor {

    private static final int MAX_KEYWORDS = 6;
    // tên quá ngắn ("Tiền", "Thuế") khớp bừa, không đáng làm từ khoá
    private static final int MIN_NAME_LENGTH = 8;

    private final List<ReportName> reportNames;
    private final List<Alias> aliases;

    @AllArgsConstructor
    static class Alias {
        private final Pattern form;
        private final List<String> keywords;
    }

    @AllArgsConstructor
    private static class ReportName {
        private final String folded;
        private final String text;
        private final Pattern pattern;
    }

    @Autowired
    public KeywordExtractor(JdbcTemplate jdbcTemplate) {
        this(loadReportNames(jdbcTemplate), loadAliases());
    }

    // dùng cho test: truyền thẳng danh sách tên, không cần DB
    KeywordExtractor(List<String> reportNames, List<Alias> aliases) {
        Map<String, ReportName> names = new LinkedHashMap<>();
        for (String name : reportNames) {
            String clean = cleanName(name);
            String folded = VietnameseText.fold(clean);
            if (clean.length() >= MIN_NAME_LENGTH && !names.containsKey(folded)) {
                names.put(folded, new ReportName(folded, clean.toLowerCase(), wordPattern(folded)));
            }
        }
        this.reportNames = List.copyOf(names.values());
        this.aliases = aliases;
    }

    public List<String> extract(NormalizedQuery query) {
        // câu đã chuẩn hoá có cả dạng mở rộng của viết tắt: "LNST (lợi nhuận sau thuế)"
        String folded = VietnameseText.fold(query.getText());

        // tên chỉ tiêu khớp nguyên cụm, nhớ vị trí để xét cách gọi đời thường bên dưới
        Map<String, String> names = new LinkedHashMap<>();
        List<int[]> nameSpans = new ArrayList<>();
        for (ReportName name : reportNames) {
            Matcher m = name.pattern.matcher(folded);
            while (m.find()) {
                names.putIfAbsent(name.folded, name.text);
                nameSpans.add(new int[] { m.start(), m.end() });
            }
        }

        // không dấu -> có dấu, giữ thứ tự tìm thấy
        Map<String, String> found = new LinkedHashMap<>();
        for (Alias alias : aliases) {
            Matcher m = alias.form.matcher(folded);
            while (m.find()) {
                // "tài sản" nằm trong "tài sản ngắn hạn" (tên chỉ tiêu đã khớp) thì không phải cách gọi
                // "tổng tài sản", bỏ qua để không thêm nhầm "tổng cộng tài sản"
                if (!insideLongerName(m.start(), m.end(), nameSpans)) {
                    alias.keywords.forEach(k -> found.putIfAbsent(VietnameseText.fold(k), k));
                    break;
                }
            }
        }
        names.forEach(found::putIfAbsent);
        // cụm đã nằm trong cụm dài hơn thì bỏ: có "chi phí tài chính" rồi thì thôi "chi phí"
        List<String> keys = new ArrayList<>(found.keySet());
        keys.sort(Comparator.comparingInt(String::length).reversed());
        List<String> kept = new ArrayList<>();
        for (String key : keys) {
            if (kept.size() < MAX_KEYWORDS && kept.stream().noneMatch(longer -> longer.contains(key))) {
                kept.add(key);
            }
        }
        // trả lại theo thứ tự tìm thấy cho dễ đọc
        return found.keySet().stream().filter(kept::contains).map(found::get).toList();
    }

    // "LCTT — Tiền thu từ đi vay" -> "Tiền thu từ đi vay"; "Hàng tồn kho — mã 140 (...)" -> "Hàng tồn kho"
    static String cleanName(String name) {
        String s = name.strip()
                .replaceFirst("^(LCTT\\(TT\\)|LCTT)\\s*—\\s*", "")
                .replaceFirst("^Trong đó:\\s*", "")
                .replaceFirst("^[-+]\\s*", "")
                .replaceFirst("^\\d+\\.\\s*", "");
        int cut = s.indexOf(" — mã");
        if (cut > 0) {
            s = s.substring(0, cut);
        }
        return s.replace(" — ", " ").strip();
    }

    private static boolean insideLongerName(int start, int end, List<int[]> nameSpans) {
        return nameSpans.stream().anyMatch(s -> s[0] <= start && end <= s[1] && s[1] - s[0] > end - start);
    }

    // khớp nguyên cụm theo ranh giới từ, tránh khớp giữa chừng một từ khác
    private static Pattern wordPattern(String foldedPhrase) {
        return Pattern.compile("(?<![\\p{L}\\p{N}])" + Pattern.quote(foldedPhrase) + "(?![\\p{L}\\p{N}])");
    }

    private static List<String> loadReportNames(JdbcTemplate jdbcTemplate) {
        List<String> names = new ArrayList<>(
                jdbcTemplate.queryForList("SELECT ten_chuan FROM financial_items", String.class));
        names.addAll(jdbcTemplate.queryForList(
                "SELECT DISTINCT ten_tren_bao_cao FROM financial_item_mappings", String.class));
        return names;
    }

    @SuppressWarnings("unchecked")
    static List<Alias> loadAliases() {
        try (InputStream in = KeywordExtractor.class.getClassLoader()
                .getResourceAsStream("preretrieval/keywords.yml")) {
            Map<String, Object> yaml = new Yaml().load(in);
            List<Alias> result = new ArrayList<>();
            for (Map<String, Object> entry : (List<Map<String, Object>>) yaml.get("aliases")) {
                List<String> forms = ((List<Object>) entry.get("forms")).stream().map(String::valueOf).toList();
                List<String> keywords = ((List<Object>) entry.get("keywords")).stream().map(String::valueOf).toList();
                String any = String.join("|", forms.stream().map(f -> Pattern.quote(VietnameseText.fold(f))).toList());
                result.add(new Alias(Pattern.compile("(?<![\\p{L}\\p{N}])(?:" + any + ")(?![\\p{L}\\p{N}])"),
                        keywords));
            }
            return result;
        } catch (IOException | NullPointerException e) {
            throw new IllegalStateException("không đọc được preretrieval/keywords.yml", e);
        }
    }
}
