<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ChatDotRound, Close, Lock, Moon, Sunny, User } from '@element-plus/icons-vue'
import { registerRequest } from '@/services/auth'
import { useAuthStore } from '@/stores/auth'
import { useUiStore } from '@/stores/ui'
import { getDeviceFingerprint, getDeviceName } from '@/utils/device'

type ViewMode = 'accounts' | 'login' | 'register' | 'challenge' | 'recovery-email' | 'recovery-code' | 'recovery-reset'
type StatusTone = 'error' | 'info' | 'success'

const authStore = useAuthStore()
const uiStore = useUiStore()
const router = useRouter()
const route = useRoute()

const deviceFingerprint = getDeviceFingerprint()
const deviceName = getDeviceName()

const viewMode = ref<ViewMode>(route.query.add === '1' || !authStore.hasStoredAccounts ? 'login' : 'accounts')
const statusMessage = ref('')
const statusTone = ref<StatusTone>('info')
const challengeTicket = ref('')
const challengeMaskedEmail = ref('')
const recoveryToken = ref('')
const recoveryAccounts = ref<{ userId: number; username: string; nickname: string; avatarUrl: string | null }[]>([])
const capsLockVisible = ref(false)
const registerLoading = ref(false)

const form = reactive({
  username: '',
  password: '',
  rememberMe: true,
  trustDevice: false,
})

const registerForm = reactive({
  username: '',
  nickname: '',
  password: '',
  confirmPassword: '',
})

const challengeForm = reactive({
  code: '',
})

const recoveryForm = reactive({
  email: '',
  code: '',
  newPassword: '',
  confirmPassword: '',
})

const currentTitle = computed(() => {
  if (viewMode.value === 'accounts') return '选择账号'
  if (viewMode.value === 'register') return '注册账号'
  if (viewMode.value === 'challenge') return '邮箱验证'
  if (viewMode.value === 'recovery-email') return '找回账号或密码'
  if (viewMode.value === 'recovery-code') return '输入验证码'
  if (viewMode.value === 'recovery-reset') return '重置密码'
  return '账号登录'
})

function setStatus(message: string, tone: StatusTone = 'info') {
  statusMessage.value = message
  statusTone.value = tone
}

function clearStatus() {
  statusMessage.value = ''
}

function openLoginForm(username = '') {
  form.username = username
  form.password = ''
  challengeForm.code = ''
  capsLockVisible.value = false
  viewMode.value = 'login'
  clearStatus()
}

function openRecoveryEmail() {
  recoveryForm.email = ''
  recoveryForm.code = ''
  recoveryForm.newPassword = ''
  recoveryForm.confirmPassword = ''
  recoveryToken.value = ''
  recoveryAccounts.value = []
  viewMode.value = 'recovery-email'
  clearStatus()
}

function openRegisterForm() {
  registerForm.username = ''
  registerForm.nickname = ''
  registerForm.password = ''
  registerForm.confirmPassword = ''
  viewMode.value = 'register'
  clearStatus()
}

function toggleRememberMe() {
  form.rememberMe = !form.rememberMe
  if (!form.rememberMe) {
    form.trustDevice = false
  }
}

function toggleTrustDevice() {
  if (!form.rememberMe) return
  form.trustDevice = !form.trustDevice
}

async function submit() {
  clearStatus()

  try {
    const response = await authStore.login({
      username: form.username.trim(),
      password: form.password,
      rememberMe: form.rememberMe,
      trustDevice: form.rememberMe && form.trustDevice,
      deviceFingerprint,
      deviceName,
    })

    if (response.status === 'challenge_required') {
      challengeTicket.value = response.challengeTicket ?? ''
      challengeMaskedEmail.value = response.maskedEmail ?? ''
      challengeForm.code = ''
      viewMode.value = 'challenge'
      setStatus('验证码已发送', 'info')
      return
    }

    await router.push('/chat')
  } catch (error) {
    setStatus(error instanceof Error ? error.message : '登录失败', 'error')
  }
}

