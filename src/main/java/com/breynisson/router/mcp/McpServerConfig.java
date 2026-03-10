package com.breynisson.router.mcp;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.server.McpSyncServerExchange;
import io.modelcontextprotocol.server.transport.HttpServletStreamableServerTransportProvider;
import io.modelcontextprotocol.spec.McpSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.stream.Stream;

@Configuration
public class McpServerConfig {

    private static final Logger log = LoggerFactory.getLogger(McpServerConfig.class);
    private static final int MAX_RESPONSE_CHARS = 900_000;
    private static final int JSON_WRAPPER_OVERHEAD = 14;  // {"results":[]}
    private static final int JSON_ENTRY_OVERHEAD = 35;    // per-entry: keys, quotes, colons, comma
    private static final int SNIPPET_CHARS = 2_000;

    final Path mcpResourcesDir;
    private final Path normalizedBase;
    private final ObjectMapper objectMapper;
    private final EmbeddingIndex embeddingIndex;

    public McpServerConfig(
            @Value("${data.dir:.}") String dataDir,
            ObjectMapper objectMapper,
            EmbeddingIndex embeddingIndex) {
        this.mcpResourcesDir = Paths.get(dataDir, ResourceReceiver.MCP_RESOURCES_DIR);
        this.normalizedBase = mcpResourcesDir.normalize().toAbsolutePath();
        this.objectMapper = objectMapper;
        this.embeddingIndex = embeddingIndex;
    }

    @Bean
    HttpServletStreamableServerTransportProvider mcpTransport() {
        return HttpServletStreamableServerTransportProvider.builder()
                .mcpEndpoint("/mcp")
                .build();
    }

    @Bean
    ServletRegistrationBean<HttpServletStreamableServerTransportProvider> mcpServlet(
            HttpServletStreamableServerTransportProvider transport) {
        ServletRegistrationBean<HttpServletStreamableServerTransportProvider> bean =
                new ServletRegistrationBean<>(transport, "/mcp");
        bean.setAsyncSupported(true);
        bean.setName("mcp");
        return bean;
    }

    @Bean(destroyMethod = "close")
    McpSyncServer mcpServer(HttpServletStreamableServerTransportProvider transport) {
        McpSyncServer server = McpServer.sync(transport)
                .serverInfo("digital-me", "1.0.0")
                .capabilities(McpSchema.ServerCapabilities.builder()
                        .resources(false, true)
                        .tools(true)
                        .build())
                .toolCall(buildSearchTool(), buildSearchHandler())
                .build();
        for (McpServerFeatures.SyncResourceSpecification spec : buildResourceSpecs()) {
            server.addResource(spec);
        }
        return server;
    }

    // Package-private for testability
    List<McpServerFeatures.SyncResourceSpecification> buildResourceSpecs() {
        List<McpServerFeatures.SyncResourceSpecification> specs = new ArrayList<>();
        BiFunction<McpSyncServerExchange, McpSchema.ReadResourceRequest, McpSchema.ReadResourceResult> handler =
                buildReadHandler();
        try (Stream<Path> walk = Files.walk(mcpResourcesDir)) {
            walk.filter(Files::isRegularFile).forEach(file -> {
                String uri = file.toUri().toString();
                String name = file.getFileName().toString();
                String description = mcpResourcesDir.relativize(file).toString().replace('\\', '/');
                McpSchema.Resource resource = McpSchema.Resource.builder()
                        .uri(uri).name(name).description(description).mimeType("text/plain").build();
                specs.add(new McpServerFeatures.SyncResourceSpecification(resource, handler));
            });
        } catch (IOException e) {
            log.warn("Failed to scan {}", mcpResourcesDir, e);
        }
        return specs;
    }

    // Package-private for testability
    BiFunction<McpSyncServerExchange, McpSchema.ReadResourceRequest, McpSchema.ReadResourceResult> buildReadHandler() {
        return (exchange, request) -> {
            Path requested;
            try {
                requested = Paths.get(URI.create(request.uri())).normalize().toAbsolutePath();
            } catch (Exception e) {
                return errorReadResult("Invalid URI: " + request.uri());
            }
            if (!requested.startsWith(normalizedBase)) {
                return errorReadResult("Access denied: path outside mcp-resources");
            }
            try {
                String content = Files.readString(requested, StandardCharsets.UTF_8);
                if (content.length() > MAX_RESPONSE_CHARS) {
                    content = content.substring(0, MAX_RESPONSE_CHARS) + "\n[truncated]";
                }
                return new McpSchema.ReadResourceResult(
                        List.of(new McpSchema.TextResourceContents(request.uri(), "text/plain", content)));
            } catch (NoSuchFileException e) {
                return errorReadResult("File not found: " + request.uri());
            } catch (IOException e) {
                log.warn("Error reading resource {}", request.uri(), e);
                return errorReadResult("Error reading file: " + e.getMessage());
            }
        };
    }

