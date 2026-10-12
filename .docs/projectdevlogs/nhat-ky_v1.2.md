# Nhật ký — v1.2

*Bản trước: [nhat-ky_v1.0.md](nhat-ky_v1.0.md) (tới 21/09/2026). Từ 22/09 tới 09/10 nhật ký không được ghi theo ngày; việc của giai đoạn đó nằm trong các log chủ đề (tien-xu-ly, lam-sach, lap-chi-muc, truy-hoi, quyet-dinh).*

## Thay đổi so với bản trước (v1.1)

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
