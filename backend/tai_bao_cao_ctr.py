"""
Tải toàn bộ tài liệu công bố của Viettel Construction (CTR) và xếp theo loại.

Phiên bản 2 (2026-09-17) — viết lại hoàn toàn so với bản dùng Playwright:
  * Website chỉ chặn bot bằng một cookie tĩnh (trang trả về đoạn JS
    `document.cookie="D1N=..."` rồi reload). Đặt lại cookie đó là requests
    lấy được HTML đầy đủ -> KHÔNG cần trình duyệt, nhanh hơn nhiều lần và tải
    song song được.
  * Bản cũ click từng ô năm, nhưng HTML đã chứa SẴN bảng của mọi năm (chỉ ẩn
    bằng CSS) -> việc click không lọc gì, và bản cũ bỏ sót ~83/172 file BCTC.
    Bản này đọc thẳng năm từ class/id `y-YYYY` của từng bảng.
  * Tiêu đề hiển thị trên web (chứ không phải tên file) được dùng để phân loại,
    đoán kỳ báo cáo, nhận biết bản tiếng Anh.

Nguồn (xem logs/du-lieu_v1.0.md để biết lý do chọn):
    01 bao_cao_tai_chinh    /bao-cao-tai-chinh/
    02 bao_cao_thuong_nien  /bao-cao-thuong-nien/
    03 dai_hoi_dong_co_dong /quan-he-co-dong/dai-hoi-dong-co-dong/
    04 cong_bo_thong_tin    /quan-he-co-dong/cong-bo-thong-tin/  (lọc theo từ khoá)
    05 bao_cao_quan_tri     /quan-he-co-dong/quan-tri-doanh-nghiep/bao-cao-quan-tri/
    06 dieu_le_quy_che      /quan-he-co-dong/quan-tri-doanh-nghiep/dieu-le-va-cac-quy-dinh/
    07 ban_cao_bach         /quan-he-co-dong/quan-tri-doanh-nghiep/ban-cao-bach/
    08 du_lieu_web          lịch sử cổ tức, lịch sử tăng vốn, cơ cấu sở hữu (bảng HTML -> .md)

Đầu ra:  data/raw/CTR/<nguon>/<nam>/<nhom>/<file>
         data/raw/CTR/manifest.csv   mọi mục tìm thấy, kể cả mục bị bỏ qua + lý do

CÀI ĐẶT:  pip install requests beautifulsoup4
CHẠY:
    python tai_bao_cao_ctr.py                     # tải 2019–2026
    python tai_bao_cao_ctr.py --liet-ke           # chỉ ghi manifest, không tải
    python tai_bao_cao_ctr.py --tu 2021 --den 2025
    python tai_bao_cao_ctr.py --tat-ca            # không bỏ qua văn bản hành chính / tiếng Anh
Chạy lại an toàn: file đã có (đúng kích thước) sẽ được bỏ qua.
"""

from __future__ import annotations

import argparse
import csv
import hashlib
import re
import sys
import time
import unicodedata
from concurrent.futures import ThreadPoolExecutor, as_completed
from dataclasses import dataclass, asdict
from datetime import date
from pathlib import Path
from urllib.parse import urljoin, urlparse, unquote

import requests
from bs4 import BeautifulSoup, Tag

# ----------------------------------------------------------------------------
# CẤU HÌNH
# ----------------------------------------------------------------------------

GOC = "https://viettelconstruction.com.vn"
QHCD = f"{GOC}/quan-he-co-dong"

# (mã, thư mục, url, cách xếp năm)
#   "ky"  : năm lấy theo KỲ BÁO CÁO đoán từ tiêu đề (BCTC năm 2025 công bố 2026 -> 2025)
#   "tab" : năm lấy theo tab năm trên web (= năm đăng / năm diễn ra sự kiện)
#   None  : không chia thư mục năm
NGUON = [
    ("bctc",  "01_bao_cao_tai_chinh",    f"{GOC}/bao-cao-tai-chinh/",                            "ky"),
    ("bctn",  "02_bao_cao_thuong_nien",  f"{GOC}/bao-cao-thuong-nien/",                          "ky"),
    ("dhdcd", "03_dai_hoi_dong_co_dong", f"{QHCD}/dai-hoi-dong-co-dong/",                         "tab"),
    ("cbtt",  "04_cong_bo_thong_tin",    f"{QHCD}/cong-bo-thong-tin/",                            "tab"),
    ("bcqt",  "05_bao_cao_quan_tri",     f"{QHCD}/quan-tri-doanh-nghiep/bao-cao-quan-tri/",       "ky"),
    ("dieule","06_dieu_le_quy_che",      f"{QHCD}/quan-tri-doanh-nghiep/dieu-le-va-cac-quy-dinh/", None),
    ("cabach","07_ban_cao_bach",         f"{QHCD}/quan-tri-doanh-nghiep/ban-cao-bach/",           None),
]

