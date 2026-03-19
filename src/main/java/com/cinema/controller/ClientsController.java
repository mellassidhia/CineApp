package com.cinema.controller;

import com.cinema.dao.ClientDAO;
import com.cinema.dao.ReservationDAO;
import com.cinema.model.Client;
import com.cinema.model.Reservation;
import com.cinema.util.AlertUtil;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.time.format.DateTimeFormatter;
import java.util.List;

public class ClientsController {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final VBox view;
    private final ClientDAO clientDAO = new ClientDAO();
    private final ReservationDAO resDAO = new ReservationDAO();
    private final ObservableList<Client> data = FXCollections.observableArrayList();
    private TableView<Client> table;
    private TextField searchField;

    public ClientsController() {
        view = new VBox(16);
        view.getStyleClass().add("content-root");
        view.setPadding(new Insets(0, 24, 24, 24));

        Label title = new Label("Gestion des Clients");
        title.getStyleClass().add("page-title");
        Label sub = new Label("Consultez et gérez les comptes clients.");
        sub.getStyleClass().add("page-subtitle");

        HBox filterBar = new HBox(12);
        filterBar.getStyleClass().add("filter-bar");
        filterBar.setAlignment(Pos.CENTER_LEFT);
        searchField = new TextField();
        searchField.setPromptText("🔍 Rechercher par nom ou email...");
        searchField.setPrefWidth(280);
        searchField.setOnKeyReleased(e -> filterData());
        Button refreshBtn = new Button("🔄 Rafraîchir");
        refreshBtn.getStyleClass().add("btn-secondary");
        refreshBtn.setOnAction(e -> loadData());
        Region spacer = new Region(); HBox.setHgrow(spacer, Priority.ALWAYS);
        filterBar.getChildren().addAll(searchField, refreshBtn, spacer);

        table = buildTable();
        view.getChildren().addAll(title, sub, filterBar, table);
        VBox.setVgrow(table, Priority.ALWAYS);
        loadData();
    }

    private List<Client> allClients;

    private void loadData() {
        try {
            allClients = clientDAO.getAll();
            data.setAll(allClients);
            searchField.clear();
        } catch (Exception e) {
            AlertUtil.showError("Erreur", e.getMessage());
        }
    }

    private void filterData() {
        if (allClients == null) return;
        String q = searchField.getText().toLowerCase();
        if (q.isEmpty()) { data.setAll(allClients); return; }
        data.setAll(allClients.stream()
            .filter(c -> c.getNomComplet().toLowerCase().contains(q)
                      || c.getEmail().toLowerCase().contains(q))
            .toList());
    }

    @SuppressWarnings("unchecked")
    private TableView<Client> buildTable() {
        TableView<Client> t = new TableView<>(data);
        t.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<Client,String> cNom   = col("Nom complet", c -> c.getNomComplet());
        TableColumn<Client,String> cEmail = col("Email",       c -> c.getEmail());
        TableColumn<Client,String> cTel   = col("Téléphone",   c -> c.getTelephone() != null ? c.getTelephone() : "");
        TableColumn<Client,String> cActif = col("Actif",       c -> c.isActif() ? "✅" : "❌");

        TableColumn<Client, Void> cActions = new TableColumn<>("Actions");
        cActions.setCellFactory(tc -> new TableCell<>() {
            final Button histBtn = new Button("📋 Historique");
            final Button editBtn = new Button("✏️ Modifier");
            { histBtn.getStyleClass().add("btn-secondary");
              editBtn.getStyleClass().add("btn-secondary");
              histBtn.setOnAction(e -> showHistory(getTableView().getItems().get(getIndex())));
              editBtn.setOnAction(e -> openForm(getTableView().getItems().get(getIndex())));
            }
            @Override protected void updateItem(Void v, boolean empty) {
                super.updateItem(v, empty);
                if (empty) { setGraphic(null); return; }
                HBox box = new HBox(6, editBtn, histBtn);
                box.setAlignment(Pos.CENTER);
                setGraphic(box);
            }
        });

        t.getColumns().addAll(cNom, cEmail, cTel, cActif, cActions);
        t.setPlaceholder(new Label("Aucun client trouvé."));
        return t;
    }

