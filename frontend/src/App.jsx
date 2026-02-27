import { useState } from 'react'
import './App.css'

const PAGE_SIZE = 10

function buildHref(source) {
  if (source.startsWith('http')) {
    return source
  }
  const normalized = source.replaceAll('\\', '/')
  return '/localFile?filePath=' + encodeURIComponent(normalized)
}

function App() {
  const [keywords, setKeywords] = useState('')
  const [results, setResults] = useState([])
  const [page, setPage] = useState(0)
  const [loading, setLoading] = useState(false)
  const [searched, setSearched] = useState(false)
  const [error, setError] = useState(null)

  async function doSearch() {
    const trimmed = keywords.trim()
    if (!trimmed) return
    setLoading(true)
    setError(null)
    try {
      const res = await fetch('/search?keywords=' + encodeURIComponent(trimmed))
      if (!res.ok) throw new Error('Server returned ' + res.status)
      const data = await res.json()
      setResults(data.results || [])
      setPage(0)
      setSearched(true)
    } catch (e) {
      setError(e.message)
    } finally {
      setLoading(false)
    }
  }

  function handleKeyDown(e) {
    if (e.key === 'Enter') doSearch()
  }

  const totalPages = Math.ceil(results.length / PAGE_SIZE)
  const pageResults = results.slice(page * PAGE_SIZE, page * PAGE_SIZE + PAGE_SIZE)

  return (
    <div className="app">
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
          {results.length === 0 ? (
            <p className="no-results">No results for <strong>{keywords}</strong>.</p>
          ) : (
            <>
              <p className="result-count">
                {results.length} result{results.length !== 1 ? 's' : ''}
                {totalPages > 1 && ` — page ${page + 1} of ${totalPages}`}
              </p>
              <ul>
                {pageResults.map((item, i) => {
                  const label = item.name || item.source
                  const display = label.length > 90 ? label.slice(0, 90) + '...' : label
                  return (
                    <li key={i}>
                      <a
                        href={buildHref(item.source)}
                        target="_blank"
                        rel="noopener noreferrer"
                      >
                        {display}
                      </a>
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
            </>
          )}
        </div>
      )}
    </div>
  )
}

export default App
