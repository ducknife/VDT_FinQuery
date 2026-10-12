# Truy hồi (tiền truy xuất, rerank), đánh giá trong lúc phát triển và hạ tầng gọi model — v1.6

> Ngày: 11/10/2026. Phạm vi: phía **trả lời câu hỏi** của nhánh RAG, sau khi kho đã được lập chỉ mục (xem `lap-chi-muc_v1.12.md`).
> Luồng (đã cài đặt, mục 7): câu hỏi → **tiền truy xuất** (chuẩn hoá, trích từ khoá, trích bộ lọc) → embed Q′ → Qdrant **80 chunk có lọc** → **rerank tầng 1** giữ 40 → **rerank tầng 2** giữ 20 → LLM sinh câu trả lời kèm trích dẫn.

## Thay đổi so với bản trước (v1.5)

- **Bản đồ file của truy hồi** (mục 7.8): chức năng từng file cấu hình, tiền truy xuất, truy hồi, test.
- **Ghi chú vận hành** (mục 7.9): PowerShell cắt tham số `-D`; đổi số bằng tham số dòng lệnh; sửa `batchSize` mặc định 50 → 40.
- Kết quả chạy thật lần đầu đã ghi ở mục 7.7.

## Thay đổi của v1.5 so với v1.4

- **Đã cài đặt truy hồi** (mục 7, QĐ-030): trích từ khoá, trích bộ lọc, tìm Qdrant có lọc, rerank 2 tầng, service và endpoint thử, test.
- **Chốt số chunk 80 → 40 → 20** (mục 7.1): paper không nêu số ứng viên lấy từ kho; lý do chọn và cách kiểm chứng.
- **Model mặc định khi phát triển là Qwen3.5** (mục 7.6, QĐ-031); ước tính chi phí từng bước.
- Các chỗ ghi "khoảng 100 → 20" ở mục 2 là thiết kế cũ, số chốt xem mục 7.1.

## Thay đổi của v1.4 so với v1.3

- **Đã cài đặt bộ chuẩn hoá** `FinancialTextNormalizer` (mục 1.7): file, logic chung, cách thêm dấu, cách đọc số tiền, test, giới hạn. File dữ liệu đổi tên sang tiếng Anh: `abbreviations.yml`, `terms.yml` (QĐ-027 ghi tên cũ `viet-tat.yml`).
- **CHỐT bỏ bước diễn giải lại** (mục 1.8, QĐ-028): tiền truy xuất còn chuẩn hoá + trích từ khoá (câu hỏi) và table-to-json (ngữ liệu).
- **Rerank theo paper** (mục 2.4): paper dùng model rerank chuyên dụng, không dùng LLM lõi; số đo Bảng 2 (8B hơn 4B ở tầng 2).
- **Model suy luận** (mục 5.5, QĐ-029): mặc định `anthropic/claude-sonnet-5-5`; danh sách so sánh ghi sẵn trong `.env`.

(Thay đổi của v1.3 so với v1.2: chốt 6 quy tắc chuẩn hoá; phân tích chuẩn hoá ngữ liệu trong Bảng 1; thí nghiệm 2 × 2 và quy tắc quyết định.)

---

## 1. Tiền truy xuất

### 1.1. Chốt: ba kỹ thuật, không dùng HyDE

Paper Hierarchical Reranker dùng bốn bước: chuẩn hoá, trích xuất từ khoá, diễn giải lại, sinh tài liệu giả định (HyDE). FinQuery **dùng ba bước đầu, không dùng HyDE**.

| Kỹ thuật | Cần gọi LLM? | Thời gian thêm | Rủi ro với báo cáo tài chính |
|---|---|---|---|
| Chuẩn hoá | Không: NFC, khoảng trắng, mở rộng viết tắt theo từ điển tài chính | Vài mili giây | Thấp |
| Trích xuất từ khoá | Phần lớn không: năm, quý, hợp nhất/riêng, loại tài liệu bằng quy tắc | Vài mili giây | Thấp |
| Diễn giải lại | **Có**, một lượt sinh ngắn | Khoảng 1–2 giây | Thấp |
| HyDE | **Có**, một lượt sinh dài | Khoảng 2–5 giây | **Cao**: đoạn giả định chứa con số bịa |

Lý do bỏ HyDE: (1) thời gian và chi phí là 2 trong 3 tiêu chí cốt lõi; (2) rủi ro con số bịa; (3) Qwen3-Embedding là model bất đối xứng có tiền tố `Instruct:` cho câu hỏi, đã thu hẹp khoảng cách giữa cách viết câu hỏi và tài liệu. Để có bằng chứng cho báo cáo: cài HyDE dưới dạng cờ tắt sẵn và chạy như một thí nghiệm.

### 1.2. Thiết kế: tối đa một lượt LLM và một lượt embedding mỗi câu hỏi

```
câu hỏi
  ├─ Tầng 1: QUY TẮC (không gọi LLM): chuẩn hoá; trích bộ lọc nam / ky / pham_vi / loai; trích thuật ngữ
  ├─ Tầng 2: MỘT lượt LLM nhỏ, trả JSON { paraphrases: [2 câu], keywords: [...] }
  └─ MỘT lượt embedding: câu gốc + 2 câu diễn giải trong cùng một lô
       → 3 lượt tìm trên Qdrant (cùng bộ lọc) → gộp kết quả → rerank
```

**Chưa chốt:** model cho lượt LLM ở tầng 2. Ứng viên trên DeepInfra: `openai/gpt-oss-120b` (0,037 / 0,17 USD mỗi 1 triệu token vào / ra), `Qwen/Qwen3-32B` (0,08 / 0,28), `deepseek-ai/DeepSeek-V4-Flash` (0,09 / 0,18).

### 1.3. Số đo của paper về tiền truy xuất (Bảng 1, Lee & Hong, arXiv 2607.27523)

NDCG@20 trên kho của paper (tiếng Anh). Phía kho tài liệu cố định "chuẩn hoá + table-to-json":

| Phía câu hỏi | NDCG@20 |
|---|---|
| Chuẩn hoá | 0,7529 |
| Chuẩn hoá + **trích từ khoá** | **0,7918** (cao nhất bảng, hơn mốc không tiền xử lý +5,9%) |
| Chuẩn hoá + diễn giải lại | 0,7677 |
| Chuẩn hoá + HyDE | 0,6843 |

Phía kho, với câu hỏi "chuẩn hoá + trích từ khoá": chuẩn hoá 0,7503 → thêm table-to-json **0,7918**.

Hệ quả cho FinQuery:
- Giữ quyết định **bỏ HyDE**: trong mọi tổ hợp của bảng, HyDE đều làm giảm điểm.
- **Trích từ khoá** là bước đáng làm nhất ở phía câu hỏi; **diễn giải lại** kém hơn và tốn một lượt LLM, nên đưa vào benchmark như tuỳ chọn thay vì mặc định.
- Paper đo trên tiếng Anh với model của họ; phải đo lại trên báo cáo tiếng Việt.

### 1.4. Quan sát khi tìm thử trên kho đã nạp (10/10/2026)

"Tổng tài sản hợp nhất cuối năm 2023", lọc `nam = 2023`, `pham_vi = hop_nhat`, `loai = bctc`: trang B01 của báo cáo năm 2023 (trang 6, 7) nằm trong top 5 nhưng **không đứng đầu**; đứng đầu là trang bìa; trang của báo cáo quý 1 lọt vào. Hai việc của tiền truy xuất và rerank được chứng minh bằng chính dữ liệu:
1. trích "cuối năm" thành `ky = nam` (bộ lọc payload đã có index);
2. rerank đẩy trang có bảng số lên đầu.

### 1.5. Bước chuẩn hoá — CHỐT 6 quy tắc (11/10/2026)

**Paper mô tả** (Lee & Hong, mục 4.1): *"All input queries are first normalized to reduce lexical ambiguity. This step includes lowercase, typographical correction, and expansion of financial abbreviations (e.g., EPS: Earnings per share, YoY: Year-over-Year). Measurement units such as "K" or "M" are standardized into consistent numeric expressions, and missing contextual terms are restored based on document metadata. ... The corpus is normalized likewise for consistency."* Thuật toán 1 áp lên cả hai đầu: `Normalization(Q)` và `Normalization(C)`. Paper nhấn mạnh làm **bằng quy tắc**, không dùng LLM (*"deterministic normalization ... without introducing LLM-induced hallucinations"*), nhưng **không công bố bộ quy tắc**; bộ quy tắc tiếng Việt cho báo cáo tài chính dưới đây do đề tài tự xây.

**6 quy tắc, theo thứ tự chạy:**

