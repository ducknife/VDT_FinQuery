# Từ điển tài chính cho FinQuery — v1.2

*Ngày: 2026-09-21 · Thay cho [v1.1](tu-dien-tai-chinh_v1.1.md)*

## Thay đổi so với v1.1
- ⚠️ **Cảnh báo lớn ở mục 3:** đối chiếu BCTC thật của CTR cho thấy **mã chỉ tiêu đã thay đổi giữa các năm**. Báo cáo bán niên 2026 dùng tên **"Báo cáo tình hình tài chính"** (không còn gọi là "Bảng cân đối kế toán") và bộ mã khác với báo cáo năm 2023. Xem mục 3.0 mới.
- Hệ quả thiết kế: khoá của bảng số liệu **không thể chỉ là (mẫu biểu, mã)**, mà phải gắn thêm **chế độ trình bày theo kỳ**, hoặc quy về **mã nội bộ** của hệ thống.

## Thay đổi của v1.1 so với v1.0 (giữ lại để tham khảo)
- Thêm **mục 0: sáu ý cần nhớ**, đọc một trang là đủ để làm đề tài. Từ mục 1 trở đi là **phần tra cứu**, không cần học thuộc.
- Mục 1: đổi tên cột "Nhánh trong FinQuery" thành "Dùng thế nào trong FinQuery"; viết lại hai dòng *hợp nhất/riêng* và *kiểm toán/soát xét/tự lập* cho dễ hiểu.

---

## 0. Sáu ý cần nhớ (đọc phần này là đủ)

> Không cần thuộc thuật ngữ. Khi gặp từ lạ trong danh mục câu hỏi hay trong code, hãy tra từ mục 1 trở đi, hoặc hỏi sư phụ.

1. **BCTC = 3 bảng số + phần chữ.** Ba bảng số (cân đối kế toán, kết quả kinh doanh, lưu chuyển tiền tệ) → lưu vào **DB**, truy vấn bằng function calling. Phần chữ (thuyết minh) → **RAG**.
2. **Các tài liệu khác đều là chữ** (báo cáo thường niên, nghị quyết, công bố thông tin, công văn giải trình) → **RAG**. Riêng vài con số trong đó (kế hoạch năm, cổ tức) được nhập tay vào DB.
3. **Hợp nhất và riêng:** hợp nhất = cả công ty mẹ và các công ty con, coi như một khối; riêng = chỉ công ty mẹ. **Người dùng không nói rõ thì dùng hợp nhất**, và câu trả lời ghi rõ đang dùng loại nào.
4. **Ba kỳ báo cáo, ba mức tin cậy:** quý (công ty tự lập) < bán niên (có kiểm toán viên soát xét) < năm (được kiểm toán đầy đủ). Hỏi số cả năm thì lấy báo cáo năm, **không cộng 4 quý**.
5. **Lũy kế:** trên BCTC quý, bảng lưu chuyển tiền tệ thường là số cộng dồn từ đầu năm. Muốn số riêng một quý thì phải lấy quý này trừ quý trước.
6. **Chỉ số (ROE, nợ/vốn chủ, biên lợi nhuận…) = công thức tính từ vài dòng số trên BCTC.** Java tính theo bảng công thức ở mục 4, không để LLM tự tính.

Ngoài sáu ý này, chỉ cần biết mỗi mảng hỏi về gì: **A** vay và vốn · **B** dòng tiền · **C** chi phí và lợi nhuận · **D** đầu tư tài sản · **E** kế hoạch so với thực hiện · **F** chia cổ tức.

---

## Phần tra cứu (không cần đọc hết)

**Dùng để làm gì:**
1. Để người không học tài chính đọc hiểu được danh mục câu hỏi, schema DB và tài liệu của CTR.
2. Làm **ngữ cảnh chuẩn** khi thiết kế: tên chỉ tiêu, công thức, viết tắt. Sau này có thể dùng lại cho từ điển chuẩn hoá truy vấn (bước tiền truy xuất) và cho mô tả của các hàm function calling.

