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
  border: 1px solid var(--color-shell-border);
  border-radius: 28px;
  background: var(--color-shell-card);
  box-shadow:
    var(--shadow-inset-soft),
    var(--shadow-card);
  text-align: center;
  color: var(--color-text-2);
}

.state-panel--compact {
  min-height: 260px;
}

.state-panel__eyebrow {
  color: var(--color-shell-eyebrow);
  font: var(--font-eyebrow);
  text-transform: uppercase;
  letter-spacing: 0.12em;
}

.state-panel strong {
  color: var(--color-text-1);
  font: var(--font-title-sm);
}

.state-panel p {
  max-width: 30ch;
  color: var(--color-text-3);
  font-size: 0.86rem;
  line-height: 1.5;
}

.state-panel__action {
  min-height: var(--control-height-sm);
  padding: 0 16px;
  border: 1px solid var(--color-shell-border);
  border-radius: 999px;
  background: var(--color-shell-action);
  color: var(--color-text-1);
  font-size: 0.84rem;
  font-weight: 600;
}

.state-panel__action:hover,
.state-panel__action:focus-visible {
  border-color: var(--color-shell-border-strong);
  background: var(--color-shell-action-hover);
}
</style>
