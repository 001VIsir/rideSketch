<template>
  <div class="map-page">
    <div class="map-container">
      <!-- 顶部功能栏 -->
      <div class="top-bar">
        <!-- 搜索栏 -->
        <div class="search-bar">
          <el-input
            v-model="searchKeyword"
            placeholder="搜索地址/POI"
            class="search-input"
            @keyup.enter="handleSearch"
          >
            <template #append>
              <el-button :icon="Search" @click="handleSearch" />
            </template>
          </el-input>
        </div>

        <!-- 功能切换 -->
        <div class="function-tabs">
          <el-radio-group v-model="activeFunction" @change="handleFunctionChange">
            <el-radio-button value="search">搜索</el-radio-button>
            <el-radio-button value="route">路线规划</el-radio-button>
          </el-radio-group>
        </div>
      </div>

      <!-- 地图 -->
      <div class="map-wrapper">
        <Amap
          ref="amapRef"
          :center="mapStore.center"
          :zoom="mapStore.zoom"
          :enable-click="mapStore.isPickingMode || activeFunction === 'route'"
          @click="handleMapClick"
          @load="handleMapLoad"
        />

        <!-- 坐标显示面板 -->
        <div v-if="mapStore.selectedLocation && activeFunction === 'search'" class="location-panel">
          <el-card shadow="hover">
            <template #header>
              <div class="panel-header">
                <span>当前坐标</span>
                <el-button
                  text
                  type="danger"
                  @click="mapStore.clearSelectedLocation"
                >
                  清除
                </el-button>
              </div>
            </template>
            <div class="location-info">
              <p>
                <strong>经度:</strong> {{ mapStore.selectedLocation?.lng.toFixed(6) }}
              </p>
              <p>
                <strong>纬度:</strong> {{ mapStore.selectedLocation?.lat.toFixed(6) }}
              </p>
              <p v-if="mapStore.selectedAddress">
                <strong>地址:</strong> {{ mapStore.selectedAddress }}
              </p>
            </div>
          </el-card>
        </div>
      </div>

      <!-- 侧边栏 -->
      <div class="sidebar">
        <!-- 搜索功能面板 -->
        <template v-if="activeFunction === 'search'">
          <!-- 搜索结果面板 -->
          <div v-if="mapStore.searchResults.length > 0" class="search-results-panel">
            <el-card shadow="hover">
              <template #header>
                <div class="panel-header">
                  <span>搜索结果 ({{ mapStore.searchResults.length }})</span>
                  <el-button text @click="mapStore.clearSearchResults">
                    关闭
                  </el-button>
                </div>
              </template>
              <div class="results-list">
                <div
                  v-for="item in mapStore.searchResults"
                  :key="item.id"
                  class="result-item"
                  @click="handleSelectResult(item)"
                >
                  <div class="result-name">{{ item.name }}</div>
                  <div class="result-address">{{ item.address || item.province + item.city + item.district }}</div>
                </div>
              </div>
            </el-card>
          </div>
        </template>

        <!-- 路线规划功能面板 -->
        <template v-else-if="activeFunction === 'route'">
          <el-tabs v-model="routeTab" class="route-tabs">
            <el-tab-pane label="普通规划" name="normal">
              <RoutePanel ref="routePanelRef" />
            </el-tab-pane>
            <el-tab-pane label="AI规划" name="ai">
              <AIRoutePanel />
            </el-tab-pane>
            <el-tab-pane label="结果" name="result">
              <RouteResultPanel />
            </el-tab-pane>
          </el-tabs>
        </template>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'
import { Search } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { searchAddress } from '@/api/map'
import { useMapStore } from '@/stores/mapStore'
import { useRouteStore } from '@/stores/routeStore'
import { getMapInstance, drawRoute, clearRoute, addMarker, removeMarker } from '@/utils/amap'
import Amap from '@/components/common/Amap.vue'
import RoutePanel from '@/components/route/RoutePanel.vue'
import AIRoutePanel from '@/components/route/AIRoutePanel.vue'
import RouteResultPanel from '@/components/route/RouteResultPanel.vue'
import type { PoiInfo } from '@/types/map'
import type { PathInfo } from '@/api/route'

const mapStore = useMapStore()
const routeStore = useRouteStore()
const searchKeyword = ref('')

