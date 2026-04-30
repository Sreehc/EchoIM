import { expect, test } from '@playwright/test'
import {
  dropRealtimeConnection,
  editMessage,
  findMessageIdByText,
  listConversations,
  listMessages,
  login,
  openConversation,
  recallMessage,
  reconnectRealtime,
  refreshConversationMessages,
  sendMessage,
  statusForMessage,
  waitForSingleConversation,
} from '../helpers/chat-app'

test('single chat ui flow and reconnect recovery', async ({ browser }) => {
  test.setTimeout(180_000)
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
    await pageB.waitForFunction(
      (conversationId) =>
        Boolean(window.__ECHOIM_E2E__?.listConversations().find((item) => item.conversationId === conversationId)),
      conversationA.conversationId,
    )
    const conversationB =
      (await listConversations(pageB)).find((item) => item.conversationId === conversationA.conversationId) ??
      await waitForSingleConversation(pageB, 10001)

    await openConversation(pageA, conversationA.conversationId)
    await sendMessage(pageA, messageA)
    await expect
      .poll(async () => {
        const messages = await listMessages(pageA, conversationA.conversationId)
        return messages.filter((item) => item.content === messageA).length
      }, { timeout: 15000 })
      .toBe(1)

    await openConversation(pageB, conversationB.conversationId)
    await expect
      .poll(async () => {
        await refreshConversationMessages(pageB, conversationB.conversationId)
        const message = (await listMessages(pageB, conversationB.conversationId)).find((item) => item.content === messageA)
        return message?.messageId ?? -1
      }, { timeout: 60000 })
      .toBeGreaterThan(0)
    await expect
      .poll(async () => {
        await refreshConversationMessages(pageB, conversationB.conversationId)
        const messages = await listMessages(pageB, conversationB.conversationId)
        return messages.filter((item) => item.content === messageA).length
      }, { timeout: 60000 })
      .toBe(1)
    await expect(pageB.getByTestId('message-pane').getByText(messageA, { exact: true })).toBeVisible()
    await expect(statusForMessage(pageA, messageA)).toHaveText('✓✓')
    await expect(statusForMessage(pageA, messageA)).toHaveAttribute('aria-label', '已读')

    await expect
      .poll(async () => {
        await refreshConversationMessages(pageA, conversationA.conversationId)
        return (await findMessageIdByText(pageA, conversationA.conversationId, messageA)) ?? -1
      }, { timeout: 60000 })
      .toBeGreaterThan(0)

    const resolvedMessageId = await findMessageIdByText(pageA, conversationA.conversationId, messageA)
    expect(resolvedMessageId).not.toBeNull()
    const messageRowA = pageA.locator(`[data-search-message-id="${resolvedMessageId!}"]`)
    const messageRowB = pageB.locator(`[data-search-message-id="${resolvedMessageId!}"]`)
    await editMessage(pageA, resolvedMessageId!, editedMessageA)

    await expect(messageRowA.getByText(editedMessageA, { exact: true })).toBeVisible()
    await expect(messageRowB.getByText(editedMessageA, { exact: true })).toBeVisible()
    await expect(messageRowA.getByText('已编辑', { exact: true })).toBeVisible()

    await recallMessage(pageA, resolvedMessageId!)

    await expect
      .poll(async () => {
        const message = (await listMessages(pageB, conversationB.conversationId)).find((item) => item.messageId === resolvedMessageId)
        return JSON.stringify(message ?? null)
      })
      .toContain('"recalled":true')
    await expect
      .poll(async () => {
        const conversation = (await pageB.evaluate(
          (conversationId) =>
            window.__ECHOIM_E2E__?.listConversations().find((item) => item.conversationId === conversationId) ?? null,
          conversationB.conversationId,
        )) as { lastMessagePreview?: string } | null
        return conversation?.lastMessagePreview ?? ''
      })
      .toContain('撤回了一条消息')

    await expect(messageRowA).toContainText('撤回了一条消息')

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