| # | Quy tắc | Ví dụ | Câu hỏi | Ngữ liệu |
|---|---|---|---|---|
| 1 | **NFC + khoảng trắng** | "ế" 3 code point → 1; gộp dấu cách thừa | ✓ | ✓ (đã có trong `TextCleaner`) |
| 2 | **Gõ không dấu → thêm dạng có dấu**, chỉ cho thuật ngữ có trong từ điển và tên chỉ tiêu | "doanh thu nam 2023" → "doanh thu nam 2023 (doanh thu năm 2023)" | ✓ | — (ngữ liệu đã có dấu) |
| 3 | **Mở rộng viết tắt** theo `viet-tat.yml` (lấy từ mục 6 của `tu-dien-tai-chinh_v1.2`) | "LNST 2024" → "LNST (lợi nhuận sau thuế) 2024" | ✓ | ✓ |
| 4 | **Chuẩn hoá cách gọi kỳ** | "Q3/2024" → "Q3/2024 (quý 3 năm 2024)"; "6T", "bán niên" → thêm "(6 tháng đầu năm)"; "FY2023" → thêm "(năm 2023)" | ✓ | ✓ |
| 5 | **Chuẩn hoá đơn vị, số tiền** | "8,2 nghìn tỷ" → "8,2 nghìn tỷ (8.200.000.000.000 đồng)"; "500tr"; "VNĐ", "đ" | ✓ | ✓ (chữ trong thuyết minh, BCTN) |
| 6 | **Điền ngữ cảnh còn thiếu từ metadata** | "năm ngoái" → "năm ngoái (năm 2025)"; thời gian tương đối tính theo **kỳ mới nhất trong `documents`** (30/06/2026), không theo đồng hồ hệ thống; không nêu công ty → CTR; không nêu phạm vi → hợp nhất (bẫy số 2 của từ điển) | ✓ | Đã có (dòng `ten_hien_thi` đầu mỗi chunk) |

**Nguyên tắc: thêm dạng chuẩn vào sau, giữ nguyên chữ gốc, không thay hẳn.**
1. Ngữ liệu hiện chưa chuẩn hoá (vẫn có "LNST", "Quý IV/2023"): giữ cả hai dạng thì khớp được cả hai phía.
2. Quy tắc có thể đoán sai (thêm dấu, viết tắt nhập nhằng): còn chữ gốc thì câu hỏi không bị hỏng.
3. Một chính sách cho mọi chế độ: khi bật chuẩn hoá ngữ liệu, ngữ liệu cũng được thêm dạng chuẩn y như vậy, nên hai đầu chắc chắn chung một dạng chuẩn.

Ngoại lệ được thay hẳn vì không đổi nghĩa: NFC và khoảng trắng thừa.

**Lệch khỏi paper:** paper đưa câu hỏi về chữ thường; FinQuery chỉ dùng chữ thường **bên trong khi so khớp quy tắc** (`VietnameseText.fold`), văn bản đầu ra giữ nguyên hoa thường, vì nhiều viết tắt phân biệt nhờ chữ hoa.

**Viết tắt nhập nhằng** (`HN` hợp nhất / Hà Nội, `TM` thuyết minh / thương mại, `KH` kế hoạch / khách hàng, `R` riêng, `DT`): đánh dấu trong `viet-tat.yml`, chỉ mở rộng khi có ngữ cảnh ("BCTC HN"), không thì để nguyên.

**Nơi gọi — cùng MỘT class `FinancialTextNormalizer`, hai chế độ `QUERY` và `CORPUS`** (quy tắc 2 và 6 chỉ chạy ở `QUERY`):

```
LÚC NẠP (một lần)    chunk → table-to-json → chuẩn hoá (CORPUS, khi indexing.corpus-normalization = true) → embed → Qdrant
LÚC HỎI (mỗi câu)    câu hỏi → chuẩn hoá (QUERY) → trích bộ lọc, trích từ khoá → embed → tìm trong Qdrant
```

- Phía ngữ liệu gọi **lúc nạp**, không gọi lúc truy xuất; chỉ chuẩn hoá **văn bản đem embed**, `noi_dung` giữ nguyên văn để trích dẫn (giống table-to-json).
- Tuỳ chọn gọi lúc truy xuất: chuẩn hoá đoạn `noi_dung` đưa cho reranker khi ngữ liệu đã chuẩn hoá lúc embed; benchmark quyết.
- Dùng chung một class cho hai đầu để sửa một quy tắc là cả hai đầu cùng đổi (như `NFCNormalization` hiện nay).

**Đầu ra khi chuẩn hoá câu hỏi:** `NormalizedQuery { original, text, amounts, resolvedTimes }`. `original` giữ câu gốc để hiện cho người dùng và đưa cho LLM; `text` (có thêm dạng chuẩn) dùng để embed, rerank, trích bộ lọc; `amounts` (số tiền đã đổi ra đồng) cho nhánh DB và chốt kiểm "số phải có nguồn". Trích bộ lọc (`nam`, `ky`, `pham_vi`, `loai`) và trích từ khoá là hai class **riêng**, chạy sau chuẩn hoá, chỉ cho câu hỏi.

**Chỗ đặt:** `preretrieval/normalizer/FinancialTextNormalizer` (đề nghị đổi tên thư mục `nomalizer/` hiện tại, thiếu chữ r); dữ liệu `src/main/resources/preretrieval/viet-tat.yml` (thêm viết tắt không phải sửa Java); cờ `indexing.corpus-normalization` mặc định **tắt**.

**Trạng thái 11/10/2026:** mới thiết kế; chưa có class, chưa có file `viet-tat.yml`, chưa có cờ. Collection hiện tại (`finq_pages_qwen3e8b`) dùng tiếp, không nạp lại.

### 1.6. Chuẩn hoá ngữ liệu: chưa được chứng minh, làm qua thí nghiệm

Các cặp dòng trong Bảng 1 của paper giữ nguyên phía câu hỏi, chỉ đổi ngữ liệu từ nguyên bản sang chuẩn hoá:

| Phía câu hỏi | Ngữ liệu nguyên bản | Ngữ liệu chuẩn hoá | Chênh |
|---|---|---|---|
| Nguyên bản | 0,7323 | 0,7211 | −0,011 |
| Chuẩn hoá | 0,7446 | 0,7333 | −0,011 |
| Chuẩn hoá + từ khoá | 0,7542 | 0,7503 | −0,004 |
| Chuẩn hoá + diễn giải lại | 0,7347 | 0,7446 | +0,010 |
| Chuẩn hoá + HyDE | 0,7347 | 0,6759 | −0,059 |

4/5 cặp giảm. Con số tốt nhất (0,7918) là "ngữ liệu chuẩn hoá + table-to-json", nhưng bảng không có dòng "ngữ liệu nguyên bản + table-to-json" nên không tách được phần đóng góp; mỗi dòng chỉ một con số, không có khoảng tin cậy. Kết luận: số đo của paper ủng hộ chuẩn hoá **câu hỏi** và **table-to-json**, chưa chứng minh chuẩn hoá **ngữ liệu**.

**Thí nghiệm 2 × 2** (bộ 50 câu, phía câu hỏi chuẩn hoá cố định):

| | Bảng markdown | Bảng JSON |
|---|---|---|
| Ngữ liệu nguyên bản | nạp thêm | **đang có** (`finq_pages_qwen3e8b`) |
| Ngữ liệu chuẩn hoá | nạp thêm | nạp thêm |

Ba collection thêm, mỗi cái khoảng 15 phút và 0,1 USD; mỗi ô chỉ là đổi cờ (`table-format`, `corpus-normalization`) rồi nạp vào collection mới. Tách được tác dụng riêng của table-to-json, của chuẩn hoá ngữ liệu, và tác dụng khi kết hợp.

**Quy tắc quyết định (chốt trước khi đo):** chuyển sang collection ngữ liệu chuẩn hoá chỉ khi chênh lệch nDCG@10 (hoặc Recall@20) có khoảng tin cậy bootstrap **không chứa 0**, kiểm định ghép cặp có ý nghĩa, và không làm tệ rõ rệt nhóm câu nào. Khi chuyển: đổi `QDRANT_COLLECTION` **và** bật `indexing.corpus-normalization: true` cùng lúc (để tài liệu tải lên sau cũng chuẩn hoá giống vậy), giữ collection cũ vài ngày để quay về được. Không thoả thì giữ bản hiện tại và ghi "không phân biệt được với 50 câu".

### 1.7. Cài đặt bộ chuẩn hoá (11/10/2026)

**Các file**

| File | Vai trò |
|---|---|
| `preretrieval/normalizer/FinancialTextNormalizer.java` | Class chính, dùng chung cho câu hỏi (`normalizeQuery`) và ngữ liệu (`normalizeCorpus`) |
| `preretrieval/normalizer/NormalizedQuery.java` | Kết quả chuẩn hoá câu hỏi: `original`, `text`, `amounts`, `maCongTy` (+ cờ mặc định), `phamVi` (+ cờ mặc định), `applied` |
| `preretrieval/normalizer/NormalizationDictionary.java` | Đọc hai file YAML (SnakeYAML có sẵn trong Spring Boot) |
| `preretrieval/normalizer/VietnameseAmount.java` | Đọc số tiền kiểu Việt Nam ra đồng |
| `resources/preretrieval/abbreviations.yml` | 66 viết tắt (mục 6 từ điển tài chính); khoá: `abbreviations`, `forms`, `expansion`, `context` |
| `resources/preretrieval/terms.yml` | Thuật ngữ để thêm dấu, công ty; khoá: `terms`, `default_company`, `companies` |
| `common/text/VietnameseText` | Thêm `foldKeepLength` (bỏ dấu, giữ nguyên độ dài) và `hasDiacritics` |
| `FinancialTextNormalizerTest` | 10 test, khoảng 30 câu mẫu |

