# Truy hồi và sinh câu trả lời — v1.8

*Ngày: 2026-10-12 tối · Kế thừa [truy-hoi_v1.7.md](truy-hoi_v1.7.md) · Prompt: [prompt_v1.1.md](prompt_v1.1.md), [prompt_v1.2.md](prompt_v1.2.md)*

Bản này gồm ba việc:
1. Lọc tiếng Anh trong tài liệu song ngữ.
2. Đo lại chất lượng truy hồi.
3. **Nghiên cứu độ trễ** của cả luồng hỏi đáp bằng số đo, không sửa mò.

## 1. Bộ lọc tiếng Anh (`common/text/EnglishFilter.java`)

**Vấn đề.**
- Quét kho ngày 12/10/2026: 1.072/6.470 trang thuần tiếng Anh (bản dịch của trang tiếng Việt trước đó) và 45 trang song ngữ.
- Trang thuần tiếng Anh chiếm chỗ trong 80 ứng viên mà không thêm thông tin.
- Trang song ngữ xen đoạn tiếng Anh giữa các đoạn tiếng Việt. Model ghép hai câu tiếng Việt qua đoạn tiếng Anh, nên câu trích bị đánh ✗ (ca V02, xem prompt_v1.1).

**Cách làm: lọc một lần, dùng ở hai nơi.**
- Lấy 80 chunk từ Qdrant như cũ.
- Bỏ chunk thuần tiếng Anh. Không xin thêm chunk để bù: bỏ xong còn bao nhiêu thì đưa vào rerank bấy nhiêu, rồi 40 → 20. Quyết định của người làm đề tài: tài liệu song ngữ chỉ là một phần kho, không phải tài liệu nào cũng có.
- Mỗi chunk còn lại được bỏ đoạn tiếng Anh **một lần**, kết quả lưu ở `RetrievedChunk.noiDungSach`. Reranker và LLM đọc bản sạch, và câu trích được kiểm trên bản sạch. Bản gốc `noiDung` giữ nguyên để hiển thị trang.

**Luật nhận diện** (không gọi model):

| Mức | Điều kiện nhận là tiếng Anh |
|---|---|
| Chunk | ≥ 200 chữ cái, tỷ lệ chữ mang dấu tiếng Việt < 2%, ≥ 10 từ chức năng tiếng Anh |
| Đoạn | ≥ 30 chữ cái, tỷ lệ chữ mang dấu < 2%, ≥ 2 từ chức năng tiếng Anh |

- Dòng bảng (bắt đầu bằng `|`) luôn giữ.
- Đoạn ngắn (tiêu đề, số, ngày) luôn giữ.
- Bật/tắt bằng `retrieval.drop-english` (mặc định bật). RAG cơ bản (đối chứng) không lọc.
- Log mỗi lần truy hồi, ví dụ: `lọc tiếng Anh: Qdrant 120 chunk, bỏ 30 chunk tiếng Anh, làm sạch 16 chunk song ngữ`. Lần chạy này thuộc bản đầu, còn xin thêm 40 chunk; xem mục 2.

**Kiểm câu trích (`CitationVerifier`).** Thêm hai điều chỉnh, kỷ luật bằng chứng không nới:
1. Câu trích không khớp liền mạch thì tách theo câu. **Mọi** đoạn từ 15 ký tự trở lên phải có nguyên văn trong nguồn; một đoạn bịa là trượt cả câu trích.
2. Bỏ qua ký hiệu định dạng markdown: gạch đầu dòng đứng riêng và dấu `:`. Ca thật lúc 20:24, câu cổ tức 2024: nguồn là `### Ngày thanh toán` + `- 15/10/2025.`, model chép thành `- Ngày thanh toán: - 15/10/2025.`. Chữ và số vẫn phải khớp từng ký tự.

Test:
- `EnglishFilterTest`: 5 ca.
- `CitationVerifierTest`: 5 ca, gồm ca đoạn bịa phải trượt và ca số sai một chữ số phải trượt.

## 2. Chất lượng truy hồi sau khi lọc

