# Luồng hỏi đáp — v1.0

*Buổi thảo luận: 2026-09-28 · Ghi lại: 2026-09-29 · Tài liệu mới*

Ghi lại buổi bàn về **kiến trúc luồng hỏi đáp**: hai nhánh DB/RAG, bảng ánh xạ mã,
thiết kế tool, dẫn chứng, và ba chốt kiểm. Viết theo dạng **hỏi — đáp** vì phần lớn
nội dung sinh ra từ các câu hỏi trong buổi, và để lần sau đọc lại tìm được nhanh.

Mọi con số trong tài liệu này đều **đo thật trên kho `data/ocr/CTR`** hoặc **gọi thật
lên API Viettel AI**, không phải ước lượng.

---

## 0. Bốn cột trụ

```
1. BẢNG ÁNH XẠ    lúc nạp   →  quyết định con số nào có nghĩa gì, lưu dưới tên gì
                  lúc build →  sinh ra enum cho tool

2. LLM            mỗi vòng  →  chọn tool nào (chính là chọn nhánh)
                               "cả hai nhánh" do VÒNG LẶP đẻ ra, không phải chế độ riêng

3. DẪN CHỨNG      lúc nạp   →  ghi kèm vào dòng DB và payload chunk
                               lúc trả lời không đi tìm, chỉ gom và khử trùng

4. MÃ JAVA KIỂM   ba chốt   →  lúc nạp · lúc gọi tool · lúc trả lời
```

**Ba cột đầu làm hệ thống chạy được; cột 4 làm nó dùng được.**

---

## 1. Bảng ánh xạ mã

### 1.1 Vì sao khoá bắt buộc đủ ba cột

Quét 5.569 trang tiếng Việt, đếm mỗi mã số ứng với bao nhiêu tên khác nhau:

```
MÃ 10  →  30 tên khác nhau, 233 lần xuất hiện
      94 lần  - TĂNG/GIẢM HÀNG TỒN KHO              ← B03
      76 lần  DOANH THU THUẦN BÁN HÀNG...           ← B02
      16 lần  DOANH THU THUẦN VỀ BÁN HÀNG...        ← B02 (OCR khác chữ)
       4 lần  GIẢI PHÁP TÍCH HỢP                    ← bảng khác hẳn, rác
       3 lần  QUYỀN BẢO MẬT THÔNG TIN KHÁCH HÀNG    ← rác

MÃ 01  →  12 tên khác nhau, 209 lần
      94 lần  DOANH THU BÁN HÀNG VÀ CUNG CẤP DỊCH VỤ   ← B02
      88 lần  LỢI NHUẬN TRƯỚC THUẾ                     ← B03
```

Hai loại đụng độ, **mỗi loại đòi một cột**:

| Loại đụng độ | Ví dụ | Cột cần thêm |
|---|---|---|
| Cùng mã, khác **mẫu biểu** | mã 01: B02 là doanh thu, B03 là LNTT | `mau_bieu` |
| Cùng mã, khác **chế độ** | mã 136: TT200 là phải thu, TT99 là dự phòng | `che_do` |

Số liệu chứng minh vế thứ hai, quét trên cùng kho:

```
MÃ 136:  92 lần  PHẢI THU NGẮN HẠN KHÁC              ← TT200
          6 lần  DỰ PHÒNG PHẢI THU NGẮN HẠN KHÓ ĐÒI  ← TT99
          1 lần  PHẢI THU NGÂN HÀNG KHÁC             ← OCR đọc sai "ngắn hạn"
MÃ 270:  94 lần  TỔNG CỘNG TÀI SẢN                   ← TT200
          6 lần  TÀI SẢN DÀI HẠN KHÁC                ← TT99
MÃ 280:   6 lần  TỔNG CỘNG TÀI SẢN                   ← TT99
```

Con số **6** lặp lại ba lần — đó chính là bộ tài liệu 2026 dùng TT99.

Tra bằng `"10"` một mình thì ra **30 kết quả**, script không quyết được. Bảng ánh xạ
**không có dòng nào khoá bằng mã đơn lẻ**; ba cột đầu **cùng nhau** mới là khoá.

### 1.2 Ba cột đến từ ba tầng khác nhau

Không cùng nằm trên một dòng — đây là chỗ hay nhầm nhất:

```
┌── TẦNG TÀI LIỆU ──────────── đọc MỘT LẦN cho cả tệp
│   che_do = TT200      (thuyết minh "Chế độ kế toán áp dụng", hoặc suy từ năm)
│   ┌── TẦNG BẢNG ─────────── đọc MỘT LẦN cho mỗi bảng
│   │   mau_bieu = B02   (tiêu đề "BÁO CÁO KẾT QUẢ HOẠT ĐỘNG KINH DOANH")
│   │   ┌── TẦNG DÒNG ────── đọc cho TỪNG dòng
│   │   │   ma = 10
```

```python
che_do = doc_che_do(tai_lieu)          # 1 lần / tài liệu
for bang in cac_bang(trang):
    mau_bieu = doc_mau_bieu(bang)      # 1 lần / bảng
    if mau_bieu is None:
        continue                       # ← cửa chặn rác "GIẢI PHÁP TÍCH HỢP"
    for ma, ten, gia_tri in bang:
        ma_noi_bo = ANH_XA.get((che_do, mau_bieu, ma))
        if ma_noi_bo is None:
            ghi_nhat_ky_chua_biet(...)  # KHÔNG bỏ qua âm thầm
            continue
        luu(...)
```

`mau_bieu` làm **hai việc**: phân biệt nghĩa, và chặn rác từ bảng không liên quan.

### 1.3 Vì sao nó KHÔNG phải bảng để trích dẫn

Một dòng trong DB có **hai nhóm cột tách biệt**:

```
┌─ NGHĨA: con số này LÀ GÌ ─────────────┐
│ ma_noi_bo  : DOANH_THU_THUAN          │  ← bảng ánh xạ cho ra
│ gia_tri    : 13939517269142           │
│ ngay       : 2025-12-31               │
│ pham_vi    : hop_nhat                 │
└───────────────────────────────────────┘
┌─ NGUỒN: lấy Ở ĐÂU ra ─────────────────┐
│ tai_lieu_id: CTR-BCTC-HN-2025-N       │  ← script tự biết khi đọc tệp
│ trang      : 8                        │     KHÔNG liên quan bảng ánh xạ
└───────────────────────────────────────┘
```

Trích dẫn là nhóm dưới, script tự ghi được. Bảng ánh xạ lo nhóm trên: nó là **từ điển**
dịch ký hiệu trên giấy thành khái niệm hệ thống hiểu được.

Năm chỗ dùng: ETL · sinh enum cho tool · truy vấn xuyên năm bằng một `WHERE` ·
công thức kiểm tra viết một lần cho mọi chế độ · tên hiển thị.

### 1.4 Dùng lúc NẠP, không dùng lúc HỎI

```
LÚC NẠP  (offline, 1 lần)
   (TT200, B02, 10)  ──tra bảng ánh xạ──►  DOANH_THU_THUAN
    ba thứ vào                              một thứ ra

LÚC HỎI  (mỗi câu hỏi)
   mô hình chỉ điền:  ma_chi_tieu = "DOANH_THU_THUAN"
   KHÔNG cần biết TT nào, mẫu biểu nào, mã số mấy
```

> **Bảng ánh xạ có mặt để lúc hỏi chỉ còn phải biết MỘT thứ, thay vì ba.**

