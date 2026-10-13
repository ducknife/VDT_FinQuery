package com.vdt.finq.modules.dbquery.support;

import org.springframework.ai.tool.annotation.ToolParam;

// một con số cụ thể do LLM điền: mã chỉ tiêu + kỳ + phạm vi
public record FigureRef(
        @ToolParam(description = "Indicator code from the INDICATOR CODES list") String indicator,
        @ToolParam(description = "Calendar year, e.g. 2025") int year,
        @ToolParam(description = "Q1..Q4, H1, M9 or FY (same meaning as in get_financial_figures)") PeriodCode period,
        @ToolParam(description = "CONSOLIDATED (hợp nhất, default) or SEPARATE (riêng, parent company only)") Scope scope) {

    public ReportPeriod toPeriod() {
        return new ReportPeriod(year, period);
    }

    public Scope scopeOrDefault() {
        return scope == null ? Scope.CONSOLIDATED : scope;
    }
}
