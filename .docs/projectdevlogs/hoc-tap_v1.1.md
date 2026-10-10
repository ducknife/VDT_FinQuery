# Học tập trong đề tài — v1.1

*Ngày: 2026-09-21 · Thay cho [v1.0](hoc-tap_v1.0.md)*

## Thay đổi so với v1.0
- Thêm **mục 3: phân rã đệ quy như một cách làm việc** (QĐ-022), kèm điều kiện dừng và cái bẫy phải tránh.
- **Dựng lại bản đồ kỹ năng** (nay là mục 4): bỏ cách chia theo tuần 1–6, chuyển sang **bốn khối nền** (QĐ-026); thay MariaDB VECTOR bằng **Qdrant** (QĐ-021); thêm OCR, mô hình chuyên tiếng Việt, hồ sơ cấu trúc tài liệu, đo độ trễ và chi phí.
- Bổ sung nhật ký bài học tới ngày 21/09.

---

## 1. Tinh thần của đề tài
FinQuery là đề tài **vừa học vừa làm**.
- **Chủ đề tài:** đã nắm luồng xử lý và ý tưởng của 2 paper (FinAI Data Assistant, Hierarchical Reranker); đã code Java nhưng **chưa từng làm RAG, LLM hay function calling bằng Java**; chưa làm chủ Kafka, Redis, Docker, K8s.
- **Claude ("sư phụ")** là **mentor thứ hai** của đề tài: giải thích, thiết kế cùng, viết code mẫu và code khó, review code, ghi lại bài học.

## 2. Cách làm việc mặc định cho mỗi phần việc

| Bước | Ai làm | Nội dung |
|---|---|---|
| 1. Hiểu | Sư phụ | Giải thích **ngắn, đúng phần đang cần**, bằng ví dụ gắn với FinQuery (không giảng thuật ngữ tràn lan) |
| 2. Thiết kế | Cùng làm | Thống nhất port, adapter, dữ liệu vào ra; ghi quyết định vào log |
| 3. Code | Tuỳ loại việc (xem bảng dưới) | |
| 4. Review | Sư phụ | Góp ý, chỉ lỗi, giải thích *vì sao* |
| 5. Ghi bài học | Sư phụ ghi, chủ đề tài bổ sung | Mục 5 của file này. Nguyên liệu cho phần "kiến thức thu được" trong báo cáo |

**Chia việc code:**

| Loại việc | Cách làm |
|---|---|
| Phần **mới hoàn toàn và khó** (tích hợp Spring AI, function calling, rerank 2 tầng, embedding, cấu hình Kafka/K8s lần đầu) | **Sư phụ viết mẫu** có chú thích tiếng Việt, giải thích từng khối. Chủ đề tài đọc, chạy thử và hỏi |
| Phần **lặp lại theo mẫu** (thêm tool SQL thứ 2, 3…; thêm adapter; thêm endpoint; migration Flyway) | **Chủ đề tài tự viết** theo mẫu đã có, sư phụ review |
| Phần **quen thuộc với dân Java** (REST, JPA, test) | Chủ đề tài viết, sư phụ review |
| Khi **trễ tiến độ** | Sư phụ viết nhiều hơn để giữ tỷ lệ hoàn thiện, ghi rõ trong log |

Mục tiêu: đến cuối đề tài, chủ đề tài **tự giải thích được mọi dòng code của lõi** trước hội đồng.

---

## 3. Phân rã đệ quy — một cách làm việc, không chỉ một lần chia

Góp ý của mentor "tiền xử lý → xử lý → đánh giá, rồi trong mỗi phần lại chia tiếp" (QĐ-022) **không phải là sơ đồ vẽ một lần cho đẹp**, mà là một thói quen áp dụng lại ở mọi mức.

### 3.1 Quy tắc
Với bất kỳ việc nào chưa làm được ngay, hỏi đúng ba câu:
1. **Đầu vào là gì, đầu ra là gì?**
2. **Chia thành 3–5 bước con, mỗi bước cũng có đầu vào và đầu ra rõ ràng.**
3. Bước con nào **vẫn chưa làm được ngay** thì lặp lại từ câu 1 cho chính nó.

### 3.2 Điều kiện dừng
Đây là phần quan trọng nhất, thiếu nó thì chia mãi không xong. **Dừng chia khi một bước con thoả cả ba:**
- Làm xong trong **khoảng nửa ngày**.
- **Chạy được và nhìn thấy kết quả** (một con số, một tệp, một dòng log), không phải "đã suy nghĩ xong".
- **Sai thì biết ngay là sai ở bước này**, không phải mò cả hệ thống.

### 3.3 Ví dụ ba mức trong chính đề tài này

