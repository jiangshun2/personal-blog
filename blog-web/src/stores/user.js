/**
 * 用户状态：token 与登录用户信息
 * 登录成功后同时写入 localStorage，刷新页面不丢失登录态
 */
import { defineStore } from 'pinia'
import { login as loginApi, getMe } from '@/api/auth'
import { TOKEN_KEY, USER_INFO_KEY } from '@/utils/request'

/** 从 localStorage 安全读取用户信息（防止脏数据导致 JSON.parse 报错） */
function readUserInfo() {
  try {
    const raw = localStorage.getItem(USER_INFO_KEY)
    return raw ? JSON.parse(raw) : null
  } catch (e) {
    return null
  }
}

export const useUserStore = defineStore('user', {
  state: () => ({
    token: localStorage.getItem(TOKEN_KEY) || '',
    userInfo: readUserInfo()
  }),

  getters: {
    /** 是否已登录 */
    isLoggedIn: (state) => !!state.token,
    /** 展示名称：优先昵称，其次用户名 */
    displayName: (state) => {
      if (!state.userInfo) return '管理员'
      return state.userInfo.nickname || state.userInfo.username || '管理员'
    },
    avatar: (state) => (state.userInfo && state.userInfo.avatar) || ''
  },

  actions: {
    /**
     * 登录
     * @param {Object} form { username, password }
     */
    async login(form) {
      const data = await loginApi(form)
      this.token = (data && data.token) || ''
      this.userInfo = (data && data.userInfo) || null
      localStorage.setItem(TOKEN_KEY, this.token)
      localStorage.setItem(USER_INFO_KEY, JSON.stringify(this.userInfo))
      return data
    },

    /** 拉取当前登录用户信息（页面刷新后调用） */
    async fetchUserInfo() {
      const data = await getMe()
      this.userInfo = data || null
      localStorage.setItem(USER_INFO_KEY, JSON.stringify(this.userInfo))
      return data
    },

    /** 退出登录：清空内存与本地存储 */
    logout() {
      this.token = ''
      this.userInfo = null
      localStorage.removeItem(TOKEN_KEY)
      localStorage.removeItem(USER_INFO_KEY)
    }
  }
})
