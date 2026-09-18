# Sổ quyết định — v1.4

*Ngày: 2026-09-17 · Thay cho [v1.3](quyet-dinh_v1.3.md)*

## Thay đổi so với v1.3
- **Thêm QĐ-014:** hạ Spring Boot xuống 3.5.16 và dùng Spring AI 1.1.8.
- QĐ-013: bỏ ghi chú "phải kiểm tra Spring AI với Boot 4" (không còn dùng Boot 4).

## Thay đổi của v1.3 so với v1.2 (giữ lại để tham khảo)
- **QĐ-013 viết lại và chốt ✅.** Yêu cầu của chủ đề tài: **phần lõi (RAG phân cấp và function calling SQL) phải viết bằng Java.** Python chỉ còn hai vai trò:
  - các công cụ chạy ngoài hệ thống (thu thập dữ liệu, đo chất lượng),
  - *có thể* là **dịch vụ chạy mô hình không chứa logic** (OCR; reranker/embedding nếu tự host).
- **QĐ-011:** worker nạp tài liệu chuyển từ Python sang **Java**. OCR là dịch vụ bên ngoài, chỉ trả kết quả thô. Chuyển trạng thái sang ✅ (kiến trúc đã chốt; công cụ OCR vẫn chờ QĐ-008).
- **QĐ-012:** bộ trích xuất đổi sang thư viện Java (PDFBox, POI, Jsoup, Commons CSV). Tài liệu chuẩn được sinh ra ở Java.

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
- **Bối cảnh:** website CTR có tài liệu từ 2015. Nếu lấy quá ngắn thì không hỏi được xu hướng. Nếu lấy quá dài thì tốn công OCR, chunk và kiểm tra, trong khi giá trị thêm vào không nhiều.
- **Quyết định:** lấy **năm tài chính 2019 → 2025 (7 năm đủ), cộng thêm 2026 đến bán niên**.
  - **Vùng lõi 2021–2025:** dùng khi phát triển và kiểm thử pipeline.
  - **Vùng mở rộng 2019–2020:** nạp sau.

**Số tài liệu theo năm** (đếm trên `manifest.csv`, tính cả mục bị bỏ qua):

| Nguồn | 2015 | 2016 | 2017 | 2018 | **2019** | **2020** | **2021** | **2022** | **2023** | **2024** | **2025** | **2026** |
|---|---|---|---|---|---|---|---|---|---|---|---|---|
| BCTC | 8 | 8 | 11 | 14 | 18 | 20 | 19 | 17 | 17 | 14 | 17 | 9 |
| BCTN | – | – | 1 | 1 | 1 | 1 | 1 | 1 | 2 | 2 | 4 | – |
| ĐHĐCĐ | – | – | 2 | 2 | 4 | 2 | 24 | 24 | 22 | 29 | 23 | 11 |
| CBTT | 4 | 5 | 7 | 11 | 28 | 37 | 37 | 23 | 31 | 18 | 14 | 27 |
| Báo cáo quản trị | – | – | – | – | – | 1 | 2 | 2 | 2 | 2 | 2 | 2 |

- **Lý do chọn mốc đầu là 2019:**
  1. **BCTC đầy đủ mọi kỳ từ 2019.** Từ 2019 có đủ Q1–Q4, bán niên và năm, cả hợp nhất lẫn riêng. Trước 2019 bị thiếu kỳ: 2015 thiếu Q2 và Q4; 2016 thiếu Q4; 2017 Q1 chỉ có một bản. Câu hỏi theo quý (mảng B, C) cần chuỗi liền mạch.
  2. **Số CBTT tăng rõ từ 2019** (14–37 văn bản mỗi năm trong giai đoạn 2019–2026, so với 4–11 văn bản mỗi năm trong 2015–2018). Mảng A, D, F cần dữ liệu sự kiện (phát hành, cổ tức, đầu tư), nên trước 2019 gần như không có gì để hỏi.
  3. **Mảng E cần bộ ba tài liệu cho mỗi năm:** nghị quyết ĐHĐCĐ năm N (kế hoạch), BCTC năm N (thực hiện) và BCTN năm N (giải trình). Giai đoạn 2019–2025 có **đủ bộ ba cho cả 7 năm**, tạo được 7 cặp kế hoạch–thực hiện. 7 cặp là đủ cho câu hỏi E và cho một phần đánh giá.
  4. **Không mất số liệu năm 2018.** BCTC năm 2019 đã có cột số liệu so sánh 31/12/2018, nên vẫn tính được các chỉ số cần số bình quân của năm 2019 (ROE, ROA, vòng quay).
  5. **Lịch sử cổ tức và tăng vốn 2015–2021 đã có sẵn** trong `08_du_lieu_web` (lấy từ API, không cần PDF). Lấy thêm PDF trước 2019 chỉ tốn công mà không thêm thông tin cho mảng F.
  6. **Chuỗi đủ dài và đa dạng bối cảnh:** 7 năm (28 quý) cộng 2 kỳ của 2026, trải qua giai đoạn trước COVID (2019), COVID (2020–2021), chuyển sang niêm yết trên HOSE (02/2022) và giai đoạn gần đây. Đủ để hỏi xu hướng, so sánh cùng kỳ và so sánh nhiều năm.
  7. **Chi phí xử lý:** không lấy 2015–2018 thì bỏ được 74 mục, tức là bớt được khối lượng OCR đáng kể (xem QĐ-011).
