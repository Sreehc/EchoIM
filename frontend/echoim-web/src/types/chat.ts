export type ThemeMode = 'light' | 'dark'
export type LeftPanelMode = 'conversations' | 'contacts' | 'me' | 'settings'
export type SettingsSection = 'appearance' | 'chat' | 'notifications' | 'security'
export type ConversationFolder = 'inbox' | 'archived' | 'unread' | 'single' | 'group' | 'channel'
export type SpecialConversationType = 'SAVED_MESSAGES'

export interface UserInfo {
  userId: number
  username: string
  nickname: string
  avatarUrl: string | null
}

export interface CurrentUserProfile extends UserInfo {
  userNo: string
  gender: number | null
  phone: string | null
  email: string | null
  signature: string | null
  status: number | null
}

export interface PublicUserProfile {
  username: string
  nickname: string
  avatarUrl: string | null
  signature: string | null
}

export interface UpdateCurrentUserProfilePayload {
  username?: string | null
  nickname: string
  avatarUrl: string | null
  gender: number | null
  signature: string | null
}

export interface ChangePasswordPayload {
  oldPassword: string
  newPassword: string
}

export interface ChatPreferences {
  enterToSend: boolean
  compactList: boolean
  compactBubbles: boolean
}

export interface AuthSession {
  token: string
  tokenType: string
  expiresIn: number
  expireAt: string | null
  refreshToken: string | null
  refreshTokenExpireAt: string | null
  userInfo: UserInfo
}

export interface LoginFlowResponse {
  status: 'authenticated' | 'challenge_required'
  token?: string
  tokenType?: string
  expiresIn?: number
  expireAt?: string | null
  refreshToken?: string | null
  refreshTokenExpireAt?: string | null
  userInfo?: UserInfo
  challengeTicket?: string
  maskedEmail?: string
  resendAfterSeconds?: number
  trustedDeviceGrantToken?: string | null
  trustedDeviceExpireAt?: string | null
}

export interface StoredAccount {
  userInfo: UserInfo
  lastActiveAt: string
  authenticatedAt: string | null
  rememberMe: boolean
  sessionToken: string | null
  sessionTokenType: string | null
  sessionExpireAt: string | null
  refreshToken: string | null
  refreshTokenExpireAt: string | null
  trustedDeviceGrantToken: string | null
  trustedDeviceExpireAt: string | null
  deviceFingerprint: string | null
}

export interface CodeDispatchResult {
  maskedEmail: string
  resendAfterSeconds: number
}

export interface RecoveryAccountSummary extends UserInfo {}

export interface RecoveryVerifyResult {
  recoveryToken: string
  accounts: RecoveryAccountSummary[]
}

export interface TrustedDeviceSummary {
  deviceId: number
  deviceName: string
  deviceFingerprint: string
  expireAt: string | null
  lastUsedAt: string | null
}

export interface SecurityEventSummary {
  eventId: number
  eventType: string
  eventStatus: string
  ip: string | null
  userAgent: string | null
  detail: string | null
  createdAt: string | null
}

export type ConversationType = 1 | 2 | 3
export type MessageType = 'TEXT' | 'STICKER' | 'IMAGE' | 'GIF' | 'FILE' | 'SYSTEM' | 'VOICE'
export type ConnectionStatus = 'disconnected' | 'connecting' | 'ready' | 'reconnecting'
export type CallType = 'audio'
export type CallSessionStatus = 'ringing' | 'accepted' | 'rejected' | 'cancelled' | 'ended' | 'missed' | 'failed'
export type CallEndReason = 'hangup' | 'reject' | 'timeout' | 'busy' | 'offline' | 'error'
export type CallPhase = 'idle' | 'incoming' | 'outgoing' | 'connecting' | 'connected' | 'ended'

export interface CallIceServer {
  urls: string[]
  username?: string | null
  credential?: string | null
}

export interface CallSessionSummary {
  callId: number
  conversationId: number
  callType: CallType
  status: CallSessionStatus
  endReason?: CallEndReason | null
  callerUserId: number
  calleeUserId: number
  peerUserId: number
  peerDisplayName: string
  peerAvatarUrl: string | null
  startedAt: string
  answeredAt?: string | null
  endedAt?: string | null
  durationSeconds: number
  iceServers: CallIceServer[]
}

export interface ChatErrorState {
  bootstrapError: string | null
  messageLoadError: string | null
  sendError: string | null
  syncError: string | null
  noticeMessage: string | null
}

