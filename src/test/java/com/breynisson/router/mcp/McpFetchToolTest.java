package com.breynisson.router.mcp;

import com.breynisson.router.digitalme.SemanticSearch;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.spec.McpSchema;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.function.BiFunction;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class McpFetchToolTest {

    @TempDir
    Path dataDir;
    McpServerConfig config;

    @BeforeEach
    void setUp() {
        EmbeddingIndex embeddingIndex = new EmbeddingIndex(text -> null, dataDir.toString());
        SemanticSearch semanticSearch = new SemanticSearch(embeddingIndex, dataDir.toString());
        config = new McpServerConfig(dataDir.toString(), new ObjectMapper(), semanticSearch);
    }

    @Test
    void fetchReturnsFullContent() throws Exception {
        Path monthDir = dataDir.resolve("mcp-resources").resolve("2026-03");
        Files.createDirectories(monthDir);
        Files.writeString(monthDir.resolve("doc.txt"), "http://example.com/page\nFull document content here.");

        BiFunction<io.modelcontextprotocol.server.McpSyncServerExchange, McpSchema.CallToolRequest, McpSchema.CallToolResult> handler =
                config.buildFetchHandler();

        McpSchema.CallToolResult result = handler.apply(null,
                new McpSchema.CallToolRequest("fetch", Map.of("filename", "doc.txt")));

        assertFalse(result.isError());
        String text = ((McpSchema.TextContent) result.content().get(0)).text();
        assertTrue(text.contains("Full document content here."), "Expected full content: " + text);
        assertTrue(text.contains("http://example.com/page"), "Expected source URL in content: " + text);
    }

    @Test
    void fetchReturnsErrorForUnknownFile() {
        BiFunction<io.modelcontextprotocol.server.McpSyncServerExchange, McpSchema.CallToolRequest, McpSchema.CallToolResult> handler =
                config.buildFetchHandler();

        McpSchema.CallToolResult result = handler.apply(null,
                new McpSchema.CallToolRequest("fetch", Map.of("filename", "nonexistent.txt")));

        assertTrue(result.isError());
    }

    @Test
    void fetchHandlesMissingFilename() {
        BiFunction<io.modelcontextprotocol.server.McpSyncServerExchange, McpSchema.CallToolRequest, McpSchema.CallToolResult> handler =
                config.buildFetchHandler();

        McpSchema.CallToolResult result = handler.apply(null,
                new McpSchema.CallToolRequest("fetch", Map.of()));

        assertTrue(result.isError());
    }
}
