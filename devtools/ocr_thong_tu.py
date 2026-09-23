# -*- coding: utf-8 -*-
"""OCR Thong tu 99/2025/TT-BTC thanh Markdown, MOT LUONG duy nhat.

Chay tung lo 70 trang; sau moi lo thi quet xem da ra cong thuc chua rồi bao lai.
Mot luong de khong tranh bang thong voi OCR Studio dang chay.

  python devtools/ocr_thong_tu.py               # chay tu trang 1
  python devtools/ocr_thong_tu.py --tu 600      # chay tu trang 600
  python devtools/ocr_thong_tu.py --lo 40       # lo 40 trang
"""
import os, sys, json, time, base64, urllib.request, urllib.error, re, argparse, io

try:
    import pymupdf as fitz
except ImportError:
    import fitz

GOC = r"D:/VDT_FinQuery"
TEP = r"D:/Downloads/Thông tư số 99-TT-BTC ngày 27-10-2025 về hướng dẫn chế kế toán doanh nghiệp.pdf"
RA = os.path.join(GOC, "data", "ocr", "phap_ly", "TT99-2025")
API = "https://viettelai.vn/model-as-a-service/v1/chat/completions"
MODEL = "Qwen3-VL-32B-Instruct-FP8"
DPI = 170

NHAC = (
    "Trich xuat toan bo noi dung trang nay thanh Markdown.\n"
    "- Giu nguyen so dieu, so khoan, so muc va cach danh so goc.\n"
    "- CONG THUC tinh chi tieu phai giu nguyen dang, vi du: Ma so 280 = Ma so 100 + Ma so 200.\n"
    "- Bang thi xuat ra bang Markdown, moi cot mot cot rieng.\n"
    "- Giu day du tung chu so cua moi con so.\n"
    "- Chi tra ve noi dung trang, khong giai thich gi them."
)

# dau hieu cong thuc
MAU_CONG_THUC = [
    (re.compile(r"M[ãa]\s*s[ốo]\s*\d+\s*=", re.I), "cong thuc cong don ma chi tieu"),
    (re.compile(r"=\s*M[ãa]\s*s[ốo]\s*\d+", re.I), "cong thuc cong don ma chi tieu"),
    (re.compile(r"Ph[ươuo]+ng\s+ph[áa]p\s+l[ậa]p", re.I), "muc 'Phuong phap lap'"),
    (re.compile(r"s[ốo]\s*d[ưu]\s*(N[ợo]|C[óo])\s*c[ủu]a\s*T[àa]i\s*kho[ảa]n\s*\d+", re.I), "chi tieu lay tu tai khoan"),
    (re.compile(r"Ch[ỉi]\s*ti[êe]u\s*[\"“][^\"”]{3,60}[\"”]\s*=", re.I), "dang thuc giua cac chi tieu"),
]


def khoa_api():
    for l in io.open(os.path.join(GOC, ".env"), encoding="utf-8"):
        if l.startswith("LLM_API_KEY"):
            return l.split("=", 1)[1].strip()
    raise SystemExit("khong thay LLM_API_KEY trong .env")


def bo_rao(t):
    d = t.strip().split("\n")
    if len(d) >= 2 and re.match(r"^```[a-zA-Z]*\s*$", d[0]) and re.match(r"^```\s*$", d[-1]):
        return "\n".join(d[1:-1]).strip()
    return t.strip()


def mot_trang(doc, i, KHOA):
    dich = os.path.join(RA, "trang_%03d.md" % (i + 1))
    if os.path.exists(dich) and os.path.getsize(dich) > 40:
        return None, 0, True

    pix = doc[i].get_pixmap(dpi=DPI)
    uri = "data:image/png;base64," + base64.b64encode(pix.tobytes("png")).decode()
    than = json.dumps({
        "model": MODEL, "max_tokens": 6000, "temperature": 0,
        "messages": [{"role": "user", "content": [
            {"type": "text", "text": NHAC},
            {"type": "image_url", "image_url": {"url": uri}}]}],
    }).encode()

    cuoi = None
    for lan in range(3):
        try:
            req = urllib.request.Request(API, data=than, headers={
                "Authorization": "Bearer " + KHOA, "Content-Type": "application/json"})
            with urllib.request.urlopen(req, timeout=420) as r:
                j = json.loads(r.read().decode())
            noi = bo_rao(j["choices"][0]["message"].get("content") or "")
            io.open(dich, "w", encoding="utf-8").write(noi + "\n")
            return noi, j.get("usage", {}).get("total_tokens", 0), False
        except Exception as e:
            cuoi = e
            time.sleep(5 * (lan + 1))
    raise RuntimeError("trang %d that bai: %s" % (i + 1, cuoi))


