# Truy hồi (tiền truy xuất, rerank) và hạ tầng gọi model — v1.0

> Ngày: 09/10/2026. Phạm vi: phía **trả lời câu hỏi** của nhánh RAG, sau khi kho đã được lập chỉ mục (xem `lap-chi-muc_v1.8.md`).
> Luồng: câu hỏi → **tiền truy xuất** (chuẩn hoá, trích bộ lọc và từ khoá, diễn giải) → embed → Qdrant (khoảng 100 ứng viên) → **rerank tầng 1** (khoảng 20) → **rerank tầng 2** → LLM sinh câu trả lời kèm trích dẫn.
> Trạng thái: **thiết kế và đề xuất**, chưa cài đặt. Mục nào đã chốt thì ghi rõ.

## 1. Tiền truy xuất

### 1.1. Chốt: ba kỹ thuật, không dùng HyDE

Paper Hierarchical Reranker dùng bốn bước tiền truy xuất: chuẩn hoá, trích xuất từ khoá, diễn giải lại, sinh tài liệu giả định (HyDE). FinQuery **dùng ba bước đầu, không dùng HyDE**.

| Kỹ thuật | Cần gọi LLM? | Thời gian thêm | Rủi ro với báo cáo tài chính |
|---|---|---|---|
| Chuẩn hoá | Không: NFC, khoảng trắng, mở rộng viết tắt (LNST → lợi nhuận sau thuế) theo từ điển tài chính | Vài mili giây | Thấp |
| Trích xuất từ khoá | Phần lớn không: năm, quý, hợp nhất/riêng, loại tài liệu bắt bằng quy tắc; thuật ngữ tra theo từ điển | Vài mili giây | Thấp |
| Diễn giải lại | **Có**, một lượt sinh ngắn (2–3 câu hỏi khác cách nói) | Khoảng 1–2 giây | Thấp: chỉ đổi cách hỏi |
| HyDE | **Có**, một lượt sinh **dài** (đoạn trả lời giả định) | Khoảng 2–5 giây | **Cao**: đoạn giả định chứa **con số bịa**, có thể kéo truy hồi về trang có số gần giống thay vì trang đúng |

Lý do bỏ HyDE:
1. **Thời gian và chi phí** là 2 trong 3 tiêu chí cốt lõi của đề tài; HyDE sinh văn bản dài hơn diễn giải nhiều.
2. **Rủi ro con số bịa**, đặc thù với dữ liệu tài chính.
3. **Model embedding đã xử lý phần lớn việc HyDE làm.** HyDE sinh ra để thu hẹp khoảng cách giữa cách viết câu hỏi và cách viết tài liệu. Qwen3-Embedding là model bất đối xứng, được huấn luyện có tiền tố `Instruct:` riêng cho câu hỏi. Phép thử ngày 08/10/2026 cho thấy câu hỏi đã tìm đúng chunk với cosine 0,59–0,73 (`lap-chi-muc_v1.8.md`, mục 1.5.3).

**Để có bằng chứng cho báo cáo:** cài HyDE dưới dạng **cờ tắt sẵn** (`pre-retrieval.hyde.enabled=false`) và chạy nó như một thí nghiệm trong chương đánh giá (độ chính xác, độ trễ, chi phí khi có và không có HyDE).

### 1.2. Thiết kế đề xuất: tối đa một lượt LLM và một lượt embedding mỗi câu hỏi

```
câu hỏi
  ├─ Tầng 1: QUY TẮC (không gọi LLM, vài ms)
  │    chuẩn hoá NFC, khoảng trắng, mở rộng viết tắt theo từ điển tài chính
  │    trích bộ lọc: "quý 4/2023", "Q4 năm 2023", "quý IV 2023" → nam = 2023, ky = Q4
  │                  "hợp nhất" → pham_vi = hop_nhat;  "BCTN" → loai = bctn
  │    trích thuật ngữ tài chính theo từ điển
  ├─ Tầng 2: MỘT lượt LLM (model nhỏ, nhanh), trả JSON
  │    { "paraphrases": [2 câu khác cách nói], "keywords": [thuật ngữ quy tắc bỏ sót] }
  └─ MỘT lượt embedding: câu gốc + 2 câu diễn giải trong cùng một lô
       → 3 vector → 3 lượt tìm trên Qdrant (cùng bộ lọc) → gộp kết quả → rerank
```

- **Bộ lọc từ tầng quy tắc là phần giá trị nhất:** payload Qdrant đã có `nam`, `ky`, `pham_vi`, `loai`, `la_ban_chinh` (có index). Lọc chính xác trước khi so vector loại được phần lớn chunk sai kỳ mà gần như không tốn gì.
- Gộp diễn giải và trích từ khoá vào **một** lượt LLM; embed 3 câu hỏi trong **một** lô.
- **Chưa chốt:** model cho lượt LLM ở tầng 2 (ứng viên: `gpt-oss-120b`, `Qwen3-32B`; mục 3.3).

