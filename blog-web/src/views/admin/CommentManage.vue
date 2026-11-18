<template>
  <div class="admin-page">
    <div class="admin-page-head">
      <div>
        <h2 class="admin-page-title">评论管理</h2>
        <p class="admin-page-desc">共 {{ total }} 条评论；状态为「隐藏」的评论不会在前台展示</p>
      </div>
    </div>

    <div class="admin-card">
      <!-- 筛选工具栏 -->
      <div class="admin-toolbar">
        <div class="admin-toolbar-left">
          <el-input
            v-model="query.articleId"
            placeholder="按文章 ID 筛选"
            clearable
            style="width: 160px"
            @keyup.enter="handleSearch"
          />
          <el-select
            v-model="query.status"
            placeholder="全部状态"
            clearable
            style="width: 130px"
            @change="handleSearch"
          >
            <el-option label="显示" :value="1" />
            <el-option label="隐藏" :value="0" />
          </el-select>
          <el-button type="primary" :icon="Search" @click="handleSearch">搜索</el-button>
          <el-button :icon="RefreshLeft" @click="resetQuery">重置</el-button>
        </div>
      </div>

      <el-table :data="comments" v-loading="loading" stripe style="width: 100%">
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column label="文章ID" width="90">
          <template #default="{ row }">
            <el-link type="primary" :underline="false" @click="openArticle(row.articleId)">
              {{ row.articleId }}
            </el-link>
          </template>
        </el-table-column>
        <el-table-column label="昵称" width="130" show-overflow-tooltip>
          <template #default="{ row }">{{ row.nickname || '匿名访客' }}</template>
        </el-table-column>
        <el-table-column
          prop="content"
          label="评论内容"
          min-width="260"
          show-overflow-tooltip
        />
        <el-table-column prop="createTime" label="评论时间" width="170" />
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
              {{ row.status === 1 ? '显示' : '隐藏' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="handleToggleStatus(row)">
              {{ row.status === 1 ? '隐藏' : '显示' }}
            </el-button>
            <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="admin-pagination">
        <el-pagination
          v-model:current-page="query.page"
          v-model:page-size="query.size"
          background
          layout="total, sizes, prev, pager, next"
          :total="total"
          :page-sizes="[10, 20, 50]"
          @current-change="loadComments"
          @size-change="handleSizeChange"
        />
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { RefreshLeft, Search } from '@element-plus/icons-vue'
import {
  deleteComment,
  getAdminCommentPage,
  updateCommentStatus
} from '@/api/admin'

const loading = ref(false)
const comments = ref([])
const total = ref(0)

const query = reactive({
  page: 1,
  size: 10,
  articleId: '',
  status: ''
})

/** 新窗口打开对应文章前台页面 */
function openArticle(articleId) {
  if (!articleId) return
  window.open('/article/' + articleId, '_blank')
}

async function loadComments() {
  loading.value = true
  try {
    const data = await getAdminCommentPage({
      page: query.page,
      size: query.size,
      articleId: query.articleId === '' ? undefined : query.articleId,
      status: query.status === '' || query.status === null ? undefined : query.status
    })
    comments.value = (data && data.records) || []
    total.value = (data && data.total) || 0
  } catch (e) {
    comments.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  query.page = 1
  loadComments()
}

function handleSizeChange() {
  query.page = 1
  loadComments()
}

function resetQuery() {
  query.articleId = ''
  query.status = ''
  query.page = 1
  loadComments()
}

/** 切换显示 / 隐藏 */
async function handleToggleStatus(row) {
  const nextStatus = row.status === 1 ? 0 : 1
  try {
    await updateCommentStatus(row.id, nextStatus)
    row.status = nextStatus
    ElMessage.success(nextStatus === 1 ? '评论已显示' : '评论已隐藏')
  } catch (e) {
    // 错误提示已由 axios 拦截器统一处理
  }
}

/** 删除评论 */
function handleDelete(row) {
  ElMessageBox.confirm('确定删除这条评论吗？删除后不可恢复。', '删除确认', {
    type: 'warning',
    confirmButtonText: '删除',
    cancelButtonText: '取消'
  })
    .then(async () => {
      try {
        await deleteComment(row.id)
        ElMessage.success('删除成功')
        if (comments.value.length === 1 && query.page > 1) {
          query.page -= 1
        }
        loadComments()
      } catch (e) {
        // 错误提示已由 axios 拦截器统一处理
      }
    })
    .catch(() => {})
}

onMounted(loadComments)
</script>
