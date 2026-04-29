<script setup lang="ts">
import { computed } from 'vue'
import { ArrowLeft, CirclePlus, Lock, MessageBox, SwitchButton } from '@element-plus/icons-vue'
import type { FriendListItem, FriendRequestItem } from '@/types/chat'
import AvatarBadge from './AvatarBadge.vue'
import ChatStatePanel from './ChatStatePanel.vue'

type ContactsTab = 'friends' | 'requests' | 'blocked'

const props = defineProps<{
  friends: FriendListItem[]
  requests: FriendRequestItem[]
  blocked: FriendListItem[]
  activeTab: ContactsTab
  keyword: string
  loading?: boolean
  errorMessage?: string | null
}>()

const emit = defineEmits<{
  back: []
  'update:activeTab': [value: ContactsTab]
  'update:keyword': [value: string]
  'open-add-contact': []
  'open-chat': [userId: number]
  'update-remark': [friend: FriendListItem]
  'block-friend': [friendId: number]
  'unblock-friend': [friendId: number]
  'delete-friend': [friendId: number]
  'approve-request': [requestId: number]
  'reject-request': [requestId: number]
}>()

const visibleItems = computed(() => {
  const keyword = props.keyword.trim().toLowerCase()
  const source =
    props.activeTab === 'friends'
      ? props.friends
      : props.activeTab === 'blocked'
        ? props.blocked
        : props.requests

  if (!keyword) return source

  return source.filter((item) => {
    if ('requestId' in item) {
      return (
        item.nickname.toLowerCase().includes(keyword) ||
        item.userNo.toLowerCase().includes(keyword) ||
        (item.applyMsg ?? '').toLowerCase().includes(keyword)
      )
    }

    return (
      item.displayName.toLowerCase().includes(keyword) ||
      item.nickname.toLowerCase().includes(keyword) ||
      item.userNo.toLowerCase().includes(keyword)
    )
  })
})

function requestCountBadge(item: FriendRequestItem) {
  return item.direction === 'INBOUND' ? '待处理' : '已发送'
}
</script>

