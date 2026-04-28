const suspiciousPattern = /[\u00C0-\u00FF\u0152\u0153\u0160\u0161\u0178]/g
const cjkPattern = /[\u3400-\u9fff]/

export function normalizeDisplayText(value: string | null | undefined): string {
  if (!value) return ''
  const trimmed = value.trim()
  if (!trimmed) return value

  const suspiciousMatches = trimmed.match(suspiciousPattern) ?? []
  if (suspiciousMatches.length < 2 || cjkPattern.test(trimmed)) {
    return value
  }

  try {
    const bytes = Uint8Array.from(Array.from(trimmed, (char) => char.charCodeAt(0) & 0xff))
    const repaired = new TextDecoder('utf-8', { fatal: false }).decode(bytes)

    if (!repaired || repaired === trimmed) {
      return value
    }

    const repairedSuspiciousMatches = repaired.match(suspiciousPattern) ?? []
    if (cjkPattern.test(repaired) || repairedSuspiciousMatches.length < suspiciousMatches.length) {
      return repaired
    }
  } catch {
    return value
  }

  return value
}
