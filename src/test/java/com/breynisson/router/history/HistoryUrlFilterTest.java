package com.breynisson.router.history;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class HistoryUrlFilterTest {

    @Test
    void shouldRejectPlainTextUrl() {

        //given
        HistoryUrlFilter filter = newHistoryFilter("https://facebook.com/");

        //when
        boolean result = filter.test(new HistoryUrl("https://facebook.com/", 0, null));

        //then
        assertFalse(result);
    }

    @Test
    void shouldRejectRegexUrl() {

        //given
        HistoryUrlFilter filter = newHistoryFilter("https://quora.com/.*");

        //when
        boolean result = filter.test(new HistoryUrl("https://quora.com/whatever", 0, null));

        //then
        assertFalse(result);
    }

    @Test
    void shouldAcceptNonFilteredUrl() {

        //given
        HistoryUrlFilter filter = newHistoryFilter("https://quora.com/.*\nhttps://facebook.com/");

        //when
        boolean result = filter.test(new HistoryUrl("https://mbl.is", 0, null));

        //then
        assertTrue(result);
    }


    private HistoryUrlFilter newHistoryFilter(String filterFileContent) {
        HistoryUrlFilter filter = new HistoryUrlFilter();
        filter.populateFilterSets(filterFileContent);
        return filter;
    }
}