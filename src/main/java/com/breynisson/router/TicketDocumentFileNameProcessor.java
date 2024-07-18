package com.breynisson.router;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.Processor;

import java.nio.charset.StandardCharsets;

public class TicketDocumentFileNameProcessor implements Processor {

    @Override
    public void process(Exchange exchange) {
        String ticketNumber = "undefined";
        String versionTimestamp = "0";

        Message message = exchange.getMessage();
        Object body = message.getBody();
        String xml;
        if (body instanceof String) {
            xml = message.getBody().toString();
        } else if (body instanceof byte[]) {
            xml = new String((byte[]) body, StandardCharsets.UTF_8);
        } else {
            throw new RouterException("Unknown body type: " + body.getClass());
        }

        String type;
        if(xml.contains("<ticketDocuments")) {
            type = "TicketDocument";
        } else if(xml.contains("<salesDocuments")) {
            type = "SalesDocument";
        } else {
            return;
        }

        int ticketNumberIndex = xml.indexOf("<ticketNumber>");
        if (ticketNumberIndex != -1) {
            int ticketNumberEndIndex = xml.indexOf("</ticketNumber");
            ticketNumber = xml.substring(ticketNumberIndex + 14, ticketNumberEndIndex);
        }
        int versionIndex = xml.indexOf("<versionTimestamp>");
        if (versionIndex != -1) {
            int versionEndIndex = xml.indexOf("</versionTimestamp>");
            versionTimestamp = xml.substring(versionIndex + 18, versionEndIndex);
        }

        exchange.getMessage().setHeader(Exchange.FILE_NAME, type + "-" + ticketNumber + "-" + versionTimestamp + ".xml");
    }
}
