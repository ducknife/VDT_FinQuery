-- ---------------------------------------------------------------------------
-- Thêm trạng thái nạp vào documents.
--
-- Đây là thứ làm cho LUỒNG NẠP chạy lại được mà không sinh trùng. Không có mấy
-- cột này thì mỗi lần chạy lại, luồng không biết bắt đầu từ đâu: OCR lại cả
-- 6.690 trang, hoặc embed trùng vào Qdrant.
--
--     tài liệu mới  →  OCR          →  ngay_ocr
--                   →  trích số     →  ngay_trich_so
--                   →  chunk+embed  →  ngay_embed, so_chunk
--
-- Mỗi bước chỉ chạy khi cột tương ứng còn NULL. Bỏ dở giữa chừng thì lần sau
-- chạy tiếp đúng chỗ, không làm lại từ đầu.
--
-- Để cột thay vì bảng riêng: trạng thái là thuộc tính của tài liệu, mỗi tài liệu
-- đúng một dòng. Dựng bảng riêng chỉ đáng khi cần giữ LỊCH SỬ nhiều lần nạp —
-- chưa cần.
-- ---------------------------------------------------------------------------

ALTER TABLE documents
    ADD COLUMN ngay_ocr      DATETIME     NULL
        COMMENT 'NULL = chưa OCR' AFTER ghi_chu,
    ADD COLUMN ngay_trich_so DATETIME     NULL
        COMMENT 'NULL = chưa trích số vào financial_figures' AFTER ngay_ocr,
    ADD COLUMN ngay_embed    DATETIME     NULL
        COMMENT 'NULL = chưa chunk + embed vào Qdrant' AFTER ngay_trich_so,
    ADD COLUMN so_chunk      INT UNSIGNED NULL
        COMMENT 'Số chunk đã đẩy lên Qdrant — đối chiếu khi nạp lại' AFTER ngay_embed;

-- OCR đã chạy xong toàn bộ 6.690 trang trước khi có bảng này, nên đánh dấu luôn
-- cho 278 tài liệu hiện có. Hai bước sau vẫn NULL: chưa trích, chưa embed.
UPDATE documents SET ngay_ocr = CURRENT_TIMESTAMP WHERE ngay_ocr IS NULL;
