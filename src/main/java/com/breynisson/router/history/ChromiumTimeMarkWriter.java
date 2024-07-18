package com.breynisson.router.history;

import com.breynisson.router.Constants;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static com.breynisson.router.Constants.*;

public class ChromiumTimeMarkWriter implements Processor {

    @Override
    public void process(Exchange exchange) throws Exception {
        String markerFileName = (String) exchange.getProperty(MSG_PROPERTY_URL_HISTORY_MARKER_FILE);
        Path markerFilePath = Paths.get(markerFileName);
        long lastTimeMarker = exchange.getProperty(MSG_PROPERTY_URL_HISTORY_LAST_TIME_MARKER, Long.class);
        Files.write(markerFilePath, ("" + lastTimeMarker).getBytes());
    }
}
