# Giao diện — v1.4: màn hình theo vai trò, quên mật khẩu, ảnh đăng nhập vector

*Ngày: 2026-10-13 · Kế thừa [giao-dien_v1.3.md](giao-dien_v1.3.md) · QĐ-046*

## 1. Màn hình theo vai trò

Tab trên thanh trên, chỉ hiện theo `permissions` của `/api/v1/me`. Mở thẳng bằng địa chỉ: `#hoi-dap`, `#tai-lieu`, `#nguoi-dung`.

| Tab | Ai thấy | Nội dung |
|---|---|---|
| Hỏi đáp | Cả ba vai trò | Như trước (giữ nguyên trạng thái khi đổi tab) |
| Tài liệu | Quản lý tài liệu, Quản trị | Bảng tài liệu (tìm theo tên/mã, lọc loại, phân trang 15); trạng thái Chờ OCR / Chờ index / Đã index + số chunk; "Index lại"; hộp thoại "Tải tài liệu lên" (PDF + tên, loại, năm, kỳ, phạm vi, ngày kết thúc kỳ, ngày ban hành; mã công ty CTR) |
| Người dùng | Quản trị | Bảng tài khoản (tìm theo email/tên); đổi vai trò ngay trên dòng (Chờ duyệt / Nhân viên / Quản lý tài liệu / Quản trị); tài khoản chờ duyệt tô vàng; khoá/mở; "Gửi mail đặt lại mật khẩu"; "Tạo tài khoản" (email, họ, tên, vai trò; người dùng nhận mail để tự đặt mật khẩu); không đổi được vai trò, không khoá được chính mình |

Backend: `GET /api/v1/documents?q=&loai=&page=&size=` (mới, sắp theo ngày ban hành giảm dần), `DocumentStatusResponse` thêm `ky`, `phamVi`, `ngayBanHanh`.

## 2. Trang đăng nhập

- **Quên mật khẩu ngay trên FinQuery:** nhập email → `POST /api/v1/auth/forgot-password` → "Nếu email có tài khoản FinQuery, hướng dẫn đặt lại mật khẩu đã được gửi…". Không chuyển sang Keycloak.
- **Ảnh** `src/assets/images/login-hero.svg` (vector, nét ở mọi kích thước) thay ảnh 447 px bị mờ:
  - nền đỏ chuyển sắc theo bộ nhận diện, đường cong trắng;
  - thẻ biểu đồ cột + đường xu hướng, thẻ trích dẫn "C1" có dấu kiểm, kính lúp;
  - câu "Hỏi số liệu, nhận câu trả lời có nguồn";
  - không dùng logo Viettel. Có logo độ phân giải cao từ bộ nhận diện thì đặt thêm.
- Nút bo góc 14 px, ô nhập 12 px (trước 8 px).

## 3. Khác

- `vite.config.ts`: đích proxy `/api` đổi được bằng biến `FINQ_API` (mặc định `http://localhost:8080`).
- Thuộc tính `[hidden]` thắng `display` của class, để giữ màn hình hỏi đáp khi đổi tab.

## 4. Kiểm

- `tsc -b`, `eslint`, `vite build` sạch.
- Chụp Chrome headless (1600×900): trang đăng nhập; "Tài liệu" với 278 tài liệu thật; "Người dùng" với 3 tài khoản demo (qua core thử ở cổng 8091).
- Chưa chụp được: hộp thoại tải lên, tạo tài khoản, màn hình quên mật khẩu (cần thao tác bấm).
