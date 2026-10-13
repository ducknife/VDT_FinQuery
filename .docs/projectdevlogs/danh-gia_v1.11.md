# Đánh giá — v1.11: bảng benchmark tổng hợp cuối (chốt core)

*Ngày: 2026-10-13 · Kế thừa [danh-gia_v1.10.md](danh-gia_v1.10.md), [danh-gia_v1.8.md](danh-gia_v1.8.md), [danh-gia_v1.7.md](danh-gia_v1.7.md) · Mọi số lấy từ file kết quả trong `data/eval/ket-qua/`*

Bảng gom theo 5 tiêu chí của đề tài: độ chính xác, độ chính xác của trích dẫn, độ trễ, lượng token, chi phí API. Model sinh câu trả lời ở mọi bảng là `anthropic/claude-sonnet-5-5` qua DeepInfra, giá 2/10 USD cho 1 triệu token vào/ra (QĐ-035).

## 0. Phạm vi và lưu ý

| Bộ test | Số câu | Bảng |
|---|---|---|
| `data/eval/eval_test.yml` | 45 câu có trang vàng | A. Truy hồi |
| `data/eval/eval_test.yml`, nhóm `rag_*` + X01, X02, X05 | 26 + 3 từ chối | C. Câu trả lời nhánh tài liệu |
| `data/eval/eval_dbquery.yml` | 18 câu có số + 3 từ chối | B. Câu trả lời nhánh số liệu |

