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
- Chủ đề tài **chưa có tài khoản AWS**. Hướng dẫn đăng ký gói **Free**: 100 USD khi đăng ký, cộng tối đa 100 USD nữa khi làm 5 hoạt động (mỗi hoạt động 20 USD: EC2 bật rồi tắt, RDS, Lambda, Bedrock, **Budgets**), phải làm xong trong 6 tháng. Việc đầu tiên sau khi đăng ký: bật MFA cho root và tạo Budgets (Budgets cũng là một hoạt động được thưởng credit).
- Dự phòng K8s bằng Docker Desktop; máy dự kiến nâng lên khoảng 12 GB RAM. Làm rõ **Kafka không cần 3 broker khi phát triển**: dùng 1 node KRaft gộp, RF = 1; 3 broker là cấu hình production; làm một buổi thí nghiệm 3 broker để học failover. Tạo `quyet-dinh_v1.12.md`.
- Chủ đề tài nhấn mạnh: đây là **đề tài vừa học vừa làm**. Chủ đề tài đã nắm ý tưởng 2 paper, biết Java nhưng chưa làm RAG/LLM bằng Java; sư phụ là **mentor thứ hai** (hỗ trợ từ thiết kế đến code). Tạo `hoc-tap_v1.0.md`: cách làm việc, cách chia việc code, bản đồ kỹ năng, nhật ký bài học.

## 2026-09-20

### Báo cáo tiến độ gửi mentor
- Mentor: **Đỗ Duy Tùng**, đơn vị **Viettel Digital Finance**.
- Tên rút gọn dùng cho báo cáo: **FinQuery — Hệ thống hỏi đáp tài chính an toàn và hiệu quả** (tên đầy đủ vẫn là FinQuery-RAG…).
- Tạo `D:\VDT_Reports\Nguyen_Van_Hung_FinQuery_BaoCaoTienDo.docx`: bìa theo mẫu báo cáo giai đoạn 1 (Times New Roman, căn giữa), mục lục tự động, và khung 9 mục + 2 phụ lục. Các mục chưa viết nội dung được đánh dấu "[Nội dung dự kiến]".
- Đã điền sẵn: loại ứng dụng, bảng chức năng theo vai trò, bảng phạm vi A–F, ba tài liệu tham khảo kèm đường dẫn (FinAI Data Assistant arXiv 2510.14162; Hierarchical Reranking arXiv 2607.27523; ViRanker arXiv 2509.09131 + Hugging Face), bảng công nghệ, danh sách kiến thức học được, ràng buộc và phần ngoài phạm vi.
- Kiểm chứng: xuất PDF bằng Word COM, 10 trang, bìa và mục lục hiển thị đúng.
- Cập nhật báo cáo theo góp ý: bỏ nhãn "[Nội dung dự kiến]" trong thân bài, thay bằng **"(dự kiến)" cạnh tiêu đề** và viết luôn nội dung dự kiến bên dưới; mục 6 bỏ kế hoạch 6 tuần và thứ tự cắt giảm, thay bằng **kế hoạch tuần tới: bắt đầu triển khai hệ thống**; mục 7 chỉ còn 7.1–7.3. Yêu cầu chức năng (CN-01…CN-10) và phi chức năng (PCN-01…PCN-08) đã liệt kê thành bảng. Báo cáo hiện 15 trang. Giữ nguyên chỉnh sửa của chủ đề tài ở mục 3.3 (còn 2 ràng buộc); bản cũ được sao lưu trong thư mục tạm.
- Theo yêu cầu: **xoá mục 8 (rủi ro) và mục 9 (đề xuất, hỗ trợ từ mentor)** khỏi báo cáo, sửa trực tiếp trên bản chủ đề tài đang chỉnh (không dựng lại file), giữ nguyên mọi chỉnh sửa khác. Báo cáo còn 14 trang. Nội dung rủi ro và đề xuất vẫn được lưu trong logs (`quyet-dinh`, `ke-hoach`) nếu cần dùng lại.
- Rút gọn báo cáo tiến độ xuống **5 trang** (bìa + 4 trang nội dung) để mentor nắm nhanh: gộp mục 1 thành một bảng tóm tắt, mục 2 thành bảng 3 dòng kèm đường dẫn, yêu cầu chức năng xếp 2 cột, bỏ hai phụ lục. **Thêm logo Viettel lên bìa** (lấy từ báo cáo giai đoạn 1). Bản 14 trang được sao lưu trong thư mục tạm.

