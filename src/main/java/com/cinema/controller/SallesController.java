package com.cinema.controller;

import com.cinema.dao.SalleDAO;
import com.cinema.model.Salle;
import com.cinema.model.Siege;
import com.cinema.util.AlertUtil;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

public class SallesController {

    private final VBox view;
    private final SalleDAO dao = new SalleDAO();
    private final ObservableList<Salle> data = FXCollections.observableArrayList();
    private TableView<Salle> table;

    public SallesController() {
        view = new VBox(16);
        view.getStyleClass().add("content-root");
        view.setPadding(new Insets(0, 24, 24, 24));

        Label title = new Label("Gestion des Salles");
        title.getStyleClass().add("page-title");
        Label sub = new Label("Gérez les salles et leurs configurations de sièges.");
        sub.getStyleClass().add("page-subtitle");

        HBox filterBar = new HBox(12);
        filterBar.getStyleClass().add("filter-bar");
        filterBar.setAlignment(Pos.CENTER_LEFT);
        Button refreshBtn = new Button("🔄 Rafraîchir");
        refreshBtn.getStyleClass().add("btn-secondary");
        refreshBtn.setOnAction(e -> loadData());
        Button addBtn = new Button("＋ Nouvelle salle");
        addBtn.getStyleClass().add("btn-primary");
        addBtn.setOnAction(e -> openForm(null));
        Region spacer = new Region(); HBox.setHgrow(spacer, Priority.ALWAYS);
        filterBar.getChildren().addAll(refreshBtn, spacer, addBtn);

        table = buildTable();

        view.getChildren().addAll(title, sub, filterBar, table);
        VBox.setVgrow(table, Priority.ALWAYS);
        loadData();
    }

    private void loadData() {
        try { data.setAll(dao.getAll()); }
        catch (Exception e) { AlertUtil.showError("Erreur", e.getMessage()); }
    }

    @SuppressWarnings("unchecked")
    private TableView<Salle> buildTable() {
        TableView<Salle> t = new TableView<>(data);
        t.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<Salle,String> colNum  = col("Numéro",    s -> s.getNumero());
        TableColumn<Salle,String> colCap  = col("Capacité",  s -> String.valueOf(s.getCapacite()));
        TableColumn<Salle,String> colType = col("Type",      s -> s.getTypeSalle());
        TableColumn<Salle,String> colActif= col("Actif",     s -> s.isActif() ? "✅" : "❌");

        TableColumn<Salle, Void> colActions = new TableColumn<>("Actions");
        colActions.setCellFactory(tc -> new TableCell<>() {
            final Button editBtn   = new Button("✏️ Modifier");
            final Button delBtn    = new Button("🗑️ Supprimer");
            final Button siegesBtn = new Button("💺 Sièges");
            { editBtn.getStyleClass().add("btn-secondary");
              delBtn.getStyleClass().add("btn-danger");
              siegesBtn.getStyleClass().add("btn-secondary");
              editBtn.setOnAction(e   -> openForm(getTableView().getItems().get(getIndex())));
              delBtn.setOnAction(e    -> deleteSalle(getTableView().getItems().get(getIndex())));
              siegesBtn.setOnAction(e -> openSieges(getTableView().getItems().get(getIndex())));
            }
            @Override protected void updateItem(Void v, boolean empty) {
                super.updateItem(v, empty);
                if (empty) { setGraphic(null); return; }
                HBox box = new HBox(6, editBtn, siegesBtn, delBtn);
                box.setAlignment(Pos.CENTER);
                setGraphic(box);
            }
        });

        t.getColumns().addAll(colNum, colCap, colType, colActif, colActions);
        t.setPlaceholder(new Label("Aucune salle trouvée."));
        return t;
    }

    private void openForm(Salle salle) {
        boolean isEdit = salle != null;
        Dialog<Salle> dialog = new Dialog<>();
        dialog.setTitle(isEdit ? "Modifier la salle" : "Nouvelle salle");

        GridPane grid = new GridPane();
        grid.setHgap(12); grid.setVgap(10);
        grid.setPadding(new Insets(20));

        TextField numF  = new TextField(isEdit ? salle.getNumero() : "");
        TextField capF  = new TextField(isEdit ? String.valueOf(salle.getCapacite()) : "");
        ComboBox<String> typeF = new ComboBox<>();
        typeF.getItems().addAll("2D","3D","IMAX","VIP","4DX");
        typeF.setValue(isEdit ? salle.getTypeSalle() : "2D");

        addRow(grid, 0, "Numéro *", numF);
        addRow(grid, 1, "Capacité *", capF);
        addRow(grid, 2, "Type *", typeF);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.setResultConverter(btn -> {
            if (btn != ButtonType.OK) return null;
            Salle s = isEdit ? salle : new Salle();
            s.setNumero(numF.getText().trim());
            s.setTypeSalle(typeF.getValue());
            s.setActif(true);
            try { s.setCapacite(Integer.parseInt(capF.getText().trim())); }
            catch (NumberFormatException ex) { s.setCapacite(0); }
            return s;
        });

        dialog.showAndWait().ifPresent(s -> {
            if (s.getNumero().isEmpty() || s.getCapacite() <= 0) {
                AlertUtil.showError("Validation", "Numéro et capacité sont obligatoires.");
                return;
            }
            try {
                if (isEdit) dao.update(s); else dao.insert(s);
                loadData();
                AlertUtil.showInfo("Succès", isEdit ? "Salle modifiée." : "Salle ajoutée.");
            } catch (Exception ex) {
                AlertUtil.showError("Erreur", ex.getMessage());
            }
        });
    }

