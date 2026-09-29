-- ---------------------------------------------------------------------------
-- Dữ liệu từ điển mã chỉ tiêu — mỗi bản ghi một câu INSERT.
--   financial_items          : 123 bản ghi
--   financial_item_mappings  : 250 bản ghi
--
-- Sinh bởi devtools/sinh_sql_financial_items.py từ
--   devtools/ra/ma_chi_tieu_de_xuat.csv
-- quét từ 130 tài liệu BCTC gốc của CTR (2019-2026), 330 trang
-- B01/B02/B03. Phủ 7.685/7.685 lần xuất hiện, 0 khoá trùng.
--
-- SAU KHI CHẠY, tệp này là nguồn sự thật. Thêm/sửa mã thì viết
-- migration mới (V3, V4...), ĐỪNG sửa tệp này — Flyway kiểm tổng kiểm.
-- ---------------------------------------------------------------------------

-- ===================== financial_items (123) =====================

INSERT INTO financial_items (ma_noi_bo, ten_chuan, loai_so, dau_am_tu_nhien, thay_the_bang, ly_do_thay_the, ghi_chu) VALUES ('BDS_DAU_TU', 'Bất động sản đầu tư', 'thoi_diem', 0, NULL, NULL, NULL);
INSERT INTO financial_items (ma_noi_bo, ten_chuan, loai_so, dau_am_tu_nhien, thay_the_bang, ly_do_thay_the, ghi_chu) VALUES ('BDS_DAU_TU_HAO_MON', 'Bất động sản đầu tư — hao mòn luỹ kế', 'thoi_diem', 1, NULL, NULL, NULL);
INSERT INTO financial_items (ma_noi_bo, ten_chuan, loai_so, dau_am_tu_nhien, thay_the_bang, ly_do_thay_the, ghi_chu) VALUES ('BDS_DAU_TU_NGUYEN_GIA', 'Bất động sản đầu tư — nguyên giá', 'thoi_diem', 0, NULL, NULL, NULL);
INSERT INTO financial_items (ma_noi_bo, ten_chuan, loai_so, dau_am_tu_nhien, thay_the_bang, ly_do_thay_the, ghi_chu) VALUES ('CAC_KHOAN_GIAM_TRU_DT', 'Các khoản giảm trừ doanh thu', 'thoi_ky', 1, NULL, NULL, NULL);
INSERT INTO financial_items (ma_noi_bo, ten_chuan, loai_so, dau_am_tu_nhien, thay_the_bang, ly_do_thay_the, ghi_chu) VALUES ('CHENH_LECH_TY_GIA', 'Chênh lệch tỷ giá hối đoái', 'thoi_diem', 0, NULL, NULL, NULL);
INSERT INTO financial_items (ma_noi_bo, ten_chuan, loai_so, dau_am_tu_nhien, thay_the_bang, ly_do_thay_the, ghi_chu) VALUES ('CHI_PHI_BAN_HANG', 'Chi phí bán hàng', 'thoi_ky', 0, NULL, NULL, NULL);
INSERT INTO financial_items (ma_noi_bo, ten_chuan, loai_so, dau_am_tu_nhien, thay_the_bang, ly_do_thay_the, ghi_chu) VALUES ('CHI_PHI_CHO_PHAN_BO_DAI_HAN', 'Chi phí chờ phân bổ dài hạn', 'thoi_diem', 0, NULL, NULL, NULL);
INSERT INTO financial_items (ma_noi_bo, ten_chuan, loai_so, dau_am_tu_nhien, thay_the_bang, ly_do_thay_the, ghi_chu) VALUES ('CHI_PHI_CHO_PHAN_BO_NGAN_HAN', 'Chi phí chờ phân bổ ngắn hạn', 'thoi_diem', 0, NULL, NULL, NULL);
INSERT INTO financial_items (ma_noi_bo, ten_chuan, loai_so, dau_am_tu_nhien, thay_the_bang, ly_do_thay_the, ghi_chu) VALUES ('CHI_PHI_DI_VAY', 'Trong đó: chi phí đi vay', 'thoi_ky', 0, NULL, NULL, NULL);
INSERT INTO financial_items (ma_noi_bo, ten_chuan, loai_so, dau_am_tu_nhien, thay_the_bang, ly_do_thay_the, ghi_chu) VALUES ('CHI_PHI_KHAC', 'Chi phí khác', 'thoi_ky', 0, NULL, NULL, NULL);
INSERT INTO financial_items (ma_noi_bo, ten_chuan, loai_so, dau_am_tu_nhien, thay_the_bang, ly_do_thay_the, ghi_chu) VALUES ('CHI_PHI_PHAI_TRA_NH', 'Chi phí phải trả ngắn hạn', 'thoi_diem', 0, NULL, NULL, NULL);
INSERT INTO financial_items (ma_noi_bo, ten_chuan, loai_so, dau_am_tu_nhien, thay_the_bang, ly_do_thay_the, ghi_chu) VALUES ('CHI_PHI_QUAN_LY', 'Chi phí quản lý doanh nghiệp', 'thoi_ky', 0, NULL, NULL, NULL);
INSERT INTO financial_items (ma_noi_bo, ten_chuan, loai_so, dau_am_tu_nhien, thay_the_bang, ly_do_thay_the, ghi_chu) VALUES ('CHI_PHI_TAI_CHINH', 'Chi phí tài chính', 'thoi_ky', 0, NULL, NULL, NULL);
INSERT INTO financial_items (ma_noi_bo, ten_chuan, loai_so, dau_am_tu_nhien, thay_the_bang, ly_do_thay_the, ghi_chu) VALUES ('CHI_PHI_THUE_TNDN_HIEN_HANH', 'Chi phí thuế TNDN hiện hành', 'thoi_ky', 0, NULL, NULL, NULL);
INSERT INTO financial_items (ma_noi_bo, ten_chuan, loai_so, dau_am_tu_nhien, thay_the_bang, ly_do_thay_the, ghi_chu) VALUES ('CHI_PHI_THUE_TNDN_HOAN_LAI', 'Chi phí thuế TNDN hoãn lại', 'thoi_ky', 0, NULL, NULL, NULL);
INSERT INTO financial_items (ma_noi_bo, ten_chuan, loai_so, dau_am_tu_nhien, thay_the_bang, ly_do_thay_the, ghi_chu) VALUES ('CHI_PHI_XDCB_DO_DANG', 'Chi phí xây dựng cơ bản dở dang', 'thoi_diem', 0, NULL, NULL, NULL);
INSERT INTO financial_items (ma_noi_bo, ten_chuan, loai_so, dau_am_tu_nhien, thay_the_bang, ly_do_thay_the, ghi_chu) VALUES ('DAU_TU_CONG_TY_CON', 'Đầu tư vào công ty con', 'thoi_diem', 0, NULL, NULL, NULL);
INSERT INTO financial_items (ma_noi_bo, ten_chuan, loai_so, dau_am_tu_nhien, thay_the_bang, ly_do_thay_the, ghi_chu) VALUES ('DAU_TU_NAM_GIU_DAO_HAN_NH', 'Đầu tư nắm giữ đến ngày đáo hạn ngắn hạn', 'thoi_diem', 0, NULL, NULL, NULL);
INSERT INTO financial_items (ma_noi_bo, ten_chuan, loai_so, dau_am_tu_nhien, thay_the_bang, ly_do_thay_the, ghi_chu) VALUES ('DAU_TU_TC_DAI_HAN', 'Đầu tư tài chính dài hạn', 'thoi_diem', 0, NULL, NULL, NULL);
INSERT INTO financial_items (ma_noi_bo, ten_chuan, loai_so, dau_am_tu_nhien, thay_the_bang, ly_do_thay_the, ghi_chu) VALUES ('DAU_TU_TC_NGAN_HAN', 'Đầu tư tài chính ngắn hạn', 'thoi_diem', 0, NULL, NULL, NULL);
INSERT INTO financial_items (ma_noi_bo, ten_chuan, loai_so, dau_am_tu_nhien, thay_the_bang, ly_do_thay_the, ghi_chu) VALUES ('DOANH_THU_BAN_HANG', 'Doanh thu bán hàng và cung cấp dịch vụ', 'thoi_ky', 0, NULL, NULL, NULL);
INSERT INTO financial_items (ma_noi_bo, ten_chuan, loai_so, dau_am_tu_nhien, thay_the_bang, ly_do_thay_the, ghi_chu) VALUES ('DOANH_THU_CHUA_THUC_HIEN_NH', 'Doanh thu chưa thực hiện ngắn hạn', 'thoi_diem', 0, NULL, NULL, NULL);
INSERT INTO financial_items (ma_noi_bo, ten_chuan, loai_so, dau_am_tu_nhien, thay_the_bang, ly_do_thay_the, ghi_chu) VALUES ('DOANH_THU_TAI_CHINH', 'Doanh thu hoạt động tài chính', 'thoi_ky', 0, NULL, NULL, NULL);
INSERT INTO financial_items (ma_noi_bo, ten_chuan, loai_so, dau_am_tu_nhien, thay_the_bang, ly_do_thay_the, ghi_chu) VALUES ('DOANH_THU_THUAN', 'Doanh thu thuần về bán hàng và cung cấp dịch vụ', 'thoi_ky', 0, NULL, NULL, NULL);
INSERT INTO financial_items (ma_noi_bo, ten_chuan, loai_so, dau_am_tu_nhien, thay_the_bang, ly_do_thay_the, ghi_chu) VALUES ('DU_PHONG_GIAM_GIA_HTK', 'Dự phòng giảm giá hàng tồn kho', 'thoi_diem', 1, NULL, NULL, NULL);
INSERT INTO financial_items (ma_noi_bo, ten_chuan, loai_so, dau_am_tu_nhien, thay_the_bang, ly_do_thay_the, ghi_chu) VALUES ('DU_PHONG_PHAI_THU_KHO_DOI', 'Dự phòng phải thu ngắn hạn khó đòi', 'thoi_diem', 1, NULL, NULL, NULL);
INSERT INTO financial_items (ma_noi_bo, ten_chuan, loai_so, dau_am_tu_nhien, thay_the_bang, ly_do_thay_the, ghi_chu) VALUES ('DU_PHONG_PHAI_TRA_NH', 'Dự phòng phải trả ngắn hạn', 'thoi_diem', 1, NULL, NULL, NULL);
INSERT INTO financial_items (ma_noi_bo, ten_chuan, loai_so, dau_am_tu_nhien, thay_the_bang, ly_do_thay_the, ghi_chu) VALUES ('GIA_VON_HANG_BAN', 'Giá vốn hàng bán', 'thoi_ky', 0, NULL, NULL, NULL);
INSERT INTO financial_items (ma_noi_bo, ten_chuan, loai_so, dau_am_tu_nhien, thay_the_bang, ly_do_thay_the, ghi_chu) VALUES ('HANG_TON_KHO_M140', 'Hàng tồn kho — mã 140 (thuần, đã trừ dự phòng)', 'thoi_diem', 0, NULL, NULL, NULL);
INSERT INTO financial_items (ma_noi_bo, ten_chuan, loai_so, dau_am_tu_nhien, thay_the_bang, ly_do_thay_the, ghi_chu) VALUES ('HANG_TON_KHO_M141', 'Hàng tồn kho — mã 141 (giá gốc, chưa trừ dự phòng)', 'thoi_diem', 0, NULL, NULL, NULL);
INSERT INTO financial_items (ma_noi_bo, ten_chuan, loai_so, dau_am_tu_nhien, thay_the_bang, ly_do_thay_the, ghi_chu) VALUES ('LAI_LO_BAN_THANH_LY_BDSDT', 'Lãi/lỗ của hoạt động bán, thanh lý bất động sản đầu tư', 'thoi_ky', 0, NULL, NULL, NULL);
INSERT INTO financial_items (ma_noi_bo, ten_chuan, loai_so, dau_am_tu_nhien, thay_the_bang, ly_do_thay_the, ghi_chu) VALUES ('LAI_LO_LIEN_DOANH_LIEN_KET', 'Phần lãi/lỗ trong công ty liên doanh, liên kết', 'thoi_ky', 0, NULL, NULL, NULL);
INSERT INTO financial_items (ma_noi_bo, ten_chuan, loai_so, dau_am_tu_nhien, thay_the_bang, ly_do_thay_the, ghi_chu) VALUES ('LCTT_ANH_HUONG_TY_GIA', 'LCTT — Ảnh hưởng thay đổi tỷ giá quy đổi ngoại tệ', 'thoi_ky', 0, NULL, NULL, NULL);
INSERT INTO financial_items (ma_noi_bo, ten_chuan, loai_so, dau_am_tu_nhien, thay_the_bang, ly_do_thay_the, ghi_chu) VALUES ('LCTT_CHI_CHO_VAY', 'LCTT — Tiền chi cho vay, mua công cụ nợ', 'thoi_ky', 0, NULL, NULL, NULL);
INSERT INTO financial_items (ma_noi_bo, ten_chuan, loai_so, dau_am_tu_nhien, thay_the_bang, ly_do_thay_the, ghi_chu) VALUES ('LCTT_CHI_GOP_VON', 'LCTT — Tiền chi đầu tư góp vốn vào đơn vị khác', 'thoi_ky', 0, NULL, NULL, NULL);
INSERT INTO financial_items (ma_noi_bo, ten_chuan, loai_so, dau_am_tu_nhien, thay_the_bang, ly_do_thay_the, ghi_chu) VALUES ('LCTT_CHI_MUA_TSCD', 'LCTT — Tiền chi mua sắm, xây dựng TSCĐ', 'thoi_ky', 0, NULL, NULL, NULL);
INSERT INTO financial_items (ma_noi_bo, ten_chuan, loai_so, dau_am_tu_nhien, thay_the_bang, ly_do_thay_the, ghi_chu) VALUES ('LCTT_CHI_PHI_DI_VAY', 'LCTT — Chi phí đi vay', 'thoi_ky', 0, NULL, NULL, NULL);
INSERT INTO financial_items (ma_noi_bo, ten_chuan, loai_so, dau_am_tu_nhien, thay_the_bang, ly_do_thay_the, ghi_chu) VALUES ('LCTT_CHI_PHI_DI_VAY_DA_TRA', 'LCTT — Chi phí đi vay đã trả', 'thoi_ky', 0, NULL, NULL, NULL);
INSERT INTO financial_items (ma_noi_bo, ten_chuan, loai_so, dau_am_tu_nhien, thay_the_bang, ly_do_thay_the, ghi_chu) VALUES ('LCTT_CO_TUC_DA_TRA', 'LCTT — Cổ tức, lợi nhuận đã trả cho chủ sở hữu', 'thoi_ky', 0, NULL, NULL, NULL);
INSERT INTO financial_items (ma_noi_bo, ten_chuan, loai_so, dau_am_tu_nhien, thay_the_bang, ly_do_thay_the, ghi_chu) VALUES ('LCTT_DIEU_CHINH_KHAC', 'LCTT — Các khoản điều chỉnh khác', 'thoi_ky', 0, NULL, NULL, NULL);
INSERT INTO financial_items (ma_noi_bo, ten_chuan, loai_so, dau_am_tu_nhien, thay_the_bang, ly_do_thay_the, ghi_chu) VALUES ('LCTT_DU_PHONG', 'LCTT — Các khoản dự phòng', 'thoi_ky', 0, NULL, NULL, NULL);
INSERT INTO financial_items (ma_noi_bo, ten_chuan, loai_so, dau_am_tu_nhien, thay_the_bang, ly_do_thay_the, ghi_chu) VALUES ('LCTT_KHAU_HAO', 'LCTT — Khấu hao TSCĐ và bất động sản đầu tư', 'thoi_ky', 0, NULL, NULL, NULL);
INSERT INTO financial_items (ma_noi_bo, ten_chuan, loai_so, dau_am_tu_nhien, thay_the_bang, ly_do_thay_the, ghi_chu) VALUES ('LCTT_LAI_LO_DAU_TU', 'LCTT — Lãi/lỗ từ hoạt động đầu tư', 'thoi_ky', 0, NULL, NULL, NULL);
INSERT INTO financial_items (ma_noi_bo, ten_chuan, loai_so, dau_am_tu_nhien, thay_the_bang, ly_do_thay_the, ghi_chu) VALUES ('LCTT_LAI_LO_TY_GIA', 'LCTT — Lãi/lỗ chênh lệch tỷ giá do đánh giá lại', 'thoi_ky', 0, NULL, NULL, NULL);
INSERT INTO financial_items (ma_noi_bo, ten_chuan, loai_so, dau_am_tu_nhien, thay_the_bang, ly_do_thay_the, ghi_chu) VALUES ('LCTT_LN_TRUOC_THAY_DOI_VLD', 'LCTT — Lợi nhuận từ HĐKD trước thay đổi vốn lưu động', 'thoi_ky', 0, NULL, NULL, NULL);
INSERT INTO financial_items (ma_noi_bo, ten_chuan, loai_so, dau_am_tu_nhien, thay_the_bang, ly_do_thay_the, ghi_chu) VALUES ('LCTT_LOI_NHUAN_TRUOC_THUE', 'LCTT — Lợi nhuận trước thuế', 'thoi_ky', 0, NULL, NULL, NULL);
INSERT INTO financial_items (ma_noi_bo, ten_chuan, loai_so, dau_am_tu_nhien, thay_the_bang, ly_do_thay_the, ghi_chu) VALUES ('LCTT_TANG_GIAM_CHI_PHI_CHO_PHAN_BO', 'LCTT — Tăng/giảm chi phí chờ phân bổ', 'thoi_ky', 0, NULL, NULL, NULL);
INSERT INTO financial_items (ma_noi_bo, ten_chuan, loai_so, dau_am_tu_nhien, thay_the_bang, ly_do_thay_the, ghi_chu) VALUES ('LCTT_TANG_GIAM_HTK', 'LCTT — Tăng/giảm hàng tồn kho', 'thoi_ky', 0, NULL, NULL, NULL);
INSERT INTO financial_items (ma_noi_bo, ten_chuan, loai_so, dau_am_tu_nhien, thay_the_bang, ly_do_thay_the, ghi_chu) VALUES ('LCTT_TANG_GIAM_PHAI_THU', 'LCTT — Tăng/giảm các khoản phải thu', 'thoi_ky', 0, NULL, NULL, NULL);
INSERT INTO financial_items (ma_noi_bo, ten_chuan, loai_so, dau_am_tu_nhien, thay_the_bang, ly_do_thay_the, ghi_chu) VALUES ('LCTT_TANG_GIAM_PHAI_TRA', 'LCTT — Tăng/giảm các khoản phải trả', 'thoi_ky', 0, NULL, NULL, NULL);
INSERT INTO financial_items (ma_noi_bo, ten_chuan, loai_so, dau_am_tu_nhien, thay_the_bang, ly_do_thay_the, ghi_chu) VALUES ('LCTT_THUAN_HDDT', 'Lưu chuyển tiền thuần từ hoạt động đầu tư', 'thoi_ky', 0, NULL, NULL, NULL);
INSERT INTO financial_items (ma_noi_bo, ten_chuan, loai_so, dau_am_tu_nhien, thay_the_bang, ly_do_thay_the, ghi_chu) VALUES ('LCTT_THUAN_HDKD', 'Lưu chuyển tiền thuần từ hoạt động kinh doanh', 'thoi_ky', 0, NULL, NULL, NULL);
INSERT INTO financial_items (ma_noi_bo, ten_chuan, loai_so, dau_am_tu_nhien, thay_the_bang, ly_do_thay_the, ghi_chu) VALUES ('LCTT_THUAN_HDTC', 'Lưu chuyển tiền thuần từ hoạt động tài chính', 'thoi_ky', 0, NULL, NULL, NULL);
INSERT INTO financial_items (ma_noi_bo, ten_chuan, loai_so, dau_am_tu_nhien, thay_the_bang, ly_do_thay_the, ghi_chu) VALUES ('LCTT_THUAN_TRONG_KY', 'Lưu chuyển tiền thuần trong kỳ', 'thoi_ky', 0, NULL, NULL, NULL);
INSERT INTO financial_items (ma_noi_bo, ten_chuan, loai_so, dau_am_tu_nhien, thay_the_bang, ly_do_thay_the, ghi_chu) VALUES ('LCTT_THUE_TNDN_DA_NOP', 'LCTT — Thuế TNDN đã nộp', 'thoi_ky', 0, NULL, NULL, NULL);
INSERT INTO financial_items (ma_noi_bo, ten_chuan, loai_so, dau_am_tu_nhien, thay_the_bang, ly_do_thay_the, ghi_chu) VALUES ('LCTT_THU_DI_VAY', 'LCTT — Tiền thu từ đi vay', 'thoi_ky', 0, NULL, NULL, NULL);
INSERT INTO financial_items (ma_noi_bo, ten_chuan, loai_so, dau_am_tu_nhien, thay_the_bang, ly_do_thay_the, ghi_chu) VALUES ('LCTT_THU_HOI_CHO_VAY', 'LCTT — Tiền thu hồi cho vay, bán lại công cụ nợ', 'thoi_ky', 0, NULL, NULL, NULL);
INSERT INTO financial_items (ma_noi_bo, ten_chuan, loai_so, dau_am_tu_nhien, thay_the_bang, ly_do_thay_the, ghi_chu) VALUES ('LCTT_THU_HOI_GOP_VON', 'LCTT — Tiền thu hồi đầu tư góp vốn', 'thoi_ky', 0, NULL, NULL, NULL);
INSERT INTO financial_items (ma_noi_bo, ten_chuan, loai_so, dau_am_tu_nhien, thay_the_bang, ly_do_thay_the, ghi_chu) VALUES ('LCTT_THU_LAI_CO_TUC', 'LCTT — Tiền thu lãi cho vay, cổ tức, lợi nhuận được chia', 'thoi_ky', 0, NULL, NULL, NULL);
INSERT INTO financial_items (ma_noi_bo, ten_chuan, loai_so, dau_am_tu_nhien, thay_the_bang, ly_do_thay_the, ghi_chu) VALUES ('LCTT_THU_PHAT_HANH_CP', 'LCTT — Tiền thu từ phát hành cổ phiếu, nhận vốn góp', 'thoi_ky', 0, NULL, NULL, NULL);
INSERT INTO financial_items (ma_noi_bo, ten_chuan, loai_so, dau_am_tu_nhien, thay_the_bang, ly_do_thay_the, ghi_chu) VALUES ('LCTT_THU_THANH_LY_TSCD', 'LCTT — Tiền thu thanh lý, nhượng bán TSCĐ', 'thoi_ky', 0, NULL, NULL, NULL);
INSERT INTO financial_items (ma_noi_bo, ten_chuan, loai_so, dau_am_tu_nhien, thay_the_bang, ly_do_thay_the, ghi_chu) VALUES ('LCTT_TIEN_CHI_KHAC_HDKD', 'LCTT — Tiền chi khác cho hoạt động kinh doanh', 'thoi_ky', 0, NULL, NULL, NULL);
INSERT INTO financial_items (ma_noi_bo, ten_chuan, loai_so, dau_am_tu_nhien, thay_the_bang, ly_do_thay_the, ghi_chu) VALUES ('LCTT_TIEN_THU_KHAC_HDKD', 'LCTT — Tiền thu khác từ hoạt động kinh doanh', 'thoi_ky', 0, NULL, NULL, NULL);
INSERT INTO financial_items (ma_noi_bo, ten_chuan, loai_so, dau_am_tu_nhien, thay_the_bang, ly_do_thay_the, ghi_chu) VALUES ('LCTT_TRA_NO_GOC_VAY', 'LCTT — Tiền trả nợ gốc vay', 'thoi_ky', 0, NULL, NULL, NULL);
INSERT INTO financial_items (ma_noi_bo, ten_chuan, loai_so, dau_am_tu_nhien, thay_the_bang, ly_do_thay_the, ghi_chu) VALUES ('LCTT_TT_CHI_NGUOI_CUNG_CAP', 'LCTT(TT) — Tiền chi trả người cung cấp hàng hoá, dịch vụ', 'thoi_ky', 0, NULL, NULL, NULL);
INSERT INTO financial_items (ma_noi_bo, ten_chuan, loai_so, dau_am_tu_nhien, thay_the_bang, ly_do_thay_the, ghi_chu) VALUES ('LCTT_TT_CHI_NGUOI_LAO_DONG', 'LCTT(TT) — Tiền chi trả cho người lao động', 'thoi_ky', 0, NULL, NULL, NULL);
INSERT INTO financial_items (ma_noi_bo, ten_chuan, loai_so, dau_am_tu_nhien, thay_the_bang, ly_do_thay_the, ghi_chu) VALUES ('LCTT_TT_THU_BAN_HANG', 'LCTT(TT) — Tiền thu từ bán hàng, cung cấp dịch vụ', 'thoi_ky', 0, NULL, NULL, NULL);
INSERT INTO financial_items (ma_noi_bo, ten_chuan, loai_so, dau_am_tu_nhien, thay_the_bang, ly_do_thay_the, ghi_chu) VALUES ('LNST_CDKKS', 'Lợi nhuận sau thuế của cổ đông không kiểm soát', 'thoi_ky', 0, NULL, NULL, NULL);
INSERT INTO financial_items (ma_noi_bo, ten_chuan, loai_so, dau_am_tu_nhien, thay_the_bang, ly_do_thay_the, ghi_chu) VALUES ('LNST_CHUA_PHAN_PHOI', 'Lợi nhuận sau thuế chưa phân phối', 'thoi_diem', 0, NULL, NULL, NULL);
INSERT INTO financial_items (ma_noi_bo, ten_chuan, loai_so, dau_am_tu_nhien, thay_the_bang, ly_do_thay_the, ghi_chu) VALUES ('LNST_CONG_TY_ME', 'Lợi nhuận sau thuế của công ty mẹ', 'thoi_ky', 0, NULL, NULL, NULL);
INSERT INTO financial_items (ma_noi_bo, ten_chuan, loai_so, dau_am_tu_nhien, thay_the_bang, ly_do_thay_the, ghi_chu) VALUES ('LOI_ICH_CDKKS', 'Lợi ích cổ đông không kiểm soát', 'thoi_diem', 0, NULL, NULL, NULL);
INSERT INTO financial_items (ma_noi_bo, ten_chuan, loai_so, dau_am_tu_nhien, thay_the_bang, ly_do_thay_the, ghi_chu) VALUES ('LOI_NHUAN_GOP', 'Lợi nhuận gộp về bán hàng và cung cấp dịch vụ', 'thoi_ky', 0, NULL, NULL, NULL);
INSERT INTO financial_items (ma_noi_bo, ten_chuan, loai_so, dau_am_tu_nhien, thay_the_bang, ly_do_thay_the, ghi_chu) VALUES ('LOI_NHUAN_KHAC', 'Lợi nhuận khác', 'thoi_ky', 0, NULL, NULL, NULL);
INSERT INTO financial_items (ma_noi_bo, ten_chuan, loai_so, dau_am_tu_nhien, thay_the_bang, ly_do_thay_the, ghi_chu) VALUES ('LOI_NHUAN_SAU_THUE', 'Lợi nhuận sau thuế TNDN', 'thoi_ky', 0, NULL, NULL, NULL);
INSERT INTO financial_items (ma_noi_bo, ten_chuan, loai_so, dau_am_tu_nhien, thay_the_bang, ly_do_thay_the, ghi_chu) VALUES ('LOI_NHUAN_THUAN_HDKD', 'Lợi nhuận thuần từ hoạt động kinh doanh', 'thoi_ky', 0, NULL, NULL, NULL);
INSERT INTO financial_items (ma_noi_bo, ten_chuan, loai_so, dau_am_tu_nhien, thay_the_bang, ly_do_thay_the, ghi_chu) VALUES ('LOI_NHUAN_TRUOC_THUE', 'Tổng lợi nhuận kế toán trước thuế', 'thoi_ky', 0, NULL, NULL, NULL);
INSERT INTO financial_items (ma_noi_bo, ten_chuan, loai_so, dau_am_tu_nhien, thay_the_bang, ly_do_thay_the, ghi_chu) VALUES ('NGUOI_MUA_TRA_TIEN_TRUOC_NH', 'Người mua trả tiền trước ngắn hạn', 'thoi_diem', 0, NULL, NULL, NULL);
INSERT INTO financial_items (ma_noi_bo, ten_chuan, loai_so, dau_am_tu_nhien, thay_the_bang, ly_do_thay_the, ghi_chu) VALUES ('NO_DAI_HAN', 'Nợ dài hạn', 'thoi_diem', 0, NULL, NULL, NULL);
INSERT INTO financial_items (ma_noi_bo, ten_chuan, loai_so, dau_am_tu_nhien, thay_the_bang, ly_do_thay_the, ghi_chu) VALUES ('NO_NGAN_HAN', 'Nợ ngắn hạn', 'thoi_diem', 0, NULL, NULL, NULL);
INSERT INTO financial_items (ma_noi_bo, ten_chuan, loai_so, dau_am_tu_nhien, thay_the_bang, ly_do_thay_the, ghi_chu) VALUES ('NO_PHAI_TRA', 'Nợ phải trả', 'thoi_diem', 0, NULL, NULL, NULL);
INSERT INTO financial_items (ma_noi_bo, ten_chuan, loai_so, dau_am_tu_nhien, thay_the_bang, ly_do_thay_the, ghi_chu) VALUES ('PHAI_THU_KHACH_HANG_NH', 'Phải thu ngắn hạn của khách hàng', 'thoi_diem', 0, NULL, NULL, NULL);
INSERT INTO financial_items (ma_noi_bo, ten_chuan, loai_so, dau_am_tu_nhien, thay_the_bang, ly_do_thay_the, ghi_chu) VALUES ('PHAI_THU_NGAN_HAN', 'Các khoản phải thu ngắn hạn', 'thoi_diem', 0, NULL, NULL, NULL);
INSERT INTO financial_items (ma_noi_bo, ten_chuan, loai_so, dau_am_tu_nhien, thay_the_bang, ly_do_thay_the, ghi_chu) VALUES ('PHAI_THU_NGAN_HAN_KHAC', 'Phải thu ngắn hạn khác', 'thoi_diem', 0, NULL, NULL, NULL);
INSERT INTO financial_items (ma_noi_bo, ten_chuan, loai_so, dau_am_tu_nhien, thay_the_bang, ly_do_thay_the, ghi_chu) VALUES ('PHAI_TRA_CO_TUC_LOI_NHUAN', 'Phải trả cổ tức, lợi nhuận', 'thoi_diem', 0, NULL, NULL, NULL);
INSERT INTO financial_items (ma_noi_bo, ten_chuan, loai_so, dau_am_tu_nhien, thay_the_bang, ly_do_thay_the, ghi_chu) VALUES ('PHAI_TRA_DAI_HAN_KHAC', 'Phải trả dài hạn khác', 'thoi_diem', 0, NULL, NULL, NULL);
INSERT INTO financial_items (ma_noi_bo, ten_chuan, loai_so, dau_am_tu_nhien, thay_the_bang, ly_do_thay_the, ghi_chu) VALUES ('PHAI_TRA_NGAN_HAN_KHAC', 'Phải trả ngắn hạn khác', 'thoi_diem', 0, NULL, NULL, NULL);
INSERT INTO financial_items (ma_noi_bo, ten_chuan, loai_so, dau_am_tu_nhien, thay_the_bang, ly_do_thay_the, ghi_chu) VALUES ('PHAI_TRA_NGUOI_BAN_NH', 'Phải trả người bán ngắn hạn', 'thoi_diem', 0, NULL, NULL, NULL);
INSERT INTO financial_items (ma_noi_bo, ten_chuan, loai_so, dau_am_tu_nhien, thay_the_bang, ly_do_thay_the, ghi_chu) VALUES ('PHAI_TRA_NGUOI_LAO_DONG', 'Phải trả người lao động', 'thoi_diem', 0, NULL, NULL, NULL);
INSERT INTO financial_items (ma_noi_bo, ten_chuan, loai_so, dau_am_tu_nhien, thay_the_bang, ly_do_thay_the, ghi_chu) VALUES ('PHAI_TRA_NOI_BO_NH', 'Phải trả nội bộ ngắn hạn', 'thoi_diem', 0, NULL, NULL, NULL);
INSERT INTO financial_items (ma_noi_bo, ten_chuan, loai_so, dau_am_tu_nhien, thay_the_bang, ly_do_thay_the, ghi_chu) VALUES ('PHAI_TRA_TIEN_DO_HDXD', 'Phải trả theo tiến độ kế hoạch hợp đồng xây dựng', 'thoi_diem', 0, NULL, NULL, NULL);
INSERT INTO financial_items (ma_noi_bo, ten_chuan, loai_so, dau_am_tu_nhien, thay_the_bang, ly_do_thay_the, ghi_chu) VALUES ('QUY_DAU_TU_PHAT_TRIEN', 'Quỹ đầu tư phát triển', 'thoi_diem', 0, NULL, NULL, NULL);
INSERT INTO financial_items (ma_noi_bo, ten_chuan, loai_so, dau_am_tu_nhien, thay_the_bang, ly_do_thay_the, ghi_chu) VALUES ('QUY_KHEN_THUONG_PHUC_LOI', 'Quỹ khen thưởng, phúc lợi', 'thoi_diem', 0, NULL, NULL, NULL);
INSERT INTO financial_items (ma_noi_bo, ten_chuan, loai_so, dau_am_tu_nhien, thay_the_bang, ly_do_thay_the, ghi_chu) VALUES ('TAI_SAN_CO_DINH', 'Tài sản cố định', 'thoi_diem', 0, NULL, NULL, NULL);
INSERT INTO financial_items (ma_noi_bo, ten_chuan, loai_so, dau_am_tu_nhien, thay_the_bang, ly_do_thay_the, ghi_chu) VALUES ('TAI_SAN_DAI_HAN', 'Tài sản dài hạn', 'thoi_diem', 0, NULL, NULL, NULL);
INSERT INTO financial_items (ma_noi_bo, ten_chuan, loai_so, dau_am_tu_nhien, thay_the_bang, ly_do_thay_the, ghi_chu) VALUES ('TAI_SAN_DAI_HAN_KHAC', 'Tài sản dài hạn khác', 'thoi_diem', 0, NULL, NULL, NULL);
INSERT INTO financial_items (ma_noi_bo, ten_chuan, loai_so, dau_am_tu_nhien, thay_the_bang, ly_do_thay_the, ghi_chu) VALUES ('TAI_SAN_DO_DANG_DH', 'Tài sản dở dang dài hạn', 'thoi_diem', 0, NULL, NULL, NULL);
INSERT INTO financial_items (ma_noi_bo, ten_chuan, loai_so, dau_am_tu_nhien, thay_the_bang, ly_do_thay_the, ghi_chu) VALUES ('TAI_SAN_NGAN_HAN', 'Tài sản ngắn hạn', 'thoi_diem', 0, NULL, NULL, NULL);
INSERT INTO financial_items (ma_noi_bo, ten_chuan, loai_so, dau_am_tu_nhien, thay_the_bang, ly_do_thay_the, ghi_chu) VALUES ('TAI_SAN_NGAN_HAN_KHAC', 'Tài sản ngắn hạn khác', 'thoi_diem', 0, NULL, NULL, NULL);
INSERT INTO financial_items (ma_noi_bo, ten_chuan, loai_so, dau_am_tu_nhien, thay_the_bang, ly_do_thay_the, ghi_chu) VALUES ('TAI_SAN_THUE_TNDN_HOAN_LAI', 'Tài sản thuế thu nhập hoãn lại', 'thoi_diem', 0, NULL, NULL, NULL);
INSERT INTO financial_items (ma_noi_bo, ten_chuan, loai_so, dau_am_tu_nhien, thay_the_bang, ly_do_thay_the, ghi_chu) VALUES ('THANG_DU_VON', 'Thặng dư vốn', 'thoi_diem', 0, NULL, NULL, NULL);
INSERT INTO financial_items (ma_noi_bo, ten_chuan, loai_so, dau_am_tu_nhien, thay_the_bang, ly_do_thay_the, ghi_chu) VALUES ('THUE_GTGT_DUOC_KHAU_TRU', 'Thuế GTGT được khấu trừ', 'thoi_diem', 0, NULL, NULL, NULL);
INSERT INTO financial_items (ma_noi_bo, ten_chuan, loai_so, dau_am_tu_nhien, thay_the_bang, ly_do_thay_the, ghi_chu) VALUES ('THUE_PHAI_NOP_NHA_NUOC', 'Thuế và các khoản phải nộp Nhà nước', 'thoi_diem', 0, NULL, NULL, NULL);
INSERT INTO financial_items (ma_noi_bo, ten_chuan, loai_so, dau_am_tu_nhien, thay_the_bang, ly_do_thay_the, ghi_chu) VALUES ('THUE_PHAI_THU_NHA_NUOC', 'Thuế và các khoản khác phải thu Nhà nước', 'thoi_diem', 0, NULL, NULL, NULL);
INSERT INTO financial_items (ma_noi_bo, ten_chuan, loai_so, dau_am_tu_nhien, thay_the_bang, ly_do_thay_the, ghi_chu) VALUES ('THU_NHAP_KHAC', 'Thu nhập khác', 'thoi_ky', 0, NULL, NULL, NULL);
INSERT INTO financial_items (ma_noi_bo, ten_chuan, loai_so, dau_am_tu_nhien, thay_the_bang, ly_do_thay_the, ghi_chu) VALUES ('TIEN_CUOI_KY', 'Tiền và tương đương tiền cuối kỳ', 'thoi_diem', 0, NULL, NULL, 'Số dư tại CUỐI kỳ — dùng den_ngay làm mốc; phải bằng TIEN_VA_TUONG_DUONG_TIEN cùng ngày');
INSERT INTO financial_items (ma_noi_bo, ten_chuan, loai_so, dau_am_tu_nhien, thay_the_bang, ly_do_thay_the, ghi_chu) VALUES ('TIEN_DAU_KY', 'Tiền và tương đương tiền đầu kỳ', 'thoi_diem', 0, NULL, NULL, 'Số dư tại ĐẦU kỳ — dùng tu_ngay làm mốc');
INSERT INTO financial_items (ma_noi_bo, ten_chuan, loai_so, dau_am_tu_nhien, thay_the_bang, ly_do_thay_the, ghi_chu) VALUES ('TIEN_M111', 'Tiền (tiền mặt và tiền gửi không kỳ hạn)', 'thoi_diem', 0, NULL, NULL, NULL);
INSERT INTO financial_items (ma_noi_bo, ten_chuan, loai_so, dau_am_tu_nhien, thay_the_bang, ly_do_thay_the, ghi_chu) VALUES ('TIEN_VA_TUONG_DUONG_TIEN', 'Tiền và các khoản tương đương tiền', 'thoi_diem', 0, NULL, NULL, NULL);
INSERT INTO financial_items (ma_noi_bo, ten_chuan, loai_so, dau_am_tu_nhien, thay_the_bang, ly_do_thay_the, ghi_chu) VALUES ('TONG_NGUON_VON', 'Tổng cộng nguồn vốn', 'thoi_diem', 0, NULL, NULL, NULL);
INSERT INTO financial_items (ma_noi_bo, ten_chuan, loai_so, dau_am_tu_nhien, thay_the_bang, ly_do_thay_the, ghi_chu) VALUES ('TONG_TAI_SAN', 'Tổng cộng tài sản', 'thoi_diem', 0, NULL, NULL, NULL);
INSERT INTO financial_items (ma_noi_bo, ten_chuan, loai_so, dau_am_tu_nhien, thay_the_bang, ly_do_thay_the, ghi_chu) VALUES ('TRA_TRUOC_NGUOI_BAN_NH', 'Trả trước cho người bán ngắn hạn', 'thoi_diem', 0, NULL, NULL, NULL);
INSERT INTO financial_items (ma_noi_bo, ten_chuan, loai_so, dau_am_tu_nhien, thay_the_bang, ly_do_thay_the, ghi_chu) VALUES ('TSCD_HUU_HINH', 'Tài sản cố định hữu hình', 'thoi_diem', 0, NULL, NULL, NULL);
INSERT INTO financial_items (ma_noi_bo, ten_chuan, loai_so, dau_am_tu_nhien, thay_the_bang, ly_do_thay_the, ghi_chu) VALUES ('TSCD_HUU_HINH_HAO_MON', 'TSCĐ hữu hình — hao mòn luỹ kế', 'thoi_diem', 1, NULL, NULL, NULL);
INSERT INTO financial_items (ma_noi_bo, ten_chuan, loai_so, dau_am_tu_nhien, thay_the_bang, ly_do_thay_the, ghi_chu) VALUES ('TSCD_HUU_HINH_NGUYEN_GIA', 'TSCĐ hữu hình — nguyên giá', 'thoi_diem', 0, NULL, NULL, NULL);
INSERT INTO financial_items (ma_noi_bo, ten_chuan, loai_so, dau_am_tu_nhien, thay_the_bang, ly_do_thay_the, ghi_chu) VALUES ('TSCD_VO_HINH', 'Tài sản cố định vô hình', 'thoi_diem', 0, NULL, NULL, NULL);
INSERT INTO financial_items (ma_noi_bo, ten_chuan, loai_so, dau_am_tu_nhien, thay_the_bang, ly_do_thay_the, ghi_chu) VALUES ('TSCD_VO_HINH_HAO_MON', 'TSCĐ vô hình — hao mòn luỹ kế', 'thoi_diem', 1, NULL, NULL, NULL);
INSERT INTO financial_items (ma_noi_bo, ten_chuan, loai_so, dau_am_tu_nhien, thay_the_bang, ly_do_thay_the, ghi_chu) VALUES ('TSCD_VO_HINH_NGUYEN_GIA', 'TSCĐ vô hình — nguyên giá', 'thoi_diem', 0, NULL, NULL, NULL);
INSERT INTO financial_items (ma_noi_bo, ten_chuan, loai_so, dau_am_tu_nhien, thay_the_bang, ly_do_thay_the, ghi_chu) VALUES ('TUONG_DUONG_TIEN', 'Các khoản tương đương tiền', 'thoi_diem', 0, NULL, NULL, NULL);
INSERT INTO financial_items (ma_noi_bo, ten_chuan, loai_so, dau_am_tu_nhien, thay_the_bang, ly_do_thay_the, ghi_chu) VALUES ('VAY_NO_THUE_TC_DH', 'Vay và nợ thuê tài chính dài hạn', 'thoi_diem', 0, NULL, NULL, NULL);
INSERT INTO financial_items (ma_noi_bo, ten_chuan, loai_so, dau_am_tu_nhien, thay_the_bang, ly_do_thay_the, ghi_chu) VALUES ('VAY_NO_THUE_TC_NH', 'Vay và nợ thuê tài chính ngắn hạn', 'thoi_diem', 0, NULL, NULL, NULL);
INSERT INTO financial_items (ma_noi_bo, ten_chuan, loai_so, dau_am_tu_nhien, thay_the_bang, ly_do_thay_the, ghi_chu) VALUES ('VON_CHU_SO_HUU_M400', 'Vốn chủ sở hữu — mã 400 (khối D, tổng)', 'thoi_diem', 0, NULL, NULL, NULL);
INSERT INTO financial_items (ma_noi_bo, ten_chuan, loai_so, dau_am_tu_nhien, thay_the_bang, ly_do_thay_the, ghi_chu) VALUES ('VON_GOP_CHU_SO_HUU', 'Vốn góp của chủ sở hữu', 'thoi_diem', 0, NULL, NULL, NULL);
INSERT INTO financial_items (ma_noi_bo, ten_chuan, loai_so, dau_am_tu_nhien, thay_the_bang, ly_do_thay_the, ghi_chu) VALUES ('VON_CHU_SO_HUU_M410', 'Vốn chủ sở hữu — mã 410 (nhóm I, chỉ TT200)', 'thoi_diem', 0, 'VON_CHU_SO_HUU_M400', 'TT99 (từ 2026) bỏ mã 410; mã 400 là tổng vốn chủ sở hữu', NULL);


