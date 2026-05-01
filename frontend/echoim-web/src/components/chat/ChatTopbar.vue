<script setup lang="ts">
import { computed, nextTick, ref, watch } from 'vue'
import { ArrowDown, ArrowLeft, ArrowUp, Close, MoreFilled, Search, User } from '@element-plus/icons-vue'
import type { ConversationProfile, ConversationSummary } from '@/types/chat'
import AvatarBadge from './AvatarBadge.vue'

const props = defineProps<{
  conversation: ConversationSummary | null
  profile: ConversationProfile | null
  isMobile: boolean
  menuOpen: boolean
  messageSearchOpen?: boolean
  messageSearchQuery?: string
  messageSearchMatchCount?: number
  activeMessageSearchIndex?: number
}>()

const emit = defineEmits<{
  back: []
  openProfile: []
  focusSearch: []
  openForwardSelection: []
  closeSearch: []
  navigateSearchMatch: [step: -1 | 1]
  action: [command: 'toggle-top' | 'toggle-mute' | 'mark-read' | 'delete']
  'start-call': []
  'update:menuOpen': [value: boolean]
  'update:messageSearchQuery': [value: string]
}>()

const messageSearchInput = ref<{ focus: () => void } | null>(null)
const searchCountLabel = computed(() => {
  if (!props.messageSearchQuery?.trim()) {
    return ''
  }

  if (!props.messageSearchMatchCount) {
    return '无匹配内容'
  }

  return `${(props.activeMessageSearchIndex ?? 0) + 1} / ${props.messageSearchMatchCount}`
})

watch(
  () => props.messageSearchOpen,
  async (value) => {
    if (!value) return
    await nextTick()
    messageSearchInput.value?.focus()
  },
)
</script>

<template>
  <header class="chat-topbar">
    <div class="chat-topbar__main">
      <button v-if="isMobile" class="chat-topbar__icon" type="button" aria-label="返回会话列表" @click="emit('back')">
        <ArrowLeft />
      </button>
      <div class="chat-topbar__identity">
        <AvatarBadge
          class="chat-topbar__avatar"
          :name="conversation?.conversationName"
          :avatar-url="conversation?.avatarUrl"
          :type="conversation?.conversationType === 1 ? 'user' : conversation?.conversationType === 2 ? 'group' : 'channel'"
          size="lg"
        />
        <div class="chat-topbar__copy">
          <div class="chat-topbar__title-row">
            <strong>{{ conversation?.conversationName ?? '请选择会话' }}</strong>
          </div>
          <p>{{ profile?.subtitle ?? '消息将保持同步' }}</p>
        </div>
      </div>
    </div>

    <div class="chat-topbar__actions">
      <div v-if="messageSearchOpen" class="chat-topbar__search-shell">
        <div class="chat-topbar__search-input">
          <Search />
          <el-input
            ref="messageSearchInput"
            :model-value="messageSearchQuery ?? ''"
            clearable
            placeholder="搜索当前会话中的消息"
            @update:model-value="emit('update:messageSearchQuery', typeof $event === 'string' ? $event : '')"
          />
        </div>
        <span v-if="searchCountLabel" class="chat-topbar__search-count">{{ searchCountLabel }}</span>
        <button
          class="chat-topbar__search-nav"
          type="button"
          :disabled="!messageSearchMatchCount"
          aria-label="上一条匹配消息"
          @click="emit('navigateSearchMatch', -1)"
        >
          <ArrowUp />
        </button>
        <button
          class="chat-topbar__search-nav"
          type="button"
          :disabled="!messageSearchMatchCount"
          aria-label="下一条匹配消息"
          @click="emit('navigateSearchMatch', 1)"
        >
          <ArrowDown />
        </button>
        <button class="chat-topbar__search-close" type="button" aria-label="关闭会话内搜索" @click="emit('closeSearch')">
          <Close />
        </button>
      </div>
      <button
        v-else-if="!isMobile"
        class="chat-topbar__icon"
        :class="{ 'is-active': messageSearchOpen }"
        type="button"
        aria-label="搜索当前会话内容"
        @click="emit('focusSearch')"
      >
        <Search />
      </button>
      <el-dropdown
        :hide-on-click="true"
        trigger="click"
        :teleported="true"
        :visible="menuOpen"
        @visible-change="emit('update:menuOpen', $event)"
        @command="emit('action', $event)"
      >
        <button class="chat-topbar__icon" type="button" aria-label="更多操作">
          <MoreFilled />
        </button>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item
              v-if="conversation?.conversationType === 1 && conversation?.specialType !== 'SAVED_MESSAGES'"
              @click="emit('start-call')"
            >
              发起语音通话
            </el-dropdown-item>
            <el-dropdown-item v-if="isMobile" @click="emit('focusSearch')">
              搜索消息
            </el-dropdown-item>
            <el-dropdown-item v-if="isMobile" @click="emit('openProfile')">
              查看资料
            </el-dropdown-item>
            <el-dropdown-item command="toggle-top">
              {{ props.conversation?.isTop ? '取消置顶' : '会话置顶' }}
            </el-dropdown-item>
            <el-dropdown-item command="toggle-mute">
              {{ props.conversation?.isMute ? '关闭免打扰' : '消息免打扰' }}
            </el-dropdown-item>
            <el-dropdown-item command="mark-read" :disabled="!props.conversation?.unreadCount">
              标记已读
            </el-dropdown-item>
            <el-dropdown-item @click="emit('openForwardSelection')">
              多选转发
            </el-dropdown-item>
            <el-dropdown-item command="delete" divided>
              删除会话
            </el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
      <button v-if="!isMobile" class="chat-topbar__icon" type="button" aria-label="打开会话详情" @click="emit('openProfile')">
        <User />
      </button>
    </div>
  </header>