> ⚠️ Mã chỉ tiêu ở mục 3 theo mẫu biểu của **Thông tư 200/2014/TT-BTC** (BCTC riêng) và **Thông tư 202/2014/TT-BTC** (BCTC hợp nhất). Trước khi dùng làm khoá DB **phải đối chiếu với BCTC thật của CTR**, vì mỗi doanh nghiệp có thể thêm hoặc bỏ dòng.

---

## 1. Các loại tài liệu

| Tài liệu | Hiểu nôm na | Dạng nội dung | Dùng thế nào trong FinQuery |
|---|---|---|---|
| **BCTC** (báo cáo tài chính) | "Sổ khám sức khoẻ" của công ty, lập theo quý, bán niên và năm | 3 bảng số + thuyết minh | Bảng số → **DB**; thuyết minh → **RAG** |
| • **Bảng cân đối kế toán** (CĐKT, mẫu B01) | **Ảnh chụp tại một ngày**: công ty *có* gì (tài sản), *nợ* ai (nợ phải trả), và phần của cổ đông (VCSH). Luôn có: **Tổng tài sản = Nợ phải trả + VCSH** | Số tại ngày (đầu kỳ, cuối kỳ) | DB |
| • **Báo cáo kết quả kinh doanh** (KQKD, mẫu B02) | **Phim trong một kỳ**: doanh thu, chi phí, lợi nhuận | Số trong kỳ | DB |
| • **Báo cáo lưu chuyển tiền tệ** (LCTT, mẫu B03) | Tiền mặt **thật sự** vào và ra, chia 3 hoạt động: kinh doanh, đầu tư, tài chính | Số trong kỳ (**BCTC quý thường là lũy kế từ đầu năm**) | DB |
| • **Thuyết minh BCTC** (mẫu B09) | Giải thích chi tiết từng khoản: vay ai, lãi suất, tài sản đảm bảo, phải thu của ai, chi tiết TSCĐ, bộ phận kinh doanh… | Chữ + bảng nhỏ | **RAG** |
| **BCTC hợp nhất** / **BCTC riêng** | Hợp nhất = mẹ và các con cộng lại như **một công ty**, giao dịch mẹ–con bị loại bỏ (giống sổ thu chi của cả nhà). Riêng = chỉ pháp nhân công ty mẹ; công ty con chỉ hiện là "khoản đầu tư" | Mỗi kỳ có đủ cả hai bộ | **Quy tắc:** không nói rõ thì dùng hợp nhất (`loai_bctc = hop_nhat`). Dùng riêng khi hỏi "công ty mẹ", "BCTC riêng", hoặc hỏi lợi nhuận riêng làm căn cứ chia cổ tức |
| **Tự lập / soát xét / kiểm toán** | Mức độ được kiểm tra. Quý: công ty **tự lập**, không ai kiểm tra. Bán niên: kiểm toán độc lập **soát xét** (kiểm tra nhẹ). Năm: **kiểm toán** đầy đủ (ví dụ BCTC 2025 của CTR do AASC kiểm toán). Số đã kiểm toán vẫn có thể bị Kiểm toán Nhà nước yêu cầu trình bày lại | Cột `ky` trong manifest: Q1–Q4 / 6T / N | **Quy tắc:** số cả năm lấy bản kiểm toán, số 6 tháng lấy bản soát xét, số riêng một quý lấy bản tự lập (ghi rõ "chưa kiểm toán"). Lưu phiên bản số liệu và ưu tiên bản mới nhất |
| **Công văn giải trình** | Giải thích vì sao lợi nhuận thay đổi **so với cùng kỳ** (bắt buộc khi biến động từ 10% trở lên) | Chữ | RAG — ⚠️ **không phải** giải trình so với kế hoạch |
| **BCTN** (báo cáo thường niên) | Báo cáo năm viết bằng lời: tình hình kinh doanh, các mảng, **kết quả so với kế hoạch**, định hướng | Chữ + biểu đồ | RAG |
| **Báo cáo phát triển bền vững** | Môi trường, xã hội, quản trị (ESG) | Chữ | RAG (ít dùng) |
| **Nghị quyết / Biên bản ĐHĐCĐ** | Cổ đông biểu quyết: kế hoạch năm, phân phối lợi nhuận, cổ tức, phát hành, chọn kiểm toán | Chữ | RAG, và nguồn nhập tay bảng kế hoạch, cổ tức |
| **Tờ trình ĐHĐCĐ** | Đề xuất của HĐQT trình cổ đông duyệt (trước khi thành nghị quyết) | Chữ + bảng | RAG |
| **Báo cáo của HĐQT / Ban điều hành / BKS** | Báo cáo trình ĐHĐCĐ: kết quả năm, đánh giá, giải trình | Chữ | RAG |
| **Nghị quyết HĐQT** | Quyết định của hội đồng quản trị: triển khai trả cổ tức, thế chấp, đầu tư… | Chữ | RAG |
| **CBTT** (công bố thông tin) | Thông báo bắt buộc ra công chúng: ngày chốt quyền, kết quả phát hành, thay đổi vốn… | Chữ | RAG, và nguồn nhập tay bảng sự kiện |
| **Báo cáo tình hình quản trị** | 6 tháng / năm: họp HĐQT, các nghị quyết, giao dịch với người có liên quan | Chữ + bảng | RAG |
| **Điều lệ / Quy chế** | "Hiến pháp" của công ty, gồm cả quy định chia lợi nhuận | Chữ | Tham khảo |
| **Bản cáo bạch** | Tài liệu giới thiệu khi niêm yết hoặc chào bán | Chữ | Tham khảo |

