# Lược đồ cơ sở dữ liệu — v1.0

*Ngày: 2026-09-29 · Tài liệu mới · Nối tiếp [luong-hoi-dap_v1.0.md](luong-hoi-dap_v1.0.md)*

Ghi lại phần **cơ sở dữ liệu lõi**: năm bảng, các quyết định lược đồ, và — phần
quan trọng nhất để đọc lại — **những tiêu chí khiến một dòng số liệu bị loại**.

Lý do đằng sau từng quyết định nằm ở `luong-hoi-dap_v1.0.md`; tài liệu này chỉ
chốt kết quả và những gì mới phát sinh hôm nay.

---

## 1. Năm bảng lõi

```
financial_items            123 dòng   BỘ TỪ VỰNG — 123 mã nội bộ, nguồn sinh enum
      ▲
      ├── financial_item_mappings     250 dòng   dịch (chế độ, mẫu biểu, mã) → mã nội bộ
      └── financial_figures        ~7.000 dòng   SỐ LIỆU — nhánh SQL truy vấn bảng này
                  │
documents ◄───────┤  278 dòng   danh mục tài liệu — nơi trích dẫn trỏ về
      ▲           │
      └── extraction_exceptions      nhật ký dòng bị loại
```

| Migration | Việc |
|---|---|
| `V1` `V2` | `financial_items` + `financial_item_mappings` |
| `V3` | sửa `dau_am_tu_nhien` (bỏ 2, thêm 9 → 14 mã) |
| `V4` `V5` | `documents`, kèm sửa collation |
| `V6` | `financial_figures` |
| `V7` | `extraction_exceptions` |
| `V8` | thêm 4 cột trạng thái nạp vào `documents` |

Còn treo, chưa chặn ai: `V9 query_audit` (cần cho chương đánh giá) · sổ công thức
· bảng sự kiện cổ tức / tăng vốn.

---

## 2. NĂM TIÊU CHÍ KHIẾN MỘT DÒNG BỊ LOẠI

**Đơn vị là một DÒNG** — một bộ ba `(mã, tên, giá trị)` trong một bảng trên một
trang. Không phải cả bảng, không phải cả trang. Một trang 30 dòng mà 2 dòng lệch
thì 28 dòng kia vẫn vào `financial_figures`.

| # | `loai_loi` | Nghĩa | Nguyên nhân thường gặp |
|---|---|---|---|
| 1 | `ma_la` | cặp `(chế độ, mẫu biểu, mã)` không có trong bảng ánh xạ | người làm tài liệu tự thêm dòng hoặc tự đặt số |
| 2 | `ten_khong_khop` | mã có trong bảng nhưng chữ trên giấy khác hẳn tên kỳ vọng | trình bày rút gọn, đánh lại số khác chuẩn |
| 3 | `sai_dau` | chỉ tiêu `dau_am_tu_nhien = 1` mà đọc ra số dương | OCR làm rơi dấu ngoặc đơn |
| 4 | `khong_doc_duoc_so` | ô tiền không phân tích được thành số | OCR hỏng ô |
| 5 | `lech_can_doi` | phép cộng không khớp, ví dụ `mã 10 ≠ mã 01 − mã 02` | **dính cả cụm**, không phải một dòng |

Ba loại đầu là **người trình bày khác chuẩn**. Loại 4 là lỗi máy. Loại 5 khác
hẳn về bản chất — nó bắt theo cụm.

**Ca thật đã gặp** — loại 2, 3 trang:

```
tai_lieu_id   CTR-BCTC-HN-20211231-NAM-xxxxxx
trang         6
loai_loi      ten_khong_khop
che_do/mau_bieu/ma    TT200 / B01 / 223
ten_doc_duoc  2. Tài sản cố định vô hình
ten_ky_vong   - Giá trị hao mòn lũy kế
gia_tri_doc   26.277.949.538
chi_tiet      CTR trinh bay rut gon, danh ma 223 thay cho 227 theo chuan TT200
```

