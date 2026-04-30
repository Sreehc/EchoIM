<script setup lang="ts">
import { computed, nextTick, onMounted, onUnmounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import ConversationSidebar from '@/components/chat/ConversationSidebar.vue'
import ContactsPanel from '@/components/chat/ContactsPanel.vue'
import ChatTopbar from '@/components/chat/ChatTopbar.vue'
import CallOverlay from '@/components/chat/CallOverlay.vue'
import MessagePane from '@/components/chat/MessagePane.vue'
import MessageComposer from '@/components/chat/MessageComposer.vue'
import ConversationProfileDrawer from '@/components/chat/ConversationProfileDrawer.vue'
import AvatarBadge from '@/components/chat/AvatarBadge.vue'
import { useAuthStore } from '@/stores/auth'
import { useCallStore } from '@/stores/call'
import { useChatStore } from '@/stores/chat'
import { useUiStore } from '@/stores/ui'
import { uploadFile } from '@/services/files'
import { checkUsernameAvailability, searchUsers } from '@/services/user'
import {
  approveFriendRequest,
  blockFriend,
  createFriendRequest,
  deleteFriend,
  fetchBlockedFriends,
  fetchFriendRequests,
  fetchFriends,
  rejectFriendRequest,
  unblockFriend,
  updateFriendRemark,
} from '@/services/friends'
import { searchGlobal } from '@/services/search'
import {
  addGroupMembers,
  dissolveGroup,
  leaveGroup,
  removeGroupMember,
  updateGroup,
  updateGroupMemberRole,
} from '@/services/groups'
import { forwardMessages } from '@/services/messages'
import { STICKER_LIBRARY } from '@/stickers/library'
import type {
  ChangePasswordPayload,
  ChatMessage,
  ChatFile,
  ConversationSummary,
  FriendListItem,
  FriendRequestItem,
  GlobalSearchMessageItem,
  GroupCreatePayload,
  LeftPanelMode,
  MessageReplySource,
  StickerDefinition,
  UpdateCurrentUserProfilePayload,
  UserSearchItem,
} from '@/types/chat'
import { adaptConversationSummary, adaptGlobalSearchMessage } from '@/adapters/chat'
import { formatConversationTime } from '@/utils/format'
import { buildPublicProfilePath } from '@/utils/publicProfiles'

type ConversationActionCommand = 'open-tab' | 'mark-unread' | 'toggle-top' | 'toggle-mute' | 'archive' | 'delete'
type ComposeAction = 'single' | 'group' | 'channel'
type ContactsTab = 'friends' | 'requests' | 'blocked'

const authStore = useAuthStore()
const callStore = useCallStore()
const chatStore = useChatStore()
const uiStore = useUiStore()
const route = useRoute()
const router = useRouter()
const editingMessageId = ref<number | null>(null)
const editingMessageDraft = ref('')
const messageActionPendingId = ref<number | null>(null)
const messageSearchOpen = ref(false)
const messageSearchQuery = ref('')
const messageSearchMatchCount = ref(0)
const activeMessageSearchIndex = ref(0)
const replyingMessage = ref<ChatMessage | null>(null)
const usernameCheck = reactive<{
  checking: boolean
  available: boolean | null
  message: string | null
}>({
  checking: false,
  available: null,
  message: null,
})
const attachmentUploading = ref(false)
const attachmentError = ref<string | null>(null)
const jumpMessageId = ref<number | null>(null)
const forwardSelectionMode = ref(false)
const selectedForwardMessageIds = ref<number[]>([])
const composeDialog = reactive<{
  visible: boolean
  mode: ComposeAction
  keyword: string
  groupName: string
  users: UserSearchItem[]
  selectedUserIds: number[]
  loading: boolean
  submitting: boolean
  error: string | null
}>({
  visible: false,
  mode: 'single',
  keyword: '',
  groupName: '',
  users: [],
  selectedUserIds: [],
  loading: false,
  submitting: false,
  error: null,
})

const contactsState = reactive<{
  loading: boolean
  error: string | null
  activeTab: ContactsTab
  keyword: string
  friends: FriendListItem[]
  requests: FriendRequestItem[]
  blocked: FriendListItem[]
  addDialogVisible: boolean
  addKeyword: string
  addUsers: UserSearchItem[]
  addLoading: boolean
  addError: string | null
}>({
  loading: false,
  error: null,
  activeTab: 'friends',
  keyword: '',
  friends: [],
  requests: [],
  blocked: [],
  addDialogVisible: false,
  addKeyword: '',
  addUsers: [],
  addLoading: false,
  addError: null,
})
const globalSearchState = reactive<{
  visible: boolean
  keyword: string
  loading: boolean
  error: string | null
  conversations: ConversationSummary[]
  users: UserSearchItem[]
  messages: GlobalSearchMessageItem[]
}>({
  visible: false,
  keyword: '',
  loading: false,
  error: null,
  conversations: [],
  users: [],
  messages: [],
})
const forwardDialog = reactive<{
  visible: boolean
  keyword: string
  selectedConversationIds: number[]
  includeSavedMessages: boolean
  sourceMessageIds: number[]
  submitting: boolean
  error: string | null
}>({
  visible: false,
  keyword: '',
  selectedConversationIds: [],
  includeSavedMessages: false,
  sourceMessageIds: [],
  submitting: false,
  error: null,
})
const forwardSearchInput = ref<{ focus?: () => void } | null>(null)

const shouldShowConversationList = computed(() => !uiStore.isMobile || uiStore.mobileView === 'list')
const shouldShowMainPanel = computed(() => !uiStore.isMobile || uiStore.mobileView === 'chat')
const shouldShowContactsPanel = computed(() => uiStore.leftPanelMode === 'contacts' && shouldShowConversationList.value)
const sidebarErrorMessage = computed(() =>
  uiStore.leftPanelMode === 'conversations' && !chatStore.conversations.length ? chatStore.errors.bootstrapError : null,
)
const currentSidebarScrollTop = computed(() => uiStore.panelScrollTop[uiStore.leftPanelMode])
const forwardCandidateConversations = computed(() => {
  const keyword = forwardDialog.keyword.trim().toLowerCase()
  return chatStore.conversations.filter((conversation) => {
    if (!keyword) return true
    return (
      conversation.conversationName.toLowerCase().includes(keyword) ||
      conversation.lastMessagePreview.toLowerCase().includes(keyword)
    )
  })
})
const selectedForwardMessages = computed(() =>
  selectedForwardMessageIds.value
    .map((messageId) => chatStore.activeMessages.find((message) => message.messageId === messageId) ?? null)
    .filter((message): message is ChatMessage => Boolean(message)),
)
const hasForwardKeyword = computed(() => Boolean(forwardDialog.keyword.trim()))
const forwardTargetCount = computed(
  () => forwardDialog.selectedConversationIds.length + (forwardDialog.includeSavedMessages ? 1 : 0),
)
const forwardEmptyState = computed(() => {
  if (hasForwardKeyword.value) {
    return {
      title: '没有匹配的会话',
      description: '试试搜索会话名称、最近消息，或直接转发到 Saved Messages。',
    }
  }

  return {
    title: '还没有可用目标',
    description: '当前没有可转发的会话时，仍可先存入 Saved Messages。',
  }
})
const connectionStatusLabel = computed(() => {
  if (uiStore.connectionStatus === 'ready') return '实时连接已就绪'
  if (uiStore.connectionStatus === 'reconnecting') return '正在重连'
  if (uiStore.connectionStatus === 'connecting') return '正在连接'
  return chatStore.errorMessage ?? '等待建立实时连接'
})
const hasGlobalSearchKeyword = computed(() => Boolean(globalSearchState.keyword.trim()))
const globalSearchTotal = computed(
  () =>
    globalSearchState.conversations.length +
    globalSearchState.users.length +
    globalSearchState.messages.length,
)
const transientBanner = computed(() => {
  if (chatStore.errors.syncError) {
    return { tone: 'warning', message: chatStore.errors.syncError }
  }

  if (chatStore.errors.noticeMessage) {
    return { tone: 'warning', message: chatStore.errors.noticeMessage }
  }

  if (chatStore.errors.sendError) {
    return { tone: 'error', message: chatStore.errors.sendError }
  }

  if (uiStore.connectionStatus === 'reconnecting') {
    return { tone: 'warning', message: '实时连接已断开，正在尝试恢复。' }
  }

  if (uiStore.connectionStatus === 'connecting') {
    return { tone: 'muted', message: '正在建立实时连接。' }
  }

  return null
})
const liveStatusMessage = computed(
  () =>
    transientBanner.value?.message ??
    (uiStore.connectionStatus === 'ready' ? '实时连接已就绪' : connectionStatusLabel.value),
)
const e2eHooksEnabled = computed(
  () => import.meta.env.DEV || import.meta.env.VITE_ENABLE_E2E_HOOKS === 'true',
)

onMounted(() => {
  uiStore.applyTheme(uiStore.theme)
  uiStore.initializeViewport()
  window.addEventListener('keydown', handleGlobalKeydown)
  registerDebugHooks()
})

onUnmounted(() => {
  window.removeEventListener('keydown', handleGlobalKeydown)
  if (window.__ECHOIM_E2E__) {
    delete window.__ECHOIM_E2E__
  }
})

watch(
  () => route.params.conversationId,
  async (value) => {
    try {
      await chatStore.bootstrap()
      await authStore.ensureCurrentProfile().catch(() => null)
    } catch {
      return
    }

    const conversationId = Number(value)
    if (Number.isFinite(conversationId) && conversationId > 0) {
      try {
        await chatStore.openConversation(conversationId)
      } catch {
        return
      }
      uiStore.setMobileView('chat')
    } else {
      chatStore.clearActiveConversation()
      uiStore.setMobileView('list')
    }
  },
  { immediate: true },
)

watch(
  () => uiStore.conversationFolder,
  async () => {
    try {
      await chatStore.refreshConversationList(true)
    } catch {
      return
    }
  },
)

watch(
  () => chatStore.activeConversationId,
  () => {
    cancelMessageEditing()
    closeMessageSearch()
    clearReplyMessage()
    attachmentError.value = null
    jumpMessageId.value = null
    cancelForwardSelection()
  },
)

watch(
  () => chatStore.activeMessages.map((message) => `${message.messageId}:${message.recalled}:${message.content}`).join('|'),
  () => {
    if (!editingMessageId.value) return
    const targetMessage = chatStore.activeMessages.find((message) => message.messageId === editingMessageId.value)
    if (!targetMessage || targetMessage.recalled) {
      cancelMessageEditing()
    }
  },
)

watch(
  () => contactsState.addKeyword,
  () => {
    if (!contactsState.addDialogVisible) return
    void runAddContactSearch()
  },
)

watch(
  () => globalSearchState.keyword,
  () => {
    if (!globalSearchState.visible) return
    void runGlobalSearch()
  },
)

watch(
  () => forwardDialog.visible,
  async (visible) => {
    if (!visible) return
    await nextTick()
    forwardSearchInput.value?.focus?.()
  },
)

async function selectConversation(conversationId: number) {
  uiStore.setGlobalMenuOpen(false)
  uiStore.setTopbarMenuOpen(false)

  try {
    await chatStore.openConversation(conversationId)
  } catch {
    return
  }

  uiStore.setMobileView('chat')
  router.push(`/chat/${conversationId}`)
}

function handleBack() {
  if (uiStore.globalMenuOpen) {
    uiStore.setGlobalMenuOpen(false)
    return
  }

  if (uiStore.profileOpen) {
    uiStore.setProfileOpen(false)
    return
  }

  uiStore.setTopbarMenuOpen(false)
  chatStore.clearActiveConversation()
  uiStore.setMobileView('list')
  router.push('/chat')
}

function openProfile() {
  uiStore.setGlobalMenuOpen(false)
  uiStore.setTopbarMenuOpen(false)
  uiStore.setProfileOpen(true)
}

function openLeftPanel(mode: LeftPanelMode) {
  uiStore.setGlobalMenuOpen(false)
  uiStore.setTopbarMenuOpen(false)
  uiStore.openLeftPanel(mode)

  if (mode === 'contacts') {
    void loadContacts()
  }

  if (mode === 'conversations' && uiStore.isMobile && !chatStore.activeConversationId) {
    uiStore.setMobileView('list')
  }
}

async function loadContacts(force = false) {
  if (!force && contactsState.loading) return

  contactsState.loading = true
  contactsState.error = null

  try {
    const [friends, requests, blocked] = await Promise.all([
      fetchFriends(),
      fetchFriendRequests(),
      fetchBlockedFriends(),
    ])
    contactsState.friends = friends
    contactsState.requests = requests
    contactsState.blocked = blocked
  } catch (error) {
    contactsState.error = error instanceof Error ? error.message : '联系人加载失败'
  } finally {
    contactsState.loading = false
  }
}

function openAddContactDialog() {
  contactsState.addDialogVisible = true
  contactsState.addKeyword = ''
  contactsState.addUsers = []
  contactsState.addError = null
  void runAddContactSearch()
}

async function runAddContactSearch() {
  contactsState.addLoading = true
  contactsState.addError = null
  try {
    const page = await searchUsers(contactsState.addKeyword, 1, 12)
    contactsState.addUsers = page.list
  } catch (error) {
    contactsState.addError = error instanceof Error ? error.message : '搜索用户失败'
  } finally {
    contactsState.addLoading = false
  }
}

async function submitFriendRequestForUser(userId: number) {
  try {
    await createFriendRequest(userId, '你好，希望加个好友。')
    await loadContacts(true)
    await runAddContactSearch()
    chatStore.clearNoticeMessage()
    chatStore.errors.noticeMessage = '好友申请已发送'
  } catch (error) {
    contactsState.addError = error instanceof Error ? error.message : '好友申请发送失败'
  }
}

async function openSavedMessages() {
  try {
    if (uiStore.conversationFolder !== 'inbox') {
      uiStore.setConversationFolder('inbox')
      await chatStore.refreshConversationList(true)
    }
    const conversation = await chatStore.createSavedConversation()
    await selectConversation(conversation.conversationId)
  } catch {
    return
  }
}

function openGlobalSearch() {
  uiStore.setGlobalMenuOpen(false)
  globalSearchState.visible = true
  globalSearchState.keyword = ''
  globalSearchState.error = null
  globalSearchState.conversations = []
  globalSearchState.users = []
  globalSearchState.messages = []
}

async function runGlobalSearch() {
  const keyword = globalSearchState.keyword.trim()
  if (!keyword) {
    globalSearchState.conversations = []
    globalSearchState.users = []
    globalSearchState.messages = []
    return
  }

  globalSearchState.loading = true
  globalSearchState.error = null
  try {
    const result = await searchGlobal(keyword)
    globalSearchState.conversations = result.conversations.map(adaptConversationSummary)
    globalSearchState.users = result.users
    globalSearchState.messages = result.messages.map(adaptGlobalSearchMessage)
  } catch (error) {
    globalSearchState.error = error instanceof Error ? error.message : '全局搜索失败'
  } finally {
    globalSearchState.loading = false
  }
}

async function handleGlobalConversationSelect(conversation: ConversationSummary) {
  globalSearchState.visible = false
  if (conversation.archived && uiStore.conversationFolder !== 'archived') {
    uiStore.setConversationFolder('archived')
    await chatStore.refreshConversationList(true)
  } else if (!conversation.archived && uiStore.conversationFolder !== 'inbox') {
    uiStore.setConversationFolder('inbox')
    await chatStore.refreshConversationList(true)
  }
  await selectConversation(conversation.conversationId)
}

async function handleGlobalUserSelect(user: UserSearchItem) {
  globalSearchState.visible = false
  if (uiStore.conversationFolder !== 'inbox') {
    uiStore.setConversationFolder('inbox')
    await chatStore.refreshConversationList(true)
  }
  const conversation = await chatStore.createSingleConversation(user.userId)
  await selectConversation(conversation.conversationId)
}

async function handleGlobalMessageSelect(message: GlobalSearchMessageItem) {
  globalSearchState.visible = false
  jumpMessageId.value = message.messageId
  await handleGlobalConversationSelect({
    conversationId: message.conversationId,
    conversationType: message.conversationType,
    conversationName: message.conversationName,
    avatarUrl: null,
    lastMessagePreview: message.preview,
    lastMessageTime: message.sentAt,
    unreadCount: 0,
    isTop: 0,
    isMute: 0,
    peerUserId: null,
    groupId: null,
    latestSeq: 0,
    canSend: true,
    myRole: null,
    archived: message.archived,
    manualUnread: false,
    specialType: message.specialType ?? null,
  })
}

function openForwardDialogForMessages(messages: ChatMessage[]) {
  if (!messages.length) return
  forwardDialog.visible = true
  forwardDialog.keyword = ''
  forwardDialog.selectedConversationIds = []
  forwardDialog.includeSavedMessages = false
  forwardDialog.sourceMessageIds = messages.map((message) => message.messageId)
  forwardDialog.submitting = false
  forwardDialog.error = null
}

function handleForwardMessage(message: ChatMessage) {
  openForwardDialogForMessages([message])
}

function toggleForwardSelection(message: ChatMessage) {
  if (selectedForwardMessageIds.value.includes(message.messageId)) {
    selectedForwardMessageIds.value = selectedForwardMessageIds.value.filter((item) => item !== message.messageId)
    return
  }

  selectedForwardMessageIds.value = [...selectedForwardMessageIds.value, message.messageId]
}

function startForwardSelection() {
  forwardSelectionMode.value = true
  selectedForwardMessageIds.value = []
}

function cancelForwardSelection() {
  forwardSelectionMode.value = false
  selectedForwardMessageIds.value = []
}

async function submitForwardDialog() {
  forwardDialog.submitting = true
  forwardDialog.error = null

  try {
    const targetConversationIds = [...forwardDialog.selectedConversationIds]
    if (forwardDialog.includeSavedMessages) {
      const savedConversation = await chatStore.createSavedConversation()
      targetConversationIds.push(savedConversation.conversationId)
    }
    if (!forwardDialog.sourceMessageIds.length || !targetConversationIds.length) {
      throw new Error('请选择至少一条消息和一个目标会话')
    }
    await forwardMessages(forwardDialog.sourceMessageIds, targetConversationIds)
    forwardDialog.visible = false
    cancelForwardSelection()
    chatStore.errors.noticeMessage = `已转发 ${forwardDialog.sourceMessageIds.length} 条消息`
  } catch (error) {
    forwardDialog.error = error instanceof Error ? error.message : '消息转发失败'
  } finally {
    forwardDialog.submitting = false
  }
}

async function openChatFromContact(userId: number) {
  const conversation = await chatStore.createSingleConversation(userId)
  uiStore.returnToConversationList()
  await selectConversation(conversation.conversationId)
}

async function handleApproveRequest(requestId: number) {
  await approveFriendRequest(requestId)
  await loadContacts(true)
}

async function handleRejectRequest(requestId: number) {
  await rejectFriendRequest(requestId)
  await loadContacts(true)
}

async function handleUpdateFriendRemark(friend: FriendListItem) {
  const { value } = await ElMessageBox.prompt('输入新的联系人备注', '修改备注', {
    inputValue: friend.remark ?? friend.displayName,
    confirmButtonText: '保存',
    cancelButtonText: '取消',
  }).catch(() => ({ value: null }))
  if (value == null) return
  await updateFriendRemark(friend.friendUserId, value)
  await loadContacts(true)
}

async function handleBlockFriend(friendId: number) {
  try {
    await ElMessageBox.confirm('拉黑后将限制新的私聊与好友申请。', '拉黑联系人', {
      confirmButtonText: '拉黑',
      cancelButtonText: '取消',
      type: 'warning',
    })
  } catch {
    return
  }
  await blockFriend(friendId)
  await loadContacts(true)
}

async function handleUnblockFriend(friendId: number) {
  await unblockFriend(friendId)
  await loadContacts(true)
}

async function handleDeleteFriend(friendId: number) {
  try {
    await ElMessageBox.confirm('删除后双方好友关系会解除。', '删除好友', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning',
    })
  } catch {
    return
  }
  await deleteFriend(friendId)
  await loadContacts(true)
}

