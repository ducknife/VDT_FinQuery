# Đánh giá — v1.7: bảng tổng hợp benchmark cuối

*Ngày: 2026-10-13 · Kế thừa [danh-gia_v1.6.md](danh-gia_v1.6.md) · Mọi số lấy từ file kết quả trong `data/eval/ket-qua/`, không ước đoán*

File này gom mọi bảng benchmark theo **5 tiêu chí của đề tài** (pham-vi_v1.0):
1. độ chính xác;
2. độ chính xác của trích dẫn;
3. độ trễ;
4. lượng token;
5. chi phí API.

## 0. Bộ test, cấu hình và độ phủ

**Bộ test**

| Bộ test | Số câu | Dùng cho |
|---|---|---|
| `data/eval/eval_test.yml` | 50: 11 văn bản, 10 bảng, 5 nhiều trang, 11 số liệu một số, 7 số liệu so sánh, 6 từ chối | benchmark truy hồi (45 câu có trang vàng) |
| `data/eval/eval_dbquery.yml` | 21: 18 câu số liệu + 3 câu từ chối có số (X03, X04, X06) | câu trả lời nhánh số liệu |

**Độ phủ 5 tiêu chí theo bảng**

| Bảng | Chính xác | Trích dẫn | Độ trễ | Token | Chi phí |
|---|---|---|---|---|---|
| A. Truy hồi (45 câu, 8 cấu hình + lọc tiếng Anh) | ✔ (truy hồi) | — (chưa sinh câu trả lời) | ✔ | — (không ghi riêng) | ✔ |
| B. Câu trả lời nhánh số liệu (21 câu × 3 cấu hình) | ✔ | ✔ | ✔ | ✔ | ✔ |
| C. Câu trả lời nhánh tài liệu (FinQuery so với RAG cơ bản) | **chưa chạy** | | | | |
| D. Trục model, cả luồng (3 câu thử) | ✔ (mẫu nhỏ) | ✔ | ✔ | ✔ | ✔ |
| E. Đo tách bước sinh câu trả lời (6 mẫu) | — | — | ✔ | ✔ | ✔ |

## A. Truy hồi nhánh tài liệu: FinQuery so với RAG cơ bản và các cấu hình rút bớt

- **Nguồn:** `benchmark-truy-hoi_20261012-0800.md/.csv` (lượt chính) và `benchmark-truy-hoi_20261012-2002.md/.csv` (có lọc tiếng Anh).
- **Điều kiện:** 45 câu có trang vàng; không gọi LLM.
- **Chỉ số tính theo trang:**
  - Recall@20: có ít nhất một trang vàng trong 20;
  - nDCG@20: tính mức 2/1;
  - nDCG theo bằng chứng: mỗi bằng chứng chỉ cần một trang;
  - MRR: 1 / hạng của trang vàng đầu tiên.
- **Chi phí** là embedding + rerank qua DeepInfra.

| Cấu hình | Mô tả | Recall@20 | nDCG@20 | nDCG theo bằng chứng | MRR | p50 | p95 | USD/câu |
|---|---|---|---|---|---|---|---|---|
| **CHINH + lọc tiếng Anh** (12/10 tối) | cấu hình đang dùng | **95,6%** | **0,526** | **0,622** | **0,719** | 18,6 s* | 34,7 s* | 0,0025 |
| CHINH | từ khoá + bộ lọc + rerank 2 tầng 80 → 40 → 20 | 95,6% | 0,483 | 0,557 | 0,623 | 9,8 s | 14,8 s | 0,0026 |
| MOT_TANG_4B | rerank 1 tầng (4B chấm cả 80) | 95,6% | 0,485 | 0,546 | 0,619 | 11,8 s | 23,8 s | 0,0029 |
| KHONG_LOC | tắt bộ lọc metadata | 93,3% | 0,465 | 0,544 | 0,609 | 10,9 s | 22,1 s | 0,0027 |
| KHONG_TU_KHOA | tắt trích từ khoá | 93,3% | 0,459 | 0,525 | 0,562 | 11,2 s | 18,9 s | 0,0025 |
| RUT_GON | 50 → 20 → 10 | 86,7% | 0,450 | 0,528 | 0,605 | 11,1 s | 19,1 s | 0,0014 |
| B_4B_8B | rerank 4B → 8B | 91,1% | 0,441 | 0,499 | 0,503 | 19,9 s | 37,1 s | 0,0059 |
| KHONG_RERANK | chỉ tìm vector, lấy 20 đầu | 91,1% | 0,381 | 0,507 | 0,526 | 0,6 s | 7,6 s | 0 |
| **RAG_CO_BAN** | nguyên câu hỏi, vector top 20; không chuẩn hoá, lọc, rerank | **68,9%** | **0,294** | 0,363 | 0,357 | 0,8 s | 13,7 s | 0 |

