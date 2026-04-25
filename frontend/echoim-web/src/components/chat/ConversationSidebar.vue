<script setup lang="ts">
import { nextTick, onUnmounted, ref, watch } from 'vue'
import { Moon, Search, Sunny } from '@element-plus/icons-vue'
import type { ConversationSummary, UserInfo } from '@/types/chat'
import ConversationListItem from './ConversationListItem.vue'
import AvatarBadge from './AvatarBadge.vue'
import ChatStatePanel from './ChatStatePanel.vue'

const props = defineProps<{
  currentUser: UserInfo | null
  conversations: ConversationSummary[]
  selectedConversationId: number | null
  searchQuery: string
  theme: 'light' | 'dark'
  loading?: boolean
  errorMessage?: string | null
  focusSearchToken?: number
  sidebarScrollTop?: number
}>()

const emit = defineEmits<{
  'update:searchQuery': [value: string]
  select: [conversationId: number]
  toggleTheme: []
  clearSearch: []
  retry: []
  'update:sidebarScrollTop': [value: number]
}>()

const searchInput = ref()
const scrollbarRef = ref()
const draftSearchQuery = ref(props.searchQuery)
let searchTimer: number | null = null

watch(
  () => props.focusSearchToken,
  async () => {
    await nextTick()
    searchInput.value?.focus?.()
  },
)

watch(
  () => props.sidebarScrollTop,
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
    if (!value) return
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

onUnmounted(() => {
  if (searchTimer) {
    window.clearTimeout(searchTimer)
  }
})

function handleScroll({ scrollTop }: { scrollTop: number }) {
  emit('update:sidebarScrollTop', scrollTop)
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
</script>

<template>
  <aside class="sidebar-panel">
    <header class="sidebar-panel__header">
      <div class="profile-card">
        <AvatarBadge
          class="profile-card__avatar"
          :name="currentUser?.nickname"
          :avatar-url="currentUser?.avatarUrl"
          size="md"
        />
        <div class="profile-card__identity">
          <strong>{{ currentUser?.nickname ?? '未登录' }}</strong>
          <p>@{{ currentUser?.username ?? 'echo_demo_01' }}</p>
        </div>
      </div>
      <button class="sidebar-panel__theme" type="button" aria-label="切换主题" @click="emit('toggleTheme')">
        <Sunny v-if="theme === 'light'" />
        <Moon v-else />
      </button>
    </header>

    <div class="sidebar-panel__controls">
      <div class="sidebar-panel__search">
        <el-input
          ref="searchInput"
          :model-value="draftSearchQuery"
          :prefix-icon="Search"
          placeholder="搜索会话"
          aria-label="搜索会话"
          clearable
          @update:model-value="handleSearchInput"
          @clear="clearSearch"
        />
      </div>
    </div>

    <el-scrollbar ref="scrollbarRef" class="sidebar-panel__scroll" @scroll="handleScroll">
      <div v-if="loading" class="sidebar-panel__skeletons">
        <el-skeleton v-for="item in 6" :key="item" animated :rows="2" />
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
      <div v-else-if="conversations.length" class="sidebar-panel__list">
        <ConversationListItem
          v-for="conversation in conversations"
          :key="conversation.conversationId"
          :item="conversation"
          :active="selectedConversationId === conversation.conversationId"
          :search-query="searchQuery"
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
  </aside>
</template>

<style scoped>
.sidebar-panel {
  min-height: 0;
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 10px;
  border: 1px solid var(--color-line);
  border-radius: 16px;
  background: var(--color-bg-surface);
  box-shadow: var(--shadow-soft);
}

.sidebar-panel__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  min-height: 42px;
  padding: 2px 2px 4px;
}

.profile-card {
  display: flex;
  align-items: center;
  gap: 9px;
  min-width: 0;
}

.profile-card__avatar {
  flex-shrink: 0;
}

.profile-card__identity {
  min-width: 0;
}

.profile-card strong {
  display: block;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
  font-size: 0.86rem;
  line-height: 1.1;
  font-weight: 600;
}

.profile-card p {
  margin-top: 2px;
  color: var(--color-text-soft);
  font: 500 0.68rem/1 var(--font-mono);
}

.sidebar-panel__theme {
  width: 32px;
  height: 32px;
  display: grid;
  place-items: center;
  border-radius: 10px;
  border: 1px solid transparent;
  background: transparent;
  color: var(--color-text-2);
}

.sidebar-panel__theme:hover,
.sidebar-panel__theme:focus-visible {
  background: var(--color-hover);
  border-color: var(--color-line);
  color: var(--color-text-1);
}

.sidebar-panel__controls {
  display: grid;
  gap: 8px;
}

.sidebar-panel__search :deep(.el-input__wrapper) {
  min-height: 38px;
  padding-inline: 10px;
  border: 1px solid var(--color-line);
  background: var(--color-bg-elevated);
}

.sidebar-panel__search :deep(.el-input__suffix) {
  cursor: pointer;
}

.sidebar-panel__scroll {
  min-height: 0;
  margin-top: 2px;
}

.sidebar-panel__list,
.sidebar-panel__skeletons {
  display: grid;
  gap: 2px;
}
</style>
