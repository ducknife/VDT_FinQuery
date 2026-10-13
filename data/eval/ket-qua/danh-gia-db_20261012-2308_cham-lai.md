# Đánh giá câu trả lời nhánh số liệu — 13/10/2026 08:06

Bộ câu hỏi: `data/eval/eval_dbquery.yml` (21 câu). Cùng model, chấm máy "đúng và đủ số được hỏi" (`DbAnswerScorer`), **cần người soát lại**. Chi tiết: `danh-gia-db_20261012-2308_cham-lai.csv`.

- FINQUERY: 3 tool SQL mẫu, LLM chỉ điền tham số
- TEXT_TO_SQL: LLM tự viết SQL (tool execute_sql, user chỉ đọc)
- LLM_ONLY: không công cụ, trả lời từ trí nhớ

## 1. Tổng hợp

| Cấu hình | Đúng | Một số | So sánh | Từ chối | Thời gian TB ± ĐLC (s) | p50 (s) | USD/câu | Token vào TB | Lỗi SQL | Số không có nguồn |
|---|---|---|---|---|---|---|---|---|---|---|
| FINQUERY | **21/21** | 11/11 | 7/7 | 3/3 | 8.2 ± 4.4 | 7.1 | 0.0505 | 22214 | 0 | 0 |
| TEXT_TO_SQL | **19/21** | 9/11 | 7/7 | 3/3 | 10.2 ± 2.4 | 10.4 | 0.0498 | 19172 | 0 | 0 |
| LLM_ONLY | **3/21** | 0/11 | 0/7 | 3/3 | 6.3 ± 0.8 | 6.5 | 0.0078 | 149 | 0 | 0 |

## 2. Từng câu

| Câu | Nhóm | FINQUERY | TEXT_TO_SQL | LLM_ONLY |
|---|---|---|---|---|
| D01 | db_mot_so | ✓ | ✓ | ✗ thiếu 12.609.519.560.081 (hoặc 12.609,52 tỷ) |
| D02 | db_mot_so | ✓ | ✗ thiếu 587.417.356.381 (hoặc 587,42 tỷ) | ✗ thiếu 587.417.356.381 (hoặc 587,42 tỷ) |
| D03 | db_mot_so | ✓ | ✓ | ✗ thiếu 7.435.180.982.045 (hoặc 7.435,18 tỷ) |
| D04 | db_mot_so | ✓ | ✓ | ✗ thiếu 424.971.712.679 (hoặc 424,97 tỷ) |
| D05 | db_mot_so | ✓ | ✓ | ✗ thiếu 295.939.415.278 (hoặc 295,94 tỷ) |
| D06 | db_mot_so | ✓ | ✓ | ✗ thiếu 521.996.574.332 (hoặc 522,00 tỷ) |
| D07 | db_mot_so | ✓ | ✓ | ✗ thiếu 3.773.814.395.170 (hoặc 3.773,81 tỷ) |
| D08 | db_mot_so | ✓ | ✓ | ✗ thiếu 146.262.730.079 (hoặc 146,26 tỷ) |
| C01 | db_so_sanh | ✓ | ✓ | ✗ thiếu 10,55% |
| C02 | db_so_sanh | ✓ | ✓ | ✗ thiếu 7,01% |
| C03 | db_so_sanh | ✓ | ✓ | ✗ thiếu 74,75% |
| C04 | db_so_sanh | ✓ | ✓ | ✗ thiếu 20,56% |
| C05 | db_so_sanh | ✓ | ✓ | ✗ thiếu 2,43 lần |
| C06 | db_so_sanh | ✓ | ✓ | ✗ thiếu 30,39% |
| C07 | db_so_sanh | ✓ | ✓ | ✗ thiếu 84,59% |
| D09 | db_mot_so | ✓ | ✓ | ✗ thiếu 5.099.889.976.331 (hoặc 5.099,89 tỷ) |
| D10 | db_mot_so | ✓ | ✓ | ✗ thiếu 7.106.257.863.668 (hoặc 7.106,26 tỷ) |
| D11 | db_mot_so | ✓ | ✗ thiếu 1.123.253.891.417 (hoặc 1.123,25 tỷ) | ✗ thiếu 1.123.253.891.417 (hoặc 1.123,25 tỷ) |
| X06 | tu_choi | ✓ | ✓ | ✓ |
| X03 | tu_choi | ✓ | ✓ | ✓ |
| X04 | tu_choi | ✓ | ✓ | ✓ |

## 3. Câu trả lời để soát

### D01 — Doanh thu thuần hợp nhất năm 2024 của CTR là bao nhiêu?

**Đáp án vàng:** 12.609.519.560.081 đồng

- **FINQUERY** ✓ (có số đồng chính xác, 7.2 s): Doanh thu thuần về bán hàng và cung cấp dịch vụ hợp nhất của CTR năm 2024 (01/01/2024–31/12/2024) là 12.609.519.560.081 đồng, tương đương 12.609,52 tỷ đồng [D1].
  - công cụ: get_financial_figures([DOANH_THU_THUAN], 1 kỳ, [CONSOLIDATED]) -> 1 số
- **TEXT_TO_SQL** ✓ (có số đồng chính xác, 8.9 s): Doanh thu thuần hợp nhất năm 2024 của CTR là 12.609.519.560.081 đồng, ghi tại trang 8 của Báo cáo tài chính hợp nhất năm 2024 (bản 1/2) [D1].
  - công cụ: execute_sql -> 1 dòng: SELECT f.ma_noi_bo, f.tu_ngay, f.den_ngay, f.pham_vi, f.gia_tri, f.trang_thai, f.trang, d.ten_hien_thi FROM financial_figures f LEFT JOIN documents d ON d.tai_lieu_id=f.tai_lieu_id WHERE f.ma_noi_bo='DOANH_THU_THUAN' AND f.pham_vi='hop_nhat' AND f.tu_ngay='2024-01-01' AND f.den_ngay='2024-12-31'
- **LLM_ONLY** ✗ (thiếu 12.609.519.560.081 (hoặc 12.609,52 tỷ), 5.5 s): Tôi không nhớ chính xác con số doanh thu thuần hợp nhất năm 2024 của CTR nên không thể đưa ra số liệu chính xác đến từng đồng. /  / Theo trí nhớ, doanh thu hợp nhất năm 2024 của CTR vào khoảng 7.800-7.900 tỷ đồng, tăng khoảng 35% so với năm 2023 (khoảng 5.800 tỷ đồng). Đây chỉ là ước lượng, chưa được kiểm chứng. Con số này cũng có thể là tổng doanh thu chứ chưa chắc là doanh thu thuần. /  / Bạn nên đối chiếu với Báo cáo tài chính hợp nhất kiểm toán năm 2024 của CTR, mục 'Doanh thu thuần về bán hàng và cung cấp dịch vụ' trong báo cáo kết quả kinh doanh. Báo cáo này được công bố trên website của Viettel Construction (ctr.com.vn), HOSE hoặc các cổng dữ liệu như CafeF và Vietstock.

### D02 — LNST trên báo cáo tài chính riêng năm 2025 là bao nhiêu?

**Đáp án vàng:** 587.417.356.381 đồng

- **FINQUERY** ✓ (có số đồng chính xác, 6.2 s): Lợi nhuận sau thuế TNDN trên báo cáo tài chính riêng năm 2025 (01/01/2025–31/12/2025) là 587.417.356.381 đồng, tức khoảng 587,42 tỷ đồng [D1].
  - công cụ: get_financial_figures([LOI_NHUAN_SAU_THUE], 1 kỳ, [SEPARATE]) -> 1 số
- **TEXT_TO_SQL** ✗ (thiếu 587.417.356.381 (hoặc 587,42 tỷ), 6.5 s): Lợi nhuận sau thuế trên báo cáo tài chính riêng năm 2025 là 587.417.356 381 đồng [D1].
  - công cụ: execute_sql -> 1 dòng: SELECT f.ma_noi_bo, f.tu_ngay, f.den_ngay, f.pham_vi, f.gia_tri, f.trang_thai, f.trang, d.ten_hien_thi FROM financial_figures f JOIN documents d ON d.tai_lieu_id=f.tai_lieu_id WHERE f.ma_noi_bo='LOI_NHUAN_SAU_THUE' AND f.pham_vi='rieng' AND f.tu_ngay='2025-01-01' AND f.den_ngay='2025-12-31'
- **LLM_ONLY** ✗ (thiếu 587.417.356.381 (hoặc 587,42 tỷ), 7.4 s): Tôi không có số liệu chính xác về lợi nhuận sau thuế (LNST) trên báo cáo tài chính riêng năm 2025 của Viettel Construction (CTR), nên không đưa ra con số để tránh sai lệch. Tôi cũng không muốn ước tính hay suy diễn từ các năm trước. /  / Bạn có thể tra cứu số liệu này tại: / - **Trang cổ đông của CTR** (viettelconstruction.com.vn), mục Quan hệ cổ đông / Báo cáo tài chính. Chọn BCTC riêng quý IV/2025 hoặc BCTC riêng năm 2025 đã kiểm toán. / - **HOSE** (hsx.vn) hoặc **UBCKNN** (congbothongtin.ssc.gov.vn), nơi công bố thông tin chính thức. / - **CafeF, Vietstock, FireAnt** và các trang dữ liệu tài chính khác, có mục BCTC riêng và hợp nhất. /  / Khi đọc báo cáo, hãy xem chỉ tiêu **'Lợi nhuận sau thuế thu nhập doanh nghiệp' (mã số 60)** trong Báo cáo kết quả hoạt động kinh doanh của báo cáo **riêng** (công ty mẹ), không phải báo cáo hợp nhất. Hai con số này thường khác nhau. /  / Nếu bạn gửi số liệu hoặc đoạn trích từ báo cáo, tôi có thể giúp đối chiếu, so sánh với năm trước hoặc tính các chỉ tiêu liên quan.

