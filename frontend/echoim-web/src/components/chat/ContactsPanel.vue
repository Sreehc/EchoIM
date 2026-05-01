<script setup lang="ts">
import { computed } from 'vue'
import { ArrowLeft, CirclePlus, Delete, EditPen, Lock, MessageBox, MoreFilled } from '@element-plus/icons-vue'
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

const pendingRequests = computed(() => props.requests.filter((item) => item.status === 0))

const visibleItems = computed(() => {
  const keyword = props.keyword.trim().toLowerCase()
  const source =
    props.activeTab === 'friends'
      ? props.friends
      : props.activeTab === 'blocked'
        ? props.blocked
        : pendingRequests.value

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
  if (item.status === 1) return '已通过'
  if (item.status === 2) return '已拒绝'
  return item.direction === 'INBOUND' ? '待处理' : '已发送'
}

function requestCountBadgeClass(item: FriendRequestItem) {
  if (item.status === 1) return 'is-success'
  if (item.status === 2) return 'is-danger'
  return item.direction === 'INBOUND' ? 'is-pending' : 'is-sent'
}
</script>

<template>
  <section class="contacts-panel">
    <header class="contacts-panel__hero">
      <button class="contacts-panel__back" type="button" aria-label="返回会话列表" @click="emit('back')">
        <ArrowLeft />
      </button>
      <div class="contacts-panel__copy">
        <strong>联系人</strong>
      </div>
      <button class="contacts-panel__add" type="button" aria-label="添加联系人" @click="emit('open-add-contact')">
        <CirclePlus />
      </button>
    </header>

    <div class="contacts-panel__switcher">
      <button
        v-for="tab in [
          { key: 'friends', label: `好友 ${friends.length}` },
          { key: 'requests', label: `申请 ${pendingRequests.length}` },
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
      <template v-else-if="activeTab === 'friends' && friends.length">
        <div v-if="!keyword" class="contacts-carousel">
          <div class="contacts-carousel__header">
            <span>好友</span>
            <small>{{ friends.length }} 人</small>
          </div>
          <div class="contacts-carousel__track">
            <button
              v-for="friend in friends.slice(0, 20)"
              :key="friend.friendUserId"
              class="contacts-carousel__item"
              type="button"
              @click="emit('open-chat', friend.friendUserId)"
            >
              <AvatarBadge
                class="contacts-carousel__avatar"
                :name="friend.displayName"
                :avatar-url="friend.avatarUrl"
                size="lg"
                type="user"
              />
              <span class="contacts-carousel__name">{{ friend.displayName }}</span>
            </button>
          </div>
        </div>
        <div class="contacts-list">
          <div class="contacts-list__header">
            <span>全部好友</span>
            <small>{{ visibleItems.length }}</small>
          </div>
          <article
            v-for="item in visibleItems"
            :key="`friend-${item.friendUserId}`"
            class="contacts-row"
          >
            <AvatarBadge
              class="contacts-row__avatar"
              :name="item.displayName"
              :avatar-url="item.avatarUrl"
              size="lg"
              type="user"
            />
            <div class="contacts-row__copy">
              <strong>{{ item.displayName }}</strong>
              <p>{{ item.remark ? `备注：${item.remark}` : `@${item.nickname}` }}</p>
            </div>
            <div class="contacts-row__actions">
              <button
                class="contacts-row__btn"
                type="button"
                @click="emit('open-chat', item.friendUserId)"
              >
                <MessageBox />
              </button>
              <el-dropdown trigger="click" placement="bottom-end" popper-class="contacts-dropdown">
                <button class="contacts-row__btn" type="button" aria-label="更多操作">
                  <MoreFilled />
                </button>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item @click="emit('update-remark', item)">
                      <EditPen />
                      备注
                    </el-dropdown-item>
                    <el-dropdown-item @click="emit('block-friend', item.friendUserId)">
                      <Lock />
                      拉黑
                    </el-dropdown-item>
                    <el-dropdown-item class="is-danger" @click="emit('delete-friend', item.friendUserId)">
                      <Delete />
                      删除好友
                    </el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
            </div>
          </article>
        </div>
      </template>
      <template v-else-if="activeTab === 'requests' && visibleItems.length">
        <div class="contacts-list">
          <article
            v-for="item in visibleItems"
            :key="`request-${item.requestId}`"
            class="contacts-row contacts-row--request"
          >
            <AvatarBadge
              class="contacts-row__avatar"
              :name="item.nickname"
              :avatar-url="item.avatarUrl"
              size="lg"
              type="user"
            />
            <div class="contacts-row__copy">
              <div class="contacts-row__head">
                <strong>{{ item.nickname }}</strong>
                <span class="contacts-row__badge" :class="requestCountBadgeClass(item)">
                  {{ requestCountBadge(item) }}
                </span>
              </div>
              <p>{{ item.applyMsg || '这条申请没有附言。' }}</p>
            </div>
            <div class="contacts-row__actions">
              <button
                v-if="item.direction === 'INBOUND' && item.status === 0"
                class="contacts-row__btn contacts-row__btn--primary"
                type="button"
                @click="emit('approve-request', item.requestId)"
              >
                通过
              </button>
              <button
                class="contacts-row__btn"
                type="button"
                @click="emit('open-chat', item.direction === 'INBOUND' ? item.fromUserId : item.toUserId)"
              >
                <MessageBox />
              </button>
              <el-dropdown
                v-if="item.direction === 'INBOUND' && item.status === 0"
                trigger="click"
                placement="bottom-end"
                popper-class="contacts-dropdown"
              >
                <button class="contacts-row__btn" type="button" aria-label="更多操作">
                  <MoreFilled />
                </button>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item class="is-danger" @click="emit('reject-request', item.requestId)">
                      <Delete />
                      拒绝申请
                    </el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
            </div>
          </article>
        </div>
      </template>
      <template v-else-if="activeTab === 'blocked' && visibleItems.length">
        <div class="contacts-list">
          <article
            v-for="item in visibleItems"
            :key="`blocked-${item.friendUserId}`"
            class="contacts-row"
          >
            <AvatarBadge
              class="contacts-row__avatar"
              :name="item.displayName"
              :avatar-url="item.avatarUrl"
              size="lg"
              type="user"
            />
            <div class="contacts-row__copy">
              <strong>{{ item.displayName }}</strong>
              <p>{{ item.remark ? `备注：${item.remark}` : `@${item.nickname}` }}</p>
            </div>
            <div class="contacts-row__actions">
              <button
                class="contacts-row__btn contacts-row__btn--primary"
                type="button"
                @click="emit('unblock-friend', item.friendUserId)"
              >
                解除
              </button>
            </div>
          </article>
        </div>
      </template>
      <ChatStatePanel
        v-else
        compact
        :title="activeTab === 'friends' ? '还没有联系人' : activeTab === 'requests' ? '还没有申请记录' : '黑名单为空'"
        description=""
      />
    </el-scrollbar>
  </section>
</template>

<style scoped>
.contacts-panel {
  display: grid;
  grid-template-rows: auto auto auto minmax(0, 1fr);
  gap: 10px;
  height: 100%;
  min-height: 0;
  padding: 18px 18px 16px;
  background:
    radial-gradient(circle at 100% 0%, color-mix(in srgb, var(--interactive-focus-ring) 52%, transparent), transparent 28%),
    linear-gradient(180deg, color-mix(in srgb, var(--surface-panel) 96%, transparent), transparent);
}

.contacts-panel__hero {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr) auto;
  gap: 10px;
  align-items: center;
}

