<script setup lang="ts">
import type { ConversationProfile, ConversationSummary } from '@/types/chat'
import ConversationProfilePanelBody from './ConversationProfilePanelBody.vue'

defineProps<{
  conversation: ConversationSummary | null
  profile: ConversationProfile | null
  loading?: boolean
  errorMessage?: string | null
  overlay: boolean
  visible: boolean
}>()

const emit = defineEmits<{
  'update:visible': [value: boolean]
  action: [command: 'toggle-top' | 'toggle-mute' | 'mark-read']
  'open-public-profile': [path: string]
  'update-group-meta': []
  'update-group-notice': []
  'promote-member': [payload: { userId: number; role: 2 | 3 }]
  'remove-member': [userId: number]
  'add-members': []
  'leave-group': []
  'dissolve-group': []
}>()
</script>

<template>
  <el-drawer
    v-if="overlay"
    class="conversation-profile-drawer"
    body-class="conversation-profile-drawer__body"
    :model-value="visible"
    :with-header="false"
    direction="rtl"
    size="392px"
    @close="emit('update:visible', false)"
  >
    <ConversationProfilePanelBody
      :conversation="conversation"
      :profile="profile"
      :loading="loading"
      :error-message="errorMessage"
      @close="emit('update:visible', false)"
      @action="emit('action', $event)"
      @open-public-profile="emit('open-public-profile', $event)"
      @update-group-meta="emit('update-group-meta')"
      @update-group-notice="emit('update-group-notice')"
      @promote-member="emit('promote-member', $event)"
      @remove-member="emit('remove-member', $event)"
      @add-members="emit('add-members')"
      @leave-group="emit('leave-group')"
      @dissolve-group="emit('dissolve-group')"
    />
  </el-drawer>

  <aside v-else class="profile-panel-shell">
    <ConversationProfilePanelBody
      :conversation="conversation"
      :profile="profile"
      :loading="loading"
      :error-message="errorMessage"
      @close="emit('update:visible', false)"
      @action="emit('action', $event)"
      @open-public-profile="emit('open-public-profile', $event)"
      @update-group-meta="emit('update-group-meta')"
      @update-group-notice="emit('update-group-notice')"
      @promote-member="emit('promote-member', $event)"
      @remove-member="emit('remove-member', $event)"
      @add-members="emit('add-members')"
      @leave-group="emit('leave-group')"
      @dissolve-group="emit('dissolve-group')"
    />
  </aside>
</template>

<style scoped>
:global(.conversation-profile-drawer),
:global(.conversation-profile-drawer .el-drawer),
.profile-panel-shell {
  background: transparent;
}

:global(.conversation-profile-drawer .el-drawer) {
  overflow: hidden;
  box-shadow: none;
  border-left: 1px solid var(--color-shell-border);
  background: color-mix(in srgb, var(--color-shell-panel) 94%, transparent);
  backdrop-filter: blur(16px) saturate(108%);
}

:global(.conversation-profile-drawer__body),
:global(.conversation-profile-drawer .el-drawer__body) {
  height: 100%;
  padding: 0;
  background: transparent;
}

.profile-panel-shell {
  height: 100%;
  min-height: 0;
  overflow: hidden;
  background: color-mix(in srgb, var(--color-shell-panel) 94%, transparent);
  backdrop-filter: blur(16px) saturate(108%);
}
</style>
