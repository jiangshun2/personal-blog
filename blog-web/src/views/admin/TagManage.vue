<template>
  <div class="admin-page">
    <div class="admin-page-head">
      <div>
        <h2 class="admin-page-title">标签管理</h2>
        <p class="admin-page-desc">共 {{ tags.length }} 个标签；删除标签会同时清理文章与标签的关联</p>
      </div>
      <el-button type="primary" :icon="Plus" @click="openCreate">新增标签</el-button>
    </div>

    <div class="admin-card">
      <el-table :data="tags" v-loading="loading" stripe style="width: 100%">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="name" label="标签名称" min-width="180" />
        <el-table-column prop="articleCount" label="文章数" width="120" />
        <el-table-column label="操作" width="180" fixed="right">
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
      :title="editingId ? '编辑标签' : '新增标签'"
      width="420px"
      @closed="resetForm"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="名称" prop="name">
          <el-input
            v-model="form.name"
            maxlength="20"
            show-word-limit
            placeholder="请输入标签名称"
          />
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
  createTag,
  deleteTag,
  getAdminTagList,
  updateTag
} from '@/api/admin'

const loading = ref(false)
const saving = ref(false)
const tags = ref([])

const dialogVisible = ref(false)
const editingId = ref(null)
const formRef = ref(null)

const form = reactive({ name: '' })

const rules = {
  name: [{ required: true, message: '请输入标签名称', trigger: 'blur' }]
}

async function loadTags() {
  loading.value = true
  try {
    const data = await getAdminTagList()
    tags.value = data || []
  } catch (e) {
    tags.value = []
  } finally {
    loading.value = false
  }
}

function resetForm() {
  editingId.value = null
  form.name = ''
  if (formRef.value) formRef.value.clearValidate()
}

function openCreate() {
  resetForm()
  dialogVisible.value = true
}

function openEdit(row) {
  editingId.value = row.id
  form.name = row.name || ''
  dialogVisible.value = true
}

async function handleSubmit() {
  if (!formRef.value) return
  formRef.value.validate(async (valid) => {
    if (!valid) return
    saving.value = true
    try {
      const payload = { name: form.name.trim() }
      if (editingId.value) {
        await updateTag(editingId.value, payload)
        ElMessage.success('标签已更新')
      } else {
        await createTag(payload)
        ElMessage.success('标签已创建')
      }
      dialogVisible.value = false
      loadTags()
    } catch (e) {
      // 错误提示已由 axios 拦截器统一处理
    } finally {
      saving.value = false
    }
  })
}

function handleDelete(row) {
  ElMessageBox.confirm(
    '确定删除标签「' + row.name + '」吗？删除后会同时清理文章与标签的关联。',
    '删除确认',
    {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消'
    }
  )
    .then(async () => {
      try {
        await deleteTag(row.id)
        ElMessage.success('删除成功')
        loadTags()
      } catch (e) {
        // 错误提示已由 axios 拦截器统一处理
      }
    })
    .catch(() => {})
}

onMounted(loadTags)
</script>
