<script setup lang="ts">
import { computed, ref } from 'vue'
import { ChatDotRound, Paperclip, Promotion } from '@element-plus/icons-vue'
import type { StickerDefinition } from '@/types/chat'

const props = withDefaults(
  defineProps<{
    enterToSend?: boolean
    canSend?: boolean
    disabledReason?: string
    replyingMessage?: { content: string | null; file?: { fileName: string } | null } | null
    attachmentUploading?: boolean
    attachmentError?: string | null
    stickers?: StickerDefinition[]
  }>(),
  {
    enterToSend: true,
    canSend: true,
    disabledReason: '当前会话暂不可发送消息',
    replyingMessage: null,
    attachmentUploading: false,
    attachmentError: null,
    stickers: () => [],
  },
)

const emit = defineEmits<{
  send: [content: string]
  'upload-file': [file: File]
  'send-sticker': [sticker: StickerDefinition]
  'cancel-reply': []
}>()

const draft = ref('')
const hasText = computed(() => Boolean(draft.value.trim()))
const fileInput = ref<HTMLInputElement | null>(null)
const stickerTrayOpen = ref(false)
const replyPreview = computed(() => props.replyingMessage?.content || props.replyingMessage?.file?.fileName || '原消息')

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

function openFilePicker() {
  if (!props.canSend || props.attachmentUploading) return
  fileInput.value?.click()
}

function toggleStickerTray() {
  if (!props.canSend) return
  stickerTrayOpen.value = !stickerTrayOpen.value
}

function onSelectFile(event: Event) {
  const target = event.target as HTMLInputElement | null
  const file = target?.files?.[0]
  if (!file) return
  emit('upload-file', file)
  target.value = ''
}

function sendSticker(sticker: StickerDefinition) {
  stickerTrayOpen.value = false
  emit('send-sticker', sticker)
}
</script>

<template>
  <footer class="composer">
    <div class="composer__inner">
      <div v-if="replyingMessage || attachmentError" class="composer__status">
        <div v-if="replyingMessage" class="composer__reply">
          <span>回复中：{{ replyPreview }}</span>
          <button type="button" @click="emit('cancel-reply')">取消</button>
        </div>
        <div v-if="attachmentError" class="composer__error">{{ attachmentError }}</div>
      </div>
      <div v-if="stickerTrayOpen" class="composer__stickers">
        <button
          v-for="sticker in stickers"
          :key="sticker.stickerId"
          class="composer__sticker"
          type="button"
          :style="{ '--sticker-accent': sticker.accent }"
          @click="sendSticker(sticker)"
        >
          <div class="composer__sticker-art" v-html="sticker.svg"></div>
          <strong>{{ sticker.title }}</strong>
        </button>
      </div>
      <div class="composer__bar">
        <button class="composer__icon" type="button" aria-label="贴纸面板" @click="toggleStickerTray">
          <ChatDotRound />
        </button>
        <button class="composer__icon" type="button" aria-label="附件" :disabled="attachmentUploading" @click="openFilePicker">
          <Paperclip />
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
        <el-button
          type="primary"
          circle
          class="composer__action"
          :loading="attachmentUploading"
          :disabled="!canSend || !hasText"
          aria-label="发送消息"
          data-testid="send-message"
          @click="submit"
        >
          <Promotion />
        </el-button>
        <input ref="fileInput" class="composer__file-input" type="file" @change="onSelectFile" />
      </div>
    </div>
  </footer>
</template>

<style scoped>
.composer {
  position: relative;
  z-index: 1;
  padding: 14px 24px 20px;
}

.composer__inner {
  width: min(760px, 100%);
  margin: 0 auto;
  display: grid;
  gap: 10px;
}

.composer__status {
  grid-column: 1 / -1;
  display: grid;
  gap: 8px;
}

.composer__stickers {
  grid-column: 1 / -1;
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
  padding: 11px;
  border: 1px solid var(--border-default);
  border-radius: var(--radius-card);
  background:
    radial-gradient(circle at top left, color-mix(in srgb, var(--color-shell-glow) 14%, transparent), transparent 42%),
    var(--surface-card);
  box-shadow: var(--shadow-sm);
}

