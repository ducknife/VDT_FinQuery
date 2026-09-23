# Học tập trong đề tài — v1.0

*Ngày: 2026-09-19 · Bản đầu tiên*

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
| 5. Ghi bài học | Sư phụ ghi, chủ đề tài bổ sung | Mục 4 của file này. Nguyên liệu cho phần "kiến thức thu được" trong báo cáo |

**Chia việc code:**

| Loại việc | Cách làm |
|---|---|
| Phần **mới hoàn toàn và khó** (tích hợp Spring AI, function calling, rerank 2 tầng, embedding, cấu hình Kafka/K8s lần đầu) | **Sư phụ viết mẫu** có chú thích tiếng Việt, giải thích từng khối. Chủ đề tài đọc, chạy thử và hỏi |
| Phần **lặp lại theo mẫu** (thêm tool SQL thứ 2, 3…; thêm adapter; thêm endpoint; migration Flyway) | **Chủ đề tài tự viết** theo mẫu đã có, sư phụ review |
| Phần **quen thuộc với dân Java** (REST, JPA, test) | Chủ đề tài viết, sư phụ review |
| Khi **trễ tiến độ** | Sư phụ viết nhiều hơn để giữ tỷ lệ hoàn thiện, ghi rõ trong log |

Mục tiêu: đến cuối đề tài, chủ đề tài **tự giải thích được mọi dòng code của lõi** trước hội đồng.

## 3. Bản đồ kỹ năng

| Kỹ năng | Hiện tại | Học khi nào (gắn với kế hoạch) | Đã đạt khi |
|---|---|---|---|
| Hexagonal + Spring Modulith | Mới biết khái niệm | Tuần 1–2 (dựng khung project) | Tự thêm được một adapter mới đúng ranh giới |
| Docker / Compose | Đã có công cụ | Tuần 1–2 | Tự viết `compose.yaml` dựng MariaDB, Redis, Kafka, Keycloak |
| Kafka (KRaft) | Chưa | Tuần 2 (pipeline nạp tài liệu) | Giải thích được topic, partition, consumer group, offset; làm thí nghiệm 3 broker |
| Nạp tài liệu (PDFBox, POI, OCR, chunk, table-to-json) | Chưa | Tuần 2 | Nạp được một BCTC thành chunk có số trang |
| Embedding + tìm vector (MariaDB VECTOR) | Chưa | Tuần 2 | Giải thích được embedding và độ tương đồng cosine; truy vấn top-k |
| Spring AI + LLM + **function calling** | Chưa | Tuần 3 | Tự thêm một tool SQL mới |
| Rerank 2 tầng (ViRanker + tầng 2) | Đã hiểu ý tưởng paper | Tuần 4 | Giải thích được cross-encoder khác bi-encoder thế nào; đo được NDCG |
| Redis (bộ nhớ hội thoại, cache) | Chưa | Tuần 3–4 | Hội thoại nhiều lượt chạy được |
| Đánh giá (bộ câu hỏi, baseline, Welch t-test) | Đã đọc paper | Tuần 5 | Chạy được bảng so sánh 3 phương án |
| Keycloak / Spring Security | Chưa | Tuần 5 | Đăng nhập SSO, phân 3 vai trò |
| Kubernetes (k3s / Docker Desktop) | Có kubectl | Tuần 5–6 | Triển khai `api` + `worker` + hạ tầng bằng manifest |

## 4. Nhật ký bài học
*(Ghi dần: khái niệm → hiểu thế nào → áp dụng ở đâu trong FinQuery.)*

| Ngày | Bài học | Áp dụng |
|---|---|---|
| 17/09 | Website chặn bot bằng cookie JS; HTML chứa sẵn dữ liệu mọi năm | Viết lại crawler không cần trình duyệt (QĐ-005) |
| 17/09 | "Chọn được chữ" ≠ "chữ đúng": PDF scan có lớp OCR kém | Đo chất lượng từng trang trước khi đưa vào RAG (QĐ-011) |
| 19/09 | Sáu ý tài chính cốt lõi; hợp nhất/riêng; kiểm toán/soát xét/tự lập; trái phiếu và cổ phiếu | Quy tắc mặc định hợp nhất; ưu tiên số đã kiểm toán |
| 19/09 | ConvFinQA là bộ đề kiểm tra (input, đáp án, các bước tính), không phải thuật toán | Cấu trúc bộ đánh giá FinQuery |
| 19/09 | Clean và Hexagonal cùng luật phụ thuộc; port chỉ tạo khi có ≥ 2 adapter | Kiến trúc QĐ-020 |
| 19/09 | Kafka: 3 broker là cho chịu lỗi ở production; phát triển thì 1 node KRaft | QĐ-019 v1.12 |
