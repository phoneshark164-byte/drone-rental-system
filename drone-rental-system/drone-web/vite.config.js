import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import path from 'path'

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': path.resolve(__dirname, 'src'),
      '#': path.resolve(__dirname, 'src/submodule')
    },
    extensions: ['.mjs', '.js', '.ts', '.jsx', '.tsx', '.json', '.vue']
  },
  server: {
    port: 5173,
    proxy: {
      // 用户 API - 转发到 Spring Boot 后端
      '/user/api': {
        target: 'http://localhost:8081',
        changeOrigin: true
      },
      // 管理员 API
      '/admin/api': {
        target: 'http://localhost:8081',
        changeOrigin: true
      },
      // 运营方 API
      '/operator/api': {
        target: 'http://localhost:8081',
        changeOrigin: true
      },
      // 上传文件访问
      '/api/uploads': {
        target: 'http://localhost:8081',
        changeOrigin: true
      },
      // 验证码 API
      '/api/captcha': {
        target: 'http://localhost:8081',
        changeOrigin: true
      },
      // 检测 API - Flask 服务
      '/detection-api': {
        target: 'http://localhost:5000',
        changeOrigin: true
      }
    }
  }
})
