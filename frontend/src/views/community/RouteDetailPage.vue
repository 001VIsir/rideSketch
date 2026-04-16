<template>
  <div class="route-detail-page">
    <div class="detail-container">
      <div v-if="loading" class="loading-state">
        <el-skeleton :rows="10" animated />
      </div>

      <template v-else-if="routeDetail">
        <div class="back-bar">
          <el-button class="back-btn" @click="handleBack">
            <svg class="btn-icon" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <line x1="19" y1="12" x2="5" y2="12"/>
              <polyline points="12 19 5 12 12 5"/>
            </svg>
            返回
          </el-button>
          <div class="action-buttons" v-if="isAuthor">
            <el-button type="primary" class="edit-btn" @click="handleEdit">
              <svg class="btn-icon" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"/>
                <path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"/>
              </svg>
              编辑
            </el-button>
            <el-button type="danger" class="delete-btn" @click="handleDelete">
              <svg class="btn-icon" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <polyline points="3 6 5 6 21 6"/>
                <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"/>
              </svg>
              删除
            </el-button>
          </div>
        </div>

        <div class="route-card">
          <div class="route-header">
            <div class="route-title-section">
              <h1 class="route-title">{{ routeDetail?.title }}</h1>
              <div class="route-meta">
                <span class="city-tag">
                  <svg class="tag-icon" width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0 1 18 0z"/>
                    <circle cx="12" cy="10" r="3"/>
                  </svg>
                  {{ routeDetail?.city }}
                </span>
                <span class="author-info">
                  <svg class="tag-icon" width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/>
                    <circle cx="12" cy="7" r="4"/>
                  </svg>
                  {{ getAuthorName() }}
                </span>
                <span class="time-info">
                  <svg class="tag-icon" width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <circle cx="12" cy="12" r="10"/>
                    <polyline points="12 6 12 12 16 14"/>
                  </svg>
                  {{ routeDetail?.createTime }}
                </span>
              </div>
            </div>
          </div>

          <div class="route-stats">
            <div class="stat-item">
              <div class="stat-icon-wrapper">
                <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0 1 18 0z"/>
                  <circle cx="12" cy="10" r="3"/>
                </svg>
              </div>
              <span class="stat-label">距离</span>
              <span class="stat-value">{{ formatDistance(getRouteDistance()) }}</span>
            </div>
            <div class="stat-item">
              <div class="stat-icon-wrapper">
                <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <circle cx="12" cy="12" r="10"/>
                  <polyline points="12 6 12 12 16 14"/>
                </svg>
              </div>
              <span class="stat-label">时长</span>
              <span class="stat-value">{{ formatDuration(getRouteDuration()) }}</span>
            </div>
            <div class="stat-item">
              <div class="stat-icon-wrapper">
                <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z"/>
                </svg>
              </div>
              <span class="stat-label">点赞</span>
              <span class="stat-value">{{ routeDetail?.likes }}</span>
            </div>
            <div class="stat-item">
              <div class="stat-icon-wrapper">
                <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/>
                </svg>
              </div>
              <span class="stat-label">评论</span>
              <span class="stat-value">{{ routeDetail?.comments }}</span>
            </div>
          </div>

          <div class="route-description">
            <h3 class="section-title">路线描述</h3>
            <p class="description-text">{{ routeDetail?.description }}</p>
          </div>
        </div>

        <div class="interaction-card">
          <el-button
            :type="liked ? 'danger' : 'default'"
            class="like-button"
            @click="handleLike"
          >
            <svg class="btn-icon" width="18" height="18" viewBox="0 0 24 24" :fill="liked ? 'currentColor' : 'none'" stroke="currentColor" stroke-width="2">
              <path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z"/>
            </svg>
            {{ liked ? '已赞' : '点赞' }}
          </el-button>
        </div>

        <div class="comments-card">
          <div class="comments-header">
            <h3 class="section-title">评论 ({{ comments.length }})</h3>
          </div>

          <div v-if="commentsLoading" class="comments-loading">
            <el-skeleton :rows="3" animated />
          </div>
          <div v-else-if="comments.length > 0" class="comments-list">
            <div v-for="comment in comments" :key="comment.id" class="comment-item">
              <el-avatar :size="40" :src="comment.avatar">
                {{ comment.username?.charAt(0) }}
              </el-avatar>
              <div class="comment-body">
                <div class="comment-header">
                  <span class="comment-username">{{ comment.username }}</span>
                  <span class="comment-time">{{ comment.createTime }}</span>
                </div>
                <div class="comment-content">{{ comment.content }}</div>
              </div>
              <el-button
                v-if="comment.userId === currentUserId"
                text
                type="danger"
                size="small"
                class="delete-comment-btn"
                @click="handleDeleteComment(comment.id)"
              >
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <polyline points="3 6 5 6 21 6"/>
                  <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"/>
                </svg>
              </el-button>
            </div>
          </div>

          <el-empty v-else description="暂无评论，快来抢沙发吧" />

          <div class="comment-form">
            <el-input
              v-model="newComment"
              type="textarea"
              :rows="3"
              placeholder="发表你的评论..."
              class="comment-input"
            />
            <el-button type="primary" class="submit-comment-btn" :loading="posting" @click="handlePostComment">
              发表评论
            </el-button>
          </div>
        </div>
      </template>

      <el-empty v-else description="路线不存在" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getRouteDetail,
  likeRoute,
  unlikeRoute,
  getComments,
  postComment,
  deleteComment,
  updateRoute,
  deleteRoute,
  type RouteDetail,
  type CommentInfo,
} from '@/api/community'
import { getToken, getUserId } from '@/api/user'

