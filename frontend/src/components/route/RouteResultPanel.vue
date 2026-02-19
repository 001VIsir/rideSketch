<template>
  <div class="route-result-panel">
    <template v-if="routeStore.hasRoute">
      <!-- 路线概览 -->
      <div class="route-overview">
        <div class="overview-item">
          <span class="overview-label">总距离</span>
          <span class="overview-value">{{ routeStore.formatDistance(selectedPath?.distance || '0') }}</span>
        </div>
        <div class="overview-item">
          <span class="overview-label">预计时间</span>
          <span class="overview-value">{{ routeStore.formatDuration(selectedPath?.duration || '0') }}</span>
        </div>
      </div>

      <!-- 路线方案选择 -->
      <div v-if="paths.length > 1" class="path-selector">
        <div class="selector-header">
          <span>路线方案 ({{ paths.length }})</span>
        </div>
        <el-radio-group
          v-model="selectedPathIndex"
          class="path-radio-group"
          @change="handlePathChange"
        >
          <el-radio-button
            v-for="(path, index) in paths"
            :key="index"
            :value="index"
          >
            方案{{ index + 1 }}
          </el-radio-button>
        </el-radio-group>
      </div>

      <!-- 路线步骤 -->
      <div class="route-steps">
        <div class="steps-header">
          <span>路线详情</span>
        </div>
        <div class="steps-list">
          <div
            v-for="(step, index) in currentSteps"
            :key="index"
            class="step-item"
          >
            <div class="step-connector">
              <div class="step-dot"></div>
              <div v-if="index < currentSteps.length - 1" class="step-line"></div>
            </div>
            <div class="step-content">
              <div class="step-instruction">{{ step.instruction }}</div>
              <div class="step-info">
                <span>{{ routeStore.formatDistance(step.distance) }}</span>
                <span>{{ routeStore.formatDuration(step.duration) }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 操作按钮 -->
      <div class="action-buttons">
        <el-button type="primary" size="small" @click="handleSaveRoute">
          保存路线
        </el-button>
        <el-button size="small" @click="handleShareRoute">
          分享路线
        </el-button>
      </div>
    </template>

    <!-- 无结果提示 -->
    <div v-else class="no-result">
      <el-empty description="暂无路线规划结果" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { useRouteStore } from '@/stores/routeStore'
import type { PathInfo, StepInfo } from '@/api/route'

const routeStore = useRouteStore()

// 选中的路线方案索引
const selectedPathIndex = ref(0)

// 获取所有路径
const paths = computed(() => {
  if (!routeStore.routeResult?.route?.paths) return []
  return routeStore.routeResult.route.paths
})

// 获取当前选中的路径
const selectedPath = computed(() => {
  return paths.value[selectedPathIndex.value] || null
})

// 获取当前路径的步骤
const currentSteps = computed(() => {
  if (!selectedPath.value?.steps) return []
  return selectedPath.value.steps
})

// 监听路径变化
watch(
  () => routeStore.routeResult,
  () => {
    selectedPathIndex.value = 0
  }
)

// 处理路径方案切换
function handlePathChange(index: number) {
  const path = paths.value[index]
  if (path) {
    routeStore.setSelectedPath(path)
  }
}

// 保存路线
function handleSaveRoute() {
  ElMessage.info('保存路线功能开发中')
}

// 分享路线
function handleShareRoute() {
  ElMessage.info('分享路线功能开发中')
}
</script>

<style scoped>
.route-result-panel {
  padding: 16px;
}

.route-overview {
  display: flex;
  justify-content: space-around;
  padding: 12px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 8px;
  margin-bottom: 16px;
}

.overview-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
}

.overview-label {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.8);
}

.overview-value {
  font-size: 18px;
  font-weight: 600;
  color: #fff;
}

.path-selector {
  margin-bottom: 16px;
}

.selector-header {
  font-size: 14px;
  font-weight: 500;
  margin-bottom: 8px;
}

.path-radio-group {
  width: 100%;
}

.path-radio-group :deep(.el-radio-button) {
  flex: 1;
}

.path-radio-group :deep(.el-radio-button__inner) {
  width: 100%;
}

.route-steps {
  margin-bottom: 16px;
}

.steps-header {
  font-size: 14px;
  font-weight: 500;
  margin-bottom: 12px;
}

.steps-list {
  max-height: 300px;
  overflow-y: auto;
}

.step-item {
  display: flex;
  gap: 12px;
  padding-bottom: 12px;
}

.step-connector {
  display: flex;
  flex-direction: column;
  align-items: center;
  width: 20px;
}

.step-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: #409eff;
  flex-shrink: 0;
}

.step-line {
  width: 2px;
  flex: 1;
  background: #dcdfe6;
  min-height: 20px;
}

.step-content {
  flex: 1;
}

.step-instruction {
  font-size: 14px;
  color: #303133;
  line-height: 1.5;
  margin-bottom: 4px;
}

.step-info {
  display: flex;
  gap: 12px;
  font-size: 12px;
  color: #909399;
}

.action-buttons {
  display: flex;
  gap: 8px;
}

.action-buttons .el-button {
  flex: 1;
}

.no-result {
  padding: 40px 0;
}
</style>
