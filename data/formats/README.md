# Hồ sơ cấu trúc tài liệu (QĐ-025)

Mỗi tệp JSON mô tả "chuẩn chung" của một loại tài liệu: căn cứ pháp lý, metadata bắt buộc, danh sách mục chuẩn (kèm các cách viết đã gặp trong tài liệu thật), vai trò của từng mục (đưa vào cơ sở dữ liệu số liệu, đưa vào chỉ mục tài liệu, hay bỏ qua) và quy tắc kiểm tra tính đầy đủ.

**Dùng để:** xác định tập dữ liệu cần xử lý · làm mốc tách đoạn · sinh metadata để lọc khi truy xuất · kiểm tra tài liệu mới nạp có thiếu mục bắt buộc không.

| Tệp | Loại tài liệu | Căn cứ pháp lý |
|---|---|---|
| `bctc.json` | Báo cáo tài chính | Thông tư 99/2025/TT-BTC (từ 01/01/2026), Thông tư 200/2014/TT-BTC (trước đó) |
| `bctn.json` | Báo cáo thường niên | Thông tư 96/2020/TT-BTC, Phụ lục IV |
| `dhdcd.json` | Tài liệu Đại hội đồng cổ đông | Luật Doanh nghiệp 2020, Thông tư 96/2020/TT-BTC |

**Vì sao báo cáo của các doanh nghiệp giống nhau:** cả ba loại đều có **mẫu do pháp luật quy định**, nên FPT, CMC, CTR hay doanh nghiệp niêm yết nào cũng theo cùng một khung. Khác biệt giữa các công ty chỉ nằm ở phần trình bày (thiết kế, thứ tự mục tuỳ chọn, mức chi tiết của thuyết minh).

⚠️ **Trạng thái: bản nháp.** Danh sách mục hiện dựng từ nhóm tài liệu đọc được chữ và từ mẫu biểu pháp lý. Sẽ hoàn thiện sau bước OCR bằng cách trích toàn bộ tiêu đề mục trong 314 tài liệu rồi gom nhóm các cách viết khác nhau. Nên đối chiếu thêm với báo cáo của một vài doanh nghiệp khác (ví dụ FPT, CMC) để kiểm chứng phần mục tuỳ chọn.
