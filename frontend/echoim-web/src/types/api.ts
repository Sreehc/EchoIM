import type { ChatFile, ConversationType, MessageType, UserInfo } from './chat'

export interface ApiResponse<T> {
  code: number
  message: string
  data: T
  requestId: string | null
}

export interface PageResponse<T> {
  list: T[]
  pageNo: number
  pageSize: number
  total: number
}

export interface ApiLoginResponse {
  token: string
  tokenType: string
  expiresIn: number
  userInfo: UserInfo
}

export interface ApiConversationItem {
  conversationId: number
  conversationType: ConversationType
  conversationName: string
  avatarUrl: string | null
  lastMessagePreview: string | null
  lastMessageTime: string | null
  unreadCount: number | null
  isTop: number | null
  isMute: number | null
  peerUserId: number | null
  groupId: number | null
  latestSeq: number | null
}

export interface ApiMessageItem {
  messageId: number
  conversationId: number
  seqNo: number
  clientMsgId: string
  fromUserId: number
  toUserId: number | null
  groupId: number | null
  msgType: MessageType
  content: string | null
  fileId: number | null
  file?: ChatFile | null
  sentAt: string | null
  sendStatus: number | null
  recalled?: boolean | null
  edited?: boolean | null
  delivered?: boolean | null
  read?: boolean | null
}

export interface ApiOfflineSyncConversation {
  conversation: ApiConversationItem
  fromSeq: number
  toSeq: number
  hasMore: boolean
  messages: ApiMessageItem[]
}

export interface ApiOfflineSyncResponse {
  conversations: ApiOfflineSyncConversation[]
  hasMore: boolean
}

export interface ApiImInfo {
  mode: string
  transport: string
  status: string
  port: number
  path: string
}

export type WsMessageType =
  | 'AUTH'
  | 'PING'
  | 'PONG'
  | 'CHAT_SINGLE'
  | 'CHAT_GROUP'
  | 'ACK'
  | 'READ'
  | 'MESSAGE_RECALL'
  | 'MESSAGE_EDIT'
  | 'NOTICE'
  | 'CONVERSATION_CHANGE'
  | 'FORCE_OFFLINE'
  | 'OFFLINE_SYNC'

export interface WsEnvelope<T = unknown> {
  type: WsMessageType
  traceId?: string | null
  clientMsgId?: string | null
  timestamp?: number | null
  data: T
}

export interface WsAuthPayload {
  status: 'SUCCESS' | 'FAILED'
  userId?: number
}

export interface WsNoticePayload {
  code: number
  message: string
}

export interface WsPongPayload {
  status: string
}

export interface WsChatPayload {
  message: ApiMessageItem
}

export interface WsAckPayload {
  ackType: 'SEND' | 'DELIVERED'
  status: 'SUCCESS' | 'FAILED'
  duplicate?: boolean
  retryable?: boolean
  code?: number
  errorMessage?: string
  conversationId?: number
  messageId?: number
  seqNo?: number
  userId?: number
  message?: ApiMessageItem | string
}

export interface WsReadPayload {
  status: 'SUCCESS' | 'FAILED'
  conversationId: number
  readerUserId: number
  lastReadSeq: number
}

export interface WsConversationChangePayload {
  changeType: string
  conversation: ApiConversationItem
  message?: ApiMessageItem
}

export interface WsSendSinglePayload {
  conversationId: number
  toUserId: number
  msgType: MessageType
  content: string | null
  fileId: number | null
  extraJson?: unknown
}

export interface WsSendGroupPayload {
  conversationId: number
  groupId: number
  msgType: MessageType
  content: string | null
  fileId: number | null
  extraJson?: unknown
}

export interface WsAckRequestPayload {
  ackType: 'DELIVERED'
  conversationId: number
  messageId: number
  seqNo: number
}

export interface OfflineSyncPoint {
  conversationId: number
  lastSyncSeq: number
}

export interface OfflineSyncRequest {
  defaultLastSyncSeq: number
  syncPoints: OfflineSyncPoint[]
  perConversationLimit: number
  totalLimit: number
}
