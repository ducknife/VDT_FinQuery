package com.vdt.finq.modules.hierarchicalrag.preretrieval.normalizer;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.regex.Pattern;

// đọc số tiền người Việt hay viết thành số đồng:
//     "8,2" + "nghìn tỷ"  -> 8.200.000.000.000     (dấu phẩy là phần thập phân)
//     "8.195" + "tỷ"      -> 8.195.000.000.000     (dấu chấm ngăn nghìn)
//     "1.5" + "tỷ"        -> 1.500.000.000         (chấm không ngăn đúng 3 chữ số: thập phân kiểu Anh)
//     "5" + "trăm triệu"  -> 500.000.000           ("tỉ" viết chữ i cũng nhận như "tỷ")
public final class VietnameseAmount {

    private static final Pattern GROUPED = Pattern.compile("\\d{1,3}(\\.\\d{3})+(,\\d+)?");

    private VietnameseAmount() {
    }

    // hệ số của đơn vị (đã bỏ dấu, chữ thường); đơn vị lạ thì 0
    public static long multiplier(String unit) {
        return switch (unit.replaceAll("\\s+", " ").strip()) {
            case "nghin ty", "ngan ty", "nghin ti", "ngan ti" -> 1_000_000_000_000L;
            case "tram ty", "tram ti" -> 100_000_000_000L;
            case "ty", "ti" -> 1_000_000_000L;
            case "tram trieu" -> 100_000_000L;
            case "trieu", "tr" -> 1_000_000L;
            case "tram nghin", "tram ngan" -> 100_000L;
            case "nghin", "ngan", "k" -> 1_000L;
            case "tram" -> 100L;
            case "dong", "vnd", "d" -> 1L;
            default -> 0L;
        };
    }

    public static long toDong(String number, String unit) {
        return parse(number).multiply(BigDecimal.valueOf(multiplier(unit)))
                .setScale(0, RoundingMode.HALF_UP).longValueExact();
    }

    static BigDecimal parse(String number) {
        String s = number.strip();
        if (GROUPED.matcher(s).matches()) {
            return new BigDecimal(s.replace(".", "").replace(',', '.'));
        }
        if (s.contains(",")) {
            return new BigDecimal(s.replace(".", "").replace(',', '.'));
        }
        return new BigDecimal(s);
    }

    // 8200000000000 -> "8.200.000.000.000 đồng"
    public static String format(long dong) {
        return String.format("%,d", dong).replace(',', '.') + " đồng";
    }
}
