package com.vdt.finq.modules.rag.preretrieval.keyword;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.vdt.finq.modules.rag.preprocessing.datacleaner.NFCNormalization;
import com.vdt.finq.modules.rag.preretrieval.normalizer.FinancialTextNormalizer;

// trích từ khoá trên câu mẫu, không cần DB:  ./mvnw test -Dtest=KeywordExtractorTest
class KeywordExtractorTest {

    private static final LocalDate LATEST = LocalDate.of(2026, 6, 30);

    private static final List<String> REPORT_NAMES = List.of(
            "Doanh thu thuần về bán hàng và cung cấp dịch vụ",
            "Doanh thu hoạt động tài chính",
            "Chi phí tài chính",
            "Chi phí khác",
            "Lợi nhuận sau thuế TNDN",
            "Lợi nhuận sau thuế chưa phân phối",
            "Hàng tồn kho — mã 140 (thuần, đã trừ dự phòng)",
            "Tài sản ngắn hạn",
            "Tổng cộng tài sản",
            "LCTT — Tiền thu từ đi vay",
            "Tiền");

    private final FinancialTextNormalizer normalizer = new FinancialTextNormalizer(new NFCNormalization());
    private final KeywordExtractor extractor = new KeywordExtractor(REPORT_NAMES, KeywordExtractor.loadAliases());

    private List<String> keywords(String question) {
        List<String> result = extractor.extract(normalizer.normalizeQuery(question, LATEST));
        System.out.println(question + "\n  -> từ khoá: " + result);
        return result;
    }

    // cách gọi đời thường -> tên đúng như báo cáo viết
    @Test
    void everydayWordsBecomeReportNames() {
        assertThat(keywords("Lãi ròng quý 3/2024 của CTR là bao nhiêu?"))
                .contains("lợi nhuận sau thuế thu nhập doanh nghiệp");
        assertThat(keywords("Doanh thu năm 2023"))
                .contains("doanh thu thuần về bán hàng và cung cấp dịch vụ");
        assertThat(keywords("Công ty có bao nhiêu nhân viên?"))
                .contains("số lượng lao động");
    }

    // viết tắt đã được bộ chuẩn hoá mở rộng, nên từ khoá bắt được cả "LNST"
    @Test
    void abbreviationsWorkThroughNormalizer() {
        assertThat(keywords("LNST năm 2024")).contains("lợi nhuận sau thuế thu nhập doanh nghiệp");
    }

    // tên chỉ tiêu có sẵn trong DB khớp nguyên cụm; "Hàng tồn kho — mã 140 (...)" được làm gọn
    @Test
    void reportNamesFromDatabase() {
        assertThat(keywords("Chi phí tài chính quý 2/2025")).containsExactly("chi phí tài chính");
        assertThat(keywords("Hàng tồn kho cuối năm 2025")).containsExactly("hàng tồn kho");
    }

    // "tài sản" trong "tài sản ngắn hạn" không thêm "tổng cộng tài sản"; "tổng tài sản" thì có
    @Test
    void everydayWordInsideLongerNameIsIgnored() {
        assertThat(keywords("Tài sản ngắn hạn năm 2024")).containsExactly("tài sản ngắn hạn");
        assertThat(keywords("Tổng tài sản năm 2024")).contains("tổng cộng tài sản");
        // "doanh thu" trong "doanh thu hoạt động tài chính" không kéo theo "doanh thu thuần"
        assertThat(keywords("Doanh thu hoạt động tài chính 2024")).containsExactly("doanh thu hoạt động tài chính");
    }

    // câu không liên quan tài chính: không có từ khoá nào, không đoán bừa
    @Test
    void nothingToExtract() {
        assertThat(keywords("Thời tiết hôm nay thế nào?")).isEmpty();
    }

    @Test
    void cleansReportNames() {
        assertThat(KeywordExtractor.cleanName("LCTT — Tiền thu từ đi vay")).isEqualTo("Tiền thu từ đi vay");
        assertThat(KeywordExtractor.cleanName("Hàng tồn kho — mã 140 (thuần, đã trừ dự phòng)")).isEqualTo("Hàng tồn kho");
        assertThat(KeywordExtractor.cleanName("TSCĐ hữu hình — nguyên giá")).isEqualTo("TSCĐ hữu hình nguyên giá");
        assertThat(KeywordExtractor.cleanName("Trong đó: chi phí đi vay")).isEqualTo("chi phí đi vay");
    }
}
