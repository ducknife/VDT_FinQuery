# -*- coding: utf-8 -*-
"""Tien trinh phu: doc lenh JSON tung dong tu stdin, tra ket qua JSON tung dong ra stdout.

Lenh:
  {"id":1,"lenh":"sotrang","tep":"D:/.../a.pdf"}
  {"id":2,"lenh":"anh","tep":"D:/.../a.pdf","trang":0,"dpi":170}

Giu tien trinh song de khoi phai khoi dong lai Python cho tung trang
(khoi dong mat ~0.3s, nhan voi 10.000 trang la khong chap nhan duoc).
"""
import sys, json, base64, io

try:
    import pymupdf as fitz
except ImportError:
    import fitz

_mo = {}


def tai(tep):
    if tep not in _mo:
        _mo[tep] = fitz.open(tep)
    return _mo[tep]


def xu_ly(g):
    lenh = g.get("lenh")
    if lenh == "sotrang":
        d = tai(g["tep"])
        # dem luon so ky tu cua lop chu san co, de biet tep nao la ban quet
        ky_tu = sum(len(d[i].get_text().strip()) for i in range(min(d.page_count, 12)))
        return {"ok": True, "sotrang": d.page_count, "kytu_12trang_dau": ky_tu}

    if lenh == "anh":
        d = tai(g["tep"])
        pix = d[g["trang"]].get_pixmap(dpi=g.get("dpi", 170))
        # JPEG xam thay vi PNG mau: nhe hon ~4 lan ma chu quet khong khac gi
        from PIL import Image
        im = Image.frombytes("RGB", (pix.width, pix.height), pix.samples).convert("L")
        b = io.BytesIO()
        im.save(b, "JPEG", quality=g.get("chat_luong", 85), optimize=True)
        raw = b.getvalue()
        return {"ok": True, "b64": base64.b64encode(raw).decode(), "kieu": "image/jpeg",
                "byte": len(raw), "rong": pix.width, "cao": pix.height}

    if lenh == "dong":
        for d in _mo.values():
            try:
                d.close()
            except Exception:
                pass
        _mo.clear()
        return {"ok": True}

    return {"ok": False, "loi": "lenh la: " + str(lenh)}


def main():
    out = io.TextIOWrapper(sys.stdout.buffer, encoding="utf-8", newline="\n")
    for dong in sys.stdin:
        dong = dong.strip()
        if not dong:
            continue
        try:
            g = json.loads(dong)
        except Exception as e:
            out.write(json.dumps({"ok": False, "loi": "JSON hong: %s" % e}) + "\n")
            out.flush()
            continue
        try:
            kq = xu_ly(g)
        except Exception as e:
            kq = {"ok": False, "loi": "%s: %s" % (type(e).__name__, e)}
        kq["id"] = g.get("id")
        out.write(json.dumps(kq, ensure_ascii=False) + "\n")
        out.flush()


if __name__ == "__main__":
    main()
