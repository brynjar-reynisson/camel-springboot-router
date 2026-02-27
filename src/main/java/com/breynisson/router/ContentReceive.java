package com.breynisson.router;

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;

public class ContentReceive implements Processor {
    private final CamelContext camelContext;

    public ContentReceive(CamelContext camelContext) {
        this.camelContext = camelContext;
    }

    @Override
    public void process(Exchange exchange) throws Exception {
        String body = exchange.getMessage().getBody(String.class);
        System.out.println(body);
    }
}
