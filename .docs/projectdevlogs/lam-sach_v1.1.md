# Luật và chính sách làm sạch văn bản sau OCR — v1.1

> Ngày: 06/10/2026. Phạm vi: 6.690 trang `trang_NNN.md` của 278 tài liệu CTR trong `data/ocr/CTR/`.
> Mã nguồn: `preprocessing/datacleaner/NFCNormalization.java`, `preprocessing/datacleaner/TextCleaner.java`.

## Thay đổi so với bản trước (v1.0)

- Thêm **mục 2 — Chính sách**: làm sạch lúc đọc, phạm vi trách nhiệm, NFC ở hai cổng, chính sách khi đổi luật (phải embed lại), quy trình thêm luật mới.
- Thêm **bảng đối chiếu số hiệu luật**: số hiệu lúc quét kho (1–7) khác với số thứ tự ghi trong comment của `TextCleaner`.
- Ghi rõ luật bóc hàng rào **chỉ** áp dụng khi hàng rào bọc cả trang.
- Thêm **trạng thái triển khai** của từng cổng và từng nơi gọi.

---

## 1. Mục đích

Đầu ra OCR (Qwen3-VL) đúng về nội dung nhưng còn nhiễu định dạng: đệm `&nbsp;`, code fence bọc cả trang, khoảng trắng thừa. Nhiễu này làm phình token, làm lệch embedding và làm hỏng việc nhận diện bảng. Bộ làm sạch chỉ xử lý **nhiễu định dạng**, không sửa **nội dung**.

## 2. Chính sách

### 2.1. Làm sạch lúc đọc, không bao giờ ghi đè `.md`

- File `trang_NNN.md` là **dữ liệu gốc**: 49 MB, tốn khoảng một ngày mạng để OCR. Không công cụ nào được ghi đè nó.
- Làm sạch là một **phép biến đổi trong bộ nhớ** mỗi lần đọc: `PageStore.read()` → `TextCleaner.clean()` → bên tiêu thụ. Chi phí khoảng 0,03 ms/trang.
- Hệ quả: sửa luật thì **không phải OCR lại**, chỉ cần chạy lại các bước phía sau.
- **Không OCR lại 6.690 trang cũ.** Tài liệu cũ và tài liệu mới tải lên đều đi qua cùng một `clean()`.

### 2.2. Yêu cầu bắt buộc với mọi luật

| Yêu cầu | Ý nghĩa |
|---|---|
| Luỹ đẳng | `clean(clean(x)) == clean(x)`. Chạy nhiều lần không làm hỏng thêm |
| Không phá markdown | Dòng kẻ `\|---\|`, ô bảng, heading `#`, thụt đầu dòng giữ nguyên |
| Không đổi nghĩa | Không xoá chữ số, dấu chấm phân cách nghìn, dấu âm, dấu ngoặc của số âm |
| Có số đo | Luật chỉ được thêm khi đã đếm số lần xuất hiện và số trang bị ảnh hưởng trên toàn kho |
| Rẻ | Chỉ dùng thao tác chuỗi/regex, không gọi mạng, không gọi LLM |

### 2.3. Phạm vi trách nhiệm: cleaner KHÔNG làm

- Không sửa lỗi OCR về nội dung (đọc sai chữ số, sai dấu tiếng Việt). Đó là việc của bước kiểm tra số liệu khi trích vào `financial_figures`.
- Không xoá tiêu đề lặp mỗi trang (tên công ty, "Báo cáo tài chính hợp nhất"), vì đó là ngữ cảnh có ích cho truy hồi.
- Không chuyển bảng sang JSON (table-to-json là bước riêng của bên trích xuất).
- Không cắt chunk (đó là việc của `PageChunker` trong indexing).

### 2.4. Chuẩn hoá NFC ở hai cổng

Hai phía đem ra so với nhau thì phải cùng một dạng Unicode. Cột `utf8mb4_bin` so từng byte, nên `ế` NFC (1 code point) và `ế` NFD (3 code point) bị coi là khác nhau.

| Cổng | Đi qua | Trạng thái |
|---|---|---|
| Tài liệu (nội dung trang) | `TextCleaner.clean()` → `NFCNormalization.toNfc()` | Đã có |
| Câu hỏi người dùng | Bước chuẩn hoá ở pre-retrieval, trước khi trích bộ lọc và embed câu hỏi | **Chưa làm** |
| Tên file tải lên → `documents.duong_dan` | Chuẩn hoá trước khi ghi DB (macOS hay gửi tên file dạng NFD) | **Chưa làm** |

Không dùng NFKC: nó đổi cả ký tự tương thích như `²` → `2`, `①` → `1`, làm sai nghĩa.

### 2.5. Khi đổi luật

Vì vector trong Qdrant được tính từ văn bản **đã làm sạch**, nên đổi luật mà không embed lại thì vector cũ không còn khớp với văn bản mới.

1. Chạy audit trên Cleaner Studio để xem luật mới làm thay đổi bao nhiêu trang và trang nào.
2. Mở bản log mới (`lam-sach_v1.2.md`, ...), ghi số đo và lý do.
3. Đặt `documents.ngay_embed = NULL` cho các tài liệu bị ảnh hưởng (hoặc toàn bộ), rồi chạy lại indexing. Point ID cố định nên upsert lại sẽ ghi đè, không sinh bản trùng.
4. Nếu bước trích số liệu đã chạy thì cũng đặt `ngay_trich_so = NULL` cho các tài liệu đó.

*Đề xuất, chưa quyết:* lưu một hằng `CLEANER_VERSION` vào payload của từng point để biết vector nào được tính bằng bộ luật nào.

### 2.6. Quy trình thêm một luật mới

