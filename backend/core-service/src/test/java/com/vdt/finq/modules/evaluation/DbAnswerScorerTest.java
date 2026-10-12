package com.vdt.finq.modules.evaluation;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

class DbAnswerScorerTest {

    private static boolean ok(String answer, Map<String, Object> cham) {
        return DbAnswerScorer.score(answer, cham).correct();
    }

    @Test
    void soTien() {
        Map<String, Object> d01 = Map.of("kieu", "so_dong", "gia_tri", 12609519560081L);
        assertThat(ok("Doanh thu thuần hợp nhất năm 2024 là 12.609.519.560.081 đồng [D1].", d01)).isTrue();
        assertThat(ok("Doanh thu đạt 12.609,52 tỷ đồng.", d01)).isTrue();
        assertThat(ok("Doanh thu đạt 12.609,5 tỷ đồng.", d01)).isFalse();
        assertThat(ok("Doanh thu đạt 12.609.519.560.082 đồng.", d01)).isFalse();
    }

    @Test
    void soAm() {
        Map<String, Object> d05 = Map.of("kieu", "so_dong", "gia_tri", -295939415278L, "am", true);
        assertThat(ok("Âm, -295.939.415.278 đồng.", d05)).isTrue();
        assertThat(ok("Dòng tiền âm 295.939.415.278 đồng.", d05)).isTrue();
        assertThat(ok("Dòng tiền là 295.939.415.278 đồng.", d05)).isFalse();
    }

    @Test
    void phanTramVaLan() {
        assertThat(ok("tăng 10,55% so với 2024", Map.of("kieu", "phan_tram", "gia_tri", "10,55", "huong", "tang"))).isTrue();
        assertThat(ok("giảm 185,51 tỷ đồng (30,39%)", Map.of("kieu", "phan_tram", "gia_tri", "30,39", "huong", "giam"))).isTrue();
        assertThat(ok("thay đổi 30,39%", Map.of("kieu", "phan_tram", "gia_tri", "30,39", "huong", "giam"))).isFalse();
        assertThat(ok("gấp 2,43 lần", Map.of("kieu", "lan", "gia_tri", "2,43"))).isTrue();
        assertThat(ok("gấp 12,43 lần", Map.of("kieu", "lan", "gia_tri", "2,43"))).isFalse();
    }

    @Test
    void tuChoi() {
        Map<String, Object> x06 = Map.of("kieu", "tu_choi", "so_duoc_phep", List.of(7958838568947L));
        assertThat(ok("Chưa có số cả năm 2026; 6 tháng đầu năm là 7.958.838.568.947 đồng.", x06)).isTrue();
        assertThat(ok("Doanh thu cả năm 2026 ước đạt 15.900 tỷ đồng.", x06)).isFalse();
        Map<String, Object> x03 = Map.of("kieu", "tu_choi", "so_duoc_phep", List.of());
        assertThat(ok("Kỳ mới nhất là 30/06/2026, chưa có quý 3/2026.", x03)).isTrue();
        assertThat(ok("Doanh thu quý 3/2026 là 3.512.000.000.000 đồng.", x03)).isFalse();
    }
}
