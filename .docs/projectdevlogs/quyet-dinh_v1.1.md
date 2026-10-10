# Sổ quyết định — v1.1

*Ngày: 2026-09-17 · Thay cho [v1.0](quyet-dinh_v1.0.md)*

## Thay đổi so với v1.0
- **QĐ-003:** viết lại phần lý do chọn 2019–2026, có số liệu đếm từ `manifest.csv`. Bổ sung điểm yếu của 2019–2020. **Sửa** nhận định cũ "tài liệu 2015–2018 nhiều bản scan": chưa đo nên không có căn cứ, và chính BCTC 2019–2020 cũng là bản scan.
- **Thêm QĐ-011:** OCR là một thành phần của hệ thống (pipeline nạp tài liệu), không phải bước chạy một lần.
- **Thêm QĐ-012:** mọi định dạng đầu vào đều được quy về một "tài liệu chuẩn" trước khi đưa vào RAG.
- **QĐ-008:** chuyển thành một phần của QĐ-011. Việc chọn công cụ OCR vẫn chưa chốt.

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

### QĐ-011 · OCR là thành phần của hệ thống: pipeline nạp tài liệu 🟡
*(2026-09-17 · đề xuất, chờ chốt công cụ OCR)*
- **Bối cảnh:**
  - 286/314 tài liệu hiện có không đọc chữ trực tiếp được.
  - Sau này người dùng sẽ nạp thêm tài liệu mới (BCTC quý mới, nghị quyết mới…), và rất có thể cũng là bản scan.
  - Nếu OCR chỉ là script chạy tay một lần thì hệ thống không tự xử lý được tài liệu mới.
- **Quyết định:** xây **pipeline nạp tài liệu (ingestion)** như một thành phần của hệ thống, và OCR là một bước bên trong. Bộ dữ liệu 314 file hiện có cũng đi qua **đúng pipeline này** ở chế độ chạy lô (CLI), nên chỉ có một luồng code.
- **Luồng xử lý:**
  1. `core-service` (Java) nhận file tải lên và kiểm tra an toàn: loại file theo *magic bytes* (không tin đuôi file), dung lượng tối đa, số trang tối đa.
  2. `core-service` lưu file, tạo bản ghi `tai_lieu` với trạng thái `CHO_XU_LY`, rồi đẩy job vào hàng đợi Redis.
  3. **Worker Python** nhận job và kiểm tra chất lượng **theo từng trang, không theo cả file**. Một PDF có thể lẫn trang văn bản số với trang scan (ví dụ trang chữ ký, trang có dấu). Dùng lại tiêu chí của `kiem_tra_pdf.py`, xếp mỗi trang vào một trong ba nhánh:
     - `so_hoa`: đọc lớp chữ có sẵn,
     - `ocr_kem`: bỏ lớp chữ có sẵn, OCR lại,
     - `scan`: OCR.
  4. Worker tạo "tài liệu chuẩn" (QĐ-012), rồi chuyển bảng sang JSON, chunk và embed để nạp vào vector store.
  5. Worker cập nhật trạng thái (`DA_XU_LY` / `CAN_KIEM_TRA` / `LOI`). Trang nào có **độ tin cậy OCR thấp** thì đánh dấu `CAN_KIEM_TRA` để người kiểm tra lại, và RAG phải ghi rõ nguồn là OCR khi trích dẫn.
- **Lý do:**
  - Hệ thống tự xử lý được tài liệu mới, không phụ thuộc thao tác tay.
  - Chất lượng OCR được theo dõi được, phục vụ tiêu chí *độ chính xác trích dẫn*.
  - Việc kiểm tra file tải lên nằm ở Java, đúng tinh thần "an toàn" của đề tài.
  - OCR, xử lý PDF và các model đều mạnh nhất ở hệ sinh thái Python, nên đặt chung với sidecar rerank thành một **dịch vụ Python duy nhất** (`ml-service`: nạp tài liệu + rerank) để không phát sinh thêm dịch vụ.
