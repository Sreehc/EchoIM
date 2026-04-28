import type {
  ApiConversationItem,
  ApiImInfo,
  ApiMessageItem,
  ApiOfflineSyncResponse,
  OfflineSyncRequest,
  PageResponse,
} from '@/types/api'
import { deleteJson, getJson, postJson, putJson } from './http'

export function fetchConversations(pageNo = 1, pageSize = 100) {
  return getJson<PageResponse<ApiConversationItem>>(
    `/api/conversations?pageNo=${pageNo}&pageSize=${pageSize}`,
  )
}

export function fetchConversationMessages(
  conversationId: number,
  options?: {
    pageNo?: number
    pageSize?: number
    maxSeqNo?: number
    afterSeq?: number
  },
) {
  const params = new URLSearchParams({
    pageNo: String(options?.pageNo ?? 1),
    pageSize: String(options?.pageSize ?? 50),
  })

  if (typeof options?.maxSeqNo === 'number') {
    params.set('maxSeqNo', String(options.maxSeqNo))
  }

  if (typeof options?.afterSeq === 'number') {
    params.set('afterSeq', String(options.afterSeq))
  }

  return getJson<PageResponse<ApiMessageItem>>(`/api/conversations/${conversationId}/messages?${params.toString()}`)
}

export function updateConversationTop(conversationId: number, isTop: number) {
  return putJson(`/api/conversations/${conversationId}/top`, { isTop })
}

export function updateConversationMute(conversationId: number, isMute: number) {
  return putJson(`/api/conversations/${conversationId}/mute`, { isMute })
}

export function deleteConversation(conversationId: number) {
  return deleteJson<void>(`/api/conversations/${conversationId}`)
}

export function markConversationReadRequest(conversationId: number, lastReadSeq: number) {
  return putJson(`/api/conversations/${conversationId}/read`, { lastReadSeq })
}

export function fetchImInfo() {
  return getJson<ApiImInfo>('/api/im/info')
}

export function syncOfflineMessages(payload: OfflineSyncRequest) {
  return postJson<ApiOfflineSyncResponse>('/api/offline-sync/messages', payload)
}
