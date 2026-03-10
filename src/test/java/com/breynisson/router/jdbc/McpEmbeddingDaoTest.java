package com.breynisson.router.jdbc;

import com.breynisson.router.jdbc.model.McpEmbedding;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class McpEmbeddingDaoTest {

    @TempDir
    static Path dbDir;

    @BeforeAll
    static void setUpDatabase() {
        DatabaseAdapter.setDefaultDatabasePath(dbDir.resolve("test.db").toString());
        DatabaseAdapter.init();
    }

    @AfterAll
    static void tearDownDatabase() {
        DatabaseAdapter.setDefaultDatabasePath(null);
    }

    private static byte[] embeddingBytes(float... values) {
        ByteBuffer buf = ByteBuffer.allocate(values.length * Float.BYTES);
        for (float v : values) buf.putFloat(v);
        return buf.array();
    }

    private static void cleanup(String filePath) {
        DatabaseAdapter.runSql("DELETE FROM MCP_EMBEDDING WHERE FILE_PATH='" + filePath + "'");
    }

    @Test
    void upsertAndFindAll() {
        String path = "/tmp/dao-test-1.txt";
        McpEmbeddingDao.upsert(new McpEmbedding(path, "http://example.com/1", embeddingBytes(1.0f, 2.0f), "2026-01-01T00:00:00Z"));

        List<McpEmbedding> all = McpEmbeddingDao.findAll();
        assertTrue(all.stream().anyMatch(e -> e.filePath.equals(path) && e.sourceUrl.equals("http://example.com/1")));
        cleanup(path);
    }

    @Test
    void findAllFilePathsReturnsStoredPath() {
        String path = "/tmp/dao-test-2.txt";
        McpEmbeddingDao.upsert(new McpEmbedding(path, "http://example.com/2", embeddingBytes(1.0f), "2026-01-01T00:00:00Z"));

        Set<String> paths = McpEmbeddingDao.findAllFilePaths();
        assertTrue(paths.contains(path));
        cleanup(path);
    }

    @Test
    void upsertReplacesPreviousEntry() {
        String path = "/tmp/dao-test-3.txt";
        McpEmbeddingDao.upsert(new McpEmbedding(path, "http://old.com", embeddingBytes(1.0f), "2026-01-01T00:00:00Z"));
        McpEmbeddingDao.upsert(new McpEmbedding(path, "http://new.com", embeddingBytes(2.0f), "2026-01-02T00:00:00Z"));

        List<McpEmbedding> matching = McpEmbeddingDao.findAll().stream()
                .filter(e -> e.filePath.equals(path)).toList();
        assertEquals(1, matching.size());
        assertEquals("http://new.com", matching.get(0).sourceUrl);
        cleanup(path);
    }

    @Test
    void findAllEmbeddingBytesRoundTrip() {
        String path = "/tmp/dao-test-4.txt";
        float[] original = {0.1f, 0.5f, -0.3f};
        McpEmbeddingDao.upsert(new McpEmbedding(path, "http://example.com/4", embeddingBytes(original), "2026-01-01T00:00:00Z"));

        McpEmbedding stored = McpEmbeddingDao.findAll().stream()
                .filter(e -> e.filePath.equals(path)).findFirst().orElseThrow();
        ByteBuffer buf = ByteBuffer.wrap(stored.embedding);
        assertArrayEquals(original, new float[]{buf.getFloat(), buf.getFloat(), buf.getFloat()}, 0.0001f);
        cleanup(path);
    }
}
