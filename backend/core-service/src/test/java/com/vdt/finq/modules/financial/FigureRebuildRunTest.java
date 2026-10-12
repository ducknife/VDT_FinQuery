package com.vdt.finq.modules.financial;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeSet;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

// trích lại cả kho và in khác biệt; GHI THẬT vào financial_figures:
//     ./mvnw test "-Dtest=FigureRebuildRunTest" "-Dextract=true"
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = { "cleaner.trial.enabled=false", "spring.devtools.restart.enabled=false" })
@EnabledIfSystemProperty(named = "extract", matches = "true")
class FigureRebuildRunTest {

    private static final String SNAPSHOT = "SELECT CONCAT(ma_noi_bo, '|', tu_ngay, '|', den_ngay, '|', pham_vi) k, "
            + "gia_tri, tai_lieu_id, trang FROM financial_figures WHERE trang_thai = 'hieu_luc'";

    @Autowired
    private FinancialExtractionService service;
    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void rebuildAndDiff() {
        Map<String, String> before = snapshot();
        ExtractionSummary summary = service.rebuildAll();
        Map<String, String> after = snapshot();

        TreeSet<String> changed = new TreeSet<>();
        TreeSet<String> removed = new TreeSet<>();
        TreeSet<String> added = new TreeSet<>();
        before.forEach((k, v) -> {
            String now = after.get(k);
            if (now == null) {
                removed.add(k + "  " + v);
            } else if (!value(now).equals(value(v))) {
                changed.add(k + "  " + v + "  ->  " + now);
            }
        });
        after.keySet().stream().filter(k -> !before.containsKey(k)).forEach(k -> added.add(k + "  " + after.get(k)));

        System.out.println("TÓM TẮT: " + summary);
        System.out.printf("trước %d số, sau %d số: đổi giá trị %d, bị bỏ %d, mới %d%n",
                before.size(), after.size(), changed.size(), removed.size(), added.size());
        System.out.println("== ĐỔI GIÁ TRỊ (khoá | giá trị cũ nguồn cũ -> giá trị mới nguồn mới)");
        changed.forEach(System.out::println);
        System.out.println("== BỊ BỎ");
        removed.forEach(System.out::println);
        System.out.println("== MỚI");
        added.forEach(System.out::println);
    }

    private Map<String, String> snapshot() {
        Map<String, String> out = new HashMap<>();
        jdbc.query(SNAPSHOT, rs -> {
            out.put(rs.getString("k"), rs.getLong("gia_tri") + " @" + rs.getString("tai_lieu_id") + " tr." + rs.getInt("trang"));
        });
        return out;
    }

    private static String value(String s) {
        return s.substring(0, s.indexOf(' '));
    }
}
