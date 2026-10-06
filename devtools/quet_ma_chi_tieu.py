# -*- coding: utf-8 -*-
"""Quét kho OCR, rút ra mọi cặp (chế độ, mẫu biểu, mã, tên) có thật trong B01/B02/B03.

Đầu ra:
  devtools/ra/ma_chi_tieu_tho.csv   — một dòng cho mỗi (chế độ, mẫu biểu, mã)
  devtools/ra/ma_chi_tieu_bien.csv  — các biến thể tên của cùng một mã (soi lỗi OCR)
  in ra màn hình bản tóm tắt

Chạy:  python -X utf8 devtools/quet_ma_chi_tieu.py
"""
import io
import os
import re
import csv
import glob
import collections

GOC = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
OCR = os.path.join(GOC, "data", "ocr", "CTR")
RA = os.path.join(GOC, "devtools", "ra")

# ---------------------------------------------------------------- nhận chế độ
TT99 = re.compile(r"Th[ôo]ng t[ưu]\s*(?:s[ốo]\s*)?99[/\s]", re.I)
TT200 = re.compile(r"Th[ôo]ng t[ưu]\s*(?:s[ốo]\s*)?200[/\s]", re.I)

# ---------------------------------------------------------------- nhận mẫu biểu
# Dùng khớp lỏng vì OCR hay sai dấu ("GIỮA NHIỀU ĐỘ" thay cho "GIỮA NIÊN ĐỘ").
TIEU_DE = [
    # TT99/2025 đổi tên biểu B01: "Bảng cân đối kế toán" → "Báo cáo tình hình tài chính"
    ("B01", re.compile(r"B[ẢA]NG\s+C[ÂA]N\s+[ĐD][ỐO]I\s+K[ẾE]\s+TO[ÁA]N"
                       r"|B[ÁA]O\s+C[ÁA]O\s+T[ÌI]NH\s+H[ÌI]NH\s+T[ÀA]I\s+CH[ÍI]NH", re.I)),
    # "HOẠT ĐỘNG" là tuỳ chọn: tài liệu đính chính ghi "KẾT QUẢ KINH DOANH"
    ("B02", re.compile(r"K[ẾE]T\s+QU[ẢA]\s+(?:HO[ẠA]T\s+[ĐD][ỘO]NG\s+)?KINH\s+DOANH", re.I)),
    ("B03", re.compile(r"L[ƯU]U\s+CHUY[ỂE]N\s+TI[ỀE]N\s+T[ỆE]", re.I)),
]
TIEP_THEO = re.compile(r"\(\s*ti[ếe]p\s+theo\s*\)", re.I)

# Phân biệt hai phương pháp của B03: trực tiếp mở đầu bằng "Tiền thu từ bán hàng",
# gián tiếp mở đầu bằng "Lợi nhuận trước thuế".
B03_TRUC_TIEP = re.compile(r"ti[ềe]n\s+thu\s+t[ừu]\s+b[áa]n\s+h[àa]ng", re.I)
B03_GIAN_TIEP = re.compile(r"l[ợo]i\s+nhu[ậa]n\s+tr[ưu][ớo]c\s+thu[ếe]", re.I)

# ---------------------------------------------------------------- nhận dòng mã
DONG = re.compile(r"^\|\s*\**\s*(\d{2,3})\s*\**\s*\|\s*([^|]{4,90}?)\s*\|(.*)$", re.M)
MIN_DONG = 4          # một biểu thật luôn có nhiều hơn vài dòng mã kèm số


def nguon_dung_de_trich(duong_dan):
    """Chỉ trích số từ BCTC gốc và các văn bản đính chính.

    Báo cáo thường niên CHÉP LẠI bảng của BCTC — trích cả hai sẽ sinh hai dòng cho
    cùng một chỉ tiêu, cùng một ngày. Tệ hơn: BCTN tự đánh lại mã (BCTN 2022 ghi
    250/251/252 cho thứ mà BCTC ghi 260/261/262), làm bẩn bảng ánh xạ.
    BCTN vẫn nằm nguyên trong Qdrant, chỉ không dùng để trích số.
    """
    d = duong_dan.replace(os.sep, "/")
    if "/01_bao_cao_tai_chinh/" in d:
        return True
    return "/dinh_chinh/" in d          # số trình bày lại, không có ở nguồn nào khác
