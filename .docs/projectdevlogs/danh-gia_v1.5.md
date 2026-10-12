# Kế hoạch đánh giá — v1.5

*Ngày: 2026-10-12 · Thay cho [v1.4](danh-gia_v1.4.md)*

## Thay đổi so với v1.4
- **Benchmark truy hồi** 3 lần và 8 cấu hình, gồm RAG cơ bản (mục 4). Bộ câu hỏi đổi tên thành `data/eval/eval_test.yml`.
- Độ trễ từng bước (mục 4.4); lỗi do bộ đánh giá phát hiện (mục 4.5).

## Thay đổi của v1.4 so với v1.3
- **Đã có bộ câu hỏi chuẩn** (T1 rút gọn còn 50 câu, cũng dùng cho T5 so sánh reranker): `data/eval/cau-hoi_v1.yml`, trang đáp án có chấm mức, ca biên, trang dễ nhầm (mục 1).
- Phát hiện lỗi trích số ở bảng vắt trang khi soạn đáp án (mục 2).
- Các mục khác của v1.3 (T2–T8, cách đo độ trễ, token, chi phí) giữ nguyên, xem [v1.3](danh-gia_v1.3.md).

## 1. Bộ 50 câu: `data/eval/cau-hoi_v1.yml`

- Sinh bằng `data/eval/dung_bo_cau_hoi.py`. Mỗi câu khai báo **chuỗi bằng chứng** (nguyên văn trong trang OCR) và tài liệu chính. Script quét toàn bộ trang OCR của kho, tìm **mọi** trang chứa bằng chứng và chấm mức: 2 = trang thuộc tài liệu chính, 1 = trang khác cũng chứa đúng bằng chứng (BCTN nhắc lại, báo cáo kỳ sau ghi cột kỳ trước). Tài liệu chính không chứa bằng chứng thì script dừng và báo lỗi.
- Phép tính so sánh, tỷ lệ do script tính bằng `Decimal`, không tính tay.
- Mức 2/1 dùng cho nDCG@20 (chỉ số chính của paper). `can_du_moi_bang_chung` đánh dấu 12 câu cần đủ nhiều trang mới trả lời được.
- `trang_de_nham` (29 trang ở 9 câu): trang trông giống nhưng sai (khác năm, khác phạm vi, quý thay vì luỹ kế, số trước khi trình bày lại), dùng để phân tích reranker có bị lừa không.

| Nhóm | Số câu |
|---|---|
| RAG đoạn văn (vì sao, quyết định, nhân sự) | 11 |
| RAG tra bảng (số ngoài danh mục chỉ tiêu BCTC) | 10 |
| RAG nhiều trang / nhiều kỳ | 5 |
| DB một con số | 11 |
| DB so sánh, tỷ lệ | 7 |
| Từ chối / thiếu dữ liệu | 6 |

Ca biên đã phủ: viết tắt (7), cách gọi đời thường (5), thời gian tương đối (4), báo cáo riêng (4), tỷ lệ (4), không dấu (2), quý và luỹ kế (2), bán niên (2), cuối năm (3), nhiều năm/kỳ (4), số âm, bảng vắt trang không tiêu đề, số liệu trình bày lại, năm liền kề dễ nhầm, kỳ chưa có, kỳ chưa đủ (trả lời một phần), công ty khác, dự báo giá, năm ngoài kho, câu không liên quan.

Lệch so với dự kiến 25/15/5: thành 26 RAG / 18 DB / 6 từ chối để đủ 50 câu và thêm ca biên.

## 2. Lỗi phát hiện khi soạn: trích số gán nhầm cột ở bảng vắt trang

BCTC hợp nhất Q3/2024 trang 6 là trang nối tiếp không có tiêu đề cột; dòng LNST chỉ có 3 giá trị (146.262.730.079 = Q3/2024; 140.605.159.668 = Q3/2023; 387.502.476.388 = luỹ kế 9 tháng 2024). `financial_figures` đang ghi 140.605.159.668 là **luỹ kế 9 tháng 2024** (sai). Sửa ngày 12/10: thêm luật kiểm "luỹ kế ≥ quý" cho chỉ tiêu không âm, trích lại. Câu D08 giữ ca này để kiểm.

