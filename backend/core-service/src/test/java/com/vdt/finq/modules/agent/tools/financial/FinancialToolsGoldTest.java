package com.vdt.finq.modules.agent.tools.financial;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.vdt.finq.modules.agent.tools.QueryState;
import com.vdt.finq.modules.agent.tools.financial.support.FigureRef;
import com.vdt.finq.modules.agent.tools.financial.support.FinancialRatio;
import com.vdt.finq.modules.agent.tools.financial.support.PeriodCode;
import com.vdt.finq.modules.agent.tools.financial.support.PeriodInput;
import com.vdt.finq.modules.agent.tools.financial.support.Scope;

// gọi thẳng 3 tool số liệu (không qua LLM) và so với đáp án vàng của eval_test.yml (cần MySQL):
//     ./mvnw test "-Dtest=FinancialToolsGoldTest" "-Ddbtools=true"
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = { "cleaner.trial.enabled=false", "spring.devtools.restart.enabled=false" })
@EnabledIfSystemProperty(named = "dbtools", matches = "true")
class FinancialToolsGoldTest {

    private static final Scope HN = Scope.CONSOLIDATED;
    private static final Scope R = Scope.SEPARATE;

    @Autowired
    private GetFinancialFigures get;
    @Autowired
    private RelateTwoFigures relate;
    @Autowired
    private ComputeFinancialRatio ratio;

    private ToolContext ctx() {
        return new ToolContext(Map.of(QueryState.KEY, new QueryState()));
    }

    private String one(String code, int year, PeriodCode p, Scope scope) {
        String out = get.getFinancialFigures(List.of(code), List.of(new PeriodInput(year, p)), List.of(scope), ctx());
        System.out.println("---- " + code + " " + p + "/" + year + " " + scope + "\n" + out);
        return out;
    }

    @Test
    void motSo() {
        assertThat(one("DOANH_THU_THUAN", 2024, PeriodCode.FY, HN)).contains("12.609.519.560.081 đồng");          // D01
        assertThat(one("LOI_NHUAN_SAU_THUE", 2025, PeriodCode.FY, R)).contains("587.417.356.381 đồng");          // D02
        assertThat(one("NO_PHAI_TRA", 2026, PeriodCode.H1, HN)).contains("7.435.180.982.045 đồng");              // D03
        assertThat(one("HANG_TON_KHO_M140", 2025, PeriodCode.FY, HN)).contains("424.971.712.679 đồng");          // D04
        assertThat(one("LCTT_THUAN_HDKD", 2025, PeriodCode.Q1, HN)).contains("-295.939.415.278 đồng");           // D05
        assertThat(one("LOI_NHUAN_GOP", 2026, PeriodCode.H1, HN)).contains("521.996.574.332 đồng");              // D06
        assertThat(one("DOANH_THU_THUAN", 2026, PeriodCode.Q2, R)).contains("3.773.814.395.170 đồng");           // D07
        assertThat(one("LOI_NHUAN_SAU_THUE", 2024, PeriodCode.Q3, HN)).contains("146.262.730.079 đồng");         // D08
        assertThat(one("DOANH_THU_THUAN", 2019, PeriodCode.FY, HN))                                              // D09
                .contains("5.099.889.976.331 đồng").contains("5.053.710.724.827 đồng");
        assertThat(one("TONG_TAI_SAN", 2024, PeriodCode.FY, HN)).contains("7.106.257.863.668 đồng");             // D10
        assertThat(one("TIEN_VA_TUONG_DUONG_TIEN", 2026, PeriodCode.Q1, HN)).contains("1.123.253.891.417 đồng"); // D11
    }

