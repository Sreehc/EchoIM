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
  EditPen,
  Guide,
  Lock,
  Moon,
  MoreFilled,
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
  ConversationSummary,
  CurrentUserProfile,
  LeftPanelMode,
  SettingsSection,
  ThemeMode,
  UpdateCurrentUserProfilePayload,
  UserInfo,
} from '@/types/chat'
import { readBrowserNotificationPermission } from '@/utils/browserNotifications'
import { STORAGE_KEYS } from '@/utils/storage'
import ConversationListItem from './ConversationListItem.vue'
import AvatarBadge from './AvatarBadge.vue'
import ChatStatePanel from './ChatStatePanel.vue'

const props = defineProps<{
  currentUser: UserInfo | null
  currentProfile: CurrentUserProfile | null
  conversations: ConversationSummary[]
  selectedConversationId: number | null
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
  'save-profile': [payload: UpdateCurrentUserProfilePayload]
  'change-password': [payload: ChangePasswordPayload]
  'clear-profile-error': []
  'clear-profile-notice': []
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
  nickname: string
  avatarUrl: string
  gender: number | null
  signature: string
}>({
  nickname: '',
  avatarUrl: '',
  gender: null,
  signature: '',
})
let searchTimer: number | null = null

const panelTitle = computed(() => {
  if (props.leftPanelMode === 'me') return '个人中心'
  if (props.leftPanelMode === 'settings') return '设置'
  return '会话'
})

