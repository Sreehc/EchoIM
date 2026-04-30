<script setup lang="ts">
import type { ConversationSummary } from '@/types/chat'
import { formatConversationTime, highlightText } from '@/utils/format'
import AvatarBadge from './AvatarBadge.vue'

const props = defineProps<{
  item: ConversationSummary
  active: boolean
  searchQuery?: string
  compact?: boolean
}>()

function visibleUnreadCount(item: ConversationSummary) {
  return Math.max(item.unreadCount, item.manualUnread ? 1 : 0)
}
</script>

<template>
  <button
    :id="`conversation-item-${item.conversationId}`"
    :data-testid="`conversation-item-${item.conversationId}`"
    class="conversation-item"
    :class="{ 'is-active': active, 'is-muted': item.isMute === 1, 'is-compact': compact }"
    type="button"
  >
    <AvatarBadge
      class="conversation-item__avatar"
      :name="item.conversationName"
      :avatar-url="item.avatarUrl"
      :online="item.conversationType === 1"
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
            <svg viewBox="0 0 16 16" class="conversation-item__state-icon" aria-hidden="true">
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
          <span v-if="item.isTop" class="conversation-item__state" aria-label="置顶" title="置顶">
            <svg viewBox="0 0 16 16" class="conversation-item__state-icon" aria-hidden="true">
              <path
                d="M4.45 2.45c0-.52.43-.95.95-.95h5.2c.52 0 .95.43.95.95 0 .28-.12.55-.34.73L9.9 4.28v3.14l1.34 1.22c.2.18.31.44.31.71 0 .53-.42.95-.95.95H8.72v3.6a.72.72 0 1 1-1.44 0v-3.6H5.4a.95.95 0 0 1-.64-1.66L6.1 7.42V4.28L4.79 3.18a.98.98 0 0 1-.34-.73Z"
                fill="none"
                stroke="currentColor"
                stroke-linejoin="round"
                stroke-width="1.35"
              />
            </svg>
          </span>
          <span class="conversation-item__time">{{ formatConversationTime(item.lastMessageTime) }}</span>
        </div>
      </div>
      <div class="conversation-item__foot">
        <p>
          <template
            v-for="(part, index) in highlightText(item.lastMessagePreview, props.searchQuery ?? '')"
            :key="index"
          >
            <mark v-if="part.matched" class="conversation-item__highlight">{{ part.text }}</mark>
            <span v-else>{{ part.text }}</span>
          </template>
        </p>
        <div v-if="visibleUnreadCount(item)" class="conversation-item__meta">
          <span class="conversation-item__badge">{{ visibleUnreadCount(item) }}</span>
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
  font-size: 0.93rem;
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
  font: 500 0.75rem/1 var(--font-mono);
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
  font-size: 0.8rem;
  line-height: 1.32;
}

.conversation-item__meta {
  display: inline-flex;
  align-items: center;
  margin-left: 8px;
  flex-shrink: 0;
}

.conversation-item__state {
  width: 16px;
  height: 16px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  color: var(--text-quaternary);
}

.conversation-item__state-icon {
  width: 14px;
  height: 14px;
}

.conversation-item__badge {
  min-width: 19px;
  height: 19px;
  padding: 0 6px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: var(--radius-pill);
  background: color-mix(in srgb, var(--status-success) 88%, white);
  color: var(--text-on-brand);
  font: 700 0.62rem/1 var(--font-mono);
  letter-spacing: 0.02em;
}

.conversation-item.is-muted .conversation-item__badge {
  background: var(--text-quaternary);
}

.conversation-item.is-active .conversation-item__time,
.conversation-item.is-active .conversation-item__foot p,
.conversation-item.is-active .conversation-item__state {
  color: var(--text-secondary);
}

.conversation-item.is-active .conversation-item__badge {
  background: color-mix(in srgb, var(--interactive-primary-bg) 82%, white);
}
</style>