### D03 — Nợ phải trả hợp nhất tại kỳ báo cáo gần nhất là bao nhiêu?

**Đáp án vàng:** Kỳ gần nhất 30/06/2026: 7.435.180.982.045 đồng

- **FINQUERY** ✓ (có số đồng chính xác, 6.1 s): Nợ phải trả hợp nhất tại 30/06/2026 là 7.435.180.982.045 đồng (khoảng 7.435,18 tỷ đồng) [D1].
  - công cụ: get_financial_figures([NO_PHAI_TRA], 1 kỳ, [CONSOLIDATED]) -> 1 số
- **TEXT_TO_SQL** ✓ (có số đồng chính xác, 8.6 s): Nợ phải trả hợp nhất tại 30/06/2026 (kỳ báo cáo gần nhất trong dữ liệu) là 7.435.180.982.045 đồng. Số liệu lấy từ Báo cáo tài chính hợp nhất bán niên 2026, trang 7 [D1].
  - công cụ: execute_sql -> 1 dòng: SELECT f.ma_noi_bo, f.tu_ngay, f.den_ngay, f.pham_vi, f.gia_tri, f.trang_thai, f.trang, d.ten_hien_thi FROM financial_figures f JOIN documents d ON d.tai_lieu_id=f.tai_lieu_id WHERE f.ma_noi_bo='NO_PHAI_TRA' AND f.pham_vi='hop_nhat' AND f.den_ngay='2026-06-30' ORDER BY f.trang_thai
- **LLM_ONLY** ✗ (thiếu 7.435.180.982.045 (hoặc 7.435,18 tỷ), 6.9 s): Tôi không có số liệu chính xác, đã được kiểm chứng về nợ phải trả hợp nhất của Tổng Công ty cổ phần Công trình Viettel (CTR) ở kỳ báo cáo gần nhất, nên không đưa ra con số cụ thể để tránh sai lệch. Tôi cũng không có dữ liệu cập nhật theo thời gian thực để biết kỳ gần nhất hiện nay là quý nào. /  / Bạn có thể tra chỉ tiêu này như sau: / - **Nguồn:** báo cáo tài chính hợp nhất quý hoặc năm gần nhất trên website của CTR (mục Quan hệ cổ đông) hoặc trên HOSE. Các trang như CafeF, Vietstock, FireAnt cũng có số liệu này. / - **Chỉ tiêu:** trên Bảng cân đối kế toán hợp nhất, mã số 300 'C. Nợ phải trả' (gồm nợ ngắn hạn mã 310 và nợ dài hạn mã 330). / - **Đơn vị:** báo cáo thường trình bày bằng đồng Việt Nam (VND), nên cần kiểm tra xem số liệu ở đơn vị đồng hay triệu đồng. /  / Nếu bạn gửi số liệu từ báo cáo, tôi có thể giúp tính tỷ lệ nợ trên vốn chủ sở hữu, nợ trên tổng tài sản hoặc so sánh với các kỳ trước.

### D04 — HTK hợp nhất cuối năm 2025?

**Đáp án vàng:** 424.971.712.679 đồng (mã 140, đã trừ dự phòng)

- **FINQUERY** ✓ (có số đồng chính xác, 7.4 s): Hàng tồn kho hợp nhất tại 31/12/2025 (thuần, đã trừ dự phòng, mã 140) là 424.971.712.679 đồng, tức 424,97 tỷ đồng [D1]. Theo giá gốc (chưa trừ dự phòng, mã 141), hàng tồn kho hợp nhất tại 31/12/2025 là 428.595.861.715 đồng, tức 428,60 tỷ đồng [D2].
  - công cụ: get_financial_figures([HANG_TON_KHO_M140, HANG_TON_KHO_M141], 1 kỳ, [CONSOLIDATED]) -> 2 số
- **TEXT_TO_SQL** ✓ (có số đồng chính xác, 12.9 s): Tại 31/12/2025, chỉ tiêu hàng tồn kho hợp nhất (mã HANG_TON_KHO_M140) là 424.971.712.679 đồng [D1]. Cùng ngày, phạm vi hợp nhất, cơ sở dữ liệu còn có dòng HANG_TON_KHO_M141 là 428.595.861.715 đồng và dòng dự phòng giảm giá HTK (DU_PHONG_GIAM_GIA_HTK) là -3.624.149.036 đồng [D1]. Cả ba số đều lấy từ Báo cáo tài chính hợp nhất năm 2025 (bản 1/2), trang 6 [D1].
  - công cụ: execute_sql -> 3 dòng: SELECT f.ma_noi_bo, f.den_ngay, f.pham_vi, f.gia_tri, f.trang_thai, f.trang, d.ten_hien_thi FROM financial_figures f JOIN documents d ON d.tai_lieu_id=f.tai_lieu_id WHERE f.ma_noi_bo IN ('HANG_TON_KHO_M140','HANG_TON_KHO_M141','DU_PHONG_GIAM_GIA_HTK') AND f.den_ngay='2025-12-31' AND f.pham_vi='hop_nhat'
- **LLM_ONLY** ✗ (thiếu 424.971.712.679 (hoặc 424,97 tỷ), 5.8 s): Tôi không có số liệu chính xác về hàng tồn kho (HTK) hợp nhất của Viettel Construction (CTR) tại 31/12/2025, nên không đưa ra con số để tránh sai lệch. /  / Bạn có thể tra trực tiếp trong: / - **Báo cáo tài chính hợp nhất quý 4/2025** hoặc **báo cáo tài chính hợp nhất năm 2025 (đã kiểm toán)** của CTR, trên website công ty (phần Quan hệ cổ đông) hoặc trang công bố thông tin của HOSE/UPCoM. / - **Bảng cân đối kế toán hợp nhất**, mục 'Hàng tồn kho' (mã số 140, hoặc 141 nếu chưa trừ dự phòng), cột 'Số cuối năm'. /  / Với CTR, HTK chủ yếu là chi phí sản xuất kinh doanh dở dang của các công trình xây lắp và hạ tầng viễn thông đang thi công, nên số dư thường biến động theo tiến độ và thời điểm nghiệm thu dự án cuối năm. /  / Nếu bạn gửi số liệu hoặc đoạn trích báo cáo, tôi có thể giúp tính vòng quay HTK, so sánh với đầu năm hoặc phân tích cơ cấu.

### D05 — Dòng tiền kinh doanh hợp nhất quý 1/2025 âm hay dương, bao nhiêu?

**Đáp án vàng:** Âm: lưu chuyển tiền thuần từ HĐKD 3 tháng đầu 2025 là -295.939.415.278 đồng.

- **FINQUERY** ✓ (có số đồng chính xác, 13.0 s): Dòng tiền kinh doanh hợp nhất quý 1/2025 là ÂM: lưu chuyển tiền thuần từ hoạt động kinh doanh hợp nhất quý 1/2025 (01/01/2025–31/03/2025) đạt -295.939.415.278 đồng (khoảng -295,94 tỷ đồng) [D1].
  - công cụ: get_financial_figures([LCTT_THUAN_HDKD], 1 kỳ, [CONSOLIDATED]) -> 1 số
- **TEXT_TO_SQL** ✓ (có số đồng chính xác, 6.9 s): Dòng tiền thuần từ hoạt động kinh doanh hợp nhất quý 1/2025 (từ 01/01/2025 đến 31/03/2025) là số âm, đạt -295.939.415.278 đồng [D1]. Số liệu lấy từ Báo cáo tài chính hợp nhất quý 1 2025, trang 7 [D1]. Bằng chứng cung cấp không có giá trị quy đổi sang tỷ đồng và không giải thích nguyên nhân.
  - công cụ: execute_sql -> 1 dòng: SELECT f.ma_noi_bo, f.tu_ngay, f.den_ngay, f.pham_vi, f.gia_tri, f.trang_thai, f.trang, d.ten_hien_thi FROM financial_figures f JOIN documents d ON d.tai_lieu_id=f.tai_lieu_id WHERE f.ma_noi_bo='LCTT_THUAN_HDKD' AND f.pham_vi='hop_nhat' AND f.tu_ngay='2025-01-01' AND f.den_ngay='2025-03-31'
- **LLM_ONLY** ✗ (thiếu 295.939.415.278 (hoặc 295,94 tỷ), 7.1 s): Tôi không có số liệu đủ chắc chắn về lưu chuyển tiền thuần từ hoạt động kinh doanh hợp nhất quý 1/2025 của CTR (Tổng Công ty Cổ phần Công trình Viettel), nên không thể xác nhận là âm hay dương, cũng như giá trị cụ thể. Tôi không muốn đưa ra con số ước đoán vì có thể sai. /  / Bạn có thể tra trực tiếp như sau: / - **Tài liệu:** Báo cáo tài chính hợp nhất quý 1/2025 của CTR, phần 'Báo cáo lưu chuyển tiền tệ'. / - **Chỉ tiêu:** 'Lưu chuyển tiền thuần từ hoạt động kinh doanh' (mã số 20), đơn vị thường là đồng hoặc triệu đồng. / - **Nguồn:** Trang quan hệ cổ đông của CTR, website HOSE, hoặc các trang dữ liệu như Vietstock, CafeF, FireAnt. /  / Cần lưu ý chọn đúng báo cáo hợp nhất, không phải báo cáo riêng công ty mẹ, và kỳ quý 1/2025 (3 tháng kết thúc ngày 31/03/2025). /  / Nếu bạn gửi số liệu hoặc đoạn báo cáo, tôi có thể giúp phân tích nguyên nhân dòng tiền âm hoặc dương, ví dụ do biến động các khoản phải thu, hàng tồn kho hay các khoản phải trả.

### D06 — Lợi nhuận gộp hợp nhất 6 tháng đầu năm 2026 là bao nhiêu?

