<template>
  <div class="front-layout">
    <!-- ==================== 顶部导航 ==================== -->
    <header class="site-header" :class="{ 'is-scrolled': scrolled }">
      <div class="container header-inner">
        <router-link to="/" class="site-brand">
          <img
            v-if="siteStore.siteAvatar"
            :src="siteStore.siteAvatar"
            :alt="siteStore.siteName"
            class="brand-avatar"
          />
          <span v-else class="brand-logo">{{ brandChar }}</span>
          <span class="brand-text">{{ siteStore.siteName }}</span>
        </router-link>

        <nav class="site-nav" :class="{ 'is-open': menuOpen }">
          <router-link
            v-for="item in navItems"
            :key="item.path"
            :to="item.path"
            class="nav-link"
            :class="{ active: isActive(item.path) }"
            @click="menuOpen = false"
          >
            {{ item.label }}
          </router-link>
        </nav>

        <button
          type="button"
          class="menu-toggle"
          :class="{ 'is-open': menuOpen }"
          aria-label="展开或收起导航菜单"
          @click="menuOpen = !menuOpen"
        >
          <span></span>
          <span></span>
          <span></span>
        </button>
      </div>
    </header>

    <!-- ==================== 页面内容 ==================== -->
    <main class="site-main">
      <router-view v-slot="{ Component }">
        <transition name="fade" mode="out-in">
          <component :is="Component" />
        </transition>
      </router-view>
    </main>

    <!-- ==================== 页脚 ==================== -->
    <footer class="site-footer">
      <div class="container footer-inner">
        <p class="footer-copy">
          © {{ currentYear }} {{ siteStore.siteName }} · 用文字记录学习与生活
        </p>
        <div class="footer-links">
          <a v-if="siteStore.github" :href="siteStore.github" target="_blank" rel="noopener">GitHub</a>
          <a v-if="siteStore.email" :href="'mailto:' + siteStore.email">邮箱</a>
          <router-link to="/admin/login">管理后台</router-link>
        </div>
      </div>
    </footer>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { useSiteStore } from '@/stores/site'

const siteStore = useSiteStore()
const route = useRoute()

/** 移动端菜单是否展开 */
const menuOpen = ref(false)
/** 页面是否已滚动（用于给顶栏加阴影） */
const scrolled = ref(false)

const navItems = [
  { path: '/', label: '首页' },
  { path: '/category', label: '分类' },
  { path: '/tag', label: '标签' },
  { path: '/archive', label: '归档' },
  { path: '/about', label: '关于' }
]

const currentYear = new Date().getFullYear()

/** 没有头像时用站点名称首字符作为 Logo */
const brandChar = computed(() => (siteStore.siteName || '博').charAt(0))

function isActive(path) {
  if (path === '/') return route.path === '/'
  return route.path.indexOf(path) === 0
}

function handleScroll() {
  scrolled.value = window.scrollY > 8
}

onMounted(() => {
  siteStore.fetchConfig()
  handleScroll()
  window.addEventListener('scroll', handleScroll, { passive: true })
})

onBeforeUnmount(() => {
  window.removeEventListener('scroll', handleScroll)
})
</script>
