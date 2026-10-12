-- User chỉ đọc cho tool số liệu của agent (QĐ-037). Chạy bằng root.
-- Mật khẩu chỉ điền lúc chạy, không lưu vào file này (file lên git).
-- Sau khi chạy, thêm vào backend/core-service/.env:
--   DB_READONLY_USERNAME=finq_reader
--   DB_READONLY_PASSWORD=<mật khẩu>

CREATE USER IF NOT EXISTS 'finq_reader'@'localhost' IDENTIFIED BY '<MAT_KHAU>';

GRANT SELECT ON FINQUERY.financial_figures TO 'finq_reader'@'localhost';
GRANT SELECT ON FINQUERY.financial_items   TO 'finq_reader'@'localhost';
GRANT SELECT ON FINQUERY.documents         TO 'finq_reader'@'localhost';

-- phải ra 1 dòng USAGE + 3 dòng SELECT
SHOW GRANTS FOR 'finq_reader'@'localhost';

-- kiểm (đăng nhập finq_reader):
--   SELECT COUNT(*) FROM financial_figures;        -- được
--   DELETE FROM financial_figures WHERE id = -1;   -- bị từ chối
-- gỡ: DROP USER IF EXISTS 'finq_reader'@'localhost';
