package com.breynisson.router;

import com.breynisson.router.jdbc.DatabaseAdapter;
import com.breynisson.router.jdbc.TextEntryDao;
import com.breynisson.router.lucene.LuceneIndex;
import org.apache.lucene.document.Document;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FileChangeWatcherTest {

    @TempDir
    static Path dbDir;

    @TempDir
    Path tempDir;

    @TempDir
    Path indexDir;

    private final FileChangeWatcher watcher = new FileChangeWatcher();

    @BeforeAll
    static void setUpDatabase() {
        DatabaseAdapter.setDefaultDatabasePath(dbDir.resolve("test.db").toString());
        DatabaseAdapter.init();
    }

    @AfterAll
    static void tearDownDatabase() {
        DatabaseAdapter.setDefaultDatabasePath(null);
    }

    @BeforeEach
    void setUp() {
        LuceneIndex.setIndexPath(indexDir.toString());
        LuceneIndex.deleteIndex();
    }

    @AfterEach
    void tearDown() {
        LuceneIndex.deleteIndex();
    }

    private void cleanupDb(Path... files) {
        for (Path file : files) {
            TextEntryDao.findByName(file.toAbsolutePath().toString())
                        .forEach(e -> TextEntryDao.delete(e.uuid));
        }
    }

    @Test
    void indexesTxtFilesInDirectory() throws IOException {
        Path file = tempDir.resolve("doc.txt");
        Files.writeString(file, "hello world content");

        watcher.watchDirectory(tempDir.toString());

        List<Document> results = LuceneIndex.find("hello");
        assertEquals(1, results.size());
        assertEquals(file.toAbsolutePath().toString(), results.get(0).get("source"));
        assertFalse(TextEntryDao.findByName(file.toAbsolutePath().toString()).isEmpty());

        cleanupDb(file);
    }

    @Test
    void ignoresNonTxtFiles() throws IOException {
        Path mdFile = tempDir.resolve("notes.md");
        Path jsonFile = tempDir.resolve("data.json");
        Files.writeString(mdFile, "markdown content");
        Files.writeString(jsonFile, "json content");

        watcher.watchDirectory(tempDir.toString());

        assertTrue(TextEntryDao.findByName(mdFile.toAbsolutePath().toString()).isEmpty());
        assertTrue(TextEntryDao.findByName(jsonFile.toAbsolutePath().toString()).isEmpty());
    }

    @Test
    void doesNotIndexSubdirectoriesWithoutWildcard() throws IOException {
        Path sub = Files.createDirectory(tempDir.resolve("sub"));
        Path subFile = sub.resolve("nested.txt");
        Files.writeString(subFile, "nested content");

        watcher.watchDirectory(tempDir.toString());

        assertTrue(TextEntryDao.findByName(subFile.toAbsolutePath().toString()).isEmpty());
    }

    @Test
    void recursivelyIndexesSubdirectories() throws IOException {
        Path sub1 = Files.createDirectory(tempDir.resolve("sub1"));
        Path sub2 = Files.createDirectory(tempDir.resolve("sub2"));
        Path rootFile = tempDir.resolve("root.txt");
        Path sub1File = sub1.resolve("a.txt");
        Path sub2File = sub2.resolve("b.txt");
        Files.writeString(rootFile, "xyzroot content");
        Files.writeString(sub1File, "xyzsub1 content");
        Files.writeString(sub2File, "xyzsub2 content");

        watcher.watchDirectory(tempDir + "/*");

        assertEquals(1, LuceneIndex.find("xyzroot").size());
        assertEquals(1, LuceneIndex.find("xyzsub1").size());
        assertEquals(1, LuceneIndex.find("xyzsub2").size());

        cleanupDb(rootFile, sub1File, sub2File);
    }

    @Test
    void updatesIndexWhenFileIsModified() throws IOException {
        Path file = tempDir.resolve("file.txt");
        Files.writeString(file, "original content");

        watcher.watchDirectory(tempDir.toString());
        assertEquals(1, LuceneIndex.find("original").size());

        // Push last-modified into the future so it exceeds the DB entry's instant
        file.toFile().setLastModified(System.currentTimeMillis() + 10_000);
        Files.writeString(file, "updated content");

        watcher.watchDirectory(tempDir.toString());

        assertEquals(1, LuceneIndex.find("updated content").size());

        cleanupDb(file);
    }

    @Test
    void doesNotReindexUnchangedFile() throws IOException {
        Path file = tempDir.resolve("stable.txt");
        Files.writeString(file, "stable content");

        watcher.watchDirectory(tempDir.toString());
        assertEquals(1, LuceneIndex.find("stable content").size());

        // Push last-modified into the past so the file appears older than the DB record
        file.toFile().setLastModified(System.currentTimeMillis() - 10_000);

        // Clear the index; a re-index would repopulate it
        LuceneIndex.deleteIndex();

        watcher.watchDirectory(tempDir.toString());

        // Index should remain empty — the file was not re-indexed
        assertEquals(1, TextEntryDao.findByName(file.toAbsolutePath().toString()).size());
        assertThrows(Exception.class, () -> LuceneIndex.find("stable content"));

        cleanupDb(file);
    }
}
