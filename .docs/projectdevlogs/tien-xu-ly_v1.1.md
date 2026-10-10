# Tiền xử lý — v1.1

*Ngày: 2026-10-10 · Bản trước: v1.0 (2026-10-03) · Nối tiếp [luong-hoi-dap_v1.0.md](luong-hoi-dap_v1.0.md) · Chi tiết chunk, embed, Qdrant: [lap-chi-muc_v1.11.md](lap-chi-muc_v1.11.md)*

## Thay đổi so với bản trước (v1.0)

- **Thêm mục 11 — Luồng tiền xử lý hoàn chỉnh**, kèm **sơ đồ** (`images/tien-xu-ly-luong_v1.1.png`, vẽ bằng HTML): hai lối vào (tải lên, nạp lại), luồng HTTP và luồng nền, các bước OCR → làm sạch → chunk → embed → Qdrant, cờ trạng thái và bất biến, module `document`, mã công ty (`V10`), kiểm tra đầu vào.
- **Thêm mục 12 — Sự cố OCR khi đổi sang DeepInfra** (`model returned almost nothing`): nguyên nhân ở định dạng stream SSE, cách sửa, test.
- Mục 10 cập nhật trạng thái.

Ghi lại **khâu tiền xử lý**: OCR hoạt động ra sao, các tham số kết xuất ảnh và lý do
chọn, luật làm sạch, số liệu cho khâu chunk. Mọi con số đều **đo thật**, nêu rõ đo
trên tệp nào.

---

## 1. Tiền xử lý KHÁC tiền truy xuất

Hai khâu khác nhau cả về thời điểm chạy lẫn đối tượng xử lý — rất dễ nhầm vì tên
gần giống.

```
OFFLINE — chạy MỘT LẦN cho mỗi tài liệu, trước khi có ai hỏi
  tài liệu → OCR → làm sạch → trích số / chunk → embed → Qdrant
             └──────────── TIỀN XỬ LÝ (preprocessing) ────────────┘
                            xử lý NGỮ LIỆU

ONLINE — chạy MỖI câu hỏi
  câu hỏi → chuẩn hoá, trích bộ lọc → tìm vector → rerank → sinh trả lời
            └ TIỀN TRUY XUẤT ┘        └ truy xuất ┘        └ sinh ┘
              (pre-retrieval)
              xử lý CÂU HỎI
```

Tiền truy xuất trích ra bộ lọc `nam`, `pham_vi`, `loai_tai_lieu` từ câu hỏi — đó là
**đòn bẩy chất lượng lớn nhất** của nhánh RAG, lọc bỏ ~95% kho trước khi tìm vector.

---

## 2. OCR ở đây không phải OCR cổ điển

```
OCR cổ điển (Tesseract)     nhận dạng TỪNG KÝ TỰ → trả về chuỗi chữ
                            bảng biến thành một dòng chữ lộn xộn

Mô hình thị giác (Qwen3-VL) NHÌN cả trang như mắt người → trả về Markdown
                            giữ được cột, giữ được cấu trúc bảng
```

**65% số trang trong kho có bảng** (4.321/6.690). Đó là lý do bắt buộc dùng mô hình
thị giác — và cũng là lý do phải **kết xuất trang thành ảnh** thay vì trích chữ thuần
từ PDF: trích chữ làm mất bố cục cột.

Dây chuyền:

```
trang PDF ──kết xuất──► ảnh JPEG xám ──gửi API──► Markdown ──► trang_NNN.md
```

---

## 3. Tham số kết xuất ảnh — đo trên trang 6 của `BCTC-HN-2025_full`

Trang khổ **8,2 × 11,8 inch** (593×850 điểm, 1 điểm = 1/72 inch).

### DPI quyết định số điểm ảnh, số điểm ảnh quyết định dung lượng

```
DPI   kích thước px   JPEG màu   JPEG xám   PNG xám
110   907 × 1300       231 KB     223 KB     404 KB
130   1072 × 1536      293 KB     283 KB     534 KB
170   1401 × 2009      444 KB     428 KB     832 KB   ← đang dùng
200   1649 × 2363      558 KB     536 KB    1085 KB
300   2473 × 3544      902 KB     861 KB    2004 KB
```

Công thức: `số điểm ảnh ngang = chiều rộng inch × DPI`. Tăng DPI gấp đôi thì số điểm
ảnh gấp **bốn** (hai chiều), dung lượng tăng gần tương ứng.

**Chọn 170 DPI** vì đó là điểm cân bằng: chữ số trong bảng còn đọc rõ, mà ảnh ~430 KB
vẫn qua được đường truyền yếu. 300 DPI cho ảnh 861 KB — gấp đôi, trong khi mạng hữu
dụng đo được chỉ ~0,12 Mbps.

