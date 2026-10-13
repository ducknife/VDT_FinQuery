# Giao diện — v1.6: thanh biểu tượng, sáng/tối, thông tin cá nhân, quản lý hội thoại

*Ngày: 2026-10-13 · Kế thừa [giao-dien_v1.5.md](giao-dien_v1.5.md)*

## 1. Thay đổi giao diện

| Mục | Nội dung |
|---|---|
| Thanh biểu tượng (`NavRail`) | Thay thanh trên cùng: logo; nút tròn Hỏi đáp / Tài liệu / Người dùng (theo quyền, nút đang chọn tô đỏ); tooltip bóng thoại khi rê chuột; dưới cùng là công tắc sáng/tối dạng viên thuốc dọc và ảnh đại diện (chữ cái đầu tên, nền xanh nhạt) |
| Sáng/tối (`theme.ts`) | Biến màu cho chế độ tối dưới `:root[data-theme='dark']`; chưa chọn thì theo hệ điều hành; chỉ lưu khi người dùng bấm chọn; áp trước khi vẽ trang để không nháy |
| Thông tin cá nhân (`ProfileDialog`) | Bấm ảnh đại diện: họ tên, email, vai trò; sửa họ, tên; đổi mật khẩu (mật khẩu hiện tại + mới + nhập lại, kèm luật mật khẩu); đăng xuất. Sửa tên xong thì xin token mới rồi hỏi lại `/me` để tên mới hiện ngay |
| Quản lý hội thoại | Nút "⋮" (tooltip "Thêm tùy chọn") trên mỗi hội thoại: Đổi tên (sửa tại chỗ, Enter lưu, Esc huỷ), Xoá (hộp xác nhận; xoá hội thoại đang mở thì về hội thoại mới) |
| Kéo giãn thanh lịch sử | Kéo mép phải, 220–480 px, nhớ độ rộng trong localStorage |
| Thanh cuộn | Mảnh 6 px, bo tròn, màu theo chế độ sáng/tối (cả trang) |
| Các bước | Đang trả lời thì mở; xong thì thu gọn thành "Các bước (n) ▾", bấm mới mở rộng |
| Ảnh trang dẫn chứng | Thanh phóng −/%/+ (50–300%), bấm % để về vừa khung; Ctrl + lăn chuột để phóng (sự kiện gắn `passive: false` để không phóng cả trang) |
| Màn hình hẹp | Thanh biểu tượng 60 px; lịch sử mở bằng nút "☰ Lịch sử" |

## 2. Backend kèm theo

- `DELETE /api/v1/conversations/{id}` và `PATCH /api/v1/conversations/{id}` (đổi tên, 1–200 ký tự): chỉ chủ hội thoại; xoá kéo theo tin nhắn và feedback (`ON DELETE CASCADE` sẵn có ở V11, không sửa migration).
- `PUT /api/v1/me/profile` (họ, tên) và `PUT /api/v1/me/password` (mật khẩu hiện tại + mới):
  - kiểm mật khẩu hiện tại bằng cách xin token cho `finquery-web`;
  - đặt mật khẩu mới qua Admin API;
  - lỗi chính sách mật khẩu dịch sang tiếng Việt;
  - chế độ dev (tắt bảo mật) báo không có tài khoản để sửa.
- `SecurityConfig`: `/api/v1/me/**` cho mọi người đã đăng nhập.
- Test:
  - `AccountLiveTest.tuSuaHoTenVaDoiMatKhau` (Keycloak thật): sửa họ tên đúng dấu; sai mật khẩu hiện tại bị chặn; mật khẩu ngắn báo "ít nhất 12"; chứa "Viettel" báo lỗi tiếng Việt; đổi đúng thì mật khẩu mới đăng nhập được, mật khẩu cũ không;
  - `SecurityMatrixTest` ép `finq.keycloak.client-secret=` rỗng để không phụ thuộc môi trường.

## 3. Sự cố người làm đề tài gặp: mọi tài khoản đều thành "dev" có quyền quản trị

- **Nguyên nhân:** `.env` của backend không có `FINQ_SECURITY_ENABLED=true`, nên `finq.security.enabled=false`. Core bỏ qua token và coi mọi người là `dev` (đủ ba vai trò). Hội thoại cũng lưu chung dưới `dev`. Đã kiểm: `GET /api/v1/me` không token ở cổng 8080 trả 200.
- **Sửa:** thêm `FINQ_SECURITY_ENABLED=true` vào `backend/core-service/.env`, khởi động lại backend. Hội thoại đã lưu dưới `dev` không hiện cho tài khoản thật (đúng thiết kế).

## 4. Kiểm

- `tsc -b`, `eslint`, `vite build` sạch.
- Chụp Chrome headless qua core thử ở cổng 8091: hỏi đáp (sáng), tài liệu (tối, hồ sơ trình duyệt mới đặt `preferredColorScheme=dark`).
