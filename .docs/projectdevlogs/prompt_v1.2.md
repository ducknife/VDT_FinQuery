# Lời nhắc hệ thống (system prompt) — v1.2

*Ngày: 2026-10-12 · Kế thừa [prompt_v1.1.md](prompt_v1.1.md) · Mã: `SystemPrompt.java` (`VERSION = "v1.2"`), `SearchDocuments.java`*

## 1. Trường hợp gây ra

- **Câu hỏi:** V02 "Nguyên nhân lãi ròng hợp nhất quý 3/2025 tăng so với cùng kỳ là gì?". Chạy 2 lần với v1.1: `agent-thu_20261012-195831.md` và `agent-thu_20261012-200117.md`.
- **Hiện tượng:** cả hai lần agent gọi `search_documents` 3 lần, dù lượt 1 đã có trang vàng (Công văn giải trình 25/10/2025, trang 2):
  - lượt 2 viết lại câu hỏi;
  - lượt 3 tìm đích danh "Công văn giải trình 2025 ngày 25/10/2025 trang 2-3", tức tài liệu đã có.
- **Hậu quả:** thêm 2 lần gọi LLM và 2 lần truy hồi. Bước trả lời nhận nhiều chunk hơn: 45 nghìn token so với 36 nghìn. Tổng thời gian 115–173 s, so với 90 s khi chỉ tìm 1 lần.
- **Nguyên nhân:** tool chỉ cho agent xem 350 ký tự đầu mỗi trang, mà đó thường là tiêu đề công văn (tên công ty, số công văn). Agent không thấy đoạn "nguyên nhân" nên tưởng chưa có bằng chứng.

## 2. Thay đổi

**`AGENT_SYSTEM_PROMPT`.** Bỏ dòng "Prefer few, targeted calls…", thay bằng mục mới:

```
ONE SEARCH IS NORMALLY ENOUGH
- search_documents already runs the full retrieval pipeline (filters by company and period, ranks
  with two rerankers) and returns the 20 best passages. The answer step reads the FULL TEXT of
  every passage returned. You only see a short preview of each page, which often shows just the
  letterhead or the title, not the part that answers the question.
- So do NOT search again because a preview does not show the answer. If the listed sources include
  a document that fits the question (e.g. the explanation letter for that quarter, the resolution
  for that year), stop: the evidence is there.
- Search again ONLY when (a) the question has a separate part that the first query did not cover
  (e.g. a figure AND next year's plan, which live in different documents), or (b) the listed
  sources are clearly the wrong company, period or topic.
- Never search again for a document or page that is already in the results, and never just
  rephrase the same query.
```

**Câu trả về của `search_documents`.** Câu dẫn trước danh sách xem trước nói rõ:
- bước trả lời đọc toàn văn;
- phần xem trước chỉ là đầu trang;
- xem trước không thấy đáp án không có nghĩa là thiếu đáp án.

Thông tin về toàn văn được nói ở hai nơi, prompt và kết quả tool, vì model đọc kết quả tool ngay lúc quyết định có tìm tiếp hay không.

## 3. Kết quả đo lại

- Chạy 3 câu, Qdrant 80 chunk, có lọc tiếng Anh. Log: `data/eval/ket-qua/agent-thu_20261012-202126.md`, `-202413.md`, `-202531.md`.
- Câu kế hoạch LNST năm nay là câu dùng năm tương đối.

| Câu | Lần tìm (v1.1 → v1.2) | Lần gọi LLM ở vòng agent | Trạng thái | Câu trích ✓ |
|---|---|---|---|---|
| V02 | 3 → **1** | 2 | answered | 1/1 |
| Cổ tức 2024 | → **1** | 2 | answered | 1/2; câu ✗ do ký hiệu markdown, đã sửa bộ kiểm (truy-hoi_v1.8 mục 1) |
| Kế hoạch LNST năm nay | → **1** | 2 | answered | 4/4 |

- **Mục tiêu đạt:** agent không còn tìm thừa.
- **Tổng thời gian vẫn cao** (78–167 s) vì Qwen trên DeepInfra sinh chữ chậm (16–22 token/s) và thinking dao động mạnh. Đo chi tiết ở [truy-hoi_v1.8.md](truy-hoi_v1.8.md) mục 3.

## 4. Nhật ký thay đổi

| Phiên bản | Ngày | Trường hợp gây ra | Thay đổi |
|---|---|---|---|
| v1.0 | 12/10/2026 | bản đầu | xem [prompt_v1.0.md](prompt_v1.0.md) |
| v1.1 | 12/10/2026 | V02: câu trích ghép qua đoạn tiếng Anh | câu trích ngắn, liền mạch; xem [prompt_v1.1.md](prompt_v1.1.md) |
| v1.2 | 12/10/2026 | V02: agent tìm 3 lần dù lượt 1 đã có trang vàng | mục "ONE SEARCH IS NORMALLY ENOUGH", câu trả về của tool nói rõ bước trả lời đọc toàn văn |
