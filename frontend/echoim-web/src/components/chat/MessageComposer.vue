<script setup lang="ts">
import { computed, nextTick, ref, watch } from 'vue'
import { ChatDotRound, Close, Clock, Paperclip, Timer } from '@element-plus/icons-vue'
import type { GroupMemberItem, MentionItem, StickerDefinition } from '@/types/chat'
import type { VoiceRecordResult } from './VoiceRecorder.vue'
import VoiceRecorder from './VoiceRecorder.vue'
import MentionSelector from './MentionSelector.vue'

const SELF_DESTRUCT_OPTIONS = [
  { label: '关闭', value: 0 },
  { label: '5秒', value: 5 },
  { label: '30秒', value: 30 },
  { label: '1分钟', value: 60 },
  { label: '5分钟', value: 300 },
  { label: '1小时', value: 3600 },
]

const props = withDefaults(
  defineProps<{
    conversationId?: number | null
    enterToSend?: boolean
    canSend?: boolean
    disabledReason?: string
    replyingMessage?: { content: string | null; file?: { fileName: string } | null } | null
    attachmentUploading?: boolean
    attachmentError?: string | null
    stickers?: StickerDefinition[]
    groupMembers?: GroupMemberItem[]
    currentUserId?: number
  }>(),
  {
    conversationId: null,
    enterToSend: true,
    canSend: true,
    disabledReason: '当前会话暂不可发送消息',
    replyingMessage: null,
    attachmentUploading: false,
    attachmentError: null,
    stickers: () => [],
    groupMembers: () => [],
    currentUserId: 0,
  },
)

