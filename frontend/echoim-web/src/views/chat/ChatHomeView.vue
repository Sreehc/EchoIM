<script setup lang="ts">
import { computed, nextTick, onMounted, onUnmounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import ConversationSidebar from '@/components/chat/ConversationSidebar.vue'
import ContactsPanel from '@/components/chat/ContactsPanel.vue'
import ChatTopbar from '@/components/chat/ChatTopbar.vue'
import CallOverlay from '@/components/chat/CallOverlay.vue'
import ImageViewer from '@/components/chat/ImageViewer.vue'
import type { ImageViewerImage } from '@/components/chat/ImageViewer.vue'
import MessagePane from '@/components/chat/MessagePane.vue'
import MessageComposer from '@/components/chat/MessageComposer.vue'
import type { VoiceRecordResult } from '@/components/chat/VoiceRecorder.vue'
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
import { ChatRound, Close, Guide, Search, UserFilled } from '@element-plus/icons-vue'
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

const editGroupDialog = reactive({
  visible: false,
  field: 'name' as 'name' | 'notice',
  value: '',
  saving: false,
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
const imageViewerState = reactive({
  visible: false,
  images: [] as ImageViewerImage[],
  startIndex: 0,
})

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

const typingLabel = computed(() => {
  const convId = chatStore.activeConversationId
  if (!convId) return ''
  const userIds = chatStore.getTypingUserIds(convId)
  if (!userIds.length) return ''
  // For simplicity, show generic label (peer name resolution would require profile lookup)
  return userIds.length === 1 ? '对方正在输入...' : `${userIds.length} 人正在输入...`
})

function getAddContactState(user: UserSearchItem) {
  switch (user.friendStatus) {
    case 'FRIEND':
      return {
        label: '已是好友',
        tone: 'success',
        actionable: true,
        actionLabel: '发起聊天',
      }
    case 'PENDING_OUT':
    case 'PENDING_IN':
      return {
        label: '申请处理中',
        tone: 'pending',
        actionable: false,
        actionLabel: '处理中',
      }
    case 'BLOCKED_OUT':
      return {
        label: '已拉黑',
        tone: 'danger',
        actionable: false,
        actionLabel: '已拉黑',
      }
    case 'BLOCKED_IN':
      return {
        label: '对方已拉黑',
        tone: 'danger',
        actionable: false,
        actionLabel: '不可添加',
      }
    case 'SELF':
      return {
        label: '本人',
        tone: 'neutral',
        actionable: false,
        actionLabel: '当前账号',
      }
    default:
      return {
        label: '可添加',
        tone: 'brand',
        actionable: true,
        actionLabel: '加好友',
      }
  }
}

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

function handleOpenImageViewer(messageId: number, imageUrl: string) {
  const imageMessages = chatStore.activeMessages.filter(
    (message) => message.msgType === 'IMAGE' && message.file?.downloadUrl,
  )
  const viewerImages: ImageViewerImage[] = imageMessages.map((message) => ({
    messageId: message.messageId,
    imageUrl: message.file!.downloadUrl!,
    fileName: message.file?.fileName ?? null,
  }))
  const startIndex = viewerImages.findIndex((img) => img.messageId === messageId)
  imageViewerState.images = viewerImages
  imageViewerState.startIndex = startIndex >= 0 ? startIndex : 0
  imageViewerState.visible = true
}

function handleImageViewerForward(messageId: number) {
  const message = chatStore.activeMessages.find((m) => m.messageId === messageId)
  if (message) {
    imageViewerState.visible = false
    handleForwardMessage(message)
  }
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

function openEditGroupDialog(field: 'name' | 'notice') {
  if (!chatStore.activeProfile?.group) return
  editGroupDialog.field = field
  editGroupDialog.value = field === 'name'
    ? chatStore.activeProfile.group.groupName
    : chatStore.activeProfile.group.notice ?? ''
  editGroupDialog.saving = false
  editGroupDialog.visible = true
}

async function submitEditGroupDialog() {
  if (!chatStore.activeConversation?.groupId) return
  editGroupDialog.saving = true
  try {
    const payload = editGroupDialog.field === 'name'
      ? { groupName: editGroupDialog.value.trim() }
      : { notice: editGroupDialog.value.trim() }
    await updateGroup(chatStore.activeConversation.groupId, payload)
    await chatStore.fetchConversationProfile(chatStore.activeConversation.conversationId, true)
    editGroupDialog.visible = false
  } catch {
    // error handled by store
  } finally {
    editGroupDialog.saving = false
  }
}

async function handleUpdateGroupMeta() {
  openEditGroupDialog('name')
}

async function handleUpdateGroupNotice() {
  openEditGroupDialog('notice')
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
  try {
    await ElMessageBox.confirm('退出后将不再收到该会话的新消息，确认退出？', '退出会话', {
      confirmButtonText: '退出',
      cancelButtonText: '取消',
      type: 'warning',
    })
  } catch {
    return
  }
  await leaveGroup(chatStore.activeConversation.groupId, true)
  uiStore.setProfileOpen(false)
  await chatStore.refreshConversationList(true)
  await router.push('/chat')
}

async function handleDissolveActiveGroup() {
  if (!chatStore.activeConversation?.groupId) return
  try {
    await ElMessageBox.confirm('解散后所有成员将无法继续在该会话中发送消息，此操作不可撤销。', '解散会话', {
      confirmButtonText: '解散',
      cancelButtonText: '取消',
      type: 'error',
    })
  } catch {
    return
  }
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

async function handleStartCall(callType: 'audio' | 'video' = 'audio') {
  if (!chatStore.activeConversation) return
  try {
    await callStore.startOutgoingCall(chatStore.activeConversation, callType)
  } catch (error) {
    const label = callType === 'video' ? '发起视频通话失败' : '发起语音通话失败'
    chatStore.errors.noticeMessage = error instanceof Error ? error.message : label
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

let typingThrottleTimer: ReturnType<typeof setTimeout> | null = null
function handleTypingInput() {
  const convId = chatStore.activeConversationId
  if (!convId) return
  if (typingThrottleTimer) return // Throttle: send at most once per 2 seconds
  chatStore.sendTyping(convId)
  typingThrottleTimer = setTimeout(() => {
    typingThrottleTimer = null
  }, 2000)
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

async function handleUploadMultipleFiles(files: File[]) {
  if (!files.length) return
  attachmentUploading.value = true
  attachmentError.value = null
  let uploadedCount = 0

  try {
    for (const file of files) {
      const bizType = file.type.startsWith('image/') ? 2 : 4
      const uploadedFile = (await uploadFile(file, bizType)) as ChatFile
      await chatStore.sendMessage({
        currentUserId: authStore.currentUser?.userId ?? 0,
        content: null,
        msgType: file.type === 'image/gif' ? 'GIF' : bizType === 2 ? 'IMAGE' : 'FILE',
        fileId: uploadedFile.fileId,
        file: uploadedFile,
      })
      uploadedCount++
    }
    if (uploadedCount > 1) {
      chatStore.errors.noticeMessage = `已发送 ${uploadedCount} 张图片`
    }
  } catch (error) {
    const remaining = files.length - uploadedCount
    attachmentError.value = error instanceof Error
      ? `${error.message}（已发送 ${uploadedCount} 张，剩余 ${remaining} 张发送失败）`
      : `附件上传失败（已发送 ${uploadedCount} 张）`
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

async function handleSendVoiceMessage(result: VoiceRecordResult) {
  attachmentUploading.value = true
  attachmentError.value = null

  try {
    const ext = result.blob.type.includes('webm') ? 'webm' : result.blob.type.includes('ogg') ? 'ogg' : 'm4a'
    const file = new File([result.blob], `voice-${Date.now()}.${ext}`, { type: result.blob.type })
    const uploaded = await uploadFile(file, 5)

    await chatStore.sendMessage({
      currentUserId: authStore.currentUser?.userId ?? 0,
      content: null,
      msgType: 'VOICE',
      fileId: uploaded.fileId,
      file: uploaded,
      replySource: toReplySource(replyingMessage.value),
      voice: {
        duration: result.duration,
        waveform: result.waveform,
      },
    })
    clearReplyMessage()
  } catch (error) {
    attachmentError.value = error instanceof Error ? error.message : '语音发送失败'
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
  if (imageViewerState.visible) {
    imageViewerState.visible = false
    event.preventDefault()
    return
  }
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
          @start-call="(callType) => handleStartCall(callType)"
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
            @open-image-viewer="handleOpenImageViewer"
          />
          <div v-if="typingLabel" class="chat-page__typing" data-testid="typing-indicator">
            {{ typingLabel }}
          </div>
          <div
            v-if="chatStore.activeConversation.groupStatus === 2"
            class="chat-page__dissolved"
          >
            {{ chatStore.activeConversation.conversationType === 3 ? '该频道已解散' : '该群已解散' }}
          </div>
          <MessageComposer
            v-else
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
            @upload-files="handleUploadMultipleFiles"
            @send-sticker="handleSendSticker"
            @send-voice="handleSendVoiceMessage"
            @typing="handleTypingInput"
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
      :local-camera-off="callStore.localCameraOff"
      :busy="callStore.busy"
      :error="callStore.error"
      :local-stream="callStore.localStream"
      :remote-stream="callStore.remoteStream"
      @accept="callStore.acceptIncomingCall"
      @reject="callStore.rejectIncomingCall"
      @cancel="callStore.cancelOutgoingCall"
      @end="callStore.endCurrentCall"
      @toggle-minimized="callStore.toggleMinimized"
      @toggle-mute="callStore.toggleMute"
      @toggle-camera="callStore.toggleCamera"
    />

    <ImageViewer
      :visible="imageViewerState.visible"
      :images="imageViewerState.images"
      :start-index="imageViewerState.startIndex"
      @close="imageViewerState.visible = false"
      @forward="handleImageViewerForward"
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
      width="520px"
      destroy-on-close
      :show-close="false"
      class="compose-modal"
    >
      <header class="compose-modal__header">
        <div class="compose-modal__header-main">
          <span class="compose-modal__icon" :class="`compose-modal__icon--${composeDialog.mode}`">
            <ChatRound v-if="composeDialog.mode === 'single'" />
            <UserFilled v-else-if="composeDialog.mode === 'group'" />
            <Guide v-else />
          </span>
          <div class="compose-modal__header-copy">
            <strong>
              {{ composeDialog.mode === 'single' ? '新建私聊' : composeDialog.mode === 'channel' ? '新建频道' : '新建群组' }}
            </strong>
            <p>
              {{ composeDialog.mode === 'single' ? '搜索并选择一位用户开始对话' : composeDialog.mode === 'channel' ? '创建频道，向关注者广播消息' : '创建群组，邀请成员一起交流' }}
            </p>
          </div>
        </div>
        <button class="compose-modal__close" type="button" aria-label="关闭" @click="closeComposeDialog">
          <Close />
        </button>
      </header>

      <div class="compose-modal__body">
        <div v-if="composeDialog.mode !== 'single'" class="compose-modal__name-field">
          <label class="compose-modal__label">
            {{ composeDialog.mode === 'channel' ? '频道名称' : '群组名称' }}
          </label>
          <el-input
            v-model="composeDialog.groupName"
            :placeholder="composeDialog.mode === 'channel' ? '给频道起个名字' : '给群组起个名字'"
            maxlength="40"
            class="compose-modal__name-input"
          />
        </div>

        <div class="compose-modal__search-field">
          <label class="compose-modal__label">添加成员</label>
          <div class="compose-modal__search-bar">
            <Search class="compose-modal__search-icon" />
            <el-input
              v-model="composeDialog.keyword"
              placeholder="搜索昵称、用户名或编号"
              clearable
              class="compose-modal__search-input"
            />
          </div>
        </div>

        <div v-if="composeDialog.selectedUserIds.length" class="compose-modal__chips">
          <span
            v-for="uid in composeDialog.selectedUserIds"
            :key="uid"
            class="compose-modal__chip"
          >
            {{ composeDialog.users.find(u => u.userId === uid)?.nickname ?? uid }}
            <button type="button" aria-label="移除" @click="toggleComposeUser(uid)">
              <Close />
            </button>
          </span>
        </div>

        <div v-if="composeDialog.error" class="compose-modal__error">{{ composeDialog.error }}</div>

        <div class="compose-modal__user-list">
          <div v-if="composeDialog.loading" class="compose-modal__status">
            <span class="compose-modal__spinner"></span>
            正在搜索…
          </div>
          <button
            v-for="user in composeDialog.users"
            :key="user.userId"
            class="compose-modal__user"
            :class="{ 'is-selected': composeDialog.selectedUserIds.includes(user.userId) }"
            type="button"
            @click="toggleComposeUser(user.userId)"
          >
            <AvatarBadge :name="user.nickname" :avatar-url="user.avatarUrl" size="lg" />
            <div class="compose-modal__user-copy">
              <div class="compose-modal__user-head">
                <strong>{{ user.nickname }}</strong>
                <span class="compose-modal__user-tag">@{{ user.username }}</span>
              </div>
              <p>{{ user.signature || user.userNo }}</p>
            </div>
            <span v-if="composeDialog.selectedUserIds.includes(user.userId)" class="compose-modal__check">
              <svg viewBox="0 0 16 16" fill="none"><path d="M3.5 8.5L6.5 11.5L12.5 4.5" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"/></svg>
            </span>
          </button>
          <div v-if="!composeDialog.loading && !composeDialog.users.length && composeDialog.keyword" class="compose-modal__status">
            没有找到匹配的用户
          </div>
          <div v-if="!composeDialog.loading && !composeDialog.users.length && !composeDialog.keyword" class="compose-modal__status compose-modal__status--hint">
            输入关键词搜索用户
          </div>
        </div>
      </div>

      <footer class="compose-modal__footer">
        <button class="compose-modal__btn compose-modal__btn--secondary" type="button" @click="closeComposeDialog">
          取消
        </button>
        <button
          class="compose-modal__btn compose-modal__btn--primary"
          type="button"
          :disabled="composeDialog.submitting || (composeDialog.mode === 'single' ? !composeDialog.selectedUserIds.length : (!composeDialog.groupName.trim() || !composeDialog.selectedUserIds.length))"
          @click="submitComposeDialog"
        >
          <span v-if="composeDialog.submitting" class="compose-modal__spinner compose-modal__spinner--light"></span>
          {{ composeDialog.mode === 'single' ? '开始聊天' : '创建并进入' }}
        </button>
      </footer>
    </el-dialog>

    <el-dialog v-model="contactsState.addDialogVisible" title="添加联系人" width="520px" destroy-on-close class="add-contact-dialog">
      <div class="compose-dialog">
        <el-input v-model="contactsState.addKeyword" placeholder="搜索昵称、用户名或账号编号" clearable />
        <div v-if="contactsState.addError" class="compose-dialog__error">{{ contactsState.addError }}</div>
        <div class="compose-dialog__users compose-dialog__users--contacts">
          <div v-if="contactsState.addLoading" class="compose-dialog__empty">正在搜索用户…</div>
          <article v-for="user in contactsState.addUsers" :key="user.userId" class="compose-dialog__user compose-dialog__user--result">
            <AvatarBadge :name="user.nickname" :avatar-url="user.avatarUrl" size="lg" />
            <div class="compose-dialog__user-copy">
              <div class="compose-dialog__user-head">
                <strong>{{ user.nickname }}</strong>
                <span class="compose-dialog__status" :class="`is-${getAddContactState(user).tone}`">
                  {{ getAddContactState(user).label }}
                </span>
              </div>
              <div class="compose-dialog__user-meta">
                <span>@{{ user.username }}</span>
                <button class="compose-dialog__mini-link" type="button" @click.stop="openPublicProfile(user.username)">主页</button>
              </div>
              <p>{{ user.signature || user.userNo }}</p>
            </div>
            <div class="compose-dialog__result-actions">
              <button
                v-if="getAddContactState(user).actionable && user.friendStatus === 'FRIEND'"
                class="compose-dialog__inline-action compose-dialog__inline-action--secondary"
                type="button"
                @click="openChatFromContact(user.userId)"
              >
                发起聊天
              </button>
              <button
                v-else-if="getAddContactState(user).actionable"
                class="compose-dialog__inline-action compose-dialog__inline-action--primary"
                type="button"
                @click="submitFriendRequestForUser(user.userId)"
              >
                加好友
              </button>
              <span v-else class="compose-dialog__result-note" :class="`is-${getAddContactState(user).tone}`">
                {{ getAddContactState(user).actionLabel }}
              </span>
            </div>
          </article>
          <div v-if="!contactsState.addLoading && !contactsState.addUsers.length" class="compose-dialog__empty">没有匹配的用户</div>
        </div>
      </div>
    </el-dialog>

    <el-dialog v-model="globalSearchState.visible" title="全局搜索" width="960px" destroy-on-close class="global-search-dialog">
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

    <el-dialog
      v-model="editGroupDialog.visible"
      :title="editGroupDialog.field === 'name' ? '编辑名称' : '编辑公告'"
      width="440px"
      destroy-on-close
      :show-close="false"
      class="edit-group-modal"
    >
      <header class="edit-group-modal__header">
        <strong>{{ editGroupDialog.field === 'name' ? '编辑名称' : '编辑公告' }}</strong>
        <button class="edit-group-modal__close" type="button" aria-label="关闭" @click="editGroupDialog.visible = false">
          <Close />
        </button>
      </header>
      <div class="edit-group-modal__body">
        <label class="edit-group-modal__label">
          {{ editGroupDialog.field === 'name' ? (chatStore.activeConversation?.conversationType === 3 ? '频道名称' : '群聊名称') : (chatStore.activeConversation?.conversationType === 3 ? '频道公告' : '群公告') }}
        </label>
        <el-input
          v-model="editGroupDialog.value"
          :type="editGroupDialog.field === 'notice' ? 'textarea' : 'text'"
          :autosize="editGroupDialog.field === 'notice' ? { minRows: 4, maxRows: 8 } : undefined"
          :maxlength="editGroupDialog.field === 'name' ? 40 : 500"
          :placeholder="editGroupDialog.field === 'name' ? '输入新的名称' : '输入公告内容'"
          :disabled="editGroupDialog.saving"
          class="edit-group-modal__input"
        />
      </div>
      <footer class="edit-group-modal__footer">
        <button class="edit-group-modal__btn edit-group-modal__btn--secondary" type="button" @click="editGroupDialog.visible = false">
          取消
        </button>
        <button
          class="edit-group-modal__btn edit-group-modal__btn--primary"
          type="button"
          :disabled="editGroupDialog.saving || !editGroupDialog.value.trim()"
          @click="submitEditGroupDialog"
        >
          <span v-if="editGroupDialog.saving" class="edit-group-modal__spinner"></span>
          保存
        </button>
      </footer>
    </el-dialog>
  </main>
</template>

<style scoped>
.chat-page {
  display: grid;
  grid-template-columns: clamp(280px, 25vw, 332px) minmax(0, 1fr);
  gap: 0;
  height: 100%;
  min-height: 0;
  border: 1px solid var(--border-default);
  border-radius: var(--radius-lg);
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

.chat-page__stage::after {
  content: '';
  position: absolute;
  inset: 0;
  pointer-events: none;
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
  font: 500 var(--text-sm)/1.45 var(--font-body);
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
  min-width: var(--btn-min-size);
  min-height: var(--btn-min-size);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border: 0;
  background: transparent;
  color: inherit;
  font: inherit;
}

.chat-page__typing {
  padding: 4px 16px 0;
  font-size: 12px;
  color: var(--text-tertiary);
  animation: typing-pulse 1.5s ease-in-out infinite;
}

.chat-page__dissolved {
  padding: 20px 24px;
  text-align: center;
  font-size: var(--text-sm);
  color: var(--text-quaternary);
}

@keyframes typing-pulse {
  0%, 100% { opacity: 0.6; }
  50% { opacity: 1; }
}

.chat-page__empty {
  flex: 1;
  min-height: 0;
  position: relative;
  background:
    linear-gradient(180deg, rgba(10, 10, 16, 0.1), transparent 14%),
    var(--chat-stage-base);
}

.chat-page__empty::after {
  content: '';
  position: absolute;
  inset: 0;
  pointer-events: none;
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
  font: 600 var(--text-xs)/1 var(--font-mono);
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.chat-page__forward-bar strong {
  margin-top: 5px;
  color: var(--text-primary);
  font-size: var(--text-base);
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
  min-height: var(--btn-min-size);
  padding: 0 14px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border: 1px solid var(--border-default);
  border-radius: var(--radius-control);
  background: var(--interactive-secondary-bg);
}

.compose-dialog__inline-action {
  font: 600 var(--text-sm)/1 var(--font-body);
}

.compose-dialog__user--result {
  grid-template-columns: 48px minmax(0, 1fr) auto;
  gap: 11px;
  align-items: center;
  padding: 10px 11px;
}

.compose-dialog__user-head {
  display: flex;
  align-items: center;
  gap: 7px;
  min-width: 0;
}

.compose-dialog__user-head strong {
  min-width: 0;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.compose-dialog__user-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}

.compose-dialog__status,
.compose-dialog__result-note {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 24px;
  padding: 0 7px;
  border-radius: var(--radius-pill);
  border: 1px solid var(--border-default);
  background: color-mix(in srgb, var(--interactive-secondary-bg) 88%, transparent);
  font: 600 var(--text-2xs)/1 var(--font-body);
  white-space: nowrap;
}

.compose-dialog__status.is-brand,
.compose-dialog__result-note.is-brand {
  border-color: color-mix(in srgb, var(--interactive-primary-bg) 20%, var(--border-default));
  color: var(--interactive-selected-fg);
}

.compose-dialog__status.is-success,
.compose-dialog__result-note.is-success {
  border-color: color-mix(in srgb, var(--status-success) 18%, var(--border-default));
  color: var(--status-success);
}

.compose-dialog__status.is-pending,
.compose-dialog__result-note.is-pending {
  border-color: color-mix(in srgb, var(--status-warning) 22%, var(--border-default));
  color: color-mix(in srgb, var(--status-warning) 72%, var(--text-primary));
}

.compose-dialog__status.is-danger,
.compose-dialog__result-note.is-danger {
  border-color: color-mix(in srgb, var(--status-danger) 22%, var(--border-default));
  color: var(--text-danger);
}

.compose-dialog__status.is-neutral,
.compose-dialog__result-note.is-neutral {
  color: var(--text-tertiary);
}

.compose-dialog__result-actions {
  display: inline-flex;
  align-items: center;
  justify-content: flex-end;
  min-width: 84px;
}

.compose-dialog__inline-action--primary {
  border-color: color-mix(in srgb, var(--interactive-primary-bg) 24%, var(--border-default));
  background: color-mix(in srgb, var(--interactive-primary-bg) 10%, var(--interactive-secondary-bg));
  color: var(--interactive-selected-fg);
}

.compose-dialog__inline-action--secondary {
  min-width: 84px;
}

.compose-dialog__result-note {
  min-width: 84px;
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
  font: 620 var(--text-base)/1.12 var(--font-display);
  letter-spacing: -0.02em;
}

.search-sheet__hero-copy p {
  margin-top: 4px;
  color: var(--text-tertiary);
  font-size: var(--text-sm);
  line-height: 1.48;
}

.search-sheet__hero-shortcut {
  flex: 0 0 auto;
  padding: 5px 8px;
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-pill);
  background: color-mix(in srgb, var(--surface-panel) 90%, transparent);
  color: var(--text-quaternary);
  font: 600 var(--text-xs)/1 var(--font-mono);
}

.search-sheet__summary {
  min-height: 20px;
  display: flex;
  align-items: center;
  color: var(--text-tertiary);
  font-size: var(--text-xs);
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
  font: 620 var(--text-base)/1.16 var(--font-display);
}

.search-sheet__blank p {
  color: var(--text-tertiary);
  font-size: var(--text-sm);
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
  font: 620 var(--text-sm)/1.12 var(--font-display);
}

.search-sheet__section-head span {
  padding: 4px 7px;
  border-radius: var(--radius-pill);
  background: var(--interactive-secondary-bg);
  color: var(--text-quaternary);
  font: 700 var(--text-xs)/1 var(--font-mono);
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
  font-size: var(--text-sm);
  font-weight: 600;
  line-height: 1.28;
}

.search-sheet__row-copy span,
.search-sheet__row-copy p {
  color: var(--text-tertiary);
  font-size: var(--text-xs);
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
  font: 600 var(--text-xs)/1 var(--font-mono);
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
  border-radius: var(--radius-md);
  color: var(--text-quaternary);
  font-size: var(--text-sm);
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
  font: 600 var(--text-2xs)/1 var(--font-mono);
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.forward-sheet__summary strong {
  color: var(--text-primary);
  font: 600 var(--text-sm)/1.2 var(--font-body);
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
  font-size: var(--text-sm);
  font-weight: 600;
  line-height: 1.22;
}

.forward-sheet__saved-copy span {
  color: var(--text-tertiary);
  font-size: var(--text-xs);
  line-height: 1.42;
}

.forward-sheet__pill {
  flex-shrink: 0;
  padding: 5px 8px;
  border-radius: var(--radius-pill);
  border: 1px solid var(--border-default);
  background: color-mix(in srgb, var(--interactive-secondary-bg) 70%, transparent);
  color: var(--text-primary);
  font: 600 var(--text-2xs)/1 var(--font-mono);
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

.compose-dialog__users--contacts {
  max-height: 58vh;
  gap: 8px;
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
  font-size: var(--text-sm);
}

.compose-dialog__user--result .compose-dialog__user-copy {
  gap: 4px;
}

.compose-dialog__user--result .compose-dialog__user-copy p {
  white-space: normal;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  font-size: var(--text-xs);
  line-height: 1.34;
}

.compose-dialog__user--result .compose-dialog__mini-link {
  flex-shrink: 0;
}

.compose-dialog__user--result :deep(.avatar-badge) {
  width: 42px;
  height: 42px;
}

.compose-dialog__user--result .compose-dialog__user-head strong {
  font-size: var(--text-base);
  line-height: 1.16;
}

.compose-dialog__user--result .compose-dialog__user-meta span {
  font-size: var(--text-xs);
}

.compose-dialog__user--result .compose-dialog__mini-link {
  padding: 4px 7px;
  font-size: var(--text-xs);
}

.compose-dialog__empty,
.compose-dialog__error {
  color: var(--text-tertiary);
  font-size: var(--text-sm);
}

.compose-dialog__error {
  color: var(--status-danger);
}

/* ── Compose modal ── */

:deep(.compose-modal .el-dialog) {
  border: 1px solid var(--border-default);
  border-radius: var(--radius-card);
  background: color-mix(in srgb, var(--surface-overlay) 97%, transparent);
  box-shadow: var(--shadow-overlay);
  overflow: hidden;
}

:deep(.compose-modal .el-dialog__header) {
  display: none;
}

:deep(.compose-modal .el-dialog__body) {
  padding: 0;
}

:deep(.compose-modal .el-dialog__footer) {
  display: none;
}

.compose-modal__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  padding: 22px 24px 16px;
}

.compose-modal__header-main {
  display: flex;
  align-items: flex-start;
  gap: 14px;
}

.compose-modal__icon {
  width: 40px;
  height: 40px;
  display: grid;
  place-items: center;
  border-radius: var(--radius-md);
  flex-shrink: 0;
}

.compose-modal__icon svg {
  width: 20px;
  height: 20px;
}

.compose-modal__icon--single {
  background: color-mix(in srgb, var(--interactive-primary-bg) 12%, transparent);
  color: var(--interactive-primary-bg);
}

.compose-modal__icon--group {
  background: color-mix(in srgb, var(--status-success) 12%, transparent);
  color: var(--status-success);
}

.compose-modal__icon--channel {
  background: color-mix(in srgb, var(--status-warning) 12%, transparent);
  color: var(--status-warning);
}

.compose-modal__header-copy strong {
  display: block;
  font: 620 var(--text-lg)/1.12 var(--font-display);
  color: var(--text-primary);
  letter-spacing: -0.02em;
}

.compose-modal__header-copy p {
  margin-top: 4px;
  color: var(--text-tertiary);
  font-size: var(--text-sm);
  line-height: 1.3;
}

.compose-modal__close {
  width: 32px;
  height: 32px;
  display: grid;
  place-items: center;
  border: 0;
  border-radius: var(--radius-control);
  background: transparent;
  color: var(--text-tertiary);
  flex-shrink: 0;
  transition:
    background var(--motion-fast) var(--motion-ease-out),
    color var(--motion-fast) var(--motion-ease-out);
}

.compose-modal__close svg {
  width: 16px;
  height: 16px;
}

.compose-modal__close:hover,
.compose-modal__close:focus-visible {
  background: var(--interactive-secondary-bg-hover);
  color: var(--text-primary);
}

.compose-modal__body {
  display: grid;
  gap: 14px;
  padding: 0 24px;
  max-height: min(56vh, 460px);
  overflow-y: auto;
}

.compose-modal__label {
  display: block;
  margin-bottom: 6px;
  color: var(--text-secondary);
  font: 500 var(--text-xs)/1 var(--font-mono);
  letter-spacing: 0.06em;
  text-transform: uppercase;
}

.compose-modal__name-field :deep(.el-input__wrapper) {
  border-radius: var(--radius-md);
  background: var(--surface-card);
  box-shadow: 0 0 0 1px var(--border-default);
  padding: 4px 12px;
  min-height: 42px;
}

.compose-modal__name-field :deep(.el-input__inner) {
  font-size: var(--text-base);
  font-weight: 500;
  color: var(--text-primary);
}

.compose-modal__search-bar {
  position: relative;
  display: flex;
  align-items: center;
}

.compose-modal__search-icon {
  position: absolute;
  left: 12px;
  width: 15px;
  height: 15px;
  color: var(--text-quaternary);
  pointer-events: none;
  z-index: 1;
}

.compose-modal__search-input :deep(.el-input__wrapper) {
  border-radius: var(--radius-md);
  background: var(--surface-card);
  box-shadow: 0 0 0 1px var(--border-default);
  padding: 4px 12px 4px 34px;
  min-height: 40px;
}

.compose-modal__search-input :deep(.el-input__inner) {
  font-size: var(--text-sm);
  color: var(--text-primary);
}

.compose-modal__search-input :deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 1px var(--interactive-primary-bg) !important;
}

.compose-modal__chips {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.compose-modal__chip {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 4px 6px 4px 10px;
  border-radius: var(--radius-pill);
  background: color-mix(in srgb, var(--interactive-selected-bg) 80%, var(--surface-card));
  border: 1px solid color-mix(in srgb, var(--interactive-primary-bg) 16%, var(--border-default));
  color: var(--text-primary);
  font-size: var(--text-xs);
  font-weight: 500;
  line-height: 1;
}

.compose-modal__chip button {
  width: 16px;
  height: 16px;
  display: grid;
  place-items: center;
  padding: 0;
  border: 0;
  border-radius: 50%;
  background: color-mix(in srgb, var(--text-tertiary) 14%, transparent);
  color: var(--text-tertiary);
  transition:
    background var(--motion-fast) var(--motion-ease-out),
    color var(--motion-fast) var(--motion-ease-out);
}

.compose-modal__chip button svg {
  width: 10px;
  height: 10px;
}

.compose-modal__chip button:hover {
  background: color-mix(in srgb, var(--status-danger) 18%, transparent);
  color: var(--status-danger);
}

.compose-modal__error {
  padding: 8px 12px;
  border-radius: var(--radius-md);
  background: color-mix(in srgb, var(--status-danger) 6%, var(--surface-card));
  border: 1px solid color-mix(in srgb, var(--status-danger) 14%, var(--border-default));
  color: var(--status-danger);
  font-size: var(--text-sm);
}

.compose-modal__user-list {
  display: grid;
  gap: 2px;
  max-height: 280px;
  overflow-y: auto;
  margin: 0 -24px;
  padding: 0 24px;
}

.compose-modal__status {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 28px 16px;
  color: var(--text-quaternary);
  font-size: var(--text-sm);
}

.compose-modal__status--hint {
  color: var(--text-tertiary);
}

.compose-modal__spinner {
  width: 16px;
  height: 16px;
  border: 2px solid var(--border-default);
  border-top-color: var(--interactive-primary-bg);
  border-radius: 50%;
  animation: compose-spin 0.6s linear infinite;
}

.compose-modal__spinner--light {
  border-color: color-mix(in srgb, var(--interactive-primary-fg) 30%, transparent);
  border-top-color: var(--interactive-primary-fg);
}

@keyframes compose-spin {
  to { transform: rotate(360deg); }
}

.compose-modal__user {
  display: grid;
  grid-template-columns: 42px minmax(0, 1fr) auto;
  gap: 12px;
  align-items: center;
  padding: 10px 12px;
  border: 0;
  border-radius: var(--radius-md);
  background: transparent;
  text-align: left;
  width: 100%;
  transition:
    background var(--motion-fast) var(--motion-ease-out);
}

.compose-modal__user:hover {
  background: color-mix(in srgb, var(--interactive-secondary-bg-hover) 60%, transparent);
}

.compose-modal__user.is-selected {
  background: color-mix(in srgb, var(--interactive-selected-bg) 72%, var(--surface-card));
}

.compose-modal__user-copy {
  min-width: 0;
  display: grid;
  gap: 3px;
}

.compose-modal__user-head {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}

.compose-modal__user-head strong {
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
  font-size: var(--text-sm);
  font-weight: 600;
  color: var(--text-primary);
  line-height: 1.2;
}

.compose-modal__user-tag {
  flex-shrink: 0;
  color: var(--text-quaternary);
  font: 500 var(--text-2xs)/1 var(--font-mono);
  letter-spacing: 0.02em;
}

.compose-modal__user-copy p {
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
  color: var(--text-tertiary);
  font-size: var(--text-xs);
  line-height: 1.3;
}

.compose-modal__check {
  width: 22px;
  height: 22px;
  display: grid;
  place-items: center;
  border-radius: 50%;
  background: var(--interactive-primary-bg);
  color: var(--interactive-primary-fg);
  flex-shrink: 0;
}

.compose-modal__check svg {
  width: 13px;
  height: 13px;
}

.compose-modal__footer {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 10px;
  padding: 16px 24px 20px;
  border-top: 1px solid color-mix(in srgb, var(--border-default) 70%, transparent);
}

.compose-modal__btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  min-height: 38px;
  padding: 0 20px;
  border: 0;
  border-radius: var(--radius-md);
  font: 500 var(--text-sm)/1 var(--font-sans);
  letter-spacing: -0.01em;
  cursor: pointer;
  transition:
    background var(--motion-fast) var(--motion-ease-out),
    color var(--motion-fast) var(--motion-ease-out),
    box-shadow var(--motion-fast) var(--motion-ease-out),
    transform var(--motion-fast) var(--motion-ease-out);
}

.compose-modal__btn--secondary {
  background: var(--interactive-secondary-bg);
  color: var(--text-secondary);
  border: 1px solid var(--border-default);
}

.compose-modal__btn--secondary:hover {
  background: var(--interactive-secondary-bg-hover);
  color: var(--text-primary);
  border-color: var(--border-strong);
}

.compose-modal__btn--primary {
  background: var(--interactive-primary-bg);
  color: var(--interactive-primary-fg);
  box-shadow:
    0 2px 8px color-mix(in srgb, var(--interactive-primary-bg) 24%, transparent),
    0 6px 16px color-mix(in srgb, var(--interactive-primary-bg) 12%, transparent);
}

.compose-modal__btn--primary:hover:not(:disabled) {
  background: var(--interactive-primary-bg-hover);
  box-shadow:
    0 4px 12px color-mix(in srgb, var(--interactive-primary-bg) 30%, transparent),
    0 8px 20px color-mix(in srgb, var(--interactive-primary-bg) 16%, transparent);
  transform: translateY(-1px);
}

.compose-modal__btn--primary:active:not(:disabled) {
  transform: translateY(0);
  box-shadow: 0 1px 4px color-mix(in srgb, var(--interactive-primary-bg) 20%, transparent);
}

.compose-modal__btn--primary:disabled {
  background: color-mix(in srgb, var(--interactive-primary-bg) 40%, var(--surface-panel));
  box-shadow: none;
  opacity: 0.5;
  cursor: not-allowed;
}

:deep(.forward-dialog .el-dialog) {
  border: 1px solid var(--border-default);
  border-radius: var(--radius-card);
  background: color-mix(in srgb, var(--surface-overlay) 96%, transparent);
  box-shadow: var(--shadow-overlay);
}

:deep(.add-contact-dialog .el-dialog) {
  border: 1px solid var(--border-default);
  border-radius: var(--radius-card);
  background: color-mix(in srgb, var(--surface-overlay) 96%, transparent);
  box-shadow: var(--shadow-overlay);
}

:deep(.add-contact-dialog .el-dialog__body) {
  padding-top: 12px;
}

:deep(.add-contact-dialog .el-dialog) {
  min-height: min(520px, 70vh);
}

:deep(.forward-dialog .el-dialog__header) {
  padding-bottom: 4px;
}

:deep(.forward-dialog .el-dialog__title) {
  font: 620 var(--text-lg)/1.08 var(--font-display);
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
  font: 600 var(--text-2xs)/1 var(--font-mono);
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
  font: 600 var(--text-sm)/1.2 var(--font-body);
}

.forward-sheet__empty p {
  color: var(--text-tertiary);
  font-size: var(--text-xs);
  line-height: 1.45;
}

.forward-sheet__error {
  border-style: solid;
  border-color: color-mix(in srgb, var(--status-danger) 16%, var(--border-default));
  background: color-mix(in srgb, var(--status-danger) 4%, var(--surface-panel));
}

/* ── Edit group dialog ── */

:deep(.edit-group-modal .el-dialog) {
  border-radius: var(--radius-panel);
  border: 1px solid color-mix(in srgb, var(--border-default) 92%, transparent);
  background: color-mix(in srgb, var(--surface-card) 96%, transparent);
  backdrop-filter: blur(16px) saturate(108%);
  box-shadow: var(--shadow-lg);
  overflow: hidden;
}

:deep(.edit-group-modal .el-dialog__header),
:deep(.edit-group-modal .el-dialog__footer) {
  display: none;
}

:deep(.edit-group-modal .el-dialog__body) {
  padding: 0;
}

.edit-group-modal__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 18px 20px 0;
}

.edit-group-modal__header strong {
  font: 620 var(--text-base)/1.08 var(--font-display);
  letter-spacing: -0.02em;
  color: var(--text-primary);
}

.edit-group-modal__close {
  width: 30px;
  height: 30px;
  display: grid;
  place-items: center;
  border: 0;
  border-radius: var(--radius-control);
  background: transparent;
  color: var(--text-tertiary);
  cursor: pointer;
  transition:
    background var(--motion-fast) var(--motion-ease-out),
    color var(--motion-fast) var(--motion-ease-out);
}

.edit-group-modal__close:hover {
  background: var(--interactive-secondary-bg-hover);
  color: var(--text-primary);
}

.edit-group-modal__body {
  padding: 16px 20px 4px;
}

.edit-group-modal__label {
  display: block;
  margin-bottom: 6px;
  color: var(--text-secondary);
  font: 500 var(--text-xs)/1 var(--font-body);
}

.edit-group-modal__input {
  width: 100%;
}

:deep(.edit-group-modal__input .el-input__wrapper),
:deep(.edit-group-modal__input .el-textarea__inner) {
  border-radius: var(--radius-control);
  background: color-mix(in srgb, var(--surface-panel) 72%, transparent);
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.06),
    0 0 0 1px color-mix(in srgb, var(--border-default) 80%, transparent);
  color: var(--text-primary);
  font-size: var(--text-sm);
  transition:
    box-shadow var(--motion-fast) var(--motion-ease-out);
}

:deep(.edit-group-modal__input .el-input__wrapper.is-focus),
:deep(.edit-group-modal__input .el-textarea__inner:focus) {
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.08),
    0 0 0 1px color-mix(in srgb, var(--interactive-primary-bg) 28%, transparent),
    0 0 0 4px color-mix(in srgb, var(--interactive-focus-ring) 24%, transparent);
}

.edit-group-modal__footer {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  padding: 14px 20px 18px;
}

.edit-group-modal__btn {
  min-height: 36px;
  padding: 0 16px;
  border: 0;
  border-radius: var(--radius-control);
  font: 500 var(--text-sm)/1 var(--font-body);
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  transition:
    background var(--motion-fast) var(--motion-ease-out),
    color var(--motion-fast) var(--motion-ease-out),
    opacity var(--motion-fast) var(--motion-ease-out);
}

.edit-group-modal__btn--secondary {
  background: var(--interactive-secondary-bg);
  color: var(--text-secondary);
  border: 1px solid var(--border-default);
}

.edit-group-modal__btn--secondary:hover {
  background: var(--interactive-secondary-bg-hover);
  color: var(--text-primary);
}

.edit-group-modal__btn--primary {
  background: color-mix(in srgb, var(--interactive-primary-bg) 88%, transparent);
  color: var(--text-on-brand);
}

.edit-group-modal__btn--primary:hover:not(:disabled) {
  background: var(--interactive-primary-bg);
}

.edit-group-modal__btn--primary:disabled {
  opacity: 0.45;
  cursor: not-allowed;
}

.edit-group-modal__spinner {
  width: 14px;
  height: 14px;
  border: 2px solid color-mix(in srgb, var(--text-on-brand) 30%, transparent);
  border-top-color: var(--text-on-brand);
  border-radius: 50%;
  animation: eg-spin 0.6s linear infinite;
}

@keyframes eg-spin {
  to { transform: rotate(360deg); }
}

@media (max-width: 1279px) {
  .chat-page {
    grid-template-columns: clamp(280px, 25vw, 332px) minmax(0, 1fr);
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

  .compose-dialog__user--result {
    grid-template-columns: 48px minmax(0, 1fr);
  }

  .compose-dialog__result-actions {
    grid-column: 2;
    justify-content: flex-start;
    min-width: 0;
  }

  :deep(.compose-modal .el-dialog) {
    width: calc(100vw - 32px) !important;
    margin: 16px;
  }

  .compose-modal__header {
    padding: 18px 18px 14px;
  }

  .compose-modal__body {
    padding: 0 18px;
    max-height: min(60vh, 400px);
  }

  .compose-modal__user-list {
    margin: 0 -18px;
    padding: 0 18px;
  }

  .compose-modal__footer {
    padding: 14px 18px 16px;
  }

  .compose-modal__user {
    grid-template-columns: 38px minmax(0, 1fr) auto;
    padding: 9px 10px;
  }

}
</style>
