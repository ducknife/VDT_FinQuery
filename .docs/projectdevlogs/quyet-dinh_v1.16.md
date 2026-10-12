# Sổ quyết định — v1.16

*Ngày: 2026-10-11 · Thay cho [v1.15](quyet-dinh_v1.15.md)*

## Thay đổi so với v1.15
- **Thêm QĐ-027:** bước chuẩn hoá của tiền truy xuất gồm **6 quy tắc**, theo nguyên tắc "thêm dạng chuẩn, giữ chữ gốc"; chuẩn hoá ngữ liệu chỉ bật khi thí nghiệm chứng minh có lợi.

## Thay đổi của v1.15 so với v1.14
- Đổi "MariaDB" thành "MySQL" ở các chỗ còn sót (QĐ-019, QĐ-021, bảng phân vai Java/Python). Dòng tiêu đề của v1.15 vẫn ghi "v1.14" do sót khi tạo bản.

## Thay đổi của v1.14 so với v1.13
- **Thêm QĐ-026:** bốn **khối nền** mentor yêu cầu kiểm định trước — DB core, Model, Inference, Measure + Test. Kế hoạch được tổ chức lại theo bốn khối này ([ke-hoach_v1.1.md](ke-hoach_v1.1.md)).

## Thay đổi của v1.13 so với v1.12 (theo góp ý của mentor)
- **Thêm QĐ-021:** cơ sở dữ liệu vector chuyển sang **Qdrant** (thay kiểu VECTOR của MySQL).
- **Thêm QĐ-022:** phân rã bài toán thành **tiền xử lý → xử lý → đánh giá**, mỗi pha lại chia nhỏ theo cùng cách.
- **Thêm QĐ-023:** **thứ tự ưu tiên** — hoàn thiện lõi trước; Hexagonal, Redis, Kafka để sau rồi dựng lại.
- **Thêm QĐ-024:** đánh giá thêm các mô hình **chuyên cho tiếng Việt** (PhoBERT và nhánh phái sinh), xem chất lượng có tăng không và pipeline có phải đổi không.
- **Thêm QĐ-025:** lập **hồ sơ cấu trúc** cho từng loại tài liệu (BCTC, BCTN, ĐHĐCĐ, CBTT) để xác định được tập dữ liệu cần xử lý.
- **QĐ-019 và QĐ-020 bị điều chỉnh thứ tự** bởi QĐ-023 (nội dung kỹ thuật giữ nguyên).

## Thay đổi của v1.12 so với v1.11 (giữ lại để tham khảo)
- **QĐ-019:** thêm phương án dự phòng chạy K8s bằng Docker Desktop; máy dự kiến nâng lên 12 GB RAM; chốt **cấu hình Kafka** (1 broker KRaft khi phát triển, thí nghiệm 3 broker để học).

## Thay đổi của v1.11 so với v1.10 (giữ lại để tham khảo)
- **QĐ-020:** chủ đề tài chốt **gọi kiến trúc là Hexagonal**, không gọi là Clean. Cách gọi trong báo cáo đổi thành "Modular monolith + Hexagonal (Ports & Adapters)". Phần so sánh Clean/Hexagonal giữ lại làm tư liệu, không dùng để đặt tên.

## Thay đổi của v1.10 so với v1.9 (giữ lại để tham khảo)
- **QĐ-020:** chốt đặt tên tiếng Anh trong code. Bổ sung phần *Clean Architecture và Hexagonal khác nhau thế nào*, và cách gọi kiến trúc trong báo cáo.
- **QĐ-019:** chốt chạy K8s trên **AWS EC2 (k3s)**, kèm ngân sách và cách tiết kiệm credit.

## Thay đổi của v1.9 so với v1.8 (giữ lại để tham khảo)
- **QĐ-019 chốt ✅** (chỗ chạy K8s: EC2 hay Docker Desktop, còn chờ).
- **Thêm QĐ-020:** kiến trúc modular monolith + hexagonal (ports & adapters).

## Thay đổi của v1.8 so với v1.7 (giữ lại để tham khảo)
- **Thêm QĐ-019 (🟡 đề xuất):** hạ tầng Docker, Redis, Kafka, Kubernetes. Đây là mục tiêu học công nghệ của chủ đề tài, và gắn với tiêu chí "tính ứng dụng" ([tieu-chi-vdt_v1.0.md](tieu-chi-vdt_v1.0.md)).

## Thay đổi của v1.7 so với v1.6 (giữ lại để tham khảo)
- **QĐ-018 chốt ✅:** tầng 1 dùng ViRanker, tầng 2 dùng mô hình rerank chất lượng cao của một nhà cung cấp. Nếu máy không chạy được thì **cả hai tầng gọi qua API**.

## Thay đổi của v1.6 so với v1.5 (giữ lại để tham khảo)
- **Thêm QĐ-017:** ranh giới phạm vi, bốn kiểu phản hồi, hội thoại nhiều lượt, chỉ tiếng Việt, câu trả lời gồm bảng + văn bản.
- **Thêm QĐ-018:** dùng ViRanker cho rerank tiếng Việt (vị trí trong chuỗi rerank đang đề xuất).

## Thay đổi của v1.5 so với v1.4 (giữ lại để tham khảo)
- **Thêm QĐ-015:** người dùng mục tiêu là nội bộ doanh nghiệp (phòng kế toán, tài chính).
- **Thêm QĐ-016:** đăng nhập SSO bằng Keycloak, 3 vai trò, nhật ký kiểm toán.

## Thay đổi của v1.4 so với v1.3 (giữ lại để tham khảo)
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
  | Gọi embedding, lưu và tìm vector | **Java** | MySQL `VECTOR` |
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
  2. Dòng Spring AI 1.1.x đã ổn định, và BOM có đủ module cần dùng: tool calling, `spring-ai-rag`, vector store MySQL, Transformers/ONNX, cùng nhiều nhà cung cấp LLM.
  3. Hệ sinh thái Boot 3 (tài liệu, ví dụ, thư viện bên thứ ba) còn phong phú hơn Boot 4, nên ít rủi ro hơn trong 6 tuần.
  4. Code hiện có chưa dùng API riêng của Boot 4 (chỉ có `FinqApplication` và test mặc định), nên hạ phiên bản không phải sửa code.
- **Phương án khác đã cân nhắc:** giữ Boot 4 và dùng Spring AI 2.0.x (đã có 2.0.1). Không chọn vì dòng 2.0 còn mới (vừa ra bản chính thức), ví dụ và kinh nghiệm cộng đồng còn ít, và chủ đề tài muốn dùng Boot 3.
- **Thay đổi trong `pom.xml`:** xem bảng ở [cong-nghe_v1.2.md](cong-nghe_v1.2.md) §2.2.
- **Kiểm chứng:** `mvnw test-compile` → thành công (EXIT=0).
- **Lưu ý:** Boot 3.5 là dòng 3.x cuối cùng. Nên kiểm tra thời hạn hỗ trợ bản vá trên trang Spring. Việc này không ảnh hưởng tới đề tài 6 tuần, nhưng nên nhắc trong phần hướng phát triển (nâng lên Boot 4 + Spring AI 2.x).

