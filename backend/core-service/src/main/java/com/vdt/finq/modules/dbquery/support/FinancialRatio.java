package com.vdt.finq.modules.dbquery.support;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

// sổ công thức theo mục 4 của tu-dien-tai-chinh_v1.2.md ([B01.xxx] là mã in trên báo cáo)
@Getter
@RequiredArgsConstructor
public enum FinancialRatio {
    GROSS_MARGIN("Biên lợi nhuận gộp", "Lợi nhuận gộp [B02.20] / Doanh thu thuần [B02.10]"),
    NET_MARGIN("Biên lợi nhuận ròng", "Lợi nhuận sau thuế [B02.60] / Doanh thu thuần [B02.10]"),
    DEBT_TO_ASSETS("Nợ phải trả / Tổng tài sản", "Nợ phải trả [B01.300] / Tổng tài sản [B01.270], số cuối kỳ"),
    DEBT_TO_EQUITY("Nợ phải trả / Vốn chủ sở hữu", "Nợ phải trả [B01.300] / Vốn chủ sở hữu [B01.400], số cuối kỳ"),
    BORROWINGS_TO_EQUITY("Nợ vay / Vốn chủ sở hữu",
            "(Vay ngắn hạn [B01.320] + Vay dài hạn [B01.338]) / Vốn chủ sở hữu [B01.400], số cuối kỳ"),
    INTEREST_COVERAGE("Khả năng thanh toán lãi vay",
            "(Lợi nhuận trước thuế [B02.50] + Chi phí lãi vay [B02.23]) / Chi phí lãi vay [B02.23]"),
    ROE("ROE", "hợp nhất: LNST của công ty mẹ [B02.61] / bình quân (Vốn chủ sở hữu [B01.400] − Lợi ích cổ đông "
            + "không kiểm soát [B01.429]); riêng: LNST [B02.60] / bình quân Vốn chủ sở hữu [B01.400]"),
    ROA("ROA", "Lợi nhuận sau thuế [B02.60] / bình quân Tổng tài sản [B01.270]"),
    ASSET_TURNOVER("Vòng quay tổng tài sản", "Doanh thu thuần [B02.10] / bình quân Tổng tài sản [B01.270]"),
    FREE_CASH_FLOW("Dòng tiền tự do (FCF)",
            "Lưu chuyển tiền thuần từ HĐKD [B03.20] + Tiền chi mua sắm TSCĐ [B03.21] (mã 21 là số âm)"),
    CAPEX("Chi đầu tư tài sản cố định (CAPEX)", "− Tiền chi mua sắm, xây dựng TSCĐ [B03.21]"),
    DSO("Số ngày thu tiền (DSO)", "bình quân Phải thu khách hàng [B01.131] / Doanh thu thuần [B02.10] × số ngày của kỳ"),
    DIO("Số ngày tồn kho (DIO)", "bình quân Hàng tồn kho [B01.140] / Giá vốn hàng bán [B02.11] × số ngày của kỳ"),
    DPO("Số ngày trả tiền (DPO)", "bình quân Phải trả người bán [B01.311] / Giá vốn hàng bán [B02.11] × số ngày của kỳ"),
    CCC("Chu kỳ chuyển đổi tiền mặt (CCC)", "DSO + DIO − DPO");

    private final String label;
    private final String formula;
}
