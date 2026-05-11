<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useMediaRecorder } from '@/composables/useMediaRecorder'
import { formatVoiceDuration } from '@/utils/format'

export interface VoiceRecordResult {
  blob: Blob
  duration: number
  waveform: number[]
}

const emit = defineEmits<{
  send: [result: VoiceRecordResult]
  cancel: []
}>()

const MAX_DURATION = 60
const { supported, recording, paused, elapsed, error, liveWaveform, start, stop, cancel, togglePause } =
  useMediaRecorder({ maxDuration: MAX_DURATION, waveformBars: 32 })

const phase = ref<'idle' | 'recording' | 'review'>('idle')
const reviewBlob = ref<Blob | null>(null)
const reviewDuration = ref(0)
const reviewWaveform = ref<number[]>([])
const reviewAudioUrl = ref<string | null>(null)
const reviewAudio = ref<HTMLAudioElement | null>(null)
const reviewPlaying = ref(false)
const reviewProgress = ref(0)

const displayElapsed = computed(() => formatVoiceDuration(elapsed.value))
const displayReviewDuration = computed(() => formatVoiceDuration(reviewDuration.value))
const progressPercent = computed(() => (MAX_DURATION > 0 ? Math.min(100, (elapsed.value / MAX_DURATION) * 100) : 0))
const reviewProgressPercent = computed(() =>
  reviewDuration.value > 0 ? Math.min(100, (reviewProgress.value / reviewDuration.value) * 100) : 0,
)

watch(recording, (active) => {
  if (active) {
    phase.value = 'recording'
  }
})

async function handleStart() {
  if (!supported.value) {
    return
  }
  reviewBlob.value = null
  reviewWaveform.value = []
  reviewDuration.value = 0
  reviewProgress.value = 0
  reviewPlaying.value = false
  releaseReviewAudio()
  await start()
}

async function handleStop() {
  const result = await stop()
  if (!result || result.duration < 1) {
    phase.value = 'idle'
    return
  }
  reviewBlob.value = result.blob
  reviewDuration.value = result.duration
  reviewWaveform.value = result.waveform
  reviewAudioUrl.value = URL.createObjectURL(result.blob)
  phase.value = 'review'
}

function handleCancel() {
  cancel()
  releaseReviewAudio()
  reviewBlob.value = null
  reviewWaveform.value = []
  reviewDuration.value = 0
  reviewProgress.value = 0
  phase.value = 'idle'
  emit('cancel')
}

function handleSend() {
  if (!reviewBlob.value) return
  emit('send', {
    blob: reviewBlob.value,
    duration: reviewDuration.value,
    waveform: reviewWaveform.value,
  } as VoiceRecordResult)
  releaseReviewAudio()
  reviewBlob.value = null
  reviewWaveform.value = []
  reviewDuration.value = 0
  reviewProgress.value = 0
  phase.value = 'idle'
}

function handleRerecord() {
  releaseReviewAudio()
  reviewBlob.value = null
  reviewWaveform.value = []
  reviewDuration.value = 0
  reviewProgress.value = 0
  phase.value = 'idle'
  void handleStart()
}

function toggleReviewPlayback() {
  if (!reviewAudioUrl.value) return

  if (!reviewAudio.value) {
    reviewAudio.value = new Audio(reviewAudioUrl.value)
    reviewAudio.value.addEventListener('timeupdate', () => {
      reviewProgress.value = reviewAudio.value?.currentTime ?? 0
    })
    reviewAudio.value.addEventListener('ended', () => {
      reviewPlaying.value = false
      reviewProgress.value = 0
    })
  }

  if (reviewPlaying.value) {
    reviewAudio.value.pause()
    reviewPlaying.value = false
  } else {
    reviewAudio.value.play()
    reviewPlaying.value = true
  }
}

function releaseReviewAudio() {
  if (reviewAudio.value) {
    reviewAudio.value.pause()
    reviewAudio.value.src = ''
    reviewAudio.value = null
  }
  if (reviewAudioUrl.value) {
    URL.revokeObjectURL(reviewAudioUrl.value)
    reviewAudioUrl.value = null
  }
  reviewPlaying.value = false
  reviewProgress.value = 0
}
</script>

