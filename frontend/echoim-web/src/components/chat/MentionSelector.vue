<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import type { GroupMemberItem } from '@/types/chat'
import AvatarBadge from './AvatarBadge.vue'

export interface MentionItem {
  userId: number
  displayName: string
}

const props = defineProps<{
  visible: boolean
  members: GroupMemberItem[]
  query: string
  currentUserId: number
}>()

const emit = defineEmits<{
  select: [mention: MentionItem]
  close: []
}>()

const selectedIndex = ref(0)

const filteredMembers = computed(() => {
  const q = props.query.toLowerCase()
  return props.members
    .filter((m) => m.userId !== props.currentUserId)
    .filter((m) => {
      if (!q) return true
      return (
        m.nickname.toLowerCase().includes(q) ||
        m.userNo.toLowerCase().includes(q)
      )
    })
    .slice(0, 8)
})

watch(() => props.visible, (visible) => {
  if (visible) {
    selectedIndex.value = 0
  }
})

watch(filteredMembers, () => {
  if (selectedIndex.value >= filteredMembers.value.length) {
    selectedIndex.value = Math.max(0, filteredMembers.value.length - 1)
  }
})

function handleKeydown(event: KeyboardEvent) {
  if (!props.visible) return

  if (event.key === 'ArrowDown') {
    event.preventDefault()
    selectedIndex.value = (selectedIndex.value + 1) % filteredMembers.value.length
    return
  }

  if (event.key === 'ArrowUp') {
    event.preventDefault()
    selectedIndex.value = (selectedIndex.value - 1 + filteredMembers.value.length) % filteredMembers.value.length
    return
  }

  if (event.key === 'Enter' || event.key === 'Tab') {
    event.preventDefault()
    selectMember(filteredMembers.value[selectedIndex.value])
    return
  }

  if (event.key === 'Escape') {
    event.preventDefault()
    emit('close')
  }
}

function selectMember(member: GroupMemberItem | undefined) {
  if (!member) return
  emit('select', {
    userId: member.userId,
    displayName: member.nickname,
  })
}

defineExpose({ handleKeydown })
</script>

<template>
  <transition name="mention-selector-fade">
    <div v-if="visible && filteredMembers.length > 0" class="mention-selector" role="listbox" aria-label="选择要提及的成员">
      <button
        v-for="(member, index) in filteredMembers"
        :key="member.userId"
        class="mention-selector__item"
        :class="{ 'is-selected': index === selectedIndex }"
        type="button"
        role="option"
        :aria-selected="index === selectedIndex"
        @click="selectMember(member)"
        @mouseenter="selectedIndex = index"
      >
        <AvatarBadge
          class="mention-selector__avatar"
          :name="member.nickname"
          :seed="member.userId"
          size="sm"
          type="user"
        />
        <div class="mention-selector__info">
          <span class="mention-selector__name">{{ member.nickname }}</span>
          <span class="mention-selector__no">{{ member.userNo }}</span>
        </div>
        <span v-if="member.role === 1" class="mention-selector__role">群主</span>
        <span v-else-if="member.role === 3" class="mention-selector__role">管理员</span>
      </button>
    </div>
  </transition>
</template>

<style scoped>
.mention-selector {
  position: absolute;
  bottom: 100%;
  left: 0;
  right: 0;
  max-height: 240px;
  overflow-y: auto;
  margin-bottom: 4px;
  padding: 6px;
  border: 1px solid var(--border-default);
  border-radius: var(--radius-panel);
  background: var(--surface-overlay);
  box-shadow: var(--shadow-md);
  z-index: 100;
}

.mention-selector__item {
  width: 100%;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 10px;
  border: 0;
  border-radius: var(--radius-md);
  background: transparent;
  text-align: left;
  cursor: pointer;
  transition:
    background var(--motion-fast) var(--motion-ease-out);
}

.mention-selector__item:hover,
.mention-selector__item.is-selected {
  background: color-mix(in srgb, var(--interactive-selected-bg) 90%, transparent);
}

.mention-selector__avatar {
  flex-shrink: 0;
}

.mention-selector__info {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.mention-selector__name {
  font: 600 var(--text-sm)/1.2 var(--font-body);
  color: var(--text-primary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.mention-selector__no {
  font: 500 var(--text-xs)/1 var(--font-mono);
  color: var(--text-quaternary);
}

.mention-selector__role {
  flex-shrink: 0;
  padding: 2px 6px;
  border-radius: var(--radius-pill);
  background: color-mix(in srgb, var(--interactive-primary-bg) 10%, var(--surface-panel));
  color: var(--interactive-selected-fg);
  font: 600 var(--text-2xs)/1 var(--font-mono);
}

.mention-selector-fade-enter-active,
.mention-selector-fade-leave-active {
  transition: opacity 0.15s ease, transform 0.15s ease;
}

.mention-selector-fade-enter-from,
.mention-selector-fade-leave-to {
  opacity: 0;
  transform: translateY(4px);
}
</style>