- **Lý do lấy đến 2026:**
  - Có dữ liệu mới nhất (Q1, Q2 và bán niên 2026; ĐHĐCĐ 2026 với kế hoạch năm 2026; cổ tức 2025 trả trong 2026) cho các câu hỏi dạng "hiện tại / gần nhất".
  - Kế hoạch 2026 mới **thực hiện được một phần**. Đây là tình huống thực tế tốt để kiểm tra hệ thống có trả lời đúng "mới có số liệu đến 30/06/2026" hay không.
- **Điểm yếu đã biết:**
  - **2019–2020:** toàn bộ BCTC là ảnh scan. Tài liệu ĐHĐCĐ chỉ có nghị quyết và biên bản (tờ trình, báo cáo HĐQT/Ban điều hành chỉ có từ 2021). Báo cáo quản trị bắt đầu từ 2020. Vì vậy giai đoạn này **xếp thứ 3 trong danh sách cắt giảm** ([ke-hoach_v1.0.md](ke-hoach_v1.0.md)).
  - **2026:** chưa có BCTC năm và BCTN.
- **Hệ quả:** bỏ qua 74 mục nằm ngoài khoảng năm. Muốn mở rộng thì chạy lại `tai_bao_cao_ctr.py --tu 2015`.

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

### QĐ-008 · OCR tiếng Việt trước khi chunk ⏳ *(v1.1: gộp vào QĐ-011)*
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

### QĐ-011 · OCR là thành phần của hệ thống: pipeline nạp tài liệu ✅
*(2026-09-17 · chốt kiến trúc ở v1.3; công cụ OCR vẫn chờ thử nghiệm — QĐ-008)*
- **Bối cảnh:**
  - 286/314 tài liệu hiện có không đọc chữ trực tiếp được.
  - Sau này người dùng sẽ nạp thêm tài liệu mới (BCTC quý mới, nghị quyết mới…), và rất có thể cũng là bản scan.
  - Nếu OCR chỉ là script chạy tay một lần thì hệ thống không tự xử lý được tài liệu mới.
- **Quyết định:** xây **pipeline nạp tài liệu (ingestion)** như một thành phần của hệ thống, và OCR là một bước bên trong. Bộ dữ liệu 314 file hiện có cũng đi qua **đúng pipeline này** ở chế độ chạy lô (CLI), nên chỉ có một luồng code.
- **Luồng xử lý:**
  1. `core-service` (Java) nhận file tải lên và kiểm tra an toàn: loại file theo *magic bytes* (không tin đuôi file), dung lượng tối đa, số trang tối đa.
  2. `core-service` lưu file, tạo bản ghi `tai_lieu` với trạng thái `CHO_XU_LY`, rồi đẩy job vào hàng đợi Redis.
  3. **Worker Java** (trong `core-service`) nhận job và kiểm tra chất lượng **theo từng trang, không theo cả file**. Một PDF có thể lẫn trang văn bản số với trang scan (ví dụ trang chữ ký, trang có dấu). Dùng lại tiêu chí của `kiem_tra_pdf.py`, xếp mỗi trang vào một trong ba nhánh:
     - `so_hoa`: đọc lớp chữ có sẵn,
     - `ocr_kem`: bỏ lớp chữ có sẵn, OCR lại,
     - `scan`: OCR.
  4. Trang cần OCR được gửi sang **dịch vụ OCR** (QĐ-013). Dịch vụ này chỉ trả về kết quả thô: chữ, vị trí, lưới bảng, độ tin cậy.
     Sau đó worker Java tạo "tài liệu chuẩn" (QĐ-012), chuyển bảng sang JSON, chunk và embed để nạp vào vector store.
  5. Worker cập nhật trạng thái (`DA_XU_LY` / `CAN_KIEM_TRA` / `LOI`). Trang nào có **độ tin cậy OCR thấp** thì đánh dấu `CAN_KIEM_TRA` để người kiểm tra lại, và RAG phải ghi rõ nguồn là OCR khi trích dẫn.
