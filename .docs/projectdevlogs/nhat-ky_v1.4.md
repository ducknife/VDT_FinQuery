# Nhật ký — v1.4

*Bản trước: [nhat-ky_v1.3.md](nhat-ky_v1.3.md).*

## Thay đổi so với bản trước (v1.3)

- Thêm ngày 13/10/2026: benchmark câu trả lời nhánh tài liệu, chẩn đoán và sửa B07, M03, bảng benchmark cuối, đổi cấu trúc package backend.

## Thay đổi của v1.3 so với v1.2

- Thêm ngày 12/10/2026: benchmark truy hồi, sửa bộ lọc và trích số, độ trễ, chọn nhà cung cấp, chuẩn bị hội đồng.

## Thay đổi của v1.2 so với v1.1

- Thêm ngày 11/10/2026: tiền truy xuất, truy hồi chạy thật, đổi model mặc định, báo cáo dời lên 14/10.

## Thay đổi của v1.1 so với v1.0

- Mở bản mới theo quy ước (hết tuần trong kế hoạch). Ghi từ 10/10/2026.

---

## 2026-10-10

### Hạ tầng gọi model
- Key Viettel AI vẫn hết credit; chốt **DeepInfra** (nạp 5 USD). Embedding đổi sang **Qwen3-Embedding-8B** (4.096 chiều). Reranker: Qwen3-Reranker 0.6B → 4B, kèm kế hoạch benchmark (`truy-hoi_v1.1`).
- OCR trên DeepInfra báo "model returned almost nothing": chunk SSE đầu có `content: ""` bị hiểu nhầm là hết stream. Sửa một dòng, thêm 3 test (`tien-xu-ly_v1.1` mục 12).

### Lập chỉ mục
- Cài `QdrantIndexWriter`, `DocumentIndexingService`, module `document` (entity, enum, sinh `tai_lieu_id`), migration **V10** thêm `ma_cong_ty` (bỏ "CTR" viết cứng), `DocumentController` (tải lên, trạng thái, nạp lại, nạp tất cả), `DocumentIngestionService` (luồng nền).
- Thêm **table-to-json** (paper Hierarchical Reranker), sau chunk, trước embed.
- **Nạp cả kho:** 278/278 tài liệu, **7.179 point**, khớp MySQL. Sự cố: DevTools tự khởi động lại làm ngắt lượt nạp ở 55/277 (dừng an toàn, chạy lại với restart tắt); DeepInfra 429 (tăng số lần thử).

### Trích số
- Module `financial`: trích B01/B02/B03 bằng quy tắc, 34 phép cộng kế toán theo mã nội bộ, chọn nguồn có thẩm quyền. Sau 5 vòng sửa quy tắc: **6.544 số hiệu lực**, 261 số trình bày lại, 160 ngoại lệ (`tien-xu-ly_v1.2` mục 13). Bắt đúng ca trình bày lại 19.047.848.409 đồng và lỗi OCR lệch 9.000 đồng.

### Kiểm thử và dọn dẹp
- `PreprocessingTrialTest`: chạy cả luồng trừ OCR trên 6 trang thật, ghi `trial-preprocessing.md`. Toàn bộ 35 test qua.
- Sơ đồ luồng tiền xử lý vẽ bằng HTML (`images/tien-xu-ly-luong_v1.1.png`).
- Người làm đề tài: đổi `FinancialFigureStore` → `FinancialFigureRepository`; xoá `devtools/` (khôi phục được từ commit `069d07d`); gom `logs/` và `why/` vào `.docs/projectdevlogs/` và `.docs/whyfinquery/`. Gốc dự án chỉ còn `data/`, `backend/`, `frontend/`, `infra/` (+ `pom.xml` gộp cho VS Code).

### Quyết định
- Bộ câu hỏi đánh giá: **50 câu** (`truy-hoi_v1.2` mục 4.2).
- Còn mở: cách xử lý số liệu của tài liệu tải lên mới, (a) tự động / (b) điền sẵn + người xác nhận / (c) nhập tay (`tien-xu-ly_v1.2` mục 13.8).

