import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import type { AuthSession } from '@/types/chat'
import { loginRequest } from '@/services/auth'
import { STORAGE_KEYS } from '@/utils/storage'

export const useAuthStore = defineStore('auth', () => {
  const session = ref<AuthSession | null>(readStoredSession())
  const isLoading = ref(false)

  const isAuthenticated = computed(() => Boolean(session.value?.token))
  const currentUser = computed(() => session.value?.userInfo ?? null)

  async function login(username: string, password: string) {
    isLoading.value = true

    try {
      session.value = await loginRequest(username, password)
      persistSession(session.value)
    } finally {
      isLoading.value = false
    }
  }

  function logout() {
    clearSession()
  }

  function clearSession() {
    session.value = null
    localStorage.removeItem(STORAGE_KEYS.session)
  }

  function setSession(nextSession: AuthSession | null) {
    session.value = nextSession

    if (nextSession) {
      persistSession(nextSession)
      return
    }

    localStorage.removeItem(STORAGE_KEYS.session)
  }

  return {
    session,
    isLoading,
    isAuthenticated,
    currentUser,
    login,
    logout,
    clearSession,
    setSession,
  }
})

function readStoredSession(): AuthSession | null {
  const raw = localStorage.getItem(STORAGE_KEYS.session)
  if (!raw) return null

  try {
    return JSON.parse(raw) as AuthSession
  } catch {
    return null
  }
}

function persistSession(session: AuthSession) {
  localStorage.setItem(STORAGE_KEYS.session, JSON.stringify(session))
}
