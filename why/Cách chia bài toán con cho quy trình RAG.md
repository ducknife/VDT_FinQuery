Cách chia bài toán con cho quy trình
tiền xử lí -> xử lí -> đánh giá

*****KHI NÀO CHIA*****

=> 0. Điều kiện cần: gọi tên được input và output CỤ THỂ
   ("một trang PDF quét 170 DPI" -> "markdown có bảng", chứ không phải "tài liệu" -> "dữ liệu")
   Chưa gọi tên được thì chưa chia nổi, phải làm rõ trước

=> 1. Khi có input và output cụ thể => áp dụng ngay preprocessing -> processing -> evaluate

=> 2. Nếu có từ "Và" hoặc "Rồi" — hai từ này báo HAI KIỂU CHIA KHÁC NHAU:
   - "RỒI" = bước sau cần kết quả bước trước => chia DỌC, dây chuyền, thứ tự bắt buộc
     vd: dựng ảnh RỒI gọi model RỒI ghi tệp
   - "VÀ"  = hai việc không phụ thuộc nhau   => chia NGANG, nhánh độc lập, chạy song song được
     vd: nạp BCTC VÀ nạp nghị quyết ĐHĐCĐ
   (phân biệt được thì biết chỗ nào đặt nhiều luồng, chỗ nào là nút thắt)

=> 3. Khi có sự thay đổi về mặt trạng thái của dữ liệu => tách bước, VÀ LƯU LẠI Ở RANH GIỚI ĐÓ
   PDF quét -> ảnh JPEG -> markdown/trang -> bản ghép -> chunk -> vector -> câu trả lời
   Mỗi trạng thái là một chỗ lưu được, lưu được thì chạy tiếp được
   (nhờ lưu ở mức markdown từng trang nên tắt máy giữa chừng vẫn không mất công đã làm)

*****KHI NÀO THÔI CHIA*****

=> Dừng khi bước con thoả CẢ BA:
   - làm xong trong khoảng nửa ngày
   - chạy ra kết quả NHÌN THẤY ĐƯỢC (một con số, một tệp, một dòng log)
   - sai thì biết ngay sai ở bước này, không phải mò cả hệ thống

*****"ĐÁNH GIÁ" Ở MỨC CON NGHĨA LÀ GÌ*****

=> Mức trên cùng: đánh giá = bộ test T1-T9, chạy vài tiếng
=> Mức bước con: đánh giá = MỘT PHÉP KIỂM RẺ TIỀN, không phải bộ đánh giá đầy đủ
   - dựng ảnh      -> kích thước và dung lượng nằm trong khoảng mong đợi
   - OCR một trang -> năm đẳng thức cân đối khớp
   - ghép toàn văn -> số trang ghép đúng bằng số trang gốc
   - cắt đoạn      -> không đoạn nào vượt giới hạn token, không con số nào bị cắt đôi
   - nhúng vector  -> số vector đúng bằng số chunk, đúng số chiều

=> Lý do bắt buộc phải có: hai cái bẫy tệ nhất đã gặp (mã chỉ tiêu 136 đổi nghĩa giữa TT200 và TT99,
   ROE tính trên số bình quân chứ không phải số cuối kỳ) đều là lỗi SAI IM LẶNG.
   Không kiểm ở từng bước thì lỗi trôi tới câu trả lời cuối mới lộ, lúc đó không biết hỏng ở đâu.

*****CÁI BẪY KHI ÁP DỤNG*****

=> Chia đệ quy dễ dẫn tới làm xong TOÀN BỘ tiền xử lí rồi mới đụng tới xử lí,
   đến lúc đó mới phát hiện cách cắt đoạn không hợp với reranker -> làm lại từ đầu
=> Cách tránh: đi MỘT LÁT CẮT DỌC MỎNG trước (một tài liệu, một câu hỏi, chạy suốt từ nạp tới trả lời),
   rồi mới quay lại đào sâu từng nhánh
=> Nói gọn: đệ quy để HIỂU bài toán, lát cắt dọc để KIỂM CHỨNG
