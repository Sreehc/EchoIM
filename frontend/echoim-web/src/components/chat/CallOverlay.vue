<script setup lang="ts">
import { computed, onBeforeUnmount, ref, watch } from 'vue'
import { Microphone, Phone, TopRight, VideoCamera, VideoPause } from '@element-plus/icons-vue'
import type { CallPhase, CallSessionSummary } from '@/types/chat'
import AvatarBadge from './AvatarBadge.vue'

const props = defineProps<{
  visible: boolean
  call: CallSessionSummary | null
  phase: CallPhase
  minimized: boolean
  localMuted: boolean
  localCameraOff: boolean
  busy?: boolean
  error?: string | null
  localStream: MediaStream | null
  remoteStream: MediaStream | null
}>()

const emit = defineEmits<{
  accept: []
  reject: []
  cancel: []
  end: []
  'toggle-minimized': []
  'toggle-mute': []
  'toggle-camera': []
}>()

const remoteAudio = ref<HTMLAudioElement | null>(null)
const remoteVideo = ref<HTMLVideoElement | null>(null)
const localVideo = ref<HTMLVideoElement | null>(null)

const isVideoCall = computed(() => props.call?.callType === 'video')

const statusLabel = computed(() => {
  const video = isVideoCall.value
  if (props.phase === 'incoming') return video ? '视频来电' : '语音来电'
  if (props.phase === 'outgoing') return '等待对方接听'
  if (props.phase === 'connecting') return video ? '正在建立视频连接' : '正在建立语音连接'
  if (props.phase === 'connected') return formatDuration(props.call?.durationSeconds ?? 0)
  if (props.call?.status === 'missed') return video ? '未接视频通话' : '未接来电'
  if (props.call?.status === 'rejected') return '对方已拒绝'
  if (props.call?.status === 'cancelled') return '通话已取消'
  if (props.call?.status === 'ended') return '通话已结束'
  return video ? '视频通话' : '语音通话'
})

const eyebrowLabel = computed(() => isVideoCall.value ? 'EchoIM Video' : 'EchoIM Voice')

const canAccept = computed(() => props.phase === 'incoming')
const canCancel = computed(() => props.phase === 'outgoing')
const canEnd = computed(() => props.phase === 'connecting' || props.phase === 'connected')

// Bind remote stream to video/audio elements
watch(
  () => [props.remoteStream, remoteVideo.value, remoteAudio.value] as const,
  ([stream, video, audio]) => {
    if (video) {
      video.srcObject = stream
    }
    if (audio) {
      audio.srcObject = stream
    }
  },
  { immediate: true },
)

// Bind local stream to local video element
watch(
  () => [props.localStream, localVideo.value] as const,
  ([stream, video]) => {
    if (video) {
      video.srcObject = stream
    }
  },
  { immediate: true },
)

onBeforeUnmount(() => {
  if (remoteAudio.value) {
    remoteAudio.value.srcObject = null
  }
  if (remoteVideo.value) {
    remoteVideo.value.srcObject = null
  }
  if (localVideo.value) {
    localVideo.value.srcObject = null
  }
})

function formatDuration(durationSeconds: number) {
  const minutes = Math.floor(durationSeconds / 60)
  const seconds = durationSeconds % 60
  return `${String(minutes).padStart(2, '0')}:${String(seconds).padStart(2, '0')}`
}
</script>

