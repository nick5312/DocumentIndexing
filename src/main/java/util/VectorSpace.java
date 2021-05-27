package util;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class VectorSpace {
    private final ConcurrentHashMap<String, FloatVector> data;
    private final String name;
    private final int dimensions;
    private final int vocabularySize;

    public VectorSpace(Path dataPath) throws IOException {
        var fileName = dataPath.getFileName().toString();
        var reader = new BufferedReader(new FileReader(dataPath.toFile()));
        var metaData = reader.readLine().split("\\s+");

        this.data = new ConcurrentHashMap<>();
        this.vocabularySize = Integer.parseInt(metaData[0]);
        this.dimensions = Integer.parseInt(metaData[1]);
        this.name = fileName.substring(0, fileName.lastIndexOf("."));

        var currentLineCount = 0;
        var lines = new ArrayList<String>(this.vocabularySize);
        var line = "";

        Consumer<String> processor = (String x) -> {
            var splitPos = x.indexOf(' ');
            var term = x.substring(0, splitPos);
            var vectorData = x.substring(splitPos + 1);
            this.data.put(term, new FloatVector(vectorData, this.dimensions));
        };

        while (line != null) {
            while ((line = reader.readLine()) != null && currentLineCount < 200000) {
                lines.add(line);
                currentLineCount += 1;
            }

            currentLineCount = 0;
            lines.parallelStream().forEach(processor);
            lines.clear();
        }

        reader.close();
    }

    public String getName() {
        return this.name;
    }

    public int getDimensions() {
        return this.dimensions;
    }

    public int getVocabularySize() {
        return this.vocabularySize;
    }

    public FloatVector getWordVector(String word) {
        return Optional
                .ofNullable(this.data.get(word))
                .orElse(new FloatVector(this.dimensions));
    }
}
