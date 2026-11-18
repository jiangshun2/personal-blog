<template>
  <div class="admin-page">
    <div class="admin-page-head">
      <div>
        <h2 class="admin-page-title">{{ isEdit ? '编辑文章' : '新建文章' }}</h2>
        <p class="admin-page-desc">支持 Markdown 语法，右侧实时预览渲染效果</p>
      </div>
      <el-button :icon="Back" @click="router.push('/admin/article')">返回列表</el-button>
    </div>

    <div class="admin-card" v-loading="loading">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="88px">
        <el-form-item label="标题" prop="title">
          <el-input
            v-model="form.title"
            maxlength="120"
            show-word-limit
            placeholder="请输入文章标题"
          />
        </el-form-item>

        <el-form-item label="摘要" prop="summary">
          <el-input
            v-model="form.summary"
            type="textarea"
            :rows="2"
            maxlength="300"
            show-word-limit
            placeholder="文章摘要（可选），会展示在文章列表中"
          />
        </el-form-item>

        <el-form-item label="封面图" prop="cover">
          <div class="cover-field">
            <el-input v-model="form.cover" placeholder="封面图地址（可选）" clearable />
            <img
              v-if="form.cover"
              :src="form.cover"
              alt="封面预览"
              class="article-cover-preview"
            />
          </div>
        </el-form-item>

        <el-row :gutter="16">
          <el-col :xs="24" :sm="8">
            <el-form-item label="分类" prop="categoryId">
              <el-select
                v-model="form.categoryId"
                placeholder="请选择分类"
                clearable
                style="width: 100%"
              >
                <el-option
                  v-for="item in categories"
                  :key="item.id"
                  :label="item.name"
                  :value="item.id"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="10">
            <el-form-item label="标签" prop="tagIds">
              <el-select
                v-model="form.tagIds"
                multiple
                collapse-tags
                collapse-tags-tooltip
                placeholder="请选择标签"
                style="width: 100%"
              >
                <el-option
                  v-for="item in tags"
                  :key="item.id"
                  :label="item.name"
                  :value="item.id"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="6">
            <el-form-item label="置顶">
              <el-switch v-model="form.isTop" active-text="置顶" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio :value="1">发布</el-radio>
            <el-radio :value="0">草稿</el-radio>
          </el-radio-group>
        </el-form-item>

        <el-form-item label="正文" prop="content">
          <div class="editor-layout">
            <div class="editor-pane">
              <div class="editor-pane-title">
                <span>Markdown 正文</span>
                <span class="editor-tip">支持标题、列表、代码块、表格等语法</span>
              </div>
              <el-input
                v-model="form.content"
                type="textarea"
                :rows="22"
                class="editor-textarea"
                placeholder="使用 Markdown 语法编写正文…"
              />
            </div>
            <div class="editor-pane">
              <div class="editor-pane-title">
                <span>实时预览</span>
              </div>
              <div class="editor-preview">
                <div v-if="form.content" class="markdown-body" v-html="previewHtml"></div>
                <div v-else class="editor-empty-tip">
                  在左侧输入 Markdown，这里会实时显示渲染效果
                </div>
              </div>
            </div>
          </div>
        </el-form-item>
      </el-form>

      <div class="editor-footer">
        <el-button @click="router.push('/admin/article')">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">
          {{ isEdit ? '保存更新' : '保存文章' }}
        </el-button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Back } from '@element-plus/icons-vue'
import {
  createArticle,
  getAdminArticle,
  getAdminCategoryList,
  getAdminTagList,
  updateArticle
} from '@/api/admin'
import { renderMarkdown } from '@/utils/markdown'

const route = useRoute()
const router = useRouter()

const formRef = ref(null)
const loading = ref(false)
const saving = ref(false)
const categories = ref([])
const tags = ref([])

const isEdit = computed(() => !!route.params.id)

const form = reactive({
  title: '',
  summary: '',
  content: '',
  cover: '',
  categoryId: '',
  tagIds: [],
  status: 1,
  isTop: false
})

const rules = {
  title: [{ required: true, message: '请输入文章标题', trigger: 'blur' }],
  content: [{ required: true, message: '请输入文章正文', trigger: 'blur' }]
}

/** Markdown 实时预览 */
const previewHtml = computed(() => renderMarkdown(form.content))

/** 加载分类、标签下拉数据 */
async function loadOptions() {
  try {
    const [categoryData, tagData] = await Promise.all([
      getAdminCategoryList(),
      getAdminTagList()
    ])
    categories.value = categoryData || []
    tags.value = tagData || []
  } catch (e) {
    // 下拉数据加载失败不阻塞编辑
  }
}

/** 编辑模式：加载文章详情 */
async function loadArticle() {
  if (!isEdit.value) return
  loading.value = true
  try {
    const data = await getAdminArticle(route.params.id)
    if (!data) {
      ElMessage.error('文章不存在')
      router.push('/admin/article')
      return
    }
    form.title = data.title || ''
    form.summary = data.summary || ''
    form.content = data.content || ''
    form.cover = data.cover || ''
    form.categoryId = data.categoryId || ''
    form.tagIds = data.tagIds || []
    form.status = data.status === 0 ? 0 : 1
    form.isTop = !!data.isTop
  } catch (e) {
    router.push('/admin/article')
  } finally {
    loading.value = false
  }
}

/** 保存（新建或更新） */
async function handleSave() {
  if (!formRef.value) return
  formRef.value.validate(async (valid) => {
    if (!valid) return
    saving.value = true
    try {
      const payload = {
        title: form.title.trim(),
        summary: form.summary || '',
        content: form.content,
        cover: form.cover || '',
        categoryId: form.categoryId || null,
        tagIds: form.tagIds || [],
        status: form.status,
        isTop: form.isTop
      }

      if (isEdit.value) {
        await updateArticle(route.params.id, payload)
        ElMessage.success('文章已更新')
      } else {
        await createArticle(payload)
        ElMessage.success('文章已保存')
      }
      router.push('/admin/article')
    } catch (e) {
      // 错误提示已由 axios 拦截器统一处理
    } finally {
      saving.value = false
    }
  })
}

onMounted(() => {
  loadOptions()
  loadArticle()
})
</script>

<style scoped>
.cover-field {
  display: flex;
  align-items: center;
  gap: 14px;
  width: 100%;
}

.cover-field .el-input {
  max-width: 420px;
}

.editor-layout {
  width: 100%;
}

.editor-tip {
  font-weight: 400;
  color: #b0b8c6;
}

.editor-textarea :deep(.el-textarea__inner) {
  font-family: var(--font-mono);
  font-size: 13.5px;
  line-height: 1.7;
}
</style>