async function handleUpdateGroupMeta() {
  if (!chatStore.activeConversation?.groupId || !chatStore.activeProfile?.group) return

  const { value } = await ElMessageBox.prompt('输入新的群名或频道名', '更新名称', {
    inputValue: chatStore.activeProfile.group.groupName,
    confirmButtonText: '保存',
    cancelButtonText: '取消',
  }).catch(() => ({ value: null }))
  if (!value) return
  await updateGroup(chatStore.activeConversation.groupId, { groupName: value })
  await chatStore.fetchConversationProfile(chatStore.activeConversation.conversationId, true)
}

async function handleUpdateGroupNotice() {
  if (!chatStore.activeConversation?.groupId || !chatStore.activeProfile?.group) return

  const { value } = await ElMessageBox.prompt('输入公告内容', '更新公告', {
    inputValue: chatStore.activeProfile.group.notice ?? '',
    confirmButtonText: '保存',
    cancelButtonText: '取消',
    inputType: 'textarea',
  }).catch(() => ({ value: null }))
  if (value == null) return
  await updateGroup(chatStore.activeConversation.groupId, { notice: value })
  await chatStore.fetchConversationProfile(chatStore.activeConversation.conversationId, true)
}

async function handlePromoteGroupMember(userId: number, role: 2 | 3) {
  if (!chatStore.activeConversation?.groupId) return
  await updateGroupMemberRole(chatStore.activeConversation.groupId, userId, role)
  await chatStore.fetchConversationProfile(chatStore.activeConversation.conversationId, true)
}

