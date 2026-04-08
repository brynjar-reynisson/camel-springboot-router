export interface SearchResult {
  source: string
  name?: string
  snippet?: string
}

export interface SearchResponse {
  results: SearchResult[]
}

export interface SummarizeResponse {
  summary: string
}
