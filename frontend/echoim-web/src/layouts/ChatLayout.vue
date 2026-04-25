<script setup lang="ts">
import { onMounted } from 'vue'
import { useUiStore } from '@/stores/ui'

const uiStore = useUiStore()

onMounted(() => {
  uiStore.applyTheme(uiStore.theme)
  uiStore.initializeViewport()
})
</script>

<template>
  <div class="chat-shell">
    <div class="chat-shell__backdrop"></div>
    <a href="#chat-main" class="skip-link">跳到主内容</a>
    <RouterView />
  </div>
</template>

<style scoped>
.chat-shell {
  position: relative;
  min-height: 100dvh;
  padding: 12px;
}

.chat-shell__backdrop {
  position: absolute;
  inset: 0;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.02), transparent 20%);
  pointer-events: none;
}

.skip-link {
  position: absolute;
  left: 24px;
  top: -48px;
  z-index: 10;
  padding: 10px 14px;
  border-radius: 999px;
  background: var(--color-primary);
  color: #fff;
  transition: top var(--motion-fast) ease;
}

.skip-link:focus-visible {
  top: 16px;
}

@media (max-width: 767px) {
  .chat-shell {
    padding: 0;
  }
}
</style>
