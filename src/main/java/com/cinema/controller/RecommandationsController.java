package com.cinema.controller;

import com.cinema.dao.RecommandationDAO;
import com.cinema.model.Film;
import com.cinema.util.AlertUtil;
import com.cinema.util.SessionManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.List;

public class RecommandationsController {

    private final VBox view;
    private final RecommandationDAO dao = new RecommandationDAO();

    public RecommandationsController() {
        view = new VBox(20);
        view.getStyleClass().add("content-root");
        view.setPadding(new Insets(0, 24, 24, 24));

        Label title = new Label("Recommandations pour vous");
        title.getStyleClass().add("page-title");
        Label sub = new Label("Films selectionnes selon vos preferences et votre historique.");
        sub.getStyleClass().add("page-subtitle");

        ScrollPane scroll = new ScrollPane();
        VBox inner = new VBox(24);
        inner.setPadding(new Insets(0, 4, 24, 4));
        scroll.setContent(inner);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background:transparent; -fx-background-color:transparent;");

        view.getChildren().addAll(title, sub, scroll);
        VBox.setVgrow(scroll, Priority.ALWAYS);

        loadRecommandations(inner);
    }

    private void loadRecommandations(VBox inner) {
        int idClient = SessionManager.getInstance().getCurrentUser().getIdClient();
        try {
            List<Integer> dejaVus    = dao.getFilmsVusIds(idClient);
            List<String>  genres     = dao.getGenresFavoris(idClient);
            List<Film>    parGenre   = dao.getRecommandationsParGenre(idClient, genres, dejaVus);
            List<Film>    populaires = dao.getFilmsPopulaires(dejaVus);
            List<Film>    nouveautes = dao.getNouveautes(dejaVus);

            if (dejaVus.isEmpty()) {
                Label noHist = new Label("Vous n avez pas encore de reservations. Reservez des films pour des recommandations personnalisees !");
                noHist.setStyle("-fx-text-fill:#888; -fx-font-size:13px; -fx-padding:20;");
                noHist.setWrapText(true);
                inner.getChildren().add(noHist);
            }

            if (!genres.isEmpty() && !parGenre.isEmpty()) {
                inner.getChildren().add(buildSection(
                        "Selon vos gouts  (" + String.join(", ", genres) + ")",
                        parGenre, "#141450"));
            }

            if (!populaires.isEmpty()) {
                inner.getChildren().add(buildSection(
                        "Les plus populaires en ce moment",
                        populaires, "#C81E1E"));
            }

            if (!nouveautes.isEmpty()) {
                inner.getChildren().add(buildSection(
                        "Nouveautes",
                        nouveautes, "#2196F3"));
            }

        } catch (Exception ex) {
            AlertUtil.showError("Erreur recommandations", ex.getMessage());
        }
    }

    private VBox buildSection(String sectionTitle, List<Film> films, String color) {
        VBox section = new VBox(10);

        Label titleLbl = new Label(sectionTitle);
        titleLbl.setStyle("-fx-font-size:16px; -fx-font-weight:bold; -fx-text-fill:" + color + ";");

        HBox filmRow = new HBox(14);
        filmRow.setAlignment(Pos.TOP_LEFT);
        for (Film f : films) {
            filmRow.getChildren().add(buildCard(f, color));
        }

        section.getChildren().addAll(titleLbl, filmRow);
        return section;
    }

    private VBox buildCard(Film film, String color) {
        // Outer card
        VBox card = new VBox();
        card.setPrefWidth(185);
        card.setMaxWidth(185);
        card.setMinWidth(185);
        card.setStyle(
                "-fx-background-color:white;" +
                        "-fx-background-radius:10;" +
                        "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.12),8,0,0,2);" +
                        "-fx-cursor:hand;"
        );

        // Colored top strip
        Label strip = new Label();
        strip.setPrefHeight(10);
        strip.setMaxWidth(Double.MAX_VALUE);
        strip.setStyle(
                "-fx-background-color:" + color + ";" +
                        "-fx-background-radius:10 10 0 0;"
        );

        // Content inside
        VBox content = new VBox(6);
        content.setPadding(new Insets(10, 12, 12, 12));

        // Genre badge
        Label genre = new Label(film.getGenre() != null ? film.getGenre() : "");
        genre.setStyle(
                "-fx-background-color:" + color + "33;" +
                        "-fx-text-fill:" + color + ";" +
                        "-fx-background-radius:20;" +
                        "-fx-padding:2 8;" +
                        "-fx-font-size:10px;" +
                        "-fx-font-weight:bold;"
        );

        // ---- FILM TITLE ----
        String titreText = film.getTitre() != null ? film.getTitre() : "Sans titre";
        Label filmName = new Label(titreText);
        filmName.setStyle(
                "-fx-font-size:13px;" +
                        "-fx-font-weight:bold;" +
                        "-fx-text-fill:#111111;" +
                        "-fx-wrap-text:true;"
        );
        filmName.setWrapText(true);
        filmName.setMaxWidth(161);
        filmName.setMinHeight(36);

        // Duration + classification
        Label info = new Label(film.getDuree() + " min  |  " + (film.getClassification() != null ? film.getClassification() : ""));
        info.setStyle("-fx-font-size:11px; -fx-text-fill:#888888;");

        // Description
        String descText = film.getDescription() != null && !film.getDescription().isEmpty()
                ? (film.getDescription().length() > 85 ? film.getDescription().substring(0, 85) + "..." : film.getDescription())
                : "";
        Label desc = new Label(descText);
        desc.setStyle("-fx-font-size:10px; -fx-text-fill:#666666; -fx-wrap-text:true;");
        desc.setWrapText(true);
        desc.setMaxWidth(161);

        content.getChildren().addAll(genre, filmName, info, desc);
        card.getChildren().addAll(strip, content);
        return card;
    }

    public VBox getView() { return view; }
}