import indexing.DocumentIndex;
import indexing.InvertedIndex;
import indexing.NearestNeighbourIndex;
import util.DocumentUtils;
import util.VectorSpace;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

public class Main {
    private final Scanner sc = new Scanner(System.in);

    private String getUserInput(String msg, List<String> allowed) {
        while (true) {
            System.out.print(msg);
            var userInput = this.sc.nextLine().trim();
            if (allowed.isEmpty() || allowed.contains(userInput)) {
                return userInput;
            }
        }
    }

    private String getUserChoice(String msg, List<String> choices) {
        var allowedInputs = new ArrayList<String>();
        for (var index = 0; index < choices.size(); index++) {
            var choiceNum = String.valueOf(index + 1);
            System.out.println(choiceNum + ". " + choices.get(index));
            allowedInputs.add(choiceNum);
        }

        return getUserInput(msg, allowedInputs);
    }

    private DocumentIndex chooseDocumentIndex() throws IOException {
        var indexList = Arrays.asList(
                "inverse_model",
                "fasttext_model_100",
                "ssg_model_100",
                "word2vec_model_100",
                "fasttext_model_200",
                "ssg_model_200",
                "word2vec_model_200",
                "fasttext_model_300",
                "ssg_model_300",
                "word2vec_model_300"
        );

        var indexNum = getUserChoice("Select index: ", indexList);
        return switch (Integer.parseInt(indexNum)) {
            case 1 -> new InvertedIndex();
            case 2 -> new NearestNeighbourIndex(new VectorSpace(Paths.get("./embeddings/fasttext_model_100.txt")));
            case 3 -> new NearestNeighbourIndex(new VectorSpace(Paths.get("./embeddings/ssg_model_100.txt")));
            case 4 -> new NearestNeighbourIndex(new VectorSpace(Paths.get("./embeddings/word2vec_model_100.txt")));
            case 5 -> new NearestNeighbourIndex(new VectorSpace(Paths.get("./embeddings/fasttext_model_200.txt")));
            case 6 -> new NearestNeighbourIndex(new VectorSpace(Paths.get("./embeddings/ssg_model_200.txt")));
            case 7 -> new NearestNeighbourIndex(new VectorSpace(Paths.get("./embeddings/word2vec_model_200.txt")));
            case 8 -> new NearestNeighbourIndex(new VectorSpace(Paths.get("./embeddings/fasttext_model_300.txt")));
            case 9 -> new NearestNeighbourIndex(new VectorSpace(Paths.get("./embeddings/ssg_model_300.txt")));
            case 10 -> new NearestNeighbourIndex(new VectorSpace(Paths.get("./embeddings/word2vec_model_300.txt")));
            default -> throw new IllegalStateException("Unexpected value: " + Integer.parseInt(indexNum));
        };
    }

    private void viewMode() throws Exception {
        var index = chooseDocumentIndex();
        index.addDocuments(DocumentUtils.loadWikiDocuments(Paths.get("./documents")));

        System.out.printf("Created index %s  with size %d mb\n", index.getId(), index.getSize());

        while (true) {
            var query = getUserInput("Enter query or 0 to quit: ", new ArrayList<>());
            if (query.equals("0")) {
                return;
            }

            var startTime = System.nanoTime();
            var queryResults = index.query(query, 10);
            System.out.printf("Query elapsed time %d ms\n", TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime));
            for (var result : queryResults) {
                System.out.printf("[%s] [%f]\n", result.doc.get("title"), result.score);
            }

            System.out.println();
        }
    }

    public void run() {
        while (true) {
            try {
                var modes = Arrays.asList("View", "Quit");
                var modeChoice = getUserChoice("Select mode: ", modes);
                if (Integer.parseInt(modeChoice) == 1) {
                    viewMode();
                } else {
                    return;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {


        var app = new Main();
        app.run();
    }
}
