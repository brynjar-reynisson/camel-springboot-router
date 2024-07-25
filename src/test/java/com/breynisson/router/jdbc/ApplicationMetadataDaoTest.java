package com.breynisson.router.jdbc;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ApplicationMetadataDaoTest {

    @Test
    void insertGetAndDelete() {
        String testKey = "test-key";
        String testValue = "testValue";
        ApplicationMetadataDao.insert(testKey, testValue);
        String actualValue = ApplicationMetadataDao.getValue(testKey);
        assertEquals(testValue, actualValue);
        ApplicationMetadataDao.deleteValue(testKey);
    }

}