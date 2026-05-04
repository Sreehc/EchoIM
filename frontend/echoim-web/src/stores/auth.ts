import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import type {
  AuthSession,
  ChangePasswordPayload,
  CodeDispatchResult,
  CurrentUserProfile,
  LoginFlowResponse,
  SecurityEventSummary,
  StoredAccount,
  TrustedDeviceSummary,
  UpdateCurrentUserProfilePayload,
} from '@/types/chat'
import type { ApiBlockedUserItem } from '@/types/api'
import {
  bindEmailRequest,
  changePasswordRequest,
  fetchSecurityEventsRequest,
  fetchTrustedDevicesRequest,
  loginRequest,
  logoutRequest,
  refreshSessionRequest,
  resendLoginChallengeRequest,
  resetRecoveryPasswordRequest,
  revokeAllTrustedDevicesRequest,
  revokeTrustedDeviceRequest,
  sendEmailBindCodeRequest,
  sendRecoveryCodeRequest,
  trustedDeviceLoginRequest,
  verifyLoginChallengeRequest,
  verifyRecoveryCodeRequest,
} from '@/services/auth'
import { fetchBlockedUsers, unblockUser } from '@/services/blocks'
import { HttpError } from '@/services/http'
import { updateCurrentUserProfile, fetchCurrentUserProfile } from '@/services/user'
import { STORAGE_KEYS } from '@/utils/storage'
import { normalizeDisplayText } from '@/utils/text'

type SessionPersistence = 'local' | 'session' | 'none'
const REFRESH_LEAD_TIME_MS = 10 * 60 * 1000
const MIN_SESSION_VALIDITY_MS = 90 * 1000

const initialSessionState = readPersistedSession()

