-- ---------------------------------------------------------------------------
-- extraction_exceptions — nhật ký dòng lệch chuẩn khi trích số.
--
-- Nguyên tắc đã chốt: DB chỉ nhận thứ khớp chuẩn. Lệch chuẩn thì KHÔNG lưu vào
-- financial_figures, chỉ ghi vào đây. Trang vẫn nằm trong Qdrant nên nhánh RAG
-- vẫn trả lời được — không có gì rơi ra ngoài.
--
--     khớp chuẩn  →  financial_figures  →  nhánh SQL, chính xác tới đồng
--     lệch chuẩn  →  bảng này           →  người soát định kỳ
--     mọi trang   →  Qdrant             →  nhánh RAG
--
-- Bảng này còn là THƯỚC ĐO: log ngắn thì bỏ qua, phình to thì mới đáng thêm
-- dòng ánh xạ ngoại lệ. Đừng đoán trước.
--
-- Ca đã biết: 3 trang ghi mã 223 cho "Tài sản cố định vô hình" (đúng chuẩn TT200
-- là mã 227). Số liệu đúng, phép cộng khớp, chỉ mã sai chuẩn — do CTR trình bày
-- rút gọn. Đã kiểm: cả 3 giá trị đều có ở mã 227 đúng chuẩn tại 11, 3 và 12 tài
-- liệu khác, nên bỏ qua KHÔNG mất dữ liệu.
-- ---------------------------------------------------------------------------

CREATE TABLE extraction_exceptions (
    id             BIGINT UNSIGNED   NOT NULL AUTO_INCREMENT,

    tai_lieu_id    VARCHAR(64) COLLATE utf8mb4_bin NOT NULL,
    trang          SMALLINT UNSIGNED NOT NULL,

    loai_loi       VARCHAR(32)       NOT NULL
                   COMMENT 'ma_la | ten_khong_khop | lech_can_doi | sai_dau | khong_doc_duoc_so | thieu_che_do | thieu_mau_bieu',

    -- Khoá ba cột đọc được trên giấy. Để rời, KHÔNG đặt khoá ngoại sang
    -- financial_item_mappings — vì phần lớn lỗi ở đây chính là "cặp này không có
    -- trong bảng ánh xạ", khoá ngoại sẽ chặn mất chính thứ cần ghi lại.
    che_do         VARCHAR(8)            NULL,
    mau_bieu       VARCHAR(8)            NULL,
    ma             VARCHAR(4)            NULL,

    ten_doc_duoc   VARCHAR(200)          NULL  COMMENT 'Nguyên văn đọc được trên trang',
    ten_ky_vong    VARCHAR(200)          NULL  COMMENT 'Tên mà bảng ánh xạ chờ đợi',
    gia_tri_doc    VARCHAR(64)           NULL  COMMENT 'Chuỗi THÔ, vì có thể chính nó không đọc được thành số',
    chi_tiet       VARCHAR(500)          NULL  COMMENT 'Ví dụ: "mã 10 ≠ mã 01 − mã 02, lệch 7.248.298.072"',

    ngay_ghi       DATETIME          NOT NULL DEFAULT CURRENT_TIMESTAMP,
    da_xu_ly       TINYINT(1)        NOT NULL DEFAULT 0,
    cach_xu_ly     VARCHAR(200)          NULL  COMMENT 'Người soát ghi lại đã quyết gì',

    PRIMARY KEY (id),
    CONSTRAINT fk_exceptions_tai_lieu
        FOREIGN KEY (tai_lieu_id) REFERENCES documents (tai_lieu_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
  COMMENT = 'Nhat ky dong lech chuan khi trich so — thuoc do de biet co dang them ngoai le khong';
