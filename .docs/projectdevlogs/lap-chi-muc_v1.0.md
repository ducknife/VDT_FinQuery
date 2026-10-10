# Indexing pipeline (lập chỉ mục) — v1.0

> Ngày: 06/10/2026. Phạm vi: 6.690 trang `trang_NNN.md` của 278 tài liệu CTR.
> Luồng: `PageStore.read()` → `TextCleaner.clean()` → **chunk** → **embed** → upsert vào Qdrant → cập nhật `documents.ngay_embed`, `so_chunk`.
> Bản này chốt hai việc: **model embedding** và **thuật toán chunk**. Các phần khác ở mục 4 mới là đề xuất.

## 1. Model embedding — CHỐT: `Qwen3-Embedding-4B`

### 1.1. Các ứng viên trên gateway Viettel AI

Gateway là LiteLLM, tương thích OpenAI, endpoint `POST /v1/embeddings`. Mỗi model gọi 3 lần, lúc 16:19 ngày 06/10/2026.

| Model | Kết quả | Số chiều | Thời gian một input ngắn |
|---|---|---|---|
| `vai/VT-Embed-8B-v1` | 404 cả 3 lần | — | — |
| `vai/VT-Embed-4B-v1` | 404 cả 3 lần | — | — |
| `vai/VT-Embed-1B-v2` | Chạy | 2048 | 0,5–0,9 giây |
| **`Qwen3-Embedding-4B`** | **Chạy** | **2560** | 0,5–1,9 giây |

- Hai model VT-Embed 8B và 4B có tên trong `/v1/models` nhưng trả `default backend - 404`. Đây là lỗi của nginx ingress khi không có dịch vụ phía sau: gateway còn giữ tên model nhưng server chạy model đã tắt hoặc chưa triển khai. Lỗi ở phía nhà cung cấp, không phải do key hay request.
- Tên model phải viết **đúng** là `Qwen3-Embedding-4B`. Viết `Qwen/Qwen3-Embedding-4B` thì gateway báo `no healthy deployments`.
- `.env` đã đổi từ `EMBEDDING_MODEL=vai/VT-Embed-8B-v1` (hỏng) sang `EMBEDDING_MODEL=Qwen3-Embedding-4B`.

### 1.2. Lý do chọn

- **Đồng bộ họ model:** OCR dùng Qwen3-VL-32B, reranker tầng 2 dùng Qwen3-Reranker-4B. Embedding và reranker cùng họ Qwen3, được huấn luyện để dùng cặp với nhau.
- **Mạnh về đa ngôn ngữ,** gồm cả tiếng Việt.
- Là một trong hai model embedding đang chạy được. Ứng viên còn lại `VT-Embed-1B-v2` nhỏ hơn 4 lần.

**Giới hạn của quyết định:** chưa so sánh hai model trên dữ liệu thật. Nếu cần bằng chứng cho báo cáo thì làm thêm phép đo: khoảng 30 câu hỏi đã biết trang đáp án, khoảng 300 trang, so Recall@5 và MRR. Mỗi model mất chưa tới 1 phút.

### 1.3. Đặc tính đo được

| Đặc tính | Số đo | Hệ quả |
|---|---|---|
| Số chiều | 2560, vector đã chuẩn hoá (norm = 1,0000) | Collection Qdrant dùng `Cosine`. Với vector đã chuẩn hoá, cosine bằng tích vô hướng |
| Ký tự trên mỗi token | khoảng 1,94 (tiếng Việt có bảng số), đo trên 64 trang | 4.000 ký tự ≈ 2.060 token |
| Cửa sổ ngữ cảnh | 40.000 ký tự (20.214 token) vẫn chạy; 120.000 ký tự lỗi `ContextWindowExceeded` | Khoảng 32k token. Model không buộc phải cắt nhỏ |
| Bất đối xứng | Tài liệu embed nguyên văn. Câu hỏi thêm tiền tố `Instruct: <nhiệm vụ>\nQuery: <câu hỏi>` | Phía truy hồi phải thêm tiền tố, nếu không sẽ giảm độ chính xác |

**Kích thước batch** (input là các trang dài 2.000–2.600 ký tự):

| Batch | Tổng thời gian | Giây mỗi chunk |
|---|---|---|
| 1 | 0,67 | 0,674 |
| 8 | 1,40 | 0,175 |
| 16 | 2,62 | 0,164 |
| **32** | **3,15** | **0,098** |
| 64 | 15,14 | 0,237 |

→ **Batch 32.** Lên 64 thì chậm hẳn, có lẽ phía server phải xếp hàng. Ước tính 7.245 chunk, tức 226 request, chạy tuần tự **khoảng 12 phút** (OCR mất khoảng 4,6 giờ). Vì vậy không cần thread pool như OCR.