-- ================ financial_item_mappings (250) ================

-- ----- TT200 / B01 -----
INSERT INTO financial_item_mappings (che_do, mau_bieu, ma, ma_noi_bo, ten_tren_bao_cao, so_lan_gap, so_bien_the, che_do_suy_doan, ghi_chu) VALUES ('TT200', 'B01', '300', 'NO_PHAI_TRA', 'NỢ PHẢI TRẢ', 78, 1, 0, NULL);
INSERT INTO financial_item_mappings (che_do, mau_bieu, ma, ma_noi_bo, ten_tren_bao_cao, so_lan_gap, so_bien_the, che_do_suy_doan, ghi_chu) VALUES ('TT200', 'B01', '400', 'VON_CHU_SO_HUU_M400', 'VỐN CHỦ SỞ HỮU', 78, 1, 0, NULL);
INSERT INTO financial_item_mappings (che_do, mau_bieu, ma, ma_noi_bo, ten_tren_bao_cao, so_lan_gap, so_bien_the, che_do_suy_doan, ghi_chu) VALUES ('TT200', 'B01', '440', 'TONG_NGUON_VON', 'TỔNG CỘNG NGUỒN VỐN', 78, 1, 0, NULL);
INSERT INTO financial_item_mappings (che_do, mau_bieu, ma, ma_noi_bo, ten_tren_bao_cao, so_lan_gap, so_bien_the, che_do_suy_doan, ghi_chu) VALUES ('TT200', 'B01', '310', 'NO_NGAN_HAN', 'Nợ ngắn hạn', 76, 1, 0, NULL);
INSERT INTO financial_item_mappings (che_do, mau_bieu, ma, ma_noi_bo, ten_tren_bao_cao, so_lan_gap, so_bien_the, che_do_suy_doan, ghi_chu) VALUES ('TT200', 'B01', '311', 'PHAI_TRA_NGUOI_BAN_NH', 'Phải trả người bán ngắn hạn', 76, 1, 0, NULL);
INSERT INTO financial_item_mappings (che_do, mau_bieu, ma, ma_noi_bo, ten_tren_bao_cao, so_lan_gap, so_bien_the, che_do_suy_doan, ghi_chu) VALUES ('TT200', 'B01', '312', 'NGUOI_MUA_TRA_TIEN_TRUOC_NH', 'Người mua trả tiền trước ngắn hạn', 76, 1, 0, NULL);
INSERT INTO financial_item_mappings (che_do, mau_bieu, ma, ma_noi_bo, ten_tren_bao_cao, so_lan_gap, so_bien_the, che_do_suy_doan, ghi_chu) VALUES ('TT200', 'B01', '313', 'THUE_PHAI_NOP_NHA_NUOC', 'Thuế và các khoản phải nộp Nhà nước', 76, 1, 0, NULL);
INSERT INTO financial_item_mappings (che_do, mau_bieu, ma, ma_noi_bo, ten_tren_bao_cao, so_lan_gap, so_bien_the, che_do_suy_doan, ghi_chu) VALUES ('TT200', 'B01', '314', 'PHAI_TRA_NGUOI_LAO_DONG', 'Phải trả người lao động', 76, 1, 0, NULL);
INSERT INTO financial_item_mappings (che_do, mau_bieu, ma, ma_noi_bo, ten_tren_bao_cao, so_lan_gap, so_bien_the, che_do_suy_doan, ghi_chu) VALUES ('TT200', 'B01', '315', 'CHI_PHI_PHAI_TRA_NH', 'Chi phí phải trả ngắn hạn', 76, 1, 0, NULL);
INSERT INTO financial_item_mappings (che_do, mau_bieu, ma, ma_noi_bo, ten_tren_bao_cao, so_lan_gap, so_bien_the, che_do_suy_doan, ghi_chu) VALUES ('TT200', 'B01', '318', 'DOANH_THU_CHUA_THUC_HIEN_NH', 'Doanh thu chưa thực hiện ngắn hạn', 76, 1, 0, NULL);
INSERT INTO financial_item_mappings (che_do, mau_bieu, ma, ma_noi_bo, ten_tren_bao_cao, so_lan_gap, so_bien_the, che_do_suy_doan, ghi_chu) VALUES ('TT200', 'B01', '319', 'PHAI_TRA_NGAN_HAN_KHAC', 'Phải trả ngắn hạn khác', 76, 1, 0, NULL);
INSERT INTO financial_item_mappings (che_do, mau_bieu, ma, ma_noi_bo, ten_tren_bao_cao, so_lan_gap, so_bien_the, che_do_suy_doan, ghi_chu) VALUES ('TT200', 'B01', '322', 'QUY_KHEN_THUONG_PHUC_LOI', 'Quỹ khen thưởng phúc lợi', 76, 2, 0, NULL);
INSERT INTO financial_item_mappings (che_do, mau_bieu, ma, ma_noi_bo, ten_tren_bao_cao, so_lan_gap, so_bien_the, che_do_suy_doan, ghi_chu) VALUES ('TT200', 'B01', '410', 'VON_CHU_SO_HUU_M410', 'Vốn chủ sở hữu', 76, 1, 0, NULL);
INSERT INTO financial_item_mappings (che_do, mau_bieu, ma, ma_noi_bo, ten_tren_bao_cao, so_lan_gap, so_bien_the, che_do_suy_doan, ghi_chu) VALUES ('TT200', 'B01', '411', 'VON_GOP_CHU_SO_HUU', 'Vốn góp của chủ sở hữu', 76, 1, 0, NULL);
INSERT INTO financial_item_mappings (che_do, mau_bieu, ma, ma_noi_bo, ten_tren_bao_cao, so_lan_gap, so_bien_the, che_do_suy_doan, ghi_chu) VALUES ('TT200', 'B01', '421', 'LNST_CHUA_PHAN_PHOI', 'Lợi nhuận sau thuế chưa phân phối', 76, 1, 0, NULL);

