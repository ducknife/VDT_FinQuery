package com.vdt.finq.modules.document.dto;

import java.time.LocalDate;
import java.util.regex.Pattern;

import org.springframework.format.annotation.DateTimeFormat;

import com.vdt.finq.common.exception.InvalidRequestException;
import com.vdt.finq.modules.document.Document;
import com.vdt.finq.modules.document.DocumentEnums.CheDo;
import com.vdt.finq.modules.document.DocumentEnums.Ky;
import com.vdt.finq.modules.document.DocumentEnums.LoaiTaiLieu;
import com.vdt.finq.modules.document.DocumentEnums.PhamVi;

import lombok.Getter;
import lombok.Setter;

// metadata người tải lên nhập kèm file (form multipart).
// enum nhận đúng chuỗi như trong DB: loai=bctc, ky=Q4, phamVi=hop_nhat
@Getter
@Setter
public class UploadDocumentRequest {
    // mã công ty còn là tên thư mục trên đĩa: chỉ cho chữ hoa và số, chặn kiểu ../
    private static final Pattern MA_CONG_TY = Pattern.compile("[A-Z0-9]{2,16}");

    private String maCongTy;
    private String tenHienThi;
    private String loai;
    private Integer nam;
    private String ky;
    private String phamVi;
    private String cheDo;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate ngayKetThucKy;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate ngayBanHanh;

    // kiểm tra và đổi sang entity; sai thì báo 400 kèm lý do
    public Document toDocument() {
        if (tenHienThi == null || tenHienThi.isBlank()) {
            throw new InvalidRequestException("thiếu tenHienThi");
        }
        if (loai == null || nam == null) {
            throw new InvalidRequestException("thiếu loai hoặc nam");
        }
        String congTy = maCongTy == null ? "" : maCongTy.strip().toUpperCase();
        if (!MA_CONG_TY.matcher(congTy).matches()) {
            throw new InvalidRequestException("maCongTy phải gồm 2-16 chữ hoa hoặc số, ví dụ CTR");
        }
        Document document = new Document();
        document.setMaCongTy(congTy);
        document.setTenHienThi(tenHienThi.strip());
        document.setNam(nam);
        document.setNgayKetThucKy(ngayKetThucKy);
        document.setNgayBanHanh(ngayBanHanh);
        try {
            document.setLoai(LoaiTaiLieu.fromValue(loai));
            document.setKy(Ky.fromValue(blankToNull(ky)));
            document.setPhamVi(PhamVi.fromValue(blankToNull(phamVi)));
            document.setCheDo(CheDo.fromValue(blankToNull(cheDo)));
        } catch (IllegalArgumentException e) {
            throw new InvalidRequestException(e.getMessage());
        }
        return document;
    }

    // form gửi ô trống thành "" chứ không phải null
    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }
}
