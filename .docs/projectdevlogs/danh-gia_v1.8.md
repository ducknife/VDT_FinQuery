# Đánh giá — v1.8: bộ chấm v2 và chấm lại nhánh số liệu

*Ngày: 2026-10-13 · Kế thừa [danh-gia_v1.7.md](danh-gia_v1.7.md) (các bảng A, C, D, E giữ nguyên; bảng B cập nhật dòng "chính xác") · QĐ-039 ([quyet-dinh_v1.22.md](quyet-dinh_v1.22.md))*

## 1. Thay đổi

**Tiêu chí câu từ chối** (người làm đề tài chốt, QĐ-039):
- không khẳng định số cho kỳ chưa có;
- được đưa số kỳ gần nhất nếu có nguồn và ghi rõ kỳ.

**Bộ chấm v2** (`DbAnswerScorer`):

| Kiểu | v1 | v2 |
|---|---|---|
| Từ chối, cấu hình có công cụ | chỉ được nhắc số trong danh sách cố định | đúng khi trạng thái khác `answered` **và** mọi số đều có nguồn (`so_khong_nguon = 0`) |
| Từ chối, LLM không công cụ | không được đưa số nào ngoài danh sách | giữ nguyên |
| Phần trăm, số lần | phải đúng chuỗi "84,59" | chấp nhận số chưa làm tròn nếu làm tròn 2 chữ số khớp (84,58694% → 84,59%) |
| Số tiền | số đồng chính xác hoặc số tỷ làm tròn 2 chữ số | giữ nguyên: **viết sai số vẫn tính sai** |

**Chế độ chấm lại không gọi API:** `DbAnswerEvaluationTest -Ddbeval=true -Ddbeval.rescore=<csv>` ghi ra `<csv>_cham-lai.csv/.md`; bản gốc giữ nguyên.

**`NOT_AVAILABLE` thành bằng chứng D#** (`FigureLookup.notAvailable`):
- Kỳ chưa công bố hoặc trước 2017 thì sinh một D# dạng ghi chú, ví dụ *"Doanh thu thuần…, hợp nhất, năm 2026: chưa công bố; kỳ mới nhất trong kho là 30/06/2026."*
- Nguồn hiển thị: "hệ thống số liệu FinQuery".
- `FinancialToolsGoldTest` kiểm X03, X04, X06 đều sinh `[D1] NOT_AVAILABLE`.

## 2. Bảng B (câu trả lời nhánh số liệu) sau khi chấm lại

Nguồn: `data/eval/ket-qua/danh-gia-db_20261012-2308_cham-lai.md` (cùng câu trả lời của lượt 12/10, 23:08; chỉ chấm lại).

| Cấu hình | Đúng v1 | **Đúng v2** | Một số | So sánh | Từ chối | Còn sai |
|---|---|---|---|---|---|---|
| **FinQuery** | 21/21 | **21/21** | 11/11 | 7/7 | 3/3 | — |
| Text-to-SQL | 14/21 | **19/21** | 9/11 | 7/7 | 3/3 | D02 viết sai số ("587.417.356 381"), D11 chọn sai chỉ tiêu ("tiền mặt" → mã 111) |
| LLM không công cụ | 3/21 | **3/21** | 0/11 | 0/7 | 3/3 | 18 câu có số: không có dữ liệu |

Các cột khác của bảng B (trích dẫn, độ trễ, token, chi phí) **không đổi** so với danh-gia_v1.7.

**Kết quả v2 khớp với phần soát tay** trong danh-gia_v1.6 (text-to-SQL khoảng 18–19/21).

## 3. Trial X06 sau khi có runner (13/10, 07:50; trước khi sửa NOT_AVAILABLE)

`AnswerTrialRunner` → `backend/core-service/trial-answer.txt`.
- **FinQuery:** hỏi 2 kỳ; trả lời "chưa có số cả năm 2026… 6 tháng đầu năm 7.958,84 tỷ… chỉ là số nửa năm, không phải cả năm" (`partial`, trích [D1] đúng).
- **Text-to-SQL:** tương tự, kèm 2 quý. Câu trích là **dòng thô của bảng SQL** (`2026-01-01 | 2026-06-30 | hop_nhat | 7.958…`), không có tên tài liệu và trang dễ đọc.
- **LLM không công cụ:** từ chối.

Hành vi của FinQuery dao động giữa các lượt (23:08 chỉ nói "không tìm thấy"). Đây là lý do cho bản sửa `NOT_AVAILABLE` → D#. **Chưa chạy lại** sau khi sửa.
