# Đánh giá — v1.9: câu trả lời nhánh tài liệu và trục model nhánh số liệu

*Ngày: 2026-10-13 · Kế thừa [danh-gia_v1.8.md](danh-gia_v1.8.md) (bảng A, B, D giữ nguyên; thêm bảng F, cập nhật bảng E)*

## 1. Bảng F: câu trả lời nhánh tài liệu (Sonnet 5.5)

**Thiết lập**
- Bộ câu: `data/eval/eval_test.yml`, 26 câu `rag_*` cùng X01, X02, X05 (từ chối).
- Model: `anthropic/claude-sonnet-5-5`, giá 2/10 USD cho 1 triệu token (QĐ-035).
- Chấm: `AnswerJudge` (LLM chấm đúng / một phần / sai so với đáp án vàng), **người làm đề tài duyệt lại** các câu sai, một phần và mẫu câu đúng.
- Kết quả: `data/eval/ket-qua/danh-gia-rag_20261013-0847.csv/.md`; test `RagAnswerEvaluationTest -Dragevel=true`.

**Hai cấu hình**
- **FINQUERY**: agent, RAG phân cấp (chuẩn hóa → Qdrant 80 → lọc tiếng Anh → rerank 40 → 20), có cả công cụ số liệu.
- **RAG_CO_BAN**: nguyên câu hỏi → vector top 20 → cùng bước trả lời.

| Cấu hình | Đúng | Một phần | Sai | Điểm (đúng 1, một phần 0,5) | Từ chối đúng | Trích đúng trang vàng | Câu trích ✓ | Câu có số không nguồn | Thời gian TB ± ĐLC | p50 | p95 | Token vào / ra TB | USD/câu |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| **FINQUERY** | **24/26** | 1 | 1 | **24,5/26 (94,2%)** | 3/3 | **24/26** | 139/139 | 1 | 28,1 ± 13,1 s | 27,8 s | 48,1 s | 50.043 / 1.423 | 0,1159 |
| RAG_CO_BAN | 20/26 | 3 | 3 | 21,5/26 (82,7%) | 3/3 | 19/26 | 125/125 | 19 | 19,2 ± 5,3 s | 18,4 s | 29,2 s | 26.552 / 1.105 | 0,0642 |

**Theo nhóm câu** (đúng / một phần / sai):

| Nhóm | FINQUERY | RAG_CO_BAN |
|---|---|---|
| rag_van_ban (11) | 11 / 0 / 0 | 10 / 1 / 0 |
| rag_bang (10) | 9 / 0 / 1 | 8 / 0 / 2 |
| rag_nhieu_trang (5) | 4 / 1 / 0 | 2 / 2 / 1 |

**Nhận xét**
- FinQuery hơn rõ nhất ở **câu nhiều trang**:
  - M03 là một ví dụ: RAG cơ bản không tìm được LNST quý 2/2024.
  - FinQuery lấy được quý 3/2025 từ DB (D#) và quý 2/2024 từ tài liệu (C#).
- Cả hai cấu hình đều có **0 câu trích sai** (CitationVerifier). Tuy vậy, RAG cơ bản có 19 câu chứa số không có nguồn, FinQuery chỉ có 1 (M02).
- FinQuery chậm hơn khoảng 9 s và đắt hơn khoảng 1,8 lần, vì agent cần trung bình 1,93 lượt gọi model trước bước trả lời và đưa vào nhiều ngữ cảnh hơn.
- Chi phí cả lượt chạy:
  - FINQUERY 3,36 USD;
  - RAG_CO_BAN 1,86 USD;
  - chấm LLM khoảng 0,15 USD.

**Cần người duyệt** (chi tiết ở mục 4 file `.md` kết quả)

| Câu | Cấu hình | Chấm | Ghi chú |
|---|---|---|---|
| B07 | cả hai | sai | Cả hai đưa vốn điều lệ **theo phương án** (1.281.121.844.800) và nói chưa có số thực tế. Đáp án vàng là 1.281.095.330.000 (13.723.654 cổ phiếu thực phát hành), cần kiểm tra bộ tài liệu có trang nào nêu số này không. Nếu không có thì đây là **lỗi đáp án vàng**, không phải lỗi hệ thống. |
| M03 | FINQUERY | một phần | Đúng cả hai số nhưng không nêu chênh lệch 43,8 tỷ: không có kết quả Java tính sẵn giữa C# và D#, nên model không tự trừ. |
| M03 | RAG_CO_BAN | sai | Không tìm thấy LNST quý 2/2024. |
| B05 | RAG_CO_BAN | sai | Không tìm thấy vốn hóa 31/12/2025. |
| V03, M04, M05 | RAG_CO_BAN | một phần | Thiếu ý (nguyên nhân tăng LNST; mốc cuối 2024; lần tăng vốn 2026). |
| B08, V11, B02, V10, M02 | mẫu đúng | đúng | Năm câu đúng chọn ngẫu nhiên để soát chất lượng chấm. |

## 2. Bảng E (trục model) cập nhật: nhánh số liệu với Qwen

Cùng bộ `eval_dbquery.yml` 21 câu, cấu hình FinQuery, prompt v1.3. Kết quả ở `danh-gia-db_20261013-0814_qwen.*`.

| Model | Đúng | Thời gian TB ± ĐLC | p50 | USD/câu |
|---|---|---|---|---|
| **Claude Sonnet 5.5** | **21/21** | **8,2 ± 4,4 s** | 7,1 s | 0,0505 |
| Qwen (DeepInfra, có thinking) | 20/21 (D11 chọn sai chỉ tiêu "tiền mặt") | 153,1 ± 75,1 s | 163,1 s | 0,0162 |

- Sonnet nhanh hơn khoảng **19 lần** (theo thời gian TB) và đắt hơn khoảng 3 lần.
- Cùng công cụ và cùng SQL cố định, độ chính xác gần như nhau. Khác biệt chính là độ trễ, đúng như lý do chọn Sonnet ở QĐ-035.

## 3. Bài học vận hành

- **Không biên dịch lại backend khi một test benchmark đang chạy.** JVM đang chạy đọc class mới bị thiếu và gây `NoClassDefFoundError` giữa lượt (FINQUERY B04), nên phải xóa dòng hỏng rồi chạy tiếp bằng `-Dragevel.resume`.
- **Hết số dư DeepInfra (HTTP 402)** cũng làm hỏng dòng. Khi lọc dòng hỏng, phải lọc theo cột `trang_thai`/`ly_do` chứ không tìm chuỗi "402" trong cả dòng, vì chuỗi này xuất hiện trong số liệu. Lần đầu lọc sai đã xóa nhầm dòng đúng, và phải dựng lại từ bản sao lưu.
- **Qdrant cần RAM.** Sau khi khởi động lại máy, phải bật Docker và `finq-qdrant` trước. Khi máy còn dưới 0,6 GB RAM trống, Qdrant báo `DEADLINE_EXCEEDED`.

## 4. Việc còn lại của đánh giá
- Người làm đề tài duyệt mục 1 (đặc biệt B07: xác minh đáp án vàng).
- Có thể thêm một bước Java tính chênh lệch giữa C# và D# (sửa M03); đã ghi vào hàng chờ cùng các công cụ DB hoãn lại.
- Chạy lại benchmark truy hồi với cấu hình 80 không oversample, và A/B độ trễ của lọc tiếng Anh.
