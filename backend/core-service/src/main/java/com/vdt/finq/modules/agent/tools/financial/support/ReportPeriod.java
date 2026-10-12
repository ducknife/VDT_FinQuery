package com.vdt.finq.modules.agent.tools.financial.support;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

// một kỳ báo cáo (năm + kỳ) và các ngày Java suy ra từ nó
public record ReportPeriod(int year, PeriodCode period) {

    private static final DateTimeFormatter VN_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    // ngày chốt: chỉ tiêu thời điểm lấy số tại ngày này, chỉ tiêu thời kỳ kết thúc ở ngày này
    public LocalDate end() {
        return switch (period) {
            case Q1 -> LocalDate.of(year, 3, 31);
            case Q2, H1 -> LocalDate.of(year, 6, 30);
            case Q3, M9 -> LocalDate.of(year, 9, 30);
            case Q4, FY -> LocalDate.of(year, 12, 31);
        };
    }

    // ngày đầu của chỉ tiêu thời kỳ: quý thì đầu quý, còn lại là luỹ kế từ 01/01
    public LocalDate flowStart() {
        return switch (period) {
            case Q2 -> LocalDate.of(year, 4, 1);
            case Q3 -> LocalDate.of(year, 7, 1);
            case Q4 -> LocalDate.of(year, 10, 1);
            default -> LocalDate.of(year, 1, 1);
        };
    }

    // số dư đầu kỳ (để tính bình quân) = số tại ngày ngay trước kỳ
    public ReportPeriod opening() {
        return switch (period) {
            case Q2 -> new ReportPeriod(year, PeriodCode.Q1);
            case Q3 -> new ReportPeriod(year, PeriodCode.H1);
            case Q4 -> new ReportPeriod(year, PeriodCode.M9);
            default -> new ReportPeriod(year - 1, PeriodCode.FY);
        };
    }

    public long days() {
        return ChronoUnit.DAYS.between(flowStart(), end()) + 1;
    }

    public boolean isSingleLaterQuarter() {
        return period == PeriodCode.Q2 || period == PeriodCode.Q3 || period == PeriodCode.Q4;
    }

    // quý N chưa có số riêng quý (lưu chuyển tiền tệ chỉ có luỹ kế): quý N = luỹ kế đến N - luỹ kế đến N-1
    public ReportPeriod cumulativeToEnd() {
        return switch (period) {
            case Q2 -> new ReportPeriod(year, PeriodCode.H1);
            case Q3 -> new ReportPeriod(year, PeriodCode.M9);
            case Q4 -> new ReportPeriod(year, PeriodCode.FY);
            default -> this;
        };
    }

    public ReportPeriod cumulativeBefore() {
        return switch (period) {
            case Q2 -> new ReportPeriod(year, PeriodCode.Q1);
            case Q3 -> new ReportPeriod(year, PeriodCode.H1);
            case Q4 -> new ReportPeriod(year, PeriodCode.M9);
            default -> this;
        };
    }

    // nhãn tiếng Việt của chỉ tiêu thời kỳ
    public String flowLabel() {
        String range = " (" + VN_DATE.format(flowStart()) + "–" + VN_DATE.format(end()) + ")";
        return switch (period) {
            case Q1, Q2, Q3, Q4 -> "quý " + period.name().substring(1) + "/" + year + range;
            case H1 -> "6 tháng đầu năm " + year + range;
            case M9 -> "9 tháng đầu năm " + year + range;
            case FY -> "năm " + year + range;
        };
    }

    public String pointLabel() {
        return "tại " + VN_DATE.format(end());
    }

    public static String date(LocalDate d) {
        return VN_DATE.format(d);
    }
}
