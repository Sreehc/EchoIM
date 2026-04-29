<script setup lang="ts">
import { computed, nextTick, ref, watch } from 'vue'
import { ArrowLeft, Close, MoreFilled, Search, Setting } from '@element-plus/icons-vue'
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
          size="md"
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
          上一个
        </button>
        <button
          class="chat-topbar__search-nav"
          type="button"
          :disabled="!messageSearchMatchCount"
          aria-label="下一条匹配消息"
          @click="emit('navigateSearchMatch', 1)"
        >
          下一个
        </button>
        <button class="chat-topbar__search-close" type="button" aria-label="关闭会话内搜索" @click="emit('closeSearch')">
          <Close />
        </button>
      </div>
      <button
        v-else
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
      <button class="chat-topbar__icon" type="button" aria-label="打开会话详情" @click="emit('openProfile')">
        <Setting />
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
  min-height: 76px;
  height: 76px;
  padding: 12px 20px;
  border-bottom: 1px solid var(--color-shell-border);
  background: color-mix(in srgb, var(--color-shell-toolbar) 92%, transparent);
  backdrop-filter: blur(20px);
}

.chat-topbar__main,
.chat-topbar__identity,
.chat-topbar__actions {
  display: flex;
  align-items: center;
  gap: 10px;
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
  width: 40px;
  height: 40px;
}

.chat-topbar__identity {
  min-width: 0;
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
  font-size: 1.02rem;
  line-height: 1.15;
  font-weight: 700;
  letter-spacing: -0.02em;
}

.chat-topbar__identity p {
  margin-top: 3px;
  color: var(--color-text-soft);
  font-size: 0.8rem;
  line-height: 1.2;
}

.chat-topbar__icon {
  display: grid;
  place-items: center;
  border-radius: 16px;
  border: 1px solid var(--color-shell-border);
  background: var(--color-shell-action);
  color: var(--color-text-2);
  transition:
    background var(--motion-fast) ease,
    color var(--motion-fast) ease,
    transform var(--motion-fast) ease,
    border-color var(--motion-fast) ease;
}

.chat-topbar__icon:hover,
.chat-topbar__icon:focus-visible {
  background: var(--color-shell-action-hover);
  border-color: var(--color-shell-border);
  color: var(--color-text-1);
  transform: translateY(-1px);
}

.chat-topbar__icon.is-active {
  background: color-mix(in srgb, var(--color-selected) 88%, var(--color-shell-action));
  color: var(--color-primary-strong);
  border-color: var(--color-shell-border-strong);
}

.chat-topbar__search-shell {
  display: flex;
  align-items: center;
  gap: 10px;
  width: clamp(460px, 52vw, 680px);
  max-width: 100%;
  min-height: 56px;
  padding: 6px;
  border: 1px solid var(--color-shell-border);
  border-radius: 999px;
  background: var(--color-shell-card-strong);
  box-shadow: var(--shadow-card);
}

.chat-topbar__search-input {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 260px;
  flex: 1 1 340px;
  padding: 0 16px;
  min-height: 100%;
  border: 1px solid color-mix(in srgb, var(--color-primary) 18%, var(--color-shell-border));
  border-radius: 999px;
  background: color-mix(in srgb, var(--color-shell-inline) 92%, transparent);
}

.chat-topbar__search-input > svg {
  width: 13px;
  height: 13px;
  color: var(--color-shell-eyebrow);
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
  color: var(--color-text-1);
  font-size: 0.96rem;
}

.chat-topbar__search-count {
  min-width: 3.75rem;
  color: var(--color-text-2);
  font: 600 0.68rem/1.2 var(--font-mono);
  letter-spacing: 0.04em;
  text-align: right;
}

.chat-topbar__search-nav,
.chat-topbar__search-close {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  height: 40px;
  padding: 0 14px;
  border: 1px solid var(--color-shell-border);
  border-radius: 18px;
  background: var(--color-shell-action);
  color: var(--color-text-1);
  font: 600 0.76rem/1 var(--font-body);
  transition:
    transform var(--motion-fast) ease,
    background var(--motion-fast) ease,
    border-color var(--motion-fast) ease;
}

.chat-topbar__search-nav {
  min-width: 76px;
}

.chat-topbar__search-nav:hover:not(:disabled),
.chat-topbar__search-nav:focus-visible:not(:disabled),
.chat-topbar__search-close:hover,
.chat-topbar__search-close:focus-visible {
  transform: translateY(-1px);
  background: var(--color-shell-action-hover);
  border-color: var(--color-shell-border-strong);
}

.chat-topbar__search-nav:disabled {
  opacity: 0.45;
  cursor: not-allowed;
}

.chat-topbar__search-close {
  width: 36px;
  height: 36px;
  padding: 0;
}

.chat-topbar__search-close :deep(svg) {
  width: 18px;
  height: 18px;
}

@media (max-width: 767px) {
  .chat-topbar {
    padding-inline: 16px;
    min-height: 72px;
    height: 72px;
  }

  .chat-topbar__search-shell {
    width: auto;
    max-width: calc(100vw - 124px);
    gap: 6px;
    padding-inline: 5px;
  }

  .chat-topbar__search-count {
    display: none;
  }

  .chat-topbar__search-input {
    min-width: 0;
    padding-inline: 12px;
  }

  .chat-topbar__search-nav {
    min-width: 64px;
    padding-inline: 10px;
    font-size: 0.66rem;
  }
}
</style>
