# -*- coding: utf-8 -*-
"""Quét 278 tài liệu, rút metadata cho bảng tai_lieu.

Mọi cột SINH TẤT ĐỊNH từ đường dẫn và nội dung — chạy lại ra y hệt, và dùng lại
được cho tài liệu admin đẩy lên sau này.

Hai loại ngày, đừng lẫn:
    ngay_ket_thuc_ky  báo cáo tài chính / thường niên — mốc chốt số liệu
    ngay_ban_hanh     công văn, nghị quyết, tờ trình  — ngày ký

Ra:  devtools/ra/tai_lieu.csv
"""
import io
import os
import re
import csv
import glob
import hashlib
import unicodedata
import collections

GOC = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
OCR = os.path.join(GOC, "data", "ocr", "CTR")
RA = os.path.join(GOC, "devtools", "ra")

# --------------------------------------------------------------- phân loại
# Khoá là "thư_mục_gốc/thư_mục_con" (bỏ phần năm ở giữa).
LOAI = {
    "01_bao_cao_tai_chinh/hop_nhat": ("bctc", "Báo cáo tài chính"),
    "01_bao_cao_tai_chinh/rieng": ("bctc", "Báo cáo tài chính"),
    "01_bao_cao_tai_chinh/giai_trinh": ("giai_trinh", "Công văn giải trình"),
    "02_bao_cao_thuong_nien/thuong_nien": ("bctn", "Báo cáo thường niên"),
    "02_bao_cao_thuong_nien/phat_trien_ben_vung": ("bao_cao_pt", "Báo cáo phát triển bền vững"),
    "03_dai_hoi_dong_co_dong/to_trinh": ("to_trinh", "Tờ trình ĐHĐCĐ"),
    "03_dai_hoi_dong_co_dong/nghi_quyet_bien_ban": ("nghi_quyet", "Nghị quyết / biên bản ĐHĐCĐ"),
    "03_dai_hoi_dong_co_dong/bao_cao": ("bao_cao_dhdcd", "Báo cáo trình ĐHĐCĐ"),
    "03_dai_hoi_dong_co_dong/tai_lieu_hop": ("tai_lieu_hop", "Tài liệu họp ĐHĐCĐ"),
    "04_cong_bo_thong_tin/co_tuc": ("cbtt_co_tuc", "Công bố thông tin — cổ tức"),
    "04_cong_bo_thong_tin/phat_hanh_tang_von": ("cbtt_phat_hanh", "Công bố thông tin — phát hành, tăng vốn"),
    "04_cong_bo_thong_tin/nghi_quyet_dhdcd": ("nghi_quyet", "Nghị quyết ĐHĐCĐ"),
    "04_cong_bo_thong_tin/dinh_chinh": ("dinh_chinh", "Đính chính / trình bày lại số liệu"),
    "04_cong_bo_thong_tin/ke_hoach_kinh_doanh": ("ke_hoach", "Kế hoạch kinh doanh"),
    "04_cong_bo_thong_tin/bctn_bctc": ("cbtt_khac", "Công bố thông tin"),
    "04_cong_bo_thong_tin/dau_tu_thoai_von": ("cbtt_khac", "Công bố thông tin — đầu tư, thoái vốn"),
    "04_cong_bo_thong_tin/lay_y_kien_co_dong": ("cbtt_khac", "Lấy ý kiến cổ đông"),
    "04_cong_bo_thong_tin/the_chap_vay_von": ("cbtt_khac", "Công bố thông tin — thế chấp, vay vốn"),
}
# Chỉ những loại này mới có kỳ báo cáo và phạm vi hợp nhất/riêng.
CO_KY = {"bctc", "bctn", "dinh_chinh"}

TT99 = re.compile(r"Th[ôo]ng t[ưu]\s*(?:s[ốo]\s*)?99[/\s]", re.I)
TT200 = re.compile(r"Th[ôo]ng t[ưu]\s*(?:s[ốo]\s*)?200[/\s]", re.I)

# Ngày CHỐT SỐ LIỆU của báo cáo.
NGAY_KY = [
    re.compile(r"k[ếe]t\s+th[úu]c\s+ng[àa]y\s*:?\s*(\d{1,2})[/.-](\d{1,2})[/.-](\d{4})", re.I),
    re.compile(r"[Tt][ạa]i\s+ng[àa]y\s*:?\s*(\d{1,2})\s*th[áa]ng\s*(\d{1,2})\s*n[ăa]m\s*(\d{4})", re.I),
    re.compile(r"[Tt][ạa]i\s+ng[àa]y\s*:?\s*(\d{1,2})[/.-](\d{1,2})[/.-](\d{4})", re.I),
    re.compile(r"\|\s*(\d{1,2})[/.-](\d{1,2})[/.-](\d{4})\s*(?:VND|\|)", re.I),
]
# Ngày KÝ văn bản: "Hà Nội, ngày 29 tháng 10 năm 2019".
NGAY_BH = [
    re.compile(r"ng[àa]y\s*(\d{1,2})\s*th[áa]ng\s*(\d{1,2})\s*n[ăa]m\s*(\d{4})", re.I),
    re.compile(r"ng[àa]y\s*(\d{1,2})[/.-](\d{1,2})[/.-](\d{4})", re.I),
]