### Định dạng và chất lượng

```
PNG màu       1691 KB   ← mặc định nếu không nghĩ gì
PNG xám        832 KB
JPEG màu q85   444 KB
JPEG xám q85   428 KB   ← đang dùng

PNG màu / JPEG xám = 4,0 lần
```

**ĐÍNH CHÍNH một hiểu nhầm đã lưu hành trong dự án**: con số "nhẹ hơn 4 lần" **không**
đến từ việc chuyển sang ảnh xám. Phân tách ra:

```
PNG màu  → JPEG màu    1691 → 444 KB    3,8 lần   ← NÉN JPEG làm gần hết việc
JPEG màu → JPEG xám     444 → 428 KB    1,04 lần  ← xám chỉ thêm 4%
```

Ảnh xám vẫn nên giữ, nhưng vì **hai lý do khác**, không phải vì dung lượng:
1. `ImageType.GRAY` cho ra `TYPE_BYTE_GRAY`, **không có kênh alpha** — bộ ghi JPEG của
   Java ném ngoại lệ nếu ảnh có alpha
2. Báo cáo tài chính vốn đen trắng, màu không mang thông tin

Chất lượng JPEG, ảnh xám 170 DPI:

```
q60   263 KB      q75   331 KB      q85   428 KB   ← đang dùng      q95   724 KB
```

Chọn **q85**: dưới mức này chữ số bắt đầu nhoè ở vùng nét mảnh; trên mức này dung
lượng tăng nhanh mà mắt không phân biệt được.

### Khoảng dung lượng nên thấy

```
150–400 KB   bình thường cho A4 ở 170 DPI xám
> 1 MB       DPI hoặc chất lượng quá cao → mạng nghẹn
< 50 KB      quá thấp → mô hình đọc nhầm chữ số
```

---

## 4. Thang DPI khi thử lại

```
lần 1–2   170 DPI    (mặc định)
lần 3–4   130 DPI
lần 5–6   110 DPI
sau 6 lần thất bại → ghi tệp .md.loi, KHÔNG bỏ qua âm thầm
```

Lý do hạ DPI chứ không phải đổi mô hình: **đa số lỗi là mạng, không phải mô hình**.
Ảnh 110 DPI chỉ nặng 223 KB, bằng **52%** của 170 DPI — dễ qua hơn hẳn khi đường
truyền chập chờn.

---

## 5. Hai hạn giờ — bài học đắt nhất của khâu này

```
SAI   một hạn 120s cho CẢ TRANG   →  giết ~30% số trang
      vì phân vị 75 cần tới 126s. Trang dài KHÔNG phải trang treo.

ĐÚNG  mảnh chữ đầu tiên  ≤ 120s   (tải ảnh lên + mô hình đọc ảnh)
      giữa hai mảnh      ≤  30s   (đo thật: khoảng lặng dài nhất chỉ 4,4s)
      chặn trên tuyệt đối  900s
```

Chữ còn chảy thì trang dài bao nhiêu cũng chạy tiếp. Đây là lý do **bắt buộc phát
luồng** (`stream: true`) — không có luồng thì không phân biệt được "đang chạy" với
"đã chết".

Định dạng luồng thật của cổng Viettel (đo ngày 29/9):

```
data: {"choices":[{"delta":{"content":"X","role":"assistant"}}]}
                                          ← dòng RỖNG ngăn cách
data: {"choices":[{"delta":{"content":"in lỗi, tôi không..."}}]}

data: {"choices":[{"finish_reason":"length","delta":{}}]}   ← delta RỖNG, không có content

data: [DONE]
```

Bốn tình huống bộ phân tích phải chịu được: tiền tố `data:` (có hoặc không dấu cách),
dòng rỗng, `delta` không có `content`, và dấu kết `[DONE]`.

**Mẹo kỹ thuật**: `InputStream.read()` chặn luồng và Java **không có** hạn giờ cho nó.
Cách duy nhất bẻ gãy là **đóng stream từ luồng khác** — đó là việc của luồng canh.

---

## 6. Luật làm sạch — làm ÍT thôi

Làm sạch quá tay là **phá dữ liệu**. Biến thể OCR như `thuê`/`thuế`, `hoãn`/`hoàn`
trông như lỗi chính tả nhưng không biết chắc cái nào đúng — sửa là đoán.

**Bốn luật không mất thông tin:**

```
1. chuẩn hoá Unicode NFC
2. "&nbsp;"  → dấu cách, rồi gộp khoảng trắng
3. "<br>"    → dấu cách   (OCR gộp dòng tiêu đề trong ô bảng)
4. gộp dòng trống liên tiếp
```