export const useAuthStore = defineStore('auth', () => {
  const storedAccounts = ref<StoredAccount[]>(readStoredAccounts())
  const session = ref<AuthSession | null>(initialSessionState.session)
  const sessionPersistence = ref<SessionPersistence>(initialSessionState.persistence)
  const isLoading = ref(false)
  const profile = ref<CurrentUserProfile | null>(null)
  const profileLoading = ref(false)
  const profileSaving = ref(false)
  const passwordSaving = ref(false)
  const emailBindingLoading = ref(false)
  const trustedDevicesLoading = ref(false)
  const securityEventsLoading = ref(false)
  const blockedUsersLoading = ref(false)
  const trustedDevices = ref<TrustedDeviceSummary[]>([])
  const securityEvents = ref<SecurityEventSummary[]>([])
  const blockedUsers = ref<ApiBlockedUserItem[]>([])
  const profileError = ref<string | null>(null)
  const profileNotice = ref<string | null>(null)
  let refreshPromise: Promise<AuthSession | null> | null = null
  let refreshTimer: number | null = null

  const isAuthenticated = computed(() => Boolean(session.value?.token))
  const currentUser = computed(() => session.value?.userInfo ?? null)
  const hasStoredAccounts = computed(() => storedAccounts.value.length > 0)

  async function login(payload: {
    username: string
    password: string
    rememberMe: boolean
    trustDevice: boolean
    deviceFingerprint: string
    deviceName: string
  }) {
    isLoading.value = true

    try {
      const response = normalizeLoginFlow(await loginRequest(payload))
      if (response.status === 'authenticated') {
        applyAuthenticatedSession(response, {
          rememberMe: payload.rememberMe,
          deviceFingerprint: payload.deviceFingerprint,
          persistStoredAccount: payload.rememberMe,
        })
      }
      return response
    } finally {
      isLoading.value = false
    }
  }

  async function verifyLoginChallenge(payload: {
    challengeTicket: string
    code: string
    rememberMe: boolean
    deviceFingerprint: string
  }) {
    isLoading.value = true

    try {
      const response = normalizeLoginFlow(await verifyLoginChallengeRequest(payload))
      if (response.status !== 'authenticated') {
        throw new Error('登录状态异常')
      }
      applyAuthenticatedSession(response, {
        rememberMe: payload.rememberMe,
        deviceFingerprint: payload.deviceFingerprint,
        persistStoredAccount: payload.rememberMe,
      })
      return response
    } finally {
      isLoading.value = false
    }
  }

  async function resendLoginChallenge(challengeTicket: string) {
    return resendLoginChallengeRequest({ challengeTicket })
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
      clearStoredSessionTokensForCurrentAccount()
      clearTrustedGrantForCurrentAccount()
      profileNotice.value = '密码已更新'
    } catch (error) {
      profileError.value = error instanceof Error ? error.message : '密码更新失败'
      throw error
    } finally {
      passwordSaving.value = false
    }
  }

  async function refreshSession() {
    if (refreshPromise) {
      return refreshPromise
    }

    const currentSession = session.value
    if (!currentSession?.refreshToken) {
      throw new Error('登录已失效，请重新登录')
    }

    const persistence = sessionPersistence.value
    const userId = currentSession.userInfo.userId
    const deviceFingerprint = getStoredAccount(userId)?.deviceFingerprint ?? null

    refreshPromise = (async () => {
      try {
        const response = normalizeLoginFlow(await refreshSessionRequest({
          refreshToken: currentSession.refreshToken ?? '',
        }))
        if (response.status !== 'authenticated') {
          throw new Error('登录状态异常')
        }

        applyAuthenticatedSession(response, {
          rememberMe: persistence === 'local',
          deviceFingerprint,
          persistStoredAccount: persistence === 'local',
        })
        return session.value
      } catch (error) {
        clearStoredSessionTokens(userId)
        clearSession()
        throw error
      } finally {
        refreshPromise = null
      }
    })()

    return refreshPromise
  }

  async function ensureSessionFresh(minValidityMs = MIN_SESSION_VALIDITY_MS) {
    const currentSession = session.value
    if (!currentSession) {
      throw new Error('登录已失效，请重新登录')
    }

    const expiresAt = new Date(currentSession.expireAt ?? '').getTime()
    if (!Number.isFinite(expiresAt) || Date.now() >= expiresAt - minValidityMs) {
      const nextSession = await refreshSession()
      if (!nextSession) {
        throw new Error('登录已失效，请重新登录')
      }
      return nextSession
    }

    return currentSession
  }

  async function logout() {
    const refreshToken = session.value?.refreshToken ?? null
    const userId = session.value?.userInfo.userId ?? null
    try {
      await logoutRequest({ refreshToken })
    } finally {
      if (userId != null) {
        clearStoredAuthState(userId)
      }
      clearSession()
    }
  }

  async function activateStoredAccount(userId: number) {
    const account = storedAccounts.value.find((item) => item.userInfo.userId === userId)
    if (!account) return null

    if (isStoredSessionUsable(account)) {
      const expiresAt = new Date(account.sessionExpireAt ?? 0).getTime()
      const nextSession = normalizeSession({
        token: account.sessionToken ?? '',
        tokenType: account.sessionTokenType ?? 'Bearer',
        expiresIn: Math.max(60, Math.floor((expiresAt - Date.now()) / 1000)),
        expireAt: account.sessionExpireAt,
        refreshToken: account.refreshToken,
        refreshTokenExpireAt: account.refreshTokenExpireAt,
        userInfo: account.userInfo,
      })
      session.value = nextSession
      sessionPersistence.value = 'local'
      persistSession(nextSession, 'local')
      upsertStoredAccount(nextSession, {
        rememberMe: true,
        sessionToken: nextSession.token,
        sessionTokenType: nextSession.tokenType,
        sessionExpireAt: account.sessionExpireAt,
        refreshToken: account.refreshToken,
        refreshTokenExpireAt: account.refreshTokenExpireAt,
        trustedDeviceGrantToken: account.trustedDeviceGrantToken,
        trustedDeviceExpireAt: account.trustedDeviceExpireAt,
        deviceFingerprint: account.deviceFingerprint,
      })
      return session.value
    }

    if (isStoredRefreshUsable(account)) {
      isLoading.value = true

      try {
        const response = normalizeLoginFlow(await refreshSessionRequest({
          refreshToken: account.refreshToken ?? '',
        }))
        if (response.status !== 'authenticated') {
          throw new Error('账号切换失败')
        }
        applyAuthenticatedSession(response, {
          rememberMe: true,
          deviceFingerprint: account.deviceFingerprint,
          persistStoredAccount: true,
        })
        return session.value
      } catch (error) {
        if (error instanceof HttpError && error.code && [401, 40100, 40101, 40102].includes(error.code)) {
          clearStoredSessionTokens(userId)
        }
        throw error
      } finally {
        isLoading.value = false
      }
    }

    if (!isTrustedDeviceGrantUsable(account)) {
      throw new Error('该账号本地会话已失效，请重新输入密码。')
    }

    isLoading.value = true

    try {
      const response = normalizeLoginFlow(await trustedDeviceLoginRequest({
        userId,
        deviceFingerprint: account.deviceFingerprint ?? '',
        grantToken: account.trustedDeviceGrantToken ?? '',
      }))
      if (response.status !== 'authenticated') {
        throw new Error('账号切换失败')
      }
      applyAuthenticatedSession(response, {
        rememberMe: true,
        deviceFingerprint: account.deviceFingerprint,
        persistStoredAccount: true,
      })
      return session.value
    } catch (error) {
      if (error instanceof HttpError && error.code && [401, 40100, 40101, 40102].includes(error.code)) {
        clearTrustedGrant(userId)
      }
      throw error
    } finally {
      isLoading.value = false
    }
  }

  async function sendRecoveryCode(email: string) {
    return sendRecoveryCodeRequest({ email })
  }

  async function verifyRecoveryCode(email: string, code: string) {
    return verifyRecoveryCodeRequest({ email, code })
  }

  async function resetRecoveryPassword(recoveryToken: string, newPassword: string) {
    return resetRecoveryPasswordRequest({ recoveryToken, newPassword })
  }

  async function sendEmailBindCode(email: string, currentPassword: string): Promise<CodeDispatchResult> {
    emailBindingLoading.value = true
    profileError.value = null
    profileNotice.value = null

    try {
      const result = await sendEmailBindCodeRequest({ email, currentPassword })
      profileNotice.value = '验证码已发送'
      return result
    } catch (error) {
      profileError.value = error instanceof Error ? error.message : '验证码发送失败'
      throw error
    } finally {
      emailBindingLoading.value = false
    }
  }

  async function bindEmail(email: string, code: string, currentPassword: string) {
    emailBindingLoading.value = true
    profileError.value = null
    profileNotice.value = null

    try {
      profile.value = normalizeProfile(await bindEmailRequest({ email, code, currentPassword }))
      syncSessionUserInfo(profile.value)
      profileNotice.value = '邮箱已更新'
      return profile.value
    } catch (error) {
      profileError.value = error instanceof Error ? error.message : '邮箱更新失败'
      throw error
    } finally {
      emailBindingLoading.value = false
    }
  }

  async function loadTrustedDevices(force = false) {
    if (!force && trustedDevices.value.length && !trustedDevicesLoading.value) return trustedDevices.value
    trustedDevicesLoading.value = true

    try {
      trustedDevices.value = await fetchTrustedDevicesRequest()
      return trustedDevices.value
    } finally {
      trustedDevicesLoading.value = false
    }
  }

  async function revokeTrustedDevice(deviceId: number, deviceFingerprint?: string | null) {
    trustedDevicesLoading.value = true

    try {
      await revokeTrustedDeviceRequest(deviceId)
      trustedDevices.value = trustedDevices.value.filter((item) => item.deviceId !== deviceId)
      clearTrustedGrantForMatchingDevice(deviceFingerprint ?? null)
      profileNotice.value = '设备已移除'
    } catch (error) {
      profileError.value = error instanceof Error ? error.message : '设备移除失败'
      throw error
    } finally {
      trustedDevicesLoading.value = false
    }
  }

  async function revokeAllTrustedDevices() {
    trustedDevicesLoading.value = true

    try {
      await revokeAllTrustedDevicesRequest()
      trustedDevices.value = []
      clearAllTrustedGrants()
      profileNotice.value = '已移除全部受信设备'
    } catch (error) {
      profileError.value = error instanceof Error ? error.message : '设备移除失败'
      throw error
    } finally {
      trustedDevicesLoading.value = false
    }
  }

  async function loadSecurityEvents(force = false) {
    if (!force && securityEvents.value.length && !securityEventsLoading.value) return securityEvents.value
    securityEventsLoading.value = true

    try {
      securityEvents.value = await fetchSecurityEventsRequest()
      return securityEvents.value
    } finally {
      securityEventsLoading.value = false
    }
  }

  async function loadBlockedUsers(force = false) {
    if (!force && blockedUsers.value.length && !blockedUsersLoading.value) return blockedUsers.value
    blockedUsersLoading.value = true

    try {
      blockedUsers.value = await fetchBlockedUsers()
      return blockedUsers.value
    } finally {
      blockedUsersLoading.value = false
    }
  }

  async function handleUnblockUser(userId: number) {
    blockedUsersLoading.value = true

    try {
      await unblockUser(userId)
      blockedUsers.value = blockedUsers.value.filter((item) => item.userId !== userId)
      profileNotice.value = '已取消屏蔽'
    } catch (error) {
      profileError.value = error instanceof Error ? error.message : '取消屏蔽失败'
      throw error
    } finally {
      blockedUsersLoading.value = false
    }
  }

  function removeStoredAccount(userId: number) {
    storedAccounts.value = storedAccounts.value.filter((item) => item.userInfo.userId !== userId)
    persistStoredAccounts(storedAccounts.value)
    if (session.value?.userInfo.userId === userId && sessionPersistence.value === 'local') {
      clearSession()
    }
  }

  function clearSession() {
    refreshPromise = null
    stopRefreshTimer()
    session.value = null
    sessionPersistence.value = 'none'
    profile.value = null
    profileError.value = null
    profileNotice.value = null
    localStorage.removeItem(STORAGE_KEYS.session)
    sessionStorage.removeItem(STORAGE_KEYS.session)
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
    persistSession(session.value, sessionPersistence.value)
    if (sessionPersistence.value === 'local') {
      upsertStoredAccount(session.value, {
        rememberMe: true,
        deviceFingerprint: getStoredAccount(nextProfile.userId)?.deviceFingerprint ?? null,
      })
    }
  }

  function applyAuthenticatedSession(
    response: LoginFlowResponse,
    options: {
      rememberMe: boolean
      deviceFingerprint: string | null
      persistStoredAccount: boolean
    },
  ) {
    if (!response.token || !response.tokenType || !response.expiresIn || !response.userInfo || !response.refreshToken) {
      throw new Error('登录返回不完整')
    }

    const expireAt = response.expireAt ?? new Date(Date.now() + response.expiresIn * 1000).toISOString()
    const refreshTokenExpireAt = response.refreshTokenExpireAt ?? null

    const nextSession: AuthSession = normalizeSession({
      token: response.token,
      tokenType: response.tokenType,
      expiresIn: response.expiresIn,
      expireAt,
      refreshToken: response.refreshToken,
      refreshTokenExpireAt,
      userInfo: response.userInfo,
    })

    session.value = nextSession
    sessionPersistence.value = options.rememberMe ? 'local' : 'session'
    persistSession(nextSession, sessionPersistence.value)
    scheduleRefresh(nextSession)
    if (options.persistStoredAccount) {
      const sessionExpireAt = new Date(Date.now() + nextSession.expiresIn * 1000).toISOString()
      upsertStoredAccount(nextSession, {
        rememberMe: true,
        sessionToken: nextSession.token,
        sessionTokenType: nextSession.tokenType,
        sessionExpireAt: expireAt || sessionExpireAt,
        refreshToken: nextSession.refreshToken,
        refreshTokenExpireAt,
        trustedDeviceGrantToken: response.trustedDeviceGrantToken ?? null,
        trustedDeviceExpireAt: response.trustedDeviceExpireAt ?? null,
        deviceFingerprint: options.deviceFingerprint,
      })
    }
  }

  function upsertStoredAccount(
    nextSession: AuthSession,
    meta?: {
      rememberMe?: boolean
      sessionToken?: string | null
      sessionTokenType?: string | null
      sessionExpireAt?: string | null
      refreshToken?: string | null
      refreshTokenExpireAt?: string | null
      trustedDeviceGrantToken?: string | null
      trustedDeviceExpireAt?: string | null
      deviceFingerprint?: string | null
    },
  ) {
    const existing = getStoredAccount(nextSession.userInfo.userId)
    const nextAccount = toStoredAccount(nextSession, existing, meta)
    const existingIndex = storedAccounts.value.findIndex((item) => item.userInfo.userId === nextAccount.userInfo.userId)
    if (existingIndex === -1) {
      storedAccounts.value = [nextAccount, ...storedAccounts.value]
    } else {
      const nextList = [...storedAccounts.value]
      nextList.splice(existingIndex, 1)
      storedAccounts.value = [nextAccount, ...nextList]
    }
    persistStoredAccounts(storedAccounts.value)
  }

  function clearTrustedGrant(userId: number) {
    const existing = getStoredAccount(userId)
    if (!existing) return

    storedAccounts.value = storedAccounts.value.map((account) =>
      account.userInfo.userId === userId
        ? {
            ...account,
            trustedDeviceGrantToken: null,
            trustedDeviceExpireAt: null,
          }
        : account,
    )
    persistStoredAccounts(storedAccounts.value)
  }

  function clearTrustedGrantForCurrentAccount() {
    const userId = session.value?.userInfo.userId
    if (!userId) return
    clearTrustedGrant(userId)
  }

  function clearStoredSessionTokensForCurrentAccount() {
    const userId = session.value?.userInfo.userId
    if (!userId) return
    clearStoredSessionTokens(userId)
  }

  function clearStoredSessionTokens(userId: number) {
    storedAccounts.value = storedAccounts.value.map((account) =>
      account.userInfo.userId === userId
        ? {
            ...account,
            sessionToken: null,
            sessionTokenType: null,
            sessionExpireAt: null,
            refreshToken: null,
            refreshTokenExpireAt: null,
          }
        : account,
    )
    persistStoredAccounts(storedAccounts.value)
  }

  function clearStoredAuthState(userId: number) {
    clearStoredSessionTokens(userId)
    clearTrustedGrant(userId)
  }

  function clearAllTrustedGrants() {
    storedAccounts.value = storedAccounts.value.map((account) => ({
      ...account,
      trustedDeviceGrantToken: null,
      trustedDeviceExpireAt: null,
    }))
    persistStoredAccounts(storedAccounts.value)
  }

  function clearTrustedGrantForMatchingDevice(deviceFingerprint: string | null) {
    if (!deviceFingerprint) {
      clearAllTrustedGrants()
      return
    }

    storedAccounts.value = storedAccounts.value.map((account) =>
      account.deviceFingerprint === deviceFingerprint
        ? {
            ...account,
            trustedDeviceGrantToken: null,
            trustedDeviceExpireAt: null,
          }
        : account,
    )
    persistStoredAccounts(storedAccounts.value)
  }

  function getStoredAccount(userId: number) {
    return storedAccounts.value.find((item) => item.userInfo.userId === userId) ?? null
  }

  function scheduleRefresh(nextSession: AuthSession | null) {
    stopRefreshTimer()
    if (!nextSession) return

    const expiresAt = new Date(nextSession.expireAt ?? '').getTime()
    if (!Number.isFinite(expiresAt)) return

    const delay = Math.max(5_000, expiresAt - Date.now() - REFRESH_LEAD_TIME_MS)
    refreshTimer = window.setTimeout(() => {
      void refreshSession().catch(() => undefined)
    }, delay)
  }

  function stopRefreshTimer() {
    if (refreshTimer != null) {
      window.clearTimeout(refreshTimer)
      refreshTimer = null
    }
  }

  scheduleRefresh(session.value)

  return {
    session,
    storedAccounts,
    hasStoredAccounts,
    isLoading,
    profile,
    profileLoading,
    profileSaving,
    passwordSaving,
    emailBindingLoading,
    trustedDevicesLoading,
    securityEventsLoading,
    blockedUsersLoading,
    trustedDevices,
    securityEvents,
    blockedUsers,
    profileError,
    profileNotice,
    isAuthenticated,
    currentUser,
    login,
    verifyLoginChallenge,
    resendLoginChallenge,
    logout,
    activateStoredAccount,
    sendRecoveryCode,
    verifyRecoveryCode,
    resetRecoveryPassword,
    sendEmailBindCode,
    bindEmail,
    loadTrustedDevices,
    revokeTrustedDevice,
    revokeAllTrustedDevices,
    loadSecurityEvents,
    loadBlockedUsers,
    handleUnblockUser,
    removeStoredAccount,
    clearSession,
    ensureCurrentProfile,
    saveCurrentProfile,
    changePassword,
    refreshSession,
    ensureSessionFresh,
    clearProfileNotice,
    clearProfileError,
  }
})

