import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import {
  adaptChatMessage,
  adaptConversationSummary,
  mapOfflineConversation,
  mergeMessages,
  messagePreviewFromMessage,
} from '@/adapters/chat'
import { mockProfiles } from '@/mock/profiles'
import {
  fetchConversationMessages,
  fetchConversations,
  fetchImInfo,
  markConversationReadRequest,
  syncOfflineMessages,
  updateConversationMute,
  updateConversationTop,
} from '@/services/conversations'
import { HttpError } from '@/services/http'
import { EchoWsClient } from '@/services/ws'
import { useAuthStore } from '@/stores/auth'
import { useUiStore } from '@/stores/ui'
import type {
  ApiConversationItem,
  ApiMessageItem,
  OfflineSyncRequest,
  WsAckPayload,
  WsConversationChangePayload,
  WsEnvelope,
  WsNoticePayload,
  WsReadPayload,
} from '@/types/api'
import type { ChatErrorState, ChatMessage, ConversationProfile, ConversationSummary } from '@/types/chat'

interface ConversationRuntimeMeta {
  loaded: boolean
  lastReadSeq: number
}

const DEFAULT_PAGE_SIZE = 50

export const useChatStore = defineStore('chat', () => {
  const authStore = useAuthStore()
  const uiStore = useUiStore()

  const conversations = ref<ConversationSummary[]>([])
  const messagesByConversation = ref<Record<number, ChatMessage[]>>({})
  const profiles = ref<Record<number, ConversationProfile>>(structuredClone(mockProfiles))
  const runtimeMeta = ref<Record<number, ConversationRuntimeMeta>>({})
  const activeConversationId = ref<number | null>(null)
  const lastOpenedConversationId = ref<number | null>(null)
  const searchQuery = ref('')
  const loading = ref(false)
  const messagesLoading = ref(false)
  const errors = ref<ChatErrorState>({
    bootstrapError: null,
    messageLoadError: null,
    sendError: null,
    syncError: null,
    noticeMessage: null,
  })
  const initialized = ref(false)
  const debugEvents = ref<string[]>([])

  let bootstrapPromise: Promise<void> | null = null
  let wsClient: EchoWsClient | null = null

  const filteredConversations = computed(() => {
    const keyword = searchQuery.value.trim().toLowerCase()

    return [...conversations.value]
      .filter((item) => {
        if (!keyword) return true
        return (
          item.conversationName.toLowerCase().includes(keyword) ||
          item.lastMessagePreview.toLowerCase().includes(keyword)
        )
      })
      .sort((left, right) => {
        if (left.isTop !== right.isTop) return right.isTop - left.isTop
        return new Date(right.lastMessageTime).getTime() - new Date(left.lastMessageTime).getTime()
      })
  })

  const activeConversation = computed(
    () => conversations.value.find((item) => item.conversationId === activeConversationId.value) ?? null,
  )

  const activeMessages = computed(() =>
    activeConversationId.value ? messagesByConversation.value[activeConversationId.value] ?? [] : [],
  )

  const activeProfile = computed(() =>
    activeConversationId.value ? profiles.value[activeConversationId.value] ?? null : null,
  )

  const errorMessage = computed(
    () =>
      errors.value.bootstrapError ??
      errors.value.messageLoadError ??
      errors.value.sendError ??
      errors.value.syncError ??
      errors.value.noticeMessage,
  )

  async function bootstrap(force = false) {
    if (!authStore.isAuthenticated) return
    if (initialized.value && !force) {
      if (!wsClient) {
        await connectRealtime()
      }
      return
    }
    if (bootstrapPromise && !force) return bootstrapPromise

    bootstrapPromise = (async () => {
      loading.value = true
      errors.value.bootstrapError = null
      errors.value.noticeMessage = null

      try {
        await fetchConversationList()
        await connectRealtime()
        initialized.value = true
      } catch (error) {
        errors.value.bootstrapError = toErrorMessage(error, '会话加载失败')
        throw error
      } finally {
        loading.value = false
      }
    })()

    try {
      await bootstrapPromise
    } finally {
      bootstrapPromise = null
    }
  }

  async function fetchConversationList() {
    const page = await fetchConversations(1, 100)
    applyConversationPage(page.list)
  }

  async function openConversation(conversationId: number) {
    activeConversationId.value = conversationId
    lastOpenedConversationId.value = conversationId

    if (!runtimeMeta.value[conversationId]?.loaded) {
      await loadConversationMessages(conversationId)
    }

    await markConversationRead(conversationId)
  }

  async function loadConversationMessages(conversationId: number, force = false) {
    if (!force && runtimeMeta.value[conversationId]?.loaded) return

    messagesLoading.value = true
    errors.value.messageLoadError = null

    try {
      const page = await fetchConversationMessages(conversationId, {
        pageNo: 1,
        pageSize: DEFAULT_PAGE_SIZE,
      })
      const nextMessages = page.list.map(adaptChatMessage)
      messagesByConversation.value = {
        ...messagesByConversation.value,
        [conversationId]: nextMessages,
      }
      ensureConversationMeta(conversationId).loaded = true
    } catch (error) {
      errors.value.messageLoadError = toErrorMessage(error, '消息加载失败')
      throw error
    } finally {
      messagesLoading.value = false
    }
  }

  function clearActiveConversation() {
    activeConversationId.value = null
  }

  function setSearchQuery(value: string) {
    searchQuery.value = value
  }

  function clearSearchQuery() {
    searchQuery.value = ''
  }

  async function markConversationRead(conversationId: number) {
    const conversation = conversations.value.find((item) => item.conversationId === conversationId)
    if (!conversation) return

    const messages = messagesByConversation.value[conversationId] ?? []
    const lastReadSeq = messages[messages.length - 1]?.seqNo ?? conversation.latestSeq ?? 0
    const previousUnread = conversation.unreadCount

    conversation.unreadCount = 0
    ensureConversationMeta(conversationId).lastReadSeq = lastReadSeq

    try {
      await markConversationReadRequest(conversationId, lastReadSeq)
      try {
        wsClient?.sendRead(conversationId, lastReadSeq)
      } catch {
        // ignore ws read sync failures; HTTP state is authoritative
      }
      markOwnMessagesRead(conversationId, lastReadSeq)
    } catch (error) {
      conversation.unreadCount = previousUnread
      errors.value.noticeMessage = toErrorMessage(error, '标记已读失败')
      throw error
    }
  }

  async function toggleConversationTop(conversationId: number) {
    const conversation = conversations.value.find((item) => item.conversationId === conversationId)
    if (!conversation) return

    const nextValue = conversation.isTop ? 0 : 1
    const previousValue = conversation.isTop
    conversation.isTop = nextValue
    syncProfileActions(conversationId)

    try {
      await updateConversationTop(conversationId, nextValue)
    } catch (error) {
      conversation.isTop = previousValue
      syncProfileActions(conversationId)
      errors.value.noticeMessage = toErrorMessage(error, '置顶状态更新失败')
      throw error
    }
  }

  async function toggleConversationMute(conversationId: number) {
    const conversation = conversations.value.find((item) => item.conversationId === conversationId)
    if (!conversation) return

    const nextValue = conversation.isMute ? 0 : 1
    const previousValue = conversation.isMute
    conversation.isMute = nextValue
    syncProfileActions(conversationId)

    try {
      await updateConversationMute(conversationId, nextValue)
    } catch (error) {
      conversation.isMute = previousValue
      syncProfileActions(conversationId)
      errors.value.noticeMessage = toErrorMessage(error, '免打扰状态更新失败')
      throw error
    }
  }

  async function sendMessage(payload: { currentUserId: number; content: string }) {
    const conversation = activeConversation.value
    if (!conversation || !payload.content.trim()) return

    const existing = messagesByConversation.value[conversation.conversationId] ?? []
    const latestSeq = existing[existing.length - 1]?.seqNo ?? conversation.latestSeq ?? 0
    const clientMsgId = `local-${Date.now()}-${Math.random().toString(16).slice(2, 8)}`
    const now = new Date().toISOString()
    errors.value.sendError = null

    const optimisticMessage: ChatMessage = {
      messageId: -(Date.now()),
      conversationId: conversation.conversationId,
      seqNo: latestSeq + 1,
      clientMsgId,
      fromUserId: payload.currentUserId,
      toUserId: conversation.peerUserId,
      groupId: conversation.groupId,
      msgType: 'TEXT',
      content: payload.content.trim(),
      fileId: null,
      file: null,
      sentAt: now,
      sendStatus: 0,
      delivered: false,
      read: false,
      errorMessage: null,
    }

    messagesByConversation.value = {
      ...messagesByConversation.value,
      [conversation.conversationId]: [...existing, optimisticMessage],
    }

    applyLatestMessageToConversation(conversation.conversationId, optimisticMessage, false)

    try {
      sendMessageThroughRealtime(conversation, optimisticMessage)
    } catch (error) {
      markMessageFailed(conversation.conversationId, clientMsgId, toErrorMessage(error, '消息发送失败'))
      errors.value.sendError = toErrorMessage(error, '消息发送失败')
    }
  }

  async function retryMessage(clientMsgId: string) {
    const target = findMessageByClientMsgId(clientMsgId)
    if (!target || target.message.sendStatus !== 2) return

    const conversation = conversations.value.find((item) => item.conversationId === target.conversationId)
    if (!conversation) return

    const collection = messagesByConversation.value[target.conversationId] ?? []
    const nextCollection = collection.map((item) =>
      item.clientMsgId === clientMsgId
        ? {
            ...item,
            sendStatus: 0,
            delivered: false,
            read: false,
            errorMessage: null,
          }
        : item,
    )

    messagesByConversation.value = {
      ...messagesByConversation.value,
      [target.conversationId]: nextCollection,
    }
    errors.value.sendError = null

    try {
      const nextMessage = nextCollection.find((item) => item.clientMsgId === clientMsgId)
      if (!nextMessage) return
      sendMessageThroughRealtime(conversation, nextMessage)
    } catch (error) {
      markMessageFailed(target.conversationId, clientMsgId, toErrorMessage(error, '消息发送失败'))
      errors.value.sendError = toErrorMessage(error, '消息发送失败')
    }
  }

  async function connectRealtime() {
    if (!authStore.session?.token) return
    if (!wsClient) {
      wsClient = new EchoWsClient({
        onEnvelope: handleWsEnvelope,
        onStatusChange: (status) => uiStore.setConnectionStatus(status),
        onReconnectReady: handleReconnectReady,
      })
    }

    const info = await fetchImInfo()
    await wsClient.connect({
      wsUrl: info.path || '/ws',
      token: authStore.session.token,
    })
  }

  function disconnectRealtime() {
    wsClient?.disconnect()
    wsClient = null
  }

  async function handleReconnectReady() {
    try {
      errors.value.syncError = null
      const payload: OfflineSyncRequest = {
        defaultLastSyncSeq: 0,
        syncPoints: conversations.value.map((conversation) => {
          const collection = messagesByConversation.value[conversation.conversationId] ?? []
          const latestMessageSeq = collection[collection.length - 1]?.seqNo ?? 0

          return {
            conversationId: conversation.conversationId,
            lastSyncSeq: Math.max(conversation.latestSeq, latestMessageSeq),
          }
        }),
        perConversationLimit: 50,
        totalLimit: 500,
      }

      const response = await syncOfflineMessages(payload)

      for (const item of response.conversations) {
        const mapped = mapOfflineConversation(item)
        upsertConversation(mapped.conversation)
        messagesByConversation.value = {
          ...messagesByConversation.value,
          [mapped.conversation.conversationId]: mergeMessages(
            messagesByConversation.value[mapped.conversation.conversationId] ?? [],
            mapped.messages,
          ),
        }
      }
    } catch (error) {
      errors.value.syncError = toErrorMessage(error, '离线消息同步失败')
    }
  }

  function handleWsEnvelope(envelope: WsEnvelope) {
    if (envelope.type === 'ACK') {
      const payload = envelope.data as WsAckPayload
      recordDebugEvent(`recv:ACK:${payload.ackType}:${payload.status}`)
    } else if (envelope.type === 'READ') {
      const payload = envelope.data as WsReadPayload
      recordDebugEvent(`recv:READ:${payload.conversationId}:${payload.lastReadSeq}`)
    } else {
      recordDebugEvent(`recv:${envelope.type}`)
    }

    if (envelope.type === 'PONG') {
      return
    }

    if (envelope.type === 'NOTICE') {
      errors.value.noticeMessage = (envelope.data as WsNoticePayload).message
      return
    }

    if (envelope.type === 'FORCE_OFFLINE') {
      authStore.clearSession()
      resetState()
      window.location.assign('/login')
      return
    }

    if (envelope.type === 'CHAT_SINGLE' || envelope.type === 'CHAT_GROUP') {
      const payload = envelope.data as { message: ApiMessageItem }
      if (!payload?.message) return
      const message = adaptChatMessage(payload.message)
      mergeIncomingMessage(message)

      if (
        envelope.type === 'CHAT_SINGLE' &&
        message.fromUserId !== authStore.currentUser?.userId &&
        message.messageId > 0
      ) {
        try {
          wsClient?.sendDeliveredAck(
            {
              ackType: 'DELIVERED',
              conversationId: message.conversationId,
              messageId: message.messageId,
              seqNo: message.seqNo,
            },
            envelope.clientMsgId ?? `ack-${message.messageId}`,
          )
          recordDebugEvent(`send:ACK:DELIVERED:${message.messageId}`)
        } catch {
          recordDebugEvent(`sendfail:ACK:DELIVERED:${message.messageId}`)
          // ignore delivered ack failures; reconnect sync will heal state
        }
      }

      if (message.conversationId === activeConversationId.value && message.fromUserId !== authStore.currentUser?.userId) {
        void markConversationRead(message.conversationId).catch(() => undefined)
      }

      return
    }

    if (envelope.type === 'ACK') {
      handleWsAck(envelope.data as WsAckPayload, envelope.clientMsgId ?? null)
      return
    }

    if (envelope.type === 'READ') {
      const payload = envelope.data as WsReadPayload
      markOwnMessagesRead(payload.conversationId, payload.lastReadSeq)
      const conversation = conversations.value.find((item) => item.conversationId === payload.conversationId)
      if (conversation && payload.readerUserId === authStore.currentUser?.userId) {
        conversation.unreadCount = 0
      }
      return
    }

    if (envelope.type === 'CONVERSATION_CHANGE') {
      handleConversationChange(envelope.data as WsConversationChangePayload)
    }
  }

  function handleWsAck(payload: WsAckPayload, clientMsgId: string | null) {
    if (payload.ackType === 'SEND') {
      if (payload.status === 'SUCCESS' && payload.message && typeof payload.message !== 'string') {
        const message = adaptChatMessage(payload.message)
        replacePendingMessage(message.conversationId, clientMsgId || message.clientMsgId, message)
        applyLatestMessageToConversation(message.conversationId, message, false)
        errors.value.sendError = null
        return
      }

      if (payload.status === 'FAILED' && clientMsgId) {
        const conversationId = findConversationIdByClientMsgId(clientMsgId)
        if (conversationId) {
          markMessageFailed(
            conversationId,
            clientMsgId,
            payload.errorMessage || (typeof payload.message === 'string' ? payload.message : '') || '发送失败',
          )
          errors.value.sendError =
            payload.errorMessage || (typeof payload.message === 'string' ? payload.message : '') || '发送失败'
        }
      }
      return
    }

    if (payload.ackType === 'DELIVERED' && payload.conversationId && payload.messageId) {
      updateMessageReceipt(Number(payload.conversationId), Number(payload.messageId), {
        delivered: true,
      }, clientMsgId)
    }
  }

  function handleConversationChange(payload: WsConversationChangePayload) {
    upsertConversation(adaptConversationSummary(payload.conversation))

    if (payload.message) {
      const message = adaptChatMessage(payload.message)
      mergeIncomingMessage(message)
    }
  }

  function mergeIncomingMessage(message: ChatMessage) {
    const conversationId = message.conversationId
    const collection = messagesByConversation.value[conversationId] ?? []

    messagesByConversation.value = {
      ...messagesByConversation.value,
      [conversationId]: mergeMessages(collection, [message]),
    }

    applyLatestMessageToConversation(
      conversationId,
      message,
      message.fromUserId !== authStore.currentUser?.userId && activeConversationId.value !== conversationId,
    )
  }

  function replacePendingMessage(conversationId: number, clientMsgId: string, nextMessage: ChatMessage) {
    const collection = messagesByConversation.value[conversationId] ?? []
    messagesByConversation.value = {
      ...messagesByConversation.value,
      [conversationId]: collection.map((message) =>
        message.clientMsgId === clientMsgId || message.messageId === nextMessage.messageId
          ? {
              ...nextMessage,
              delivered: Boolean(message.delivered || nextMessage.delivered),
              read: Boolean(message.read || nextMessage.read),
              errorMessage: nextMessage.errorMessage ?? message.errorMessage ?? null,
            }
          : message,
      ),
    }
  }

  function markMessageFailed(conversationId: number, clientMsgId: string, message: string) {
    const collection = messagesByConversation.value[conversationId] ?? []
    messagesByConversation.value = {
      ...messagesByConversation.value,
      [conversationId]: collection.map((item) =>
        item.clientMsgId === clientMsgId
          ? {
              ...item,
              sendStatus: 2,
              delivered: false,
              read: false,
              errorMessage: message,
            }
          : item,
      ),
    }
  }

  function updateMessageReceipt(
    conversationId: number,
    messageId: number,
    partial: Pick<ChatMessage, 'delivered' | 'read'>,
    clientMsgId?: string | null,
  ) {
    const collection = messagesByConversation.value[conversationId] ?? []
    messagesByConversation.value = {
      ...messagesByConversation.value,
      [conversationId]: collection.map((item) =>
        item.messageId === messageId || (clientMsgId ? item.clientMsgId === clientMsgId : false)
          ? {
              ...item,
              ...partial,
            }
          : item,
      ),
    }
  }

  function markOwnMessagesRead(conversationId: number, lastReadSeq: number) {
    const collection = messagesByConversation.value[conversationId] ?? []
    const currentUserId = authStore.currentUser?.userId

    if (!currentUserId || !collection.length) return

    messagesByConversation.value = {
      ...messagesByConversation.value,
      [conversationId]: collection.map((message) =>
        message.fromUserId === currentUserId && message.seqNo <= lastReadSeq
          ? {
              ...message,
              delivered: true,
              read: true,
            }
          : message,
      ),
    }
  }

  function sendMessageThroughRealtime(conversation: ConversationSummary, message: ChatMessage) {
    if (!wsClient) {
      throw new Error('实时连接尚未建立')
    }

    if (conversation.conversationType === 1) {
      if (!conversation.peerUserId) {
        throw new Error('当前单聊会话缺少对端用户标识，暂时无法发送')
      }

      recordDebugEvent(`send:CHAT_SINGLE:${message.clientMsgId}`)
      wsClient.sendSingleMessage(
        {
          conversationId: conversation.conversationId,
          toUserId: conversation.peerUserId,
          msgType: message.msgType,
          content: message.content,
          fileId: message.fileId,
        },
        message.clientMsgId,
      )
      return
    }

    if (!conversation.groupId) {
      throw new Error('当前群聊会话缺少群组标识，暂时无法发送')
    }

    recordDebugEvent(`send:CHAT_GROUP:${message.clientMsgId}`)
    wsClient.sendGroupMessage(
      {
        conversationId: conversation.conversationId,
        groupId: conversation.groupId,
        msgType: message.msgType,
        content: message.content,
        fileId: message.fileId,
      },
      message.clientMsgId,
    )
  }

  function findMessageByClientMsgId(clientMsgId: string) {
    for (const [key, collection] of Object.entries(messagesByConversation.value)) {
      const message = collection.find((item) => item.clientMsgId === clientMsgId)
      if (message) {
        return {
          conversationId: Number(key),
          message,
        }
      }
    }

    return null
  }

  function findConversationIdByClientMsgId(clientMsgId: string) {
    return findMessageByClientMsgId(clientMsgId)?.conversationId ?? null
  }

  function applyConversationPage(items: ApiConversationItem[]) {
    conversations.value = items.map((item) => adaptConversationSummary(item))

    syncAllProfiles()
  }

  function upsertConversation(conversation: ConversationSummary) {
    const index = conversations.value.findIndex((item) => item.conversationId === conversation.conversationId)

    if (index === -1) {
      conversations.value = [...conversations.value, conversation]
      syncProfileActions(conversation.conversationId)
      return
    }

    Object.assign(conversations.value[index], conversation)
    syncProfileActions(conversation.conversationId)
  }

  function applyLatestMessageToConversation(conversationId: number, message: ChatMessage, incrementUnread: boolean) {
    const conversation = conversations.value.find((item) => item.conversationId === conversationId)
    if (!conversation) return

    conversation.lastMessagePreview = messagePreviewFromMessage(message)
    conversation.lastMessageTime = message.sentAt
    conversation.latestSeq = Math.max(conversation.latestSeq, message.seqNo)
    if (incrementUnread && message.msgType !== 'SYSTEM') {
      conversation.unreadCount += 1
    }
  }

  function ensureConversationMeta(conversationId: number) {
    if (!runtimeMeta.value[conversationId]) {
      runtimeMeta.value[conversationId] = {
        loaded: false,
        lastReadSeq: 0,
      }
    }

    return runtimeMeta.value[conversationId]
  }

  function syncAllProfiles() {
    for (const conversation of conversations.value) {
      syncProfileActions(conversation.conversationId)
    }
  }

  function syncProfileActions(conversationId: number) {
    const conversation = conversations.value.find((item) => item.conversationId === conversationId)
    const profile = profiles.value[conversationId]
    if (!conversation || !profile) return

    profile.actions = profile.actions.map((action) => {
      if (action.key === 'mute') {
        return { ...action, value: conversation.isMute ? '已开启' : '关闭' }
      }

      if (action.key === 'top') {
        return { ...action, value: conversation.isTop ? '开启' : '关闭' }
      }

      return action
    })

    if (!profile.actions.some((action) => action.key === 'top')) {
      profile.actions = [
        ...profile.actions,
        { key: 'top', label: '会话置顶', value: conversation.isTop ? '开启' : '关闭' },
      ]
    }
  }

  function resetState() {
    disconnectRealtime()
    uiStore.setConnectionStatus('disconnected')
    conversations.value = []
    messagesByConversation.value = {}
    profiles.value = structuredClone(mockProfiles)
    runtimeMeta.value = {}
    activeConversationId.value = null
    lastOpenedConversationId.value = null
    searchQuery.value = ''
    loading.value = false
    messagesLoading.value = false
    errors.value = {
      bootstrapError: null,
      messageLoadError: null,
      sendError: null,
      syncError: null,
      noticeMessage: null,
    }
    debugEvents.value = []
    initialized.value = false
  }

  function recordDebugEvent(event: string) {
    debugEvents.value = [...debugEvents.value.slice(-29), `${new Date().toISOString()} ${event}`]
  }

  function clearNoticeMessage() {
    errors.value.noticeMessage = null
  }

  function clearSendError() {
    errors.value.sendError = null
  }

  function clearSyncError() {
    errors.value.syncError = null
  }

  function simulateRealtimeDrop(pauseReconnect = false) {
    wsClient?.simulateConnectionLossForTest({ pauseReconnect })
  }

  async function reconnectRealtimeNow() {
    if (!wsClient) {
      await connectRealtime()
      return
    }

    await wsClient.reconnectNowForTest()
  }

  return {
    conversations,
    messagesByConversation,
    profiles,
    activeConversationId,
    lastOpenedConversationId,
    searchQuery,
    loading,
    messagesLoading,
    errors,
    debugEvents,
    errorMessage,
    initialized,
    filteredConversations,
    activeConversation,
    activeMessages,
    activeProfile,
    bootstrap,
    fetchConversationList,
    openConversation,
    loadConversationMessages,
    clearActiveConversation,
    setSearchQuery,
    clearSearchQuery,
    markConversationRead,
    toggleConversationTop,
    toggleConversationMute,
    sendMessage,
    retryMessage,
    connectRealtime,
    disconnectRealtime,
    simulateRealtimeDrop,
    reconnectRealtimeNow,
    clearNoticeMessage,
    clearSendError,
    clearSyncError,
    resetState,
  }
})

function toErrorMessage(error: unknown, fallback: string) {
  if (error instanceof HttpError) {
    return error.message || fallback
  }

  if (error instanceof Error) {
    return error.message || fallback
  }

  return fallback
}