**Đáp án vàng:** 521.996.574.332 đồng

- **FINQUERY** ✓ (có số đồng chính xác, 5.8 s): Lợi nhuận gộp về bán hàng và cung cấp dịch vụ hợp nhất 6 tháng đầu năm 2026 (01/01/2026–30/06/2026) là 521.996.574.332 đồng, tương đương 522,00 tỷ đồng [D1].
  - công cụ: get_financial_figures([LOI_NHUAN_GOP], 1 kỳ, [CONSOLIDATED]) -> 1 số
- **TEXT_TO_SQL** ✓ (có số đồng chính xác, 8.8 s): Lợi nhuận gộp hợp nhất 6 tháng đầu năm 2026 (từ 01/01/2026 đến 30/06/2026) là 521.996.574.332 đồng [D1]. Số liệu lấy từ Báo cáo tài chính hợp nhất bán niên 2026, trang 8, trạng thái hiệu lực [D1].
  - công cụ: execute_sql -> 1 dòng: SELECT f.ma_noi_bo, f.tu_ngay, f.den_ngay, f.pham_vi, f.gia_tri, f.trang_thai, f.trang, d.ten_hien_thi FROM financial_figures f JOIN documents d ON d.tai_lieu_id=f.tai_lieu_id WHERE f.ma_noi_bo='LOI_NHUAN_GOP' AND f.pham_vi='hop_nhat' AND f.tu_ngay='2026-01-01' AND f.den_ngay='2026-06-30'
- **LLM_ONLY** ✗ (thiếu 521.996.574.332 (hoặc 522,00 tỷ), 7.0 s): Tôi không có số liệu về lợi nhuận gộp hợp nhất 6 tháng đầu năm 2026 của Viettel Construction (CTR). Kỳ báo cáo này nằm ngoài phạm vi dữ liệu tôi nắm được, và tôi không muốn đưa ra con số ước đoán có thể sai. /  / Bạn có thể tra cứu chính xác tại: / - **Báo cáo tài chính hợp nhất quý 2/2026** (hoặc báo cáo soát xét bán niên 2026) trong mục Quan hệ cổ đông trên website của CTR. / - **Cổng công bố thông tin của HOSE** (hoặc UBCKNN). / - **Các trang dữ liệu tài chính** như CafeF, Vietstock, FireAnt. /  / Trong báo cáo, bạn xem chỉ tiêu **'Lợi nhuận gộp về bán hàng và cung cấp dịch vụ'** (mã số 20) trong Báo cáo kết quả kinh doanh hợp nhất, cột lũy kế 6 tháng đầu năm. Nếu bạn gửi số liệu, tôi có thể giúp tính biên lợi nhuận gộp hoặc so sánh với cùng kỳ.

### D07 — Doanh thu thuần riêng của quý 2/2026 (chỉ riêng quý, không luỹ kế) là bao nhiêu?

**Đáp án vàng:** 3.773.814.395.170 đồng

- **FINQUERY** ✓ (có số đồng chính xác, 5.9 s): Doanh thu thuần về bán hàng và cung cấp dịch vụ riêng của quý 2/2026 (01/04/2026–30/06/2026, chỉ riêng quý) là 3.773.814.395.170 đồng, tương đương 3.773,81 tỷ đồng [D1].
  - công cụ: get_financial_figures([DOANH_THU_THUAN], 1 kỳ, [SEPARATE]) -> 1 số
- **TEXT_TO_SQL** ✓ (có số đồng chính xác, 7.1 s): Doanh thu thuần riêng của quý 2/2026 (từ 01/04/2026 đến 30/06/2026, chỉ riêng quý) là 3.773.814.395.170 đồng [D1]. Số liệu lấy từ Báo cáo tài chính riêng quý 2 2026, trang 5 [D1]. Bằng chứng không ghi giá trị quy đổi sang tỷ đồng nên tôi giữ nguyên số liệu bằng đồng.
  - công cụ: execute_sql -> 1 dòng: SELECT f.ma_noi_bo, f.tu_ngay, f.den_ngay, f.pham_vi, f.gia_tri, f.trang_thai, f.trang, d.ten_hien_thi FROM financial_figures f JOIN documents d ON d.tai_lieu_id=f.tai_lieu_id WHERE f.ma_noi_bo='DOANH_THU_THUAN' AND f.pham_vi='rieng' AND f.tu_ngay='2026-04-01' AND f.den_ngay='2026-06-30'
- **LLM_ONLY** ✗ (thiếu 3.773.814.395.170 (hoặc 3.773,81 tỷ), 4.8 s): Tôi không có số liệu về doanh thu thuần riêng (báo cáo tài chính riêng lẻ) của Tổng công ty Cổ phần Công trình Viettel (CTR) cho quý 2/2026. Kiến thức của tôi không bao gồm kỳ báo cáo này, và tôi không muốn đưa ra con số phỏng đoán vì có thể gây sai lệch. /  / **Gợi ý để tra cứu số liệu chính xác:** / - **Báo cáo tài chính riêng quý 2/2026** của CTR, công bố trên website của Viettel Construction (investor/quan hệ cổ đông) hoặc trên cổng thông tin của HOSE/UPCoM (CTR niêm yết trên UPCoM, sau đó chuyển sang HOSE). / - Tại báo cáo kết quả kinh doanh, xem chỉ tiêu **'Doanh thu thuần về bán hàng và cung cấp dịch vụ' (mã số 10)**, cột **'Quý 2/2026'** (3 tháng, từ 01/04 đến 30/06/2026), không phải cột lũy kế 6 tháng. / - Nếu chỉ có số lũy kế 6 tháng, bạn có thể tính doanh thu riêng quý 2 = lũy kế 6 tháng − doanh thu quý 1. /  / Nếu bạn cung cấp số liệu từ báo cáo, tôi có thể giúp tính toán, so sánh cùng kỳ hoặc phân tích xu hướng.

### D08 — Lãi ròng hợp nhất riêng quý 3/2024 là bao nhiêu?

**Đáp án vàng:** 146.262.730.079 đồng (chỉ quý III/2024)

- **FINQUERY** ✓ (có số đồng chính xác, 10.9 s): Câu hỏi có cụm 'hợp nhất riêng' hơi mơ hồ, nên tôi trả lời theo số liệu hợp nhất có trong tài liệu. Lợi nhuận sau thuế TNDN hợp nhất quý 3/2024 là 146.262.730.079 đồng (≈ 146,26 tỷ đồng) [D1]. Lợi nhuận sau thuế của công ty mẹ, hợp nhất, quý 3/2024 cũng là 146.262.730.079 đồng (≈ 146,26 tỷ đồng) [D2]. Hai chỉ tiêu này có cùng giá trị. Bằng chứng không có số liệu báo cáo riêng (công ty mẹ riêng lẻ) của quý 3/2024.
  - công cụ: get_financial_figures([LOI_NHUAN_SAU_THUE, LNST_CONG_TY_ME], 1 kỳ, [CONSOLIDATED]) -> 2 số
- **TEXT_TO_SQL** ✓ (có số đồng chính xác, 12.6 s): Câu hỏi có thể hiểu theo hai phạm vi nên dưới đây là cả hai. Lợi nhuận sau thuế quý 3/2024 (từ 01/07/2024 đến 30/09/2024) là: (1) hợp nhất: 146.262.730.079 đồng [D1]; (2) riêng: 138.986.117.657 đồng [D1]. Lợi nhuận sau thuế của công ty mẹ hợp nhất quý 3/2024 cũng là 146.262.730.079 đồng [D1]. Các số này được lấy từ Báo cáo tài chính hợp nhất quý 3 2025 và Báo cáo tài chính riêng quý 3 2025, trang 6 [D1]. Tôi dùng chỉ tiêu 'lợi nhuận sau thuế' cho 'lãi ròng'.
  - công cụ: execute_sql -> 3 dòng: SELECT f.ma_noi_bo, f.tu_ngay, f.den_ngay, f.pham_vi, f.gia_tri, f.trang_thai, f.trang, d.ten_hien_thi FROM financial_figures f LEFT JOIN documents d ON d.tai_lieu_id=f.tai_lieu_id WHERE f.ma_noi_bo IN ('LOI_NHUAN_SAU_THUE','LNST_CONG_TY_ME') AND f.tu_ngay='2024-07-01' AND f.den_ngay='2024-09-30' ORDER BY f.ma_noi_bo, f.pham_vi, f.trang_thai
- **LLM_ONLY** ✗ (thiếu 146.262.730.079 (hoặc 146,26 tỷ), 6.6 s): Tôi không nhớ chính xác con số lãi ròng hợp nhất riêng quý 3/2024 của Tổng công ty Cổ phần Công trình Viettel (CTR), nên không muốn đưa ra số liệu có thể sai. /  / Theo ấn tượng của tôi, lợi nhuận sau thuế quý 3/2024 của CTR ở mức khoảng vài chục đến gần 100 tỷ đồng và tăng so với cùng kỳ năm 2023. Đây chỉ là ước chừng, chưa được kiểm chứng. /  / Để lấy số chính xác, bạn có thể xem: / - **Báo cáo tài chính hợp nhất quý 3/2024 của CTR**, mục 'Lợi nhuận sau thuế thu nhập doanh nghiệp' và dòng 'Lợi nhuận sau thuế của cổ đông công ty mẹ' (nếu cần phần thuộc cổ đông công ty mẹ). / - Trang quan hệ cổ đông của Viettel Construction hoặc các trang như HOSE, CafeF, Vietstock. /  / Nếu bạn gửi số liệu hoặc đoạn báo cáo, tôi có thể giúp tính tăng trưởng so với cùng kỳ, biên lợi nhuận hoặc các chỉ tiêu liên quan.

### C01 — Doanh thu thuần hợp nhất năm 2025 tăng bao nhiêu phần trăm so với năm 2024?

