"""
Kéo dữ liệu tài chính cho MỘT mã chứng khoán, xuất ra CSV.

Bản 3 — tự dò cách lấy nhiều kỳ trên đúng phiên bản vnstock đang cài.
Thử lần lượt 4 cách, cách nào chạy được thì dùng; cuối cùng lùi về cách
gọi thường (chỉ 4 kỳ) để không bao giờ bị chặn.

    python keo_du_lieu_vnstock.py
"""

import inspect
from pathlib import Path
from vnstock import Vnstock

TICKER = "CTR"
SO_KY_NAM = 8
SO_KY_QUY = 24

OUT = Path(f"data_raw_{TICKER}")
OUT.mkdir(exist_ok=True)

stock = Vnstock().stock(symbol=TICKER, source="VCI")
fin = stock.finance

# --- 0. CHẨN ĐOÁN: phiên bản và API thực tế của máy này ----------------------
print("=" * 72)
print("CHẨN ĐOÁN MÔI TRƯỜNG")
print("=" * 72)

try:
    from importlib.metadata import version
    print(f"  vnstock version : {version('vnstock')}")
except Exception as e:
    print(f"  vnstock version : không xác định ({e})")

print(f"  kiểu đối tượng  : {type(fin).__module__}.{type(fin).__name__}")

try:
    print(f"  income_statement{inspect.signature(fin.income_statement)}")
except Exception:
    pass

ham_report = [m for m in dir(fin) if "report" in m.lower()]
print(f"  hàm chứa 'report': {ham_report}")
print(f"  toàn bộ thuộc tính: {[m for m in dir(fin) if not m.startswith('__')]}")


# --- 1. Dò cách lấy được nhiều kỳ -------------------------------------------
def lay_bao_cao(report_type: str, period: str, limit: int):
    """Thử lần lượt các cách, trả về (DataFrame, tên_cách_dùng_được)."""
    ham_cong_khai = getattr(fin, report_type)

    # Cách 1: hàm công khai có tham số limit
    try:
        if "limit" in inspect.signature(ham_cong_khai).parameters:
            return ham_cong_khai(period=period, lang="vi", limit=limit), "công khai + limit"
    except Exception:
        pass

    # Cách 2 & 3: các hàm nội bộ (tên khác nhau tùy phiên bản)
    for ten_ham in ("_get_financial_report", "_get_report", "_financial_report", "_report"):
        ham = getattr(fin, ten_ham, None)
        if ham is None:
            continue
        for kwargs in (
            {"report_type": report_type, "period": period, "lang": "vi", "limit": limit},
            {"report_type": report_type, "period": period, "limit": limit},
        ):
            try:
                return ham(**kwargs), f"{ten_ham}({', '.join(kwargs)})"
            except TypeError:
                continue
            except Exception:
                break

    # Cách 4: lùi về gọi thường — chỉ được 4 kỳ
    return ham_cong_khai(period=period, lang="vi"), "gọi thường (4 kỳ)"


print("\n" + "=" * 72)
print("BÁO CÁO TÀI CHÍNH")
print("=" * 72)

for report_type in ("income_statement", "balance_sheet", "cash_flow"):
    for period, limit in (("year", SO_KY_NAM), ("quarter", SO_KY_QUY)):
        try:
            df, cach = lay_bao_cao(report_type, period, limit)
            path = OUT / f"{TICKER}_{report_type}_{period}.csv"
            df.to_csv(path, index=False, encoding="utf-8-sig")

            cot_ky = [c for c in df.columns
                      if c not in ("item", "item_en", "item_id") and not str(c).startswith("Unnamed")]
            print(f"  {report_type:18s} {period:8s} {len(df):3d} chỉ tiêu x {len(cot_ky):2d} kỳ "
                  f"[{cot_ky[0] if cot_ky else '?'} ... {cot_ky[-1] if cot_ky else '?'}]  ({cach})")
        except Exception as e:
            print(f"  {report_type:18s} {period:8s} LỖI: {type(e).__name__}: {e}")

# --- 2. Hồ sơ doanh nghiệp ---------------------------------------------------
print("\n" + "=" * 72)
print("HỒ SƠ DOANH NGHIỆP")
print("=" * 72)
for ten, ham in {
    "overview":     stock.company.overview,
    "subsidiaries": stock.company.subsidiaries,
    "affiliate":    stock.company.affiliate,
    "shareholders": stock.company.shareholders,
    "officers":     stock.company.officers,
    "events":       stock.company.events,
}.items():
    try:
        df = ham()
        if df is None or df.empty:
            print(f"  {ten:14s} không có dữ liệu")
            continue
        df.to_csv(OUT / f"{TICKER}_{ten}.csv", index=False, encoding="utf-8-sig")
        print(f"  {ten:14s} {len(df)} dòng")
    except Exception as e:
        print(f"  {ten:14s} LỖI: {type(e).__name__}")

# --- 3. Giá lịch sử ----------------------------------------------------------
print("\n" + "=" * 72)
print("GIÁ LỊCH SỬ")
print("=" * 72)
try:
    hist = stock.quote.history(start="2019-01-01", end="2026-12-31", interval="1D")
    hist.to_csv(OUT / f"{TICKER}_price_history.csv", index=False, encoding="utf-8-sig")
    print(f"  {len(hist)} phiên")
except Exception as e:
    print(f"  LỖI: {e}")

print(f"\nXong. Dữ liệu trong: {OUT.resolve()}")
print("""
LƯU Ý KHI NẠP VÀO DB:
  - KHÔNG dùng finance.ratio() (nhánh RATIO trả sai kỳ). Tự tính chỉ số từ 3 báo cáo.
  - Giá vốn hàng bán mang dấu ÂM -> Lợi nhuận gộp = Doanh thu thuần + Giá vốn.
  - Số tiền cỡ 13.939.517.269.142 -> dùng NUMERIC/BIGINT, không dùng INT.
  - Khóa chỉ tiêu: dùng item_id (snake_case), không dùng item (tiếng Việt).
  - Dữ liệu dạng chỉ tiêu-theo-hàng -> phải xoay bảng (unpivot) khi nạp vào SQL.

VẪN PHẢI TRÍCH TAY: segment_revenue, debt_instrument, plan_target, dividend
""")
