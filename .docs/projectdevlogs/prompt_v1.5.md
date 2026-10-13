# Lời nhắc hệ thống (system prompt) — v1.5

*Ngày: 2026-10-13 · Kế thừa [prompt_v1.4.md](prompt_v1.4.md) · Mã: `SystemPrompt.java` (`VERSION = "v1.5"`) · QĐ-041 ([quyet-dinh_v1.24.md](quyet-dinh_v1.24.md))*

## 1. Trường hợp gây ra

**Câu M03** *"So sánh LNST hợp nhất quý 2/2024 với quý 3/2025"*, bị chấm "một phần":
- quý 3/2025 lấy được từ DB (D#), còn quý 2/2024 chỉ có trong tài liệu (C#), vì DB chưa trích LNST quý 1 và quý 2 ([danh-gia_v1.10.md](danh-gia_v1.10.md) mục 5);
- `relate_two_figures` chỉ nhận số DB nên không tính được;
- prompt v1.4 cấm model tự tính, nên câu trả lời không nêu được mức chênh lệch.

Agent chỉ thấy 350 ký tự đầu của mỗi trang, nên không điền được số lấy từ tài liệu vào một tool ở vòng agent. Vì vậy phép tính đặt ở **bước trả lời**, nơi model đọc toàn văn bằng chứng.

## 2. Thay đổi (`ANSWER_SYSTEM_PROMPT`)

- Bỏ câu: *"If a growth rate, difference or ratio is needed and it is not in the evidence, say it was not computed instead of calculating."*
- Thêm mục **CALCULATIONS**:
  - cần chênh lệch, tổng, % thay đổi hay tỷ lệ giữa **hai số có trong bằng chứng** (C# hoặc D#, có thể mỗi loại một số) mà chưa có sẵn thì khai báo trong `calculations` và viết chỗ đặt `{{K1}}` trong câu trả lời;
  - mỗi đầu vào gồm mã bằng chứng, số **chép nguyên văn** (không kèm đơn vị) và đơn vị (`đồng`, `nghìn đồng`, `triệu đồng`, `tỷ đồng`, `%`, hoặc vật được đếm);
  - `op`: `difference` (a − b), `sum` (a + b), `percent_change` ((a − b) / b), `ratio` (a / b); thay đổi theo thời gian thì a là số sau, b là số trước;
  - trích các mã đầu vào cạnh chỗ đặt;
  - không bao giờ tự viết kết quả.
- Thêm trường `calculations` vào JSON đầu ra (danh sách rỗng khi không cần tính), kèm ví dụ.

## 3. Phía Java

| Thành phần | Việc |
|---|---|
| `EvidenceCalculator` (mới) | Kiểm số có nguyên văn trong bằng chứng được nêu (bỏ khoảng trắng khi so); đọc số kiểu Việt/kiểu Anh, số âm trong ngoặc; quy đồng/nghìn/triệu/tỷ; tính BigDecimal; điền `{{K#}}`; bỏ chữ "tăng/giảm" model viết thừa trước chỗ đặt |
| Kết quả | Tiền: số đồng chính xác kèm số tỷ (`VnNumber.money`); `%` − `%` → "điểm phần trăm"; `percent_change` → "tăng/giảm x,xx%"; `ratio` → "x,xx lần" |
| Không tính | Số không có trong bằng chứng, mã không tồn tại, khác đơn vị đếm, chia cho 0 → chỗ đặt thành "(không tính được: lý do)" |
| `AnswerGenerator.finish` | Chạy phép tính, điền câu trả lời; kết quả Java tính được coi là nguồn hợp lệ khi kiểm số không nguồn |
| `AnswerResult.calculations` | Danh sách phép tính (mã, biểu thức, kết quả, ok) |
| `QueryLoop` | Có phép tính thì phát sự kiện `answer_final` (bản đã điền) và thêm `calculations` vào sự kiện `verification` |
| Giao diện (`turn.ts`) | `answer_final` thay chữ đã stream (còn `{{K#}}`) bằng bản đã điền |

Test: `EvidenceCalculatorTest` (7 ca).

## 4. Kết quả

`RagAnswerEvaluationTest -Dragevel.ids=M03,B07 -Dragevel.configs=FINQUERY`, kết quả ở `data/eval/ket-qua/danh-gia-rag_*_m03-phep-tinh.*`.

| Câu | Trước | Sau |
|---|---|---|
| M03 | một phần (không nêu chênh lệch) | **đúng**: Java tính K1 = [D2] 168.940.075.230 − [C8] 125,108,143,249 = **43.831.931.981 đồng (≈ 43,83 tỷ)**, K2 = **tăng 35,04%** |
| B07 | đúng (sau QĐ-040) | vẫn đúng |

- Model dùng số quý 2/2024 **đã trình bày lại** (cột so sánh của BCTC quý 2/2025, C8) và nêu cả số công bố ban đầu 125.050.312.483 (C2). Đáp án vàng 43,8 tỷ dùng số ban đầu, nên hai kết quả lệch khoảng 0,06 tỷ; giám khảo LLM vẫn chấm đúng.
- Lượt này lộ lỗi "tăng tăng 35,04%" (model viết "tăng {{K2}}"); đã sửa trong `fill`.
- Còn 1 "số không nguồn": model viết lại số kiểu Anh của C8 thành kiểu Việt (125.108.143.249). Đây là hành vi có từ trước, không do phép tính gây ra.

## 5. Còn phải kiểm

Đổi prompt bước trả lời ảnh hưởng mọi câu (cả nhánh tài liệu lẫn nhánh số liệu). Cần chạy lại cấu hình FinQuery trên bộ câu tài liệu (khoảng 3,4 USD) và bộ DB (khoảng 1,1 USD) trước khi chốt số liệu báo cáo.