### QĐ-015 · Người dùng mục tiêu: nội bộ doanh nghiệp, phòng kế toán và tài chính ✅
*(2026-09-17 · chốt khi brainstorm, chủ đề 1)*
- **Bối cảnh:** có ba hướng: (A) nhà đầu tư, nhà phân tích bên ngoài; (B) nội bộ doanh nghiệp; (C) cả hai, có phân quyền.
- **Quyết định:** **B**. FinQuery là *trợ lý phân tích tài chính cho doanh nghiệp*, phục vụ **phòng kế toán và phòng tài chính**. Dữ liệu thử nghiệm là tài liệu công bố của CTR ("mô phỏng bằng dữ liệu công bố").
- **Lý do:**
  1. Chữ "an toàn" trong tên đề tài có ý nghĩa thật trong môi trường doanh nghiệp: không chạy SQL tuỳ ý, không sửa dữ liệu, câu trả lời phải có bằng chứng, và mọi thao tác đều kiểm toán được.
  2. Mảng E (kế hoạch và thực hiện, kèm giải trình) và mảng A (khoản vay, tài sản đảm bảo) là việc thật của phòng tài chính.
  3. Phân biệt được với các trang dữ liệu công khai: FinQuery giải thích số liệu kèm trích dẫn và kết hợp được nhiều nguồn.
  4. Khớp với paper Hierarchical Reranker (được triển khai trong quy trình kiểm toán và đầu tư của tổ chức).
  5. Phạm vi vừa sức 6 tuần (C quá rộng).
- **Hệ quả:**
  - Bắt buộc có đăng nhập và phân quyền (QĐ-016).
  - Danh mục câu hỏi viết theo cách nhân viên kế toán, tài chính hay hỏi.
  - **Kiểm chứng với người thật:** chủ đề tài sẽ nhờ anh/chị ở phòng kế toán, tài chính trong công ty rà lại danh mục câu hỏi và từ điển ([tu-dien-tai-chinh_v1.0.md](tu-dien-tai-chinh_v1.0.md)).
  - Giới hạn: doanh nghiệp thật có dữ liệu nội bộ (sổ cái, hợp đồng vay…), còn đề tài chỉ dùng dữ liệu công bố. Phải ghi rõ trong báo cáo.

### QĐ-016 · Đăng nhập SSO bằng Keycloak, 3 vai trò, nhật ký kiểm toán ✅
*(2026-09-17 · phần phụ, không thuộc lõi)*
- **Quyết định:**
  - **SSO:** Keycloak (mã nguồn mở, chạy Docker), chuẩn OpenID Connect.
    - React đăng nhập qua Authorization Code + PKCE.
    - `core-service` là OAuth2 Resource Server (Spring Security): kiểm tra JWT và đọc vai trò từ token.
  - **Vai trò:**

    | Vai trò | Quyền |
    |---|---|
    | `NHAN_VIEN` | Hỏi đáp, xem trích dẫn, xem lịch sử câu hỏi của mình |
    | `QUAN_LY_TAI_LIEU` | Như trên, cộng: tải tài liệu lên, duyệt trang OCR `CAN_KIEM_TRA` |
    | `QUAN_TRI` | Quản lý tài khoản (trên Keycloak), xem nhật ký kiểm toán, thống kê chi phí và token |

  - **Nhật ký kiểm toán:** mỗi câu hỏi lưu các thông tin sau:
    - người hỏi, thời điểm,
    - câu hỏi gốc và câu hỏi sau chuẩn hoá,
    - nhánh (DB / RAG / kết hợp),
    - **hàm đã gọi, tham số, mã mẫu SQL**,
    - các chunk và tài liệu đã trích dẫn,
    - token vào/ra, chi phí, độ trễ từng bước,
    - kết quả (có câu trả lời / không xác định / lỗi).
- **Lý do:**
  - Keycloak lo sẵn màn hình đăng nhập, lưu mật khẩu, quên mật khẩu, MFA, nên ít việc hơn tự viết.
  - Keycloak liên kết được với Active Directory/LDAP của doanh nghiệp, nên khi triển khai thật chỉ cần trỏ vào hệ thống tài khoản sẵn có.
  - Nhật ký kiểm toán vừa phục vụ tính "an toàn", vừa **là nguồn số liệu cho phần đánh giá** (độ trễ, token, chi phí), không cần đo riêng.
- **Giới hạn thời gian:** SSO khoảng 1–2 ngày, làm ở tuần 5 cùng frontend. Nếu gặp khó thì tạm dùng tài khoản cố định, không để ảnh hưởng lõi. Nhật ký kiểm toán thì làm cùng lõi (tuần 3–4), vì phần đánh giá cần.

### QĐ-017 · Ranh giới phạm vi và hình thức hỏi đáp ✅
*(2026-09-19 · brainstorm chủ đề 3)*
- **Bốn kiểu phản hồi** (mỗi câu trong bộ đánh giá được gắn nhãn kiểu phản hồi *đúng*):

  | Kiểu | Khi nào |
  |---|---|
  | Trả lời | Có dữ liệu, đúng phạm vi |
  | Trả lời một phần | Chỉ có một phần dữ liệu (ví dụ kế hoạch 2026 mới có số đến 30/06/2026). Phải nói rõ giới hạn |
  | Không xác định | Đúng phạm vi nhưng không có dữ liệu (ngoài 2019–2026, covenant, trái phiếu CTR…) |
  | Từ chối | Ngoài phạm vi hoặc không an toàn |

- **Ngoài phạm vi / từ chối:**
  - dự báo tương lai (nếu có kế hoạch đã công bố thì trả lời kế hoạch, ghi rõ không phải dự báo),
  - khuyên đầu tư,
  - so sánh với công ty khác (nhưng mọi hàm đều có tham số `ma_cong_ty` để mở rộng sau),
  - giá cổ phiếu và các chỉ số thị trường,
  - câu tổng hợp mở (không đưa vào bộ đánh giá; hệ thống gợi ý hỏi cụ thể hơn),
  - câu ngoài tài chính.
- **An toàn:**
  - Hệ thống **chỉ đọc**. Tài khoản DB của function calling chỉ có quyền `SELECT`.
  - Từ chối yêu cầu ghi hoặc sửa dữ liệu.
  - Từ chối tấn công prompt và yêu cầu chạy SQL tuỳ ý.
  - Bộ đánh giá có **khoảng 10–15 câu an toàn** làm hạng mục riêng khi so với baseline text-to-SQL.
