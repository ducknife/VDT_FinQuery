# -*- coding: utf-8 -*-
"""Soát toàn bộ kết quả OCR — không tin con số của tool, đếm lại từ đĩa.

  python devtools/soat_ocr.py            # bản tóm tắt
  python devtools/soat_ocr.py --chi-tiet # liệt kê từng tài liệu có vấn đề
"""
import os, re, io, json, glob, argparse, collections

GOC = r"D:/VDT_FinQuery"
RAW = os.path.join(GOC, "data", "raw", "CTR")
OCR = os.path.join(GOC, "data", "ocr", "CTR")
CHI_MUC = os.path.join(GOC, "data", "ocr", "_chi_muc.json")

DAU_VIET = re.compile(
    r"[àáảãạăằắẳẵặâầấẩẫậèéẻẽẹêềếểễệìíỉĩịòóỏõọôồốổỗộơờớởỡợùúủũụưừứửữựỳýỷỹỵđ]", re.I)

# dau hieu trang OCR hong: model tu choi, tra loi rong, lap lai vo han
NGHI_NGO = [
    (re.compile(r"^\s*$"), "rỗng"),
    # Chỉ tính là từ chối khi câu đó nằm ở ĐẦU kết quả. Giữa thuyết minh
    # tiếng Anh có cụm "unable to meet its obligations" (định nghĩa rủi ro
    # tín dụng) hoàn toàn bình thường — trước đây bị bắt nhầm.
    (re.compile(r"\A\s*(I'm sorry|I cannot|I can't|I am unable|Xin lỗi)", re.I), "model từ chối"),
    # Chỉ bắt lặp NỘI DUNG THẬT. Dòng phân cách bảng "-----" và khoảng trắng
    # căn cột đều là định dạng Markdown bình thường, không phải lỗi.
    (re.compile(r"((?=[^\n]*[A-Za-z0-9À-ỹ])[^\n]{15,80}?)(?:\1){4,}"), "lặp vô hạn"),
]


def so_trang_goc(rel, co):
    try:
        chi = json.load(io.open(CHI_MUC, encoding="utf-8"))
        n = chi.get(rel + ".pdf", {}).get("so_trang")
        if n:
            return n
    except Exception:
        pass
    return max(co) if co else 0


def soat():
    ds = []
    for root, _, files in os.walk(OCR):
        trang = {}
        loi, en = {}, set()
        for f in files:
            m = re.match(r"^trang_(\d+)\.md$", f)
            if m:
                trang[int(m.group(1))] = os.path.join(root, f)
            elif f.endswith(".md.loi"):
                n = int(re.match(r"trang_(\d+)", f).group(1))
                try:
                    loi[n] = json.load(io.open(os.path.join(root, f), encoding="utf-8"))
                except Exception:
                    loi[n] = {"lan": "?"}
            elif f.endswith(".md.en"):
                en.add(int(re.match(r"trang_(\d+)", f).group(1)))
        if not trang:
            continue

        rel = os.path.relpath(root, OCR).replace("\\", "/")
        tong = so_trang_goc(rel, set(trang))
        thieu = [i for i in range(1, tong + 1) if i not in trang]

        ngan, xau, en_do = [], [], []
        for i, p in sorted(trang.items()):
            t = io.open(p, encoding="utf-8").read()
            if len(t.strip()) < 60:
                ngan.append(i)
            for mau, ly_do in NGHI_NGO:
                if mau.search(t):
                    xau.append((i, ly_do)); break
            # tu nhan dien tieng Anh lai, khong tin tep .en
            if len(t) > 400 and len(DAU_VIET.findall(t)) / len(t) < 0.005:
                en_do.append(i)

        ds.append(dict(rel=rel, thu_muc=rel.split("/")[0], tong=tong, co=len(trang),
                       thieu=thieu, loi=loi, en_tep=en, en_do=en_do,
                       ngan=ngan, xau=xau,
                       toan_van=os.path.exists(os.path.join(root, "_TOAN_VAN.md"))))
    return ds