async function handleRemoveGroupMember(userId: number) {
  if (!chatStore.activeConversation?.groupId) return
  await removeGroupMember(chatStore.activeConversation.groupId, userId)
  await chatStore.fetchConversationProfile(chatStore.activeConversation.conversationId, true)
}

async function handleAddGroupMembers() {
  if (!chatStore.activeConversation?.groupId) return

  const page = await searchUsers('', 1, 20)
  const memberIds = page.list
    .filter((user) => !chatStore.activeProfile?.members?.some((member) => member.userId === user.userId))
    .slice(0, 3)
    .map((user) => user.userId)
  if (!memberIds.length) return
  await addGroupMembers(chatStore.activeConversation.groupId, memberIds)
  await chatStore.fetchConversationProfile(chatStore.activeConversation.conversationId, true)
}

async function handleLeaveActiveGroup() {
  if (!chatStore.activeConversation?.groupId) return
  await leaveGroup(chatStore.activeConversation.groupId, true)
  uiStore.setProfileOpen(false)
  await chatStore.refreshConversationList(true)
  await router.push('/chat')
}

async function handleDissolveActiveGroup() {
  if (!chatStore.activeConversation?.groupId) return
  await dissolveGroup(chatStore.activeConversation.groupId)
  uiStore.setProfileOpen(false)
  await chatStore.refreshConversationList(true)
  await router.push('/chat')
}

async function saveProfile(payload: UpdateCurrentUserProfilePayload) {
  try {
    await authStore.saveCurrentProfile(payload)
  } catch {
    return
  }
}

async function changePassword(payload: ChangePasswordPayload) {
  try {
    await authStore.changePassword(payload)
  } catch {
    return
  }
}

async function handleConversationAction(command: 'toggle-top' | 'toggle-mute' | 'mark-read' | 'delete') {
  const conversationId = chatStore.activeConversationId
  if (!conversationId) return

  await runConversationAction(command, conversationId)
  uiStore.setTopbarMenuOpen(false)
}

async function handleStartCall() {
  if (!chatStore.activeConversation) return
  try {
    await callStore.startOutgoingCall(chatStore.activeConversation)
  } catch (error) {
    chatStore.errors.noticeMessage = error instanceof Error ? error.message : '发起语音通话失败'
  }
}

async function runConversationAction(
  command: 'toggle-top' | 'toggle-mute' | 'mark-read' | 'delete',
  conversationId: number,
) {
  try {
    if (command === 'toggle-top') {
      await chatStore.toggleConversationTop(conversationId)
    } else if (command === 'toggle-mute') {
      await chatStore.toggleConversationMute(conversationId)
    } else if (command === 'delete') {
      await ElMessageBox.confirm('删除后会从当前列表隐藏该会话，后续有新消息时会再次恢复。', '删除会话', {
        confirmButtonText: '删除',
        cancelButtonText: '取消',
        type: 'warning',
      })
      await chatStore.deleteConversation(conversationId)
      uiStore.setProfileOpen(false)
      uiStore.setMobileView('list')
      await router.push('/chat')
    } else {
      await chatStore.markConversationRead(conversationId)
    }
  } catch {
    return
  }
}

async function handleConversationContextAction(payload: { command: ConversationActionCommand; conversationId: number }) {
  if (payload.command === 'open-tab') {
    const target = router.resolve({ name: 'chat-home', params: { conversationId: payload.conversationId } })
    window.open(target.href, '_blank', 'noopener')
    return
  }

  if (payload.command === 'mark-unread') {
    try {
      await chatStore.markConversationUnread(payload.conversationId)
      if (chatStore.activeConversationId === payload.conversationId) {
        chatStore.clearActiveConversation()
        uiStore.setProfileOpen(false)
        uiStore.setMobileView('list')
        await router.push('/chat')
      }
    } catch {
      return
    }
    return
  }

  if (payload.command === 'archive') {
    try {
      await chatStore.toggleConversationArchive(payload.conversationId)
      if (chatStore.activeConversationId === payload.conversationId) {
        uiStore.setProfileOpen(false)
        uiStore.setMobileView('list')
        await router.push('/chat')
      }
    } catch {
      return
    }
    return
  }

  await runConversationAction(payload.command, payload.conversationId)
}

function openComposeDialog(mode: ComposeAction) {
  composeDialog.visible = true
  composeDialog.mode = mode
  composeDialog.keyword = ''
  composeDialog.groupName = ''
  composeDialog.users = []
  composeDialog.selectedUserIds = []
  composeDialog.loading = false
  composeDialog.submitting = false
  composeDialog.error = null
}

function closeComposeDialog() {
  composeDialog.visible = false
}

async function runComposeSearch() {
  composeDialog.loading = true
  composeDialog.error = null

  try {
    const page = await searchUsers(composeDialog.keyword, 1, 20)
    composeDialog.users = page.list
  } catch (error) {
    composeDialog.error = error instanceof Error ? error.message : '用户搜索失败'
  } finally {
    composeDialog.loading = false
  }
}

function toggleComposeUser(userId: number) {
  if (composeDialog.mode === 'single') {
    composeDialog.selectedUserIds = [userId]
    return
  }

  if (composeDialog.selectedUserIds.includes(userId)) {
    composeDialog.selectedUserIds = composeDialog.selectedUserIds.filter((id) => id !== userId)
    return
  }

  composeDialog.selectedUserIds = [...composeDialog.selectedUserIds, userId]
}

async function submitComposeDialog() {
  composeDialog.submitting = true
  composeDialog.error = null

  try {
    if (uiStore.conversationFolder !== 'inbox') {
      uiStore.setConversationFolder('inbox')
      await chatStore.refreshConversationList(true)
    }

    if (composeDialog.mode === 'single') {
      const targetUserId = composeDialog.selectedUserIds[0]
      if (!targetUserId) {
        throw new Error('请选择一个用户')
      }
      const conversation = await chatStore.createSingleConversation(targetUserId)
      closeComposeDialog()
      await selectConversation(conversation.conversationId)
      return
    }

    if (!composeDialog.groupName.trim()) {
      throw new Error(composeDialog.mode === 'channel' ? '请输入频道名称' : '请输入群组名称')
    }
    if (!composeDialog.selectedUserIds.length) {
      throw new Error(composeDialog.mode === 'channel' ? '请至少选择一个频道成员' : '请至少选择一个群成员')
    }

    const payload: GroupCreatePayload = {
      groupName: composeDialog.groupName.trim(),
      memberIds: composeDialog.selectedUserIds,
      conversationType: composeDialog.mode === 'channel' ? 3 : 2,
    }
    const { result } = await chatStore.createGroupConversation(payload)
    closeComposeDialog()
    await selectConversation(result.conversationId)
  } catch (error) {
    composeDialog.error = error instanceof Error ? error.message : '创建失败'
  } finally {
    composeDialog.submitting = false
  }
}

function handleFocusSearch() {
  if (!chatStore.activeConversation) return

  uiStore.setGlobalMenuOpen(false)
  uiStore.setTopbarMenuOpen(false)
  messageSearchOpen.value = true
}

function closeMessageSearch() {
  messageSearchOpen.value = false
  messageSearchQuery.value = ''
  messageSearchMatchCount.value = 0
  activeMessageSearchIndex.value = 0
}

function cycleMessageSearch(step: -1 | 1) {
  if (!messageSearchMatchCount.value) return

  activeMessageSearchIndex.value =
    (activeMessageSearchIndex.value + step + messageSearchMatchCount.value) % messageSearchMatchCount.value
}

function handleMessageSearchCountUpdate(value: number) {
  messageSearchMatchCount.value = value

  if (!value) {
    activeMessageSearchIndex.value = 0
    return
  }

  if (activeMessageSearchIndex.value >= value) {
    activeMessageSearchIndex.value = 0
  }
}

async function handleRetry() {
  try {
    await chatStore.bootstrap(true)
    await authStore.ensureCurrentProfile(true).catch(() => null)
  } catch {
    return
  }
}

async function handleLogout() {
  try {
    await authStore.logout()
  } finally {
    callStore.resetState()
    cancelMessageEditing()
    clearReplyMessage()
    closeComposeDialog()
    cancelForwardSelection()
    forwardDialog.visible = false
    globalSearchState.visible = false
    contactsState.addDialogVisible = false
    chatStore.resetState()
    uiStore.setGlobalMenuOpen(false)
    uiStore.setTopbarMenuOpen(false)
    uiStore.setProfileOpen(false)
    uiStore.setMobileView('list')
    await router.replace('/login')
  }
}

function handleAddAccount() {
  router.push('/login?add=1')
}

async function handleSwitchAccount(userId: number) {
  try {
    await authStore.activateStoredAccount(userId)
  } catch {
    await router.replace('/login?add=1')
    return
  }
  callStore.resetState()
  chatStore.resetState()
  uiStore.setGlobalMenuOpen(false)
  uiStore.setTopbarMenuOpen(false)
  uiStore.setProfileOpen(false)
  await router.replace('/chat')
  try {
    await chatStore.bootstrap(true)
    await authStore.ensureCurrentProfile(true)
  } catch {
    return
  }
}

function handleRemoveStoredAccount(userId: number) {
  authStore.removeStoredAccount(userId)
}

async function handleSendEmailBindCode(payload: { email: string; currentPassword: string }) {
  await authStore.sendEmailBindCode(payload.email, payload.currentPassword)
}

