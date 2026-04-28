import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import {
  adaptChatMessage,
  adaptConversationSummary,
  mapOfflineConversation,
  mergeMessages,
  messagePreviewFromMessage,
} from '@/adapters/chat'
import {
  deleteConversation as deleteConversationRequest,
  fetchConversationMessages,
  fetchConversations,
  fetchImInfo,
  markConversationReadRequest,
  syncOfflineMessages,
  updateConversationMute,
  updateConversationTop,
} from '@/services/conversations'
import { fetchGroupDetail } from '@/services/groups'
import { HttpError } from '@/services/http'
import { editMessage as editMessageRequest, recallMessage as recallMessageRequest } from '@/services/messages'
import { EchoWsClient } from '@/services/ws'
import { useAuthStore } from '@/stores/auth'
import { useUiStore } from '@/stores/ui'
import { fetchUserPublicProfile } from '@/services/user'
import { showIncomingMessageNotification } from '@/utils/browserNotifications'
import { normalizeDisplayText } from '@/utils/text'
import type {
  ApiConversationItem,
  ApiGroupDetail,
  ApiMessageItem,
  OfflineSyncRequest,
  ApiUserPublicProfile,
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
  hasOlder: boolean
  loadingOlder: boolean
  olderError: string | null
  profileLoaded: boolean
  profileLoading: boolean
  profileError: string | null
}

const DEFAULT_PAGE_SIZE = 50

