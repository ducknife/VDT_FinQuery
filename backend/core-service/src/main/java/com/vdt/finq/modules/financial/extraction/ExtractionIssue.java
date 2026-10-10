package com.vdt.finq.modules.financial.extraction;

import com.vdt.finq.modules.financial.FinancialEnums.LoaiLoi;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

// một dòng lệch chuẩn, ghi vào extraction_exceptions cho người soát; KHÔNG chứa dữ liệu dùng để trả lời
@Getter
@ToString
@AllArgsConstructor
public class ExtractionIssue {
    private final String taiLieuId;
    private final int trang;
    private final LoaiLoi loaiLoi;
    private final String cheDo;
    private final String mauBieu;
    private final String ma;
    private final String tenDocDuoc;
    private final String tenKyVong;
    // chuỗi thô, vì có thể chính nó không đọc được thành số
    private final String giaTriDoc;
    private final String chiTiet;
}