.contacts-panel__copy strong {
  display: block;
  font: 620 1rem/1.04 var(--font-display);
}

.contacts-panel__back,
.contacts-panel__add,
.contacts-panel__switch {
  transition:
    transform var(--motion-fast) ease,
    border-color var(--motion-fast) ease,
    background var(--motion-fast) ease,
    color var(--motion-fast) ease;
}

.contacts-panel__back,
.contacts-panel__switch {
  border: 1px solid var(--border-default);
  background: var(--interactive-secondary-bg);
  color: var(--text-primary);
}

.contacts-panel__back {
  width: var(--btn-icon-size);
  height: var(--btn-icon-size);
  border-radius: var(--radius-control);
}

.contacts-panel__add {
  width: var(--btn-icon-size);
  min-width: var(--btn-icon-size);
  height: var(--btn-icon-size);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border: 1px solid color-mix(in srgb, var(--interactive-primary-bg) 24%, var(--border-default));
  border-radius: var(--radius-control);
  background: color-mix(in srgb, var(--interactive-primary-bg) 10%, var(--interactive-secondary-bg));
  color: var(--text-primary);
}

.contacts-panel__back:hover,
.contacts-panel__back:focus-visible,
.contacts-panel__add:hover,
.contacts-panel__add:focus-visible,
.contacts-panel__switch:hover,
.contacts-panel__switch:focus-visible {
  transform: none;
}

