package com.cinema.controller;

import com.cinema.dao.StatistiquesDAO;
import com.cinema.util.AlertUtil;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.util.List;
import java.util.Map;

public class StatistiquesController {

    private final VBox view;
    private final StatistiquesDAO dao = new StatistiquesDAO();

    // KPI labels
    private final Label lblCA       = kpiValue("0 DT");
    private final Label lblRes      = kpiValue("0");
    private final Label lblClients  = kpiValue("0");
    private final Label lblFilms    = kpiValue("0");

    // Chart canvases
    private final Canvas canvasBar  = new Canvas(680, 240);
    private final Canvas canvasPie  = new Canvas(380, 240);
    private final Canvas canvasFill = new Canvas(680, 200);

    // Period selector
    private final ComboBox<String> periodBox = new ComboBox<>();

    public StatistiquesController() {
        view = new VBox(18);
        view.getStyleClass().add("content-root");
        view.setPadding(new Insets(0, 24, 24, 24));

        Label title = new Label("Tableau de Bord");
        title.getStyleClass().add("page-title");
        Label sub = new Label("Statistiques et performances du cinema.");
        sub.getStyleClass().add("page-subtitle");

        // Period selector + refresh
        HBox toolbar = new HBox(12);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.getStyleClass().add("filter-bar");
        periodBox.getItems().addAll("7 derniers jours", "30 derniers jours", "90 derniers jours");
        periodBox.setValue("30 derniers jours");
        periodBox.setOnAction(e -> loadAll());
        Button refreshBtn = new Button("Rafraichir");
        refreshBtn.getStyleClass().add("btn-secondary");
        refreshBtn.setOnAction(e -> loadAll());
        toolbar.getChildren().addAll(new Label("Periode :"), periodBox, refreshBtn);

        // KPI cards row
        HBox kpiRow = new HBox(16);
        kpiRow.getChildren().addAll(
                kpiCard("Chiffre d Affaires", lblCA,      "#141450", "DT"),
                kpiCard("Reservations",        lblRes,     "#C81E1E", ""),
                kpiCard("Clients actifs",      lblClients, "#2196F3", ""),
                kpiCard("Films actifs",        lblFilms,   "#4CAF50", "")
        );

        // Bar chart card — reservations over time
        VBox barCard = new VBox(8);
        barCard.getStyleClass().add("card");
        Label barTitle = new Label("Reservations dans le temps");
        barTitle.getStyleClass().add("section-title");
        barCard.getChildren().addAll(barTitle, canvasBar);

        // Pie chart card — revenue by genre
        VBox pieCard = new VBox(8);
        pieCard.getStyleClass().add("card");
        Label pieTitle = new Label("Recette par genre");
        pieTitle.getStyleClass().add("section-title");
        pieCard.getChildren().addAll(pieTitle, canvasPie);

        // Top films card
        VBox topFilmsCard = new VBox(8);
        topFilmsCard.getStyleClass().add("card");
        topFilmsCard.setPrefWidth(320);
        Label topTitle = new Label("Top 5 films");
        topTitle.getStyleClass().add("section-title");
        this.topFilmsContent = new VBox(6); VBox topFilmsContent = this.topFilmsContent;
        topFilmsCard.getChildren().addAll(topTitle, topFilmsContent);

        // Charts row
        HBox chartsRow = new HBox(16);
        HBox.setHgrow(barCard, Priority.ALWAYS);
        chartsRow.getChildren().addAll(barCard, pieCard);

        // Fill rate card
        VBox fillCard = new VBox(8);
        fillCard.getStyleClass().add("card");
        Label fillTitle = new Label("Taux de remplissage des salles (%)");
        fillTitle.getStyleClass().add("section-title");
        fillCard.getChildren().addAll(fillTitle, canvasFill);

        // Revenue per film card
        VBox recetteCard = new VBox(8);
        recetteCard.getStyleClass().add("card");
        Label recetteTitle = new Label("Recette par film (DT)");
        recetteTitle.getStyleClass().add("section-title");
        canvasRecette = new Canvas(680, 200);
        recetteCard.getChildren().addAll(recetteTitle, canvasRecette);

        // Bottom row
        HBox bottomRow = new HBox(16);
        HBox.setHgrow(fillCard, Priority.ALWAYS);
        HBox.setHgrow(recetteCard, Priority.ALWAYS);
        bottomRow.getChildren().addAll(fillCard, topFilmsCard);

        ScrollPane scroll = new ScrollPane();
        VBox inner = new VBox(18, toolbar, kpiRow, chartsRow, recetteCard, bottomRow);
        inner.setPadding(new Insets(0, 0, 24, 0));
        scroll.setContent(inner);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background:transparent; -fx-background-color:transparent;");

        view.getChildren().addAll(title, sub, scroll);
        VBox.setVgrow(scroll, Priority.ALWAYS);

        // Load data and draw charts
        loadAll();

        // Store reference for top films refresh


    }

