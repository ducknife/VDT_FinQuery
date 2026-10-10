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
}
