# Benchmark truy hồi — 12/10/2026 20:18

Bộ câu hỏi: `data/eval/eval_test.yml` (45 câu có trang vàng; câu từ chối chỉ ghi điểm rerank). Model trong `.env`: tầng 1 `Qwen/Qwen3-Reranker-0.6B`, tầng 2 `Qwen/Qwen3-Reranker-4B`. Chi tiết từng câu: `benchmark-truy-hoi_20261012-2002.csv`.

Chỉ số tính theo trang (k chunk đầu đổi về trang, mỗi trang tính một lần). Recall@k = có ít nhất một trang vàng trong k; nDCG@20 có tính mức 2/1; MRR = 1/hạng trang vàng đầu tiên; Đủ bằng chứng = câu nhiều trang lấy đủ mọi bằng chứng trong 20; Bị lừa = trang dễ nhầm xếp trên trang vàng. nDCG@20 theo bằng chứng: mỗi bằng chứng chỉ cần một trang (cùng con số in lại ở nhiều báo cáo thì một trang là đủ trả lời), không phạt việc không lấy hết các bản in lại.

## 1. Tổng hợp theo cấu hình

| Cấu hình | Mô tả | n | Recall@Qdrant | Recall@tầng 1 | Recall@20 | nDCG@20 | nDCG@20 theo bằng chứng | MRR | Đủ bằng chứng | Bị lừa | p50 ms | p95 ms | USD/câu | Lỗi |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| CHINH | từ khoá + bộ lọc + rerank 2 tầng, 80 → 40 → 20 | 45 | 95,6% | 95,6% | 95,6% | 0,526 | 0,622 | 0,719 | 7/12 | 1/9 | 18565 | 35926 | 0.0025 | 0 |

## 2. So với cấu hình chính (bootstrap ghép cặp, 10.000 lần, khoảng tin cậy 95%)

Khoảng không chứa 0 thì chênh lệch đáng tin; chứa 0 thì chưa kết luận được với 50 câu.

| Cấu hình | Δ nDCG@20 | KTC 95% | Δ nDCG@20 theo bằng chứng | KTC 95% | Δ Recall@20 | KTC 95% |
|---|---|---|---|---|---|---|

## 3. Cấu hình chính theo nhóm câu

| Nhóm | n | Recall@20 | nDCG@20 | nDCG@20 theo bằng chứng | MRR |
|---|---|---|---|---|---|
| db_mot_so | 11 | 100,0% | 0,551 | 0,723 | 0,715 |
| db_so_sanh | 7 | 100,0% | 0,359 | 0,628 | 0,814 |
| rag_bang | 10 | 100,0% | 0,589 | 0,528 | 0,744 |
| rag_nhieu_trang | 5 | 80,0% | 0,334 | 0,412 | 0,633 |
| rag_van_ban | 11 | 100,0% | 0,684 | 0,757 | 0,742 |
| tu_choi | 1 | 0,0% | 0,000 | 0,000 | 0,000 |

## 4. Cấu hình chính theo ca biên

| Ca biên | n | Recall@20 | nDCG@20 | nDCG@20 theo bằng chứng |
|---|---|---|---|---|
| ban_nien | 2 | 100,0% | 0,618 | 1,000 |
| bang_vat_trang | 1 | 100,0% | 0,594 | 1,000 |
| cach_goi_doi_thuong | 5 | 100,0% | 0,608 | 0,800 |
| cau_mo | 1 | 100,0% | 1,000 | 1,000 |
| cau_vi_sao | 3 | 100,0% | 0,710 | 0,710 |
| cuoi_nam | 3 | 100,0% | 0,404 | 0,583 |
| khong_dau | 2 | 100,0% | 0,526 | 0,565 |
| ky_chua_du | 1 | 0,0% | 0,000 | 0,000 |
| nam_gan | 1 | 100,0% | 0,674 | 0,500 |
| ngay_thang | 1 | 100,0% | 1,000 | 1,000 |
| nhan_su | 1 | 100,0% | 0,263 | 0,263 |
| nhieu_ky | 1 | 100,0% | 0,532 | 0,613 |
| nhieu_nam | 3 | 66,7% | 0,213 | 0,175 |
| pham_vi_rieng | 4 | 100,0% | 0,604 | 0,660 |
| quy_vs_luy_ke | 2 | 100,0% | 1,000 | 1,000 |
| so_am | 1 | 100,0% | 0,532 | 1,000 |
| so_sanh_ke_hoach | 1 | 100,0% | 0,501 | 0,920 |
| tang_truong | 2 | 100,0% | 0,290 | 0,807 |
| thoi_gian_tuong_doi | 4 | 100,0% | 0,717 | 0,625 |
| tra_loi_mot_phan | 1 | 0,0% | 0,000 | 0,000 |
| trinh_bay_lai | 1 | 100,0% | 0,039 | 0,120 |
| ty_le | 4 | 100,0% | 0,397 | 0,577 |
| viet_tat | 7 | 100,0% | 0,436 | 0,593 |