**KHÔNG** sửa chính tả, **KHÔNG** sửa số. Lỗi mã số để `extraction_exceptions` bắt —
nơi có phép cộng để kiểm chứng chứ không phải đoán.

### Đo mức độ của `&nbsp;`

```
Trang có &nbsp;  :  63 / 6.690  (1%)
Tổng số lần      :  3.903
Thực thể khác    :  &KSNB; — 1 lần, chỉ là chữ bị kẹp nhầm dấu &;
```

Phân bố rất lệch:

```
62 trang   16–32 lần, chiếm 2,6–14% ký tự      → không đáng lo
 1 trang   2.909 lần, chiếm 97,3% ký tự        → HỎNG
```

`BCR-nam-2019/trang_002.md`: **17.940 ký tự, bỏ `&nbsp;` đi còn 491 ký tự.** Mô hình
bị kẹt vòng lặp, nhả ra gần ba nghìn thực thể liên tiếp; nội dung thật chỉ là bảng
mục lục 10 dòng.

Đây cũng là **trang dài nhất trong cả kho** — nên con số "trang dài nhất 17.939 ký tự"
dùng để tính chunk là **sai lệch**: nó không phải trang dày nội dung mà là trang rỗng
bị phồng.

**Phát sinh**: cần thêm phép **phát hiện lặp bất thường** (một chuỗi con lặp > N lần
liên tiếp). Nhưng chỉ nên **ghi cảnh báo**, chưa tự động thử lại — chưa biết ngưỡng N
nào đúng, đặt sai thì chặn nhầm trang tốt.

### Làm sạch là phép BIẾN ĐỔI, không phải sửa tệp

```
SAI    đọc .md → làm sạch → GHI ĐÈ .md
       mất bản gốc; đổi luật làm sạch thì không lùi lại được

ĐÚNG   đọc .md → làm sạch trong bộ nhớ → chunk → embed
       .md giữ nguyên vĩnh viễn
```

Lý do: `.md` là **thứ đắt nhất** — 49 MB nhưng tốn một ngày mạng mới có. PDF gốc
2,25 GB thì tải lại được từ web CTR. Xếp theo khả năng dựng lại:

| | Dung lượng | Mất thì sao |
|---|---|---|
| `.md` | 49 MB | **OCR lại — một ngày mạng** |
| PDF | 2,25 GB | chạy `tai_bao_cao_ctr.py` tải lại |
| `financial_figures` | vài MB | chạy script trích, vài giây |
| Qdrant | vài trăm MB | embed lại |

**Việc cần làm ngay: sao lưu 49 MB đó.** Rẻ đến mức không có lý do hoãn.

---

## 7. Số liệu cho khâu chunk

Phân bố độ dài trang (6.690 trang, **trước** khi làm sạch):

```
nhỏ nhất      63 ký tự        p90      3.491
p25        1.802              p99      9.292
trung vị   2.309              lớn nhất 17.939  ← trang &nbsp; hỏng, xem mục 6
p75        2.939

> 4.000 ký tự :  407 trang (6%)
Trang có bảng : 4.321 (65%)
```

**94% số trang dưới 4.000 ký tự** ⇒ chiến lược đơn giản nhất là tốt nhất:

```
MỘT TRANG = MỘT CHUNK
  lợi lớn nhất: trích dẫn chính xác tuyệt đối — chunk nào cũng biết đúng
                tai_lieu_id + trang, không phải đoán
  ngoại lệ: 407 trang > 4.000 ký tự thì cắt nhỏ,
            CẮT Ở RANH GIỚI BẢNG, tuyệt đối không cắt giữa một bảng
```

Vì sao không cắt giữa bảng: nửa bảng **tệ hơn không có gì** — mô hình đọc được vài
dòng số rồi trả lời tự tin, trong khi phần quan trọng nằm ở nửa kia.

Hai thứ còn phải đo trước khi chốt:
- giới hạn ngữ cảnh của `Qwen3-Embedding-4B` (2560 chiều đã biết)
- thời gian embed một lô 100 chunk, rồi mới suy ra cả kho — **đừng ước lượng**

---

## 8. Thiết kế lớp

```
preprocessing/
├── extraction/                      ← Strategy theo định dạng
│   ├── DocumentExtractor            interface: supports() + extract()
│   ├── ExtractionService            chọn bản cài qua List<DocumentExtractor>
│   └── PdfDocumentExtractor         gọi vào ocr/
└── ocr/
    ├── PageStore / FileSystemPageStore
    ├── PdfRenderer                  PDF → JPEG xám
    ├── VisionOcrClient              JPEG → Markdown (luồng + đồng hồ canh)
    ├── PageOcrWorker                thang DPI, thử lại
    └── DocumentOcrService           điều phối
```

