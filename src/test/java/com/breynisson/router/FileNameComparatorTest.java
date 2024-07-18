package com.breynisson.router;

import com.breynisson.router.FileNameComparator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FileNameComparatorTest {

    private final FileNameComparator comparator = new FileNameComparator();

    @Test
    void shouldMatchFileNameWithNumberNearEnd() {
        assertTrue(FileNameComparator.matches("WWWXXX-7.xml"));
        assertTrue(FileNameComparator.matches("WWWXXX_03.xml"));
    }

    @Test
    void shouldNotMatchWhenEndDoesNotContainNumber() {
        assertFalse(FileNameComparator.matches("AAABBB.xml"));
        assertFalse(FileNameComparator.matches("AAABBB-C.xml"));
    }

    @Test
    void sevenIsMoreThanFive() {
        assertTrue(comparator.compare("AAAAAA-7.xml", "AAAAAA-5.xml") > 0);
    }

    @Test
    void aIsLessThanB() {
        assertTrue(comparator.compare("AAAAAA-3.xml", "BBBBBB-1.xml") < 0);
    }

}