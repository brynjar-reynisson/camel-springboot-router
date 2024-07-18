package com.breynisson.router.history;

import com.breynisson.router.RouterException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Pattern;

public class HistoryUrlFilter implements Predicate<HistoryUrl> {

    private static final Path filterfile = Paths.get("./config/history-url-filter.txt");
    private static Set<String> plainTextUrls = null;
    private static Set<Pattern> regexUrls = null;

    @Override
    public boolean test(HistoryUrl historyUrl) {
        if(plainTextUrls == null) {
            populateFilterSets();
        }
        if(plainTextUrls.contains(historyUrl.url)) {
            return false;
        }
        for (Pattern regexUrl : regexUrls) {
            if(regexUrl.matcher(historyUrl.url).matches()) {
                return false;
            }
        }
        return true;
    }

    private synchronized void populateFilterSets() {
        try {
            String filterFileContent = Files.readString(filterfile);
            populateFilterSets(filterFileContent);
        } catch (IOException e) {
            throw new RouterException(e);
        }
    }

    void populateFilterSets(String filterFileContent) {
        regexUrls = new HashSet<>();
        plainTextUrls = new HashSet<>();
        filterFileContent.lines().forEach((filterLine)-> {
            if(filterLine.contains(".*")) {
                regexUrls.add(Pattern.compile(filterLine));
            } else {
                plainTextUrls.add(filterLine);
            }
        });
    }
}
