# Switch Frontend to TypeScript — Design Spec

**Date:** 2026-03-25
**Branch:** feature/switch-to-typescript
**Scope:** Frontend only (`frontend/` directory)

---

## Goal

Migrate the React frontend from JavaScript (`.jsx`) to TypeScript (`.tsx`) with strict type-checking enabled. No logic changes; pure type annotation on top of existing code.

---

## Approach

Option A — rename and type everything at once. The frontend has exactly two source files (~160 lines total), making a complete migration in one pass the simplest and cleanest path.

---

## Section 1: Dependencies & Configuration

- Add `typescript` to `devDependencies` in `package.json`
- Add `typescript-eslint` to `devDependencies` in `package.json`
- `@types/react` and `@types/react-dom` are already present — no change needed
- Add `tsconfig.json` at `frontend/` root:
  - `"strict": true`
  - `"jsx": "react-jsx"`
  - `"target": "ES2021"` — upgraded from ES2020 to include native `replaceAll` support (see commit 53e355b)
  - `"moduleResolution": "bundler"` (Vite-compatible)
  - `"module": "ESNext"`
  - `"lib": ["ES2021", "DOM", "DOM.Iterable"]`
  - `"skipLibCheck": true`
  - `"isolatedModules": true` (required: Vite uses esbuild per-file transpilation; this surfaces errors esbuild would silently mishandle)
  - `"noEmit": true` (tsc is used as type-checker only; Vite/esbuild handles emit)
  - `"include": ["src"]`
- Rename `vite.config.js` → `vite.config.ts` (no content changes required)
- Update `eslint.config.js` to cover `.ts`/`.tsx` files and add `@typescript-eslint` parser + recommended rules

---

## Section 2: Source File Migration

### `main.jsx` → `main.tsx`
- Add null guard on `getElementById('root')` — throws a clear error if the element is missing rather than casting unsafely:
  ```ts
  const root = document.getElementById('root')
  if (!root) throw new Error('Root element #root not found in DOM')
  createRoot(root).render(...)
  ```
- Update import: `from './App.jsx'` → `from './App'` (TypeScript with `moduleResolution: bundler` rejects explicit `.jsx` extensions pointing to `.tsx` files)

### `App.jsx` → `App.tsx`
Introduce the following types:

```ts
interface SearchResult {
  source: string;
  name?: string;
  snippet?: string;
}

interface ResultSectionProps {
  title: string;
  results: SearchResult[];
  topSummary?: string | null;
  pageSize?: number;
}
```

Error handling:
- `catch (e)` binding is typed as `unknown` under `"strict": true`; `setError(e.message)` will not compile. Fix: `setError((e as Error).message)`

State generics:
- `useState<string>('')` — keywords
- `useState<SearchResult[]>([])` — semanticResults, keywordResults
- `useState<boolean>(false)` — loading, searched
- `useState<string | null>(null)` — error
- `useState<string | null | undefined>(undefined)` — topSummary

Event handler types:
- `onChange`: `(e: React.ChangeEvent<HTMLInputElement>) => void`
- `onKeyDown` / `handleKeyDown`: `(e: React.KeyboardEvent<HTMLInputElement>) => void`

---

## Section 3: ESLint & Build

- Add `typescript-eslint` (the unified package, not the legacy `@typescript-eslint/eslint-plugin`) to devDependencies
- Use `tseslint.config(...)` helper (from `typescript-eslint`) instead of `defineConfig` from `eslint/config`, and spread `...tseslint.configs.recommended` for TS rules
- Extend file pattern to `**/*.{js,jsx,ts,tsx}` (covers `eslint.config.js` and `vite.config.ts` as well)
- Replace `no-unused-vars` with `@typescript-eslint/no-unused-vars` (`varsIgnorePattern: '^_'` — standard convention for intentionally-unused identifiers)
- Type-aware linting (`parserOptions.project`) is **out of scope** for this migration — not setting it keeps ESLint fast and avoids needing a separate `tsconfig.eslint.json`
- Vite handles `.tsx` natively via `@vitejs/plugin-react` — no other Vite config changes needed beyond the rename
- Maven build (`frontend-maven-plugin` runs `npm run build`) requires no `pom.xml` changes

---

## Files Changed

| File | Action |
|---|---|
| `frontend/package.json` | Add `typescript`, `typescript-eslint` devDependencies |
| `frontend/tsconfig.json` | Create new |
| `frontend/vite.config.js` | Rename to `vite.config.ts` |
| `frontend/eslint.config.js` | Add TS parser + rules, extend file patterns |
| `frontend/index.html` | Update script reference from `/src/main.jsx` to `/src/main.tsx` |
| `frontend/src/main.jsx` | Rename to `main.tsx`, add null guard + throw, update App import |
| `frontend/src/App.jsx` | Rename to `App.tsx`, add all type annotations |

---

## Out of Scope

- No logic changes
- No new components or files
- No changes to backend, pom.xml, or Chrome extension
- No addition of stricter flags beyond `"strict": true` (e.g. no `noUncheckedIndexedAccess`)
