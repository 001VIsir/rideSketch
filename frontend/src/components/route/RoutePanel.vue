<template>
  <div class="route-panel">
    <!-- 出行方式选择 -->
    <div class="mode-selector">
      <el-radio-group v-model="routeStore.mode" @change="handleModeChange">
        <el-radio-button value="riding">骑行</el-radio-button>
        <el-radio-button value="walking">步行</el-radio-button>
      </el-radio-group>
    </div>

    <!-- 起点输入 -->
    <div class="input-group">
      <div class="input-label start-label">
        <span class="label-icon">A</span>
        <span>起点</span>
      </div>
      <el-input
        v-model="originText"
        placeholder="请输入起点地址或点击地图拾取"
        clearable
        @focus="setInputFocus('origin')"
        @clear="handleClearOrigin"
      >
        <template #append>
          <el-button :icon="Location" @click="handlePickOrigin" />
        </template>
      </el-input>
    </div>

    <!-- 终点输入 -->
    <div class="input-group">
      <div class="input-label end-label">
        <span class="label-icon">B</span>
        <span>终点</span>
      </div>
      <el-input
        v-model="destinationText"
        placeholder="请输入终点地址或点击地图拾取"
        clearable
        @focus="setInputFocus('destination')"
        @clear="handleClearDestination"
      >
        <template #append>
          <el-button :icon="Location" @click="handlePickDestination" />
        </template>
      </el-input>
    </div>

    <!-- 途经点列表 -->
    <div v-if="routeStore.waypoints.length > 0" class="waypoints-section">
      <div class="section-header">
        <span>途经点 ({{ routeStore.waypoints.length }})</span>
        <el-button text type="danger" size="small" @click="handleClearWaypoints">
          清除
        </el-button>
      </div>
      <div class="waypoints-list">
        <div
          v-for="(wp, index) in routeStore.waypoints"
          :key="index"
          class="waypoint-item"
        >
          <span class="waypoint-index">{{ index + 1 }}</span>
          <span class="waypoint-name">{{ wp.name || `${wp.lng.toFixed(4)}, ${wp.lat.toFixed(4)}` }}</span>
          <el-button
            text
            type="danger"
            size="small"
            @click="routeStore.removeWaypoint(index)"
          >
            <el-icon><Delete /></el-icon>
          </el-button>
        </div>
      </div>
    </div>

    <!-- 规划按钮 -->
    <el-button
      type="primary"
      :loading="routeStore.loading"
      :disabled="!routeStore.canPlanRoute && (!originText.trim() || !destinationText.trim())"
      class="plan-button"
      @click="handlePlanRoute"
    >
      规划路线
    </el-button>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { Location, Delete } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { useRouteStore } from '@/stores/routeStore'
import { planRoute } from '@/api/route'
import { geocode as geocodeByMapApi } from '@/utils/amap'
import { geocode as geocodeByBackend } from '@/api/map'

const routeStore = useRouteStore()

// 输入框文本
const originText = ref('')
const destinationText = ref('')

// 当前输入焦点
const inputFocus = ref<'origin' | 'destination' | null>(null)

// 监听输入焦点变化，通知父组件
function setInputFocus(focus: 'origin' | 'destination') {
  inputFocus.value = focus
}

// 处理出行方式变化
function handleModeChange() {
  // 如果已有路线，重新规划
  if (routeStore.hasRoute && routeStore.canPlanRoute) {
    handlePlanRoute()
  }
}

// 处理拾取起点
function handlePickOrigin() {
  inputFocus.value = 'origin'
  ElMessage.info('请在地图上点击选择起点')
}

// 处理拾取终点
function handlePickDestination() {
  inputFocus.value = 'destination'
  ElMessage.info('请在地图上点击选择终点')
}

// 清除起点
function handleClearOrigin() {
  routeStore.setOrigin(null)
  originText.value = ''
}

// 清除终点
function handleClearDestination() {
  routeStore.setDestination(null)
  destinationText.value = ''
}

// 清除途经点
function handleClearWaypoints() {
  routeStore.clearWaypoints()
}

// 处理地图点击
async function handleMapClick(lng: number, lat: number, name?: string) {
  if (inputFocus.value === 'origin') {
    routeStore.setOrigin({ lng, lat, name })
    originText.value = name || `${lng.toFixed(4)}, ${lat.toFixed(4)}`
    inputFocus.value = null
  } else if (inputFocus.value === 'destination') {
    routeStore.setDestination({ lng, lat, name })
    destinationText.value = name || `${lng.toFixed(4)}, ${lat.toFixed(4)}`
    inputFocus.value = null
  }
}

