<script setup lang="ts">
import { computed } from 'vue'
import { Bell, Close } from '@element-plus/icons-vue'
import type { ConversationProfile, ConversationSummary } from '@/types/chat'
import AvatarBadge from './AvatarBadge.vue'
import ChatStatePanel from './ChatStatePanel.vue'

const props = defineProps<{
  conversation: ConversationSummary | null
  profile: ConversationProfile | null
  loading?: boolean
  errorMessage?: string | null
}>()

const emit = defineEmits<{
  close: []
  action: [command: 'toggle-top' | 'toggle-mute' | 'mark-read']
  'update-group-meta': []
  'update-group-notice': []
  'promote-member': [payload: { userId: number; role: 2 | 3 }]
  'remove-member': [userId: number]
  'add-members': []
  'leave-group': []
  'dissolve-group': []
}>()

const spotlightTags = computed(() => {
  const tags = [
    props.conversation?.conversationType === 1
      ? '单聊会话'
      : props.conversation?.conversationType === 2
        ? '群聊会话'
        : '频道会话',
  ]

  if (props.conversation?.isTop) {
    tags.push('已置顶')
  }

  if (props.conversation?.isMute) {
    tags.push('免打扰中')
  }

  if (props.conversation?.conversationType === 3) {
    tags.push(props.conversation.canSend ? '你可发言' : '仅创建者发言')
  }

  return tags
})

const actionCards = computed<
  Array<{
    command: 'toggle-top' | 'toggle-mute' | 'mark-read'
    eyebrow: string
    label: string
    description: string
    value: string
  }>
>(() => [
  {
    command: 'toggle-mute',
    eyebrow: '提醒设置',
    label: '消息免打扰',
    description: props.conversation?.isMute ? '当前会话的新消息会静默到达。' : '保持正常提醒，重要动态不会错过。',
    value: props.conversation?.isMute ? '已开启' : '未开启',
  },
  {
    command: 'toggle-top',
    eyebrow: '排序偏好',
    label: '会话置顶',
    description: props.conversation?.isTop ? '当前会话会稳定停留在列表顶部。' : '置顶后能更快回到这个聊天。',
    value: props.conversation?.isTop ? '已置顶' : '未置顶',
  },
  {
    command: 'mark-read',
    eyebrow: '收件状态',
    label: '标记已读',
    description: '立即清空当前会话未读数，不影响消息内容。',
    value: '执行',
  },
])
</script>

