# Lời nhắc hệ thống (system prompt) — v1.6: xưng "tôi", gọi người dùng là "đồng chí"

*Ngày: 2026-10-13 · Kế thừa [prompt_v1.5.md](prompt_v1.5.md) · Mã: `SystemPrompt.java` (`VERSION = "v1.6"`)*

## 1. Yêu cầu

Người làm đề tài muốn FinQuery xưng hô theo văn hoá Viettel: hệ thống xưng **"tôi"**, gọi người dùng là **"đồng chí"**.

## 2. Thay đổi

- `AGENT_SYSTEM_PROMPT`, mục ghi chú tiến trình (hiện trên giao diện): xưng "tôi", gọi "đồng chí", ví dụ *"Tôi sẽ tra … để trả lời đồng chí."*; không dùng "bạn/anh/chị/em".
- `ANSWER_SYSTEM_PROMPT`, mục STYLE: xưng "tôi", gọi "đồng chí" (ví dụ *"Thưa đồng chí, …"*); không dùng "bạn/anh/chị/em"; "đồng chí" tối đa 1–2 lần mỗi câu trả lời, thường ở câu mở đầu, không được thay cho câu trả lời trực tiếp.
- `QueryLoop`: câu từ chối ngoài phạm vi bắt đầu bằng "Thưa đồng chí, câu hỏi này nằm ngoài phạm vi FinQuery: …".

## 3. Chưa kiểm bằng LLM

Chưa chạy lại câu hỏi nào với prompt v1.6 (hết số dư API). Thay đổi chỉ ở cách xưng hô, không đổi luật bằng chứng hay phép tính; nên chạy thử vài câu khi có số dư để xem văn phong.
