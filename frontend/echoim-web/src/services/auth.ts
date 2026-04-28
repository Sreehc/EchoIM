import type { ApiChangePasswordPayload, ApiLoginResponse } from '@/types/api'
import { postJson } from './http'

export function loginRequest(username: string, password: string) {
  return postJson<ApiLoginResponse>('/api/auth/login', { username, password })
}

export function changePasswordRequest(payload: ApiChangePasswordPayload) {
  return postJson<void>('/api/auth/change-password', payload)
}

export function logoutRequest() {
  return postJson<void>('/api/auth/logout')
}
