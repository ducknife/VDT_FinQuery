# Nhật ký & tài liệu dự án FinQuery-RAG

Thư mục này ghi lại **đã làm gì, vì sao, kết quả ra sao** trong suốt dự án. Mục đích là khi kết thúc có sẵn nguyên liệu để viết báo cáo cuối kỳ VDT, không phải nhớ lại.

## Quy ước phiên bản

- Mỗi tài liệu đặt tên theo mẫu `<chu-de>_v<lon>.<nho>.md`, ví dụ `du-lieu_v1.0.md`.
- **Không sửa đè nội dung của bản cũ.** Khi cần thay đổi thì chép sang bản mới (`v1.0` → `v1.1`), sửa trong bản mới, và ghi phần *Thay đổi so với bản trước* ở đầu file. Bản cũ giữ nguyên để truy vết. Chỉ được sửa bản cũ khi là lỗi chính tả.
- Tăng số **nhỏ** (`v1.1`, `v1.2`) khi bổ sung hoặc chỉnh sửa. Tăng số **lớn** (`v2.0`) khi đổi hướng, ví dụ đổi phạm vi dữ liệu hay đổi kiến trúc.
- Riêng **nhật ký** thì ghi nối tiếp theo ngày trong bản hiện hành. Mỗi khi hết một tuần trong kế hoạch 6 tuần thì mở bản mới.

## Danh mục (bản hiện hành)

| Tài liệu | Nội dung | Bản hiện hành |
|---|---|---|
| Nhật ký | Việc đã làm theo từng ngày | [nhat-ky_v1.0.md](nhat-ky_v1.0.md) |
| Phạm vi đề tài | Mục tiêu, 6 mảng A–F, cách 2 paper được áp dụng | [pham-vi_v1.0.md](pham-vi_v1.0.md) |
| Dữ liệu | Nguồn, cấu trúc thư mục, thống kê, chất lượng, lỗ hổng | [du-lieu_v1.0.md](du-lieu_v1.0.md) |
| Công nghệ | Stack và công cụ, lý do chọn | [cong-nghe_v1.4.md](cong-nghe_v1.4.md) |
| Luồng hỏi đáp | Hai nhánh DB/RAG, bảng ánh xạ mã, thiết kế tool, dẫn chứng, ba chốt kiểm | [luong-hoi-dap_v1.0.md](luong-hoi-dap_v1.0.md) |
| Lược đồ CSDL | Năm bảng lõi, năm tiêu chí loại dòng, sổ công thức và cách chống bịa, bốn lỗi lược đồ đã mắc | [luoc-do-db_v1.0.md](luoc-do-db_v1.0.md) |
| Tiền xử lý | OCR bằng mô hình thị giác, tham số kết xuất ảnh (DPI, JPEG), hai hạn giờ, luật làm sạch, số liệu chunk, thiết kế lớp | [tien-xu-ly_v1.0.md](tien-xu-ly_v1.0.md) |
| Làm sạch | Luật làm sạch sau OCR đã chốt (NFC, hàng rào code, &nbsp;, khoảng trắng), luật hoãn, số đo trên kho | [lam-sach_v1.0.md](lam-sach_v1.0.md) |
| Quyết định | Sổ quyết định (QĐ-xxx): bối cảnh, lựa chọn, lý do | [quyet-dinh_v1.14.md](quyet-dinh_v1.14.md) |
| Kế hoạch | Bốn khối nền, lộ trình, thứ tự cắt giảm khi trễ | [ke-hoach_v1.1.md](ke-hoach_v1.1.md) |
| Kế hoạch đánh giá | Bộ test T1–T9, cách đo độ trễ/token/chi phí, chiến lược 3 lớp, tác động khi đổi mô hình | [danh-gia_v1.3.md](danh-gia_v1.3.md) |
| Tiêu chí VDT | Thang điểm VDT 2026 và cách FinQuery đáp ứng | [tieu-chi-vdt_v1.0.md](tieu-chi-vdt_v1.0.md) |
| Học tập | Tinh thần vừa học vừa làm, cách làm việc với mentor, phân rã đệ quy, bản đồ kỹ năng, nhật ký bài học | [hoc-tap_v1.1.md](hoc-tap_v1.1.md) |
| Từ điển tài chính | Tài liệu, thuật ngữ A–F, mã chỉ tiêu BCTC, công thức, bẫy, viết tắt | [tu-dien-tai-chinh_v1.2.md](tu-dien-tai-chinh_v1.2.md) |

Khi tạo bản mới, nhớ cập nhật cột *Bản hiện hành* ở bảng trên.
