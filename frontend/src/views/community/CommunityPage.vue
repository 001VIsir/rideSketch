<template>
  <div class="community-page">
    <div class="community-container">
      <!-- 顶部栏 -->
      <div class="top-bar">
        <h2 class="page-title">社区路线</h2>
        <el-button type="primary" @click="handlePublish">
          发布路线
        </el-button>
      </div>

      <!-- 加载状态 -->
      <div v-if="loading" class="loading-state">
        <el-skeleton :rows="5" animated />
      </div>

      <!-- 路线列表 -->
      <div v-else-if="routeList.length > 0" class="route-list">
        <el-card
          v-for="route in routeList"
          :key="route.id"
          class="route-card"
          shadow="hover"
          @click="handleViewDetail(route.id)"
        >
          <div class="route-content">
            <div class="route-header">
              <div class="route-title">{{ route.title }}</div>
              <div class="route-city">{{ route.city }}</div>
            </div>

            <div class="route-desc">{{ route.description }}</div>

            <div class="route-stats">
              <span class="stat-item">
                <span class="stat-icon">📏</span>
                {{ formatDistance(route.distance) }}
              </span>
              <span class="stat-item">
                <span class="stat-icon">⏱️</span>
                {{ formatDuration(route.duration) }}
              </span>
              <span class="stat-item">
                <span class="stat-icon">❤️</span>
                {{ route.likes }}
              </span>
              <span class="stat-item">
                <span class="stat-icon">💬</span>
                {{ route.comments }}
              </span>
            </div>

            <div class="route-author">
              <el-avatar :size="24" :src="route.authorAvatar">
                {{ route.authorName?.charAt(0) }}
              </el-avatar>
              <span class="author-name">{{ route.authorName }}</span>
              <span class="create-time">{{ route.createTime }}</span>
            </div>
          </div>
        </el-card>
      </div>

      <!-- 空状态 -->
      <el-empty v-else description="暂无路线，快来发布第一条路线吧" />

      <!-- 分页 -->
      <div v-if="total > 0" class="pagination">
        <el-pagination
          v-model:current-page="page"
          :page-size="size"
          :total="total"
          layout="prev, pager, next"
          @current-change="handlePageChange"
        />
      </div>
    </div>

    <!-- 发布路线对话框 -->
    <el-dialog v-model="showPublishDialog" title="发布路线" width="500px">
      <el-form
        ref="formRef"
        :model="publishForm"
        :rules="formRules"
        label-width="80px"
      >
        <el-form-item label="标题" prop="title">
          <el-input v-model="publishForm.title" placeholder="请输入路线标题" />
        </el-form-item>

        <el-form-item label="描述" prop="description">
          <el-input
            v-model="publishForm.description"
            type="textarea"
            :rows="3"
            placeholder="请输入路线描述"
          />
        </el-form-item>

        <el-form-item label="城市" prop="city">
          <el-input v-model="publishForm.city" placeholder="请输入城市名称" />
        </el-form-item>

        <el-form-item label="距离(km)" prop="distance">
          <el-input-number v-model="publishForm.distance" :min="1" :max="500" />
        </el-form-item>

        <el-form-item label="时长(分钟)" prop="duration">
          <el-input-number v-model="publishForm.duration" :min="1" :max="1440" />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="showPublishDialog = false">取消</el-button>
        <el-button type="primary" :loading="publishing" @click="handleSubmitPublish">
          发布
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { getRouteList, publishRoute, type RouteBasicInfo } from '@/api/community'

const router = useRouter()
const formRef = ref<FormInstance>()

// 状态
const loading = ref(false)
const routeList = ref<RouteBasicInfo[]>([])
const total = ref(0)
const page = ref(1)
const size = ref(10)

// 发布对话框
const showPublishDialog = ref(false)
const publishing = ref(false)

// 发布表单
const publishForm = reactive({
  title: '',
  description: '',
  city: '',
  distance: 10,
  duration: 60,
})

// 表单验证
const formRules: FormRules = {
  title: [{ required: true, message: '请输入路线标题', trigger: 'blur' }],
  description: [{ required: true, message: '请输入路线描述', trigger: 'blur' }],
  city: [{ required: true, message: '请输入城市名称', trigger: 'blur' }],
}

// 格式化距离
function formatDistance(meters: number): string {
  if (meters >= 1000) {
    return (meters / 1000).toFixed(1) + ' km'
  }
  return meters + ' m'
}

// 格式化时间
function formatDuration(seconds: number): string {
  const minutes = Math.floor(seconds / 60)
  if (minutes >= 60) {
    const hours = Math.floor(minutes / 60)
    const mins = minutes % 60
    return `${hours}小时${mins}分钟`
  }
  return `${minutes}分钟`
}

// 加载路线列表
async function loadRouteList() {
  loading.value = true
  try {
    const result = await getRouteList({ page: page.value, size: size.value })
    routeList.value = result.list
    total.value = result.total
  } catch (error) {
    console.error('加载路线列表失败:', error)
    ElMessage.error('加载失败，请稍后重试')
  } finally {
    loading.value = false
  }
}

// 处理分页变化
function handlePageChange(newPage: number) {
  page.value = newPage
  loadRouteList()
}

// 查看路线详情
function handleViewDetail(id: number) {
  router.push(`/community/route/${id}`)
}

// 发布路线
function handlePublish() {
  showPublishDialog.value = true
}

// 提交发布
async function handleSubmitPublish() {
  if (!formRef.value) return

  await formRef.value.validate(async (valid) => {
    if (!valid) return

    publishing.value = true
    try {
      await publishRoute({
        title: publishForm.title,
        description: publishForm.description,
        city: publishForm.city,
        distance: publishForm.distance * 1000, // 转换为米
        duration: publishForm.duration * 60, // 转换为秒
        path: '',
      })

      ElMessage.success('发布成功')
      showPublishDialog.value = false
      loadRouteList()

      // 重置表单
      publishForm.title = ''
      publishForm.description = ''
      publishForm.city = ''
      publishForm.distance = 10
      publishForm.duration = 60
    } catch (error: any) {
      console.error('发布失败:', error)
      ElMessage.error(error.response?.data?.message || '发布失败，请稍后重试')
    } finally {
      publishing.value = false
    }
  })
}

onMounted(() => {
  loadRouteList()
})
</script>

<style scoped>
.community-page {
  min-height: 100vh;
  padding: 20px;
  background: #f5f7fa;
}

.community-container {
  max-width: 800px;
  margin: 0 auto;
}

.top-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.page-title {
  font-size: 20px;
  font-weight: 600;
  color: #303133;
}

.loading-state {
  padding: 20px;
}

.route-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.route-card {
  cursor: pointer;
  transition: transform 0.2s;
}

.route-card:hover {
  transform: translateY(-2px);
}

.route-content {
  padding: 8px;
}

.route-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.route-title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

.route-city {
  font-size: 12px;
  color: #909399;
  background: #f0f2f5;
  padding: 2px 8px;
  border-radius: 4px;
}

.route-desc {
  font-size: 14px;
  color: #606266;
  margin-bottom: 12px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.route-stats {
  display: flex;
  gap: 16px;
  margin-bottom: 12px;
}

.stat-item {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 13px;
  color: #909399;
}

.stat-icon {
  font-size: 14px;
}

.route-author {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
  color: #909399;
}

.author-name {
  color: #606266;
}

.create-time {
  margin-left: auto;
}

.pagination {
  margin-top: 20px;
  display: flex;
  justify-content: center;
}
</style>
