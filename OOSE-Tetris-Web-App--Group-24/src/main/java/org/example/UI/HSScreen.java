package org.example.UI;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.scene.Node;

public class HSScreen implements Screen {
    private final Frame parent;
    private VBox highScoreScreen;
    private Screen mainScreen;

    // ---Field dimensions for rendering playfield (seperate from col and rows), used for scaling---
    private final double fieldWidth = 420; // play area width
    private final double fieldHeight = 640; // play area height

    public HSScreen(Frame frame) {
        parent = frame;
        buildScreen();
    }

    @Override
    public Node getScreen() {
        return highScoreScreen;
    }

    private void buildScreen() {
        highScoreScreen = new VBox(10);
        highScoreScreen.setPadding(new Insets(30));//
        Label labelscore = new Label("High Score");

        GridPane score = new GridPane();
        score.setPadding(new Insets(20));
        score.setHgap(fieldWidth * 0.5);
        score.setVgap(fieldHeight * 0.03);

        // Using an Array to store names and scores, so can later order from highest - lowest
        String[] names = {"Michelle","Gaines","Zachariah","Reem","Connor", "Tom","Makanaka","Peter","Anesu","Anotida"};
        int[] scoresArr = {34543,633,4568,7845,76348,7494,6474,8922,8922,6474};

        // Bubble Sort Algorithm (descending order by score)
        for (int i = 0; i < scoresArr.length - 1; i++) {
            for (int j = i + 1; j < scoresArr.length; j++) {
                if (scoresArr[j] > scoresArr[i]) {
                    int tmpScore = scoresArr[i];
                    scoresArr[i] = scoresArr[j];
                    scoresArr[j] = tmpScore;

                    String tmpName = names[i];
                    names[i] = names[j];
                    names[j] = tmpName;
                }
            }
        }

        // Add sorted results to GridPane
        for (int i = 0; i < names.length; i++) {
            score.add(new Label(names[i]), 0, i);
            score.add(new Label(String.valueOf(scoresArr[i])), 1, i);
        }

        Button back = new Button("Back");
        back.setAlignment(Pos.BOTTOM_CENTER);
        back.setOnAction(e -> parent.showScreen(mainScreen));
        highScoreScreen.getChildren().addAll(labelscore, score, back);
        //link external CSS
        //scene.getStylesheets().add(getClass().getResource("/assets/styles.css").toExternalForm());
    }

    @Override
    public void setRoute(String path, Screen screen) {
        if ("back".equals(path)) {
            mainScreen = screen;
            buildScreen();
        }
    }
}
