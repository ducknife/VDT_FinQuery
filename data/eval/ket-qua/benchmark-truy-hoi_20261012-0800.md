# Benchmark truy hồi — 12/10/2026 11:41

Bộ câu hỏi: `data/eval/eval_test.yml` (45 câu có trang vàng; câu từ chối chỉ ghi điểm rerank). Model trong `.env`: tầng 1 `Qwen/Qwen3-Reranker-0.6B`, tầng 2 `Qwen/Qwen3-Reranker-4B`. Chi tiết từng câu: `benchmark-truy-hoi_20261012-0800.csv`.

Chỉ số tính theo trang (k chunk đầu đổi về trang, mỗi trang tính một lần). Recall@k = có ít nhất một trang vàng trong k; nDCG@20 có tính mức 2/1; MRR = 1/hạng trang vàng đầu tiên; Đủ bằng chứng = câu nhiều trang lấy đủ mọi bằng chứng trong 20; Bị lừa = trang dễ nhầm xếp trên trang vàng. nDCG@20 theo bằng chứng: mỗi bằng chứng chỉ cần một trang (cùng con số in lại ở nhiều báo cáo thì một trang là đủ trả lời), không phạt việc không lấy hết các bản in lại.

## 1. Tổng hợp theo cấu hình

| Cấu hình | Mô tả | n | Recall@Qdrant | Recall@tầng 1 | Recall@20 | nDCG@20 | nDCG@20 theo bằng chứng | MRR | Đủ bằng chứng | Bị lừa | p50 ms | p95 ms | USD/câu | Lỗi |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| CHINH | từ khoá + bộ lọc + rerank 2 tầng, 80 → 40 → 20 | 45 | 95,6% | 95,6% | 95,6% | 0,483 | 0,557 | 0,623 | 6/12 | 1/9 | 9762 | 14831 | 0.0026 | 0 |
| KHONG_LOC | tắt bộ lọc metadata | 45 | 95,6% | 95,6% | 93,3% | 0,465 | 0,544 | 0,609 | 7/12 | 2/9 | 10889 | 22144 | 0.0027 | 0 |
| KHONG_TU_KHOA | tắt trích từ khoá | 45 | 95,6% | 93,3% | 93,3% | 0,459 | 0,525 | 0,562 | 6/12 | 1/9 | 11245 | 18945 | 0.0025 | 0 |
| KHONG_RERANK | chỉ tìm vector, lấy 20 chunk đầu | 45 | 95,6% | 93,3% | 91,1% | 0,381 | 0,507 | 0,526 | 8/12 | 3/9 | 631 | 7551 | 0.0000 | 0 |
| MOT_TANG_4B | rerank 1 tầng: 4B chấm cả 80 chunk | 45 | 95,6% | 93,3% | 95,6% | 0,485 | 0,546 | 0,619 | 7/12 | 1/9 | 11839 | 23772 | 0.0029 | 0 |
| RUT_GON | rút gọn 50 → 20 → 10 | 45 | 93,3% | 88,9% | 86,7% | 0,450 | 0,528 | 0,605 | 5/12 | 1/9 | 11056 | 19146 | 0.0014 | 0 |
| B_4B_8B | rerank 2 tầng 4B → 8B | 45 | 95,6% | 95,6% | 91,1% | 0,441 | 0,499 | 0,503 | 5/12 | 1/9 | 19943 | 37133 | 0.0059 | 0 |
| RAG_CO_BAN | RAG cơ bản: nguyên câu hỏi, vector top 20, không lọc, không rerank | 45 | 95,6% | 88,9% | 68,9% | 0,294 | 0,363 | 0,357 | 5/12 | 3/9 | 809 | 13666 | 0.0000 | 0 |

## 2. So với cấu hình chính (bootstrap ghép cặp, 10.000 lần, khoảng tin cậy 95%)

Khoảng không chứa 0 thì chênh lệch đáng tin; chứa 0 thì chưa kết luận được với 50 câu.

