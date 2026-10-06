# -*- coding: utf-8 -*-
"""Sinh migration Flyway cho bảng documents.

    V4__create_table_documents.sql   lược đồ
    V5__seed_data_documents.sql            278 câu INSERT, mỗi tài liệu một câu

Chạy:  python -X utf8 devtools/sinh_sql_tai_lieu.py
"""
import io
import os
import csv
import sys
import collections

GOC = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
VAO = os.path.join(GOC, "devtools", "ra", "tai_lieu.csv")
DICH = os.path.join(GOC, "backend", "core-service", "src", "main",
                    "resources", "db", "migration")
TEN_V4 = "V4__create_table_documents.sql"
TEN_V5 = "V5__seed_data_documents.sql"

CO_KY = {"bctc", "bctn", "dinh_chinh"}

DDL = '''-- ---------------------------------------------------------------------------
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
'''


def thoat(s):
    return s.replace("'", "''")


def o(s):
    return "NULL" if s in (None, "") else "'" + thoat(str(s)) + "'"


def soat(b):
    """Dòng nào máy không chắc thì bật cờ, kèm lý do."""
    ly = []
    if b["loai"] in CO_KY:
        if not b["ngay_ket_thuc_ky"]:
            ly.append("thieu ngay_ket_thuc_ky")
        if not b["ky"]:
            ly.append("thieu ky")
        if b["loai"] != "bctn" and not b["pham_vi"]:
            ly.append("thieu pham_vi")
    elif not b["ngay_ban_hanh"]:
        ly.append("thieu ngay_ban_hanh")
    if b["nguon_pham_vi"] == "tieu_de":
        ly.append("pham_vi suy tu tieu de")
    # Suy kỳ từ 31/12, 31/03, 30/09 là KHÔNG THỂ SAI — chỉ 30/06 mới mơ hồ, vì
    # cùng ngày đó có thể là báo cáo bán niên (soát xét) hoặc báo cáo quý 2.
    if b["nguon_ky"] == "ngay" and b["ngay_ket_thuc_ky"].endswith("-06-30"):
        ly.append("ky 30/06 mo ho: ban_nien hay Q2")
    return (1 if ly else 0), " · ".join(ly)


def main():
    dong = list(csv.DictReader(io.open(VAO, encoding="utf-8-sig")))
    assert dong, "khong doc duoc " + VAO
    os.makedirs(DICH, exist_ok=True)
    da_co = [n for n in (TEN_V4, TEN_V5) if os.path.exists(os.path.join(DICH, n))]
    if da_co and "--force" not in sys.argv:
        print("DUNG: %s da ton tai." % ", ".join(da_co))
        print("Flyway kiem tong kiem — ghi de tep DA CHAY se lam ung dung khong khoi dong.")
        print("Chac chan muon ghi de: them --force")
        return

    with io.open(os.path.join(DICH, TEN_V4), "w", encoding="utf-8", newline="\n") as f:
        f.write(DDL)

    with io.open(os.path.join(DICH, TEN_V5), "w", encoding="utf-8", newline="\n") as f:
        f.write("-- ------------------------------------------------------------------"
                "---------\n")
        f.write("-- Dữ liệu bảng documents — %d tài liệu, mỗi tài liệu một câu INSERT.\n"
                % len(dong))
        f.write("--\n")
        f.write("-- Sinh bởi devtools/sinh_sql_tai_lieu.py từ devtools/ra/tai_lieu.csv,\n")
        f.write("-- vốn quét từ data/ocr/CTR. Mọi cột suy TẤT ĐỊNH từ đường dẫn và nội dung.\n")
        f.write("--\n")
        f.write("-- Cờ can_soat = 1 đánh dấu dòng máy không nhận đủ metadata. Riêng 90 báo\n")
        f.write("-- cáo tài chính thì đủ cả ngay_ket_thuc_ky, ky, pham_vi (90/90).\n")
        f.write("-- ------------------------------------------------------------------"
                "---------\n\n")
        f.write('''-- --- SỬA COLLATION TRƯỚC KHI NẠP ---------------------------------------
-- V4 tạo bảng với utf8mb4_unicode_ci cho mọi cột. Đó là collation cho VĂN BẢN:
-- nó bỏ qua cả dấu lẫn hoa/thường. Với hai đường dẫn có thật trong kho:
--     01_bao_cao_tai_chinh/2022/rieng/BCTC-rieng
--     01_bao_cao_tai_chinh/2022/rieng/BCTC-riêng
-- MySQL coi là MỘT, nên khoá duy nhất uk_documents_duong_dan báo trùng.
--
-- Đường dẫn và mã tài liệu là ĐỊNH DANH, phải so khớp chính xác từng byte.
-- utf8mb4_bin làm đúng việc đó. Các cột văn bản khác giữ nguyên _unicode_ci để
-- tìm kiếm tiếng Việt vẫn bỏ dấu được.
ALTER TABLE documents
    MODIFY tai_lieu_id VARCHAR(64)  COLLATE utf8mb4_bin NOT NULL,
    MODIFY duong_dan   VARCHAR(255) COLLATE utf8mb4_bin NOT NULL
        COMMENT 'Duong dan thu muc OCR, tuong doi tu data/ocr/CTR';

-- Dọn sạch nếu lần chạy trước đã nạp được một phần rồi mới gãy.
DELETE FROM documents;


''')

        loai_truoc = None
        dem_soat = 0
        for b in sorted(dong, key=lambda b: (b["loai"], b["nam"], b["tai_lieu_id"])):
            if b["loai"] != loai_truoc:
                f.write("\n-- ===== %s =====\n" % b["loai"])
                loai_truoc = b["loai"]
            cs, gc = soat(b)
            dem_soat += cs
            f.write(
                "INSERT INTO documents "
                "(tai_lieu_id, ten_hien_thi, loai, nam, ky, ngay_ket_thuc_ky, "
                "ngay_ban_hanh, pham_vi, che_do, tong_so_trang, la_ban_chinh, "
                "so_ban_cung_ky, duong_dan, can_soat, ghi_chu) VALUES "
                "(%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %d, %s);\n"
                % (o(b["tai_lieu_id"]), o(b["ten_hien_thi"]), o(b["loai"]), b["nam"],
                   o(b["ky"]), o(b["ngay_ket_thuc_ky"]), o(b["ngay_ban_hanh"]),
                   o(b["pham_vi"]), o(b["che_do"]), b["so_trang"],
                   b["la_ban_chinh"], b["so_ban_cung_ky"], o(b["duong_dan"]),
                   cs, o(gc)))

    # --------------------------------------------------------------------- đo
    print("Da ghi:")
    print("  %s" % TEN_V4)
    print("  %s   (%d ban ghi)" % (TEN_V5, len(dong)))
    print()
    print("  ID rieng          : %d / %d" % (len({b["tai_lieu_id"] for b in dong}), len(dong)))
    print("  duong_dan rieng   : %d / %d" % (len({b["duong_dan"] for b in dong}), len(dong)))
    print("  can_soat = 1      : %d" % dem_soat)
    print("  la_ban_chinh = 0  : %d  (ban trung ky, khong dung trich dan)"
          % sum(1 for b in dong if b["la_ban_chinh"] == "0"))
    print()
    for k, v in sorted(collections.Counter(b["loai"] for b in dong).items(),
                       key=lambda kv: -kv[1]):
        print("     %-16s %3d" % (k, v))


if __name__ == "__main__":
    main()
