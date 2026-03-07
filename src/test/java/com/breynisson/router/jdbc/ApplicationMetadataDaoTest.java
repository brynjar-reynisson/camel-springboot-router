package com.breynisson.router.jdbc;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;

import static org.junit.jupiter.api.Assertions.*;

class ApplicationMetadataDaoTest {

    @BeforeAll
    static void setUp() {
        new File("digital-me-unit-tests").mkdirs();
        DatabaseAdapter.setDefaultDatabasePath("./digital-me-unit-tests/digital-me.db");
        DatabaseAdapter.init();
    }

    @Test
    void insertGetAndDelete() {
        // given
        String testKey = "test-key";
        String testValue = "testValue";
        ApplicationMetadataDao.deleteValue(testKey); // ensure clean state

        // when
        ApplicationMetadataDao.insert(testKey, testValue);

        // then
        String actualValue = ApplicationMetadataDao.getValue(testKey);
        assertEquals(testValue, actualValue);
        ApplicationMetadataDao.deleteValue(testKey);
    }

}