**Đáp án vàng:** 13.939,52 tỷ đồng so với 12.609,52 tỷ đồng: tăng 10,55%.

- **FINQUERY** ✓ (có 10,55%, 6.3 s): Doanh thu thuần hợp nhất năm 2025 tăng 10,55% so với năm 2024 [D3]. Cụ thể, doanh thu thuần về bán hàng và cung cấp dịch vụ hợp nhất năm 2025 là 13.939,52 tỷ đồng [D2], so với 12.609,52 tỷ đồng của năm 2024 [D1]; mức tăng tuyệt đối là 1.330,00 tỷ đồng (1.329.997.709.061 đồng) [D3].
  - công cụ: get_financial_figures([DOANH_THU_THUAN], 2 kỳ, [CONSOLIDATED]) -> 2 số
- **TEXT_TO_SQL** ✓ (có 10,55%, 11.8 s): Doanh thu thuần hợp nhất năm 2025 tăng 10,55% so với năm 2024 [D2]. Mức tăng tuyệt đối là 1.329.997.709.061 đồng [D2]. Hai số liệu đầu vào: doanh thu thuần hợp nhất năm 2024 là 12.609.519.560.081 đồng [D1] và năm 2025 là 13.939.517.269.142 đồng [D1]. Cả hai đều lấy từ báo cáo tài chính hợp nhất, trang 8 [D1].
  - công cụ: execute_sql -> 2 dòng: SELECT f.ma_noi_bo, f.tu_ngay, f.den_ngay, f.pham_vi, f.gia_tri, f.trang_thai, f.trang, f.tai_lieu_id, d.ten_hien_thi FROM financial_figures f LEFT JOIN documents d ON d.tai_lieu_id=f.tai_lieu_id WHERE f.ma_noi_bo='DOANH_THU_THUAN' AND f.pham_vi='hop_nhat' AND f.tu_ngay IN ('2024-01-01','2025-01-01') AND f.den_ngay IN ('2024-12-31','2025-12-31') ORDER BY f.den_ngay, f.trang_thai || execute_sql -> 1 dòng: SELECT ROUND((13939517269142-12609519560081)/12609519560081*100,2) AS tang_truong_pct, 13939517269142-12609519560081 AS chenh_lech
- **LLM_ONLY** ✗ (thiếu 10,55%, 5.4 s): Tôi không có số liệu đáng tin cậy về doanh thu thuần hợp nhất năm 2025 của Viettel Construction (CTR), nên không thể đưa ra tỷ lệ tăng trưởng chính xác so với năm 2024. Nếu tôi nêu một con số, đó sẽ chỉ là phỏng đoán. /  / Theo trí nhớ, doanh thu thuần hợp nhất năm 2024 của CTR vào khoảng gần 7.000 tỷ đồng, nhưng tôi không chắc chắn về con số này. /  / Để có số chính xác, bạn có thể tra: / - Báo cáo tài chính hợp nhất kiểm toán năm 2025 của CTR, mục 'Doanh thu thuần về bán hàng và cung cấp dịch vụ'. / - Báo cáo tài chính quý IV/2025 hợp nhất, công bố trên website CTR hoặc HOSE/HNX/UPCoM. / - Nghị quyết hoặc tài liệu Đại hội đồng cổ đông thường niên 2026, thường có so sánh thực hiện 2025 với 2024. /  / Nếu bạn cung cấp số liệu doanh thu hai năm, tôi có thể tính giúp tỷ lệ tăng trưởng.

### C02 — Biên lợi nhuận gộp hợp nhất năm 2024 là bao nhiêu?

**Đáp án vàng:** Lợi nhuận gộp 884,22 tỷ đồng / doanh thu thuần 12.609,52 tỷ đồng = 7,01%.

- **FINQUERY** ✓ (có 7,01%, 6.7 s): Biên lợi nhuận gộp hợp nhất năm 2024 là 7,01% [D3]. Tỷ lệ này được tính bằng lợi nhuận gộp chia doanh thu thuần [D3]. Lợi nhuận gộp về bán hàng và cung cấp dịch vụ hợp nhất năm 2024 là 884,22 tỷ đồng [D1], doanh thu thuần về bán hàng và cung cấp dịch vụ hợp nhất năm 2024 là 12.609,52 tỷ đồng [D2].
  - công cụ: compute_financial_ratio(GROSS_MARGIN, FY/2024, CONSOLIDATED)
- **TEXT_TO_SQL** ✓ (có 7,01%, 10.8 s): Biên lợi nhuận gộp hợp nhất năm 2024 là 7,01% [D2]. Con số này là lợi nhuận gộp chia cho doanh thu thuần: lợi nhuận gộp hợp nhất năm 2024 là 884.221.709.521 đồng và doanh thu thuần hợp nhất năm 2024 là 12.609.519.560.081 đồng, theo Báo cáo tài chính hợp nhất năm 2024 (bản 1/2), trang 8 [D2][D1].
  - công cụ: execute_sql -> 3 dòng: SELECT f.ma_noi_bo, f.tu_ngay, f.den_ngay, f.pham_vi, f.gia_tri, f.trang_thai, f.trang, d.ten_hien_thi FROM financial_figures f JOIN documents d ON d.tai_lieu_id=f.tai_lieu_id WHERE f.ma_noi_bo IN ('LOI_NHUAN_GOP','DOANH_THU_THUAN','DOANH_THU_BAN_HANG') AND f.tu_ngay='2024-01-01' AND f.den_ngay='2024-12-31' AND f.pham_vi='hop_nhat' || execute_sql -> 1 dòng: SELECT g.gia_tri AS loi_nhuan_gop, t.gia_tri AS doanh_thu_thuan, ROUND(g.gia_tri/t.gia_tri*100,2) AS bien_gop_pct, g.trang, d.ten_hien_thi FROM financial_figures g JOIN financial_figures t ON t.ma_noi_bo='DOANH_THU_THUAN' AND t.tu_ngay=g.tu_ngay AND t.den_ngay=g.den_ngay AND t.pham_vi=g.pham_vi AND t.trang_thai='hieu_luc' JOIN documents d ON d.tai_lieu_id=g.tai_lieu_id WHERE g.ma_noi_bo='LOI_NHUAN_GOP' AND g.tu_ngay='2024-01-01' AND g.den_ngay='2024-12-31' AND g.pham_vi='hop_nhat' AND g.trang_thai='hieu_luc'
- **LLM_ONLY** ✗ (thiếu 7,01%, 6.2 s): Tôi không nhớ chính xác con số biên lợi nhuận gộp hợp nhất năm 2024 của Viettel Construction (CTR), nên không muốn đưa ra một số liệu có thể sai. /  / Theo trí nhớ, biên lợi nhuận gộp hợp nhất của CTR những năm gần đây nằm trong khoảng 10-11% doanh thu thuần, thấp vì đặc thù ngành xây lắp và hạ tầng viễn thông. Đây chỉ là mức ước chừng, chưa được kiểm chứng. /  / Để có số chính xác, bạn có thể tự tính từ báo cáo tài chính hợp nhất kiểm toán năm 2024 của CTR: /  / - Biên lợi nhuận gộp = Lợi nhuận gộp về bán hàng và cung cấp dịch vụ (mã số 20) ÷ Doanh thu thuần (mã số 10) × 100% / - Nguồn: báo cáo kết quả hoạt động kinh doanh hợp nhất trên website CTR (ctr.com.vn) hoặc các cổng như HOSE, CafeF, Vietstock. /  / Nếu bạn gửi doanh thu thuần và lợi nhuận gộp hợp nhất 2024, tôi có thể tính giúp.

### C03 — Tỷ lệ nợ phải trả trên tổng tài sản hợp nhất cuối năm 2025 là bao nhiêu?

**Đáp án vàng:** 6.118,37 tỷ đồng / 8.185,54 tỷ đồng = 74,75%.

- **FINQUERY** ✓ (có 74,75%, 6.5 s): Tỷ lệ nợ phải trả trên tổng tài sản hợp nhất tại 31/12/2025 là 74,75% [D3]. Tỷ lệ này được tính từ nợ phải trả hợp nhất tại 31/12/2025 là 6.118.368.595.239 đồng (≈ 6.118,37 tỷ đồng) [D1] và tổng tài sản hợp nhất tại 31/12/2025 là 8.185.544.854.669 đồng (≈ 8.185,54 tỷ đồng) [D2].
  - công cụ: compute_financial_ratio(DEBT_TO_ASSETS, FY/2025, CONSOLIDATED)
- **TEXT_TO_SQL** ✓ (có 74,75%, 11.6 s): Tại 31/12/2025, tỷ lệ nợ phải trả trên tổng tài sản hợp nhất là 74,75% [D2]. Số liệu đầu vào (hợp nhất, tại 31/12/2025): nợ phải trả là 6.118.368.595.239 đồng [D1] và tổng tài sản là 8.185.544.854.669 đồng [D1]. Cả hai số liệu lấy từ Báo cáo tài chính hợp nhất năm 2025 (bản 1/2) [D1].
  - công cụ: execute_sql -> 2 dòng: SELECT f.ma_noi_bo, f.den_ngay, f.pham_vi, f.gia_tri, f.trang_thai, f.trang, d.ten_hien_thi, f.tai_lieu_id FROM financial_figures f JOIN documents d ON d.tai_lieu_id=f.tai_lieu_id WHERE f.ma_noi_bo IN ('NO_PHAI_TRA','TONG_TAI_SAN') AND f.den_ngay='2025-12-31' AND f.pham_vi='hop_nhat' || execute_sql -> 1 dòng: SELECT a.gia_tri AS no_phai_tra, b.gia_tri AS tong_tai_san, ROUND(a.gia_tri/b.gia_tri*100,2) AS ty_le_pct FROM financial_figures a JOIN financial_figures b ON b.ma_noi_bo='TONG_TAI_SAN' AND b.den_ngay=a.den_ngay AND b.pham_vi=a.pham_vi AND b.trang_thai='hieu_luc' WHERE a.ma_noi_bo='NO_PHAI_TRA' AND a.den_ngay='2025-12-31' AND a.pham_vi='hop_nhat' AND a.trang_thai='hieu_luc'
