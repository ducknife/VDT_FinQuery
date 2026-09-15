"""
Kiểm tra độ phủ và chất lượng dữ liệu đã kéo về từ vnstock.
Chạy trong thư mục cha của data_raw_<TICKER>/

    python kiem_tra_du_lieu.py
"""

from pathlib import Path
import pandas as pd

TICKER = "CTR"
THU_MUC = Path(f"data_raw_{TICKER}")


def doc_csv(path: Path):
    """Đọc CSV an toàn. Trả về (DataFrame, thông_báo_lỗi)."""
    if not path.exists():
        return None, "THIẾU FILE"
    if path.stat().st_size == 0:
        return None, "FILE RỖNG (nguồn không có dữ liệu)"
    try:
        df = pd.read_csv(path)
    except pd.errors.EmptyDataError:
        return None, "FILE RỖNG (không có cột nào)"
    except Exception as e:
        return None, f"LỖI ĐỌC: {type(e).__name__}"
    if df.empty:
        return df, "KHÔNG CÓ DÒNG DỮ LIỆU"
    return df, None


print(f"KIỂM TRA DỮ LIỆU: {TICKER}")
print("=" * 72)

# --- Báo cáo tài chính: số kỳ, khoảng thời gian, tỷ lệ ô trống ---------------
print("\n--- BÁO CÁO TÀI CHÍNH ---")
for ten in [
    "income_statement_year", "income_statement_quarter",
    "balance_sheet_year",    "balance_sheet_quarter",
    "cash_flow_year",        "cash_flow_quarter",
    "ratio_year",            "ratio_quarter",
]:
    df, loi = doc_csv(THU_MUC / f"{TICKER}_{ten}.csv")
    if loi:
        print(f"{ten:30s} {loi}")
        continue

    so_ky, so_cot = df.shape

    cot_nam = next(
        (c for c in df.columns
         if str(c).strip().lower() in ("nam", "năm", "year", "yearreport", "fiscalyear")),
        None,
    )
    if cot_nam is not None:
        nam = pd.to_numeric(df[cot_nam], errors="coerce").dropna()
        khoang = f"{int(nam.min())}–{int(nam.max())}" if len(nam) else "?"
    else:
        khoang = f"cột đầu: {str(df.columns[0])[:20]}"

    trong = df.isna().sum().sum() / (so_ky * so_cot) * 100 if so_ky * so_cot else 0
    print(f"{ten:30s} {so_ky:3d} kỳ x {so_cot:4d} chỉ tiêu | {khoang:22s} | trống {trong:4.1f}%")

# --- Hồ sơ doanh nghiệp ------------------------------------------------------
print("\n--- HỒ SƠ DOANH NGHIỆP ---")
for ten in ["overview", "subsidiaries", "affiliate", "shareholders",
            "officers", "events", "ratio_summary", "price_history"]:
    df, loi = doc_csv(THU_MUC / f"{TICKER}_{ten}.csv")
    if loi:
        print(f"{ten:16s} {loi}")
    else:
        print(f"{ten:16s} {len(df):5d} dòng x {df.shape[1]:3d} cột")

# --- Xác nhận đúng doanh nghiệp ---------------------------------------------
print("\n--- XÁC NHẬN DOANH NGHIỆP ---")
df, loi = doc_csv(THU_MUC / f"{TICKER}_overview.csv")
if loi:
    print(f"  {loi}")
else:
    for cot in df.columns:
        gia_tri = str(df[cot].iloc[0])
        if len(gia_tri) < 150:
            print(f"  {cot}: {gia_tri}")

# --- Tên chỉ tiêu: nguyên liệu đặt tên hàm cho DB tool ----------------------
for nhan, ten_file in [
    ("BÁO CÁO KQKD", "income_statement_year"),
    ("CHỈ SỐ TÀI CHÍNH", "ratio_year"),
]:
    print(f"\n--- TÊN CHỈ TIÊU: {nhan} ---")
    df, loi = doc_csv(THU_MUC / f"{TICKER}_{ten_file}.csv")
    if loi:
        print(f"  {loi}")
    else:
        for c in df.columns:
            print(f"  {c}")
