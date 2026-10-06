# -*- coding: utf-8 -*-
"""Sinh hai tệp migration Flyway từ bảng ánh xạ đã chốt.

    V1__create_table_financial_items.sql   lược đồ hai bảng
    V2__seed_data_financial_items.sql            123 + 250 câu INSERT, mỗi bản ghi một câu

Hai bảng chứ không một, vì chúng là hai thứ khác nhau:
    financial_items          BỘ TỪ VỰNG — 123 mã nội bộ, mỗi mã một dòng
    financial_item_mappings  CÁCH DỊCH  — 250 ánh xạ (chế độ, mẫu biểu, mã) → mã nội bộ

Tách ra thì chi_tieu mới đặt được khoá ngoại sang financial_items: gõ sai mã nội bộ
là DB TỪ CHỐI GHI, không phải phát hiện sau.

Chạy:  python -X utf8 devtools/sinh_sql_financial_items.py
"""
import io
import os
import csv
import collections
import sys

GOC = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
VAO = os.path.join(GOC, "devtools", "ra", "ma_chi_tieu_de_xuat.csv")
DICH = os.path.join(GOC, "backend", "core-service", "src", "main",
                    "resources", "db", "migration")

TEN_V1 = "V1__create_table_financial_items.sql"
TEN_V2 = "V2__seed_data_financial_items.sql"

# ---------------------------------------------------------------------------
# Năm mã có tên khác nhau giữa TT200 và TT99. Tên chuẩn phải khớp với tên mã nội
# bộ đã đặt, không lấy theo tần suất — nếu không thì mã CHI_PHI_DI_VAY lại hiện ra
# chữ "chi phí lãi vay", đọc rất khó hiểu.
# ---------------------------------------------------------------------------
TEN_CHUAN_CHOT = {
    "CHI_PHI_DI_VAY": "Trong đó: chi phí đi vay",
    "CHI_PHI_CHO_PHAN_BO_NGAN_HAN": "Chi phí chờ phân bổ ngắn hạn",
    "CHI_PHI_CHO_PHAN_BO_DAI_HAN": "Chi phí chờ phân bổ dài hạn",
    "DOANH_THU_CHUA_THUC_HIEN_NH": "Doanh thu chưa thực hiện ngắn hạn",
    "THANG_DU_VON": "Thặng dư vốn",
    # Hai mã LCTT dưới đây không bị lệch giữa hai chế độ trong bảng ánh xạ (vì dùng
    # chung một bảng tên), nhưng tên cũ vẫn là "lãi vay" trong khi mã đã đổi sang
    # "đi vay" theo quyết định 29/9. Chốt lại cho khớp.
    "LCTT_CHI_PHI_DI_VAY": "LCTT — Chi phí đi vay",
    "LCTT_CHI_PHI_DI_VAY_DA_TRA": "LCTT — Chi phí đi vay đã trả",
    "LCTT_TANG_GIAM_CHI_PHI_CHO_PHAN_BO": "LCTT — Tăng/giảm chi phí chờ phân bổ",
}

# Hai mã này nằm trong B03 (báo cáo thời kỳ) nhưng bản thân chúng là số TẠI MỘT
# THỜI ĐIỂM — số dư tiền đầu kỳ và cuối kỳ. Quy tắc "B01 là thời điểm" không bắt được.
LOAI_SO_CHOT = {
    "TIEN_DAU_KY": ("thoi_diem", "Số dư tại ĐẦU kỳ — dùng tu_ngay làm mốc"),
    "TIEN_CUOI_KY": ("thoi_diem", "Số dư tại CUỐI kỳ — dùng den_ngay làm mốc; "
                                  "phải bằng TIEN_VA_TUONG_DUONG_TIEN cùng ngày"),
}

DDL = '''-- ---------------------------------------------------------------------------
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
'''


def thoat(s):
    """Nhân đôi dấu nháy đơn — cách thoát chuỗi chuẩn của SQL."""
    return s.replace("'", "''")


def o(s):
    return "NULL" if not s else "'" + thoat(s) + "'"


