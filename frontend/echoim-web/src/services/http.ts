import type { ApiResponse } from '@/types/api'

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL?.trim() ?? ''
const UNAUTHORIZED_CODES = new Set([401, 40100, 40101, 40102])

type TokenGetter = () => string | null
type UnauthorizedHandler = () => void

let getToken: TokenGetter = () => null
let handleUnauthorized: UnauthorizedHandler = () => undefined

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
}) {
  getToken = options.getToken
  handleUnauthorized = options.onUnauthorized
}

export async function getJson<T>(path: string, init?: RequestInit) {
  return requestJson<T>(path, { ...init, method: 'GET' })
}

export async function postJson<T>(path: string, body?: unknown, init?: RequestInit) {
  return requestJson<T>(path, {
    ...init,
    method: 'POST',
    body: body === undefined ? undefined : JSON.stringify(body),
  })
}

export async function putJson<T>(path: string, body?: unknown, init?: RequestInit) {
  return requestJson<T>(path, {
    ...init,
    method: 'PUT',
    body: body === undefined ? undefined : JSON.stringify(body),
  })
}

function resolveUrl(path: string) {
  if (!API_BASE_URL) return path
  if (path.startsWith('http://') || path.startsWith('https://')) return path
  return `${API_BASE_URL}${path}`
}

async function requestJson<T>(path: string, init: RequestInit): Promise<T> {
  const headers = new Headers(init.headers ?? {})
  const token = getToken()

  if (init.body && !headers.has('Content-Type')) {
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
    if (UNAUTHORIZED_CODES.has(code)) {
      handleUnauthorized()
    }

    throw new HttpError(message, {
      code,
      requestId: payload?.requestId ?? null,
    })
  }

  return payload.data
}