HOP_NHAT = re.compile(r"h[ợo]p\s+nh[ấa]t|consolidated", re.I)
RIENG = re.compile(r"\bri[êe]ng\b|c[ôo]ng\s+ty\s+m[ẹe]|separate", re.I)

# Gợi ý kỳ từ tên tệp — đáng tin hơn suy từ ngày, vì 30/06 vừa có thể là "Quý 2"
# vừa có thể là "bán niên": hai loại báo cáo khác nhau, cùng ngày chốt.
GOI_Y_KY = [
    (re.compile(r"ban[-_\s]?nien|giua[-_\s]?nien[-_\s]?do", re.I), "ban_nien"),
    # Không dùng \b ở đầu: \b coi dấu gạch dưới là ký tự từ, nên "BCTC_HN_Q4" không
    # có ranh giới giữa "_" và "Q". Dùng lookaround theo chữ-số để bắt được cả
    # "Quy-3", "QII.", "_Q4.", "-Q1-".
    (re.compile(r"(?<![A-Za-z0-9])q(?:uy)?[-_.\s]*(?:1|I(?![IV]))(?![A-Za-z0-9])", re.I), "Q1"),
    (re.compile(r"(?<![A-Za-z0-9])q(?:uy)?[-_.\s]*(?:2|II(?!I))(?![A-Za-z0-9])", re.I), "Q2"),
    (re.compile(r"(?<![A-Za-z0-9])q(?:uy)?[-_.\s]*(?:3|III)(?![A-Za-z0-9])", re.I), "Q3"),
    (re.compile(r"(?<![A-Za-z0-9])q(?:uy)?[-_.\s]*(?:4|IV)(?![A-Za-z0-9])", re.I), "Q4"),
]
KY_THEO_NGAY = {(3, 31): "Q1", (6, 30): "ban_nien", (9, 30): "Q3", (12, 31): "nam"}
TEN_KY = {"nam": "năm", "ban_nien": "bán niên",
          "Q1": "quý 1", "Q2": "quý 2", "Q3": "quý 3", "Q4": "quý 4"}


def doc(p):
    return io.open(p, encoding="utf-8", errors="replace").read()


def khong_dau(s):
    s = unicodedata.normalize("NFD", s)
    s = "".join(c for c in s if unicodedata.category(c) != "Mn")
    return s.replace("đ", "d").replace("Đ", "D")


def hop_le(y, m, d):
    return 1 <= d <= 31 and 1 <= m <= 12 and 2015 <= y <= 2035


def tim_ngay(mau_list, noi_dung, nam_uu_tien):
    dem = collections.Counter()
    for mau in mau_list:
        for a, b, c in mau.findall(noi_dung):
            y, m, d = int(c), int(b), int(a)
            if hop_le(y, m, d):
                dem[(y, m, d)] += 1
    if not dem:
        return ""
    hop = [k for k in dem if k[0] == nam_uu_tien]
    y, m, d = max(hop or list(dem), key=lambda k: (dem[k], k))
    return "%04d-%02d-%02d" % (y, m, d)


def nhan_pham_vi(duong_dan, tieu_de):
    r = "/" + duong_dan.replace(os.sep, "/") + "/"
    if "/hop_nhat/" in r:
        return "hop_nhat", "duong_dan"
    if "/rieng/" in r:
        return "rieng", "duong_dan"
    if HOP_NHAT.search(tieu_de):
        return "hop_nhat", "tieu_de"
    if RIENG.search(tieu_de):
        return "rieng", "tieu_de"
    return "", ""


SOAT_XET = re.compile(r"so[áa]t\s*x[ée]t", re.I)
QUY_2 = re.compile(r"qu[ýy]\s*(?:2|II(?!I))\b", re.I)


def nhan_ky(ten_thu_muc, duong_dan, ngay, noi_dung=""):
    goc = khong_dau(ten_thu_muc + " " + duong_dan)
    for mau, k in GOI_Y_KY:
        if mau.search(goc):
            return k, "ten_tep"
    if ngay:
        _, m, d = (int(x) for x in ngay.split("-"))
        k = KY_THEO_NGAY.get((m, d))
        if k == "ban_nien" and noi_dung:
            # Ngày 30/06 vừa có thể là báo cáo bán niên (soát xét, kỳ 01/01-30/06)
            # vừa có thể là báo cáo quý 2. Nội dung phân biệt được.
            if SOAT_XET.search(noi_dung):
                return "ban_nien", "soat_xet"
            if QUY_2.search(noi_dung):
                return "Q2", "noi_dung"
        if k:
            return k, "ngay"
    return "", ""


