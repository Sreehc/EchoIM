import type {
  ChangePasswordPayload,
  CallIceServer,
  CallSessionSummary,
  CallType,
  ChatFile,
  ConversationType,
  CurrentUserProfile,
  FriendRequestItem,
  MessageForwardSource,
  MessageReplySource,
  MessageType,
  GroupMemberItem,
  GroupCreatePayload,
  GroupCreateResult,
  GroupUpdatePayload,
  FriendListItem,
  MessageReactionStat,
  StickerPayload,
  SpecialConversationType,
  UpdateCurrentUserProfilePayload,
  UserSearchItem,
  UserInfo,
} from './chat'

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

export type ApiCurrentUserProfile = CurrentUserProfile
export type ApiUpdateCurrentUserProfilePayload = UpdateCurrentUserProfilePayload
export type ApiChangePasswordPayload = ChangePasswordPayload
export type ApiCallSessionSummary = CallSessionSummary
export type ApiCallIceServer = CallIceServer

export interface ApiUserPublicProfile {
  userId: number
  userNo: string
  username: string
  nickname: string
  avatarUrl: string | null
  gender: number | null
  signature: string | null
  status: number | null
  friendStatus: string | null
  pendingRequestId: number | null
}

export interface ApiGroupDetail {
  groupId: number
  groupNo: string
  groupName: string
  ownerUserId: number
  avatarUrl: string | null
  notice: string | null
  status: number | null
  memberCount: number | null
  myRole: number | null
  conversationType: ConversationType | null
  canSend: boolean | null
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
  canSend: boolean | null
  myRole: number | null
  archived?: boolean | null
  manualUnread?: boolean | null
  specialType?: SpecialConversationType | null
  folderHints?: string[] | null
}

export interface ApiMessageItem {
  messageId: number
  conversationId: number
  conversationType: ConversationType
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
  recalledAt?: string | null
  edited?: boolean | null
  editedAt?: string | null
  delivered?: boolean | null
  deliveredAt?: string | null
  read?: boolean | null
  readAt?: string | null
  viewCount?: number | null
  forwardSource?: MessageForwardSource | null
  replySource?: MessageReplySource | null
  reactions?: MessageReactionStat[] | null
  sticker?: StickerPayload | null
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
  | 'CALL_INVITE'
  | 'CALL_ACCEPT'
  | 'CALL_REJECT'
  | 'CALL_CANCEL'
  | 'CALL_END'
  | 'CALL_OFFER'
  | 'CALL_ANSWER'
  | 'CALL_ICE_CANDIDATE'
  | 'CALL_STATE'
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

export interface WsCallSummaryPayload extends ApiCallSessionSummary {}

export interface WsCallSignalPayload {
  callId: number
  conversationId: number
  sdp?: string | null
  candidate?: string | null
  sdpMid?: string | null
  sdpMLineIndex?: number | null
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

export interface CreateCallPayload {
  conversationId: number
  callType: CallType
}

export type ApiUserSearchItem = UserSearchItem
export type ApiGroupCreatePayload = GroupCreatePayload
export type ApiGroupCreateResult = GroupCreateResult
export type ApiFriendListItem = FriendListItem
export type ApiFriendRequestItem = FriendRequestItem
export type ApiGroupMemberItem = GroupMemberItem
export type ApiGroupUpdatePayload = GroupUpdatePayload

export interface ApiGlobalSearchResult {
  conversations: ApiConversationItem[]
  users: ApiUserSearchItem[]
  messages: ApiGlobalSearchMessageItem[]
}

export interface ApiGlobalSearchMessageItem {
  messageId: number
  conversationId: number
  conversationType: ConversationType
  conversationName: string
  specialType?: SpecialConversationType | null
  fromUserId: number
  senderName: string
  msgType: MessageType
  preview: string
  sentAt: string | null
  archived: boolean | null
}

export interface ApiUsernameAvailability {
  available: boolean
  username: string
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
