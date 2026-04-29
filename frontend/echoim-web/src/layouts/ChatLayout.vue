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
  background: var(--color-bg-app);
}

.chat-shell__backdrop {
  position: absolute;
  inset: 0;
  background:
    radial-gradient(circle at 10% 10%, color-mix(in srgb, var(--color-primary) 12%, transparent), transparent 22%),
    radial-gradient(circle at 82% 10%, color-mix(in srgb, var(--color-shell-glow) 88%, transparent), transparent 18%),
    radial-gradient(circle at 50% 120%, color-mix(in srgb, var(--color-bg-surface) 94%, transparent), transparent 28%),
    linear-gradient(180deg, rgba(255, 255, 255, 0.04), transparent 20%);
  pointer-events: none;
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
  border-radius: 999px;
  background: color-mix(in srgb, var(--color-primary) 84%, white);
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
  padding: 14px;
}

@media (max-width: 767px) {
  .chat-shell__content {
    padding: 0;
  }
}
</style>
