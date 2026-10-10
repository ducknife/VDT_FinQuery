package com.vdt.finq.modules.document;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import com.vdt.finq.modules.document.DocumentEnums.Ky;
import com.vdt.finq.modules.document.DocumentEnums.LoaiTaiLieu;
import com.vdt.finq.modules.document.DocumentEnums.PhamVi;

// ID sinh bằng Java phải trùng đúng ID mà devtools/quet_tai_lieu.py đã sinh trong V5
class DocumentIdGeneratorTest {

    @Test
    void matchesSeedIds() {
        assertThat(DocumentIdGenerator.generate(document(LoaiTaiLieu.BCTC, 2019, Ky.Q1, PhamVi.HOP_NHAT,
                LocalDate.of(2019, 3, 31), null,
                "01_bao_cao_tai_chinh/2019/hop_nhat/CTR-BCTC-hop-nhat-Quy-1.2019")))
                .isEqualTo("CTR-BCTC-HN-20190331-Q1-a02c8c");

        assertThat(DocumentIdGenerator.generate(document(LoaiTaiLieu.BCTC, 2019, Ky.BAN_NIEN, PhamVi.RIENG,
                LocalDate.of(2019, 6, 30), null,
                "01_bao_cao_tai_chinh/2019/rieng/BCTC-R-ban-nien-2019")))
                .isEqualTo("CTR-BCTC-R-20190630-BAN_NIEN-4e84c9");

        assertThat(DocumentIdGenerator.generate(document(LoaiTaiLieu.BAO_CAO_DHDCD, 2021, null, null,
                null, LocalDate.of(2021, 4, 2),
                "03_dai_hoi_dong_co_dong/2021/bao_cao/06.-BC-cua-BKS")))
                .isEqualTo("CTR-BAOCAODHDCD-20210402-0e1967");

        // không có ngày nào thì dùng NA + năm
        assertThat(DocumentIdGenerator.generate(document(LoaiTaiLieu.DINH_CHINH, 2022, null, null,
                null, null,
                "04_cong_bo_thong_tin/2022/dinh_chinh/20220419-CTR-Dinh-chinh-BCTC-2021")))
                .isEqualTo("CTR-DINHCHINH-NA2022-fcee93");
    }

    // chuỗi trong DB -> enum: null giữ null, chuỗi lạ báo lỗi
    @Test
    void enumsParseDatabaseValues() {
        assertThat(LoaiTaiLieu.fromValue("cbtt_co_tuc")).isEqualTo(LoaiTaiLieu.CBTT_CO_TUC);
        assertThat(Ky.fromValue("ban_nien")).isEqualTo(Ky.BAN_NIEN);
        assertThat(PhamVi.fromValue(null)).isNull();
        assertThatThrownBy(() -> Ky.fromValue("Q5")).isInstanceOf(IllegalArgumentException.class);
    }

    private static Document document(LoaiTaiLieu loai, int nam, Ky ky, PhamVi phamVi,
            LocalDate ngayKetThucKy, LocalDate ngayBanHanh, String duongDan) {
        Document document = new Document();
        document.setMaCongTy("CTR");
        document.setLoai(loai);
        document.setNam(nam);
        document.setKy(ky);
        document.setPhamVi(phamVi);
        document.setNgayKetThucKy(ngayKetThucKy);
        document.setNgayBanHanh(ngayBanHanh);
        document.setDuongDan(duongDan);
        return document;
    }
}
