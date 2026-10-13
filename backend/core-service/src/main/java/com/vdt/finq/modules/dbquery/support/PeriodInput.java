package com.vdt.finq.modules.dbquery.support;

import org.springframework.ai.tool.annotation.ToolParam;

// một kỳ do LLM điền: năm + mã kỳ (Java đổi ra ngày)
public record PeriodInput(
        @ToolParam(description = "Calendar year, e.g. 2025") int year,
        @ToolParam(description = "Q1..Q4 = that single quarter (balance sheet: at quarter end); H1 = first 6 months "
                + "cumulative (at 30/06); M9 = first 9 months cumulative (at 30/09); FY = full year (at 31/12)") PeriodCode period) {

    public ReportPeriod toPeriod() {
        return new ReportPeriod(year, period);
    }
}