Nếu lúc hỏi vẫn phải cung cấp đủ ba thì bảng đó vô dụng — cái rối của chuẩn mực kế toán
bị đẩy sang cho mô hình và người dùng, và hỏi xuyên năm sẽ gãy (2019 là TT200, 2026 là TT99).

Hai việc nhẹ còn lại, đều đi chiều ngược (một mã → một tên):
lúc **build** sinh `enum` Java; lúc **trả lời** lấy `ten_chuan` để in chữ.

### 1.5 Hai bảng khác nhau, đừng lẫn

| | `anh_xa_chi_tieu` | `chi_tieu` |
|---|---|---|
| Là gì | **danh mục** (từ điển) | **chuỗi thời gian** (số liệu) |
| Ai ghi | người, bằng tay | script, tự động |
| Số dòng | ~100–300 | ~7.000 và tăng |
| Đổi khi nào | rất hiếm, khi có thông tư mới | mỗi lần nạp báo cáo mới |
| Truy vấn người dùng đọc | không (chỉ lấy `ten_chuan`) | **đọc bảng này** |

> Bảng ánh xạ **không chứa dữ liệu**. Nó là công cụ để ghi dữ liệu vào bảng kia cho đúng chỗ.

### 1.6 Ai viết, và quét bằng gì

**Không dùng LLM.** Cả dây chuyền nạp B01/B02/B03 là Java thuần:

```
| 10 | 3. Doanh thu thuần bán hàng... | | 13.939.517.269.142 | 12.609.519.560.081 |
   │ tách ô bằng split, giữ ô rỗng            │                  │     ← Java
   │ ANH_XA.get(TT200, B02, "10") → DOANH_THU_THUAN               │     ← HashMap
   │ tiêu đề trang → mẫu biểu · tiêu đề cột → ngày                │     ← regex
   │ docSo("13.939.517.269.142") → long ───────┘                  │     ← Java
   ▼
INSERT × 2  (2025-12-31 và 2024-12-31)
```

Tra bảng ánh xạ = `map.get(key)`, **không phải gọi mô hình**.

Kiểm được ngay bằng số học, trên chính trang 8 của `BCTC-HN-2025_full`:

```
mã 01 − mã 02 = mã 10 :  13.939.517.269.142 − 0 = 13.939.517.269.142   ✔
mã 10 − mã 11 = mã 20 :  13.939.517.269.142 − 12.957.576.695.759
                       = 981.940.573.383                               ✔
```

LLM chỉ tham gia ở **~300 trang bảng thuyết minh** (bố cục mỗi báo cáo mỗi khác, không có
cột mã), và ở đó cũng chỉ trả JSON theo khuôn định sẵn rồi vẫn phải qua kiểm.

**Bảng ánh xạ thì người viết tay.** Việc quyết định "mã 10 của B02 gọi là `DOANH_THU_THUAN`"
là quyết định nghiệp vụ, phải ổn định suốt đời dự án. Để LLM đặt thì lần sau chạy lại nó
đặt tên khác, toàn bộ DB lệch.

**Máy quét = script regex ~30 dòng, 4 giây, 0 đồng** — không phải mô hình. Lý do:
đếm phải chính xác; chạy lại phải ra y hệt; nền móng phải lặp lại được.

Phạm vi quét: đọc cả 6.690 trang nhưng **chỉ lấy từ ~244 trang** nhận diện được là
B01/B02/B03. Không chặn thì từ điển nhiễm rác (xem mã 10 ở 1.1).

### 1.7 Bảng lớn lên nhờ được dùng

Đừng chép từ thông tư — quá nửa danh mục TT200 không bao giờ xuất hiện trong tài liệu CTR.
**Quét trước, điền sau**, rồi lặp:

```
vòng 1  điền ~40 cặp hay gặp nhất   → chạy script → nhật ký: 23 cặp chưa biết
vòng 2  điền thêm 23 cặp            → chạy lại    → nhật ký: 4 cặp chưa biết
vòng 3  2 cặp thật, 2 cặp rác OCR   → điền 2, bỏ 2 → sạch
```

Đúng tinh thần **chia dọc** trong `hoc-tap_v1.1.md`: có kết quả chạy được ngay từ vòng 1,
mỗi vòng thất bại đều chỉ đích danh chỗ thiếu.

Cột **số lần gặp** có hai việc: lọc nhiễu (dòng 1 lần thường là lỗi OCR — xem mã 136),
và cho thứ tự ưu tiên (40 cặp đầu phủ ~95% số liệu).

### 1.7b Kết quả chốt (29/9) — bảng ánh xạ đã dựng xong

```
Tài liệu dùng để trích :  130   (bỏ 148 — BCTN, nghị quyết, tờ trình…)
Trang B01/B02/B03      :  330   (B01 161 · B02 81 · B03-GT 84 · B03-TT 4)
Cặp (chế độ, mẫu, mã)  :  250   — đặt tên 250/250
Mã nội bộ riêng        :  123   ← chính là enum của tool
Phủ                    :  7.685 / 7.685 lần xuất hiện
Khoá trùng             :  0
```

Tệp: `devtools/ra/ma_chi_tieu_de_xuat.csv` · bộ quét `devtools/quet_ma_chi_tieu.py`
· bảng tên `devtools/de_xuat_ma_noi_bo.py` (sửa ở đây rồi chạy lại).

**Ba phát hiện về chuẩn mực**

1. **TT99 đổi tên biểu B01**: *"Bảng cân đối kế toán"* → *"Báo cáo tình hình tài chính"*.
   Lần quét đầu bỏ sót toàn bộ bảng cân đối 2026 vì lý do này.
2. **TT99 đánh lại mã hàng loạt**, 26 ca đụng độ. B02 mã 21–27 dịch nguyên khối
   (21 doanh thu TC → 22; 22 chi phí TC → 23; 23 lãi vay → 24).
   B01 dịch ở các khối 240–272, 313–323, 338–339, 420.
3. **B03 phải tách hai phương pháp**: gián tiếp mã 01 = *Lợi nhuận trước thuế*,
   trực tiếp mã 01 = *Tiền thu từ bán hàng*. `mau_bieu` có 4 giá trị:
   `B01 · B02 · B03-GT · B03-TT`.

**Bảng ánh xạ hoạt động đúng chức năng**: 37 mã nội bộ gộp nhiều mã gốc, ví dụ
`TONG_TAI_SAN ← TT200/B01/270 + TT99/B01/280`. Hỏi xuyên 2019–2026 là **một** `WHERE`.

**Quy tắc đặt tên**: tên mô tả bằng tiếng Việt không dấu, UPPER_SNAKE. Khi hai chỉ tiêu
anh em dễ lẫn thì **hậu tố `_M<mã>`** thay cho tính từ mơ hồ:

```
HANG_TON_KHO_M140    mã 140 — thuần, đã trừ dự phòng
HANG_TON_KHO_M141    mã 141 — giá gốc
VON_CHU_SO_HUU_M400  mã 400 — khối D, tổng
VON_CHU_SO_HUU_M410  mã 410 — nhóm I, chỉ TT200
TIEN_M111            mã 111 — tiền mặt và tiền gửi (phân biệt với 110, 112)
```

**Sáu quyết định đặt tên (29/9)** — ba câu đầu quyết bằng số liệu, không bằng cảm tính:

