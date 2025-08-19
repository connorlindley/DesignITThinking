package org.example;

import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import java.util.Arrays;
import java.util.Optional;
import org.example.UI.Screen;
import org.example.UI.*;


public class Main extends Application implements Frame {

    // ---Main container (UI structure)---
    private StackPane root;
    private Scene scene;

    // ---Automatically state configuration checkboxes as 'false'---
    private boolean music = false;
    private boolean soundEffect = false;
    private boolean aiPlay = false;
    private boolean extendMode = false;

    // ---Tetris board sizes---
    private static final int COLS = 10; // columns
    private static final int ROWS = 20; // rows
    private static final int CELL = 20; // size of each block

    // ---Board state and active pieces---
    private Pane boardPane;
    private int[][] board = new int[ROWS][COLS]; // grid: 0=empty, >0=filled with color index
    private Piece active; // current falling tetris piece

    // ---Gameplay control flags---
    private boolean paused = false; // True if game is paused (automatically unpaused)
    private boolean softDropHeld = false; // Speed up when down key is held (auto fall rate is normal gravity)

    // ---Timing variables (gravity/speed)---
    private long fallDelayMs = 700; // normal gravity (milliseconds)
    private long softDropDelayMs = 60; // faster when Down key is held
    private long lastFallAtMs = 0; // Timestamp of last automatic fall, calculate when next piece should drop

    // ---Main game loop---
    private AnimationTimer gameLoop; // main loop
    private StackPane pauseOverlay; // shown when paused

    // ---Field dimensions for rendering playfield (seperate from col and rows), used for scaling---
    private final double fieldWidth = 420; // play area width
    private final double fieldHeight = 640; // play area height

    // --------------------------------------Pause Overlay----------------------------------------
    // Builds a dark, click-blocking overlay with "Paused" + Resume/Main Menu actions.
    // Shown/hidden by showPauseOverlay()/hidePauseOverlay().

    private StackPane buildPauseOverlay() {
        Label pausedLbl = new Label("Paused");
        pausedLbl.getStyleClass().add("mainMenuLabel");

        Button resume = new Button("Resume");
        resume.setFocusTraversable(false);
        resume.setOnAction(e -> {togglePause(); Platform.runLater(() -> boardPane.requestFocus());}); // unpause

        Button toMenu = new Button("Main Menu");
        toMenu.setOnAction(e -> {
            stopGameLoop(); // stop timer so it isn't updated off-screen
            buildScreens(); // return to main menu
        });

        VBox box = new VBox(12, pausedLbl, resume, toMenu);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(20));
        box.getStyleClass().add("pauseCard");

