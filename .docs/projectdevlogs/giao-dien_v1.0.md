# Giao diện — v1.0: bộ nhận diện theo Viettel Money

*Ngày: 2026-10-13 · Nguồn: CSS của https://viettelmoney.vn/ (bộ token `--token-*` trong `wp-content/themes/viettel-money-static-site-2/style.css` và kit Elementor `post-885.css`), tải về đọc ngày 13/10/2026. Dùng chung cho frontend và slide.*

Người làm đề tài làm việc tại Viettel Money nên giao diện và slide theo đúng màu, font và phong cách của sản phẩm này. FinQuery vẫn ghi tên **FinQuery**; **không dùng logo Viettel Money** trong giao diện.

## 1. Màu

**Màu chủ đạo, đỏ Viettel Money** (`viettelpay-red`):

| Token | Mã | Dùng cho |
|---|---|---|
| red-100 | `#EE0033` | nút chính, nhấn mạnh, đường kẻ nhấn, tiêu đề slide |
| red-80 | `#F1335C` | hover |
| red-60 | `#F56685` | |
| red-40 | `#F899AD` | |
| red-20 | `#FCCCD6` | viền nhạt |
| red-10 | `#FDE5EB` | nền nhãn, nền dòng được chọn |
| red-05 | `#FEF2F5` | nền khối nhấn nhẹ |
| red-02 | `#FFFAFB` | |

**Chữ và màu trung tính** (`dark`):

| Mã | Dùng cho |
|---|---|
| `#191919` | tiêu đề |
| `#222222` (dark-100) | chữ đậm |
| `#4A4A4A` | chữ thường (màu chữ chính của trang) |
| `#7A7A7A` (dark-60) | chữ phụ, chú thích |
| `#A7A7A7` (dark-40) | |
| `#D3D3D3` (dark-20) | viền |
| `#E9E9E9` (dark-10) | đường kẻ |
| `#F4F4F4` (dark-05) | |
| `#FBFBFB` (dark-02) | |
| `#FFFFFF` | nền chính |

**Màu phụ:**

| Màu | Mã chính | Nền nhạt | Dùng cho |
|---|---|---|---|
| space-blue | `#0A2956` (navy) | `#F3F4F7`, `#FAFBFC` | nền khu vực, khối phụ, biểu đồ thứ hai |
| pine-blue | `#2CA196` | `#EAF6F4` | thành công: trích dẫn ✓ |
| gold | `#FFBE41` | `#FFF8EC` | cảnh báo: đang kiểm chứng, kết quả một phần |
| scarlet-red | `#FF4850` | `#FFEDED` | lỗi: trích dẫn ✗, số không có nguồn |

## 2. Chữ

- **Roboto** (Google Fonts): chữ thường 400, tiêu đề 600, nhấn 500. Font chính của trang.
- **FS Magistral Bold**: font thương hiệu Viettel, trang dùng cho tiêu đề lớn. Font có bản quyền, **không đóng gói vào code**. Máy nào đã cài thì slide dùng, không có thì dùng Roboto 700.
- Tiêu đề khu vực viết **IN HOA** (ví dụ "SẢN PHẨM", "DỊCH VỤ").

## 3. Hình khối

| Thuộc tính | Giá trị |
|---|---|
| Bo góc | thẻ 20 px (token 8, 12, 16, 20, 24 px); nút dạng viên thuốc 25–30 px; nhãn nhỏ 4–8 px |
| Bóng thẻ | `0 4px 12px rgba(0,0,0,.08)` |
| Bóng nút đỏ | `0 4px 12px rgba(238,0,51,.25)` (bóng ánh đỏ) |
| Bố cục | nền trắng, khu vực xen nền xám xanh nhạt `#F3F4F7`, thẻ trắng bo tròn, nhiều khoảng trắng, nút CTA đỏ dạng viên thuốc |

## 4. Áp dụng cho FinQuery

| Thành phần | Kiểu |
|---|---|
| Nút gửi câu hỏi | đỏ `#EE0033`, viên thuốc, bóng đỏ |
| Dòng thời gian các bước (sự kiện SSE) | chấm tròn đỏ cho bước đang chạy, xám cho bước đã xong; chữ `#4A4A4A` |
| Trích dẫn `[C1]`, `[D1]` | nhãn nền `#FDE5EB`, chữ `#EE0033`; bấm vào mở trang |
| Kết quả kiểm | ✓ pine-blue `#2CA196`, ✗ scarlet-red `#FF4850`, "đang kiểm chứng" gold `#FFBE41` |
| Khung xem trang | thẻ trắng bo 20 px, bóng thẻ; ảnh trang bên trái, chữ OCR bên phải, câu trích bôi nền `#FDE5EB` |
| Slide | nền trắng, thanh nhấn đỏ, tiêu đề `#191919`, số liệu nổi bật màu đỏ, bảng viền `#E9E9E9` |