### Vì sao Strategy chứ không Factory

Ba định dạng cần **ba luồng khác hẳn**, không phải ba bản cài của một luồng:

```
PDF    phải raster hoá rồi OCR — bảng mất cấu trúc nếu trích chữ thuần
DOCX   Apache POI đọc thẳng chữ VÀ bảng; OCR ở đây là BƯỚC LÙI
XLSX   vốn đã là bảng; chuyển sang .md rồi phân tích ngược là mất mát vô ích
```

Chỗ chung nằm ở **đầu ra** (các đơn vị Markdown có đánh số, ghi qua `PageStore`),
không phải đầu vào.

Không cần viết lớp Factory, vì **không có gì được tạo ra** lúc chọn — Spring đã tạo
cả ba extractor lúc khởi động. Chỉ cần tra cứu:

```java
extractors.stream().filter(e -> e.supports(name)).findFirst()
```

Phép thử phân biệt: *"có cái gì được TẠO RA không?"* Có → factory. Không, chỉ chọn
trong số đã có → strategy. Đối chiếu trong chính mã dự án:

```
PdfRenderer.open(path)        ← LÀ factory: mỗi tài liệu một PDDocument mới
extractors.filter(supports)   ← KHÔNG phải: ba thể hiện có sẵn, chỉ nhặt một
```

Tính chất quan trọng hơn tên gọi: **thêm định dạng = thêm một `@Component`, không sửa
dòng nào trong mã chọn.**

### Ba điều mọi bản cài phải tuân

```
1. supports()  chỉ TRẢ LỜI, không mở tệp — phải rẻ
2. extract()   ghi qua PageStore, không tự ghi tệp
               → mọi định dạng dùng chung quy ước trang_NNN.md và .md.loi
3. đánh số     đơn vị đầu tiên là 1, dù nguồn là "trang" PDF hay "sheet" Excel
```

Điều 2 là then chốt: nhờ nó mà khâu chunk và embed **không cần biết tài liệu gốc là
gì** — chúng chỉ thấy các tệp `.md` có đánh số.

### Ba bẫy kỹ thuật trong `PdfRenderer`

```
Loader.loadPDF(file)        PDFBox 3. Hướng dẫn cũ viết PDDocument.load() — đó là bản 2
synchronized (document)     PDDocument KHÔNG an toàn đa luồng; hỏng THẤT THƯỜNG,
                            có thể chạy 100 trang mới lỗi một lần
writer.dispose()            trong finally. Quên thì rò bộ nhớ: vô hình qua 10 trang
                            thử, chết qua 6.690 trang
```

Kết xuất nằm **trong** khoá, nén JPEG nằm **ngoài** khoá — ảnh là biến cục bộ nên nén
song song được. Giá của khoá gần bằng 0: kết xuất ~200ms so với gọi mạng 20–120s, sáu
luồng tranh khoá chỉ ~2% thời gian.

### Vì sao 6 luồng, không dùng virtual thread

```
6 luồng   6.690 × 60s ÷ 6  ≈  18,6 giờ   ← đúng bằng một ngày đã chạy thật
1 luồng   6.690 × 60s      ≈  111 giờ = 4,6 ngày
```

Nút thắt là **mạng** (~0,12 Mbps hữu dụng), không phải CPU hay luồng. Virtual thread
giải bài toán *"hàng nghìn lời gọi chặn cùng lúc"* — ở đây ta **cố ý chỉ muốn 6**.
Thêm nữa, trên Java 21 virtual thread bị **ghim** khi chặn trong khối `synchronized`
(JDK 24 mới sửa, JEP 491), mà `PdfRenderer.render` có đúng cấu trúc đó.

---

## 9. Ghi tệp phải NGUYÊN TỬ

`Files.writeString` cắt tệp rồi mới ghi. Bị ngắt giữa chừng (Ctrl+C, JVM chết) thì
còn lại tệp **nửa nội dung** — mà `exists()` thấy khác rỗng nên coi là **đã xong**,
không bao giờ OCR lại. Trang hỏng mà hệ thống tin là tốt.

```java
Path tmp = mdPath.resolveSibling(mdPath.getFileName() + ".tmp");
Files.writeString(tmp, markdown, UTF_8);
Files.move(tmp, mdPath, ATOMIC_MOVE, REPLACE_EXISTING);
```

Đổi tên là **một thao tác sửa mục thư mục**, không chia cắt được — dữ liệu đã nằm đủ
trên đĩa trước khi cái tên xuất hiện. `.tmp` phải **cùng thư mục** (cùng ổ đĩa), nếu
không `ATOMIC_MOVE` ném `AtomicMoveNotSupportedException`.

