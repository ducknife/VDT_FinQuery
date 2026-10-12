# Chạy thử luồng tiền xử lý (trừ OCR)

Tài liệu `CTR-BCTC-HN-20231231-Q4-dfab0e`: Báo cáo tài chính hợp nhất quý 4 2023, trang 5–10.
Không ghi gì vào MySQL hay Qdrant. Sinh bởi `PreprocessingTrialTest`.


## 1. Đọc trang OCR và làm sạch (`TextCleaner`)

| Trang | Ký tự gốc | Sau làm sạch | Bỏ đi |
|---|---|---|---|
| 5 | 3083 | 2997 | 86 |
| 6 | 2646 | 2634 | 12 |
| 7 | 2169 | 2122 | 47 |
| 8 | 840 | 827 | 13 |
| 9 | 3154 | 3142 | 12 |
| 10 | 2528 | 2524 | 4 |

## 2. Sửa tiêu đề bảng vắt trang (`TableStitcher`)

Trang 8 được chép thêm tiêu đề bảng từ trang 7. 12 dòng đầu sau khi sửa:

```
(tiêu đề bảng từ trang 7)

| Mã số | CHỈ TIÊU | Thuyết minh | Quý IV/2023 VND | Quý IV/2022 Đã điều chỉnh VND | Năm 2023 VND | Năm 2022 Đã điều chỉnh VND |
|-------|----------|-------------|------------------|-------------------------------|--------------|-----------------------------|
| 51 | 15. Chi phí thuế TNDN hiện hành | 30 | 36.240.869.667 | 34.379.096.201 | 129.639.757.405 | 113.448.466.450 |
| 52 | 16. Chi phí thuế TNDN hoãn lại | 31 | (693.501.657) | 515.937.971 | (693.501.657) | 1.662.466.796 |
| 60 | 17. Lợi nhuận sau thuế TNDN | | 143.241.887.486 | 134.795.704.915 | 516.503.281.820 | 455.184.638.327 |
| 61 | 18. Lợi nhuận sau thuế của Công ty mẹ | | 143.241.887.486 | 134.795.183.466 | 516.503.281.820 | 455.155.190.939 |
| 62 | 19. Lợi nhuận sau thuế của cổ đông không kiểm soát | | - | 521.449 | - | 29.447.388 |
| 70 | 20. Lãi cơ bản trên cổ phiếu | 32 | 1.252 | 1.178 | 4.515 | 3.979 |

---
```


## 3. Chunk (`Chunking`)

| # | Trang | Phần | Ký tự | Kiểu | Gối đầu |
|---|---|---|---|---|---|
| 1 | 5 | 0 | 2997 | mixed |  |
| 2 | 6 | 0 | 2634 | mixed |  |
| 3 | 7 | 0 | 2122 | mixed |  |
| 4 | 8 | 0 | 1052 | mixed |  |
| 5 | 9 | 0 | 3142 | mixed |  |
| 6 | 10 | 0 | 2524 | mixed |  |

<details><summary>Chunk 1 (trang 5, phần 0)</summary>

````markdown
# Tổng Công ty Cổ phần Công trình Viettel
Số 01 phố Giang Văn Minh, quận Ba Đình, Hà Nội

## Báo cáo tài chính hợp nhất
Cho năm tài chính kết thúc ngày 31/12/2023

---

## BẢNG CÂN ĐỐI KẾ TOÁN HỢP NHẤT
Tại ngày 31 tháng 12 năm 2023

| Mã số | TÀI SẢN | Thuyết minh | 31/12/2023 VND | 01/01/2023 Đã điều chỉnh VND |
|-------|---------|-------------|----------------|-----------------------------|
| 100 | A. TÀI SẢN NGẮN HẠN | | 5.627.168.242.619 | 5.009.963.242.010 |
| 110 | I. Tiền và các khoản tương đương tiền | 3 | 817.851.176.502 | 413.861.121.979 |
| 111 | 1. Tiền | | 517.851.176.502 | 263.861.121.979 |
| 112 | 2. Các khoản tương đương tiền | | 300.000.000.000 | 150.000.000.000 |
| 120 | II. Các khoản đầu tư tài chính ngắn hạn | 4 | 1.000.000.000.000 | 1.222.000.000.000 |
| 123 | 1. Đầu tư nắm giữ đến ngày đáo hạn | | 1.000.000.000.000 | 1.222.000.000.000 |
| 130 | III. Các khoản phải thu ngắn hạn | | 2.944.779.856.337 | 2.471.687.422.167 |
| 131 | 1. Phải thu ngắn hạn của khách hàng | 5 | 2.012.977.847.756 | 1.660.092.970.924 |
| 132 | 2. Trả trước cho người bán ngắn hạn | 6 | 249.244.604.125 | 145.414.213.725 |
| 136 | 3. Phải thu ngắn hạn khác | 7 | 786.109.791.957 | 693.136.375.745 |
| 137 | 4. Dự phòng phải thu ngắn hạn khó đòi | | (103.552.387.501) | (26.956.138.227) |
| 140 | IV. Hàng tồn kho | 9 | 783.427.189.358 | 818.866.205.996 |
| 141 | 1. Hàng tồn kho | | 802.280.586.132 | 839.765.852.740 |
| 149 | 2. Dự phòng giảm giá hàng tồn kho | | (18.853.396.774) | (20.899.646.744) |
| 150 | V. Tài sản ngắn hạn khác | | 81.110.020.422 | 83.548.491.868 |
| 151 | 1. Chi phí trả trước ngắn hạn | 14 | 81.110.020.422 | 81.603.252.638 |
| 153 | 2. Thuế và các khoản khác phải thu Nhà nước | 18 | - | 1.945.239.230 |
| 200 | B. TÀI SẢN DÀI HẠN | | 1.301.931.686.640 | 1.050.049.134.079 |
| 220 | I. Tài sản cố định | | 284.556.699.923 | 374.255.456.016 |
| 221 | 1. Tài sản cố định hữu hình | 11 | 261.538.671.066 | 345.044.655.145 |
| | - Nguyên giá | | 740.194.216.248 | 717.717.320.115 |
| | - Giá trị hao mòn lũy kế | | (478.655.545.182) | (372.672.664.970) |
| 227 | 2. Tài sản cố định vô hình | 12 | 23.018.028.857 | 29.210.800.871 |
| | - Nguyên giá | | 53.507.223.798 | 50.854.212.719 |
| | - Giá trị hao mòn lũy kế | | (30.489.194.941) | (21.643.411.848) |
| 230 | II. Bất động sản đầu tư | 13 | 829.293.428.954 | 562.548.375.246 |
| 231 | - Nguyên giá | | 1.164.214.426.790 | 754.013.373.444 |
| 232 | - Giá trị hao mòn lũy kế | | (334.920.997.836) | (191.464.998.198) |
| 240 | III. Tài sản dở dang dài hạn | | 152.430.706.597 | 93.602.531.054 |
| 242 | 1. Chi phí xây dựng cơ bản dở dang | 10 | 152.430.706.597 | 93.602.531.054 |
| 260 | IV. Tài sản dài hạn khác | | 35.650.851.166 | 19.642.771.763 |
| 261 | 1. Chi phí trả trước dài hạn | 14 | 34.957.349.509 | 19.642.771.763 |
| 262 | 2. Tài sản thuê thu nhập hoãn lại | 31 | 693.501.657 | - |
| 270 | TỔNG CỘNG TÀI SẢN | | 6.929.099.929.259 | 6.060.012.376.089 |
````
</details>

<details><summary>Chunk 2 (trang 6, phần 0)</summary>

````markdown
# Tổng Công ty Cổ phần Công trình Viettel
Số 01 phố Giang Văn Minh, quận Ba Đình, Hà Nội

## Báo cáo tài chính hợp nhất
Cho năm tài chính kết thúc ngày 31/12/2023

---

## BẢNG CÂN ĐỐI KẾ TOÁN HỢP NHẤT
Tại ngày 31 tháng 12 năm 2023
(tiếp theo)

