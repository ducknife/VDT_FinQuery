# Giao diện — v1.5: lời chào "đồng chí", gợi ý câu hỏi chung, bỏ chữ CTR

*Ngày: 2026-10-13 · Kế thừa [giao-dien_v1.4.md](giao-dien_v1.4.md) · prompt v1.6*

- **Màn hình hội thoại mới:**
  - "Chào buổi sáng / trưa / chiều / tối, đồng chí [họ tên]!";
  - "Hôm nay đồng chí cần tra cứu thông tin gì?";
  - một câu giới thiệu nguồn (báo cáo tài chính, báo cáo thường niên, nghị quyết, công bố thông tin) và trích dẫn;
  - mục "Gợi ý cho đồng chí" với 6 câu: tăng trưởng doanh thu, nguyên nhân lợi nhuận tăng, ROE, cổ tức, kế hoạch theo nghị quyết ĐHĐCĐ, so sánh tiền giữa hai kỳ.
- **Bỏ chữ "CTR"** khỏi giao diện (trước đây "Hỏi về số liệu và tài liệu của CTR"); nội dung viết chung, khi thuyết trình người làm đề tài nói rõ dữ liệu là của CTR.
- **Họ tên:** `/api/v1/me` thêm `displayName` = họ + tên theo kiểu Việt Nam (`family_name` + `given_name` trong token). Thanh trên, lời chào, màn hình chờ duyệt dùng họ tên; không có thì dùng tên đăng nhập. `CurrentUser` thêm trường `displayName`.
- **Xưng hô trên giao diện:** "Đồng chí đã đánh giá: …" (thay "Bạn đã đánh giá"); màn hình chờ duyệt "Chào đồng chí …, tài khoản của đồng chí đang chờ…".
- **Kiểm:** `tsc -b`, `eslint`, `vite build` sạch; chụp màn hình hội thoại mới (core thử cổng 8091, tài khoản dev).
