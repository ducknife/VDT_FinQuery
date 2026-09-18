# Công nghệ — v1.2

*Ngày: 2026-09-17 · Thay cho [v1.1](cong-nghe_v1.1.md)*

## Thay đổi so với v1.1
- **Hạ Spring Boot từ 4.0.8 xuống 3.5.16** và **nâng Spring AI từ 1.1.7 lên 1.1.8** (QĐ-014 trong [quyet-dinh_v1.4.md](quyet-dinh_v1.4.md)). Đã build thử (`mvnw test-compile`) thành công.
- Đổi tên starter cho đúng Boot 3; xác nhận BOM Spring AI 1.1.8 có sẵn các module cần dùng (mục 2.1).

## Thay đổi của v1.1 so với v1.0 (giữ lại để tham khảo)
- **Lõi hệ thống viết bằng Java** (QĐ-013 trong [quyet-dinh_v1.3.md](quyet-dinh_v1.3.md)): RAG phân cấp, function calling SQL, nạp tài liệu, rerank (phần điều phối), đánh giá.
- Bỏ "sidecar Python chứa logic rerank". Python chỉ còn dùng cho **công cụ chạy ngoài hệ thống** và (nếu cần) **dịch vụ chạy mô hình không chứa logic**.
- Thêm thư viện Java cho phần nạp tài liệu và các phương án chạy mô hình.

## 1. Kiến trúc tổng quát

```
                          ┌──────────────────────────── core-service (Java, Spring Boot 3.5) ─────────────────────────┐
Người dùng ─► frontend ─► │ API ─► Router (DB / RAG / kết hợp)                                                        │
 (React TS)               │          ├─► Function calling SQL: tool → kiểm tra tham số → mẫu SQL → MariaDB → tính chỉ số │
                          │          └─► RAG phân cấp: chuẩn hoá → từ khoá → tìm vector → rerank 1 (100) → rerank 2 (20)│
                          │                             → sinh câu trả lời + trích dẫn (chia/hợp nhất nếu > 64k)        │
                          │ Nạp tài liệu: tải lên → Tika kiểm tra → hàng đợi Redis → worker → trích xuất / OCR           │
                          │               → tài liệu chuẩn → table-to-json → chunk → embed → MariaDB VECTOR              │
                          └──────────────┬───────────────────────────┬───────────────────────────────┬─────────────────┘
                                         ▼                           ▼                               ▼
                                  LLM API (chưa chốt)      Mô hình: embedding / reranker / OCR     MariaDB · Redis
                                                           (Java ONNX, API ngoài, hoặc container mô hình)
```

## 2. Stack chính

| Tầng | Công nghệ | Trạng thái | Ghi chú |
|---|---|---|---|
| Backend lõi | Java 21, **Spring Boot 3.5.16** (`backend/core-service`, `com.vdt.finq`) | ✅ Build được | Bản mới nhất của dòng 3.x (tra Maven Central ngày 17/09/2026) |
| Tích hợp LLM, tool calling, RAG | **Spring AI 1.1.8** (BOM) | ✅ Khớp | Spring AI 1.1.8 được build trên Spring Boot 3.5.15 (theo POM trên Maven Central) |
| CSDL | MariaDB + Flyway | Đã khai báo | Số liệu tài chính, sự kiện, trạng thái tài liệu |
| Vector store | MariaDB `VECTOR` (11.7+) | Dự kiến | Kiểm tra Spring AI có vector store cho MariaDB ở phiên bản đang dùng |
| Hàng đợi, cache | Redis | Dự kiến | Job nạp tài liệu; cache truy vấn |
| Frontend | React 19 + TypeScript + Vite | Đã khởi tạo | |
| Giám sát | Actuator + Micrometer Prometheus | Đã khai báo | Đo độ trễ và token cho phần đánh giá |
| Triển khai | Docker, EC2 | Dự kiến | |
| LLM | **Chưa chốt** (không dùng OpenAI) | ⏳ QĐ-010 | Phải có tool calling ổn định và có hỗ trợ trong Spring AI |

### 2.1 Module Spring AI 1.1.8 sẽ dùng (đã có trong BOM, **chưa thêm vào `pom.xml`**)

| Module | Dùng cho |
|---|---|
| `spring-ai-starter-model-<nhà cung cấp>` | LLM + tool calling. BOM có sẵn Anthropic, Google GenAI, DeepSeek, Mistral, Ollama… Chọn khi chốt QĐ-010 |
| `spring-ai-rag`, `spring-ai-advisors-vector-store` | Các bước RAG theo module (biến đổi truy vấn, truy xuất, hậu xử lý) |
| `spring-ai-starter-vector-store-mariadb` (`spring-ai-mariadb-store`) | Lưu và tìm vector trong MariaDB |
| `spring-ai-starter-model-transformers` (`spring-ai-transformers`) | Embedding chạy cục bộ bằng ONNX |
| `spring-ai-tika-document-reader`, `spring-ai-pdf-document-reader` | Tham khảo. Phần trích xuất chính vẫn tự viết để giữ số trang, khối bảng và cách trích (QĐ-012) |

