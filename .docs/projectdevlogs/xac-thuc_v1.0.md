# Xác thực và tài khoản (Keycloak) — v1.0

*Ngày: 2026-10-13 · QĐ-038, QĐ-042 ([quyet-dinh_v1.25.md](quyet-dinh_v1.25.md)) · File: `infra/keycloak/`*

## 1. Mô hình

Keycloak là máy chủ riêng, cài chuẩn **OAuth 2.0 + OpenID Connect**:

| Vai | Ở FinQuery |
|---|---|
| Authorization Server / OpenID Provider | Keycloak (container `finq-keycloak`, cổng 8180, realm `finquery`) |
| Client | Frontend `finquery-web`: public client, Authorization Code + **PKCE S256** |
| Resource Server | Core: kiểm chữ ký, hạn, issuer của JWT bằng khoá công khai (JWKS) của realm; đọc `realm_access.roles` |

- Mật khẩu chỉ Keycloak thấy và kiểm (băm argon2, mặc định của bản 26). Core và frontend không bao giờ nhận mật khẩu.
- Core không có bảng người dùng; bảng hội thoại, feedback chỉ lưu `sub`.
- Phân quyền ở core (`SecurityConfig`, bật bằng `FINQ_SECURITY_ENABLED=true`):
  - `POST /api/v1/documents/**` cần `admin`;
  - `/api/**` còn lại cần `analyst` hoặc `admin`;
  - token không có vai trò nào thì nhận 403.

## 2. Hai cách có tài khoản

| Cách | Luồng |
|---|---|
| **Tự đăng ký** (trang của Keycloak, frontend gọi `keycloak.register()`) | Nhập email công ty + họ tên + mật khẩu → Keycloak gửi **link xác nhận** → bấm link → đăng nhập được nhưng **chưa có quyền** → admin gán `analyst` |
| **Admin tạo** (`tao-nguoi-dung.ps1`) | Admin nhập email + **mật khẩu tạm** → người dùng **bắt buộc đổi mật khẩu** ở lần đăng nhập đầu (admin không biết mật khẩu thật) |

Triển khai thật trong công ty (hướng phát triển): nối Keycloak với **AD/LDAP** (User Federation) hoặc SSO sẵn có qua OIDC/SAML; core không phải sửa.

## 3. Cấu hình realm (`realm-finquery.json`)

| Mục | Giá trị | Ghi chú |
|---|---|---|
| Đăng ký | `registrationAllowed: true`, `registrationEmailAsUsername: true` | Tên đăng nhập là email |
| Xác minh email | `verifyEmail: true` | Chưa bấm link thì không đăng nhập được |
| Email công ty | User Profile, luật `pattern` cho trường email: `^[^@\s]+@viettel\.com\.vn$` | Áp cho cả form đăng ký và API admin; đổi tên miền ở luật này |
| Vai trò mặc định | `default-roles-finquery` = `offline_access`, `uma_authorization` (**không** gồm `analyst`) | Người mới chưa có quyền FinQuery cho tới khi admin duyệt |
| Chống dò mật khẩu | `bruteForceProtected: true`, khoá sau 5 lần sai | |
| Quên mật khẩu | `resetPasswordAllowed: true` | Link đặt lại gửi qua mail |
| SMTP | `mailpit:1025` (demo) | Triển khai thật: SMTP của công ty; mật khẩu SMTP để trong `infra/keycloak/.env`, không ghi vào json |
| Thông báo lỗi mật khẩu | Tiếng Việt (`localizationTexts.vi`) | |

### Chính sách mật khẩu

