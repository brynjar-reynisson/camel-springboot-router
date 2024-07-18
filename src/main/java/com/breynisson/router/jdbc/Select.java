package com.breynisson.router.jdbc;

import com.breynisson.router.RouterException;

import java.sql.*;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class Select<T> {

    private String jdbcUrl;
    private String statement;
    private Consumer<PreparedStatement> statementPopulator;

    public Select<T> withJdbcUrl(String jdbcUrl) {
        this.jdbcUrl = jdbcUrl;
        return this;
    }

    public Select<T> withStatement(String statement) {
        this.statement = statement;
        return this;
    }

    public Select<T> withStatementPopulator(Consumer<PreparedStatement> statementPopulator) {
        this.statementPopulator = statementPopulator;
        return this;
    }

    public List<T> executeQuery(Function<ResultSet,List<T>> converter) {

        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            throw new RouterException(e);
        }
        try (Connection con = DriverManager
                .getConnection(jdbcUrl)) {
            try (PreparedStatement pstmt = con.prepareStatement(statement)) {
                statementPopulator.accept(pstmt);
                try (ResultSet resultSet = pstmt.executeQuery()) {
                    return converter.apply(resultSet);
                }
            }
            // use con here
        } catch (SQLException e) {
            throw new RouterException(e);
        }
    }
}
