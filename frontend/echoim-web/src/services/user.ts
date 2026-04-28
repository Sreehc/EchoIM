import type { ApiCurrentUserProfile, ApiUpdateCurrentUserProfilePayload, ApiUserPublicProfile } from '@/types/api'
import { getJson, putJson } from './http'

export function fetchCurrentUserProfile() {
  return getJson<ApiCurrentUserProfile>('/api/users/me')
}

export function updateCurrentUserProfile(payload: ApiUpdateCurrentUserProfilePayload) {
  return putJson<ApiCurrentUserProfile>('/api/users/me', payload)
}

export function fetchUserPublicProfile(userId: number) {
  return getJson<ApiUserPublicProfile>(`/api/users/${userId}`)
}
