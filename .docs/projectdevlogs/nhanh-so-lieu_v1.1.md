# Nhánh số liệu (tool SQL của agent) — v1.1

*Ngày: 2026-10-12, 22:50 · Kế thừa [nhanh-so-lieu_v1.0.md](nhanh-so-lieu_v1.0.md). Phần thiết kế, kết quả test và 8 câu chạy thật giữ nguyên như v1.0; bản này chỉ ghi phần mới.*

## 1. User MySQL chỉ đọc: đã tạo và kiểm

Người làm đề tài tạo user `finq_reader` theo [data/sql/create_readonly_user.sql](../../data/sql/create_readonly_user.sql) và thêm `DB_READONLY_*` vào `.env`. Claude kiểm, không in mật khẩu:

| Thử bằng `finq_reader` | Kết quả |
|---|---|
| `SELECT COUNT(*) FROM financial_figures` | ✔ 6.780 dòng |
| `DELETE FROM financial_figures` | ✘ `DELETE command denied` |
| `UPDATE documents` | ✘ `UPDATE command denied` |
| `SELECT ... FROM extraction_exceptions` (bảng không được cấp) | ✘ `SELECT command denied` |
| `DROP TABLE financial_figures` | ✘ `DROP command denied` |
| `SHOW GRANTS` | 1 dòng USAGE + 3 dòng SELECT (documents, financial_figures, financial_items) |

Kiểm ứng dụng dùng đúng user này: bài test `FinancialToolsGoldTest` chạy lại **3/3 xanh**. Log khởi động in *"tool số liệu dùng user chỉ đọc finq_reader"* và mở pool `finq-readonly`.

**Ba lớp an toàn của nhánh số liệu đã đủ:**
1. SQL hằng số với PreparedStatement.
2. Java kiểm tham số.
3. MySQL chặn mọi lệnh ghi và mọi bảng khác. Lớp này nằm ngoài code của mình.

## 2. Tổ chức lại thư mục

Theo yêu cầu, `modules/agent/tools/financial/` chỉ còn **3 tool chính**: `GetFinancialFigures`, `RelateTwoFigures`, `ComputeFinancialRatio`. Các class thành phần chuyển vào `financial/support/`: `FigureQueryRepository` (4 câu SQL hằng số), `IndicatorCatalog`, `FigureLookup`, `PeriodCode`, `ReportPeriod`, `PeriodInput`, `FigureRef`, `Scope`, `FinancialRatio`, `VnNumber`.

## 3. Đối chiếu công thức với số CTR tự công bố (BCTN 2025, trang 27–28)

| Tỷ số | Java | CTR công bố | Khớp |
|---|---|---|---|
| ROE | 30,50% (VCSH bình quân) | 30,50% (phần phân tích, trang 28) | ✔ |
| Nợ / Tổng tài sản | 74,75% | 0,75 lần | ✔ |
| Nợ / VCSH | 2,96 lần (tính tay từ số DB) | 2,96 | ✔ |
| ROA | ≈ 7,84% (tính tay, bình quân) | 7,84% (trang 28) | ✔ |

**Phát hiện:** CTR dùng **hai quy ước trong cùng báo cáo**:
- bảng số trang 27 chia cho số **cuối kỳ**: LNST/VCSH 29,01%, LNST/TTS 7,33%;
- phần phân tích trang 28 chia cho số **bình quân**: ROE 30,50%, ROA 7,84%.

Java đang theo quy ước bình quân. Đề xuất trả cả hai giá trị (xem mục 4).

## 4. Để sau, làm nếu kịp (người làm đề tài quyết định 12/10/2026: chưa thêm tool)

| # | Việc | Lý do |
|---|---|---|
| 1 | `sum_figures`: cộng/trừ, kèm bảng chặn cộng trùng chỉ tiêu cha + con (~25 dòng tổng chính theo thông tư) | hiện hỏi "tổng…" thì chỉ liệt kê số, không cộng |
| 2 | CAGR trong `relate_two_figures` | "tăng trưởng bình quân/năm" |
| 3 | Tỷ số trả cả hai quy ước (bình quân và cuối kỳ) | khớp cả hai chỗ trong BCTN của CTR |
| 4 | Thêm 6 tỷ số CTR công bố: thanh toán ngắn hạn, thanh toán nhanh, VCSH/TTS, vòng quay HTK, phải thu, phải trả | người dùng sẽ hỏi |
| 5 | Test so kết quả Java với số CTR công bố (BCTN 2024, 2025) | bằng chứng cho phần trình bày |
| 6 | Từ điển viết tắt thiếu: KD, HĐKD/HĐĐT/HĐTC, CTM, CĐKKS | "dòng tiền KD", "CTM" |
| 7 | Prompt v1.4: "tiền mặt" = mã 110 (ca D11) | 1 câu sai trong lượt chạy 8 câu |

**Giới hạn phải nói thẳng** (kể cả khi đã làm #1): bảng chặn cộng trùng chỉ phủ các dòng tổng chính. Cặp dòng chi tiết có liên quan theo nghiệp vụ nhưng không nằm trong cấu trúc tổng thì không chặn được. Mỗi số hạng hiện kèm mã in trên báo cáo để người dùng tự soát.