def loai_so(ma_noi_bo, mau_bieu):
    if ma_noi_bo in LOAI_SO_CHOT:
        return LOAI_SO_CHOT[ma_noi_bo][0]
    # Bảng cân đối đo MỘT thời điểm; kết quả kinh doanh và lưu chuyển tiền đo GIỮA
    # hai mốc (xem logs/luong-hoi-dap mục 1.9).
    return "thoi_diem" if mau_bieu == "B01" else "thoi_ky"


# Danh sách chốt sau khi đối chiếu dữ liệu thật (xem V3). Quy tắc theo tên đã sai
# hai chiều: quơ nhầm DU_PHONG_PHAI_TRA_NH (là khoản nợ, luôn dương) và bỏ sót
# toàn bộ dòng chi tiền của LCTT.
DAU_AM = {
    # tài khoản điều chỉnh giảm
    "BDS_DAU_TU_HAO_MON", "DU_PHONG_GIAM_GIA_HTK", "DU_PHONG_PHAI_THU_KHO_DOI",
    "TSCD_HUU_HINH_HAO_MON", "TSCD_VO_HINH_HAO_MON",
    # dòng chi tiền của lưu chuyển tiền tệ — quy ước in trong ngoặc
    "LCTT_TIEN_CHI_KHAC_HDKD", "LCTT_THUE_TNDN_DA_NOP", "LCTT_CO_TUC_DA_TRA",
    "LCTT_TRA_NO_GOC_VAY", "LCTT_CHI_MUA_TSCD", "LCTT_CHI_PHI_DI_VAY_DA_TRA",
    "LCTT_CHI_CHO_VAY", "LCTT_TT_CHI_NGUOI_CUNG_CAP", "LCTT_TT_CHI_NGUOI_LAO_DONG",
}


def dau_am(ma_noi_bo):
    return int(ma_noi_bo in DAU_AM)


def gom_tu_vung(dong):
    """Gộp 250 dòng ánh xạ thành 123 dòng từ vựng."""
    tv = {}
    for d in dong:
        m = d["ma_noi_bo"]
        if m not in tv:
            tv[m] = {
                "ma_noi_bo": m,
                "ten_chuan": TEN_CHUAN_CHOT.get(m, d["ten_chuan"]),
                "loai_so": loai_so(m, d["mau_bieu"]),
                "dau_am_tu_nhien": dau_am(m),
                "thay_the_bang": d["thay_the_bang"],
                "ly_do_thay_the": d["ly_do_thay_the"],
                "ghi_chu": LOAI_SO_CHOT.get(m, ("", ""))[1],
            }
        elif d["thay_the_bang"] and not tv[m]["thay_the_bang"]:
            tv[m]["thay_the_bang"] = d["thay_the_bang"]
            tv[m]["ly_do_thay_the"] = d["ly_do_thay_the"]
    return tv