---

## 2. Thuật ngữ theo 6 mảng

### A. Huy động và cơ cấu nguồn vốn
| Thuật ngữ | Nghĩa |
|---|---|
| **Vốn chủ sở hữu (VCSH)** | Phần tài sản thuộc về cổ đông = Tổng tài sản − Nợ phải trả |
| **Vốn điều lệ (VĐL) / vốn góp** | Số vốn cổ đông đã góp, tính theo mệnh giá (thường 10.000 đ/cổ phiếu) |
| **Nợ phải trả** | Mọi khoản công ty phải trả: vay, phải trả người bán, thuế, lương… |
| **Nợ vay** (vay và nợ thuê tài chính) | Chỉ các khoản đi vay có lãi, là một phần của nợ phải trả |
| **Ngắn hạn / dài hạn** | Đến hạn trong vòng 12 tháng / sau 12 tháng |
| **Đòn bẩy tài chính** | Mức độ dựa vào vốn vay (xem công thức ở mục 4) |
| **Khả năng thanh toán lãi vay** | Lợi nhuận trước lãi vay và thuế gấp mấy lần tiền lãi phải trả |
| **Tài sản đảm bảo (TSĐB), thế chấp** | Tài sản đem cầm cố để được vay |
| **Covenant** (điều khoản ràng buộc) | Điều kiện ngân hàng đặt ra (ví dụ nợ/VCSH không được vượt X). **Doanh nghiệp VN hầu như không công bố** |
| **Phát hành cổ phiếu** | Bán thêm cổ phiếu hoặc chia cổ phiếu để tăng vốn: chào bán, trả cổ tức bằng cổ phiếu, tăng vốn từ nguồn VCSH |
| **ESOP** | Phát hành cổ phiếu ưu đãi cho người lao động |
| **Trái phiếu** | Giấy nợ công ty phát hành để vay tiền (chưa thấy CTR phát hành trong 2019–2026) |

### B. Quản lý dòng tiền
| Thuật ngữ | Nghĩa |
|---|---|
| **Lưu chuyển tiền thuần từ hoạt động kinh doanh (CFO)** | Tiền thu về từ hoạt động chính sau khi trả các chi phí |
| **… từ hoạt động đầu tư (CFI)** | Tiền chi mua tài sản hoặc góp vốn (thường âm), cộng tiền thu từ thanh lý |
| **… từ hoạt động tài chính (CFF)** | Tiền đi vay, trả nợ, trả cổ tức |
| **Dòng tiền tự do (FCF)** | Tiền "rảnh" thật sự sau khi đã đầu tư duy trì và mở rộng |
| **Vốn lưu động** | Tài sản ngắn hạn − Nợ ngắn hạn: tiền để "xoay vòng" hằng ngày |
| **Phải thu khách hàng** | Đã bán nhưng chưa thu được tiền |
| **Hàng tồn kho (HTK)** | Hàng hoá, vật tư, chi phí sản xuất kinh doanh dở dang (với công ty xây lắp: công trình chưa nghiệm thu) |
| **Phải trả người bán** | Đã mua nhưng chưa trả tiền |
| **Số ngày thu tiền (DSO) / tồn kho (DIO) / trả tiền (DPO)** | Trung bình mất bao nhiêu ngày để thu tiền, bán hết kho, trả nhà cung cấp |
| **Chu kỳ chuyển đổi tiền mặt (CCC)** | Số ngày từ lúc bỏ tiền ra đến lúc thu tiền về. Càng ngắn càng tốt |
| **Lũy kế** | Cộng dồn từ đầu năm đến cuối kỳ. Ví dụ LCTT quý 3 thường là số của 9 tháng |