const emit = defineEmits<{
  send: [content: string, mentions: MentionItem[], selfDestructSeconds?: number]
  'scheduled-send': [content: string, mentions: MentionItem[], scheduledAt: string]
  'open-scheduled-panel': []
  'upload-file': [file: File]
  'upload-files': [files: File[]]
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
const selfDestructSeconds = ref(0)
const selfDestructMenuOpen = ref(false)
const scheduledSendOpen = ref(false)
const scheduledDate = ref('')
const scheduledTime = ref('')
const replyPreview = computed(() => props.replyingMessage?.content || props.replyingMessage?.file?.fileName || '原消息')

// Draft auto-save to localStorage and backend
import * as draftService from '@/services/drafts'

const DRAFT_STORAGE_KEY = 'echoim_drafts'

function getDraftKey(conversationId: number | null | undefined): string {
  return `draft_${conversationId || 'none'}`
}

function loadDraftFromStorage() {
  if (!props.conversationId) return
  try {
    const drafts = JSON.parse(localStorage.getItem(DRAFT_STORAGE_KEY) || '{}')
    draft.value = drafts[getDraftKey(props.conversationId)] || ''
  } catch {
    draft.value = ''
  }
}

function saveDraftToStorage() {
  if (!props.conversationId) return
  try {
    const drafts = JSON.parse(localStorage.getItem(DRAFT_STORAGE_KEY) || '{}')
    const key = getDraftKey(props.conversationId)
    if (draft.value.trim()) {
      drafts[key] = draft.value
    } else {
      delete drafts[key]
    }
    localStorage.setItem(DRAFT_STORAGE_KEY, JSON.stringify(drafts))
  } catch {
    // Ignore storage errors
  }
}

// Sync draft to backend (debounced)
let syncDraftTimeout: ReturnType<typeof setTimeout> | null = null
function syncDraftToBackend() {
  if (!props.conversationId) return
  if (syncDraftTimeout) clearTimeout(syncDraftTimeout)
  syncDraftTimeout = setTimeout(async () => {
    try {
      await draftService.saveDraft(props.conversationId!, draft.value)
    } catch {
      // Ignore sync errors - localStorage is the primary store
    }
  }, 1000)
}

// Load draft when conversation changes
watch(() => props.conversationId, async () => {
  loadDraftFromStorage()
  // Also try to load from backend (in case localStorage is stale)
  if (props.conversationId) {
    try {
      const result = await draftService.loadDraft(props.conversationId)
      if (result.draftContent && result.draftContent !== draft.value) {
        draft.value = result.draftContent
        saveDraftToStorage() // Update localStorage with backend value
      }
    } catch {
      // Ignore load errors - localStorage value is already loaded
    }
  }
}, { immediate: true })

// Save draft on input changes (debounced)
let saveDraftTimeout: ReturnType<typeof setTimeout> | null = null
watch(draft, () => {
  if (saveDraftTimeout) clearTimeout(saveDraftTimeout)
  saveDraftTimeout = setTimeout(() => {
    saveDraftToStorage()
    syncDraftToBackend()
  }, 500)
})

const minScheduledDateTime = computed(() => {
  const now = new Date()
  now.setMinutes(now.getMinutes() + 5) // Minimum 5 minutes from now
  return now.toISOString().slice(0, 16)
})

const canSchedule = computed(() => {
  return scheduledDate.value && scheduledTime.value
})

// @mention state
const mentionSelectorVisible = ref(false)
const mentionQuery = ref('')
const mentionStartIndex = ref(-1)
const pendingMentions = ref<MentionItem[]>([])
const mentionSelectorRef = ref<InstanceType<typeof MentionSelector> | null>(null)

const isGroupChat = computed(() => props.groupMembers.length > 0)

function submit() {
  if (!props.canSend) return

  const content = draft.value.trim()
  if (!content) return

  // Collect all mentions that are actually in the text
  const mentionsInText = pendingMentions.value.filter((m) =>
    content.includes(`@${m.displayName}`) || content.includes(`@${m.userId}`)
  )

  emit('send', content, mentionsInText, selfDestructSeconds.value > 0 ? selfDestructSeconds.value : undefined)
  draft.value = ''
  saveDraftToStorage() // Clear draft from storage
  pendingMentions.value = []
  closeMentionSelector()
}

function submitScheduled() {
  if (!props.canSend) return

  const content = draft.value.trim()
  if (!content) return
  if (!canSchedule.value) return

  // Collect all mentions that are actually in the text
  const mentionsInText = pendingMentions.value.filter((m) =>
    content.includes(`@${m.displayName}`) || content.includes(`@${m.userId}`)
  )

  const scheduledAt = `${scheduledDate.value}T${scheduledTime.value}:00`
  emit('scheduled-send', content, mentionsInText, scheduledAt)
  draft.value = ''
  saveDraftToStorage() // Clear draft from storage
  pendingMentions.value = []
  closeMentionSelector()
  scheduledSendOpen.value = false
  scheduledDate.value = ''
  scheduledTime.value = ''
}

function onKeydown(event: Event | KeyboardEvent) {
  if (!(event instanceof KeyboardEvent)) return
  if (!props.canSend) return

  // Let mention selector handle keys first
  if (mentionSelectorVisible.value) {
    if (['ArrowDown', 'ArrowUp', 'Enter', 'Tab', 'Escape'].includes(event.key)) {
      mentionSelectorRef.value?.handleKeydown(event)
      if (event.defaultPrevented) return
    }
  }

  if (props.enterToSend && event.key === 'Enter' && !event.shiftKey) {
    event.preventDefault()
    submit()
  }
}

function onInput() {
  emit('typing')
  if (!isGroupChat.value) return

  const textarea = getInputElement()
  if (!textarea) return

  const cursorPos = textarea.selectionStart ?? draft.value.length
  const textBeforeCursor = draft.value.slice(0, cursorPos)

  // Find the last @ before cursor
  const lastAtIndex = textBeforeCursor.lastIndexOf('@')

  if (lastAtIndex >= 0) {
    // Check there's no space between @ and cursor (simple mention detection)
    const textAfterAt = textBeforeCursor.slice(lastAtIndex + 1)
    if (!textAfterAt.includes(' ') && !textAfterAt.includes('\n')) {
      mentionStartIndex.value = lastAtIndex
      mentionQuery.value = textAfterAt
      mentionSelectorVisible.value = true
      return
    }
  }

  closeMentionSelector()
}

function closeMentionSelector() {
  mentionSelectorVisible.value = false
  mentionQuery.value = ''
  mentionStartIndex.value = -1
}

function handleMentionSelect(mention: MentionItem) {
  const before = draft.value.slice(0, mentionStartIndex.value)
  const cursorPos = getInputElement()?.selectionStart ?? draft.value.length
  const after = draft.value.slice(cursorPos)

  draft.value = `${before}@${mention.displayName} ${after}`

  // Track the mention
  const existing = pendingMentions.value.findIndex((m) => m.userId === mention.userId)
  if (existing >= 0) {
    pendingMentions.value[existing] = mention
  } else {
    pendingMentions.value.push(mention)
  }

  closeMentionSelector()

  nextTick(() => {
    const textarea = getInputElement()
    if (textarea) {
      const newPos = before.length + mention.displayName.length + 2
      textarea.setSelectionRange(newPos, newPos)
      textarea.focus()
    }
  })
}

function getInputElement(): HTMLTextAreaElement | null {
  const textarea = document.querySelector('.composer__bar .el-textarea__inner')
  return textarea instanceof HTMLTextAreaElement ? textarea : null
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
  const fileList = target?.files
  if (!fileList || fileList.length === 0) return
  const files = Array.from(fileList)
  if (files.length === 1) {
    emit('upload-file', files[0])
  } else {
    emit('upload-files', files)
  }
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
      <div class="composer__bar" style="position: relative;">
        <MentionSelector
          ref="mentionSelectorRef"
          :visible="mentionSelectorVisible"
          :members="groupMembers"
          :query="mentionQuery"
          :current-user-id="currentUserId"
          @select="handleMentionSelect"
          @close="closeMentionSelector"
        />
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
        <div class="composer__self-destruct-wrapper">
          <button
            class="composer__icon"
            :class="{ 'is-active': selfDestructSeconds > 0 }"
            type="button"
            aria-label="阅后即焚"
            data-testid="self-destruct-toggle"
            @click="selfDestructMenuOpen = !selfDestructMenuOpen"
          >
            <Timer />
          </button>
          <transition name="self-destruct-menu-fade">
            <div v-if="selfDestructMenuOpen" class="composer__self-destruct-menu">
              <div class="composer__self-destruct-header">
                <strong>阅后即焚</strong>
                <p>消息将在对方查看后自动销毁</p>
              </div>
              <button
                v-for="option in SELF_DESTRUCT_OPTIONS"
                :key="option.value"
                class="composer__self-destruct-option"
                :class="{ 'is-active': selfDestructSeconds === option.value }"
                type="button"
                @click="selfDestructSeconds = option.value; selfDestructMenuOpen = false"
              >
                {{ option.label }}
              </button>
            </div>
          </transition>
        </div>
        <div class="composer__scheduled-wrapper">
          <button
            class="composer__icon"
            :class="{ 'is-active': scheduledSendOpen }"
            type="button"
            aria-label="定时发送"
            data-testid="scheduled-send-toggle"
            @click="scheduledSendOpen = !scheduledSendOpen"
          >
            <Clock />
          </button>
          <transition name="scheduled-menu-fade">
            <div v-if="scheduledSendOpen" class="composer__scheduled-menu">
              <div class="composer__scheduled-header">
                <strong>定时发送</strong>
                <p>消息将在指定时间自动发送</p>
              </div>
              <div class="composer__scheduled-fields">
                <el-date-picker
                  v-model="scheduledDate"
                  type="date"
                  placeholder="选择日期"
                  format="YYYY-MM-DD"
                  value-format="YYYY-MM-DD"
                  :disabled-date="(date: Date) => date < new Date(new Date().setHours(0, 0, 0, 0))"
                  size="small"
                />
                <el-time-picker
                  v-model="scheduledTime"
                  placeholder="选择时间"
                  format="HH:mm"
                  value-format="HH:mm"
                  size="small"
                />
              </div>
              <div class="composer__scheduled-actions">
                <button
                  class="composer__scheduled-confirm"
                  type="button"
                  :disabled="!canSchedule || !hasText"
                  @click="submitScheduled"
                >
                  确认定时
                </button>
                <button
                  class="composer__scheduled-cancel"
                  type="button"
                  @click="scheduledSendOpen = false; scheduledDate = ''; scheduledTime = ''"
                >
                  取消
                </button>
              </div>
            </div>
          </transition>
        </div>
        <button
          class="composer__icon"
          type="button"
          aria-label="查看定时消息"
          data-testid="scheduled-panel-toggle"
          @click="emit('open-scheduled-panel')"
        >
          <svg viewBox="0 0 24 24" fill="none">
            <rect x="3" y="4" width="18" height="18" rx="2" stroke="currentColor" stroke-width="1.8"/>
            <line x1="16" y1="2" x2="16" y2="6" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"/>
            <line x1="8" y1="2" x2="8" y2="6" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"/>
            <line x1="3" y1="10" x2="21" y2="10" stroke="currentColor" stroke-width="1.8"/>
            <circle cx="12" cy="16" r="1.5" fill="currentColor"/>
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
          @input="onInput"
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
        <input ref="fileInput" class="composer__file-input" type="file" multiple @change="onSelectFile" />
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

.composer__self-destruct-wrapper {
  position: relative;
}

.composer__self-destruct-menu {
  position: absolute;
  bottom: calc(100% + 8px);
  left: 50%;
  transform: translateX(-50%);
  min-width: 180px;
  padding: 10px;
  border: 1px solid var(--border-default);
  border-radius: var(--radius-panel);
  background: var(--surface-overlay);
  box-shadow: var(--shadow-md);
  z-index: 100;
}

.composer__self-destruct-header {
  margin-bottom: 8px;
  padding-bottom: 8px;
  border-bottom: 1px solid var(--border-subtle);
}

.composer__self-destruct-header strong {
  display: block;
  font-size: var(--text-sm);
  font-weight: 600;
  color: var(--text-primary);
}

.composer__self-destruct-header p {
  margin: 4px 0 0;
  font-size: var(--text-xs);
  color: var(--text-secondary);
}

.composer__self-destruct-option {
  display: block;
  width: 100%;
  padding: 8px 10px;
  border: 0;
  border-radius: var(--radius-md);
  background: transparent;
  color: var(--text-primary);
  font-size: var(--text-sm);
  text-align: left;
  cursor: pointer;
  transition: background var(--motion-fast) var(--motion-ease-out);
}

.composer__self-destruct-option:hover {
  background: var(--interactive-secondary-bg-hover);
}

.composer__self-destruct-option.is-active {
  background: color-mix(in srgb, var(--interactive-primary-bg) 12%, transparent);
  color: var(--interactive-selected-fg);
  font-weight: 600;
}

.self-destruct-menu-fade-enter-active,
.self-destruct-menu-fade-leave-active {
  transition: opacity 0.16s ease, transform 0.16s ease;
}

.self-destruct-menu-fade-enter-from,
.self-destruct-menu-fade-leave-to {
  opacity: 0;
  transform: translateX(-50%) translateY(4px) scale(0.98);
}

.composer__scheduled-wrapper {
  position: relative;
}

.composer__scheduled-menu {
  position: absolute;
  bottom: calc(100% + 8px);
  left: 50%;
  transform: translateX(-50%);
  min-width: 260px;
  padding: 12px;
  border: 1px solid var(--border-default);
  border-radius: var(--radius-panel);
  background: var(--surface-overlay);
  box-shadow: var(--shadow-md);
  z-index: 100;
}

.composer__scheduled-header {
  margin-bottom: 10px;
  padding-bottom: 10px;
  border-bottom: 1px solid var(--border-subtle);
}

.composer__scheduled-header strong {
  display: block;
  font-size: var(--text-sm);
  font-weight: 600;
  color: var(--text-primary);
}

.composer__scheduled-header p {
  margin: 4px 0 0;
  font-size: var(--text-xs);
  color: var(--text-secondary);
}

.composer__scheduled-fields {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px;
  margin-bottom: 10px;
}

.composer__scheduled-fields :deep(.el-date-editor) {
  width: 100%;
}

.composer__scheduled-actions {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px;
}

.composer__scheduled-confirm,
.composer__scheduled-cancel {
  padding: 8px 12px;
  border: 1px solid var(--border-default);
  border-radius: var(--radius-control);
  font-size: var(--text-sm);
  font-weight: 600;
  cursor: pointer;
  transition: background var(--motion-fast) var(--motion-ease-out);
}

.composer__scheduled-confirm {
  background: var(--interactive-primary-bg);
  color: white;
  border-color: var(--interactive-primary-bg);
}

.composer__scheduled-confirm:hover:not(:disabled) {
  background: var(--interactive-primary-bg-hover);
}

.composer__scheduled-confirm:disabled {
  opacity: 0.55;
  cursor: not-allowed;
}

.composer__scheduled-cancel {
  background: transparent;
  color: var(--text-secondary);
}

.composer__scheduled-cancel:hover {
  background: var(--interactive-secondary-bg-hover);
}

.scheduled-menu-fade-enter-active,
.scheduled-menu-fade-leave-active {
  transition: opacity 0.16s ease, transform 0.16s ease;
}

.scheduled-menu-fade-enter-from,
.scheduled-menu-fade-leave-to {
  opacity: 0;
  transform: translateX(-50%) translateY(4px) scale(0.98);
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