-- ----- TT200 / B02 -----
INSERT INTO financial_item_mappings (che_do, mau_bieu, ma, ma_noi_bo, ten_tren_bao_cao, so_lan_gap, so_bien_the, che_do_suy_doan, ghi_chu) VALUES ('TT200', 'B02', '01', 'DOANH_THU_BAN_HANG', 'Doanh thu bán hàng và cung cấp dịch vụ', 75, 1, 0, NULL);
INSERT INTO financial_item_mappings (che_do, mau_bieu, ma, ma_noi_bo, ten_tren_bao_cao, so_lan_gap, so_bien_the, che_do_suy_doan, ghi_chu) VALUES ('TT200', 'B02', '11', 'GIA_VON_HANG_BAN', 'Giá vốn hàng bán', 75, 1, 0, NULL);
INSERT INTO financial_item_mappings (che_do, mau_bieu, ma, ma_noi_bo, ten_tren_bao_cao, so_lan_gap, so_bien_the, che_do_suy_doan, ghi_chu) VALUES ('TT200', 'B02', '20', 'LOI_NHUAN_GOP', 'Lợi nhuận gộp về bán hàng và cung cấp dịch vụ', 75, 2, 0, NULL);

-- ----- TT200 / B03-GT -----
INSERT INTO financial_item_mappings (che_do, mau_bieu, ma, ma_noi_bo, ten_tren_bao_cao, so_lan_gap, so_bien_the, che_do_suy_doan, ghi_chu) VALUES ('TT200', 'B03-GT', '40', 'LCTT_THUAN_HDTC', 'Lưu chuyển tiền thuần từ hoạt động tài chính', 75, 1, 0, NULL);

-- ----- TT200 / B01 -----
INSERT INTO financial_item_mappings (che_do, mau_bieu, ma, ma_noi_bo, ten_tren_bao_cao, so_lan_gap, so_bien_the, che_do_suy_doan, ghi_chu) VALUES ('TT200', 'B01', '100', 'TAI_SAN_NGAN_HAN', 'TÀI SẢN NGẮN HẠN', 74, 1, 0, NULL);
INSERT INTO financial_item_mappings (che_do, mau_bieu, ma, ma_noi_bo, ten_tren_bao_cao, so_lan_gap, so_bien_the, che_do_suy_doan, ghi_chu) VALUES ('TT200', 'B01', '200', 'TAI_SAN_DAI_HAN', 'TÀI SẢN DÀI HẠN', 74, 1, 0, NULL);
INSERT INTO financial_item_mappings (che_do, mau_bieu, ma, ma_noi_bo, ten_tren_bao_cao, so_lan_gap, so_bien_the, che_do_suy_doan, ghi_chu) VALUES ('TT200', 'B01', '270', 'TONG_TAI_SAN', 'TỔNG CỘNG TÀI SẢN', 74, 1, 0, NULL);

