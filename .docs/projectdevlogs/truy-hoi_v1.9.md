# Truy hồi và sinh câu trả lời — v1.9

*Ngày: 2026-10-12, 20:48 · Kế thừa [truy-hoi_v1.8.md](truy-hoi_v1.8.md) · Quyết định: QĐ-035, QĐ-036 ([quyet-dinh_v1.20.md](quyet-dinh_v1.20.md))*

Bản v1.8 giữ nguyên. Bản này chỉ ghi phần mới: đổi model suy luận sang Sonnet 5.5 và đo lại cả luồng.

## 1. Thay đổi

| Chỗ | Trước | Sau |
|---|---|---|
| `.env` `REASONING_MODEL` | `Qwen/Qwen3.5-397B-A17B` | `anthropic/claude-sonnet-5-5` |
| `application.yml` `llm.price-*` | 0,45 / 3,00 | 2,00 / 10,00 USD mỗi 1 triệu token |
| `ModelOptions.thinking` | luôn gửi `chat_template_kwargs` | chỉ gửi khi model là `Qwen/...`. Model khác gọi mặc định, không thêm tham số (yêu cầu của người làm đề tài) |
| `RetrievalService` | Qdrant 120 → bỏ tiếng Anh → giữ 80 | Qdrant 80 → bỏ tiếng Anh → rerank phần còn lại (QĐ-036) |
| `CitationVerifier.normalize` | bỏ `| * # \` _` | bỏ thêm gạch đầu dòng đứng riêng và dấu `:` |

**Về thinking của Sonnet.** Trang API của DeepInfra cho Sonnet 5.5 không ghi tham số bật thinking. Vì vậy Sonnet chạy ở chế độ mặc định của nhà cung cấp, không khẳng định có hay không có thinking mở rộng. Chất lượng được đánh giá bằng kết quả: câu trích, số liệu, cách nói về phần thiếu.

## 2. Kết quả trên 3 câu

Điều kiện: prompt v1.2, Qdrant 80 chunk, có lọc tiếng Anh. Log: `data/eval/ket-qua/agent-thu_20261012-204719.md`, `-204741.md`, `-204805.md`.

| Câu | Model | Lần tìm | Agent | Tool | Trả lời | **Tổng** | Câu trích ✓ | USD |
|---|---|---|---|---|---|---|---|---|
| V02 nguyên nhân LN Q3/2025 | Qwen | 1 | 37,7 s | 20,2 s | 75,6 s | 133,7 s | 1/1 | 0,024 |
| | **Sonnet** | 1 | 6,1 s | 7,8 s | 13,1 s | **27,2 s** | **4/4** | 0,079 |
| Cổ tức 2024 | Qwen | 1 | 34,0 s | 11,9 s | 121,0 s | 167,0 s | 1/2 | 0,017 |
| | **Sonnet** | 1 | 5,3 s | 10,1 s | 6,1 s | **21,6 s** | **6/6** | 0,086 |
| Kế hoạch LNST năm nay | Qwen | 1 | 24,6 s | 14,4 s | 38,8 s | 77,8 s | 4/4 | 0,016 |
| | **Sonnet** | 1 | 6,3 s | 13,7 s | 4,4 s | **24,4 s** | **2/2** | 0,079 |

- Mỗi lần gọi Sonnet ở vòng agent mất 1,6–4,1 s, so với 6,7–27,3 s của Qwen.
- Không có số nào lệch nguồn, không có mã trích dẫn lạ, cả 3 câu `answered`.
- Với Sonnet, **truy hồi trở thành phần lớn nhất** (8–14 s, chiếm 35–55% tổng). Muốn nhanh hơn nữa thì tối ưu ở truy hồi.

**Đọc câu trả lời:**
- **V02:** nói thẳng công văn chỉ giải thích bằng biến động các khoản mục; ghi ở phần thiếu rằng tài liệu không nêu nguyên nhân kinh doanh cụ thể. Không suy đoán.
- **Cổ tức 2024:** tỷ lệ 21,5% (2.150 đồng/cổ phiếu), ngày đăng ký cuối cùng 01/10/2025, ngày thanh toán 15/10/2025, đối chiếu ba nguồn (thông báo, nghị quyết, báo cáo năm 2026).
- **Kế hoạch LNST năm nay:** hiểu đúng "năm nay" là 2026. Kế hoạch 622,4 tỷ đồng, tăng 3,8%; phân biệt hợp nhất và công ty mẹ (596 tỷ).

## 3. Còn mở

- Đo xen kẽ bật/tắt lọc tiếng Anh cho độ trễ rerank (`LatencyProbeTest -Dlatency.ab=true`), giờ là phần lớn nhất của thời gian.
- Chạy lại benchmark truy hồi với bản 80 chunk không bù (QĐ-036).
- Xem 5 câu nDCG giảm sau khi lọc: V02, V03, B01, D05, C06.
- Đánh giá câu trả lời trên đủ 50 câu, khoảng 4 USD với Sonnet.