<template>
  <transition name="call-overlay">
    <aside v-if="visible && call" class="call-overlay" :class="{ 'is-minimized': minimized, 'is-video': isVideoCall }">
      <!-- Audio-only element for audio calls (hidden for video calls, video element handles audio too) -->
      <audio v-if="!isVideoCall" ref="remoteAudio" autoplay playsinline />

      <!-- Remote video (fullscreen background when video call and not minimized) -->
      <video
        v-if="isVideoCall && !minimized"
        ref="remoteVideo"
        class="call-overlay__remote-video"
        autoplay
        playsinline
      />

      <!-- Local video PiP (bottom-right corner when video call and not minimized) -->
      <div v-if="isVideoCall && !minimized" class="call-overlay__local-pip">
        <video
          ref="localVideo"
          class="call-overlay__local-video"
          autoplay
          playsinline
          muted
        />
        <div v-if="props.localCameraOff" class="call-overlay__local-video-off">
          <AvatarBadge
            :name="call.peerDisplayName"
            :avatar-url="call.peerAvatarUrl"
            :seed="call.peerUserId"
            type="user"
            size="sm"
          />
        </div>
      </div>

      <div class="call-overlay__shell">
        <div class="call-overlay__header">
          <span class="call-overlay__eyebrow">{{ eyebrowLabel }}</span>
          <button class="call-overlay__ghost" type="button" @click="emit('toggle-minimized')">
            <TopRight />
            {{ minimized ? '展开' : '最小化' }}
          </button>
        </div>

        <!-- Full-size body (non-minimized) -->
        <div v-if="!minimized" class="call-overlay__body">
          <!-- For audio calls: show avatar + status -->
          <template v-if="!isVideoCall">
            <AvatarBadge
              :name="call.peerDisplayName"
              :avatar-url="call.peerAvatarUrl"
              :seed="call.peerUserId"
              type="user"
              size="lg"
            />
            <div class="call-overlay__copy">
              <strong>{{ call.peerDisplayName }}</strong>
              <p>{{ statusLabel }}</p>
            </div>
          </template>

          <!-- For video calls: show status overlay at top -->
          <template v-else>
            <div class="call-overlay__video-status">
              <strong>{{ call.peerDisplayName }}</strong>
              <p>{{ statusLabel }}</p>
            </div>
          </template>

          <p v-if="error" class="call-overlay__error">{{ error }}</p>
        </div>

        <!-- Minimized body -->
        <div v-else class="call-overlay__mini">
          <AvatarBadge
            :name="call.peerDisplayName"
            :avatar-url="call.peerAvatarUrl"
            :seed="call.peerUserId"
            type="user"
            size="sm"
          />
          <div>
            <strong>{{ call.peerDisplayName }}</strong>
            <p>{{ statusLabel }}</p>
          </div>
        </div>

        <div class="call-overlay__actions">
          <button
            class="call-overlay__action"
            :class="{ 'is-active': localMuted }"
            type="button"
            :disabled="!canEnd"
            @click="emit('toggle-mute')"
          >
            <Microphone />
            {{ localMuted ? '取消静音' : '静音' }}
          </button>
          <button
            v-if="isVideoCall"
            class="call-overlay__action"
            :class="{ 'is-active': localCameraOff }"
            type="button"
            :disabled="!canEnd"
            @click="emit('toggle-camera')"
          >
            <component :is="localCameraOff ? VideoPause : VideoCamera" />
            {{ localCameraOff ? '打开摄像头' : '关闭摄像头' }}
          </button>
          <button
            v-if="canAccept"
            class="call-overlay__action is-accept"
            type="button"
            :disabled="busy"
            @click="emit('accept')"
          >
            <Phone />
            接听
          </button>
          <button
            v-if="canAccept"
            class="call-overlay__action is-danger"
            type="button"
            :disabled="busy"
            @click="emit('reject')"
          >
            挂断
          </button>
          <button
            v-else-if="canCancel"
            class="call-overlay__action is-danger"
            type="button"
            :disabled="busy"
            @click="emit('cancel')"
          >
            取消呼叫
          </button>
          <button
            v-else-if="canEnd"
            class="call-overlay__action is-danger"
            type="button"
            :disabled="busy"
            @click="emit('end')"
          >
            挂断
          </button>
        </div>
      </div>
    </aside>
  </transition>
</template>

<style scoped>
.call-overlay {
  position: fixed;
  right: 22px;
  bottom: 22px;
  z-index: 60;
  width: min(360px, calc(100vw - 28px));
}

/* Video call: full-screen overlay */
.call-overlay.is-video {
  right: 0;
  bottom: 0;
  width: 100vw;
  height: 100vh;
}

.call-overlay.is-video.is-minimized {
  right: 22px;
  bottom: 22px;
  width: min(280px, calc(100vw - 28px));
  height: auto;
}