def quet_cong_thuc(tu, den):
    """Doc lai cac trang da OCR trong khoang, tra ve danh sach phat hien."""
    thay = []
    for i in range(tu, den):
        p = os.path.join(RA, "trang_%03d.md" % (i + 1))
        if not os.path.exists(p):
            continue
        t = io.open(p, encoding="utf-8").read()
        for dong in t.split("\n"):
            for mau, ten in MAU_CONG_THUC:
                if mau.search(dong):
                    thay.append((i + 1, ten, dong.strip()[:130]))
                    break
    return thay


def gop():
    ds = sorted(f for f in os.listdir(RA) if re.match(r"^trang_\d+\.md$", f))
    with io.open(os.path.join(RA, "_TOAN_VAN.md"), "w", encoding="utf-8") as g:
        g.write("# Thông tư 99/2025/TT-BTC — Hướng dẫn Chế độ kế toán doanh nghiệp\n\n")
        g.write("*Bản Markdown do OCR (Qwen3-VL-32B qua Viettel AI) từ bản quét 778 trang.*\n")
        for f in ds:
            g.write("\n\n<!-- ===== TRANG %s ===== -->\n\n" % f[6:9].lstrip("0"))
            g.write(io.open(os.path.join(RA, f), encoding="utf-8").read())
    return len(ds)


def main():
    a = argparse.ArgumentParser()
    a.add_argument("--tu", type=int, default=1)
    a.add_argument("--den", type=int, default=0)
    a.add_argument("--lo", type=int, default=70)
    g = a.parse_args()

    os.makedirs(RA, exist_ok=True)
    KHOA = khoa_api()
    doc = fitz.open(TEP)
    den = g.den or doc.page_count
    print("TT99: %d trang | OCR tu %d den %d | lo %d trang | MOT luong"
          % (doc.page_count, g.tu, den, g.lo), flush=True)

    tong_tok = xong = co_san = 0
    t0 = time.time()
    lo_bd = g.tu - 1

    while lo_bd < den:
        lo_kt = min(lo_bd + g.lo, den)
        print("\n===== LO trang %d-%d =====" % (lo_bd + 1, lo_kt), flush=True)
        for i in range(lo_bd, lo_kt):
            t1 = time.time()
            try:
                noi, tok, sn = mot_trang(doc, i, KHOA)
            except Exception as e:
                print("  ! trang %3d  %s" % (i + 1, e), flush=True)
                continue
            if sn:
                co_san += 1
                continue
            xong += 1; tong_tok += tok
            print("  + trang %3d  %5.1fs  %5d tok  %5d ky tu"
                  % (i + 1, time.time() - t1, tok, len(noi)), flush=True)

        thay = quet_cong_thuc(lo_bd, lo_kt)
        print("\n  --- quet cong thuc trong lo: %d phat hien ---" % len(thay), flush=True)
        for tr, ten, dong in thay[:25]:
            print("    tr%-4d [%s] %s" % (tr, ten, dong), flush=True)
        if thay:
            print("\n  >>> DA RA CONG THUC o lo nay <<<", flush=True)

        n = gop()
        print("  (da ghep %d trang vao _TOAN_VAN.md | tong %d trang moi, %d co san, %d token, %.1f phut)"
              % (n, xong, co_san, tong_tok, (time.time() - t0) / 60), flush=True)
        lo_bd = lo_kt

    print("\nXONG. %d trang moi | %d token | %.1f phut | %s"
          % (xong, tong_tok, (time.time() - t0) / 60, RA), flush=True)


if __name__ == "__main__":
    main()