# Các trang "Thông tin cổ đông" dựng bảng bằng JS từ API JSON này -> gọi thẳng API.
API = f"{GOC}/wp-json/quanhecodong/v1"
DU_LIEU_WEB = {
    # tên file: (phương thức, endpoint, trang hiển thị, tiêu đề cột {khoá JSON: tên cột})
    "lich_su_co_tuc": ("POST", "getDividendHistory", f"{QHCD}/thong-tin-co-dong/lich-su-co-tuc/",
                       {"year": "Năm", "period": "Kỳ", "ratio": "Tỷ lệ (%)", "gdkhq_date": "Ngày GDKHQ",
                        "last_regis_date": "Ngày ĐKCC", "pay_date": "Ngày thanh toán",
                        "dividend_type": "Hình thức"}),
    "lich_su_tang_von": ("POST", "getDataCrHistory", f"{QHCD}/thong-tin-co-dong/lich-su-tang-von/",
                         {"year": "Năm", "timeline": "Ngày", "authorized_capital": "Vốn điều lệ sau (VNĐ)",
                          "amount_change": "Tăng/giảm (VNĐ)", "reason": "Lý do"}),
    "co_cau_so_huu__loai_co_phieu": ("GET", "getDataBaseOnShare", f"{QHCD}/thong-tin-co-dong/co-cau-so-huu/", None),
    "co_cau_so_huu__doi_tuong": ("GET", "getDataBaseOnObject", f"{QHCD}/thong-tin-co-dong/co-cau-so-huu/", None),
    "co_cau_so_huu__dia_ly": ("GET", "getDataBaseOnGeo", f"{QHCD}/thong-tin-co-dong/co-cau-so-huu/", None),
    "co_cau_so_huu__co_dong_lon": ("GET", "getDataBaseOnShareHolder", f"{QHCD}/thong-tin-co-dong/co-cau-so-huu/", None),
    "co_cau_so_huu__quy_mo_so_huu": ("GET", "getDataBaseOnStock", f"{QHCD}/thong-tin-co-dong/co-cau-so-huu/", None),
}

THU_MUC_LUU = Path(__file__).resolve().parents[1] / "data" / "raw" / "CTR"
NAM_TU, NAM_DEN = 2019, 2026
DUOI = (".pdf", ".doc", ".docx", ".xls", ".xlsx", ".zip", ".rar")
SO_LUONG_TAI = 6

NAM_RE = re.compile(r"(?<!\d)(20[0-3]\d)(?!\d)")

# ----------------------------------------------------------------------------
# TIỆN ÍCH CHUỖI
# ----------------------------------------------------------------------------


def khong_dau(s: str) -> str:
    s = unicodedata.normalize("NFD", s or "")
    s = "".join(c for c in s if unicodedata.category(c) != "Mn")
    return s.replace("đ", "d").replace("Đ", "D").lower()


def co(s: str, *tu: str) -> bool:
    """s (đã bỏ dấu) có chứa một trong các cụm từ (viết không dấu, khớp trọn từ) không."""
    return any(re.search(rf"(?<![a-z0-9]){re.escape(t)}(?![a-z0-9])", s) for t in tu)


def theo_luat(k: str, luat: list[tuple[str, tuple[str, ...], str]],
              mac_dinh: tuple[str, str]) -> tuple[str, str]:
    """Luật đầu tiên khớp thắng. Mỗi luật: (nhóm, từ khoá, lý do bỏ qua — rỗng nếu giữ)."""
    for nhom, tu, ly_do in luat:
        if co(k, *tu):
            return nhom, ly_do
    return mac_dinh


