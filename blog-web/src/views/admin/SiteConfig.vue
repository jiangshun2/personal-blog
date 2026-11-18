<template>
  <div class="admin-page">
    <div class="admin-page-head">
      <div>
        <h2 class="admin-page-title">站点设置</h2>
        <p class="admin-page-desc">站点名称、描述、头像与社交链接会展示在前台页面</p>
      </div>
      <el-button :icon="Refresh" :loading="loading" @click="loadConfig">刷新</el-button>
    </div>

    <div class="admin-card">
      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-width="110px"
        style="max-width: 680px"
        v-loading="loading"
      >
        <el-form-item label="站点名称" prop="site_name">
          <el-input
            v-model="form.site_name"
            maxlength="30"
            show-word-limit
            placeholder="请输入站点名称"
          />
        </el-form-item>

        <el-form-item label="站点描述" prop="site_description">
          <el-input
            v-model="form.site_description"
            type="textarea"
            :rows="3"
            maxlength="120"
            show-word-limit
            placeholder="一句话介绍你的博客"
          />
        </el-form-item>

        <el-form-item label="站长头像" prop="site_avatar">
          <div class="config-field">
            <el-input v-model="form.site_avatar" placeholder="头像图片地址" clearable />
            <img
              v-if="form.site_avatar"
              :src="form.site_avatar"
              alt="头像预览"
              class="config-avatar-preview"
            />
          </div>
        </el-form-item>

        <el-form-item label="GitHub" prop="github">
          <el-input
            v-model="form.github"
            placeholder="GitHub 主页地址，例如 https://github.com/yourname"
            clearable
          />
        </el-form-item>

        <el-form-item label="联系邮箱" prop="email">
          <el-input v-model="form.email" placeholder="联系邮箱" clearable />
        </el-form-item>

        <el-form-item>
          <el-button type="primary" :loading="saving" @click="handleSave">保存设置</el-button>
          <el-button @click="loadConfig">还原</el-button>
        </el-form-item>
      </el-form>
    </div>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'
import { getSiteConfig } from '@/api/site'
import { updateSiteConfig } from '@/api/admin'
import { useSiteStore } from '@/stores/site'

const siteStore = useSiteStore()

const formRef = ref(null)
const loading = ref(false)
const saving = ref(false)

const form = reactive({
  site_name: '',
  site_description: '',
  site_avatar: '',
  github: '',
  email: ''
})

const rules = {
  site_name: [{ required: true, message: '请输入站点名称', trigger: 'blur' }],
  email: [{ type: 'email', message: '邮箱格式不正确', trigger: 'blur' }]
}

/** 读取站点配置（公开接口） */
async function loadConfig() {
  loading.value = true
  try {
    const data = await getSiteConfig()
    const config = data || {}
    form.site_name = config.site_name || ''
    form.site_description = config.site_description || ''
    form.site_avatar = config.site_avatar || ''
    form.github = config.github || ''
    form.email = config.email || ''
  } catch (e) {
    // 错误提示已由 axios 拦截器统一处理
  } finally {
    loading.value = false
  }
}

/** 保存站点配置（后台接口，保存后刷新全局站点状态） */
async function handleSave() {
  if (!formRef.value) return
  formRef.value.validate(async (valid) => {
    if (!valid) return
    saving.value = true
    try {
      await updateSiteConfig({
        site_name: form.site_name.trim(),
        site_description: form.site_description || '',
        site_avatar: form.site_avatar || '',
        github: form.github || '',
        email: form.email || ''
      })
      ElMessage.success('站点设置已保存')
      siteStore.fetchConfig(true)
    } catch (e) {
      // 错误提示已由 axios 拦截器统一处理
    } finally {
      saving.value = false
    }
  })
}

onMounted(loadConfig)
</script>

<style scoped>
.config-field {
  display: flex;
  align-items: center;
  gap: 14px;
  width: 100%;
}

.config-field .el-input {
  max-width: 420px;
}

.config-avatar-preview {
  width: 52px;
  height: 52px;
  border: 1px solid var(--el-border-color);
  border-radius: 50%;
  object-fit: cover;
}
</style>
