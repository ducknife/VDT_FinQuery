# Đánh giá câu trả lời nhánh số liệu — 13/10/2026 09:08

Bộ câu hỏi: `data/eval/eval_dbquery.yml` (21 câu). Model: `Qwen/Qwen3.5-397B-A17B`, cùng cho mọi cấu hình; chấm máy "đúng và đủ số được hỏi" (`DbAnswerScorer`), **cần người soát lại**. Chi tiết: `danh-gia-db_20261013-0814_qwen.csv`.

- FINQUERY: 3 tool SQL mẫu, LLM chỉ điền tham số
- TEXT_TO_SQL: LLM tự viết SQL (tool execute_sql, user chỉ đọc)
- LLM_ONLY: không công cụ, trả lời từ trí nhớ

## 1. Tổng hợp

| Cấu hình | Đúng | Một số | So sánh | Từ chối | Thời gian TB ± ĐLC (s) | p50 (s) | USD/câu | Token vào TB | Lỗi SQL | Số không có nguồn |
|---|---|---|---|---|---|---|---|---|---|---|
| FINQUERY | **20/21** | 10/11 | 7/7 | 3/3 | 153.1 ± 75.1 | 163.1 | 0.0162 | 13447 | 0 | 0 |

## 2. Từng câu

| Câu | Nhóm | FINQUERY |
|---|---|---|
| D01 | db_mot_so | ✓ |
| D02 | db_mot_so | ✓ |
| D03 | db_mot_so | ✓ |
| D04 | db_mot_so | ✓ |
| D05 | db_mot_so | ✓ |
| D06 | db_mot_so | ✓ |
| D07 | db_mot_so | ✓ |
| D08 | db_mot_so | ✓ |
| C01 | db_so_sanh | ✓ |
| C02 | db_so_sanh | ✓ |
| C03 | db_so_sanh | ✓ |
| C04 | db_so_sanh | ✓ |
| C05 | db_so_sanh | ✓ |
| C06 | db_so_sanh | ✓ |
| C07 | db_so_sanh | ✓ |
| D09 | db_mot_so | ✓ |
| D10 | db_mot_so | ✓ |
| D11 | db_mot_so | ✗ thiếu 1.123.253.891.417 (hoặc 1.123,25 tỷ) |
| X06 | tu_choi | ✓ |
| X03 | tu_choi | ✓ |
| X04 | tu_choi | ✓ |

## 3. Câu trả lời để soát

### D01 — Doanh thu thuần hợp nhất năm 2024 của CTR là bao nhiêu?

**Đáp án vàng:** 12.609.519.560.081 đồng

- **FINQUERY** ✓ (có số đồng chính xác, 131.3 s): Doanh thu thuần hợp nhất năm 2024 của CTR là 12.609.519.560.081 đồng (≈ 12.609,52 tỷ đồng) [D1].
  - công cụ: get_financial_figures([DOANH_THU_THUAN], 1 kỳ, [CONSOLIDATED]) -> 1 số

### D02 — LNST trên báo cáo tài chính riêng năm 2025 là bao nhiêu?

**Đáp án vàng:** 587.417.356.381 đồng

- **FINQUERY** ✓ (có số đồng chính xác, 156.1 s): Lợi nhuận sau thuế trên báo cáo tài chính riêng năm 2025 là 587.417.356.381 đồng (≈ 587,42 tỷ đồng) [D1].
  - công cụ: get_financial_figures([LOI_NHUAN_SAU_THUE], 1 kỳ, [SEPARATE]) -> 1 số

### D03 — Nợ phải trả hợp nhất tại kỳ báo cáo gần nhất là bao nhiêu?

**Đáp án vàng:** Kỳ gần nhất 30/06/2026: 7.435.180.982.045 đồng

- **FINQUERY** ✓ (có số đồng chính xác, 226.4 s): Nợ phải trả hợp nhất tại kỳ báo cáo gần nhất (30/06/2026) là 7.435.180.982.045 đồng (≈ 7.435,18 tỷ đồng) [D1].
  - công cụ: get_financial_figures([NO_PHAI_TRA], 1 kỳ, [CONSOLIDATED]) -> 1 số

### D04 — HTK hợp nhất cuối năm 2025?

**Đáp án vàng:** 424.971.712.679 đồng (mã 140, đã trừ dự phòng)

- **FINQUERY** ✓ (có số đồng chính xác, 94.1 s): Hàng tồn kho hợp nhất tại 31/12/2025 là 424.971.712.679 đồng (≈ 424,97 tỷ đồng) [D1].
  - công cụ: get_financial_figures([HANG_TON_KHO_M140], 1 kỳ, [CONSOLIDATED]) -> 1 số

