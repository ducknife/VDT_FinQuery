# Xác thực và tài khoản (Keycloak) — v1.2: script bash, lỗi `$` trong `.env`

*Ngày: 2026-10-13 · Kế thừa [xac-thuc_v1.1.md](xac-thuc_v1.1.md) (các mục không nhắc ở đây giữ nguyên)*

## 1. Script bash `tao-nguoi-dung.sh`

Bản bash của `tao-nguoi-dung.ps1`, chạy được trên Linux, macOS, Git Bash:

```bash
./tao-nguoi-dung.sh --email nhan.vien@viettel.com.vn --first-name 'Nhân' --last-name 'Viên' --permanent
./tao-nguoi-dung.sh --email tai.lieu@viettel.com.vn  --first-name 'Tài'  --last-name 'Liệu' --role document_manager --permanent
./tao-nguoi-dung.sh --email quan.tri@viettel.com.vn  --first-name 'Quản' --last-name 'Trị' --role admin --permanent
```

- Hỏi mật khẩu bằng `read -s` (không hiện ký tự).
- Mật khẩu admin đi qua biến môi trường `KC_CLI_PASSWORD` (chỉ ở lệnh đăng nhập, `unset` ngay sau đó); mật khẩu người dùng đi qua stdin dưới dạng JSON.
- Đặt `MSYS_NO_PATHCONV=1` để chạy được trên Git Bash.
- `KC_CONTAINER` đổi tên container nếu cần.
- File thực thi (`100755` trong git). Thêm `.gitattributes` ở gốc repo: `*.sh` luôn xuống dòng LF (CRLF làm bash lỗi `$'\r'`), `*.ps1` giữ CRLF.

**Kiểm** bằng một lệnh `docker` giả (ghi lại tham số, biến môi trường và stdin), vì máy chỉ còn khoảng 0,6 GB RAM nên không chạy được Keycloak thử thứ hai:
- `KC_CLI_PASSWORD` chỉ có ở lệnh `config credentials`; các lệnh sau không có.
- Không mật khẩu nào nằm trong tham số.
- JSON thoát đúng `"`, `\` và chữ tiếng Việt.
- Vai trò sai thì in hướng dẫn và thoát.

## 2. Lỗi `$` trong `infra/keycloak/.env`

- **Hiện tượng:** chạy script báo `Invalid user credentials [invalid_grant]` dù gõ đúng mật khẩu admin.
- **Nguyên nhân:** Docker Compose coi `$xxx` trong `.env` là biến và thay bằng chuỗi rỗng. Mật khẩu 16 ký tự trong file thành 8 byte trong container (so bằng mã băm SHA-256 và độ dài, không đọc hay in mật khẩu).
- **Sửa:** bọc giá trị trong nháy đơn (`KC_ADMIN_PASSWORD='...'`) hoặc viết `$` thành `$$`, rồi `docker compose down` và `up -d`. Keycloak chỉ tạo admin tổng ở lần đầu với DB trống, nên `restart` không đủ.
- Sau khi người làm đề tài sửa và tạo lại container: mã băm trong container khớp `.env`.
