package com.breynisson.router;

import com.breynisson.router.jdbc.DatabaseAdapter;
import com.breynisson.router.lucene.LuceneIndex;
import org.apache.camel.CamelContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AppConfig {

    public AppConfig(@Value("${data.dir:.}") String dataDir) {
        DatabaseAdapter.setDefaultDatabasePath(dataDir + "/digital-me.db");
        LuceneIndex.setIndexPath(dataDir + "/lucene-index");
        DatabaseAdapter.init();
    }

    @Bean
    public FileDeletion fileDeletion(CamelContext camelContext) {
        return new FileDeletion();
    }

    @Bean
    public FileCopy fileCopy(CamelContext camelContext) {
        return new FileCopy();
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
