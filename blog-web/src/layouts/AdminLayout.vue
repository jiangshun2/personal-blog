<template>
  <el-container class="admin-layout">
    <!-- 移动端：抽屉打开时的遮罩 -->
    <div v-if="isMobile && mobileOpen" class="admin-mask" @click="mobileOpen = false"></div>

    <!-- ==================== 侧边菜单（桌面固定 / 移动端抽屉） ==================== -->
    <el-aside
      class="admin-aside"
      :class="{ 'is-mobile': isMobile, 'is-open': mobileOpen }"
      :width="isMobile ? '230px' : (collapsed ? '64px' : '220px')"
    >
      <div class="admin-logo">
        <span class="admin-logo-mark">B</span>
        <span v-if="!collapsed || isMobile" class="admin-logo-text">博客管理后台</span>
      </div>

      <el-menu
        :default-active="activeMenu"
        :collapse="collapsed && !isMobile"
        :collapse-transition="false"
        router
        class="admin-menu"
        @select="handleMenuSelect"
      >
        <el-menu-item index="/admin/dashboard">
          <el-icon><DataLine /></el-icon>
          <template #title>仪表盘</template>
        </el-menu-item>
        <el-menu-item index="/admin/article">
          <el-icon><Document /></el-icon>
          <template #title>文章管理</template>
        </el-menu-item>
        <el-menu-item index="/admin/category">
          <el-icon><FolderOpened /></el-icon>
          <template #title>分类管理</template>
        </el-menu-item>
        <el-menu-item index="/admin/tag">
          <el-icon><PriceTag /></el-icon>
          <template #title>标签管理</template>
        </el-menu-item>
        <el-menu-item index="/admin/comment">
          <el-icon><ChatDotRound /></el-icon>
          <template #title>评论管理</template>
        </el-menu-item>
        <el-menu-item index="/admin/site">
          <el-icon><Setting /></el-icon>
          <template #title>站点设置</template>
        </el-menu-item>
      </el-menu>

      <div v-if="!collapsed || isMobile" class="admin-aside-footer">
        <router-link to="/" class="admin-aside-link">← 返回前台</router-link>
      </div>
    </el-aside>

    <!-- ==================== 主区域 ==================== -->
    <el-container class="admin-body">
      <el-header class="admin-header" height="60px">
        <div class="admin-header-left">
          <!-- 移动端：打开抽屉菜单 -->
          <button type="button" class="mobile-menu-btn" aria-label="打开菜单" @click="mobileOpen = true">
            <el-icon :size="20"><Expand /></el-icon>
          </button>

          <!-- 桌面端：折叠/展开侧边栏 -->
          <button
            type="button"
            class="collapse-btn"
            :title="collapsed ? '展开菜单' : '收起菜单'"
            @click="collapsed = !collapsed"
          >
            <el-icon :size="18">
              <Expand v-if="collapsed" />
              <Fold v-else />
            </el-icon>
          </button>

          <el-breadcrumb class="admin-breadcrumb" separator="/">
            <el-breadcrumb-item :to="{ path: '/admin/dashboard' }">后台</el-breadcrumb-item>
            <el-breadcrumb-item>{{ pageTitle }}</el-breadcrumb-item>
          </el-breadcrumb>
        </div>

        <div class="admin-header-right">
          <el-dropdown trigger="click" @command="handleCommand">
            <span class="admin-user">
              <el-avatar :size="28" :src="userStore.avatar">
                {{ userStore.displayName.charAt(0) }}
              </el-avatar>
              <span class="admin-user-name">{{ userStore.displayName }}</span>
              <el-icon :size="12"><ArrowDown /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="front">访问前台</el-dropdown-item>
                <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>

      <el-main class="admin-main">
        <router-view v-slot="{ Component }">
          <transition name="fade" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { useSiteStore } from '@/stores/site'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const siteStore = useSiteStore()

/** 桌面端：侧边栏是否收起 */
const collapsed = ref(false)
/** 是否移动端（宽度 <= 768px） */
const isMobile = ref(false)
/** 移动端：抽屉菜单是否打开 */
const mobileOpen = ref(false)

/** 媒体查询对象，用于监听窗口宽度变化 */
let mediaQuery = null

/** 根据窗口宽度同步 isMobile；回到桌面端时关闭抽屉 */
function syncViewport() {
  isMobile.value = window.matchMedia('(max-width: 768px)').matches
  if (!isMobile.value) {
    mobileOpen.value = false
  }
}

/** 当前高亮菜单：文章新建 / 编辑都高亮「文章管理」 */
const activeMenu = computed(() => {
  if (route.path.indexOf('/admin/article') === 0) return '/admin/article'
  return route.path
})

const pageTitle = computed(() => (route.meta && route.meta.title) || '管理后台')

/** 移动端点击菜单项后自动关闭抽屉 */
function handleMenuSelect() {
  if (isMobile.value) {
    mobileOpen.value = false
  }
}

/** 路由变化时关闭移动端抽屉 */
watch(
  () => route.path,
  () => {
    if (isMobile.value) {
      mobileOpen.value = false
    }
  }
)

function handleCommand(command) {
  if (command === 'front') {
    window.open('/', '_blank')
    return
  }

  if (command === 'logout') {
    ElMessageBox.confirm('确定要退出登录吗？', '提示', {
      type: 'warning',
      confirmButtonText: '退出登录',
      cancelButtonText: '取消'
    })
      .then(() => {
        userStore.logout()
        ElMessage.success('已退出登录')
        router.push('/admin/login')
      })
      .catch(() => {
        // 用户取消，无需处理
      })
  }
}

onMounted(() => {
  // 刷新页面后重新拉取用户信息；站点配置供后台顶栏 / 设置页使用
  if (!userStore.userInfo) {
    userStore.fetchUserInfo().catch(() => {})
  }
  siteStore.fetchConfig()

  // 监听视口变化，实现响应式切换
  mediaQuery = window.matchMedia('(max-width: 768px)')
  syncViewport()
  if (mediaQuery.addEventListener) {
    mediaQuery.addEventListener('change', syncViewport)
  } else {
    // 兼容旧版浏览器
    mediaQuery.addListener(syncViewport)
  }
})

onBeforeUnmount(() => {
  if (mediaQuery) {
    if (mediaQuery.removeEventListener) {
      mediaQuery.removeEventListener('change', syncViewport)
    } else {
      mediaQuery.removeListener(syncViewport)
    }
  }
})
</script>