\* Lượt có lọc tiếng Anh đo lúc mạng/DeepInfra chậm: rerank p50 13,9 s so với 6,9 s, trong khi chi phí (tức lượng token) không đổi. Độ trễ của dòng này **chưa so được** với các dòng khác; cần đo xen kẽ (`LatencyProbeTest -Dlatency.ab=true`, chưa chạy).

**So ghép cặp với CHINH** (bootstrap 10.000 lần, khoảng tin cậy 95%; khoảng không chứa 0 là chênh lệch đáng tin):

| So với CHINH | Δ nDCG@20 | KTC 95% | Δ Recall@20 | KTC 95% |
|---|---|---|---|---|
| **RAG_CO_BAN** | **−0,190** | [−0,266; −0,120] | **−26,7 điểm** | [−42,2; −13,3] |
| KHONG_RERANK | −0,103 | [−0,177; −0,032] | −4,4 | [−11,1; 0] |
| RUT_GON | −0,034 | [−0,060; −0,010] | −8,9 | [−17,8; −2,2] |
| MOT_TANG_4B | +0,002 | [−0,015; +0,017] | 0 | — |
| Lọc tiếng Anh (Δ của lượt 20:02) | +0,038 (nDCG@20), **+0,086 (MRR)**, **+0,059 (nDCG theo bằng chứng)** | MRR [0,008; 0,165]; theo bằng chứng [0,005; 0,112] | 0 | — |

**Token:** không ghi riêng cho truy hồi. Chi phí rerank tính theo token mà DeepInfra trả về: khoảng 0,0026 USD/câu, tương đương khoảng 0,13 USD cho 50 câu.

## B. Câu trả lời nhánh số liệu: FinQuery so với text-to-SQL và LLM không công cụ

- **Nguồn:** `danh-gia-db_20261012-2308.csv/.md`, chạy ngày 12/10, 23:08.
- **Điều kiện:**
  - cùng model `anthropic/claude-sonnet-5-5` qua DeepInfra;
  - FinQuery và text-to-SQL dùng **cùng vòng agent, cùng bước trả lời, cùng bộ kiểm trích dẫn**;
  - text-to-SQL chạy SQL qua `SqlGuard` và user chỉ đọc `finq_reader`.
- **Chấm:** "đúng và đủ số được hỏi", như RQ3 của paper FinAI Data Assistant.

| Tiêu chí | FinQuery (SQL mẫu) | Text-to-SQL | LLM không công cụ |
|---|---|---|---|
| **Chính xác, máy chấm** | **21/21** | 14/21 | 3/21 (chỉ 3 câu từ chối) |
| Chính xác, soát tay | **21/21** | ~18–19/21 (sai D11; D02 viết sai số; C06 lủng củng) | **0/18 câu có số** |
| — một số (11) | 11/11 | 9/11 | 0/11 |
| — so sánh, tỷ lệ (7) | 7/7 | 4/7 (máy) / ~6/7 (tay: không làm tròn) | 0/7 |
| — từ chối (3) | 3/3 | 1/3 (máy) / 3/3 (tay) | 3/3 |
| **Trích dẫn đúng** (câu trích có nguyên văn trong nguồn) | **40/40** | 45/45 | không có nguồn để trích |
| Số trong câu trả lời không có trong nguồn | 0 | 0 | 3 câu đưa số **từ trí nhớ, đều sai** |
| **Độ trễ** TB ± ĐLC | **8,2 ± 4,4 s** | 10,2 ± 2,4 s | 6,3 ± 0,8 s |
| p50 / p95 | **7,1 / 13,0 s** | 10,4 / 12,9 s | 6,5 / 7,5 s |
| Lượt chọn tool TB | 1,90 | 2,29 | — |
| **Token** vào / ra TB mỗi câu | 22.214 / 610 | 19.172 / 1.145 | 149 / 746 |
| Token cả lượt (21 câu) | 479.297 | 426.659 | 18.788 |
| **Chi phí** USD/câu | 0,0505 | 0,0498 | 0,0078 |
| Chi phí cả lượt | 1,06 USD | 1,05 USD | 0,16 USD |
| Lỗi SQL / câu SQL bị chặn | không áp dụng (SQL cố định) | 0 / 0 | — |