- **Hệ quả:**
  - Tuần 2 phải xây worker, không chỉ chạy script. Để kịp tiến độ, bản đầu chỉ cần: kiểm tra từng trang, **một** công cụ OCR, chạy lô cho dữ liệu hiện có, và **một** endpoint tải lên. Giao diện kiểm tra lại (`CAN_KIEM_TRA`) để sau nếu còn thời gian.
  - **Cần cập nhật kế hoạch** khi chốt QĐ-011 (ke-hoach v1.1).
  - Công cụ OCR vẫn chưa chốt. Sẽ thử Tesseract `vie`, PaddleOCR và LLM đọc ảnh trên 5–10 trang BCTC mẫu. Tiêu chí: độ đúng của dấu và số, khả năng giữ cấu trúc bảng, tốc độ trên CPU, chi phí.

### QĐ-012 · Nhiều định dạng đầu vào → một "tài liệu chuẩn" → RAG 🟡
*(2026-09-17 · đề xuất)*
- **Bối cảnh:** tài liệu tài chính có thể là PDF văn bản số, PDF scan, ảnh chụp, Word, Excel hoặc HTML. Nếu mỗi định dạng có một luồng chunk riêng thì bước chunk, table-to-json và trích dẫn sẽ bị viết lặp lại và dễ lệch nhau.
- **Quyết định:** mỗi định dạng chỉ có một **bộ trích xuất** riêng. Tất cả bộ trích xuất đều xuất ra cùng một cấu trúc **tài liệu chuẩn**. Từ đó trở đi (table-to-json → chunk → embed → trích dẫn), mọi tài liệu dùng chung một luồng.

  | Định dạng | Bộ trích xuất |
  |---|---|
  | PDF văn bản số | đọc lớp chữ, dò bảng |
  | PDF scan / lớp chữ kém, ảnh PNG/JPG | OCR có nhận dạng bố cục bảng |
  | DOCX | đọc đoạn văn, heading, bảng gốc |
  | XLSX | đọc bảng trực tiếp (không cần OCR, chính xác nhất) |
  | HTML | đọc bảng và đoạn văn (ví dụ dữ liệu web ở `08_du_lieu_web`) |

- **Khung tài liệu chuẩn (dự kiến):**
  ```json
  {
    "tai_lieu_id": "...", "nguon": "url | upload", "sha256": "...",
    "metadata": {"cong_ty": "CTR", "loai": "bctc", "nam": 2025, "ky": "N",
                 "hop_nhat": true, "ngon_ngu": "vi"},
    "trang": [
      {"so_trang": 12, "cach_trich": "text_layer | ocr", "do_tin_cay": 0.93,
       "khoi": [
         {"loai": "heading", "cap": 2, "van_ban": "24. Các khoản mục ngoài bảng..."},
         {"loai": "doan_van", "van_ban": "..."},
         {"loai": "bang", "tieu_de": "...", "cot": ["Chỉ tiêu", "31/12/2025", "01/01/2025"],
          "hang": [{"Chỉ tiêu": "Từ 1 năm trở xuống", "31/12/2025": 902340390204, "01/01/2025": 744132168672}]}
       ]}
    ]
  }
  ```
- **Lý do:**
  - Thêm định dạng mới thì chỉ cần viết thêm một bộ trích xuất.
  - `so_trang` và `cach_trich` đi theo từng khối đến tận chunk, nên câu trả lời trích dẫn được đến trang và cho biết số liệu đến từ OCR hay từ bản gốc.
  - Table-to-json (paper Hierarchical Reranker) chỉ phải viết một lần.
- **Giới hạn trong phạm vi đề tài:** "mọi định dạng" được hiểu là **danh sách hỗ trợ ở bảng trên**. Định dạng khác bị từ chối ngay ở bước tải lên (an toàn). `chuyen_pdf_rag.py` hiện có sẽ được tách thành hai phần: *bộ trích xuất PDF* và *luồng chung* (chunk, table-to-json).
