package com.chatapp.client;

import com.chatapp.client.util.SceneManager;
import javafx.application.Application;
import javafx.stage.Stage;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) {
        SceneManager.init(primaryStage);
        primaryStage.setMinWidth(1000);
        primaryStage.setMinHeight(650);
        SceneManager.show("login", "ChatApp", 1000, 650);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
