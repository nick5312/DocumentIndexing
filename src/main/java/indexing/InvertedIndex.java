package indexing;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.lv.LatvianAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.TopScoreDocCollector;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.MMapDirectory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class InvertedIndex implements DocumentIndex {
    private final Directory index;
    private final Analyzer analyzer;
    private final Path indexPath;

    public InvertedIndex() throws IOException {
        this.indexPath = Paths.get("./indexes/index_inverted");
        this.index = new MMapDirectory(indexPath);
        this.analyzer = new LatvianAnalyzer();
    }

    @Override
    public void addDocuments(List<Document> documents) throws IOException {
        var fileCount = Optional
                .ofNullable(this.indexPath.toFile().listFiles())
                .orElseThrow();

        if (fileCount.length != 0) {
            return;
        }

        var indexConfiguration = new IndexWriterConfig(analyzer);
        var indexer = new IndexWriter(this.index, indexConfiguration);

        indexer.addDocuments(documents);
        indexer.forceMerge(1, true);
        indexer.close();
    }

    @Override
    public List<QueryResult> query(String queryStr, int depth) throws IOException, ParseException {
        var queryResults = new ArrayList<QueryResult>();
        var query = new QueryParser(DocumentIndex.CONTENT_FIELD_NAME, this.analyzer).parse(queryStr);

        var reader = DirectoryReader.open(this.index);
        var searcher = new IndexSearcher(reader);

        var collector = TopScoreDocCollector.create(depth, 50);
        searcher.search(query, collector);
        var hits = collector.topDocs().scoreDocs;

        for (var hit : hits) {
            var queryResult = new QueryResult(searcher.doc(hit.doc), hit.score);
            queryResults.add(queryResult);
        }

        reader.close();

        return queryResults;
    }

    @Override
    public String getId() {
        return "inverted";
    }

    @Override
    public long getSize() {
        try {
            return Files.walk(this.indexPath)
                    .filter(p -> p.toFile().isFile())
                    .mapToLong(p -> p.toFile().length())
                    .sum() / (1024 * 1024);
        } catch (Exception ignored) {}
        return 0;
    }
}
