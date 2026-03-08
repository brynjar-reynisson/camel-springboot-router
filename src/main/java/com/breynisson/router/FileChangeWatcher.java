package com.breynisson.router;

import com.breynisson.router.digitalme.AddContentRequest;
import com.breynisson.router.digitalme.DigitalMeStorage;
import com.breynisson.router.jdbc.TextEntryDao;
import com.breynisson.router.jdbc.model.TextEntry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Stream;

public class FileChangeWatcher {

    private static final Logger log = LoggerFactory.getLogger(FileChangeWatcher.class);

    private final DigitalMeStorage storage;

    public FileChangeWatcher(DigitalMeStorage storage) {
        this.storage = storage;
    }

    public void watchDirectory(String directoryPath) throws IOException {

        if (directoryPath.endsWith("/*")) {
            String baseDir = directoryPath.substring(0, directoryPath.length() - 2);
            watchDirectory(baseDir);
            try (Stream<Path> subDirs = Files.list(Paths.get(baseDir))) {
                subDirs.filter(p -> p.toFile().isDirectory())
                       .forEach(p -> {
                           try {
                               watchDirectory(p + "/*");
                           } catch (IOException e) {
                               log.error("Error watching directory {}", p, e);
                           }
                       });
            }
            return;
        }

        //scan files in directory
        try (Stream<Path> pathsStream = Files.list(Paths.get(directoryPath))) {
            pathsStream.forEach((path) -> {
                File file = path.toFile();
                if (file.getName().endsWith("txt")) {
                    try {
                        String source = file.getAbsolutePath();
                        List<TextEntry> textEntries = TextEntryDao.findByName(source);
                        boolean isNew = textEntries.isEmpty();
                        boolean isModified = !isNew &&
                                textEntries.get(0).instant.getEpochSecond() < file.lastModified() / 1000;
                        if (isNew || isModified) {
                            updateFileInfo(file);
                        }
                    } catch (IOException | RouterException e) {
                        log.error("Error processing file {}", file.getAbsolutePath(), e);
                    }
                }
            });
        }
    }

    private void updateFileInfo(File file) throws IOException {
        String source = file.getAbsolutePath();
        log.info("Indexing {}", source);
        AddContentRequest req = new AddContentRequest();
        req.setSource(source);
        req.setName(file.getName());
        req.setContent(Files.readString(file.toPath()));
        storage.addContent(req);
    }
}
