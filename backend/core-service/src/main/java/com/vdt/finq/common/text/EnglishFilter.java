package com.vdt.finq.common.text;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// nhận ra và bỏ phần tiếng Anh trong tài liệu song ngữ bằng luật (tỷ lệ chữ có dấu + từ chức năng tiếng Anh).
// chỉ bỏ nguyên đoạn, không sửa chữ tiếng Việt; dòng bảng luôn giữ
public final class EnglishFilter {

    private static final String VIETNAMESE_MARKED =
            "ăâđêôơưáàảãạắằẳẵặấầẩẫậéèẻẽẹếềểễệíìỉĩịóòỏõọốồổỗộớờởỡợúùủũụứừửữựýỳỷỹỵ";
    private static final Pattern ENGLISH_WORDS = Pattern.compile(
            "\\b(the|and|of|in|to|for|with|on|by|from|which|was|were|is|are|this|that|as|at)\\b",
            Pattern.CASE_INSENSITIVE);

    // một đoạn: đủ dài, gần như không có chữ mang dấu, có từ chức năng tiếng Anh
    private static final int PARAGRAPH_MIN_LETTERS = 30;
    private static final int PARAGRAPH_MIN_ENGLISH_WORDS = 2;
    // cả chunk: thuần tiếng Anh (cùng ngưỡng đã dùng để quét kho)
    private static final int CHUNK_MIN_ENGLISH_WORDS = 10;
    private static final double MAX_VIETNAMESE_RATIO = 0.02;

    private EnglishFilter() {
    }

    // chunk thuần tiếng Anh (bản dịch của trang tiếng Việt): loại khỏi danh sách ứng viên
    public static boolean isEnglishChunk(String text) {
        String t = nfc(text);
        Stats s = stats(t);
        return s.letters >= 200 && s.vietnameseRatio() < MAX_VIETNAMESE_RATIO && englishWords(t) >= CHUNK_MIN_ENGLISH_WORDS;
    }

    // bỏ các đoạn văn tiếng Anh, giữ tiếng Việt, dòng bảng, đoạn ngắn (tiêu đề, số, ngày)
    public static String stripEnglish(String text) {
        String t = nfc(text);
        List<String> out = new ArrayList<>();
        List<String> paragraph = new ArrayList<>();
        for (String line : t.split("\n", -1)) {
            boolean boundary = line.isBlank() || line.stripLeading().startsWith("|");
            if (boundary) {
                flush(paragraph, out);
                out.add(line);
            } else {
                paragraph.add(line);
            }
        }
        flush(paragraph, out);
        return String.join("\n", out).replaceAll("\n{3,}", "\n\n").strip();
    }

    static boolean isEnglishParagraph(String paragraph) {
        Stats s = stats(paragraph);
        return s.letters >= PARAGRAPH_MIN_LETTERS && s.vietnameseRatio() < MAX_VIETNAMESE_RATIO
                && englishWords(paragraph) >= PARAGRAPH_MIN_ENGLISH_WORDS;
    }

    private static void flush(List<String> paragraph, List<String> out) {
        if (paragraph.isEmpty()) {
            return;
        }
        String p = String.join("\n", paragraph);
        if (!isEnglishParagraph(p)) {
            out.add(p);
        }
        paragraph.clear();
    }

    private record Stats(int letters, int vietnamese) {
        double vietnameseRatio() {
            return letters == 0 ? 0 : (double) vietnamese / letters;
        }
    }

    private static Stats stats(String text) {
        int letters = 0;
        int vietnamese = 0;
        String lower = text.toLowerCase();
        for (int i = 0; i < lower.length(); i++) {
            char c = lower.charAt(i);
            if (Character.isLetter(c)) {
                letters++;
                if (VIETNAMESE_MARKED.indexOf(c) >= 0) {
                    vietnamese++;
                }
            }
        }
        return new Stats(letters, vietnamese);
    }

    private static int englishWords(String text) {
        Matcher m = ENGLISH_WORDS.matcher(text);
        int n = 0;
        while (m.find()) {
            n++;
        }
        return n;
    }

    private static String nfc(String text) {
        return text == null ? "" : Normalizer.normalize(text, Normalizer.Form.NFC);
    }
}
