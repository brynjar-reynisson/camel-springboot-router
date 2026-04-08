import { useState } from 'react'
import { SearchResult } from './types'
import { ResultItem } from './ResultItem'

interface ResultSectionProps {
  title: string
  results: SearchResult[]
  topSummary?: string | null | undefined
  pageSize: number
}

export function ResultSection({ title, results, topSummary, pageSize }: ResultSectionProps) {
  const [page, setPage] = useState(0)

  const totalPages = Math.ceil(results.length / pageSize)
  const pageResults = results.slice(page * pageSize, page * pageSize + pageSize)

  if (results.length === 0) return null

  return (
    <div className="result-section">
      <h2 className="result-section-title">{title}</h2>
      <p className="result-count">
        {results.length} result{results.length !== 1 ? 's' : ''}
        {totalPages > 1 && ` — page ${page + 1} of ${totalPages}`}
      </p>
      <ul>
        {pageResults.map((item, i) => {
          const isTop = i === 0 && page === 0 && topSummary !== undefined
          return (
            <ResultItem 
              key={item.source} 
              item={item} 
              isTop={isTop} 
              topSummary={topSummary} 
            />
          )
        })}
      </ul>
      {totalPages > 1 && (
        <div className="pagination">
          <button onClick={() => setPage(p => p - 1)} disabled={page === 0}>
            ← Previous
          </button>
          <span>{page + 1} / {totalPages}</span>
          <button onClick={() => setPage(p => p + 1)} disabled={page >= totalPages - 1}>
            Next →
          </button>
        </div>
      )}
    </div>
  )
}
