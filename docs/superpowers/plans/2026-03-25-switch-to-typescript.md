# Switch Frontend to TypeScript — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Migrate the React frontend from JavaScript (`.jsx`) to TypeScript (`.tsx`) with `"strict": true`, adding full type annotations and no logic changes.

**Architecture:** Two source files (`App.jsx`, `main.jsx`) are renamed to `.tsx` and annotated with interfaces, state generics, and event handler types. Configuration files (`tsconfig.json`, `vite.config.ts`, `eslint.config.js`) are added/updated. No new runtime dependencies; `typescript` and `typescript-eslint` are added as devDependencies only.

**Tech Stack:** React 19, Vite 7, TypeScript 5.x, typescript-eslint (flat config), `@vitejs/plugin-react`

---

## File Map

| File | Action | Responsibility |
|---|---|---|
| `frontend/package.json` | Modify | Add `typescript`, `typescript-eslint` devDependencies |
| `frontend/tsconfig.json` | Create | TypeScript compiler config (strict, noEmit, isolatedModules) |
| `frontend/vite.config.js` | Rename → `vite.config.ts` | No content change — Vite config is already typed via `defineConfig` |
| `frontend/eslint.config.js` | Modify | Add TS parser + recommended rules via `typescript-eslint` |
| `frontend/index.html` | Modify | Update script src from `/src/main.jsx` → `/src/main.tsx` |
| `frontend/src/main.jsx` | Rename → `main.tsx` | Entry point — add non-null assertion, drop `.jsx` extension on App import |
| `frontend/src/App.jsx` | Rename → `App.tsx` | Main component — add interfaces, state generics, event handler types |

---

## Task 1: Install TypeScript and create tsconfig.json

**Files:**
- Modify: `frontend/package.json`
- Create: `frontend/tsconfig.json`

- [ ] **Step 1: Install devDependencies**

Run from the repo root:
```bash
cd frontend && npm install --save-dev typescript typescript-eslint
```
Expected: `package.json` updated, `node_modules` populated. No output errors.

- [ ] **Step 2: Verify TypeScript is available**

```bash
cd frontend && npx tsc --version
```
Expected output: `Version 5.x.x` (any 5.x is fine)

- [ ] **Step 3: Create `frontend/tsconfig.json`**

```json
{
  "compilerOptions": {
    "target": "ES2020",
    "module": "ESNext",
    "moduleResolution": "bundler",
    "lib": ["ES2020", "DOM", "DOM.Iterable"],
    "jsx": "react-jsx",
    "strict": true,
    "isolatedModules": true,
    "noEmit": true,
    "skipLibCheck": true
  },
  "include": ["src"]
}
```

Notes:
- `moduleResolution: "bundler"` is the correct Vite mode (not `"node"`)
- `isolatedModules: true` is required because Vite uses esbuild per-file transpilation
- `noEmit: true` — `tsc` is used only as a type-checker; Vite/esbuild handles the actual emit
- `include: ["src"]` limits type-checking to the source files only

- [ ] **Step 4: Commit**

```bash
cd ..
git add frontend/package.json frontend/package-lock.json frontend/tsconfig.json
git commit -m "feat: add TypeScript and tsconfig to frontend"
```

---

## Task 2: Rename vite.config.js and update index.html

**Files:**
- Rename: `frontend/vite.config.js` → `frontend/vite.config.ts`
- Modify: `frontend/index.html`

- [ ] **Step 1: Rename vite.config.js**

```bash
git mv frontend/vite.config.js frontend/vite.config.ts
```

No content changes needed — `defineConfig` from Vite is already typed.

- [ ] **Step 2: Update index.html script reference**

In `frontend/index.html` line 11, change:
```html
<script type="module" src="/src/main.jsx"></script>
```
to:
```html
<script type="module" src="/src/main.tsx"></script>
```

- [ ] **Step 3: Commit**

```bash
git add frontend/vite.config.ts frontend/index.html
git commit -m "feat: rename vite.config to .ts, update index.html entry point"
```

