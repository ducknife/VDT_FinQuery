# Xác thực và tài khoản (Keycloak) — v1.3: script tự đọc mật khẩu admin từ `.env`

*Ngày: 2026-10-13 · Kế thừa [xac-thuc_v1.2.md](xac-thuc_v1.2.md) (các mục không nhắc ở đây giữ nguyên)*

## 1. Hiện tượng

Sau khi sửa `$` trong `.env` và tạo lại container, chạy script vẫn báo `Invalid user credentials`. Đã kiểm:
- mật khẩu trong container khớp `.env` (so mã băm);
- log Keycloak có `LOGIN_ERROR … error="invalid_user_credentials", username="admin"`.

Vậy chuỗi gõ vào ô ẩn khác với giá trị thật. Nguyên nhân hay gặp:
- bộ gõ tiếng Việt (Unikey/EVKey, Telex) tự đổi `aa`, `dd`, `w`, `s/f/r/x/j` khi gõ, mà ô ẩn không cho thấy;
- gõ cả phần thoát của `.env` (dấu nháy `'...'`, `$$`).

## 2. Sửa

Cả `tao-nguoi-dung.ps1` (hàm `Read-EnvPassword`) và `tao-nguoi-dung.sh` (hàm `env_password`):
- **tự đọc `KC_ADMIN_PASSWORD` từ `.env` cạnh script** theo quy tắc của Compose: `'...'` giữ nguyên; `"..."` bỏ nháy; không nháy thì `$$` → `$`;
- không có `.env` thì mới hỏi;
- không in mật khẩu (chỉ báo "Dùng KC_ADMIN_PASSWORD trong .env");
- thông báo lỗi đăng nhập nhắc tắt bộ gõ tiếng Việt và không gõ phần thoát.

Kiểm trên Keycloak của người làm đề tài (chỉ bước đăng nhập admin, không tạo tài khoản): cả hai bản đọc được mật khẩu 13 ký tự và đăng nhập thành công. File cấu hình đăng nhập thử trong container đã xoá.

## 3. Sự cố trong lúc sửa

Một lệnh Python một dòng mở `tao-nguoi-dung.sh` để ghi (làm file rỗng) trước khi đọc, nên xoá trắng file. Đã khôi phục từ bản đã stage trong git (`git show :infra/keycloak/tao-nguoi-dung.sh`) rồi áp lại phần sửa.

**Bài học:** đọc hết nội dung vào biến rồi mới mở file để ghi.
