package com.breynisson.router.jdbc;

import com.breynisson.router.jdbc.model.TextEntry;
import com.breynisson.router.jdbc.model.TextEntryMetadata;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static com.breynisson.router.jdbc.DatabaseAdapter.runSql;

public class TextEntryDao {

    private static final String TABLE_NAME = "TEXT_ENTRY";

    public static String insert(String name, Instant instant) {
        String uuid = UUID.randomUUID().toString();
        if (instant == null) {
            instant = Instant.now();
        }
        String dateTimeStr = DatabaseAdapter.instantToTime(instant);
        runSql("INSERT INTO " + TABLE_NAME + " (UUID, NAME, TIME) VALUES ('" + uuid + "', '" + name + "', '" + dateTimeStr + "')");
        return uuid;
    }

    public static void update(TextEntry textEntry) {
        String dateTimeStr = DatabaseAdapter.instantToTime(textEntry.instant);
        runSql("UPDATE " + TABLE_NAME + " SET NAME='" + textEntry.name + "', TIME='" + dateTimeStr + "' WHERE UUID='" +  textEntry.uuid + "'");
    }

    public static TextEntry findByUUID(String uuid) {
        return DatabaseAdapter.selectOne("SELECT * FROM " + TABLE_NAME + " WHERE UUID='" + uuid + "'", new TextEntry.ResultSetTransform());
    }

    public static List<TextEntry> findByName(String name) {
        return DatabaseAdapter.selectList("SELECT * FROM " + TABLE_NAME + " WHERE NAME='" + name + "'", new TextEntry.ResultSetTransform());
    }

    public static void insertOrUpdate(String source) {
        List<TextEntry> textEntries = findByName(source);
        if (!textEntries.isEmpty()) {
            TextEntry textEntry = textEntries.get(0);
            update(new TextEntry(textEntry.uuid, Instant.now(), textEntry.name));
        } else {
            insert(source, Instant.now());
        }
    }

    public static void delete(String uuid) {
        TextEntryMetadataDao.deleteByUUID(uuid);
        DatabaseAdapter.runSql("DELETE FROM " + TABLE_NAME + " WHERE UUID='" + uuid + "'");
    }
}
