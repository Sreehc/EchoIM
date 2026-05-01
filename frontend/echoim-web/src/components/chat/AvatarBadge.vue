<script setup lang="ts">
import { computed } from 'vue'
import { UserFilled, User } from '@element-plus/icons-vue'

const props = withDefaults(
  defineProps<{
    name?: string | null
    seed?: string | number | null
    avatarUrl?: string | null
    size?: 'sm' | 'md' | 'lg' | 'xl'
    online?: boolean
    type?: 'user' | 'group' | 'channel'
  }>(),
  {
    name: '',
    seed: null,
    avatarUrl: null,
    size: 'md',
    online: false,
    type: 'user',
  },
)

const initials = computed(() => {
  const source = props.name?.trim() ?? ''
  if (!source) {
    if (props.type === 'group') return '群'
    if (props.type === 'channel') return '频'
    return ''
  }
  return source.slice(0, 1).toUpperCase()
})

const toneIndex = computed(() => {
  const source = String(props.seed ?? props.name ?? '')
  let hash = 0
  for (const char of source) {
    hash = (hash * 31 + char.charCodeAt(0)) % 5
  }
  return hash
})

const toneClass = computed(() => `tone-${toneIndex.value}`)
</script>

<template>
  <div class="avatar-badge" :class="[`size-${size}`, `type-${type}`, toneClass]">
    <img v-if="avatarUrl" :src="avatarUrl" :alt="name || 'avatar'" class="avatar-badge__image" />
    <template v-else>
      <span v-if="initials" class="avatar-badge__initials">{{ initials }}</span>
      <component :is="type === 'user' ? User : UserFilled" v-else class="avatar-badge__icon" />
    </template>
    <span v-if="online && type === 'user'" class="avatar-badge__online" aria-label="在线" />
  </div>
</template>

<style scoped>
.avatar-badge {
  position: relative;
  display: grid;
  place-items: center;
  overflow: hidden;
  border-radius: 50%;
  border: 1px solid color-mix(in srgb, var(--border-subtle) 70%, transparent);
  color: #fff;
  font-family: var(--font-display);
  font-weight: 700;
  flex-shrink: 0;
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.12);
}

.avatar-badge.size-sm {
  width: 28px;
  height: 28px;
  font-size: var(--text-xs);
}

.avatar-badge.size-md {
  width: 38px;
  height: 38px;
  font-size: var(--text-base);
}

.avatar-badge.size-lg {
  width: 44px;
  height: 44px;
  font-size: var(--text-base);
}

.avatar-badge.size-xl {
  width: 52px;
  height: 52px;
  font-size: var(--text-lg);
}

.avatar-badge__image {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.avatar-badge__icon {
  width: 50%;
  height: 50%;
  opacity: 0.92;
}

.avatar-badge__initials {
  line-height: 1;
  display: flex;
  align-items: center;
  justify-content: center;
}

.avatar-badge__online {
  position: absolute;
  bottom: 0;
  right: 0;
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: #22c55e;
  border: 2px solid var(--surface-card);
  box-shadow: 0 0 0 1px rgba(0, 0, 0, 0.08);
}

.size-sm .avatar-badge__online {
  width: 8px;
  height: 8px;
  border-width: 1.5px;
}

.size-xl .avatar-badge__online {
  width: 12px;
  height: 12px;
  border-width: 2.5px;
}

.type-user.tone-0 {
  background: linear-gradient(135deg, #8da0bc 0%, #6d809a 100%);
}

.type-user.tone-1 {
  background: linear-gradient(135deg, #b09a87 0%, #8e7b6b 100%);
}

.type-user.tone-2 {
  background: linear-gradient(135deg, #82a08b 0%, #65826e 100%);
}

.type-user.tone-3 {
  background: linear-gradient(135deg, #9e94b8 0%, #7e7599 100%);
}

.type-user.tone-4 {
  background: linear-gradient(135deg, #a69688 0%, #887969 100%);
}

.type-group.tone-0 {
  background: linear-gradient(135deg, #6a7e98 0%, #546881 100%);
}

.type-group.tone-1 {
  background: linear-gradient(135deg, #7d8a6e 0%, #646f58 100%);
}

.type-group.tone-2 {
  background: linear-gradient(135deg, #658688 0%, #4f6d6f 100%);
}

.type-group.tone-3 {
  background: linear-gradient(135deg, #8a7590 0%, #6e5d76 100%);
}

.type-group.tone-4 {
  background: linear-gradient(135deg, #857e74 0%, #6b655c 100%);
}

.type-channel.tone-0 {
  background: linear-gradient(135deg, #4e7198 0%, #3a5a7e 100%);
}

.type-channel.tone-1 {
  background: linear-gradient(135deg, #50847e 0%, #3d6b66 100%);
}

.type-channel.tone-2 {
  background: linear-gradient(135deg, #6b6290 0%, #534c75 100%);
}

.type-channel.tone-3 {
  background: linear-gradient(135deg, #7a7058 0%, #605843 100%);
}

.type-channel.tone-4 {
  background: linear-gradient(135deg, #487472 0%, #365b5a 100%);
}
</style>
