import type { ApiGroupDetail } from '@/types/api'
import { getJson } from './http'

export function fetchGroupDetail(groupId: number) {
  return getJson<ApiGroupDetail>(`/api/groups/${groupId}`)
}
