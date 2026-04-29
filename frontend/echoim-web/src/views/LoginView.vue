<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ChatDotRound, Close, Lock, User } from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'
import { useUiStore } from '@/stores/ui'

const authStore = useAuthStore()
const uiStore = useUiStore()
const router = useRouter()
const route = useRoute()

const form = reactive({
  username: 'echo_demo_01',
  password: '123456',
})
const loginError = ref<string | null>(null)
const showAddAccountForm = computed(() => route.query.add === '1' || !authStore.hasStoredAccounts)

async function submit() {
  loginError.value = null

  try {
    await authStore.login(form.username, form.password)
    router.push('/chat')
  } catch (error) {
    loginError.value = error instanceof Error ? error.message : '登录失败'
  }
}

async function activateStoredAccount(userId: number) {
  loginError.value = null
  authStore.activateStoredAccount(userId)
  await router.push('/chat')
}

function removeStoredAccount(userId: number) {
  authStore.removeStoredAccount(userId)
}
</script>

<template>
  <main class="login-page">
    <div class="login-shell">
      <header class="login-shell__header">
        <div class="brand-line">
          <ChatDotRound class="brand-line__icon" />
          <strong>EchoIM</strong>
          <span>Quiet premium messaging</span>
        </div>
        <button class="theme-toggle" type="button" @click="uiStore.toggleTheme">
          {{ uiStore.theme === 'light' ? '深色' : '浅色' }}
        </button>
      </header>

      <section class="login-card">
        <div class="login-card__intro">
          <span class="login-card__eyebrow">WELCOME BACK</span>
          <h1>安静一点，也可以把沟通做得更高级。</h1>
          <p>EchoIM 让会话、联系人和个人工作台落在同一块安静的界面里。演示账号已预填，登录后可以直接进入完整三栏聊天体验。</p>
          <div class="login-card__highlights">
            <article>
              <strong>Workstation layout</strong>
              <p>会话、主聊天区和资料侧轨保持同一节奏，信息密度更稳。</p>
            </article>
            <article>
              <strong>Quiet presence</strong>
              <p>减少噪音色和彩色按钮，让未读、提醒和重点内容更容易被看见。</p>
            </article>
            <article>
              <strong>Light and dark</strong>
              <p>浅色和深色都单独校准，长时间聊天阅读不会刺眼，也不会发灰。</p>
            </article>
          </div>
        </div>

        <div class="login-card__section">
          <div class="login-card__section-head">
            <span>{{ authStore.hasStoredAccounts ? '身份切换' : '账号登录' }}</span>
            <strong>{{ showAddAccountForm ? 'Secure entry' : 'Quick return' }}</strong>
          </div>
          <div v-if="authStore.storedAccounts.length" class="login-card__accounts">
            <article
              v-for="account in authStore.storedAccounts"
              :key="account.userInfo.userId"
              class="login-account"
              role="button"
              tabindex="0"
              @click="activateStoredAccount(account.userInfo.userId)"
              @keydown.enter.prevent="activateStoredAccount(account.userInfo.userId)"
            >
              <div class="login-account__copy">
                <strong>{{ account.userInfo.nickname }}</strong>
                <span>@{{ account.userInfo.username }}</span>
              </div>
              <button class="login-account__remove" type="button" aria-label="移除账号" @click.stop="removeStoredAccount(account.userInfo.userId)">
                <Close />
              </button>
            </article>
          </div>
          <div v-if="authStore.hasStoredAccounts && !showAddAccountForm" class="login-card__switch-hint">
            <p>已保存的账号会以身份卡片展示。可以直接返回，也可以继续添加新的登录身份。</p>
            <el-button plain @click="router.replace('/login?add=1')">添加其他账号</el-button>
          </div>
          <el-form v-if="showAddAccountForm" label-position="top" @submit.prevent="submit">
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

          <div v-if="showAddAccountForm" class="login-card__meta">
            <div>
              <span class="login-card__meta-label">Demo account</span>
              <strong>echo_demo_01</strong>
            </div>
            <div>
              <span class="login-card__meta-label">Demo password</span>
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
  padding: 40px 24px;
}

.login-shell {
  width: min(100%, 980px);
}

.login-shell__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
}

.brand-line {
  display: inline-flex;
  align-items: center;
  gap: 11px;
  padding: 12px 18px;
  border: 1px solid var(--color-shell-border);
  border-radius: 999px;
  background: color-mix(in srgb, var(--color-shell-toolbar) 92%, transparent);
  box-shadow: var(--shadow-card);
  font-size: 0.82rem;
  backdrop-filter: blur(18px);
}

