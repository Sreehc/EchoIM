const timeFormatter = new Intl.DateTimeFormat('zh-CN', {
  hour: '2-digit',
  minute: '2-digit',
})

const dayFormatter = new Intl.DateTimeFormat('zh-CN', {
  month: '2-digit',
  day: '2-digit',
})

export function formatConversationTime(value: string): string {
  const date = new Date(value)
  const now = new Date()
  const sameDay = now.toDateString() === date.toDateString()

  if (sameDay) {
    return timeFormatter.format(date)
  }

  return dayFormatter.format(date)
}

export function formatMessageTime(value: string): string {
  return timeFormatter.format(new Date(value))
}

export function highlightText(value: string, keyword: string) {
  const query = keyword.trim()
  if (!query) {
    return [{ text: value, matched: false }]
  }

  const source = value.toLowerCase()
  const target = query.toLowerCase()
  const segments: Array<{ text: string; matched: boolean }> = []
  let start = 0
  let index = source.indexOf(target, start)

  if (index < 0) {
    return [{ text: value, matched: false }]
  }

  while (index >= 0) {
    if (index > start) {
      segments.push({ text: value.slice(start, index), matched: false })
    }

    const end = index + query.length
    segments.push({ text: value.slice(index, end), matched: true })
    start = end
    index = source.indexOf(target, start)
  }

  if (start < value.length) {
    segments.push({ text: value.slice(start), matched: false })
  }

  return segments
}

export function formatVoiceDuration(seconds: number): string {
  const totalSeconds = Math.max(0, Math.round(seconds))
  if (totalSeconds < 60) {
    return `${totalSeconds}"`
  }
  const minutes = Math.floor(totalSeconds / 60)
  const remaining = totalSeconds % 60
  return `${minutes}'${remaining.toString().padStart(2, '0')}"`
}
