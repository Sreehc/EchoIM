# Instructions

- Following Playwright test failed.
- Explain why, be concise, respect Playwright best practices.
- Provide a snippet of code with the fix, if possible.

# Test info

- Name: e2e/chat-realtime.spec.ts >> single chat ui flow and reconnect recovery
- Location: tests/e2e/chat-realtime.spec.ts:12:1

# Error details

```
Error: expect(page).toHaveURL(expected) failed

Expected pattern: /\/chat/
Received string:  "http://127.0.0.1:4173/login"
Timeout: 15000ms

Call log:
  - Expect "toHaveURL" with timeout 15000ms
    19 × unexpected value "http://127.0.0.1:4173/login"

```

# Test source

```ts
  1  | import { expect, type Page } from '@playwright/test'
  2  | 
  3  | interface E2EConversation {
  4  |   conversationId: number
  5  |   conversationName: string
  6  |   peerUserId: number | null
  7  |   groupId: number | null
  8  |   lastMessagePreview: string
  9  |   unreadCount: number
  10 | }
  11 | 
  12 | export async function login(page: Page, username: string, password: string) {
  13 |   await page.goto('/login')
  14 |   await page.getByLabel('用户名').fill(username)
  15 |   await page.getByLabel('密码').fill(password)
  16 |   await page.getByRole('button', { name: '登录' }).click()
> 17 |   await expect(page).toHaveURL(/\/chat/)
     |                      ^ Error: expect(page).toHaveURL(expected) failed
  18 |   await page.waitForFunction(() => Boolean(window.__ECHOIM_E2E__))
  19 |   await page.waitForFunction(() => window.__ECHOIM_E2E__?.getConnectionStatus() === 'ready')
  20 | }
  21 | 
  22 | export async function listConversations(page: Page) {
  23 |   return page.evaluate(() => window.__ECHOIM_E2E__?.listConversations() ?? []) as Promise<E2EConversation[]>
  24 | }
  25 | 
  26 | export async function waitForSingleConversation(page: Page, peerUserId: number) {
  27 |   await page.waitForFunction(
  28 |     (targetPeerUserId) =>
  29 |       Boolean(window.__ECHOIM_E2E__?.listConversations().find((item) => item.peerUserId === targetPeerUserId)),
  30 |     peerUserId,
  31 |   )
  32 | 
  33 |   const conversations = await listConversations(page)
  34 |   const conversation = conversations.find((item) => item.peerUserId === peerUserId)
  35 |   if (!conversation) {
  36 |     throw new Error(`Missing conversation for peerUserId=${peerUserId}`)
  37 |   }
  38 | 
  39 |   return conversation
  40 | }
  41 | 
  42 | export async function openConversation(page: Page, conversationId: number) {
  43 |   await page.evaluate(async (targetConversationId) => {
  44 |     await window.__ECHOIM_E2E__?.openConversation(targetConversationId)
  45 |   }, conversationId)
  46 | 
  47 |   await page.waitForURL(new RegExp(`/chat/${conversationId}$`))
  48 | }
  49 | 
  50 | export async function openLeftPanel(page: Page, mode: 'conversations' | 'me' | 'settings') {
  51 |   await page.evaluate((targetMode) => {
  52 |     window.__ECHOIM_E2E__?.openLeftPanel(targetMode)
  53 |   }, mode)
  54 | }
  55 | 
  56 | export async function dropRealtimeConnection(page: Page, pauseReconnect = true) {
  57 |   await page.evaluate((shouldPauseReconnect) => {
  58 |     window.__ECHOIM_E2E__?.dropRealtimeConnection(shouldPauseReconnect)
  59 |   }, pauseReconnect)
  60 | 
  61 |   await page.waitForFunction(() => window.__ECHOIM_E2E__?.getConnectionStatus() === 'reconnecting')
  62 | }
  63 | 
  64 | export async function reconnectRealtime(page: Page) {
  65 |   await page.evaluate(async () => {
  66 |     await window.__ECHOIM_E2E__?.reconnectRealtime()
  67 |   })
  68 | 
  69 |   await page.waitForFunction(() => window.__ECHOIM_E2E__?.getConnectionStatus() === 'ready')
  70 | }
  71 | 
  72 | export async function sendMessage(page: Page, content: string) {
  73 |   await page.getByLabel('消息输入框').fill(content)
  74 |   await page.getByTestId('send-message').click()
  75 | }
  76 | 
  77 | export function statusForMessage(page: Page, messageText: string) {
  78 |   return page
  79 |     .locator('[data-testid^="message-row-"]')
  80 |     .filter({ hasText: messageText })
  81 |     .last()
  82 |     .getByTestId('message-status')
  83 | }
  84 | 
```