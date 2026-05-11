import { getJson, postJson, putJson } from './http'
import type { ConfigItem, ConfigRequest } from '@/types/api'

export function fetchConfigs() {
  return getJson<ConfigItem[]>('/api/admin/configs')
}

export function createConfig(data: ConfigRequest) {
  return postJson<ConfigItem>('/api/admin/configs', data)
}

export function updateConfig(id: number, data: ConfigRequest) {
  return putJson<ConfigItem>(`/api/admin/configs/${id}`, data)
}
