package com.breynisson.router.jdbc.model;

import com.breynisson.router.jdbc.DatabaseAdapter;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class McpEmbedding {

    public final String filePath;
    public final String sourceUrl;
    public final byte[] embedding;
    public final String indexedAt;

    public McpEmbedding(String filePath, String sourceUrl, byte[] embedding, String indexedAt) {
        this.filePath = filePath;
        this.sourceUrl = sourceUrl;
        this.embedding = embedding;
        this.indexedAt = indexedAt;
    }

    public static class ResultSetTransform implements DatabaseAdapter.ResultSetTransform<McpEmbedding> {

        @Override
        public List<McpEmbedding> transform(ResultSet rset) throws SQLException {
            List<McpEmbedding> list = new ArrayList<>();
            while (rset.next()) {
                list.add(new McpEmbedding(
                        rset.getString(1),
                        rset.getString(2),
                        rset.getBytes(3),
                        null)); // INDEXED_AT not needed for search
            }
            return list;
        }
    }
}
