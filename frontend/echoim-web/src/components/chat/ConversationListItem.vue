<script setup lang="ts">
import { computed } from 'vue'
import { MuteNotification } from '@element-plus/icons-vue'
import type { ConversationSummary } from '@/types/chat'
import { formatConversationTime, highlightText } from '@/utils/format'
import { useChatStore } from '@/stores/chat'
import AvatarBadge from './AvatarBadge.vue'

const props = defineProps<{
  item: ConversationSummary
  active: boolean
  searchQuery?: string
  compact?: boolean
}>()

const chatStore = useChatStore()

const isMentioned = computed(() => chatStore.mentionedConversationIds.has(props.item.conversationId))

const isOnline = computed(() => {
  if (props.item.conversationType !== 1) return false
  const peerId = props.item.peerUserId
  return peerId != null && chatStore.isUserOnline(peerId)
})

const hasDraft = computed(() => {
  // Check backend draftContent first, then fallback to localStorage
  if (props.item.draftContent) return true
  try {
    const drafts = JSON.parse(localStorage.getItem('echoim_drafts') || '{}')
    return Boolean(drafts[`draft_${props.item.conversationId}`])
  } catch {
    return false
  }
})

function visibleUnreadCount(item: ConversationSummary) {
  return Math.max(item.unreadCount, item.manualUnread ? 1 : 0)
}
</script>

<template>
  <button
    :id="`conversation-item-${item.conversationId}`"
    :data-testid="`conversation-item-${item.conversationId}`"
    class="conversation-item"
    :class="{ 'is-active': active, 'is-muted': item.isMute === 1, 'is-compact': compact, 'is-top': item.isTop }"
    type="button"
  >
    <AvatarBadge
      class="conversation-item__avatar"
      :name="item.conversationName"
      :avatar-url="item.avatarUrl"
      :online="isOnline"
      :type="item.conversationType === 1 ? 'user' : item.conversationType === 2 ? 'group' : 'channel'"
      size="xl"
    />
    <div class="conversation-item__body">
      <div class="conversation-item__head">
        <strong class="conversation-item__name">
          <template v-for="(part, index) in highlightText(item.conversationName, props.searchQuery ?? '')" :key="index">
            <mark v-if="part.matched" class="conversation-item__highlight">{{ part.text }}</mark>
            <span v-else>{{ part.text }}</span>
          </template>
        </strong>
        <div class="conversation-item__head-meta">
          <span v-if="item.isMute" class="conversation-item__state" aria-label="消息免打扰" title="消息免打扰">
            <MuteNotification class="conversation-item__state-icon" />
          </span>
          <span class="conversation-item__time">{{ formatConversationTime(item.lastMessageTime) }}</span>
        </div>
      </div>
      <div class="conversation-item__foot">
        <p v-if="hasDraft" class="conversation-item__draft">
          <span class="conversation-item__draft-badge">草稿</span>
          <template
            v-for="(part, index) in highlightText(item.lastMessagePreview, props.searchQuery ?? '')"
            :key="index"
          >
            <mark v-if="part.matched" class="conversation-item__highlight">{{ part.text }}</mark>
            <span v-else>{{ part.text }}</span>
          </template>
        </p>
        <p v-else>
          <template
            v-for="(part, index) in highlightText(item.lastMessagePreview, props.searchQuery ?? '')"
            :key="index"
          >
            <mark v-if="part.matched" class="conversation-item__highlight">{{ part.text }}</mark>
            <span v-else>{{ part.text }}</span>
          </template>
        </p>
        <div v-if="visibleUnreadCount(item) || isMentioned" class="conversation-item__meta">
          <span v-if="isMentioned" class="conversation-item__mention-badge">@</span>
          <span v-if="visibleUnreadCount(item)" class="conversation-item__badge">{{ visibleUnreadCount(item) }}</span>
        </div>
      </div>
    </div>
  </button>
</template>

<style scoped>
.conversation-item {
  position: relative;
  width: 100%;
  display: grid;
  grid-template-columns: 48px minmax(0, 1fr);
  gap: 12px;
  min-height: 70px;
  padding: 8px 12px 8px 14px;
  border: 1px solid transparent;
  border-radius: var(--radius-panel);
  background: transparent;
  text-align: left;
  content-visibility: auto;
  contain-intrinsic-size: auto 70px;
  transition:
    background var(--motion-base) var(--motion-ease-out),
    color var(--motion-base) var(--motion-ease-out),
    border-color var(--motion-fast) var(--motion-ease-out),
    box-shadow var(--motion-fast) var(--motion-ease-out);
}