const panelEyebrow = computed(() => {
  if (props.leftPanelMode === 'me') return 'Echo account'
  if (props.leftPanelMode === 'settings') return 'Workspace settings'
  return 'Workspace panel'
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
    profileDraft.avatarUrl = value?.avatarUrl ?? props.currentUser?.avatarUrl ?? ''
    profileDraft.gender = value?.gender ?? null
    profileDraft.signature = value?.signature ?? ''
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

onMounted(() => {
  document.addEventListener('pointerdown', handleDocumentPointerDown)
})

onUnmounted(() => {
  if (searchTimer) {
    window.clearTimeout(searchTimer)
  }
  document.removeEventListener('pointerdown', handleDocumentPointerDown)
})

function handleScroll({ scrollTop }: { scrollTop: number }) {
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

function handlePlaceholderAction() {
  closeGlobalMenu()
  closeComposeMenu()
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
  profileDraft.nickname = props.currentProfile?.nickname ?? props.currentUser?.nickname ?? ''
  profileDraft.avatarUrl = props.currentProfile?.avatarUrl ?? props.currentUser?.avatarUrl ?? ''
  profileDraft.gender = props.currentProfile?.gender ?? null
  profileDraft.signature = props.currentProfile?.signature ?? ''
}

function submitProfile() {
  emit('save-profile', {
    nickname: profileDraft.nickname.trim(),
    avatarUrl: profileDraft.avatarUrl.trim() || null,
    gender: profileDraft.gender,
    signature: profileDraft.signature.trim() || null,
  })
  editingProfile.value = false
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
        <div class="sidebar-panel__toolbar">
          <button
            class="sidebar-panel__menu-trigger"
            type="button"
            aria-label="打开工作区菜单"
            data-testid="sidebar-open-menu"
            @click="toggleGlobalMenu"
          >
            <span class="sidebar-panel__menu-bars" aria-hidden="true">
              <span></span>
              <span></span>
              <span></span>
            </span>
          </button>
          <div class="sidebar-panel__search">
            <el-input
              ref="searchInput"
              :model-value="draftSearchQuery"
              :prefix-icon="Search"
              placeholder="Search"
              aria-label="搜索会话"
              clearable
              @update:model-value="handleSearchInput"
              @clear="clearSearch"
            />
          </div>
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
              <button class="sidebar-global-menu__item" type="button" role="menuitem" @click="handlePlaceholderAction">
                <span class="sidebar-global-menu__icon"><Plus /></span>
                <span class="sidebar-global-menu__label">添加账号</span>
              </button>
            </div>

            <div class="sidebar-global-menu__group">
              <button class="sidebar-global-menu__item" type="button" role="menuitem" @click="handlePlaceholderAction">
                <span class="sidebar-global-menu__icon"><Collection /></span>
                <span class="sidebar-global-menu__label">收藏消息</span>
              </button>
              <button class="sidebar-global-menu__item" type="button" role="menuitem" @click="handlePlaceholderAction">
                <span class="sidebar-global-menu__icon"><CirclePlus /></span>
                <span class="sidebar-global-menu__label">我的动态</span>
              </button>
              <button class="sidebar-global-menu__item" type="button" role="menuitem" @click="handlePlaceholderAction">
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
          <span class="sidebar-panel__notice-copy">
            <strong>
              {{ notificationPromptCopy.title }}
              <Bell aria-hidden="true" />
            </strong>
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
          title="这里还没有会话"
          description="新的单聊或群聊会在这里按照最近消息时间排序展示。"
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
          <button class="sidebar-compose-menu__item" type="button" role="menuitem" @click="handlePlaceholderAction">
            <span class="sidebar-compose-menu__icon"><Guide /></span>
            <span>新建频道</span>
          </button>
          <button class="sidebar-compose-menu__item" type="button" role="menuitem" @click="handlePlaceholderAction">
            <span class="sidebar-compose-menu__icon"><UserFilled /></span>
            <span>新建群组</span>
          </button>
          <button class="sidebar-compose-menu__item" type="button" role="menuitem" @click="handlePlaceholderAction">
            <span class="sidebar-compose-menu__icon"><ChatRound /></span>
            <span>新建私聊</span>
          </button>
        </div>
      </transition>

      <button
        class="sidebar-panel__fab"
        type="button"
        aria-label="打开新建菜单"
        aria-haspopup="menu"
        :aria-expanded="composeMenuOpen"
        data-testid="sidebar-open-compose"
        @click="toggleComposeMenu"
      >
        <EditPen />
      </button>
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
                <strong>{{ currentProfile?.username ?? currentUser?.username ?? '加载中' }}</strong>
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
                  :disabled="!profileDraft.nickname.trim()"
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
  background: var(--color-sidebar-bg);
  color: var(--color-text-1);
  overflow: hidden;
}

.sidebar-panel__header {
  position: relative;
  z-index: 3;
  padding: 18px 18px 10px;
  background: var(--color-sidebar-bg);
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
  grid-template-columns: 40px minmax(0, 1fr);
  align-items: center;
  gap: 12px;
}

.sidebar-panel__menu-trigger,
.sidebar-panel__back {
  width: 40px;
  height: 40px;
  display: grid;
  place-items: center;
  border: 0;
  border-radius: 50%;
  background: transparent;
  color: var(--color-text-2);
  transition:
    background var(--motion-fast) ease,
    color var(--motion-fast) ease;
}

.sidebar-panel__menu-trigger:hover,
.sidebar-panel__menu-trigger:focus-visible,
.sidebar-panel__back:hover,
.sidebar-panel__back:focus-visible {
  background: var(--color-hover);
  color: var(--color-text-1);
}

.sidebar-panel__menu-bars {
  display: inline-grid;
  gap: 4px;
}

.sidebar-panel__menu-bars span {
  width: 18px;
  height: 2px;
  border-radius: 999px;
  background: currentColor;
}

.sidebar-panel__search :deep(.el-input__wrapper) {
  min-height: 44px;
  border: 0;
  border-radius: 22px;
  background: var(--color-sidebar-muted);
  box-shadow: none;
}

.sidebar-panel__search :deep(.el-input__inner) {
  color: var(--color-text-1);
}

.sidebar-panel__search :deep(.el-input__prefix),
.sidebar-panel__search :deep(.el-input__suffix) {
  color: var(--color-text-soft);
}

.sidebar-panel__notice {
  margin: 0 18px 10px;
  display: grid;
  grid-template-columns: minmax(0, 1fr) 36px;
  gap: 0;
  min-height: 92px;
  overflow: hidden;
  border-radius: 16px;
  background: #303030;
  color: #f4f5f7;
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.035),
    0 10px 24px rgba(0, 0, 0, 0.14);
}

.sidebar-panel__notice-action,
.sidebar-panel__notice-close {
  border: 0;
  background: transparent;
  color: inherit;
}

.sidebar-panel__notice-action {
  width: 100%;
  padding: 15px 0 15px 18px;
  text-align: left;
  transition: background var(--motion-fast) ease;
}

.sidebar-panel__notice-action:hover,
.sidebar-panel__notice-action:focus-visible {
  background: rgba(255, 255, 255, 0.04);
}

.sidebar-panel__notice-copy {
  display: block;
  min-width: 0;
}

.sidebar-panel__notice strong {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  max-width: 100%;
  color: #ffffff;
  font-size: 0.92rem;
  font-weight: 700;
  line-height: 1.2;
  letter-spacing: -0.015em;
}

.sidebar-panel__notice strong :deep(svg) {
  width: 16px;
  height: 16px;
  color: #ffd35c;
  flex: 0 0 auto;
}

.sidebar-panel__notice p {
  margin-top: 8px;
  color: #bdc1cb;
  font-size: 0.8rem;
  line-height: 1.42;
}

.sidebar-panel__notice-close {
  width: 34px;
  height: 34px;
  margin: 8px 6px 0 0;
  display: grid;
  place-items: center;
  align-self: start;
  border-radius: 50%;
  color: #aeb3bd;
  transition:
    background var(--motion-fast) ease,
    color var(--motion-fast) ease;
}

.sidebar-panel__notice-close :deep(svg) {
  width: 18px;
  height: 18px;
}

.sidebar-panel__notice-close:hover,
.sidebar-panel__notice-close:focus-visible {
  background: rgba(255, 255, 255, 0.08);
  color: #ffffff;
}

.sidebar-panel__scroll {
  min-height: 0;
}

.sidebar-panel__scroll--list {
  padding-bottom: 82px;
}

.sidebar-panel__list,
.sidebar-panel__skeletons,
.sidebar-detail {
  display: grid;
}

.sidebar-panel__list {
  gap: 2px;
  padding: 0 8px 0 10px;
}

.sidebar-panel__skeletons,
.sidebar-detail {
  gap: 12px;
  padding: 4px 18px 24px;
}

.sidebar-panel__fab {
  position: absolute;
  right: 22px;
  bottom: 22px;
  z-index: 5;
  width: 58px;
  height: 58px;
  display: grid;
  place-items: center;
  border: 0;
  border-radius: 50%;
  background: var(--color-primary);
  color: #fff;
  box-shadow: 0 14px 30px rgba(135, 116, 225, 0.36);
}

.sidebar-panel__fab :deep(svg) {
  width: 22px;
  height: 22px;
}

.sidebar-compose-menu {
  position: absolute;
  right: 12px;
  bottom: 88px;
  z-index: 6;
  width: min(286px, calc(100% - 24px));
  overflow: hidden;
  border: 1px solid rgba(255, 255, 255, 0.05);
  border-radius: 4px;
  background: #242424;
  box-shadow: 0 18px 38px rgba(0, 0, 0, 0.36);
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
  color: #f5f6f8;
  text-align: left;
  font-size: 0.9rem;
  font-weight: 600;
}

.sidebar-compose-menu__item:hover,
.sidebar-compose-menu__item:focus-visible {
  background: #333333;
}

.sidebar-compose-menu__icon {
  width: 26px;
  height: 26px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: #f5f6f8;
}

.sidebar-compose-menu__icon :deep(svg) {
  width: 21px;
  height: 21px;
}

.sidebar-global-menu {
  position: absolute;
  top: 8px;
  left: 8px;
  z-index: 10;
  width: min(360px, calc(100% - 16px));
  display: grid;
  gap: 0;
  overflow: hidden;
  padding: 0;
  border-radius: 4px;
  border: 1px solid rgba(255, 255, 255, 0.05);
  background: #232323;
  box-shadow: 0 18px 44px rgba(0, 0, 0, 0.42);
}

.sidebar-global-menu__identity {
  width: 100%;
  display: grid;
  grid-template-columns: 50px minmax(0, 1fr);
  align-items: center;
  gap: 12px;
  min-height: 64px;
  padding: 8px 18px 8px 16px;
  border: 0;
  background: transparent;
  color: #f5f6f8;
  text-align: left;
}

.sidebar-global-menu__identity:hover,
.sidebar-global-menu__identity:focus-visible {
  background: #313131;
}

.sidebar-global-menu__identity :deep(.avatar-badge) {
  width: 46px;
  height: 46px;
  border: 2px solid #8067ff;
}

.sidebar-global-menu__copy strong {
  display: block;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
  color: #f5f6f8;
  font-size: 0.92rem;
  font-weight: 700;
  line-height: 1.18;
}

.sidebar-global-menu__copy p {
  margin-top: 5px;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
  color: #aeb4bf;
  font: 500 0.72rem/1 var(--font-mono);
}

.sidebar-global-menu__group {
  display: grid;
  padding: 5px 0;
  border-top: 1px solid rgba(255, 255, 255, 0.07);
}

.sidebar-global-menu__item {
  display: grid;
  grid-template-columns: 36px minmax(0, 1fr) 18px;
  align-items: center;
  gap: 10px;
  min-height: 44px;
  padding: 0 14px 0 18px;
  border: 0;
  border-radius: 0;
  background: transparent;
  color: #f3f4f6;
  text-align: left;
  transition:
    background var(--motion-fast) ease,
    color var(--motion-fast) ease;
}

.sidebar-global-menu__item:hover,
.sidebar-global-menu__item:focus-visible {
  background: #333333;
}

.sidebar-global-menu__item.is-active {
  background: #333333;
}

.sidebar-global-menu__icon {
  width: 26px;
  height: 26px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: #f5f6f8;
}

.sidebar-global-menu__icon :deep(svg) {
  width: 21px;
  height: 21px;
}

.sidebar-global-menu__label {
  color: #f5f6f8;
  font-size: 0.9rem;
  font-weight: 600;
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

.sidebar-panel__panel-copy span {
  display: block;
  color: var(--color-text-soft);
  font: 500 0.66rem/1 var(--font-mono);
  letter-spacing: 0.12em;
  text-transform: uppercase;
  text-align: center;
}

.sidebar-panel__panel-copy strong {
  display: block;
  margin-top: 6px;
  font: 700 1rem/1.1 var(--font-display);
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
  padding: 18px;
  border-radius: 18px;
  background: var(--color-sidebar-surface);
  border: 1px solid var(--color-line);
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
  color: var(--color-text-soft);
  font: 500 0.64rem/1 var(--font-mono);
  letter-spacing: 0.12em;
  text-transform: uppercase;
}

.sidebar-section__head strong {
  display: block;
  margin-top: 6px;
  font: 700 0.98rem/1.1 var(--font-display);
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
  padding: 14px;
  border: 1px solid var(--color-line);
  border-radius: 16px;
  background: var(--color-sidebar-muted);
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
  min-height: 40px;
  border: 1px solid var(--color-line);
  border-radius: 12px;
  background: var(--color-sidebar-surface);
  color: var(--color-text-2);
  font-weight: 700;
}

.settings-tabs__item.is-active,
.settings-tabs__item:hover,
.settings-tabs__item:focus-visible {
  background: var(--color-sidebar-pill);
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
  border-radius: 50%;
  background: rgba(135, 116, 225, 0.12);
  color: var(--color-primary);
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
  background: rgba(135, 116, 225, 0.12);
  border-color: rgba(135, 116, 225, 0.22);
}

.settings-item {
  align-items: flex-start;
}

.sidebar-notice {
  padding: 12px 14px;
  border-radius: 14px;
  font-size: 0.8rem;
  line-height: 1.42;
}

.sidebar-notice--success {
  background: rgba(52, 211, 153, 0.14);
  color: #78f3c4;
}

.sidebar-notice--error {
  background: rgba(248, 113, 113, 0.14);
  color: #ffb0b0;
}

@media (max-width: 767px) {
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
