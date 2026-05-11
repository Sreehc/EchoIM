<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { Clock, Close } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { ScheduledMessage } from '@/types/chat'
import { SCHEDULED_MESSAGE_STATUS } from '@/types/chat'
import * as scheduledMessageService from '@/services/scheduledMessages'

const props = defineProps<{
  conversationId: number | null
  visible: boolean
}>()

const emit = defineEmits<{
  close: []
  'message-sent': []
}>()

const loading = ref(false)
const scheduledMessages = ref<ScheduledMessage[]>([])

const pendingMessages = computed(() =>
  scheduledMessages.value.filter((m) => m.status === SCHEDULED_MESSAGE_STATUS.PENDING)
)

async function loadScheduledMessages() {
  if (!props.conversationId) return

  loading.value = true
  try {
    scheduledMessages.value = await scheduledMessageService.fetchScheduledMessages(props.conversationId)
  } catch (error) {
    ElMessage.error('加载定时消息失败')
  } finally {
    loading.value = false
  }
}

async function handleCancel(id: number) {
  try {
    await ElMessageBox.confirm('确定要取消这条定时消息吗？', '取消定时消息', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    })
    await scheduledMessageService.cancelScheduledMessage(id)
    ElMessage.success('定时消息已取消')
    await loadScheduledMessages()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error instanceof Error ? error.message : '取消失败')
    }
  }
}

async function handleSendNow(id: number) {
  try {
    await ElMessageBox.confirm('确定要立即发送这条定时消息吗？', '立即发送', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'info',
    })
    await scheduledMessageService.sendScheduledMessageNow(id)
    ElMessage.success('消息已发送')
    await loadScheduledMessages()
    emit('message-sent')
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error instanceof Error ? error.message : '发送失败')
    }
  }
}

function formatScheduledTime(time: string) {
  const date = new Date(time)
  const now = new Date()
  const isToday = date.toDateString() === now.toDateString()
  const isTomorrow = new Date(now.getTime() + 24 * 60 * 60 * 1000).toDateString() === date.toDateString()

  const timeStr = date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })

  if (isToday) {
    return `今天 ${timeStr}`
  } else if (isTomorrow) {
    return `明天 ${timeStr}`
  } else {
    return date.toLocaleDateString('zh-CN', { month: 'short', day: 'numeric' }) + ' ' + timeStr
  }
}

function formatMessageType(msgType: number) {
  switch (msgType) {
    case 1: return '文本'
    case 2: return '表情'
    case 3: return '图片'
    case 4: return 'GIF'
    case 5: return '文件'
    case 7: return '语音'
    default: return '消息'
  }
}

watch(
  () => [props.visible, props.conversationId],
  () => {
    if (props.visible && props.conversationId) {
      loadScheduledMessages()
    }
  },
  { immediate: true }
)
</script>

<template>
  <transition name="panel-slide">
    <div v-if="visible" class="scheduled-panel">
      <div class="scheduled-panel__header">
        <div class="scheduled-panel__title">
          <Clock />
          <strong>定时消息</strong>
          <span v-if="pendingMessages.length">{{ pendingMessages.length }} 条待发送</span>
        </div>
        <button class="scheduled-panel__close" type="button" aria-label="关闭" @click="emit('close')">
          <Close />
        </button>
      </div>

      <div class="scheduled-panel__body">
        <div v-if="loading" class="scheduled-panel__loading">加载中...</div>
        <div v-else-if="!pendingMessages.length" class="scheduled-panel__empty">
          <p>暂无定时消息</p>
          <p class="scheduled-panel__hint">在输入框中点击时钟图标可设置定时发送</p>
        </div>
        <div v-else class="scheduled-panel__list">
          <div v-for="message in pendingMessages" :key="message.id" class="scheduled-item">
            <div class="scheduled-item__content">
              <div class="scheduled-item__meta">
                <span class="scheduled-item__type">{{ formatMessageType(message.msgType) }}</span>
                <span class="scheduled-item__time">{{ formatScheduledTime(message.scheduledAt) }}</span>
              </div>
              <p class="scheduled-item__text">{{ message.content || '(无文本内容)' }}</p>
            </div>
            <div class="scheduled-item__actions">
              <button
                class="scheduled-item__action scheduled-item__action--send"
                type="button"
                @click="handleSendNow(message.id)"
              >
                立即发送
              </button>
              <button
                class="scheduled-item__action scheduled-item__action--cancel"
                type="button"
                @click="handleCancel(message.id)"
              >
                取消
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>
  </transition>