-- ----- TT200 / B03-GT -----
INSERT INTO financial_item_mappings (che_do, mau_bieu, ma, ma_noi_bo, ten_tren_bao_cao, so_lan_gap, so_bien_the, che_do_suy_doan, ghi_chu) VALUES ('TT200', 'B03-GT', '03', 'LCTT_DU_PHONG', '- Các khoản dự phòng', 74, 1, 0, NULL);
INSERT INTO financial_item_mappings (che_do, mau_bieu, ma, ma_noi_bo, ten_tren_bao_cao, so_lan_gap, so_bien_the, che_do_suy_doan, ghi_chu) VALUES ('TT200', 'B03-GT', '04', 'LCTT_LAI_LO_TY_GIA', '- Lãi, lỗ chênh lệch tỷ giá hối đoái do đánh giá lại các khoản mục tiền tệ có gốc ngoại tệ', 74, 1, 0, NULL);
INSERT INTO financial_item_mappings (che_do, mau_bieu, ma, ma_noi_bo, ten_tren_bao_cao, so_lan_gap, so_bien_the, che_do_suy_doan, ghi_chu) VALUES ('TT200', 'B03-GT', '05', 'LCTT_LAI_LO_DAU_TU', '- Lãi/lỗ từ hoạt động đầu tư', 74, 1, 0, NULL);
INSERT INTO financial_item_mappings (che_do, mau_bieu, ma, ma_noi_bo, ten_tren_bao_cao, so_lan_gap, so_bien_the, che_do_suy_doan, ghi_chu) VALUES ('TT200', 'B03-GT', '08', 'LCTT_LN_TRUOC_THAY_DOI_VLD', 'Lợi nhuận từ hoạt động kinh doanh trước thay đổi vốn lưu động', 74, 1, 0, NULL);
INSERT INTO financial_item_mappings (che_do, mau_bieu, ma, ma_noi_bo, ten_tren_bao_cao, so_lan_gap, so_bien_the, che_do_suy_doan, ghi_chu) VALUES ('TT200', 'B03-GT', '09', 'LCTT_TANG_GIAM_PHAI_THU', '- Tăng/giảm các khoản phải thu', 74, 1, 0, NULL);
INSERT INTO financial_item_mappings (che_do, mau_bieu, ma, ma_noi_bo, ten_tren_bao_cao, so_lan_gap, so_bien_the, che_do_suy_doan, ghi_chu) VALUES ('TT200', 'B03-GT', '10', 'LCTT_TANG_GIAM_HTK', '- Tăng/giảm hàng tồn kho', 74, 1, 0, NULL);
INSERT INTO financial_item_mappings (che_do, mau_bieu, ma, ma_noi_bo, ten_tren_bao_cao, so_lan_gap, so_bien_the, che_do_suy_doan, ghi_chu) VALUES ('TT200', 'B03-GT', '11', 'LCTT_TANG_GIAM_PHAI_TRA', '- Tăng/giảm các khoản phải trả (không kể lãi vay phải trả, thuế TNDN phải nộp)', 74, 2, 0, NULL);
INSERT INTO financial_item_mappings (che_do, mau_bieu, ma, ma_noi_bo, ten_tren_bao_cao, so_lan_gap, so_bien_the, che_do_suy_doan, ghi_chu) VALUES ('TT200', 'B03-GT', '12', 'LCTT_TANG_GIAM_CHI_PHI_CHO_PHAN_BO', '- Tăng/giảm chi phí trả trước', 74, 1, 0, NULL);
INSERT INTO financial_item_mappings (che_do, mau_bieu, ma, ma_noi_bo, ten_tren_bao_cao, so_lan_gap, so_bien_the, che_do_suy_doan, ghi_chu) VALUES ('TT200', 'B03-GT', '17', 'LCTT_TIEN_CHI_KHAC_HDKD', '- Tiền chi khác cho hoạt động kinh doanh', 74, 1, 0, NULL);
INSERT INTO financial_item_mappings (che_do, mau_bieu, ma, ma_noi_bo, ten_tren_bao_cao, so_lan_gap, so_bien_the, che_do_suy_doan, ghi_chu) VALUES ('TT200', 'B03-GT', '20', 'LCTT_THUAN_HDKD', 'Lưu chuyển tiền thuần từ hoạt động kinh doanh', 74, 1, 0, NULL);
INSERT INTO financial_item_mappings (che_do, mau_bieu, ma, ma_noi_bo, ten_tren_bao_cao, so_lan_gap, so_bien_the, che_do_suy_doan, ghi_chu) VALUES ('TT200', 'B03-GT', '27', 'LCTT_THU_LAI_CO_TUC', 'Tiền thu lãi cho vay, cổ tức và lợi nhuận được chia', 74, 1, 0, NULL);
INSERT INTO financial_item_mappings (che_do, mau_bieu, ma, ma_noi_bo, ten_tren_bao_cao, so_lan_gap, so_bien_the, che_do_suy_doan, ghi_chu) VALUES ('TT200', 'B03-GT', '30', 'LCTT_THUAN_HDDT', 'Lưu chuyển tiền thuần từ hoạt động đầu tư', 74, 1, 0, NULL);

-- ----- TT200 / B02 -----
INSERT INTO financial_item_mappings (che_do, mau_bieu, ma, ma_noi_bo, ten_tren_bao_cao, so_lan_gap, so_bien_the, che_do_suy_doan, ghi_chu) VALUES ('TT200', 'B02', '10', 'DOANH_THU_THUAN', 'Doanh thu thuần bán hàng và cung cấp dịch vụ', 73, 2, 0, NULL);
INSERT INTO financial_item_mappings (che_do, mau_bieu, ma, ma_noi_bo, ten_tren_bao_cao, so_lan_gap, so_bien_the, che_do_suy_doan, ghi_chu) VALUES ('TT200', 'B02', '21', 'DOANH_THU_TAI_CHINH', 'Doanh thu hoạt động tài chính', 73, 1, 0, NULL);
INSERT INTO financial_item_mappings (che_do, mau_bieu, ma, ma_noi_bo, ten_tren_bao_cao, so_lan_gap, so_bien_the, che_do_suy_doan, ghi_chu) VALUES ('TT200', 'B02', '22', 'CHI_PHI_TAI_CHINH', 'Chi phí tài chính', 73, 1, 0, NULL);
INSERT INTO financial_item_mappings (che_do, mau_bieu, ma, ma_noi_bo, ten_tren_bao_cao, so_lan_gap, so_bien_the, che_do_suy_doan, ghi_chu) VALUES ('TT200', 'B02', '26', 'CHI_PHI_QUAN_LY', 'Chi phí quản lý doanh nghiệp', 73, 1, 0, NULL);
INSERT INTO financial_item_mappings (che_do, mau_bieu, ma, ma_noi_bo, ten_tren_bao_cao, so_lan_gap, so_bien_the, che_do_suy_doan, ghi_chu) VALUES ('TT200', 'B02', '30', 'LOI_NHUAN_THUAN_HDKD', 'Lợi nhuận thuần từ hoạt động kinh doanh', 73, 1, 0, NULL);
INSERT INTO financial_item_mappings (che_do, mau_bieu, ma, ma_noi_bo, ten_tren_bao_cao, so_lan_gap, so_bien_the, che_do_suy_doan, ghi_chu) VALUES ('TT200', 'B02', '31', 'THU_NHAP_KHAC', 'Thu nhập khác', 73, 1, 0, NULL);
INSERT INTO financial_item_mappings (che_do, mau_bieu, ma, ma_noi_bo, ten_tren_bao_cao, so_lan_gap, so_bien_the, che_do_suy_doan, ghi_chu) VALUES ('TT200', 'B02', '32', 'CHI_PHI_KHAC', 'Chi phí khác', 73, 1, 0, NULL);
INSERT INTO financial_item_mappings (che_do, mau_bieu, ma, ma_noi_bo, ten_tren_bao_cao, so_lan_gap, so_bien_the, che_do_suy_doan, ghi_chu) VALUES ('TT200', 'B02', '40', 'LOI_NHUAN_KHAC', 'Lợi nhuận khác', 73, 1, 0, NULL);

-- ----- TT200 / B03-GT -----
INSERT INTO financial_item_mappings (che_do, mau_bieu, ma, ma_noi_bo, ten_tren_bao_cao, so_lan_gap, so_bien_the, che_do_suy_doan, ghi_chu) VALUES ('TT200', 'B03-GT', '15', 'LCTT_THUE_TNDN_DA_NOP', '- Thuế thu nhập doanh nghiệp đã nộp', 73, 1, 0, NULL);

-- ----- TT200 / B01 -----
INSERT INTO financial_item_mappings (che_do, mau_bieu, ma, ma_noi_bo, ten_tren_bao_cao, so_lan_gap, so_bien_the, che_do_suy_doan, ghi_chu) VALUES ('TT200', 'B01', '110', 'TIEN_VA_TUONG_DUONG_TIEN', 'Tiền và các khoản tương đương tiền', 72, 1, 0, NULL);
INSERT INTO financial_item_mappings (che_do, mau_bieu, ma, ma_noi_bo, ten_tren_bao_cao, so_lan_gap, so_bien_the, che_do_suy_doan, ghi_chu) VALUES ('TT200', 'B01', '111', 'TIEN_M111', 'Tiền', 72, 1, 0, NULL);
INSERT INTO financial_item_mappings (che_do, mau_bieu, ma, ma_noi_bo, ten_tren_bao_cao, so_lan_gap, so_bien_the, che_do_suy_doan, ghi_chu) VALUES ('TT200', 'B01', '112', 'TUONG_DUONG_TIEN', 'Các khoản tương đương tiền', 72, 1, 0, NULL);
INSERT INTO financial_item_mappings (che_do, mau_bieu, ma, ma_noi_bo, ten_tren_bao_cao, so_lan_gap, so_bien_the, che_do_suy_doan, ghi_chu) VALUES ('TT200', 'B01', '130', 'PHAI_THU_NGAN_HAN', 'Các khoản phải thu ngắn hạn', 72, 1, 0, NULL);
INSERT INTO financial_item_mappings (che_do, mau_bieu, ma, ma_noi_bo, ten_tren_bao_cao, so_lan_gap, so_bien_the, che_do_suy_doan, ghi_chu) VALUES ('TT200', 'B01', '131', 'PHAI_THU_KHACH_HANG_NH', 'Phải thu ngắn hạn của khách hàng', 72, 1, 0, NULL);
INSERT INTO financial_item_mappings (che_do, mau_bieu, ma, ma_noi_bo, ten_tren_bao_cao, so_lan_gap, so_bien_the, che_do_suy_doan, ghi_chu) VALUES ('TT200', 'B01', '132', 'TRA_TRUOC_NGUOI_BAN_NH', 'Trả trước cho người bán ngắn hạn', 72, 1, 0, NULL);
INSERT INTO financial_item_mappings (che_do, mau_bieu, ma, ma_noi_bo, ten_tren_bao_cao, so_lan_gap, so_bien_the, che_do_suy_doan, ghi_chu) VALUES ('TT200', 'B01', '136', 'PHAI_THU_NGAN_HAN_KHAC', 'Phải thu ngắn hạn khác', 72, 1, 0, NULL);
INSERT INTO financial_item_mappings (che_do, mau_bieu, ma, ma_noi_bo, ten_tren_bao_cao, so_lan_gap, so_bien_the, che_do_suy_doan, ghi_chu) VALUES ('TT200', 'B01', '137', 'DU_PHONG_PHAI_THU_KHO_DOI', 'Dự phòng phải thu ngắn hạn khó đòi', 72, 2, 0, NULL);
INSERT INTO financial_item_mappings (che_do, mau_bieu, ma, ma_noi_bo, ten_tren_bao_cao, so_lan_gap, so_bien_the, che_do_suy_doan, ghi_chu) VALUES ('TT200', 'B01', '140', 'HANG_TON_KHO_M140', 'Hàng tồn kho', 72, 1, 0, NULL);
INSERT INTO financial_item_mappings (che_do, mau_bieu, ma, ma_noi_bo, ten_tren_bao_cao, so_lan_gap, so_bien_the, che_do_suy_doan, ghi_chu) VALUES ('TT200', 'B01', '141', 'HANG_TON_KHO_M141', 'Hàng tồn kho', 72, 1, 0, NULL);
INSERT INTO financial_item_mappings (che_do, mau_bieu, ma, ma_noi_bo, ten_tren_bao_cao, so_lan_gap, so_bien_the, che_do_suy_doan, ghi_chu) VALUES ('TT200', 'B01', '149', 'DU_PHONG_GIAM_GIA_HTK', 'Dự phòng giảm giá hàng tồn kho', 72, 2, 0, NULL);
INSERT INTO financial_item_mappings (che_do, mau_bieu, ma, ma_noi_bo, ten_tren_bao_cao, so_lan_gap, so_bien_the, che_do_suy_doan, ghi_chu) VALUES ('TT200', 'B01', '150', 'TAI_SAN_NGAN_HAN_KHAC', 'Tài sản ngắn hạn khác', 72, 1, 0, NULL);
INSERT INTO financial_item_mappings (che_do, mau_bieu, ma, ma_noi_bo, ten_tren_bao_cao, so_lan_gap, so_bien_the, che_do_suy_doan, ghi_chu) VALUES ('TT200', 'B01', '151', 'CHI_PHI_CHO_PHAN_BO_NGAN_HAN', 'Chi phí trả trước ngắn hạn', 72, 1, 0, NULL);
INSERT INTO financial_item_mappings (che_do, mau_bieu, ma, ma_noi_bo, ten_tren_bao_cao, so_lan_gap, so_bien_the, che_do_suy_doan, ghi_chu) VALUES ('TT200', 'B01', '220', 'TAI_SAN_CO_DINH', 'Tài sản cố định', 72, 1, 0, NULL);
INSERT INTO financial_item_mappings (che_do, mau_bieu, ma, ma_noi_bo, ten_tren_bao_cao, so_lan_gap, so_bien_the, che_do_suy_doan, ghi_chu) VALUES ('TT200', 'B01', '221', 'TSCD_HUU_HINH', 'Tài sản cố định hữu hình', 72, 1, 0, NULL);
INSERT INTO financial_item_mappings (che_do, mau_bieu, ma, ma_noi_bo, ten_tren_bao_cao, so_lan_gap, so_bien_the, che_do_suy_doan, ghi_chu) VALUES ('TT200', 'B01', '230', 'BDS_DAU_TU', 'Bất động sản đầu tư', 72, 1, 0, NULL);
INSERT INTO financial_item_mappings (che_do, mau_bieu, ma, ma_noi_bo, ten_tren_bao_cao, so_lan_gap, so_bien_the, che_do_suy_doan, ghi_chu) VALUES ('TT200', 'B01', '260', 'TAI_SAN_DAI_HAN_KHAC', 'Tài sản dài hạn khác', 72, 1, 0, NULL);
INSERT INTO financial_item_mappings (che_do, mau_bieu, ma, ma_noi_bo, ten_tren_bao_cao, so_lan_gap, so_bien_the, che_do_suy_doan, ghi_chu) VALUES ('TT200', 'B01', '261', 'CHI_PHI_CHO_PHAN_BO_DAI_HAN', 'Chi phí trả trước dài hạn', 72, 1, 0, NULL);

-- ----- TT200 / B03-GT -----
INSERT INTO financial_item_mappings (che_do, mau_bieu, ma, ma_noi_bo, ten_tren_bao_cao, so_lan_gap, so_bien_the, che_do_suy_doan, ghi_chu) VALUES ('TT200', 'B03-GT', '34', 'LCTT_TRA_NO_GOC_VAY', 'Tiền trả nợ gốc vay', 72, 1, 0, NULL);
INSERT INTO financial_item_mappings (che_do, mau_bieu, ma, ma_noi_bo, ten_tren_bao_cao, so_lan_gap, so_bien_the, che_do_suy_doan, ghi_chu) VALUES ('TT200', 'B03-GT', '36', 'LCTT_CO_TUC_DA_TRA', 'Cổ tức, lợi nhuận đã trả cho chủ sở hữu', 71, 2, 0, NULL);

-- ----- TT200 / B01 -----
INSERT INTO financial_item_mappings (che_do, mau_bieu, ma, ma_noi_bo, ten_tren_bao_cao, so_lan_gap, so_bien_the, che_do_suy_doan, ghi_chu) VALUES ('TT200', 'B01', '227', 'TSCD_VO_HINH', 'Tài sản cố định vô hình', 69, 1, 0, NULL);

-- ----- TT200 / B03-GT -----
INSERT INTO financial_item_mappings (che_do, mau_bieu, ma, ma_noi_bo, ten_tren_bao_cao, so_lan_gap, so_bien_the, che_do_suy_doan, ghi_chu) VALUES ('TT200', 'B03-GT', '01', 'LCTT_LOI_NHUAN_TRUOC_THUE', 'Lợi nhuận trước thuế', 69, 3, 0, NULL);

-- ----- TT200 / B01 -----
INSERT INTO financial_item_mappings (che_do, mau_bieu, ma, ma_noi_bo, ten_tren_bao_cao, so_lan_gap, so_bien_the, che_do_suy_doan, ghi_chu) VALUES ('TT200', 'B01', '231', 'BDS_DAU_TU_NGUYEN_GIA', '- Nguyên giá', 68, 1, 0, NULL);
INSERT INTO financial_item_mappings (che_do, mau_bieu, ma, ma_noi_bo, ten_tren_bao_cao, so_lan_gap, so_bien_the, che_do_suy_doan, ghi_chu) VALUES ('TT200', 'B01', '232', 'BDS_DAU_TU_HAO_MON', '- Giá trị hao mòn lũy kế', 68, 2, 0, NULL);

-- ----- TT200 / B02 -----
INSERT INTO financial_item_mappings (che_do, mau_bieu, ma, ma_noi_bo, ten_tren_bao_cao, so_lan_gap, so_bien_the, che_do_suy_doan, ghi_chu) VALUES ('TT200', 'B02', '23', 'CHI_PHI_DI_VAY', '- Trong đó: Chi phí lãi vay', 68, 1, 0, NULL);

-- ----- TT200 / B03-GT -----
INSERT INTO financial_item_mappings (che_do, mau_bieu, ma, ma_noi_bo, ten_tren_bao_cao, so_lan_gap, so_bien_the, che_do_suy_doan, ghi_chu) VALUES ('TT200', 'B03-GT', '50', 'LCTT_THUAN_TRONG_KY', 'Lưu chuyển tiền thuần trong kỳ', 68, 2, 0, NULL);

-- ----- TT200 / B01 -----
INSERT INTO financial_item_mappings (che_do, mau_bieu, ma, ma_noi_bo, ten_tren_bao_cao, so_lan_gap, so_bien_the, che_do_suy_doan, ghi_chu) VALUES ('TT200', 'B01', '240', 'TAI_SAN_DO_DANG_DH', 'Tài sản dở dang dài hạn', 66, 1, 0, NULL);
INSERT INTO financial_item_mappings (che_do, mau_bieu, ma, ma_noi_bo, ten_tren_bao_cao, so_lan_gap, so_bien_the, che_do_suy_doan, ghi_chu) VALUES ('TT200', 'B01', '242', 'CHI_PHI_XDCB_DO_DANG', 'Chi phí xây dựng cơ bản dở dang', 66, 1, 0, NULL);