- **Hội thoại nhiều lượt:** **có**. Hệ thống nhớ ngữ cảnh của câu trước (công ty, kỳ báo cáo, chỉ tiêu, loại BCTC) để hiểu câu hỏi nối tiếp như "còn 2024 thì sao?". Cách đặt câu hỏi nhiều lượt tham khảo bộ ConvFinQA; bộ đánh giá có một nhóm hội thoại nhiều lượt tiếng Việt tự xây.
- **Ngôn ngữ:** chỉ **tiếng Việt**.
- **Hình thức câu trả lời:** **văn bản + bảng là bắt buộc**, luôn kèm trích dẫn. **Biểu đồ là hướng bổ sung** (làm ở frontend nếu còn thời gian).

### QĐ-018 · Rerank 2 tầng: tầng 1 ViRanker, tầng 2 mô hình rerank của nhà cung cấp ✅
*(2026-09-19 · chốt ở v1.7; phương thức chạy cụ thể chọn sau khi đo tốc độ)*
- **Bối cảnh:** hệ thống chỉ dùng tiếng Việt. Reranker trong paper (jina-reranker-v3, Qwen3-Reranker) là mô hình đa ngôn ngữ.
- **ViRanker** (Dang Phuong-Nam, Nguyen Kieu-Linh, Pham Thanh-Hieu, 2025; arXiv 2509.09131; Hugging Face `namdp-ptit/ViRanker`):
  - cross-encoder khoảng **0,6B tham số**, tinh chỉnh từ **BGE-M3** (kết hợp Blockwise Parallel Transformer),
  - tối đa **512 token**, giấy phép **Apache 2.0**,
  - trên MMARCO-VI: NDCG@10 **0,7302**, MRR@10 **0,7107**, NDCG@3 **0,6815**. Vượt BGE-Reranker-v2-m3 ở cả ba chỉ số; PhoRanker nhỉnh hơn ở NDCG@10 (0,7422).
- **Đề xuất vị trí:** dùng ViRanker ở **tầng 1** (thay jina-reranker-v3; cả hai cùng khoảng 0,6B). Tầng 2 giữ vai trò mô hình lớn (Qwen3-Reranker qua API, hoặc LLM chấm điểm).
  - Cách này giữ đúng ý "mô hình nhỏ lọc rộng, mô hình lớn xét sâu" của paper.
  - Việc thay reranker đa ngôn ngữ bằng reranker chuyên tiếng Việt trở thành **một điểm đóng góp đo được**: so ViRanker với jina-reranker-v3 và BGE-Reranker-v2-m3 ở tầng 1.
- **Chạy mô hình (QĐ-013):** Java gọi mô hình, không chứa logic trong phần chạy mô hình. Hai cách:
  - xuất ONNX rồi chạy bằng ONNX Runtime Java,
  - container phục vụ reranker (ví dụ TEI; cần kiểm tra TEI có nạp được ViRanker không, vì cùng kiến trúc với BGE-Reranker-v2-m3).
- **Rủi ro:** chạy cross-encoder 0,6B trên CPU với khoảng 100–200 cặp (câu hỏi, chunk) có thể mất hàng chục giây. Cần **đo thực tế**. Nếu chậm thì: giảm số ứng viên, cắt chunk ngắn hơn, hoặc thuê GPU.
- **Hệ quả:** chunk nên **≤ 512 token** để ViRanker đọc được trọn. Cần đưa giới hạn này vào thiết kế chunk.

**Chốt ở v1.7:**
- **Tầng 1 = ViRanker** (chuyên tiếng Việt, khoảng 0,6B): lọc từ ứng viên của bước tìm vector xuống **100**.
- **Tầng 2 = mô hình rerank chất lượng cao của một nhà cung cấp** (chưa chọn cụ thể; ứng viên: Qwen3-Reranker được host qua API, Cohere Rerank, Jina Reranker, hoặc LLM chấm điểm theo danh sách): lọc từ 100 xuống **20**.
- **Cách chạy mô hình** (ViRanker có kiến trúc `XLMRobertaForSequenceClassification` chuẩn, tương thích TEI và HF Inference Endpoints, nhưng không có API miễn phí sẵn):
  1. Thử **ONNX INT8 trên CPU** trước (đo ở đầu tuần 2, cùng đợt thử OCR).
  2. Nếu máy không chạy được thì **cả hai tầng gọi qua API**: ViRanker qua HF Inference Endpoints/TEI (có tự tắt khi không dùng), tầng 2 qua API của nhà cung cấp.
- **Thiết kế Java:** một interface `Reranker`, mỗi cách chạy là một cài đặt riêng (ONNX cục bộ / TEI HTTP / API nhà cung cấp). Đổi cách chạy chỉ cần đổi cấu hình, và cũng dùng cơ chế này để chạy các thử nghiệm so sánh reranker.
- **Hệ quả khi gọi API:**
  - Độ trễ mạng và chi phí của cả hai tầng được **tính vào phần đánh giá** (độ trễ, chi phí API).
  - **Dữ liệu rời khỏi hệ thống:** đoạn văn tài liệu được gửi sang dịch vụ bên ngoài. Với dữ liệu công bố của CTR thì không sao. Trong báo cáo cần ghi rõ: khi triển khai thật cho phòng kế toán với dữ liệu nội bộ, nên **tự host** (container TEI trong mạng nội bộ) để dữ liệu không ra ngoài.

### QĐ-019 · Hạ tầng: Docker, Redis, Kafka, Kubernetes ✅
*(2026-09-19 · chốt ở v1.9, kèm điều kiện **lõi nghiệp vụ phải xong trước**; v1.10: K8s chạy trên AWS EC2)*
- **Bối cảnh:**
  - Chủ đề tài muốn **làm chủ Kafka, Redis, Docker, K8s** trong hơn một tháng tới, vì công ty đang dùng các công nghệ này.
  - Tiêu chí VDT cho điểm tối đa phần "tính ứng dụng" khi hệ thống **áp dụng được ngay**.
  - **Ràng buộc máy:** Windows 11, **RAM khoảng 7,6 GiB**, 8 luồng CPU, không có GPU. Docker 29.5, Compose v5.1, kubectl 1.34 đã có sẵn; Docker Desktop đang tắt; chưa có minikube/kind/helm.
