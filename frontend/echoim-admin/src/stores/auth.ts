import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { AdminInfo } from '@/types/api'
import { adminLogin } from '@/api/auth'
import { TOKEN_KEY, ADMIN_INFO_KEY } from '@/api/http'

export const useAuthStore = defineStore('auth', () => {
  const token = ref<string>(localStorage.getItem(TOKEN_KEY) || '')
  const adminInfo = ref<AdminInfo | null>(loadAdminInfo())

  const isLoggedIn = computed(() => !!token.value)
  const displayName = computed(() => adminInfo.value?.nickname || adminInfo.value?.username || '管理员')

  function loadAdminInfo(): AdminInfo | null {
    try {
      const raw = localStorage.getItem(ADMIN_INFO_KEY)
      return raw ? JSON.parse(raw) : null
    } catch {
      return null
    }
  }

  async function login(username: string, password: string) {
    const result = await adminLogin({ username, password })
    token.value = result.token
    adminInfo.value = result.adminInfo
    localStorage.setItem(TOKEN_KEY, result.token)
    localStorage.setItem(ADMIN_INFO_KEY, JSON.stringify(result.adminInfo))
  }

  function logout() {
    token.value = ''
    adminInfo.value = null
    localStorage.removeItem(TOKEN_KEY)
    localStorage.removeItem(ADMIN_INFO_KEY)
  }

  return { token, adminInfo, isLoggedIn, displayName, login, logout }
})
