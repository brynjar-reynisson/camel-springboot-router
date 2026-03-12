package com.breynisson.router.extract;

import io.github.thoroldvix.api.TranscriptApiFactory;
import io.github.thoroldvix.api.TranscriptRetrievalException;

import java.net.URI;
import java.util.Arrays;

public class YouTubeCaptionExtractor {

    public String extractFromYouTubeUrl(String url) throws TranscriptRetrievalException {
        String videoId = Arrays.stream(URI.create(url).getQuery().split("&"))
                .filter(p -> p.startsWith("v="))
                .map(p -> p.substring(2))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No video ID in URL: " + url));
        return extract(videoId);
    }

    public String extract(String videoId) throws TranscriptRetrievalException {
        StringBuilder sb = new StringBuilder();
        TranscriptApiFactory.createDefault()
                .getTranscript(videoId)
                .getContent()
                .forEach(f -> sb.append(String.format("[%.2f] %s%n", f.getStart(), f.getText())));
        return sb.toString();
    }
}
