<template>
  <el-drawer
    :model-value="open"
    title="系统公告"
    size="420px"
    :with-header="true"
    @close="emit('close')"
  >
    <template #header>
      <div class="notice-center__header">
        <div>
          <strong>系统公告</strong>
          <p>未读 {{ unreadCount }} 条</p>
        </div>
        <el-button text @click="emit('refresh')">刷新</el-button>
      </div>
    </template>

    <div v-if="loading" class="notice-center__state">
      <el-skeleton animated :rows="5" />
    </div>
    <div v-else-if="errorMessage" class="notice-center__state">
      <el-alert :title="errorMessage" type="warning" :closable="false" show-icon />
    </div>
    <div v-else-if="!notices.length" class="notice-center__state">
      <el-empty description="暂无系统公告" />
    </div>
    <div v-else class="notice-center__list">
      <button
        v-for="notice in notices"
        :key="notice.noticeId"
        class="notice-center__item"
        :class="{ 'is-unread': !notice.read, 'is-active': activeNoticeId === notice.noticeId }"
        type="button"
        @click="emit('open-notice', notice.noticeId)"
      >
        <div class="notice-center__meta">
          <span class="notice-center__title">{{ notice.title }}</span>
          <span class="notice-center__time">{{ formatTime(notice.publishedAt) }}</span>
        </div>
        <p class="notice-center__preview">{{ notice.content }}</p>
        <span v-if="!notice.read" class="notice-center__badge">未读</span>
      </button>
    </div>

    <div v-if="activeNotice" class="notice-center__detail">
      <div class="notice-center__detail-head">
        <div>
          <strong>{{ activeNotice.title }}</strong>
          <p>{{ formatTime(activeNotice.publishedAt) }}</p>
        </div>
        <el-tag :type="activeNotice.read ? 'info' : 'danger'" size="small">
          {{ activeNotice.read ? '已读' : '未读' }}
        </el-tag>
      </div>
      <article class="notice-center__detail-body">
        {{ activeNotice.content }}
      </article>
      <div class="notice-center__detail-actions">
        <el-button
          v-if="!activeNotice.read"
          type="primary"
          @click="emit('read-notice', activeNotice.noticeId)"
        >
          标记已读
        </el-button>
      </div>
    </div>
  </el-drawer>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { ApiUserNoticeItem } from '@/types/api'

const props = defineProps<{
  open: boolean
  notices: ApiUserNoticeItem[]
  unreadCount: number
  loading?: boolean
  errorMessage?: string | null
  activeNoticeId: number | null
}>()

const emit = defineEmits<{
  close: []
  refresh: []
  'open-notice': [noticeId: number]
  'read-notice': [noticeId: number]
}>()

const activeNotice = computed(
  () => props.notices.find((notice) => notice.noticeId === props.activeNoticeId) ?? null,
)

function formatTime(value: string) {
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value
  return `${date.getMonth() + 1}月${date.getDate()}日 ${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}`
}
</script>

<style scoped>
.notice-center__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  width: 100%;
}

.notice-center__header p,
.notice-center__detail-head p {
  margin: 4px 0 0;
  color: #6b7280;
  font-size: 12px;
}

.notice-center__state {
  padding: 8px 0 20px;
}

.notice-center__list {
  display: grid;
  gap: 10px;
}

.notice-center__item {
  position: relative;
  width: 100%;
  padding: 14px 16px;
  border: 1px solid rgba(15, 23, 42, 0.08);
  border-radius: 14px;
  background: #fff;
  text-align: left;
  transition: border-color 0.2s ease, box-shadow 0.2s ease, transform 0.2s ease;
}

.notice-center__item:hover,
.notice-center__item.is-active {
  border-color: rgba(37, 99, 235, 0.35);
  box-shadow: 0 10px 26px rgba(15, 23, 42, 0.08);
  transform: translateY(-1px);
}

.notice-center__item.is-unread {
  background: linear-gradient(180deg, rgba(239, 246, 255, 0.95), #fff);
}

.notice-center__meta {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: flex-start;
}

.notice-center__title {
  font-size: 14px;
  font-weight: 700;
  color: #111827;
}

.notice-center__time {
  flex: none;
  color: #6b7280;
  font-size: 12px;
}

.notice-center__preview {
  margin: 8px 0 0;
  color: #4b5563;
  font-size: 13px;
  line-height: 1.6;
  display: -webkit-box;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
  overflow: hidden;
}

.notice-center__badge {
  display: inline-flex;
  margin-top: 10px;
  padding: 3px 8px;
  border-radius: 999px;
  background: rgba(220, 38, 38, 0.12);
  color: #b91c1c;
  font-size: 12px;
  font-weight: 600;
}

.notice-center__detail {
  margin-top: 18px;
  padding-top: 18px;
  border-top: 1px solid rgba(15, 23, 42, 0.08);
}

.notice-center__detail-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.notice-center__detail-body {
  margin-top: 12px;
  color: #1f2937;
  font-size: 14px;
  line-height: 1.8;
  white-space: pre-wrap;
}

.notice-center__detail-actions {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
