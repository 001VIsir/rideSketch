<template>
  <div class="map-page">
    <div class="map-container">
      <!-- 顶部功能栏 -->
      <div class="top-bar">
        <div class="search-section">
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

          <div class="function-tabs">
            <el-radio-group v-model="activeFunction" @change="handleFunctionChange">
              <el-radio-button value="search">搜索</el-radio-button>
              <el-radio-button value="route">路线规划</el-radio-button>
            </el-radio-group>
          </div>
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

async function handleSearch() {
  if (!searchKeyword.value.trim()) {
    ElMessage.warning('请输入搜索关键词')
    return
  }

  mapStore.setSearching(true)
  try {
    const { placeSearch: amapPlaceSearch, loadAMap } = await import('@/utils/amap')
    await loadAMap()

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

    let pois = await amapPlaceSearch(searchKeyword.value, city)

    if (!pois || pois.length === 0) {
      const backendResult = await searchAddress(searchKeyword.value, city)
      if (backendResult?.status === '1' && backendResult.pois?.length > 0) {
        pois = backendResult.pois
      }
    }

    if (pois && pois.length > 0) {
      mapStore.setSearchResults(pois)
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

async function handleMapClick(lng: number, lat: number) {
  if (mapStore.isPickingMode) {
    mapStore.setSelectedLocation(lng, lat)
    mapStore.isPickingMode = false

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

  if (activeFunction.value === 'route') {
    await handleRouteMapClick(lng, lat)
  }
}

function handleSelectResult(item: PoiInfo) {
  const lng = parseFloat(item.longitude)
  const lat = parseFloat(item.latitude)
  mapStore.setCenter(lng, lat)
  mapStore.setSelectedLocation(lng, lat, item.name)
  mapStore.clearSearchResults()
}

function handleMapLoad() {
  mapStore.setMapLoaded(true)
}

const activeFunction = ref<'search' | 'route'>('search')
const routeTab = ref('normal')
const routePanelRef = ref()
const markerMode = ref<'origin' | 'destination' | 'waypoint' | null>(null)

function handleFunctionChange(value: string) {
  activeFunction.value = value as 'search' | 'route'
  if (value === 'search') {
    markerMode.value = null
  }
}

function toggleRoutePanel() {
  if (activeFunction.value !== 'route') {
    activeFunction.value = 'route'
  }
}

function handleClearRoute() {
  routeStore.clearAll()
  clearRoute()
  ElMessage.success('路线已清除')
}

async function drawRouteOnMap(path: PathInfo) {
  const map = getMapInstance()
  if (!map || !path.path) return

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

  await drawRoute(pathPoints, '#14b8a6')

  if (routeStore.origin) {
    addMarker([routeStore.origin.lng, routeStore.origin.lat], 'A', '起点')
  }
  if (routeStore.destination) {
    addMarker([routeStore.destination.lng, routeStore.destination.lat], 'B', '终点')
  }

  if (pathPoints.length > 0) {
    map.setFitView()
  }
}

async function handleRouteMapClick(lng: number, lat: number) {
  if (activeFunction.value !== 'route') return

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

watch(() => routePanelRef.value?.inputFocus, (focus) => {
  if (focus) {
    markerMode.value = focus
    ElMessage.info(focus === 'origin' ? '请在地图上点击选择起点' : '请在地图上点击选择终点')
  }
})

watch(() => routeStore.selectedPath, (path) => {
  if (path) {
    drawRouteOnMap(path)
  }
})

onMounted(() => {
  mapStore.setCenter(116.397428, 39.90923)
})
</script>

<style scoped>
.map-page {
  height: 100vh;
  display: flex;
  flex-direction: column;
  background: #f8fafc;
}

.map-container {
  flex: 1;
  position: relative;
  display: flex;
  flex-direction: column;
}

.top-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 24px;
  background: #ffffff;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
  z-index: 10;
}

.search-section {
  display: flex;
  align-items: center;
  gap: 16px;
}

.search-bar {
  display: flex;
  gap: 10px;
}

.search-input {
  width: 320px;
}

.search-input :deep(.el-input__wrapper) {
  border-radius: 10px 0 0 10px !important;
}

.search-input :deep(.el-input-group__append) {
  border-radius: 0 10px 10px 0 !important;
  background: #14b8a6;
  border-color: #14b8a6;
  color: white;
}

.function-tabs :deep(.el-radio-button__inner) {
  border-radius: 8px !important;
}

.function-tabs :deep(.el-radio-button:first-child .el-radio-button__inner) {
  border-radius: 8px !important;
}

.function-tabs :deep(.el-radio-button:last-child .el-radio-button__inner) {
  border-radius: 8px !important;
}

.map-wrapper {
  flex: 1;
  position: relative;
  background: #f1f5f9;
}

.location-panel {
  position: absolute;
  top: 16px;
  right: 16px;
  z-index: 10;
  width: 280px;
}

.location-panel :deep(.el-card) {
  border-radius: 12px;
  border: none;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.08);
}

.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-weight: 500;
}

.location-info p {
  margin: 8px 0;
  font-size: 14px;
  color: #475569;
}

.location-info strong {
  color: #14b8a6;
  font-weight: 500;
}

.results-list {
  max-height: 200px;
  overflow-y: auto;
}

.result-item {
  padding: 12px;
  border-bottom: 1px solid #f1f5f9;
  cursor: pointer;
  transition: background-color 0.2s;
}

.result-item:hover {
  background-color: #f0fdfa;
}

.result-item:last-child {
  border-bottom: none;
}

.result-name {
  font-size: 14px;
  font-weight: 500;
  color: #1e293b;
  margin-bottom: 4px;
}

.result-address {
  font-size: 12px;
  color: #94a3b8;
}

.sidebar {
  position: absolute;
  top: 80px;
  left: 24px;
  bottom: 24px;
  width: 360px;
  z-index: 10;
  overflow-y: auto;
}

.search-results-panel :deep(.el-card) {
  border-radius: 12px;
  border: none;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.08);
}

.route-tabs {
  background: #ffffff;
  border-radius: 12px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.08);
  overflow: hidden;
}

.route-tabs :deep(.el-tabs__header) {
  margin: 0;
}

.route-tabs :deep(.el-tabs__nav-wrap::after) {
  height: 1px;
}

.route-tabs :deep(.el-tabs__content) {
  max-height: 500px;
  overflow-y: auto;
  padding: 16px;
}
</style>