**Đọc bảng:**
- FinQuery đúng tuyệt đối và nhanh hơn text-to-SQL khoảng 2–3 s (p50).
- Chi phí **ngang nhau**, vì danh sách 123 mã chỉ tiêu nằm trong prompt (khoảng 22.000 token vào mỗi câu). Đây là điểm còn tối ưu được.
- Text-to-SQL với model mạnh vẫn lấy đúng phần lớn số. Yếu ở **trình bày không ổn định** (không làm tròn, viết sai dấu nhóm số) và **chọn sai chỉ tiêu** theo cách gọi đời thường. Ngoài ra phải tin vào SQL do LLM viết.
- LLM không công cụ: Sonnet phần lớn từ chối trung thực. Khi đoán thì sai xa: doanh thu 2024 ~7.900 tỷ so với thật 12.609,52 tỷ; biên gộp ~11% so với thật 7,01%.

**Giới hạn:**
- 21 câu, mỗi cấu hình một lượt.
- Câu trả lời dao động giữa các lượt: X06 của FinQuery ở lượt 21:44 đưa thêm số 6 tháng, lượt 23:08 chỉ nói "không tìm thấy".
- Tiêu chí X06 và bộ chấm v2 còn chờ người làm đề tài chốt.

## C. Câu trả lời nhánh tài liệu: FinQuery so với RAG cơ bản (**chưa chạy**)

Hiện **chỉ có so sánh ở mức truy hồi** (bảng A: Recall@20 95,6% so với 68,9%). Chưa có so sánh **câu trả lời cuối** cho 26 câu tài liệu. Cần làm:
1. thêm cấu hình RAG cơ bản cho câu trả lời: vector top 20, không agent, cùng bước trả lời;
2. chọn cách chấm (tự chấm / LLM chấm / kết hợp);
3. chạy khoảng 2 USD.

## D. Trục model, cả luồng (mẫu nhỏ, chỉ để định hướng)

- **Nguồn:** `agent-thu_20261012-2021*…2025*.md` (Qwen, prompt v1.2) và `agent-thu_20261012-2047*.md` (Sonnet).
- **Điều kiện:** 3 câu nhánh tài liệu: V02, cổ tức 2024, kế hoạch LNST năm nay.

| Model | Thời gian/câu | Câu trích đúng | Token vào + ra (tổng 3 câu) | USD/câu |
|---|---|---|---|---|
| Qwen3.5-397B (bật thinking) | 77,8–167,0 s (TB 126 s) | 6/7 (1 câu ✗ do ký hiệu markdown, đã sửa bộ kiểm) | khoảng 110.000 | ~0,019 |
| **Sonnet 5.5** | **21,6–27,2 s** (TB 24,4 s) | **12/12** | khoảng 165.000 | ~0,081 |

Sonnet nhanh hơn khoảng 3–8 lần (trung bình khoảng 5 lần), đắt hơn khoảng 4 lần. Đây là căn cứ cho QĐ-035.

## E. Đo tách bước sinh câu trả lời (cùng đầu vào V02, khoảng 24.000 token)

- **Nguồn:** `do-tre-llm_20261012-2029.md`, 2 lần lặp mỗi biến thể.

| Biến thể | Tổng | Chờ token đầu | Token ra | Ghi chú chất lượng | USD/lần |
|---|---|---|---|---|---|
| Qwen, bật thinking | 70,3–162,5 s | 2,2–15,1 s | 1.450–3.269 | tốt, nhưng dao động mạnh | 0,015–0,021 |
| Qwen, tắt thinking | 34,6–40,7 s | 1,6–2,9 s | 613 | tự thêm đánh giá "chủ yếu do", "tăng đột biến" (vi phạm quy tắc không suy đoán) | 0,013 |
| **Sonnet 5.5** | **13,5–16,1 s** | — | 2.164–2.435 | tốt nhất | 0,067–0,072 |

## Việc còn thiếu để đủ bộ benchmark

1. **Bảng C**: câu trả lời nhánh tài liệu, FinQuery so với RAG cơ bản. Quan trọng nhất.
2. Chốt tiêu chí X06 và bộ chấm v2, rồi chấm lại bảng B trên CSV có sẵn (không tốn tiền).
3. Lặp bảng B thêm 1–2 lượt để có độ dao động (khoảng 2 USD/lượt).
4. Đo xen kẽ độ trễ khi bật/tắt lọc tiếng Anh, để dòng đầu bảng A so được độ trễ.
5. (Tuỳ chọn) Qwen trên đủ bộ test cho trục model.
