import type { App } from 'vue'

interface ErrorReport {
  type: 'js' | 'promise' | 'vue' | 'http' | 'ws'
  message: string
  stack?: string
  url?: string
  line?: number
  column?: number
  component?: string
  traceId?: string
  timestamp: number
  userAgent: string
}

const MAX_QUEUE_SIZE = 20
const FLUSH_INTERVAL = 10000

let queue: ErrorReport[] = []
let flushTimer: ReturnType<typeof setInterval> | null = null
let initialized = false

function buildReport(type: ErrorReport['type'], message: string, error?: Error, extra?: Partial<ErrorReport>): ErrorReport {
  return {
    type,
    message: message.slice(0, 500),
    stack: error?.stack?.slice(0, 2000),
    url: window.location.href,
    timestamp: Date.now(),
    userAgent: navigator.userAgent,
    ...extra,
  }
}

function enqueue(report: ErrorReport) {
  if (queue.length >= MAX_QUEUE_SIZE) return
  queue.push(report)
}

async function flush() {
  if (queue.length === 0) return
  const batch = queue.splice(0)
  try {
    await fetch('/api/errors/report', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ errors: batch }),
      keepalive: true,
    }).catch(() => {
      // best-effort — silently discard on network failure
    })
  } catch {
    // prevent flush from crashing the app
  }
}

function startFlushTimer() {
  if (flushTimer) return
  flushTimer = setInterval(flush, FLUSH_INTERVAL)
}

export function initErrorReporting(app?: App) {
  if (initialized) return
  initialized = true

  // Global JS errors
  window.addEventListener('error', (event) => {
    enqueue(buildReport('js', event.message, undefined, {
      url: event.filename,
      line: event.lineno,
      column: event.colno,
    }))
  })

  // Unhandled promise rejections
  window.addEventListener('unhandledrejection', (event) => {
    const reason = event.reason
    const message = reason instanceof Error ? reason.message : String(reason)
    const error = reason instanceof Error ? reason : undefined
    enqueue(buildReport('promise', message, error))
  })

  // Vue errorHandler
  if (app) {
    app.config.errorHandler = (err, vm, info) => {
      const componentName = (vm as any)?.$options?.name || (vm as any)?.$?.type?.name || 'unknown'
      enqueue(buildReport('vue', String(err), err instanceof Error ? err : undefined, {
        component: componentName,
        info,
      } as any))
    }
  }

  startFlushTimer()

  // Flush on page unload
  window.addEventListener('visibilitychange', () => {
    if (document.visibilityState === 'hidden') {
      flush()
    }
  })
}

export function reportHttpError(message: string, traceId?: string) {
  enqueue(buildReport('http', message, undefined, { traceId }))
}

export function reportWsError(message: string, traceId?: string) {
  enqueue(buildReport('ws', message, undefined, { traceId }))
}
