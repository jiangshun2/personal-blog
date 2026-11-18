/**
 * 路由配置：单项目双路由
 * - 前台 /（FrontLayout）：首页、文章详情、分类、标签、归档、关于
 * - 后台 /admin（AdminLayout）：登录 + 管理页面，meta.requiresAuth 标记需要登录
 */
import { createRouter, createWebHistory } from 'vue-router'
import FrontLayout from '@/layouts/FrontLayout.vue'
import AdminLayout from '@/layouts/AdminLayout.vue'
import { TOKEN_KEY } from '@/utils/request'

const routes = [
  /* ==================== 前台（公开） ==================== */
  {
    path: '/',
    component: FrontLayout,
    children: [
      {
        path: '',
        name: 'Home',
        component: () => import('@/views/front/Home.vue'),
        meta: { title: '首页' }
      },
      {
        path: 'article/:id',
        name: 'ArticleDetail',
        component: () => import('@/views/front/ArticleDetail.vue'),
        meta: { title: '文章详情' }
      },
      {
        path: 'category',
        name: 'Category',
        component: () => import('@/views/front/Category.vue'),
        meta: { title: '分类' }
      },
      {
        path: 'tag',
        name: 'Tag',
        component: () => import('@/views/front/Tag.vue'),
        meta: { title: '标签' }
      },
      {
        path: 'archive',
        name: 'Archive',
        component: () => import('@/views/front/Archive.vue'),
        meta: { title: '归档' }
      },
      {
        path: 'about',
        name: 'About',
        component: () => import('@/views/front/About.vue'),
        meta: { title: '关于' }
      }
    ]
  },

  /* ==================== 后台登录（公开） ==================== */
  {
    path: '/admin/login',
    name: 'AdminLogin',
    component: () => import('@/views/admin/Login.vue'),
    meta: { title: '登录' }
  },

  /* ==================== 后台（需登录） ==================== */
  {
    path: '/admin',
    component: AdminLayout,
    redirect: '/admin/dashboard',
    meta: { requiresAuth: true },
    children: [
      {
        path: 'dashboard',
        name: 'AdminDashboard',
        component: () => import('@/views/admin/Dashboard.vue'),
        meta: { title: '仪表盘' }
      },
      {
        path: 'article',
        name: 'AdminArticleList',
        component: () => import('@/views/admin/ArticleList.vue'),
        meta: { title: '文章管理' }
      },
      {
        path: 'article/edit',
        name: 'AdminArticleCreate',
        component: () => import('@/views/admin/ArticleEdit.vue'),
        meta: { title: '新建文章' }
      },
      {
        path: 'article/edit/:id',
        name: 'AdminArticleEdit',
        component: () => import('@/views/admin/ArticleEdit.vue'),
        meta: { title: '编辑文章' }
      },
      {
        path: 'category',
        name: 'AdminCategory',
        component: () => import('@/views/admin/CategoryManage.vue'),
        meta: { title: '分类管理' }
      },
      {
        path: 'tag',
        name: 'AdminTag',
        component: () => import('@/views/admin/TagManage.vue'),
        meta: { title: '标签管理' }
      },
      {
        path: 'comment',
        name: 'AdminComment',
        component: () => import('@/views/admin/CommentManage.vue'),
        meta: { title: '评论管理' }
      },
      {
        path: 'site',
        name: 'AdminSite',
        component: () => import('@/views/admin/SiteConfig.vue'),
        meta: { title: '站点设置' }
      }
    ]
  },

  /* ==================== 兜底 ==================== */
  {
    path: '/:pathMatch(.*)*',
    redirect: '/'
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior(to, from, savedPosition) {
    if (savedPosition) return savedPosition
    return { top: 0 }
  }
})

/**
 * 全局前置守卫：后台路由校验 token，未登录统一跳转登录页
 */
router.beforeEach((to, from, next) => {
  const token = localStorage.getItem(TOKEN_KEY)
  const requiresAuth = to.matched.some((record) => record.meta && record.meta.requiresAuth)

  // 1. 需要登录但没有 token：跳登录页，并记录来源路径
  if (requiresAuth && !token) {
    next({ path: '/admin/login', query: { redirect: to.fullPath } })
    return
  }

  // 2. 已登录还访问登录页：直接进后台
  if (to.path === '/admin/login' && token) {
    next('/admin/dashboard')
    return
  }

  // 3. 设置页面标题
  const title = to.meta && to.meta.title
  document.title = title ? title + ' - 我的个人博客' : '我的个人博客'
  next()
})

export default router
