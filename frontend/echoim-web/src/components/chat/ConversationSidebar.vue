<script setup lang="ts">
import { computed, nextTick, onMounted, onUnmounted, reactive, ref, useAttrs, watch } from 'vue'
import {
  ArrowLeft,
  Bell,
  ChatRound,
  CirclePlus,
  Collection,
  Delete,
  FolderOpened,
  Guide,
  Lock,
  MessageBox,
  Moon,
  MoreFilled,
  Open,
  Plus,
  Search,
  Setting,
  Sunny,
  User,
  UserFilled,
} from '@element-plus/icons-vue'
import type {
  ChangePasswordPayload,
  ChatPreferences,
  ChatFile,
  ConversationFolder,
  ConversationSummary,
  CurrentUserProfile,
  LeftPanelMode,
  SecurityEventSummary,
  SettingsSection,
  StoredAccount,
  ThemeMode,
  TrustedDeviceSummary,
  UpdateCurrentUserProfilePayload,
  UserInfo,
} from '@/types/chat'
import type { ApiBlockedUserItem } from '@/types/api'
import { readBrowserNotificationPermission } from '@/utils/browserNotifications'
import { STORAGE_KEYS } from '@/utils/storage'
import { uploadFile } from '@/services/files'
import { formatMessageTime } from '@/utils/format'
import { buildPublicProfileUrl } from '@/utils/publicProfiles'
import ConversationListItem from './ConversationListItem.vue'
import AvatarBadge from './AvatarBadge.vue'
import ChatStatePanel from './ChatStatePanel.vue'

type ConversationContextAction = 'open-tab' | 'mark-unread' | 'toggle-top' | 'toggle-mute' | 'archive' | 'delete'
type ComposeAction = 'single' | 'group' | 'channel'
type SecurityEventTone = 'success' | 'failure' | 'warning' | 'neutral'
type SecurityRiskTone = 'critical' | 'warning' | 'neutral'
type NormalizedSecurityEvent = {
  eventId: number
  timestamp: number
  dayKey: string
  dayLabel: string
  title: string
  statusLabel: string
  statusTone: SecurityEventTone
  riskLabel: string
  riskTone: SecurityRiskTone
  timeLabel: string
  detail: string
  meta: string[]
  alertLabel: string
  alertDescription: string
  highlight: boolean
}

defineOptions({
  inheritAttrs: false,
})

const props = defineProps<{
  currentUser: UserInfo | null
  currentProfile: CurrentUserProfile | null
  storedAccounts: StoredAccount[]
  conversations: ConversationSummary[]
  selectedConversationId: number | null
  conversationFolder: ConversationFolder
  searchQuery: string
  theme: ThemeMode
  leftPanelMode: LeftPanelMode
  settingsSection: SettingsSection
  globalMenuOpen: boolean
  chatPreferences: ChatPreferences
  loading?: boolean
  errorMessage?: string | null
  focusSearchToken?: number
  panelScrollTop?: number
  profileLoading?: boolean
  profileSaving?: boolean
  passwordSaving?: boolean
  emailBindingLoading?: boolean
  trustedDevicesLoading?: boolean
  securityEventsLoading?: boolean
  trustedDevices?: TrustedDeviceSummary[]
  securityEvents?: SecurityEventSummary[]
  blockedUsers?: ApiBlockedUserItem[]
  blockedUsersLoading?: boolean
  profileError?: string | null
  profileNotice?: string | null
  usernameChecking?: boolean
  usernameAvailable?: boolean | null
  usernameMessage?: string | null
  totpEnabled?: boolean
  totpRecoveryCodesRemaining?: number
  totpLoading?: boolean
  totpSetupData?: { secret: string; uri: string; recoveryCodes: string[] } | null
}>()

const emit = defineEmits<{
  'update:searchQuery': [value: string]
  select: [conversationId: number]
  toggleTheme: []
  clearSearch: []
  retry: []
  'update:panelScrollTop': [value: number]
  'update:settingsSection': [value: SettingsSection]
  'update:globalMenuOpen': [value: boolean]
  'update:chatPreferences': [value: Partial<ChatPreferences>]
  'open-panel': [mode: LeftPanelMode]
  'open-global-search': []
  'open-saved-messages': []
  'add-account': []
  'switch-account': [userId: number]
  'remove-account': [userId: number]
  'check-username': [username: string]
  'save-profile': [payload: UpdateCurrentUserProfilePayload]
  'change-password': [payload: ChangePasswordPayload]
  'send-email-bind-code': [payload: { email: string; currentPassword: string }]
  'bind-email': [payload: { email: string; code: string; currentPassword: string }]
  'refresh-trusted-devices': []
  'revoke-trusted-device': [payload: { deviceId: number; deviceFingerprint: string }]
  'revoke-all-trusted-devices': []
  'refresh-security-events': []
  'refresh-blocked-users': []
  'unblock-user': [userId: number]
  'clear-profile-error': []
  'clear-profile-notice': []
  'conversation-action': [payload: { command: ConversationContextAction; conversationId: number }]
  'update:conversation-folder': [value: ConversationFolder]
  'compose-action': [value: ComposeAction]
  'setup-totp': []
  'enable-totp': [payload: { code: string; secret: string; recoveryCodes: string[] }]
  'disable-totp': [code: string]
  'load-totp-status': []
  logout: []
}>()

const searchInput = ref()
const avatarUploadInput = ref<HTMLInputElement | null>(null)
const scrollbarRef = ref()
const attrs = useAttrs()
const draftSearchQuery = ref(props.searchQuery)
const notificationPromptVisible = ref(localStorage.getItem(STORAGE_KEYS.desktopNotificationPromptDismissed) !== 'true')
const notificationPermission = ref<NotificationPermission | 'unsupported'>(readBrowserNotificationPermission())
const notificationRequesting = ref(false)
const composeMenuOpen = ref(false)
const editingProfile = ref(false)
const avatarUploading = ref(false)
const avatarUploadError = ref('')
const profileActionNotice = ref('')
const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: '',
  localError: '',
})
const emailForm = reactive({
  email: '',
  currentPassword: '',
  code: '',
  localError: '',
})
const totpSetupData = computed(() => props.totpSetupData ?? null)
const totpEnableCode = ref('')
const totpDisableCode = ref('')
const totpQrCodeUrl = computed(() => {
  if (!totpSetupData.value?.uri) return ''
  return `https://api.qrserver.com/v1/create-qr-code/?size=200x200&data=${encodeURIComponent(totpSetupData.value.uri)}`
})
const profileDraft = reactive<{
  username: string
  nickname: string
  avatarUrl: string
  gender: number | null
  signature: string
}>({
  username: '',
  nickname: '',
  avatarUrl: '',
  gender: null,
  signature: '',
})
const usernameAvailability = reactive<{
  checking: boolean
  available: boolean | null
  message: string | null
}>({
  checking: false,
  available: null,
  message: null,
})
let searchTimer: number | null = null
let usernameTimer: number | null = null
const conversationContextMenu = reactive<{
  visible: boolean
  x: number
  y: number
  conversationId: number | null
}>({
  visible: false,
  x: 0,
  y: 0,
  conversationId: null,
})

const panelTitle = computed(() => {
  if (props.leftPanelMode === 'me') return '个人中心'
  if (props.leftPanelMode === 'settings') return '设置'
  return '会话'
})
const settingsSections = [
  {
    key: 'appearance',
    title: '外观',
  },
  {
    key: 'chat',
    title: '聊天偏好',
  },
  {
    key: 'notifications',
    title: '通知',
  },
  {
    key: 'security',
    title: '账号安全',
  },
  {
    key: 'privacy',
    title: '隐私',
  },
] as const satisfies ReadonlyArray<{
  key: SettingsSection
  title: string
}>
const trustedDeviceCount = computed(() => props.trustedDevices?.length ?? 0)
const securityEventCount = computed(() => props.securityEvents?.length ?? 0)
const securityOverview = computed(() => {
  if (!props.currentProfile?.email) {
    return {
      tone: 'warning',
      eyebrow: '需要完成',
      title: '账号保护尚未完整',
      description: '先绑定安全邮箱，再管理设备和找回能力。',
      primaryLabel: '未绑定邮箱',
    }
  }

  return {
    tone: 'stable',
    eyebrow: '已保护',
    title: '账号保护已启用',
    description: '邮箱、设备与安全记录会集中显示在这里。',
    primaryLabel: '邮箱已绑定',
  }
})
const normalizedSecurityEvents = computed<NormalizedSecurityEvent[]>(() =>
  [...(props.securityEvents ?? [])]
    .sort((left, right) => getSecurityEventTimestamp(right.createdAt) - getSecurityEventTimestamp(left.createdAt))
    .map((event) => ({
      eventId: event.eventId,
      timestamp: getSecurityEventTimestamp(event.createdAt),
      dayKey: getSecurityEventDayKey(event.createdAt),
      dayLabel: formatSecurityEventDayLabel(event.createdAt),
      title: formatSecurityEventTitle(event.eventType),
      statusLabel: formatSecurityEventStatusLabel(event.eventStatus),
      statusTone: getSecurityEventStatusTone(event.eventStatus),
      riskLabel: getSecurityEventRiskLabel(event),
      riskTone: getSecurityEventRiskTone(event),
      timeLabel: event.createdAt ? formatMessageTime(event.createdAt) : '未知时间',
      detail: formatSecurityEventDetail(event),
      meta: [event.ip, summarizeSecurityUserAgent(event.userAgent)].filter((value): value is string => Boolean(value)),
      alertLabel: getSecurityAlertLabel(event),
      alertDescription: getSecurityAlertDescription(event),
      highlight: isSecurityAlertEvent(event),
    })),
)
const securityRiskSummary = computed(() => {
  const cutoff = Date.now() - 7 * 24 * 60 * 60 * 1000
  const recentEvents = normalizedSecurityEvents.value.filter((event) => event.timestamp >= cutoff)
  const criticalCount = recentEvents.filter((event) => event.riskTone === 'critical').length
  const warningCount = recentEvents.filter((event) => event.riskTone === 'warning').length

  return {
    windowLabel: '近 7 天',
    criticalCount,
    warningCount,
    hasSignals: criticalCount > 0 || warningCount > 0,
    headline:
      criticalCount > 0 || warningCount > 0
        ? `高风险 ${criticalCount} 条 / 敏感变更 ${warningCount} 条`
        : '未发现高风险或敏感变更',
  }
})
const criticalSecurityAlerts = computed(() => normalizedSecurityEvents.value.filter((event) => event.highlight).slice(0, 3))
const groupedSecurityEvents = computed(() => {
  const groups = new Map<
    string,
    {
      key: string
      label: string
      items: NormalizedSecurityEvent[]
    }
  >()

  for (const event of normalizedSecurityEvents.value) {
    const key = event.dayKey
    if (!groups.has(key)) {
      groups.set(key, {
        key,
        label: event.dayLabel,
        items: [],
      })
    }

    groups.get(key)?.items.push(event)
  }

  return Array.from(groups.values())
})
const notificationPromptCopy = computed(() => {
  if (notificationRequesting.value) {
    return {
      title: '正在打开通知确认',
      description: '请在浏览器弹窗中选择是否开启桌面通知。',
    }
  }

  if (notificationPermission.value === 'denied') {
    return {
      title: '浏览器通知已关闭',
      description: '如需提醒，请在地址栏权限设置中重新允许通知。',
    }
  }

  return {
    title: '开启桌面通知',
    description: '切到其他窗口时，也能及时收到新消息提醒。',
  }
})
const notificationStatusLabel = computed(() => {
  if (notificationPermission.value === 'granted') return '已开启'
  if (notificationPermission.value === 'denied') return '已拒绝'
  if (notificationPermission.value === 'unsupported') return '当前浏览器不支持'
  return '未设置'
})
const contextConversation = computed(
  () => props.conversations.find((item) => item.conversationId === conversationContextMenu.conversationId) ?? null,
)
const contextMenuItems = computed(() => [
  { key: 'open-tab' as const, label: '新标签页打开', icon: Open, danger: false },
  { key: 'mark-unread' as const, label: '标为未读', icon: MessageBox, danger: false },
  { key: 'toggle-top' as const, label: contextConversation.value?.isTop ? '取消置顶' : '置顶会话', icon: null, danger: false },
  {
    key: 'toggle-mute' as const,
    label: contextConversation.value?.isMute ? '关闭免打扰' : '消息免打扰',
    icon: null,
    danger: false,
  },
  { key: 'archive' as const, label: contextConversation.value?.archived ? '恢复到收件箱' : '归档会话', icon: FolderOpened, danger: false },
  { key: 'delete' as const, label: '删除会话', icon: Delete, danger: true },
])

const canSubmitProfile = computed(
  () =>
    Boolean(profileDraft.nickname.trim()) &&
    Boolean(profileDraft.username.trim()) &&
    !props.usernameChecking &&
    props.usernameAvailable !== false &&
    !avatarUploading.value,
)

watch(
  () => props.focusSearchToken,
  async () => {
    if (props.leftPanelMode !== 'conversations') return
    await nextTick()
    searchInput.value?.focus?.()
  },
)

watch(
  () => props.panelScrollTop,
  async (value) => {
    if (typeof value !== 'number') return
    await nextTick()
    scrollbarRef.value?.setScrollTop?.(value)
  },
  { immediate: true },
)

watch(
  () => props.selectedConversationId,
  async (value) => {
    if (!value || props.leftPanelMode !== 'conversations') return
    await nextTick()
    document.getElementById(`conversation-item-${value}`)?.scrollIntoView({ block: 'nearest' })
  },
)

watch(
  () => props.searchQuery,
  (value) => {
    if (value !== draftSearchQuery.value) {
      draftSearchQuery.value = value
    }
  },
)

watch(
  () => props.currentProfile,
  (value) => {
    profileDraft.nickname = value?.nickname ?? props.currentUser?.nickname ?? ''
    profileDraft.username = value?.username ?? props.currentUser?.username ?? ''
    profileDraft.avatarUrl = value?.avatarUrl ?? props.currentUser?.avatarUrl ?? ''
    profileDraft.gender = value?.gender ?? null
    profileDraft.signature = value?.signature ?? ''
    emailForm.email = value?.email ?? ''
    resetUsernameState()
  },
  { immediate: true },
)

watch(
  () => props.leftPanelMode,
  () => {
    editingProfile.value = false
    passwordForm.oldPassword = ''
    passwordForm.newPassword = ''
    passwordForm.confirmPassword = ''
    passwordForm.localError = ''
    emailForm.currentPassword = ''
    emailForm.code = ''
    emailForm.localError = ''
  },
)

watch(
  () => [props.leftPanelMode, props.settingsSection] as const,
  ([mode, section]) => {
    if (mode === 'settings' && section === 'security') {
      emit('refresh-trusted-devices')
      emit('refresh-security-events')
      emit('load-totp-status')
    }
    if (mode === 'settings' && section === 'privacy') {
      emit('refresh-blocked-users')
    }
  },
  { immediate: true },
)

