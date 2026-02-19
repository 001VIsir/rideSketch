<template>
  <div class="profile-page">
    <div class="profile-container">
      <el-card class="profile-card">
        <template #header>
          <div class="card-header">
            <span>个人中心</span>
            <el-button text type="danger" @click="handleLogout">
              退出登录
            </el-button>
          </div>
        </template>

        <div v-if="loading" class="loading">
          <el-skeleton :rows="5" animated />
        </div>

        <div v-else-if="userInfo" class="profile-content">
          <!-- 头像区域 -->
          <div class="avatar-section">
            <el-avatar :size="100" :src="userInfo.avatar || defaultAvatar">
              {{ userInfo.nickname?.charAt(0) || userInfo.username.charAt(0) }}
            </el-avatar>
            <el-button size="small" style="margin-top: 12px" @click="showAvatarDialog = true">
              更换头像
            </el-button>
          </div>

          <!-- 用户信息表单 -->
          <el-form
            ref="formRef"
            :model="formData"
            :rules="formRules"
            label-width="80px"
            class="profile-form"
          >
            <el-form-item label="用户名">
              <el-input v-model="userInfo.username" disabled />
            </el-form-item>

            <el-form-item label="昵称" prop="nickname">
              <el-input v-model="formData.nickname" placeholder="请输入昵称" />
            </el-form-item>

            <el-form-item label="邮箱" prop="email">
              <el-input v-model="formData.email" placeholder="请输入邮箱" />
            </el-form-item>

            <el-form-item>
              <el-button type="primary" :loading="saving" @click="handleSave">
                保存修改
              </el-button>
            </el-form-item>
          </el-form>
        </div>
      </el-card>
    </div>

    <!-- 更换头像对话框 -->
    <el-dialog v-model="showAvatarDialog" title="更换头像" width="400px">
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

// 状态
const loading = ref(true)
const saving = ref(false)
const showAvatarDialog = ref(false)

// 默认头像
const defaultAvatar = 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png'

// 预设头像选项
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

// 用户信息
const userInfo = ref<{
  id: number
  username: string
  nickname: string
  email: string
  avatar: string
} | null>(null)

// 表单数据
const formData = reactive({
  nickname: '',
  email: '',
  avatar: '',
})

// 表单验证规则
const formRules: FormRules = {
  nickname: [
    { max: 20, message: '昵称不能超过20个字符', trigger: 'blur' },
  ],
  email: [
    { type: 'email', message: '请输入正确的邮箱格式', trigger: 'blur' },
  ],
}

// 加载用户信息
async function loadUserInfo() {
  // 检查是否已登录
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

// 选择头像
function selectAvatar(avatar: string) {
  formData.avatar = avatar
}

// 确认选择头像
function confirmAvatar() {
  showAvatarDialog.value = false
}

// 保存修改
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

// 退出登录
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
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 40px 20px;
}

.profile-container {
  max-width: 600px;
  margin: 0 auto;
}

.profile-card {
  border-radius: 12px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 18px;
  font-weight: 600;
}

.loading {
  padding: 20px;
}

.profile-content {
  padding: 20px 0;
}

.avatar-section {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 20px 0;
  border-bottom: 1px solid #eee;
  margin-bottom: 20px;
}

.profile-form {
  max-width: 400px;
  margin: 0 auto;
}

.avatar-options {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  padding: 20px;
}

.avatar-option {
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 8px;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
}

.avatar-option:hover {
  background-color: #f5f7fa;
}

.avatar-option.selected {
  background-color: #ecf5ff;
  border: 2px solid #409eff;
}
</style>