### 1.4. Quy tắc khi đổi model

Số chiều gắn chặt với collection. Đổi model thì phải tạo collection mới và embed lại toàn bộ. Vì thế:
- Tên model nằm trong `.env`, không viết cứng trong code.
- Tên collection ghi kèm tên model, ví dụ `finq_pages_qwen3e4b`.

## 2. Thuật toán chunk — CHỐT: recursive theo cấu trúc tài liệu, đơn vị gốc là trang

### 2.1. Các thuật toán đã cân nhắc

| Thuật toán | Đánh giá với kho này |
|---|---|
| Fixed-size | 65% trang có bảng. Cắt theo số ký tự là chắc chắn cắt giữa bảng |
| Semantic | So độ giống nhau giữa các câu liền kề, mà bảng số liệu không có câu. Vector của các dòng số gần như giống nhau nên ngưỡng không phân biệt được. Phải embed thêm từng câu, tức thêm hàng chục nghìn lượt gọi |
| LLM-based | 6.690 lượt gọi LLM, tương đương vài giờ như OCR, chỉ để quyết định chỗ cắt mà 94% trang vốn không cần cắt |
| Recursive thuần | Danh sách dấu phân cách `["\n\n", "\n", " ", ""]` không biết gì về bảng. Có thể cắt đôi bảng, nửa sau mất tiêu đề cột |
| Document structure thuần | Cắt theo heading cho chunk quá vụn (mục 2.3) |
| **Recursive + document structure** | **Chọn.** Giữ khung đệ quy, nhưng thay dấu phân cách bằng các đơn vị cấu trúc của markdown |

Cách ghép này tương tự `MarkdownHeaderTextSplitter` nối với `RecursiveCharacterTextSplitter` trong LangChain. Tên chung là *structure-aware recursive chunking*.

### 2.2. Thuật toán

Giới hạn `MAX = 4.000` ký tự (khoảng 2.060 token). Giới hạn này nhằm **truy hồi chính xác**: một chunk ôm nhiều chủ đề thì vector bị pha loãng. Nó không xuất phát từ giới hạn của model.

| Mức | Đơn vị cắt | Khi nào xuống mức này |
|---|---|---|
| 0 | Trang | Luôn bắt đầu ở đây. Trang ≤ MAX thì là một chunk (94,3% số trang) |
| 1 | Mục theo heading `#` | Trang dài hơn MAX |
| 2 | Khối: bảng (các dòng liên tiếp bắt đầu bằng `\|`) hoặc đoạn văn (cách nhau bởi dòng trống) | Mục vẫn dài hơn MAX |
| 3 | Dòng của bảng, **lặp lại dòng tiêu đề và dòng `\|---\|` ở đầu mỗi phần** / câu của đoạn văn | Một bảng hoặc một đoạn vẫn dài hơn MAX |
| 4 | Cắt cứng theo số ký tự | Phương án cuối (một dòng bảng dài hơn MAX), gần như không xảy ra |

```
split(text, level):
    if len(text) <= MAX: return [text]
    if level == HARD_CUT: return cutByChars(text)
    pieces = splitBy(UNIT[level], text)
    result = []
    for p in pieces: result += split(p, level + 1)
    return mergeGreedy(result, MAX)   // gộp các mảnh liền kề cho tới gần MAX
```

- **Gộp tham lam** sau mỗi lần cắt. Thiếu bước này thì recursive sinh ra rất nhiều chunk vụn.
- **Lặp lại tiêu đề bảng** là bắt buộc với báo cáo tài chính. Nếu không, phần sau của bảng chỉ còn con số mà không biết cột nào là "Số cuối kỳ", cột nào là "Số đầu năm".

### 2.3. Số đo làm căn cứ (sau khi làm sạch)

| Số đo | Giá trị |
|---|---|
| Kích thước trang | Trung vị 2.276 ký tự, p90 3.448, p99 8.364, lớn nhất 14.998 |
| Token ước tính | Trung vị khoảng 1.170, p99 khoảng 4.300, lớn nhất khoảng 7.700 |
| Trang dài hơn 4.000 | 379 (5,7%). Chỉ các trang này dùng tới đệ quy |
| Bảng đơn lẻ dài hơn 4.000 | 69 (phải xuống mức 3, cắt theo dòng bảng) |
| Đoạn văn đơn lẻ dài hơn 4.000 | 7 |
| Số chunk ước tính | 7.245 |
| Số heading mỗi trang | 0: 1.948 trang · 1: 294 · 2: 507 · 3: 958 · 4: 686 · 5 trở lên: **2.297** |