async function handleBindEmail(payload: { email: string; code: string; currentPassword: string }) {
  await authStore.bindEmail(payload.email, payload.code, payload.currentPassword)
}

async function handleRefreshTrustedDevices() {
  await authStore.loadTrustedDevices(true)
}

async function handleRevokeTrustedDevice(payload: { deviceId: number; deviceFingerprint: string }) {
  await authStore.revokeTrustedDevice(payload.deviceId, payload.deviceFingerprint)
}

async function handleRevokeAllTrustedDevices() {
  await authStore.revokeAllTrustedDevices()
}

async function handleRefreshSecurityEvents() {
  await authStore.loadSecurityEvents(true)
}

async function handleLoadOlderMessages() {
  const conversationId = chatStore.activeConversationId
  if (!conversationId) return

  try {
    await chatStore.loadOlderMessages(conversationId)
  } catch {
    return
  }
}

function startEditingMessage(message: ChatMessage) {
  editingMessageId.value = message.messageId
  editingMessageDraft.value = message.content ?? ''
}

function cancelMessageEditing() {
  editingMessageId.value = null
  editingMessageDraft.value = ''
}

async function saveEditingMessage(payload: { messageId: number; content: string }) {
  messageActionPendingId.value = payload.messageId

  try {
    await chatStore.editMessage(payload.messageId, payload.content)
    cancelMessageEditing()
  } catch {
    return
  } finally {
    messageActionPendingId.value = null
  }
}

async function handleRecallMessage(messageId: number) {
  try {
    await ElMessageBox.confirm('撤回后会同步更新当前会话和对方消息列表。', '撤回消息', {
      confirmButtonText: '撤回',
      cancelButtonText: '取消',
      type: 'warning',
    })
  } catch {
    return
  }

  messageActionPendingId.value = messageId

  try {
    await chatStore.recallMessage(messageId)
    if (editingMessageId.value === messageId) {
      cancelMessageEditing()
    }
  } catch {
    return
  } finally {
    messageActionPendingId.value = null
  }
}

async function handleToggleReaction(payload: { messageId: number; emoji: string }) {
  try {
    await chatStore.toggleReaction(payload.messageId, payload.emoji)
  } catch {
    return
  }
}

function handleReplyMessage(message: ChatMessage) {
  replyingMessage.value = message
}

function clearReplyMessage() {
  replyingMessage.value = null
}

function toReplySource(message: ChatMessage | null): MessageReplySource | null {
  if (!message) return null
  return {
    sourceMessageId: message.messageId,
    sourceConversationId: message.conversationId,
    sourceSenderId: message.fromUserId,
    sourceMsgType: message.msgType,
    sourcePreview: message.content ?? message.file?.fileName ?? null,
  }
}

async function handleSendTextMessage(content: string) {
  attachmentError.value = null
  await chatStore.sendMessage({
    currentUserId: authStore.currentUser?.userId ?? 0,
    content,
    replySource: toReplySource(replyingMessage.value),
  })
  clearReplyMessage()
}

async function handleUploadAttachment(file: File) {
  attachmentUploading.value = true
  attachmentError.value = null

  try {
    const bizType = file.type.startsWith('image/') ? 2 : 4
    const uploadedFile = (await uploadFile(file, bizType)) as ChatFile
    await chatStore.sendMessage({
      currentUserId: authStore.currentUser?.userId ?? 0,
      content: null,
      msgType: file.type === 'image/gif' ? 'GIF' : bizType === 2 ? 'IMAGE' : 'FILE',
      fileId: uploadedFile.fileId,
      file: uploadedFile,
      replySource: toReplySource(replyingMessage.value),
    })
    clearReplyMessage()
  } catch (error) {
    attachmentError.value = error instanceof Error ? error.message : '附件上传失败'
  } finally {
    attachmentUploading.value = false
  }
}

async function handleSendSticker(sticker: StickerDefinition) {
  attachmentUploading.value = true
  attachmentError.value = null

  try {
    await chatStore.sendMessage({
      currentUserId: authStore.currentUser?.userId ?? 0,
      content: sticker.title,
      msgType: 'STICKER',
      replySource: toReplySource(replyingMessage.value),
      sticker: {
        stickerId: sticker.stickerId,
        title: sticker.title,
      },
    })
    clearReplyMessage()
  } catch (error) {
    attachmentError.value = error instanceof Error ? error.message : '贴纸发送失败'
  } finally {
    attachmentUploading.value = false
  }
}

async function handleCheckUsername(username: string) {
  const normalized = username.trim()
  if (!normalized) {
    usernameCheck.checking = false
    usernameCheck.available = null
    usernameCheck.message = null
    return
  }
  usernameCheck.checking = true
  try {
    const result = await checkUsernameAvailability(normalized)
    usernameCheck.available = result.available
    usernameCheck.message = result.available ? '这个 @username 可以使用。' : '这个 @username 已被占用。'
  } catch (error) {
    usernameCheck.available = false
    usernameCheck.message = error instanceof Error ? error.message : '用户名校验失败'
  } finally {
    usernameCheck.checking = false
  }
}

function openPublicProfile(username: string) {
  router.push(buildPublicProfilePath(username)).catch(() => undefined)
}

function handleOpenConversationPublicProfile(path: string) {
  router.push(path).catch(() => undefined)
}

function handleGlobalKeydown(event: KeyboardEvent) {
  if ((event.metaKey || event.ctrlKey) && event.key.toLowerCase() === 'k') {
    event.preventDefault()
    openGlobalSearch()
    return
  }

  if (event.key !== 'Escape') return
  if (globalSearchState.visible) {
    globalSearchState.visible = false
    event.preventDefault()
    return
  }
  if (forwardDialog.visible) {
    forwardDialog.visible = false
    event.preventDefault()
    return
  }
  if (forwardSelectionMode.value) {
    cancelForwardSelection()
    event.preventDefault()
    return
  }
  if (messageSearchOpen.value) {
    closeMessageSearch()
    event.preventDefault()
    return
  }
  if (uiStore.closeFloatingUi()) {
    event.preventDefault()
  }
}

watch(messageSearchQuery, () => {
  activeMessageSearchIndex.value = 0
})

watch(
  () => composeDialog.visible,
  async (visible) => {
    if (!visible) return
    await runComposeSearch()
  },
)

watch(
  () => composeDialog.keyword,
  async () => {
    if (!composeDialog.visible) return
    await runComposeSearch()
  },
)

function dismissTransientBanner() {
  chatStore.clearNoticeMessage()
  chatStore.clearSendError()
  chatStore.clearSyncError()
}

function registerDebugHooks() {
  if (!e2eHooksEnabled.value) return

  window.__ECHOIM_E2E__ = {
    getConnectionStatus: () => uiStore.connectionStatus,
    getCurrentUserId: () => authStore.currentUser?.userId ?? null,
    getActiveConversationId: () => chatStore.activeConversationId,
    getLeftPanelMode: () => uiStore.leftPanelMode,
    listConversations: () =>
      chatStore.conversations.map((item) => ({
        conversationId: item.conversationId,
        conversationName: item.conversationName,
        peerUserId: item.peerUserId,
        groupId: item.groupId,
        lastMessagePreview: item.lastMessagePreview,
        unreadCount: item.unreadCount,
        latestSeq: item.latestSeq,
      })),
    listMessages: (conversationId: number) =>
      (chatStore.messagesByConversation[conversationId] ?? []).map((message) => ({
        messageId: message.messageId,
        clientMsgId: message.clientMsgId,
        content: message.content,
        recalled: Boolean(message.recalled),
        edited: Boolean(message.edited),
        seqNo: message.seqNo,
      })),
    openConversation: async (conversationId: number) => {
      await selectConversation(conversationId)
    },
    refreshConversationMessages: async (conversationId: number) => {
      await chatStore.loadConversationMessages(conversationId, true)
    },
    sendTextMessage: async (content: string) => {
      await handleSendTextMessage(content)
    },
    openLeftPanel: (mode: LeftPanelMode) => {
      openLeftPanel(mode)
    },
    dropRealtimeConnection: (pauseReconnect?: boolean) => {
      chatStore.simulateRealtimeDrop(Boolean(pauseReconnect))
    },
    reconnectRealtime: async () => {
      await chatStore.reconnectRealtimeNow()
    },
    findMessageIdByText: (conversationId: number, content: string) => {
      const targetContent = content.trim()
      const matches = (chatStore.messagesByConversation[conversationId] ?? []).filter(
        (message) => !message.recalled && (message.content ?? '') === targetContent,
      )
      return matches[matches.length - 1]?.messageId ?? null
    },
    editMessage: async (messageId: number, content: string) => {
      await chatStore.editMessage(messageId, content)
    },
    recallMessage: async (messageId: number) => {
      await chatStore.recallMessage(messageId)
    },
    getErrors: () => ({ ...chatStore.errors }),
    getWsEvents: () => [...chatStore.debugEvents],
  }
}
</script>

