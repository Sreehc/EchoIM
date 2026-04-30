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
  'open-public-profile': [path: string]
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
            <strong>{{ conversation?.conversationName }}</strong>
            <p>{{ profile.subtitle }}</p>
          </div>
          <div class="profile-hero__tags">
            <span v-for="tag in spotlightTags" :key="tag">{{ tag }}</span>
          </div>
        </section>

        <div class="profile-reading-panel">
          <section v-if="profile.signature" class="profile-card profile-card--copy">
            <div class="profile-card__header">
              <span>个性签名</span>
              <small>资料</small>
            </div>
            <p>{{ profile.signature }}</p>
          </section>

          <section v-if="profile.notice" class="profile-card profile-card--notice">
            <div class="profile-card__header">
              <span>{{ conversation?.conversationType === 3 ? '频道公告' : '群公告' }}</span>
              <small>最新内容</small>
            </div>
            <p>{{ profile.notice }}</p>
          </section>

          <section v-if="profile.fields.length" class="profile-card">
            <div class="profile-card__header">
              <span>资料字段</span>
              <small>基础信息</small>
            </div>
            <dl class="profile-grid">
              <div v-for="field in profile.fields" :key="field.key">
                <dt>{{ field.label }}</dt>
                <dd>{{ field.value }}</dd>
              </div>
            </dl>
          </section>

          <section v-if="profile.publicProfilePath" class="profile-card">
            <div class="profile-card__header">
              <span>公开主页</span>
              <small>@username</small>
            </div>
            <button class="profile-action" type="button" @click="emit('open-public-profile', profile.publicProfilePath)">
              <div class="profile-action__copy">
                <span>站内入口</span>
                <strong>查看公开主页</strong>
                <p>用公开链接查看这个用户对外展示的基础资料。</p>
              </div>
            </button>
          </section>

          <section class="profile-card">
            <div class="profile-card__header">
              <span>快捷操作</span>
              <small>当前会话</small>
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
              <small>管理权限</small>
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
                class="profile-action is-danger"
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
                class="profile-action is-danger"
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

          <section v-if="profile.members?.length" class="profile-card profile-card--members">
            <div class="profile-card__header">
              <span>成员列表</span>
              <small>{{ profile.members.length }} 人</small>
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
  padding: 14px;
  background:
    radial-gradient(circle at top right, color-mix(in srgb, var(--color-shell-glow) 44%, transparent), transparent 30%),
    transparent;
  overflow: hidden;
}

.profile-panel__toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 10px;
}

.profile-panel__toolbar strong {
  display: block;
  margin-top: 0;
  font: 620 0.92rem/1.08 var(--font-display);
  letter-spacing: -0.02em;
}

.profile-panel__body {
  min-height: 0;
}

.profile-panel__close {
  width: 34px;
  height: 34px;
  display: grid;
  place-items: center;
  border: 1px solid var(--color-shell-border);
  border-radius: 12px;
  background: color-mix(in srgb, var(--color-shell-card) 82%, transparent);
  color: var(--color-text-2);
  transition:
    background var(--motion-fast) ease,
    border-color var(--motion-fast) ease,
    color var(--motion-fast) ease;
}

.profile-panel__close:hover,
.profile-panel__close:focus-visible {
  background: var(--color-shell-action-hover);
  border-color: var(--color-shell-border-strong);
  color: var(--color-text-1);
}

.profile-content {
  display: grid;
  gap: 10px;
}

.profile-hero,
.profile-reading-panel {
  position: relative;
  overflow: hidden;
  border: 1px solid var(--color-shell-border);
  border-radius: 18px;
  background: var(--color-shell-card);
}

.profile-hero {
  padding: 20px 18px 18px;
  background:
    linear-gradient(180deg, color-mix(in srgb, var(--color-shell-card-strong) 78%, transparent), transparent 100%),
    var(--color-shell-card);
}

.profile-hero__glow {
  position: absolute;
  inset: auto -14% -26% auto;
  width: 132px;
  height: 132px;
  border-radius: 50%;
  background: radial-gradient(circle, color-mix(in srgb, var(--color-shell-glow) 42%, transparent), transparent 70%);
  pointer-events: none;
}

.profile-hero__avatar {
  margin-bottom: 12px;
}

.profile-hero__copy {
  position: relative;
  z-index: 1;
}

.profile-hero__copy strong {
  display: block;
  font: 620 1.02rem/1.06 var(--font-display);
  letter-spacing: -0.03em;
}

.profile-hero__copy p {
  margin-top: 6px;
  max-width: 22rem;
  color: var(--color-text-2);
  font-size: 0.79rem;
  line-height: 1.48;
}

.profile-hero__tags {
  position: relative;
  z-index: 1;
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: 12px;
}

.profile-hero__tags span {
  padding: 6px 9px;
  border-radius: 999px;
  border: 1px solid var(--color-shell-border);
  background: color-mix(in srgb, var(--color-shell-action) 72%, transparent);
  color: var(--color-text-1);
  font: 600 0.62rem/1 var(--font-mono);
  letter-spacing: 0.04em;
}

.profile-reading-panel {
  display: grid;
  background:
    linear-gradient(180deg, color-mix(in srgb, var(--color-shell-card-strong) 52%, transparent), transparent 22%),
    var(--color-shell-card);
}

.profile-reading-panel > .profile-card {
  border: 0;
  border-radius: 0;
  background: transparent;
}

.profile-reading-panel > .profile-card + .profile-card {
  border-top: 1px solid color-mix(in srgb, var(--color-shell-border) 84%, transparent);
}

.profile-card {
  padding: 15px;
}

.profile-card--copy p,
.profile-card--notice p {
  white-space: pre-wrap;
  color: var(--color-text-1);
  font-size: 0.79rem;
  line-height: 1.56;
}

