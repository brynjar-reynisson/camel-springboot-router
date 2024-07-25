package com.breynisson.router.jdbc;

import javax.xml.crypto.Data;
import java.util.List;

public class ApplicationMetadataDao {

    private static final String TABLE_NAME = "APPLICATION_METADATA";

    public static void insert(String key, String value) {
        DatabaseAdapter.runSql("INSERT INTO " + TABLE_NAME + " (KEY, VALUE) VALUES ('" + key + "','" + value + "')");
    }

    public static String getValue(String key) {
        return DatabaseAdapter.selectOne("SELECT VALUE FROM " + TABLE_NAME + " WHERE KEY=?",
                DatabaseAdapter.RESULT_SET_STRING_TRANSFORM,
                key);
    }

    public static void deleteValue(String key) {
        DatabaseAdapter.runSql("DELETE FROM " + TABLE_NAME + " WHERE KEY='" + key + "'");
    }
}
