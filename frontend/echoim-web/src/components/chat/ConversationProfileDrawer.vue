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
    size="420px"
    @close="emit('update:visible', false)"
  >
    <ConversationProfilePanelBody
      :conversation="conversation"
      :profile="profile"
      :loading="loading"
      :error-message="errorMessage"
      @close="emit('update:visible', false)"
      @action="emit('action', $event)"
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
  border-left: 1px solid var(--color-line);
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
  border-left: 1px solid var(--color-line);
  overflow: hidden;
}
</style>