def sinh_id(b):
    """ID tất định và BỀN: phần đuôi là băm của đường dẫn tài liệu.

    Không dùng số thứ tự trong cụm, vì thêm tài liệu mới sẽ làm số thứ tự của tài
    liệu cũ dịch đi — mọi trích dẫn đã lưu trong Qdrant và chi_tieu sẽ trỏ sai.
    Băm chỉ phụ thuộc vào chính tài liệu đó, nên ID không bao giờ đổi.
    """
    p = ["CTR", b["loai"].upper().replace("_", "")]
    if b["pham_vi"]:
        p.append("HN" if b["pham_vi"] == "hop_nhat" else "R")
    ngay = b["ngay_ket_thuc_ky"] or b["ngay_ban_hanh"]
    p.append(ngay.replace("-", "") if ngay else "NA%d" % b["nam"])
    if b["ky"]:
        p.append(b["ky"].upper())
    bam = hashlib.sha1(b["duong_dan"].encode("utf-8")).hexdigest()[:6]
    p.append(bam)
    return "-".join(p)


def main():
    os.makedirs(RA, exist_ok=True)
    thu_muc = sorted(os.path.dirname(p)
                     for p in glob.glob(OCR + "/**/_TOAN_VAN.md", recursive=True))
    ban = []
    chua_biet = collections.Counter()

    for tm in thu_muc:
        r = os.path.relpath(tm, OCR).replace(os.sep, "/")
        phan = [x for x in r.split("/")[:-1] if not re.fullmatch(r"20\d{2}", x)]
        khoa_loai = "/".join(phan)
        loai, ten_loai = LOAI.get(khoa_loai, ("khac", "Tài liệu"))
        if khoa_loai not in LOAI:
            chua_biet[khoa_loai] += 1

        m = re.search(r"/(20\d{2})/", "/" + r + "/")
        nam = int(m.group(1)) if m else 0

        tep = sorted(glob.glob(os.path.join(tm, "trang_*.md")))
        dau = "\n".join(doc(p) for p in tep[:6])
        tieu_de = "\n".join(l for l in dau.split("\n") if l.startswith("#"))
        toan = doc(os.path.join(tm, "_TOAN_VAN.md"))

        b = {"loai": loai, "ten_loai": ten_loai, "nam": nam,
             "duong_dan": r, "ten_thu_muc": os.path.basename(tm),
             "so_trang": len(tep)}

        if loai == "bctn":
            # Báo cáo thường niên năm N luôn chốt 31/12/N — theo định nghĩa, không
            # phải đoán. Không tìm trong văn bản vì mấy trang đầu là phần giới thiệu,
            # dễ bắt nhầm ngày khác (đã gặp: BCTN 2022 bắt ra 30/12/2022).
            b["ngay_ket_thuc_ky"] = "%04d-12-31" % nam if nam else ""
            b["ngay_ban_hanh"] = ""
            # BCTN không phân hợp nhất/riêng — nó trình bày cả hai. Để trống là ĐÚNG.
            b["pham_vi"], b["nguon_pham_vi"] = "", ""
            b["ky"], b["nguon_ky"] = ("nam", "dinh_nghia") if nam else ("", "")
        elif loai in CO_KY:
            b["ngay_ket_thuc_ky"] = tim_ngay(NGAY_KY, dau, nam)
            b["ngay_ban_hanh"] = ""
            b["pham_vi"], b["nguon_pham_vi"] = nhan_pham_vi(tm, tieu_de)
            b["ky"], b["nguon_ky"] = nhan_ky(b["ten_thu_muc"], r, b["ngay_ket_thuc_ky"], dau)
        else:
            b["ngay_ket_thuc_ky"] = ""
            # lấy 2 trang đầu thôi: ngày ký nằm ở cuối văn bản ngắn, đầu văn bản dài
            b["ngay_ban_hanh"] = tim_ngay(NGAY_BH, "\n".join(doc(p) for p in tep), nam)
            b["pham_vi"], b["nguon_pham_vi"] = "", ""
            b["ky"], b["nguon_ky"] = "", ""

        b["che_do"] = ("TT99" if TT99.search(toan)
                       else "TT200" if TT200.search(toan) else "")
        ban.append(b)

    # --- nhận bản trùng kỳ: chỉ có nghĩa với báo cáo, không với công văn ---
    cum = collections.defaultdict(list)
    for b in ban:
        if b["loai"] in CO_KY and b["ngay_ket_thuc_ky"]:
            cum[(b["loai"], b["pham_vi"], b["ngay_ket_thuc_ky"], b["ky"])].append(b)
        else:
            b["stt_trung"] = 0
            b["so_ban_cung_ky"] = 1
            b["la_ban_chinh"] = 1
    for ds in cum.values():
        ds.sort(key=lambda b: (-b["so_trang"], b["ten_thu_muc"]))
        for i, b in enumerate(ds):
            b["stt_trung"] = i + 1 if len(ds) > 1 else 0
            b["so_ban_cung_ky"] = len(ds)
            b["la_ban_chinh"] = 1 if i == 0 else 0     # bản đầy đủ nhất làm bản chính

    for b in ban:
        b["tai_lieu_id"] = sinh_id(b)
        ten = b["ten_loai"]
        if b["pham_vi"]:
            ten += " hợp nhất" if b["pham_vi"] == "hop_nhat" else " riêng"
        if b["ky"]:
            ten += " " + TEN_KY.get(b["ky"], b["ky"])
        if b["nam"]:
            ten += " %d" % b["nam"]
        if b["so_ban_cung_ky"] > 1:
            ten += " (bản %d/%d)" % (b["stt_trung"], b["so_ban_cung_ky"])
        elif b["loai"] not in CO_KY and b["ngay_ban_hanh"]:
            ten += " — ký ngày %s" % "/".join(reversed(b["ngay_ban_hanh"].split("-")))
        b["ten_hien_thi"] = ten

    cot = ["tai_lieu_id", "ten_hien_thi", "loai", "nam", "ky", "ngay_ket_thuc_ky",
           "ngay_ban_hanh", "pham_vi", "che_do", "so_trang", "la_ban_chinh",
           "so_ban_cung_ky", "duong_dan", "ten_thu_muc", "nguon_pham_vi", "nguon_ky"]
    with io.open(os.path.join(RA, "tai_lieu.csv"), "w",
                 encoding="utf-8-sig", newline="") as f:
        w = csv.DictWriter(f, fieldnames=cot, extrasaction="ignore")
        w.writeheader()
        for b in sorted(ban, key=lambda b: (b["loai"], b["nam"], b["tai_lieu_id"])):
            w.writerow(b)

    # ------------------------------------------------------------------- đo
    n = len(ban)
    bao_cao = [b for b in ban if b["loai"] in CO_KY]
    van_ban = [b for b in ban if b["loai"] not in CO_KY]
    print("Tai lieu : %d   (bao cao co ky: %d · van ban: %d)"
          % (n, len(bao_cao), len(van_ban)))
    if chua_biet:
        print("  CANH BAO — thu muc chua phan loai:", dict(chua_biet))
    print()
    print("  BAO CAO (bctc, bctn, dinh_chinh)")
    for c in ("ngay_ket_thuc_ky", "ky", "che_do"):
        print("     %-18s %3d / %d" % (c, sum(1 for b in bao_cao if b[c]), len(bao_cao)))
    co_pv = [b for b in bao_cao if b["loai"] != "bctn"]     # BCTN khong phan pham vi
    print("     %-18s %3d / %d  (bo BCTN — khong phan hop nhat/rieng)"
          % ("pham_vi", sum(1 for b in co_pv if b["pham_vi"]), len(co_pv)))
    chi_bctc = [b for b in bao_cao if b["loai"] == "bctc"]
    print("     %-18s %3d / %d"
          % ("bctc du ca 3 cot", sum(1 for b in chi_bctc
                                     if b["ngay_ket_thuc_ky"] and b["ky"] and b["pham_vi"]),
             len(chi_bctc)))
    print("  VAN BAN (cong van, nghi quyet, to trinh...)")
    print("     %-18s %3d / %d"
          % ("ngay_ban_hanh", sum(1 for b in van_ban if b["ngay_ban_hanh"]), len(van_ban)))
    print()
    print("  ID bi trung          : %d" % (n - len({b["tai_lieu_id"] for b in ban})))
    nhieu = [v for v in cum.values() if len(v) > 1]
    print("  Cum nhieu ban cung ky: %d cum, %d tai lieu, giu %d ban chinh"
          % (len(nhieu), sum(len(v) for v in nhieu), len(nhieu)))
    print()
    print("  Theo loai:")
    for k, v in sorted(collections.Counter(b["loai"] for b in ban).items(),
                       key=lambda kv: -kv[1]):
        print("     %-16s %3d" % (k, v))
    print()
    print("Da ghi: devtools/ra/tai_lieu.csv")


if __name__ == "__main__":
    main()