| Yêu cầu | Hệ thống tự chặn? | Cách |
|---|---|---|
| Từ 12 ký tự (tối đa 64) | Có | `length(12)`, `maxLength(64)` |
| Có chữ hoa, chữ thường, số, ký tự đặc biệt | Có | `upperCase(1)`, `lowerCase(1)`, `digits(1)`, `specialChars(1)` |
| Không có dấu nháy `'` `"` `` ` `` | Có | `regexPattern` |
| Không chứa tên công ty, hệ thống (Viettel, VCC, CTR, FinQuery; không phân biệt hoa thường) | Có | `regexPattern`. Hệ quả phụ: chặn cả từ có chứa "ctr" như "electric" |
| Không trùng, không chứa tên đăng nhập, email | Có | `notUsername`, `notContainsUsername`, `notEmail` |
| Không dùng lại 5 mật khẩu gần nhất | Có | `passwordHistory(5)` |
| Không chứa tên cá nhân, ngày sinh | **Không** (chỉ chặn được phần nằm trong email) | Keycloak không lưu ngày sinh, không có luật so với họ tên. Thành quy định cho người dùng |
| Không dùng chung cho nhiều hệ thống; không chia sẻ qua tin nhắn, email; không lưu nơi công khai | **Không** | Quy định cho người dùng; mật khẩu tạm do admin đặt phải đổi ngay nên admin không giữ mật khẩu thật |

## 4. Hạ tầng (`docker-compose.keycloak.yml`)

- `keycloak/keycloak:26.0.7` (bản Docker Hub; quay.io không tải được từ máy dev), chế độ `start-dev`, DB H2 nhúng, heap 384 MB, `mem_limit` 700 MB.
- `axllent/mailpit:v1.20.0`: hộp thư thử ở `http://localhost:8025`, `mem_limit` 64 MB.
- Chạy: tạo `infra/keycloak/.env` chứa `KC_ADMIN_PASSWORD` (file này đã nằm trong `.gitignore`), rồi `docker compose -f docker-compose.keycloak.yml up -d`.
- **Chưa có volume cho dữ liệu Keycloak**: `docker compose down` thì mất tài khoản đã tạo; realm nạp lại từ json. Muốn giữ thì thêm named volume cho `/opt/keycloak/data`. Khi đó sửa json phải xoá realm cũ mới nạp lại được.

## 5. Script tạo tài khoản (`tao-nguoi-dung.ps1`)

```powershell
.\tao-nguoi-dung.ps1 -Email nguyen.van.a@viettel.com.vn
.\tao-nguoi-dung.ps1 -Email quan.tri@viettel.com.vn -Role admin -FirstName "Quản" -LastName "Trị"
```

- Hỏi mật khẩu admin Keycloak và mật khẩu tạm lúc chạy (ẩn ký tự); **không có mật khẩu trong file**.
- Mật khẩu admin đi qua biến môi trường `KC_CLI_PASSWORD` (`docker exec -e KC_CLI_PASSWORD` không kèm giá trị). Mật khẩu người dùng đi qua stdin dưới dạng JSON. Không mật khẩu nào nằm trên dòng lệnh; script xoá biến sau khi dùng.
- Không dùng `Invoke-Expression` (bản cũ ghép mật khẩu vào chuỗi lệnh nên vỡ khi có dấu nháy).
- Gán `analyst` (và `admin` nếu `-Role admin`); `emailVerified = true` vì admin tạo từ danh bạ công ty.
- File lưu UTF-8 có BOM để PowerShell 5.1 hiện đúng tiếng Việt.

## 6. Kiểm bằng Keycloak chạy thật (13/10)

Chạy một Keycloak thử: mật khẩu admin sinh ngẫu nhiên, không in ra, xoá khi xong.

| Ca | Kết quả |
|---|---|
| Nạp realm | **Bản json cũ lỗi** `Unable to find composite realm role: uma_authorization` (chưa từng chạy thử). Sửa bằng cách khai báo `offline_access`, `uma_authorization`. Sau sửa: `Realm 'finquery' imported` |
| Email `@gmail.com` | Chặn: "Chỉ nhận email công ty (@viettel.com.vn)." |
| Mật khẩu 9 ký tự | Chặn (`invalidPasswordMinLengthMessage`) |
| Thiếu ký tự đặc biệt | Chặn |
| Có dấu nháy | Chặn (`regexPattern`) |
| Chứa "Viettel" | Chặn (`regexPattern`) |
| Chứa tên đăng nhập | Chặn (`notContainsUsername`) |
| `Bau-Troi#Xanh2026` | Tạo được; việc bắt buộc `UPDATE_PASSWORD` (mật khẩu tạm) |
| Vai trò người mới | Chỉ `default-roles-finquery`, `offline_access`, `uma_authorization`; gán `analyst` xong mới có quyền |
| Gửi mail xác nhận | Mailpit nhận mail "Verify email" có link `…/login-actions/action-token?key=…` |
| Đăng nhập admin qua `KC_CLI_PASSWORD` | Chạy |

Chưa kiểm: luồng đăng nhập từ frontend (chưa có `keycloak-js`), và core với `FINQ_SECURITY_ENABLED=true`.

## 7. Việc còn lại

- Frontend:
  - `keycloak-js`: đăng nhập, nút đăng ký (`keycloak.register()`), gắn `Authorization: Bearer`;
  - hiện "Tài khoản đang chờ duyệt" khi token không có vai trò `analyst`/`admin`.
- Chạy core với `FINQ_SECURITY_ENABLED=true` và thử từng API theo vai trò.
- Tuỳ chọn: volume dữ liệu Keycloak; giao diện (theme) trang đăng nhập theo màu Viettel Money.