### C. Chi phí và hiệu quả
| Thuật ngữ | Nghĩa |
|---|---|
| **Doanh thu thuần (DTT)** | Doanh thu sau khi trừ các khoản giảm trừ (chiết khấu, hàng bán bị trả lại…) |
| **Giá vốn hàng bán (GVHB)** | Chi phí trực tiếp để tạo ra hàng hoá, dịch vụ đã bán |
| **Lợi nhuận gộp** | DTT − GVHB |
| **Chi phí bán hàng / chi phí quản lý doanh nghiệp (QLDN)** | Chi phí bộ máy bán hàng / bộ máy văn phòng |
| **Doanh thu / chi phí hoạt động tài chính** | Lãi tiền gửi, cổ tức nhận được / lãi vay phải trả, lỗ tỷ giá… |
| **LNTT / LNST** | Lợi nhuận trước thuế / sau thuế thu nhập doanh nghiệp |
| **LNST của cổ đông công ty mẹ** | Phần LNST thuộc về cổ đông công ty mẹ (trong BCTC hợp nhất, trừ phần của cổ đông thiểu số) |
| **Biên lợi nhuận gộp / ròng** | Mỗi 100 đồng doanh thu thì còn bao nhiêu đồng lợi nhuận gộp / lợi nhuận sau thuế |
| **ROE / ROA** | Lợi nhuận trên vốn chủ / trên tổng tài sản: đo hiệu quả dùng tiền |
| **Hiệu suất sử dụng tài sản** (vòng quay tài sản) | 1 đồng tài sản tạo ra bao nhiêu đồng doanh thu |
| **EBIT / EBITDA** | Lợi nhuận trước lãi vay và thuế / trước lãi vay, thuế và khấu hao |
| **Báo cáo bộ phận** | Tách doanh thu (có khi cả lợi nhuận) theo mảng kinh doanh hoặc theo khu vực |
| **Mảng kinh doanh của CTR** | Theo BCTN (tên chính xác cần đối chiếu): vận hành khai thác, xây lắp, giải pháp tích hợp, hạ tầng cho thuê (TowerCo)… |

### D. Đầu tư và phân bổ vốn
| Thuật ngữ | Nghĩa |
|---|---|
| **CAPEX** | Tiền chi mua sắm, xây dựng tài sản dài hạn (trạm BTS, cáp quang, thiết bị…) |
| **Tài sản cố định (TSCĐ)** | Tài sản dùng lâu dài: hữu hình (máy móc, nhà cửa), vô hình (phần mềm, quyền sử dụng đất) |
| **Khấu hao** | Phân bổ dần giá trị TSCĐ vào chi phí qua các năm |
| **Chi phí xây dựng cơ bản dở dang (XDCB)** | Công trình, tài sản đang đầu tư, chưa hoàn thành |
| **Bất động sản đầu tư** | Tài sản giữ để cho thuê (với CTR: hạ tầng, tuyến cáp, trạm BTS cho thuê) |
| **Công ty con / liên doanh, liên kết** | Công ty mẹ kiểm soát (thường nắm trên 50%) / có ảnh hưởng đáng kể (thường 20–50%) |
| **Thoái vốn / thanh lý** | Bán phần vốn đã góp / bán, huỷ tài sản |
| **Đầu tư ra nước ngoài** | Góp vốn, lập pháp nhân ở nước khác (CTR từng chấm dứt một số dự án năm 2019, 2021, 2023) |