| Mã số | NGUỒN VỐN | Thuyết minh | 31/12/2023 VND | 01/01/2023 Đã điều chỉnh VND |
|--------|-------------|--------------|----------------|------------------------------|
| 300 | C. NỢ PHẢI TRẢ | | 4.938.347.117.084 | 4.422.921.386.354 |
| 310 | I. Nợ ngắn hạn | | 4.540.668.097.741 | 4.139.842.500.551 |
| 311 | 1. Phải trả người bán ngắn hạn | 16 | 594.472.080.099 | 518.872.943.502 |
| 312 | 2. Người mua trả tiền trước ngắn hạn | 17 | 660.847.773.848 | 404.574.778.485 |
| 313 | 3. Thuế và các khoản phải nộp Nhà nước | 18 | 168.753.300.312 | 104.068.206.717 |
| 314 | 4. Phải trả người lao động | | 804.478.633.516 | 742.019.222.332 |
| 315 | 5. Chi phí phải trả ngắn hạn | 19 | 693.166.400.194 | 765.687.505.959 |
| 318 | 6. Doanh thu chưa thực hiện ngắn hạn | 21 | 160.524.346.346 | 103.116.358.852 |
| 319 | 7. Phải trả ngắn hạn khác | 20 | 237.767.256.484 | 277.526.601.361 |
| 320 | 8. Vay và nợ thuê tài chính ngắn hạn | 15 | 1.190.586.227.884 | 1.196.684.057.443 |
| 321 | 9. Dự phòng phải trả ngắn hạn | | 9.085.539.741 | 7.645.560.576 |
| 322 | 10. Quỹ khen thưởng phúc lợi | | 20.986.539.317 | 19.647.265.324 |
| 330 | II. Nợ dài hạn | | 397.679.019.343 | 283.078.885.803 |
| 337 | 1. Phải trả dài hạn khác | 20 | 14.664.000.000 | 14.247.000.000 |
| 338 | 2. Vay và nợ thuê tài chính dài hạn | 15 | 383.015.019.343 | 268.831.885.803 |
| 400 | D. VỐN CHỦ SỞ HỮU | | 1.990.752.812.175 | 1.637.090.989.735 |
| 410 | I. Vốn chủ sở hữu | 22 | 1.990.752.812.175 | 1.637.090.989.735 |
| 411 | 1. Vốn góp của chủ sở hữu | | 1.143.858.790.000 | 1.143.858.790.000 |
| 411a | Cổ phiếu phổ thông có quyền biểu quyết | | 1.143.858.790.000 | 1.143.858.790.000 |
| 412 | 2. Thặng dư vốn cổ phần | | (15.000.000) | (15.000.000) |
| 417 | 3. Chênh lệch tỷ giá hối đoái | | 22.746.893.093 | 4.444.473.473 |
| 421 | 4. Lợi nhuận sau thuế chưa phân phối | | 824.162.129.082 | 488.751.824.367 |
| 421a | LNST chưa phân phối lũy kế đến cuối năm trước | | 307.658.847.262 | 33.596.633.428 |
| 421b | LNST chưa phân phối kỳ này | | 516.503.281.820 | 455.155.190.939 |
| 429 | 5. Lợi ích cổ đông không kiểm soát | | - | 50.901.895 |
| 440 | TỔNG CỘNG NGUỒN VỐN | | 6.929.099.929.259 | 6.060.012.376.089 |

---

**Phạm Tuấn Thành**
Người lập biểu

**Bùi Duy Bảo**
Kế toán trưởng

**Phạm Đình Trường**
Tổng Giám đốc
Hà Nội, ngày 30 tháng 01 năm 2024
````
</details>

<details><summary>Chunk 3 (trang 7, phần 0)</summary>

````markdown
# Tổng Công ty Cổ phần Công trình Viettel
Số 01 phố Giang Văn Minh, quận Ba Đình, Hà Nội

## Báo cáo tài chính hợp nhất
Cho năm tài chính kết thúc ngày 31/12/2023

---

## BÁO CÁO KẾT QUẢ HOẠT ĐỘNG KINH DOANH HỢP NHẤT
### Năm 2023

| Mã số | CHỈ TIÊU | Thuyết minh | Quý IV/2023 VND | Quý IV/2022 Đã điều chỉnh VND | Năm 2023 VND | Năm 2022 Đã điều chỉnh VND |
|-------|----------|-------------|------------------|-------------------------------|--------------|-----------------------------|
| 01 | 1. Doanh thu bán hàng và cung cấp dịch vụ | 23 | 3.174.681.752.702 | 2.636.449.388.951 | 11.298.950.641.691 | 9.466.997.451.987 |
| 02 | 2. Các khoản giảm trừ doanh thu | | - | - | - | - |
| 10 | 3. Doanh thu thuần bán hàng và cung cấp dịch vụ | | 3.174.681.752.702 | 2.636.449.388.951 | 11.298.950.641.691 | 9.466.997.451.987 |
| 11 | 4. Giá vốn hàng bán | 24 | 2.900.216.227.579 | 2.402.900.387.349 | 10.413.160.882.859 | 8.710.415.826.649 |
| 20 | 5. Lợi nhuận gộp về bán hàng và cung cấp dịch vụ | | 274.465.525.123 | 233.549.001.602 | 885.789.758.832 | 756.581.625.338 |
| 21 | 6. Doanh thu hoạt động tài chính | 25 | 30.214.815.185 | 19.297.584.899 | 104.557.514.596 | 27.209.582.580 |
| 22 | 7. Chi phí tài chính | 26 | 26.533.852.977 | 20.051.706.115 | 93.821.759.418 | 24.247.418.911 |
| 23 | - Trong đó: Chi phí lãi vay | | 18.283.290.994 | 18.841.121.673 | 85.743.481.662 | 23.036.834.469 |
| 25 | 8. Chi phí bán hàng | | - | - | - | - |
| 26 | 9. Chi phí quản lý doanh nghiệp | 27 | 96.956.306.117 | 62.593.238.494 | 248.934.257.517 | 189.088.231.316 |
| 30 | 10. Lợi nhuận thuần từ hoạt động kinh doanh | | 181.190.181.214 | 170.201.641.892 | 647.591.256.493 | 570.455.557.691 |
| 31 | 11. Thu nhập khác | 28 | 642.500.474 | (20.668.737) | 2.821.202.051 | 1.095.349.823 |
| 32 | 12. Chi phí khác | 29 | 3.043.426.192 | 490.234.068 | 4.962.920.976 | 1.255.335.941 |
| 40 | 13. Lợi nhuận khác | | (2.400.925.718) | (510.902.805) | (2.141.718.925) | (159.986.118) |
| 50 | 14. Tổng lợi nhuận kế toán trước thuế | | 178.789.255.496 | 169.690.739.087 | 645.449.537.568 | 570.295.571.573 |
````
</details>

<details><summary>Chunk 4 (trang 8, phần 0)</summary>

````markdown
(tiêu đề bảng từ trang 7)

| Mã số | CHỈ TIÊU | Thuyết minh | Quý IV/2023 VND | Quý IV/2022 Đã điều chỉnh VND | Năm 2023 VND | Năm 2022 Đã điều chỉnh VND |
|-------|----------|-------------|------------------|-------------------------------|--------------|-----------------------------|
| 51 | 15. Chi phí thuế TNDN hiện hành | 30 | 36.240.869.667 | 34.379.096.201 | 129.639.757.405 | 113.448.466.450 |
| 52 | 16. Chi phí thuế TNDN hoãn lại | 31 | (693.501.657) | 515.937.971 | (693.501.657) | 1.662.466.796 |
| 60 | 17. Lợi nhuận sau thuế TNDN | | 143.241.887.486 | 134.795.704.915 | 516.503.281.820 | 455.184.638.327 |
| 61 | 18. Lợi nhuận sau thuế của Công ty mẹ | | 143.241.887.486 | 134.795.183.466 | 516.503.281.820 | 455.155.190.939 |
| 62 | 19. Lợi nhuận sau thuế của cổ đông không kiểm soát | | - | 521.449 | - | 29.447.388 |
| 70 | 20. Lãi cơ bản trên cổ phiếu | 32 | 1.252 | 1.178 | 4.515 | 3.979 |

---

**Phạm Tuấn Thành**
Người lập biểu

**Bùi Duy Bảo**
Kế toán trưởng

**Phạm Đình Trường**
Tổng Giám đốc
Hà Nội, ngày 30 tháng 01 năm 2024
````
</details>

<details><summary>Chunk 5 (trang 9, phần 0)</summary>

````markdown
# BÁO CÁO LƯU CHUYỂN TIỀN TỆ HỢP NHẤT

**Năm 2023**
*(Theo phương pháp gián tiếp)*

