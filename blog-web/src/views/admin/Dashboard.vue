<template>
  <div class="admin-page">
    <div class="admin-page-head">
      <div>
        <h2 class="admin-page-title">仪表盘</h2>
        <p class="admin-page-desc">博客整体数据概览</p>
      </div>
      <el-button :icon="Refresh" :loading="loading" @click="loadStats">刷新数据</el-button>
    </div>

    <!-- 统计卡片 -->
    <div class="stat-grid" v-loading="loading">
      <div v-for="card in statCards" :key="card.label" class="stat-card">
        <div class="stat-icon" :class="card.color">
          <el-icon><component :is="card.icon" /></el-icon>
        </div>
        <div class="stat-info">
          <span class="stat-value">{{ formatNumber(card.value) }}</span>
          <span class="stat-label">{{ card.label }}</span>
        </div>
      </div>
    </div>

    <!-- 最近文章 -->
    <div class="admin-card">
      <div class="admin-toolbar">
        <div class="admin-toolbar-left">
          <h3 class="admin-page-title" style="font-size: 16px">最近文章</h3>
        </div>
        <el-button type="primary" :icon="EditPen" @click="router.push('/admin/article/edit')">
          写文章
        </el-button>
      </div>

      <el-table :data="recentArticles" v-loading="loading" stripe style="width: 100%">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="title" label="标题" min-width="220" show-overflow-tooltip />
        <el-table-column prop="viewCount" label="浏览量" width="110" />
        <el-table-column prop="createTime" label="创建时间" width="190" />
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-button
              link
              type="primary"
              @click="router.push('/admin/article/edit/' + row.id)"
            >
              编辑
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div v-if="!loading && !recentArticles.length" class="admin-empty">
        还没有文章，点击右上角「写文章」发布第一篇吧。
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, markRaw, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import {
  ChatDotRound,
  Document,
  EditPen,
  FolderOpened,
  PriceTag,
  Refresh,
  View
} from '@element-plus/icons-vue'
import { getDashboardStats } from '@/api/admin'
import { formatNumber } from '@/utils/format'

const router = useRouter()

const loading = ref(false)
const stats = ref({
  articleCount: 0,
  commentCount: 0,
  categoryCount: 0,
  tagCount: 0,
  totalViewCount: 0
})
const recentArticles = ref([])

const statCards = computed(() => [
  {
    label: '文章总数',
    value: stats.value.articleCount,
    icon: markRaw(Document),
    color: 'is-blue'
  },
  {
    label: '评论总数',
    value: stats.value.commentCount,
    icon: markRaw(ChatDotRound),
    color: 'is-green'
  },
  {
    label: '分类数',
    value: stats.value.categoryCount,
    icon: markRaw(FolderOpened),
    color: 'is-orange'
  },
  {
    label: '标签数',
    value: stats.value.tagCount,
    icon: markRaw(PriceTag),
    color: 'is-purple'
  },
  {
    label: '总浏览量',
    value: stats.value.totalViewCount,
    icon: markRaw(View),
    color: 'is-red'
  }
])

async function loadStats() {
  loading.value = true
  try {
    const data = await getDashboardStats()
    if (data) {
      stats.value = {
        articleCount: data.articleCount || 0,
        commentCount: data.commentCount || 0,
        categoryCount: data.categoryCount || 0,
        tagCount: data.tagCount || 0,
        totalViewCount: data.totalViewCount || 0
      }
      recentArticles.value = data.recentArticles || []
    }
  } catch (e) {
    // 错误提示已由 axios 拦截器统一处理
  } finally {
    loading.value = false
  }
}

onMounted(loadStats)
</script>
