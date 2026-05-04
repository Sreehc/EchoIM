import { postJson } from './http'

export function submitReport(targetType: number, targetId: number, reason: string, description?: string) {
  return postJson<void>('/api/reports', { targetType, targetId, reason, description })
}
