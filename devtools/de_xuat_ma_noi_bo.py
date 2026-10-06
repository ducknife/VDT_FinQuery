# -*- coding: utf-8 -*-
"""Điền đề xuất ma_noi_bo + ten_chuan vào bảng thô do quet_ma_chi_tieu.py sinh ra.

ĐÂY LÀ ĐỀ XUẤT ĐỂ NGƯỜI REVIEW, không phải chốt. Tên trong bảng DE_XUAT dưới đây
là quyết định nghiệp vụ — sửa thẳng ở đây rồi chạy lại.

Chạy:  python -X utf8 devtools/de_xuat_ma_noi_bo.py
Ra:    devtools/ra/ma_chi_tieu_de_xuat.csv
"""
import io
import os
import csv
import collections

GOC = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
RA = os.path.join(GOC, "devtools", "ra")

# ---------------------------------------------------------------------------
# (mẫu biểu, chế độ, mã)  ->  (ma_noi_bo, ten_chuan)
# ---------------------------------------------------------------------------
DE_XUAT = {}


def them(mau_bieu, che_do, bang):
    for ma, (mnb, ten) in bang.items():
        DE_XUAT[(mau_bieu, che_do, ma)] = (mnb, ten)


# ------------------------------------------------------------------ B01 TT200
them("B01", "TT200", {
    "100": ("TAI_SAN_NGAN_HAN", "Tài sản ngắn hạn"),
    "110": ("TIEN_VA_TUONG_DUONG_TIEN", "Tiền và các khoản tương đương tiền"),
    "111": ("TIEN_M111", "Tiền (tiền mặt và tiền gửi không kỳ hạn)"),
    "112": ("TUONG_DUONG_TIEN", "Các khoản tương đương tiền"),
    "120": ("DAU_TU_TC_NGAN_HAN", "Đầu tư tài chính ngắn hạn"),
    "123": ("DAU_TU_NAM_GIU_DAO_HAN_NH", "Đầu tư nắm giữ đến ngày đáo hạn ngắn hạn"),
    "130": ("PHAI_THU_NGAN_HAN", "Các khoản phải thu ngắn hạn"),
    "131": ("PHAI_THU_KHACH_HANG_NH", "Phải thu ngắn hạn của khách hàng"),
    "132": ("TRA_TRUOC_NGUOI_BAN_NH", "Trả trước cho người bán ngắn hạn"),
    "136": ("PHAI_THU_NGAN_HAN_KHAC", "Phải thu ngắn hạn khác"),
    "137": ("DU_PHONG_PHAI_THU_KHO_DOI", "Dự phòng phải thu ngắn hạn khó đòi"),
    "140": ("HANG_TON_KHO_M140", "Hàng tồn kho — mã 140 (thuần, đã trừ dự phòng)"),
    "141": ("HANG_TON_KHO_M141", "Hàng tồn kho — mã 141 (giá gốc, chưa trừ dự phòng)"),
    "149": ("DU_PHONG_GIAM_GIA_HTK", "Dự phòng giảm giá hàng tồn kho"),
    "150": ("TAI_SAN_NGAN_HAN_KHAC", "Tài sản ngắn hạn khác"),
    "151": ("CHI_PHI_CHO_PHAN_BO_NGAN_HAN", "Chi phí trả trước ngắn hạn"),
    "152": ("THUE_GTGT_DUOC_KHAU_TRU", "Thuế GTGT được khấu trừ"),
    "153": ("THUE_PHAI_THU_NHA_NUOC", "Thuế và các khoản khác phải thu Nhà nước"),
    "200": ("TAI_SAN_DAI_HAN", "Tài sản dài hạn"),
    "220": ("TAI_SAN_CO_DINH", "Tài sản cố định"),
    "221": ("TSCD_HUU_HINH", "Tài sản cố định hữu hình"),
    "222": ("TSCD_HUU_HINH_NGUYEN_GIA", "TSCĐ hữu hình — nguyên giá"),
    "223": ("TSCD_HUU_HINH_HAO_MON", "TSCĐ hữu hình — hao mòn luỹ kế"),
    "227": ("TSCD_VO_HINH", "Tài sản cố định vô hình"),
    "228": ("TSCD_VO_HINH_NGUYEN_GIA", "TSCĐ vô hình — nguyên giá"),
    "229": ("TSCD_VO_HINH_HAO_MON", "TSCĐ vô hình — hao mòn luỹ kế"),
    "230": ("BDS_DAU_TU", "Bất động sản đầu tư"),
    "231": ("BDS_DAU_TU_NGUYEN_GIA", "Bất động sản đầu tư — nguyên giá"),
    "232": ("BDS_DAU_TU_HAO_MON", "Bất động sản đầu tư — hao mòn luỹ kế"),
    "240": ("TAI_SAN_DO_DANG_DH", "Tài sản dở dang dài hạn"),
    "242": ("CHI_PHI_XDCB_DO_DANG", "Chi phí xây dựng cơ bản dở dang"),
    "250": ("DAU_TU_TC_DAI_HAN", "Đầu tư tài chính dài hạn"),
    "251": ("DAU_TU_CONG_TY_CON", "Đầu tư vào công ty con"),
    "260": ("TAI_SAN_DAI_HAN_KHAC", "Tài sản dài hạn khác"),
    "261": ("CHI_PHI_CHO_PHAN_BO_DAI_HAN", "Chi phí trả trước dài hạn"),
    "262": ("TAI_SAN_THUE_TNDN_HOAN_LAI", "Tài sản thuế thu nhập hoãn lại"),
    "270": ("TONG_TAI_SAN", "Tổng cộng tài sản"),
    "300": ("NO_PHAI_TRA", "Nợ phải trả"),
    "310": ("NO_NGAN_HAN", "Nợ ngắn hạn"),
    "311": ("PHAI_TRA_NGUOI_BAN_NH", "Phải trả người bán ngắn hạn"),
    "312": ("NGUOI_MUA_TRA_TIEN_TRUOC_NH", "Người mua trả tiền trước ngắn hạn"),
    "313": ("THUE_PHAI_NOP_NHA_NUOC", "Thuế và các khoản phải nộp Nhà nước"),
    "314": ("PHAI_TRA_NGUOI_LAO_DONG", "Phải trả người lao động"),
    "315": ("CHI_PHI_PHAI_TRA_NH", "Chi phí phải trả ngắn hạn"),
    "316": ("PHAI_TRA_NOI_BO_NH", "Phải trả nội bộ ngắn hạn"),
    "317": ("PHAI_TRA_TIEN_DO_HDXD", "Phải trả theo tiến độ kế hoạch hợp đồng xây dựng"),
    "318": ("DOANH_THU_CHUA_THUC_HIEN_NH", "Doanh thu chưa thực hiện ngắn hạn"),
    "319": ("PHAI_TRA_NGAN_HAN_KHAC", "Phải trả ngắn hạn khác"),
    "320": ("VAY_NO_THUE_TC_NH", "Vay và nợ thuê tài chính ngắn hạn"),
    "321": ("DU_PHONG_PHAI_TRA_NH", "Dự phòng phải trả ngắn hạn"),
    "322": ("QUY_KHEN_THUONG_PHUC_LOI", "Quỹ khen thưởng, phúc lợi"),
    "330": ("NO_DAI_HAN", "Nợ dài hạn"),
    "337": ("PHAI_TRA_DAI_HAN_KHAC", "Phải trả dài hạn khác"),
    "338": ("VAY_NO_THUE_TC_DH", "Vay và nợ thuê tài chính dài hạn"),
    "400": ("VON_CHU_SO_HUU_M400", "Vốn chủ sở hữu — mã 400 (khối D, tổng)"),
    "410": ("VON_CHU_SO_HUU_M410", "Vốn chủ sở hữu — mã 410 (nhóm I, chỉ TT200)"),
    "411": ("VON_GOP_CHU_SO_HUU", "Vốn góp của chủ sở hữu"),
    "412": ("THANG_DU_VON", "Thặng dư vốn cổ phần"),
    "417": ("CHENH_LECH_TY_GIA", "Chênh lệch tỷ giá hối đoái"),
    "418": ("QUY_DAU_TU_PHAT_TRIEN", "Quỹ đầu tư phát triển"),
    "421": ("LNST_CHUA_PHAN_PHOI", "Lợi nhuận sau thuế chưa phân phối"),
    "429": ("LOI_ICH_CDKKS", "Lợi ích cổ đông không kiểm soát"),
    "440": ("TONG_NGUON_VON", "Tổng cộng nguồn vốn"),
})