| # | Quyết | Bằng chứng |
|---|---|---|
| 1 | `CHI_PHI_DI_VAY` dùng cho **cả** TT200/B02/23 lẫn TT99/B02/24 | 6T/2025 = 43.025.726.263 ở cả hai cách gọi |
| 2 | `CHI_PHI_CHO_PHAN_BO_NGAN_HAN` / `_DAI_HAN` dùng cho **cả** 151,261 và 161,271 | TT200/2025 cuối kỳ = TT99/2026 đầu năm, khớp từng đồng, cùng TM 14 |
| 5 | `THANG_DU_VON` dùng cho **cả** hai | mã 412 = (15.000.000) liên tục qua mốc đổi chế độ |
| 3 | `HANG_TON_KHO_M140` / `_M141` | — |
| 4 | `VON_CHU_SO_HUU_M400` / `_M410` | — |
| 6 | mã 252 **không tồn tại** sau khi loại BCTN | xem dưới |

Nguyên tắc rút ra cho câu 1·2·5: **số liệu liên tục qua mốc đổi chế độ ⇒ cùng một tài
khoản, chỉ đổi tên gọi ⇒ một mã nội bộ.** Nếu nội hàm đổi thì chuẩn mực buộc phải trình
bày lại; CTR không trình bày lại. Tách đôi sẽ khiến `WHERE ma_noi_bo='CHI_PHI_DI_VAY'`
**im lặng trả rỗng** cho 2019–2025 — đúng kiểu sai mà cả kiến trúc này sinh ra để chặn.
Chữ gốc từng chế độ vẫn giữ ở cột `ten_tren_bao_cao` để hiển thị đúng từ của kỳ đó.

**Chỉ trích số từ BCTC gốc và văn bản đính chính.** Báo cáo thường niên **chép lại** bảng
của BCTC nhưng tự đánh lại mã (BCTN 2022 ghi 250/251/252 cho thứ BCTC ghi 260/261/262,
cùng giá trị 18.131.162.127). Trích cả hai sẽ sinh hai dòng cho cùng chỉ tiêu cùng ngày.
BCTN vẫn nằm nguyên trong Qdrant — chỉ không dùng để trích số.

**Mã bị bãi bỏ ⇒ báo rõ, không thay ngầm.** `VON_CHU_SO_HUU_M410` chỉ có ở TT200.
Hỏi nó cho kỳ 2026 thì tool trả:

```json
{"trang_thai": "chi_tieu_khong_ap_dung_ky_nay",
 "ly_do": "TT99 (từ 2026) bỏ mã 410",
 "goi_y": {"ma_noi_bo": "VON_CHU_SO_HUU_M400", "ten": "Vốn chủ sở hữu (mã 400)"}}
```

Cột `thay_the_bang` trong bảng ánh xạ giữ gợi ý này. Mô hình **không được tự thay** —
bài học EBITDA ở 7.1.

Phân bố 123 mã theo chế độ: **109 có ở cả hai** · 12 chỉ TT200 · 2 chỉ TT99.
Trong 14 mã lệch, chỉ `VON_CHU_SO_HUU_M410` là do bãi bỏ; số còn lại vắng vì CTR không
phát sinh giao dịch (ví dụ `LOI_ICH_CDKKS`: mã 62 của B02 năm 2026 có mặt, giá trị 0 —
CTR không còn cổ đông thiểu số). Hai loại này phải trả **hai thông điệp khác nhau**:
*"chỉ tiêu không áp dụng cho kỳ này"* vs *"không có số liệu kỳ này"*.

**Hai lỗi bộ quét đã sửa, ghi lại vì dễ tái phạm**

| Lỗi | Hậu quả | Sửa |
|---|---|---|
| Lọc bỏ tên ngắn dưới 5 ký tự | mã **111 "Tiền"** mất sạch **77 lần** | hạ ngưỡng còn 3 |
| Cho `?` vào giá trị `che_do` khi suy từ năm | khoá chính nhân đôi: `TT200` và `TT200?` thành hai dòng cho cùng một thứ (262 → 250 cặp sau khi gộp) | độ tin cậy ra **cột riêng** `che_do_suy_doan` |

Bài học chung: **đừng bao giờ nhét cờ trạng thái vào một cột thuộc khoá chính.**

**Chi phí**: 123 mã, tên dài trung bình 19 ký tự ⇒ enum khoảng **900 token**, gửi kèm mọi
lời gọi có mang nó. Đây là con số để đo ở mục 4.5 — **đo rồi hãy quyết** có rút gọn không.

### 1.8 Lược đồ `chi_tieu`: bảng DỌC, không phải bảng NGANG

Sau khi tra bảng ánh xạ ra `DOANH_THU_THUAN`, con số đi đâu?

```
NGANG (đừng làm)
nam  │ doanh_thu_thuan │ tong_tai_san │ von_chu_so_huu │ ... 100 cột
2019 │ ...             │ ...          │ ...            │

DỌC (làm cái này)
ma_noi_bo        │ ngay       │ pham_vi  │ gia_tri       │ tai_lieu_id  │ trang
DOANH_THU_THUAN  │ 2019-12-31 │ hop_nhat │ ...           │ CTR-...-2019 │ 7
TONG_TAI_SAN     │ 2019-12-31 │ hop_nhat │ 2464000613941 │ CTR-...-2019 │ 3
```

Mỗi con số **một dòng**, không phải một ô. Lý do, xếp theo sức nặng thật:

1. **Dẫn chứng theo từng con số.** Doanh thu ở trang 7, tổng tài sản ở trang 3 — bảng ngang
   thì phải thêm `doanh_thu_thuan_trang`, `tong_tai_san_trang`, ... → 200 cột. Ca trình bày
   lại (`trang_thai`, `thay_the_id` ở 2.3) thì chịu hẳn.
2. **Thêm chỉ tiêu phải `ALTER TABLE`** — mỗi lần điền thêm dòng vào bảng ánh xạ là một lần
   đổi lược đồ.
3. **Thưa** — báo cáo quý không có đủ chỉ tiêu như báo cáo năm → `NULL` đầy bảng.
4. **Chuỗi SQL bất biến** — lợi thế thật, nhưng là thứ tư, không phải thứ nhất (xem dưới).

Bảng ngang vẫn **hiện được** cho người dùng: truy vấn dọc rồi Java xoay thành lưới.
Dọc → ngang thì dễ; ngang → dọc thì mất thông tin, không quay lại được.

#### Đính chính (29/9)

Trong buổi, sư phụ nói bảng ngang *"phá hỏng chữ an toàn, mất sạch lớp bảo vệ"*. **Nói quá.**
Bảng ngang vẫn chặn được tiêm nhiễm bằng danh sách trắng:

```java
if (!COT_HOP_LE.contains(ten)) throw new CotLaException(ten);
String sql = "SELECT " + ten + " FROM chi_tieu WHERE nam = ?";
```

Chỗ khác nhau thật là **loại bảo đảm**, không phải có hay không:

| | Bảng dọc | Bảng ngang + danh sách trắng |
|---|---|---|
| Chuỗi SQL | **hằng số lúc biên dịch** | ghép lúc chạy, có kiểm |
| Chứng minh an toàn | nhìn là thấy | phải đọc và tin phép kiểm |
| Người sau sửa mã | không phá được | quên `if` một chỗ là thủng |

Và danh sách trắng đó **chính là cái enum** — làm thêm một lớp mà không được gì mới.
Bảng dọc không phải cách *duy nhất* để an toàn; nó là cách khiến an toàn là **mặc định**
chứ không phải điều phải nhớ.

#### Vì sao tên cột không tham số hoá được