Thư mục `preretrieval/nomalizer/` (sai chính tả) đổi thành `normalizer/`. Tên file và khoá YAML tiếng Anh, comment trong file tiếng Việt.

**Logic chung**
1. **Tìm trên bản không dấu, chèn vào câu gốc.** Câu được đổi sang bản không dấu, chữ thường, **cùng độ dài** (`foldKeepLength`: mỗi ký tự NFC ra đúng một ký tự). Mọi quy tắc tìm trên bản này ("Quý III", "quy iii" đều khớp); vị trí tìm thấy dùng thẳng trên câu gốc để chèn dạng chuẩn ngay sau cụm gốc. Các chỗ chèn gom lại rồi dựng câu một lần.
2. **Thêm, không thay**; bỏ qua khi cụm đã đúng dạng chuẩn hoặc đã có ngoặc giải thích ngay sau.
3. Danh sách `applied` ghi lại quy tắc nào đã chạy (`abbreviation:LNST`, `period:Q3/2024`, `amount:500tr`, `relative_time:năm ngoái`, `diacritics`, `context:company=CTR`…), dùng để gỡ lỗi và viết báo cáo.

**Từng quy tắc như đã cài**

| # | Quy tắc | Nhận được | Ví dụ (đầu ra thật của test) |
|---|---|---|---|
| 1 | NFC + khoảng trắng (thay hẳn) | | `"Doanh thu  quý   3"` → `"Doanh thu quý 3"` |
| 2 | Thêm dấu (chỉ câu hỏi) | Chỉ khi cả câu không có dấu | `doanh thu thuan nam 2023 la bao nhieu (doanh thu thuần năm 2023 là bao nhiêu)` |
| 3 | Viết tắt | Lần xuất hiện đầu; cụm ≥ 3 ký tự không phân biệt hoa thường; cụm ngắn phải đúng chữ hoa; bỏ qua nếu dạng đầy đủ đã có trong câu | `LNST (lợi nhuận sau thuế)`, `YoY (so với cùng kỳ năm trước)`; `BCTC HN` → `HN (hợp nhất)` nhưng "Chi nhánh HN" giữ nguyên |
| 4 | Cách gọi kỳ | `Q3/2024`, `quý III năm 2023`, `quý 2/2025`, `bán niên`, `6T`, `H1`, `9 tháng`, `FY2023`, `năm tài chính 2023`; "6 tháng **cuối** năm" không bị nhận nhầm | `Q3/2024 (quý 3 năm 2024)`, `bán niên 2025 (6 tháng đầu năm 2025)` |
| 5 | Số tiền, phần trăm | Đơn vị: `nghìn tỷ`, `trăm tỷ`, `tỷ`, `tỉ`, `trăm triệu`, `triệu`, `tr`, `trăm nghìn`, `nghìn`, `ngàn`, `trăm`, `k`; `VNĐ`, `đ` sau số có nhóm nghìn; `phần trăm` | `8,2 nghìn tỷ (8.200.000.000.000 đồng)`, `500tr (500.000.000 đồng)`, `5 trăm triệu` → 500.000.000, `12 phần trăm (12%)` |
| 6 | Thời gian tương đối (chỉ câu hỏi) | `năm nay`, `năm ngoái`, `năm trước` (khi câu không có năm cụ thể), `quý này`, `quý trước`, `gần nhất`, `mới nhất`, `hiện nay`, `hiện tại`; mốc là `DocumentRepository.findLatestPeriodEnd()` (30/06/2026) | `năm ngoái (năm 2025)`, `quý trước (quý 1 năm 2026)`, `gần nhất (kỳ 30/06/2026)` |
| 6 | Công ty, phạm vi mặc định (chỉ câu hỏi) | Không nêu công ty → `CTR`; không nêu phạm vi → `hop_nhat`; kèm cờ "mặc định". **Ghi vào trường, không chèn vào câu**: lọc bằng payload chính xác hơn thêm chữ vào câu đem embed | "báo cáo riêng của Viettel Construction" → `RIENG`, `CTR` (không phải mặc định) |

**Thêm dấu (quy tắc 2) là tra từ điển, không phải thuật toán đoán dấu.** Danh sách thuật ngữ (từ `terms.yml` và các dạng đầy đủ của viết tắt) sắp **dài trước, ngắn sau**. Quét câu từ trái sang phải; ở đầu mỗi từ, thử khớp từng thuật ngữ (đã bỏ dấu) và đòi ranh giới từ ở cuối; khớp thì ghi bản có dấu và nhảy qua cụm, không khớp thì chép nguyên ký tự. Chỉ dùng kết quả khi có ít nhất một cụm thật sự được thêm dấu; nối cả câu có dấu vào cuối trong ngoặc. Ví dụ "doanh thu thuan" khớp "doanh thu thuần" trước "doanh thu" nhờ sắp dài trước. Không dùng model thêm dấu hay LLM: tất định, không tốn tiền, đúng với thuật ngữ tài chính đã khai báo.

**Đọc số tiền (`VietnameseAmount`):** dấu chấm ngăn đúng 3 chữ số là ngăn nghìn (`8.195` → 8195); dấu phẩy là thập phân (`8,2`); dấu chấm không ngăn đúng 3 chữ số là thập phân kiểu Anh (`1.5` → 1,5). Quy tắc chỉ nhận khi **ngay trước đơn vị là một con số** (chỉ cách bởi dấu cách), con số đứng độc lập, và ngay sau đơn vị không phải chữ cái; nhờ vậy "12 phần trăm" không bị đọc thành số tiền, "3 trang", "3 tiền" không bị nhận nhầm `tr`, `ti`. Đơn vị xếp cụm dài trước ("trăm triệu" trước "trăm").

**Gắn vào luồng:** phía ngữ liệu gọi trong `Embedding.bodyOf()` sau table-to-json, theo cờ `indexing.corpus-normalization` (mặc định `false`, collection hiện tại không đổi). Phía câu hỏi **chưa gọi ở đâu**: sẽ gắn khi viết service truy hồi. Spring dùng constructor có `@Autowired` (đọc từ điển trong classpath); constructor còn lại cho test.

**Giới hạn:** chỉ thêm dấu cho cụm có trong danh sách; câu nửa có dấu nửa không thì không thêm dấu; "nam" luôn thành "năm"; số viết bằng chữ ("năm trăm triệu") chưa nhận; "1,234 tỷ" kiểu Anh bị hiểu là 1,234 tỷ; "cùng kỳ" chưa đổi thành kỳ cụ thể (để bước trích bộ lọc xử lý).

**Kiểm tra:** `FinancialTextNormalizerTest` 10/10; toàn bộ test của dự án đạt; ứng dụng khởi động được với bean mới.

### 1.8. Bỏ bước diễn giải lại và "Summary Extraction" — CHỐT (11/10/2026)

Bảng 1 của paper (NDCG@20):

| Phía câu hỏi | Ngữ liệu nguyên bản | Ngữ liệu chuẩn hoá + table-to-json |
|---|---|---|
| Chuẩn hoá | 0,7446 | 0,7529 |
| Chuẩn hoá + diễn giải lại | 0,7347 (giảm) | 0,7677 |
| Chuẩn hoá + trích từ khoá | 0,7542 | **0,7918** |

Lý do bỏ diễn giải lại: kết quả lẫn lộn (ngữ liệu nguyên bản thì giảm) và luôn kém trích từ khoá; không có dòng nào kết hợp trích từ khoá với diễn giải lại; là bước duy nhất của tiền truy xuất cần gọi LLM (thêm khoảng 1–2 giây và chi phí mỗi câu hỏi); khớp với danh sách cắt giảm trong `pham-vi_v1.0`. Giữ dạng tuỳ chọn (một cờ) trong benchmark nếu còn thời gian. HyDE bỏ hẳn.

**Phía ngữ liệu còn một cột thứ tư trong Bảng 1: "Summary Extraction"** (thay cả tài liệu bằng bản tóm tắt, theo văn bản của paper: *"When the corpus size is extremely large (over 10k tokens) ... each document was replaced through summarization substitution, preserving only the essential financial indicators, results, and contextual information"*). 5 dòng có bật cột này:

| Phía câu hỏi | Ngữ liệu: tóm tắt | NDCG@20 |
|---|---|---|
| Nguyên bản | Tóm tắt | 0,6544 |
| Chuẩn hoá | Tóm tắt | 0,6501 |
| Chuẩn hoá + trích từ khoá | Tóm tắt | 0,6579 |
| Chuẩn hoá + diễn giải lại | Tóm tắt | 0,6542 |
| Chuẩn hoá + HyDE | Tóm tắt | **0,5853** (thấp nhất toàn bảng) |

