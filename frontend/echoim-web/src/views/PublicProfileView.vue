<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import AvatarBadge from '@/components/chat/AvatarBadge.vue'
import { useAuthStore } from '@/stores/auth'
import { useChatStore } from '@/stores/chat'
import { createFriendRequest } from '@/services/friends'
import { fetchPublicUserProfileByUsername, fetchUserPublicProfileByUsername } from '@/services/user'
import type { ApiUserPublicProfile } from '@/types/api'
import type { PublicUserProfile } from '@/types/chat'
import { buildPublicProfileUrl } from '@/utils/publicProfiles'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const chatStore = useChatStore()

const loading = ref(false)
const actionLoading = ref(false)
const errorMessage = ref<string | null>(null)
const noticeMessage = ref<string | null>(null)
const publicProfile = ref<PublicUserProfile | null>(null)
const privateProfile = ref<ApiUserPublicProfile | null>(null)

const username = computed(() => String(route.params.username ?? '').trim())
const isSelf = computed(() => Boolean(privateProfile.value && authStore.currentUser?.userId === privateProfile.value.userId))
const relationLabel = computed(() => {
  switch (privateProfile.value?.friendStatus) {
    case 'FRIEND':
      return '已是好友'
    case 'PENDING_OUT':
    case 'PENDING_IN':
      return '申请处理中'
    case 'BLOCKED_OUT':
      return '你已拉黑对方'
    case 'BLOCKED_IN':
      return '对方已拉黑你'
    default:
      return null
  }
})

async function loadProfile() {
  if (!username.value) {
    errorMessage.value = '用户名不能为空'
    return
  }

  loading.value = true
  errorMessage.value = null
  noticeMessage.value = null
  privateProfile.value = null

  try {
    publicProfile.value = await fetchPublicUserProfileByUsername(username.value)
    if (authStore.isAuthenticated) {
      try {
        privateProfile.value = await fetchUserPublicProfileByUsername(username.value)
      } catch {
        privateProfile.value = null
      }
    }
  } catch (error) {
    publicProfile.value = null
    privateProfile.value = null
    errorMessage.value = error instanceof Error ? error.message : '公开资料加载失败'
  } finally {
    loading.value = false
  }
}

async function handleStartChat() {
  if (!privateProfile.value || isSelf.value) return
  actionLoading.value = true
  noticeMessage.value = null
  try {
    const conversation = await chatStore.createSingleConversation(privateProfile.value.userId)
    await router.push({ name: 'chat-home', params: { conversationId: conversation.conversationId } })
  } catch (error) {
    noticeMessage.value = error instanceof Error ? error.message : '发起聊天失败'
  } finally {
    actionLoading.value = false
  }
}

async function handleAddFriend() {
  if (!privateProfile.value || isSelf.value) return
  actionLoading.value = true
  noticeMessage.value = null
  try {
    await createFriendRequest(privateProfile.value.userId, '你好，希望加个好友。')
    privateProfile.value = {
      ...privateProfile.value,
      friendStatus: 'PENDING_OUT',
      pendingRequestId: privateProfile.value.pendingRequestId ?? -1,
    }
    noticeMessage.value = '好友申请已发送'
  } catch (error) {
    noticeMessage.value = error instanceof Error ? error.message : '好友申请发送失败'
  } finally {
    actionLoading.value = false
  }
}

async function copyPublicLink() {
  try {
    await navigator.clipboard.writeText(buildPublicProfileUrl(username.value))
    noticeMessage.value = '公开链接已复制'
  } catch {
    noticeMessage.value = '公开链接复制失败'
  }
}

function openLogin() {
  router.push({ name: 'login' }).catch(() => undefined)
}

watch(username, () => {
  void loadProfile()
}, { immediate: true })

onMounted(() => {
  document.documentElement.scrollTop = 0
})
</script>

