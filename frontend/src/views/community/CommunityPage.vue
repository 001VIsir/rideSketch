<template>
  <div class="community-page">
    <div class="community-container">
      <!-- 顶部栏 -->
      <div class="top-bar">
        <div class="page-header">
          <h2 class="page-title">社区路线</h2>
          <p class="page-subtitle">发现精彩骑行路线</p>
        </div>
        <el-button type="primary" class="publish-btn" @click="handlePublish">
          <svg class="btn-icon" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <line x1="12" y1="5" x2="12" y2="19"/>
            <line x1="5" y1="12" x2="19" y2="12"/>
          </svg>
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
              <div class="route-city">
                <svg class="city-icon" width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0 1 18 0z"/>
                  <circle cx="12" cy="10" r="3"/>
                </svg>
                {{ route.city }}
              </div>
            </div>

            <div class="route-desc">{{ route.description }}</div>

            <div class="route-stats">
              <span class="stat-item">
                <svg class="stat-icon" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0 1 18 0z"/>
                  <circle cx="12" cy="10" r="3"/>
                </svg>
                {{ formatDistance(route.distance) }}
              </span>
              <span class="stat-item">
                <svg class="stat-icon" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <circle cx="12" cy="12" r="10"/>
                  <polyline points="12 6 12 12 16 14"/>
                </svg>
                {{ formatDuration(route.duration) }}
              </span>
              <span class="stat-item">
                <svg class="stat-icon" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z"/>
                </svg>
                {{ route.likes }}
              </span>
              <span class="stat-item">
                <svg class="stat-icon" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/>
                </svg>
                {{ route.comments }}
              </span>
            </div>

            <div class="route-author">
              <el-avatar :size="28" :src="route.authorAvatar">
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
    <el-dialog v-model="showPublishDialog" title="发布路线" width="500px" class="publish-dialog">
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

const loading = ref(false)
const routeList = ref<RouteBasicInfo[]>([])
const total = ref(0)
const page = ref(1)
const size = ref(10)

const showPublishDialog = ref(false)
const publishing = ref(false)

const publishForm = reactive({
  title: '',
  description: '',
  city: '',
  distance: 10,
  duration: 60,
})

const formRules: FormRules = {
  title: [{ required: true, message: '请输入路线标题', trigger: 'blur' }],
  description: [{ required: true, message: '请输入路线描述', trigger: 'blur' }],
  city: [{ required: true, message: '请输入城市名称', trigger: 'blur' }],
}

function formatDistance(meters: number): string {
  if (meters >= 1000) {
    return (meters / 1000).toFixed(1) + ' km'
  }
  return meters + ' m'
}

function formatDuration(seconds: number): string {
  const minutes = Math.floor(seconds / 60)
  if (minutes >= 60) {
    const hours = Math.floor(minutes / 60)
    const mins = minutes % 60
    return `${hours}小时${mins}分钟`
  }
  return `${minutes}分钟`
}

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

function handlePageChange(newPage: number) {
  page.value = newPage
  loadRouteList()
}

function handleViewDetail(id: number) {
  router.push(`/community/route/${id}`)
}

function handlePublish() {
  showPublishDialog.value = true
}

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
        distance: publishForm.distance * 1000,
        duration: publishForm.duration * 60,
        path: '',
      })

      ElMessage.success('发布成功')
      showPublishDialog.value = false
      loadRouteList()

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
  padding: 24px;
  background: #f8fafc;
}

.community-container {
  max-width: 900px;
  margin: 0 auto;
}

.top-bar {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 24px;
  padding: 24px;
  background: #ffffff;
  border-radius: 16px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);
}

.page-header {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.page-title {
  font-size: 24px;
  font-weight: 700;
  color: #1e293b;
  margin: 0;
}

.page-subtitle {
  font-size: 14px;
  color: #94a3b8;
  margin: 0;
}

.publish-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 10px 20px;
  border-radius: 10px !important;
  font-weight: 500;
  background: linear-gradient(135deg, #14b8a6 0%, #0d9488 100%) !important;
  border: none !important;
  box-shadow: 0 4px 12px rgba(20, 184, 166, 0.25);
  transition: all 0.3s ease;
}

.publish-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 16px rgba(20, 184, 166, 0.35);
}

.btn-icon {
  width: 16px;
  height: 16px;
}

.loading-state {
  padding: 20px;
  background: #ffffff;
  border-radius: 16px;
}

.route-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.route-card {
  border-radius: 16px !important;
  border: none !important;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04) !important;
  cursor: pointer;
  transition: all 0.3s ease;
}

.route-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.08) !important;
}

.route-content {
  padding: 8px;
}

.route-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.route-title {
  font-size: 18px;
  font-weight: 600;
  color: #1e293b;
}

.route-city {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: #64748b;
  background: #f1f5f9;
  padding: 4px 10px;
  border-radius: 20px;
}

.city-icon {
  color: #94a3b8;
}

.route-desc {
  font-size: 14px;
  color: #64748b;
  margin-bottom: 16px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  line-height: 1.6;
}

.route-stats {
  display: flex;
  gap: 20px;
  margin-bottom: 16px;
  padding-bottom: 16px;
  border-bottom: 1px solid #f1f5f9;
}

.stat-item {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: #64748b;
}

.stat-icon {
  color: #94a3b8;
}

.route-author {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 13px;
}

.author-name {
  color: #475569;
  font-weight: 500;
}

.create-time {
  margin-left: auto;
  color: #94a3b8;
}

.pagination {
  margin-top: 24px;
  display: flex;
  justify-content: center;
}

.pagination :deep(.el-pager li) {
  border-radius: 8px;
  font-weight: 500;
}

.pagination :deep(.el-pager li.is-active) {
  background: #14b8a6;
}

.publish-dialog :deep(.el-dialog) {
  border-radius: 16px;
}

.publish-dialog :deep(.el-dialog__header) {
  padding: 20px 24px;
  border-bottom: 1px solid #f1f5f9;
}

.publish-dialog :deep(.el-dialog__body) {
  padding: 24px;
}

.publish-dialog :deep(.el-dialog__footer) {
  padding: 16px 24px;
  border-top: 1px solid #f1f5f9;
}
</style>