- **Lý do:**
  - Hệ thống tự xử lý được tài liệu mới, không phụ thuộc thao tác tay.
  - Chất lượng OCR được theo dõi được, phục vụ tiêu chí *độ chính xác trích dẫn*.
  - Việc kiểm tra file tải lên nằm ở Java, đúng tinh thần "an toàn" của đề tài.
  - ~~Đặt nạp tài liệu và rerank trong một dịch vụ Python (`ml-service`).~~ **Bỏ ở v1.3**: logic nạp tài liệu và rerank nằm ở Java (QĐ-013).
- **Làm rõ (v1.2):** có đúng một module OCR, được gọi theo hai cách:
  - (a) chạy lô cho 286 file hiện có (lệnh chạy của Spring Boot, ví dụ profile `ingest-batch`),
  - (b) chạy qua hàng đợi cho file tải lên.

  Nếu trễ tiến độ thì làm (a) trước; (b) làm sau cũng không phải sửa code.
- **Hệ quả:**
  - Tuần 2 phải xây worker Java, không chỉ chạy script. Logic của `chuyen_pdf_rag.py` (chunk, table-to-json) phải **viết lại bằng Java**; bản Python được giữ làm bản mẫu để đối chiếu. Để kịp tiến độ, bản đầu chỉ cần: kiểm tra từng trang, **một** công cụ OCR, chạy lô cho dữ liệu hiện có, và **một** endpoint tải lên. Giao diện kiểm tra lại (`CAN_KIEM_TRA`) để sau nếu còn thời gian.
  - **Cần cập nhật kế hoạch** khi chốt QĐ-011 (ke-hoach v1.1).
  - Công cụ OCR vẫn chưa chốt. Sẽ thử Tesseract `vie`, PaddleOCR và LLM đọc ảnh trên 5–10 trang BCTC mẫu. Tiêu chí: độ đúng của dấu và số, khả năng giữ cấu trúc bảng, tốc độ trên CPU, chi phí.

### QĐ-012 · Nhiều định dạng đầu vào → một "tài liệu chuẩn" → RAG 🟡
*(2026-09-17 · đề xuất; v1.2 viết lại bảng định dạng)*
- **Bối cảnh:** tài liệu tài chính có thể ở nhiều dạng. Toàn bộ 613 tài liệu trên web CTR hiện là PDF, nhưng người dùng có thể tải lên file Excel số liệu, Word tờ trình hoặc ảnh chụp văn bản.
- **Quyết định:**
  - **Mọi định dạng được hỗ trợ đều đi qua pipeline.** Không có định dạng nào "bỏ qua pipeline". Chỉ khác nhau ở **bộ trích xuất**, và chỉ PDF chữ kém/scan và ảnh mới qua bước OCR.
  - Tất cả bộ trích xuất đều xuất ra **tài liệu chuẩn**. Sau đó dùng chung một luồng: table-to-json → chunk → embed → trích dẫn.
  - Định dạng không có trong danh sách bị **từ chối ngay khi tải lên**.

**Định dạng được hỗ trợ**

