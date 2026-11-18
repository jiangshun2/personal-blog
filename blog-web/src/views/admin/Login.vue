<template>
  <div class="login-page">
    <div class="login-card">
      <div class="login-brand">
        <span class="login-mark">B</span>
        <div class="login-brand-text">
          <h1 class="login-title">博客管理后台</h1>
          <p class="login-subtitle">{{ siteStore.siteName }}</p>
        </div>
      </div>

      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-position="top"
        size="large"
        @keyup.enter="handleLogin"
      >
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" placeholder="请输入用户名" :prefix-icon="User" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input
            v-model="form.password"
            type="password"
            show-password
            placeholder="请输入密码"
            :prefix-icon="Lock"
          />
        </el-form-item>

        <el-button
          type="primary"
          class="login-submit"
          :loading="loading"
          @click="handleLogin"
        >
          登 录
        </el-button>
      </el-form>

      <div class="login-footer">
        <router-link to="/" class="login-link">← 返回博客首页</router-link>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Lock, User } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { useSiteStore } from '@/stores/site'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const siteStore = useSiteStore()

const formRef = ref(null)
const loading = ref(false)

const form = reactive({
  username: '',
  password: ''
})

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

async function handleLogin() {
  if (!formRef.value) return
  formRef.value.validate(async (valid) => {
    if (!valid) return
    loading.value = true
    try {
      await userStore.login({ username: form.username.trim(), password: form.password })
      ElMessage.success('登录成功，欢迎回来！')
      // 登录成功后回到来源页面，默认进入仪表盘
      const redirect = route.query.redirect
      if (typeof redirect === 'string' && redirect.indexOf('/admin') === 0) {
        router.replace(redirect)
      } else {
        router.replace('/admin/dashboard')
      }
    } catch (e) {
      // 错误提示已由 axios 拦截器统一处理
    } finally {
      loading.value = false
    }
  })
}

onMounted(() => {
  siteStore.fetchConfig()
})
</script>

<style scoped>
.login-page {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 100vh;
  padding: 24px;
  background: linear-gradient(135deg, #eef3ff 0%, #f7f9fd 45%, #e9faf7 100%);
  overflow: hidden;
}

.login-page::before,
.login-page::after {
  content: '';
  position: absolute;
  border-radius: 50%;
  pointer-events: none;
}

.login-page::before {
  width: 420px;
  height: 420px;
  top: -160px;
  right: -100px;
  background: radial-gradient(circle at 30% 30%, rgba(140, 175, 255, 0.5), rgba(140, 175, 255, 0));
}

.login-page::after {
  width: 360px;
  height: 360px;
  bottom: -150px;
  left: -80px;
  background: radial-gradient(circle at 60% 40%, rgba(140, 225, 214, 0.5), rgba(140, 225, 214, 0));
}

.login-card {
  position: relative;
  z-index: 2;
  width: 100%;
  max-width: 400px;
  padding: 36px 36px 26px;
  border: 1px solid #e5ebf6;
  border-radius: 18px;
  background: #fff;
  box-shadow: 0 18px 50px rgba(31, 58, 120, 0.12);
}

.login-brand {
  display: flex;
  align-items: center;
  gap: 14px;
  margin-bottom: 26px;
}

.login-mark {
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  width: 46px;
  height: 46px;
  border-radius: 13px;
  background: linear-gradient(135deg, #2f6fed, #12b3a8);
  color: #fff;
  font-size: 22px;
  font-weight: 800;
}

.login-title {
  font-size: 21px;
  font-weight: 700;
  line-height: 1.3;
}

.login-subtitle {
  margin-top: 2px;
  font-size: 13px;
  color: #8b95a5;
}

.login-submit {
  width: 100%;
  height: 44px;
  margin-top: 6px;
  font-size: 15px;
  letter-spacing: 4px;
}

.login-footer {
  margin-top: 18px;
  text-align: center;
}

.login-link {
  font-size: 13px;
  color: #8b95a5;
  transition: color 0.2s ease;
}

.login-link:hover {
  color: #2f6fed;
}
</style>
