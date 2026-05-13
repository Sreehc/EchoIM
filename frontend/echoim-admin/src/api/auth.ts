import { postJson } from './http'
import type { AdminLoginRequest, AdminLoginResult } from '@/types/api'

export function adminLogin(data: AdminLoginRequest) {
  return postJson<AdminLoginResult>('/api/admin/auth/login', data)
}

export function adminLogout() {
  return postJson<void>('/api/admin/auth/logout')
}
