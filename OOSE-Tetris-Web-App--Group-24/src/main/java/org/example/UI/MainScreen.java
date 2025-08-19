package org.example.UI;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.scene.Node;

public class MainScreen implements Screen {
    private Screen configScreen, gameScreen, highScoreScreen;
    private Frame parent;
    private VBox mainScreen;

    @Override
    public Node getScreen() {
        return mainScreen;
    }

    @Override
    public void setRoute(String path, Screen screen) {
        switch (path) {
            case "config" -> configScreen = screen;
            case "game" -> gameScreen = screen;
            case "hs" -> highScoreScreen = screen;
            default -> {
            }
        }
    }

    public MainScreen(Frame frame) {
        parent = frame;
        buildScreen();
    }

    private void buildScreen(){
        mainScreen = new VBox(10);
        mainScreen.setAlignment(Pos.CENTER);
        mainScreen.setPadding(new Insets(20));
        Label label = new Label("Main Screen");

        Button startButton = new Button("Start Game"); //making the button
        startButton.setOnAction(e -> parent.startGame());

        Button configButton = new Button("Configuration");
        configButton.setOnAction(e -> parent.showScreen(configScreen));//so when you click the button it works

        Button highscoreButton = new Button("High score");
        highscoreButton.setOnAction(e -> parent.showScreen(highScoreScreen));
        Button exitButton = new Button("Exit");
        exitButton.setOnAction(e -> parent.exitPopUp());

        // Authors at bottom, seen in demo
        Label authorsLabel = new Label("Authors: Zachariah, Connor, Michelle, Reem");
        authorsLabel.getStyleClass().add("authorsLabel");
        BorderPane.setAlignment(authorsLabel, Pos.CENTER);
        //mainScreen.setBottom(authorsLabel);


        mainScreen.getChildren().addAll(label, startButton, configButton, highscoreButton, exitButton, authorsLabel);//to combine everything
    }
}