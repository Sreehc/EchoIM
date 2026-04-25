<script setup lang="ts">
import { Bell, Close, CollectionTag, Files, UserFilled } from '@element-plus/icons-vue'
import type { ConversationProfile, ConversationSummary } from '@/types/chat'
import AvatarBadge from './AvatarBadge.vue'
import ChatStatePanel from './ChatStatePanel.vue'

defineProps<{
  conversation: ConversationSummary | null
  profile: ConversationProfile | null
  overlay: boolean
  visible: boolean
}>()

const emit = defineEmits<{
  'update:visible': [value: boolean]
  action: [command: 'toggle-top' | 'toggle-mute' | 'mark-read']
}>()

function resolveActionCommand(key: string) {
  if (key === 'mute') return 'toggle-mute'
  if (key === 'top') return 'toggle-top'
  return null
}
</script>

<template>
  <el-drawer
    v-if="overlay"
    :model-value="visible"
    :with-header="false"
    direction="rtl"
    size="360px"
    @close="emit('update:visible', false)"
  >
    <div class="profile-panel">
      <div class="profile-panel__toolbar">
        <strong>会话详情</strong>
        <button class="profile-panel__close" type="button" aria-label="关闭详情" @click="emit('update:visible', false)">
          <Close />
        </button>
      </div>
      <div v-if="profile" class="profile-content">
        <div class="profile-content__identity">
          <AvatarBadge
            class="profile-content__avatar"
            :name="conversation?.conversationName"
            :avatar-url="conversation?.avatarUrl"
            :type="conversation?.conversationType === 2 ? 'group' : 'user'"
            size="lg"
          />
          <strong>{{ conversation?.conversationName }}</strong>
          <p>{{ profile.subtitle }}</p>
        </div>

        <section class="profile-block">
          <span class="profile-block__title">签名</span>
          <p>{{ profile.signature }}</p>
        </section>

        <section v-if="profile.notice" class="profile-block">
          <span class="profile-block__title">公告</span>
          <p>{{ profile.notice }}</p>
        </section>

        <section class="profile-stats">
          <article>
            <Files />
            <strong>{{ profile.sharedFilesCount }}</strong>
            <span>共享文件</span>
          </article>
          <article>
            <CollectionTag />
            <strong>{{ profile.sharedMediaCount }}</strong>
            <span>共享媒体</span>
          </article>
        </section>

        <section v-if="profile.members?.length" class="profile-block">
          <span class="profile-block__title">成员摘要</span>
          <ul class="profile-members">
            <li v-for="member in profile.members" :key="member.id">
              <div>
                <strong>{{ member.name }}</strong>
                <span>{{ member.role }}</span>
              </div>
              <UserFilled />
            </li>
          </ul>
        </section>

        <section class="profile-block">
          <span class="profile-block__title">操作</span>
          <ul class="profile-actions">
            <li v-for="action in profile.actions" :key="action.key">
              <button
                class="profile-actions__button"
                type="button"
                :disabled="!resolveActionCommand(action.key)"
                @click="resolveActionCommand(action.key) && emit('action', resolveActionCommand(action.key)!)"
              >
                <div>
                  <strong>{{ action.label }}</strong>
                  <span>{{ action.value }}</span>
                </div>
                <Bell />
              </button>
            </li>
            <li>
              <button class="profile-actions__button" type="button" @click="emit('action', 'mark-read')">
                <div>
                  <strong>标记已读</strong>
                  <span>清空当前会话未读数</span>
                </div>
                <Bell />
              </button>
            </li>
          </ul>
        </section>
      </div>

      <ChatStatePanel
        v-else
        compact
        title="没有详情内容"
        description="选择会话后，这里会展示单聊或群聊的资料卡片。"
      />
    </div>
  </el-drawer>

  <aside v-else class="profile-panel">
    <div class="profile-panel__toolbar">
      <strong>会话详情</strong>
      <button class="profile-panel__close" type="button" aria-label="关闭详情" @click="emit('update:visible', false)">
        <Close />
      </button>
    </div>
    <div v-if="profile" class="profile-content">
      <div class="profile-content__identity">
        <AvatarBadge
          class="profile-content__avatar"
          :name="conversation?.conversationName"
          :avatar-url="conversation?.avatarUrl"
          :type="conversation?.conversationType === 2 ? 'group' : 'user'"
          size="lg"
        />
        <strong>{{ conversation?.conversationName }}</strong>
        <p>{{ profile.subtitle }}</p>
      </div>

      <section class="profile-block">
        <span class="profile-block__title">签名</span>
        <p>{{ profile.signature }}</p>
      </section>

      <section v-if="profile.notice" class="profile-block">
        <span class="profile-block__title">公告</span>
        <p>{{ profile.notice }}</p>
      </section>

      <section class="profile-stats">
        <article>
          <Files />
          <strong>{{ profile.sharedFilesCount }}</strong>
          <span>共享文件</span>
        </article>
        <article>
          <CollectionTag />
          <strong>{{ profile.sharedMediaCount }}</strong>
          <span>共享媒体</span>
        </article>
      </section>

      <section v-if="profile.members?.length" class="profile-block">
        <span class="profile-block__title">成员摘要</span>
        <ul class="profile-members">
          <li v-for="member in profile.members" :key="member.id">
            <div>
              <strong>{{ member.name }}</strong>
              <span>{{ member.role }}</span>
            </div>
            <UserFilled />
          </li>
        </ul>
      </section>

      <section class="profile-block">
        <span class="profile-block__title">操作</span>
        <ul class="profile-actions">
          <li v-for="action in profile.actions" :key="action.key">
            <button
              class="profile-actions__button"
              type="button"
              :disabled="!resolveActionCommand(action.key)"
              @click="resolveActionCommand(action.key) && emit('action', resolveActionCommand(action.key)!)"
            >
              <div>
                <strong>{{ action.label }}</strong>
                <span>{{ action.value }}</span>
              </div>
              <Bell />
            </button>
          </li>
          <li>
            <button class="profile-actions__button" type="button" @click="emit('action', 'mark-read')">
              <div>
                <strong>标记已读</strong>
                <span>清空当前会话未读数</span>
              </div>
              <Bell />
            </button>
          </li>
        </ul>
      </section>
    </div>

    <ChatStatePanel
      v-else
      compact
      title="没有详情内容"
      description="选择会话后，这里会展示单聊或群聊的资料卡片。"
    />
  </aside>
