<template>
  <div class="pattern-page">
    <div class="pattern-content">
      <!-- 左侧：表单和结果 -->
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
                    :key="pattern.key"
                    class="pattern-item"
                    :class="{ selected: selectedPatternKey === pattern.key }"
                    @click="selectedPatternKey = pattern.key"
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
        </el-card>
      </div>

      <!-- 右侧：地图预览 -->
      <div v-if="result" class="map-container">
        <el-card class="map-card">
          <template #header>
            <div class="card-header">
              <span>地图预览</span>
              <el-button
                size="small"
                :disabled="!result?.routePaths?.length && !result?.routePath"
                @click="handleViewOnMap(result?.routePaths?.[0] ?? null)"
              >
                全屏查看
              </el-button>
            </div>
          </template>
          <div class="map-wrapper">
            <Amap
              ref="amapRef"
              :center="mapCenter"
              :zoom="14"
            />
          </div>
        </el-card>
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

// 地图中心点
const mapCenter = ref<[number, number]>([116.397428, 39.90923])

// 图案选项
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

// 表单数据
const selectedPatternKey = ref(defaultPatternOption.key)
const city = ref('')
const distance = ref(10)
const description = ref('')
const loading = ref(false)

// 结果
const result = ref<{
  pattern: string
  city: string
  totalDistance: number
  routePaths: PathInfo[]
  patternPointCount: number
  patternPoints?: Array<{ longitude: number; latitude: number; index: number }>
  routePath?: string
} | null>(null)

// 监听结果变化，绘制地图
watch(result, async (newResult) => {
  if (newResult && newResult.routePath) {
    await nextTick()
    // 延迟一下等待地图加载
    setTimeout(() => {
      drawPatternRoute(newResult)
    }, 500)
  }
}, { immediate: false })

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
    const selectedPattern = patternOptions.find(option => option.key === selectedPatternKey.value) ?? defaultPatternOption

    const data = await generatePatternRoute({
      pattern: selectedPattern.pattern,
      patternType: selectedPattern.patternType,
      city: city.value,
      // 距离滑块映射到后端稳定可控区间，避免图案过大/过小导致形状异常
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

    // 获取图案点
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

    // 设置地图中心点为第一个图案点或城市中心
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

// 在地图上绘制图案路线
async function drawPatternRoute(routeData: typeof result.value) {
  if (!routeData) return

  try {
    const map = getMapInstance()
    if (!map) {
      console.warn('地图实例未准备好')
      return
    }

    // 清除之前的路线
    clearRoute()

    // 如果有路线数据，使用路线数据
    let pathPoints: [number, number][] = []
    
    if (routeData.routePath) {
      // 解析路径坐标
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
    
    // 如果没有路线数据，使用图案点数据
    if (pathPoints.length === 0 && routeData.patternPoints && routeData.patternPoints.length > 0) {
      pathPoints = routeData.patternPoints.map(p => [p.longitude, p.latitude] as [number, number])
    }

    if (pathPoints.length > 0) {
      // 绘制路线
      await drawRoute(pathPoints, '#ff6b6b')

      // 绘制图案点作为标记
      if (routeData.patternPoints && routeData.patternPoints.length > 0) {
        routeData.patternPoints.forEach((point, index) => {
          addMarker([point.longitude, point.latitude], String(index + 1), `点 ${index + 1}`)
        })
      }

      // 调整地图视野
      map.setFitView()
    }
  } catch (error) {
    console.error('绘制路线失败:', error)
  }
}

// 在地图上查看
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
  padding: 40px 20px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.pattern-content {
  display: flex;
  gap: 20px;
  max-width: 1400px;
  margin: 0 auto;
}

.pattern-container {
  flex: 0 0 400px;
}

.map-container {
  flex: 1;
}

.map-card {
  height: 100%;
  min-height: 600px;
}

.map-card :deep(.el-card__body) {
  height: calc(100% - 55px);
}

.map-wrapper {
  width: 100%;
  height: 100%;
  min-height: 500px;
  border-radius: 8px;
  overflow: hidden;
}

.pattern-card {
  border-radius: 12px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
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
