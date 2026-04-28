<script setup lang="ts">
import { computed, nextTick, ref, watch } from 'vue'
import type { ChatMessage, ConversationType } from '@/types/chat'
import { formatMessageTime } from '@/utils/format'
import MessageBubble from './MessageBubble.vue'
import ChatStatePanel from './ChatStatePanel.vue'

const props = defineProps<{
  conversationId: number
  messages: ChatMessage[]
  currentUserId: number
  currentUserName: string
  conversationName: string
  conversationType: ConversationType
  compact?: boolean
  loading?: boolean
  errorMessage?: string | null
  hasOlderMessages?: boolean
  olderMessagesLoading?: boolean
  olderMessagesError?: string | null
  editingMessageId?: number | null
  editingDraft?: string
  messageActionPendingId?: number | null
}>()

const emit = defineEmits<{
  retry: []
  loadOlder: []
  retryMessage: [clientMsgId: string]
  'start-edit-message': [message: ChatMessage]
  'update:editing-draft': [value: string]
  'cancel-edit-message': []
  'save-edit-message': [payload: { messageId: number; content: string }]
  'recall-message': [messageId: number]
}>()

const scroller = ref()
const stickToBottom = ref(true)
const restoreAnchor = ref<{ scrollHeight: number; scrollTop: number } | null>(null)

function getWrapElement() {
  return scroller.value?.wrapRef as HTMLElement | undefined
}

function syncWrapAccessibility() {
  const wrap = getWrapElement()
  if (!wrap) return
  wrap.tabIndex = 0
  wrap.setAttribute('role', 'region')
  wrap.setAttribute('aria-label', '消息列表')
}

function scrollToBottom(force = false) {
  const wrap = getWrapElement()
  if (!wrap) return
  if (!force && !stickToBottom.value) return
  wrap.scrollTop = wrap.scrollHeight
}

function resolveSenderName(message: ChatMessage): string {
  const senderNameMap: Record<number, string> = {
    10001: props.currentUserName,
    10002: props.conversationType === 1 ? props.conversationName : '周序',
    10003: '宋眠',
    10004: '裴见',
    10005: '程原',
    10006: '沈曜',
  }

  return (
    senderNameMap[message.fromUserId] ??
    (message.fromUserId === props.currentUserId ? props.currentUserName : `用户 ${message.fromUserId}`)
  )
}

const decoratedMessages = computed(() =>
  props.messages.map((message, index) => {
    const previous = props.messages[index - 1]
    const next = props.messages[index + 1]
    const senderChanged = !previous || previous.fromUserId !== message.fromUserId
    const previousIsSystem = previous?.msgType === 'SYSTEM'
    const currentIsSystem = message.msgType === 'SYSTEM'
    const nextIsSystem = next?.msgType === 'SYSTEM'
    const previousTime = previous ? new Date(previous.sentAt).getTime() : 0
    const currentTime = new Date(message.sentAt).getTime()
    const minutesSincePrev = previous ? Math.floor((currentTime - previousTime) / 60000) : Number.MAX_SAFE_INTEGER
    const isGroupedWithPrev =
      !currentIsSystem &&
      Boolean(previous) &&
      !previousIsSystem &&
      !senderChanged &&
      minutesSincePrev <= 10

    const nextTime = next ? new Date(next.sentAt).getTime() : Number.MAX_SAFE_INTEGER
    const minutesToNext = next ? Math.floor((nextTime - currentTime) / 60000) : Number.MAX_SAFE_INTEGER
    const isGroupedWithNext =
      !currentIsSystem &&
      Boolean(next) &&
      !nextIsSystem &&
      next.fromUserId === message.fromUserId &&
      minutesToNext <= 10

    const showTimestampDivider =
      currentIsSystem ||
      !previous ||
      previousIsSystem ||
      minutesSincePrev >= 30

    return {
      message,
      isGroupedWithPrev,
      isGroupedWithNext,
      showAvatar: !currentIsSystem && !isGroupedWithNext && message.fromUserId !== props.currentUserId,
      showSenderLabel:
        props.conversationType === 2 &&
        !currentIsSystem &&
        !isGroupedWithPrev &&
        message.fromUserId !== props.currentUserId,
      senderName:
        resolveSenderName(message),
      showTimestampDivider,
      dividerLabel: currentIsSystem ? message.content ?? '系统消息' : formatTimelineLabel(message.sentAt),
    }
  }),
)

watch(
  () => props.messages.length,
  async () => {
    await nextTick()
    syncWrapAccessibility()
    if (restoreAnchor.value) {
      const wrap = getWrapElement()
      if (wrap) {
        wrap.scrollTop = wrap.scrollHeight - restoreAnchor.value.scrollHeight + restoreAnchor.value.scrollTop
      }
      restoreAnchor.value = null
      return
    }
    const latestMessage = props.messages[props.messages.length - 1]
    const shouldForce = latestMessage?.fromUserId === props.currentUserId
    scrollToBottom(shouldForce)
  },
)