        StackPane overlay = new StackPane(box);
        overlay.setPickOnBounds(true); // block clicking game
        overlay.setStyle("-fx-background-color: rgba(0,0,0,0.35);"); // dimming
        return overlay;
    }

    // ---Toggle visibility---
    private void showPauseOverlay() { pauseOverlay.setVisible(true); }
    private void hidePauseOverlay() { pauseOverlay.setVisible(false); }

    // ---Flips pause state. When resuming, reset timing so gravity doesn’t “catch up”---
    private void togglePause() {
        paused = !paused;
        if (paused) {
            showPauseOverlay();
        } else {
            hidePauseOverlay();
            boardPane.requestFocus();
            lastFallAtMs = nowMs(); // prevent immediate drop after resume
        }
    }

    // ------------------------------------Game over Overlay---------------------------------------
    // Visible only when game is over.

    private StackPane buildGameOverOverlay() {
        Label over = new Label("GAME OVER");
        over.getStyleClass().add("mainMenuLabel");

        Label hint = new Label("Go back to Main Menu to start a new game");
        hint.getStyleClass().add("subtitleLabel");

        Button toMenu = new Button("Main Menu");
        toMenu.setOnAction(e -> {
            stopGameLoop();
            buildScreens();
        });

        VBox box = new VBox(12, over, hint, toMenu);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(20));
        box.getStyleClass().add("pauseCard");

        StackPane overlay = new StackPane(box);
        overlay.setPickOnBounds(true); // blocks clicks to game
        overlay.setStyle("-fx-background-color: rgba(0,0,0,0.55);");
        overlay.setVisible(false); // hidden by default
        return overlay;
    }

    // -----------------------------------------Game Loop---------------------------------------------
    // Starts (or restarts) the per-frame update loop, using AnimationTimer
    // Spawns first tetromino shape shortly after entering Play (500ms)
    // Applies gravity based on normal or soft-drop delay (holding down)
    // On landing: lock piece, clear lines, spawn next, then redraw

    private void startGameLoop() {
        if (gameLoop != null) gameLoop.stop(); // ensure only one loop is running

        gameLoop = new AnimationTimer() {
            @Override public void handle(long now) {
                if (paused) return; // skip updates when paused

                if (active == null && nowMs() - lastFallAtMs >= 500) { // Ensure a piece exists within 500ms of entering Play:
                    spawnNewPiece();
                    drawAll();
                    lastFallAtMs = nowMs();
                    return;
                }

                // Gravity tick: choose delay based on whether soft drop is held
                long delay = softDropHeld ? softDropDelayMs : fallDelayMs;
                if (active != null && nowMs() - lastFallAtMs >= delay) {
                    if (!trySoftFall()) { // can't move down
                        lockPiece(); // fix piece into the board[][]
                        clearCompletedLines(); // remove full rows
                        spawnNewPiece(); // next piece
                    }
                    drawAll(); // redraw board + active piece
                    lastFallAtMs = nowMs(); // schedule next gravity tick
                }
            }
        };
        gameLoop.start();
    }

    // ---Stops the loop safely (used when leaving Play or going to menu)---
    private void stopGameLoop() {
        if (gameLoop != null) gameLoop.stop();
    }
    private static long nowMs() { return System.nanoTime() / 1_000_000L; }

    // -------------------------------------Piece Model------------------------------------------
    // Represents one tetromino shape and four different rotation states

    private static class Piece {
        int colour; // 1..7 for color index
        int rot; // 0..3
        int x, y; // board coordinates of the piece anchor
        int[][][] shapes; // rotation → up to 4 (x,y) cell offsets

        Piece(int colour, int[][][] shapes, int x, int y) {
            this.colour = colour;
            this.shapes = shapes;
            this.x = x;
            this.y = y;
            this.rot = 0;
        }

        int[][] cells() { return shapes[rot]; } // Returns the current rotation's cell offsets.
        void rotateCW()  { rot = (rot + 1) & 3; } // Rotate clockwise (wrap 0..3).
        void rotateCCW() { rot = (rot + 3) & 3; }  // Rotate counter-clockwise (wrap 0..3).
    }

    // -------------------------Building 7 Tetromino Shapes (clockwise rotations)---------------------------
    private Piece randomPiece() {
        int mid = COLS / 2 - 2; // Spawn near centre
        switch ((int)(Math.random()*7)) { // Shapes should fall at random
            case 0: // Piece I
                return new Piece(1, new int[][][]{
                        {{0,1},{1,1},{2,1},{3,1}},
                        {{2,0},{2,1},{2,2},{2,3}},
                        {{0,2},{1,2},{2,2},{3,2}},
                        {{1,0},{1,1},{1,2},{1,3}},
                }, mid, -2);
            case 1: // Piece O
                return new Piece(2, new int[][][]{
                        {{1,0},{2,0},{1,1},{2,1}},
                        {{1,0},{2,0},{1,1},{2,1}},
                        {{1,0},{2,0},{1,1},{2,1}},
                        {{1,0},{2,0},{1,1},{2,1}},
                }, mid, -1);
            case 2: // Piece T
                return new Piece(3, new int[][][]{
                        {{1,0},{0,1},{1,1},{2,1}},
                        {{1,0},{1,1},{2,1},{1,2}},
                        {{0,1},{1,1},{2,1},{1,2}},
                        {{1,0},{0,1},{1,1},{1,2}},
                }, mid, -2);
            case 3: // Piece S
                return new Piece(4, new int[][][]{
                        {{1,0},{2,0},{0,1},{1,1}},
                        {{1,0},{1,1},{2,1},{2,2}},
                        {{1,1},{2,1},{0,2},{1,2}},
                        {{0,0},{0,1},{1,1},{1,2}},
                }, mid, -2);
            case 4: // Piece Z
                return new Piece(5, new int[][][]{
                        {{0,0},{1,0},{1,1},{2,1}},
                        {{2,0},{1,1},{2,1},{1,2}},
                        {{0,1},{1,1},{1,2},{2,2}},
                        {{1,0},{0,1},{1,1},{0,2}},
                }, mid, -2);
            case 5: // Piece J
                return new Piece(6, new int[][][]{
                        {{0,0},{0,1},{1,1},{2,1}},
                        {{1,0},{2,0},{1,1},{1,2}},
                        {{0,1},{1,1},{2,1},{2,2}},
                        {{1,0},{1,1},{0,2},{1,2}},
                }, mid, -2);
            default: // Piece L
                return new Piece(7, new int[][][]{
                        {{2,0},{0,1},{1,1},{2,1}},
                        {{1,0},{1,1},{1,2},{2,2}},
                        {{0,1},{1,1},{2,1},{0,2}},
                        {{0,0},{1,0},{1,1},{1,2}},
                }, mid, -2);
        }
    }

    // ----------------------------Setting Shape Colours---------------------------------------
    private Rectangle cellRect(int cx, int cy, int colorIdx) {
        Rectangle r = new Rectangle(cx * CELL, cy * CELL, CELL, CELL);
        r.getStyleClass().add("mino");
        r.setFill(switch (colorIdx) {
            case 1 -> Color.CYAN;   // I
            case 2 -> Color.YELLOW; // O
            case 3 -> Color.PURPLE; // T
            case 4 -> Color.LIME;   // S
            case 5 -> Color.RED;    // Z
            case 6 -> Color.BLUE;   // J
            case 7 -> Color.ORANGE; // L
            default -> Color.GRAY;
        });
        r.setStroke(Color.rgb(0,0,0,0.18));
        return r;
    }

    // ----------------------------Collision Checks and Movement----------------------------------

    // ---Returns true if piece p could be placed at (nx, ny) with rotation nro ---
    private boolean canPlace(Piece p, int nx, int ny, int nrot) {
        int prevRot = p.rot;
        p.rot = nrot & 3; // clamp to 0..3

        for (int[] c : p.cells()) {
            int x = nx + c[0];
            int y = ny + c[1];

            if (x < 0 || x >= COLS || y >= ROWS) { // Out of bounds (left/right/bottom)
                p.rot = prevRot;
                return false;
            }
            if (y >= 0 && board[y][x] != 0) { // Collision with existing block (ignore cells above top: y < 0)
                p.rot = prevRot;
                return false;
            }
        }
        p.rot = prevRot;
        return true;
    }

    // ---Attempt to move an active piece by dy/dx (fast fall)---
    private void tryMove(int dx, int dy) {
        if (active == null) return;
        int nx = active.x + dx;
        int ny = active.y + dy;

        if (canPlace(active, nx, ny, active.rot)) {
            active.x = nx; active.y = ny;
            drawAll();
        }
    }

    // ---Attempt to move active piece by 1 row down (soft fall)---
    private boolean trySoftFall() {
        if (active == null) return false;

        if (canPlace(active, active.x, active.y + 1, active.rot)) {
            active.y += 1;
            return true;
        }
        return false;
    }

    // ---Attempting to rotate active piece clockwise---
    private void tryRotateCW() {
        if (active == null) return;
        int newRot = (active.rot + 1) & 3;

        int[][] kicks = { {0,0}, {-1,0}, {1,0}, {-2,0}, {2,0} }; // simple left/right kicks

        for (int[] k : kicks) {
            int nx = active.x + k[0];
            int ny = active.y + k[1];

            if (canPlace(active, nx, ny, newRot)) { // if no kick works, keep current rotation/position
                active.x = nx;
                active.y = ny;
                active.rot = newRot;
                drawAll();
                return;
            }
        }
    }

    // -------------------------------------Piece Lifecycle-------------------------------------------

    // ---Spawn new piece into 'active'---
    private void spawnNewPiece() {
        active = randomPiece();

        if (!canPlace(active, active.x, active.y, active.rot)) { // if cannot place at spawn -> game over
            for (int r = 0; r < ROWS; r++) Arrays.fill(board[r], 0);
            drawAll(); // clear board

            Label gameOverMsg = new Label("GAME OVER – Use Back to return to Main Menu"); // Message shown for game over
            gameOverMsg.getStyleClass().add("gameOverLabel");

            ((StackPane) boardPane.getParent()).getChildren().add(gameOverMsg);
            stopGameLoop();
        }
    }

    // ---Locking active piece into board grid (copy cell to board [][])
    private void lockPiece() {
        if (active == null) return;

        for (int[] c : active.cells()) {
            int x = active.x + c[0];
            int y = active.y + c[1];

            if (y >= 0 && y < ROWS && x >= 0 && x < COLS) {
                board[y][x] = active.colour;
            }
        }
        active = null;
    }

    // ---Clear any fully-complete rows and compacts board down---
    private void clearCompletedLines() {
        int write = ROWS - 1;

        for (int r = ROWS - 1; r >= 0; r--) { // For each non-full row r, copy it to 'write' and decrement 'write'
            boolean full = true;
            for (int c = 0; c < COLS; c++) if (board[r][c] == 0) { full = false; break; }
            if (!full) {
                if (write != r) board[write] = Arrays.copyOf(board[r], COLS);
                write--;
            }
        }
        for (int r = write; r >= 0; r--) Arrays.fill(board[r], 0); // fill remaining rows at top with empty
    }

    // ------------------------------------------Rendering------------------------------------------------

    // ---Redraw entire board with active pieces----
    private void drawAll() {
        boardPane.getChildren().removeIf(n -> n instanceof Rectangle); // remove previous blocks (keep grid lines which are Lines)

        for (int r = 0; r < ROWS; r++) { // draw locked board
            for (int c = 0; c < COLS; c++) {
                int colour = board[r][c];
                if (colour != 0) boardPane.getChildren().add(cellRect(c, r, colour));
            }
        }

        if (active != null) { // draw active piece on top
            for (int[] cc : active.cells()) {
                int x = active.x + cc[0];
                int y = active.y + cc[1];
                if (y >= 0) boardPane.getChildren().add(cellRect(x, y, active.colour)); // ignore hidden cells above the top
            }
        }
    }


    // =====================================================================================================
    //                                   Code for setting up Screens
    // =====================================================================================================

    // -------------------------------Startup Screen with Splash Screen-------------------------------

    private void buildScreens(){
        Screen mainScreen = new MainScreen(this);
        ScreenWithGame configScreen = new ConfigScreen(this);
        //ScreenWithGame gameScreen = new GameScreen(this);
        Screen highScoreScreen = new HSScreen(this);

        mainScreen.setRoute("config", configScreen);
        //mainScreen.setRoute("game", gameScreen);
        mainScreen.setRoute("hs", highScoreScreen);

        //configScreen.setGame(game);
        configScreen.setRoute("back", mainScreen);

        //gameScreen.setGame(game);
        //gameScreen.setRoute("back", mainScreen);

        highScoreScreen.setRoute("back", mainScreen);

        showScreen(mainScreen);
    }

    @Override
    public void start(Stage primaryStage) {
        root = new StackPane();
        scene = new Scene(root, fieldWidth, fieldHeight);
        Stage splashStage = new Stage(StageStyle.UNDECORATED);
        ImageView splashImage = new ImageView(new Image(getClass().getResource("/assets/splashImage.png").toExternalForm())); // Path to image.
        splashImage.setFitHeight(fieldHeight);
        splashImage.setFitWidth(fieldWidth);
        splashImage.setSmooth(true);

        Label groupLabel = new Label("Group 24 Tetris Project");
        groupLabel.getStyleClass().add("grouplabel");
        scene.getStylesheets().add(getClass().getResource("/assets/styles.css").toExternalForm());

        StackPane splashLayout = new StackPane(splashImage, groupLabel);
        StackPane.setAlignment(groupLabel, Pos.BOTTOM_CENTER);
        Scene splashScene = new Scene(splashLayout, fieldWidth, fieldHeight); // Size of pop up window
        splashScene.getStylesheets().add(getClass().getResource("/assets/styles.css").toExternalForm());


        splashStage.setScene(splashScene);
        splashStage.show();

        Task<Void> loadTask = new Task<>(){ // Timer for how long splash screen appears
            @Override
            protected Void call() throws Exception{
                Thread.sleep(3000); // Display for approx 3.0s
                return null;
            }
            @Override
            protected void succeeded() {
                Platform.runLater(()->{
                    splashStage.close();
                    //showMainScreen();
                    primaryStage.show();
                });
            }
        };
        new Thread(loadTask).start();
        buildScreens();

        //showMainScreen();// CALLING THE FUNCTION

        primaryStage.setTitle("JavaFX Multi-Screen Game");
        primaryStage.setScene(scene);
        //primaryStage.show();// prints the things for the user

        //Closing application
        primaryStage.setOnCloseRequest(event ->{
            event.consume();
            exitPopUp();
        });
    }

    public void showScreen(Screen scr) {
        root.getChildren().setAll(scr.getScreen());
    }

    // --------------------------------------Game Screen----------------------------------------
    @Override
    public void startGame() {
        showGameScreen();
    }

    private void showGameScreen() {
        BorderPane gameLayout = new BorderPane();
        gameLayout.setPadding(new Insets(20));

        Label title = new Label("Play");
        title.getStyleClass().add("playTitle");
        BorderPane.setAlignment(title, Pos.CENTER);
        gameLayout.setTop(title);
        Label pause = new Label("Press 'p' to pause game");
        pause.getStyleClass().add("pauseLabel");

        double boardW = COLS * CELL;
        double boardH = ROWS * CELL;

        boardPane = new Pane();
        boardPane.setPrefSize(boardW, boardH);
        boardPane.setMinSize(boardW, boardH);
        boardPane.setMaxSize(boardW, boardH);
        boardPane.getStyleClass().add("tetrisBoard");

        // ---Draw faint grid lines---
        boardPane.getChildren().clear();
        for (int c = 0; c <= COLS; c++) {
            Line v = new Line(c * CELL, 0, c * CELL, boardH);
            v.getStyleClass().add("gridLine");
            boardPane.getChildren().add(v);
        }
        for (int r = 0; r <= ROWS; r++) {
            Line h = new Line(0, r * CELL, boardW, r * CELL);
            h.getStyleClass().add("gridLine");
            boardPane.getChildren().add(h);
        }

        StackPane wrapper = new StackPane(boardPane);
        wrapper.setAlignment(Pos.CENTER);
        gameLayout.setCenter(wrapper);

        Button backBtn = new Button("Back"); // Bottom of screen: Back + author
        backBtn.setOnAction(e -> {
            if (paused == true) { //Statement checks if game is paused to ensure it remembers state
                quitGamePopUp();
            } else {
                togglePause();
                quitGamePopUp();
            }
        });
        HBox backBar = new HBox(backBtn);
        backBar.getStyleClass().add("backBar");

        Label author = new Label("Authors: Group 24");
        author.getStyleClass().add("authorFooter");

        VBox bottomBox = new VBox(backBar, author);
        bottomBox.setAlignment(Pos.CENTER);
        gameLayout.setBottom(bottomBox);

        pauseOverlay = buildPauseOverlay(); // Pause overlay
        StackPane centerStack = new StackPane(wrapper, pauseOverlay);
        gameLayout.setCenter(centerStack);
        hidePauseOverlay();

        root.getChildren().setAll(gameLayout);

        //scene.getWindow().setWidth(fieldWidth);
        //scene.getWindow().setHeight(fieldHeight);

        for (int r = 0; r < ROWS; r++) Arrays.fill(board[r], 0); // Reset board & state
        active = null;
        paused = false;
        softDropHeld = false;

        Platform.runLater(() -> root.requestFocus());
        boardPane.setOnMouseClicked(e -> root.requestFocus());

        // ---Key inputs (slide-style, simple)---
        scene.setOnKeyPressed(ev -> {
            ev.consume();
            if (ev.getCode() == KeyCode.P) {   // pause works anytime pressing key 'p'
                togglePause();
                return;
            }
            if (paused) return;

            switch (ev.getCode()) {
                case LEFT  -> tryMove(-1, 0); // left arrow moves shape left
                case RIGHT -> tryMove(1, 0); // right arrow moves shape right
                case UP    -> tryRotateCW(); // up arrow rotates shape
                case DOWN  -> softDropHeld = true; // down arrow forces drop
                default -> {}
            }
        });

        scene.setOnKeyReleased(ev -> {
            ev.consume();
            if (ev.getCode() == KeyCode.DOWN) {
                softDropHeld = false;
            }
        });

        lastFallAtMs = nowMs(); // Start loop: spawn first piece within 500ms
        startGameLoop();
    }

    // ----------------------------------Exiting Application------------------------------------
    //@Override
    public void exitPopUp() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Exit Confirmation");
        alert.setContentText("Are you sure you want to exit?");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK){
            System.exit(0);
        }
    }

    public void quitGamePopUp() {
        Alert endGameAlert = new Alert(Alert.AlertType.CONFIRMATION);
        endGameAlert.setTitle("Stop Game Confirmation");
        endGameAlert.setContentText("Are you sure you want to stop playing?");

        Optional<ButtonType> result = endGameAlert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK){
            stopGameLoop();
            buildScreens();
        } //else if (paused = true){
        //togglePause();
        //}
    }

    // -------------------------------------Java Entry Point--------------------------------------
    public static void main(String[] args) {
        launch(args);
    }
}