.profile-card--copy p,
.profile-card--notice p {
  padding: 12px 13px;
  border-radius: 14px;
  border: 1px solid color-mix(in srgb, var(--color-shell-border) 74%, transparent);
  background: color-mix(in srgb, var(--color-shell-card-muted) 58%, transparent);
}

.profile-card--notice {
  background:
    linear-gradient(180deg, color-mix(in srgb, var(--color-shell-glow) 7%, transparent), transparent 88%),
    transparent;
}

.profile-card__header {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 10px;
  margin-bottom: 10px;
}

.profile-card--members .profile-card__header {
  margin-bottom: 12px;
}

.profile-card__header span,
.profile-card__header small {
  display: block;
}

.profile-card__header span {
  color: var(--color-text-1);
  font: 620 0.79rem/1.08 var(--font-display);
  letter-spacing: -0.01em;
}

.profile-card__header small {
  color: var(--color-text-soft);
  font: 600 0.58rem/1 var(--font-mono);
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.profile-card__header small::before {
  content: '/ ';
  opacity: 0.72;
}

.profile-grid,
.profile-actions {
  display: grid;
  gap: 8px;
  margin: 0;
}

.profile-grid {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.profile-grid div {
  padding: 12px;
  border-radius: 14px;
  background: color-mix(in srgb, var(--color-shell-card-muted) 70%, transparent);
  border: 1px solid var(--color-shell-border);
}

.profile-grid dt {
  margin-bottom: 5px;
  color: var(--color-text-soft);
  font: 600 0.58rem/1 var(--font-mono);
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.profile-grid dd {
  margin: 0;
  color: var(--color-text-1);
  font-size: 0.78rem;
  line-height: 1.4;
}

.profile-actions {
  list-style: none;
  padding: 0;
}

.profile-actions li {
  display: block;
}

.profile-action {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 12px 13px;
  border: 1px solid var(--color-shell-border);
  border-radius: 14px;
  background: color-mix(in srgb, var(--color-shell-card-muted) 66%, transparent);
  color: inherit;
  text-align: left;
  transition:
    border-color var(--motion-fast) ease,
    background var(--motion-fast) ease;
}

.profile-action:hover,
.profile-action:focus-visible {
  border-color: var(--color-shell-border-strong);
  background: var(--color-shell-action-hover);
}

.profile-action.is-danger {
  border-color: color-mix(in srgb, var(--color-danger) 10%, var(--color-shell-border));
}

.profile-action.is-danger:hover,
.profile-action.is-danger:focus-visible {
  border-color: color-mix(in srgb, var(--color-danger) 18%, var(--color-shell-border));
  background: color-mix(in srgb, var(--color-danger) 4%, var(--color-shell-action-hover));
}

.profile-action__copy span {
  display: block;
  margin-bottom: 4px;
  color: var(--color-shell-eyebrow);
  font: 600 0.58rem/1 var(--font-mono);
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.profile-action__copy strong {
  display: block;
  font-size: 0.8rem;
  font-weight: 600;
  line-height: 1.22;
}

.profile-action__copy p {
  margin-top: 4px;
  color: var(--color-text-2);
  font-size: 0.72rem;
  line-height: 1.42;
}

.profile-action__meta {
  display: grid;
  justify-items: end;
  gap: 6px;
  flex-shrink: 0;
  min-width: 54px;
}

.profile-action__meta span {
  padding: 5px 8px;
  border-radius: 999px;
  border: 1px solid var(--color-shell-border);
  background: color-mix(in srgb, var(--color-shell-action) 72%, transparent);
  color: var(--color-text-1);
  font: 600 0.6rem/1 var(--font-mono);
  letter-spacing: 0.04em;
}

.profile-action__meta svg {
  width: 15px;
  height: 15px;
  color: var(--color-shell-eyebrow);
}

.profile-governance,
.profile-members {
  display: grid;
  gap: 8px;
}

.profile-member {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr) auto;
  gap: 10px;
  align-items: center;
  padding: 12px;
  border-radius: 14px;
  border: 1px solid var(--color-shell-border);
  background: color-mix(in srgb, var(--color-shell-card-muted) 66%, transparent);
}

.profile-member__copy strong,
.profile-member__copy span {
  display: block;
}

.profile-member__copy strong {
  font-size: 0.78rem;
  font-weight: 600;
}

.profile-member__copy span {
  margin-top: 3px;
  color: var(--color-text-soft);
  font: 600 0.58rem/1 var(--font-mono);
  letter-spacing: 0.06em;
}

.profile-member__actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 6px;
}

.profile-member__role,
.profile-member__button {
  padding: 5px 8px;
  border-radius: 999px;
  font: 600 0.6rem/1 var(--font-mono);
  letter-spacing: 0.04em;
}

.profile-member__role {
  border: 1px solid var(--color-shell-border);
  background: color-mix(in srgb, var(--color-shell-action) 68%, transparent);
}

.profile-member__button {
  border: 1px solid var(--color-shell-border);
  background: transparent;
  color: var(--color-text-2);
  transition:
    border-color var(--motion-fast) ease,
    background var(--motion-fast) ease,
    color var(--motion-fast) ease;
}

.profile-member__button:hover,
.profile-member__button:focus-visible {
  border-color: var(--color-shell-border-strong);
  background: var(--color-shell-action-hover);
  color: var(--color-text-1);
}

.profile-member__button.is-danger {
  color: var(--color-danger);
}

.profile-member__button.is-danger:hover,
.profile-member__button.is-danger:focus-visible {
  border-color: color-mix(in srgb, var(--color-danger) 18%, var(--color-shell-border));
  background: color-mix(in srgb, var(--color-danger) 4%, var(--color-shell-action-hover));
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