<template>
  <section class="contacts-panel">
    <header class="contacts-panel__hero">
      <button class="contacts-panel__back" type="button" aria-label="返回会话列表" @click="emit('back')">
        <ArrowLeft />
      </button>
      <div class="contacts-panel__copy">
        <span>Relationship desk</span>
        <strong>联系人</strong>
        <p>好友、申请与黑名单都集中在同一条侧轨里处理。</p>
      </div>
      <button class="contacts-panel__add" type="button" aria-label="添加联系人" @click="emit('open-add-contact')">
        <CirclePlus />
      </button>
    </header>

    <div class="contacts-panel__switcher">
      <button
        v-for="tab in [
          { key: 'friends', label: `好友 ${friends.length}` },
          { key: 'requests', label: `申请 ${requests.length}` },
          { key: 'blocked', label: `黑名单 ${blocked.length}` },
        ]"
        :key="tab.key"
        class="contacts-panel__switch"
        :class="{ 'is-active': activeTab === tab.key }"
        type="button"
        @click="emit('update:activeTab', tab.key as ContactsTab)"
      >
        {{ tab.label }}
      </button>
    </div>

    <el-input
      class="contacts-panel__search"
      :model-value="keyword"
      clearable
      placeholder="搜索昵称、备注、申请文案或账号编号"
      @update:model-value="emit('update:keyword', String($event ?? ''))"
    />

    <el-scrollbar class="contacts-panel__scroll">
      <div v-if="loading" class="contacts-panel__skeletons">
        <el-skeleton v-for="item in 6" :key="item" animated :rows="2" />
      </div>
      <ChatStatePanel
        v-else-if="errorMessage"
        compact
        title="联系人加载失败"
        :description="errorMessage"
        role="alert"
      />
      <div v-else-if="visibleItems.length" class="contacts-panel__list">
        <article
          v-for="item in visibleItems"
          :key="'requestId' in item ? `request-${item.requestId}` : `friend-${item.friendUserId}`"
          class="contacts-card"
        >
          <div class="contacts-card__main">
            <AvatarBadge
              :name="'requestId' in item ? item.nickname : item.displayName"
              :avatar-url="item.avatarUrl"
              size="lg"
            />
            <div class="contacts-card__copy">
              <div class="contacts-card__title">
                <strong>{{ 'requestId' in item ? item.nickname : item.displayName }}</strong>
                <span>{{ 'requestId' in item ? requestCountBadge(item) : `#${item.userNo}` }}</span>
              </div>
              <p v-if="'requestId' in item">{{ item.applyMsg || '这条申请没有附言。' }}</p>
              <p v-else>{{ item.remark ? `备注：${item.remark}` : item.nickname }}</p>
            </div>
          </div>

          <div v-if="'requestId' in item" class="contacts-card__actions">
            <button class="contacts-card__action" type="button" @click="emit('open-chat', item.direction === 'INBOUND' ? item.fromUserId : item.toUserId)">
              <MessageBox />
              发起聊天
            </button>
            <button
              v-if="item.direction === 'INBOUND' && item.status === 0"
              class="contacts-card__action is-primary"
              type="button"
              @click="emit('approve-request', item.requestId)"
            >
              通过
            </button>
            <button
              v-if="item.direction === 'INBOUND' && item.status === 0"
              class="contacts-card__action is-danger"
              type="button"
              @click="emit('reject-request', item.requestId)"
            >
              拒绝
            </button>
          </div>

          <div v-else class="contacts-card__actions">
            <button class="contacts-card__action" type="button" @click="emit('open-chat', item.friendUserId)">
              <MessageBox />
              聊天
            </button>
            <button
              v-if="activeTab === 'friends'"
              class="contacts-card__action"
              type="button"
              @click="emit('update-remark', item)"
            >
              <SwitchButton />
              备注
            </button>
            <button
              v-if="activeTab === 'friends'"
              class="contacts-card__action is-danger"
              type="button"
              @click="emit('block-friend', item.friendUserId)"
            >
              <Lock />
              拉黑
            </button>
            <button
              v-if="activeTab === 'friends'"
              class="contacts-card__action is-danger"
              type="button"
              @click="emit('delete-friend', item.friendUserId)"
            >
              删除
            </button>
            <button
              v-if="activeTab === 'blocked'"
              class="contacts-card__action is-primary"
              type="button"
              @click="emit('unblock-friend', item.friendUserId)"
            >
              解除拉黑
            </button>
          </div>
        </article>
      </div>
      <ChatStatePanel
        v-else
        compact
        :title="activeTab === 'friends' ? '还没有联系人' : activeTab === 'requests' ? '还没有申请记录' : '黑名单为空'"
        :description="activeTab === 'friends' ? '从这里开始补齐联系人闭环。' : '切换到别的分组继续操作。'"
      />
    </el-scrollbar>
  </section>
</template>

<style scoped>
.contacts-panel {
  display: grid;
  grid-template-rows: auto auto auto minmax(0, 1fr);
  gap: 12px;
  height: 100%;
  min-height: 0;
  padding: 20px;
  border: 1px solid var(--color-shell-border);
  border-radius: 32px;
  background:
    radial-gradient(circle at 100% 0%, color-mix(in srgb, var(--color-shell-glow) 60%, transparent), transparent 30%),
    linear-gradient(180deg, color-mix(in srgb, var(--color-shell-card) 96%, transparent), var(--color-shell-panel));
  box-shadow: var(--shadow-panel);
  backdrop-filter: blur(24px);
}

.contacts-panel__hero {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr) auto;
  gap: 12px;
  align-items: start;
}

.contacts-panel__copy span {
  display: block;
  color: var(--color-shell-eyebrow);
  font: var(--font-eyebrow);
  letter-spacing: 0.12em;
  text-transform: uppercase;
}

.contacts-panel__copy strong {
  display: block;
  margin-top: 4px;
  font: var(--font-title-md);
}