async function verifyChallenge() {
  clearStatus()

  try {
    await authStore.verifyLoginChallenge({
      challengeTicket: challengeTicket.value,
      code: challengeForm.code.trim(),
      rememberMe: form.rememberMe,
      deviceFingerprint,
    })
    await router.push('/chat')
  } catch (error) {
    setStatus(error instanceof Error ? error.message : '验证失败', 'error')
  }
}

async function resendChallenge() {
  clearStatus()

  try {
    const result = await authStore.resendLoginChallenge(challengeTicket.value)
    challengeMaskedEmail.value = result.maskedEmail
    setStatus(`验证码已重新发送`, 'info')
  } catch (error) {
    setStatus(error instanceof Error ? error.message : '发送失败', 'error')
  }
}

async function activateStoredAccount(userId: number) {
  clearStatus()
  const account = authStore.storedAccounts.find((item) => item.userInfo.userId === userId) ?? null

  try {
    await authStore.activateStoredAccount(userId)
    await router.push('/chat')
  } catch (error) {
    openLoginForm(account?.userInfo.username ?? '')
    setStatus(error instanceof Error ? error.message : '该账号需要重新登录', 'error')
  }
}

function removeStoredAccount(userId: number) {
  authStore.removeStoredAccount(userId)
  if (!authStore.storedAccounts.length) {
    openLoginForm()
  }
}

async function submitRecoveryEmail() {
  clearStatus()

  try {
    await authStore.sendRecoveryCode(recoveryForm.email.trim())
    viewMode.value = 'recovery-code'
    setStatus('验证码已发送', 'info')
  } catch (error) {
    setStatus(error instanceof Error ? error.message : '发送失败', 'error')
  }
}

async function submitRegister() {
  clearStatus()

  if (registerForm.username.trim().length < 3) {
    setStatus('用户名至少需要 3 位', 'error')
    return
  }

  if (registerForm.nickname.trim().length < 2) {
    setStatus('昵称至少需要 2 位', 'error')
    return
  }

  if (registerForm.password.trim().length < 6) {
    setStatus('密码至少需要 6 位', 'error')
    return
  }

  if (registerForm.password !== registerForm.confirmPassword) {
    setStatus('两次输入的密码不一致', 'error')
    return
  }

  registerLoading.value = true

  try {
    await registerRequest({
      username: registerForm.username.trim(),
      nickname: registerForm.nickname.trim(),
      password: registerForm.password,
    })
    openLoginForm(registerForm.username.trim())
    setStatus('注册成功，请登录', 'success')
  } catch (error) {
    setStatus(error instanceof Error ? error.message : '注册失败', 'error')
  } finally {
    registerLoading.value = false
  }
}

async function verifyRecoveryCode() {
  clearStatus()

  try {
    const result = await authStore.verifyRecoveryCode(recoveryForm.email.trim(), recoveryForm.code.trim())
    recoveryToken.value = result.recoveryToken
    recoveryAccounts.value = result.accounts
    viewMode.value = 'recovery-reset'
  } catch (error) {
    setStatus(error instanceof Error ? error.message : '验证失败', 'error')
  }
}

async function submitRecoveryPassword() {
  clearStatus()

  if (recoveryForm.newPassword.trim().length < 6) {
    setStatus('新密码至少需要 6 位', 'error')
    return
  }

  if (recoveryForm.newPassword !== recoveryForm.confirmPassword) {
    setStatus('两次输入的密码不一致', 'error')
    return
  }

  try {
    await authStore.resetRecoveryPassword(recoveryToken.value, recoveryForm.newPassword.trim())
    openLoginForm(recoveryAccounts.value[0]?.username ?? '')
    setStatus('密码已重置', 'success')
  } catch (error) {
    setStatus(error instanceof Error ? error.message : '重置失败', 'error')
  }
}

function handlePasswordEvent(event: Event) {
  if ('getModifierState' in event && typeof event.getModifierState === 'function') {
    capsLockVisible.value = event.getModifierState('CapsLock')
    return
  }

  capsLockVisible.value = false
}
</script>

