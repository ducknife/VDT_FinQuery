# Sổ quyết định — v1.0

*Ngày: 2026-09-17 · Bản đầu tiên*

Mỗi quyết định ghi theo khung: **Bối cảnh → Quyết định → Lý do → Hệ quả**. Trạng thái có thể là ✅ đã chốt, 🟡 tạm thời hoặc ⏳ chưa chốt.

---

### QĐ-001 · Một monorepo duy nhất ✅
*(2026-09-14)*
- **Bối cảnh:** `backend/core-service` từng bị tạo thành một git repo lồng bên trong repo chính.
- **Quyết định:** gộp thành một repo `ducknife/VDT_FinQuery`. Không chạy `git init` trong các thư mục con.
- **Lý do:** một lịch sử commit duy nhất, dễ nộp bài và dễ review.

### QĐ-002 · Doanh nghiệp mẫu là Viettel Construction (CTR), tài chính doanh nghiệp mảng A–F ✅
- **Bối cảnh:** hai paper gốc dùng báo cáo 10-K của doanh nghiệp Mỹ.
- **Quyết định:** chuyển sang doanh nghiệp Việt Nam, lấy CTR làm mẫu, với 6 mảng A–F (xem [pham-vi_v1.0.md](pham-vi_v1.0.md)).
- **Lý do:** gắn với bối cảnh Viettel; website CTR công bố đầy đủ BCTC, BCTN và nghị quyết.
- **Hệ quả:** phải tự xây dựng phần chuẩn hoá tiếng Việt và bảng tra mã cổ phiếu.

### QĐ-003 · Khoảng thời gian 2019 → 2026 ✅
- **Bối cảnh:** website có tài liệu từ 2015. Cần cân đối giữa độ dài chuỗi thời gian và công xử lý.
- **Quyết định:** lấy 2019–2026 (năm 2026 đến bán niên). Khi phát triển pipeline thì chạy trước trên 2021–2025, sau đó mới mở rộng.
- **Lý do:**
  - Mảng E cần bộ ba tài liệu (nghị quyết năm N, BCTC năm N, BCTN năm N), và khoảng này có đủ bộ ba cho 7 năm.
  - Khoảng 28 quý là đủ cho câu hỏi xu hướng.
  - Số đầu kỳ 2019 đã có trong cột so sánh của BCTC 2019.
  - Tài liệu 2015–2018 lộn xộn và nhiều bản scan.
- **Hệ quả:** 74 mục bị bỏ qua vì nằm ngoài khoảng năm. Muốn mở rộng thì chỉ cần chạy lại tool với `--tu`.

### QĐ-004 · Bổ sung nguồn: CBTT, báo cáo quản trị, điều lệ, bản cáo bạch, dữ liệu web ✅
- **Bối cảnh:** ba nguồn ban đầu (BCTC, BCTN, ĐHĐCĐ) chỉ có *phương án* phát hành và cổ tức, không có *sự kiện thực tế* như kết quả phát hành, ngày chốt quyền hay thoái vốn.
- **Quyết định:** thêm nguồn 04–08. Với CBTT thì **lọc theo từ khoá**; chỉ giữ cổ tức, phát hành/tăng vốn, đầu tư/thoái vốn, thế chấp/vay, kế hoạch kinh doanh, đính chính và nghị quyết.
- **Lý do:** có dữ liệu sự kiện thì mới tạo được câu hỏi kết hợp kiểu "ĐHĐCĐ duyệt X, thực tế đã làm Y chưa". Đây là điểm nổi bật của đề tài.
- **Giới hạn đặt ra:** hoàn tất trong ngày 17/09 (đã đạt). Dữ liệu sự kiện (khoảng 30–50 bản ghi) sẽ **nhập tay** vào DB thay vì viết code trích tự động.