    // Stored refs for dynamic update
    private VBox topFilmsContent;
    private Canvas canvasRecette;

    private void loadAll() {
        try {
            int days = switch (periodBox.getValue()) {
                case "7 derniers jours"  -> 7;
                case "90 derniers jours" -> 90;
                default                  -> 30;
            };

            // KPIs
            lblCA.setText(String.format("%.2f DT", dao.getChiffreAffairesGlobal()));
            lblRes.setText(String.valueOf(dao.getTotalReservations()));
            lblClients.setText(String.valueOf(dao.getTotalClients()));
            lblFilms.setText(String.valueOf(dao.getTotalFilms()));

            // Bar chart — reservations per day
            Map<String, Integer> resParJour = dao.getReservationsParJour(days);
            drawBarChart(canvasBar, resParJour, "Reservations", Color.web("#141450"));

            // Pie chart — revenue by genre
            Map<String, Double> recetteGenre = dao.getRecetteParGenre();
            drawPieChart(canvasPie, recetteGenre);

            // Fill rate horizontal bars
            Map<String, Double> taux = dao.getTauxRemplissage();
            drawHorizontalBars(canvasFill, taux, "%", Color.web("#2196F3"));

            // Revenue per film bar chart
            Map<String, Double> recetteFilm = dao.getRecetteParFilm();
            drawBarChartDouble(canvasRecette, recetteFilm, "DT", Color.web("#C81E1E"));

            // Top films list
            topFilmsContent.getChildren().clear();
            Map<String, Integer> topFilms = dao.getTopFilms(5);
            if (topFilms.isEmpty()) {
                topFilmsContent.getChildren().add(new Label("Aucune donnee disponible."));
            } else {
                int rank = 1;
                for (Map.Entry<String, Integer> e : topFilms.entrySet()) {
                    String titre = (e.getKey() != null && !e.getKey().isEmpty()) ? e.getKey() : "Film inconnu";
                    String line  = "#" + rank + "  " + titre + "  -  " + e.getValue() + " res.";
                    Label rowLbl = new Label(line);
                    rowLbl.setMaxWidth(Double.MAX_VALUE);
                    rowLbl.setWrapText(true);
                    rowLbl.setStyle("-fx-font-size:12px; -fx-text-fill:#111111; -fx-padding:6 4 6 4;");
                    topFilmsContent.getChildren().add(rowLbl);
                    topFilmsContent.getChildren().add(new Separator());
                    rank++;
                }
            }

        } catch (Exception ex) {
            AlertUtil.showError("Erreur statistiques", ex.getMessage());
        }
    }

    // ---- Drawing helpers ----

    private void drawBarChart(Canvas canvas, Map<String, Integer> data, String unit, Color color) {
        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());

        if (data.isEmpty()) {
            gc.setFill(Color.GRAY);
            gc.fillText("Aucune donnee disponible.", 20, 120);
            return;
        }

        double w = canvas.getWidth();
        double h = canvas.getHeight();
        double padL = 50, padR = 20, padT = 20, padB = 40;
        double chartW = w - padL - padR;
        double chartH = h - padT - padB;

        int maxVal = data.values().stream().mapToInt(v -> v).max().orElse(1);
        if (maxVal == 0) maxVal = 1;

        List<Map.Entry<String, Integer>> entries = List.copyOf(data.entrySet());
        double barW = chartW / entries.size() * 0.6;
        double gap  = chartW / entries.size();

