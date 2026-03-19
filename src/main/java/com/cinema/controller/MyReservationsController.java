package com.cinema.controller;

import com.cinema.dao.ReservationDAO;
import com.cinema.model.Reservation;
import com.cinema.model.Siege;
import com.cinema.util.AlertUtil;
import com.cinema.util.SessionManager;
import com.cinema.util.TicketPrinter;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.stream.Collectors;

public class MyReservationsController {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final VBox view;
    private final ReservationDAO resDAO = new ReservationDAO();
    private final ObservableList<Reservation> data = FXCollections.observableArrayList();
    private TableView<Reservation> table;

    public MyReservationsController() {
        view = new VBox(16);
        view.getStyleClass().add("content-root");
        view.setPadding(new Insets(0, 24, 24, 24));

        Label title = new Label("Mes Reservations");
        title.getStyleClass().add("page-title");
        Label sub = new Label("Consultez vos reservations et telechargez vos billets.");
        sub.getStyleClass().add("page-subtitle");

        HBox filterBar = new HBox(12);
        filterBar.getStyleClass().add("filter-bar");
        filterBar.setAlignment(Pos.CENTER_LEFT);
        Button refreshBtn = new Button("Rafraichir");
        refreshBtn.getStyleClass().add("btn-secondary");
        refreshBtn.setOnAction(e -> loadData());
        filterBar.getChildren().add(refreshBtn);

        table = buildTable();
        view.getChildren().addAll(title, sub, filterBar, table);
        VBox.setVgrow(table, Priority.ALWAYS);
        loadData();
    }

    private void loadData() {
        try {
            int idClient = SessionManager.getInstance().getCurrentUser().getIdClient();
            data.setAll(resDAO.getByClient(idClient));
        } catch (Exception e) {
            AlertUtil.showError("Erreur", e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private TableView<Reservation> buildTable() {
        TableView<Reservation> t = new TableView<>(data);
        t.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<Reservation,String> cRef    = col("Reference",   r -> r.getReference());
        TableColumn<Reservation,String> cFilm   = col("Film",        r -> r.getTitreFilm());
        TableColumn<Reservation,String> cDate   = col("Seance",      r -> r.getDateSeance() != null ? r.getDateSeance().format(FMT) : "");
        TableColumn<Reservation,String> cSalle  = col("Salle",       r -> r.getNumeroSalle());
        TableColumn<Reservation,String> cSieges = col("Sieges",      r -> {
            if (r.getSieges() == null || r.getSieges().isEmpty()) return "-";
            return r.getSieges().stream().map(Siege::getLabel).collect(Collectors.joining(", "));
        });
        TableColumn<Reservation,String> cPrix   = col("Prix total",  r -> r.getPrixTotal() != null ? r.getPrixTotal().toPlainString() + " DT" : "");

        TableColumn<Reservation, String> cStatut = new TableColumn<>("Statut");
        cStatut.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getStatut()));
        cStatut.setCellFactory(tc -> new TableCell<>() {
            @Override protected void updateItem(String s, boolean empty) {
                super.updateItem(s, empty);
                if (empty || s == null) { setGraphic(null); setText(null); return; }
                Label badge = new Label(s);
                String style = switch (s) {
                    case "CONFIRMEE"  -> "-fx-background-color:#E8F5E9; -fx-text-fill:#2E7D32; -fx-background-radius:20; -fx-padding:2 10; -fx-font-weight:bold;";
                    case "EN_ATTENTE" -> "-fx-background-color:#FFF8E1; -fx-text-fill:#F57F17; -fx-background-radius:20; -fx-padding:2 10; -fx-font-weight:bold;";
                    default           -> "-fx-background-color:#FFEBEE; -fx-text-fill:#C62828; -fx-background-radius:20; -fx-padding:2 10; -fx-font-weight:bold;";
                };
                badge.setStyle(style);
                setGraphic(badge);
                setText(null);
            }
        });

        TableColumn<Reservation, Void> cActions = new TableColumn<>("Actions");
        cActions.setCellFactory(tc -> new TableCell<>() {
            final Button ticketBtn = new Button("Billet PDF");
            final Button cancelBtn = new Button("Annuler");
            {
                ticketBtn.getStyleClass().add("btn-secondary");
                cancelBtn.getStyleClass().add("btn-danger");
                ticketBtn.setOnAction(e -> printTicket(getTableView().getItems().get(getIndex())));
                cancelBtn.setOnAction(e  -> cancelReservation(getTableView().getItems().get(getIndex())));
            }
            @Override protected void updateItem(Void v, boolean empty) {
                super.updateItem(v, empty);
                if (empty) { setGraphic(null); return; }
                Reservation res = getTableView().getItems().get(getIndex());
                boolean canCancel = "CONFIRMEE".equals(res.getStatut()) || "EN_ATTENTE".equals(res.getStatut());
                boolean isFuture  = res.getDateSeance() != null && res.getDateSeance().isAfter(LocalDateTime.now().plusHours(2));
                cancelBtn.setDisable(!canCancel || !isFuture);
                HBox box = new HBox(6, ticketBtn, cancelBtn);
                box.setAlignment(Pos.CENTER);
                setGraphic(box);
            }
        });

        t.getColumns().addAll(cRef, cFilm, cDate, cSalle, cSieges, cPrix, cStatut, cActions);
        t.setPlaceholder(new Label("Aucune reservation trouvee."));
        return t;
    }

    private void printTicket(Reservation res) {
        try {
            TicketPrinter.generateTicket(res);
        } catch (Exception ignored) {}
        String path = System.getProperty("user.home") + "/ticket_" + res.getReference() + ".pdf";
        AlertUtil.showInfo("Billet genere", "Billet sauvegarde :\n" + path);
    }

    private void cancelReservation(Reservation res) {
        if (!AlertUtil.showConfirm("Annuler", "Annuler la reservation " + res.getReference() + " ?")) return;
        try {
            resDAO.cancel(res.getId());
            loadData();
            AlertUtil.showInfo("Annulee", "Reservation annulee avec succes.");
        } catch (Exception ex) {
            AlertUtil.showError("Erreur", ex.getMessage());
        }
    }

    private TableColumn<Reservation, String> col(String title, java.util.function.Function<Reservation, String> m) {
        TableColumn<Reservation, String> c = new TableColumn<>(title);
        c.setCellValueFactory(cd -> new SimpleStringProperty(m.apply(cd.getValue())));
        return c;
    }

    public VBox getView() { return view; }
}
