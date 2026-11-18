<template>
  <div class="container page-home">
    <!-- ==================== 欢迎区 ==================== -->
    <section class="home-hero">
      <div class="home-hero-content">
        <p class="home-hero-eyebrow">WELCOME TO MY BLOG</p>
        <h1 class="home-hero-title">{{ siteStore.siteName }}</h1>
        <p class="home-hero-desc">{{ siteStore.siteDescription }}</p>
        <div class="home-hero-stats">
          <span class="hero-stat"><strong>{{ total }}</strong> 篇文章</span>
          <span class="hero-stat"><strong>{{ categories.length }}</strong> 个分类</span>
          <span class="hero-stat"><strong>{{ tags.length }}</strong> 个标签</span>
        </div>
      </div>
      <div class="home-hero-deco" aria-hidden="true">
        <span class="deco-circle c1"></span>
        <span class="deco-circle c2"></span>
      </div>
    </section>

    <!-- ==================== 筛选栏 ==================== -->
    <section class="filter-bar">
      <div class="filter-row">
        <div class="filter-item">
          <label class="filter-label" for="filter-category">分类</label>
          <select id="filter-category" v-model="query.categoryId" class="filter-select" @change="handleFilterChange">
            <option value="">全部分类</option>
            <option v-for="category in categories" :key="category.id" :value="category.id">
              {{ category.name }}（{{ category.articleCount }}）
            </option>
          </select>
        </div>
        <div class="filter-item">
          <label class="filter-label" for="filter-tag">标签</label>
          <select id="filter-tag" v-model="query.tagId" class="filter-select" @change="handleFilterChange">
            <option value="">全部标签</option>
            <option v-for="tag in tags" :key="tag.id" :value="tag.id">
              {{ tag.name }}（{{ tag.articleCount }}）
            </option>
          </select>
        </div>
        <form class="search-form" @submit.prevent="handleSearch">
          <input
            v-model="keywordInput"
            class="search-input"
            type="search"
            placeholder="搜索标题 / 摘要关键词"
            aria-label="搜索文章"
          />
          <button type="submit" class="search-btn">搜索</button>
        </form>
      </div>

      <div v-if="hasFilter" class="filter-active">
        <span class="filter-active-label">当前筛选：</span>
        <span v-if="query.categoryId" class="filter-chip">
          {{ activeCategoryName }}
          <button type="button" class="chip-close" aria-label="移除分类筛选" @click="clearFilter('category')">×</button>
        </span>
        <span v-if="query.tagId" class="filter-chip">
          {{ activeTagName }}
          <button type="button" class="chip-close" aria-label="移除标签筛选" @click="clearFilter('tag')">×</button>
        </span>
        <span v-if="query.keyword" class="filter-chip">
          关键词：{{ query.keyword }}
          <button type="button" class="chip-close" aria-label="移除关键词筛选" @click="clearFilter('keyword')">×</button>
        </span>
        <button type="button" class="link-btn" @click="resetFilter">重置筛选</button>
      </div>
    </section>

    <!-- ==================== 文章列表 ==================== -->
    <section class="article-list">
      <div v-if="loading" class="state-block">正在加载文章…</div>
      <template v-else>
        <ArticleCard v-for="article in articles" :key="article.id" :article="article" />
        <div v-if="!articles.length" class="state-block">
          没有找到符合条件的文章，换个筛选条件试试吧。
        </div>
        <Pagination
          :current="query.page"
          :page-size="query.size"
          :total="total"
          @change="handlePageChange"
        />
      </template>
    </section>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { getArticlePage } from '@/api/article'
import { getCategoryList } from '@/api/category'
import { getTagList } from '@/api/tag'
import { useSiteStore } from '@/stores/site'
import ArticleCard from '@/components/ArticleCard.vue'
import Pagination from '@/components/Pagination.vue'

const siteStore = useSiteStore()

const loading = ref(false)
const articles = ref([])
const total = ref(0)
const categories = ref([])
const tags = ref([])
const keywordInput = ref('')

/** 查询条件：page / size / categoryId / tagId / keyword */
const query = reactive({
  page: 1,
  size: 8,
  categoryId: '',
  tagId: '',
  keyword: ''
})

const hasFilter = computed(() => !!(query.categoryId || query.tagId || query.keyword))

const activeCategoryName = computed(() => {
  const item = categories.value.find((category) => category.id === query.categoryId)
  return item ? '分类：' + item.name : ''
})

const activeTagName = computed(() => {
  const item = tags.value.find((tag) => tag.id === query.tagId)
  return item ? '标签：' + item.name : ''
})

/** 加载文章分页数据 */
async function fetchArticles() {
  loading.value = true
  try {
    const data = await getArticlePage({
      page: query.page,
      size: query.size,
      categoryId: query.categoryId || undefined,
      tagId: query.tagId || undefined,
      keyword: query.keyword || undefined
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

/** 加载分类、标签（用于筛选下拉） */
async function loadFilters() {
  try {
    const [categoryData, tagData] = await Promise.all([getCategoryList(), getTagList()])
    categories.value = categoryData || []
    tags.value = tagData || []
  } catch (e) {
    // 筛选数据加载失败不影响文章列表展示
  }
}

function handleFilterChange() {
  query.page = 1
  fetchArticles()
}

function handleSearch() {
  query.keyword = keywordInput.value.trim()
  query.page = 1
  fetchArticles()
}

function clearFilter(type) {
  if (type === 'category') query.categoryId = ''
  if (type === 'tag') query.tagId = ''
  if (type === 'keyword') {
    query.keyword = ''
    keywordInput.value = ''
  }
  query.page = 1
  fetchArticles()
}

function resetFilter() {
  query.categoryId = ''
  query.tagId = ''
  query.keyword = ''
  keywordInput.value = ''
  query.page = 1
  fetchArticles()
}

function handlePageChange(page) {
  query.page = page
  fetchArticles()
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

onMounted(() => {
  siteStore.fetchConfig()
  loadFilters()
  fetchArticles()
})
</script>