- **Nguyên tắc:** mỗi công nghệ phải giải quyết **một vấn đề thật** của FinQuery. Không thêm vào chỉ để trình diễn.

  | Công nghệ | Vai trò trong FinQuery | Vì sao cần thật sự |
  |---|---|---|
  | **Docker + Compose** | Đóng gói mọi dịch vụ: `core-service`, frontend, MySQL, Redis, Kafka, Keycloak, dịch vụ mô hình (TEI/OCR) | Một lệnh dựng cả hệ thống, nên dễ bàn giao và "áp dụng ngay" |
  | **Redis** | (1) **Bộ nhớ hội thoại nhiều lượt** (QĐ-017) theo phiên, có thời hạn; (2) cache câu trả lời và kết quả truy vấn lặp lại; (3) cache embedding của câu hỏi; (4) giới hạn tần suất gọi theo người dùng (bảo vệ chi phí API) | Hội thoại nhiều lượt và kiểm soát chi phí cần lưu trạng thái nhanh, có thời hạn |
  | **Kafka** | (1) **Pipeline nạp tài liệu theo sự kiện**: `tai-lieu.da-tai-len` → `trang.can-ocr` → `trang.da-trich` → `chunk.da-tao` → `chunk.da-embed`; (2) **luồng sự kiện hỏi đáp** cho nhật ký kiểm toán và thống kê chi phí | Thay hàng đợi Redis ở QĐ-011. Lợi ích thật: **phát lại (replay)**, tức là đổi cách chunk hoặc đổi mô hình embedding thì đọc lại topic để nạp lại toàn bộ tài liệu, không phải tải lại; **nhiều bên đọc cùng một sự kiện** (nhật ký kiểm toán, thống kê, đánh giá) mà không ảnh hưởng lõi |
  | **Kubernetes** | Triển khai toàn bộ hệ thống bằng manifest (hoặc Helm); dịch vụ mô hình tách pod riêng; tự mở rộng `core-service` | "Áp dụng ngay" trong môi trường doanh nghiệp; demo được việc triển khai |

- **Thứ tự học và làm (gắn với kế hoạch 6 tuần):**
  1. **Docker/Compose:** ngay tuần 1–2, dựng MySQL, Redis, Keycloak, Kafka để phát triển.
  2. **Kafka:** tuần 2, cùng worker nạp tài liệu (QĐ-011).
  3. **Redis:** tuần 3–4, cùng bộ nhớ hội thoại và cache.
  4. **Kubernetes:** tuần 5–6, triển khai và demo.
- **Rủi ro và cách giảm:**
  - **RAM 7,6 GiB không đủ chạy đồng thời** Kafka (KRaft, 1 node), Keycloak, MySQL, Redis, `core-service`, ViRanker ONNX và cụm K8s. Cách giảm:
    - Kafka chạy chế độ KRaft 1 node, giới hạn heap;
    - mô hình gọi qua API (QĐ-018);
    - chạy K8s trên EC2 (ví dụ k3s 1 node) hoặc bật Kubernetes trong Docker Desktop **chỉ khi demo**;
    - phát triển hằng ngày bằng Compose với các profile bật/tắt từng nhóm dịch vụ.
  - **Rủi ro tiến độ:** học 4 công nghệ cùng lúc với làm lõi. Theo tiêu chí "tính hoàn thiện" (30 điểm), **lõi phải xong trước**. Nếu trễ thì theo thứ tự cắt: K8s chỉ dựng manifest, demo bằng Compose → Kafka chỉ dùng cho pipeline nạp tài liệu → Redis giữ tối thiểu phần bộ nhớ hội thoại.

### QĐ-020 · Kiến trúc: modular monolith + Hexagonal (Ports & Adapters) ✅
*(2026-09-19 · chủ đề tài đề xuất, sư phụ đồng ý; quy ước đặt tên còn chờ)*
- **Bối cảnh:** chủ đề tài muốn áp dụng Clean/Hexagonal Architecture, vừa làm vừa học. Hệ thống có thể chỉ là một monolith.
- **Quyết định:**
  - **Một codebase, một file jar** (`core-service`), chia thành **module theo nghiệp vụ**.
  - Mỗi module theo **hexagonal**: `domain` (luật nghiệp vụ thuần Java) ← `application` (use case + **port**) ← `adapter` (vào: REST, Kafka listener; ra: JPA, Spring AI, reranker, Redis, Kafka).
- **Vì sao hợp với FinQuery:** đề tài có **rất nhiều thành phần cần thay qua lại**, và mỗi thành phần chính là một *port* với nhiều *adapter*:

  | Port (interface) | Các adapter |
  |---|---|
  | `LlmPort` | nhà cung cấp LLM (chưa chốt), mock khi test |
  | `RerankerPort` | ViRanker ONNX cục bộ / ViRanker qua TEI / API nhà cung cấp tầng 2 / jina, BGE để so sánh |
  | `OcrPort` | Tess4J / LLM đọc ảnh / container PaddleOCR |
  | `EmbeddingPort`, `VectorStorePort` | ONNX cục bộ / API; MySQL VECTOR |
  | `EventPublisherPort` | Kafka / bộ nhớ trong (khi test) |
  | `ConversationMemoryPort` | Redis / bộ nhớ trong |
  | `AnswerStrategy` (ở tầng ứng dụng) | **FinQuery** / **baseline text-to-SQL** / **baseline RAG cơ bản**. Ba phương án chạy chung một bộ đánh giá |

  Nhờ vậy việc so sánh (các reranker, OCR, baseline) chỉ là **đổi adapter bằng cấu hình**. Các bảng so sánh trong báo cáo đến từ đây.
- **Vì sao monolith là đúng:**
  - 1 người làm trong 6 tuần, RAM 7,6 GiB.
  - Microservice làm tốn công vận hành mà không thêm giá trị cho lõi.
  - Các dịch vụ bên ngoài (Keycloak, Kafka, MySQL, Redis, container mô hình) là **hạ tầng**, không phải microservice nghiệp vụ.
- **Tận dụng K8s mà vẫn là monolith:** cùng một jar chạy **hai vai trò** bằng Spring profile:
  - `api`: nhận câu hỏi, REST;
  - `worker`: tiêu thụ Kafka, nạp tài liệu, OCR, embed.

  Trên K8s là hai Deployment, mở rộng độc lập.
- **Module dự kiến:**

  | Module | Nhiệm vụ |
  |---|---|
  | `qa` | Điều phối hỏi đáp: router DB/RAG/kết hợp, hội thoại nhiều lượt, ghép câu trả lời + trích dẫn |
  | `sqlquery` | Function calling: định nghĩa tool, kiểm tra tham số, mẫu SQL, tính chỉ số |
  | `retrieval` | RAG phân cấp: chuẩn hoá, trích từ khoá, tìm vector, rerank 2 tầng, quản lý ngữ cảnh 64k |
  | `ingestion` | Nạp tài liệu: kiểm tra file, trích xuất, OCR, tài liệu chuẩn, table-to-json, chunk, embed |
  | `evaluation` | Bộ câu hỏi chuẩn, chạy baseline, chấm điểm |
  | `audit` | Nhật ký kiểm toán, thống kê token và chi phí (đọc sự kiện) |
  | `identity` | Tích hợp Keycloak, vai trò |
  | `shared` | Kiểu dữ liệu dùng chung: `CompanyCode`, `ReportPeriod`, `StatementScope` (hợp nhất/riêng), `Citation`… |

