<script setup lang="ts">
import { computed, ref } from 'vue'
import { ChatDotRound, Microphone, Paperclip, Promotion } from '@element-plus/icons-vue'
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
        <input ref="fileInput" class="composer__file-input" type="file" @change="onSelectFile" />
      </div>
      <el-button
        type="primary"
        circle
        class="composer__action"
        :loading="attachmentUploading"
        :disabled="!canSend || (!hasText && !attachmentUploading)"
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
  padding: 14px 24px 22px;
}

.composer__inner {
  width: min(760px, 100%);
  margin: 0 auto;
  display: grid;
  grid-template-columns: minmax(0, 1fr) 58px;
  align-items: end;
  gap: 14px;
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
  padding: 12px;
  border: 1px solid var(--color-shell-border);
  border-radius: 26px;
  background:
    radial-gradient(circle at top left, color-mix(in srgb, var(--color-shell-glow) 18%, transparent), transparent 44%),
    var(--color-shell-card-strong);
  box-shadow: var(--shadow-card);
}

.composer__sticker {
  display: grid;
  gap: 8px;
  padding: 10px;
  border: 1px solid color-mix(in srgb, var(--sticker-accent, var(--color-primary)) 28%, var(--color-shell-border));
  border-radius: 18px;
  background: color-mix(in srgb, var(--sticker-accent, var(--color-primary)) 8%, var(--color-shell-card));
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
  border-radius: 18px;
  background: color-mix(in srgb, var(--color-shell-card-strong) 92%, transparent);
  border: 1px solid var(--color-shell-border);
  color: var(--color-text-2);
  font-size: 0.82rem;
}

.composer__reply button {
  padding: 0;
  border: 0;
  background: transparent;
  color: var(--color-primary);
  font: inherit;
}

.composer__error {
  color: var(--color-danger);
}

.composer__bar {
  display: grid;
  grid-template-columns: 42px 42px minmax(0, 1fr);
  align-items: end;
  gap: 8px;
  min-height: 62px;
  padding: 8px 12px;
  border: 1px solid var(--color-shell-border);
  border-radius: 26px;
  background: color-mix(in srgb, var(--color-shell-card-strong) 94%, transparent);
  box-shadow: var(--shadow-card);
  backdrop-filter: blur(18px);
}

.composer__icon {
  width: 42px;
  height: 42px;
  display: grid;
  place-items: center;
  align-self: end;
  border: 0;
  border: 1px solid transparent;
  border-radius: 16px;
  background: var(--color-shell-action);
  color: var(--color-text-soft);
  transition:
    background var(--motion-fast) ease,
    color var(--motion-fast) ease,
    border-color var(--motion-fast) ease;
}

.composer__icon:disabled {
  opacity: 0.6;
}

.composer__icon:hover,
.composer__icon:focus-visible {
  background: var(--color-shell-action-hover);
  border-color: var(--color-shell-border);
  color: var(--color-text-1);
}

.composer__bar :deep(.el-textarea) {
  grid-column: 3;
  flex: 1;
}

.composer__file-input {
  display: none;
}

.composer__bar :deep(.el-textarea__inner) {
  min-height: 44px !important;
  max-height: 112px;
  padding: 11px 8px 10px;
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
  width: 58px;
  min-width: 58px;
  height: 58px;
  padding: 0;
  border: 0;
  background: var(--color-primary);
  box-shadow: 0 18px 30px color-mix(in srgb, var(--color-primary) 24%, transparent);
}

.composer__action:disabled {
  background: color-mix(in srgb, var(--color-primary) 76%, rgba(255, 255, 255, 0.18));
  opacity: 1;
}

@media (max-width: 767px) {
  .composer {
    padding: 10px 12px 14px;
  }

  .composer__inner {
    gap: 10px;
    grid-template-columns: minmax(0, 1fr) 54px;
  }

  .composer__action {
    width: 54px;
    min-width: 54px;
    height: 54px;
  }
}
</style>
