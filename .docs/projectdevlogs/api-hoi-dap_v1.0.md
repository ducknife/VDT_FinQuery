# API hỏi đáp và xem trích dẫn — v1.0

*Ngày: 2026-10-13 · Thiết kế "cách C" đã chốt: stream tiến trình + stream chữ câu trả lời + kết quả kiểm · Prompt: [prompt_v1.4.md](prompt_v1.4.md)*

## 1. Endpoint

| Endpoint | Trả về | Dùng cho |
|---|---|---|
| `POST /api/v1/ask` body `{"question": "..."}` | `text/event-stream` (SSE) | giao diện chat |
| `POST /api/v1/ask/sync` | JSON `AskResult` một lần | test, công cụ |
| `GET /api/v1/documents/{taiLieuId}/pages/{trang}/image` | `image/jpeg` (PDF gốc, 110 dpi) | bấm trích dẫn: xem ảnh trang |
| `GET /api/v1/documents/{taiLieuId}/pages/{trang}/text` | `text/markdown` (chữ OCR) | bấm trích dẫn: xem chữ, bôi câu trích |

**Kiểm đầu vào:**
- câu hỏi rỗng hoặc dài quá 1.000 ký tự: 400;
- trang ngoài `1..tong_so_trang`: 404;
- tài liệu không có PDF gốc: 404 với ảnh, chữ vẫn xem được.

**`trang`** là số thứ tự trang **trong file PDF** (trang bìa là trang 1), có thể lệch số in ở chân trang. Giao diện nên ghi "trang N của file".

## 2. Sự kiện SSE (theo thứ tự)

| `event` | `data` | Phát từ |
|---|---|---|
| `step` | `{phase: understand, normalized}` · `{phase: agent_round, round}` · `{phase: answer, evidence}` | `QueryLoop` |
| `plan` | `{text}`: câu kế hoạch của agent trước khi gọi tool, hoặc ghi chú khi dừng (prompt v1.4: không chứa số) | `QueryLoop` |
| `tool` | `{name, text}`: tool được gọi, diễn đạt tiếng Việt (`ToolCallText`) | `QueryLoop` |
| `retrieval` | `{step: preprocess, query, filter}` · `{step: search, filterMode, found, englishDropped, kept, ms}` · `{step: rerank_start, candidates}` · `{step: rerank, tiers, kept, ms}` | `RetrievalService` qua `SearchDocuments` |
| `sources` | `{items: [{id, taiLieuId, title, page, score}]}` (5 đoạn đầu) | `SearchDocuments` |
| `tool_result` | `{text, ms}`: tóm tắt kết quả từng tool | `QueryState.record` |
| `answer_delta` | `{text}`: mẩu chữ mới của câu trả lời | `AnswerGenerator` (stream) |
| `verification` | `{status, citations[{id, taiLieuId, tenHienThi, trang, quote, verified}], unverifiedNumbers, unknownIds, missing}` | `QueryLoop` sau `finish` |
| `done` | `{result: AskResult}` (thời gian, token, chi phí, phiên bản prompt, model) | `AskController` |
| `error` | `{message}` | `AskController` |

**Kỷ luật bằng chứng giữ nguyên:**
- chữ câu trả lời chạy trước;
- `verification` đến sau, cho biết từng trích dẫn có thật trong nguồn không và số nào không có nguồn.

Giao diện hiện nhãn "đang kiểm chứng" cho tới khi nhận `verification`.

## 3. Mã nguồn

- `modules/agent/AskController`: SSE bằng `SseEmitter` (180 s), mỗi câu hỏi chạy trên một luồng ảo (Java 21).
- `modules/agent/events/AgentEvent`, `ToolCallText`.
- `QueryState`: thêm kênh sự kiện `events` và `emit()`. Lỗi gửi (người dùng đóng trang) không làm hỏng câu trả lời.
- `RetrievalService.retrieve(question, options, progress)`: bản có báo tiến trình; bản cũ giữ nguyên.
- `AnswerGenerator.generate(..., onDelta)`: stream bằng `chatModel.stream` kèm `streamUsage` để vẫn có token.
  - `AnswerFieldStreamer` đọc dần trường `answer` trong JSON đang sinh (giải mã `\n`, `\"`, `\uXXXX`).
  - Nhánh chia 2 nửa (vượt 64k) không stream.
- `modules/document/DocumentPageService`: ảnh trang (`PdfRenderer`) và chữ OCR (`PageStore`).

## 4. Chạy thử thật (13/10, 08:23–08:26, Sonnet 5.5, prompt v1.4, câu D11 "Tiền mặt hợp nhất cuối quý trước là bao nhiêu?")

**Lần 1:** sự kiện đúng tới `answer_delta`, rồi lỗi "bước trả lời không trả JSON".
- Nguyên nhân: bug trong `AnswerFieldStreamer.feed`. Sau khi đọc xong trường `answer`, các mẩu sau (`citations`, dấu `}`) bị bỏ, không ghi vào JSON đầy đủ.
- Đã sửa và thêm test `giuDuJsonSauKhiHetAnswer`.

**Lần 2: chạy trọn.**

| Thời điểm | Sự kiện |
|---|---|
| 0,2 s | `step understand`: "quý trước (quý 1 năm 2026)" |
| 3,9 s | `plan`: "Tôi sẽ tra tiền và các khoản tương đương tiền hợp nhất tại 31/03/2026 (cuối quý 1/2026)." |
| 3,9 s | `tool`: "Tra số liệu: [TIEN_VA_TUONG_DUONG_TIEN] · Q1/2026 · [CONSOLIDATED]" |
| 5,1 s | `plan` (ghi chú khi dừng, không có số) |
| **6,6 s** | **chữ đầu tiên của câu trả lời** |
| 8,9 s | hết chữ (35 mẩu) |
| 9,0 s | `verification`: D1 ✓, không số nào thiếu nguồn |
| 9,0 s | `done`: tổng 8,6 s, 0,055 USD |

- Câu trả lời: *"…tại 31/03/2026 (cuối quý 1/2026) là 1.123.253.891.417 đồng, tức khoảng 1.123,25 tỷ đồng [D1]…"*. **Đúng đáp án vàng**: quy tắc "tiền mặt" = mã 110 của prompt v1.4 có tác dụng.
- Xem trích dẫn D1 (`CTR-BCTC-HN-20260331-Q1-e1bb93`, trang 3):
  - ảnh: HTTP 200, 160 KB, 6,0 s (lần đầu mở PDF 68 trang);
  - chữ: HTTP 200, có dòng `110 | Tiền và các khoản tương đương tiền | 1.123.253.891.417`;
  - trang 999: 404.

## 5. Còn lại

- Ảnh trang lần đầu mất khoảng 6 s (mở cả file PDF). Nếu giao diện thấy chậm thì lưu đệm ảnh đã dựng.
- Bảng hội thoại, compaction, feedback: làm sau Keycloak (QĐ-038).