.contacts-panel__copy p {
  margin-top: 6px;
  color: var(--color-text-2);
  font-size: 0.82rem;
  line-height: 1.5;
}

.contacts-panel__back,
.contacts-panel__add,
.contacts-panel__switch,
.contacts-card__action {
  transition:
    transform var(--motion-fast) ease,
    border-color var(--motion-fast) ease,
    background var(--motion-fast) ease,
    color var(--motion-fast) ease;
}

.contacts-panel__back,
.contacts-panel__switch,
.contacts-card__action {
  border: 1px solid var(--color-shell-border);
  background: var(--color-shell-action);
  color: var(--color-text-1);
}

.contacts-panel__back {
  width: 44px;
  height: 44px;
  border-radius: 16px;
}

.contacts-panel__add {
  width: 44px;
  min-width: 44px;
  height: 44px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border: 1px solid color-mix(in srgb, var(--color-primary) 24%, var(--color-shell-border));
  border-radius: 16px;
  background: color-mix(in srgb, var(--color-primary) 14%, var(--color-shell-action));
  color: var(--color-text-1);
}

.contacts-panel__back:hover,
.contacts-panel__back:focus-visible,
.contacts-panel__add:hover,
.contacts-panel__add:focus-visible,
.contacts-panel__switch:hover,
.contacts-panel__switch:focus-visible,
.contacts-card__action:hover,
.contacts-card__action:focus-visible {
  transform: translateY(-1px);
}

.contacts-panel__switcher {
  display: flex;
  gap: 8px;
}

.contacts-panel__switch {
  padding: 10px 15px;
  border-radius: 999px;
  font: 600 0.72rem/1 var(--font-body);
}

.contacts-panel__switch.is-active {
  border-color: color-mix(in srgb, var(--color-primary) 24%, var(--color-shell-border));
  background: color-mix(in srgb, var(--color-primary) 14%, var(--color-shell-action));
  color: var(--color-primary-strong);
}

.contacts-panel__scroll {
  min-height: 0;
}

.contacts-panel__list,
.contacts-panel__skeletons {
  display: grid;
  gap: 12px;
}

.contacts-card {
  display: grid;
  gap: 12px;
  padding: 18px;
  border: 1px solid var(--color-shell-border);
  border-radius: 26px;
  background: color-mix(in srgb, var(--color-shell-card) 90%, transparent);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.18);
}

.contacts-card__main {
  display: flex;
  gap: 12px;
  align-items: flex-start;
}

.contacts-card__copy {
  min-width: 0;
  flex: 1;
}

.contacts-card__title {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
}

.contacts-card__title strong {
  font-size: 0.92rem;
}

.contacts-card__title span {
  padding: 5px 8px;
  border-radius: 999px;
  background: var(--color-shell-action);
  color: var(--color-text-soft);
  font: 600 0.66rem/1 var(--font-mono);
}

.contacts-card__copy p {
  margin-top: 8px;
  color: var(--color-text-2);
  font-size: 0.78rem;
  line-height: 1.5;
}

.contacts-card__actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.contacts-card__action {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  padding: 10px 12px;
  border-radius: 16px;
  font: 600 0.74rem/1 var(--font-body);
}

.contacts-card__action.is-primary {
  border-color: color-mix(in srgb, var(--color-accent) 18%, var(--color-shell-border));
  background: color-mix(in srgb, var(--color-accent) 12%, var(--color-shell-action));
}

.contacts-card__action.is-danger {
  border-color: color-mix(in srgb, var(--color-danger) 18%, var(--color-shell-border));
  background: color-mix(in srgb, var(--color-danger) 10%, var(--color-shell-action));
}

@media (max-width: 767px) {
  .contacts-panel {
    padding: 14px;
    border-radius: 0;
  }

  .contacts-panel__hero {
    grid-template-columns: auto 1fr;
  }

  .contacts-panel__add {
    grid-column: 1 / -1;
    justify-content: center;
  }
}
</style>