Đã kiểm: cả 3 giá trị đều có ở mã 227 đúng chuẩn tại 11, 3 và 12 tài liệu khác
⇒ **bỏ qua không mất dữ liệu**.

### Số dòng lỗi dồn cục theo trang là tín hiệu

```
1–2 dòng lỗi / trang    người làm sửa vài dòng — bỏ qua được
20 dòng lỗi / trang     hỏng CẤU TRÚC, không phải hỏng dòng
                        (ví dụ OCR đảo cột: "kỳ này" nằm chỗ "kỳ trước")
```

Nhìn phân bố lỗi theo trang thì biết nên sửa dòng hay sửa bộ đọc.

### Giới hạn — điều năm tiêu chí này KHÔNG bắt được

> **Bắt được dòng lệch chuẩn một cách lộ liễu. Dòng lệch chuẩn một cách nhất
> quán thì không thấy.**

Nếu người làm tài liệu đổi mã **và** đổi tên cho khớp nhau, mà phép cộng vẫn
đúng, thì không chốt kiểm nào phát hiện — con số vào `financial_figures` dưới
một mã sai.

Ca 223 bị bắt **chỉ vì** họ đổi mã mà giữ nguyên tên. May, không phải nhờ thiết kế.

Cách chặn triệt để duy nhất là **đối chiếu với số đã công bố ở nguồn khác** —
như việc đã làm với bộ tỷ số BCTN (mục 4). Nhưng cách đó chỉ áp dụng được cho
vài chục chỉ tiêu tổng, không cho cả 123 mã. Với 3 dòng lỗi trên 330 trang thì
mức bảo đảm hiện tại đủ dùng.

---

## 3. Ba đường — không có gì rơi ra ngoài

```
khớp chuẩn   →  financial_figures        nhánh SQL trả lời, chính xác tới đồng
lệch chuẩn   →  extraction_exceptions    ghi chú cho người soát, KHÔNG trả lời câu hỏi
MỌI trang    →  Qdrant                   nguyên văn, nhánh RAG trả lời được
```

`extraction_exceptions` **không chứa dữ liệu**, chỉ chứa lý do bỏ qua. Cột
`gia_tri_doc` là **chuỗi thô** (`VARCHAR`), giữ để người đọc thấy, không phải để
truy vấn.

Trang bị loại vẫn nằm nguyên trong Qdrant. Hỏi *"TSCĐ vô hình 2021"* thì:
nhánh SQL trả *"chưa có số liệu"*, nhánh RAG tìm được trang 6 và trả kèm trích dẫn.
**Bỏ qua ở DB không mất câu trả lời**, chỉ mất khả năng tính toán và liệt kê.

Công dụng thật của bảng này là **làm thước đo**: log 3 dòng thì kệ, log 300 dòng
thì có gì đó sai hệ thống. Không có nó thì script bỏ qua âm thầm — đó mới nguy hiểm.

---

## 4. Sổ công thức lấy ở đâu — và cách chống bịa

Ba nguồn, đều là văn bản pháp luật, **không tự nghĩ ra**:

| Nguồn | Cho gì |
|---|---|
| TT99 / TT200 | số học mã: `280 = 100 + 200`, `440 = 300 + 400` |
| **TT96/2020 Phụ lục IV mục II.4.b** | **10 tỷ số luật bắt buộc, kèm công thức** |
| VAS 30 | EPS |

**Quy trình bốn bước, máy quyết chứ không phải người đoán:**

```
1. lấy công thức từ văn bản pháp luật
2. tính từ financial_figures
3. so với số CTR đã công bố trong BCTN
4. lệch → loại. khớp → ghi vào sổ, kèm năm đã đối chiếu
```

Chạy thật trên BCTN 2019 (CTR công bố 13 tỷ số):

