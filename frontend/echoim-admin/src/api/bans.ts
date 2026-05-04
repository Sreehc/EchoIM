import { getJson, postJson, putJson } from './http'

export interface BanItem {
  banId: number
  userId: number
  nickname: string
  userNo: string
  banType: number
  reason: string
  banMinutes?: number
  expireAt?: string
  bannedBy: number
  status: number
  createdAt: string
}

export interface BanPageResult {
  list: BanItem[]
  pageNo: number
  pageSize: number
  total: number
}

export function fetchBans(params: { userId?: number; pageNo?: number; pageSize?: number }) {
  const query = new URLSearchParams()
  if (params.userId) query.set('userId', String(params.userId))
  if (params.pageNo) query.set('pageNo', String(params.pageNo))
  if (params.pageSize) query.set('pageSize', String(params.pageSize))
  return getJson<BanPageResult>(`/api/admin/bans?${query.toString()}`)
}

export function banUser(payload: { userId: number; reason: string; banMinutes?: number }) {
  return postJson<{ banId: number; success: boolean }>('/api/admin/bans', payload)
}

export function unbanUser(banId: number) {
  return putJson<void>(`/api/admin/bans/${banId}/unban`)
}