def ten_sach(url: str) -> str:
    ten = unquote(Path(urlparse(url).path).name) or "tai_lieu.pdf"
    ten = unicodedata.normalize("NFC", ten)
    return re.sub(r"[^\w\-. ]", "_", ten)[:150]


CHU_VIET = set("ăâđêôơưàảãáạằẳẵắặầẩẫấậèẻẽéẹềểễếệìỉĩíịòỏõóọồổỗốộờởỡớợùủũúụừửữứựỳỷỹýỵ")


def ngon_ngu(tieu_de: str, url: str) -> str:
    """'vi' | 'en' | 'vi+en' — dựa vào tiêu đề hiển thị, bổ sung bằng tên file."""
    t = tieu_de.lower()
    k = khong_dau(tieu_de) + " " + khong_dau(ten_sach(url))
    if co(k, "tv+ta", "tv-ta", "tvta", "vi-en", "vieeng", "vi_en", "vien", "tieng viet va tieng anh",
          "_viet-anh", "song ngu"):
        return "vi+en"
    if any(c in CHU_VIET for c in t):
        return "vi"
    if re.search(r"\b(report|statement|financial|resolution|minute|meeting|notice|announcement|"
                 r"disclosure|shareholder|regulation|article|governance|annual|explanation|"
                 r"change|dividend|board|document|interim|consolidated|separate|quarter|"
                 r"organization|proposal)s?\b", t):
        return "en"
    if co(khong_dau(ten_sach(url)), "_ta.", "-ta.", "_ta-", "-en.", "_en.", "english"):
        return "en"
    return "vi"


# ----------------------------------------------------------------------------
# ĐOÁN KỲ BÁO CÁO & PHÂN NHÓM
# ----------------------------------------------------------------------------


def doan_nam_ky(tieu_de: str, url: str) -> int | None:
    n = NAM_RE.findall(tieu_de)
    if n:
        return max(int(x) for x in n)
    n = NAM_RE.findall(ten_sach(url))
    # tên file hay mở đầu bằng ngày đăng (20260807-...) -> chỉ lấy khi không có gì khác
    return max(int(x) for x in n) if n else None


def doan_ky(tieu_de: str) -> str:
    k = khong_dau(tieu_de)
    m = re.search(r"quy\s*(1|2|3|4|i{1,3}|iv)\b", k)
    if m:
        return "Q" + {"i": "1", "ii": "2", "iii": "3", "iv": "4"}.get(m.group(1), m.group(1))
    if co(k, "ban nien", "giua nien do", "6 thang", "06 thang", "sau thang"):
        return "6T"
    if co(k, "9 thang", "09 thang"):
        return "9T"
    if co(k, "nam"):
        return "N"
    return ""


def nhom_bctc(k: str) -> tuple[str, str]:
    """(nhóm, lý do bỏ qua hoặc '')"""
    if co(k, "giai trinh"):
        return "giai_trinh", ""
    if co(k, "hop dong kiem toan", "lua chon don vi kiem toan", "gia han", "chap thuan", "cham cong bo",
          "cbtt ky", "ky hop dong"):
        return "hanh_chinh", "văn bản hành chính (hợp đồng kiểm toán, gia hạn...)"
    if co(k, "hop nhat", "consolidated"):
        return "hop_nhat", ""
    if co(k, "rieng", "cong ty me", "separate"):
        return "rieng", ""
    return "khac", ""


def nhom_bctn(k: str) -> tuple[str, str]:
    if co(k, "ben vung", "sustainab"):
        return "phat_trien_ben_vung", ""
    return "thuong_nien", ""


LY_DO_THU_TUC = "thủ tục tổ chức họp (thư mời, uỷ quyền, chương trình, quy chế họp, đề cử...)"
LY_DO_QUAN_TRI = "điều lệ / quy chế / nhân sự — ngoài phạm vi A–F (điều lệ hiện hành có ở nguồn 06)"

