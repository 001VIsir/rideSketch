<template>
  <div class="pattern-page">
    <div class="pattern-content">
      <!-- 左侧：表单和结果 -->
      <div class="pattern-container">
        <div class="pattern-card">
          <div class="card-header-section">
            <div class="card-icon">
              <svg width="24" height="24" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                <polygon points="12 2 2 7 12 12 22 7 12 2" fill="#14b8a6"/>
                <polyline points="2 17 12 22 22 17" stroke="#14b8a6" stroke-width="2" fill="none"/>
                <polyline points="2 12 12 17 22 12" stroke="#14b8a6" stroke-width="2" fill="none"/>
              </svg>
            </div>
            <div class="card-title-group">
              <h2 class="card-title">图案路书生成</h2>
              <p class="card-subtitle">创造独特的骑行轨迹</p>
            </div>
          </div>

          <!-- 图案选择 -->
          <div class="pattern-section">
            <div class="section-label">选择图案</div>
            <div class="pattern-grid">
              <div
                v-for="pattern in patternOptions"
                :key="pattern.key"
                class="pattern-item"
                :class="{ selected: selectedPatternKey === pattern.key }"
                @click="selectedPatternKey = pattern.key"
              >
                <div class="pattern-icon">{{ pattern.icon }}</div>
                <div class="pattern-name">{{ pattern.name }}</div>
              </div>
            </div>

            <div class="form-row">
              <div class="form-item">
                <label class="form-label">城市</label>
                <el-input v-model="city" placeholder="如：北京" />
              </div>
            </div>

            <div class="form-row">
              <div class="form-item">
                <label class="form-label">骑行距离</label>
                <div class="slider-wrapper">
                  <el-slider v-model="distance" :min="1" :max="50" :marks="distanceMarks" />
                  <span class="slider-value">{{ distance }} km</span>
                </div>
              </div>
            </div>

            <div class="form-row">
              <div class="form-item">
                <label class="form-label">路线描述</label>
                <el-input
                  v-model="description"
                  type="textarea"
                  :rows="2"
                  placeholder="描述你想要的骑行路线"
                />
              </div>
            </div>

            <el-button
              type="primary"
              :loading="loading"
              class="generate-button"
              @click="handleGenerate"
            >
              <svg v-if="!loading" class="btn-icon" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <polygon points="13 2 3 14 12 14 11 22 21 10 12 10 13 2"/>
              </svg>
              生成路书
            </el-button>
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
                <span class="info-value">{{ routeStore.formatDistance(result.totalDistance) }}</span>
              </div>
              <div class="info-item">
                <span class="info-label">图案点数</span>
                <span class="info-value">{{ result.patternPointCount }}</span>
              </div>
            </div>

            <div v-if="result.routePaths.length > 0" class="route-details">
              <div class="details-header">路线详情</div>
              <div class="path-list">
                <div
                  v-for="(path, index) in result.routePaths"
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
        </div>
      </div>

      <!-- 右侧：地图预览 -->
      <div v-if="result" class="map-container">
        <div class="map-card">
          <div class="map-card-header">
            <span class="map-title">地图预览</span>
            <el-button
              size="small"
              :disabled="!result?.routePaths?.length && !result?.routePath"
              @click="handleViewOnMap(result?.routePaths?.[0] ?? null)"
            >
              全屏查看
            </el-button>
          </div>
          <div class="map-wrapper">
            <Amap
              ref="amapRef"
              :center="mapCenter"
              :zoom="14"
            />
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useRouteStore } from '@/stores/routeStore'
import { generatePatternRoute, type PathInfo } from '@/api/route'
import { getMapInstance, drawRoute, clearRoute, addMarker } from '@/utils/amap'
import Amap from '@/components/common/Amap.vue'

const router = useRouter()
const routeStore = useRouteStore()
const amapRef = ref()

const mapCenter = ref<[number, number]>([116.397428, 39.90923])

