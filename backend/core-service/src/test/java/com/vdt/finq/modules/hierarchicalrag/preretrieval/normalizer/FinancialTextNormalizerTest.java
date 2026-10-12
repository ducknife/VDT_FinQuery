package com.vdt.finq.modules.hierarchicalrag.preretrieval.normalizer;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import com.vdt.finq.modules.document.DocumentEnums.PhamVi;
import com.vdt.finq.modules.hierarchicalrag.preprocessing.datacleaner.NFCNormalization;

// 6 quy tắc chuẩn hoá (QĐ-027) trên câu hỏi mẫu; kỳ mới nhất trong kho là 30/06/2026
class FinancialTextNormalizerTest {

    private static final LocalDate LATEST = LocalDate.of(2026, 6, 30);

    private final FinancialTextNormalizer normalizer = new FinancialTextNormalizer(new NFCNormalization());

    private NormalizedQuery q(String question) {
        NormalizedQuery result = normalizer.normalizeQuery(question, LATEST);
        System.out.println(question + "\n  -> " + result.getText() + "\n     " + result.getApplied()
                + " amounts=" + result.getAmounts() + " company=" + result.getMaCongTy() + " scope=" + result.getPhamVi());
        return result;
    }

    // quy tắc 1: NFC + khoảng trắng (THAY, vì không đổi nghĩa)
    @Test
    void nfcAndSpaces() {
        String nfd = "Doanh thu  quý   3";   // "quý" viết dạng NFD, nhiều dấu cách
        assertThat(q(nfd).getText()).startsWith("Doanh thu quý 3");
    }

    // quy tắc 2: gõ không dấu -> thêm câu có dấu ở cuối; câu đã có dấu thì không đụng
    @Test
    void restoresDiacriticsForUnaccentedQuestion() {
        NormalizedQuery r = q("doanh thu thuan nam 2023 la bao nhieu");
        assertThat(r.getText()).startsWith("doanh thu thuan nam 2023 la bao nhieu (");
        assertThat(r.getText()).contains("doanh thu thuần năm 2023 là bao nhiêu");
        assertThat(q("Doanh thu năm 2023").getApplied()).doesNotContain("diacritics");
    }

    // quy tắc 3: viết tắt mở rộng bằng cách THÊM, giữ chữ gốc
    @Test
    void expandsAbbreviations() {
        NormalizedQuery r = q("LNST 2024 của CTR tăng bao nhiêu YoY?");
        assertThat(r.getText()).contains("LNST (lợi nhuận sau thuế)").contains("YoY (so với cùng kỳ năm trước)");
        // đã có dạng đầy đủ trong câu thì không thêm lần nữa
        assertThat(q("Lợi nhuận sau thuế (LNST) năm 2024").getText()).doesNotContain("LNST (lợi");
        // 3 ký tự trở lên: không phân biệt hoa thường
        assertThat(q("vcsh cuối năm 2025").getText()).contains("vcsh (vốn chủ sở hữu)");
    }

    // viết tắt nhập nhằng chỉ mở rộng khi có ngữ cảnh
    @Test
    void ambiguousAbbreviationsNeedContext() {
        assertThat(q("Tổng tài sản trên BCTC HN quý 2/2025").getText()).contains("HN (hợp nhất)");
        assertThat(q("Chi nhánh HN có bao nhiêu nhân viên").getText()).doesNotContain("hợp nhất");
        assertThat(q("Doanh thu so với KH năm 2025").getText()).contains("KH (kế hoạch)");
    }

    // quy tắc 4: cách gọi kỳ chuẩn
    @Test
    void canonicalPeriods() {
        assertThat(q("Doanh thu Q3/2024").getText()).contains("Q3/2024 (quý 3 năm 2024)");
        assertThat(q("Lợi nhuận quý III năm 2023").getText()).contains("quý III năm 2023 (quý 3 năm 2023)");
        assertThat(q("Báo cáo bán niên 2025").getText()).contains("bán niên 2025 (6 tháng đầu năm 2025)");
        assertThat(q("Doanh thu FY2023").getText()).contains("FY2023 (năm 2023)");
        // đã đúng dạng chuẩn thì không thêm
        assertThat(q("Doanh thu quý 3 năm 2024").getApplied()).noneMatch(a -> a.startsWith("period"));
        assertThat(q("Chi phí 6 tháng cuối năm 2024").getApplied()).noneMatch(a -> a.startsWith("period"));
    }

