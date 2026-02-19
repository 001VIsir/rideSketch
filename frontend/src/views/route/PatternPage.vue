<template>
  <div class="pattern-page">
    <div class="pattern-container">
      <el-card class="pattern-card">
        <template #header>
          <div class="card-header">
            <span>图案路书生成</span>
          </div>
        </template>

        <!-- 图案选择 -->
        <div class="pattern-section">
          <el-form label-width="80px">
            <el-form-item label="选择图案">
              <div class="pattern-grid">
                <div
                  v-for="pattern in patternOptions"
                  :key="pattern.name"
                  class="pattern-item"
                  :class="{ selected: selectedPattern === pattern.name }"
                  @click="selectedPattern = pattern.name"
                >
                  <div class="pattern-icon">{{ pattern.icon }}</div>
                  <div class="pattern-name">{{ pattern.name }}</div>
                </div>
              </div>
            </el-form-item>

            <el-form-item label="城市">
              <el-input v-model="city" placeholder="请输入城市名称，如：北京" />
            </el-form-item>

            <el-form-item label="距离(km)">
              <el-slider v-model="distance" :min="1" :max="50" :marks="distanceMarks" />
            </el-form-item>

            <el-form-item label="路线描述">
              <el-input
                v-model="description"
                type="textarea"
                :rows="3"
                placeholder="描述你想要的骑行路线"
              />
            </el-form-item>

            <el-form-item>
              <el-button
                type="primary"
                :loading="loading"
                class="generate-button"
                @click="handleGenerate"
              >
                生成路书
              </el-button>
            </el-form-item>
          </el-form>
        </div>

        <!-- 结果展示 -->
        <div v-if="result" class="result-section">
          <el-divider content-position="center">生成结果</el-divider>

          <div class="result-info">
            <div class="info-item">
              <span class="info-label">图案</span>
              <span class="info-value">{{ result.pattern }}</span>
            </div>
            <div class="info-item">
              <span class="info-label">城市</span>
              <span class="info-value">{{ result.city }}</span>
            </div>
            <div class="info-item">
              <span class="info-label">总距离</span>
              <span class="info-value">{{ routeStore.formatDistance(result.distance) }}</span>
            </div>
          </div>

          <div v-if="result.route" class="route-details">
            <div class="details-header">路线详情</div>
            <div v-if="result.route.route?.paths?.length > 0" class="path-list">
              <div
                v-for="(path, index) in result.route.route.paths"
                :key="index"
                class="path-item"
              >
                <div class="path-info">
                  <span>距离: {{ routeStore.formatDistance(path.distance) }}</span>
                  <span>时间: {{ routeStore.formatDuration(path.duration) }}</span>
                </div>
                <el-button type="primary" size="small" @click="handleViewOnMap(path)">
                  在地图上查看
                </el-button>
              </div>
            </div>
          </div>
        </div>
      </el-card>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useRouteStore } from '@/stores/routeStore'
import { generatePatternRoute } from '@/api/route'

const router = useRouter()
const routeStore = useRouteStore()

// 图案选项
const patternOptions = [
  { name: '爱心', icon: '❤️' },
  { name: '星星', icon: '⭐' },
  { name: '圆形', icon: '⭕' },
  { name: '正方形', icon: '⬜' },
  { name: '三角形', icon: '🔺' },
  { name: '数字8', icon: '8️⃣' },
  { name: '字母M', icon: '🔤' },
  { name: '字母Z', icon: '🔤' },
]

// 表单数据
const selectedPattern = ref('爱心')
const city = ref('')
const distance = ref(10)
const description = ref('')
const loading = ref(false)

// 结果
const result = ref<{
  pattern: string
  city: string
  distance: number
  route: any
} | null>(null)

// 距离标记
const distanceMarks = {
  1: '1km',
  10: '10km',
  25: '25km',
  50: '50km',
}

// 生成路书
async function handleGenerate() {
  if (!city.value.trim()) {
    ElMessage.warning('请输入城市名称')
    return
  }

  loading.value = true
  try {
    const data = await generatePatternRoute({
      pattern: selectedPattern.value,
      city: city.value,
      distance: distance.value,
      description: description.value || undefined,
    })

    result.value = data
    ElMessage.success('图案路书生成成功')
  } catch (error: any) {
    console.error('生成失败:', error)
    ElMessage.error(error.response?.data?.message || '生成失败，请稍后重试')
  } finally {
    loading.value = false
  }
}

// 在地图上查看
function handleViewOnMap(path: any) {
  routeStore.setRouteResult(path)
  router.push('/map')
}
</script>

<style scoped>
.pattern-page {
  min-height: 100vh;
  padding: 40px 20px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.pattern-container {
  max-width: 800px;
  margin: 0 auto;
}

.pattern-card {
  border-radius: 12px;
}

.card-header {
  font-size: 18px;
  font-weight: 600;
}

.pattern-section {
  padding: 20px 0;
}

.pattern-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
}

.pattern-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 16px 8px;
  border: 2px solid #dcdfe6;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.3s;
}

.pattern-item:hover {
  border-color: #409eff;
}

.pattern-item.selected {
  border-color: #409eff;
  background-color: #ecf5ff;
}

.pattern-icon {
  font-size: 28px;
  margin-bottom: 8px;
}

.pattern-name {
  font-size: 13px;
  color: #606266;
}

.generate-button {
  width: 100%;
  margin-top: 16px;
}

.result-section {
  padding: 20px 0;
}

.result-info {
  display: flex;
  justify-content: space-around;
  padding: 20px;
  background: #f5f7fa;
  border-radius: 8px;
  margin-bottom: 20px;
}

.info-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
}

.info-label {
  font-size: 12px;
  color: #909399;
}

.info-value {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

.route-details {
  padding: 16px;
  background: #fff;
  border-radius: 8px;
  border: 1px solid #ebeef5;
}

.details-header {
  font-size: 14px;
  font-weight: 500;
  margin-bottom: 12px;
}

.path-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.path-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px;
  background: #f5f7fa;
  border-radius: 6px;
}

.path-info {
  display: flex;
  gap: 16px;
  font-size: 13px;
  color: #606266;
}
</style>
