package com.cinema.controller;

import com.cinema.dao.FilmDAO;
import com.cinema.model.Film;
import com.cinema.util.AlertUtil;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.time.LocalDate;
import java.util.List;

public class FilmsController {

    private final VBox view;
    private final FilmDAO dao = new FilmDAO();
    private final ObservableList<Film> data = FXCollections.observableArrayList();
    private TableView<Film> table;
    private ComboBox<String> genreFilter;

    public FilmsController() {
        view = new VBox(16);
        view.getStyleClass().add("content-root");
        view.setPadding(new Insets(0, 24, 24, 24));

        // Header
        Label title = new Label("Gestion des Films");
        title.getStyleClass().add("page-title");
        Label sub = new Label("Ajoutez, modifiez ou supprimez les films du catalogue.");
        sub.getStyleClass().add("page-subtitle");

        // Filter bar
        HBox filterBar = new HBox(12);
        filterBar.getStyleClass().add("filter-bar");
        filterBar.setAlignment(Pos.CENTER_LEFT);
        genreFilter = new ComboBox<>();
        genreFilter.getItems().add("Tous les genres");
        genreFilter.setValue("Tous les genres");
        genreFilter.setOnAction(e -> loadData());
        Button refreshBtn = new Button("🔄 Rafraîchir");
        refreshBtn.getStyleClass().add("btn-secondary");
        refreshBtn.setOnAction(e -> loadData());
        Button addBtn = new Button("＋ Nouveau film");
        addBtn.getStyleClass().add("btn-primary");
        addBtn.setOnAction(e -> openForm(null));
        Region spacer = new Region(); HBox.setHgrow(spacer, Priority.ALWAYS);
        filterBar.getChildren().addAll(new Label("Genre :"), genreFilter, refreshBtn, spacer, addBtn);

        // Table
        table = buildTable();

        view.getChildren().addAll(title, sub, filterBar, table);
        VBox.setVgrow(table, Priority.ALWAYS);

        loadGenres();
        loadData();
    }

    private void loadGenres() {
        try {
            List<String> genres = dao.getGenres();
            genreFilter.getItems().setAll("Tous les genres");
            genreFilter.getItems().addAll(genres);
        } catch (Exception ignored) {}
    }