So hai lượt benchmark:
- **Trước:** lượt 3, `benchmark-truy-hoi_20261012-0800.csv`.
- **Sau:** có lọc, `benchmark-truy-hoi_20261012-2002.csv`.

Lượt "sau" chạy với bản đầu còn xin Qdrant 120 chunk; chưa chạy lại với bản 80 chunk.

So ghép cặp 50 câu, bootstrap 10.000 lần:

| Chỉ số | Trước | Sau | Δ | KTC 95% | Kết luận |
|---|---|---|---|---|---|
| Recall@20 | 95,6% | 95,6% | 0 | — | giữ nguyên |
| nDCG@20 | 0,483 | 0,526 | +0,038 | [−0,008; 0,084] | xu hướng tốt hơn, chưa chắc |
| nDCG@20 theo bằng chứng | 0,557 | 0,622 | **+0,059** | [0,005; 0,112] | **tốt hơn, có ý nghĩa** |
| MRR | 0,623 | 0,719 | **+0,086** | [0,008; 0,165] | **tốt hơn, có ý nghĩa** |
| Đủ bằng chứng (câu nhiều trang) | 6/12 | 7/12 | +1 | — | |

- nDCG@20 tốt hơn ở 24 câu và kém hơn ở 8 câu.
- Năm câu giảm quá 0,05: V02, V03, B01, D05, C06. Cả năm vẫn có trang vàng trong top 20. **Cần xem kỹ:** có thể trang vàng bị thay bằng bản tiếng Việt khác, hoặc bản tiếng Anh của trang vàng vốn là nhãn phụ.

**Độ trễ: rerank p50 tăng từ 6,9 s lên 13,9 s.** Chi phí rerank gần như không đổi (0,0026 → 0,0025 USD/câu), tức lượng token gửi đi tương đương. Nghi phía DeepInfra hoặc mạng chậm lúc đo (log có lần embedding mất 4,6 s, bình thường 0,8 s). **Chưa kết luận.** Đã viết bài đo xen kẽ bật/tắt lọc trên cùng câu, ghi cả token mỗi tầng (`LatencyProbeTest -Dlatency.ab=true`); **chưa chạy**.

## 3. Nghiên cứu độ trễ của luồng hỏi đáp

### 3.1 Agent tìm thừa (đã sửa, prompt v1.2)

- **Hiện tượng:** V02 chạy 2 lần với prompt v1.1, cả hai lần agent gọi `search_documents` **3 lần**. Lượt 2 viết lại câu hỏi. Lượt 3 tìm đích danh "Công văn giải trình… trang 2-3", tức tài liệu **đã có** ở lượt 1.
- **Nguyên nhân:** tool chỉ cho agent xem 350 ký tự đầu trang, mà đó thường là tiêu đề công văn. Agent tưởng chưa có bằng chứng.
- **Sửa:** prompt v1.2 và câu trả về của tool nói rõ bước trả lời đọc toàn văn; một lần tìm thường là đủ; chỉ tìm lại trong hai trường hợp cụ thể.
- **Đo lại trên 3 câu** (V02, cổ tức 2024, kế hoạch LNST năm nay): **cả 3 câu chỉ tìm 1 lần**, đều `answered`, không có số lệch nguồn.

### 3.2 Bước trả lời: đo tách từng phần (`LlmLatencyProbeTest`)

**Cách đo:**
- Gọi thẳng API DeepInfra ở chế độ stream, cùng một đầu vào cố định cho mọi biến thể: V02, 20 chunk, khoảng 24.000 token. Temperature 0, mỗi biến thể chạy 2 lần.
- Kết quả: `data/eval/ket-qua/do-tre-llm_20261012-2029.md`. Đầu ra thô (thinking + JSON) ở thư mục `_tho`.

