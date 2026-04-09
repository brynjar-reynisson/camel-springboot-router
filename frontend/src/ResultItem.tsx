import { SearchResult } from './types'
import { buildHref, truncateLabel } from './utils'

interface ResultItemProps {
  item: SearchResult
  isTop?: boolean
  topSummary?: string | null | undefined
}

export function ResultItem({ item, isTop, topSummary }: ResultItemProps) {
  const label = item.name || item.source
  const display = truncateLabel(label)

  return (
    <li>
      <a href={buildHref(item.source)} target="_blank" rel="noopener noreferrer">
        {display}
      </a>
      {isTop && topSummary === null && (
        <p className="result-summary result-summary--loading">Summarizing…</p>
      )}
      {isTop && topSummary && (
        <p className="result-summary">{topSummary}</p>
      )}
      {!isTop && item.snippet && (
        <p 
          className="result-snippet" 
          dangerouslySetInnerHTML={{ __html: item.snippet }} 
        />
      )}
    </li>
  )
}
