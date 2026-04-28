<script setup lang="ts">
import { ArrowLeft, MoreFilled, Search, Setting } from '@element-plus/icons-vue'
import type { ConversationProfile, ConversationSummary } from '@/types/chat'
import AvatarBadge from './AvatarBadge.vue'

const props = defineProps<{
  conversation: ConversationSummary | null
  profile: ConversationProfile | null
  isMobile: boolean
  menuOpen: boolean
}>()

const emit = defineEmits<{
  back: []
  openProfile: []
  focusSearch: []
  action: [command: 'toggle-top' | 'toggle-mute' | 'mark-read' | 'delete']
  'update:menuOpen': [value: boolean]
}>()
</script>

<template>
  <header class="chat-topbar">
    <div class="chat-topbar__main">
      <button v-if="isMobile" class="chat-topbar__icon" type="button" aria-label="返回会话列表" @click="emit('back')">
        <ArrowLeft />
      </button>
      <div class="chat-topbar__identity">
        <AvatarBadge
          class="chat-topbar__avatar"
          :name="conversation?.conversationName"
          :avatar-url="conversation?.avatarUrl"
          :type="conversation?.conversationType === 2 ? 'group' : 'user'"
          size="md"
        />
        <div class="chat-topbar__copy">
          <div class="chat-topbar__title-row">
            <strong>{{ conversation?.conversationName ?? '请选择会话' }}</strong>
          </div>
          <p>{{ profile?.subtitle ?? '消息将保持同步' }}</p>
        </div>
      </div>
    </div>

    <div class="chat-topbar__actions">
      <button class="chat-topbar__icon" type="button" aria-label="搜索会话" @click="emit('focusSearch')">
        <Search />
      </button>
      <button class="chat-topbar__icon" type="button" aria-label="打开会话详情" @click="emit('openProfile')">
        <Setting />
      </button>
      <el-dropdown
        :hide-on-click="true"
        trigger="click"
        :teleported="false"
        :visible="menuOpen"
        @visible-change="emit('update:menuOpen', $event)"
        @command="emit('action', $event)"
      >
        <button class="chat-topbar__icon" type="button" aria-label="更多操作">
          <MoreFilled />
        </button>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item command="toggle-top">
              {{ props.conversation?.isTop ? '取消置顶' : '会话置顶' }}
            </el-dropdown-item>
            <el-dropdown-item command="toggle-mute">
              {{ props.conversation?.isMute ? '关闭免打扰' : '消息免打扰' }}
            </el-dropdown-item>
            <el-dropdown-item command="mark-read" :disabled="!props.conversation?.unreadCount">
              标记已读
            </el-dropdown-item>
            <el-dropdown-item command="delete" divided>
              删除会话
            </el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </div>
  </header>
</template>

<style scoped>
.chat-topbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  min-height: 58px;
  padding: 8px 18px;
  border-bottom: 1px solid var(--color-line);
  background: #212121;
}

.chat-topbar__main,
.chat-topbar__identity,
.chat-topbar__actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.chat-topbar__avatar {
  flex-shrink: 0;
}

.chat-topbar__icon {
  width: 36px;
  height: 36px;
}

.chat-topbar__identity {
  min-width: 0;
}

.chat-topbar__copy {
  min-width: 0;
}

.chat-topbar__title-row {
  display: flex;
  align-items: center;
  gap: 6px;
}

.chat-topbar__identity strong {
  display: block;
  font-size: 0.98rem;
  line-height: 1.15;
  font-weight: 700;
}

.chat-topbar__identity p {
  margin-top: 3px;
  color: var(--color-text-soft);
  font-size: 0.8rem;
  line-height: 1.1;
}

.chat-topbar__icon {
  display: grid;
  place-items: center;
  border-radius: 50%;
  border: 0;
  background: transparent;
  color: var(--color-text-2);
  transition:
    background var(--motion-fast) ease,
    color var(--motion-fast) ease,
    transform var(--motion-fast) ease;
}

.chat-topbar__icon:hover,
.chat-topbar__icon:focus-visible {
  background: var(--color-hover);
  color: var(--color-text-1);
  transform: translateY(-1px);
}

@media (max-width: 767px) {
  .chat-topbar {
    padding-inline: 16px;
  }
}
</style>
