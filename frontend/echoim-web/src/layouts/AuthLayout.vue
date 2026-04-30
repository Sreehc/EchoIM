<template>
  <div class="auth-shell">
    <div class="auth-shell__texture"></div>
    <div class="auth-shell__glow auth-shell__glow--left"></div>
    <div class="auth-shell__glow auth-shell__glow--right"></div>
    <RouterView />
  </div>
</template>

<style scoped>
.auth-shell {
  min-height: 100dvh;
  position: relative;
  overflow: hidden;
  background:
    radial-gradient(circle at 16% 18%, color-mix(in srgb, var(--interactive-focus-ring) 64%, transparent), transparent 24%),
    radial-gradient(circle at 82% 10%, color-mix(in srgb, var(--interactive-primary-bg) 10%, transparent), transparent 22%),
    linear-gradient(180deg, transparent, color-mix(in srgb, var(--surface-inverse) 10%, transparent)),
    var(--surface-canvas);
}

.auth-shell__texture {
  position: absolute;
  inset: 0;
  background:
    linear-gradient(120deg, rgba(255, 255, 255, 0.03), transparent 22%),
    radial-gradient(circle at 50% 0%, color-mix(in srgb, var(--interactive-focus-ring) 40%, transparent), transparent 42%),
    linear-gradient(90deg, rgba(255, 255, 255, 0.015), transparent 24%, transparent 76%, rgba(255, 255, 255, 0.015));
  pointer-events: none;
  opacity: 0.76;
}

.auth-shell__glow {
  position: absolute;
  width: 38rem;
  height: 38rem;
  border-radius: 50%;
  pointer-events: none;
  filter: blur(58px);
  opacity: 0.14;
}

.auth-shell__glow--left {
  inset: auto auto -14rem -12rem;
  background: radial-gradient(circle, color-mix(in srgb, var(--interactive-primary-bg) 16%, transparent), transparent 64%);
}

.auth-shell__glow--right {
  inset: -12rem -16rem auto auto;
  background: radial-gradient(circle, color-mix(in srgb, var(--interactive-focus-ring) 42%, transparent), transparent 64%);
}
</style>
