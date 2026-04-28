<script setup lang="ts">
import { computed, ref } from 'vue'
import { ChatDotRound, Microphone, Paperclip, Promotion } from '@element-plus/icons-vue'

const props = withDefaults(
  defineProps<{
    enterToSend?: boolean
    canSend?: boolean
    disabledReason?: string
  }>(),
  {
    enterToSend: true,
    canSend: true,
    disabledReason: '当前会话暂不可发送消息',
  },
)

const emit = defineEmits<{
  send: [content: string]
}>()

const draft = ref('')
const hasText = computed(() => Boolean(draft.value.trim()))

function submit() {
  if (!props.canSend) return

  const content = draft.value.trim()
  if (!content) return

  emit('send', content)
  draft.value = ''
}

function onKeydown(event: Event | KeyboardEvent) {
  if (!(event instanceof KeyboardEvent)) return
  if (!props.canSend) return

  if (props.enterToSend && event.key === 'Enter' && !event.shiftKey) {
    event.preventDefault()
    submit()
  }
}
</script>

<template>
  <footer class="composer">
    <div class="composer__inner">
      <div class="composer__bar">
        <button class="composer__icon" type="button" aria-label="表情">
          <ChatDotRound />
        </button>
        <el-input
          v-model="draft"
          :autosize="{ minRows: 1, maxRows: 4 }"
          maxlength="500"
          :placeholder="canSend ? (enterToSend ? '输入消息，Enter 发送' : '输入消息，Shift + Enter 换行') : disabledReason"
          :disabled="!canSend"
          resize="none"
          type="textarea"
          aria-label="消息输入框"
          @keydown="onKeydown"
        />
        <button class="composer__icon" type="button" aria-label="附件">
          <Paperclip />
        </button>
      </div>
      <el-button
        type="primary"
        circle
        class="composer__action"
        :disabled="!canSend || !hasText"
        aria-label="发送消息"
        data-testid="send-message"
        @click="submit"
      >
        <Promotion v-if="hasText" />
        <Microphone v-else />
      </el-button>
    </div>
  </footer>
</template>

<style scoped>
.composer {
  position: relative;
  z-index: 1;
  padding: 12px 24px 18px;
}

.composer__inner {
  width: min(760px, 100%);
  margin: 0 auto;
  display: grid;
  grid-template-columns: minmax(0, 1fr) 54px;
  align-items: end;
  gap: 12px;
}

.composer__bar {
  display: grid;
  grid-template-columns: 42px minmax(0, 1fr) 42px;
  align-items: end;
  gap: 6px;
  min-height: 58px;
  padding: 7px 10px;
  border: 1px solid var(--color-shell-border);
  border-radius: 22px;
  background: var(--color-shell-card-strong);
  box-shadow: var(--shadow-card);
}

.composer__icon {
  width: 42px;
  height: 42px;
  display: grid;
  place-items: center;
  align-self: end;
  border: 0;
  border: 1px solid transparent;
  border-radius: 14px;
  background: var(--color-shell-action);
  color: var(--color-text-soft);
  transition:
    background var(--motion-fast) ease,
    color var(--motion-fast) ease,
    border-color var(--motion-fast) ease;
}

.composer__icon:hover,
.composer__icon:focus-visible {
  background: var(--color-shell-action-hover);
  border-color: var(--color-shell-border);
  color: var(--color-text-1);
}

.composer__bar :deep(.el-textarea) {
  flex: 1;
}

.composer__bar :deep(.el-textarea__inner) {
  min-height: 42px !important;
  max-height: 112px;
  padding: 10px 8px 9px;
  border: 0;
  background: transparent;
  color: var(--color-text-1);
  line-height: 1.48;
  font-size: 0.98rem;
  resize: none;
}

.composer__bar :deep(.el-textarea__inner::placeholder) {
  color: var(--color-text-soft);
}

.composer__action {
  width: 54px;
  min-width: 54px;
  height: 54px;
  padding: 0;
  border: 0;
  background: var(--color-primary);
  box-shadow: 0 14px 24px color-mix(in srgb, var(--color-primary) 26%, transparent);
}

.composer__action:disabled {
  background: rgba(135, 116, 225, 0.82);
  opacity: 1;
}

@media (max-width: 767px) {
  .composer {
    padding: 10px 12px 14px;
  }

  .composer__inner {
    gap: 10px;
    grid-template-columns: minmax(0, 1fr) 52px;
  }

  .composer__action {
    width: 52px;
    min-width: 52px;
    height: 52px;
  }
}
</style>
