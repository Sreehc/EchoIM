<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import { Document, Picture } from '@element-plus/icons-vue'
import type { ChatMessage, ConversationType } from '@/types/chat'
import { formatMessageTime, highlightBubbleContent } from '@/utils/format'
import { findStickerDefinition } from '@/stickers/library'
import AvatarBadge from './AvatarBadge.vue'
import VoicePlayer from './VoicePlayer.vue'

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
  'open-image-viewer': [messageId: number, imageUrl: string]
  'view-profile': [userId: number]
  pin: []
  unpin: []
}>()

type MessageContextCommand = 'copy' | 'reply' | 'forward' | 'edit' | 'recall' | 'retry' | 'pin' | 'unpin'

const isSelf = computed(() => props.message.fromUserId === props.currentUserId)
const isSystem = computed(() => props.message.msgType === 'SYSTEM')
const stickerDefinition = computed(() => findStickerDefinition(props.message.sticker?.stickerId))
const stickerTitle = computed(() => props.message.sticker?.title ?? attachmentMeta.value?.title ?? '贴纸')
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
const channelViewCount = computed(() => {
  if (!isSelf.value || props.conversationType !== 3 || props.message.sendStatus !== 1) {
    return null
  }

  return props.message.viewCount ?? 0
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
    return [{ text: bubbleText.value, matched: false, mention: false }]
  }

  return highlightBubbleContent(
    bubbleText.value,
    props.searchQuery,
    props.message.mentions,
  )
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

  if (!props.message.recalled && props.message.messageId > 0 && props.message.sendStatus === 1) {
    if (props.message.pinned) {
      actions.push({ key: 'unpin', label: '取消置顶', testId: 'message-context-unpin' })
    } else {
      actions.push({ key: 'pin', label: '置顶', testId: 'message-context-pin' })
    }
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
const imageDisplayUrl = computed(() => {
  if (props.message.msgType === 'IMAGE') {
    return props.message.file?.thumbnailUrl || props.message.file?.downloadUrl || null
  }
  return props.message.file?.downloadUrl || null
})

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

function handleMentionClick(userId: number) {
  emit('view-profile', userId)
}

function handleImageClick() {
  if (props.message.msgType === 'IMAGE') {
    const fullUrl = props.message.file?.downloadUrl || props.message.file?.url
    if (fullUrl) {
      emit('open-image-viewer', props.message.messageId, fullUrl)
    }
  } else {
    openAttachment(props.message.file?.downloadUrl)
  }
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

  if (command === 'pin') {
    emit('pin')
    return
  }

  if (command === 'unpin') {
    emit('unpin')
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
      size="md"
      type="user"
    />
    <div v-else-if="!isSystem && !isSelf && conversationType !== 1" class="message-row__avatar-spacer"></div>
    <button
      v-if="forwardSelectionMode && !isSystem"
      class="message-row__selector"
      :class="{ 'is-selected': selectedForForward }"
      type="button"
      :aria-pressed="selectedForForward"
      :aria-label="selectedForForward ? '取消选择这条消息' : '选择这条消息用于转发'"
      @click="emit('toggle-forward-selection')"
    >
      <span class="message-row__selector-circle" aria-hidden="true">
        <span class="message-row__selector-check">✓</span>
      </span>
    </button>
    <div
      class="message-bubble"
      :class="{
        'is-self': isSelf,
        'is-system': isSystem,
        'is-grouped': groupedWithPrev,
        'is-grouped-next': groupedWithNext,
        'is-compact': compact,
        'is-search-active': searchActive,
      }"
    >
      <span v-if="showSenderLabel" class="message-bubble__sender">{{ senderName }}</span>
      <span v-if="message.pinned" class="message-bubble__pinned-badge" aria-label="已置顶">
        <svg viewBox="0 0 16 16" fill="none" aria-hidden="true">
          <path d="M10.5 2l3.5 3.5-4.3 4.3L10.5 14l-1.3-4.3L5 13.5l.7-4.3L1.5 5 10.5 2z" stroke="currentColor" stroke-width="1.2" stroke-linejoin="round"/>
        </svg>
      </span>
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
        <div class="message-bubble__file message-bubble__file--sticker">
          <div
            v-if="stickerDefinition"
            class="message-bubble__sticker-art"
            :aria-label="stickerTitle"
            role="img"
            v-html="stickerDefinition.svg"
          ></div>
          <div class="message-bubble__file-copy">
            <strong>{{ stickerTitle }}</strong>
            <p>{{ stickerDefinition ? attachmentMeta?.description : '当前客户端未内置这张贴纸，将按文本回退显示。' }}</p>
          </div>
        </div>
      </template>
      <template v-else-if="message.msgType === 'IMAGE' || message.msgType === 'GIF'">
        <div class="message-bubble__file message-bubble__file--image" @click="handleImageClick">
          <img
            v-if="imageDisplayUrl"
            class="message-bubble__image"
            v-lazy-image="imageDisplayUrl"
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
      <template v-else-if="message.msgType === 'VOICE'">
        <div class="message-bubble__voice">
          <VoicePlayer
            :audio-url="message.file?.downloadUrl ?? null"
            :duration="message.voice?.duration ?? 0"
            :waveform="message.voice?.waveform ?? []"
            :self="isSelf"
          />
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
              <mark v-if="part.mention && part.mentionUserId" class="message-bubble__mention" @click.stop="handleMentionClick(part.mentionUserId!)">{{ part.text }}</mark>
              <mark v-else-if="part.matched" class="message-bubble__highlight">{{ part.text }}</mark>
              <template v-else>{{ part.text }}</template>
            </template>
          </p>
          <span class="message-bubble__meta message-bubble__meta--inline">
            <span>{{ formatMessageTime(message.sentAt) }}</span>
            <span v-if="message.edited && !message.recalled" class="message-bubble__edited">已编辑</span>
            <span v-if="channelViewCount !== null" class="message-bubble__views">
              <svg class="message-bubble__views-icon" viewBox="0 0 16 16" aria-hidden="true">
                <path d="M1.5 8s2.6-4.5 6.5-4.5S14.5 8 14.5 8s-2.6 4.5-6.5 4.5S1.5 8 1.5 8Z" fill="none" stroke="currentColor" stroke-width="1.3" stroke-linecap="round" stroke-linejoin="round"/>
                <circle cx="8" cy="8" r="2.2" fill="none" stroke="currentColor" stroke-width="1.3"/>
              </svg>
              <span>{{ channelViewCount }}</span>
            </span>
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
        <span v-if="channelViewCount !== null" class="message-bubble__views">
          <svg class="message-bubble__views-icon" viewBox="0 0 16 16" aria-hidden="true">
            <path d="M1.5 8s2.6-4.5 6.5-4.5S14.5 8 14.5 8s-2.6 4.5-6.5 4.5S1.5 8 1.5 8Z" fill="none" stroke="currentColor" stroke-width="1.3" stroke-linecap="round" stroke-linejoin="round"/>
            <circle cx="8" cy="8" r="2.2" fill="none" stroke="currentColor" stroke-width="1.3"/>
          </svg>
          <span>{{ channelViewCount }}</span>
        </span>
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
  gap: 9px;
  align-items: flex-end;
  margin-top: 12px;
}

.message-row.is-compact {
  margin-top: 6px;
}

.message-row.is-self {
  justify-content: flex-end;
}

.message-row.is-grouped {
  margin-top: 3px;
}

.message-row.is-system {
  justify-content: center;
  margin-top: 14px;
}

.message-row__avatar {
  margin-bottom: 2px;
}

.message-row__avatar-spacer {
  width: 34px;
  flex-shrink: 0;
}

.message-row__selector {
  width: 32px;
  height: 32px;
  flex: 0 0 32px;
  display: grid;
  place-items: center;
  align-self: center;
  padding: 0;
  border: 0;
  background: transparent;
  color: var(--text-quaternary);
  transition:
    transform var(--motion-fast) var(--ease-out),
    color var(--motion-fast) var(--ease-out);
}

.message-row__selector:hover {
  transform: translateY(-1px);
  color: var(--interactive-primary-bg);
}

.message-row__selector:focus-visible {
  outline: none;
}

.message-row__selector-circle {
  width: 24px;
  height: 24px;
  display: grid;
  place-items: center;
  border: 1.5px solid color-mix(in srgb, var(--interactive-primary-bg) 18%, var(--border-strong));
  border-radius: 999px;
  background: color-mix(in srgb, var(--surface-panel) 94%, transparent);
  box-shadow: var(--shadow-xs);
  transition:
    border-color var(--motion-fast) var(--ease-out),
    background-color var(--motion-fast) var(--ease-out),
    box-shadow var(--motion-fast) var(--ease-out),
    transform var(--motion-fast) var(--ease-out);
}

.message-row__selector-check {
  opacity: 0;
  transform: scale(0.72);
  color: var(--text-on-brand);
  font: 700 var(--text-xs)/1 var(--font-mono);
  transition:
    opacity var(--motion-fast) var(--ease-out),
    transform var(--motion-fast) var(--ease-out);
}

.message-row__selector:hover .message-row__selector-circle {
  border-color: color-mix(in srgb, var(--interactive-primary-bg) 44%, var(--border-default));
  background: color-mix(in srgb, var(--interactive-primary-bg) 7%, var(--surface-card));
}

.message-row__selector:focus-visible .message-row__selector-circle {
  box-shadow:
    0 0 0 1px color-mix(in srgb, var(--interactive-primary-bg) 18%, transparent),
    0 0 0 4px color-mix(in srgb, var(--interactive-focus-ring) 58%, transparent);
}

.message-row__selector.is-selected .message-row__selector-circle {
  border-color: var(--interactive-primary-bg);
  background: var(--interactive-primary-bg);
  box-shadow:
    0 10px 20px color-mix(in srgb, var(--interactive-primary-bg) 22%, transparent),
    var(--shadow-xs);
}

.message-row__selector.is-selected .message-row__selector-check {
  opacity: 1;
  transform: scale(1);
}

.message-bubble {
  position: relative;
  max-width: min(56ch, 68%);
  padding: 10px 13px 9px;
  border: 1px solid var(--chat-bubble-peer-border);
  border-radius: var(--radius-bubble) var(--radius-bubble) var(--radius-bubble) 12px;
  background: var(--chat-bubble-peer-bg);
  box-shadow: var(--shadow-xs);
  content-visibility: auto;
  contain-intrinsic-size: auto 60px;
}

.message-bubble.is-compact {
  padding: 8px 10px 7px;
}

.message-bubble.is-self {
  border-color: var(--chat-bubble-self-border);
  border-radius: var(--radius-bubble) var(--radius-bubble) 12px var(--radius-bubble);
  background: var(--chat-bubble-self-bg);
}

.message-bubble.is-search-active {
  box-shadow:
    0 0 0 1px color-mix(in srgb, var(--interactive-primary-bg) 14%, transparent),
    0 0 0 4px color-mix(in srgb, var(--interactive-focus-ring) 44%, transparent),
    0 1px 2px rgba(15, 23, 42, 0.025);
}

.message-bubble.is-grouped:not(.is-self) {
  border-top-left-radius: 10px;
}

.message-bubble.is-grouped-next:not(.is-self) {
  border-bottom-left-radius: 10px;
}

.message-bubble.is-self.is-grouped {
  border-top-right-radius: 10px;
}

.message-bubble.is-self.is-grouped-next {
  border-bottom-right-radius: 10px;
}

.message-bubble.is-system {
  max-width: unset;
  padding: 0;
  border: 0;
  background: transparent;
  color: var(--text-tertiary);
  font: 500 var(--text-xs)/1.1 var(--font-mono);
  letter-spacing: 0.04em;
}

.message-bubble__sender {
  display: inline-block;
  margin-bottom: 4px;
  color: var(--text-quaternary);
  font: 600 var(--text-xs)/1 var(--font-mono);
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.message-bubble p {
  margin: 0;
  white-space: pre-wrap;
  font-size: var(--text-base);
  line-height: 1.58;
  letter-spacing: -0.008em;
}

.message-bubble__text-layout {
  display: block;
  color: var(--text-primary);
}

.message-bubble__text {
  display: inline;
}

.message-bubble__highlight {
  padding: 0 0.08em;
  border-radius: 6px;
  background: color-mix(in srgb, var(--interactive-selected-bg) 72%, white);
  color: var(--text-primary);
}

.message-bubble__mention {
  padding: 0 0.08em;
  border-radius: 6px;
  background: color-mix(in srgb, var(--interactive-primary-bg) 12%, transparent);
  color: var(--interactive-selected-fg);
  font-weight: 600;
  cursor: pointer;
  text-decoration: none;
  transition: background-color var(--motion-fast) var(--ease-out);
}

.message-bubble__mention:hover {
  background: color-mix(in srgb, var(--interactive-primary-bg) 22%, transparent);
}

.message-bubble__meta {
  display: inline-flex;
  justify-content: flex-end;
  align-items: center;
  flex-wrap: wrap;
  gap: 4px;
  margin-top: 4px;
  color: color-mix(in srgb, var(--text-tertiary) 84%, transparent);
  font: 500 var(--text-xs)/1 var(--font-mono);
  letter-spacing: 0.04em;
}

.message-bubble__meta--inline {
  margin-top: 0;
  margin-left: 7px;
  white-space: nowrap;
}

.message-bubble__forward {
  margin-bottom: 5px;
  color: var(--text-tertiary);
  font-size: var(--text-xs);
  line-height: 1.38;
}

.message-bubble__reply {
  width: 100%;
  margin-bottom: 6px;
  padding: 7px 10px;
  border: 1px solid color-mix(in srgb, var(--interactive-primary-bg) 8%, var(--border-subtle));
  border-radius: 12px;
  background: color-mix(in srgb, var(--interactive-primary-bg) 4%, var(--surface-panel));
  color: var(--text-secondary);
  text-align: left;
  font: 500 var(--text-xs)/1.42 var(--font-body);
}

.message-bubble__status {
  min-width: 15px;
  display: inline-flex;
  justify-content: flex-end;
  color: color-mix(in srgb, var(--text-tertiary) 72%, transparent);
  font-size: var(--text-sm);
  font-weight: 700;
  letter-spacing: -0.18em;
}

.message-bubble__status.is-read {
  color: var(--interactive-selected-fg);
}

.message-bubble__status.is-sent {
  letter-spacing: 0;
}

.message-bubble__status.is-group {
  color: color-mix(in srgb, #d6efff 86%, var(--interactive-primary-bg));
}

.message-bubble__status.is-failed {
  color: var(--status-danger);
  letter-spacing: 0;
}

.message-bubble__status.is-sending {
  color: color-mix(in srgb, var(--text-quaternary) 82%, transparent);
  letter-spacing: 0;
}

.message-bubble__retry {
  min-height: var(--btn-min-size);
  display: inline-flex;
  align-items: center;
  padding: 0;
  border: 0;
  background: transparent;
  color: var(--interactive-primary-bg);
  font: inherit;
}

.message-bubble__retry:hover,
.message-bubble__retry:focus-visible {
  text-decoration: underline;
}

.message-bubble__edited {
  color: var(--text-quaternary);
}

.message-bubble__views {
  display: inline-flex;
  align-items: center;
  gap: 3px;
  color: color-mix(in srgb, var(--text-tertiary) 84%, transparent);
  font: 600 var(--text-xs)/1 var(--font-mono);
  letter-spacing: 0.03em;
}

.message-bubble__views-icon {
  width: 13px;
  height: 13px;
  flex-shrink: 0;
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
  color: var(--interactive-primary-bg);
  font: 600 var(--text-sm)/1 var(--font-body);
}

.message-bubble__editor-actions button:disabled,
.message-bubble__retry:disabled {
  color: var(--text-quaternary);
}

.message-bubble__file {
  display: flex;
  align-items: center;
  gap: 11px;
  min-width: 220px;
  padding: 2px 0;
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
  border-radius: 12px;
  background: color-mix(in srgb, var(--interactive-primary-bg) 7%, var(--surface-subtle));
  flex-shrink: 0;
}

.message-bubble__preview svg {
  width: 18px;
  height: 18px;
  color: var(--interactive-primary-bg);
}

.message-bubble__preview--file {
  background: color-mix(in srgb, var(--text-primary) 6%, var(--surface-subtle));
}

.message-bubble__file-copy {
  min-width: 0;
}

.message-bubble__file strong {
  display: block;
  margin-bottom: 2px;
  font-size: var(--text-sm);
  line-height: 1.2;
}

.message-bubble__file p {
  color: var(--text-secondary);
  font-size: var(--text-sm);
}

.message-bubble__download {
  margin-top: 8px;
  padding: 0;
  border: 0;
  background: transparent;
  color: var(--interactive-primary-bg);
  font: 600 var(--text-sm)/1 var(--font-body);
}

.message-bubble__file--image {
  min-width: 240px;
  padding: 8px;
  border-radius: 12px;
  background: color-mix(in srgb, var(--interactive-primary-bg) 3%, var(--surface-subtle));
  cursor: pointer;
  flex-direction: column;
  align-items: stretch;
}

.message-bubble__file--sticker {
  min-width: 180px;
  padding: 8px;
  border-radius: 14px;
  background: color-mix(in srgb, var(--surface-card) 84%, transparent);
  cursor: pointer;
  flex-direction: column;
  align-items: center;
}

.message-bubble__image--sticker {
  width: min(180px, 100%);
  max-height: 180px;
  object-fit: contain;
}

.message-bubble__sticker-art {
  width: min(180px, 100%);
  display: flex;
  align-items: center;
  justify-content: center;
}

.message-bubble__sticker-art :deep(svg) {
  display: block;
  width: 100%;
  height: auto;
}

.message-bubble__voice {
  min-width: 220px;
  padding: 4px 2px;
}

.message-bubble__pinned-badge {
  position: absolute;
  top: 4px;
  right: 4px;
  width: 18px;
  height: 18px;
  display: grid;
  place-items: center;
  color: var(--text-quaternary);
  opacity: 0.6;
}

.message-bubble__pinned-badge svg {
  width: 14px;
  height: 14px;
}

.message-bubble__gif-badge {
  align-self: flex-start;
  padding: 4px 8px;
  border-radius: var(--radius-pill);
  background: color-mix(in srgb, var(--interactive-primary-bg) 12%, var(--surface-subtle));
  color: var(--interactive-selected-fg);
  font: 700 var(--text-xs)/1 var(--font-mono);
}

.message-bubble__reactions {
  display: flex;
  flex-wrap: wrap;
  gap: 5px;
  margin-top: 5px;
}

.message-bubble__reaction,
.message-bubble__reaction-pick {
  border: 0;
  border-radius: var(--radius-pill);
  background: color-mix(in srgb, var(--surface-card) 86%, transparent);
}

.message-bubble__reaction {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 2px 8px;
  color: var(--text-primary);
  font-size: var(--text-base);
  line-height: 1;
  transition:
    background var(--motion-fast) var(--motion-ease-out);
}

.message-bubble__reaction:hover {
  background: color-mix(in srgb, var(--interactive-selected-bg) 80%, transparent);
}

.message-bubble__reaction.is-active {
  background: color-mix(in srgb, var(--interactive-primary-bg) 14%, transparent);
  color: var(--interactive-selected-fg);
}

.message-bubble__reaction strong {
  font: 700 var(--text-xs)/1 var(--font-mono);
}

.message-bubble__reaction-menu {
  display: flex;
  gap: 6px;
  padding-bottom: 8px;
  margin-bottom: 6px;
  border-bottom: 1px solid color-mix(in srgb, var(--border-subtle) 84%, transparent);
}

.message-bubble__reaction-pick {
  min-width: 30px;
  height: 30px;
  display: grid;
  place-items: center;
  padding: 0 6px;
  color: var(--text-primary);
  font-size: var(--text-lg);
  cursor: pointer;
  transition:
    background var(--motion-fast) var(--motion-ease-out);
}

.message-bubble__reaction-pick:hover {
  background: color-mix(in srgb, var(--interactive-selected-bg) 80%, transparent);
}

.message-bubble__context-menu {
  position: fixed;
  z-index: 2200;
  min-width: 216px;
  padding: 10px;
  border: 1px solid color-mix(in srgb, var(--border-default) 92%, transparent);
  border-radius: var(--radius-panel);
  background: color-mix(in srgb, var(--surface-overlay) 94%, rgba(12, 14, 18, 0.94));
  box-shadow: var(--shadow-md);
  backdrop-filter: blur(16px);
}

.message-bubble__context-item {
  width: 100%;
  display: block;
  padding: 9px 11px;
  border: 0;
  border-radius: 11px;
  background: transparent;
  color: var(--text-primary);
  text-align: left;
  font: 600 var(--text-sm)/1.2 var(--font-body);
}

.message-bubble__context-item:hover,
.message-bubble__context-item:focus-visible {
  background: color-mix(in srgb, var(--interactive-selected-bg) 90%, transparent);
}

.message-bubble__context-item.is-danger {
  color: var(--status-danger);
}

.message-bubble__context-item.is-danger:hover,
.message-bubble__context-item.is-danger:focus-visible {
  background: color-mix(in srgb, var(--status-danger) 10%, transparent);
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
  .message-row__selector {
    width: 32px;
    height: 32px;
    flex-basis: 32px;
  }

  .message-row__selector-circle {
    width: 22px;
    height: 22px;
  }

  .message-bubble {
    max-width: 92%;
  }
}
</style>