<template>
  <main class="chat-page">
    <h1 class="chat-page__sr-only">EchoIM 聊天工作台</h1>
    <p class="chat-page__sr-only" role="status" aria-live="polite">{{ liveStatusMessage }}</p>

    <ContactsPanel
      v-if="shouldShowContactsPanel"
      class="chat-page__sidebar"
      :friends="contactsState.friends"
      :requests="contactsState.requests"
      :blocked="contactsState.blocked"
      :active-tab="contactsState.activeTab"
      :keyword="contactsState.keyword"
      :loading="contactsState.loading"
      :error-message="contactsState.error"
      @back="openLeftPanel('conversations')"
      @update:active-tab="contactsState.activeTab = $event"
      @update:keyword="contactsState.keyword = $event"
      @open-add-contact="openAddContactDialog"
      @open-chat="openChatFromContact"
      @update-remark="handleUpdateFriendRemark"
      @block-friend="handleBlockFriend"
      @unblock-friend="handleUnblockFriend"
      @delete-friend="handleDeleteFriend"
      @approve-request="handleApproveRequest"
      @reject-request="handleRejectRequest"
    />

    <ConversationSidebar
      v-else-if="shouldShowConversationList"
      class="chat-page__sidebar"
      :current-user="authStore.currentUser"
      :current-profile="authStore.profile"
      :stored-accounts="authStore.storedAccounts"
      :conversations="chatStore.filteredConversations"
      :selected-conversation-id="chatStore.activeConversationId"
      :conversation-folder="uiStore.conversationFolder"
      :search-query="chatStore.searchQuery"
      :theme="uiStore.theme"
      :left-panel-mode="uiStore.leftPanelMode"
      :settings-section="uiStore.settingsSection"
      :global-menu-open="uiStore.globalMenuOpen"
      :chat-preferences="uiStore.chatPreferences"
      :loading="chatStore.loading"
      :error-message="sidebarErrorMessage"
      :focus-search-token="uiStore.sidebarSearchFocusToken"
      :panel-scroll-top="currentSidebarScrollTop"
      :profile-loading="authStore.profileLoading"
      :profile-saving="authStore.profileSaving"
      :password-saving="authStore.passwordSaving"
      :email-binding-loading="authStore.emailBindingLoading"
      :trusted-devices-loading="authStore.trustedDevicesLoading"
      :security-events-loading="authStore.securityEventsLoading"
      :trusted-devices="authStore.trustedDevices"
      :security-events="authStore.securityEvents"
      :profile-error="authStore.profileError"
      :profile-notice="authStore.profileNotice"
      :username-checking="usernameCheck.checking"
      :username-available="usernameCheck.available"
      :username-message="usernameCheck.message"
      @update:search-query="chatStore.setSearchQuery"
      @update:conversation-folder="uiStore.setConversationFolder"
      @update:panel-scroll-top="uiStore.setPanelScrollTop(uiStore.leftPanelMode, $event)"
      @update:settings-section="uiStore.setSettingsSection"
      @update:global-menu-open="uiStore.setGlobalMenuOpen"
      @update:chat-preferences="uiStore.setChatPreferences"
      @clear-search="chatStore.clearSearchQuery"
      @retry="handleRetry"
      @select="selectConversation"
      @toggle-theme="uiStore.toggleTheme"
      @open-panel="openLeftPanel"
      @save-profile="saveProfile"
      @check-username="handleCheckUsername"
      @change-password="changePassword"
      @send-email-bind-code="handleSendEmailBindCode"
      @bind-email="handleBindEmail"
      @refresh-trusted-devices="handleRefreshTrustedDevices"
      @revoke-trusted-device="handleRevokeTrustedDevice"
      @revoke-all-trusted-devices="handleRevokeAllTrustedDevices"
      @refresh-security-events="handleRefreshSecurityEvents"
      @clear-profile-error="authStore.clearProfileError"
      @clear-profile-notice="authStore.clearProfileNotice"
      @conversation-action="handleConversationContextAction"
      @compose-action="openComposeDialog"
      @open-global-search="openGlobalSearch"
      @open-saved-messages="openSavedMessages"
      @add-account="handleAddAccount"
      @switch-account="handleSwitchAccount"
      @remove-account="handleRemoveStoredAccount"
      @logout="handleLogout"
    />

    <section v-if="shouldShowMainPanel" id="chat-main" tabindex="-1" class="chat-page__main">
      <template v-if="chatStore.activeConversation">
        <ChatTopbar
          :conversation="chatStore.activeConversation"
          :profile="chatStore.activeProfile"
          :is-mobile="uiStore.isMobile"
          :menu-open="uiStore.topbarMenuOpen"
          :message-search-open="messageSearchOpen"
          :message-search-query="messageSearchQuery"
          :message-search-match-count="messageSearchMatchCount"
          :active-message-search-index="activeMessageSearchIndex"
          @back="handleBack"
          @focus-search="handleFocusSearch"
          @close-search="closeMessageSearch"
          @navigate-search-match="cycleMessageSearch"
          @open-forward-selection="startForwardSelection"
          @update:message-search-query="messageSearchQuery = $event"
          @action="handleConversationAction"
          @update:menu-open="uiStore.setTopbarMenuOpen"
          @open-profile="openProfile"
          @start-call="handleStartCall"
        />
        <div
          v-if="transientBanner"
          class="chat-page__banner"
          :class="`is-${transientBanner.tone}`"
          role="status"
          aria-live="polite"
          data-testid="connection-status"
        >
          <span>{{ transientBanner.message }}</span>
          <button type="button" aria-label="关闭状态提示" @click="dismissTransientBanner">关闭</button>
        </div>
        <div class="chat-page__stage">
          <div v-if="forwardSelectionMode" class="chat-page__forward-bar">
            <div>
              <span>批量转发模式</span>
              <strong>已选 {{ selectedForwardMessageIds.length }} 条消息</strong>
            </div>
            <div class="chat-page__forward-actions">
              <button type="button" :disabled="!selectedForwardMessageIds.length" @click="openForwardDialogForMessages(selectedForwardMessages)">
                转发选中消息
              </button>
              <button type="button" @click="cancelForwardSelection">取消</button>
            </div>
          </div>
          <MessagePane
            :conversation-id="chatStore.activeConversation.conversationId"
            :messages="chatStore.activeMessages"
            :current-user-id="authStore.currentUser?.userId ?? 0"
            :current-user-name="authStore.currentUser?.nickname ?? '我'"
            :conversation-name="chatStore.activeConversation.conversationName"
            :conversation-type="chatStore.activeConversation.conversationType"
            :compact="uiStore.chatPreferences.compactBubbles"
            :loading="chatStore.messagesLoading"
            :error-message="chatStore.errors.messageLoadError"
            :has-older-messages="chatStore.activeHasOlderMessages"
            :older-messages-loading="chatStore.activeOlderMessagesLoading"
            :older-messages-error="chatStore.activeOlderMessagesError"
            :editing-message-id="editingMessageId"
            :editing-draft="editingMessageDraft"
            :message-action-pending-id="messageActionPendingId"
            :search-query="messageSearchQuery"
            :active-match-index="activeMessageSearchIndex"
            :forward-selection-mode="forwardSelectionMode"
            :selected-forward-message-ids="selectedForwardMessageIds"
            :jump-message-id="jumpMessageId"
            @retry="chatStore.loadConversationMessages(chatStore.activeConversation.conversationId, true)"
            @load-older="handleLoadOlderMessages"
            @retry-message="chatStore.retryMessage"
            @update:search-match-count="handleMessageSearchCountUpdate"
            @start-edit-message="startEditingMessage"
            @update:editing-draft="editingMessageDraft = $event"
            @cancel-edit-message="cancelMessageEditing"
            @save-edit-message="saveEditingMessage"
            @recall-message="handleRecallMessage"
            @reply-message="handleReplyMessage"
            @forward-message="handleForwardMessage"
            @toggle-forward-selection="toggleForwardSelection"
            @toggle-reaction="handleToggleReaction"
          />
          <MessageComposer
            :enter-to-send="uiStore.chatPreferences.enterToSend"
            :can-send="chatStore.activeConversation.canSend"
            :replying-message="replyingMessage"
            :attachment-uploading="attachmentUploading"
            :attachment-error="attachmentError"
            :stickers="STICKER_LIBRARY"
            :disabled-reason="
              chatStore.activeConversation.conversationType === 3
                ? '仅频道创建者可发送消息'
                : '当前会话暂不可发送消息'
            "
            @cancel-reply="clearReplyMessage"
            @send="handleSendTextMessage"
            @upload-file="handleUploadAttachment"
            @send-sticker="handleSendSticker"
          />
        </div>
      </template>

      <div v-else class="chat-page__empty" data-testid="chat-empty-state"></div>
    </section>

    <CallOverlay
      :visible="callStore.isVisible"
      :call="callStore.activeCall"
      :phase="callStore.phase"
      :minimized="callStore.minimized"
      :local-muted="callStore.localMuted"
      :busy="callStore.busy"
      :error="callStore.error"
      :remote-stream="callStore.remoteStream"
      @accept="callStore.acceptIncomingCall"
      @reject="callStore.rejectIncomingCall"
      @cancel="callStore.cancelOutgoingCall"
      @end="callStore.endCurrentCall"
      @toggle-minimized="callStore.toggleMinimized"
      @toggle-mute="callStore.toggleMute"
    />

    <ConversationProfileDrawer
      v-if="chatStore.activeConversation && uiStore.profileOpen"
      class="chat-page__profile"
      :conversation="chatStore.activeConversation"
      :profile="chatStore.activeProfile"
      :loading="chatStore.activeProfileLoading"
      :error-message="chatStore.activeProfileError"
      :overlay="uiStore.useOverlayProfile"
      :visible="uiStore.profileOpen"
      @action="handleConversationAction"
      @open-public-profile="handleOpenConversationPublicProfile"
      @update:visible="uiStore.setProfileOpen"
      @update-group-meta="handleUpdateGroupMeta"
      @update-group-notice="handleUpdateGroupNotice"
      @promote-member="handlePromoteGroupMember($event.userId, $event.role)"
      @remove-member="handleRemoveGroupMember"
      @add-members="handleAddGroupMembers"
      @leave-group="handleLeaveActiveGroup"
      @dissolve-group="handleDissolveActiveGroup"
    />

    <el-dialog
      v-model="composeDialog.visible"
      :title="composeDialog.mode === 'single' ? '新建私聊' : composeDialog.mode === 'channel' ? '新建频道' : '新建群组'"
      width="520px"
      destroy-on-close
    >
      <div class="compose-dialog">
        <el-input
          v-model="composeDialog.keyword"
          placeholder="搜索用户昵称、用户名或编号"
          clearable
        />
        <el-input
          v-if="composeDialog.mode !== 'single'"
          v-model="composeDialog.groupName"
          :placeholder="composeDialog.mode === 'channel' ? '输入频道名称' : '输入群组名称'"
          maxlength="40"
        />
        <div v-if="composeDialog.error" class="compose-dialog__error">{{ composeDialog.error }}</div>
        <div class="compose-dialog__users">
          <div v-if="composeDialog.loading" class="compose-dialog__empty">正在搜索用户…</div>
          <button
            v-for="user in composeDialog.users"
            :key="user.userId"
            class="compose-dialog__user"
            :class="{ 'is-selected': composeDialog.selectedUserIds.includes(user.userId) }"
            type="button"
            @click="toggleComposeUser(user.userId)"
          >
            <AvatarBadge :name="user.nickname" :avatar-url="user.avatarUrl" size="lg" />
            <div class="compose-dialog__user-copy">
              <strong>{{ user.nickname }}</strong>
              <span>
                @{{ user.username }}
                <button class="compose-dialog__mini-link" type="button" @click.stop="openPublicProfile(user.username)">主页</button>
              </span>
              <p>{{ user.signature || user.userNo }}</p>
            </div>
          </button>
          <div v-if="!composeDialog.loading && !composeDialog.users.length" class="compose-dialog__empty">
            没有匹配的用户
          </div>
        </div>
      </div>
      <template #footer>
        <el-button @click="closeComposeDialog">取消</el-button>
        <el-button type="primary" :loading="composeDialog.submitting" @click="submitComposeDialog">
          {{ composeDialog.mode === 'single' ? '开始聊天' : '创建并进入' }}
        </el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="contactsState.addDialogVisible" title="添加联系人" width="520px" destroy-on-close>
      <div class="compose-dialog">
        <el-input v-model="contactsState.addKeyword" placeholder="搜索昵称、用户名或账号编号" clearable />
        <div v-if="contactsState.addError" class="compose-dialog__error">{{ contactsState.addError }}</div>
        <div class="compose-dialog__users">
          <div v-if="contactsState.addLoading" class="compose-dialog__empty">正在搜索用户…</div>
          <article v-for="user in contactsState.addUsers" :key="user.userId" class="compose-dialog__user compose-dialog__user--card">
            <AvatarBadge :name="user.nickname" :avatar-url="user.avatarUrl" size="lg" />
            <div class="compose-dialog__user-copy">
              <strong>{{ user.nickname }}</strong>
              <span>
                @{{ user.username }}
                <button class="compose-dialog__mini-link" type="button" @click.stop="openPublicProfile(user.username)">主页</button>
              </span>
              <p>{{ user.signature || user.userNo }}</p>
            </div>
            <button class="compose-dialog__inline-action" type="button" @click="submitFriendRequestForUser(user.userId)">
              {{ user.friendStatus === 'FRIEND' ? '已是好友' : user.pendingRequestId ? '处理中' : '加好友' }}
            </button>
          </article>
          <div v-if="!contactsState.addLoading && !contactsState.addUsers.length" class="compose-dialog__empty">没有匹配的用户</div>
        </div>
      </div>
    </el-dialog>

    <el-dialog v-model="globalSearchState.visible" title="全局搜索" width="860px" destroy-on-close class="global-search-dialog">
      <div class="search-sheet">
        <div class="search-sheet__hero">
          <div class="search-sheet__hero-copy">
            <strong>搜索你的会话、联系人和消息</strong>
            <p>结果只来自你当前可访问的内容，不会跨出你的工作区。</p>
          </div>
          <span class="search-sheet__hero-shortcut">Cmd/Ctrl + K</span>
        </div>
        <el-input v-model="globalSearchState.keyword" placeholder="输入关键词，搜索会话、用户或消息内容" clearable />
        <div v-if="globalSearchState.error" class="compose-dialog__error">{{ globalSearchState.error }}</div>
        <div class="search-sheet__summary">
          <span v-if="globalSearchState.loading">正在搜索…</span>
          <span v-else-if="hasGlobalSearchKeyword">共找到 {{ globalSearchTotal }} 条结果</span>
          <span v-else>输入关键词后开始搜索</span>
        </div>
        <div v-if="!hasGlobalSearchKeyword" class="search-sheet__blank">
          <strong>支持统一搜索</strong>
          <p>你可以直接查找会话标题、联系人昵称、用户名以及消息内容。</p>
        </div>
        <div v-else class="search-sheet__results">
          <div class="search-sheet__rail">
            <section class="search-sheet__section search-sheet__section--side">
              <header class="search-sheet__section-head">
                <strong>会话</strong>
                <span>{{ globalSearchState.conversations.length }}</span>
              </header>
              <div v-if="globalSearchState.loading" class="search-sheet__empty">正在搜索会话…</div>
              <button
                v-for="conversation in globalSearchState.conversations"
                :key="conversation.conversationId"
                class="search-sheet__row"
                type="button"
                @click="handleGlobalConversationSelect(conversation)"
              >
                <AvatarBadge :name="conversation.conversationName" :avatar-url="conversation.avatarUrl" size="md" />
                <div class="search-sheet__row-copy">
                  <strong>{{ conversation.conversationName }}</strong>
                  <span>{{ conversation.archived ? '已归档会话' : '收件箱会话' }}</span>
                  <p>{{ conversation.lastMessagePreview || '还没有消息' }}</p>
                </div>
              </button>
              <div v-if="!globalSearchState.loading && !globalSearchState.conversations.length" class="search-sheet__empty">
                没有匹配的会话
              </div>
            </section>
            <section class="search-sheet__section search-sheet__section--side">
              <header class="search-sheet__section-head">
                <strong>用户</strong>
                <span>{{ globalSearchState.users.length }}</span>
              </header>
              <div v-if="globalSearchState.loading" class="search-sheet__empty">正在搜索用户…</div>
              <button
                v-for="user in globalSearchState.users"
                :key="user.userId"
                class="search-sheet__row"
                type="button"
                @click="handleGlobalUserSelect(user)"
              >
                <AvatarBadge :name="user.nickname" :avatar-url="user.avatarUrl" size="md" />
                <div class="search-sheet__row-copy">
                  <strong>{{ user.nickname }}</strong>
                  <span>
                    @{{ user.username }}
                    <button class="compose-dialog__mini-link" type="button" @click.stop="openPublicProfile(user.username)">主页</button>
                  </span>
                  <p>{{ user.signature || user.userNo }}</p>
                </div>
              </button>
              <div v-if="!globalSearchState.loading && !globalSearchState.users.length" class="search-sheet__empty">
                没有匹配的用户
              </div>
            </section>
          </div>
          <section class="search-sheet__section search-sheet__section--messages">
            <header class="search-sheet__section-head">
              <strong>消息</strong>
              <span>{{ globalSearchState.messages.length }}</span>
            </header>
            <div v-if="globalSearchState.loading" class="search-sheet__empty">正在搜索消息…</div>
            <button
              v-for="message in globalSearchState.messages"
              :key="message.messageId"
              class="search-sheet__row search-sheet__row--message"
              type="button"
              @click="handleGlobalMessageSelect(message)"
            >
              <AvatarBadge :name="message.conversationName" size="md" />
              <div class="search-sheet__row-copy search-sheet__row-copy--message">
                <div class="search-sheet__headline">
                  <strong>{{ message.conversationName }}</strong>
                  <time class="search-sheet__time">{{ formatConversationTime(message.sentAt) }}</time>
                </div>
                <span class="search-sheet__meta">{{ message.senderName }}</span>
                <p>{{ message.preview }}</p>
              </div>
            </button>
            <div v-if="!globalSearchState.loading && !globalSearchState.messages.length" class="search-sheet__empty">
              没有匹配的消息
            </div>
          </section>
        </div>
      </div>
    </el-dialog>

    <el-dialog v-model="forwardDialog.visible" class="forward-dialog" title="转发消息" width="620px" destroy-on-close>
      <div class="forward-sheet">
        <el-input ref="forwardSearchInput" v-model="forwardDialog.keyword" class="forward-sheet__search" placeholder="搜索目标会话" clearable />
        <div class="forward-sheet__summary">
          <span>选择目标会话</span>
          <strong>
            {{ forwardDialog.sourceMessageIds.length }} 条消息 ·
            {{ forwardTargetCount }} 个目标
          </strong>
        </div>
        <div v-if="forwardDialog.error" class="compose-dialog__error forward-sheet__error">{{ forwardDialog.error }}</div>
        <div class="forward-sheet__saved">
          <button
            class="forward-sheet__saved-button"
            :class="{ 'is-active': forwardDialog.includeSavedMessages }"
            type="button"
            :aria-pressed="forwardDialog.includeSavedMessages"
            @click="forwardDialog.includeSavedMessages = !forwardDialog.includeSavedMessages"
          >
            <div class="forward-sheet__saved-copy">
              <strong>Saved Messages</strong>
              <span>转发到自己的消息收藏</span>
            </div>
            <span class="forward-sheet__pill">{{ forwardDialog.includeSavedMessages ? '已选' : '可选' }}</span>
          </button>
        </div>
        <div class="compose-dialog__users" role="list" aria-label="可转发目标会话">
          <button
            v-for="conversation in forwardCandidateConversations"
            :key="conversation.conversationId"
            class="compose-dialog__user forward-sheet__conversation"
            :class="{ 'is-selected': forwardDialog.selectedConversationIds.includes(conversation.conversationId) }"
            type="button"
            :aria-pressed="forwardDialog.selectedConversationIds.includes(conversation.conversationId)"
            @click="
              forwardDialog.selectedConversationIds = forwardDialog.selectedConversationIds.includes(conversation.conversationId)
                ? forwardDialog.selectedConversationIds.filter((id) => id !== conversation.conversationId)
                : [...forwardDialog.selectedConversationIds, conversation.conversationId]
            "
          >
            <AvatarBadge :name="conversation.conversationName" :avatar-url="conversation.avatarUrl" size="lg" />
            <div class="compose-dialog__user-copy forward-sheet__conversation-copy">
              <div class="forward-sheet__conversation-head">
                <strong>{{ conversation.conversationName }}</strong>
                <span class="forward-sheet__conversation-state">
                  {{ forwardDialog.selectedConversationIds.includes(conversation.conversationId) ? '已选' : conversation.archived ? '归档' : '会话' }}
                </span>
              </div>
              <span>{{ conversation.specialType === 'SAVED_MESSAGES' ? '专属自聊' : conversation.archived ? '已归档会话' : '会话' }}</span>
              <p>{{ conversation.lastMessagePreview || '还没有消息' }}</p>
            </div>
          </button>
          <div v-if="!forwardCandidateConversations.length" class="compose-dialog__empty forward-sheet__empty">
            <strong>{{ forwardEmptyState.title }}</strong>
            <p>{{ forwardEmptyState.description }}</p>
          </div>
        </div>
      </div>
      <template #footer>
        <el-button @click="forwardDialog.visible = false">取消</el-button>
        <el-button type="primary" :disabled="!forwardTargetCount || !forwardDialog.sourceMessageIds.length" :loading="forwardDialog.submitting" @click="submitForwardDialog">确认转发</el-button>
      </template>
    </el-dialog>
  </main>
