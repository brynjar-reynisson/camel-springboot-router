package com.breynisson.router.digitalme;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

public class TestDigitalMeStorage implements DigitalMeStorage {

    private final List<AddContentRequest> store = new ArrayList<>();

    @Override
    public SearchResponse search(String keywords) {
        LinkedHashSet<SearchResult> results = new LinkedHashSet<>();
        String lowerKeywords = keywords.toLowerCase();
        for (AddContentRequest entry : store) {
            if (entry.getContent().toLowerCase().contains(lowerKeywords)
                    || entry.getName().toLowerCase().contains(lowerKeywords)) {
                results.add(new SearchResult(entry.getSource(), entry.getName()));
            }
        }
        return new SearchResponse(results);
    }

    @Override
    public AddContentResponse addContent(AddContentRequest addContentRequest) {
        AddContentRequest copy = new AddContentRequest();
        copy.setSource(addContentRequest.getSource());
        copy.setName(addContentRequest.getName());
        copy.setContent(addContentRequest.getContent());
        store.add(copy);
        AddContentResponse response = new AddContentResponse();
        response.setSuccess(true);
        return response;
    }

    public void clear() {
        store.clear();
    }
}
