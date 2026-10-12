package com.vdt.finq.modules.financial.extraction;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.vdt.finq.common.text.VietnameseText;

import lombok.Getter;

// đọc tiêu đề cột thành kỳ, ví dụ "31/12/2023 VND" -> 2023-12-31, "Quý IV/2023" -> 2023-10-01..12-31,
// "9 tháng đầu năm 2023" -> 2023-01-01..09-30; "Kỳ này" -> null (suy từ tài liệu)
@Getter
public final class PeriodLabel {

    private static final Pattern RANGE = Pattern.compile(
            "tu (?:ngay )?(\\d{1,2})/(\\d{1,2})/(\\d{4}) den (?:ngay )?(\\d{1,2})/(\\d{1,2})/(\\d{4})");
    private static final Pattern MONTHS = Pattern.compile("(\\d{1,2}) thang (?:dau )?(?:nam )?(\\d{4})");
    private static final Pattern QUARTER = Pattern.compile("quy\\s*(iv|iii|ii|i|[1-4])\\s*[/.-]?\\s*(?:nam\\s*)?(\\d{4})");
    private static final Pattern DATE = Pattern.compile("(\\d{1,2})/(\\d{1,2})/(\\d{4})");
    private static final Pattern YEAR = Pattern.compile("nam (\\d{4})");

    private final Period period;
    private final boolean restated;

    private PeriodLabel(Period period, boolean restated) {
        this.period = period;
        this.restated = restated;
    }

    public static PeriodLabel parse(String label) {
        String s = VietnameseText.fold(label);
        boolean restated = s.contains("dieu chinh") || s.contains("trinh bay lai");
        boolean cumulative = s.contains("luy ke");
        Period period;
        try {
            period = resolve(s, cumulative);
        } catch (java.time.DateTimeException e) {
            // OCR đọc ra ngày không có thật (31/02): coi như không đọc được kỳ
            period = null;
        }
        return new PeriodLabel(period, restated);
    }

    private static Period resolve(String s, boolean cumulative) {
        Matcher m = RANGE.matcher(s);
        if (m.find()) {
            return Period.range(date(m.group(1), m.group(2), m.group(3)), date(m.group(4), m.group(5), m.group(6)));
        }
        m = MONTHS.matcher(s);
        if (m.find()) {
            int months = Integer.parseInt(m.group(1));
            int year = Integer.parseInt(m.group(2));
            if (months < 1 || months > 12) {
                return null;
            }
            return Period.yearToDate(YearMonth.of(year, months).atEndOfMonth());
        }
        m = QUARTER.matcher(s);
        if (m.find()) {
            Period quarter = Period.quarter(Integer.parseInt(m.group(2)), quarterNumber(m.group(1)));
            // cột "lũy kế" ghi quý: là từ đầu năm tới hết quý đó
            return cumulative ? Period.yearToDate(quarter.getDenNgay()) : quarter;
        }
        m = DATE.matcher(s);
        if (m.find()) {
            LocalDate date = date(m.group(1), m.group(2), m.group(3));
            // số dư 01/01 chính là số dư 31/12 năm trước: quy về một mốc để các báo cáo khớp nhau
            if (date.getDayOfMonth() == 1 && date.getMonthValue() == 1) {
                date = date.minusDays(1);
            }
            return Period.point(date);
        }
        m = YEAR.matcher(s);
        if (m.find()) {
            return Period.year(Integer.parseInt(m.group(1)));
        }
        return null;
    }

    private static int quarterNumber(String token) {
        return switch (token) {
            case "i", "1" -> 1;
            case "ii", "2" -> 2;
            case "iii", "3" -> 3;
            default -> 4;
        };
    }

    private static LocalDate date(String day, String month, String year) {
        return LocalDate.of(Integer.parseInt(year), Integer.parseInt(month), Integer.parseInt(day));
    }
}