# ------------------------------------------------------------------- B01 TT99
# Đánh lại số hàng loạt so với TT200. Chú ý 136, 240-272, 313-323, 338-339, 420.
them("B01", "TT99", {
    "100": ("TAI_SAN_NGAN_HAN", "Tài sản ngắn hạn"),
    "110": ("TIEN_VA_TUONG_DUONG_TIEN", "Tiền và các khoản tương đương tiền"),
    "111": ("TIEN_M111", "Tiền (tiền mặt và tiền gửi không kỳ hạn)"),
    "112": ("TUONG_DUONG_TIEN", "Các khoản tương đương tiền"),
    "120": ("DAU_TU_TC_NGAN_HAN", "Đầu tư tài chính ngắn hạn"),
    "123": ("DAU_TU_NAM_GIU_DAO_HAN_NH", "Đầu tư nắm giữ đến ngày đáo hạn ngắn hạn"),
    "130": ("PHAI_THU_NGAN_HAN", "Các khoản phải thu ngắn hạn"),
    "131": ("PHAI_THU_KHACH_HANG_NH", "Phải thu ngắn hạn của khách hàng"),
    "132": ("TRA_TRUOC_NGUOI_BAN_NH", "Trả trước cho người bán ngắn hạn"),
    "135": ("PHAI_THU_NGAN_HAN_KHAC", "Phải thu ngắn hạn khác"),
    "136": ("DU_PHONG_PHAI_THU_KHO_DOI", "Dự phòng phải thu ngắn hạn khó đòi"),
    "140": ("HANG_TON_KHO_M140", "Hàng tồn kho — mã 140 (thuần, đã trừ dự phòng)"),
    "141": ("HANG_TON_KHO_M141", "Hàng tồn kho — mã 141 (giá gốc, chưa trừ dự phòng)"),
    "142": ("DU_PHONG_GIAM_GIA_HTK", "Dự phòng giảm giá hàng tồn kho"),
    "160": ("TAI_SAN_NGAN_HAN_KHAC", "Tài sản ngắn hạn khác"),
    "161": ("CHI_PHI_CHO_PHAN_BO_NGAN_HAN", "Chi phí chờ phân bổ ngắn hạn"),
    "162": ("THUE_GTGT_DUOC_KHAU_TRU", "Thuế GTGT được khấu trừ"),
    "163": ("THUE_PHAI_THU_NHA_NUOC", "Thuế và các khoản khác phải thu Nhà nước"),
    "200": ("TAI_SAN_DAI_HAN", "Tài sản dài hạn"),
    "220": ("TAI_SAN_CO_DINH", "Tài sản cố định"),
    "221": ("TSCD_HUU_HINH", "Tài sản cố định hữu hình"),
    "222": ("TSCD_HUU_HINH_NGUYEN_GIA", "TSCĐ hữu hình — nguyên giá"),
    "223": ("TSCD_HUU_HINH_HAO_MON", "TSCĐ hữu hình — hao mòn luỹ kế"),
    "227": ("TSCD_VO_HINH", "Tài sản cố định vô hình"),
    "228": ("TSCD_VO_HINH_NGUYEN_GIA", "TSCĐ vô hình — nguyên giá"),
    "229": ("TSCD_VO_HINH_HAO_MON", "TSCĐ vô hình — hao mòn luỹ kế"),
    "240": ("BDS_DAU_TU", "Bất động sản đầu tư"),
    "241": ("BDS_DAU_TU_NGUYEN_GIA", "Bất động sản đầu tư — nguyên giá"),
    "242": ("BDS_DAU_TU_HAO_MON", "Bất động sản đầu tư — hao mòn luỹ kế"),
    "250": ("TAI_SAN_DO_DANG_DH", "Tài sản dở dang dài hạn"),
    "252": ("CHI_PHI_XDCB_DO_DANG", "Chi phí xây dựng cơ bản dở dang"),
    "260": ("DAU_TU_TC_DAI_HAN", "Đầu tư tài chính dài hạn"),
    "261": ("DAU_TU_CONG_TY_CON", "Đầu tư vào công ty con"),
    "270": ("TAI_SAN_DAI_HAN_KHAC", "Tài sản dài hạn khác"),
    "271": ("CHI_PHI_CHO_PHAN_BO_DAI_HAN", "Chi phí chờ phân bổ dài hạn"),
    "272": ("TAI_SAN_THUE_TNDN_HOAN_LAI", "Tài sản thuế thu nhập hoãn lại"),
    "280": ("TONG_TAI_SAN", "Tổng cộng tài sản"),
    "300": ("NO_PHAI_TRA", "Nợ phải trả"),
    "310": ("NO_NGAN_HAN", "Nợ ngắn hạn"),
    "311": ("PHAI_TRA_NGUOI_BAN_NH", "Phải trả người bán ngắn hạn"),
    "312": ("NGUOI_MUA_TRA_TIEN_TRUOC_NH", "Người mua trả tiền trước ngắn hạn"),
    "313": ("PHAI_TRA_CO_TUC_LOI_NHUAN", "Phải trả cổ tức, lợi nhuận"),
    "314": ("THUE_PHAI_NOP_NHA_NUOC", "Thuế và các khoản phải nộp Nhà nước"),
    "315": ("PHAI_TRA_NGUOI_LAO_DONG", "Phải trả người lao động"),
    "316": ("CHI_PHI_PHAI_TRA_NH", "Chi phí phải trả ngắn hạn"),
    "319": ("DOANH_THU_CHUA_THUC_HIEN_NH", "Doanh thu chờ phân bổ ngắn hạn"),
    "320": ("PHAI_TRA_NGAN_HAN_KHAC", "Phải trả ngắn hạn khác"),
    "321": ("VAY_NO_THUE_TC_NH", "Vay và nợ thuê tài chính ngắn hạn"),
    "322": ("DU_PHONG_PHAI_TRA_NH", "Dự phòng phải trả ngắn hạn"),
    "323": ("QUY_KHEN_THUONG_PHUC_LOI", "Quỹ khen thưởng, phúc lợi"),
    "330": ("NO_DAI_HAN", "Nợ dài hạn"),
    "338": ("PHAI_TRA_DAI_HAN_KHAC", "Phải trả dài hạn khác"),
    "339": ("VAY_NO_THUE_TC_DH", "Vay và nợ thuê tài chính dài hạn"),
    "400": ("VON_CHU_SO_HUU_M400", "Vốn chủ sở hữu — mã 400 (khối D, tổng)"),
    "411": ("VON_GOP_CHU_SO_HUU", "Vốn góp của chủ sở hữu"),
    "412": ("THANG_DU_VON", "Thặng dư vốn"),
    "417": ("CHENH_LECH_TY_GIA", "Chênh lệch tỷ giá hối đoái"),
    "418": ("QUY_DAU_TU_PHAT_TRIEN", "Quỹ đầu tư phát triển"),
    "420": ("LNST_CHUA_PHAN_PHOI", "Lợi nhuận sau thuế chưa phân phối"),
    "440": ("TONG_NGUON_VON", "Tổng cộng nguồn vốn"),
})

