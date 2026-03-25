# TypeScript Review Fixes Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Apply all code-quality-reviewer findings from the TypeScript migration review — two major fixes (null safety, typed API responses) and four minor fixes (ESLint config, list keys, spec doc, build script).

**Architecture:** Pure type/config changes on top of the existing frontend. No runtime logic changes, no new components. Each task is a single focused file edit followed by lint/typecheck verification.

**Tech Stack:** React 19, TypeScript 5 (strict), Vite 7, ESLint 9 flat config, typescript-eslint 8.

---

## Files Modified

| File | Change |
|---|---|
| `frontend/src/main.tsx` | Replace unsafe `as HTMLElement` cast with null guard |
| `frontend/src/App.tsx` | Add `SearchResponse`/`SummarizeResponse` interfaces; type `.json()` calls; fix list key |
| `frontend/eslint.config.js` | Remove redundant `parserOptions.ecmaVersion`; narrow `varsIgnorePattern` to `'^_'` |
| `frontend/package.json` | Add `tsc --noEmit &&` prefix to `build` script |
| `docs/superpowers/specs/2026-03-25-switch-to-typescript-design.md` | Update target/lib to ES2021; record intentional upgrade reason |

---

### Task 1: Fix null guard in main.tsx (M1)

**Files:**
- Modify: `frontend/src/main.tsx:6`

- [ ] **Step 1: Edit main.tsx — replace cast with null guard**

Replace the entire file content:

```tsx
import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import './index.css'
import App from './App'

const root = document.getElementById('root')
if (!root) throw new Error('Root element #root not found in DOM')
createRoot(root).render(
  <StrictMode>
    <App />
  </StrictMode>,
)
```

- [ ] **Step 2: Verify TypeScript accepts it**

Run from `frontend/`:
```bash
cd frontend && npx tsc --noEmit
```
Expected: no output (zero errors).

- [ ] **Step 3: Commit**

```bash
git add frontend/src/main.tsx
git commit -m "fix: replace unsafe HTMLElement cast with null guard in main.tsx"
```

---

### Task 2: Add typed API response interfaces + fix list key in App.tsx (M2 + m4)

**Files:**
- Modify: `frontend/src/App.tsx`

The two problems are:
1. `semanticRes.json()` / `keywordRes.json()` / `r.json()` all return `Promise<any>` — the results are then silently accepted as typed values.
2. `key={i}` in `pageResults.map` uses array index, causing wrong React reconciliation when results change.

- [ ] **Step 1: Add response interfaces after the existing interfaces in App.tsx**

After line 18 (after the closing `}` of `ResultSectionProps`), add:

```ts
interface SearchResponse {
  results: SearchResult[]
}

interface SummarizeResponse {
  summary: string
}
```

- [ ] **Step 2: Type the `.json()` calls in the `doSearch` function**

Replace lines 100–106 (the `Promise.all` json block through setting keyword results):

```ts
      const [semanticData, keywordData] = await Promise.all([
        semanticRes.json() as Promise<SearchResponse>,
        keywordRes.json() as Promise<SearchResponse>,
      ])
      const semantic: SearchResult[] = semanticData.results || []
      setSemanticResults(semantic)
      setKeywordResults(keywordData.results || [])
```

- [ ] **Step 3: Type the summarize `.json()` call**

Replace line 116–117 (the `.then(d => ...)` chain):

```ts
          .then(r => r.json() as Promise<SummarizeResponse>)
          .then(d => setTopSummary(d.summary || ''))
```

- [ ] **Step 4: Fix list item key**

Replace line 48 (`<li key={i}>`):

```tsx
            <li key={item.source}>
```

- [ ] **Step 5: Verify TypeScript and lint pass**

```bash
cd frontend && npx tsc --noEmit && npx eslint src/App.tsx
```
Expected: no output (zero errors, zero warnings).

- [ ] **Step 6: Commit**

```bash
git add frontend/src/App.tsx
git commit -m "fix: add typed API response interfaces and fix list key in App.tsx"
```

---

### Task 3: Clean up eslint.config.js (m2 + m3)

**Files:**
- Modify: `frontend/eslint.config.js`

Two problems:
1. `parserOptions.ecmaVersion: 'latest'` is redundant — `languageOptions.ecmaVersion` is the authoritative setting in flat config.
2. `varsIgnorePattern: '^[A-Z_]'` silences all PascalCase symbols (components, interfaces, type aliases). Narrow to `'^_'` — the TypeScript community convention for intentionally-unused identifiers.

- [ ] **Step 1: Edit eslint.config.js**

Replace the entire file:

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
        ecmaFeatures: { jsx: true },
        sourceType: 'module',
      },
    },
    rules: {
      'no-unused-vars': 'off',
      '@typescript-eslint/no-unused-vars': ['error', { varsIgnorePattern: '^_' }],
    },
  },
)
```

- [ ] **Step 2: Verify lint still passes over all source files**

```bash
cd frontend && npx eslint .
```
Expected: no output (zero errors).

- [ ] **Step 3: Commit**

```bash
git add frontend/eslint.config.js
git commit -m "fix: remove redundant parserOptions.ecmaVersion and narrow varsIgnorePattern in ESLint config"
```

---

### Task 4: Add tsc to build script + update spec doc (recommendation + m1 + m5)

**Files:**
- Modify: `frontend/package.json`
- Modify: `docs/superpowers/specs/2026-03-25-switch-to-typescript-design.md`

- [ ] **Step 1: Update build script in package.json**

Replace the `"build"` line:

```json
    "build": "tsc --noEmit && vite build",
```

- [ ] **Step 2: Verify the build still works**

```bash
cd frontend && npm run build
```
Expected: output ends with `✓ built in ...ms` (or similar Vite success message), no TypeScript errors.

- [ ] **Step 3: Update spec doc — fix ES2021 discrepancy and record reason**

In `docs/superpowers/specs/2026-03-25-switch-to-typescript-design.md`, update Section 1 tsconfig bullet points. Replace:

```
  - `"target": "ES2020"`
  - `"moduleResolution": "bundler"` (Vite-compatible)
  - `"module": "ESNext"`
  - `"lib": ["ES2020", "DOM", "DOM.Iterable"]`
```

With:

```
  - `"target": "ES2021"` — upgraded from ES2020 to include native `replaceAll` support (see commit 53e355b)
  - `"moduleResolution": "bundler"` (Vite-compatible)
  - `"module": "ESNext"`
  - `"lib": ["ES2021", "DOM", "DOM.Iterable"]`
```

Also replace the ESLint section 3 bullet that mentions `varsIgnorePattern`:

```
- Replace `no-unused-vars` with `@typescript-eslint/no-unused-vars` (same `varsIgnorePattern: '^[A-Z_]'`)
```

With:

```
- Replace `no-unused-vars` with `@typescript-eslint/no-unused-vars` (`varsIgnorePattern: '^_'` — standard convention for intentionally-unused identifiers)
```

- [ ] **Step 4: Commit everything**

```bash
git add frontend/package.json docs/superpowers/specs/2026-03-25-switch-to-typescript-design.md
git commit -m "fix: add tsc type-check to build script and sync spec doc with actual implementation"
```
