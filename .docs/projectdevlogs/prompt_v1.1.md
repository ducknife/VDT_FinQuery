# Lời nhắc hệ thống (system prompt) — v1.1

*Ngày: 2026-10-12 · Kế thừa [prompt_v1.0.md](prompt_v1.0.md) · Mã: `SystemPrompt.java` (`VERSION = "v1.1"`)*

Phần cấu trúc, kỷ luật bằng chứng và quy tắc nghiệp vụ giữ nguyên như v1.0. Bản này chỉ ghi phần thay đổi.

## 1. Trường hợp gây ra

- **Câu hỏi:** V02 "Nguyên nhân lãi ròng hợp nhất quý 3/2025 tăng so với cùng kỳ là gì?"
- **Bản chạy:** `data/eval/ket-qua/agent-thu_20261012-175932.md`, prompt v1.0.
- **Kết quả:** câu trả lời đúng ý, đúng trang vàng (Công văn giải trình 25/10/2025, trang 2). Nhưng một câu trích bị đánh **✗ (không thấy nguyên văn trong nguồn)**.
- **Vì sao sai:** trang 2 là trang **song ngữ**, mỗi đoạn tiếng Việt có một đoạn tiếng Anh xen giữa. Model nối hai câu tiếng Việt thật, vốn cách nhau bởi một đoạn tiếng Anh, thành một câu trích. Từng câu đều có thật, nhưng chuỗi ghép thì không có liền mạch trong nguồn, nên bộ kiểm nguyên văn từ chối. Ý đúng, cách trích sai.

## 2. Thay đổi

Ba lớp, xử lý tận gốc chứ không chỉ sửa prompt:

| Lớp | Thay đổi |
|---|---|
| Dữ liệu đưa vào model | `EnglishFilter` (truy hồi, xem [truy-hoi_v1.8.md](truy-hoi_v1.8.md)): bỏ chunk thuần tiếng Anh, bỏ đoạn tiếng Anh trong chunk song ngữ. Reranker và LLM đọc bản sạch; câu trích được kiểm trên chính bản sạch đó. Bản gốc giữ nguyên để hiển thị trang. |
| Bộ kiểm câu trích (`CitationVerifier`) | Nếu cả câu trích không có liền mạch, tách theo câu (dấu `. ; : ! ?` có khoảng trắng phía sau, hoặc `...`). Mọi đoạn dài từ 15 ký tự trở lên phải có nguyên văn trong nguồn. Một đoạn bịa là trượt cả câu trích. Số `12.609.519` không bị tách vì không có khoảng trắng sau dấu chấm. |
| Prompt trả lời (`ANSWER_SYSTEM_PROMPT`) | Thêm quy tắc về câu trích, xem dưới. |

Đoạn thêm vào mục EVIDENCE DISCIPLINE, ngay sau quy tắc "QUOTE ... rejected":

```
- Keep each quote SHORT and CONTIGUOUS: one or two sentences (or one table row) taken from a
  single place in the item, just enough to support the claim. Do not join sentences from different
  paragraphs into one quote; if you need two places, cite the id twice with two quotes.
```

## 3. Kết quả đo lại (V02, cùng câu hỏi)

| Lần chạy | Prompt | Câu trích | Lượt chọn tool | Thời gian (agent / tool / trả lời / tổng) | Token vào / ra | Chi phí |
|---|---|---|---|---|---|---|
| 17:59 | v1.0, chưa lọc | 1 ✗ | 1 search | 23,7 / 15,6 / 51,1 / **90,5 s** | 36.499 / 2.671 | 0,027 USD |
| 19:58 | v1.1, có lọc | **2/2 ✓** | 3 search | 59,9 / 27,3 / 86,1 / 173,5 s | 44.696 / 3.034 | 0,033 USD |
| 20:01 | v1.1, có lọc | **2/2 ✓** | 3 search | 32,7 / 27,5 / 54,6 / 114,9 s | 45.205 / 2.335 | 0,031 USD |

- **Mục tiêu của bản này đạt:** câu trích từ ✗ thành ✓ ở cả hai lần chạy. Không có số nào lệch nguồn, không có mã trích dẫn lạ.
- **Tốc độ chưa đạt:** tổng thời gian tăng. Nguyên nhân không nằm ở bộ lọc mà ở **hành vi của agent**: ở cả hai lần, model tự tìm thêm 2 lượt nữa dù lượt đầu đã có trang vàng. Mỗi lượt thêm tốn khoảng 10–20 s gọi LLM cộng 4–5 s truy hồi, và đưa thêm chunk vào bước trả lời.
- Bước trả lời dao động mạnh giữa hai lần chạy (86 s và 55 s) dù cùng câu hỏi, cùng số token. Đó là độ trễ phía DeepInfra, không do mã nguồn.
- Thời điểm 19:58 và 20:01 lấy theo tên file `agent-thu_20261012-195831.md` và `agent-thu_20261012-200117.md`.

## 4. Nhật ký thay đổi

| Phiên bản | Ngày | Trường hợp gây ra | Thay đổi |
|---|---|---|---|
| v1.0 | 12/10/2026 | bản đầu | xem [prompt_v1.0.md](prompt_v1.0.md) |
| v1.1 | 12/10/2026 | V02: câu trích ghép hai câu tiếng Việt cách nhau một đoạn tiếng Anh, bị đánh ✗ | thêm quy tắc câu trích ngắn, liền mạch, một chỗ; kèm lọc tiếng Anh và kiểm câu trích theo từng câu |
