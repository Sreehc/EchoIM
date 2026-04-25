<script setup lang="ts">
import { computed } from 'vue'
import { Document, Picture } from '@element-plus/icons-vue'
import type { ChatMessage } from '@/types/chat'
import { formatMessageTime } from '@/utils/format'
import AvatarBadge from './AvatarBadge.vue'

const props = defineProps<{
  message: ChatMessage
  currentUserId: number
  senderName: string
  showAvatar: boolean
  showSenderLabel: boolean
  groupedWithPrev: boolean
  groupedWithNext: boolean
}>()

const emit = defineEmits<{
  retry: [clientMsgId: string]
}>()

const isSelf = computed(() => props.message.fromUserId === props.currentUserId)
const isSystem = computed(() => props.message.msgType === 'SYSTEM')
const attachmentMeta = computed(() => {
  if (props.message.msgType === 'IMAGE') {
    return {
      title: props.message.file?.fileName ?? props.message.content ?? '图片预览',
      description: props.message.file?.contentType ?? '图片消息',
    }
  }

  if (props.message.msgType === 'FILE') {
    return {
      title: props.message.file?.fileName ?? props.message.content ?? '未命名文件',
      description: [props.message.file?.contentType, formatFileSize(props.message.file?.fileSize ?? null)]
        .filter(Boolean)
        .join(' · '),
    }
  }

  return null
})

function formatFileSize(value: number | null) {
  if (!value || value <= 0) return ''
  if (value >= 1024 * 1024) return `${(value / 1024 / 1024).toFixed(1)} MB`
  if (value >= 1024) return `${Math.round(value / 1024)} KB`
  return `${value} B`
}
</script>

<template>
  <div
    class="message-row"
    :data-testid="`message-row-${message.clientMsgId || message.messageId}`"
    :class="{
      'is-self': isSelf,
      'is-system': isSystem,
      'is-grouped': groupedWithPrev,
      'is-grouped-next': groupedWithNext,
    }"
  >
    <AvatarBadge
      v-if="!isSystem && showAvatar"
      class="message-row__avatar"
      :name="senderName"
      :seed="message.fromUserId"
      size="sm"
      :type="message.groupId ? 'group' : 'user'"
    />
    <div v-else-if="!isSystem && !isSelf" class="message-row__avatar-spacer"></div>
    <div
      class="message-bubble"
      :class="{
        'is-self': isSelf,
        'is-system': isSystem,
        'is-grouped': groupedWithPrev,
        'is-grouped-next': groupedWithNext,
      }"
    >
      <span v-if="showSenderLabel" class="message-bubble__sender">{{ senderName }}</span>
      <template v-if="message.msgType === 'SYSTEM'">
        <span>{{ message.content }}</span>
      </template>
      <template v-else-if="message.msgType === 'IMAGE'">
        <div class="message-bubble__file message-bubble__file--image">
          <div class="message-bubble__preview">
            <Picture />
          </div>
          <div class="message-bubble__file-copy">
            <strong>{{ attachmentMeta?.title }}</strong>
            <p>{{ attachmentMeta?.description }}</p>
          </div>
        </div>
      </template>
      <template v-else-if="message.msgType === 'FILE'">
        <div class="message-bubble__file">
          <div class="message-bubble__preview message-bubble__preview--file">
            <Document />
          </div>
          <div class="message-bubble__file-copy">
            <strong>{{ attachmentMeta?.title }}</strong>
            <p>{{ attachmentMeta?.description }}</p>
          </div>
        </div>
      </template>
      <template v-else>
        <p>{{ message.content }}</p>
      </template>

      <footer v-if="!isSystem" class="message-bubble__meta">
        <span>{{ formatMessageTime(message.sentAt) }}</span>
        <span v-if="isSelf" class="message-bubble__status" data-testid="message-status">
          {{
            message.sendStatus === 2
              ? '发送失败'
              : message.read
                ? '已读'
                : message.delivered
                  ? '已送达'
                  : '发送中'
          }}
        </span>
        <button
          v-if="isSelf && message.sendStatus === 2"
          class="message-bubble__retry"
          type="button"
          :aria-label="`重试发送：${message.content ?? attachmentMeta?.title ?? '消息'}`"
          @click="emit('retry', message.clientMsgId)"
        >
          重试
        </button>
      </footer>
    </div>
  </div>
