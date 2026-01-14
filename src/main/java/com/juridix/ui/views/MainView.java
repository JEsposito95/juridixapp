package com.juridix.ui.views;

import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.geometry.Insets;

public class MainView {

    private final BorderPane root;
    private final Scene scene;

    private final Button nuevoExpedienteBtn;
    private final Button usuariosBtn;
    private final Button salirBtn;
    private final Label userLabel;

    public MainView() {

        root = new BorderPane();
        scene = new Scene(root, 900, 600);

        nuevoExpedienteBtn = new Button("Nuevo expediente");
        usuariosBtn = new Button("Usuarios");
        salirBtn = new Button("Salir");
        userLabel = new Label();

        HBox topBar = new HBox(10);
        topBar.setPadding(new Insets(10));

        topBar.getChildren().addAll(
                nuevoExpedienteBtn,
                usuariosBtn,
                salirBtn,
                userLabel
        );

        root.setTop(topBar);
    }

    public Scene getScene() {
        return scene;
    }

    public BorderPane getRoot() {
        return root;
    }

    public Button getNuevoExpedienteBtn() {
        return nuevoExpedienteBtn;
    }

    public Button getUsuariosBtn() {
        return usuariosBtn;
    }

    public Button getSalirBtn() {
        return salirBtn;
    }

    public Label getUserLabel() {
        return userLabel;
    }
}
