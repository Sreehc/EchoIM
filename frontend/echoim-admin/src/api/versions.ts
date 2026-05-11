import { getJson, postJson, putJson } from './http'
import type { VersionItem, VersionRequest } from '@/types/api'

export function fetchVersions() {
  return getJson<VersionItem[]>('/api/admin/versions')
}

export function createVersion(data: VersionRequest) {
  return postJson<VersionItem>('/api/admin/versions', data)
}

export function updateVersion(id: number, data: VersionRequest) {
  return putJson<VersionItem>(`/api/admin/versions/${id}`, data)
}
