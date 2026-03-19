package com.cinema.view;

import com.cinema.controller.*;
import com.cinema.util.SessionManager;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

public class ViewManager {

    private static Stage stage;
    private static BorderPane mainLayout;

    public static void init(Stage s) {
        stage = s;
        stage.setTitle("Cinema App");
        stage.setMinWidth(1100);
        stage.setMinHeight(700);
    }

    public static void showLogin() {
        LoginController ctrl = new LoginController();
        Scene scene = new Scene(ctrl.getView(), 900, 600);
        scene.getStylesheets().add(ViewManager.class.getResource("/com/cinema/css/style.css").toExternalForm());
        stage.setScene(scene);
        stage.centerOnScreen();
        stage.show();
    }

    public static void showRegister() {
        RegisterController ctrl = new RegisterController();
        Scene scene = new Scene(ctrl.getView(), 900, 680);
        scene.getStylesheets().add(ViewManager.class.getResource("/com/cinema/css/style.css").toExternalForm());
        stage.setScene(scene);
    }

    public static void showMain() {
        mainLayout = new BorderPane();
        SidebarController sidebar = new SidebarController();
        mainLayout.setLeft(sidebar.getView());

        if (SessionManager.getInstance().isAdmin()) {
            showFilms();
        } else {
            showBrowseMovies();
        }

        Scene scene = new Scene(mainLayout, 1200, 750);
        scene.getStylesheets().add(ViewManager.class.getResource("/com/cinema/css/style.css").toExternalForm());
        stage.setScene(scene);
        stage.centerOnScreen();
    }

    public static void showFilms()            { setCenter(new FilmsController().getView()); }
    public static void showSalles()           { setCenter(new SallesController().getView()); }
    public static void showSeances()          { setCenter(new SeancesController().getView()); }
    public static void showClients()          { setCenter(new ClientsController().getView()); }
    public static void showStatistiques()     { setCenter(new StatistiquesController().getView()); }
    public static void showBrowseMovies()     { setCenter(new BrowseMoviesController().getView()); }
    public static void showMyReservations()   { setCenter(new MyReservationsController().getView()); }
    public static void showRecommandations()  { setCenter(new RecommandationsController().getView()); }
    public static void showProfile()          { setCenter(new ProfileController().getView()); }

    private static void setCenter(javafx.scene.Node node) {
        if (mainLayout != null) mainLayout.setCenter(node);
    }

    public static Stage getStage() { return stage; }
}
