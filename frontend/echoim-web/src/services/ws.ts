import type {
  WsAckRequestPayload,
  WsEnvelope,
  WsMessageType,
  WsSendGroupPayload,
  WsSendSinglePayload,
} from '@/types/api'
import type { ConnectionStatus } from '@/types/chat'

const WS_BASE_URL = import.meta.env.VITE_WS_URL?.trim() ?? ''
const RECONNECT_DELAYS = [1000, 2000, 5000, 10000, 20000]
const HEARTBEAT_INTERVAL = 25000

interface ConnectOptions {
  wsUrl: string
  token: string
}

interface EchoWsClientOptions {
  onEnvelope: (message: WsEnvelope) => void
  onStatusChange: (status: ConnectionStatus) => void
  onReconnectReady: () => void | Promise<void>
}

function buildWsUrl(path: string) {
  if (WS_BASE_URL) return WS_BASE_URL

  if (path.startsWith('ws://') || path.startsWith('wss://')) return path

  const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
  const normalizedPath = path.startsWith('/') ? path : `/${path}`
  return `${protocol}//${window.location.host}${normalizedPath}`
}

export class EchoWsClient {
  private socket: WebSocket | null = null
  private heartbeatTimer: number | null = null
  private reconnectTimer: number | null = null
  private options: EchoWsClientOptions
  private connectOptions: ConnectOptions | null = null
  private reconnectIndex = 0
  private manualClose = false
  private reconnectPausedForTest = false
  private status: ConnectionStatus = 'disconnected'

  constructor(options: EchoWsClientOptions) {
    this.options = options
  }

  async connect(options: ConnectOptions) {
    this.connectOptions = {
      wsUrl: buildWsUrl(options.wsUrl),
      token: options.token,
    }
    this.manualClose = false

    if (this.socket && (this.socket.readyState === WebSocket.OPEN || this.socket.readyState === WebSocket.CONNECTING)) {
      return
    }

    await this.openSocket(false)
  }

  disconnect() {
    this.manualClose = true
    this.reconnectPausedForTest = false
    this.clearTimers()
    this.socket?.close()
    this.socket = null
    this.setStatus('disconnected')
  }

  getStatus() {
    return this.status
  }

  simulateConnectionLossForTest(options?: { pauseReconnect?: boolean }) {
    this.manualClose = false
    this.reconnectPausedForTest = Boolean(options?.pauseReconnect)
    this.clearTimers()
    this.socket?.close()
  }

  async reconnectNowForTest() {
    this.manualClose = false
    this.reconnectPausedForTest = false

    if (this.reconnectTimer) {
      window.clearTimeout(this.reconnectTimer)
      this.reconnectTimer = null
    }

    if (this.socket && (this.socket.readyState === WebSocket.OPEN || this.socket.readyState === WebSocket.CONNECTING)) {
      return
    }

    await this.openSocket(true)
  }

  sendSingleMessage(data: WsSendSinglePayload, clientMsgId: string) {
    this.send('CHAT_SINGLE', data, clientMsgId)
  }

  sendGroupMessage(data: WsSendGroupPayload, clientMsgId: string) {
    this.send('CHAT_GROUP', data, clientMsgId)
  }

  sendDeliveredAck(data: WsAckRequestPayload, clientMsgId: string) {
    this.send('ACK', data, clientMsgId)
  }

  sendRead(conversationId: number, lastReadSeq: number) {
    this.send('READ', { conversationId, lastReadSeq })
  }

  private async openSocket(isReconnect: boolean) {
    const options = this.connectOptions
    if (!options) return

    this.setStatus(isReconnect ? 'reconnecting' : 'connecting')

    await new Promise<void>((resolve, reject) => {
      const socket = new WebSocket(options.wsUrl)
      let settled = false

      const settleResolve = () => {
        if (settled) return
        settled = true
        resolve()
      }

      const settleReject = (error: Error) => {
        if (settled) return
        settled = true
        reject(error)
      }

      const handleOpen = () => {
        this.socket = socket
        this.send('AUTH', { token: options.token })
      }

      const handleMessage = async (event: MessageEvent) => {
        let message: WsEnvelope

        try {
          message = JSON.parse(event.data) as WsEnvelope
        } catch {
          return
        }

        if (message.type === 'AUTH') {
          const status = (message.data as { status?: string })?.status
        if (status === 'SUCCESS') {
          this.reconnectIndex = 0
          this.startHeartbeat()
          this.setStatus('ready')
          settleResolve()
          if (isReconnect) {
            await this.options.onReconnectReady()
          }
            return
          }

          settleReject(new Error('WebSocket 鉴权失败'))
          return
        }

        if (message.type === 'PONG') {
          this.options.onEnvelope(message)
          return
        }

        this.options.onEnvelope(message)
      }

      const handleClose = () => {
        this.socket = null
        this.stopHeartbeat()
        if (!settled) {
          settleReject(new Error('WebSocket 连接已关闭'))
          return
        }
        if (!this.manualClose) {
          this.scheduleReconnect()
        } else {
          this.setStatus('disconnected')
        }
      }

      const handleError = () => {
        if (!settled) {
          settleReject(new Error('WebSocket 连接失败'))
        }
      }

      socket.onopen = handleOpen
      socket.onmessage = handleMessage
      socket.onclose = handleClose
      socket.onerror = handleError
    }).catch((error) => {
      if (!this.manualClose) {
        this.scheduleReconnect()
      }
      throw error
    })
  }

  private scheduleReconnect() {
    if (this.reconnectTimer || this.manualClose || !this.connectOptions) return

    const delay = RECONNECT_DELAYS[Math.min(this.reconnectIndex, RECONNECT_DELAYS.length - 1)]
    this.reconnectIndex += 1
    this.setStatus('reconnecting')

    if (this.reconnectPausedForTest) {
      return
    }

    this.reconnectTimer = window.setTimeout(async () => {
      this.reconnectTimer = null
      try {
        await this.openSocket(true)
      } catch {
        // retry is handled by openSocket -> scheduleReconnect
      }
    }, delay)
  }

  private startHeartbeat() {
    this.stopHeartbeat()
    this.heartbeatTimer = window.setInterval(() => {
      this.send('PING', { at: Date.now() })
    }, HEARTBEAT_INTERVAL)
  }

  private stopHeartbeat() {
    if (this.heartbeatTimer) {
      window.clearInterval(this.heartbeatTimer)
      this.heartbeatTimer = null
    }
  }

  private clearTimers() {
    this.stopHeartbeat()

    if (this.reconnectTimer) {
      window.clearTimeout(this.reconnectTimer)
      this.reconnectTimer = null
    }
  }

  private send(type: WsMessageType, data: unknown, clientMsgId?: string) {
    if (!this.socket || this.socket.readyState !== WebSocket.OPEN) {
      throw new Error('实时连接尚未就绪')
    }

    const message: WsEnvelope = {
      type,
      traceId: `${Date.now()}-${Math.random().toString(16).slice(2, 10)}`,
      clientMsgId: clientMsgId ?? null,
      timestamp: Date.now(),
      data,
    }

    this.socket.send(JSON.stringify(message))
  }

  private setStatus(status: ConnectionStatus) {
    this.status = status
    this.options.onStatusChange(status)
  }
}