function readPersistedSession(): { session: AuthSession | null; persistence: SessionPersistence } {
  const sessionStorageRaw = sessionStorage.getItem(STORAGE_KEYS.session)
  if (sessionStorageRaw) {
    try {
      return {
        session: normalizeSession(JSON.parse(sessionStorageRaw) as AuthSession),
        persistence: 'session',
      }
    } catch {
      sessionStorage.removeItem(STORAGE_KEYS.session)
    }
  }

  const localStorageRaw = localStorage.getItem(STORAGE_KEYS.session)
  if (localStorageRaw) {
    try {
      return {
        session: normalizeSession(JSON.parse(localStorageRaw) as AuthSession),
        persistence: 'local',
      }
    } catch {
      localStorage.removeItem(STORAGE_KEYS.session)
    }
  }

  return { session: null, persistence: 'none' }
}

function readStoredAccounts(): StoredAccount[] {
  const raw = localStorage.getItem(STORAGE_KEYS.storedAccounts)
  if (!raw) return []

  try {
    return (JSON.parse(raw) as unknown[]).map(normalizeStoredAccount).filter((item): item is StoredAccount => Boolean(item))
  } catch {
    return []
  }
}

function persistSession(session: AuthSession | null, persistence: SessionPersistence) {
  localStorage.removeItem(STORAGE_KEYS.session)
  sessionStorage.removeItem(STORAGE_KEYS.session)
  if (!session) return

  const serialized = JSON.stringify(session)
  if (persistence === 'local') {
    localStorage.setItem(STORAGE_KEYS.session, serialized)
    return
  }

  if (persistence === 'session') {
    sessionStorage.setItem(STORAGE_KEYS.session, serialized)
  }
}

