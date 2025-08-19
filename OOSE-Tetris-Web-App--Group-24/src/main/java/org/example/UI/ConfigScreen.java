package org.example.UI;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import org.example.model.Game;

public class ConfigScreen implements ScreenWithGame {
    private final Frame parent;
    private Game game;
    private VBox configScreen;
    private Screen mainScreen;

    private boolean hasShadow = false;
    private boolean music = false;
    private boolean soundEffect = false;
    private boolean aiPlay = false;
    private boolean extendMode = false;

    private String colorString = "RED";
    private int size = 10;
    private CheckBox status;
    Label statusLabel;
    Label sizeLabel = new Label("Size:" + size);


    private Color getColor() {
        return switch (colorString) {
            case "RED" -> Color.RED;
            case "GREEN" -> Color.GREEN;
            case "BLUE" -> Color.BLUE;
            default -> Color.BLACK;

        };
    }

    public ConfigScreen(Frame frame) {
        parent = frame;
        //configScreen = new BorderPane();
    }

    private void buildScreen() {
        configScreen = new VBox(10);
        configScreen.setPadding(new Insets(20));//
        //STACKPANE TO MAKE SHURE THE VBOX ON TOP OF VIDEO
        //STACKPANE splashlayout = new StackPane

        Label label = new Label("Configuration");
        Label widthlabel = new Label("Field Width");
        Label heightlabel = new Label("Field Height");
        Label gameLevelabel = new Label("Game Level ");


        Slider widthSlider = new Slider(5, 20, 12);
        widthSlider.setShowTickLabels(true);
        widthSlider.setShowTickMarks(true);
        widthSlider.setMajorTickUnit(1);
        widthSlider.valueProperty().addListener((obs, oldVal, newVal) -> {


        });
        Slider heightSlider = new Slider(15, 30, 12);
        heightSlider.setShowTickLabels(true);
        heightSlider.setShowTickMarks(true);
        heightSlider.setMajorTickUnit(1);
        heightSlider.valueProperty().addListener((obs, oldVal, newVal) -> {

        });

        Slider gamelevelSlider = new Slider(1, 10, 1);
        gamelevelSlider.setShowTickLabels(true);
        gamelevelSlider.setShowTickMarks(true);
        gamelevelSlider.setMajorTickUnit(1);

        gamelevelSlider.valueProperty().addListener((obs, oldVal, newVal) -> {


        });


        //Gridpane and controlls


        CheckBox ms = new CheckBox("Music(on/off)");

        ms.setSelected(music);

        Label musiclabel = new Label("off");
        ms.setOnAction(e -> {
            music = ms.isSelected();
            musiclabel.setText((music ? "on" : "off"));
        });

        HBox musicStatus = new HBox(120, ms, musiclabel);
        musicStatus.setAlignment(Pos.CENTER_LEFT);

        CheckBox se = new CheckBox("Sound Effect(on/off)");
        se.setSelected(soundEffect);
        Label soundlabel = new Label("off");
        se.setOnAction(e -> {
            soundEffect = se.isSelected();
            soundlabel.setText((soundEffect ? "on" : "off"));
        });

        HBox soundStatus = new HBox(80, se, soundlabel);
        soundStatus.setAlignment(Pos.CENTER_LEFT);

        CheckBox ai = new CheckBox("AI Play(on/off)");
        ai.setSelected(aiPlay);
        Label ailabel = new Label("off");
        ai.setOnAction(e -> {
            aiPlay = ai.isSelected();
            ailabel.setText((aiPlay ? "on" : "off"));
        });

        HBox aiStatus = new HBox(120, ai, ailabel);
        aiStatus.setAlignment(Pos.CENTER_LEFT);

        CheckBox em = new CheckBox("Extend Mode(on/off)");
        em.setSelected(extendMode);
        Label emlabel = new Label("off");
        em.setOnAction(e -> {
            extendMode = em.isSelected();
            emlabel.setText((extendMode ? "on" : "off"));
        });

        HBox emStatus = new HBox(80, em, emlabel);
        emStatus.setAlignment(Pos.CENTER_LEFT);

        GridPane grid = new GridPane();
        grid.setPadding(new Insets(20));
        grid.setHgap(80);
        grid.setVgap(10);

        grid.add(widthlabel, 0, 0);
        grid.add(widthSlider, 1, 0);

        grid.add(heightlabel, 0, 1);
        grid.add(heightSlider, 1, 1);

        grid.add(gameLevelabel, 0, 2);
        grid.add(gamelevelSlider, 1, 2);


        Button back = new Button("Back");
        back.setOnAction(e -> parent.showScreen(mainScreen));
        configScreen.getChildren().addAll(label, grid, musicStatus, soundStatus, aiStatus, emStatus, back);
    }

    @Override
    public Node getScreen() {
        return configScreen;
    }

    @Override
    public void setGame(Game game) {
        this.game = game;
    }

    @Override
    public void setRoute(String path, Screen screen) {
        if ("back".equals(path)) {
            mainScreen = screen;
            buildScreen();
        }
    }

    public static class GamePane {
    }
}
