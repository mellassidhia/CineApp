package com.cinema.controller;

import com.cinema.dao.ClientDAO;
import com.cinema.dao.UserDAO;
import com.cinema.model.Client;
import com.cinema.util.AlertUtil;
import com.cinema.util.SessionManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

public class ProfileController {

    private final VBox view;
    private final ClientDAO clientDAO = new ClientDAO();

    public ProfileController() {
        view = new VBox(20);
        view.getStyleClass().add("content-root");
        view.setPadding(new Insets(0, 24, 24, 24));

        Label title = new Label("Mon Profil");
        title.getStyleClass().add("page-title");
        Label sub = new Label("Mettez à jour vos informations personnelles.");
        sub.getStyleClass().add("page-subtitle");

        int idClient = SessionManager.getInstance().getCurrentUser().getIdClient();

        Client client;
        try {
            client = clientDAO.getById(idClient);
        } catch (Exception e) {
            AlertUtil.showError("Erreur", e.getMessage());
            view.getChildren().addAll(title, sub);
            return;
        }

        // Info card
        VBox card = new VBox(16);
        card.getStyleClass().add("card");
        card.setMaxWidth(500);

        Label cardTitle = new Label("Informations personnelles");
        cardTitle.getStyleClass().add("section-title");

        GridPane grid = new GridPane();
        grid.setHgap(16); grid.setVgap(12);

        TextField nomF    = new TextField(client.getNom());
        TextField prenomF = new TextField(client.getPrenom());
        TextField emailF  = new TextField(client.getEmail());
        TextField telF    = new TextField(client.getTelephone() != null ? client.getTelephone() : "");
        DatePicker dnF    = new DatePicker(client.getDateNaissance());

        nomF.getStyleClass().add("form-field");
        prenomF.getStyleClass().add("form-field");
        emailF.getStyleClass().add("form-field");
        telF.getStyleClass().add("form-field");

        addRow(grid, 0, "Nom *", nomF);
        addRow(grid, 1, "Prénom *", prenomF);
        addRow(grid, 2, "Email *", emailF);
        addRow(grid, 3, "Téléphone", telF);
        addRow(grid, 4, "Date de naissance", dnF);

        Label errLbl = new Label();
        errLbl.getStyleClass().add("error-label");

        Button saveBtn = new Button("💾 Enregistrer les modifications");
        saveBtn.getStyleClass().add("btn-primary");
        saveBtn.setOnAction(e -> {
            String nom    = nomF.getText().trim();
            String prenom = prenomF.getText().trim();
            String email  = emailF.getText().trim();
            if (nom.isEmpty() || prenom.isEmpty() || email.isEmpty()) {
                errLbl.setText("Nom, prénom et email sont obligatoires."); return;
            }
            client.setNom(nom);
            client.setPrenom(prenom);
            client.setEmail(email);
            client.setTelephone(telF.getText().trim());
            client.setDateNaissance(dnF.getValue());
            try {
                clientDAO.update(client);
                errLbl.setText("");
                AlertUtil.showInfo("Succès", "Profil mis à jour avec succès.");
            } catch (Exception ex) {
                errLbl.setText("Erreur : " + ex.getMessage());
            }
        });

        card.getChildren().addAll(cardTitle, new Separator(), grid, errLbl, saveBtn);

        // Password change card
        VBox pwCard = new VBox(14);
        pwCard.getStyleClass().add("card");
        pwCard.setMaxWidth(500);
        Label pwTitle = new Label("Changer le mot de passe");
        pwTitle.getStyleClass().add("section-title");

        PasswordField oldPwF  = new PasswordField(); oldPwF.setPromptText("Mot de passe actuel"); oldPwF.getStyleClass().add("form-field"); oldPwF.setMaxWidth(Double.MAX_VALUE);
        PasswordField newPwF  = new PasswordField(); newPwF.setPromptText("Nouveau mot de passe"); newPwF.getStyleClass().add("form-field"); newPwF.setMaxWidth(Double.MAX_VALUE);
        PasswordField newPw2F = new PasswordField(); newPw2F.setPromptText("Confirmer nouveau mot de passe"); newPw2F.getStyleClass().add("form-field"); newPw2F.setMaxWidth(Double.MAX_VALUE);
        Label pwErr = new Label(); pwErr.getStyleClass().add("error-label");

        Button changePwBtn = new Button("🔑 Changer le mot de passe");
        changePwBtn.getStyleClass().add("btn-secondary");
        changePwBtn.setOnAction(e -> {
            String oldPw  = oldPwF.getText();
            String newPw  = newPwF.getText();
            String newPw2 = newPw2F.getText();
            if (oldPw.isEmpty() || newPw.isEmpty()) { pwErr.setText("Remplissez tous les champs."); return; }
            if (!newPw.equals(newPw2)) { pwErr.setText("Les mots de passe ne correspondent pas."); return; }
            if (newPw.length() < 6) { pwErr.setText("Minimum 6 caractères."); return; }
            try {
                UserDAO uDao = new UserDAO();
                String hashedOld = UserDAO.hashPassword(oldPw);
                if (!hashedOld.equals(SessionManager.getInstance().getCurrentUser().getPasswordHash())) {
                    pwErr.setText("Mot de passe actuel incorrect."); return;
                }
                // Update password
                String hashedNew = UserDAO.hashPassword(newPw);
                try (var con = com.cinema.util.DatabaseConnection.getConnection();
                     var ps = con.prepareStatement("UPDATE users SET password_hash=? WHERE id=?")) {
                    ps.setString(1, hashedNew);
                    ps.setInt(2, SessionManager.getInstance().getCurrentUser().getId());
                    ps.executeUpdate();
                }
                SessionManager.getInstance().getCurrentUser().setPasswordHash(hashedNew);
                pwErr.setText("");
                oldPwF.clear(); newPwF.clear(); newPw2F.clear();
                AlertUtil.showInfo("Succès", "Mot de passe modifié.");
            } catch (Exception ex) {
                pwErr.setText("Erreur : " + ex.getMessage());
            }
        });

        GridPane pwGrid = new GridPane();
        pwGrid.setHgap(16); pwGrid.setVgap(12);
        addRow(pwGrid, 0, "Actuel", oldPwF);
        addRow(pwGrid, 1, "Nouveau", newPwF);
        addRow(pwGrid, 2, "Confirmer", newPw2F);

        pwCard.getChildren().addAll(pwTitle, new Separator(), pwGrid, pwErr, changePwBtn);

        view.getChildren().addAll(title, sub, card, pwCard);
    }

    private void addRow(GridPane g, int row, String lbl, javafx.scene.Node field) {
        Label l = new Label(lbl); l.getStyleClass().add("form-label");
        g.add(l, 0, row); g.add(field, 1, row);
        GridPane.setHgrow(field, Priority.ALWAYS);
        if (field instanceof TextField tf) tf.setPrefWidth(280);
        if (field instanceof DatePicker dp) dp.setMaxWidth(Double.MAX_VALUE);
    }

    public VBox getView() { return view; }
}
