package com.breynisson.router;

import com.breynisson.router.jdbc.TextEntryDao;
import com.breynisson.router.jdbc.model.TextEntry;
import com.breynisson.router.lucene.LuceneIndex;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.List;
import java.util.stream.Stream;

public class FileChangeWatcher {

    public void watchDirectory(String directoryPath) throws IOException {

        //scan files in directory
        try (Stream<Path> pathsStream = Files.list(Paths.get(directoryPath))) {
            pathsStream.forEach((path) -> {
                File file = path.toFile();
                if (file.getName().endsWith("txt")) {
                    try {
                        String source = file.getAbsolutePath();
                        List<TextEntry> textEntries = TextEntryDao.findByName(source);
                        if (!textEntries.isEmpty()) {
                            TextEntry textEntry = textEntries.get(0);
                            long instantSecond = textEntry.instant.getEpochSecond();
                            long fileSecond = file.lastModified() / 1000;
                            if (instantSecond < fileSecond) {
                                TextEntry newTextEntry = new TextEntry(textEntry.uuid, Instant.now(), textEntry.name);
                                updateFileInfo(file, newTextEntry);
                            }
                        } else {
                            //new entry
                            updateFileInfo(file, null);
                        }
                    } catch (IOException | RouterException e) {
                        System.out.println(e.getMessage());
                    }
                }
            });
        }
    }

    private void updateFileInfo(File file, TextEntry textEntry) throws IOException {
        String content = Files.readString(file.toPath());
        String source = file.getAbsolutePath();
        LuceneIndex.createOrUpdateIndex(content, source, file.getName());
        if (textEntry != null) {
            TextEntryDao.update(textEntry);
        } else {
            TextEntryDao.insert(source, Instant.now());
        }
    }
}