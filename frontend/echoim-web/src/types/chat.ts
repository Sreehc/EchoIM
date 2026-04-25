export type ThemeMode = 'light' | 'dark'

export interface UserInfo {
  userId: number
  username: string
  nickname: string
  avatarUrl: string | null
}

export interface AuthSession {
  token: string
  tokenType: string
  expiresIn: number
  userInfo: UserInfo
}

export type ConversationType = 1 | 2
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
  edited?: boolean
  delivered?: boolean
  read?: boolean
  errorMessage?: string | null
}

export interface ProfileAction {
  key: string
  label: string
  value?: string
}

export interface ProfileMember {
  id: number
  name: string
  role?: string
}

export interface ConversationProfile {
  conversationId: number
  conversationType: ConversationType
  subtitle: string
  signature: string
  notice?: string
  sharedFilesCount: number
  sharedMediaCount: number
  members?: ProfileMember[]
  actions: ProfileAction[]
}