| Định dạng | Nhận diện (magic bytes) | Bộ trích xuất (dự kiến) | OCR? | Ghi chú |
|---|---|---|---|---|
| **PDF** văn bản số | `%PDF` | Apache PDFBox (chữ + vị trí); bảng: Tabula-java | Không | Xét **từng trang**: trang đạt chuẩn thì đọc thẳng |
| **PDF** scan / lớp chữ hỏng | `%PDF` | OCR theo trang (QĐ-008) | **Có** | 286/314 file hiện có thuộc nhóm này. Không tin lớp chữ có sẵn |
| **Ảnh** PNG, JPG/JPEG | PNG / JPEG | OCR | **Có** | Ảnh chụp văn bản. Có thể cần xoay/làm thẳng trước khi OCR |
| **Ảnh** TIFF (nhiều trang) | TIFF | tách trang → OCR | **Có** | Định dạng phổ biến của máy scan văn phòng |
| **DOCX** | ZIP có `word/document.xml` | Apache POI (XWPF): heading theo style, đoạn văn, bảng gốc | Không | Ảnh nhúng trong DOCX **không OCR** (giới hạn đã biết) |
| **XLSX** | ZIP có `xl/workbook.xml` | Apache POI (XSSF, đọc giá trị đã tính của công thức) | Không | Mỗi sheet → một hoặc nhiều khối `bang`. Tách ô gộp. Chính xác nhất cho bảng số |
| **CSV** | văn bản, dò dấu phân cách | Apache Commons CSV → khối `bang` | Không | Ví dụ file xuất từ vnstock. Số liệu có cấu trúc **ưu tiên nạp vào DB** (QĐ-009) |
| **HTML** | có `<html` | Jsoup: heading, đoạn văn, `<table>` | Không | Trang tin, bản chụp trang web |
| **TXT / MD** | UTF-8 hợp lệ | tách đoạn; MD thì đọc heading `#` | Không | Ghi chú, văn bản đã số hoá |
| DOC, XLS (định dạng cũ) | OLE2 | **Tuỳ chọn:** chuyển sang DOCX/XLSX bằng LibreOffice headless rồi xử lý như trên | Không | Dữ liệu hiện có không có file nào loại này. Nếu không cài LibreOffice thì từ chối kèm thông báo "hãy lưu lại dạng .docx/.xlsx" |

**Định dạng bị từ chối** (kèm lý do trả về cho người dùng)

| Định dạng | Lý do |
|---|---|
| ZIP, RAR, 7z | Rủi ro file nén bom (zip bomb) và file độc hại lồng bên trong. Yêu cầu người dùng giải nén rồi tải từng file |
| PDF có mật khẩu / mã hoá | Không đọc được. Trạng thái `LOI` |
| PPTX, ODT, ODS, RTF, EPUB… | Hiếm gặp trong công bố tài chính, nằm ngoài phạm vi |
| File thực thi, script, file có macro (XLSM, DOCM) | An toàn |
| File có đuôi không khớp nội dung thật | An toàn: nhận diện bằng magic bytes (Apache Tika), không tin đuôi file |

**Giới hạn tải lên (dự kiến)**
- **≤ 100 MB** mỗi file. File lớn nhất hiện có là 43,9 MB (BCTC hợp nhất 2024 kèm giải trình).
- **≤ 1.000 trang** mỗi file. File dài nhất hiện có là 518 trang (báo cáo quản trị năm 2025, song ngữ).
- Với XLSX: giới hạn số sheet và số ô để tránh file "bom" (ví dụ ≤ 50 sheet, ≤ 1 triệu ô).

**Khung tài liệu chuẩn (dự kiến)**
```json
{
  "tai_lieu_id": "...", "nguon": "url | upload", "sha256": "...",
  "dinh_dang": "pdf | png | jpg | tiff | docx | xlsx | csv | html | txt | md",
  "metadata": {"cong_ty": "CTR", "loai": "bctc", "nam": 2025, "ky": "N",
               "hop_nhat": true, "ngon_ngu": "vi"},
  "trang": [
    {"so_trang": 12, "cach_trich": "text_layer | ocr | native", "do_tin_cay": 0.93,
     "khoi": [
       {"loai": "heading", "cap": 2, "van_ban": "24. Các khoản mục ngoài bảng..."},
       {"loai": "doan_van", "van_ban": "..."},
       {"loai": "bang", "tieu_de": "...", "cot": ["Chỉ tiêu", "31/12/2025", "01/01/2025"],
        "hang": [{"Chỉ tiêu": "Từ 1 năm trở xuống", "31/12/2025": 902340390204, "01/01/2025": 744132168672}]}
     ]}
  ]
}
```
Với định dạng không có khái niệm trang thì quy ước như sau:
- DOCX: `so_trang` là số thứ tự của mục.
- XLSX: mỗi sheet là một "trang", và thêm trường `sheet`.
- CSV, HTML, TXT, MD: toàn bộ nội dung là trang 1.

