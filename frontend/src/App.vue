<script setup lang="ts">
import { computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { getToken } from '@/api/user'

const router = useRouter()
const route = useRoute()

const isLoggedIn = computed(() => !!getToken())

function goToMap() {
  router.push('/map')
}

function goToProfile() {
  router.push('/profile')
}

function handleLogout() {
  import('@/api/user').then(({ logout }) => {
    logout()
    router.push('/')
  })
}
</script>

<template>
  <div id="app-wrapper">
    <!-- 顶部导航栏 -->
    <nav class="navbar">
      <div class="nav-brand" @click="goToMap">
        <span class="brand-icon">🚴</span>
        <span class="brand-text">骑迹</span>
      </div>
      <div class="nav-links">
        <a
          class="nav-link"
          :class="{ active: route.path === '/map' }"
          @click="goToMap"
        >
          地图
        </a>
        <a
          v-if="isLoggedIn"
          class="nav-link"
          :class="{ active: route.path === '/profile' }"
          @click="goToProfile"
        >
          个人中心
        </a>
        <a
          v-if="!isLoggedIn"
          class="nav-link login-link"
          @click="goToProfile"
        >
          登录
        </a>
        <a
          v-else
          class="nav-link login-link"
          @click="handleLogout"
        >
          退出
        </a>
      </div>
    </nav>

    <!-- 主内容区 -->
    <router-view />
  </div>
</template>

<style>
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

html, body, #app {
  width: 100%;
  height: 100%;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
}

#app-wrapper {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
}

.navbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 24px;
  height: 56px;
  background: #fff;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  z-index: 100;
}

.nav-brand {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  font-weight: 600;
  font-size: 18px;
  color: #333;
}

.brand-icon {
  font-size: 24px;
}

.nav-links {
  display: flex;
  align-items: center;
  gap: 8px;
}

.nav-link {
  padding: 8px 16px;
  color: #666;
  text-decoration: none;
  cursor: pointer;
  border-radius: 4px;
  transition: all 0.2s;
  font-size: 14px;
}

.nav-link:hover {
  color: #409eff;
  background-color: #f5f7fa;
}

.nav-link.active {
  color: #409eff;
  background-color: #ecf5ff;
}

.login-link {
  color: #409eff;
  font-weight: 500;
}
</style>