SO = re.compile(r"\(?\s*\d{1,3}(?:\.\d{3}){2,}\s*\)?")
CHI_SO = re.compile(r"^[\d.,()\-\s]+$")
DAU_DONG = re.compile(r"^\s*(?:[-–•*]\s*)?(?:[IVXivx]+|[A-Ga-g]|\d{1,2})\s*[.)]\s*")


def doc(p):
    return io.open(p, encoding="utf-8", errors="replace").read()


def chuan_ten(t):
    t = t.replace("*", " ")
    t = re.sub(r"\s+", " ", t).strip()
    for _ in range(3):                       # "A. 1. Doanh thu..." bóc nhiều lớp
        moi = DAU_DONG.sub("", t).strip()
        if moi == t:
            break
        t = moi
    return t


def nhan_che_do(toan_van, duong_dan):
    """Trả (chế_độ, có_phải_suy_đoán).

    che_do LÀ MỘT PHẦN CỦA KHOÁ CHÍNH nên không được lẫn dấu '?' vào — làm vậy thì
    ("TT200", B01, 100) và ("TT200?", B01, 100) thành hai dòng cho cùng một thứ.
    Độ tin cậy để ở cột riêng.
    """
    if TT99.search(toan_van):
        return "TT99", False
    if TT200.search(toan_van):
        return "TT200", False
    m = re.search(r"/(20\d{2})/", duong_dan.replace(os.sep, "/"))
    nam = int(m.group(1)) if m else 0
    return ("TT99" if nam >= 2026 else "TT200"), True


def loai_b03(doan):
    if B03_TRUC_TIEP.search(doan):
        return "B03-TT"
    if B03_GIAN_TIEP.search(doan):
        return "B03-GT"
    return "B03-?"


def cat_doan(noi_dung):
    """Cắt một trang thành các đoạn theo tiêu đề biểu.

    Một trang có thể chứa HAI biểu (đã gặp thật: trang đính chính có cả bảng cân đối
    lẫn kết quả kinh doanh). Gán một mẫu biểu cho cả trang là sai.

    Trả về [(mau_bieu | None, đoạn văn bản)], đoạn đầu có thể mang None nếu trang
    bắt đầu giữa chừng một biểu của trang trước.
    """
    moc = []
    for ten, mau in TIEU_DE:
        for m in mau.finditer(noi_dung):
            moc.append((m.start(), ten))
    moc.sort()

    if not moc:
        return [(None, noi_dung)]

    doan = []
    if moc[0][0] > 0:
        doan.append((None, noi_dung[:moc[0][0]]))
    for i, (vt, ten) in enumerate(moc):
        het = moc[i + 1][0] if i + 1 < len(moc) else len(noi_dung)
        phan = noi_dung[vt:het]
        doan.append((loai_b03(phan) if ten == "B03" else ten, phan))
    return doan


def hop_le(dong_van_ban):
    """Dòng bảng chỉ được tính khi có số tiền hoặc dấu gạch thay cho số 0."""
    return bool(SO.search(dong_van_ban)) or re.search(r"\|\s*[-–]\s*\|", dong_van_ban)