### D05 — Dòng tiền kinh doanh hợp nhất quý 1/2025 âm hay dương, bao nhiêu?

**Đáp án vàng:** Âm: lưu chuyển tiền thuần từ HĐKD 3 tháng đầu 2025 là -295.939.415.278 đồng.

- **FINQUERY** ✓ (có số đồng chính xác, 190.6 s): Dòng tiền kinh doanh hợp nhất quý 1/2025 âm, đạt -295.939.415.278 đồng (≈ -295,94 tỷ đồng) [D1].
  - công cụ: get_financial_figures([LCTT_THUAN_HDKD], 1 kỳ, [CONSOLIDATED]) -> 1 số

### D06 — Lợi nhuận gộp hợp nhất 6 tháng đầu năm 2026 là bao nhiêu?

**Đáp án vàng:** 521.996.574.332 đồng

- **FINQUERY** ✓ (có số đồng chính xác, 190.1 s): Lợi nhuận gộp hợp nhất 6 tháng đầu năm 2026 là 521.996.574.332 đồng (≈ 522,00 tỷ đồng) [D1].
  - công cụ: get_financial_figures([LOI_NHUAN_GOP], 1 kỳ, [CONSOLIDATED]) -> 1 số

### D07 — Doanh thu thuần riêng của quý 2/2026 (chỉ riêng quý, không luỹ kế) là bao nhiêu?

**Đáp án vàng:** 3.773.814.395.170 đồng

- **FINQUERY** ✓ (có số đồng chính xác, 146.1 s): Doanh thu thuần riêng của quý 2/2026 (01/04/2026–30/06/2026) là 3.773.814.395.170 đồng (≈ 3.773,81 tỷ đồng) [D1].
  - công cụ: get_financial_figures([DOANH_THU_THUAN], 1 kỳ, [SEPARATE]) -> 1 số

### D08 — Lãi ròng hợp nhất riêng quý 3/2024 là bao nhiêu?

**Đáp án vàng:** 146.262.730.079 đồng (chỉ quý III/2024)

- **FINQUERY** ✓ (có số đồng chính xác, 238.3 s): Tài liệu cung cấp số liệu lợi nhuận sau thuế cho phạm vi hợp nhất và riêng quý 3/2024 như sau: Lợi nhuận sau thuế hợp nhất quý 3/2024 là 146.262.730.079 đồng (≈ 146,26 tỷ đồng) [D1]. Lợi nhuận sau thuế riêng quý 3/2024 là 138.986.117.657 đồng (≈ 138,99 tỷ đồng) [D2].
  - công cụ: get_financial_figures([LOI_NHUAN_SAU_THUE], 1 kỳ, [CONSOLIDATED, SEPARATE]) -> 2 số

### C01 — Doanh thu thuần hợp nhất năm 2025 tăng bao nhiêu phần trăm so với năm 2024?

**Đáp án vàng:** 13.939,52 tỷ đồng so với 12.609,52 tỷ đồng: tăng 10,55%.

- **FINQUERY** ✓ (có 10,55%, 173.1 s): Doanh thu thuần hợp nhất năm 2025 tăng 10,55% so với năm 2024 [D3]. Cụ thể, doanh thu thuần năm 2024 là 12.609.519.560.081 đồng [D1] và năm 2025 là 13.939.517.269.142 đồng [D2].
  - công cụ: get_financial_figures([DOANH_THU_THUAN], 2 kỳ, [CONSOLIDATED]) -> 2 số

### C02 — Biên lợi nhuận gộp hợp nhất năm 2024 là bao nhiêu?

**Đáp án vàng:** Lợi nhuận gộp 884,22 tỷ đồng / doanh thu thuần 12.609,52 tỷ đồng = 7,01%.

- **FINQUERY** ✓ (có 7,01%, 255.3 s): Biên lợi nhuận gộp hợp nhất năm 2024 là 7,01% [D3]. Tỷ lệ này được tính dựa trên Lợi nhuận gộp về bán hàng và cung cấp dịch vụ, hợp nhất, năm 2024 là 884.221.709.521 đồng (≈ 884,22 tỷ đồng) [D1] và Doanh thu thuần về bán hàng và cung cấp dịch vụ, hợp nhất, năm 2024 là 12.609.519.560.081 đồng (≈ 12.609,52 tỷ đồng) [D2].
  - công cụ: compute_financial_ratio(GROSS_MARGIN, FY/2024, CONSOLIDATED)

