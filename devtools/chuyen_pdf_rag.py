"""
Chuyển PDF (BCTN, BCTC, Nghị quyết ĐHĐCĐ của CTR) sang định dạng tối ưu cho RAG
phân cấp — đúng kỹ thuật tiền xử lý ở mục 4.3, Ke_Hoach_6_Tuan_FinQuery-RAG.md:

    "chunk theo mục, áp dụng table-to-json cho mọi bảng số (kỹ thuật Paper 1 —
    cải thiện NDCG@20 +5,9%), gắn metadata (loại tài liệu, năm, mục) để lọc
    trước khi rerank."

Đầu ra — MỖI PDF ra HAI file:
    <ten_pdf>.md            bản dựng lại đầy đủ dạng Markdown (đọc để soát lỗi
                             bằng mắt — heading giữ nguyên cấp, bảng hiện lại
                             dạng bảng cho dễ đối chiếu với PDF gốc)
    <ten_pdf>.chunks.jsonl  các chunk sẵn sàng nạp embedding, MỖI DÒNG 1 JSON

Vì sao JSONL chứ không phải CSV cho phần chunk: mỗi chunk là văn bản tự do
nhiều dòng (đoạn văn tiếng Việt, dấu phẩy, ngoặc kép) và với chunk bảng còn có
thêm một JSON lồng bên trong (bang_json). CSV không biểu diễn được cấu trúc
lồng, và escape sai một dấu phẩy/ngoặc kép là hỏng cả file. JSONL là chuẩn
thực tế cho pipeline embedding: đọc theo dòng (streaming), không cần load hết
vào RAM, mỗi dòng độc lập nên lỗi một chunk không hỏng cả file. Số liệu có
cấu trúc (BCTC theo quý/năm) đã có CSV riêng từ keo_du_lieu_vnstock.py — đây
là hai loại dữ liệu khác nhau, không dùng chung một định dạng.

Kỹ thuật table-to-json (theo đúng nguyên bản Paper 1): thay vì làm phẳng bảng
thành văn bản tuần tự (dễ mất mối liên hệ hàng-cột), mỗi bảng được chuyển
thành JSON {cot: [...], hang: [{cot1: giá_trị, cot2: giá_trị, ...}, ...]} —
tiêu đề cột trở thành khoá, mỗi hàng số liệu trở thành một object. Bên cạnh
đó có thêm bản "diễn dịch" ra câu văn (vd: "Doanh thu thuần — 2024: 12.610;
2025: 13.940") để model embedding nhúng được ngữ nghĩa hàng-cột, không chỉ
nhúng chuỗi số rời rạc.

GIẢ ĐỊNH ĐẦU VÀO: PDF dạng văn bản số hoá sẵn (không phải bản scan/ảnh — tool
này KHÔNG OCR). Nếu một file trích được quá ít chữ so với số trang, tool sẽ
cảnh báo để bạn xử lý riêng bằng OCR (xem pdf skill: pytesseract + pdf2image).

Đặt tên file đầu vào theo đúng quy ước đã chốt trong dự án để tool đoán đúng
loại tài liệu / năm / kỳ từ TÊN FILE (không phải đoán từ nội dung):
    CTR_<loai>_<nam>[_<ky>].pdf
    vd: CTR_BCTC_2024_KiemToan.pdf · CTR_BCTN_2023.pdf · CTR_NQ_DHDCD_2025.pdf
File không theo quy ước vẫn xử lý được (đoán năm bằng regex, loại = "khac"),
nhưng nên đổi tên trước để metadata chính xác — phục vụ lọc trước rerank.

CÀI ĐẶT:
    pip install pdfplumber

CHẠY:
    python chuyen_pdf_rag.py tai_lieu_CTR/                 # cả thư mục (đệ quy)
    python chuyen_pdf_rag.py CTR_BCTN_2023.pdf              # một file
    python chuyen_pdf_rag.py tai_lieu_CTR/ --ra out_rag/    # đổi thư mục ghi ra
    python chuyen_pdf_rag.py tai_lieu_CTR/ --xem-truoc      # chỉ in thống kê
"""

from __future__ import annotations

import argparse
import json
import re
import sys
import unicodedata
from collections import Counter
from dataclasses import dataclass, asdict, field
from pathlib import Path

import pdfplumber

# ----------------------------------------------------------------------------
# CẤU HÌNH
# ----------------------------------------------------------------------------

