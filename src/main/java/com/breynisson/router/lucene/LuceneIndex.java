package com.breynisson.router.lucene;

import com.breynisson.router.RouterException;
import com.breynisson.router.digitalme.SearchResult;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.*;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.search.highlight.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.*;

public class LuceneIndex {

    public static final String FIELD_SOURCE = "source";
    public static final String FIELD_NAME = "name";
    public static final String FIELD_BODY = "body";

    public static void createOrUpdateIndex(String content, String source) {
        createOrUpdateIndex(content, source, source);
    }

    public static void createOrUpdateIndex(String content, String source, String name) {
        Map<String, String> properties = new HashMap<>();
        properties.put(FIELD_SOURCE, source);
        properties.put(FIELD_NAME, name);
        createOrUpdateIndex(content, properties);
    }

    public static void createOrUpdateIndex(String content, Map<String, String> properties) {
        try (Directory indexDir = FSDirectory.open(Paths.get(getIndexPath()));
             Analyzer analyzer = new StandardAnalyzer()
        ) {
            IndexWriterConfig indexWriterConfig = new IndexWriterConfig();
            indexWriterConfig.setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);
            try (IndexWriter writer = new IndexWriter(indexDir, indexWriterConfig)) {
                String source = properties.get(FIELD_SOURCE);
                writer.deleteDocuments(new Term(FIELD_SOURCE, source));
                Document doc = new Document();
                properties.forEach((key, value) -> {
                    Field field = new StringField(key, value, Field.Store.YES);
                    doc.add(field);
                });
                TextField textField = new TextField(FIELD_BODY, content, Field.Store.YES);
                doc.add(textField);
                writer.addDocument(doc);
            }
        } catch (IOException e) {
            throw new RouterException(e);
        }
    }

    public static List<SearchResult> find(String phrase) {
        try (Directory indexDir = FSDirectory.open(Paths.get(getIndexPath()));
             IndexReader reader = DirectoryReader.open(indexDir);
             Analyzer analyzer = new StandardAnalyzer();
        ) {
            IndexSearcher searcher = new IndexSearcher(reader);
            QueryParser queryParser = new QueryParser(FIELD_BODY, analyzer);
            Query query = queryParser.parse(phrase);
            TopDocs topDocs = searcher.search(query, 100); // reduced from 100 for sanity

            org.apache.lucene.search.highlight.Formatter formatter = new SimpleHTMLFormatter("<mark>", "</mark>");
            QueryScorer scorer = new QueryScorer(query);
            Highlighter highlighter = new Highlighter(formatter, scorer);
            highlighter.setTextFragmenter(new SimpleSpanFragmenter(scorer, 100));

            List<SearchResult> results = new ArrayList<>();
            for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
                Document doc = searcher.doc(scoreDoc.doc);
                String source = doc.get(FIELD_SOURCE);
                String name = doc.get(FIELD_NAME);
                String text = doc.get(FIELD_BODY);

                TokenStream tokenStream = TokenSources.getAnyTokenStream(reader, scoreDoc.doc, FIELD_BODY, analyzer);
                String snippet = highlighter.getBestFragment(tokenStream, text);
                
                if (snippet == null || snippet.isBlank()) {
                    snippet = text.length() > 100 ? text.substring(0, 100) + "..." : text;
                }

                results.add(new SearchResult(source, name, snippet));
            }
            return results;
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