const router = useRouter()
const routeParams = useRoute()

const loading = ref(true)
const routeId = ref<number>(0)
const routeDetail = ref<RouteDetail | null>(null)
const comments = ref<CommentInfo[]>([])
const commentsLoading = ref(false)
const liked = ref(false)
const newComment = ref('')
const posting = ref(false)

const currentUserId = computed(() => {
  return getUserId() || 0
})

const isAuthor = computed(() => {
  if (!routeDetail.value) return false
  return routeDetail.value.userId === currentUserId.value
})

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

function getRouteDistance(): number {
  if (!routeDetail.value) return 0
  return routeDetail.value.distance || 0
}

function getRouteDuration(): number {
  if (!routeDetail.value) return 0
  return routeDetail.value.duration || 0
}

function getAuthorName(): string {
  if (!routeDetail.value) return ''
  return routeDetail.value.authorName || '匿名用户'
}

async function loadRouteDetail() {
  loading.value = true
  try {
    const id = parseInt(routeParams.params.id as string)
    routeId.value = id
    routeDetail.value = await getRouteDetail(id)
    liked.value = !!routeDetail.value.liked
  } catch (error) {
    console.error('加载路线详情失败:', error)
    ElMessage.error('加载失败，请稍后重试')
  } finally {
    loading.value = false
  }
}

async function loadComments() {
  commentsLoading.value = true
  try {
    comments.value = await getComments(routeId.value)
  } catch (error) {
    console.error('加载评论失败:', error)
  } finally {
    commentsLoading.value = false
  }
}

async function handleLike() {
  const token = getToken()
  if (!token) {
    ElMessage.warning('请先登录')
    router.push('/login')
    return
  }

  try {
    if (liked.value) {
      const result = await unlikeRoute(routeId.value)
      liked.value = !!result.liked
      if (routeDetail.value) {
        routeDetail.value.likes = result.likes
      }
    } else {
      const result = await likeRoute(routeId.value)
      liked.value = !!result.liked
      if (routeDetail.value) {
        routeDetail.value.likes = result.likes
      }
    }
  } catch (error) {
    console.error('操作失败:', error)
    ElMessage.error('操作失败，请稍后重试')
  }
}

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
    if (routeDetail.value && typeof routeDetail.value.comments === 'number') {
      routeDetail.value.comments += 1
    }
  } catch (error) {
    console.error('评论失败:', error)
    ElMessage.error('评论失败，请稍后重试')
  } finally {
    posting.value = false
  }
}

async function handleDeleteComment(commentId: number) {
  try {
    await ElMessageBox.confirm('确定要删除这条评论吗？', '提示', {
      type: 'warning',
    })

    await deleteComment(commentId)
    comments.value = comments.value.filter(c => c.id !== commentId)
    ElMessage.success('删除成功')
    if (routeDetail.value && typeof routeDetail.value.comments === 'number' && routeDetail.value.comments > 0) {
      routeDetail.value.comments -= 1
    }
  } catch (error: any) {
    if (error !== 'cancel') {
      console.error('删除失败:', error)
      ElMessage.error('删除失败，请稍后重试')
    }
  }
}

function handleBack() {
  router.push('/community')
}

