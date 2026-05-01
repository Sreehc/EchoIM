<script setup lang="ts">
defineProps<{
  title: string
  description: string
  eyebrow?: string
  compact?: boolean
  actionLabel?: string
  role?: 'status' | 'alert'
  ariaLive?: 'polite' | 'assertive' | 'off'
}>()

const emit = defineEmits<{
  action: []
}>()
</script>

<template>
  <div class="state-panel" :class="{ 'state-panel--compact': compact }" :role="role" :aria-live="ariaLive">
    <span v-if="eyebrow" class="state-panel__eyebrow">{{ eyebrow }}</span>
    <strong>{{ title }}</strong>
    <p>{{ description }}</p>
    <button v-if="actionLabel" class="state-panel__action" type="button" @click="emit('action')">
      {{ actionLabel }}
    </button>
  </div>
</template>

<style scoped>
.state-panel {
  min-height: 100%;
  display: grid;
  place-content: center;
  gap: 12px;
  justify-items: center;
  padding: 30px 24px;
  border: 1px solid var(--border-default);
  border-radius: var(--radius-card);
  background:
    linear-gradient(180deg, color-mix(in srgb, var(--interactive-focus-ring) 18%, transparent), transparent 30%),
    var(--surface-card);
  box-shadow: var(--shadow-sm);
  text-align: center;
  color: var(--text-secondary);
}

.state-panel--compact {
  min-height: 200px;
}

.state-panel__eyebrow {
  color: var(--text-quaternary);
  font: var(--font-eyebrow);
  text-transform: uppercase;
  letter-spacing: 0.12em;
}

.state-panel strong {
  color: var(--text-primary);
  font: var(--font-title-sm);
}

.state-panel p {
  max-width: 30ch;
  color: var(--text-tertiary);
  font-size: var(--text-base);
  line-height: 1.5;
}

.state-panel__action {
  min-height: var(--control-height-sm);
  padding: 0 16px;
  border: 1px solid var(--border-default);
  border-radius: var(--radius-pill);
  background: var(--interactive-secondary-bg);
  color: var(--text-primary);
  font-size: var(--text-base);
  font-weight: 600;
  transition:
    border-color var(--motion-fast) var(--motion-ease-out),
    background var(--motion-fast) var(--motion-ease-out),
    color var(--motion-fast) var(--motion-ease-out);
}

.state-panel__action:hover,
.state-panel__action:focus-visible {
  border-color: var(--border-strong);
  background: var(--interactive-secondary-bg-hover);
}
</style>