## 2026-09-21

### Đối chiếu format 10-K với BCTC riêng bán niên 2026 của CTR
- **Kết luận 1:** một tệp BCTC của Việt Nam chỉ tương ứng **Item 8** của Form 10-K (báo cáo tài chính và thuyết minh). Các phần còn lại của 10-K (mô tả kinh doanh, rủi ro, MD&A, quản trị, thù lao, giao dịch bên liên quan) nằm rải ở BCTN, báo cáo quản trị, tài liệu ĐHĐCĐ và CBTT → **khẳng định quyết định QĐ-004 (nhiều nguồn) là đúng**.
- **Kết luận 2 (quan trọng):** mã chỉ tiêu **thay đổi giữa các kỳ**. BCTC riêng 2026 gọi là "Báo cáo tình hình tài chính" và đổi mã: tổng tài sản 270 → **280**, BĐS đầu tư 230 → 240, tài sản dở dang 240 → 250, đầu tư tài chính dài hạn 250 → 260, dự phòng HTK 149 → 142… Chưa xác minh văn bản pháp lý. Ghi vào `tu-dien-tai-chinh_v1.2.md` (mục 3.0) kèm hệ quả: schema phải dùng **mã nội bộ + bảng ánh xạ theo chế độ trình bày**.
- **Kết luận 3:** tệp 2026 là bản **song ngữ** (33 trang tiếng Việt + phần tiếng Anh), nên khi nạp phải cắt bỏ phần tiếng Anh.
- **Kết luận 4:** báo cáo bán niên **không có báo cáo kiểm toán**, chỉ có chữ ký Ban Tổng Giám đốc; số mục thuyết minh ít hơn báo cáo năm.
- Gom các bộ test đã thảo luận thành `danh-gia_v1.0.md`: T1 bộ câu hỏi chuẩn, T2 hội thoại nhiều lượt, T3 an toàn, T4 "không xác định", T5 so sánh reranker, T6 so sánh mô hình ngôn ngữ, T7 đo hiệu năng, T8 đo với người dùng thật. Kèm cách đo **độ trễ theo từng chặng** và **token, chi phí theo từng lần gọi mô hình**, tất cả lấy từ nhật ký kiểm toán.