## 2. Rerank hai tầng

### 2.1. Kế hoạch theo paper

| Tầng | Vai trò | Model dự kiến |
|---|---|---|
| 1. Lọc nhẹ | Khoảng 100 ứng viên từ Qdrant → khoảng 20 | `BAAI/bge-reranker-v2-m3` (biến `RANKER_LAYER_1ST_MODEL` trong `.env`) |
| 2. Xếp hạng kỹ | Khoảng 20 → kết quả cuối | `Qwen/Qwen3-Reranker-4B` (biến `RANKER_LAYER_2ND_MODEL`), đọc được ngữ cảnh rất dài |

### 2.2. ViRanker — ứng viên cho rerank tầng 1

| Thông tin | Giá trị |
|---|---|
| Model | `namdp-ptit/ViRanker` trên Hugging Face |
| Loại | Cross-encoder rerank **chuyên tiếng Việt**: nhận cặp (câu hỏi, đoạn văn), trả điểm liên quan 0–1 |
| Nền | `BAAI/bge-m3`, **cùng họ** với `bge-reranker-v2-m3` |
| Kích thước | **0,6 tỷ tham số** (khoảng 1,2 GB ở FP16) |
| Giấy phép | Apache 2.0 |
| Độ dài tối đa | **512 token** |
| Code tuỳ chỉnh | Không cần (cross-encoder chuẩn, gọi được bằng `FlagReranker` hoặc `AutoModelForSequenceClassification`) |
| Kết quả công bố | Trên MMARCO-VI, vượt các reranker đa ngôn ngữ và ngang PhoRanker ở các vị trí đầu bảng xếp hạng (Dang, Nguyen, Pham, arXiv 2509.09131, 09/2025; có bản trong kỷ yếu Springer) |
| Gọi qua API | **Không có**: trang model ghi chưa nhà cung cấp nào phục vụ, phải tự chạy (mục 3.4) |

**Vấn đề: giới hạn 512 token.** Chunk của FinQuery dài hơn nhiều:

| | Ký tự | Token (khoảng 1,94 ký tự/token) |
|---|---|---|
| Chunk trung vị | 2.301 | khoảng 1.190 |
| Chunk p90 | 3.385 | khoảng 1.750 |
| Phần ViRanker đọc được | khoảng 1.000 | 512 |

Dùng nguyên trạng thì ViRanker chỉ đọc **chưa tới nửa đầu** chunk trung vị; con số ở cuối trang không bao giờ được chấm điểm. Đây đúng là rủi ro "reranker cắt đầu vào" đã ghi trong `lap-chi-muc` (mục 2.6.1, cận trên theo reranker).

**Cách xử lý đề xuất: chấm điểm theo cửa sổ, lấy điểm cao nhất (max-pooling)**
```
với mỗi chunk ứng viên:
    chia nội dung thành các cửa sổ khoảng 450 token, gối nhau khoảng 50 token
    điểm(chunk) = max( điểm_ViRanker(câu hỏi, cửa sổ) cho mọi cửa sổ )
```
Số cặp phải chấm tăng khoảng 2–4 lần, nhưng ViRanker nhẹ nên trên GPU vẫn nhanh. Cửa sổ nên cắt ở ranh giới dòng hoặc câu, và bảng thì lặp lại dòng tiêu đề, giống cách chunker xử lý.

**Lưu ý về miền dữ liệu:** ViRanker được huấn luyện trên MMARCO-VI (câu hỏi và đoạn văn đời thường, dịch sang tiếng Việt), **không phải** văn bản tài chính hay bảng số liệu. Chưa thể khẳng định nó tốt hơn `bge-reranker-v2-m3` trên dữ liệu FinQuery.

**Thí nghiệm đề xuất cho chương đánh giá:** so ViRanker và `bge-reranker-v2-m3` ở tầng 1, cùng cách chấm theo cửa sổ, trên bộ câu hỏi đánh giá. Đo nDCG@k, MRR, Recall@20 (trang đúng có lọt vào 20 ứng viên chuyển sang tầng 2 không) và thời gian rerank mỗi câu hỏi. Câu hỏi nghiên cứu: *reranker chuyên tiếng Việt có tốt hơn reranker đa ngôn ngữ trên báo cáo tài chính tiếng Việt không?*

## 3. Hạ tầng gọi model

### 3.1. Sự cố: key Viettel AI hết credit

Kiểm tra ngày 09/10/2026:

