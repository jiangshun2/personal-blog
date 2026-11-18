<template>
  <div class="admin-page">
    <div class="admin-page-head">
      <div>
        <h2 class="admin-page-title">分类管理</h2>
        <p class="admin-page-desc">共 {{ categories.length }} 个分类；分类下仍有文章时不允许删除</p>
      </div>
      <el-button type="primary" :icon="Plus" @click="openCreate">新增分类</el-button>
    </div>

    <div class="admin-card">
      <el-table :data="categories" v-loading="loading" stripe style="width: 100%">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="name" label="分类名称" min-width="150" />
        <el-table-column
          prop="description"
          label="描述"
          min-width="220"
          show-overflow-tooltip
        />
        <el-table-column prop="sort" label="排序" width="90" />
        <el-table-column prop="articleCount" label="文章数" width="100" />
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- 新增 / 编辑弹窗 -->
    <el-dialog
      v-model="dialogVisible"
      :title="editingId ? '编辑分类' : '新增分类'"
      width="460px"
      @closed="resetForm"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="名称" prop="name">
          <el-input
            v-model="form.name"
            maxlength="20"
            show-word-limit
            placeholder="请输入分类名称"
          />
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input
            v-model="form.description"
            type="textarea"
            :rows="3"
            maxlength="100"
            show-word-limit
            placeholder="分类描述（可选）"
          />
        </el-form-item>
        <el-form-item label="排序" prop="sort">
          <el-input-number v-model="form.sort" :min="0" :max="999" controls-position="right" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSubmit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import {
  createCategory,
  deleteCategory,
  getAdminCategoryList,
  updateCategory
} from '@/api/admin'

const loading = ref(false)
const saving = ref(false)
const categories = ref([])

const dialogVisible = ref(false)
const editingId = ref(null)
const formRef = ref(null)

const form = reactive({
  name: '',
  description: '',
  sort: 0
})

const rules = {
  name: [{ required: true, message: '请输入分类名称', trigger: 'blur' }]
}

/** 加载分类列表 */
async function loadCategories() {
  loading.value = true
  try {
    const data = await getAdminCategoryList()
    categories.value = data || []
  } catch (e) {
    categories.value = []
  } finally {
    loading.value = false
  }
}

function resetForm() {
  editingId.value = null
  form.name = ''
  form.description = ''
  form.sort = 0
  if (formRef.value) formRef.value.clearValidate()
}

function openCreate() {
  resetForm()
  dialogVisible.value = true
}

function openEdit(row) {
  editingId.value = row.id
  form.name = row.name || ''
  form.description = row.description || ''
  form.sort = row.sort || 0
  dialogVisible.value = true
}

async function handleSubmit() {
  if (!formRef.value) return
  formRef.value.validate(async (valid) => {
    if (!valid) return
    saving.value = true
    try {
      const payload = {
        name: form.name.trim(),
        description: form.description || '',
        sort: form.sort || 0
      }
      if (editingId.value) {
        await updateCategory(editingId.value, payload)
        ElMessage.success('分类已更新')
      } else {
        await createCategory(payload)
        ElMessage.success('分类已创建')
      }
      dialogVisible.value = false
      loadCategories()
    } catch (e) {
      // 错误提示已由 axios 拦截器统一处理
    } finally {
      saving.value = false
    }
  })
}

/** 删除分类（分类下有文章时后端会拒绝） */
function handleDelete(row) {
  const tip =
    row.articleCount > 0
      ? '分类「' + row.name + '」下还有 ' + row.articleCount + ' 篇文章，请先移除这些文章后再删除。'
      : '确定删除分类「' + row.name + '」吗？'

  if (row.articleCount > 0) {
    ElMessage.warning(tip)
    return
  }

  ElMessageBox.confirm(tip, '删除确认', {
    type: 'warning',
    confirmButtonText: '删除',
    cancelButtonText: '取消'
  })
    .then(async () => {
      try {
        await deleteCategory(row.id)
        ElMessage.success('删除成功')
        loadCategories()
      } catch (e) {
        // 错误提示已由 axios 拦截器统一处理
      }
    })
    .catch(() => {})
}

onMounted(loadCategories)
</script>
