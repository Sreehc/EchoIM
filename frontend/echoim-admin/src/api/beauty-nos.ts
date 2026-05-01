import { deleteJson, getJson, postJson } from './http'
import type { BeautyNoItem, BeautyNoRequest } from '@/types/api'

export function fetchBeautyNos() {
  return getJson<BeautyNoItem[]>('/admin/beauty-nos')
}

export function createBeautyNo(data: BeautyNoRequest) {
  return postJson<BeautyNoItem>('/admin/beauty-nos', data)
}

export function deleteBeautyNo(id: number) {
  return deleteJson<void>(`/admin/beauty-nos/${id}`)
}