| Kiểm tra | Kết quả |
|---|---|
| `GET /key/info` | HTTP 200: key còn tồn tại, không bị khoá, không có hạn dùng riêng |
| Gọi embedding và chat | **HTTP 400**, `budget_exceeded`: credit của **tài khoản cấp key** hết hạn lúc **2026-10-08 13:41:14 UTC** (20:41 giờ Việt Nam) |

Mọi lượt gọi model (OCR, embedding, LLM) đều bị chặn. `EmbeddingClient` xử lý đúng: lỗi 400 thuộc nhóm không thử lại, nên dừng ngay. Việc cần làm: nhờ người cấp key gia hạn credit hoặc xin key riêng. Cùng gateway thì chỉ thay `LLM_API_KEY` trong `.env`, không đổi model, không sửa code.

### 3.2. Nếu đổi nhà cung cấp

| Model | Phải đổi | Ảnh hưởng dữ liệu đã có |
|---|---|---|
| Embedding | `EMBEDDING_MODEL`, `EMBEDDING_API_URL` (và tách `EMBEDDING_API_KEY`) | **Embed lại cả kho**: vector của hai nơi khác nhau không chắc khớp, kể cả cùng tên model (độ chính xác số khi phục vụ có thể khác). Chi phí nhỏ: vài phút, khoảng 0,15 USD |
| OCR | `OCR_MODEL`, `LLM_API_URL` | Không: 6.690 trang `.md` giữ nguyên, chỉ tài liệu mới dùng model mới |
| LLM | Tên model trong cấu hình | Không ảnh hưởng dữ liệu; có thể phải chỉnh prompt |

Tên model và địa chỉ API đều nằm trong `.env` và `application.yml`, nên đổi nhà cung cấp không phải sửa logic Java. Nếu nhà cung cấp mới không hỗ trợ `encoding_format: "base64"` thì cần thêm tuỳ chọn cấu hình cho `EmbeddingClient` đọc cả dạng mảng số.

### 3.3. Gọi model qua API theo lượt (tra ngày 09/10/2026)

| Model | Nơi cung cấp | Giá (USD / 1 triệu token) | Ghi chú |
|---|---|---|---|
| `Qwen3-Embedding-4B` | DeepInfra (gọi thẳng) | 0,02 | Có endpoint tương thích OpenAI `/v1/openai/embeddings`; tài liệu không nói về base64 |
| `openai/gpt-oss-120b` | Router Hugging Face, rẻ nhất qua DeepInfra | 0,037 vào / 0,17 ra | Endpoint tương thích OpenAI `https://router.huggingface.co/v1/chat/completions` |
| `Qwen/Qwen3-32B` | Router Hugging Face (Nscale, DeepInfra) | 0,08 vào / 0,25–0,28 ra | |
| `Qwen/Qwen3-VL-30B-A3B-Instruct` | Router Hugging Face (DeepInfra) | 0,15 vào / 0,6 ra | Thay thế gần nhất cho OCR; **router không có Qwen3-VL-32B** |
| `Qwen3-VL-32B-Instruct` | OpenRouter | 0,104 vào / 0,416 ra | Có thông báo **ngừng phục vụ ngày 09/10/2026** |

- Router Hugging Face: endpoint tương thích OpenAI **chỉ dành cho chat**; embedding phải dùng định dạng khác (`{"inputs": [...]}`, trả mảng số, không có base64). Vì vậy embedding nên gọi thẳng DeepInfra.
- Tài khoản Hugging Face miễn phí **không có credit hằng tháng**, phải mua credit; gói PRO được 2 USD credit mỗi tháng. Hugging Face không cộng thêm phí vào giá nhà cung cấp.
- Các nhà cung cấp này đặt máy chủ ở nước ngoài và cần thẻ thanh toán quốc tế.

### 3.4. Thuê GPU tự chạy model

Tự tải model từ Hugging Face về máy thuê, dựng server tương thích OpenAI bằng **vLLM** (LLM, model đọc ảnh, embedding) hoặc **Text Embeddings Inference** (embedding, và endpoint `/rerank` cho cross-encoder như ViRanker). Code Java gọi vào địa chỉ của máy thuê. Đúng quy ước dự án: Python chỉ cho công cụ offline hoặc phục vụ model không chứa logic.

| Nơi thuê | GPU | Giá mỗi giờ | Ghi chú |
|---|---|---|---|
| **ezycloudx.com** (TP.HCM) | RTX 4060 Ti/5060 Ti 16 GB | 6.000 đ | Máy trong nước, tính tiền bằng VND theo giờ; không có sẵn template model |
| | RTX 3090 24 GB | 9.000 đ | |
| | RTX 5090 32 GB | 19.000 đ | |
| **Vast.ai** (chợ cho thuê, giá tra qua API tìm kiếm ngày 09/10/2026, mẫu tối đa 64 máy mỗi loại) | RTX 4060 Ti 16 GB | rẻ nhất 0,107 USD, trung vị 0,150 USD | Tính tiền theo giây; có template vLLM, Ollama, Jupyter; nhiều máy thuộc chủ máy cá nhân (một phần đã xác minh, ít máy ở trung tâm dữ liệu) |
| | RTX 3090 24 GB | rẻ nhất 0,122 USD, trung vị 0,202 USD | |
| | RTX 4090 24 GB | rẻ nhất 0,366 USD, trung vị 0,473 USD | |
| | RTX 5090 32 GB | rẻ nhất 0,282 USD, trung vị 0,654 USD | |