def main():
    dong = list(csv.DictReader(io.open(VAO, encoding="utf-8-sig")))
    assert dong, "khong doc duoc " + VAO
    os.makedirs(DICH, exist_ok=True)
    da_co = [n for n in (TEN_V1, TEN_V2) if os.path.exists(os.path.join(DICH, n))]
    if da_co and "--force" not in sys.argv:
        print("DUNG: %s da ton tai." % ", ".join(da_co))
        print("Flyway kiem tong kiem — ghi de tep DA CHAY se lam ung dung khong khoi dong.")
        print("Sua thi viet migration moi (V4, V5...). Chac chan muon ghi de: them --force")
        return
    tv = gom_tu_vung(dong)

    with io.open(os.path.join(DICH, TEN_V1), "w",
                 encoding="utf-8", newline="\n") as f:
        f.write(DDL)

    with io.open(os.path.join(DICH, TEN_V2), "w",
                 encoding="utf-8", newline="\n") as f:
        f.write("-- ---------------------------------------------------------------"
                "------------\n")
        f.write("-- Dữ liệu từ điển mã chỉ tiêu — mỗi bản ghi một câu INSERT.\n")
        f.write("--   financial_items          : %3d bản ghi\n" % len(tv))
        f.write("--   financial_item_mappings  : %3d bản ghi\n" % len(dong))
        f.write("--\n")
        f.write("-- Sinh bởi devtools/sinh_sql_financial_items.py từ\n")
        f.write("--   devtools/ra/ma_chi_tieu_de_xuat.csv\n")
        f.write("-- quét từ 130 tài liệu BCTC gốc của CTR (2019-2026), 330 trang\n")
        f.write("-- B01/B02/B03. Phủ 7.685/7.685 lần xuất hiện, 0 khoá trùng.\n")
        f.write("--\n")
        f.write("-- SAU KHI CHẠY, tệp này là nguồn sự thật. Thêm/sửa mã thì viết\n")
        f.write("-- migration mới (V3, V4...), ĐỪNG sửa tệp này — Flyway kiểm tổng kiểm.\n")
        f.write("-- ---------------------------------------------------------------"
                "------------\n\n")

        # --- financial_items -------------------------------------------------
        # Dòng không tự tham chiếu phải vào trước, vì thay_the_bang là khoá ngoại
        # trỏ về chính bảng này.
        f.write("-- ===================== financial_items (%d) ====================="
                "\n\n" % len(tv))
        for v in sorted(tv.values(), key=lambda x: (bool(x["thay_the_bang"]),
                                                    x["ma_noi_bo"])):
            f.write(
                "INSERT INTO financial_items "
                "(ma_noi_bo, ten_chuan, loai_so, dau_am_tu_nhien, "
                "thay_the_bang, ly_do_thay_the, ghi_chu) VALUES "
                "(%s, %s, %s, %d, %s, %s, %s);\n"
                % (o(v["ma_noi_bo"]), o(v["ten_chuan"]), o(v["loai_so"]),
                   v["dau_am_tu_nhien"], o(v["thay_the_bang"]),
                   o(v["ly_do_thay_the"]), o(v["ghi_chu"])))

        # --- financial_item_mappings -----------------------------------------
        f.write("\n\n-- ================ financial_item_mappings (%d) ================\n"
                % len(dong))
        mau_truoc = None
        for d in dong:
            khoa = (d["che_do"], d["mau_bieu"])
            if khoa != mau_truoc:
                f.write("\n-- ----- %s / %s -----\n" % (d["che_do"], d["mau_bieu"]))
                mau_truoc = khoa
            f.write(
                "INSERT INTO financial_item_mappings "
                "(che_do, mau_bieu, ma, ma_noi_bo, ten_tren_bao_cao, "
                "so_lan_gap, so_bien_the, che_do_suy_doan, ghi_chu) VALUES "
                "(%s, %s, %s, %s, %s, %s, %s, %d, %s);\n"
                % (o(d["che_do"]), o(d["mau_bieu"]), o(d["ma"]), o(d["ma_noi_bo"]),
                   o(d["ten_tren_bao_cao"]), d["so_lan"], d["so_bien_the"],
                   1 if d.get("che_do_suy_doan") else 0, o(d["ghi_chu"])))

    # ------------------------------------------------------------------ tóm tắt
    print("Da ghi:")
    print("  %s" % os.path.join("...", "db", "migration", TEN_V1))
    print("  %s   (%d + %d ban ghi)"
          % (os.path.join("...", "db", "migration", TEN_V2), len(tv), len(dong)))
    print()
    c = collections.Counter(v["loai_so"] for v in tv.values())
    print("  financial_items : %d ma" % len(tv))
    print("     loai_so       : %d thoi_diem · %d thoi_ky"
          % (c["thoi_diem"], c["thoi_ky"]))
    print("     dau_am        : %d" % sum(v["dau_am_tu_nhien"] for v in tv.values()))
    print("     thay_the_bang : %d" % sum(1 for v in tv.values() if v["thay_the_bang"]))
    print("  financial_item_mappings : %d anh xa" % len(dong))
    thieu = {d["ma_noi_bo"] for d in dong} - set(tv)
    print("  Ma trong mappings KHONG co trong items (se vi pham khoa ngoai): %d"
          % len(thieu))
    tt = {v["thay_the_bang"] for v in tv.values() if v["thay_the_bang"]} - set(tv)
    print("  thay_the_bang tro toi ma KHONG ton tai: %d" % len(tt))


if __name__ == "__main__":
    main()
