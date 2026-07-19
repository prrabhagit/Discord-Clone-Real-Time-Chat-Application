package com.chatapp.client.util;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Objects;

/** Small helper around the primary Stage for swapping between top-level screens. */
public final class SceneManager {

    private static Stage primaryStage;

    private SceneManager() {
    }

    public static void init(Stage stage) {
        primaryStage = stage;
    }

    public static Stage stage() {
        return primaryStage;
    }

    /** Loads an FXML view (from /fxml/&lt;name&gt;.fxml) and shows it, applying the shared dark theme. */
    public static <T> T show(String fxmlName, String title, double width, double height) {
        try {
            FXMLLoader loader = new FXMLLoader(Objects.requireNonNull(
                    SceneManager.class.getResource("/fxml/" + fxmlName + ".fxml")));
            Parent root = loader.load();
            Scene scene = new Scene(root, width, height);
            scene.getStylesheets().add(Objects.requireNonNull(
                    SceneManager.class.getResource("/css/dark-theme.css")).toExternalForm());
            primaryStage.setTitle(title);
            primaryStage.setScene(scene);
            primaryStage.centerOnScreen();
            primaryStage.show();
            return loader.getController();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load view: " + fxmlName, e);
        }
    }
}
