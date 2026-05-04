import { computed, ref, watch } from 'vue'
import { defineStore } from 'pinia'
import {
  adaptChatMessage,
  adaptConversationSummary,
  mapOfflineConversation,
  mergeMessages,
  messagePreviewFromMessage,
} from '@/adapters/chat'
import {
  createSavedConversation as createSavedConversationRequest,
  createSingleConversation as createSingleConversationRequest,
  deleteConversation as deleteConversationRequest,
  fetchConversationMessages,
  fetchConversationsByFolder,
  fetchImInfo,
  markConversationUnreadRequest,
  markConversationReadRequest,
  syncOfflineMessages,
  updateConversationArchive,
  updateConversationMute,
  updateConversationTop,
} from '@/services/conversations'
import { createGroup as createGroupRequest, fetchGroupDetail, fetchGroupMembers } from '@/services/groups'
import { getJson, HttpError } from '@/services/http'
import { editMessage as editMessageRequest, reactMessage as reactMessageRequest, recallMessage as recallMessageRequest, deleteMessage as deleteMessageRequest, pinMessage as pinMessageRequest, unpinMessage as unpinMessageRequest } from '@/services/messages'
import { EchoWsClient } from '@/services/ws'
import { useAuthStore } from '@/stores/auth'
import { useCallStore } from '@/stores/call'
import { useUiStore } from '@/stores/ui'
import { fetchUserPublicProfile } from '@/services/user'
import { showIncomingMessageNotification } from '@/utils/browserNotifications'
import { normalizeDisplayText } from '@/utils/text'
import { buildPublicProfilePath } from '@/utils/publicProfiles'
import type {
  ApiConversationItem,
  ApiGroupDetail,
  ApiMessageItem,
  OfflineSyncRequest,
  ApiUserPublicProfile,
  WsAckPayload,
  WsCallSignalPayload,
  WsCallSummaryPayload,
  WsConversationChangePayload,
  WsEnvelope,
  WsNoticePayload,
  WsReadPayload,
} from '@/types/api'
import type {
  ChatErrorState,
  ChatFile,
  ChatMessage,
  ConversationFolder,
  ConversationProfile,
  ConversationSummary,
  GroupCreatePayload,
  GroupGovernanceMeta,
  MentionItem,
  MessageReplySource,
} from '@/types/chat'

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
  const callStore = useCallStore()
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

  // Typing indicator state: conversationId → Set of userIds currently typing
  const typingUsers = ref<Record<number, Set<number>>>({})
  const typingTimers = ref<Record<string, ReturnType<typeof setTimeout>>>({})

  // Online status tracking: userId → online
  const onlineUsers = ref<Set<number>>(new Set())

  // @mention tracking: conversationIds where current user has been @mentioned
  const mentionedConversationIds = ref<Set<number>>(new Set())

  let bootstrapPromise: Promise<void> | null = null
  let wsClient: EchoWsClient | null = null

  watch(
    () => authStore.session?.token ?? null,
    (token) => {
      if (!token) {
        disconnectRealtime()
        return
      }
      void wsClient?.updateToken(token)
    },
  )

  const filteredConversations = computed(() => {
    const keyword = searchQuery.value.trim().toLowerCase()

    return [...conversations.value]
      .filter((item) => {
        if (!matchesConversationFolder(item, uiStore.conversationFolder)) return false
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
    const page = await fetchConversationsByFolder(uiStore.conversationFolder, 1, 100)
    applyConversationPage(page.list)
  }

  async function refreshConversationList(force = false) {
    if (!authStore.isAuthenticated) return
    if (!initialized.value && !force) {
      await bootstrap()
      return
    }

    loading.value = true
    errors.value.bootstrapError = null
    try {
      await fetchConversationList()
    } catch (error) {
      errors.value.bootstrapError = toErrorMessage(error, '会话加载失败')
      throw error
    } finally {
      loading.value = false
    }
  }

  async function openConversation(conversationId: number) {
    activeConversationId.value = conversationId
    lastOpenedConversationId.value = conversationId

    if (mentionedConversationIds.value.has(conversationId)) {
      const next = new Set(mentionedConversationIds.value)
      next.delete(conversationId)
      mentionedConversationIds.value = next
    }

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
        conversation.conversationType === 1
          ? await buildSingleConversationProfile(conversation)
          : await buildCollectiveConversationProfile(conversation)

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

  async function markConversationUnread(conversationId: number) {
    const conversation = conversations.value.find((item) => item.conversationId === conversationId)
    if (!conversation) return

    const previousManualUnread = conversation.manualUnread
    conversation.manualUnread = true

    try {
      await markConversationUnreadRequest(conversationId)
      errors.value.noticeMessage = '已标记为未读'
    } catch (error) {
      conversation.manualUnread = previousManualUnread
      errors.value.noticeMessage = toErrorMessage(error, '标记未读失败')
      throw error
    }
  }

  async function toggleConversationArchive(conversationId: number) {
    const conversation = conversations.value.find((item) => item.conversationId === conversationId)
    if (!conversation) return

    const nextArchived = !conversation.archived
    const previousArchived = conversation.archived
    conversation.archived = nextArchived

    try {
      await updateConversationArchive(conversationId, nextArchived)
      conversations.value = conversations.value.filter((item) => item.conversationId !== conversationId)
      if (activeConversationId.value === conversationId) {
        clearActiveConversation()
      }
      if (lastOpenedConversationId.value === conversationId) {
        lastOpenedConversationId.value = null
      }

      errors.value.noticeMessage = nextArchived ? '会话已归档' : '会话已恢复到收件箱'
    } catch (error) {
      conversation.archived = previousArchived
      errors.value.noticeMessage = toErrorMessage(error, nextArchived ? '归档失败' : '恢复会话失败')
      throw error
    }
  }

  async function markConversationRead(conversationId: number) {
    const conversation = conversations.value.find((item) => item.conversationId === conversationId)
    if (!conversation) return

    const messages = messagesByConversation.value[conversationId] ?? []
    const lastReadSeq = messages[messages.length - 1]?.seqNo ?? conversation.latestSeq ?? 0
    const previousUnread = conversation.unreadCount
    const previousManualUnread = conversation.manualUnread

    conversation.unreadCount = 0
    conversation.manualUnread = false
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
      conversation.manualUnread = previousManualUnread
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

  async function createSingleConversation(targetUserId: number) {
    const item = adaptConversationSummary(await createSingleConversationRequest(targetUserId))
    upsertConversation(item)
    return item
  }

  async function createSavedConversation() {
    const item = adaptConversationSummary(await createSavedConversationRequest())
    upsertConversation(item)
    return item
  }

  async function createGroupConversation(payload: GroupCreatePayload) {
    const result = await createGroupRequest(payload)
    await refreshConversationList(true)
    const item = conversations.value.find((conversation) => conversation.conversationId === result.conversationId) ?? null
    return { result, item }
  }

  async function sendMessage(payload: {
    currentUserId: number
    content: string | null
    msgType?: ChatMessage['msgType']
    fileId?: number | null
    file?: ChatFile | null
    replySource?: MessageReplySource | null
    sticker?: ChatMessage['sticker']
    voice?: ChatMessage['voice']
    mentions?: MentionItem[]
    selfDestructSeconds?: number
  }) {
    const conversation = activeConversation.value
    const nextMsgType = payload.msgType ?? 'TEXT'
    const nextContent = payload.content?.trim() ?? ''

    if (!conversation) return
    if (nextMsgType === 'TEXT' && !nextContent) return
    if ((nextMsgType === 'IMAGE' || nextMsgType === 'GIF' || nextMsgType === 'FILE') && !payload.fileId) return
    if (nextMsgType === 'STICKER' && !payload.sticker?.stickerId) return
    if (nextMsgType === 'VOICE' && !payload.fileId) return
    if (!conversation.canSend) {
      errors.value.noticeMessage = '当前频道仅创建者可发送消息'
      return
    }

    const existing = messagesByConversation.value[conversation.conversationId] ?? []
    const latestSeq = existing[existing.length - 1]?.seqNo ?? conversation.latestSeq ?? 0
    const clientMsgId = `local-${Date.now()}-${Math.random().toString(16).slice(2, 8)}`
    const now = new Date().toISOString()
    errors.value.sendError = null

    const optimisticMessage: ChatMessage = {
      messageId: -(Date.now()),
      conversationId: conversation.conversationId,
      conversationType: conversation.conversationType,
      seqNo: latestSeq + 1,
      clientMsgId,
      fromUserId: payload.currentUserId,
      toUserId: conversation.peerUserId,
      groupId: conversation.groupId,
      msgType: nextMsgType,
      content: nextMsgType === 'TEXT' ? nextContent : payload.file?.fileName ?? (nextContent || null),
      fileId: payload.fileId ?? null,
      file: payload.file ?? null,
      sentAt: now,
      sendStatus: 0,
      delivered: false,
      read: false,
      viewCount: 0,
      replySource: payload.replySource ?? null,
      reactions: [],
      sticker: payload.sticker ?? null,
      voice: nextMsgType === 'VOICE' ? (payload.voice ?? null) : null,
      selfDestructSeconds: payload.selfDestructSeconds ?? null,
      selfDestructAt: null,
      errorMessage: null,
    }

    messagesByConversation.value = {
      ...messagesByConversation.value,
      [conversation.conversationId]: [...existing, optimisticMessage],
    }

    applyLatestMessageToConversation(conversation.conversationId, optimisticMessage, false)

    try {
      sendMessageThroughRealtime(conversation, optimisticMessage, payload.mentions)
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

  async function deleteMessage(messageId: number) {
    const target = findMessageById(messageId)
    if (!target) return

    errors.value.noticeMessage = null

    try {
      await deleteMessageRequest(messageId)
      // Remove the message from the local list
      const conversationMessages = messagesByConversation.value[target.conversationId] ?? []
      messagesByConversation.value = {
        ...messagesByConversation.value,
        [target.conversationId]: conversationMessages.filter(m => m.messageId !== messageId),
      }
    } catch (error) {
      errors.value.noticeMessage = toErrorMessage(error, '删除消息失败')
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

  async function toggleReaction(messageId: number, emoji: string) {
    const target = findMessageById(messageId)
    if (!target) return

    try {
      const nextMessage = adaptChatMessage(await reactMessageRequest(messageId, emoji))
      applyMessageMutation(target.conversationId, nextMessage)
    } catch (error) {
      errors.value.noticeMessage = toErrorMessage(error, '消息反应失败')
      throw error
    }
  }

  async function pinMessage(messageId: number) {
    const target = findMessageById(messageId)
    if (!target) return

    try {
      const nextMessage = adaptChatMessage(await pinMessageRequest(messageId))
      applyMessageMutation(target.conversationId, nextMessage)
    } catch (error) {
      errors.value.noticeMessage = toErrorMessage(error, '置顶消息失败')
      throw error
    }
  }

  async function unpinMessage(messageId: number) {
    const target = findMessageById(messageId)
    if (!target) return

    try {
      const nextMessage = adaptChatMessage(await unpinMessageRequest(messageId))
      applyMessageMutation(target.conversationId, nextMessage)
    } catch (error) {
      errors.value.noticeMessage = toErrorMessage(error, '取消置顶失败')
      throw error
    }
  }

  async function connectRealtime() {
    if (!authStore.session?.token) return
    const currentSession = await authStore.ensureSessionFresh()
    if (!wsClient) {
      wsClient = new EchoWsClient({
        onEnvelope: handleWsEnvelope,
        onStatusChange: (status) => uiStore.setConnectionStatus(status),
        onReconnectReady: handleReconnectReady,
      })
    }
    callStore.attachRealtime(wsClient)

    const info = await fetchImInfo()
    await wsClient.connect({
      wsUrl: info.path || '/ws',
      token: currentSession.token,
      resolveToken: async () => (await authStore.ensureSessionFresh()).token,
    })
  }

  function disconnectRealtime() {
    wsClient?.disconnect()
    wsClient = null
    callStore.attachRealtime(null)
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
      await callStore.syncActiveCall()
    } catch (error) {
      errors.value.syncError = toErrorMessage(error, '离线消息同步失败')
    }
  }

  async function handleWsEnvelope(envelope: WsEnvelope) {
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
      const offlineData = envelope.data as { code?: number; message?: string } | undefined
      const TOKEN_EXPIRED_CODE = 40102

      if (offlineData?.code === TOKEN_EXPIRED_CODE) {
        try {
          await authStore.refreshSession()
          await connectRealtime()
          return
        } catch {
          // refresh failed, fall through to logout
        }
      }

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

    if (envelope.type === 'MESSAGE_DELETE') {
      const payload = envelope.data as { messageId: number; conversationId: number }
      if (!payload?.messageId || !payload?.conversationId) return
      const conversationMessages = messagesByConversation.value[payload.conversationId] ?? []
      messagesByConversation.value = {
        ...messagesByConversation.value,
        [payload.conversationId]: conversationMessages.filter(m => m.messageId !== payload.messageId),
      }
      return
    }

    if (envelope.type === 'MESSAGE_RECALL' || envelope.type === 'MESSAGE_EDIT' || envelope.type === 'MESSAGE_PIN' || envelope.type === 'MESSAGE_UNPIN') {
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
        conversation.manualUnread = false
      }
      return
    }

    if (envelope.type === 'CONVERSATION_CHANGE') {
      handleConversationChange(envelope.data as WsConversationChangePayload)
      return
    }

    if (envelope.type === 'TYPING') {
      const payload = envelope.data as { conversationId: number; userId: number }
      if (payload?.conversationId && payload?.userId) {
        handleTypingEvent(payload.conversationId, payload.userId)
      }
      return
    }

    if (envelope.type === 'USER_ONLINE' || envelope.type === 'USER_OFFLINE') {
      const payload = envelope.data as { userId: number; online: boolean }
      if (payload?.userId) {
        handlePresenceEvent(payload.userId, payload.online)
      }
      return
    }

    if (
      envelope.type === 'CALL_INVITE' ||
      envelope.type === 'CALL_ACCEPT' ||
      envelope.type === 'CALL_REJECT' ||
      envelope.type === 'CALL_CANCEL' ||
      envelope.type === 'CALL_END' ||
      envelope.type === 'CALL_STATE' ||
      envelope.type === 'CALL_OFFER' ||
      envelope.type === 'CALL_ANSWER' ||
      envelope.type === 'CALL_ICE_CANDIDATE'
    ) {
      void callStore.handleWsEvent(
        envelope.type,
        envelope.data as WsCallSummaryPayload | WsCallSignalPayload,
      ).catch((error) => {
        errors.value.noticeMessage = toErrorMessage(error, '通话状态更新失败')
      })
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

  function handleTypingEvent(conversationId: number, userId: number) {
    const key = `${conversationId}:${userId}`
    const current = typingUsers.value[conversationId] ?? new Set<number>()
    current.add(userId)
    typingUsers.value = { ...typingUsers.value, [conversationId]: current }

    // Clear after 3 seconds of inactivity
    if (typingTimers.value[key]) {
      clearTimeout(typingTimers.value[key])
    }
    typingTimers.value[key] = setTimeout(() => {
      const set = typingUsers.value[conversationId]
      if (set) {
        set.delete(userId)
        typingUsers.value = { ...typingUsers.value, [conversationId]: new Set(set) }
      }
      delete typingTimers.value[key]
    }, 3000)
  }

  function sendTyping(conversationId: number) {
    wsClient?.sendTyping(conversationId)
  }

  function getTypingUserIds(conversationId: number): number[] {
    return Array.from(typingUsers.value[conversationId] ?? [])
  }

  function handlePresenceEvent(userId: number, online: boolean) {
    const next = new Set(onlineUsers.value)
    if (online) {
      next.add(userId)
    } else {
      next.delete(userId)
    }
    onlineUsers.value = next
  }

  function isUserOnline(userId: number): boolean {
    return onlineUsers.value.has(userId)
  }

  async function fetchOnlineStatus(userIds: number[]) {
    if (!userIds.length) return
    try {
      const params = userIds.map((id) => `userIds=${id}`).join('&')
      const res = await getJson<Record<number, boolean>>(`/api/im/online-status?${params}`)
      if (res.code === 0 && res.data) {
        const next = new Set(onlineUsers.value)
        for (const [id, online] of Object.entries(res.data)) {
          if (online) {
            next.add(Number(id))
          } else {
            next.delete(Number(id))
          }
        }
        onlineUsers.value = next
      }
    } catch {
      // best-effort; ignore failures
    }
  }

  function handleConversationChange(payload: WsConversationChangePayload) {
    upsertConversation(adaptConversationSummary(payload.conversation))

    if (payload.atMentionedUserIds?.length && authStore.currentUser?.userId) {
      if (payload.atMentionedUserIds.includes(authStore.currentUser.userId)) {
        const next = new Set(mentionedConversationIds.value)
        next.add(payload.conversation.conversationId)
        mentionedConversationIds.value = next
      }
    }

    if (!payload.message) {
      // For MESSAGE_DELETE, the message is null but we still need to handle it
      if (payload.changeType === 'MESSAGE_DELETE' && payload.conversation) {
        // The message was deleted - we'll handle this via the direct WS message
        return
      }
      return
    }

    const message = adaptChatMessage(payload.message)

    if (payload.changeType === 'MESSAGE_RECALL' || payload.changeType === 'MESSAGE_EDIT' || payload.changeType === 'MESSAGE_PIN' || payload.changeType === 'MESSAGE_UNPIN') {
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
              viewCount: Math.max(message.viewCount ?? 0, nextMessage.viewCount ?? 0),
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
    partial: Partial<Pick<ChatMessage, 'delivered' | 'read' | 'viewCount'>>,
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

  function sendMessageThroughRealtime(conversation: ConversationSummary, message: ChatMessage, mentions?: MentionItem[]) {
    if (!wsClient) {
      throw new Error('实时连接尚未建立')
    }

    if (conversation.conversationType === 1) {
      const targetUserId = conversation.specialType === 'SAVED_MESSAGES'
        ? authStore.currentUser?.userId ?? null
        : conversation.peerUserId

      if (!targetUserId) {
        throw new Error('当前单聊会话缺少对端用户标识，暂时无法发送')
      }

      recordDebugEvent(`send:CHAT_SINGLE:${message.clientMsgId}`)
      wsClient.sendSingleMessage(
        {
          conversationId: conversation.conversationId,
          toUserId: targetUserId,
          msgType: message.msgType,
          content: message.content,
          fileId: message.fileId,
          extraJson: buildMessageExtraWithMentions(message, mentions),
        },
        message.clientMsgId,
      )
      return
    }

    if (!conversation.groupId) {
      throw new Error('当前群聊或频道会话缺少群组标识，暂时无法发送')
    }

    if (!conversation.canSend) {
      throw new Error('当前频道仅创建者可发送消息')
    }

    recordDebugEvent(`send:CHAT_GROUP:${message.clientMsgId}`)
    wsClient.sendGroupMessage(
      {
        conversationId: conversation.conversationId,
        groupId: conversation.groupId,
        msgType: message.msgType,
        content: message.content,
        fileId: message.fileId,
        extraJson: buildMessageExtraWithMentions(message, mentions),
      },
      message.clientMsgId,
    )
  }

  function buildMessageExtra(message: ChatMessage) {
    const extra: Record<string, unknown> = {}
    if (message.replySource) {
      extra.replySource = message.replySource
    }
    if (message.sticker) {
      extra.sticker = message.sticker
    }
    if (message.voice) {
      extra.voice = message.voice
    }
    if (message.selfDestructSeconds && message.selfDestructSeconds > 0) {
      extra.selfDestructSeconds = message.selfDestructSeconds
    }
    return Object.keys(extra).length ? extra : undefined
  }

  function buildMessageExtraWithMentions(message: ChatMessage, mentions?: MentionItem[]) {
    const extra = buildMessageExtra(message) ?? {}
    if (mentions && mentions.length > 0) {
      extra.mentions = mentions
    }
    return Object.keys(extra).length ? extra : undefined
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

    // Fetch online status for single-chat peers
    const peerIds = items
      .filter((item) => item.conversationType === 1 && item.peerUserId)
      .map((item) => item.peerUserId as number)
    if (peerIds.length) {
      void fetchOnlineStatus(peerIds)
    }
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

  function matchesConversationFolder(conversation: ConversationSummary, folder: ConversationFolder) {
    if (folder === 'archived') return conversation.archived
    if (folder === 'unread') return !conversation.archived && (conversation.unreadCount > 0 || conversation.manualUnread)
    if (folder === 'single') return !conversation.archived && conversation.conversationType === 1
    if (folder === 'group') return !conversation.archived && conversation.conversationType === 2
    if (folder === 'channel') return !conversation.archived && conversation.conversationType === 3
    return !conversation.archived
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
    if (conversation.specialType === 'SAVED_MESSAGES') {
      const me = authStore.profile ?? authStore.currentUser
      return {
        conversationId: conversation.conversationId,
        conversationType: conversation.conversationType,
        subtitle: '把消息、文件和灵感都存进自己的工作台',
        signature: null,
        notice: 'Saved Messages 会像普通会话一样同步搜索、转发和附件内容。',
        fields: compactProfileFields([
          { key: 'saved-scope', label: '会话类型', value: '专属自聊' },
          { key: 'saved-owner', label: '当前账号', value: me?.nickname ?? '当前用户' },
        ]),
        actions: buildProfileActions(conversation),
        publicProfilePath: null,
        specialType: conversation.specialType,
        group: null,
        members: [],
      }
    }

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
      publicProfilePath: buildPublicProfilePath(profile.username),
      specialType: conversation.specialType,
      group: null,
      members: [],
    }
  }

  async function buildCollectiveConversationProfile(conversation: ConversationSummary): Promise<ConversationProfile> {
    if (!conversation.groupId) {
      throw new Error('当前群聊或频道会话缺少群组标识')
    }

    const group = await fetchGroupDetail(conversation.groupId)
    const members = await fetchGroupMembers(conversation.groupId)
    const isChannel = conversation.conversationType === 3
    const groupMeta: GroupGovernanceMeta = {
      groupId: group.groupId,
      groupNo: group.groupNo,
      groupName: group.groupName,
      ownerUserId: group.ownerUserId,
      notice: group.notice ?? null,
      memberCount: group.memberCount ?? null,
      myRole: group.myRole ?? null,
      conversationType: group.conversationType,
      canSend: group.canSend,
      canEditMeta: group.myRole === 1 || group.myRole === 3,
      canManageMembers: group.myRole === 1 || group.myRole === 3,
      canManageRoles: group.myRole === 1,
      canDissolve: group.myRole === 1,
      canLeave: group.myRole !== 1,
    }

    return {
      conversationId: conversation.conversationId,
      conversationType: conversation.conversationType,
      subtitle: group.memberCount ? `${group.memberCount} 位成员` : isChannel ? '频道会话' : '群聊会话',
      signature: null,
      notice: group.notice ? normalizeDisplayText(group.notice) : null,
      fields: compactProfileFields([
        { key: 'group-no', label: isChannel ? '频道号' : '群号', value: group.groupNo },
        { key: 'member-count', label: '成员数', value: group.memberCount ? `${group.memberCount}` : '' },
        { key: 'my-role', label: '我的角色', value: formatCollectiveRole(group.myRole, isChannel) },
        { key: 'send-permission', label: '发送权限', value: isChannel ? (group.canSend ? '可发送消息' : '仅创建者可发言') : '全体成员可发送' },
        { key: 'owner-user-id', label: isChannel ? '创建者 ID' : '群主 ID', value: group.ownerUserId ? `${group.ownerUserId}` : '' },
      ]),
      actions: buildProfileActions(conversation),
      publicProfilePath: null,
      specialType: conversation.specialType,
      group: groupMeta,
      members,
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

  function formatCollectiveRole(value: ApiGroupDetail['myRole'], isChannel = false) {
    switch (value) {
      case 1:
        return isChannel ? '创建者' : '群主'
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
    callStore.resetState()
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
    refreshConversationList,
    clearActiveConversation,
    setSearchQuery,
    clearSearchQuery,
    markConversationUnread,
    toggleConversationArchive,
    markConversationRead,
    toggleConversationTop,
    toggleConversationMute,
    deleteConversation,
    createSingleConversation,
    createSavedConversation,
    createGroupConversation,
    sendMessage,
    retryMessage,
    recallMessage,
    deleteMessage,
    editMessage,
    toggleReaction,
    pinMessage,
    unpinMessage,
    connectRealtime,
    disconnectRealtime,
    simulateRealtimeDrop,
    reconnectRealtimeNow,
    clearNoticeMessage,
    clearSendError,
    clearSyncError,
    sendTyping,
    getTypingUserIds,
    onlineUsers,
    isUserOnline,
    fetchOnlineStatus,
    mentionedConversationIds,
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
