import { getJson, putJson } from './http'
import type { AdminUserItem, PageResponse } from '@/types/api'

export function fetchUsers(params: { pageNo?: number; pageSize?: number; keyword?: string }) {
  return getJson<PageResponse<AdminUserItem>>('/api/admin/users', { params })
}

export function updateUserStatus(userId: number, status: number) {
  return putJson<{ userId: number; status: number }>(`/api/admin/users/${userId}/status`, { status })
}

export function forceOffline(userId: number) {
  return putJson<void>(`/api/admin/users/${userId}/offline`)
}