</template>

<style scoped>
.chat-page {
  display: grid;
  grid-template-columns: 332px minmax(0, 1fr);
  gap: 0;
  height: 100%;
  min-height: 0;
  border: 1px solid var(--border-default);
  border-radius: 32px;
  background: linear-gradient(
    180deg,
    color-mix(in srgb, var(--surface-panel) 96%, transparent),
    color-mix(in srgb, var(--surface-card) 98%, transparent)
  );
  box-shadow: var(--shadow-lg);
  backdrop-filter: blur(18px);
  overflow: hidden;
}

.chat-page__sidebar,
.chat-page__main,
.chat-page__profile {
  min-height: 0;
  overflow: hidden;
}

.chat-page__sidebar {
  border-right: 1px solid var(--border-subtle);
}

.chat-page__main {
  display: flex;
  flex-direction: column;
  min-height: 0;
  background: transparent;
  overflow: hidden;
}

.chat-page__stage {
  position: relative;
  display: flex;
  flex: 1;
  min-height: 0;
  flex-direction: column;
  overflow: hidden;
  background: var(--chat-stage-base);
}

.chat-page__stage::before,
.chat-page__stage::after {
  content: '';
  position: absolute;
  inset: 0;
  pointer-events: none;
}

.chat-page__stage::before {
  background-image: var(--chat-wallpaper-image);
  background-position: center;
  background-repeat: no-repeat;
  background-size: cover;
  opacity: var(--chat-wallpaper-opacity);
}