# ------------------------------------------------------------------ B02 TT200
them("B02", "TT200", {
    "01": ("DOANH_THU_BAN_HANG", "Doanh thu bán hàng và cung cấp dịch vụ"),
    "02": ("CAC_KHOAN_GIAM_TRU_DT", "Các khoản giảm trừ doanh thu"),
    "10": ("DOANH_THU_THUAN", "Doanh thu thuần về bán hàng và cung cấp dịch vụ"),
    "11": ("GIA_VON_HANG_BAN", "Giá vốn hàng bán"),
    "20": ("LOI_NHUAN_GOP", "Lợi nhuận gộp về bán hàng và cung cấp dịch vụ"),
    "21": ("DOANH_THU_TAI_CHINH", "Doanh thu hoạt động tài chính"),
    "22": ("CHI_PHI_TAI_CHINH", "Chi phí tài chính"),
    "23": ("CHI_PHI_DI_VAY", "Trong đó: chi phí lãi vay"),
    "24": ("LAI_LO_LIEN_DOANH_LIEN_KET", "Phần lãi/lỗ trong công ty liên doanh, liên kết"),
    "25": ("CHI_PHI_BAN_HANG", "Chi phí bán hàng"),
    "26": ("CHI_PHI_QUAN_LY", "Chi phí quản lý doanh nghiệp"),
    "30": ("LOI_NHUAN_THUAN_HDKD", "Lợi nhuận thuần từ hoạt động kinh doanh"),
    "31": ("THU_NHAP_KHAC", "Thu nhập khác"),
    "32": ("CHI_PHI_KHAC", "Chi phí khác"),
    "40": ("LOI_NHUAN_KHAC", "Lợi nhuận khác"),
    "50": ("LOI_NHUAN_TRUOC_THUE", "Tổng lợi nhuận kế toán trước thuế"),
    "51": ("CHI_PHI_THUE_TNDN_HIEN_HANH", "Chi phí thuế TNDN hiện hành"),
    "52": ("CHI_PHI_THUE_TNDN_HOAN_LAI", "Chi phí thuế TNDN hoãn lại"),
    "60": ("LOI_NHUAN_SAU_THUE", "Lợi nhuận sau thuế TNDN"),
    "61": ("LNST_CONG_TY_ME", "Lợi nhuận sau thuế của công ty mẹ"),
    "62": ("LNST_CDKKS", "Lợi nhuận sau thuế của cổ đông không kiểm soát"),
    "70": ("LAI_CO_BAN_TREN_CO_PHIEU", "Lãi cơ bản trên cổ phiếu"),
})