| Cấu hình | Δ nDCG@20 | KTC 95% | Δ nDCG@20 theo bằng chứng | KTC 95% | Δ Recall@20 | KTC 95% |
|---|---|---|---|---|---|---|
| KHONG_LOC | -0,018 | [-0,053; +0,015] | -0,013 | [-0,047; +0,019] | -0,022 | [-0,089; +0,044] |
| KHONG_TU_KHOA | -0,024 | [-0,055; +0,003] | -0,032 | [-0,091; +0,017] | -0,022 | [-0,067; +0,000] |
| KHONG_RERANK | -0,103 | [-0,177; -0,032] | -0,050 | [-0,133; +0,032] | -0,044 | [-0,111; +0,000] |
| MOT_TANG_4B | +0,002 | [-0,015; +0,017] | -0,011 | [-0,034; +0,004] | +0,000 | [+0,000; +0,000] |
| RUT_GON | -0,034 | [-0,060; -0,010] | -0,029 | [-0,085; +0,006] | -0,089 | [-0,178; -0,022] |
| B_4B_8B | -0,043 | [-0,099; +0,015] | -0,058 | [-0,143; +0,024] | -0,044 | [-0,111; +0,000] |
| RAG_CO_BAN | -0,190 | [-0,266; -0,120] | -0,194 | [-0,285; -0,110] | -0,267 | [-0,422; -0,133] |

## 3. Cấu hình chính theo nhóm câu

| Nhóm | n | Recall@20 | nDCG@20 | nDCG@20 theo bằng chứng | MRR |
|---|---|---|---|---|---|
| db_mot_so | 11 | 100,0% | 0,438 | 0,537 | 0,472 |
| db_so_sanh | 7 | 100,0% | 0,305 | 0,576 | 0,667 |
| rag_bang | 10 | 100,0% | 0,517 | 0,464 | 0,630 |
| rag_nhieu_trang | 5 | 80,0% | 0,311 | 0,350 | 0,633 |
| rag_van_ban | 11 | 100,0% | 0,734 | 0,794 | 0,790 |
| tu_choi | 1 | 0,0% | 0,000 | 0,000 | 0,000 |

## 4. Cấu hình chính theo ca biên

| Ca biên | n | Recall@20 | nDCG@20 | nDCG@20 theo bằng chứng |
|---|---|---|---|---|
| ban_nien | 2 | 100,0% | 0,336 | 0,557 |
| bang_vat_trang | 1 | 100,0% | 0,587 | 1,000 |
| cach_goi_doi_thuong | 5 | 100,0% | 0,743 | 0,826 |
| cau_mo | 1 | 100,0% | 1,000 | 1,000 |
| cau_vi_sao | 3 | 100,0% | 1,000 | 1,000 |
| cuoi_nam | 3 | 100,0% | 0,364 | 0,681 |
| khong_dau | 2 | 100,0% | 0,712 | 0,750 |
| ky_chua_du | 1 | 0,0% | 0,000 | 0,000 |
| nam_gan | 1 | 100,0% | 0,586 | 0,500 |
| ngay_thang | 1 | 100,0% | 1,000 | 1,000 |
| nhan_su | 1 | 100,0% | 0,301 | 0,301 |
| nhieu_ky | 1 | 100,0% | 0,532 | 0,613 |
| nhieu_nam | 3 | 66,7% | 0,202 | 0,175 |
| pham_vi_rieng | 4 | 100,0% | 0,482 | 0,528 |
| quy_vs_luy_ke | 2 | 100,0% | 0,565 | 0,565 |
| so_am | 1 | 100,0% | 0,867 | 1,000 |
| so_sanh_ke_hoach | 1 | 100,0% | 0,417 | 0,613 |
| tang_truong | 2 | 100,0% | 0,212 | 0,500 |
| thoi_gian_tuong_doi | 4 | 100,0% | 0,547 | 0,437 |
| tra_loi_mot_phan | 1 | 0,0% | 0,000 | 0,000 |
| trinh_bay_lai | 1 | 100,0% | 0,039 | 0,120 |
| ty_le | 4 | 100,0% | 0,323 | 0,605 |
| viet_tat | 7 | 100,0% | 0,375 | 0,525 |