## 3. Cần người kiểm chứng

Đối chiếu với PDF: câu hỏi có hợp lý không, đáp án đúng không, trang mức 2 đúng không. Các câu có nhiều trang mức 1 (B02, M01, M04, C0x, D10) chủ yếu do số được nhắc lại ở báo cáo kỳ sau và tài liệu ĐHĐCĐ; đã loại trùng chữ sai nghĩa ("12.000 trạm" trong BCTN 2019 nói về năm 2009).

---

## 4. Benchmark truy hồi (12/10/2026)

**File:** bộ câu hỏi đổi tên `data/eval/cau-hoi_v1.yml` → `data/eval/eval_test.yml` (thêm `bang_chung_so` cho từng trang vàng). Kết quả trong `data/eval/ket-qua/`: `benchmark-truy-hoi_20261012-0800.{csv,md}` (lần 3, 8 cấu hình), hai lần trước (`..._0730_truoc-sua-bo-loc.csv`, `..._0747_lan2-chinh.csv`), bảng tổng hợp `benchmark-truy-hoi_20261012.xlsx`, đo độ trễ `do-tre_*.md`.
**Code:** `modules/evaluation/RetrievalMetrics` (Recall@k, nDCG@20, nDCG@20 theo bằng chứng, MRR, bị lừa, bootstrap; 6 test), `RetrievalBenchmarkTest` (chạy cấu hình, xuất csv/md, chạy tiếp được khi bị ngắt), `LatencyProbeTest` (độ trễ từng bước).

### 4.1. Ba lần chạy cấu hình chính (45 câu có trang đáp án)

| Lần | Thay đổi | Recall@20 | nDCG@20 | MRR |
|---|---|---|---|---|
| 1 | ban đầu | 73,3% | 0,367 | 0,513 |
| 2 | bỏ lọc loại theo chữ "BCTC"; văn bản sự kiện nhận năm Y+1 | 84,4% | 0,431 | 0,551 |
| 3 | năm ban hành ≠ năm nội dung (sự kiện Y..Y+2, kế hoạch Y-1, trình bày lại Y+1, "đến nay"); "hợp nhất" thắng "riêng"; bổ sung nhãn V05 (đã kiểm tay) | **95,6%** | **0,483** | **0,623** |

Ở mọi lần, Recall@80 = Recall@40 = Recall@20: **reranker không làm rơi trang đúng nào**; câu trượt đều mất từ bước Qdrant (bộ lọc, embedding). Các sửa rút ra từ chính bộ 50 câu nên điểm lần 3 có phần **lạc quan** (chưa có bộ câu độc lập).

### 4.2. So sánh 8 cấu hình (lần 3; bootstrap ghép cặp 10.000 lần, KTC 95%)

| Cấu hình | Recall@20 | nDCG@20 | MRR | p50 / p95 | USD/câu (rerank) | Δ nDCG@20 [KTC] | Kết luận |
|---|---|---|---|---|---|---|---|
| **CHINH** (từ khoá + lọc + 0.6B → 4B, 80 → 40 → 20) | **95,6%** | **0,483** | **0,623** | 9,8 / 14,8 s | 0,0026 | — | |
| RAG_CO_BAN (nguyên câu hỏi, vector top 20) | 68,9% | 0,294 | 0,357 | 0,8 / 13,7 s | 0 | −0,190 [−0,266; −0,120] | **đáng tin**: Recall −26,7 điểm [−42,2; −13,3] |
| KHONG_RERANK | 91,1% | 0,381 | 0,526 | 0,6 / 7,6 s | 0 | −0,103 [−0,177; −0,032] | **đáng tin**: rerank cải thiện xếp hạng ~27% |
| MOT_TANG_4B | 95,6% | 0,485 | 0,619 | 11,8 / 23,8 s | 0,0029 | +0,002 [−0,015; +0,017] | ngang chất lượng; 2 tầng nhanh, ổn định, rẻ hơn |
| RUT_GON (50 → 20 → 10) | 86,7% | 0,450 | 0,605 | 11,1 / 19,1 s | 0,0014 | −0,034 [−0,060; −0,010] | **đáng tin**: mất đáp án (Recall −8,9%) |
| B_4B_8B | 91,1% | 0,441 | 0,503 | 19,9 / 37,1 s | 0,0059 | −0,043 [−0,099; +0,015] | không tốt hơn; tiền ×2,3, thời gian ×2 |
| KHONG_LOC | 93,3% | 0,465 | 0,609 | 10,9 / 22,1 s | 0,0027 | −0,018 [−0,053; +0,015] | chưa đáng tin; lọc giúp ổn định thời gian |
| KHONG_TU_KHOA | 93,3% | 0,459 | 0,562 | 11,2 / 18,9 s | 0,0025 | −0,024 [−0,055; +0,003] | chưa đáng tin; MRR giảm |