function persistStoredAccounts(accounts: StoredAccount[]) {
  localStorage.setItem(STORAGE_KEYS.storedAccounts, JSON.stringify(accounts))
}

function normalizeSession(session: AuthSession): AuthSession {
  const expireAt = typeof session.expireAt === 'string' ? session.expireAt : new Date(0).toISOString()
  const refreshToken = typeof session.refreshToken === 'string' ? session.refreshToken : null
  const refreshTokenExpireAt = typeof session.refreshTokenExpireAt === 'string' ? session.refreshTokenExpireAt : null
  return {
    ...session,
    expireAt,
    refreshToken,
    refreshTokenExpireAt,
    userInfo: {
      ...session.userInfo,
      nickname: normalizeDisplayText(session.userInfo.nickname),
    },
  }
}

function normalizeStoredAccount(raw: unknown): StoredAccount | null {
  if (!raw || typeof raw !== 'object') return null

  const record = raw as Record<string, unknown>
  const userInfoRecord = typeof record.userInfo === 'object' && record.userInfo ? record.userInfo as Record<string, unknown> : record
  const userId = Number(userInfoRecord.userId)
  const username = String(userInfoRecord.username ?? '')
  const nickname = String(userInfoRecord.nickname ?? '')

  if (!Number.isFinite(userId) || !username || !nickname) return null

  return {
    userInfo: {
      userId,
      username,
      nickname: normalizeDisplayText(nickname),
      avatarUrl: typeof userInfoRecord.avatarUrl === 'string' ? userInfoRecord.avatarUrl : null,
    },
    lastActiveAt: typeof record.lastActiveAt === 'string' ? record.lastActiveAt : new Date(0).toISOString(),
    authenticatedAt: typeof record.authenticatedAt === 'string' ? record.authenticatedAt : null,
    rememberMe: record.rememberMe !== false,
    sessionToken: typeof record.sessionToken === 'string' ? record.sessionToken : null,
    sessionTokenType: typeof record.sessionTokenType === 'string' ? record.sessionTokenType : null,
    sessionExpireAt: typeof record.sessionExpireAt === 'string' ? record.sessionExpireAt : null,
    refreshToken: typeof record.refreshToken === 'string' ? record.refreshToken : null,
    refreshTokenExpireAt: typeof record.refreshTokenExpireAt === 'string' ? record.refreshTokenExpireAt : null,
    trustedDeviceGrantToken: typeof record.trustedDeviceGrantToken === 'string' ? record.trustedDeviceGrantToken : null,
    trustedDeviceExpireAt: typeof record.trustedDeviceExpireAt === 'string' ? record.trustedDeviceExpireAt : null,
    deviceFingerprint: typeof record.deviceFingerprint === 'string' ? record.deviceFingerprint : null,
  }
}

