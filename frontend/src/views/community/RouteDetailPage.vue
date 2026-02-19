<template>
  <div class="route-detail-page">
    <div class="detail-container">
      <!-- 加载状态 -->
      <div v-if="loading" class="loading-state">
        <el-skeleton :rows="10" animated />
      </div>

      <template v-else-if="routeDetail">
        <!-- 返回按钮 -->
        <div class="back-bar">
          <el-button :icon="Back" @click="handleBack">返回</el-button>
          <el-button
            v-if="isAuthor"
            type="primary"
            @click="handleEdit"
          >
            编辑
          </el-button>
          <el-button
            v-if="isAuthor"
            type="danger"
            @click="handleDelete"
          >
            删除
          </el-button>
        </div>

        <!-- 路线信息卡片 -->
        <el-card class="route-card">
          <template #header>
            <div class="card-header">
              <span class="route-title">{{ routeDetail?.title }}</span>
              <el-tag>{{ routeDetail?.city }}</el-tag>
            </div>
          </template>

          <div class="route-stats">
            <div class="stat-item">
              <span class="stat-label">距离</span>
              <span class="stat-value">{{ formatDistance(routeDetail?.distance || 0) }}</span>
            </div>
            <div class="stat-item">
              <span class="stat-label">时长</span>
              <span class="stat-value">{{ formatDuration(routeDetail?.duration || 0) }}</span>
            </div>
            <div class="stat-item">
              <span class="stat-label">作者</span>
              <span class="stat-value">{{ routeDetail?.authorName }}</span>
            </div>
            <div class="stat-item">
              <span class="stat-label">发布时间</span>
              <span class="stat-value">{{ routeDetail?.createTime }}</span>
            </div>
          </div>

          <el-divider />

          <div class="route-description">
            <h4>路线描述</h4>
            <p>{{ routeDetail?.description }}</p>
          </div>
        </el-card>

        <!-- 点赞和评论区域 -->
        <el-card class="interaction-card">
          <div class="interaction-bar">
            <el-button
              :type="liked ? 'danger' : 'default'"
              :icon="Star"
              @click="handleLike"
            >
              {{ liked ? '已赞' : '点赞' }} ({{ routeDetail?.likes }})
            </el-button>
          </div>
        </el-card>

        <!-- 评论区域 -->
        <el-card class="comments-card">
          <template #header>
            <div class="comments-header">
              <span>评论 ({{ comments.length }})</span>
            </div>
          </template>

          <!-- 评论列表 -->
          <div v-if="comments.length > 0" class="comments-list">
            <div v-for="comment in comments" :key="comment.id" class="comment-item">
              <div class="comment-header">
                <el-avatar :size="32" :src="comment.avatar">
                  {{ comment.username?.charAt(0) }}
                </el-avatar>
                <div class="comment-info">
                  <span class="comment-username">{{ comment.username }}</span>
                  <span class="comment-time">{{ comment.createTime }}</span>
                </div>
                <el-button
                  v-if="comment.userId === currentUserId"
                  text
                  type="danger"
                  size="small"
                  @click="handleDeleteComment(comment.id)"
                >
                  删除
                </el-button>
              </div>
              <div class="comment-content">{{ comment.content }}</div>
            </div>
          </div>

          <el-empty v-else description="暂无评论，快来抢沙发吧" />

          <!-- 评论输入框 -->
          <div class="comment-form">
            <el-input
              v-model="newComment"
              type="textarea"
              :rows="2"
              placeholder="发表你的评论..."
            />
            <el-button type="primary" :loading="posting" @click="handlePostComment">
              发表评论
            </el-button>
          </div>
        </el-card>
      </template>

      <!-- 加载失败 -->
      <el-empty v-else description="路线不存在" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Back, Star } from '@element-plus/icons-vue'
import { getRouteDetail, likeRoute, unlikeRoute, getComments, postComment, deleteComment, type RouteDetail, type CommentInfo } from '@/api/community'
import { getToken } from '@/api/user'

const router = useRouter()
const routeParams = useRoute()

// 状态
const loading = ref(true)
const routeId = ref<number>(0)
const routeDetail = ref<RouteDetail | null>(null)
const comments = ref<CommentInfo[]>([])
const liked = ref(false)
const newComment = ref('')
const posting = ref(false)

// 当前用户ID（从token解析，这里简化处理）
const currentUserId = computed(() => {
  // 实际应该从用户信息中获取
  return 0
})

// 是否是作者
const isAuthor = computed(() => {
  if (!routeDetail.value) return false
  // 实际应该比对当前用户ID
  return false
})

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

