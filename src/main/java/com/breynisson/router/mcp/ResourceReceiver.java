package com.breynisson.router.mcp;

import com.breynisson.router.digitalme.AddContentRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.regex.Pattern;

public class ResourceReceiver {

    private static final Logger log = LoggerFactory.getLogger(ResourceReceiver.class);
    static final String MCP_RESOURCES_DIR = "mcp-resources";
    private static final Pattern INVALID_CHARS = Pattern.compile("[\\\\/:*?\"<>|\\s]");

    private final Path mcpResourcesDir;
    private YearMonth cachedMonth;
    private Path cachedMonthDir;

    public ResourceReceiver(String dataDir) {
        this.mcpResourcesDir = Paths.get(dataDir, MCP_RESOURCES_DIR);
    }

    public void addContent(AddContentRequest request) throws IOException {
        String rawName = request.getName() != null ? request.getName() : request.getSource();
        // prefix with day-hour-minute-second to avoid conflicts; sanitize invalid chars
        LocalDateTime now = LocalDateTime.now();
        String fileName = String.format("%02d-%02d-%02d-%02d-%s",
                now.getDayOfMonth(),
                now.getHour(),
                now.getMinute(),
                now.getSecond(),
                rawName);
        fileName = INVALID_CHARS.matcher(fileName).replaceAll("_");
        if (!fileName.toLowerCase().endsWith(".txt")) {
            fileName += ".txt";
        }
        Path monthDir = monthDir(YearMonth.from(now));
        Files.writeString(monthDir.resolve(fileName), request.getSource() + "\n" + request.getContent());
        log.info("Wrote resource: {}/{}", monthDir.getFileName(), fileName);
    }

    private Path monthDir(YearMonth yearMonth) throws IOException {
        if (!yearMonth.equals(cachedMonth)) {
            cachedMonth = yearMonth;
            cachedMonthDir = mcpResourcesDir.resolve(yearMonth.toString());
            Files.createDirectories(cachedMonthDir);
        }
        return cachedMonthDir;
    }
}
