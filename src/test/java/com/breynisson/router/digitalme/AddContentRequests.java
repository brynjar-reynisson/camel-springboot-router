package com.breynisson.router.digitalme;

public class AddContentRequests {

    public static AddContentRequest of(String source, String name, String content) {
        AddContentRequest req = new AddContentRequest();
        req.setSource(source);
        req.setName(name);
        req.setContent(content);
        return req;
    }
}