        // Grid lines
        gc.setStroke(Color.web("#e0e0e0"));
        gc.setLineWidth(1);
        for (int i = 0; i <= 4; i++) {
            double y = padT + chartH - (chartH * i / 4.0);
            gc.strokeLine(padL, y, padL + chartW, y);
            gc.setFill(Color.GRAY);
            gc.setFont(Font.font(10));
            gc.fillText(String.valueOf(maxVal * i / 4), padL - 35, y + 4);
        }

        // Bars
        for (int i = 0; i < entries.size(); i++) {
            Map.Entry<String, Integer> e = entries.get(i);
            double barH = chartH * e.getValue() / maxVal;
            double x = padL + i * gap + (gap - barW) / 2;
            double y = padT + chartH - barH;

            gc.setFill(color);
            gc.fillRoundRect(x, y, barW, barH, 4, 4);

            // Value on top
            gc.setFill(Color.web("#333"));
            gc.setFont(Font.font(10));
            gc.fillText(String.valueOf(e.getValue()), x + barW / 2 - 4, y - 4);

            // Label below — show every Nth label to avoid overlap
            if (entries.size() <= 15 || i % (entries.size() / 7 + 1) == 0) {
                String lbl = e.getKey();
                if (lbl.length() > 8) lbl = lbl.substring(5); // show MM-DD
                gc.setFont(Font.font(9));
                gc.fillText(lbl, x, padT + chartH + 14);
            }
        }

