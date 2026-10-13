package com.vdt.finq.modules.document;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import com.vdt.finq.modules.document.DocumentEnums.LoaiTaiLieu;

public interface DocumentRepository extends JpaRepository<Document, String> {

    // màn hình quản lý tài liệu: tìm theo tên hoặc mã, lọc theo loại (null = mọi loại)
    @Query("select d from Document d where (:q is null or lower(d.tenHienThi) like lower(concat('%', :q, '%')) "
            + "or lower(d.taiLieuId) like lower(concat('%', :q, '%'))) and (:loai is null or d.loai = :loai)")
    Page<Document> search(@Param("q") String q, @Param("loai") LoaiTaiLieu loai, Pageable pageable);

    // tài liệu đã OCR nhưng chưa nạp vào Qdrant
    List<Document> findByNgayOcrIsNotNullAndNgayEmbedIsNullOrderByTaiLieuId();

    // tài liệu dùng để trích số (BCTC, đính chính) đã OCR xong
    List<Document> findByLoaiInAndNgayOcrIsNotNullOrderByTaiLieuId(Collection<LoaiTaiLieu> loai);

    // kỳ mới nhất có số liệu: mốc của "năm nay", "quý trước" khi chuẩn hoá câu hỏi
    @Query("select max(d.ngayKetThucKy) from Document d")
    LocalDate findLatestPeriodEnd();

    // chặn tải lên trùng: (ma_cong_ty, duong_dan) có UNIQUE KEY
    boolean existsByMaCongTyAndDuongDan(String maCongTy, String duongDan);

    // chỉ cập nhật hai cột trạng thái, không ghi đè các cột khác của dòng
    @Modifying
    @Transactional
    @Query("update Document d set d.ngayEmbed = :ngayEmbed, d.soChunk = :soChunk where d.taiLieuId = :taiLieuId")
    int updateEmbedState(@Param("taiLieuId") String taiLieuId,
            @Param("ngayEmbed") LocalDateTime ngayEmbed,
            @Param("soChunk") Integer soChunk);

    @Modifying
    @Transactional
    @Query("update Document d set d.ngayOcr = :ngayOcr, d.tongSoTrang = :tongSoTrang where d.taiLieuId = :taiLieuId")
    int updateOcrState(@Param("taiLieuId") String taiLieuId,
            @Param("ngayOcr") LocalDateTime ngayOcr,
            @Param("tongSoTrang") int tongSoTrang);
}
