import { deleteJson, getJson } from './http'
import type { AdminGroupItem, PageResponse } from '@/types/api'

export function fetchGroups(params: { pageNo?: number; pageSize?: number }) {
  return getJson<PageResponse<AdminGroupItem>>('/admin/groups', { params })
}

export function dissolveGroup(groupId: number) {
  return deleteJson<void>(`/admin/groups/${groupId}`)
}