**Vì sao đơn vị gốc là trang mà không phải mục theo heading:**
- 2.297 trang có từ 5 heading trở lên. Cắt theo heading thì mỗi chunk trung bình chỉ khoảng 450 ký tự, quá ngắn, mất ngữ cảnh.
- Trích dẫn tính theo trang ("trang 12 của BCTC hợp nhất Q1/2019"). `documents` và `PageStore` cũng tổ chức theo trang.
- Trang là đơn vị tự nhiên của đầu ra OCR: mỗi trang một file, một lần gọi model.

### 2.4. Điểm yếu đã biết

Trang là ranh giới **vật lý**, không phải ranh giới **logic**.
- **411 trang mở đầu bằng một dòng bảng**, nhiều khả năng là bảng nối tiếp từ trang trước.
- Trong đó 28 trang thiếu hẳn dòng `|---|` ở dòng thứ hai. Ở các trang còn lại, model OCR tự dựng lại khung bảng, nhưng dòng nó coi là tiêu đề có khi chỉ là một dòng số liệu.

Cách giảm:
1. **Contextual header** gắn trước mỗi chunk khi embed (mục 4).
2. Phía truy hồi: lấy trúng trang N thì lấy kèm trang N−1 và N+1 làm ngữ cảnh cho LLM ("truy hồi bằng đơn vị nhỏ, trả về ngữ cảnh rộng").

## 3. Hạn mức API

Kiểm tra qua endpoint quản lý của LiteLLM (`GET /key/info`, `GET /user/info`, gửi kèm `Authorization: Bearer <key>`):

| Trường | Giá trị |
|---|---|
| `max_budget`, `soft_budget` | `null`: không giới hạn ngân sách |
| `tpm_limit`, `rpm_limit`, `max_parallel_requests` | `null`: không giới hạn token/phút, request/phút, số request song song |
| `expires` | `null`: key không hết hạn |
| `models` | `[]`: được gọi mọi model |
| `spend` | Chỉ là con số cộng dồn chi phí, không phải số dư |

- Key này **không có khái niệm "còn bao nhiêu token"**. Quản trị viên có thể đặt giới hạn bất cứ lúc nào.
- `/global/spend` và `/spend/logs` trả 403 vì key có vai trò `internal_user`, chỉ admin mới xem được.
- Mỗi response có header `x-litellm-response-cost` (chi phí của lần gọi đó) và `x-litellm-key-spend` (tổng đã dùng). **Nên ghi log `x-litellm-response-cost` sau mỗi batch** để có số liệu chi phí API cho chương đánh giá.
- Key gắn với tài khoản của người khác cấp: không commit `.env`, không đưa key vào log.

## 4. Đề xuất, CHƯA chốt

| Hạng mục | Đề xuất |
|---|---|
| Contextual header | Text đưa đi embed = `[<documents.ten_hien_thi> — trang N]\n<nội dung>`. Payload giữ nội dung gốc để trích dẫn |
| Point ID | `UUID.nameUUIDFromBytes("<tai_lieu_id>\|<trang>\|<phan>")`: cố định, upsert lại thì ghi đè, không trùng |
| Payload | `tai_lieu_id, trang, phan, nam, ky, pham_vi, loai, la_ban_chinh, text`. Payload index: `nam` (integer); `loai, ky, pham_vi, tai_lieu_id` (keyword) |
| Thư viện | `EmbeddingClient` tự viết bằng `HttpClient` (cùng khuôn với `VisionOcrClient`, không stream) và `QdrantClient` gRPC cổng 6334 (đã có sẵn qua `spring-ai-qdrant-store`). Không dùng `QdrantVectorStore.add()`, vì nó tự chia batch theo tokenizer cl100k (đếm sai tiếng Việt) và không cho tự đặt ID |
| Luỹ đẳng | Bỏ qua tài liệu có `ngay_embed` khác NULL. Mỗi tài liệu: xoá point cũ theo `tai_lieu_id` → upsert → ghi `ngay_embed` và `so_chunk` **sau cùng** |
| Hạ tầng | `docker-compose.yml` cho Qdrant (cổng 6333/6334, volume). Vector chỉ tốn khoảng 74 MB RAM (7.245 × 2560 × 4 byte) |
| Gộp chunk nhỏ | Có nên gộp các phần ngắn (dưới khoảng 500 ký tự) vào phần liền kề không: chưa quyết |
| Lớp | `IndexingProperties`, `Chunk` (record), `PageChunker`, `EmbeddingClient` / `HttpEmbeddingClient`, `QdrantIndexWriter`, `DocumentIndexingService` |