# ------------------------------------------------------------------- B02 TT99
# Mã 21-27 bị đẩy toàn bộ so với TT200 — đây là ca đụng độ nặng nhất của B02.
them("B02", "TT99", {
    "01": ("DOANH_THU_BAN_HANG", "Doanh thu bán hàng và cung cấp dịch vụ"),
    "02": ("CAC_KHOAN_GIAM_TRU_DT", "Các khoản giảm trừ doanh thu"),
    "10": ("DOANH_THU_THUAN", "Doanh thu thuần về bán hàng và cung cấp dịch vụ"),
    "11": ("GIA_VON_HANG_BAN", "Giá vốn hàng bán"),
    "20": ("LOI_NHUAN_GOP", "Lợi nhuận gộp về bán hàng và cung cấp dịch vụ"),
    "21": ("LAI_LO_BAN_THANH_LY_BDSDT", "Lãi/lỗ của hoạt động bán, thanh lý bất động sản đầu tư"),
    "22": ("DOANH_THU_TAI_CHINH", "Doanh thu hoạt động tài chính"),
    "23": ("CHI_PHI_TAI_CHINH", "Chi phí tài chính"),
    "24": ("CHI_PHI_DI_VAY", "Trong đó: chi phí đi vay"),
    "25": ("CHI_PHI_BAN_HANG", "Chi phí bán hàng"),
    "26": ("CHI_PHI_QUAN_LY", "Chi phí quản lý doanh nghiệp"),
    "27": ("LAI_LO_LIEN_DOANH_LIEN_KET", "Phần lãi/lỗ trong công ty liên doanh, liên kết"),
    "30": ("LOI_NHUAN_THUAN_HDKD", "Lợi nhuận thuần từ hoạt động kinh doanh"),
    "31": ("THU_NHAP_KHAC", "Thu nhập khác"),
    "32": ("CHI_PHI_KHAC", "Chi phí khác"),
    "40": ("LOI_NHUAN_KHAC", "Lợi nhuận khác"),
    "50": ("LOI_NHUAN_TRUOC_THUE", "Tổng lợi nhuận kế toán trước thuế"),
    "51": ("CHI_PHI_THUE_TNDN_HIEN_HANH", "Chi phí thuế TNDN hiện hành"),
    "52": ("CHI_PHI_THUE_TNDN_HOAN_LAI", "Chi phí thuế TNDN hoãn lại"),
    "60": ("LOI_NHUAN_SAU_THUE", "Lợi nhuận sau thuế TNDN"),
    "61": ("LNST_CONG_TY_ME", "Lợi nhuận sau thuế của công ty mẹ"),
    "62": ("LNST_CDKKS", "Lợi nhuận sau thuế của cổ đông không kiểm soát"),
    "70": ("LAI_CO_BAN_TREN_CO_PHIEU", "Lãi cơ bản trên cổ phiếu"),
})

