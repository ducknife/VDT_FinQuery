# Nhánh số liệu (tool SQL của agent) — v1.0

*Ngày: 2026-10-12 tối · Thiết kế gốc: [luong-hoi-dap_v1.0.md](luong-hoi-dap_v1.0.md) mục 3–6 · Quyết định: QĐ-037, QĐ-038 ([quyet-dinh_v1.21.md](quyet-dinh_v1.21.md)) · Prompt: [prompt_v1.3.md](prompt_v1.3.md)*

## 1. Nguyên tắc

Theo paper FinAI Data Assistant (Kim và cộng sự, CIKM'25):

```
TA VIẾT SẴN   câu SQL là HẰNG SỐ trong Java, chỉ có dấu ?
LLM ĐIỀN      mã chỉ tiêu, năm, kỳ, phạm vi (chỉ điền giá trị, không viết SQL)
JAVA KIỂM     mã sai / kỳ tương lai / trước 2017 → TỪ CHỐI, không chạm DB
JAVA TÍNH     chênh lệch, %, tỷ lệ, tỷ số, bình quân → LLM không tự tính số nào
```

Ba lớp an toàn:
1. **SQL hằng số và PreparedStatement.**
2. **Tham số đã kiểm.**
3. **Kết nối chỉ đọc:**
   - Có cấu hình `DB_READONLY_*`: dùng user MySQL chỉ có SELECT trên 3 bảng.
   - Chưa có: dùng giao dịch `READ ONLY`, MySQL từ chối mọi lệnh ghi.

User chỉ đọc chưa tạo: hệ thống chặn việc Claude ghi mật khẩu vào `.env`; người làm đề tài tự tạo theo câu SQL ở mục 6.

## 2. Mã nguồn: `modules/agent/tools/financial/`

| File | Việc |
|---|---|
| `FigureQueryRepository` | 4 câu SQL hằng số: S1 tra một số (kèm số cũ đã bị trình bày lại), S2 kỳ mới nhất, S3 các kỳ đang có, từ điển chỉ tiêu. Pool riêng cho user chỉ đọc, không đăng ký làm bean |
| `IndicatorCatalog` | 123 mã chỉ tiêu: kiểm mã, gợi ý mã gần đúng, sinh danh sách gửi **một lần** trong system prompt |
| `PeriodCode`, `ReportPeriod`, `PeriodInput` | LLM điền `year` + `Q1..Q4/H1/M9/FY`; Java đổi ra (từ ngày, đến ngày), tính đầu kỳ, số ngày |
| `Scope`, `FigureRef`, `VnNumber` | phạm vi; tham chiếu một số; viết số kiểu Việt Nam (đồng chính xác, tỷ đồng làm tròn, %, lần, ngày) |
| `FigureLookup` | chốt kiểm và tra một số rồi gói thành bằng chứng D#. **Quý N của chỉ tiêu thời kỳ mà kho chỉ có luỹ kế** (lưu chuyển tiền tệ): Java tính luỹ kế N − luỹ kế N−1, trích cả hai nguồn |
| `GetFinancialFigures` | tool `get_financial_figures`: chỉ tiêu × kỳ × phạm vi (tối đa 4 × 8 × 2); từ 2 kỳ trở lên thì tự tính thay đổi giữa các kỳ liền nhau |
| `RelateTwoFigures` | tool `relate_two_figures`: RATIO (a/b lần), SHARE (a chiếm % b), CHANGE (a so với b) |
| `FinancialRatio`, `ComputeFinancialRatio` | tool `compute_financial_ratio`: 15 tỷ số theo mục 4 của [tu-dien-tai-chinh_v1.2.md](tu-dien-tai-chinh_v1.2.md), bình quân = (đầu kỳ + cuối kỳ) / 2 |

Sửa ở chỗ khác:
- `EvidenceStore`: thêm `figure(key)` để chống trùng D# và `addComputed()` cho bằng chứng do Java tính.
- `Evidence.source()`: bằng chứng do Java tính hiện "Java tính từ các số liệu đã dẫn".
- `AgentConfig`: đăng ký 3 tool.
- `QueryLoop`: system prompt cộng danh sách mã chỉ tiêu; câu hỏi gửi kèm "Latest period in the figures database".

## 3. Dữ liệu (truy vấn ngày 12/10/2026)

| Thông tin | Giá trị |
|---|---|
| Số dòng hiệu lực | 6.521, gồm 123 mã, từ 31/12/2017 đến 30/06/2026 |
| Hợp nhất / riêng | 3.370 / 3.151 |
| Số cũ đã bị thay (trình bày lại) | 259 dòng, vẫn giữ |
| Kết quả kinh doanh (B02) | có số riêng quý và số luỹ kế |
| Lưu chuyển tiền tệ (B03) | chỉ có luỹ kế |

**DB không cần bổ sung bảng hay chỉ mục nào** để trả lời 18 câu số liệu.

**Phát hiện khi test.** Lợi ích cổ đông không kiểm soát (mã 429):
- 50,9 triệu đồng tại 31/12/2022;
- **0** tại 31/12/2023, rồi báo cáo thôi in dòng này;
- LNST của cổ đông không kiểm soát bằng 0 từ 2023, và LNST công ty mẹ bằng đúng LNST hợp nhất.

Vì vậy khi tính ROE hợp nhất, dòng 429 vắng mặt trong khi bảng cân đối cùng ngày vẫn có số thì **coi bằng 0 và ghi rõ trong bằng chứng**. Kết quả ROE hợp nhất 2025 = **30,50%**, khớp từ điển.

## 4. Kiểm phần Java bằng đáp án vàng (không qua LLM, không tốn tiền)

- Test: `FinancialToolsGoldTest` (`-Ddbtools=true`). Gọi thẳng 3 tool với tham số đúng của từng câu.
- Kết quả: **3/3 nhóm xanh**.
  - **D01–D11:** đúng từng đồng, kể cả D05 dòng tiền âm, D07 doanh thu riêng một quý, D09 số trình bày lại cùng số gốc.
  - **C01–C07:** 10,55%; 7,01%; 74,75%; tăng 54,88 tỷ (20,56%); 2,43 lần; giảm 185,51 tỷ (30,39%); 84,59%.
  - **Biên:**
    - X03, X06 (kỳ chưa công bố) và X04 (trước 2017) trả về `NOT_AVAILABLE`.
    - Mã lạ trả về `UNKNOWN_INDICATOR`.
    - Dòng tiền riêng quý 2/2025 tính bằng 6 tháng − quý 1.
    - ROE và CCC năm 2025 tính được.

Trong lúc viết đã sửa một lỗi: % của số giảm bị gắn hai dấu trừ. Nay % luôn dương, chiều thay đổi nói bằng chữ "tăng"/"giảm".

## 5. Chạy thật cả luồng: Sonnet 5.5, prompt v1.3, 8 câu

Log: `data/eval/ket-qua/agent-thu_20261012-2143*.md`, `-2144*.md`.

| Câu | Kết quả | Tool | Thời gian | USD |
|---|---|---|---|---|
| D03 nợ phải trả kỳ gần nhất | ✓ 7.435,18 tỷ tại 30/06/2026 | get | 8,4 s | 0,050 |
| D07 doanh thu riêng, chỉ quý 2/2026 | ✓ 3.773,81 tỷ | get | 6,8 s | 0,050 |
| D09 doanh thu 2019 trình bày lại | ✓ số trình bày lại và số gốc | get | 8,2 s | 0,053 |
| D11 "tiền mặt" cuối quý trước | ⚠ đúng kỳ 31/03/2026, nhưng chọn mã 111 "Tiền" (623,25 tỷ). Đáp án vàng: mã 110 "Tiền và các khoản tương đương tiền" (1.123,25 tỷ). Câu trả lời nói rõ mình dùng chỉ tiêu nào | get | 11,6 s | 0,061 |
| C01 tăng trưởng doanh thu | ✓ 10,55% | get, 2 kỳ | 9,0 s | 0,054 |
| C03 nợ / tổng tài sản | ✓ 74,75% | compute_financial_ratio | 7,3 s | 0,053 |
| C05 dòng tiền gấp mấy lần LNST | ✓ 2,43 lần | relate_two_figures | 7,4 s | 0,055 |
| X06 doanh thu cả năm 2026 | ✓ nói chưa có, đưa số 6 tháng, không ngoại suy | get | 11,2 s | 0,056 |

- **Đúng 7/8.** 18/18 câu trích ✓, không có số nào lệch nguồn, agent chọn đúng tool ở cả 8 câu.
- **Thời gian 6,8–11,6 s/câu**, nhanh gấp 2–3 lần nhánh tài liệu (22–27 s) vì không phải truy hồi và rerank. Tool chạy 6–56 ms.
- **Chi phí khoảng 0,05 USD/câu, 80% nằm ở vòng agent.** Danh sách 123 mã trong system prompt khoảng 10.000 token mỗi lần gọi, × 2 lần. Hướng giảm (chưa làm): rút gọn tên trong danh sách, hoặc chỉ gửi danh sách khi câu hỏi có dấu hiệu số liệu.

**D11 là ca chọn chỉ tiêu theo cách gọi đời thường.** "Tiền mặt" trong cách nói của người làm tài chính thường là "tiền và các khoản tương đương tiền" (mã 110). Cách sửa đề xuất (chưa làm, cần duyệt): thêm vào prompt quy tắc "tiền mặt" = mã 110, và nêu thêm mã 111 nếu người hỏi muốn tách. Ghi thành prompt v1.4 kèm ca này.

## 6. Việc còn lại

- Người làm đề tài tạo user chỉ đọc:

  ```sql
  CREATE USER 'finq_reader'@'localhost' IDENTIFIED BY '<mật khẩu>';
  GRANT SELECT ON FINQUERY.financial_figures TO 'finq_reader'@'localhost';
  GRANT SELECT ON FINQUERY.financial_items   TO 'finq_reader'@'localhost';
  GRANT SELECT ON FINQUERY.documents         TO 'finq_reader'@'localhost';
  ```

  rồi thêm `DB_READONLY_USERNAME`, `DB_READONLY_PASSWORD` vào `.env`.
- Ca D11 "tiền mặt" (prompt v1.4).
- Giảm token của danh sách mã.
- Đánh giá câu trả lời trên đủ 50 câu (cả hai nhánh), so với RAG cơ bản và text-to-SQL.
