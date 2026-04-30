export function buildPublicProfilePath(username: string) {
  return `/u/${encodeURIComponent(username)}`
}

export function buildPublicProfileUrl(username: string) {
  if (typeof window === 'undefined') {
    return buildPublicProfilePath(username)
  }
  return new URL(buildPublicProfilePath(username), window.location.origin).toString()
}