## 5. Câu cấu hình chính trượt (không có trang vàng trong 20) hoặc trang vàng xếp sau hạng 5

| Câu | Nhóm | Câu hỏi | Hạng trang vàng | Có ở Qdrant? | Có sau tầng 1? | Hạng 1 là | Lọc |
|---|---|---|---|---|---|---|---|
| B04 | rag_bang | ROE và ROA năm 2025 của CTR là bao nhiêu? | 20 | có | có | Nghị quyết ĐHĐCĐ 2025 — ký ngày 26/03/2025 tr.4 | strict |
| B07 | rag_bang | Sau đợt phát hành cổ phiếu trả cổ tức năm 2025, vốn điều lệ của công ty là bao nhiêu? | 7 | có | có | Nghị quyết ĐHĐCĐ 2026 — ký ngày 01/04/2026 tr.72 | strict |
| M02 | rag_nhieu_trang | Tỷ lệ cổ tức bằng tiền năm 2023 và năm 2024 khác nhau thế nào? | 6 | có | có | Báo cáo thường niên năm 2024 tr.19 | strict |
| M05 | rag_nhieu_trang | Vốn điều lệ của công ty đã thay đổi thế nào từ năm 2022 đến nay? | — | KHÔNG | KHÔNG | Đính chính / trình bày lại số liệu năm 2022 tr.4 | strict |
| D02 | db_mot_so | LNST trên báo cáo tài chính riêng năm 2025 là bao nhiêu? | 7 | có | có | Báo cáo thường niên năm 2025 tr.63 | strict |
| D09 | db_mot_so | Doanh thu thuần hợp nhất năm 2019 (theo số liệu đã trình bày lại) là bao nhiêu? | 17 | có | có | Báo cáo tài chính hợp nhất năm 2019 (bản 1/2) tr.9 | strict |
| V11 | rag_van_ban | Ai ký công văn giải trình kết quả kinh doanh quý 2/2024 gửi Ủy ban Chứng khoán? | 13 | có | có | Công văn giải trình 2024 — ký ngày 28/07/2024 tr.1 | strict |
| X06 | tu_choi | Doanh thu thuần hợp nhất cả năm 2026 là bao nhiêu? | — | KHÔNG | KHÔNG | Công bố thông tin 2026 — ký ngày 16/03/2026 tr.64 | strict |

## 6. Điểm rerank cao nhất: câu từ chối so với câu trả lời được (cấu hình chính)

Nếu câu từ chối có điểm cao nhất thấp hơn rõ rệt thì dùng được làm ngưỡng từ chối.

| Câu | Nhóm | Điểm cao nhất |
|---|---|---|
| X06 | tu_choi | 0.9769 |
| X01 | tu_choi | 0.9943 |
| X02 | tu_choi | 0.9659 |
| X03 | tu_choi | 0.6158 |
| X04 | tu_choi | 0.8579 |
| X05 | tu_choi | 0.0130 |
| (câu trả lời được) | thấp nhất / trung vị | 0.9715 / 0.9974 |
