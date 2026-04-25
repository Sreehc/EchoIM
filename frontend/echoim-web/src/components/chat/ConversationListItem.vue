<script setup lang="ts">
import { Bell, Top } from '@element-plus/icons-vue'
import type { ConversationSummary } from '@/types/chat'
import { formatConversationTime, highlightText } from '@/utils/format'
import AvatarBadge from './AvatarBadge.vue'

const props = defineProps<{
  item: ConversationSummary
  active: boolean
  searchQuery?: string
}>()
</script>

<template>
  <button
    :id="`conversation-item-${item.conversationId}`"
    :data-testid="`conversation-item-${item.conversationId}`"
    class="conversation-item"
    :class="{ 'is-active': active, 'is-muted': item.isMute === 1 }"
    type="button"
  >
    <AvatarBadge
      class="conversation-item__avatar"
      :name="item.conversationName"
      :avatar-url="item.avatarUrl"
      :online="item.conversationType === 1"
      :type="item.conversationType === 2 ? 'group' : 'user'"
      size="md"
    />
    <div class="conversation-item__body">
      <div class="conversation-item__head">
        <strong>
          <template v-for="(part, index) in highlightText(item.conversationName, props.searchQuery ?? '')" :key="index">
            <mark v-if="part.matched" class="conversation-item__highlight">{{ part.text }}</mark>
            <span v-else>{{ part.text }}</span>
          </template>
        </strong>
        <span>{{ formatConversationTime(item.lastMessageTime) }}</span>
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
        <div class="conversation-item__meta">
          <Top v-if="item.isTop" class="conversation-item__meta-icon" />
          <Bell v-if="item.isMute" class="conversation-item__meta-icon" />
          <span v-if="item.unreadCount" class="conversation-item__badge">{{ item.unreadCount }}</span>
        </div>
      </div>
    </div>
  </button>
</template>

<style scoped>
.conversation-item {
  width: 100%;
  display: grid;
  grid-template-columns: 40px 1fr;
  gap: 10px;
  padding: 12px 10px;
  border: 1px solid transparent;
  border-radius: 12px;
  background: transparent;
  text-align: left;
  transition:
    background var(--motion-base) ease,
    border-color var(--motion-base) ease,
    transform var(--motion-fast) ease;
}

.conversation-item:hover,
.conversation-item:focus-visible,
.conversation-item.is-active {
  border-color: color-mix(in srgb, var(--color-primary) 14%, var(--color-line));
  background: var(--color-selected);
}

.conversation-item:active {
  transform: scale(0.99);
}

.conversation-item__avatar {
  margin-top: 1px;
}

.conversation-item__highlight {
  padding: 0;
  background: transparent;
  color: var(--color-primary-strong);
}

.conversation-item__head,
.conversation-item__foot {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 10px;
}

.conversation-item__head strong {
  font-size: 0.96rem;
  font-weight: 600;
  line-height: 1.15;
}

.conversation-item__head span {
  color: var(--color-text-soft);
  font: 500 0.66rem/1 var(--font-mono);
  letter-spacing: -0.01em;
}

.conversation-item__foot p {
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
  color: var(--color-text-3);
  font-size: 0.76rem;
  line-height: 1.25;
}

.conversation-item__meta {
  display: inline-flex;
  align-items: center;
  gap: 5px;
}

.conversation-item__meta-icon {
  width: 12px;
  height: 12px;
  color: var(--color-text-soft);
}

.conversation-item__badge {
  min-width: 18px;
  height: 18px;
  padding: 0 5px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 999px;
  background: var(--color-primary);
  color: #fff;
  font: 600 0.62rem/1 var(--font-mono);
}

.conversation-item.is-muted .conversation-item__badge {
  background: var(--color-text-soft);
}
</style>