# ------------------------------------------- B03 gián tiếp (mã giống nhau 2 chế độ)
B03_GT = {
    "01": ("LCTT_LOI_NHUAN_TRUOC_THUE", "LCTT — Lợi nhuận trước thuế"),
    "02": ("LCTT_KHAU_HAO", "LCTT — Khấu hao TSCĐ và bất động sản đầu tư"),
    "03": ("LCTT_DU_PHONG", "LCTT — Các khoản dự phòng"),
    "04": ("LCTT_LAI_LO_TY_GIA", "LCTT — Lãi/lỗ chênh lệch tỷ giá do đánh giá lại"),
    "05": ("LCTT_LAI_LO_DAU_TU", "LCTT — Lãi/lỗ từ hoạt động đầu tư"),
    "06": ("LCTT_CHI_PHI_DI_VAY", "LCTT — Chi phí lãi vay"),
    "07": ("LCTT_DIEU_CHINH_KHAC", "LCTT — Các khoản điều chỉnh khác"),
    "08": ("LCTT_LN_TRUOC_THAY_DOI_VLD", "LCTT — Lợi nhuận từ HĐKD trước thay đổi vốn lưu động"),
    "09": ("LCTT_TANG_GIAM_PHAI_THU", "LCTT — Tăng/giảm các khoản phải thu"),
    "10": ("LCTT_TANG_GIAM_HTK", "LCTT — Tăng/giảm hàng tồn kho"),
    "11": ("LCTT_TANG_GIAM_PHAI_TRA", "LCTT — Tăng/giảm các khoản phải trả"),
    "12": ("LCTT_TANG_GIAM_CHI_PHI_CHO_PHAN_BO", "LCTT — Tăng/giảm chi phí trả trước"),
    "14": ("LCTT_CHI_PHI_DI_VAY_DA_TRA", "LCTT — Tiền lãi vay đã trả"),
    "15": ("LCTT_THUE_TNDN_DA_NOP", "LCTT — Thuế TNDN đã nộp"),
    "16": ("LCTT_TIEN_THU_KHAC_HDKD", "LCTT — Tiền thu khác từ hoạt động kinh doanh"),
    "17": ("LCTT_TIEN_CHI_KHAC_HDKD", "LCTT — Tiền chi khác cho hoạt động kinh doanh"),
    "20": ("LCTT_THUAN_HDKD", "Lưu chuyển tiền thuần từ hoạt động kinh doanh"),
    "21": ("LCTT_CHI_MUA_TSCD", "LCTT — Tiền chi mua sắm, xây dựng TSCĐ"),
    "22": ("LCTT_THU_THANH_LY_TSCD", "LCTT — Tiền thu thanh lý, nhượng bán TSCĐ"),
    "23": ("LCTT_CHI_CHO_VAY", "LCTT — Tiền chi cho vay, mua công cụ nợ"),
    "24": ("LCTT_THU_HOI_CHO_VAY", "LCTT — Tiền thu hồi cho vay, bán lại công cụ nợ"),
    "25": ("LCTT_CHI_GOP_VON", "LCTT — Tiền chi đầu tư góp vốn vào đơn vị khác"),
    "26": ("LCTT_THU_HOI_GOP_VON", "LCTT — Tiền thu hồi đầu tư góp vốn"),
    "27": ("LCTT_THU_LAI_CO_TUC", "LCTT — Tiền thu lãi cho vay, cổ tức, lợi nhuận được chia"),
    "30": ("LCTT_THUAN_HDDT", "Lưu chuyển tiền thuần từ hoạt động đầu tư"),
    "31": ("LCTT_THU_PHAT_HANH_CP", "LCTT — Tiền thu từ phát hành cổ phiếu, nhận vốn góp"),
    "33": ("LCTT_THU_DI_VAY", "LCTT — Tiền thu từ đi vay"),
    "34": ("LCTT_TRA_NO_GOC_VAY", "LCTT — Tiền trả nợ gốc vay"),
    "36": ("LCTT_CO_TUC_DA_TRA", "LCTT — Cổ tức, lợi nhuận đã trả cho chủ sở hữu"),
    "40": ("LCTT_THUAN_HDTC", "Lưu chuyển tiền thuần từ hoạt động tài chính"),
    "50": ("LCTT_THUAN_TRONG_KY", "Lưu chuyển tiền thuần trong kỳ"),
    "60": ("TIEN_DAU_KY", "Tiền và tương đương tiền đầu kỳ"),
    "61": ("LCTT_ANH_HUONG_TY_GIA", "LCTT — Ảnh hưởng thay đổi tỷ giá quy đổi ngoại tệ"),
    "70": ("TIEN_CUOI_KY", "Tiền và tương đương tiền cuối kỳ"),
}
them("B03-GT", "TT200", B03_GT)
them("B03-GT", "TT99", B03_GT)

