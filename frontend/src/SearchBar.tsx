import { useState, useEffect } from 'react'

interface SearchBarProps {
  keywords: string
  setKeywords: (val: string) => void
  onSearch: () => void
  loading: boolean
}

interface OllamaStatus {
  online: boolean
  embedding: boolean
  summarize: boolean
}

export function SearchBar({ keywords, setKeywords, onSearch, loading }: SearchBarProps) {
  const [status, setStatus] = useState<OllamaStatus | null>(null)

  useEffect(() => {
    fetch('/health/ollama')
      .then(r => r.json())
      .then(setStatus)
      .catch(() => setStatus({ online: false, embedding: false, summarize: false }))
  }, [])

  function handleKeyDown(e: React.KeyboardEvent<HTMLInputElement>) {
    if (e.key === 'Enter') onSearch()
  }

  return (
    <div className="search-bar">
      <div className="search-input-wrapper">
        <input
          type="search"
          placeholder="Search..."
          value={keywords}
          onChange={e => setKeywords(e.target.value)}
          onKeyDown={handleKeyDown}
          autoFocus
        />
        <div 
          className={`ollama-status ${status?.online ? 'ollama-status--online' : 'ollama-status--offline'}`}
          title={status?.online ? 'Semantic search active' : 'Semantic search unavailable'}
        />
      </div>
      <button onClick={onSearch} disabled={loading}>
        {loading ? 'Searching…' : 'Search'}
      </button>
    </div>
  )
}
