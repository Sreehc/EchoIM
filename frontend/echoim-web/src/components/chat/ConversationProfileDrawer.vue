<script setup lang="ts">
import { Bell, Close } from '@element-plus/icons-vue'
import type { ConversationProfile, ConversationSummary } from '@/types/chat'
import AvatarBadge from './AvatarBadge.vue'
import ChatStatePanel from './ChatStatePanel.vue'

defineProps<{
  conversation: ConversationSummary | null
  profile: ConversationProfile | null
  loading?: boolean
  errorMessage?: string | null
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
    size="400px"
    @close="emit('update:visible', false)"
  >
    <div class="profile-panel">
      <div class="profile-panel__toolbar">
        <strong>会话详情</strong>
        <button class="profile-panel__close" type="button" aria-label="关闭详情" @click="emit('update:visible', false)">
          <Close />
        </button>
      </div>
      <el-scrollbar class="profile-panel__body">
        <div v-if="profile" class="profile-content" data-testid="conversation-profile">
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

          <section v-if="profile.signature" class="profile-block">
            <span class="profile-block__title">签名</span>
            <p>{{ profile.signature }}</p>
          </section>

          <section v-if="profile.notice" class="profile-block">
            <span class="profile-block__title">公告</span>
            <p>{{ profile.notice }}</p>
          </section>

          <section v-if="profile.fields.length" class="profile-block">
            <span class="profile-block__title">资料字段</span>
            <dl class="profile-fields">
              <div v-for="field in profile.fields" :key="field.key">
                <dt>{{ field.label }}</dt>
                <dd>{{ field.value }}</dd>
              </div>
            </dl>
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
          description="选择会话后，这里会展示单聊或群聊的资料卡片。"
        />
      </el-scrollbar>
    </div>
  </el-drawer>

  <aside v-else class="profile-panel">
    <div class="profile-panel__toolbar">
      <strong>会话详情</strong>
      <button class="profile-panel__close" type="button" aria-label="关闭详情" @click="emit('update:visible', false)">
        <Close />
      </button>
    </div>
    <el-scrollbar class="profile-panel__body">
      <div v-if="profile" class="profile-content" data-testid="conversation-profile">
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

        <section v-if="profile.signature" class="profile-block">
          <span class="profile-block__title">签名</span>
          <p>{{ profile.signature }}</p>
        </section>

        <section v-if="profile.notice" class="profile-block">
          <span class="profile-block__title">公告</span>
          <p>{{ profile.notice }}</p>
        </section>

        <section v-if="profile.fields.length" class="profile-block">
          <span class="profile-block__title">资料字段</span>
          <dl class="profile-fields">
            <div v-for="field in profile.fields" :key="field.key">
              <dt>{{ field.label }}</dt>
              <dd>{{ field.value }}</dd>
            </div>
          </dl>
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
        description="选择会话后，这里会展示单聊或群聊的资料卡片。"
      />
    </el-scrollbar>
  </aside>
</template>

<style scoped>
.profile-panel {
  height: 100%;
  min-height: 0;
  display: flex;
  flex-direction: column;
  padding: 18px;
  border-left: 1px solid var(--color-line);
  background: #212121;
  overflow: hidden;
}

.profile-panel__toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.profile-panel__toolbar strong {
  font-size: 0.92rem;
  font-weight: 700;
}

.profile-panel__body {
  min-height: 0;
}

.profile-panel__close {
  width: 36px;
  height: 36px;
  display: grid;
  place-items: center;
  border: 0;
  border-radius: 50%;
  background: transparent;
  color: var(--color-text-2);
}

.profile-panel__close:hover,
.profile-panel__close:focus-visible {
  background: var(--color-hover);
  color: var(--color-text-1);
}

.profile-content {
  display: grid;
  gap: 12px;
}

.profile-content__identity {
  padding: 18px;
  border: 1px solid var(--color-line);
  border-radius: 18px;
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

.profile-fields,
.profile-actions {
  display: grid;
  gap: 10px;
}

.profile-fields {
  margin: 0;
}

.profile-fields div,
.profile-actions li {
  padding: 11px;
  border: 1px solid var(--color-line);
  border-radius: 12px;
  background: var(--color-bg-elevated);
}

.profile-fields dt {
  color: var(--color-text-2);
  font-size: 0.78rem;
}

.profile-fields dd {
  margin: 4px 0 0;
  font-weight: 600;
}

.profile-actions strong {
  display: block;
  margin-bottom: 4px;
}

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