**Bỏ hẳn "Summary Extraction", lý do rõ hơn cả diễn giải lại (không lẫn lộn, thua toàn diện):**
1. Mọi dòng có tóm tắt đều thấp hơn hẳn mọi cách xử lý ngữ liệu khác trong bảng (0,58–0,66, so với 0,68–0,79 của các cột còn lại) — thua khoảng 0,07–0,15 điểm, không có ngoại lệ.
2. Hợp lý về bản chất: tóm tắt chỉ giữ "chỉ tiêu thiết yếu", trong khi câu hỏi tài chính thường cần đúng một con số cụ thể ở một trang; con số đó dễ bị bản tóm tắt bỏ sót.
3. **Vấn đề mà bước này giải quyết không phát sinh trong kiến trúc FinQuery.** Paper dùng tóm tắt khi embed cả tài liệu dài hơn 10k token; FinQuery chunk theo trang ngay từ đầu (trần 4.000 ký tự), nên không có đơn vị embed nào đủ dài để cần tóm tắt — vấn đề đã được giải bằng cách chunk, không cần thêm bước này.
4. Tóm tắt phải dùng LLM, thêm một chỗ có thể làm sai lệch con số, ngược nguyên tắc "quy tắc, không LLM" đã áp dụng cho table-to-json.

Tiền truy xuất của FinQuery còn **3 bước**, đúng tổ hợp tốt nhất của Bảng 1: chuẩn hoá + trích từ khoá (câu hỏi), table-to-json (ngữ liệu). Mục 1.1–1.2 (thiết kế "một lượt LLM nhỏ cho diễn giải") **thay bằng mục này**: tiền truy xuất không gọi LLM.

**Việc tiếp theo:** trích từ khoá bằng quy tắc (tra `financial_items.ten_chuan`, `financial_item_mappings.ten_tren_bao_cao`, từ điển), nối vào câu đem embed (*"keywords are extracted and appended to each query"*).

## 2. Rerank hai tầng

### 2.1. Chốt

| Tầng | Vai trò | Model | Giá DeepInfra (USD / 1 triệu token) |
|---|---|---|---|
| 1. Lọc nhẹ | Khoảng 100 ứng viên → khoảng 20 | **`Qwen/Qwen3-Reranker-0.6B`** (thay `bge-reranker-v2-m3`) | 0,010 |
| 2. Xếp hạng kỹ | Khoảng 20 → kết quả cuối | **`Qwen/Qwen3-Reranker-4B`** | 0,025 |

Bảng điểm reranker do Qwen công bố:

| Model | Tham số | MTEB-R | MMTEB-R (đa ngôn ngữ) | MLDR (văn bản dài) | Ngữ cảnh |
|---|---|---|---|---|---|
| bge-reranker-v2-m3 | 0,6B | 57,03 | 58,36 | 59,51 | |
| Qwen3-Reranker-0.6B | 0,6B | 65,80 | 66,36 | 67,28 | 32k token |
| Qwen3-Reranker-4B | 4B | **69,76** | 72,74 | 69,97 | 32k token |
| Qwen3-Reranker-8B | 8B | 69,02 | **72,94** | **70,19** | 32k token |

Lý do:
- Qwen3-Reranker-0.6B **cùng cỡ** bge-reranker-v2-m3 nhưng **hơn khoảng 8 điểm** MMTEB-R, đọc được **32k token** (không vướng giới hạn ngắn như ViRanker), và **có API**.
- Bản 8B **gần như không hơn** bản 4B (MMTEB-R +0,2, MTEB-R còn thấp hơn), nên tầng 2 dùng 4B.
- Hai tầng **cùng họ model**: một `RerankClient` dùng chung, cùng kiểu instruction.
- Với giá API, rerank thẳng 100 ứng viên bằng bản 4B cũng chỉ khoảng 0,003 USD mỗi câu hỏi. **Lý do giữ hai tầng giờ là độ trễ, không phải chi phí**: cần đo để kiểm chứng (mục 2.2).

Các điểm trên là trung bình nhiều ngôn ngữ do Qwen công bố, chưa đo riêng trên báo cáo tài chính tiếng Việt.

### 2.2. Kế hoạch benchmark rerank

| Cấu hình | Tầng 1 (100 → 20) | Tầng 2 (20 → kết quả) | Mục đích |
|---|---|---|---|
| **A** (mặc định) | Qwen3-Reranker-0.6B | Qwen3-Reranker-4B | Phân cấp đúng tinh thần paper |
| **B** | Qwen3-Reranker-4B | Qwen3-Reranker-8B | Cặp lớn hơn (đề xuất của người làm đề tài) |
| **C** | — | Chỉ 4B, chấm thẳng 100 | Một tầng: hai tầng có đáng không |
| **D** | — | Chỉ 8B, chấm thẳng 100 | Như trên |
| **Không rerank** | — | — | Mốc so sánh: chỉ thứ tự của Qdrant |
| **Định dạng bảng** (thí nghiệm riêng) | Markdown / JSON lúc embed / JSON chỉ lúc rerank | | Tái hiện kết quả table-to-json của paper trên báo cáo tiếng Việt; mỗi định dạng embed một collection riêng |
| *(Tuỳ chọn)* bge-reranker-v2-m3, ViRanker | Chạy offline trên CPU | | Chỉ so tầng 1 |

- **Chỉ số:** nDCG@10, MRR@10, Recall@20 (riêng tầng 1), Hit@1, thời gian rerank mỗi câu hỏi, số token.
- **Đo thêm nếu API cho phép:** có và không có instruction viết riêng cho báo cáo tài chính (Qwen khuyến nghị instruction theo lĩnh vực).
- **Công bằng:** embed câu hỏi một lần, **lưu top-100 ứng viên** của mỗi câu hỏi ra file; mọi cấu hình rerank **đúng danh sách đó**.
- **Thống kê:** cùng bộ câu hỏi nên dùng **kiểm định ghép cặp** (Wilcoxon hoặc bootstrap), không dùng Welch t-test.
- **Chi phí:** khoảng 1–2 USD cho một lượt đo cả năm cấu hình trên 100 câu hỏi.
- **Cài đặt:** viết bằng Java, dùng chính `RerankClient` mà hệ thống sẽ dùng, để thời gian đo phản ánh đúng hệ thống thật.
- **Phụ thuộc:** (1) kho đã nạp vào Qdrant; (2) bộ câu hỏi có gắn trang đáp án (mục 4.2); (3) `RerankClient`.

### 2.3. Các ứng viên khác

| Model | Đặc điểm | Trạng thái |
|---|---|---|
| **ViRanker** (`namdp-ptit/ViRanker`) | Cross-encoder chuyên tiếng Việt, nền BGE-M3, 0,6B, Apache 2.0; **chỉ đọc 512 token** (chunk trung vị khoảng 1.190 token, phải chấm theo cửa sổ rồi lấy điểm cao nhất); huấn luyện trên MMARCO-VI, không phải văn bản tài chính (Dang và cộng sự, arXiv 2509.09131, 2025) | **Không có API.** Tuỳ chọn: so offline trên CPU máy con |
| **bge-reranker-v2-m3** | Reranker đa ngôn ngữ 0,6B | Không có trên DeepInfra. Tuỳ chọn: so offline |
| **Qwen3-VL-Reranker-2B** | Đa phương thức (chữ, ảnh, video), 2B, 32k token; MMTEB (Retrieval) 70,0, ViDoRe v3 60,8 | Không có trên DeepInfra. **Hướng phát triển:** rerank bằng **ảnh trang PDF** (đã có `PdfRenderer`) để tránh lỗi OCR trong bảng |
| **qwen3-rerank** (Alibaba) | 0,10 USD / 1 triệu token; tối đa 500 tài liệu, **4.000 token mỗi tài liệu**, **120.000 token mỗi lần gọi** (100 chunk × khoảng 1.250 token đã vượt); điểm chỉ so được trong cùng một lần gọi; có tham số `instruct`; không công bố cỡ model | Không chọn (mục 5.2) |

### 2.4. Rerank trong paper: model chuyên dụng, không dùng LLM lõi

Paper (mục 5.2, Bảng 2): tầng 1 `jina-reranker-v3` (nhẹ), tầng 2 thử nhiều model; **`Qwen3-Reranker-8B` tốt nhất (NDCG@20 0,7918)**, `Qwen3-Reranker-4B` 0,7763; hai tầng hơn một tầng khoảng 6,5%. LLM (Claude 4.6 Opus tốt nhất, độ chính xác 0,8152) chỉ dùng để sinh câu trả lời.

FinQuery cũng dùng reranker chuyên dụng (cross-encoder, trả điểm) qua API DeepInfra, không dùng Claude để rerank: chấm 100 ứng viên bằng Claude tốn khoảng 0,36 USD mỗi câu hỏi (120k token × 3 USD / 1 triệu), gấp khoảng 100 lần reranker, chậm và điểm không ổn định. Phần đề tài tự làm là **thuật toán**: Qdrant top 100 → tầng 1 → top 20 → tầng 2 → ngữ cảnh ≤ 64k thì LLM trả lời một lần, vượt thì chia đôi và ghép theo luật của paper (chỉ một bên có đáp án thì lấy bên đó; cả hai có thì lấy bên tự tin hơn; không bên nào thì "không xác định"). Với kho này top 20 khoảng 25k token, nhánh chia đôi hiếm khi chạy; vẫn cài và có test ép ngưỡng nhỏ.

