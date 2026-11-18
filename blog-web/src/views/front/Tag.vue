<template>
  <div class="container page-tag">
    <header class="page-header">
      <h1 class="page-title">标签云</h1>
      <p class="page-subtitle">共 {{ tags.length }} 个标签，点击标签查看文章</p>
    </header>

    <!-- 标签云 -->
    <section class="tag-cloud-panel">
      <div class="tag-cloud">
        <button
          type="button"
          class="cloud-tag"
          :class="{ active: !activeId }"
          @click="selectTag(null)"
        >
          全部
        </button>
        <button
          v-for="tag in tags"
          :key="tag.id"
          type="button"
          class="cloud-tag"
          :class="{ active: activeId === tag.id }"
          :style="{ fontSize: tagFontSize(tag.articleCount) + 'px' }"
          @click="selectTag(tag.id)"
        >
          {{ tag.name }}
          <span class="cloud-tag-count">{{ tag.articleCount || 0 }}</span>
        </button>
      </div>
      <p v-if="!tags.length" class="sidebar-empty">暂无标签</p>
    </section>

    <!-- 选中标签下的文章 -->
    <section class="tag-content">
      <div v-if="currentTag" class="category-content-head">
        <h2 class="category-name"># {{ currentTag.name }}</h2>
        <p class="category-desc">该标签下共有 {{ currentTag.articleCount || total }} 篇文章</p>
      </div>

      <div v-if="loading" class="state-block">正在加载文章…</div>
      <template v-else>
        <ArticleCard v-for="article in articles" :key="article.id" :article="article" />
        <div v-if="!articles.length" class="state-block">该标签下还没有文章。</div>
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
import { useRoute, useRouter } from 'vue-router'
import { getArticlePage } from '@/api/article'
import { getTagList } from '@/api/tag'
import ArticleCard from '@/components/ArticleCard.vue'
import Pagination from '@/components/Pagination.vue'

const route = useRoute()
const router = useRouter()

const tags = ref([])
const activeId = ref(null)
const articles = ref([])
const total = ref(0)
const loading = ref(false)
const query = reactive({ page: 1, size: 8 })

const currentTag = computed(() => tags.value.find((tag) => tag.id === activeId.value) || null)

/** 标签字号：文章越多字越大（14px ~ 28px） */
function tagFontSize(articleCount) {
  const counts = tags.value.map((tag) => tag.articleCount || 0)
  const max = Math.max.apply(null, counts.concat([1]))
  const min = Math.min.apply(null, counts.concat([0]))
  const count = articleCount || 0
  if (max === min) return 16
  return 14 + Math.round(((count - min) / (max - min)) * 14)
}

async function fetchArticles() {
  loading.value = true
  try {
    const data = await getArticlePage({
      page: query.page,
      size: query.size,
      tagId: activeId.value || undefined
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

/** 点击标签：更新 URL 查询参数并重新加载 */
function selectTag(id) {
  if (activeId.value === id) return
  activeId.value = id
  query.page = 1
  router.replace({ path: '/tag', query: id ? { id: String(id) } : {} })
  fetchArticles()
}

async function loadTags() {
  try {
    const data = await getTagList()
    tags.value = data || []
    // 支持从文章详情页跳转：/tag?name=标签名 或 /tag?id=1
    const queryName = route.query.name
    const queryId = route.query.id
    let target = null
    if (queryId) {
      target = tags.value.find((tag) => String(tag.id) === String(queryId))
    } else if (queryName) {
      target = tags.value.find((tag) => tag.name === queryName)
    }
    activeId.value = target ? target.id : null
  } catch (e) {
    tags.value = []
  } finally {
    fetchArticles()
  }
}

function handlePageChange(page) {
  query.page = page
  fetchArticles()
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

onMounted(loadTags)
</script>
