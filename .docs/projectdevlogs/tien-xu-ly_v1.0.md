# Tiền xử lý — v1.0

*Ngày: 2026-10-03 · Tài liệu mới · Nối tiếp [luong-hoi-dap_v1.0.md](luong-hoi-dap_v1.0.md)*

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

```
✔ OCR 6.690 trang (công cụ Node)
✔ FileSystemPageStore (Java)
~ PdfRenderer · VisionOcrClient        đang viết
  PageOcrWorker · DocumentOcrService
  PdfDocumentExtractor (vỏ Strategy)
  TextCleaner (4 luật ở mục 6)
  trích số → financial_figures         330 trang
  chunk + embed → Qdrant               6.690 trang
```

Thứ tự đề nghị: **trích số trước, chunk/embed sau** — nhánh SQL chặn nhiều thứ hơn
(tool, enum, chương đánh giá), còn nhánh RAG chạy được ngay cả khi chunk chưa tối ưu.

`documents` đã có sẵn ba cột để lần: `ngay_ocr` · `ngay_trich_so` · `ngay_embed`.