Số đo Bảng 2 (8B hơn 4B khoảng 0,016 ở tầng 2) khác bảng điểm đa ngôn ngữ của Qwen (8B gần như không hơn): càng cần giữ cấu hình B, D trong benchmark. Cấu hình ghi sẵn trong `.env` (comment): A 0.6B → 4B (mặc định), B 4B → 8B, C và D một tầng (4B, 8B chấm thẳng 100). Người làm đề tài không dùng reranker của NVIDIA.

### 2.5. API rerank của DeepInfra

`POST https://api.deepinfra.com/v1/inference/<tên model>`, body `{"queries": [...], "documents": [...]}`, trả mảng `scores` kèm `input_tokens` và `inference_status` (có `runtime_ms`, `cost`). **Không theo chuẩn OpenAI.** Tài liệu không ghi số tài liệu tối đa mỗi lần gọi và **không nhắc tới trường instruction**: cần thử thực tế khi viết `RerankClient`. Biến `.env`: `RERANK_API_URL=https://api.deepinfra.com/v1/inference`.

## 3. Embedding

Đổi sang **`Qwen/Qwen3-Embedding-8B`** qua DeepInfra, 4.096 chiều. Chi tiết, số đo và kết quả thử ở `lap-chi-muc_v1.9.md`, mục 1.6.

| Model | MMTEB, trung bình | MMTEB, truy hồi | Số chiều | Giá DeepInfra |
|---|---|---|---|---|
| Qwen3-Embedding-4B | 69,45 | 85,05 | 2.560 | 0,020 USD / 1 triệu token |
| **Qwen3-Embedding-8B** | **70,58** | **86,40** | 4.096 | **0,010** USD / 1 triệu token |

Thí nghiệm tuỳ chọn cho chương đánh giá: so 4B với 8B trên bộ câu hỏi (Recall@5, MRR). Chi phí không đáng kể.

## 4. Đánh giá trong lúc phát triển

### 4.1. Đánh giá từng tầng, không cần model suy luận

Mỗi câu hỏi có **trang đáp án** (`tai_lieu_id` + `trang`). Chấm truy hồi và rerank chỉ cần hỏi *trang đúng có trong kết quả không, đứng thứ mấy*, không cần LLM viết câu trả lời. Truy hồi không lấy được trang đúng thì LLM nào cũng không trả lời đúng, nên **truy hồi là tầng đáng đo nhất và rẻ nhất**.

| Tầng | Đo | Cần | Chi phí mỗi lượt |
|---|---|---|---|
| Chunking | Thuật toán, số chunk, gối đầu | Unit test (đã có 11 test) | 0 |
| Tiền truy xuất, phần quy tắc | Trích đúng bộ lọc | Unit test | 0 |
| Truy hồi (embedding + bộ lọc + Qdrant) | Recall@k | API embedding, Qdrant | Vài cent |
| Diễn giải câu hỏi | Recall@k khi có và không có câu diễn giải | Thêm một LLM nhỏ | Rất nhỏ |
| Rerank | nDCG@10, MRR, Recall@20, thời gian | API rerank | khoảng 0,2–0,3 USD (100 câu, một cấu hình) |
| **Câu trả lời cuối** | Đúng con số, đúng trích dẫn | **Model suy luận** | **Đắt nhất**: chỉ chạy ở cuối, trên bộ nhỏ |

Lưu kết quả trung gian (vector câu hỏi, top-100 ứng viên) để đổi một thành phần thì chỉ chạy lại đúng thành phần đó. Mỗi thay đổi (thuật toán chunk, bộ lọc, model) đều có con số chứng minh tốt hơn hay tệ hơn, giống kiểm thử hồi quy cho chất lượng truy hồi.

### 4.2. Bộ câu hỏi có gắn trang đáp án (chốt 50 câu, 10/10/2026)

Người làm đề tài chốt **50 câu** (tham khảo: một nhóm thường dùng khoảng 100 câu, một người khoảng 50). Thay cho 60–100 câu ở bản trước.

| Nhóm | Số câu | Đáp án ghi kèm |
|---|---|---|
| RAG: tra số trong bảng | 10 | `tai_lieu_id` + `trang` |
| RAG: giải thích bằng chữ (thuyết minh, BCTN) | 10 | `tai_lieu_id` + `trang` |
| RAG: vắt trang hoặc có kỳ cụ thể | 5 | `tai_lieu_id` + `trang` |
| DB: một chỉ tiêu, một kỳ | 8 | `ma_noi_bo` + kỳ + phạm vi |
| DB: so sánh, tăng trưởng, chỉ số | 7 | các `ma_noi_bo` + kỳ |
| Không trả lời được (phải từ chối) | 5 | không có |

- Sư phụ (Claude) soạn bản nháp; người làm đề tài duyệt, sửa, và tự viết thêm vài câu theo cách người làm kế toán thực sự hỏi (câu do LLM đặt hay lặp đúng chữ trong trang).
- **Thống kê với 50 câu:** chỉ khác biệt đủ lớn mới có ý nghĩa (ước chừng nDCG chênh từ 0,05). Dùng **kiểm định ghép cặp** (Wilcoxon) và **khoảng tin cậy bootstrap**; chênh ít thì ghi "không phân biệt được với 50 câu", không kết luận quá tay.
- Chi phí một lượt benchmark rerank giảm theo (50 thay vì 100 câu).

### 4.3. Bộ test công khai

| Bộ test | Nội dung | Hợp với FinQuery tới đâu |
|---|---|---|
| **VN-MTEB** (Pham và cộng sự, 07/2025) | 41 bộ dữ liệu tiếng Việt, sáu tác vụ (có rerank), tạo bằng dịch máy từ MTEB rồi lọc bằng LLM; bộ sưu tập `GreenNode/vn-mteb` trên Hugging Face | So trên mặt bằng chung; văn bản dịch, chủ đề đời thường |
| **MMARCO-VI** | Bản dịch tiếng Việt của MS MARCO | Như trên, đoạn văn ngắn |
| **FinDER** (2025), **FinRAG-QA** (09/2026) | Hỏi đáp, truy hồi trên báo cáo tài chính | Đúng lĩnh vực nhưng **tiếng Anh** |

**Không tìm thấy bộ test truy hồi trên báo cáo tài chính tiếng Việt.** Bộ câu hỏi tự xây (mục 4.2) là một đóng góp của đề tài. Tuỳ chọn: chạy thêm phần rerank của VN-MTEB để xem thứ hạng các reranker trên mặt bằng chung có khác trên dữ liệu tài chính không.

## 5. Hạ tầng gọi model

### 5.1. Sự cố key Viettel AI

| Thời điểm kiểm tra | Kết quả |
|---|---|
| 09/10/2026 | Embedding và chat trả **HTTP 400 `budget_exceeded`**: credit của tài khoản cấp key hết hạn lúc 2026-10-08 13:41:14 UTC (20:41 giờ Việt Nam). `/key/info` vẫn đọc được, key không bị khoá |
| 10/10/2026 10:19 | **Không đổi**: embedding, chat và rerank (`Qwen/Qwen3-Reranker-4B`) đều 400; tổng chi tiêu của key đứng yên |

Cấu hình Viettel được giữ trong `.env` dưới dạng dự phòng; khi được gia hạn thì đổi lại.

### 5.2. Các lựa chọn đã cân nhắc

**Hai cách tính tiền khác nhau về bản chất:** thuê GPU trả tiền theo **thời gian máy bật** (kể cả lúc ngồi không); API trả tiền theo **số token** thật sự xử lý.