<template>
  <main class="login-page">
    <section class="login-card">
      <h1 class="login-card__sr-only">EchoIM {{ currentTitle }}</h1>
      <div class="login-card__topbar">
        <div class="login-card__logo-wrap">
          <div class="login-card__logo">
            <ChatDotRound class="login-card__logo-icon" />
          </div>
          <div class="login-card__brand">
            <strong>EchoIM</strong>
            <span>{{ currentTitle }}</span>
          </div>
        </div>
        <button class="theme-toggle" type="button" aria-label="切换主题" @click="uiStore.toggleTheme">
          <Sunny v-if="uiStore.theme === 'light'" />
          <Moon v-else />
        </button>
      </div>

      <p v-if="viewMode === 'login'" class="login-card__intro">
        更轻、更专注的团队聊天空间。
      </p>

      <div v-if="viewMode === 'accounts'" class="login-card__accounts">
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

      <el-form v-else-if="viewMode === 'login'" class="login-card__form" @submit.prevent="submit">
        <el-form-item>
          <el-input
            v-model="form.username"
            :prefix-icon="User"
            placeholder="用户名"
            aria-label="用户名"
          />
        </el-form-item>
        <el-form-item>
          <el-input
            v-model="form.password"
            :prefix-icon="Lock"
            placeholder="密码"
            show-password
            type="password"
            aria-label="密码"
            @keyup="handlePasswordEvent"
            @keydown="handlePasswordEvent"
            @focus="handlePasswordEvent"
            @blur="capsLockVisible = false"
          />
        </el-form-item>

        <p v-if="capsLockVisible" class="login-card__hint">Caps Lock 已开启</p>

        <div class="login-card__checks">
          <button
            class="login-check"
            :class="{ 'is-active': form.rememberMe }"
            type="button"
            @click="toggleRememberMe()"
          >
            <span class="login-check__box" aria-hidden="true">
              <span class="login-check__mark" />
            </span>
            <span class="login-check__copy">记住我（自动续登）</span>
          </button>
          <button
            class="login-check"
            :class="{ 'is-active': form.trustDevice, 'is-disabled': !form.rememberMe }"
            type="button"
            :disabled="!form.rememberMe"
            @click="toggleTrustDevice()"
          >
            <span class="login-check__box" aria-hidden="true">
              <span class="login-check__mark" />
            </span>
            <span class="login-check__copy">信任此设备 30 天</span>
          </button>
        </div>

        <el-button class="login-card__submit" native-type="submit" type="primary" :loading="authStore.isLoading">
          登录
        </el-button>
      </el-form>

      <el-form v-else-if="viewMode === 'register'" class="login-card__form" @submit.prevent="submitRegister">
        <el-form-item>
          <el-input
            v-model="registerForm.username"
            :prefix-icon="User"
            placeholder="用户名"
            aria-label="注册用户名"
          />
        </el-form-item>
        <el-form-item>
          <el-input
            v-model="registerForm.nickname"
            placeholder="昵称"
            aria-label="注册昵称"
          />
        </el-form-item>
        <el-form-item>
          <el-input
            v-model="registerForm.password"
            :prefix-icon="Lock"
            placeholder="密码"
            show-password
            type="password"
            aria-label="注册密码"
          />
        </el-form-item>
        <el-form-item>
          <el-input
            v-model="registerForm.confirmPassword"
            :prefix-icon="Lock"
            placeholder="确认密码"
            show-password
            type="password"
            aria-label="确认注册密码"
          />
        </el-form-item>
        <el-button class="login-card__submit" native-type="submit" type="primary" :loading="registerLoading">
          注册
        </el-button>
      </el-form>

      <el-form v-else-if="viewMode === 'challenge'" class="login-card__form" @submit.prevent="verifyChallenge">
        <el-form-item>
          <el-input
            v-model="challengeForm.code"
            placeholder="6 位验证码"
            aria-label="邮箱验证码"
          />
        </el-form-item>
        <p class="login-card__hint">{{ challengeMaskedEmail }}</p>
        <el-button class="login-card__submit" native-type="submit" type="primary" :loading="authStore.isLoading">
          验证并登录
        </el-button>
      </el-form>

      <el-form v-else-if="viewMode === 'recovery-email'" class="login-card__form" @submit.prevent="submitRecoveryEmail">
        <el-form-item>
          <el-input
            v-model="recoveryForm.email"
            placeholder="邮箱"
            aria-label="恢复邮箱"
          />
        </el-form-item>
        <el-button class="login-card__submit" native-type="submit" type="primary" :loading="authStore.isLoading">
          发送验证码
        </el-button>
      </el-form>

      <el-form v-else-if="viewMode === 'recovery-code'" class="login-card__form" @submit.prevent="verifyRecoveryCode">
        <el-form-item>
          <el-input
            v-model="recoveryForm.code"
            placeholder="6 位验证码"
            aria-label="恢复验证码"
          />
        </el-form-item>
        <el-button class="login-card__submit" native-type="submit" type="primary" :loading="authStore.isLoading">
          下一步
        </el-button>
      </el-form>

      <el-form v-else class="login-card__form" @submit.prevent="submitRecoveryPassword">
        <div v-if="recoveryAccounts[0]" class="login-card__account-preview">
          <strong>{{ recoveryAccounts[0].nickname }}</strong>
          <span>@{{ recoveryAccounts[0].username }}</span>
        </div>
        <el-form-item>
          <el-input
            v-model="recoveryForm.newPassword"
            placeholder="新密码"
            show-password
            type="password"
            aria-label="新密码"
          />
        </el-form-item>
        <el-form-item>
          <el-input
            v-model="recoveryForm.confirmPassword"
            placeholder="确认新密码"
            show-password
            type="password"
            aria-label="确认新密码"
          />
        </el-form-item>
        <el-button class="login-card__submit" native-type="submit" type="primary" :loading="authStore.isLoading">
          重置密码
        </el-button>
      </el-form>

      <div class="login-card__links">
        <button
          v-if="viewMode === 'accounts'"
          class="login-card__link"
          type="button"
          @click="openLoginForm()"
        >
          使用其他账号
        </button>
        <button
          v-if="viewMode === 'login'"
          class="login-card__link"
          type="button"
          @click="openRecoveryEmail()"
        >
          找回账号或密码
        </button>
        <button
          v-if="viewMode === 'accounts' || viewMode === 'login'"
          class="login-card__link"
          type="button"
          @click="openRegisterForm()"
        >
          注册
        </button>
        <button
          v-if="viewMode === 'register'"
          class="login-card__link"
          type="button"
          @click="openLoginForm(registerForm.username)"
        >
          已有账号，去登录
        </button>
        <button
          v-if="viewMode === 'challenge'"
          class="login-card__link"
          type="button"
          @click="openLoginForm(form.username)"
        >
          改用密码登录
        </button>
        <button
          v-if="viewMode === 'challenge'"
          class="login-card__link"
          type="button"
          @click="resendChallenge()"
        >
          重新发送验证码
        </button>
        <button
          v-if="viewMode === 'recovery-code'"
          class="login-card__link"
          type="button"
          @click="submitRecoveryEmail()"
        >
          重新发送验证码
        </button>
        <button
          v-if="viewMode === 'recovery-email'"
          class="login-card__link"
          type="button"
          @click="openLoginForm(form.username)"
        >
          回到登录
        </button>
        <button
          v-if="viewMode === 'recovery-code'"
          class="login-card__link"
          type="button"
          @click="openRecoveryEmail()"
        >
          重新输入邮箱
        </button>
        <button
          v-if="viewMode === 'recovery-reset'"
          class="login-card__link"
          type="button"
          @click="openLoginForm(recoveryAccounts[0]?.username ?? '')"
        >
          回到登录
        </button>
      </div>

      <p v-if="statusMessage" class="login-card__status" :class="`is-${statusTone}`" role="alert" aria-live="polite">
        {{ statusMessage }}
      </p>
    </section>
  </main>
