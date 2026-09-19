# Nhật ký — v1.0

*Bắt đầu: 2026-09-17 · Tuần 1 (17–27/9)*

---

## 2026-09-17 (thứ Năm)

### Chốt định hướng
- Đề tài FinQuery-RAG dựa trên hai paper: *FinAI Data Assistant* (gọi hàm thay cho text-to-SQL) và *Hierarchical Reranker* (RAG phân cấp).
- Doanh nghiệp mẫu là **CTR – Viettel Construction**, phạm vi gồm 6 mảng tài chính doanh nghiệp A–F.
- Khoảng năm **2019–2026** (QĐ-003).
- Đối chiếu mảng A–F với ba nguồn ban đầu thì thiếu các sự kiện sau nghị quyết, nên quyết định **bổ sung nguồn** (QĐ-004).
- Lập kế hoạch 6 tuần đến 31/10 ([ke-hoach_v1.0.md](ke-hoach_v1.0.md)).

### Viết lại tool thu thập (`backend/tai_bao_cao_ctr.py`, bản 2)
- **Phân tích website** (`viettelconstruction.com.vn`):
  - Có ba kiểu bố cục (timeline, bảng, thẻ). Bảng của mọi năm có sẵn trong HTML, chỉ bị ẩn.
  - Trang ĐHĐCĐ gom file theo nhóm cha (thuộc tính `value` của `td.title`).
  - Lớp chặn bot là cookie JS `D1N`, vượt qua được bằng `requests`.
  - Các trang lịch sử cổ tức, tăng vốn và cơ cấu sở hữu lấy dữ liệu từ API `wp-json/quanhecodong/v1/*`.
- Tool cũ (Playwright) chỉ lấy được 89/172 file BCTC. Lý do: tool click ô năm, nhưng việc click không lọc gì cả (QĐ-005).
- **Tool mới:**
  - đọc năm từ class `y-YYYY`,
  - phân loại theo tiêu đề web bằng bộ luật từ khoá có thứ tự,
  - nhận biết bản tiếng Anh và bản song ngữ,
  - đoán kỳ báo cáo (Q1–Q4 / 6T / 9T / N),
  - tải song song, loại trùng theo URL và SHA-256,
  - ghi `manifest.csv` gồm cả mục bị bỏ qua.
- Chạy thử ở chế độ `--liet-ke` nhiều vòng để tinh chỉnh luật phân loại CBTT và ĐHĐCĐ. Các lỗi đã sửa:
  - "Tờ trình thông báo BCTC" bị bỏ nhầm,
  - "Mẫu biên bản họp nhóm cổ đông" bị giữ nhầm,
  - "Nghị quyết HĐQT thông qua kết quả SXKD và kế hoạch" bị bỏ nhầm vì chứa cụm "kế hoạch tổ chức",
  - "thành lập Phòng Kiểm toán nội bộ" bị xếp nhầm vào nhóm đầu tư.

### Tải dữ liệu
- **Kết quả:** 613 mục trên web, **tải về 314 file (khoảng 2,4 GB, 10.424 trang), không file nào lỗi**, bỏ qua 292, trùng URL 7.
- Dữ liệu đặt ở `data/raw/CTR/` (QĐ-006). Thư mục cũ `backend/core-service/docs/` đã trống từ trước.
- **Phát hiện và xử lý:**
  - Web ghi "BCTC hợp nhất **Quý 4/2026**", thực chất là Q4/2025. Đã thêm vào `SUA_TAY`.
  - Hai file khác nhau cùng tên `BCTC-rieng.pdf` → thêm hậu tố tháng đăng.
  - File `VCC-...` **là của CTR** (VCC là tên viết tắt nội bộ). Nhận định trước đó sai.
  - API lịch sử cổ tức và tăng vốn **không được cập nhật sau 2022**; cơ cấu sở hữu dừng ở 30/09/2024.
  - Có các văn bản **đính chính BCTC 2021** và **trình bày lại số liệu theo Kiểm toán Nhà nước (01/2024)**, cần lưu ý khi nạp DB.

### Đánh giá chất lượng PDF (`backend/kiem_tra_pdf.py`)
- **Kết quả:** so_hoa 28 file, **ocr_kem 178**, **scan 108**.
- **Không có BCTC nào ở mức so_hoa.** Lớp chữ của BCTC là OCR kém do máy scan tạo ra, mất dấu.
- Kết luận:
  - cần bước OCR tiếng Việt khoảng 6.700 trang (QĐ-008),
  - số liệu có cấu trúc lấy từ vnstock, không lấy từ PDF (QĐ-009).
