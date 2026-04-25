import type { ApiLoginResponse } from '@/types/api'
import { postJson } from './http'

export function loginRequest(username: string, password: string) {
  return postJson<ApiLoginResponse>('/api/auth/login', { username, password })
}
