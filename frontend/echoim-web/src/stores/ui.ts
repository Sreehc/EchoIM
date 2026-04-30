import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import type {
  ChatPreferences,
  ConnectionStatus,
  ConversationFolder,
  LeftPanelMode,
  SettingsSection,
  ThemeMode,
} from '@/types/chat'
import { STORAGE_KEYS } from '@/utils/storage'

type MobileView = 'list' | 'chat'

export const useUiStore = defineStore('ui', () => {
  const theme = ref<ThemeMode>(readTheme())
  const chatPreferences = ref<ChatPreferences>(readChatPreferences())
  const viewportWidth = ref(window.innerWidth)
  const profileOpen = ref(false)
  const topbarMenuOpen = ref(false)
  const globalMenuOpen = ref(false)
  const mobileView = ref<MobileView>('list')
  const leftPanelMode = ref<LeftPanelMode>('conversations')
  const conversationFolder = ref<ConversationFolder>('inbox')
  const settingsSection = ref<SettingsSection>('appearance')
  const sidebarSearchFocusToken = ref(0)
  const panelScrollTop = ref<Record<LeftPanelMode, number>>({
    conversations: 0,
    contacts: 0,
    me: 0,
    settings: 0,
  })
  const connectionStatus = ref<ConnectionStatus>('disconnected')
  let initialized = false

  const isMobile = computed(() => viewportWidth.value <= 767)
  const isTablet = computed(() => viewportWidth.value >= 768 && viewportWidth.value <= 1279)
  const isDesktop = computed(() => viewportWidth.value >= 1280)
  const useOverlayProfile = computed(() => true)

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

  function setChatPreferences(nextValue: Partial<ChatPreferences>) {
    chatPreferences.value = {
      ...chatPreferences.value,
      ...nextValue,
    }
    localStorage.setItem(STORAGE_KEYS.chatPreferences, JSON.stringify(chatPreferences.value))
  }

  function setProfileOpen(value: boolean) {
    profileOpen.value = value
  }

  function setTopbarMenuOpen(value: boolean) {
    topbarMenuOpen.value = value
  }

  function setGlobalMenuOpen(value: boolean) {
    globalMenuOpen.value = value
  }

  function closeFloatingUi() {
    if (topbarMenuOpen.value) {
      topbarMenuOpen.value = false
      return true
    }

    if (globalMenuOpen.value) {
      globalMenuOpen.value = false
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

  function setLeftPanelMode(value: LeftPanelMode) {
    leftPanelMode.value = value
  }

  function setConversationFolder(value: ConversationFolder) {
    conversationFolder.value = value
  }

  function openLeftPanel(value: LeftPanelMode, nextSection?: SettingsSection) {
    leftPanelMode.value = value
    if (nextSection) {
      settingsSection.value = nextSection
    }
    if (isMobile.value) {
      mobileView.value = 'list'
    }
  }

  function returnToConversationList() {
    leftPanelMode.value = 'conversations'
  }

  function setSettingsSection(value: SettingsSection) {
    settingsSection.value = value
  }

  function requestSidebarSearchFocus() {
    sidebarSearchFocusToken.value += 1
  }

  function setPanelScrollTop(mode: LeftPanelMode, value: number) {
    panelScrollTop.value = {
      ...panelScrollTop.value,
      [mode]: value,
    }
  }

  function setConnectionStatus(value: ConnectionStatus) {
    connectionStatus.value = value
  }

  return {
    theme,
    chatPreferences,
    viewportWidth,
    profileOpen,
    topbarMenuOpen,
    mobileView,
    leftPanelMode,
    conversationFolder,
    settingsSection,
    globalMenuOpen,
    sidebarSearchFocusToken,
    panelScrollTop,
    connectionStatus,
    isMobile,
    isTablet,
    isDesktop,
    useOverlayProfile,
    initializeViewport,
    applyTheme,
    toggleTheme,
    setChatPreferences,
    setProfileOpen,
    setTopbarMenuOpen,
    setGlobalMenuOpen,
    closeFloatingUi,
    setMobileView,
    setLeftPanelMode,
    setConversationFolder,
    openLeftPanel,
    returnToConversationList,
    setSettingsSection,
    requestSidebarSearchFocus,
    setPanelScrollTop,
    setConnectionStatus,
  }
})

function readTheme(): ThemeMode {
  const stored = localStorage.getItem(STORAGE_KEYS.theme)
  return stored === 'dark' ? 'dark' : 'light'
}

function readChatPreferences(): ChatPreferences {
  const fallback: ChatPreferences = {
    enterToSend: true,
    compactList: false,
    compactBubbles: false,
  }

  const stored = localStorage.getItem(STORAGE_KEYS.chatPreferences)
  if (!stored) return fallback

  try {
    const parsed = JSON.parse(stored) as Partial<ChatPreferences>
    return {
      enterToSend: parsed.enterToSend ?? fallback.enterToSend,
      compactList: parsed.compactList ?? fallback.compactList,
      compactBubbles: parsed.compactBubbles ?? fallback.compactBubbles,
    }
  } catch {
    return fallback
  }
}
