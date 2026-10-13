import react, { reactCompilerPreset } from '@vitejs/plugin-react'
import babel from '@rolldown/plugin-babel'
import { defineConfig } from 'vite'

// https://vite.dev/config/
export default defineConfig({
  plugins: [
    react(),
    babel({ presets: [reactCompilerPreset()] })
  ],
  // gọi backend qua cùng origin khi chạy dev: /api -> Spring Boot (FINQ_API đổi địa chỉ, mặc định cổng 8080)
  server: {
    proxy: {
      '/api': process.env.FINQ_API ?? 'http://localhost:8080',
    },
  },
})
