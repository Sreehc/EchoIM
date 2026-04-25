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
  const index = source.indexOf(target)

  if (index === -1) {
    return [{ text: value, matched: false }]
  }

  const end = index + query.length

  return [
    ...(index > 0 ? [{ text: value.slice(0, index), matched: false }] : []),
    { text: value.slice(index, end), matched: true },
    ...(end < value.length ? [{ text: value.slice(end), matched: false }] : []),
  ]
}
