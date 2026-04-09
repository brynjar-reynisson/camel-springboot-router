package com.breynisson.router.digitalme;

import java.util.Objects;

public record SearchResult(String source, String name, String snippet, Double score) {

    public SearchResult(String source, String name) {
        this(source, name, null, null);
    }

    public SearchResult(String source, String name, String snippet) {
        this(source, name, snippet, null);
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
