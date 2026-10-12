package com.vdt.finq.common.text;

import java.text.Normalizer;
import java.util.regex.Pattern;

// so khớp chữ tiếng Việt không phân biệt dấu, hoa thường: "Lũy kế" và "LUỸ KẾ" đều thành "luy ke"
public final class VietnameseText {

    private static final Pattern MARKS = Pattern.compile("\\p{M}+");
    private static final Pattern SPACES = Pattern.compile("\\s+");

    private VietnameseText() {
    }

    public static String fold(String text) {
        if (text == null) {
            return "";
        }
        String s = Normalizer.normalize(text.toLowerCase(), Normalizer.Form.NFD);
        s = MARKS.matcher(s).replaceAll("").replace('đ', 'd');
        return SPACES.matcher(s).replaceAll(" ").strip();
    }

    // như fold nhưng giữ nguyên độ dài (đầu vào NFC): vị trí tìm trên bản không dấu dùng được trên câu gốc
    public static String foldKeepLength(String text) {
        StringBuilder out = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c < 128) {
                out.append(Character.toLowerCase(c));
                continue;
            }
            String base = MARKS.matcher(Normalizer.normalize(String.valueOf(c), Normalizer.Form.NFD)).replaceAll("");
            char f = base.isEmpty() ? c : Character.toLowerCase(base.charAt(0));
            out.append(f == 'đ' ? 'd' : f);
        }
        return out.toString();
    }

    // câu có chứa chữ có dấu tiếng Việt không (để biết người dùng có gõ không dấu)
    public static boolean hasDiacritics(String text) {
        return !foldKeepLength(text).equals(text.toLowerCase());
    }
}