<template>
  <div class="profile-panel">
    <div class="profile-panel__toolbar">
      <div>
        <span class="profile-panel__eyebrow">Conversation Lens</span>
        <strong>会话详情</strong>
      </div>
      <button class="profile-panel__close" type="button" aria-label="关闭详情" @click="emit('close')">
        <Close />
      </button>
    </div>

    <el-scrollbar class="profile-panel__body">
      <div v-if="profile" class="profile-content" data-testid="conversation-profile">
        <section class="profile-hero">
          <div class="profile-hero__glow"></div>
          <AvatarBadge
            class="profile-hero__avatar"
            :name="conversation?.conversationName"
            :avatar-url="conversation?.avatarUrl"
            :type="conversation?.conversationType === 1 ? 'user' : conversation?.conversationType === 2 ? 'group' : 'channel'"
            size="lg"
          />
          <div class="profile-hero__copy">
            <span class="profile-hero__eyebrow">
              {{
                conversation?.conversationType === 1
                  ? 'DIRECT LINE'
                  : conversation?.conversationType === 2
                    ? 'GROUP ROOM'
                    : 'BROADCAST CHANNEL'
              }}
            </span>
            <strong>{{ conversation?.conversationName }}</strong>
            <p>{{ profile.subtitle }}</p>
          </div>
          <div class="profile-hero__tags">
            <span v-for="tag in spotlightTags" :key="tag">{{ tag }}</span>
          </div>
        </section>

        <section v-if="profile.signature" class="profile-card profile-card--copy">
          <div class="profile-card__header">
            <span>个性签名</span>
            <small>公开资料</small>
          </div>
          <p>{{ profile.signature }}</p>
        </section>

        <section v-if="profile.notice" class="profile-card profile-card--notice">
          <div class="profile-card__header">
            <span>{{ conversation?.conversationType === 3 ? '频道公告' : '群公告' }}</span>
            <small>实时信息</small>
          </div>
          <p>{{ profile.notice }}</p>
        </section>

        <section v-if="profile.fields.length" class="profile-card">
          <div class="profile-card__header">
            <span>资料字段</span>
            <small>真实接口返回</small>
          </div>
          <dl class="profile-grid">
            <div v-for="field in profile.fields" :key="field.key">
              <dt>{{ field.label }}</dt>
              <dd>{{ field.value }}</dd>
            </div>
          </dl>
        </section>

        <section class="profile-card">
          <div class="profile-card__header">
            <span>快捷操作</span>
            <small>仅影响当前会话</small>
          </div>
          <ul class="profile-actions">
            <li v-for="action in actionCards" :key="action.command">
              <button class="profile-action" type="button" @click="emit('action', action.command)">
                <div class="profile-action__copy">
                  <span>{{ action.eyebrow }}</span>
                  <strong>{{ action.label }}</strong>
                  <p>{{ action.description }}</p>
                </div>
                <div class="profile-action__meta">
                  <span>{{ action.value }}</span>
                  <Bell />
                </div>
              </button>
            </li>
          </ul>
        </section>

        <section v-if="profile.group" class="profile-card">
          <div class="profile-card__header">
            <span>治理操作</span>
            <small>group governance</small>
          </div>
          <div class="profile-governance">
            <button
              v-if="profile.group.canEditMeta"
              class="profile-action"
              type="button"
              @click="emit('update-group-meta')"
            >
              <div class="profile-action__copy">
                <span>基础信息</span>
                <strong>更新名称</strong>
                <p>同步群名或频道名到会话资料与列表摘要。</p>
              </div>
            </button>
            <button
              v-if="profile.group.canEditMeta"
              class="profile-action"
              type="button"
              @click="emit('update-group-notice')"
            >
              <div class="profile-action__copy">
                <span>公告内容</span>
                <strong>更新公告</strong>
                <p>群主和管理员可以维护对所有成员可见的公告。</p>
              </div>
            </button>
            <button
              v-if="profile.group.canManageMembers"
              class="profile-action"
              type="button"
              @click="emit('add-members')"
            >
              <div class="profile-action__copy">
                <span>成员操作</span>
                <strong>邀请成员</strong>
                <p>快速把最近搜索到的用户拉入当前群组或频道。</p>
              </div>
            </button>
            <button
              v-if="profile.group.canLeave"
              class="profile-action"
              type="button"
              @click="emit('leave-group')"
            >
              <div class="profile-action__copy">
                <span>成员状态</span>
                <strong>退出当前会话</strong>
                <p>离开后保留会话历史，但不再参与新消息协作。</p>
              </div>
            </button>
            <button
              v-if="profile.group.canDissolve"
              class="profile-action"
              type="button"
              @click="emit('dissolve-group')"
            >
              <div class="profile-action__copy">
                <span>群主权限</span>
                <strong>解散会话</strong>
                <p>仅群主或频道创建者可执行，操作不可撤回。</p>
              </div>
            </button>
          </div>
        </section>

        <section v-if="profile.members?.length" class="profile-card">
          <div class="profile-card__header">
            <span>成员列表</span>
            <small>{{ profile.members.length }} members</small>
          </div>
          <div class="profile-members">
            <article v-for="member in profile.members" :key="member.userId" class="profile-member">
              <AvatarBadge
                :name="member.nickname"
                :avatar-url="member.avatarUrl"
                size="md"
              />
              <div class="profile-member__copy">
                <strong>{{ member.nickname }}</strong>
                <span>#{{ member.userNo }}</span>
              </div>
              <div class="profile-member__actions">
                <span class="profile-member__role">
                  {{ member.role === 1 ? '群主' : member.role === 3 ? '管理员' : '成员' }}
                </span>
                <button
                  v-if="profile.group?.canManageRoles && member.role !== 1"
                  class="profile-member__button"
                  type="button"
                  @click="emit('promote-member', { userId: member.userId, role: member.role === 3 ? 2 : 3 })"
                >
                  {{ member.role === 3 ? '撤销管理员' : '设为管理员' }}
                </button>
                <button
                  v-if="profile.group?.canManageMembers && member.role !== 1"
                  class="profile-member__button is-danger"
                  type="button"
                  @click="emit('remove-member', member.userId)"
                >
                  移除
                </button>
              </div>
            </article>
          </div>
        </section>
      </div>

      <ChatStatePanel
        v-else-if="loading"
        compact
        title="正在获取会话详情"
        description="当前会话的真实资料正在同步。"
      />
      <ChatStatePanel
        v-else-if="errorMessage"
        compact
        title="会话详情加载失败"
        :description="errorMessage"
        role="alert"
      />
      <ChatStatePanel
        v-else
        compact
        title="没有详情内容"
        description="选择会话后，这里会展示单聊、群聊或频道的资料卡片。"
      />
    </el-scrollbar>
  </div>
