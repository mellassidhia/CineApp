package com.cinema;

import com.cinema.view.ViewManager;
import javafx.application.Application;
import javafx.stage.Stage;

public class MainApp extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        ViewManager.init(primaryStage);
        ViewManager.showLogin();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
