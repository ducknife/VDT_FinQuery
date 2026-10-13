# Lời nhắc hệ thống (system prompt) — v1.4

*Ngày: 2026-10-13 · Kế thừa [prompt_v1.3.md](prompt_v1.3.md) · Mã: `SystemPrompt.java` (`VERSION = "v1.4"`)*

## 1. Trường hợp gây ra

1. **Câu D11** *"Tiền mặt hợp nhất cuối quý trước là bao nhiêu?"*:
   - lượt 12/10, 21:43 (FinQuery) và lượt 23:08 (text-to-SQL) chọn mã 111 "Tiền" (623,25 tỷ);
   - đáp án vàng là mã 110 "Tiền và các khoản tương đương tiền" (1.123,25 tỷ);
   - người làm kế toán nói "tiền mặt" thường là ý mã 110.
2. **Hiển thị tiến trình** (API `/ask` stream cách C):
   - lúc gọi tool, agent không viết gì;
   - lúc dừng, có lần agent ghi cả **con số** vào ghi chú ("kế hoạch… 622,4 tỷ đồng"), tức số chưa qua bộ kiểm.

## 2. Thay đổi (`AGENT_SYSTEM_PROMPT`)

Thêm vào mục YOUR JOB IN THIS PHASE:

```
Your notes are shown to the user as progress, so:
- Before calling tools, write ONE short sentence in Vietnamese saying what you will look up and why,
  e.g. "Tôi sẽ tra doanh thu thuần hợp nhất năm 2024 và 2025 để tính mức tăng."
- When you stop, write ONE short sentence in Vietnamese saying which evidence you have (document,
  period), e.g. "Đã có công văn giải trình kết quả quý III/2025."
- NEVER put figures, results or conclusions in these notes; only the answer step states figures.
```

Thêm vào mục Filling parameters, dòng indicator:

```
"Tiền mặt" in Vietnamese finance usage means cash and cash equivalents: use TIEN_VA_TUONG_DUONG_TIEN
(code 110); use TIEN_M111 (cash only, code 111) only when the user explicitly excludes equivalents.
```

## 3. Kết quả đo lại

Một lần chạy D11 qua `/ask` (13/10, 08:26, Sonnet 5.5; chi tiết ở [api-hoi-dap_v1.0.md](api-hoi-dap_v1.0.md) mục 4):
- **Chọn đúng mã 110**, trả lời 1.123,25 tỷ (đúng đáp án vàng);
- câu kế hoạch trước khi gọi tool: *"Tôi sẽ tra tiền và các khoản tương đương tiền hợp nhất tại 31/03/2026 (cuối quý 1/2026)."*;
- ghi chú khi dừng không chứa số.

**Chưa** chạy lại cả bộ 21 câu với v1.4. Lượt Qwen sáng 13/10 bắt đầu trước khi đổi prompt nên vẫn là v1.3, so được với lượt Sonnet v1.3.

## 4. Nhật ký thay đổi

| Phiên bản | Ngày | Trường hợp gây ra | Thay đổi |
|---|---|---|---|
| v1.0 | 12/10/2026 | bản đầu | xem [prompt_v1.0.md](prompt_v1.0.md) |
| v1.1 | 12/10/2026 | câu trích ghép qua đoạn tiếng Anh | câu trích ngắn, liền mạch |
| v1.2 | 12/10/2026 | agent tìm 3 lần | "ONE SEARCH IS NORMALLY ENOUGH" |
| v1.3 | 12/10/2026 | thêm nhánh số liệu | mô tả 3 tool SQL, cách điền tham số, danh sách mã |
| v1.4 | 13/10/2026 | D11 "tiền mặt" chọn sai mã; cần ghi chú hiển thị cho người dùng | "tiền mặt" = mã 110; một câu kế hoạch trước khi gọi tool; ghi chú không chứa số |