<template>
  <main class="public-profile">
    <section class="public-profile__shell">
      <div class="public-profile__hero">
        <AvatarBadge
          :name="publicProfile?.nickname || username"
          :avatar-url="publicProfile?.avatarUrl"
          size="lg"
        />
        <div class="public-profile__copy">
          <span class="public-profile__eyebrow">EchoIM Public Profile</span>
          <strong>{{ publicProfile?.nickname || '公开主页' }}</strong>
          <p>@{{ publicProfile?.username || username }}</p>
        </div>
      </div>

      <div v-if="loading" class="public-profile__state">正在加载公开资料…</div>
      <div v-else-if="errorMessage" class="public-profile__state is-error">{{ errorMessage }}</div>
      <template v-else-if="publicProfile">
        <section class="public-profile__card">
          <div class="public-profile__card-head">
            <strong>公开资料</strong>
            <button class="public-profile__ghost" type="button" @click="copyPublicLink">复制链接</button>
          </div>
          <dl class="public-profile__grid">
            <div>
              <dt>用户名</dt>
              <dd>@{{ publicProfile.username }}</dd>
            </div>
            <div>
              <dt>昵称</dt>
              <dd>{{ publicProfile.nickname }}</dd>
            </div>
          </dl>
          <p class="public-profile__signature">
            {{ publicProfile.signature || '这个用户还没有填写公开签名。' }}
          </p>
        </section>

        <section class="public-profile__card">
          <div class="public-profile__card-head">
            <strong>{{ authStore.isAuthenticated ? '站内操作' : '继续操作' }}</strong>
            <span v-if="relationLabel">{{ relationLabel }}</span>
          </div>
          <div v-if="noticeMessage" class="public-profile__notice">{{ noticeMessage }}</div>
          <div v-if="!authStore.isAuthenticated" class="public-profile__actions">
            <button class="public-profile__primary" type="button" @click="openLogin">登录后发消息或加好友</button>
          </div>
          <div v-else-if="isSelf" class="public-profile__actions">
            <button class="public-profile__primary" type="button" @click="router.push({ name: 'chat-home' })">返回聊天</button>
          </div>
          <div v-else class="public-profile__actions">
            <button
              class="public-profile__primary"
              type="button"
              :disabled="actionLoading || !privateProfile || ['BLOCKED_OUT', 'BLOCKED_IN'].includes(privateProfile.friendStatus || '')"
              @click="handleStartChat"
            >
              发消息
            </button>
            <button
              class="public-profile__secondary"
              type="button"
              :disabled="actionLoading || !privateProfile || ['FRIEND', 'PENDING_OUT', 'PENDING_IN', 'BLOCKED_OUT', 'BLOCKED_IN'].includes(privateProfile.friendStatus || '')"
              @click="handleAddFriend"
            >
              {{ privateProfile?.friendStatus === 'FRIEND' ? '已是好友' : privateProfile?.pendingRequestId ? '处理中' : '加好友' }}
            </button>
          </div>
        </section>
      </template>
    </section>
  </main>
</template>

<style scoped>
.public-profile {
  min-height: 100dvh;
  display: grid;
  align-content: center;
  padding: 32px 16px;
  background:
    radial-gradient(circle at 14% 14%, color-mix(in srgb, var(--interactive-primary-bg) 8%, transparent), transparent 26%),
    radial-gradient(circle at 88% 8%, color-mix(in srgb, var(--interactive-focus-ring) 50%, transparent), transparent 22%),
    radial-gradient(circle at 50% 100%, color-mix(in srgb, var(--surface-subtle) 60%, transparent), transparent 30%),
    linear-gradient(180deg, transparent, color-mix(in srgb, var(--surface-inverse) 8%, transparent)),
    var(--surface-canvas);
}

.public-profile__shell {
  width: min(720px, 100%);
  display: grid;
  gap: 20px;
}

