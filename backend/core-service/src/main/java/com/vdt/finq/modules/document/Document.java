package com.vdt.finq.modules.document;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.vdt.finq.modules.document.DocumentEnumConverters.CheDoConverter;
import com.vdt.finq.modules.document.DocumentEnumConverters.KyConverter;
import com.vdt.finq.modules.document.DocumentEnumConverters.LoaiTaiLieuConverter;
import com.vdt.finq.modules.document.DocumentEnumConverters.PhamViConverter;
import com.vdt.finq.modules.document.DocumentEnums.CheDo;
import com.vdt.finq.modules.document.DocumentEnums.Ky;
import com.vdt.finq.modules.document.DocumentEnums.LoaiTaiLieu;
import com.vdt.finq.modules.document.DocumentEnums.PhamVi;
import com.vdt.finq.modules.document.DocumentEnums.TrangThaiNap;

import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

// một dòng của bảng documents (V4, V8, V10). tên cột tự suy từ tên trường: taiLieuId -> tai_lieu_id
@Entity
@Table(name = "documents")
@Getter
@Setter
@ToString
@NoArgsConstructor
public class Document {
    // sinh tất định từ đường dẫn (DocumentIdGenerator), không tự tăng
    @Id
    private String taiLieuId;
    // mã công ty, cũng là thư mục gốc trong data/ocr và data/raw
    private String maCongTy;
    private String tenHienThi;
    @Convert(converter = LoaiTaiLieuConverter.class)
    private LoaiTaiLieu loai;
    private Integer nam;
    @Convert(converter = KyConverter.class)
    private Ky ky;
    // báo cáo: mốc chốt số liệu; văn bản: ngày ký. một tài liệu chỉ có một trong hai
    private LocalDate ngayKetThucKy;
    private LocalDate ngayBanHanh;
    @Convert(converter = PhamViConverter.class)
    private PhamVi phamVi;
    @Convert(converter = CheDoConverter.class)
    private CheDo cheDo;
    private int tongSoTrang;
    private boolean laBanChinh = true;
    private int soBanCungKy = 1;
    // thư mục OCR, tương đối từ data/ocr/<ma_cong_ty>
    private String duongDan;
    private boolean canSoat;
    private String ghiChu;
    // trạng thái nạp: NULL = bước đó chưa xong
    private LocalDateTime ngayOcr;
    private LocalDateTime ngayTrichSo;
    private LocalDateTime ngayEmbed;
    private Integer soChunk;

    // đường dẫn đưa cho PageStore: <ma_cong_ty>/<duong_dan>
    public String ocrRelativePath() {
        return maCongTy + "/" + duongDan;
    }

    public TrangThaiNap trangThaiNap() {
        if (ngayOcr == null) {
            return TrangThaiNap.CHO_OCR;
        }
        return ngayEmbed == null ? TrangThaiNap.CHO_EMBED : TrangThaiNap.DA_EMBED;
    }
}
