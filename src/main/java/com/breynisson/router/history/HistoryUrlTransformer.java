package com.breynisson.router.history;

import com.breynisson.router.RouterException;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;

public class HistoryUrlTransformer implements Processor {
    @Override
    public void process(Exchange exchange) throws Exception {
        HistoryUrl historyUrl = exchange.getMessage().getBody(HistoryUrl.class);
        if(historyUrl == null || historyUrl.url == null) {
            throw new RouterException("No url in message");
        }

        String newUrl = null;
        if(historyUrl.url.startsWith("https://www.google.com/search?")) {
            newUrl = trimFromFirstAmpersand(historyUrl.url);
        }

        if(newUrl != null) {
            HistoryUrl newHistoryUrl = new HistoryUrl(newUrl, historyUrl.timeMillis, historyUrl.dateStr);
            exchange.getMessage().setBody(newHistoryUrl);
        }
    }

    private String trimFromFirstAmpersand(String url) {
        int index = url.indexOf("&");
        if(index != -1) {
            return url.substring(0, index);
        } else {
            return url;
        }
    }
}