def main():
    os.makedirs(RA, exist_ok=True)
    thu_muc = sorted(os.path.dirname(p)
                     for p in glob.glob(OCR + "/**/_TOAN_VAN.md", recursive=True))

    dem = collections.Counter()               # (che_do, mau_bieu, ma, ten) -> số lần
    trang_theo_mau = collections.Counter()
    ke_thua = 0
    suy_doan = set()
    xac_nhan = set()      # cap nao co IT NHAT MOT tai lieu doc duoc che do trong van ban
    tong_trang = 0

    bo_qua = 0
    for tm in thu_muc:
        if not nguon_dung_de_trich(tm):
            bo_qua += 1
            continue
        che_do, suy = nhan_che_do(doc(os.path.join(tm, "_TOAN_VAN.md")), tm)
        truoc = None                          # mẫu biểu của đoạn cuối trang trước
        b03_truoc = None                      # phương pháp LCTT gần nhất trong tài liệu

        for p in sorted(glob.glob(os.path.join(tm, "trang_*.md"))):
            if os.path.exists(p + ".en"):     # bỏ trang tiếng Anh
                continue
            tong_trang += 1
            t = doc(p)
            co_bieu = False

            for mau, phan in cat_doan(t):
                # đoạn mở đầu trang không có tiêu đề: chỉ nhận khi ghi "(tiếp theo)"
                if mau is None:
                    if truoc and TIEP_THEO.search(t[:400]):
                        mau, tu_ke_thua = truoc, True
                    else:
                        continue
                else:
                    tu_ke_thua = False

                # trang đuôi của LCTT không lộ phương pháp → lấy của đoạn trước
                if mau == "B03-?" and b03_truoc:
                    mau = b03_truoc

                dong = [(ma, ten) for ma, ten, duoi in DONG.findall(phan)
                        if hop_le(duoi)]
                if len(dong) < MIN_DONG:      # bảng nghị quyết, tờ trình... bị loại ở đây
                    continue

                truoc = mau
                if mau.startswith("B03"):
                    b03_truoc = mau
                if tu_ke_thua:
                    ke_thua += 1
                co_bieu = True
                trang_theo_mau[mau] += 1

                for ma, ten in dong:
                    ten = chuan_ten(ten)
                    # nguong 3: ma 111 la "Tien" — 4 ky tu. Ngung 5 tung lam mat no.
                    if len(ten) < 3 or CHI_SO.match(ten):
                        continue
                    dem[(che_do, mau, ma, ten)] += 1
                    (suy_doan if suy else xac_nhan).add((che_do, mau, ma))

            if not co_bieu:
                truoc = None

    # ---------------------------------------------- gộp biến thể tên của cùng một mã
    theo_ma = collections.defaultdict(collections.Counter)
    for (cd, mb, ma, ten), n in dem.items():
        theo_ma[(cd, mb, ma)][ten] += n

    chua_chac = (suy_doan - xac_nhan) & set(theo_ma)

    with io.open(os.path.join(RA, "ma_chi_tieu_tho.csv"), "w",
                 encoding="utf-8-sig", newline="") as f:
        w = csv.writer(f)
        w.writerow(["che_do", "mau_bieu", "ma", "ten_tren_bao_cao",
                    "so_lan", "so_bien_the", "che_do_suy_doan",
                    "ma_noi_bo", "ten_chuan", "ghi_chu"])
        for (cd, mb, ma), c in sorted(theo_ma.items(),
                                      key=lambda kv: (-sum(kv[1].values()),
                                                      kv[0][0], kv[0][1], kv[0][2])):
            ten, _ = c.most_common(1)[0]
            w.writerow([cd, mb, ma, ten, sum(c.values()), len(c),
                        "x" if (cd, mb, ma) in chua_chac else "", "", "", ""])

    with io.open(os.path.join(RA, "ma_chi_tieu_bien.csv"), "w",
                 encoding="utf-8-sig", newline="") as f:
        w = csv.writer(f)
        w.writerow(["che_do", "mau_bieu", "ma", "bien_the", "so_lan", "nghi_rac"])
        for (cd, mb, ma), c in sorted(theo_ma.items()):
            if len(c) < 2:
                continue
            nhieu = c.most_common(1)[0][1]
            for ten, n in c.most_common():
                w.writerow([cd, mb, ma, ten, n, "x" if n == 1 and nhieu >= 5 else ""])

    # ---------------------------------------------------------------- tóm tắt
    print("Tai lieu                 : %d  (bo qua %d — khong dung de trich so)"
          % (len(thu_muc) - bo_qua, bo_qua))
    print("Trang tieng Viet doc qua : %d" % tong_trang)
    print("Trang nhan ra la B01/02/03: %d  (trong do ke thua tieu de: %d)"
          % (sum(trang_theo_mau.values()), ke_thua))
    print()
    print("  theo mau bieu:")
    for k, v in sorted(trang_theo_mau.items()):
        print("    %-8s %5d trang" % (k, v))
    print()
    print("  Cap chua xac nhan duoc che_do (khong tai lieu nao ghi thong tu): %d"
          % len(chua_chac))
    print()
    cd_dem = collections.Counter(k[0] for k in theo_ma)
    print("  theo che do (so cap ma):")
    for k, v in sorted(cd_dem.items()):
        print("    %-8s %5d cap" % (k, v))
    print()
    print("  TONG SO CAP (che_do, mau_bieu, ma) : %d" % len(theo_ma))
    print("  Cap chi gap 1 lan (nghi rac)       : %d"
          % sum(1 for c in theo_ma.values() if sum(c.values()) == 1))
    print()
    print("Da ghi:")
    print("  devtools/ra/ma_chi_tieu_tho.csv")
    print("  devtools/ra/ma_chi_tieu_bien.csv")


if __name__ == "__main__":
    main()
