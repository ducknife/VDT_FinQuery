package com.vdt.finq.modules.financial.extraction;

import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;

import lombok.EqualsAndHashCode;
import lombok.Getter;

// khoảng thời gian của một con số: thời điểm (tuNgay = denNgay) hoặc thời kỳ
@Getter
@EqualsAndHashCode
public final class Period {
    private final LocalDate tuNgay;
    private final LocalDate denNgay;

    private Period(LocalDate tuNgay, LocalDate denNgay) {
        this.tuNgay = tuNgay;
        this.denNgay = denNgay;
    }

    public static Period point(LocalDate date) {
        return new Period(date, date);
    }

    public static Period range(LocalDate tuNgay, LocalDate denNgay) {
        return new Period(tuNgay, denNgay);
    }

    // từ đầu năm tới ngày end
    public static Period yearToDate(LocalDate end) {
        return range(end.withDayOfYear(1), end);
    }

    // quý chứa ngày end
    public static Period quarterOf(LocalDate end) {
        int firstMonth = (end.getMonthValue() - 1) / 3 * 3 + 1;
        return range(LocalDate.of(end.getYear(), firstMonth, 1), end);
    }

    public static Period quarter(int year, int quarter) {
        LocalDate start = LocalDate.of(year, (quarter - 1) * 3 + 1, 1);
        return range(start, start.plusMonths(2).with(TemporalAdjusters.lastDayOfMonth()));
    }

    public static Period year(int year) {
        return range(LocalDate.of(year, 1, 1), LocalDate.of(year, 12, 31));
    }

    public boolean isPoint() {
        return tuNgay.equals(denNgay);
    }

    // cùng kỳ năm trước; ngày cuối tháng vẫn là ngày cuối tháng (29/02 -> 28/02)
    public Period previousYear() {
        return new Period(shift(tuNgay), shift(denNgay));
    }

    private static LocalDate shift(LocalDate date) {
        LocalDate moved = date.minusYears(1);
        boolean monthEnd = date.equals(date.with(TemporalAdjusters.lastDayOfMonth()));
        return monthEnd ? moved.with(TemporalAdjusters.lastDayOfMonth()) : moved;
    }

    @Override
    public String toString() {
        return isPoint() ? tuNgay.toString() : tuNgay + ".." + denNgay;
    }
}