### E. Kế hoạch và kết quả thực hiện
| Thuật ngữ | Nghĩa |
|---|---|
| **Kế hoạch SXKD** | Mục tiêu doanh thu, lợi nhuận… do ĐHĐCĐ thông qua đầu năm (HĐQT có thể giao kế hoạch trước đó) |
| **Thực hiện (TH)** | Kết quả thực tế lấy từ BCTC |
| **Tỷ lệ hoàn thành kế hoạch** | TH / KH × 100% |
| **So với cùng kỳ (YoY)** | So với cùng kỳ năm trước. **Đây không phải so với kế hoạch** |
| **Giải trình** | Lý do chênh lệch: so với kế hoạch (BCTN, báo cáo Ban điều hành) hoặc so với cùng kỳ (công văn giải trình) |

### F. Phân phối lợi nhuận
| Thuật ngữ | Nghĩa |
|---|---|
| **Cổ tức** | Phần lợi nhuận chia cho cổ đông, bằng **tiền mặt** hoặc **cổ phiếu** |
| **Tỷ lệ cổ tức (%)** | Tính **trên mệnh giá** 10.000 đ. Ví dụ 10% = 1.000 đ/cổ phiếu (tiền mặt), hoặc 100 cổ phiếu được nhận thêm 10 cổ phiếu |
| **Ngày giao dịch không hưởng quyền (GDKHQ)** | Từ ngày này, người mua cổ phiếu không còn được nhận cổ tức đợt đó |
| **Ngày đăng ký cuối cùng (ĐKCC, ngày chốt quyền)** | Ai có tên trong danh sách cổ đông vào ngày này thì được nhận |
| **Ngày thanh toán** | Ngày thực trả tiền hoặc thực phát hành cổ phiếu |
| **Lợi nhuận sau thuế chưa phân phối** (lợi nhuận giữ lại) | Lợi nhuận tích luỹ chưa chia, chưa trích quỹ |
| **Trích lập quỹ** | Chia lợi nhuận vào các quỹ: đầu tư phát triển, khen thưởng phúc lợi… |
| **Tỷ lệ chi trả cổ tức (payout)** | Cổ tức tiền mặt / LNST, tức phần lợi nhuận đem chia |
| **EPS** (lãi cơ bản trên cổ phiếu) | LNST thuộc cổ đông phổ thông / số cổ phiếu lưu hành bình quân |

---

## 3. Các dòng chính trên BCTC và mã chỉ tiêu (theo TT200/TT202, cần đối chiếu với CTR)

### 3.0. ⚠️ Mã chỉ tiêu thay đổi theo kỳ báo cáo (đo trên BCTC thật của CTR, 21/09/2026)

Đối chiếu **BCTC riêng bán niên 2026** với **BCTC riêng năm 2023**:

| Chỉ tiêu | Mã ở BCTC 2023 | Mã ở BCTC 2026 |
|---|---|---|
| Dự phòng phải thu ngắn hạn khó đòi | 137 | **136** |
| Dự phòng giảm giá hàng tồn kho | 149 | **142** |
| Tài sản ngắn hạn khác | 150 | **160** |
| Chi phí trả trước ngắn hạn | 151 | **161** |
| Bất động sản đầu tư | 230 | **240** |
| Tài sản dở dang dài hạn | 240 | **250** |
| Đầu tư tài chính dài hạn | 250 | **260** |
| Tài sản dài hạn khác | 260 | **270** |
| **TỔNG CỘNG TÀI SẢN** | **270** | **280** |
| Nợ phải trả | 300 | 300 (không đổi) |

Khác biệt khác:
- Tên báo cáo: 2023 là **"Bảng cân đối kế toán riêng"**, 2026 là **"Báo cáo tình hình tài chính riêng giữa niên độ"**.
- Bản 2026 có thêm các dòng không có ở bản 2023, ví dụ mã 116, 119, 124, 125, 138, 153, 155, 157, 158, 177, 178, 179, 190, 199, 208.
- **Chưa xác minh văn bản pháp lý** nào quy định cách trình bày mới; cần tra cứu trước khi viết tài liệu cuối kỳ.

