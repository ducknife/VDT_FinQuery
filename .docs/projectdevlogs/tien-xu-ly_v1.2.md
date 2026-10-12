# Tiền xử lý — v1.2

*Ngày: 2026-10-10 (tối) · Bản trước: v1.1 (2026-10-10) · Nối tiếp [luong-hoi-dap_v1.0.md](luong-hoi-dap_v1.0.md) · Chi tiết chunk, embed, Qdrant: [lap-chi-muc_v1.12.md](lap-chi-muc_v1.12.md)*

## Thay đổi so với bản trước (v1.1)

- **Khâu tiền xử lý XONG** cả code lẫn dữ liệu: 278/278 tài liệu đã nạp vào Qdrant (7.179 point, khớp `SUM(so_chunk)`), 6.544 số liệu hiệu lực trong `financial_figures`.
- **Thêm mục 13 — Trích số vào `financial_figures`**: thiết kế, 5 bước, các lớp, 5 vòng chạy và sửa quy tắc, kết quả, ngoại lệ, các quyết định còn mở.
- **Thêm mục 14 — Nạp cả kho vào Qdrant**: kết quả, sự cố DevTools tự khởi động lại, DeepInfra quá tải (429), file `.class` hỏng, tìm thử.
- **Thêm mục 15 — Chạy thử cả luồng** (`PreprocessingTrialTest`).
- Mục 10, 11.1, 11.9 cập nhật trạng thái và đường dẫn: thư mục log chuyển từ `logs/` sang `.docs/projectdevlogs/` (10/10/2026).
- Luồng nạp có thêm bước trích số: OCR → **trích số** → lập chỉ mục.

(Thay đổi của v1.1 so với v1.0: thêm mục 11 luồng tiền xử lý hoàn chỉnh kèm sơ đồ; mục 12 sự cố OCR với DeepInfra; cập nhật mục 10.)

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

Trạng thái 10/10/2026, tối:

```
✔ OCR 6.690 trang
✔ làm sạch, chunk, table-to-json, embed, ghi Qdrant          278/278 tài liệu, 7.179 point
✔ trích số -> financial_figures (mục 13)                    6.544 số hiệu lực, 160 ngoại lệ
✔ luồng nạp hoàn chỉnh: tải lên -> OCR -> trích số -> lập chỉ mục
  quyết định cách xử lý số liệu của tài liệu TẢI LÊN MỚI      mục 13.8, chờ người làm đề tài chọn
  kiểm tra ngẫu nhiên khoảng 50 số so với PDF                 số đo độ chính xác cho báo cáo
```

Thứ tự thực tế: nhánh RAG làm trước (chunk, embed, Qdrant), trích số làm sau, ngược với đề nghị của bản v1.0. Lý do: RAG là phần chính của đề tài và trả lời được mọi trang ngay cả khi chưa có số liệu.

---

## 11. Luồng tiền xử lý hoàn chỉnh (10/10/2026)

### 11.1. Sơ đồ

![Luồng tiền xử lý FinQuery-RAG: từ file tải lên tới Qdrant](images/tien-xu-ly-luong_v1.1.png)

Nguồn của hình: `images/tien-xu-ly-luong_v1.1.html` (HTML + CSS). Sửa file HTML rồi chụp lại bằng Edge chạy ngầm, sau đó cắt phần trắng thừa ở đáy:

```
msedge --headless=new --hide-scrollbars --force-device-scale-factor=2 --window-size=1400,2400 \
       --screenshot=out.png file:///D:/VDT_FinQuery/.docs/projectdevlogs/images/tien-xu-ly-luong_v1.1.html
```

Sơ đồ vẽ trước khi có bước trích số; luồng hiện tại thêm bước trích số giữa OCR và lập chỉ mục (mục 13.6).

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

