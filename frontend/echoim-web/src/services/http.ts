import type { ApiResponse } from '@/types/api'
import { reportHttpError } from './errorReporter'

const UNAUTHORIZED_CODES = new Set([401, 40100, 40101, 40102])

type TokenGetter = () => string | null
type UnauthorizedHandler = () => void
type RefreshHandler = () => Promise<boolean>

export interface RequestJsonInit extends RequestInit {
  skipAuthRefresh?: boolean
}

let getToken: TokenGetter = () => null
let handleUnauthorized: UnauthorizedHandler = () => undefined
let refreshSession: RefreshHandler = async () => false
let refreshPromise: Promise<boolean> | null = null

export class HttpError extends Error {
  code?: number
  requestId?: string | null

  constructor(message: string, options?: { code?: number; requestId?: string | null }) {
    super(message)
    this.name = 'HttpError'
    this.code = options?.code
    this.requestId = options?.requestId
  }
}

export function configureHttpClient(options: {
  getToken: TokenGetter
  onUnauthorized: UnauthorizedHandler
  refreshSession: RefreshHandler
}) {
  getToken = options.getToken
  handleUnauthorized = options.onUnauthorized
  refreshSession = options.refreshSession
}

export async function getJson<T>(path: string, init?: RequestJsonInit) {
  return requestJson<T>(path, { ...init, method: 'GET' })
}

export async function postJson<T>(path: string, body?: unknown, init?: RequestJsonInit) {
  return requestJson<T>(path, {
    ...init,
    method: 'POST',
    body: body === undefined ? undefined : JSON.stringify(body),
  })
}

export async function putJson<T>(path: string, body?: unknown, init?: RequestJsonInit) {
  return requestJson<T>(path, {
    ...init,
    method: 'PUT',
    body: body === undefined ? undefined : JSON.stringify(body),
  })
}

export async function postForm<T>(path: string, body: FormData, init?: RequestJsonInit) {
  return requestJson<T>(path, {
    ...init,
    method: 'POST',
    body,
  })
}

export async function deleteJson<T>(path: string, init?: RequestJsonInit) {
  return requestJson<T>(path, { ...init, method: 'DELETE' })
}

function resolveUrl(path: string) {
  if (path.startsWith('http://') || path.startsWith('https://')) {
    throw new Error(`HTTP 请求必须使用同源相对路径，当前收到绝对地址：${path}`)
  }

  if (!path.startsWith('/')) {
    throw new Error(`HTTP 请求必须以 / 开头，当前收到：${path}`)
  }

  return path
}

export async function requestJson<T>(path: string, init: RequestJsonInit): Promise<T> {
  const headers = new Headers(init.headers ?? {})
  const token = getToken()

  if (init.body && !(init.body instanceof FormData) && !headers.has('Content-Type')) {
    headers.set('Content-Type', 'application/json')
  }

  if (token) {
    headers.set('Authorization', `Bearer ${token}`)
  }

  const response = await fetch(resolveUrl(path), {
    ...init,
    headers,
  })

  let payload: ApiResponse<T> | null = null

  try {
    payload = (await response.json()) as ApiResponse<T>
  } catch {
    payload = null
  }

  const code = payload?.code ?? response.status
  const message = payload?.message || response.statusText || '请求失败'

  if (!response.ok || !payload || payload.code !== 0) {
    if (UNAUTHORIZED_CODES.has(code) && !init.skipAuthRefresh) {
      const refreshed = await ensureRefreshedSession()
      if (refreshed) {
        return requestJson<T>(path, {
          ...init,
          skipAuthRefresh: true,
        })
      }

      handleUnauthorized()
    }

    reportHttpError(message, payload?.traceId)
    throw new HttpError(message, {
      code,
      requestId: payload?.requestId ?? null,
    })
  }

  return payload.data
}

async function ensureRefreshedSession() {
  if (!refreshPromise) {
    refreshPromise = refreshSession()
      .catch(() => false)
      .finally(() => {
        refreshPromise = null
      })
  }

  return refreshPromise
}
