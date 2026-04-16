<template>
  <div class="profile-page">
    <div class="profile-container">
      <div class="profile-card">
        <div class="card-header-section">
          <div class="card-icon">
            <svg width="24" height="24" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
              <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/>
              <circle cx="12" cy="7" r="4"/>
            </svg>
          </div>
          <div class="card-title-group">
            <h2 class="card-title">个人中心</h2>
            <p class="card-subtitle">管理您的账户信息</p>
          </div>
          <el-button text type="danger" class="logout-btn" @click="handleLogout">
            <svg class="btn-icon" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"/>
              <polyline points="16 17 21 12 16 7"/>
              <line x1="21" y1="12" x2="9" y2="12"/>
            </svg>
            退出登录
          </el-button>
        </div>

        <div v-if="loading" class="loading">
          <el-skeleton :rows="5" animated />
        </div>

        <div v-else-if="userInfo" class="profile-content">
          <div class="avatar-section">
            <div class="avatar-wrapper">
              <el-avatar :size="100" :src="userInfo.avatar || defaultAvatar">
                {{ userInfo.nickname?.charAt(0) || userInfo.username.charAt(0) }}
              </el-avatar>
              <div class="avatar-badge" @click="showAvatarDialog = true">
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M23 19a2 2 0 0 1-2 2H3a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h4l2-3h6l2 3h4a2 2 0 0 1 2 2z"/>
                  <circle cx="12" cy="13" r="4"/>
                </svg>
              </div>
            </div>
            <el-button size="small" class="change-avatar-btn" @click="showAvatarDialog = true">
              更换头像
            </el-button>
          </div>

          <el-form
            ref="formRef"
            :model="formData"
            :rules="formRules"
            label-position="top"
            class="profile-form"
          >
            <div class="form-section">
              <div class="form-section-title">基本信息</div>
              <el-form-item label="用户名">
                <el-input v-model="userInfo.username" disabled />
              </el-form-item>

              <el-form-item label="昵称" prop="nickname">
                <el-input v-model="formData.nickname" placeholder="请输入昵称" />
              </el-form-item>

              <el-form-item label="邮箱" prop="email">
                <el-input v-model="formData.email" placeholder="请输入邮箱" />
              </el-form-item>
            </div>

            <el-form-item>
              <el-button type="primary" :loading="saving" class="save-button" @click="handleSave">
                保存修改
              </el-button>
            </el-form-item>
          </el-form>
        </div>
      </div>
    </div>

    <!-- 更换头像对话框 -->
    <el-dialog v-model="showAvatarDialog" title="选择头像" width="480px" class="avatar-dialog">
      <div class="avatar-options">
        <div
          v-for="avatar in avatarOptions"
          :key="avatar"
          class="avatar-option"
          :class="{ selected: formData.avatar === avatar }"
          @click="selectAvatar(avatar)"
        >
          <el-avatar :size="60" :src="avatar" />
        </div>
      </div>
      <template #footer>
        <el-button @click="showAvatarDialog = false">取消</el-button>
        <el-button type="primary" @click="confirmAvatar">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { getCurrentUser, updateUserInfo, logout, getToken } from '@/api/user'

const router = useRouter()
const formRef = ref<FormInstance>()

const loading = ref(true)
const saving = ref(false)
const showAvatarDialog = ref(false)

const defaultAvatar = 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png'

const avatarOptions = [
  'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png',
  'https://cube.elemecdn.com/9/c2/f0ee8a3c7c9638a54940382568c9dpng.png',
  'https://cube.elemecdn.com/e/dd/2807e0c7c7d7d3c23e3c2e9e0d29png.png',
  'https://cube.elemecdn.com/a/3f/3302e59f9a93d1e4a9d5d2e1d9c7png.png',
  'https://cube.elemecdn.com/0/88/03b0d39583f48203968b5694e1d28png.png',
  'https://cube.elemecdn.com/6/94/4d3ea53c084bad6931a56d5158a48jpeg.jpeg',
  'https://cube.elemecdn.com/3/22/2014b5e4d9ed73e5d62318e70e6f9jpeg.jpeg',
  'https://cube.elemecdn.com/1/34/d4e7fc2a055d4f9323c6f9e6b7b9jpeg.jpeg',
]

const userInfo = ref<{
  id: number
  username: string
  nickname: string
  email: string
  avatar: string
} | null>(null)

const formData = reactive({
  nickname: '',
  email: '',
  avatar: '',
})

const formRules: FormRules = {
  nickname: [
    { max: 20, message: '昵称不能超过20个字符', trigger: 'blur' },
  ],
  email: [
    { type: 'email', message: '请输入正确的邮箱格式', trigger: 'blur' },
  ],
}

