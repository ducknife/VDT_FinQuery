# OCR Studio

Công cụ nội bộ để **OCR tiếp** kho tài liệu CTR. Mở lên bấm một nút, nó tự tìm những trang
chưa làm và làm tiếp — trang nào xong rồi thì bỏ qua, nên đóng giữa chừng lúc nào cũng được.

## Chạy

```
devtools\ocr-studio\chay.cmd
```

Hoặc:

```
cd devtools\ocr-studio
node server.mjs
```

Rồi mở <http://localhost:5179>.

Yêu cầu: **Node 18+** (máy đang có v24) và **Python có pymupdf** (đã có). Không cần `npm install` —
máy chủ chỉ dùng thư viện sẵn của Node, còn React nạp thẳng từ CDN.

## Nó làm gì

1. Quét toàn bộ PDF trong `data/raw/CTR/`, đếm số trang, nhớ vào `data/ocr/_chi_muc.json`
   (lần sau khỏi đếm lại).
2. So với `data/ocr/CTR/` xem trang nào đã có, trang nào còn thiếu.
3. Gọi **Qwen3-VL-32B-Instruct-FP8** qua Viettel AI, mỗi trang một lần, nhiều luồng song song.
4. Ghi từng trang ra `trang_001.md`, `trang_002.md`… rồi ghép thành `_TOAN_VAN.md`.

Cấu trúc kết quả phản chiếu đúng cấu trúc kho gốc:

```
data/raw/CTR/01_bao_cao_tai_chinh/2025/hop_nhat/BCTC-HN-2025_full.pdf
        ↓
data/ocr/CTR/01_bao_cao_tai_chinh/2025/hop_nhat/BCTC-HN-2025_full/
        trang_001.md … trang_098.md
        _TOAN_VAN.md
```

## Vài điểm đáng biết

**Tiếp tục được, không làm lại.** Điều kiện bỏ qua là tệp `trang_XXX.md` đã tồn tại và lớn hơn
40 byte. Muốn làm lại một trang thì xoá đúng tệp đó đi.

**Tự nhận trang tiếng Anh.** BCTC của CTR là bản song ngữ. Sau khi OCR, nếu một trang dài mà
gần như không có dấu tiếng Việt thì được đánh dấu `.en` và **loại khỏi `_TOAN_VAN.md`**. Tệp
gốc vẫn giữ, phòng khi cần.

**Nhắc gửi cho model** đã cố định trong `server.mjs`, gồm bốn điều kiện rút từ thực tế:
mỗi bảng ra bảng Markdown cột rời rạc · giữ đầy đủ từng chữ số · số âm giữ dấu ngoặc đơn ·
ô trống để trống chứ không điền 0.

**`temperature: 0`** — OCR phải tất định, chạy lại hai lần phải ra giống nhau.

**Số luồng.** Mặc định 6 vì chưa biết hạn mức của khoá. Đo được 5,2 trang/phút ở mức này.
Nâng lên 12–24 nếu hạn mức cho phép; nếu bị từ chối thì hạ xuống.

## Khoá API

Đọc từ `LLM_API_KEY` trong `.env` ở gốc dự án. Tệp đó đã nằm trong `.gitignore`.

## Hoạt ảnh

Ba hoạt ảnh nằm trong `public/hieu_ung.json` (245 KB), hiện ở **góc phải khung log**, có nút chọn:

| Tên | Nguồn | Kích thước |
|---|---|---|
| **Poké Ball** (mặc định) | tự dựng, đỏ–trắng đúng tông | 30×15, 16 khung, 14 fps |
| **Lửa** | [ascii-motion.app — "Fire"](https://ascii-motion.app/community/project/44870d67-36e2-4e2a-a6de-96eb077c81eb) | 40×22, 95 khung, 30 fps |
| **Charmander** | [ascii-motion.app](https://ascii-motion.app/community/project/10b59a51-da05-4a47-957f-782c7f054918) | 52×26, 16 khung, 12 fps |

Thư viện cộng đồng của ascii-motion.app chỉ có **20 dự án công bố và đúng 1 cái Pokémon**
(Charmander). Poké Ball là do tự dựng để có thêm một hoạt ảnh Pokémon sạch nét.

Cái đang chọn hiện ở trên, cái phụ hiện bên dưới **chỉ khi đang OCR**, đổi qua lại mỗi 10 giây.
Khi chạy thì hoạt ảnh nhanh hơn 1,4 lần — khung **không lắc lư**.

Mỗi hoạt ảnh tự mang nền riêng của nó (Charmander cần nền xanh đậm `#0A1628` mới đọc được nét).

## Màu

Đỏ–trắng, hai chế độ dùng chung một bộ biến CSS:

| | nền | chữ | nhấn |
|---|---|---|---|
| tối | `#3C3C3C` | `#FFFFFF` | `#E3350D` |
| sáng | `#FFFFFF` | `#1A1A1A` | `#C62828` |

Không dùng emoji. Trạng thái phân biệt bằng chữ và độ đậm: xong là trắng, lỗi là đỏ đậm,
bỏ qua là xám mờ.

## Ghi chú về dữ liệu

`data/raw/_ngoai_pham_vi/` chứa ba thư mục nằm ngoài sáu mảng A–F (báo cáo quản trị, điều lệ
quy chế, bản cáo bạch — 36 tệp, 415 MB). Chúng **không bị xoá**, chỉ chuyển ra ngoài phạm vi
quét. Muốn OCR luôn thì chuyển ngược vào `data/raw/CTR/`.
