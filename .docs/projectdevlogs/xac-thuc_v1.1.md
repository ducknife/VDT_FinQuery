# Xác thực và tài khoản (Keycloak) — v1.1: ba vai trò, tài khoản demo, kiểm từ đầu đến cuối

*Ngày: 2026-10-13 · Kế thừa [xac-thuc_v1.0.md](xac-thuc_v1.0.md) (các mục không nhắc ở đây giữ nguyên) · QĐ-043 ([quyet-dinh_v1.26.md](quyet-dinh_v1.26.md))*

## 1. Ba vai trò lồng nhau

| Vai trò | Gồm | Được làm |
|---|---|---|
| `analyst` | — | Hỏi đáp, xem trích dẫn, xem trang tài liệu, lịch sử hội thoại, gửi feedback |
| `document_manager` | `analyst` | + nạp, index, trích số tài liệu (`POST /api/v1/documents/**`) |
| `admin` | `document_manager` + quyền client `realm-management`: `manage-users`, `view-users`, `query-users` | + duyệt và quản lý tài khoản ngay trong realm `finquery` (`http://localhost:8180/admin/finquery/console`), không cần tài khoản admin tổng của Keycloak; + API thử `/test/**` |

- Token mang **vai trò đã mở rộng** (ví dụ `admin` có cả `document_manager`, `analyst` trong `realm_access.roles`), nên core chỉ cần đọc token.
- Tài khoản admin tổng (`master`, `KC_ADMIN_PASSWORD`) chỉ người làm đề tài giữ; không đưa cho giám khảo.

`SecurityConfig` (khi `FINQ_SECURITY_ENABLED=true`):
- `POST /api/v1/documents/**` cần `document_manager` hoặc `admin`;
- `/api/**` cần một trong ba vai trò;
- **`/test/**` chỉ `admin`**: trước đây rơi vào `permitAll`, nghĩa là ai cũng gọi được API thử của bộ làm sạch kể cả khi đã bật bảo mật. Đã vá.

## 2. Tài khoản demo cho giám khảo

Giám khảo không có email `@viettel.com.vn` nên không tự đăng ký được. Người làm đề tài tạo sẵn 3 tài khoản bằng `-Permanent` (mật khẩu cố định, không bắt đổi ở lần đầu):

```powershell
.\tao-nguoi-dung.ps1 -Email nhan.vien@viettel.com.vn -FirstName 'Nhân' -LastName 'Viên' -Permanent
.\tao-nguoi-dung.ps1 -Email tai.lieu@viettel.com.vn  -FirstName 'Tài'  -LastName 'Liệu' -Role document_manager -Permanent
.\tao-nguoi-dung.ps1 -Email quan.tri@viettel.com.vn  -FirstName 'Quản' -LastName 'Trị' -Role admin -Permanent
```

- Mật khẩu đưa trực tiếp tại buổi báo cáo; không ghi vào repo, slide hay tin nhắn. Mật khẩu vẫn phải qua chính sách.
- **Họ và tên là tham số bắt buộc.** User Profile của realm yêu cầu họ tên; thiếu thì Keycloak báo "Account is not fully set up" và đòi bổ sung hồ sơ ở lần đăng nhập đầu.

## 3. Kiểm từ đầu đến cuối (Keycloak thử + core với `FINQ_SECURITY_ENABLED=true`)

Keycloak thử có mật khẩu admin ngẫu nhiên, không in ra; tắt và xoá sau khi kiểm. Core chạy ở cổng 8091, chỉ gọi các API không tốn LLM.

**Vai trò trong token của `finquery-web`** (đăng nhập bằng mật khẩu chỉ bật tạm trên bản thử; file realm vẫn tắt):

| Tài khoản | `realm_access.roles` | `iss` |
|---|---|---|
| nhan.vien | analyst | `http://localhost:8180/realms/finquery` (khớp `KEYCLOAK_ISSUER_URI`) |
| tai.lieu | document_manager, analyst | như trên |
| quan.tri | admin, document_manager, analyst | như trên |

**Phân quyền ở core:**

| API | Không token | analyst | document_manager | admin |
|---|---|---|---|---|
| `GET /api/v1/me`, `GET /api/v1/conversations` | 401 | 200 | 200 | 200 |
| `GET /api/v1/documents/{id}` (mã thử không tồn tại) | 401 | 404 | 404 | 404 |
| `POST /api/v1/documents/{id}/index` | 401 | **403** | 404 (đã qua kiểm quyền) | 404 |
| `GET /test/cleaner/audit` | 401 | 403 | 403 | 200 |

- `GET /api/v1/me` với token `tai.lieu` trả `{"id": "<sub>", "name": "tai.lieu@viettel.com.vn"}`.

**Quản lý tài khoản trong realm:** `quan.tri` xem được danh sách tài khoản; `nhan.vien` bị 403.

**Ghi chú kỹ thuật:**
- Client `admin-cli` của Keycloak 24+ phát **token rút gọn** (không có `realm_access`). Đây không phải lỗi; core nhận token của `finquery-web`.
- Gọi `kcadm.sh` từ Git Bash phải đặt `MSYS_NO_PATHCONV=1`, nếu không đường dẫn `/opt/...` bị đổi thành đường dẫn Windows.

## 4. Việc còn lại

- Frontend `keycloak-js`: đăng nhập, nút đăng ký, gắn `Authorization: Bearer`; hiện "chờ duyệt" khi token không có vai trò FinQuery; ẩn chức năng nạp tài liệu nếu không phải `document_manager`/`admin`.
- Người làm đề tài tạo `infra/keycloak/.env` (`KC_ADMIN_PASSWORD`) và 3 tài khoản demo trước buổi báo cáo.
- Tuỳ chọn: volume dữ liệu Keycloak để `docker compose down` không mất tài khoản.
