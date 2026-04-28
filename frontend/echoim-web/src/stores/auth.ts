import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import type {
  AuthSession,
  ChangePasswordPayload,
  CurrentUserProfile,
  UpdateCurrentUserProfilePayload,
} from '@/types/chat'
import { changePasswordRequest, loginRequest, logoutRequest } from '@/services/auth'
import { updateCurrentUserProfile, fetchCurrentUserProfile } from '@/services/user'
import { STORAGE_KEYS } from '@/utils/storage'
import { normalizeDisplayText } from '@/utils/text'

export const useAuthStore = defineStore('auth', () => {
  const session = ref<AuthSession | null>(readStoredSession())
  const isLoading = ref(false)
  const profile = ref<CurrentUserProfile | null>(null)
  const profileLoading = ref(false)
  const profileSaving = ref(false)
  const passwordSaving = ref(false)
  const profileError = ref<string | null>(null)
  const profileNotice = ref<string | null>(null)

  const isAuthenticated = computed(() => Boolean(session.value?.token))
  const currentUser = computed(() => session.value?.userInfo ?? null)

  async function login(username: string, password: string) {
    isLoading.value = true

    try {
      session.value = normalizeSession(await loginRequest(username, password))
      persistSession(session.value)
    } finally {
      isLoading.value = false
    }
  }

  async function ensureCurrentProfile(force = false) {
    if (!isAuthenticated.value) return null
    if (profile.value && !force) return profile.value

    profileLoading.value = true
    profileError.value = null

    try {
      profile.value = normalizeProfile(await fetchCurrentUserProfile())
      syncSessionUserInfo(profile.value)
      return profile.value
    } catch (error) {
      profileError.value = error instanceof Error ? error.message : '个人资料加载失败'
      throw error
    } finally {
      profileLoading.value = false
    }
  }

  async function saveCurrentProfile(payload: UpdateCurrentUserProfilePayload) {
    profileSaving.value = true
    profileError.value = null
    profileNotice.value = null

    try {
      profile.value = normalizeProfile(await updateCurrentUserProfile(payload))
      syncSessionUserInfo(profile.value)
      profileNotice.value = '个人资料已更新'
      return profile.value
    } catch (error) {
      profileError.value = error instanceof Error ? error.message : '个人资料保存失败'
      throw error
    } finally {
      profileSaving.value = false
    }
  }

  async function changePassword(payload: ChangePasswordPayload) {
    passwordSaving.value = true
    profileError.value = null
    profileNotice.value = null

    try {
      await changePasswordRequest(payload)
      profileNotice.value = '密码已更新'
    } catch (error) {
      profileError.value = error instanceof Error ? error.message : '密码更新失败'
      throw error
    } finally {
      passwordSaving.value = false
    }
  }

  async function logout() {
    try {
      await logoutRequest()
    } finally {
      clearSession()
    }
  }

  function clearSession() {
    session.value = null
    profile.value = null
    profileError.value = null
    profileNotice.value = null
    localStorage.removeItem(STORAGE_KEYS.session)
  }

  function setSession(nextSession: AuthSession | null) {
    session.value = nextSession ? normalizeSession(nextSession) : null
    if (!nextSession) {
      profile.value = null
    }

    if (nextSession) {
      persistSession(session.value)
      return
    }

    localStorage.removeItem(STORAGE_KEYS.session)
  }

  function clearProfileNotice() {
    profileNotice.value = null
  }

  function clearProfileError() {
    profileError.value = null
  }

  function syncSessionUserInfo(nextProfile: CurrentUserProfile) {
    if (!session.value) return

    session.value = normalizeSession({
      ...session.value,
      userInfo: {
        userId: nextProfile.userId,
        username: nextProfile.username,
        nickname: nextProfile.nickname,
        avatarUrl: nextProfile.avatarUrl,
      },
    })
    persistSession(session.value)
  }

  return {
    session,
    isLoading,
    profile,
    profileLoading,
    profileSaving,
    passwordSaving,
    profileError,
    profileNotice,
    isAuthenticated,
    currentUser,
    login,
    logout,
    clearSession,
    setSession,
    ensureCurrentProfile,
    saveCurrentProfile,
    changePassword,
    clearProfileNotice,
    clearProfileError,
  }
})

function readStoredSession(): AuthSession | null {
  const raw = localStorage.getItem(STORAGE_KEYS.session)
  if (!raw) return null

  try {
    return normalizeSession(JSON.parse(raw) as AuthSession)
  } catch {
    return null
  }
}

function persistSession(session: AuthSession | null) {
  if (!session) return
  localStorage.setItem(STORAGE_KEYS.session, JSON.stringify(session))
}

function normalizeSession(session: AuthSession): AuthSession {
  return {
    ...session,
    userInfo: {
      ...session.userInfo,
      nickname: normalizeDisplayText(session.userInfo.nickname),
    },
  }
}

function normalizeProfile(profile: CurrentUserProfile): CurrentUserProfile {
  return {
    ...profile,
    nickname: normalizeDisplayText(profile.nickname),
    signature: profile.signature ? normalizeDisplayText(profile.signature) : null,
  }
}
