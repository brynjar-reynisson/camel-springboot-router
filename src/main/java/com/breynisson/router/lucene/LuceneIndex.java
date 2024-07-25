package com.breynisson.router.lucene;

import com.breynisson.router.RouterException;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.Map;

public class LuceneIndex {

    public static void createOrUpdateIndex(String content, Map<String, String> properties) {
        try (Directory indexDir = FSDirectory.open(Paths.get(getIndexPath()));
             Analyzer analyzer = new StandardAnalyzer()
        ) {
            IndexWriterConfig indexWriterConfig = new IndexWriterConfig();
            indexWriterConfig.setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);
            try (IndexWriter writer = new IndexWriter(indexDir, indexWriterConfig)) {
                Document doc = new Document();
                properties.forEach((key, value) -> {
                    Field field = new StringField(key, value, Field.Store.YES);
                    doc.add(field);
                });
                TextField textField = new TextField("Text", content, Field.Store.YES);
                doc.add(textField);
                writer.addDocument(doc);
            }
        } catch (IOException e) {
            throw new RouterException(e);
        }
    }

    public static void find(String phrase) {

    }

    static String getIndexPath() {
        return "lucene-index";
    }
}