-- ----- TT200 / B03-GT -----
INSERT INTO financial_item_mappings (che_do, mau_bieu, ma, ma_noi_bo, ten_tren_bao_cao, so_lan_gap, so_bien_the, che_do_suy_doan, ghi_chu) VALUES ('TT200', 'B03-GT', '02', 'LCTT_KHAU_HAO', '- Khấu hao tài sản cố định và bất động sản đầu tư', 66, 2, 0, NULL);
INSERT INTO financial_item_mappings (che_do, mau_bieu, ma, ma_noi_bo, ten_tren_bao_cao, so_lan_gap, so_bien_the, che_do_suy_doan, ghi_chu) VALUES ('TT200', 'B03-GT', '06', 'LCTT_CHI_PHI_DI_VAY', '- Chi phí lãi vay', 66, 1, 0, NULL);
INSERT INTO financial_item_mappings (che_do, mau_bieu, ma, ma_noi_bo, ten_tren_bao_cao, so_lan_gap, so_bien_the, che_do_suy_doan, ghi_chu) VALUES ('TT200', 'B03-GT', '14', 'LCTT_CHI_PHI_DI_VAY_DA_TRA', '- Tiền lãi vay đã trả', 66, 1, 0, NULL);
INSERT INTO financial_item_mappings (che_do, mau_bieu, ma, ma_noi_bo, ten_tren_bao_cao, so_lan_gap, so_bien_the, che_do_suy_doan, ghi_chu) VALUES ('TT200', 'B03-GT', '33', 'LCTT_THU_DI_VAY', 'Tiền thu từ đi vay', 66, 4, 0, NULL);

-- ----- TT200 / B01 -----
INSERT INTO financial_item_mappings (che_do, mau_bieu, ma, ma_noi_bo, ten_tren_bao_cao, so_lan_gap, so_bien_the, che_do_suy_doan, ghi_chu) VALUES ('TT200', 'B01', '321', 'DU_PHONG_PHAI_TRA_NH', 'Dự phòng phải trả ngắn hạn', 62, 1, 0, NULL);
INSERT INTO financial_item_mappings (che_do, mau_bieu, ma, ma_noi_bo, ten_tren_bao_cao, so_lan_gap, so_bien_the, che_do_suy_doan, ghi_chu) VALUES ('TT200', 'B01', '330', 'NO_DAI_HAN', 'Nợ dài hạn', 61, 1, 0, NULL);
INSERT INTO financial_item_mappings (che_do, mau_bieu, ma, ma_noi_bo, ten_tren_bao_cao, so_lan_gap, so_bien_the, che_do_suy_doan, ghi_chu) VALUES ('TT200', 'B01', '338', 'VAY_NO_THUE_TC_DH', 'Vay và nợ thuê tài chính dài hạn', 61, 1, 0, NULL);

-- ----- TT200 / B02 -----
INSERT INTO financial_item_mappings (che_do, mau_bieu, ma, ma_noi_bo, ten_tren_bao_cao, so_lan_gap, so_bien_the, che_do_suy_doan, ghi_chu) VALUES ('TT200', 'B02', '02', 'CAC_KHOAN_GIAM_TRU_DT', 'Các khoản giảm trừ doanh thu', 60, 1, 0, NULL);

-- ----- TT200 / B01 -----
INSERT INTO financial_item_mappings (che_do, mau_bieu, ma, ma_noi_bo, ten_tren_bao_cao, so_lan_gap, so_bien_the, che_do_suy_doan, ghi_chu) VALUES ('TT200', 'B01', '412', 'THANG_DU_VON', 'Thặng dư vốn cổ phần', 59, 1, 0, NULL);

-- ----- TT200 / B02 -----
INSERT INTO financial_item_mappings (che_do, mau_bieu, ma, ma_noi_bo, ten_tren_bao_cao, so_lan_gap, so_bien_the, che_do_suy_doan, ghi_chu) VALUES ('TT200', 'B02', '25', 'CHI_PHI_BAN_HANG', 'Chi phí bán hàng', 59, 1, 0, NULL);

-- ----- TT200 / B01 -----
INSERT INTO financial_item_mappings (che_do, mau_bieu, ma, ma_noi_bo, ten_tren_bao_cao, so_lan_gap, so_bien_the, che_do_suy_doan, ghi_chu) VALUES ('TT200', 'B01', '223', 'TSCD_HUU_HINH_HAO_MON', '- Giá trị hao mòn lũy kế', 58, 3, 0, NULL);

-- ----- TT200 / B03-GT -----
INSERT INTO financial_item_mappings (che_do, mau_bieu, ma, ma_noi_bo, ten_tren_bao_cao, so_lan_gap, so_bien_the, che_do_suy_doan, ghi_chu) VALUES ('TT200', 'B03-GT', '60', 'TIEN_DAU_KY', 'Tiền và tương đương tiền đầu kỳ', 58, 2, 0, NULL);
INSERT INTO financial_item_mappings (che_do, mau_bieu, ma, ma_noi_bo, ten_tren_bao_cao, so_lan_gap, so_bien_the, che_do_suy_doan, ghi_chu) VALUES ('TT200', 'B03-GT', '61', 'LCTT_ANH_HUONG_TY_GIA', 'Ảnh hưởng của thay đổi tỷ giá hối đoái quy đổi ngoại tệ', 58, 1, 0, NULL);
INSERT INTO financial_item_mappings (che_do, mau_bieu, ma, ma_noi_bo, ten_tren_bao_cao, so_lan_gap, so_bien_the, che_do_suy_doan, ghi_chu) VALUES ('TT200', 'B03-GT', '70', 'TIEN_CUOI_KY', 'Tiền và tương đương tiền cuối kỳ', 58, 2, 0, NULL);

-- ----- TT200 / B01 -----
INSERT INTO financial_item_mappings (che_do, mau_bieu, ma, ma_noi_bo, ten_tren_bao_cao, so_lan_gap, so_bien_the, che_do_suy_doan, ghi_chu) VALUES ('TT200', 'B01', '320', 'VAY_NO_THUE_TC_NH', 'Vay và nợ thuê tài chính ngắn hạn', 57, 1, 0, NULL);
INSERT INTO financial_item_mappings (che_do, mau_bieu, ma, ma_noi_bo, ten_tren_bao_cao, so_lan_gap, so_bien_the, che_do_suy_doan, ghi_chu) VALUES ('TT200', 'B01', '229', 'TSCD_VO_HINH_HAO_MON', '- Giá trị hao mòn lũy kế', 56, 2, 0, NULL);
INSERT INTO financial_item_mappings (che_do, mau_bieu, ma, ma_noi_bo, ten_tren_bao_cao, so_lan_gap, so_bien_the, che_do_suy_doan, ghi_chu) VALUES ('TT200', 'B01', '228', 'TSCD_VO_HINH_NGUYEN_GIA', '- Nguyên giá', 55, 1, 0, NULL);

-- ----- TT200 / B03-GT -----
INSERT INTO financial_item_mappings (che_do, mau_bieu, ma, ma_noi_bo, ten_tren_bao_cao, so_lan_gap, so_bien_the, che_do_suy_doan, ghi_chu) VALUES ('TT200', 'B03-GT', '21', 'LCTT_CHI_MUA_TSCD', 'Tiền chi để mua sắm, xây dựng TSCĐ và các tài sản dài hạn khác', 53, 2, 0, NULL);

-- ----- TT200 / B01 -----
INSERT INTO financial_item_mappings (che_do, mau_bieu, ma, ma_noi_bo, ten_tren_bao_cao, so_lan_gap, so_bien_the, che_do_suy_doan, ghi_chu) VALUES ('TT200', 'B01', '222', 'TSCD_HUU_HINH_NGUYEN_GIA', '- Nguyên giá', 51, 1, 0, NULL);
INSERT INTO financial_item_mappings (che_do, mau_bieu, ma, ma_noi_bo, ten_tren_bao_cao, so_lan_gap, so_bien_the, che_do_suy_doan, ghi_chu) VALUES ('TT200', 'B01', '418', 'QUY_DAU_TU_PHAT_TRIEN', 'Quỹ đầu tư phát triển', 51, 1, 0, NULL);

-- ----- TT200 / B03-GT -----
INSERT INTO financial_item_mappings (che_do, mau_bieu, ma, ma_noi_bo, ten_tren_bao_cao, so_lan_gap, so_bien_the, che_do_suy_doan, ghi_chu) VALUES ('TT200', 'B03-GT', '23', 'LCTT_CHI_CHO_VAY', 'Tiền chi cho vay, mua các công cụ nợ của đơn vị khác', 49, 1, 0, NULL);

-- ----- TT200 / B01 -----
INSERT INTO financial_item_mappings (che_do, mau_bieu, ma, ma_noi_bo, ten_tren_bao_cao, so_lan_gap, so_bien_the, che_do_suy_doan, ghi_chu) VALUES ('TT200', 'B01', '120', 'DAU_TU_TC_NGAN_HAN', 'Các khoản đầu tư tài chính ngắn hạn', 48, 1, 0, NULL);
INSERT INTO financial_item_mappings (che_do, mau_bieu, ma, ma_noi_bo, ten_tren_bao_cao, so_lan_gap, so_bien_the, che_do_suy_doan, ghi_chu) VALUES ('TT200', 'B01', '123', 'DAU_TU_NAM_GIU_DAO_HAN_NH', 'Đầu tư nắm giữ đến ngày đáo hạn', 48, 1, 0, NULL);

-- ----- TT200 / B03-GT -----
INSERT INTO financial_item_mappings (che_do, mau_bieu, ma, ma_noi_bo, ten_tren_bao_cao, so_lan_gap, so_bien_the, che_do_suy_doan, ghi_chu) VALUES ('TT200', 'B03-GT', '24', 'LCTT_THU_HOI_CHO_VAY', 'Tiền thu hồi cho vay, bán lại các công cụ nợ của đơn vị khác', 46, 2, 0, NULL);

-- ----- TT200 / B02 -----
INSERT INTO financial_item_mappings (che_do, mau_bieu, ma, ma_noi_bo, ten_tren_bao_cao, so_lan_gap, so_bien_the, che_do_suy_doan, ghi_chu) VALUES ('TT200', 'B02', '50', 'LOI_NHUAN_TRUOC_THUE', 'Tổng lợi nhuận kế toán trước thuế', 45, 1, 0, NULL);

-- ----- TT200 / B01 -----
INSERT INTO financial_item_mappings (che_do, mau_bieu, ma, ma_noi_bo, ten_tren_bao_cao, so_lan_gap, so_bien_the, che_do_suy_doan, ghi_chu) VALUES ('TT200', 'B01', '417', 'CHENH_LECH_TY_GIA', 'Chênh lệch tỷ giá hối đoái', 39, 1, 0, NULL);
INSERT INTO financial_item_mappings (che_do, mau_bieu, ma, ma_noi_bo, ten_tren_bao_cao, so_lan_gap, so_bien_the, che_do_suy_doan, ghi_chu) VALUES ('TT200', 'B01', '250', 'DAU_TU_TC_DAI_HAN', 'Đầu tư tài chính dài hạn', 36, 1, 0, NULL);
INSERT INTO financial_item_mappings (che_do, mau_bieu, ma, ma_noi_bo, ten_tren_bao_cao, so_lan_gap, so_bien_the, che_do_suy_doan, ghi_chu) VALUES ('TT200', 'B01', '251', 'DAU_TU_CONG_TY_CON', 'Đầu tư vào công ty con', 36, 1, 0, NULL);

-- ----- TT200 / B03-GT -----
INSERT INTO financial_item_mappings (che_do, mau_bieu, ma, ma_noi_bo, ten_tren_bao_cao, so_lan_gap, so_bien_the, che_do_suy_doan, ghi_chu) VALUES ('TT200', 'B03-GT', '22', 'LCTT_THU_THANH_LY_TSCD', 'Tiền thu từ thanh lý, nhượng bán TSCĐ và các tài sản dài hạn khác', 36, 5, 0, NULL);

-- ----- TT200 / B01 -----
INSERT INTO financial_item_mappings (che_do, mau_bieu, ma, ma_noi_bo, ten_tren_bao_cao, so_lan_gap, so_bien_the, che_do_suy_doan, ghi_chu) VALUES ('TT200', 'B01', '262', 'TAI_SAN_THUE_TNDN_HOAN_LAI', 'Tài sản thuê thu nhập hoãn lại', 33, 4, 0, NULL);

-- ----- TT200 / B02 -----
INSERT INTO financial_item_mappings (che_do, mau_bieu, ma, ma_noi_bo, ten_tren_bao_cao, so_lan_gap, so_bien_the, che_do_suy_doan, ghi_chu) VALUES ('TT200', 'B02', '51', 'CHI_PHI_THUE_TNDN_HIEN_HANH', 'Chi phí thuế TNDN hiện hành', 30, 1, 0, NULL);
INSERT INTO financial_item_mappings (che_do, mau_bieu, ma, ma_noi_bo, ten_tren_bao_cao, so_lan_gap, so_bien_the, che_do_suy_doan, ghi_chu) VALUES ('TT200', 'B02', '60', 'LOI_NHUAN_SAU_THUE', 'Lợi nhuận sau thuế TNDN', 29, 1, 0, NULL);

-- ----- TT200 / B01 -----
INSERT INTO financial_item_mappings (che_do, mau_bieu, ma, ma_noi_bo, ten_tren_bao_cao, so_lan_gap, so_bien_the, che_do_suy_doan, ghi_chu) VALUES ('TT200', 'B01', '153', 'THUE_PHAI_THU_NHA_NUOC', 'Thuế và các khoản khác phải thu Nhà nước', 28, 2, 0, NULL);

-- ----- TT200 / B02 -----
INSERT INTO financial_item_mappings (che_do, mau_bieu, ma, ma_noi_bo, ten_tren_bao_cao, so_lan_gap, so_bien_the, che_do_suy_doan, ghi_chu) VALUES ('TT200', 'B02', '52', 'CHI_PHI_THUE_TNDN_HOAN_LAI', 'Chi phí thuế TNDN hoãn lại', 28, 1, 0, NULL);

-- ----- TT200 / B01 -----
INSERT INTO financial_item_mappings (che_do, mau_bieu, ma, ma_noi_bo, ten_tren_bao_cao, so_lan_gap, so_bien_the, che_do_suy_doan, ghi_chu) VALUES ('TT200', 'B01', '429', 'LOI_ICH_CDKKS', 'Lợi ích cổ đông không kiểm soát', 27, 1, 0, NULL);
INSERT INTO financial_item_mappings (che_do, mau_bieu, ma, ma_noi_bo, ten_tren_bao_cao, so_lan_gap, so_bien_the, che_do_suy_doan, ghi_chu) VALUES ('TT200', 'B01', '337', 'PHAI_TRA_DAI_HAN_KHAC', 'Phải trả dài hạn khác', 26, 1, 0, NULL);
INSERT INTO financial_item_mappings (che_do, mau_bieu, ma, ma_noi_bo, ten_tren_bao_cao, so_lan_gap, so_bien_the, che_do_suy_doan, ghi_chu) VALUES ('TT200', 'B01', '152', 'THUE_GTGT_DUOC_KHAU_TRU', 'Thuế GTGT được khấu trừ', 20, 1, 0, NULL);

-- ----- TT200 / B02 -----
INSERT INTO financial_item_mappings (che_do, mau_bieu, ma, ma_noi_bo, ten_tren_bao_cao, so_lan_gap, so_bien_the, che_do_suy_doan, ghi_chu) VALUES ('TT200', 'B02', '24', 'LAI_LO_LIEN_DOANH_LIEN_KET', 'Phân lãi hoặc lỗ trong công ty liên doanh, liên kết', 15, 2, 0, NULL);

-- ----- TT200 / B03-GT -----
INSERT INTO financial_item_mappings (che_do, mau_bieu, ma, ma_noi_bo, ten_tren_bao_cao, so_lan_gap, so_bien_the, che_do_suy_doan, ghi_chu) VALUES ('TT200', 'B03-GT', '26', 'LCTT_THU_HOI_GOP_VON', 'Tiền thu hồi đầu tư góp vốn vào đơn vị khác', 15, 2, 0, NULL);

-- ----- TT200 / B02 -----
INSERT INTO financial_item_mappings (che_do, mau_bieu, ma, ma_noi_bo, ten_tren_bao_cao, so_lan_gap, so_bien_the, che_do_suy_doan, ghi_chu) VALUES ('TT200', 'B02', '61', 'LNST_CONG_TY_ME', 'Lợi nhuận sau thuế của Công ty mẹ', 13, 1, 0, NULL);
INSERT INTO financial_item_mappings (che_do, mau_bieu, ma, ma_noi_bo, ten_tren_bao_cao, so_lan_gap, so_bien_the, che_do_suy_doan, ghi_chu) VALUES ('TT200', 'B02', '62', 'LNST_CDKKS', 'Lợi nhuận sau thuế của cổ đông không kiểm soát', 13, 1, 0, NULL);

-- ----- TT200 / B03-GT -----
INSERT INTO financial_item_mappings (che_do, mau_bieu, ma, ma_noi_bo, ten_tren_bao_cao, so_lan_gap, so_bien_the, che_do_suy_doan, ghi_chu) VALUES ('TT200', 'B03-GT', '25', 'LCTT_CHI_GOP_VON', 'Tiền chi đầu tư góp vốn vào đơn vị khác', 8, 1, 0, NULL);

