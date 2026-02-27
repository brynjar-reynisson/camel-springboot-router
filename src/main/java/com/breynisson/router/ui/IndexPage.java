package com.breynisson.router.ui;

import com.breynisson.router.ResourceReader;
import com.breynisson.router.jdbc.TextEntryDao;
import com.breynisson.router.jdbc.model.TextEntry;
import com.breynisson.router.lucene.LuceneIndex;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexableField;
import org.jsoup.Jsoup;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;
import org.springframework.web.util.HtmlUtils;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@RestController
public class IndexPage {

    private Lock lock = new ReentrantLock();

    @GetMapping("/")
    public RedirectView index() throws IOException, URISyntaxException {
        return new RedirectView("/index.html");
    }

    @GetMapping("/search")
    public Map<String, Object> search(@RequestParam String keywords) {
        List<Document> results = LuceneIndex.find(keywords);
        Map<String, Object> resultMap = new HashMap<>();
        Set<SearchResult> list = new LinkedHashSet<>();
        resultMap.put("results", list);
        for (Document document : results) {
            String source = document.getField("source").stringValue();
            IndexableField nameField = document.getField("name");
            list.add(new SearchResult(source, nameField != null ? nameField.stringValue() : source));
        }
        return resultMap;
    }

    @GetMapping("/localFile")
    public String localFile(@RequestParam String filePath) throws IOException {
        String content = Files.readString(Paths.get(filePath));
        content = HtmlUtils.htmlEscape(content);
        return "<html><body><p style='white-space: pre-wrap;'>" +
                content +
                "</p></body></html>";
    }

    @PostMapping(value="/addContent", consumes = "application/json", produces = "application/json")
            public AddContentResponse addContent(@RequestBody AddContentRequest addContentRequest)
    {
        lock.lock();
        AddContentResponse contentResponse = new AddContentResponse();
        try {
            String content = addContentRequest.getContent();
            if (addContentRequest.getSource().startsWith("http")) {
                content = Jsoup.parse(content).text();
            }
            LuceneIndex.createOrUpdateIndex(content, addContentRequest.getSource(), addContentRequest.getName());
            List<TextEntry> textEntries = TextEntryDao.findByName(addContentRequest.getSource());
            if (!textEntries.isEmpty()) {
                TextEntry textEntry = textEntries.get(0);
                textEntry = new TextEntry(textEntry.uuid, Instant.now(), textEntry.name);
                TextEntryDao.update(textEntry);
            } else {
                TextEntryDao.insert(addContentRequest.getSource(), Instant.now());
            }
            contentResponse.setSuccess(true);
        } catch (Exception e) {
            contentResponse.setSuccess(false);
            contentResponse.setErrorMessage(e.getMessage());
        }
        finally {
            lock.unlock();
        }
        return contentResponse;
    }
}