| Mã số | CHỈ TIÊU | Thuyết minh | Năm 2023 VND | Năm 2022 Đã điều chỉnh VND |
|-------|----------|-------------|--------------|-----------------------------|
| 01 | 1. Lợi nhuận trước thuế | | 645.449.537.568 | 570.295.571.573 |
| 02 | 2. Điều chỉnh cho các khoản<br>- Khấu hao tài sản cố định và bất động sản đầu tư | | 258.284.662.943 | 206.442.863.972 |
| 03 | - Các khoản dự phòng | | 75.989.978.469 | 32.256.829.962 |
| 04 | - Lãi, lỗ chênh lệch tỷ giá hối đoái do đánh giá lại các khoản mục tiền tệ có gốc ngoại tệ | | 18.255.616.875 | 728.145.288 |
| 05 | - Lãi/lỗ từ hoạt động đầu tư | | (103.023.666.892) | (25.468.252.582) |
| 06 | - Chi phí lãi vay | | 84.959.623.445 | 23.036.834.469 |
| 08 | 3. Lợi nhuận từ hoạt động kinh doanh trước thay đổi vốn lưu động | | 979.915.752.408 | 807.291.992.682 |
| 09 | - Tăng/giảm các khoản phải thu | | (549.259.144.179) | (861.044.938.908) |
| 10 | - Tăng/giảm hàng tồn kho | | 37.485.266.608 | (253.241.452.178) |
| 11 | - Tăng/giảm các khoản phải trả<br>(không kể lãi vay phải trả, thuế TNDN phải nộp) | | 326.670.917.409 | 576.199.823.077 |
| 12 | - Tăng/giảm chi phí trả trước | | (14.821.345.530) | (13.758.137.732) |
| 14 | - Tiền lãi vay đã trả | | (84.852.488.631) | (20.915.408.047) |
| 15 | - Thuế thu nhập doanh nghiệp đã nộp | | (57.698.258.718) | (107.575.383.402) |
| 17 | - Tiền chi khác cho hoạt động kinh doanh | | (62.918.726.007) | (54.724.549.723) |
| 20 | **Lưu chuyển tiền thuần từ hoạt động kinh doanh** | | **574.521.973.360** | **72.231.945.769** |
| 21 | II. LƯU CHUYỂN TIỀN TỪ HOẠT ĐỘNG ĐẦU TƯ<br>1. Tiền chi để mua sắm, xây dựng TSCĐ và các tài sản dài hạn khác | | (483.442.221.964) | (406.520.385.050) |
| 23 | 2. Tiền chi cho vay, mua các công cụ nợ của đơn vị khác | | (1.549.000.000.000) | (1.652.000.000.000) |
| 24 | 3. Tiền thu hồi cho vay, bán lại các công cụ nợ của đơn vị khác | | 1.771.000.000.000 | 530.000.000.000 |
| 27 | 4. Tiền thu lãi cho vay, cổ tức và lợi nhuận được chia | | 102.671.543.751 | 8.605.926.554 |
| 30 | **Lưu chuyển tiền thuần từ hoạt động đầu tư** | | **(158.770.678.213)** | **(1.519.914.458.496)** |
| 33 | III. LƯU CHUYỂN TIỀN TỪ HOẠT ĐỘNG TÀI CHÍNH<br>1. Tiền thu từ đi vay | | 2.455.208.243.463 | 1.322.688.329.908 |
| 34 | 2. Tiền trả nợ gốc vay | | (2.347.122.939.482) | (37.291.965.423) |
| 36 | 3. Cổ tức, lợi nhuận đã trả cho chủ sở hữu | | (119.893.347.350) | (91.877.102.750) |
| 40 | **Lưu chuyển tiền thuần từ hoạt động tài chính** | | **(11.808.043.369)** | **1.193.519.261.735** |
| 50 | **Lưu chuyển tiền thuần trong kỳ** | | **403.943.251.778** | **(254.163.250.992)** |
| 60 | Tiền và tương đương tiền đầu kỳ | | 413.861.121.979 | 668.144.408.661 |
| 61 | Ảnh hưởng của thay đổi tỷ giá hối đoái quy đổi ngoại tệ | | 46.802.745 | (120.035.690) |
| 70 | **Tiền và tương đương tiền cuối kỳ** | | **847.851.176.502** | **413.861.121.979** |

---

**Phạm Tuấn Thành**
Người lập biểu

**Bùi Duy Bảo**
Kế toán trưởng

**Phạm Đình Trường**
Tổng Giám đốc
Hà Nội, ngày 30 tháng 01 năm 2024
````
</details>

<details><summary>Chunk 6 (trang 10, phần 0)</summary>

````markdown
# THUYẾT MINH BÁO CÁO TÀI CHÍNH HỢP NHẤT
**Năm 2023**

---

## 1. ĐẶC ĐIỂM HOẠT ĐỘNG CỦA DOANH NGHIỆP

### Hình thức sở hữu vốn

Tổng Công ty được chuyển đổi từ doanh nghiệp nhà nước Công ty TNHH NN MTV Công trình Viettel sang Công ty Cổ phần theo Quyết định số 135/QĐ-BQP ngày 15/01/2010 về việc phê duyệt phương án và chuyển Công ty Công trình Viettel thuộc Tổng Công ty Viễn thông Quân đội (nay là Tập đoàn Công nghiệp - Viễn thông Quân đội) thành Công ty Cổ phần. Tổng Công ty đổi tên từ Công ty Cổ phần Công trình Viettel thành Tổng Công ty Cổ phần Công trình Viettel theo Nghị quyết Đại hội cổ đông ngày 19/06/2018, hoạt động theo Giấy đăng ký kinh doanh số 0104753865 do Sở Kế hoạch và Đầu tư Thành phố Hà Nội cấp ngày 09/06/2010 và đăng ký thay đổi lần thứ 17 tại ngày 10/08/2022.

Trụ sở chính của Tổng Công ty tại: Số 01 phố Giang Văn Minh, quận Ba Đình, Hà Nội.

Văn phòng giao dịch của Tổng Công ty đặt tại số 6 Phạm Văn Bạch, quận Cầu Giấy, Hà Nội.

Vốn góp của chủ sở hữu Tổng Công ty là 1.143.858.790.000 đồng; tương đương 114.385.879 cổ phần, mệnh giá một cổ phần là 10.000 đồng.

### Lĩnh vực kinh doanh

Lĩnh vực kinh doanh của Tổng Công ty là xây lắp, vận hành khai thác công trình viễn thông và kinh doanh thương mại vật tư viễn thông.

### Ngành nghề kinh doanh

- Xây dựng nhà các loại, công trình đường sắt, đường bộ, công trình công ích và công trình kỹ thuật dân dụng khác;
- Lắp đặt, sửa chữa, bảo trì, bảo dưỡng cơ sở hạ tầng viễn thông;
- Sản xuất, lắp đặt, bảo trì, bảo dưỡng các thiết bị bưu chính - viễn thông - công nghệ thông tin;
- Sản xuất, sửa chữa và kinh doanh các thiết bị điện, điện tử, viễn thông, công nghệ thông tin, thiết bị truyền thông;
- Kinh doanh các thiết bị toàn bộ về điện tử, viễn thông, công nghệ thông tin và các sản phẩm điện tử, viễn thông và công nghệ thông tin;
- Hoạt động kinh doanh bất động sản, quyền sử dụng đất thuộc chủ sở hữu, chủ sử dụng hoặc đi thuê;
- Hoạt động viễn thông có dây, viễn thông không dây;
- Kinh doanh xây lắp, bảo dưỡng vận hành mạng lưới.

### Cấu trúc tập đoàn

- Tổng Công ty có các công ty con được hợp nhất báo cáo tài chính tại thời điểm 31/12/2023 gồm:

| Tên công ty | Địa chỉ | Tỷ lệ lợi ích | Tỷ lệ quyền biểu quyết | Hoạt động kinh doanh chính |
|-------------|---------|----------------|------------------------|------------------------------|
| 1. Viettel Construction Cambodia | Thủ đô Phnôm - Pênh, Campuchia | 100,0% | 100,0% | Quản lý thi công lắp đặt công trình điện tử, viễn thông |
````
</details>

## 4. Văn bản đem embed (`TableToJson` + tên tài liệu)

| # | Markdown | Đem embed (JSON) | Tỉ lệ |
|---|---|---|---|
| 1 | 2997 | 5236 | 1.75 |
| 2 | 2634 | 4379 | 1.66 |
| 3 | 2122 | 3681 | 1.73 |
| 4 | 1052 | 1555 | 1.48 |
| 5 | 3142 | 4660 | 1.48 |
| 6 | 2524 | 2465 | 0.98 |

Văn bản đem embed của chunk 1 (30 dòng đầu):

```
[Báo cáo tài chính hợp nhất quý 4 2023]
# Tổng Công ty Cổ phần Công trình Viettel
Số 01 phố Giang Văn Minh, quận Ba Đình, Hà Nội

## Báo cáo tài chính hợp nhất
Cho năm tài chính kết thúc ngày 31/12/2023

---

## BẢNG CÂN ĐỐI KẾ TOÁN HỢP NHẤT
Tại ngày 31 tháng 12 năm 2023

[
{"Mã số":"100","TÀI SẢN":"A. TÀI SẢN NGẮN HẠN","31/12/2023 VND":"5.627.168.242.619","01/01/2023 Đã điều chỉnh VND":"5.009.963.242.010"},
{"Mã số":"110","TÀI SẢN":"I. Tiền và các khoản tương đương tiền","Thuyết minh":"3","31/12/2023 VND":"817.851.176.502","01/01/2023 Đã điều chỉnh VND":"413.861.121.979"},
{"Mã số":"111","TÀI SẢN":"1. Tiền","31/12/2023 VND":"517.851.176.502","01/01/2023 Đã điều chỉnh VND":"263.861.121.979"},
{"Mã số":"112","TÀI SẢN":"2. Các khoản tương đương tiền","31/12/2023 VND":"300.000.000.000","01/01/2023 Đã điều chỉnh VND":"150.000.000.000"},
{"Mã số":"120","TÀI SẢN":"II. Các khoản đầu tư tài chính ngắn hạn","Thuyết minh":"4","31/12/2023 VND":"1.000.000.000.000","01/01/2023 Đã điều chỉnh VND":"1.222.000.000.000"},
{"Mã số":"123","TÀI SẢN":"1. Đầu tư nắm giữ đến ngày đáo hạn","31/12/2023 VND":"1.000.000.000.000","01/01/2023 Đã điều chỉnh VND":"1.222.000.000.000"},
{"Mã số":"130","TÀI SẢN":"III. Các khoản phải thu ngắn hạn","31/12/2023 VND":"2.944.779.856.337","01/01/2023 Đã điều chỉnh VND":"2.471.687.422.167"},
{"Mã số":"131","TÀI SẢN":"1. Phải thu ngắn hạn của khách hàng","Thuyết minh":"5","31/12/2023 VND":"2.012.977.847.756","01/01/2023 Đã điều chỉnh VND":"1.660.092.970.924"},
{"Mã số":"132","TÀI SẢN":"2. Trả trước cho người bán ngắn hạn","Thuyết minh":"6","31/12/2023 VND":"249.244.604.125","01/01/2023 Đã điều chỉnh VND":"145.414.213.725"},
{"Mã số":"136","TÀI SẢN":"3. Phải thu ngắn hạn khác","Thuyết minh":"7","31/12/2023 VND":"786.109.791.957","01/01/2023 Đã điều chỉnh VND":"693.136.375.745"},
{"Mã số":"137","TÀI SẢN":"4. Dự phòng phải thu ngắn hạn khó đòi","31/12/2023 VND":"(103.552.387.501)","01/01/2023 Đã điều chỉnh VND":"(26.956.138.227)"},
{"Mã số":"140","TÀI SẢN":"IV. Hàng tồn kho","Thuyết minh":"9","31/12/2023 VND":"783.427.189.358","01/01/2023 Đã điều chỉnh VND":"818.866.205.996"},
{"Mã số":"141","TÀI SẢN":"1. Hàng tồn kho","31/12/2023 VND":"802.280.586.132","01/01/2023 Đã điều chỉnh VND":"839.765.852.740"},
{"Mã số":"149","TÀI SẢN":"2. Dự phòng giảm giá hàng tồn kho","31/12/2023 VND":"(18.853.396.774)","01/01/2023 Đã điều chỉnh VND":"(20.899.646.744)"},
{"Mã số":"150","TÀI SẢN":"V. Tài sản ngắn hạn khác","31/12/2023 VND":"81.110.020.422","01/01/2023 Đã điều chỉnh VND":"83.548.491.868"},
{"Mã số":"151","TÀI SẢN":"1. Chi phí trả trước ngắn hạn","Thuyết minh":"14","31/12/2023 VND":"81.110.020.422","01/01/2023 Đã điều chỉnh VND":"81.603.252.638"},
{"Mã số":"153","TÀI SẢN":"2. Thuế và các khoản khác phải thu Nhà nước","Thuyết minh":"18","31/12/2023 VND":"-","01/01/2023 Đã điều chỉnh VND":"1.945.239.230"},
```

