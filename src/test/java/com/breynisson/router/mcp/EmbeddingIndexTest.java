package com.breynisson.router.mcp;

import com.breynisson.router.jdbc.DatabaseAdapter;
import com.breynisson.router.jdbc.McpEmbeddingDao;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class EmbeddingIndexTest {

    @TempDir
    static Path dbDir;

    @TempDir
    Path dataDir;

    @BeforeAll
    static void setUpDatabase() {
        DatabaseAdapter.setDefaultDatabasePath(dbDir.resolve("test.db").toString());
        DatabaseAdapter.init();
    }

    @AfterAll
    static void tearDownDatabase() {
        DatabaseAdapter.setDefaultDatabasePath(null);
    }

    private static void cleanup(Path... files) {
        for (Path f : files)
            DatabaseAdapter.runSql("DELETE FROM MCP_EMBEDDING WHERE FILE_PATH='" + f.toAbsolutePath() + "'");
    }

    @Test
    void indexFileStoresEmbedding() throws Exception {
        Path file = dataDir.resolve("page.txt");
        Files.writeString(file, "http://example.com\nhello world");

        EmbeddingIndex index = new EmbeddingIndex(text -> new float[]{1.0f, 0.0f}, dataDir.toString());
        index.indexFile(file);

        assertTrue(McpEmbeddingDao.findAllFilePaths().contains(file.toAbsolutePath().toString()));
        cleanup(file);
    }

    @Test
    void indexFileSkipsWhenOllamaUnavailable() throws Exception {
        Path file = dataDir.resolve("unavailable.txt");
        Files.writeString(file, "http://example.com\ncontent");

        EmbeddingIndex index = new EmbeddingIndex(text -> null, dataDir.toString());
        index.indexFile(file);

        assertFalse(McpEmbeddingDao.findAllFilePaths().contains(file.toAbsolutePath().toString()));
    }

    @Test
    void indexAllSkipsAlreadyIndexedFiles() throws Exception {
        Path mcpDir = Files.createDirectories(dataDir.resolve("mcp-resources").resolve("2026-03"));
        Path file = mcpDir.resolve("once.txt");
        Files.writeString(file, "http://example.com\ncontent");

        AtomicInteger callCount = new AtomicInteger();
        EmbeddingIndex index = new EmbeddingIndex(text -> {
            callCount.incrementAndGet();
            return new float[]{1.0f};
        }, dataDir.toString());

        index.indexAll();
        int afterFirst = callCount.get();
        index.indexAll(); // already indexed — should not call embed again

        assertEquals(afterFirst, callCount.get());
        cleanup(file);
    }

    @Test
    void findSimilarRanksCloserResultFirst() throws Exception {
        Path dir = Files.createDirectories(dataDir.resolve("mcp-resources").resolve("2026-03"));
        Path fileA = dir.resolve("a.txt");
        Path fileB = dir.resolve("b.txt");
        Files.writeString(fileA, "http://a.com\ndoc a");
        Files.writeString(fileB, "http://b.com\ndoc b");

        // fileA embedding [1,0], fileB embedding [0,1], query [1,0] → fileA scores 1.0, fileB scores 0.0
        EmbeddingIndex index = new EmbeddingIndex(text -> {
            if (text.contains("doc a")) return new float[]{1.0f, 0.0f};
            if (text.contains("doc b")) return new float[]{0.0f, 1.0f};
            return new float[]{1.0f, 0.0f}; // query
        }, dataDir.toString());

        index.indexFile(fileA);
        index.indexFile(fileB);

        List<EmbeddingIndex.ScoredResult> results = index.findSimilar("query", 2);
        assertEquals(2, results.size());
        assertEquals("http://a.com", results.get(0).sourceUrl());
        assertTrue(results.get(0).score() > results.get(1).score());
        cleanup(fileA, fileB);
    }

    @Test
    void findSimilarReturnsEmptyWhenOllamaUnavailable() {
        EmbeddingIndex index = new EmbeddingIndex(text -> null, dataDir.toString());
        assertTrue(index.findSimilar("query", 5).isEmpty());
    }

    @Test
    void indexFileTruncatesContentBeyond4000Chars() throws Exception {
        Path file = dataDir.resolve("large.txt");
        Files.writeString(file, "http://example.com\n" + "x".repeat(10_000));

        String[] captured = {null};
        EmbeddingIndex index = new EmbeddingIndex(text -> {
            captured[0] = text;
            return new float[]{1.0f};
        }, dataDir.toString());

        index.indexFile(file);
        assertNotNull(captured[0]);
        assertTrue(captured[0].length() <= 4_000);
        cleanup(file);
    }
}