### Cập nhật theo góp ý mentor (21/09)
- **Qdrant** thay MariaDB VECTOR làm cơ sở dữ liệu vector (QĐ-021); Spring AI 1.1.8 có sẵn starter Qdrant. MariaDB giữ số liệu, sự kiện, nhật ký.
- **Phân rã bài toán**: tiền xử lý → xử lý → đánh giá, mỗi pha chia nhỏ tiếp (QĐ-022).
- **Thứ tự ưu tiên**: lõi trước; Hexagonal, Kafka, Redis, K8s, Keycloak làm sau rồi dựng lại (QĐ-023). Giữ ba ranh giới ngay từ đầu: cổng mô hình, cổng chiến lược trả lời, cổng lưu trữ vector.
- **Đánh giá thêm mô hình chuyên tiếng Việt** (PhoBERT, PhoRanker…) (QĐ-024): tách T5 thành T5a (embedding) và T5b (reranker) trong `danh-gia_v1.1.md`; lưu ý PhoBERT cần tách từ và giới hạn ~256 token nên ảnh hưởng tới kích thước chunk.
- **Hồ sơ cấu trúc tài liệu** (QĐ-025): tạo `data/formats/{bctc,bctn,dhdcd}.json` và README.
- **Tra cứu pháp lý:** Thông tư 99/2025/TT-BTC thay Thông tư 200 từ 01/01/2026 (B01-DN đổi tên thành "Báo cáo tình hình tài chính") → đúng hiện tượng ở BCTC 2026 của CTR. BCTN theo Phụ lục IV Thông tư 96/2020/TT-BTC. Vì theo mẫu pháp lý nên báo cáo của FPT, CMC, CTR cùng khung.
- Tạo `quyet-dinh_v1.13.md`, `cong-nghe_v1.3.md`, `danh-gia_v1.1.md`.
- **Mentor chốt bốn khối nền cần kiểm định trước: DB core → Model → Inference → Measure + Test** (QĐ-026). Kế hoạch tổ chức lại theo bốn khối, mỗi khối có định nghĩa hoàn thành và cách mentor kiểm chứng; hạ tầng bổ trợ chỉ làm sau khi nghiệm thu nền. Tạo `quyet-dinh_v1.14.md` và `ke-hoach_v1.1.md`.
- **Chiến lược đánh giá ba lớp** (giảm công gán nhãn): L1 bộ chuẩn công khai **VN-MTEB** (41 tập, có VieQuAD, Zalo Legal, mMARCO-VI) · L2 bộ sinh tự động 300–500 cặp từ chính chunk của CTR · L3 bộ tự làm 40–60 câu (không thể bỏ: câu kết hợp, trích dẫn, an toàn, "không xác định").
- **Trục đánh giá thứ hai theo yêu cầu của chủ đề tài:** đổi mô hình thì **pipeline phải đổi gì**. Thêm **T9 thử nghiệm tương thích**, **phiếu tương thích mô hình** (tách từ, độ dài tối đa, tiền tố, số chiều, cách phục vụ, giấy phép, chi phí tạo lại chỉ mục) và **thang M0–M3** kèm quy tắc kết luận. Tạo `danh-gia_v1.2.md`.
- Chủ đề tài lưu ý: phần đánh giá hiện **mới là ý tưởng, chưa chốt**; đổi mô hình có phải sửa pipeline hay không thì **đến lúc làm mới biết**. Đã thêm phần trạng thái vào `danh-gia_v1.3.md`, và quy ước: làm tới đâu thì cập nhật lại bằng số đo thật tới đó.
- **Khung nghị quyết** (chủ đề tài phát hiện khi đọc tài liệu CTR và FPT): nghị quyết theo quy ước soạn thảo văn bản hành chính — tiêu đề, phần "Căn cứ", dòng "QUYẾT NGHỊ", **các Điều** (mỗi Điều là một nội dung thông qua, thường kèm tỷ lệ biểu quyết), điều khoản thi hành, chữ ký. Giá trị: mỗi Điều là **ranh giới tách đoạn tự nhiên**; tỷ lệ biểu quyết là **trường có cấu trúc**; phần "Căn cứ" liên kết nghị quyết với biên bản. Khung này **dùng chung cho nhiều doanh nghiệp**, củng cố khả năng mở rộng của hệ thống. Đã ghi vào `data/formats/dhdcd.json` (khối `cau_truc_nghi_quyet`), kèm các biến thể đã gặp và ghi rõ **mới kiểm bằng mắt trên vài file đọc được chữ, cần kiểm lại sau OCR**.
- Đồng thời ghi rõ trong file: khung ĐHĐCĐ **rút từ 75 tài liệu thật của CTR**, không phải từ mẫu pháp lý (loại tài liệu này không có mẫu do luật ban hành); hai dòng trích dẫn luật để trạng thái **chưa kiểm chứng**.

### Sửa lại hồ sơ cấu trúc BCTC theo bản gốc mẫu B09-DN
- Chủ đề tài cung cấp **bản gốc Mẫu số B09-DN "Bản thuyết minh Báo cáo tài chính" ban hành kèm Thông tư 99/2025/TT-BTC ngày 27/10/2025** và nhận xét `data/formats/bctc.json` chưa chính xác. **Đúng**: bản v1 coi thuyết minh là một mục duy nhất rồi tự liệt kê vài "mục con quan trọng"; trong khi mẫu có cấu trúc cố định.
- **Cấu trúc thật của B09-DN:** I. Đặc điểm hoạt động · II. Kỳ kế toán, đơn vị tiền tệ · III. Chuẩn mực và chế độ kế toán · IV. Chính sách kế toán (29 mục) · **V. Thông tin bổ sung cho Báo cáo tình hình tài chính (32 mục)** · **VII. Thông tin bổ sung cho Kết quả kinh doanh (11 mục)** · VIII. Thông tin bổ sung cho Lưu chuyển tiền tệ (5 mục) · IX. Những thông tin khác (8 mục) · X. Nội dung sửa đổi biểu mẫu. Không thấy phần VI trong bản gốc → cần kiểm lại trên toàn văn thông tư.
- **Ba điều quan trọng rút ra:**
  1. **Doanh nghiệp được chủ động đánh số thứ tự thuyết minh** (mục 2.d và 4.6 của mẫu) → khi nhận dạng phải **khớp theo tên mục, không theo số thứ tự**.
  2. Báo cáo giữa niên độ chỉ lập **thuyết minh chọn lọc theo VAS 27** → giải thích vì sao BCTC bán niên 2026 của CTR ít mục hơn báo cáo năm; thiếu mục **không** coi là lỗi.
  3. Cột "Thuyết minh" trên B01, B02, B03 **trỏ tới mục trong B09** → đây là liên kết sẵn có giữa con số và đoạn văn giải thích, dùng được cho trích dẫn.
