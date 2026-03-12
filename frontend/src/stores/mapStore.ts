import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { PoiInfo } from '@/types/map'

export const useMapStore = defineStore('map', () => {
  // 地图中心点
  const center = ref<[number, number]>([116.397428, 39.90923])

  // 缩放级别
  const zoom = ref(15)

  // 当前选中的坐标
  const selectedLocation = ref<{ lng: number; lat: number } | null>(null)

  // 当前选中的地址
  const selectedAddress = ref<string>('')

  // 搜索结果列表
  const searchResults = ref<PoiInfo[]>([])

  // 是否正在搜索
  const isSearching = ref(false)

  // 地图是否加载完成
  const isMapLoaded = ref(false)

  // 坐标拾取模式
  const isPickingMode = ref(false)

  // 设置地图中心
  function setCenter(lng: number, lat: number) {
    center.value = [lng, lat]
    selectedLocation.value = { lng, lat }
  }

  // 设置缩放级别
  function setZoom(level: number) {
    zoom.value = level
  }

  // 设置选中的位置
  function setSelectedLocation(lng: number, lat: number, address?: string) {
    selectedLocation.value = { lng, lat }
    if (address) {
      selectedAddress.value = address
    }
  }

  // 清除选中位置
  function clearSelectedLocation() {
    selectedLocation.value = null
    selectedAddress.value = ''
  }

  // 设置搜索结果
  function setSearchResults(results: PoiInfo[]) {
    searchResults.value = results
  }

  // 清除搜索结果
  function clearSearchResults() {
    searchResults.value = []
  }

  // 设置搜索状态
  function setSearching(status: boolean) {
    isSearching.value = status
  }

  // 设置地图加载状态
  function setMapLoaded(status: boolean) {
    isMapLoaded.value = status
  }

  // 切换坐标拾取模式
  function togglePickingMode() {
    isPickingMode.value = !isPickingMode.value
  }

  // 格式化坐标显示
  const formattedLocation = computed(() => {
    if (!selectedLocation.value) return ''
    const { lng, lat } = selectedLocation.value
    return `经度: ${lng.toFixed(6)}, 纬度: ${lat.toFixed(6)}`
  })

  return {
    center,
    zoom,
    selectedLocation,
    selectedAddress,
    searchResults,
    isSearching,
    isMapLoaded,
    isPickingMode,
    formattedLocation,
    setCenter,
    setZoom,
    setSelectedLocation,
    clearSelectedLocation,
    setSearchResults,
    clearSearchResults,
    setSearching,
    setMapLoaded,
    togglePickingMode,
  }
})