Giới hạn: che được tiến trình bị giết, **không** che được mất điện (mục thư mục có
thể đã ghi mà khối dữ liệu còn trong đệm). Không đáng trả giá `fsync` cho mỗi trang.

### Ba trạng thái của một trang

```
không có tệp nào         chưa thử           → sẽ OCR
có .md                   xong               → bỏ qua
có .md.loi (không .md)   thử 6 lần, hỏng    → cần xem tay, VẪN OCR LẠI lần sau
có cả hai                LỖI LOGIC          → thiếu deleteIfExists khi ghi thành công
```

---

## 10. Việc còn lại của khâu tiền xử lý

Trạng thái 10/10/2026 (bản v1.0 ghi ngày 03/10 ở cuối mục):

```
✔ OCR 6.690 trang (công cụ Node)
✔ FileSystemPageStore · PdfRenderer · VisionOcrClient · PageOcrWorker
✔ DocumentOcrService · PdfDocumentExtractor (vỏ Strategy)
✔ TextCleaner (4 luật ở mục 6)
✔ chunk + embed + ghi Qdrant              code xong, test đạt (lap-chi-muc_v1.11)
✔ luồng nạp hoàn chỉnh (mục 11)           code xong; CHƯA chạy thật với Qdrant
  nạp cả kho vào Qdrant                    chờ bật Docker, khoảng 0,08 USD
  trích số → financial_figures             330 trang, nhánh DB
```

`documents` có ba cột để lần: `ngay_ocr` · `ngay_trich_so` · `ngay_embed`; luồng ở mục 11 đã dùng `ngay_ocr` và `ngay_embed`.

**Bản v1.0 (03/10) ghi:** thứ tự đề nghị là trích số trước, chunk/embed sau, vì nhánh SQL chặn nhiều thứ hơn. Thực tế đã làm nhánh RAG trước (chunk, embed, Qdrant) vì nó là phần chính của đề tài và phụ thuộc ít vào việc xác minh số liệu bằng tay.

---

## 11. Luồng tiền xử lý hoàn chỉnh (10/10/2026)

### 11.1. Sơ đồ

![Luồng tiền xử lý FinQuery-RAG: từ file tải lên tới Qdrant](images/tien-xu-ly-luong_v1.1.png)

Nguồn của hình: `images/tien-xu-ly-luong_v1.1.html` (HTML + CSS). Sửa file HTML rồi chụp lại bằng Edge chạy ngầm, sau đó cắt phần trắng thừa ở đáy:

```
msedge --headless=new --hide-scrollbars --force-device-scale-factor=2 --window-size=1400,2400 \
       --screenshot=out.png file:///D:/VDT_FinQuery/logs/images/tien-xu-ly-luong_v1.1.html
```

### 11.2. Hai lối vào

| API | Dùng khi | Trả về |
|---|---|---|
| `POST /api/v1/documents` (multipart: `file` + `maCongTy`, `tenHienThi`, `loai`, `nam`, `ky`, `phamVi`, `cheDo`, `ngayKetThucKy`, `ngayBanHanh`) | Tải tài liệu mới | `202 Accepted` + trạng thái |
| `GET /api/v1/documents/{id}` | Giao diện hỏi định kỳ xem đã xong chưa | `cho_ocr` / `cho_embed` / `da_embed`, `soChunk`, các mốc thời gian |
| `POST /api/v1/documents/{id}/index` | Nạp lại một tài liệu (đổi luật làm sạch, đổi thuật toán chunk, OCR lại trang lỗi) | `202 Accepted` |
| `POST /api/v1/documents/index-pending` | Nạp mọi tài liệu đã OCR mà chưa embed: **cách nạp 278 tài liệu có sẵn** | `202 Accepted` + số tài liệu chờ |

Enum trong form nhận đúng chuỗi như trong DB (`loai=bctc`, `ky=Q4`, `phamVi=hop_nhat`). Lỗi trả JSON `{code, message}` qua `GlobalHandlerException` (trước đây file này trống, mọi lỗi đều thành 500).

### 11.3. Các bước

