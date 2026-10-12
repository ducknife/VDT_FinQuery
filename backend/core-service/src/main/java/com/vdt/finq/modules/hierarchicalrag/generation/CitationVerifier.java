package com.vdt.finq.modules.hierarchicalrag.generation;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// kiểm kỷ luật bằng chứng bằng code: câu trích có thật trong nguồn, số trong câu trả lời có trong nguồn
public final class CitationVerifier {

    // số có ý nghĩa: nhóm nghìn (12.609.519), thập phân phẩy (599,72), phần trăm (15,50%); bỏ số nhỏ lẻ
    private static final Pattern NUMBER = Pattern.compile("(?<![\\d.,])(\\d{1,3}(?:\\.\\d{3})+(?:,\\d+)?|\\d+,\\d+|\\d{3,})(?![\\d])");
    private static final Pattern YEAR = Pattern.compile("(19|20)\\d{2}");
    private static final Pattern CITE_ID = Pattern.compile("\\[([CD]\\d+)\\]");
    // ranh giới câu: dấu câu + khoảng trắng (không cắt số 12.609.519), hoặc "..."
    private static final Pattern SEGMENT_SPLIT = Pattern.compile("(?<=[.;:!?])\\s+|\\s*(?:\\.{3}|…)\\s*");
    private static final int MIN_SEGMENT_CHARS = 15;

    private CitationVerifier() {
    }

    public static String normalize(String text) {
        if (text == null) {
            return "";
        }
        String s = Normalizer.normalize(text, Normalizer.Form.NFC).toLowerCase();
        s = s.replaceAll("[|*#`_]", " ").replace(' ', ' ');
        // bỏ ký hiệu định dạng (gạch đầu dòng, dấu hai chấm); chữ và số vẫn phải khớp từng ký tự
        s = s.replaceAll("(^|\\s)[-•+](?=\\s)", " ").replace(":", " ");
        return s.replaceAll("\\s+", " ").strip();
    }

    public static boolean quoteFound(String quote, String source) {
        String q = normalize(quote);
        if (q.isEmpty()) {
            return false;
        }
        String s = normalize(source);
        if (s.contains(q)) {
            return true;
        }
        // câu trích nối hai câu thật cách nhau một đoạn: mọi câu dài đều phải có nguyên văn trong nguồn
        List<String> segments = new ArrayList<>();
        for (String part : SEGMENT_SPLIT.split(q)) {
            String p = part.strip();
            if (p.length() >= MIN_SEGMENT_CHARS) {
                segments.add(p);
            }
        }
        return segments.size() >= 2 && segments.stream().allMatch(s::contains);
    }

    // các số trong câu trả lời (trừ năm và mã trích dẫn) không xuất hiện trong bất kỳ bằng chứng nào
    public static List<String> unverifiedNumbers(String answer, List<String> sources) {
        String text = CITE_ID.matcher(answer == null ? "" : answer).replaceAll(" ");
        String all = normalize(String.join("\n", sources));
        Set<String> missing = new LinkedHashSet<>();
        Matcher m = NUMBER.matcher(text);
        while (m.find()) {
            String n = m.group(1);
            if (YEAR.matcher(n).matches() || all.contains(n)) {
                continue;
            }
            missing.add(n);
        }
        return new ArrayList<>(missing);
    }

    // các mã [C#]/[D#] xuất hiện trong câu trả lời
    public static List<String> citedIds(String answer) {
        Set<String> ids = new LinkedHashSet<>();
        Matcher m = CITE_ID.matcher(answer == null ? "" : answer);
        while (m.find()) {
            ids.add(m.group(1));
        }
        return new ArrayList<>(ids);
    }
}
