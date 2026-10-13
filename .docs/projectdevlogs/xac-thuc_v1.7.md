# Xác thực và tài khoản (Keycloak) — v1.7: bật direct access grant cho `finquery-web`

*Ngày: 2026-10-13 · Kế thừa [xac-thuc_v1.6.md](xac-thuc_v1.6.md) (các mục không nhắc ở đây giữ nguyên) · QĐ-045*

- `finquery-web`: `directAccessGrantsEnabled: true` trong `realm-finquery.json` và trên realm đang chạy (kcadm), để trang đăng nhập của FinQuery lấy token bằng email + mật khẩu ([giao-dien_v1.2.md](giao-dien_v1.2.md)).
- Vẫn giữ Authorization Code + PKCE (`standardFlowEnabled`, redirect `http://localhost:5173/*`) cho trang "Quên mật khẩu" và nếu sau này đổi lại cách chuyển hướng.
- Keycloak vẫn kiểm mật khẩu, chính sách, chống dò (khoá sau 5 lần sai). Core không đổi: chỉ kiểm token.
- **Đánh đổi:** frontend nhận mật khẩu trong lúc gửi đi; OAuth 2.1 không khuyến khích password grant. Chấp nhận vì là công cụ nội bộ; khi triển khai thật bắt buộc HTTPS. Hướng thay thế an toàn hơn nếu cần: theme trang Keycloak cho giống FinQuery (vẫn chuyển hướng), hoặc BFF (core giữ token trong cookie httpOnly).
