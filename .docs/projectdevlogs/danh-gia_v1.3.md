# Kế hoạch đánh giá — v1.3

*Ngày: 2026-09-21 · Thay cho [v1.2](danh-gia_v1.2.md)*

> ## ⚠️ Trạng thái: **ý tưởng và định hướng, chưa chốt**
> Toàn bộ tài liệu này là **danh sách ý** để khi bắt tay làm thì có sẵn hướng, **không phải quyết định cuối cùng**. Cụ thể:
> - Số lượng câu hỏi, số mô hình đưa vào so sánh, ngưỡng "chất lượng tăng rõ rệt", thang M0–M3: đều là **con số tạm tính**.
> - Việc đổi mô hình có phải sửa pipeline hay không, sửa những gì: **chỉ biết chắc khi chạy thật** (T9).
> - Bộ chuẩn công khai (VN-MTEB…) có dùng được không còn tuỳ **định dạng dữ liệu và công sức tích hợp thực tế**.
>
> **Cách dùng:** đến từng bước trong kế hoạch thì mở tài liệu này ra, làm thật, rồi **cập nhật lại bằng số đo thật** ở bản kế tiếp. Chỗ nào thực tế khác dự kiến thì ghi rõ khác ở đâu — đó chính là nội dung tốt cho phần "bài học rút ra" của báo cáo.

## Thay đổi so với v1.2
- Thêm phần trạng thái ở trên: làm rõ đây là ý tưởng, chưa chốt.

## Thay đổi của v1.2 so với v1.1
- Bổ sung **chiến lược ba lớp** để giảm công gán nhãn: bộ chuẩn công khai tiếng Việt (VN-MTEB), bộ sinh tự động trên dữ liệu CTR, và bộ tự làm 40–60 câu (mục 8).
- Bổ sung **trục đánh giá thứ hai**: đổi mô hình thì **pipeline phải thay đổi những gì và tốn bao nhiêu** — có phiếu tương thích và thang đo mức độ thay đổi (mục 9).
- Thêm **T9: thử nghiệm tương thích mô hình**.

## Thay đổi của v1.1 so với v1.0
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
| **T9** | **Thử nghiệm tương thích mô hình** | Trả lời câu hỏi "đổi mô hình thì pipeline phải đổi gì" | Mỗi mô hình ứng viên chạy trên 20 tài liệu mẫu | Số bước pipeline bị ảnh hưởng · thời gian và chi phí tạo lại chỉ mục · lỗi phát sinh · công sức ước tính |

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

---

## 8. Chiến lược ba lớp để giảm công gán nhãn

| Lớp | Nguồn | Công sức | Trả lời câu hỏi |
|---|---|---|---|
| **L1. Bộ chuẩn công khai tiếng Việt** | **VN-MTEB** (Findings EACL 2026, arXiv 2507.21500): 41 tập dữ liệu, 6 nhóm nhiệm vụ, có VieQuAD, Zalo Legal Text, mMARCO-VI | Gần như không | Mô hình tiếng Việt có tốt hơn mô hình đa ngôn ngữ **nói chung** không → dùng sàng lọc ở Khối 2 |
| **L2. Bộ sinh tự động trên dữ liệu CTR** | Cho mô hình sinh câu hỏi từ chính các chunk của mình, thu được 300–500 cặp (câu hỏi ↔ đoạn đúng) | Một buổi | Mô hình nào truy xuất tốt hơn **trên đúng lĩnh vực và đúng dữ liệu** của đề tài |
| **L3. Bộ tự làm** | 40–60 câu do chủ đề tài viết và chấm | Vài buổi | **Đánh giá chính**: độ chính xác, trích dẫn, câu kết hợp, an toàn, "không xác định" |