watch(
  () => profileDraft.username,
  (value) => {
    if (!editingProfile.value) return
    if (usernameTimer) {
      window.clearTimeout(usernameTimer)
    }
    usernameTimer = window.setTimeout(() => {
      emit('check-username', value)
    }, 220)
  },
)

onMounted(() => {
  document.addEventListener('pointerdown', handleDocumentPointerDown)
})

onUnmounted(() => {
  if (searchTimer) {
    window.clearTimeout(searchTimer)
  }
  if (usernameTimer) {
    window.clearTimeout(usernameTimer)
  }
  document.removeEventListener('pointerdown', handleDocumentPointerDown)
})

function handleScroll({ scrollTop }: { scrollTop: number }) {
  if (conversationContextMenu.visible) {
    closeConversationContextMenu()
  }
  emit('update:panelScrollTop', scrollTop)
}

function handleSearchInput(value: string) {
  draftSearchQuery.value = value

  if (searchTimer) {
    window.clearTimeout(searchTimer)
  }

  searchTimer = window.setTimeout(() => {
    emit('update:searchQuery', draftSearchQuery.value)
  }, 120)
}

function clearSearch() {
  draftSearchQuery.value = ''
  if (searchTimer) {
    window.clearTimeout(searchTimer)
    searchTimer = null
  }
  emit('clearSearch')
}

function handleDocumentPointerDown(event: PointerEvent) {
  if (!(event.target instanceof Element)) return

  if (props.globalMenuOpen && !event.target.closest('.sidebar-global-menu, .sidebar-panel__menu-trigger')) {
    emit('update:globalMenuOpen', false)
  }

  if (composeMenuOpen.value && !event.target.closest('.sidebar-compose-menu, .sidebar-panel__compose-anchor')) {
    composeMenuOpen.value = false
  }

  if (conversationContextMenu.visible && !event.target.closest('.sidebar-context-menu')) {
    closeConversationContextMenu()
  }
}

function toggleGlobalMenu() {
  composeMenuOpen.value = false
  emit('update:globalMenuOpen', !props.globalMenuOpen)
}

function closeGlobalMenu() {
  emit('update:globalMenuOpen', false)
}

function toggleComposeMenu() {
  closeGlobalMenu()
  composeMenuOpen.value = !composeMenuOpen.value
}

function closeComposeMenu() {
  composeMenuOpen.value = false
}

function openConversationContextMenu(event: MouseEvent, conversationId: number) {
  closeGlobalMenu()
  closeComposeMenu()

  conversationContextMenu.visible = true
  conversationContextMenu.conversationId = conversationId
  conversationContextMenu.x = Math.min(event.clientX, window.innerWidth - 248)
  conversationContextMenu.y = Math.min(event.clientY, window.innerHeight - 332)
}

function closeConversationContextMenu() {
  conversationContextMenu.visible = false
  conversationContextMenu.conversationId = null
}

function handleConversationContextAction(command: ConversationContextAction) {
  const conversationId = conversationContextMenu.conversationId
  if (!conversationId) return

  closeConversationContextMenu()
  emit('conversation-action', { command, conversationId })
}

function handleAddAccount() {
  closeGlobalMenu()
  closeComposeMenu()
  emit('add-account')
}

function handleSwitchAccount(userId: number) {
  closeGlobalMenu()
  closeComposeMenu()
  emit('switch-account', userId)
}

function handleRemoveAccount(userId: number) {
  emit('remove-account', userId)
}

function handleOpenGlobalSearch() {
  closeGlobalMenu()
  closeComposeMenu()
  emit('open-global-search')
}

function handleOpenSavedMessages() {
  closeGlobalMenu()
  closeComposeMenu()
  emit('open-saved-messages')
}

function handleComposeAction(action: ComposeAction) {
  closeGlobalMenu()
  closeComposeMenu()
  emit('compose-action', action)
}

function handleLogout() {
  closeGlobalMenu()
  closeComposeMenu()
  emit('logout')
}

function dismissNotificationPrompt() {
  notificationPromptVisible.value = false
  localStorage.setItem(STORAGE_KEYS.desktopNotificationPromptDismissed, 'true')
}

async function requestDesktopNotifications() {
  notificationPermission.value = readBrowserNotificationPermission()

  if (notificationRequesting.value || notificationPermission.value === 'unsupported') return
  if (notificationPermission.value === 'granted') {
    dismissNotificationPrompt()
    return
  }

  if (notificationPermission.value === 'denied') return

  notificationRequesting.value = true

  try {
    notificationPermission.value = await Notification.requestPermission()
    if (notificationPermission.value === 'granted') {
      dismissNotificationPrompt()
    }
  } finally {
    notificationRequesting.value = false
  }
}

function openPanel(mode: LeftPanelMode) {
  closeGlobalMenu()
  closeComposeMenu()
  emit('open-panel', mode)
}

function startProfileEditing() {
  editingProfile.value = true
  avatarUploadError.value = ''
  profileActionNotice.value = ''
  emit('clear-profile-error')
  emit('clear-profile-notice')
}

function cancelProfileEditing() {
  editingProfile.value = false
  avatarUploadError.value = ''
  profileActionNotice.value = ''
  profileDraft.username = props.currentProfile?.username ?? props.currentUser?.username ?? ''
  profileDraft.nickname = props.currentProfile?.nickname ?? props.currentUser?.nickname ?? ''
  profileDraft.avatarUrl = props.currentProfile?.avatarUrl ?? props.currentUser?.avatarUrl ?? ''
  profileDraft.gender = props.currentProfile?.gender ?? null
  profileDraft.signature = props.currentProfile?.signature ?? ''
  resetUsernameState()
}

function submitProfile() {
  if (!profileDraft.username.trim() || !profileDraft.nickname.trim()) {
    return
  }
  if (avatarUploading.value) {
    return
  }
  emit('save-profile', {
    username: profileDraft.username.trim(),
    nickname: profileDraft.nickname.trim(),
    avatarUrl: profileDraft.avatarUrl.trim() || null,
    gender: profileDraft.gender,
    signature: profileDraft.signature.trim() || null,
  })
  editingProfile.value = false
  profileActionNotice.value = ''
}

function openAvatarPicker() {
  if (!editingProfile.value || props.profileSaving || avatarUploading.value) return
  avatarUploadInput.value?.click()
}

async function handleAvatarChange(event: Event) {
  const input = event.target as HTMLInputElement | null
  const file = input?.files?.[0] ?? null
  if (input) {
    input.value = ''
  }
  if (!file) return

  avatarUploadError.value = ''
  emit('clear-profile-error')
  emit('clear-profile-notice')

  if (!file.type.startsWith('image/')) {
    avatarUploadError.value = '请选择图片文件'
    return
  }

  avatarUploading.value = true

  try {
    const uploaded = await uploadFile(file, 1) as ChatFile
    const nextUrl = uploaded.url ?? uploaded.downloadUrl ?? ''
    if (!nextUrl) {
      throw new Error('头像上传成功，但未返回可用地址')
    }
    profileDraft.avatarUrl = nextUrl
  } catch (error) {
    avatarUploadError.value = error instanceof Error ? error.message : '头像上传失败'
  } finally {
    avatarUploading.value = false
  }
}

function clearAvatarDraft() {
  if (!editingProfile.value || props.profileSaving || avatarUploading.value) return
  profileDraft.avatarUrl = ''
  avatarUploadError.value = ''
}

async function copyPublicProfileLink() {
  const username = props.currentProfile?.username ?? props.currentUser?.username ?? ''
  if (!username) return

  try {
    await navigator.clipboard.writeText(buildPublicProfileUrl(username))
    profileActionNotice.value = '公开链接已复制'
  } catch {
    profileActionNotice.value = '公开链接复制失败'
  }
}

function resetUsernameState() {
  usernameAvailability.checking = false
  usernameAvailability.available = null
  usernameAvailability.message = null
}

function submitPassword() {
  passwordForm.localError = ''
  emit('clear-profile-error')
  emit('clear-profile-notice')

  if (!passwordForm.oldPassword.trim() || !passwordForm.newPassword.trim()) {
    passwordForm.localError = '请输入完整的旧密码和新密码'
    return
  }

  if (passwordForm.newPassword.trim().length < 6) {
    passwordForm.localError = '新密码至少需要 6 位'
    return
  }

  if (passwordForm.newPassword !== passwordForm.confirmPassword) {
    passwordForm.localError = '两次输入的新密码不一致'
    return
  }

  emit('change-password', {
    oldPassword: passwordForm.oldPassword,
    newPassword: passwordForm.newPassword,
  })
  passwordForm.oldPassword = ''
  passwordForm.newPassword = ''
  passwordForm.confirmPassword = ''
}

function sendEmailBindCode() {
  emailForm.localError = ''
  emit('clear-profile-error')
  emit('clear-profile-notice')

  if (!emailForm.email.trim() || !emailForm.currentPassword.trim()) {
    emailForm.localError = '请输入邮箱和当前密码'
    return
  }

  emit('send-email-bind-code', {
    email: emailForm.email.trim(),
    currentPassword: emailForm.currentPassword,
  })
}

function submitEmailBind() {
  emailForm.localError = ''
  emit('clear-profile-error')
  emit('clear-profile-notice')

  if (!emailForm.email.trim() || !emailForm.currentPassword.trim() || !emailForm.code.trim()) {
    emailForm.localError = '请输入邮箱、验证码和当前密码'
    return
  }

  emit('bind-email', {
    email: emailForm.email.trim(),
    code: emailForm.code.trim(),
    currentPassword: emailForm.currentPassword,
  })
}

async function handleSetupTotp() {
  emit('clear-profile-error')
  emit('clear-profile-notice')
  emit('setup-totp')
}

function handleEnableTotp() {
  emit('clear-profile-error')
  emit('clear-profile-notice')

  if (!totpEnableCode.value.trim()) {
    return
  }

  if (!totpSetupData.value) {
    return
  }

  emit('enable-totp', {
    code: totpEnableCode.value.trim(),
    secret: totpSetupData.value.secret,
    recoveryCodes: totpSetupData.value.recoveryCodes,
  })
  totpEnableCode.value = ''
  totpSetupData.value = null
}

function handleDisableTotp() {
  emit('clear-profile-error')
  emit('clear-profile-notice')

  if (!totpDisableCode.value.trim()) {
    return
  }

  emit('disable-totp', totpDisableCode.value.trim())
  totpDisableCode.value = ''
}

function cancelTotpSetup() {
  totpSetupData.value = null
  totpEnableCode.value = ''
}

function formatSecurityDetail(detail: string | null | undefined) {
  if (!detail) return ''

  try {
    const parsed = JSON.parse(detail) as Record<string, unknown>
    return Object.entries(parsed)
      .map(([key, value]) => `${key}: ${String(value)}`)
      .join(' · ')
  } catch {
    return detail
  }
}

function getSecurityEventDayKey(value: string | null | undefined) {
  if (!value) return 'older'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return 'older'
  return `${date.getFullYear()}-${date.getMonth() + 1}-${date.getDate()}`
}

function formatSecurityEventDayLabel(value: string | null | undefined) {
  if (!value) return '较早'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return '较早'

  const today = new Date()
  const todayKey = `${today.getFullYear()}-${today.getMonth() + 1}-${today.getDate()}`
  const dayKey = `${date.getFullYear()}-${date.getMonth() + 1}-${date.getDate()}`
  if (todayKey === dayKey) return '今天'

  const yesterday = new Date(today)
  yesterday.setDate(today.getDate() - 1)
  const yesterdayKey = `${yesterday.getFullYear()}-${yesterday.getMonth() + 1}-${yesterday.getDate()}`
  if (yesterdayKey === dayKey) return '昨天'

  return `${date.getMonth() + 1}月${date.getDate()}日`
}

function formatSecurityEventTitle(eventType: string) {
  const value = eventType.trim()
  const lower = value.toLowerCase()
  if (lower.includes('login') || lower.includes('signin')) return '登录尝试'
  if (lower.includes('password')) return '密码操作'
  if (lower.includes('email')) return '邮箱操作'
  if (lower.includes('device') || lower.includes('trusted')) return '设备信任变更'
  if (lower.includes('verify') || lower.includes('otp') || lower.includes('code')) return '验证码校验'
  if (lower.includes('logout') || lower.includes('signout')) return '退出登录'
  if (lower.includes('session') || lower.includes('token')) return '会话状态变更'
  return value || '安全事件'
}

function formatSecurityEventStatusLabel(eventStatus: string) {
  const value = eventStatus.trim()
  const lower = value.toLowerCase()
  if (lower.includes('success') || lower.includes('passed') || lower.includes('approved')) return '已完成'
  if (lower.includes('fail') || lower.includes('reject') || lower.includes('denied') || lower.includes('blocked')) return '已拦截'
  if (lower.includes('pending') || lower.includes('review')) return '处理中'
  return value || '未知状态'
}

function getSecurityEventStatusTone(eventStatus: string): SecurityEventTone {
  const lower = eventStatus.trim().toLowerCase()
  if (lower.includes('success') || lower.includes('passed') || lower.includes('approved')) return 'success'
  if (lower.includes('fail') || lower.includes('reject') || lower.includes('denied') || lower.includes('blocked')) return 'failure'
  if (lower.includes('pending') || lower.includes('review')) return 'warning'
  return 'neutral'
}

function getSecurityEventRiskTone(event: SecurityEventSummary): SecurityRiskTone {
  const content = `${event.eventType} ${event.eventStatus} ${event.detail ?? ''}`.toLowerCase()
  if (
    content.includes('fail') ||
    content.includes('reject') ||
    content.includes('denied') ||
    content.includes('blocked') ||
    content.includes('risk') ||
    content.includes('suspicious')
  ) {
    return 'critical'
  }

  if (
    content.includes('password') ||
    content.includes('email') ||
    content.includes('device') ||
    content.includes('trusted') ||
    content.includes('session') ||
    content.includes('token') ||
    content.includes('remove') ||
    content.includes('revoke')
  ) {
    return 'warning'
  }

  return 'neutral'
}

function getSecurityEventRiskLabel(event: SecurityEventSummary) {
  const tone = getSecurityEventRiskTone(event)
  if (tone === 'critical') return '高风险'
  if (tone === 'warning') return '敏感变更'
  return '常规记录'
}

function getSecurityEventTimestamp(value: string | null | undefined) {
  if (!value) return 0
  const timestamp = new Date(value).getTime()
  return Number.isNaN(timestamp) ? 0 : timestamp
}

function isSecurityAlertEvent(event: SecurityEventSummary) {
  if (getSecurityEventRiskTone(event) === 'critical') return true

  const content = `${event.eventType} ${event.eventStatus} ${event.detail ?? ''}`.toLowerCase()
  return (
    content.includes('email') ||
    content.includes('device') ||
    content.includes('trusted') ||
    content.includes('password') ||
    content.includes('revoke') ||
    content.includes('remove')
  )
}

