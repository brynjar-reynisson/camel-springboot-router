export function buildHref(source: string): string {
  if (source.startsWith('http')) {
    return source
  }
  const normalized = source.replaceAll('\\', '/')
  return '/localFile?filePath=' + encodeURIComponent(normalized)
}

export function truncateLabel(label: string, maxLength: number = 90): string {
  return label.length > maxLength ? label.slice(0, maxLength) + '...' : label
}
