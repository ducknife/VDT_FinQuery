package com.vdt.finq.modules.rag.preprocessing.indexing.vectordb;

// tên các trường payload, đặt theo tên cột của bảng documents
public final class QdrantConstants {

    private QdrantConstants() {
    }

    public static final String TAI_LIEU_ID = "tai_lieu_id";
    public static final String MA_CONG_TY = "ma_cong_ty";
    public static final String TRANG = "trang";
    public static final String PHAN = "phan";
    public static final String NAM = "nam";
    public static final String KY = "ky";
    public static final String PHAM_VI = "pham_vi";
    public static final String LOAI = "loai";
    public static final String LA_BAN_CHINH = "la_ban_chinh";
    // giá trị: QdrantEnums.ElementType
    public static final String ELEMENT_TYPE = "element_type";
    public static final String NOI_DUNG = "noi_dung";
}
