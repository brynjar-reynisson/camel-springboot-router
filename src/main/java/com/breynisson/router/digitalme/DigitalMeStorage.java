package com.breynisson.router.digitalme;

public interface DigitalMeStorage {

    SearchResponse search(String keywords);

    AddContentResponse addContent(AddContentRequest addContentRequest);
}
