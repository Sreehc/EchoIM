import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import type { ConnectionStatus, ThemeMode } from '@/types/chat'
import { STORAGE_KEYS } from '@/utils/storage'

type MobileView = 'list' | 'chat'

export const useUiStore = defineStore('ui', () => {
  const theme = ref<ThemeMode>(readTheme())
  const viewportWidth = ref(window.innerWidth)
  const profileOpen = ref(false)
  const topbarMenuOpen = ref(false)
  const mobileView = ref<MobileView>('list')
  const sidebarSearchFocusToken = ref(0)
  const sidebarScrollTop = ref(0)
  const connectionStatus = ref<ConnectionStatus>('disconnected')
  let initialized = false

  const isMobile = computed(() => viewportWidth.value <= 767)
  const isTablet = computed(() => viewportWidth.value >= 768 && viewportWidth.value <= 1279)
  const isDesktop = computed(() => viewportWidth.value >= 1280)
  const useOverlayProfile = computed(() => !isDesktop.value)

  function initializeViewport() {
    if (initialized) return

    const handler = () => {
      viewportWidth.value = window.innerWidth
    }

    initialized = true
    handler()
    window.addEventListener('resize', handler)
  }

  function applyTheme(mode: ThemeMode) {
    theme.value = mode
    document.documentElement.dataset.theme = mode
    localStorage.setItem(STORAGE_KEYS.theme, mode)
  }

  function toggleTheme() {
    applyTheme(theme.value === 'light' ? 'dark' : 'light')
  }

  function setProfileOpen(value: boolean) {
    profileOpen.value = value
  }

  function setTopbarMenuOpen(value: boolean) {
    topbarMenuOpen.value = value
  }

  function closeFloatingUi() {
    if (topbarMenuOpen.value) {
      topbarMenuOpen.value = false
      return true
    }

    if (profileOpen.value) {
      profileOpen.value = false
      return true
    }

    return false
  }

  function setMobileView(value: MobileView) {
    mobileView.value = value
  }

  function requestSidebarSearchFocus() {
    sidebarSearchFocusToken.value += 1
  }

  function setSidebarScrollTop(value: number) {
    sidebarScrollTop.value = value
  }

  function setConnectionStatus(value: ConnectionStatus) {
    connectionStatus.value = value
  }

  return {
    theme,
    viewportWidth,
    profileOpen,
    topbarMenuOpen,
    mobileView,
    sidebarSearchFocusToken,
    sidebarScrollTop,
    connectionStatus,
    isMobile,
    isTablet,
    isDesktop,
    useOverlayProfile,
    initializeViewport,
    applyTheme,
    toggleTheme,
    setProfileOpen,
    setTopbarMenuOpen,
    closeFloatingUi,
    setMobileView,
    requestSidebarSearchFocus,
    setSidebarScrollTop,
    setConnectionStatus,
  }
})

function readTheme(): ThemeMode {
  const stored = localStorage.getItem(STORAGE_KEYS.theme)
  return stored === 'dark' ? 'dark' : 'light'
}
