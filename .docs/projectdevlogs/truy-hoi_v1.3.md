# Truy hồi (tiền truy xuất, rerank), đánh giá trong lúc phát triển và hạ tầng gọi model — v1.3

> Ngày: 11/10/2026. Phạm vi: phía **trả lời câu hỏi** của nhánh RAG, sau khi kho đã được lập chỉ mục (xem `lap-chi-muc_v1.12.md`).
> Luồng: câu hỏi → **tiền truy xuất** (chuẩn hoá, trích bộ lọc và từ khoá, diễn giải) → embed → Qdrant (khoảng 100 ứng viên) → **rerank tầng 1** (khoảng 20) → **rerank tầng 2** → LLM sinh câu trả lời kèm trích dẫn.

## Thay đổi so với bản trước (v1.2)

- **CHỐT bước chuẩn hoá: 6 quy tắc** (mục 1.5, QĐ-027 trong `quyet-dinh_v1.16.md`): cách paper mô tả, 6 quy tắc, nguyên tắc "thêm dạng chuẩn, giữ chữ gốc", nơi gọi (lúc nạp và lúc hỏi), đầu ra `NormalizedQuery`.
- **Chuẩn hoá ngữ liệu:** phân tích Bảng 1 cho thấy paper không chứng minh được nó có lợi; chỉ làm qua thí nghiệm 2 × 2 và quy tắc quyết định chốt trước (mục 1.6).
- Trạng thái: **chưa cài đặt** bộ chuẩn hoá; collection đã chuẩn hoá ngữ liệu **chưa tồn tại**.

(Thay đổi của v1.2 so với v1.1: bộ câu hỏi chốt 50 câu; số đo tiền truy xuất của paper; quan sát khi tìm thử; thêm thí nghiệm định dạng bảng vào benchmark.)

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

### 2.4. API rerank của DeepInfra

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
