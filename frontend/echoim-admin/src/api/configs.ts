import { getJson, postJson, putJson } from './http'
import type { ConfigItem, ConfigRequest } from '@/types/api'

export function fetchConfigs() {
  return getJson<ConfigItem[]>('/admin/configs')
}

export function createConfig(data: ConfigRequest) {
  return postJson<ConfigItem>('/admin/configs', data)
}

export function updateConfig(id: number, data: ConfigRequest) {
  return putJson<ConfigItem>(`/admin/configs/${id}`, data)
}
