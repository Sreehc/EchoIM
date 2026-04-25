<script setup lang="ts">
import { computed, nextTick, onMounted, onUnmounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import ConversationSidebar from '@/components/chat/ConversationSidebar.vue'
import ChatTopbar from '@/components/chat/ChatTopbar.vue'
import MessagePane from '@/components/chat/MessagePane.vue'
import MessageComposer from '@/components/chat/MessageComposer.vue'
import ConversationProfileDrawer from '@/components/chat/ConversationProfileDrawer.vue'
import ChatStatePanel from '@/components/chat/ChatStatePanel.vue'
import { useAuthStore } from '@/stores/auth'
import { useChatStore } from '@/stores/chat'
import { useUiStore } from '@/stores/ui'

const authStore = useAuthStore()
const chatStore = useChatStore()
const uiStore = useUiStore()
const route = useRoute()
const router = useRouter()

const shouldShowConversationList = computed(() => !uiStore.isMobile || uiStore.mobileView === 'list')
const showDesktopProfile = computed(() => uiStore.isDesktop && uiStore.profileOpen)
const shouldShowMainPanel = computed(() => !uiStore.isMobile || uiStore.mobileView === 'chat')
const shouldShowWelcomeState = computed(() => !chatStore.activeConversation && !uiStore.isMobile)
const sidebarErrorMessage = computed(() => (chatStore.conversations.length ? null : chatStore.errors.bootstrapError))
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
    } catch (error) {
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

async function selectConversation(conversationId: number) {
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
  uiStore.setTopbarMenuOpen(false)
  uiStore.setProfileOpen(true)
}

async function handleConversationAction(command: 'toggle-top' | 'toggle-mute' | 'mark-read') {
  const conversationId = chatStore.activeConversationId
  if (!conversationId) return

  try {
    if (command === 'toggle-top') {
      await chatStore.toggleConversationTop(conversationId)
    } else if (command === 'toggle-mute') {
      await chatStore.toggleConversationMute(conversationId)
    } else {
      await chatStore.markConversationRead(conversationId)
    }
  } catch {
    return
  }

  uiStore.setTopbarMenuOpen(false)
}

function handleFocusSearch() {
  uiStore.setTopbarMenuOpen(false)

  if (uiStore.isMobile && chatStore.activeConversationId) {
    chatStore.clearActiveConversation()
    uiStore.setMobileView('list')
    router.push('/chat')
  }

  nextTick(() => {
    uiStore.requestSidebarSearchFocus()
  })
}

async function handleRetry() {
  try {
    await chatStore.bootstrap(true)
  } catch {
    return
  }
}

function handleGlobalKeydown(event: KeyboardEvent) {
  if (event.key !== 'Escape') return
  if (uiStore.closeFloatingUi()) {
    event.preventDefault()
  }
}

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
  <main class="chat-page" :class="{ 'chat-page--with-profile': showDesktopProfile }">
    <h1 class="chat-page__sr-only">EchoIM 聊天工作台</h1>
    <p class="chat-page__sr-only" role="status" aria-live="polite">{{ liveStatusMessage }}</p>
    <ConversationSidebar
      v-if="shouldShowConversationList"
      class="chat-page__sidebar"
      :current-user="authStore.currentUser"
      :conversations="chatStore.filteredConversations"
      :selected-conversation-id="chatStore.activeConversationId ?? chatStore.lastOpenedConversationId"
      :search-query="chatStore.searchQuery"
      :theme="uiStore.theme"
      :loading="chatStore.loading"
      :error-message="sidebarErrorMessage"
      :focus-search-token="uiStore.sidebarSearchFocusToken"
      :sidebar-scroll-top="uiStore.sidebarScrollTop"
      @update:search-query="chatStore.setSearchQuery"
      @update:sidebar-scroll-top="uiStore.setSidebarScrollTop"
      @clear-search="chatStore.clearSearchQuery"
      @retry="handleRetry"
      @select="selectConversation"
      @toggle-theme="uiStore.toggleTheme"
    />

    <section v-if="shouldShowMainPanel" class="chat-page__main">
      <template v-if="chatStore.activeConversation">
        <ChatTopbar
          :conversation="chatStore.activeConversation"
          :profile="chatStore.activeProfile"
          :is-mobile="uiStore.isMobile"
          :menu-open="uiStore.topbarMenuOpen"
          @back="handleBack"
          @focus-search="handleFocusSearch"
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
        <MessagePane
          :conversation-id="chatStore.activeConversation.conversationId"
          :messages="chatStore.activeMessages"
          :current-user-id="authStore.currentUser?.userId ?? 0"
          :current-user-name="authStore.currentUser?.nickname ?? '我'"
          :conversation-name="chatStore.activeConversation.conversationName"
          :conversation-type="chatStore.activeConversation.conversationType"
          :loading="chatStore.messagesLoading"
          :error-message="chatStore.errors.messageLoadError"
          @retry="chatStore.loadConversationMessages(chatStore.activeConversation.conversationId, true)"
          @retry-message="chatStore.retryMessage"
        />
        <MessageComposer
          @send="chatStore.sendMessage({ currentUserId: authStore.currentUser?.userId ?? 0, content: $event })"
        />
      </template>

      <div v-else-if="shouldShowWelcomeState" class="chat-page__welcome">
        <div class="chat-page__welcome-card">
          <ChatStatePanel
            eyebrow="EchoIM"
            title="选择一个会话开始"
            description="从左侧列表打开最近会话。置顶会话会始终保持在最上方，便于快速进入。"
            action-label="聚焦搜索"
            role="status"
            aria-live="polite"
            @action="handleFocusSearch"
          />
          <dl class="chat-page__welcome-meta">
            <div>
              <dt>当前账号</dt>
              <dd>{{ authStore.currentUser?.nickname ?? authStore.currentUser?.username ?? '未登录' }}</dd>
            </div>
            <div>
              <dt>同步状态</dt>
              <dd>{{ connectionStatusLabel }}</dd>
            </div>
          </dl>
        </div>
      </div>
    </section>

    <ConversationProfileDrawer
      v-if="chatStore.activeConversation && (uiStore.useOverlayProfile || showDesktopProfile)"
      class="chat-page__profile"
      :conversation="chatStore.activeConversation"
      :profile="chatStore.activeProfile"
      :overlay="uiStore.useOverlayProfile"
      :visible="uiStore.profileOpen"
      @action="handleConversationAction"
      @update:visible="uiStore.setProfileOpen"
    />
  </main>
</template>

<style scoped>
.chat-page {
  display: grid;
  grid-template-columns: 280px minmax(0, 1fr);
  gap: 14px;
  min-height: calc(100dvh - 24px);
}

.chat-page--with-profile {
  grid-template-columns: 280px minmax(0, 1fr) 320px;
}

.chat-page__sidebar,
.chat-page__main,
.chat-page__profile {
  min-height: 0;
}

.chat-page__main {
  display: grid;
  grid-template-rows: auto minmax(0, 1fr) auto;
  border: 1px solid var(--color-line);
  border-radius: 16px;
  background: var(--color-bg-surface);
  box-shadow: var(--shadow-soft);
  overflow: hidden;
}

.chat-page__banner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 9px 14px;
  border-bottom: 1px solid var(--color-line);
  color: var(--color-text-2);
  font-size: 0.82rem;
}

