import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
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
        {
          path: 'reports',
          name: 'reports',
          component: () => import('@/views/report/ReportList.vue'),
          meta: { title: '举报管理' },
        },
        {
          path: 'sensitive-words',
          name: 'sensitive-words',
          component: () => import('@/views/sensitive-word/SensitiveWordList.vue'),
          meta: { title: '敏感词管理' },
        },
        {
          path: 'notices',
          name: 'notices',
          component: () => import('@/views/notice/NoticeList.vue'),
          meta: { title: '系统公告' },
        },
        {
          path: 'bans',
          name: 'bans',
          component: () => import('@/views/ban/BanList.vue'),
          meta: { title: '用户封禁' },
        },
        {
          path: 'operation-logs',
          name: 'operation-logs',
          component: () => import('@/views/operation-log/OperationLogList.vue'),
          meta: { title: '操作日志' },
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