export interface ConversationSummary {
  conversationId: number
  conversationNo: string
  conversationType: ConversationType
  conversationName: string
  avatarUrl: string | null
  lastMessagePreview: string
  lastMessageTime: string
  unreadCount: number
  isTop: number
  isMute: number
  peerUserId: number | null
  groupId: number | null
  latestSeq: number
  canSend: boolean
  groupStatus: number | null
  myRole: number | null
  archived: boolean
  manualUnread: boolean
  specialType?: SpecialConversationType | null
  folderHints?: string[] | null
}

export interface ChatFile {
  fileId: number
  fileName: string
  fileExt: string | null
  contentType: string | null
  fileSize: number | null
  bizType: number | null
  objectKey: string | null
  url: string | null
  downloadUrl: string | null
  expiresIn: number | null
  expireAt: string | null
}

export interface ChatMessage {
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
  sentAt: string
  sendStatus: number
  recalled?: boolean
  recalledAt?: string | null
  edited?: boolean
  editedAt?: string | null
  delivered?: boolean
  deliveredAt?: string | null
  read?: boolean
  readAt?: string | null
  viewCount?: number
  forwardSource?: MessageForwardSource | null
  replySource?: MessageReplySource | null
  reactions?: MessageReactionStat[]
  sticker?: StickerPayload | null
  voice?: VoicePayload | null
  errorMessage?: string | null
}

export interface MessageReactionStat {
  emoji: string
  count: number
  reacted: boolean
}

export interface StickerPayload {
  stickerId: string
  title: string
}

export interface StickerDefinition extends StickerPayload {
  svg: string
  accent: string
}

export interface VoicePayload {
  duration: number
  waveform: number[]
}

export interface MessageForwardSource {
  sourceMessageId: number
  sourceConversationId: number
  sourceSenderId: number
  sourceMsgType: MessageType
  sourcePreview: string | null
}

export interface MessageReplySource {
  sourceMessageId: number
  sourceConversationId: number
  sourceSenderId: number
  sourceMsgType: MessageType
  sourcePreview: string | null
}

export interface UserSearchItem {
  userId: number
  userNo: string
  username: string
  nickname: string
  avatarUrl: string | null
  gender: number | null
  signature: string | null
  friendStatus: string | null
  pendingRequestId: number | null
}

export interface FriendListItem {
  friendUserId: number
  userNo: string
  nickname: string
  avatarUrl: string | null
  remark: string | null
  displayName: string
}

export interface FriendRequestItem {
  requestId: number
  fromUserId: number
  toUserId: number
  applyMsg: string | null
  status: number
  direction: 'INBOUND' | 'OUTBOUND'
  createdAt: string
  userNo: string
  nickname: string
  avatarUrl: string | null
}

export interface GlobalSearchMessageItem {
  messageId: number
  conversationId: number
  conversationType: ConversationType
  conversationName: string
  specialType?: SpecialConversationType | null
  fromUserId: number
  senderName: string
  msgType: MessageType
  preview: string
  sentAt: string
  archived: boolean
}

export interface GlobalSearchResult {
  conversations: ConversationSummary[]
  users: UserSearchItem[]
  messages: GlobalSearchMessageItem[]
}

export interface GroupMemberItem {
  userId: number
  userNo: string
  nickname: string
  avatarUrl: string | null
  role: number
  status: number
}

export interface GroupUpdatePayload {
  groupName?: string
  notice?: string
}

export interface GroupGovernanceMeta {
  groupId: number
  groupNo: string
  groupName: string
  ownerUserId: number
  notice: string | null
  memberCount: number | null
  myRole: number | null
  conversationType: ConversationType | null
  canSend: boolean | null
  canEditMeta: boolean
  canManageMembers: boolean
  canManageRoles: boolean
  canDissolve: boolean
  canLeave: boolean
}

export interface GroupCreatePayload {
  groupName: string
  memberIds: number[]
  conversationType: 2 | 3
}

export interface GroupCreateResult {
  groupId: number
  groupNo: string
  groupName: string
  conversationId: number
  memberCount: number
  conversationType: 2 | 3
}

export interface ProfileAction {
  key: string
  label: string
  value?: string
}

export interface ProfileField {
  key: string
  label: string
  value: string
}

export interface ConversationProfile {
  conversationId: number
  conversationType: ConversationType
  subtitle: string
  signature?: string | null
  notice?: string | null
  fields: ProfileField[]
  actions: ProfileAction[]
  publicProfilePath?: string | null
  specialType?: SpecialConversationType | null
  group?: GroupGovernanceMeta | null
  members?: GroupMemberItem[]
}
