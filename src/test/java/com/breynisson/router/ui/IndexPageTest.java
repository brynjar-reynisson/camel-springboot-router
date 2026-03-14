package com.breynisson.router.ui;

import com.breynisson.router.digitalme.AddContentRequest;
import com.breynisson.router.digitalme.AddContentRequests;
import com.breynisson.router.digitalme.AddContentResponse;
import com.breynisson.router.digitalme.SearchResponse;
import com.breynisson.router.digitalme.TestDigitalMeStorage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class IndexPageTest {

    private TestDigitalMeStorage storage;
    private IndexPage indexPage;

    @BeforeEach
    void setUp() {
        storage = new TestDigitalMeStorage();
        indexPage = new IndexPage(storage, null);
    }

    @Test
    void searchReturnsResultsFromStorage() {
        storage.addContent(request("http://example.com", "Example Page", "hello world content"));

        SearchResponse response = indexPage.search("hello");

        assertEquals(1, response.results().size());
        assertEquals("http://example.com", response.results().iterator().next().source());
    }

    @Test
    void searchReturnsEmptyWhenNoMatch() {
        SearchResponse response = indexPage.search("nonexistent");

        assertTrue(response.results().isEmpty());
    }

    @Test
    void addContentDelegatesToStorage() {
        AddContentResponse response = indexPage.addContent(request("http://example.com", "Example", "some content"));

        assertTrue(response.isSuccess());
        assertEquals(1, indexPage.search("some content").results().size());
    }

    private static AddContentRequest request(String source, String name, String content) {
        return AddContentRequests.of(source, name, content);
    }
}
