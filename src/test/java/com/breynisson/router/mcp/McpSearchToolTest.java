package com.breynisson.router.mcp;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.spec.McpSchema;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.function.BiFunction;

import static org.junit.jupiter.api.Assertions.*;

class McpSearchToolTest {

    @TempDir
    Path dataDir;
    McpServerConfig config;

    @BeforeEach
    void setUp() {
        EmbeddingIndex embeddingIndex = new EmbeddingIndex(text -> null, dataDir.toString());
        config = new McpServerConfig(dataDir.toString(), new ObjectMapper(), embeddingIndex);
    }

    @Test
    void searchReturnsResultsAsJson() throws Exception {
        Path monthDir = dataDir.resolve("mcp-resources").resolve("2026-03");
        Files.createDirectories(monthDir);
        Files.writeString(monthDir.resolve("test.txt"), "http://example.com/page\ndigital me rocks");

        BiFunction<io.modelcontextprotocol.server.McpSyncServerExchange, McpSchema.CallToolRequest, McpSchema.CallToolResult> handler =
                config.buildSearchHandler();

        McpSchema.CallToolResult result = handler.apply(null,
                new McpSchema.CallToolRequest("search", Map.of("keywords", "digital me")));

        assertFalse(result.isError());
        String text = ((McpSchema.TextContent) result.content().get(0)).text();
        assertTrue(text.contains("http://example.com/page"), "Expected source in JSON: " + text);
        assertTrue(text.contains("digital me rocks"), "Expected snippet content in JSON: " + text);
    }

    @Test
    void searchReturnsEmptyJson() throws Exception {
        BiFunction<io.modelcontextprotocol.server.McpSyncServerExchange, McpSchema.CallToolRequest, McpSchema.CallToolResult> handler =
                config.buildSearchHandler();

        McpSchema.CallToolResult result = handler.apply(null,
                new McpSchema.CallToolRequest("search", Map.of("keywords", "nonexistent")));

        assertFalse(result.isError());
        String text = ((McpSchema.TextContent) result.content().get(0)).text();
        assertTrue(text.contains("\"results\":[]") || text.contains("\"results\": []"),
                "Expected empty results: " + text);
    }

    @Test
    void snippetIsTruncatedWithFetchHint() throws Exception {
        Path monthDir = dataDir.resolve("mcp-resources").resolve("2026-03");
        Files.createDirectories(monthDir);
        String longContent = "http://example.com/page\n" + "x".repeat(3000);
        Files.writeString(monthDir.resolve("long.txt"), longContent);

        BiFunction<io.modelcontextprotocol.server.McpSyncServerExchange, McpSchema.CallToolRequest, McpSchema.CallToolResult> handler =
                config.buildSearchHandler();

        McpSchema.CallToolResult result = handler.apply(null,
                new McpSchema.CallToolRequest("search", Map.of("keywords", "x")));

        assertFalse(result.isError());
        String text = ((McpSchema.TextContent) result.content().get(0)).text();
        assertTrue(text.contains("<truncated, use fetch tool>"), "Expected truncation hint: " + text);
    }

    @Test
    void snippetNotTruncatedWhenShort() throws Exception {
        Path monthDir = dataDir.resolve("mcp-resources").resolve("2026-03");
        Files.createDirectories(monthDir);
        Files.writeString(monthDir.resolve("short.txt"), "http://example.com/page\nshort content");

        BiFunction<io.modelcontextprotocol.server.McpSyncServerExchange, McpSchema.CallToolRequest, McpSchema.CallToolResult> handler =
                config.buildSearchHandler();

        McpSchema.CallToolResult result = handler.apply(null,
                new McpSchema.CallToolRequest("search", Map.of("keywords", "short")));

        assertFalse(result.isError());
        String text = ((McpSchema.TextContent) result.content().get(0)).text();
        assertFalse(text.contains("<truncated, use fetch tool>"), "Expected no truncation hint: " + text);
    }

    @Test
    void searchHandlesMissingKeyword() {
        BiFunction<io.modelcontextprotocol.server.McpSyncServerExchange, McpSchema.CallToolRequest, McpSchema.CallToolResult> handler =
                config.buildSearchHandler();

        McpSchema.CallToolResult result = handler.apply(null,
                new McpSchema.CallToolRequest("search", Map.of()));

        assertTrue(result.isError());
    }
}
