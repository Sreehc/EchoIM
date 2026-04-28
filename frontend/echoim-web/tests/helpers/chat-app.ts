import { expect, type Page } from '@playwright/test'

interface E2EConversation {
  conversationId: number
  conversationName: string
  peerUserId: number | null
  groupId: number | null
  lastMessagePreview: string
  unreadCount: number
}

export async function login(page: Page, username: string, password: string) {
  await page.goto('/login')
  await page.getByLabel('用户名').fill(username)
  await page.getByLabel('密码').fill(password)
  await page.getByRole('button', { name: '登录' }).click()
  await expect(page).toHaveURL(/\/chat/)
  await page.waitForFunction(() => Boolean(window.__ECHOIM_E2E__))
  await page.waitForFunction(() => window.__ECHOIM_E2E__?.getConnectionStatus() === 'ready')
}

export async function listConversations(page: Page) {
  return page.evaluate(() => window.__ECHOIM_E2E__?.listConversations() ?? []) as Promise<E2EConversation[]>
}

export async function waitForSingleConversation(page: Page, peerUserId: number) {
  await page.waitForFunction(
    (targetPeerUserId) =>
      Boolean(window.__ECHOIM_E2E__?.listConversations().find((item) => item.peerUserId === targetPeerUserId)),
    peerUserId,
  )

  const conversations = await listConversations(page)
  const conversation = conversations.find((item) => item.peerUserId === peerUserId)
  if (!conversation) {
    throw new Error(`Missing conversation for peerUserId=${peerUserId}`)
  }

  return conversation
}

export async function openConversation(page: Page, conversationId: number) {
  await page.evaluate(async (targetConversationId) => {
    await window.__ECHOIM_E2E__?.openConversation(targetConversationId)
  }, conversationId)

  await page.waitForURL(new RegExp(`/chat/${conversationId}$`))
}

export async function openLeftPanel(page: Page, mode: 'conversations' | 'me' | 'settings') {
  await page.evaluate((targetMode) => {
    window.__ECHOIM_E2E__?.openLeftPanel(targetMode)
  }, mode)
}

export async function dropRealtimeConnection(page: Page, pauseReconnect = true) {
  await page.evaluate((shouldPauseReconnect) => {
    window.__ECHOIM_E2E__?.dropRealtimeConnection(shouldPauseReconnect)
  }, pauseReconnect)

  await page.waitForFunction(() => window.__ECHOIM_E2E__?.getConnectionStatus() === 'reconnecting')
}

export async function reconnectRealtime(page: Page) {
  await page.evaluate(async () => {
    await window.__ECHOIM_E2E__?.reconnectRealtime()
  })

  await page.waitForFunction(() => window.__ECHOIM_E2E__?.getConnectionStatus() === 'ready')
}

export async function sendMessage(page: Page, content: string) {
  await page.getByLabel('消息输入框').fill(content)
  await page.getByTestId('send-message').click()
}

export function statusForMessage(page: Page, messageText: string) {
  return page
    .locator('[data-testid^="message-row-"]')
    .filter({ hasText: messageText })
    .last()
    .getByTestId('message-status')
}