.brand-line strong {
  font: var(--font-title-sm);
}

.brand-line span {
  color: var(--color-text-3);
  font: 500 0.76rem/1 var(--font-mono);
  letter-spacing: 0.04em;
  text-transform: uppercase;
}

.brand-line__icon {
  width: 16px;
  height: 16px;
  color: var(--color-shell-eyebrow);
}

.login-card {
  position: relative;
  display: grid;
  grid-template-columns: minmax(0, 1.18fr) minmax(330px, 0.82fr);
  gap: 24px;
  overflow: hidden;
  border: 1px solid var(--color-shell-border);
  border-radius: 36px;
  background:
    radial-gradient(circle at bottom right, color-mix(in srgb, var(--color-shell-glow) 72%, transparent), transparent 28%),
    radial-gradient(circle at top left, color-mix(in srgb, var(--color-primary) 8%, transparent), transparent 24%),
    var(--color-shell-panel);
  box-shadow:
    var(--shadow-panel),
    var(--shadow-inset-soft);
  padding: 28px;
  backdrop-filter: blur(24px);
}

.theme-toggle {
  min-height: 40px;
  padding: 0 16px;
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
  gap: 22px;
  padding: 12px 8px 12px 6px;
}

.login-card__eyebrow {
  display: inline-block;
  color: var(--color-shell-eyebrow);
  font: var(--font-eyebrow);
  letter-spacing: 0.14em;
}

.login-card__intro h1 {
  max-width: 12ch;
  font: var(--font-title-lg);
  letter-spacing: -0.06em;
  text-wrap: balance;
}

.login-card__intro p,
.login-card__meta {
  color: var(--color-text-2);
  font-size: 0.94rem;
  line-height: 1.68;
}

.login-card__highlights {
  display: grid;
  gap: 12px;
}

.login-card__highlights article {
  padding: 16px 18px;
  border: 1px solid var(--color-shell-border);
  border-radius: 22px;
  background: color-mix(in srgb, var(--color-shell-card-muted) 94%, transparent);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.28);
}

.login-card__highlights strong {
  display: block;
  margin-bottom: 6px;
  font: var(--font-title-sm);
}

.login-card__highlights p {
  font-size: 0.84rem;
  line-height: 1.6;
}

.login-card__section {
  padding: 22px;
  border: 1px solid var(--color-shell-border);
  border-radius: 28px;
  background:
    linear-gradient(180deg, color-mix(in srgb, rgba(255, 255, 255, 0.3) 40%, transparent), transparent 22%),
    var(--color-shell-card-strong);
  box-shadow: var(--shadow-inset-soft);
  backdrop-filter: blur(18px);
}

.login-card__accounts {
  display: grid;
  gap: 12px;
  margin-bottom: 18px;
}

.login-account {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 28px;
  align-items: center;
  gap: 10px;
  padding: 16px 18px;
  border: 1px solid var(--color-shell-border);
  border-radius: 22px;
  background: color-mix(in srgb, var(--color-shell-card-muted) 94%, transparent);
  text-align: left;
  transition:
    transform var(--motion-fast) ease,
    background var(--motion-fast) ease,
    border-color var(--motion-fast) ease,
    box-shadow var(--motion-fast) ease;
}

.login-account:hover,
.login-account:focus-visible {
  transform: translateY(-1px);
  border-color: var(--color-shell-border-strong);
  background: color-mix(in srgb, var(--color-shell-action-hover) 94%, transparent);
  box-shadow: var(--shadow-card);
}

.login-account__copy strong {
  display: block;
  font: var(--font-title-sm);
}

.login-account__copy span {
  display: block;
  margin-top: 4px;
  color: var(--color-text-soft);
  font: 500 0.72rem/1 var(--font-mono);
}

.login-account__remove {
  width: 28px;
  height: 28px;
  border: 0;
  border-radius: 12px;
  background: transparent;
  color: var(--color-text-soft);
}

.login-card__switch-hint {
  display: grid;
  gap: 12px;
  margin-bottom: 18px;
  color: var(--color-text-2);
  font-size: 0.88rem;
  line-height: 1.55;
}

.login-card__section-head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 10px;
  margin-bottom: 18px;
}

.login-card__section-head span {
  color: var(--color-text-1);
  font: var(--font-title-sm);
}

.login-card__section-head strong {
  color: var(--color-text-soft);
  font: 600 0.72rem/1 var(--font-mono);
  letter-spacing: 0.1em;
  text-transform: uppercase;
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
  padding: 16px 16px;
  border: 1px solid var(--color-shell-border);
  border-radius: 20px;
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
    border-radius: 28px;
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
