# Giao diện — v1.8: logo nguyên vẹn trên thanh biểu tượng, bớt thông tin kỹ thuật dưới câu trả lời

*Ngày: 2026-10-13 · Kế thừa [giao-dien_v1.7.md](giao-dien_v1.7.md)*

- **Logo bị cắt:** bản `logo-mark.png` (v1.7) cắt ngang trên dải chữ FINQUERY, nhưng thân nhân vật kéo dài xuống sau dải chữ nên bị cụt. Hình và chữ đan vào nhau, cắt kiểu nào cũng cụt. Thanh biểu tượng dùng lại logo đầy đủ (`logo-full.png`, cao 44 px); đã xoá `logo-mark.png`; `Logo` chỉ còn một kiểu. Đã chụp kiểm ở chế độ sáng và tối.
- **Dòng số đo dưới câu trả lời:** bỏ "prompt v…" và tên model (`anthropic/claude-sonnet-5-5`); còn thời gian, USD, token vào/ra. Hai thông tin này vẫn có trong `AskResult` lưu ở backend.