| Phương án | Ưu | Nhược | Kết luận |
|---|---|---|---|
| **Thuê GPU, Vast.ai** | Rẻ nhất trong các nơi thuê (máy đã xác minh, giá trung vị 09/10: RTX 3090 0,204 USD/giờ, RTX A6000 48 GB 0,444, RTX 5090 0,704); tính theo giây; có template vLLM; chạy được mọi model | Phải tự dựng server; máy phần lớn ở Mỹ và châu Âu (độ trễ thêm khoảng 0,2–0,3 giây mỗi lượt gọi, làm sai số đo độ trễ); máy dừng vẫn tính tiền ổ đĩa (khoảng 0,20 USD / GB / tháng), huỷ thì mất dữ liệu; cần thẻ quốc tế | Loại |
| **Thuê GPU, ezycloudx** (TP.HCM) | Trong nước, độ trễ thấp; thanh toán VND bằng chuyển khoản hoặc ví, **không cần thẻ**; máy nhiều RAM | Máy thuê chạy **Windows 10 Pro** (vLLM chỉ chạy trên Linux; có dịch vụ "GPU Docker" nhưng không rõ chi tiết); không ghi có IP công khai, mở cổng; **dữ liệu bị xoá sau mỗi phiên**; giá RTX 3090 9.000 đ/giờ | Loại (giữ làm phương án khi cần máy trong nước) |
| **Alibaba Token Plan (gói 6 USD/tháng)** | Rẻ, nhiều model | Điều khoản **chỉ cho dùng tương tác trong công cụ lập trình**, **cấm** backend tự viết và gọi theo lô; không có embedding và rerank | Loại (vi phạm điều khoản) |
| **Alibaba trả theo lượt** | Qwen chính chủ; embedding `qwen3.7-text-embedding` 0,07 USD / 1 triệu token, có 1 triệu token miễn phí trong 90 ngày | Rerank chỉ một model (`qwen3-rerank`, 0,10 USD), giới hạn 120.000 token mỗi lần gọi; đắt hơn DeepInfra khoảng 4–7 lần | Loại |
| **Router Hugging Face** | Một token, nhiều nhà cung cấp, không cộng phí | Endpoint tương thích OpenAI chỉ cho chat; embedding phải dùng định dạng khác, không có base64; tài khoản miễn phí không có credit | Loại |
| **DeepInfra** | Đủ mọi model cần; rẻ nhất; embedding tương thích OpenAI và có base64 | Máy chủ không ghi rõ vị trí (nhiều khả năng ở Mỹ); cần thẻ quốc tế; API rerank không theo chuẩn OpenAI | **CHỌN** |

Chi phí ước tính tới 31/10 với mức dùng vừa phải: thuê RTX 3090 trên Vast.ai khoảng 12 USD (khoảng 60 giờ máy), trên ezycloudx khoảng 540.000 đ; gọi API DeepInfra khoảng 5 USD. Thuê GPU chỉ rẻ ngang API khi máy bận gần như 100% thời gian, điều không xảy ra trong lúc phát triển. Thuê GPU chỉ đáng khi cần model không có API (ViRanker, bge-reranker, Qwen3-VL-Reranker-2B) hoặc cần dữ liệu ở trong nước.

### 5.3. DeepInfra: model và giá (tra 10/10/2026)

| Thành phần | Model | Giá (USD / 1 triệu token, vào / ra) |
|---|---|---|
| Embedding | `Qwen/Qwen3-Embedding-8B` | 0,010 |
| Rerank tầng 1 / tầng 2 | `Qwen/Qwen3-Reranker-0.6B` / `Qwen/Qwen3-Reranker-4B` | 0,010 / 0,025 |
| LLM diễn giải câu hỏi | `openai/gpt-oss-120b` hoặc `Qwen/Qwen3-32B` | 0,037 / 0,17; 0,08 / 0,28 |
| Model suy luận | `anthropic/claude-sonnet-4-6` (đúng model đang dùng) | 3,0 / 15,0 |
| OCR tài liệu mới | `Qwen/Qwen3-VL-30B-A3B-Instruct` (không có đúng bản 32B) | 0,15 / 0,60 |

**Quyền riêng tư** (trang chính sách của DeepInfra): không ghi dữ liệu gửi lên ra đĩa khi suy luận thông thường; không lưu nội dung request vào log (chỉ lưu thông tin gỡ lỗi như mã request, chi phí); không huấn luyện trên dữ liệu người dùng. **Ngoại lệ:** với model của Anthropic hoặc Google, dữ liệu được chuyển sang Anthropic/Google và áp dụng chính sách của họ. Trang không ghi vị trí máy chủ.

**Ngân sách:** đã nạp **5 USD** (10/10/2026). Ước tính tới 31/10: embedding dưới 0,5 USD; rerank và diễn giải qua nhiều lượt đánh giá khoảng 3–5 USD; model suy luận (khoảng 0,03 USD mỗi câu hỏi) khoảng 5–10 USD. Tổng khoảng **10–15 USD**; khoản lớn nhất là model suy luận. Có thể so Claude Sonnet 4.6 với một model mở rẻ (ví dụ DeepSeek-V4-Flash) trên khoảng 20 câu hỏi cho mục "tác động khi đổi mô hình" của chương đánh giá.

### 5.4. Cấu hình `.env` (10/10/2026)

| Biến | Giá trị |
|---|---|
| `LLM_API_KEY` | Key DeepInfra, dùng chung cho mọi model |
| `LLM_API_URL` | `https://api.deepinfra.com/v1/openai/chat/completions` |
| `REASONING_MODEL` | `anthropic/claude-sonnet-4-6` |
| `OCR_MODEL` | `Qwen/Qwen3-VL-30B-A3B-Instruct` |
| `EMBEDDING_API_URL`, `EMBEDDING_MODEL`, `EMBEDDING_DIMENSIONS` | `https://api.deepinfra.com/v1/openai/embeddings`, `Qwen/Qwen3-Embedding-8B`, `4096` |
| `QDRANT_COLLECTION` | `finq_pages_qwen3e8b` |
| `RERANK_API_URL`, `RANKER_LAYER_1ST_MODEL`, `RANKER_LAYER_2ND_MODEL` | `https://api.deepinfra.com/v1/inference`, `Qwen/Qwen3-Reranker-0.6B`, `Qwen/Qwen3-Reranker-4B` |

## 6. Tài liệu tham khảo

- Qwen3-Embedding-8B (bảng điểm MMTEB): https://huggingface.co/Qwen/Qwen3-Embedding-8B
- Qwen3-Reranker-4B (bảng điểm MTEB-R, MMTEB-R, MLDR): https://huggingface.co/Qwen/Qwen3-Reranker-4B
- Qwen3-VL-Reranker-2B: https://huggingface.co/Qwen/Qwen3-VL-Reranker-2B
- Dang, P.-N., Nguyen, K.-L., Pham, T.-H. (2025). *ViRanker.* arXiv:2509.09131. https://arxiv.org/abs/2509.09131 — model: https://huggingface.co/namdp-ptit/ViRanker
- Pham, L. và cộng sự (2025). *VN-MTEB: Vietnamese Massive Text Embedding Benchmark.* arXiv:2507.21500. https://arxiv.org/abs/2507.21500
- *FinDER: Financial Dataset for Question Answering and Evaluating RAG.* arXiv:2504.15800. https://arxiv.org/abs/2504.15800
- Miola, A. và cộng sự (2026). *Enhancing Financial Question Answering: A Novel Benchmark Dataset of Banks' financial statements.* arXiv:2609.03654. https://arxiv.org/abs/2609.03654
- DeepInfra: Qwen3-Embedding-4B API https://deepinfra.com/Qwen/Qwen3-Embedding-4B/api — Qwen3-Reranker-4B API https://deepinfra.com/Qwen/Qwen3-Reranker-4B/api — chính sách dữ liệu https://docs.deepinfra.com/account/data-privacy
- Hugging Face Inference Providers: https://huggingface.co/docs/inference-providers/index — giá: https://huggingface.co/docs/inference-providers/pricing
- Alibaba Cloud Model Studio: embedding https://www.alibabacloud.com/help/en/model-studio/embedding — qwen3-rerank https://www.alibabacloud.com/help/en/model-studio/qwen3-rerank — API rerank https://www.alibabacloud.com/help/en/model-studio/text-rerank-api — Token Plan https://www.alibabacloud.com/help/en/model-studio/token-plan-personal-overview
- Vast.ai: https://vast.ai/pricing — ezycloudx: https://ezycloudx.com

### 5.5. Model suy luận và danh sách so sánh (11/10/2026)

DeepInfra có `anthropic/claude-sonnet-5-5`: **2 / 10 USD** mỗi 1 triệu token vào / ra, ngữ cảnh 1 triệu token, có nhãn "Function"; rẻ hơn `claude-sonnet-4-6` (3 / 15) khoảng một phần ba. Một câu hỏi RAG điển hình (25k token vào, 600 ra): khoảng 0,056 USD thay vì 0,084 USD. Người làm đề tài đặt `REASONING_MODEL=anthropic/claude-sonnet-5-5`.

Danh sách so sánh (theo yêu cầu: không dùng Opus vì đắt), ghi trong `.env`:

| Model | Giá (USD / 1 triệu token vào / ra) | Vai trò |
|---|---|---|
| `anthropic/claude-sonnet-5-5` | 2 / 10 | Mặc định |
| `anthropic/claude-sonnet-4-6` | 3 / 15 | Mốc: model cũ |
| `Qwen/Qwen3.5-397B-A17B` | 0,45 / 3 | Model mở |

Trước khi dùng cho nhánh DB phải gọi thử tool (function calling). Chi phí một lượt so sánh 3 model trên 50 câu: khoảng 5 USD. Tương tự Bảng 3 của paper (so nhiều LLM trên cùng quy trình).

---

## 7. Cài đặt truy hồi (11/10/2026)

### 7.1. Số chunk mỗi tầng: 80 → 40 → 20

**Paper không nêu số ứng viên lấy từ kho trước khi rerank.** Mục 3.1 chỉ nói "embedding-based coarse retrieval followed by fine-grained reranking"; thuật toán 1 viết `C100 ← Rerank1(Q′, C′)` trên cả kho C′. Paper chỉ ghi rõ tầng 1 ra 100, tầng 2 ra 20, và chia 20 thành 1–10, 11–20 khi vượt 64k token.

