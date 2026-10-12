package com.vdt.finq.modules.agent.tools.financial;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import com.vdt.finq.modules.agent.evidence.Evidence;
import com.vdt.finq.modules.agent.evidence.EvidenceStore;
import com.vdt.finq.modules.agent.tools.QueryState;
import com.vdt.finq.modules.agent.tools.financial.support.FigureLookup.Fetched;
import com.vdt.finq.modules.agent.tools.financial.support.FigureLookup;
import com.vdt.finq.modules.agent.tools.financial.support.FinancialRatio;
import com.vdt.finq.modules.agent.tools.financial.support.PeriodCode;
import com.vdt.finq.modules.agent.tools.financial.support.ReportPeriod;
import com.vdt.finq.modules.agent.tools.financial.support.Scope;
import com.vdt.finq.modules.agent.tools.financial.support.VnNumber;

import lombok.RequiredArgsConstructor;

// tool tỷ số có công thức: Java lấy đủ đầu vào (cả đầu kỳ để tính bình quân) rồi tính
@Component
@RequiredArgsConstructor
public class ComputeFinancialRatio {

    private static final BigDecimal TWO = BigDecimal.valueOf(2);

    private final FigureLookup lookup;

    @Tool(name = "compute_financial_ratio", description = """
            Compute a named financial ratio exactly from the financial-statements database, with its formula and
            input figures (averages use opening and closing balances). Ratios: GROSS_MARGIN, NET_MARGIN,
            DEBT_TO_ASSETS, DEBT_TO_EQUITY, BORROWINGS_TO_EQUITY, INTEREST_COVERAGE, ROE, ROA, ASSET_TURNOVER,
            FREE_CASH_FLOW, CAPEX, DSO, DIO, DPO, CCC. For a period shorter than a year the result is for that
            period, not annualized.""")
    public String computeFinancialRatio(
            @ToolParam(description = "The ratio to compute") FinancialRatio ratio,
            @ToolParam(description = "Calendar year, e.g. 2025") int year,
            @ToolParam(description = "Q1..Q4, H1, M9 or FY (same meaning as in get_financial_figures)") PeriodCode period,
            @ToolParam(required = false, description = "CONSOLIDATED (hợp nhất, default) or SEPARATE (riêng)") Scope scope,
            ToolContext toolContext) {
        QueryState state = QueryState.of(toolContext);
        long start = System.nanoTime();
        if (ratio == null || period == null) {
            return "Error: ratio and period are required.";
        }
        Calc c = new Calc(state.getEvidence(), new ReportPeriod(year, period), scope == null ? Scope.CONSOLIDATED : scope);
        String result = compute(ratio, c);
        StringBuilder out = new StringBuilder();
        c.inputs.forEach(f -> out.append('[').append(f.evidence().getId()).append("] ").append(f.evidence().getContent()).append('\n'));
        if (c.error != null) {
            out.append(c.error).append('\n');
        } else {
            Evidence e = state.getEvidence().addComputed(ratio.getLabel() + ", " + c.scope.getLabel() + ", "
                    + c.p.flowLabel() + ": " + result + ". Công thức: " + ratio.getFormula() + ". " + String.join(" ", c.notes)
                    + (c.p.period() == PeriodCode.FY ? "" : " Tính cho kỳ, không quy năm.") + " Java tính.");
            out.append('[').append(e.getId()).append("] ").append(e.getContent()).append('\n');
        }
        state.record("compute_financial_ratio(" + ratio + ", " + period + "/" + year + ", " + c.scope + ")",
                (System.nanoTime() - start) / 1_000_000, 0);
        return out.toString();
    }

    private static String compute(FinancialRatio ratio, Calc c) {
        return switch (ratio) {
            case GROSS_MARGIN -> c.percent(c.div(c.val("LOI_NHUAN_GOP"), c.val("DOANH_THU_THUAN")));
            case NET_MARGIN -> c.percent(c.div(c.val("LOI_NHUAN_SAU_THUE"), c.val("DOANH_THU_THUAN")));
            case DEBT_TO_ASSETS -> c.percent(c.div(c.val("NO_PHAI_TRA"), c.val("TONG_TAI_SAN")));
            case DEBT_TO_EQUITY -> c.times(c.div(c.val("NO_PHAI_TRA"), c.val("VON_CHU_SO_HUU_M400")));
            case BORROWINGS_TO_EQUITY -> c.times(c.div(c.add(c.val("VAY_NO_THUE_TC_NH"), c.val("VAY_NO_THUE_TC_DH")),
                    c.val("VON_CHU_SO_HUU_M400")));
            case INTEREST_COVERAGE -> {
                BigDecimal interest = c.val("CHI_PHI_DI_VAY");
                yield c.times(c.div(c.add(c.val("LOI_NHUAN_TRUOC_THUE"), interest), interest));
            }
            case ROE -> c.scope == Scope.CONSOLIDATED
                    ? c.percent(c.div(c.val("LNST_CONG_TY_ME"), c.avgEquityOfParent()))
                    : c.percent(c.div(c.val("LOI_NHUAN_SAU_THUE"), c.avg("VON_CHU_SO_HUU_M400")));
            case ROA -> c.percent(c.div(c.val("LOI_NHUAN_SAU_THUE"), c.avg("TONG_TAI_SAN")));
            case ASSET_TURNOVER -> c.times(c.div(c.val("DOANH_THU_THUAN"), c.avg("TONG_TAI_SAN")));
            case FREE_CASH_FLOW -> c.money(c.add(c.val("LCTT_THUAN_HDKD"), c.val("LCTT_CHI_MUA_TSCD")));
            case CAPEX -> c.money(c.neg(c.val("LCTT_CHI_MUA_TSCD")));
            case DSO -> c.days(c.daysOf("PHAI_THU_KHACH_HANG_NH", "DOANH_THU_THUAN"));
            case DIO -> c.days(c.daysOf("HANG_TON_KHO_M140", "GIA_VON_HANG_BAN"));
            case DPO -> c.days(c.daysOf("PHAI_TRA_NGUOI_BAN_NH", "GIA_VON_HANG_BAN"));
            case CCC -> {
                BigDecimal dso = c.daysOf("PHAI_THU_KHACH_HANG_NH", "DOANH_THU_THUAN");
                BigDecimal dio = c.daysOf("HANG_TON_KHO_M140", "GIA_VON_HANG_BAN");
                BigDecimal dpo = c.daysOf("PHAI_TRA_NGUOI_BAN_NH", "GIA_VON_HANG_BAN");
                if (c.error == null) {
                    c.notes.add("DSO " + VnNumber.days(dso) + ", DIO " + VnNumber.days(dio) + ", DPO " + VnNumber.days(dpo) + ".");
                }
                yield c.days(c.add(c.add(dso, dio), c.neg(dpo)));
            }
        };
    }

