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
    getActiveConversationId: () => number | null
    getLeftPanelMode: () => 'conversations' | 'me' | 'settings'
    listConversations: () => Array<{
      conversationId: number
      conversationName: string
      peerUserId: number | null
      groupId: number | null
      lastMessagePreview: string
      unreadCount: number
    }>
    openConversation: (conversationId: number) => Promise<void>
    openLeftPanel: (mode: 'conversations' | 'me' | 'settings') => void
    dropRealtimeConnection: (pauseReconnect?: boolean) => void
    reconnectRealtime: () => Promise<void>
    getErrors: () => Record<string, string | null>
    getWsEvents: () => string[]
  }
}