    @Test
    void soSanh() {
        String c01 = get.getFinancialFigures(List.of("DOANH_THU_THUAN"),
                List.of(new PeriodInput(2025, PeriodCode.FY), new PeriodInput(2024, PeriodCode.FY)), null, ctx());
        System.out.println("---- C01\n" + c01);
        assertThat(c01).contains("13.939,52 tỷ đồng").contains("12.609,52 tỷ đồng").contains("tăng").contains("10,55%");

        String c02 = ratio.computeFinancialRatio(FinancialRatio.GROSS_MARGIN, 2024, PeriodCode.FY, HN, ctx());
        System.out.println("---- C02\n" + c02);
        assertThat(c02).contains("7,01%");

        String c03 = ratio.computeFinancialRatio(FinancialRatio.DEBT_TO_ASSETS, 2025, PeriodCode.FY, HN, ctx());
        System.out.println("---- C03\n" + c03);
        assertThat(c03).contains("74,75%");

        String c04 = get.getFinancialFigures(List.of("LOI_NHUAN_SAU_THUE"),
                List.of(new PeriodInput(2026, PeriodCode.H1), new PeriodInput(2025, PeriodCode.H1)), null, ctx());
        System.out.println("---- C04\n" + c04);
        assertThat(c04).contains("tăng").contains("54,88 tỷ đồng").contains("20,56%");

        String c05 = relate.relateTwoFigures(new FigureRef("LCTT_THUAN_HDKD", 2025, PeriodCode.FY, HN),
                new FigureRef("LOI_NHUAN_SAU_THUE", 2025, PeriodCode.FY, HN), RelateTwoFigures.Relation.RATIO, ctx());
        System.out.println("---- C05\n" + c05);
        assertThat(c05).contains("2,43 lần");

        String c06 = get.getFinancialFigures(List.of("HANG_TON_KHO_M140"),
                List.of(new PeriodInput(2024, PeriodCode.FY), new PeriodInput(2025, PeriodCode.FY)), null, ctx());
        System.out.println("---- C06\n" + c06);
        assertThat(c06).contains("giảm").contains("185,51 tỷ đồng").contains("30,39%");

        String c07 = relate.relateTwoFigures(new FigureRef("DOANH_THU_THUAN", 2025, PeriodCode.FY, R),
                new FigureRef("DOANH_THU_THUAN", 2025, PeriodCode.FY, HN), RelateTwoFigures.Relation.SHARE, ctx());
        System.out.println("---- C07\n" + c07);
        assertThat(c07).contains("84,59%");
    }

    @Test
    void tuChoiVaBien() {
        assertThat(one("DOANH_THU_THUAN", 2026, PeriodCode.Q3, HN)).contains("NOT_AVAILABLE").contains("30/06/2026"); // X03
        assertThat(one("DOANH_THU_THUAN", 2026, PeriodCode.FY, HN)).contains("NOT_AVAILABLE");                        // X06
        assertThat(one("LOI_NHUAN_SAU_THUE", 2015, PeriodCode.FY, HN)).contains("NOT_AVAILABLE");                     // X04
        assertThat(one("DOANH_THU_FPT", 2025, PeriodCode.FY, HN)).contains("UNKNOWN_INDICATOR");
        // dòng tiền riêng quý 2/2025: kho chỉ có luỹ kế, Java tính 6T - Q1 và trích cả hai nguồn
        String q2 = one("LCTT_THUAN_HDKD", 2025, PeriodCode.Q2, HN);
        assertThat(q2).contains("Java tính").contains("luỹ kế");
        String roe = ratio.computeFinancialRatio(FinancialRatio.ROE, 2025, PeriodCode.FY, HN, ctx());
        System.out.println("---- ROE 2025\n" + roe);
        assertThat(roe).contains("ROE").contains("%").doesNotContain("CANNOT_COMPUTE");
        String ccc = ratio.computeFinancialRatio(FinancialRatio.CCC, 2025, PeriodCode.FY, HN, ctx());
        System.out.println("---- CCC 2025\n" + ccc);
        assertThat(ccc).contains("ngày").doesNotContain("CANNOT_COMPUTE");
    }
}