// 搜索地址 - 直接使用高德JS API
async function handleSearch() {
  if (!searchKeyword.value.trim()) {
    ElMessage.warning('请输入搜索关键词')
    return
  }

  mapStore.setSearching(true)
  try {
    // 直接使用前端的高德JS API进行搜索
    const { placeSearch: amapPlaceSearch, loadAMap } = await import('@/utils/amap')
    await loadAMap() // 确保地图已加载

    // 获取当前城市
    let city = '北京'
    try {
      const { reGeocode } = await import('@/utils/amap')
      const [lng, lat] = mapStore.center
      const address = await reGeocode(lng, lat)
      if (address) {
        const cityMatch = address.match(/^(北京市|天津市|上海市|重庆市|.*?市)/);
        const matchedCity = cityMatch?.[1]
        if (matchedCity) {
          city = matchedCity.replace('市', '')
        }
      }
    } catch (e) {
      console.warn('获取城市失败，使用默认北京')
    }

    // 使用高德JS API搜索
    let pois = await amapPlaceSearch(searchKeyword.value, city)

    // 如果前端JS API不可用，回退到后端搜索接口
    if (!pois || pois.length === 0) {
      const backendResult = await searchAddress(searchKeyword.value, city)
      if (backendResult?.status === '1' && backendResult.pois?.length > 0) {
        pois = backendResult.pois
      }
    }

    if (pois && pois.length > 0) {
      mapStore.setSearchResults(pois)
      // 优先选择名称包含关键词的结果
      const keyword = searchKeyword.value.toLowerCase()
      const firstPoi = pois.find(p =>
        p.name && p.name.toLowerCase().includes(keyword)
      ) || pois[0]

      if (firstPoi && firstPoi.longitude && firstPoi.latitude) {
        const lng = Number(firstPoi.longitude)
        const lat = Number(firstPoi.latitude)
        mapStore.setCenter(lng, lat)
        mapStore.setSelectedLocation(lng, lat, firstPoi.name)
        ElMessage.success(`已定位到: ${firstPoi.name}`)
      }
    } else {
      ElMessage.info('未找到相关结果')
      mapStore.clearSearchResults()
    }
  } catch (error) {
    console.error('搜索失败:', error)
    ElMessage.error('搜索失败，请稍后重试')
  } finally {
    mapStore.setSearching(false)
  }
}

// 处理地图点击
async function handleMapClick(lng: number, lat: number) {
  // 搜索模式
  if (mapStore.isPickingMode) {
    mapStore.setSelectedLocation(lng, lat)
    mapStore.isPickingMode = false

    // 获取地址信息
    try {
      const { reGeocode } = await import('@/utils/amap')
      const address = await reGeocode(lng, lat)
      if (address) {
        mapStore.selectedAddress = address
      }
    } catch (error) {
      console.error('获取地址失败:', error)
    }

    ElMessage.success(`已拾取坐标: ${lng.toFixed(6)}, ${lat.toFixed(6)}`)
    return
  }

  // 路线规划模式
  if (activeFunction.value === 'route') {
    await handleRouteMapClick(lng, lat)
  }
}

// 处理搜索结果选择
function handleSelectResult(item: PoiInfo) {
  const lng = parseFloat(item.longitude)
  const lat = parseFloat(item.latitude)
  mapStore.setCenter(lng, lat)
  mapStore.setSelectedLocation(lng, lat, item.name)
  mapStore.clearSearchResults()
}

// 地图加载完成
function handleMapLoad() {
  mapStore.setMapLoaded(true)
  console.log('地图加载完成')
}

// 路线相关状态
const activeFunction = ref<'search' | 'route'>('search')
const routeTab = ref('normal')
const routePanelRef = ref()
const markerMode = ref<'origin' | 'destination' | 'waypoint' | null>(null)

// 切换功能
function handleFunctionChange(value: string) {
  activeFunction.value = value as 'search' | 'route'
  if (value === 'search') {
    markerMode.value = null
  }
}

// 切换路线面板显示
function toggleRoutePanel() {
  if (activeFunction.value !== 'route') {
    activeFunction.value = 'route'
  }
}

// 清除路线
function handleClearRoute() {
  // 清除路线数据
  routeStore.clearAll()
  // 清除地图上的路线和标记
  clearRoute()
  ElMessage.success('路线已清除')
}

