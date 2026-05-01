import { getJson, putJson } from './http'
import type { AdminUserItem, PageResponse } from '@/types/api'

export function fetchUsers(params: { pageNo?: number; pageSize?: number; keyword?: string }) {
  return getJson<PageResponse<AdminUserItem>>('/admin/users', { params })
}

export function updateUserStatus(userId: number, status: number) {
  return putJson<{ userId: number; status: number }>(`/admin/users/${userId}/status`, { status })
}

export function forceOffline(userId: number) {
  return putJson<void>(`/admin/users/${userId}/offline`)
}
