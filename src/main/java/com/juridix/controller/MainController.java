package com.juridix.controller;

import com.juridix.model.Expediente;
import com.juridix.model.EstadoExpediente;
import com.juridix.service.ExpedienteService;
import com.juridix.seguridad.SesionUsuario;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public class MainController {

    private final Stage stage;
    private Scene scene;

    // Servicios
    private ExpedienteService expedienteService;

    // Componentes del formulario
    private TextField txtNumero;
    private TextField txtCaratula;
    private TextField txtCliente;
    private TextField txtDemandado;
    private ComboBox<String> cmbFuero;
    private TextField txtJuzgado;
    private TextField txtSecretaria;
    private ComboBox<EstadoExpediente> cmbEstado;
    private DatePicker dpFechaInicio;
    private TextField txtMontoEstimado;
    private TextArea txtObservaciones;

    // Componentes de la tabla
    private TableView<Expediente> tablaExpedientes;
    private ObservableList<Expediente> listaExpedientes;

    // Componentes de búsqueda
    private TextField txtBuscar;
    private ComboBox<EstadoExpediente> cmbFiltroEstado;

    // Estado
    private Expediente expedienteSeleccionado;

    public MainController(Stage stage) {
        this.stage = stage;
        this.expedienteService = new ExpedienteService();
        this.listaExpedientes = FXCollections.observableArrayList();
        inicializarUI();
        cargarExpedientes();
    }

    private void inicializarUI() {
        BorderPane root = new BorderPane();
        root.setPadding(new Insets(10));

        // Top: Barra de título y usuario
        root.setTop(crearBarraSuperior());

        // Center: Panel principal con formulario y tabla
        root.setCenter(crearPanelPrincipal());

        // Bottom: Barra de estado
        root.setBottom(crearBarraEstado());

        scene = new Scene(root, 1200, 700);

        // Aplicar estilo si existe
        try {
            scene.getStylesheets().add(
                    getClass().getResource("/com/juridix/style/style.css").toExternalForm()
            );
        } catch (Exception e) {
            // Si no existe el CSS, continuar sin estilos
        }
    }

    private HBox crearBarraSuperior() {
        HBox barra = new HBox(15);
        barra.setPadding(new Insets(10));
        barra.setAlignment(Pos.CENTER_LEFT);
        barra.setStyle("-fx-background-color: #2c3e50; -fx-text-fill: white;");

        Label lblTitulo = new Label("JURIDIX - Gestión de Expedientes");
        lblTitulo.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: white;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label lblUsuario = new Label("Usuario: " + SesionUsuario.getUsuarioActual().getUsername());
        lblUsuario.setStyle("-fx-text-fill: white;");

        Button btnCerrarSesion = new Button("Cerrar Sesión");
        btnCerrarSesion.setOnAction(e -> cerrarSesion());

        barra.getChildren().addAll(lblTitulo, spacer, lblUsuario, btnCerrarSesion);
        return barra;
    }

    private VBox crearPanelPrincipal() {
        VBox panel = new VBox(10);
        panel.setPadding(new Insets(10));

        // Pestañas
        TabPane tabPane = new TabPane();

        Tab tabExpedientes = new Tab("Expedientes");
        tabExpedientes.setClosable(false);
        tabExpedientes.setContent(crearPanelExpedientes());

        Tab tabEstadisticas = new Tab("Estadísticas");
        tabEstadisticas.setClosable(false);
        tabEstadisticas.setContent(crearPanelEstadisticas());

        tabPane.getTabs().addAll(tabExpedientes, tabEstadisticas);

        panel.getChildren().add(tabPane);
        VBox.setVgrow(tabPane, Priority.ALWAYS);

        return panel;
    }

    private SplitPane crearPanelExpedientes() {
        SplitPane splitPane = new SplitPane();
        splitPane.setDividerPositions(0.35);

        // Panel izquierdo: Formulario
        VBox panelFormulario = crearFormulario();

        // Panel derecho: Tabla y búsqueda
        VBox panelTabla = crearPanelTabla();

        splitPane.getItems().addAll(panelFormulario, panelTabla);

        return splitPane;
    }

    private VBox crearFormulario() {
        VBox form = new VBox(10);
        form.setPadding(new Insets(10));
        form.setStyle("-fx-background-color: #ecf0f1; -fx-border-color: #bdc3c7; -fx-border-width: 1;");

        Label lblTitulo = new Label("Datos del Expediente");
        lblTitulo.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

        // Campos del formulario
        txtNumero = new TextField();
        txtCaratula = new TextField();
        txtCliente = new TextField();
        txtDemandado = new TextField();

        cmbFuero = new ComboBox<>();
        cmbFuero.setItems(FXCollections.observableArrayList(
                "Civil", "Penal", "Laboral", "Comercial", "Familia", "Contencioso Administrativo"
        ));
        cmbFuero.setMaxWidth(Double.MAX_VALUE);

        txtJuzgado = new TextField();
        txtSecretaria = new TextField();

        cmbEstado = new ComboBox<>();
        cmbEstado.setItems(FXCollections.observableArrayList(EstadoExpediente.values()));
        cmbEstado.setValue(EstadoExpediente.ACTIVO);
        cmbEstado.setMaxWidth(Double.MAX_VALUE);

        dpFechaInicio = new DatePicker(LocalDate.now());
        dpFechaInicio.setMaxWidth(Double.MAX_VALUE);

        txtMontoEstimado = new TextField();

        txtObservaciones = new TextArea();
        txtObservaciones.setPrefRowCount(3);

        // Agregar campos al formulario con labels
        form.getChildren().addAll(
                lblTitulo,
                new Separator(),
                new Label("Número *:"), txtNumero,
                new Label("Carátula *:"), txtCaratula,
                new Label("Cliente *:"), txtCliente,
                new Label("Demandado:"), txtDemandado,
                new Label("Fuero:"), cmbFuero,
                new Label("Juzgado:"), txtJuzgado,
                new Label("Secretaría:"), txtSecretaria,
                new Label("Estado *:"), cmbEstado,
                new Label("Fecha Inicio *:"), dpFechaInicio,
                new Label("Monto Estimado:"), txtMontoEstimado,
                new Label("Observaciones:"), txtObservaciones,
                crearBotonesFormulario()
        );

        ScrollPane scroll = new ScrollPane(form);
        scroll.setFitToWidth(true);

        VBox container = new VBox(scroll);
        VBox.setVgrow(scroll, Priority.ALWAYS);

        return container;
    }

    private HBox crearBotonesFormulario() {
        HBox botones = new HBox(10);
        botones.setAlignment(Pos.CENTER);
        botones.setPadding(new Insets(10, 0, 0, 0));

        Button btnGuardar = new Button("Guardar");
        btnGuardar.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white;");
        btnGuardar.setOnAction(e -> guardarExpediente());

        Button btnNuevo = new Button("Nuevo");
        btnNuevo.setOnAction(e -> limpiarFormulario());

        Button btnEliminar = new Button("Eliminar");
        btnEliminar.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white;");
        btnEliminar.setOnAction(e -> eliminarExpediente());

        botones.getChildren().addAll(btnGuardar, btnNuevo, btnEliminar);
        return botones;
    }

    private VBox crearPanelTabla() {
        VBox panel = new VBox(10);
        panel.setPadding(new Insets(10));

        // Panel de búsqueda
        HBox panelBusqueda = new HBox(10);
        panelBusqueda.setAlignment(Pos.CENTER_LEFT);

        Label lblBuscar = new Label("Buscar:");
        txtBuscar = new TextField();
        txtBuscar.setPromptText("Número, cliente...");
        txtBuscar.setPrefWidth(200);
        txtBuscar.textProperty().addListener((obs, old, val) -> buscarExpedientes());

        Label lblFiltro = new Label("Estado:");
        cmbFiltroEstado = new ComboBox<>();
        cmbFiltroEstado.setItems(FXCollections.observableArrayList(EstadoExpediente.values()));
        cmbFiltroEstado.setPromptText("Todos");
        cmbFiltroEstado.setOnAction(e -> buscarExpedientes());

        Button btnLimpiarFiltro = new Button("Limpiar");
        btnLimpiarFiltro.setOnAction(e -> {
            txtBuscar.clear();
            cmbFiltroEstado.setValue(null);
            cargarExpedientes();
        });

        Button btnActualizar = new Button("Actualizar");
        btnActualizar.setOnAction(e -> cargarExpedientes());

        panelBusqueda.getChildren().addAll(
                lblBuscar, txtBuscar, lblFiltro, cmbFiltroEstado,
                btnLimpiarFiltro, btnActualizar
        );

        // Tabla
        tablaExpedientes = new TableView<>();
        tablaExpedientes.setItems(listaExpedientes);

        TableColumn<Expediente, String> colNumero = new TableColumn<>("Número");
        colNumero.setCellValueFactory(new PropertyValueFactory<>("numero"));
        colNumero.setPrefWidth(120);

        TableColumn<Expediente, String> colCaratula = new TableColumn<>("Carátula");
        colCaratula.setCellValueFactory(new PropertyValueFactory<>("caratula"));
        colCaratula.setPrefWidth(300);

        TableColumn<Expediente, String> colCliente = new TableColumn<>("Cliente");
        colCliente.setCellValueFactory(new PropertyValueFactory<>("cliente"));
        colCliente.setPrefWidth(150);

        TableColumn<Expediente, EstadoExpediente> colEstado = new TableColumn<>("Estado");
        colEstado.setCellValueFactory(new PropertyValueFactory<>("estado"));
        colEstado.setPrefWidth(100);

        TableColumn<Expediente, LocalDate> colFecha = new TableColumn<>("Fecha Inicio");
        colFecha.setCellValueFactory(new PropertyValueFactory<>("fechaInicio"));
        colFecha.setPrefWidth(120);

        tablaExpedientes.getColumns().addAll(colNumero, colCaratula, colCliente, colEstado, colFecha);

        // Listener para selección
        tablaExpedientes.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldVal, newVal) -> {
                    if (newVal != null) {
                        cargarExpedienteEnFormulario(newVal);
                    }
                }
        );

        panel.getChildren().addAll(panelBusqueda, tablaExpedientes);
        VBox.setVgrow(tablaExpedientes, Priority.ALWAYS);

        return panel;
    }

    private VBox crearPanelEstadisticas() {
        VBox panel = new VBox(20);
        panel.setPadding(new Insets(20));
        panel.setAlignment(Pos.CENTER);

        Label lblTitulo = new Label("Estadísticas de Expedientes");
        lblTitulo.setStyle("-fx-font-size: 20px; -fx-font-weight: bold;");

        GridPane grid = new GridPane();
        grid.setHgap(20);
        grid.setVgap(15);
        grid.setAlignment(Pos.CENTER);

        try {
            ExpedienteService.EstadisticasExpedientes stats = expedienteService.obtenerEstadisticas();

            grid.add(crearTarjetaEstadistica("Total", String.valueOf(stats.getTotal()), "#3498db"), 0, 0);
            grid.add(crearTarjetaEstadistica("Activos", String.valueOf(stats.getActivos()), "#27ae60"), 1, 0);
            grid.add(crearTarjetaEstadistica("Archivados", String.valueOf(stats.getArchivados()), "#95a5a6"), 0, 1);
            grid.add(crearTarjetaEstadistica("Finalizados", String.valueOf(stats.getFinalizados()), "#2ecc71"), 1, 1);

        } catch (SQLException e) {
            Label lblError = new Label("Error al cargar estadísticas: " + e.getMessage());
            lblError.setStyle("-fx-text-fill: red;");
            panel.getChildren().add(lblError);
        }

        panel.getChildren().addAll(lblTitulo, grid);
        return panel;
    }

    private VBox crearTarjetaEstadistica(String titulo, String valor, String color) {
        VBox tarjeta = new VBox(10);
        tarjeta.setAlignment(Pos.CENTER);
        tarjeta.setPadding(new Insets(20));
        tarjeta.setStyle("-fx-background-color: " + color + "; -fx-background-radius: 10;");
        tarjeta.setPrefSize(150, 100);

        Label lblTitulo = new Label(titulo);
        lblTitulo.setStyle("-fx-text-fill: white; -fx-font-size: 14px;");

        Label lblValor = new Label(valor);
        lblValor.setStyle("-fx-text-fill: white; -fx-font-size: 32px; -fx-font-weight: bold;");

        tarjeta.getChildren().addAll(lblTitulo, lblValor);
        return tarjeta;
    }

    private HBox crearBarraEstado() {
        HBox barra = new HBox();
        barra.setPadding(new Insets(5));
        barra.setStyle("-fx-background-color: #ecf0f1;");

        Label lblEstado = new Label("Listo");
        barra.getChildren().add(lblEstado);

        return barra;
    }

    // ==================== OPERACIONES CRUD ====================

    private void guardarExpediente() {
        try {
            if (!validarCampos()) {
                return;
            }

            Expediente expediente;

            if (expedienteSeleccionado != null && expedienteSeleccionado.getId() != null) {
                expediente = expedienteSeleccionado;
            } else {
                expediente = new Expediente();
                expediente.setCreadorId(SesionUsuario.getUsuarioActual().getId());
            }

            expediente.setNumero(txtNumero.getText().trim().toUpperCase());
            expediente.setCaratula(txtCaratula.getText().trim());
            expediente.setCliente(txtCliente.getText().trim());
            expediente.setDemandado(txtDemandado.getText().trim());
            expediente.setFuero(cmbFuero.getValue());
            expediente.setJuzgado(txtJuzgado.getText().trim());
            expediente.setSecretaria(txtSecretaria.getText().trim());
            expediente.setEstado(cmbEstado.getValue());
            expediente.setFechaInicio(dpFechaInicio.getValue());

            if (!txtMontoEstimado.getText().trim().isEmpty()) {
                try {
                    expediente.setMontoEstimado(Double.parseDouble(txtMontoEstimado.getText().trim()));
                } catch (NumberFormatException e) {
                    mostrarError("El monto estimado debe ser un número válido");
                    return;
                }
            }

            expediente.setObservaciones(txtObservaciones.getText().trim());

            if (expediente.getId() == null) {
                expedienteService.crearExpediente(expediente);
                mostrarInfo("Expediente creado correctamente");
            } else {
                expedienteService.actualizarExpediente(expediente);
                mostrarInfo("Expediente actualizado correctamente");
            }

            limpiarFormulario();
            cargarExpedientes();

        } catch (IllegalArgumentException e) {
            mostrarAdvertencia(e.getMessage());
        } catch (SQLException e) {
            mostrarError("Error de base de datos: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void eliminarExpediente() {
        if (expedienteSeleccionado == null) {
            mostrarAdvertencia("Seleccione un expediente para eliminar");
            return;
        }

        Alert confirmacion = new Alert(Alert.AlertType.CONFIRMATION);
        confirmacion.setTitle("Confirmar eliminación");
        confirmacion.setHeaderText("¿Está seguro de eliminar este expediente?");
        confirmacion.setContentText(expedienteSeleccionado.getNumero() + " - " +
                expedienteSeleccionado.getCaratula());

        Optional<ButtonType> resultado = confirmacion.showAndWait();
        if (resultado.isPresent() && resultado.get() == ButtonType.OK) {
            try {
                expedienteService.eliminarExpediente(expedienteSeleccionado.getId());
                mostrarInfo("Expediente eliminado correctamente");
                limpiarFormulario();
                cargarExpedientes();
            } catch (SQLException e) {
                mostrarError("No se pudo eliminar el expediente: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    private void cargarExpedientes() {
        try {
            List<Expediente> expedientes = expedienteService.listarTodos();
            listaExpedientes.clear();
            listaExpedientes.addAll(expedientes);
        } catch (SQLException e) {
            mostrarError("Error al cargar expedientes: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void buscarExpedientes() {
        try {
            String textoBusqueda = txtBuscar.getText().trim();
            EstadoExpediente estadoFiltro = cmbFiltroEstado.getValue();

            List<Expediente> resultados;

            if (textoBusqueda.isEmpty() && estadoFiltro == null) {
                resultados = expedienteService.listarTodos();
            } else {
                resultados = expedienteService.buscarPorCriterios(
                        textoBusqueda.isEmpty() ? null : textoBusqueda,
                        textoBusqueda.isEmpty() ? null : textoBusqueda,
                        estadoFiltro,
                        null
                );
            }

            listaExpedientes.clear();
            listaExpedientes.addAll(resultados);

        } catch (SQLException e) {
            mostrarError("Error al buscar: " + e.getMessage());
        }
    }

    private void cargarExpedienteEnFormulario(Expediente exp) {
        expedienteSeleccionado = exp;
        txtNumero.setText(exp.getNumero());
        txtCaratula.setText(exp.getCaratula());
        txtCliente.setText(exp.getCliente());
        txtDemandado.setText(exp.getDemandado());
        cmbFuero.setValue(exp.getFuero());
        txtJuzgado.setText(exp.getJuzgado());
        txtSecretaria.setText(exp.getSecretaria());
        cmbEstado.setValue(exp.getEstado());
        dpFechaInicio.setValue(exp.getFechaInicio());

        if (exp.getMontoEstimado() != null) {
            txtMontoEstimado.setText(exp.getMontoEstimado().toString());
        } else {
            txtMontoEstimado.clear();
        }

        txtObservaciones.setText(exp.getObservaciones());
    }

    private void limpiarFormulario() {
        txtNumero.clear();
        txtCaratula.clear();
        txtCliente.clear();
        txtDemandado.clear();
        cmbFuero.setValue(null);
        txtJuzgado.clear();
        txtSecretaria.clear();
        cmbEstado.setValue(EstadoExpediente.ACTIVO);
        dpFechaInicio.setValue(LocalDate.now());
        txtMontoEstimado.clear();
        txtObservaciones.clear();
        expedienteSeleccionado = null;
        tablaExpedientes.getSelectionModel().clearSelection();
    }

    private boolean validarCampos() {
        if (txtNumero.getText().trim().isEmpty()) {
            mostrarAdvertencia("El número de expediente es obligatorio");
            txtNumero.requestFocus();
            return false;
        }

        if (txtCaratula.getText().trim().isEmpty()) {
            mostrarAdvertencia("La carátula es obligatoria");
            txtCaratula.requestFocus();
            return false;
        }

        if (txtCliente.getText().trim().isEmpty()) {
            mostrarAdvertencia("El nombre del cliente es obligatorio");
            txtCliente.requestFocus();
            return false;
        }

        if (dpFechaInicio.getValue() == null) {
            mostrarAdvertencia("La fecha de inicio es obligatoria");
            dpFechaInicio.requestFocus();
            return false;
        }

        return true;
    }

    // ==================== UTILIDADES ====================

    private void cerrarSesion() {
        Alert confirmacion = new Alert(Alert.AlertType.CONFIRMATION);
        confirmacion.setTitle("Cerrar Sesión");
        confirmacion.setHeaderText("¿Está seguro que desea cerrar sesión?");

        Optional<ButtonType> resultado = confirmacion.showAndWait();
        if (resultado.isPresent() && resultado.get() == ButtonType.OK) {
            SesionUsuario.cerrarSesion();
            LoginController loginController = new LoginController(stage);
            loginController.mostrar();
        }
    }

    private void mostrarError(String mensaje) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }

    private void mostrarAdvertencia(String mensaje) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Advertencia");
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }

    private void mostrarInfo(String mensaje) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Información");
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }

    public void mostrar() {
        stage.setScene(scene);
        stage.setTitle("Juridix - Sistema de Gestión de Expedientes");
        stage.setMaximized(true);
        stage.show();
    }
}