</template>

<style scoped>
.profile-panel {
  height: 100%;
  min-height: 0;
  display: flex;
  flex-direction: column;
  padding: 18px;
  background:
    radial-gradient(circle at top right, color-mix(in srgb, var(--color-shell-glow) 82%, transparent), transparent 34%),
    var(--color-shell-panel);
  overflow: hidden;
}

.profile-panel__toolbar {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 14px;
}

.profile-panel__toolbar strong {
  display: block;
  margin-top: 4px;
  font: var(--font-title-md);
}

.profile-panel__eyebrow {
  color: var(--color-shell-eyebrow);
  font: var(--font-eyebrow);
  letter-spacing: 0.14em;
  text-transform: uppercase;
}

.profile-panel__body {
  min-height: 0;
}

.profile-panel__close {
  width: 38px;
  height: 38px;
  display: grid;
  place-items: center;
  border: 1px solid var(--color-shell-border);
  border-radius: 14px;
  background: var(--color-shell-action);
  color: var(--color-text-2);
  transition:
    transform var(--motion-fast) ease,
    background var(--motion-fast) ease,
    color var(--motion-fast) ease;
}

.profile-panel__close:hover,
.profile-panel__close:focus-visible {
  transform: translateY(-1px);
  background: var(--color-shell-action-hover);
  color: var(--color-text-1);
}

.profile-content {
  display: grid;
  gap: 14px;
}

.profile-hero,
.profile-card {
  position: relative;
  overflow: hidden;
  border: 1px solid var(--color-shell-border);
  border-radius: 28px;
  background: var(--color-shell-card);
  box-shadow:
    var(--shadow-inset-soft),
    var(--shadow-card);
}

.profile-hero {
  padding: 24px;
}

.profile-hero__glow {
  position: absolute;
  inset: auto -20% -30% auto;
  width: 180px;
  height: 180px;
  border-radius: 50%;
  background: radial-gradient(circle, color-mix(in srgb, var(--color-shell-glow) 92%, transparent), transparent 68%);
  pointer-events: none;
}

.profile-hero__avatar {
  margin-bottom: 16px;
}

.profile-hero__copy {
  position: relative;
  z-index: 1;
}

.profile-hero__eyebrow {
  display: inline-block;
  margin-bottom: 10px;
  color: var(--color-shell-eyebrow);
  font: var(--font-eyebrow);
  letter-spacing: 0.14em;
}

.profile-hero__copy strong {
  display: block;
  font: 700 1.18rem/1.08 var(--font-display);
}

.profile-hero__copy p {
  margin-top: 8px;
  max-width: 24rem;
  color: var(--color-text-2);
  font-size: 0.84rem;
  line-height: 1.5;
}

.profile-hero__tags {
  position: relative;
  z-index: 1;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 16px;
}

.profile-hero__tags span {
  padding: 7px 11px;
  border-radius: 999px;
  border: 1px solid var(--color-shell-border);
  background: var(--color-shell-action);
  color: var(--color-text-1);
  font: 600 0.7rem/1 var(--font-body);
}

