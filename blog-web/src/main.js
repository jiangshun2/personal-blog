/**
 * 应用入口：注册 Pinia、Vue Router、Element Plus，并挂载根组件
 */
import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'

import App from './App.vue'
import router from './router'

// 样式：Element Plus 基础样式 + 代码高亮主题 + 项目自定义样式
import 'element-plus/dist/index.css'
import 'highlight.js/styles/github.css'
import './assets/styles/global.css'
import './assets/styles/front.css'
import './assets/styles/admin.css'

const app = createApp(App)

// 全局注册 Element Plus 图标组件（后台菜单、按钮会用到）
for (const [name, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(name, component)
}

app.use(createPinia())
app.use(router)
app.use(ElementPlus, { locale: zhCn })
app.mount('#app')
