import { getJson, putJson } from './http'

export function saveDraft(conversationId: number, draftContent: string) {
  return putJson<void>(`/api/conversations/${conversationId}/draft`, { draftContent })
}

export function loadDraft(conversationId: number) {
  return getJson<{ conversationId: number; draftContent: string }>(`/api/conversations/${conversationId}/draft`)
}
