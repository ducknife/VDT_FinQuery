package com.vdt.finq.modules.financial.extraction;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import com.vdt.finq.common.text.VietnameseText;

// so tên đọc được với tên chờ đợi; bắt ca đổi mã giữ tên và ca dòng lệch cột
public final class NameMatcher {

    // tiền tố đánh số và gạch đầu dòng: "A.", "II.", "1.", "(1)", "-", "+"
    private static final Pattern NUMBERING = Pattern.compile("^((\\(?([a-z]|[ivx]{1,4}|\\d{1,2})[.)]|[-+•])\\s*)+");
    private static final Pattern TRONG_DO = Pattern.compile("^trong do\\s*:?\\s*");
    private static final Pattern NON_WORD = Pattern.compile("[^a-z0-9 ]");

    private NameMatcher() {
    }

    public static boolean matches(String read, String expected) {
        String a = normalize(read);
        String b = normalize(expected);
        if (a.isEmpty() || b.isEmpty()) {
            return false;
        }
        if (a.equals(b) || a.contains(b) || b.contains(a)) {
            return true;
        }
        return jaccard(a, b) >= 0.6;
    }

    static String normalize(String name) {
        String s = VietnameseText.fold(name);
        for (int i = 0; i < 2; i++) {
            s = NUMBERING.matcher(s).replaceAll("");
            s = TRONG_DO.matcher(s).replaceAll("");
        }
        return NON_WORD.matcher(s).replaceAll(" ").replaceAll("\\s+", " ").strip();
    }

    private static double jaccard(String a, String b) {
        // Set.of ném lỗi khi có từ lặp ("chi phi ... chi phi"), nên dựng từ mảng
        Set<String> x = new HashSet<>(java.util.Arrays.asList(a.split(" ")));
        Set<String> y = new HashSet<>(java.util.Arrays.asList(b.split(" ")));
        Set<String> union = new HashSet<>(x);
        union.addAll(y);
        x.retainAll(y);
        return union.isEmpty() ? 0 : (double) x.size() / union.size();
    }
}
