import type {
  ApiGroupCreatePayload,
  ApiGroupCreateResult,
  ApiGroupDetail,
  ApiGroupInviteItem,
  ApiGroupInviteLink,
  ApiGroupJoinRequestItem,
  ApiGroupMemberItem,
  ApiGroupUpdatePayload,
  ApiInvitePreview,
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

// 7.3 邀请链接
export function createInviteLink(groupId: number, payload: { maxUses?: number | null; expireHours?: number | null }) {
  return postJson<ApiGroupInviteLink>(`/api/groups/${groupId}/invites`, payload)
}

export function fetchInviteLinks(groupId: number) {
  return getJson<ApiGroupInviteItem[]>(`/api/groups/${groupId}/invites`)
}

export function revokeInviteLink(groupId: number, inviteId: number) {
  return deleteJson<void>(`/api/groups/${groupId}/invites/${inviteId}`)
}

export function fetchInvitePreview(token: string) {
  return getJson<ApiInvitePreview>(`/api/groups/invite/${token}/preview`)
}

export function joinByInvite(token: string) {
  return postJson<void>(`/api/groups/invite/${token}/join`, {})
}

// 7.4 禁言
export function muteGroupMember(groupId: number, userId: number, durationMinutes?: number | null) {
  return putJson<void>(`/api/groups/${groupId}/members/${userId}/mute`, { durationMinutes })
}

export function unmuteGroupMember(groupId: number, userId: number) {
  return deleteJson<void>(`/api/groups/${groupId}/members/${userId}/mute`)
}

// 7.4 入群审批
export function submitJoinRequest(groupId: number, applyMsg?: string) {
  const query = applyMsg ? `?applyMsg=${encodeURIComponent(applyMsg)}` : ''
  return postJson<void>(`/api/groups/${groupId}/join-requests${query}`, {})
}

export function fetchJoinRequests(groupId: number) {
  return getJson<ApiGroupJoinRequestItem[]>(`/api/groups/${groupId}/join-requests`)
}

export function reviewJoinRequest(groupId: number, requestId: number, approved: boolean) {
  return putJson<void>(`/api/groups/${groupId}/join-requests/${requestId}`, { approved })
}
