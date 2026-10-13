# Giao diện — v1.3: trang đăng nhập kiểu SSO, logo, font FS Magistral cho đề mục

*Ngày: 2026-10-13 · Kế thừa [giao-dien_v1.2.md](giao-dien_v1.2.md) (luồng đăng nhập giữ nguyên)*

## 1. Thay đổi

| Mục | Nội dung |
|---|---|
| Bỏ dòng mô tả | Bỏ "Trợ lý tra cứu báo cáo tài chính · Tổng Công ty Cổ phần Công trình Viettel (CTR)" ở thanh trên và trang đăng nhập (theo yêu cầu người làm đề tài) |
| Trang đăng nhập | Bố cục như trang SSO mẫu: khung ảnh bo góc bên trái (`src/assets/images/Viettel.jpg` do người làm đề tài cung cấp, phủ kín khung), bên phải: logo, "Đăng nhập", dòng hướng dẫn, ô email, ô mật khẩu (Hiện/Ẩn), nút đỏ "Đăng nhập bằng mật khẩu" (cao 58 px, bo 8 px), "Quên mật khẩu?", chân trang "© 2026 FinQuery". Màn hình hẹp ẩn ảnh |
| Logo | `Logo.tsx`: biểu tượng đỏ (cột số liệu + kính lúp) + chữ FinQuery; dùng ở trang đăng nhập và thanh trên |
| Font | FS Magistral (file trong `src/assets/fonts/FS Magistral/`, người làm đề tài cung cấp): Light/Book/Medium/Bold/ExtraBold khai báo bằng `@font-face`; dùng cho đề mục (`h1–h3`, logo, tiêu đề trang đăng nhập, "Các bước", "Lịch sử", tên tài liệu trong khung trích dẫn, nhãn đánh giá). Chữ thường vẫn Roboto. Đã kiểm hiển thị đủ dấu tiếng Việt ("Đăng nhập") |
| Khung trích dẫn | Ảnh trang bọc trong khung `.page-image` có thanh cuộn: ảnh rộng bằng khung, dài hơn thì cuộn dọc (trước đây bị cắt, không cuộn được) |

## 2. Kiểm

- `tsc -b`, `eslint`, `vite build` sạch; chụp trang đăng nhập bằng Chrome headless (1600×900).

## 3. Lưu ý

- FS Magistral là font có bản quyền: nếu giấy phép không cho phân phối lại thì không đưa thư mục `src/assets/fonts/` lên git công khai.
