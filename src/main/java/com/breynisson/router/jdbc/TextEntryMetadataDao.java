package com.breynisson.router.jdbc;

import com.breynisson.router.jdbc.model.TextEntryMetadata;

import java.util.List;

public class TextEntryMetadataDao {

    private static final String TABLE_NAME = "TEXT_ENTRY_METADATA";

    public static void insert(String uuid, String key, String value) {
        DatabaseAdapter.runSql("INSERT INTO " + TABLE_NAME + " (TEXT_ENTRY_UUID,KEY,VALUE) VALUES ('" + uuid + "','" + key + "','" + value + "')");
    }

    public static String get(String uuid, String key) {
        return DatabaseAdapter.selectOne("SELECT VALUE FROM " + TABLE_NAME + " WHERE TEXT_ENTRY_UUID=? AND KEY=?", DatabaseAdapter.RESULT_SET_STRING_TRANSFORM, uuid, key);
    }

    public static void deleteByUUID(String uuid) {
        DatabaseAdapter.runSql("DELETE FROM " + TABLE_NAME + " WHERE TEXT_ENTRY_UUID='" + uuid + "'");
    }
}
