# Kế hoạch đánh giá — v1.1

*Ngày: 2026-09-21 · Thay cho [v1.0](danh-gia_v1.0.md)*

## Thay đổi so với v1.0
- Tách **T5** thành **T5a (so sánh mô hình embedding)** và **T5b (so sánh reranker)**, bổ sung nhóm **mô hình chuyên tiếng Việt** như PhoBERT và PhoRanker (QĐ-024).
- Thêm mục 7: **ảnh hưởng của lựa chọn mô hình lên pipeline** (tách từ, độ dài chunk, tạo lại chỉ mục).

Tài liệu này mô tả **các bộ test** và **cách đo**, tập trung vào ba tiêu chí dễ bị bỏ sót khi thiết kế muộn: **độ trễ, lượng token và chi phí**. Nguyên tắc: mọi số đo đều lấy từ **nhật ký kiểm toán** (QĐ-016), không đo thủ công.

---

## 1. Các bộ test

| # | Bộ test | Mục đích | Quy mô dự kiến | Đo gì |
|---|---|---|---|---|
| **T1** | **Bộ câu hỏi chuẩn FinQuery** (tự xây, tiếng Việt, về CTR) | Đánh giá chính: so FinQuery với hai hệ thống đối chứng | 100–150 câu, phủ sáu mảng A–F | Độ chính xác · độ chính xác trích dẫn · độ trễ · token · chi phí |
| **T2** | **Bộ hội thoại nhiều lượt** | Kiểm tra khả năng nhớ ngữ cảnh (công ty, kỳ, chỉ tiêu, loại BCTC) | 10–15 chuỗi, mỗi chuỗi 3–4 lượt | Độ chính xác từng lượt · token cộng dồn theo lượt |
| **T3** | **Bộ kiểm tra an toàn** | Yêu cầu sửa dữ liệu, chèn lệnh, truy vấn bảng ngoài phạm vi | 10–15 câu | Tỷ lệ chặn đúng · có rò rỉ lược đồ hay dữ liệu không |
| **T4** | **Bộ câu “không xác định”** | Câu đúng phạm vi nhưng không có dữ liệu (covenant, trái phiếu, năm 2015, kế hoạch 2027) | 10 câu | Tỷ lệ trả lời “không xác định” đúng thay vì bịa |
| **T5a** | **So sánh mô hình embedding** (đa ngôn ngữ so với chuyên tiếng Việt) | Chọn mô hình tạo chỉ mục | Các câu nhánh RAG của T1, có gán nhãn đoạn đúng | Recall@100 · NDCG@20 trước khi rerank · độ trễ tạo embedding · thời gian và chi phí tạo lại chỉ mục |
| **T5b** | **So sánh reranker** | Chọn mô hình tầng 1 và tầng 2 | Như trên | NDCG@20 · **độ trễ từng tầng** · chi phí nếu gọi API |
| **T6** | **Bộ so sánh mô hình ngôn ngữ** | Chọn nhà cung cấp | 30–40 câu rút gọn từ T1 | Độ chính xác · độ trễ · token vào/ra · chi phí mỗi câu |
| **T7** | **Bộ đo hiệu năng** | Đo độ trễ ổn định, không quan tâm nội dung | 20 câu đại diện, mỗi câu chạy 5 lần | p50, p95 độ trễ · tỷ lệ trúng cache |
| **T8** | **Đo hiệu quả với người dùng thật** | Bằng chứng cho tiêu chí hiệu quả kinh tế (30 điểm VDT) | 10–15 câu, 3–5 người | Thời gian tra cứu thủ công so với dùng hệ thống |

**Bộ chuẩn quốc tế chỉ dùng để tham khảo, không chạy trực tiếp** vì bằng tiếng Anh và về doanh nghiệp Mỹ: FinQA, ConvFinQA, FinanceBench, TATQA (paper 2 dùng để đánh giá), MMARCO-VI (bài báo ViRanker dùng). Vai trò của chúng: học **cách tổ chức một bộ đề** (đầu vào, đáp án chuẩn, các bước tính) và lấy làm mốc tham chiếu khi bàn luận.

---

## 2. Ba hệ thống được so sánh (chạy cùng dữ liệu, cùng mô hình ngôn ngữ)

| Hệ thống | Mô tả |
|---|---|
| **FinQuery** | Định tuyến ba nhánh; function calling; RAG phân cấp có rerank hai tầng |
| **Text-to-SQL** | Mô hình nhận lược đồ cơ sở dữ liệu và tự sinh SQL; không đọc tài liệu |
| **RAG cơ bản** | Chunk cố định, tìm vector top-k, không rerank, không truy vấn cơ sở dữ liệu |

Cả ba là ba adapter của cùng một cổng `AnswerStrategy` (QĐ-020), nên chạy chung một bộ test và cùng một cách đo.

---

## 3. Đo độ trễ

**Đo theo từng chặng**, không chỉ đo tổng, để biết nút thắt nằm ở đâu:

| Chặng | Ghi vào nhật ký |
|---|---|
| Chuẩn hoá truy vấn, trích từ khoá | `ms_tien_truy_xuat` |
| Định tuyến (gọi mô hình chọn nhánh) | `ms_router` |
| Truy vấn cơ sở dữ liệu (function calling) | `ms_sql` |
| Tạo embedding câu hỏi | `ms_embed` |
| Tìm kiếm vector | `ms_vector` |
| Rerank tầng 1 | `ms_rerank1` |
| Rerank tầng 2 | `ms_rerank2` |
| Sinh câu trả lời | `ms_sinh` |
| **Tổng** | `ms_tong` |

