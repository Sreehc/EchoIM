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
    radial-gradient(circle at top right, color-mix(in srgb, var(--color-shell-glow) 100%, transparent), transparent 30%),
    linear-gradient(180deg, transparent, color-mix(in srgb, var(--color-shell-mask) 12%, transparent)),
    var(--color-bg-app);
}

.auth-shell__texture {
  position: absolute;
  inset: 0;
  background:
    linear-gradient(120deg, rgba(255, 255, 255, 0.03), transparent 22%),
    radial-gradient(circle at 50% 0%, color-mix(in srgb, var(--color-shell-glow) 48%, transparent), transparent 42%);
  pointer-events: none;
}

.auth-shell__glow {
  position: absolute;
  width: 32rem;
  height: 32rem;
  border-radius: 50%;
  pointer-events: none;
  filter: blur(30px);
  opacity: 0.22;
}

.auth-shell__glow--left {
  inset: auto auto -10rem -14rem;
  background: radial-gradient(circle, color-mix(in srgb, var(--color-primary) 14%, transparent), transparent 64%);
}

.auth-shell__glow--right {
  inset: -10rem -14rem auto auto;
  background: radial-gradient(circle, color-mix(in srgb, var(--color-shell-glow) 52%, transparent), transparent 64%);
}
</style>