.chat-page__stage::after {
  background:
    linear-gradient(180deg, var(--chat-stage-top), transparent 16%),
    linear-gradient(0deg, var(--chat-stage-bottom), transparent 22%),
    radial-gradient(circle at top right, color-mix(in srgb, var(--chat-stage-glow) 74%, transparent), transparent 42%);
}

.chat-page__banner {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 8px 16px;
  border-bottom: 1px solid var(--border-subtle);
  color: var(--text-secondary);
  font: 500 0.78rem/1.45 var(--font-body);
  background: color-mix(in srgb, var(--surface-card) 96%, transparent);
  backdrop-filter: blur(14px);
}

.chat-page__banner.is-warning {
  background: color-mix(in srgb, var(--status-warning) 10%, var(--surface-card));
}

.chat-page__banner.is-error {
  background: color-mix(in srgb, var(--status-danger) 10%, var(--surface-card));
}

.chat-page__banner.is-muted {
  background: color-mix(in srgb, var(--interactive-primary-bg) 8%, var(--surface-card));
}

.chat-page__banner button {
  border: 0;
  background: transparent;
  color: inherit;
  font: inherit;
}

.chat-page__empty {
  flex: 1;
  min-height: 0;
  position: relative;
  background:
    linear-gradient(180deg, rgba(10, 10, 16, 0.1), transparent 14%),
    var(--chat-stage-base);
}

.chat-page__empty::before,
.chat-page__empty::after {
  content: '';
  position: absolute;
  inset: 0;
  pointer-events: none;
}

.chat-page__empty::before {
  background-image: var(--chat-wallpaper-image);
  background-position: center;
  background-repeat: no-repeat;
  background-size: cover;
  opacity: var(--chat-wallpaper-opacity);
}

.chat-page__empty::after {
  background:
    linear-gradient(180deg, var(--chat-stage-top), transparent 16%),
    linear-gradient(0deg, var(--chat-stage-bottom), transparent 20%);
}

.chat-page__sr-only {
  position: absolute;
  width: 1px;
  height: 1px;
  padding: 0;
  margin: -1px;
  overflow: hidden;
  clip: rect(0, 0, 0, 0);
  white-space: nowrap;
  border: 0;
}

.chat-page__forward-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  width: min(760px, 100%);
  margin: 0 auto 12px;
  padding: 14px 16px;
  border: 1px solid color-mix(in srgb, var(--interactive-primary-bg) 14%, var(--border-default));
  border-radius: var(--radius-panel);
  background: color-mix(in srgb, var(--interactive-primary-bg) 6%, var(--surface-card));
}

.chat-page__forward-bar span,
.chat-page__forward-bar strong {
  display: block;
}

.chat-page__forward-bar span {
  color: var(--text-quaternary);
  font: 600 0.7rem/1 var(--font-mono);
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.chat-page__forward-bar strong {
  margin-top: 5px;
  color: var(--text-primary);
  font-size: 0.88rem;
  font-weight: 600;
}

.chat-page__forward-actions {
  display: flex;
  gap: 8px;
}

.chat-page__forward-actions button,
.compose-dialog__inline-action,
.search-sheet__row {
  transition:
    border-color var(--motion-fast) ease,
    background var(--motion-fast) ease;
}

.chat-page__forward-actions button,
.compose-dialog__inline-action {
  padding: 9px 11px;
  border: 1px solid var(--border-default);
  border-radius: var(--radius-control);
  background: var(--interactive-secondary-bg);
}

.compose-dialog__user--card {
  grid-template-columns: 48px minmax(0, 1fr) auto;
}

.compose-dialog__inline-action {
  font: 600 0.76rem/1 var(--font-body);
}

.search-sheet {
  display: grid;
  gap: 10px;
}

.search-sheet__hero {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 14px;
  padding: 0 2px;
}

.search-sheet__hero-copy strong {
  display: block;
  font: 620 0.9rem/1.12 var(--font-display);
  letter-spacing: -0.02em;
}

.search-sheet__hero-copy p {
  margin-top: 4px;
  color: var(--text-tertiary);
  font-size: 0.78rem;
  line-height: 1.48;
}

.search-sheet__hero-shortcut {
  flex: 0 0 auto;
  padding: 5px 8px;
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-pill);
  background: color-mix(in srgb, var(--surface-panel) 90%, transparent);
  color: var(--text-quaternary);
  font: 600 0.64rem/1 var(--font-mono);
}

.search-sheet__summary {
  min-height: 20px;
  display: flex;
  align-items: center;
  color: var(--text-tertiary);
  font-size: 0.74rem;
  line-height: 1.42;
}

.search-sheet__blank {
  display: grid;
  gap: 6px;
  padding: 17px 17px 18px;
  border: 1px dashed var(--border-strong);
  border-radius: var(--radius-panel);
  background: color-mix(in srgb, var(--surface-card) 84%, transparent);
}

.search-sheet__blank strong {
  font: 620 0.88rem/1.16 var(--font-display);
}

.search-sheet__blank p {
  color: var(--text-tertiary);
  font-size: 0.78rem;
  line-height: 1.5;
}

.search-sheet__results {
  display: grid;
  grid-template-columns: minmax(220px, 0.78fr) minmax(0, 1.22fr);
  gap: 14px;
  max-height: 56vh;
  overflow: auto;
  padding-right: 4px;
}

.search-sheet__rail {
  display: grid;
  gap: 10px;
  align-content: start;
}

.search-sheet__section {
  display: grid;
  gap: 9px;
  align-content: start;
  padding: 12px;
  border: 1px solid var(--border-default);
  border-radius: var(--radius-md);
  background: color-mix(in srgb, var(--surface-card) 92%, transparent);
}

.search-sheet__section--messages {
  min-width: 0;
}

.search-sheet__section-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.search-sheet__section-head strong {
  font: 620 0.83rem/1.12 var(--font-display);
}

.search-sheet__section-head span {
  padding: 4px 7px;
  border-radius: var(--radius-pill);
  background: var(--interactive-secondary-bg);
  color: var(--text-quaternary);
  font: 700 0.64rem/1 var(--font-mono);
}

.search-sheet__row {
  display: grid;
  grid-template-columns: 40px minmax(0, 1fr);
  align-items: start;
  gap: 10px;
  width: 100%;
  padding: 10px;
  border: 1px solid var(--border-default);
  border-radius: var(--radius-control);
  background: color-mix(in srgb, var(--surface-panel) 92%, transparent);
  text-align: left;
  transition:
    background var(--motion-fast) var(--motion-ease-out),
    border-color var(--motion-fast) var(--motion-ease-out),
    box-shadow var(--motion-fast) var(--motion-ease-out);
}

.search-sheet__row:hover,
.search-sheet__row:focus-visible {
  border-color: color-mix(in srgb, var(--interactive-primary-bg) 18%, var(--border-strong));
  background: color-mix(in srgb, var(--interactive-selected-bg) 92%, var(--surface-card));
  box-shadow: 0 0 0 3px color-mix(in srgb, var(--interactive-focus-ring) 26%, transparent);
}

.search-sheet__row-copy {
  min-width: 0;
  display: grid;
  gap: 3px;
}

