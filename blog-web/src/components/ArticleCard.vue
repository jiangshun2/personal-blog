<template>
  <article class="article-card" :class="{ 'with-cover': article.cover }">
    <div class="article-card-body">
      <h2 class="article-card-title">
        <router-link :to="'/article/' + article.id">{{ article.title }}</router-link>
        <span v-if="article.isTop" class="badge-top">置顶</span>
      </h2>
      <p class="article-card-summary">{{ article.summary || '这篇文章还没有写摘要，点击阅读全文。' }}</p>
      <div class="article-card-meta">
        <span class="meta-item">{{ formatDate(article.createTime) }}</span>
        <span v-if="article.categoryName" class="meta-item">{{ article.categoryName }}</span>
        <span class="meta-item">{{ article.viewCount || 0 }} 阅读</span>
      </div>
      <div v-if="article.tags && article.tags.length" class="article-card-tags">
        <router-link
          v-for="tag in article.tags"
          :key="tag"
          :to="{ path: '/tag', query: { name: tag } }"
          class="tag-chip"
        >
          {{ tag }}
        </router-link>
      </div>
    </div>
    <router-link v-if="article.cover" :to="'/article/' + article.id" class="article-card-cover">
      <img :src="article.cover" :alt="article.title" loading="lazy" />
    </router-link>
  </article>
</template>

<script setup>
import { formatDate } from '@/utils/format'

defineProps({
  /** ArticleListVO：id, title, summary, cover, categoryName, tags, viewCount, isTop, createTime */
  article: {
    type: Object,
    required: true
  }
})
</script>
