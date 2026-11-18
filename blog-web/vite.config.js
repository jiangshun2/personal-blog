import fs from 'node:fs'
import { fileURLToPath, URL } from 'node:url'
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// Windows 下部分安全软件会短暂占用 esbuild 的临时文件，导致构建结束删除临时文件时报
// "Access is denied"（EPERM）。把临时目录重定向到项目内的 node_modules 可规避该问题。
if (process.platform === 'win32') {
  const localTempDir = fileURLToPath(new URL('./node_modules/.vite-temp', import.meta.url))
  try {
    fs.mkdirSync(localTempDir, { recursive: true })
    process.env.TEMP = localTempDir
    process.env.TMP = localTempDir
  } catch (e) {
    // 重定向失败时退回系统临时目录，不影响正常构建
  }
}

// Vite 配置：https://vitejs.dev/config/
export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      // 用 @ 指向 src 目录，避免层层相对路径
      '@': fileURLToPath(new URL('./src', import.meta.url))
    }
  },
  server: {
    host: '0.0.0.0',
    port: 5173,
    open: false,
    // 开发环境代理：前端 /api 开头的请求转发到 Spring Boot 后端（8080）
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true
      }
    }
  },
  build: {
    outDir: 'dist',
    sourcemap: false,
    // Element Plus + highlight.js 体积较大，放宽单文件体积告警阈值
    chunkSizeWarningLimit: 1600
  }
})
