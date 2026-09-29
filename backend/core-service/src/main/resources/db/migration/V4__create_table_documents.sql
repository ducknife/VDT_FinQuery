-- ---------------------------------------------------------------------------
-- documents — danh mục tài liệu nguồn.
--
-- Chứa CẢ 278 tài liệu, không chỉ 90 báo cáo tài chính. Lý do: Qdrant lưu chunk
-- của mọi tài liệu, và mỗi trích dẫn phải hiện ra tên đọc được. Nếu bảng này chỉ
-- có BCTC thì mọi trích dẫn từ nghị quyết, tờ trình, công bố thông tin đều không
-- có tên. Lưu tên thẳng vào payload Qdrant thì thành hai nguồn sự thật.
--
-- tai_lieu_id sinh TẤT ĐỊNH, phần đuôi là băm của đường dẫn:
--     CTR-BCTC-HN-20251231-NAM-226686
-- Không dùng số thứ tự trong cụm, vì thêm tài liệu mới sẽ làm số thứ tự của tài
-- liệu cũ dịch đi — mọi trích dẫn đã lưu trong chi_tieu và Qdrant sẽ trỏ sai.
-- Băm chỉ phụ thuộc vào chính tài liệu, nên ID không bao giờ đổi.
--
-- HAI LOẠI NGÀY, đừng lẫn:
--     ngay_ket_thuc_ky  báo cáo — mốc chốt số liệu (31/12, 30/06, …)
--     ngay_ban_hanh     công văn, nghị quyết, tờ trình — ngày ký
-- Một tài liệu chỉ có một trong hai.
-- ---------------------------------------------------------------------------

CREATE TABLE documents (
    tai_lieu_id       VARCHAR(64)       NOT NULL,
    ten_hien_thi      VARCHAR(200)      NOT NULL  COMMENT 'Tên đọc được, dùng trong trích dẫn',
    loai              VARCHAR(24)       NOT NULL  COMMENT 'bctc | bctn | dinh_chinh | nghi_quyet | to_trinh | giai_trinh | cbtt_* | …',
    nam               SMALLINT UNSIGNED NOT NULL,

    ky                VARCHAR(12)           NULL  COMMENT 'nam | ban_nien | Q1..Q4 — chỉ báo cáo mới có',
    ngay_ket_thuc_ky  DATE                  NULL  COMMENT 'Mốc chốt số liệu của báo cáo',
    ngay_ban_hanh     DATE                  NULL  COMMENT 'Ngày ký văn bản',
    pham_vi           VARCHAR(12)           NULL  COMMENT 'hop_nhat | rieng — BCTN để trống vì trình bày cả hai',
    che_do            VARCHAR(8)            NULL  COMMENT 'TT200 | TT99',

    tong_so_trang     SMALLINT UNSIGNED NOT NULL  COMMENT 'Tổng số trang của tài liệu — KHÔNG phải vị trí trang',
    la_ban_chinh      TINYINT(1)        NOT NULL DEFAULT 1
                      COMMENT '0 nếu là bản trùng kỳ không dùng để trích dẫn (bản rút gọn, đăng lại)',
    so_ban_cung_ky    SMALLINT UNSIGNED NOT NULL DEFAULT 1
                      COMMENT 'Số tài liệu cùng (loai, pham_vi, ngay_ket_thuc_ky, ky)',

    duong_dan         VARCHAR(255)      NOT NULL  COMMENT 'Đường dẫn thư mục OCR, tương đối từ data/ocr/CTR',
    can_soat          TINYINT(1)        NOT NULL DEFAULT 0
                      COMMENT '1 nếu máy không nhận đủ metadata, cần người xác nhận',
    ghi_chu           VARCHAR(255)          NULL,

    PRIMARY KEY (tai_lieu_id),
    -- Chặn nạp trùng: đẩy lại cùng một tài liệu không sinh ra dòng thứ hai.
    UNIQUE KEY uk_documents_duong_dan (duong_dan)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
  COMMENT = 'Danh muc tai lieu nguon — noi trich dan tro ve';
