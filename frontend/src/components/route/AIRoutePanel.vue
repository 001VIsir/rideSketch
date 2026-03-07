<template>
  <div class="ai-route-panel">
    <!-- AI输入区域 -->
    <div class="ai-input-section">
      <div class="input-header">
        <span class="ai-icon">🤖</span>
        <span>AI智能路线规划</span>
      </div>
      <el-input
        v-model="aiDescription"
        type="textarea"
        :rows="3"
        placeholder="请描述您想要的骑行路线，例如：从天安门出发，经过故宫、中山公园，最后到天坛公园"
        resize="none"
      />
      <div class="input-tips">
        <span>提示：描述您想要的路线特点、途经景点或地点</span>
      </div>
    </div>

    <!-- 城市选择 -->
    <div class="city-section">
      <el-form-item label="所在城市">
        <el-input v-model="city" placeholder="请输入城市名称，如：北京" />
      </el-form-item>
    </div>

    <!-- 规划按钮 -->
    <el-button
      type="primary"
      :loading="routeStore.loading"
      class="plan-button"
      @click="handleAIRoutePlanning"
    >
      开始AI规划
    </el-button>

    <!-- AI规划结果 -->
    <div v-if="aiResult" class="ai-result">
      <div class="result-header">
        <span>AI规划结果</span>
      </div>
      <div class="result-content">
        <p>{{ aiResult.analysis }}</p>
        <p v-if="aiResult.recommendedWaypoints.length > 0">
          推荐途经点：{{ aiResult.recommendedWaypoints.map((item) => item.name).join('、') }}
        </p>
      </div>
      <div class="result-actions">
        <el-button type="primary" size="small" @click="handleApplyResult">
          应用路线
        </el-button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useRouteStore } from '@/stores/routeStore'
import { planAIRoute, planRoute } from '@/api/route'

const routeStore = useRouteStore()

// 输入数据
const aiDescription = ref('')
const city = ref('')

// AI结果
const aiResult = ref<{
  analysis: string
  recommendedWaypoints: Array<{ name: string; location: string }>
} | null>(null)

function parseCoord(value?: string): { lng: number; lat: number } | null {
  if (!value || !value.includes(',')) {
    return null
  }
  const [lngText, latText] = value.split(',')
  const lng = Number(lngText)
  const lat = Number(latText)
  if (Number.isNaN(lng) || Number.isNaN(lat)) {
    return null
  }
  return { lng, lat }
}

// AI路线规划
async function handleAIRoutePlanning() {
  if (!aiDescription.value.trim()) {
    ElMessage.warning('请输入路线描述')
    return
  }

  if (!city.value.trim()) {
    ElMessage.warning('请输入城市名称')
    return
  }

  routeStore.setLoading(true)
  try {
    const result = await planAIRoute({
      description: aiDescription.value,
      city: city.value,
      mode: routeStore.mode,
    })

    if (result.status !== '1') {
      ElMessage.error(result.info || 'AI路线规划失败')
      return
    }

    aiResult.value = {
      analysis: result.analysis || result.info || '',
      recommendedWaypoints: result.recommendedWaypoints || [],
    }
    ElMessage.success('AI路线规划完成')

    if (result.origin && result.destination) {
      const waypoints = (result.recommendedWaypoints || [])
        .map((item) => item.location)
        .filter(Boolean)
        .join('|')

      const routeResult = await planRoute({
        mode: routeStore.mode,
        origin: result.origin,
        destination: result.destination,
        waypoints: waypoints || undefined,
      })

      if (routeResult.status === '1') {
        routeStore.setRouteResult(routeResult)
        const originPoint = parseCoord(result.origin)
        const destinationPoint = parseCoord(result.destination)
        if (originPoint) {
          routeStore.setOrigin({ ...originPoint, name: 'AI推荐起点' })
        }
        if (destinationPoint) {
          routeStore.setDestination({ ...destinationPoint, name: 'AI推荐终点' })
        }
      } else {
        ElMessage.warning(routeResult.info || 'AI规划完成，但路线生成失败')
      }
    }
  } catch (error: any) {
    console.error('AI路线规划失败:', error)
    ElMessage.error(error.response?.data?.message || 'AI路线规划失败，请稍后重试')
  } finally {
    routeStore.setLoading(false)
  }
}

// 应用路线结果
function handleApplyResult() {
  if (routeStore.routeResult?.route?.paths?.length) {
    ElMessage.success('路线已应用，请在地图上查看')
  }
}
</script>

<style scoped>
.ai-route-panel {
  padding: 16px;
}

.ai-input-section {
  margin-bottom: 16px;
}

.input-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
  font-size: 14px;
  font-weight: 500;
}

.ai-icon {
  font-size: 18px;
}

.input-tips {
  margin-top: 8px;
  font-size: 12px;
  color: #909399;
}

.city-section {
  margin-bottom: 16px;
}

.plan-button {
  width: 100%;
}

.ai-result {
  margin-top: 16px;
  padding: 12px;
  background: #f5f7fa;
  border-radius: 8px;
}

.result-header {
  font-size: 14px;
  font-weight: 500;
  margin-bottom: 8px;
}

.result-content {
  font-size: 13px;
  color: #606266;
  line-height: 1.6;
  margin-bottom: 12px;
}

.result-actions {
  display: flex;
  justify-content: flex-end;
}
</style>
