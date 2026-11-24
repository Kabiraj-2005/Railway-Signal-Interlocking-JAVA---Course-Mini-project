package com.trains.interlocking.ui;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

public class MainApp extends Application {

    @Override
    public void start(Stage stage) {
        BorderPane root = new BorderPane();
        root.setCenter(new Label("Railway Interlocking — setup OK"));

        stage.setTitle("Railway Interlocking");
        stage.setScene(new Scene(root, 800, 500));
        stage.show();
    }
}
