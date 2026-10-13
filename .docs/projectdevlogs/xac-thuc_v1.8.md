# Xác thực và tài khoản (Keycloak) — v1.8: quản lý tài khoản trong FinQuery, quên mật khẩu không rời trang

*Ngày: 2026-10-13 · Kế thừa [xac-thuc_v1.7.md](xac-thuc_v1.7.md) (các mục không nhắc ở đây giữ nguyên) · QĐ-046 ([quyet-dinh_v1.29.md](quyet-dinh_v1.29.md))*

## 1. Client `finquery-core` (tài khoản dịch vụ của core)

- Confidential client, chỉ `serviceAccountsEnabled`; không có đăng nhập người dùng.
- Tài khoản dịch vụ có quyền client `realm-management`: `manage-users`, `view-users`, `query-users`, `view-realm`. Cần `view-realm` để đọc vai trò realm khi gán; thiếu thì Keycloak trả 403.
- Có trong `realm-finquery.json` (không ghi secret) và đã tạo trên realm đang chạy.
- **Secret** do Keycloak sinh: Keycloak → realm `finquery` → Clients → `finquery-core` → Credentials → Client secret. Chép vào môi trường của backend: `KEYCLOAK_CORE_CLIENT_SECRET=...`. Thiếu secret thì API quản lý tài khoản trả 503, còn quên mật khẩu im lặng không gửi (ghi log cảnh báo).

## 2. Backend `modules/account`

| Thành phần | Việc |
|---|---|
| `KeycloakProperties` (`finq.keycloak.*`) | url, realm, client-id, client-secret (`KEYCLOAK_CORE_CLIENT_SECRET`), frontend-url (trang quay về sau khi đặt mật khẩu), thời hạn link 900 s |
| `KeycloakAdminClient` | Token `client_credentials` (dùng lại tới khi còn dưới 30 s); tìm, tạo, sửa, xoá người dùng; vai trò realm; `execute-actions-email` (UPDATE_PASSWORD); lỗi Keycloak thành `AppException` kèm `errorMessage` |
| `AccountService` | Mỗi người một vai trò FinQuery gán trực tiếp (pending = chờ duyệt); tạo tài khoản → gán vai trò → gửi mail để **người dùng tự đặt mật khẩu**; không tự hạ quyền, không tự khoá mình; quên mật khẩu luôn trả cùng một câu (không lộ email nào có tài khoản), mỗi email tối đa 1 lần / 60 s |
| `AccountController` | `GET/POST /api/v1/admin/users`, `PUT …/{id}/role`, `PUT …/{id}/enabled`, `POST …/{id}/reset-password` (chỉ admin); `POST /api/v1/auth/forgot-password` (mở, trả 202) |

`SecurityConfig`: thêm `POST /api/v1/auth/forgot-password` mở; `/api/v1/admin/**` chỉ `admin`.

## 3. Kiểm

- `SecurityMatrixTest` (token giả lập):
  - danh sách tài liệu: 3 vai trò được gọi;
  - quản lý người dùng: chỉ admin (503 vì môi trường test không có secret);
  - quên mật khẩu: 202 kể cả khi không có token.
- `AccountLiveTest` (Keycloak + Mailpit thật; secret lấy bằng kcadm vào biến môi trường của lệnh test, không in, không ghi file):
  - tạo tài khoản: vai trò analyst, họ tên đúng dấu;
  - đổi vai trò document_manager → pending; khoá → mở;
  - tự hạ quyền bị chặn;
  - **3 mail** (tạo, gửi lại, quên mật khẩu) tới Mailpit;
  - `@gmail.com` bị chặn ("Chỉ nhận email công ty");
  - đã xoá tài khoản tạm.
- Chạy core thử ở cổng 8091 (bảo mật tắt): `GET /api/v1/admin/users` trả 3 tài khoản demo; `GET /api/v1/documents` trả 278 tài liệu.

## 4. Khác

- Họ tên theo kiểu Việt Nam: họ = `lastName` đứng trước, tên = `firstName`. Ví dụ trong `tao-nguoi-dung.ps1/.sh` trước đây đảo ngược (lỗi của trợ lý); đã sửa ví dụ và đổi lại họ/tên 3 tài khoản demo (chỉ tên hiển thị).
- Mail đặt mật khẩu dẫn tới trang đặt mật khẩu của Keycloak (link dùng một lần, 15 phút), xong thì quay về FinQuery. Đây là trang duy nhất của Keycloak người dùng còn thấy, và chỉ thấy khi mở từ mail.
