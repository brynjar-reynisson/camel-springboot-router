package com.breynisson.router.mcp;

import com.breynisson.router.digitalme.DigitalMeStorage;
import com.breynisson.router.digitalme.SearchResponse;
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

    final Path mcpResourcesDir;
    private final Path normalizedBase;
    private final DigitalMeStorage storage;
    private final ObjectMapper objectMapper;

    public McpServerConfig(
            @Value("${data.dir:.}") String dataDir,
            DigitalMeStorage storage,
            ObjectMapper objectMapper) {
        this.mcpResourcesDir = Paths.get(dataDir, ResourceReceiver.MCP_RESOURCES_DIR);
        this.normalizedBase = mcpResourcesDir.normalize().toAbsolutePath();
        this.storage = storage;
        this.objectMapper = objectMapper;
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
                SearchResponse resp = storage.search(kw.toString());
                String json = objectMapper.writeValueAsString(Map.of("results",
                        resp.results.stream()
                                .map(r -> Map.of("source", r.source, "name", r.name))
                                .toList()));
                return McpSchema.CallToolResult.builder().addTextContent(json).build();
            } catch (Exception e) {
                log.warn("Search failed for keywords '{}'", kw, e);
                return McpSchema.CallToolResult.builder().isError(true)
                        .addTextContent("Search failed: " + e.getMessage()).build();
            }
        };
    }
}
