interface IncomingNotificationPayload {
  title: string
  body: string
  tag?: string
}

export function isBrowserNotificationSupported() {
  return typeof window !== 'undefined' && 'Notification' in window
}

export function readBrowserNotificationPermission(): NotificationPermission | 'unsupported' {
  if (!isBrowserNotificationSupported()) return 'unsupported'
  return Notification.permission
}

export function canShowBrowserNotification() {
  return readBrowserNotificationPermission() === 'granted'
}

export function showIncomingMessageNotification(payload: IncomingNotificationPayload) {
  if (!canShowBrowserNotification()) return

  try {
    const notification = new Notification(payload.title, {
      body: payload.body,
      tag: payload.tag,
    })

    notification.onclick = () => {
      window.focus()
      notification.close()
    }
  } catch {
    // Notification delivery is best-effort and must not affect message sync.
  }
}
