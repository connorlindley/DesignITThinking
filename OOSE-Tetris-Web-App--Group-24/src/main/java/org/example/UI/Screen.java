package org.example.UI;

import javafx.scene.Node;

public interface Screen {
    Node getScreen();
    void setRoute(String path, Screen screen);
}
