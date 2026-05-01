import { postJson } from './http'
import type { AdminLoginRequest, AdminLoginResult } from '@/types/api'

export function adminLogin(data: AdminLoginRequest) {
  return postJson<AdminLoginResult>('/admin/auth/login', data)
}
