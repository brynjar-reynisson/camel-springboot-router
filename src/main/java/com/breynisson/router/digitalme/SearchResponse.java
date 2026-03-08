package com.breynisson.router.digitalme;

import java.util.LinkedHashSet;

public class SearchResponse {

    public final LinkedHashSet<SearchResult> results;

    public SearchResponse(LinkedHashSet<SearchResult> results) {
        this.results = results;
    }
}
