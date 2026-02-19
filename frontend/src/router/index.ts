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
  ],
})

export default router