Các phương án đã cân nhắc (đơn vị là **chunk**, không phải trang: trang dài hơn 4.000 ký tự bị cắt thành nhiều chunk):

| Phương án | Ưu | Nhược |
|---|---|---|
| 300 → 100 → 20 (bám paper) | ít sót đáp án ở bước Qdrant | rerank chậm hơn (~3–5 giây), tầng 2 chấm 100 chunk |
| 50 → 20 → 10 | rẻ nhất (LLM chỉ đọc 10 chunk) | dễ sót trang ở bước Qdrant; câu so sánh nhiều kỳ có thể thiếu ngữ cảnh |
| 80 → 60 → 20 | — | tầng 1 chỉ loại 25%, gần như không có tác dụng |
| **80 → 40 → 20 (chốt)** | tầng 1 loại một nửa, tầng 2 nhanh (~1,5 giây), LLM vẫn có 20 chunk như paper | phải kiểm chứng Recall@80 |

Kiểm chứng: đo Recall theo từng tầng trên bộ 50 câu (Qdrant @80 → tầng 1 @40 → tầng 2 @20), so với @100, @200 của riêng bước Qdrant. Đáp án gán theo **trang**, nên đổi chunk về trang (mỗi trang tính một lần) trước khi tính.

### 7.2. Trích từ khoá (`preretrieval/keyword/KeywordExtractor`)

- Nguồn: (1) tên chỉ tiêu trong `financial_items.ten_chuan` và `financial_item_mappings.ten_tren_bao_cao`, làm gọn (bỏ tiền tố "LCTT —", "Trong đó:", bỏ phần "— mã 140 (...)"), bỏ tên ngắn hơn 8 ký tự; (2) cách gọi đời thường trong `resources/preretrieval/keywords.yml` (ví dụ "lãi ròng" → "lợi nhuận sau thuế thu nhập doanh nghiệp").
- So khớp trên câu **đã chuẩn hoá**, bỏ dấu, theo ranh giới từ. Nhờ bộ chuẩn hoá, viết tắt cũng được bắt ("LNST (lợi nhuận sau thuế)").
- Cách gọi đời thường nằm **bên trong** một tên chỉ tiêu dài hơn đã khớp thì bỏ qua: "tài sản" trong "tài sản ngắn hạn" không kéo theo "tổng cộng tài sản"; "doanh thu" trong "doanh thu hoạt động tài chính" không kéo theo "doanh thu thuần".
- Cụm nằm trong cụm dài hơn thì bỏ; tối đa 6 từ khoá.
- Q′ = câu đã chuẩn hoá + dòng `Từ khoá: a; b; c`. Q′ dùng cho cả embed và rerank, đúng thuật toán 1 của paper.

### 7.3. Trích bộ lọc (`preretrieval/filter/FilterExtractor`, `retrieval/QdrantFilters`)

| Trường | Rút từ đâu | Ví dụ |
|---|---|---|
| `ma_cong_ty` | bộ chuẩn hoá (quy tắc 6), mặc định CTR | luôn lọc |
| `nam` | số 2010–2039 đứng riêng (không dính số tiền) | "Q3/2024" → 2024; "năm ngoái (năm 2025)" → 2025; "2024 so với 2023" → [2023, 2024] |
| `ky` | "quý N"/"QN"; ngày cuối kỳ; "bán niên", "6 tháng đầu năm" → bán niên + Q2; "cả năm", "cuối năm" → năm + Q4 | "gần nhất (kỳ 30/06/2026)" → Q2 + bán niên |
| `pham_vi` | bộ chuẩn hoá, mặc định hợp nhất | "báo cáo riêng" → riêng |
| `loai` | chỉ khi gọi đúng tên loại tài liệu | "nghị quyết" → nghi_quyet; "báo cáo thường niên" → bctn; "cổ tức" → không lọc |

Dịch sang Qdrant (lọc **trong lúc** tìm, dùng payload index):
```
must: ma_cong_ty = CTR
      nam ∈ {2024}
      (ky ∈ {Q3}          OR ky trống)       ← nghị quyết, giải trình không có kỳ
      (pham_vi = hop_nhat OR pham_vi trống)  ← bctn không có phạm vi
      loai ∈ {...}                           (chỉ khi có)
```
Lọc chặt ra ít hơn 5 chunk thì tìm lại với bộ lọc nới (chỉ `ma_cong_ty`); kết quả ghi `filterMode = relaxed`.

### 7.4. Tìm Qdrant và rerank hai tầng

- `QdrantRetriever`: embed Q′ với tiền tố `Instruct: ...` / `Query:` mà Qwen3-Embedding khuyên dùng cho phía câu hỏi (chunk lúc nạp không có tiền tố); `retrieval.query-instruction` để trống thì bỏ tiền tố (để so sánh).
- `RerankClient`: `POST https://api.deepinfra.com/v1/inference/{model}` với `{"queries":[Q′],"documents":[...]}`. Qwen3-Reranker chấm **từng cặp** (câu hỏi, chunk) độc lập, điểm = xác suất model trả lời "yes" (0..1), nên chia lô không đổi điểm. Lô 40 chunk, các lô gửi **song song**. Số điểm trả về khác số chunk gửi lên thì báo lỗi ngay. Ghi `input_tokens` và `inference_status.cost` thật.
- `HierarchicalReranker`: sắp giảm dần theo điểm, lấy top N (không dùng ngưỡng điểm); bằng điểm thì chunk có điểm vector cao hơn đứng trước. Ít hơn 40 ứng viên thì bỏ tầng 1. Model tầng nào để trống trong `.env` thì bỏ tầng đó (cấu hình rerank một tầng C/D). Văn bản đưa reranker = `[tên tài liệu]` + `TableToJson(noi_dung)`, đúng bản đã embed. LLM về sau đọc markdown (ít token hơn JSON).

### 7.5. Service, endpoint, test

- `RetrievalService.retrieve(câu hỏi, RetrievalOptions)`: công tắc `keywords`, `filter`, `rerank` để benchmark đo riêng từng bước. Trả `RetrievalResult` gồm Q′, từ khoá, bộ lọc, chế độ lọc, số chunk Qdrant trả, số đo từng tầng (model, vào → giữ, ms, token, USD), top 20 chunk (điểm vector, tầng 1, tầng 2), thời gian từng bước.
- Endpoint thử: `GET /api/v1/retrieval/search?q=...&keywords=true&filter=true&rerank=true`.
- Test (không cần mạng): `KeywordExtractorTest` (6), `FilterExtractorTest` (7, có in Filter của Qdrant), `HierarchicalRerankerTest` (5, reranker giả: 80 → 40 → 20, bỏ tầng 1 khi ≤ 40, một tầng, văn bản đưa reranker, đọc response thật của DeepInfra). Toàn bộ: 65 test, 0 lỗi, 4 bỏ qua (test Qdrant thật khi Qdrant tắt).
- Chạy thật: `./mvnw test -Dtest=RetrievalTrialTest -Dtrial.retrieval=true` → `trial-retrieval.md` (6 câu mẫu, cần MySQL + Qdrant). **Chưa chạy** tại thời điểm ghi: Docker đang tắt.

### 7.6. Chi phí ước tính (QĐ-031)

Giả sử ~1.000 token mỗi chunk (kể cả khung lời nhắc của reranker):

| Bước | Token | Tiền mỗi câu |
|---|---|---|
| Rerank tầng 1, 0.6B × 80 chunk | ~80k | ~0,0008 USD |
| Rerank tầng 2, 4B × 40 chunk | ~40k | ~0,001 USD |
| LLM, 20 chunk, Qwen3.5 | ~21k vào + ~500 ra | ~0,011 USD |
| LLM, 20 chunk, Sonnet 5.5 | như trên | ~0,047 USD |

Gọi thử thật một lần (3 tài liệu ngắn): 0.6B 293 token, 0,0000029 USD, 86 ms; 4B 293 token, 0,0000073 USD, 820 ms. Số đo thật mỗi câu sẽ lấy từ `RetrievalResult`.

### 7.7. Chạy thật lần đầu (11/10/2026, 6 câu mẫu, `trial-retrieval.md`)

Độ chính xác: cả 6 câu đều có chunk đúng ở hạng 1 (BCKQKD hợp nhất Q3/2024 trang 5; BCKQKD năm 2025; BCTC **riêng** năm 2023; lịch sử cổ tức trong BCTN 2024; **công văn giải trình** Q2/2025; định hướng trong nghị quyết và BCTN). Bộ lọc chạy chế độ `strict` ở cả 6 câu, không phải nới.

| Số đo mỗi câu | Lô 40 | Lô 10 |
|---|---|---|
| Rerank (2 tầng) | 5,1–8,1 giây, trung bình 6,6 | 4,7–8,0 giây, trung bình 5,9 |
| Embed câu hỏi | 0,5–11 giây (dao động lớn) | 0,5–15 giây |
| Token tầng 1 / tầng 2 | ~67–135k / ~31–67k | như cũ |
| Tiền rerank | ~0,0015–0,003 USD | như cũ |

