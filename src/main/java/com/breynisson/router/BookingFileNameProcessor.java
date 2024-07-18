package com.breynisson.router;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.Processor;

public class BookingFileNameProcessor implements Processor {

    @Override
    public void process(Exchange exchange) {
        String rloc = "undefined";
        String version = "0";

        Message message = exchange.getMessage();
        Object body = message.getBody();
        String xml;
        if (body instanceof String) {
            xml = message.getBody().toString();
        } else if (body instanceof byte[]) {
            xml = new String((byte[]) body);
        } else {
            throw new RouterException("Unknown body type: " + body.getClass());
        }

        if(!xml.contains("<bookings")) {
            return;
        }

        int rlocIndex = xml.indexOf("<rloc>");
        if (rlocIndex != -1) {
            rloc = xml.substring(rlocIndex + 6, rlocIndex + 12);
        }
        int versionIndex = xml.indexOf("<versionNumber>");
        if (versionIndex != -1) {
            int versionEndIndex = xml.indexOf("</versionNumber>");
            version = xml.substring(versionIndex + 15, versionEndIndex);
        }

        exchange.getMessage().setHeader(Exchange.FILE_NAME, "Booking-" + rloc + "-" + version + ".xml");
    }
}
