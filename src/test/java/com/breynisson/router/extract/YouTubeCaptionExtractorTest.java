package com.breynisson.router.extract;

import io.github.thoroldvix.api.TranscriptRetrievalException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class YouTubeCaptionExtractorTest {

    @Test
    void fetchTranscript() throws TranscriptRetrievalException {
        String result = new YouTubeCaptionExtractor().extract("CEvIs9y1uog");

        assertThat(result).contains("[music]");
        assertThat(result).contains("agent skills");
        assertThat(result).contains("Thank you.");
    }
}
