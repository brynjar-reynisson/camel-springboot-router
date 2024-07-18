package com.breynisson.router;

import com.breynisson.router.BookingFileNameProcessor;
import org.apache.camel.Exchange;
import org.apache.camel.impl.engine.SimpleCamelContext;
import org.apache.camel.support.DefaultExchange;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BookingFileNameProcessorTest {

    @Test
    void process() {
        //given
        String expectedFileName = "Booking-AAAAAA-1.xml";
        String body = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<cepMessage xmlns=\"http://www.sabre.com/asx/booking/epservicesV2\"><header><message>ReservationCreated</message><dateSent>2023-10-18T11:41:37</dateSent></header><content><bookings xmlns=\"http://www.sabre.com/asx/booking\"><count>1</count><booking>" +
                "<rloc>AAAAAA</rloc><versionNumber>1</versionNumber>" +
                "</booking></bookings></content></cepMessage>";
        SimpleCamelContext camelContext = new SimpleCamelContext();
        DefaultExchange exchange = new DefaultExchange(camelContext);
        exchange.getMessage().setBody(body);
        //when
        new BookingFileNameProcessor().process(exchange);
        //then
        assertEquals(expectedFileName, exchange.getMessage().getHeader(Exchange.FILE_NAME));
    }
}