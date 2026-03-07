package com.breynisson.router;

import org.apache.camel.CamelContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class AppConfig {

    @Bean
    public FileDeletion fileDeletion(CamelContext camelContext) {
        return new FileDeletion();
    }

    @Bean
    public FileCopy fileCopy(CamelContext camelContext) {
        return new FileCopy();
    }

    @Bean
    public File luceneHistoryUrl(CamelContext camelContext) {
        Path path = Paths.get(Constants.LUCENE_HISTORY_INDEX_DIR);
        if(!path.toFile().isDirectory()) {
            try {
                Files.createDirectories(path);
            } catch (IOException e) {
                throw new RouterException(e);
            }
        }
        return path.toFile();
    }

    @Bean
    public FileChangeWatcher fileChangeWatcher(CamelContext camelContext) {
        return new FileChangeWatcher();
    }

    @Bean
    public ContentReceive contentReceive(CamelContext camelContext) {
        return new ContentReceive(camelContext);
    }
}