    /** Returns top-20 semantically similar results; empty list if Ollama is unavailable. */
    private List<Map<String, String>> semanticSearch(String query) {
        return embeddingIndex.findSimilar(query, 20).stream()
                .map(r -> {
                    Path p = Path.of(r.filePath());
                    String snip = "";
                    try {
                        snip = snippet(Files.readString(p, StandardCharsets.UTF_8));
                    } catch (IOException e) {
                        log.warn("Could not read {} for snippet", r.filePath());
                    }
                    return Map.of("source", r.sourceUrl(),
                                  "name",   p.getFileName().toString(),
                                  "snippet", snip);
                })
                .toList();
    }

    /** Case-insensitive OR keyword scan across all files in mcp-resources/. */
    private List<Map<String, String>> keywordSearch(String query) {
        String[] terms = query.toLowerCase().split("\\s+");
        List<Map<String, String>> results = new ArrayList<>();
        try (Stream<Path> walk = Files.walk(mcpResourcesDir)) {
            walk.filter(Files::isRegularFile).forEach(file -> {
                try {
                    String raw = Files.readString(file, StandardCharsets.UTF_8);
                    String lower = raw.toLowerCase();
                    // OR logic: include file if any term matches
                    for (String term : terms) {
                        if (lower.contains(term)) {
                            String source = ResourceReceiver.firstLine(raw);
                            results.add(Map.of("source", source, "name", file.getFileName().toString(),
                                               "snippet", snippet(raw)));
                            break;
                        }
                    }
                } catch (IOException e) {
                    log.warn("Error reading {}", file, e);
                }
            });
        } catch (IOException e) {
            log.warn("Failed to walk {}", mcpResourcesDir, e);
        }
        return results;
    }

    private McpSchema.CallToolResult buildSearchResult(List<Map<String, String>> results) throws Exception {
        int sizeEst = JSON_WRAPPER_OVERHEAD;
        int limit = results.size();
        for (int i = 0; i < results.size(); i++) {
            Map<String, String> e = results.get(i);
            sizeEst += e.get("source").length() + e.get("name").length()
                     + e.getOrDefault("snippet", "").length() + JSON_ENTRY_OVERHEAD;
            if (sizeEst > MAX_RESPONSE_CHARS) { limit = i; break; }
        }
        boolean truncated = limit < results.size();
        Map<String, Object> payload = truncated
                ? Map.of("results", results.subList(0, limit), "truncated", true, "total", results.size())
                : Map.of("results", results);
        return McpSchema.CallToolResult.builder()
                .addTextContent(objectMapper.writeValueAsString(payload)).build();
    }

    /** Extracts content after the first line (source URL), normalised and capped at SNIPPET_CHARS. */
    private static String snippet(String raw) {
        int nl = raw.indexOf('\n');
        String body = nl >= 0 ? raw.substring(nl + 1) : "";
        // Cap before normalising so regex only runs on the portion we'll actually use
        if (body.length() > SNIPPET_CHARS) body = body.substring(0, SNIPPET_CHARS);
        return body.replace("\\n", " ").replace("\\t", " ").replace("\\r", " ")
                   .replaceAll("\\s+", " ").strip();
    }

    private static McpSchema.ReadResourceResult errorReadResult(String message) {
        return new McpSchema.ReadResourceResult(
                List.of(new McpSchema.TextResourceContents("", "text/plain", message)));
    }

    // Package-private for testability
    McpSchema.Tool buildSearchTool() {
        McpSchema.JsonSchema inputSchema = new McpSchema.JsonSchema(
                "object",
                Map.of("keywords", Map.of("type", "string", "description", "Search terms")),
                List.of("keywords"),
                null, null, null);
        return McpSchema.Tool.builder()
                .name("search")
                .description("Search the Digital Me personal index for content matching the given keywords")
                .inputSchema(inputSchema)
                .build();
    }

    // Package-private for testability
    BiFunction<McpSyncServerExchange, McpSchema.CallToolRequest, McpSchema.CallToolResult> buildSearchHandler() {
        return (exchange, request) -> {
            Object kw = request.arguments() != null ? request.arguments().get("keywords") : null;
            if (kw == null || kw.toString().isBlank()) {
                return McpSchema.CallToolResult.builder().isError(true)
                        .addTextContent("Missing required argument: keywords").build();
            }
            try {
                // Semantic search via embeddings; falls back to keyword scan when Ollama is unavailable
                List<Map<String, String>> results = semanticSearch(kw.toString());
                if (results.isEmpty()) {
                    results = keywordSearch(kw.toString());
                }
                return buildSearchResult(results);
            } catch (Exception e) {
                log.warn("Search failed for keywords '{}'", kw, e);
                return McpSchema.CallToolResult.builder().isError(true)
                        .addTextContent("Search failed: " + e.getMessage()).build();
            }
        };
    }
}
