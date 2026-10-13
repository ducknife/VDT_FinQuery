# Xác thực và tài khoản (Keycloak) — v1.4: lỗi BOM khi tạo tài khoản bằng PowerShell 5.1

*Ngày: 2026-10-13 · Kế thừa [xac-thuc_v1.3.md](xac-thuc_v1.3.md) (các mục không nhắc ở đây giữ nguyên)*

## 1. Hiện tượng

`tao-nguoi-dung.ps1` đăng nhập admin được, nhưng bước tạo tài khoản báo `HTTP error - 400 Bad Request`, không kèm lý do. Lỗi chính sách mật khẩu thì Keycloak luôn ghi tên luật, nên nghi dữ liệu gửi lên bị hỏng.

## 2. Nguyên nhân (đo bằng byte)

Ghi phần container nhận được ra file rồi soi:
- **3 byte đầu là `EF BB BF` (BOM UTF-8)** đứng trước `{`. Keycloak báo `Unexpected character (0xfeff)`.
- PowerShell 5.1 tự chèn BOM khi đẩy chuỗi sang chương trình ngoài, kể cả khi đã đặt `$OutputEncoding` là UTF-8 không BOM.
- Dùng `Process.StandardInput` cũng bị: .NET Framework tạo bộ ghi theo mã hoá console và ghi BOM ngay lúc mở stdin.
- Bản bash không bị (bash không chèn BOM).

## 3. Sửa

Hàm `New-KeycloakUser` trong `tao-nguoi-dung.ps1` **không dùng stdin**:
- JSON → byte UTF-8 không BOM → base64 → biến môi trường `KC_USER_JSON`;
- `docker exec -e KC_USER_JSON finq-keycloak sh -c 'printf %s $KC_USER_JSON | base64 -d | kcadm.sh create users -r finquery -f -'`;
- biến được xoá ngay sau khi dùng; mật khẩu vẫn không nằm trên dòng lệnh.

Không đặt dấu nháy kép trong chuỗi `sh -c`: PowerShell 5.1 không thoát nháy kép bên trong tham số truyền cho chương trình ngoài. Chuỗi base64 không có khoảng trắng nên không cần nháy.

## 4. Kiểm trên Keycloak của người làm đề tài

Tạo một tài khoản tạm `kiem.tra.tam@viettel.com.vn` (họ tên tiếng Việt) bằng đúng hàm của script:
- tạo thành công (exit 0);
- đọc lại: `firstName "Kiểm"`, `lastName "Trà"` (đúng dấu);
- biến `KC_USER_JSON` không còn sau khi chạy;
- đã xoá tài khoản tạm.