1. **Đo:** đếm số lần xuất hiện, số trang, xem phân bố (đầu trang, cuối trang, trong bảng hay ngoài bảng).
2. **Xem mẫu:** mở ít nhất 5 trang bị ảnh hưởng nặng nhất trên Cleaner Studio.
3. **Kiểm** các yêu cầu ở mục 2.2, nhất là luỹ đẳng và không phá bảng.
4. **Ghi** vào log theo bảng ở mục 3 rồi mới merge code.

## 3. Luật đã chốt

Thứ tự chạy trong `clean()`. Thứ tự là có chủ đích: chuẩn hoá Unicode trước để mọi luật sau chỉ gặp một kiểu mã hoá, và `&nbsp;` phải thành dấu cách trước khi gộp dãy dấu cách.

| Thứ tự | Số hiệu lúc quét | Luật | Cách làm | Số đo trên kho | Lý do |
|---|---|---|---|---|---|
| 1 | 7 | Chuẩn hoá Unicode NFC | `Normalizer.normalize(text, NFC)` trong `NFCNormalization.toNfc()` | 0 trang NFD hiện tại | Phòng tài liệu mới (Word trên macOS, copy từ web). Xem mục 2.4 |
| 2 | 1 | Bóc hàng rào code | Nếu trang **bắt đầu** bằng ```` ``` ```` thì bỏ dòng mở (```` ``` ```` hoặc ```` ```markdown ````) và ```` ``` ```` ở cuối. Hàng rào nằm giữa trang thì giữ nguyên | 76 hàng rào trên 64 trang | Model đôi khi bọc cả trang trong code block, làm hỏng nhận diện bảng và heading. Công cụ Node cũ chỉ bắt dạng không nhãn |
| 3 | 2 | `&nbsp;` → dấu cách | `replace("&nbsp;", " ")` | 3.903 lần trên 63 trang. Tệ nhất `BCR-nam-2019/trang_002.md` có 97,3% là đệm: 17.940 → 491 ký tự | Đệm vô nghĩa, làm phình token và lệch embedding |
| 4 | 6 | Dãy từ hai dấu cách trở lên giữa dòng → một | `(?<=\S)[ \t]{2,}` → `" "` | 22.761 chỗ giữa dòng | Chỉ gộp sau ký tự khác trắng, nên **thụt đầu dòng được giữ** (1.003 dòng thụt 4+ dấu cách) |
| 5 | 5 | Khoảng trắng cuối dòng | `[ \t]+$` (MULTILINE) → `""` | 29.418 dòng trên 3.293 trang | Rác vô hình, chỉ tốn token |
| 6 | — | `strip()` cả trang | | | Bỏ dòng trống ở đầu và cuối |

**Đối chiếu số hiệu:** comment đầu file `TextCleaner.java` đánh số 1 fence, 2 `&nbsp;`, 3 cuối dòng, 4 dãy dấu cách, còn code chạy dãy dấu cách **trước** khoảng trắng cuối dòng. Hai luật này đổi chỗ cho nhau thì kết quả vẫn như nhau. Log này dùng số hiệu lúc quét (1–7) để khớp với các trao đổi trước.

## 4. Luật hoãn lại

| Số hiệu | Luật | Số đo | Vì sao hoãn |
|---|---|---|---|
| 3 | `<br>` trong ô bảng | 2.470 lần trên 380 trang | `<br>` đang **ngăn cách các mục trong cùng một ô** (ví dụ nhiều khoản vay trong một ô). Xoá thẳng thì các chữ dính vào nhau. Nếu làm thì thay bằng `" ; "`, nhưng cần xem mẫu trước |
| 4 | Số trang đứng một mình | 1.597 dòng; giới hạn trong 5 dòng đầu/cuối trang thì bắt được 1.596 | Dễ nhầm với số liệu thật (một dòng chỉ có `12` có thể là số thuyết minh). Ảnh hưởng tới embedding nhỏ |

## 5. Cố ý không xử lý

| Hiện tượng | Số đo | Lý do giữ nguyên |
|---|---|---|
| Dãy gạch `-----` | 2.515 trên 2.516 lần là dòng kẻ bảng markdown | Xoá là hỏng bảng |
| Tiêu đề lặp mỗi trang | Tên công ty khoảng 1.286 trang; "## Báo cáo tài chính hợp nhất" 422 trang | Ngữ cảnh có ích cho truy hồi (mục 2.3) |
| Thụt đầu dòng | 1.003 dòng | Có thể mang cấu trúc phân cấp mục |
| CRLF, ký tự vô hình, U+00A0 | 0 | Không có trong kho |

## 6. Nơi gọi

| Nơi gọi | Mục đích | Trạng thái |
|---|---|---|
| `PageChunker` (indexing) | Làm sạch trước khi cắt chunk và embed | Chưa viết |
| Bộ trích số liệu vào `financial_figures` | Làm sạch trước khi parse bảng | Chưa viết |
| `CleanerTrialRunner` (bật bằng `cleaner.trial.enabled`) | Thử trên một trang, ghi `trial-cleaned.md` | Có, chỉ để thử |
| `TestCleanerController` (`/test/cleaner/audit`, `/test/cleaner/page`, bật bằng `cleaner.test-api.enabled`) cùng `devtools/cleaner-studio` (cổng 5180) | Quét toàn kho, xem trước/sau từng trang. **Chỉ đọc, không ghi** | Có, chỉ để thử |

## 7. Kích thước trang sau khi làm sạch

Trung vị 2.276 ký tự, p90 3.448, p99 8.364, lớn nhất 14.998. Không trang nào gần rỗng (dưới 20 ký tự). Đây là đầu vào cho thiết kế chunk ở `tien-xu-ly`.