.search-sheet__row-copy strong,
.search-sheet__row-copy span,
.search-sheet__row-copy p {
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.search-sheet__row-copy strong {
  font-size: 0.8rem;
  font-weight: 600;
  line-height: 1.28;
}

.search-sheet__row-copy span,
.search-sheet__row-copy p {
  color: var(--text-tertiary);
  font-size: 0.71rem;
  line-height: 1.38;
}

.search-sheet__row--message {
  grid-template-columns: 36px minmax(0, 1fr);
  gap: 11px;
}

.search-sheet__row-copy--message {
  gap: 4px;
}

.search-sheet__headline {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 10px;
}

.search-sheet__meta,
.search-sheet__time {
  color: var(--text-quaternary);
  font: 600 0.64rem/1 var(--font-mono);
  letter-spacing: 0.04em;
}

.search-sheet__meta {
  text-transform: uppercase;
}

.search-sheet__empty {
  display: grid;
  place-items: center;
  min-height: 96px;
  padding: 13px;
  border: 1px dashed var(--border-default);
  border-radius: 15px;
  color: var(--text-quaternary);
  font-size: 0.76rem;
  text-align: center;
}

:deep(.global-search-dialog .el-dialog) {
  border-radius: 24px;
}

:deep(.global-search-dialog .el-dialog__body) {
  padding-top: 16px;
}

.forward-sheet {
  display: grid;
  gap: 10px;
}

.forward-sheet__search {
  margin-bottom: 2px;
}

.forward-sheet__summary {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 10px;
}

.forward-sheet__summary span,
.forward-sheet__summary strong {
  display: block;
}

.forward-sheet__summary span {
  color: var(--text-quaternary);
  font: 600 0.6rem/1 var(--font-mono);
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.forward-sheet__summary strong {
  color: var(--text-primary);
  font: 600 0.76rem/1.2 var(--font-body);
}

.forward-sheet__saved {
  display: flex;
}

.forward-sheet__saved-button {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 12px 13px;
  border: 1px solid var(--border-default);
  border-radius: var(--radius-control);
  background: color-mix(in srgb, var(--surface-panel) 72%, transparent);
  text-align: left;
  transition:
    border-color var(--motion-fast) var(--motion-ease-out),
    background var(--motion-fast) var(--motion-ease-out),
    color var(--motion-fast) var(--motion-ease-out);
}

.forward-sheet__saved-button:hover,
.forward-sheet__saved-button:focus-visible {
  border-color: var(--border-strong);
  background: var(--interactive-secondary-bg-hover);
}

.forward-sheet__saved-button:focus-visible {
  box-shadow: 0 0 0 4px color-mix(in srgb, var(--interactive-focus-ring) 28%, transparent);
}

.forward-sheet__saved-copy {
  min-width: 0;
  display: grid;
  gap: 4px;
}

.forward-sheet__saved-copy strong {
  font-size: 0.8rem;
  font-weight: 600;
  line-height: 1.22;
}

.forward-sheet__saved-copy span {
  color: var(--text-tertiary);
  font-size: 0.72rem;
  line-height: 1.42;
}

.forward-sheet__pill {
  flex-shrink: 0;
  padding: 5px 8px;
  border-radius: var(--radius-pill);
  border: 1px solid var(--border-default);
  background: color-mix(in srgb, var(--interactive-secondary-bg) 70%, transparent);
  color: var(--text-primary);
  font: 600 0.6rem/1 var(--font-mono);
  letter-spacing: 0.04em;
}

.forward-sheet__saved-button.is-active {
  border-color: color-mix(in srgb, var(--interactive-primary-bg) 24%, var(--border-default));
  background: color-mix(in srgb, var(--interactive-selected-bg) 90%, var(--surface-panel));
}

.forward-sheet__saved-button.is-active .forward-sheet__pill {
  border-color: color-mix(in srgb, var(--interactive-primary-bg) 20%, var(--border-default));
  background: color-mix(in srgb, var(--interactive-primary-bg) 10%, var(--interactive-secondary-bg));
  color: color-mix(in srgb, var(--interactive-primary-bg) 82%, var(--text-primary));
}

.compose-dialog {
  display: grid;
  gap: 12px;
}

.compose-dialog__users {
  display: grid;
  gap: 9px;
  max-height: 360px;
  overflow: auto;
}

.compose-dialog__user {
  display: grid;
  grid-template-columns: 48px minmax(0, 1fr);
  gap: 12px;
  align-items: center;
  padding: 13px;
  border: 1px solid var(--border-default);
  border-radius: var(--radius-md);
  background: color-mix(in srgb, var(--surface-card) 92%, transparent);
  text-align: left;
}

.compose-dialog__user:not(.forward-sheet__conversation).is-selected {
  border-color: color-mix(in srgb, var(--interactive-primary-bg) 30%, var(--border-default));
  background: color-mix(in srgb, var(--interactive-selected-bg) 92%, var(--surface-card));
}

.compose-dialog__user-copy {
  min-width: 0;
  display: grid;
  gap: 2px;
}

.compose-dialog__user-copy strong,
.compose-dialog__user-copy span,
.compose-dialog__user-copy p {
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.compose-dialog__user-copy span,
.compose-dialog__user-copy p {
  color: var(--text-tertiary);
  font-size: 0.78rem;
}

.compose-dialog__empty,
.compose-dialog__error {
  color: var(--text-tertiary);
  font-size: 0.8rem;
}

.compose-dialog__error {
  color: var(--status-danger);
}

:deep(.forward-dialog .el-dialog) {
  border: 1px solid var(--border-default);
  border-radius: var(--radius-card);
  background: color-mix(in srgb, var(--surface-overlay) 96%, transparent);
  box-shadow: var(--shadow-overlay);
}

:deep(.forward-dialog .el-dialog__header) {
  padding-bottom: 4px;
}

:deep(.forward-dialog .el-dialog__headerbtn) {
  width: 34px;
  height: 34px;
  border: 1px solid transparent;
  border-radius: 12px;
  color: var(--text-quaternary);
  transition:
    border-color var(--motion-fast) var(--motion-ease-out),
    background var(--motion-fast) var(--motion-ease-out),
    color var(--motion-fast) var(--motion-ease-out);
}

:deep(.forward-dialog .el-dialog__headerbtn:hover),
:deep(.forward-dialog .el-dialog__headerbtn:focus-visible) {
  border-color: var(--border-default);
  background: var(--interactive-secondary-bg-hover);
  color: var(--text-primary);
}

:deep(.forward-dialog .el-dialog__title) {
  font: 620 1rem/1.08 var(--font-display);
  letter-spacing: -0.02em;
}

:deep(.forward-dialog .el-dialog__body) {
  padding-top: 12px;
}

:deep(.forward-dialog .el-dialog__footer) {
  padding-top: 10px;
  border-top: 1px solid color-mix(in srgb, var(--border-default) 76%, transparent);
}

:deep(.forward-dialog .el-dialog__footer .el-button) {
  min-height: 38px;
  padding-inline: 14px;
  border-radius: 12px;
  border-color: var(--border-default);
  background: color-mix(in srgb, var(--surface-card) 90%, transparent);
  color: var(--text-primary);
  box-shadow: none;
  transition:
    border-color var(--motion-fast) ease,
    background var(--motion-fast) ease,
    color var(--motion-fast) ease,
    opacity var(--motion-fast) ease;
}

:deep(.forward-dialog .el-dialog__footer .el-button:hover),
:deep(.forward-dialog .el-dialog__footer .el-button:focus-visible) {
  border-color: var(--border-strong);
  background: var(--interactive-secondary-bg-hover);
}

:deep(.forward-dialog .el-dialog__footer .el-button--primary) {
  border-color: color-mix(in srgb, var(--interactive-primary-bg) 22%, var(--border-default));
  background: color-mix(in srgb, var(--interactive-primary-bg) 10%, var(--surface-card));
  color: var(--interactive-selected-fg);
}

:deep(.forward-dialog .el-dialog__footer .el-button--primary:hover),
:deep(.forward-dialog .el-dialog__footer .el-button--primary:focus-visible) {
  border-color: color-mix(in srgb, var(--interactive-primary-bg) 32%, var(--border-default));
  background: color-mix(in srgb, var(--interactive-primary-bg) 12%, var(--interactive-secondary-bg-hover));
}

:deep(.forward-dialog .el-dialog__footer .el-button.is-loading) {
  opacity: 0.92;
}

:deep(.forward-dialog .el-input__wrapper) {
  min-height: 42px;
  border-radius: var(--radius-control);
  background: color-mix(in srgb, var(--surface-card) 94%, transparent);
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.08),
    0 0 0 1px color-mix(in srgb, var(--border-default) 82%, transparent);
}

:deep(.forward-dialog .el-input__wrapper.is-focus) {
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.1),
    0 0 0 1px color-mix(in srgb, var(--interactive-primary-bg) 24%, transparent),
    0 0 0 4px color-mix(in srgb, var(--interactive-focus-ring) 28%, transparent);
}

:deep(.forward-dialog .el-input__inner) {
  color: var(--text-primary);
}

:deep(.forward-dialog .el-input__prefix),
:deep(.forward-dialog .el-input__suffix) {
  color: var(--text-quaternary);
}

.forward-sheet .compose-dialog__users {
  gap: 7px;
  max-height: 344px;
  padding-right: 2px;
}

.forward-sheet__conversation {
  grid-template-columns: 48px minmax(0, 1fr);
  gap: 11px;
  align-items: start;
  padding: 12px 13px;
  border-radius: var(--radius-control);
  background: color-mix(in srgb, var(--surface-card) 88%, transparent);
  transition:
    border-color var(--motion-fast) var(--motion-ease-out),
    background var(--motion-fast) var(--motion-ease-out),
    color var(--motion-fast) var(--motion-ease-out);
}

.forward-sheet__conversation:hover,
.forward-sheet__conversation:focus-visible {
  border-color: var(--border-strong);
  background: var(--interactive-secondary-bg-hover);
}

.forward-sheet__conversation:focus-visible {
  box-shadow: 0 0 0 4px color-mix(in srgb, var(--interactive-focus-ring) 28%, transparent);
  outline: none;
}

.forward-sheet__conversation.is-selected {
  border-color: color-mix(in srgb, var(--interactive-primary-bg) 24%, var(--border-default));
  background: color-mix(in srgb, var(--interactive-selected-bg) 90%, var(--surface-card));
}

.forward-sheet__conversation-copy {
  gap: 4px;
}

.forward-sheet__conversation-head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 12px;
}

.forward-sheet__conversation-state {
  flex-shrink: 0;
  color: var(--text-quaternary);
  font: 600 0.58rem/1 var(--font-mono);
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.forward-sheet__conversation.is-selected .forward-sheet__conversation-state {
  color: color-mix(in srgb, var(--interactive-primary-bg) 82%, var(--text-primary));
}

.forward-sheet__conversation-copy p {
  white-space: normal;
  display: -webkit-box;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
  line-clamp: 2;
}

.forward-sheet__empty,
.forward-sheet__error {
  display: grid;
  gap: 5px;
  padding: 12px 13px;
  border-radius: 12px;
  border: 1px dashed var(--border-default);
  background: color-mix(in srgb, var(--surface-panel) 74%, transparent);
}

.forward-sheet__empty strong,
.forward-sheet__empty p {
  display: block;
  margin: 0;
}

.forward-sheet__empty strong {
  color: var(--text-primary);
  font: 600 0.78rem/1.2 var(--font-body);
}

.forward-sheet__empty p {
  color: var(--text-tertiary);
  font-size: 0.72rem;
  line-height: 1.45;
}

.forward-sheet__error {
  border-style: solid;
  border-color: color-mix(in srgb, var(--status-danger) 16%, var(--border-default));
  background: color-mix(in srgb, var(--status-danger) 4%, var(--surface-panel));
}

@media (max-width: 1279px) {
  .chat-page {
    grid-template-columns: 332px minmax(0, 1fr);
  }
}

@media (max-width: 767px) {
  .search-sheet__hero {
    display: grid;
    grid-template-columns: 1fr;
  }

  .search-sheet__hero-shortcut {
    justify-self: start;
  }

  .search-sheet__results {
    grid-template-columns: 1fr;
  }

  .chat-page {
    grid-template-columns: 1fr;
    gap: 0;
    border: 0;
    border-radius: 0;
    box-shadow: none;
    backdrop-filter: none;
  }

  .chat-page__sidebar,
  .chat-page__main {
    border-inline: 0;
    border-radius: 0;
  }

  .chat-page__profile {
    border-left: 0;
  }

  .chat-page__forward-bar {
    flex-direction: column;
    align-items: stretch;
  }

}
</style>