| # | Bước | Lớp chính | Kết quả, nơi lưu | Cờ |
|---|---|---|---|---|
| 1 | Tiếp nhận (luồng HTTP) | `DocumentController`, `DocumentIngestionService.upload()` | File gốc `data/raw/<ma_cong_ty>/tai_len/<loai>/<nam>/<tên>.<đuôi>`; một dòng `documents` | (dòng mới, `ngay_ocr = NULL`) |
| 2 | OCR, chỉ khi `ngay_ocr = NULL` | `ExtractionService` → `PdfDocumentExtractor` → `DocumentOcrService` → `PageOcrWorker` → `VisionOcrClient` → `FileSystemPageStore` | `data/ocr/<ma_cong_ty>/<duong_dan>/trang_NNN.md`; bỏ qua trang đã có file | Đủ trang thì `ngay_ocr`, `tong_so_trang`; còn trang lỗi thì dừng |
| 3 | Làm sạch | `TextCleaner` | Không lưu, làm lại mỗi lần đọc (mục 6) | — |
| 4 | Chunk | `Chunking`, `TableStitcher`, `TextStitcher` | Trong bộ nhớ | — |
| 5 | Embed | `Embedding`, `EmbeddingClient` (Qwen3-Embedding-8B qua DeepInfra) | Trong bộ nhớ, `float[4096]` mỗi chunk | — |
| 6 | Ghi Qdrant | `QdrantIndexWriter` | 1 chunk = 1 point trong `finq_pages_qwen3e8b` | Hạ cờ trước, dựng cờ `ngay_embed`, `so_chunk` sau cùng |

Bước 3–6 nằm trong `DocumentIndexingService.index()` (gốc gói `indexing/`). Bước 1–2 nằm trong `DocumentIngestionService` (gốc gói `preprocessing/`), service này gọi xuống `DocumentIndexingService.indexDocument()`.

### 11.4. Cờ trạng thái và bất biến

Các cột `ngay_ocr`, `ngay_embed` là **sổ ghi chép của code Java**, không phải thứ Qdrant đọc: Qdrant không biết MySQL tồn tại. `indexPending()` chọn tài liệu bằng `WHERE ngay_ocr IS NOT NULL AND ngay_embed IS NULL`; `GET /{id}` suy trạng thái từ hai cột này.

> **Bất biến:** `ngay_embed` khác NULL thì Qdrant chắc chắn có đủ point của tài liệu.

Thứ tự trong `index()` để giữ bất biến khi lỗi xảy ra ở bất kỳ đâu:

```
1. đọc trang → làm sạch → chunk → EMBED (gọi API)   ← phần chậm, dễ lỗi làm trước; lỗi thì Qdrant còn nguyên bản cũ
2. MySQL: ngay_embed = NULL                          ← HẠ CỜ, trước khi đụng Qdrant
3. Qdrant: deleteDocument → upsert (chờ ghi xong)
4. Qdrant: countDocument == số chunk?                ← lệch thì ném lỗi, cờ vẫn NULL
5. MySQL: ngay_embed = now, so_chunk = n             ← DỰNG CỜ, luôn là bước cuối
```

**Vì sao phải hạ cờ.** Chỉ có tác dụng khi **nạp lại** tài liệu đã nạp (cờ đang có giá trị). Không hạ cờ mà chết giữa bước xoá và bước ghi: MySQL vẫn nói "đã nạp", Qdrant có 0 point, và `indexPending()` sẽ bỏ qua tài liệu này mãi mãi (biến mất khỏi kết quả tìm kiếm mà không ai biết). Có hạ cờ: cờ là NULL nên lần chạy sau tự nạp lại. Với tài liệu mới, cờ vốn đã NULL nên bước này không đổi gì.

Hạ cờ và dựng cờ đặt trong `index()`, không đặt ở controller: **mọi đường nạp** (tải lên, nạp lại, `indexPending`) đều đi qua `index()`, nên không đường nào bỏ sót được. Cả hai dùng `DocumentRepository.updateEmbedState()`, một câu `UPDATE` chỉ chạm hai cột, không ghi đè các cột khác của dòng.

### 11.5. Luồng HTTP và luồng nền

```java
public void submit(String taiLieuId) {
    ingestionExecutor.submit(() -> process(taiLieuId));
}
```

- `() -> process(taiLieuId)` là **lambda**: một việc được gói lại nhưng **chưa chạy**. `ingestionExecutor.submit(...)` đưa việc vào hàng đợi rồi trả về ngay, nên controller trả `202 Accepted` trong vài mili giây; OCR có thể mất vài phút.
- `ingestionExecutor` (`config/IngestionConfig.java`) có **đúng một luồng**, tên `ingestion`: các tài liệu xử lý lần lượt. Song song đã có ở bên trong (`ocrPool` OCR 6 trang cùng lúc, embed theo lô), chạy nhiều tài liệu cùng lúc chỉ làm nghẽn API và tốn RAM.
- Exception trên luồng nền không quay về được controller: `process()` bắt mọi lỗi và ghi log; người dùng xem kết quả qua `GET /{id}`.
- Bước sau dự kiến thay hàng đợi trong bộ nhớ bằng **Kafka** (topic "tài liệu mới tải lên"): hàng đợi sống sót khi tắt ứng dụng, nhiều máy cùng xử lý được.

### 11.6. Module `modules/document/`

