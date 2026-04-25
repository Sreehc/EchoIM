import AxeBuilder from '@axe-core/playwright'
import { expect, test } from '@playwright/test'
import { login, openConversation, waitForSingleConversation } from '../helpers/chat-app'

test('login page passes axe smoke check', async ({ page }) => {
  await page.goto('/login')

  const results = await new AxeBuilder({ page }).analyze()
  expect(results.violations).toEqual([])
})

test('chat home passes axe smoke check', async ({ page }) => {
  await login(page, 'echo_demo_01', '123456')
  const conversation = await waitForSingleConversation(page, 10002)
  await openConversation(page, conversation.conversationId)

  const results = await new AxeBuilder({ page }).analyze()
  expect(results.violations).toEqual([])
})
