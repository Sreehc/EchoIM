<template>
  <div class="auth-shell">
    <div class="auth-shell__texture"></div>
    <RouterView />
  </div>
</template>

<style scoped>
.auth-shell {
  min-height: 100dvh;
  position: relative;
  overflow: hidden;
}

.auth-shell__texture {
  position: absolute;
  inset: 0;
  background:
    radial-gradient(circle at 50% 0%, rgba(59, 130, 246, 0.035), transparent 42%);
  pointer-events: none;
}
</style>
