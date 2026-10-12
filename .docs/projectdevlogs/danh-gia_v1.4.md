# Kế hoạch đánh giá — v1.4

*Ngày: 2026-10-11 · Thay cho [v1.3](danh-gia_v1.3.md)*

## Thay đổi so với v1.3
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