## 5. Embedding (`Embedding` -> `EmbeddingClient`)

Bỏ qua (không gọi API). Chạy với `-Dtrial.embed=true` để embed thật.

## 6. Point Qdrant (`QdrantIndexWriter.payloadOf`), chưa ghi


**Point 1** `455975ef-24c2-38ac-9a4c-9d03cbc86969` (chưa có vector)

| Trường | Giá trị |
|---|---|
| tai_lieu_id | CTR-BCTC-HN-20231231-Q4-dfab0e |
| ma_cong_ty | CTR |
| trang | 5 |
| phan | 0 |
| nam | 2023 |
| loai | bctc |
| la_ban_chinh | true |
| element_type | mixed |
| noi_dung | (2997 ký tự, markdown) |
| ky | Q4 |
| pham_vi | hop_nhat |

**Point 2** `e323bdee-f4bd-3347-a59b-8cde43dc9b7c` (chưa có vector)

| Trường | Giá trị |
|---|---|
| tai_lieu_id | CTR-BCTC-HN-20231231-Q4-dfab0e |
| ma_cong_ty | CTR |
| trang | 6 |
| phan | 0 |
| nam | 2023 |
| loai | bctc |
| la_ban_chinh | true |
| element_type | mixed |
| noi_dung | (2634 ký tự, markdown) |
| ky | Q4 |
| pham_vi | hop_nhat |

**Point 3** `6e49ebbc-5b00-3a3b-a17a-f18fe0bde7a6` (chưa có vector)

| Trường | Giá trị |
|---|---|
| tai_lieu_id | CTR-BCTC-HN-20231231-Q4-dfab0e |
| ma_cong_ty | CTR |
| trang | 7 |
| phan | 0 |
| nam | 2023 |
| loai | bctc |
| la_ban_chinh | true |
| element_type | mixed |
| noi_dung | (2122 ký tự, markdown) |
| ky | Q4 |
| pham_vi | hop_nhat |

**Point 4** `0756b939-3d9b-3247-ac3d-7552cf06e5f1` (chưa có vector)

| Trường | Giá trị |
|---|---|
| tai_lieu_id | CTR-BCTC-HN-20231231-Q4-dfab0e |
| ma_cong_ty | CTR |
| trang | 8 |
| phan | 0 |
| nam | 2023 |
| loai | bctc |
| la_ban_chinh | true |
| element_type | mixed |
| noi_dung | (1052 ký tự, markdown) |
| ky | Q4 |
| pham_vi | hop_nhat |

**Point 5** `6dbdb92a-ad6b-3a8a-9a09-467a6f146a5c` (chưa có vector)

| Trường | Giá trị |
|---|---|
| tai_lieu_id | CTR-BCTC-HN-20231231-Q4-dfab0e |
| ma_cong_ty | CTR |
| trang | 9 |
| phan | 0 |
| nam | 2023 |
| loai | bctc |
| la_ban_chinh | true |
| element_type | mixed |
| noi_dung | (3142 ký tự, markdown) |
| ky | Q4 |
| pham_vi | hop_nhat |

**Point 6** `7f514b2a-188d-34d3-ad9e-05d9cffa1ab0` (chưa có vector)

| Trường | Giá trị |
|---|---|
| tai_lieu_id | CTR-BCTC-HN-20231231-Q4-dfab0e |
| ma_cong_ty | CTR |
| trang | 10 |
| phan | 0 |
| nam | 2023 |
| loai | bctc |
| la_ban_chinh | true |
| element_type | mixed |
| noi_dung | (2524 ký tự, markdown) |
| ky | Q4 |
| pham_vi | hop_nhat |

## 7. Trích số (`DocumentFigureExtractor`), chưa ghi

5 bảng mẫu biểu, 246 số qua hết các chốt, 2 ngoại lệ.

