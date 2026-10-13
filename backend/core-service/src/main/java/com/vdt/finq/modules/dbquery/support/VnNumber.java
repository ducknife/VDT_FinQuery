package com.vdt.finq.modules.dbquery.support;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;

// viết số kiểu Việt Nam; D# mang cả số đồng và số tỷ để LLM chép đúng chuỗi
public final class VnNumber {

    private static final BigDecimal BILLION = BigDecimal.valueOf(1_000_000_000L);
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private VnNumber() {
    }

    private static DecimalFormat format(String pattern) {
        DecimalFormatSymbols s = new DecimalFormatSymbols();
        s.setGroupingSeparator('.');
        s.setDecimalSeparator(',');
        s.setMinusSign('-');
        return new DecimalFormat(pattern, s);
    }

    // 12609519560081 -> "12.609.519.560.081 đồng (≈ 12.609,52 tỷ đồng)"
    public static String money(long dong) {
        return dong(dong) + " đồng (≈ " + billion(dong) + " tỷ đồng)";
    }

    public static String dong(long dong) {
        return format("#,##0").format(dong);
    }

    public static String billion(long dong) {
        return format("#,##0.00").format(BigDecimal.valueOf(dong).divide(BILLION, 2, RoundingMode.HALF_UP));
    }

    public static String money(BigDecimal dong) {
        return money(dong.setScale(0, RoundingMode.HALF_UP).longValueExact());
    }

    // tỷ lệ 0.1055 -> "10,55%"
    public static String percent(BigDecimal ratio) {
        return format("#,##0.00").format(ratio.multiply(HUNDRED).setScale(2, RoundingMode.HALF_UP)) + "%";
    }

    public static String times(BigDecimal ratio) {
        return format("#,##0.00").format(ratio.setScale(2, RoundingMode.HALF_UP)) + " lần";
    }

    public static String days(BigDecimal days) {
        return format("#,##0.0").format(days.setScale(1, RoundingMode.HALF_UP)) + " ngày";
    }
}