.contacts-panel__switcher {
  display: flex;
  gap: 8px;
  padding-top: 2px;
}

.contacts-panel__switch {
  padding: 9px 14px;
  border-radius: var(--radius-pill);
  font: 600 var(--text-xs)/1 var(--font-body);
  letter-spacing: 0.01em;
}

.contacts-panel__switch.is-active {
  border-color: var(--border-brand);
  background: color-mix(in srgb, var(--interactive-selected-bg) 90%, var(--interactive-secondary-bg));
  color: var(--interactive-selected-fg);
}

.contacts-panel__scroll {
  min-height: 0;
}

.contacts-panel__skeletons {
  display: grid;
  gap: 8px;
}

/* ── Online friends carousel ── */

.contacts-carousel {
  margin-bottom: 14px;
}

.contacts-carousel__header {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 8px;
  margin-bottom: 10px;
  padding: 0 2px;
}

.contacts-carousel__header span {
  color: var(--text-primary);
  font: 620 var(--text-sm)/1.08 var(--font-display);
  letter-spacing: -0.01em;
}

.contacts-carousel__header small {
  color: var(--text-quaternary);
  font: 600 var(--text-2xs)/1 var(--font-mono);
  letter-spacing: 0.04em;
}

.contacts-carousel__track {
  display: flex;
  gap: 14px;
  overflow-x: auto;
  padding: 4px 2px 8px;
  scrollbar-width: none;
}

.contacts-carousel__track::-webkit-scrollbar {
  display: none;
}

.contacts-carousel__item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  min-width: 64px;
  padding: 6px 4px;
  border: 0;
  border-radius: var(--radius-md);
  background: transparent;
  cursor: pointer;
  transition:
    background var(--motion-fast) var(--motion-ease-out),
    transform var(--motion-fast) var(--motion-ease-out);
}

.contacts-carousel__item:hover {
  background: color-mix(in srgb, var(--interactive-secondary-bg-hover) 50%, transparent);
  transform: translateY(-1px);
}

.contacts-carousel__avatar {
  flex-shrink: 0;
}

.contacts-carousel__name {
  max-width: 60px;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
  color: var(--text-secondary);
  font-size: var(--text-xs);
  line-height: 1.2;
  text-align: center;
}

/* ── Contact list ── */

.contacts-list {
  display: grid;
  gap: 2px;
}

.contacts-list__header {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 8px;
  padding: 6px 2px 8px;
}

.contacts-list__header span {
  color: var(--text-primary);
  font: 620 var(--text-sm)/1.08 var(--font-display);
  letter-spacing: -0.01em;
}

.contacts-list__header small {
  color: var(--text-quaternary);
  font: 600 var(--text-2xs)/1 var(--font-mono);
  letter-spacing: 0.04em;
}

/* ── Contact row ── */

.contacts-row {
  display: grid;
  grid-template-columns: 42px minmax(0, 1fr) auto;
  gap: 12px;
  align-items: center;
  padding: 10px 12px;
  border: 0;
  border-radius: var(--radius-md);
  background: transparent;
  transition:
    background var(--motion-fast) var(--motion-ease-out);
}

.contacts-row:hover {
  background: color-mix(in srgb, var(--interactive-secondary-bg-hover) 50%, transparent);
}

.contacts-row__avatar {
  flex-shrink: 0;
}

.contacts-row__copy {
  min-width: 0;
  display: grid;
  gap: 3px;
}

.contacts-row__head {
  display: flex;
  align-items: center;
  gap: 8px;
}

.contacts-row__copy strong {
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
  font-size: var(--text-sm);
  font-weight: 600;
  color: var(--text-primary);
  line-height: 1.2;
}

.contacts-row__copy p {
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
  color: var(--text-tertiary);
  font-size: var(--text-xs);
  line-height: 1.3;
}

.contacts-row__badge {
  flex-shrink: 0;
  padding: 3px 7px;
  border-radius: var(--radius-pill);
  background: var(--interactive-secondary-bg);
  color: var(--text-quaternary);
  font: 600 var(--text-2xs)/1 var(--font-mono);
  letter-spacing: 0.04em;
}