watch(
  () => props.conversationId,
  async () => {
    stickToBottom.value = true
    await nextTick()
    syncWrapAccessibility()
    scrollToBottom(true)
  },
  { immediate: true },
)

function formatTimelineLabel(value: string): string {
  const date = new Date(value)
  const now = new Date()
  const sameDay = now.toDateString() === date.toDateString()
  return sameDay ? formatMessageTime(value) : `${date.getMonth() + 1}月${date.getDate()}日 ${formatMessageTime(value)}`
}

function handleScroll() {
  const wrap = getWrapElement()
  if (!wrap) return
  const remaining = wrap.scrollHeight - wrap.scrollTop - wrap.clientHeight
  stickToBottom.value = remaining < 48
}

function requestLoadOlder() {
  const wrap = getWrapElement()
  if (wrap) {
    restoreAnchor.value = {
      scrollHeight: wrap.scrollHeight,
      scrollTop: wrap.scrollTop,
    }
  }
  emit('loadOlder')
}
</script>

<template>
  <el-scrollbar
    ref="scroller"
    class="message-pane"
    :class="{ 'is-compact': compact }"
    id="chat-main-scroll"
    data-testid="message-pane"
    @scroll="handleScroll"
  >
    <div v-if="loading" class="message-pane__skeleton">
      <el-skeleton v-for="item in 5" :key="item" animated :rows="2" />
    </div>
    <ChatStatePanel
      v-else-if="errorMessage"
      title="消息加载失败"
      :description="errorMessage"
      action-label="重新加载"
      role="alert"
      aria-live="assertive"
      @action="emit('retry')"
    />
    <div v-else-if="decoratedMessages.length" class="message-pane__stack">
      <div class="message-pane__history">
        <el-button
          v-if="hasOlderMessages || olderMessagesLoading"
          text
          :loading="olderMessagesLoading"
          data-testid="message-load-older"
          @click="requestLoadOlder"
        >
          加载更早消息
        </el-button>
        <p v-else class="message-pane__history-tip">已经到最早的消息了</p>
        <p v-if="olderMessagesError" class="message-pane__history-error" role="status">{{ olderMessagesError }}</p>
      </div>
      <template v-for="entry in decoratedMessages" :key="entry.message.messageId">
        <div v-if="entry.showTimestampDivider" class="message-pane__divider">
          <span>{{ entry.dividerLabel }}</span>
        </div>
        <MessageBubble
          v-if="entry.message.msgType !== 'SYSTEM'"
          :message="entry.message"
          :current-user-id="currentUserId"
          :sender-name="entry.senderName"
          :show-avatar="entry.showAvatar"
          :show-sender-label="entry.showSenderLabel"
          :grouped-with-prev="entry.isGroupedWithPrev"
          :grouped-with-next="entry.isGroupedWithNext"
          :compact="compact"
          :editing="editingMessageId === entry.message.messageId"
          :editing-draft="editingDraft ?? ''"
          :action-pending="messageActionPendingId === entry.message.messageId"
          @retry="emit('retryMessage', $event)"
          @start-edit="emit('start-edit-message', entry.message)"
          @update:editing-draft="emit('update:editing-draft', $event)"
          @cancel-edit="emit('cancel-edit-message')"
          @save-edit="emit('save-edit-message', { messageId: entry.message.messageId, content: $event })"
          @recall="emit('recall-message', entry.message.messageId)"
        />
      </template>
    </div>
    <ChatStatePanel
      v-else
      title="这里还没有消息"
      description="从输入区发送第一条消息，文件和图片消息也会在这里显示。"
      role="status"
      aria-live="polite"
    />
  </el-scrollbar>
</template>

<style scoped>
.message-pane {
  position: relative;
  z-index: 1;
  min-height: 0;
  padding: 14px 24px 8px;
  background: transparent;
}

.message-pane.is-compact {
  padding-top: 12px;
}

.message-pane__stack,
.message-pane__skeleton {
  display: grid;
  gap: 4px;
  width: min(760px, 100%);
  margin: 0 auto;
}

.message-pane__history {
  display: grid;
  justify-items: center;
  gap: 6px;
  margin-bottom: 6px;
}

.message-pane__history-tip,
.message-pane__history-error {
  color: var(--color-text-soft);
  font-size: 0.76rem;
}

.message-pane__history-error {
  color: var(--color-danger);
}

.message-pane.is-compact .message-pane__stack {
  gap: 0;
}

.message-pane__divider {
  display: flex;
  justify-content: center;
  margin: 10px 0 12px;
}

.message-pane__divider span {
  padding: 7px 14px;
  border: 0;
  border-radius: 999px;
  background: rgba(102, 61, 142, 0.9);
  color: #fff;
  font: 600 0.7rem/1.2 var(--font-body);
  box-shadow: 0 10px 24px rgba(48, 25, 72, 0.24);
}

@media (max-width: 767px) {
  .message-pane {
    padding: 14px 12px 8px;
  }
}
</style>
