package indexing;

import org.apache.lucene.document.Document;
import org.apache.lucene.queryparser.classic.ParseException;

import java.io.IOException;
import java.util.List;

public interface DocumentIndex {
    String CONTENT_FIELD_NAME = "content";

    void addDocuments(List<Document> documents) throws IOException;
    List<QueryResult> query(String queryStr, int depth) throws IOException, ParseException;
    String getId();
    long getSize();
}
