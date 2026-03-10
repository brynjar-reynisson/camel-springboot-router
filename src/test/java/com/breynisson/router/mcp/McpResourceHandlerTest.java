package com.breynisson.router.mcp;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.spec.McpSchema;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.function.BiFunction;

import static org.junit.jupiter.api.Assertions.*;

class McpResourceHandlerTest {

    @TempDir
    Path dataDir;
    McpServerConfig config;

    @BeforeEach
    void setUp() {
        EmbeddingIndex embeddingIndex = new EmbeddingIndex(text -> null, dataDir.toString());
        config = new McpServerConfig(dataDir.toString(), new ObjectMapper(), embeddingIndex);
    }

    @Test
    void listReturnsEmptyWhenDirAbsent() {
        List<McpServerFeatures.SyncResourceSpecification> specs = config.buildResourceSpecs();
        assertTrue(specs.isEmpty());
    }

    @Test
    void listReturnsFilesAfterWrite() throws Exception {
        Path monthDir = dataDir.resolve("mcp-resources").resolve("2026-03");
        Files.createDirectories(monthDir);
        Files.writeString(monthDir.resolve("test.txt"), "hello");

        List<McpServerFeatures.SyncResourceSpecification> specs = config.buildResourceSpecs();

        assertEquals(1, specs.size());
        McpSchema.Resource r = specs.get(0).resource();
        assertEquals("test.txt", r.name());
        assertTrue(r.uri().endsWith("test.txt"));
        assertEquals("text/plain", r.mimeType());
    }

    @Test
    void readReturnsContent() throws Exception {
        Path monthDir = dataDir.resolve("mcp-resources").resolve("2026-03");
        Files.createDirectories(monthDir);
        Path file = monthDir.resolve("hello.txt");
        Files.writeString(file, "expected content");

        BiFunction<io.modelcontextprotocol.server.McpSyncServerExchange, McpSchema.ReadResourceRequest, McpSchema.ReadResourceResult> handler =
                config.buildReadHandler();

        String uri = file.toUri().toString();
        McpSchema.ReadResourceResult result = handler.apply(null, new McpSchema.ReadResourceRequest(uri));

        assertEquals(1, result.contents().size());
        McpSchema.TextResourceContents contents = (McpSchema.TextResourceContents) result.contents().get(0);
        assertEquals("expected content", contents.text());
    }

    @Test
    void readRejectsPathTraversal() throws Exception {
        Files.createDirectories(dataDir.resolve("mcp-resources"));
        Path outsideFile = dataDir.resolve("secret.txt");
        Files.writeString(outsideFile, "sensitive");

        BiFunction<io.modelcontextprotocol.server.McpSyncServerExchange, McpSchema.ReadResourceRequest, McpSchema.ReadResourceResult> handler =
                config.buildReadHandler();

        McpSchema.ReadResourceResult result = handler.apply(null,
                new McpSchema.ReadResourceRequest(outsideFile.toUri().toString()));

        McpSchema.TextResourceContents contents = (McpSchema.TextResourceContents) result.contents().get(0);
        assertTrue(contents.text().contains("Access denied") || contents.text().contains("outside"),
                "Expected access denied error, got: " + contents.text());
    }
}
