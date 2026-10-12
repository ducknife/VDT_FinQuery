package com.vdt.finq.common.text;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class EnglishFilterTest {

    private static final String VI = "Lợi nhuận sau thuế hợp nhất năm 2025 đạt 599,72 tỷ đồng, tăng 15,5% so với năm trước.";
    private static final String EN = "Consolidated profit after tax for the year 2025 reached VND 599.72 billion, which is an "
            + "increase of 15.5% compared to the previous year and is in line with the plan.";

    @Test
    void trangThuanTiengAnhBiLoai() {
        String page = (EN + "\n\n").repeat(4);
        assertThat(EnglishFilter.isEnglishChunk(page)).isTrue();
    }

    @Test
    void trangTiengVietKhongBiLoai() {
        String page = (VI + "\n\n").repeat(6);
        assertThat(EnglishFilter.isEnglishChunk(page)).isFalse();
    }

    @Test
    void trangSongNguBoDoanTiengAnhGiuTiengViet() {
        String page = "# BÁO CÁO TÀI CHÍNH\n\n" + VI + "\n" + EN + "\n\n" + VI;
        // đoạn VI và EN liền nhau (không dòng trống) thì gộp làm một đoạn có dấu, nên giữ
        assertThat(EnglishFilter.stripEnglish(page)).contains(VI);

        String separated = "# BÁO CÁO TÀI CHÍNH\n\n" + VI + "\n\n" + EN + "\n\n" + VI;
        String clean = EnglishFilter.stripEnglish(separated);
        assertThat(clean).doesNotContain("Consolidated profit").contains(VI).contains("# BÁO CÁO TÀI CHÍNH");
    }

    @Test
    void dongBangLuonGiu() {
        String page = "| Chỉ tiêu | Item | 2025 |\n|---|---|---|\n| Doanh thu | Revenue of the year from sales and services | 12.609.519 |\n\n" + EN;
        String clean = EnglishFilter.stripEnglish(page);
        assertThat(clean).contains("| Doanh thu | Revenue of the year from sales and services | 12.609.519 |")
                .doesNotContain("Consolidated profit");
    }

    @Test
    void doanNganKhongBiBo() {
        String page = VI + "\n\nTotal assets\n\n" + VI;
        assertThat(EnglishFilter.stripEnglish(page)).contains("Total assets");
    }
}
