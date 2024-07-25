package com.breynisson.router.lucene;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

class LuceneIndexTest {

    @Test
    void createOrUpdateIndex() {

        String content = "Trump golden shoes are not selling";
        Map<String, String> properties = new HashMap<>();
        properties.put("path", "./trump.txt");

        LuceneIndex.createOrUpdateIndex(content, properties);
    }
}