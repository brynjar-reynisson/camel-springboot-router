package com.breynisson.router.history;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static com.breynisson.router.Constants.MSG_PROPERTY_URL_HISTORY_MARKER_FILE;
import static com.breynisson.router.Constants.MSG_PROPERTY_URL_HISTORY_TIME_MARKER;

public class ChromiumTimeMarkReader implements Processor {

    @Override
    public void process(Exchange exchange) throws Exception {
        String markerFileName = (String) exchange.getProperty(MSG_PROPERTY_URL_HISTORY_MARKER_FILE);
        Path markerFilePath = Paths.get(markerFileName);
        long timeMarker = 0;
        if(markerFilePath.toFile().isFile()) {
            String markerFileContent = Files.readString(markerFilePath);
            timeMarker = Long.parseLong(markerFileContent);
        }
        exchange.setProperty(MSG_PROPERTY_URL_HISTORY_TIME_MARKER, timeMarker);
    }
}
