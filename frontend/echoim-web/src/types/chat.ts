export type ThemeMode = 'light' | 'dark'
export type LeftPanelMode = 'conversations' | 'me' | 'settings'
export type SettingsSection = 'appearance' | 'chat' | 'security'

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

export interface UpdateCurrentUserProfilePayload {
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
  userInfo: UserInfo
}

export type ConversationType = 1 | 2 | 3
export type MessageType = 'TEXT' | 'IMAGE' | 'FILE' | 'SYSTEM'
export type ConnectionStatus = 'disconnected' | 'connecting' | 'ready' | 'reconnecting'

export interface ChatErrorState {
  bootstrapError: string | null
  messageLoadError: string | null
  sendError: string | null
  syncError: string | null
  noticeMessage: string | null
}

export interface ConversationSummary {
  conversationId: number
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
  myRole: number | null
}

export interface ChatFile {
  fileId: number
  fileName: string
  fileExt: string | null
  contentType: string | null
  fileSize: number | null
  bizType: number | null
  objectKey: string | null
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
  errorMessage?: string | null
}

export interface MessageForwardSource {
  sourceMessageId: number
  sourceConversationId: number
  sourceSenderId: number
  sourceMsgType: MessageType
  sourcePreview: string | null
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
}
