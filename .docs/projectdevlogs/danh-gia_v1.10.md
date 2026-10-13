# Đánh giá — v1.10: chẩn đoán B07, M03 và sửa truy hồi

*Ngày: 2026-10-13 · Kế thừa [danh-gia_v1.9.md](danh-gia_v1.9.md) · QĐ-040 ([quyet-dinh_v1.23.md](quyet-dinh_v1.23.md))*

## 1. Sửa lại v1.9

- **B07 không phải lỗi đáp án vàng.** v1.9 ghi "có thể là lỗi đáp án vàng"; điều đó sai.
  - Tài liệu `CTR-CBTTPHATHANH-20260720-6e5e03` trang 1 ("Thông báo thay đổi số lượng cổ phiếu có quyền biểu quyết", số 22/TB-VCC, 20/07/2026) ghi vốn điều lệ sau thay đổi **1.281.095.330.000 đồng** (tăng 137.236.540.000 đồng, 13.723.654 cổ phiếu).
  - Trang đã được index trong Qdrant (2 trang, `nam=2026`).
- Bảng F của v1.9 giữ nguyên (đó là kết quả của lượt chạy 08:47–09:48). Kết quả sau khi sửa ghi ở mục 4.

## 2. B07: chuỗi chẩn đoán

### 2.1. Kết luận

**Nguyên nhân gốc:** agent tự viết lại câu tìm ("Phát hành cổ phiếu để trả cổ tức năm 2025, vốn điều lệ sau khi phát hành"), và câu này đẩy trang đúng ra ngoài 80 ứng viên của Qdrant. Câu người dùng gõ thì đưa trang này vào top 20.

| Câu đem đi tìm (80 ứng viên) | Cosine với trang vàng | Ngưỡng hạng 80 | Qdrant (sau lọc tiếng Anh) | Rerank tầng 1 | Top 20 |
|---|---|---|---|---|---|
| Câu agent viết trong benchmark | 0,6248 | 0,6345 | ngoài 80 | — | ✗ |
| Câu agent viết trong lượt thử (trial) | 0,6421 | 0,6207 | #42/55 | #4 | #5 ✓ |
| Câu người dùng gõ | 0,6459 | 0,6353 | #43/56 | #8 | #7 ✓ |
| RAG cơ bản (câu gốc, không lọc) | — | — | #59/80 | — | ✗ |

