package com.cinema.controller;

import com.cinema.dao.FilmDAO;
import com.cinema.dao.SalleDAO;
import com.cinema.dao.SeanceDAO;
import com.cinema.dao.ReservationDAO;
import com.cinema.model.Film;
import com.cinema.model.Seance;
import com.cinema.model.Siege;
import com.cinema.util.AlertUtil;
import com.cinema.util.SessionManager;
import com.cinema.util.TicketPrinter;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class BrowseMoviesController {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final VBox view;
    private final FilmDAO    filmDAO   = new FilmDAO();
    private final SeanceDAO  seanceDAO = new SeanceDAO();
    private final SalleDAO   salleDAO  = new SalleDAO();
    private final ReservationDAO resDAO = new ReservationDAO();

    private final ObservableList<Film>   filmData   = FXCollections.observableArrayList();
    private final ObservableList<Seance> seanceData = FXCollections.observableArrayList();

    private ListView<Film>   filmList;
    private ListView<Seance> seanceList;
    private Label filmInfoLabel;

    public BrowseMoviesController() {
        view = new VBox(16);
        view.getStyleClass().add("content-root");
        view.setPadding(new Insets(0, 24, 24, 24));

        Label title = new Label("Films a l affiche");
        title.getStyleClass().add("page-title");
        Label sub = new Label("Choisissez un film, puis une seance pour reserver vos places.");
        sub.getStyleClass().add("page-subtitle");

        HBox content = new HBox(16);
        VBox.setVgrow(content, Priority.ALWAYS);

        // Left — Film list
        VBox filmPane = new VBox(10);
        filmPane.getStyleClass().add("card");
        filmPane.setPrefWidth(300);
        filmPane.setMinWidth(260);
        Label filmTitle = new Label("Films disponibles");
        filmTitle.getStyleClass().add("section-title");

        ComboBox<String> genreFilter = new ComboBox<>();
        genreFilter.setPromptText("Tous les genres");
        genreFilter.setMaxWidth(Double.MAX_VALUE);

        filmList = new ListView<>(filmData);
        filmList.setCellFactory(lv -> new ListCell<>() {
            @Override protected void updateItem(Film f, boolean empty) {
                super.updateItem(f, empty);
                if (empty || f == null) { setText(null); setGraphic(null); return; }
                VBox b = new VBox(2);
                Label t = new Label(f.getTitre());
                t.setStyle("-fx-font-weight:bold; -fx-font-size:13px;");
                Label info = new Label(f.getGenre() + " - " + f.getDuree() + " min - " + f.getClassification());
                info.setStyle("-fx-font-size:11px; -fx-text-fill:#888;");
                b.getChildren().addAll(t, info);
                setGraphic(b);
            }
        });
        VBox.setVgrow(filmList, Priority.ALWAYS);
        filmPane.getChildren().addAll(filmTitle, genreFilter, filmList);

        // Middle — Seances
        VBox seancePane = new VBox(10);
        seancePane.getStyleClass().add("card");
        seancePane.setPrefWidth(320);
        Label seanceTitle = new Label("Seances disponibles");
        seanceTitle.getStyleClass().add("section-title");

        filmInfoLabel = new Label("Selectionnez un film");
        filmInfoLabel.setStyle("-fx-font-size:12px; -fx-text-fill:#666; -fx-wrap-text:true;");
        filmInfoLabel.setWrapText(true);
        filmInfoLabel.setMaxWidth(300);

        seanceList = new ListView<>(seanceData);
        seanceList.setCellFactory(lv -> new ListCell<>() {
            @Override protected void updateItem(Seance s, boolean empty) {
                super.updateItem(s, empty);
                if (empty || s == null) { setText(null); setGraphic(null); return; }
                VBox b = new VBox(2);
                Label dt = new Label(s.getDateHeure() != null ? s.getDateHeure().format(FMT) : "");
                dt.setStyle("-fx-font-weight:bold;");
                Label info = new Label(s.getNumeroSalle() + " - " + s.getLangue() + " - " + s.getPrixBillet() + " DT");
                info.setStyle("-fx-font-size:11px; -fx-text-fill:#888;");
                b.getChildren().addAll(dt, info);
                setGraphic(b);
            }
        });
        VBox.setVgrow(seanceList, Priority.ALWAYS);

        Button reserveBtn = new Button("Choisir les places");
        reserveBtn.getStyleClass().add("btn-primary");
        reserveBtn.setMaxWidth(Double.MAX_VALUE);
        reserveBtn.setDisable(true);
        reserveBtn.setOnAction(e -> openSeatSelection());
        seanceList.getSelectionModel().selectedItemProperty().addListener((obs, o, n) -> reserveBtn.setDisable(n == null));

        seancePane.getChildren().addAll(seanceTitle, filmInfoLabel, seanceList, reserveBtn);
        HBox.setHgrow(seancePane, Priority.ALWAYS);

        content.getChildren().addAll(filmPane, seancePane);
        view.getChildren().addAll(title, sub, content);

        filmList.getSelectionModel().selectedItemProperty().addListener((obs, o, n) -> {
            if (n == null) return;
            filmInfoLabel.setText(n.getDescription() != null ? n.getDescription() : "Pas de description.");
            loadSeances(n.getId());
        });

        genreFilter.setOnAction(e -> {
            String g = genreFilter.getValue();
            loadFilms(g == null || g.isEmpty() ? null : g);
        });

        try {
            genreFilter.getItems().add(null);
            genreFilter.getItems().addAll(filmDAO.getGenres());
            genreFilter.setCellFactory(lv -> new ListCell<>() {
                @Override protected void updateItem(String s, boolean empty) {
                    super.updateItem(s, empty); setText(empty || s == null ? "Tous les genres" : s);
                }
            });
            genreFilter.setButtonCell(new ListCell<>() {
                @Override protected void updateItem(String s, boolean empty) {
                    super.updateItem(s, empty); setText(empty || s == null ? "Tous les genres" : s);
                }
            });
        } catch (Exception ignored) {}

        loadFilms(null);
    }

    private void loadFilms(String genre) {
        try { filmData.setAll(filmDAO.getAll(genre, true)); }
        catch (Exception e) { AlertUtil.showError("Erreur", e.getMessage()); }
    }

    private void loadSeances(int idFilm) {
        try {
            List<Seance> all = seanceDAO.getFutureSeances();
            seanceData.setAll(all.stream().filter(s -> s.getIdFilm() == idFilm).toList());
        } catch (Exception e) { AlertUtil.showError("Erreur", e.getMessage()); }
    }

    private void openSeatSelection() {
        Seance seance = seanceList.getSelectionModel().getSelectedItem();
        if (seance == null) return;

        List<Siege> sieges;
        try {
            sieges = salleDAO.getSiegesWithEtat(seance.getId());
        } catch (Exception ex) {
            AlertUtil.showError("Erreur", ex.getMessage());
            return;
        }

        // Track selected siege IDs
        List<Integer> selected = new ArrayList<>();

        Dialog<List<Integer>> dialog = new Dialog<>();
        dialog.setTitle("Choix des sieges — " + seance.getTitreFilm());
        dialog.setHeaderText(
            (seance.getDateHeure() != null ? seance.getDateHeure().format(FMT) : "") +
            " - " + seance.getNumeroSalle() +
            " - " + seance.getPrixBillet() + " DT/siege"
        );
        dialog.getDialogPane().setPrefWidth(750);
        dialog.getDialogPane().setPrefHeight(580);

        VBox content = new VBox(12);
        content.setPadding(new Insets(12));

        // Screen label
        Label screenLbl = new Label("============  ECRAN  ============");
        screenLbl.setMaxWidth(Double.MAX_VALUE);
        screenLbl.setAlignment(Pos.CENTER);
        screenLbl.setStyle("-fx-font-weight:bold; -fx-text-fill:#888;");

        // Legend
        HBox legend = new HBox(20);
        legend.setAlignment(Pos.CENTER);
        legend.getChildren().addAll(
            legendItem("Disponible",  "#4CAF50"),
            legendItem("Selectionne", "#2196F3"),
            legendItem("Reserve",     "#F44336"),
            legendItem("VIP",         "#FF9800")
        );

        // Total label
        Label totalLabel = new Label("Total : 0.00 DT");
        totalLabel.setStyle("-fx-font-size:16px; -fx-font-weight:bold; -fx-text-fill:#141450;");

        // Build seat grid
        GridPane seatGrid = new GridPane();
        seatGrid.setHgap(6);
        seatGrid.setVgap(6);
        seatGrid.setPadding(new Insets(12));
        seatGrid.setAlignment(Pos.CENTER);

        // Group sieges by row
        String currentRow = null;
        int col = 0;
        int row = 0;

        for (Siege sg : sieges) {
            if (!sg.getRangee().equals(currentRow)) {
                currentRow = sg.getRangee();
                row++;
                col = 0;
                Label rowLbl = new Label(currentRow);
                rowLbl.setStyle("-fx-font-weight:bold; -fx-text-fill:#555; -fx-min-width:20px;");
                seatGrid.add(rowLbl, col, row);
                col++;
            }

            boolean isVip      = "VIP".equals(sg.getTypeSiege());
            boolean isReserved = "RESERVE".equals(sg.getEtat()) || "OCCUPE".equals(sg.getEtat());

            Button btn = new Button(String.valueOf(sg.getNumeroSiege()));
            btn.setMinWidth(34);
            btn.setMinHeight(30);
            btn.setPrefWidth(34);
            btn.setPrefHeight(30);
            btn.setStyle(buildStyle(isReserved, false, isVip));
            btn.setDisable(isReserved);

            if (!isReserved) {
                final int siegeId = sg.getId();
                final boolean vip = isVip;
                btn.setOnAction(e -> {
                    if (selected.contains(siegeId)) {
                        selected.remove((Integer) siegeId);
                        btn.setStyle(buildStyle(false, false, vip));
                    } else {
                        selected.add(siegeId);
                        btn.setStyle(buildStyle(false, true, vip));
                    }
                    BigDecimal total = seance.getPrixBillet().multiply(BigDecimal.valueOf(selected.size()));
                    totalLabel.setText("Total : " + String.format("%.2f", total) + " DT  (" + selected.size() + " siege(s))");
                });
            }

            seatGrid.add(btn, col, row);
            col++;
        }

        ScrollPane scroll = new ScrollPane(seatGrid);
        scroll.setFitToWidth(true);
        scroll.setPrefHeight(350);

        content.getChildren().addAll(screenLbl, legend, scroll, totalLabel);
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dialog.setResultConverter(btn -> btn == ButtonType.OK ? selected : null);

        dialog.showAndWait().ifPresent(sel -> {
            if (sel == null || sel.isEmpty()) {
                AlertUtil.showError("Aucun siege selectionne", "Veuillez selectionner au moins un siege.");
                return;
            }
            confirmReservation(seance, sel);
        });
    }

    // Build inline style for seat buttons — avoids CSS class conflicts
    private String buildStyle(boolean reserved, boolean selected, boolean vip) {
        String bg;
        if (reserved)       bg = "#F44336";
        else if (selected)  bg = "#2196F3";
        else if (vip)       bg = "#FF9800";
        else                bg = "#4CAF50";

        String border = selected ? "2px solid #0d47a1" : "1px solid rgba(0,0,0,0.2)";
        String cursor  = reserved ? "default" : "hand";

        return "-fx-background-color:" + bg + ";" +
               "-fx-text-fill:white;" +
               "-fx-background-radius:5;" +
               "-fx-border-radius:5;" +
               "-fx-border-color:" + (selected ? "#0d47a1" : "transparent") + ";" +
               "-fx-border-width:" + border.split(" ")[0] + ";" +
               "-fx-cursor:" + cursor + ";" +
               "-fx-font-size:11px;";
    }

    private HBox legendItem(String text, String color) {
        javafx.scene.shape.Rectangle rect = new javafx.scene.shape.Rectangle(16, 16);
        rect.setFill(Color.web(color));
        rect.setArcWidth(4); rect.setArcHeight(4);
        Label l = new Label(text);
        l.setStyle("-fx-font-size:11px;");
        HBox b = new HBox(6, rect, l);
        b.setAlignment(Pos.CENTER_LEFT);
        return b;
    }

    private void confirmReservation(Seance seance, List<Integer> siegeIds) {
        BigDecimal total = seance.getPrixBillet().multiply(BigDecimal.valueOf(siegeIds.size()));
        boolean ok = AlertUtil.showConfirm("Confirmer la reservation",
            "Film : " + seance.getTitreFilm() + "\n" +
            "Date : " + (seance.getDateHeure() != null ? seance.getDateHeure().format(FMT) : "") + "\n" +
            "Sieges : " + siegeIds.size() + "\n" +
            "Total : " + String.format("%.2f", total) + " DT\n\nConfirmer ?");
        if (!ok) return;
        try {
            int idClient = SessionManager.getInstance().getCurrentUser().getIdClient();
            var res = resDAO.create(idClient, seance.getId(), siegeIds, total);
            AlertUtil.showInfo("Reservation confirmee", "Reference : " + res.getReference() + "\nMerci !");
            try {
                TicketPrinter.generateTicket(res);
            } catch (Exception ignored) {}
            String path = System.getProperty("user.home") + "/ticket_" + res.getReference() + ".pdf";
            AlertUtil.showInfo("Billet genere", "Votre billet a ete sauvegarde :\n" + path);
        } catch (Exception ex) {
            AlertUtil.showError("Erreur", ex.getMessage());
        }
    }

    public VBox getView() { return view; }
}
