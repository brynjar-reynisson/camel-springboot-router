package com.breynisson.router.history;

import com.breynisson.router.Constants;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.jsoup.Jsoup;

public class HtmlTextExtractor implements Processor {
    @Override
    public void process(Exchange exchange) throws Exception {
        String url = exchange.getProperty(Constants.MSG_PROPERTY_HISTORY_URL, String.class);
        String html = exchange.getMessage().getBody(String.class);
        String extractedText = Jsoup.parse(html).text();
        exchange.getMessage().setBody(url + "\n" + extractedText);
    }
}