**Đã kiểm tra thêm (10/10, tối):** nạp thật cả kho vào Qdrant (mục 14). **Còn chưa kiểm tra:** tải lên một PDF thật rồi OCR qua luồng nền. Thứ tự dự kiến: bật Docker → 5 test Qdrant → `POST /{id}/index` một tài liệu nhỏ → `POST /index-pending` cả kho → đối chiếu khoảng 7.179 point.

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

---

## 13. Trích số vào `financial_figures` (10/10/2026)

### 13.1. Ý tưởng

Báo cáo tài chính Việt Nam theo **mẫu biểu pháp định** (TT200, từ 2026 là TT99): B01 bảng cân đối kế toán (TT99 gọi là báo cáo tình hình tài chính), B02 kết quả kinh doanh, B03 lưu chuyển tiền tệ. Mỗi dòng có **mã số cố định**, đó là cái neo để trích **bằng quy tắc**, không cần LLM và không cần người gõ tay.

```
tìm bảng có cột "Mã số" -> nhận ra B01 / B02 / B03 -> gán kỳ cho từng cột
-> tra mã nội bộ, kiểm tên, dấu, phép cộng kế toán -> chọn nguồn có thẩm quyền -> ghi DB
```

Không dùng LLM: quy tắc cho kết quả **chính xác tới đồng, không tốn tiền, chạy lại ra đúng kết quả cũ, kiểm thử được**; LLM có thể chép nhầm một chữ số mà không ai biết.

### 13.2. Các bước và quyết định chính

| Bước | Lớp | Quyết định |
|---|---|---|
| Đọc bảng | `common/markdown/MarkdownTableParser` | Dùng chung với table-to-json |
| Nhận bảng mẫu biểu | `StatementTableReader` | Cột mã nhận theo **dữ liệu** chứ không chỉ theo tiêu đề (OCR hay gộp ô "Mã số CHỈ TIÊU"). Giá trị của mỗi dòng là **N ô cuối** (cột số luôn ở bên phải cùng; ô thuyết minh lúc có lúc không làm các dòng lệch nhau). Chỉ nhận bảng có **2 hoặc 4** cột giá trị. Loại bảng đối chiếu trình bày lại ("phân loại lại", "đã trình bày trên", "chênh lệch"). Nhận mã dính đầu ô tên ("01 1. Doanh thu"). Suy mã cho dòng "- Nguyên giá", "- Giá trị hao mòn lũy kế" không ghi mã: cha 221 → 222, 223 |
| Nhận mẫu biểu | `DocumentFigureExtractor` | Theo tên gần bảng nhất (kể cả tên nằm trong bảng); kiểm chéo bằng **độ dài mã**: mã 3 chữ số là B01, 2 chữ số là B02 hoặc B03; không rõ thì cho các dòng "bỏ phiếu" theo (mã, tên). B03 gián tiếp hay trực tiếp: theo chữ trên trang, rồi bỏ phiếu |
| Bảng mất tiêu đề ở trang sau | `TableStitcher.repairTableHeaders` (dùng lại của bước chunk) + `readContinuation` | Bảng đầu trang ngay sau một mẫu biểu ở trang trước: dùng lại mẫu biểu và kỳ của cột; chỉ nhận khi **ít nhất 50% dòng** khớp (mã, tên) của đúng mẫu biểu đó |
| Gán kỳ cho cột | `ColumnPlanner`, `PeriodLabel` | **Kỳ của tài liệu quyết định các phương án được phép; tiêu đề cột chỉ để chọn và xác nhận; mâu thuẫn thì từ chối, không đoán.** B01: [cuối kỳ, 31/12 năm trước]. B02/B03 bốn cột: [quý này, quý này năm trước, lũy kế, lũy kế năm trước]. "01/01/Y" quy về 31/12/(Y−1). Cột 3–4 của bố cục 4 cột luôn là lũy kế, kể cả khi OCR làm rơi chữ "lũy kế" |
| Đọc số | `VietnameseNumberParser` | `5.627.168.242.619`; `(103.552.387.501)` là số âm; `-` là 0; sai dạng thì báo lỗi, không đoán |
| Tra mã | `FinancialCatalog` | `(che_do, mau_bieu, ma)` → `ma_noi_bo` qua `financial_item_mappings`; `documents.che_do` trống thì suy từ năm (TT99 từ 2026). Mã con có chữ (411a) bỏ qua |
| Kiểm tên | `NameMatcher` | Bỏ đánh số, so không dấu; bằng, chứa nhau, hoặc Jaccard ≥ 0,6. Bắt được ca đã biết "mã 223 ghi cho TSCĐ vô hình" |
| Kiểm dấu | | Chỉ tiêu bản chất âm (`dau_am_tu_nhien`) mà đọc ra số dương thì loại |
| Kiểm cân đối | `BalanceIdentities` | 34 phép cộng viết theo **mã nội bộ**, đúng cho cả TT200 và TT99 (TT99 đổi số mã: tổng tài sản 270 → 280). Ví dụ `TONG_TAI_SAN = TONG_NGUON_VON`, `LOI_NHUAN_GOP = DOANH_THU_THUAN − GIA_VON_HANG_BAN`, `TIEN_CUOI_KY = TIEN_DAU_KY + LCTT_THUAN_TRONG_KY + LCTT_ANH_HUONG_TY_GIA`. Lệch thì **loại mọi số tham gia** phép cộng đó |
| Chọn nguồn | `FigureSelector` | Mỗi kỳ một dòng hiệu lực. Thứ tự (theo V6, thêm hai bậc cho báo cáo quý): cột so sánh **đã trình bày lại** > đính chính > báo cáo năm > bán niên > cột so sánh của báo cáo năm, bán niên kỳ sau > báo cáo quý > cột so sánh của báo cáo quý. Số trình bày lại thắng mà khác số gốc thì giữ số gốc ở trạng thái `da_thay_the`, có `thay_the_id` |
| Ghi | `FinancialFigureRepository` (`JdbcTemplate`) | Xoá rồi ghi lại toàn bộ trong **một giao dịch**; ngoại lệ đã được người soát xử lý (`da_xu_ly = 1`) giữ lại |