Prepared statement chạy hai pha: gửi **chuỗi SQL** (DB phân tích cú pháp, lập kế hoạch —
phải biết đọc cột nào), rồi mới gửi **tham số** (đi kênh dữ liệu, không bao giờ được phân
tích như mã). Dấu `?` là chỗ trống cho một *giá trị*, không thể là tên cột.

```sql
SELECT ? FROM chi_tieu;    -- bind "gia_tri"
→ trả về chuỗi "gia_tri" lặp mỗi dòng, KHÔNG phải cột gia_tri
```

```sql
WHERE ma_noi_bo = ?
      └────┬───┘   └┬┘
   TÊN CỘT          GIÁ TRỊ
   gõ cứng trong    mô hình điền,
   chuỗi SQL        đi kênh dữ liệu
```

> **`?` chỉ thay được thứ là dữ liệu. Mọi thứ thuộc cấu trúc câu lệnh phải nằm cứng
> trong chuỗi:** tên bảng · tên cột · cột trong `ORDER BY` · `ASC`/`DESC` · toán tử · từ khoá.

Ngoại lệ trông giống ghép chuỗi nhưng vẫn an toàn — `IN` với số phần tử thay đổi:

```java
String cho = String.join(",", Collections.nCopies(ma.size(), "?"));
String sql = "SELECT ... WHERE ma_noi_bo IN (" + cho + ")";
```

Chỉ ghép **dấu `?`**, không mảnh nào đến từ mô hình.

### 1.9 Kỳ: hai cột ngày, và quy tắc quý riêng / luỹ kế

**Phát hiện (29/9).** Báo cáo quý có **bốn** cột số, không phải hai.
`BCTC-HN-Quy-3.2019/trang_005.md`:

```
| Mã số CHỈ TIÊU | TM | Số liệu phát sinh trong kỳ | Số liệu lũy kế từ đầu năm |
|                |    | Quý 3/2019 | Quý 3/2018    | Quý 3/2019 | Quý 3/2018   |
```

Cùng `DOANH_THU_THUAN`, cùng ngày kết thúc 30/09/2019, cùng `hop_nhat` — vẫn **hai giá
trị khác nhau**. Đo trên kho: **674 trang / 110 tài liệu** có cụm "luỹ kế".

Nên `WHERE nam = 2019` hỏng, mà `WHERE ngay = '2019-09-30'` **cũng hỏng**.

Gốc rễ: hai loại số khác bản chất.

| | Ví dụ | Đo cái gì | Cần mấy ngày |
|---|---|---|---|
| **Thời điểm** | Tổng tài sản | lát cắt tại một mốc | 1 |
| **Thời kỳ** | Doanh thu, lợi nhuận | tích luỹ giữa hai mốc | 2 |

**Lược đồ: hai cột ngày.**

```
ma_noi_bo       │ tu_ngay    │ den_ngay   │ pham_vi  │ loai_so
────────────────┼────────────┼────────────┼──────────┼────────────
TONG_TAI_SAN    │ 2019-09-30 │ 2019-09-30 │ hop_nhat │ thoi_diem
DOANH_THU_THUAN │ 2019-07-01 │ 2019-09-30 │ hop_nhat │ thoi_ky     ← quý 3 riêng
DOANH_THU_THUAN │ 2019-01-01 │ 2019-09-30 │ hop_nhat │ thoi_ky     ← luỹ kế 9 tháng
DOANH_THU_THUAN │ 2019-01-01 │ 2019-12-31 │ hop_nhat │ thoi_ky     ← cả năm
```

Khoá duy nhất: `(ma_noi_bo, tu_ngay, den_ngay, pham_vi)`. Bốn dòng không đụng nhau.
`loai_so` suy ra được từ `tu_ngay == den_ngay`, nhưng ghi rõ thì chốt kiểm bắt được ngay
khi script trích sai loại.

**QUY TẮC (người dùng chốt 29/9): hỏi rõ thì trả rõ; không nói gì thì mặc định quý riêng.**

```
"Q3/2019"     → 2019-07-01 … 2019-09-30    quý riêng   ← MẶC ĐỊNH khi chỉ nói "quý 3"
"9T/2019"     → 2019-01-01 … 2019-09-30    luỹ kế
"2019"        → 2019-01-01 … 2019-12-31    cả năm
"30/09/2019"  → 2019-09-30 … 2019-09-30    thời điểm
```

| Người dùng nói | `ky` |
|---|---|
| "quý 3", "Q3", "quý III/2019" | `Q3/2019` |
| "9 tháng đầu năm", "luỹ kế đến quý 3", "từ đầu năm đến hết quý 3" | `9T/2019` |
| "năm 2019", "cả năm 2019" | `2019` |
| "tại 30/9", "cuối quý 3" *(chỉ tiêu thời điểm)* | `30/09/2019` |

Hai ca biên:

1. **Chỉ tiêu thời điểm thì phân biệt này vô nghĩa** — không có "tổng tài sản quý 3 riêng".
   Mô hình điền `traChiTieu(TONG_TAI_SAN, "Q3/2019")` thì Java **tự quy về thời điểm**,
   không báo lỗi: `if (loaiSo == THOI_DIEM) tuNgay = denNgay;`
2. **Hỏi dạng không có trong báo cáo thì nói không có** — đừng trả luỹ kế thay cho quý riêng.
   Đúng bài học EBITDA (7.1). Trả `{"trang_thai":"khong_co_ky_nay","co_san":["9T/2019"]}`.

**Câu trả lời bắt buộc nói rõ kỳ:** *"Doanh thu thuần **quý 3/2019 (01/07–30/09)** đạt ... đ"*.
Hai con số chênh nhau rất xa, người đọc phải biết ngay mình cầm cái nào.

> **Bẫy công cụ:** `grep` trên Windows nuốt ký tự tiếng Việt trong mẫu tìm — lần đầu quét
> "luỹ kế" ra **0 trang**, đếm lại bằng Python ra **674**. Trong dự án này dùng Python
> hoặc `Grep` tool cho chữ tiếng Việt, đừng dùng `grep` qua shell.

---

## 2. Vì sao cần DB khi đã có tài liệu

### 2.1 Không phải đọc hai lần

```
LÚC NẠP (offline, 1 lần)        LÚC HỎI (mỗi câu hỏi)
─────────────────────────       ──────────────────────
đọc 6.690 trang                 KHÔNG đọc trang nào
tra bảng ánh xạ                 chỉ:  SELECT ... WHERE ...
ghi vào DB                      trả về 1 dòng, ~20ms
```

Tài liệu **không bị bỏ** — vẫn nằm nguyên trong Qdrant. DB là bản trích của một phần nhỏ.

### 2.2 Ba phép đo

**Đống rơm.** 175/5.569 trang có cụm "doanh thu thuần". RAG lấy top-20 rồi phải trúng
đúng trang, đúng dòng, đúng cột (báo cáo nào cũng có 2 cột: kỳ này / kỳ trước).

**Một con số nằm nhiều chỗ.** Tổng tài sản hợp nhất 31/12/2024 = `7.106.257.863.668`
xuất hiện ở **10 tài liệu**. RAG phải *chọn* 1 trong 10; DB có **một** dòng.

**Dòng "TỔNG CỘNG TÀI SẢN"** xuất hiện ở **94 chỗ** trong kho.

### 2.3 Ca chí mạng — trình bày lại 31/12/2022

`data/ocr/CTR/04_cong_bo_thong_tin/2024/dinh_chinh/`
`20240110-CTR-Trinh-bay-lai-so-lieu-BCTC-theo-TB-cua-KTNN/trang_003.md`

