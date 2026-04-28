import { expect, test } from '@playwright/test'
import {
  dropRealtimeConnection,
  login,
  openConversation,
  reconnectRealtime,
  sendMessage,
  statusForMessage,
  waitForSingleConversation,
} from '../helpers/chat-app'

test('single chat ui flow and reconnect recovery', async ({ browser }) => {
  const contextA = await browser.newContext()
  const contextB = await browser.newContext()
  const pageA = await contextA.newPage()
  const pageB = await contextB.newPage()

  const messageA = `pw-${Date.now()}-a`
  const editedMessageA = `${messageA}-edited`
  const messageB = `pw-${Date.now()}-b`

  try {
    await login(pageA, 'echo_demo_01', '123456')
    await login(pageB, 'echo_demo_02', '123456')

    const conversationA = await waitForSingleConversation(pageA, 10002)
    const conversationB = await waitForSingleConversation(pageB, 10001)

    await openConversation(pageA, conversationA.conversationId)
    await sendMessage(pageA, messageA)

    await pageB.waitForFunction(
      ([conversationId, expectedPreview]) => {
        const conversation = window.__ECHOIM_E2E__
          ?.listConversations()
          .find((item) => item.conversationId === conversationId)
        return Boolean(
          conversation && conversation.lastMessagePreview.includes(expectedPreview) && conversation.unreadCount >= 1,
        )
      },
      [conversationB.conversationId, messageA],
    )

    await openConversation(pageB, conversationB.conversationId)
    await expect(pageB.getByTestId('message-pane').getByText(messageA, { exact: true })).toBeVisible()
    await expect(statusForMessage(pageA, messageA)).toHaveText('✓✓')
    await expect(statusForMessage(pageA, messageA)).toHaveAttribute('aria-label', '已读')

    const messageRowA = pageA.locator('[data-testid^="message-row-"]').filter({ hasText: messageA }).last()
    await messageRowA.getByTestId('message-start-edit').click()
    await messageRowA.getByRole('textbox').fill(editedMessageA)
    await messageRowA.getByTestId('message-save-edit').click()

    await expect(pageA.getByTestId('message-pane').getByText(editedMessageA, { exact: true })).toBeVisible()
    await expect(pageB.getByTestId('message-pane').getByText(editedMessageA, { exact: true })).toBeVisible()
    await expect(pageA.getByText('已编辑')).toBeVisible()

    await pageA.locator('[data-testid^="message-row-"]').filter({ hasText: editedMessageA }).last().getByTestId('message-recall').click()
    await pageA.getByRole('button', { name: '撤回' }).click()

    await expect(pageA.getByTestId('message-pane').getByText('撤回了一条消息')).toBeVisible()
    await expect(pageB.getByTestId('message-pane').getByText('撤回了一条消息')).toBeVisible()

    await dropRealtimeConnection(pageA, true)
    await sendMessage(pageB, messageB)
    await reconnectRealtime(pageA)

    await expect(pageA.getByTestId('message-pane').getByText(messageB, { exact: true })).toBeVisible()
    await expect(statusForMessage(pageB, messageB)).toHaveText('✓✓')
  } finally {
    await contextA.close()
    await contextB.close()
  }
})
