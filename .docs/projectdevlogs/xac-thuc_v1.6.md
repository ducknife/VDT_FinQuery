# Xác thực và tài khoản (Keycloak) — v1.6: tắt tự đăng ký

*Ngày: 2026-10-13 · Kế thừa [xac-thuc_v1.5.md](xac-thuc_v1.5.md) (các mục không nhắc ở đây giữ nguyên) · QĐ-044*

- `registrationAllowed: false` trong `realm-finquery.json`; realm đang chạy cũng đã tắt bằng `kcadm update realms/finquery -s registrationAllowed=false`, không cần tạo lại container nên 3 tài khoản demo giữ nguyên.
- Trang đăng nhập không còn link "Đăng ký" (đã kiểm). Frontend không có nút đăng ký.
- Giữ: luật email `@viettel.com.vn`, chính sách mật khẩu, xác minh email và quên mật khẩu (gửi qua Mailpit khi demo), 3 vai trò.
- Tài khoản chỉ do quản trị viên tạo (script `tao-nguoi-dung.ps1`/`.sh` hoặc trang quản trị realm). Màn hình "chờ duyệt" vẫn giữ cho tài khoản tạo ra mà chưa gán vai trò.
- Đăng nhập trên frontend: xem [giao-dien_v1.1.md](giao-dien_v1.1.md).