**Chạy lại toàn bộ mỗi lần** chứ không từng tài liệu: chọn nguồn có thẩm quyền cần ứng viên của mọi tài liệu (báo cáo năm mới tải lên có thể thay số của báo cáo quý cũ). Chỉ đọc file trên máy, cả kho khoảng 7 giây.

`B03`: `TIEN_DAU_KY` và `TIEN_CUOI_KY` là số **thời điểm** nằm trong báo cáo thời kỳ: quy về ngày trước đầu kỳ và ngày cuối kỳ.

### 13.3. Năm vòng chạy và sửa quy tắc (cả kho, 91 tài liệu)

| Vòng | Sửa gì | Số qua chốt | Dòng ghi DB | Ngoại lệ | Ghi chú |
|---|---|---|---|---|---|
| 1 | Bản đầu | 15.098 | 6.289 | 264 | Trùng mã khác số 39 ca: bảng "23. Số liệu so sánh" (thuyết minh) bị đọc như B01 |
| 2 | Loại bảng đối chiếu; mã dính ô tên; cột lũy kế; thứ tự ưu tiên theo V6 | 16.023 | 6.534 | 153 | Trùng mã còn 0 |
| 3 | Chép tiêu đề bảng sang trang sau; suy mã 222/223; kiểm chéo mẫu biểu bằng độ dài mã | 16.286 | 6.576 | 117 | `LOI_NHUAN_SAU_THUE` vẫn chỉ 36 dòng |
| 4 | Lấy N ô cuối của dòng; bảng mất tiêu đề ở đầu trang dùng kỳ của bảng trước | 16.738 | 6.805 | 211 | `LOI_NHUAN_SAU_THUE` lên 76; mã lạ tăng vì bắt nhầm bảng thuyết minh |
| 5 | Phần tiếp theo phải có ≥ 50% dòng khớp (mã, tên) | 16.738 | 6.805 | **160** | Kết quả hiện tại |