.contacts-row__badge.is-pending {
  color: var(--status-warning);
}

.contacts-row__badge.is-sent {
  color: var(--interactive-selected-fg);
}

.contacts-row__badge.is-success {
  color: var(--status-success);
}

.contacts-row__badge.is-danger {
  color: var(--text-danger);
}

.contacts-row__actions {
  display: flex;
  align-items: center;
  gap: 4px;
  flex-shrink: 0;
}

.contacts-row__btn {
  width: 32px;
  height: 32px;
  display: grid;
  place-items: center;
  padding: 0;
  border: 0;
  border-radius: var(--radius-control);
  background: transparent;
  color: var(--text-tertiary);
  cursor: pointer;
  transition:
    background var(--motion-fast) var(--motion-ease-out),
    color var(--motion-fast) var(--motion-ease-out);
}

.contacts-row__btn svg {
  width: 16px;
  height: 16px;
}

.contacts-row__btn:hover,
.contacts-row__btn:focus-visible {
  background: var(--interactive-secondary-bg-hover);
  color: var(--text-primary);
}

.contacts-row__btn--primary {
  width: auto;
  padding: 0 12px;
  min-height: 28px;
  border: 1px solid color-mix(in srgb, var(--interactive-primary-bg) 24%, var(--border-default));
  background: color-mix(in srgb, var(--interactive-primary-bg) 10%, var(--interactive-secondary-bg));
  color: var(--interactive-primary-bg);
  font: 600 var(--text-xs)/1 var(--font-sans);
}

.contacts-row__btn--primary:hover {
  border-color: color-mix(in srgb, var(--interactive-primary-bg) 36%, var(--border-default));
  background: color-mix(in srgb, var(--interactive-primary-bg) 16%, var(--interactive-secondary-bg));
}

:deep(.contacts-dropdown) {
  padding: 8px;
  border: 1px solid color-mix(in srgb, var(--border-default) 92%, transparent);
  border-radius: 18px;
  background: color-mix(in srgb, var(--surface-overlay) 96%, rgba(18, 22, 30, 0.08));
  box-shadow: var(--shadow-md);
  backdrop-filter: blur(18px);
}

:deep(.contacts-dropdown .el-dropdown-menu) {
  display: grid;
  gap: 4px;
  padding: 0;
}

:deep(.contacts-dropdown .el-dropdown-menu__item) {
  min-width: 148px;
  min-height: 42px;
  padding: 0 12px;
  border-radius: 12px;
  color: var(--text-primary);
  font-size: var(--text-sm);
  font-weight: 600;
  line-height: 1.2;
}

:deep(.contacts-dropdown .el-dropdown-menu__item:hover),
:deep(.contacts-dropdown .el-dropdown-menu__item:focus-visible) {
  background: color-mix(in srgb, var(--interactive-selected-bg) 88%, transparent);
  color: var(--text-primary);
}

:deep(.contacts-dropdown .el-dropdown-menu__item [class*='el-icon']),
:deep(.contacts-dropdown .el-dropdown-menu__item svg) {
  width: 16px;
  height: 16px;
  margin-right: 8px;
  color: var(--text-tertiary);
}

:deep(.contacts-dropdown .el-dropdown-menu__item.is-danger) {
  color: var(--text-danger);
}

:deep(.contacts-dropdown .el-dropdown-menu__item.is-danger:hover),
:deep(.contacts-dropdown .el-dropdown-menu__item.is-danger:focus-visible) {
  background: color-mix(in srgb, var(--status-danger) 10%, var(--surface-panel));
}

:deep(.contacts-dropdown .el-dropdown-menu__item.is-danger [class*='el-icon']),
:deep(.contacts-dropdown .el-dropdown-menu__item.is-danger svg) {
  color: currentColor;
}

@media (max-width: 767px) {
  .contacts-panel {
    padding: 14px;
    border-radius: 0;
  }

  .contacts-panel__hero {
    grid-template-columns: auto 1fr;
    gap: 8px;
  }

  .contacts-panel__add {
    grid-column: 1 / -1;
    justify-content: center;
  }

  .contacts-carousel__item {
    min-width: 56px;
  }

  .contacts-carousel__name {
    max-width: 52px;
    font-size: var(--text-2xs);
  }

  .contacts-row {
    grid-template-columns: 38px minmax(0, 1fr) auto;
    padding: 9px 10px;
    gap: 10px;
  }
}
</style>
