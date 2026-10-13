# Triển khai — v1.1: Keycloak giữ dữ liệu qua volume

*Ngày: 2026-10-13 · Kế thừa [trien-khai_v1.0.md](trien-khai_v1.0.md)*

- **Sự cố:** đăng nhập đúng mật khẩu vẫn nhận 401 từ `/realms/finquery/protocol/openid-connect/token`. Log Keycloak ghi `LOGIN_ERROR ... error="user_not_found"`.
- **Nguyên nhân:** container `finq-keycloak` đã bị tạo lại lúc 17:41, cùng lúc với `finq-mailpit` và `finq-qdrant`. Keycloak chạy `start-dev` và lưu CSDL H2 ở `/opt/keycloak/data/h2` bên trong container. Compose chưa gắn volume, nên tạo lại container là mất toàn bộ tài khoản. Lúc khởi động, `--import-realm` chỉ nạp lại cấu hình realm từ `realm-finquery.json` (vai trò, client, chính sách mật khẩu), không có người dùng.
- **Sửa:** `infra/keycloak/docker-compose.keycloak.yml` gắn volume có tên `keycloak-data` vào `/opt/keycloak/data`. File realm vẫn gắn chỉ đọc ở `data/import`. Đã kiểm tra `keycloakdb.mv.db` nằm trong volume `finq_keycloak-data`. Từ nay `down`/`up` hay tạo lại container đều giữ tài khoản. Chỉ `down -v` mới xoá.
- **Phải làm lại một lần sau sự cố:**
  1. Tạo lại 3 tài khoản demo bằng `tao-nguoi-dung.ps1` (`-Permanent`).
  2. Lấy secret mới của client `finquery-core` dán vào `backend/core-service/.env` (`KEYCLOAK_CORE_CLIENT_SECRET`), rồi khởi động lại backend. File realm không ghi secret, nên mỗi lần import Keycloak sinh secret mới.
