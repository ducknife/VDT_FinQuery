package com.vdt.finq.modules.hierarchicalrag.generation;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

class CitationVerifierTest {

    private static final String SOURCE = "Doanh thu hợp nhất năm 2025 đạt 12.609.519 triệu đồng.\n\n"
            + "Nguyên nhân chủ yếu do mảng hạ tầng cho thuê tăng trưởng mạnh.";

    @Test
    void trichNguyenVanThiDung() {
        assertThat(CitationVerifier.quoteFound("đạt 12.609.519 triệu đồng", SOURCE)).isTrue();
    }

    @Test
    void noiHaiCauThatThiVanDung() {
        // hai câu thật, nằm ở hai đoạn khác nhau: mỗi đoạn có nguyên văn trong nguồn
        String quote = "Doanh thu hợp nhất năm 2025 đạt 12.609.519 triệu đồng. "
                + "Nguyên nhân chủ yếu do mảng hạ tầng cho thuê tăng trưởng mạnh.";
        assertThat(CitationVerifier.quoteFound(quote, SOURCE)).isTrue();
        assertThat(CitationVerifier.quoteFound("Doanh thu hợp nhất năm 2025 ... mảng hạ tầng cho thuê tăng", SOURCE))
                .isTrue();
    }

    @Test
    void motDoanBiaThiSai() {
        String quote = "Doanh thu hợp nhất năm 2025 đạt 12.609.519 triệu đồng. "
                + "Nguyên nhân chủ yếu do giá vốn giảm mạnh trong năm.";
        assertThat(CitationVerifier.quoteFound(quote, SOURCE)).isFalse();
    }

    @Test
    void boQuaGachDauDongVaHaiCham() {
        // ca thật (cổ tức 2024, 20:24): nguồn là tiêu đề markdown + gạch đầu dòng, model chép kèm ":" và "-"
        String source = "### Ngày thanh toán\n\n- 15/10/2025.";
        assertThat(CitationVerifier.quoteFound("- Ngày thanh toán: - 15/10/2025.", source)).isTrue();
        assertThat(CitationVerifier.quoteFound("- Ngày thanh toán: - 16/10/2025.", source)).isFalse();
    }

    @Test
    void soSaiThiSai() {
        assertThat(CitationVerifier.quoteFound("đạt 12.609.591 triệu đồng", SOURCE)).isFalse();
        assertThat(CitationVerifier.unverifiedNumbers("Doanh thu 12.609.591 triệu đồng [C1]", List.of(SOURCE)))
                .containsExactly("12.609.591");
    }
}
