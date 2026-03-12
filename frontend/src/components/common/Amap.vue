<template>
  <div ref="mapContainer" class="amap-container"></div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, watch } from 'vue'
import { createMap, destroyMap, getMapInstance, loadAMap } from '@/utils/amap'

const props = defineProps<{
  center?: [number, number]
  zoom?: number
  enableClick?: boolean
}>()

const emit = defineEmits<{
  (e: 'click', lng: number, lat: number): void
  (e: 'load'): void
}>()

const mapContainer = ref<HTMLElement>()

// 初始化地图
onMounted(async () => {
  if (!mapContainer.value) return

  try {
    await loadAMap()

    const map = await createMap(mapContainer.value, {
      center: props.center || [116.397428, 39.90923],
      zoom: props.zoom || 15,
    })

    // 如果启用点击事件
    if (props.enableClick !== false) {
      map.on('click', (e: any) => {
        emit('click', e.lnglat.getLng(), e.lnglat.getLat())
      })
    }

    emit('load')
  } catch (error) {
    console.error('地图加载失败:', error)
  }
})

// 监听中心点变化
watch(
  () => props.center,
  (newCenter) => {
    if (newCenter && getMapInstance()) {
      getMapInstance()?.setCenter(newCenter)
    }
  }
)

// 监听缩放级别变化
watch(
  () => props.zoom,
  (newZoom) => {
    if (newZoom && getMapInstance()) {
      getMapInstance()?.setZoom(newZoom)
    }
  }
)

// 组件卸载时销毁地图
onUnmounted(() => {
  destroyMap()
})

// 暴露方法给父组件
defineExpose({
  getMapInstance,
})
</script>

<style scoped>
.amap-container {
  width: 100%;
  height: 100%;
  min-height: 400px;
}
</style>
