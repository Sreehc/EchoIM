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
  </div>
</template>

<style scoped>
.avatar-badge {
  position: relative;
  display: grid;
  place-items: center;
  overflow: hidden;
  border-radius: 50%;
  border: 1px solid color-mix(in srgb, var(--color-line) 70%, transparent);
  color: #fff;
  font-family: var(--font-display);
  font-weight: 700;
  flex-shrink: 0;
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.12);
}

.avatar-badge.size-sm {
  width: 28px;
  height: 28px;
  font-size: 0.68rem;
}

.avatar-badge.size-md {
  width: 38px;
  height: 38px;
  font-size: 0.84rem;
}

.avatar-badge.size-lg {
  width: 44px;
  height: 44px;
  font-size: 0.92rem;
}

.avatar-badge.size-xl {
  width: 52px;
  height: 52px;
  font-size: 0.98rem;
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
}

.type-user.tone-0 {
  background: linear-gradient(135deg, #97abc1 0%, #75879d 100%);
}

.type-user.tone-1 {
  background: linear-gradient(135deg, #96afc3 0%, #768ea2 100%);
}

.type-user.tone-2 {
  background: linear-gradient(135deg, #95ada5 0%, #718781 100%);
}

.type-user.tone-3 {
  background: linear-gradient(135deg, #a3a1c4 0%, #7f7ea1 100%);
}

.type-user.tone-4 {
  background: linear-gradient(135deg, #a4abb3 0%, #818892 100%);
}

.type-group.tone-0 {
  background: linear-gradient(135deg, #6d7e98 0%, #586881 100%);
}

.type-group.tone-1 {
  background: linear-gradient(135deg, #6f7f90 0%, #5a6877 100%);
}

.type-group.tone-2 {
  background: linear-gradient(135deg, #708080 0%, #5b6969 100%);
}

.type-group.tone-3 {
  background: linear-gradient(135deg, #787598 0%, #625f7d 100%);
}

.type-group.tone-4 {
  background: linear-gradient(135deg, #737983 0%, #5e656f 100%);
}

.type-channel.tone-0 {
  background: linear-gradient(135deg, #56799d 0%, #3f5f83 100%);
}

.type-channel.tone-1 {
  background: linear-gradient(135deg, #4d7692 0%, #3d6077 100%);
}

.type-channel.tone-2 {
  background: linear-gradient(135deg, #547b88 0%, #41626d 100%);
}

.type-channel.tone-3 {
  background: linear-gradient(135deg, #5f7198 0%, #485982 100%);
}

.type-channel.tone-4 {
  background: linear-gradient(135deg, #546f86 0%, #415969 100%);
}
</style>
