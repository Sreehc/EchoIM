<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { Bell, Close } from '@element-plus/icons-vue'
import type { ConversationProfile, ConversationSummary } from '@/types/chat'
import { fetchConversationFiles, type ConversationFileItem } from '@/services/conversations'
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
  'manage-invites': []
  'manage-join-requests': []
  'mute-member': [payload: { userId: number; nickname: string }]
  'unmute-member': [userId: number]
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

// ── Shared files ──
const sharedFiles = ref<ConversationFileItem[]>([])
const filesLoading = ref(false)
const filesTotal = ref(0)
const filesPageNo = ref(1)
const filesPageSize = 20

async function loadFiles(page = 1) {
  const id = props.conversation?.conversationId
  if (!id) return
  filesLoading.value = true
  try {
    const res = await fetchConversationFiles(id, page, filesPageSize)
    if (res.code === 0 && res.data) {
      sharedFiles.value = res.data.list ?? []
      filesTotal.value = res.data.total ?? 0
      filesPageNo.value = page
    }
  } catch {
    // silently ignore — file list is non-critical
  } finally {
    filesLoading.value = false
  }
}

watch(() => props.conversation?.conversationId, (id) => {
  if (id) loadFiles()
  else { sharedFiles.value = []; filesTotal.value = 0 }
})

onMounted(() => {
  if (props.conversation?.conversationId) loadFiles()
})

