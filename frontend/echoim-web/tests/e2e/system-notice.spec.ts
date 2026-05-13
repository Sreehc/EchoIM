import { expect, test } from '@playwright/test'
import { login } from '../helpers/chat-app'

const API_ORIGIN = process.env.PLAYWRIGHT_API_ORIGIN?.trim() || 'http://127.0.0.1:8080'

test('system notice reaches user and can be marked as read', async ({ page }) => {
  const title = `系统公告-${Date.now()}`
  const content = '这是公告冒烟测试，请打开并标记已读。'

  await login(page, 'echo_demo_01', '123456')

  await createSystemNotice(title, content)

  await expect(page.getByText(`收到新公告：${title}`)).toBeVisible({ timeout: 15000 })
  await expect(page.getByLabel('打开系统公告')).toContainText(/\d+/)

  await page.getByLabel('打开系统公告').click()
  await expect(page.getByRole('dialog', { name: '系统公告' })).toBeVisible()
  await expect(page.getByText(title, { exact: true })).toBeVisible()
  await page.getByRole('button', { name: '标记已读', exact: true }).click()
  await expect(page.getByText('已读', { exact: true })).toBeVisible()
})

async function createSystemNotice(title: string, content: string) {
  const loginResponse = await fetch(`${API_ORIGIN}/api/admin/auth/login`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ username: 'admin', password: 'EchoIM@Admin2026!' }),
  })
  const loginPayload = (await loginResponse.json()) as { data?: { token?: string } }
  const token = loginPayload.data?.token
  if (!token) {
    throw new Error('Failed to login admin user for notice smoke test')
  }

  const createResponse = await fetch(`${API_ORIGIN}/api/admin/notices`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      Authorization: `Bearer ${token}`,
    },
    body: JSON.stringify({
      title,
      content,
      noticeType: 1,
    }),
  })
  const payload = (await createResponse.json()) as { code?: number }
  if (payload.code !== 0) {
    throw new Error(`Failed to create system notice: ${JSON.stringify(payload)}`)
  }
}
