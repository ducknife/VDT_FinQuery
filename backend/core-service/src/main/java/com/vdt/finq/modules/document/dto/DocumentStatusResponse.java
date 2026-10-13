package com.vdt.finq.modules.document.dto;

import java.time.LocalDateTime;

import com.vdt.finq.modules.document.Document;

import lombok.AllArgsConstructor;
import lombok.Getter;

// trạng thái nạp của một tài liệu, trả cho giao diện hỏi định kỳ sau khi tải lên
@Getter
@AllArgsConstructor
public class DocumentStatusResponse {
    private final String taiLieuId;
    private final String maCongTy;
    private final String tenHienThi;
    private final String loai;
    private final Integer nam;
    private final String ky;
    private final String phamVi;
    private final java.time.LocalDate ngayBanHanh;
    private final int tongSoTrang;
    // cho_ocr | cho_embed | da_embed
    private final String trangThai;
    private final LocalDateTime ngayOcr;
    private final LocalDateTime ngayEmbed;
    private final Integer soChunk;

    public static DocumentStatusResponse from(Document document) {
        return new DocumentStatusResponse(
                document.getTaiLieuId(),
                document.getMaCongTy(),
                document.getTenHienThi(),
                document.getLoai().getValue(),
                document.getNam(),
                document.getKy() == null ? null : document.getKy().getValue(),
                document.getPhamVi() == null ? null : document.getPhamVi().getValue(),
                document.getNgayBanHanh(),
                document.getTongSoTrang(),
                document.trangThaiNap().getValue(),
                document.getNgayOcr(),
                document.getNgayEmbed(),
                document.getSoChunk());
    }
}