.composer__sticker {
  display: grid;
  gap: 8px;
  padding: 10px;
  border: 1px solid color-mix(in srgb, var(--sticker-accent, var(--interactive-primary-bg)) 22%, var(--border-subtle));
  border-radius: var(--radius-md);
  background: color-mix(in srgb, var(--sticker-accent, var(--interactive-primary-bg)) 6%, var(--surface-panel));
  text-align: left;
}

.composer__sticker-art {
  display: grid;
  place-items: center;
  min-height: 92px;
}

.composer__sticker-art :deep(svg) {
  width: 74px;
  height: 74px;
}

.composer__sticker strong {
  font-size: 0.78rem;
  line-height: 1.2;
}

.composer__reply,
.composer__error {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 10px 14px;
  border-radius: var(--radius-md);
  background: color-mix(in srgb, var(--surface-card) 94%, transparent);
  border: 1px solid var(--border-default);
  color: var(--text-secondary);
  font-size: 0.8rem;
}

.composer__reply button {
  padding: 0;
  border: 0;
  background: transparent;
  color: var(--interactive-primary-bg);
  font: inherit;
}

.composer__error {
  border-color: color-mix(in srgb, var(--status-danger) 18%, var(--border-default));
  background: color-mix(in srgb, var(--status-danger) 5%, var(--surface-card));
  color: var(--status-danger);
}

.composer__bar {
  display: grid;
  grid-template-columns: 42px 42px minmax(0, 1fr) 46px;
  align-items: end;
  gap: 6px;
  min-height: 60px;
  padding: 8px 9px 8px 10px;
  border: 1px solid var(--border-default);
  border-radius: var(--radius-panel);
  background: color-mix(in srgb, var(--surface-card) 94%, transparent);
  box-shadow: var(--shadow-sm);
  backdrop-filter: blur(12px);
}

.composer__icon {
  width: 42px;
  height: 42px;
  display: grid;
  place-items: center;
  align-self: end;
  border: 0;
  border: 1px solid transparent;
  border-radius: var(--radius-control);
  background: color-mix(in srgb, var(--interactive-secondary-bg) 96%, transparent);
  color: var(--text-tertiary);
  transition:
    background var(--motion-fast) var(--motion-ease-out),
    color var(--motion-fast) var(--motion-ease-out),
    border-color var(--motion-fast) var(--motion-ease-out),
    box-shadow var(--motion-fast) var(--motion-ease-out),
    transform var(--motion-fast) var(--motion-ease-out);
}

.composer__icon:disabled {
  opacity: 0.6;
}

.composer__icon:hover,
.composer__icon:focus-visible {
  background: var(--interactive-secondary-bg-hover);
  border-color: var(--border-strong);
  color: var(--text-primary);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.24);
  transform: translateY(-1px);
}

.composer__bar :deep(.el-textarea) {
  grid-column: 3;
  flex: 1;
}

.composer__file-input {
  display: none;
}

.composer__bar :deep(.el-textarea__inner) {
  min-height: 42px !important;
  max-height: 112px;
  padding: 10px 8px 9px;
  border: 0;
  background: transparent;
  color: var(--text-primary);
  line-height: 1.5;
  font-size: 0.94rem;
  letter-spacing: -0.01em;
  resize: none;
}

.composer__bar :deep(.el-textarea__inner::placeholder) {
  color: var(--text-quaternary);
}

.composer__action {
  width: 40px;
  min-width: 40px;
  height: 44px;
  padding: 0;
  border: 1px solid color-mix(in srgb, var(--interactive-primary-bg) 20%, transparent);
  border-radius: var(--radius-control);
  background: var(--interactive-primary-bg);
  color: var(--interactive-primary-fg);
  box-shadow: 0 12px 24px color-mix(in srgb, var(--interactive-primary-bg) 16%, transparent);
}

.composer__action:disabled {
  background: color-mix(in srgb, var(--interactive-primary-bg) 72%, rgba(255, 255, 255, 0.18));
  opacity: 0.72;
}

@media (max-width: 767px) {
  .composer {
    padding: 10px 12px 14px;
  }

  .composer__action {
    width: 40px;
    min-width: 40px;
    height: 42px;
  }
}
</style>
