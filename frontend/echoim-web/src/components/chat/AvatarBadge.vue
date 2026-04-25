<script setup lang="ts">
import { computed } from 'vue'
import { UserFilled, User } from '@element-plus/icons-vue'

const props = withDefaults(
  defineProps<{
    name?: string | null
    seed?: string | number | null
    avatarUrl?: string | null
    size?: 'sm' | 'md' | 'lg'
    online?: boolean
    type?: 'user' | 'group'
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
  if (!source) return props.type === 'group' ? '群' : ''
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
      <component :is="type === 'group' ? UserFilled : User" v-else class="avatar-badge__icon" />
    </template>
    <span v-if="online" class="avatar-badge__online"></span>
  </div>
</template>

<style scoped>
.avatar-badge {
  position: relative;
  display: grid;
  place-items: center;
  overflow: hidden;
  border-radius: 50%;
  border: 1px solid color-mix(in srgb, var(--color-line) 82%, transparent);
  color: #fff;
  font-family: var(--font-display);
  font-weight: 700;
  flex-shrink: 0;
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

.avatar-badge__online {
  position: absolute;
  right: 1px;
  bottom: 1px;
  width: 10px;
  height: 10px;
  border: 2px solid var(--color-bg-surface);
  border-radius: 50%;
  background: var(--color-online);
}

.type-user.tone-0 {
  background: linear-gradient(135deg, #8fa2ba 0%, #73849c 100%);
}

.type-user.tone-1 {
  background: linear-gradient(135deg, #90aabf 0%, #738ba0 100%);
}

.type-user.tone-2 {
  background: linear-gradient(135deg, #8fa89f 0%, #70867f 100%);
}

.type-user.tone-3 {
  background: linear-gradient(135deg, #9d9bc0 0%, #7f7ea1 100%);
}

.type-user.tone-4 {
  background: linear-gradient(135deg, #9da3ac 0%, #7f8690 100%);
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
</style>
