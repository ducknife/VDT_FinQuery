# Đánh giá câu trả lời nhánh tài liệu — 13/10/2026 11:40

Bộ câu: `data/eval/eval_test.yml`, 1 câu (rag_* + X01, X02, X05). Model: `anthropic/claude-sonnet-5-5`. Chấm đúng ý: `AnswerJudge` (LLM), **cần người duyệt**. Chi tiết: `danh-gia-rag_20261013-1139_b07-exact.csv`.

- FINQUERY: agent + RAG phân cấp
- RAG_CO_BAN: nguyên câu hỏi -> vector top 20, cùng bước trả lời

## 1. Tổng hợp

| Cấu hình | Đúng | Một phần | Sai | Điểm (đúng 1, một phần 0,5) | Từ chối đúng | Trích đúng trang vàng | Câu trích ✓ | Số không nguồn | Thời gian TB ± ĐLC | p50 | p95 | Token vào / ra TB | USD/câu |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| FINQUERY | 0/1 | 0 | 1 | **0.0/1** | 0/0 | 0/1 | 7/7 | 0 | 31.6 ± 0.0 s | 31.6 s | 31.6 s | 59224 / 2429 | 0.1448 |
| RAG_CO_BAN | 0/1 | 0 | 1 | **0.0/1** | 0/0 | 0/1 | 4/4 | 2 | 21.3 ± 0.0 s | 21.3 s | 21.3 s | 31807 / 1727 | 0.0809 |

## 2. Theo nhóm câu (đúng / một phần / sai)

| Nhóm | FINQUERY | RAG_CO_BAN |
|---|---|---|
| rag_van_ban | 0 / 0 / 0 | 0 / 0 / 0 |
| rag_bang | 0 / 0 / 1 | 0 / 0 / 1 |
| rag_nhieu_trang | 0 / 0 / 0 | 0 / 0 / 0 |

## 3. Từng câu

| Câu | Nhóm | FINQUERY | RAG_CO_BAN |
|---|---|---|---|
| B07 | rag_bang | ✗ sai | ✗ sai |

## 4. Cần người duyệt

### B07 · FINQUERY · ✗ sai

**Câu hỏi:** Sau đợt phát hành cổ phiếu trả cổ tức năm 2025, vốn điều lệ của công ty là bao nhiêu?  
**Đáp án vàng:** 1.281.095.330.000 đồng (128.109.533 cổ phiếu), tăng từ 1.143.858.790.000 đồng do phát hành 13.723.654 cổ phiếu trả cổ tức năm 2025.  
**Lý do chấm:** Câu trả lời nêu vốn điều lệ 1.281.121.844.800 đồng (theo phương án) và 13.726.305 cổ phiếu, không khớp với đáp án 1.281.095.330.000 đồng và 13.723.654 cổ phiếu; còn nói chưa có số liệu thực tế sau phát hành.  
**Trang trích:** CTR-NGHIQUYET-20260401-17e416#6 CTR-NGHIQUYET-20260424-6c9935#1 CTR-CBTTCOTUC-20260525-cdc26e#2 CTR-BCTC-HN-20260630-BAN_NIEN-b7334c#7 CTR-CBTTCOTUC-20260720-771a68#2

Tài liệu chỉ cho biết vốn điều lệ theo phương án đã được thông qua, chưa có số liệu thực tế sau khi phát hành xong. Theo phương án phân phối lợi nhuận năm 2025 được Đại hội đồng cổ đông thông qua, vốn điều lệ cuối kỳ là 1.281.121.844.800 đồng [C11]. Nghị quyết số 36/NQ-VCC ngày 24/04/2026 ghi cùng con số này [C16]. Phương án này gắn với việc trả cổ tức bằng cổ phiếu với số lượng cổ phiếu dự kiến phát hành là 13.726.305 cổ phiếu [C6].

Số liệu đã ghi nhận chưa phản ánh đợt phát hành. Vốn góp của chủ sở hữu hợp nhất tại 30/06/2026 là 1.143.858.790.000 đồng [D2], không đổi so với tại 31/12/2025 [D3]. Báo cáo kết quả đợt phát hành ngày 20/07/2026 (phần được cung cấp) chỉ nêu vốn điều lệ 1.143.858.790.000 đồng ở phần giới thiệu tổ chức phát hành [C18].

Lưu ý về chênh lệch giữa các tài liệu: bảng phân phối lợi nhuận ghi giá trị trả cổ tức bằng cổ phiếu là 137.263.054.800 đồng [C11], còn nghị quyết của Hội đồng quản trị ghi tổng giá trị dự kiến phát hành theo mệnh giá là 137.263.050.000 đồng [C6]. Tôi không tự tính lại vốn điều lệ sau phát hành.

### B07 · RAG_CO_BAN · ✗ sai

**Câu hỏi:** Sau đợt phát hành cổ phiếu trả cổ tức năm 2025, vốn điều lệ của công ty là bao nhiêu?  
**Đáp án vàng:** 1.281.095.330.000 đồng (128.109.533 cổ phiếu), tăng từ 1.143.858.790.000 đồng do phát hành 13.723.654 cổ phiếu trả cổ tức năm 2025.  
**Lý do chấm:** Câu trả lời nêu vốn điều lệ theo phương án là 1.281.121.844.800 đồng và nói chưa có số thực tế, không đưa ra đáp án 1.281.095.330.000 đồng (13.723.654 cổ phiếu) như tham chiếu; chỉ có số vốn trước phát hành là đúng.  
**Trang trích:** CTR-TAILIEUHOP-20260401-e24dee#33 CTR-CBTTCOTUC-20260720-771a68#2 CTR-CBTTKHAC-20260316-bd9141#71 CTR-CBTTCOTUC-20260525-cdc26e#4

Các tài liệu được cung cấp chưa có số vốn điều lệ thực tế sau khi phát hành xong; chỉ có số theo phương án trình Đại hội đồng cổ đông. Theo Tờ trình ngày 01/04/2026 về phân phối lợi nhuận năm 2025 và phát hành cổ phiếu trả cổ tức, vốn điều lệ cuối kỳ là 1.281.121.844.800 đồng [C12]. Tờ trình ghi giá trị trả cổ tức bằng cổ phiếu là 137.263.054.800 đồng [C12]. Đây là số trong phương án, chưa phải kết quả đợt phát hành.

Vốn điều lệ trước đợt phát hành là 1.143.858.790.000 đồng, theo Báo cáo kết quả đợt phát hành ngày 20/07/2026 [C7]. Con số này cũng là vốn góp cuối năm 2025 trên báo cáo tài chính hợp nhất [C6].

Nghị quyết HĐQT ngày 25/05/2026 phê duyệt phát hành 13.726.305 cổ phiếu, tổng giá trị theo mệnh giá 137.263.050.000 đồng [C17]. Con số này lệch so với 137.263.054.800 đồng trong Tờ trình [C12]. Đoạn trích của báo cáo kết quả phát hành [C7] dừng ở phần trước phát hành, nên chưa có số vốn điều lệ sau phát hành do công ty xác nhận.
