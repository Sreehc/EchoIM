import { getJson, putJson } from './http'

export interface ReportItem {
  reportId: number
  reporterUserId: number
  reporterNickname: string
  targetType: number
  targetId: number
  targetNickname?: string
  reason: string
  description?: string
  status: number
  handledBy?: number
  handledAt?: string
  handleRemark?: string
  createdAt: string
}

export interface ReportPageResult {
  list: ReportItem[]
  pageNo: number
  pageSize: number
  total: number
}

export function fetchReports(params: { status?: number; pageNo?: number; pageSize?: number }) {
  const query = new URLSearchParams()
  if (params.status != null) query.set('status', String(params.status))
  if (params.pageNo) query.set('pageNo', String(params.pageNo))
  if (params.pageSize) query.set('pageSize', String(params.pageSize))
  return getJson<ReportPageResult>(`/api/admin/reports?${query.toString()}`)
}

export function handleReport(id: number, action: number, remark?: string) {
  return putJson<void>(`/api/admin/reports/${id}/handle`, { action, remark })
}
