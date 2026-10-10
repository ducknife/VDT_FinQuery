package com.vdt.finq.modules.financial;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

// giá trị cố định của các bảng financial_*; value là đúng chuỗi lưu trong DB
public final class FinancialEnums {

    private FinancialEnums() {
    }

    // mẫu biểu của báo cáo tài chính, cột mau_bieu trong financial_item_mappings
    @Getter
    @RequiredArgsConstructor
    public enum MauBieu {
        B01("B01"),       // bảng cân đối kế toán (TT99: báo cáo tình hình tài chính)
        B02("B02"),       // kết quả hoạt động kinh doanh
        B03_GT("B03-GT"), // lưu chuyển tiền tệ, phương pháp gián tiếp
        B03_TT("B03-TT"); // lưu chuyển tiền tệ, phương pháp trực tiếp

        private final String value;

        public boolean isCashFlow() {
            return this == B03_GT || this == B03_TT;
        }
    }

    // số lấy từ cột nào của báo cáo
    @Getter
    @RequiredArgsConstructor
    public enum CachLay {
        COT_KY_NAY("cot_ky_nay"),
        COT_KY_TRUOC("cot_ky_truoc");

        private final String value;
    }

    @Getter
    @RequiredArgsConstructor
    public enum TrangThaiSo {
        HIEU_LUC("hieu_luc"),
        DA_THAY_THE("da_thay_the"); // bị số trình bày lại thay thế, giữ để còn dấu vết

        private final String value;
    }

    // thoi_diem: số dư tại một ngày (B01); thoi_ky: phát sinh giữa hai ngày (B02, B03)
    @Getter
    @RequiredArgsConstructor
    public enum LoaiSo {
        THOI_DIEM("thoi_diem"),
        THOI_KY("thoi_ky");

        private final String value;
    }

    // lý do một dòng không vào financial_figures, cột loai_loi của extraction_exceptions
    @Getter
    @RequiredArgsConstructor
    public enum LoaiLoi {
        MA_LA("ma_la"),                           // mã số không có trong bảng ánh xạ
        TEN_KHONG_KHOP("ten_khong_khop"),         // mã có nhưng tên đọc được khác tên chờ đợi
        LECH_CAN_DOI("lech_can_doi"),             // phép cộng kế toán không khớp
        SAI_DAU("sai_dau"),                       // chỉ tiêu bản chất âm mà đọc ra số dương
        KHONG_DOC_DUOC_SO("khong_doc_duoc_so"),   // ô giá trị không phải số hợp lệ
        THIEU_CHE_DO("thieu_che_do"),             // không biết TT200 hay TT99
        THIEU_MAU_BIEU("thieu_mau_bieu"),         // bảng có mã số nhưng không biết là B01, B02 hay B03
        KHONG_XAC_DINH_KY("khong_xac_dinh_ky"),   // tiêu đề cột không khớp kỳ của tài liệu
        TRUNG_MA_KHAC_SO("trung_ma_khac_so");     // cùng mã, cùng cột mà hai số khác nhau

        private final String value;
    }
}
