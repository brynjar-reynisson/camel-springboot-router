# Architecture

## REST API (`IndexPage.java`)

| Method | Path | Description |
|---|---|---|
| `GET` | `/` | Redirects to `/index.html` |
| `GET` | `/search?keywords=...` | Lucene full-text search; returns `{ results: [{source, name}] }` |
| `GET` | `/localFile?filePath=...` | Reads a local file and returns HTML-escaped content |
| `POST` | `/addContent` | Indexes content; body: `{ source, name, content }` |

`/addContent` uses a `ReentrantLock` for thread safety. If `source` starts with `http`, content is stripped to plain text via Jsoup before indexing.

---

## Camel routes

Routes are XML files in `digital-me-dev/routes/` loaded at runtime via:
```
camel.springboot.routes-include-pattern = file:./routes/*.xml
```
The app must be run with `digital-me-dev/` as the working directory so relative paths resolve correctly. Modifying route files does **not** require a rebuild — restart the JVM.

### `local-file-changes.xml` (active)
- `scheduler:file-change-watcher` fires every 5 seconds
- Calls `FileChangeWatcher.watchDirectory()` for configured paths
- `file:content-receive` polls the `content-receive/` directory and processes dropped files via `ContentReceive`

---

## Key subsystems

### `FileChangeWatcher`
- Only indexes `.txt` files (other extensions are ignored)
- Path with `/*` suffix triggers recursive subdirectory scanning (one level deep, then recurses)
- Compares file `lastModified` vs. DB `TIME` to skip unchanged files
- On new/changed file: calls `LuceneIndex.createOrUpdateIndex()` + `TextEntryDao.insert/update()`

### `LuceneIndex` (static utility class)
- Index path defaults to `./lucene-index/` but can be overridden via `LuceneIndex.setIndexPath()` (used in tests)
- At runtime resolves to `digital-me-dev/lucene-index/`
- Document key field: `source` — delete-then-reinsert on update
- Stored fields: `source` (StringField), `name` (StringField), `body` (TextField)
- `find()` uses `QueryParser` on the `body` field, returns up to 1,000,000 hits
- `deleteIndex()` deletes all files in the index dir (used in tests)

### `DatabaseAdapter` (static utility class)
- Singleton SQLite connection; reopens if closed
- `init()` must be called at startup — runs numbered migration scripts from classpath (`digital-me-db-N.sql`)
- Migration tracking: `APPLICATION_METADATA` table with `database.version` key
- To add a migration: create `src/main/resources/digital-me-db-3.sql` (next number after the existing two)
- `setDefaultDatabasePath()` also closes and nulls the current connection, so the new path takes effect immediately

### `TextEntryDao`
- `NAME` column stores the file absolute path or URL (`source`)
- `findByName(source)` is used to check if an entry exists before insert vs. update
- Note: SQL is built via string concatenation (not parameterized for some queries)

### `EmbeddingClient` (functional interface)
- Single method: `float[] embed(String text)` — returns `null` when Ollama is unavailable
- Used as a lambda throughout; tests pass `text -> null` (unavailable) or `text -> new float[]{...}` (mock)

### `OllamaEmbeddingClient`
- Posts to `http://localhost:11434/api/embeddings` with model `nomic-embed-text`
- **Normalises text** before embedding: replaces literal `\\n`, `\\t`, `\\r` (Chrome extension artifact) with spaces, collapses whitespace
- **Caps at 3000 chars** after normalisation (nomic-embed-text has 2048-token context; Chrome extension content can be token-dense)
- Sends `options.num_ctx=2048` in every request to set the model context window
- Returns `null` on HTTP error or connection failure (caller skips indexing gracefully)

### `EmbeddingIndex`
- Runs `indexAll()` on a daemon thread at startup: loads already-indexed file paths from DB (one SELECT), then walks `mcp-resources/` and indexes new files only
- `indexFile(path)`: reads file, truncates content to **4000 chars**, calls `embeddingClient.embed()`, stores result in `MCP_EMBEDDING` via `McpEmbeddingDao`
- `findSimilar(query, topK)`: embeds the query, loads all stored embeddings, ranks by cosine similarity, returns top-K `ScoredResult` records
- First line of each file is the source URL (written by `ResourceReceiver`); subsequent lines are the content

### `McpEmbeddingDao`
- `upsert(McpEmbedding)` — INSERT OR REPLACE into `MCP_EMBEDDING`
- `findAll()` — returns list of `McpEmbedding` (reads FILE_PATH, SOURCE_URL, EMBEDDING columns only)
- `findAllFilePaths()` — returns `Set<String>` of already-indexed paths (used by `indexAll()` to skip re-indexing)

---

## Database schema

```sql
APPLICATION_METADATA (KEY PK, VALUE)   -- stores database.version
TEXT_ENTRY (UUID PK, TIME, NAME)        -- indexed content entries
TEXT_ENTRY_METADATA (TEXT_ENTRY_UUID, KEY, VALUE, PK composite)
MCP_EMBEDDING (FILE_PATH PK, SOURCE_URL, EMBEDDING BLOB, INDEXED_AT)  -- vector embeddings for semantic search
```

`TIME` and `INDEXED_AT` are stored as ISO-8601 instant strings (e.g. `2024-01-15T10:30:00Z`).
`EMBEDDING` is a raw `BLOB` of packed IEEE 754 floats (4 bytes each, big-endian via `ByteBuffer`).

---

## Frontend (`frontend/`)

- Single `App.jsx` component — no router
- Pagination: 10 results per page (`PAGE_SIZE = 10`)
- Local file results: linked to `/localFile?filePath=<encoded-path>`
- Web results: linked directly to the URL
- Labels truncated to 90 characters in the result list

---

## Chrome extension (`chrome-extension/`)

- Manifest V3, permissions: `activeTab`, `tabs`, `webNavigation`, `scripting`
- `content-script.js` runs on all `http://` and `https://` pages
- `background.js` (service worker) POSTs page content to `http://localhost:8080/addContent`

---

## Actuator endpoints

Exposed at `/actuator`:
- `health` — `GET /actuator/health`
- `info` — `GET /actuator/info`
- `camelroutes` — `GET /actuator/camelroutes` (read-only)