- **Ánh xạ mục thuyết minh sang 6 mảng A–F** đã ghi vào file, ví dụ: V.16 Vay và nợ thuê tài chính → A · V.27 Vốn chủ sở hữu (gồm cổ phiếu, cổ tức) → A và F · V.30 Tài sản dùng cầm cố thế chấp → A · VII.10 Chi phí theo yếu tố → C · **IX.4 Báo cáo bộ phận theo VAS 28 → C (nguồn duy nhất cho câu hỏi doanh thu, lợi nhuận theo mảng kinh doanh)** · IX.3 Bên liên quan → A và D. Mảng E (kế hoạch so với thực hiện) **không có trong BCTC** — đúng như đã xác định, phải lấy từ nghị quyết ĐHĐCĐ và BCTN.
- Ghi `data/formats/bctc.json` bản v2, đánh dấu căn cứ TT99 là **đã đối chiếu bản gốc**; phần **TT200 vẫn để trạng thái chưa đối chiếu** (cấu trúc B09-DN của TT200 tương tự nhưng không trùng khớp, mà 2019–2025 toàn bộ theo TT200).
- Tạo `hoc-tap_v1.1.md`: thêm mục **phân rã đệ quy như một cách làm việc** (quy tắc 3 câu hỏi, **điều kiện dừng**: nửa ngày + chạy ra kết quả nhìn thấy được + sai thì khoanh vùng được ngay, cái bẫy chia ngang và cách chữa bằng lát cắt dọc mỏng); **dựng lại bản đồ kỹ năng** theo bốn khối nền thay cho cách chia tuần 1–6, thay MariaDB VECTOR bằng Qdrant, bổ sung OCR, mô hình chuyên tiếng Việt, đọc mẫu biểu kế toán, đo độ trễ và chi phí; bổ sung 6 dòng nhật ký bài học ngày 21/09.

