package com.vdt.finq.modules.agent.tools.financial;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import com.vdt.finq.modules.agent.evidence.Evidence;
import com.vdt.finq.modules.agent.tools.QueryState;
import com.vdt.finq.modules.agent.tools.financial.support.FigureLookup.Fetched;
import com.vdt.finq.modules.agent.tools.financial.support.FigureLookup;
import com.vdt.finq.modules.agent.tools.financial.support.PeriodInput;
import com.vdt.finq.modules.agent.tools.financial.support.ReportPeriod;
import com.vdt.finq.modules.agent.tools.financial.support.Scope;
import com.vdt.finq.modules.agent.tools.financial.support.VnNumber;

import lombok.RequiredArgsConstructor;

// tool tra số: chỉ tiêu × kỳ × phạm vi; từ 2 kỳ thì Java tính thay đổi giữa các kỳ liền nhau
@Component
@RequiredArgsConstructor
public class GetFinancialFigures {

    private static final int MAX_INDICATORS = 4;
    private static final int MAX_PERIODS = 8;

    private final FigureLookup lookup;

    @Tool(name = "get_financial_figures", description = """
            Exact reported figures from the validated financial-statements database (balance sheet, income
            statement, cash flow), each with its source document and page. Use it for any standard line item, a
            series over several periods, or a comparison between periods: with 2+ periods the tool also returns
            the difference and % change between consecutive periods, computed exactly. Restated figures come with
            the originally published value. Do not compute anything yourself.""")
    public String getFinancialFigures(
            @ToolParam(description = "1 to 4 indicator codes from the INDICATOR CODES list") List<String> indicators,
            @ToolParam(description = "1 to 8 periods") List<PeriodInput> periods,
            @ToolParam(required = false, description = "CONSOLIDATED (hợp nhất, default) and/or SEPARATE (riêng)") List<Scope> scopes,
            ToolContext toolContext) {
        QueryState state = QueryState.of(toolContext);
        long start = System.nanoTime();
        if (indicators == null || indicators.isEmpty() || periods == null || periods.isEmpty()) {
            return "Error: give at least one indicator and one period.";
        }
        if (indicators.size() > MAX_INDICATORS || periods.size() > MAX_PERIODS) {
            return "Error: at most " + MAX_INDICATORS + " indicators and " + MAX_PERIODS + " periods per call.";
        }
        List<Scope> scopeList = scopes == null || scopes.isEmpty() ? List.of(Scope.CONSOLIDATED) : scopes;
        List<ReportPeriod> ordered = periods.stream().map(PeriodInput::toPeriod).distinct()
                .sorted(Comparator.comparing(ReportPeriod::end).thenComparing(ReportPeriod::flowStart)).toList();

        StringBuilder out = new StringBuilder();
        int found = 0;
        for (String code : indicators) {
            for (Scope scope : scopeList) {
                List<Fetched> series = new ArrayList<>();
                for (ReportPeriod p : ordered) {
                    Fetched f = lookup.fetch(state.getEvidence(), code, p, scope);
                    if (f.ok()) {
                        found++;
                        series.add(f);
                        out.append('[').append(f.evidence().getId()).append("] ").append(f.evidence().getContent()).append('\n');
                    } else {
                        out.append(f.error()).append('\n');
                    }
                }
                for (int i = 1; i < series.size(); i++) {
                    Evidence c = change(state, series.get(i - 1), series.get(i));
                    out.append('[').append(c.getId()).append("] ").append(c.getContent()).append('\n');
                }
            }
        }
        state.record("get_financial_figures(" + indicators + ", " + periods.size() + " kỳ, " + scopeList + ") -> " + found
                + " số", (System.nanoTime() - start) / 1_000_000, 0);
        return out.toString();
    }

    // thay đổi giữa hai kỳ: chênh lệch tuyệt đối và % so với kỳ trước
    private static Evidence change(QueryState state, Fetched before, Fetched after) {
        BigDecimal diff = after.value().subtract(before.value());
        String pct = before.value().signum() == 0 ? "không tính được % (kỳ trước bằng 0)"
                : VnNumber.percent(diff.abs().divide(before.value().abs(), 10, RoundingMode.HALF_UP));
        // % luôn dương, chiều nói bằng chữ "tăng"/"giảm" (cách viết của báo cáo)
        String direction = diff.signum() > 0 ? "tăng" : diff.signum() < 0 ? "giảm" : "không đổi";
        return state.getEvidence().addComputed("Thay đổi: " + after.label() + " [" + after.evidence().getId() + "] "
                + VnNumber.money(after.value()) + " so với " + before.label() + " [" + before.evidence().getId() + "] "
                + VnNumber.money(before.value()) + ": " + direction + " " + VnNumber.money(diff.abs()) + ", tương ứng "
                + pct + ". Java tính.");
    }
}
