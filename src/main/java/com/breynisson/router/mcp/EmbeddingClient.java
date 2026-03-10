package com.breynisson.router.mcp;

/**
 * Generates a dense vector embedding for a piece of text.
 * Returns {@code null} if the backing model is unavailable.
 */
public interface EmbeddingClient {
    float[] embed(String text);
}