Note: `git mv` pre-stages the rename, but explicitly adding `vite.config.ts` ensures it is included even if staging was reset.

---

## Task 3: Migrate App.jsx → App.tsx

**Files:**
- Rename + Modify: `frontend/src/App.jsx` → `frontend/src/App.tsx`

- [ ] **Step 1: Rename the file**

```bash
git mv frontend/src/App.jsx frontend/src/App.tsx
```

- [ ] **Step 2: Replace the file content with the fully typed version**

Write the following to `frontend/src/App.tsx`:

```tsx
import { useState } from 'react'
import './App.css'

const PAGE_SIZE = 10
const SEMANTIC_PAGE_SIZE = 5

interface SearchResult {
  source: string
  name?: string
  snippet?: string
}

interface ResultSectionProps {
  title: string
  results: SearchResult[]
  topSummary?: string | null
  pageSize?: number
}

function buildHref(source: string): string {
  if (source.startsWith('http')) {
    return source
  }
  const normalized = source.replaceAll('\\', '/')
  return '/localFile?filePath=' + encodeURIComponent(normalized)
}

function ResultSection({ title, results, topSummary, pageSize = PAGE_SIZE }: ResultSectionProps) {
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
  const [keywords, setKeywords] = useState<string>('')
  const [semanticResults, setSemanticResults] = useState<SearchResult[]>([])
  const [keywordResults, setKeywordResults] = useState<SearchResult[]>([])
  const [loading, setLoading] = useState<boolean>(false)
  const [searched, setSearched] = useState<boolean>(false)
  const [error, setError] = useState<string | null>(null)
  const [topSummary, setTopSummary] = useState<string | null | undefined>(undefined)

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
          .then(r => r.json())
          .then(d => setTopSummary(d.summary || ''))
          .catch(() => setTopSummary(''))
      }
    } catch (e) {
      setError((e as Error).message)
    } finally {
      setLoading(false)
    }
  }

  function handleKeyDown(e: React.KeyboardEvent<HTMLInputElement>) {
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
          onChange={(e: React.ChangeEvent<HTMLInputElement>) => setKeywords(e.target.value)}
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
```

Notes on changes from App.jsx:
- Removed unused `useEffect` import (was imported but never called — `@typescript-eslint/no-unused-vars` would flag it)
- Added `SearchResult` interface (shapes the API response objects)
- Added `ResultSectionProps` interface (types the component props)
- Added `: string` return type to `buildHref`
- Added `useState<T>` generics on all state variables
- `catch (e)` → `setError((e as Error).message)` (under `strict`, `e` is `unknown`)
- `handleKeyDown` parameter typed as `React.KeyboardEvent<HTMLInputElement>`
- `onChange` handler typed as `React.ChangeEvent<HTMLInputElement>`
- `const semantic: SearchResult[]` annotation on the parsed API response

- [ ] **Step 3: Commit**

```bash
git add frontend/src/App.tsx
git commit -m "feat: migrate App.jsx to App.tsx with full type annotations"
```

---

## Task 4: Migrate main.jsx → main.tsx

**Files:**
- Rename + Modify: `frontend/src/main.jsx` → `frontend/src/main.tsx`

- [ ] **Step 1: Rename the file**

```bash
git mv frontend/src/main.jsx frontend/src/main.tsx
```

- [ ] **Step 2: Replace the file content**

Write the following to `frontend/src/main.tsx`:

```tsx
import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import './index.css'
import App from './App'

createRoot(document.getElementById('root') as HTMLElement).render(
  <StrictMode>
    <App />
  </StrictMode>,
)
```

Notes on changes from main.jsx:
- `document.getElementById('root') as HTMLElement` — non-null assertion required because `getElementById` returns `HTMLElement | null`; `createRoot` requires `HTMLElement`
- `import App from './App'` — dropped explicit `.jsx` extension; TypeScript with `moduleResolution: "bundler"` rejects `.jsx` extensions pointing to `.tsx` files

- [ ] **Step 3: Run the TypeScript type-checker**

