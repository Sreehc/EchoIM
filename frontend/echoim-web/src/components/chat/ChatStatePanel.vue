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
  gap: 10px;
  justify-items: center;
  text-align: center;
  color: var(--color-text-2);
}

.state-panel--compact {
  min-height: 240px;
}

.state-panel__eyebrow {
  color: var(--color-text-soft);
  font: 600 0.68rem/1 var(--font-mono);
  text-transform: uppercase;
  letter-spacing: 0.08em;
}

.state-panel strong {
  color: var(--color-text-1);
  font-size: 0.96rem;
  line-height: 1.15;
}

.state-panel p {
  max-width: 30ch;
  color: var(--color-text-3);
  font-size: 0.86rem;
  line-height: 1.5;
}

.state-panel__action {
  min-height: 36px;
  padding: 0 14px;
  border: 1px solid var(--color-line);
  border-radius: 999px;
  background: var(--color-bg-elevated);
  color: var(--color-text-1);
  font-size: 0.84rem;
  font-weight: 600;
}

.state-panel__action:hover,
.state-panel__action:focus-visible {
  border-color: color-mix(in srgb, var(--color-primary) 24%, var(--color-line));
  background: var(--color-selected);
}
</style>