- **Giữ ranh giới bằng công cụ, không chỉ bằng quy ước:**
  - **Spring Modulith 1.4.13**: đã kiểm tra trên Maven Central, được build trên đúng Spring Boot 3.5.16. Dùng để kiểm tra phụ thuộc giữa các module, phát sự kiện giữa module, và sinh sơ đồ module cho báo cáo.
  - **ArchUnit**: kiểm tra luật trong từng module (`domain` không phụ thuộc Spring, Spring AI hay JPA).
- **Chống làm quá tay (quan trọng cho tiến độ):**
  - Chỉ tạo port khi có **≥ 2 adapter thật**, hoặc cần mock khi test.
  - Chỗ nào chỉ có một cách làm thì dùng thẳng.
  - Dùng Java `record` cho DTO và value object để giảm code chuyển đổi.
  - Không bắt buộc tách entity JPA và model domain ở những chỗ chỉ đọc đơn giản.
- **Đặt tên (chốt ở v1.10):** package, lớp, hàm và biến trong Java dùng **tiếng Anh**. Chú thích (comment/Javadoc), log và tài liệu viết **tiếng Việt**. Thuật ngữ nghiệp vụ Việt Nam không có từ tương đương thì giữ nguyên dạng không dấu và giải thích trong Javadoc, ví dụ `StatementScope.HOP_NHAT`.
- **Clean Architecture hay Hexagonal? (bổ sung ở v1.10)**
  - Cả hai cùng một **nguyên tắc cốt lõi**: *luật phụ thuộc*. Phụ thuộc chỉ hướng vào trong; nghiệp vụ không biết framework, DB hay LLM là gì.
  - **Hexagonal** (Cockburn, 2005) mô tả theo *trong/ngoài*: lõi ứng dụng, **port**, **adapter** (bên gọi vào, ví dụ REST; bên bị gọi, ví dụ DB, LLM).
  - **Clean** (Robert C. Martin, 2012) là tổng hợp từ hexagonal, onion và các kiến trúc tương tự, mô tả theo *vòng tròn lớp*: Entities → Use Cases → Interface Adapters → Frameworks & Drivers. Clean quy định chi tiết hơn: Input/Output Boundary, Presenter, request/response model riêng cho từng use case.
  - **Đối chiếu với thiết kế FinQuery:**

    | Clean Architecture | FinQuery |
    |---|---|
    | Entities | `domain` |
    | Use Cases (+ boundary) | `application` (use case + port) |
    | Interface Adapters | `adapter/in` (REST, Kafka listener), `adapter/out` (JPA, Spring AI, Redis…) |
    | Frameworks & Drivers | Spring Boot, MySQL, Kafka, Redis, nhà cung cấp mô hình |

  - **Kết luận:** FinQuery **đang dùng Clean Architecture**, và dùng **từ vựng port/adapter của hexagonal** để gọi tên, vì điểm biến động lớn nhất của đề tài là **thay các thành phần bên ngoài** (LLM, reranker, OCR, baseline). Phần nghi thức riêng của Clean (Presenter, Output Boundary, model riêng cho mỗi use case) được **lược bỏ** để giữ tiến độ, vì với REST + JSON thì controller trả DTO trực tiếp là đủ.
  - ~~Cách gọi trong báo cáo: "Modular monolith theo Clean Architecture, hiện thực bằng Ports & Adapters".~~ **(v1.11) Cách gọi chính thức: "Modular monolith + Hexagonal Architecture (Ports & Adapters)".** Phần so sánh với Clean ở trên chỉ để hiểu, không dùng để gọi tên.

**Bổ sung QĐ-019 ở v1.10: chạy Kubernetes trên AWS EC2**
- **Tài khoản:** chủ đề tài đã có tài khoản AWS. Chính sách từ 15/07/2025: tài khoản mới nhận **100 USD credit khi đăng ký, làm thêm các hoạt động được tối đa thêm 100 USD** (tổng 200 USD). Gói Free **tự đóng sau 6 tháng hoặc khi hết credit**. Tài khoản tạo trước 15/07/2025 thì theo Free Tier cũ (12 tháng, máy micro 1 GB RAM, không đủ cho hệ thống này). → **Cần kiểm tra** trang Billing → Credits: loại gói, số credit còn lại, ngày hết hạn.
- **Cách chạy:** **k3s 1 node trên một máy EC2** (không dùng EKS, vì riêng control plane của EKS đã tốn khoảng 0,10 USD/giờ, tức khoảng 73 USD/tháng). Region **Singapore (ap-southeast-1)**, gần Việt Nam nhất.
- **Cỡ máy:** hệ thống cần khoảng 5–6 GB RAM (Kafka, Keycloak, MySQL, Redis, `api` + `worker`, k3s) → khoảng **t3.large (2 vCPU, 8 GB)**. Mô hình gọi qua API, không chạy trên máy này.
- **Ngân sách (ước tính, cần kiểm tra lại bằng AWS Pricing Calculator):** t3.large ở Singapore khoảng 0,1 USD/giờ.
  - Bật **24/7** thì khoảng 75 USD/tháng → **hết credit rất nhanh, không làm**.
  - **Chỉ bật khi thử và demo** (ví dụ khoảng 80 giờ trong cả dự án) thì khoảng **8–10 USD**, cộng ổ đĩa EBS khoảng 30 GB (vài USD/tháng) và IPv4 công khai (khoảng 0,005 USD/giờ khi bật).
  - **Tổng dự kiến dưới 20–30 USD.**
- **Bắt buộc làm trước khi tạo máy:**
  - tạo **AWS Budgets** cảnh báo ở mức 10, 30 và 50 USD;
  - luôn **Stop** máy khi không dùng;
  - xoá tài nguyên thừa (EBS, snapshot, Elastic IP, Load Balancer) sau mỗi lần thử.
- **Thời điểm:** tuần 5–6. Trong tuần 1–4 phát triển bằng Docker Compose trên máy cá nhân, **không tốn credit**.

**Bổ sung QĐ-019 ở v1.12**
- **Dự phòng K8s:** nếu không dùng được AWS thì chạy **Kubernetes trong Docker Desktop**. Chủ đề tài dự định **nâng RAM thêm 4 GB** (khoảng 12 GB), đủ để chạy cụm K8s một node cùng hệ thống khi demo.
- **Kafka: không bắt buộc 3 broker.**
  - **3 broker là khuyến nghị cho môi trường production**: `replication.factor = 3`, `min.insync.replicas = 2`, chịu được việc mất 1 broker mà không mất dữ liệu và không dừng ghi. Controller quorum (KRaft) cũng nên có 3 node.
  - **Khi phát triển và làm đề tài: 1 node KRaft ở chế độ gộp** (một tiến trình vừa là broker vừa là controller, không cần ZooKeeper; Kafka 4.x đã bỏ hẳn ZooKeeper).
    - Mọi replication factor đặt bằng 1: `offsets.topic.replication.factor`, `transaction.state.log.replication.factor`, `transaction.state.log.min.isr`, `default.replication.factor`.
    - Heap khoảng 512 MB – 1 GB.
    - Dùng image chính thức `apache/kafka`.
  - **Để học:** làm một **buổi thí nghiệm 3 broker** (Compose hoặc K8s), tạo topic với RF = 3 và tắt thử một broker để quan sát chọn leader mới và ISR. Không chạy 3 broker hằng ngày vì tốn RAM.
  - **Trong báo cáo:** ghi rõ cấu hình dùng khi làm đề tài (1 broker) và cấu hình đề xuất cho production (3 broker, RF 3, min ISR 2); trên K8s production có thể dùng operator như Strimzi.