    private void deleteSalle(Salle s) {
        if (!AlertUtil.showConfirm("Supprimer", "Supprimer la salle \"" + s.getNumero() + "\" ?")) return;
        try {
            if (!dao.delete(s.getId())) {
                AlertUtil.showError("Impossible", "Des séances futures sont associées à cette salle.");
                return;
            }
            loadData();
            AlertUtil.showInfo("Succès", "Salle supprimée.");
        } catch (Exception ex) {
            AlertUtil.showError("Erreur", ex.getMessage());
        }
    }

    private void openSieges(Salle salle) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Sièges — " + salle.getNumero());
        dialog.setHeaderText("Configuration des sièges");
        dialog.getDialogPane().setPrefWidth(600);

        VBox content = new VBox(12);
        content.setPadding(new Insets(16));

        ObservableList<Siege> siegeData = FXCollections.observableArrayList();
        TableView<Siege> siegeTable = new TableView<>(siegeData);
        siegeTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        siegeTable.setPrefHeight(300);

        TableColumn<Siege,String> cRangee = new TableColumn<>("Rangée");
        cRangee.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getRangee()));
        TableColumn<Siege,String> cNum    = new TableColumn<>("Numéro");
        cNum.setCellValueFactory(c -> new SimpleStringProperty(String.valueOf(c.getValue().getNumeroSiege())));
        TableColumn<Siege,String> cType   = new TableColumn<>("Type");
        cType.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getTypeSiege()));

        TableColumn<Siege, Void> cDel = new TableColumn<>("Actions");
        cDel.setCellFactory(tc -> new TableCell<>() {
            final Button delBtn = new Button("🗑️");
            { delBtn.getStyleClass().add("btn-danger");
              delBtn.setOnAction(e -> {
                  Siege sg = getTableView().getItems().get(getIndex());
                  try { dao.deleteSiege(sg.getId()); siegeData.remove(sg); }
                  catch (Exception ex) { AlertUtil.showError("Erreur", ex.getMessage()); }
              });
            }
            @Override protected void updateItem(Void v, boolean empty) {
                super.updateItem(v, empty); setGraphic(empty ? null : delBtn);
            }
        });

        siegeTable.getColumns().addAll(cRangee, cNum, cType);

        // Add siege form
        HBox addRow = new HBox(8);
        addRow.setAlignment(Pos.CENTER_LEFT);
        TextField rangeeF = new TextField(); rangeeF.setPromptText("Rangée (A)"); rangeeF.setPrefWidth(80);
        TextField numF    = new TextField(); numF.setPromptText("Numéro"); numF.setPrefWidth(80);
        ComboBox<String> typeF = new ComboBox<>();
        typeF.getItems().addAll("STANDARD","VIP","PMR"); typeF.setValue("STANDARD");
        Button addSiegeBtn = new Button("＋ Ajouter");
        addSiegeBtn.getStyleClass().add("btn-primary");
        addSiegeBtn.setOnAction(e -> {
            try {
                Siege sg = new Siege();
                sg.setIdSalle(salle.getId());
                sg.setRangee(rangeeF.getText().trim().toUpperCase());
                sg.setNumeroSiege(Integer.parseInt(numF.getText().trim()));
                sg.setTypeSiege(typeF.getValue());
                dao.addSiege(sg);
                siegeData.add(sg);
                rangeeF.clear(); numF.clear();
            } catch (Exception ex) {
                AlertUtil.showError("Erreur", ex.getMessage());
            }
        });
        addRow.getChildren().addAll(new Label("Rangée:"), rangeeF, new Label("N°:"), numF, new Label("Type:"), typeF, addSiegeBtn);

        try { siegeData.setAll(dao.getSiegesBySalle(salle.getId())); }
        catch (Exception ex) { AlertUtil.showError("Erreur", ex.getMessage()); }

        content.getChildren().addAll(siegeTable, new Separator(), addRow);
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.showAndWait();
    }

    private <T> TableColumn<Salle, String> col(String title, java.util.function.Function<Salle, String> m) {
        TableColumn<Salle, String> c = new TableColumn<>(title);
        c.setCellValueFactory(cd -> new SimpleStringProperty(m.apply(cd.getValue())));
        return c;
    }

    private void addRow(GridPane g, int row, String lbl, javafx.scene.Node field) {
        Label l = new Label(lbl); l.getStyleClass().add("form-label");
        g.add(l, 0, row); g.add(field, 1, row);
        GridPane.setHgrow(field, Priority.ALWAYS);
        if (field instanceof TextField tf) tf.setPrefWidth(250);
    }

    public VBox getView() { return view; }
}