    // một lượt tính: gom đầu vào D#, thiếu số thì dừng và báo, không đoán
    private final class Calc {
        private final EvidenceStore store;
        private final ReportPeriod p;
        private final Scope scope;
        private final List<Fetched> inputs = new ArrayList<>();
        private final List<String> notes = new ArrayList<>();
        private String error;

        Calc(EvidenceStore store, ReportPeriod p, Scope scope) {
            this.store = store;
            this.p = p;
            this.scope = scope;
        }

        BigDecimal val(String code) {
            return at(code, p);
        }

        private BigDecimal at(String code, ReportPeriod period) {
            if (error != null) {
                return BigDecimal.ZERO;
            }
            Fetched f = lookup.fetch(store, code, period, scope);
            if (!f.ok()) {
                error = "CANNOT_COMPUTE: missing input. " + f.error();
                return BigDecimal.ZERO;
            }
            if (inputs.stream().noneMatch(x -> x.evidence().getId().equals(f.evidence().getId()))) {
                inputs.add(f);
            }
            return f.value();
        }

        // bình quân = (đầu kỳ + cuối kỳ) / 2 của chỉ tiêu thời điểm
        BigDecimal avg(String code) {
            BigDecimal open = at(code, p.opening());
            BigDecimal close = val(code);
            BigDecimal avg = open.add(close).divide(TWO, 2, RoundingMode.HALF_UP);
            if (error == null) {
                notes.add("Bình quân " + code + " = (đầu kỳ " + VnNumber.dong(open.longValue()) + " + cuối kỳ "
                        + VnNumber.dong(close.longValue()) + ") / 2 = " + VnNumber.money(avg) + ".");
            }
            return avg;
        }

        // lợi ích CĐ không kiểm soát (mã 429) bằng 0 từ 31/12/2023 nên không còn in; vắng thì coi 0 và ghi rõ
        private BigDecimal minorityAt(ReportPeriod period) {
            if (error != null) {
                return BigDecimal.ZERO;
            }
            Fetched f = lookup.fetch(store, "LOI_ICH_CDKKS", period, scope);
            if (f.ok()) {
                if (inputs.stream().noneMatch(x -> x.evidence().getId().equals(f.evidence().getId()))) {
                    inputs.add(f);
                }
                return f.value();
            }
            if (f.error().startsWith("NOT_FOUND")) {
                notes.add("Bảng cân đối hợp nhất " + period.pointLabel() + " không có dòng Lợi ích cổ đông không kiểm soát "
                        + "(mã 429), coi bằng 0.");
                return BigDecimal.ZERO;
            }
            error = "CANNOT_COMPUTE: missing input. " + f.error();
            return BigDecimal.ZERO;
        }

        // vốn chủ sở hữu thuộc cổ đông công ty mẹ, bình quân (hợp nhất)
        BigDecimal avgEquityOfParent() {
            BigDecimal open = at("VON_CHU_SO_HUU_M400", p.opening()).subtract(minorityAt(p.opening()));
            BigDecimal close = val("VON_CHU_SO_HUU_M400").subtract(minorityAt(p));
            BigDecimal avg = open.add(close).divide(TWO, 2, RoundingMode.HALF_UP);
            if (error == null) {
                notes.add("Vốn chủ sở hữu của cổ đông công ty mẹ bình quân = (đầu kỳ " + VnNumber.dong(open.longValue())
                        + " + cuối kỳ " + VnNumber.dong(close.longValue()) + ") / 2 = " + VnNumber.money(avg) + ".");
            }
            return avg;
        }

        BigDecimal daysOf(String balance, String flow) {
            BigDecimal d = div(avg(balance), val(flow));
            return error == null ? d.multiply(BigDecimal.valueOf(p.days())) : BigDecimal.ZERO;
        }

        BigDecimal div(BigDecimal a, BigDecimal b) {
            if (error != null) {
                return BigDecimal.ZERO;
            }
            if (b.signum() == 0) {
                error = "CANNOT_COMPUTE: the denominator is zero.";
                return BigDecimal.ZERO;
            }
            return a.divide(b, 10, RoundingMode.HALF_UP);
        }

        BigDecimal add(BigDecimal a, BigDecimal b) {
            return a.add(b);
        }

        BigDecimal neg(BigDecimal a) {
            return a.negate();
        }

        String percent(BigDecimal r) {
            return error == null ? VnNumber.percent(r) : null;
        }

        String times(BigDecimal r) {
            return error == null ? VnNumber.times(r) : null;
        }

        String money(BigDecimal v) {
            return error == null ? VnNumber.money(v) : null;
        }

        String days(BigDecimal v) {
            return error == null ? VnNumber.days(v) : null;
        }
    }
}
