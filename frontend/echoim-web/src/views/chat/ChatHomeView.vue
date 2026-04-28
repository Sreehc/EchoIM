<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import ConversationSidebar from '@/components/chat/ConversationSidebar.vue'
import ChatTopbar from '@/components/chat/ChatTopbar.vue'
import MessagePane from '@/components/chat/MessagePane.vue'
import MessageComposer from '@/components/chat/MessageComposer.vue'
import ConversationProfileDrawer from '@/components/chat/ConversationProfileDrawer.vue'
import { useAuthStore } from '@/stores/auth'
import { useChatStore } from '@/stores/chat'
import { useUiStore } from '@/stores/ui'
import type { ChangePasswordPayload, ChatMessage, LeftPanelMode, UpdateCurrentUserProfilePayload } from '@/types/chat'

type ConversationActionCommand = 'open-tab' | 'mark-unread' | 'toggle-top' | 'toggle-mute' | 'archive' | 'delete'

const authStore = useAuthStore()
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

const shouldShowConversationList = computed(() => !uiStore.isMobile || uiStore.mobileView === 'list')
const shouldShowMainPanel = computed(() => !uiStore.isMobile || uiStore.mobileView === 'chat')
const sidebarErrorMessage = computed(() =>
  uiStore.leftPanelMode === 'conversations' && !chatStore.conversations.length ? chatStore.errors.bootstrapError : null,
)
const currentSidebarScrollTop = computed(() => uiStore.panelScrollTop[uiStore.leftPanelMode])
const connectionStatusLabel = computed(() => {
  if (uiStore.connectionStatus === 'ready') return '实时连接已就绪'
  if (uiStore.connectionStatus === 'reconnecting') return '正在重连'
  if (uiStore.connectionStatus === 'connecting') return '正在连接'
  return chatStore.errorMessage ?? '等待建立实时连接'
})
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
  () => chatStore.activeConversationId,
  () => {
    cancelMessageEditing()
    closeMessageSearch()
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

  if (mode === 'conversations' && uiStore.isMobile && !chatStore.activeConversationId) {
    uiStore.setMobileView('list')
  }
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
    chatStore.markConversationUnreadLocally(payload.conversationId)
    if (chatStore.activeConversationId === payload.conversationId) {
      chatStore.clearActiveConversation()
      uiStore.setProfileOpen(false)
      uiStore.setMobileView('list')
      await router.push('/chat')
    }
    return
  }

  if (payload.command === 'archive') {
    chatStore.archiveConversationLocally(payload.conversationId)
    if (chatStore.activeConversationId === payload.conversationId) {
      uiStore.setProfileOpen(false)
      uiStore.setMobileView('list')
      await router.push('/chat')
    }
    return
  }

  await runConversationAction(payload.command, payload.conversationId)
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
    cancelMessageEditing()
    chatStore.resetState()
    uiStore.setGlobalMenuOpen(false)
    uiStore.setTopbarMenuOpen(false)
    uiStore.setProfileOpen(false)
    uiStore.setMobileView('list')
    await router.replace('/login')
  }
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

function handleGlobalKeydown(event: KeyboardEvent) {
  if (event.key !== 'Escape') return
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

function dismissTransientBanner() {
  chatStore.clearNoticeMessage()
  chatStore.clearSendError()
  chatStore.clearSyncError()
}

function registerDebugHooks() {
  if (!e2eHooksEnabled.value) return

  window.__ECHOIM_E2E__ = {
    getConnectionStatus: () => uiStore.connectionStatus,
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
      })),
    openConversation: async (conversationId: number) => {
      await selectConversation(conversationId)
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
    getErrors: () => ({ ...chatStore.errors }),
    getWsEvents: () => [...chatStore.debugEvents],
  }
}
</script>

