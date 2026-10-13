# Giao diện — v1.2: trang đăng nhập riêng của FinQuery

*Ngày: 2026-10-13 · Kế thừa [giao-dien_v1.1.md](giao-dien_v1.1.md) (hội thoại, feedback giữ nguyên) · QĐ-045 ([quyet-dinh_v1.28.md](quyet-dinh_v1.28.md))*

## 1. Thay đổi

Người làm đề tài muốn đăng nhập ngay trên giao diện FinQuery, không chuyển sang trang Keycloak.

```
Trang đăng nhập FinQuery: email công ty + mật khẩu
  → POST {keycloak}/realms/finquery/protocol/openid-connect/token
      (client_id=finquery-web, grant_type=password)
  → access token (5 phút) + refresh token
  → GET /api/v1/me (Bearer) → vào ứng dụng / chờ duyệt
```

| Thành phần | Việc |
|---|---|
| `src/auth/auth.ts` (viết lại, bỏ `keycloak-js`) | `login` (password grant), tự gia hạn bằng refresh token khi access token còn dưới 30 s (một lần cho cả trang), `logout` (thu hồi refresh token ở endpoint `logout` của Keycloak), `onSessionExpired` (về trang đăng nhập), `apiFetch`/`authHeaders` |
| Phiên | `sessionStorage`: tải lại trang không phải đăng nhập lại, đóng tab là hết |
| `AuthGate.tsx` | Form email + mật khẩu (nút Hiện/Ẩn), lỗi tiếng Việt (sai email/mật khẩu; tài khoản cần hoàn tất thiết lập; tài khoản bị khoá do sai nhiều lần), "Quên mật khẩu?" mở trang đặt lại của Keycloak (gửi link qua email), "Chờ duyệt", "Phiên đã kết thúc" |
| Đã bỏ | `keycloak-js`, `public/silent-check-sso.html` |

## 2. Kiểm

- `tsc -b`, `eslint`, `vite build` sạch.
- Ảnh chụp trang đăng nhập (Chrome headless): đúng màu, thẻ trắng bo góc, nút đỏ Viettel.
- Endpoint token của Keycloak nhận password grant từ `finquery-web`: sai mật khẩu trả `invalid_grant / Invalid user credentials`, có `Access-Control-Allow-Origin: http://localhost:5173`.
- Chưa: đăng nhập thật trên trình duyệt với tài khoản demo (cần mật khẩu của người làm đề tài và backend bật `FINQ_SECURITY_ENABLED=true`).

## 3. Lưu ý

- Tài khoản có việc bắt buộc (mật khẩu tạm, chưa xác minh email) không đăng nhập được bằng form này: Keycloak trả "Account is not fully set up". Tài khoản demo tạo bằng `-Permanent` nên không vướng. Tài khoản thường do admin tạo bằng mật khẩu tạm sẽ cần đổi mật khẩu qua "Quên mật khẩu".
- Sai mật khẩu 5 lần thì Keycloak tạm khoá tài khoản (chống dò mật khẩu).
