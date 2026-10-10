# Kế hoạch 6 tuần — v1.0

*Ngày: 2026-09-17 · Hạn nộp: 31/10/2026*

## Lộ trình

| Tuần | Thời gian | Việc chính | Đầu ra | Trạng thái |
|---|---|---|---|---|
| **1** | 17–27/9 | Sửa crawler, thêm nguồn CBTT, loại trùng. Schema DB, nạp vnstock (tách LCTT quý). Nhập tay kế hoạch, cổ tức, phát hành. **Bắt đầu bộ câu hỏi chuẩn** | Kho tài liệu sạch, DB có số liệu, khoảng 40 câu hỏi chuẩn | 🔄 Tải dữ liệu **xong 17/9** |
| **2** | 28/9–4/10 | **OCR** (QĐ-008), sau đó PDF → chunk, table-to-json, metadata (kỳ báo cáo, hợp nhất/riêng, loại văn bản, **số trang**). Tạo embedding và nạp vector store | Chỉ mục vector trích dẫn được đến số trang | ⬜ |
| **3** | 5–11/10 | Kết nối nhà cung cấp LLM. Thư viện hàm SQL theo A–F, hàm tính chỉ số trong Java, router DB / RAG / kết hợp | Nhánh DB chạy được từ đầu đến cuối | ⬜ |
| **4** | 12–18/10 | Chuẩn hoá truy vấn (từ điển viết tắt tiếng Việt), trích từ khoá, sidecar rerank 2 tầng, sinh câu trả lời kèm trích dẫn, trả lời "không xác định" | Nhánh RAG và nhánh kết hợp chạy được | ⬜ |
| **5** | 19–25/10 | Hai baseline (text-to-SQL, RAG cơ bản). Công cụ đánh giá (độ chính xác, trích dẫn, độ trễ, token, chi phí). Hoàn thiện bộ khoảng 100–150 câu. Frontend tối thiểu | Bảng kết quả thô | ⬜ |
| **6** | 26–31/10 | Phân tích kết quả, kiểm định Welch, sửa lỗi, viết báo cáo và slide | Nộp bài | ⬜ |

Frontend (khung chat, hiển thị trích dẫn, trang kết quả đánh giá) làm xen kẽ ở tuần 3–5.

## Rủi ro mới phát hiện (17/9)

- **OCR khoảng 6.700 trang** chưa được tính trong kế hoạch ban đầu. Cách giảm rủi ro:
  - OCR theo thứ tự ưu tiên (xem [du-lieu_v1.0.md](du-lieu_v1.0.md) §5),
  - chạy OCR qua đêm,
  - nếu vẫn không kịp thì RAG tuần 2 chỉ dùng BCTN (sạch) cộng BCTC hợp nhất năm và bán niên.
- **Phiên bản Spring AI** có thể không khớp với Boot 4 (xem [cong-nghe_v1.0.md](cong-nghe_v1.0.md)).

## Thứ tự cắt giảm khi trễ
1. HyDE và diễn giải lại truy vấn (paper cho thấy hai bước này làm giảm NDCG).
2. Cơ chế chia đôi và hợp nhất khi vượt 64k token (với top-20 chunk gần như không bao giờ chạm ngưỡng; chỉ cần nhắc trong báo cáo).
3. Dữ liệu 2019–2020 (toàn bộ BCTC hai năm này là bản scan).
4. Streaming qua WebSocket.
5. Điều lệ, quy chế, bản cáo bạch (không OCR).

**Không được cắt:** bộ câu hỏi chuẩn và phần đánh giá.