// 绘制路线
async function drawRouteOnMap(path: PathInfo) {
  const map = getMapInstance()
  if (!map || !path.path) return

  // 解析路径坐标
  const pathPoints: [number, number][] = []
  const coordStrings = path.path.split(';')
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

  // 绘制路线
  await drawRoute(pathPoints, '#409eff')

  // 如果有起点，添加标记
  if (routeStore.origin) {
    addMarker([routeStore.origin.lng, routeStore.origin.lat], 'A', '起点')
  }
  // 如果有终点，添加标记
  if (routeStore.destination) {
    addMarker([routeStore.destination.lng, routeStore.destination.lat], 'B', '终点')
  }

  // 调整地图视野以显示完整路线
  if (pathPoints.length > 0) {
    map.setFitView()
  }
}

// 处理地图点击（路线规划模式）
async function handleRouteMapClick(lng: number, lat: number) {
  if (activeFunction.value !== 'route') return

  // 获取地址信息
  let address = ''
  try {
    const { reGeocode } = await import('@/utils/amap')
    address = await reGeocode(lng, lat) || ''
  } catch (error) {
    console.error('获取地址失败:', error)
  }

  if (markerMode.value === 'origin') {
    routeStore.setOrigin({ lng, lat, name: address })
    ElMessage.success('起点已设置')
    markerMode.value = null
  } else if (markerMode.value === 'destination') {
    routeStore.setDestination({ lng, lat, name: address })
    ElMessage.success('终点已设置')
    markerMode.value = null
  } else if (markerMode.value === 'waypoint') {
    routeStore.addWaypoint({ name: address, lng, lat })
    ElMessage.success('途经点已添加')
  }
}

// 监听RoutePanel的焦点变化
watch(() => routePanelRef.value?.inputFocus, (focus) => {
  if (focus) {
    markerMode.value = focus
    ElMessage.info(focus === 'origin' ? '请在地图上点击选择起点' : '请在地图上点击选择终点')
  }
})

// 监听路线结果变化，自动绘制路线
watch(() => routeStore.selectedPath, (path) => {
  if (path) {
    drawRouteOnMap(path)
  }
})

onMounted(() => {
  // 设置默认中心点为北京
  mapStore.setCenter(116.397428, 39.90923)
})
</script>

<style scoped>
.map-page {
  height: 100vh;
  display: flex;
  flex-direction: column;
}

.map-container {
  flex: 1;
  position: relative;
  display: flex;
  flex-direction: column;
}

.search-bar {
  display: flex;
  gap: 10px;
  padding: 16px;
  background: #fff;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
  z-index: 10;
}

.search-input {
  max-width: 400px;
}

.map-wrapper {
  flex: 1;
  position: relative;
}

.location-panel {
  position: absolute;
  top: 16px;
  right: 16px;
  z-index: 10;
  width: 280px;
}

.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.location-info p {
  margin: 8px 0;
  font-size: 14px;
}

.location-info strong {
  color: #409eff;
}

.search-results {
  position: absolute;
  bottom: 16px;
  left: 16px;
  right: 16px;
  max-height: 300px;
  z-index: 10;
}

.results-list {
  max-height: 200px;
  overflow-y: auto;
}

.result-item {
  padding: 12px;
  border-bottom: 1px solid #eee;
  cursor: pointer;
  transition: background-color 0.2s;
}

.result-item:hover {
  background-color: #f5f7fa;
}

.result-item:last-child {
  border-bottom: none;
}

.result-name {
  font-size: 14px;
  font-weight: 500;
  color: #303133;
  margin-bottom: 4px;
}

.result-address {
  font-size: 12px;
  color: #909399;
}

/* 顶部功能栏 */
.top-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  background: #fff;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
  z-index: 10;
}

.function-tabs {
  display: flex;
  gap: 12px;
}

/* 侧边栏 */
.sidebar {
  position: absolute;
  top: 70px;
  left: 16px;
  bottom: 16px;
  width: 360px;
  z-index: 10;
  overflow-y: auto;
}

.search-results-panel {
  max-height: 400px;
}

.route-tabs {
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.route-tabs :deep(.el-tabs__content) {
  max-height: 500px;
  overflow-y: auto;
}
</style>