function getSecurityAlertLabel(event: SecurityEventSummary) {
  const content = `${event.eventType} ${event.eventStatus} ${event.detail ?? ''}`.toLowerCase()
  if ((content.includes('login') || content.includes('signin')) && getSecurityEventStatusTone(event.eventStatus) === 'failure') {
    return '登录异常'
  }
  if (content.includes('device') || content.includes('trusted') || content.includes('revoke') || content.includes('remove')) {
    return '设备变更'
  }
  if (content.includes('email')) return '邮箱变更'
  if (content.includes('password')) return '密码相关'
  return '异常提醒'
}

function getSecurityAlertDescription(event: SecurityEventSummary) {
  const content = `${event.eventType} ${event.eventStatus} ${event.detail ?? ''}`.toLowerCase()
  if ((content.includes('login') || content.includes('signin')) && getSecurityEventStatusTone(event.eventStatus) === 'failure') {
    return '检测到失败登录或访问受阻，建议优先核对设备来源与登录凭证。'
  }
  if (content.includes('device') || content.includes('trusted') || content.includes('revoke') || content.includes('remove')) {
    return '设备信任状态发生变化，建议确认是否为本人操作，并及时清理不再使用的设备。'
  }
  if (content.includes('email')) {
    return '账号邮箱发生改动，建议确认找回方式仍然可用，并检查是否需要同步更新验证信息。'
  }
  if (content.includes('password')) {
    return '检测到密码相关操作，建议确认最近登录活动是否可信，并保留当前恢复渠道。'
  }
  return '这条记录值得优先确认，以确保账号保护状态保持稳定。'
}

function summarizeSecurityUserAgent(userAgent: string | null | undefined) {
  if (!userAgent) return ''
  if (userAgent.includes('Mac OS')) return 'macOS 设备'
  if (userAgent.includes('Windows')) return 'Windows 设备'
  if (userAgent.includes('iPhone')) return 'iPhone'
  if (userAgent.includes('Android')) return 'Android 设备'
  return '浏览器访问'
}

function formatSecurityEventDetail(event: SecurityEventSummary) {
  const detail = formatSecurityDetail(event.detail)
  if (detail) return detail
  return event.userAgent || event.ip || '没有更多附加信息'
}

function updateSettingsSection(section: SettingsSection) {
  emit('update:settingsSection', section)
}

function updatePreference(key: keyof ChatPreferences, value: boolean) {
  emit('update:chatPreferences', { [key]: value })
}

function isMenuItemActive(mode: LeftPanelMode, section?: SettingsSection) {
  if (mode === 'settings') {
    return props.leftPanelMode === 'settings' && props.settingsSection === section
  }

  return props.leftPanelMode === mode
}
</script>