Quy ước khi đo:
- Chạy mỗi câu **ít nhất 3 lần**, báo cáo **p50 và p95**, không báo cáo trung bình đơn lẻ.
- Ghi rõ **trúng cache hay không** (Redis); báo cáo hai cột: có cache và không cache.
- Ghi **độ trễ mạng** riêng cho các mô hình gọi qua API, vì phần này phụ thuộc nhà cung cấp.
- Ghi điều kiện chạy: máy, vùng đặt máy chủ, thời điểm (giờ cao điểm hay không).

---

## 4. Đo token và chi phí

**Ghi cho mỗi lượt hỏi** (tách theo từng lần gọi mô hình):

| Trường | Ý nghĩa |
|---|---|
| `mo_hinh` | Tên mô hình và phiên bản |
| `muc_dich` | router / sinh câu trả lời / rerank / embedding |
| `token_vao`, `token_ra` | Số token vào và ra |
| `token_cache` | Token được tính giá ưu đãi nhờ cache của nhà cung cấp (nếu có) |
| `don_gia_vao`, `don_gia_ra` | Đơn giá tại thời điểm chạy |
| `chi_phi` | Tính ngay khi ghi, để không phụ thuộc bảng giá sau này |

Cách báo cáo:
- **Chi phí trung bình mỗi câu hỏi** và **chi phí cho 1.000 câu hỏi** (con số này dễ hình dung khi nói chuyện với người dùng).
- Tách theo **loại nhánh**: câu đi nhánh cơ sở dữ liệu thường rẻ hơn nhiều so với câu đi nhánh tài liệu.
- Nêu rõ phần token của **ngữ cảnh truy xuất** (20 đoạn văn) trong tổng token vào, vì đây là khoản lớn nhất của nhánh RAG.
- Với text-to-SQL, nhớ tính cả **token của lược đồ** đưa vào prompt và **các lần sinh lại khi SQL lỗi**. Đây chính là chỗ FinQuery tiết kiệm.

---

## 5. So sánh và kết luận thống kê
- Với mỗi cặp hệ thống, so sánh trên **cùng tập câu hỏi**.
- Dùng **kiểm định t của Welch** cho độ trễ và chi phí (phương sai và cỡ mẫu hai nhóm thường không bằng nhau). Báo cáo trung bình, p50, p95 và giá trị p.
- Với độ chính xác (số liệu nhị phân đúng/sai), dùng tỷ lệ kèm khoảng tin cậy; nếu so hai hệ thống trên cùng bộ câu hỏi thì dùng kiểm định cho dữ liệu ghép cặp.

---

## 6. Việc cần làm
- [ ] Soạn T1 (khoảng 40 câu trong tuần đầu, đủ 100–150 câu ở tuần 5).
- [ ] Gán nhãn đoạn văn đúng cho các câu nhánh RAG để chạy được T5.
- [ ] Bổ sung các trường đo độ trễ, token, chi phí vào bảng nhật ký kiểm toán **ngay khi thiết kế schema**, không để đến cuối.
- [ ] Viết công cụ chạy bộ test theo lô và xuất bảng kết quả.
- [ ] Chuẩn bị kịch bản cho T8 (đo với người dùng thật).

---

## 7. So sánh mô hình tiếng Việt và ảnh hưởng tới pipeline (QĐ-024)

**Hai câu hỏi cần trả lời:** dùng mô hình chuyên tiếng Việt thì chất lượng truy xuất có tăng không, và nếu tăng thì pipeline có phải đổi không.

| Vai trò | Nhóm đa ngôn ngữ | Nhóm chuyên tiếng Việt |
|---|---|---|
| Embedding (T5a) | BGE-M3, multilingual-E5 | Bi-encoder nền **PhoBERT**, các mô hình embedding tiếng Việt khác |
| Reranker (T5b) | BGE-Reranker-v2-m3 | **ViRanker**, **PhoRanker** |

**Ảnh hưởng tới pipeline nếu chọn nhánh PhoBERT:**

| Điểm khác biệt | Hệ quả |
|---|---|
| PhoBERT cần **tách từ** trước khi đưa vào mô hình | Thêm một bước ở 1.5 (chuẩn hoá khi nạp) và 2.1 (chuẩn hoá truy vấn). **Phải tách y hệt nhau ở cả hai chỗ**, nếu lệch thì kết quả tìm kiếm sai |
| Giới hạn độ dài: dòng PhoBERT khoảng 256 token, ViRanker 512, BGE-M3 dài hơn nhiều | **Kích thước chunk phải chọn theo mô hình ngắn nhất trong chuỗi**, nếu không nội dung bị cắt |
| Đổi mô hình embedding | **Tạo lại toàn bộ chỉ mục** → mỗi mô hình một collection riêng trong Qdrant; tính cả thời gian và chi phí tạo lại chỉ mục vào bảng so sánh |

**Cách kết luận:** nếu mô hình tiếng Việt cho Recall@100 và NDCG@20 cao hơn rõ rệt thì chấp nhận đổi pipeline; nếu chênh lệch nhỏ mà phải thêm bước tách từ và giảm kích thước chunk, thì giữ mô hình đa ngôn ngữ và ghi lại kết quả thí nghiệm trong báo cáo.

**Cần kiểm chứng trước khi chạy:** tên và phiên bản chính xác của từng mô hình tiếng Việt, độ dài tối đa, giấy phép, cách chạy (ONNX cục bộ hay dịch vụ mô hình).
