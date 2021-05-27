package indexing;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.LowerCaseFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.core.WhitespaceTokenizer;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexOptions;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.TopScoreDocCollector;
import org.apache.lucene.search.similarities.ClassicSimilarity;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.MMapDirectory;
import util.DocumentUtils;
import util.VectorSpace;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

class SimpleAnalyzer extends Analyzer {
    @Override
    protected TokenStreamComponents createComponents(String s) {
        Tokenizer source = new StandardTokenizer();
        TokenStream result = new LowerCaseFilter(source);
        return new TokenStreamComponents(source, result);
    }
}

class EmbeddingAnalyzer extends Analyzer {
    @Override
    protected TokenStreamComponents createComponents(String s) {
        Tokenizer source = new WhitespaceTokenizer();
        TokenStream result = new LowerCaseFilter(source);
        return new TokenStreamComponents(source, result);
    }
}

public class NearestNeighbourIndex implements DocumentIndex {
    private final VectorSpace wordEmbeddings;
    private final Directory index;
    private final Analyzer documentAnalyzer;
    private final Analyzer embeddingAnalyzer;
    private final Path indexPath;
    private final int quantizationFactor;

    public NearestNeighbourIndex(VectorSpace wordEmbeddings) throws IOException {
        this.indexPath = Paths.get("./indexes/" + "nearest_neighbor_" + wordEmbeddings.getName());
        this.wordEmbeddings = wordEmbeddings;
        this.index = new MMapDirectory(this.indexPath);
        this.documentAnalyzer = new SimpleAnalyzer();
        this.embeddingAnalyzer = new EmbeddingAnalyzer();
        this.quantizationFactor = 70;

        // Boolean queries get very long due to document encoding
        // 300 dimensions wont work without this
        BooleanQuery.setMaxClauseCount(2048);
    }

    @Override
    public void addDocuments(List<Document> documents) throws IOException {
        var fileCount = Optional
                .ofNullable(this.indexPath.toFile().listFiles())
                .orElseThrow();

        if (fileCount.length != 0) {
            return;
        }

        var indexConfiguration = new IndexWriterConfig(this.embeddingAnalyzer);
        var indexer = new IndexWriter(this.index, indexConfiguration);

        // Need to convert each document to one that the index understands
        for (var doc : documents) {
            var indexDoc = new Document();
            var content = doc.get(DocumentIndex.CONTENT_FIELD_NAME);
            var contentTokens = DocumentUtils.analyze(this.documentAnalyzer, content);

            // Add text fields to the token list, from which average vector is computer
            // Might be this negatively affects results, not sure
            for (var field : doc) {
                var opts = field.fieldType().indexOptions();
                if (opts != IndexOptions.NONE && !field.name().equals(DocumentIndex.CONTENT_FIELD_NAME))  {
                    var fieldContent = field.stringValue();
                    contentTokens.addAll(DocumentUtils.analyze(this.documentAnalyzer, fieldContent));
                }
                indexDoc.add(new StoredField(field.name(), field.stringValue()));
            }

            // Encode document vector using "fake words" method
            var documentVector = DocumentUtils.documentToVector(contentTokens, this.wordEmbeddings);
            if (documentVector.zeroed()) {
                continue;
            }

            var fakeWord = DocumentUtils.embeddingToFakeWord(documentVector, this.quantizationFactor);
            indexDoc.add(new TextField(DocumentIndex.CONTENT_FIELD_NAME, fakeWord, Field.Store.YES));

            indexer.addDocument(indexDoc);
        }

        indexer.forceMerge(1, true);
        indexer.close();
    }

    @Override
    public List<QueryResult> query(String queryStr, int depth) throws IOException, ParseException {
        var queryResults = new ArrayList<QueryResult>();
        var queryTokens = DocumentUtils.analyze(this.documentAnalyzer, queryStr);
        var queryVector = DocumentUtils.documentToVector(queryTokens, this.wordEmbeddings);

        if (queryVector.zeroed()) {
            throw new ParseException("Query vector could not be built");
        }

        var reader = DirectoryReader.open(this.index);
        var searcher = new IndexSearcher(reader);
        searcher.setSimilarity(new ClassicSimilarity());

        var queryFakeWord = DocumentUtils.embeddingToFakeWord(queryVector, this.quantizationFactor);
        var query = new QueryParser(DocumentIndex.CONTENT_FIELD_NAME, this.embeddingAnalyzer).parse(queryFakeWord);

        var collector = TopScoreDocCollector.create(depth, Integer.MAX_VALUE);
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
        return "nearest_neighbor_" + this.wordEmbeddings.getName();
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
