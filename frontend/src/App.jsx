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

function ResultSection({ title, results }) {
  const [page, setPage] = useState(0)
  const totalPages = Math.ceil(results.length / PAGE_SIZE)
  const pageResults = results.slice(page * PAGE_SIZE, page * PAGE_SIZE + PAGE_SIZE)

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
          return (
            <li key={i}>
              <a href={buildHref(item.source)} target="_blank" rel="noopener noreferrer">
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

  async function doSearch() {
    const trimmed = keywords.trim()
    if (!trimmed) return
    setLoading(true)
    setError(null)
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
      setSemanticResults(semanticData.results || [])
      setKeywordResults(keywordData.results || [])
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

  const totalResults = semanticResults.length + keywordResults.length

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
          {totalResults === 0 ? (
            <p className="no-results">No results for <strong>{keywords}</strong>.</p>
          ) : (
            <>
              <ResultSection title="Semantic Search Results" results={semanticResults} />
              <ResultSection title="Keyword Search Results" results={keywordResults} />
            </>
          )}
        </div>
      )}
    </div>
  )
}

export default App