Nguyên nhân `LOI_NHUAN_SAU_THUE` thấp ở vòng 1–3: phần cuối B02 (mã 50–62) nằm ở trang sau, tiêu đề chỉ còn `| | | |`, và các dòng lệch cột (dòng 50 thiếu ô thuyết minh, dòng 51 có).

### 13.4. Kết quả (vòng 5)

| Chỉ số | Giá trị |
|---|---|
| Tài liệu | 91 (90 BCTC + 1 đính chính); 3 đính chính bỏ qua vì thiếu kỳ hoặc phạm vi, không có bảng mẫu biểu |
| Bảng mẫu biểu đọc được | 370 |
| Số qua hết các chốt (ứng viên) | 16.738 |
| `financial_figures` | **6.544 dòng `hieu_luc`** + 261 dòng `da_thay_the` |
| Phủ thời gian | 33 mốc (hợp nhất), 34 mốc (riêng), 31/12/2017 → 30/06/2026 |
| Khác số giữa các nguồn | 1.389 khoá; đa số là chênh lệch thật (soát xét, kiểm toán, phân loại lại), bộ chọn lấy nguồn có thẩm quyền hơn |
| Thời gian | khoảng 7 giây cả kho |

Ngoại lệ (160): `lech_can_doi` 89, `ma_la` 35, `ten_khong_khop` 17, `khong_xac_dinh_ky` 11, `trung_ma_khac_so` 5, `khong_doc_duoc_so` 3.

**Ca trình bày lại có thật đã bắt đúng:** tổng tài sản hợp nhất 31/12/2022: 6.040.964.527.680 (`da_thay_the`) → 6.060.012.376.089 (`hieu_luc`, cột "01/01/2023 Đã điều chỉnh" của báo cáo năm 2023), lệch 19.047.848.409 đồng, khớp đúng ghi chú của V6.

**Lỗi thật mà các chốt bắt được:**
- Báo cáo năm 2021 hợp nhất: tổng tài sản lệch tổng nguồn vốn **9.000 đồng** (OCR sai một chữ số); các mốc lệch 900, 1.000, 6.000 đồng ở B03.
- Ô `(1047642.776.397)`, `104.302.74.795`: OCR làm hỏng nhóm nghìn.
- BCTC riêng quý 1/2022 và quý 1/2023 ghi cột so sánh là "31/12/**2020**": lỗi của chính báo cáo, bị từ chối vì mâu thuẫn kỳ.
- Mã `216` thay cho `261`; mã `223` ghi cho TSCĐ vô hình (ca đã biết).
- Chạy thử trang 9 BCTC hợp nhất quý 4/2023: `TIEN_CUOI_KY` lệch đúng 30 tỷ; phép cộng ra 817.851.176.502, đúng bằng "Tiền và tương đương tiền" ở B01 trang 5.

Chênh lệch thật giữa các nguồn, ví dụ: số đầu năm 2018 trong báo cáo năm 2019 khác báo cáo quý 3/2019 vì chi phí trả trước 29,2 tỷ được **phân loại lại** từ dài hạn sang ngắn hạn; số bán niên 2019 đã soát xét khác số quý 2.

### 13.5. Các file

| Gói | File |
|---|---|
| `modules/financial/` | `FinancialEnums` (MauBieu, CachLay, TrangThaiSo, LoaiSo, LoaiLoi), `FinancialCatalog`, `FinancialExtractionService`, `FinancialFigureRepository` (người làm đề tài đổi tên từ `FinancialFigureStore`), `ExtractionSummary` |
| `modules/financial/extraction/` | `StatementTableReader`, `StatementTable`, `ColumnPlanner`, `PeriodLabel`, `Period`, `VietnameseNumberParser`, `NameMatcher`, `BalanceIdentities`, `DocumentFigureExtractor`, `FigureCandidate`, `ExtractionIssue`, `FigureSelector` |
| `common/` | `markdown/MarkdownTable`, `markdown/MarkdownTableParser`, `text/VietnameseText` (so khớp không dấu) |
| `preprocessing/` | `DocumentPageReader` (đọc và làm sạch các trang, dùng chung cho lập chỉ mục và trích số) |
| Test | `FigureExtractionTest` (10 test, dùng các đoạn bảng thật đã gặp) |

