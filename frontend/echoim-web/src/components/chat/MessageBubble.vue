<script setup lang="ts">
import { computed } from 'vue'
import { Document, Picture } from '@element-plus/icons-vue'
import type { ChatMessage, ConversationType } from '@/types/chat'
import { formatMessageTime, highlightText } from '@/utils/format'
import AvatarBadge from './AvatarBadge.vue'

const props = defineProps<{
  message: ChatMessage
  conversationType: ConversationType
  currentUserId: number
  senderName: string
  showAvatar: boolean
  showSenderLabel: boolean
  groupedWithPrev: boolean
  groupedWithNext: boolean
  compact?: boolean
  editing?: boolean
  editingDraft?: string
  actionPending?: boolean
  searchQuery?: string
  searchActive?: boolean
}>()

const emit = defineEmits<{
  retry: [clientMsgId: string]
  'start-edit': []
  'update:editing-draft': [value: string]
  'cancel-edit': []
  'save-edit': [value: string]
  recall: []
}>()

const isSelf = computed(() => props.message.fromUserId === props.currentUserId)
const isSystem = computed(() => props.message.msgType === 'SYSTEM')
const statusMeta = computed(() => {
  if (props.message.sendStatus === 2) {
    return {
      label: '发送失败',
      glyph: '!',
      tone: 'failed',
    }
  }

  if (props.message.sendStatus !== 1) {
    return {
      label: '发送中',
      glyph: '…',
      tone: 'sending',
    }
  }

  return {
    label:
      props.conversationType === 2
        ? '发送成功'
        : props.message.read
          ? '已读'
          : '已发送',
    glyph:
      props.conversationType === 2
        ? '✓✓'
        : props.message.read
          ? '✓✓'
          : '✓',
    tone:
      props.conversationType === 2
        ? 'group'
        : props.message.read
          ? 'read'
          : 'sent',
  }
})
const channelViewLabel = computed(() => {
  if (!isSelf.value || props.conversationType !== 3 || props.message.sendStatus !== 1) {
    return ''
  }

  return `${props.message.viewCount ?? 0} 人看过`
})
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
const canManageMessage = computed(
  () =>
    isSelf.value &&
    props.message.msgType === 'TEXT' &&
    props.message.sendStatus === 1 &&
    !props.message.recalled,
)
const bubbleText = computed(() => {
  if (props.message.recalled) {
    return '撤回了一条消息'
  }

  return props.message.content ?? ''
})
const highlightedBubbleText = computed(() => {
  if (props.message.recalled) {
    return [{ text: bubbleText.value, matched: false }]
  }

  return highlightText(bubbleText.value, props.searchQuery ?? '')
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
      'is-compact': compact,
      'is-search-active': searchActive,
    }"
    :data-search-message-id="message.messageId"
  >
    <AvatarBadge
      v-if="!isSystem && showAvatar"
      class="message-row__avatar"
      :name="senderName"
      :seed="message.fromUserId"
      size="sm"
      type="user"
    />
    <div v-else-if="!isSystem && !isSelf" class="message-row__avatar-spacer"></div>
    <div
      class="message-bubble"
      :class="{
        'is-self': isSelf,
        'is-system': isSystem,
        'is-grouped': groupedWithPrev,
        'is-grouped-next': groupedWithNext,
        'is-compact': compact,
      }"
    >
      <span v-if="showSenderLabel" class="message-bubble__sender">{{ senderName }}</span>
      <div v-if="message.forwardSource && !message.recalled" class="message-bubble__forward">
        转发自 #{{ message.forwardSource.sourceConversationId }} · {{ message.forwardSource.sourcePreview || '消息' }}
      </div>
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
        <div v-if="editing" class="message-bubble__editor">
          <el-input
            :model-value="editingDraft"
            type="textarea"
            :autosize="{ minRows: 2, maxRows: 5 }"
            maxlength="500"
            @update:model-value="emit('update:editing-draft', String($event))"
          />
          <div class="message-bubble__editor-actions">
            <button type="button" data-testid="message-cancel-edit" :disabled="actionPending" @click="emit('cancel-edit')">取消</button>
            <button
              type="button"
              data-testid="message-save-edit"
              :disabled="actionPending || !editingDraft?.trim()"
              @click="emit('save-edit', editingDraft ?? '')"
            >
              保存
            </button>
          </div>
        </div>
        <p v-else class="message-bubble__text">
          <template v-for="(part, index) in highlightedBubbleText" :key="`${message.messageId}-${index}`">
            <mark v-if="part.matched" class="message-bubble__highlight">{{ part.text }}</mark>
            <template v-else>{{ part.text }}</template>
          </template>
        </p>
      </template>

      <footer v-if="!isSystem" class="message-bubble__meta">
        <span>{{ formatMessageTime(message.sentAt) }}</span>
        <span v-if="message.edited && !message.recalled" class="message-bubble__edited">已编辑</span>
        <span v-if="channelViewLabel" class="message-bubble__views">{{ channelViewLabel }}</span>
        <span
          v-else-if="isSelf"
          class="message-bubble__status"
          :class="`is-${statusMeta.tone}`"
          :aria-label="statusMeta.label"
          data-testid="message-status"
        >
          {{ statusMeta.glyph }}
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
        <template v-else-if="canManageMessage">
          <button
            v-if="!editing"
            class="message-bubble__action"
            type="button"
            data-testid="message-start-edit"
            :disabled="actionPending"
            @click="emit('start-edit')"
          >
            编辑
          </button>
          <button
            v-if="!editing"
            class="message-bubble__action"
            type="button"
            data-testid="message-recall"
            :disabled="actionPending"
            @click="emit('recall')"
          >
            撤回
          </button>
        </template>
      </footer>
    </div>
  </div>