```
Mức 0: FinQuery
└── Mức 1: Tiền xử lý
    └── Mức 2: Nạp một tài liệu thành chunk
        ├── Mức 3: Xác định khung tài liệu   ← đang làm (data/formats/*.json)
        ├── Mức 3: Tách phần tiếng Việt khỏi bản song ngữ
        ├── Mức 3: Đo chất lượng lớp chữ     ← đã xong (kiem_tra_pdf.py)
        ├── Mức 3: OCR nếu lớp chữ kém        ← chưa chọn công cụ
        └── Mức 3: Cắt đoạn theo mục thuyết minh
```

Mức 3 dừng lại được, vì mỗi ô đều chạy ra kết quả nhìn thấy trong nửa ngày. Còn "tiền xử lý" ở mức 1 thì không — nên phải chia tiếp.

### 3.4 Cái bẫy phải tránh
Đệ quy theo chiều ngang dễ dẫn tới **làm xong toàn bộ tiền xử lý rồi mới đụng tới xử lý**, đến lúc đó mới phát hiện cách cắt đoạn không hợp với reranker — phải làm lại từ đầu.

Cách tránh: đi **một lát cắt dọc mỏng trước** (nguyên tắc số 1 trong `ke-hoach_v1.1.md`) — một tài liệu, một câu hỏi, chạy suốt từ nạp tới câu trả lời — rồi mới quay lại đào sâu từng nhánh. Nói cách khác: **đệ quy để hiểu bài toán, lát cắt dọc để kiểm chứng**. Bốn khối nền (QĐ-026) chính là lát cắt dọc đó.

### 3.5 Vì sao đáng học
Cách này dùng được cho mọi dự án sau này, không riêng FinQuery. Nó cho ba thứ: **ước lượng thời gian sát hơn** (ước lượng ô nửa ngày dễ hơn ước lượng "làm RAG"), **biết mình đang tắc ở đâu**, và **chia việc cho người khác được**. Đây là nội dung đáng viết vào phần "kiến thức thu được" của báo cáo cuối kỳ.

---

## 4. Bản đồ kỹ năng

*Cột "học khi nào" bám theo bốn khối nền trong `ke-hoach_v1.1.md`, không chia theo tuần nữa.*

### Khối 1 — DB core (22–27/9)

| Kỹ năng | Hiện tại | Đã đạt khi |
|---|---|---|
| Đọc mẫu biểu kế toán (B01, B02, B03, B09) | Đã đối chiếu bản gốc B09-DN của TT99 | Nhìn một BCTC lạ là chỉ ra được mục nào chứa số cần lấy |
| Thiết kế lược đồ có **mã nội bộ + bảng ánh xạ** theo chế độ kế toán | Đã hiểu vấn đề mã đổi giữa TT200 và TT99 | Truy vấn cùng một chỉ tiêu xuyên 2019–2026 ra đúng số |
| Flyway migration | Chưa | Tự viết được migration thêm bảng mới, chạy lại từ đầu không lỗi |
| **Qdrant** (thay MariaDB VECTOR — QĐ-021) | Chưa | Tạo collection, thiết kế payload, lọc theo năm và loại tài liệu khi tìm |
| Docker / Compose | Đã có công cụ | Tự viết `compose.yaml` dựng MariaDB và Qdrant |

### Khối 2 — Model (22–27/9)

| Kỹ năng | Hiện tại | Đã đạt khi |
|---|---|---|
| **OCR** (chưa chọn công cụ — QĐ-008) | Đã đo được 6.700 trang cần OCR | Chạy OCR một BCTC scan, đọc lại được tên mục thuyết minh |
| Embedding và độ tương đồng | Chưa | Giải thích được embedding, cosine; truy vấn top-k trên Qdrant |
| **Mô hình chuyên tiếng Việt** (PhoBERT, PhoRanker, ViRanker — QĐ-024) | Đã tra thông số ViRanker | Nói rõ được vì sao chọn mô hình này thay vì mô hình đa ngôn ngữ, bằng **số đo**, không bằng cảm tính |
| Rerank 2 tầng (cross-encoder) | Đã hiểu ý tưởng paper | Giải thích được cross-encoder khác bi-encoder thế nào; đo được NDCG |
| Phục vụ mô hình (API bên ngoài hay chạy tại chỗ) | Chưa | Đổi nhà cung cấp chỉ bằng đổi cấu hình, không sửa code lõi |

### Khối 3 — Inference (28/9 – 4/10)

| Kỹ năng | Hiện tại | Đã đạt khi |
|---|---|---|
| Nạp tài liệu (PDFBox, POI, chunk, table-to-json) | Chưa | Nạp được một BCTC thành chunk **có số trang và tên mục thuyết minh** |
| Spring AI + **function calling** | Chưa | Tự thêm một tool SQL mới, mô hình gọi đúng tham số |
| Định tuyến 3 nhánh (DB · tài liệu · kết hợp) | Chưa | Câu hỏi đi đúng nhánh, ghi lại được lý do chọn nhánh |
| Trích dẫn nguồn và ca "không xác định" | Chưa | Câu trả lời nào cũng chỉ ra được trang gốc; hỏi ngoài dữ liệu thì từ chối |
| **Hexagonal** + Spring Modulith | Mới biết khái niệm | Tự thêm được một adapter mới đúng ranh giới; ArchUnit không báo vi phạm |