API: `POST /api/v1/documents/extract-figures` (chạy lại toàn bộ, nền). Luồng nạp: OCR → **trích số** (chỉ BCTC, đính chính, khi `ngay_trich_so` NULL) → lập chỉ mục; trích số lỗi không chặn lập chỉ mục.

### 13.6. Vì sao ghi bằng `JdbcTemplate` mà không dùng Spring Data JPA

- Bảng dùng id `AUTO_INCREMENT`; với kiểu id này Hibernate **tắt ghi theo lô**, `saveAll` sẽ chạy khoảng 6.800 câu `INSERT` riêng lẻ.
- Cột sinh `khoa_hieu_luc` và thao tác "xoá hết, ghi lại trong một giao dịch" viết bằng SQL rõ hơn.
- Nhánh truy vấn DB sẽ dùng **SQL mẫu có tham số** (paper FinAI Data Assistant), hợp với `JdbcTemplate`.
- `documents` vẫn dùng JPA vì mỗi lần chỉ đọc hoặc sửa một dòng.

`id` của `financial_figures` lên tới khoảng 33.000 sau 5 vòng chạy: `AUTO_INCREMENT` không lùi khi `DELETE`. Không ảnh hưởng (không nơi nào tham chiếu `id` ngoài `thay_the_id`, được ghi lại mỗi vòng). Không dùng `TRUNCATE` vì trong MySQL nó tự commit, phá giao dịch.

### 13.7. Không cần file seed Flyway cho số liệu

Flyway dành cho lược đồ và dữ liệu tham chiếu cố định (từ điển chỉ tiêu, bảng ánh xạ). Số liệu là **kết quả chạy code trên file OCR**; chụp lại thành seed thì khi sửa quy tắc, seed cũ lệch với code mà không ai biết. Chạy lại bằng API khi cần.

### 13.8. Còn mở: số liệu của tài liệu tải lên mới

Quy tắc đã được chỉnh theo kiểu lỗi OCR của kho CTR; tài liệu công ty khác có thể sinh nhiều ngoại lệ hơn, nhưng cái giá là **thiếu số chứ không sai số** nhờ các phép cộng. Ba phương án:

| | Cách | Đánh giá |
|---|---|---|
| (a) | Tự động hoàn toàn, chỉ soát ngoại lệ (hiện tại) | Đủ cho 91 tài liệu đã có |
| (b) | Tự động điền sẵn form theo mẫu biểu, dòng trượt chốt tô đỏ, người xác nhận rồi mới ghi | **Đề xuất cho tài liệu tải lên mới**; cần giao diện |
| (c) | Nhập tay hoàn toàn | 300–400 số mỗi báo cáo, chậm và cũng sai được |

Chưa chốt. Việc nên làm cho báo cáo: kiểm tra ngẫu nhiên khoảng 50 số trong `financial_figures` so với PDF để có độ chính xác đo được.

---

## 14. Nạp cả kho vào Qdrant (10/10/2026)

### 14.1. Kết quả

| Chỉ số | Giá trị |
|---|---|
| Tài liệu | **278/278** có `ngay_embed` |
| Point trong Qdrant | **7.179**, đúng bằng `SUM(so_chunk)` trong MySQL và bằng con số ước tính từ hồi thiết kế chunk |
| Collection | `finq_pages_qwen3e8b`, 4.096 chiều, Cosine, trạng thái `green`, 8 payload index |
| Văn bản đem embed | Bảng dạng JSON (`indexing.table-format: json`) |
| Nạp thử một tài liệu trước | `CTR-BCTC-HN-20210630-Q2-713a6c`: 23 trang → 24 chunk, 9,3 giây |

### 14.2. Sự cố và cách xử lý