.profile-card {
  padding: 20px;
}

.profile-card--copy p,
.profile-card--notice p {
  white-space: pre-wrap;
  color: var(--color-text-1);
  font-size: 0.84rem;
  line-height: 1.6;
}

.profile-card--notice {
  background:
    linear-gradient(180deg, color-mix(in srgb, var(--color-shell-glow) 36%, transparent), transparent 120%),
    var(--color-shell-card);
}

.profile-card__header {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 10px;
  margin-bottom: 14px;
}

.profile-card__header span,
.profile-card__header small {
  display: block;
}

.profile-card__header span {
  color: var(--color-text-1);
  font: 700 0.88rem/1.1 var(--font-display);
}

.profile-card__header small {
  color: var(--color-text-soft);
  font: 600 0.64rem/1 var(--font-mono);
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.profile-grid,
.profile-actions {
  display: grid;
  gap: 10px;
  margin: 0;
}

.profile-grid {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.profile-grid div {
  padding: 16px;
  border-radius: 20px;
  background: var(--color-shell-card-muted);
  border: 1px solid var(--color-shell-border);
}

.profile-grid dt {
  margin-bottom: 7px;
  color: var(--color-text-soft);
  font: 600 0.66rem/1 var(--font-mono);
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.profile-grid dd {
  margin: 0;
  color: var(--color-text-1);
  font-size: 0.84rem;
  line-height: 1.4;
}

.profile-actions {
  list-style: none;
  padding: 0;
}

.profile-action {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 16px 17px;
  border: 1px solid var(--color-shell-border);
  border-radius: 20px;
  background: var(--color-shell-card-muted);
  color: inherit;
  text-align: left;
  transition:
    transform var(--motion-fast) ease,
    border-color var(--motion-fast) ease,
    background var(--motion-fast) ease;
}

.profile-action:hover,
.profile-action:focus-visible {
  transform: translateY(-1px);
  border-color: var(--color-shell-border-strong);
  background: var(--color-shell-action-hover);
}

.profile-action__copy span {
  display: block;
  margin-bottom: 6px;
  color: var(--color-shell-eyebrow);
  font: var(--font-eyebrow);
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.profile-action__copy strong {
  display: block;
  font-size: 0.86rem;
  line-height: 1.25;
}

.profile-action__copy p {
  margin-top: 6px;
  color: var(--color-text-2);
  font-size: 0.77rem;
  line-height: 1.45;
}

.profile-action__meta {
  display: grid;
  justify-items: end;
  gap: 8px;
  flex-shrink: 0;
}

.profile-action__meta span {
  padding: 7px 10px;
  border-radius: 999px;
  background: var(--color-shell-action);
  color: var(--color-text-1);
  font: 600 0.68rem/1 var(--font-body);
}

.profile-action__meta svg {
  width: 18px;
  height: 18px;
  color: var(--color-shell-eyebrow);
}

.profile-governance,
.profile-members {
  display: grid;
  gap: 10px;
}

.profile-member {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr) auto;
  gap: 10px;
  align-items: center;
  padding: 16px;
  border-radius: 20px;
  border: 1px solid var(--color-shell-border);
  background: var(--color-shell-card-muted);
}

.profile-member__copy strong,
.profile-member__copy span {
  display: block;
}

.profile-member__copy strong {
  font-size: 0.84rem;
}

.profile-member__copy span {
  margin-top: 4px;
  color: var(--color-text-soft);
  font: 600 0.64rem/1 var(--font-mono);
}

.profile-member__actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 8px;
}

.profile-member__role,
.profile-member__button {
  padding: 7px 10px;
  border-radius: 999px;
  font: 600 0.68rem/1 var(--font-body);
}

.profile-member__role {
  background: var(--color-shell-action);
}

.profile-member__button {
  border: 1px solid var(--color-shell-border);
  background: transparent;
}

.profile-member__button.is-danger {
  color: var(--color-danger);
}

@media (max-width: 767px) {
  .profile-panel {
    padding: 14px;
  }

  .profile-grid {
    grid-template-columns: 1fr;
  }
}
</style>
