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
        <p>{{ aiResult.description }}</p>
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
import { planAIRoute } from '@/api/route'

const routeStore = useRouteStore()

// 输入数据
const aiDescription = ref('')
const city = ref('')

// AI结果
const aiResult = ref<{
  description: string
  routes: any[]
} | null>(null)

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
    })

    aiResult.value = result
    ElMessage.success('AI路线规划完成')

    // 自动应用第一条路线
    if (result.routes && result.routes.length > 0) {
      routeStore.setRouteResult(result.routes[0] || null)
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
  if (aiResult.value?.routes && aiResult.value.routes.length > 0) {
    routeStore.setRouteResult(aiResult.value.routes[0] || null)
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
