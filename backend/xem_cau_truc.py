"""
Xem cấu trúc thô của các file CSV để thiết kế bước nạp vào DB cho đúng.
Chạy cạnh thư mục data_raw_<TICKER>/

    python xem_cau_truc.py
"""

from pathlib import Path
import pandas as pd

TICKER = "CTR"
THU_MUC = Path(f"data_raw_{TICKER}")

FILES = [
    "income_statement_year",
    "income_statement_quarter",
    "balance_sheet_year",
    "ratio_year",
    "ratio_quarter",
    "cash_flow_year",
]

for ten in FILES:
    path = THU_MUC / f"{TICKER}_{ten}.csv"
    print("\n" + "=" * 78)
    print(f"FILE: {path.name}")
    print("=" * 78)

    if not path.exists() or path.stat().st_size == 0:
        print("  (thiếu hoặc rỗng)")
        continue

    # 1. Bốn dòng đầu ở dạng văn bản thô -> lộ ra header có mấy tầng
    print("\n[4 DÒNG ĐẦU DẠNG THÔ]")
    with open(path, encoding="utf-8-sig") as f:
        for i, line in enumerate(f):
            if i >= 4:
                break
            print(f"  {i}| {line.rstrip()[:180]}")

    # 2. Đọc bình thường
    df = pd.read_csv(path)
    print(f"\n[ĐỌC THƯỜNG] shape = {df.shape}")
    print(f"  cột: {list(df.columns)[:14]}")

    # 3. Nếu là báo cáo có cột 'item', in vài chỉ tiêu đầu để thấy nội dung
    if "item" in df.columns:
        cot_ky = [c for c in df.columns
                  if c not in ("Unnamed: 0", "item", "item_en", "item_id")]
        print(f"  số cột kỳ: {len(cot_ky)} -> {cot_ky[:12]}")
        print("\n[5 CHỈ TIÊU ĐẦU]")
        for _, r in df.head(5).iterrows():
            gia_tri = " | ".join(f"{c}={r[c]}" for c in cot_ky[:4])
            print(f"  {str(r['item'])[:55]:57s} {gia_tri}")

    # 4. Thử đọc với header 2 tầng, xem có hợp lý hơn không
    try:
        df2 = pd.read_csv(path, header=[0, 1])
        print(f"\n[ĐỌC VỚI HEADER 2 TẦNG] shape = {df2.shape}")
        print(f"  cột: {list(df2.columns)[:8]}")
    except Exception as e:
        print(f"\n[ĐỌC VỚI HEADER 2 TẦNG] không áp dụng ({type(e).__name__})")

print("\n" + "=" * 78)
print("KIỂM TRA RIÊNG: year và quarter có thực sự khác nhau không?")
print("=" * 78)
for goc in ["income_statement", "balance_sheet", "cash_flow", "ratio"]:
    fy = THU_MUC / f"{TICKER}_{goc}_year.csv"
    fq = THU_MUC / f"{TICKER}_{goc}_quarter.csv"
    if fy.exists() and fq.exists() and fy.stat().st_size and fq.stat().st_size:
        giong_het = fy.read_bytes() == fq.read_bytes()
        print(f"  {goc:20s} year == quarter ? {'GIỐNG HỆT (period không ăn!)' if giong_het else 'khác nhau (OK)'}")
