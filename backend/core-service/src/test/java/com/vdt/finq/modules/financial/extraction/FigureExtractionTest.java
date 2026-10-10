package com.vdt.finq.modules.financial.extraction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.vdt.finq.common.markdown.MarkdownTableParser;
import com.vdt.finq.modules.financial.FinancialEnums.MauBieu;

// các ca lấy từ trang OCR thật trong kho (.docs/projectdevlogs/tien-xu-ly_v1.2.md, mục 13)
class FigureExtractionTest {

    @Test
    void parsesVietnameseNumbers() {
        assertThat(VietnameseNumberParser.parse("5.627.168.242.619")).isEqualTo(5_627_168_242_619L);
        assertThat(VietnameseNumberParser.parse("(103.552.387.501)")).isEqualTo(-103_552_387_501L);
        assertThat(VietnameseNumberParser.parse("**1.143.858.790.000**")).isEqualTo(1_143_858_790_000L);
        assertThat(VietnameseNumberParser.parse("-")).isZero();
        assertThat(VietnameseNumberParser.parse("")).isNull();
        // OCR đọc sai nhóm nghìn: phải lộ ra, không được đoán
        assertThatThrownBy(() -> VietnameseNumberParser.parse("104.302.74.795")).isInstanceOf(NumberFormatException.class);
        assertThatThrownBy(() -> VietnameseNumberParser.parse("(1047642.776.397)")).isInstanceOf(NumberFormatException.class);
    }

