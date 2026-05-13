import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import { fetchNotices, markNoticeRead } from '@/services/notices'
import type { ApiUserNoticeItem, WsSystemNoticePayload } from '@/types/api'

export const useNoticeStore = defineStore('notices', () => {
  const items = ref<ApiUserNoticeItem[]>([])
  const loading = ref(false)
  const error = ref<string | null>(null)
  const unreadCount = ref(0)
  const bannerMessage = ref<string | null>(null)
  const initialized = ref(false)

  const hasUnread = computed(() => unreadCount.value > 0)

  async function bootstrap(force = false) {
    if (initialized.value && !force) return
    await refresh()
  }

  async function refresh() {
    loading.value = true
    error.value = null
    try {
      const response = await fetchNotices()
      items.value = response.list
      unreadCount.value = response.unreadCount
      initialized.value = true
    } catch (err) {
      error.value = err instanceof Error ? err.message : '公告加载失败'
      throw err
    } finally {
      loading.value = false
    }
  }

  async function readNotice(noticeId: number) {
    const current = items.value.find((item) => item.noticeId === noticeId)
    if (!current || current.read) {
      return
    }

    await markNoticeRead(noticeId)
    items.value = items.value.map((item) =>
      item.noticeId === noticeId
        ? {
            ...item,
            read: true,
          }
        : item,
    )
    unreadCount.value = Math.max(0, unreadCount.value - 1)
  }

  function handleSystemNotice(payload: WsSystemNoticePayload) {
    bannerMessage.value = `收到新公告：${payload.title}`
    void refresh().catch(() => undefined)
  }

  function clearBannerMessage() {
    bannerMessage.value = null
  }

  function resetState() {
    items.value = []
    loading.value = false
    error.value = null
    unreadCount.value = 0
    bannerMessage.value = null
    initialized.value = false
  }

  return {
    items,
    loading,
    error,
    unreadCount,
    hasUnread,
    bannerMessage,
    bootstrap,
    refresh,
    readNotice,
    handleSystemNotice,
    clearBannerMessage,
    resetState,
  }
})