### QĐ-021 · Cơ sở dữ liệu vector: Qdrant ✅
*(2026-09-21 · theo góp ý của mentor)*
- **Quyết định:** dùng **Qdrant** làm nơi lưu và tìm vector. **MySQL vẫn giữ** cho số liệu tài chính, dữ liệu sự kiện, người dùng và nhật ký kiểm toán.
- **Lý do:**
  - Lọc theo metadata mạnh và nhanh (loại tài liệu, năm, kỳ, hợp nhất/riêng, số trang) — đây là thứ FinQuery dùng liên tục, vì gần như câu hỏi nào cũng kèm điều kiện lọc.
  - Là cơ sở dữ liệu vector chuyên dụng: hỗ trợ nhiều vector cho một điểm dữ liệu, tìm kiếm lai (đặc và thưa), lượng tử hoá, snapshot.
  - Chạy bằng Docker một container, có sẵn giao diện web để xem điểm dữ liệu — tiện khi gỡ lỗi truy xuất.
  - **Spring AI 1.1.8 có sẵn `spring-ai-starter-vector-store-qdrant`** (đã kiểm tra trên Maven Central), nên tích hợp từ Java không phải viết thêm.
  - Đổi mô hình embedding (QĐ-024) thì chỉ cần tạo collection mới, không đụng tới cơ sở dữ liệu quan hệ.
- **Hệ quả:** thêm một container vào môi trường phát triển; cần xử lý việc **đồng bộ giữa MySQL và Qdrant** (mã tài liệu và mã chunk là khoá liên kết).

### QĐ-022 · Phân rã bài toán: tiền xử lý → xử lý → đánh giá ✅
*(2026-09-21 · theo góp ý của mentor)*
- **Quyết định:** chia bài toán thành ba pha lớn, mỗi pha **chia nhỏ tiếp theo cùng nguyên tắc**, làm xong pha trước mới sang pha sau (vẫn cho phép quay lại sửa).

| Pha | Các bước con |
|---|---|
| **1. Tiền xử lý** | (1.1) thu thập tài liệu · (1.2) đánh giá chất lượng, phân loại · (1.3) OCR theo trang · (1.4) trích xuất thành tài liệu chuẩn · (1.5) chuẩn hoá và chuyển bảng sang JSON · (1.6) tách đoạn (chunk) · (1.7) tạo embedding, nạp Qdrant · (1.8) nạp số liệu tài chính vào MySQL |
| **2. Xử lý** | (2.1) chuẩn hoá truy vấn, trích từ khoá · (2.2) định tuyến ba nhánh · (2.3) nhánh cơ sở dữ liệu: chọn hàm, kiểm tra tham số, chạy SQL, tính chỉ số · (2.4) nhánh tài liệu: tìm vector, rerank tầng 1, rerank tầng 2 · (2.5) sinh câu trả lời kèm trích dẫn · (2.6) xử lý ngữ cảnh dài và hợp nhất · (2.7) trả lời "không xác định" |
| **3. Đánh giá** | (3.1) xây bộ câu hỏi chuẩn · (3.2) dựng hai hệ thống đối chứng · (3.3) chấm độ chính xác và trích dẫn · (3.4) đo độ trễ, token, chi phí · (3.5) so sánh mô hình (embedding, reranker, mô hình ngôn ngữ) · (3.6) đo với người dùng thật |

- **Lý do:** mỗi bước con có **đầu vào và đầu ra rõ ràng**, nên kiểm thử được riêng, đo được riêng, và khi hỏng thì khoanh vùng nhanh. Cách chia này cũng là bố cục tự nhiên cho báo cáo cuối kỳ.
- **Hệ quả:** mỗi bước con cần một lệnh chạy độc lập (chạy lô) và một bộ dữ liệu mẫu nhỏ để kiểm thử.

### QĐ-023 · Thứ tự ưu tiên: lõi trước, phần hạ tầng bổ trợ sau ✅
*(2026-09-21 · theo góp ý của mentor)*
- **Quyết định:** làm **lõi nghiệp vụ chạy thông suốt trước**, bằng cách đơn giản nhất. Các phần bổ trợ làm sau rồi **dựng lại (refactor)**.

| Hạng mục | Giai đoạn đầu (ưu tiên lõi) | Làm sau |
|---|---|---|
| Kiến trúc | Chia gói theo nghiệp vụ, cách viết thẳng thắn, chỉ tách interface ở chỗ thật sự cần (mô hình, reranker, chiến lược trả lời) | Dựng lại đầy đủ theo **Hexagonal**, thêm Spring Modulith và ArchUnit |
| Hàng đợi nạp tài liệu | Chạy **đồng bộ** hoặc hàng đợi trong bộ nhớ; chế độ chạy lô bằng lệnh | Chuyển sang **Kafka** theo sự kiện |
| Bộ nhớ hội thoại, cache | Lưu tạm trong bộ nhớ ứng dụng | Chuyển sang **Redis** |
| Triển khai | Docker Compose trên máy cá nhân | **Kubernetes** trên EC2 |
| Đăng nhập | Tài khoản cấu hình sẵn | **Keycloak SSO** |

- **Lý do:** tiêu chí VDT cho **30 điểm tính hoàn thiện**; phần lõi (function calling và RAG phân cấp) mới là đóng góp của đề tài. Hạ tầng làm trước sẽ ngốn thời gian mà không tạo ra kết quả đánh giá nào.
- **Điều kiện để không phải viết lại từ đầu:** ngay từ đầu vẫn giữ **ba ranh giới** — cổng gọi mô hình, cổng chiến lược trả lời (để cắm được hai hệ thống đối chứng), và cổng lưu trữ vector. Ba chỗ này rẻ mà giúp việc dựng lại về sau nhẹ nhàng.
- **Ghi chú:** mục tiêu học Kafka, Redis, K8s (QĐ-019) vẫn giữ, chỉ **lùi thời điểm**.

### QĐ-024 · Đánh giá thêm các mô hình chuyên cho tiếng Việt 🟡
*(2026-09-21 · theo góp ý của mentor; danh sách mô hình cần kiểm chứng trước khi chạy)*
- **Câu hỏi cần trả lời bằng thực nghiệm:**
  1. Dùng mô hình chuyên tiếng Việt thì **chất lượng truy xuất có tăng** so với mô hình đa ngôn ngữ không?
  2. Nếu tăng thì **pipeline có phải thay đổi** không?
