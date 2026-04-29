import type { ChatFile } from '@/types/chat'
import { getJson, postForm } from './http'

export function uploadFile(file: File, bizType?: number) {
  const body = new FormData()
  body.append('file', file)
  if (typeof bizType === 'number') {
    body.append('bizType', String(bizType))
  }
  return postForm<ChatFile>('/api/files/upload', body)
}

export function fetchFileDownload(fileId: number) {
  return getJson<Pick<ChatFile, 'downloadUrl' | 'expireAt' | 'expiresIn'> & { fileId: number }>(
    `/api/files/${fileId}/download`,
  )
}
