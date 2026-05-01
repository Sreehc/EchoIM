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
    <nav class="skip-nav" aria-label="辅助跳转">
      <a href="#chat-main" class="skip-link">跳到主内容</a>
    </nav>
    <div class="chat-shell__content">
      <RouterView />
    </div>
  </div>
</template>

<style scoped>
.chat-shell {
  position: relative;
  display: flex;
  flex-direction: column;
  height: 100dvh;
  padding: 0;
  overflow: hidden;
  background: var(--surface-canvas);
}

.chat-shell__backdrop {
  position: absolute;
  inset: 0;
  background:
    radial-gradient(circle at 82% 10%, color-mix(in srgb, var(--interactive-focus-ring) 38%, transparent), transparent 20%),
    radial-gradient(circle at 50% 110%, color-mix(in srgb, var(--surface-subtle) 72%, transparent), transparent 26%);
  pointer-events: none;
  opacity: 0.45;
}

.skip-link {
  position: absolute;
  left: 24px;
  top: -48px;
  z-index: 10;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 40px;
  padding: 10px 14px;
  border-radius: var(--radius-pill);
  background: color-mix(in srgb, var(--interactive-primary-bg) 84%, white);
  color: #fff;
  white-space: nowrap;
  writing-mode: horizontal-tb;
  line-height: 1;
  transition: top var(--motion-fast) ease;
}

.skip-link:focus-visible {
  top: 16px;
}

.skip-nav {
  position: absolute;
  inset: 0 auto auto 0;
  z-index: 10;
}

.chat-shell__content {
  position: relative;
  flex: 1;
  min-height: 0;
  padding: 16px;
}

@media (max-width: 767px) {
  .chat-shell__content {
    padding: 0;
  }
}
</style>