const patternOptions = [
  { key: 'heart', name: '爱心', icon: '❤️', pattern: '心形', patternType: 'shape' as const },
  { key: 'star', name: '星星', icon: '⭐', pattern: '五角星', patternType: 'shape' as const },
  { key: 'circle', name: '圆形', icon: '⭕', pattern: '圆形', patternType: 'shape' as const },
  { key: 'square', name: '正方形', icon: '⬜', pattern: '正方形', patternType: 'shape' as const },
  { key: 'triangle', name: '三角形', icon: '🔺', pattern: '三角形', patternType: 'shape' as const },
  { key: 'digit-8', name: '数字8', icon: '8️⃣', pattern: '8', patternType: 'text' as const },
  { key: 'letter-m', name: '字母M', icon: '🔤', pattern: 'M', patternType: 'text' as const },
  { key: 'letter-z', name: '字母Z', icon: '🔤', pattern: 'Z', patternType: 'text' as const },
]

const defaultPatternOption = patternOptions[0]!

const selectedPatternKey = ref(defaultPatternOption.key)
const city = ref('')
const distance = ref(10)
const description = ref('')
const loading = ref(false)

const result = ref<{
  pattern: string
  city: string
  totalDistance: number
  routePaths: PathInfo[]
  patternPointCount: number
  patternPoints?: Array<{ longitude: number; latitude: number; index: number }>
  routePath?: string
} | null>(null)

watch(result, async (newResult) => {
  if (newResult && newResult.routePath) {
    await nextTick()
    setTimeout(() => {
      drawPatternRoute(newResult)
    }, 500)
  }
}, { immediate: false })

const distanceMarks = {
  1: '1km',
  10: '10km',
  25: '25km',
  50: '50km',
}

async function handleGenerate() {
  if (!city.value.trim()) {
    ElMessage.warning('请输入城市名称')
    return
  }

  loading.value = true
  try {
    const selectedPattern = patternOptions.find(option => option.key === selectedPatternKey.value) ?? defaultPatternOption

    const data = await generatePatternRoute({
      pattern: selectedPattern.pattern,
      patternType: selectedPattern.patternType,
      city: city.value,
      scale: Math.max(0.005, Math.min(0.2, distance.value / 200)),
      description: description.value || undefined,
    })

    if (data.status !== '1') {
      ElMessage.error(data.info || '图案路书生成失败')
      return
    }

    const totalDistance = data.quantifiedData?.totalDistance
      ?? (data.quantifiedData?.totalDistanceKm ? Number(data.quantifiedData.totalDistanceKm) * 1000 : 0)

    const routePaths = data.routeData?.route?.paths || []
    const patternPointCount = data.patternPoints?.length || 0

    const patternPoints = data.patternPoints || []
    const routePath = data.routePath || ''

    result.value = {
      pattern: data.pattern || selectedPattern.name,
      city: data.city || city.value,
      totalDistance,
      routePaths,
      patternPointCount,
      patternPoints,
      routePath,
    }

    const firstPoint = patternPoints[0]
    if (firstPoint) {
      mapCenter.value = [firstPoint.longitude, firstPoint.latitude]
    }

    ElMessage.success('图案路书生成成功')
  } catch (error: any) {
    console.error('生成失败:', error)
    ElMessage.error(error.response?.data?.message || '生成失败，请稍后重试')
  } finally {
    loading.value = false
  }
}

async function drawPatternRoute(routeData: typeof result.value) {
  if (!routeData) return

  try {
    const map = getMapInstance()
    if (!map) {
      console.warn('地图实例未准备好')
      return
    }

    clearRoute()

    let pathPoints: [number, number][] = []

    if (routeData.routePath) {
      const coordStrings = routeData.routePath.split(';')
      for (const coord of coordStrings) {
        const parts = coord.split(',')
        if (parts.length >= 2) {
          const lng = Number(parts[0])
          const lat = Number(parts[1])
          if (!isNaN(lng) && !isNaN(lat)) {
            pathPoints.push([lng, lat])
          }
        }
      }
    }

    if (pathPoints.length === 0 && routeData.patternPoints && routeData.patternPoints.length > 0) {
      pathPoints = routeData.patternPoints.map(p => [p.longitude, p.latitude] as [number, number])
    }

    if (pathPoints.length > 0) {
      await drawRoute(pathPoints, '#14b8a6')

      if (routeData.patternPoints && routeData.patternPoints.length > 0) {
        routeData.patternPoints.forEach((point, index) => {
          addMarker([point.longitude, point.latitude], String(index + 1), `点 ${index + 1}`)
        })
      }

      map.setFitView()
    }
  } catch (error) {
    console.error('绘制路线失败:', error)
  }
}

