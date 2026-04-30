import { expect, test } from '@playwright/test'
import { login, openConversation, openLeftPanel, waitForSingleConversation } from '../helpers/chat-app'

test('chat workspace keeps fixed viewport and supports sidebar mode switches', async ({ page }) => {
  await login(page, 'echo_demo_01', '123456')
  await expect(page.getByTestId('chat-empty-state')).toBeVisible()
  await expect(page.getByTestId('message-pane')).toHaveCount(0)
  await expect(page.getByLabel('消息输入框')).toHaveCount(0)
  expect(await page.evaluate(() => window.__ECHOIM_E2E__?.getActiveConversationId() ?? null)).toBeNull()

  const conversation = await waitForSingleConversation(page, 10002)
  await openConversation(page, conversation.conversationId)
  await expect(page.getByTestId('message-pane')).toBeVisible()
  await expect(page.getByLabel('消息输入框')).toBeVisible()

  const viewportState = await page.evaluate(() => ({
    docHeight: document.documentElement.scrollHeight,
    clientHeight: document.documentElement.clientHeight,
    bodyOverflow: getComputedStyle(document.body).overflow,
  }))

  expect(viewportState.bodyOverflow).toBe('hidden')
  expect(viewportState.docHeight).toBeLessThanOrEqual(viewportState.clientHeight + 1)

  await page.getByTestId('sidebar-open-menu').click()
  await expect(page.getByTestId('sidebar-global-menu')).toBeVisible()
  await expect(page.getByRole('menuitem', { name: /添加账号/ })).toBeVisible()
  await expect(page.getByRole('menuitem', { name: /收藏消息/ })).toBeVisible()
  await expect(page.getByRole('menuitem', { name: /通知与权限/ })).toBeVisible()
  await page.getByTestId('sidebar-menu-profile-settings').click()
  await expect(page.getByTestId('sidebar-panel-settings')).toBeVisible()

  await page.getByTestId('sidebar-back').click()
  await expect(page.getByTestId('conversation-list')).toBeVisible()

  await page.getByTestId('sidebar-open-compose').click()
  await expect(page.getByTestId('sidebar-compose-menu')).toBeVisible()
  await expect(page.getByRole('menuitem', { name: '新建频道' })).toBeVisible()
  await expect(page.getByRole('menuitem', { name: '新建群组' })).toBeVisible()
  await expect(page.getByRole('menuitem', { name: '新建私聊' })).toBeVisible()
  await page.getByRole('menuitem', { name: '新建私聊' }).click()
  await expect(page.getByRole('dialog', { name: '新建私聊' })).toBeVisible()
  await page.getByRole('button', { name: '取消' }).click()

  await openLeftPanel(page, 'me')
  await expect(page.getByTestId('sidebar-panel-me')).toBeVisible()

  await page.getByTestId('profile-edit').click()
  await expect(page.getByRole('dialog', { name: '编辑资料' })).toBeVisible()
  await page.getByTestId('profile-save').click()
  await expect(page.getByText('个人资料已更新')).toBeVisible()

  await page.getByText('进入设置').click()
  await expect(page.getByTestId('sidebar-panel-settings')).toBeVisible()
  await page.getByTestId('settings-tab-chat').click()
  await expect(page.getByText('Enter 直接发送')).toBeVisible()

  await page.getByRole('button', { name: '打开会话详情' }).click()
  await expect(page.getByText('会话详情')).toBeVisible()
  await expect(page.getByTestId('conversation-profile')).toBeVisible()
  await page.getByRole('button', { name: '关闭详情' }).click()

  await page.getByTestId('sidebar-back').click()
  await expect(page.getByTestId('conversation-list')).toBeVisible()

  await openLeftPanel(page, 'settings')
  await expect(page.getByTestId('sidebar-panel-settings')).toBeVisible()
  await page.getByTestId('settings-tab-security').click()
  await page.getByTestId('sidebar-logout').click()
  await expect(page).toHaveURL(/\/login$/)
})

test('desktop notification prompt requests browser permission when clicked', async ({ page }) => {
  await page.addInitScript(() => {
    class MockNotification {
      static permission: NotificationPermission = 'default'

      static async requestPermission() {
        ;(window as typeof window & { __notificationPermissionRequests?: number }).__notificationPermissionRequests =
          ((window as typeof window & { __notificationPermissionRequests?: number }).__notificationPermissionRequests ??
            0) + 1
        MockNotification.permission = 'granted'
        return MockNotification.permission
      }
    }

    Object.defineProperty(window, 'Notification', {
      configurable: true,
      value: MockNotification,
    })
  })

  await login(page, 'echo_demo_01', '123456')
  await openLeftPanel(page, 'settings')
  await page.getByTestId('settings-tab-notifications').click()
  await page.getByRole('button', { name: '开启桌面通知' }).click()

  expect(
    await page.evaluate(
      () => (window as typeof window & { __notificationPermissionRequests?: number }).__notificationPermissionRequests,
    ),
  ).toBe(1)
})