### C03 — Tỷ lệ nợ phải trả trên tổng tài sản hợp nhất cuối năm 2025 là bao nhiêu?

**Đáp án vàng:** 6.118,37 tỷ đồng / 8.185,54 tỷ đồng = 74,75%.

- **FINQUERY** ✓ (có 74,75%, 194.8 s): Tỷ lệ nợ phải trả trên tổng tài sản hợp nhất cuối năm 2025 là 74,75% [D3]. Tỷ lệ này được tính dựa trên nợ phải trả 6.118.368.595.239 đồng [D1] và tổng tài sản 8.185.544.854.669 đồng [D2].
  - công cụ: compute_financial_ratio(DEBT_TO_ASSETS, FY/2025, CONSOLIDATED)

### C04 — LNST hợp nhất 6 tháng đầu 2026 tăng bao nhiêu so với cùng kỳ năm trước?

**Đáp án vàng:** 321,81 tỷ đồng so với 266,93 tỷ đồng: tăng 54,88 tỷ đồng (20,56%).

- **FINQUERY** ✓ (có 20,56%, 265.8 s): Lợi nhuận sau thuế hợp nhất 6 tháng đầu năm 2026 tăng 54.880.940.404 đồng (≈ 54,88 tỷ đồng), tương ứng 20,56% so với cùng kỳ năm trước [D3]. Cụ thể, LNST hợp nhất 6 tháng đầu năm 2026 đạt 321.807.293.831 đồng (≈ 321,81 tỷ đồng) [D2], tăng so với mức 266.926.353.427 đồng (≈ 266,93 tỷ đồng) của 6 tháng đầu năm 2025 [D1].
  - công cụ: get_financial_figures([LOI_NHUAN_SAU_THUE], 2 kỳ, [CONSOLIDATED]) -> 2 số

### C05 — Năm 2025 dòng tiền thuần từ hoạt động kinh doanh hợp nhất gấp bao nhiêu lần lợi nhuận sau thuế?

**Đáp án vàng:** 1.455,42 tỷ đồng / 599,72 tỷ đồng ≈ 2,43 lần.

- **FINQUERY** ✓ (có 2,43 lần, 146.9 s): Năm 2025, dòng tiền thuần từ hoạt động kinh doanh hợp nhất gấp 2,43 lần lợi nhuận sau thuế [D3]. Cụ thể, lưu chuyển tiền thuần từ hoạt động kinh doanh hợp nhất năm 2025 là 1.455.415.537.450 đồng [D1], lợi nhuận sau thuế hợp nhất năm 2025 là 599.717.125.636 đồng [D2].
  - công cụ: relate_two_figures(LCTT_THUAN_HDKD FY/2025, LOI_NHUAN_SAU_THUE FY/2025, RATIO)

### C06 — Hàng tồn kho hợp nhất cuối 2025 thay đổi thế nào so với cuối 2024?

**Đáp án vàng:** Giảm từ 610,48 tỷ đồng xuống 424,97 tỷ đồng: giảm 185,51 tỷ đồng (30,39%).

- **FINQUERY** ✓ (có 30,39%, 163.1 s): Hàng tồn kho hợp nhất tại 31/12/2025 là 424.971.712.679 đồng (≈ 424,97 tỷ đồng) [D2]. Tại 31/12/2024, giá trị này là 610.480.085.129 đồng (≈ 610,48 tỷ đồng) [D1]. So với cuối 2024, hàng tồn kho hợp nhất cuối 2025 giảm 185.508.372.450 đồng (≈ 185,51 tỷ đồng), tương ứng 30,39% [D3].
  - công cụ: get_financial_figures([HANG_TON_KHO_M140], 2 kỳ, [CONSOLIDATED]) -> 2 số

### C07 — Doanh thu thuần của công ty mẹ chiếm bao nhiêu phần trăm doanh thu thuần hợp nhất năm 2025?

**Đáp án vàng:** 11.791,01 tỷ đồng / 13.939,52 tỷ đồng = 84,59%.

- **FINQUERY** ✓ (có 84,59%, 185.6 s): Doanh thu thuần của công ty mẹ chiếm 84,59% doanh thu thuần hợp nhất năm 2025 [D3]. Theo báo cáo, doanh thu thuần về bán hàng và cung cấp dịch vụ riêng năm 2025 là 11.791.011.025.399 đồng (≈ 11.791,01 tỷ đồng) [D1], còn doanh thu thuần hợp nhất năm 2025 là 13.939.517.269.142 đồng (≈ 13.939,52 tỷ đồng) [D2].
  - công cụ: relate_two_figures(DOANH_THU_THUAN FY/2025, DOANH_THU_THUAN FY/2025, SHARE)