```
BẢNG CÂN ĐỐI KẾ TOÁN HỢP NHẤT TẠI NGÀY 31/12/2022

| Mã  | TÀI SẢN            | Số sẽ trình bày lại | Số đã công bố     | Chênh lệch     |
| 270 | TỔNG CỘNG TÀI SẢN  | 6.060.012.376.089   | 6.040.964.527.680 | 19.047.848.409 |
| 440 | TỔNG CỘNG NGUỒN VỐN| 6.060.012.376.089   | 6.040.964.527.680 | 19.047.848.409 |
```

CTR phải trình bày lại theo thông báo Kiểm toán Nhà nước. **Lệch 19,05 tỷ đồng** cho cùng
một ngày, cùng một chỉ tiêu. Cả hai con số đang nằm rải trong kho:

| Giá trị | Số tài liệu chứa |
|---|---|
| `6.040.964.527.680` (đã công bố) | 7 |
| `6.060.012.376.089` (trình bày lại) | 4 |

RAG thuần không quyết được — 11 trang giống nhau như đúc, và 7 ăn 4 là trả về con số
**đã bị thay thế**. Với DB thì đây là quy tắc viết được:

```
ma_noi_bo   : TONG_TAI_SAN
ngay        : 2022-12-31
gia_tri     : 6060012376089          ← bản sau trình bày lại
trang_thai  : da_trinh_bay_lai
thay_the_id : <dòng cũ 6.040.964.527.680>
ly_do       : TB Kiểm toán Nhà nước
```

Câu trả lời đúng phải là: *"6.060.012.376.089 đ (số đã trình bày lại theo yêu cầu KTNN;
số công bố ban đầu 6.040.964.527.680 đ, chênh 19,05 tỷ)"*.

### 2.4 Hai việc RAG không làm được

- **Tính toán**: "tăng bao nhiêu %" cần phép chia trên số 13–14 chữ số
- **Gộp theo chiều**: "liệt kê từng năm 2019–2026" — top-k không có khái niệm "đủ 8 năm"

### 2.5 Chia việc

| | **MySQL** | **Qdrant** |
|---|---|---|
| Cái gì | con số có mã, có ngày, có phạm vi | toàn bộ 6.690 trang, nguyên văn |
| Bao nhiêu | ~244 trang B01/B02/B03 + ~300 trang thuyết minh | 100% |
| Trả lời | *"Bao nhiêu?" "Tăng mấy %?" "Liệt kê các năm"* | *"Vì sao?" "Chính sách gì?"* |

**Cùng một trang dùng cho cả hai.** Trang 6 của `BCTC-HN-2025_full` vừa cho 30 dòng số
vào MySQL, vừa nằm nguyên trong Qdrant.

### 2.6 Ranh giới không phải "số vs chữ"

Mà là **con số đó có mã chuẩn hoá được không**:

| Câu hỏi | Có số | Đi đâu | Vì sao |
|---|---|---|---|
| "Tổng tài sản cuối 2024?" | ✔ | **DB** | có mã 270/280 |
| "Lãi suất khoản vay VCB?" | ✔ | **RAG** | mỗi khế ước một dòng, không có mã chuẩn |
| "Chính sách khấu hao?" | ✘ | **RAG** | văn bản thuần |
| "Tỷ lệ cổ tức 2024?" | ✔ | **DB** (sự kiện) | có ngày, có loại, chuẩn hoá được |

Chiều RAG thì chắc chắn ("vì sao / chính sách / điều kiện" → luôn RAG).
Chiều DB thì không — "bao nhiêu" vẫn có thể phải về RAG.

---

## 3. Chọn nhánh DB hay RAG

### 3.1 Không có bộ phân loại nào cả

Không có lớp `Router`, không có `if (cauHoi.chuaTuKhoaSo())`. Toàn bộ là:

```java
var msgs = new ArrayList<Message>(List.of(system, user));

for (int vong = 1; vong <= MAX_VONG; vong++) {
    var resp = chatModel.call(msgs, DANH_SACH_TOOL);   // gửi CẢ HAI tool

    if (resp.toolCalls().isEmpty()) return resp.content();

    msgs.add(resp.asAssistantMessage());
    for (var tc : resp.toolCalls()) {
        msgs.add(ToolMessage.of(tc.id(), toolRegistry.chay(tc)));
    }
}
throw new QuaSoVongException();
```

Không dòng nào quyết định DB hay RAG. **Biến RAG thành một tool nữa** thì việc "chọn nhánh"
tan biến — nó thành "chọn tool", mà mô hình vốn làm tốt.

### 3.2 Phân loại xảy ra TRƯỚC khi chạm dữ liệu

```
t0   câu hỏi tới
t1   gọi mô hình — CHỈ kèm danh sách tool, KHÔNG kèm dữ liệu   ← chưa chạm MySQL/Qdrant
t2   mô hình trả:  tra_chi_tieu(ma="DOANH_THU_THUAN", ...)     ← ĐÂY LÀ BƯỚC PHÂN LOẠI
t3   kiểm tham số                                              ← vẫn chưa chạm dữ liệu
t4   chạy SQL                                                  ← BÂY GIỜ mới chạm MySQL
```

**Bước phân loại và bước chọn tool là MỘT.** Kết quả phân loại chính là tên tool nó trả về:

```
trả "tra_chi_tieu"  ≡  phán "câu này thuộc loại DB"
trả "tim_van_ban"   ≡  phán "câu này thuộc loại văn bản"
```

Không cần thêm một lượt gọi để hỏi "câu này loại gì" — làm thế là trả tiền hai lần cho
cùng một câu hỏi. Lượt t1 đo được: **660 token vào, 217 ra, ~1 giây**, không đọc trang nào.

### 3.3 "Kết hợp" do vòng lặp đẻ ra

Không có chế độ kết hợp. Chỉ có nhiều vòng, mỗi vòng một lựa chọn đơn:

```
vòng 1  thấy: câu hỏi                      → tra_chi_tieu × 2
vòng 2  thấy: câu hỏi + 2 số               → "thiếu vì sao" → tim_van_ban
vòng 3  thấy: câu hỏi + 2 số + đoạn văn    → "đủ rồi" → viết trả lời
```

Ba cái tên DB / RAG / kết hợp là **cách mô tả kết quả sau khi chạy**, không phải cách hệ
thống hoạt động. Bỏ vòng lặp thì trường hợp kết hợp biến mất.

> **Viết mã cho một vòng, rồi cho nó lặp. Trường hợp kết hợp tự có.**

### 3.4 Ta quy định hay LLM quyết

```
TA QUY ĐỊNH     có tool nào · tham số gì · enum nào · mẫu SQL nào   → vẽ KHÔNG GIAN
LLM CHỌN        gọi tool nào · điền giá trị gì                      → chọn MỘT ĐIỂM
TA KIỂM         sai enum / sai ngày → TỪ CHỐI, không chạy SQL       → PHỦ QUYẾT
```

> **LLM không bao giờ viết SQL. Nó chỉ điền vào chỗ trống.**

```sql
SELECT gia_tri, ngay, tai_lieu_id, trang
FROM   chi_tieu
WHERE  ma_noi_bo = ?     -- LLM điền, phải thuộc enum
  AND  ngay      = ?     -- LLM điền, phải parse được
  AND  pham_vi   = ?     -- LLM điền, chỉ 'hop_nhat' | 'rieng'
```

