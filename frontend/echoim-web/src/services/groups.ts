import type {
  ApiGroupCreatePayload,
  ApiGroupCreateResult,
  ApiGroupDetail,
  ApiGroupMemberItem,
  ApiGroupUpdatePayload,
} from '@/types/api'
import { deleteJson, getJson, postJson, putJson } from './http'

export function fetchGroupDetail(groupId: number) {
  return getJson<ApiGroupDetail>(`/api/groups/${groupId}`)
}

export function createGroup(payload: ApiGroupCreatePayload) {
  return postJson<ApiGroupCreateResult>('/api/groups', payload)
}

export function fetchGroupMembers(groupId: number) {
  return getJson<ApiGroupMemberItem[]>(`/api/groups/${groupId}/members`)
}

export function updateGroup(groupId: number, payload: ApiGroupUpdatePayload) {
  return putJson<ApiGroupDetail>(`/api/groups/${groupId}`, payload)
}

export function updateGroupMemberRole(groupId: number, userId: number, role: 2 | 3) {
  return putJson<void>(`/api/groups/${groupId}/members/${userId}/role`, { role })
}

export function addGroupMembers(groupId: number, memberIds: number[]) {
  return postJson<ApiGroupDetail>(`/api/groups/${groupId}/members`, { memberIds })
}

export function removeGroupMember(groupId: number, userId: number) {
  return deleteJson<void>(`/api/groups/${groupId}/members/${userId}`)
}

export function leaveGroup(groupId: number, keepConversation = true) {
  return deleteJson<void>(`/api/groups/${groupId}/members/me?keepConversation=${keepConversation ? 'true' : 'false'}`)
}

export function dissolveGroup(groupId: number) {
  return deleteJson<void>(`/api/groups/${groupId}`)
}
