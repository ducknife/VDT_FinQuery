-- ---------------------------------------------------------------------------
-- Sửa cột dau_am_tu_nhien của financial_items.
--
-- V2 đặt cờ này bằng QUY TẮC THEO TÊN (mã bắt đầu bằng DU_PHONG_, kết thúc bằng
-- _HAO_MON, hoặc là CAC_KHOAN_GIAM_TRU_DT). Đối chiếu lại với dữ liệu thật trong
-- kho OCR (đếm số lần giá trị in trong ngoặc đơn) thì quy tắc đó sai hai chiều.
--
-- Ranh giới đúng: đánh dấu khi QUY ƯỚC TRÌNH BÀY bắt buộc số âm (tài khoản điều
-- chỉnh giảm, dòng chi tiền của LCTT). KHÔNG đánh dấu khi chỉ là số liệu của CTR
-- tình cờ âm, hoặc khi chỉ tiêu bản chất hai chiều.
-- ---------------------------------------------------------------------------

-- === BỎ đánh dấu: 2 mã ===

-- Dự phòng phải trả là một khoản NỢ, nằm bên nguồn vốn nên luôn dương.
-- Đo trên kho: 0 lần trong ngoặc / 105 lần dương. Để nguyên thì chốt kiểm sẽ
-- chặn sai toàn bộ 105 dòng hợp lệ.
UPDATE financial_items SET dau_am_tu_nhien = 0 WHERE ma_noi_bo = 'DU_PHONG_PHAI_TRA_NH';

-- Mã 02 in DƯƠNG trên báo cáo, rồi mã 10 = 01 − 02. Là phép trừ, không phải số âm.
-- (CTR không phát sinh khoản giảm trừ nào nên kho không có bằng chứng ngược lại.)
UPDATE financial_items SET dau_am_tu_nhien = 0 WHERE ma_noi_bo = 'CAC_KHOAN_GIAM_TRU_DT';


-- === THÊM đánh dấu: 9 mã chi tiền của lưu chuyển tiền tệ ===
-- Quy ước LCTT: mọi dòng chi tiền in trong ngoặc. Đo trên kho, tỷ lệ âm/dương:

UPDATE financial_items SET dau_am_tu_nhien = 1
WHERE ma_noi_bo IN (
    'LCTT_TIEN_CHI_KHAC_HDKD',      -- 179 / 0
    'LCTT_THUE_TNDN_DA_NOP',        -- 178 / 0
    'LCTT_CO_TUC_DA_TRA',           -- 153 / 0
    'LCTT_TRA_NO_GOC_VAY',          -- 139 / 1   (1 lần dương, nghi OCR mất ngoặc)
    'LCTT_CHI_MUA_TSCD',            -- 132 / 0
    'LCTT_CHI_PHI_DI_VAY_DA_TRA',   -- 128 / 0
    'LCTT_CHI_CHO_VAY',             --  96 / 0
    'LCTT_TT_CHI_NGUOI_CUNG_CAP',   --   8 / 0
    'LCTT_TT_CHI_NGUOI_LAO_DONG'    --   8 / 0
);


-- === CỐ Ý KHÔNG đánh dấu, ghi lại để sau khỏi bàn lại ===
--
--   LCTT_THUAN_HDDT     170 / 10  — số THUẦN, âm hay dương đều hợp lệ
--   LCTT_LAI_LO_DAU_TU  172 / 0   — dòng "lãi/lỗ", bản chất hai chiều
--   THANG_DU_VON        110 / 0   — CTR âm 15 triệu, nhưng thặng dư vốn của doanh
--                                   nghiệp khác có thể dương; đây là sự thật của
--                                   CTR chứ không phải quy luật trình bày
--
-- Sau migration này: 14 mã được đánh dấu (5 tài khoản điều chỉnh giảm + 9 dòng chi).
--
-- Ghi chú cho chốt kiểm: hai mã dưới đây có vài dòng dương lẫn trong dữ liệu toàn
-- âm — gần như chắc chắn OCR làm rơi dấu ngoặc. Chốt kiểm sẽ nêu chúng ra:
--   DU_PHONG_PHAI_THU_KHO_DOI  164 / 3
--   TSCD_HUU_HINH_HAO_MON      128 / 6
