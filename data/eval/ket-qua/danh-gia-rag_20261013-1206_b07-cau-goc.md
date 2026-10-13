# Đánh giá câu trả lời nhánh tài liệu — 13/10/2026 12:06

Bộ câu: `data/eval/eval_test.yml`, 1 câu (rag_* + X01, X02, X05). Model: `anthropic/claude-sonnet-5-5`. Chấm đúng ý: `AnswerJudge` (LLM), **cần người duyệt**. Chi tiết: `danh-gia-rag_20261013-1206_b07-cau-goc.csv`.

- FINQUERY: agent + RAG phân cấp
- RAG_CO_BAN: nguyên câu hỏi -> vector top 20, cùng bước trả lời

## 1. Tổng hợp

| Cấu hình | Đúng | Một phần | Sai | Điểm (đúng 1, một phần 0,5) | Từ chối đúng | Trích đúng trang vàng | Câu trích ✓ | Số không nguồn | Thời gian TB ± ĐLC | p50 | p95 | Token vào / ra TB | USD/câu |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| FINQUERY | 1/1 | 0 | 0 | **1.0/1** | 0/0 | 1/1 | 4/4 | 0 | 30.0 ± 0.0 s | 30.0 s | 30.0 s | 62190 / 2409 | 0.1508 |

## 2. Theo nhóm câu (đúng / một phần / sai)

| Nhóm | FINQUERY |
|---|---|
| rag_van_ban | 0 / 0 / 0 |
| rag_bang | 1 / 0 / 0 |
| rag_nhieu_trang | 0 / 0 / 0 |

## 3. Từng câu

| Câu | Nhóm | FINQUERY |
|---|---|---|
| B07 | rag_bang | ✓ đúng · trang vàng |

## 4. Cần người duyệt

### B07 · FINQUERY · ✓ đúng

**Câu hỏi:** Sau đợt phát hành cổ phiếu trả cổ tức năm 2025, vốn điều lệ của công ty là bao nhiêu?  
**Đáp án vàng:** 1.281.095.330.000 đồng (128.109.533 cổ phiếu), tăng từ 1.143.858.790.000 đồng do phát hành 13.723.654 cổ phiếu trả cổ tức năm 2025.  
**Lý do chấm:** Câu trả lời nêu đúng vốn điều lệ 1.281.095.330.000 đồng, số vốn trước đó 1.143.858.790.000 đồng và 13.723.654 cổ phiếu phát hành thêm, khớp với đáp án tham chiếu.  
**Trang trích:** CTR-CBTTPHATHANH-20260720-6e5e03#1 CTR-NGHIQUYET-20260401-17e416#72 CTR-CBTTCOTUC-20260525-cdc26e#2 CTR-BCTC-HN-20260630-BAN_NIEN-b7334c#7

Theo thông báo của công ty về thay đổi số lượng cổ phiếu có quyền biểu quyết (ngày 20/07/2026), vốn điều lệ sau khi phát hành cổ phiếu để trả cổ tức năm 2025 là 1.281.095.330.000 đồng. Vốn điều lệ trước khi thay đổi là 1.143.858.790.000 đồng và phần tăng thêm là 137.236.540.000 đồng [C7]. Tổng số cổ phiếu tăng từ 114.385.879 lên 128.109.533 (phát hành thêm 13.723.654 cổ phiếu) [C7].

Các số liệu khác trong tài liệu không trùng với con số trên:
- Tờ trình ĐHĐCĐ ngày 01/04/2026 nêu phương án vốn điều lệ cuối kỳ là 1.281.121.844.800 đồng [C1].
- Nghị quyết HĐQT ngày 25/05/2026 nêu số cổ phiếu dự kiến phát hành là 13.726.305 cổ phiếu, tương đương 137.263.050.000 đồng theo mệnh giá [C17].
- Đây là số theo phương án, còn thông báo ngày 20/07/2026 là số sau khi thay đổi. Vì vậy tôi lấy 1.281.095.330.000 đồng làm số vốn điều lệ sau phát hành. Tài liệu không giải thích vì sao hai con số khác nhau.
- Báo cáo tài chính hợp nhất tại 30/06/2026 vẫn ghi vốn góp của chủ sở hữu là 1.143.858.790.000 đồng [D2].
