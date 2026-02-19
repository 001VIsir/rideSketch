<template>
  <div class="register-page">
    <div class="register-container">
      <el-card class="register-card">
        <template #header>
          <div class="card-header">
            <span>注册</span>
            <span class="login-link">
              已有账号？
              <a @click="goToLogin">立即登录</a>
            </span>
          </div>
        </template>

        <el-form
          ref="formRef"
          :model="formData"
          :rules="formRules"
          label-position="top"
          class="register-form"
        >
          <el-form-item label="用户名" prop="username">
            <el-input
              v-model="formData.username"
              placeholder="请输入用户名"
              size="large"
              :prefix-icon="User"
            />
          </el-form-item>

          <el-form-item label="邮箱" prop="email">
            <el-input
              v-model="formData.email"
              placeholder="请输入邮箱"
              size="large"
              :prefix-icon="Message"
            />
          </el-form-item>

          <el-form-item label="昵称（可选）" prop="nickname">
            <el-input
              v-model="formData.nickname"
              placeholder="请输入昵称"
              size="large"
              :prefix-icon="UserFilled"
            />
          </el-form-item>

          <el-form-item label="密码" prop="password">
            <el-input
              v-model="formData.password"
              type="password"
              placeholder="请输入密码（至少6位）"
              size="large"
              :prefix-icon="Lock"
              show-password
            />
          </el-form-item>

          <el-form-item label="确认密码" prop="confirmPassword">
            <el-input
              v-model="formData.confirmPassword"
              type="password"
              placeholder="请再次输入密码"
              size="large"
              :prefix-icon="Lock"
              show-password
              @keyup.enter="handleRegister"
            />
          </el-form-item>

          <el-form-item>
            <el-button
              type="primary"
              size="large"
              :loading="loading"
              class="register-button"
              @click="handleRegister"
            >
              注册
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
import { User, Lock, Message, UserFilled } from '@element-plus/icons-vue'
import { register } from '@/api/user'

const router = useRouter()
const formRef = ref<FormInstance>()

// 状态
const loading = ref(false)

// 表单数据
const formData = reactive({
  username: '',
  email: '',
  nickname: '',
  password: '',
  confirmPassword: '',
})

// 验证确认密码
const validateConfirmPassword = (rule: any, value: string, callback: any) => {
  if (value !== formData.password) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

// 表单验证规则
const formRules: FormRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 20, message: '用户名长度在 3 到 20 个字符', trigger: 'blur' },
  ],
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '请输入正确的邮箱格式', trigger: 'blur' },
  ],
  nickname: [
    { max: 20, message: '昵称不能超过20个字符', trigger: 'blur' },
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码至少6位', trigger: 'blur' },
  ],
  confirmPassword: [
    { required: true, message: '请再次输入密码', trigger: 'blur' },
    { validator: validateConfirmPassword, trigger: 'blur' },
  ],
}

// 注册
async function handleRegister() {
  if (!formRef.value) return

  await formRef.value.validate(async (valid) => {
    if (!valid) return

    loading.value = true
    try {
      const response = await register({
        username: formData.username,
        email: formData.email,
        password: formData.password,
        nickname: formData.nickname || undefined,
      })

      if (response.success) {
        ElMessage.success('注册成功，请登录')
        router.push('/login')
      } else {
        ElMessage.error(response.message || '注册失败')
      }
    } catch (error: any) {
      console.error('注册失败:', error)
      ElMessage.error(error.response?.data?.message || '注册失败，请稍后重试')
    } finally {
      loading.value = false
    }
  })
}

// 跳转到登录页
function goToLogin() {
  router.push('/login')
}
</script>

<style scoped>
.register-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 20px;
}

.register-container {
  width: 100%;
  max-width: 420px;
}

.register-card {
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

.login-link {
  font-size: 14px;
  color: #666;
  font-weight: normal;
}

.login-link a {
  color: #409eff;
  cursor: pointer;
  text-decoration: none;
}

.login-link a:hover {
  text-decoration: underline;
}

.register-form {
  padding: 20px 0;
}

.register-button {
  width: 100%;
  margin-top: 10px;
}
</style>