<template>
  <aside v-bind="attrs" class="sidebar-panel" :class="[`mode-${leftPanelMode}`]">
    <template v-if="leftPanelMode === 'conversations'">
      <header class="sidebar-panel__header sidebar-panel__header--conversations">
        <div class="sidebar-panel__identity-row">
          <button
            class="sidebar-panel__identity"
            type="button"
            @click="openPanel('me')"
          >
            <AvatarBadge
              :name="currentUser?.nickname"
              :avatar-url="currentUser?.avatarUrl"
              size="md"
            />
            <div class="sidebar-panel__identity-copy">
              <strong>{{ currentUser?.nickname ?? '未登录' }}</strong>
              <span>@{{ currentUser?.username ?? 'echo_demo_01' }}</span>
            </div>
          </button>
          <div class="sidebar-panel__toolbar-actions">
            <button
              class="sidebar-panel__menu-trigger"
              type="button"
              aria-label="打开设置"
              data-testid="sidebar-open-settings"
              @click="openPanel('settings')"
            >
              <Setting />
            </button>
            <button
              class="sidebar-panel__menu-trigger"
              type="button"
              aria-label="打开工作区菜单"
              data-testid="sidebar-open-menu"
              @click="toggleGlobalMenu"
            >
              <MoreFilled />
            </button>
          </div>
        </div>
        <div class="sidebar-panel__toolbar">
          <div class="sidebar-panel__search">
            <el-input
              ref="searchInput"
              :model-value="draftSearchQuery"
              :prefix-icon="Search"
              placeholder="搜索好友、群聊或消息"
              aria-label="搜索会话"
              clearable
              @update:model-value="handleSearchInput"
              @clear="clearSearch"
            />
          </div>
          <div class="sidebar-panel__compose-anchor">
            <button
              class="sidebar-panel__compose-trigger"
              type="button"
              aria-label="打开新建菜单"
              aria-haspopup="menu"
              :aria-expanded="composeMenuOpen"
              data-testid="sidebar-open-compose"
              @click="toggleComposeMenu"
            >
              <Plus />
            </button>

            <transition name="sidebar-menu-fade">
              <div
                v-if="composeMenuOpen"
                class="sidebar-compose-menu"
                role="menu"
                aria-label="新建菜单"
                data-testid="sidebar-compose-menu"
              >
                <button class="sidebar-compose-menu__item" type="button" role="menuitem" @click="handleComposeAction('channel')">
                  <span class="sidebar-compose-menu__icon"><Guide /></span>
                  <span>新建频道</span>
                </button>
                <button class="sidebar-compose-menu__item" type="button" role="menuitem" @click="handleComposeAction('group')">
                  <span class="sidebar-compose-menu__icon"><UserFilled /></span>
                  <span>新建群组</span>
                </button>
                <button class="sidebar-compose-menu__item" type="button" role="menuitem" @click="handleComposeAction('single')">
                  <span class="sidebar-compose-menu__icon"><ChatRound /></span>
                  <span>新建私聊</span>
                </button>
              </div>
            </transition>
          </div>
        </div>
        <div class="sidebar-panel__folders">
          <button
            class="sidebar-panel__folder"
            :class="{ 'is-active': conversationFolder === 'inbox' }"
            type="button"
            @click="emit('update:conversation-folder', 'inbox')"
          >
            全部
          </button>
          <button
            class="sidebar-panel__folder"
            :class="{ 'is-active': conversationFolder === 'unread' }"
            type="button"
            @click="emit('update:conversation-folder', 'unread')"
          >
            未读
          </button>
          <button
            class="sidebar-panel__folder"
            :class="{ 'is-active': conversationFolder === 'archived' }"
            type="button"
            @click="emit('update:conversation-folder', 'archived')"
          >
            已归档
          </button>
        </div>

        <transition name="sidebar-menu-fade">
          <div
            v-if="globalMenuOpen"
            class="sidebar-global-menu"
            role="menu"
            aria-label="工作区菜单"
            data-testid="sidebar-global-menu"
          >
            <button
              class="sidebar-global-menu__identity"
              type="button"
              role="menuitem"
              data-testid="sidebar-menu-profile-center"
              @click="openPanel('me')"
            >
              <AvatarBadge
                :name="currentUser?.nickname"
                :avatar-url="currentUser?.avatarUrl"
                size="lg"
              />
              <div class="sidebar-global-menu__copy">
                <strong>{{ currentUser?.nickname ?? '未登录' }}</strong>
                <p>@{{ currentUser?.username ?? 'echo_demo_01' }}</p>
              </div>
            </button>

            <div class="sidebar-global-menu__group">
              <button class="sidebar-global-menu__item" type="button" role="menuitem" @click="handleAddAccount">
                <span class="sidebar-global-menu__icon"><Plus /></span>
                <span class="sidebar-global-menu__label">添加账号</span>
              </button>
              <button
                v-for="account in storedAccounts"
                :key="account.userInfo.userId"
                class="sidebar-global-menu__item"
                type="button"
                role="menuitem"
                @click="handleSwitchAccount(account.userInfo.userId)"
              >
                <span class="sidebar-global-menu__icon"><UserFilled /></span>
                <span class="sidebar-global-menu__label">
                  {{ account.userInfo.nickname }}
                  <small v-if="currentUser?.userId === account.userInfo.userId">当前</small>
                </span>
                <span class="sidebar-global-menu__meta">@{{ account.userInfo.username }}</span>
                <span
                  v-if="storedAccounts.length > 1 && currentUser?.userId !== account.userInfo.userId"
                  class="sidebar-global-menu__remove"
                  role="button"
                  tabindex="0"
                  @click.stop="handleRemoveAccount(account.userInfo.userId)"
                >
                  移除
                </span>
              </button>
            </div>

            <div class="sidebar-global-menu__group">
              <button class="sidebar-global-menu__item" type="button" role="menuitem" @click="handleOpenSavedMessages">
                <span class="sidebar-global-menu__icon"><Collection /></span>
                <span class="sidebar-global-menu__label">收藏消息</span>
              </button>
              <button class="sidebar-global-menu__item" type="button" role="menuitem" @click="handleOpenGlobalSearch">
                <span class="sidebar-global-menu__icon"><CirclePlus /></span>
                <span class="sidebar-global-menu__label">全局搜索</span>
              </button>
              <button class="sidebar-global-menu__item" type="button" role="menuitem" @click="openPanel('contacts')">
                <span class="sidebar-global-menu__icon"><User /></span>
                <span class="sidebar-global-menu__label">联系人</span>
              </button>
            </div>

            <div class="sidebar-global-menu__group">
              <button
                class="sidebar-global-menu__item"
                :class="{ 'is-active': isMenuItemActive('settings', 'appearance') }"
                type="button"
                role="menuitem"
                data-testid="sidebar-open-settings"
                @click="openPanel('settings')"
              >
                <span class="sidebar-global-menu__icon"><Setting /></span>
                <span class="sidebar-global-menu__label">设置</span>
              </button>
              <button class="sidebar-global-menu__item" type="button" role="menuitem" @click="emit('update:settingsSection', 'notifications'); openPanel('settings')">
                <span class="sidebar-global-menu__icon"><Bell /></span>
                <span class="sidebar-global-menu__label">通知与权限</span>
              </button>
              <button class="sidebar-global-menu__item" type="button" role="menuitem" @click="handleLogout">
                <span class="sidebar-global-menu__icon"><Lock /></span>
                <span class="sidebar-global-menu__label">退出登录</span>
              </button>
            </div>
          </div>
        </transition>
      </header>

      <el-scrollbar ref="scrollbarRef" class="sidebar-panel__scroll sidebar-panel__scroll--list" @scroll="handleScroll">
        <div v-if="loading" class="sidebar-panel__skeletons">
          <el-skeleton v-for="item in 8" :key="item" animated :rows="2" />
        </div>
        <ChatStatePanel
          v-else-if="errorMessage"
          compact
          title="会话加载失败"
          :description="errorMessage"
          action-label="重新加载"
          role="alert"
          aria-live="assertive"
          @action="emit('retry')"
        />
        <div
          v-else-if="conversations.length"
          class="sidebar-panel__list"
          :class="{ 'is-compact': chatPreferences.compactList }"
          data-testid="conversation-list"
        >
          <ConversationListItem
            v-for="conversation in conversations"
            :key="conversation.conversationId"
            :item="conversation"
            :active="selectedConversationId === conversation.conversationId"
            :search-query="searchQuery"
            :compact="chatPreferences.compactList"
            @click="emit('select', conversation.conversationId)"
            @contextmenu.prevent="openConversationContextMenu($event, conversation.conversationId)"
          />
        </div>
        <ChatStatePanel
          v-else-if="searchQuery"
          compact
          title="没有找到匹配的会话"
          description="试试别的关键词，或者清空搜索恢复完整列表。"
          action-label="清除搜索"
          role="status"
          aria-live="polite"
          @action="clearSearch"
        />
        <ChatStatePanel
          v-else
          compact
          :title="conversationFolder === 'archived' ? '这里还没有归档会话' : '这里还没有会话'"
          :description="
            conversationFolder === 'archived'
              ? '归档后的会话会集中出现在这里。'
              : '新的单聊、群组或频道会在这里按最近消息时间排序展示。'
          "
          role="status"
          aria-live="polite"
        />
      </el-scrollbar>

      <transition name="sidebar-menu-fade">
        <div
          v-if="conversationContextMenu.visible && contextConversation"
          class="sidebar-context-menu"
          :style="{ left: `${conversationContextMenu.x}px`, top: `${conversationContextMenu.y}px` }"
          role="menu"
          aria-label="会话操作菜单"
        >
          <button
            v-for="item in contextMenuItems"
            :key="item.key"
            class="sidebar-context-menu__item"
            :class="{ 'is-danger': item.danger }"
            type="button"
            role="menuitem"
            @click="handleConversationContextAction(item.key)"
          >
            <span class="sidebar-context-menu__icon" aria-hidden="true">
              <component :is="item.icon" v-if="item.icon" />
              <svg v-else-if="item.key === 'toggle-top'" viewBox="0 0 16 16" class="sidebar-context-menu__glyph">
                <path
                  d="M4.45 2.45c0-.52.43-.95.95-.95h5.2c.52 0 .95.43.95.95 0 .28-.12.55-.34.73L9.9 4.28v3.14l1.34 1.22c.2.18.31.44.31.71 0 .53-.42.95-.95.95H8.72v3.6a.72.72 0 1 1-1.44 0v-3.6H5.4a.95.95 0 0 1-.64-1.66L6.1 7.42V4.28L4.79 3.18a.98.98 0 0 1-.34-.73Z"
                  fill="none"
                  stroke="currentColor"
                  stroke-linejoin="round"
                  stroke-width="1.35"
                />
              </svg>
              <svg v-else viewBox="0 0 16 16" class="sidebar-context-menu__glyph">
                <path
                  d="M2.2 6.15a.7.7 0 0 1 .7-.7H4.3l2.25-1.9c.46-.39 1.15-.06 1.15.54v7.82c0 .6-.69.93-1.15.54L4.3 10.55H2.9a.7.7 0 0 1-.7-.7v-3.7Z"
                  fill="currentColor"
                />
                <path
                  d="M10.15 6.05 13 8.9m0-2.85-2.85 2.85"
                  fill="none"
                  stroke="currentColor"
                  stroke-linecap="round"
                  stroke-width="1.35"
                />
              </svg>
            </span>
            <span class="sidebar-context-menu__label">{{ item.label }}</span>
          </button>
        </div>
      </transition>
    </template>

    <template v-else>
      <header class="sidebar-panel__header sidebar-panel__header--detail">
        <button
          class="sidebar-panel__back"
          type="button"
          aria-label="返回会话列表"
          data-testid="sidebar-back"
          @click="openPanel('conversations')"
        >
          <ArrowLeft />
        </button>
        <div class="sidebar-panel__panel-copy">
          <strong>{{ panelTitle }}</strong>
        </div>
        <span class="sidebar-panel__panel-spacer" aria-hidden="true"></span>
      </header>

      <el-scrollbar ref="scrollbarRef" class="sidebar-panel__scroll" @scroll="handleScroll">
        <div v-if="leftPanelMode === 'me'" class="sidebar-detail" data-testid="sidebar-panel-me">
          <section class="sidebar-profile-card">
            <div class="sidebar-profile-card__main">
              <AvatarBadge
                :name="currentProfile?.nickname ?? currentUser?.nickname"
                :avatar-url="currentProfile?.avatarUrl ?? currentUser?.avatarUrl"
                size="lg"
              />
              <div class="sidebar-profile-card__copy">
                <p class="sidebar-profile-card__eyebrow">个人资料</p>
                <strong>{{ currentProfile?.nickname ?? currentUser?.nickname ?? '未登录' }}</strong>
                <p>@{{ currentProfile?.username ?? currentUser?.username ?? 'echo_demo_01' }}</p>
                <span>{{ currentProfile?.signature || '写一段签名，让联系人快速识别你。' }}</span>
              </div>
            </div>
            <div class="sidebar-profile-card__meta">
              <article>
                <span>账号编号</span>
                <strong>{{ currentProfile?.userNo ?? '加载中' }}</strong>
              </article>
              <article>
                <span>邮箱状态</span>
                <strong>{{ currentProfile?.email ? '已绑定' : '未绑定' }}</strong>
              </article>
            </div>
            <div v-if="profileActionNotice" class="sidebar-notice sidebar-notice--success">{{ profileActionNotice }}</div>
            <div class="sidebar-profile-card__actions">
              <el-button type="primary" class="sidebar-profile-card__action" data-testid="profile-edit" @click="startProfileEditing">
                编辑资料
              </el-button>
              <div class="sidebar-profile-card__secondary-actions">
                <el-button class="sidebar-profile-card__action sidebar-profile-card__action--secondary" @click="copyPublicProfileLink">
                  复制公开链接
                </el-button>
                <el-button class="sidebar-profile-card__action sidebar-profile-card__action--secondary" @click="openPanel('settings')">
                  进入设置
                </el-button>
              </div>
            </div>
          </section>

          <div v-if="profileNotice" class="sidebar-notice sidebar-notice--success">{{ profileNotice }}</div>
          <div v-if="profileError" class="sidebar-notice sidebar-notice--error">{{ profileError }}</div>

          <ChatStatePanel
            v-if="profileLoading && !currentProfile"
            compact
            title="正在获取个人资料"
            description="已登录信息可用，完整资料正在同步。"
          />
        </div>

        <div v-else class="sidebar-detail sidebar-detail--settings" data-testid="sidebar-panel-settings">
          <section class="settings-header">
            <div class="settings-header__title">
              <strong>设置</strong>
            </div>
            <div class="settings-header__account">
              <AvatarBadge
                :name="currentProfile?.nickname ?? currentUser?.nickname"
                :avatar-url="currentProfile?.avatarUrl ?? currentUser?.avatarUrl"
                size="md"
              />
              <div class="settings-header__account-copy">
                <small>当前账号</small>
                <strong>{{ currentProfile?.nickname ?? currentUser?.nickname ?? '未登录' }}</strong>
                <p>@{{ currentProfile?.username ?? currentUser?.username ?? 'echo_demo_01' }}</p>
              </div>
            </div>
          </section>

          <section class="settings-switcher" aria-label="设置分组">
            <div class="settings-tabs">
              <button
                v-for="section in settingsSections"
                :key="section.key"
                class="settings-tabs__item"
                :class="{ 'is-active': settingsSection === section.key }"
                type="button"
                :data-testid="`settings-tab-${section.key}`"
                @click="updateSettingsSection(section.key)"
              >
                <strong>{{ section.title }}</strong>
              </button>
            </div>
          </section>

          <div v-if="profileNotice" class="sidebar-notice sidebar-notice--success">{{ profileNotice }}</div>
          <div v-if="profileError || passwordForm.localError || emailForm.localError" class="sidebar-notice sidebar-notice--error">
            {{ passwordForm.localError || emailForm.localError || profileError }}
          </div>

          <div v-if="settingsSection === 'appearance'" class="settings-panel">
            <div class="settings-panel__head">
              <strong>外观</strong>
            </div>
            <div class="theme-card theme-card--stacked">
              <button
                class="theme-card__option"
                :class="{ 'is-active': theme === 'light' }"
                type="button"
                @click="theme !== 'light' && emit('toggleTheme')"
              >
                <span class="theme-card__icon">
                  <Sunny />
                </span>
                <div class="theme-card__copy">
                  <strong>浅色模式</strong>
                  <p>明亮、清爽，适合白天和高亮环境。</p>
                </div>
              </button>
              <button
                class="theme-card__option"
                :class="{ 'is-active': theme === 'dark' }"
                type="button"
                @click="theme !== 'dark' && emit('toggleTheme')"
              >
                <span class="theme-card__icon">
                  <Moon />
                </span>
                <div class="theme-card__copy">
                  <strong>深色模式</strong>
                  <p>沉稳、聚焦，适合长时间使用。</p>
                </div>
              </button>
            </div>
          </div>

          <div v-else-if="settingsSection === 'chat'" class="settings-panel">
            <div class="settings-panel__head">
              <strong>聊天偏好</strong>
            </div>
            <div class="settings-list settings-list--grouped">
              <label class="settings-item">
                <div>
                  <strong>Enter 直接发送</strong>
                  <p>关闭后仅 Shift + Enter 换行，Enter 不会立即发送。</p>
                </div>
                <el-switch
                  :model-value="chatPreferences.enterToSend"
                  @change="updatePreference('enterToSend', Boolean($event))"
                />
              </label>
              <label class="settings-item">
                <div>
                  <strong>紧凑会话列表</strong>
                  <p>压缩列表项纵向间距，提升一屏显示数量。</p>
                </div>
                <el-switch
                  :model-value="chatPreferences.compactList"
                  @change="updatePreference('compactList', Boolean($event))"
                />
              </label>
              <label class="settings-item">
                <div>
                  <strong>紧凑消息气泡</strong>
                  <p>减小消息留白，让长对话显示更多内容。</p>
                </div>
                <el-switch
                  :model-value="chatPreferences.compactBubbles"
                  @change="updatePreference('compactBubbles', Boolean($event))"
                />
              </label>
            </div>
          </div>

          <div v-else-if="settingsSection === 'notifications'" class="sidebar-stack">
            <div class="settings-panel">
              <div class="settings-panel__head">
                <strong>通知</strong>
              </div>
              <article class="settings-item settings-item--stacked settings-item--status">
                <div class="settings-item__status-copy">
                  <strong>桌面通知</strong>
                  <p>{{ notificationPromptCopy.description }}</p>
                </div>
                <span class="settings-tag settings-tag--status">{{ notificationStatusLabel }}</span>
              </article>
              <div class="settings-actions settings-actions--stacked">
                <el-button
                  v-if="notificationPermission !== 'granted' && notificationPermission !== 'unsupported'"
                  type="primary"
                  class="settings-actions__primary"
                  :loading="notificationRequesting"
                  @click="requestDesktopNotifications"
                >
                  {{ notificationPromptCopy.title }}
                </el-button>
                <el-button v-if="notificationPermission !== 'unsupported'" plain class="settings-actions__secondary" @click="dismissNotificationPrompt">
                  忽略提醒
                </el-button>
              </div>
            </div>
          </div>

          <div v-else-if="settingsSection === 'security'" class="sidebar-stack sidebar-stack--security">
            <div v-if="!currentProfile?.email" class="sidebar-notice sidebar-notice--warning">
              未绑定邮箱的账号无法使用找回密码和设备信任。
            </div>

            <div class="security-overview" :class="`is-${securityOverview.tone}`">
              <div class="security-overview__header">
                <div class="security-overview__copy">
                  <span>{{ securityOverview.eyebrow }}</span>
                  <strong>{{ securityOverview.title }}</strong>
                </div>
                <div class="security-overview__status">
                  <span class="settings-tag" :class="{ 'is-failure': !currentProfile?.email, 'is-success': !!currentProfile?.email }">
                    {{ securityOverview.primaryLabel }}
                  </span>
                </div>
              </div>
              <div class="security-metrics">
                <article>
                  <span>当前邮箱</span>
                  <strong>{{ currentProfile?.email || '未绑定' }}</strong>
                </article>
                <article>
                  <span>高风险</span>
                  <strong>{{ securityEventsLoading ? '同步中' : `${securityRiskSummary.criticalCount} 条` }}</strong>
                </article>
                <article>
                  <span>敏感变更</span>
                  <strong>{{ securityEventsLoading ? '同步中' : `${securityRiskSummary.warningCount} 条` }}</strong>
                </article>
                <article>
                  <span>受信设备</span>
                  <strong>{{ trustedDevicesLoading ? '同步中' : `${trustedDeviceCount} 台` }}</strong>
                </article>
              </div>
            </div>

            <section v-if="criticalSecurityAlerts.length" class="security-alerts">
              <header class="security-alerts__head">
                <span>异常提醒</span>
                <strong>优先确认这些关键安全事件</strong>
              </header>
              <div class="security-alerts__list">
                <article
                  v-for="event in criticalSecurityAlerts.slice(0, 2)"
                  :key="event.eventId"
                  class="security-alert security-alert--compact"
                  :class="[`is-${event.riskTone}`, `status-${event.statusTone}`]"
                >
                  <div class="security-alert__topline">
                    <div class="security-alert__headline">
                      <span class="security-alert__eyebrow">{{ event.alertLabel }}</span>
                      <strong>{{ event.title }}</strong>
                    </div>
                    <time>{{ event.timeLabel }}</time>
                  </div>
                  <p>{{ event.alertDescription }}</p>
                  <div class="security-alert__badges">
                    <span class="security-event__badge" :class="`is-${event.riskTone}`">{{ event.riskLabel }}</span>
                    <span class="security-event__badge security-event__badge--status" :class="`is-${event.statusTone}`">{{ event.statusLabel }}</span>
                  </div>
                </article>
              </div>
              <div v-if="criticalSecurityAlerts.length > 2" class="security-alerts__footer">
                其余 {{ criticalSecurityAlerts.length - 2 }} 条异常已收进下方安全记录。
              </div>
            </section>

            <div class="settings-panel security-operations">
              <div class="settings-panel__head">
                <strong>安全操作</strong>
              </div>

              <details class="security-disclosure" :open="!currentProfile?.email">
                <summary class="security-disclosure__summary">
                  <div class="security-disclosure__lead">
                    <span class="security-disclosure__icon"><Lock /></span>
                    <div class="security-disclosure__copy">
                      <strong>邮箱安全</strong>
                      <p>{{ currentProfile?.email ? currentProfile.email : '绑定安全邮箱' }}</p>
                    </div>
                  </div>
                  <span class="security-disclosure__meta">{{ currentProfile?.email ? '已绑定' : '未绑定' }}</span>
                </summary>
                <div class="security-disclosure__body">
                  <el-form label-position="top" class="profile-form">
                    <el-form-item label="邮箱">
                      <el-input v-model="emailForm.email" placeholder="name@example.com" />
                    </el-form-item>
                    <el-form-item label="当前密码">
                      <el-input v-model="emailForm.currentPassword" show-password />
                    </el-form-item>
                    <el-form-item label="验证码">
                      <div class="security-inline">
                        <el-input v-model="emailForm.code" placeholder="6 位验证码" />
                        <el-button plain :loading="emailBindingLoading" @click="sendEmailBindCode">发送验证码</el-button>
                      </div>
                    </el-form-item>
                  </el-form>
                  <div class="sidebar-actions">
                    <el-button type="primary" :loading="emailBindingLoading" @click="submitEmailBind">保存邮箱</el-button>
                  </div>
                </div>
              </details>

              <details class="security-disclosure">
                <summary class="security-disclosure__summary">
                  <div class="security-disclosure__lead">
                    <span class="security-disclosure__icon"><Guide /></span>
                    <div class="security-disclosure__copy">
                      <strong>受信设备</strong>
                      <p>{{ trustedDevicesLoading ? '设备同步中' : trustedDeviceCount ? `${trustedDeviceCount} 台设备保留登录状态` : '暂无受信设备' }}</p>
                    </div>
                  </div>
                  <span class="security-disclosure__meta">{{ trustedDevicesLoading ? '同步中' : `${trustedDeviceCount} 台` }}</span>
                </summary>
                <div class="security-disclosure__body">
                  <div v-if="trustedDevicesLoading" class="settings-empty">正在加载设备…</div>
                  <div v-else-if="trustedDevices?.length" class="settings-list settings-list--grouped">
                    <article v-for="device in trustedDevices" :key="device.deviceId" class="settings-item settings-item--stacked">
                      <div>
                        <strong>{{ device.deviceName }}</strong>
                        <p>{{ device.lastUsedAt || '未使用' }}</p>
                        <p>{{ device.expireAt || '无过期时间' }}</p>
                      </div>
                      <el-button text type="danger" @click="emit('revoke-trusted-device', { deviceId: device.deviceId, deviceFingerprint: device.deviceFingerprint })">
                        移除
                      </el-button>
                    </article>
                  </div>
                  <div v-else class="settings-empty">暂无受信设备</div>
                  <div class="security-actions" :class="{ 'security-actions--split': !!trustedDevices?.length, 'security-actions--single': !trustedDevices?.length }">
                    <el-button plain class="security-actions__secondary" @click="emit('refresh-trusted-devices')">刷新</el-button>
                    <el-button
                      v-if="trustedDevices?.length"
                      plain
                      type="danger"
                      class="security-actions__danger"
                      @click="emit('revoke-all-trusted-devices')"
                    >
                      全部移除
                    </el-button>
                  </div>
                </div>
              </details>

              <details class="security-disclosure">
                <summary class="security-disclosure__summary">
                  <div class="security-disclosure__lead">
                    <span class="security-disclosure__icon"><Lock /></span>
                    <div class="security-disclosure__copy">
                      <strong>密码与退出</strong>
                      <p>修改登录密码，或退出当前账号</p>
                    </div>
                  </div>
                  <span class="security-disclosure__meta">账户操作</span>
                </summary>
                <div class="security-disclosure__body">
                  <el-form label-position="top" class="profile-form">
                    <el-form-item label="旧密码">
                      <el-input v-model="passwordForm.oldPassword" show-password />
                    </el-form-item>
                    <el-form-item label="新密码">
                      <el-input v-model="passwordForm.newPassword" show-password />
                    </el-form-item>
                    <el-form-item label="确认新密码">
                      <el-input v-model="passwordForm.confirmPassword" show-password @keyup.enter="submitPassword" />
                    </el-form-item>
                  </el-form>
                  <div class="security-actions security-actions--split">
                    <el-button type="primary" class="security-actions__primary" :loading="passwordSaving" @click="submitPassword">更新密码</el-button>
                    <el-button plain class="security-actions__secondary" data-testid="sidebar-logout" @click="handleLogout">退出登录</el-button>
                  </div>
                </div>
              </details>

              <details class="security-disclosure">
                <summary class="security-disclosure__summary">
                  <div class="security-disclosure__lead">
                    <span class="security-disclosure__icon"><Lock /></span>
                    <div class="security-disclosure__copy">
                      <strong>两步验证</strong>
                      <p>{{ totpEnabled ? '已启用 TOTP 验证' : '使用验证器应用增强账号安全' }}</p>
                    </div>
                  </div>
                  <span class="security-disclosure__meta">{{ totpEnabled ? '已启用' : '未启用' }}</span>
                </summary>
                <div class="security-disclosure__body">
                  <div v-if="totpLoading" class="settings-empty">正在加载…</div>
                  <template v-else>
                    <div v-if="totpEnabled" class="totp-status">
                      <div class="totp-status__info">
                        <p>两步验证已启用，登录时需要输入验证器应用中的验证码。</p>
                        <p>剩余恢复码：{{ totpRecoveryCodesRemaining }} 个</p>
                      </div>
                      <el-form label-position="top" class="profile-form">
                        <el-form-item label="验证码">
                          <el-input v-model="totpDisableCode" placeholder="6 位验证码" maxlength="6" />
                        </el-form-item>
                      </el-form>
                      <div class="sidebar-actions">
                        <el-button type="danger" :loading="totpLoading" @click="handleDisableTotp">关闭两步验证</el-button>
                      </div>
                    </div>
                    <div v-else class="totp-setup">
                      <div v-if="!totpSetupData" class="totp-setup__start">
                        <p>启用两步验证后，登录时需要输入验证器应用（如 Google Authenticator）中的验证码。</p>
                        <div class="sidebar-actions">
                          <el-button type="primary" :loading="totpLoading" @click="handleSetupTotp">开始设置</el-button>
                        </div>
                      </div>
                      <div v-else class="totp-setup__config">
                        <p class="totp-setup__instruction">使用验证器应用扫描下方二维码，或手动输入密钥：</p>
                        <div class="totp-setup__secret">
                          <code>{{ totpSetupData.secret }}</code>
                        </div>
                        <div class="totp-setup__qr">
                          <img :src="totpQrCodeUrl" alt="TOTP 二维码" />
                        </div>
                        <div v-if="totpSetupData.recoveryCodes.length" class="totp-setup__recovery">
                          <strong>恢复码（请妥善保管）</strong>
                          <div class="totp-recovery-codes">
                            <code v-for="code in totpSetupData.recoveryCodes" :key="code">{{ code }}</code>
                          </div>
                        </div>
                        <el-form label-position="top" class="profile-form">
                          <el-form-item label="验证码">
                            <el-input v-model="totpEnableCode" placeholder="输入验证器应用中的 6 位验证码" maxlength="6" />
                          </el-form-item>
                        </el-form>
                        <div class="sidebar-actions">
                          <el-button type="primary" :loading="totpLoading" @click="handleEnableTotp">启用两步验证</el-button>
                          <el-button plain :loading="totpLoading" @click="cancelTotpSetup">取消</el-button>
                        </div>
                      </div>
                    </div>
                  </template>
                </div>
              </details>
            </div>

            <details class="settings-panel security-history">
              <summary class="security-history__summary">
                <div class="settings-panel__head">
                  <strong>安全记录</strong>
                  <span class="security-history__meta">{{ securityEventsLoading ? '同步中' : `${securityEventCount} 条` }}</span>
                </div>
              </summary>
              <div class="security-history__body">
                <div v-if="securityEventsLoading" class="settings-empty">正在加载记录…</div>
                <div v-else-if="groupedSecurityEvents.length" class="security-timeline">
                  <section v-for="group in groupedSecurityEvents" :key="group.key" class="security-timeline__group">
                    <header class="security-timeline__group-head">
                      <span>{{ group.label }}</span>
                    </header>
                    <div class="security-timeline__list">
                      <article v-for="event in group.items" :key="event.eventId" class="security-event" :class="[`is-${event.riskTone}`, `status-${event.statusTone}`]">
                        <div class="security-event__rail" aria-hidden="true">
                          <span class="security-event__dot"></span>
                        </div>
                        <div class="security-event__body">
                          <div class="security-event__head">
                            <strong>{{ event.title }}</strong>
                            <time>{{ event.timeLabel }}</time>
                          </div>
                          <div class="security-event__badges">
                            <span class="security-event__badge" :class="`is-${event.riskTone}`">{{ event.riskLabel }}</span>
                            <span class="security-event__badge security-event__badge--status" :class="`is-${event.statusTone}`">{{ event.statusLabel }}</span>
                          </div>
                          <p>{{ event.detail }}</p>
                          <div v-if="event.meta.length" class="security-event__meta">
                            <span v-for="item in event.meta" :key="item">{{ item }}</span>
                          </div>
                        </div>
                      </article>
                    </div>
                  </section>
                </div>
                <div v-else class="settings-empty">暂无记录</div>
              </div>
            </details>
          </div>

          <div v-else-if="settingsSection === 'privacy'" class="sidebar-stack">
            <div class="settings-panel">
              <div class="settings-panel__head">
                <strong>已屏蔽用户</strong>
                <el-button
                  plain
                  size="small"
                  :loading="blockedUsersLoading"
                  @click="emit('refresh-blocked-users')"
                >
                  刷新
                </el-button>
              </div>
              <div v-if="blockedUsersLoading" class="settings-empty">正在加载屏蔽列表…</div>
              <div v-else-if="!blockedUsers?.length" class="settings-empty">暂无屏蔽用户</div>
              <div v-else class="blocked-users-list">
                <div
                  v-for="item in blockedUsers"
                  :key="item.blockId"
                  class="blocked-user-item"
                >
                  <AvatarBadge
                    :name="item.nickname"
                    :avatar-url="item.avatarUrl"
                    size="sm"
                  />
                  <div class="blocked-user-item__copy">
                    <strong>{{ item.nickname }}</strong>
                    <span>@{{ item.userNo }}</span>
                  </div>
                  <el-button
                    type="danger"
                    plain
                    size="small"
                    :loading="blockedUsersLoading"
                    @click="emit('unblock-user', item.userId)"
                  >
                    取消屏蔽
                  </el-button>
                </div>
              </div>
            </div>
          </div>
        </div>
      </el-scrollbar>
    </template>
  </aside>

  <el-dialog v-model="editingProfile" title="编辑资料" width="520px" destroy-on-close @closed="cancelProfileEditing">
    <el-form label-position="top" class="profile-form">
      <el-form-item label="公开用户名">
        <el-input
          v-model="profileDraft.username"
          :disabled="profileSaving"
          maxlength="24"
          placeholder="3-24 位字母、数字或下划线"
        />
        <div v-if="usernameChecking || usernameMessage" class="profile-form__hint" :class="{ 'is-error': usernameAvailable === false }">
          {{ usernameChecking ? '正在检查用户名可用性…' : usernameMessage }}
        </div>
      </el-form-item>
      <el-form-item label="昵称">
        <el-input v-model="profileDraft.nickname" :disabled="profileSaving" maxlength="24" />
      </el-form-item>
      <el-form-item label="头像">
        <div class="profile-avatar-field" :class="{ 'is-disabled': profileSaving }">
          <AvatarBadge
            :name="profileDraft.nickname || profileDraft.username || currentUser?.nickname"
            :avatar-url="profileDraft.avatarUrl || currentProfile?.avatarUrl || currentUser?.avatarUrl"
            size="lg"
          />
          <div class="profile-avatar-field__copy">
            <strong>{{ avatarUploading ? '正在上传头像' : '上传头像' }}</strong>
            <p>支持 JPG、PNG、WEBP、GIF</p>
          </div>
          <div class="profile-avatar-field__actions">
            <el-button :disabled="profileSaving || avatarUploading" @click="openAvatarPicker">
              {{ profileDraft.avatarUrl ? '更换头像' : '选择图片' }}
            </el-button>
            <el-button
              v-if="profileDraft.avatarUrl"
              :disabled="profileSaving || avatarUploading"
              @click="clearAvatarDraft"
            >
              移除
            </el-button>
          </div>
          <input
            ref="avatarUploadInput"
            class="profile-avatar-field__input"
            type="file"
            accept="image/png,image/jpeg,image/webp,image/gif"
            @change="handleAvatarChange"
          />
        </div>
        <div v-if="avatarUploadError" class="profile-form__hint is-error">
          {{ avatarUploadError }}
        </div>
      </el-form-item>
      <el-form-item label="性别">
        <el-radio-group
          :model-value="profileDraft.gender ?? 0"
          :disabled="profileSaving"
          @update:model-value="profileDraft.gender = Number($event)"
        >
          <el-radio-button :value="1">男</el-radio-button>
          <el-radio-button :value="2">女</el-radio-button>
          <el-radio-button :value="0">保密</el-radio-button>
        </el-radio-group>
      </el-form-item>
      <el-form-item label="签名">
        <el-input
          v-model="profileDraft.signature"
          type="textarea"
          :disabled="profileSaving"
          :autosize="{ minRows: 3, maxRows: 6 }"
          maxlength="120"
          show-word-limit
        />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button :disabled="profileSaving || avatarUploading" @click="cancelProfileEditing">取消</el-button>
      <el-button
        type="primary"
        data-testid="profile-save"
        :loading="profileSaving"
        :disabled="!canSubmitProfile"
        @click="submitProfile"
      >
        保存资料
      </el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
