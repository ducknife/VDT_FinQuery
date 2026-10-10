-- ---------------------------------------------------------------------------
-- Thêm mã công ty vào documents.
--
-- Trước đây "CTR" bị viết cứng trong code ở ba chỗ: thư mục OCR (data/ocr/CTR),
-- thư mục file gốc (data/raw/CTR) và tiền tố của tai_lieu_id. Thêm công ty thứ
-- hai là phải sửa code. Giờ mỗi tài liệu tự mang mã công ty của nó:
--
--     data/ocr/<ma_cong_ty>/<duong_dan>/trang_NNN.md
--     data/raw/<ma_cong_ty>/<duong_dan>.<đuôi>
--     tai_lieu_id = <ma_cong_ty>-<LOAI>-...
--
-- Cột là chuỗi tự do (kiểm tra định dạng ở tầng ứng dụng), KHÔNG phải enum:
-- thêm công ty là thêm dữ liệu, không phải sửa code. Bảng companies riêng
-- (tên, mã chứng khoán, ...) để dành cho hướng mở rộng nhiều công ty.
-- ---------------------------------------------------------------------------

-- Thêm cột với mặc định 'CTR' để điền cho 278 tài liệu đang có.
-- ma_cong_ty là định danh nên so khớp từng byte (utf8mb4_bin), giống tai_lieu_id.
ALTER TABLE documents
    ADD COLUMN ma_cong_ty VARCHAR(16) COLLATE utf8mb4_bin NOT NULL DEFAULT 'CTR'
        COMMENT 'Mã công ty sở hữu tài liệu, cũng là thư mục gốc trong data/ocr và data/raw'
        AFTER tai_lieu_id;

-- Bỏ mặc định: từ giờ tài liệu mới phải ghi rõ thuộc công ty nào.
ALTER TABLE documents
    ALTER COLUMN ma_cong_ty DROP DEFAULT;

-- duong_dan chỉ cần duy nhất TRONG một công ty: hai công ty có thể cùng có
-- 01_bao_cao_tai_chinh/2024/hop_nhat/BCTC-HN-2024. Khoá mới bắt đầu bằng
-- ma_cong_ty nên cũng dùng được khi lọc tài liệu theo công ty.
ALTER TABLE documents
    DROP INDEX uk_documents_duong_dan,
    ADD UNIQUE KEY uk_documents_cong_ty_duong_dan (ma_cong_ty, duong_dan),
    MODIFY duong_dan VARCHAR(255) COLLATE utf8mb4_bin NOT NULL
        COMMENT 'Duong dan thu muc OCR, tuong doi tu data/ocr/<ma_cong_ty>';