-- ----- TT99 / B01 -----
INSERT INTO financial_item_mappings (che_do, mau_bieu, ma, ma_noi_bo, ten_tren_bao_cao, so_lan_gap, so_bien_the, che_do_suy_doan, ghi_chu) VALUES ('TT99', 'B01', '112', 'TUONG_DUONG_TIEN', 'Các khoản tương đương tiền', 7, 1, 0, NULL);
INSERT INTO financial_item_mappings (che_do, mau_bieu, ma, ma_noi_bo, ten_tren_bao_cao, so_lan_gap, so_bien_the, che_do_suy_doan, ghi_chu) VALUES ('TT99', 'B01', '123', 'DAU_TU_NAM_GIU_DAO_HAN_NH', 'Đầu tư nắm giữ đến ngày đáo hạn', 7, 2, 0, NULL);

-- ----- TT200 / B03-GT -----
INSERT INTO financial_item_mappings (che_do, mau_bieu, ma, ma_noi_bo, ten_tren_bao_cao, so_lan_gap, so_bien_the, che_do_suy_doan, ghi_chu) VALUES ('TT200', 'B03-GT', '31', 'LCTT_THU_PHAT_HANH_CP', 'Tiền thu từ phát hành cổ phiếu, nhận vốn góp của chủ sở hữu', 6, 2, 0, NULL);

-- ----- TT99 / B01 -----
INSERT INTO financial_item_mappings (che_do, mau_bieu, ma, ma_noi_bo, ten_tren_bao_cao, so_lan_gap, so_bien_the, che_do_suy_doan, ghi_chu) VALUES ('TT99', 'B01', '110', 'TIEN_VA_TUONG_DUONG_TIEN', 'Tiền và các khoản tương đương tiền', 6, 1, 0, NULL);
INSERT INTO financial_item_mappings (che_do, mau_bieu, ma, ma_noi_bo, ten_tren_bao_cao, so_lan_gap, so_bien_the, che_do_suy_doan, ghi_chu) VALUES ('TT99', 'B01', '120', 'DAU_TU_TC_NGAN_HAN', 'Đầu tư tài chính ngắn hạn', 6, 1, 0, NULL);
INSERT INTO financial_item_mappings (che_do, mau_bieu, ma, ma_noi_bo, ten_tren_bao_cao, so_lan_gap, so_bien_the, che_do_suy_doan, ghi_chu) VALUES ('TT99', 'B01', '130', 'PHAI_THU_NGAN_HAN', 'Các khoản phải thu ngắn hạn', 6, 1, 0, NULL);
INSERT INTO financial_item_mappings (che_do, mau_bieu, ma, ma_noi_bo, ten_tren_bao_cao, so_lan_gap, so_bien_the, che_do_suy_doan, ghi_chu) VALUES ('TT99', 'B01', '135', 'PHAI_THU_NGAN_HAN_KHAC', 'Phải thu ngắn hạn khác', 6, 1, 0, NULL);
INSERT INTO financial_item_mappings (che_do, mau_bieu, ma, ma_noi_bo, ten_tren_bao_cao, so_lan_gap, so_bien_the, che_do_suy_doan, ghi_chu) VALUES ('TT99', 'B01', '136', 'DU_PHONG_PHAI_THU_KHO_DOI', 'Dự phòng phải thu ngắn hạn khó đòi', 6, 2, 0, NULL);
INSERT INTO financial_item_mappings (che_do, mau_bieu, ma, ma_noi_bo, ten_tren_bao_cao, so_lan_gap, so_bien_the, che_do_suy_doan, ghi_chu) VALUES ('TT99', 'B01', '313', 'PHAI_TRA_CO_TUC_LOI_NHUAN', 'Phải trả cổ tức, lợi nhuận', 6, 1, 0, NULL);
INSERT INTO financial_item_mappings (che_do, mau_bieu, ma, ma_noi_bo, ten_tren_bao_cao, so_lan_gap, so_bien_the, che_do_suy_doan, ghi_chu) VALUES ('TT99', 'B01', '320', 'PHAI_TRA_NGAN_HAN_KHAC', 'Phải trả ngắn hạn khác', 6, 1, 0, NULL);

-- ----- TT99 / B02 -----
INSERT INTO financial_item_mappings (che_do, mau_bieu, ma, ma_noi_bo, ten_tren_bao_cao, so_lan_gap, so_bien_the, che_do_suy_doan, ghi_chu) VALUES ('TT99', 'B02', '01', 'DOANH_THU_BAN_HANG', 'Doanh thu bán hàng và cung cấp dịch vụ', 6, 1, 0, NULL);
INSERT INTO financial_item_mappings (che_do, mau_bieu, ma, ma_noi_bo, ten_tren_bao_cao, so_lan_gap, so_bien_the, che_do_suy_doan, ghi_chu) VALUES ('TT99', 'B02', '02', 'CAC_KHOAN_GIAM_TRU_DT', 'Các khoản giảm trừ doanh thu', 6, 1, 0, NULL);
INSERT INTO financial_item_mappings (che_do, mau_bieu, ma, ma_noi_bo, ten_tren_bao_cao, so_lan_gap, so_bien_the, che_do_suy_doan, ghi_chu) VALUES ('TT99', 'B02', '10', 'DOANH_THU_THUAN', 'Doanh thu thuần bán hàng và cung cấp dịch vụ', 6, 2, 0, NULL);
INSERT INTO financial_item_mappings (che_do, mau_bieu, ma, ma_noi_bo, ten_tren_bao_cao, so_lan_gap, so_bien_the, che_do_suy_doan, ghi_chu) VALUES ('TT99', 'B02', '11', 'GIA_VON_HANG_BAN', 'Giá vốn hàng bán', 6, 1, 0, NULL);
INSERT INTO financial_item_mappings (che_do, mau_bieu, ma, ma_noi_bo, ten_tren_bao_cao, so_lan_gap, so_bien_the, che_do_suy_doan, ghi_chu) VALUES ('TT99', 'B02', '20', 'LOI_NHUAN_GOP', 'Lợi nhuận gộp về bán hàng và cung cấp dịch vụ', 6, 2, 0, NULL);
INSERT INTO financial_item_mappings (che_do, mau_bieu, ma, ma_noi_bo, ten_tren_bao_cao, so_lan_gap, so_bien_the, che_do_suy_doan, ghi_chu) VALUES ('TT99', 'B02', '21', 'LAI_LO_BAN_THANH_LY_BDSDT', 'Lãi/lỗ của hoạt động bán, thanh lý bất động sản đầu tư', 6, 1, 0, NULL);
INSERT INTO financial_item_mappings (che_do, mau_bieu, ma, ma_noi_bo, ten_tren_bao_cao, so_lan_gap, so_bien_the, che_do_suy_doan, ghi_chu) VALUES ('TT99', 'B02', '22', 'DOANH_THU_TAI_CHINH', 'Doanh thu hoạt động tài chính', 6, 1, 0, NULL);
INSERT INTO financial_item_mappings (che_do, mau_bieu, ma, ma_noi_bo, ten_tren_bao_cao, so_lan_gap, so_bien_the, che_do_suy_doan, ghi_chu) VALUES ('TT99', 'B02', '23', 'CHI_PHI_TAI_CHINH', 'Chi phí tài chính', 6, 1, 0, NULL);
INSERT INTO financial_item_mappings (che_do, mau_bieu, ma, ma_noi_bo, ten_tren_bao_cao, so_lan_gap, so_bien_the, che_do_suy_doan, ghi_chu) VALUES ('TT99', 'B02', '24', 'CHI_PHI_DI_VAY', '- Trong đó: Chi phí đi vay', 6, 1, 0, NULL);
INSERT INTO financial_item_mappings (che_do, mau_bieu, ma, ma_noi_bo, ten_tren_bao_cao, so_lan_gap, so_bien_the, che_do_suy_doan, ghi_chu) VALUES ('TT99', 'B02', '25', 'CHI_PHI_BAN_HANG', 'Chi phí bán hàng', 6, 1, 0, NULL);
INSERT INTO financial_item_mappings (che_do, mau_bieu, ma, ma_noi_bo, ten_tren_bao_cao, so_lan_gap, so_bien_the, che_do_suy_doan, ghi_chu) VALUES ('TT99', 'B02', '26', 'CHI_PHI_QUAN_LY', 'Chi phí quản lý doanh nghiệp', 6, 1, 0, NULL);
INSERT INTO financial_item_mappings (che_do, mau_bieu, ma, ma_noi_bo, ten_tren_bao_cao, so_lan_gap, so_bien_the, che_do_suy_doan, ghi_chu) VALUES ('TT99', 'B02', '30', 'LOI_NHUAN_THUAN_HDKD', 'Lợi nhuận thuần từ hoạt động kinh doanh', 6, 1, 0, NULL);
INSERT INTO financial_item_mappings (che_do, mau_bieu, ma, ma_noi_bo, ten_tren_bao_cao, so_lan_gap, so_bien_the, che_do_suy_doan, ghi_chu) VALUES ('TT99', 'B02', '31', 'THU_NHAP_KHAC', 'Thu nhập khác', 6, 1, 0, NULL);
INSERT INTO financial_item_mappings (che_do, mau_bieu, ma, ma_noi_bo, ten_tren_bao_cao, so_lan_gap, so_bien_the, che_do_suy_doan, ghi_chu) VALUES ('TT99', 'B02', '32', 'CHI_PHI_KHAC', 'Chi phí khác', 6, 1, 0, NULL);
INSERT INTO financial_item_mappings (che_do, mau_bieu, ma, ma_noi_bo, ten_tren_bao_cao, so_lan_gap, so_bien_the, che_do_suy_doan, ghi_chu) VALUES ('TT99', 'B02', '40', 'LOI_NHUAN_KHAC', 'Lợi nhuận khác', 6, 1, 0, NULL);

-- ----- TT99 / B03-GT -----
INSERT INTO financial_item_mappings (che_do, mau_bieu, ma, ma_noi_bo, ten_tren_bao_cao, so_lan_gap, so_bien_the, che_do_suy_doan, ghi_chu) VALUES ('TT99', 'B03-GT', '01', 'LCTT_LOI_NHUAN_TRUOC_THUE', 'Lợi nhuận trước thuế', 6, 1, 0, NULL);
INSERT INTO financial_item_mappings (che_do, mau_bieu, ma, ma_noi_bo, ten_tren_bao_cao, so_lan_gap, so_bien_the, che_do_suy_doan, ghi_chu) VALUES ('TT99', 'B03-GT', '02', 'LCTT_KHAU_HAO', '- Khấu hao tài sản cố định và bất động sản đầu tư', 6, 2, 0, NULL);
INSERT INTO financial_item_mappings (che_do, mau_bieu, ma, ma_noi_bo, ten_tren_bao_cao, so_lan_gap, so_bien_the, che_do_suy_doan, ghi_chu) VALUES ('TT99', 'B03-GT', '03', 'LCTT_DU_PHONG', '- Các khoản dự phòng', 6, 1, 0, NULL);
INSERT INTO financial_item_mappings (che_do, mau_bieu, ma, ma_noi_bo, ten_tren_bao_cao, so_lan_gap, so_bien_the, che_do_suy_doan, ghi_chu) VALUES ('TT99', 'B03-GT', '04', 'LCTT_LAI_LO_TY_GIA', '- Lãi, lỗ chênh lệch tỷ giá hối đoái do đánh giá lại các khoản mục tiền tệ có gốc ngoại tệ', 6, 1, 0, NULL);
INSERT INTO financial_item_mappings (che_do, mau_bieu, ma, ma_noi_bo, ten_tren_bao_cao, so_lan_gap, so_bien_the, che_do_suy_doan, ghi_chu) VALUES ('TT99', 'B03-GT', '05', 'LCTT_LAI_LO_DAU_TU', '- Lãi/lỗ từ hoạt động đầu tư, tài chính', 6, 2, 0, NULL);
INSERT INTO financial_item_mappings (che_do, mau_bieu, ma, ma_noi_bo, ten_tren_bao_cao, so_lan_gap, so_bien_the, che_do_suy_doan, ghi_chu) VALUES ('TT99', 'B03-GT', '06', 'LCTT_CHI_PHI_DI_VAY', '- Chi phí đi vay', 6, 1, 0, NULL);
INSERT INTO financial_item_mappings (che_do, mau_bieu, ma, ma_noi_bo, ten_tren_bao_cao, so_lan_gap, so_bien_the, che_do_suy_doan, ghi_chu) VALUES ('TT99', 'B03-GT', '08', 'LCTT_LN_TRUOC_THAY_DOI_VLD', 'Lợi nhuận từ hoạt động kinh doanh trước thay đổi vốn lưu động', 6, 1, 0, NULL);
INSERT INTO financial_item_mappings (che_do, mau_bieu, ma, ma_noi_bo, ten_tren_bao_cao, so_lan_gap, so_bien_the, che_do_suy_doan, ghi_chu) VALUES ('TT99', 'B03-GT', '09', 'LCTT_TANG_GIAM_PHAI_THU', '- Tăng/giảm các khoản phải thu', 6, 2, 0, NULL);
INSERT INTO financial_item_mappings (che_do, mau_bieu, ma, ma_noi_bo, ten_tren_bao_cao, so_lan_gap, so_bien_the, che_do_suy_doan, ghi_chu) VALUES ('TT99', 'B03-GT', '10', 'LCTT_TANG_GIAM_HTK', '- Tăng/giảm hàng tồn kho', 6, 2, 0, NULL);
INSERT INTO financial_item_mappings (che_do, mau_bieu, ma, ma_noi_bo, ten_tren_bao_cao, so_lan_gap, so_bien_the, che_do_suy_doan, ghi_chu) VALUES ('TT99', 'B03-GT', '11', 'LCTT_TANG_GIAM_PHAI_TRA', '- Tăng/giảm các khoản phải trả (không kể lãi vay phải trả, thuế TNDN phải nộp)', 6, 3, 0, NULL);
INSERT INTO financial_item_mappings (che_do, mau_bieu, ma, ma_noi_bo, ten_tren_bao_cao, so_lan_gap, so_bien_the, che_do_suy_doan, ghi_chu) VALUES ('TT99', 'B03-GT', '12', 'LCTT_TANG_GIAM_CHI_PHI_CHO_PHAN_BO', '- Tăng/giảm chi phí chờ phân bổ', 6, 2, 0, NULL);
INSERT INTO financial_item_mappings (che_do, mau_bieu, ma, ma_noi_bo, ten_tren_bao_cao, so_lan_gap, so_bien_the, che_do_suy_doan, ghi_chu) VALUES ('TT99', 'B03-GT', '14', 'LCTT_CHI_PHI_DI_VAY_DA_TRA', '- Chi phí đi vay đã trả', 6, 1, 0, NULL);
INSERT INTO financial_item_mappings (che_do, mau_bieu, ma, ma_noi_bo, ten_tren_bao_cao, so_lan_gap, so_bien_the, che_do_suy_doan, ghi_chu) VALUES ('TT99', 'B03-GT', '15', 'LCTT_THUE_TNDN_DA_NOP', '- Thuế thu nhập doanh nghiệp đã nộp', 6, 1, 0, NULL);
INSERT INTO financial_item_mappings (che_do, mau_bieu, ma, ma_noi_bo, ten_tren_bao_cao, so_lan_gap, so_bien_the, che_do_suy_doan, ghi_chu) VALUES ('TT99', 'B03-GT', '17', 'LCTT_TIEN_CHI_KHAC_HDKD', '- Tiền chi khác cho hoạt động kinh doanh', 6, 1, 0, NULL);
INSERT INTO financial_item_mappings (che_do, mau_bieu, ma, ma_noi_bo, ten_tren_bao_cao, so_lan_gap, so_bien_the, che_do_suy_doan, ghi_chu) VALUES ('TT99', 'B03-GT', '20', 'LCTT_THUAN_HDKD', 'Lưu chuyển tiền thuần từ hoạt động kinh doanh', 6, 1, 0, NULL);
INSERT INTO financial_item_mappings (che_do, mau_bieu, ma, ma_noi_bo, ten_tren_bao_cao, so_lan_gap, so_bien_the, che_do_suy_doan, ghi_chu) VALUES ('TT99', 'B03-GT', '23', 'LCTT_CHI_CHO_VAY', 'Tiền chi cho vay, mua các công cụ nợ của đơn vị khác', 6, 1, 0, NULL);
INSERT INTO financial_item_mappings (che_do, mau_bieu, ma, ma_noi_bo, ten_tren_bao_cao, so_lan_gap, so_bien_the, che_do_suy_doan, ghi_chu) VALUES ('TT99', 'B03-GT', '24', 'LCTT_THU_HOI_CHO_VAY', 'Tiền thu hồi cho vay, bán lại các công cụ nợ của đơn vị khác', 6, 1, 0, NULL);
INSERT INTO financial_item_mappings (che_do, mau_bieu, ma, ma_noi_bo, ten_tren_bao_cao, so_lan_gap, so_bien_the, che_do_suy_doan, ghi_chu) VALUES ('TT99', 'B03-GT', '27', 'LCTT_THU_LAI_CO_TUC', 'Tiền thu lãi cho vay, cổ tức và lợi nhuận được chia', 6, 1, 0, NULL);
INSERT INTO financial_item_mappings (che_do, mau_bieu, ma, ma_noi_bo, ten_tren_bao_cao, so_lan_gap, so_bien_the, che_do_suy_doan, ghi_chu) VALUES ('TT99', 'B03-GT', '30', 'LCTT_THUAN_HDDT', 'Lưu chuyển tiền thuần từ hoạt động đầu tư', 6, 1, 0, NULL);
INSERT INTO financial_item_mappings (che_do, mau_bieu, ma, ma_noi_bo, ten_tren_bao_cao, so_lan_gap, so_bien_the, che_do_suy_doan, ghi_chu) VALUES ('TT99', 'B03-GT', '33', 'LCTT_THU_DI_VAY', 'Tiền thu từ đi vay', 6, 2, 0, NULL);
INSERT INTO financial_item_mappings (che_do, mau_bieu, ma, ma_noi_bo, ten_tren_bao_cao, so_lan_gap, so_bien_the, che_do_suy_doan, ghi_chu) VALUES ('TT99', 'B03-GT', '34', 'LCTT_TRA_NO_GOC_VAY', 'Tiền trả nợ gốc vay', 6, 1, 0, NULL);
INSERT INTO financial_item_mappings (che_do, mau_bieu, ma, ma_noi_bo, ten_tren_bao_cao, so_lan_gap, so_bien_the, che_do_suy_doan, ghi_chu) VALUES ('TT99', 'B03-GT', '36', 'LCTT_CO_TUC_DA_TRA', 'Cổ tức, lợi nhuận đã trả cho chủ sở hữu', 6, 1, 0, NULL);
INSERT INTO financial_item_mappings (che_do, mau_bieu, ma, ma_noi_bo, ten_tren_bao_cao, so_lan_gap, so_bien_the, che_do_suy_doan, ghi_chu) VALUES ('TT99', 'B03-GT', '40', 'LCTT_THUAN_HDTC', 'Lưu chuyển tiền thuần từ hoạt động tài chính', 6, 1, 0, NULL);
INSERT INTO financial_item_mappings (che_do, mau_bieu, ma, ma_noi_bo, ten_tren_bao_cao, so_lan_gap, so_bien_the, che_do_suy_doan, ghi_chu) VALUES ('TT99', 'B03-GT', '50', 'LCTT_THUAN_TRONG_KY', 'Lưu chuyển tiền thuần trong kỳ', 6, 1, 0, NULL);
INSERT INTO financial_item_mappings (che_do, mau_bieu, ma, ma_noi_bo, ten_tren_bao_cao, so_lan_gap, so_bien_the, che_do_suy_doan, ghi_chu) VALUES ('TT99', 'B03-GT', '60', 'TIEN_DAU_KY', 'Tiền và tương đương tiền đầu kỳ', 6, 2, 0, NULL);
INSERT INTO financial_item_mappings (che_do, mau_bieu, ma, ma_noi_bo, ten_tren_bao_cao, so_lan_gap, so_bien_the, che_do_suy_doan, ghi_chu) VALUES ('TT99', 'B03-GT', '61', 'LCTT_ANH_HUONG_TY_GIA', 'Ảnh hưởng của thay đổi tỷ giá hối đoái quy đổi ngoại tệ', 6, 1, 0, NULL);
INSERT INTO financial_item_mappings (che_do, mau_bieu, ma, ma_noi_bo, ten_tren_bao_cao, so_lan_gap, so_bien_the, che_do_suy_doan, ghi_chu) VALUES ('TT99', 'B03-GT', '70', 'TIEN_CUOI_KY', 'Tiền và tương đương tiền cuối kỳ', 6, 1, 0, NULL);

