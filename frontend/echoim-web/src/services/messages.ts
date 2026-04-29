import type { ApiMessageItem } from '@/types/api'
import { postJson, putJson } from './http'

export function recallMessage(messageId: number) {
  return putJson(`/api/messages/${messageId}/recall`)
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