.sidebar-panel {
  position: relative;
  min-height: 0;
  height: 100%;
  display: flex;
  flex-direction: column;
  background:
    linear-gradient(180deg, color-mix(in srgb, var(--interactive-focus-ring) 32%, transparent), transparent 12%),
    transparent;
  color: var(--text-primary);
  overflow: hidden;
}

.sidebar-panel__header {
  position: relative;
  z-index: 3;
  padding: 16px 14px 10px;
  background: transparent;
}

.sidebar-panel__identity-row {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 10px;
  align-items: center;
  margin-bottom: 10px;
}

.sidebar-panel__identity {
  display: grid;
  grid-template-columns: 48px minmax(0, 1fr);
  gap: 13px;
  align-items: center;
  min-width: 0;
  padding: 0;
  border: 0;
  background: transparent;
  text-align: left;
}

.sidebar-panel__identity-copy {
  min-width: 0;
}

.sidebar-panel__identity-copy strong,
.sidebar-panel__identity-copy span {
  display: block;
  min-width: 0;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.sidebar-panel__identity-copy strong {
  color: var(--text-primary);
  font: 600 var(--text-base)/1.1 var(--font-body);
  letter-spacing: -0.012em;
}

.sidebar-panel__identity-copy span {
  margin-top: 4px;
  color: var(--text-quaternary);
  font: 500 var(--text-xs)/1 var(--font-mono);
  letter-spacing: 0.03em;
}

.sidebar-panel__toolbar-actions {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.sidebar-panel__header--detail {
  display: grid;
  grid-template-columns: 40px minmax(0, 1fr) 40px;
  align-items: center;
  gap: 12px;
  min-height: 70px;
}

.sidebar-panel__toolbar {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 42px;
  align-items: center;
  gap: 10px;
}

.sidebar-panel__compose-anchor {
  position: relative;
}

.sidebar-panel__folders {
  display: flex;
  justify-content: flex-start;
  flex-wrap: nowrap;
  gap: 8px;
  margin-top: 14px;
  overflow-x: auto;
  overflow-y: hidden;
  scrollbar-width: none;
  -webkit-overflow-scrolling: touch;
}

.sidebar-panel__folders::-webkit-scrollbar {
  display: none;
}

.sidebar-panel__folder {
  flex: 0 0 auto;
  min-width: 0;
  height: 34px;
  padding: 0 13px;
  border: 1px solid var(--border-default);
  border-radius: var(--radius-pill);
  background: color-mix(in srgb, var(--surface-card) 82%, transparent);
  color: var(--text-secondary);
  font: 600 var(--text-xs)/1 var(--font-body);
  letter-spacing: 0.01em;
  transition:
    background var(--motion-fast) var(--motion-ease-out),
    color var(--motion-fast) var(--motion-ease-out),
    border-color var(--motion-fast) var(--motion-ease-out);
}

.sidebar-panel__folder.is-active {
  background: color-mix(in srgb, var(--interactive-selected-bg) 90%, var(--surface-card));
  border-color: var(--border-brand);
  color: var(--interactive-selected-fg);
}

.sidebar-panel__menu-trigger,
.sidebar-panel__back {
  width: 40px;
  height: 40px;
  display: grid;
  place-items: center;
  border: 1px solid var(--border-default);
  border-radius: var(--radius-control);
  background: var(--interactive-secondary-bg);
  color: var(--text-secondary);
  transition:
    background var(--motion-fast) var(--motion-ease-out),
    color var(--motion-fast) var(--motion-ease-out),
    border-color var(--motion-fast) var(--motion-ease-out);
}

.sidebar-panel__menu-trigger:hover,
.sidebar-panel__menu-trigger:focus-visible,
.sidebar-panel__back:hover,
.sidebar-panel__back:focus-visible {
  background: var(--interactive-secondary-bg-hover);
  border-color: var(--border-strong);
  color: var(--text-primary);
}

.sidebar-panel__compose-trigger {
  width: 40px;
  height: 40px;
  display: grid;
  place-items: center;
  border: 1px solid color-mix(in srgb, var(--interactive-primary-bg) 24%, var(--border-default));
  border-radius: var(--radius-control);
  background: color-mix(in srgb, var(--interactive-primary-bg) 8%, var(--interactive-secondary-bg));
  color: var(--interactive-selected-fg);
  transition:
    background var(--motion-fast) var(--motion-ease-out),
    color var(--motion-fast) var(--motion-ease-out),
    border-color var(--motion-fast) var(--motion-ease-out);
}

.sidebar-panel__compose-trigger:hover,
.sidebar-panel__compose-trigger:focus-visible {
  border-color: color-mix(in srgb, var(--interactive-primary-bg) 34%, var(--border-default));
  background: color-mix(in srgb, var(--interactive-primary-bg) 10%, var(--interactive-secondary-bg-hover));
}

.sidebar-panel__search :deep(.el-input__wrapper) {
  min-height: 44px;
  border-radius: var(--radius-pill);
  background: color-mix(in srgb, var(--surface-card) 88%, transparent);
  box-shadow: none;
  padding-inline: 11px;
}

.sidebar-panel__search :deep(.el-input__inner) {
  color: var(--text-primary);
}

.sidebar-panel__search :deep(.el-input__prefix),
.sidebar-panel__search :deep(.el-input__suffix) {
  color: var(--text-quaternary);
}

.sidebar-panel__notice {
  margin: 4px 12px 14px;
  position: relative;
  display: grid;
  grid-template-columns: minmax(0, 1fr) 34px;
  align-items: stretch;
  gap: 8px;
  overflow: hidden;
  border-radius: var(--radius-card);
  background:
    linear-gradient(180deg, color-mix(in srgb, var(--interactive-focus-ring) 10%, transparent), transparent 68%),
    linear-gradient(180deg, color-mix(in srgb, var(--surface-card) 96%, transparent), var(--surface-panel));
  color: var(--text-primary);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.04);
  border: 1px solid var(--border-default);
  transition:
    background var(--motion-base) var(--motion-ease-out),
    border-color var(--motion-fast) var(--motion-ease-out),
    box-shadow var(--motion-fast) var(--motion-ease-out);
}

.sidebar-panel__notice:hover,
.sidebar-panel__notice:focus-within {
  background:
    linear-gradient(180deg, color-mix(in srgb, var(--interactive-focus-ring) 14%, transparent), transparent 70%),
    linear-gradient(180deg, color-mix(in srgb, var(--surface-card) 96%, transparent), var(--surface-panel));
  border-color: var(--border-strong);
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.05),
    var(--shadow-sm);
}

