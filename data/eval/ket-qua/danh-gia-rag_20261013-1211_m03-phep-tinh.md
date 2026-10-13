# Đánh giá câu trả lời nhánh tài liệu — 13/10/2026 12:13

Bộ câu: `data/eval/eval_test.yml`, 2 câu (rag_* + X01, X02, X05). Model: `anthropic/claude-sonnet-5-5`. Chấm đúng ý: `AnswerJudge` (LLM), **cần người duyệt**. Chi tiết: `danh-gia-rag_20261013-1211_m03-phep-tinh.csv`.

- FINQUERY: agent + RAG phân cấp
- RAG_CO_BAN: nguyên câu hỏi -> vector top 20, cùng bước trả lời

## 1. Tổng hợp

| Cấu hình | Đúng | Một phần | Sai | Điểm (đúng 1, một phần 0,5) | Từ chối đúng | Trích đúng trang vàng | Câu trích ✓ | Số không nguồn | Thời gian TB ± ĐLC | p50 | p95 | Token vào / ra TB | USD/câu |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| FINQUERY | 2/2 | 0 | 0 | **2.0/2** | 0/0 | 1/2 | 8/8 | 1 | 29.6 ± 0.5 s | 29.2 s | 30.0 s | 64246 / 2419 | 0.1551 |

## 2. Theo nhóm câu (đúng / một phần / sai)

| Nhóm | FINQUERY |
|---|---|
| rag_van_ban | 0 / 0 / 0 |
| rag_bang | 1 / 0 / 0 |
| rag_nhieu_trang | 1 / 0 / 0 |

## 3. Từng câu

| Câu | Nhóm | FINQUERY |
|---|---|---|
| B07 | rag_bang | ✓ đúng · trang vàng |
| M03 | rag_nhieu_trang | ✓ đúng |

## 4. Cần người duyệt

### B07 · FINQUERY · ✓ đúng

**Câu hỏi:** Sau đợt phát hành cổ phiếu trả cổ tức năm 2025, vốn điều lệ của công ty là bao nhiêu?  
**Đáp án vàng:** 1.281.095.330.000 đồng (128.109.533 cổ phiếu), tăng từ 1.143.858.790.000 đồng do phát hành 13.723.654 cổ phiếu trả cổ tức năm 2025.  
**Lý do chấm:** Câu trả lời nêu đúng vốn điều lệ sau phát hành là 1.281.095.330.000 đồng, vốn trước đó 1.143.858.790.000 đồng và 13.723.654 cổ phiếu phát hành thêm, khớp với đáp án; chi tiết bổ sung không mâu thuẫn.  
**Trang trích:** CTR-CBTTPHATHANH-20260720-6e5e03#1 CTR-NGHIQUYET-20260401-17e416#72 CTR-CBTTCOTUC-20260525-cdc26e#2 CTR-BCTC-HN-20260630-BAN_NIEN-b7334c#7

Theo thông báo ngày 20/07/2026 gửi UBCKNN và HOSE, sau đợt phát hành cổ phiếu để trả cổ tức năm 2025, vốn điều lệ của Tổng Công ty là 1.281.095.330.000 đồng. Vốn điều lệ trước khi thay đổi là 1.143.858.790.000 đồng và mức tăng là 137.236.540.000 đồng [C7]. Tổng số cổ phiếu tăng từ 114.385.879 lên 128.109533, tức thêm 13.723.654 cổ phiếu [C7].

Hai con số trong phương án trước đó khác với số thực tế:
- Tờ trình ĐHĐCĐ ngày 01/04/2026 nêu vốn điều lệ cuối kỳ là 1.281.121.844.800 [C1].
- Nghị quyết HĐQT ngày 25/05/2026 nêu số cổ phiếu dự kiến phát hành là 13.726.305 cổ phiếu, tương ứng 137.263.050.000 đồng theo mệnh giá [C17].

Báo cáo tài chính hợp nhất bán niên 2026 cho thấy vốn góp của chủ sở hữu tại 30/06/2026 vẫn là 1.143.858.790.000 đồng [D2], nghĩa là chưa phản ánh đợt phát hành này. Các tài liệu được cung cấp không giải thích vì sao số thực tế khác với phương án.

### M03 · FINQUERY · ✓ đúng

**Câu hỏi:** So sánh LNST hợp nhất quý 2/2024 với quý 3/2025.  
**Đáp án vàng:** Quý II/2024: 125,1 tỷ; quý III/2025: 168,9 tỷ → cao hơn 43,8 tỷ.  
**Lý do chấm:** Số liệu quý 2/2024 (125,1 tỷ), quý 3/2025 (168,9 tỷ) và chênh lệch 43,8 tỷ khớp với đáp án; phần nêu thêm số công bố ban đầu 125,05 tỷ không mâu thuẫn.  
**Trang trích:** CTR-BCTC-HN-20250930-Q3-0e87e4#6 CTR-BCTC-HN-20250630-Q2-e98386#40 CTR-BCTC-HN-20240630-Q2-12a088#6

LNST hợp nhất quý 3/2025 (168.940.075.230 đồng) cao hơn quý 2/2024 (125.108.143.249 đồng). Chênh lệch là 43.831.931.981 đồng (≈ 43,83 tỷ đồng), tương ứng tăng tăng 35,04%.
- Quý 3/2025 (hợp nhất): lợi nhuận sau thuế TNDN là 168.940.075.230 đồng [D2]. Báo cáo tài chính hợp nhất quý 3/2025 ghi cùng số này [C3].
- Quý 2/2024 (hợp nhất): số liệu so sánh trong báo cáo tài chính hợp nhất quý 2/2025 là 125.108.143.249 đồng [C8]. Tôi dùng số này vì đây là số kỳ sau trình bày lại cho quý 2/2024.
- Báo cáo tài chính hợp nhất quý 2/2024 công bố ban đầu ghi 125.050.312.483 đồng [C2]. Hai số này lệch nhau, nên khi so sánh cần nêu rõ số nào được dùng.
- Các tài liệu được cung cấp không giải thích sự khác biệt giữa số công bố ban đầu và số so sánh của quý 2/2024.