export const useChatStore = defineStore('chat', () => {
  const authStore = useAuthStore()
  const uiStore = useUiStore()

  const conversations = ref<ConversationSummary[]>([])
  const messagesByConversation = ref<Record<number, ChatMessage[]>>({})
  const profiles = ref<Record<number, ConversationProfile>>({})
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
  const activeProfileLoading = computed(() =>
    activeConversationId.value ? ensureConversationMeta(activeConversationId.value).profileLoading : false,
  )
  const activeProfileError = computed(() =>
    activeConversationId.value ? ensureConversationMeta(activeConversationId.value).profileError : null,
  )
  const activeHasOlderMessages = computed(() =>
    activeConversationId.value ? ensureConversationMeta(activeConversationId.value).hasOlder : false,
  )
  const activeOlderMessagesLoading = computed(() =>
    activeConversationId.value ? ensureConversationMeta(activeConversationId.value).loadingOlder : false,
  )
  const activeOlderMessagesError = computed(() =>
    activeConversationId.value ? ensureConversationMeta(activeConversationId.value).olderError : null,
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

    void fetchConversationProfile(conversationId).catch(() => undefined)
    await markConversationRead(conversationId)
  }

  async function loadConversationMessages(conversationId: number, force = false) {
    const meta = ensureConversationMeta(conversationId)
    if (!force && meta.loaded) return

    messagesLoading.value = true
    errors.value.messageLoadError = null
    meta.olderError = null

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
      meta.loaded = true
      meta.hasOlder = nextMessages.length >= DEFAULT_PAGE_SIZE
    } catch (error) {
      errors.value.messageLoadError = toErrorMessage(error, '消息加载失败')
      throw error
    } finally {
      messagesLoading.value = false
    }
  }

  async function loadOlderMessages(conversationId: number) {
    const meta = ensureConversationMeta(conversationId)
    const existingMessages = messagesByConversation.value[conversationId] ?? []

    if (!existingMessages.length) {
      await loadConversationMessages(conversationId, true)
      return
    }

    if (meta.loadingOlder || !meta.hasOlder) return

    meta.loadingOlder = true
    meta.olderError = null

    try {
      const oldestSeqNo = existingMessages[0]?.seqNo ?? 0
      const page = await fetchConversationMessages(conversationId, {
        maxSeqNo: Math.max(oldestSeqNo - 1, 0),
        pageSize: DEFAULT_PAGE_SIZE,
      })
      const olderMessages = page.list.map(adaptChatMessage)
      messagesByConversation.value = {
        ...messagesByConversation.value,
        [conversationId]: mergeMessages(existingMessages, olderMessages),
      }
      meta.hasOlder = olderMessages.length >= DEFAULT_PAGE_SIZE
    } catch (error) {
      meta.olderError = toErrorMessage(error, '更早消息加载失败')
      throw error
    } finally {
      meta.loadingOlder = false
    }
  }

  async function fetchConversationProfile(conversationId: number, force = false) {
    const conversation = conversations.value.find((item) => item.conversationId === conversationId)
    if (!conversation) return null

    const meta = ensureConversationMeta(conversationId)
    if (meta.profileLoaded && !force) {
      return profiles.value[conversationId] ?? null
    }
    if (meta.profileLoading) {
      return profiles.value[conversationId] ?? null
    }

    meta.profileLoading = true
    meta.profileError = null

    try {
      const nextProfile =
        conversation.conversationType === 2
          ? await buildGroupConversationProfile(conversation)
          : await buildSingleConversationProfile(conversation)

      profiles.value = {
        ...profiles.value,
        [conversationId]: nextProfile,
      }
      meta.profileLoaded = true
      return nextProfile
    } catch (error) {
      meta.profileError = toErrorMessage(error, '会话详情加载失败')
      throw error
    } finally {
      meta.profileLoading = false
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

  async function deleteConversation(conversationId: number) {
    await deleteConversationRequest(conversationId)

    conversations.value = conversations.value.filter((item) => item.conversationId !== conversationId)
    messagesByConversation.value = omitRecordKey(messagesByConversation.value, conversationId)
    profiles.value = omitRecordKey(profiles.value, conversationId)
    runtimeMeta.value = omitRecordKey(runtimeMeta.value, conversationId)

    if (activeConversationId.value === conversationId) {
      clearActiveConversation()
    }
    if (lastOpenedConversationId.value === conversationId) {
      lastOpenedConversationId.value = null
    }
    errors.value.noticeMessage = '会话已删除'
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

  async function recallMessage(messageId: number) {
    const target = findMessageById(messageId)
    if (!target) return

    errors.value.noticeMessage = null

    try {
      await recallMessageRequest(messageId)
      applyMessageMutation(target.conversationId, {
        ...target.message,
        recalled: true,
        recalledAt: new Date().toISOString(),
        content: '撤回了一条消息',
        edited: false,
      })
    } catch (error) {
      errors.value.noticeMessage = toErrorMessage(error, '撤回消息失败')
      throw error
    }
  }

  async function editMessage(messageId: number, content: string) {
    const nextContent = content.trim()
    if (!nextContent) {
      throw new Error('消息内容不能为空')
    }

    const target = findMessageById(messageId)
    if (!target) return

    errors.value.noticeMessage = null

    try {
      await editMessageRequest(messageId, nextContent)
      applyMessageMutation(target.conversationId, {
        ...target.message,
        content: nextContent,
        edited: true,
        editedAt: new Date().toISOString(),
      })
    } catch (error) {
      errors.value.noticeMessage = toErrorMessage(error, '编辑消息失败')
      throw error
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

    if (envelope.type === 'MESSAGE_RECALL' || envelope.type === 'MESSAGE_EDIT') {
      const payload = envelope.data as { message: ApiMessageItem }
      if (!payload?.message) return
      applyMessageMutation(Number(payload.message.conversationId), adaptChatMessage(payload.message))
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

    if (!payload.message) {
      return
    }

    const message = adaptChatMessage(payload.message)

    if (payload.changeType === 'MESSAGE_RECALL' || payload.changeType === 'MESSAGE_EDIT') {
      applyMessageMutation(message.conversationId, message)
      return
    }

    if (payload.changeType === 'READ_UPDATE') {
      applyMessageMutation(message.conversationId, message)
      return
    }

    if (payload.changeType === 'MESSAGE_NEW' || payload.changeType === 'CONVERSATION_CREATED') {
      mergeIncomingMessage(message)
      return
    }

    applyMessageMutation(message.conversationId, message)
  }

  function mergeIncomingMessage(message: ChatMessage) {
    const conversationId = message.conversationId
    const collection = messagesByConversation.value[conversationId] ?? []
    const isIncomingMessage = message.fromUserId !== authStore.currentUser?.userId

    messagesByConversation.value = {
      ...messagesByConversation.value,
      [conversationId]: mergeMessages(collection, [message]),
    }

    applyLatestMessageToConversation(
      conversationId,
      message,
      isIncomingMessage && activeConversationId.value !== conversationId,
    )

    maybeShowIncomingNotification(message, isIncomingMessage)
  }

  function applyMessageMutation(conversationId: number, message: ChatMessage) {
    const collection = messagesByConversation.value[conversationId] ?? []
    messagesByConversation.value = {
      ...messagesByConversation.value,
      [conversationId]: mergeMessages(collection, [message]),
    }

    const conversation = conversations.value.find((item) => item.conversationId === conversationId)
    if (conversation && message.seqNo >= conversation.latestSeq) {
      conversation.lastMessagePreview = messagePreviewFromMessage(message)
      conversation.lastMessageTime = message.sentAt
      conversation.latestSeq = Math.max(conversation.latestSeq, message.seqNo)
    }
  }

  function maybeShowIncomingNotification(message: ChatMessage, isIncomingMessage: boolean) {
    if (!isIncomingMessage || message.msgType === 'SYSTEM') return
    if (activeConversationId.value === message.conversationId && document.visibilityState === 'visible') return

    const conversation = conversations.value.find((item) => item.conversationId === message.conversationId)
    showIncomingMessageNotification({
      title: conversation?.conversationName ?? '新消息',
      body: messagePreviewFromMessage(message),
      tag: `conversation-${message.conversationId}`,
    })
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

  function findMessageById(messageId: number) {
    for (const [key, collection] of Object.entries(messagesByConversation.value)) {
      const message = collection.find((item) => item.messageId === messageId)
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

    if (message.seqNo >= conversation.latestSeq || conversation.latestSeq === 0) {
      conversation.lastMessagePreview = messagePreviewFromMessage(message)
      conversation.lastMessageTime = message.sentAt
    }
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
        hasOlder: true,
        loadingOlder: false,
        olderError: null,
        profileLoaded: false,
        profileLoading: false,
        profileError: null,
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
  }

  async function buildSingleConversationProfile(conversation: ConversationSummary): Promise<ConversationProfile> {
    if (!conversation.peerUserId) {
      throw new Error('当前单聊会话缺少对端用户标识')
    }

    const profile = await fetchUserPublicProfile(conversation.peerUserId)

    return {
      conversationId: conversation.conversationId,
      conversationType: conversation.conversationType,
      subtitle: [profile.userNo, `@${profile.username}`].filter(Boolean).join(' · '),
      signature: profile.signature ? normalizeDisplayText(profile.signature) : null,
      notice: null,
      fields: compactProfileFields([
        { key: 'user-no', label: '账号编号', value: profile.userNo },
        { key: 'username', label: '用户名', value: `@${profile.username}` },
        { key: 'friend-status', label: '关系状态', value: formatFriendStatus(profile.friendStatus) },
      ]),
      actions: buildProfileActions(conversation),
    }
  }

  async function buildGroupConversationProfile(conversation: ConversationSummary): Promise<ConversationProfile> {
    if (!conversation.groupId) {
      throw new Error('当前群聊会话缺少群组标识')
    }

    const group = await fetchGroupDetail(conversation.groupId)

    return {
      conversationId: conversation.conversationId,
      conversationType: conversation.conversationType,
      subtitle: group.memberCount ? `${group.memberCount} 位成员` : '群聊会话',
      signature: null,
      notice: group.notice ? normalizeDisplayText(group.notice) : null,
      fields: compactProfileFields([
        { key: 'group-no', label: '群号', value: group.groupNo },
        { key: 'member-count', label: '成员数', value: group.memberCount ? `${group.memberCount}` : '' },
        { key: 'my-role', label: '我的角色', value: formatGroupRole(group.myRole) },
        { key: 'owner-user-id', label: '群主 ID', value: group.ownerUserId ? `${group.ownerUserId}` : '' },
      ]),
      actions: buildProfileActions(conversation),
    }
  }

  function buildProfileActions(conversation: ConversationSummary) {
    return [
      { key: 'mute', label: '消息免打扰', value: conversation.isMute ? '已开启' : '关闭' },
      { key: 'top', label: '会话置顶', value: conversation.isTop ? '开启' : '关闭' },
    ]
  }

  function compactProfileFields(fields: ConversationProfile['fields']) {
    return fields.filter((field) => field.value)
  }

  function formatFriendStatus(value: ApiUserPublicProfile['friendStatus']) {
    switch (value) {
      case 'FRIEND':
        return '已是好友'
      case 'PENDING_OUT':
        return '申请已发送'
      case 'PENDING_IN':
        return '等待你处理'
      case 'BLOCKED_OUT':
        return '已拉黑对方'
      case 'BLOCKED_IN':
        return '被对方拉黑'
      case 'SELF':
        return '当前登录用户'
      case 'NONE':
      default:
        return ''
    }
  }

  function formatGroupRole(value: ApiGroupDetail['myRole']) {
    switch (value) {
      case 1:
        return '群主'
      case 3:
        return '管理员'
      case 2:
        return '普通成员'
      default:
        return ''
    }
  }

  function omitRecordKey<T>(record: Record<number, T>, keyToRemove: number): Record<number, T> {
    const nextRecord = { ...record }
    delete nextRecord[keyToRemove]
    return nextRecord
  }

  function resetState() {
    disconnectRealtime()
    uiStore.setConnectionStatus('disconnected')
    conversations.value = []
    messagesByConversation.value = {}
    profiles.value = {}
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
    activeProfileLoading,
    activeProfileError,
    activeHasOlderMessages,
    activeOlderMessagesLoading,
    activeOlderMessagesError,
    bootstrap,
    fetchConversationList,
    openConversation,
    loadConversationMessages,
    loadOlderMessages,
    fetchConversationProfile,
    clearActiveConversation,
    setSearchQuery,
    clearSearchQuery,
    markConversationRead,
    toggleConversationTop,
    toggleConversationMute,
    deleteConversation,
    sendMessage,
    retryMessage,
    recallMessage,
    editMessage,
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
