# Giao diện — v1.9: câu gợi ý lấy từ tập kiểm thử

*Ngày: 2026-10-13 · Kế thừa [giao-dien_v1.8.md](giao-dien_v1.8.md)*

- **Câu gợi ý (màn hình hội thoại mới):** thay 3 câu tự đặt bằng 4 câu trong tập kiểm thử (`data/eval`). Cả 4 câu đã được FinQuery trả lời đúng khi chấm. Mỗi câu đi qua một nhánh khác nhau, nên khi trình diễn bấm lần lượt là thấy đủ các nhánh:

  | Mã | Câu | Nhánh | Công cụ đã gọi khi chấm |
  |---|---|---|---|
  | C01 | Doanh thu thuần hợp nhất năm 2025 tăng bao nhiêu phần trăm so với năm 2024? | Chỉ CSDL | `get_financial_figures` |
  | V04 | Cổ tức năm 2024 được chi trả theo tỷ lệ bao nhiêu và ngày đăng ký cuối cùng là ngày nào? | Chỉ tài liệu | `search_documents` |
  | M01 | Năm 2025 công ty có hoàn thành kế hoạch lợi nhuận sau thuế hợp nhất không? | Cả hai: kế hoạch lấy từ tài liệu, số thực hiện lấy từ CSDL | `get_financial_figures`, `search_documents` |
  | M03 | So sánh LNST hợp nhất quý 2/2024 với quý 3/2025. | Cả hai, kèm phép tính chênh lệch (QĐ-041) | `get_financial_figures`, `relate_two_figures`, `search_documents` |

- **Tiêu chí chọn:** chỉ tiêu dễ hiểu, không cần giải thích thuật ngữ (doanh thu, lợi nhuận, cổ tức; viết tắt quen thuộc như LNST thì giữ). Không có tên doanh nghiệp trong câu. B02 (trạm BTS) đúng nhưng không chọn vì là thuật ngữ chuyên ngành.
- Đã build lại container `finq-frontend` và kiểm tra bundle đang phục vụ có câu mới.