### D09 — Doanh thu thuần hợp nhất năm 2019 (theo số liệu đã trình bày lại) là bao nhiêu?

**Đáp án vàng:** 5.099.889.976.331 đồng (trình bày lại trong BCTC 2020; BCTC 2019 gốc ghi 5.053.710.724.827 đồng).

- **FINQUERY** ✓ (có số đồng chính xác, 210.2 s): Doanh thu thuần hợp nhất năm 2019 (theo số liệu đã trình bày lại) là 5.099.889.976.331 đồng (≈ 5.099,89 tỷ đồng) [D1].
  - công cụ: get_financial_figures([DOANH_THU_THUAN], 1 kỳ, [CONSOLIDATED]) -> 1 số

### D10 — tong tai san hop nhat ngay 31/12/2024 la bao nhieu

**Đáp án vàng:** 7.106.257.863.668 đồng

- **FINQUERY** ✓ (có số đồng chính xác, 93.1 s): Tổng tài sản hợp nhất tại ngày 31/12/2024 là 7.106.257.863.668 đồng (≈ 7.106,26 tỷ đồng) [D1].
  - công cụ: get_financial_figures([TONG_TAI_SAN], 1 kỳ, [CONSOLIDATED]) -> 1 số

### D11 — Tiền mặt hợp nhất cuối quý trước là bao nhiêu?

**Đáp án vàng:** Quý trước = quý 1/2026 (kỳ mới nhất là quý 2/2026). Tiền và các khoản tương đương tiền hợp nhất tại 31/03/2026: 1.123.253.891.417 đồng

- **FINQUERY** ✗ (thiếu 1.123.253.891.417 (hoặc 1.123,25 tỷ), 114.0 s): Tiền (tiền mặt và tiền gửi không kỳ hạn) hợp nhất tại ngày 31/03/2026 là 623.253.891.417 đồng (≈ 623,25 tỷ đồng) [D1].
  - công cụ: get_financial_figures([TIEN_M111], 1 kỳ, [CONSOLIDATED]) -> 1 số

### X06 — Doanh thu thuần hợp nhất cả năm 2026 là bao nhiêu?

**Đáp án vàng:** Chưa có số cả năm 2026 (kỳ mới nhất 30/06/2026). Có thể cung cấp số 6 tháng đầu 2026: 7.958.838.568.947 đồng. Không được tự ngoại suy ra số cả năm.

- **FINQUERY** ✓ (từ chối kỳ không có, mọi số đều có nguồn, 14.0 s): Câu hỏi này nằm ngoài phạm vi FinQuery: Kỳ FY 2026 (cả năm 2026) chưa được báo cáo. Dữ liệu mới nhất trong cơ sở là 30/06/2026 (H1 2026).
  - công cụ: decline_out_of_scope("Kỳ FY 2026 (cả năm 2026) chưa được báo cáo. Dữ liệu mới nhất trong cơ sở là 30/06/2026 (H1 2026).")

### X03 — Doanh thu thuần hợp nhất quý 3/2026 là bao nhiêu?

**Đáp án vàng:** Từ chối: kỳ mới nhất trong kho là 30/06/2026, chưa có báo cáo quý 3/2026.

- **FINQUERY** ✓ (từ chối kỳ không có, mọi số đều có nguồn, 18.3 s): Câu hỏi này nằm ngoài phạm vi FinQuery: Kỳ quý 3/2026 (kết thúc 30/09/2026) chưa được báo cáo. Dữ liệu mới nhất trong cơ sở là 30/06/2026 (6 tháng đầu năm 2026).
  - công cụ: decline_out_of_scope("Kỳ quý 3/2026 (kết thúc 30/09/2026) chưa được báo cáo. Dữ liệu mới nhất trong cơ sở là 30/06/2026 (6 tháng đầu năm 2026).")

### X04 — Lợi nhuận sau thuế năm 2015 của CTR là bao nhiêu?

**Đáp án vàng:** Từ chối: kho không có báo cáo năm 2015 (dữ liệu từ năm 2019, số so sánh từ cuối 2017).

- **FINQUERY** ✓ (từ chối kỳ không có, mọi số đều có nguồn, 8.6 s): Câu hỏi này nằm ngoài phạm vi FinQuery: Câu hỏi về năm 2015, ngoài phạm vi dữ liệu lưu trữ (từ 2019 đến nay).
  - công cụ: decline_out_of_scope("Câu hỏi về năm 2015, ngoài phạm vi dữ liệu lưu trữ (từ 2019 đến nay).")
