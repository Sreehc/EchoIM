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

export interface MentionSegment {
  text: string
  matched: boolean
  mention: boolean
  mentionUserId?: number
}

export function highlightMentions(
  value: string,
  mentions: Array<{ userId: number; displayName: string }>,
): MentionSegment[] {
  if (!mentions?.length) {
    return [{ text: value, matched: false, mention: false }]
  }

  const segments: MentionSegment[] = []
  let cursor = 0
  const lower = value.toLowerCase()

  const sortedMentions = [...mentions]
    .map((m) => {
      const pattern = `@${m.displayName}`
      const idx = lower.indexOf(pattern.toLowerCase(), cursor)
      return { ...m, pattern, idx }
    })
    .filter((m) => m.idx >= 0)
    .sort((a, b) => a.idx - b.idx)

  if (!sortedMentions.length) {
    return [{ text: value, matched: false, mention: false }]
  }

  for (const mention of sortedMentions) {
    if (mention.idx < cursor) continue

    if (mention.idx > cursor) {
      segments.push({ text: value.slice(cursor, mention.idx), matched: false, mention: false })
    }

    segments.push({
      text: value.slice(mention.idx, mention.idx + mention.pattern.length),
      matched: false,
      mention: true,
      mentionUserId: mention.userId,
    })
    cursor = mention.idx + mention.pattern.length
  }

  if (cursor < value.length) {
    segments.push({ text: value.slice(cursor), matched: false, mention: false })
  }

  return segments
}

export function highlightBubbleContent(
  value: string,
  searchQuery: string | undefined,
  mentions?: Array<{ userId: number; displayName: string }>,
): MentionSegment[] {
  if (mentions?.length) {
    const mentionSegments = highlightMentions(value, mentions)
    if (!searchQuery?.trim()) return mentionSegments

    const query = searchQuery.trim().toLowerCase()
    const result: MentionSegment[] = []

    for (const seg of mentionSegments) {
      if (seg.mention) {
        result.push(seg)
        continue
      }

      const lower = seg.text.toLowerCase()
      let start = 0
      let idx = lower.indexOf(query, start)

      if (idx < 0) {
        result.push(seg)
        continue
      }

      while (idx >= 0) {
        if (idx > start) {
          result.push({ text: seg.text.slice(start, idx), matched: false, mention: false })
        }
        const end = idx + query.length
        result.push({ text: seg.text.slice(idx, end), matched: true, mention: false })
        start = end
        idx = lower.indexOf(query, start)
      }

      if (start < seg.text.length) {
        result.push({ text: seg.text.slice(start), matched: false, mention: false })
      }
    }

    return result
  }

  return highlightText(value, searchQuery ?? '').map((s) => ({
    text: s.text,
    matched: s.matched,
    mention: false,
  }))
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
