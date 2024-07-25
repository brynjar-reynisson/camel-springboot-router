package com.breynisson.router.jdbc.model;

import com.breynisson.router.jdbc.DatabaseAdapter;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class TextEntry {

    public final String uuid;
    public final Instant instant;

    public final String name;

    public TextEntry(String uuid, Instant instant, String name) {
        this.uuid = uuid;
        this.instant = instant;
        this.name = name;
    }

    public static class ResultSetTransform implements DatabaseAdapter.ResultSetTransform<TextEntry> {

        @Override
        public List<TextEntry> transform(ResultSet rset) throws SQLException {
            List<TextEntry> list = new ArrayList<>();
            while(rset.next()) {
                String uuid = rset.getString(1);
                String time = rset.getString(2);
                Instant instant = DatabaseAdapter.timeToInstant(time);
                String name = rset.getString(3);
                list.add(new TextEntry(uuid, instant, name));
            }
            return list;
        }
    }
}