        // Axes
        gc.setStroke(Color.web("#999"));
        gc.setLineWidth(1.5);
        gc.strokeLine(padL, padT, padL, padT + chartH);
        gc.strokeLine(padL, padT + chartH, padL + chartW, padT + chartH);
    }

    private void drawBarChartDouble(Canvas canvas, Map<String, Double> data, String unit, Color color) {
        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());

        if (data.isEmpty()) {
            gc.setFill(Color.GRAY);
            gc.fillText("Aucune donnee disponible.", 20, 100);
            return;
        }

        double w = canvas.getWidth();
        double h = canvas.getHeight();
        double padL = 60, padR = 20, padT = 20, padB = 50;
        double chartW = w - padL - padR;
        double chartH = h - padT - padB;

        double maxVal = data.values().stream().mapToDouble(v -> v).max().orElse(1);
        if (maxVal == 0) maxVal = 1;

        List<Map.Entry<String, Double>> entries = List.copyOf(data.entrySet());
        double barW = chartW / entries.size() * 0.55;
        double gap  = chartW / entries.size();

        gc.setStroke(Color.web("#e0e0e0"));
        gc.setLineWidth(1);
        for (int i = 0; i <= 4; i++) {
            double y = padT + chartH - (chartH * i / 4.0);
            gc.strokeLine(padL, y, padL + chartW, y);
            gc.setFill(Color.GRAY);
            gc.setFont(Font.font(10));
            gc.fillText(String.format("%.0f", maxVal * i / 4), padL - 55, y + 4);
        }

        for (int i = 0; i < entries.size(); i++) {
            Map.Entry<String, Double> e = entries.get(i);
            double barH = chartH * e.getValue() / maxVal;
            double x = padL + i * gap + (gap - barW) / 2;
            double y = padT + chartH - barH;

            gc.setFill(color);
            gc.fillRoundRect(x, y, barW, barH, 4, 4);

            gc.setFill(Color.web("#333"));
            gc.setFont(Font.font(9));
            String valStr = String.format("%.0f", e.getValue());
            gc.fillText(valStr, x + barW / 2 - valStr.length() * 3, y - 4);

            String lbl = e.getKey().length() > 12 ? e.getKey().substring(0, 12) + "." : e.getKey();
            gc.setFont(Font.font(9));
            gc.save();
            gc.translate(x + barW / 2, padT + chartH + 8);
            gc.rotate(30);
            gc.fillText(lbl, 0, 0);
            gc.restore();
        }

        gc.setStroke(Color.web("#999"));
        gc.setLineWidth(1.5);
        gc.strokeLine(padL, padT, padL, padT + chartH);
        gc.strokeLine(padL, padT + chartH, padL + chartW, padT + chartH);
    }

    private static final Color[] PIE_COLORS = {
            Color.web("#141450"), Color.web("#C81E1E"), Color.web("#2196F3"),
            Color.web("#4CAF50"), Color.web("#FF9800"), Color.web("#9C27B0"),
            Color.web("#00BCD4"), Color.web("#795548")
    };

    private void drawPieChart(Canvas canvas, Map<String, Double> data) {
        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());

        if (data.isEmpty()) {
            gc.setFill(Color.GRAY);
            gc.fillText("Aucune donnee.", 20, 120);
            return;
        }

        double total = data.values().stream().mapToDouble(v -> v).sum();
        if (total == 0) return;

        double cx = 110, cy = 110, r = 90;
        double startAngle = 0;
        int ci = 0;

        List<Map.Entry<String, Double>> entries = List.copyOf(data.entrySet());
        for (Map.Entry<String, Double> e : entries) {
            double angle = 360.0 * e.getValue() / total;
            gc.setFill(PIE_COLORS[ci % PIE_COLORS.length]);
            gc.fillArc(cx - r, cy - r, r * 2, r * 2, startAngle, angle, javafx.scene.shape.ArcType.ROUND);
            gc.setStroke(Color.WHITE);
            gc.setLineWidth(1.5);
            gc.strokeArc(cx - r, cy - r, r * 2, r * 2, startAngle, angle, javafx.scene.shape.ArcType.ROUND);
            startAngle += angle;
            ci++;
        }

        // Legend
        double lx = cx + r + 20, ly = 20;
        ci = 0;
        for (Map.Entry<String, Double> e : entries) {
            gc.setFill(PIE_COLORS[ci % PIE_COLORS.length]);
            gc.fillRoundRect(lx, ly + ci * 22, 14, 14, 3, 3);
            gc.setFill(Color.web("#333"));
            gc.setFont(Font.font(10));
            String pct = String.format("%.1f%%", 100.0 * e.getValue() / total);
            String lbl = e.getKey().length() > 12 ? e.getKey().substring(0, 12) : e.getKey();
            gc.fillText(lbl + " " + pct, lx + 18, ly + ci * 22 + 11);
            ci++;
        }
    }

    private void drawHorizontalBars(Canvas canvas, Map<String, Double> data, String unit, Color color) {
        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());

        if (data.isEmpty()) {
            gc.setFill(Color.GRAY);
            gc.fillText("Aucune donnee.", 20, 80);
            return;
        }

        double w = canvas.getWidth();
        double padL = 110, padR = 60, padT = 16;
        double chartW = w - padL - padR;
        double rowH = 32;

        List<Map.Entry<String, Double>> entries = List.copyOf(data.entrySet());
        for (int i = 0; i < entries.size(); i++) {
            Map.Entry<String, Double> e = entries.get(i);
            double y = padT + i * rowH;
            double val = Math.min(e.getValue(), 100.0);

            // Background track
            gc.setFill(Color.web("#e8e8f0"));
            gc.fillRoundRect(padL, y + 6, chartW, 18, 9, 9);

            // Fill
            Color barColor = val > 80 ? Color.web("#C81E1E") : val > 50 ? Color.web("#FF9800") : color;
            gc.setFill(barColor);
            gc.fillRoundRect(padL, y + 6, chartW * val / 100.0, 18, 9, 9);

            // Label left
            gc.setFill(Color.web("#333"));
            gc.setFont(Font.font("Arial", FontWeight.BOLD, 11));
            gc.fillText(e.getKey(), 4, y + 18);

            // Value right
            gc.setFont(Font.font(11));
            gc.fillText(String.format("%.1f%%", val), padL + chartW + 6, y + 18);
        }
    }

    // ---- UI helpers ----

    private VBox kpiCard(String label, Label valueLabel, String color, String suffix) {
        VBox card = new VBox(6);
        card.setStyle("-fx-background-color:" + color + "; -fx-background-radius:10; -fx-padding:16 20;");
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPrefWidth(200);
        HBox.setHgrow(card, Priority.ALWAYS);
        Label lbl = new Label(label);
        lbl.setStyle("-fx-text-fill:rgba(255,255,255,0.8); -fx-font-size:12px;");
        card.getChildren().addAll(lbl, valueLabel);
        return card;
    }

    private static Label kpiValue(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-text-fill:white; -fx-font-size:22px; -fx-font-weight:bold;");
        return l;
    }

    public VBox getView() { return view; }
}