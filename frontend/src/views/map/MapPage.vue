<template>
  <div class="map-page">
    <div class="map-container">
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
        <el-button
          :type="mapStore.isPickingMode ? 'success' : 'default'"
          @click="mapStore.togglePickingMode"
        >
          {{ mapStore.isPickingMode ? '退出拾取' : '拾取坐标' }}
        </el-button>
      </div>

      <!-- 地图 -->
      <div class="map-wrapper">
        <Amap
          :center="mapStore.center"
          :zoom="mapStore.zoom"
          :enable-click="mapStore.isPickingMode"
          @click="handleMapClick"
          @load="handleMapLoad"
        />

        <!-- 坐标显示面板 -->
        <div v-if="mapStore.selectedLocation" class="location-panel">
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

      <!-- 搜索结果面板 -->
      <div v-if="mapStore.searchResults.length > 0" class="search-results">
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
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { Search } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { searchAddress } from '@/api/map'
import { useMapStore } from '@/stores/mapStore'
import Amap from '@/components/common/Amap.vue'
import type { PoiInfo } from '@/types/map'

const mapStore = useMapStore()
const searchKeyword = ref('')

// 搜索地址
async function handleSearch() {
  if (!searchKeyword.value.trim()) {
    ElMessage.warning('请输入搜索关键词')
    return
  }

  mapStore.setSearching(true)
  try {
    const result = await searchAddress(searchKeyword.value)
    const pois = result.pois || []
    if (result.status === '1' && pois.length > 0) {
      mapStore.setSearchResults(pois)
      // 定位到第一个结果
      const firstPoi = pois[0]
      if (firstPoi) {
        const lng = parseFloat(firstPoi.longitude)
        const lat = parseFloat(firstPoi.latitude)
        mapStore.setCenter(lng, lat)
        mapStore.setSelectedLocation(lng, lat, firstPoi.name)
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
  if (!mapStore.isPickingMode) return

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
</style>
