package com.vdt.finq.modules.document;

import java.util.function.Function;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

// giá trị cố định của bảng documents (dùng chung MySQL, Qdrant, bộ lọc); value là chuỗi lưu trong DB
public final class DocumentEnums {

    private DocumentEnums() {
    }

    // kỳ báo cáo; chỉ bctc, bctn, dinh_chinh mới có
    @Getter
    @RequiredArgsConstructor
    public enum Ky {
        NAM("nam"),
        BAN_NIEN("ban_nien"),
        Q1("Q1"),
        Q2("Q2"),
        Q3("Q3"),
        Q4("Q4");

        private final String value;

        public static Ky fromValue(String value) {
            return parse(values(), Ky::getValue, value);
        }
    }

    // phạm vi báo cáo; bctn để trống vì trình bày cả hai
    @Getter
    @RequiredArgsConstructor
    public enum PhamVi {
        HOP_NHAT("hop_nhat"), // hợp nhất: công ty mẹ + công ty con
        RIENG("rieng");       // riêng: chỉ công ty mẹ

        private final String value;

        public static PhamVi fromValue(String value) {
            return parse(values(), PhamVi::getValue, value);
        }
    }

    // loại tài liệu, theo devtools/quet_tai_lieu.py
    @Getter
    @RequiredArgsConstructor
    public enum LoaiTaiLieu {
        BCTC("bctc"),                     // báo cáo tài chính
        GIAI_TRINH("giai_trinh"),         // công văn giải trình
        BCTN("bctn"),                     // báo cáo thường niên
        BAO_CAO_PT("bao_cao_pt"),         // báo cáo phát triển bền vững
        TO_TRINH("to_trinh"),             // tờ trình ĐHĐCĐ
        NGHI_QUYET("nghi_quyet"),         // nghị quyết, biên bản ĐHĐCĐ
        BAO_CAO_DHDCD("bao_cao_dhdcd"),   // báo cáo trình ĐHĐCĐ
        TAI_LIEU_HOP("tai_lieu_hop"),     // tài liệu họp ĐHĐCĐ
        CBTT_CO_TUC("cbtt_co_tuc"),       // công bố thông tin: cổ tức
        CBTT_PHAT_HANH("cbtt_phat_hanh"), // công bố thông tin: phát hành, tăng vốn
        CBTT_KHAC("cbtt_khac"),           // công bố thông tin khác
        DINH_CHINH("dinh_chinh"),         // đính chính, trình bày lại số liệu
        KE_HOACH("ke_hoach");             // kế hoạch kinh doanh

        private final String value;

        public static LoaiTaiLieu fromValue(String value) {
            return parse(values(), LoaiTaiLieu::getValue, value);
        }
    }

    // chế độ kế toán của báo cáo
    @Getter
    @RequiredArgsConstructor
    public enum CheDo {
        TT200("TT200"), // Thông tư 200/2014
        TT99("TT99");   // Thông tư 99/2025, áp dụng từ 2026

        private final String value;

        public static CheDo fromValue(String value) {
            return parse(values(), CheDo::getValue, value);
        }
    }

    // trạng thái nạp, suy ra từ các cột ngay_ocr, ngay_embed; không lưu thành cột riêng
    @Getter
    @RequiredArgsConstructor
    public enum TrangThaiNap {
        CHO_OCR("cho_ocr"),     // mới tải lên, chưa OCR xong
        CHO_EMBED("cho_embed"), // đã OCR, chưa nạp vào Qdrant
        DA_EMBED("da_embed");   // đã có trong Qdrant, tìm được

        private final String value;
    }

    // chuỗi trong DB -> enum; null giữ null (cột cho phép trống), chuỗi lạ thì báo lỗi
    private static <E extends Enum<E>> E parse(E[] values, Function<E, String> toValue, String value) {
        if (value == null) {
            return null;
        }
        for (E e : values) {
            if (toValue.apply(e).equals(value)) {
                return e;
            }
        }
        throw new IllegalArgumentException("giá trị không hợp lệ: " + value);
    }
}
