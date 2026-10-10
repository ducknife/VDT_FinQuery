package com.vdt.finq.modules.financial.extraction;

import java.util.regex.Pattern;

// đọc số trên báo cáo tài chính Việt Nam thành long (đơn vị đồng).
//     "5.627.168.242.619"   ->  5627168242619
//     "(103.552.387.501)"   -> -103552387501    ngoặc đơn là số âm
//     "-"                   ->  0              gạch ngang là không có số phát sinh
//     ""                    ->  null           ô trống: không có giá trị
// chuỗi khác dạng thì ném NumberFormatException: OCR đọc sai thì phải lộ ra, không được đoán
public final class VietnameseNumberParser {

    // nhóm nghìn ngăn bằng dấu chấm; số nhỏ dưới 1.000 thì không có dấu chấm
    private static final Pattern GROUPED = Pattern.compile("\\d{1,3}(\\.\\d{3})*");

    private VietnameseNumberParser() {
    }

    public static Long parse(String raw) {
        if (raw == null) {
            return null;
        }
        String s = raw.replace("*", "").replace(" ", " ").strip();
        if (s.isEmpty()) {
            return null;
        }
        if (s.equals("-") || s.equals("–") || s.equals("—")) {
            return 0L;
        }
        boolean negative = false;
        if (s.startsWith("(") && s.endsWith(")")) {
            negative = true;
            s = s.substring(1, s.length() - 1).strip();
        } else if (s.startsWith("-")) {
            negative = true;
            s = s.substring(1).strip();
        }
        if (!GROUPED.matcher(s).matches()) {
            throw new NumberFormatException("không phải số dạng 1.234.567: " + raw);
        }
        long value = Long.parseLong(s.replace(".", ""));
        return negative ? -value : value;
    }

    // ô có phải số (hoặc gạch ngang) không, dùng để nhận ra cột giá trị
    public static boolean looksNumeric(String raw) {
        try {
            return parse(raw) != null;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
