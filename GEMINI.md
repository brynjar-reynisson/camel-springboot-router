# GEMINI.md - Digital Me Project Mandates

This document outlines the project-specific standards, conventions, and workflows for the Digital Me project. These mandates take precedence over general defaults.

## Core Workflows
- **Build & Run:** Use `mvn package` for a full build (includes frontend). Use `java -jar target/digital-me-0.1.jar` to run the backend.
- **Frontend Development:** Use `cd frontend && npm run dev` for the Vite dev server (proxies to backend at `localhost:8080`).
- **Testing:** Always run `mvn test` to verify changes. JUnit 5 is the standard.
- **Checkstyle:** Project enforces Checkstyle rules. Run `mvn checkstyle:check` manually to verify Java changes. Maven is at `cmd //c "C:\Program Files\JetBrains\IntelliJ IDEA Community Edition 2023.3.5\plugins\maven\lib\maven3\bin\mvn.cmd"`.

## Technical Standards
- **Backend:** Spring Boot 3.3.11, Apache Camel 4.8.0, Java 19, Maven.
- **Frontend:** React 19, Vite 7, TypeScript 5 (strict). Prefer Vanilla CSS.
- **Database:** SQLite via `sqlite-jdbc`. Use `DatabaseAdapter` for migrations and connections.
- **Search:** Apache Lucene for keyword search; Ollama (`nomic-embed-text`) for semantic search.
- **MCP:** Implements MCP SDK 1.0.0; server at `POST /mcp`.
- **Chrome Extension:** Manifest V3; sends content to `/addContent`.

## Coding Conventions
- **Java:** Follow Checkstyle rules (no unused imports, `equals-avoid-null`, etc.).
- **Database Migrations:** Add new `.sql` files to `src/main/resources/` (e.g., `digital-me-db-3.sql`) to trigger automatic migration via `DatabaseAdapter.init()`.
- **Lucene Indexing:** Use `LuceneIndex` utility. Always delete-then-reinsert on updates.
- **Semantic Search:** Normalize text (replace `\\n`, `\\t`, `\\r` with spaces) and cap at 3000 chars before embedding.
- **Exclusion Rules:** Use `ExclusionRules` to filter noise from search results (e.g., localhost, google, facebook).

## Testing Patterns
- **Database Isolation:** Use `@TempDir` for DB path in tests. Call `DatabaseAdapter.setDefaultDatabasePath()` in `@BeforeAll` and `DatabaseAdapter.setDefaultDatabasePath(null)` in `@AfterAll`.
- **Lucene Isolation:** Use `LuceneIndex.setIndexPath()` with `@TempDir` in `@BeforeEach`.
- **Mocking:** Use functional interface lambdas for `EmbeddingClient` and `SummarizeClient` mocks instead of Mockito where possible.
- **Integration Tests:** For Ollama, mock HTTP responses using JDK `HttpServer`.

## System Integrity
- **Runtime Data:** `digital-me-dev/` is the runtime data directory (gitignored). Run the app with this as the working directory to resolve relative paths for Lucene and SQLite.
- **Camel Routes:** Loaded from `digital-me-dev/routes/*.xml` at runtime. No rebuild needed for XML changes, but JVM restart is required.