- **LLM_ONLY** ✗ (thiếu 74,75%, 5.5 s): Tôi không có số liệu đáng tin cậy về tỷ lệ nợ phải trả trên tổng tài sản hợp nhất của Tổng công ty Cổ phần Công trình Viettel (CTR) tại thời điểm 31/12/2025. Tôi cũng không muốn đưa ra con số ước đoán, vì có thể sai về giá trị hoặc phạm vi hợp nhất/riêng lẻ. /  / Bạn có thể tự tính từ Báo cáo tài chính hợp nhất kiểm toán năm 2025 của CTR, trong Bảng cân đối kế toán hợp nhất tại ngày 31/12/2025: /  / - **Tỷ lệ nợ phải trả/tổng tài sản = Nợ phải trả (mã số 300) ÷ Tổng cộng tài sản (mã số 270)** /  / Nguồn tra cứu: trang quan hệ nhà đầu tư của CTR, HOSE, hoặc các cổng dữ liệu như CafeF, Vietstock, FiinTrade. /  / Nếu bạn gửi cho tôi hai số liệu nợ phải trả và tổng tài sản hợp nhất, tôi sẽ tính tỷ lệ và hỗ trợ phân tích cơ cấu nợ.

### C04 — LNST hợp nhất 6 tháng đầu 2026 tăng bao nhiêu so với cùng kỳ năm trước?

**Đáp án vàng:** 321,81 tỷ đồng so với 266,93 tỷ đồng: tăng 54,88 tỷ đồng (20,56%).

- **FINQUERY** ✓ (có 20,56%, 8.3 s): Lợi nhuận sau thuế TNDN hợp nhất 6 tháng đầu năm 2026 tăng 54.880.940.404 đồng (≈ 54,88 tỷ đồng), tương ứng tăng 20,56% so với cùng kỳ năm 2025 [D3]. Cụ thể: 6 tháng đầu năm 2026 đạt 321.807.293.831 đồng (≈ 321,81 tỷ đồng) [D2], còn 6 tháng đầu năm 2025 đạt 266.926.353.427 đồng (≈ 266,93 tỷ đồng) [D1]. Các tài liệu được cung cấp không nêu nguyên nhân của mức tăng này.
  - công cụ: get_financial_figures([LOI_NHUAN_SAU_THUE], 2 kỳ, [CONSOLIDATED]) -> 2 số
- **TEXT_TO_SQL** ✓ (có 20,56%, 12.1 s): LNST hợp nhất 6 tháng đầu năm 2026 tăng 54.880.940.404 đồng, tương ứng tăng 20,56% so với cùng kỳ năm 2025 [D2]. Cụ thể, LNST hợp nhất 6 tháng đầu năm 2026 là 321.807.293.831 đồng [D1], còn 6 tháng đầu năm 2025 là 266.926.353.427 đồng [D1].
  - công cụ: execute_sql -> 2 dòng: SELECT f.ma_noi_bo, f.tu_ngay, f.den_ngay, f.pham_vi, f.gia_tri, f.trang_thai, f.trang, d.ten_hien_thi FROM financial_figures f JOIN documents d ON d.tai_lieu_id=f.tai_lieu_id WHERE f.ma_noi_bo='LOI_NHUAN_SAU_THUE' AND f.pham_vi='hop_nhat' AND f.tu_ngay IN ('2026-01-01','2025-01-01') AND f.den_ngay IN ('2026-06-30','2025-06-30') ORDER BY f.den_ngay, f.trang_thai || execute_sql -> 1 dòng: SELECT SUM(CASE WHEN den_ngay='2026-06-30' THEN gia_tri END) AS ky_nay, SUM(CASE WHEN den_ngay='2025-06-30' THEN gia_tri END) AS cung_ky, SUM(CASE WHEN den_ngay='2026-06-30' THEN gia_tri END)-SUM(CASE WHEN den_ngay='2025-06-30' THEN gia_tri END) AS chenh_lech, ROUND((SUM(CASE WHEN den_ngay='2026-06-30' THEN gia_tri END)/SUM(CASE WHEN den_ngay='2025-06-30' THEN gia_tri END)-1)*100,2) AS tang_pct FROM financial_figures WHERE ma_noi_bo='LOI_NHUAN_SAU_THUE' AND pham_vi='hop_nhat' AND trang_thai='hieu_luc' AND tu_ngay IN ('2026-01-01','2025-01-01') AND den_ngay IN ('2026-06-30','2025-06-30')
- **LLM_ONLY** ✗ (thiếu 20,56%, 6.5 s): Tôi không có số liệu về lợi nhuận sau thuế (LNST) hợp nhất 6 tháng đầu năm 2026 của Tổng công ty cổ phần Công trình Viettel (CTR), nên không thể nêu mức tăng so với cùng kỳ năm trước. Báo cáo này nằm ngoài phạm vi dữ liệu của tôi, và tôi không muốn đưa ra con số ước đoán. /  / Bạn có thể kiểm tra số liệu chính xác tại: / - **Báo cáo tài chính hợp nhất quý II/2026** của CTR, ở phần 'Kết quả hoạt động kinh doanh', chỉ tiêu 'Lợi nhuận sau thuế'. Hãy so sánh cột lũy kế 6 tháng 2026 với cột cùng kỳ 2025. / - **Công bố thông tin trên HOSE** hoặc website quan hệ cổ đông của Viettel Construction. / - **Giải trình biến động lợi nhuận** do công ty công bố kèm báo cáo. /  / Khi so sánh, bạn nên xem LNST hợp nhất hay LNST thuộc cổ đông công ty mẹ, vì hai chỉ tiêu này khác nhau. Nếu bạn gửi số liệu hai kỳ, tôi có thể tính giúp tỷ lệ tăng trưởng.

### C05 — Năm 2025 dòng tiền thuần từ hoạt động kinh doanh hợp nhất gấp bao nhiêu lần lợi nhuận sau thuế?

**Đáp án vàng:** 1.455,42 tỷ đồng / 599,72 tỷ đồng ≈ 2,43 lần.

- **FINQUERY** ✓ (có 2,43 lần, 6.7 s): Năm 2025, dòng tiền thuần từ hoạt động kinh doanh hợp nhất gấp 2,43 lần lợi nhuận sau thuế hợp nhất [D3]. Số liệu đầu vào: lưu chuyển tiền thuần từ hoạt động kinh doanh hợp nhất năm 2025 là 1.455.415.537.450 đồng (≈ 1.455,42 tỷ đồng) [D1]; lợi nhuận sau thuế TNDN hợp nhất năm 2025 là 599.717.125.636 đồng (≈ 599,72 tỷ đồng) [D2]. Các tài liệu được cung cấp không giải thích nguyên nhân của chênh lệch này.
  - công cụ: relate_two_figures(LCTT_THUAN_HDKD FY/2025, LOI_NHUAN_SAU_THUE FY/2025, RATIO)
- **TEXT_TO_SQL** ✓ (có 2,43 lần, 11.7 s): Năm 2025, dòng tiền thuần từ hoạt động kinh doanh hợp nhất gấp 2,4268 lần lợi nhuận sau thuế hợp nhất [D2]. Hai số đầu vào: dòng tiền thuần từ hoạt động kinh doanh hợp nhất năm 2025 là 1.455.415.537.450 đồng [D1], và lợi nhuận sau thuế hợp nhất năm 2025 là 599.717.125.636 đồng [D1]. Cả hai lấy từ Báo cáo tài chính hợp nhất năm 2025 (bản 1/2) [D1]. Tỷ lệ này là kết quả hệ thống đã tính từ phép chia hai số trên [D2].
  - công cụ: execute_sql -> 2 dòng: SELECT f.ma_noi_bo, f.tu_ngay, f.den_ngay, f.pham_vi, f.gia_tri, f.trang_thai, f.trang, d.ten_hien_thi FROM financial_figures f JOIN documents d ON d.tai_lieu_id=f.tai_lieu_id WHERE f.ma_noi_bo IN ('LCTT_THUAN_HDKD','LOI_NHUAN_SAU_THUE') AND f.tu_ngay='2025-01-01' AND f.den_ngay='2025-12-31' AND f.pham_vi='hop_nhat' || execute_sql -> 1 dòng: SELECT 1455415537450/599717125636 AS ty_le
- **LLM_ONLY** ✗ (thiếu 2,43 lần, 7.6 s): Tôi không có số liệu đáng tin cậy về dòng tiền thuần từ hoạt động kinh doanh (CFO) hợp nhất và lợi nhuận sau thuế hợp nhất của CTR cho cả năm 2025, nên tôi không thể đưa ra con số chính xác về tỷ lệ này. Tôi cũng không muốn ước đoán vì CFO của CTR biến động mạnh theo từng năm, do phụ thuộc vào tiến độ thu hồi công nợ và thanh toán từ các dự án xây lắp hạ tầng viễn thông. /  / **Cách tính:** / - Tỷ lệ (lần) = Lưu chuyển tiền thuần từ hoạt động kinh doanh hợp nhất ÷ Lợi nhuận sau thuế hợp nhất, cả hai cùng kỳ năm 2025 và cùng đơn vị (tỷ đồng). / - Nếu muốn xét phần của cổ đông công ty mẹ, cần dùng 'lợi nhuận sau thuế của công ty mẹ' thay cho lợi nhuận sau thuế hợp nhất, và nêu rõ cách chọn. /  / **Nơi lấy số liệu:** / - Báo cáo tài chính hợp nhất kiểm toán năm 2025 của CTR, gồm Báo cáo kết quả kinh doanh và Báo cáo lưu chuyển tiền tệ, đăng trên website công ty (Quan hệ cổ đông) hoặc HOSE/CafeF/Vietstock. / - Nếu báo cáo kiểm toán chưa phát hành, có thể dùng báo cáo tài chính quý 4/2025 hợp nhất (số lũy kế 12 tháng). /  / Nếu bạn gửi hai số liệu này, tôi sẽ tính tỷ lệ và nhận xét về chất lượng dòng tiền.

