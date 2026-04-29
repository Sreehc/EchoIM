import type { ApiGlobalSearchResult } from '@/types/api'
import { getJson } from './http'

export function searchGlobal(keyword: string, limits?: {
  conversationLimit?: number
  userLimit?: number
  messageLimit?: number
}) {
  const params = new URLSearchParams({
    keyword,
    conversationLimit: String(limits?.conversationLimit ?? 8),
    userLimit: String(limits?.userLimit ?? 8),
    messageLimit: String(limits?.messageLimit ?? 12),
  })
  return getJson<ApiGlobalSearchResult>(`/api/search/global?${params.toString()}`)
}