CHUNK_TOI_DA_MAC_DINH = 1200   # số ký tự tối đa cho một chunk văn bản
CHONG_LAN_MAC_DINH = 150       # số ký tự chồng lấn giữa hai chunk văn bản liền kề
NGUONG_SCAN_KY_TU_MOI_TRANG = 20  # dưới mức này/trang -> nghi là bản scan

LOAI_TU_KHOA = [
    # THỨ TỰ QUAN TRỌNG: xét từ khoá cụ thể nhất trước, giống crawl_tai_lieu_ctr.py
    ("nghi_quyet_dhdcd", ("nghi-quyet", "nghiquyet", "_nq_", "nq_", "dhdcd",
                          "dai-hoi", "daihoi", "bien-ban", "resolution")),
    ("bao_cao_quan_tri", ("quan-tri", "quantri", "bcqt", "governance")),
    ("bao_cao_thuong_nien", ("bctn", "bao-cao-thuong-nien", "baocaothuongnien",
                             "thuong-nien", "annual")),
    ("bao_cao_tai_chinh", ("bctc", "tai-chinh", "taichinh", "financial",
                           "kiem-toan", "kiemtoan")),
]

NAM_RE = re.compile(r"(?<!\d)(20[0-3]\d)(?!\d)")
KY_RE = re.compile(r"[Qq](?:uy)?[_\-]?([1-4])\b")

RE_MUC_LA_MA = re.compile(r"^[IVXLC]{1,6}[\.\)]\s+\S")
RE_MUC_SO = re.compile(r"^\d{1,2}(\.\d{1,2}){0,3}[\.\)]\s+\S")
RE_MUC_CHU = re.compile(r"^[a-z][\.\)]\s+\S")

# ----------------------------------------------------------------------------


def khong_dau(s: str) -> str:
    s = unicodedata.normalize("NFD", s)
    s = "".join(c for c in s if unicodedata.category(c) != "Mn")
    return s.replace("đ", "d").replace("Đ", "D").lower()


TEN_LOAI_HOP_LE = {ten_loai for ten_loai, _ in LOAI_TU_KHOA}


def doan_metadata(duong_dan: Path) -> dict:
    """
    Đoán loại tài liệu / năm / kỳ. File tải về bằng crawl_tai_lieu_ctr.py hay
    tai_bao_cao_ctr.py KHÔNG được đổi tên theo quy ước CTR_<loai>_<nam> — hai
    tool đó chỉ XẾP vào thư mục <loai>/<nam>/ và giữ nguyên tên gốc lấy từ web
    (nhiều khi không có năm/từ khoá gì trong tên). Nên phải đoán theo thứ tự:

      1. Tên thư mục cha khớp THẲNG với đúng khoá phân loại đã dùng lúc tải
         (tin cậy cao nhất — do chính tool tải xếp, không phải suy đoán)
      2. Từ khoá trong tên file, rồi mới tới từ khoá trong tên thư mục cha
      3. Số năm: tìm trong tên file trước, không có mới tìm trong thư mục cha
         (thư mục "2024/" do tool tải đặt tên đúng bằng năm)
    """
    ten_file = duong_dan.name
    thu_muc_cha = [p.name for p in duong_dan.parents[:3]]
    tin_hieu = [ten_file] + thu_muc_cha

    loai = next((ten for ten in thu_muc_cha if ten in TEN_LOAI_HOP_LE), None)
    if loai is None:
        gop = khong_dau(" ".join(tin_hieu))
        loai = next((tl for tl, tu_khoa in LOAI_TU_KHOA if any(tk in gop for tk in tu_khoa)), "khac")

    nam = None
    for nguon in tin_hieu:
        m = NAM_RE.findall(nguon)
        if m:
            nam = int(m[0])
            break

    ky = None
    for nguon in tin_hieu:
        m = KY_RE.search(nguon)
        if m:
            ky = f"Q{m.group(1)}"
            break

    return {"loai_tai_lieu": loai, "nam": nam, "ky": ky}


# -- table-to-json -------------------------------------------------------


def don_gian_o(o) -> str:
    return "" if o is None else " ".join(str(o).split())