</template>

<style scoped>
.message-row {
  display: flex;
  gap: 12px;
  align-items: flex-end;
  margin-top: 8px;
}

.message-row.is-self {
  justify-content: flex-end;
}

.message-row.is-grouped {
  margin-top: 1px;
}

.message-row.is-system {
  justify-content: center;
}

.message-row__avatar {
  margin-bottom: 2px;
}

.message-row__avatar-spacer {
  width: 28px;
  flex-shrink: 0;
}

.message-bubble {
  max-width: min(58ch, 69%);
  padding: 8px 11px 9px;
  border: 1px solid var(--color-line);
  border-radius: 14px 14px 14px 6px;
  background: var(--color-bubble-peer);
}

.message-bubble.is-self {
  border-radius: 14px 14px 6px 14px;
  background: var(--color-bubble-self);
}

.message-bubble.is-grouped:not(.is-self) {
  border-top-left-radius: 8px;
}

.message-bubble.is-grouped-next:not(.is-self) {
  border-bottom-left-radius: 8px;
}

.message-bubble.is-self.is-grouped {
  border-top-right-radius: 8px;
}

.message-bubble.is-self.is-grouped-next {
  border-bottom-right-radius: 8px;
}

.message-bubble.is-system {
  max-width: unset;
  padding: 0;
  border: 0;
  background: transparent;
  color: var(--color-text-3);
  font: 500 0.68rem/1.1 var(--font-mono);
}

.message-bubble__sender {
  display: inline-block;
  margin-bottom: 3px;
  color: var(--color-primary);
  font-size: 0.66rem;
  font-weight: 600;
}

.message-bubble p {
  white-space: pre-wrap;
  font-size: 0.9rem;
  line-height: 1.42;
}

.message-bubble__meta {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  gap: 6px;
  margin-top: 5px;
  color: var(--color-text-2);
  font: 500 0.62rem/1 var(--font-mono);
}

.message-bubble__retry {
  padding: 0;
  border: 0;
  background: transparent;
  color: var(--color-primary);
  font: inherit;
}

.message-bubble__retry:hover,
.message-bubble__retry:focus-visible {
  text-decoration: underline;
}

.message-bubble__file {
  display: flex;
  align-items: center;
  gap: 9px;
  min-width: 220px;
  padding: 2px;
}

.message-bubble__preview {
  width: 42px;
  height: 42px;
  display: grid;
  place-items: center;
  border-radius: 12px;
  background: color-mix(in srgb, var(--color-primary) 8%, var(--color-bg-panel));
  flex-shrink: 0;
}

.message-bubble__preview svg {
  width: 18px;
  height: 18px;
  color: var(--color-primary);
}

.message-bubble__preview--file {
  background: color-mix(in srgb, var(--color-text-1) 6%, var(--color-bg-panel));
}

.message-bubble__file-copy {
  min-width: 0;
}

.message-bubble__file strong {
  display: block;
  margin-bottom: 2px;
  font-size: 0.86rem;
  line-height: 1.2;
}

.message-bubble__file p {
  color: var(--color-text-2);
  font-size: 0.76rem;
}

.message-bubble__file--image {
  min-width: 240px;
  padding: 10px;
  border-radius: 10px;
  background: color-mix(in srgb, var(--color-primary) 4%, var(--color-bg-panel));
}

@media (max-width: 767px) {
  .message-bubble {
    max-width: 84%;
  }
}
</style>
