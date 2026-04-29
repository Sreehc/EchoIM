<script setup lang="ts">
import { computed, nextTick, onMounted, onUnmounted, reactive, ref, watch } from 'vue'
import {
  ArrowLeft,
  ArrowRight,
  Bell,
  ChatRound,
  CirclePlus,
  Close,
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
  ConversationFolder,
  ConversationSummary,
  CurrentUserProfile,
  LeftPanelMode,
  SettingsSection,
  StoredAccount,
  ThemeMode,
  UpdateCurrentUserProfilePayload,
  UserInfo,
} from '@/types/chat'
import { readBrowserNotificationPermission } from '@/utils/browserNotifications'
import { STORAGE_KEYS } from '@/utils/storage'
import ConversationListItem from './ConversationListItem.vue'
import AvatarBadge from './AvatarBadge.vue'
import ChatStatePanel from './ChatStatePanel.vue'

type ConversationContextAction = 'open-tab' | 'mark-unread' | 'toggle-top' | 'toggle-mute' | 'archive' | 'delete'
type ComposeAction = 'single' | 'group' | 'channel'

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
  profileError?: string | null
  profileNotice?: string | null
  usernameChecking?: boolean
  usernameAvailable?: boolean | null
  usernameMessage?: string | null
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
  'clear-profile-error': []
  'clear-profile-notice': []
  'conversation-action': [payload: { command: ConversationContextAction; conversationId: number }]
  'update:conversation-folder': [value: ConversationFolder]
  'compose-action': [value: ComposeAction]
  logout: []
}>()

const searchInput = ref()
const scrollbarRef = ref()
const draftSearchQuery = ref(props.searchQuery)
const notificationPromptVisible = ref(localStorage.getItem(STORAGE_KEYS.desktopNotificationPromptDismissed) !== 'true')
const notificationPermission = ref<NotificationPermission | 'unsupported'>(readBrowserNotificationPermission())
const notificationRequesting = ref(false)
const composeMenuOpen = ref(false)
const editingProfile = ref(false)
const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: '',
  localError: '',
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

