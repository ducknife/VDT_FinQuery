# Dữ liệu — v1.0

*Ngày: 2026-09-17 · Bản đầu tiên (thu thập lần 1)*

## 1. Nguồn

Toàn bộ dữ liệu lấy từ website chính thức của CTR (`https://viettelconstruction.com.vn`).

| # | Thư mục | Trang nguồn | Dùng cho mảng |
|---|---|---|---|
| 01 | `01_bao_cao_tai_chinh` | `/bao-cao-tai-chinh/` | A B C D E |
| 02 | `02_bao_cao_thuong_nien` | `/bao-cao-thuong-nien/` | C E (giải trình, mảng kinh doanh) |
| 03 | `03_dai_hoi_dong_co_dong` | `/quan-he-co-dong/dai-hoi-dong-co-dong/` | A E F |
| 04 | `04_cong_bo_thong_tin` | `/quan-he-co-dong/cong-bo-thong-tin/` (**có lọc**) | A D F |
| 05 | `05_bao_cao_quan_tri` | `/quan-he-co-dong/quan-tri-doanh-nghiep/bao-cao-quan-tri/` | A D (giao dịch với bên liên quan, NQ HĐQT) |
| 06 | `06_dieu_le_quy_che` | `/quan-he-co-dong/quan-tri-doanh-nghiep/dieu-le-va-cac-quy-dinh/` | tham khảo (quy định phân phối lợi nhuận) |
| 07 | `07_ban_cao_bach` | `/quan-he-co-dong/quan-tri-doanh-nghiep/ban-cao-bach/` | tham khảo (lịch sử, cơ cấu vốn khi niêm yết HOSE) |
| 08 | `08_du_lieu_web` | API `wp-json/quanhecodong/v1/*` | F (cổ tức), A (tăng vốn, cơ cấu sở hữu) |

Ba nguồn ban đầu là 01, 02 và 03. Nguồn **04–08 được bổ sung** vì ba nguồn ban đầu thiếu các sự kiện xảy ra sau nghị quyết (kết quả phát hành, ngày chốt quyền nhận cổ tức, thoái vốn, nghị quyết HĐQT). Chi tiết ở QĐ-004 trong [quyet-dinh_v1.0.md](quyet-dinh_v1.0.md).

## 2. Cách thu thập

- Tool: [`backend/tai_bao_cao_ctr.py`](../backend/tai_bao_cao_ctr.py) (bản 2). Lệnh chạy: `python backend/tai_bao_cao_ctr.py`.
- Chạy lại an toàn: file đã có thì bỏ qua.
- Khoảng năm mặc định: **2019–2026** (`--tu`, `--den`).
- Cách xác định **năm** để xếp thư mục:
  - Nguồn 01, 02, 05: theo **kỳ báo cáo** đọc từ tiêu đề. Ví dụ BCTC năm 2025 công bố tháng 3/2026 vẫn nằm trong `2025/`.
  - Nguồn 03, 04: theo **tab năm trên web**, tức năm đăng hoặc năm diễn ra sự kiện. Ví dụ ĐHĐCĐ 2026 (duyệt kết quả 2025 và kế hoạch 2026) nằm trong `2026/`.
- Phân loại dựa vào **tiêu đề hiển thị trên web**, không dựa vào tên file (nhiều tên file vô nghĩa như `BCTC_Mẹ.pdf`, `CV-giai-trinh.pdf`).
- **Bỏ qua**, có ghi lý do trong manifest:
  - bản chỉ có tiếng Anh (bản song ngữ TV+TA vẫn giữ),
  - văn bản thủ tục họp (thư mời, uỷ quyền, chương trình, quy chế họp, đề cử),
  - văn bản hành chính (hợp đồng kiểm toán, gia hạn công bố),
  - CBTT ngoài phạm vi A–F (nhân sự, đăng ký kinh doanh, giao dịch người nội bộ),
  - dự thảo điều lệ.
- **Loại trùng:** trùng URL giữa các nguồn thì giữ nguồn đứng trước. Trùng nội dung (cùng SHA-256) thì chỉ giữ một bản.
- **Trùng tên file** (ví dụ hai file `BCTC-rieng.pdf` khác nhau ở 2023): bản sau được thêm hậu tố tháng đăng (`BCTC-rieng__2023-08.pdf`).

## 3. Cấu trúc thư mục

