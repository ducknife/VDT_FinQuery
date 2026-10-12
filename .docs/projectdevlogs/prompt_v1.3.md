# Lời nhắc hệ thống (system prompt) — v1.3

*Ngày: 2026-10-12 · Kế thừa [prompt_v1.2.md](prompt_v1.2.md) · Mã: `SystemPrompt.java` (`VERSION = "v1.3"`), `IndicatorCatalog.promptSection()`, `QueryLoop`*

## 1. Lý do

Đây không phải sửa vì một ca sai, mà vì **thêm nhánh số liệu**: 3 tool SQL mẫu, xem [nhanh-so-lieu_v1.0.md](nhanh-so-lieu_v1.0.md). Prompt v1.0 chỉ mô tả trước một tool `get_financial_figures` chưa tồn tại.

## 2. Thay đổi

**`AGENT_SYSTEM_PROMPT`, mục TOOLS:**
- Mô tả 3 tool số liệu: `get_financial_figures`, `relate_two_figures`, `compute_financial_ratio`. Nói rõ LLM chỉ điền tham số, hệ thống tự tính.
- Cách điền tham số:
  - `indicator` lấy từ danh sách mã, chọn theo nghĩa, **cấm thay chỉ tiêu gần giống**;
  - `period` Q1..Q4 / H1 / M9 / FY, kèm cách hiểu "cuối năm", "6 tháng đầu năm", "riêng quý 2", "luỹ kế 9 tháng";
  - `scope` mặc định hợp nhất;
  - kỳ tương đối tính theo kỳ mới nhất gửi kèm câu hỏi; kỳ sau đó là chưa công bố, không ngoại suy;
  - số không phải dòng chuẩn trên báo cáo (trạm BTS, nhân sự, cổ tức, kế hoạch) thì dùng `search_documents`.

**`AGENT_SYSTEM_PROMPT`, mục HOW TO COLLECT EVIDENCE:**
- `NOT_FOUND` thì tìm thêm trong tài liệu; `NOT_AVAILABLE` (kỳ chưa công bố) là kết luận cuối.
- Ưu tiên **một** lần gọi tool số liệu cho tất cả những gì cần hỏi.

**Danh sách 123 mã chỉ tiêu** (mã | tên | P/F | mã thay thế theo TT99), sinh từ `financial_items` và nối vào system prompt **một lần** (QĐ-037), không lặp trong schema từng tool.

**Câu hỏi gửi agent** thêm dòng `Latest period in the figures database: 30/06/2026`.

**`ANSWER_SYSTEM_PROMPT`, mô tả bằng chứng [D#]:**
- có giá trị bằng đồng và tỷ đồng, kèm nguồn và trang;
- có loại D# **do hệ thống tính**: dùng nó thì trích cả nó lẫn các D# đầu vào;
- chép số **đúng như viết trong bằng chứng**, không tự làm tròn hay tính lại;
- số trình bày lại thì nêu cả số công bố ban đầu khi cần.

## 3. Kết quả

8 câu số liệu với Sonnet 5.5: đúng 7/8, 18/18 câu trích ✓, chọn đúng tool ở cả 8 câu, 6,8–11,6 s/câu. Chi tiết và ca D11 ở [nhanh-so-lieu_v1.0.md](nhanh-so-lieu_v1.0.md) mục 5.

## 4. Nhật ký thay đổi

| Phiên bản | Ngày | Trường hợp gây ra | Thay đổi |
|---|---|---|---|
| v1.0 | 12/10/2026 | bản đầu | xem [prompt_v1.0.md](prompt_v1.0.md) |
| v1.1 | 12/10/2026 | V02: câu trích ghép qua đoạn tiếng Anh | câu trích ngắn, liền mạch |
| v1.2 | 12/10/2026 | V02: agent tìm 3 lần | "ONE SEARCH IS NORMALLY ENOUGH" |
| v1.3 | 12/10/2026 | thêm nhánh số liệu (3 tool SQL mẫu) | mô tả tool, cách điền tham số, danh sách 123 mã, kỳ mới nhất, quy tắc trích D# do hệ thống tính |
