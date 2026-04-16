<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { getToken, logout } from '@/api/user'

const router = useRouter()
const route = useRoute()
const authVersion = ref(0)

const isLoggedIn = computed(() => {
  authVersion.value
  route.path
  return !!getToken()
})

function handleAuthChanged() {
  authVersion.value += 1
}

onMounted(() => {
  window.addEventListener('auth-changed', handleAuthChanged)
  window.addEventListener('storage', handleAuthChanged)
})

onUnmounted(() => {
  window.removeEventListener('auth-changed', handleAuthChanged)
  window.removeEventListener('storage', handleAuthChanged)
})

function goToMap() {
  router.push('/map')
}

function goToProfile() {
  router.push('/profile')
}

function goToLogin() {
  router.push('/login')
}

function goToPattern() {
  router.push('/pattern')
}

function goToCommunity() {
  router.push('/community')
}

function handleLogout() {
  logout()
  router.push('/')
}
</script>

<template>
  <div id="app-wrapper">
    <!-- 顶部导航栏 -->
    <nav class="navbar">
      <div class="nav-brand" @click="goToMap">
        <div class="brand-icon">
          <svg width="28" height="28" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
            <path d="M12 2C8.13 2 5 5.13 5 9c0 5.25 7 13 7 13s7-7.75 7-13c0-3.87-3.13-7-7-7z" fill="#14b8a6"/>
            <circle cx="12" cy="9" r="2.5" fill="white"/>
          </svg>
        </div>
        <span class="brand-text">骑迹</span>
      </div>

      <div class="nav-links">
        <a
          class="nav-link"
          :class="{ active: route.path === '/map' }"
          @click="goToMap"
        >
          <svg class="nav-icon" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0 1 18 0z"/>
            <circle cx="12" cy="10" r="3"/>
          </svg>
          地图
        </a>
        <a
          class="nav-link"
          :class="{ active: route.path === '/pattern' }"
          @click="goToPattern"
        >
          <svg class="nav-icon" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <polygon points="12 2 2 7 12 12 22 7 12 2"/>
            <polyline points="2 17 12 22 22 17"/>
            <polyline points="2 12 12 17 22 12"/>
          </svg>
          图案路书
        </a>
        <a
          class="nav-link"
          :class="{ active: route.path.startsWith('/community') }"
          @click="goToCommunity"
        >
          <svg class="nav-icon" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/>
            <circle cx="9" cy="7" r="4"/>
            <path d="M23 21v-2a4 4 0 0 0-3-3.87"/>
            <path d="M16 3.13a4 4 0 0 1 0 7.75"/>
          </svg>
          社区
        </a>

        <div class="nav-divider"></div>

        <template v-if="!isLoggedIn">
          <a
            class="nav-link nav-auth"
            @click="goToLogin"
          >
            <svg class="nav-icon" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M15 3h4a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2h-4"/>
              <polyline points="10 17 15 12 10 7"/>
              <line x1="15" y1="12" x2="3" y2="12"/>
            </svg>
            登录
          </a>
        </template>
        <template v-else>
          <a
            class="nav-link"
            :class="{ active: route.path === '/profile' }"
            @click="goToProfile"
          >
            <svg class="nav-icon" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/>
              <circle cx="12" cy="7" r="4"/>
            </svg>
            个人中心
          </a>
          <a
            class="nav-link nav-auth nav-logout"
            @click="handleLogout"
          >
            <svg class="nav-icon" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"/>
              <polyline points="16 17 21 12 16 7"/>
              <line x1="21" y1="12" x2="9" y2="12"/>
            </svg>
            退出
          </a>
        </template>
      </div>
    </nav>

    <!-- 主内容区 -->
    <main class="main-content">
      <router-view />
    </main>
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
  min-height: 100vh;
  font-family: 'Inter', -apple-system, BlinkMacSystemFont, 'PingFang SC', 'Segoe UI', Roboto, sans-serif;
}

#app-wrapper {
  width: 100%;
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background-color: #f8fafc;
}

.navbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 32px;
  height: 64px;
  background: #ffffff;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05), 0 1px 2px rgba(0, 0, 0, 0.03);
  z-index: 100;
  position: sticky;
  top: 0;
}

.nav-brand {
  display: flex;
  align-items: center;
  gap: 10px;
  cursor: pointer;
  font-weight: 600;
  font-size: 20px;
  color: #1e293b;
  transition: opacity 0.2s;
}

.nav-brand:hover {
  opacity: 0.8;
}

.brand-icon {
  display: flex;
  align-items: center;
  justify-content: center;
}

.brand-text {
  letter-spacing: -0.02em;
}

.nav-links {
  display: flex;
  align-items: center;
  gap: 4px;
}

.nav-link {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 14px;
  color: #64748b;
  text-decoration: none;
  cursor: pointer;
  border-radius: 10px;
  transition: all 0.2s ease;
  font-size: 14px;
  font-weight: 500;
}

.nav-icon {
  flex-shrink: 0;
}

.nav-link:hover {
  color: #14b8a6;
  background-color: #f0fdfa;
}

.nav-link.active {
  color: #14b8a6;
  background-color: #f0fdfa;
}

.nav-divider {
  width: 1px;
  height: 24px;
  background: #e2e8f0;
  margin: 0 8px;
}

.nav-auth {
  background: linear-gradient(135deg, #14b8a6 0%, #0d9488 100%);
  color: #ffffff !important;
  padding: 8px 16px;
}

.nav-auth:hover {
  background: linear-gradient(135deg, #0d9488 0%, #0f766e 100%);
  color: #ffffff !important;
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(20, 184, 166, 0.3);
}

.nav-logout {
  background: transparent;
  color: #94a3b8 !important;
  border: 1px solid #e2e8f0;
}

.nav-logout:hover {
  background: #fef2f2 !important;
  color: #ef4444 !important;
  border-color: #fecaca;
  box-shadow: none;
  transform: none;
}

.main-content {
  flex: 1;
  display: flex;
  flex-direction: column;
}
</style>