function handleEdit() {
  if (!routeDetail.value) return

  ElMessageBox.prompt('请输入新的路线标题', '编辑路线', {
    inputValue: routeDetail.value.title,
    inputPattern: /\S+/,
    inputErrorMessage: '标题不能为空',
  })
    .then(async (result: any) => {
      const value = String(result?.value ?? result)
      await updateRoute(routeId.value, {
        title: value,
      })
      ElMessage.success('编辑成功')
      await loadRouteDetail()
    })
    .catch(() => {})
}

async function handleDelete() {
  try {
    await ElMessageBox.confirm('确定要删除这条路线吗？', '提示', {
      type: 'warning',
    })

    await deleteRoute(routeId.value)
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
  padding: 24px;
  background: #f8fafc;
}

.detail-container {
  max-width: 800px;
  margin: 0 auto;
}

.loading-state {
  padding: 40px 20px;
  background: #ffffff;
  border-radius: 16px;
}

.back-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.back-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  border-radius: 10px !important;
  color: #64748b;
  border-color: #e2e8f0;
}

.back-btn:hover {
  color: #14b8a6;
  border-color: #14b8a6;
  background: #f0fdfa;
}

.btn-icon {
  width: 14px;
  height: 14px;
}

.action-buttons {
  display: flex;
  gap: 8px;
}

.edit-btn, .delete-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  border-radius: 8px !important;
}

.route-card {
  background: #ffffff;
  border-radius: 16px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);
  padding: 28px;
  margin-bottom: 16px;
}

.route-header {
  margin-bottom: 24px;
}

.route-title {
  font-size: 26px;
  font-weight: 700;
  color: #1e293b;
  margin: 0 0 12px 0;
  letter-spacing: -0.02em;
}

.route-meta {
  display: flex;
  align-items: center;
  gap: 16px;
  flex-wrap: wrap;
}

.city-tag, .author-info, .time-info {
  display: flex;
  align-items: center;
  gap: 5px;
  font-size: 13px;
  color: #64748b;
}

.tag-icon {
  color: #94a3b8;
}

.route-stats {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  padding: 24px;
  background: #f8fafc;
  border-radius: 12px;
  margin-bottom: 24px;
}

.stat-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
}

.stat-icon-wrapper {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 44px;
  height: 44px;
  background: #ffffff;
  border-radius: 12px;
  color: #14b8a6;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

.stat-label {
  font-size: 12px;
  color: #94a3b8;
}

.stat-value {
  font-size: 16px;
  font-weight: 600;
  color: #1e293b;
}

.section-title {
  font-size: 16px;
  font-weight: 600;
  color: #1e293b;
  margin: 0 0 12px 0;
}

.route-description {
  padding-top: 8px;
}

.description-text {
  font-size: 15px;
  color: #475569;
  line-height: 1.8;
  margin: 0;
}

.interaction-card {
  background: #ffffff;
  border-radius: 16px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);
  padding: 16px;
  margin-bottom: 16px;
  display: flex;
  justify-content: center;
}

.like-button {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 24px;
  border-radius: 10px !important;
  font-weight: 500;
}

.comments-card {
  background: #ffffff;
  border-radius: 16px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);
  padding: 24px;
}

.comments-header {
  margin-bottom: 20px;
  padding-bottom: 16px;
  border-bottom: 1px solid #f1f5f9;
}

.comments-list {
  margin-bottom: 20px;
}

.comment-item {
  display: flex;
  gap: 12px;
  padding: 16px 0;
  border-bottom: 1px solid #f1f5f9;
}

.comment-item:last-child {
  border-bottom: none;
}

.comment-body {
  flex: 1;
}

.comment-header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 6px;
}

.comment-username {
  font-size: 14px;
  font-weight: 600;
  color: #1e293b;
}

.comment-time {
  font-size: 12px;
  color: #94a3b8;
}

.comment-content {
  font-size: 14px;
  color: #475569;
  line-height: 1.6;
}

.delete-comment-btn {
  align-self: flex-start;
  opacity: 0.5;
}

.delete-comment-btn:hover {
  opacity: 1;
}

.comment-form {
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding-top: 20px;
  border-top: 1px solid #f1f5f9;
}

.comment-input :deep(.el-textarea__inner) {
  border-radius: 10px;
  resize: none;
}

.submit-comment-btn {
  align-self: flex-end;
  border-radius: 10px !important;
  padding: 10px 20px;
  background: linear-gradient(135deg, #14b8a6 0%, #0d9488 100%) !important;
  border: none !important;
}
</style>
