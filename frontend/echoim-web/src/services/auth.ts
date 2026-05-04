import type {
  ApiCurrentUserProfile,
  ApiChangePasswordPayload,
  ApiCodeDispatchResult,
  ApiLoginResponse,
  ApiRecoveryVerifyResult,
  ApiSecurityEventSummary,
  ApiTrustedDeviceSummary,
} from '@/types/api'
import { deleteJson, getJson, postJson } from './http'

export function loginRequest(payload: {
  username: string
  password: string
  rememberMe: boolean
  trustDevice: boolean
  deviceFingerprint: string
  deviceName: string
}) {
  return postJson<ApiLoginResponse>('/api/auth/login', payload)
}

export function registerRequest(payload: { username: string; password: string; nickname: string }) {
  return postJson<{ userId: number; username: string; nickname: string }>('/api/auth/register', payload)
}

export function verifyLoginChallengeRequest(payload: { challengeTicket: string; code: string }) {
  return postJson<ApiLoginResponse>('/api/auth/login/challenge/verify', payload)
}

export function resendLoginChallengeRequest(payload: { challengeTicket: string }) {
  return postJson<ApiCodeDispatchResult>('/api/auth/login/challenge/resend', payload)
}

export function trustedDeviceLoginRequest(payload: { userId: number; deviceFingerprint: string; grantToken: string }) {
  return postJson<ApiLoginResponse>('/api/auth/trusted-devices/login', payload)
}

export function refreshSessionRequest(payload: { refreshToken: string }) {
  return postJson<ApiLoginResponse>('/api/auth/refresh', payload, { skipAuthRefresh: true })
}

export function changePasswordRequest(payload: ApiChangePasswordPayload) {
  return postJson<void>('/api/auth/change-password', payload)
}

export function logoutRequest(payload?: { refreshToken?: string | null }) {
  return postJson<void>('/api/auth/logout', payload ?? {}, { skipAuthRefresh: true })
}

export function sendRecoveryCodeRequest(payload: { email: string }) {
  return postJson<ApiCodeDispatchResult>('/api/auth/recovery/send-code', payload)
}

export function verifyRecoveryCodeRequest(payload: { email: string; code: string }) {
  return postJson<ApiRecoveryVerifyResult>('/api/auth/recovery/verify-code', payload)
}

export function resetRecoveryPasswordRequest(payload: { recoveryToken: string; newPassword: string }) {
  return postJson<void>('/api/auth/recovery/reset-password', payload)
}

export function sendEmailBindCodeRequest(payload: { email: string; currentPassword: string }) {
  return postJson<ApiCodeDispatchResult>('/api/auth/email/send-bind-code', payload)
}

export function bindEmailRequest(payload: { email: string; code: string; currentPassword: string }) {
  return postJson<ApiCurrentUserProfile>('/api/auth/email/bind', payload)
}

export function fetchTrustedDevicesRequest() {
  return getJson<ApiTrustedDeviceSummary[]>('/api/auth/trusted-devices')
}

export function revokeTrustedDeviceRequest(deviceId: number) {
  return deleteJson<void>(`/api/auth/trusted-devices/${deviceId}`)
}

export function revokeAllTrustedDevicesRequest() {
  return postJson<void>('/api/auth/trusted-devices/revoke-all')
}

export function fetchSecurityEventsRequest() {
  return getJson<ApiSecurityEventSummary[]>('/api/auth/security-events')
}

export function fetchTotpStatusRequest() {
  return getJson<{ enabled: boolean; recoveryCodesRemaining: number }>('/api/auth/totp/status')
}

export function setupTotpRequest() {
  return postJson<{ secret: string; uri: string; recoveryCodes: string[] }>('/api/auth/totp/setup')
}

export function enableTotpRequest(payload: { code: string; secret: string; recoveryCodes: string[] }) {
  return postJson<void>('/api/auth/totp/enable', payload)
}

export function disableTotpRequest(payload: { code: string }) {
  return postJson<void>('/api/auth/totp/disable', payload)
}

export function verifyTotpLoginRequest(payload: { challengeTicket: string; code: string }) {
  return postJson<ApiLoginResponse>('/api/auth/login/totp/verify', payload)
}
