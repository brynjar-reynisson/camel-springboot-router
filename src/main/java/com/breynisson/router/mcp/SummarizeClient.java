package com.breynisson.router.mcp;

@FunctionalInterface
public interface SummarizeClient {

    /** Returns a short summary of the text, or {@code null} if unavailable. */
    String summarize(String text);
}
