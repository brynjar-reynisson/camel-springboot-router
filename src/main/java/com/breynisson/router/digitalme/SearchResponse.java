package com.breynisson.router.digitalme;

import java.util.LinkedHashSet;

public record SearchResponse(LinkedHashSet<SearchResult> results) {

}
