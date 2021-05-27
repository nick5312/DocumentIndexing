package util;

import indexing.DocumentIndex;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.document.TextField;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class DocumentUtils {
    public static ArrayList<String> analyze(Analyzer analyzer, String source) throws IOException {
        var result = new ArrayList<String>();
        var tokenStream = analyzer.tokenStream(null, new StringReader(source));
        tokenStream.reset();

        while (tokenStream.incrementToken()) {
            var tokenValue = tokenStream.getAttribute(CharTermAttribute.class).toString();
            if (tokenValue.length() > 0) {
                result.add(tokenValue);
            }
        }

        tokenStream.end();
        tokenStream.close();

        return result;
    }

    public static String embeddingToFakeWord(FloatVector documentVec, double quantizationFactor) {
        var encodingTokens = new ArrayList<String>();
        for (var i = 0; i < documentVec.getLength(); i++) {
            var vecElement = documentVec.get(i);
            var integerVectorElement = (int)Math.floor(vecElement * quantizationFactor);
            // Template - f{dimension_num}{n if negative}
            var prefix = "f" + (i + 1) + (integerVectorElement < 0 ? "n" : "");

            for (var j = 0; j < Math.abs(integerVectorElement); j++) {
                encodingTokens.add(prefix);
            }
        }

        return String.join(" ", encodingTokens);
    }

    public static FloatVector documentToVector(ArrayList<String> tokens, VectorSpace vectorSpace) {
        var documentVectorTermCount = 0;
        var documentVector = new FloatVector(vectorSpace.getDimensions());

        for (var token : tokens) {
            var tokenEmbedding = vectorSpace.getWordVector(token);
            if (!tokenEmbedding.zeroed()) {
                documentVector.add(tokenEmbedding);
                documentVectorTermCount++;
            }
        }

        // Calculate average word vector from all document words
        // Normalization required for similarity calculations to be precise
        documentVector.div(documentVectorTermCount);
        documentVector.normalize();

        return documentVector;
    }

    public static List<Document> loadWikiDocuments(Path documentPath) throws ParserConfigurationException, IOException, SAXException {
        // Loads documents exported by wikiExtractor tool
        var result = new ArrayList<Document>();
        var factory = DocumentBuilderFactory.newInstance();
        var builder = factory.newDocumentBuilder();
        var documentFolder = documentPath.toFile();

        for (var documentSubfolder : Objects.requireNonNull(documentFolder.listFiles())) {
            for (var xmlFile : Objects.requireNonNull(documentSubfolder.listFiles())) {
                var xmlDocumentContent = "<root>" + Files.readString(xmlFile.toPath()) + "</root>";
                var xmlInputStream = new InputSource(new StringReader(xmlDocumentContent));
                var xmlDocument = builder.parse(xmlInputStream);

                var documentList = xmlDocument.getElementsByTagName("doc");
                for (var i = 0; i < documentList.getLength(); i++) {
                    var documentElement = (Element)documentList.item(i);
                    var title = documentElement.getAttribute("title");
                    var url = documentElement.getAttribute("url");
                    var content = documentElement.getTextContent();

                    var doc = new Document();
                    doc.add(new StoredField("url", url));
                    doc.add(new TextField("title", title, Field.Store.YES));
                    doc.add(new TextField(DocumentIndex.CONTENT_FIELD_NAME, content, Field.Store.YES));
                    result.add(doc);
                }
            }
        }

        System.out.println(result.size());
        return result;
    }
}