</template>

<style scoped>
.login-page {
  min-height: 100dvh;
  display: grid;
  place-items: center;
  padding: var(--space-5);
}

.login-card__sr-only {
  position: absolute;
  width: 1px;
  height: 1px;
  padding: 0;
  margin: -1px;
  overflow: hidden;
  clip: rect(0, 0, 0, 0);
  white-space: nowrap;
  border: 0;
}

.login-card {
  position: relative;
  width: min(100%, 436px);
  display: flex;
  flex-direction: column;
  gap: 18px;
  padding: 30px;
  border: 1px solid var(--border-default);
  border-radius: var(--radius-card);
  background:
    linear-gradient(180deg, color-mix(in srgb, var(--surface-overlay) 96%, transparent), transparent 120%),
    color-mix(in srgb, var(--surface-overlay) 98%, transparent);
  box-shadow: var(--shadow-md);
  backdrop-filter: blur(16px);
}

.login-card::before {
  content: '';
  position: absolute;
  inset: 0;
  border-radius: inherit;
  border: 1px solid color-mix(in srgb, white 24%, transparent);
  pointer-events: none;
  opacity: 0.4;
}

.login-card__topbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
}

.login-card__logo-wrap {
  display: flex;
  align-items: center;
  gap: 14px;
  min-width: 0;
}