Khác text-to-SQL ngây thơ: ở đó LLM có thể `DROP TABLE`, join sai, đọc bảng không được phép.
Ở đây nó **chỉ được cầm ba dấu `?`**.

Tóm một câu: **ta cầm luật, LLM cầm bút, ta đọc lại trước khi ký.**

---

## 4. Thiết kế tool

### 4.1 Một tool cho một *hình dạng truy vấn*

```
SAI:  traTongTaiSan(), traDoanhThu(), traVonChuSoHuu(), ...   → 100 tool
ĐÚNG: traChiTieu(maChiTieu, ...)                              → 1 tool, 100 giá trị enum
```

Chỉ tiêu là **tham số**. Hình dạng câu SQL mới quyết định số tool.
Mỗi hình dạng ứng với một mẫu SQL cố định trong `modules/dbquery/st8ments`.

### 4.2 Bộ tool đề nghị để khởi đầu

| Tool | Trả về | Mẫu SQL |
|---|---|---|
| `traChiTieu(ma, ky, phamVi)` | 1 dòng | `WHERE ma=? AND ngay=? AND pham_vi=?` |
| `lietKeChiTieu(ma, kyTu, kyDen, phamVi)` | N dòng **+ chênh lệch, % tính sẵn** | `WHERE ma=? AND ngay BETWEEN ? AND ?` |
| `traTySo(maTySo, ky, phamVi, coSo)` | 1 dòng + công thức | từ sổ công thức |
| `timVanBan(truyVan, nam, loaiTaiLieu, phamVi)` | k đoạn | Qdrant + lọc payload |
| `khongHoTro(chiTieuDuocHoi)` | — | lối thoát |

- `lietKeChiTieu` **gộp luôn việc so sánh** — "liệt kê 2019–2026" và "2025 tăng mấy % so 2024"
  cùng một hình dạng. Không cần tool `soSanh` riêng.
- **Java tự tính chênh lệch, đừng để mô hình tính.** Trả về sẵn
  `{"chenh_lech":2206238657195,"phan_tram":18.80}`. Điều này còn làm phép kiểm bám nguồn
  (mục 6) phủ kín được mọi con số.
- `traTySo` tách riêng vì có tham số `coSo` (cuối kỳ / bình quân). CTR dùng **bình quân**
  (ROE 30,5% chứ không phải 29,0%). Nối vào **sổ công thức** còn treo.
- `timVanBan` một tool là đủ, nhưng phải có **tham số lọc** `nam`, `loaiTaiLieu`, `phamVi`.
  Lọc payload Qdrant ăn đứt thêm một tầng rerank: lọc bỏ hẳn 95% kho trước khi tìm,
  rerank chỉ sắp lại thứ đã lấy về.

### 4.3 Mô tả tool và system prompt — ba lớp, giảm dần độ mềm

```
1. Mô tả tool      gửi kèm MỖI lời gọi, gắn với TỪNG tool   → mô hình có thể lờ đi
2. System prompt   luật chung, cắt ngang mọi tool           → mô hình có thể lờ đi
3. Mã Java kiểm    chạy sau khi trả lời, trước khi chạm DB  → KHÔNG lờ được
```

| Nội dung | Mô tả tool | System prompt |
|---|---|---|
| Tool này làm gì · điều kiện dùng · nghĩa tham số · `enum` · trỏ sang lối thoát | ✔ | |
| **Cấm thay chỉ tiêu gần giống** | ✔ | ✔ (đáng để cả hai) |
| Luôn kèm trích dẫn · đơn vị tiền · giọng văn, định dạng | | ✔ |

Chỉ **một** luật đáng nhắc hai lần. Còn lại đừng chép đôi — schema gửi trong **mọi** lời gọi,
mỗi 200 token thừa là nhân với mọi câu hỏi. Tiêu chí VDT có 30% cho hiệu quả.

### 4.4 Trong Spring AI

Java enum tự dịch thành JSON-schema enum:

```java
public enum MaChiTieu { TONG_TAI_SAN, DOANH_THU_THUAN, VON_CHU_SO_HUU, ... }

@Tool(description = """
    Tra mot chi tieu tai chinh da chuan hoa tu BCTC.
    CHI dung khi chi tieu duoc hoi co mat CHINH XAC trong ma_chi_tieu.
    KHONG duoc thay bang chi tieu gan giong — hay goi khongHoTro.
    """)
public KetQua traChiTieu(MaChiTieu maChiTieu, String ky, PhamVi phamVi) { ... }
```

Với ~100 chỉ tiêu thì **sinh enum từ bảng ánh xạ** (lúc build hoặc lúc chạy), đừng gõ tay.
Một nguồn sự thật.

Mặc định Spring AI **tự chạy tool và tự lặp** — tiện nhưng giấu mất vòng lặp, không cắm
được ba rào ở 4.6. Muốn tự cầm:

```java
ToolCallingChatOptions.builder()
    .toolCallbacks(traChiTieu, timVanBan, khongHoTro)
    .internalToolExecutionEnabled(false)     // ← ta tự lặp
    .build();
```

Đề tài này **nên tự cầm**, vì chương đánh giá cần đo số vòng, số tool gọi, token mỗi vòng.
*(Tên phương thức nhớ theo API, chưa mở mã 1.1.8 ra kiểm — khi viết nhớ xác nhận lại.)*

### 4.5 Bẫy: enum bị nhân bản

```
enum 100 mã  ≈ 500 token  ×  3 tool DB  ≈ 1.500 token
```

Trả **mỗi câu hỏi, mỗi vòng lặp**. Ba vòng là 4.500 token chỉ để chép lại một danh sách.

Ba hướng chữa, **đo rồi hãy quyết**:
1. Rút enum còn ~30–40 chỉ tiêu hay hỏi, phần đuôi để `khongHoTro` bắt
2. Gom bớt tool — cho `traChiTieu` nhận thêm `kyDen` tuỳ chọn thì nuốt luôn `lietKe`
3. Thử `$defs`/`$ref` — nhưng **phải thử mới biết** LiteLLM có chuyển tiếp đúng không

Hướng 2 rẻ nhất nhưng tham số càng nhiều mô hình càng dễ điền sai.
Đây là một mục đẹp cho chương đánh giá: *số tool vs tỷ lệ điền đúng tham số vs token mỗi câu*.

### 4.6 Ba rào phải có trong `modules/agent/loop`

| Rào | Vì sao |
|---|---|
| **Trần số vòng** (3–4) rồi dừng cứng | không có trần thì gặp câu lạ nó lặp vô hạn |
| **Chống gọi trùng** | vết 1 ở mục 8 |
| **Kiểm bám nguồn trước khi trả lời** | vết 2 ở mục 8 — đắt nhất |

Thêm: sau khi dùng xong, **thay đoạn RAG dài trong lịch sử bằng bản rút gọn** — đoạn RAG thật
cỡ 4.000–8.000 token và bị gửi lại ở **mọi vòng sau**.

---

## 5. Dẫn chứng

### 5.1 Gắn lúc nạp, không đi tìm lúc trả lời

```
LÚC NẠP
  script đang đọc  .../BCTC-HN-2025_full/trang_008.md
       │  ← ngay lúc này nó ĐÃ BIẾT tài liệu nào, trang mấy (nằm trong đường dẫn)
       ▼
  INSERT (..., 'CTR-BCTC-HN-2025-N', 8)
  chunk vào Qdrant  →  payload {tai_lieu_id, trang, nam, ...}

LÚC HỎI
  SELECT gia_tri, tai_lieu_id, trang ...   ← cùng một dòng, cùng một lần đọc đĩa
  Qdrant luôn trả payload kèm kết quả      ← không tốn thêm
```

