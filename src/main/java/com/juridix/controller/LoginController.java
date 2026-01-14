package com.juridix.controller;

import com.juridix.controller.MainController;
import com.juridix.model.Usuario;
import com.juridix.seguridad.AuthService;
import com.juridix.seguridad.SesionUsuario;
import com.juridix.ui.views.LoginView;
import javafx.stage.Stage;

public class LoginController {

    private final LoginView view;
    private final Stage stage;

    public LoginController(Stage stage) {
        this.stage = stage;
        this.view = new LoginView();
        init();
    }

    private void init() {
        view.getLoginButton().setOnAction(e -> handleLogin());
    }

    public void mostrar() {
        stage.setScene(view.getScene());
        stage.setTitle("Login - Juridix");
        stage.show();
    }

    private void handleLogin() {

        String user = view.getUsernameField().getText();
        String pass = view.getPasswordField().getText();

        if (user.isEmpty() || pass.isEmpty()) {
            view.getMessageLabel().setText("Complete todos los campos");
            return;
        }

        AuthService auth = new AuthService();
        Usuario usuario = auth.login(user, pass);

        if (usuario != null) {
            SesionUsuario.iniciarSesion(usuario);

            view.getMessageLabel().setStyle("-fx-text-fill: green;");
            view.getMessageLabel().setText("Bienvenido " + usuario.getUsername());

            MainController mainController = new MainController(stage);
            mainController.mostrar();

        } else {
            view.getMessageLabel().setStyle("-fx-text-fill: red;");
            view.getMessageLabel().setText("Usuario o contraseña incorrectos");
        }
    }
}

