# CLAUDE.md — Digital Me

Personal search engine that indexes local `.txt` files and web pages visited via a Chrome extension, with full-text search via a React UI.

---

## Workflow rules

- Always run /simplify after changing source files.

---

## Tech stack

| Layer | Technology |
|---|---|
| Backend | Spring Boot 2.7.5, Apache Camel 4.0.2, Java 19, Maven |
| Web server | Undertow (Tomcat explicitly excluded in pom.xml) |
| Full-text search | Apache Lucene (via camel-lucene-starter) |
| Database | SQLite via `sqlite-jdbc` |
| HTML parsing | Jsoup |
| Frontend | React 19, Vite 7 |
| Browser extension | Chrome/Edge Manifest V3 |

---

## Project structure

```
camel-springboot-router/
├── chrome-extension/         Browser extension (Manifest V3)
│   ├── manifest.json
│   ├── content-script.js     Extracts page text, sends to background
│   └── background.js         POSTs to /addContent
├── config/
│   └── history-url-filter.txt  URL blocklist for Edge history route
├── frontend/                 React + Vite search UI
│   └── src/
│       └── App.jsx           Single-page search UI
├── routes/                   Camel XML routes (loaded at runtime)
│   ├── local-file-changes.xml            Active: file watcher + content-receive
│   └── edge-browsing-history.xml.disabled  Disabled: rename to .xml to enable
├── src/main/java/com/breynisson/router/
│   ├── SpringBootApplication.java   Entry point; calls DatabaseAdapter.init()
│   ├── AppConfig.java               Spring @Configuration; registers all beans
│   ├── Constants.java               Shared string constants
│   ├── FileChangeWatcher.java       Watches dirs, indexes changed .txt files
│   ├── ContentReceive.java          Camel processor for file:content-receive route
│   ├── docker/                      Custom Camel component: docker:copyto
│   ├── history/                     Edge browsing history ingestion
│   ├── jdbc/
│   │   ├── DatabaseAdapter.java     SQLite connection + migration runner
│   │   ├── TextEntryDao.java        CRUD for TEXT_ENTRY table
│   │   ├── TextEntryMetadataDao.java
│   │   ├── ApplicationMetadataDao.java
│   │   └── model/                  TextEntry, TextEntryMetadata POJOs
│   ├── lucene/
│   │   └── LuceneIndex.java         Static Lucene index helpers
│   └── ui/
│       └── IndexPage.java           REST controller (@RestController)
└── src/main/resources/
    ├── application.properties
    ├── digital-me-db-1.sql          DB migration script (schema v1)
    └── static/                      Built frontend assets (committed to git)
```

---

## Build and run

### Full build (backend + frontend)
```bash
mvn package
java -jar target/camel-springboot-router-0.1.jar
```
The `frontend-maven-plugin` runs `npm install` + `npm run build` automatically as part of `mvn package`. Built assets go to `src/main/resources/static/` and are committed to git.

### Frontend dev server (hot reload)
```bash
cd frontend && npm run dev
```
Proxies `/search`, `/localFile`, `/addContent` to `localhost:8080`. The backend must be running separately.

### Run tests
```bash
mvn test
```

### Frontend only
```bash
cd frontend && npm run build   # production build
cd frontend && npm run lint    # ESLint
```

---

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

Routes are XML files in `./routes/` loaded at runtime via:
```
camel.springboot.routes-include-pattern = file:./routes/*.xml
```
Modifying route files does **not** require a rebuild — restart the JVM.

### `local-file-changes.xml` (active)
- `scheduler:file-change-watcher` fires every 5 seconds
- Calls `FileChangeWatcher.watchDirectory()` for configured paths
- `file:content-receive` polls the `content-receive/` directory and processes dropped files via `ContentReceive`

### `edge-browsing-history.xml.disabled` (disabled)
- Reads Edge browser SQLite history file on a 60-second schedule
- Filters URLs via `config/history-url-filter.txt`
- Downloads pages, extracts text, indexes into `./lucene-history-url/`
- Enable by renaming to `.xml`

---

## Key subsystems

### `FileChangeWatcher`
- Only indexes `.txt` files (other extensions are ignored)
- Path with `/*` suffix triggers recursive subdirectory scanning (one level deep, then recurses)
- Compares file `lastModified` vs. DB `TIME` to skip unchanged files
- On new/changed file: calls `LuceneIndex.createOrUpdateIndex()` + `TextEntryDao.insert/update()`

### `LuceneIndex` (static utility class)
- Index stored at `./lucene-index/` relative to the working directory
- Document key field: `source` — delete-then-reinsert on update
- Stored fields: `source` (StringField), `name` (StringField), `body` (TextField)
- `find()` uses `QueryParser` on the `body` field, returns up to 1,000,000 hits
- `deleteIndex()` deletes all files in the index dir (used in tests)

### `DatabaseAdapter` (static utility class)
- Singleton SQLite connection; reopens if closed
- `init()` must be called at startup — runs numbered migration scripts from classpath (`digital-me-db-N.sql`)
- Migration tracking: `APPLICATION_METADATA` table with `database.version` key
- To add a migration: create `src/main/resources/digital-me-db-2.sql` (next number)
- `setDefaultDatabasePath()` used in tests to point at a test DB

### `TextEntryDao`
- `NAME` column stores the file absolute path or URL (`source`)
- `findByName(source)` is used to check if an entry exists before insert vs. update
- Note: SQL is built via string concatenation (not parameterized for some queries)

### URL filter (`HistoryUrlFilter`)
- Reads `config/history-url-filter.txt` once (lazy, cached)
- Lines containing `.*` are compiled as regex; others are exact-match strings
- Returns `false` (filter out) if URL matches any rule

### Docker component (`docker/`)
- Custom Camel component registered as `docker:`
- Only supported command: `docker:copyto?container=...&destDir=...&tmpDir=...`
- Copies files into a running Docker container

---

## Database schema

```sql
APPLICATION_METADATA (KEY PK, VALUE)   -- stores database.version
TEXT_ENTRY (UUID PK, TIME, NAME)        -- indexed content entries
TEXT_ENTRY_METADATA (TEXT_ENTRY_UUID, KEY, VALUE, PK composite)
```

`TIME` is stored as ISO-8601 instant string (e.g. `2024-01-15T10:30:00Z`).

---

## Configuration (`application.properties`)

| Property | Default | Purpose |
|---|---|---|
| `edge.history.src` | `%USERPROFILE%\AppData\...\History` | Edge history SQLite file |
| `edge.history.copyfile` | same dir, `HistoryCopy` | Where Edge history is copied before reading |

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

## Testing conventions

- JUnit 5; test class names end in `Test`
- Tests hit the real SQLite DB — use `DatabaseAdapter.setDefaultDatabasePath()` to redirect to a temp DB
- Use `LuceneIndex.deleteIndex()` in `@BeforeEach`/`@AfterEach` to reset index state
- Use `@TempDir` for temp file creation
- Clean up DB rows explicitly in tests via `TextEntryDao.delete()` (no automatic rollback)
- `LuceneQuery.java` in `src/test/` is a manual query utility, not a test class

---

## Actuator endpoints

Exposed at `/actuator`:
- `health` — `GET /actuator/health`
- `info` — `GET /actuator/info`
- `camelroutes` — `GET /actuator/camelroutes` (read-only)