```
Tỷ số                        CTR công bố   Ta tính    Kết luận
Hệ số thanh toán ngắn hạn        1,36       1,36      khớp
Hệ số thanh toán nhanh           1,13       1,13      khớp
Nợ / Tổng tài sản                0,64       0,64      khớp
Nợ / Vốn chủ sở hữu              1,78       1,78      khớp
VCSH / Tổng tài sản              0,36       0,36      khớp
ROE — cơ sở CUỐI KỲ              21,8      20,51      lệch 1,29  → LOẠI
ROE — cơ sở BÌNH QUÂN            21,8      21,83      khớp       → CHỌN
ROA — cơ sở CUỐI KỲ               7,6       7,39      lệch 0,21  → LOẠI
ROA — cơ sở BÌNH QUÂN             7,6       7,59      khớp       → CHỌN
```

Đây là bằng chứng cho tham số `co_so` của `traTySo`: **CTR dùng cơ sở bình quân**,
và điều đó được chứng minh bằng số chứ không phải suy đoán.

Tỷ số nào **không đối chiếu được** (CTR không công bố) thì **không đưa vào tool** —
để `khongHoTro` bắt. Thà từ chối còn hơn đưa một con số không kiểm được.

---

## 5. Quyết định lược đồ chốt hôm nay

**`financial_figures`**
- bảng **dọc**, mỗi con số một dòng
- hai cột ngày `tu_ngay` / `den_ngay` — phân biệt thời điểm / thời kỳ, và quý
  riêng / luỹ kế (674 trang có cột luỹ kế song song)
- `gia_tri BIGINT`, đơn vị **đồng**, âm cho dự phòng / hao mòn / dòng chi
- dẫn chứng `tai_lieu_id` + `trang` ghi **lúc nạp**, không đi tìm lúc trả lời
- `trang_thai` + `thay_the_id` cho ca trình bày lại 19,05 tỷ
- **khoá duy nhất qua cột sinh**: mỗi kỳ một dòng hiệu lực, dòng cũ vẫn giữ được

```sql
khoa_hieu_luc VARCHAR(140) GENERATED ALWAYS AS (
    IF(trang_thai = 'hieu_luc',
       CONCAT_WS('|', ma_noi_bo, tu_ngay, den_ngay, pham_vi), NULL)) STORED
```

**Giả định đang áp dụng — một kỳ = MỘT dòng.** Tổng tài sản 31/12/2024 có ở 10
tài liệu; chọn nguồn theo thứ tự *báo cáo năm kiểm toán → giữa niên độ soát xét
→ cột so sánh kỳ sau*, cùng bậc thì ưu tiên `la_ban_chinh = 1`. Đổi sang 10 dòng
thì phải đổi khoá này, và phải đổi **trước khi nạp**.

**`documents` — 4 cột trạng thái nạp (`V8`)**

Không phải nhật ký. Là **trạng thái hiện tại**, một dòng mỗi tài liệu, ghi đè:

```
ngay_ocr  ·  ngay_trich_so  ·  ngay_embed  ·  so_chunk
```

Mỗi bước chỉ chạy khi cột tương ứng còn `NULL`. Nhờ vậy luồng nạp **chạy lại
không sinh trùng** và **bỏ dở thì chạy tiếp đúng chỗ**.

**Không tạo chỉ số nào ngoài khoá và khoá ngoại.** Khoá ngoại đã tự sinh chỉ số
trên `ma_noi_bo`; lọc theo mã còn ~60 dòng trong ~7.000, quét nốt mất vài micro
giây. Thêm khi **đo** thấy chậm, không thêm vì đoán.

---

## 6. Bốn lỗi đã mắc hôm nay — ghi lại vì dễ tái phạm

### 6.1 Collation: kiểm ràng buộc DB bằng luật của Python

`V5` gãy ở dòng 81, báo trùng khoá. Nguyên nhân:

```
01_bao_cao_tai_chinh/2022/rieng/BCTC-rieng      ┐ Python: KHÁC NHAU
01_bao_cao_tai_chinh/2022/rieng/BCTC-riêng      ┘ MySQL utf8mb4_unicode_ci: GIỐNG
```

`_unicode_ci` là collation cho **văn bản** — bỏ qua cả dấu lẫn hoa/thường.
Đường dẫn và mã tài liệu là **định danh**, phải dùng `utf8mb4_bin`.

