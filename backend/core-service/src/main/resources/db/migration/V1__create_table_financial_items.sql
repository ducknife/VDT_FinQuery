-- ---------------------------------------------------------------------------
-- Từ điển mã chỉ tiêu báo cáo tài chính — hai bảng.
--
-- Bảng này KHÔNG chứa số liệu. Số liệu nằm ở bảng chi_tieu (migration sau).
--
--   financial_items          BỘ TỪ VỰNG: 123 mã nội bộ, mỗi mã một dòng.
--                            chi_tieu.ma_noi_bo trỏ khoá ngoại về đây.
--   financial_item_mappings  CÁCH DỊCH: 250 ánh xạ mã in trên giấy → mã nội bộ.
--
-- Khoá của bảng ánh xạ là BA cột (che_do, mau_bieu, ma) — đã chứng minh tối thiểu:
--   · cùng mã khác mẫu biểu → khác nghĩa   (mã 01: B02 doanh thu, B03 LNTT)
--   · cùng mã khác chế độ   → khác nghĩa   (mã 136: TT200 phải thu, TT99 dự phòng)
-- Đo trên kho CTR 2019-2026: 26 ca đụng độ thật.
--
-- Chiều tra (che_do, mau_bieu, ma) → ma_noi_bo  dùng LÚC NẠP.
-- Chiều tra ma_noi_bo → ten_chuan               dùng LÚC TRẢ LỜI.
-- Cột ma_noi_bo của financial_items là nguồn sinh enum cho tool function calling
-- (không cần DISTINCT — bảng này mỗi mã đúng một dòng).
--
-- KHÔNG tạo chỉ số nào ngoài khoá. Bảng 123/250 dòng, đọc một lần lúc khởi động
-- vào bộ nhớ; thêm chỉ số theo phỏng đoán chỉ tốn chi phí ghi. Cần thì thêm sau
-- bằng migration mới, sau khi ĐO thấy chậm.
-- ---------------------------------------------------------------------------

CREATE TABLE financial_items (
    ma_noi_bo        VARCHAR(48)  NOT NULL  COMMENT 'Mã nội bộ, bất biến giữa các chế độ',
    ten_chuan        VARCHAR(160) NOT NULL  COMMENT 'Tên hiển thị cho người dùng',
    loai_so          VARCHAR(12)  NOT NULL  COMMENT 'thoi_diem (một mốc) | thoi_ky (giữa hai mốc)',
    dau_am_tu_nhien  TINYINT(1)   NOT NULL DEFAULT 0
                     COMMENT '1 nếu bản chất là số âm: dự phòng, hao mòn, giảm trừ',
    thay_the_bang    VARCHAR(48)      NULL
                     COMMENT 'Mã gợi ý khi mã này bị bãi bỏ theo chế độ — KHÔNG thay ngầm, chỉ gợi ý',
    ly_do_thay_the   VARCHAR(200)     NULL,
    ghi_chu          VARCHAR(255)     NULL,

    PRIMARY KEY (ma_noi_bo),
    CONSTRAINT fk_financial_items_thay_the
        FOREIGN KEY (thay_the_bang) REFERENCES financial_items (ma_noi_bo)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
  COMMENT = 'Bo tu vung chi tieu tai chinh — nguon sinh enum cho tool';


CREATE TABLE financial_item_mappings (
    che_do            VARCHAR(8)        NOT NULL  COMMENT 'TT200 | TT99',
    mau_bieu          VARCHAR(8)        NOT NULL  COMMENT 'B01 | B02 | B03-GT | B03-TT',
    ma                VARCHAR(4)        NOT NULL  COMMENT 'Mã số in trên báo cáo',
    ma_noi_bo         VARCHAR(48)       NOT NULL,
    ten_tren_bao_cao  VARCHAR(160)      NOT NULL
                      COMMENT 'Nguyên văn trên báo cáo — để trả lời đúng từ của kỳ đó',
    so_lan_gap        SMALLINT UNSIGNED NOT NULL  COMMENT 'Số lần gặp khi quét kho, dùng để soát',
    so_bien_the       SMALLINT UNSIGNED NOT NULL  COMMENT 'Số biến thể tên do OCR',
    che_do_suy_doan   TINYINT(1)        NOT NULL DEFAULT 0
                      COMMENT '1 nếu KHÔNG tài liệu nào ghi rõ thông tư, phải suy từ năm',
    ghi_chu           VARCHAR(255)          NULL,

    PRIMARY KEY (che_do, mau_bieu, ma),
    CONSTRAINT fk_mappings_ma_noi_bo
        FOREIGN KEY (ma_noi_bo) REFERENCES financial_items (ma_noi_bo)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
  COMMENT = 'Anh xa ma in tren bao cao (TT200/TT99) sang ma noi bo';