- Máy hiện chưa có Tesseract/PaddleOCR và không có GPU NVIDIA.

### Tài liệu
- Tạo thư mục `logs/` gồm README (quy ước phiên bản), phạm vi, dữ liệu, công nghệ, quyết định, kế hoạch và nhật ký, tất cả ở bản v1.0.
- Tạo `.gitignore` ở gốc repo để loại PDF thô khỏi git.

### Việc tiếp theo
- [ ] Chốt nhà cung cấp LLM (QĐ-010).
- [ ] Viết lại script kéo vnstock; kiểm tra độ phủ BCTC hợp nhất theo quý cho CTR từ 2019.
- [ ] Thiết kế schema DB (bảng chỉ tiêu theo kỳ có phân biệt hợp nhất/riêng; bảng `ke_hoach_kinh_doanh`, `co_tuc`, `phat_hanh`, `su_kien_dau_tu` có cột nguồn).
- [ ] Nhập tay kế hoạch, cổ tức, phát hành 2019–2026 từ nghị quyết và CBTT.
- [ ] Bắt đầu bộ câu hỏi chuẩn (mục tiêu khoảng 40 câu trong tuần 1).
- [ ] Build thử `core-service` với Spring AI.
- [ ] Thử công cụ OCR trên 5–10 trang BCTC mẫu.
- [ ] Cập nhật `chuyen_pdf_rag.py` để đọc `manifest.csv` và cấu trúc thư mục mới.

### Bổ sung chiều 17/09: kiến trúc nạp tài liệu
- **Câu hỏi 1:** OCR có nên nằm trong hệ thống không, để sau này nạp tài liệu scan mới vẫn xử lý được?
  - **Trả lời:** có. Đề xuất **pipeline nạp tài liệu** gồm hai phần:
    - `core-service` nhận file, kiểm tra an toàn, đẩy job vào hàng đợi Redis;
    - worker Python kiểm tra chất lượng **theo từng trang**, OCR khi cần, rồi chunk và embed.
  - Bộ dữ liệu hiện có cũng chạy qua đúng pipeline này ở chế độ chạy lô.
  - Ghi thành **QĐ-011**.
- **Câu hỏi 2:** đầu vào có thể là bất kỳ định dạng nào, và đều được đưa về một dạng chung cho RAG?
  - **Trả lời:** đúng. Mỗi định dạng có một bộ trích xuất, tất cả xuất ra **tài liệu chuẩn** (trang → khối heading/đoạn văn/bảng, kèm số trang, cách trích và độ tin cậy). Sau đó dùng một luồng chung: table-to-json → chunk → embed.
  - Trong đề tài, "mọi định dạng" giới hạn ở PDF (số hoá và scan), ảnh, DOCX, XLSX và HTML.
  - Ghi thành **QĐ-012**.
- Viết lại lý do chọn 2019–2026, có bảng đếm tài liệu theo năm từ manifest (**QĐ-003**).
- Kết quả là tạo `quyet-dinh_v1.1.md` (bản v1.0 giữ nguyên).
- Sửa nhận định sai trong v1.0 "tài liệu 2015–2018 nhiều bản scan": chưa đo nên không có căn cứ.
- **Việc tiếp theo bổ sung:**
  - [ ] Chốt QĐ-011 và QĐ-012, sau đó tạo `ke-hoach_v1.1.md` (tuần 2 phải xây worker nạp tài liệu, không chỉ chạy script).
  - [ ] Cập nhật `cong-nghe` v1.1: thêm `ml-service` (Python, gồm nạp tài liệu và rerank) và hàng đợi Redis.

### Bổ sung chiều 17/09 (2): định dạng đầu vào và ngôn ngữ cho phần nạp tài liệu
- **Làm rõ OCR:** OCR áp dụng cho trang PDF **không có chữ** và trang PDF **có chữ nhưng chữ hỏng**. Nhóm thứ hai lớn nhất: toàn bộ BCTC 2021–2026 bôi chọn được chữ nhưng mất dấu. OCR cũng áp dụng cho file ảnh. Việc có OCR hay không quyết định theo chất lượng từng trang, không theo việc chọn được chữ.
- **OCR nằm trong pipeline** dưới dạng một module duy nhất. 286 file hiện có được chạy lô bằng chính module đó (CLI); file tải lên đi qua hàng đợi.
- **File không phải PDF vẫn đi qua pipeline**, chỉ bỏ qua bước OCR. Đã lập bảng định dạng được hỗ trợ và bị từ chối, kèm giới hạn tải lên (≤ 100 MB, ≤ 1.000 trang, dựa trên file lớn nhất hiện có là 43,9 MB và file dài nhất là 518 trang) (QĐ-012 v1.2).
- Toàn bộ 613 tài liệu trên web CTR đều là PDF.
- **Java hay Python:** đề xuất Java kiểm tra (Tika), Python trích xuất, OCR và rerank. Có thể chuyển sang dùng Java hoàn toàn nếu cả OCR lẫn rerank đều gọi qua API (QĐ-013).
- Tạo `quyet-dinh_v1.2.md`.