const panelEyebrow = computed(() => {
  if (props.leftPanelMode === 'me') return 'My Echo'
  if (props.leftPanelMode === 'settings') return 'Preferences'
  return 'Workspace'
})
const shouldShowNotificationPrompt = computed(
  () =>
    notificationPromptVisible.value &&
    notificationPermission.value !== 'unsupported' &&
    notificationPermission.value !== 'granted',
)
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
    title: '不错过新消息',
    description: '开启桌面通知，切到其他窗口也能及时收到提醒。',
  }
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
  },
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

  if (composeMenuOpen.value && !event.target.closest('.sidebar-compose-menu, .sidebar-panel__fab')) {
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

function handlePlaceholderAction() {
  closeGlobalMenu()
  closeComposeMenu()
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
  emit('clear-profile-error')
  emit('clear-profile-notice')
}

function cancelProfileEditing() {
  editingProfile.value = false
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
  emit('save-profile', {
    username: profileDraft.username.trim(),
    nickname: profileDraft.nickname.trim(),
    avatarUrl: profileDraft.avatarUrl.trim() || null,
    gender: profileDraft.gender,
    signature: profileDraft.signature.trim() || null,
  })
  editingProfile.value = false
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
  <aside class="sidebar-panel" :class="[`mode-${leftPanelMode}`]">
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
        </div>
        <div class="sidebar-panel__folders">
          <button
            class="sidebar-panel__folder"
            :class="{ 'is-active': conversationFolder === 'inbox' }"
            type="button"
            @click="emit('update:conversation-folder', 'inbox')"
          >
            收件箱
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
            :class="{ 'is-active': conversationFolder === 'single' }"
            type="button"
            @click="emit('update:conversation-folder', 'single')"
          >
            私聊
          </button>
          <button
            class="sidebar-panel__folder"
            :class="{ 'is-active': conversationFolder === 'group' }"
            type="button"
            @click="emit('update:conversation-folder', 'group')"
          >
            群组
          </button>
          <button
            class="sidebar-panel__folder"
            :class="{ 'is-active': conversationFolder === 'channel' }"
            type="button"
            @click="emit('update:conversation-folder', 'channel')"
          >
            频道
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
              data-testid="sidebar-menu-profile-settings"
              @click="openPanel('settings')"
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
                <span class="sidebar-global-menu__label">Saved Messages</span>
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
              <button class="sidebar-global-menu__item" type="button" role="menuitem" @click="handlePlaceholderAction">
                <span class="sidebar-global-menu__icon"><MoreFilled /></span>
                <span class="sidebar-global-menu__label">更多</span>
                <span class="sidebar-global-menu__chevron"><ArrowRight /></span>
              </button>
              <button class="sidebar-global-menu__item" type="button" role="menuitem" @click="handleLogout">
                <span class="sidebar-global-menu__icon"><Lock /></span>
                <span class="sidebar-global-menu__label">退出登录</span>
              </button>
            </div>
          </div>
        </transition>
      </header>

      <div v-if="shouldShowNotificationPrompt" class="sidebar-panel__notice">
        <button
          class="sidebar-panel__notice-action"
          type="button"
          :aria-label="notificationPromptCopy.title"
          :aria-busy="notificationRequesting"
          @click="requestDesktopNotifications"
        >
          <span class="sidebar-panel__notice-icon" aria-hidden="true">
            <Bell />
          </span>
          <span class="sidebar-panel__notice-copy">
            <strong>{{ notificationPromptCopy.title }}</strong>
            <p>{{ notificationPromptCopy.description }}</p>
          </span>
        </button>
        <button
          class="sidebar-panel__notice-close"
          type="button"
          aria-label="关闭通知提示"
          @click.stop="dismissNotificationPrompt"
        >
          <Close />
        </button>
      </div>

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
          <span>{{ panelEyebrow }}</span>
          <strong>{{ panelTitle }}</strong>
        </div>
        <span class="sidebar-panel__panel-spacer" aria-hidden="true"></span>
      </header>

      <el-scrollbar ref="scrollbarRef" class="sidebar-panel__scroll" @scroll="handleScroll">
        <div v-if="leftPanelMode === 'me'" class="sidebar-detail" data-testid="sidebar-panel-me">
          <div class="sidebar-hero">
            <AvatarBadge
              :name="currentProfile?.nickname ?? currentUser?.nickname"
              :avatar-url="currentProfile?.avatarUrl ?? currentUser?.avatarUrl"
              size="lg"
            />
            <div class="sidebar-hero__copy">
              <strong>{{ currentProfile?.nickname ?? currentUser?.nickname ?? '未登录' }}</strong>
              <p>@{{ currentProfile?.username ?? currentUser?.username ?? 'echo_demo_01' }}</p>
              <span>{{ currentProfile?.signature || '写一段签名，让联系人快速识别你。' }}</span>
            </div>
            <div class="sidebar-hero__actions">
              <el-button type="primary" plain @click="openPanel('settings')">进入设置</el-button>
              <el-button v-if="!editingProfile" data-testid="profile-edit" @click="startProfileEditing">编辑资料</el-button>
            </div>
          </div>

          <div v-if="profileNotice" class="sidebar-notice sidebar-notice--success">{{ profileNotice }}</div>
          <div v-if="profileError" class="sidebar-notice sidebar-notice--error">{{ profileError }}</div>

          <div class="sidebar-section">
            <div class="sidebar-section__head">
              <span>账号信息</span>
              <strong>Current profile</strong>
            </div>
            <div class="info-grid">
              <article>
                <span>账号编号</span>
                <strong>{{ currentProfile?.userNo ?? '加载中' }}</strong>
              </article>
              <article>
                <span>用户名</span>
                <strong>@{{ currentProfile?.username ?? currentUser?.username ?? '加载中' }}</strong>
              </article>
              <article>
                <span>手机号</span>
                <strong>{{ currentProfile?.phone || '未设置' }}</strong>
              </article>
              <article>
                <span>邮箱</span>
                <strong>{{ currentProfile?.email || '未设置' }}</strong>
              </article>
            </div>
          </div>

          <div class="sidebar-section">
            <div class="sidebar-section__head">
              <span>资料编辑</span>
              <strong>Edit identity</strong>
            </div>
            <el-form label-position="top" class="profile-form">
              <el-form-item label="公开用户名">
                <el-input
                  v-model="profileDraft.username"
                  :disabled="!editingProfile || profileSaving"
                  maxlength="24"
                  placeholder="3-24 位字母、数字或下划线"
                />
                <div v-if="editingProfile && (usernameChecking || usernameMessage)" class="profile-form__hint" :class="{ 'is-error': usernameAvailable === false }">
                  {{ usernameChecking ? '正在检查用户名可用性…' : usernameMessage }}
                </div>
              </el-form-item>
              <el-form-item label="昵称">
                <el-input v-model="profileDraft.nickname" :disabled="!editingProfile || profileSaving" maxlength="24" />
              </el-form-item>
              <el-form-item label="头像链接">
                <el-input
                  v-model="profileDraft.avatarUrl"
                  :disabled="!editingProfile || profileSaving"
                  placeholder="https://..."
                />
              </el-form-item>
              <el-form-item label="性别">
                <el-radio-group
                  :model-value="profileDraft.gender ?? 0"
                  :disabled="!editingProfile || profileSaving"
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
                  :disabled="!editingProfile || profileSaving"
                  :autosize="{ minRows: 3, maxRows: 6 }"
                  maxlength="120"
                  show-word-limit
                />
              </el-form-item>
            </el-form>
            <div class="sidebar-actions">
              <template v-if="editingProfile">
                <el-button :disabled="profileSaving" @click="cancelProfileEditing">取消</el-button>
                <el-button
                  type="primary"
                  data-testid="profile-save"
                  :loading="profileSaving"
                  :disabled="!profileDraft.nickname.trim() || !profileDraft.username.trim() || usernameChecking || usernameAvailable === false"
                  @click="submitProfile"
                >
                  保存资料
                </el-button>
              </template>
              <el-button v-else type="primary" plain @click="startProfileEditing">开始编辑</el-button>
            </div>
          </div>

          <ChatStatePanel
            v-if="profileLoading && !currentProfile"
            compact
            title="正在获取个人资料"
            description="已登录信息可用，完整资料正在同步。"
          />
        </div>

        <div v-else class="sidebar-detail" data-testid="sidebar-panel-settings">
          <div class="settings-tabs">
            <button
              class="settings-tabs__item"
              :class="{ 'is-active': settingsSection === 'appearance' }"
              type="button"
              data-testid="settings-tab-appearance"
              @click="updateSettingsSection('appearance')"
            >
              外观
            </button>
            <button
              class="settings-tabs__item"
              :class="{ 'is-active': settingsSection === 'chat' }"
              type="button"
              data-testid="settings-tab-chat"
              @click="updateSettingsSection('chat')"
            >
              聊天偏好
            </button>
            <button
              class="settings-tabs__item"
              :class="{ 'is-active': settingsSection === 'security' }"
              type="button"
              data-testid="settings-tab-security"
              @click="updateSettingsSection('security')"
            >
              账号安全
            </button>
          </div>

          <div v-if="profileNotice" class="sidebar-notice sidebar-notice--success">{{ profileNotice }}</div>
          <div v-if="profileError || passwordForm.localError" class="sidebar-notice sidebar-notice--error">
            {{ passwordForm.localError || profileError }}
          </div>

          <div v-if="settingsSection === 'appearance'" class="sidebar-section">
            <div class="sidebar-section__head">
              <span>主题</span>
              <strong>Appearance</strong>
            </div>
            <div class="theme-card">
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

          <div v-else-if="settingsSection === 'chat'" class="sidebar-section">
            <div class="sidebar-section__head">
              <span>输入与列表</span>
              <strong>Chat preferences</strong>
            </div>
            <div class="settings-list">
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

          <div v-else class="sidebar-section">
            <div class="sidebar-section__head">
              <span>密码</span>
              <strong>Account security</strong>
            </div>
            <div class="security-card">
              <div class="security-card__icon">
                <Lock />
              </div>
              <div>
                <strong>修改登录密码</strong>
                <p>保存后下次登录请使用新密码，当前会话保持不变。</p>
              </div>
            </div>
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
            <div class="sidebar-actions">
              <el-button type="primary" :loading="passwordSaving" @click="submitPassword">更新密码</el-button>
              <el-button plain data-testid="sidebar-logout" @click="handleLogout">退出登录</el-button>
            </div>
          </div>
        </div>
      </el-scrollbar>
    </template>
  </aside>
</template>

<style scoped>
.sidebar-panel {
  position: relative;
  min-height: 0;
  height: 100%;
  display: flex;
  flex-direction: column;
  background:
    linear-gradient(180deg, color-mix(in srgb, var(--color-shell-glow) 36%, transparent), transparent 14%),
    var(--color-shell-panel);
  color: var(--color-text-1);
  overflow: hidden;
  border: 1px solid var(--color-shell-border);
  border-radius: 32px;
  box-shadow: var(--shadow-panel);
  backdrop-filter: blur(24px);
}

.sidebar-panel__header {
  position: relative;
  z-index: 3;
  padding: 18px 18px 12px;
  background: transparent;
}

.sidebar-panel__identity-row {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 12px;
  align-items: center;
  margin-bottom: 14px;
}

.sidebar-panel__identity {
  display: grid;
  grid-template-columns: 42px minmax(0, 1fr);
  gap: 12px;
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
  color: var(--color-text-1);
  font: 700 0.96rem/1.15 var(--font-display);
  letter-spacing: -0.02em;
}

.sidebar-panel__identity-copy span {
  margin-top: 5px;
  color: var(--color-text-soft);
  font: 500 0.72rem/1 var(--font-mono);
}

.sidebar-panel__toolbar-actions {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.sidebar-panel__header--detail {
  display: grid;
  grid-template-columns: 40px minmax(0, 1fr) 40px;
  align-items: center;
  gap: 12px;
  min-height: 72px;
}

.sidebar-panel__toolbar {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 42px;
  align-items: center;
  gap: 12px;
}

.sidebar-panel__folders {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 14px;
}

.sidebar-panel__folder {
  min-width: 68px;
  height: 36px;
  padding: 0 13px;
  border: 1px solid var(--color-shell-border);
  border-radius: 999px;
  background: color-mix(in srgb, var(--color-shell-card-strong) 88%, transparent);
  color: var(--color-text-2);
  font: 600 0.74rem/1 var(--font-body);
  transition:
    background var(--motion-fast) ease,
    color var(--motion-fast) ease,
    border-color var(--motion-fast) ease;
}

.sidebar-panel__folder.is-active {
  background: color-mix(in srgb, var(--color-primary) 14%, var(--color-shell-card-strong));
  border-color: color-mix(in srgb, var(--color-primary) 28%, var(--color-shell-border));
  color: var(--color-text-1);
}

.sidebar-panel__menu-trigger,
.sidebar-panel__back {
  width: 42px;
  height: 42px;
  display: grid;
  place-items: center;
  border: 1px solid var(--color-shell-border);
  border-radius: 16px;
  background: var(--color-shell-action);
  color: var(--color-text-2);
  transition:
    background var(--motion-fast) ease,
    color var(--motion-fast) ease,
    border-color var(--motion-fast) ease,
    transform var(--motion-fast) ease;
}

.sidebar-panel__menu-trigger:hover,
.sidebar-panel__menu-trigger:focus-visible,
.sidebar-panel__back:hover,
.sidebar-panel__back:focus-visible {
  background: var(--color-shell-action-hover);
  border-color: var(--color-shell-border-strong);
  color: var(--color-text-1);
  transform: translateY(-1px);
}

.sidebar-panel__compose-trigger {
  width: 42px;
  height: 42px;
  display: grid;
  place-items: center;
  border: 1px solid color-mix(in srgb, var(--color-primary) 24%, var(--color-shell-border));
  border-radius: 16px;
  background: color-mix(in srgb, var(--color-primary) 10%, var(--color-shell-action));
  color: var(--color-primary-strong);
  transition:
    background var(--motion-fast) ease,
    color var(--motion-fast) ease,
    border-color var(--motion-fast) ease,
    transform var(--motion-fast) ease;
}

.sidebar-panel__compose-trigger:hover,
.sidebar-panel__compose-trigger:focus-visible {
  transform: translateY(-1px);
  border-color: color-mix(in srgb, var(--color-primary) 34%, var(--color-shell-border));
  background: color-mix(in srgb, var(--color-primary) 14%, var(--color-shell-action-hover));
}

.sidebar-panel__search :deep(.el-input__wrapper) {
  min-height: 48px;
  border-radius: 999px;
  background: color-mix(in srgb, var(--color-shell-card-strong) 92%, transparent);
  box-shadow: var(--shadow-card);
}

.sidebar-panel__search :deep(.el-input__inner) {
  color: var(--color-text-1);
}

.sidebar-panel__search :deep(.el-input__prefix),
.sidebar-panel__search :deep(.el-input__suffix) {
  color: var(--color-text-soft);
}

.sidebar-panel__notice {
  margin: 4px 12px 14px;
  position: relative;
  display: grid;
  grid-template-columns: minmax(0, 1fr) 34px;
  align-items: stretch;
  gap: 8px;
  overflow: hidden;
  border-radius: 22px;
  background:
    linear-gradient(180deg, color-mix(in srgb, var(--color-shell-glow) 4%, transparent), transparent 68%),
    linear-gradient(180deg, color-mix(in srgb, var(--color-shell-card-strong) 96%, transparent), var(--color-shell-card));
  color: var(--color-text-1);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.04);
  border: 1px solid var(--color-shell-border);
  transition:
    background var(--motion-base) ease,
    border-color var(--motion-fast) ease,
    box-shadow var(--motion-fast) ease;
}

.sidebar-panel__notice:hover,
.sidebar-panel__notice:focus-within {
  background:
    linear-gradient(180deg, color-mix(in srgb, var(--color-shell-glow) 8%, transparent), transparent 70%),
    linear-gradient(180deg, color-mix(in srgb, var(--color-shell-card-strong) 96%, transparent), var(--color-shell-card));
  border-color: var(--color-shell-border);
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.05),
    var(--shadow-card);
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
  border: 1px solid color-mix(in srgb, var(--color-shell-border) 88%, transparent);
  background:
    radial-gradient(circle at 30% 30%, color-mix(in srgb, #ffffff 8%, transparent), transparent 58%),
    color-mix(in srgb, var(--color-shell-inline) 92%, transparent);
  color: color-mix(in srgb, var(--color-text-2) 88%, var(--color-shell-eyebrow));
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.04);
}

.sidebar-panel__notice-icon :deep(svg) {
  width: 18px;
  height: 18px;
}

.sidebar-panel__notice strong {
  display: block;
  max-width: 100%;
  color: color-mix(in srgb, var(--color-text-1) 96%, white);
  font: 700 0.98rem/1.08 var(--font-display);
  letter-spacing: -0.02em;
  text-wrap: balance;
}

.sidebar-panel__notice p {
  margin-top: 5px;
  color: color-mix(in srgb, var(--color-text-2) 82%, var(--color-text-3));
  font: 400 0.85rem/1.28 var(--font-body);
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
  color: var(--color-text-soft);
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
  background: color-mix(in srgb, var(--color-shell-action-hover) 90%, transparent);
  color: var(--color-text-2);
  transform: translateY(-1px);
}

.sidebar-panel__scroll {
  min-height: 0;
}

.sidebar-panel__scroll--list {
  padding-bottom: 92px;
}

.sidebar-panel__list,
.sidebar-panel__skeletons,
.sidebar-detail {
  display: grid;
}

.sidebar-panel__list {
  gap: 2px;
  padding: 0 10px 0 12px;
}

.sidebar-panel__skeletons,
.sidebar-detail {
  gap: 12px;
  padding: 4px 18px 26px;
}

.sidebar-context-menu {
  position: fixed;
  z-index: 30;
  width: 236px;
  padding: 8px;
  border: 1px solid var(--color-shell-border);
  border-radius: 22px;
  background: color-mix(in srgb, var(--color-shell-card-strong) 94%, rgba(20, 20, 22, 0.12));
  box-shadow: var(--shadow-panel);
  backdrop-filter: blur(22px);
}

.sidebar-context-menu__item {
  width: 100%;
  min-height: 44px;
  display: grid;
  grid-template-columns: 28px minmax(0, 1fr);
  align-items: center;
  gap: 12px;
  padding: 0 12px;
  border: 0;
  border-radius: 14px;
  background: transparent;
  color: var(--color-text-1);
  text-align: left;
  transition:
    background var(--motion-fast) ease,
    color var(--motion-fast) ease;
}

.sidebar-context-menu__item:hover,
.sidebar-context-menu__item:focus-visible {
  background: var(--color-shell-inline);
}

.sidebar-context-menu__item.is-danger {
  color: #ff6e68;
}

.sidebar-context-menu__item.is-danger:hover,
.sidebar-context-menu__item.is-danger:focus-visible {
  background: color-mix(in srgb, var(--color-danger) 10%, var(--color-shell-inline));
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
  font-size: 0.94rem;
  font-weight: 500;
  line-height: 1.2;
}

.sidebar-compose-menu {
  position: absolute;
  right: 14px;
  bottom: 88px;
  z-index: 6;
  width: min(286px, calc(100% - 24px));
  overflow: hidden;
  border: 1px solid var(--color-shell-border);
  border-radius: 24px;
  background: var(--color-shell-card-strong);
  box-shadow: var(--shadow-panel);
}

.sidebar-compose-menu__item {
  width: 100%;
  min-height: 48px;
  display: grid;
  grid-template-columns: 36px minmax(0, 1fr);
  align-items: center;
  gap: 10px;
  padding: 0 16px;
  border: 0;
  background: transparent;
  color: var(--color-text-1);
  text-align: left;
  font-size: 0.9rem;
  font-weight: 600;
}

.sidebar-compose-menu__item:hover,
.sidebar-compose-menu__item:focus-visible {
  background: var(--color-shell-action-hover);
}

.sidebar-compose-menu__icon {
  width: 26px;
  height: 26px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: var(--color-shell-eyebrow);
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
  width: min(340px, calc(100% - 16px));
  display: grid;
  gap: 0;
  overflow: hidden;
  padding: 0;
  border-radius: 24px;
  border: 1px solid var(--color-shell-border);
  background: var(--color-shell-card-strong);
  box-shadow: var(--shadow-panel);
  backdrop-filter: blur(16px);
}

.sidebar-global-menu__identity {
  width: 100%;
  display: grid;
  grid-template-columns: 50px minmax(0, 1fr);
  align-items: center;
  gap: 12px;
  min-height: 72px;
  padding: 8px 18px 8px 16px;
  border: 0;
  background: transparent;
  color: var(--color-text-1);
  text-align: left;
}

.sidebar-global-menu__identity:hover,
.sidebar-global-menu__identity:focus-visible {
  background: var(--color-shell-action-hover);
}

.sidebar-global-menu__identity :deep(.avatar-badge) {
  width: 46px;
  height: 46px;
  border: 2px solid color-mix(in srgb, var(--color-primary) 65%, white);
}

.sidebar-global-menu__copy strong {
  display: block;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
  color: var(--color-text-1);
  font-size: 0.92rem;
  font-weight: 700;
  line-height: 1.18;
}

.sidebar-global-menu__copy p {
  margin-top: 5px;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
  color: var(--color-text-soft);
  font: 500 0.72rem/1 var(--font-mono);
}

.sidebar-global-menu__group {
  display: grid;
  padding: 6px 0;
  border-top: 1px solid var(--color-shell-border);
}

.sidebar-global-menu__item {
  display: grid;
  grid-template-columns: 36px minmax(0, 1fr) auto auto;
  align-items: center;
  gap: 10px;
  min-height: 46px;
  padding: 0 14px 0 18px;
  border: 0;
  border-radius: 0;
  background: transparent;
  color: var(--color-text-1);
  text-align: left;
  transition:
    background var(--motion-fast) ease,
    color var(--motion-fast) ease;
}

.sidebar-global-menu__item:hover,
.sidebar-global-menu__item:focus-visible {
  background: var(--color-shell-action-hover);
}

.sidebar-global-menu__item.is-active {
  background: var(--color-shell-inline);
}

.sidebar-global-menu__icon {
  width: 26px;
  height: 26px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: var(--color-shell-eyebrow);
}

.sidebar-global-menu__icon :deep(svg) {
  width: 21px;
  height: 21px;
}

.sidebar-global-menu__label {
  color: var(--color-text-1);
  font-size: 0.9rem;
  font-weight: 600;
}

.sidebar-global-menu__label small {
  margin-left: 6px;
  color: var(--color-text-soft);
  font-size: 0.72rem;
  font-weight: 600;
}

.sidebar-global-menu__meta {
  color: var(--color-text-soft);
  font: 500 0.68rem/1 var(--font-mono);
}

.sidebar-global-menu__remove {
  color: var(--color-danger);
  font-size: 0.76rem;
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
  color: var(--color-text-soft);
  font-size: 0.76rem;
  line-height: 1.5;
}

.profile-form__hint.is-error {
  color: var(--color-danger);
}

.sidebar-panel__panel-copy span {
  display: block;
  color: var(--color-shell-eyebrow);
  font: var(--font-eyebrow);
  letter-spacing: 0.14em;
  text-transform: uppercase;
  text-align: center;
}

.sidebar-panel__panel-copy strong {
  display: block;
  margin-top: 6px;
  font: var(--font-title-md);
  text-align: center;
}

.sidebar-panel__panel-spacer {
  width: 40px;
  height: 40px;
}

.sidebar-hero,
.sidebar-section {
  display: grid;
  gap: 14px;
  padding: 20px;
  border-radius: 24px;
  background: color-mix(in srgb, var(--color-shell-card-strong) 88%, transparent);
  border: 1px solid var(--color-shell-border);
  box-shadow: var(--shadow-inset-soft);
}

.sidebar-hero__copy strong {
  display: block;
  font: 700 1.02rem/1.1 var(--font-display);
}

.sidebar-hero__copy p {
  margin-top: 5px;
  color: var(--color-text-soft);
  font: 500 0.72rem/1 var(--font-mono);
}

.sidebar-hero__copy span {
  display: block;
  margin-top: 10px;
  color: var(--color-text-2);
  font-size: 0.84rem;
  line-height: 1.45;
}

.sidebar-hero__actions,
.sidebar-actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 8px;
}

.sidebar-section__head span {
  display: block;
  color: var(--color-shell-eyebrow);
  font: var(--font-eyebrow);
  letter-spacing: 0.12em;
  text-transform: uppercase;
}

.sidebar-section__head strong {
  display: block;
  margin-top: 6px;
  font: var(--font-title-sm);
}

.info-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.info-grid article,
.settings-item,
.theme-card__option,
.security-card {
  padding: 16px;
  border: 1px solid var(--color-shell-border);
  border-radius: 18px;
  background: var(--color-shell-card-muted);
}

.info-grid span {
  display: block;
  color: var(--color-text-soft);
  font: 500 0.64rem/1 var(--font-mono);
  text-transform: uppercase;
}

.info-grid strong {
  display: block;
  margin-top: 8px;
  font-size: 0.84rem;
  line-height: 1.35;
}

.profile-form :deep(.el-form-item) {
  margin-bottom: 14px;
}

.settings-tabs {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 8px;
}

.settings-tabs__item {
  min-height: 42px;
  border: 1px solid var(--color-shell-border);
  border-radius: 16px;
  background: var(--color-shell-action);
  color: var(--color-text-2);
  font-weight: 700;
}

.settings-tabs__item.is-active,
.settings-tabs__item:hover,
.settings-tabs__item:focus-visible {
  background: var(--color-shell-inline);
  border-color: var(--color-shell-border-strong);
  color: var(--color-text-1);
}

.theme-card,
.settings-list {
  display: grid;
  gap: 10px;
}

.theme-card__option,
.settings-item,
.security-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  width: 100%;
  color: var(--color-text-1);
  text-align: left;
}

