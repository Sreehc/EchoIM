import { getJson } from './http'

export interface DashboardOverview {
  totalUsers: number
  newUsersToday: number
  totalMessages: number
  messagesToday: number
  onlineUsers: number
}

export interface TrendItem {
  date: string
  count: number
}

export interface MessageTypeItem {
  name: string
  value: number
}

export interface OnlineStats {
  currentOnline: number
  messagesReceived: number
  messagesSent: number
  connectionsOpened: number
  connectionsClosed: number
  authFailures: number
}

export function fetchDashboardOverview() {
  return getJson<DashboardOverview>('/api/admin/dashboard/overview')
}

export function fetchMessageTrend(days = 7) {
  return getJson<TrendItem[]>(`/api/admin/dashboard/message-trend?days=${days}`)
}

export function fetchUserTrend(days = 7) {
  return getJson<TrendItem[]>(`/api/admin/dashboard/user-trend?days=${days}`)
}

export function fetchMessageTypeBreakdown() {
  return getJson<MessageTypeItem[]>('/api/admin/dashboard/message-types')
}

export function fetchOnlineStats() {
  return getJson<OnlineStats>('/api/admin/dashboard/online-stats')
}