**Chi phí tìm dẫn chứng: 0.** Giống chép trích dẫn vào phiếu — ghi tên sách lên phiếu
*ngay lúc chép*, chứ không chép xong rồi lùng lại cả thư viện.

### 5.2 PHÁT HIỆN — tên thư mục trùng, không đủ làm `tai_lieu_id`

Quét 278 tài liệu (thư mục có `_TOAN_VAN.md`), **4 tên trùng, dính 10 tài liệu**:

```
01_bao_cao_tai_chinh/2022/rieng/BCTC-rieng          ┐
01_bao_cao_tai_chinh/2023/rieng/BCTC-rieng          ├─ ba tài liệu KHÁC NHAU
01_bao_cao_tai_chinh/2024/rieng/BCTC-rieng          ┘  cùng một tên

01_bao_cao_tai_chinh/2023/hop_nhat/BCTC-hop-nhat    ┐ hai tài liệu khác nhau
01_bao_cao_tai_chinh/2024/hop_nhat/BCTC-hop-nhat    ┘

Giai-trinh (2022, 2023, 2025) · CV-giai-trinh (×2)
```

Lấy tên thư mục làm khoá thì số liệu 2022/2023/2024 **chồng lên nhau** — lỗi im lặng,
không báo gì, chỉ ra số sai. Và `BCTC-rieng trang 6` cũng vô nghĩa với người đọc.

**Cần bảng `tai_lieu` với mã tự sinh:**

```
tai_lieu_id   : CTR-BCTC-R-2023-N          ← ta tự sinh, ổn định
ten_hien_thi  : Báo cáo tài chính riêng năm 2023
nam           : 2023
ky            : nam                        (nam · ban_nien · Q1..Q4)
pham_vi       : rieng
che_do        : TT200
duong_dan     : 01_bao_cao_tai_chinh/2023/rieng/BCTC-rieng
```

Dẫn chứng in ra: *"Báo cáo tài chính riêng năm 2023, trang 6"*.
Lợi thêm: cột `che_do` của bảng này chính là **tầng 1** trong sơ đồ ba tầng ở 1.2.

### 5.3 Hai mã, hai việc

| | Dùng để | Ai thấy |
|---|---|---|
| `chunk_id` | kiểm bám nguồn, ghi nhật ký, dò lỗi | chỉ mã Java |
| `(tai_lieu_id, trang)` | in ra dẫn chứng | người dùng |

Hai chunk cùng trang thì **khử trùng ở lớp hiển thị**, nhưng bên trong giữ đủ hai `chunk_id`
— lúc kiểm bám nguồn cần biết câu văn khớp với *đoạn nào*, không phải *trang nào*.

### 5.4 Luật quan trọng

> **Đừng để mô hình nhớ số trang. Để mã Java gắn vào.**

```java
var dan = ketQuaTool.stream()
        .map(k -> k.taiLieuId() + " · trang " + k.trang())
        .distinct().toList();
```

Dẫn chứng **không bao giờ sai** vì nó không đi qua mô hình.

Số dẫn xuất thì dẫn chứng là **hai trang** (2025 và 2024) — gắn theo **từng dữ kiện**,
rồi mới gom lại ở cuối, không gắn cho cả câu trả lời.

### 5.5 Hai bên khác nhau về độ chắc

| | DB | RAG |
|---|---|---|
| Dẫn chứng có sẵn | ✔ trong dòng dữ liệu | ✔ trong metadata chunk |
| Kiểm tự động được | ✔ so số với trang | ✘ phải kiểm ngữ nghĩa |
| Rủi ro | gần như không | mô hình thêm thắt |

---

## 6. Ba chốt kiểm — "mã Java kiểm" là gì

Không phải thư viện, không phải mô hình. **Mã Java bình thường, mấy câu `if`.**
Chỗ để nó: `common/validation` (đã có sẵn trong cây gói).

```
LÚC NẠP          trang OCR → [kiểm] → INSERT        phép cộng có khớp không
LÚC GỌI TOOL     LLM chọn  → [kiểm] → chạy SQL      tham số có hợp lệ không
LÚC TRẢ LỜI      LLM viết  → [kiểm] → người dùng    số có nguồn không
```

**Chốt 1 — lúc nạp**

```java
// common/validation/KiemCanDoi.java
long a = so.get("DOANH_THU_THUAN");
long b = so.get("DOANH_THU_BAN_HANG") - so.get("CAC_KHOAN_GIAM_TRU");
if (a != b) throw new LechCanDoiException("mã 10 ≠ mã 01 − mã 02, lệch " + (a - b));
```

Lệch một đồng là biết OCR sai, biết đúng trang nào.

**Chốt 2 — lúc gọi tool**

```java
// common/validation/KiemThamSo.java
if (t.ngay().isAfter(LocalDate.now()))     return tuChoi("kỳ ở tương lai");
if (!kyCoTrongDb(t.ngay(), t.phamVi()))    return tuChoi("chưa có số liệu kỳ này");
```

Không có dòng kiểm enum — Jackson đã từ chối trước khi vào hàm. **Nhưng phải bắt ngoại lệ
đó** và biến thành câu trả lời tử tế, không thì người dùng nhận HTTP 500:

```java
catch (InvalidFormatException e) { return "Chỉ tiêu bạn hỏi chưa có trong kho số liệu."; }
```

**Chốt 3 — lúc trả lời**

```java
// common/validation/KiemBamNguon.java
var duocPhep    = ketQuaTool.stream().flatMap(k -> k.moiSo()).collect(toSet());
var trongTraLoi = RUT_SO.matcher(traLoi).results().map(...).collect(toSet());
if (!duocPhep.containsAll(trongTraLoi)) {
    ghiNhatKy("số không rõ nguồn: " + ...);
    return traLoiLai();
}
```

Ba mức kiểm bám nguồn:

| Mức | Làm gì | Giá | Dùng ở đâu |
|---|---|---|---|
| **1** | mọi **con số** trong trả lời phải có trong kết quả tool | gần như 0 | **chạy thật, mọi câu** |
| **2** | mỗi câu văn so độ giống với các đoạn đã lấy | rẻ (đã có embedding) | nếu đo thấy kịp |
| **3** | gọi mô hình khác chấm từng mệnh đề | đắt, +1 lượt | **chỉ bộ đánh giá offline** |

**Số thì khoá chặt được bằng mã thường**, và với phòng tài chính kế toán thì số sai mới
là thứ chết người.

---

## 7. Số đo hôm nay

### 7.1 Ba phép thử API (Viettel AI · DeepSeek-V4-Flash · `temperature=0`)

**Thử 1 — chọn nhánh.** Khai 2 tool, hỏi 5 câu:

| Hỏi | Chọn | |
|---|---|---|
| Tổng tài sản HN 2024? | `tra_chi_tieu` | ✔ |
| Chính sách khấu hao TSCĐ? | `tim_van_ban` | ✔ |
| **Lãi suất vay Vietcombank?** | `tim_van_ban` | ✔ **ca khó** — có "bao nhiêu" nhưng `LAI_SUAT` không có trong enum |
| Doanh thu 2025 tăng mấy % so 2024? | `tra_chi_tieu` × 2 (song song) | ✔ |
| **EBITDA quý 3/2025?** | `tra_chi_tieu(LOI_NHUAN_TRUOC_THUE)` | ✘ **thay chỉ tiêu** |