**Hệ quả bắt buộc với schema:**
1. Bảng số liệu phải lưu **mã nội bộ của hệ thống** (ví dụ `TONG_TAI_SAN`), kèm bảng ánh xạ **(chế độ trình bày, mẫu biểu, mã) → mã nội bộ**.
2. Mỗi bản ghi phải ghi rõ **kỳ báo cáo và nguồn**, vì cùng một mã số ở hai năm có thể là hai chỉ tiêu khác nhau.
3. Câu trả lời cho người dùng nên dùng **tên chỉ tiêu**, và chỉ nêu mã số kèm theo kỳ báo cáo tương ứng.

### 3.1 Bảng cân đối kế toán (B01)
| Mã | Chỉ tiêu |
|---|---|
| 100 | Tài sản ngắn hạn |
| 110 | Tiền và các khoản tương đương tiền |
| 130 | Các khoản phải thu ngắn hạn |
| 131 | Phải thu ngắn hạn của khách hàng |
| 140 | Hàng tồn kho (tổng) |
| 200 | Tài sản dài hạn |
| 220 | Tài sản cố định |
| 240 | Tài sản dở dang dài hạn (gồm chi phí XDCB dở dang) |
| **270** | **Tổng cộng tài sản** |
| **300** | **Nợ phải trả** |
| 310 | Nợ ngắn hạn |
| 311 | Phải trả người bán ngắn hạn |
| 320 | Vay và nợ thuê tài chính ngắn hạn |
| 330 | Nợ dài hạn |
| 338 | Vay và nợ thuê tài chính dài hạn |
| **400** | **Vốn chủ sở hữu** |
| 411 | Vốn góp của chủ sở hữu |
| 418 | Quỹ đầu tư phát triển |
| 421 | Lợi nhuận sau thuế chưa phân phối |
| 429 | Lợi ích cổ đông không kiểm soát (chỉ có trên BCTC hợp nhất) |
| 440 | Tổng cộng nguồn vốn (= 270) |

### 3.2 Báo cáo kết quả kinh doanh (B02)
| Mã | Chỉ tiêu |
|---|---|
| 01 | Doanh thu bán hàng và cung cấp dịch vụ |
| 02 | Các khoản giảm trừ doanh thu |
| **10** | **Doanh thu thuần** |
| 11 | Giá vốn hàng bán |
| **20** | **Lợi nhuận gộp** |
| 21 | Doanh thu hoạt động tài chính |
| 22 | Chi phí tài chính |
| 23 | *trong đó:* Chi phí lãi vay |
| 24 | Phần lãi/lỗ trong công ty liên doanh, liên kết (chỉ có trên BCTC hợp nhất) |
| 25 | Chi phí bán hàng |
| 26 | Chi phí quản lý doanh nghiệp |
| 30 | Lợi nhuận thuần từ hoạt động kinh doanh |
| 40 | Lợi nhuận khác |
| **50** | **Tổng lợi nhuận kế toán trước thuế** |
| 51, 52 | Chi phí thuế TNDN hiện hành, hoãn lại |
| **60** | **Lợi nhuận sau thuế** |
| 61, 62 | LNST của cổ đông công ty mẹ / của cổ đông không kiểm soát (chỉ có trên BCTC hợp nhất) |
| 70 | Lãi cơ bản trên cổ phiếu (EPS) |

### 3.3 Báo cáo lưu chuyển tiền tệ (B03)
| Mã | Chỉ tiêu |
|---|---|
| **20** | **Lưu chuyển tiền thuần từ hoạt động kinh doanh** |
| 21 | Tiền chi để mua sắm, xây dựng TSCĐ và tài sản dài hạn khác (**CAPEX**, số âm) |
| **30** | **Lưu chuyển tiền thuần từ hoạt động đầu tư** |
| **40** | **Lưu chuyển tiền thuần từ hoạt động tài chính** |
| 50 | Lưu chuyển tiền thuần trong kỳ |
| 60 / 70 | Tiền đầu kỳ / cuối kỳ |