// 处理添加途经点
function handleAddWaypoint(lng: number, lat: number, name?: string) {
  routeStore.addWaypoint({ lng, lat, name })
  ElMessage.success('途经点已添加')
}

// 规划路线
async function handlePlanRoute() {
  if (!routeStore.origin && originText.value.trim()) {
    const originPoint = await resolveAddressToPoint(originText.value.trim())
    if (originPoint) {
      routeStore.setOrigin({
        lng: originPoint.lng,
        lat: originPoint.lat,
        name: originText.value.trim(),
      })
    }
  }

  if (!routeStore.destination && destinationText.value.trim()) {
    const destinationPoint = await resolveAddressToPoint(destinationText.value.trim())
    if (destinationPoint) {
      routeStore.setDestination({
        lng: destinationPoint.lng,
        lat: destinationPoint.lat,
        name: destinationText.value.trim(),
      })
    }
  }

  if (!routeStore.canPlanRoute) {
    ElMessage.warning('请设置起点和终点')
    return
  }

  routeStore.setLoading(true)
  try {
    const result = await planRoute({
      mode: routeStore.mode,
      origin: routeStore.getOriginString(),
      destination: routeStore.getDestinationString(),
      waypoints: routeStore.getWaypointsString() || undefined,
    })

    if (result.status === '1' && result.route && result.route.paths) {
      routeStore.setRouteResult(result)
      ElMessage.success('路线规划成功')
    } else {
      ElMessage.error(result.info || '路线规划失败')
    }
  } catch (error: any) {
    console.error('路线规划失败:', error)
    ElMessage.error(error.response?.data?.message || '路线规划失败，请稍后重试')
  } finally {
    routeStore.setLoading(false)
  }
}

async function resolveAddressToPoint(address: string): Promise<{ lng: number; lat: number } | null> {
  try {
    const mapPoint = await geocodeByMapApi(address)
    if (mapPoint) {
      return mapPoint
    }
  } catch (error) {
    console.warn('前端地图地理编码失败，尝试后端接口回退:', error)
  }

  try {
    const result = await geocodeByBackend(address)
    const geocodeInfo = result?.geocodes
    if (!geocodeInfo) {
      return null
    }
    const lng = Number(geocodeInfo.lng)
    const lat = Number(geocodeInfo.lat)
    if (Number.isNaN(lng) || Number.isNaN(lat)) {
      return null
    }
    return { lng, lat }
  } catch (error) {
    console.warn('后端地理编码回退失败:', error)
    return null
  }
}

watch(
  () => routeStore.origin,
  (newOrigin) => {
    if (!newOrigin) return
    originText.value = newOrigin.name || `${newOrigin.lng.toFixed(4)}, ${newOrigin.lat.toFixed(4)}`
  }
)

watch(
  () => routeStore.destination,
  (newDestination) => {
    if (!newDestination) return
    destinationText.value = newDestination.name || `${newDestination.lng.toFixed(4)}, ${newDestination.lat.toFixed(4)}`
  }
)

// 暴露方法给父组件
defineExpose({
  handleMapClick,
  handleAddWaypoint,
  inputFocus,
})
</script>

<style scoped>
.route-panel {
  padding: 16px;
}

.mode-selector {
  margin-bottom: 16px;
  display: flex;
  justify-content: center;
}

.input-group {
  margin-bottom: 12px;
}

.input-label {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
  font-size: 14px;
  font-weight: 500;
}

.label-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
  border-radius: 50%;
  color: #fff;
  font-size: 12px;
  font-weight: bold;
}

.start-label .label-icon {
  background: #409eff;
}

.end-label .label-icon {
  background: #67c23a;
}

.waypoints-section {
  margin-bottom: 12px;
  padding: 12px;
  background: #f5f7fa;
  border-radius: 8px;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
  font-size: 14px;
  font-weight: 500;
}

.waypoints-list {
  max-height: 120px;
  overflow-y: auto;
}

.waypoint-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px;
  background: #fff;
  border-radius: 4px;
  margin-bottom: 4px;
}

.waypoint-index {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 20px;
  height: 20px;
  border-radius: 50%;
  background: #e6a23c;
  color: #fff;
  font-size: 12px;
}

.waypoint-name {
  flex: 1;
  font-size: 13px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.plan-button {
  width: 100%;
  margin-top: 8px;
}
</style>
