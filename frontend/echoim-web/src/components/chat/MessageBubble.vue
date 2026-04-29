<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
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
  forwardSelectionMode?: boolean
  selectedForForward?: boolean
}>()

const emit = defineEmits<{
  retry: [clientMsgId: string]
  'start-edit': []
  'update:editing-draft': [value: string]
  'cancel-edit': []
  'save-edit': [value: string]
  recall: []
  reply: []
  forward: []
  'toggle-reaction': [emoji: string]
  'toggle-forward-selection': []
  'jump-to-source': [sourceMessageId: number]
}>()

type MessageContextCommand = 'copy' | 'reply' | 'forward' | 'edit' | 'recall' | 'retry'

const isSelf = computed(() => props.message.fromUserId === props.currentUserId)
const isSystem = computed(() => props.message.msgType === 'SYSTEM')
const contextMenuVisible = ref(false)
const contextMenuX = ref(0)
const contextMenuY = ref(0)
const QUICK_REACTIONS = ['👍', '❤️', '🔥', '😂', '👀']
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
  if (props.message.msgType === 'STICKER') {
    return {
      title: props.message.sticker?.title ?? props.message.file?.fileName ?? '贴纸',
      description: '贴纸消息',
    }
  }

  if (props.message.msgType === 'IMAGE') {
    return {
      title: props.message.file?.fileName ?? props.message.content ?? '图片预览',
      description: props.message.file?.contentType ?? '图片消息',
    }
  }

  if (props.message.msgType === 'GIF') {
    return {
      title: props.message.file?.fileName ?? props.message.content ?? 'GIF 预览',
      description: 'GIF 消息',
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
const replyLabel = computed(() => {
  if (!props.message.replySource) return ''
  const prefix = props.message.replySource.sourceMsgType === 'IMAGE'
    ? '[图片]'
    : props.message.replySource.sourceMsgType === 'FILE'
      ? '[文件]'
      : ''
  return props.message.replySource.sourcePreview || prefix || '原消息'
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
const canCopyMessage = computed(
  () => props.message.msgType === 'TEXT' && !props.message.recalled && Boolean(props.message.content?.trim()),
)
const canReactToMessage = computed(
  () => !props.message.recalled && props.message.messageId > 0 && props.message.sendStatus === 1,
)
const canForwardMessage = computed(
  () => !props.message.recalled && !props.forwardSelectionMode && props.message.messageId > 0 && props.message.sendStatus === 1,
)
const contextMenuActions = computed(() => {
  const actions: Array<{ key: MessageContextCommand; label: string; danger?: boolean; testId: string }> = []

  if (canCopyMessage.value) {
    actions.push({ key: 'copy', label: '复制', testId: 'message-context-copy' })
  }

  if (!props.message.recalled) {
    actions.push({ key: 'reply', label: '回复', testId: 'message-context-reply' })
  }

  if (canForwardMessage.value) {
    actions.push({ key: 'forward', label: '转发', testId: 'message-context-forward' })
  }

  if (canManageMessage.value && !props.editing) {
    actions.push({ key: 'edit', label: '编辑', testId: 'message-context-edit' })
    actions.push({ key: 'recall', label: '撤回', danger: true, testId: 'message-context-recall' })
  }

  if (isSelf.value && props.message.sendStatus === 2) {
    actions.push({ key: 'retry', label: '重试发送', testId: 'message-context-retry' })
  }

  return actions
})
const canOpenContextMenu = computed(() => !isSystem.value && (canReactToMessage.value || contextMenuActions.value.length > 0))

function formatFileSize(value: number | null) {
  if (!value || value <= 0) return ''
  if (value >= 1024 * 1024) return `${(value / 1024 / 1024).toFixed(1)} MB`
  if (value >= 1024) return `${Math.round(value / 1024)} KB`
  return `${value} B`
}

function openAttachment(url: string | null | undefined) {
  if (!url) return
  window.open(url, '_blank', 'noopener')
}

function closeContextMenu() {
  contextMenuVisible.value = false
}

function emitReaction(emoji: string) {
  closeContextMenu()
  emit('toggle-reaction', emoji)
}

async function copyMessageContent() {
  const content = props.message.content?.trim()
  if (!content) return

  try {
    await navigator.clipboard.writeText(content)
  } catch {
    return
  }
}

function openContextMenu(event: MouseEvent) {
  if (!canOpenContextMenu.value) return

  event.preventDefault()
  window.dispatchEvent(
    new CustomEvent('echoim:message-context-open', {
      detail: props.message.messageId,
    }),
  )

  const estimatedMenuWidth = 248
  const estimatedMenuHeight = canReactToMessage.value ? 220 : 176
  contextMenuX.value = Math.min(event.clientX, Math.max(16, window.innerWidth - estimatedMenuWidth))
  contextMenuY.value = Math.min(event.clientY, Math.max(16, window.innerHeight - estimatedMenuHeight))
  contextMenuVisible.value = true
}

function handleContextCommand(command: MessageContextCommand) {
  closeContextMenu()

  if (command === 'copy') {
    void copyMessageContent()
    return
  }

  if (command === 'reply') {
    emit('reply')
    return
  }

  if (command === 'forward') {
    emit('forward')
    return
  }

  if (command === 'edit') {
    emit('start-edit')
    return
  }

  if (command === 'recall') {
    emit('recall')
    return
  }

  if (props.message.clientMsgId) {
    emit('retry', props.message.clientMsgId)
  }
}

function handleGlobalPointerDown(event: PointerEvent) {
  if (!contextMenuVisible.value) return
  if (!(event.target instanceof Element)) return
  if (event.target.closest('.message-bubble__context-menu')) return
  closeContextMenu()
}

function handleGlobalKeydown(event: KeyboardEvent) {
  if (event.key === 'Escape') {
    closeContextMenu()
  }
}

function handleGlobalScroll() {
  closeContextMenu()
}

function handleExternalContextMenu(event: Event) {
  if ((event as CustomEvent<number>).detail === props.message.messageId) return
  closeContextMenu()
}

watch(contextMenuVisible, (visible) => {
  if (visible) {
    window.addEventListener('pointerdown', handleGlobalPointerDown)
    window.addEventListener('keydown', handleGlobalKeydown)
    window.addEventListener('scroll', handleGlobalScroll, true)
    return
  }

  window.removeEventListener('pointerdown', handleGlobalPointerDown)
  window.removeEventListener('keydown', handleGlobalKeydown)
  window.removeEventListener('scroll', handleGlobalScroll, true)
})

watch(
  () => [props.editing, props.message.recalled, props.message.sendStatus],
  () => {
    closeContextMenu()
  },
)

onMounted(() => {
  window.addEventListener('echoim:message-context-open', handleExternalContextMenu as EventListener)
})

onUnmounted(() => {
  closeContextMenu()
  window.removeEventListener('pointerdown', handleGlobalPointerDown)
  window.removeEventListener('keydown', handleGlobalKeydown)
  window.removeEventListener('scroll', handleGlobalScroll, true)
  window.removeEventListener('echoim:message-context-open', handleExternalContextMenu as EventListener)
})
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
    @contextmenu.prevent.stop="openContextMenu"
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
      <button
        v-if="forwardSelectionMode && !isSystem"
        class="message-bubble__selector"
        :class="{ 'is-selected': selectedForForward }"
        type="button"
        @click="emit('toggle-forward-selection')"
      >
        {{ selectedForForward ? '已选' : '选择' }}
      </button>
      <span v-if="showSenderLabel" class="message-bubble__sender">{{ senderName }}</span>
      <button
        v-if="message.replySource && !message.recalled"
        class="message-bubble__reply"
        type="button"
        @click="emit('jump-to-source', message.replySource.sourceMessageId)"
      >
        回复 #{{ message.replySource.sourceConversationId }} · {{ replyLabel }}
      </button>
      <div v-if="message.forwardSource && !message.recalled" class="message-bubble__forward">
        转发自 #{{ message.forwardSource.sourceConversationId }} · {{ message.forwardSource.sourcePreview || '消息' }}
      </div>
      <template v-if="message.msgType === 'SYSTEM'">
        <span>{{ message.content }}</span>
      </template>
      <template v-else-if="message.msgType === 'STICKER'">
        <div class="message-bubble__file message-bubble__file--sticker" @click="openAttachment(message.file?.downloadUrl)">
          <img
            v-if="message.file?.downloadUrl"
            class="message-bubble__image message-bubble__image--sticker"
            :src="message.file.downloadUrl"
            :alt="attachmentMeta?.title || '贴纸消息'"
          />
          <div class="message-bubble__file-copy">
            <strong>{{ attachmentMeta?.title }}</strong>
            <p>{{ attachmentMeta?.description }}</p>
          </div>
        </div>
      </template>
      <template v-else-if="message.msgType === 'IMAGE' || message.msgType === 'GIF'">
        <div class="message-bubble__file message-bubble__file--image" @click="openAttachment(message.file?.downloadUrl)">
          <img
            v-if="message.file?.downloadUrl"
            class="message-bubble__image"
            :src="message.file.downloadUrl"
            :alt="attachmentMeta?.title || '图片消息'"
          />
          <div v-else class="message-bubble__preview">
            <Picture />
          </div>
          <div class="message-bubble__file-copy">
            <strong>{{ attachmentMeta?.title }}</strong>
            <p>{{ attachmentMeta?.description }}</p>
          </div>
          <span v-if="message.msgType === 'GIF'" class="message-bubble__gif-badge">GIF</span>
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
            <button
              v-if="message.file?.downloadUrl"
              class="message-bubble__download"
              type="button"
              @click.stop="openAttachment(message.file?.downloadUrl)"
            >
              下载文件
            </button>
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
        <div v-else class="message-bubble__text-layout">
          <p class="message-bubble__text">
            <template v-for="(part, index) in highlightedBubbleText" :key="`${message.messageId}-${index}`">
              <mark v-if="part.matched" class="message-bubble__highlight">{{ part.text }}</mark>
              <template v-else>{{ part.text }}</template>
            </template>
          </p>
          <span class="message-bubble__meta message-bubble__meta--inline">
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
          </span>
        </div>
      </template>

      <footer
        v-if="!isSystem && message.msgType !== 'TEXT' && !message.recalled"
        class="message-bubble__meta"
      >
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
      </footer>
      <div v-if="message.reactions?.length" class="message-bubble__reactions">
        <button
          v-for="reaction in message.reactions"
          :key="`${message.messageId}-${reaction.emoji}`"
          class="message-bubble__reaction"
          :class="{ 'is-active': reaction.reacted }"
          type="button"
          @click="emitReaction(reaction.emoji)"
        >
          <span>{{ reaction.emoji }}</span>
          <strong>{{ reaction.count }}</strong>
        </button>
      </div>
    </div>
    <teleport to="body">
      <transition name="message-context-menu-fade">
        <div
          v-if="contextMenuVisible"
          class="message-bubble__context-menu"
          :style="{ left: `${contextMenuX}px`, top: `${contextMenuY}px` }"
          role="menu"
          aria-label="消息操作菜单"
        >
          <div v-if="canReactToMessage" class="message-bubble__reaction-menu">
            <button
              v-for="emoji in QUICK_REACTIONS"
              :key="emoji"
              class="message-bubble__reaction-pick"
              type="button"
              @click="emitReaction(emoji)"
            >
              {{ emoji }}
            </button>
          </div>
          <button
            v-for="action in contextMenuActions"
            :key="action.key"
            class="message-bubble__context-item"
            :class="{ 'is-danger': action.danger }"
            type="button"
            role="menuitem"
            :data-testid="action.testId"
            @click="handleContextCommand(action.key)"
          >
            {{ action.label }}
          </button>
        </div>
      </transition>
    </teleport>
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
  position: relative;
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

.message-bubble__selector {
  position: absolute;
  top: 10px;
  right: 10px;
  padding: 5px 9px;
  border: 1px solid color-mix(in srgb, var(--color-primary) 14%, var(--color-shell-border));
  border-radius: 999px;
  background: color-mix(in srgb, var(--color-shell-card) 76%, transparent);
  color: var(--color-text-2);
  font: 600 0.66rem/1 var(--font-body);
}

.message-bubble__selector.is-selected {
  border-color: color-mix(in srgb, var(--color-primary) 26%, var(--color-shell-border));
  background: color-mix(in srgb, var(--color-primary) 14%, var(--color-shell-card));
  color: var(--color-primary-strong);
}

.message-bubble p {
  margin: 0;
  white-space: pre-wrap;
  font-size: 0.92rem;
  line-height: 1.45;
}

.message-bubble__text-layout {
  color: var(--color-text-1);
}

.message-bubble__text {
  display: inline;
}

.message-bubble__highlight {
  padding: 0 0.08em;
  border-radius: 0.35rem;
  background: color-mix(in srgb, #ffe7a2 72%, #fff);
  color: #342100;
}

.message-bubble__meta {
  display: inline-flex;
  justify-content: flex-end;
  align-items: center;
  flex-wrap: wrap;
  gap: 5px;
  margin-top: 5px;
  color: color-mix(in srgb, var(--color-text-2) 82%, transparent);
  font: 500 0.64rem/1 var(--font-body);
}

.message-bubble__meta--inline {
  margin-top: 0;
  margin-left: 8px;
  vertical-align: bottom;
}

.message-bubble__forward {
  margin-bottom: 6px;
  color: var(--color-text-soft);
  font-size: 0.72rem;
  line-height: 1.35;
}

.message-bubble__reply {
  width: 100%;
  margin-bottom: 6px;
  padding: 8px 10px;
  border: 1px solid color-mix(in srgb, var(--color-primary) 10%, var(--color-shell-border));
  border-radius: 12px;
  background: color-mix(in srgb, var(--color-primary) 5%, transparent);
  color: var(--color-text-2);
  text-align: left;
  font: 500 0.72rem/1.35 var(--font-body);
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
.message-bubble__retry:disabled {
  color: var(--color-text-soft);
}

.message-bubble__file {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 220px;
  padding: 2px 1px;
}

.message-bubble__image {
  width: min(320px, 100%);
  max-height: 280px;
  object-fit: cover;
  border-radius: 12px;
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

.message-bubble__download {
  margin-top: 8px;
  padding: 0;
  border: 0;
  background: transparent;
  color: var(--color-primary);
  font: 600 0.72rem/1 var(--font-body);
}

.message-bubble__file--image {
  min-width: 240px;
  padding: 10px;
  border-radius: 14px;
  background: color-mix(in srgb, var(--color-primary) 4%, var(--color-bg-panel));
  cursor: pointer;
  flex-direction: column;
  align-items: stretch;
}

.message-bubble__file--sticker {
  min-width: 180px;
  padding: 8px;
  border-radius: 18px;
  background: color-mix(in srgb, var(--color-shell-card-strong) 86%, transparent);
  cursor: pointer;
  flex-direction: column;
  align-items: center;
}

.message-bubble__image--sticker {
  width: min(180px, 100%);
  max-height: 180px;
  object-fit: contain;
}

.message-bubble__gif-badge {
  align-self: flex-start;
  padding: 4px 8px;
  border-radius: 999px;
  background: color-mix(in srgb, var(--color-primary) 16%, var(--color-bg-panel));
  color: var(--color-primary-strong);
  font: 700 0.62rem/1 var(--font-mono);
}

.message-bubble__reactions {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: 8px;
}

.message-bubble__reaction,
.message-bubble__reaction-pick {
  border: 1px solid var(--color-shell-border);
  border-radius: 999px;
  background: color-mix(in srgb, var(--color-shell-card-strong) 92%, transparent);
}

.message-bubble__reaction {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 4px 9px;
  color: var(--color-text-1);
  font-size: 0.72rem;
}

.message-bubble__reaction.is-active {
  border-color: color-mix(in srgb, var(--color-primary) 38%, var(--color-shell-border));
  background: color-mix(in srgb, var(--color-primary) 12%, transparent);
}

.message-bubble__reaction strong {
  font: 700 0.64rem/1 var(--font-mono);
}

.message-bubble__reaction-menu {
  display: flex;
  gap: 8px;
  padding-bottom: 12px;
  margin-bottom: 10px;
  border-bottom: 1px solid color-mix(in srgb, var(--color-shell-border) 88%, transparent);
}

.message-bubble__reaction-pick {
  min-width: 34px;
  height: 34px;
  display: grid;
  place-items: center;
  padding: 0 10px;
  color: var(--color-text-1);
  font-size: 1rem;
}

.message-bubble__context-menu {
  position: fixed;
  z-index: 2200;
  min-width: 216px;
  padding: 12px;
  border: 1px solid color-mix(in srgb, var(--color-shell-border) 92%, transparent);
  border-radius: 18px;
  background: color-mix(in srgb, var(--color-shell-card) 96%, rgba(12, 14, 18, 0.94));
  box-shadow: var(--shadow-float);
  backdrop-filter: blur(18px);
}

.message-bubble__context-item {
  width: 100%;
  display: block;
  padding: 10px 12px;
  border: 0;
  border-radius: 12px;
  background: transparent;
  color: var(--color-text-1);
  text-align: left;
  font: 600 0.82rem/1.2 var(--font-body);
}

.message-bubble__context-item:hover,
.message-bubble__context-item:focus-visible {
  background: color-mix(in srgb, var(--color-primary) 10%, transparent);
}

.message-bubble__context-item.is-danger {
  color: var(--color-danger);
}

.message-bubble__context-item.is-danger:hover,
.message-bubble__context-item.is-danger:focus-visible {
  background: color-mix(in srgb, var(--color-danger) 10%, transparent);
}

.message-context-menu-fade-enter-active,
.message-context-menu-fade-leave-active {
  transition: opacity 0.16s ease, transform 0.16s ease;
}

.message-context-menu-fade-enter-from,
.message-context-menu-fade-leave-to {
  opacity: 0;
  transform: translateY(4px) scale(0.98);
}

@media (max-width: 767px) {
  .message-bubble {
    max-width: 92%;
  }
}
</style>
