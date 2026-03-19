package com.cinema.controller;

import com.cinema.dao.FilmDAO;
import com.cinema.dao.SalleDAO;
import com.cinema.dao.SeanceDAO;
import com.cinema.model.Film;
import com.cinema.model.Salle;
import com.cinema.model.Seance;
import com.cinema.util.AlertUtil;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class SeancesController {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final VBox view;
    private final SeanceDAO seanceDAO = new SeanceDAO();
    private final FilmDAO   filmDAO   = new FilmDAO();
    private final SalleDAO  salleDAO  = new SalleDAO();
    private final ObservableList<Seance> data = FXCollections.observableArrayList();
    private TableView<Seance> table;
    private ComboBox<Film> filmFilter;

    public SeancesController() {
        view = new VBox(16);
        view.getStyleClass().add("content-root");
        view.setPadding(new Insets(0, 24, 24, 24));

        Label title = new Label("Gestion des Séances");
        title.getStyleClass().add("page-title");
        Label sub = new Label("Planifiez et gérez les projections.");
        sub.getStyleClass().add("page-subtitle");

        HBox filterBar = new HBox(12);
        filterBar.getStyleClass().add("filter-bar");
        filterBar.setAlignment(Pos.CENTER_LEFT);
        filmFilter = new ComboBox<>();
        filmFilter.setPromptText("Tous les films");
        filmFilter.setOnAction(e -> loadData());
        filmFilter.setPrefWidth(200);
        Button refreshBtn = new Button("🔄 Rafraîchir");
        refreshBtn.getStyleClass().add("btn-secondary");
        refreshBtn.setOnAction(e -> loadData());
        Button addBtn = new Button("＋ Planifier une séance");
        addBtn.getStyleClass().add("btn-primary");
        addBtn.setOnAction(e -> openForm(null));
        Region spacer = new Region(); HBox.setHgrow(spacer, Priority.ALWAYS);
        filterBar.getChildren().addAll(new Label("Film :"), filmFilter, refreshBtn, spacer, addBtn);

        table = buildTable();
        view.getChildren().addAll(title, sub, filterBar, table);
        VBox.setVgrow(table, Priority.ALWAYS);

        loadFilmFilter();
        loadData();
    }

    private void loadFilmFilter() {
        try {
            List<Film> films = filmDAO.getAll(null, true);
            filmFilter.getItems().clear();
            filmFilter.getItems().add(null);
            filmFilter.getItems().addAll(films);
            filmFilter.setCellFactory(lv -> new ListCell<>() {
                @Override protected void updateItem(Film f, boolean empty) {
                    super.updateItem(f, empty);
                    setText(empty || f == null ? "Tous les films" : f.getTitre());
                }
            });
            filmFilter.setButtonCell(new ListCell<>() {
                @Override protected void updateItem(Film f, boolean empty) {
                    super.updateItem(f, empty);
                    setText(empty || f == null ? "Tous les films" : f.getTitre());
                }
            });
        } catch (Exception ignored) {}
    }

    private void loadData() {
        try {
            Film f = filmFilter.getValue();
            data.setAll(seanceDAO.getAll(f != null ? f.getId() : null, null));
        } catch (Exception e) {
            AlertUtil.showError("Erreur", e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private TableView<Seance> buildTable() {
        TableView<Seance> t = new TableView<>(data);
        t.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<Seance,String> cFilm    = col("Film",       s -> s.getTitreFilm());
        TableColumn<Seance,String> cSalle   = col("Salle",      s -> s.getNumeroSalle());
        TableColumn<Seance,String> cDate    = col("Date/Heure", s -> s.getDateHeure() != null ? s.getDateHeure().format(FMT) : "");
        TableColumn<Seance,String> cPrix    = col("Prix (DT)",  s -> s.getPrixBillet() != null ? s.getPrixBillet().toPlainString() : "");
        TableColumn<Seance,String> cLangue  = col("Langue",     s -> s.getLangue());
        TableColumn<Seance,String> cStatut  = col("Statut",     s -> s.getStatut());

        TableColumn<Seance, Void> cActions = new TableColumn<>("Actions");
        cActions.setCellFactory(tc -> new TableCell<>() {
            final Button editBtn = new Button("✏️ Modifier");
            final Button delBtn  = new Button("🗑️ Supprimer");
            { editBtn.getStyleClass().add("btn-secondary");
              delBtn.getStyleClass().add("btn-danger");
              editBtn.setOnAction(e -> openForm(getTableView().getItems().get(getIndex())));
              delBtn.setOnAction(e  -> deleteSeance(getTableView().getItems().get(getIndex())));
            }
            @Override protected void updateItem(Void v, boolean empty) {
                super.updateItem(v, empty);
                if (empty) { setGraphic(null); return; }
                HBox box = new HBox(6, editBtn, delBtn);
                box.setAlignment(Pos.CENTER);
                setGraphic(box);
            }
        });

        t.getColumns().addAll(cFilm, cSalle, cDate, cPrix, cLangue, cStatut, cActions);
        t.setPlaceholder(new Label("Aucune séance trouvée."));
        return t;
    }

    private void openForm(Seance seance) {
        boolean isEdit = seance != null;
        Dialog<Seance> dialog = new Dialog<>();
        dialog.setTitle(isEdit ? "Modifier la séance" : "Planifier une séance");

        GridPane grid = new GridPane();
        grid.setHgap(12); grid.setVgap(10);
        grid.setPadding(new Insets(20));
        grid.setPrefWidth(500);

        ComboBox<Film> filmCB = new ComboBox<>();
        ComboBox<Salle> salleCB = new ComboBox<>();
        DatePicker datePicker = new DatePicker();
        TextField heureF    = new TextField(isEdit && seance.getDateHeure() != null ? seance.getDateHeure().format(DateTimeFormatter.ofPattern("HH:mm")) : "20:00");
        TextField prixF     = new TextField(isEdit && seance.getPrixBillet() != null ? seance.getPrixBillet().toPlainString() : "");
        ComboBox<String> langCB = new ComboBox<>();
        langCB.getItems().addAll("VF","VO","VOSTFR");
        langCB.setValue(isEdit ? seance.getLangue() : "VF");
        ComboBox<String> statutCB = new ComboBox<>();
        statutCB.getItems().addAll("PLANIFIEE","EN_COURS","TERMINEE","ANNULEE");
        statutCB.setValue(isEdit ? seance.getStatut() : "PLANIFIEE");

        try {
            filmCB.getItems().setAll(filmDAO.getAll(null, true));
            salleCB.getItems().setAll(salleDAO.getAll());
            if (isEdit) {
                filmCB.getItems().stream().filter(f -> f.getId() == seance.getIdFilm()).findFirst().ifPresent(filmCB::setValue);
                salleCB.getItems().stream().filter(s -> s.getId() == seance.getIdSalle()).findFirst().ifPresent(salleCB::setValue);
                datePicker.setValue(seance.getDateHeure().toLocalDate());
            }
        } catch (Exception ex) { AlertUtil.showError("Erreur", ex.getMessage()); }

        addRow(grid, 0, "Film *", filmCB);
        addRow(grid, 1, "Salle *", salleCB);
        addRow(grid, 2, "Date *", datePicker);
        addRow(grid, 3, "Heure (HH:mm) *", heureF);
        addRow(grid, 4, "Prix (DT) *", prixF);
        addRow(grid, 5, "Langue", langCB);
        addRow(grid, 6, "Statut", statutCB);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.setResultConverter(btn -> {
            if (btn != ButtonType.OK) return null;
            if (filmCB.getValue() == null || salleCB.getValue() == null || datePicker.getValue() == null) return null;
            Seance s = isEdit ? seance : new Seance();
            s.setIdFilm(filmCB.getValue().getId());
            s.setIdSalle(salleCB.getValue().getId());
            s.setLangue(langCB.getValue());
            s.setStatut(statutCB.getValue());
            try {
                String[] parts = heureF.getText().split(":");
                s.setDateHeure(datePicker.getValue().atTime(Integer.parseInt(parts[0]), Integer.parseInt(parts[1])));
                s.setPrixBillet(new BigDecimal(prixF.getText().trim()));
            } catch (Exception ex) { return null; }
            return s;
        });

        dialog.showAndWait().ifPresent(s -> {
            if (s == null) { AlertUtil.showError("Validation", "Données invalides."); return; }
            try {
                int duree = filmDAO.getById(s.getIdFilm()).getDuree();
                if (seanceDAO.hasConflict(s.getIdSalle(), s.getDateHeure(), duree, isEdit ? s.getId() : 0)) {
                    AlertUtil.showError("Conflit horaire", "Une autre séance occupe déjà cette salle à cet horaire.");
                    return;
                }
                if (isEdit) {
                    if (!seanceDAO.update(s)) {
                        AlertUtil.showError("Impossible", "Des réservations confirmées existent pour cette séance.");
                        return;
                    }
                } else {
                    seanceDAO.insert(s);
                }
                loadData();
                AlertUtil.showInfo("Succès", isEdit ? "Séance modifiée." : "Séance planifiée.");
            } catch (Exception ex) {
                AlertUtil.showError("Erreur", ex.getMessage());
            }
        });
    }

    private void deleteSeance(Seance s) {
        if (!AlertUtil.showConfirm("Supprimer", "Supprimer cette séance ?")) return;
        try {
            if (!seanceDAO.delete(s.getId())) {
                AlertUtil.showError("Impossible", "Des réservations confirmées existent pour cette séance.");
                return;
            }
            loadData();
            AlertUtil.showInfo("Succès", "Séance supprimée.");
        } catch (Exception ex) {
            AlertUtil.showError("Erreur", ex.getMessage());
        }
    }

    private <T> TableColumn<Seance, String> col(String title, java.util.function.Function<Seance, String> m) {
        TableColumn<Seance, String> c = new TableColumn<>(title);
        c.setCellValueFactory(cd -> new SimpleStringProperty(m.apply(cd.getValue())));
        return c;
    }

    private void addRow(GridPane g, int row, String lbl, javafx.scene.Node field) {
        Label l = new Label(lbl); l.getStyleClass().add("form-label");
        g.add(l, 0, row); g.add(field, 1, row);
        GridPane.setHgrow(field, Priority.ALWAYS);
        if (field instanceof TextField tf) tf.setPrefWidth(250);
        if (field instanceof ComboBox cb) cb.setMaxWidth(Double.MAX_VALUE);
        if (field instanceof DatePicker dp) dp.setMaxWidth(Double.MAX_VALUE);
    }

    public VBox getView() { return view; }
}