function formatFileSize(bytes: number): string {
  if (bytes < 1024) return `${bytes} B`
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`
  if (bytes < 1024 * 1024 * 1024) return `${(bytes / (1024 * 1024)).toFixed(1)} MB`
  return `${(bytes / (1024 * 1024 * 1024)).toFixed(1)} GB`
}

function fileIcon(ext: string): string {
  const imageExts = ['jpg', 'jpeg', 'png', 'gif', 'webp', 'bmp', 'svg']
  const videoExts = ['mp4', 'mov', 'avi', 'mkv', 'webm']
  const audioExts = ['mp3', 'wav', 'ogg', 'aac', 'm4a']
  const archiveExts = ['zip', 'rar', '7z', 'tar', 'gz']
  const e = ext.toLowerCase()
  if (imageExts.includes(e)) return '🖼️'
  if (videoExts.includes(e)) return '🎬'
  if (audioExts.includes(e)) return '🎵'
  if (archiveExts.includes(e)) return '📦'
  if (e === 'pdf') return '📄'
  if (['doc', 'docx'].includes(e)) return '📝'
  if (['xls', 'xlsx'].includes(e)) return '📊'
  if (['ppt', 'pptx'].includes(e)) return '📎'
  return '📁'
}

function isMemberMuted(muteUntil: string | null | undefined): boolean {
  if (!muteUntil) return false
  return new Date(muteUntil) > new Date()
}

function muteRemainingText(muteUntil: string | null | undefined): string {
  if (!muteUntil) return ''
  const diff = new Date(muteUntil).getTime() - Date.now()
  if (diff <= 0) return ''
  const minutes = Math.ceil(diff / 60000)
  if (minutes < 60) return `${minutes}分钟`
  const hours = Math.ceil(minutes / 60)
  if (hours < 24) return `${hours}小时`
  return '永久'
}

function onMemberClick(member: { userId: number; nickname: string; role: number; muteUntil: string | null }) {
  if (member.role === 1) return // can't mute owner
  if (!profile.value?.group?.canManageMembers) return
  if (isMemberMuted(member.muteUntil)) {
    emit('unmute-member', member.userId)
  } else {
    emit('mute-member', { userId: member.userId, nickname: member.nickname })
  }
}

</script>

<template>
  <div class="profile-panel">
    <div class="profile-panel__toolbar">
      <strong>会话详情</strong>
      <button class="profile-panel__close" type="button" aria-label="关闭详情" @click="emit('close')">
        <Close />
      </button>
    </div>

    <el-scrollbar class="profile-panel__body">
      <div v-if="profile" class="profile-content" data-testid="conversation-profile">

        <!-- Hero: centered avatar + name -->
        <section class="profile-hero">
          <AvatarBadge
            class="profile-hero__avatar"
            :name="conversation?.conversationName"
            :avatar-url="conversation?.avatarUrl"
            :type="conversation?.conversationType === 1 ? 'user' : conversation?.conversationType === 2 ? 'group' : 'channel'"
            size="xl"
          />
          <strong class="profile-hero__name">{{ conversation?.conversationName }}</strong>
          <p class="profile-hero__sub">{{ profile.subtitle }}</p>
          <div class="profile-hero__tags">
            <span v-for="tag in spotlightTags" :key="tag">{{ tag }}</span>
          </div>
        </section>

        <!-- Members grid (group / channel) -->
        <section v-if="profile.members?.length" class="profile-section">
          <div class="profile-section__header">
            <span>成员</span>
            <small>{{ profile.members.length }} 人</small>
          </div>
          <div class="profile-members-grid">
            <div
              v-for="member in profile.members.slice(0, 30)"
              :key="member.userId"
              class="profile-members-grid__item"
              :class="{ 'is-clickable': member.role !== 1 && profile.group?.canManageMembers }"
              :title="member.role !== 1 && profile.group?.canManageMembers ? (isMemberMuted(member.muteUntil) ? '点击解除禁言' : '点击禁言') : ''"
              @click="onMemberClick(member)"
            >
              <AvatarBadge
                :name="member.nickname"
                :avatar-url="member.avatarUrl"
                size="md"
                type="user"
              />
              <span class="profile-members-grid__name">{{ member.nickname }}</span>
              <span v-if="member.role !== 2" class="profile-members-grid__role">
                {{ member.role === 1 ? '群主' : '管理' }}
              </span>
              <span v-if="isMemberMuted(member.muteUntil)" class="profile-members-grid__muted" :title="member.muteUntil ? `禁言至 ${member.muteUntil}` : '永久禁言'">
                禁言{{ muteRemainingText(member.muteUntil) ? ` ${muteRemainingText(member.muteUntil)}` : '' }}
              </span>
            </div>
          </div>
        </section>

        <!-- Signature -->
        <section v-if="profile.signature" class="profile-section">
          <div class="profile-section__header">
            <span>个性签名</span>
          </div>
          <p class="profile-section__text">{{ profile.signature }}</p>
        </section>

        <!-- Notice -->
        <section v-if="profile.notice" class="profile-section">
          <div class="profile-section__header">
            <span>{{ conversation?.conversationType === 3 ? '频道公告' : '群公告' }}</span>
          </div>
          <p class="profile-section__text profile-section__text--notice">{{ profile.notice }}</p>
        </section>

        <!-- Info fields -->
        <section v-if="profile.fields.length" class="profile-section">
          <div class="profile-section__header">
            <span>{{ conversation?.conversationType === 1 ? '个人信息' : '资料' }}</span>
          </div>
          <dl class="profile-fields">
            <div v-for="field in profile.fields" :key="field.key" class="profile-field">
              <dt>{{ field.label }}</dt>
              <dd>{{ field.value }}</dd>
            </div>
          </dl>
        </section>

        <!-- Shared files -->
        <section v-if="sharedFiles.length || filesLoading" class="profile-section">
          <div class="profile-section__header">
            <span>共享文件</span>
            <small v-if="filesTotal > 0">{{ filesTotal }} 个</small>
          </div>
          <div v-if="filesLoading && !sharedFiles.length" class="profile-files__loading">
            <span>加载中…</span>
          </div>
          <ul v-else class="profile-files">
            <li
              v-for="file in sharedFiles"
              :key="file.fileId"
              class="profile-files__item"
            >
              <span class="profile-files__icon">{{ fileIcon(file.fileExt) }}</span>
              <div class="profile-files__info">
                <a
                  :href="file.url"
                  target="_blank"
                  rel="noopener"
                  class="profile-files__name"
                  :title="file.fileName"
                >{{ file.fileName }}</a>
                <span class="profile-files__meta">{{ formatFileSize(file.fileSize) }}</span>
              </div>
            </li>
          </ul>
          <button
            v-if="filesTotal > sharedFiles.length"
            class="profile-files__more"
            type="button"
            @click="loadFiles(filesPageNo + 1)"
          >
            加载更多
          </button>
        </section>

        <!-- Public profile link -->
        <section v-if="profile.publicProfilePath" class="profile-section">
          <button class="profile-link-row" type="button" @click="emit('open-public-profile', profile.publicProfilePath)">
            <span>查看公开主页</span>
            <svg viewBox="0 0 16 16" fill="none"><path d="M6 4l4 4-4 4" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/></svg>
          </button>
        </section>

        <!-- Quick actions -->
        <section class="profile-section">
          <div class="profile-segment">
            <button
              class="profile-segment__btn"
              :class="{ 'is-active': conversation?.isMute }"
              type="button"
              @click="emit('action', 'toggle-mute')"
            >
              <Bell class="profile-segment__icon" />
              <span>免打扰</span>
            </button>
            <button
              class="profile-segment__btn"
              :class="{ 'is-active': conversation?.isTop }"
              type="button"
              @click="emit('action', 'toggle-top')"
            >
              <svg class="profile-segment__icon" viewBox="0 0 16 16" fill="none"><path d="M4 10L8 3L12 10" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/><path d="M5.5 8H10.5" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/></svg>
              <span>置顶</span>
            </button>
          </div>
        </section>

        <!-- Governance (group / channel) -->
        <section v-if="profile.group" class="profile-section">
          <div class="profile-section__header">
            <span>管理</span>
          </div>
          <div class="profile-rows">
            <button v-if="profile.group.canEditMeta" class="profile-row-action" type="button" @click="emit('update-group-meta')">
              <span>编辑资料</span>
              <svg viewBox="0 0 16 16" fill="none"><path d="M6 4l4 4-4 4" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/></svg>
            </button>
            <button v-if="profile.group.canManageMembers" class="profile-row-action" type="button" @click="emit('add-members')">
              <span>邀请成员</span>
              <svg viewBox="0 0 16 16" fill="none"><path d="M6 4l4 4-4 4" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/></svg>
            </button>
            <button v-if="profile.group.canManageMembers" class="profile-row-action" type="button" @click="emit('manage-invites')">
              <span>邀请链接</span>
              <svg viewBox="0 0 16 16" fill="none"><path d="M6 4l4 4-4 4" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/></svg>
            </button>
            <button v-if="profile.group.canManageMembers" class="profile-row-action" type="button" @click="emit('manage-join-requests')">
              <span>入群审批</span>
              <svg viewBox="0 0 16 16" fill="none"><path d="M6 4l4 4-4 4" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/></svg>
            </button>
            <button v-if="profile.group.canLeave" class="profile-row-action is-danger" type="button" @click="emit('leave-group')">
              <span>退出会话</span>
              <svg viewBox="0 0 16 16" fill="none"><path d="M6 4l4 4-4 4" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/></svg>
            </button>
            <button v-if="profile.group.canDissolve" class="profile-row-action is-danger" type="button" @click="emit('dissolve-group')">
              <span>解散会话</span>
              <svg viewBox="0 0 16 16" fill="none"><path d="M6 4l4 4-4 4" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/></svg>
            </button>
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
/* ── Panel shell ── */

.profile-panel {
  height: 100%;
  min-height: 0;
  display: flex;
  flex-direction: column;
  padding: 14px 16px;
  background:
    radial-gradient(circle at top right, color-mix(in srgb, var(--interactive-focus-ring) 28%, transparent), transparent 32%),
    var(--surface-card);
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
  font: 620 var(--text-base)/1.08 var(--font-display);
  letter-spacing: -0.02em;
}

.profile-panel__body {
  min-height: 0;
  overflow-x: hidden;
}

.profile-panel__close {
  width: 32px;
  height: 32px;
  display: grid;
  place-items: center;
  border: 0;
  border-radius: var(--radius-control);
  background: transparent;
  color: var(--text-tertiary);
  transition:
    background var(--motion-fast) var(--motion-ease-out),
    color var(--motion-fast) var(--motion-ease-out);
}

.profile-panel__close:hover,
.profile-panel__close:focus-visible {
  background: var(--interactive-secondary-bg-hover);
  color: var(--text-primary);
}

.profile-content {
  display: grid;
  gap: 14px;
  overflow-x: hidden;
}

/* ── Hero ── */

.profile-hero {
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  padding: 18px 14px 14px;
  border-radius: var(--radius-panel);
  border: 1px solid var(--border-default);
  background:
    linear-gradient(180deg, color-mix(in srgb, var(--surface-overlay) 60%, transparent), transparent 100%),
    var(--surface-card);
  overflow: hidden;
  position: relative;
}

.profile-hero::after {
  content: '';
  position: absolute;
  inset: auto -20% -30% auto;
  width: 140px;
  height: 140px;
  border-radius: 50%;
  background: radial-gradient(circle, color-mix(in srgb, var(--interactive-focus-ring) 36%, transparent), transparent 70%);
  pointer-events: none;
}

.profile-hero__avatar {
  margin-bottom: 10px;
  position: relative;
  z-index: 1;
}

.profile-hero__name {
  position: relative;
  z-index: 1;
  font: 620 var(--text-base)/1.12 var(--font-display);
  letter-spacing: -0.01em;
  color: var(--text-primary);
}

.profile-hero__sub {
  position: relative;
  z-index: 1;
  margin-top: 3px;
  color: var(--text-tertiary);
  font-size: var(--text-xs);
  line-height: 1.3;
}

.profile-hero__tags {
  position: relative;
  z-index: 1;
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 5px;
  margin-top: 10px;
}

.profile-hero__tags span {
  padding: 5px 10px;
  border-radius: var(--radius-pill);
  border: 1px solid var(--border-default);
  background: color-mix(in srgb, var(--interactive-secondary-bg) 72%, transparent);
  color: var(--text-secondary);
  font: 600 var(--text-2xs)/1 var(--font-mono);
  letter-spacing: 0.04em;
}

/* ── Sections ── */

.profile-section {
  padding: 0;
  min-width: 0;
  overflow: hidden;
}

.profile-section__header {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 8px;
  margin-bottom: 8px;
}

.profile-section__header span {
  color: var(--text-primary);
  font: 620 var(--text-xs)/1.08 var(--font-display);
  letter-spacing: -0.01em;
}

.profile-section__header small {
  color: var(--text-quaternary);
  font: 600 var(--text-2xs)/1 var(--font-mono);
  letter-spacing: 0.04em;
}

.profile-section__text {
  white-space: pre-wrap;
  color: var(--text-primary);
  font-size: var(--text-xs);
  line-height: 1.5;
  padding: 8px 10px;
  border-radius: var(--radius-md);
  border: 1px solid color-mix(in srgb, var(--border-default) 70%, transparent);
  background: color-mix(in srgb, var(--surface-panel) 60%, transparent);
}

.profile-section__text--notice {
  border-color: color-mix(in srgb, var(--interactive-focus-ring) 18%, var(--border-default));
  background:
    linear-gradient(180deg, color-mix(in srgb, var(--interactive-focus-ring) 8%, transparent), transparent 100%),
    color-mix(in srgb, var(--surface-panel) 60%, transparent);
}

/* ── Members grid ── */

.profile-members-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(64px, 1fr));
  gap: 10px 6px;
  padding: 2px 0 4px;
}

.profile-members-grid__item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  padding: 4px 2px;
  position: relative;
  min-width: 0;
}

.profile-members-grid__name {
  max-width: 100%;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
  color: var(--text-secondary);
  font-size: var(--text-2xs);
  line-height: 1.2;
  text-align: center;
}

.profile-members-grid__role {
  position: absolute;
  top: 2px;
  right: 0;
  padding: 2px 5px;
  border-radius: var(--radius-pill);
  background: color-mix(in srgb, var(--interactive-primary-bg) 88%, white);
  color: var(--text-on-brand);
  font: 600 9px/1 var(--font-mono);
  letter-spacing: 0.02em;
}

.profile-members-grid__muted {
  position: absolute;
  top: 2px;
  left: 0;
  padding: 2px 5px;
  border-radius: var(--radius-pill);
  background: color-mix(in srgb, var(--status-danger) 85%, white);
  color: white;
  font: 600 8px/1 var(--font-mono);
  letter-spacing: 0.02em;
  white-space: nowrap;
}

.profile-members-grid__item.is-clickable {
  cursor: pointer;
  border-radius: var(--radius-md);
  transition: background var(--motion-fast) var(--motion-ease-out);
}

.profile-members-grid__item.is-clickable:hover {
  background: var(--interactive-secondary-bg-hover);
}

/* ── Personal info fields ── */

.profile-fields {
  display: grid;
  gap: 0;
  margin: 0;
}

.profile-field {
  padding: 8px 0;
}

.profile-field + .profile-field {
  border-top: 1px solid color-mix(in srgb, var(--border-default) 50%, transparent);
}

.profile-field dt {
  margin-bottom: 3px;
  color: var(--text-tertiary);
  font-size: var(--text-xs);
  font-weight: 500;
  line-height: 1.2;
}

.profile-field dd {
  margin: 0;
  color: var(--text-primary);
  font-size: var(--text-sm);
  font-weight: 500;
  line-height: 1.4;
  word-break: break-all;
}

/* ── Link row ── */

.profile-link-row {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  padding: 8px 10px;
  border: 1px solid var(--border-default);
  border-radius: var(--radius-md);
  background: color-mix(in srgb, var(--surface-panel) 60%, transparent);
  color: var(--text-primary);
  font: 500 var(--text-xs)/1 var(--font-sans);
  cursor: pointer;
  transition:
    background var(--motion-fast) var(--motion-ease-out),
    border-color var(--motion-fast) var(--motion-ease-out);
}

.profile-link-row:hover {
  background: var(--interactive-secondary-bg-hover);
  border-color: var(--border-strong);
}

.profile-link-row svg {
  width: 12px;
  height: 12px;
  color: var(--text-quaternary);
}

/* ── Segmented control ── */

.profile-segment {
  display: flex;
  gap: 0;
  border: 1px solid var(--border-default);
  border-radius: var(--radius-md);
  overflow: hidden;
  background: color-mix(in srgb, var(--surface-panel) 60%, transparent);
}

.profile-segment__btn {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
  min-height: 34px;
  padding: 0 8px;
  border: 0;
  background: transparent;
  color: var(--text-tertiary);
  font: 500 var(--text-xs)/1 var(--font-sans);
  letter-spacing: -0.01em;
  cursor: pointer;
  transition:
    background var(--motion-fast) var(--motion-ease-out),
    color var(--motion-fast) var(--motion-ease-out);
}

.profile-segment__btn + .profile-segment__btn {
  border-left: 1px solid color-mix(in srgb, var(--border-default) 60%, transparent);
}

.profile-segment__btn:hover:not(:disabled) {
  background: var(--interactive-secondary-bg-hover);
  color: var(--text-primary);
}

.profile-segment__btn.is-active {
  background: color-mix(in srgb, var(--interactive-primary-bg) 12%, var(--surface-card));
  color: var(--interactive-primary-bg);
}

.profile-segment__btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.profile-segment__icon {
  width: 13px;
  height: 13px;
  flex-shrink: 0;
}

/* ── Row actions (governance) ── */

.profile-rows {
  display: grid;
  gap: 2px;
}

.profile-row-action {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  padding: 9px 10px;
  border: 0;
  border-radius: var(--radius-md);
  background: transparent;
  color: var(--text-secondary);
  font: 500 var(--text-xs)/1 var(--font-sans);
  cursor: pointer;
  transition:
    background var(--motion-fast) var(--motion-ease-out),
    color var(--motion-fast) var(--motion-ease-out);
}

.profile-row-action:hover,
.profile-row-action:focus-visible {
  background: var(--interactive-secondary-bg-hover);
  color: var(--text-primary);
}

.profile-row-action svg {
  width: 14px;
  height: 14px;
  color: var(--text-quaternary);
  flex-shrink: 0;
}

.profile-row-action.is-danger {
  color: var(--status-danger);
}

.profile-row-action.is-danger:hover {
  background: color-mix(in srgb, var(--status-danger) 6%, transparent);
  color: var(--status-danger);
}

.profile-row-action.is-danger svg {
  color: var(--status-danger);
}

/* ── Shared files ── */

.profile-files {
  list-style: none;
  margin: 0;
  padding: 0;
  display: grid;
  gap: 2px;
}

.profile-files__item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 7px 8px;
  border-radius: var(--radius-md);
  transition: background var(--motion-fast) var(--motion-ease-out);
}

.profile-files__item:hover {
  background: var(--interactive-secondary-bg-hover);
}

.profile-files__icon {
  flex-shrink: 0;
  font-size: 16px;
  line-height: 1;
  width: 20px;
  text-align: center;
}

.profile-files__info {
  min-width: 0;
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.profile-files__name {
  color: var(--text-primary);
  font-size: var(--text-xs);
  font-weight: 500;
  line-height: 1.3;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
  text-decoration: none;
}

.profile-files__name:hover {
  color: var(--interactive-primary-bg);
  text-decoration: underline;
}

.profile-files__meta {
  color: var(--text-quaternary);
  font: 500 var(--text-2xs)/1 var(--font-mono);
  letter-spacing: 0.02em;
}

.profile-files__loading {
  padding: 12px 0;
  text-align: center;
  color: var(--text-tertiary);
  font-size: var(--text-xs);
}

.profile-files__more {
  width: 100%;
  margin-top: 6px;
  padding: 7px 0;
  border: 0;
  border-radius: var(--radius-md);
  background: transparent;
  color: var(--interactive-primary-bg);
  font: 500 var(--text-xs)/1 var(--font-sans);
  cursor: pointer;
  transition: background var(--motion-fast) var(--motion-ease-out);
}

.profile-files__more:hover {
  background: color-mix(in srgb, var(--interactive-primary-bg) 8%, transparent);
}

/* ── Mobile ── */

@media (max-width: 767px) {
  .profile-panel {
    padding: 10px 12px;
  }

  .profile-hero {
    padding: 14px 12px 12px;
  }

  .profile-segment__btn {
    min-height: 32px;
    padding: 0 6px;
    font-size: var(--text-2xs);
    gap: 3px;
  }

  .profile-segment__icon {
    width: 12px;
    height: 12px;
  }

  .profile-field dd {
    font-size: var(--text-xs);
  }

  .profile-members-grid {
    grid-template-columns: repeat(auto-fill, minmax(52px, 1fr));
    gap: 6px 4px;
  }
}
</style>