- **Nhóm mô hình đưa vào so sánh:**

| Vai trò | Ứng viên đa ngôn ngữ | Ứng viên chuyên tiếng Việt |
|---|---|---|
| Embedding (tìm kiếm thô) | BGE-M3, multilingual-E5 | Bi-encoder nền **PhoBERT**, các mô hình embedding tiếng Việt khác |
| Reranker | BGE-Reranker-v2-m3 | **ViRanker** (QĐ-018), **PhoRanker** |

- **Lưu ý kỹ thuật, ảnh hưởng trực tiếp tới pipeline:**
  - **PhoBERT yêu cầu tách từ** (word segmentation) trước khi đưa vào mô hình. Nếu chọn nhánh PhoBERT thì **bước 1.5 và 2.1 phải thêm bước tách từ**, và phải tách y hệt nhau ở cả lúc nạp lẫn lúc hỏi.
  - **Giới hạn độ dài khác nhau:** dòng PhoBERT khoảng 256 token, ViRanker 512 token, BGE-M3 dài hơn nhiều. Vì vậy **kích thước chunk phải chọn theo mô hình yếu nhất trong chuỗi**, nếu không sẽ bị cắt mất nội dung.
  - Đổi mô hình embedding thì phải **tạo lại toàn bộ chỉ mục** → đây là lý do nên để mỗi mô hình một collection riêng trong Qdrant.
- **Cách chạy:** thí nghiệm nằm ở bước 3.5, dùng chung bộ câu hỏi nhánh tài liệu đã gán nhãn đoạn đúng; đo NDCG@20, Recall@100, độ trễ và chi phí.
- **Cần kiểm chứng trước khi chạy:** tên và phiên bản chính xác của từng mô hình tiếng Việt, độ dài tối đa, giấy phép, và cách chạy (ONNX cục bộ hay dịch vụ mô hình).

### QĐ-025 · Hồ sơ cấu trúc cho từng loại tài liệu ✅
*(2026-09-21 · theo nhận xét của chủ đề tài: các loại tài liệu chưa có chuẩn chung)*
- **Vấn đề:** không như Form 10-K của Mỹ (có khung mục cố định do SEC quy định), tài liệu doanh nghiệp Việt Nam **không theo một khung thống nhất**: mỗi loại tài liệu, thậm chí mỗi năm, lại trình bày khác nhau. Đã thấy rõ khi đối chiếu BCTC 2023 với BCTC 2026 (đổi tên báo cáo, đổi mã chỉ tiêu). Nếu không chốt được "chuẩn chung" thì không xác định được **tập dữ liệu cần xử lý** và không viết được luật trích xuất.
- **Quyết định:** với mỗi loại tài liệu, xây một **hồ sơ cấu trúc** (viết dưới dạng JSON, đặt trong `data/formats/`, theo tinh thần của `why/10K_Report_Format.json` nhưng dựa trên tài liệu thật của CTR), gồm bốn phần:
  1. **Metadata bắt buộc:** công ty, loại tài liệu, kỳ báo cáo, hợp nhất hay riêng, mức kiểm tra (tự lập / soát xét / kiểm toán), ngày ký, ngôn ngữ, chế độ trình bày.
  2. **Danh sách mục chuẩn:** tên mục, mục bắt buộc hay tuỳ chọn, các cách viết khác nhau đã gặp (dùng để nhận dạng khi tách đoạn).
  3. **Vai trò từng mục:** đưa vào cơ sở dữ liệu số liệu, hay đưa vào chỉ mục tài liệu, hay bỏ qua.
  4. **Quy tắc kiểm tra đầy đủ:** thiếu mục bắt buộc thì đánh dấu `CAN_KIEM_TRA`.
- **Khung sơ bộ (sẽ hoàn thiện sau khi OCR):**

| Loại tài liệu | Các mục chuẩn (dự kiến) |
|---|---|
| **BCTC** (quý, bán niên, năm; hợp nhất, riêng) | Mục lục · báo cáo của Ban Tổng Giám đốc · báo cáo kiểm toán hoặc soát xét (chỉ có ở bán niên và năm) · **báo cáo tình hình tài chính / bảng cân đối kế toán** · **kết quả kinh doanh** · **lưu chuyển tiền tệ** · **thuyết minh** (các mục đánh số) |
| **BCTN** | Thông điệp lãnh đạo · tổng quan doanh nghiệp · tình hình hoạt động theo mảng · báo cáo Ban điều hành · đánh giá của HĐQT · **kế hoạch năm sau và kết quả so với kế hoạch** · quản trị công ty · rủi ro · BCTC tóm tắt |
| **Tài liệu ĐHĐCĐ** | Nghị quyết (các điều, tỷ lệ biểu quyết) · biên bản · tờ trình (số hiệu, nội dung trình, phương án) · báo cáo của HĐQT, Ban điều hành, Ban kiểm soát |
| **CBTT** | Số công văn · ngày · loại sự kiện · nội dung (thường kèm nghị quyết HĐQT) |

- **Cách xây dựng (thực nghiệm, không đoán):** lấy mẫu vài tài liệu mỗi loại và mỗi năm → trích danh sách tiêu đề mục (ưu tiên trang mục lục vì dễ đọc) → gom nhóm các cách viết khác nhau của cùng một mục → chốt tên chuẩn → viết vào tệp JSON.
- **Căn cứ pháp lý (tra cứu ngày 21/09/2026) — có chuẩn chung, do pháp luật quy định:**
  - **BCTC:** **Thông tư 99/2025/TT-BTC** (ban hành 27/10/2025) thay thế Thông tư 200/2014/TT-BTC, **hiệu lực từ 01/01/2026**. Mẫu **B01-DN đổi tên thành "Báo cáo tình hình tài chính"** → giải thích đúng hiện tượng quan sát được ở BCTC bán niên 2026 của CTR (đổi tên, đổi mã chỉ tiêu).
  - **BCTN:** **Thông tư 96/2020/TT-BTC, Phụ lục IV** quy định mẫu báo cáo thường niên của công ty đại chúng.
  - **Tài liệu ĐHĐCĐ:** Luật Doanh nghiệp 2020 (nội dung bắt buộc của nghị quyết, biên bản) và Thông tư 96/2020/TT-BTC (nghĩa vụ công bố).
  → Vì đều theo mẫu pháp lý nên báo cáo của FPT, CMC, CTR và các doanh nghiệp niêm yết khác **giống nhau về khung**; khác biệt chỉ ở cách trình bày và mức chi tiết.
