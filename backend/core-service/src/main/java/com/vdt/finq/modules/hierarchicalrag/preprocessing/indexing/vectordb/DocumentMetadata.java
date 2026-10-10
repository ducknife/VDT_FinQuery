package com.vdt.finq.modules.hierarchicalrag.preprocessing.indexing.vectordb;

import com.vdt.finq.modules.document.Document;
import com.vdt.finq.modules.document.DocumentEnums.Ky;
import com.vdt.finq.modules.document.DocumentEnums.LoaiTaiLieu;
import com.vdt.finq.modules.document.DocumentEnums.PhamVi;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

// thông tin một tài liệu trong bảng documents, dùng để dựng payload cho mọi chunk của nó.
// ky và phamVi có thể null (ví dụ nghị quyết không có kỳ báo cáo), khi đó không ghi vào payload
@Getter
@ToString
@AllArgsConstructor
public class DocumentMetadata {
    private final String taiLieuId;
    private final String maCongTy;
    private final LoaiTaiLieu loai;
    private final int nam;
    private final Ky ky;
    private final PhamVi phamVi;
    private final boolean laBanChinh;

    // chỉ lấy các cột cần cho payload, để QdrantIndexWriter không phụ thuộc entity JPA
    public static DocumentMetadata from(Document document) {
        return new DocumentMetadata(document.getTaiLieuId(), document.getMaCongTy(), document.getLoai(), document.getNam(),
                document.getKy(), document.getPhamVi(), document.isLaBanChinh());
    }
}
