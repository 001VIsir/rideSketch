<template>
  <div class="login-page">
    <div class="login-container">
      <div class="login-card">
        <div class="card-header">
          <div class="brand-mark">
            <svg width="40" height="40" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
              <path d="M12 2C8.13 2 5 5.13 5 9c0 5.25 7 13 7 13s7-7.75 7-13c0-3.87-3.13-7-7-7z" fill="#14b8a6"/>
              <circle cx="12" cy="9" r="2.5" fill="white"/>
            </svg>
          </div>
          <h1 class="card-title">欢迎回来</h1>
          <p class="card-subtitle">登录到骑迹，开始您的骑行之旅</p>
        </div>

        <el-form
          ref="formRef"
          :model="formData"
          :rules="formRules"
          label-position="top"
          class="login-form"
        >
          <el-form-item label="用户名 / 邮箱" prop="usernameOrEmail">
            <el-input
              v-model="formData.usernameOrEmail"
              placeholder="请输入用户名或邮箱"
              size="large"
              :prefix-icon="User"
            />
          </el-form-item>

          <el-form-item label="密码" prop="password">
            <el-input
              v-model="formData.password"
              type="password"
              placeholder="请输入密码"
              size="large"
              :prefix-icon="Lock"
              show-password
              @keyup.enter="handleLogin"
            />
          </el-form-item>

          <el-form-item>
            <el-button
              type="primary"
              size="large"
              :loading="loading"
              class="login-button"
              @click="handleLogin"
            >
              登录
            </el-button>
          </el-form-item>
        </el-form>

        <div class="card-footer">
          <span class="footer-text">还没有账号？</span>
          <a class="footer-link" @click="goToRegister">立即注册</a>
        </div>
      </div>

      <div class="decoration decoration-1"></div>
      <div class="decoration decoration-2"></div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { User, Lock } from '@element-plus/icons-vue'
import { login } from '@/api/user'

const router = useRouter()
const formRef = ref<FormInstance>()

const loading = ref(false)

const formData = reactive({
  usernameOrEmail: '',
  password: '',
})

const formRules: FormRules = {
  usernameOrEmail: [
    { required: true, message: '请输入用户名或邮箱', trigger: 'blur' },
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码至少6位', trigger: 'blur' },
  ],
}

async function handleLogin() {
  if (!formRef.value) return

  await formRef.value.validate(async (valid) => {
    if (!valid) return

    loading.value = true
    try {
      const response = await login({
        usernameOrEmail: formData.usernameOrEmail,
        password: formData.password,
      })

      if (response.success) {
        ElMessage.success('登录成功')
        router.push('/map')
      } else {
        ElMessage.error(response.message || '登录失败')
      }
    } catch (error: any) {
      console.error('登录失败:', error)
      ElMessage.error(error.response?.data?.message || '登录失败，请检查用户名和密码')
    } finally {
      loading.value = false
    }
  })
}

function goToRegister() {
  router.push('/register')
}
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #f8fafc 0%, #f0fdfa 50%, #faf5f0 100%);
  padding: 20px;
  position: relative;
  overflow: hidden;
}

.login-container {
  width: 100%;
  max-width: 420px;
  position: relative;
  z-index: 1;
}

.login-card {
  background: #ffffff;
  border-radius: 20px;
  box-shadow: 0 4px 24px rgba(0, 0, 0, 0.06), 0 1px 2px rgba(0, 0, 0, 0.04);
  padding: 40px;
  border: 1px solid rgba(226, 232, 240, 0.8);
}

.card-header {
  text-align: center;
  margin-bottom: 32px;
}

.brand-mark {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 72px;
  height: 72px;
  background: linear-gradient(135deg, #f0fdfa 0%, #ccfbf1 100%);
  border-radius: 18px;
  margin-bottom: 20px;
  box-shadow: 0 4px 12px rgba(20, 184, 166, 0.15);
}

.card-title {
  font-size: 26px;
  font-weight: 700;
  color: #1e293b;
  margin-bottom: 8px;
  letter-spacing: -0.02em;
}

.card-subtitle {
  font-size: 14px;
  color: #94a3b8;
  margin: 0;
}

.login-form {
  padding: 0;
}

.login-form :deep(.el-form-item__label) {
  font-weight: 500;
  color: #475569;
  font-size: 14px;
  padding-bottom: 8px !important;
}

.login-form :deep(.el-input__wrapper) {
  border-radius: 12px !important;
  padding: 4px 16px;
  height: 48px;
}

.login-form :deep(.el-input__inner) {
  font-size: 15px;
}

.login-button {
  width: 100%;
  height: 48px;
  border-radius: 12px !important;
  font-size: 16px;
  font-weight: 600;
  margin-top: 8px;
  background: linear-gradient(135deg, #14b8a6 0%, #0d9488 100%) !important;
  border: none !important;
  box-shadow: 0 4px 12px rgba(20, 184, 166, 0.25);
  transition: all 0.3s ease;
}

.login-button:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(20, 184, 166, 0.35);
}

.login-button:active {
  transform: translateY(0);
}

.card-footer {
  text-align: center;
  margin-top: 24px;
  padding-top: 24px;
  border-top: 1px solid #f1f5f9;
}

.footer-text {
  color: #94a3b8;
  font-size: 14px;
}

.footer-link {
  color: #14b8a6;
  font-weight: 600;
  font-size: 14px;
  margin-left: 4px;
  cursor: pointer;
  transition: color 0.2s;
}

.footer-link:hover {
  color: #0d9488;
}

/* Decorative Elements */
.decoration {
  position: absolute;
  border-radius: 50%;
  opacity: 0.6;
}

.decoration-1 {
  width: 300px;
  height: 300px;
  background: linear-gradient(135deg, rgba(20, 184, 166, 0.1) 0%, rgba(20, 184, 166, 0.05) 100%);
  top: -100px;
  right: -100px;
}

.decoration-2 {
  width: 200px;
  height: 200px;
  background: linear-gradient(135deg, rgba(249, 115, 22, 0.08) 0%, rgba(249, 115, 22, 0.03) 100%);
  bottom: -50px;
  left: -80px;
}
</style>