# -------------------------------------------------------- B03 trực tiếp (TT200)
# Mã 01-07 KHÁC HẲN bản gián tiếp — đây là lý do phải tách B03-TT / B03-GT.
them("B03-TT", "TT200", {
    "01": ("LCTT_TT_THU_BAN_HANG", "LCTT(TT) — Tiền thu từ bán hàng, cung cấp dịch vụ"),
    "02": ("LCTT_TT_CHI_NGUOI_CUNG_CAP", "LCTT(TT) — Tiền chi trả người cung cấp hàng hoá, dịch vụ"),
    "03": ("LCTT_TT_CHI_NGUOI_LAO_DONG", "LCTT(TT) — Tiền chi trả cho người lao động"),
    "04": ("LCTT_CHI_PHI_DI_VAY_DA_TRA", "LCTT — Tiền lãi vay đã trả"),
    "05": ("LCTT_THUE_TNDN_DA_NOP", "LCTT — Thuế TNDN đã nộp"),
    "06": ("LCTT_TIEN_THU_KHAC_HDKD", "LCTT — Tiền thu khác từ hoạt động kinh doanh"),
    "07": ("LCTT_TIEN_CHI_KHAC_HDKD", "LCTT — Tiền chi khác cho hoạt động kinh doanh"),
    "20": ("LCTT_THUAN_HDKD", "Lưu chuyển tiền thuần từ hoạt động kinh doanh"),
    "21": ("LCTT_CHI_MUA_TSCD", "LCTT — Tiền chi mua sắm, xây dựng TSCĐ"),
    "22": ("LCTT_THU_THANH_LY_TSCD", "LCTT — Tiền thu thanh lý, nhượng bán TSCĐ"),
    "23": ("LCTT_CHI_CHO_VAY", "LCTT — Tiền chi cho vay, mua công cụ nợ"),
    "24": ("LCTT_THU_HOI_CHO_VAY", "LCTT — Tiền thu hồi cho vay, bán lại công cụ nợ"),
    "27": ("LCTT_THU_LAI_CO_TUC", "LCTT — Tiền thu lãi cho vay, cổ tức, lợi nhuận được chia"),
    "30": ("LCTT_THUAN_HDDT", "Lưu chuyển tiền thuần từ hoạt động đầu tư"),
    "33": ("LCTT_THU_DI_VAY", "LCTT — Tiền thu từ đi vay"),
    "34": ("LCTT_TRA_NO_GOC_VAY", "LCTT — Tiền trả nợ gốc vay"),
    "36": ("LCTT_CO_TUC_DA_TRA", "LCTT — Cổ tức, lợi nhuận đã trả cho chủ sở hữu"),
    "40": ("LCTT_THUAN_HDTC", "Lưu chuyển tiền thuần từ hoạt động tài chính"),
    "50": ("LCTT_THUAN_TRONG_KY", "Lưu chuyển tiền thuần trong kỳ"),
    "60": ("TIEN_DAU_KY", "Tiền và tương đương tiền đầu kỳ"),
    "61": ("LCTT_ANH_HUONG_TY_GIA", "LCTT — Ảnh hưởng thay đổi tỷ giá quy đổi ngoại tệ"),
    "70": ("TIEN_CUOI_KY", "Tiền và tương đương tiền cuối kỳ"),
})

