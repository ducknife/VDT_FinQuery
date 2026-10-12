# Đánh giá — v1.6: câu trả lời nhánh số liệu, FinQuery so với text-to-SQL và LLM không công cụ

*Ngày: 2026-10-12, 23:20 · Kế thừa [danh-gia_v1.5.md](danh-gia_v1.5.md) (benchmark truy hồi giữ nguyên) · Kết quả: `data/eval/ket-qua/danh-gia-db_20261012-2308.md` / `.csv`*

## 1. Thiết kế (theo RQ3 của paper FinAI Data Assistant, arXiv 2510.14162)

Paper:
- 110 câu;
- thành công = trả về **đúng và đủ** giá trị được hỏi;
- đo độ chính xác, độ trễ, chi phí (token × đơn giá), trung bình ± độ lệch chuẩn;
- không dùng kiểm định cho RQ3; Welch's t-test chỉ dùng cho RQ1;
- không ghi model và prompt của đối chứng text-to-SQL.

FinQuery:
- **Bộ câu:** `data/eval/eval_dbquery.yml`, sinh bởi `data/eval/dung_bo_cau_hoi_db.py` từ `eval_test.yml`. Gồm 21 câu: 11 câu một số, 7 câu so sánh/tỷ lệ, 3 câu từ chối có số (X03, X04, X06). Mỗi câu có mục `cham` để máy chấm.
- **Ba cấu hình, cùng model Sonnet 5.5:**
  - **FINQUERY:** 3 tool SQL mẫu, LLM chỉ điền tham số.
  - **TEXT_TO_SQL:** LLM tự viết SQL, chạy qua tool `execute_sql`.
    - Câu SQL qua `SqlGuard`: chỉ một câu SELECT/WITH; không comment; không từ khoá ghi, `INTO`, `SLEEP`…
    - Chạy bằng user chỉ đọc `finq_reader`, giới hạn 5 s và 200 dòng.
    - **Cùng vòng agent, cùng bước trả lời, cùng bộ kiểm trích dẫn** (`QueryLoop.ask(question, prompt, tools)`).
    - Prompt cho LLM biết đúng những gì tool FinQuery đã mã hoá sẵn: nghĩa các cột, số quý và số luỹ kế, số trình bày lại, danh sách 123 mã.
  - **LLM_ONLY:** không công cụ, một lần gọi (như RQ1 của paper).
- **Chấm máy:** `DbAnswerScorer`.
  - số đồng chính xác, hoặc số tỷ làm tròn 2 chữ số;
  - đúng chuỗi phần trăm hoặc số lần;
  - từ chối thì không đưa số ngoài danh sách được phép.
- **Mã nguồn:** `modules/evaluation/baseline/` (`SqlGuard`, `ExecuteSql`, `BaselinePrompts`, `BaselineRunner`), `DbAnswerScorer`, test `DbAnswerEvaluationTest`. Kết nối chỉ đọc tách thành `common/db/ReadOnlyJdbc`, dùng chung cho FinQuery và đối chứng.

## 2. Kết quả chấm máy

| Cấu hình | Đúng | Một số | So sánh | Từ chối | Thời gian TB ± ĐLC | p50 | USD/câu | Tổng USD |
|---|---|---|---|---|---|---|---|---|
| **FINQUERY** | **21/21** | 11/11 | 7/7 | 3/3 | **8,2 ± 4,4 s** | **7,1 s** | 0,0505 | 1,06 |
| TEXT_TO_SQL | 14/21 | 9/11 | 4/7 | 1/3 | 10,2 ± 2,4 s | 10,4 s | 0,0498 | 1,05 |
| LLM_ONLY | 3/21 | 0/11 | 0/7 | 3/3 | 6,3 ± 0,8 s | 6,5 s | 0,0078 | 0,16 |

Toàn lượt khoảng 2,3 USD, cộng 0,07 USD chạy thử. Không có lỗi SQL hay câu bị chặn. Số không có trong nguồn: 0 ở cả ba cấu hình.

## 3. Soát tay 7 câu TEXT_TO_SQL bị chấm sai (trung thực: bộ chấm quá chặt ở nhiều câu)