    private void openForm(Client client) {
        Dialog<Client> dialog = new Dialog<>();
        dialog.setTitle("Modifier le client");

        GridPane grid = new GridPane();
        grid.setHgap(12); grid.setVgap(10);
        grid.setPadding(new Insets(20));

        TextField nomF    = new TextField(client.getNom());
        TextField prenomF = new TextField(client.getPrenom());
        TextField emailF  = new TextField(client.getEmail());
        TextField telF    = new TextField(client.getTelephone() != null ? client.getTelephone() : "");

        addRow(grid, 0, "Nom *", nomF);
        addRow(grid, 1, "Prénom *", prenomF);
        addRow(grid, 2, "Email *", emailF);
        addRow(grid, 3, "Téléphone", telF);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.setResultConverter(btn -> {
            if (btn != ButtonType.OK) return null;
            client.setNom(nomF.getText().trim());
            client.setPrenom(prenomF.getText().trim());
            client.setEmail(emailF.getText().trim());
            client.setTelephone(telF.getText().trim());
            return client;
        });

        dialog.showAndWait().ifPresent(c -> {
            try {
                clientDAO.update(c);
                loadData();
                AlertUtil.showInfo("Succès", "Client mis à jour.");
            } catch (Exception ex) {
                AlertUtil.showError("Erreur", ex.getMessage());
            }
        });
    }

    private void showHistory(Client client) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Historique — " + client.getNomComplet());
        dialog.getDialogPane().setPrefWidth(700);
        dialog.getDialogPane().setPrefHeight(450);

        ObservableList<Reservation> resData = FXCollections.observableArrayList();
        TableView<Reservation> resTable = new TableView<>(resData);
        resTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<Reservation,String> cRef    = colR("Référence",  r -> r.getReference());
        TableColumn<Reservation,String> cFilm   = colR("Film",       r -> r.getTitreFilm());
        TableColumn<Reservation,String> cDate   = colR("Date séance",r -> r.getDateSeance() != null ? r.getDateSeance().format(FMT) : "");
        TableColumn<Reservation,String> cSalle  = colR("Salle",      r -> r.getNumeroSalle());
        TableColumn<Reservation,String> cPrix   = colR("Prix total", r -> r.getPrixTotal() != null ? r.getPrixTotal().toPlainString() + " DT" : "");
        TableColumn<Reservation,String> cStatut = colR("Statut",     r -> r.getStatut());
        resTable.getColumns().addAll(cRef, cFilm, cDate, cSalle, cPrix, cStatut);

        try {
            resData.setAll(resDAO.getByClient(client.getId()));
        } catch (Exception ex) {
            AlertUtil.showError("Erreur", ex.getMessage());
        }

        dialog.getDialogPane().setContent(new VBox(resTable));
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.showAndWait();
    }

    private <T> TableColumn<Client, String> col(String title, java.util.function.Function<Client, String> m) {
        TableColumn<Client, String> c = new TableColumn<>(title);
        c.setCellValueFactory(cd -> new SimpleStringProperty(m.apply(cd.getValue())));
        return c;
    }

    private TableColumn<Reservation, String> colR(String title, java.util.function.Function<Reservation, String> m) {
        TableColumn<Reservation, String> c = new TableColumn<>(title);
        c.setCellValueFactory(cd -> new SimpleStringProperty(m.apply(cd.getValue())));
        return c;
    }

    private void addRow(GridPane g, int row, String lbl, javafx.scene.Node field) {
        Label l = new Label(lbl); l.getStyleClass().add("form-label");
        g.add(l, 0, row); g.add(field, 1, row);
        GridPane.setHgrow(field, Priority.ALWAYS);
        if (field instanceof TextField tf) tf.setPrefWidth(260);
    }

    public VBox getView() { return view; }
}
