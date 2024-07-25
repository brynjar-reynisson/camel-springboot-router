package com.breynisson.router.jdbc;

import com.breynisson.router.jdbc.model.TextEntry;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TextEntryDaoTest {

    @Test
    void insertFindAndDelete() {
        String name = "textEntryName";
        Instant instant = Instant.now();
        String uuid = TextEntryDao.insert(name, instant);
        TextEntry uuidEntry = TextEntryDao.findByUUID(uuid);
        assertNotNull(uuidEntry);
        assertEquals(instant, uuidEntry.instant);
        List<TextEntry> nameList = TextEntryDao.findByName(name);
        assertEquals(1, nameList.size());

        String key = "metadataKey";
        String value = "metadataValue";
        TextEntryMetadataDao.insert(uuid, key, value);
        String actualValue = TextEntryMetadataDao.get(uuid, key);
        assertEquals(value, actualValue);

        TextEntryDao.delete(uuid);
    }
}