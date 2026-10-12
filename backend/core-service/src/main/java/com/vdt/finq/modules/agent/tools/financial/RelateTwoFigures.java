package com.vdt.finq.modules.agent.tools.financial;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import com.vdt.finq.modules.agent.evidence.Evidence;
import com.vdt.finq.modules.agent.tools.QueryState;
import com.vdt.finq.modules.agent.tools.financial.support.FigureLookup.Fetched;
import com.vdt.finq.modules.agent.tools.financial.support.FigureLookup;
import com.vdt.finq.modules.agent.tools.financial.support.FigureRef;
import com.vdt.finq.modules.agent.tools.financial.support.VnNumber;

import lombok.RequiredArgsConstructor;

// tool quan hệ hai số: a/b (lần), a chiếm % b, a so với b
@Component
@RequiredArgsConstructor
public class RelateTwoFigures {

    public enum Relation {
        RATIO, SHARE, CHANGE
    }

    private final FigureLookup lookup;

    @Tool(name = "relate_two_figures", description = """
            Relate two specific figures from the financial-statements database, computed exactly by the system:
            RATIO = a / b in times ("a is how many times b"), SHARE = a as a percentage of b ("a accounts for what % of
            b"), CHANGE = difference a - b and % change versus b. a and b may differ in indicator, period or scope.
            For named ratios (margins, ROE, ROA, debt ratios, CCC...) use compute_financial_ratio instead.""")
    public String relateTwoFigures(
            @ToolParam(description = "The first figure (numerator, or the newer value for CHANGE)") FigureRef a,
            @ToolParam(description = "The second figure (denominator, or the base value for CHANGE)") FigureRef b,
            @ToolParam(description = "RATIO, SHARE or CHANGE") Relation relation,
            ToolContext toolContext) {
        QueryState state = QueryState.of(toolContext);
        long start = System.nanoTime();
        if (a == null || b == null || relation == null) {
            return "Error: a, b and relation are required.";
        }
        Fetched fa = lookup.fetch(state.getEvidence(), a.indicator(), a.toPeriod(), a.scopeOrDefault());
        Fetched fb = lookup.fetch(state.getEvidence(), b.indicator(), b.toPeriod(), b.scopeOrDefault());
        StringBuilder out = new StringBuilder();
        for (Fetched f : new Fetched[] { fa, fb }) {
            out.append(f.ok() ? "[" + f.evidence().getId() + "] " + f.evidence().getContent() : f.error()).append('\n');
        }
        if (fa.ok() && fb.ok()) {
            if (fb.value().signum() == 0 && relation != Relation.CHANGE) {
                out.append("Error: the second figure is zero, the ratio is undefined.\n");
            } else {
                Evidence c = state.getEvidence().addComputed(relate(fa, fb, relation));
                out.append('[').append(c.getId()).append("] ").append(c.getContent()).append('\n');
            }
        }
        state.record("relate_two_figures(" + a.indicator() + " " + a.period() + "/" + a.year() + ", " + b.indicator() + " "
                + b.period() + "/" + b.year() + ", " + relation + ")", (System.nanoTime() - start) / 1_000_000, 0);
        return out.toString();
    }

    private static String relate(Fetched a, Fetched b, Relation relation) {
        String ia = a.label() + " [" + a.evidence().getId() + "] " + VnNumber.money(a.value());
        String ib = b.label() + " [" + b.evidence().getId() + "] " + VnNumber.money(b.value());
        return switch (relation) {
            case RATIO -> ia + " / " + ib + " = " + VnNumber.times(a.value().divide(b.value(), 10, RoundingMode.HALF_UP))
                    + ". Java tính.";
            case SHARE -> ia + " chiếm " + VnNumber.percent(a.value().divide(b.value(), 10, RoundingMode.HALF_UP)) + " của "
                    + ib + ". Java tính.";
            case CHANGE -> {
                BigDecimal diff = a.value().subtract(b.value());
                String pct = b.value().signum() == 0 ? "không tính được %"
                        : VnNumber.percent(diff.abs().divide(b.value().abs(), 10, RoundingMode.HALF_UP));
                String dir = diff.signum() > 0 ? "tăng" : diff.signum() < 0 ? "giảm" : "không đổi";
                yield ia + " so với " + ib + ": " + dir + " " + VnNumber.money(diff.abs()) + ", tương ứng " + pct + ". Java tính.";
            }
        };
    }
}
