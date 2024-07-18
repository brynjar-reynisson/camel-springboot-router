package com.breynisson.router.history;

import com.breynisson.router.Constants;
import com.breynisson.router.RouterException;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class HistoryUrlDownloadProcessor implements Processor {
    @Override
    public void process(Exchange exchange) throws Exception {
        HistoryUrl historyUrl = exchange.getMessage().getBody(HistoryUrl.class);
        if(historyUrl == null || historyUrl.url == null) {
            throw new RouterException("No url in message");
        }
        exchange.setProperty(Constants.MSG_PROPERTY_HISTORY_URL, historyUrl.url);
        try (BufferedInputStream in = new BufferedInputStream(new URL(historyUrl.url).openStream());
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] dataBuffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = in.read(dataBuffer, 0, 1024)) != -1) {
                out.write(dataBuffer, 0, bytesRead);
            }
            exchange.getMessage().setBody(out.toString(StandardCharsets.UTF_8));
        } catch (IOException e) {
            exchange.getMessage().setBody("<html><body><p>" + e.getMessage() + "</p></body></html>");
        }
    }
}