-- ----- TT99 / B01 -----
INSERT INTO financial_item_mappings (che_do, mau_bieu, ma, ma_noi_bo, ten_tren_bao_cao, so_lan_gap, so_bien_the, che_do_suy_doan, ghi_chu) VALUES ('TT99', 'B01', '100', 'TAI_SAN_NGAN_HAN', 'TÀI SẢN NGẮN HẠN', 5, 1, 0, NULL);
INSERT INTO financial_item_mappings (che_do, mau_bieu, ma, ma_noi_bo, ten_tren_bao_cao, so_lan_gap, so_bien_the, che_do_suy_doan, ghi_chu) VALUES ('TT99', 'B01', '111', 'TIEN_M111', 'Tiền', 5, 1, 0, NULL);
INSERT INTO financial_item_mappings (che_do, mau_bieu, ma, ma_noi_bo, ten_tren_bao_cao, so_lan_gap, so_bien_the, che_do_suy_doan, ghi_chu) VALUES ('TT99', 'B01', '131', 'PHAI_THU_KHACH_HANG_NH', 'Phải thu ngắn hạn của khách hàng', 5, 1, 0, NULL);
INSERT INTO financial_item_mappings (che_do, mau_bieu, ma, ma_noi_bo, ten_tren_bao_cao, so_lan_gap, so_bien_the, che_do_suy_doan, ghi_chu) VALUES ('TT99', 'B01', '132', 'TRA_TRUOC_NGUOI_BAN_NH', 'Trả trước cho người bán ngắn hạn', 5, 1, 0, NULL);
INSERT INTO financial_item_mappings (che_do, mau_bieu, ma, ma_noi_bo, ten_tren_bao_cao, so_lan_gap, so_bien_the, che_do_suy_doan, ghi_chu) VALUES ('TT99', 'B01', '140', 'HANG_TON_KHO_M140', 'Hàng tồn kho', 5, 1, 0, NULL);
INSERT INTO financial_item_mappings (che_do, mau_bieu, ma, ma_noi_bo, ten_tren_bao_cao, so_lan_gap, so_bien_the, che_do_suy_doan, ghi_chu) VALUES ('TT99', 'B01', '141', 'HANG_TON_KHO_M141', 'Hàng tồn kho', 5, 1, 0, NULL);
INSERT INTO financial_item_mappings (che_do, mau_bieu, ma, ma_noi_bo, ten_tren_bao_cao, so_lan_gap, so_bien_the, che_do_suy_doan, ghi_chu) VALUES ('TT99', 'B01', '142', 'DU_PHONG_GIAM_GIA_HTK', 'Dự phòng giảm giá hàng tồn kho', 5, 1, 0, NULL);
INSERT INTO financial_item_mappings (che_do, mau_bieu, ma, ma_noi_bo, ten_tren_bao_cao, so_lan_gap, so_bien_the, che_do_suy_doan, ghi_chu) VALUES ('TT99', 'B01', '160', 'TAI_SAN_NGAN_HAN_KHAC', 'Tài sản ngắn hạn khác', 5, 1, 0, NULL);
INSERT INTO financial_item_mappings (che_do, mau_bieu, ma, ma_noi_bo, ten_tren_bao_cao, so_lan_gap, so_bien_the, che_do_suy_doan, ghi_chu) VALUES ('TT99', 'B01', '161', 'CHI_PHI_CHO_PHAN_BO_NGAN_HAN', 'Chi phí chờ phân bổ ngắn hạn', 5, 1, 0, NULL);
INSERT INTO financial_item_mappings (che_do, mau_bieu, ma, ma_noi_bo, ten_tren_bao_cao, so_lan_gap, so_bien_the, che_do_suy_doan, ghi_chu) VALUES ('TT99', 'B01', '162', 'THUE_GTGT_DUOC_KHAU_TRU', 'Thuế GTGT được khấu trừ', 5, 1, 0, NULL);
INSERT INTO financial_item_mappings (che_do, mau_bieu, ma, ma_noi_bo, ten_tren_bao_cao, so_lan_gap, so_bien_the, che_do_suy_doan, ghi_chu) VALUES ('TT99', 'B01', '163', 'THUE_PHAI_THU_NHA_NUOC', 'Thuế và các khoản khác phải thu Nhà nước', 5, 1, 0, NULL);
INSERT INTO financial_item_mappings (che_do, mau_bieu, ma, ma_noi_bo, ten_tren_bao_cao, so_lan_gap, so_bien_the, che_do_suy_doan, ghi_chu) VALUES ('TT99', 'B01', '200', 'TAI_SAN_DAI_HAN', 'TÀI SẢN DÀI HẠN', 5, 1, 0, NULL);
INSERT INTO financial_item_mappings (che_do, mau_bieu, ma, ma_noi_bo, ten_tren_bao_cao, so_lan_gap, so_bien_the, che_do_suy_doan, ghi_chu) VALUES ('TT99', 'B01', '220', 'TAI_SAN_CO_DINH', 'Tài sản cố định', 5, 1, 0, NULL);
INSERT INTO financial_item_mappings (che_do, mau_bieu, ma, ma_noi_bo, ten_tren_bao_cao, so_lan_gap, so_bien_the, che_do_suy_doan, ghi_chu) VALUES ('TT99', 'B01', '221', 'TSCD_HUU_HINH', 'Tài sản cố định hữu hình', 5, 1, 0, NULL);
INSERT INTO financial_item_mappings (che_do, mau_bieu, ma, ma_noi_bo, ten_tren_bao_cao, so_lan_gap, so_bien_the, che_do_suy_doan, ghi_chu) VALUES ('TT99', 'B01', '227', 'TSCD_VO_HINH', 'Tài sản cố định vô hình', 5, 1, 0, NULL);
INSERT INTO financial_item_mappings (che_do, mau_bieu, ma, ma_noi_bo, ten_tren_bao_cao, so_lan_gap, so_bien_the, che_do_suy_doan, ghi_chu) VALUES ('TT99', 'B01', '240', 'BDS_DAU_TU', 'Bất động sản đầu tư', 5, 1, 0, NULL);
INSERT INTO financial_item_mappings (che_do, mau_bieu, ma, ma_noi_bo, ten_tren_bao_cao, so_lan_gap, so_bien_the, che_do_suy_doan, ghi_chu) VALUES ('TT99', 'B01', '241', 'BDS_DAU_TU_NGUYEN_GIA', '- Nguyên giá', 5, 1, 0, NULL);
INSERT INTO financial_item_mappings (che_do, mau_bieu, ma, ma_noi_bo, ten_tren_bao_cao, so_lan_gap, so_bien_the, che_do_suy_doan, ghi_chu) VALUES ('TT99', 'B01', '242', 'BDS_DAU_TU_HAO_MON', '- Giá trị hao mòn lũy kế', 5, 1, 0, NULL);
INSERT INTO financial_item_mappings (che_do, mau_bieu, ma, ma_noi_bo, ten_tren_bao_cao, so_lan_gap, so_bien_the, che_do_suy_doan, ghi_chu) VALUES ('TT99', 'B01', '250', 'TAI_SAN_DO_DANG_DH', 'Tài sản dở dang dài hạn', 5, 1, 0, NULL);
INSERT INTO financial_item_mappings (che_do, mau_bieu, ma, ma_noi_bo, ten_tren_bao_cao, so_lan_gap, so_bien_the, che_do_suy_doan, ghi_chu) VALUES ('TT99', 'B01', '252', 'CHI_PHI_XDCB_DO_DANG', 'Chi phí xây dựng cơ bản dở dang', 5, 1, 0, NULL);
INSERT INTO financial_item_mappings (che_do, mau_bieu, ma, ma_noi_bo, ten_tren_bao_cao, so_lan_gap, so_bien_the, che_do_suy_doan, ghi_chu) VALUES ('TT99', 'B01', '270', 'TAI_SAN_DAI_HAN_KHAC', 'Tài sản dài hạn khác', 5, 1, 0, NULL);
INSERT INTO financial_item_mappings (che_do, mau_bieu, ma, ma_noi_bo, ten_tren_bao_cao, so_lan_gap, so_bien_the, che_do_suy_doan, ghi_chu) VALUES ('TT99', 'B01', '271', 'CHI_PHI_CHO_PHAN_BO_DAI_HAN', 'Chi phí chờ phân bổ dài hạn', 5, 1, 0, NULL);
INSERT INTO financial_item_mappings (che_do, mau_bieu, ma, ma_noi_bo, ten_tren_bao_cao, so_lan_gap, so_bien_the, che_do_suy_doan, ghi_chu) VALUES ('TT99', 'B01', '280', 'TONG_TAI_SAN', 'TỔNG CỘNG TÀI SẢN', 5, 1, 0, NULL);
INSERT INTO financial_item_mappings (che_do, mau_bieu, ma, ma_noi_bo, ten_tren_bao_cao, so_lan_gap, so_bien_the, che_do_suy_doan, ghi_chu) VALUES ('TT99', 'B01', '300', 'NO_PHAI_TRA', 'NỢ PHẢI TRẢ', 5, 2, 0, NULL);
INSERT INTO financial_item_mappings (che_do, mau_bieu, ma, ma_noi_bo, ten_tren_bao_cao, so_lan_gap, so_bien_the, che_do_suy_doan, ghi_chu) VALUES ('TT99', 'B01', '310', 'NO_NGAN_HAN', 'Nợ ngắn hạn', 5, 1, 0, NULL);

-- ----- TT99 / B03-GT -----
INSERT INTO financial_item_mappings (che_do, mau_bieu, ma, ma_noi_bo, ten_tren_bao_cao, so_lan_gap, so_bien_the, che_do_suy_doan, ghi_chu) VALUES ('TT99', 'B03-GT', '21', 'LCTT_CHI_MUA_TSCD', 'Tiền chi để mua sắm, xây dựng TSCĐ và các tài sản dài hạn khác', 5, 1, 0, NULL);

-- ----- TT200 / B03-TT -----
INSERT INTO financial_item_mappings (che_do, mau_bieu, ma, ma_noi_bo, ten_tren_bao_cao, so_lan_gap, so_bien_the, che_do_suy_doan, ghi_chu) VALUES ('TT200', 'B03-TT', '01', 'LCTT_TT_THU_BAN_HANG', 'Tiền thu từ bán hàng, cung cấp dịch vụ và doanh thu khác', 4, 1, 0, NULL);
INSERT INTO financial_item_mappings (che_do, mau_bieu, ma, ma_noi_bo, ten_tren_bao_cao, so_lan_gap, so_bien_the, che_do_suy_doan, ghi_chu) VALUES ('TT200', 'B03-TT', '02', 'LCTT_TT_CHI_NGUOI_CUNG_CAP', 'Tiền chi trả cho người cung cấp hàng hóa và dịch vụ', 4, 1, 0, NULL);
INSERT INTO financial_item_mappings (che_do, mau_bieu, ma, ma_noi_bo, ten_tren_bao_cao, so_lan_gap, so_bien_the, che_do_suy_doan, ghi_chu) VALUES ('TT200', 'B03-TT', '03', 'LCTT_TT_CHI_NGUOI_LAO_DONG', 'Tiền chi trả cho người lao động', 4, 1, 0, NULL);
INSERT INTO financial_item_mappings (che_do, mau_bieu, ma, ma_noi_bo, ten_tren_bao_cao, so_lan_gap, so_bien_the, che_do_suy_doan, ghi_chu) VALUES ('TT200', 'B03-TT', '04', 'LCTT_CHI_PHI_DI_VAY_DA_TRA', 'Tiền lãi vay đã trả', 4, 1, 0, NULL);
INSERT INTO financial_item_mappings (che_do, mau_bieu, ma, ma_noi_bo, ten_tren_bao_cao, so_lan_gap, so_bien_the, che_do_suy_doan, ghi_chu) VALUES ('TT200', 'B03-TT', '05', 'LCTT_THUE_TNDN_DA_NOP', 'Thuế thu nhập doanh nghiệp đã nộp', 4, 1, 0, NULL);
INSERT INTO financial_item_mappings (che_do, mau_bieu, ma, ma_noi_bo, ten_tren_bao_cao, so_lan_gap, so_bien_the, che_do_suy_doan, ghi_chu) VALUES ('TT200', 'B03-TT', '06', 'LCTT_TIEN_THU_KHAC_HDKD', 'Tiền thu khác từ hoạt động kinh doanh', 4, 1, 0, NULL);
INSERT INTO financial_item_mappings (che_do, mau_bieu, ma, ma_noi_bo, ten_tren_bao_cao, so_lan_gap, so_bien_the, che_do_suy_doan, ghi_chu) VALUES ('TT200', 'B03-TT', '07', 'LCTT_TIEN_CHI_KHAC_HDKD', 'Tiền chi khác cho hoạt động kinh doanh', 4, 1, 0, NULL);
INSERT INTO financial_item_mappings (che_do, mau_bieu, ma, ma_noi_bo, ten_tren_bao_cao, so_lan_gap, so_bien_the, che_do_suy_doan, ghi_chu) VALUES ('TT200', 'B03-TT', '20', 'LCTT_THUAN_HDKD', 'Lưu chuyển tiền thuần từ hoạt động kinh doanh', 4, 1, 0, NULL);
INSERT INTO financial_item_mappings (che_do, mau_bieu, ma, ma_noi_bo, ten_tren_bao_cao, so_lan_gap, so_bien_the, che_do_suy_doan, ghi_chu) VALUES ('TT200', 'B03-TT', '21', 'LCTT_CHI_MUA_TSCD', 'Tiền chi để mua sắm, xây dựng TSCĐ và các tài sản dài hạn khác', 4, 1, 0, NULL);
INSERT INTO financial_item_mappings (che_do, mau_bieu, ma, ma_noi_bo, ten_tren_bao_cao, so_lan_gap, so_bien_the, che_do_suy_doan, ghi_chu) VALUES ('TT200', 'B03-TT', '27', 'LCTT_THU_LAI_CO_TUC', 'Tiền thu lãi cho vay, cổ tức và lợi nhuận được chia', 4, 1, 0, NULL);
INSERT INTO financial_item_mappings (che_do, mau_bieu, ma, ma_noi_bo, ten_tren_bao_cao, so_lan_gap, so_bien_the, che_do_suy_doan, ghi_chu) VALUES ('TT200', 'B03-TT', '30', 'LCTT_THUAN_HDDT', 'Lưu chuyển tiền thuần từ hoạt động đầu tư', 4, 1, 0, NULL);
INSERT INTO financial_item_mappings (che_do, mau_bieu, ma, ma_noi_bo, ten_tren_bao_cao, so_lan_gap, so_bien_the, che_do_suy_doan, ghi_chu) VALUES ('TT200', 'B03-TT', '33', 'LCTT_THU_DI_VAY', 'Tiền thu từ đi vay', 4, 1, 0, NULL);
INSERT INTO financial_item_mappings (che_do, mau_bieu, ma, ma_noi_bo, ten_tren_bao_cao, so_lan_gap, so_bien_the, che_do_suy_doan, ghi_chu) VALUES ('TT200', 'B03-TT', '34', 'LCTT_TRA_NO_GOC_VAY', 'Tiền chi trả nợ gốc vay', 4, 1, 0, NULL);
INSERT INTO financial_item_mappings (che_do, mau_bieu, ma, ma_noi_bo, ten_tren_bao_cao, so_lan_gap, so_bien_the, che_do_suy_doan, ghi_chu) VALUES ('TT200', 'B03-TT', '36', 'LCTT_CO_TUC_DA_TRA', 'Cổ tức, lợi nhuận đã trả cho chủ sở hữu', 4, 1, 0, NULL);
INSERT INTO financial_item_mappings (che_do, mau_bieu, ma, ma_noi_bo, ten_tren_bao_cao, so_lan_gap, so_bien_the, che_do_suy_doan, ghi_chu) VALUES ('TT200', 'B03-TT', '40', 'LCTT_THUAN_HDTC', 'Lưu chuyển tiền thuần từ hoạt động tài chính', 4, 1, 0, NULL);
INSERT INTO financial_item_mappings (che_do, mau_bieu, ma, ma_noi_bo, ten_tren_bao_cao, so_lan_gap, so_bien_the, che_do_suy_doan, ghi_chu) VALUES ('TT200', 'B03-TT', '50', 'LCTT_THUAN_TRONG_KY', 'Lưu chuyển tiền thuần trong kỳ', 4, 1, 0, NULL);
INSERT INTO financial_item_mappings (che_do, mau_bieu, ma, ma_noi_bo, ten_tren_bao_cao, so_lan_gap, so_bien_the, che_do_suy_doan, ghi_chu) VALUES ('TT200', 'B03-TT', '60', 'TIEN_DAU_KY', 'Tiền và tương đương tiền đầu kỳ', 4, 1, 0, NULL);
INSERT INTO financial_item_mappings (che_do, mau_bieu, ma, ma_noi_bo, ten_tren_bao_cao, so_lan_gap, so_bien_the, che_do_suy_doan, ghi_chu) VALUES ('TT200', 'B03-TT', '61', 'LCTT_ANH_HUONG_TY_GIA', 'Ảnh hưởng của thay đổi tỷ giá hối đoái quy đổi ngoại tệ', 4, 1, 0, NULL);
INSERT INTO financial_item_mappings (che_do, mau_bieu, ma, ma_noi_bo, ten_tren_bao_cao, so_lan_gap, so_bien_the, che_do_suy_doan, ghi_chu) VALUES ('TT200', 'B03-TT', '70', 'TIEN_CUOI_KY', 'Tiền và tương đương tiền cuối kỳ', 4, 1, 0, NULL);

