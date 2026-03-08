package com.breynisson.router.mcp;

import com.breynisson.router.digitalme.AddContentRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.YearMonth;
import java.util.regex.Pattern;

public class ResourceReceiver {

    private static final Logger log = LoggerFactory.getLogger(ResourceReceiver.class);
    private static final String MCP_RESOURCES_DIR = "mcp-resources";
    private static final Pattern INVALID_CHARS = Pattern.compile("[\\\\/:*?\"<>|\\s]");

    private final Path mcpResourcesDir;
    private YearMonth cachedMonth;
    private Path cachedMonthDir;

    public ResourceReceiver(String dataDir) {
        this.mcpResourcesDir = Paths.get(dataDir, MCP_RESOURCES_DIR);
    }

    public void addContent(AddContentRequest request) throws IOException {
        String rawName = request.getName() != null ? request.getName() : request.getSource();
        String filename = INVALID_CHARS.matcher(rawName).replaceAll("_");
        Path monthDir = monthDir();
        Files.writeString(monthDir.resolve(filename), request.getContent());
        log.info("Wrote resource: {}/{}", monthDir.getFileName(), filename);
    }

    private Path monthDir() throws IOException {
        YearMonth yearMonth = YearMonth.now();
        if (!yearMonth.equals(cachedMonth)) {
            cachedMonth = yearMonth;
            cachedMonthDir = mcpResourcesDir.resolve(yearMonth.toString());
            Files.createDirectories(cachedMonthDir);
        }
        return cachedMonthDir;
    }
}
