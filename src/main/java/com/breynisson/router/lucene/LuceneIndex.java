package com.breynisson.router.lucene;

import com.breynisson.router.RouterException;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.*;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.*;

public class LuceneIndex {

    public static void createOrUpdateIndex(String content, String source) {
        createOrUpdateIndex(content, source, source);
    }

    public static void createOrUpdateIndex(String content, String source, String name) {
        Map<String, String> properties = new HashMap<>();
        properties.put("source", source);
        properties.put("name", name);
        createOrUpdateIndex(content, properties);
    }

    public static void createOrUpdateIndex(String content, Map<String, String> properties) {
        try (Directory indexDir = FSDirectory.open(Paths.get(getIndexPath()));
             Analyzer analyzer = new StandardAnalyzer()
        ) {
            IndexWriterConfig indexWriterConfig = new IndexWriterConfig();
            indexWriterConfig.setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);
            try (IndexWriter writer = new IndexWriter(indexDir, indexWriterConfig)) {
                String source = properties.get("source");
                writer.deleteDocuments(new Term("source", source));
                Document doc = new Document();
                properties.forEach((key, value) -> {
                    Field field = new StringField(key, value, Field.Store.YES);
                    doc.add(field);
                });
                TextField textField = new TextField("body", content, Field.Store.YES);
                doc.add(textField);
                writer.addDocument(doc);
            }
        } catch (IOException e) {
            throw new RouterException(e);
        }
    }

    public static List<Document> find(String phrase) {
        try (Directory indexDir = FSDirectory.open(Paths.get(getIndexPath()));
             IndexReader reader = DirectoryReader.open(indexDir);
             Analyzer analyzer = new StandardAnalyzer();
        ) {
            IndexSearcher searcher = new IndexSearcher(reader);
            QueryParser queryParser = new QueryParser("body", analyzer);
            Query query = queryParser.parse(phrase);
            TopDocs topDocs = searcher.search(query, 1000000);
            List<Document> documents = new ArrayList<>();
            for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
                documents.add(searcher.doc(scoreDoc.doc));
            }
            return documents;
        } catch (Exception e) {
            throw new RouterException(e);
        }
    }

    private static String indexPath = "lucene-index";

    public static void setIndexPath(String path) {
        indexPath = path;
    }

    static String getIndexPath() {
        return indexPath;
    }

    public static void deleteIndex() {
        for (File file : Objects.requireNonNull(new File(getIndexPath()).listFiles())) {
            if (file.isFile()) {
                file.delete();
            }
        }
    }
}