<template>
  <div class="voice-recorder" data-testid="voice-recorder">
    <div v-if="!supported" class="voice-recorder__unsupported">
      当前浏览器不支持语音录制
    </div>

    <template v-else>
      <div v-if="phase === 'idle'" class="voice-recorder__idle">
        <button
          class="voice-recorder__mic"
          type="button"
          aria-label="按住录音"
          data-testid="voice-start"
          @click="handleStart"
        >
          <svg viewBox="0 0 24 24" fill="none">
            <rect x="9" y="2" width="6" height="12" rx="3" stroke="currentColor" stroke-width="1.8"/>
            <path d="M5 11a7 7 0 0 0 14 0" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"/>
            <line x1="12" y1="18" x2="12" y2="22" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"/>
            <line x1="8" y1="22" x2="16" y2="22" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"/>
          </svg>
        </button>
        <span class="voice-recorder__hint">点击录制语音</span>
      </div>

      <div v-if="phase === 'recording'" class="voice-recorder__active">
        <div class="voice-recorder__waveform">
          <span
            v-for="(level, index) in liveWaveform"
            :key="index"
            class="voice-recorder__bar"
            :style="{ '--bar-height': `${Math.max(8, level * 100)}%` }"
          ></span>
        </div>
        <span class="voice-recorder__timer">{{ displayElapsed }}</span>
        <div class="voice-recorder__progress-bar">
          <div class="voice-recorder__progress-fill" :style="{ width: `${progressPercent}%` }"></div>
        </div>
        <div class="voice-recorder__controls">
          <button
            class="voice-recorder__btn voice-recorder__btn--cancel"
            type="button"
            aria-label="取消录音"
            data-testid="voice-cancel"
            @click="handleCancel"
          >
            取消
          </button>
          <button
            class="voice-recorder__btn voice-recorder__btn--pause"
            type="button"
            :aria-label="paused ? '继续录音' : '暂停录音'"
            data-testid="voice-pause"
            @click="togglePause"
          >
            {{ paused ? '继续' : '暂停' }}
          </button>
          <button
            class="voice-recorder__btn voice-recorder__btn--stop"
            type="button"
            aria-label="完成录音"
            data-testid="voice-stop"
            @click="handleStop"
          >
            完成
          </button>
        </div>
      </div>

      <div v-if="phase === 'review'" class="voice-recorder__review">
        <div class="voice-recorder__waveform voice-recorder__waveform--review">
          <span
            v-for="(level, index) in reviewWaveform"
            :key="index"
            class="voice-recorder__bar"
            :class="{ 'is-played': reviewPlaying && index / reviewWaveform.length <= reviewProgressPercent / 100 }"
            :style="{ '--bar-height': `${Math.max(8, level * 100)}%` }"
          ></span>
        </div>
        <span class="voice-recorder__timer">{{ displayReviewDuration }}</span>
        <div class="voice-recorder__progress-bar">
          <div class="voice-recorder__progress-fill" :style="{ width: `${reviewProgressPercent}%` }"></div>
        </div>
        <div class="voice-recorder__controls">
          <button
            class="voice-recorder__btn voice-recorder__btn--cancel"
            type="button"
            aria-label="取消"
            data-testid="voice-review-cancel"
            @click="handleCancel"
          >
            取消
          </button>
          <button
            class="voice-recorder__btn voice-recorder__btn--play"
            type="button"
            :aria-label="reviewPlaying ? '暂停预览' : '播放预览'"
            data-testid="voice-review-play"
            @click="toggleReviewPlayback"
          >
            {{ reviewPlaying ? '暂停' : '试听' }}
          </button>
          <button
            class="voice-recorder__btn voice-recorder__btn--rerecord"
            type="button"
            aria-label="重新录制"
            data-testid="voice-rerecord"
            @click="handleRerecord"
          >
            重录
          </button>
          <button
            class="voice-recorder__btn voice-recorder__btn--send"
            type="button"
            aria-label="发送语音"
            data-testid="voice-send"
            @click="handleSend"
          >
            发送
          </button>
        </div>
      </div>

      <div v-if="error" class="voice-recorder__error" data-testid="voice-error">{{ error }}</div>
    </template>
  </div>
</template>

<style scoped>
.voice-recorder {
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding: 12px 14px;
  border: 1px solid var(--border-default);
  border-radius: var(--radius-panel);
  background: color-mix(in srgb, var(--surface-card) 94%, transparent);
  backdrop-filter: blur(12px);
}

