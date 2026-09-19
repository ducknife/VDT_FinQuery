# Tiêu chí chấm VDT 2026 và cách FinQuery đáp ứng — v1.0

*Ngày: 2026-09-19 · Bản đầu tiên*

Nguồn: *Tiêu chí đánh giá Sáng kiến Công nghệ*, theo Phụ lục 04 — Tiêu chí đánh giá các giải thưởng VDT 2026 (chủ đề tài cung cấp).

## 1. Thang điểm (tổng 100)

| # | Tiêu chí | Tối đa | Các mức |
|---|---|---|---|
| 1 | **Tính hiệu quả kinh tế** | **30** | Cải tiến tác động lớn đến lợi nhuận: Top 20% → 30 · Top 21–25% → 20 · Top 26–30% → 10 |
| 2 | Tính mới | 20 | Chưa có ở Viettel → 10 · chưa có ở Việt Nam → 15 · chưa có trên thế giới → 20 |
| 3 | Tính ứng dụng | 20 | Cần ≥ 5 năm để áp dụng tại Viettel → 5 · 3–5 năm → 10 · 1–2 năm → 15 · **áp dụng được ngay → 20** |
| 4 | **Tính hoàn thiện** | **30** | Hoàn thiện 40–59% → 10 · 60–79% → 20 · **80–100% → 30** |

**Ưu tiên hàng đầu: tính hiệu quả và tính hoàn thiện** (hai tiêu chí nhiều điểm nhất, quyết định thứ hạng).
Tính ứng dụng: có thể áp dụng ngay tại Viettel / Viettel Digital.

## 2. Hệ quả với FinQuery

| Tiêu chí | FinQuery cần chứng minh gì | Bằng chứng cần chuẩn bị | Việc phải làm |
|---|---|---|---|
| **Hiệu quả kinh tế (30)** | Tiết kiệm **thời gian của phòng kế toán, tài chính** khi tra cứu và đối chiếu; rẻ hơn, nhanh hơn, đúng hơn text-to-SQL | **Đo thật:** 3–5 anh/chị ở phòng kế toán, tài chính làm cùng một bộ câu hỏi theo hai cách (tra cứu thủ công / dùng FinQuery), ghi thời gian và độ đúng. Quy đổi ra **giờ công và tiền tiết kiệm mỗi năm**, cộng **chi phí mỗi câu hỏi** (token, API) | Thiết kế buổi đo sớm (tuần 3–4) để có kết quả trước tuần 6. Nhật ký kiểm toán (QĐ-016) cung cấp số liệu chi phí |
| Tính mới (20) | Điểm mới là tổ hợp: truy vấn DB an toàn bằng function calling **+** RAG phân cấp với **reranker tiếng Việt** (ViRanker) **+** pipeline OCR cho BCTC scan tiếng Việt **+** trích dẫn đến trang **+** nhật ký kiểm toán, dành cho phòng kế toán, tài chính | Khảo sát nhanh: Viettel đã có công cụ tương tự chưa (hỏi nội bộ); ở Việt Nam đã có sản phẩm nào (tìm kiếm) | Làm khảo sát ở tuần 1–2. **Không tự nhận "chưa có ở Việt Nam" nếu chưa kiểm chứng** |
| Tính ứng dụng (20) | **Dùng được ngay** tại một phòng kế toán, tài chính của Viettel | Triển khai được bằng container/Kubernetes; đăng nhập SSO nối được LDAP/AD; tự host được mô hình để dữ liệu nội bộ không ra ngoài; nạp tài liệu mới không cần lập trình viên; mở rộng sang công ty khác nhờ tham số `ma_cong_ty` | Hạ tầng Docker, K8s (QĐ-019). Nêu các công ty con niêm yết của Viettel như những đơn vị có thể áp dụng tiếp |
| **Tính hoàn thiện (30)** | Sản phẩm **chạy trọn vẹn từ đầu đến cuối**, đạt ≥ 80% so với phạm vi đã cam kết | Danh sách tính năng cam kết cùng trạng thái; demo; kết quả đánh giá đầy đủ; tài liệu triển khai | **Chốt phạm vi cam kết vừa sức** và bám theo. Làm nhỏ mà xong vẫn hơn làm lớn mà dở dang |

## 3. Nguyên tắc rút ra
1. **Không mở rộng phạm vi nếu việc đó làm giảm tỷ lệ hoàn thiện.** Mọi tính năng mới phải qua câu hỏi: "có làm tụt phần trăm hoàn thiện không?"
2. **Hiệu quả kinh tế phải là số đo, không phải lời hứa.** Buổi đo với người dùng thật là việc **bắt buộc**, không phải tuỳ chọn.
3. **"Áp dụng ngay"** nghĩa là hệ thống triển khai được, có SSO, có tự host, có tài liệu vận hành. Đây cũng là lý do đưa Docker/K8s vào (QĐ-019).
