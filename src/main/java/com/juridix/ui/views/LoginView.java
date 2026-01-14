package com.juridix.ui.views;

import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

public class LoginView {

    private final TextField usernameField = new TextField();
    private final PasswordField passwordField = new PasswordField();
    private final Button loginButton = new Button("Ingresar");
    private final Label messageLabel = new Label();
    private final Scene scene;

    public LoginView() {

        VBox root = new VBox(10);
        root.setStyle("-fx-padding: 20;");

        usernameField.setPromptText("Usuario");
        passwordField.setPromptText("Contraseña");

        root.getChildren().addAll(
                usernameField,
                passwordField,
                loginButton,
                messageLabel
        );

        scene = new Scene(root, 300, 200);
    }

    public Scene getScene() {
        return scene;
    }

    public TextField getUsernameField() {
        return usernameField;
    }

    public PasswordField getPasswordField() {
        return passwordField;
    }

    public Button getLoginButton() {
        return loginButton;
    }

    public Label getMessageLabel() {
        return messageLabel;
    }
}