def main():
    a = argparse.ArgumentParser()
    a.add_argument("--chi-tiet", action="store_true")
    g = a.parse_args()
    ds = soat()

    tong_trang = sum(d["tong"] for d in ds)
    tong_co = sum(d["co"] for d in ds)
    tong_thieu = sum(len(d["thieu"]) for d in ds)
    tong_loi = sum(len(d["loi"]) for d in ds)
    tong_en_tep = sum(len(d["en_tep"]) for d in ds)
    tong_en_do = sum(len(d["en_do"]) for d in ds)
    tong_ngan = sum(len(d["ngan"]) for d in ds)
    tong_xau = sum(len(d["xau"]) for d in ds)
    thieu_tv = [d for d in ds if not d["toan_van"]]

    # tep PDF chua he duoc dung toi
    co_thu_muc = {d["rel"] for d in ds}
    chua = []
    for p in glob.glob(RAW + "/**/*.pdf", recursive=True):
        rel = os.path.relpath(p, RAW).replace("\\", "/")[:-4]
        if rel not in co_thu_muc:
            chua.append(rel)

    print("=" * 74)
    print("  SOÁT KẾT QUẢ OCR")
    print("=" * 74)
    print("  Tài liệu có kết quả      : %4d" % len(ds))
    print("  Tài liệu CHƯA đụng tới   : %4d  %s" % (len(chua), "<-- CẦN XEM" if chua else ""))
    print("  Tổng số trang gốc        : %5d" % tong_trang)
    print("  Đã OCR                   : %5d  (%.1f%%)" % (tong_co, 100 * tong_co / max(tong_trang, 1)))
    print("  THIẾU                    : %5d  %s" % (tong_thieu, "<-- CẦN VÁ" if tong_thieu else "(sạch)"))
    print()
    print("  Trang có tệp .loi (từng hỏng) : %4d" % tong_loi)
    print("  Trang đánh dấu tiếng Anh      : %4d" % tong_en_tep)
    print("  Trang tự đo LÀ tiếng Anh      : %4d  %s"
          % (tong_en_do, "<-- LỆCH, xem bên dưới" if tong_en_do != tong_en_tep else ""))
    print("  Trang ngắn bất thường (<60 ký tự) : %4d" % tong_ngan)
    print("  Trang nội dung đáng ngờ           : %4d" % tong_xau)
    print("  Tài liệu thiếu _TOAN_VAN.md       : %4d" % len(thieu_tv))

    print()
    print("  %-26s %5s %6s %7s %6s %6s" % ("THƯ MỤC", "tệp", "trang", "đã OCR", "thiếu", "hỏng"))
    tm = collections.defaultdict(lambda: [0, 0, 0, 0, 0])
    for d in ds:
        k = tm[d["thu_muc"]]
        k[0] += 1; k[1] += d["tong"]; k[2] += d["co"]; k[3] += len(d["thieu"]); k[4] += len(d["loi"])
    for k in sorted(tm):
        v = tm[k]
        print("  %-26s %5d %6d %7d %6d %6d" % (k, v[0], v[1], v[2], v[3], v[4]))

    if chua:
        print()
        print("  --- TÀI LIỆU CHƯA OCR TRANG NÀO ---")
        for r in chua[:20]:
            print("    %s" % r)
        if len(chua) > 20:
            print("    ... còn %d tệp" % (len(chua) - 20))

    xau_ds = [d for d in ds if d["thieu"] or d["loi"] or d["ngan"] or d["xau"] or not d["toan_van"]]
    if xau_ds:
        print()
        print("  --- TÀI LIỆU CÓ VẤN ĐỀ (%d) ---" % len(xau_ds))
        for d in sorted(xau_ds, key=lambda x: -(len(x["thieu"]) + len(x["xau"]) + len(x["ngan"])))[:25]:
            v = []
            if d["thieu"]: v.append("thiếu %s" % (d["thieu"][:6] if len(d["thieu"]) <= 6 else str(d["thieu"][:6]) + "..."))
            if d["loi"]: v.append("từng hỏng %d trang" % len(d["loi"]))
            if d["ngan"]: v.append("ngắn %s" % d["ngan"][:6])
            if d["xau"]: v.append("đáng ngờ %s" % d["xau"][:4])
            if not d["toan_van"]: v.append("thiếu _TOAN_VAN")
            print("    %-46s %s" % (os.path.basename(d["rel"])[:46], " · ".join(v)))

    if g.chi_tiet:
        print()
        print("  --- TRANG TỰ ĐO LÀ TIẾNG ANH NHƯNG CHƯA ĐÁNH DẤU ---")
        n = 0
        for d in ds:
            chenh = sorted(set(d["en_do"]) - d["en_tep"])
            if chenh:
                print("    %-46s %s" % (os.path.basename(d["rel"])[:46], chenh[:12]))
                n += len(chenh)
        print("    tổng %d trang" % n)

    print()
    if not tong_thieu and not chua and not tong_xau:
        print("  ==> OCR ĐÃ SẠCH. Không thiếu trang, không trang hỏng.")
    else:
        print("  ==> CHƯA XONG. Xem các mục 'CẦN' ở trên.")


if __name__ == "__main__":
    main()