.call-overlay__shell {
  display: grid;
  gap: 14px;
  padding: 20px;
  border: 1px solid color-mix(in srgb, var(--border-default) 82%, rgba(255, 255, 255, 0.25));
  border-radius: var(--radius-card);
  background:
    radial-gradient(circle at top, color-mix(in srgb, var(--surface-card) 92%, var(--interactive-focus-ring) 8%), transparent 72%),
    color-mix(in srgb, var(--surface-overlay) 94%, rgba(7, 17, 24, 0.05));
  box-shadow: var(--shadow-overlay);
  backdrop-filter: blur(28px);
}

/* Video call shell: transparent overlay on top of video */
.call-overlay.is-video:not(.is-minimized) .call-overlay__shell {
  position: absolute;
  inset: 0;
  border: 0;
  border-radius: 0;
  background: linear-gradient(to bottom, rgba(0, 0, 0, 0.55) 0%, transparent 30%, transparent 70%, rgba(0, 0, 0, 0.65) 100%);
  box-shadow: none;
  backdrop-filter: none;
  align-content: space-between;
  padding: 24px;
}

.call-overlay.is-minimized {
  width: min(280px, calc(100vw - 28px));
  max-height: 120px;
  overflow: hidden;
}

.call-overlay__header,
.call-overlay__mini,
.call-overlay__actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.call-overlay__eyebrow {
  color: var(--text-quaternary);
  font: 600 var(--text-xs)/1 var(--font-body);
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

/* Video call eyebrow: white text */
.call-overlay.is-video:not(.is-minimized) .call-overlay__eyebrow {
  color: rgba(255, 255, 255, 0.7);
}

.call-overlay__ghost,
.call-overlay__action {
  min-height: var(--btn-min-size);
  border: 1px solid var(--border-default);
  border-radius: var(--radius-pill);
  background: color-mix(in srgb, var(--interactive-secondary-bg) 88%, transparent);
  color: var(--text-secondary);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 10px 15px;
  cursor: pointer;
  transition:
    border-color var(--motion-fast) var(--motion-ease-out),
    background var(--motion-fast) var(--motion-ease-out),
    color var(--motion-fast) var(--motion-ease-out),
    box-shadow var(--motion-fast) var(--motion-ease-out);
}

/* Video call ghost button: semi-transparent */
.call-overlay.is-video:not(.is-minimized) .call-overlay__ghost {
  border-color: rgba(255, 255, 255, 0.2);
  background: rgba(0, 0, 0, 0.3);
  color: rgba(255, 255, 255, 0.9);
}

.call-overlay__ghost {
  min-height: var(--btn-min-size);
  padding: 8px 12px;
}

.call-overlay__body {
  display: grid;
  justify-items: center;
  gap: 12px;
  text-align: center;
}

.call-overlay__copy strong,
.call-overlay__mini strong {
  display: block;
  font-size: 1rem;
  font-weight: 700;
}

.call-overlay__mini {
  min-width: 0;
}

.call-overlay__mini strong {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.call-overlay__mini p {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.call-overlay__copy p,
.call-overlay__mini p {
  margin: 4px 0 0;
  color: var(--text-quaternary);
}

.call-overlay__error {
  margin: 0;
  color: var(--status-danger);
  font-size: var(--text-sm);
}

/* Video call error: white text on dark bg */
.call-overlay.is-video:not(.is-minimized) .call-overlay__error {
  color: #ff8a80;
}

.call-overlay__actions {
  flex-wrap: wrap;
  justify-content: center;
}

/* Video call action buttons: semi-transparent */
.call-overlay.is-video:not(.is-minimized) .call-overlay__action {
  border-color: rgba(255, 255, 255, 0.2);
  background: rgba(0, 0, 0, 0.35);
  color: rgba(255, 255, 255, 0.9);
}

.call-overlay.is-video:not(.is-minimized) .call-overlay__action.is-active {
  border-color: rgba(255, 255, 255, 0.35);
  background: rgba(255, 255, 255, 0.15);
  color: #fff;
}

.call-overlay.is-video:not(.is-minimized) .call-overlay__action.is-accept {
  border-color: rgba(76, 175, 80, 0.5);
  background: rgba(76, 175, 80, 0.25);
  color: #a5d6a7;
}

.call-overlay.is-video:not(.is-minimized) .call-overlay__action.is-danger {
  border-color: rgba(244, 67, 54, 0.4);
  background: rgba(244, 67, 54, 0.2);
  color: #ef9a9a;
}

.call-overlay__action.is-active {
  border-color: color-mix(in srgb, var(--interactive-primary-bg) 24%, var(--border-default));
  background: color-mix(in srgb, var(--interactive-selected-bg) 88%, var(--interactive-secondary-bg));
  color: var(--interactive-selected-fg);
}

.call-overlay__action.is-accept {
  border-color: color-mix(in srgb, var(--status-success) 24%, var(--border-default));
  background: color-mix(in srgb, var(--status-success) 12%, var(--interactive-secondary-bg));
  color: color-mix(in srgb, var(--status-success) 82%, var(--text-primary));
}

.call-overlay__action.is-danger {
  border-color: color-mix(in srgb, var(--status-danger) 24%, var(--border-default));
  background: color-mix(in srgb, var(--status-danger) 10%, var(--interactive-secondary-bg));
  color: color-mix(in srgb, var(--status-danger) 82%, var(--text-primary));
}

.call-overlay__ghost:hover,
.call-overlay__ghost:focus-visible,
.call-overlay__action:hover,
.call-overlay__action:focus-visible {
  border-color: var(--border-strong);
  background: var(--interactive-secondary-bg-hover);
  color: var(--text-primary);
}

.call-overlay__action.is-accept:hover,
.call-overlay__action.is-accept:focus-visible {
  border-color: color-mix(in srgb, var(--status-success) 34%, var(--border-default));
  background: color-mix(in srgb, var(--status-success) 16%, var(--interactive-secondary-bg-hover));
}

.call-overlay__action.is-danger:hover,
.call-overlay__action.is-danger:focus-visible {
  border-color: color-mix(in srgb, var(--status-danger) 34%, var(--border-default));
  background: color-mix(in srgb, var(--status-danger) 14%, var(--interactive-secondary-bg-hover));
}

/* ---- Remote video (fullscreen background) ---- */
.call-overlay__remote-video {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
  z-index: 0;
  background: #000;
}

/* ---- Local video PiP ---- */
.call-overlay__local-pip {
  position: absolute;
  z-index: 2;
  bottom: 100px;
  right: 24px;
  width: 140px;
  height: 186px;
  border-radius: 12px;
  overflow: hidden;
  border: 2px solid rgba(255, 255, 255, 0.25);
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.4);
  cursor: grab;
  background: #1a1a2e;
}

.call-overlay__local-video {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transform: scaleX(-1); /* mirror */
}

.call-overlay__local-video-off {
  position: absolute;
  inset: 0;
  display: grid;
  place-items: center;
  background: color-mix(in srgb, var(--surface-card) 85%, #000);
}

/* ---- Video status overlay (peer name during video call) ---- */
.call-overlay__video-status {
  text-align: center;
  padding-top: 8px;
}

.call-overlay__video-status strong {
  display: block;
  font-size: 1.25rem;
  font-weight: 700;
  color: #fff;
  text-shadow: 0 1px 4px rgba(0, 0, 0, 0.5);
}

.call-overlay__video-status p {
  margin: 6px 0 0;
  color: rgba(255, 255, 255, 0.75);
  font-size: var(--text-sm);
  text-shadow: 0 1px 3px rgba(0, 0, 0, 0.4);
}

.call-overlay-enter-active,
.call-overlay-leave-active {
  transition: opacity var(--motion-fast) ease, transform var(--motion-fast) ease;
}

.call-overlay-enter-from,
.call-overlay-leave-to {
  opacity: 0;
  transform: translateY(14px) scale(0.98);
}

/* Video call transitions: fade only, no slide */
.call-overlay.is-video.call-overlay-enter-from,
.call-overlay.is-video.call-overlay-leave-to {
  transform: none;
}

@media (max-width: 767px) {
  .call-overlay:not(.is-video) {
    right: 14px;
    left: 14px;
    bottom: 14px;
    width: auto;
  }

  .call-overlay__local-pip {
    width: 100px;
    height: 133px;
    bottom: 90px;
    right: 16px;
  }
}
</style>
