# Công nghệ — v1.1

*Ngày: 2026-09-17 · Thay cho [v1.0](cong-nghe_v1.0.md)*

## Thay đổi so với v1.0
- **Lõi hệ thống viết bằng Java** (QĐ-013 trong [quyet-dinh_v1.3.md](quyet-dinh_v1.3.md)): RAG phân cấp, function calling SQL, nạp tài liệu, rerank (phần điều phối), đánh giá.
- Bỏ "sidecar Python chứa logic rerank". Python chỉ còn dùng cho **công cụ chạy ngoài hệ thống** và (nếu cần) **dịch vụ chạy mô hình không chứa logic**.
- Thêm thư viện Java cho phần nạp tài liệu và các phương án chạy mô hình.

## 1. Kiến trúc tổng quát

```
                          ┌──────────────────────────── core-service (Java, Spring Boot 4) ───────────────────────────┐
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
| Backend lõi | Java 21, **Spring Boot 4.0.8** (`backend/core-service`, `com.vdt.finq`) | Đã khởi tạo | |
| Tích hợp LLM, tool calling, RAG | **Spring AI** (BOM đang khai báo `1.1.7`) | ⚠️ **Phải kiểm tra** | Spring AI 1.x được viết cho Boot 3.x; Boot 4 nhiều khả năng cần Spring AI 2.x. Build thử trước khi viết code lõi |
| CSDL | MariaDB + Flyway | Đã khai báo | Số liệu tài chính, sự kiện, trạng thái tài liệu |
| Vector store | MariaDB `VECTOR` (11.7+) | Dự kiến | Kiểm tra Spring AI có vector store cho MariaDB ở phiên bản đang dùng |
| Hàng đợi, cache | Redis | Dự kiến | Job nạp tài liệu; cache truy vấn |
| Frontend | React 19 + TypeScript + Vite | Đã khởi tạo | |
| Giám sát | Actuator + Micrometer Prometheus | Đã khai báo | Đo độ trễ và token cho phần đánh giá |
| Triển khai | Docker, EC2 | Dự kiến | |
| LLM | **Chưa chốt** (không dùng OpenAI) | ⏳ QĐ-010 | Phải có tool calling ổn định và có hỗ trợ trong Spring AI |

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
- [ ] **Build thử `core-service` với Spring AI** và chốt phiên bản khớp Boot 4 (việc chặn đường cho tuần 3).
- [ ] Kiểm tra Spring AI (phiên bản đã chốt) có: tool calling, các module RAG, vector store MariaDB, `TransformersEmbeddingModel`.
- [ ] Chốt nhà cung cấp LLM (QĐ-010).
- [ ] Thử OCR trên 5–10 trang BCTC mẫu: Tess4J `vie` / LLM đọc ảnh / PaddleOCR (QĐ-008).
- [ ] Chọn reranker tầng 1 và tầng 2 phù hợp với việc máy không có GPU.
- [ ] Chọn mô hình embedding đa ngôn ngữ có hỗ trợ tiếng Việt.
