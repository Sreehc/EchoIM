<script setup lang="ts">
import { computed, onBeforeUnmount, ref, watch } from 'vue'
import { Microphone, Phone, TopRight } from '@element-plus/icons-vue'
import type { CallPhase, CallSessionSummary } from '@/types/chat'
import AvatarBadge from './AvatarBadge.vue'

const props = defineProps<{
  visible: boolean
  call: CallSessionSummary | null
  phase: CallPhase
  minimized: boolean
  localMuted: boolean
  busy?: boolean
  error?: string | null
  remoteStream: MediaStream | null
}>()

const emit = defineEmits<{
  accept: []
  reject: []
  cancel: []
  end: []
  'toggle-minimized': []
  'toggle-mute': []
}>()

const remoteAudio = ref<HTMLAudioElement | null>(null)

const statusLabel = computed(() => {
  if (props.phase === 'incoming') return '语音来电'
  if (props.phase === 'outgoing') return '等待对方接听'
  if (props.phase === 'connecting') return '正在建立语音连接'
  if (props.phase === 'connected') return formatDuration(props.call?.durationSeconds ?? 0)
  if (props.call?.status === 'missed') return '未接来电'
  if (props.call?.status === 'rejected') return '对方已拒绝'
  if (props.call?.status === 'cancelled') return '通话已取消'
  if (props.call?.status === 'ended') return '通话已结束'
  return '语音通话'
})

const canAccept = computed(() => props.phase === 'incoming')
const canCancel = computed(() => props.phase === 'outgoing')
const canEnd = computed(() => props.phase === 'connecting' || props.phase === 'connected')

watch(
  () => [props.remoteStream, remoteAudio.value] as const,
  ([stream, audio]) => {
    if (!audio) return
    audio.srcObject = stream
  },
  { immediate: true },
)

onBeforeUnmount(() => {
  if (remoteAudio.value) {
    remoteAudio.value.srcObject = null
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
    <aside v-if="visible && call" class="call-overlay" :class="{ 'is-minimized': minimized }">
      <audio ref="remoteAudio" autoplay playsinline />
      <div class="call-overlay__shell">
        <div class="call-overlay__header">
          <span class="call-overlay__eyebrow">EchoIM Voice</span>
          <button class="call-overlay__ghost" type="button" @click="emit('toggle-minimized')">
            <TopRight />
            {{ minimized ? '展开' : '最小化' }}
          </button>
        </div>

        <div v-if="!minimized" class="call-overlay__body">
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
          <p v-if="error" class="call-overlay__error">{{ error }}</p>
        </div>

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

.call-overlay.is-minimized {
  width: min(280px, calc(100vw - 28px));
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
  font: 600 0.74rem/1 var(--font-body);
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.call-overlay__ghost,
.call-overlay__action {
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

.call-overlay__ghost {
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

.call-overlay__copy p,
.call-overlay__mini p {
  margin: 4px 0 0;
  color: var(--text-quaternary);
}

.call-overlay__error {
  margin: 0;
  color: var(--status-danger);
  font-size: 0.82rem;
}

.call-overlay__actions {
  flex-wrap: wrap;
  justify-content: center;
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

.call-overlay-enter-active,
.call-overlay-leave-active {
  transition: opacity var(--motion-fast) ease, transform var(--motion-fast) ease;
}

.call-overlay-enter-from,
.call-overlay-leave-to {
  opacity: 0;
  transform: translateY(14px) scale(0.98);
}

@media (max-width: 767px) {
  .call-overlay {
    right: 14px;
    left: 14px;
    bottom: 14px;
    width: auto;
  }
}
</style>