.theme-card__icon,
.security-card__icon {
  width: 48px;
  height: 48px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 18px;
  background: color-mix(in srgb, var(--color-shell-glow) 64%, transparent);
  color: var(--color-shell-eyebrow);
  flex-shrink: 0;
}

.theme-card__copy {
  display: grid;
  gap: 4px;
}

.theme-card__option strong,
.settings-item strong,
.security-card strong {
  display: block;
  font-size: 0.88rem;
}

.theme-card__option p,
.settings-item p,
.security-card p {
  color: var(--color-text-2);
  font-size: 0.78rem;
  line-height: 1.4;
}

.theme-card__option.is-active {
  background: var(--color-shell-inline);
  border-color: var(--color-shell-border-strong);
}

.settings-item {
  align-items: flex-start;
}

.sidebar-notice {
  padding: 13px 15px;
  border-radius: 16px;
  font-size: 0.8rem;
  line-height: 1.42;
}

.sidebar-notice--success {
  background: color-mix(in srgb, var(--color-accent) 12%, var(--color-shell-card-muted));
  color: color-mix(in srgb, var(--color-accent) 80%, white);
}

.sidebar-notice--error {
  background: color-mix(in srgb, var(--color-danger) 12%, var(--color-shell-card-muted));
  color: color-mix(in srgb, var(--color-danger) 78%, white);
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
}
</style>