</template>

<style scoped>
.profile-panel {
  height: 100%;
  padding: 14px;
  border: 1px solid var(--color-line);
  border-radius: 16px;
  background: var(--color-bg-surface);
  box-shadow: var(--shadow-soft);
}

.profile-panel__toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.profile-panel__toolbar strong {
  font-size: 0.84rem;
  font-weight: 600;
}

.profile-panel__close {
  width: 30px;
  height: 30px;
  display: grid;
  place-items: center;
  border: 1px solid transparent;
  border-radius: 8px;
  background: transparent;
  color: var(--color-text-2);
}

.profile-panel__close:hover,
.profile-panel__close:focus-visible {
  border-color: var(--color-line);
  background: var(--color-hover);
  color: var(--color-text-1);
}

.profile-content {
  display: grid;
  gap: 12px;
}

.profile-content__identity {
  padding: 14px;
  border: 1px solid var(--color-line);
  border-radius: 14px;
  background: var(--color-bg-elevated);
}

.profile-content__avatar {
  margin-bottom: 12px;
}

.profile-content__identity strong {
  display: block;
  font: 700 0.92rem/1.1 var(--font-display);
}

.profile-content__identity p {
  margin-top: 5px;
  color: var(--color-text-2);
  font-size: 0.82rem;
}

.profile-block {
  display: grid;
  gap: 10px;
}

.profile-block__title {
  color: var(--color-text-soft);
  font: 600 0.68rem/1 var(--font-mono);
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.profile-stats {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.profile-stats article,
.profile-members li,
.profile-actions li {
  padding: 11px;
  border: 1px solid var(--color-line);
  border-radius: 12px;
  background: var(--color-bg-elevated);
}

.profile-stats article {
  display: grid;
  gap: 8px;
}

.profile-stats svg {
  width: 16px;
  height: 16px;
  color: var(--color-primary);
}

.profile-stats strong {
  font: 700 1.1rem/1 var(--font-display);
}

.profile-stats span {
  color: var(--color-text-2);
  font-size: 0.84rem;
}

.profile-members,
.profile-actions {
  display: grid;
  gap: 10px;
}

.profile-members li {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.profile-members strong,
.profile-actions strong {
  display: block;
  margin-bottom: 4px;
}

.profile-members span,
.profile-actions span {
  color: var(--color-text-2);
  font-size: 0.8rem;
}

.profile-actions__button {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 0;
  border: 0;
  background: transparent;
  color: inherit;
  text-align: left;
}

.profile-actions__button:disabled {
  cursor: default;
  opacity: 0.72;
}
</style>
