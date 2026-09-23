# -*- coding: utf-8 -*-
"""Va cac trang OCR bi hong le te.

Tim nhung tai liet da OCR gan xong nhung con thieu vai trang o giua
(dau hieu cua hong nhat thoi do mang), roi OCR lai dung nhung trang do —
MOT LUONG, DPI thap, de chay duoc ca khi mang yeu.

  python devtools/va_lo_ocr.py              # chi xem, khong goi API
  python devtools/va_lo_ocr.py --chay       # va that
  python devtools/va_lo_ocr.py --chay --dpi 130
  python devtools/va_lo_ocr.py --chay --nguong 0.5   # nhan ca tai lieu moi xong 50%
"""
import os, re, io, json, time, base64, argparse, urllib.request, urllib.error

try:
    import pymupdf as fitz
except ImportError:
    import fitz
from PIL import Image

GOC = r"D:/VDT_FinQuery"
RAW = os.path.join(GOC, "data", "raw", "CTR")
OCR = os.path.join(GOC, "data", "ocr", "CTR")
CHI_MUC = os.path.join(GOC, "data", "ocr", "_chi_muc.json")
API = "https://viettelai.vn/model-as-a-service/v1/chat/completions"
MODEL = "Qwen3-VL-32B-Instruct-FP8"

NHAC = ("Trich xuat toan bo noi dung trang nay thanh Markdown.\n"
        "- Moi bang phai xuat ra dang bang Markdown, moi cot mot cot rieng biet.\n"
        "- Giu nguyen ma so chi tieu, so thuyet minh va DAY DU tung chu so cua moi con so.\n"
        "- So am giu nguyen dau ngoac don.\n"
        "- O trong thi de trong, khong dien 0.\n"
        "- Chi tra ve noi dung trang, khong giai thich gi them.")


def khoa():
    for l in io.open(os.path.join(GOC, ".env"), encoding="utf-8"):
        if l.startswith("LLM_API_KEY"):
            return l.split("=", 1)[1].strip()
    raise SystemExit("khong thay LLM_API_KEY trong .env")


def so_trang_goc(rel, co):
    try:
        chi = json.load(io.open(CHI_MUC, encoding="utf-8"))
        n = chi.get(rel + ".pdf", {}).get("so_trang")
        if n:
            return n
    except Exception:
        pass
    p = os.path.join(RAW, rel + ".pdf")
    return fitz.open(p).page_count if os.path.exists(p) else max(co)


def tim_lo(nguong):
    """Tra ve [(rel, so_trang, [trang thieu])] cho cac tai lieu da xong >= nguong."""
    ra = []
    for root, _, files in os.walk(OCR):
        co = {int(re.match(r"trang_(\d+)", f).group(1))
              for f in files if re.match(r"trang_\d+\.md$", f)}
        if not co:
            continue
        rel = os.path.relpath(root, OCR).replace("\\", "/")
        n = so_trang_goc(rel, co)
        thieu = [i for i in range(1, n + 1) if i not in co]
        if thieu and len(co) / n >= nguong:
            ra.append((rel, n, thieu))
    return sorted(ra, key=lambda x: len(x[2]))


def bo_rao(t):
    d = t.strip().split("\n")
    if len(d) >= 2 and re.match(r"^```[a-zA-Z]*\s*$", d[0]) and re.match(r"^```\s*$", d[-1]):
        return "\n".join(d[1:-1]).strip()
    return t.strip()


def ocr(pdf, trang, dpi, chat, KHOA, han):
    d = fitz.open(pdf)
    pix = d[trang - 1].get_pixmap(dpi=dpi)
    im = Image.frombytes("RGB", (pix.width, pix.height), pix.samples).convert("L")
    b = io.BytesIO(); im.save(b, "JPEG", quality=chat, optimize=True)
    raw = b.getvalue()
    than = json.dumps({
        "model": MODEL, "max_tokens": 6000, "temperature": 0,
        "messages": [{"role": "user", "content": [
            {"type": "text", "text": NHAC},
            {"type": "image_url", "image_url":
                {"url": "data:image/jpeg;base64," + base64.b64encode(raw).decode()}}]}],
    }).encode()
    req = urllib.request.Request(API, data=than, headers={
        "Authorization": "Bearer " + KHOA, "Content-Type": "application/json"})
    with urllib.request.urlopen(req, timeout=han) as r:
        j = json.loads(r.read().decode())
    return (bo_rao(j["choices"][0]["message"].get("content") or ""),
            j.get("usage", {}).get("total_tokens", 0), len(raw) * 4 // 3 // 1024)


def main():
    a = argparse.ArgumentParser()
    a.add_argument("--chay", action="store_true", help="thuc su goi API (mac dinh chi xem)")
    a.add_argument("--dpi", type=int, default=110)
    a.add_argument("--chat", type=int, default=82, help="chat luong JPEG")
    a.add_argument("--nguong", type=float, default=0.8, help="chi va tai lieu da xong tu ty le nay")
    a.add_argument("--han", type=int, default=240, help="han cho moi trang, giay")
    g = a.parse_args()

    lo = tim_lo(g.nguong)
    tong = sum(len(x[2]) for x in lo)
    print("Tim thay %d tai lieu con lo, tong %d trang thieu (nguong %.0f%%)\n"
          % (len(lo), tong, g.nguong * 100))
    for rel, n, thieu in lo:
        print("  %-52s %d trang, thieu %s" % (os.path.basename(rel)[:52], n, thieu))
    if not g.chay:
        print("\n(chi xem. Them --chay de va that)")
        return
    if not lo:
        return

    KHOA = khoa()
    print("\nBat dau va — MOT luong, %d DPI, JPEG %d\n" % (g.dpi, g.chat))
    xong = hong = tok = 0
    t0 = time.time()
    for rel, n, thieu in lo:
        pdf = os.path.join(RAW, rel + ".pdf")
        if not os.path.exists(pdf):
            print("  ! khong thay PDF goc: %s" % rel); continue
        for tr in thieu:
            dich = os.path.join(OCR, rel, "trang_%03d.md" % tr)
            t1 = time.time()
            try:
                noi, t, kb = ocr(pdf, tr, g.dpi, g.chat, KHOA, g.han)
                io.open(dich, "w", encoding="utf-8").write(noi + "\n")
                xong += 1; tok += t
                print("  + %-40s tr%-4d %3d KB %6.1fs %5d tok %5d ky tu"
                      % (os.path.basename(rel)[:40], tr, kb, time.time() - t1, t, len(noi)), flush=True)
            except Exception as e:
                hong += 1
                print("  ! %-40s tr%-4d HONG sau %.1fs: %s"
                      % (os.path.basename(rel)[:40], tr, time.time() - t1, str(e)[:70]), flush=True)
    print("\nXONG: va duoc %d trang, hong %d, %d token, %.1f phut"
          % (xong, hong, tok, (time.time() - t0) / 60))


if __name__ == "__main__":
    main()