    @Test
    void readsPeriodLabels() {
        assertThat(PeriodLabel.parse("31/12/2023 VND").getPeriod()).isEqualTo(Period.point(LocalDate.of(2023, 12, 31)));
        // số dư 01/01 quy về 31/12 năm trước, có đánh dấu trình bày lại
        PeriodLabel opening = PeriodLabel.parse("01/01/2024 (Đã điều chỉnh) VND");
        assertThat(opening.getPeriod()).isEqualTo(Period.point(LocalDate.of(2023, 12, 31)));
        assertThat(opening.isRestated()).isTrue();
        assertThat(PeriodLabel.parse("Quý IV/2023").getPeriod()).isEqualTo(Period.quarter(2023, 4));
        assertThat(PeriodLabel.parse("Số liệu lũy kế từ đầu năm đến cuối kỳ Quý 4/2019").getPeriod())
                .isEqualTo(Period.year(2019));
        assertThat(PeriodLabel.parse("9 tháng đầu năm 2023 VND").getPeriod())
                .isEqualTo(Period.range(LocalDate.of(2023, 1, 1), LocalDate.of(2023, 9, 30)));
        assertThat(PeriodLabel.parse("Từ 01/01/2019 đến 30/06/2019").getPeriod())
                .isEqualTo(Period.range(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 6, 30)));
        assertThat(PeriodLabel.parse("Kỳ này VND").getPeriod()).isNull();
    }

    // trang 5, BCTC hợp nhất quý 4/2023
    @Test
    void readsBalanceSheetTable() {
        String page = """
                ## BẢNG CÂN ĐỐI KẾ TOÁN HỢP NHẤT
                | Mã số | TÀI SẢN | Thuyết minh | 31/12/2023 VND | 01/01/2023 Đã điều chỉnh VND |
                |-------|---------|-------------|----------------|-----------------------------|
                | 100   | A. TÀI SẢN NGẮN HẠN | | 5.627.168.242.619 | 5.009.963.242.010 |
                | 221   | 1. Tài sản cố định hữu hình | 11 | 261.538.671.066 | 345.044.655.145 |
                |       | - Nguyên giá | | 740.194.216.248 | 717.717.320.115 |
                |       | - Giá trị hao mòn lũy kế | | (478.655.545.182) | (372.672.664.970) |
                """;
        StatementTable table = StatementTableReader.read(MarkdownTableParser.tables(page).get(0));
        assertThat(table.getValueCount()).isEqualTo(2);
        // dòng con không ghi mã được suy từ dòng cha 221
        assertThat(table.getRows()).extracting(StatementTable.Row::getCode).containsExactly("100", "221", "222", "223");
        assertThat(table.getRows().get(3).getValues()).containsExactly("(478.655.545.182)", "(372.672.664.970)");

        ColumnPlanner.Plan plan = ColumnPlanner.plan(true, LocalDate.of(2023, 12, 31), table, page);
        assertThat(plan.getColumns()).extracting(ColumnPlanner.Column::getPeriod).containsExactly(
                Period.point(LocalDate.of(2023, 12, 31)), Period.point(LocalDate.of(2022, 12, 31)));
        assertThat(plan.getColumns().get(0).isCurrent()).isTrue();
        assertThat(plan.getColumns().get(1).isRestated()).isTrue();
    }

    // trang 9, BCTC hợp nhất bán niên 2019: OCR gộp ô "Mã số CHỈ TIÊU", mã dính đầu ô tên
    @Test
    void readsCodeEmbeddedInNameCell() {
        String page = """
                | Mã số CHỈ TIÊU | Thuyết minh | 6 tháng đầu năm 2019 VND | 6 tháng đầu năm 2018 VND |
                |----------------|-------------|---------------------------|---------------------------|
                | 01 1. Doanh thu bán hàng | 22 | 2.421.351.766.568 | 2.016.647.485.997 |
                | 10 3. Doanh thu thuần về bán hàng và cung cấp dịch vụ | | 2.421.351.766.568 | 2.016.647.485.997 |
                | 11 4. Giá vốn hàng bán | 23 | 2.300.418.484.745 | 1.897.362.947.517 |
                """;
        StatementTable table = StatementTableReader.read(MarkdownTableParser.tables(page).get(0));
        assertThat(table.getRows()).extracting(StatementTable.Row::getCode).containsExactly("01", "10", "11");
        assertThat(table.getRows().get(0).getName()).isEqualTo("1. Doanh thu bán hàng");
        // ô thuyết minh "22" không bị lấy làm giá trị
        assertThat(table.getRows().get(0).getValues()).containsExactly("2.421.351.766.568", "2.016.647.485.997");
    }

    // trang 6, BCTC hợp nhất quý 2/2019: dòng 50 thiếu ô thuyết minh, dòng 51 có -> lấy n ô cuối
    @Test
    void readsHeaderlessContinuationWithRaggedRows() {
        String page = """
                | | | |
                |---|---|---|
                | 50 | 12. Tổng lợi nhuận kế toán trước thuế | 50.709.728.678 | 46.983.887.264 | 97.235.316.515 | 85.465.102.375 |
                | 51 | 13. Chi phí thuế TNDN hiện hành | 22 | 11.175.325.409 | 10.729.429.898 | 20.699.497.457 | 18.792.697.119 |
                """;
        StatementTable table = StatementTableReader.readContinuation(MarkdownTableParser.tables(page).get(0), 4);
        assertThat(table.getRows().get(0).getValues()).containsExactly(
                "50.709.728.678", "46.983.887.264", "97.235.316.515", "85.465.102.375");
        assertThat(table.getRows().get(1).getValues()).containsExactly(
                "11.175.325.409", "10.729.429.898", "20.699.497.457", "18.792.697.119");
    }

    // bảng đối chiếu trong thuyết minh "Số liệu so sánh": không phải mẫu biểu
    @Test
    void rejectsReconciliationTable() {
        String page = """
                | | Mã số | Phân loại lại (VND) | Đã trình bày trên báo cáo năm trước (VND) |
                |---|---|---|---|
                | - Tiền | 111 | 155.702.500.927 | 158.433.014.215 |
                | - Phải thu ngắn hạn của khách hàng | 131 | 606.684.176.253 | 557.980.623.628 |
                """;
        assertThat(StatementTableReader.read(MarkdownTableParser.tables(page).get(0))).isNull();
    }

    // B02 bốn cột mà OCR làm rơi chữ "lũy kế": cột 3-4 vẫn là lũy kế
    @Test
    void quarterlyIncomeStatementFourColumns() {
        String page = """
                | Mã số | Chỉ tiêu | Thuyết minh | Quý 2/2024 | Quý 2/2023 | Quý 2/2024 | Quý 2/2023 |
                |---|---|---|---|---|---|---|
                | 01 | 1. Doanh thu bán hàng và cung cấp dịch vụ | | 3.000.000 | 2.000.000 | 5.000.000 | 4.000.000 |
                | 10 | 3. Doanh thu thuần | | 3.000.000 | 2.000.000 | 5.000.000 | 4.000.000 |
                """;
        StatementTable table = StatementTableReader.read(MarkdownTableParser.tables(page).get(0));
        ColumnPlanner.Plan plan = ColumnPlanner.plan(false, LocalDate.of(2024, 6, 30), table, page);
        assertThat(plan.getColumns()).extracting(ColumnPlanner.Column::getPeriod).containsExactly(
                Period.quarter(2024, 2), Period.quarter(2023, 2),
                Period.range(LocalDate.of(2024, 1, 1), LocalDate.of(2024, 6, 30)),
                Period.range(LocalDate.of(2023, 1, 1), LocalDate.of(2023, 6, 30)));
    }

    // tiêu đề cột mâu thuẫn kỳ của tài liệu: từ chối, không đoán (báo cáo riêng quý 1/2022 ghi nhầm năm)
    @Test
    void rejectsHeaderContradictingDocumentPeriod() {
        String page = """
                | Mã số | TÀI SẢN | Thuyết minh | 31/03/2022 VND | 01/01/2021 VND |
                |---|---|---|---|---|
                | 100 | A. TÀI SẢN NGẮN HẠN | | 1.000.000 | 900.000 |
                | 110 | I. Tiền | | 500.000 | 400.000 |
                """;
        StatementTable table = StatementTableReader.read(MarkdownTableParser.tables(page).get(0));
        assertThat(ColumnPlanner.plan(true, LocalDate.of(2022, 3, 31), table, page).getColumns()).isNull();
    }

    // số thật trang 5-6 BCTC hợp nhất quý 4/2023: tổng tài sản = tổng nguồn vốn; lệch một chữ số là lộ
    @Test
    void balanceIdentities() {
        Map<String, Long> ok = Map.of(
                "TAI_SAN_NGAN_HAN", 5_627_168_242_619L, "TAI_SAN_DAI_HAN", 1_301_931_686_640L,
                "TONG_TAI_SAN", 6_929_099_929_259L, "NO_PHAI_TRA", 4_938_347_117_084L,
                "VON_CHU_SO_HUU_M400", 1_990_752_812_175L, "TONG_NGUON_VON", 6_929_099_929_259L);
        assertThat(BalanceIdentities.check(MauBieu.B01, ok)).isEmpty();

        Map<String, Long> ocrError = new java.util.HashMap<>(ok);
        ocrError.put("TONG_NGUON_VON", 6_929_099_920_259L);
        List<BalanceIdentities.Failure> failures = BalanceIdentities.check(MauBieu.B01, ocrError);
        assertThat(failures).extracting(f -> f.getIdentity().getTotal())
                .containsExactlyInAnyOrder("TONG_NGUON_VON", "TONG_TAI_SAN");
    }

    @Test
    void namesMatchDespiteNumberingAndOcrVariants() {
        assertThat(NameMatcher.matches("A. TÀI SẢN NGẮN HẠN", "TÀI SẢN NGẮN HẠN")).isTrue();
        assertThat(NameMatcher.matches("3. Doanh thu thuần về bán hàng và cung cấp dịch vụ",
                "Doanh thu thuần bán hàng và cung cấp dịch vụ")).isTrue();
        assertThat(NameMatcher.matches("- Trong đó: Chi phí lãi vay", "- Trong đó: Chi phí lãi vay")).isTrue();
        // ca đã biết: mã 223 ghi cho TSCĐ vô hình, chuẩn TT200 là hao mòn
        assertThat(NameMatcher.matches("2. Tài sản cố định vô hình", "- Giá trị hao mòn lũy kế")).isFalse();
    }
}