### QĐ-005 · Thu thập bằng `requests` + BeautifulSoup, bỏ Playwright ✅
- **Bối cảnh:** tool cũ dùng Playwright click từng ô năm, nhưng chỉ lấy được 89/172 file BCTC.
- **Phát hiện:**
  1. HTML chứa sẵn bảng của **mọi năm** (chỉ ẩn bằng CSS), nên việc click không có tác dụng lọc.
  2. Lớp chặn bot chỉ là một cookie tĩnh do JS đặt.
  3. Các trang "Thông tin cổ đông" lấy dữ liệu từ API JSON công khai.
- **Quyết định:** viết lại tool bằng `requests`: tự đặt cookie, đọc năm từ class `y-YYYY`, tải song song 6 luồng, và gọi thẳng API cho dữ liệu dạng bảng.
- **Lý do:** nhanh hơn, không cần trình duyệt, lấy đủ dữ liệu, dễ chạy lại.

### QĐ-006 · Dữ liệu thô đặt ở `data/raw/CTR/`, không đưa PDF lên git ✅
- **Bối cảnh:** trước đây PDF nằm trong `backend/core-service/docs/`, tức bên trong mã nguồn service.
- **Quyết định:** chuyển ra `data/raw/CTR/` ở gốc repo. PDF (khoảng 2,4 GB) đưa vào `.gitignore`; manifest và bảng chất lượng vẫn đưa lên git.
- **Lý do:** tách dữ liệu khỏi mã nguồn, tránh làm repo phình to. Manifest đủ để tái lập dữ liệu.

### QĐ-007 · Phân loại theo tiêu đề web; lưu mọi mục kể cả mục bỏ qua vào manifest ✅
- **Lý do:**
  - Tên file trên web không đáng tin (`BCTC_Mẹ.pdf`, `Giai-trinh.pdf`, trùng tên giữa các kỳ).
  - Lưu cả mục bị bỏ qua kèm lý do giúp kiểm tra lại, và giúp phần báo cáo giải thích được vì sao dữ liệu có quy mô như vậy.
- **Hệ quả:** sai sót của website được sửa bằng bảng `SUA_TAY` có ghi bằng chứng (hiện có 1 mục: BCTC "Quý 4/2026" thực chất là Q4/2025).

### QĐ-008 · OCR tiếng Việt trước khi chunk ⏳
- **Bối cảnh:** 286/314 PDF (khoảng 6.700 trang) có lớp chữ kém hoặc không có chữ, trong đó có toàn bộ BCTC.
- **Hướng đi:** thêm bước OCR trước `chuyen_pdf_rag.py` và **không tin lớp chữ có sẵn**.
- **Chưa chốt** công cụ: Tesseract `vie`, PaddleOCR, hoặc LLM có thị giác. Tiêu chí chọn:
  - độ chính xác của dấu tiếng Việt và của số,
  - khả năng giữ cấu trúc bảng,
  - thời gian xử lý trên máy không có GPU NVIDIA,
  - chi phí.
- **Cách chọn:** thử trên 5–10 trang BCTC mẫu ở đầu tuần 2.

### QĐ-009 · Số liệu có cấu trúc lấy từ vnstock, không trích từ PDF 🟡
- **Lý do:** PDF BCTC đều là bản scan, trích số liệu từ đó rủi ro cao.
- **Hệ quả:**
  - PDF chỉ dùng cho RAG (thuyết minh, giải trình) và làm bằng chứng trích dẫn.
  - Phải kiểm tra vnstock có đủ BCTC hợp nhất theo quý cho CTR từ 2019 hay không.
  - Phải xử lý các số liệu đã được đính chính hoặc trình bày lại.

### QĐ-010 · Nhà cung cấp LLM ⏳
- **Bối cảnh:** không dùng OpenAI như trong paper.
- **Tiêu chí:** hỗ trợ tool/function calling ổn định, hiểu tiếng Việt tốt, có bảng giá token rõ ràng để tính chi phí khi đánh giá, và có SDK hoặc Spring AI hỗ trợ.
