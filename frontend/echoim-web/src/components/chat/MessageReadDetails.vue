<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { ElDialog, ElTabPane, ElTabs } from 'element-plus'
import AvatarBadge from './AvatarBadge.vue'
import type { MessageReadDetail } from '@/types/chat'

const props = defineProps<{
  visible: boolean
  detail: MessageReadDetail | null
  loading: boolean
}>()

const emit = defineEmits<{
  'update:visible': [value: boolean]
}>()

const activeTab = ref('read')

const dialogVisible = computed({
  get: () => props.visible,
  set: (val: boolean) => emit('update:visible', val),
})

watch(() => props.visible, (val) => {
  if (val) {
    activeTab.value = 'read'
  }
})

function formatReadTime(value: string | null): string {
  if (!value) return ''
  const date = new Date(value)
  const now = new Date()
  const sameDay = now.toDateString() === date.toDateString()
  const time = date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit', second: '2-digit' })
  if (sameDay) return time
  const day = date.toLocaleDateString('zh-CN', { month: '2-digit', day: '2-digit' })
  return `${day} ${time}`
}
</script>

<template>
  <el-dialog
    v-model="dialogVisible"
    title="消息已读详情"
    width="420px"
    :close-on-click-modal="true"
    class="read-details-dialog"
    append-to-body
  >
    <div v-if="loading" class="read-details__loading">
      <span>加载中...</span>
    </div>
    <div v-else-if="detail" class="read-details__content">
      <div class="read-details__summary">
        共 {{ detail.totalMembers }} 位成员，已读 {{ detail.readCount }} 人，未读 {{ detail.unreadCount }} 人
      </div>
      <el-tabs v-model="activeTab" class="read-details__tabs">
        <el-tab-pane :label="`已读 (${detail.readCount})`" name="read">
          <div class="read-details__list">
            <div
              v-for="item in detail.readList"
              :key="`read-${item.userId}`"
              class="read-details__item"
            >
              <AvatarBadge
                class="read-details__avatar"
                :name="item.nickname"
                :seed="item.userId"
                size="sm"
                type="user"
              />
              <div class="read-details__info">
                <span class="read-details__name">{{ item.nickname }}</span>
                <span class="read-details__time">{{ formatReadTime(item.readAt) }}</span>
              </div>
            </div>
            <div v-if="!detail.readList.length" class="read-details__empty">
              暂无已读成员
            </div>
          </div>
        </el-tab-pane>
        <el-tab-pane :label="`未读 (${detail.unreadCount})`" name="unread">
          <div class="read-details__list">
            <div
              v-for="item in detail.unreadList"
              :key="`unread-${item.userId}`"
              class="read-details__item"
            >
              <AvatarBadge
                class="read-details__avatar"
                :name="item.nickname"
                :seed="item.userId"
                size="sm"
                type="user"
              />
              <div class="read-details__info">
                <span class="read-details__name">{{ item.nickname }}</span>
              </div>
            </div>
            <div v-if="!detail.unreadList.length" class="read-details__empty">
              所有成员均已阅读
            </div>
          </div>
        </el-tab-pane>
      </el-tabs>
    </div>
  </el-dialog>
</template>

<style scoped>
.read-details__loading {
  display: flex;
  justify-content: center;
  padding: 24px 0;
  color: var(--text-tertiary);
  font-size: var(--text-sm);
}

.read-details__summary {
  margin-bottom: 12px;
  color: var(--text-secondary);
  font-size: var(--text-sm);
}

.read-details__tabs :deep(.el-tabs__header) {
  margin-bottom: 8px;
}

.read-details__list {
  max-height: 360px;
  overflow-y: auto;
}

.read-details__item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 4px;
  border-radius: 8px;
  transition: background-color 0.15s ease;
}

.read-details__item:hover {
  background: color-mix(in srgb, var(--interactive-selected-bg) 60%, transparent);
}

.read-details__avatar {
  flex-shrink: 0;
}

.read-details__info {
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.read-details__name {
  color: var(--text-primary);
  font-size: var(--text-sm);
  font-weight: 500;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.read-details__time {
  color: var(--text-quaternary);
  font-size: var(--text-xs);
  font-family: var(--font-mono);
}

.read-details__empty {
  display: flex;
  justify-content: center;
  padding: 24px 0;
  color: var(--text-quaternary);
  font-size: var(--text-sm);
}
</style>
