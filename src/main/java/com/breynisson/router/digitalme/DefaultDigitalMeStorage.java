package com.breynisson.router.digitalme;

import com.breynisson.router.jdbc.TextEntryDao;
import com.breynisson.router.lucene.LuceneIndex;
import com.breynisson.router.mcp.ResourceReceiver;

import static com.breynisson.router.lucene.LuceneIndex.FIELD_NAME;
import static com.breynisson.router.lucene.LuceneIndex.FIELD_SOURCE;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexableField;
import org.jsoup.Jsoup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class DefaultDigitalMeStorage implements DigitalMeStorage {

    private static final Logger log = LoggerFactory.getLogger(DefaultDigitalMeStorage.class);

    private final Lock lock = new ReentrantLock();
    private final ResourceReceiver resourceReceiver;

    public DefaultDigitalMeStorage(String dataDir) {
        this.resourceReceiver = new ResourceReceiver(dataDir);
    }

    @Override
    public SearchResponse search(String keywords) {
        log.info("Search: {}", keywords);
        List<Document> results = LuceneIndex.find(keywords);
        LinkedHashSet<SearchResult> list = new LinkedHashSet<>();
        for (Document document : results) {
            String source = document.getField(FIELD_SOURCE).stringValue();
            IndexableField nameField = document.getField(FIELD_NAME);
            list.add(new SearchResult(source, nameField != null ? nameField.stringValue() : source));
        }
        return new SearchResponse(list);
    }

    @Override
    public AddContentResponse addContent(AddContentRequest addContentRequest) {
        lock.lock();
        AddContentResponse contentResponse = new AddContentResponse();
        try {
            log.info("addContent: {}", addContentRequest.getSource());
            String content = addContentRequest.getContent();
            if (addContentRequest.getSource().startsWith("http")) {
                content = Jsoup.parse(content).text();
                content = content.replace('\n', ' ');
                content = content.replace('\t', ' ');
                addContentRequest.setContent(content);
            }
            resourceReceiver.addContent(addContentRequest);
            LuceneIndex.createOrUpdateIndex(content, addContentRequest.getSource(), addContentRequest.getName());
            TextEntryDao.insertOrUpdate(addContentRequest.getSource());
            contentResponse.setSuccess(true);
        } catch (Exception e) {
            log.error("Error in addContent for {}", addContentRequest.getSource(), e);
            contentResponse.setSuccess(false);
            contentResponse.setErrorMessage(e.getMessage());
        } finally {
            lock.unlock();
        }
        return contentResponse;
    }
}
