/**
 * Axios 统一封装
 *
 * 1. baseURL 固定为 /api（开发环境由 vite.config.js 代理到 http://localhost:8080）
 * 2. 请求拦截器：自动把 localStorage 中的 token 注入 Authorization: Bearer xxx
 * 3. 响应拦截器：
 *    - 业务码 code !== 0 时用 ElMessage 提示并 reject
 *    - HTTP 401 时清除 token，并跳转到 /admin/login
 *    - 成功时直接返回后端 data 字段，业务代码无需再判断 code
 */
import axios from 'axios'
import { ElMessage } from 'element-plus'

/** localStorage 中 token 的键名（与 Pinia user store 保持一致） */
export const TOKEN_KEY = 'token'
/** localStorage 中用户信息的键名 */
export const USER_INFO_KEY = 'userInfo'

const request = axios.create({
  baseURL: '/api',
  timeout: 15000
})

/** 清除本地登录态并跳转到后台登录页（携带 redirect 便于登录后回到原页面） */
function handleUnauthorized() {
  localStorage.removeItem(TOKEN_KEY)
  localStorage.removeItem(USER_INFO_KEY)

  const current = window.location.pathname + window.location.search + window.location.hash
  // 已经在登录页就不再跳转，避免死循环
  if (current.indexOf('/admin/login') === 0) return

  window.location.href = '/admin/login?redirect=' + encodeURIComponent(current)
}

// ==================== 请求拦截器：注入 token ====================
request.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem(TOKEN_KEY)
    if (token) {
      config.headers.Authorization = 'Bearer ' + token
    }
    return config
  },
  (error) => Promise.reject(error)
)

// ==================== 响应拦截器：统一处理业务码与异常 ====================
request.interceptors.response.use(
  (response) => {
    const res = response.data
    // 非标准 JSON（如文件流）直接透传
    if (!res || typeof res !== 'object' || typeof res.code === 'undefined') {
      return res
    }

    if (res.code !== 0) {
      if (res.code === 4010) {
        ElMessage.error(res.message || '登录已过期，请重新登录')
        handleUnauthorized()
      } else {
        ElMessage.error(res.message || '请求失败')
      }
      return Promise.reject(new Error(res.message || '请求失败'))
    }

    // 成功：只把 data 交给业务层
    return res.data
  },
  (error) => {
    const status = error.response ? error.response.status : 0

    if (status === 401) {
      ElMessage.error('登录已过期，请重新登录')
      handleUnauthorized()
    } else if (status === 429) {
      ElMessage.warning('请求过于频繁，请稍后再试')
    } else {
      const data = error.response ? error.response.data : null
      const message = (data && data.message) || error.message || '网络异常，请稍后重试'
      ElMessage.error(message)
    }
    return Promise.reject(error)
  }
)

export default request