### Sửa lại hồ sơ cấu trúc BCTN theo bản gốc Phụ lục IV và BCTN 2025 thật của CTR
- Chủ đề tài cung cấp **toàn văn Phụ lục IV Thông tư 96/2020/TT-BTC** (mẫu song ngữ) và chỉ sang **BCTN 2025 của CTR** (`BC-Thuong-nien-2025_TV_260409_14h.pdf`, 78 trang). Đã đối chiếu cả ba: bản gốc, file json cũ, báo cáo thật.
- **Bản v1 sai thứ bậc:** để 11 mục phẳng, trong khi mẫu có **sáu phần I–VI**, và *Ngành nghề*, *Mô hình quản trị*, *Định hướng phát triển*, *Các rủi ro* là **mục con của I. Thông tin chung**. Thiếu hẳn: giao dịch cổ phiếu quỹ, thù lao và giao dịch của người nội bộ, ý kiến kiểm toán, và toàn bộ 8 mục ESG.
- **BCTN 2025 của CTR bám sát mẫu:** PHẦN 1–6 ứng một-một với I–VI, các bảng tái hiện gần như nguyên văn. Nhưng CTR **đặt lại tên và thêm mục** ("Quản trị rủi ro" thay "Các rủi ro"; thêm thông điệp Chủ tịch, thông điệp TGĐ, "Nhìn lại năm", "Diễn biến giao dịch cổ phiếu"), và **mục thù lao + giao dịch người nội bộ nằm trong thân bài trang 59, không lên mục lục** → nhận dạng mục phải khớp theo **ý nghĩa**, không khớp cứng theo tên.
- **Phát hiện giá trị nhất — bảng hệ số tài chính do luật quy định** (mục II.4.b): 4 nhóm, 10 hệ số, **kèm sẵn công thức trong chính văn bản pháp luật** (thanh toán ngắn hạn, thanh toán nhanh, nợ/tổng tài sản, nợ/vốn chủ, vòng quay HTK, vòng quay tổng tài sản, LNST/DTT, ROE, ROA, LN HĐKD/DTT). CTR công bố đủ ở trang 27–28. → Dùng làm **bộ đối chứng** cho tỷ số hệ thống tự tính từ BCTC: lệch là biết ngay sai công thức hay sai số đầu vào.
- **Phát hiện thứ hai — mảng E có nguồn pháp định trong BCTN**, không chỉ ở nghị quyết ĐHĐCĐ: mục II.1 bắt buộc "tình hình thực hiện so với kế hoạch" kèm phân tích nguyên nhân; mục III.1 bắt buộc nêu **nguyên nhân và trách nhiệm của Ban Giám đốc** khi không đạt kế hoạch; mục III.4 và IV.3 là kế hoạch tương lai. Ở CTR, **trang 34** có bảng đủ cột `TH năm trước · KH · TH · %KH · ∆ · %` cho tổng doanh thu, tổng chi phí, LNTT, LNST, ROA, ROE — **tốt hơn nghị quyết vì nghị quyết chỉ có số kế hoạch**.
- **Bẫy số liệu đã gặp thật:** thư Tổng Giám đốc (trang 5) viết "doanh thu 14.050 tỷ và **lợi nhuận sau thuế 745,1 tỷ**", trong khi bảng trang 34 ghi LNTT 746 tỷ, **LNST 600 tỷ**, và bảng trang 27 ghi doanh thu thuần 13.940 tỷ. Số trong thư khớp với **lợi nhuận trước thuế**, và "doanh thu" là **tổng** doanh thu chứ không phải doanh thu **thuần**. → Quy tắc: **chỉ lấy số từ bảng, không lấy từ văn xuôi**; đồng thời là một ca kiểm thử tốt cho nhóm câu hỏi an toàn.
- Ghi `data/formats/bctn.json` bản v2 kèm khối `doi_chieu_voi_CTR`. Còn lại: đối chiếu thêm BCTN của FPT hoặc CMC; và kiểm BCTN 2019–2020 vì Thông tư 96/2020 hiệu lực từ 01/01/2021, trước đó là Thông tư 155/2015 (**chưa kiểm chứng**).

### Đo va chạm mã chỉ tiêu giữa TT200 và TT99 trên tài liệu thật
- Câu hỏi: TT200 hay TT99 "hết hạn"? **Trả lời: hết hiệu lực để LẬP báo cáo mới ≠ hết giá trị của báo cáo đã lập.** TT200 hết hiệu lực từ kỳ kế toán 2026, nhưng BCTC 2019–2025 vĩnh viễn là tài liệu hợp lệ và không được lập lại → **phải xử lý cả hai chế độ**, không được bỏ chế độ nào.
- **Đo được (BCTC riêng bán niên 2026, trang 3):** báo cáo có hai cột `30/06/2026` và `01/01/2026`; cột thứ hai là số cuối năm 2025 nhưng **mang mã của TT99**. Kiểm chứng bằng một con số cụ thể — dự phòng giảm giá hàng tồn kho tại 31/12/2025 = `(3.624.149.036)`:
  - BCTC riêng 2025 (TT200) → **mã 149**
  - BCTC riêng 6T/2026 (TT99), cột 01/01/2026 → **mã 142**