```bash
cd frontend && npx tsc --noEmit
```
Expected: no output, exit code 0. If you see errors, read them carefully — all expected type issues are covered in this plan. Common surprises:
- `Object is of type 'unknown'` → check that the `catch (e)` cast in App.tsx uses `(e as Error).message`
- `Argument of type 'HTMLElement | null'` → check the `as HTMLElement` assertion in main.tsx

- [ ] **Step 4: Run the Vite build**

```bash
cd frontend && npm run build
```
Expected: `dist/` (ignored) and `../src/main/resources/static/` populated with built assets. No TypeScript errors in the output.

- [ ] **Step 5: Commit**

```bash
cd ..
git add frontend/src/main.tsx
git commit -m "feat: migrate main.jsx to main.tsx"
```

---

## Task 5: Update ESLint config for TypeScript

**Files:**
- Modify: `frontend/eslint.config.js`

- [ ] **Step 1: Replace eslint.config.js content**

Write the following to `frontend/eslint.config.js`:

```js
import js from '@eslint/js'
import tseslint from 'typescript-eslint'
import globals from 'globals'
import reactHooks from 'eslint-plugin-react-hooks'
import reactRefresh from 'eslint-plugin-react-refresh'

export default tseslint.config(
  { ignores: ['dist'] },
  {
    files: ['**/*.{js,jsx,ts,tsx}'],
    extends: [
      js.configs.recommended,
      ...tseslint.configs.recommended,
      reactHooks.configs.flat.recommended,
      reactRefresh.configs.vite,
    ],
    languageOptions: {
      ecmaVersion: 2020,
      globals: globals.browser,
      parserOptions: {
        ecmaVersion: 'latest',
        ecmaFeatures: { jsx: true },
        sourceType: 'module',
      },
    },
    rules: {
      'no-unused-vars': 'off',
      '@typescript-eslint/no-unused-vars': ['error', { varsIgnorePattern: '^[A-Z_]' }],
    },
  },
)
```

Notes:
- `tseslint.config()` replaces `defineConfig` from `eslint/config` — it is the `typescript-eslint` config helper
- `js.configs.recommended` is retained from the original config (baseline JS rules: no-undef, no-redeclare, etc.); `@eslint/js` is already in devDependencies
- `...tseslint.configs.recommended` spreads the TS recommended rule set (includes `@typescript-eslint/parser` automatically)
- `'no-unused-vars': 'off'` disables the base JS rule to avoid double-reporting; `@typescript-eslint/no-unused-vars` takes over
- `varsIgnorePattern: '^[A-Z_]'` preserves the existing behaviour (variables starting with uppercase or underscore are ignored)
- Type-aware linting (`parserOptions.project`) is deliberately excluded — not needed for this codebase size

- [ ] **Step 2: Run ESLint**

```bash
cd frontend && npm run lint
```
Expected: no errors, exit code 0.

- [ ] **Step 3: Commit**

```bash
cd ..
git add frontend/eslint.config.js frontend/package.json frontend/package-lock.json
git commit -m "feat: update ESLint config for TypeScript"
```

---

## Task 6: Final verification and docs update

- [ ] **Step 1: Full type-check**

```bash
cd frontend && npx tsc --noEmit
```
Expected: clean, exit code 0.

- [ ] **Step 2: Full lint**

```bash
cd frontend && npm run lint
```
Expected: clean, exit code 0.

- [ ] **Step 3: Full build**

```bash
cd frontend && npm run build
```
Expected: build succeeds, assets written to `../src/main/resources/static/`.

- [ ] **Step 4: Update CLAUDE.md tech stack table**

In `CLAUDE.md`, in the Tech Stack table, update the Frontend row:

Change:
```
| Frontend | React 19, Vite 7 |
```
To:
```
| Frontend | React 19, Vite 7, TypeScript 5 (strict) |
```

- [ ] **Step 5: Commit docs update**

```bash
git add CLAUDE.md
git commit -m "docs: update CLAUDE.md to reflect TypeScript frontend"
```
