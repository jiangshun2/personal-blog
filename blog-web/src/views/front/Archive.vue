<template>
  <div class="container page-archive">
    <header class="page-header">
      <h1 class="page-title">文章归档</h1>
      <p class="page-subtitle">共 {{ articleTotal }} 篇文章，按年份倒序排列</p>
    </header>

    <div v-if="loading" class="state-block">正在加载归档…</div>
    <div v-else-if="!years.length" class="state-block">还没有发布文章。</div>
    <div v-else class="archive-list">
      <section v-for="group in years" :key="group.year" class="archive-year">
        <h2 class="archive-year-title">
          <span class="archive-year-label">{{ group.year }}</span>
          <span class="archive-year-count">{{ (group.articles || []).length }} 篇</span>
        </h2>
        <ul class="archive-items">
          <li v-for="item in group.articles" :key="item.id" class="archive-item">
            <span class="archive-date">{{ formatDate(item.createTime).slice(5) }}</span>
            <router-link :to="'/article/' + item.id" class="archive-link">
              {{ item.title }}
            </router-link>
          </li>
        </ul>
      </section>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { getArchive } from '@/api/article'
import { formatDate } from '@/utils/format'

const years = ref([])
const loading = ref(false)

const articleTotal = computed(() =>
  years.value.reduce((sum, group) => sum + ((group.articles || []).length), 0)
)

async function loadArchive() {
  loading.value = true
  try {
    const data = await getArchive()
    years.value = data || []
  } catch (e) {
    years.value = []
  } finally {
    loading.value = false
  }
}

onMounted(loadArchive)
</script>
