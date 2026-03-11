package com.breynisson.router.extract;

import io.github.thoroldvix.api.TranscriptApiFactory;
import io.github.thoroldvix.api.TranscriptRetrievalException;

public class YouTubeCaptionExtractor {

    public String extract(String videoId) throws TranscriptRetrievalException {
        StringBuilder sb = new StringBuilder();
        TranscriptApiFactory.createDefault()
                .getTranscript(videoId)
                .getContent()
                .forEach(f -> sb.append(String.format("[%.2f] %s%n", f.getStart(), f.getText())));
        return sb.toString();
    }
}
