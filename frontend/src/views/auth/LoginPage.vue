<template>
  <div class="login-page">
    <div class="login-container">
      <el-card class="login-card">
        <template #header>
          <div class="card-header">
            <span>登录</span>
            <span class="register-link">
              还没有账号？
              <a @click="goToRegister">立即注册</a>
            </span>
          </div>
        </template>

        <el-form
          ref="formRef"
          :model="formData"
          :rules="formRules"
          label-position="top"
          class="login-form"
        >
          <el-form-item label="用户名/邮箱" prop="usernameOrEmail">
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
      </el-card>
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

// 状态
const loading = ref(false)

// 表单数据
const formData = reactive({
  usernameOrEmail: '',
  password: '',
})

// 表单验证规则
const formRules: FormRules = {
  usernameOrEmail: [
    { required: true, message: '请输入用户名或邮箱', trigger: 'blur' },
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码至少6位', trigger: 'blur' },
  ],
}

// 登录
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

// 跳转到注册页
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
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 20px;
}

.login-container {
  width: 100%;
  max-width: 420px;
}

.login-card {
  border-radius: 12px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.1);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 20px;
  font-weight: 600;
}

.register-link {
  font-size: 14px;
  color: #666;
  font-weight: normal;
}

.register-link a {
  color: #409eff;
  cursor: pointer;
  text-decoration: none;
}

.register-link a:hover {
  text-decoration: underline;
}

.login-form {
  padding: 20px 0;
}

.login-button {
  width: 100%;
  margin-top: 10px;
}
</style>