.login-card__logo {
  width: 48px;
  height: 48px;
  display: grid;
  place-items: center;
  border-radius: var(--radius-md);
  background: var(--surface-panel);
  border: 1px solid var(--border-default);
}

.login-card__logo-icon {
  width: 22px;
  height: 22px;
  color: var(--text-tertiary);
}

.login-card__brand {
  min-width: 0;
}

.login-card__brand strong,
.login-card__brand span {
  display: block;
}

.login-card__brand strong {
  font: 620 1.42rem/0.96 var(--font-display);
  letter-spacing: -0.04em;
}

.login-card__brand span {
  margin-top: 5px;
  color: var(--text-tertiary);
  font-size: 0.76rem;
  font-weight: 600;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.login-card__intro {
  margin-top: -2px;
  color: var(--text-secondary);
  font-size: 0.83rem;
  line-height: 1.58;
  max-width: 24rem;
}

.theme-toggle {
  width: 36px;
  height: 36px;
  display: grid;
  place-items: center;
  border: 1px solid var(--border-default);
  border-radius: var(--radius-control);
  background: var(--interactive-secondary-bg);
  color: var(--text-secondary);
  transition:
    background var(--motion-fast) var(--motion-ease-out),
    border-color var(--motion-fast) var(--motion-ease-out),
    color var(--motion-fast) var(--motion-ease-out);
}

.theme-toggle:hover,
.theme-toggle:focus-visible {
  background: var(--interactive-secondary-bg-hover);
  color: var(--text-primary);
  border-color: var(--border-strong);
}

.login-card__accounts {
  display: grid;
  gap: 9px;
}

.login-account {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 28px;
  gap: 10px;
  align-items: center;
  padding: 14px 15px;
  border: 1px solid var(--border-default);
  border-radius: var(--radius-md);
  background: color-mix(in srgb, var(--surface-panel) 94%, transparent);
  text-align: left;
  transition:
    border-color var(--motion-fast) var(--motion-ease-out),
    background var(--motion-fast) var(--motion-ease-out);
}

.login-account:hover,
.login-account:focus-visible {
  border-color: var(--border-strong);
  background: color-mix(in srgb, var(--interactive-secondary-bg-hover) 94%, transparent);
}

.login-account__copy strong {
  display: block;
  font-size: 0.89rem;
  font-weight: 600;
  line-height: 1.24;
}

.login-account__copy span {
  display: block;
  margin-top: 4px;
  color: var(--text-quaternary);
  font: 500 0.68rem/1 var(--font-mono);
  letter-spacing: 0.06em;
}

.login-account__remove {
  width: 28px;
  height: 28px;
  border: 0;
  border-radius: var(--radius-control);
  background: transparent;
  color: var(--text-quaternary);
}

.login-card__form {
  display: grid;
  gap: 0;
}

.login-card__hint {
  margin-top: -2px;
  color: var(--text-secondary);
  font-size: 0.78rem;
  line-height: 1.46;
}

.login-card__checks {
  display: grid;
  gap: 8px;
  margin: 8px 0 12px;
}

.login-card__submit {
  width: 100%;
  min-height: 48px;
  border-radius: var(--radius-control);
  letter-spacing: -0.01em;
}

.login-card__account-preview {
  padding: 13px 14px;
  border: 1px solid var(--border-default);
  border-radius: var(--radius-md);
  background: color-mix(in srgb, var(--surface-panel) 94%, transparent);
}

.login-card__account-preview strong,
.login-card__account-preview span {
  display: block;
}

.login-card__account-preview span {
  margin-top: 4px;
  color: var(--text-quaternary);
  font: 500 0.68rem/1 var(--font-mono);
  letter-spacing: 0.06em;
}

.login-card__links {
  display: flex;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 10px 14px;
  padding-top: 2px;
}

.login-card__link {
  border: 0;
  padding: 0;
  background: transparent;
  color: var(--text-secondary);
  font-size: 0.8rem;
  font-weight: 500;
  line-height: 1.4;
  transition: color var(--motion-fast) var(--motion-ease-out);
}

.login-card__link:hover,
.login-card__link:focus-visible {
  color: var(--text-primary);
}

.login-check {
  min-height: 40px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  padding: 0 4px 0 2px;
  border: 0;
  border-radius: 0;
  background: transparent;
  color: var(--text-secondary);
  text-align: left;
  transition:
    border-color var(--motion-fast) var(--motion-ease-out),
    background-color var(--motion-fast) var(--motion-ease-out),
    color var(--motion-fast) var(--motion-ease-out),
    transform var(--motion-fast) var(--motion-ease-out);
}

.login-check:hover,
.login-check:focus-visible {
  color: var(--text-primary);
}

.login-check.is-active {
  color: var(--text-primary);
}

.login-check.is-disabled {
  opacity: 0.52;
}

.login-check__box {
  width: 16px;
  height: 16px;
  flex: 0 0 16px;
  display: grid;
  place-items: center;
  border: 1px solid color-mix(in srgb, var(--border-strong) 88%, transparent);
  border-radius: 4px;
  background: color-mix(in srgb, var(--surface-panel) 92%, transparent);
  transition:
    border-color var(--motion-fast) var(--motion-ease-out),
    background-color var(--motion-fast) var(--motion-ease-out);
  order: 2;
}

.login-check.is-active .login-check__box {
  border-color: color-mix(in srgb, var(--interactive-primary-bg) 48%, transparent);
  background: var(--interactive-primary-bg);
}

.login-check__mark {
  width: 7px;
  height: 3px;
  border-left: 1.6px solid transparent;
  border-bottom: 1.6px solid transparent;
  transform: rotate(-45deg) translateY(-1px);
  opacity: 0;
  transition: opacity var(--motion-fast) var(--motion-ease-out);
}

.login-check.is-active .login-check__mark {
  border-color: white;
  opacity: 1;
}

.login-check__copy {
  font-size: 0.79rem;
  line-height: 1.24;
  flex: 1;
}

.login-card__status {
  padding: 10px 12px;
  border: 1px solid var(--border-default);
  border-radius: var(--radius-control);
  font-size: 0.79rem;
  line-height: 1.5;
}

.login-card__status.is-error {
  border-color: color-mix(in srgb, var(--status-danger) 16%, var(--border-default));
  background: color-mix(in srgb, var(--status-danger) 8%, var(--surface-panel));
  color: var(--status-danger);
}

.login-card__status.is-info {
  border-color: color-mix(in srgb, var(--interactive-primary-bg) 16%, var(--border-default));
  background: color-mix(in srgb, var(--interactive-selected-bg) 84%, var(--surface-panel));
  color: var(--text-primary);
}

.login-card__status.is-success {
  border-color: color-mix(in srgb, var(--status-success) 16%, var(--border-default));
  background: color-mix(in srgb, var(--status-success) 8%, var(--surface-panel));
  color: color-mix(in srgb, var(--status-success) 84%, var(--text-primary));
}

:deep(.login-card__form .el-form-item) {
  margin-bottom: 11px;
}

:deep(.login-card__form .el-input__wrapper) {
  min-height: 46px;
  border-radius: var(--radius-control);
  padding: 0 14px;
  background: color-mix(in srgb, var(--surface-panel) 94%, transparent);
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.16),
    0 0 0 1px color-mix(in srgb, var(--border-default) 84%, transparent);
}

:deep(.login-card__form .el-input__wrapper.is-focus) {
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.18),
    0 0 0 1px color-mix(in srgb, var(--interactive-primary-bg) 34%, transparent),
    0 0 0 4px color-mix(in srgb, var(--interactive-focus-ring) 32%, transparent);
}
</style>