### Khối 4 — Measure + Test (5–11/10)

| Kỹ năng | Hiện tại | Đã đạt khi |
|---|---|---|
| Đo **độ trễ theo chặng, token, chi phí** | Chưa | Mỗi câu hỏi ghi được đủ số vào nhật ký kiểm toán, xuất ra bảng |
| Làm bộ câu hỏi chuẩn và chấm điểm | Đã có khung T1–T9 (chưa chốt) | Chạy 40 câu × 2 hệ thống, xuất bảng so sánh |
| Kiểm định thống kê (Welch t-test) | Đã đọc paper | Nói được "hơn bao nhiêu, và có đáng tin không" |

### Sau khi nghiệm thu nền (QĐ-023)

| Kỹ năng | Hiện tại | Đã đạt khi |
|---|---|---|
| Kafka (KRaft) | Chưa | Giải thích được topic, partition, consumer group, offset; làm thí nghiệm 3 broker để xem chịu lỗi |
| Redis (bộ nhớ hội thoại, cache, giới hạn tần suất) | Chưa | Hội thoại nhiều lượt chạy được; đo được tỷ lệ cache trúng |
| Keycloak / Spring Security | Chưa | Đăng nhập SSO, phân 3 vai trò |
| Kubernetes (k3s / Docker Desktop) | Có kubectl | Triển khai `api` + `worker` + hạ tầng bằng manifest |

---

## 5. Nhật ký bài học
*(Ghi dần: khái niệm → hiểu thế nào → áp dụng ở đâu trong FinQuery.)*

| Ngày | Bài học | Áp dụng |
|---|---|---|
| 17/09 | Website chặn bot bằng cookie JS; HTML chứa sẵn dữ liệu mọi năm | Viết lại crawler không cần trình duyệt (QĐ-005) |
| 17/09 | "Chọn được chữ" ≠ "chữ đúng": PDF scan có lớp OCR kém | Đo chất lượng từng trang trước khi đưa vào RAG (QĐ-011) |
| 19/09 | Sáu ý tài chính cốt lõi; hợp nhất/riêng; kiểm toán/soát xét/tự lập; trái phiếu và cổ phiếu | Quy tắc mặc định hợp nhất; ưu tiên số đã kiểm toán |
| 19/09 | ConvFinQA là bộ đề kiểm tra (input, đáp án, các bước tính), không phải thuật toán | Cấu trúc bộ đánh giá FinQuery |
| 19/09 | Clean và Hexagonal cùng luật phụ thuộc; port chỉ tạo khi có ≥ 2 adapter | Kiến trúc QĐ-020 |
| 19/09 | Kafka: 3 broker là cho chịu lỗi ở production; phát triển thì 1 node KRaft | QĐ-019 v1.12 |
| 21/09 | Một tệp BCTC Việt Nam chỉ tương đương **Item 8** của Form 10-K; phần còn lại nằm ở BCTN, BCQT, ĐHĐCĐ, CBTT | Khẳng định quyết định nhiều nguồn (QĐ-004) |
| 21/09 | **Mã chỉ tiêu đổi theo chế độ kế toán** (TT200 → TT99): 270 năm 2023 là tổng tài sản, năm 2026 lại là tài sản dài hạn khác | Lược đồ phải dùng mã nội bộ + bảng ánh xạ, không dùng thẳng mã trên báo cáo |
| 21/09 | **Phân rã đệ quy có điều kiện dừng** (mục 3), và phải đi kèm lát cắt dọc mỏng để khỏi làm thừa | Cách chia việc cho toàn bộ phần còn lại của đề tài |
| 21/09 | Nghị quyết theo quy ước văn bản hành chính: Căn cứ → QUYẾT NGHỊ → các Điều kèm tỷ lệ biểu quyết | Mỗi Điều là ranh giới tách đoạn; tỷ lệ biểu quyết là trường có cấu trúc |
| 21/09 | Mẫu B09-DN: **doanh nghiệp được tự đánh số thứ tự thuyết minh** → nhận dạng phải khớp theo tên mục | `data/formats/bctc.json` v2 |
| 21/09 | Báo cáo giữa niên độ chỉ có **thuyết minh chọn lọc** theo VAS 27 | Thiếu mục ở báo cáo quý, bán niên **không** phải lỗi dữ liệu |
| 21/09 | Cột "Thuyết minh" trên B01/B02/B03 đã trỏ sẵn sang mục trong B09 | Liên kết có sẵn giữa con số và đoạn văn giải thích — dùng cho trích dẫn |