.chat-page__banner.is-warning {
  background: color-mix(in srgb, var(--color-warning) 9%, var(--color-bg-surface));
}

.chat-page__banner.is-error {
  background: color-mix(in srgb, var(--color-danger) 8%, var(--color-bg-surface));
}

.chat-page__banner.is-muted {
  background: var(--color-bg-elevated);
}

.chat-page__banner button {
  padding: 0;
  border: 0;
  background: transparent;
  color: inherit;
  font-size: 0.76rem;
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

.chat-page__profile {
  width: 320px;
}

.chat-page__welcome {
  min-height: 0;
  display: grid;
  place-items: center;
  padding: 24px;
  background:
    radial-gradient(circle at top left, color-mix(in srgb, var(--color-primary) 9%, transparent), transparent 30%),
    var(--color-bg-surface);
}

.chat-page__welcome-card {
  width: min(100%, 420px);
  padding: 28px 28px 24px;
  border: 1px solid var(--color-line);
  border-radius: 20px;
  background: color-mix(in srgb, var(--color-bg-elevated) 92%, var(--color-bg-surface));
  box-shadow: var(--shadow-soft);
}

.chat-page__welcome-eyebrow {
  display: inline-block;
  margin-bottom: 14px;
  color: var(--color-text-soft);
  font: 600 0.72rem/1 var(--font-mono);
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.chat-page__welcome-card h1 {
  margin: 0 0 10px;
  font: 700 1.4rem/1.08 var(--font-display);
  letter-spacing: -0.03em;
}

.chat-page__welcome-card p {
  color: var(--color-text-2);
  font-size: 0.92rem;
}

.chat-page__welcome-meta {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
  margin: 18px 0 0;
  padding-top: 16px;
  border-top: 1px solid var(--color-line);
}

.chat-page__welcome-meta dt {
  margin-bottom: 4px;
  color: var(--color-text-soft);
  font: 500 0.68rem/1 var(--font-mono);
  text-transform: uppercase;
}

.chat-page__welcome-meta dd {
  margin: 0;
  color: var(--color-text-1);
  font-size: 0.9rem;
  font-weight: 600;
}

@media (max-width: 767px) {
  .chat-page {
    grid-template-columns: 1fr;
    gap: 0;
    min-height: 100dvh;
  }

  .chat-page__main {
    min-height: 100dvh;
    border-radius: 0;
    border-inline: 0;
    box-shadow: none;
  }
}
</style>
