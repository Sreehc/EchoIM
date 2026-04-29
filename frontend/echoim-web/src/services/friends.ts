import type { ApiFriendListItem, ApiFriendRequestItem } from '@/types/api'
import { deleteJson, getJson, postJson, putJson } from './http'

export function fetchFriends() {
  return getJson<ApiFriendListItem[]>('/api/friends')
}

export function fetchBlockedFriends() {
  return getJson<ApiFriendListItem[]>('/api/friends/blocked')
}

export function fetchFriendRequests() {
  return getJson<ApiFriendRequestItem[]>('/api/friend-requests')
}

export function createFriendRequest(toUserId: number, applyMsg: string) {
  return postJson<{ requestId: number }>('/api/friend-requests', { toUserId, applyMsg })
}

export function approveFriendRequest(requestId: number) {
  return putJson<void>(`/api/friend-requests/${requestId}/approve`)
}

export function rejectFriendRequest(requestId: number) {
  return putJson<void>(`/api/friend-requests/${requestId}/reject`)
}

export function updateFriendRemark(friendId: number, remark: string) {
  return putJson<void>(`/api/friends/${friendId}/remark`, { remark })
}

export function blockFriend(friendId: number) {
  return putJson<void>(`/api/friends/${friendId}/block`)
}

export function unblockFriend(friendId: number) {
  return putJson<void>(`/api/friends/${friendId}/unblock`)
}

export function deleteFriend(friendId: number) {
  return deleteJson<void>(`/api/friends/${friendId}`)
}
