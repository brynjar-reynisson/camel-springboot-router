package com.breynisson.router.lucene;

import org.apache.lucene.document.Document;
import org.apache.lucene.search.ScoreDoc;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LuceneIndexTest {

    @BeforeEach
    void beforeEach() {
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
            LuceneIndex.createOrUpdateIndex(s, "test");
        }
        List<Document> documents = LuceneIndex.find("trump golden shoes");
        assertEquals(5, documents.size());
        assertEquals(content[5], documents.get(0).getField("body").stringValue());
        for (Document document: documents) {
            System.out.println(document);
        }
    }


}