.public-profile__hero,
.public-profile__card {
  border: 1px solid var(--border-default);
  border-radius: var(--radius-card);
  background:
    radial-gradient(circle at top right, color-mix(in srgb, var(--interactive-focus-ring) 20%, transparent), transparent 34%),
    var(--surface-card);
}

.public-profile__hero {
  display: flex;
  align-items: center;
  gap: 20px;
  padding: 28px;
  box-shadow: var(--shadow-md);
}

.public-profile__copy {
  display: grid;
  gap: 8px;
}

.public-profile__eyebrow {
  color: var(--text-quaternary);
  font: var(--font-eyebrow);
  letter-spacing: 0.12em;
  text-transform: uppercase;
}

.public-profile__copy strong {
  color: var(--text-primary);
  font-size: clamp(1.6rem, 3vw, 2.2rem);
  line-height: 0.98;
  letter-spacing: -0.032em;
}

.public-profile__copy p,
.public-profile__card-head span,
.public-profile__signature,
.public-profile__state,
.public-profile__notice {
  color: var(--text-secondary);
}

.public-profile__card {
  padding: 24px;
  display: grid;
  gap: 18px;
  background: var(--surface-raised);
  box-shadow: var(--shadow-sm);
}

.public-profile__card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.public-profile__card-head strong {
  color: var(--text-primary);
  font: 600 var(--text-lg)/1.08 var(--font-body);
  letter-spacing: -0.02em;
}

.public-profile__grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.public-profile__grid div {
  padding: 14px;
  border-radius: var(--radius-panel);
  background: var(--surface-panel);
  border: 1px solid var(--border-subtle);
}

.public-profile__grid dt {
  font-size: var(--text-sm);
  color: var(--text-quaternary);
}

.public-profile__grid dd {
  margin: 6px 0 0;
  color: var(--text-primary);
  font-weight: 600;
}

.public-profile__signature {
  margin: 0;
  line-height: 1.7;
}

.public-profile__actions {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.public-profile__primary,
.public-profile__secondary,
.public-profile__ghost {
  border-radius: var(--radius-pill);
  min-height: var(--btn-min-size);
  padding: 0 18px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border: 1px solid var(--border-default);
  background: var(--interactive-secondary-bg);
  color: var(--interactive-secondary-fg);
  transition:
    border-color var(--motion-fast) var(--motion-ease-out),
    background var(--motion-fast) var(--motion-ease-out),
    color var(--motion-fast) var(--motion-ease-out),
    box-shadow var(--motion-fast) var(--motion-ease-out);
}

.public-profile__primary {
  min-width: 120px;
  background: var(--interactive-primary-bg);
  border-color: transparent;
  color: var(--interactive-primary-fg);
  box-shadow: 0 12px 28px color-mix(in srgb, var(--interactive-primary-bg) 18%, transparent);
}

.public-profile__primary:hover,
.public-profile__primary:focus-visible {
  background: var(--interactive-primary-bg-hover);
}

.public-profile__secondary:hover,
.public-profile__secondary:focus-visible,
.public-profile__ghost:hover,
.public-profile__ghost:focus-visible {
  border-color: var(--border-strong);
  background: var(--interactive-secondary-bg-hover);
}

.public-profile__notice {
  padding: 12px 14px;
  border-radius: var(--radius-md);
  border: 1px solid var(--border-subtle);
  background: color-mix(in srgb, var(--surface-panel) 92%, transparent);
}

.public-profile__state {
  padding: 24px;
  border-radius: var(--radius-card);
  background: var(--surface-card);
  border: 1px solid var(--border-default);
  box-shadow: var(--shadow-sm);
}

.public-profile__state.is-error {
  color: var(--status-danger);
}

@media (max-width: 640px) {
  .public-profile__hero {
    align-items: flex-start;
    flex-direction: column;
  }

  .public-profile__grid {
    grid-template-columns: 1fr;
  }

  .public-profile__actions {
    flex-direction: column;
  }
}
</style>
