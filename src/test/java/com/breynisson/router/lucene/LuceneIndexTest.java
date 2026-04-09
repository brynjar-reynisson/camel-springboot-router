package com.breynisson.router.lucene;

import com.breynisson.router.digitalme.SearchResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LuceneIndexTest {

    @TempDir
    Path indexDir;

    @BeforeEach
    void beforeEach() {
        LuceneIndex.setIndexPath(indexDir.toString());
        LuceneIndex.deleteIndex();
    }

    @Test
    void createOrUpdateIndex() {

        String[] content = {
                "Blue Suede Shoes was a very popular song",
                "Trump wears shoes",
                "Trump likes golden things",
                "Golden Brown is a Stranglers song",
                "This should not show up",
                "Trump golden shoes are not selling"
        };
        for (String s : content) {
            LuceneIndex.createOrUpdateIndex(s, s);
        }
        List<SearchResult> results = LuceneIndex.find("trump golden shoes");
        assertEquals(5, results.size());
        
        // The most relevant result should have highlighted terms
        String topSnippet = results.get(0).snippet();
        assertTrue(topSnippet.contains("<mark>Trump</mark>") || topSnippet.contains("<mark>golden</mark>") || topSnippet.contains("<mark>shoes</mark>"));
        
        for (SearchResult res : results) {
            System.out.println(res);
        }
    }
}