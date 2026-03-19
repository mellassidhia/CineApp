package com.cinema.controller;

import com.cinema.dao.UserDAO;
import com.cinema.view.ViewManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

public class RegisterController {

    private final VBox view;

    private final TextField     nomF    = new TextField();
    private final TextField     prenomF = new TextField();
    private final TextField     emailF  = new TextField();
    private final TextField     telF    = new TextField();
    private final TextField     userF   = new TextField();
    private final PasswordField passF   = new PasswordField();
    private final PasswordField pass2F  = new PasswordField();
    private final Label         errLbl  = new Label();

    public RegisterController() {

        // Left decorative pane
        VBox leftPane = new VBox();
        leftPane.getStyleClass().add("auth-root");
        leftPane.setPrefWidth(320);
        leftPane.setMinWidth(320);
        leftPane.setMaxWidth(320);
        leftPane.setAlignment(Pos.CENTER);
        leftPane.setSpacing(12);
        Label icon = new Label("🎬");
        icon.setStyle("-fx-font-size:60px;");
        Label msg = new Label("Rejoignez-nous !\nReservez vos films preferes.");
        msg.setStyle("-fx-font-size:14px; -fx-text-fill:white; -fx-text-alignment:center; -fx-alignment:center;");
        leftPane.getChildren().addAll(icon, msg);

        // Setup fields
        nomF.setPromptText("ex: Dupont");
        prenomF.setPromptText("ex: Marie");
        emailF.setPromptText("ex: marie@email.com");
        telF.setPromptText("ex: 0612345678");
        userF.setPromptText("ex: marie123");
        passF.setPromptText("minimum 6 caracteres");
        pass2F.setPromptText("repetez le mot de passe");

        for (Control c : new Control[]{nomF, prenomF, emailF, telF, userF, passF, pass2F}) {
            c.getStyleClass().add("auth-field");
            ((Region) c).setMaxWidth(Double.MAX_VALUE);
            ((Region) c).setPrefHeight(36);
        }

        errLbl.setWrapText(true);
        errLbl.setMaxWidth(400);
        errLbl.setStyle("-fx-text-fill: red; -fx-font-size: 12px;");

        // Card — pure VBox, one field per row
        VBox card = new VBox(8);
        card.setStyle("-fx-background-color:white; -fx-background-radius:12; -fx-padding:30;");
        card.setMaxWidth(480);

        Label title = new Label("Creer un compte");
        title.getStyleClass().add("auth-title");
        Label sub = new Label("Tous les champs marques * sont obligatoires.");
        sub.getStyleClass().add("auth-subtitle");

        Button regBtn = new Button("Creer le compte");
        regBtn.getStyleClass().add("btn-primary");
        regBtn.setMaxWidth(Double.MAX_VALUE);
        regBtn.setPrefHeight(40);
        regBtn.setOnAction(e -> doRegister());

        Label loginLink = new Label("Deja un compte ? Se connecter");
        loginLink.setStyle("-fx-text-fill:#141450; -fx-underline:true; -fx-cursor:hand; -fx-font-size:12px;");
        loginLink.setOnMouseClicked(e -> ViewManager.showLogin());

        card.getChildren().addAll(
            title, sub, new Separator(),
            lbl("Nom *"),        nomF,
            lbl("Prenom *"),     prenomF,
            lbl("Email *"),      emailF,
            lbl("Telephone"),    telF,
            lbl("Utilisateur *"),userF,
            lbl("Mot de passe *"),     passF,
            lbl("Confirmer mot de passe *"), pass2F,
            errLbl,
            regBtn,
            loginLink
        );

        // Right wrapper
        ScrollPane scroll = new ScrollPane(card);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background:white; -fx-background-color:white; -fx-border-color:transparent;");
        scroll.setPadding(new Insets(20));
        HBox.setHgrow(scroll, Priority.ALWAYS);

        HBox root = new HBox(leftPane, scroll);
        VBox.setVgrow(root, Priority.ALWAYS);
        this.view = new VBox(root);
        VBox.setVgrow(root, Priority.ALWAYS);
    }

    private Label lbl(String text) {
        Label l = new Label(text);
        l.getStyleClass().add("form-label");
        VBox.setMargin(l, new Insets(6, 0, 0, 0));
        return l;
    }

    private void doRegister() {
        String nom    = nomF.getText().trim();
        String prenom = prenomF.getText().trim();
        String email  = emailF.getText().trim();
        String tel    = telF.getText().trim();
        String user   = userF.getText().trim();
        String pass   = passF.getText();
        String pass2  = pass2F.getText();

        if (nom.isEmpty())         { errLbl.setText("Le champ Nom est vide.");                  return; }
        if (prenom.isEmpty())      { errLbl.setText("Le champ Prenom est vide.");               return; }
        if (email.isEmpty())       { errLbl.setText("Le champ Email est vide.");                return; }
        if (!email.contains("@")) { errLbl.setText("Email invalide.");                          return; }
        if (user.isEmpty())        { errLbl.setText("Le champ Utilisateur est vide.");          return; }
        if (pass.isEmpty())        { errLbl.setText("Le champ Mot de passe est vide.");         return; }
        if (!pass.equals(pass2))   { errLbl.setText("Les mots de passe ne correspondent pas."); return; }
        if (pass.length() < 6)    { errLbl.setText("Mot de passe: minimum 6 caracteres.");      return; }

        try {
            UserDAO dao = new UserDAO();
            if (dao.usernameExists(user)) { errLbl.setText("Nom d utilisateur deja pris.");    return; }
            if (dao.emailExists(email))   { errLbl.setText("Email deja utilise.");             return; }
            dao.register(user, pass, nom, prenom, email, tel);
            errLbl.setStyle("-fx-text-fill: green; -fx-font-size:12px;");
            errLbl.setText("Compte cree avec succes ! Redirection vers la connexion...");
            new Thread(() -> {
                try { Thread.sleep(1500); } catch (InterruptedException ignored) {}
                javafx.application.Platform.runLater(ViewManager::showLogin);
            }).start();
        } catch (Exception ex) {
            errLbl.setStyle("-fx-text-fill: red; -fx-font-size:12px;");
            errLbl.setText("Erreur : " + ex.getMessage());
        }
    }

    public VBox getView() { return view; }
}
