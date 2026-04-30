import { createRouter, createWebHistory } from 'vue-router'
import { pinia } from '@/stores/pinia'
import { useAuthStore } from '@/stores/auth'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/',
      redirect: '/chat',
    },
    {
      path: '/login',
      component: () => import('@/layouts/AuthLayout.vue'),
      meta: { guestOnly: true },
      children: [{ path: '', name: 'login', component: () => import('@/views/LoginView.vue') }],
    },
    {
      path: '/u',
      component: () => import('@/layouts/AuthLayout.vue'),
      children: [{ path: ':username', name: 'public-profile', component: () => import('@/views/PublicProfileView.vue') }],
    },
    {
      path: '/chat',
      component: () => import('@/layouts/ChatLayout.vue'),
      meta: { requiresAuth: true },
      children: [
        { path: ':conversationId?', name: 'chat-home', component: () => import('@/views/chat/ChatHomeView.vue') },
      ],
    },
  ],
})

router.beforeEach((to) => {
  const authStore = useAuthStore(pinia)

  if (to.meta.requiresAuth && !authStore.isAuthenticated) {
    return { name: 'login' }
  }

  if (to.meta.guestOnly && authStore.isAuthenticated && to.query.add !== '1') {
    return { name: 'chat-home' }
  }

  return true
})

export { router }
