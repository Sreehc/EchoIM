import type {
  ApiCurrentUserProfile,
  ApiUpdateCurrentUserProfilePayload,
  ApiUsernameAvailability,
  ApiUserPublicProfile,
  ApiUserSearchItem,
  PageResponse,
} from '@/types/api'
import { getJson, postJson, putJson } from './http'

export function fetchCurrentUserProfile() {
  return getJson<ApiCurrentUserProfile>('/api/users/me')
}

export function updateCurrentUserProfile(payload: ApiUpdateCurrentUserProfilePayload) {
  return putJson<ApiCurrentUserProfile>('/api/users/me', payload)
}

export function fetchUserPublicProfile(userId: number) {
  return getJson<ApiUserPublicProfile>(`/api/users/${userId}`)
}

export function fetchUserPublicProfileByUsername(username: string) {
  return getJson<ApiUserPublicProfile>(`/api/users/by-username/${encodeURIComponent(username)}`)
}

export function searchUsers(keyword: string, pageNo = 1, pageSize = 20) {
  const params = new URLSearchParams({
    keyword,
    pageNo: String(pageNo),
    pageSize: String(pageSize),
  })
  return getJson<PageResponse<ApiUserSearchItem>>(`/api/users/search?${params.toString()}`)
}

export function checkUsernameAvailability(username: string) {
  return postJson<ApiUsernameAvailability>('/api/users/username/check', { username })
}