-- ----- TT99 / B01 -----
INSERT INTO financial_item_mappings (che_do, mau_bieu, ma, ma_noi_bo, ten_tren_bao_cao, so_lan_gap, so_bien_the, che_do_suy_doan, ghi_chu) VALUES ('TT99', 'B01', '222', 'TSCD_HUU_HINH_NGUYEN_GIA', '- Nguyên giá', 4, 1, 0, NULL);
INSERT INTO financial_item_mappings (che_do, mau_bieu, ma, ma_noi_bo, ten_tren_bao_cao, so_lan_gap, so_bien_the, che_do_suy_doan, ghi_chu) VALUES ('TT99', 'B01', '223', 'TSCD_HUU_HINH_HAO_MON', '- Giá trị hao mòn lũy kế', 4, 1, 0, NULL);
INSERT INTO financial_item_mappings (che_do, mau_bieu, ma, ma_noi_bo, ten_tren_bao_cao, so_lan_gap, so_bien_the, che_do_suy_doan, ghi_chu) VALUES ('TT99', 'B01', '228', 'TSCD_VO_HINH_NGUYEN_GIA', '- Nguyên giá', 4, 1, 0, NULL);
INSERT INTO financial_item_mappings (che_do, mau_bieu, ma, ma_noi_bo, ten_tren_bao_cao, so_lan_gap, so_bien_the, che_do_suy_doan, ghi_chu) VALUES ('TT99', 'B01', '229', 'TSCD_VO_HINH_HAO_MON', '- Giá trị hao mòn lũy kế', 4, 1, 0, NULL);
INSERT INTO financial_item_mappings (che_do, mau_bieu, ma, ma_noi_bo, ten_tren_bao_cao, so_lan_gap, so_bien_the, che_do_suy_doan, ghi_chu) VALUES ('TT99', 'B01', '311', 'PHAI_TRA_NGUOI_BAN_NH', 'Phải trả người bán ngắn hạn', 4, 1, 0, NULL);
INSERT INTO financial_item_mappings (che_do, mau_bieu, ma, ma_noi_bo, ten_tren_bao_cao, so_lan_gap, so_bien_the, che_do_suy_doan, ghi_chu) VALUES ('TT99', 'B01', '312', 'NGUOI_MUA_TRA_TIEN_TRUOC_NH', 'Người mua trả tiền trước ngắn hạn', 4, 1, 0, NULL);
INSERT INTO financial_item_mappings (che_do, mau_bieu, ma, ma_noi_bo, ten_tren_bao_cao, so_lan_gap, so_bien_the, che_do_suy_doan, ghi_chu) VALUES ('TT99', 'B01', '314', 'THUE_PHAI_NOP_NHA_NUOC', 'Thuế và các khoản phải nộp Nhà nước', 4, 2, 0, NULL);
INSERT INTO financial_item_mappings (che_do, mau_bieu, ma, ma_noi_bo, ten_tren_bao_cao, so_lan_gap, so_bien_the, che_do_suy_doan, ghi_chu) VALUES ('TT99', 'B01', '315', 'PHAI_TRA_NGUOI_LAO_DONG', 'Phải trả người lao động', 4, 1, 0, NULL);
INSERT INTO financial_item_mappings (che_do, mau_bieu, ma, ma_noi_bo, ten_tren_bao_cao, so_lan_gap, so_bien_the, che_do_suy_doan, ghi_chu) VALUES ('TT99', 'B01', '316', 'CHI_PHI_PHAI_TRA_NH', 'Chi phí phải trả ngắn hạn', 4, 1, 0, NULL);
INSERT INTO financial_item_mappings (che_do, mau_bieu, ma, ma_noi_bo, ten_tren_bao_cao, so_lan_gap, so_bien_the, che_do_suy_doan, ghi_chu) VALUES ('TT99', 'B01', '319', 'DOANH_THU_CHUA_THUC_HIEN_NH', 'Doanh thu chờ phân bổ ngắn hạn', 4, 1, 0, NULL);
INSERT INTO financial_item_mappings (che_do, mau_bieu, ma, ma_noi_bo, ten_tren_bao_cao, so_lan_gap, so_bien_the, che_do_suy_doan, ghi_chu) VALUES ('TT99', 'B01', '321', 'VAY_NO_THUE_TC_NH', 'Vay và nợ thuê tài chính ngắn hạn', 4, 1, 0, NULL);
INSERT INTO financial_item_mappings (che_do, mau_bieu, ma, ma_noi_bo, ten_tren_bao_cao, so_lan_gap, so_bien_the, che_do_suy_doan, ghi_chu) VALUES ('TT99', 'B01', '322', 'DU_PHONG_PHAI_TRA_NH', 'Dự phòng phải trả ngắn hạn', 4, 1, 0, NULL);
INSERT INTO financial_item_mappings (che_do, mau_bieu, ma, ma_noi_bo, ten_tren_bao_cao, so_lan_gap, so_bien_the, che_do_suy_doan, ghi_chu) VALUES ('TT99', 'B01', '323', 'QUY_KHEN_THUONG_PHUC_LOI', 'Quỹ khen thưởng phúc lợi', 4, 1, 0, NULL);
INSERT INTO financial_item_mappings (che_do, mau_bieu, ma, ma_noi_bo, ten_tren_bao_cao, so_lan_gap, so_bien_the, che_do_suy_doan, ghi_chu) VALUES ('TT99', 'B01', '330', 'NO_DAI_HAN', 'Nợ dài hạn', 4, 1, 0, NULL);
INSERT INTO financial_item_mappings (che_do, mau_bieu, ma, ma_noi_bo, ten_tren_bao_cao, so_lan_gap, so_bien_the, che_do_suy_doan, ghi_chu) VALUES ('TT99', 'B01', '339', 'VAY_NO_THUE_TC_DH', 'Vay và nợ thuê tài chính dài hạn', 4, 1, 0, NULL);
INSERT INTO financial_item_mappings (che_do, mau_bieu, ma, ma_noi_bo, ten_tren_bao_cao, so_lan_gap, so_bien_the, che_do_suy_doan, ghi_chu) VALUES ('TT99', 'B01', '400', 'VON_CHU_SO_HUU_M400', 'VỐN CHỦ SỞ HỮU', 4, 1, 0, NULL);
INSERT INTO financial_item_mappings (che_do, mau_bieu, ma, ma_noi_bo, ten_tren_bao_cao, so_lan_gap, so_bien_the, che_do_suy_doan, ghi_chu) VALUES ('TT99', 'B01', '411', 'VON_GOP_CHU_SO_HUU', 'Vốn góp của chủ sở hữu', 4, 1, 0, NULL);
INSERT INTO financial_item_mappings (che_do, mau_bieu, ma, ma_noi_bo, ten_tren_bao_cao, so_lan_gap, so_bien_the, che_do_suy_doan, ghi_chu) VALUES ('TT99', 'B01', '412', 'THANG_DU_VON', 'Thặng dư vốn', 4, 1, 0, NULL);
INSERT INTO financial_item_mappings (che_do, mau_bieu, ma, ma_noi_bo, ten_tren_bao_cao, so_lan_gap, so_bien_the, che_do_suy_doan, ghi_chu) VALUES ('TT99', 'B01', '418', 'QUY_DAU_TU_PHAT_TRIEN', 'Quỹ đầu tư phát triển', 4, 1, 0, NULL);
INSERT INTO financial_item_mappings (che_do, mau_bieu, ma, ma_noi_bo, ten_tren_bao_cao, so_lan_gap, so_bien_the, che_do_suy_doan, ghi_chu) VALUES ('TT99', 'B01', '420', 'LNST_CHUA_PHAN_PHOI', 'Lợi nhuận sau thuế chưa phân phối', 4, 1, 0, NULL);
INSERT INTO financial_item_mappings (che_do, mau_bieu, ma, ma_noi_bo, ten_tren_bao_cao, so_lan_gap, so_bien_the, che_do_suy_doan, ghi_chu) VALUES ('TT99', 'B01', '440', 'TONG_NGUON_VON', 'TỔNG CỘNG NGUỒN VỐN', 4, 1, 0, NULL);

-- ----- TT99 / B02 -----
INSERT INTO financial_item_mappings (che_do, mau_bieu, ma, ma_noi_bo, ten_tren_bao_cao, so_lan_gap, so_bien_the, che_do_suy_doan, ghi_chu) VALUES ('TT99', 'B02', '50', 'LOI_NHUAN_TRUOC_THUE', 'Tổng lợi nhuận kế toán trước thuế', 4, 1, 0, NULL);

-- ----- TT99 / B03-GT -----
INSERT INTO financial_item_mappings (che_do, mau_bieu, ma, ma_noi_bo, ten_tren_bao_cao, so_lan_gap, so_bien_the, che_do_suy_doan, ghi_chu) VALUES ('TT99', 'B03-GT', '07', 'LCTT_DIEU_CHINH_KHAC', '- Các khoản điều chỉnh khác', 4, 1, 0, NULL);

-- ----- TT200 / B03-GT -----
INSERT INTO financial_item_mappings (che_do, mau_bieu, ma, ma_noi_bo, ten_tren_bao_cao, so_lan_gap, so_bien_the, che_do_suy_doan, ghi_chu) VALUES ('TT200', 'B03-GT', '07', 'LCTT_DIEU_CHINH_KHAC', '- Các khoản điều chỉnh khác', 3, 1, 0, NULL);

-- ----- TT200 / B03-TT -----
INSERT INTO financial_item_mappings (che_do, mau_bieu, ma, ma_noi_bo, ten_tren_bao_cao, so_lan_gap, so_bien_the, che_do_suy_doan, ghi_chu) VALUES ('TT200', 'B03-TT', '24', 'LCTT_THU_HOI_CHO_VAY', 'Tiền thu hồi cho vay, bán lại các công cụ nợ của đơn vị khác', 3, 1, 0, NULL);

-- ----- TT99 / B01 -----
INSERT INTO financial_item_mappings (che_do, mau_bieu, ma, ma_noi_bo, ten_tren_bao_cao, so_lan_gap, so_bien_the, che_do_suy_doan, ghi_chu) VALUES ('TT99', 'B01', '272', 'TAI_SAN_THUE_TNDN_HOAN_LAI', 'Tài sản thuế thu nhập hoãn lại', 3, 2, 0, NULL);
INSERT INTO financial_item_mappings (che_do, mau_bieu, ma, ma_noi_bo, ten_tren_bao_cao, so_lan_gap, so_bien_the, che_do_suy_doan, ghi_chu) VALUES ('TT99', 'B01', '260', 'DAU_TU_TC_DAI_HAN', 'Đầu tư tài chính dài hạn', 2, 1, 0, NULL);
INSERT INTO financial_item_mappings (che_do, mau_bieu, ma, ma_noi_bo, ten_tren_bao_cao, so_lan_gap, so_bien_the, che_do_suy_doan, ghi_chu) VALUES ('TT99', 'B01', '261', 'DAU_TU_CONG_TY_CON', 'Đầu tư vào công ty con', 2, 1, 0, NULL);
INSERT INTO financial_item_mappings (che_do, mau_bieu, ma, ma_noi_bo, ten_tren_bao_cao, so_lan_gap, so_bien_the, che_do_suy_doan, ghi_chu) VALUES ('TT99', 'B01', '338', 'PHAI_TRA_DAI_HAN_KHAC', 'Phải trả dài hạn khác', 2, 1, 0, NULL);
INSERT INTO financial_item_mappings (che_do, mau_bieu, ma, ma_noi_bo, ten_tren_bao_cao, so_lan_gap, so_bien_the, che_do_suy_doan, ghi_chu) VALUES ('TT99', 'B01', '417', 'CHENH_LECH_TY_GIA', 'Chênh lệch tỷ giá hối đoái', 2, 1, 0, NULL);

-- ----- TT99 / B02 -----
INSERT INTO financial_item_mappings (che_do, mau_bieu, ma, ma_noi_bo, ten_tren_bao_cao, so_lan_gap, so_bien_the, che_do_suy_doan, ghi_chu) VALUES ('TT99', 'B02', '51', 'CHI_PHI_THUE_TNDN_HIEN_HANH', 'Chi phí thuế TNDN hiện hành', 2, 1, 0, NULL);
INSERT INTO financial_item_mappings (che_do, mau_bieu, ma, ma_noi_bo, ten_tren_bao_cao, so_lan_gap, so_bien_the, che_do_suy_doan, ghi_chu) VALUES ('TT99', 'B02', '52', 'CHI_PHI_THUE_TNDN_HOAN_LAI', 'Chi phí thuế TNDN hoãn lại', 2, 1, 0, NULL);
INSERT INTO financial_item_mappings (che_do, mau_bieu, ma, ma_noi_bo, ten_tren_bao_cao, so_lan_gap, so_bien_the, che_do_suy_doan, ghi_chu) VALUES ('TT99', 'B02', '60', 'LOI_NHUAN_SAU_THUE', 'Lợi nhuận sau thuế TNDN', 2, 1, 0, NULL);

-- ----- TT200 / B01 -----
INSERT INTO financial_item_mappings (che_do, mau_bieu, ma, ma_noi_bo, ten_tren_bao_cao, so_lan_gap, so_bien_the, che_do_suy_doan, ghi_chu) VALUES ('TT200', 'B01', '316', 'PHAI_TRA_NOI_BO_NH', 'Phải trả nội bộ ngắn hạn', 1, 1, 0, NULL);
INSERT INTO financial_item_mappings (che_do, mau_bieu, ma, ma_noi_bo, ten_tren_bao_cao, so_lan_gap, so_bien_the, che_do_suy_doan, ghi_chu) VALUES ('TT200', 'B01', '317', 'PHAI_TRA_TIEN_DO_HDXD', 'Phải trả theo tiến độ kế hoạch hợp đồng xây dựng', 1, 1, 0, NULL);

-- ----- TT200 / B03-GT -----
INSERT INTO financial_item_mappings (che_do, mau_bieu, ma, ma_noi_bo, ten_tren_bao_cao, so_lan_gap, so_bien_the, che_do_suy_doan, ghi_chu) VALUES ('TT200', 'B03-GT', '16', 'LCTT_TIEN_THU_KHAC_HDKD', '- Tiền thu khác từ hoạt động kinh doanh', 1, 1, 0, NULL);

-- ----- TT200 / B03-TT -----
INSERT INTO financial_item_mappings (che_do, mau_bieu, ma, ma_noi_bo, ten_tren_bao_cao, so_lan_gap, so_bien_the, che_do_suy_doan, ghi_chu) VALUES ('TT200', 'B03-TT', '23', 'LCTT_CHI_CHO_VAY', 'Tiền chi cho vay, mua các công cụ nợ của đơn vị khác', 1, 1, 0, NULL);

-- ----- TT99 / B02 -----
INSERT INTO financial_item_mappings (che_do, mau_bieu, ma, ma_noi_bo, ten_tren_bao_cao, so_lan_gap, so_bien_the, che_do_suy_doan, ghi_chu) VALUES ('TT99', 'B02', '27', 'LAI_LO_LIEN_DOANH_LIEN_KET', 'Phần lãi hoặc lỗ trong công ty liên doanh, liên kết', 1, 1, 0, NULL);
INSERT INTO financial_item_mappings (che_do, mau_bieu, ma, ma_noi_bo, ten_tren_bao_cao, so_lan_gap, so_bien_the, che_do_suy_doan, ghi_chu) VALUES ('TT99', 'B02', '61', 'LNST_CONG_TY_ME', 'Lợi nhuận sau thuế của Công ty mẹ', 1, 1, 0, NULL);
INSERT INTO financial_item_mappings (che_do, mau_bieu, ma, ma_noi_bo, ten_tren_bao_cao, so_lan_gap, so_bien_the, che_do_suy_doan, ghi_chu) VALUES ('TT99', 'B02', '62', 'LNST_CDKKS', 'Lợi nhuận sau thuế của cổ đông không kiểm soát', 1, 1, 0, NULL);