## 5. Câu cấu hình chính trượt (không có trang vàng trong 20) hoặc trang vàng xếp sau hạng 5

| Câu | Nhóm | Câu hỏi | Hạng trang vàng | Có ở Qdrant? | Có sau tầng 1? | Hạng 1 là | Lọc |
|---|---|---|---|---|---|---|---|
| B04 | rag_bang | ROE và ROA năm 2025 của CTR là bao nhiêu? | 20 | có | có | Nghị quyết ĐHĐCĐ 2025 — ký ngày 26/03/2025 tr.4 | strict |
| B05 | rag_bang | Vốn hóa thị trường của cổ phiếu CTR tại ngày 31/12/2025 là bao nhiêu? | 7 | có | có | Báo cáo trình ĐHĐCĐ 2025 — ký ngày 26/03/2025 tr.16 | strict |
| B07 | rag_bang | Sau đợt phát hành cổ phiếu trả cổ tức năm 2025, vốn điều lệ của công ty là bao nhiêu? | 9 | có | có | Nghị quyết ĐHĐCĐ 2026 — ký ngày 01/04/2026 tr.72 | strict |
| M02 | rag_nhieu_trang | Tỷ lệ cổ tức bằng tiền năm 2023 và năm 2024 khác nhau thế nào? | 6 | có | có | Báo cáo thường niên năm 2024 tr.19 | strict |
| M05 | rag_nhieu_trang | Vốn điều lệ của công ty đã thay đổi thế nào từ năm 2022 đến nay? | — | KHÔNG | KHÔNG | Đính chính / trình bày lại số liệu năm 2022 tr.4 | strict |
| D02 | db_mot_so | LNST trên báo cáo tài chính riêng năm 2025 là bao nhiêu? | 10 | có | có | Báo cáo thường niên năm 2025 tr.63 | strict |
| D03 | db_mot_so | Nợ phải trả hợp nhất tại kỳ báo cáo gần nhất là bao nhiêu? | 9 | có | có | Báo cáo tài chính hợp nhất quý 2 2026 tr.4 | strict |
| D09 | db_mot_so | Doanh thu thuần hợp nhất năm 2019 (theo số liệu đã trình bày lại) là bao nhiêu? | 17 | có | có | Báo cáo tài chính hợp nhất năm 2019 (bản 1/2) tr.9 | strict |
| V11 | rag_van_ban | Ai ký công văn giải trình kết quả kinh doanh quý 2/2024 gửi Ủy ban Chứng khoán? | 9 | có | có | Công văn giải trình 2024 — ký ngày 28/07/2024 tr.1 | strict |
| X06 | tu_choi | Doanh thu thuần hợp nhất cả năm 2026 là bao nhiêu? | — | KHÔNG | KHÔNG | Công bố thông tin 2026 — ký ngày 08/04/2026 tr.67 | strict |

## 6. Điểm rerank cao nhất: câu từ chối so với câu trả lời được (cấu hình chính)

Nếu câu từ chối có điểm cao nhất thấp hơn rõ rệt thì dùng được làm ngưỡng từ chối.

| Câu | Nhóm | Điểm cao nhất |
|---|---|---|
| X06 | tu_choi | 0.9870 |
| X01 | tu_choi | 0.9947 |
| X02 | tu_choi | 0.9676 |
| X03 | tu_choi | 0.8331 |
| X04 | tu_choi | 0.8579 |
| X05 | tu_choi | 0.0121 |
| (câu trả lời được) | thấp nhất / trung vị | 0.9383 / 0.9982 |