Nhận xét:
- Mỗi chunk thực tế ~1.300 token (ước tính 1.000). Tiền rerank cả lượt 6 câu: 0,0146 USD.
- Chia lô nhỏ hơn chỉ nhanh ~10%: nút thắt là tổng token, không phải số lô gửi song song. Giữ lô 40.
- Embed câu hỏi qua DeepInfra lúc nhanh lúc chậm (0,5–15 giây), là phần dao động lớn nhất.
- Bản tiếng Anh của cùng tài liệu (BCTC, công văn, nghị quyết song ngữ) chiếm chỗ trong top 20: tốn token của LLM mà không thêm thông tin.

Hướng giảm độ trễ (chưa làm, đo trong benchmark): cắt ngắn chunk khi đưa tầng 1 (model nhỏ chỉ cần lọc thô); loại trang tiếng Anh trùng nội dung; lưu sẵn kết quả câu hỏi lặp lại (Redis); so với rerank một tầng 4B.

### 7.8. Bản đồ file của truy hồi (11/10/2026)

Gốc mã: `backend/core-service/src/main/java/com/vdt/finq/`. Thứ tự bảng theo đúng thứ tự chạy của một câu hỏi.

**Cấu hình**

| File | Chức năng |
|---|---|
| `common/properties/RetrievalProperties.java` | Khối `retrieval:`: `top-k` 80 (số chunk lấy từ Qdrant), `tier1-keep` 40, `final-k` 20 (số chunk đưa LLM), `min-filtered-hits` 5 (ngưỡng nới lọc), `query-instruction` (tiền tố hướng dẫn của Qwen3-Embedding). |
| `common/properties/RerankProperties.java` | Khối `rerank:`: URL, key, `tier1-model`, `tier2-model` (để trống = bỏ tầng), `batch-size` 40 (số chunk tối đa **mỗi lần gọi** API), timeout, số lần thử lại. |
| `resources/application.yml` | Giá trị thật của hai khối trên; **ghi đè** giá trị mặc định trong code Java. |
| `.env` | `RANKER_LAYER_1ST_MODEL`, `RANKER_LAYER_2ND_MODEL`, `RERANK_API_URL`; `REASONING_MODEL` mặc định Qwen3.5 (QĐ-031). |

**Tiền truy xuất** (`modules/hierarchicalrag/preretrieval/`)

| File | Chức năng |
|---|---|
| `normalizer/FinancialTextNormalizer.java` (đã có từ trước) | 6 quy tắc chuẩn hoá (QĐ-027): thêm dạng chuẩn trong ngoặc, điền công ty, phạm vi mặc định, đổi thời gian tương đối theo kỳ mới nhất. |
| `keyword/KeywordExtractor.java` | Trích tối đa 6 từ khoá: tên chỉ tiêu trong `financial_items`, `financial_item_mappings` (đọc từ MySQL lúc khởi động) khớp nguyên cụm, cộng cách gọi đời thường trong `keywords.yml`. Bỏ cách gọi nằm bên trong một tên chỉ tiêu dài hơn đã khớp. |
| `resources/preretrieval/keywords.yml` | Từ điển "cách người dùng gọi → tên đúng trên báo cáo" (lãi ròng → lợi nhuận sau thuế…, cổ tức, nhân sự, dòng tiền…). Sửa file này, không phải sửa code. |
| `filter/QueryFilter.java` | Dữ liệu bộ lọc: `maCongTy`, `nams`, `kys`, `phamVi`, `loais`, danh sách luật đã chạy (`applied`). |
| `filter/FilterExtractor.java` | Đọc câu đã chuẩn hoá → năm (số 2010–2039 đứng riêng), kỳ (quý, ngày cuối kỳ, bán niên, cả năm), loại tài liệu (chỉ khi gọi đúng tên). Công ty, phạm vi lấy từ bộ chuẩn hoá. |
| `PreparedQuery.java` | Kết quả tiền truy xuất: câu đã chuẩn hoá, từ khoá, **Q′** (câu + dòng từ khoá), bộ lọc. |
| `QueryPreprocessor.java` | Ghép 3 bước: chuẩn hoá → từ khoá → bộ lọc. Có công tắc tắt từ khoá để benchmark. |

**Truy hồi** (`modules/hierarchicalrag/retrieval/`)

| File | Chức năng |
|---|---|
| `QdrantFilters.java` | Dịch `QueryFilter` sang `Filter` của Qdrant. `strict`: công ty AND năm AND (kỳ OR không có kỳ) AND (phạm vi OR không có phạm vi) AND loại. `relaxed`: chỉ công ty. |
| `QdrantRetriever.java` | Gửi vector Q′ + bộ lọc + `limit` (= `top-k`) lên Qdrant, nhận chunk đã sắp theo cosine, đọc payload thành `RetrievedChunk`. |
| `RetrievedChunk.java` | Một chunk ứng viên: toạ độ (tài liệu, trang, phần), metadata, `noiDung` markdown, điểm vector, tên tài liệu, điểm tầng 1, tầng 2. |
| `rerank/RerankClient.java` | Gọi `POST {RERANK_API_URL}/{model}` của DeepInfra; chia lô `batch-size`, gửi các lô song song (virtual thread), thử lại khi 429/5xx, kiểm số điểm = số chunk, cộng `input_tokens` và `cost` thật. |
| `rerank/HierarchicalReranker.java` | Rerank hai tầng: tầng 1 chấm 80 giữ 40, tầng 2 chấm 40 giữ 20; sắp giảm dần theo điểm, bằng điểm thì xét điểm vector. Bỏ tầng 1 khi ≤ 40 ứng viên hoặc không cấu hình model. Văn bản đưa reranker = `[tên tài liệu]` + `TableToJson(noi_dung)`. |
| `rerank/TierStat.java` | Số đo một tầng: model, vào → giữ, ms, token, USD, lý do bỏ qua. |
| `RetrievalOptions.java` | Công tắc `keywords`, `filter`, `rerank` cho benchmark; `DEFAULT` = bật hết. |
| `RetrievalService.java` | Chạy cả luồng: tiền truy xuất → embed Q′ (kèm tiền tố hướng dẫn) → Qdrant lọc chặt, < 5 chunk thì nới → gắn tên tài liệu từ MySQL → rerank → `RetrievalResult` kèm thời gian từng bước. |
| `RetrievalResult.java` | Kết quả trả về: Q′, bộ lọc, chế độ lọc, số chunk Qdrant trả, số đo từng tầng, top 20 chunk, thời gian, tiền rerank. |
| `RetrievalController.java` | `GET /api/v1/retrieval/search?q=...&keywords=&filter=&rerank=`: thử bằng tay, tắt từng bước để so sánh. |

**Test** (`src/test/java/com/vdt/finq/modules/hierarchicalrag/`)

| File | Chức năng |
|---|---|
| `preretrieval/keyword/KeywordExtractorTest.java` | 6 test, không cần DB: cách gọi đời thường, viết tắt qua bộ chuẩn hoá, tên chỉ tiêu, bỏ cách gọi nằm trong tên dài hơn, câu không liên quan, làm gọn tên. |
| `preretrieval/filter/FilterExtractorTest.java` | 7 test: quý + năm, thời gian tương đối, bán niên / cả năm, nhiều năm, loại tài liệu, không lọc khi không nói rõ, in `Filter` của Qdrant. |
| `retrieval/rerank/HierarchicalRerankerTest.java` | 5 test với reranker giả (không tốn tiền): 80 → 40 → 20, bỏ tầng 1 khi 25 chunk, một tầng, văn bản đưa reranker, đọc response thật của DeepInfra. |
| `retrieval/RetrievalTrialTest.java` | Chạy thật 6 câu mẫu, ghi `trial-retrieval.md` (cần MySQL + Qdrant, ~0,015 USD). Trong PowerShell phải bọc tham số trong ngoặc kép: `./mvnw test "-Dtest=RetrievalTrialTest" "-Dtrial.retrieval=true"`. |

### 7.9. Ghi chú vận hành

- **PowerShell cắt tham số `-D...` ở dấu chấm** (`-Dtrial.retrieval=true` thành `.retrieval=true`, Maven báo "Unknown lifecycle phase"). Bọc tham số trong ngoặc kép. Git Bash không bị.
- **Đổi số mà không sửa code:** `"-Dretrieval.top-k=100"`, `"-Drerank.batch-size=10"`, `"-Dretrieval.query-instruction="` (bỏ tiền tố hướng dẫn) khi chạy test hoặc app.
- `RerankProperties.batchSize` trong code từng còn 50 (lần sửa trước không khớp chuỗi); đã đổi về 40. Lượt chạy thật không bị ảnh hưởng vì `application.yml` ghi đè thành 40.
- Toàn bộ test: **65 qua, 0 lỗi**, 4 bỏ qua (test Qdrant thật không bật cờ); `RetrievalTrialTest` chạy thật qua.
