# syntax=docker/dockerfile:1
# FinQuery web: build React bằng Node, phục vụ bằng nginx; nginx chuyển /api sang backend (kể cả SSE).

FROM node:22.12.0-alpine AS build
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci
COPY . .
# biến VITE_* (địa chỉ Keycloak, không bí mật) lấy từ .env.production
RUN npm run build

FROM nginx:1.27.3-alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html
EXPOSE 80