- **Đã tạo bản nháp:** `data/formats/bctc.json`, `bctn.json`, `dhdcd.json` kèm `README.md`.
- **Phụ thuộc:** hoàn thiện đầy đủ **sau bước OCR** (bước 1.3), vì hiện 286/314 tài liệu chưa đọc được chữ. Trước mắt dựng bản nháp từ nhóm tài liệu đọc được (báo cáo thường niên, báo cáo quản trị, trang mục lục của BCTC).
- **Dùng để làm gì:** xác định tập dữ liệu cần xử lý · làm mốc tách đoạn theo mục · sinh metadata để lọc khi truy xuất · kiểm tra tính đầy đủ của tài liệu mới nạp.

### QĐ-026 · Bốn khối nền phải kiểm định trước khi mở rộng ✅
*(2026-09-21 · yêu cầu của mentor: "db core, model, inference, measure + test — mấy cái là nền để kiểm định trước")*
- **Quyết định:** làm và nghiệm thu **bốn khối nền** trên một **lát cắt mỏng, dữ liệu nhỏ**, rồi mới mở rộng ra toàn bộ dữ liệu và thêm tính năng.

| Khối | Nội dung chính | Hoàn thành khi | Mentor kiểm định bằng |
|---|---|---|---|
| **1. DB core** | Lược đồ MySQL (chỉ tiêu theo **mã nội bộ** + ánh xạ TT200/TT99, kế hoạch, cổ tức, phát hành, tài liệu, chunk, nhật ký kiểm toán), Flyway, nạp số liệu; collection Qdrant kèm payload | Truy vấn 5 chỉ tiêu × 3 năm khớp với PDF gốc; mã TT200 và TT99 cùng quy về một mã nội bộ | Sơ đồ lược đồ + bảng đối chiếu số liệu với PDF |
| **2. Model** | Chọn và chạy thử OCR, embedding, reranker (ViRanker và tầng 2), mô hình ngôn ngữ; mỗi nhóm là một cổng thay được bằng cấu hình | Mỗi mô hình chạy được từ Java; có bảng đo sơ bộ chất lượng, độ trễ, chi phí; chốt lựa chọn mặc định | Bảng so sánh mô hình kèm lý do chọn |
| **3. Inference** | Chuỗi xử lý chạy thông: chuẩn hoá → định tuyến → nhánh cơ sở dữ liệu / nhánh tài liệu (rerank hai tầng) → câu trả lời kèm trích dẫn → "không xác định" | 10 câu hỏi mẫu đủ ba nhánh chạy đúng, có trích dẫn tới số trang | Demo trực tiếp 10 câu |
| **4. Measure + Test** | Nhật ký kiểm toán đủ trường đo; công cụ chạy bộ test theo lô; bộ câu hỏi bản đầu (~40 câu); một hệ thống đối chứng (text-to-SQL) | Chạy 40 câu × 2 hệ thống, xuất bảng: độ chính xác, trích dẫn, độ trễ p50/p95, token, chi phí | Bảng kết quả và tệp nhật ký |

- **Lý do:**
  1. Bốn khối này là **xương sống**: thiếu bất kỳ khối nào thì không chứng minh được điều gì, dù có thêm bao nhiêu tính năng.
  2. Có Measure + Test ngay từ nền nên **mọi thay đổi về sau đều đo được**, không phải chạy lại thí nghiệm từ đầu.
  3. Khớp với tiêu chí VDT: tính hoàn thiện (30 điểm) đo bằng một hệ thống chạy trọn vẹn, chứ không phải nhiều mảnh rời rạc.
- **Hệ quả:** hạ tầng bổ trợ (Hexagonal đầy đủ, Kafka, Redis, Kubernetes, Keycloak) **chỉ làm sau khi nghiệm thu nền** (thống nhất với QĐ-023). Kế hoạch chi tiết và mốc thời gian ở [ke-hoach_v1.1.md](ke-hoach_v1.1.md).

### QĐ-027 · Chuẩn hoá trong tiền truy xuất: 6 quy tắc, thêm dạng chuẩn và giữ chữ gốc ✅
*(2026-10-11 · căn cứ: paper Hierarchical Reranker, mục 4.1 và Bảng 1; chi tiết: [truy-hoi_v1.3.md](truy-hoi_v1.3.md), mục 1.5–1.6)*
- **Quyết định:** một class `FinancialTextNormalizer` dùng chung cho câu hỏi (chế độ `QUERY`) và ngữ liệu (chế độ `CORPUS`), gồm 6 quy tắc theo thứ tự:
  1. NFC + khoảng trắng
  2. gõ không dấu → thêm dạng có dấu (chỉ câu hỏi)
  3. mở rộng viết tắt theo `viet-tat.yml`
  4. chuẩn hoá cách gọi kỳ (Q3/2024, bán niên, FY2023)
  5. chuẩn hoá đơn vị, số tiền về đồng
  6. điền ngữ cảnh còn thiếu từ metadata (chỉ câu hỏi): thời gian tương đối theo kỳ mới nhất trong `documents`, mặc định CTR và hợp nhất
- **Nguyên tắc:** **thêm dạng chuẩn vào sau, giữ nguyên chữ gốc** ("LNST (lợi nhuận sau thuế)"); chỉ NFC và khoảng trắng được thay hẳn. Câu gốc giữ riêng trong `NormalizedQuery.original`.
- **Lý do:**
  1. Paper đo được chuẩn hoá câu hỏi giúp tăng điểm (NDCG@20 0,7323 → 0,7446) và nhấn mạnh làm bằng quy tắc, không dùng LLM.
  2. Ngữ liệu chưa chuẩn hoá vẫn chứa dạng viết tắt; giữ cả hai dạng thì khớp cả hai phía mà không phải nạp lại.
  3. Quy tắc có thể đoán sai (thêm dấu, viết tắt nhập nhằng); giữ chữ gốc thì câu hỏi không bị hỏng.
  4. Chữ thường chỉ dùng khi so khớp, không áp lên đầu ra, vì viết tắt tiếng Việt phân biệt nhờ chữ hoa (lệch có chủ đích so với paper).
- **Chuẩn hoá ngữ liệu:** cờ `indexing.corpus-normalization` mặc định **tắt**. Trong Bảng 1 của paper, 4/5 cặp so sánh cho thấy chuẩn hoá ngữ liệu làm điểm giảm, và không có dòng nào tách được nó khỏi table-to-json. Chỉ bật sau thí nghiệm 2 × 2 (ngữ liệu nguyên bản / chuẩn hoá × bảng markdown / JSON) trên bộ 50 câu, khi chênh lệch có khoảng tin cậy bootstrap không chứa 0. Khi bật thì đổi collection và cờ cùng lúc.
- **Hệ quả:** collection hiện tại (`finq_pages_qwen3e8b`) dùng tiếp, không nạp lại. Bộ quy tắc tiếng Việt cho báo cáo tài chính là phần đề tài tự xây (paper không công bố bộ quy tắc).