Bảng `documents` phục vụ cả lập chỉ mục, trích dẫn khi trả lời và agent, nên entity đặt ở module dùng chung chứ không nằm trong `indexing/`.

| File | Vai trò |
|---|---|
| `Document.java` | Entity của bảng `documents` (đủ cột V4, V8, V10). Tên cột tự suy từ tên trường (`taiLieuId` → `tai_lieu_id`). `ocrRelativePath()` = `<ma_cong_ty>/<duong_dan>`; `trangThaiNap()` suy `cho_ocr` / `cho_embed` / `da_embed` |
| `DocumentEnums.java` | `Ky`, `PhamVi`, `LoaiTaiLieu` (chuyển từ `QdrantEnums` sang, vì mô tả tài liệu chứ không riêng Qdrant), thêm `CheDo` (TT200, TT99) và `TrangThaiNap`. `QdrantEnums` chỉ còn `ElementType` |
| `DocumentEnumConverters.java` | `AttributeConverter` cho từng enum: JPA ghi `"bctc"` như dữ liệu đang có. `@Enumerated(EnumType.STRING)` sẽ ghi tên hằng `"BCTC"`, không khớp |
| `DocumentRepository.java` | `findByNgayOcrIsNotNullAndNgayEmbedIsNullOrderByTaiLieuId()`, `existsByMaCongTyAndDuongDan()`, hai câu `UPDATE` chỉ chạm cột trạng thái |
| `DocumentIdGenerator.java` | Viết lại hàm `sinh_id` của `devtools/quet_tai_lieu.py`. Test đối chiếu **4 ID thật** trong V5 (BCTC hợp nhất Q1, BCTC riêng bán niên, báo cáo ĐHĐCĐ có ngày ban hành, đính chính không có ngày): khớp cả 4 |
| `DocumentController.java`, `dto/UploadDocumentRequest.java`, `dto/DocumentStatusResponse.java` | API ở mục 11.2 |

`DocumentMetadata.from(Document)` lấy đúng các cột cần cho payload, để `QdrantIndexWriter` không phụ thuộc entity JPA.

### 11.7. Mã công ty thay cho "CTR" viết cứng (V10)

Trước đây "CTR" bị viết cứng ở ba chỗ: thư mục OCR, thư mục file gốc, tiền tố `tai_lieu_id`. Thêm công ty thứ hai là phải sửa code.

`V10__modify_documents_add_ma_cong_ty.sql`:
- Thêm cột `ma_cong_ty VARCHAR(16) utf8mb4_bin`, mặc định `'CTR'` để điền cho 278 dòng có sẵn, rồi **bỏ mặc định**: tài liệu mới bắt buộc ghi rõ công ty.
- Khoá duy nhất đổi từ `duong_dan` thành `(ma_cong_ty, duong_dan)`: hai công ty có thể có cùng đường dẫn.
- Đã áp lên MySQL ngày 10/10: `Successfully applied 1 migration ... now at version v10`.

Code: `Document.ocrRelativePath()`, `DocumentIdGenerator` và thư mục file gốc đều dùng `maCongTy`. ID cũ không đổi vì vốn bắt đầu bằng `CTR`. Payload Qdrant thêm `ma_cong_ty` kèm payload index (lọc theo công ty khi truy hồi).

`ma_cong_ty` là **chuỗi, không phải enum**: thêm công ty là thêm dữ liệu, không phải sửa code. Bảng `companies` riêng (tên, mã chứng khoán, ...) để dành cho hướng mở rộng nhiều công ty.

Còn "CTR" ở `EmbeddingTrialRunner` và `ChunkingTest`: cố ý giữ, vì đó là dữ liệu mẫu cố định của CTR, không phải cấu hình.

### 11.8. Kiểm tra đầu vào khi tải lên

| Kiểm tra | Lý do |
|---|---|
| Đuôi file phải có extractor (`ExtractionService.canExtract`): PDF, DOCX, XLSX | Từ chối trước khi lưu, không để lại file rác |
| `maCongTy` chỉ 2–16 chữ hoa hoặc số | Mã còn là **tên thư mục trên đĩa**; chặn kiểu `../x` (đã thử, trả 400) |
| Tên file: bỏ thư mục, bỏ đuôi, chỉ giữ chữ, số, `.`, `_`, `-` | Chặn path traversal; `FileSystemPageStore` cũng tự chặn đường dẫn thoát khỏi `data/ocr` |
| Tên file và `tenHienThi` chuẩn hoá **NFC** | Cổng NFC thứ ba trong `lam-sach_v1.1.md` mục 2.4 (trước đây ghi "chưa làm"). macOS gửi tên file dạng NFD, còn `duong_dan` so từng byte (`utf8mb4_bin`): "riêng" NFD và NFC thành hai đường dẫn khác nhau, chặn trùng bị lọt. `tenHienThi` nằm trong văn bản đem embed nên cũng phải chuẩn hoá |
| `(ma_cong_ty, duong_dan)` đã có thì trả 409 | Chặn nạp trùng |
| Lưu file **trước**, ghi DB **sau** | Lỗi giữa chừng chỉ để lại một file thừa, không để lại dòng trỏ vào file không tồn tại |
| `spring.servlet.multipart.max-file-size: 100MB` | PDF lớn nhất trong kho khoảng 44 MB; mặc định của Spring chỉ 1 MB. Vượt thì trả 413 |