def bang_sang_json(bang_tho: list[list]) -> dict | None:
    """
    Kỹ thuật table-to-json (Paper 1): tiêu đề cột -> khoá, mỗi hàng số liệu ->
    1 object. Nếu hàng đầu không đủ giống tiêu đề (quá nhiều ô rỗng), giữ
    nguyên dạng mảng-hàng thay vì đoán bừa.
    """
    if not bang_tho:
        return None
    bang = [[don_gian_o(o) for o in hang] for hang in bang_tho]
    bang = [hang for hang in bang if any(hang)]
    if not bang:
        return None

    tieu_de = bang[0]
    so_o_co_chu = sum(1 for o in tieu_de if o)
    if so_o_co_chu >= max(2, len(tieu_de) // 2) and len(bang) > 1:
        cols, dem = [], {}
        for i, o in enumerate(tieu_de):
            khoa = o or f"cot_{i + 1}"
            n = dem.get(khoa, 0)
            dem[khoa] = n + 1
            cols.append(khoa if n == 0 else f"{khoa}_{n + 1}")
        hang_json = []
        for hang in bang[1:]:
            hang = hang + [""] * (len(cols) - len(hang))
            hang_json.append({cols[i]: hang[i] for i in range(len(cols))})
        return {"cot": cols, "hang": hang_json}

    return {"cot": None, "hang": bang}


def bang_thanh_van_ban(bang_json: dict, chu_thich: str = "") -> str:
    """Diễn dịch bảng JSON thành câu văn để nhúng embedding không mất ngữ nghĩa hàng-cột."""
    dong = [chu_thich] if chu_thich else []
    if bang_json["cot"]:
        khoa_chinh = bang_json["cot"][0]
        for hang in bang_json["hang"]:
            nhan = hang.get(khoa_chinh, "").strip()
            phan = [f"{c}: {hang[c]}" for c in bang_json["cot"][1:] if hang.get(c, "").strip()]
            if nhan and phan:
                dong.append(f"{nhan} — " + "; ".join(phan))
            elif nhan or phan:
                dong.append(nhan or "; ".join(phan))
    else:
        for hang in bang_json["hang"]:
            dong.append(" | ".join(hang))
    return "\n".join(dong)


def bang_sang_markdown(bang_json: dict) -> str:
    if bang_json["cot"]:
        cols = bang_json["cot"]
        dong = ["| " + " | ".join(cols) + " |", "|" + "---|" * len(cols)]
        for hang in bang_json["hang"]:
            dong.append("| " + " | ".join(hang.get(c, "") for c in cols) + " |")
    else:
        dong = ["| " + " | ".join(hang) + " |" for hang in bang_json["hang"]]
        if dong:
            dong.insert(1, "|" + "---|" * len(bang_json["hang"][0]))
    return "\n".join(dong)


# -- trích khối nội dung theo thứ tự đọc trên từng trang ------------------


@dataclass
class Khoi:
    loai: str          # "heading" | "doan_van" | "bang"
    trang: int
    noi_dung: object    # str với heading/doan_van, dict (bang_json) với bang
    cap: int = 0        # cấp heading (1 = lớn nhất), chỉ dùng khi loai=="heading"


def co_chu_pho_bien(trang_list) -> float:
    """Cỡ chữ xuất hiện nhiều nhất trong cả tài liệu -> coi là cỡ chữ văn bản thường."""
    dem = Counter()
    for trang in trang_list:
        for w in trang.extract_words(extra_attrs=["size"]):
            dem[round(w["size"] * 2) / 2] += 1
    return dem.most_common(1)[0][0] if dem else 10.0


def trong_bbox(x0, top, x1, bottom, hop) -> bool:
    hx0, htop, hx1, hbottom = hop
    cx, cy = (x0 + x1) / 2, (top + bottom) / 2
    return hx0 <= cx <= hx1 and htop <= cy <= hbottom


def la_heading(text: str, co_chu: float, in_dam: bool, co_chu_pho_bien: float) -> int:
    """Trả về cấp heading (1,2,3) nếu là heading, 0 nếu là văn bản thường."""
    text = text.strip()
    if not text or len(text) > 140:
        return 0
    ung_vien = False
    cap = 3
    if RE_MUC_LA_MA.match(text):
        ung_vien, cap = True, 1
    elif RE_MUC_SO.match(text):
        tien_to = text[: text.find(" ")]
        so_nhom = len(re.findall(r"\d+", tien_to))   # "1." -> 1 -> cấp 2; "2.1." -> 2 -> cấp 3
        cap = min(1 + so_nhom, 3)
        ung_vien = True
    elif RE_MUC_CHU.match(text):
        ung_vien, cap = True, 3
    elif co_chu >= co_chu_pho_bien + 2.5:
        ung_vien, cap = True, 1
    elif co_chu >= co_chu_pho_bien + 1 or (in_dam and len(text.split()) <= 14):
        ung_vien, cap = True, 2
    elif text.isupper() and 3 <= len(text.split()) <= 14:
        ung_vien, cap = True, 2
    return cap if ung_vien else 0


def trich_khoi_pdf(pdf: "pdfplumber.PDF", canh_bao: list[str]) -> list[Khoi]:
    body_size = co_chu_pho_bien(pdf.pages)
    ket_qua: list[Khoi] = []
    tong_ky_tu = 0

    for so_trang, trang in enumerate(pdf.pages, start=1):
        muc_moi: list[tuple[float, Khoi]] = []

        # 1) bảng trước — cần bbox để loại từ nằm trong bảng khỏi luồng văn bản
        try:
            bang_tho_list = [(t.bbox, t.extract()) for t in trang.find_tables()]
        except Exception as e:
            bang_tho_list = []
            canh_bao.append(f"trang {so_trang}: lỗi dò bảng ({type(e).__name__})")

        hop_bang = []
        for bbox, bang_tho in bang_tho_list:
            bj = bang_sang_json(bang_tho)
            if bj is None:
                continue
            hop_bang.append(bbox)
            muc_moi.append((bbox[1], Khoi("bang", so_trang, bj)))

        # 2) chữ, loại bỏ chữ nằm trong vùng bảng
        try:
            words = trang.extract_words(extra_attrs=["size", "fontname"])
        except Exception as e:
            words = []
            canh_bao.append(f"trang {so_trang}: lỗi trích chữ ({type(e).__name__})")

        words = [
            w for w in words
            if not any(trong_bbox(w["x0"], w["top"], w["x1"], w["bottom"], h) for h in hop_bang)
        ]
        tong_ky_tu += sum(len(w["text"]) for w in words)

        # gộp theo dòng (cùng "top", sai số nhỏ)
        dong: dict[int, list] = {}
        for w in words:
            khoa = round(w["top"] / 3)
            dong.setdefault(khoa, []).append(w)

        dong_sx = sorted(dong.items())
        i = 0
        while i < len(dong_sx):
            _, tu_list = dong_sx[i]
            tu_list.sort(key=lambda w: w["x0"])
            text = " ".join(w["text"] for w in tu_list)
            co_chu_max = max(w["size"] for w in tu_list)
            in_dam = any("bold" in w["fontname"].lower() for w in tu_list)
            top = min(w["top"] for w in tu_list)
            cap = la_heading(text, co_chu_max, in_dam, body_size)
            if cap:
                muc_moi.append((top, Khoi("heading", so_trang, text, cap)))
                i += 1
                continue
            # gộp các dòng liên tiếp không phải heading thành 1 đoạn văn
            doan, top_dau = [text], top
            j = i + 1
            while j < len(dong_sx):
                _, tu_ke = dong_sx[j]
                tu_ke.sort(key=lambda w: w["x0"])
                text_ke = " ".join(w["text"] for w in tu_ke)
                co_chu_ke = max(w["size"] for w in tu_ke)
                in_dam_ke = any("bold" in w["fontname"].lower() for w in tu_ke)
                if la_heading(text_ke, co_chu_ke, in_dam_ke, body_size):
                    break
                doan.append(text_ke)
                j += 1
            muc_moi.append((top_dau, Khoi("doan_van", so_trang, " ".join(doan))))
            i = j

        muc_moi.sort(key=lambda x: x[0])
        ket_qua.extend(k for _, k in muc_moi)

    if pdf.pages and tong_ky_tu / len(pdf.pages) < NGUONG_SCAN_KY_TU_MOI_TRANG:
        canh_bao.append(
            f"chỉ trích được {tong_ky_tu} ký tự / {len(pdf.pages)} trang — "
            "file này nhiều khả năng là bản SCAN/ẢNH, cần OCR riêng (tool này bỏ qua OCR)."
        )
    return ket_qua


# -- gộp khối thành chunk --------------------------------------------------


@dataclass
class Chunk:
    chunk_id: str
    nguon_file: str
    loai_tai_lieu: str
    nam: int | None
    ky: str | None
    muc: str
    trang_bat_dau: int
    trang_ket_thuc: int
    loai_chunk: str          # "van_ban" | "bang"
    noi_dung: str            # text dùng để nhúng embedding
    bang_json: dict | None = None


def gop_thanh_chunk(
    khoi_list: list[Khoi], meta: dict, ten_file: str,
    chunk_toi_da: int, chong_lan: int,
) -> list[Chunk]:
    chunks: list[Chunk] = []
    stt = 0

    duong_dan_muc: list[str] = []  # ngăn xếp heading -> đường dẫn "mục" hiện tại
    cap_hien_tai: list[int] = []

    dem_van_ban: list[str] = []
    trang_dau_hien, trang_cuoi_hien = None, None

    def duong_dan_str() -> str:
        return " > ".join(duong_dan_muc) if duong_dan_muc else "(không mục)"

    def xa_chunk_van_ban(giu_lai_chong_lan: bool):
        nonlocal stt, trang_dau_hien, trang_cuoi_hien
        noi_dung = "\n".join(dem_van_ban).strip()
        if noi_dung:
            stt += 1
            chunks.append(Chunk(
                chunk_id=f"{ten_file}::{stt:04d}",
                nguon_file=ten_file,
                loai_tai_lieu=meta["loai_tai_lieu"],
                nam=meta["nam"],
                ky=meta["ky"],
                muc=duong_dan_str(),
                trang_bat_dau=trang_dau_hien or 0,
                trang_ket_thuc=trang_cuoi_hien or 0,
                loai_chunk="van_ban",
                noi_dung=noi_dung,
            ))
        dem_van_ban.clear()
        trang_dau_hien, trang_cuoi_hien = None, None
        if giu_lai_chong_lan and noi_dung and chong_lan > 0:
            dem_van_ban.append(noi_dung[-chong_lan:])

    for k in khoi_list:
        if k.loai == "heading":
            xa_chunk_van_ban(giu_lai_chong_lan=False)
            while cap_hien_tai and cap_hien_tai[-1] >= k.cap:
                cap_hien_tai.pop()
                duong_dan_muc.pop()
            cap_hien_tai.append(k.cap)
            duong_dan_muc.append(k.noi_dung)
            continue

        if k.loai == "bang":
            xa_chunk_van_ban(giu_lai_chong_lan=False)
            stt += 1
            bj = k.noi_dung
            chu_thich = f"Bảng trong mục: {duong_dan_str()}"
            chunks.append(Chunk(
                chunk_id=f"{ten_file}::{stt:04d}",
                nguon_file=ten_file,
                loai_tai_lieu=meta["loai_tai_lieu"],
                nam=meta["nam"],
                ky=meta["ky"],
                muc=duong_dan_str(),
                trang_bat_dau=k.trang,
                trang_ket_thuc=k.trang,
                loai_chunk="bang",
                noi_dung=bang_thanh_van_ban(bj, chu_thich),
                bang_json=bj,
            ))
            continue

        # đoạn văn
        trang_dau_hien = trang_dau_hien or k.trang
        trang_cuoi_hien = k.trang
        dem_van_ban.append(k.noi_dung)
        do_dai = sum(len(d) for d in dem_van_ban)
        if do_dai >= chunk_toi_da:
            xa_chunk_van_ban(giu_lai_chong_lan=True)

    xa_chunk_van_ban(giu_lai_chong_lan=False)
    return chunks


# -- dựng lại Markdown để soát lỗi -----------------------------------------


def dung_markdown(khoi_list: list[Khoi], meta: dict, ten_file: str) -> str:
    dong = [
        f"<!-- nguồn: {ten_file} | loại: {meta['loai_tai_lieu']} | "
        f"năm: {meta['nam']} | kỳ: {meta['ky']} -->",
        "",
    ]
    for k in khoi_list:
        if k.loai == "heading":
            dong.append(f"{'#' * min(k.cap + 1, 6)} {k.noi_dung}")
            dong.append("")
        elif k.loai == "doan_van":
            dong.append(k.noi_dung)
            dong.append("")
        elif k.loai == "bang":
            dong.append(f"_[Bảng, trang {k.trang}]_")
            dong.append("")
            dong.append(bang_sang_markdown(k.noi_dung))
            dong.append("")
    return "\n".join(dong)


# -- luồng chính ------------------------------------------------------------


def xu_ly_mot_file(duong_dan: Path, thu_muc_ra: Path, chunk_toi_da: int,
                    chong_lan: int, xem_truoc: bool) -> dict:
    meta = doan_metadata(duong_dan)
    canh_bao: list[str] = []
    try:
        with pdfplumber.open(duong_dan) as pdf:
            khoi_list = trich_khoi_pdf(pdf, canh_bao)
    except Exception as e:
        return {"file": duong_dan.name, "loi": f"{type(e).__name__}: {e}"}

    chunks = gop_thanh_chunk(khoi_list, meta, duong_dan.stem, chunk_toi_da, chong_lan)
    md = dung_markdown(khoi_list, meta, duong_dan.name)

    so_bang = sum(1 for c in chunks if c.loai_chunk == "bang")
    if not xem_truoc:
        thu_muc_ra.mkdir(parents=True, exist_ok=True)
        (thu_muc_ra / f"{duong_dan.stem}.md").write_text(md, encoding="utf-8")
        with open(thu_muc_ra / f"{duong_dan.stem}.chunks.jsonl", "w", encoding="utf-8") as f:
            for c in chunks:
                f.write(json.dumps(asdict(c), ensure_ascii=False) + "\n")

    return {
        "file": duong_dan.name, "meta": meta, "so_chunk": len(chunks),
        "so_bang": so_bang, "canh_bao": canh_bao,
    }


def main():
    ap = argparse.ArgumentParser(
        description="Chuyển PDF CTR sang Markdown + chunk JSONL tối ưu cho RAG phân cấp")
    ap.add_argument("dau_vao", help="File PDF hoặc thư mục chứa PDF (quét đệ quy)")
    ap.add_argument("--ra", default="tai_lieu_CTR_rag", help="Thư mục ghi kết quả")
    ap.add_argument("--chunk-toi-da", type=int, default=CHUNK_TOI_DA_MAC_DINH,
                     help=f"Số ký tự tối đa mỗi chunk văn bản (mặc định {CHUNK_TOI_DA_MAC_DINH})")
    ap.add_argument("--chong-lan", type=int, default=CHONG_LAN_MAC_DINH,
                     help=f"Số ký tự chồng lấn giữa 2 chunk liên tiếp (mặc định {CHONG_LAN_MAC_DINH})")
    ap.add_argument("--xem-truoc", action="store_true", help="Chỉ in thống kê, không ghi file")
    args = ap.parse_args()

    nguon = Path(args.dau_vao)
    if nguon.is_file():
        danh_sach = [nguon]
    elif nguon.is_dir():
        danh_sach = sorted(nguon.rglob("*.pdf"))
    else:
        print(f"Không tìm thấy: {nguon}")
        sys.exit(1)

    if not danh_sach:
        print("Không có file PDF nào để xử lý.")
        sys.exit(1)

    print("=" * 74)
    print(f"CHUYỂN {len(danh_sach)} PDF SANG ĐỊNH DẠNG RAG"
          f"{'   [XEM TRƯỚC]' if args.xem_truoc else ''}")
    print("=" * 74)

    thu_muc_ra = Path(args.ra)
    tong_chunk = tong_bang = 0
    loi_list = []

    for f in danh_sach:
        print(f"\n> {f}")
        kq = xu_ly_mot_file(f, thu_muc_ra, args.chunk_toi_da, args.chong_lan, args.xem_truoc)
        if "loi" in kq:
            print(f"  LỖI: {kq['loi']}")
            loi_list.append(f"{f.name}: {kq['loi']}")
            continue
        m = kq["meta"]
        print(f"  [{m['loai_tai_lieu']:20s}] năm {m['nam']}"
              f"{' kỳ ' + m['ky'] if m['ky'] else ''}")
        print(f"  {kq['so_chunk']} chunk ({kq['so_bang']} bảng, "
              f"{kq['so_chunk'] - kq['so_bang']} văn bản)")
        for c in kq["canh_bao"]:
            print(f"  ⚠ {c}")
        tong_chunk += kq["so_chunk"]
        tong_bang += kq["so_bang"]

    print("\n" + "=" * 74)
    print("TỔNG KẾT")
    print("=" * 74)
    print(f"  {len(danh_sach) - len(loi_list)}/{len(danh_sach)} file xử lý thành công")
    print(f"  Tổng {tong_chunk} chunk ({tong_bang} bảng, {tong_chunk - tong_bang} văn bản)")
    if not args.xem_truoc:
        print(f"  Đã ghi vào: {thu_muc_ra}/")
    if loi_list:
        print(f"\n  {len(loi_list)} file lỗi:")
        for l in loi_list:
            print(f"      - {l}")
    if args.xem_truoc:
        print("\nChạy lại không có --xem-truoc để ghi file .md và .chunks.jsonl")


if __name__ == "__main__":
    main()
