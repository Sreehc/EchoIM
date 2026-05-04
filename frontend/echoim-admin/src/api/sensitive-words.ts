import { getJson, postJson, deleteJson } from './http'

export function fetchSensitiveWords() {
  return getJson<string[]>('/api/admin/sensitive-words')
}

export function addSensitiveWord(word: string, category?: string, level?: number, action?: number) {
  return postJson<{ success: boolean }>('/api/admin/sensitive-words', { word, category, level, action })
}

export function removeSensitiveWord(wordId: number) {
  return deleteJson<{ success: boolean }>(`/api/admin/sensitive-words/${wordId}`)
}

export function reloadSensitiveWordCache() {
  return postJson<{ success: boolean }>('/api/admin/sensitive-words/reload')
}