.conversation-item::before {
  content: '';
  position: absolute;
  top: 10px;
  bottom: 10px;
  left: 4px;
  width: 2px;
  border-radius: 999px;
  pointer-events: none;
  background: color-mix(in srgb, var(--interactive-primary-bg) 72%, transparent);
  opacity: 0;
  transition: opacity var(--motion-fast) var(--motion-ease-out), transform var(--motion-fast) var(--motion-ease-out);
  transform: scaleY(0.72);
}

.conversation-item.is-compact {
  min-height: 64px;
  padding-block: 7px;
}

.conversation-item:hover,
.conversation-item:focus-visible {
  background: color-mix(in srgb, var(--surface-panel) 88%, transparent);
  border-color: var(--border-subtle);
}

.conversation-item:hover::before,
.conversation-item:focus-visible::before,
.conversation-item.is-active::before {
  opacity: 1;
  transform: scaleY(1);
}

.conversation-item.is-active {
  background: color-mix(in srgb, var(--interactive-selected-bg) 86%, var(--surface-card));
  color: var(--text-primary);
  border-color: color-mix(in srgb, var(--interactive-primary-bg) 16%, var(--border-default));
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.06);
}

.conversation-item.is-top:not(.is-active) {
  background:
    linear-gradient(135deg, color-mix(in srgb, var(--interactive-focus-ring) 8%, transparent), transparent 50%),
    color-mix(in srgb, var(--surface-card) 88%, transparent);
  border-color: color-mix(in srgb, var(--border-default) 60%, transparent);
}

.conversation-item__avatar {
  align-self: center;
}

.conversation-item__highlight {
  padding: 0 0.08em;
  border-radius: 6px;
  background: color-mix(in srgb, var(--interactive-selected-bg) 78%, white);
  color: var(--text-primary);
}

.conversation-item__head,
.conversation-item__foot {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 10px;
  min-width: 0;
}

.conversation-item__body {
  display: grid;
  align-content: center;
  gap: 5px;
  min-width: 0;
}

.conversation-item__name {
  min-width: 0;
  flex: 1 1 auto;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
  color: var(--text-primary);
  font-size: var(--text-lg);
  font-weight: 600;
  letter-spacing: -0.012em;
  line-height: 1.18;
}

.conversation-item__head-meta {
  display: inline-flex;
  align-items: center;
  justify-content: flex-end;
  gap: 5px;
  flex: 0 0 auto;
  min-width: 0;
}

.conversation-item__time {
  color: var(--text-tertiary);
  flex-shrink: 0;
  font: 500 var(--text-xs)/1 var(--font-mono);
  white-space: nowrap;
  letter-spacing: 0.02em;
}

.conversation-item__foot p {
  flex: 1 1 auto;
  min-width: 0;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
  color: var(--text-tertiary);
  font-size: var(--text-sm);
  line-height: 1.32;
}

.conversation-item__meta {
  display: inline-flex;
  align-items: center;
  margin-left: 8px;
  flex-shrink: 0;
}

.conversation-item__state {
  width: 18px;
  height: 18px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  color: var(--text-quaternary);
}

.conversation-item__state-icon {
  width: 16px;
  height: 16px;
}

.conversation-item__badge {
  min-width: 20px;
  height: 20px;
  padding: 0 6px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: var(--radius-pill);
  background: color-mix(in srgb, var(--status-success) 88%, white);
  color: var(--text-on-brand);
  font: 700 var(--text-2xs)/1 var(--font-mono);
  letter-spacing: 0.02em;
}

.conversation-item.is-muted .conversation-item__badge {
  background: var(--text-quaternary);
}

.conversation-item__mention-badge {
  min-width: 20px;
  height: 20px;
  padding: 0 6px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: var(--radius-pill);
  background: color-mix(in srgb, var(--interactive-primary-bg) 88%, white);
  color: var(--text-on-brand);
  font: 700 var(--text-2xs)/1 var(--font-mono);
  letter-spacing: 0.02em;
}

.conversation-item.is-active .conversation-item__time,
.conversation-item.is-active .conversation-item__foot p,
.conversation-item.is-active .conversation-item__state {
  color: var(--text-secondary);
}

.conversation-item.is-active .conversation-item__badge {
  background: color-mix(in srgb, var(--interactive-primary-bg) 82%, white);
}

.conversation-item__draft {
  display: flex;
  align-items: center;
  gap: 6px;
}

.conversation-item__draft-badge {
  flex-shrink: 0;
  padding: 1px 5px;
  border-radius: 4px;
  background: color-mix(in srgb, var(--status-warning) 88%, white);
  color: var(--text-primary);
  font: 600 var(--text-2xs)/1 var(--font-mono);
  letter-spacing: 0.02em;
}
</style>