| Biến thể | Lượt | Chờ token đầu | Thinking | Viết | **Tổng** | Token ra | USD |
|---|---|---|---|---|---|---|---|
| Qwen3.5-397B, bật thinking | 1 | 15,1 s | 115,2 s | 32,2 s | **162,5 s** | 3.269 | 0,021 |
| Qwen3.5-397B, bật thinking | 2 | 2,2 s | 22,0 s | 46,2 s | **70,3 s** | 1.450 | 0,015 |
| Qwen3.5-397B, tắt thinking | 1 | 2,9 s | — | 37,8 s | **40,7 s** | 613 | 0,013 |
| Qwen3.5-397B, tắt thinking | 2 | 1,6 s | — | 33,0 s | **34,6 s** | 613 | 0,013 |
| Sonnet 5.5 | 1 | — | — | — | **16,1 s** | 2.435 | 0,072 |
| Sonnet 5.5 | 2 | — | — | — | **13,5 s** | 2.164 | 0,067 |

Sonnet 5.5 trả cả cụm sau khoảng 7–9 s nên không tách được chờ và viết; chỉ tổng thời gian là đáng tin.

**Kết luận từ số đo:**
1. **Nút thắt chính là tốc độ sinh chữ của Qwen trên DeepInfra, chỉ 16–22 token/s.** Bước trả lời cần 600–3.300 token ra, nên mất 35–160 s.
2. **Thinking của Qwen dao động mạnh** dù temperature 0: 1.450 hoặc 3.269 token, 70 hoặc 162 s cùng một đầu vào. Không thể hứa thời gian trả lời ổn định.
3. **Tắt thinking thì nhanh và ổn định** (35–41 s, đúng 613 token cả hai lần), **nhưng chất lượng kém hơn.** Câu trả lời tự thêm đánh giá không có trong nguồn: "chủ yếu do", "tăng mạnh", "tăng đột biến". Như vậy là vi phạm quy tắc không suy đoán. **Không chọn** cách này.
4. **Sonnet 5.5 nhanh gấp 5–10 lần (13–16 s) và trả lời tốt nhất:**
   - nói thẳng công văn chỉ giải thích bằng biến động khoản mục, không nêu nguyên nhân kinh doanh;
   - tách mỗi câu trích thành một câu liền mạch, đúng quy tắc v1.1;
   - thêm số chính xác đến đồng từ báo cáo hợp nhất.

   **Nhưng đắt gấp khoảng 5 lần ở bước trả lời** (0,07 so với 0,013–0,021 USD).
5. Spring AI đọc thinking ra 0 ký tự, trong khi stream thô có 2.430–7.304 ký tự nháp. Spring AI không đọc trường thinking của DeepInfra; chỉ ảnh hưởng log, không ảnh hưởng kết quả.

### 3.3 Vòng agent

- Mỗi lần gọi agent chỉ ra 185–515 token mà mất 6,7–27,3 s, cùng nguyên nhân (Qwen trên DeepInfra chậm, cộng thinking).
- Sau khi sửa 3.1, mỗi câu còn **2 lần gọi**:
  - lần 1 chọn tool;
  - lần 2 chỉ để nói "đủ rồi".

  Lần 2 chiếm 14–27 s. Đây là ứng viên tối ưu tiếp, chưa làm.

### 3.4 Tổng hợp ba câu, prompt v1.2, Qwen

| Câu | Agent | Tool | Trả lời | Tổng | USD |
|---|---|---|---|---|---|
| V02 nguyên nhân LN Q3/2025 | 37,7 s | 20,2 s | 75,6 s | 133,7 s | 0,024 |
| Cổ tức 2024 | 34,0 s | 11,9 s | 121,0 s | 167,0 s | 0,017 |
| Kế hoạch LNST năm nay | 24,6 s | 14,4 s | 38,8 s | 77,8 s | 0,016 |

Hai phần LLM (agent và trả lời) chiếm 70–90% thời gian. Truy hồi chỉ 12–20 s.

## 4. Việc tiếp theo

- **Chọn model cho bước trả lời và agent (người làm đề tài quyết):** giữ Qwen để rẻ, hay dùng Sonnet 5.5 để nhanh và đúng hơn. QĐ-031 đã định Sonnet cho demo và benchmark chính thức.
- Chạy A/B xen kẽ bộ lọc tiếng Anh để kết luận phần độ trễ rerank.
- Xem 5 câu nDCG giảm.
- Cân nhắc bỏ lần gọi agent thứ 2 khi chỉ để nói "đủ rồi", cần đo trước.
