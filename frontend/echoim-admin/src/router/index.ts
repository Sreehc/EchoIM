import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/login',
      name: 'login',
      component: () => import('@/views/Login.vue'),
      meta: { public: true },
    },
    {
      path: '/',
      component: () => import('@/layouts/AdminLayout.vue'),
      redirect: '/',
      children: [
        {
          path: '',
          name: 'dashboard',
          component: () => import('@/views/dashboard/Dashboard.vue'),
          meta: { title: '数据看板' },
        },
        {
          path: 'users',
          name: 'users',
          component: () => import('@/views/user/UserList.vue'),
          meta: { title: '用户管理' },
        },
        {
          path: 'groups',
          name: 'groups',
          component: () => import('@/views/group/GroupList.vue'),
          meta: { title: '群组管理' },
        },
        {
          path: 'beauty-nos',
          name: 'beauty-nos',
          component: () => import('@/views/beauty-no/BeautyNoList.vue'),
          meta: { title: '靓号管理' },
        },
        {
          path: 'configs',
          name: 'configs',
          component: () => import('@/views/config/ConfigList.vue'),
          meta: { title: '系统配置' },
        },
        {
          path: 'versions',
          name: 'versions',
          component: () => import('@/views/version/VersionList.vue'),
          meta: { title: '版本管理' },
        },
      ],
    },
  ],
})

router.beforeEach((to) => {
  if (to.meta.public) return true
  const authStore = useAuthStore()
  if (!authStore.isLoggedIn) {
    return { name: 'login', query: { redirect: to.fullPath } }
  }
  return true
})

export default router
