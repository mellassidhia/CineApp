package com.cinema.controller;

import com.cinema.dao.UserDAO;
import com.cinema.model.User;
import com.cinema.util.SessionManager;
import com.cinema.view.ViewManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Text;

public class LoginController {

    private final VBox view;

    public LoginController() {
        // ---- Left decorative pane ----
        VBox leftPane = new VBox();
        leftPane.getStyleClass().add("auth-root");
        leftPane.setPrefWidth(420);
        leftPane.setAlignment(Pos.CENTER);
        leftPane.setSpacing(12);

        Label cinema  = new Label("🎬");
        cinema.setStyle("-fx-font-size:64px;");
        Label tagline = new Label("Votre expérience cinéma\ncommence ici");
        tagline.setStyle("-fx-font-size:16px; -fx-text-fill:white; -fx-text-alignment:center; -fx-alignment:center;");
        leftPane.getChildren().addAll(cinema, tagline);

        // ---- Right form pane ----
        VBox card = new VBox(14);
        card.getStyleClass().add("auth-card");
        card.setMaxWidth(360);
        card.setAlignment(Pos.TOP_LEFT);

        Label title    = new Label("Connexion");
        title.getStyleClass().add("auth-title");
        Label subtitle = new Label("Bienvenue ! Veuillez vous connecter.");
        subtitle.getStyleClass().add("auth-subtitle");

        Label userLbl = new Label("Nom d'utilisateur");
        userLbl.getStyleClass().add("form-label");
        TextField userField = new TextField();
        userField.getStyleClass().add("auth-field");
        userField.setPromptText("Entrez votre nom d'utilisateur");
        userField.setMaxWidth(Double.MAX_VALUE);

        Label passLbl = new Label("Mot de passe");
        passLbl.getStyleClass().add("form-label");
        PasswordField passField = new PasswordField();
        passField.getStyleClass().add("auth-field");
        passField.setPromptText("Entrez votre mot de passe");
        passField.setMaxWidth(Double.MAX_VALUE);

        Label errorLbl = new Label();
        errorLbl.getStyleClass().add("error-label");
        errorLbl.setWrapText(true);

        Button loginBtn = new Button("Se connecter");
        loginBtn.getStyleClass().add("btn-primary");
        loginBtn.setMaxWidth(Double.MAX_VALUE);

        Separator sep = new Separator();

        HBox regRow = new HBox(6);
        regRow.setAlignment(Pos.CENTER);
        Label noAccount = new Label("Pas encore de compte ?");
        noAccount.getStyleClass().add("info-label");
        Label regLink = new Label("S'inscrire");
        regLink.getStyleClass().add("link-label");
        regRow.getChildren().addAll(noAccount, regLink);

        card.getChildren().addAll(title, subtitle, new Separator(),
                userLbl, userField, passLbl, passField,
                errorLbl, loginBtn, sep, regRow);

        // ---- Actions ----
        Runnable doLogin = () -> {
            String u = userField.getText().trim();
            String p = passField.getText();
            if (u.isEmpty() || p.isEmpty()) {
                errorLbl.setText("Veuillez remplir tous les champs.");
                return;
            }
            try {
                UserDAO dao = new UserDAO();
                User user = dao.authenticate(u, p);
                if (user == null) {
                    errorLbl.setText("Identifiants incorrects.");
                    return;
                }
                SessionManager.getInstance().setCurrentUser(user);
                ViewManager.showMain();
            } catch (Exception ex) {
                errorLbl.setText("Erreur DB : " + ex.getMessage());
            }
        };

        loginBtn.setOnAction(e -> doLogin.run());
        passField.setOnAction(e -> doLogin.run());
        regLink.setOnMouseClicked(e -> ViewManager.showRegister());

        // ---- Right wrapper ----
        StackPane rightWrapper = new StackPane(card);
        rightWrapper.setStyle("-fx-background-color:white;");
        rightWrapper.setPadding(new Insets(40));
        HBox.setHgrow(rightWrapper, Priority.ALWAYS);

        // ---- Root ----
        HBox root = new HBox(leftPane, rightWrapper);
        HBox.setHgrow(leftPane, Priority.NEVER);
        this.view = new VBox(root);
        VBox.setVgrow(root, Priority.ALWAYS);
    }

    public VBox getView() { return view; }
}
