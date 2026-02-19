import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { RouteMode, RoutePlanningResult, RoutePoint, PathInfo } from '@/api/route'

export const useRouteStore = defineStore('route', () => {
  // 状态
  const mode = ref<RouteMode>('riding')
  const origin = ref<RoutePoint | null>(null)
  const destination = ref<RoutePoint | null>(null)
  const waypoints = ref<RoutePoint[]>([])
  const routeResult = ref<RoutePlanningResult | null>(null)
  const selectedPath = ref<PathInfo | null>(null)
  const loading = ref(false)
  const activeTab = ref<'route' | 'ai' | 'pattern'>('route')

  // 计算属性
  const hasRoute = computed(() => !!routeResult.value && !!routeResult.value.route)
  const hasOrigin = computed(() => !!origin.value)
  const hasDestination = computed(() => !!destination.value)
  const canPlanRoute = computed(() => hasOrigin.value && hasDestination.value)

  // 格式化距离（米转公里）
  function formatDistance(meters: string | number): string {
    const m = typeof meters === 'string' ? parseInt(meters) : meters
    if (m >= 1000) {
      return (m / 1000).toFixed(2) + ' 公里'
    }
    return m + ' 米'
  }

  // 格式化时间（秒转分钟/小时）
  function formatDuration(seconds: string | number): string {
    const s = typeof seconds === 'string' ? parseInt(seconds) : seconds
    const minutes = Math.floor(s / 60)
    const hours = Math.floor(minutes / 60)
    const remainingMinutes = minutes % 60

    if (hours > 0) {
      return `${hours} 小时 ${remainingMinutes} 分钟`
    }
    return `${minutes} 分钟`
  }

  // Actions
  function setMode(newMode: RouteMode) {
    mode.value = newMode
  }

  function setOrigin(point: RoutePoint | null) {
    origin.value = point
  }

  function setDestination(point: RoutePoint | null) {
    destination.value = point
  }

  function setRouteResult(result: RoutePlanningResult | null) {
    routeResult.value = result
    if (result && result.route && result.route.paths && result.route.paths.length > 0) {
      selectedPath.value = result.route.paths[0] || null
    } else {
      selectedPath.value = null
    }
  }

  function setSelectedPath(path: PathInfo | null) {
    selectedPath.value = path
  }

  function addWaypoint(point: RoutePoint) {
    waypoints.value.push(point)
  }

  function removeWaypoint(index: number) {
    waypoints.value.splice(index, 1)
  }

  function clearWaypoints() {
    waypoints.value = []
  }

  function setLoading(isLoading: boolean) {
    loading.value = isLoading
  }

  function setActiveTab(tab: 'route' | 'ai' | 'pattern') {
    activeTab.value = tab
  }

  function clearRoute() {
    routeResult.value = null
    selectedPath.value = null
  }

  function clearAll() {
    origin.value = null
    destination.value = null
    waypoints.value = []
    routeResult.value = null
    selectedPath.value = null
    loading.value = false
  }

  // 导出坐标字符串
  function getOriginString(): string {
    if (!origin.value) return ''
    return `${origin.value.lng},${origin.value.lat}`
  }

  function getDestinationString(): string {
    if (!destination.value) return ''
    return `${destination.value.lng},${destination.value.lat}`
  }

  function getWaypointsString(): string {
    if (waypoints.value.length === 0) return ''
    return waypoints.value.map(p => `${p.lng},${p.lat}`).join('|')
  }

  return {
    // 状态
    mode,
    origin,
    destination,
    waypoints,
    routeResult,
    selectedPath,
    loading,
    activeTab,

    // 计算属性
    hasRoute,
    hasOrigin,
    hasDestination,
    canPlanRoute,

    // 方法
    formatDistance,
    formatDuration,

    // Actions
    setMode,
    setOrigin,
    setDestination,
    setRouteResult,
    setSelectedPath,
    addWaypoint,
    removeWaypoint,
    clearWaypoints,
    setLoading,
    setActiveTab,
    clearRoute,
    clearAll,
    getOriginString,
    getDestinationString,
    getWaypointsString,
  }
})
