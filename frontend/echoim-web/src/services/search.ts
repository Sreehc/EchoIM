import type { ApiGlobalSearchResult } from '@/types/api'
import { getJson } from './http'

export function searchGlobal(keyword: string, limits?: {
  conversationLimit?: number
  userLimit?: number
  messageLimit?: number
  msgType?: string
  dateFrom?: string
  dateTo?: string
}) {
  const params = new URLSearchParams({
    keyword,
    conversationLimit: String(limits?.conversationLimit ?? 8),
    userLimit: String(limits?.userLimit ?? 8),
    messageLimit: String(limits?.messageLimit ?? 12),
  })
  if (limits?.msgType) {
    params.set('msgType', limits.msgType)
  }
  if (limits?.dateFrom) {
    params.set('dateFrom', limits.dateFrom)
  }
  if (limits?.dateTo) {
    params.set('dateTo', limits.dateTo)
  }
  return getJson<ApiGlobalSearchResult>(`/api/search/global?${params.toString()}`)
}
