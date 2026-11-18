<template>
  <div class="container page-article">
    <div v-if="loading" class="state-block">正在加载文章…</div>

    <div v-else-if="!article" class="state-block">
      文章不存在或已被删除。
      <router-link to="/" class="text-link">返回首页</router-link>
    </div>

    <template v-else>
      <!-- ==================== 文章正文 ==================== -->
      <article class="article-detail">
        <header class="article-detail-header">
          <h1 class="article-detail-title">{{ article.title }}</h1>
          <div class="article-detail-meta">
            <span class="meta-item">{{ formatDateTime(article.createTime) }}</span>
            <span v-if="article.categoryName" class="meta-item">
              <router-link
                :to="{ path: '/category', query: { name: article.categoryName } }"
                class="meta-link"
              >
                {{ article.categoryName }}
              </router-link>
            </span>
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
        </header>

        <!-- markdown-it 渲染后的正文，代码块由 highlight.js 高亮 -->
        <div class="markdown-body" v-html="renderedContent"></div>

        <footer class="article-detail-footer">
          <div class="article-nav">
            <router-link
              v-if="article.prevId"
              :to="'/article/' + article.prevId"
              class="article-nav-item"
            >
              <span class="nav-label">← 上一篇</span>
              <span class="nav-title">{{ article.prevTitle }}</span>
            </router-link>
            <div v-else class="article-nav-item disabled">
              <span class="nav-label">← 上一篇</span>
              <span class="nav-title">已经是第一篇了</span>
            </div>

            <router-link
              v-if="article.nextId"
              :to="'/article/' + article.nextId"
              class="article-nav-item next"
            >
              <span class="nav-label">下一篇 →</span>
              <span class="nav-title">{{ article.nextTitle }}</span>
            </router-link>
            <div v-else class="article-nav-item next disabled">
              <span class="nav-label">下一篇 →</span>
              <span class="nav-title">已经是最后一篇了</span>
            </div>
          </div>
        </footer>
      </article>

      <!-- ==================== 评论区 ==================== -->
      <section class="comment-section">
        <h2 class="section-title">
          评论
          <span class="section-count">{{ commentTotal }}</span>
        </h2>

        <div v-if="commentLoading" class="state-block small">评论加载中…</div>
        <div v-else class="comment-list">
          <CommentItem
            v-for="comment in comments"
            :key="comment.id"
            :comment="comment"
            @reply="handleReply"
          />
          <div v-if="!comments.length" class="state-block small">
            还没有评论，来抢沙发吧～
          </div>
        </div>

        <Pagination
          v-if="commentTotal > commentQuery.size"
          :current="commentQuery.page"
          :page-size="commentQuery.size"
          :total="commentTotal"
          @change="handleCommentPageChange"
        />

        <!-- 发表评论 / 回复 -->
        <form class="comment-form" @submit.prevent="submitComment">
          <h3 class="comment-form-title">
            {{ replyTarget ? '回复评论' : '发表评论' }}
          </h3>

          <div v-if="replyTarget" class="reply-hint">
            正在回复：{{ replyTarget.nickname || '匿名访客' }}
            <button type="button" class="link-btn" @click="replyTarget = null">取消回复</button>
          </div>

          <div class="comment-form-row">
            <input
              v-model="commentForm.nickname"
              class="comment-input"
              type="text"
              maxlength="30"
              placeholder="昵称（可选，默认匿名访客）"
            />
            <input
              v-model="commentForm.email"
              class="comment-input"
              type="email"
              maxlength="60"
              placeholder="邮箱（可选，不会公开）"
            />
          </div>

          <textarea
            v-model="commentForm.content"
            class="comment-textarea"
            rows="4"
            maxlength="500"
            placeholder="友善交流，说点什么吧…"
          ></textarea>

          <div class="comment-form-footer">
            <span class="comment-form-tip">内容最多 500 字</span>
            <button type="submit" class="primary-btn" :disabled="submitting">
              {{ submitting ? '提交中…' : '提交评论' }}
            </button>
          </div>
        </form>
      </section>
    </template>
  </div>
</template>

<script setup>
import { computed, reactive, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getArticleDetail } from '@/api/article'
import { createComment, getCommentPage } from '@/api/comment'
import { renderMarkdown } from '@/utils/markdown'
import { formatDateTime } from '@/utils/format'
import CommentItem from '@/components/CommentItem.vue'
import Pagination from '@/components/Pagination.vue'

const route = useRoute()

const article = ref(null)
const loading = ref(false)

const comments = ref([])
const commentTotal = ref(0)
const commentLoading = ref(false)
const commentQuery = reactive({ page: 1, size: 10 })

const commentForm = reactive({ nickname: '', email: '', content: '' })
const replyTarget = ref(null)
const submitting = ref(false)

/** Markdown 转 HTML */
const renderedContent = computed(() => renderMarkdown(article.value ? article.value.content : ''))

/** 加载文章详情（同时后端会增加浏览量） */
async function loadArticle() {
  const id = route.params.id
  loading.value = true
  article.value = null
  comments.value = []
  commentTotal.value = 0
  commentQuery.page = 1
  replyTarget.value = null

  try {
    const data = await getArticleDetail(id)
    article.value = data || null
    if (article.value) {
      loadComments()
    }
  } catch (e) {
    article.value = null
  } finally {
    loading.value = false
    window.scrollTo({ top: 0 })
  }
}

/** 加载评论列表 */
async function loadComments() {
  if (!article.value) return
  commentLoading.value = true
  try {
    const data = await getCommentPage({
      articleId: article.value.id,
      page: commentQuery.page,
      size: commentQuery.size
    })
    comments.value = (data && data.records) || []
    commentTotal.value = (data && data.total) || 0
  } catch (e) {
    comments.value = []
    commentTotal.value = 0
  } finally {
    commentLoading.value = false
  }
}

function handleCommentPageChange(page) {
  commentQuery.page = page
  loadComments()
}

/** 点击「回复」：把目标评论挂到表单上 */
function handleReply(comment) {
  replyTarget.value = comment
  const form = document.querySelector('.comment-form')
  if (form && form.scrollIntoView) {
    form.scrollIntoView({ behavior: 'smooth', block: 'center' })
  }
}

/** 提交评论 */
async function submitComment() {
  if (!article.value) return
  const content = commentForm.content.trim()
  if (!content) {
    ElMessage.warning('评论内容不能为空')
    return
  }

  submitting.value = true
  try {
    await createComment({
      articleId: article.value.id,
      content,
      nickname: commentForm.nickname.trim() || null,
      email: commentForm.email.trim() || null,
      parentId: replyTarget.value ? replyTarget.value.id : null
    })
    ElMessage.success(replyTarget.value ? '回复成功' : '评论成功')
    commentForm.content = ''
    replyTarget.value = null
    commentQuery.page = 1
    await loadComments()
  } catch (e) {
    // 错误提示已由 axios 拦截器统一处理
  } finally {
    submitting.value = false
  }
}

watch(() => route.params.id, loadArticle, { immediate: true })
</script>