### Kế hoạch tới 18/10
```
11/10  truy hồi Qdrant + RerankClient          12/10  tiền truy xuất + sinh câu trả lời có trích dẫn
13/10  bộ 50 câu + runner đánh giá + benchmark  14/10  nhánh DB: SQL mẫu + tool
15/10  vòng agent, ghép hai nhánh, 3 chốt kiểm   16/10  API /ask + bộ test T1–T9
17–18  dự phòng, số liệu cho báo cáo
```
Sau đó (người làm đề tài muốn làm thêm): Keycloak, Kafka, Redis (bản tối thiểu), đánh giá khi đổi model, frontend, demo, báo cáo, slide.

---

## 2026-10-11

### Tiền truy xuất
- **Bộ chuẩn hoá** `FinancialTextNormalizer`: 6 quy tắc, "thêm dạng chuẩn trong ngoặc, giữ chữ gốc" (QĐ-027). File dữ liệu đổi tên sang tiếng Anh: `abbreviations.yml`, `terms.yml`. Chuẩn hoá ngữ liệu mặc định **tắt**, chờ thí nghiệm 2 × 2.
- Bỏ diễn giải lại, HyDE, Summary Extraction: Bảng 1 của paper cho thấy không có lợi (QĐ-028).
- **Trích từ khoá** bằng luật (tên chỉ tiêu trong DB + `keywords.yml`) và **trích bộ lọc** (công ty, năm, kỳ, phạm vi, loại tài liệu) (QĐ-030).

### Truy hồi
- Chốt số chunk **Qdrant 80 → rerank tầng 1 (0.6B) giữ 40 → tầng 2 (4B) giữ 20**. Paper không nêu số lấy từ kho; đã cân nhắc 300 → 100 → 20, 50 → 20 → 10, 80 → 60 → 20 (`truy-hoi_v1.5` mục 7.1).
- Cài `QdrantFilters`, `QdrantRetriever`, `RerankClient` (DeepInfra, lô gửi song song, ghi tiền thật), `HierarchicalReranker`, `RetrievalService`, endpoint `GET /api/v1/retrieval/search`.
- **Chạy thật 6 câu mẫu:** cả 6 câu có chunk đúng ở hạng 1 (kể cả công văn giải trình cho câu "vì sao"); bộ lọc không phải nới lần nào. Mỗi câu 6–20 giây (rerank 5–8 giây, embed DeepInfra dao động 0,5–15 giây); tiền rerank ~0,0025 USD/câu. Chia lô nhỏ hơn chỉ nhanh ~10%.
- Test: **65 qua, 0 lỗi** (thêm 18 test mới).

### Model và chi phí
- Model suy luận mặc định khi phát triển đổi sang **Qwen3.5-397B**; Sonnet 5.5 chỉ cho lượt benchmark chính thức và demo (QĐ-031). Ước tính cả dự án ~5–7 USD.

### Lịch
- **Báo cáo dời lên 14/10/2026**, có phó tổng giám đốc dự. Trọng tâm: tính hiệu quả và tính hoàn thiện; trình bày theo vấn đề nghiệp vụ được giải quyết, ít nói công nghệ.

### Còn mở
- Tăng tốc truy hồi: cắt ngắn chunk ở tầng 1, loại trang tiếng Anh trùng nội dung, lưu sẵn câu hỏi lặp lại.
- Bước sinh câu trả lời có trích dẫn (+ chia 64k), bộ 50 câu, nhánh DB.
- Cách xử lý số liệu của tài liệu tải lên mới (a / b / c).

---

## 2026-10-12

### Đánh giá
- Bộ 50 câu đổi tên thành `data/eval/eval_test.yml`; thêm chỉ số bằng chứng cho từng trang vàng.
- Viết bộ tính chỉ số và trình chạy benchmark bằng Java (Recall theo tầng, nDCG@20, nDCG@20 theo bằng chứng, MRR, bị lừa, bootstrap), chạy tiếp được khi bị ngắt; đo độ trễ từng bước.
- Benchmark 3 lần: Recall@20 cấu hình chính 73,3% → 84,4% → **95,6%**. 8 cấu hình: RAG cơ bản 68,9%; không rerank 91,1%; 1 tầng ngang 2 tầng nhưng chậm hơn; rút gọn mất đáp án; 4B → 8B không tốt hơn (`danh-gia_v1.5` mục 4). Xuất `benchmark-truy-hoi_20261012.xlsx`.

