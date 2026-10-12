package com.vdt.finq.modules.hierarchicalrag.preretrieval.filter;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import com.vdt.finq.modules.document.DocumentEnums.Ky;
import com.vdt.finq.modules.document.DocumentEnums.LoaiTaiLieu;
import com.vdt.finq.modules.document.DocumentEnums.PhamVi;
import com.vdt.finq.modules.hierarchicalrag.preprocessing.datacleaner.NFCNormalization;
import com.vdt.finq.modules.hierarchicalrag.preretrieval.normalizer.FinancialTextNormalizer;
import com.vdt.finq.modules.hierarchicalrag.preretrieval.normalizer.NormalizedQuery;
import com.vdt.finq.modules.hierarchicalrag.retrieval.QdrantFilters;

import io.qdrant.client.grpc.Points.Filter;

// trích bộ lọc (QĐ-030) trên câu hỏi mẫu; kỳ mới nhất trong kho là 30/06/2026.
// chạy và xem in ra:  ./mvnw test -Dtest=FilterExtractorTest
class FilterExtractorTest {

    private static final LocalDate LATEST = LocalDate.of(2026, 6, 30);

    private final FinancialTextNormalizer normalizer = new FinancialTextNormalizer(new NFCNormalization());
    private final FilterExtractor extractor = new FilterExtractor();

    private QueryFilter f(String question) {
        NormalizedQuery normalized = normalizer.normalizeQuery(question, LATEST);
        QueryFilter filter = extractor.extract(normalized);
        System.out.println(question + "\n  chuẩn hoá: " + normalized.getText() + "\n  bộ lọc:    " + filter.getApplied());
        return filter;
    }

    @Test
    void quarterAndYear() {
        QueryFilter q = f("Doanh thu thuần Q3/2024 là bao nhiêu?");
        assertThat(q.getNams()).containsExactly(2024);
        assertThat(q.getKys()).containsExactly(Ky.Q3);
        assertThat(q.getPhamVi()).isEqualTo(PhamVi.HOP_NHAT);
        assertThat(q.getMaCongTy()).isEqualTo("CTR");
        assertThat(q.getLoais()).isEmpty();
        // số La Mã: bộ chuẩn hoá đã thêm "(quý 4 năm 2023)"
        assertThat(f("Lợi nhuận quý IV/2023").getKys()).containsExactly(Ky.Q4);
    }

    // thời gian tương đối đã được bộ chuẩn hoá đổi ra năm, kỳ cụ thể
    @Test
    void relativeTime() {
        assertThat(f("LNST năm ngoái").getNams()).containsExactly(2025);
        QueryFilter latest = f("Tiền mặt gần nhất");
        assertThat(latest.getNams()).containsExactly(2026);
        assertThat(latest.getKys()).containsExactlyInAnyOrder(Ky.Q2, Ky.BAN_NIEN);
    }

    @Test
    void halfYearAndFullYear() {
        assertThat(f("Doanh thu bán niên 2025").getKys()).containsExactlyInAnyOrder(Ky.BAN_NIEN, Ky.Q2);
        QueryFilter yearEnd = f("Tổng tài sản cuối năm 2023 trên báo cáo riêng");
        assertThat(yearEnd.getKys()).containsExactlyInAnyOrder(Ky.NAM, Ky.Q4);
        assertThat(yearEnd.getPhamVi()).isEqualTo(PhamVi.RIENG);
    }

    // so sánh nhiều năm: lọc cả hai năm
    @Test
    void severalYears() {
        assertThat(f("Doanh thu 2024 so với 2023 tăng bao nhiêu?").getNams()).containsExactly(2023, 2024);
    }

    // loại tài liệu chỉ lọc khi người hỏi gọi đúng tên
    @Test
    void documentTypeOnlyWhenNamed() {
        assertThat(f("Cổ tức năm 2024 theo nghị quyết ĐHĐCĐ").getLoais()).containsExactly(LoaiTaiLieu.NGHI_QUYET);
        assertThat(f("Báo cáo thường niên 2023 nói gì về chiến lược?").getLoais()).containsExactly(LoaiTaiLieu.BCTN);
        assertThat(f("Cổ tức năm 2024 là bao nhiêu?").getLoais()).isEmpty();
    }