> Mã 20, 30… **trùng số** giữa các bảng (B02 mã 20 là lợi nhuận gộp, B03 mã 20 là CFO). Vì vậy khoá trong DB phải gồm **(mẫu biểu, mã chỉ tiêu)**.

---

## 4. Công thức chỉ số (tính bằng Java, không để LLM tự tính)

"Bình quân" = (đầu kỳ + cuối kỳ) / 2. Ký hiệu `[B01.300]` nghĩa là mã 300 trên bảng cân đối kế toán.

| Chỉ số | Công thức | Mảng |
|---|---|---|
| Nợ phải trả / VCSH | `[B01.300] / [B01.400]` | A |
| Nợ vay / VCSH | `([B01.320] + [B01.338]) / [B01.400]` | A |
| Nợ phải trả / Tổng tài sản | `[B01.300] / [B01.270]` | A |
| Khả năng thanh toán lãi vay (lần) | `([B02.50] + [B02.23]) / [B02.23]` | A |
| Dòng tiền tự do (FCF) | `[B03.20] + [B03.21]` (vì mã 21 là số âm) | B |
| DSO (ngày) | `[B01.131] bình quân / [B02.10] × số ngày trong kỳ` | B |
| DIO (ngày) | `[B01.140] bình quân / [B02.11] × số ngày` | B |
| DPO (ngày) | `[B01.311] bình quân / [B02.11] × số ngày` | B |
| CCC (ngày) | `DSO + DIO − DPO` | B |
| Biên lợi nhuận gộp | `[B02.20] / [B02.10]` | C |
| Biên lợi nhuận ròng | `[B02.60] / [B02.10]` | C |
| Tỷ trọng chi phí | `[B02.11]`, `[B02.25]`, `[B02.26]`, `[B02.22]` chia cho `[B02.10]` | C |
| ROE | `[B02.60]` (hoặc `[B02.61]` nếu hợp nhất) `/ [B01.400]` bình quân (hợp nhất: VCSH trừ `[B01.429]`) | C |
| ROA | `[B02.60] / [B01.270]` bình quân | C |
| Vòng quay tổng tài sản | `[B02.10] / [B01.270]` bình quân | C |
| CAPEX | `−[B03.21]` | D |
| Tỷ lệ hoàn thành kế hoạch | `thực hiện / kế hoạch` (bảng kế hoạch nhập tay) | E |
| Tỷ lệ chi trả cổ tức | `cổ tức tiền mặt (tỷ lệ × 10.000 đ × số cổ phiếu) / LNST cổ đông công ty mẹ` | F |

> Với kỳ **quý**, chỉ số tính trên số của năm (ROE, ROA, vòng quay) phải ghi rõ là "theo quý" hay "quy năm". Với số liệu LCTT quý, **phải tách từ số lũy kế** trước khi tính.

---

## 5. Bẫy thường gặp (phải xử lý trong hệ thống)

| Bẫy | Cách xử lý |
|---|---|
| **LCTT quý là số lũy kế** | Tách: quý N = lũy kế N − lũy kế N−1. Lưu cả hai loại số, gắn cờ |
| **Hợp nhất và riêng** | Mặc định dùng hợp nhất. Câu trả lời phải ghi rõ đang dùng loại nào |
| **Đơn vị** | BCTC ghi theo đồng (VND). Người dùng hỏi "tỷ" hoặc "triệu". Chuẩn hoá khi hỏi và khi trả lời |
| **Đầu kỳ và cuối kỳ** | Bảng CĐKT có hai cột. "Số dư năm 2025" hiểu là số tại 31/12/2025 |
| **Q4 và cả năm** | BCTC quý 4 (tự lập) khác BCTC năm (đã kiểm toán). Số liệu năm ưu tiên bản kiểm toán |
| **Số đính chính / trình bày lại** | BCTC năm sau có thể trình bày lại số năm trước. CTR có đính chính BCTC 2021 và trình bày lại theo Kiểm toán Nhà nước (01/2024). Phải ưu tiên số mới nhất và ghi chú lại |
| **LNST tổng và LNST cổ đông công ty mẹ** | ROE, EPS và tỷ lệ chi trả cổ tức dùng phần của cổ đông công ty mẹ |
| **Ba ngày của cổ tức** | GDKHQ ≠ ĐKCC ≠ ngày thanh toán. Hỏi "khi nào trả" thì dùng ngày thanh toán |
| **Tỷ lệ cổ tức tính trên mệnh giá**, không phải trên giá thị trường | 10% = 1.000 đ/cổ phiếu |
| **"So với cùng kỳ" và "so với kế hoạch"** | Hai loại giải trình khác nhau, lấy từ hai nguồn khác nhau |
| **Kỳ chưa có số liệu** | Ví dụ "kế hoạch 2026 thực hiện ra sao": chỉ có đến 30/06/2026, phải nói rõ |

