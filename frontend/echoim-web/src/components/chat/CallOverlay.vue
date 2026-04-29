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
  border: 1px solid color-mix(in srgb, var(--color-shell-border) 82%, rgba(255, 255, 255, 0.25));
  border-radius: 30px;
  background:
    radial-gradient(circle at top, color-mix(in srgb, var(--color-shell-card) 94%, #d5ede7 6%), transparent 72%),
    color-mix(in srgb, var(--color-shell-card-strong) 94%, rgba(7, 17, 24, 0.05));
  box-shadow: 0 30px 84px rgba(6, 15, 27, 0.18);
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
  color: var(--color-text-soft);
  font: 600 0.74rem/1 var(--font-body);
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.call-overlay__ghost,
.call-overlay__action {
  border: 1px solid var(--color-shell-border);
  border-radius: 999px;
  background: color-mix(in srgb, var(--color-shell-action) 88%, transparent);
  color: var(--color-text-2);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 10px 15px;
  cursor: pointer;
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
  color: var(--color-text-soft);
}

.call-overlay__error {
  margin: 0;
  color: var(--color-danger);
  font-size: 0.82rem;
}

.call-overlay__actions {
  flex-wrap: wrap;
  justify-content: center;
}

.call-overlay__action.is-active {
  background: color-mix(in srgb, var(--color-shell-card-strong) 65%, #ffc5be 35%);
}

.call-overlay__action.is-accept {
  background: color-mix(in srgb, #d7f6eb 70%, var(--color-shell-card) 30%);
  color: #0f7b74;
}

.call-overlay__action.is-danger {
  background: color-mix(in srgb, #ffd8d2 72%, var(--color-shell-card) 28%);
  color: #bd3e2f;
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