    // không nói rõ thì không lọc; số tiền không bị hiểu nhầm là năm
    @Test
    void noFilterWhenNotStated() {
        QueryFilter q = f("Chiến lược phát triển của công ty là gì?");
        assertThat(q.getNams()).isEmpty();
        assertThat(q.getKys()).isEmpty();
        assertThat(q.getLoais()).isEmpty();
        assertThat(f("Doanh thu 2.024 tỷ đồng").getNams()).isEmpty();
    }

    // bộ lọc dịch sang Filter của Qdrant: in ra để xem đúng cấu trúc
    @Test
    void toQdrantFilter() {
        QueryFilter q = f("Doanh thu quý 3 năm 2024 trên báo cáo tài chính");
        Filter strict = QdrantFilters.strict(q);
        System.out.println("  Qdrant (chặt):\n" + strict);
        // "báo cáo tài chính" không còn là tín hiệu lọc loại (benchmark 12/10, câu V01)
        assertThat(q.getLoais()).isEmpty();
        // công ty, năm (đúng năm, hoặc Y+1 với văn bản không có kỳ), kỳ (hoặc không có kỳ), phạm vi (hoặc trống)
        assertThat(strict.getMustCount()).isEqualTo(4);
        assertThat(strict.getMust(1).getFilter().getShouldCount()).isEqualTo(2);
        assertThat(QdrantFilters.relaxed(q).getMustCount()).isEqualTo(1);
    }

    // năm ghi trên tài liệu là năm ban hành: văn bản sự kiện nhận Y..Y+2; báo cáo định kỳ thêm Y-1 khi hỏi
    // kế hoạch, thêm Y+1 khi hỏi số trình bày lại (benchmark 12/10/2026: V08, V10, D09)
    @Test
    void documentYearIsPublicationYear() {
        QueryFilter dividend = f("Tổng tỷ lệ cổ tức năm 2022 công ty đã chi trả là bao nhiêu?");
        assertThat(dividend.getPeriodNams()).containsExactly(2022);
        assertThat(dividend.getEventNams()).containsExactly(2022, 2023, 2024);
        assertThat(f("Khẩu hiệu và định hướng trọng tâm năm 2025 mà HĐQT đề ra là gì?").getPeriodNams())
                .containsExactly(2024, 2025);
        assertThat(f("Doanh thu thuần hợp nhất năm 2019 (theo số liệu đã trình bày lại) là bao nhiêu?").getPeriodNams())
                .containsExactly(2019, 2020);
        // "đến nay" kéo tới năm mới nhất trong kho
        NormalizedQuery untilNow = normalizer.normalizeQuery("Vốn điều lệ thay đổi thế nào từ năm 2022 đến nay?", LATEST);
        assertThat(extractor.extract(untilNow, 2026).getNams()).containsExactly(2022, 2023, 2024, 2025, 2026);
    }

    // "hợp nhất riêng quý 3": nói rõ hợp nhất thì "riêng" là "chỉ riêng", không phải báo cáo riêng (câu D08)
    @Test
    void consolidatedWinsOverOnly() {
        assertThat(f("Lãi ròng hợp nhất riêng quý 3/2024 là bao nhiêu?").getPhamVi()).isEqualTo(PhamVi.HOP_NHAT);
        assertThat(f("Tổng tài sản trên báo cáo riêng năm 2023").getPhamVi()).isEqualTo(PhamVi.RIENG);
    }

    // câu V01 của bộ đánh giá: chữ "báo cáo tài chính riêng" chỉ nói phạm vi, không lọc loại tài liệu
    @Test
    void financialStatementWordsDoNotFilterDocumentType() {
        QueryFilter q = f("Vì sao lợi nhuận sau thuế trên báo cáo tài chính riêng quý 2/2024 tăng so với cùng kỳ?");
        assertThat(q.getLoais()).isEmpty();
        assertThat(q.getPhamVi()).isEqualTo(PhamVi.RIENG);
    }
}
