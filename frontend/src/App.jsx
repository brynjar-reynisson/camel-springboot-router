import { useState, useEffect } from 'react'
import './App.css'

const PAGE_SIZE = 10
const SEMANTIC_PAGE_SIZE = 5

function buildHref(source) {
  if (source.startsWith('http')) {
    return source
  }
  const normalized = source.replaceAll('\\', '/')
  return '/localFile?filePath=' + encodeURIComponent(normalized)
}

function ResultSection({ title, results, topSummary, pageSize = PAGE_SIZE }) {
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
          const label = item.name || item.source
          const display = label.length > 90 ? label.slice(0, 90) + '...' : label
          const isTop = i === 0 && page === 0 && topSummary !== undefined
          return (
            <li key={i}>
              <a href={buildHref(item.source)} target="_blank" rel="noopener noreferrer">
                {display}
              </a>
              {isTop && topSummary === null && (
                <p className="result-summary result-summary--loading">Summarizing…</p>
              )}
              {isTop && topSummary && (
                <p className="result-summary">{topSummary}</p>
              )}
            </li>
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

function App() {
  const [keywords, setKeywords] = useState('')
  const [semanticResults, setSemanticResults] = useState([])
  const [keywordResults, setKeywordResults] = useState([])
  const [loading, setLoading] = useState(false)
  const [searched, setSearched] = useState(false)
  const [error, setError] = useState(null)
  const [topSummary, setTopSummary] = useState(undefined)

  async function doSearch() {
    const trimmed = keywords.trim()
    if (!trimmed) return
    setLoading(true)
    setError(null)
    setTopSummary(undefined)
    try {
      const encoded = encodeURIComponent(trimmed)
      const [semanticRes, keywordRes] = await Promise.all([
        fetch('/semanticSearch?keywords=' + encoded),
        fetch('/search?keywords=' + encoded),
      ])
      if (!semanticRes.ok) throw new Error('Semantic search returned ' + semanticRes.status)
      if (!keywordRes.ok) throw new Error('Keyword search returned ' + keywordRes.status)
      const [semanticData, keywordData] = await Promise.all([
        semanticRes.json(),
        keywordRes.json(),
      ])
      const semantic = semanticData.results || []
      setSemanticResults(semantic)
      setKeywordResults(keywordData.results || [])
      setSearched(true)

      if (semantic.length > 0 && semantic[0].snippet) {
        setTopSummary(null)
        fetch('/summarize', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ text: semantic[0].snippet }),
        })
          .then(r => r.json())
          .then(d => setTopSummary(d.summary || ''))
          .catch(() => setTopSummary(''))
      }
    } catch (e) {
      setError(e.message)
    } finally {
      setLoading(false)
    }
  }

  function handleKeyDown(e) {
    if (e.key === 'Enter') doSearch()
  }

  const totalResults = semanticResults.length + keywordResults.length

  return (
    <div className={searched ? 'app' : 'app app--centered'}>
      <h1 className="app-title">Digital Me</h1>
      <div className="search-bar">
        <input
          type="search"
          placeholder="Search..."
          value={keywords}
          onChange={e => setKeywords(e.target.value)}
          onKeyDown={handleKeyDown}
          autoFocus
        />
        <button onClick={doSearch} disabled={loading}>
          {loading ? 'Searching…' : 'Search'}
        </button>
      </div>

      {error && <p className="error">Error: {error}</p>}

      {searched && !loading && (
        <div className="results">
          {totalResults === 0 ? (
            <p className="no-results">No results for <strong>{keywords}</strong>.</p>
          ) : (
            <>
              <ResultSection title="Semantic Search Results" results={semanticResults} topSummary={topSummary} pageSize={SEMANTIC_PAGE_SIZE} />
              <ResultSection title="Keyword Search Results" results={keywordResults} />
            </>
          )}
        </div>
      )}
    </div>
  )
}

export default App