### Bổ sung chiều 17/09 (3): chốt lõi hệ thống viết bằng Java
- **Yêu cầu của chủ đề tài:** khi triển khai, lõi gồm **RAG phân cấp và function calling SQL phải là Java**.
- Chốt **QĐ-013** (v1.3):
  - Java đảm nhận router, function calling, tiền truy xuất, nạp tài liệu (kể cả table-to-json và chunk), điều phối rerank, sinh câu trả lời và đánh giá.
  - Python chỉ còn làm công cụ ngoài hệ thống (thu thập, đo chất lượng), và nếu cần thì làm dịch vụ chạy mô hình không chứa logic (OCR, reranker/embedding tự host).
- **QĐ-011:** worker nạp tài liệu là Java; OCR là dịch vụ bên ngoài trả kết quả thô. Kiến trúc chuyển sang ✅.
- **QĐ-012:** bộ trích xuất dùng PDFBox, POI, Jsoup, Commons CSV, Tika.
- **Rủi ro mới:**
  - `chuyen_pdf_rag.py` phải viết lại bằng Java.
  - Reranker tầng 2 (Qwen3-Reranker-8B) không chạy được trên máy không có GPU, nên phải dùng API hoặc thay bằng mô hình nhỏ hơn.
  - Phiên bản Spring AI phải khớp với Boot 4.
- Tạo `quyet-dinh_v1.3.md` và `cong-nghe_v1.1.md`.
- **Việc tiếp theo bổ sung:**
  - [ ] Build thử Spring AI với Boot 4 (việc chặn đường).
  - [ ] Tạo `ke-hoach_v1.1.md`: tuần 2 gồm worker Java và việc chuyển `chuyen_pdf_rag.py` sang Java.

### Bổ sung chiều 17/09 (4): hạ Spring Boot về 3.x
- Theo yêu cầu: `backend/core-service/pom.xml` đổi từ **Spring Boot 4.0.8 → 3.5.16**, **Spring AI 1.1.7 → 1.1.8**.
- Đã tra Maven Central:
  - Boot 3.x mới nhất là 3.5.16.
  - Spring AI 1.1.8 phụ thuộc Boot 3.5.15, tức là khớp.
  - Dòng Spring AI 2.0.x là dòng dành cho Boot 4.
- Đổi starter: `webmvc` → `web`; `spring-boot-starter-flyway` → `flyway-core`; bốn starter test của Boot 4 → `spring-boot-starter-test`. Gộp cấu hình Lombok.
- **Kiểm chứng:** `mvnw test-compile` thành công. Chưa chạy `contextLoads` vì chưa cấu hình MariaDB.
- Xác nhận BOM 1.1.8 có sẵn: `spring-ai-rag`, vector store MariaDB, Transformers (ONNX), cùng các starter LLM (Anthropic, Google GenAI, DeepSeek, Mistral, Ollama…).
- Tạo `quyet-dinh_v1.4.md` (QĐ-014) và `cong-nghe_v1.2.md`.

### Brainstorm (17/09): chủ đề 1, người dùng mục tiêu (đã chốt)
- **Người dùng:** nội bộ doanh nghiệp, cụ thể là **phòng kế toán và phòng tài chính** (phương án B). Định vị là "trợ lý phân tích tài chính cho doanh nghiệp"; dữ liệu thử nghiệm là tài liệu công bố của CTR.
- **Lý do:**
  - chữ "an toàn" trong tên đề tài có ý nghĩa thật trong môi trường doanh nghiệp;
  - mảng E (kế hoạch so với thực hiện) và mảng A (khoản vay, tài sản đảm bảo) là nhu cầu thật của phòng tài chính;
  - phân biệt được với các trang dữ liệu công khai: hệ thống giải thích số liệu kèm bằng chứng và kết hợp nhiều nguồn.
