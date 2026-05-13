import type { ApiNoticePageResult, ApiUserNoticeItem } from '@/types/api'
import { getJson, putJson } from './http'

export function fetchNotices(pageNo = 1, pageSize = 20) {
  return getJson<ApiNoticePageResult>(`/api/notices?pageNo=${pageNo}&pageSize=${pageSize}`)
}

export function markNoticeRead(noticeId: number) {
  return putJson<{ noticeId: number; read: boolean }>(`/api/notices/${noticeId}/read`)
}

export type { ApiUserNoticeItem }
