package com.breynisson.router;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;

public class ProcessingBean implements Processor {
    @Override
    public void process(Exchange exchange) throws Exception {
        exchange.getMessage().setBody("Set the body here");
    }
}