| Mẫu | Mã | Mã nội bộ | Kỳ | Giá trị | Trang | Cột |
|---|---|---|---|---|---|---|
| B01 | 100 | TAI_SAN_NGAN_HAN | 2023-12-31 | 5.627.168.242.619 | 5 | kỳ này |
| B01 | 110 | TIEN_VA_TUONG_DUONG_TIEN | 2023-12-31 | 817.851.176.502 | 5 | kỳ này |
| B01 | 111 | TIEN_M111 | 2023-12-31 | 517.851.176.502 | 5 | kỳ này |
| B01 | 112 | TUONG_DUONG_TIEN | 2023-12-31 | 300.000.000.000 | 5 | kỳ này |
| B01 | 120 | DAU_TU_TC_NGAN_HAN | 2023-12-31 | 1.000.000.000.000 | 5 | kỳ này |
| B01 | 123 | DAU_TU_NAM_GIU_DAO_HAN_NH | 2023-12-31 | 1.000.000.000.000 | 5 | kỳ này |
| B01 | 130 | PHAI_THU_NGAN_HAN | 2023-12-31 | 2.944.779.856.337 | 5 | kỳ này |
| B01 | 131 | PHAI_THU_KHACH_HANG_NH | 2023-12-31 | 2.012.977.847.756 | 5 | kỳ này |
| B01 | 132 | TRA_TRUOC_NGUOI_BAN_NH | 2023-12-31 | 249.244.604.125 | 5 | kỳ này |
| B01 | 136 | PHAI_THU_NGAN_HAN_KHAC | 2023-12-31 | 786.109.791.957 | 5 | kỳ này |
| B01 | 137 | DU_PHONG_PHAI_THU_KHO_DOI | 2023-12-31 | -103.552.387.501 | 5 | kỳ này |
| B01 | 140 | HANG_TON_KHO_M140 | 2023-12-31 | 783.427.189.358 | 5 | kỳ này |
| B01 | 141 | HANG_TON_KHO_M141 | 2023-12-31 | 802.280.586.132 | 5 | kỳ này |
| B01 | 149 | DU_PHONG_GIAM_GIA_HTK | 2023-12-31 | -18.853.396.774 | 5 | kỳ này |
| B01 | 150 | TAI_SAN_NGAN_HAN_KHAC | 2023-12-31 | 81.110.020.422 | 5 | kỳ này |
| B01 | 151 | CHI_PHI_CHO_PHAN_BO_NGAN_HAN | 2023-12-31 | 81.110.020.422 | 5 | kỳ này |
| B01 | 153 | THUE_PHAI_THU_NHA_NUOC | 2023-12-31 | 0 | 5 | kỳ này |
| B01 | 200 | TAI_SAN_DAI_HAN | 2023-12-31 | 1.301.931.686.640 | 5 | kỳ này |
| B01 | 220 | TAI_SAN_CO_DINH | 2023-12-31 | 284.556.699.923 | 5 | kỳ này |
| B01 | 221 | TSCD_HUU_HINH | 2023-12-31 | 261.538.671.066 | 5 | kỳ này |
| B01 | 222 | TSCD_HUU_HINH_NGUYEN_GIA | 2023-12-31 | 740.194.216.248 | 5 | kỳ này |
| B01 | 223 | TSCD_HUU_HINH_HAO_MON | 2023-12-31 | -478.655.545.182 | 5 | kỳ này |
| B01 | 227 | TSCD_VO_HINH | 2023-12-31 | 23.018.028.857 | 5 | kỳ này |
| B01 | 228 | TSCD_VO_HINH_NGUYEN_GIA | 2023-12-31 | 53.507.223.798 | 5 | kỳ này |
| B01 | 229 | TSCD_VO_HINH_HAO_MON | 2023-12-31 | -30.489.194.941 | 5 | kỳ này |
| B01 | 230 | BDS_DAU_TU | 2023-12-31 | 829.293.428.954 | 5 | kỳ này |
| B01 | 231 | BDS_DAU_TU_NGUYEN_GIA | 2023-12-31 | 1.164.214.426.790 | 5 | kỳ này |
| B01 | 232 | BDS_DAU_TU_HAO_MON | 2023-12-31 | -334.920.997.836 | 5 | kỳ này |
| B01 | 240 | TAI_SAN_DO_DANG_DH | 2023-12-31 | 152.430.706.597 | 5 | kỳ này |
| B01 | 242 | CHI_PHI_XDCB_DO_DANG | 2023-12-31 | 152.430.706.597 | 5 | kỳ này |
| B01 | 260 | TAI_SAN_DAI_HAN_KHAC | 2023-12-31 | 35.650.851.166 | 5 | kỳ này |
| B01 | 261 | CHI_PHI_CHO_PHAN_BO_DAI_HAN | 2023-12-31 | 34.957.349.509 | 5 | kỳ này |
| B01 | 262 | TAI_SAN_THUE_TNDN_HOAN_LAI | 2023-12-31 | 693.501.657 | 5 | kỳ này |
| B01 | 270 | TONG_TAI_SAN | 2023-12-31 | 6.929.099.929.259 | 5 | kỳ này |
| B01 | 300 | NO_PHAI_TRA | 2023-12-31 | 4.938.347.117.084 | 6 | kỳ này |
| B01 | 310 | NO_NGAN_HAN | 2023-12-31 | 4.540.668.097.741 | 6 | kỳ này |
| B01 | 311 | PHAI_TRA_NGUOI_BAN_NH | 2023-12-31 | 594.472.080.099 | 6 | kỳ này |
| B01 | 312 | NGUOI_MUA_TRA_TIEN_TRUOC_NH | 2023-12-31 | 660.847.773.848 | 6 | kỳ này |
| B01 | 313 | THUE_PHAI_NOP_NHA_NUOC | 2023-12-31 | 168.753.300.312 | 6 | kỳ này |
| B01 | 314 | PHAI_TRA_NGUOI_LAO_DONG | 2023-12-31 | 804.478.633.516 | 6 | kỳ này |
| B01 | 315 | CHI_PHI_PHAI_TRA_NH | 2023-12-31 | 693.166.400.194 | 6 | kỳ này |
| B01 | 318 | DOANH_THU_CHUA_THUC_HIEN_NH | 2023-12-31 | 160.524.346.346 | 6 | kỳ này |
| B01 | 319 | PHAI_TRA_NGAN_HAN_KHAC | 2023-12-31 | 237.767.256.484 | 6 | kỳ này |
| B01 | 320 | VAY_NO_THUE_TC_NH | 2023-12-31 | 1.190.586.227.884 | 6 | kỳ này |
| B01 | 321 | DU_PHONG_PHAI_TRA_NH | 2023-12-31 | 9.085.539.741 | 6 | kỳ này |
| B01 | 322 | QUY_KHEN_THUONG_PHUC_LOI | 2023-12-31 | 20.986.539.317 | 6 | kỳ này |
| B01 | 330 | NO_DAI_HAN | 2023-12-31 | 397.679.019.343 | 6 | kỳ này |
| B01 | 337 | PHAI_TRA_DAI_HAN_KHAC | 2023-12-31 | 14.664.000.000 | 6 | kỳ này |
| B01 | 338 | VAY_NO_THUE_TC_DH | 2023-12-31 | 383.015.019.343 | 6 | kỳ này |
| B01 | 400 | VON_CHU_SO_HUU_M400 | 2023-12-31 | 1.990.752.812.175 | 6 | kỳ này |
| B01 | 410 | VON_CHU_SO_HUU_M410 | 2023-12-31 | 1.990.752.812.175 | 6 | kỳ này |
| B01 | 411 | VON_GOP_CHU_SO_HUU | 2023-12-31 | 1.143.858.790.000 | 6 | kỳ này |
| B01 | 412 | THANG_DU_VON | 2023-12-31 | -15.000.000 | 6 | kỳ này |
| B01 | 417 | CHENH_LECH_TY_GIA | 2023-12-31 | 22.746.893.093 | 6 | kỳ này |
| B01 | 421 | LNST_CHUA_PHAN_PHOI | 2023-12-31 | 824.162.129.082 | 6 | kỳ này |
| B01 | 429 | LOI_ICH_CDKKS | 2023-12-31 | 0 | 6 | kỳ này |
| B01 | 440 | TONG_NGUON_VON | 2023-12-31 | 6.929.099.929.259 | 6 | kỳ này |
| B01 | 100 | TAI_SAN_NGAN_HAN | 2022-12-31 | 5.009.963.242.010 | 5 | so sánh, đã điều chỉnh |
| B01 | 110 | TIEN_VA_TUONG_DUONG_TIEN | 2022-12-31 | 413.861.121.979 | 5 | so sánh, đã điều chỉnh |
| B01 | 111 | TIEN_M111 | 2022-12-31 | 263.861.121.979 | 5 | so sánh, đã điều chỉnh |
| B01 | 112 | TUONG_DUONG_TIEN | 2022-12-31 | 150.000.000.000 | 5 | so sánh, đã điều chỉnh |
| B01 | 120 | DAU_TU_TC_NGAN_HAN | 2022-12-31 | 1.222.000.000.000 | 5 | so sánh, đã điều chỉnh |
| B01 | 123 | DAU_TU_NAM_GIU_DAO_HAN_NH | 2022-12-31 | 1.222.000.000.000 | 5 | so sánh, đã điều chỉnh |
| B01 | 130 | PHAI_THU_NGAN_HAN | 2022-12-31 | 2.471.687.422.167 | 5 | so sánh, đã điều chỉnh |
| B01 | 131 | PHAI_THU_KHACH_HANG_NH | 2022-12-31 | 1.660.092.970.924 | 5 | so sánh, đã điều chỉnh |
| B01 | 132 | TRA_TRUOC_NGUOI_BAN_NH | 2022-12-31 | 145.414.213.725 | 5 | so sánh, đã điều chỉnh |
| B01 | 136 | PHAI_THU_NGAN_HAN_KHAC | 2022-12-31 | 693.136.375.745 | 5 | so sánh, đã điều chỉnh |
| B01 | 137 | DU_PHONG_PHAI_THU_KHO_DOI | 2022-12-31 | -26.956.138.227 | 5 | so sánh, đã điều chỉnh |
| B01 | 140 | HANG_TON_KHO_M140 | 2022-12-31 | 818.866.205.996 | 5 | so sánh, đã điều chỉnh |
| B01 | 141 | HANG_TON_KHO_M141 | 2022-12-31 | 839.765.852.740 | 5 | so sánh, đã điều chỉnh |
| B01 | 149 | DU_PHONG_GIAM_GIA_HTK | 2022-12-31 | -20.899.646.744 | 5 | so sánh, đã điều chỉnh |
| B01 | 150 | TAI_SAN_NGAN_HAN_KHAC | 2022-12-31 | 83.548.491.868 | 5 | so sánh, đã điều chỉnh |
| B01 | 151 | CHI_PHI_CHO_PHAN_BO_NGAN_HAN | 2022-12-31 | 81.603.252.638 | 5 | so sánh, đã điều chỉnh |
| B01 | 153 | THUE_PHAI_THU_NHA_NUOC | 2022-12-31 | 1.945.239.230 | 5 | so sánh, đã điều chỉnh |
| B01 | 200 | TAI_SAN_DAI_HAN | 2022-12-31 | 1.050.049.134.079 | 5 | so sánh, đã điều chỉnh |
| B01 | 220 | TAI_SAN_CO_DINH | 2022-12-31 | 374.255.456.016 | 5 | so sánh, đã điều chỉnh |
| B01 | 221 | TSCD_HUU_HINH | 2022-12-31 | 345.044.655.145 | 5 | so sánh, đã điều chỉnh |
| B01 | 222 | TSCD_HUU_HINH_NGUYEN_GIA | 2022-12-31 | 717.717.320.115 | 5 | so sánh, đã điều chỉnh |
| B01 | 223 | TSCD_HUU_HINH_HAO_MON | 2022-12-31 | -372.672.664.970 | 5 | so sánh, đã điều chỉnh |
| B01 | 227 | TSCD_VO_HINH | 2022-12-31 | 29.210.800.871 | 5 | so sánh, đã điều chỉnh |
| B01 | 228 | TSCD_VO_HINH_NGUYEN_GIA | 2022-12-31 | 50.854.212.719 | 5 | so sánh, đã điều chỉnh |
| B01 | 229 | TSCD_VO_HINH_HAO_MON | 2022-12-31 | -21.643.411.848 | 5 | so sánh, đã điều chỉnh |
| B01 | 230 | BDS_DAU_TU | 2022-12-31 | 562.548.375.246 | 5 | so sánh, đã điều chỉnh |
| B01 | 231 | BDS_DAU_TU_NGUYEN_GIA | 2022-12-31 | 754.013.373.444 | 5 | so sánh, đã điều chỉnh |
| B01 | 232 | BDS_DAU_TU_HAO_MON | 2022-12-31 | -191.464.998.198 | 5 | so sánh, đã điều chỉnh |
| B01 | 240 | TAI_SAN_DO_DANG_DH | 2022-12-31 | 93.602.531.054 | 5 | so sánh, đã điều chỉnh |
| B01 | 242 | CHI_PHI_XDCB_DO_DANG | 2022-12-31 | 93.602.531.054 | 5 | so sánh, đã điều chỉnh |
| B01 | 260 | TAI_SAN_DAI_HAN_KHAC | 2022-12-31 | 19.642.771.763 | 5 | so sánh, đã điều chỉnh |
| B01 | 261 | CHI_PHI_CHO_PHAN_BO_DAI_HAN | 2022-12-31 | 19.642.771.763 | 5 | so sánh, đã điều chỉnh |
| B01 | 262 | TAI_SAN_THUE_TNDN_HOAN_LAI | 2022-12-31 | 0 | 5 | so sánh, đã điều chỉnh |
| B01 | 270 | TONG_TAI_SAN | 2022-12-31 | 6.060.012.376.089 | 5 | so sánh, đã điều chỉnh |
| B01 | 300 | NO_PHAI_TRA | 2022-12-31 | 4.422.921.386.354 | 6 | so sánh, đã điều chỉnh |
| B01 | 310 | NO_NGAN_HAN | 2022-12-31 | 4.139.842.500.551 | 6 | so sánh, đã điều chỉnh |
| B01 | 311 | PHAI_TRA_NGUOI_BAN_NH | 2022-12-31 | 518.872.943.502 | 6 | so sánh, đã điều chỉnh |
| B01 | 312 | NGUOI_MUA_TRA_TIEN_TRUOC_NH | 2022-12-31 | 404.574.778.485 | 6 | so sánh, đã điều chỉnh |
| B01 | 313 | THUE_PHAI_NOP_NHA_NUOC | 2022-12-31 | 104.068.206.717 | 6 | so sánh, đã điều chỉnh |
| B01 | 314 | PHAI_TRA_NGUOI_LAO_DONG | 2022-12-31 | 742.019.222.332 | 6 | so sánh, đã điều chỉnh |
| B01 | 315 | CHI_PHI_PHAI_TRA_NH | 2022-12-31 | 765.687.505.959 | 6 | so sánh, đã điều chỉnh |
| B01 | 318 | DOANH_THU_CHUA_THUC_HIEN_NH | 2022-12-31 | 103.116.358.852 | 6 | so sánh, đã điều chỉnh |
| B01 | 319 | PHAI_TRA_NGAN_HAN_KHAC | 2022-12-31 | 277.526.601.361 | 6 | so sánh, đã điều chỉnh |
| B01 | 320 | VAY_NO_THUE_TC_NH | 2022-12-31 | 1.196.684.057.443 | 6 | so sánh, đã điều chỉnh |
| B01 | 321 | DU_PHONG_PHAI_TRA_NH | 2022-12-31 | 7.645.560.576 | 6 | so sánh, đã điều chỉnh |
| B01 | 322 | QUY_KHEN_THUONG_PHUC_LOI | 2022-12-31 | 19.647.265.324 | 6 | so sánh, đã điều chỉnh |
| B01 | 330 | NO_DAI_HAN | 2022-12-31 | 283.078.885.803 | 6 | so sánh, đã điều chỉnh |
| B01 | 337 | PHAI_TRA_DAI_HAN_KHAC | 2022-12-31 | 14.247.000.000 | 6 | so sánh, đã điều chỉnh |
| B01 | 338 | VAY_NO_THUE_TC_DH | 2022-12-31 | 268.831.885.803 | 6 | so sánh, đã điều chỉnh |
| B01 | 400 | VON_CHU_SO_HUU_M400 | 2022-12-31 | 1.637.090.989.735 | 6 | so sánh, đã điều chỉnh |
| B01 | 410 | VON_CHU_SO_HUU_M410 | 2022-12-31 | 1.637.090.989.735 | 6 | so sánh, đã điều chỉnh |
| B01 | 411 | VON_GOP_CHU_SO_HUU | 2022-12-31 | 1.143.858.790.000 | 6 | so sánh, đã điều chỉnh |
| B01 | 412 | THANG_DU_VON | 2022-12-31 | -15.000.000 | 6 | so sánh, đã điều chỉnh |
| B01 | 417 | CHENH_LECH_TY_GIA | 2022-12-31 | 4.444.473.473 | 6 | so sánh, đã điều chỉnh |
| B01 | 421 | LNST_CHUA_PHAN_PHOI | 2022-12-31 | 488.751.824.367 | 6 | so sánh, đã điều chỉnh |
| B01 | 429 | LOI_ICH_CDKKS | 2022-12-31 | 50.901.895 | 6 | so sánh, đã điều chỉnh |
| B01 | 440 | TONG_NGUON_VON | 2022-12-31 | 6.060.012.376.089 | 6 | so sánh, đã điều chỉnh |
| B02 | 01 | DOANH_THU_BAN_HANG | 2023-10-01..2023-12-31 | 3.174.681.752.702 | 7 | kỳ này |
| B02 | 02 | CAC_KHOAN_GIAM_TRU_DT | 2023-10-01..2023-12-31 | 0 | 7 | kỳ này |
| B02 | 10 | DOANH_THU_THUAN | 2023-10-01..2023-12-31 | 3.174.681.752.702 | 7 | kỳ này |
| B02 | 11 | GIA_VON_HANG_BAN | 2023-10-01..2023-12-31 | 2.900.216.227.579 | 7 | kỳ này |
| B02 | 20 | LOI_NHUAN_GOP | 2023-10-01..2023-12-31 | 274.465.525.123 | 7 | kỳ này |
| B02 | 21 | DOANH_THU_TAI_CHINH | 2023-10-01..2023-12-31 | 30.214.815.185 | 7 | kỳ này |
| B02 | 22 | CHI_PHI_TAI_CHINH | 2023-10-01..2023-12-31 | 26.533.852.977 | 7 | kỳ này |
| B02 | 23 | CHI_PHI_DI_VAY | 2023-10-01..2023-12-31 | 18.283.290.994 | 7 | kỳ này |
| B02 | 25 | CHI_PHI_BAN_HANG | 2023-10-01..2023-12-31 | 0 | 7 | kỳ này |
| B02 | 26 | CHI_PHI_QUAN_LY | 2023-10-01..2023-12-31 | 96.956.306.117 | 7 | kỳ này |
| B02 | 30 | LOI_NHUAN_THUAN_HDKD | 2023-10-01..2023-12-31 | 181.190.181.214 | 7 | kỳ này |
| B02 | 31 | THU_NHAP_KHAC | 2023-10-01..2023-12-31 | 642.500.474 | 7 | kỳ này |
| B02 | 32 | CHI_PHI_KHAC | 2023-10-01..2023-12-31 | 3.043.426.192 | 7 | kỳ này |
| B02 | 40 | LOI_NHUAN_KHAC | 2023-10-01..2023-12-31 | -2.400.925.718 | 7 | kỳ này |
| B02 | 50 | LOI_NHUAN_TRUOC_THUE | 2023-10-01..2023-12-31 | 178.789.255.496 | 7 | kỳ này |
| B02 | 51 | CHI_PHI_THUE_TNDN_HIEN_HANH | 2023-10-01..2023-12-31 | 36.240.869.667 | 8 | kỳ này |
| B02 | 52 | CHI_PHI_THUE_TNDN_HOAN_LAI | 2023-10-01..2023-12-31 | -693.501.657 | 8 | kỳ này |
| B02 | 60 | LOI_NHUAN_SAU_THUE | 2023-10-01..2023-12-31 | 143.241.887.486 | 8 | kỳ này |
| B02 | 61 | LNST_CONG_TY_ME | 2023-10-01..2023-12-31 | 143.241.887.486 | 8 | kỳ này |
| B02 | 62 | LNST_CDKKS | 2023-10-01..2023-12-31 | 0 | 8 | kỳ này |
| B02 | 01 | DOANH_THU_BAN_HANG | 2022-10-01..2022-12-31 | 2.636.449.388.951 | 7 | so sánh, đã điều chỉnh |
| B02 | 02 | CAC_KHOAN_GIAM_TRU_DT | 2022-10-01..2022-12-31 | 0 | 7 | so sánh, đã điều chỉnh |
| B02 | 10 | DOANH_THU_THUAN | 2022-10-01..2022-12-31 | 2.636.449.388.951 | 7 | so sánh, đã điều chỉnh |
| B02 | 11 | GIA_VON_HANG_BAN | 2022-10-01..2022-12-31 | 2.402.900.387.349 | 7 | so sánh, đã điều chỉnh |
| B02 | 20 | LOI_NHUAN_GOP | 2022-10-01..2022-12-31 | 233.549.001.602 | 7 | so sánh, đã điều chỉnh |
| B02 | 21 | DOANH_THU_TAI_CHINH | 2022-10-01..2022-12-31 | 19.297.584.899 | 7 | so sánh, đã điều chỉnh |
| B02 | 22 | CHI_PHI_TAI_CHINH | 2022-10-01..2022-12-31 | 20.051.706.115 | 7 | so sánh, đã điều chỉnh |
| B02 | 23 | CHI_PHI_DI_VAY | 2022-10-01..2022-12-31 | 18.841.121.673 | 7 | so sánh, đã điều chỉnh |
| B02 | 25 | CHI_PHI_BAN_HANG | 2022-10-01..2022-12-31 | 0 | 7 | so sánh, đã điều chỉnh |
| B02 | 26 | CHI_PHI_QUAN_LY | 2022-10-01..2022-12-31 | 62.593.238.494 | 7 | so sánh, đã điều chỉnh |
| B02 | 30 | LOI_NHUAN_THUAN_HDKD | 2022-10-01..2022-12-31 | 170.201.641.892 | 7 | so sánh, đã điều chỉnh |
| B02 | 31 | THU_NHAP_KHAC | 2022-10-01..2022-12-31 | -20.668.737 | 7 | so sánh, đã điều chỉnh |
| B02 | 32 | CHI_PHI_KHAC | 2022-10-01..2022-12-31 | 490.234.068 | 7 | so sánh, đã điều chỉnh |
| B02 | 40 | LOI_NHUAN_KHAC | 2022-10-01..2022-12-31 | -510.902.805 | 7 | so sánh, đã điều chỉnh |
| B02 | 50 | LOI_NHUAN_TRUOC_THUE | 2022-10-01..2022-12-31 | 169.690.739.087 | 7 | so sánh, đã điều chỉnh |
| B02 | 51 | CHI_PHI_THUE_TNDN_HIEN_HANH | 2022-10-01..2022-12-31 | 34.379.096.201 | 8 | so sánh, đã điều chỉnh |
| B02 | 52 | CHI_PHI_THUE_TNDN_HOAN_LAI | 2022-10-01..2022-12-31 | 515.937.971 | 8 | so sánh, đã điều chỉnh |
| B02 | 60 | LOI_NHUAN_SAU_THUE | 2022-10-01..2022-12-31 | 134.795.704.915 | 8 | so sánh, đã điều chỉnh |
| B02 | 61 | LNST_CONG_TY_ME | 2022-10-01..2022-12-31 | 134.795.183.466 | 8 | so sánh, đã điều chỉnh |
| B02 | 62 | LNST_CDKKS | 2022-10-01..2022-12-31 | 521.449 | 8 | so sánh, đã điều chỉnh |
| B02 | 01 | DOANH_THU_BAN_HANG | 2023-01-01..2023-12-31 | 11.298.950.641.691 | 7 | kỳ này |
| B02 | 02 | CAC_KHOAN_GIAM_TRU_DT | 2023-01-01..2023-12-31 | 0 | 7 | kỳ này |
| B02 | 10 | DOANH_THU_THUAN | 2023-01-01..2023-12-31 | 11.298.950.641.691 | 7 | kỳ này |
| B02 | 11 | GIA_VON_HANG_BAN | 2023-01-01..2023-12-31 | 10.413.160.882.859 | 7 | kỳ này |
| B02 | 20 | LOI_NHUAN_GOP | 2023-01-01..2023-12-31 | 885.789.758.832 | 7 | kỳ này |
| B02 | 21 | DOANH_THU_TAI_CHINH | 2023-01-01..2023-12-31 | 104.557.514.596 | 7 | kỳ này |
| B02 | 22 | CHI_PHI_TAI_CHINH | 2023-01-01..2023-12-31 | 93.821.759.418 | 7 | kỳ này |
| B02 | 23 | CHI_PHI_DI_VAY | 2023-01-01..2023-12-31 | 85.743.481.662 | 7 | kỳ này |
| B02 | 25 | CHI_PHI_BAN_HANG | 2023-01-01..2023-12-31 | 0 | 7 | kỳ này |
| B02 | 26 | CHI_PHI_QUAN_LY | 2023-01-01..2023-12-31 | 248.934.257.517 | 7 | kỳ này |
| B02 | 30 | LOI_NHUAN_THUAN_HDKD | 2023-01-01..2023-12-31 | 647.591.256.493 | 7 | kỳ này |
| B02 | 31 | THU_NHAP_KHAC | 2023-01-01..2023-12-31 | 2.821.202.051 | 7 | kỳ này |
| B02 | 32 | CHI_PHI_KHAC | 2023-01-01..2023-12-31 | 4.962.920.976 | 7 | kỳ này |
| B02 | 40 | LOI_NHUAN_KHAC | 2023-01-01..2023-12-31 | -2.141.718.925 | 7 | kỳ này |
| B02 | 50 | LOI_NHUAN_TRUOC_THUE | 2023-01-01..2023-12-31 | 645.449.537.568 | 7 | kỳ này |
| B02 | 51 | CHI_PHI_THUE_TNDN_HIEN_HANH | 2023-01-01..2023-12-31 | 129.639.757.405 | 8 | kỳ này |
| B02 | 52 | CHI_PHI_THUE_TNDN_HOAN_LAI | 2023-01-01..2023-12-31 | -693.501.657 | 8 | kỳ này |
| B02 | 60 | LOI_NHUAN_SAU_THUE | 2023-01-01..2023-12-31 | 516.503.281.820 | 8 | kỳ này |
| B02 | 61 | LNST_CONG_TY_ME | 2023-01-01..2023-12-31 | 516.503.281.820 | 8 | kỳ này |
| B02 | 62 | LNST_CDKKS | 2023-01-01..2023-12-31 | 0 | 8 | kỳ này |
| B02 | 01 | DOANH_THU_BAN_HANG | 2022-01-01..2022-12-31 | 9.466.997.451.987 | 7 | so sánh, đã điều chỉnh |
| B02 | 02 | CAC_KHOAN_GIAM_TRU_DT | 2022-01-01..2022-12-31 | 0 | 7 | so sánh, đã điều chỉnh |
| B02 | 10 | DOANH_THU_THUAN | 2022-01-01..2022-12-31 | 9.466.997.451.987 | 7 | so sánh, đã điều chỉnh |
| B02 | 11 | GIA_VON_HANG_BAN | 2022-01-01..2022-12-31 | 8.710.415.826.649 | 7 | so sánh, đã điều chỉnh |
| B02 | 20 | LOI_NHUAN_GOP | 2022-01-01..2022-12-31 | 756.581.625.338 | 7 | so sánh, đã điều chỉnh |
| B02 | 21 | DOANH_THU_TAI_CHINH | 2022-01-01..2022-12-31 | 27.209.582.580 | 7 | so sánh, đã điều chỉnh |
| B02 | 22 | CHI_PHI_TAI_CHINH | 2022-01-01..2022-12-31 | 24.247.418.911 | 7 | so sánh, đã điều chỉnh |
| B02 | 23 | CHI_PHI_DI_VAY | 2022-01-01..2022-12-31 | 23.036.834.469 | 7 | so sánh, đã điều chỉnh |
| B02 | 25 | CHI_PHI_BAN_HANG | 2022-01-01..2022-12-31 | 0 | 7 | so sánh, đã điều chỉnh |
| B02 | 26 | CHI_PHI_QUAN_LY | 2022-01-01..2022-12-31 | 189.088.231.316 | 7 | so sánh, đã điều chỉnh |
| B02 | 30 | LOI_NHUAN_THUAN_HDKD | 2022-01-01..2022-12-31 | 570.455.557.691 | 7 | so sánh, đã điều chỉnh |
| B02 | 31 | THU_NHAP_KHAC | 2022-01-01..2022-12-31 | 1.095.349.823 | 7 | so sánh, đã điều chỉnh |
| B02 | 32 | CHI_PHI_KHAC | 2022-01-01..2022-12-31 | 1.255.335.941 | 7 | so sánh, đã điều chỉnh |
| B02 | 40 | LOI_NHUAN_KHAC | 2022-01-01..2022-12-31 | -159.986.118 | 7 | so sánh, đã điều chỉnh |
| B02 | 50 | LOI_NHUAN_TRUOC_THUE | 2022-01-01..2022-12-31 | 570.295.571.573 | 7 | so sánh, đã điều chỉnh |
| B02 | 51 | CHI_PHI_THUE_TNDN_HIEN_HANH | 2022-01-01..2022-12-31 | 113.448.466.450 | 8 | so sánh, đã điều chỉnh |
| B02 | 52 | CHI_PHI_THUE_TNDN_HOAN_LAI | 2022-01-01..2022-12-31 | 1.662.466.796 | 8 | so sánh, đã điều chỉnh |
| B02 | 60 | LOI_NHUAN_SAU_THUE | 2022-01-01..2022-12-31 | 455.184.638.327 | 8 | so sánh, đã điều chỉnh |
| B02 | 61 | LNST_CONG_TY_ME | 2022-01-01..2022-12-31 | 455.155.190.939 | 8 | so sánh, đã điều chỉnh |
| B02 | 62 | LNST_CDKKS | 2022-01-01..2022-12-31 | 29.447.388 | 8 | so sánh, đã điều chỉnh |
| B03-GT | 01 | LCTT_LOI_NHUAN_TRUOC_THUE | 2023-01-01..2023-12-31 | 645.449.537.568 | 9 | kỳ này |
| B03-GT | 02 | LCTT_KHAU_HAO | 2023-01-01..2023-12-31 | 258.284.662.943 | 9 | kỳ này |
| B03-GT | 03 | LCTT_DU_PHONG | 2023-01-01..2023-12-31 | 75.989.978.469 | 9 | kỳ này |
| B03-GT | 04 | LCTT_LAI_LO_TY_GIA | 2023-01-01..2023-12-31 | 18.255.616.875 | 9 | kỳ này |
| B03-GT | 05 | LCTT_LAI_LO_DAU_TU | 2023-01-01..2023-12-31 | -103.023.666.892 | 9 | kỳ này |
| B03-GT | 06 | LCTT_CHI_PHI_DI_VAY | 2023-01-01..2023-12-31 | 84.959.623.445 | 9 | kỳ này |
| B03-GT | 08 | LCTT_LN_TRUOC_THAY_DOI_VLD | 2023-01-01..2023-12-31 | 979.915.752.408 | 9 | kỳ này |
| B03-GT | 09 | LCTT_TANG_GIAM_PHAI_THU | 2023-01-01..2023-12-31 | -549.259.144.179 | 9 | kỳ này |
| B03-GT | 10 | LCTT_TANG_GIAM_HTK | 2023-01-01..2023-12-31 | 37.485.266.608 | 9 | kỳ này |
| B03-GT | 11 | LCTT_TANG_GIAM_PHAI_TRA | 2023-01-01..2023-12-31 | 326.670.917.409 | 9 | kỳ này |
| B03-GT | 12 | LCTT_TANG_GIAM_CHI_PHI_CHO_PHAN_BO | 2023-01-01..2023-12-31 | -14.821.345.530 | 9 | kỳ này |
| B03-GT | 14 | LCTT_CHI_PHI_DI_VAY_DA_TRA | 2023-01-01..2023-12-31 | -84.852.488.631 | 9 | kỳ này |
| B03-GT | 15 | LCTT_THUE_TNDN_DA_NOP | 2023-01-01..2023-12-31 | -57.698.258.718 | 9 | kỳ này |
| B03-GT | 17 | LCTT_TIEN_CHI_KHAC_HDKD | 2023-01-01..2023-12-31 | -62.918.726.007 | 9 | kỳ này |
| B03-GT | 20 | LCTT_THUAN_HDKD | 2023-01-01..2023-12-31 | 574.521.973.360 | 9 | kỳ này |
| B03-GT | 21 | LCTT_CHI_MUA_TSCD | 2023-01-01..2023-12-31 | -483.442.221.964 | 9 | kỳ này |
| B03-GT | 23 | LCTT_CHI_CHO_VAY | 2023-01-01..2023-12-31 | -1.549.000.000.000 | 9 | kỳ này |
| B03-GT | 24 | LCTT_THU_HOI_CHO_VAY | 2023-01-01..2023-12-31 | 1.771.000.000.000 | 9 | kỳ này |
| B03-GT | 27 | LCTT_THU_LAI_CO_TUC | 2023-01-01..2023-12-31 | 102.671.543.751 | 9 | kỳ này |
| B03-GT | 30 | LCTT_THUAN_HDDT | 2023-01-01..2023-12-31 | -158.770.678.213 | 9 | kỳ này |
| B03-GT | 33 | LCTT_THU_DI_VAY | 2023-01-01..2023-12-31 | 2.455.208.243.463 | 9 | kỳ này |
| B03-GT | 34 | LCTT_TRA_NO_GOC_VAY | 2023-01-01..2023-12-31 | -2.347.122.939.482 | 9 | kỳ này |
| B03-GT | 36 | LCTT_CO_TUC_DA_TRA | 2023-01-01..2023-12-31 | -119.893.347.350 | 9 | kỳ này |
| B03-GT | 40 | LCTT_THUAN_HDTC | 2023-01-01..2023-12-31 | -11.808.043.369 | 9 | kỳ này |
| B03-GT | 01 | LCTT_LOI_NHUAN_TRUOC_THUE | 2022-01-01..2022-12-31 | 570.295.571.573 | 9 | so sánh, đã điều chỉnh |
| B03-GT | 02 | LCTT_KHAU_HAO | 2022-01-01..2022-12-31 | 206.442.863.972 | 9 | so sánh, đã điều chỉnh |
| B03-GT | 03 | LCTT_DU_PHONG | 2022-01-01..2022-12-31 | 32.256.829.962 | 9 | so sánh, đã điều chỉnh |
| B03-GT | 04 | LCTT_LAI_LO_TY_GIA | 2022-01-01..2022-12-31 | 728.145.288 | 9 | so sánh, đã điều chỉnh |
| B03-GT | 05 | LCTT_LAI_LO_DAU_TU | 2022-01-01..2022-12-31 | -25.468.252.582 | 9 | so sánh, đã điều chỉnh |
| B03-GT | 06 | LCTT_CHI_PHI_DI_VAY | 2022-01-01..2022-12-31 | 23.036.834.469 | 9 | so sánh, đã điều chỉnh |
| B03-GT | 08 | LCTT_LN_TRUOC_THAY_DOI_VLD | 2022-01-01..2022-12-31 | 807.291.992.682 | 9 | so sánh, đã điều chỉnh |
| B03-GT | 09 | LCTT_TANG_GIAM_PHAI_THU | 2022-01-01..2022-12-31 | -861.044.938.908 | 9 | so sánh, đã điều chỉnh |
| B03-GT | 10 | LCTT_TANG_GIAM_HTK | 2022-01-01..2022-12-31 | -253.241.452.178 | 9 | so sánh, đã điều chỉnh |
| B03-GT | 11 | LCTT_TANG_GIAM_PHAI_TRA | 2022-01-01..2022-12-31 | 576.199.823.077 | 9 | so sánh, đã điều chỉnh |
| B03-GT | 12 | LCTT_TANG_GIAM_CHI_PHI_CHO_PHAN_BO | 2022-01-01..2022-12-31 | -13.758.137.732 | 9 | so sánh, đã điều chỉnh |
| B03-GT | 14 | LCTT_CHI_PHI_DI_VAY_DA_TRA | 2022-01-01..2022-12-31 | -20.915.408.047 | 9 | so sánh, đã điều chỉnh |
| B03-GT | 15 | LCTT_THUE_TNDN_DA_NOP | 2022-01-01..2022-12-31 | -107.575.383.402 | 9 | so sánh, đã điều chỉnh |
| B03-GT | 17 | LCTT_TIEN_CHI_KHAC_HDKD | 2022-01-01..2022-12-31 | -54.724.549.723 | 9 | so sánh, đã điều chỉnh |
| B03-GT | 20 | LCTT_THUAN_HDKD | 2022-01-01..2022-12-31 | 72.231.945.769 | 9 | so sánh, đã điều chỉnh |
| B03-GT | 21 | LCTT_CHI_MUA_TSCD | 2022-01-01..2022-12-31 | -406.520.385.050 | 9 | so sánh, đã điều chỉnh |
| B03-GT | 23 | LCTT_CHI_CHO_VAY | 2022-01-01..2022-12-31 | -1.652.000.000.000 | 9 | so sánh, đã điều chỉnh |
| B03-GT | 24 | LCTT_THU_HOI_CHO_VAY | 2022-01-01..2022-12-31 | 530.000.000.000 | 9 | so sánh, đã điều chỉnh |
| B03-GT | 27 | LCTT_THU_LAI_CO_TUC | 2022-01-01..2022-12-31 | 8.605.926.554 | 9 | so sánh, đã điều chỉnh |
| B03-GT | 30 | LCTT_THUAN_HDDT | 2022-01-01..2022-12-31 | -1.519.914.458.496 | 9 | so sánh, đã điều chỉnh |
| B03-GT | 33 | LCTT_THU_DI_VAY | 2022-01-01..2022-12-31 | 1.322.688.329.908 | 9 | so sánh, đã điều chỉnh |
| B03-GT | 34 | LCTT_TRA_NO_GOC_VAY | 2022-01-01..2022-12-31 | -37.291.965.423 | 9 | so sánh, đã điều chỉnh |
| B03-GT | 36 | LCTT_CO_TUC_DA_TRA | 2022-01-01..2022-12-31 | -91.877.102.750 | 9 | so sánh, đã điều chỉnh |
| B03-GT | 40 | LCTT_THUAN_HDTC | 2022-01-01..2022-12-31 | 1.193.519.261.735 | 9 | so sánh, đã điều chỉnh |
| B03-GT | 50 | LCTT_THUAN_TRONG_KY | 2022-01-01..2022-12-31 | -254.163.250.992 | 9 | so sánh, đã điều chỉnh |
| B03-GT | 60 | TIEN_DAU_KY | 2021-12-31 | 668.144.408.661 | 9 | so sánh, đã điều chỉnh |
| B03-GT | 61 | LCTT_ANH_HUONG_TY_GIA | 2022-01-01..2022-12-31 | -120.035.690 | 9 | so sánh, đã điều chỉnh |
| B03-GT | 70 | TIEN_CUOI_KY | 2022-12-31 | 413.861.121.979 | 9 | so sánh, đã điều chỉnh |

Ngoại lệ:

| Trang | Loại | Mã | Đọc được | Chi tiết |
|---|---|---|---|---|
| 8 | ma_la | 70 | 20. Lãi cơ bản trên cổ phiếu | null |
| 9 | lech_can_doi | 70 | null | TIEN_CUOI_KY = + TIEN_DAU_KY + LCTT_THUAN_TRONG_KY + LCTT_ANH_HUONG_TY_GIA: cộng ra 817851176502, lệch 30000000000, cột 2023-01-01..2023-12-31 |