Chỉ thêm module khi bắt đầu dùng, để build luôn gọn.

### 2.2 Thay đổi trong `pom.xml` khi hạ về Boot 3.5

| Boot 4.0.8 (cũ) | Boot 3.5.16 (mới) |
|---|---|
| `spring-boot-starter-webmvc` | `spring-boot-starter-web` |
| `spring-boot-starter-flyway` | `flyway-core` (+ `flyway-mysql` giữ nguyên) |
| `spring-boot-starter-{data-jpa,flyway,webmvc,websocket}-test` | `spring-boot-starter-test` |
| Cấu hình Lombok cho `maven-compiler-plugin` tách 2 execution | Gộp một `annotationProcessorPaths`; loại Lombok khỏi file jar |

## 3. Thư viện Java cho phần nạp tài liệu

| Việc | Thư viện |
|---|---|
| Nhận diện loại file thật, phát hiện mật khẩu/macro | Apache Tika |
| PDF văn bản số (chữ + vị trí, xét chất lượng từng trang) | Apache PDFBox |
| Bảng trong PDF văn bản số | Tabula-java (thử nghiệm) |
| DOCX / XLSX | Apache POI (XWPF / XSSF) |
| HTML | Jsoup |
| CSV | Apache Commons CSV |
| Chuyển trang PDF thành ảnh để OCR | PDFBox `PDFRenderer` |
| OCR (phương án thuần Java) | Tess4J + dữ liệu ngôn ngữ `vie` |

## 4. Chạy mô hình (chọn theo kết quả thử ở đầu tuần 2)

| Mô hình | Thuần Java | API ngoài | Tự host |
|---|---|---|---|
| Embedding | Spring AI `TransformersEmbeddingModel` (ONNX, CPU) | API của nhà cung cấp LLM | TEI / vLLM |
| Reranker tầng 1 | Cross-encoder nhỏ chạy ONNX (ONNX Runtime / DJL) | API Jina | TEI / vLLM |
| Reranker tầng 2 | Không khả thi trên CPU | API có host Qwen3-Reranker | vLLM (cần GPU) |
| OCR | Tess4J | LLM đọc ảnh | Container PaddleOCR / VietOCR |

Máy phát triển hiện tại chạy Windows 11 và **không có GPU NVIDIA**.

## 5. Python: công cụ chạy ngoài hệ thống (không thuộc lõi)

| Công cụ | File | Vai trò |
|---|---|---|
| Thu thập tài liệu | `backend/tai_bao_cao_ctr.py` | Tải và phân loại tài liệu từ web CTR |
| Đo chất lượng PDF | `backend/kiem_tra_pdf.py` | Phân loại so_hoa / ocr_kem / scan, dùng để lập kế hoạch OCR |
| PDF → chunk (bản mẫu) | `backend/chuyen_pdf_rag.py` | **Bản mẫu để viết lại bằng Java**, dùng để đối chiếu đầu ra |
| Kéo số liệu vnstock | (viết lại) | vnstock là thư viện Python. Xuất CSV rồi nạp vào DB bằng Flyway/Java |

Python 3.14.5. Các thư viện đang dùng: `requests`, `beautifulsoup4`, `pymupdf`, `pdfplumber`.

## 6. Việc cần làm về công nghệ
- [x] ~~Build thử `core-service` với Spring AI và chốt phiên bản~~: Boot 3.5.16 + Spring AI 1.1.8, `test-compile` thành công (17/09).
- [ ] Chạy thử ứng dụng: cần cấu hình datasource MariaDB, vì `FinqApplicationTests` (contextLoads) sẽ lỗi nếu chưa có DB.
- [ ] Kiểm tra Spring AI (phiên bản đã chốt) có: tool calling, các module RAG, vector store MariaDB, `TransformersEmbeddingModel`.
- [ ] Chốt nhà cung cấp LLM (QĐ-010).
- [ ] Thử OCR trên 5–10 trang BCTC mẫu: Tess4J `vie` / LLM đọc ảnh / PaddleOCR (QĐ-008).
- [ ] Chọn reranker tầng 1 và tầng 2 phù hợp với việc máy không có GPU.
- [ ] Chọn mô hình embedding đa ngôn ngữ có hỗ trợ tiếng Việt.