</template>

<style scoped>
.chat-topbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  min-height: 64px;
  height: 64px;
  padding: 10px 20px;
  border-bottom: 1px solid var(--border-subtle);
  background: color-mix(in srgb, var(--surface-panel) 88%, transparent);
  backdrop-filter: blur(16px);
}

.chat-topbar__main,
.chat-topbar__identity,
.chat-topbar__actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.chat-topbar__main {
  min-width: 0;
  flex: 1;
  min-height: 100%;
}

.chat-topbar__avatar {
  flex-shrink: 0;
}

.chat-topbar__icon {
  width: 36px;
  height: 36px;
}

.chat-topbar__identity {
  min-width: 0;
  gap: 12px;
}

.chat-topbar__copy {
  min-width: 0;
}

.chat-topbar__actions {
  min-width: 0;
  flex-shrink: 0;
  min-height: 100%;
}

.chat-topbar__title-row {
  display: flex;
  align-items: center;
  gap: 6px;
}

.chat-topbar__identity strong {
  display: block;
  color: var(--text-primary);
  font-size: var(--text-lg);
  line-height: 1.12;
  font-weight: 600;
  letter-spacing: -0.018em;
}

.chat-topbar__identity p {
  margin-top: 4px;
  color: var(--text-tertiary);
  font-size: var(--text-sm);
  line-height: 1.28;
  letter-spacing: 0.01em;
}

.chat-topbar__icon {
  display: grid;
  place-items: center;
  border-radius: var(--radius-control);
  border: 0;
  background: transparent;
  color: var(--text-tertiary);
  transition:
    background var(--motion-fast) var(--motion-ease-out),
    color var(--motion-fast) var(--motion-ease-out);
}

.chat-topbar__icon :deep(svg) {
  width: 18px;
  height: 18px;
}

.chat-topbar__icon:hover,
.chat-topbar__icon:focus-visible {
  background: color-mix(in srgb, var(--interactive-selected-bg) 80%, transparent);
  color: var(--text-primary);
}

.chat-topbar__icon.is-active {
  background: color-mix(in srgb, var(--interactive-selected-bg) 88%, transparent);
  color: var(--interactive-selected-fg);
}

.chat-topbar__search-shell {
  display: flex;
  align-items: center;
  gap: 4px;
  width: clamp(380px, 44vw, 560px);
  max-width: 100%;
  height: 36px;
  padding: 3px 4px;
  border-radius: var(--radius-pill);
  background: color-mix(in srgb, var(--surface-panel) 92%, transparent);
  backdrop-filter: blur(12px);
}

.chat-topbar__search-input {
  display: flex;
  align-items: center;
  gap: 6px;
  min-width: 160px;
  flex: 1 1 260px;
  padding: 0 10px;
  height: 100%;
}

.chat-topbar__search-input > svg {
  width: 14px;
  height: 14px;
  color: var(--text-quaternary);
  flex-shrink: 0;
}

.chat-topbar__search-input :deep(.el-input) {
  min-width: 0;
  flex: 1;
}

.chat-topbar__search-input :deep(.el-input__wrapper) {
  min-height: 100%;
  padding: 0;
  border: 0;
  box-shadow: none;
  background: transparent;
}

.chat-topbar__search-input :deep(.el-input__wrapper.is-focus) {
  box-shadow: none !important;
  border-color: transparent;
}

.chat-topbar__search-input :deep(.el-input__inner) {
  color: var(--text-primary);
  font-size: var(--text-base);
}

.chat-topbar__search-count {
  min-width: 3rem;
  color: var(--text-quaternary);
  font: 600 var(--text-xs)/1 var(--font-mono);
  letter-spacing: 0.04em;
  text-align: right;
  white-space: nowrap;
}

.chat-topbar__search-nav,
.chat-topbar__search-close {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 26px;
  height: 26px;
  padding: 0;
  border: 0;
  border-radius: var(--radius-control);
  background: transparent;
  color: var(--text-tertiary);
  transition:
    background var(--motion-fast) var(--motion-ease-out),
    color var(--motion-fast) var(--motion-ease-out);
}

.chat-topbar__search-nav :deep(svg) {
  width: 14px;
  height: 14px;
}

.chat-topbar__search-nav:hover:not(:disabled),
.chat-topbar__search-nav:focus-visible:not(:disabled),
.chat-topbar__search-close:hover,
.chat-topbar__search-close:focus-visible {
  background: color-mix(in srgb, var(--interactive-selected-bg) 72%, transparent);
  color: var(--text-secondary);
}

.chat-topbar__search-nav:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.chat-topbar__search-close :deep(svg) {
  width: 14px;
  height: 14px;
}

@media (max-width: 767px) {
  .chat-topbar {
    padding-inline: 16px;
    min-height: 60px;
    height: 60px;
  }

  .chat-topbar__search-shell {
    width: auto;
    max-width: calc(100vw - 90px);
    gap: 3px;
    padding: 2px 3px;
  }

  .chat-topbar__search-count {
    display: none;
  }

  .chat-topbar__search-input {
    min-width: 0;
    padding-inline: 10px;
  }

  .chat-topbar__search-nav {
    width: 24px;
    min-width: 24px;
    height: 24px;
  }

  .chat-topbar__actions {
    gap: 6px;
  }
}
</style>