- **Bắt buộc có đăng nhập.** Chi tiết vai trò và phân quyền: đang brainstorm.
- Sẽ ghi vào `pham-vi_v1.1.md` khi kết thúc brainstorm.
- **Chủ đề 1b (phân quyền):** đồng ý hướng 3 vai trò (`NHAN_VIEN`, `QUAN_LY_TAI_LIEU`, `QUAN_TRI`) cộng **nhật ký kiểm toán** cho mỗi câu hỏi (nhánh, hàm đã gọi, tham số, mẫu SQL, nguồn tài liệu, token, chi phí, độ trễ). Nhật ký này dùng luôn làm nguồn số liệu cho phần đánh giá.
- Chủ đề tài muốn **đăng nhập bằng SSO**. Đây là phần phụ, **không phải lõi** (lõi vẫn là function calling SQL và RAG phân cấp). Đang brainstorm.
- Chủ đề tài không có nền tảng tài chính → sư phụ giải thích danh mục câu hỏi và các khái niệm cơ bản trước khi chốt.
- **Chốt:** SSO dùng **Keycloak**; người dùng là **nội bộ doanh nghiệp (phòng kế toán, tài chính)**. Chủ đề tài sẽ nhờ anh/chị ở các phòng này trong công ty rà lại danh mục câu hỏi. Ghi thành **QĐ-015** và **QĐ-016** (`quyet-dinh_v1.5.md`).
- Tạo **`tu-dien-tai-chinh_v1.0.md`**: các loại tài liệu, thuật ngữ theo A–F, mã chỉ tiêu BCTC (TT200/202, cần đối chiếu), công thức chỉ số, bẫy thường gặp, viết tắt. Dùng cho chủ đề tài đọc, và làm ngữ cảnh khi thiết kế.

## 2026-09-19

### Đọc từ điển tài chính
- Chủ đề tài hỏi về *hợp nhất/riêng* và *kiểm toán/soát xét/tự lập*. Sư phụ giải thích bằng ví dụ "sổ thu chi của bố mẹ và của cả nhà", và "tự chấm, thầy liếc qua, thầy chấm kỹ".
- **Phản hồi của chủ đề tài:** từ điển nhiều thuật ngữ quá, không nên học thuộc hết.
- **Xử lý:** tạo `tu-dien-tai-chinh_v1.1.md`, thêm **mục 0: sáu ý cần nhớ** (một trang). Phần còn lại chuyển thành phần tra cứu, dùng cho thiết kế và làm ngữ cảnh cho sư phụ.
- **Kiểm tra trái phiếu:** tìm cụm "trái phiếu" trong 7 BCTN (2019–2025) và 12 báo cáo quản trị (2020–2026, bản đọc được chữ) → **0 lần xuất hiện**. BCTN 2019 là bản scan nên không tìm được chữ. Cộng với 613 tiêu đề trên web cũng không có văn bản nào về trái phiếu → **gần như chắc chắn CTR không phát hành trái phiếu giai đoạn 2019–2026**. Nếu có thì trái phiếu sẽ nằm ở mục "Vay và nợ thuê tài chính" trên bảng CĐKT và phần thuyết minh khoản vay. Câu hỏi về trái phiếu CTR dùng làm **câu bẫy** "không có thông tin".

### Brainstorm chủ đề 3: ranh giới phạm vi (đã chốt)
- Bốn kiểu phản hồi: trả lời / trả lời một phần / không xác định / từ chối.
- 10 loại câu ở ranh giới: dự báo, khuyên đầu tư, so sánh công ty khác, giá cổ phiếu, câu tổng hợp mở, ngoài khoảng năm, thông tin không công bố, yêu cầu ghi/sửa dữ liệu, tấn công prompt/SQL, ngoài tài chính.
- **Chốt:**
  - **có hội thoại nhiều lượt**,
  - **chỉ tiếng Việt**,
  - câu trả lời gồm **văn bản + bảng** (bắt buộc), **biểu đồ** là hướng bổ sung.

  Ghi thành QĐ-017.
- Giải thích ConvFinQA cho chủ đề tài.
- Chủ đề tài muốn dùng **ViRanker** (Dang Phuong-Nam, Nguyen Kieu-Linh). Đã tra thông tin:
  - khoảng 0,6B tham số, dựa trên BGE-M3, 512 token, Apache 2.0,
  - trên MMARCO-VI: NDCG@10 0,7302.

  Đề xuất dùng ở tầng 1 (QĐ-018, 🟡).
