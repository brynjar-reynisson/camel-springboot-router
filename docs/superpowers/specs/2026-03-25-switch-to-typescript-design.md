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
  - `"target": "ES2020"`
  - `"moduleResolution": "bundler"` (Vite-compatible)
  - `"module": "ESNext"`
  - `"lib": ["ES2020", "DOM", "DOM.Iterable"]`
  - `"skipLibCheck": true`
  - `"include": ["src"]`
- Rename `vite.config.js` → `vite.config.ts` (no content changes required)
- Update `eslint.config.js` to cover `.ts`/`.tsx` files and add `@typescript-eslint` parser + recommended rules

---

## Section 2: Source File Migration

### `main.jsx` → `main.tsx`
- Add non-null assertion on `getElementById('root')`: `document.getElementById('root') as HTMLElement`

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

- `eslint.config.js` file pattern: `**/*.{js,jsx,ts,tsx}`
- Add `@typescript-eslint` parser and recommended rules
- Replace `no-unused-vars` with `@typescript-eslint/no-unused-vars` (same pattern: `varsIgnorePattern: '^[A-Z_]'`)
- Vite handles `.tsx` natively via `@vitejs/plugin-react` — no Vite config changes needed beyond the rename
- Maven build (`frontend-maven-plugin` runs `npm run build`) requires no `pom.xml` changes

---

## Files Changed

| File | Action |
|---|---|
| `frontend/package.json` | Add `typescript`, `typescript-eslint` devDependencies |
| `frontend/tsconfig.json` | Create new |
| `frontend/vite.config.js` | Rename to `vite.config.ts` |
| `frontend/eslint.config.js` | Add TS parser + rules, extend file patterns |
| `frontend/src/main.jsx` | Rename to `main.tsx`, add non-null assertion |
| `frontend/src/App.jsx` | Rename to `App.tsx`, add all type annotations |

---

## Out of Scope

- No logic changes
- No new components or files
- No changes to backend, pom.xml, or Chrome extension
- No addition of stricter flags beyond `"strict": true` (e.g. no `noUncheckedIndexedAccess`)