Nhờ vậy trích dẫn vẫn chỉ đúng được vị trí.

- **Lý do:**
  - Thêm định dạng mới thì chỉ cần viết thêm một bộ trích xuất.
  - `so_trang` và `cach_trich` đi theo từng khối đến tận chunk, nên câu trả lời trích dẫn được vị trí và cho biết số liệu đến từ OCR hay từ bản gốc.
  - Table-to-json (paper Hierarchical Reranker) chỉ phải viết một lần.
  - *(v1.3)* Tài liệu chuẩn được **sinh ra ở Java**. Dịch vụ OCR chỉ trả kết quả thô, nên table-to-json và chunk chỉ có một bản duy nhất, viết bằng Java.

### QĐ-013 · Lõi hệ thống viết bằng Java; Python chỉ chạy mô hình và công cụ ngoài hệ thống ✅
*(2026-09-17 · chốt theo yêu cầu của chủ đề tài; v1.2 từng đề xuất "Python trích xuất", nay bỏ)*
- **Bối cảnh:** đề tài dùng stack Java (Spring Boot). Chủ đề tài yêu cầu toàn bộ **lõi** là Java: RAG phân cấp và function calling SQL.
- **Quyết định — ranh giới:**

  | Thành phần | Ngôn ngữ | Ghi chú |
  |---|---|---|
  | Router DB / RAG / kết hợp | **Java** | |
  | Function calling SQL: định nghĩa tool, kiểm tra tham số, mẫu SQL, hàm tính chỉ số | **Java** | Spring AI tool calling (`@Tool`) + JDBC/JPA |
  | Tiền truy xuất: chuẩn hoá, từ điển viết tắt, quy đổi đơn vị, trích từ khoá | **Java** | |
  | Nạp tài liệu: kiểm tra, trích xuất, tài liệu chuẩn, **table-to-json**, chunk | **Java** | Tika, PDFBox, POI, Jsoup, Commons CSV |
  | Gọi embedding, lưu và tìm vector | **Java** | MariaDB `VECTOR` |
  | Rerank 2 tầng: **điều phối** (lấy 100 → 20, ngưỡng, ghép kết quả) | **Java** | |
  | Sinh câu trả lời, trích dẫn, quản lý ngữ cảnh 64k, hợp nhất R1/R2, trả lời "không xác định" | **Java** | |
  | Baseline text-to-SQL, RAG cơ bản, công cụ đánh giá | **Java** | Cùng hạ tầng, nên so sánh độ trễ và chi phí mới công bằng |
  | **Chạy mô hình** OCR / reranker / embedding | Java nếu có thể, còn không thì **dịch vụ mô hình** (xem dưới) | Chỉ nhận đầu vào và trả kết quả, **không chứa logic nghiệp vụ** |
  | Thu thập dữ liệu, đo chất lượng PDF | Python | Công cụ chạy ngoài hệ thống, không thuộc lõi |

- **Chạy mô hình: các phương án (chọn theo kết quả thử, ưu tiên từ trên xuống):**

  | Mô hình | Thuần Java | Qua API bên ngoài | Tự host (dịch vụ mô hình) |
  |---|---|---|---|
  | Embedding | Spring AI `TransformersEmbeddingModel` (ONNX, chạy CPU) với mô hình đa ngôn ngữ | API embedding của nhà cung cấp LLM | TEI / vLLM |
  | Reranker tầng 1 (nhẹ) | Cross-encoder nhỏ chạy ONNX qua ONNX Runtime/DJL | API Jina (jina-reranker-v3) | TEI / vLLM |
  | Reranker tầng 2 (Qwen3-Reranker, 4B–8B) | Không khả thi trên CPU | API của nhà cung cấp có host mô hình này | vLLM (**cần GPU**, máy hiện không có) |
  | OCR | **Tess4J** (Tesseract `vie`) | LLM đọc ảnh qua Spring AI | PaddleOCR / VietOCR (Python, container riêng) |

  - Dù dùng phương án nào, **Java là bên gọi** và toàn bộ logic nằm ở Java. Dịch vụ mô hình (nếu có) được coi như một API bên ngoài.
  - Spring AI có sẵn các điểm mở rộng cho RAG theo module: biến đổi truy vấn, mở rộng truy vấn, truy xuất tài liệu, hậu xử lý tài liệu. Có thể cài chuẩn hoá và trích từ khoá vào bước biến đổi truy vấn, và cài hai tầng rerank vào bước hậu xử lý. Module `spring-ai-rag` có trong BOM 1.1.8 (QĐ-014); tên lớp cụ thể sẽ xác nhận khi viết code.
