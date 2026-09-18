# Phạm vi đề tài — v1.0

*Ngày: 2026-09-17 · Bản đầu tiên*

## 1. Đề tài

**FinQuery-RAG** là hệ thống hỏi đáp tài chính kết hợp hai cách lấy dữ liệu: truy vấn cơ sở dữ liệu một cách an toàn, và truy xuất dựa trên tài liệu một cách hiệu quả. Đây là đề tài VDT giai đoạn 2 (giai đoạn 1 là AI Agent SRE).

- **Mục tiêu.** Xây dựng một trợ lý tài chính an toàn, chuyên xử lý dữ liệu tài chính có cấu trúc và các báo cáo doanh nghiệp.
- **RAG tài chính.** Áp dụng chuẩn hoá truy vấn, chuyển bảng sang JSON, tìm kiếm vector và tái xếp hạng phân cấp.
- **Đánh giá.** So sánh với text-to-SQL và RAG cơ bản theo 5 tiêu chí: độ chính xác, độ chính xác của trích dẫn, độ trễ, lượng token và chi phí API.
- **Đóng góp dự kiến.** Một trợ lý tài chính đáng tin cậy và hiệu quả, kết hợp truy vấn cơ sở dữ liệu với phân tích báo cáo dựa trên bằng chứng.

## 2. Doanh nghiệp mẫu

**Tổng Công ty Cổ phần Công trình Viettel (Viettel Construction)**
- Mã cổ phiếu: **CTR** (niêm yết trên HOSE từ tháng 2/2022, trước đó giao dịch trên UPCoM/HNX).
- Trong một số tài liệu nội bộ, công ty tự viết tắt tên mình là **VCC**.
- Nguồn dữ liệu: website chính thức viettelconstruction.com.vn (xem [du-lieu_v1.0.md](du-lieu_v1.0.md)).
- Khoảng thời gian: **2019 → 2026** (năm 2026 có số liệu đến bán niên).

## 3. Phạm vi nghiệp vụ: tài chính doanh nghiệp, 6 mảng

| Mảng | Nội dung | Nguồn chính |
|---|---|---|
| **A. Huy động & cơ cấu nguồn vốn** | Nợ/VCSH, hệ số đòn bẩy, khả năng trả lãi vay. Chi tiết khoản vay (lãi suất, kỳ hạn, tài sản bảo đảm, bên cho vay). Phát hành cổ phiếu, trái phiếu, ESOP | BCTC (bảng CĐKT, thuyết minh), NQ ĐHĐCĐ, CBTT phát hành |
| **B. Quản lý dòng tiền** | Lưu chuyển tiền từ hoạt động kinh doanh/đầu tư/tài chính, dòng tiền tự do. Vốn lưu động: phải thu, tồn kho, phải trả, chu kỳ chuyển đổi tiền mặt | BCTC (LCTT, CĐKT) |
| **C. Chi phí & hiệu quả** | Cơ cấu chi phí, biên lợi nhuận gộp/ròng, ROE, ROA, vòng quay tài sản, phân tích theo mảng kinh doanh | BCTC (KQKD, thuyết minh bộ phận), BCTN |
| **D. Đầu tư & phân bổ vốn** | CAPEX, đầu tư vào công ty con/liên kết, thanh lý, thoái vốn | BCTC (LCTT, thuyết minh), NQ HĐQT |
| **E. Kế hoạch & kết quả thực hiện** | Kế hoạch được ĐHĐCĐ thông qua so với kết quả thực hiện, và phần giải trình chênh lệch | NQ ĐHĐCĐ + BCTC + BCTN/báo cáo HĐQT, Ban điều hành |
| **F. Phân phối lợi nhuận** | Cổ tức (tỷ lệ, hình thức, thời điểm), lợi nhuận giữ lại, trích lập quỹ | NQ/tờ trình ĐHĐCĐ, CBTT cổ tức, thuyết minh VCSH |

**Nằm ngoài phạm vi:** nhân sự, quản trị nội bộ, giao dịch cổ phiếu của người nội bộ, và **điều khoản ràng buộc (covenant) của khoản vay**, vì doanh nghiệp Việt Nam hầu như không công bố thông tin này. Covenant sẽ được dùng làm *câu hỏi bẫy* trong bộ đánh giá, để kiểm tra hệ thống có trả lời "không xác định" đúng lúc hay không.

## 4. Hai paper nền tảng và cách áp dụng

| Paper | Ý chính | Áp dụng trong FinQuery |
|---|---|---|
| **FinAI Data Assistant** (Kim và cộng sự, CIKM'25) | LLM chỉ chọn hàm và điền tham số; SQL là mẫu đã tham số hoá và kiểm định trước. Cách này nhanh hơn, rẻ hơn và đáng tin cậy hơn text-to-SQL | Nhánh **truy vấn DB**: thư viện hàm SQL theo mảng A–F. Các chỉ số (ROE, D/E, CCC, FCF…) được tính bằng code Java, không để LLM tính |
| **Hierarchical Reranker** (Lee & Hong) | Tiền truy xuất (chuẩn hoá, trích từ khoá, table-to-json), rerank 2 tầng (lấy 100 rồi 20), chia đôi và hợp nhất khi ngữ cảnh vượt 64k token | Nhánh **RAG** trên BCTN, thuyết minh BCTC, nghị quyết, CBTT |

**Những điểm phải điều chỉnh cho bối cảnh Việt Nam:**
- Từ điển viết tắt tiếng Việt (LNST, VCSH, HĐQT, ĐHĐCĐ, TSĐB…) và quy đổi đơn vị (tỷ/triệu/nghìn đồng).
- Phân biệt **BCTC hợp nhất và BCTC riêng**. Mặc định dùng hợp nhất.
- Ánh xạ tên công ty sang mã cổ phiếu bằng bảng tra, không dựa vào trí nhớ của LLM. Kết quả RQ2 của paper FinAI chỉ đúng với thị trường Mỹ.
- Đổi nhà cung cấp LLM (không dùng OpenAI). Nhà cung cấp cụ thể **chưa chốt**.

**Đóng góp riêng của đề tài:** một bộ định tuyến ba nhánh **DB / RAG / KẾT HỢP**. Mảng E là trường hợp điển hình cần kết hợp: kế hoạch lấy từ nghị quyết (RAG), thực hiện lấy từ DB, giải trình lấy từ BCTN (RAG).

**Bước lược bỏ có căn cứ:** Bảng 1 của paper Hierarchical Reranker cho thấy *diễn giải lại truy vấn* và *HyDE* làm giảm NDCG@20, nên hai bước này đứng đầu danh sách cắt giảm.