# Cặp biết chắc là rác OCR — không đặt tên, ghi lý do để người soát xác nhận.
# BCTN da bi loai khoi nguon trich (xem nguon_dung_de_trich), nen ma 252 — von chi
# xuat hien trong bang rut gon tu danh lai so cua BCTN 2022 — khong con nua.
RAC = {}

# Mã bị bãi bỏ khi đổi chế độ. Tool KHÔNG được lặng lẽ thay bằng mã gần giống
# (bài học EBITDA) — phải báo rõ và gợi ý, để người hỏi tự quyết.
THAY_THE = {
    "VON_CHU_SO_HUU_M410": ("VON_CHU_SO_HUU_M400",
                            "TT99 (từ 2026) bỏ mã 410; mã 400 là tổng vốn chủ sở hữu"),
}


def main():
    vao = os.path.join(RA, "ma_chi_tieu_tho.csv")
    ra = os.path.join(RA, "ma_chi_tieu_de_xuat.csv")
    dong = list(csv.DictReader(io.open(vao, encoding="utf-8-sig")))

    chua = []
    dem = collections.Counter()
    with io.open(ra, "w", encoding="utf-8-sig", newline="") as f:
        w = csv.writer(f)
        w.writerow(["che_do", "mau_bieu", "ma", "ten_tren_bao_cao", "so_lan",
                    "so_bien_the", "che_do_suy_doan", "ma_noi_bo", "ten_chuan",
                    "thay_the_bang", "ly_do_thay_the", "ghi_chu"])
        for d in dong:
            cd = d["che_do"]
            khoa = (d["mau_bieu"], cd, d["ma"])
            mnb, tc = DE_XUAT.get(khoa, ("", ""))
            gc = RAC.get(khoa, "")
            if d.get("che_do_suy_doan"):
                gc = (gc + " · che_do suy tu nam, can xac nhan").strip(" ·")
            if not mnb and not gc:
                gc = "CHUA DAT TEN — can nguoi quyet"
                chua.append((d["che_do"], d["mau_bieu"], d["ma"],
                             d["ten_tren_bao_cao"], d["so_lan"]))
            dem["co_ten" if mnb else "chua"] += 1
            tt, lyd = THAY_THE.get(mnb, ("", ""))
            w.writerow([d["che_do"], d["mau_bieu"], d["ma"], d["ten_tren_bao_cao"],
                        d["so_lan"], d["so_bien_the"], d.get("che_do_suy_doan", ""),
                        mnb, tc, tt, lyd, gc])

    print("Tong cap          : %d" % len(dong))
    print("Da de xuat ten    : %d" % dem["co_ten"])
    print("Chua dat ten      : %d" % dem["chua"])
    n = len(set(v[0] for v in DE_XUAT.values()))
    print("So ma_noi_bo rieng: %d   (enum se co tung nay gia tri)" % n)
    if chua:
        print()
        print("  CAN NGUOI QUYET:")
        for x in chua:
            print("    %-7s %-7s %-4s %-46s %s lan" % (x[0], x[1], x[2], x[3][:46], x[4]))
    print()
    print("Da ghi: devtools/ra/ma_chi_tieu_de_xuat.csv")


if __name__ == "__main__":
    main()
