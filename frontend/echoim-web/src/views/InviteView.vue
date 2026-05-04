<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import AvatarBadge from '@/components/chat/AvatarBadge.vue'
import { fetchInvitePreview, joinByInvite } from '@/services/groups'
import { useAuthStore } from '@/stores/auth'
import type { ApiInvitePreview } from '@/types/api'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

const loading = ref(true)
const joining = ref(false)
const error = ref<string | null>(null)
const preview = ref<ApiInvitePreview | null>(null)

const token = route.params.token as string

onMounted(async () => {
  try {
    preview.value = await fetchInvitePreview(token)
  } catch (e: unknown) {
    error.value = e instanceof Error ? e.message : '邀请链接无效或已过期'
  } finally {
    loading.value = false
  }
})

async function handleJoin() {
  if (!authStore.isAuthenticated) {
    router.push({ name: 'login', query: { redirect: route.fullPath } })
    return
  }
  joining.value = true
  try {
    await joinByInvite(token)
    ElMessage.success('已成功加入群聊')
    router.push('/chat')
  } catch (e: unknown) {
    error.value = e instanceof Error ? e.message : '加入失败'
  } finally {
    joining.value = false
  }
}
</script>

<template>
  <div class="invite-page">
    <div class="invite-card">
      <div v-if="loading" class="invite-card__loading">
        <span>加载中...</span>
      </div>

      <template v-else-if="error">
        <div class="invite-card__error">
          <p>{{ error }}</p>
          <button class="invite-card__btn" type="button" @click="router.push('/chat')">返回</button>
        </div>
      </template>

      <template v-else-if="preview">
        <div class="invite-card__hero">
          <AvatarBadge
            :name="preview.groupName"
            :avatar-url="preview.avatarUrl"
            size="xl"
            type="group"
          />
          <h2 class="invite-card__name">{{ preview.groupName }}</h2>
          <p class="invite-card__meta">
            {{ preview.memberCount ?? 0 }} 位成员
            <template v-if="preview.inviterNickname">
              &middot; {{ preview.inviterNickname }} 邀请你加入
            </template>
          </p>
        </div>
        <button
          class="invite-card__btn invite-card__btn--primary"
          type="button"
          :disabled="joining"
          @click="handleJoin"
        >
          {{ joining ? '加入中...' : '加入群聊' }}
        </button>
      </template>
    </div>
  </div>
</template>

<style scoped>
.invite-page {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 100vh;
  padding: 24px;
  background: var(--surface-panel);
}

.invite-card {
  width: 100%;
  max-width: 400px;
  padding: 32px 24px;
  border-radius: var(--radius-lg);
  border: 1px solid var(--border-default);
  background: var(--surface-card);
  box-shadow: var(--shadow-lg);
  text-align: center;
}

.invite-card__loading {
  padding: 40px 0;
  color: var(--text-tertiary);
  font-size: var(--text-sm);
}

.invite-card__error {
  padding: 20px 0;
}

.invite-card__error p {
  margin-bottom: 16px;
  color: var(--status-danger);
  font-size: var(--text-sm);
}

.invite-card__hero {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
  margin-bottom: 24px;
}

.invite-card__name {
  margin: 0;
  font: 620 var(--text-lg)/1.2 var(--font-display);
  color: var(--text-primary);
}

.invite-card__meta {
  margin: 0;
  color: var(--text-tertiary);
  font-size: var(--text-xs);
}

.invite-card__btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  height: 40px;
  padding: 0 24px;
  border-radius: var(--radius-control);
  border: 1px solid var(--border-default);
  background: var(--interactive-secondary-bg);
  color: var(--text-secondary);
  font: 500 var(--text-sm)/1 var(--font-sans);
  cursor: pointer;
  transition: background var(--motion-fast) var(--motion-ease-out);
}

.invite-card__btn:hover {
  background: var(--interactive-secondary-bg-hover);
  color: var(--text-primary);
}

.invite-card__btn--primary {
  width: 100%;
  background: var(--interactive-primary-bg);
  border-color: var(--interactive-primary-bg);
  color: var(--text-on-brand);
}

.invite-card__btn--primary:hover:not(:disabled) {
  background: color-mix(in srgb, var(--interactive-primary-bg) 88%, transparent);
}

.invite-card__btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
</style>
