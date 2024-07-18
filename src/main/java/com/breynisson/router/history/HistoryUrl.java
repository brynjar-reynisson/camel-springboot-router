package com.breynisson.router.history;

public class HistoryUrl {

    public final String url;
    public final long timeMillis;

    public final String dateStr;

    public HistoryUrl(String url, long timeMillis, String dateStr) {
        this.url = url;
        this.timeMillis = timeMillis;
        this.dateStr = dateStr;
    }

    public String toString() {
        return "{ \"url\":\"" + url + "\", \"timeMillis\":\"" + timeMillis + "\", \"dateStr\":\"" + dateStr + "\" }\n";
    }
}