EBITDA = LNTT **+ lãi vay + khấu hao**. Với CTR khoản khấu hao rất lớn → sai hàng trăm tỷ.
Enum **không chặn được**, vì giá trị nó điền *hợp lệ nhưng sai ý*.

**Thử 2 — vá.** Thêm tool `khong_ho_tro` + cấm thay thế trong mô tả + luật trong system prompt:

```
EBITDA quý 3/2025   →  khong_ho_tro(chi_tieu_duoc_hoi="EBITDA quy 3 nam 2025")   ✔
ROE năm 2025        →  khong_ho_tro(chi_tieu_duoc_hoi="ROE (Return on Equity)")  ✔
hai câu đúng trước đó vẫn đúng — không hồi quy                                    ✔
```

Trong ba thứ thêm vào, **chỉ tool `khong_ho_tro` là gốc**. Trước đó mô hình **không có cửa
nào để nói "không"**; bị dồn vào chân tường thì nó chọn cái gần nhất.

> **Khi mô hình làm bậy, trước khi nghĩ "nó ngu", hãy hỏi "mình có để cho nó đường nào
> làm đúng chưa".** Đa số ca là do ta bịt hết cửa.

**Thử 3 — vòng lặp.** Câu hỗn hợp *"tăng mấy % và vì sao"*:

| Vòng | `finish_reason` | Token vào | Token ra | Gọi gì |
|---|---|---|---|---|
| 1 | `tool_calls` | 660 | 217 | `tra_chi_tieu` × 2 (2025, 2024) |
| 2 | `tool_calls` | 1.018 | 183 | `tim_van_ban` × 2 |
| 3 | `stop` | 1.363 | 436 | viết trả lời |

Vòng lặp **vá đúng chỗ hở**: chạy một lần thì vế "vì sao" bị rơi.
Mô hình tự chọn thứ tự **DB → RAG** (không phải RAG → DB); cả hai chiều đều hợp lệ.
Phép tính `2.206.238.657.195 / 11.733.278.611.947 = 18,8%` ra đúng — nhưng đó là **may**,
đừng dựa vào.

### 7.2 Đo trên kho

| Đo | Kết quả |
|---|---|
| Trang tiếng Việt | 5.569 / 6.690 |
| Trang có cụm "doanh thu thuần" | 175 |
| Dòng "TỔNG CỘNG TÀI SẢN" | 94 chỗ |
| `7.106.257.863.668` (TTS HN 31/12/2024) | 10 tài liệu |
| Tài liệu (thư mục có `_TOAN_VAN.md`) | 278 |
| Tên thư mục trùng | 4 tên / 10 tài liệu |
| Mã mơ hồ nhất | mã 10 (30 tên), 11 (23), 12 (22), 14 (16) |

---

## 8. Ba vết đã tìm ra

**Vết 1 — gọi trùng (nhẹ).** Vòng 2 gọi `tim_van_ban` hai lần với truy vấn gần y hệt
(*"Doanh thu thuần tăng trưởng năm 2025 so với 2024 nguyên nhân"* và *"giải trình biến động
doanh thu thuần hợp nhất năm 2025"*). Chữa: nhớ các lời gọi đã thực hiện, gặp trùng thì
trả lại kết quả cũ.

**Vết 2 — bịa thêm (nặng).** Đoạn RAG trả về **chỉ một câu**:

> *"Doanh thu tăng chủ yếu do mảng xây dựng hạ tầng viễn thông và mạng vận hành khai thác
> tăng trưởng mạnh."*

Mô hình viết ra:

> *"...ghi nhận tăng trưởng vượt bậc **nhờ đẩy mạnh triển khai các dự án hạ tầng mạng lưới**"*
> *"...**đóng góp tích cực vào tổng doanh thu hợp nhất**"*

Hai vế in đậm **không có trong nguồn**, mà dẫn chứng vẫn trỏ **đúng trang 31**.
Đây là kiểu sai khó bắt nhất: mọi thứ trông hợp lệ. Với phòng tài chính kế toán, câu bịa
nghe hợp lý còn tệ hơn câu sai rành rành.

**Vết 3 — token phình theo vòng.** 660 → 1.018 → 1.363, mà đoạn RAG ở đây chỉ có một dòng.
Đoạn thật cỡ 4.000–8.000 token và bị gửi lại ở mọi vòng sau.

---

## 9. Tác động đến bộ đánh giá

Ba nhóm test, **nhóm 3 mới chứng minh kiến trúc hai nhánh là cần thiết**:

1. thuần số · 2. thuần văn bản · 3. **hỗn hợp**

Và **tách hai tiêu chí, đừng gộp**:

- **Trích dẫn đúng chỗ** — trang được dẫn có thật, có liên quan. Đo máy được.
- **Bám nguồn** — từng mệnh đề truy về được một đoạn đã dẫn. Chấm tay hoặc mô hình chấm.

Câu trả lời có thể qua tiêu chí 1 mà trượt tiêu chí 2 — đúng ca vết 2.

Thêm một mục đo: *số tool vs tỷ lệ điền đúng tham số vs token mỗi câu* (mục 4.5).

---

## 10. Việc phát sinh từ buổi này

- [x] ~~Bảng ánh xạ mã~~ — **xong 29/9**: 250 cặp, 123 mã nội bộ, phủ 100% (1.7b)
- [ ] Bảng `tai_lieu` với `tai_lieu_id` tự sinh — **bắt buộc**, tên thư mục trùng (5.2)
- [ ] Cột `thay_the_bang` + hai thông điệp phân biệt: *không áp dụng kỳ này* vs *không có số liệu* (1.7b)
- [ ] Bảng `chi_tieu` dạng **dọc**, khoá `(ma_noi_bo, tu_ngay, den_ngay, pham_vi)` + `loai_so` (1.8, 1.9)
- [ ] Cột `trang_thai` + `thay_the_id` cho bảng `chi_tieu` — ca trình bày lại (2.3)
- [ ] Bộ đổi chuỗi kỳ → hai ngày (`Q3/2019`, `9T/2019`, `2019`, `30/09/2019`) (1.9)
- [ ] Tool `khongHoTro` — thiếu nó là mô hình thay chỉ tiêu (7.1)
- [ ] Java tự tính `chenh_lech` / `phan_tram` (4.2) — điều kiện để chốt 3 phủ kín
- [ ] Ba rào trong `modules/agent/loop` (4.6)
- [ ] Đổi tên `modules/dbquery/st8ments` → `statements`

## 11. Còn treo (theo thứ tự chặn đường)

1. ~~**Bảng ánh xạ mã**~~ — **xong 29/9** (1.7b). 250 cặp · 123 mã nội bộ · phủ 100%.
2. **Sổ công thức** — mỗi tỷ số khai một lần kèm `cơ sở` (cuối kỳ / bình quân). Chặn `traTySo`.
3. **Migration Flyway đầu tiên** — giờ đã đủ nguyên liệu: bảng `anh_xa_chi_tieu`,
   `tai_lieu`, `chi_tieu` với khoá `(ma_noi_bo, tu_ngay, den_ngay, pham_vi)`.
   Dựng MySQL bằng Docker Compose trước.
4. **Script trích 330 trang** B01/B02/B03 → MySQL, kèm ba chốt kiểm (mục 6).
5. **Luồng nạp một mạch** cho dữ liệu mới: OCR → trích số → chunk + embed,
   chạy lại nhiều lần không sinh trùng.
6. Mô hình rerank tầng 2 (từ `cong-nghe_v1.4.md`).
