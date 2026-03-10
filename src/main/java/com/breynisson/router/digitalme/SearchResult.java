package com.breynisson.router.digitalme;

import java.util.Objects;

public class SearchResult {

    public final String source;
    public final String name;

    public SearchResult(String source, String name) {
        this.source = source;
        this.name = name;
    }

    public boolean equals(Object other) {
        if (other == null || !other.getClass().equals(this.getClass())) {
            return false;
        }
        SearchResult otherResult = (SearchResult) other;
        return Objects.equals(this.source, otherResult.source);
    }

    public int hashCode() {
        return source.hashCode();
    }
}
