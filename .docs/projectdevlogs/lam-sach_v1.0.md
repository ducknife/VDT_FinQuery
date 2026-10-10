# Luật làm sạch văn bản sau OCR — v1.0

> Ngày chốt: 06/10/2026. Phạm vi: 6.690 trang `trang_NNN.md` của 278 tài liệu CTR trong `data/ocr/CTR/`.
> Mã nguồn: `preprocessing/datacleaner/NFCNormalization.java`, `preprocessing/datacleaner/TextCleaner.java`.

## 1. Nguyên tắc

- **Làm sạch lúc đọc, không ghi đè file `.md`.** Kho `.md` (49 MB) tốn khoảng một ngày mạng để OCR, nên nó là dữ liệu gốc. Mọi luật chạy trong bộ nhớ mỗi lần đọc (khoảng 0,03 ms/trang), nên khi sửa luật thì chỉ cần đọc lại, không phải OCR lại.
- **Không OCR lại 6.690 trang cũ.** Tài liệu cũ và tài liệu mới tải lên đều đi qua cùng `TextCleaner.clean()` lúc đọc.
- **Luật phải luỹ đẳng** (idempotent): `clean(clean(x)) == clean(x)`.
- **Không đụng tới cấu trúc markdown:** dòng kẻ bảng `|---|`, thụt đầu dòng, heading `#` giữ nguyên.
- Nơi gọi: chunker của indexing và bộ trích số liệu, không đi qua controller.

## 2. Luật đã chốt (theo thứ tự chạy)

| # | Luật | Cách làm | Số đo trên kho | Lý do |
|---|---|---|---|---|
| 7 | Chuẩn hoá Unicode NFC | `Normalizer.normalize(text, NFC)` trong `NFCNormalization.toNfc()` | 0 trang NFD hiện tại | Phòng tài liệu mới (Word trên macOS, copy từ web). Cột `utf8mb4_bin` so từng byte nên `ế` NFC (1 code point) khác `ế` NFD (3 code point). Câu hỏi người dùng cũng phải đi qua NFC để hai phía khớp nhau. Không dùng NFKC vì nó đổi cả ký tự như `²` → `2`. |
| 1 | Bóc hàng rào code | Bỏ ```` ```markdown ```` ở đầu và ```` ``` ```` ở cuối nếu bọc cả trang | 76 hàng rào trên 64 trang | Mô hình đôi khi bọc cả trang trong code block, làm hỏng việc nhận diện bảng và heading. |
| 2 | `&nbsp;` → dấu cách | `replace("&nbsp;", " ")` | 3.903 lần trên 63 trang. Trang tệ nhất `BCR-nam-2019/trang_002.md` có 97,3% là đệm: 17.940 → 491 ký tự | Đệm vô nghĩa, làm phình token và làm lệch embedding. |
| 6 | Hai dấu cách trở lên giữa dòng → một | `(?<=\S)[ \t]{2,}` → `" "` | 22.761 chỗ giữa dòng | Chỉ gộp sau ký tự khác trắng, nên **thụt đầu dòng được giữ** (1.003 dòng thụt 4+ dấu cách). |
| 5 | Khoảng trắng cuối dòng | `[ \t]+$` (MULTILINE) → `""` | 29.418 dòng trên 3.293 trang | Rác vô hình, chỉ tốn token. |
| — | `strip()` cả trang | | | Bỏ dòng trống đầu/cuối. |

## 3. Luật hoãn lại (chưa làm)

| # | Luật | Số đo | Vì sao hoãn |
|---|---|---|---|
| 3 | `<br>` trong ô bảng | 2.470 lần trên 380 trang | `<br>` đang **ngăn cách các mục trong một ô** (ví dụ nhiều khoản vay trong một ô). Xoá thẳng thì dính chữ. Nếu làm thì thay bằng `" ; "`, cần xem mẫu trước. |
| 4 | Số trang đứng một mình | 1.597 dòng; giới hạn ở 5 dòng đầu/cuối thì bắt được 1.596 | Dễ nhầm với số liệu thật (một dòng chỉ có `12` có thể là số thuyết minh). Ảnh hưởng nhỏ tới embedding. |

## 4. Cố ý không xử lý

| Hiện tượng | Số đo | Lý do giữ nguyên |
|---|---|---|
| Dãy gạch `-----` | 2.515 trên 2.516 lần là dòng kẻ bảng markdown | Xoá là hỏng bảng. |
| Tiêu đề lặp mỗi trang (tên công ty, "## Báo cáo tài chính hợp nhất") | khoảng 1.286 trang và 422 trang | Đây là ngữ cảnh có ích cho việc truy hồi (cho biết trang thuộc báo cáo nào), không phải rác. |
| CRLF, ký tự vô hình, U+00A0 | 0 | Không có trong kho. |
| Thụt đầu dòng | 1.003 dòng | Có thể mang cấu trúc phân cấp mục. |

## 5. Kiểm chứng

- `CleanerTrialRunner` (bật bằng `cleaner.trial.enabled`): làm sạch một trang, in số ký tự trước/sau, ghi `trial-cleaned.md`.
- `TestCleanerController` (`/test/cleaner/audit`, `/test/cleaner/page`, bật bằng `cleaner.test-api.enabled`) cùng `devtools/cleaner-studio` (cổng 5180): quét toàn kho, xếp theo phần trăm bị cắt giảm, xem trước/sau từng trang. **Chỉ đọc, không ghi.**
- Sau khi làm sạch, kích thước trang: trung vị 2.276 ký tự, p90 3.448, p99 8.364, lớn nhất 14.998. Không trang nào gần rỗng (< 20 ký tự).