```
data/raw/CTR/
├── manifest.csv              mọi mục tìm thấy (613), kể cả mục bỏ qua + lý do
├── chat_luong_pdf.csv        đánh giá lớp chữ từng PDF (so_hoa / ocr_kem / scan)
├── 01_bao_cao_tai_chinh/<năm>/{hop_nhat, rieng, giai_trinh}/
├── 02_bao_cao_thuong_nien/<năm>/{thuong_nien, phat_trien_ben_vung}/
├── 03_dai_hoi_dong_co_dong/<năm>/{nghi_quyet_bien_ban, to_trinh, bao_cao, tai_lieu_hop}/
├── 04_cong_bo_thong_tin/<năm>/{co_tuc, phat_hanh_tang_von, dau_tu_thoai_von, the_chap_vay_von,
│                                ke_hoach_kinh_doanh, dinh_chinh, nghi_quyet_dhdcd,
│                                lay_y_kien_co_dong, bctn_bctc}/
├── 05_bao_cao_quan_tri/<năm>/quan_tri/
├── 06_dieu_le_quy_che/{dieu_le, quy_che}/
├── 07_ban_cao_bach/ban_cao_bach/
└── 08_du_lieu_web/           *.json (bản gốc API) + *.md (bảng đọc được)
```

Các cột của `manifest.csv`:
`nguon, tieu_de, url, nam_tab, ngay_dang, su_kien, nam, ky, nhom, ngon_ngu, trang_thai, ly_do, duong_dan, kich_thuoc, sha256, ghi_chu`.
Trong đó cột `ky` nhận các giá trị Q1–Q4, 6T (bán niên), 9T hoặc N (cả năm).

PDF gốc **không đưa lên git** (xem `.gitignore`). Manifest, bảng chất lượng và `08_du_lieu_web` thì có đưa lên git.

## 4. Thống kê lần thu thập 2026-09-17

| Nguồn | Trên web | Tải về | Bỏ qua | Trùng URL |
|---|---|---|---|---|
| 01 BCTC | 172 | 126 | 46 | – |
| 02 BCTN | 14 | 10 | 4 | – |
| 03 ĐHĐCĐ | 143 | 75 | 68 | – |
| 04 CBTT | 242 | 67 | 168 | 7 |
| 05 Báo cáo quản trị | 13 | 12 | 1 | – |
| 06 Điều lệ, quy chế | 28 | 23 | 5 | – |
| 07 Bản cáo bạch | 1 | 1 | 0 | – |
| **Tổng** | **613** | **314** | **292** | **7** |

- Dung lượng khoảng **2,4 GB**, tổng **10.424 trang**. Không file nào tải lỗi.
- Lý do bỏ qua:

| Lý do | Số mục |
|---|---|
| CBTT ngoài phạm vi A–F | 106 |
| Ngoài khoảng năm 2019–2026 | 74 |
| Thủ tục họp | 46 |
| Văn bản hành chính | 26 |
| Bản tiếng Anh | 25 |
| ĐKCC để tham dự ĐHĐCĐ | 9 |
| Dự thảo điều lệ/quy chế | 6 |

**Độ phủ BCTC:** đủ **Q1–Q4, bán niên và năm, cả hợp nhất lẫn riêng, cho 2019–2025**. Năm 2026 có Q1, Q2 và bán niên. Các năm đều có công văn giải trình chênh lệch lợi nhuận.

**Độ phủ BCTN:** đủ 2019–2025. Báo cáo phát triển bền vững có từ 2023 đến 2025.

**Độ phủ ĐHĐCĐ:** có nghị quyết và biên bản thường niên cho mọi năm 2019–2026, cộng thêm:
- ĐHĐCĐ bất thường ngày 26/01/2019,
- lấy ý kiến cổ đông bằng văn bản tháng 12/2023.

Tờ trình và báo cáo HĐQT, Ban điều hành, BKS có từ 2021 đến 2025. Năm 2026 chỉ có "biên bản kèm tài liệu" và "tài liệu họp" dạng gộp.

## 5. Chất lượng PDF (đo bằng [`backend/kiem_tra_pdf.py`](../backend/kiem_tra_pdf.py))

| Mức | Ý nghĩa | Số file | Số trang |
|---|---|---|---|
| `so_hoa` | Có lớp chữ chuẩn, trích thẳng được | 28 | 3.743 |
| `ocr_kem` | Có lớp chữ nhưng do máy scan tự OCR, **mất hoặc sai dấu** | 178 | 4.665 |
| `scan` | Không có chữ, chỉ có ảnh | 108 | 2.016 |

**Phát hiện quan trọng:**
- **Không có BCTC nào ở mức `so_hoa`.**
  - BCTC 2021–2026 đều là bản scan kèm lớp OCR kém, ví dụ BCTC hợp nhất 2025 trang 5 đọc ra "BAO CÁO KIEM TOAN DQC L1P", "Bang can dOl k4 toán".
  - BCTC 2019–2020 và Q2/2022 là ảnh thuần.
  - Chữ số trong lớp OCR kém *thường* còn đúng (ví dụ `902.340.390.204`), nhưng không đủ tin cậy để trích xuất số liệu.