function toStoredAccount(
  session: AuthSession,
  existing: StoredAccount | null,
  meta?: {
    rememberMe?: boolean
    sessionToken?: string | null
    sessionTokenType?: string | null
    sessionExpireAt?: string | null
    refreshToken?: string | null
    refreshTokenExpireAt?: string | null
    trustedDeviceGrantToken?: string | null
    trustedDeviceExpireAt?: string | null
    deviceFingerprint?: string | null
  },
): StoredAccount {
  return {
    userInfo: normalizeSession(session).userInfo,
    lastActiveAt: new Date().toISOString(),
    authenticatedAt: existing?.authenticatedAt ?? new Date().toISOString(),
    rememberMe: meta?.rememberMe ?? existing?.rememberMe ?? true,
    sessionToken: meta?.sessionToken ?? existing?.sessionToken ?? session.token,
    sessionTokenType: meta?.sessionTokenType ?? existing?.sessionTokenType ?? session.tokenType,
    sessionExpireAt: meta?.sessionExpireAt ?? existing?.sessionExpireAt ?? session.expireAt,
    refreshToken: meta?.refreshToken ?? existing?.refreshToken ?? session.refreshToken,
    refreshTokenExpireAt: meta?.refreshTokenExpireAt ?? existing?.refreshTokenExpireAt ?? session.refreshTokenExpireAt,
    trustedDeviceGrantToken: meta?.trustedDeviceGrantToken ?? existing?.trustedDeviceGrantToken ?? null,
    trustedDeviceExpireAt: meta?.trustedDeviceExpireAt ?? existing?.trustedDeviceExpireAt ?? null,
    deviceFingerprint: meta?.deviceFingerprint ?? existing?.deviceFingerprint ?? null,
  }
}

