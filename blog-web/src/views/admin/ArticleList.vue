<template>
  <div class="admin-page">
    <div class="admin-page-head">
      <div>
        <h2 class="admin-page-title">文章管理</h2>
        <p class="admin-page-desc">共 {{ total }} 篇文章，支持关键词搜索、状态筛选与置顶切换</p>
      </div>
      <el-button type="primary" :icon="Plus" @click="router.push('/admin/article/edit')">
        新建文章
      </el-button>
    </div>

    <div class="admin-card">
      <!-- 筛选工具栏 -->
      <div class="admin-toolbar">
        <div class="admin-toolbar-left">
          <el-input
            v-model="query.keyword"
            placeholder="搜索文章标题"
            clearable
            style="width: 220px"
            @keyup.enter="handleSearch"
          />
          <el-select
            v-model="query.status"
            placeholder="全部状态"
            clearable
            style="width: 130px"
            @change="handleSearch"
          >
            <el-option label="已发布" :value="1" />
            <el-option label="草稿" :value="0" />
          </el-select>
          <el-button type="primary" :icon="Search" @click="handleSearch">搜索</el-button>
          <el-button :icon="RefreshLeft" @click="resetQuery">重置</el-button>
        </div>
      </div>

      <!-- 文章表格 -->
      <el-table :data="articles" v-loading="loading" stripe style="width: 100%">
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column label="标题" min-width="220" show-overflow-tooltip>
          <template #default="{ row }">
            <div class="article-title-cell">
              <el-tag v-if="row.isTop" type="warning" size="small" effect="plain">置顶</el-tag>
              <span>{{ row.title }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="categoryName" label="分类" width="110">
          <template #default="{ row }">
            <span>{{ row.categoryName || '未分类' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="标签" min-width="150">
          <template #default="{ row }">
            <template v-if="row.tags && row.tags.length">
              <el-tag
                v-for="tag in row.tags"
                :key="tag"
                size="small"
                type="info"
                effect="plain"
                class="tag-gap"
              >
                {{ tag }}
              </el-tag>
            </template>
            <span v-else class="admin-muted">—</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
              {{ row.status === 1 ? '已发布' : '草稿' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="viewCount" label="浏览量" width="90" />
        <el-table-column label="置顶" width="80">
          <template #default="{ row }">
            <el-switch
              :model-value="row.isTop"
              @change="(value) => handleTopChange(row, value)"
            />
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="170" />
        <el-table-column label="操作" width="130" fixed="right">
          <template #default="{ row }">
            <el-button
              link
              type="primary"
              @click="router.push('/admin/article/edit/' + row.id)"
            >
              编辑
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
          @current-change="loadArticles"
          @size-change="handleSizeChange"
        />
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, RefreshLeft, Search } from '@element-plus/icons-vue'
import { deleteArticle, getAdminArticlePage, updateArticleTop } from '@/api/admin'

const router = useRouter()

const loading = ref(false)
const articles = ref([])
const total = ref(0)

const query = reactive({
  page: 1,
  size: 10,
  keyword: '',
  status: ''
})

/** 加载文章分页数据 */
async function loadArticles() {
  loading.value = true
  try {
    const data = await getAdminArticlePage({
      page: query.page,
      size: query.size,
      keyword: query.keyword || undefined,
      status: query.status === '' || query.status === null ? undefined : query.status
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

function handleSearch() {
  query.page = 1
  loadArticles()
}

function handleSizeChange() {
  query.page = 1
  loadArticles()
}

function resetQuery() {
  query.keyword = ''
  query.status = ''
  query.page = 1
  loadArticles()
}

/** 切换置顶状态 */
async function handleTopChange(row, value) {
  try {
    await updateArticleTop(row.id, value)
    row.isTop = value
    ElMessage.success(value ? '已置顶' : '已取消置顶')
  } catch (e) {
    // 失败时开关保持原状态（model-value 受控）
  }
}

/** 删除文章 */
function handleDelete(row) {
  ElMessageBox.confirm('确定删除文章「' + row.title + '」吗？删除后不可恢复。', '删除确认', {
    type: 'warning',
    confirmButtonText: '删除',
    cancelButtonText: '取消'
  })
    .then(async () => {
      try {
        await deleteArticle(row.id)
        ElMessage.success('删除成功')
        // 当前页删空时回退一页
        if (articles.value.length === 1 && query.page > 1) {
          query.page -= 1
        }
        loadArticles()
      } catch (e) {
        // 错误提示已由 axios 拦截器统一处理
      }
    })
    .catch(() => {})
}

onMounted(loadArticles)
</script>

<style scoped>
.article-title-cell {
  display: flex;
  align-items: center;
  gap: 6px;
  min-width: 0;
}

.tag-gap {
  margin-right: 4px;
}

.admin-muted {
  color: #8b95a5;
}
</style>
