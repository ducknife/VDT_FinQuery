# Kế hoạch — v1.1

*Ngày: 2026-09-21 · Thay cho [v1.0](ke-hoach_v1.0.md) · Hạn nộp: 31/10/2026*

## Thay đổi so với v1.0
- Tổ chức lại theo **bốn khối nền** mà mentor yêu cầu kiểm định trước: **DB core → Model → Inference → Measure + Test**.
- Bỏ cách chia thuần theo tuần; mỗi khối có **định nghĩa hoàn thành** và **cách mentor kiểm chứng**.
- Áp dụng QĐ-023 (lõi trước, hạ tầng bổ trợ sau) và QĐ-022 (tiền xử lý → xử lý → đánh giá).

---

## 1. Nguyên tắc
1. **Làm một lát cắt mỏng chạy thông suốt trước**, trên **tập dữ liệu nhỏ**, rồi mới mở rộng ra toàn bộ 314 tài liệu.
2. Mỗi khối nền phải **kiểm chứng được bằng kết quả cụ thể**, không phải bằng lời mô tả.
3. Hạ tầng bổ trợ (Hexagonal đầy đủ, Kafka, Redis, Kubernetes, Keycloak) **chỉ làm sau khi bốn khối nền được nghiệm thu**.

---

## 2. Bốn khối nền

### Khối 1 — DB core
| | |
|---|---|
| **Nội dung** | Lược đồ MariaDB: chỉ tiêu tài chính theo **mã nội bộ** + bảng ánh xạ mã theo chế độ kế toán (TT200 và TT99), kế hoạch kinh doanh, cổ tức, sự kiện phát hành, danh mục tài liệu, danh mục chunk, nhật ký kiểm toán. Migration bằng Flyway. Nạp số liệu cho công ty mẫu. Tạo collection trong Qdrant kèm cấu trúc payload |
| **Dữ liệu dùng** | Số liệu BCTC theo quý và năm; 20–30 tài liệu mẫu (ưu tiên nhóm đọc được chữ) |
| **Hoàn thành khi** | Truy vấn được **5 chỉ tiêu × 3 năm** và số liệu **khớp với PDF gốc khi đối chiếu tay**; mã chỉ tiêu 2019–2025 (TT200) và 2026 (TT99) cùng trả về đúng một mã nội bộ |
| **Mentor kiểm định** | Sơ đồ lược đồ + bảng đối chiếu số liệu hệ thống với PDF |

### Khối 2 — Model
| | |
|---|---|
| **Nội dung** | Chọn và chạy thử bốn nhóm mô hình: **OCR**, **embedding**, **reranker** (tầng 1 ViRanker, tầng 2), **mô hình ngôn ngữ**. Mỗi nhóm là một cổng có thể thay bằng cấu hình |
| **Hoàn thành khi** | Mỗi mô hình chạy được từ Java trên một mẫu nhỏ; có **bảng đo sơ bộ**: chất lượng, độ trễ, chi phí; chốt lựa chọn mặc định |
| **Mentor kiểm định** | Bảng so sánh mô hình kèm lý do chọn |
| **Ghi chú** | Đây cũng là lúc trả lời câu hỏi mô hình chuyên tiếng Việt có hơn mô hình đa ngôn ngữ không (QĐ-024) |

### Khối 3 — Inference
| | |
|---|---|
| **Nội dung** | Chuỗi xử lý chạy thông: câu hỏi → chuẩn hoá → định tuyến → (nhánh cơ sở dữ liệu bằng function calling · nhánh tài liệu với rerank hai tầng) → câu trả lời kèm trích dẫn → trường hợp "không xác định" |
| **Dữ liệu dùng** | Tập nhỏ của Khối 1 |
| **Hoàn thành khi** | **10 câu hỏi mẫu** (đủ cả ba nhánh) chạy ra câu trả lời đúng, **có trích dẫn tới số trang** |
| **Mentor kiểm định** | Demo trực tiếp 10 câu, kèm ảnh chụp câu trả lời và nguồn |

### Khối 4 — Measure + Test
| | |
|---|---|
| **Nội dung** | Ghi nhật ký kiểm toán đầy đủ trường đo (độ trễ từng chặng, token vào/ra, chi phí); công cụ chạy bộ test theo lô; bộ câu hỏi chuẩn bản đầu (khoảng 40 câu); **một hệ thống đối chứng** (text-to-SQL) |
| **Hoàn thành khi** | Chạy được **40 câu × 2 hệ thống** và xuất ra bảng: độ chính xác, độ chính xác trích dẫn, độ trễ (p50, p95), token, chi phí |
| **Mentor kiểm định** | Bảng kết quả so sánh và tệp nhật ký kiểm toán |

**Mốc quan trọng:** bốn khối trên xong thì hệ thống đã **chạy thông suốt và đo được**. Đây là lúc mentor nghiệm thu nền, trước khi mở rộng.

---

## 3. Sau khi nền được nghiệm thu

| Nhóm việc | Nội dung |
|---|---|
| **Mở rộng dữ liệu** | OCR theo thứ tự ưu tiên (BCTC năm và bán niên → nghị quyết ĐHĐCĐ → CBTT cổ tức, phát hành); nạp toàn bộ 314 tài liệu; hoàn thiện hồ sơ cấu trúc tài liệu (QĐ-025) |
| **Hoàn thiện xử lý** | Nhánh kết hợp, hội thoại nhiều lượt, quản lý ngữ cảnh dài, nhóm câu an toàn |
| **Hoàn thiện đánh giá** | Bộ câu hỏi đủ 100–150 câu; thêm hệ thống đối chứng RAG cơ bản; so sánh mô hình (T5a, T5b, T6); đo với người dùng thật (T8) |
| **Giao diện** | Khung chat, hiển thị trích dẫn, trang thống kê chi phí |
| **Hạ tầng bổ trợ** (QĐ-023) | Dựng lại theo Hexagonal; Kafka cho pipeline nạp tài liệu; Redis cho bộ nhớ hội thoại và cache; Kubernetes; Keycloak |
| **Báo cáo** | Phân tích kết quả, kiểm định Welch, viết báo cáo và slide |

---

## 4. Mốc thời gian dự kiến

| Thời gian | Trọng tâm |
|---|---|
| 22–27/9 | Khối 1 (DB core) và Khối 2 (Model) |
| 28/9 – 4/10 | Khối 3 (Inference) |
| 5–11/10 | Khối 4 (Measure + Test) → **nghiệm thu nền với mentor** |
| 12–18/10 | Mở rộng dữ liệu và hoàn thiện xử lý |
| 19–25/10 | Hoàn thiện đánh giá, giao diện, đo với người dùng thật |
| 26–31/10 | Hạ tầng bổ trợ nếu kịp, viết báo cáo và slide |

---

## 5. Thứ tự cắt giảm khi chậm tiến độ
1. Kubernetes: chỉ viết manifest, demo bằng Docker Compose.
2. Kafka: giữ cách chạy đồng bộ.
3. Redis: giữ bộ nhớ trong ứng dụng.
4. Dựng lại theo Hexagonal đầy đủ: giữ cấu trúc gói hiện có, chỉ giữ ba cổng chính.
5. Dữ liệu 2019–2020 (toàn bộ BCTC hai năm này là bản quét).
6. Điều lệ, quy chế, bản cáo bạch: không OCR.

**Không cắt:** bốn khối nền, bộ câu hỏi chuẩn, và phần đo hiệu quả với người dùng thật.