async function loadUserInfo() {
  if (!getToken()) {
    ElMessage.warning('请先登录')
    router.push('/')
    return
  }

  loading.value = true
  try {
    const data = await getCurrentUser()
    userInfo.value = data
    formData.nickname = data.nickname || ''
    formData.email = data.email || ''
    formData.avatar = data.avatar || ''
  } catch (error) {
    console.error('获取用户信息失败:', error)
    ElMessage.error('获取用户信息失败，请重新登录')
    logout()
    router.push('/')
  } finally {
    loading.value = false
  }
}

function selectAvatar(avatar: string) {
  formData.avatar = avatar
}

function confirmAvatar() {
  showAvatarDialog.value = false
}

async function handleSave() {
  if (!formRef.value) return

  await formRef.value.validate(async (valid) => {
    if (!valid) return

    saving.value = true
    try {
      const data = await updateUserInfo({
        nickname: formData.nickname,
        email: formData.email,
        avatar: formData.avatar,
      })

      userInfo.value = data
      ElMessage.success('保存成功')
    } catch (error: any) {
      console.error('保存失败:', error)
      ElMessage.error(error.response?.data?.message || '保存失败，请稍后重试')
    } finally {
      saving.value = false
    }
  })
}

function handleLogout() {
  logout()
  ElMessage.success('已退出登录')
  router.push('/')
}

onMounted(() => {
  loadUserInfo()
})
</script>

<style scoped>
.profile-page {
  min-height: 100vh;
  padding: 40px 20px;
  background: linear-gradient(135deg, #f8fafc 0%, #f0fdfa 50%, #faf5f0 100%);
}

.profile-container {
  max-width: 600px;
  margin: 0 auto;
}

.profile-card {
  background: #ffffff;
  border-radius: 20px;
  box-shadow: 0 4px 24px rgba(0, 0, 0, 0.06);
  overflow: hidden;
}

.card-header-section {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 24px;
  background: linear-gradient(135deg, #f0fdfa 0%, #faf5f0 100%);
  border-bottom: 1px solid #f1f5f9;
}

.card-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 48px;
  height: 48px;
  background: #ffffff;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  color: #14b8a6;
}

.card-title-group {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.card-title {
  font-size: 20px;
  font-weight: 700;
  color: #1e293b;
  margin: 0;
}

.card-subtitle {
  font-size: 13px;
  color: #94a3b8;
  margin: 0;
}

.logout-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  color: #94a3b8;
}

.logout-btn:hover {
  color: #ef4444 !important;
  background: #fef2f2 !important;
}

.btn-icon {
  width: 16px;
  height: 16px;
}

.loading {
  padding: 40px 24px;
}

.profile-content {
  padding: 32px 24px;
}

.avatar-section {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 24px 0;
  margin-bottom: 24px;
  border-bottom: 1px solid #f1f5f9;
}

.avatar-wrapper {
  position: relative;
  margin-bottom: 12px;
}

.avatar-badge {
  position: absolute;
  bottom: 0;
  right: 0;
  width: 32px;
  height: 32px;
  background: #14b8a6;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #ffffff;
  cursor: pointer;
  transition: all 0.2s;
  box-shadow: 0 2px 8px rgba(20, 184, 166, 0.3);
}

.avatar-badge:hover {
  transform: scale(1.1);
}

.change-avatar-btn {
  border-radius: 8px !important;
  color: #64748b;
  border-color: #e2e8f0;
}

.change-avatar-btn:hover {
  color: #14b8a6;
  border-color: #14b8a6;
  background: #f0fdfa;
}

.profile-form {
  max-width: 400px;
  margin: 0 auto;
}

.form-section {
  margin-bottom: 24px;
}

.form-section-title {
  font-size: 14px;
  font-weight: 600;
  color: #475569;
  margin-bottom: 16px;
  padding-bottom: 8px;
  border-bottom: 1px solid #f1f5f9;
}

.profile-form :deep(.el-form-item__label) {
  font-weight: 500;
  color: #475569;
  font-size: 14px;
}

.profile-form :deep(.el-input__wrapper) {
  border-radius: 10px !important;
}

.profile-form :deep(.el-input__inner) {
  height: 40px;
}

.save-button {
  width: 100%;
  height: 44px;
  border-radius: 10px !important;
  font-size: 15px;
  font-weight: 600;
  background: linear-gradient(135deg, #14b8a6 0%, #0d9488 100%) !important;
  border: none !important;
  box-shadow: 0 4px 12px rgba(20, 184, 166, 0.25);
  transition: all 0.3s ease;
}

.save-button:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(20, 184, 166, 0.35);
}

.avatar-dialog :deep(.el-dialog) {
  border-radius: 16px;
}

.avatar-options {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  padding: 8px;
}

.avatar-option {
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 8px;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.2s;
  border: 2px solid transparent;
}

.avatar-option:hover {
  background: #f0fdfa;
}

.avatar-option.selected {
  background: #f0fdfa;
  border-color: #14b8a6;
}
</style>
