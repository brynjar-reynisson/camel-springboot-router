interface SearchBarProps {
  keywords: string
  setKeywords: (val: string) => void
  onSearch: () => void
  loading: boolean
}

export function SearchBar({ keywords, setKeywords, onSearch, loading }: SearchBarProps) {
  function handleKeyDown(e: React.KeyboardEvent<HTMLInputElement>) {
    if (e.key === 'Enter') onSearch()
  }

  return (
    <div className="search-bar">
      <input
        type="search"
        placeholder="Search..."
        value={keywords}
        onChange={e => setKeywords(e.target.value)}
        onKeyDown={handleKeyDown}
        autoFocus
      />
      <button onClick={onSearch} disabled={loading}>
        {loading ? 'Searching…' : 'Search'}
      </button>
    </div>
  )
}
