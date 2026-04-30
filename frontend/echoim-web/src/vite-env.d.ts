/// <reference types="vite/client" />

interface ImportMetaEnv {
  readonly VITE_API_BASE_URL?: string
  readonly VITE_WS_URL?: string
  readonly VITE_ENABLE_E2E_HOOKS?: string
}

interface ImportMeta {
  readonly env: ImportMetaEnv
}

interface Window {
  __ECHOIM_E2E__?: {
    getConnectionStatus: () => string
    getCurrentUserId: () => number | null
    getActiveConversationId: () => number | null
    getLeftPanelMode: () => 'conversations' | 'contacts' | 'me' | 'settings'
    listConversations: () => Array<{
      conversationId: number
      conversationName: string
      peerUserId: number | null
      groupId: number | null
      lastMessagePreview: string
      unreadCount: number
      latestSeq: number
    }>
    listMessages: (conversationId: number) => Array<{
      messageId: number
      clientMsgId: string
      content: string | null
      recalled: boolean
      edited: boolean
      seqNo: number
    }>
    openConversation: (conversationId: number) => Promise<void>
    refreshConversationMessages: (conversationId: number) => Promise<void>
    sendTextMessage: (content: string) => Promise<void>
    openLeftPanel: (mode: 'conversations' | 'contacts' | 'me' | 'settings') => void
    dropRealtimeConnection: (pauseReconnect?: boolean) => void
    reconnectRealtime: () => Promise<void>
    findMessageIdByText: (conversationId: number, content: string) => number | null
    editMessage: (messageId: number, content: string) => Promise<void>
    recallMessage: (messageId: number) => Promise<void>
    getErrors: () => Record<string, string | null>
    getWsEvents: () => string[]
  }
}
