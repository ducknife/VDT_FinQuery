# Giao diện — v1.1: đăng nhập Keycloak, lịch sử hội thoại, feedback

*Ngày: 2026-10-13 · Kế thừa [giao-dien_v1.0.md](giao-dien_v1.0.md) (bộ nhận diện giữ nguyên) · QĐ-044 ([quyet-dinh_v1.27.md](quyet-dinh_v1.27.md))*

## 1. Luồng đăng nhập

```
FinQuery (5173): chưa đăng nhập → màn hình FinQuery có nút "Đăng nhập"
  → trang đăng nhập Keycloak (8180): email + mật khẩu (không có đăng ký)
  → về lại 5173 kèm code → keycloak-js đổi code lấy token (PKCE S256)
  → GET /api/v1/me (Bearer token) → core trả roles + permissions
  → có quyền: vào ứng dụng; chưa có vai trò: "tài khoản đang chờ quản trị viên duyệt"
```

- Form mật khẩu nằm ở trang Keycloak; frontend không bao giờ cầm mật khẩu.
- `check-sso` + `public/silent-check-sso.html`: đã đăng nhập Keycloak thì vào thẳng, không hỏi lại.
- Token gia hạn trước mỗi lần gọi API (`updateToken(30)`); hết phiên thì chuyển sang trang đăng nhập.
- Thanh trên: tên người dùng, nhãn vai trò (Quản trị / Quản lý tài liệu / Nhân viên), nút Đăng xuất.

## 2. Thành phần

| File | Việc |
|---|---|
| `src/auth/auth.ts` | `keycloak-js` 26.2.4 (ghim phiên bản); `initAuth` (một lần, an toàn với StrictMode), `login`, `logout`, `authHeaders`, `apiFetch` |
| `src/components/AuthGate.tsx` | Trạng thái trước khi vào ứng dụng: đang kiểm, đăng nhập, chờ duyệt, lỗi (nhắc kiểm Keycloak/backend) |
| `src/api/ask.ts` | `/api/v1/ask` (SSE) gửi kèm token và `conversationId`; ảnh trang tài liệu tải bằng `fetch` có token thành blob (`<img src>` không gắn được header), thu hồi URL khi đóng |
| `src/api/conversations.ts` | `/me`, danh sách và chi tiết hội thoại, gửi feedback |
| `src/components/ConversationList.tsx` | Thanh bên: "Cuộc hội thoại mới", lịch sử (tiêu đề, thời gian); màn hình hẹp thành ngăn kéo mở bằng ☰ |
| `src/components/FeedbackBar.tsx` | Đúng / Sai một phần / Sai toàn bộ; sai bắt buộc ghi lý do; hiện đánh giá đã gửi |
| `src/api/turn.ts` | Sự kiện `saved` (mã câu trả lời để feedback), bước "Ngữ cảnh" (dùng lịch sử, góp ý từ feedback); `turnFromMessage` dựng lại lượt từ hội thoại đã lưu |
| `.env.development` | `VITE_KEYCLOAK_URL/REALM/CLIENT` (không bí mật). Để trống `VITE_KEYCLOAK_URL` thì tắt đăng nhập |

## 3. Hội thoại

- Câu đầu: core tạo hội thoại, trả mã ở sự kiện `saved`. Các câu sau gửi kèm `conversationId`: core gửi kèm 3 lượt gần nhất nguyên văn cùng bản tóm tắt các lượt cũ (compaction).
- Ô nhập đổi gợi ý thành "Hỏi tiếp, ví dụ: còn năm trước thì sao?" khi đang trong một hội thoại.
- Mở lại hội thoại cũ: câu hỏi, câu trả lời, trích dẫn (bấm xem trang được), các bước (từ dấu vết tool), đánh giá đã gửi.

## 4. Kiểm

- `tsc -b`, `eslint`, `vite build` sạch.
- Keycloak chấp nhận `redirect_uri` `http://localhost:5173/` và `.../silent-check-sso.html`; trang đăng nhập không còn link đăng ký; CORS của token endpoint trả `Access-Control-Allow-Origin: http://localhost:5173`.
- Chưa chạy luồng đầy đủ trên trình duyệt (cần backend bật `FINQ_SECURITY_ENABLED=true`). Hội thoại nhiều lượt và compaction chưa chạy thật (tốn LLM).

## 5. Chạy

1. Keycloak: `infra/keycloak` → `docker compose -f docker-compose.keycloak.yml up -d`.
2. Backend: thêm `FINQ_SECURITY_ENABLED=true` vào file môi trường của backend, chạy ở 8080.
3. Frontend: `npm run dev` → `http://localhost:5173`.