.voice-recorder__unsupported {
  text-align: center;
  color: var(--text-tertiary);
  font-size: var(--text-sm);
}

.voice-recorder__idle {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
}

.voice-recorder__mic {
  width: 52px;
  height: 52px;
  display: grid;
  place-items: center;
  padding: 0;
  border: 2px solid color-mix(in srgb, var(--interactive-primary-bg) 28%, var(--border-default));
  border-radius: 999px;
  background: color-mix(in srgb, var(--interactive-primary-bg) 8%, var(--surface-panel));
  color: var(--interactive-primary-bg);
  cursor: pointer;
  transition:
    background var(--motion-fast) var(--motion-ease-out),
    border-color var(--motion-fast) var(--motion-ease-out),
    box-shadow var(--motion-fast) var(--motion-ease-out),
    transform var(--motion-fast) var(--motion-ease-out);
}

.voice-recorder__mic svg {
  width: 22px;
  height: 22px;
}

.voice-recorder__mic:hover {
  background: color-mix(in srgb, var(--interactive-primary-bg) 14%, var(--surface-panel));
  border-color: color-mix(in srgb, var(--interactive-primary-bg) 48%, var(--border-strong));
  box-shadow: 0 4px 12px color-mix(in srgb, var(--interactive-primary-bg) 18%, transparent);
  transform: translateY(-1px);
}

.voice-recorder__hint {
  color: var(--text-tertiary);
  font-size: var(--text-xs);
}

.voice-recorder__active,
.voice-recorder__review {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.voice-recorder__waveform {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 2px;
  height: 40px;
  padding: 4px 0;
}

.voice-recorder__bar {
  width: 3px;
  min-height: 4px;
  height: var(--bar-height, 8%);
  border-radius: 999px;
  background: color-mix(in srgb, var(--interactive-primary-bg) 50%, var(--surface-subtle));
  transition: height 80ms ease-out, background 120ms ease-out;
}

.voice-recorder__bar.is-played {
  background: var(--interactive-primary-bg);
}

.voice-recorder__waveform--review .voice-recorder__bar {
  background: color-mix(in srgb, var(--interactive-primary-bg) 30%, var(--surface-subtle));
}

.voice-recorder__timer {
  text-align: center;
  font: 600 var(--text-sm)/1 var(--font-mono);
  color: var(--text-secondary);
  letter-spacing: 0.06em;
}

.voice-recorder__progress-bar {
  height: 3px;
  border-radius: 999px;
  background: color-mix(in srgb, var(--border-default) 60%, transparent);
  overflow: hidden;
}

.voice-recorder__progress-fill {
  height: 100%;
  border-radius: 999px;
  background: var(--interactive-primary-bg);
  transition: width 200ms linear;
}

.voice-recorder__controls {
  display: flex;
  justify-content: center;
  gap: 10px;
  flex-wrap: wrap;
}

.voice-recorder__btn {
  padding: 7px 16px;
  border: 1px solid var(--border-default);
  border-radius: var(--radius-control);
  background: transparent;
  color: var(--text-secondary);
  font: 600 var(--text-sm)/1 var(--font-body);
  cursor: pointer;
  transition:
    background var(--motion-fast) var(--motion-ease-out),
    border-color var(--motion-fast) var(--motion-ease-out),
    color var(--motion-fast) var(--motion-ease-out);
}

.voice-recorder__btn:hover {
  background: var(--interactive-secondary-bg-hover);
  border-color: var(--border-strong);
  color: var(--text-primary);
}

.voice-recorder__btn--cancel {
  border-color: color-mix(in srgb, var(--status-danger) 20%, var(--border-default));
  color: var(--status-danger);
}

.voice-recorder__btn--cancel:hover {
  background: color-mix(in srgb, var(--status-danger) 8%, transparent);
  border-color: color-mix(in srgb, var(--status-danger) 40%, var(--border-default));
}

.voice-recorder__btn--send {
  border-color: var(--interactive-primary-bg);
  background: var(--interactive-primary-bg);
  color: var(--interactive-primary-fg);
}

.voice-recorder__btn--send:hover {
  background: var(--interactive-primary-bg-hover);
}

.voice-recorder__error {
  text-align: center;
  color: var(--status-danger);
  font-size: var(--text-xs);
}

@media (max-width: 767px) {
  .voice-recorder__btn {
    padding: 6px 12px;
    font-size: var(--text-xs);
  }
}
</style>