    private void loadData() {
        try {
            String g = genreFilter.getValue();
            String genre = (g == null || g.equals("Tous les genres")) ? null : g;
            data.setAll(dao.getAll(genre, null));
        } catch (Exception e) {
            AlertUtil.showError("Erreur", e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private TableView<Film> buildTable() {
        TableView<Film> t = new TableView<>(data);
        t.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<Film, String> colTitre  = col("Titre",          f -> f.getTitre());
        TableColumn<Film, String> colDuree  = col("Durée (min)",    f -> String.valueOf(f.getDuree()));
        TableColumn<Film, String> colGenre  = col("Genre",          f -> f.getGenre());
        TableColumn<Film, String> colDate   = col("Date sortie",    f -> f.getDateSortie() != null ? f.getDateSortie().toString() : "");
        TableColumn<Film, String> colClass  = col("Classification", f -> f.getClassification());
        TableColumn<Film, String> colActif  = col("Actif",          f -> f.isActif() ? "✅" : "❌");

        colTitre.setPrefWidth(200);

        TableColumn<Film, Void> colActions = new TableColumn<>("Actions");
        colActions.setCellFactory(tc -> new TableCell<>() {
            final Button editBtn = new Button("✏️ Modifier");
            final Button delBtn  = new Button("🗑️ Supprimer");
            { editBtn.getStyleClass().add("btn-secondary");
              delBtn.getStyleClass().add("btn-danger");
              editBtn.setOnAction(e -> openForm(getTableView().getItems().get(getIndex())));
              delBtn.setOnAction(e  -> deleteFilm(getTableView().getItems().get(getIndex())));
            }
            @Override protected void updateItem(Void v, boolean empty) {
                super.updateItem(v, empty);
                if (empty) { setGraphic(null); return; }
                HBox box = new HBox(6, editBtn, delBtn);
                box.setAlignment(Pos.CENTER);
                setGraphic(box);
            }
        });

        t.getColumns().addAll(colTitre, colDuree, colGenre, colDate, colClass, colActif, colActions);
        t.setPlaceholder(new Label("Aucun film trouvé."));
        return t;
    }

    private <T> TableColumn<Film, String> col(String title, java.util.function.Function<Film, String> mapper) {
        TableColumn<Film, String> c = new TableColumn<>(title);
        c.setCellValueFactory(cd -> new SimpleStringProperty(mapper.apply(cd.getValue())));
        return c;
    }

    private void openForm(Film film) {
        boolean isEdit = film != null;
        Dialog<Film> dialog = new Dialog<>();
        dialog.setTitle(isEdit ? "Modifier le film" : "Nouveau film");
        dialog.setHeaderText(null);

        GridPane grid = new GridPane();
        grid.setHgap(12); grid.setVgap(10);
        grid.setPadding(new Insets(20));

        TextField titreF  = new TextField(isEdit ? film.getTitre() : "");
        TextField dureeF  = new TextField(isEdit ? String.valueOf(film.getDuree()) : "");
        TextField genreF  = new TextField(isEdit ? film.getGenre() : "");
        TextArea  descF   = new TextArea(isEdit ? film.getDescription() : "");
        descF.setPrefRowCount(3); descF.setWrapText(true);
        DatePicker dateF  = new DatePicker(isEdit ? film.getDateSortie() : LocalDate.now());
        ComboBox<String> classF = new ComboBox<>();
        classF.getItems().addAll("Tout public", "AP10", "AP12", "AP16", "AP18");
        classF.setValue(isEdit ? film.getClassification() : "Tout public");

        addRow(grid, 0, "Titre *", titreF);
        addRow(grid, 1, "Durée (min) *", dureeF);
        addRow(grid, 2, "Genre *", genreF);
        addRow(grid, 3, "Date sortie *", dateF);
        addRow(grid, 4, "Classification *", classF);
        addRow(grid, 5, "Description", descF);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.setResultConverter(btn -> {
            if (btn != ButtonType.OK) return null;
            Film f = isEdit ? film : new Film();
            f.setTitre(titreF.getText().trim());
            f.setGenre(genreF.getText().trim());
            f.setDescription(descF.getText().trim());
            f.setDateSortie(dateF.getValue());
            f.setClassification(classF.getValue());
            f.setActif(true);
            try { f.setDuree(Integer.parseInt(dureeF.getText().trim())); }
            catch (NumberFormatException ex) { f.setDuree(0); }
            return f;
        });

        dialog.showAndWait().ifPresent(f -> {
            if (f.getTitre().isEmpty() || f.getGenre().isEmpty() || f.getDuree() <= 0) {
                AlertUtil.showError("Validation", "Titre, genre et durée sont obligatoires.");
                return;
            }
            try {
                if (isEdit) dao.update(f); else dao.insert(f);
                loadData(); loadGenres();
                AlertUtil.showInfo("Succès", isEdit ? "Film modifié." : "Film ajouté.");
            } catch (Exception ex) {
                AlertUtil.showError("Erreur", ex.getMessage());
            }
        });
    }

    private void deleteFilm(Film f) {
        if (!AlertUtil.showConfirm("Supprimer", "Supprimer le film \"" + f.getTitre() + "\" ?")) return;
        try {
            if (!dao.delete(f.getId())) {
                AlertUtil.showError("Impossible", "Ce film a des séances futures planifiées.");
                return;
            }
            loadData();
            AlertUtil.showInfo("Succès", "Film supprimé.");
        } catch (Exception ex) {
            AlertUtil.showError("Erreur", ex.getMessage());
        }
    }

    private void addRow(GridPane g, int row, String lbl, javafx.scene.Node field) {
        Label l = new Label(lbl); l.getStyleClass().add("form-label");
        g.add(l, 0, row); g.add(field, 1, row);
        GridPane.setHgrow(field, Priority.ALWAYS);
        if (field instanceof TextField tf) tf.setPrefWidth(280);
    }

    public VBox getView() { return view; }
}