function handleViewOnMap(path: PathInfo | null) {
  const effectivePath = path ?? (result.value?.routePath
    ? {
        distance: String(Math.round(result.value.totalDistance || 0)),
        duration: '0',
        strategy: 'pattern',
        steps: [],
        path: result.value.routePath,
      }
    : null)

  if (!effectivePath) {
    ElMessage.warning('暂无可查看路线')
    return
  }

  routeStore.setRouteResult({
    status: '1',
    info: 'OK',
    route: {
      origin: '',
      destination: '',
      waypoints: '',
      paths: [effectivePath],
    },
  })
  router.push('/map')
}
</script>

<style scoped>
.pattern-page {
  min-height: 100vh;
  padding: 24px;
  background: #f8fafc;
}

.pattern-content {
  display: flex;
  gap: 24px;
  max-width: 1400px;
  margin: 0 auto;
}

.pattern-container {
  flex: 0 0 420px;
}

.pattern-card {
  background: #ffffff;
  border-radius: 20px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.06);
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
}

.card-title-group {
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

.pattern-section {
  padding: 24px;
}

.section-label {
  font-size: 14px;
  font-weight: 500;
  color: #475569;
  margin-bottom: 12px;
}

.pattern-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 10px;
  margin-bottom: 20px;
}

.pattern-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 14px 8px;
  border: 2px solid #f1f5f9;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.pattern-item:hover {
  border-color: #14b8a6;
  background: #f0fdfa;
}

.pattern-item.selected {
  border-color: #14b8a6;
  background: #f0fdfa;
}

.pattern-icon {
  font-size: 24px;
  margin-bottom: 6px;
}

.pattern-name {
  font-size: 11px;
  color: #64748b;
}

.form-row {
  margin-bottom: 16px;
}

.form-item {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.form-label {
  font-size: 14px;
  font-weight: 500;
  color: #475569;
}

.slider-wrapper {
  display: flex;
  align-items: center;
  gap: 12px;
}

.slider-wrapper :deep(.el-slider) {
  flex: 1;
}

.slider-value {
  font-size: 14px;
  font-weight: 600;
  color: #14b8a6;
  min-width: 50px;
}

.generate-button {
  width: 100%;
  height: 48px;
  border-radius: 12px !important;
  font-size: 16px;
  font-weight: 600;
  background: linear-gradient(135deg, #14b8a6 0%, #0d9488 100%) !important;
  border: none !important;
  box-shadow: 0 4px 12px rgba(20, 184, 166, 0.25);
  transition: all 0.3s ease;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}

.generate-button:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(20, 184, 166, 0.35);
}

.btn-icon {
  width: 18px;
  height: 18px;
}

.result-section {
  padding: 0 24px 24px;
}

.result-section :deep(.el-divider) {
  margin: 0 -24px 24px;
}

.result-info {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
  padding: 20px;
  background: #f8fafc;
  border-radius: 12px;
  margin-bottom: 20px;
}

.info-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
}

.info-label {
  font-size: 12px;
  color: #94a3b8;
}

.info-value {
  font-size: 15px;
  font-weight: 600;
  color: #1e293b;
}

.route-details {
  padding: 16px;
  background: #ffffff;
  border-radius: 12px;
  border: 1px solid #f1f5f9;
}

.details-header {
  font-size: 14px;
  font-weight: 500;
  color: #475569;
  margin-bottom: 12px;
}

.path-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.path-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px;
  background: #f8fafc;
  border-radius: 8px;
}

.path-info {
  display: flex;
  gap: 16px;
  font-size: 13px;
  color: #64748b;
}

.map-container {
  flex: 1;
}

.map-card {
  background: #ffffff;
  border-radius: 20px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.06);
  overflow: hidden;
  height: 100%;
  min-height: 600px;
  display: flex;
  flex-direction: column;
}

.map-card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  border-bottom: 1px solid #f1f5f9;
}

.map-title {
  font-size: 16px;
  font-weight: 600;
  color: #1e293b;
}

.map-wrapper {
  flex: 1;
  min-height: 500px;
}
</style>