.sidebar-panel__notice-action,
.sidebar-panel__notice-close {
  border: 0;
  background: transparent;
  color: inherit;
}

.sidebar-panel__notice-action {
  width: 100%;
  min-height: 68px;
  display: grid;
  grid-template-columns: 54px minmax(0, 1fr);
  align-items: center;
  gap: 14px;
  padding: 12px 0 12px 14px;
  text-align: left;
}

.sidebar-panel__notice-copy {
  display: block;
  min-width: 0;
}

.sidebar-panel__notice-icon {
  width: 48px;
  height: 48px;
  display: grid;
  place-items: center;
  border-radius: 50%;
  border: 1px solid color-mix(in srgb, var(--border-default) 88%, transparent);
  background:
    radial-gradient(circle at 30% 30%, color-mix(in srgb, #ffffff 8%, transparent), transparent 58%),
    color-mix(in srgb, var(--surface-panel) 92%, transparent);
  color: color-mix(in srgb, var(--text-secondary) 88%, var(--text-tertiary));
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.04);
}

.sidebar-panel__notice-icon :deep(svg) {
  width: 18px;
  height: 18px;
}

.sidebar-panel__notice strong {
  display: block;
  max-width: 100%;
  color: var(--text-primary);
  font: 700 var(--text-lg)/1.08 var(--font-display);
  letter-spacing: -0.02em;
  text-wrap: balance;
}

.sidebar-panel__notice p {
  margin-top: 5px;
  color: color-mix(in srgb, var(--text-secondary) 82%, var(--text-tertiary));
  font: 400 var(--text-base)/1.28 var(--font-body);
  letter-spacing: -0.01em;
}

.sidebar-panel__notice-close {
  width: 28px;
  height: 28px;
  margin: 12px 10px 0 0;
  display: grid;
  place-items: center;
  align-self: start;
  border-radius: 12px;
  color: var(--text-quaternary);
  transition:
    background var(--motion-fast) ease,
    color var(--motion-fast) ease,
    transform var(--motion-fast) ease;
}

.sidebar-panel__notice-close :deep(svg) {
  width: 16px;
  height: 16px;
}

.sidebar-panel__notice-close:hover,
.sidebar-panel__notice-close:focus-visible {
  background: color-mix(in srgb, var(--interactive-secondary-bg-hover) 90%, transparent);
  color: var(--text-secondary);
  transform: translateY(-1px);
}

.sidebar-panel__scroll {
  min-height: 0;
}

.sidebar-panel__scroll--list {
  padding-bottom: 72px;
}

.sidebar-panel__list,
.sidebar-panel__skeletons,
.sidebar-detail {
  display: grid;
}

.sidebar-stack {
  display: grid;
  gap: 12px;
}

.sidebar-panel__list {
  gap: 4px;
  padding: 10px 0 0;
}

.sidebar-panel__skeletons,
.sidebar-detail {
  gap: 10px;
  padding: 4px 18px 18px;
}

.sidebar-context-menu {
  position: fixed;
  z-index: 30;
  width: 228px;
  padding: 8px;
  border: 1px solid var(--border-default);
  border-radius: var(--radius-panel);
  background: color-mix(in srgb, var(--surface-overlay) 94%, rgba(20, 20, 22, 0.12));
  box-shadow: var(--shadow-md);
  backdrop-filter: blur(18px);
}

.sidebar-context-menu__item {
  width: 100%;
  min-height: 42px;
  display: grid;
  grid-template-columns: 28px minmax(0, 1fr);
  align-items: center;
  gap: 12px;
  padding: 0 12px;
  border: 0;
  border-radius: 12px;
  background: transparent;
  color: var(--text-primary);
  text-align: left;
  transition:
    background var(--motion-fast) var(--motion-ease-out),
    color var(--motion-fast) var(--motion-ease-out);
}

.sidebar-context-menu__item:hover,
.sidebar-context-menu__item:focus-visible {
  background: var(--interactive-selected-bg);
}

.sidebar-context-menu__item.is-danger {
  color: var(--text-danger);
}

.sidebar-context-menu__item.is-danger:hover,
.sidebar-context-menu__item.is-danger:focus-visible {
  background: color-mix(in srgb, var(--status-danger) 10%, var(--surface-panel));
}

.sidebar-context-menu__icon {
  width: 20px;
  height: 20px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.sidebar-context-menu__icon :deep(svg),
.sidebar-context-menu__glyph {
  width: 18px;
  height: 18px;
}

.sidebar-context-menu__label {
  font-size: var(--text-base);
  font-weight: 600;
  line-height: 1.2;
}

.sidebar-compose-menu {
  position: absolute;
  top: calc(100% + 10px);
  right: 0;
  z-index: 6;
  width: 212px;
  overflow: hidden;
  border: 1px solid var(--border-default);
  border-radius: var(--radius-panel);
  background: var(--surface-overlay);
  box-shadow: var(--shadow-md);
  backdrop-filter: blur(20px);
}

.sidebar-compose-menu__item {
  width: 100%;
  min-height: 44px;
  display: grid;
  grid-template-columns: 36px minmax(0, 1fr);
  align-items: center;
  gap: 10px;
  padding: 0 16px;
  border: 0;
  background: transparent;
  color: var(--text-primary);
  text-align: left;
  font-size: var(--text-base);
  font-weight: 600;
}

.sidebar-compose-menu__item:hover,
.sidebar-compose-menu__item:focus-visible {
  background: var(--interactive-secondary-bg-hover);
}

.sidebar-compose-menu__icon {
  width: 26px;
  height: 26px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: var(--text-tertiary);
}

.sidebar-compose-menu__icon :deep(svg) {
  width: 21px;
  height: 21px;
}

.sidebar-global-menu {
  position: absolute;
  top: 10px;
  left: 10px;
  z-index: 10;
  width: min(340px, calc(100% - 18px));
  display: grid;
  gap: 0;
  overflow: hidden;
  padding: 0;
  border-radius: var(--radius-panel);
  border: 1px solid var(--border-default);
  background: var(--surface-overlay);
  box-shadow: var(--shadow-md);
  backdrop-filter: blur(16px);
}

.sidebar-global-menu__identity {
  width: 100%;
  display: grid;
  grid-template-columns: 50px minmax(0, 1fr);
  align-items: center;
  gap: 12px;
  min-height: 68px;
  padding: 8px 18px 8px 16px;
  border: 0;
  background: transparent;
  color: var(--text-primary);
  text-align: left;
}

.sidebar-global-menu__identity:hover,
.sidebar-global-menu__identity:focus-visible {
  background: var(--interactive-secondary-bg-hover);
}

.sidebar-global-menu__identity :deep(.avatar-badge) {
  width: 46px;
  height: 46px;
  border: 2px solid color-mix(in srgb, var(--interactive-primary-bg) 36%, var(--border-brand));
}

.sidebar-global-menu__copy strong {
  display: block;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
  color: var(--text-primary);
  font-size: var(--text-base);
  font-weight: 600;
  line-height: 1.18;
}

.sidebar-global-menu__copy p {
  margin-top: 4px;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
  color: var(--text-quaternary);
  font: 500 var(--text-xs)/1 var(--font-mono);
}

.sidebar-global-menu__group {
  display: grid;
  padding: 6px 0;
  border-top: 1px solid var(--border-subtle);
}

.sidebar-global-menu__item {
  display: grid;
  grid-template-columns: 36px minmax(0, 1fr) auto auto;
  align-items: center;
  gap: 10px;
  min-height: 44px;
  padding: 0 14px 0 18px;
  border: 0;
  border-radius: 0;
  background: transparent;
  color: var(--text-primary);
  text-align: left;
  transition:
    background var(--motion-fast) var(--motion-ease-out),
    color var(--motion-fast) var(--motion-ease-out);
}

.sidebar-global-menu__item:hover,
.sidebar-global-menu__item:focus-visible {
  background: var(--interactive-secondary-bg-hover);
}

.sidebar-global-menu__item.is-active {
  background: var(--interactive-selected-bg);
}

.sidebar-global-menu__icon {
  width: 26px;
  height: 26px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: var(--text-tertiary);
}

.sidebar-global-menu__icon :deep(svg) {
  width: 21px;
  height: 21px;
}

.sidebar-global-menu__label {
  color: var(--text-primary);
  font-size: var(--text-base);
  font-weight: 600;
}

.sidebar-global-menu__label small {
  margin-left: 6px;
  color: var(--text-quaternary);
  font-size: var(--text-xs);
  font-weight: 600;
}

.sidebar-global-menu__meta {
  color: var(--text-quaternary);
  font: 500 var(--text-xs)/1 var(--font-mono);
}

.sidebar-global-menu__remove {
  color: var(--text-danger);
  font-size: var(--text-sm);
  font-weight: 700;
}

.sidebar-global-menu__chevron {
  display: inline-flex;
  justify-content: flex-end;
  color: #9ea3ad;
}

.sidebar-global-menu__chevron :deep(svg) {
  width: 17px;
  height: 17px;
}

.sidebar-menu-fade-enter-active,
.sidebar-menu-fade-leave-active {
  transition:
    opacity var(--motion-fast) ease,
    transform var(--motion-fast) ease;
}

.sidebar-menu-fade-enter-from,
.sidebar-menu-fade-leave-to {
  opacity: 0;
  transform: translateY(-8px);
}

.sidebar-panel__panel-copy {
  min-width: 0;
}

.profile-form__hint {
  margin-top: 8px;
  color: var(--text-quaternary);
  font-size: var(--text-xs);
  line-height: 1.5;
}

.profile-form__hint.is-error {
  color: var(--text-danger);
}

.profile-avatar-field {
  position: relative;
  display: grid;
  grid-template-columns: auto minmax(0, 1fr) auto;
  align-items: center;
  gap: 12px;
  padding: 14px;
  border: 1px solid var(--border-default);
  border-radius: 16px;
  background: color-mix(in srgb, var(--surface-card) 94%, transparent);
}

.profile-avatar-field.is-disabled {
  opacity: 0.86;
}

.profile-avatar-field__copy {
  min-width: 0;
}

.profile-avatar-field__copy strong {
  display: block;
  font-size: var(--text-base);
  line-height: 1.25;
}

.profile-avatar-field__copy p {
  margin-top: 4px;
  color: var(--text-quaternary);
  font-size: var(--text-xs);
  line-height: 1.45;
}

.profile-avatar-field__actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 8px;
}

.profile-avatar-field__input {
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

.sidebar-panel__panel-copy span {
  display: block;
  color: var(--text-quaternary);
  font: var(--font-eyebrow);
  letter-spacing: 0.14em;
  text-transform: uppercase;
  text-align: center;
}

.sidebar-panel__panel-copy strong {
  display: block;
  font: var(--font-title-md);
  text-align: center;
}

.sidebar-panel__panel-spacer {
  width: 40px;
  height: 40px;
}

.sidebar-profile-card,
.sidebar-section,
.settings-panel {
  display: grid;
  gap: 12px;
  padding: 16px;
  border-radius: var(--radius-panel);
  background: color-mix(in srgb, var(--surface-card) 94%, transparent);
  border: 1px solid var(--border-default);
  box-shadow: none;
  animation: settings-panel-enter 0.2s var(--motion-ease-out);
}

@keyframes settings-panel-enter {
  from {
    opacity: 0;
    transform: translateY(6px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.sidebar-detail--settings {
  gap: 12px;
}

.settings-header {
  display: grid;
  gap: 12px;
  padding: 4px 2px 0;
}

.settings-header__title strong,
.settings-panel__head strong {
  display: block;
  color: var(--text-primary);
  font: 620 var(--text-lg)/1.08 var(--font-display);
}

.settings-header__account {
  display: grid;
  grid-template-columns: 42px minmax(0, 1fr);
  align-items: center;
  gap: 12px;
  padding: 12px 14px;
  border: 1px solid color-mix(in srgb, var(--border-default) 88%, transparent);
  border-radius: var(--radius-md);
  background: color-mix(in srgb, var(--surface-panel) 88%, transparent);
}

.settings-header__account :deep(.avatar-badge) {
  width: 42px;
  height: 42px;
}

.settings-header__account-copy {
  min-width: 0;
  display: grid;
  gap: 3px;
}

.settings-header__account-copy small {
  color: var(--text-quaternary);
  font: 600 var(--text-2xs)/1 var(--font-body);
  letter-spacing: 0.02em;
}

.settings-header__account-copy strong {
  display: block;
  color: var(--text-primary);
  font-size: var(--text-base);
  line-height: 1.16;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.settings-header__account-copy p {
  margin: 0;
  color: var(--text-tertiary);
  font: 500 var(--text-xs)/1.12 var(--font-mono);
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.sidebar-profile-card {
  gap: 14px;
  background:
    radial-gradient(circle at top left, color-mix(in srgb, var(--interactive-focus-ring) 26%, transparent), transparent 42%),
    color-mix(in srgb, var(--surface-raised) 94%, transparent);
}

.sidebar-profile-card__main {
  grid-template-columns: auto minmax(0, 1fr);
  gap: 18px;
}

.sidebar-profile-card :deep(.avatar-badge) {
  width: 88px;
  height: 88px;
}

.sidebar-profile-card__copy {
  min-width: 0;
}

.sidebar-profile-card__eyebrow {
  margin: 2px 0 8px;
  color: var(--text-quaternary);
  font: 600 var(--text-sm)/1 var(--font-body);
  letter-spacing: 0.02em;
}

.sidebar-profile-card__copy strong {
  display: block;
  font: 620 1.02rem/1.08 var(--font-display);
  letter-spacing: -0.01em;
}

.sidebar-profile-card__copy p {
  margin-top: 8px;
  color: var(--text-quaternary);
  font: 500 var(--text-xs)/1.2 var(--font-mono);
}

.sidebar-profile-card__copy span {
  display: block;
  margin-top: 8px;
  color: var(--text-secondary);
  font-size: var(--text-base);
  line-height: 1.62;
}

.sidebar-profile-card__copy span {
  max-width: 24ch;
}

.sidebar-profile-card__meta {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.sidebar-profile-card__meta article {
  min-width: 0;
  display: grid;
  gap: 6px;
  align-content: start;
  padding: 12px 14px;
  border: 1px solid var(--border-default);
  border-radius: var(--radius-md);
  background: color-mix(in srgb, var(--surface-panel) 90%, transparent);
}

.sidebar-profile-card__meta span {
  display: block;
  color: var(--text-quaternary);
  font: 500 var(--text-xs)/1.1 var(--font-body);
  letter-spacing: 0.01em;
}

.sidebar-profile-card__meta strong {
  display: block;
  font-size: var(--text-lg);
  line-height: 1.24;
  overflow-wrap: anywhere;
}

.sidebar-profile-card__actions,
.sidebar-actions {
  display: grid;
  grid-template-columns: 1fr;
  gap: 12px;
}

.sidebar-profile-card__action {
  width: 100%;
  min-height: 44px;
  border-radius: var(--radius-pill);
  font-weight: 600;
  margin: 0;
  border: 1px solid var(--border-default);
  background: var(--interactive-secondary-bg);
  color: var(--text-primary);
  transition:
    border-color var(--motion-fast) var(--motion-ease-out),
    background var(--motion-fast) var(--motion-ease-out),
    color var(--motion-fast) var(--motion-ease-out),
    box-shadow var(--motion-fast) var(--motion-ease-out);
}

.sidebar-profile-card__action:hover,
.sidebar-profile-card__action:focus-visible {
  border-color: var(--border-strong);
  background: var(--interactive-secondary-bg-hover);
}

.sidebar-profile-card__secondary-actions {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
  align-items: stretch;
}

.sidebar-profile-card__secondary-actions > * {
  min-width: 0;
}

.sidebar-profile-card__action--secondary {
  min-height: 40px;
  border-color: transparent;
  background: color-mix(in srgb, var(--surface-panel) 88%, transparent);
  color: var(--text-secondary);
}

.sidebar-profile-card__action--secondary:hover,
.sidebar-profile-card__action--secondary:focus-visible {
  border-color: var(--border-default);
  background: var(--interactive-secondary-bg);
  color: var(--text-primary);
}

.sidebar-profile-card__actions :deep(.el-button) {
  margin: 0;
  padding-inline: 18px;
  justify-content: center;
  border-radius: var(--radius-pill);
  font-weight: 600;
}

.sidebar-profile-card__actions :deep(.el-button--primary) {
  border-color: var(--border-default);
  background: var(--interactive-secondary-bg);
  color: var(--text-primary);
  box-shadow: none;
}

.sidebar-profile-card__actions :deep(.el-button--primary:hover),
.sidebar-profile-card__actions :deep(.el-button--primary:focus-visible) {
  border-color: var(--border-strong);
  background: var(--interactive-secondary-bg-hover);
  color: var(--text-primary);
}

.sidebar-profile-card__actions :deep(.el-button > span) {
  width: 100%;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  text-align: center;
}

.sidebar-profile-card__secondary-actions :deep(.el-button + .el-button) {
  margin-left: 0;
}

.sidebar-section__head span {
  display: block;
  color: var(--text-quaternary);
  font: 600 var(--text-xs)/1 var(--font-body);
  letter-spacing: 0.02em;
}

.settings-panel__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 0;
}

.sidebar-section__head strong {
  display: block;
  font: 620 var(--text-base)/1.08 var(--font-display);
}

.sidebar-section__head p,
.settings-panel__head p {
  margin: 0;
  color: var(--text-secondary);
  font-size: var(--text-sm);
  line-height: 1.5;
}

.info-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.sidebar-info-list {
  display: grid;
  gap: 12px;
}

.sidebar-info-list__item {
  display: grid;
  gap: 8px;
  padding: 12px 14px;
  border: 1px solid var(--border-default);
  border-radius: var(--radius-md);
  background: color-mix(in srgb, var(--surface-card) 92%, transparent);
}

.sidebar-info-list__item span {
  display: block;
  color: var(--text-quaternary);
  font: 500 var(--text-xs)/1.1 var(--font-body);
  letter-spacing: 0.01em;
}

.sidebar-info-list__item strong {
  display: block;
  font-size: var(--text-lg);
  line-height: 1.4;
  overflow-wrap: anywhere;
}

.info-grid article,
.settings-item,
.theme-card__option,
.security-card {
  padding: 16px;
  border: 1px solid var(--border-default);
  border-radius: 16px;
  background: color-mix(in srgb, var(--surface-card) 92%, transparent);
}

.info-grid article {
  min-width: 0;
}

.info-grid span {
  display: block;
  color: var(--text-quaternary);
  font: 500 var(--text-xs)/1 var(--font-mono);
  text-transform: uppercase;
}

.info-grid strong {
  display: block;
  margin-top: 8px;
  font-size: var(--text-sm);
  line-height: 1.35;
  overflow-wrap: anywhere;
  word-break: break-word;
}

.profile-form :deep(.el-form-item) {
  margin-bottom: 12px;
}

.settings-tabs {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px;
}

.settings-switcher {
  display: grid;
}

.settings-tabs__item {
  display: grid;
  align-items: center;
  min-height: 50px;
  padding: 12px 14px;
  border: 1px solid color-mix(in srgb, var(--border-default) 82%, transparent);
  border-radius: 14px;
  background: color-mix(in srgb, var(--surface-panel) 72%, transparent);
  color: var(--text-secondary);
  text-align: center;
  transition:
    background var(--motion-fast) var(--motion-ease-out),
    border-color var(--motion-fast) var(--motion-ease-out),
    color var(--motion-fast) var(--motion-ease-out),
    box-shadow var(--motion-base) var(--motion-ease-out);
}

.settings-tabs__item strong {
  display: block;
  color: var(--text-primary);
  font-size: var(--text-sm);
  line-height: 1.2;
}

.settings-tabs__item:hover,
.settings-tabs__item:focus-visible {
  background: color-mix(in srgb, var(--interactive-selected-bg) 82%, var(--surface-panel));
  border-color: color-mix(in srgb, var(--border-brand) 82%, transparent);
  color: var(--text-primary);
  box-shadow: 0 0 0 2px color-mix(in srgb, var(--interactive-focus-ring) 18%, transparent);
}

.settings-tabs__item.is-active {
  background: color-mix(in srgb, var(--interactive-selected-bg) 86%, var(--surface-card));
  border-color: color-mix(in srgb, var(--interactive-primary-bg) 26%, var(--border-brand));
  color: var(--text-primary);
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.08),
    0 0 0 2px color-mix(in srgb, var(--interactive-focus-ring) 18%, transparent);
}

.theme-card,
.settings-list {
  display: grid;
  gap: 8px;
}

.theme-card--stacked,
.settings-list--grouped {
  gap: 0;
  overflow: hidden;
  border: 1px solid var(--border-default);
  border-radius: var(--radius-md);
  background: color-mix(in srgb, var(--surface-panel) 84%, transparent);
}

.theme-card__option,
.settings-item,
.security-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  width: 100%;
  color: var(--text-primary);
  text-align: left;
  transition:
    background var(--motion-fast) var(--motion-ease-out),
    border-color var(--motion-fast) var(--motion-ease-out),
    box-shadow var(--motion-base) var(--motion-ease-out);
}

.theme-card--stacked .theme-card__option,
.settings-list--grouped .settings-item {
  border: 0;
  border-radius: 0;
  background: transparent;
  padding: 12px 14px;
}

.theme-card--stacked .theme-card__option + .theme-card__option,
.settings-list--grouped .settings-item + .settings-item {
  border-top: 1px solid color-mix(in srgb, var(--border-default) 84%, transparent);
}

.theme-card__option {
  min-height: 56px;
}

.theme-card__icon,
.security-card__icon {
  width: 32px;
  height: 32px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: var(--radius-control);
  background: color-mix(in srgb, var(--interactive-focus-ring) 46%, transparent);
  color: var(--text-tertiary);
  flex-shrink: 0;
}

.theme-card__copy {
  display: grid;
  gap: 2px;
  flex: 1;
  align-content: center;
}

.theme-card__option strong,
.settings-item strong,
.security-card strong {
  display: block;
  font-size: var(--text-sm);
}

.settings-item > div:first-child,
.settings-item--stacked > div:first-child,
.security-card > div:last-child {
  display: grid;
  gap: 4px;
  flex: 1;
  min-width: 0;
}

.theme-card__option p,
.settings-item p,
.security-card p {
  color: var(--text-secondary);
  font-size: var(--text-xs);
  line-height: 1.44;
}

:deep(.theme-card__icon svg) {
  width: 24px;
  height: 24px;
}

.theme-card__option:hover,
.theme-card__option:focus-visible,
.settings-item:hover,
.settings-item:focus-within,
.security-card:hover,
.security-card:focus-within {
  background: color-mix(in srgb, var(--interactive-selected-bg) 92%, transparent);
  border-color: color-mix(in srgb, var(--border-strong) 92%, transparent);
  box-shadow: 0 0 0 3px color-mix(in srgb, var(--interactive-focus-ring) 26%, transparent);
}

.theme-card__option.is-active {
  background: color-mix(in srgb, var(--interactive-selected-bg) 92%, var(--surface-panel));
  border-color: color-mix(in srgb, var(--interactive-primary-bg) 18%, var(--border-strong));
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.08),
    0 0 0 3px color-mix(in srgb, var(--interactive-focus-ring) 26%, transparent);
}

.settings-item {
  align-items: center;
  min-height: 56px;
}

.settings-item--stacked {
  align-items: start;
}

.settings-item--status {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  align-items: center;
  gap: 12px;
  min-height: 0;
  padding: 14px 14px 12px;
}

.settings-item__status-copy {
  min-width: 0;
  display: grid;
  gap: 10px;
}

.settings-item__status-copy strong {
  font-size: var(--text-base);
  line-height: 1.18;
}

.settings-item__status-copy p {
  max-width: 24ch;
  font-size: var(--text-sm);
  line-height: 1.56;
}

.settings-actions {
  display: grid;
  gap: 10px;
  padding-top: 2px;
}

.settings-actions--stacked {
  grid-template-columns: 1fr;
}

.settings-actions :deep(.el-button) {
  width: 100%;
  min-height: 40px;
  margin: 0;
  border-radius: var(--radius-pill);
  font-weight: 600;
}

.settings-actions :deep(.el-button--primary) {
  border-color: var(--border-default);
  background: var(--interactive-secondary-bg);
  color: var(--text-primary);
  box-shadow: none;
}

.settings-actions :deep(.el-button--primary:hover),
.settings-actions :deep(.el-button--primary:focus-visible) {
  border-color: var(--border-strong);
  background: var(--interactive-secondary-bg-hover);
}

.settings-actions__primary:deep(.el-button),
.settings-actions__secondary:deep(.el-button) {
  width: 100%;
}

.settings-tag--status {
  min-width: 72px;
  min-height: 28px;
  justify-self: end;
}

.settings-empty {
  padding: 10px 2px 0;
  color: var(--text-secondary);
  font-size: var(--text-sm);
}

.blocked-users-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.blocked-user-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 12px;
  border-radius: var(--radius-panel);
  background: var(--surface-panel);
  border: 1px solid var(--border-subtle);
}

.blocked-user-item__copy {
  flex: 1;
  min-width: 0;
}

.blocked-user-item__copy strong {
  display: block;
  color: var(--text-primary);
  font-size: var(--text-sm);
  font-weight: 600;
}

.blocked-user-item__copy span {
  display: block;
  color: var(--text-quaternary);
  font-size: var(--text-xs);
  font-family: var(--font-mono);
}

.sidebar-stack--security {
  gap: 10px;
}

.security-overview {
  display: grid;
  gap: 10px;
  padding: 14px;
  border: 1px solid var(--border-default);
  border-radius: var(--radius-panel);
  background:
    linear-gradient(135deg, color-mix(in srgb, var(--interactive-primary-bg) 7%, transparent), transparent 62%),
    color-mix(in srgb, var(--surface-raised) 95%, transparent);
}

.security-overview__header {
  display: flex;
  align-items: start;
  justify-content: space-between;
  gap: 12px;
}

.security-overview.is-warning {
  background:
    linear-gradient(135deg, color-mix(in srgb, var(--status-danger) 6%, transparent), transparent 62%),
    color-mix(in srgb, var(--surface-raised) 95%, transparent);
}

.security-overview__copy {
  display: grid;
  gap: 5px;
}

.security-overview__copy span {
  color: var(--text-quaternary);
  font: 600 var(--text-xs)/1 var(--font-mono);
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.security-overview__copy strong {
  font: 620 var(--text-base)/1.08 var(--font-display);
}

.security-overview__copy p {
  color: var(--text-secondary);
  font-size: var(--text-xs);
  line-height: 1.46;
}

.security-overview__status {
  display: flex;
  justify-content: flex-start;
}

.security-metrics {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px;
}

.security-metrics article {
  min-width: 0;
  padding: 10px 12px;
  border: 1px solid color-mix(in srgb, var(--border-default) 88%, transparent);
  border-radius: var(--radius-md);
  background: color-mix(in srgb, var(--surface-panel) 86%, transparent);
}

.security-metrics span {
  display: block;
  color: var(--text-quaternary);
  font: 600 var(--text-xs)/1 var(--font-mono);
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.security-metrics strong {
  display: block;
  margin-top: 8px;
  font-size: var(--text-sm);
  line-height: 1.34;
  overflow-wrap: anywhere;
}

.security-overview__contact {
  display: grid;
  gap: 8px;
  padding: 14px 16px;
  border: 1px solid color-mix(in srgb, var(--border-default) 88%, transparent);
  border-radius: 14px;
  background: color-mix(in srgb, var(--surface-panel) 84%, transparent);
}

.security-overview__contact span {
  color: var(--text-quaternary);
  font: 600 var(--text-xs)/1 var(--font-body);
}

.security-overview__contact strong {
  font-size: var(--text-base);
  line-height: 1.4;
  overflow-wrap: anywhere;
}

.security-alerts {
  display: grid;
  gap: 8px;
  padding: 14px;
  border: 1px solid color-mix(in srgb, var(--status-danger) 12%, var(--border-default));
  border-radius: var(--radius-panel);
  background:
    linear-gradient(180deg, color-mix(in srgb, var(--status-danger) 5%, transparent), transparent 34%),
    color-mix(in srgb, var(--surface-raised) 95%, transparent);
}

.security-alerts__head {
  display: grid;
  gap: 5px;
}

.security-alerts__head span {
  color: var(--text-danger);
  font: 600 var(--text-xs)/1 var(--font-mono);
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.security-alerts__head strong {
  font: 620 var(--text-base)/1.08 var(--font-display);
}

.security-alerts__list {
  display: grid;
  gap: 8px;
}

.security-alert {
  display: grid;
  gap: 8px;
  padding: 12px 13px;
  border: 1px solid color-mix(in srgb, var(--border-default) 84%, transparent);
  border-radius: 14px;
  background: color-mix(in srgb, var(--surface-panel) 86%, transparent);
}

.security-alert.is-critical {
  border-color: color-mix(in srgb, var(--status-danger) 18%, var(--border-default));
  background:
    linear-gradient(135deg, color-mix(in srgb, var(--status-danger) 8%, transparent), transparent 78%),
    color-mix(in srgb, var(--surface-panel) 86%, transparent);
}

.security-alert.is-warning {
  border-color: color-mix(in srgb, var(--status-warning) 24%, var(--border-default));
  background:
    linear-gradient(135deg, color-mix(in srgb, var(--status-warning) 9%, transparent), transparent 78%),
    color-mix(in srgb, var(--surface-panel) 86%, transparent);
}

.security-alert__topline {
  display: flex;
  align-items: start;
  justify-content: space-between;
  gap: 12px;
}

.security-alert__headline {
  min-width: 0;
  display: grid;
  gap: 6px;
}

.security-alert__eyebrow {
  color: var(--text-quaternary);
  font: 600 var(--text-xs)/1 var(--font-mono);
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.security-alert__topline time {
  flex-shrink: 0;
  color: var(--text-quaternary);
  font: 600 var(--text-xs)/1 var(--font-mono);
  letter-spacing: 0.06em;
}

.security-alert strong {
  font-size: var(--text-base);
  line-height: 1.24;
}

.security-alert p {
  color: var(--text-secondary);
  font-size: var(--text-xs);
  line-height: 1.46;
}

.security-alert--compact {
  gap: 8px;
  padding: 12px;
}

.security-alert--compact p {
  display: -webkit-box;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
  overflow: hidden;
}

.security-alerts__footer {
  color: var(--text-tertiary);
  font-size: var(--text-xs);
  line-height: 1.4;
}

.security-alert__badges {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.sidebar-section--security-primary {
  background:
    linear-gradient(180deg, color-mix(in srgb, var(--interactive-primary-bg) 5%, transparent), transparent 28%),
    color-mix(in srgb, var(--surface-raised) 94%, transparent);
}

.sidebar-section--security-support .settings-list--grouped {
  background: color-mix(in srgb, var(--surface-panel) 78%, transparent);
}

.sidebar-section--security-danger {
  background:
    linear-gradient(180deg, color-mix(in srgb, var(--status-danger) 4%, transparent), transparent 24%),
    color-mix(in srgb, var(--surface-raised) 94%, transparent);
}

.security-card--hero {
  min-height: 84px;
  background:
    linear-gradient(135deg, color-mix(in srgb, var(--interactive-focus-ring) 18%, transparent), transparent 70%),
    color-mix(in srgb, var(--surface-card) 94%, transparent);
}

.security-operations {
  gap: 10px;
}

.security-disclosure,
.security-history {
  border: 1px solid color-mix(in srgb, var(--border-default) 88%, transparent);
  border-radius: var(--radius-panel);
  background: color-mix(in srgb, var(--surface-panel) 84%, transparent);
}

.security-disclosure[open],
.security-history[open] {
  background: color-mix(in srgb, var(--surface-card) 92%, transparent);
}

.security-disclosure__summary,
.security-history__summary {
  list-style: none;
  cursor: pointer;
}

.security-disclosure__summary::-webkit-details-marker,
.security-history__summary::-webkit-details-marker {
  display: none;
}

.security-disclosure__summary {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 12px 14px;
}

.security-disclosure__lead {
  min-width: 0;
  display: grid;
  grid-template-columns: 32px minmax(0, 1fr);
  align-items: center;
  gap: 10px;
}

.security-disclosure__icon {
  width: 32px;
  height: 32px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: var(--radius-control);
  background: color-mix(in srgb, var(--interactive-focus-ring) 36%, transparent);
  color: var(--text-tertiary);
}

.security-disclosure__icon :deep(svg) {
  width: 18px;
  height: 18px;
}

.security-disclosure__copy {
  min-width: 0;
  display: grid;
  gap: 5px;
}

.security-disclosure__copy strong {
  font-size: var(--text-base);
  line-height: 1.18;
}

.security-disclosure__copy p {
  color: var(--text-secondary);
  font-size: var(--text-xs);
  line-height: 1.42;
}

.security-disclosure__meta,
.security-history__meta {
  color: var(--text-tertiary);
  font: 600 var(--text-xs)/1 var(--font-mono);
  white-space: nowrap;
}

.security-disclosure__body,
.security-history__body {
  display: grid;
  gap: 10px;
  padding: 0 14px 14px;
}

.security-actions {
  display: grid;
  gap: 10px;
}

.security-actions--single {
  grid-template-columns: 1fr;
}

.security-actions--split {
  grid-template-columns: minmax(0, 1.25fr) minmax(0, 0.9fr);
  align-items: center;
}

.security-actions :deep(.el-button) {
  width: 100%;
  min-height: 40px;
  margin: 0;
  border-radius: var(--radius-pill);
  font-weight: 600;
}

.security-actions__secondary:deep(.el-button) {
  background: color-mix(in srgb, var(--surface-raised) 88%, transparent);
}

.security-actions :deep(.el-button--primary) {
  border-color: var(--border-default);
  background: var(--interactive-secondary-bg);
  color: var(--text-primary);
  box-shadow: none;
}

.security-actions :deep(.el-button--primary:hover),
.security-actions :deep(.el-button--primary:focus-visible) {
  border-color: var(--border-strong);
  background: var(--interactive-secondary-bg-hover);
}

.security-actions__danger:deep(.el-button) {
  border-color: color-mix(in srgb, var(--status-danger) 28%, var(--border-default));
  background: color-mix(in srgb, var(--status-danger) 10%, var(--surface-card));
  color: var(--text-danger);
}

.security-history {
  padding: 0;
}

.security-history__summary {
  padding: 14px;
}

.security-history__summary .settings-panel__head {
  margin-bottom: 0;
}

.security-timeline {
  display: grid;
  gap: 10px;
}

.security-timeline__group {
  display: grid;
  gap: 6px;
}

.security-timeline__group-head {
  display: flex;
  align-items: center;
  gap: 10px;
}

.security-timeline__group-head::after {
  content: '';
  flex: 1;
  height: 1px;
  background: color-mix(in srgb, var(--border-default) 84%, transparent);
}

.security-timeline__group-head span {
  color: var(--text-quaternary);
  font: 600 var(--text-xs)/1 var(--font-mono);
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.security-timeline__list {
  display: grid;
  gap: 8px;
}

.security-event {
  display: grid;
  grid-template-columns: 14px minmax(0, 1fr);
  gap: 10px;
  padding: 10px 12px;
  border: 1px solid color-mix(in srgb, var(--border-default) 84%, transparent);
  border-radius: var(--radius-md);
  background: color-mix(in srgb, var(--surface-panel) 84%, transparent);
}

.security-event__rail {
  position: relative;
  display: flex;
  justify-content: center;
}

.security-event__rail::before {
  content: '';
  position: absolute;
  top: 2px;
  bottom: 2px;
  width: 1px;
  background: color-mix(in srgb, var(--border-default) 84%, transparent);
}

.security-event__dot {
  position: relative;
  z-index: 1;
  width: 9px;
  height: 9px;
  margin-top: 4px;
  border-radius: 50%;
  border: 1px solid var(--border-strong);
  background: var(--surface-raised);
}

.security-event.is-critical .security-event__dot {
  border-color: color-mix(in srgb, var(--status-danger) 38%, var(--border-default));
  background: color-mix(in srgb, var(--status-danger) 24%, var(--surface-raised));
}

.security-event.is-warning .security-event__dot {
  border-color: color-mix(in srgb, var(--status-warning) 38%, var(--border-default));
  background: color-mix(in srgb, var(--status-warning) 22%, var(--surface-raised));
}

.security-event__body {
  min-width: 0;
  display: grid;
  gap: 7px;
}

.security-event__head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 12px;
}

.security-event__head strong {
  min-width: 0;
  font-size: var(--text-sm);
  line-height: 1.24;
}

.security-event__head time {
  flex-shrink: 0;
  color: var(--text-quaternary);
  font: 600 var(--text-xs)/1 var(--font-mono);
  letter-spacing: 0.06em;
}

.security-event__badges {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.security-event__badge {
  display: inline-flex;
  align-items: center;
  min-height: 22px;
  padding: 0 8px;
  border-radius: 999px;
  border: 1px solid var(--border-default);
  background: color-mix(in srgb, var(--interactive-secondary-bg) 92%, transparent);
  color: var(--text-secondary);
  font: 600 var(--text-xs)/1 var(--font-mono);
  letter-spacing: 0.04em;
}

.security-event__badge.is-critical,
.security-event__badge.is-failure {
  border-color: color-mix(in srgb, var(--status-danger) 18%, var(--border-default));
  color: var(--text-danger);
}

.security-event__badge.is-warning {
  border-color: color-mix(in srgb, var(--status-warning) 24%, var(--border-default));
  color: var(--text-primary);
}

.security-event__badge.is-success {
  border-color: color-mix(in srgb, var(--status-success) 18%, var(--border-default));
  color: var(--status-success);
}

.security-event__badge.is-neutral {
  color: var(--text-quaternary);
}

.security-event__body p {
  color: var(--text-secondary);
  font-size: var(--text-xs);
  line-height: 1.46;
}

.security-event__meta {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.security-event__meta span {
  display: inline-flex;
  align-items: center;
  min-height: 22px;
  padding: 0 8px;
  border-radius: 999px;
  background: color-mix(in srgb, var(--interactive-secondary-bg) 76%, transparent);
  color: var(--text-quaternary);
  font: 600 var(--text-xs)/1 var(--font-mono);
}

.security-inline {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 8px;
  width: 100%;
}

.settings-tag {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 72px;
  min-height: 26px;
  padding: 0 10px;
  border-radius: 999px;
  background: color-mix(in srgb, var(--interactive-secondary-bg) 92%, transparent);
  color: var(--text-secondary);
  font: 600 var(--text-xs)/1 var(--font-mono);
  text-transform: uppercase;
}

.sidebar-actions :deep(.el-button) {
  min-height: 40px;
  border-radius: 13px;
  font-weight: 600;
  letter-spacing: -0.01em;
  transition:
    background var(--motion-fast) ease,
    border-color var(--motion-fast) ease,
    color var(--motion-fast) ease,
    box-shadow var(--motion-base) ease,
    opacity var(--motion-fast) ease;
}

.sidebar-actions :deep(.el-button:hover),
.sidebar-actions :deep(.el-button:focus-visible) {
  box-shadow: 0 0 0 3px color-mix(in srgb, var(--interactive-focus-ring) 26%, transparent);
}

.sidebar-actions :deep(.el-button--danger),
.settings-list :deep(.el-button--danger) {
  color: var(--text-danger);
}

.sidebar-detail--settings :deep(.el-input__wrapper) {
  min-height: 42px;
  border-radius: 13px;
  background: color-mix(in srgb, var(--surface-card) 94%, transparent);
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.12),
    0 0 0 1px color-mix(in srgb, var(--border-default) 84%, transparent);
}

.sidebar-detail--settings :deep(.el-input__wrapper.is-focus) {
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.14),
    0 0 0 1px color-mix(in srgb, var(--interactive-primary-bg) 24%, transparent),
    0 0 0 4px color-mix(in srgb, var(--interactive-focus-ring) 28%, transparent);
}

.sidebar-detail--settings :deep(.el-switch) {
  --el-switch-on-color: var(--interactive-primary-bg);
  --el-switch-off-color: color-mix(in srgb, var(--border-strong) 82%, var(--surface-subtle));
}

.settings-list :deep(.el-switch) {
  flex-shrink: 0;
}

.settings-tag.is-success {
  color: var(--status-success);
}

.settings-tag.is-failure {
  color: var(--text-danger);
}

.sidebar-notice {
  padding: 12px 14px;
  border-radius: 14px;
  font-size: var(--text-sm);
  line-height: 1.46;
}

.sidebar-notice--success {
  background: color-mix(in srgb, var(--status-success) 12%, var(--surface-card));
  color: var(--status-success);
}

.sidebar-notice--error {
  background: color-mix(in srgb, var(--status-danger) 12%, var(--surface-card));
  color: var(--text-danger);
}

.sidebar-notice--warning {
  background: color-mix(in srgb, var(--status-warning) 14%, var(--surface-card));
  color: var(--text-primary);
}

@media (max-width: 767px) {
  .sidebar-panel {
    border-radius: 0;
    border-inline: 0;
  }

  .sidebar-panel__header,
  .sidebar-panel__skeletons,
  .sidebar-detail {
    padding-inline: 14px;
  }

  .sidebar-panel__notice {
    margin-inline: 14px;
  }

  .info-grid {
    grid-template-columns: 1fr;
  }

  .security-metrics {
    grid-template-columns: 1fr;
  }

  .sidebar-profile-card :deep(.avatar-badge) {
    width: 72px;
    height: 72px;
  }

  .sidebar-profile-card__meta,
  .settings-tabs {
    grid-template-columns: 1fr;
  }

  .sidebar-profile-card__actions {
    gap: 10px;
  }

  .sidebar-profile-card__secondary-actions {
    grid-template-columns: 1fr;
    gap: 10px;
  }

  .settings-header {
    padding-top: 0;
  }

  .settings-header__account {
    grid-template-columns: 40px minmax(0, 1fr);
    padding: 11px 12px;
  }

  .settings-tabs {
    grid-template-columns: 1fr;
  }

  .settings-panel__head {
    align-items: flex-start;
    flex-direction: column;
    gap: 6px;
  }

  .security-actions--split {
    grid-template-columns: 1fr;
  }
}

.totp-status,
.totp-setup {
  display: grid;
  gap: 12px;
}

.totp-status__info p {
  margin: 0;
  color: var(--text-secondary);
  font-size: var(--text-sm);
  line-height: 1.46;
}

.totp-setup__start p {
  margin: 0;
  color: var(--text-secondary);
  font-size: var(--text-sm);
  line-height: 1.46;
}

.totp-setup__instruction {
  margin: 0;
  color: var(--text-secondary);
  font-size: var(--text-sm);
  line-height: 1.46;
}

.totp-setup__secret {
  padding: 10px 12px;
  border: 1px solid var(--border-default);
  border-radius: var(--radius-control);
  background: color-mix(in srgb, var(--surface-panel) 94%, transparent);
  text-align: center;
}

.totp-setup__secret code {
  font: 600 var(--text-sm)/1 var(--font-mono);
  letter-spacing: 0.08em;
  word-break: break-all;
}

.totp-setup__qr {
  display: flex;
  justify-content: center;
  padding: 12px;
  border: 1px solid var(--border-default);
  border-radius: var(--radius-control);
  background: white;
}

.totp-setup__qr img {
  width: 180px;
  height: 180px;
}

.totp-setup__recovery {
  display: grid;
  gap: 8px;
}

.totp-setup__recovery strong {
  font-size: var(--text-sm);
  font-weight: 600;
}

.totp-recovery-codes {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 6px;
}

.totp-recovery-codes code {
  padding: 6px 8px;
  border: 1px solid var(--border-default);
  border-radius: var(--radius-control);
  background: color-mix(in srgb, var(--surface-panel) 94%, transparent);
  font: 500 var(--text-xs)/1 var(--font-mono);
  text-align: center;
  letter-spacing: 0.04em;
}
</style>