---

## 6. Viết tắt

| Viết tắt | Đầy đủ |
|---|---|
| BCTC | Báo cáo tài chính |
| CĐKT / BCĐKT | (Bảng) cân đối kế toán |
| KQKD | Kết quả (hoạt động) kinh doanh |
| LCTT | Lưu chuyển tiền tệ |
| TM | Thuyết minh |
| HN / R | Hợp nhất / Riêng |
| BCTN | Báo cáo thường niên |
| ĐHĐCĐ | Đại hội đồng cổ đông |
| HĐQT | Hội đồng quản trị |
| BKS | Ban kiểm soát |
| BĐH / BGĐ / BTGĐ | Ban điều hành / Ban giám đốc / Ban Tổng giám đốc |
| TGĐ / PTGĐ / KTT | Tổng giám đốc / Phó Tổng giám đốc / Kế toán trưởng |
| CBTT | Công bố thông tin |
| ĐKCC | Đăng ký cuối cùng |
| GDKHQ | Giao dịch không hưởng quyền |
| NLQ | Người có liên quan |
| VCSH | Vốn chủ sở hữu |
| VĐL | Vốn điều lệ |
| DT / DTT | Doanh thu / Doanh thu thuần |
| GVHB | Giá vốn hàng bán |
| CPBH / CP QLDN | Chi phí bán hàng / Chi phí quản lý doanh nghiệp |
| LN / LNG / LNTT / LNST | Lợi nhuận / gộp / trước thuế / sau thuế |
| TNDN | Thu nhập doanh nghiệp |
| HTK | Hàng tồn kho |
| TSCĐ | Tài sản cố định |
| XDCB | Xây dựng cơ bản |
| TSĐB | Tài sản đảm bảo |
| SXKD | Sản xuất kinh doanh |
| KH / TH | Kế hoạch / Thực hiện |
| PPLN | Phân phối lợi nhuận |
| ESOP | Cổ phiếu cho người lao động |
| CTCP / TCT | Công ty cổ phần / Tổng công ty |
| CTR / VCC | Mã cổ phiếu / tên viết tắt nội bộ của Viettel Construction |
| UBCKNN | Uỷ ban Chứng khoán Nhà nước |
| HOSE / HNX / UPCoM | Sở GDCK TP.HCM / Sở GDCK Hà Nội / Thị trường cổ phiếu chưa niêm yết |
| KTNN | Kiểm toán Nhà nước |
| TT200 / TT202 | Thông tư 200/2014 (chế độ kế toán) / 202/2014 (BCTC hợp nhất) |
| EPS, ROE, ROA | Lãi trên cổ phiếu, lợi nhuận trên VCSH, lợi nhuận trên tổng tài sản |
| FCF, CFO, CAPEX | Dòng tiền tự do, dòng tiền kinh doanh, chi đầu tư tài sản |
| DSO, DIO, DPO, CCC | Số ngày thu tiền, tồn kho, trả tiền, chu kỳ chuyển đổi tiền mặt |
| YoY | So với cùng kỳ năm trước |

---

## 7. Việc cần làm với từ điển này
- [ ] Đối chiếu mã chỉ tiêu mục 3 với BCTC hợp nhất 2025 của CTR, và với cách vnstock đặt tên chỉ tiêu.
- [ ] Nhờ anh/chị phòng kế toán, tài chính rà lại mục 2, 4 và 5.
- [ ] Tách mục 6 thành file từ điển máy đọc được (ví dụ YAML/JSON) cho bước chuẩn hoá truy vấn trong Java.