Cổng NFC còn lại chưa làm: **câu hỏi người dùng** (bước chuẩn hoá ở tiền truy xuất).

### 11.9. Đã kiểm tra và chưa kiểm tra

| Đã kiểm tra (10/10/2026) | Kết quả |
|---|---|
| Unit test: `DocumentIdGeneratorTest` 2, `ChunkingTest` 11, `EmbeddingClientTest` 2, `VisionOcrClientTest` 3 | Đạt |
| `QdrantIndexWriterTest` (5 test, trước khi thêm `ma_cong_ty`) | Đạt trên Qdrant thật; lần chạy sau bị bỏ qua vì Docker tắt |
| Chạy ứng dụng với MySQL thật, tắt mọi trial runner (không tốn tiền API) | Flyway áp V10; `GET` đọc đúng tài liệu (enum, ngày, `maCongTy`, trạng thái `cho_embed`); 404 khi không có; 400 khi thiếu tên, sai `loai`, sai `ky`, sai định dạng, `maCongTy` sai |

**Chưa kiểm tra:** nạp thật vào Qdrant (cần Docker); tải lên một PDF thật rồi OCR qua luồng nền. Thứ tự dự kiến: bật Docker → 5 test Qdrant → `POST /{id}/index` một tài liệu nhỏ → `POST /index-pending` cả kho → đối chiếu khoảng 7.179 point.

---

## 12. Sự cố OCR khi đổi sang DeepInfra (10/10/2026)

**Triệu chứng:** OCR trial báo `OcrTimeoutException: model returned almost nothing (0 chars)` với mọi trang.

**Điều tra:** gọi thẳng DeepInfra bằng đúng request của `VisionOcrClient` với `trial-page-007.jpg`. API trả 200, stream đủ **2.772 ký tự**, `finish_reason: stop`; tức là model trả đủ, lỗi nằm ở phía đọc stream.

**Nguyên nhân:** `parseChunk` dùng chuỗi rỗng `""` làm tín hiệu "hết stream" (`[DONE]`). Chunk SSE đầu tiên của DeepInfra chỉ khai báo vai trò và có `"content":""`; code trả `""`, vòng đọc hiểu là hết stream nên `break` ngay dòng đầu. Gateway Viettel trước đây không gửi `content` rỗng trong chunk đầu, nên lỗi không lộ ra khi OCR 6.690 trang.

```
data: {"choices":[{"delta":{"role":"assistant","content":"","reasoning_content":null}}]}   ← chunk đầu
```

**Sửa (một dòng):** `content` rỗng trả `null` (bỏ qua, đọc tiếp); chỉ `[DONE]` mới trả `""`.

```java
return content.isTextual() && !content.asText().isEmpty() ? content.asText() : null;
```

Hệ quả phụ có lợi: chunk khai báo vai trò không được tính là "đã có chunk đầu", nên watchdog vẫn áp hạn chờ chunk đầu (120 giây) cho tới khi có chữ thật, không chuyển sớm sang hạn khoảng lặng (30 giây).

**Test:** `VisionOcrClientTest` (3 test, server giả trả SSE, không gọi API): stream kiểu DeepInfra có chunk đầu rỗng và câu trả lời bọc ```` ``` ````; `content: null` kiểu gateway cũ; model trả quá ít vẫn báo lỗi. Gỡ tạm bản sửa thì test đỏ đúng thông báo `almost nothing (0 chars)`.

**Bài học:** đừng dùng một giá trị bình thường (chuỗi rỗng) làm tín hiệu đặc biệt, vì dữ liệu thật có thể chứa đúng giá trị đó. Cách chắc hơn là kiểm tra `[DONE]` riêng hoặc dùng một hằng số riêng.

**Chi phí OCR trên DeepInfra** (Qwen3-VL-30B-A3B-Instruct, đo ở lần thử trên): 2.752 token vào, 1.725 token ra, khoảng **0,0014 USD/trang**. OCR lại cả kho khoảng 10 USD; thực tế chỉ tài liệu mới tải lên mới cần OCR.