</template>

<style scoped>
.scheduled-panel {
  position: absolute;
  bottom: 100%;
  left: 0;
  right: 0;
  max-height: 360px;
  border: 1px solid var(--border-default);
  border-radius: var(--radius-card) var(--radius-card) 0 0;
  background: var(--surface-overlay);
  box-shadow: var(--shadow-md);
  z-index: 10;
  display: flex;
  flex-direction: column;
}

.scheduled-panel__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  border-bottom: 1px solid var(--border-subtle);
}

.scheduled-panel__title {
  display: flex;
  align-items: center;
  gap: 8px;
}

.scheduled-panel__title svg {
  width: 18px;
  height: 18px;
  color: var(--text-tertiary);
}

.scheduled-panel__title strong {
  font-size: var(--text-sm);
  font-weight: 600;
}

.scheduled-panel__title span {
  font-size: var(--text-xs);
  color: var(--text-tertiary);
}

.scheduled-panel__close {
  width: 28px;
  height: 28px;
  display: grid;
  place-items: center;
  border: 0;
  border-radius: var(--radius-control);
  background: transparent;
  color: var(--text-tertiary);
  cursor: pointer;
  transition: background var(--motion-fast) var(--motion-ease-out);
}

.scheduled-panel__close:hover {
  background: var(--interactive-secondary-bg-hover);
}

.scheduled-panel__close svg {
  width: 16px;
  height: 16px;
}

.scheduled-panel__body {
  flex: 1;
  overflow-y: auto;
  padding: 12px 16px;
}

.scheduled-panel__loading,
.scheduled-panel__empty {
  text-align: center;
  padding: 24px 0;
  color: var(--text-tertiary);
  font-size: var(--text-sm);
}

.scheduled-panel__hint {
  margin-top: 8px;
  font-size: var(--text-xs);
  color: var(--text-quaternary);
}

.scheduled-panel__list {
  display: grid;
  gap: 10px;
}

.scheduled-item {
  display: grid;
  gap: 10px;
  padding: 12px;
  border: 1px solid var(--border-default);
  border-radius: var(--radius-md);
  background: color-mix(in srgb, var(--surface-panel) 94%, transparent);
}

.scheduled-item__meta {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 6px;
}

.scheduled-item__type {
  padding: 2px 6px;
  border-radius: 4px;
  background: color-mix(in srgb, var(--interactive-primary-bg) 12%, transparent);
  color: var(--interactive-selected-fg);
  font-size: var(--text-xs);
  font-weight: 600;
}

.scheduled-item__time {
  font-size: var(--text-xs);
  color: var(--text-tertiary);
}

.scheduled-item__text {
  font-size: var(--text-sm);
  color: var(--text-primary);
  line-height: 1.46;
  word-break: break-word;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.scheduled-item__actions {
  display: flex;
  gap: 8px;
}

.scheduled-item__action {
  flex: 1;
  padding: 6px 12px;
  border: 1px solid var(--border-default);
  border-radius: var(--radius-control);
  font-size: var(--text-xs);
  font-weight: 600;
  cursor: pointer;
  transition: background var(--motion-fast) var(--motion-ease-out);
}

.scheduled-item__action--send {
  background: var(--interactive-primary-bg);
  color: white;
  border-color: var(--interactive-primary-bg);
}

.scheduled-item__action--send:hover {
  background: var(--interactive-primary-bg-hover);
}

.scheduled-item__action--cancel {
  background: transparent;
  color: var(--text-secondary);
}

.scheduled-item__action--cancel:hover {
  background: var(--interactive-secondary-bg-hover);
}

.panel-slide-enter-active,
.panel-slide-leave-active {
  transition: transform 0.2s ease, opacity 0.2s ease;
}

.panel-slide-enter-from,
.panel-slide-leave-to {
  transform: translateY(10px);
  opacity: 0;
}
</style>
