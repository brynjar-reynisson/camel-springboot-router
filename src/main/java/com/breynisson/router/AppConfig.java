package com.breynisson.router;

import com.breynisson.router.history.HistoryUrlFilter;
import com.ibm.mq.jms.MQQueueConnectionFactory;
import com.breynisson.router.docker.DockerComponent;
import com.sun.xml.bind.v2.runtime.reflect.opt.Const;
import org.apache.camel.CamelContext;
import org.apache.camel.component.jms.JmsComponent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.connection.CachingConnectionFactory;
import org.springframework.jms.connection.JmsTransactionManager;

import javax.jms.ConnectionFactory;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class AppConfig {

    @Value(value="${wmq.hostname:127.0.0.1}")
    String wmqHostName;

    @Value(value="${wmq.port:1414}")
    int wmqPort;

    @Value(value="${wmq.queue-manager:UNDEFINED}")
    String wmqQueueManager;

    @Value(value="${wmq.channel:UNDEFINED}")
    String wmqChannel;

    @Bean
    public JmsTransactionManager wmqTransactionManager() {
        return new JmsTransactionManager(wmqCachingConnectionFactory());
    }

    @Bean
    public CachingConnectionFactory wmqCachingConnectionFactory() {
        CachingConnectionFactory cachingConnectionFactory = new CachingConnectionFactory(wmqConnectionFactory());
        cachingConnectionFactory.setSessionCacheSize(10);
        return cachingConnectionFactory;
    }

    @Bean
    public ConnectionFactory wmqConnectionFactory() {
        try {
            MQQueueConnectionFactory connectionFactory = new MQQueueConnectionFactory();
            connectionFactory.setTransportType(1);
            connectionFactory.setHostName(wmqHostName);
            connectionFactory.setPort(wmqPort);
            connectionFactory.setQueueManager(wmqQueueManager);
            connectionFactory.setChannel(wmqChannel);
            return connectionFactory;
        } catch (Exception e) {
            throw new RouterException(e);
        }
    }

    @Bean
    public JmsComponent wmq(CamelContext camelContext) {
        JmsComponent jmsComponent = new JmsComponent(camelContext);
        jmsComponent.setConnectionFactory(wmqCachingConnectionFactory());
        jmsComponent.setTransactionManager(wmqTransactionManager());
        jmsComponent.setTransacted(true);
        jmsComponent.setCacheLevelName("CACHE_CONSUMER");
        return jmsComponent;
    }

    @Bean
    public DockerComponent docker(CamelContext camelContext) {
        return new DockerComponent(camelContext);
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
    public HistoryUrlFilter historyUrlFilter(CamelContext camelContext) {
        return new HistoryUrlFilter();
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
}
