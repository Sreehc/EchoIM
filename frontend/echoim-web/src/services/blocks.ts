import type { ApiBlockedUserItem } from '@/types/api'
import { deleteJson, getJson, postJson } from './http'

export function fetchBlockedUsers() {
  return getJson<ApiBlockedUserItem[]>('/api/blocks')
}

export function blockUser(targetUserId: number) {
  return postJson<void>(`/api/blocks/${targetUserId}`, {})
}

export function unblockUser(targetUserId: number) {
  return deleteJson<void>(`/api/blocks/${targetUserId}`)
}