> **Kiểm ràng buộc của DB thì phải kiểm bằng luật của DB, không phải bằng ngôn
> ngữ đang viết script.**

Kéo theo: mọi cột khoá ngoại trỏ về `documents.tai_lieu_id` cũng **phải khai
`COLLATE utf8mb4_bin` tường minh**, nếu không MySQL từ chối tạo khoá ngoại (errno 3780).

### 6.2 Cờ trạng thái nhét vào cột thuộc khoá chính

Ghi `TT200?` vào cột `che_do` khi chế độ suy từ năm → `("TT200", B01, 100)` và
`("TT200?", B01, 100)` thành hai dòng cho cùng một thứ. 262 cặp gộp còn 250 sau
khi tách cờ ra cột riêng `che_do_suy_doan`.

> **Đừng bao giờ nhét cờ trạng thái vào một cột thuộc khoá chính.**

### 6.3 Ngữ nghĩa cờ đặt sai làm cảnh báo mất giá trị

`che_do_suy_doan` ban đầu nghĩa là *"có bất kỳ tài liệu nào suy từ năm"* → 74
dòng bị gắn cờ, trong đó có dòng gặp 78 lần mà chỉ 1 lần không đọc được thông tư.
Sửa thành *"KHÔNG tài liệu nào đọc được thông tư"* → còn **0**.

Tương tự với `can_soat` của `documents`: gắn cờ cho mọi ca "kỳ suy từ ngày" thì
được 74 dòng, nhưng suy từ 31/12 → năm, 31/03 → Q1, 30/09 → Q3 là **không thể
sai**; chỉ 30/06 mới mơ hồ (bán niên hay quý 2). Siết lại còn **4**.

> Cờ cảnh báo mà bật cho cả trường hợp chắc chắn đúng thì không ai đọc nó nữa.

### 6.4 `\b` trong heredoc bị ghi thành ký tự backspace

Bốn mẫu regex nhận kỳ từ tên tệp khớp **rỗng** suốt, vì `\b` bị ghi thành `\x08`.
Hậu quả rất lớn — mọi tài liệu đều suy kỳ từ ngày:

```
              trước    sau
Q2               0      16      ← báo cáo quý 2 bị nhận nhầm thành bán niên
Q4               0       6      ← báo cáo quý 4 bị nhận nhầm thành báo cáo năm
cần soát        74       4
"bản trùng kỳ"  30       8      ← 22/30 cụm là do phân loại sai, không phải trùng thật
```

Nếu để nguyên, báo cáo quý 4 bị coi là bản phụ của báo cáo năm và **bị loại khỏi
trích dẫn**.

Sửa xong lại lộ tiếp: `\b` coi `_` là ký tự từ nên `BCTC_HN_Q4` vẫn trượt — phải
dùng `(?<![A-Za-z0-9])` thay cho `\b`.

> Đây là lần **thứ ba** heredoc làm hỏng chuỗi thoát trong dự án này.
> **Sửa mã bằng công cụ ghi tệp trực tiếp, đừng qua heredoc.**

---

## 7. Ba phép kiểm phải chạy trước khi giao migration

```
✔ cột trong DDL khớp cột trong INSERT          (đã bắt được so_trang / tong_so_trang)
✔ không thiếu dấu ; · không nháy đơn lẻ
✔ khoá duy nhất kiểm theo LUẬT COLLATION của MySQL, không theo Python
✔ khoá ngoại khớp cả KIỂU lẫn COLLATION với cột được trỏ tới
✔ không còn tệp migration tên cũ sót lại      (hai bản V4 làm Flyway từ chối khởi động)
```

Ba cái cuối đều sinh ra từ lỗi thật hôm nay.

---

## 8. Việc kế tiếp

```
1. script trích 330 trang → financial_figures     ← việc lớn nhất còn lại
2. ba chốt kiểm trong common/validation
3. V9 query_audit                                  ← cần cho chương đánh giá
4. sổ công thức (sau khi financial_figures có dữ liệu)
```
