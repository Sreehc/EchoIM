import { STORAGE_KEYS } from './storage'

function randomId() {
  return globalThis.crypto?.randomUUID?.() ?? `${Date.now()}-${Math.random().toString(36).slice(2, 10)}`
}

export function getDeviceFingerprint() {
  const existing = localStorage.getItem(STORAGE_KEYS.deviceFingerprint)
  if (existing) return existing

  const created = randomId()
  localStorage.setItem(STORAGE_KEYS.deviceFingerprint, created)
  return created
}

export function getDeviceName() {
  const userAgent = navigator.userAgent
  const platform = navigator.platform || 'Browser'

  let browser = 'Browser'
  if (/Edg\//.test(userAgent)) browser = 'Edge'
  else if (/Chrome\//.test(userAgent)) browser = 'Chrome'
  else if (/Safari\//.test(userAgent) && !/Chrome\//.test(userAgent)) browser = 'Safari'
  else if (/Firefox\//.test(userAgent)) browser = 'Firefox'

  const normalizedPlatform =
    /Mac/i.test(platform) ? 'macOS' :
      /Win/i.test(platform) ? 'Windows' :
        /Linux/i.test(platform) ? 'Linux' :
          /iPhone|iPad|iPod/i.test(userAgent) ? 'iOS' :
            /Android/i.test(userAgent) ? 'Android' :
              platform

  return `${normalizedPlatform} · ${browser}`
}
