<script setup lang="ts">
import { computed } from 'vue'
import type { ChatMessage } from '@/types/chat'

const props = defineProps<{
  pinnedMessages: ChatMessage[]
}>()

const emit = defineEmits<{
  'jump-to-message': [messageId: number]
  'unpin-message': [messageId: number]
}>()

const visible = computed(() => props.pinnedMessages.length > 0)
const latestPinned = computed(() => props.pinnedMessages[0] ?? null)
const pinnedCount = computed(() => props.pinnedMessages.length)

function previewText(message: ChatMessage): string {
  if (message.msgType === 'IMAGE') return '[图片]'
  if (message.msgType === 'GIF') return '[GIF]'
  if (message.msgType === 'FILE') return '[文件]'
  if (message.msgType === 'STICKER') return '[贴纸]'
  if (message.msgType === 'VOICE') return '[语音]'
  if (message.msgType === 'SYSTEM') return '系统消息'
  return message.content ?? '消息'
}
</script>

<template>
  <div v-if="visible" class="pinned-banner" role="region" aria-label="置顶消息">
    <div class="pinned-banner__content">
      <svg class="pinned-banner__icon" viewBox="0 0 24 24" fill="none" aria-hidden="true">
        <path d="M16 3l5 5-6.5 6.5L16 21l-2-6.5L7.5 18l1-6.5L2 8l5-1L16 3z" stroke="currentColor" stroke-width="1.8" stroke-linejoin="round"/>
      </svg>
      <div class="pinned-banner__info">
        <span class="pinned-banner__label">
          {{ pinnedCount > 1 ? `${pinnedCount} 条置顶消息` : '置顶消息' }}
        </span>
        <span v-if="latestPinned" class="pinned-banner__preview">
          {{ previewText(latestPinned) }}
        </span>
      </div>
    </div>
    <div class="pinned-banner__actions">
      <button
        v-if="latestPinned"
        class="pinned-banner__btn"
        type="button"
        aria-label="查看置顶消息"
        @click="emit('jump-to-message', latestPinned.messageId)"
      >
        查看
      </button>
    </div>
  </div>
</template>

<style scoped>
.pinned-banner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 8px 16px;
  margin: 0 22px 4px;
  border: 1px solid color-mix(in srgb, var(--interactive-primary-bg) 18%, var(--border-subtle));
  border-radius: var(--radius-md);
  background: color-mix(in srgb, var(--interactive-primary-bg) 6%, var(--surface-card));
  max-width: min(700px, 100%);
  margin-left: auto;
  margin-right: auto;
}

.pinned-banner__content {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
}

.pinned-banner__icon {
  width: 16px;
  height: 16px;
  flex-shrink: 0;
  color: var(--interactive-primary-bg);
}

.pinned-banner__info {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}

.pinned-banner__label {
  font: 600 var(--text-xs)/1 var(--font-mono);
  color: var(--text-secondary);
  white-space: nowrap;
}

.pinned-banner__preview {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 200px;
  font-size: var(--text-xs);
  color: var(--text-tertiary);
}

.pinned-banner__actions {
  display: flex;
  gap: 6px;
  flex-shrink: 0;
}

.pinned-banner__btn {
  padding: 4px 10px;
  border: 1px solid color-mix(in srgb, var(--interactive-primary-bg) 28%, var(--border-default));
  border-radius: var(--radius-control);
  background: transparent;
  color: var(--interactive-primary-bg);
  font: 600 var(--text-xs)/1 var(--font-body);
  cursor: pointer;
  transition:
    background var(--motion-fast) var(--motion-ease-out),
    border-color var(--motion-fast) var(--motion-ease-out);
}

.pinned-banner__btn:hover {
  background: color-mix(in srgb, var(--interactive-primary-bg) 10%, var(--surface-panel));
  border-color: var(--interactive-primary-bg);
}
</style>
