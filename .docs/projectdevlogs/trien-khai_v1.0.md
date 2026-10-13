# Triển khai bằng Docker — v1.0

*Ngày: 2026-10-13 · File: `docker-compose.yml` (gốc repo), `backend/core-service/Dockerfile`, `frontend/Dockerfile`, `frontend/nginx.conf`*

## 1. Mô hình

```
trình duyệt ──► finq-frontend (nginx, cổng 5173) ──/api──► finq-backend (Spring Boot, cổng 8080)
    │                                                         │ host.docker.internal
    └──► Keycloak (8180) ◄─────────── khoá công khai, Admin API ┤
                                                               ├──► MySQL trên máy (3306)
                                                               ├──► Qdrant (6334)
                                                               └──► DeepInfra (LLM, embedding, rerank, OCR)
```

- **Đóng gói mới:** backend và giao diện (project compose `finquery`).
- **Giữ nguyên, chạy riêng như trước:** MySQL trên máy, Qdrant (`infra/qdrant`, volume `finq_qdrant_storage`), Keycloak + Mailpit (`infra/keycloak`).
- Không gộp các file compose: đổi project của Keycloak sẽ tạo lại container và **mất tài khoản** (Keycloak chưa có volume dữ liệu).

## 2. Image

| Image | Build | Chạy |
|---|---|---|
| `finquery-backend` | `maven:3.9.9-eclipse-temurin-21`, bộ đệm Maven (`--mount=type=cache`) để build lại không tải lại thư viện; bỏ qua test | `eclipse-temurin:21.0.5_11-jre-jammy` + `fontconfig`, `fonts-dejavu-core` (PDFBox vẽ ảnh trang); `JAVA_OPTS=-XX:MaxRAMPercentage=75`, `mem_limit 1g` |
| `finquery-frontend` | `node:22.12.0-alpine` (Vite 8 cần Node ≥ 22.12), `npm ci`, `npm run build` với `.env.production` | `nginx:1.27.3-alpine`; `/api` chuyển sang `backend:8080` với `proxy_buffering off`, `proxy_read_timeout 600s` (SSE); tải lên tối đa 100 MB; ứng dụng một trang (`try_files … /index.html`) |

`.dockerignore`: backend không đưa `.env`, `target` vào image; frontend không đưa `node_modules`, `dist`.

## 3. Cấu hình backend trong container

- **Bí mật:** `backend/core-service/.env` gắn chỉ đọc vào `/app/.env`; Spring đọc qua `spring.config.import` như khi chạy trên máy. Không dùng `env_file` của Compose, vì Compose tự thay `$xxx` trong giá trị (xem xac-thuc_v1.2), còn Spring đọc nguyên văn.
- **Biến đè** (biến môi trường thắng giá trị trong `.env`):
  - `DB_URL=jdbc:mysql://host.docker.internal:3306/FINQUERY`;
  - `QDRANT_HOST=host.docker.internal`;
  - `OCR_OUTPUT_DIR=/app/data/ocr`, `RAW_DIR=/app/data/raw` (thư mục `./data` gắn vào `/app/data`);
  - `KEYCLOAK_ISSUER_URI=http://localhost:8180/realms/finquery` (giữ đúng `iss` của token trình duyệt lấy);
  - `SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_JWK_SET_URI=http://host.docker.internal:8180/realms/finquery/protocol/openid-connect/certs`: có `jwk-set-uri` thì Spring tải khoá ở đây và chỉ dùng `issuer-uri` để kiểm `iss`, không gọi discovery tới `localhost` (trong container `localhost` là chính nó);
  - `KEYCLOAK_URL=http://host.docker.internal:8180` (Admin API);
  - `FINQ_FRONTEND_URL=http://localhost:5173/`.
- `extra_hosts: host.docker.internal:host-gateway` để chạy được cả ngoài Docker Desktop.
- Giao diện giữ cổng **5173**, vì Keycloak đã đăng ký địa chỉ này cho `finquery-web` (CORS, chuyển hướng).

## 4. Kiểm (13/10)

- `docker compose build` thành công cả hai image; `docker compose up -d`: backend khởi động 16 s, nối MySQL (cả user chỉ đọc `finq_reader`).
- Không token: `/api/v1/me` trả **401** (bảo mật bật).
- Tài khoản admin tạm (tạo bằng kcadm, mật khẩu ngẫu nhiên, xoá sau khi kiểm), gọi qua nginx cổng 5173:
  - trang chủ 200;
  - `/api/v1/me` đúng tên, vai trò;
  - hội thoại 200; danh sách tài liệu 200;
  - **ảnh trang PDF 200** (JPEG 162 KB, PDFBox chạy trong container); chữ OCR 200;
  - `/api/v1/admin/users` 200 (Admin API của Keycloak qua `host.docker.internal`).
- Container nối được Qdrant gRPC (`host.docker.internal:6334`).
- RAM: backend 482 MiB / 1 GiB, frontend 13 MiB, Keycloak 562 MiB, Qdrant 279 MiB, Mailpit 35 MiB.
- **Chưa kiểm:** hỏi đáp SSE qua nginx (cần số dư DeepInfra); mail đặt mật khẩu tạo từ container.

## 5. Lưu ý

- Link trong mail đặt mật khẩu có dạng `http://host.docker.internal:8180/...`, vì Keycloak dựng link theo địa chỉ backend gọi tới. Trên Windows có Docker Desktop thì tên này có trong file hosts nên trình duyệt mở được. Muốn link là `localhost` thì đặt `KC_HOSTNAME=http://localhost:8180` cho Keycloak (phải tạo lại container: thêm volume dữ liệu trước).
- Sửa code thì chạy `docker compose up -d --build` để build lại.