### C06 — Hàng tồn kho hợp nhất cuối 2025 thay đổi thế nào so với cuối 2024?

**Đáp án vàng:** Giảm từ 610,48 tỷ đồng xuống 424,97 tỷ đồng: giảm 185,51 tỷ đồng (30,39%).

- **FINQUERY** ✓ (có 30,39%, 10.5 s): Hàng tồn kho hợp nhất (mã 140, thuần, đã trừ dự phòng) giảm 185.508.372.450 đồng (≈ 185,51 tỷ đồng), tương ứng 30,39%, tại 31/12/2025 so với 31/12/2024 [D3]. Cụ thể: tại 31/12/2024 là 610.480.085.129 đồng (≈ 610,48 tỷ đồng) [D1]; tại 31/12/2025 là 424.971.712.679 đồng (≈ 424,97 tỷ đồng) [D2]. Tính theo giá gốc (mã 141, chưa trừ dự phòng), hàng tồn kho hợp nhất cũng giảm 200.458.283.756 đồng (≈ 200,46 tỷ đồng), tương ứng 31,87% [D6], từ 629.054.145.471 đồng (≈ 629,05 tỷ đồng) tại 31/12/2024 [D4] xuống 428.595.861.715 đồng (≈ 428,60 tỷ đồng) tại 31/12/2025 [D5]. Các tài liệu được cung cấp không nêu nguyên nhân của sự thay đổi này.
  - công cụ: get_financial_figures([HANG_TON_KHO_M140, HANG_TON_KHO_M141], 2 kỳ, [CONSOLIDATED]) -> 4 số
- **TEXT_TO_SQL** ✓ (có 30,39%, 15.5 s): Hàng tồn kho hợp nhất cuối năm 2025 giảm so với cuối năm 2024. Cơ sở dữ liệu có hai chỉ tiêu hàng tồn kho (mã nội bộ M140 và M141), cả hai đều giảm: / - Chỉ tiêu HANG_TON_KHO_M140: tại 31/12/2024 là 610.480.085.129 đồng, tại 31/12/2025 là 424.971.712.679 đồng, giảm -185.508.372.450 đồng (-30,3873%) [D1]. / - Chỉ tiêu HANG_TON_KHO_M141: tại 31/12/2024 là 629.054.145.471 đồng, tại 31/12/2025 là 428.595.861.715 đồng, giảm -200.458.283.756 đồng (-31,8666%) [D1]. / Các số liệu trên lấy từ Báo cáo tài chính hợp nhất năm 2024 và năm 2025, trang 6 [D1]. Bằng chứng không nêu ý nghĩa của từng mã chỉ tiêu và không giải thích nguyên nhân của mức giảm.
  - công cụ: execute_sql -> 4 dòng: SELECT f.ma_noi_bo, f.den_ngay, f.pham_vi, f.gia_tri, f.trang_thai, f.trang, d.ten_hien_thi, f.gia_tri - LAG(f.gia_tri) OVER (PARTITION BY f.ma_noi_bo, f.trang_thai ORDER BY f.den_ngay) AS chenh_lech, (f.gia_tri / LAG(f.gia_tri) OVER (PARTITION BY f.ma_noi_bo, f.trang_thai ORDER BY f.den_ngay) - 1) * 100 AS pct FROM financial_figures f JOIN documents d ON d.tai_lieu_id = f.tai_lieu_id WHERE f.ma_noi_bo IN ('HANG_TON_KHO_M140','HANG_TON_KHO_M141') AND f.pham_vi='hop_nhat' AND f.den_ngay IN ('2024-12-31','2025-12-31') ORDER BY f.ma_noi_bo, f.den_ngay, f.trang_thai
- **LLM_ONLY** ✗ (thiếu 30,39%, 7.5 s): Tôi không có số liệu đáng tin cậy về hàng tồn kho hợp nhất của Tổng công ty Cổ phần Xây dựng Công trình Viettel (CTR) tại ngày 31/12/2025, nên không thể nói chính xác mức thay đổi so với 31/12/2024. Tôi cũng không muốn đưa ra con số ước đoán vì dễ sai về giá trị, đơn vị hoặc phạm vi hợp nhất/riêng lẻ. /  / Bạn có thể tự kiểm tra như sau: / - **Nguồn:** Báo cáo tài chính hợp nhất quý 4/2025, hoặc báo cáo kiểm toán năm 2025, trên website CTR hoặc các cổng thông tin như HOSE, CafeF, Vietstock. / - **Chỉ tiêu:** Mục 'Hàng tồn kho' (mã số 140) trên Bảng cân đối kế toán hợp nhất, và thuyết minh 'Hàng tồn kho' để xem chi tiết (chi phí sản xuất kinh doanh dở dang, nguyên vật liệu, công cụ dụng cụ, dự phòng giảm giá). / - **Cách tính:** Mức thay đổi tuyệt đối = số cuối năm 2025 − số đầu năm (tức cuối 2024). Mức thay đổi tương đối = chênh lệch ÷ số cuối năm 2024. /  / Với CTR, hàng tồn kho chủ yếu là chi phí sản xuất kinh doanh dở dang của các công trình xây lắp, hạ tầng viễn thông chưa nghiệm thu, nên thường biến động theo tiến độ và thời điểm nghiệm thu dự án. /  / Nếu bạn gửi số liệu từ báo cáo, tôi có thể giúp tính và phân tích mức thay đổi.

### C07 — Doanh thu thuần của công ty mẹ chiếm bao nhiêu phần trăm doanh thu thuần hợp nhất năm 2025?

**Đáp án vàng:** 11.791,01 tỷ đồng / 13.939,52 tỷ đồng = 84,59%.

- **FINQUERY** ✓ (có 84,59%, 7.1 s): Doanh thu thuần riêng của công ty mẹ chiếm 84,59% doanh thu thuần hợp nhất năm 2025 [D3]. Cụ thể, doanh thu thuần về bán hàng và cung cấp dịch vụ riêng năm 2025 là 11.791.011.025.399 đồng (≈ 11.791,01 tỷ đồng) [D1], còn doanh thu thuần hợp nhất năm 2025 là 13.939.517.269.142 đồng (≈ 13.939,52 tỷ đồng) [D2].
  - công cụ: relate_two_figures(DOANH_THU_THUAN FY/2025, DOANH_THU_THUAN FY/2025, SHARE)
- **TEXT_TO_SQL** ✓ (có 84,59%, 10.4 s): Doanh thu thuần riêng của công ty mẹ chiếm khoảng 84,58694% doanh thu thuần hợp nhất năm 2025 [D2]. Số liệu đầu vào: doanh thu thuần hợp nhất năm 2025 là 13.939.517.269.142 đồng [D1] và doanh thu thuần riêng năm 2025 là 11.791.011.025.399 đồng [D1]. Tỷ lệ này do hệ thống tính bằng công thức riêng × 100 / hợp nhất [D2].
  - công cụ: execute_sql -> 2 dòng: SELECT f.pham_vi, f.tu_ngay, f.den_ngay, f.gia_tri, f.trang_thai, f.trang, d.ten_hien_thi FROM financial_figures f LEFT JOIN documents d ON d.tai_lieu_id=f.tai_lieu_id WHERE f.ma_noi_bo='DOANH_THU_THUAN' AND f.tu_ngay='2025-01-01' AND f.den_ngay='2025-12-31' ORDER BY f.pham_vi, f.trang_thai || execute_sql -> 1 dòng: SELECT r.gia_tri AS rieng, h.gia_tri AS hop_nhat, r.gia_tri*100.0/h.gia_tri AS ty_le_pct FROM financial_figures r JOIN financial_figures h ON h.ma_noi_bo=r.ma_noi_bo AND h.tu_ngay=r.tu_ngay AND h.den_ngay=r.den_ngay AND h.pham_vi='hop_nhat' AND h.trang_thai='hieu_luc' WHERE r.ma_noi_bo='DOANH_THU_THUAN' AND r.pham_vi='rieng' AND r.trang_thai='hieu_luc' AND r.tu_ngay='2025-01-01' AND r.den_ngay='2025-12-31'
- **LLM_ONLY** ✗ (thiếu 84,59%, 6.1 s): Tôi không có số liệu đáng tin cậy về doanh thu thuần năm 2025 của CTR, cả báo cáo tài chính riêng (công ty mẹ) lẫn hợp nhất, nên không thể đưa ra tỷ lệ chính xác. Tôi cũng không muốn ước tính vì dễ sai. /  / Bạn có thể tự tính như sau: /  / - **Tỷ lệ (%) = Doanh thu thuần công ty mẹ (BCTC riêng) ÷ Doanh thu thuần hợp nhất (BCTC hợp nhất) × 100** / - Cả hai chỉ tiêu lấy ở mã số 10 trong Báo cáo kết quả hoạt động kinh doanh, đơn vị đồng hoặc tỷ đồng, kỳ từ 01/01 đến 31/12/2025. / - Nguồn: BCTC riêng và hợp nhất năm 2025 (nên dùng bản đã kiểm toán) trên website Viettel Construction (viettelconstruction.com.vn), HOSE hoặc CafeF, Vietstock. /  / Nếu bạn gửi hai con số này, tôi sẽ tính tỷ lệ và nhận xét giúp.

### D09 — Doanh thu thuần hợp nhất năm 2019 (theo số liệu đã trình bày lại) là bao nhiêu?

