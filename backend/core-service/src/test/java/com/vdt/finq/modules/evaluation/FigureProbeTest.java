package com.vdt.finq.modules.evaluation;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.vdt.finq.modules.agent.tools.QueryState;
import com.vdt.finq.modules.dbquery.GetFinancialFigures;
import com.vdt.finq.modules.dbquery.support.PeriodCode;
import com.vdt.finq.modules.dbquery.support.PeriodInput;

// in kết quả get_financial_figures cho từng chỉ tiêu@kỳ (không gọi LLM), để soát DB thiếu số:
//     ./mvnw test "-Dtest=FigureProbeTest" "-Dfigprobe=LOI_NHUAN_SAU_THUE@2024Q2;LOI_NHUAN_SAU_THUE@2024H1"
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = { "cleaner.trial.enabled=false", "answer.trial.enabled=false", "spring.devtools.restart.enabled=false" })
@EnabledIfSystemProperty(named = "figprobe", matches = ".+")
class FigureProbeTest {

    @Autowired
    private GetFinancialFigures getFinancialFigures;

    @Autowired
    private org.springframework.jdbc.core.JdbcTemplate jdbc;

    // mọi kỳ đang có của một chỉ tiêu hợp nhất, kèm tài liệu nguồn: "-Dfigprobe.periods=LOI_NHUAN_SAU_THUE"
    @Test
    void periods() {
        String code = System.getProperty("figprobe.periods");
        if (code == null) {
            return;
        }
        jdbc.queryForList("SELECT f.tu_ngay, f.den_ngay, f.tai_lieu_id, f.trang_thai FROM financial_figures f WHERE f.ma_noi_bo = ? "
                + "AND f.pham_vi = 'hop_nhat' ORDER BY f.den_ngay, f.tu_ngay", code)
                .forEach(r -> System.out.println("[KY] " + r.get("tu_ngay") + " -> " + r.get("den_ngay") + " | " + r.get("tai_lieu_id")
                        + " | " + r.get("trang_thai")));
    }

    // BCTC trong kho tài liệu và số dòng đã trích vào financial_figures: "-Dfigprobe.coverage=true"
    @Test
    void coverage() {
        if (!Boolean.getBoolean("figprobe.coverage")) {
            return;
        }
        jdbc.queryForList("SELECT d.tai_lieu_id, COUNT(f.tai_lieu_id) AS n FROM documents d LEFT JOIN financial_figures f "
                + "ON f.tai_lieu_id = d.tai_lieu_id WHERE d.tai_lieu_id LIKE 'CTR-BCTC-%' GROUP BY d.tai_lieu_id ORDER BY d.tai_lieu_id")
                .forEach(r -> System.out.println("[COV] " + r.get("tai_lieu_id") + " " + r.get("n")));
    }

    @Test
    void probe() {
        for (String item : System.getProperty("figprobe").split(";")) {
            String[] p = item.strip().split("@");
            PeriodInput period = new PeriodInput(Integer.parseInt(p[1].substring(0, 4)), PeriodCode.valueOf(p[1].substring(4)));
            ToolContext ctx = new ToolContext(Map.of(QueryState.KEY, new QueryState()));
            System.out.println("[FIG] " + item + " -> " + getFinancialFigures.getFinancialFigures(List.of(p[0]), List.of(period), null, ctx)
                    .replace('\n', ' '));
        }
    }
}
