# Xác thực và tài khoản (Keycloak) — v1.5: bảng quyền ở core và test tự động

*Ngày: 2026-10-13 · Kế thừa [xac-thuc_v1.4.md](xac-thuc_v1.4.md) (các mục không nhắc ở đây giữ nguyên) · QĐ-043*

## 1. Bảng quyền (`SecurityConfig`, bật bằng `FINQ_SECURITY_ENABLED=true`)

Vai trò và quyền định nghĩa một chỗ trong `common/security/Roles`. Quy tắc đầu tiên khớp được áp dụng:

| Đường dẫn | Ai được gọi |
|---|---|
| `GET /api/v1/me` | **Ai đã đăng nhập** (kể cả chưa được duyệt, để giao diện báo "chờ duyệt") |
| `POST /api/v1/documents/**` (nạp, index, trích số) | `document_manager`, `admin` |
| `/api/v1/retrieval/**` (thử truy hồi) | `document_manager`, `admin` (mới: trước đây cả `analyst`) |
| `/api/**` còn lại: hỏi đáp (`/ask`, `/ask/sync`), xem tài liệu, trang, hội thoại, feedback | Cả ba vai trò |
| `/test/**` | `admin` |
| Đường dẫn không khai báo | **Chặn** (mới: trước đây `permitAll`) |

- Duyệt và quản lý tài khoản không nằm ở core: `admin` làm trong trang quản trị realm `finquery` của Keycloak.
- Hội thoại, feedback luôn lọc theo `sub` của người gọi: không ai xem được của người khác, kể cả `admin`.
- Tắt bảo mật (mặc định khi chưa có Keycloak): người dùng `dev` đủ ba vai trò.

## 2. `/api/v1/me`

```json
{ "id": "<sub>", "name": "tai.lieu@viettel.com.vn",
  "roles": ["analyst", "document_manager"],
  "permissions": { "ask": true, "manageDocuments": true, "manageUsers": false } }
```

Giao diện dùng `permissions` để ẩn hoặc hiện chức năng; core vẫn kiểm lại ở mỗi request.

## 3. Test `SecurityMatrixTest`

- `spring-security-test` (scope test): token giả lập có `realm_access.roles` như Keycloak phát, đi qua đúng converter của `SecurityConfig`. Không cần Keycloak, không gọi LLM hay embedding.
- Mỗi request chọn để dừng trước phần tốn tiền (400/404 nghĩa là đã qua kiểm quyền).

| Request | Không token | Chưa duyệt | analyst | document_manager | admin |
|---|---|---|---|---|---|
| `GET /api/v1/me` | 401 | 200 | 200 | 200 | 200 |
| `GET /api/v1/conversations` | 401 | 403 | 200 | 200 | 200 |
| `POST /api/v1/feedback` (thiếu messageId) | 401 | 403 | 400 | 400 | 400 |
| `GET /api/v1/documents/{id}` (không có) | 401 | 403 | 404 | 404 | 404 |
| `POST /api/v1/documents/{id}/index` | 401 | 403 | **403** | 404 | 404 |
| `GET /api/v1/retrieval/search` (câu trống) | 401 | 403 | **403** | 400 | 400 |
| `GET /test/cleaner/page` (thiếu path) | 401 | 403 | 403 | 403 | 400 |
| Đường dẫn không khai báo | 401 | 403 | 403 | 403 | 403 |

Thêm ca kiểm `/me` trả đúng vai trò và `permissions` (chưa duyệt: không vai trò, `ask = false`). Kết quả: 2/2 ca đạt; chạy cùng các test khác 32/32 đạt.