LUAT_DHDCD = [
    ("thu_tuc", ("mau",), LY_DO_THU_TUC),
    ("nghi_quyet_bien_ban", ("nghi quyet", "bien ban", "resolution", "minutes"), ""),
    ("du_thao_dieu_le", ("sua doi dieu le", "sua doi, bo sung dieu le", "du thao dieu le"), LY_DO_QUAN_TRI),
    ("to_trinh", ("to trinh", "ttr"), ""),
    ("thu_tuc", ("thu moi", "giay moi", "giay uy quyen", "mau uy quyen", "guq", "mau", "huong dan",
                 "the bieu quyet", "phieu bieu quyet", "phieu bau", "chuong trinh", "quy che",
                 "qc", "don de cu", "ung cu", "de cu", "ung vien", "syll", "thong bao",
                 "danh sach co dong", "dang ky du", "dang ky tham du", "regulations"), LY_DO_THU_TUC),
    ("du_thao_dieu_le", ("du thao",), LY_DO_QUAN_TRI),
    ("to_trinh", ("to trinh", "ttr", "proposal", "thong qua", "lua chon", "phan phoi", "ppln",
                  "niem yet", "mien nhiem", "nganh nghe", "giao dich voi"), ""),
    ("bao_cao", ("bao cao", "bc", "report"), ""),
    ("tai_lieu_hop", ("tai lieu", "documents"), ""),
]


def nhom_dhdcd(k: str) -> tuple[str, str]:
    return theo_luat(k, LUAT_DHDCD, ("khac", ""))


# Nhóm CBTT: chỉ giữ những gì phục vụ mảng A (vốn), D (đầu tư), F (cổ tức) và các
# nghị quyết/quyết định HĐQT. Thứ tự quan trọng: cụ thể trước, chung sau.
LY_DO_CBTT = "CBTT ngoài phạm vi A–F (nhân sự, đăng ký kinh doanh, giao dịch người nội bộ...)"
LY_DO_HANH_CHINH = "văn bản hành chính (hợp đồng kiểm toán, gia hạn, tổ chức họp...)"

LUAT_CBTT = [
    # --- bỏ qua trước: các mẫu dễ khớp nhầm vào nhóm giữ lại ---
    ("hanh_chinh", ("hop dong kiem toan", "tam hoan", "gia han cong bo", "duong dan tai lieu",
                    "thong bao to chuc", "thay doi thoi gian to chuc", "chot lai danh sach",
                    "huy danh sach", "thong qua ke hoach to chuc", "phong kiem toan noi bo",
                    "thay doi ngay dang ky cuoi cung"), LY_DO_HANH_CHINH),
    ("hanh_chinh", ("quyen tham du", "tham du dhdcd", "tham du dai hoi"),
     "ĐKCC tham dự ĐHĐCĐ — thủ tục họp"),
    ("hanh_chinh", ("ngay dang ky cuoi cung thuc hien lay y kien",), LY_DO_HANH_CHINH),
    ("khac", ("nguoi noi bo", "nguoi lien quan cua", "nguoi lien quan voi", "co dong lon",
              "nhan su", "bo nhiem", "mien nhiem", "tu nhiem", "de cu", "dang ky doanh nghiep",
              "so huu nuoc ngoai", "quan tri cong ty", "tinh hinh quan tri", "xu phat",
              "thong tin lien he", "kien toan", "chi nhanh"), LY_DO_CBTT),
    # --- giữ lại ---
    ("co_tuc", ("co tuc", "loi nhuan con lai"), ""),
    ("phat_hanh_tang_von", ("phat hanh", "esop", "tang von", "von dieu le", "niem yet",
                            "dang ky bo sung", "giao dich dau tien", "chao ban", "co phieu quy",
                            "trai phieu", "so luong co phieu", "dang ky giao dich", "chuyen san"), ""),
    ("dau_tu_thoai_von", ("gop von", "thoai von", "cong ty con", "chuyen nhuong", "giai the",
                          "sap nhap", "dau tu", "du an"), ""),
    ("dinh_chinh", ("dinh chinh bao cao tai chinh", "trinh bay lai", "dinh chinh"), ""),
    ("the_chap_vay_von", ("the chap", "vay von", "bao lanh", "tin dung", "han muc"), ""),
    ("nghi_quyet_dhdcd", ("nghi quyet dai hoi dong co dong", "nghi quyet dhdcd",
                          "bien ban va nghi quyet", "nghi quyet, bien ban"), ""),
    ("lay_y_kien_co_dong", ("lay y kien",), ""),
    ("ke_hoach_kinh_doanh", ("ke hoach sxkd", "ke hoach kinh doanh", "ket qua sxkd"), ""),
    ("nghi_quyet_hdqt", ("nghi quyet hdqt", "nghi quyet cua hdqt", "nghi quyet hoi dong quan tri",
                         "quyet dinh cua hdqt", "nghi quyet"), ""),
    ("bctn_bctc", ("bao cao thuong nien", "bao cao tai chinh", "ben vung"), ""),
]


