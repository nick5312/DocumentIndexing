package indexing;

import org.apache.lucene.document.Document;

public class QueryResult {
    public Document doc;
    public float score;

    public QueryResult(Document doc, float score) {
        this.doc = doc;
        this.score = score;
    }
}
