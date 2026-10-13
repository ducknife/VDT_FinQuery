# Giao diện — v1.7: logo mới, vùng chat tối, trang đăng nhập vừa màn hình

*Ngày: 2026-10-13 · Kế thừa [giao-dien_v1.6.md](giao-dien_v1.6.md)*

| Mục | Nội dung |
|---|---|
| Logo | Người làm đề tài cung cấp `src/assets/images/logo.png` (3464 px, 637 KB, **nền trắng đặc**). Tạo thêm, giữ nguyên file gốc: `logo-full.png` (hình + chữ, nền trong suốt, rộng 900 px, 172 KB) cho trang đăng nhập; `logo-mark.png` (chỉ hình, 256 px, 26 KB) cho thanh biểu tượng. Chỉ nền nối với mép ảnh thành trong suốt (tô loang từ 4 góc), phần trắng bên trong chữ và mặt kính lúp giữ nguyên. Làm bằng Pillow trong venv tạm, không đổi Python của máy |
| Vùng chat (chế độ tối) | Biến `--chat-bg`: tối `#30333a` (theo màu người làm đề tài gửi), khác thanh lịch sử (`--bg` `#17181d`); sáng `#f3f4f7` |
| Thanh biểu tượng | Hình tròn 52 → 42 px (biểu tượng giữ 22 px), công tắc sáng/tối 34 px, thanh rộng 64 px |
| Thanh lịch sử | Tiêu đề "Trò chuyện" (FS Magistral) trên nút "Cuộc hội thoại mới" |
| Gợi ý câu hỏi | Còn 3 câu: tăng trưởng doanh thu, nguyên nhân lợi nhuận tăng, cổ tức |
| Trang đăng nhập | Khoá `height: 100vh`, `overflow: hidden` (không cuộn dọc); logo `clamp(72px, 15vh, 140px)`; màn hình thấp (≤ 760 px) thu ô nhập, nút, tiêu đề; ≤ 700 px ẩn dòng phụ trên ảnh |
| Chữ trên ảnh đăng nhập | "Truy xuất dữ liệu, hỏi đáp tài chính" + "Mỗi con số dẫn tới đúng tài liệu và trang gốc". Đặt bằng HTML với font FS Magistral, vì ảnh SVG nạp qua `<img>` không dùng được font của trang; đã bỏ chữ khỏi SVG. Ảnh neo đỉnh khung (`object-position: 50% 0`), cụm hình minh hoạ dời xuống 60 đơn vị để không đè chữ |

**Kiểm:**
- `tsc -b`, `eslint`, `vite build` sạch.
- Chụp Chrome headless:
  - đăng nhập 1600×900 và 1366×640, không đè chữ, không cuộn;
  - hỏi đáp chế độ tối (core thử cổng 8091).
