<script setup lang="ts">
import { computed, onBeforeUnmount, ref, watch } from 'vue'
import { formatVoiceDuration } from '@/utils/format'

const props = defineProps<{
  audioUrl: string | null
  duration: number
  waveform: number[]
  self?: boolean
}>()

const playing = ref(false)
const currentTime = ref(0)
const audio = ref<HTMLAudioElement | null>(null)
const ready = ref(false)

const displayDuration = computed(() => formatVoiceDuration(props.duration))
const progressPercent = computed(() =>
  props.duration > 0 ? Math.min(100, (currentTime.value / props.duration) * 100) : 0,
)

watch(
  () => props.audioUrl,
  (url) => {
    releaseAudio()
    if (!url) return
    audio.value = new Audio(url)
    audio.value.addEventListener('loadeddata', () => {
      ready.value = true
    })
    audio.value.addEventListener('timeupdate', () => {
      currentTime.value = audio.value?.currentTime ?? 0
    })
    audio.value.addEventListener('ended', () => {
      playing.value = false
      currentTime.value = 0
    })
    audio.value.addEventListener('error', () => {
      playing.value = false
      ready.value = false
    })
  },
  { immediate: true },
)

function togglePlayback() {
  if (!audio.value || !props.audioUrl) return

  if (playing.value) {
    audio.value.pause()
    playing.value = false
  } else {
    audio.value.play()
    playing.value = true
  }
}

function releaseAudio() {
  if (audio.value) {
    audio.value.pause()
    audio.value.src = ''
    audio.value = null
  }
  playing.value = false
  currentTime.value = 0
  ready.value = false
}

onBeforeUnmount(() => {
  releaseAudio()
})
</script>

<template>
  <div class="voice-player" :class="{ 'is-self': self }" data-testid="voice-player">
    <button
      class="voice-player__toggle"
      type="button"
      :aria-label="playing ? '暂停语音' : '播放语音'"
      :disabled="!audioUrl"
      data-testid="voice-player-toggle"
      @click="togglePlayback"
    >
      <svg v-if="!playing" viewBox="0 0 24 24" fill="currentColor">
        <path d="M8 5v14l11-7z"/>
      </svg>
      <svg v-else viewBox="0 0 24 24" fill="currentColor">
        <rect x="6" y="5" width="4" height="14" rx="1"/>
        <rect x="14" y="5" width="4" height="14" rx="1"/>
      </svg>
    </button>
    <div class="voice-player__body">
      <div class="voice-player__waveform">
        <span
          v-for="(level, index) in waveform"
          :key="index"
          class="voice-player__bar"
          :class="{ 'is-played': index / waveform.length <= progressPercent / 100 }"
          :style="{ '--bar-height': `${Math.max(10, level * 100)}%` }"
        ></span>
      </div>
      <span class="voice-player__duration">{{ displayDuration }}</span>
    </div>
  </div>
</template>

<style scoped>
.voice-player {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 200px;
  padding: 6px 0;
}

.voice-player__toggle {
  width: 36px;
  height: 36px;
  flex-shrink: 0;
  display: grid;
  place-items: center;
  padding: 0;
  border: 0;
  border-radius: 999px;
  background: color-mix(in srgb, var(--interactive-primary-bg) 12%, var(--surface-subtle));
  color: var(--interactive-primary-bg);
  cursor: pointer;
  transition:
    background var(--motion-fast) var(--motion-ease-out),
    transform var(--motion-fast) var(--motion-ease-out);
}

.voice-player__toggle svg {
  width: 16px;
  height: 16px;
}

.voice-player__toggle:hover:not(:disabled) {
  background: color-mix(in srgb, var(--interactive-primary-bg) 20%, var(--surface-subtle));
  transform: scale(1.06);
}

.voice-player__toggle:disabled {
  opacity: 0.45;
  cursor: default;
}

.voice-player__body {
  display: flex;
  flex-direction: column;
  gap: 5px;
  flex: 1;
  min-width: 0;
}

.voice-player__waveform {
  display: flex;
  align-items: center;
  gap: 1.5px;
  height: 28px;
}

.voice-player__bar {
  width: 2.5px;
  min-height: 3px;
  height: var(--bar-height, 10%);
  border-radius: 999px;
  background: color-mix(in srgb, var(--text-tertiary) 30%, var(--surface-subtle));
  transition: height 80ms ease-out, background 120ms ease-out;
}

.voice-player__bar.is-played {
  background: var(--interactive-primary-bg);
}

.voice-player.is-self .voice-player__bar {
  background: color-mix(in srgb, var(--text-on-brand) 30%, transparent);
}

.voice-player.is-self .voice-player__bar.is-played {
  background: var(--text-on-brand);
}

.voice-player__duration {
  font: 500 var(--text-xs)/1 var(--font-mono);
  color: var(--text-tertiary);
  letter-spacing: 0.04em;
}

.voice-player.is-self .voice-player__duration {
  color: color-mix(in srgb, var(--text-on-brand) 70%, transparent);
}
</style>
