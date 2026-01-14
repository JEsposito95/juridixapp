package com.juridix.controller;

import com.juridix.model.Expediente;
import com.juridix.model.RolUsuario;
import com.juridix.model.Usuario;
import com.juridix.seguridad.SesionUsuario;
import com.juridix.service.ExpedienteService;
import com.juridix.ui.views.MainView;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;

public class MainController {

    private final MainView view;
    private final Stage stage;

    private TableView<Expediente> table;
    private ObservableList<Expediente> data;

    public MainController(Stage stage) {
        this.stage = stage;
        this.view = new MainView();
        init();
    }

    private void init() {

        Usuario u = SesionUsuario.getUsuarioActual();
        if (u == null) {
            volverALogin();
            return;
        }

        view.getUserLabel().setText(
                "Usuario: " + u.getUsername() + " (" + u.getRol() + ")"
        );

        // Control por rol
        if (u.getRol() != RolUsuario.ADMIN) {
            view.getUsuariosBtn().setVisible(false);
            view.getUsuariosBtn().setManaged(false);
        }

        view.getNuevoExpedienteBtn()
                .setOnAction(e -> abrirDialogNuevoExpediente());

        view.getSalirBtn()
                .setOnAction(e -> logout());

        table = crearTabla();
        cargarExpedientes();

        view.getRoot().setCenter(table);
    }

    private TableView<Expediente> crearTabla() {

        TableView<Expediente> table = new TableView<>();

        TableColumn<Expediente, String> colNumero =
                new TableColumn<>("Número");
        colNumero.setCellValueFactory(
                new PropertyValueFactory<>("numero")
        );

        TableColumn<Expediente, String> colCaratula =
                new TableColumn<>("Carátula");
        colCaratula.setCellValueFactory(
                new PropertyValueFactory<>("caratula")
        );

        TableColumn<Expediente, String> colCliente =
                new TableColumn<>("Cliente");
        colCliente.setCellValueFactory(
                new PropertyValueFactory<>("cliente")
        );

        TableColumn<Expediente, String> colEstado =
                new TableColumn<>("Estado");
        colEstado.setCellValueFactory(
                new PropertyValueFactory<>("estado")
        );

        table.getColumns().addAll(
                colNumero, colCaratula, colCliente, colEstado
        );

        table.setColumnResizePolicy(
                TableView.CONSTRAINED_RESIZE_POLICY
        );

        return table;
    }

    private void abrirDialogNuevoExpediente() {

        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Nuevo expediente");

        ButtonType guardarBtn =
                new ButtonType("Guardar", ButtonBar.ButtonData.OK_DONE);

        dialog.getDialogPane()
                .getButtonTypes()
                .addAll(guardarBtn, ButtonType.CANCEL);

        TextField numeroField = new TextField();
        TextField caratulaField = new TextField();
        TextField clienteField = new TextField();

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);

        grid.add(new Label("Número:"), 0, 0);
        grid.add(numeroField, 1, 0);

        grid.add(new Label("Carátula:"), 0, 1);
        grid.add(caratulaField, 1, 1);

        grid.add(new Label("Cliente:"), 0, 2);
        grid.add(clienteField, 1, 2);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(button -> {
            if (button == guardarBtn) {

                Usuario u = SesionUsuario.getUsuarioActual();

                ExpedienteService service = new ExpedienteService();
                service.crear(
                        numeroField.getText(),
                        caratulaField.getText(),
                        clienteField.getText(),
                        u
                );

                cargarExpedientes();
            }
            return null;
        });

        dialog.showAndWait();
    }

    private void cargarExpedientes() {
        ExpedienteService service = new ExpedienteService();
        data = FXCollections.observableArrayList(
                service.listar()
        );
        table.setItems(data);
    }

    private void logout() {
        SesionUsuario.cerrarSesion();
        volverALogin();
    }

    private void volverALogin() {
        LoginController login = new LoginController(stage);
        login.mostrar();
    }

    public void mostrar() {
        stage.setScene(view.getScene());
        stage.setTitle("Juridix");
        stage.show();
    }
}
