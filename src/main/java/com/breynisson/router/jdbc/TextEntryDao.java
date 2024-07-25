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

    public static TextEntry findByUUID(String uuid) {
        return DatabaseAdapter.selectOne("SELECT * FROM " + TABLE_NAME + " WHERE UUID='" + uuid + "'", new TextEntry.ResultSetTransform());
    }

    public static List<TextEntry> findByName(String name) {
        return DatabaseAdapter.selectList("SELECT * FROM " + TABLE_NAME + " WHERE NAME='" + name + "'", new TextEntry.ResultSetTransform());
    }

    public static void delete(String uuid) {
        TextEntryMetadataDao.deleteByUUID(uuid);
        DatabaseAdapter.runSql("DELETE FROM " + TABLE_NAME + " WHERE UUID='" + uuid + "'");
    }
}
