import { useState } from 'react'
import './App.css'
import { SearchResult, SearchResponse, SummarizeResponse } from './types'
import { SearchBar } from './SearchBar'
import { ResultSection } from './ResultSection'

const PAGE_SIZE = 10
const SEMANTIC_PAGE_SIZE = 5

function App() {
  const [keywords, setKeywords] = useState('')
  const [semanticResults, setSemanticResults] = useState<SearchResult[]>([])
  const [keywordResults, setKeywordResults] = useState<SearchResult[]>([])
  const [loading, setLoading] = useState(false)
  const [searched, setSearched] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [topSummary, setTopSummary] = useState<string | null | undefined>(undefined)
  const [searchId, setSearchId] = useState(0)

  async function doSearch() {
    const trimmed = keywords.trim()
    if (!trimmed) return
    setLoading(true)
    setError(null)
    setTopSummary(undefined)
    setSearchId(id => id + 1)
    try {
      const encoded = encodeURIComponent(trimmed)
      const [semanticRes, keywordRes] = await Promise.all([
        fetch('/semanticSearch?keywords=' + encoded),
        fetch('/search?keywords=' + encoded),
      ])
      if (!semanticRes.ok) throw new Error('Semantic search returned ' + semanticRes.status)
      if (!keywordRes.ok) throw new Error('Keyword search returned ' + keywordRes.status)
      const [semanticData, keywordData] = await Promise.all([
        semanticRes.json() as Promise<SearchResponse>,
        keywordRes.json() as Promise<SearchResponse>,
      ])
      const semantic: SearchResult[] = semanticData.results || []
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
          .then(r => r.json() as Promise<SummarizeResponse>)
          .then(d => setTopSummary(d.summary || ''))
          .catch(() => setTopSummary(''))
      }
    } catch (e) {
      setError((e as Error).message)
    } finally {
      setLoading(false)
    }
  }

  const totalResults = semanticResults.length + keywordResults.length

  return (
    <div className={searched ? 'app' : 'app app--centered'}>
      <h1 className="app-title">Digital Me</h1>
      <SearchBar 
        keywords={keywords} 
        setKeywords={setKeywords} 
        onSearch={doSearch} 
        loading={loading} 
      />

      {error && <p className="error">Error: {error}</p>}

      {searched && !loading && (
        <div className="results">
          {totalResults === 0 ? (
            <p className="no-results">No results for <strong>{keywords}</strong>.</p>
          ) : (
            <>
              <ResultSection 
                key={`semantic-${searchId}`}
                title="Semantic Search Results" 
                results={semanticResults} 
                topSummary={topSummary} 
                pageSize={SEMANTIC_PAGE_SIZE} 
              />
              <ResultSection 
                key={`keyword-${searchId}`}
                title="Keyword Search Results" 
                results={keywordResults} 
                pageSize={PAGE_SIZE}
              />
            </>
          )}
        </div>
      )}
    </div>
  )
}

export default App