- BCTN, báo cáo phát triển bền vững và báo cáo quản trị là nhóm **sạch**: hầu hết ở mức `so_hoa`.
- Phần lớn nghị quyết và CBTT năm 2024–2026 là bản scan có chữ ký và dấu.
- **Tổng khoảng 6.700 trang cần OCR.** Chia theo nguồn:

| Nguồn | Số trang cần OCR |
|---|---|
| BCTC | 4.040 |
| Điều lệ, quy chế | 877 |
| ĐHĐCĐ | 786 |
| CBTT | 656 |
| Báo cáo quản trị | 132 |
| Bản cáo bạch | 129 |
| BCTN | 61 |

**Hệ quả với kiến trúc:**
- Số liệu có cấu trúc cho DB **không lấy từ PDF** mà lấy từ vnstock (dự kiến). PDF BCTC chỉ phục vụ **RAG phần thuyết minh** và làm **bằng chứng trích dẫn**.
- Tuần 2 cần một bước OCR tiếng Việt trước `chuyen_pdf_rag.py`. Công cụ OCR chưa chọn (QĐ-008).
- Nên OCR theo thứ tự ưu tiên để tiết kiệm thời gian:
  1. BCTC hợp nhất năm và bán niên,
  2. nghị quyết ĐHĐCĐ,
  3. CBTT cổ tức và phát hành,
  4. phần còn lại.

  Điều lệ và bản cáo bạch có thể để sau cùng.

## 6. Dữ liệu web dạng bảng (`08_du_lieu_web`)

| File | Số dòng | Ghi chú |
|---|---|---|
| `lich_su_co_tuc` | 14 | **Chỉ đến cổ tức năm 2021** (chi trả năm 2022) |
| `lich_su_tang_von` | 11 | **Chỉ đến 10/08/2022** (vốn điều lệ 1.143.858.790.000 đ) |
| `co_cau_so_huu__*` | 1–8 | Website ghi cập nhật **30/09/2024** |

⚠️ Các trang này **không được cập nhật**, nên cổ tức và tăng vốn từ 2022 trở đi phải lấy từ nguồn 03 và 04 (CBTT và nghị quyết).

Ví dụ các văn bản đã có:
- chi trả cổ tức năm 2022, 2023, 2024, 2025,
- phát hành cổ phiếu trả cổ tức năm 2025, báo cáo kết quả ngày 20/07/2026.

Muốn dùng cho mảng F thì **nhập tay** các văn bản này vào bảng DB `co_tuc` và `phat_hanh`, kèm nguồn.

## 7. Lỗi của website đã phát hiện và cách xử lý

| Vấn đề | Xử lý |
|---|---|
| File `2026/01/BCTC-hop-nhat-ViEn.pdf` được web ghi là "BCTC hợp nhất **Quý 4/2026**" | Sửa tay thành **Q4/2025** (`SUA_TAY` trong tool, có `ghi_chu` trong manifest). Bằng chứng: đăng ngày 29/01/2026 cùng BCTC riêng Q4/2025, trang bìa ghi "năm tài chính kết thúc 31/12/2025" |
| Hai file khác nhau cùng tên `BCTC-rieng.pdf` (bán niên 2023 và năm 2023) | Thêm hậu tố tháng đăng cho file sau |
| Các file tên `VCC-...` | **Là tài liệu của CTR**: VCC là tên viết tắt nội bộ của Viettel Construction (xuất hiện trong "Dự thảo Quy chế quản trị nội bộ VCC 2021"). Nhận định trước đó "có thể là công ty khác" là **sai** |
| Website chặn bot bằng cookie JS (`D1N=...`) | Tool tự đọc cookie từ trang trả về rồi gửi lại |
| Có văn bản đính chính và trình bày lại số liệu | Giữ trong `04_cong_bo_thong_tin/<năm>/dinh_chinh/`: đính chính BCTC hợp nhất 2021 (2022); trình bày lại số liệu theo Kiểm toán Nhà nước (01/2024); đính chính NQ HĐQT (2023). **Khi nạp số liệu phải ưu tiên số đã đính chính** |

## 8. Lỗ hổng còn lại so với phạm vi A–F

- **Covenant khoản vay:** không được công bố, nên để ngoài phạm vi và dùng làm câu hỏi bẫy.
- **Trái phiếu:** chưa thấy văn bản phát hành trái phiếu nào trong giai đoạn 2019–2026. Cần xác nhận lại trong thuyết minh BCTC.
- **Lợi nhuận theo mảng kinh doanh:** chưa kiểm tra thuyết minh bộ phận. Việc này làm ở tuần 2.
- **Số liệu có cấu trúc (DB):** chưa kéo từ vnstock. Việc này làm ở tuần 1.
- **Giải trình so với kế hoạch** nằm ở BCTN và báo cáo HĐQT/Ban điều hành. Các công văn giải trình trong 01 chỉ giải thích **biến động so với cùng kỳ**, không phải so với kế hoạch.