### Sửa lỗi do benchmark phát hiện
- Bộ lọc năm theo năm ban hành; bỏ lọc loại theo chữ "BCTC"; "hợp nhất" thắng "riêng" (QĐ-032).
- Trích số: trang nối tiếp rơi cột thì không đọc; trích lại cả kho (QĐ-033).
- Bổ sung nhãn câu V05.

### Độ trễ và nhà cung cấp
- Đo: ~9 s đến khi có 20 chunk, gần như toàn bộ là chờ DeepInfra; đổi sang 5G không cải thiện.
- Cân nhắc API QwenCloud: có `qwen3-rerank` (pay-as-you-go 0,1 USD/1 triệu token), Token Plan không phù hợp, đăng ký thanh toán vướng. **Giữ DeepInfra**, chấp nhận 10–15 s, ưu tiên độ chính xác (QĐ-034).

### Hội đồng 14/10
- Hai nhóm giám khảo: kỹ thuật/nghiệp vụ và non-tech (quan tâm hiệu quả, ứng dụng). Người dùng ban đầu: phòng tài chính kế toán; đóng góp: trợ lý tài chính đáng tin cậy, hiệu quả. Đã soạn câu chuyện và các câu dễ bị hỏi vặn.
- Phần đánh giá cần đủ 4 hệ thống: RAG cơ bản, RAG phân cấp (đã có số), text-to-SQL thường, function calling (chưa làm).

### Tiếp theo
- Agent + function calling + sinh câu trả lời có trích dẫn + feedback + API `/ask`; text-to-SQL đối chứng; giảm độ trễ (gửi đúp embed, lưu sẵn); frontend; slide.

## 2026-10-13

### Benchmark câu trả lời nhánh tài liệu
- Chạy FinQuery và RAG cơ bản trên 26 câu `rag_*` + 3 câu từ chối (Sonnet 5.5): FinQuery 24,5/26, RAG cơ bản 21,5/26 (`danh-gia_v1.9`).
- Trục model nhánh số liệu: Qwen 20/21, 153 s/câu; Sonnet 21/21, 8,2 s/câu.

### B07 (vốn điều lệ sau phát hành)
- Nguyên nhân: agent tự viết lại câu tìm, trang đúng rớt khỏi 80 ứng viên Qdrant; câu gốc thì vào top 20 (hạng 7).
- Sửa (QĐ-040): lần `search_documents` đầu dùng câu người dùng gõ; Qdrant tìm chính xác; thử lại một lần khi lỗi. Giữ 80 ứng viên.
- Hai kết luận sai trong lúc dò do công cụ đo (tham số `-D` mất dấu tiếng Việt; Python đổi `\r\n`), đã rút lại (`danh-gia_v1.10` mục 2.3).

### M03 (so sánh LNST hai quý)
- DB chưa trích LNST từ báo cáo quý 1, quý 2 (chưa sửa, ghi vào hạn chế).
- Sửa (QĐ-041, prompt v1.5): model khai báo phép tính trên số C#/D# ở bước trả lời, Java (`EvidenceCalculator`) kiểm số có nguyên văn trong nguồn rồi tính; giao diện nhận `answer_final`.
- Chạy lại B07, M03: cả hai đúng. Bảng benchmark cuối: FinQuery 26/26 (`danh-gia_v1.11`).

### Đổi cấu trúc package backend
- `modules/hierarchicalrag` → **`modules/rag`** (nhánh tài liệu).
- `modules/agent/tools/financial` → **`modules/dbquery`** (nhánh số liệu: 3 tool chính; lớp thành phần trong `modules/dbquery/support`).
- `agent/tools` còn lại tool chung của vòng agent: `search_documents`, `decline_out_of_scope`, `QueryState`.
- Đổi bằng `git mv` (giữ lịch sử); 30 test đơn vị và test nạp Spring context đều qua. Log cũ giữ đường dẫn cũ vì ghi đúng thời điểm viết.

### Còn treo
- Chạy lại cả bộ với code cuối (khoảng 4,5 USD, hết số dư).
- Comment `answer.trial.enabled` trong `application.yml`; thử Keycloak thật; commit.
