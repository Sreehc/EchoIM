import type { ApiMessageItem } from '@/types/api'
import { getJson, postJson, putJson } from './http'

export function recallMessage(messageId: number) {
  return putJson(`/api/messages/${messageId}/recall`)
}

export function deleteMessage(messageId: number) {
  return fetch(`/api/messages/${messageId}`, { method: 'DELETE', credentials: 'include' }).then(r => r.json())
}

export function editMessage(messageId: number, content: string) {
  return putJson(`/api/messages/${messageId}/edit`, { content })
}

export function forwardMessages(messageIds: number[], targetConversationIds: number[]) {
  return postJson<{ forwardedCount: number }>('/api/messages/forward', {
    messageIds,
    targetConversationIds,
  })
}

export function reactMessage(messageId: number, emoji: string) {
  return putJson<ApiMessageItem>(`/api/messages/${messageId}/reaction`, { emoji })
}

export function pinMessage(messageId: number) {
  return putJson<ApiMessageItem>(`/api/messages/${messageId}/pin`)
}

export function unpinMessage(messageId: number) {
  return putJson<ApiMessageItem>(`/api/messages/${messageId}/unpin`)
}

export function listPinnedMessages(conversationId: number) {
  return getJson<ApiMessageItem[]>(`/api/messages/pinned?conversationId=${conversationId}`)
}