**Đáp án vàng:** 5.099.889.976.331 đồng (trình bày lại trong BCTC 2020; BCTC 2019 gốc ghi 5.053.710.724.827 đồng).

- **FINQUERY** ✓ (có số đồng chính xác, 7.7 s): Doanh thu thuần về bán hàng và cung cấp dịch vụ hợp nhất năm 2019 theo số đã trình bày lại là 5.099.889.976.331 đồng (≈ 5.099,89 tỷ đồng) [D1]. Số công bố ban đầu trong Báo cáo tài chính hợp nhất năm 2019 là 5.053.710.724.827 đồng (≈ 5.053,71 tỷ đồng) [D1].
  - công cụ: get_financial_figures([DOANH_THU_THUAN], 1 kỳ, [CONSOLIDATED]) -> 1 số
- **TEXT_TO_SQL** ✓ (có số đồng chính xác, 10.3 s): Doanh thu thuần hợp nhất năm 2019 theo số liệu đã trình bày lại là 5.099.889.976.331 đồng. Số này lấy từ Báo cáo tài chính hợp nhất năm 2020 (trang 8) và đang có hiệu lực [D1]. Số công bố ban đầu trong Báo cáo tài chính hợp nhất năm 2019 (bản 1/2, trang 9) là 5.053.710.724.827 đồng, đã bị thay thế [D1]. Dữ liệu không có số quy đổi sang tỷ đồng và không nêu lý do điều chỉnh, nên tôi không quy đổi hay giải thích.
  - công cụ: execute_sql -> 2 dòng: SELECT f.ma_noi_bo, f.tu_ngay, f.den_ngay, f.pham_vi, f.gia_tri, f.trang_thai, f.trang, f.tai_lieu_id, d.ten_hien_thi FROM financial_figures f LEFT JOIN documents d ON d.tai_lieu_id=f.tai_lieu_id WHERE f.ma_noi_bo='DOANH_THU_THUAN' AND f.pham_vi='hop_nhat' AND f.tu_ngay='2019-01-01' AND f.den_ngay='2019-12-31'
- **LLM_ONLY** ✗ (thiếu 5.099.889.976.331 (hoặc 5.099,89 tỷ), 7.2 s): Tôi không nắm chắc con số chính xác về doanh thu thuần hợp nhất năm 2019 của Viettel Construction (CTR) theo số liệu đã trình bày lại, nên không muốn đưa ra một con số có thể sai. /  / Tôi chỉ nhớ mang máng rằng doanh thu thuần hợp nhất của CTR năm 2019 vào khoảng 4–5 nghìn tỷ đồng. Đây chỉ là ước lượng, chưa đủ tin cậy để trích dẫn, và tôi cũng không biết số 'đã trình bày lại' khác số ban đầu bao nhiêu. /  / Để có số chính xác, bạn có thể kiểm tra: / - **Báo cáo tài chính hợp nhất năm 2020 đã kiểm toán** của CTR, cột số liệu so sánh năm 2019 (thường ghi là 'đã trình bày lại' nếu có điều chỉnh). / - **Báo cáo tài chính hợp nhất kiểm toán năm 2019**, để đối chiếu số gốc với số trình bày lại. / - Trang quan hệ cổ đông của CTR, hoặc cổng công bố thông tin của HOSE/HNX/UPCoM. /  / Nếu bạn gửi cho tôi đoạn báo cáo hoặc số liệu cụ thể, tôi có thể giúp đối chiếu hoặc phân tích thêm.

### D10 — tong tai san hop nhat ngay 31/12/2024 la bao nhieu

**Đáp án vàng:** 7.106.257.863.668 đồng

- **FINQUERY** ✓ (có số đồng chính xác, 5.8 s): Tổng tài sản hợp nhất của Viettel Construction tại 31/12/2024 là 7.106.257.863.668 đồng, tương đương 7.106,26 tỷ đồng [D1].
  - công cụ: get_financial_figures([TONG_TAI_SAN], 1 kỳ, [CONSOLIDATED]) -> 1 số
- **TEXT_TO_SQL** ✓ (có số đồng chính xác, 7.5 s): Tổng tài sản hợp nhất tại ngày 31/12/2024 là 7.106.257.863.668 đồng [D1]. Số liệu lấy từ Báo cáo tài chính hợp nhất năm 2024 (bản 1/2), trang 6 [D1]. Bằng chứng không ghi sẵn giá trị quy đổi sang tỷ đồng nên tôi giữ nguyên số liệu theo đồng.
  - công cụ: execute_sql -> 1 dòng: SELECT f.ma_noi_bo, f.tu_ngay, f.den_ngay, f.pham_vi, f.gia_tri, f.trang_thai, f.trang, f.tai_lieu_id, d.ten_hien_thi FROM financial_figures f LEFT JOIN documents d ON d.tai_lieu_id=f.tai_lieu_id WHERE f.ma_noi_bo='TONG_TAI_SAN' AND f.den_ngay='2024-12-31' AND f.pham_vi='hop_nhat'
- **LLM_ONLY** ✗ (thiếu 7.106.257.863.668 (hoặc 7.106,26 tỷ), 5.6 s): Tôi không nhớ chính xác con số tổng tài sản hợp nhất của Tổng công ty Cổ phần Công trình Viettel (CTR) tại ngày 31/12/2024, và tôi không muốn đưa ra số liệu phỏng đoán có thể sai. /  / Bạn có thể kiểm tra số liệu chính xác tại các nguồn sau: / - **Báo cáo tài chính hợp nhất quý 4/2024 hoặc năm 2024 đã kiểm toán** của CTR, ở mục 'Bảng cân đối kế toán hợp nhất', chỉ tiêu 'TỔNG CỘNG TÀI SẢN' (mã số 270). Báo cáo được đăng trên trang quan hệ cổ đông của Viettel Construction. / - Cổng công bố thông tin của HOSE hoặc UPCoM (CTR hiện niêm yết trên HOSE), hoặc các trang dữ liệu như CafeF, Vietstock, FireAnt. /  / Nếu bạn gửi cho tôi số liệu từ báo cáo tài chính, tôi có thể giúp phân tích cơ cấu tài sản, so sánh với cuối năm 2023, hoặc tính các chỉ số như ROA và đòn bẩy tài chính.

### D11 — Tiền mặt hợp nhất cuối quý trước là bao nhiêu?

**Đáp án vàng:** Quý trước = quý 1/2026 (kỳ mới nhất là quý 2/2026). Tiền và các khoản tương đương tiền hợp nhất tại 31/03/2026: 1.123.253.891.417 đồng

- **FINQUERY** ✓ (có số đồng chính xác, 10.5 s): Tại 31/03/2026 (cuối quý 1/2026), chỉ tiêu 'Tiền' (tiền mặt và tiền gửi không kỳ hạn) hợp nhất là 623.253.891.417 đồng (≈ 623,25 tỷ đồng) [D1]. Nếu tính cả các khoản tương đương tiền, chỉ tiêu 'Tiền và các khoản tương đương tiền' hợp nhất tại 31/03/2026 là 1.123.253.891.417 đồng (≈ 1.123,25 tỷ đồng) [D2]. Báo cáo không tách riêng tiền mặt khỏi tiền gửi không kỳ hạn trong dữ liệu được cung cấp.
  - công cụ: get_financial_figures([TIEN_M111, TIEN_VA_TUONG_DUONG_TIEN], 1 kỳ, [CONSOLIDATED]) -> 2 số
- **TEXT_TO_SQL** ✗ (thiếu 1.123.253.891.417 (hoặc 1.123,25 tỷ), 12.8 s): Tại 31/03/2026 (cuối quý 1/2026), chỉ tiêu tiền (mã nội bộ TIEN_M111) trên báo cáo tài chính hợp nhất là 623.253.891.417 đồng [D1]. Số liệu lấy từ Báo cáo tài chính hợp nhất quý 1 2026, trang 3 [D1]. Bằng chứng chỉ ghi giá trị theo đồng và không nêu cách diễn giải chi tiết của chỉ tiêu này, nên tôi giữ nguyên mã chỉ tiêu và không quy đổi sang tỷ đồng.
  - công cụ: execute_sql -> 1 dòng: SELECT f.ma_noi_bo, f.tu_ngay, f.den_ngay, f.pham_vi, f.gia_tri, f.trang_thai, f.trang, d.ten_hien_thi FROM financial_figures f JOIN documents d ON d.tai_lieu_id = f.tai_lieu_id WHERE f.ma_noi_bo = 'TIEN_M111' AND f.den_ngay = '2026-03-31' AND f.pham_vi = 'hop_nhat'
- **LLM_ONLY** ✗ (thiếu 1.123.253.891.417 (hoặc 1.123,25 tỷ), 6.5 s): Tôi không có số liệu chính xác để trả lời câu hỏi này, và tôi không muốn đưa ra con số phỏng đoán. /  / - **Chưa rõ kỳ báo cáo:** 'Quý trước' phụ thuộc vào thời điểm bạn hỏi, và tôi không có dữ liệu cập nhật theo thời gian thực về báo cáo tài chính mới nhất của CTR (Tổng công ty Cổ phần Công trình Viettel). / - **Dễ nhầm chỉ tiêu:** 'Tiền mặt' có thể hiểu là riêng khoản 'Tiền' (mã số 111) hoặc cả 'Tiền và các khoản tương đương tiền' (mã số 110). Hai chỉ tiêu này có thể chênh lệch đáng kể vì CTR có tiền gửi ngân hàng và các khoản tương đương tiền. /  / **Cách tra cứu số liệu chính xác:** / 1. Mở Báo cáo tài chính hợp nhất quý gần nhất của CTR trên website công ty (mục Quan hệ cổ đông) hoặc trên HOSE, CafeF, Vietstock. / 2. Xem Bảng cân đối kế toán hợp nhất, cột 'Số cuối kỳ', chỉ tiêu 'Tiền và các khoản tương đương tiền' (mã số 110). Đơn vị thường là đồng Việt Nam (VND). /  / Nếu bạn cho biết quý cụ thể (ví dụ Q2/2025) hoặc gửi số liệu từ báo cáo, tôi có thể giúp đối chiếu, phân tích hoặc so sánh với các kỳ khác.

