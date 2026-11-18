<template>
  <div class="container page-about">
    <section class="about-card">
      <div class="about-head">
        <img
          v-if="siteStore.siteAvatar"
          :src="siteStore.siteAvatar"
          :alt="siteStore.siteName"
          class="about-avatar"
        />
        <div v-else class="about-avatar about-avatar-text">{{ brandChar }}</div>

        <div class="about-head-text">
          <h1 class="about-name">{{ siteStore.siteName }}</h1>
          <p class="about-desc">{{ siteStore.siteDescription }}</p>
          <div class="about-links">
            <a
              v-if="siteStore.github"
              :href="siteStore.github"
              target="_blank"
              rel="noopener"
              class="about-link"
            >
              GitHub
            </a>
            <a v-if="siteStore.email" :href="'mailto:' + siteStore.email" class="about-link">
              {{ siteStore.email }}
            </a>
            <router-link to="/archive" class="about-link">文章归档</router-link>
          </div>
        </div>
      </div>

      <div class="about-body">
        <h2 class="about-section-title">关于这个博客</h2>
        <p>
          这是一个前后端分离的个人博客系统，用来记录学习笔记、技术总结与生活随笔。
          前台面向访客，提供文章浏览、分类 / 标签筛选、归档检索与评论交流；
          后台面向博主，支持文章、分类、标签、评论与站点信息的完整管理。
        </p>

        <h2 class="about-section-title">技术栈</h2>
        <ul class="about-tech">
          <li>
            <strong>前端</strong>
            <span>Vue 3 + Vite 5 + Vue Router 4 + Pinia + Axios，前台自定义响应式样式，后台使用 Element Plus</span>
          </li>
          <li>
            <strong>后端</strong>
            <span>Spring Boot 3 + MyBatis-Plus + MySQL 8 + Redis 7 + JWT，提供 RESTful 接口</span>
          </li>
          <li>
            <strong>内容</strong>
            <span>Markdown 撰写，markdown-it 渲染，highlight.js 代码高亮</span>
          </li>
          <li>
            <strong>部署</strong>
            <span>前端 Vite 打包为静态资源，由 Nginx 托管并反向代理 /api 到后端</span>
          </li>
        </ul>

        <h2 class="about-section-title">联系我</h2>
        <div class="about-contact">
          <p v-if="siteStore.email">
            邮箱：<a :href="'mailto:' + siteStore.email" class="text-link">{{ siteStore.email }}</a>
          </p>
          <p v-if="siteStore.github">
            GitHub：
            <a :href="siteStore.github" target="_blank" rel="noopener" class="text-link">
              {{ siteStore.github }}
            </a>
          </p>
          <p v-if="!siteStore.email && !siteStore.github" class="text-muted">
            博主还没有填写联系方式，可以在后台「站点设置」中补充。
          </p>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup>
import { computed, onMounted } from 'vue'
import { useSiteStore } from '@/stores/site'

const siteStore = useSiteStore()

const brandChar = computed(() => (siteStore.siteName || '博').charAt(0))

onMounted(() => {
  siteStore.fetchConfig()
})
</script>