function isStoredSessionUsable(account: StoredAccount) {
  if (!account.sessionToken || !account.sessionTokenType || !account.sessionExpireAt) {
    return false
  }

  const expiresAt = new Date(account.sessionExpireAt).getTime()
  if (!Number.isFinite(expiresAt)) return false
  return Date.now() < expiresAt - 60_000
}

function isStoredRefreshUsable(account: StoredAccount) {
  if (!account.refreshToken || !account.refreshTokenExpireAt) {
    return false
  }

  const expiresAt = new Date(account.refreshTokenExpireAt).getTime()
  if (!Number.isFinite(expiresAt)) return false
  return Date.now() < expiresAt - 60_000
}

function isTrustedDeviceGrantUsable(account: StoredAccount) {
  if (!account.trustedDeviceGrantToken || !account.trustedDeviceExpireAt || !account.deviceFingerprint) {
    return false
  }

  const expiresAt = new Date(account.trustedDeviceExpireAt).getTime()
  if (!Number.isFinite(expiresAt)) return false
  return Date.now() < expiresAt - 60_000
}

function normalizeProfile(profile: CurrentUserProfile): CurrentUserProfile {
  return {
    ...profile,
    nickname: normalizeDisplayText(profile.nickname),
    signature: profile.signature ? normalizeDisplayText(profile.signature) : null,
  }
}

function normalizeLoginFlow(response: LoginFlowResponse): LoginFlowResponse {
  return {
    ...response,
    userInfo: response.userInfo
      ? {
          ...response.userInfo,
          nickname: normalizeDisplayText(response.userInfo.nickname),
        }
      : undefined,
  }
}