### X06 — Doanh thu thuần hợp nhất cả năm 2026 là bao nhiêu?

**Đáp án vàng:** Chưa có số cả năm 2026 (kỳ mới nhất 30/06/2026). Có thể cung cấp số 6 tháng đầu 2026: 7.958.838.568.947 đồng. Không được tự ngoại suy ra số cả năm.

- **FINQUERY** ✓ (từ chối kỳ không có, mọi số đều có nguồn, 7.7 s): Tôi không tìm thấy dữ liệu về doanh thu thuần hợp nhất cả năm 2026 trong các tài liệu hiện có, nên không thể trả lời câu hỏi này.
  - công cụ: get_financial_figures([DOANH_THU_THUAN], 1 kỳ, [CONSOLIDATED]) -> 0 số
- **TEXT_TO_SQL** ✓ (từ chối kỳ không có, mọi số đều có nguồn, 10.6 s): Chưa có số liệu doanh thu thuần hợp nhất cả năm 2026 trong tài liệu được cung cấp. Dữ liệu hiện có chỉ gồm quý 1, quý 2 và 6 tháng đầu năm 2026. Doanh thu thuần hợp nhất quý 1/2026 là 3.804.825.619.753 đồng [D1]. Doanh thu thuần hợp nhất quý 2/2026 là 4.154.012.949.194 đồng [D1]. Doanh thu thuần hợp nhất 6 tháng đầu năm 2026 là 7.958.838.568.947 đồng [D1]. Tôi không cộng hay ước tính thêm để suy ra số cả năm.
  - công cụ: execute_sql -> 3 dòng: SELECT f.ma_noi_bo, f.tu_ngay, f.den_ngay, f.pham_vi, f.gia_tri, f.trang_thai, d.ten_hien_thi, f.trang FROM financial_figures f JOIN documents d ON d.tai_lieu_id=f.tai_lieu_id WHERE f.ma_noi_bo='DOANH_THU_THUAN' AND f.pham_vi='hop_nhat' AND f.den_ngay>='2026-01-01' ORDER BY f.tu_ngay, f.den_ngay
- **LLM_ONLY** ✓ (không đưa số cho kỳ không có, 5.5 s): Tôi không có số liệu doanh thu thuần hợp nhất cả năm 2026 của Tổng Công ty Cổ phần Công trình Viettel (CTR). Tôi không thể xác nhận con số này, và tôi cũng không muốn đưa ra số ước tính vì dễ gây hiểu nhầm. /  / Bạn có thể tra cứu số liệu chính thức tại: / - **Báo cáo tài chính hợp nhất kiểm toán năm 2026** của CTR, công bố trên website công ty (phần Quan hệ cổ đông) hoặc trên HOSE/UPCoM. / - **Báo cáo tài chính quý IV/2026 (hợp nhất)**, nếu cần số liệu sớm hơn bản kiểm toán. / - Các cổng dữ liệu như CafeF, Vietstock, FiinTrade. /  / Nếu bạn gửi số liệu hoặc báo cáo, tôi có thể giúp phân tích tăng trưởng, biên lợi nhuận hoặc so sánh với các năm trước.

### X03 — Doanh thu thuần hợp nhất quý 3/2026 là bao nhiêu?

**Đáp án vàng:** Từ chối: kỳ mới nhất trong kho là 30/06/2026, chưa có báo cáo quý 3/2026.

- **FINQUERY** ✓ (từ chối kỳ không có, mọi số đều có nguồn, 24.4 s): Câu hỏi này nằm ngoài phạm vi FinQuery: Quý 3/2026 chưa được công bố. Dữ liệu mới nhất hiện có là đến ngày 30/06/2026, nên chưa có doanh thu thuần hợp nhất của quý 3/2026.
  - công cụ: decline_out_of_scope('Quý 3/2026 chưa được công bố. Dữ liệu mới nhất hiện có là đến ngày 30/06/2026, nên chưa có doanh thu thuần hợp nhất của quý 3/2026.')
- **TEXT_TO_SQL** ✓ (từ chối kỳ không có, mọi số đều có nguồn, 9.0 s): Cơ sở dữ liệu hiện chưa có doanh thu thuần hợp nhất quý 3/2026 (01/07/2026–30/09/2026). Số liệu mới nhất là của quý 2/2026. Doanh thu thuần hợp nhất quý 2/2026 (01/04/2026–30/06/2026) là 4.154.012.949.194 đồng [D1]. Doanh thu thuần hợp nhất quý 1/2026 là 3.804.825.619.753 đồng [D1]. Doanh thu thuần hợp nhất 6 tháng đầu năm 2026 là 7.958.838.568.947 đồng [D1].
  - công cụ: execute_sql -> 3 dòng: SELECT f.ma_noi_bo, f.tu_ngay, f.den_ngay, f.pham_vi, f.gia_tri, f.trang_thai, d.ten_hien_thi, f.trang FROM financial_figures f JOIN documents d ON d.tai_lieu_id=f.tai_lieu_id WHERE f.ma_noi_bo='DOANH_THU_THUAN' AND f.pham_vi='hop_nhat' AND f.den_ngay>='2026-01-01' ORDER BY f.den_ngay, f.tu_ngay
- **LLM_ONLY** ✓ (không đưa số cho kỳ không có, 6.0 s): Tôi không có số liệu doanh thu thuần hợp nhất quý 3/2026 của Tổng công ty cổ phần Công trình Viettel (CTR). Dữ liệu của tôi không bao gồm báo cáo tài chính của kỳ này, nên tôi không thể đưa ra con số chính xác và không muốn ước đoán. /  / Bạn có thể tra cứu số liệu chính thức tại: / - **Báo cáo tài chính hợp nhất quý 3/2026** của CTR, mục 'Báo cáo kết quả hoạt động kinh doanh', chỉ tiêu 'Doanh thu thuần về bán hàng và cung cấp dịch vụ' (đơn vị thường là đồng hoặc triệu đồng). / - Trang quan hệ cổ đông của CTR (ctr.vn) hoặc các công bố thông tin trên HOSE/UPCoM. / - Các nguồn dữ liệu như CafeF, Vietstock, FiinTrade. /  / Nếu bạn gửi số liệu hoặc báo cáo, tôi có thể giúp phân tích, ví dụ so sánh cùng kỳ, tăng trưởng theo quý hoặc biên lợi nhuận.

### X04 — Lợi nhuận sau thuế năm 2015 của CTR là bao nhiêu?

**Đáp án vàng:** Từ chối: kho không có báo cáo năm 2015 (dữ liệu từ năm 2019, số so sánh từ cuối 2017).

- **FINQUERY** ✓ (từ chối kỳ không có, mọi số đều có nguồn, 2.0 s): Câu hỏi này nằm ngoài phạm vi FinQuery: Năm 2015 nằm ngoài phạm vi dữ liệu (chỉ có tài liệu từ năm 2019 đến kỳ gần nhất 30/06/2026), nên không thể cung cấp lợi nhuận sau thuế năm 2015 của CTR.
  - công cụ: decline_out_of_scope('Năm 2015 nằm ngoài phạm vi dữ liệu (chỉ có tài liệu từ năm 2019 đến kỳ gần nhất 30/06/2026), nên không thể cung cấp lợi nhuận sau thuế năm 2015 của CTR.')
- **TEXT_TO_SQL** ✓ (từ chối kỳ không có, mọi số đều có nguồn, 7.8 s): Không tìm thấy số liệu lợi nhuận sau thuế năm 2015 của CTR trong cơ sở dữ liệu. Truy vấn cho giai đoạn 2015–2016 không trả về dòng nào [D1]. Dữ liệu lợi nhuận sau thuế hiện có chỉ bao gồm các kỳ từ 2018-01-01 đến 2026-06-30 (80 dòng) [D2].
  - công cụ: execute_sql -> 0 dòng: SELECT f.ma_noi_bo, f.tu_ngay, f.den_ngay, f.pham_vi, f.gia_tri, f.trang_thai, f.trang, d.ten_hien_thi FROM financial_figures f LEFT JOIN documents d ON d.tai_lieu_id=f.tai_lieu_id WHERE f.ma_noi_bo='LOI_NHUAN_SAU_THUE' AND f.den_ngay BETWEEN '2015-01-01' AND '2016-12-31' ORDER BY f.den_ngay, f.tu_ngay || execute_sql -> 1 dòng: SELECT MIN(tu_ngay) mn, MAX(den_ngay) mx, COUNT(*) c FROM financial_figures WHERE ma_noi_bo='LOI_NHUAN_SAU_THUE'
- **LLM_ONLY** ✓ (không đưa số cho kỳ không có, 6.5 s): Tôi không nhớ chính xác con số lợi nhuận sau thuế năm 2015 của CTR (Tổng Công ty Cổ phần Công trình Viettel), kể cả phạm vi hợp nhất hay riêng lẻ. Tôi không muốn đưa ra một số liệu phỏng đoán. /  / Bạn có thể kiểm tra trong báo cáo tài chính kiểm toán năm 2015 (hợp nhất và riêng lẻ), bản cáo bạch hoặc tài liệu đăng ký giao dịch UPCoM của CTR, hay trên website quan hệ cổ đông của Viettel Construction, HNX/UPCoM hoặc các nguồn như CafeF, Vietstock. /  / Nếu bạn có báo cáo tài chính trong tay, tôi có thể giúp đọc và phân tích chỉ tiêu 'Lợi nhuận sau thuế thu nhập doanh nghiệp'.