def nhom_cbtt(k: str) -> tuple[str, str]:
    return theo_luat(k, LUAT_CBTT, ("khac", LY_DO_CBTT))


def nhom_bcqt(k: str) -> tuple[str, str]:
    return "quan_tri", ""


def nhom_dieule(k: str) -> tuple[str, str]:
    if co(k, "dieu le"):
        return "dieu_le", ""
    return "quy_che", ""


PHAN_NHOM = {"bctc": nhom_bctc, "bctn": nhom_bctn, "dhdcd": nhom_dhdcd, "cbtt": nhom_cbtt,
             "bcqt": nhom_bcqt, "dieule": nhom_dieule, "cabach": lambda k: ("ban_cao_bach", "")}

# ----------------------------------------------------------------------------
# MẠNG
# ----------------------------------------------------------------------------


class Web:
    """requests.Session + tự vượt lớp chặn cookie JS của website."""

    def __init__(self):
        self.s = requests.Session()
        self.s.headers["User-Agent"] = ("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
                                        "(KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
        self.s.headers["Accept-Language"] = "vi-VN,vi;q=0.9"

    def get(self, url: str, so_lan: int = 4, **kw) -> requests.Response:
        loi = None
        for lan in range(1, so_lan + 1):
            try:
                r = self.s.get(url, timeout=kw.pop("timeout", 120), **kw)
                if len(r.content) < 1000:
                    m = re.search(rb'document\.cookie="(\w+)=(\w+)"', r.content)
                    if m:
                        self.s.cookies.set(m.group(1).decode(), m.group(2).decode(),
                                           domain=urlparse(GOC).hostname)
                        r = self.s.get(url, timeout=120, **kw)
                return r
            except requests.RequestException as e:
                loi = e
                time.sleep(2 * lan)
        raise loi  # type: ignore[misc]


# ----------------------------------------------------------------------------
# BÓC DANH SÁCH TÀI LIỆU TỪ HTML
# ----------------------------------------------------------------------------


@dataclass
class Muc:
    nguon: str
    tieu_de: str
    url: str
    nam_tab: int | None = None
    ngay_dang: str = ""
    su_kien: str = ""          # tiêu đề nhóm cha (trang ĐHĐCĐ) / tiêu mục
    nam: int | None = None     # năm dùng để xếp thư mục
    ky: str = ""
    nhom: str = ""
    ngon_ngu: str = ""
    trang_thai: str = ""       # tai_ve | da_co | bo_qua | loi | trung_url | trung_noi_dung
    ly_do: str = ""
    duong_dan: str = ""
    kich_thuoc: int = 0
    sha256: str = ""
    ghi_chu: str = ""          # sửa tay / cảnh báo cho người đọc manifest


def nam_tu_to_tien(el: Tag) -> int | None:
    for p in [el, *el.parents]:
        if not isinstance(p, Tag):
            continue
        for gt in [p.get("id") or "", *(p.get("class") or [])]:
            m = re.fullmatch(r"y-(\d{4})", gt)
            if m:
                return int(m.group(1))
    return None


def van_ban(el: Tag | None) -> str:
    return " ".join(el.get_text(" ", strip=True).split()) if el else ""


def boc_danh_sach(html: str, ma: str, url_trang: str) -> list[Muc]:
    soup = BeautifulSoup(html, "html.parser")
    ra: list[Muc] = []
    thay: set[str] = set()
    ngay_nhom: dict[str, str] = {}   # tiêu đề nhóm cha -> ngày đăng (trang ĐHĐCĐ)

    for a in soup.select("a[href]"):
        href = urljoin(url_trang, a["href"])
        if not urlparse(href).path.lower().endswith(DUOI) or href in thay:
            continue
        thay.add(href)

        tieu_de, ngay, su_kien = "", "", ""
        tr = a.find_parent("tr")
        art = a.find_parent("article")
        the = a.find_parent(class_="card__implement")
        if tr is not None:                                    # bố cục bảng
            td = tr.select_one("td.title")
            tieu_de = van_ban(td)
            cha = (td.get("value") or "").strip() if td else ""
            if cha and cha != tieu_de:
                su_kien = cha
            tm = tr.select_one("time[datetime]")
            if tm:
                ngay = tm["datetime"][:10]
                ngay_nhom[tieu_de] = ngay
            elif su_kien:
                ngay = ngay_nhom.get(su_kien, "")
            # tiêu mục (vd "ĐHĐCĐ thường niên") nằm ở <th colspan> phía trên
            th = tr.find_previous("th", attrs={"colspan": True})
            if th and ma == "dhdcd":
                su_kien = f"{van_ban(th)} | {su_kien}" if su_kien else van_ban(th)
        elif art is not None:                                 # bố cục timeline
            tieu_de = van_ban(art.select_one("p a")) or van_ban(a)
            tm = art.select_one("time.entry-date, time[datetime]")
            ngay = tm["datetime"][:10] if tm else ""
            h3 = art.find_previous("h3")
            su_kien = van_ban(h3.find("span")) if h3 else ""
        elif the is not None:                                 # bố cục thẻ
            tieu_de = van_ban(the.select_one(".paragraph-item"))
            tm = the.select_one("time[datetime]")
            ngay = tm["datetime"][:10] if tm else ""
        if not tieu_de:
            tieu_de = van_ban(a) or ten_sach(href)

        ra.append(Muc(nguon=ma, tieu_de=tieu_de, url=href, nam_tab=nam_tu_to_tien(a),
                      ngay_dang=ngay, su_kien=su_kien))
    return ra


# ----------------------------------------------------------------------------
# BẢNG DỮ LIỆU WEB -> MARKDOWN
# ----------------------------------------------------------------------------


def luu_du_lieu_web(web: Web, goc: Path):
    """Lưu JSON gốc (để nạp DB) + bản .md (để đọc/đối chiếu) cho từng bảng dữ liệu web."""
    import json
    thu_muc = goc / "08_du_lieu_web"
    thu_muc.mkdir(parents=True, exist_ok=True)
    hom_nay = date.today().isoformat()
    for ten, (pt, ep, trang, cot) in DU_LIEU_WEB.items():
        url = f"{API}/{ep}"
        try:
            r = (web.s.post(url, data="{}", headers={"Content-type": "application/json"}, timeout=60)
                 if pt == "POST" else web.get(url))
            ds = r.json()
        except Exception as e:  # noqa: BLE001
            print(f"  ! {ten}: {type(e).__name__}")
            continue
        (thu_muc / f"{ten}.json").write_text(json.dumps(ds, ensure_ascii=False, indent=2), encoding="utf-8")
        cot = cot or {k: k for k in (ds[0] if ds else {}) if not k.endswith("_id")}
        cap_nhat = sorted({d.get("update_date", "") for d in ds} - {""})
        dong = [f"# {ten}", "", f"- Trang hiển thị: {trang}", f"- API: `{pt} {url}`",
                f"- Ngày lấy: {hom_nay}" + (f" · website ghi cập nhật: {', '.join(cap_nhat)}" if cap_nhat else ""),
                f"- Số dòng: {len(ds)}", "",
                "| " + " | ".join(cot.values()) + " |", "|" + "---|" * len(cot)]
        dong += ["| " + " | ".join(str(d.get(k, "")).replace("|", "/") for k in cot) + " |" for d in ds]
        (thu_muc / f"{ten}.md").write_text("\n".join(dong) + "\n", encoding="utf-8")
        print(f"  lưu 08_du_lieu_web/{ten}.json|.md ({len(ds)} dòng)")


# ----------------------------------------------------------------------------
# LUỒNG CHÍNH
# ----------------------------------------------------------------------------


# Sửa tay những chỗ website ghi sai (khoá = đoạn cuối URL). Mỗi mục phải có bằng chứng.
SUA_TAY = {
    "2026/01/BCTC-hop-nhat-ViEn.pdf": dict(
        nam=2025, ky="Q4",
        ghi_chu="web ghi 'Quý 4/2026' — sai: đăng 29/01/2026 cùng BCTC riêng Q4/2025, "
                "trang bìa ghi 'năm tài chính kết thúc 31/12/2025'"),
}


def phan_loai(m: Muc, cach_nam: str | None, tu: int, den: int, tat_ca: bool):
    k = khong_dau(f"{m.su_kien} {m.tieu_de}") if m.nguon != "dhdcd" else khong_dau(m.tieu_de)
    m.ngon_ngu = ngon_ngu(m.tieu_de, m.url)
    m.ky = doan_ky(m.tieu_de) if m.nguon in ("bctc", "bcqt") else ""
    if cach_nam == "ky":
        m.nam = doan_nam_ky(m.tieu_de, m.url) or m.nam_tab
    elif cach_nam == "tab":
        m.nam = m.nam_tab or doan_nam_ky(m.tieu_de, m.url)
    m.nhom, ly_do = PHAN_NHOM[m.nguon](k)
    for duoi, sua in SUA_TAY.items():
        if m.url.endswith(duoi):
            for khoa, gt in sua.items():
                setattr(m, khoa, gt)

    if cach_nam and m.nam is not None and not (tu <= m.nam <= den):
        m.trang_thai, m.ly_do = "bo_qua", f"ngoài khoảng năm {tu}–{den}"
    elif cach_nam and m.nam is None:
        m.trang_thai, m.ly_do = "bo_qua", "không xác định được năm"
    elif tat_ca:
        pass
    elif m.ngon_ngu == "en":
        m.trang_thai, m.ly_do = "bo_qua", "bản tiếng Anh (đã có bản tiếng Việt tương ứng)"
    elif ly_do:
        m.trang_thai, m.ly_do = "bo_qua", ly_do


def duong_dan_luu(goc: Path, thu_muc: str, m: Muc, da_dung: set[Path]) -> Path:
    """Đường dẫn lưu, không trùng: web hay đặt cùng một tên file (vd BCTC-rieng.pdf) cho
    các kỳ khác nhau -> thêm hậu tố tháng đăng lấy từ URL (…/uploads.bak/2024/03/…)."""
    p = goc / thu_muc
    if m.nam is not None:
        p = p / str(m.nam)
    dich = p / m.nhom / ten_sach(m.url)
    if dich in da_dung:
        thang = re.search(r"/(20\d\d)/(\d\d)/[^/]+$", m.url)
        hau_to = f"{thang.group(1)}-{thang.group(2)}" if thang else hashlib.md5(m.url.encode()).hexdigest()[:6]
        dich = dich.with_name(f"{dich.stem}__{hau_to}{dich.suffix}")
    da_dung.add(dich)
    return dich


def tai_mot(web: Web, m: Muc, dich: Path, goc: Path) -> Muc:
    m.duong_dan = dich.relative_to(goc).as_posix()
    if dich.exists() and dich.stat().st_size > 1000:
        m.trang_thai, m.kich_thuoc = "da_co", dich.stat().st_size
        m.sha256 = hashlib.sha256(dich.read_bytes()).hexdigest()
        return m
    try:
        r = web.get(m.url)
        if r.status_code != 200:
            m.trang_thai, m.ly_do = "loi", f"HTTP {r.status_code}"
            return m
        du_lieu = r.content
        if urlparse(m.url).path.lower().endswith(".pdf") and not du_lieu.startswith(b"%PDF"):
            m.trang_thai, m.ly_do = "loi", "nội dung trả về không phải PDF"
            return m
        dich.parent.mkdir(parents=True, exist_ok=True)
        dich.write_bytes(du_lieu)
        m.trang_thai, m.kich_thuoc = "tai_ve", len(du_lieu)
        m.sha256 = hashlib.sha256(du_lieu).hexdigest()
    except Exception as e:  # noqa: BLE001 — một file hỏng không được làm sập cả phiên
        m.trang_thai, m.ly_do = "loi", type(e).__name__
    return m


def chay(goc: Path, tu: int, den: int, liet_ke: bool, tat_ca: bool):
    web = Web()
    goc.mkdir(parents=True, exist_ok=True)
    tat_ca_muc: list[Muc] = []
    da_thay: dict[str, str] = {}
    thu_muc_cua = {ma: tm for ma, tm, _, _ in NGUON}

    for ma, thu_muc, url, cach_nam in NGUON:
        r = web.get(url)
        ds = boc_danh_sach(r.text, ma, url)
        print(f"{thu_muc:28s} {len(ds):4d} tài liệu trên trang")
        for m in ds:
            phan_loai(m, cach_nam, tu, den, tat_ca)
            if not m.trang_thai and m.url in da_thay:
                m.trang_thai, m.ly_do = "trung_url", f"đã lấy ở nguồn {da_thay[m.url]}"
            if not m.trang_thai:
                da_thay[m.url] = ma
            tat_ca_muc.append(m)

    can_tai = [m for m in tat_ca_muc if not m.trang_thai]
    print(f"\nTổng {len(tat_ca_muc)} mục · cần tải {len(can_tai)} · bỏ qua {len(tat_ca_muc) - len(can_tai)}")

    if not liet_ke:
        da_dung: set[Path] = set()
        dich = {id(m): duong_dan_luu(goc, thu_muc_cua[m.nguon], m, da_dung) for m in can_tai}
        with ThreadPoolExecutor(SO_LUONG_TAI) as ex:
            viec = {ex.submit(tai_mot, web, m, dich[id(m)], goc): m for m in can_tai}
            for i, f in enumerate(as_completed(viec), 1):
                m = f.result()
                dau = {"tai_ve": "+", "da_co": "=", "loi": "!"}.get(m.trang_thai, "?")
                print(f"  [{i:3d}/{len(can_tai)}] {dau} {m.duong_dan or m.url}"
                      f"{'  ' + m.ly_do if m.ly_do else ''}")

        # cùng nội dung, khác URL (web hay đăng lại một file ở nhiều nơi) -> giữ bản đầu
        theo_hash: dict[str, Muc] = {}
        for m in tat_ca_muc:
            if m.trang_thai in ("tai_ve", "da_co") and m.sha256:
                goc_m = theo_hash.setdefault(m.sha256, m)
                if goc_m is not m:
                    (goc / m.duong_dan).unlink(missing_ok=True)
                    m.trang_thai, m.ly_do = "trung_noi_dung", f"giống hệt {goc_m.duong_dan}"
        luu_du_lieu_web(web, goc)

    ghi_manifest(goc / "manifest.csv", tat_ca_muc)
    tong_ket(tat_ca_muc)


def ghi_manifest(p: Path, ds: list[Muc]):
    cot = list(asdict(ds[0]).keys())
    with open(p, "w", newline="", encoding="utf-8-sig") as f:
        w = csv.DictWriter(f, fieldnames=cot)
        w.writeheader()
        for m in sorted(ds, key=lambda x: (x.nguon, -(x.nam or 0), x.nhom, x.tieu_de)):
            w.writerow(asdict(m))
    print(f"\nĐã ghi {p}")


def tong_ket(ds: list[Muc]):
    from collections import Counter
    print("\nTRẠNG THÁI:", dict(Counter(m.trang_thai or "chua_tai" for m in ds)))
    giu = [m for m in ds if m.trang_thai in ("tai_ve", "da_co", "")]
    bang = Counter((m.nguon, m.nam, m.nhom) for m in giu)
    for (ng, nam, nh), n in sorted(bang.items(), key=lambda x: (x[0][0], -(x[0][1] or 0), x[0][2])):
        print(f"  {ng:7s} {nam or '-':>5} {nh:28s} {n}")
    loi = [m for m in ds if m.trang_thai == "loi"]
    for m in loi:
        print(f"  LỖI {m.ly_do}: {m.url}")


def main():
    if hasattr(sys.stdout, "reconfigure"):
        sys.stdout.reconfigure(encoding="utf-8")
    ap = argparse.ArgumentParser(description="Tải tài liệu công bố của CTR")
    ap.add_argument("--thu-muc", type=Path, default=THU_MUC_LUU)
    ap.add_argument("--tu", type=int, default=NAM_TU)
    ap.add_argument("--den", type=int, default=NAM_DEN)
    ap.add_argument("--liet-ke", action="store_true", help="chỉ ghi manifest, không tải")
    ap.add_argument("--tat-ca", action="store_true",
                    help="không bỏ qua bản tiếng Anh / văn bản thủ tục (vẫn lọc theo năm)")
    a = ap.parse_args()
    chay(a.thu_muc, a.tu, a.den, a.liet_ke, a.tat_ca)


if __name__ == "__main__":
    main()