**Lưu ý về cấu hình** (hết số dư API nên chưa chạy lại toàn bộ sau QĐ-040, QĐ-041):
- Bảng C, dòng FinQuery: 27 câu lấy từ lượt 13/10 08:47–09:48 (prompt v1.4, agent tự viết câu tìm). **B07 và M03 lấy từ lượt 12:11** với code cuối (prompt v1.5, lần tìm đầu dùng câu gốc, phép tính C#/D# do Java tính). Dòng gốc trước khi thay cũng được giữ trong bảng để đối chiếu.
- Bảng B chạy ngày 12/10 với prompt v1.3 (chấm lại bằng bộ chấm v2 ngày 13/10), trước prompt v1.5.
- Chạy lại đầy đủ với code cuối cần khoảng 3,4 USD (nhánh tài liệu) + 1,1 USD (nhánh số liệu).

## 1. Tổng quan

| Tiêu chí | Nhánh tài liệu: FinQuery | Nhánh tài liệu: RAG cơ bản | Nhánh số liệu: FinQuery | Nhánh số liệu: Text-to-SQL | Nhánh số liệu: LLM không công cụ |
|---|---|---|---|---|---|
| **Truy hồi** Recall@20 / MRR | **95,6% / 0,719** | 68,9% / 0,357 | không áp dụng (SQL mẫu) | — | — |
| **Chính xác** | **26/26** (+ 3/3 từ chối) | 20/26, điểm 21,5/26 | **21/21** | 19/21 | 3/21 (0/18 câu có số) |
| **Trích dẫn**: câu trích có nguyên văn trong nguồn | **138/138** | 125/125 | **40/40** | 45/45 | không có nguồn |
| Số trong câu trả lời không có trong nguồn | **2** | 19 | **0** | 0 | 3 câu đưa số từ trí nhớ, đều sai |
| **Độ trễ** TB (p50) | 27,6 s (27,8 s) | 19,2 s (18,4 s) | **8,2 s (7,1 s)** | 10,2 s (10,4 s) | 6,3 s (6,5 s) |
| **Token** vào / ra TB | 50.076 / 1.414 | 26.552 / 1.105 | 22.214 / 610 | 19.172 / 1.145 | 149 / 746 |
| **Chi phí** USD/câu | 0,116 | 0,064 | 0,051 | 0,050 | 0,008 |

Khi dùng giao diện có stream (API `/api/v1/ask`), chữ đầu tiên của câu trả lời hiện sau **6,6 s** với câu số liệu D11 (tổng 8,6 s) ([api-hoi-dap_v1.0.md](api-hoi-dap_v1.0.md)).

## A. Truy hồi nhánh tài liệu (45 câu, không gọi LLM)

Nguồn: [danh-gia_v1.7.md](danh-gia_v1.7.md) mục A.

| Cấu hình | Recall@20 | nDCG@20 | MRR | p50 | USD/câu |
|---|---|---|---|---|---|
| **FinQuery** (chuẩn hoá + từ khoá + bộ lọc + Qdrant 80 + lọc tiếng Anh + rerank 40 → 20) | **95,6%** | **0,526** | **0,719** | 18,6 s* | 0,0025 |
| Không lọc tiếng Anh | 95,6% | 0,483 | 0,623 | 9,8 s | 0,0026 |
| Không bộ lọc metadata | 93,3% | 0,465 | 0,609 | 10,9 s | 0,0027 |
| Không từ khoá | 93,3% | 0,459 | 0,562 | 11,2 s | 0,0025 |
| Không rerank | 91,1% | 0,381 | 0,526 | 0,6 s | 0 |
| **RAG cơ bản** (vector top 20) | **68,9%** | **0,294** | **0,357** | 0,8 s | 0 |

- FinQuery so với RAG cơ bản: Recall@20 **+26,7 điểm** (KTC 95% [13,3; 42,2]), nDCG@20 **+0,190** (KTC 95% [0,120; 0,266]).
- Lọc tiếng Anh tăng MRR **+0,086** (KTC 95% [0,008; 0,165]).
- *Lượt đo p50 18,6 s chạy lúc máy thiếu RAM; độ trễ rerank thực tế khoảng 10 s.
- Từ 13/10, Qdrant tìm chính xác thay vì HNSW (8–14 ms, không đổi kết quả với kho hiện tại; QĐ-040).

## B. Câu trả lời nhánh số liệu (21 câu × 3 cấu hình)

Nguồn: `danh-gia-db_20261012-2308_cham-lai.md`, [danh-gia_v1.8.md](danh-gia_v1.8.md) (bộ chấm v2).

| Tiêu chí | **FinQuery (SQL mẫu)** | Text-to-SQL | LLM không công cụ |
|---|---|---|---|
| Đúng | **21/21** | 19/21 | 3/21 |
| — một số (11) | 11/11 | 9/11 | 0/11 |
| — so sánh, tỷ lệ (7) | 7/7 | 7/7 | 0/7 |
| — từ chối (3) | 3/3 | 3/3 | 3/3 |
| Còn sai | — | D02 viết sai số; D11 chọn sai chỉ tiêu "tiền mặt" | 18 câu có số: không có dữ liệu |
| Câu trích đúng | 40/40 | 45/45 | — |
| Số không có trong nguồn | 0 | 0 | 3 câu bịa số |
| Thời gian TB ± ĐLC | **8,2 ± 4,4 s** | 10,2 ± 2,4 s | 6,3 ± 0,8 s |
| p50 / p95 | **7,1 / 13,0 s** | 10,4 / 12,9 s | 6,5 / 7,5 s |
| Token vào / ra TB | 22.214 / 610 | 19.172 / 1.145 | 149 / 746 |
| USD/câu (cả lượt) | 0,0505 (1,06) | 0,0498 (1,05) | 0,0078 (0,16) |

## C. Câu trả lời nhánh tài liệu (26 câu + 3 từ chối)

Nguồn: `danh-gia-rag_20261013-0847.csv` và `danh-gia-rag_20261013-1211_m03-phep-tinh.csv`. Chấm bằng LLM (`AnswerJudge`) rồi người duyệt.

| Cấu hình | Đúng | Một phần | Sai | Điểm | Từ chối đúng | Trích đúng trang vàng | Câu trích ✓ | Số không nguồn | Thời gian TB ± ĐLC | p50 | p95 | Token vào / ra TB | USD/câu |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| **FinQuery (code cuối cho B07, M03)** | **26/26** | 0 | 0 | **26,0/26** | 3/3 | 24/26 | 138/138 | 2 | 27,6 ± 12,9 s | 27,8 s | 48,1 s | 50.076 / 1.414 | 0,1158 |
| FinQuery, lượt 08:47 (trước QĐ-040/041) | 24/26 | 1 | 1 | 24,5/26 | 3/3 | 24/26 | 139/139 | 1 | 28,1 ± 13,1 s | 27,8 s | 48,1 s | 50.043 / 1.423 | 0,1159 |
| RAG cơ bản | 20/26 | 3 | 3 | 21,5/26 | 3/3 | 19/26 | 125/125 | 19 | 19,2 ± 5,3 s | 18,4 s | 29,2 s | 26.552 / 1.105 | 0,0642 |

**Theo nhóm câu** (đúng / một phần / sai):

| Nhóm | FinQuery (code cuối) | RAG cơ bản |
|---|---|---|
| Văn bản (11) | 11 / 0 / 0 | 10 / 1 / 0 |
| Bảng (10) | **10 / 0 / 0** | 8 / 0 / 2 |
| Nhiều trang (5) | **5 / 0 / 0** | 2 / 2 / 1 |

**Hai câu đã sửa:**

| Câu | Trước | Nguyên nhân | Sửa | Sau |
|---|---|---|---|---|
| B07 (vốn điều lệ sau phát hành) | sai | Agent tự viết lại câu tìm, trang đúng rớt khỏi 80 ứng viên | QĐ-040: lần tìm đầu dùng câu gốc | đúng: 1.281.095.330.000 đồng |
| M03 (so sánh LNST Q2/2024 và Q3/2025) | một phần | DB thiếu LNST quý 2; không tính được giữa số tài liệu và số DB | QĐ-041: model khai báo, Java tính | đúng: chênh lệch 43,83 tỷ, tăng 35,04% |

## D. Trục model

| Phép đo | Claude Sonnet 5.5 | Qwen3.5-397B (thinking) |
|---|---|---|
| Nhánh số liệu, FinQuery, 21 câu | **21/21**, **8,2 ± 4,4 s**, 0,0505 USD/câu | 20/21 (D11 sai chỉ tiêu), 153,1 ± 75,1 s, 0,0162 USD/câu |
| Cả luồng nhánh tài liệu, 3 câu thử | 21,6–27,2 s, câu trích 12/12 | 77,8–167,0 s, câu trích 6/7 |
| Riêng bước sinh câu trả lời (V02, khoảng 24.000 token) | 13,5–16,1 s, 0,067–0,072 USD | bật thinking: 70,3–162,5 s; tắt thinking: 34,6–40,7 s nhưng tự thêm suy đoán |

Sonnet nhanh hơn khoảng 19 lần trên nhánh số liệu, đắt hơn khoảng 3 lần, và chính xác ngang hoặc hơn.

## E. Hạn chế đã biết

- **DB chưa trích LNST (và một phần chỉ tiêu) từ báo cáo quý 1 và quý 2** ([danh-gia_v1.10.md](danh-gia_v1.10.md) mục 5.2). Câu hỏi về các kỳ này được trả lời bằng số tài liệu (C#) và phép tính QĐ-041.
- Truy hồi vẫn yếu với trang bảng song ngữ ngắn có tên tài liệu chung chung (B07: cosine 0,62–0,65, hạng 57–61/80 ngay cả với câu gốc). Hướng sửa không làm chậm: tìm theo từ khoá (BM25) chạy song song.
- Phép tính QĐ-041 kiểm được số có trong nguồn, nhưng không kiểm được model chọn đúng dòng, đúng kỳ hay không.
- Chưa chạy lại cả bộ với code cuối (mục 0).
- Nhánh chia 2 nửa khi vượt 64k token chưa có câu nào kích hoạt trong bộ test.
