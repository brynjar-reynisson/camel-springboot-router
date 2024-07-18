package com.breynisson.router.history;

import com.breynisson.router.Constants;
import com.breynisson.router.RouterException;
import com.breynisson.router.jdbc.Select;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

public class ChromiumUrlReader implements Processor {

    static final String query = "select " +
            "url,last_visit_time," +
            "datetime(last_visit_time / 1000000 + (strftime('%s', '1601-01-01')), 'unixepoch', 'localtime') as time_str " +
            "from urls " +
            "where last_visit_time>? " +
            "order by time_str " +
            "limit 100";

    @Override
    public void process(Exchange exchange) throws Exception {

        Long timeMarker = (Long) exchange.getProperty(Constants.MSG_PROPERTY_URL_HISTORY_TIME_MARKER);
        String historyCopyFile = "" + exchange.getProperty(Constants.MSG_PROPERTY_URL_HISTORY_DB_FILE);
        String jdbcUrl = "jdbc:sqlite:" + historyCopyFile.replace('\\', '/');
        List<HistoryUrl> historyUrls = new Select<HistoryUrl>()
                .withJdbcUrl(jdbcUrl)
                .withStatement(query)
                .withStatementPopulator((Consumer<PreparedStatement>) preparedStatement -> {
                    try {
                        preparedStatement.setLong(1, timeMarker);
                    } catch (SQLException e) {
                        throw new RuntimeException(e);
                    }
                })
                .executeQuery(resultSet -> {
                    List<HistoryUrl> list = new ArrayList<>();
                    while (true) {
                        try {
                            if (!resultSet.next()) break;
                            String url = resultSet.getString(1);
                            long time = resultSet.getLong(2);
                            String dateStr = resultSet.getString(3);
                            list.add(new HistoryUrl(url, time, dateStr));
                        } catch (SQLException e) {
                            throw new RouterException(e);
                        }

                    }
                    return list;
                });
        long lastTimeMarker = historyUrls.get(historyUrls.size()-1).timeMillis;
        exchange.setProperty(Constants.MSG_PROPERTY_URL_HISTORY_LAST_TIME_MARKER, lastTimeMarker);
        exchange.getMessage().setBody(historyUrls);
    }
}
