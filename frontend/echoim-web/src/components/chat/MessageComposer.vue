<script setup lang="ts">
import { computed, ref } from 'vue'
import { ChatDotRound, Close, Paperclip } from '@element-plus/icons-vue'
import type { StickerDefinition } from '@/types/chat'
import type { VoiceRecordResult } from './VoiceRecorder.vue'
import VoiceRecorder from './VoiceRecorder.vue'

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
  'send-voice': [result: VoiceRecordResult]
  'cancel-reply': []
  typing: []
}>()

const draft = ref('')
const hasText = computed(() => Boolean(draft.value.trim()))
const fileInput = ref<HTMLInputElement | null>(null)
const stickerTrayOpen = ref(false)
const voiceRecorderOpen = ref(false)
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

function toggleVoiceRecorder() {
  if (!props.canSend) return
  voiceRecorderOpen.value = !voiceRecorderOpen.value
  if (voiceRecorderOpen.value) {
    stickerTrayOpen.value = false
  }
}

function handleVoiceSend(result: VoiceRecordResult) {
  voiceRecorderOpen.value = false
  emit('send-voice', result)
}

function handleVoiceCancel() {
  voiceRecorderOpen.value = false
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
      <div v-if="voiceRecorderOpen" class="composer__voice-panel">
        <VoiceRecorder @send="handleVoiceSend" @cancel="handleVoiceCancel" />
      </div>
      <div v-if="stickerTrayOpen" class="composer__stickers">
        <div class="composer__stickers-header">
          <strong>贴纸</strong>
          <button class="composer__stickers-close" type="button" aria-label="关闭贴纸面板" @click="stickerTrayOpen = false">
            <Close />
          </button>
        </div>
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
        <button
          class="composer__icon"
          :class="{ 'is-active': voiceRecorderOpen }"
          type="button"
          aria-label="语音录制"
          data-testid="voice-recorder-toggle"
          @click="toggleVoiceRecorder"
        >
          <svg viewBox="0 0 24 24" fill="none">
            <rect x="9" y="2" width="6" height="12" rx="3" stroke="currentColor" stroke-width="1.8"/>
            <path d="M5 11a7 7 0 0 0 14 0" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"/>
            <line x1="12" y1="18" x2="12" y2="22" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"/>
            <line x1="8" y1="22" x2="16" y2="22" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"/>
          </svg>
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
          @input="emit('typing')"
          @keydown="onKeydown"
        />
        <button
          class="composer__action"
          type="button"
          :disabled="!canSend || !hasText || attachmentUploading"
          aria-label="发送消息"
          data-testid="send-message"
          @click="submit"
        >
          <svg viewBox="0 0 24 24" fill="none"><path d="M5 12h14M13 5l7 7-7 7" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/></svg>
        </button>
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

.composer__voice-panel {
  grid-column: 1 / -1;
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
    radial-gradient(circle at top left, color-mix(in srgb, var(--interactive-focus-ring) 18%, transparent), transparent 42%),
    var(--surface-card);
  box-shadow: var(--shadow-sm);
}

.composer__stickers-header {
  grid-column: 1 / -1;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  margin-bottom: 2px;
}

.composer__stickers-header strong {
  font: 620 var(--text-sm)/1.08 var(--font-display);
  color: var(--text-primary);
}

.composer__stickers-close {
  width: var(--btn-icon-size);
  height: var(--btn-icon-size);
  display: grid;
  place-items: center;
  border: 1px solid var(--border-default);
  border-radius: var(--radius-control);
  background: transparent;
  color: var(--text-secondary);
  transition:
    background var(--motion-fast) var(--motion-ease-out),
    border-color var(--motion-fast) var(--motion-ease-out),
    color var(--motion-fast) var(--motion-ease-out);
}

.composer__stickers-close:hover,
.composer__stickers-close:focus-visible {
  background: var(--interactive-secondary-bg-hover);
  border-color: var(--border-strong);
  color: var(--text-primary);
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
  font-size: var(--text-sm);
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
  font-size: var(--text-sm);
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
  grid-template-columns: var(--btn-icon-size) var(--btn-icon-size) minmax(0, 1fr) var(--btn-icon-size);
  align-items: end;
  gap: 6px;
  min-height: 60px;
  padding: 8px 9px 8px 10px;
  border: 1px solid var(--border-default);
  border-radius: var(--radius-panel);
  background: color-mix(in srgb, var(--surface-card) 94%, transparent);
  backdrop-filter: blur(12px);
}

.composer__icon {
  width: var(--btn-icon-size);
  height: var(--btn-icon-size);
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

.composer__icon.is-active {
  background: color-mix(in srgb, var(--interactive-primary-bg) 12%, var(--surface-panel));
  border-color: color-mix(in srgb, var(--interactive-primary-bg) 30%, var(--border-default));
  color: var(--interactive-primary-bg);
}

.composer__bar :deep(.el-textarea) {
  grid-column: 3;
  min-width: 0;
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
  font-size: var(--text-base);
  letter-spacing: -0.01em;
  resize: none;
}

.composer__bar :deep(.el-textarea__inner::placeholder) {
  color: var(--text-quaternary);
}

.composer__action {
  width: var(--btn-icon-size);
  min-width: var(--btn-icon-size);
  max-width: var(--btn-icon-size);
  height: var(--btn-icon-size);
  display: grid;
  place-items: center;
  align-self: end;
  padding: 0;
  border: 0;
  border-radius: var(--radius-control);
  background: var(--interactive-primary-bg);
  color: var(--interactive-primary-fg);
  cursor: pointer;
  box-shadow:
    0 2px 8px color-mix(in srgb, var(--interactive-primary-bg) 30%, transparent),
    0 8px 20px color-mix(in srgb, var(--interactive-primary-bg) 14%, transparent);
  transition:
    background var(--motion-fast) var(--motion-ease-out),
    box-shadow var(--motion-fast) var(--motion-ease-out),
    transform var(--motion-fast) var(--motion-ease-out);
}

.composer__action svg {
  width: 18px;
  height: 18px;
}

.composer__action:hover:not(:disabled) {
  background: var(--interactive-primary-bg-hover);
  box-shadow:
    0 4px 12px color-mix(in srgb, var(--interactive-primary-bg) 36%, transparent),
    0 10px 24px color-mix(in srgb, var(--interactive-primary-bg) 18%, transparent);
  transform: translateY(-1px);
}

.composer__action:active:not(:disabled) {
  background: var(--interactive-primary-bg);
  box-shadow:
    0 1px 4px color-mix(in srgb, var(--interactive-primary-bg) 24%, transparent);
  transform: translateY(0);
}

.composer__action:disabled {
  background: color-mix(in srgb, var(--interactive-primary-bg) 48%, var(--surface-panel));
  box-shadow: none;
  opacity: 0.55;
}

@media (max-width: 767px) {
  .composer {
    padding: 10px 12px 14px;
  }

  .composer__action {
    width: var(--btn-icon-size);
    min-width: var(--btn-icon-size);
    height: var(--btn-icon-size);
  }
}
</style>
