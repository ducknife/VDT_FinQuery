"""
Đánh giá chất lượng lớp chữ của từng PDF đã tải — để biết file nào cần OCR trước khi
đưa vào pipeline RAG (chuyen_pdf_rag.py KHÔNG OCR).

Ba mức:
    so_hoa   PDF xuất trực tiếp từ Word/Excel: có chữ, dấu tiếng Việt đầy đủ -> trích thẳng
    ocr_kem  có lớp chữ nhưng do máy scan tự OCR, mất/lỗi dấu ("BAO CÁO TAI CHINH HQP NHAT")
             -> phải OCR lại, không dùng lớp chữ có sẵn
    scan     gần như không có chữ (chỉ ảnh, hoặc chỉ dòng chữ ký số) -> phải OCR

Tiêu chí (đo trên tối đa 12 trang mẫu rải đều file):
    ký tự / trang < 150                     -> scan
    tỷ lệ chữ cái có dấu tiếng Việt < 12%   -> ocr_kem   (văn bản tiếng Việt chuẩn ~25–35%)
    còn lại                                 -> so_hoa
File song ngữ (TV+TA) có tỷ lệ dấu thấp hơn tự nhiên nên ngưỡng hạ còn 7%.

CHẠY:  python kiem_tra_pdf.py            -> data/raw/CTR/chat_luong_pdf.csv + tóm tắt
"""

from __future__ import annotations

import csv
import sys
from collections import Counter, defaultdict
from concurrent.futures import ProcessPoolExecutor
from pathlib import Path

import pymupdf

GOC = Path(__file__).resolve().parents[1] / "data" / "raw" / "CTR"
CO_DAU = set("ăâđêôơưàảãáạằẳẵắặầẩẫấậèẻẽéẹềểễếệìỉĩíịòỏõóọồổỗốộờởỡớợùủũúụừửữứựỳỷỹýỵ")
NGUONG_KY_TU = 150
NGUONG_DAU = 0.12
NGUONG_DAU_SONG_NGU = 0.07


def trang_mau(n: int, toi_da: int = 12) -> list[int]:
    if n <= toi_da:
        return list(range(n))
    buoc = n / toi_da
    return sorted({int(i * buoc) for i in range(toi_da)})


def danh_gia(rel: str, song_ngu: bool) -> dict:
    p = GOC / rel
    try:
        with pymupdf.open(p) as d:
            n = d.page_count
            mau = trang_mau(n)
            chu = "".join(d[i].get_text() for i in mau)
    except Exception as e:  # noqa: BLE001
        return dict(duong_dan=rel, so_trang=-1, ky_tu_moi_trang=0, ty_le_dau=0,
                    muc="loi", ghi_chu=type(e).__name__)
    chu_cai = [c for c in chu.lower() if c.isalpha()]
    ty_le = sum(c in CO_DAU for c in chu_cai) / len(chu_cai) if chu_cai else 0.0
    ky_tu = len(chu.strip()) / max(len(mau), 1)
    if ky_tu < NGUONG_KY_TU:
        muc = "scan"
    elif ty_le < (NGUONG_DAU_SONG_NGU if song_ngu else NGUONG_DAU):
        muc = "ocr_kem"
    else:
        muc = "so_hoa"
    return dict(duong_dan=rel, so_trang=n, ky_tu_moi_trang=round(ky_tu), ty_le_dau=round(ty_le, 3),
                muc=muc, ghi_chu="")


def main():
    sys.stdout.reconfigure(encoding="utf-8")
    man = list(csv.DictReader(open(GOC / "manifest.csv", encoding="utf-8-sig")))
    giu = {m["duong_dan"]: m for m in man
           if m["trang_thai"] in ("tai_ve", "da_co") and m["duong_dan"].lower().endswith(".pdf")}
    with ProcessPoolExecutor() as ex:
        kq = list(ex.map(danh_gia, giu, [giu[k]["ngon_ngu"] == "vi+en" for k in giu]))

    ra = GOC / "chat_luong_pdf.csv"
    with open(ra, "w", newline="", encoding="utf-8-sig") as f:
        w = csv.DictWriter(f, fieldnames=list(kq[0].keys()))
        w.writeheader()
        w.writerows(sorted(kq, key=lambda x: x["duong_dan"]))
    print(f"Đã ghi {ra}\n")

    tong = Counter()
    trang = Counter()
    theo_nguon: dict[str, Counter] = defaultdict(Counter)
    for k in kq:
        tong[k["muc"]] += 1
        trang[k["muc"]] += max(k["so_trang"], 0)
        nguon = k["duong_dan"].split("/")[0]
        nam = k["duong_dan"].split("/")[1] if k["duong_dan"].split("/")[1].isdigit() else "-"
        theo_nguon[f"{nguon} {nam}"][k["muc"]] += 1
    print("| Mức | Số file | Số trang |\n|---|---|---|")
    for m in ("so_hoa", "ocr_kem", "scan", "loi"):
        if tong[m]:
            print(f"| {m} | {tong[m]} | {trang[m]} |")
    print(f"| **Tổng** | {sum(tong.values())} | {sum(trang.values())} |\n")
    print("| Nguồn / năm | so_hoa | ocr_kem | scan |\n|---|---|---|---|")
    for k in sorted(theo_nguon):
        c = theo_nguon[k]
        print(f"| {k} | {c['so_hoa']} | {c['ocr_kem']} | {c['scan']} |")


if __name__ == "__main__":
    main()