- **Lý do:**
  1. Theo yêu cầu đề tài và stack đã chọn. Phần đóng góp chính (RAG phân cấp + function calling) nằm trọn trong một codebase, dễ trình bày và bảo vệ.
  2. Tách "logic" khỏi "chạy mô hình" giúp đổi mô hình mà không phải sửa lõi, và đo chi phí/độ trễ của từng mô hình khi đánh giá.
  3. Hệ sinh thái Java đủ mạnh cho các phần trích xuất văn bản số và DOCX/XLSX (Tika, PDFBox, POI).
- **Hệ quả và rủi ro:**
  - `chuyen_pdf_rag.py` (chunk theo mục, table-to-json) phải **viết lại bằng Java**. Bản Python giữ làm bản mẫu và để đối chiếu đầu ra (cùng input thì chunk phải giống nhau).
  - **Reranker tầng 2 (Qwen3-Reranker-8B) không chạy được trên máy hiện tại.** Phải dùng API hoặc thay bằng mô hình nhỏ hơn. Nếu thay thì ghi rõ khác biệt so với paper trong báo cáo.
  - **OCR:** nếu Tess4J đủ tốt thì toàn hệ thống là Java thuần. Nếu không, thêm một container OCR (Python) hoặc dùng LLM đọc ảnh; cả hai đều không chứa logic.
  - ~~Phiên bản Spring AI phải khớp với Spring Boot 4.~~ Đã giải quyết bằng QĐ-014.

### QĐ-014 · Spring Boot 3.5.16 + Spring AI 1.1.8 ✅
*(2026-09-17 · theo yêu cầu của chủ đề tài)*
- **Bối cảnh:**
  - `core-service` được khởi tạo với **Spring Boot 4.0.8** nhưng lại khai báo **Spring AI 1.1.7**, là dòng Spring AI viết cho Boot 3.x (Boot 4 đi cùng Spring AI 2.0.x).
  - Toàn bộ lõi (QĐ-013) dựa vào Spring AI, nên sự lệch phiên bản này chặn đường tuần 3.
- **Quyết định:** hạ về **Spring Boot 3.5.16** (bản mới nhất của dòng 3.x trên Maven Central ngày 17/09/2026) và dùng **Spring AI 1.1.8** (bản mới nhất của dòng 1.1.x).
- **Lý do:**
  1. **Khớp chắc chắn:** POM của Spring AI 1.1.8 trên Maven Central khai báo phụ thuộc Spring Boot **3.5.15**, cùng dòng 3.5.
  2. Dòng Spring AI 1.1.x đã ổn định, và BOM có đủ module cần dùng: tool calling, `spring-ai-rag`, vector store MariaDB, Transformers/ONNX, cùng nhiều nhà cung cấp LLM.
  3. Hệ sinh thái Boot 3 (tài liệu, ví dụ, thư viện bên thứ ba) còn phong phú hơn Boot 4, nên ít rủi ro hơn trong 6 tuần.
  4. Code hiện có chưa dùng API riêng của Boot 4 (chỉ có `FinqApplication` và test mặc định), nên hạ phiên bản không phải sửa code.
- **Phương án khác đã cân nhắc:** giữ Boot 4 và dùng Spring AI 2.0.x (đã có 2.0.1). Không chọn vì dòng 2.0 còn mới (vừa ra bản chính thức), ví dụ và kinh nghiệm cộng đồng còn ít, và chủ đề tài muốn dùng Boot 3.
- **Thay đổi trong `pom.xml`:** xem bảng ở [cong-nghe_v1.2.md](cong-nghe_v1.2.md) §2.2.
- **Kiểm chứng:** `mvnw test-compile` → thành công (EXIT=0).
- **Lưu ý:** Boot 3.5 là dòng 3.x cuối cùng. Nên kiểm tra thời hạn hỗ trợ bản vá trên trang Spring. Việc này không ảnh hưởng tới đề tài 6 tuần, nhưng nên nhắc trong phần hướng phát triển (nâng lên Boot 4 + Spring AI 2.x).
