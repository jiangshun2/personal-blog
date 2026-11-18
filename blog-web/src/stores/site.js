/**
 * 站点配置状态：全局共享站点名称、描述、头像、社交链接
 */
import { defineStore } from 'pinia'
import { getSiteConfig } from '@/api/site'

/** 后端未返回配置时的兜底值 */
const DEFAULT_CONFIG = {
  site_name: '我的个人博客',
  site_description: '记录学习与生活的地方',
  site_avatar: '',
  github: '',
  email: ''
}

export const useSiteStore = defineStore('site', {
  state: () => ({
    config: { ...DEFAULT_CONFIG },
    loaded: false,
    loading: false
  }),

  getters: {
    siteName: (state) => state.config.site_name || DEFAULT_CONFIG.site_name,
    siteDescription: (state) => state.config.site_description || DEFAULT_CONFIG.site_description,
    siteAvatar: (state) => state.config.site_avatar || '',
    github: (state) => state.config.github || '',
    email: (state) => state.config.email || ''
  },

  actions: {
    /**
     * 获取站点配置（默认只请求一次，force = true 强制刷新）
     */
    async fetchConfig(force) {
      if (this.loading) return this.config
      if (this.loaded && !force) return this.config

      this.loading = true
      try {
        const data = await getSiteConfig()
        this.config = { ...DEFAULT_CONFIG, ...(data || {}) }
        this.loaded = true
        return this.config
      } catch (e) {
        // 配置读取失败时使用兜底值，不阻塞页面渲染
        return this.config
      } finally {
        this.loading = false
      }
    }
  }
})
