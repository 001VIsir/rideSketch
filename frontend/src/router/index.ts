import { createRouter, createWebHistory } from 'vue-router'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      redirect: '/map',
    },
    {
      path: '/map',
      name: 'map',
      component: () => import('@/views/map/MapPage.vue'),
    },
    {
      path: '/profile',
      name: 'profile',
      component: () => import('@/views/profile/ProfilePage.vue'),
    },
    {
      path: '/login',
      name: 'login',
      component: () => import('@/views/auth/LoginPage.vue'),
    },
    {
      path: '/register',
      name: 'register',
      component: () => import('@/views/auth/RegisterPage.vue'),
    },
    {
      path: '/pattern',
      name: 'pattern',
      component: () => import('@/views/route/PatternPage.vue'),
    },
    {
      path: '/community',
      name: 'community',
      component: () => import('@/views/community/CommunityPage.vue'),
    },
    {
      path: '/community/route/:id',
      name: 'route-detail',
      component: () => import('@/views/community/RouteDetailPage.vue'),
    },
  ],
})

export default router