// 加载路线详情
async function loadRouteDetail() {
  loading.value = true
  try {
    const id = parseInt(routeParams.params.id as string)
    routeId.value = id
    routeDetail.value = await getRouteDetail(id)
  } catch (error) {
    console.error('加载路线详情失败:', error)
    ElMessage.error('加载失败，请稍后重试')
  } finally {
    loading.value = false
  }
}

// 加载评论
async function loadComments() {
  try {
    comments.value = await getComments(routeId.value)
  } catch (error) {
    console.error('加载评论失败:', error)
  }
}

// 点赞/取消点赞
async function handleLike() {
  const token = getToken()
  if (!token) {
    ElMessage.warning('请先登录')
    router.push('/login')
    return
  }

  try {
    if (liked.value) {
      await unlikeRoute(routeId.value)
      liked.value = false
      if (routeDetail.value) routeDetail.value.likes--
    } else {
      await likeRoute(routeId.value)
      liked.value = true
      if (routeDetail.value) routeDetail.value.likes++
    }
  } catch (error) {
    console.error('操作失败:', error)
    ElMessage.error('操作失败，请稍后重试')
  }
}

// 发表评论
async function handlePostComment() {
  const token = getToken()
  if (!token) {
    ElMessage.warning('请先登录')
    router.push('/login')
    return
  }

  if (!newComment.value.trim()) {
    ElMessage.warning('请输入评论内容')
    return
  }

  posting.value = true
  try {
    const comment = await postComment(routeId.value, newComment.value)
    comments.value.push(comment)
    newComment.value = ''
    ElMessage.success('评论成功')
    if (routeDetail.value) routeDetail.value.comments++
  } catch (error) {
    console.error('评论失败:', error)
    ElMessage.error('评论失败，请稍后重试')
  } finally {
    posting.value = false
  }
}

// 删除评论
async function handleDeleteComment(commentId: number) {
  try {
    await ElMessageBox.confirm('确定要删除这条评论吗？', '提示', {
      type: 'warning',
    })

    await deleteComment(routeId.value, commentId)
    comments.value = comments.value.filter(c => c.id !== commentId)
    ElMessage.success('删除成功')
    if (routeDetail.value) routeDetail.value.comments--
  } catch (error: any) {
    if (error !== 'cancel') {
      console.error('删除失败:', error)
      ElMessage.error('删除失败，请稍后重试')
    }
  }
}

// 返回
function handleBack() {
  router.push('/community')
}

// 编辑
function handleEdit() {
  ElMessage.info('编辑功能开发中')
}

// 删除
async function handleDelete() {
  try {
    await ElMessageBox.confirm('确定要删除这条路线吗？', '提示', {
      type: 'warning',
    })

    // 调用删除API
    ElMessage.success('删除成功')
    router.push('/community')
  } catch (error: any) {
    if (error !== 'cancel') {
      console.error('删除失败:', error)
      ElMessage.error('删除失败，请稍后重试')
    }
  }
}

onMounted(() => {
  loadRouteDetail()
  loadComments()
})
</script>

<style scoped>
.route-detail-page {
  min-height: 100vh;
  padding: 20px;
  background: #f5f7fa;
}

.detail-container {
  max-width: 800px;
  margin: 0 auto;
}

.loading-state {
  padding: 40px 20px;
}

.back-bar {
  margin-bottom: 16px;
}

.route-card {
  margin-bottom: 16px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.route-title {
  font-size: 18px;
  font-weight: 600;
}

.route-stats {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  padding: 16px 0;
}

.stat-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
}

.stat-label {
  font-size: 12px;
  color: #909399;
}

.stat-value {
  font-size: 14px;
  font-weight: 500;
  color: #303133;
}

.route-description h4 {
  font-size: 14px;
  font-weight: 500;
  margin-bottom: 8px;
  color: #303133;
}

.route-description p {
  font-size: 14px;
  color: #606266;
  line-height: 1.6;
}

.interaction-card {
  margin-bottom: 16px;
}

.interaction-bar {
  display: flex;
  justify-content: center;
}

.comments-card {
  margin-bottom: 16px;
}

.comments-header {
  font-size: 14px;
  font-weight: 500;
}

.comments-list {
  margin-bottom: 16px;
}

.comment-item {
  padding: 12px 0;
  border-bottom: 1px solid #ebeef5;
}

.comment-item:last-child {
  border-bottom: none;
}

.comment-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}

.comment-info {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.comment-username {
  font-size: 13px;
  font-weight: 500;
  color: #303133;
}

.comment-time {
  font-size: 12px;
  color: #909399;
}

.comment-content {
  font-size: 14px;
  color: #606266;
  line-height: 1.6;
}

.comment-form {
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding-top: 16px;
  border-top: 1px solid #ebeef5;
}

.comment-form .el-button {
  align-self: flex-end;
}
</style>