| Sự cố | Nguyên nhân | Xử lý |
|---|---|---|
| Lượt nạp dừng ở tài liệu 55/277 | **Spring DevTools tự khởi động lại app** khi thấy file class thay đổi (IDE tự biên dịch lúc người làm đề tài sửa file); executor bị ngắt | Luồng dừng **an toàn đúng thiết kế**: tài liệu chưa làm vẫn `ngay_embed = NULL`, Qdrant khớp MySQL (1.579 point lúc dừng). Chạy lại với `--spring.devtools.restart.enabled=false` |
| 2 tài liệu lỗi | DeepInfra trả **HTTP 429 "Model busy"** cả 3 lần thử (chờ 2 s, 4 s) | Chạy lại với `--embedding.max-attempts=8` (tổng chờ gần 1 phút): 225/225 thành công |
| `mvn spring-boot:run` báo `ClassNotFoundException: ExtractionService` (không có package) | IDE biên dịch `DocumentIngestionService.class` lúc dự án đang được nạp lại (sau khi di chuyển thư mục); Maven thấy `.class` mới hơn nguồn nên không biên dịch lại | `./mvnw clean compile` sau khi lượt nạp xong; 35/35 test qua |

**Bài học:** chạy việc nền dài (nạp, trích số) thì tắt DevTools restart; hoặc chạy bản đóng gói (`java -jar`).

### 14.3. Tìm thử (embed câu hỏi + lọc payload)

| Câu hỏi | Bộ lọc | Kết quả |
|---|---|---|
| "Tổng tài sản hợp nhất cuối năm 2023 là bao nhiêu?" | `nam = 2023`, `pham_vi = hop_nhat`, `loai = bctc` | Trang B01 của báo cáo năm 2023 (trang 6, 7) có trong top 5 nhưng **không đứng đầu**; đứng đầu là trang bìa (0,742); trang báo cáo quý 1 lọt vào vì thiếu bộ lọc `ky` |
| "Chính sách khấu hao tài sản cố định của công ty" | `loai = bctc` | Đúng các trang thuyết minh chính sách kế toán |

Kết luận cho bước sau: cần **trích bộ lọc từ câu hỏi** ("cuối năm" → `ky = nam`) và **rerank** để đẩy trang có bảng số lên đầu.

Xem dữ liệu trực tiếp: http://localhost:6333/dashboard → collection `finq_pages_qwen3e8b` (tab Points, Visualize). Chỉ xem, không sửa.

---

## 15. Chạy thử cả luồng trừ OCR (`PreprocessingTrialTest`)

Test chạy trên **trang 5–10 của BCTC hợp nhất quý 4/2023** (B01 vắt trang, B02 có trang mất tiêu đề, B03, thuyết minh), gọi đúng các lớp của luồng thật và ghi từng bước ra `backend/core-service/trial-preprocessing.md`. Không ghi gì vào MySQL hay Qdrant.

```
./mvnw test -Dtest=PreprocessingTrialTest                      # không gọi API
./mvnw test -Dtest=PreprocessingTrialTest -Dtrial.embed=true   # embed thật, khoảng 0,0002 USD
```

| Bước | Kết quả |
|---|---|
| Làm sạch | Mỗi trang bỏ 4–86 ký tự |
| Sửa tiêu đề bảng | Trang 8 được chép tiêu đề từ trang 7 |
| Chunk | 6 chunk, mỗi trang một chunk |
| Table-to-json | Dài ra 1,48–1,75 lần ở chunk có bảng |
| Embedding | 6 vector 4.096 chiều, độ dài 1,0000 (đã chuẩn hoá) |
| Point Qdrant | Id + 11 trường payload, lấy từ `QdrantIndexWriter.payloadOf` (tách ra thành hàm public để test dùng chung) |
| Trích số | 5 bảng, 246 số qua chốt, 2 ngoại lệ (mã 70 EPS chưa có trong bảng ánh xạ; `TIEN_CUOI_KY` lệch 30 tỷ, mục 13.4) |
