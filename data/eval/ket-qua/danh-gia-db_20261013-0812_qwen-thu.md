# Đánh giá câu trả lời nhánh số liệu — 13/10/2026 08:14

Bộ câu hỏi: `data/eval/eval_dbquery.yml` (1 câu). Model: `Qwen/Qwen3.5-397B-A17B`, cùng cho mọi cấu hình; chấm máy "đúng và đủ số được hỏi" (`DbAnswerScorer`), **cần người soát lại**. Chi tiết: `danh-gia-db_20261013-0812_qwen-thu.csv`.

- FINQUERY: 3 tool SQL mẫu, LLM chỉ điền tham số
- TEXT_TO_SQL: LLM tự viết SQL (tool execute_sql, user chỉ đọc)
- LLM_ONLY: không công cụ, trả lời từ trí nhớ

## 1. Tổng hợp

| Cấu hình | Đúng | Một số | So sánh | Từ chối | Thời gian TB ± ĐLC (s) | p50 (s) | USD/câu | Token vào TB | Lỗi SQL | Số không có nguồn |
|---|---|---|---|---|---|---|---|---|---|---|
| FINQUERY | **1/1** | 1/1 |  |  | 96.0 ± 0.0 | 96.0 | 0.0142 | 14345 | 0 | 0 |

## 2. Từng câu

| Câu | Nhóm | FINQUERY |
|---|---|---|
| D01 | db_mot_so | ✓ |

## 3. Câu trả lời để soát

### D01 — Doanh thu thuần hợp nhất năm 2024 của CTR là bao nhiêu?

**Đáp án vàng:** 12.609.519.560.081 đồng

- **FINQUERY** ✓ (có số đồng chính xác, 96.0 s): Doanh thu thuần hợp nhất năm 2024 của CTR là 12.609.519.560.081 đồng (≈ 12.609,52 tỷ đồng) [D1].
  - công cụ: get_financial_figures([DOANH_THU_THUAN], 1 kỳ, [CONSOLIDATED]) -> 1 số
