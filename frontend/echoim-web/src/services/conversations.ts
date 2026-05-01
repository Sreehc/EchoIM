import type {
  ApiConversationItem,
  ApiImInfo,
  ApiMessageItem,
  ApiOfflineSyncResponse,
  OfflineSyncRequest,
  PageResponse,
} from '@/types/api'
import type { ConversationFolder } from '@/types/chat'
import { deleteJson, getJson, postJson, putJson } from './http'

export function fetchConversations(pageNo = 1, pageSize = 100) {
  return fetchConversationsByFolder('inbox', pageNo, pageSize)
}

export function fetchConversationsByArchive(archived = false, pageNo = 1, pageSize = 100) {
  return getJson<PageResponse<ApiConversationItem>>(
    `/api/conversations?pageNo=${pageNo}&pageSize=${pageSize}&archived=${archived ? 1 : 0}`,
  )
}

export function fetchConversationsByFolder(folder: ConversationFolder, pageNo = 1, pageSize = 100) {
  return getJson<PageResponse<ApiConversationItem>>(
    `/api/conversations?pageNo=${pageNo}&pageSize=${pageSize}&folder=${encodeURIComponent(folder)}`,
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

export function updateConversationArchive(conversationId: number, archived: boolean) {
  return putJson(`/api/conversations/${conversationId}/archive`, { archived })
}

export function markConversationUnreadRequest(conversationId: number) {
  return putJson(`/api/conversations/${conversationId}/unread`, { unread: true })
}

export function createSingleConversation(targetUserId: number) {
  return postJson<ApiConversationItem>('/api/conversations/single', { targetUserId })
}

export function createSavedConversation() {
  return postJson<ApiConversationItem>('/api/conversations/saved')
}

export interface ConversationFileItem {
  fileId: number
  fileName: string
  fileExt: string
  contentType: string
  fileSize: number
  url: string
  createdAt: string
}

export function fetchConversationFiles(conversationId: number, pageNo = 1, pageSize = 20) {
  return getJson<PageResponse<ConversationFileItem>>(
    `/api/conversations/${conversationId}/files?pageNo=${pageNo}&pageSize=${pageSize}`,
  )
}

export function fetchImInfo() {
  return getJson<ApiImInfo>('/api/im/info')
}

export function syncOfflineMessages(payload: OfflineSyncRequest) {
  return postJson<ApiOfflineSyncResponse>('/api/offline-sync/messages', payload)
}
