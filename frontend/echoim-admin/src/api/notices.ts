import { getJson, postJson, putJson } from './http'

export interface NoticeItem {
  id: number
  title: string
  content: string
  noticeType: number
  targetUserIds?: string
  status: number
  publishedBy: number
  publishedAt: string
  createdAt: string
}

export interface NoticePageResult {
  list: NoticeItem[]
  pageNo: number
  pageSize: number
  total: number
}

export function fetchNotices(params: { status?: number; pageNo?: number; pageSize?: number }) {
  const query = new URLSearchParams()
  if (params.status != null) query.set('status', String(params.status))
  if (params.pageNo) query.set('pageNo', String(params.pageNo))
  if (params.pageSize) query.set('pageSize', String(params.pageSize))
  return getJson<NoticePageResult>(`/api/admin/notices?${query.toString()}`)
}

export function createNotice(payload: { title: string; content: string; noticeType?: number; targetUserIds?: string }) {
  return postJson<{ noticeId: number; success: boolean }>('/api/admin/notices', payload)
}

export function withdrawNotice(id: number) {
  return putJson<void>(`/api/admin/notices/${id}/withdraw`)
}