Điểm rerank cao nhất **không dùng làm ngưỡng từ chối được**: câu "doanh thu FPT" đạt 0,994, ngang câu trả lời được (chỉ câu không liên quan thấp, 0,012). Việc từ chối để agent/LLM quyết.

### 4.3. Vì sao nDCG@20 thấp hơn paper (0,48 so với 0,79)

Không so trực tiếp được (khác dữ liệu, ngôn ngữ, câu hỏi). Nguyên nhân chính là cách gán nhãn: trung bình **7,4 trang vàng mỗi câu** (cùng con số in lại ở nhiều báo cáo), nDCG đòi lấy hết. Câu có 1–2 trang vàng: nDCG 0,660; 3–6 trang: 0,496; 7+ trang: 0,338, trong khi MRR lại tăng (0,549 → 0,637 → 0,666). Vì vậy thêm **nDCG@20 theo bằng chứng** (mỗi bằng chứng một trang). Paper cũng chỉ khẳng định mức tăng tương đối; mức tăng của rerank ở đây (~27%) lớn hơn của paper.

### 4.4. Độ trễ đến khi có 20 chunk (10 câu × 2 lượt, `LatencyProbeTest`)

| Lần đo | Embed | Rerank tầng 1 | Rerank tầng 2 | Tổng trung vị / p90 |
|---|---|---|---|---|
| mạng cũ | 1,4 s | 3,4 s | 3,4 s | 8,9 / 11,0 s |
| 5G lần 1 | 2,3 s | 2,4 s | 3,6 s | 8,9 / 16,0 s |
| 5G lần 2 | 3,1 s | 2,7 s | 3,6 s | 9,5 / 14,1 s |

Code của mình < 0,1 s; ~99% là chờ DeepInfra. Embed (≈50 token) dao động 0,5–13 s: xếp hàng GPU dùng chung, không phải mạng. Hướng giảm: gửi đúp yêu cầu embed khi chậm, lưu sẵn vector và kết quả, bỏ nửa tiếng Anh và rút ngắn chunk ở tầng 1, hiện tiến trình/chữ chạy dần.

### 4.5. Lỗi thật do bộ đánh giá phát hiện

1. Bộ lọc năm coi năm ghi trên tài liệu là năm nội dung (V04, V05, V08, V10, D09).
2. Bộ lọc loại hiểu chữ "báo cáo tài chính" là loại tài liệu (V01).
3. Trích số đọc lệch cột ở trang nối tiếp bị rơi một cột: 39 số sai được sửa/loại, 9 số đúng bị loại oan được nhận lại (6.544 → 6.521 số). Xem `quyet-dinh_v1.19` QĐ-033.
4. Nhãn thiếu trang đúng ở V05 (báo cáo kiểm toán trong BCTC/BCTN 2024), đã bổ sung và ghi chú trong `dung_bo_cau_hoi.py`.

### 4.6. Còn lại

- Bộ câu độc lập (~10 câu mới) để kiểm chứng các sửa đổi không bị khớp quá mức với bộ 50 câu.
- So sánh text-to-SQL với function calling trên 18 câu DB; độ chính xác câu trả lời (RAG phân cấp so với RAG cơ bản) sau khi có agent.
