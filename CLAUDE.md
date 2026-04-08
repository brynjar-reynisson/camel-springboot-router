# CLAUDE.md — Digital Me

Personal search engine that indexes local `.txt` files and web pages visited via a Chrome extension, with full-text search via a React UI.

---

## Workflow rules

- Always run /simplify after changing source files.

---

## Tech stack

| Layer | Technology |
|---|---|
| Backend | Spring Boot 3.3.11, Apache Camel 4.8.0, Java 19, Maven |
| MCP server | MCP SDK 1.0.0 (`io.modelcontextprotocol.sdk:mcp`) |
| Web server | Undertow (Tomcat explicitly excluded in pom.xml) |
| Full-text search | Apache Lucene (via camel-lucene-starter) |
| Semantic search | Ollama local AI (`nomic-embed-text` model, 2048-token context) |
| Database | SQLite via `sqlite-jdbc` |
| HTML parsing | Jsoup |
| Frontend | React 19, Vite 7, TypeScript 5 (strict) |
| Browser extension | Chrome/Edge Manifest V3 |

---

## Project structure

```
digital-me/
├── chrome-extension/         Browser extension (Manifest V3)
│   ├── manifest.json
│   ├── content-script.js     Extracts page text, sends to background
│   └── background.js         POSTs to /addContent
├── digital-me-dev/           Runtime data directory (gitignored)
│   ├── routes/               Camel XML routes (loaded at runtime)
│   │   └── local-file-changes.xml            Active: file watcher + content-receive
│   ├── lucene-index/         Lucene index files
│   ├── content-receive/      Drop files here to trigger ContentReceive route
│   ├── mcp-resources/        Files saved by MCP clients (year-month subdirs)
│   └── digital-me.db         SQLite database
├── docs/                     Detailed documentation (imported below)
├── frontend/                 React + Vite search UI
│   └── src/
│       └── App.tsx           Single-page search UI
├── src/main/java/com/breynisson/router/
│   ├── SpringBootApplication.java   Entry point; calls DatabaseAdapter.init()
│   ├── AppConfig.java               Spring @Configuration; registers all beans
│   ├── Constants.java               Shared string constants
│   ├── FileChangeWatcher.java       Watches dirs, indexes changed .txt files
│   ├── ContentReceive.java          Camel processor for file:content-receive route
│   ├── jdbc/
│   │   ├── DatabaseAdapter.java     SQLite connection + migration runner
│   │   ├── TextEntryDao.java        CRUD for TEXT_ENTRY table
│   │   ├── TextEntryMetadataDao.java
│   │   ├── ApplicationMetadataDao.java
│   │   ├── McpEmbeddingDao.java     CRUD for MCP_EMBEDDING table
│   │   └── model/                  TextEntry, TextEntryMetadata, McpEmbedding POJOs
│   ├── lucene/
│   │   └── LuceneIndex.java         Static Lucene index helpers
│   ├── mcp/
│   │   ├── McpServerConfig.java     MCP server (resources + search + fetch tools)
│   │   ├── ResourceReceiver.java    Writes MCP client content to mcp-resources/
│   │   ├── EmbeddingClient.java     Functional interface: float[] embed(String text)
│   │   ├── OllamaEmbeddingClient.java  HTTP client for Ollama /api/embeddings
│   │   ├── EmbeddingIndex.java      SQLite-backed vector store; cosine similarity search
│   │   ├── SummarizeClient.java     Functional interface: String summarize(String text)
│   │   └── OllamaSummarizeClient.java  HTTP client for Ollama /api/generate (llama3.2)
│   ├── digitalme/
│   │   ├── DigitalMeStorage.java    Abstraction over Lucene index + SQLite
│   │   ├── DefaultDigitalMeStorage.java
│   │   ├── SemanticSearch.java      EmbeddingIndex + SummarizeClient; search + snippet + summarize
│   │   ├── ExclusionRules.java      Filters noisy sources from search results
│   │   ├── AddContentRequest.java   DTO for /addContent body
│   │   ├── AddContentResponse.java  DTO for /addContent response
│   │   ├── SearchResponse.java      DTO for /search and /semanticSearch response
│   │   └── SearchResult.java        DTO for a single search result
│   ├── extract/
│   │   └── YouTubeCaptionExtractor.java  Extracts timed captions from YouTube video IDs or URLs
│   └── ui/
│       └── IndexPage.java           REST controller (@RestController)
├── src/main/resources/
│   ├── application.properties
│   ├── digital-me-db-1.sql          DB migration script (schema v1: TEXT_ENTRY tables)
│   ├── digital-me-db-2.sql          DB migration script (schema v2: MCP_EMBEDDING table)
│   └── static/                      Built frontend assets (committed to git)
├── checkstyle.xml                   Checkstyle rules (unused imports, equals-avoid-null, etc.)
└── .claude/
    ├── settings.json                Claude Code hooks config (PostToolUse → Checkstyle)
    └── scripts/
        └── checkstyle-hook.sh      Runs mvn checkstyle:check on edited Java files
```

---

## Build and run

### Full build (backend + frontend)
```bash
mvn package
java -jar target/digital-me-0.1.jar
```
The `frontend-maven-plugin` runs `npm install` + `npm run build` automatically as part of `mvn package`. Built assets go to `src/main/resources/static/` and are committed to git.

### Frontend dev server (hot reload)
```bash
cd frontend && npm run dev
```
Proxies `/search`, `/semanticSearch`, `/summarize`, `/localFile`, `/addContent` to `localhost:8080`. The backend must be running separately.

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

@docs/architecture.md

@docs/mcp.md

@docs/testing.md

@docs/tooling.md