**Model nào chạy được trên GPU nào (ước tính):**

| Model | Bộ nhớ cần | GPU phù hợp |
|---|---|---|
| Qwen3-Embedding-4B | khoảng 8 GB | 16 GB là đủ |
| ViRanker (0,6 tỷ) | khoảng 1,2 GB | Chạy chung máy với embedding trên GPU 16 GB |
| bge-reranker-v2-m3 + Qwen3-Reranker-4B | khoảng 2 GB + 8 GB | Cùng embedding: nên dùng GPU 24 GB |
| Qwen3-VL-32B-FP8 (model OCR hiện tại) | khoảng 33 GB riêng trọng số | Không vừa các GPU trên; RTX 3090 không hỗ trợ FP8. Thay bằng bản nén 4-bit (khoảng 20 GB) hoặc Qwen3-VL-8B |
| LLM tầng 2 tiền truy xuất (Qwen3-8B / Qwen3-32B nén 4-bit) | khoảng 6 GB / khoảng 20 GB | 16 GB / 24 GB |

**So sánh hai nơi thuê:**

| | ezycloudx | Vast.ai |
|---|---|---|
| Giá | Cao hơn (RTX 3090: 9.000 đ/giờ) | Rẻ hơn (RTX 3090 trung vị khoảng 0,20 USD/giờ) |
| Dữ liệu | **Trong nước** | Máy của bên thứ ba, phần lớn ở nước ngoài, nhiều máy là của cá nhân |
| Thanh toán | VND | USD, nạp trước bằng thẻ quốc tế |
| Độ trễ mạng từ Việt Nam | Thấp | Cao hơn, tuỳ vị trí máy; không đáng kể với việc chạy theo lô, nhưng cộng vào thời gian trả lời nếu dùng lúc chạy thật |
| Ổn định | Một nhà cung cấp | Tuỳ chủ máy; nên chọn máy đã xác minh hoặc ở trung tâm dữ liệu |

**Lưu ý khi thuê GPU:** đặt API key cho server (ví dụ tham số `--api-key` của vLLM) và không mở cổng công khai; nhớ tắt máy khi không dùng vì tiền tính theo thời gian; nếu ổ đĩa không được giữ lại sau khi tắt thì mỗi lần bật phải tải lại model (khoảng 8 GB cho mỗi model 4 tỷ tham số).

### 3.5. Đề xuất

| Việc | Dùng | Lý do |
|---|---|---|
| Embedding, LLM diễn giải và sinh câu trả lời | API theo lượt (gia hạn key Viettel AI; nếu chưa được thì DeepInfra) | Rẻ, không phải quản lý máy |
| Rerank hai tầng (gồm thí nghiệm ViRanker) và các đợt chạy đánh giá | **Thuê GPU theo đợt ngắn**: bật, chạy, tắt | Chạy đúng các reranker mà API giá rẻ không có; GPU 24 GB đủ cho embedding và hai reranker |
| Khi dùng thật với dữ liệu nội bộ | Ưu tiên gateway hoặc máy trong nước | Hệ thống hướng tới người dùng kế toán nội bộ |

## 4. Tài liệu tham khảo

- Dang, P.-N., Nguyen, K.-L., Pham, T.-H. (2025). *ViRanker: A BGE-M3 & Blockwise Parallel Transformer Cross-Encoder for Vietnamese Reranking.* arXiv:2509.09131. https://arxiv.org/abs/2509.09131 — bản trong kỷ yếu Springer: https://link.springer.com/chapter/10.1007/978-3-032-18316-3_34
- Model ViRanker: https://huggingface.co/namdp-ptit/ViRanker
- Hugging Face Inference Providers: https://huggingface.co/docs/inference-providers/index — giá và thanh toán: https://huggingface.co/docs/inference-providers/pricing — embedding: https://huggingface.co/docs/inference-providers/tasks/feature-extraction
- DeepInfra, Qwen3-Embedding-4B: https://deepinfra.com/Qwen/Qwen3-Embedding-4B/api
- OpenRouter, Qwen3 VL 32B Instruct: https://openrouter.ai/qwen/qwen3-vl-32b-instruct/api
- ezycloudx: https://ezycloudx.com — Vast.ai: https://vast.ai/pricing, https://docs.vast.ai/