<template>
  <main class="chat-page">
    <h1 class="chat-page__sr-only">EchoIM 聊天工作台</h1>
    <p class="chat-page__sr-only" role="status" aria-live="polite">{{ liveStatusMessage }}</p>

    <ConversationSidebar
      v-if="shouldShowConversationList"
      class="chat-page__sidebar"
      :current-user="authStore.currentUser"
      :current-profile="authStore.profile"
      :conversations="chatStore.filteredConversations"
      :selected-conversation-id="chatStore.activeConversationId"
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
      :profile-error="authStore.profileError"
      :profile-notice="authStore.profileNotice"
      @update:search-query="chatStore.setSearchQuery"
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
      @change-password="changePassword"
      @clear-profile-error="authStore.clearProfileError"
      @clear-profile-notice="authStore.clearProfileNotice"
      @conversation-action="handleConversationContextAction"
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
          @update:message-search-query="messageSearchQuery = $event"
          @action="handleConversationAction"
          @update:menu-open="uiStore.setTopbarMenuOpen"
          @open-profile="openProfile"
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
            @retry="chatStore.loadConversationMessages(chatStore.activeConversation.conversationId, true)"
            @load-older="handleLoadOlderMessages"
            @retry-message="chatStore.retryMessage"
            @update:search-match-count="handleMessageSearchCountUpdate"
            @start-edit-message="startEditingMessage"
            @update:editing-draft="editingMessageDraft = $event"
            @cancel-edit-message="cancelMessageEditing"
            @save-edit-message="saveEditingMessage"
            @recall-message="handleRecallMessage"
          />
          <MessageComposer
            :enter-to-send="uiStore.chatPreferences.enterToSend"
            :can-send="chatStore.activeConversation.canSend"
            :disabled-reason="
              chatStore.activeConversation.conversationType === 3
                ? '仅频道创建者可发送消息'
                : '当前会话暂不可发送消息'
            "
            @send="chatStore.sendMessage({ currentUserId: authStore.currentUser?.userId ?? 0, content: $event })"
          />
        </div>
      </template>

      <div v-else class="chat-page__empty" data-testid="chat-empty-state"></div>
    </section>

    <ConversationProfileDrawer
      v-if="chatStore.activeConversation && uiStore.profileOpen"
      class="chat-page__profile"
      :conversation="chatStore.activeConversation"
      :profile="chatStore.activeProfile"
      :loading="chatStore.activeProfileLoading"
      :error-message="chatStore.activeProfileError"
      :overlay="true"
      :visible="uiStore.profileOpen"
      @action="handleConversationAction"
      @update:visible="uiStore.setProfileOpen"
    />
  </main>
</template>

<style scoped>
.chat-page {
  display: grid;
  grid-template-columns: 388px minmax(0, 1fr);
  gap: 10px;
  height: 100%;
  min-height: 0;
  overflow: hidden;
}

.chat-page__sidebar,
.chat-page__main,
.chat-page__profile {
  min-height: 0;
  overflow: hidden;
}

.chat-page__main {
  display: flex;
  flex-direction: column;
  min-height: 0;
  border: 1px solid var(--color-shell-border);
  border-radius: 28px;
  background: var(--color-shell-panel);
  box-shadow: var(--shadow-panel);
  overflow: hidden;
}

.chat-page__stage {
  position: relative;
  display: flex;
  flex: 1;
  min-height: 0;
  flex-direction: column;
  overflow: hidden;
  background: var(--color-chat-stage-base);
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
    linear-gradient(180deg, var(--color-chat-stage-top), transparent 18%),
    linear-gradient(0deg, var(--color-chat-stage-bottom), transparent 26%),
    radial-gradient(circle at top right, var(--color-chat-stage-glow), transparent 38%);
}

.chat-page__banner {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 9px 18px;
  border-bottom: 1px solid var(--color-shell-border);
  color: var(--color-text-2);
  font-size: 0.78rem;
  background: color-mix(in srgb, var(--color-shell-card-strong) 92%, transparent);
}

.chat-page__banner.is-warning {
  background: color-mix(in srgb, var(--color-warning) 9%, var(--color-shell-card));
}

.chat-page__banner.is-error {
  background: color-mix(in srgb, var(--color-danger) 9%, var(--color-shell-card));
}

.chat-page__banner.is-muted {
  background: color-mix(in srgb, var(--color-primary) 6%, var(--color-shell-card));
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
    linear-gradient(180deg, rgba(10, 10, 16, 0.18), transparent 16%),
    var(--color-chat-stage-base);
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
    linear-gradient(180deg, var(--color-chat-stage-top), transparent 18%),
    linear-gradient(0deg, var(--color-chat-stage-bottom), transparent 24%);
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

@media (max-width: 1279px) {
  .chat-page {
    grid-template-columns: 348px minmax(0, 1fr);
  }
}

@media (max-width: 767px) {
  .chat-page {
    grid-template-columns: 1fr;
    gap: 0;
  }

  .chat-page__sidebar,
  .chat-page__main {
    border-inline: 0;
    border-radius: 0;
  }

}
</style>
