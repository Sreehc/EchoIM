<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ChatDotRound, Lock, User } from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'
import { useUiStore } from '@/stores/ui'

const authStore = useAuthStore()
const uiStore = useUiStore()
const router = useRouter()

const form = reactive({
  username: 'echo_demo_01',
  password: '123456',
})
const loginError = ref<string | null>(null)

async function submit() {
  loginError.value = null

  try {
    await authStore.login(form.username, form.password)
    router.push('/chat')
  } catch (error) {
    loginError.value = error instanceof Error ? error.message : '登录失败'
  }
}
</script>

<template>
  <main class="login-page">
    <div class="login-shell">
      <header class="login-shell__header">
        <div class="brand-line">
          <ChatDotRound class="brand-line__icon" />
          <strong>EchoIM</strong>
          <span>Web Client</span>
        </div>
        <button class="theme-toggle" type="button" @click="uiStore.toggleTheme">
          {{ uiStore.theme === 'light' ? '深色' : '浅色' }}
        </button>
      </header>

      <section class="login-card">
        <div class="login-card__header">
          <span class="login-card__eyebrow">Sign in</span>
          <h1>进入聊天工作台</h1>
          <p>一个更安静、更工具化的即时通讯界面。演示账号已预填，可直接进入。</p>
        </div>

        <el-form label-position="top" @submit.prevent="submit">
          <el-form-item label="用户名">
            <el-input
              v-model="form.username"
              :prefix-icon="User"
              placeholder="echo_demo_01"
              aria-label="用户名"
            />
          </el-form-item>
          <el-form-item label="密码">
            <el-input
              v-model="form.password"
              :prefix-icon="Lock"
              placeholder="请输入密码"
              show-password
              type="password"
              aria-label="密码"
            />
          </el-form-item>
          <el-button class="login-card__submit" native-type="submit" type="primary" :loading="authStore.isLoading">
            登录
          </el-button>
        </el-form>

        <p v-if="loginError" class="login-card__error" role="alert" aria-live="assertive">
          {{ loginError }}
        </p>

        <div class="login-card__meta">
          <div>
            <span class="login-card__meta-label">演示账号</span>
            <strong>echo_demo_01</strong>
          </div>
          <div>
            <span class="login-card__meta-label">演示密码</span>
            <strong>123456</strong>
          </div>
        </div>
      </section>
    </div>
  </main>
</template>

<style scoped>
.login-page {
  min-height: 100dvh;
  display: grid;
  place-items: center;
  padding: 24px;
}

.login-shell {
  width: min(100%, 396px);
}

.login-shell__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 14px;
}

.brand-line {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 8px 10px;
  border: 1px solid var(--color-line);
  border-radius: 999px;
  background: var(--color-bg-surface);
  font-size: 0.82rem;
}

.brand-line strong {
  font: 700 0.86rem/1 var(--font-display);
}

.brand-line span {
  color: var(--color-text-3);
  font: 500 0.74rem/1 var(--font-mono);
}

.brand-line__icon {
  width: 15px;
  height: 15px;
  color: var(--color-primary);
}

.login-card {
  border: 1px solid var(--color-line);
  border-radius: 16px;
  background: var(--color-bg-surface);
  box-shadow: var(--shadow-soft);
  padding: 20px;
}

.theme-toggle {
  padding: 8px 10px;
  border: 1px solid transparent;
  border-radius: 999px;
  background: transparent;
  color: var(--color-text-2);
  font-size: 0.82rem;
}

.theme-toggle:hover,
.theme-toggle:focus-visible {
  border-color: var(--color-line);
  background: var(--color-hover);
  color: var(--color-text-1);
}

.login-card__header {
  margin-bottom: 18px;
}

.login-card__eyebrow {
  display: inline-block;
  margin-bottom: 10px;
  color: var(--color-text-3);
  font: 600 0.74rem/1 var(--font-mono);
  text-transform: uppercase;
  letter-spacing: 0.12em;
}

.login-card__header h1 {
  margin: 0 0 8px;
  font: 700 1.42rem/1.08 var(--font-display);
  letter-spacing: -0.03em;
}

.login-card__header p,
.login-card__meta {
  color: var(--color-text-2);
  font-size: 0.9rem;
}

.login-card__submit {
  width: 100%;
  margin-top: 6px;
}

.login-card__meta {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
  margin-top: 16px;
  padding-top: 14px;
  border-top: 1px solid var(--color-line);
}

.login-card__meta-label {
  display: block;
  margin-bottom: 4px;
  color: var(--color-text-3);
  font: 500 0.72rem/1 var(--font-mono);
  text-transform: uppercase;
}

.login-card__meta strong {
  font: 600 0.92rem/1.2 var(--font-body);
}

.login-card__error {
  margin-top: 12px;
  color: var(--color-danger);
  font-size: 0.84rem;
  line-height: 1.45;
}

@media (max-width: 640px) {
  .login-page {
    padding: 16px;
  }

  .login-card__meta {
    grid-template-columns: 1fr;
  }
}
</style>