| Câu | Thực tế | Phân loại |
|---|---|---|
| D02 | SQL lấy đúng số, nhưng câu trả lời viết "587.417.356 381" (mất một dấu chấm) | lỗi viết số khi LLM tự chép |
| C05 | "2,4268 lần" (đúng, không làm tròn) | đúng; bộ chấm đòi "2,43" |
| C07 | "84,58694%" (đúng, không làm tròn) | đúng; bộ chấm đòi "84,59%" |
| C06 | "-30,3873%", đưa cả hai mã HTK (M140 thuần, M141 giá gốc) mà không chọn; viết "giảm -185…" | đúng một phần, trình bày lủng củng |
| D11 | "tiền mặt" → mã 111 "Tiền" (623,25 tỷ) thay vì mã 110 (1.123,25 tỷ) | **sai chỉ tiêu** (FinQuery cũng sai đúng ca này ở lượt chạy 21:43) |
| X06 | nói chưa có số cả năm; đưa số thật Q1, Q2 và 6 tháng 2026; không ngoại suy | đúng; danh sách số được phép quá hẹp |
| X03 | nói chưa có quý 3/2026; đưa số thật của các kỳ đã có | đúng; như trên |

**Chấm lại theo cách người đọc:**
- TEXT_TO_SQL đúng khoảng **18–19/21**: sai D11, D02 lỗi viết số, C06 lủng củng.
- FINQUERY vẫn **21/21**.

**LLM_ONLY:**
- 15/18 câu **từ chối trung thực** ("không có số liệu đáng tin cậy").
- 3 câu **đưa số từ trí nhớ, đều sai**:
  - D01: "≈ 7.900 tỷ … 5.800 tỷ", thật là 12.609,52 tỷ;
  - C01: "gần 7.000 tỷ";
  - C02: "≈ 11%", thật là 7,01%.
- Không trả lời đúng con số nào trong 18 câu.

## 4. Kết luận (để trình bày, nói thẳng)

1. **Không có dữ liệu thì LLM không trả lời được.** Model tốt (Sonnet 5.5) phần lớn từ chối; khi đoán thì sai xa (lệch gần một nửa doanh thu).
2. **FinQuery đúng 21/21**. Nhanh hơn text-to-SQL khoảng 2 s/câu (p50 7,1 s so với 10,4 s), chi phí ngang nhau.
3. **Text-to-SQL với model mạnh và mô tả schema tốt cũng đúng phần lớn số liệu.** Lỗi của nó **không phải lấy sai dữ liệu** mà là:
   - **trình bày không ổn định:** không làm tròn, viết sai dấu nhóm số, đưa hai mã mà không chọn;
   - **chọn sai chỉ tiêu** ở cách gọi đời thường;
   - **phải tin vào SQL do LLM viết:** cần thêm `SqlGuard` và user chỉ đọc mới chạy an toàn được.

   Khác với ấn tượng "text-to-SQL thất bại nhiều" từ biểu đồ của paper.
4. **Lợi thế thật của FinQuery** nằm ở **tính tất định và kiểm soát**:
   - Java làm tròn và viết số thống nhất;
   - kỳ quý, luỹ kế, trình bày lại được quy ước một lần trong code;
   - LLM không bao giờ cầm SQL;
   - đúng 21/21 với độ trễ thấp hơn.
5. **Chi phí FinQuery chưa thấp hơn** vì danh sách 123 mã trong system prompt (khoảng 22.000 token vào mỗi câu). Rút gọn danh sách là cách giảm chi phí rõ nhất.
6. **Giới hạn:** 21 câu là ít; mỗi cấu hình chỉ chạy một lượt (D11 của FinQuery từng sai ở lượt trước). Muốn kết luận chắc cần chạy lặp 2–3 lượt.

## 5. Việc tiếp theo

- **Người làm đề tài soát** phần 3 của file kết quả và quyết bộ chấm v2: chấp nhận số làm tròn khớp đáp án; câu từ chối cho phép số có trong bằng chứng tool. Chạy lại chấm trên CSV có sẵn, **không tốn tiền**.
- Chạy lặp thêm 1–2 lượt để có độ dao động: khoảng 2 USD mỗi lượt.
- Rút gọn danh sách mã trong prompt để giảm chi phí FinQuery.
- Prompt v1.4: "tiền mặt" = mã 110.
