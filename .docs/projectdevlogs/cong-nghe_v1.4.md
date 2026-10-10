# Công nghệ — v1.4

*Ngày: 2026-09-28 · Thay cho [v1.3](cong-nghe_v1.3.md)*

## Thay đổi so với v1.3

### 1. Cơ sở dữ liệu quan hệ: **MySQL** thay MariaDB

| | Cũ | Mới |
|---|---|---|
| Trình điều khiển | `org.mariadb.jdbc:mariadb-java-client` | **`com.mysql:mysql-connector-j`** |
| Flyway | `flyway-mysql` | **`flyway-mysql`** (không đổi — dùng chung cho MySQL và MariaDB) |

Vai trò không đổi: MySQL giữ **số liệu chỉ tiêu, sự kiện, danh mục, nhật ký kiểm toán**; Qdrant giữ vector.

Lưu ý khi viết migration: MySQL và MariaDB đã tách nhánh khá xa. Tránh cú pháp riêng của
MariaDB (`RETURNING`, kiểu `VECTOR`, `SEQUENCE`). Giữ SQL ở mức chuẩn để sau này đổi lại không đau.

### 2. Nhà cung cấp mô hình: **Viettel AI** (đóng QĐ-008 và QĐ-010)

Đã đo thật trên `https://viettelai.vn/model-as-a-service` — API tương thích OpenAI chạy trên LiteLLM,
nên dùng thẳng starter OpenAI của Spring AI, không cần viết adapter.

| Vai trò | Mô hình | Số đo |
|---|---|---|
| OCR | `Qwen3-VL-32B-Instruct-FP8` | 6.690 trang, 100% xong |
| Function calling | `DeepSeek-V4-Flash` | 1,06s · trả đúng `tool_calls` |
| Embedding | `Qwen3-Embedding-4B` | **2560 chiều** · 1,13s |
| Rerank tầng 1 | `BAAI/bge-reranker-v2-m3` | 0,84s · điểm 0,899 vs 0,001 |
| Rerank tầng 2 | *(chưa chốt)* | |

Năm vai trò đã tách thành năm biến môi trường riêng (`OCR_MODEL`, `EMBEDDING_MODEL`,
`RANKER_LAYER_1ST_MODEL`, `RANKER_LAYER_2ND_MODEL`, `REASONING_MODEL`) — đúng tinh thần
cổng mô hình của QĐ-023: đổi mô hình chỉ bằng đổi cấu hình.

### 3. Cấu hình đọc từ `.env`

`application.yml` dùng `spring.config.import: optional:file:.env[.properties]`.
Cả hai tệp `.env` (gốc dự án và `backend/core-service`) đều đã nằm trong `.gitignore`;
đã kiểm lịch sử git: **khoá chưa từng bị commit**. Có `.env.example` kèm tên biến.

### 4. Cấu trúc gói đã dựng

```
com.vdt.finq
├── common/{exception, validation}
├── config
├── factory
├── modules
│   ├── agent/{context, loop, prompt, tools}
│   ├── dbquery/st8ments
│   └── hierarchicalrag/{preprocessing/ocr, preretrieval, retrieval, generation}
└── utils
```

Thêm `pom.xml` gộp ở gốc monorepo (`com.vdt:finquery-monorepo`, packaging `pom`,
module `backend/core-service`) — cần cho bộ công cụ Java của VS Code, vì nó tìm
`pom.xml` ngay tại thư mục làm việc.

## Còn treo

- [ ] Mô hình rerank tầng 2
- [ ] Bảng ánh xạ mã chỉ tiêu `(chế độ, mẫu biểu, mã) → mã nội bộ` — chặn cả lược đồ lẫn script trích
- [ ] Sổ công thức (cơ sở tính: cuối kỳ hay bình quân)
- [ ] Dựng MySQL bằng Docker Compose, viết migration Flyway đầu tiên
