import { expect, test } from '@playwright/test'

const API_BASE = process.env.VITE_API_BASE_URL?.trim() || 'http://127.0.0.1:8080'
const WS_URL = process.env.VITE_WS_URL?.trim() || 'ws://127.0.0.1:8091/ws'

type WsEnvelope = {
  type: string
  traceId?: string | null
  clientMsgId?: string | null
  timestamp?: number | null
  data: any
}

test('raw ws single chat covers delivered and read receipts', async () => {
  const tokenA = await login('echo_demo_01')
  const tokenB = await login('echo_demo_02')
  const messageText = `ws-${Date.now()}`

  const socketA = await connectWs(tokenA)
  const socketB = await connectWs(tokenB)

  try {
    const clientMsgId = `pw-ws-${Date.now()}`

    socketA.socket.send(
      JSON.stringify({
        type: 'CHAT_SINGLE',
        traceId: 'chat-trace-a',
        clientMsgId,
        timestamp: Date.now(),
        data: {
          conversationId: 30001,
          toUserId: 10002,
          msgType: 'TEXT',
          content: messageText,
          fileId: null,
        },
      }),
    )

    const incoming = await socketB.waitFor(
      (event) => event.type === 'CHAT_SINGLE' && event.data?.message?.content === messageText,
    )
    const senderAck = await socketA.waitFor(
      (event) => event.type === 'ACK' && event.data?.ackType === 'SEND' && event.clientMsgId === clientMsgId,
    )

    expect(senderAck.data.status).toBe('SUCCESS')
    expect(incoming.data.message.clientMsgId).toBe(clientMsgId)

    socketB.socket.send(
      JSON.stringify({
        type: 'ACK',
        traceId: 'ack-trace-b',
        clientMsgId,
        timestamp: Date.now(),
        data: {
          ackType: 'DELIVERED',
          conversationId: incoming.data.message.conversationId,
          messageId: incoming.data.message.messageId,
          seqNo: incoming.data.message.seqNo,
        },
      }),
    )

    const deliveredAck = await socketA.waitFor(
      (event) =>
        event.type === 'ACK' &&
        event.data?.ackType === 'DELIVERED' &&
        event.data?.messageId === incoming.data.message.messageId,
    )

    expect(deliveredAck.data.status).toBe('SUCCESS')

    socketB.socket.send(
      JSON.stringify({
        type: 'READ',
        traceId: 'read-trace-b',
        clientMsgId,
        timestamp: Date.now(),
        data: {
          conversationId: incoming.data.message.conversationId,
          lastReadSeq: incoming.data.message.seqNo,
        },
      }),
    )

    const readEvent = await socketA.waitFor(
      (event) =>
        event.type === 'READ' &&
        event.data?.conversationId === incoming.data.message.conversationId &&
        event.data?.lastReadSeq === incoming.data.message.seqNo,
    )

    expect(readEvent.data.status).toBe('SUCCESS')
  } finally {
    socketA.socket.close()
    socketB.socket.close()
  }
})

async function login(username: string) {
  const response = await fetch(`${API_BASE}/api/auth/login`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ username, password: '123456' }),
  })
  const payload = (await response.json()) as { data?: { token?: string } }

  if (!payload.data?.token) {
    throw new Error(`Failed to login ${username}`)
  }

  return payload.data.token
}

async function connectWs(token: string) {
  const events: WsEnvelope[] = []
  let socket: WebSocket

  await new Promise<void>((resolve, reject) => {
    socket = new WebSocket(WS_URL)
    socket.onopen = () => {
      socket.send(
        JSON.stringify({
          type: 'AUTH',
          traceId: `auth-${Date.now()}`,
          clientMsgId: null,
          timestamp: Date.now(),
          data: { token },
        }),
      )
    }
    socket.onmessage = (event) => {
      const message = JSON.parse(String(event.data)) as WsEnvelope
      events.push(message)

      if (message.type === 'AUTH' && message.data?.status === 'SUCCESS') {
        resolve()
      }
    }
    socket.onerror = () => reject(new Error('WebSocket 连接失败'))
  })

  return {
    socket: socket!,
    waitFor(predicate: (event: WsEnvelope) => boolean, timeoutMs = 15_000) {
      return waitForEvent(events, predicate, timeoutMs)
    },
  }
}

function waitForEvent(events: WsEnvelope[], predicate: (event: WsEnvelope) => boolean, timeoutMs: number) {
  const existing = events.find(predicate)
  if (existing) return Promise.resolve(existing)

  return new Promise<WsEnvelope>((resolve, reject) => {
    const startedAt = Date.now()
    const timer = setInterval(() => {
      const match = events.find(predicate)
      if (match) {
        clearInterval(timer)
        resolve(match)
        return
      }

      if (Date.now() - startedAt >= timeoutMs) {
        clearInterval(timer)
        reject(new Error('Timed out waiting for ws event'))
      }
    }, 100)
  })
}