- Hai lượt chạy (09:39 và 11:39) cho cùng kết quả: 57 chunk sau lọc, không có trang vàng. Truy hồi **ổn định**, không dao động.
- Khi trang đã có trong ứng viên thì reranker xử lý tốt (#63 → #8 khi thử 200 ứng viên).
- Lượt chạy đầu (09:06) có thêm một lỗi khác:
  - Qdrant quá giờ vì máy thiếu RAM;
  - agent nhận lỗi rồi tự đổi câu tìm thành "Nghị quyết ĐHĐCĐ phương án… vốn điều lệ **dự kiến**", nên chỉ lấy được số theo phương án.

### 2.2. Các cách đã thử và bị loại

| Cách | Kết quả | Lý do loại |
|---|---|---|
| Nới ứng viên Qdrant 80 → 200 | Trang vàng vào top 20 (#15) | Làm chậm bước rerank; người làm đề tài chốt giữ 80 |
| Embed lại trang với `\n` thay `\r\n` | Cosine không đổi (0,6248) | Không có tác dụng |
| Embed lại trang chỉ phần tiếng Việt | Cosine không đổi | Phần tiếng Anh nằm lẫn trong ô bảng, `stripEnglish` không bỏ được |
| Thêm tiêu đề văn bản vào header khi embed | Cosine giảm còn 0,6078 | Tệ hơn |
| Nghi vector trong kho bị cũ, lệch | 12 chunk ngẫu nhiên: cosine(vector lưu, embed lại) = 1,0000 (một chunk 0,9982); trang B07 cũng 1,0000 | Kho không lệch |
| Nghi embed theo lô khác embed lẻ | Giống hệt nhau | Không phải nguyên nhân |

### 2.3. Hai kết luận sai trong lúc chẩn đoán (đã rút lại)

Cả hai đều do lỗi ở công cụ đo, không phải ở hệ thống:

1. **"Truy hồi dao động giữa các lần chạy, trang vàng vẫn có ở hạng 5".**
   - Sai vì tham số `-D...` truyền qua `mvnw` trên Windows bị đổi sang cp1252, làm mất dấu tiếng Việt ("Sau ??t phát hành c? phi?u…").
   - Probe đã embed câu hỏng ký tự. Phép "phát lại 3/3 đúng" cũng dùng câu hỏng nên không có giá trị.
   - **Cách tránh:** các test chẩn đoán đọc câu từ file UTF-8 bằng cú pháp `-D...=@<file>`.
2. **"Vector của trang B07 trong kho bị cũ".**
   - Sai vì Python ghi file trang ở chế độ văn bản, đổi `\r\n` thành `\r\r\n`, làm văn bản đem embed khác đi.
   - Đọc thẳng từ JSON của Qdrant thì vector khớp tuyệt đối.
   - **Cách tránh:** ghi file với `newline=''`, hoặc đọc thẳng từ payload.

## 3. Đã sửa (QĐ-040)

| Thay đổi | File | Ảnh hưởng độ trễ |
|---|---|---|
| Lần `search_documents` đầu của mỗi câu hỏi dùng câu người dùng gõ; câu agent đề xuất chỉ ghi vào trace; từ lần thứ hai dùng câu agent viết | `SearchDocuments`, `QueryState` (`question`, `searches`), `QueryLoop` | Không |
| Qdrant tìm chính xác (`SearchParams.exact`) | `QdrantRetriever` | Không (đo: 8–14 ms so với HNSW 9–31 ms) |
| Lỗi hoặc quá giờ khi tìm Qdrant thì thử lại một lần | `QdrantRetriever` | Chỉ khi lỗi |
| Log danh sách bằng chứng đưa sang bước trả lời (mã → tài liệu#trang) | `QueryLoop` | Không |

Giữ nguyên: 80 ứng viên → lọc tiếng Anh → rerank 40 → 20.

## 4. Chạy lại B07

`RagAnswerEvaluationTest -Dragevel.ids=B07 -Dragevel.configs=FINQUERY`, kết quả ở `data/eval/ket-qua/danh-gia-rag_*_b07-cau-goc.*`.

| Lượt | Câu đem đi tìm | Trang vàng trong bằng chứng | Kết quả |
|---|---|---|---|
| 09:39 (trước khi sửa) | câu agent viết | không | ✗ sai |
| 11:39 (chỉ bật `exact` và thử lại) | câu agent viết | không | ✗ sai |
| **12:06 (QĐ-040)** | **câu người dùng gõ** | **C7** | **✓ đúng**: 1.281.095.330.000 đồng, 13.723.654 cổ phiếu |

**Chưa làm:** chạy lại cấu hình FinQuery trên cả 29 câu tài liệu (khoảng 3,4 USD) để xác nhận QĐ-040 không làm hỏng câu khác.

## 5. M03: hai lỗ hổng

### 5.1. Agent đã gọi đúng công cụ, nhưng DB thiếu số

1. `get_financial_figures(LNST, 2 kỳ)` → chỉ ra 1 số (Q3/2025).
2. `relate_two_figures(Q3/2025, Q2/2024, CHANGE)` → không tính được.
3. `search_documents` → lấy được 125,05 tỷ từ BCTC quý 2/2024 (bằng chứng C#).
4. Không có công cụ nào tính giữa một số C# và một số D#, nên bước trả lời đúng luật là không tự trừ, và câu bị chấm "một phần".

### 5.2. DB chưa trích LNST từ báo cáo quý 1 và quý 2

`financial_figures`, chỉ tiêu `LOI_NHUAN_SAU_THUE`, hợp nhất, 2023–2025, kể cả dòng `da_thay_the`:

| Kỳ | Có trong DB? |
|---|---|
| Cả năm, quý 4 riêng, 6 tháng, 9 tháng | Có |
| Quý 3 riêng (2024, 2025) | Có |
| **Quý 1/2023, quý 1/2024** | **Không** |
| Quý 1/2025 | Chỉ có từ cột so sánh của BCTC quý 1/2026 |
| **Quý 2 riêng (mọi năm)** | **Không**; Java cũng không suy ra được vì thiếu quý 1 |

- BCTC hợp nhất quý 1/2024 (`CTR-BCTC-HN-20240331-Q1-32be76`) có 98 dòng trong DB nhưng không có LNST.
- Các BCTC quý 2 chỉ có 13–30 dòng (ví dụ `CTR-BCTC-HN-20240630-Q2-12a088`: 13 dòng).
- Đây là lỗi ở khâu trích số từ báo cáo kết quả kinh doanh quý 1 và quý 2, chưa sửa.

### 5.3. Hướng sửa đã chốt (chưa làm)

**Phép tính trên cả số C# và D#**, đặt ở **bước trả lời**:
- Agent chỉ thấy 350 ký tự đầu của mỗi trang nên không điền được số lấy từ tài liệu; bước trả lời thì đọc toàn văn.
- Model khai báo phép tính kèm mã bằng chứng và số nguyên văn.
- Java kiểm số có trong bằng chứng đó, quy đơn vị, tính bằng BigDecimal, rồi điền kết quả vào câu trả lời.

## 6. Công cụ chẩn đoán mới (test, bật bằng `-D`, không chạy mặc định)

| Test | Làm gì | Gọi LLM |
|---|---|---|
| `RetrievalProbeTest` | Hạng trang vàng ở Qdrant / tầng 1 / top cuối; lặp n lần | Không |
| `EmbedProbeTest#probe` | Cosine của trang theo các cách embed so với ngưỡng top-k | Không |
| `EmbedProbeTest#drift` | Vector đang lưu có khớp embed lại không | Không |
| `AnswerReplayTest` | Cố định bằng chứng, chạy lại bước trả lời n lần | Có |
| `FigureProbeTest` | Tra số DB theo chỉ tiêu@kỳ; liệt kê kỳ đang có; độ phủ BCTC | Không |

Mọi tham số chứa tiếng Việt truyền bằng `@<file UTF-8>`.

## 7. Bài học

- Kiểm công cụ đo trước khi kết luận: in lại đúng chuỗi đã đem embed và so với chuỗi gốc.
- Một trường hợp chạy đúng chưa chứng minh được gì; phải tái hiện được đúng lượt chạy sai.
- Đặt hành vi quan trọng trong code thay vì trong prompt: lần tìm đầu dùng câu gốc được cài trong `SearchDocuments`, không phụ thuộc LLM có nghe lời hay không.