- **Hệ quả 1 (lợi):** có nguồn đối chứng miễn phí — nạp cả hai tài liệu, nếu bảng ánh xạ đúng thì hai bản ghi phải trùng giá trị.
- **Hệ quả 2 (bắt buộc):** cùng một giá trị xuất hiện ở hai tài liệu với hai mã → khóa duy nhất của bảng số liệu phải là **(mã nội bộ, ngày, phạm vi hợp nhất/riêng)**, KHÔNG được là (mã báo cáo, ngày).
- **Ví dụ va chạm mã rõ hơn cả 270→280:** mã **136** tồn tại ở cả hai chế độ nhưng đổi nghĩa và đổi dấu — TT200: *Phải thu ngắn hạn khác* (dương); TT99: *Dự phòng phải thu ngắn hạn khó đòi* (âm). Ở TT99, phải thu khác chuyển sang mã 135; ở TT200, dự phòng nằm ở mã 137. Truy vấn cộng "mã 136" xuyên 2025–2026 sẽ cộng một khoản phải thu với một khoản dự phòng âm mà **không ném ra lỗi nào**.
- Đây là luận cứ mạnh cho đề tài: hệ đối chứng **text-to-SQL thuần sinh truy vấn trực tiếp trên mã báo cáo sẽ sai im lặng ở chỗ này**. Nên đưa vào bộ câu hỏi chuẩn như một ca kiểm thử riêng.
- Chưa kiểm chứng: điều khoản chuyển tiếp của TT99 (có doanh nghiệp nào được áp dụng muộn không). Riêng CTR thì đo được là áp dụng ngay từ Q1/2026.
- Chủ đề tài yêu cầu `bctc.json` **chỉ giữ khung chung do pháp luật ban hành** (từ `cau_truc_thuyet_minh_B09` trở đi), vì mẫu B09-DN bắt đầu ở "I. Đặc điểm hoạt động của doanh nghiệp" — không có mục lục, báo cáo Ban Tổng Giám đốc hay báo cáo kiểm toán. Ghi bản v3. **Nội dung đã gỡ, chép lại đây để lấy lại khi cần:**
  - `muc_chuan` (cấu tạo tệp PDF, là quan sát thực tế chứ không thuộc mẫu): `muc_luc` (bỏ qua) · `bao_cao_ban_tgd` · `bao_cao_kiem_toan` (chỉ có ở kỳ 6T và N, chứa loại ý kiến kiểm toán) · `b01` Báo cáo tình hình tài chính / Bảng cân đối kế toán (mã khác nhau giữa hai chế độ, có cột "Thuyết minh") · `b02` Kết quả hoạt động kinh doanh · `b03` Lưu chuyển tiền tệ (báo cáo quý là số lũy kế từ đầu năm) · `b09` · `chu_ky`. Vai trò: b01, b02, b03 = `co_so_du_lieu`; các mục còn lại = `chi_muc_tai_lieu` hoặc `bo_qua`.
  - `metadata_bat_buoc`: ma_cong_ty, che_do_ke_toan, ky_bao_cao, nam, pham_vi_hop_nhat, muc_kiem_tra, ngay_ky, don_vi_tien, ngon_ngu.
  - `gia_tri_cho_phep`: che_do_ke_toan ∈ {TT200, TT99} · ky_bao_cao ∈ {Q1, Q2, Q3, Q4, 6T, N} · pham_vi_hop_nhat ∈ {hop_nhat, rieng} · muc_kiem_tra ∈ {tu_lap, soat_xet, kiem_toan} · ngon_ngu ∈ {vi, en, vi+en}.
  - Hai quy tắc kiểm tra bị gỡ theo: "thiếu một trong b01/b02/b03/b09 thì đánh dấu CAN_KIEM_TRA" và "tài liệu song ngữ thì chỉ lấy phần tiếng Việt".
- Chủ đề tài gỡ nốt mục **TT200** khỏi `can_cu_phap_ly` của `bctc.json` — file này là hồ sơ của **một mẫu biểu**, mà mẫu đó thuộc TT99, nên để TT200 trong đó là lạc chỗ. Kiểm tra lại: cả ba file json vẫn hợp lệ. **Lưu ý kèm theo:** dòng cảnh báo "95% kho tài liệu theo TT200, mẫu B09 của TT200 chưa đối chiếu bản gốc" nay chỉ còn trong nhật ký này, không còn hiện trong hồ sơ. Hướng xử lý về sau nếu cần: tách thành hai file song song `bctc-b09-tt99.json` và `bctc-b09-tt200.json`, mỗi file một mẫu, không trộn.
