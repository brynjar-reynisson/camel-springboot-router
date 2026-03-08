package com.breynisson.router.mcp;

import com.breynisson.router.digitalme.AddContentRequest;
import com.breynisson.router.digitalme.TestDigitalMeStorage;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.spec.McpSchema;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.Map;
import java.util.function.BiFunction;

import static org.junit.jupiter.api.Assertions.*;

class McpSearchToolTest {

    @TempDir
    Path dataDir;
    TestDigitalMeStorage storage;
    McpServerConfig config;

    @BeforeEach
    void setUp() {
        storage = new TestDigitalMeStorage();
        config = new McpServerConfig(dataDir.toString(), storage, new ObjectMapper());
    }

    @Test
    void searchReturnsResultsAsJson() throws Exception {
        AddContentRequest req = new AddContentRequest();
        req.setSource("http://example.com/page");
        req.setName("Example Page");
        req.setContent("digital me rocks");
        storage.addContent(req);

        BiFunction<io.modelcontextprotocol.server.McpSyncServerExchange, McpSchema.CallToolRequest, McpSchema.CallToolResult> handler =
                config.buildSearchHandler();

        McpSchema.CallToolResult result = handler.apply(null,
                new McpSchema.CallToolRequest("search", Map.of("keywords", "digital me")));

        assertFalse(result.isError());
        String text = ((McpSchema.TextContent) result.content().get(0)).text();
        assertTrue(text.contains("http://example.com/page"), "Expected source in JSON: " + text);
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
    void searchHandlesMissingKeyword() {
        BiFunction<io.modelcontextprotocol.server.McpSyncServerExchange, McpSchema.CallToolRequest, McpSchema.CallToolResult> handler =
                config.buildSearchHandler();

        McpSchema.CallToolResult result = handler.apply(null,
                new McpSchema.CallToolRequest("search", Map.of()));

        assertTrue(result.isError());
    }
}