- **Rủi ro của L2:** câu hỏi sinh ra hay lặp lại từ ngữ trong đoạn nên điểm bị thổi lên. Giảm bằng cách yêu cầu diễn đạt lại bằng từ khác và **soát tay khoảng 10%**.
- **Không bỏ được L3**, vì bốn thứ sau chỉ có ở FinQuery: câu hỏi kết hợp hai nguồn, độ chính xác trích dẫn tới trang, nhóm câu an toàn, nhóm câu "không xác định".
- **Giảm công chấm:** dùng mô hình chấm thay người (cách của paper 2), nhưng phải tự chấm khoảng 20 câu để đối chiếu mức độ đồng thuận trước.

---

## 9. Trục đánh giá thứ hai: đổi mô hình thì pipeline phải đổi gì

Chỉ so chất lượng là chưa đủ. Với mỗi mô hình ứng viên phải trả lời thêm: **đổi sang mô hình này thì phải sửa những bước nào, tốn bao nhiêu công, và có rủi ro gì.**

### 9.1. Phiếu tương thích mô hình (điền cho từng ứng viên)

| Mục kiểm tra | Vì sao quan trọng |
|---|---|
| Cần **tách từ** trước không (ví dụ dòng PhoBERT) | Thêm bước ở 1.5 khi nạp và 2.1 khi hỏi; **phải tách y hệt nhau** ở cả hai chỗ |
| **Độ dài tối đa** (token) | Quyết định kích thước chunk; chuỗi phải chọn theo mô hình ngắn nhất |
| **Tiền tố hoặc chỉ dẫn** bắt buộc (một số mô hình yêu cầu đánh dấu truy vấn và đoạn văn khác nhau) | Sai tiền tố thì chất lượng tụt mà không báo lỗi |
| **Số chiều vector** và cách chuẩn hoá | Đổi số chiều thì phải tạo collection mới trong Qdrant |
| **Định dạng đầu vào của reranker** (cặp câu hỏi và đoạn văn, hay danh sách) | Ảnh hưởng cách gọi và cách gộp điểm |
| **Cách phục vụ**: ONNX cục bộ, container, hay API | Ảnh hưởng độ trễ, chi phí, và việc dữ liệu có ra ngoài hay không |
| **Giấy phép** | Có được dùng trong môi trường doanh nghiệp không |
| **Thời gian và chi phí tạo lại chỉ mục** | Đổi mô hình embedding là phải nhúng lại toàn bộ |

### 9.2. Thang đo mức độ thay đổi

| Mức | Nghĩa | Ví dụ |
|---|---|---|
| **M0** | Chỉ đổi cấu hình | Đổi mô hình rerank tầng 2 sang nhà cung cấp khác, cùng kiểu đầu vào |
| **M1** | Đổi cấu hình + **tạo lại chỉ mục** | Đổi mô hình embedding, giữ nguyên kích thước chunk |
| **M2** | Phải **sửa bước tiền xử lý** | Mô hình cần tách từ, hoặc buộc phải giảm kích thước chunk rồi chunk lại từ đầu |
| **M3** | Phải **sửa mã nguồn của pipeline** | Đổi kiểu reranker (chấm từng cặp sang chấm cả danh sách), đổi cách gộp điểm |

Mỗi ứng viên trong bảng kết quả ghi đủ ba cột: **điểm chất lượng**, **mức thay đổi (M0–M3)**, **công sức ước tính (giờ)**.

### 9.3. Quy tắc kết luận

| Chất lượng tăng | Mức thay đổi | Kết luận |
|---|---|---|
| Rõ rệt | M0 hoặc M1 | **Đổi** |
| Rõ rệt | M2 hoặc M3 | Cân nhắc: chỉ đổi nếu còn thời gian; ghi lại như một hướng phát triển |
| Nhỏ | M0 | Đổi nếu rẻ hơn hoặc nhanh hơn |
| Nhỏ | M1 trở lên | **Giữ nguyên**, và ghi kết quả thí nghiệm vào báo cáo |

Cách trình bày này cho thấy đề tài không chỉ đo "mô hình nào điểm cao hơn" mà còn đo **chi phí kỹ thuật của việc thay mô hình** — đúng tinh thần một hệ thống triển khai thật.
