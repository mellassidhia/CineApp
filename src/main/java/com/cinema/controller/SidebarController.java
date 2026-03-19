package com.cinema.controller;

import com.cinema.util.SessionManager;
import com.cinema.view.ViewManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public class SidebarController {

    private final VBox view;
    private Button activeBtn;

    public SidebarController() {
        view = new VBox();
        view.getStyleClass().add("sidebar");
        view.setPadding(new Insets(0, 10, 20, 10));

        Label title = new Label("CineApp");
        title.getStyleClass().add("sidebar-title");

        boolean isAdmin = SessionManager.getInstance().isAdmin();
        String user = SessionManager.getInstance().getCurrentUser().getUsername();
        String role = isAdmin ? "Administrateur" : "Client";
        Label subtitle = new Label(user + "\n" + role);
        subtitle.getStyleClass().add("sidebar-subtitle");

        view.getChildren().addAll(title, subtitle, sep());

        if (isAdmin) {
            Label adminLbl = sectionLabel("ADMINISTRATION");
            view.getChildren().add(adminLbl);

            Button filmsBtn   = navBtn("Films",        () -> { ViewManager.showFilms();        activate("Films"); });
            Button sallesBtn  = navBtn("Salles",       () -> { ViewManager.showSalles();       activate("Salles"); });
            Button seancesBtn = navBtn("Seances",      () -> { ViewManager.showSeances();      activate("Seances"); });
            Button clientsBtn = navBtn("Clients",      () -> { ViewManager.showClients();      activate("Clients"); });

            view.getChildren().addAll(filmsBtn, sallesBtn, seancesBtn, clientsBtn);
            view.getChildren().add(sep());

            Label statsLbl = sectionLabel("ANALYSES");
            Button statsBtn = navBtn("Statistiques",   () -> { ViewManager.showStatistiques(); activate("Statistiques"); });
            view.getChildren().addAll(statsLbl, statsBtn);

            setActive(filmsBtn);

        } else {
            Label userLbl = sectionLabel("MENU");
            view.getChildren().add(userLbl);

            Button browseBtn  = navBtn("Films a l affiche",  () -> { ViewManager.showBrowseMovies();    activate("Films a l affiche"); });
            Button recoBtn    = navBtn("Recommandations",    () -> { ViewManager.showRecommandations(); activate("Recommandations"); });
            Button resBtn     = navBtn("Mes reservations",   () -> { ViewManager.showMyReservations();  activate("Mes reservations"); });
            Button profileBtn = navBtn("Mon profil",         () -> { ViewManager.showProfile();         activate("Mon profil"); });

            view.getChildren().addAll(browseBtn, recoBtn, resBtn, profileBtn);
            setActive(browseBtn);
        }

        VBox spacer = new VBox();
        VBox.setVgrow(spacer, Priority.ALWAYS);
        view.getChildren().add(spacer);
        view.getChildren().add(sep());

        Button logoutBtn = new Button("Deconnexion");
        logoutBtn.getStyleClass().add("sidebar-logout");
        logoutBtn.setMaxWidth(Double.MAX_VALUE);
        logoutBtn.setOnAction(e -> {
            SessionManager.getInstance().logout();
            ViewManager.showLogin();
        });
        view.getChildren().add(logoutBtn);
    }

    private Button navBtn(String text, Runnable action) {
        Button btn = new Button(text);
        btn.getStyleClass().add("nav-btn");
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setOnAction(e -> action.run());
        return btn;
    }

    private void activate(String text) {
        for (Node n : view.getChildren()) {
            if (n instanceof Button b) {
                b.getStyleClass().remove("nav-btn-active");
                if (b.getText().equals(text)) setActive(b);
            }
        }
    }

    private void setActive(Button btn) {
        if (activeBtn != null) activeBtn.getStyleClass().remove("nav-btn-active");
        activeBtn = btn;
        if (btn != null) btn.getStyleClass().add("nav-btn-active");
    }

    private Label sectionLabel(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-font-size:10px; -fx-text-fill:#8888bb; -fx-padding:8 0 4 16; -fx-font-weight:bold;");
        return l;
    }

    private Separator sep() {
        Separator s = new Separator();
        s.setStyle("-fx-background-color:#2d2d60;");
        VBox.setMargin(s, new Insets(6, 0, 6, 0));
        return s;
    }

    public VBox getView() { return view; }
}
