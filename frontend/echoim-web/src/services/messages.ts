import { putJson } from './http'

export function recallMessage(messageId: number) {
  return putJson(`/api/messages/${messageId}/recall`)
}

export function editMessage(messageId: number, content: string) {
  return putJson(`/api/messages/${messageId}/edit`, { content })
}