    // quy tắc 5: số tiền về đồng
    @Test
    void canonicalAmounts() {
        NormalizedQuery r = q("Những hợp đồng trên 8,2 nghìn tỷ hoặc 500tr");
        assertThat(r.getAmounts()).containsExactly(8_200_000_000_000L, 500_000_000L);
        assertThat(r.getText()).contains("8,2 nghìn tỷ (8.200.000.000.000 đồng)").contains("500tr (500.000.000 đồng)");
        assertThat(q("Doanh thu 8.195 tỷ đồng").getAmounts()).containsExactly(8_195_000_000_000L);
        assertThat(q("Khoản vay 1.234.567.890 VNĐ").getText()).contains("VNĐ (1.234.567.890 đồng)");
        assertThat(q("Tăng 12 phần trăm").getText()).contains("12 phần trăm (12%)");
        // "trăm" ghép với đơn vị lớn; "tỉ" viết chữ i
        assertThat(q("Hợp đồng 5 trăm triệu và 3 trăm tỷ").getAmounts()).containsExactly(500_000_000L, 300_000_000_000L);
        assertThat(q("Lợi nhuận 8 tỉ").getAmounts()).containsExactly(8_000_000_000L);
        assertThat(q("Tăng 12 phần trăm").getAmounts()).isEmpty();
    }

    // quy tắc 6: thời gian tương đối theo kỳ mới nhất (30/06/2026), không theo đồng hồ hệ thống
    @Test
    void relativeTimeFromLatestPeriod() {
        assertThat(q("Doanh thu năm nay").getText()).contains("năm nay (năm 2026)");
        assertThat(q("Lợi nhuận năm ngoái").getText()).contains("năm ngoái (năm 2025)");
        assertThat(q("Tổng tài sản quý trước").getText()).contains("quý trước (quý 1 năm 2026)");
        assertThat(q("Tiền mặt gần nhất").getText()).contains("gần nhất (kỳ 30/06/2026)");
    }

    // quy tắc 6: công ty, phạm vi mặc định khi câu hỏi không nêu
    @Test
    void contextDefaults() {
        NormalizedQuery none = q("Tổng tài sản năm 2025");
        assertThat(none.getMaCongTy()).isEqualTo("CTR");
        assertThat(none.isMaCongTyMacDinh()).isTrue();
        assertThat(none.getPhamVi()).isEqualTo(PhamVi.HOP_NHAT);
        assertThat(none.isPhamViMacDinh()).isTrue();

        NormalizedQuery own = q("Tổng tài sản báo cáo riêng của Viettel Construction năm 2025");
        assertThat(own.isMaCongTyMacDinh()).isFalse();
        assertThat(own.getPhamVi()).isEqualTo(PhamVi.RIENG);
        assertThat(own.isPhamViMacDinh()).isFalse();
    }

    // câu gốc giữ nguyên để hiện lại và đưa cho LLM
    @Test
    void keepsOriginal() {
        String question = "LNST năm ngoái?";
        assertThat(q(question).getOriginal()).isEqualTo(question);
    }

    // phía ngữ liệu: chỉ quy tắc 1, 3, 4, 5; không thêm dấu, không điền ngữ cảnh
    @Test
    void corpusMode() {
        String text = normalizer.normalizeCorpus("Chi phí thuế TNDN quý IV/2023 là 12 tỷ. Năm nay tăng.");
        System.out.println(text);
        assertThat(text).contains("TNDN (thu nhập doanh nghiệp)")
                .contains("quý IV/2023 (quý 4 năm 2023)")
                .contains("12 tỷ (12.000.000.000 đồng)")
                .doesNotContain("năm 2026");
    }
}
