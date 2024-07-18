package com.breynisson.router;

import com.breynisson.router.TicketDocumentFileNameProcessor;
import org.apache.camel.Exchange;
import org.apache.camel.impl.engine.SimpleCamelContext;
import org.apache.camel.support.DefaultExchange;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TicketDocumentFileNameProcessorTest {

    @Test
    void process() {
        //given
        String expectedFileName = "TicketDocument-5262360010541-2021-03-04T22:57:37.578.xml";
        String body = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<cepMessage xmlns=\"http://www.sabre.com/asx/booking/epservicesV2\"><header><message>TicketFreeFlow</message><dateSent>2023-10-18T11:41:37</dateSent></header><content><ticketDocuments xmlns=\"http://www.sabre.com/asx/booking\"><count>1</count><ticketDocument>" +
                "<ticketNumber>5262360010541</ticketNumber><versionTimestamp>2021-03-04T22:57:37.578</versionTimestamp>" +
                "</booking></bookings></content></cepMessage>";
        SimpleCamelContext camelContext = new SimpleCamelContext();
        DefaultExchange exchange = new DefaultExchange(camelContext);
        exchange.getMessage().setBody(body);
        //when
        new TicketDocumentFileNameProcessor().process(exchange);
        //then
        assertEquals(expectedFileName, exchange.getMessage().getHeader(Exchange.FILE_NAME));
    }
}