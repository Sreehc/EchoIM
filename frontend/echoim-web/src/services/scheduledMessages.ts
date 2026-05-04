import type { ScheduledMessage } from '@/types/chat'
import { deleteJson, getJson, postJson, putJson } from './http'

export function createScheduledMessage(payload: {
  conversationId: number
  msgType: number
  content: string | null
  fileId?: number | null
  extraJson?: unknown
  scheduledAt: string
  mentions?: Array<{ userId: number; displayName: string; startIndex: number; length: number }>
}) {
  return postJson<ScheduledMessage>('/api/scheduled-messages', payload)
}

export function fetchScheduledMessages(conversationId: number) {
  return getJson<ScheduledMessage[]>(`/api/scheduled-messages?conversationId=${conversationId}`)
}

export function cancelScheduledMessage(id: number) {
  return putJson<void>(`/api/scheduled-messages/${id}/cancel`)
}

export function sendScheduledMessageNow(id: number) {
  return putJson<void>(`/api/scheduled-messages/${id}/send-now`)
}
