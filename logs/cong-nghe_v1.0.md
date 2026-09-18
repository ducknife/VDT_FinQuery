# Công nghệ — v1.0

*Ngày: 2026-09-17 · Bản đầu tiên*

## 1. Stack chính

| Tầng | Công nghệ | Trạng thái | Lý do |
|---|---|---|---|
| Backend | Java 21, **Spring Boot 4.0.8** (`backend/core-service`, package `com.vdt.finq`) | Đã khởi tạo | Theo yêu cầu đề tài; hệ sinh thái mạnh cho API và bảo mật |
| Tích hợp LLM | **Spring AI** (BOM đang khai báo `1.1.7`) | ⚠️ Cần kiểm tra | Spring AI 1.x được viết cho Boot 3.x; Boot 4 nhiều khả năng cần Spring AI 2.x. Phải build thử trước khi thêm starter |
| Frontend | React 19 + TypeScript + Vite (`frontend/`) | Đã khởi tạo | Theo yêu cầu đề tài |
| CSDL | MariaDB + Flyway | Đã khai báo | Số liệu có cấu trúc, migration có phiên bản |
| Vector store | Dự kiến kiểu `VECTOR` của MariaDB 11.7+ | Chưa chốt | Giữ nguyên stack, không cần thêm Qdrant hay pgvector |
| Cache | Redis | Dự kiến | Cache kết quả truy vấn và embedding |
| Triển khai | Docker, EC2 | Dự kiến | |
| Giám sát | Actuator + Micrometer Prometheus | Đã khai báo | Đo độ trễ và token, phục vụ đánh giá |
| LLM | **Chưa chốt** (không dùng OpenAI) | Mở | Ảnh hưởng đến cách gọi hàm và cách tính chi phí khi đánh giá |
| Reranker | Tầng 1 và tầng 2 theo paper (jina-reranker-v3 → Qwen3-Reranker) | Chưa chốt | Java không chạy trực tiếp được → sidecar Python (FastAPI) hoặc gọi API |

## 2. Công cụ xử lý dữ liệu (Python, chạy ngoài hệ thống)

Python chỉ dùng cho các việc **chạy một lần hoặc định kỳ** (thu thập, tiền xử lý), không nằm trong luồng phục vụ người dùng.

| Công cụ | File | Thư viện | Ghi chú |
|---|---|---|---|
| Thu thập tài liệu | `backend/tai_bao_cao_ctr.py` | `requests`, `beautifulsoup4` | Bản 2: bỏ Playwright (QĐ-005) |
| Đánh giá chất lượng PDF | `backend/kiem_tra_pdf.py` | `pymupdf` | Phân loại so_hoa / ocr_kem / scan |
| PDF → chunk cho RAG | `backend/chuyen_pdf_rag.py` | `pdfplumber` / `pymupdf` | Đã có từ trước, **không OCR**. Cần cập nhật để đọc `manifest.csv` và cấu trúc thư mục mới |
| OCR tiếng Việt | – | Chưa chọn | Máy hiện chưa có Tesseract/PaddleOCR và không có GPU NVIDIA (QĐ-008) |
| Kéo số liệu BCTC | (vnstock) | `vnstock` | Script cũ `keo_du_lieu_vnstock (2).py` đang ở trạng thái đã xoá trong git; cần viết lại |

Môi trường: Windows 11, Python 3.14.5.

## 3. Việc cần làm về công nghệ
- [ ] Build thử `core-service` với Spring AI và chốt phiên bản khớp Boot 4.
- [ ] Chốt nhà cung cấp LLM, phải hỗ trợ tool/function calling và có bảng giá rõ ràng.
- [ ] Chốt công cụ OCR, so sánh chất lượng trên 5–10 trang BCTC mẫu.
- [ ] Chốt embedding model đa ngôn ngữ có hỗ trợ tiếng Việt, và phiên bản MariaDB có `VECTOR`.
