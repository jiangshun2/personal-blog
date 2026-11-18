<template>
  <div class="container page-category">
    <header class="page-header">
      <h1 class="page-title">文章分类</h1>
      <p class="page-subtitle">按分类浏览文章，共 {{ categories.length }} 个分类</p>
    </header>

    <div class="category-layout">
      <!-- 分类列表 -->
      <aside class="category-sidebar">
        <h2 class="sidebar-title">全部分类</h2>
        <ul class="category-nav">
          <li>
            <button
              type="button"
              class="category-nav-item"
              :class="{ active: !activeId }"
              @click="selectCategory(null)"
            >
              <span class="category-nav-name">全部文章</span>
              <span class="category-nav-count">{{ totalAll }}</span>
            </button>
          </li>
          <li v-for="category in categories" :key="category.id">
            <button
              type="button"
              class="category-nav-item"
              :class="{ active: activeId === category.id }"
              @click="selectCategory(category.id)"
            >
              <span class="category-nav-name">{{ category.name }}</span>
              <span class="category-nav-count">{{ category.articleCount || 0 }}</span>
            </button>
          </li>
        </ul>
        <p v-if="!categories.length" class="sidebar-empty">暂无分类</p>
      </aside>

      <!-- 文章列表 -->
      <section class="category-content">
        <div v-if="currentCategory" class="category-content-head">
          <h2 class="category-name">{{ currentCategory.name }}</h2>
          <p class="category-desc">
            {{ currentCategory.description || '该分类下的全部文章' }}
          </p>
        </div>

        <div v-if="loading" class="state-block">正在加载文章…</div>
        <template v-else>
          <ArticleCard v-for="article in articles" :key="article.id" :article="article" />
          <div v-if="!articles.length" class="state-block">该分类下还没有文章。</div>
          <Pagination
            :current="query.page"
            :page-size="query.size"
            :total="total"
            @change="handlePageChange"
          />
        </template>
      </section>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getArticlePage } from '@/api/article'
import { getCategoryList } from '@/api/category'
import ArticleCard from '@/components/ArticleCard.vue'
import Pagination from '@/components/Pagination.vue'

const route = useRoute()
const router = useRouter()

const categories = ref([])
const activeId = ref(null)
const articles = ref([])
const total = ref(0)
const loading = ref(false)
const query = reactive({ page: 1, size: 8 })

const currentCategory = computed(
  () => categories.value.find((category) => category.id === activeId.value) || null
)

const totalAll = computed(() =>
  categories.value.reduce((sum, category) => sum + (category.articleCount || 0), 0)
)

/** 加载当前分类下的文章 */
async function fetchArticles() {
  loading.value = true
  try {
    const data = await getArticlePage({
      page: query.page,
      size: query.size,
      categoryId: activeId.value || undefined
    })
    articles.value = (data && data.records) || []
    total.value = (data && data.total) || 0
  } catch (e) {
    articles.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

/** 点击分类：更新 URL 查询参数并重新加载 */
function selectCategory(id) {
  if (activeId.value === id) return
  activeId.value = id
  query.page = 1
  router.replace({ path: '/category', query: id ? { id: String(id) } : {} })
  fetchArticles()
}

async function loadCategories() {
  try {
    const data = await getCategoryList()
    categories.value = data || []
    // 支持从文章详情页跳转：/category?name=分类名 或 /category?id=1
    const queryName = route.query.name
    const queryId = route.query.id
    let target = null
    if (queryId) {
      target = categories.value.find((category) => String(category.id) === String(queryId))
    } else if (queryName) {
      target = categories.value.find((category) => category.name === queryName)
    }
    activeId.value = target ? target.id : null
  } catch (e) {
    categories.value = []
  } finally {
    fetchArticles()
  }
}

function handlePageChange(page) {
  query.page = page
  fetchArticles()
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

onMounted(loadCategories)
</script>