- Tạo `quyet-dinh_v1.6.md`.
- **ViRanker qua API?** Đã kiểm tra `config.json` trên Hugging Face: kiến trúc là `XLMRobertaForSequenceClassification` chuẩn (cùng họ với BGE-Reranker-v2-m3), có nhãn `text-embeddings-inference` và `endpoints_compatible`, nhưng `inference: None`, tức là **không có API miễn phí dùng ngay**. Các cách phục vụ qua HTTP:
  - HF Inference Endpoints (TEI),
  - GPU serverless (Modal…),
  - container TEI trên máy thuê GPU,
  - ONNX INT8 chạy CPU.

  Chưa chốt; ghi vào QĐ-018 khi chốt.
- **Chốt QĐ-018:** tầng 1 = ViRanker, tầng 2 = mô hình rerank của nhà cung cấp. Nếu máy không chạy được thì cả hai tầng gọi qua API. Lưu ý đưa vào báo cáo: khi dùng dữ liệu nội bộ thật thì nên tự host để dữ liệu không ra ngoài. Tạo `quyet-dinh_v1.7.md`.
- Làm rõ với chủ đề tài: ConvFinQA là **bộ đề kiểm tra (benchmark)**. Mỗi mẫu là một hội thoại, mỗi lượt có câu hỏi, đáp án chuẩn và các bước tính. Bộ đánh giá của FinQuery sẽ theo cấu trúc tương tự.

### Tiêu chí VDT 2026 và mục tiêu học hạ tầng
- Chủ đề tài gửi bảng **tiêu chí chấm Sáng kiến Công nghệ VDT 2026**: hiệu quả kinh tế 30, tính mới 20, tính ứng dụng 20, tính hoàn thiện 30. **Ưu tiên hiệu quả và hoàn thiện.** Ghi vào `tieu-chi-vdt_v1.0.md`, kèm cách FinQuery đáp ứng từng tiêu chí.
- **Hệ quả lớn nhất:**
  - phải **đo hiệu quả với người dùng thật** (thời gian tra cứu thủ công so với dùng FinQuery),
  - phải **giữ phạm vi vừa sức** để đạt mức hoàn thiện ≥ 80%.
- Chủ đề tài muốn **làm chủ Kafka, Redis, Docker, K8s** trong hơn một tháng tới (công ty đang dùng). Đề xuất vai trò thật cho từng công nghệ (QĐ-019, 🟡). Kafka thay hàng đợi Redis trong pipeline nạp tài liệu (lợi ích: phát lại, nhiều bên cùng đọc).
- **Kiểm tra máy:** Docker 29.5, Compose v5.1, kubectl 1.34 có sẵn; **RAM khoảng 7,6 GiB**, 8 luồng CPU, không có GPU → không đủ chạy đồng thời toàn bộ, cần chiến lược (xem QĐ-019).
- Tạo `quyet-dinh_v1.8.md`.
- **Chốt QĐ-019** (hạ tầng), điều kiện: lõi nghiệp vụ phải xong trước.
- Chủ đề tài đề xuất **Clean/Hexagonal**, hệ thống là monolith → đồng ý. Chọn **modular monolith + hexagonal** (QĐ-020):
  - 8 module: `qa`, `sqlquery`, `retrieval`, `ingestion`, `evaluation`, `audit`, `identity`, `shared`;
  - các port chính có nhiều adapter: LLM, reranker, OCR, embedding, event, bộ nhớ hội thoại, và chiến lược trả lời (FinQuery / 2 baseline);
  - một jar chạy hai vai trò `api` và `worker`.
- Đã kiểm tra **Spring Modulith 1.4.13** được build trên Spring Boot 3.5.16 (khớp); ArchUnit mới nhất là 1.5.0.
- Tạo `quyet-dinh_v1.9.md`.
- **Chốt đặt tên** tiếng Anh trong code, tiếng Việt trong chú thích và tài liệu.
- Giải thích **Clean và Hexagonal**: cùng luật phụ thuộc; thiết kế hiện tại *chính là* Clean Architecture được hiện thực theo kiểu Ports & Adapters, lược bỏ Presenter và Output Boundary cho gọn.
- Chủ đề tài có tài khoản **AWS**. Chính sách từ 15/07/2025: 100 USD khi đăng ký + tối đa 100 USD nữa; gói Free tự đóng sau 6 tháng. Chọn **k3s trên EC2** (t3.large, Singapore), **chỉ bật khi dùng**, đặt cảnh báo Budgets; ước tính dưới 20–30 USD.
- Tạo `quyet-dinh_v1.10.md`.
- Chủ đề tài chốt **gọi là Hexagonal** (không gọi là Clean) → `quyet-dinh_v1.11.md`.
