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
          <span>随时保持联系</span>
        </div>
        <button class="theme-toggle" type="button" @click="uiStore.toggleTheme">
          {{ uiStore.theme === 'light' ? '深色' : '浅色' }}
        </button>
      </header>

      <section class="login-card">
        <div class="login-card__intro">
          <span class="login-card__eyebrow">欢迎使用 EchoIM</span>
          <h1>登录后，继续和重要的人保持沟通</h1>
          <p>在这里收发消息、查看未读提醒，常用会话会保持同步。演示账号已预填，登录后可以直接体验完整聊天界面。</p>
          <div class="login-card__highlights">
            <article>
              <strong>消息同步</strong>
              <p>新消息、未读状态和常用会话会自动更新。</p>
            </article>
            <article>
              <strong>清爽易读</strong>
              <p>界面更简洁，聊天记录和联系人信息一眼就能看清。</p>
            </article>
            <article>
              <strong>日夜模式</strong>
              <p>白天和夜晚都保持舒适观感，长时间使用也更轻松。</p>
            </article>
          </div>
        </div>

        <div class="login-card__section">
          <div class="login-card__section-head">
            <span>账号登录</span>
            <strong>安全登录</strong>
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
  padding: 32px 24px;
}

.login-shell {
  width: min(100%, 880px);
}

.login-shell__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.brand-line {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  padding: 11px 16px;
  border: 1px solid var(--color-shell-border);
  border-radius: 999px;
  background: var(--color-shell-toolbar);
  box-shadow: var(--shadow-card);
  font-size: 0.82rem;
}

.brand-line strong {
  font: var(--font-title-sm);
}

.brand-line span {
  color: var(--color-text-3);
  font: 500 0.8rem/1 var(--font-body);
}

.brand-line__icon {
  width: 16px;
  height: 16px;
  color: var(--color-shell-eyebrow);
}

.login-card {
  position: relative;
  display: grid;
  grid-template-columns: minmax(0, 1.08fr) minmax(320px, 0.92fr);
  gap: 22px;
  overflow: hidden;
  border: 1px solid var(--color-shell-border);
  border-radius: 30px;
  background:
    radial-gradient(circle at bottom right, color-mix(in srgb, var(--color-shell-glow) 52%, transparent), transparent 28%),
    var(--color-shell-panel);
  box-shadow:
    var(--shadow-panel),
    var(--shadow-inset-soft);
  padding: 24px;
}

.theme-toggle {
  min-height: 40px;
  padding: 0 15px;
  border: 1px solid var(--color-shell-border);
  border-radius: 999px;
  background: var(--color-shell-action);
  color: var(--color-text-2);
  font-size: 0.82rem;
  font-weight: 600;
}

.theme-toggle:hover,
.theme-toggle:focus-visible {
  border-color: var(--color-shell-border-strong);
  background: var(--color-shell-action-hover);
  color: var(--color-text-1);
}

.login-card__intro {
  display: grid;
  align-content: space-between;
  gap: 18px;
  padding: 10px 4px 10px 4px;
}

.login-card__eyebrow {
  display: inline-block;
  color: var(--color-shell-eyebrow);
  font: 700 0.74rem/1 var(--font-body);
  letter-spacing: 0.08em;
}

.login-card__intro h1 {
  max-width: 11ch;
  font: var(--font-title-lg);
  letter-spacing: -0.05em;
}

.login-card__intro p,
.login-card__meta {
  color: var(--color-text-2);
  font-size: 0.92rem;
  line-height: 1.6;
}

.login-card__highlights {
  display: grid;
  gap: 10px;
}

.login-card__highlights article {
  padding: 14px 16px;
  border: 1px solid var(--color-shell-border);
  border-radius: 18px;
  background: color-mix(in srgb, var(--color-shell-card-muted) 92%, transparent);
}

.login-card__highlights strong {
  display: block;
  margin-bottom: 6px;
  font: var(--font-title-sm);
}

.login-card__highlights p {
  font-size: 0.84rem;
  line-height: 1.55;
}

.login-card__section {
  padding: 20px;
  border: 1px solid var(--color-shell-border);
  border-radius: 24px;
  background: var(--color-shell-card-strong);
  box-shadow: var(--shadow-inset-soft);
}

.login-card__section-head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 10px;
  margin-bottom: 16px;
}

.login-card__section-head span {
  color: var(--color-text-1);
  font: var(--font-title-sm);
}

.login-card__section-head strong {
  color: var(--color-text-soft);
  font: 600 0.76rem/1 var(--font-body);
  letter-spacing: 0.08em;
}

.login-card__submit {
  width: 100%;
  margin-top: 6px;
  min-height: var(--control-height-lg);
}

.login-card__meta {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
  margin-top: 18px;
}

.login-card__meta > div {
  padding: 15px 16px;
  border: 1px solid var(--color-shell-border);
  border-radius: 18px;
  background: var(--color-shell-card-muted);
}

.login-card__meta-label {
  display: block;
  margin-bottom: 4px;
  color: var(--color-text-soft);
  font: var(--font-eyebrow);
  text-transform: uppercase;
}

.login-card__meta strong {
  font: 600 0.92rem/1.2 var(--font-body);
}

.login-card__error {
  margin-top: 14px;
  padding: 10px 12px;
  border-radius: 14px;
  background: color-mix(in srgb, var(--color-danger) 9%, var(--color-shell-card-muted));
  color: var(--color-danger);
  font-size: 0.84rem;
  line-height: 1.45;
}

@media (max-width: 640px) {
  .login-page {
    padding: 16px;
  }

  .login-shell {
    width: min(100%, 520px);
  }

  .login-card {
    grid-template-columns: 1fr;
    gap: 18px;
    padding: 18px;
    border-radius: 26px;
  }

  .login-card__intro {
    padding: 2px 0 0;
  }

  .login-card__intro h1 {
    max-width: none;
  }

  .login-card__meta {
    grid-template-columns: 1fr;
  }
}
</style>
