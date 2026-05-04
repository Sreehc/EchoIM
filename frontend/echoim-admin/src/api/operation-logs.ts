import { getJson } from './http'

export interface OperationLogItem {
  logId: number
  adminUserId: number
  adminNickname: string
  moduleName: string
  actionName: string
  targetType?: string
  targetId?: number
  requestIp?: string
  contentJson?: string
  createdAt: string
}

export interface OperationLogPageResult {
  list: OperationLogItem[]
  pageNo: number
  pageSize: number
  total: number
}

export function fetchOperationLogs(params: { adminUserId?: number; moduleName?: string; pageNo?: number; pageSize?: number }) {
  const query = new URLSearchParams()
  if (params.adminUserId) query.set('adminUserId', String(params.adminUserId))
  if (params.moduleName) query.set('moduleName', params.moduleName)
  if (params.pageNo) query.set('pageNo', String(params.pageNo))
  if (params.pageSize) query.set('pageSize', String(params.pageSize))
  return getJson<OperationLogPageResult>(`/api/admin/operation-logs?${query.toString()}`)
}