</template>

<style scoped>
.message-row {
  display: flex;
  gap: 8px;
  align-items: flex-end;
  margin-top: 11px;
}

.message-row.is-compact {
  margin-top: 4px;
}

.message-row.is-self {
  justify-content: flex-end;
}

.message-row.is-grouped {
  margin-top: 2px;
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
  max-width: min(64ch, 76%);
  padding: 9px 12px 8px;
  border: 1px solid var(--color-bubble-peer-line);
  border-radius: 16px 16px 16px 6px;
  background: var(--color-bubble-peer);
  box-shadow: var(--shadow-card);
}

.message-bubble.is-compact {
  padding: 7px 10px 6px;
}

.message-bubble.is-self {
  border-color: var(--color-bubble-self-line);
  border-radius: 16px 16px 6px 16px;
  background: var(--color-bubble-self);
}

.message-bubble.is-search-active {
  box-shadow:
    0 0 0 1px color-mix(in srgb, var(--color-primary) 28%, transparent),
    var(--shadow-float);
}

.message-bubble.is-grouped:not(.is-self) {
  border-top-left-radius: 6px;
}

.message-bubble.is-grouped-next:not(.is-self) {
  border-bottom-left-radius: 6px;
}

.message-bubble.is-self.is-grouped {
  border-top-right-radius: 6px;
}

.message-bubble.is-self.is-grouped-next {
  border-bottom-right-radius: 6px;
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
  margin-bottom: 4px;
  color: var(--color-shell-eyebrow);
  font-size: 0.7rem;
  font-weight: 600;
}

.message-bubble p {
  white-space: pre-wrap;
  font-size: 0.92rem;
  line-height: 1.45;
}

.message-bubble__highlight {
  padding: 0 0.08em;
  border-radius: 0.35rem;
  background: color-mix(in srgb, #ffe7a2 72%, #fff);
  color: #342100;
}

.message-bubble__meta {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  flex-wrap: wrap;
  gap: 5px;
  margin-top: 5px;
  color: color-mix(in srgb, var(--color-text-2) 82%, transparent);
  font: 500 0.64rem/1 var(--font-body);
}

.message-bubble__forward {
  margin-bottom: 6px;
  color: var(--color-text-soft);
  font-size: 0.72rem;
  line-height: 1.35;
}

.message-bubble__status {
  min-width: 17px;
  display: inline-flex;
  justify-content: flex-end;
  color: color-mix(in srgb, var(--color-text-2) 72%, transparent);
  font-size: 0.78rem;
  font-weight: 700;
  letter-spacing: -0.18em;
}

.message-bubble__status.is-read {
  color: var(--color-primary-strong);
}

.message-bubble__status.is-sent {
  letter-spacing: 0;
}

.message-bubble__status.is-group {
  color: color-mix(in srgb, #d6efff 86%, var(--color-primary));
}

.message-bubble__status.is-failed {
  color: var(--color-danger);
  letter-spacing: 0;
}

.message-bubble__status.is-sending {
  color: color-mix(in srgb, var(--color-text-soft) 82%, transparent);
  letter-spacing: 0;
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

.message-bubble__action {
  padding: 0;
  border: 0;
  background: transparent;
  color: var(--color-primary);
  font: inherit;
}

.message-bubble__action:hover,
.message-bubble__action:focus-visible {
  text-decoration: underline;
}

.message-bubble__edited {
  color: var(--color-text-soft);
}

.message-bubble__views {
  color: color-mix(in srgb, var(--color-text-2) 84%, transparent);
  font: 600 0.62rem/1 var(--font-mono);
  letter-spacing: 0.01em;
}

.message-bubble__editor {
  display: grid;
  gap: 8px;
}

.message-bubble__editor-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}

.message-bubble__editor-actions button {
  padding: 0;
  border: 0;
  background: transparent;
  color: var(--color-primary);
  font: 600 0.72rem/1 var(--font-body);
}

.message-bubble__editor-actions button:disabled,
.message-bubble__action:disabled {
  color: var(--color-text-soft);
}

.message-bubble__file {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 220px;
  padding: 2px 1px;
}

.message-bubble__preview {
  width: 46px;
  height: 46px;
  display: grid;
  place-items: center;
  border-radius: 14px;
  background: color-mix(in srgb, var(--color-primary) 9%, var(--color-bg-panel));
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
  margin-bottom: 3px;
  font-size: 0.88rem;
  line-height: 1.2;
}

.message-bubble__file p {
  color: var(--color-text-2);
  font-size: 0.78rem;
}

.message-bubble__file--image {
  min-width: 240px;
  padding: 10px;
  border-radius: 14px;
  background: color-mix(in srgb, var(--color-primary) 4%, var(--color-bg-panel));
}

@media (max-width: 767px) {
  .message-bubble {
    max-width: 92%;
  }
}
</style>
