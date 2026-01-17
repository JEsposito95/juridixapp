package com.juridix.controller;

import com.juridix.db.ExpedienteDAO;
import com.juridix.model.*;
import com.juridix.service.*;
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
import javafx.stage.Modality;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.ColumnConstraints;

import java.io.File;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

public class MainController {

    private final Stage stage;
    private Scene scene;

    // Servicios
    private ExpedienteService expedienteService;
    private MovimientoService movimientoService;
    private EventoAgendaService agendaService;

    // Componentes del formulario de expedientes
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

    // Componentes de la tabla de expedientes
    private TableView<Expediente> tablaExpedientes;
    private ObservableList<Expediente> listaExpedientes;

    // Componentes de búsqueda
    private TextField txtBuscar;
    private ComboBox<EstadoExpediente> cmbFiltroEstado;

    // Estado
    private Expediente expedienteSeleccionado;

    // Dashboard labels
    private Label lblTotalExpedientes;
    private Label lblExpedientesActivos;
    private Label lblEventosHoy;
    private Label lblEventosSemana;
    private ListView<String> listProximosEventos;

    // Servicios (agregar junto a los otros servicios)
    private ClienteService clienteService;
    private DocumentoClienteService documentoClienteService;

    // Tabla de clientes
    private TableView<Cliente> tablaClientes;
    private ObservableList<Cliente> listaClientes;
    private TextField txtBuscarCliente;

    // Cliente seleccionado para vista detallada
    private Cliente clienteSeleccionado;

    public MainController(Stage stage) {
        this.stage = stage;
        this.expedienteService = new ExpedienteService();
        this.movimientoService = new MovimientoService();
        this.agendaService = new EventoAgendaService();

        this.clienteService = new ClienteService();
        this.documentoClienteService = new DocumentoClienteService();

        this.listaExpedientes = FXCollections.observableArrayList();
        this.listaClientes = FXCollections.observableArrayList();
        inicializarUI();
        cargarDashboard();
        cargarExpedientes();
    }

    private void inicializarUI() {
        BorderPane root = new BorderPane();
        root.setPadding(new Insets(10));

        // Top: Barra superior
        root.setTop(crearBarraSuperior());

        // Center: Panel principal con pestañas
        root.setCenter(crearPanelPrincipal());

        // Bottom: Barra de estado
        root.setBottom(crearBarraEstado());

        scene = new Scene(root, 1200, 700);
    }

    private HBox crearBarraSuperior() {
        HBox barra = new HBox(15);
        barra.setPadding(new Insets(10));
        barra.setAlignment(Pos.CENTER_LEFT);
        barra.setStyle("-fx-background-color: #2c3e50;");

        Label lblTitulo = new Label("JURIDIX - Gestión de Estudios Jurídicos");
        lblTitulo.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: white;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label lblUsuario = new Label("👤 " + SesionUsuario.getUsuarioActual().getUsername());
        lblUsuario.setStyle("-fx-text-fill: white; -fx-font-size: 14px;");

        Button btnCerrarSesion = new Button("Cerrar Sesión");
        btnCerrarSesion.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white;");
        btnCerrarSesion.setOnAction(e -> cerrarSesion());

        barra.getChildren().addAll(lblTitulo, spacer, lblUsuario, btnCerrarSesion);
        return barra;
    }

    private VBox crearPanelPrincipal() {
        VBox panel = new VBox(10);
        panel.setPadding(new Insets(10));

        TabPane tabPane = new TabPane();

        // Pestaña Dashboard
        Tab tabDashboard = new Tab("📊 Dashboard");
        tabDashboard.setClosable(false);
        tabDashboard.setContent(crearPanelDashboard());

        // Pestaña Expedientes
        Tab tabExpedientes = new Tab("📁 Expedientes");
        tabExpedientes.setClosable(false);
        tabExpedientes.setContent(crearPanelExpedientes());

        // Pestaña Clientes ← VERIFICAR QUE ESTÉ ESTA LÍNEA
        Tab tabClientes = new Tab("👥 Clientes");
        tabClientes.setClosable(false);
        tabClientes.setContent(crearPanelClientes());

        // Pestaña Agenda
        Tab tabAgenda = new Tab("📅 Agenda");
        tabAgenda.setClosable(false);
        tabAgenda.setContent(crearPanelAgenda());

        // IMPORTANTE: Agregar TODAS las pestañas
        tabPane.getTabs().addAll(tabDashboard, tabExpedientes, tabClientes, tabAgenda);

        panel.getChildren().add(tabPane);
        VBox.setVgrow(tabPane, Priority.ALWAYS);

        return panel;
    }

    // ==================== DASHBOARD ====================

    private VBox crearPanelDashboard() {
        VBox panel = new VBox(20);
        panel.setPadding(new Insets(20));

        Label titulo = new Label("Panel de Control");
        titulo.setStyle("-fx-font-size: 24px; -fx-font-weight: bold;");

        // Tarjetas de estadísticas
        HBox tarjetas = new HBox(20);
        tarjetas.setAlignment(Pos.CENTER);

        // Crear tarjetas y guardar referencias a los labels de valores
        VBox tarjetaExpedientes = new VBox(5);
        tarjetaExpedientes.setAlignment(Pos.CENTER);
        tarjetaExpedientes.setPadding(new Insets(10));
        tarjetaExpedientes.setPrefSize(200, 120);
        tarjetaExpedientes.setStyle("-fx-background-color: white; -fx-border-color: #3498db; -fx-border-width: 3; -fx-border-radius: 10; -fx-background-radius: 10;");

        Label lblTituloExp = new Label("Total Expedientes");
        lblTituloExp.setStyle("-fx-font-size: 14px; -fx-text-fill: #7f8c8d;");

        lblTotalExpedientes = new Label("0");
        lblTotalExpedientes.setStyle("-fx-font-size: 36px; -fx-font-weight: bold; -fx-text-fill: #3498db;");

        tarjetaExpedientes.getChildren().addAll(lblTituloExp, lblTotalExpedientes);

        // Tarjeta Activos
        VBox tarjetaActivos = new VBox(5);
        tarjetaActivos.setAlignment(Pos.CENTER);
        tarjetaActivos.setPadding(new Insets(10));
        tarjetaActivos.setPrefSize(200, 120);
        tarjetaActivos.setStyle("-fx-background-color: white; -fx-border-color: #27ae60; -fx-border-width: 3; -fx-border-radius: 10; -fx-background-radius: 10;");

        Label lblTituloAct = new Label("Activos");
        lblTituloAct.setStyle("-fx-font-size: 14px; -fx-text-fill: #7f8c8d;");

        lblExpedientesActivos = new Label("0");
        lblExpedientesActivos.setStyle("-fx-font-size: 36px; -fx-font-weight: bold; -fx-text-fill: #27ae60;");

        tarjetaActivos.getChildren().addAll(lblTituloAct, lblExpedientesActivos);

        // Tarjeta Eventos Hoy
        VBox tarjetaEventosHoy = new VBox(5);
        tarjetaEventosHoy.setAlignment(Pos.CENTER);
        tarjetaEventosHoy.setPadding(new Insets(10));
        tarjetaEventosHoy.setPrefSize(200, 120);
        tarjetaEventosHoy.setStyle("-fx-background-color: white; -fx-border-color: #f39c12; -fx-border-width: 3; -fx-border-radius: 10; -fx-background-radius: 10;");

        Label lblTituloHoy = new Label("Eventos Hoy");
        lblTituloHoy.setStyle("-fx-font-size: 14px; -fx-text-fill: #7f8c8d;");

        lblEventosHoy = new Label("0");
        lblEventosHoy.setStyle("-fx-font-size: 36px; -fx-font-weight: bold; -fx-text-fill: #f39c12;");

        tarjetaEventosHoy.getChildren().addAll(lblTituloHoy, lblEventosHoy);

        // Tarjeta Eventos Semana
        VBox tarjetaEventosSemana = new VBox(5);
        tarjetaEventosSemana.setAlignment(Pos.CENTER);
        tarjetaEventosSemana.setPadding(new Insets(10));
        tarjetaEventosSemana.setPrefSize(200, 120);
        tarjetaEventosSemana.setStyle("-fx-background-color: white; -fx-border-color: #9b59b6; -fx-border-width: 3; -fx-border-radius: 10; -fx-background-radius: 10;");

        Label lblTituloSemana = new Label("Esta Semana");
        lblTituloSemana.setStyle("-fx-font-size: 14px; -fx-text-fill: #7f8c8d;");

        lblEventosSemana = new Label("0");
        lblEventosSemana.setStyle("-fx-font-size: 36px; -fx-font-weight: bold; -fx-text-fill: #9b59b6;");

        tarjetaEventosSemana.getChildren().addAll(lblTituloSemana, lblEventosSemana);

        tarjetas.getChildren().addAll(tarjetaExpedientes, tarjetaActivos, tarjetaEventosHoy, tarjetaEventosSemana);

        // Panel de próximos eventos
        VBox panelEventos = new VBox(10);
        panelEventos.setPadding(new Insets(15));
        panelEventos.setStyle("-fx-background-color: white; -fx-border-color: #bdc3c7; -fx-border-width: 1; -fx-border-radius: 5;");

        Label lblProximos = new Label("📅 Próximos Eventos");
        lblProximos.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

        listProximosEventos = new ListView<>();
        listProximosEventos.setPrefHeight(200);

        Button btnActualizarDashboard = new Button("🔄 Actualizar");
        btnActualizarDashboard.setOnAction(e -> cargarDashboard());

        panelEventos.getChildren().addAll(lblProximos, listProximosEventos, btnActualizarDashboard);

        panel.getChildren().addAll(titulo, tarjetas, panelEventos);
        return panel;
    }

    private VBox crearTarjetaEstadistica(String titulo, String valorInicial, String color) {
        VBox contenedor = new VBox(5);
        contenedor.setAlignment(Pos.CENTER);
        contenedor.setPadding(new Insets(10));
        contenedor.setPrefSize(200, 120);
        contenedor.setStyle("-fx-background-color: white; -fx-border-color: " + color + "; -fx-border-width: 3; -fx-border-radius: 10; -fx-background-radius: 10;");

        Label lblTitulo = new Label(titulo);
        lblTitulo.setStyle("-fx-font-size: 14px; -fx-text-fill: #7f8c8d;");

        VBox valorBox = new VBox();
        valorBox.setAlignment(Pos.CENTER);

        Label lblValor = new Label(valorInicial);
        lblValor.setStyle("-fx-font-size: 36px; -fx-font-weight: bold; -fx-text-fill: " + color + ";");

        valorBox.getChildren().add(lblValor);
        contenedor.getChildren().addAll(lblTitulo, valorBox);

        return contenedor;
    }

    private void cargarDashboard() {
        try {
            Integer usuarioId = SesionUsuario.getUsuarioActual().getId();

            // Estadísticas de expedientes
            ExpedienteService.EstadisticasExpedientes statsExp = expedienteService.obtenerEstadisticas();
            lblTotalExpedientes.setText(String.valueOf(statsExp.getTotal()));
            lblExpedientesActivos.setText(String.valueOf(statsExp.getActivos()));

            // Estadísticas de agenda
            List<EventoAgenda> eventosHoy = agendaService.listarHoy(usuarioId);
            List<EventoAgenda> eventosSemana = agendaService.listarEstaSemana(usuarioId);

            lblEventosHoy.setText(String.valueOf(eventosHoy.size()));
            lblEventosSemana.setText(String.valueOf(eventosSemana.size()));

            // Cargar próximos eventos
            List<EventoAgenda> proximos = agendaService.listarProximos(usuarioId, 7);
            ObservableList<String> eventosTexto = FXCollections.observableArrayList();

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
            for (EventoAgenda evento : proximos) {
                String texto = String.format("%s - %s (%s)",
                        evento.getFechaHora().format(formatter),
                        evento.getTitulo(),
                        evento.getTipo().getDisplayName()
                );
                eventosTexto.add(texto);
            }

            if (eventosTexto.isEmpty()) {
                eventosTexto.add("No hay eventos próximos");
            }

            listProximosEventos.setItems(eventosTexto);

        } catch (SQLException e) {
            mostrarError("Error al cargar dashboard: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ==================== EXPEDIENTES ====================

    private SplitPane crearPanelExpedientes() {
        SplitPane splitPane = new SplitPane();
        splitPane.setDividerPositions(0.35);

        VBox panelFormulario = crearFormularioExpediente();
        VBox panelTabla = crearPanelTablaExpedientes();

        splitPane.getItems().addAll(panelFormulario, panelTabla);

        return splitPane;
    }

    private VBox crearFormularioExpediente() {
        VBox form = new VBox(10);
        form.setPadding(new Insets(10));
        form.setStyle("-fx-background-color: #ecf0f1; -fx-border-color: #bdc3c7; -fx-border-width: 1;");

        Label lblTitulo = new Label("Datos del Expediente");
        lblTitulo.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

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
                crearBotonesFormularioExpediente()
        );

        ScrollPane scroll = new ScrollPane(form);
        scroll.setFitToWidth(true);

        VBox container = new VBox(scroll);
        VBox.setVgrow(scroll, Priority.ALWAYS);

        return container;
    }

    private HBox crearBotonesFormularioExpediente() {
        HBox botones = new HBox(10);
        botones.setAlignment(Pos.CENTER);
        botones.setPadding(new Insets(10, 0, 0, 0));

        Button btnGuardar = new Button("💾 Guardar");
        btnGuardar.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-font-weight: bold;");
        btnGuardar.setOnAction(e -> guardarExpediente());

        Button btnNuevo = new Button("📄 Nuevo");
        btnNuevo.setOnAction(e -> limpiarFormularioExpediente());

        Button btnEliminar = new Button("🗑️ Eliminar");
        btnEliminar.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-font-weight: bold;");
        btnEliminar.setOnAction(e -> eliminarExpediente());

        Button btnMovimientos = new Button("📋 Ver Movimientos");
        btnMovimientos.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-font-weight: bold;");
        btnMovimientos.setOnAction(e -> abrirVentanaMovimientos());

        botones.getChildren().addAll(btnGuardar, btnNuevo, btnEliminar, btnMovimientos);
        return botones;
    }

    private VBox crearPanelTablaExpedientes() {
        VBox panel = new VBox(10);
        panel.setPadding(new Insets(10));

        HBox panelBusqueda = new HBox(10);
        panelBusqueda.setAlignment(Pos.CENTER_LEFT);

        Label lblBuscar = new Label("🔍 Buscar:");
        txtBuscar = new TextField();
        txtBuscar.setPromptText("Número, cliente...");
        txtBuscar.setPrefWidth(200);
        txtBuscar.textProperty().addListener((obs, old, val) -> buscarExpedientes());

        Label lblFiltro = new Label("Estado:");
        cmbFiltroEstado = new ComboBox<>();
        cmbFiltroEstado.setItems(FXCollections.observableArrayList(EstadoExpediente.values()));
        cmbFiltroEstado.setPromptText("Todos");
        cmbFiltroEstado.setOnAction(e -> buscarExpedientes());

        Button btnLimpiarFiltro = new Button("🔄 Limpiar");
        btnLimpiarFiltro.setOnAction(e -> {
            txtBuscar.clear();
            cmbFiltroEstado.setValue(null);
            cargarExpedientes();
        });

        panelBusqueda.getChildren().addAll(lblBuscar, txtBuscar, lblFiltro, cmbFiltroEstado, btnLimpiarFiltro);

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

    // ==================== VENTANA DE MOVIMIENTOS ====================

    private void abrirVentanaMovimientos() {
        if (expedienteSeleccionado == null) {
            mostrarAdvertencia("Seleccione un expediente para ver sus movimientos");
            return;
        }

        Stage ventanaMovimientos = new Stage();
        ventanaMovimientos.initModality(Modality.APPLICATION_MODAL);
        ventanaMovimientos.setTitle("Movimientos - " + expedienteSeleccionado.getNumero());

        BorderPane root = new BorderPane();
        root.setPadding(new Insets(15));

        // Top: Info del expediente
        VBox header = new VBox(5);
        header.setPadding(new Insets(10));
        header.setStyle("-fx-background-color: #3498db; -fx-background-radius: 5;");

        Label lblExpediente = new Label("Expediente: " + expedienteSeleccionado.getNumero());
        lblExpediente.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: white;");

        Label lblCaratula = new Label(expedienteSeleccionado.getCaratula());
        lblCaratula.setStyle("-fx-text-fill: white;");

        header.getChildren().addAll(lblExpediente, lblCaratula);
        root.setTop(header);

        // Center: Tabla de movimientos
        TableView<Movimiento> tablaMovimientos = new TableView<>();
        ObservableList<Movimiento> listaMovimientos = FXCollections.observableArrayList();
        tablaMovimientos.setItems(listaMovimientos);

        TableColumn<Movimiento, LocalDate> colFecha = new TableColumn<>("Fecha");
        colFecha.setCellValueFactory(new PropertyValueFactory<>("fecha"));
        colFecha.setPrefWidth(100);

        TableColumn<Movimiento, TipoMovimiento> colTipo = new TableColumn<>("Tipo");
        colTipo.setCellValueFactory(new PropertyValueFactory<>("tipo"));
        colTipo.setPrefWidth(120);

        TableColumn<Movimiento, String> colDescripcion = new TableColumn<>("Descripción");
        colDescripcion.setCellValueFactory(new PropertyValueFactory<>("descripcion"));
        colDescripcion.setPrefWidth(300);

        TableColumn<Movimiento, String> colCuaderno = new TableColumn<>("Cuaderno");
        colCuaderno.setCellValueFactory(new PropertyValueFactory<>("cuaderno"));
        colCuaderno.setPrefWidth(120);

        TableColumn<Movimiento, Integer> colFoja = new TableColumn<>("Foja");
        colFoja.setCellValueFactory(new PropertyValueFactory<>("foja"));
        colFoja.setPrefWidth(80);

        tablaMovimientos.getColumns().addAll(colFecha, colTipo, colDescripcion, colCuaderno, colFoja);

        root.setCenter(tablaMovimientos);

        // Bottom: Botones
        HBox botones = new HBox(10);
        botones.setAlignment(Pos.CENTER);
        botones.setPadding(new Insets(10));

        Button btnNuevo = new Button("➕ Nuevo Movimiento");
        btnNuevo.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-font-weight: bold;");
        btnNuevo.setOnAction(e -> abrirFormularioMovimiento(null, listaMovimientos));

        Button btnEditar = new Button("✏️ Editar");
        btnEditar.setOnAction(e -> {
            Movimiento seleccionado = tablaMovimientos.getSelectionModel().getSelectedItem();
            if (seleccionado != null) {
                abrirFormularioMovimiento(seleccionado, listaMovimientos);
            } else {
                mostrarAdvertencia("Seleccione un movimiento para editar");
            }
        });

        Button btnEliminar = new Button("🗑️ Eliminar");
        btnEliminar.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white;");
        btnEliminar.setOnAction(e -> {
            Movimiento seleccionado = tablaMovimientos.getSelectionModel().getSelectedItem();
            if (seleccionado != null) {
                Alert confirmacion = new Alert(Alert.AlertType.CONFIRMATION);
                confirmacion.setTitle("Confirmar eliminación");
                confirmacion.setHeaderText("¿Eliminar este movimiento?");
                confirmacion.setContentText(seleccionado.getDescripcion());

                if (confirmacion.showAndWait().get() == ButtonType.OK) {
                    try {
                        movimientoService.eliminarMovimiento(seleccionado.getId());
                        listaMovimientos.remove(seleccionado);
                        mostrarInfo("Movimiento eliminado correctamente");
                    } catch (SQLException ex) {
                        mostrarError("Error al eliminar: " + ex.getMessage());
                    }
                }
            } else {
                mostrarAdvertencia("Seleccione un movimiento para eliminar");
            }
        });

        Button btnCerrar = new Button("❌ Cerrar");
        btnCerrar.setOnAction(e -> ventanaMovimientos.close());

        botones.getChildren().addAll(btnNuevo, btnEditar, btnEliminar, btnCerrar);
        root.setBottom(botones);

        // Cargar movimientos
        try {
            List<Movimiento> movimientos = movimientoService.listarPorExpediente(expedienteSeleccionado.getId());
            listaMovimientos.addAll(movimientos);
        } catch (SQLException e) {
            mostrarError("Error al cargar movimientos: " + e.getMessage());
        }

        Scene scene = new Scene(root, 800, 500);
        ventanaMovimientos.setScene(scene);
        ventanaMovimientos.showAndWait();
    }

    private void abrirFormularioMovimiento(Movimiento movimiento, ObservableList<Movimiento> lista) {
        Stage ventana = new Stage();
        ventana.initModality(Modality.APPLICATION_MODAL);
        ventana.setTitle(movimiento == null ? "Nuevo Movimiento" : "Editar Movimiento");

        VBox form = new VBox(10);
        form.setPadding(new Insets(20));

        DatePicker dpFecha = new DatePicker(LocalDate.now());
        ComboBox<TipoMovimiento> cmbTipo = new ComboBox<>();
        cmbTipo.setItems(FXCollections.observableArrayList(TipoMovimiento.values()));
        cmbTipo.setMaxWidth(Double.MAX_VALUE);

        TextArea txtDescripcion = new TextArea();
        txtDescripcion.setPrefRowCount(4);

        TextField txtCuaderno = new TextField();
        TextField txtFoja = new TextField();
        TextArea txtObservaciones = new TextArea();
        txtObservaciones.setPrefRowCount(3);

        if (movimiento != null) {
            dpFecha.setValue(movimiento.getFecha());
            cmbTipo.setValue(movimiento.getTipo());
            txtDescripcion.setText(movimiento.getDescripcion());
            txtCuaderno.setText(movimiento.getCuaderno());
            if (movimiento.getFoja() != null) {
                txtFoja.setText(movimiento.getFoja().toString());
            }
            txtObservaciones.setText(movimiento.getObservaciones());
        }

        form.getChildren().addAll(
                new Label("Fecha *:"), dpFecha,
                new Label("Tipo *:"), cmbTipo,
                new Label("Descripción *:"), txtDescripcion,
                new Label("Cuaderno:"), txtCuaderno,
                new Label("Foja:"), txtFoja,
                new Label("Observaciones:"), txtObservaciones
        );

        HBox botones = new HBox(10);
        botones.setAlignment(Pos.CENTER);
        botones.setPadding(new Insets(10, 0, 0, 0));

        Button btnGuardar = new Button("💾 Guardar");
        btnGuardar.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-font-weight: bold;");
        btnGuardar.setOnAction(e -> {
            try {
                Movimiento mov = movimiento != null ? movimiento : new Movimiento();
                mov.setExpedienteId(expedienteSeleccionado.getId());
                mov.setFecha(dpFecha.getValue());
                mov.setTipo(cmbTipo.getValue());
                mov.setDescripcion(txtDescripcion.getText());
                mov.setCuaderno(txtCuaderno.getText());

                if (!txtFoja.getText().trim().isEmpty()) {
                    try {
                        mov.setFoja(Integer.parseInt(txtFoja.getText().trim()));
                    } catch (NumberFormatException ex) {
                        mostrarError("El número de foja debe ser válido");
                        return;
                    }
                }

                mov.setObservaciones(txtObservaciones.getText());
                mov.setUsuarioId(SesionUsuario.getUsuarioActual().getId());

                if (movimiento == null) {
                    Movimiento nuevo = movimientoService.crearMovimiento(mov);
                    lista.add(nuevo);
                    mostrarInfo("Movimiento creado correctamente");
                } else {
                    movimientoService.actualizarMovimiento(mov);
                    lista.remove(movimiento);
                    lista.add(mov);
                    mostrarInfo("Movimiento actualizado correctamente");
                }

                ventana.close();

            } catch (IllegalArgumentException ex) {
                mostrarAdvertencia(ex.getMessage());
            } catch (SQLException ex) {
                mostrarError("Error: " + ex.getMessage());
            }
        });

        Button btnCancelar = new Button("❌ Cancelar");
        btnCancelar.setOnAction(e -> ventana.close());

        botones.getChildren().addAll(btnGuardar, btnCancelar);
        form.getChildren().add(botones);

        ScrollPane scroll = new ScrollPane(form);
        scroll.setFitToWidth(true);

        Scene scene = new Scene(scroll, 500, 550);
        ventana.setScene(scene);
        ventana.showAndWait();
    }

    // ==================== AGENDA ====================

    private VBox crearPanelAgenda() {
        VBox panel = new VBox(15);
        panel.setPadding(new Insets(15));

        Label titulo = new Label("📅 Agenda y Calendario");
        titulo.setStyle("-fx-font-size: 20px; -fx-font-weight: bold;");

        // Botones superiores
        HBox botonesSuperiores = new HBox(10);
        botonesSuperiores.setAlignment(Pos.CENTER_LEFT);

        Button btnNuevoEvento = new Button("➕ Nuevo Evento");
        btnNuevoEvento.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-font-weight: bold;");
        btnNuevoEvento.setOnAction(e -> abrirFormularioEvento(null));

        Button btnActualizar = new Button("🔄 Actualizar");
        btnActualizar.setOnAction(e -> cargarEventosAgenda());

        ComboBox<String> cmbVista = new ComboBox<>();
        cmbVista.setItems(FXCollections.observableArrayList("Todos", "Hoy", "Esta Semana", "Este Mes", "Pendientes"));
        cmbVista.setValue("Esta Semana");
        cmbVista.setOnAction(e -> filtrarEventosAgenda(cmbVista.getValue()));

        botonesSuperiores.getChildren().addAll(btnNuevoEvento, btnActualizar, new Label("Vista:"), cmbVista);

        // Tabla de eventos
        TableView<EventoAgenda> tablaEventos = crearTablaEventos();

        panel.getChildren().addAll(titulo, botonesSuperiores, tablaEventos);
        VBox.setVgrow(tablaEventos, Priority.ALWAYS);

        return panel;
    }

    private TableView<EventoAgenda> crearTablaEventos() {
        TableView<EventoAgenda> tabla = new TableView<>();
        ObservableList<EventoAgenda> listaEventos = FXCollections.observableArrayList();
        tabla.setItems(listaEventos);

        // Guardar referencia para poder actualizarla
        tabla.setUserData(listaEventos);

        TableColumn<EventoAgenda, LocalDateTime> colFechaHora = new TableColumn<>("Fecha y Hora");
        colFechaHora.setCellValueFactory(new PropertyValueFactory<>("fechaHora"));
        colFechaHora.setPrefWidth(150);

        TableColumn<EventoAgenda, String> colTitulo = new TableColumn<>("Título");
        colTitulo.setCellValueFactory(new PropertyValueFactory<>("titulo"));
        colTitulo.setPrefWidth(250);

        TableColumn<EventoAgenda, TipoEvento> colTipo = new TableColumn<>("Tipo");
        colTipo.setCellValueFactory(new PropertyValueFactory<>("tipo"));
        colTipo.setPrefWidth(120);

        TableColumn<EventoAgenda, String> colUbicacion = new TableColumn<>("Ubicación");
        colUbicacion.setCellValueFactory(new PropertyValueFactory<>("ubicacion"));
        colUbicacion.setPrefWidth(150);

        TableColumn<EventoAgenda, EstadoEvento> colEstado = new TableColumn<>("Estado");
        colEstado.setCellValueFactory(new PropertyValueFactory<>("estado"));
        colEstado.setPrefWidth(100);

        // Columna de acciones
        TableColumn<EventoAgenda, Void> colAcciones = new TableColumn<>("Acciones");
        colAcciones.setPrefWidth(200);
        colAcciones.setCellFactory(param -> new TableCell<>() {
            private final Button btnEditar = new Button("✏️");
            private final Button btnCompletar = new Button("✅");
            private final Button btnEliminar = new Button("🗑️");

            {
                btnEditar.setOnAction(e -> {
                    EventoAgenda evento = getTableView().getItems().get(getIndex());
                    abrirFormularioEvento(evento);
                });

                btnCompletar.setOnAction(e -> {
                    EventoAgenda evento = getTableView().getItems().get(getIndex());
                    try {
                        agendaService.completarEvento(evento.getId());
                        mostrarInfo("Evento completado");
                        cargarEventosAgenda();
                    } catch (SQLException ex) {
                        mostrarError("Error: " + ex.getMessage());
                    }
                });

                btnEliminar.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white;");
                btnEliminar.setOnAction(e -> {
                    EventoAgenda evento = getTableView().getItems().get(getIndex());
                    Alert confirmacion = new Alert(Alert.AlertType.CONFIRMATION);
                    confirmacion.setHeaderText("¿Eliminar este evento?");

                    if (confirmacion.showAndWait().get() == ButtonType.OK) {
                        try {
                            agendaService.eliminarEvento(evento.getId());
                            mostrarInfo("Evento eliminado");
                            cargarEventosAgenda();
                        } catch (SQLException ex) {
                            mostrarError("Error: " + ex.getMessage());
                        }
                    }
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    HBox botones = new HBox(5, btnEditar, btnCompletar, btnEliminar);
                    setGraphic(botones);
                }
            }
        });

        tabla.getColumns().addAll(colFechaHora, colTitulo, colTipo, colUbicacion, colEstado, colAcciones);

        // Cargar eventos
        try {
            Integer usuarioId = SesionUsuario.getUsuarioActual().getId();
            List<EventoAgenda> eventos = agendaService.listarEstaSemana(usuarioId);
            listaEventos.addAll(eventos);
        } catch (SQLException e) {
            mostrarError("Error al cargar eventos: " + e.getMessage());
        }

        return tabla;
    }

    @SuppressWarnings("unchecked")
    private void cargarEventosAgenda() {
        // Buscar la tabla en el panel de agenda
        TabPane tabPane = (TabPane) scene.getRoot().lookup("TabPane");
        if (tabPane != null) {
            Tab tabAgenda = tabPane.getTabs().stream()
                    .filter(t -> t.getText().contains("Agenda"))
                    .findFirst()
                    .orElse(null);

            if (tabAgenda != null) {
                VBox contenido = (VBox) tabAgenda.getContent();
                TableView<EventoAgenda> tabla = (TableView<EventoAgenda>) contenido.lookup("TableView");
                if (tabla != null) {
                    ObservableList<EventoAgenda> lista = (ObservableList<EventoAgenda>) tabla.getUserData();
                    try {
                        Integer usuarioId = SesionUsuario.getUsuarioActual().getId();
                        List<EventoAgenda> eventos = agendaService.listarEstaSemana(usuarioId);
                        lista.clear();
                        lista.addAll(eventos);
                    } catch (SQLException e) {
                        mostrarError("Error al cargar eventos: " + e.getMessage());
                    }
                }
            }
        }

        cargarDashboard(); // Actualizar también el dashboard
    }

    @SuppressWarnings("unchecked")
    private void filtrarEventosAgenda(String filtro) {
        TabPane tabPane = (TabPane) scene.getRoot().lookup("TabPane");
        if (tabPane != null) {
            Tab tabAgenda = tabPane.getTabs().stream()
                    .filter(t -> t.getText().contains("Agenda"))
                    .findFirst()
                    .orElse(null);

            if (tabAgenda != null) {
                VBox contenido = (VBox) tabAgenda.getContent();
                TableView<EventoAgenda> tabla = (TableView<EventoAgenda>) contenido.lookup("TableView");
                if (tabla != null) {
                    ObservableList<EventoAgenda> lista = (ObservableList<EventoAgenda>) tabla.getUserData();
                    try {
                        Integer usuarioId = SesionUsuario.getUsuarioActual().getId();
                        List<EventoAgenda> eventos = switch (filtro) {
                            case "Hoy" -> agendaService.listarHoy(usuarioId);
                            case "Esta Semana" -> agendaService.listarEstaSemana(usuarioId);
                            case "Este Mes" -> agendaService.listarEsteMes(usuarioId);
                            case "Pendientes" -> agendaService.listarPendientes(usuarioId);
                            default -> agendaService.listarPorUsuario(usuarioId);
                        };
                        lista.clear();
                        lista.addAll(eventos);
                    } catch (SQLException e) {
                        mostrarError("Error al filtrar eventos: " + e.getMessage());
                    }
                }
            }
        }
    }

    private void abrirFormularioEvento(EventoAgenda evento) {
        Stage ventana = new Stage();
        ventana.initModality(Modality.APPLICATION_MODAL);
        ventana.setTitle(evento == null ? "Nuevo Evento" : "Editar Evento");

        VBox form = new VBox(10);
        form.setPadding(new Insets(20));

        TextField txtTitulo = new TextField();
        TextArea txtDescripcion = new TextArea();
        txtDescripcion.setPrefRowCount(3);

        DatePicker dpFecha = new DatePicker(LocalDate.now());

        Spinner<Integer> spHora = new Spinner<>(0, 23, 9);
        Spinner<Integer> spMinuto = new Spinner<>(0, 59, 0, 15);
        HBox horaBox = new HBox(5, new Label("Hora:"), spHora, new Label(":"), spMinuto);

        Spinner<Integer> spDuracion = new Spinner<>(15, 480, 60, 15);

        ComboBox<TipoEvento> cmbTipo = new ComboBox<>();
        cmbTipo.setItems(FXCollections.observableArrayList(TipoEvento.values()));
        cmbTipo.setMaxWidth(Double.MAX_VALUE);

        TextField txtUbicacion = new TextField();

        ComboBox<Integer> cmbRecordatorio = new ComboBox<>();
        cmbRecordatorio.setItems(FXCollections.observableArrayList(
                15, 30, 60, 120, 1440, 2880
        ));
        cmbRecordatorio.setPromptText("Minutos antes");

        // Selector de expediente (opcional)
        ComboBox<String> cmbExpediente = new ComboBox<>();
        cmbExpediente.setPromptText("Sin expediente asociado");
        try {
            List<Expediente> expedientes = expedienteService.listarActivos();
            ObservableList<String> items = FXCollections.observableArrayList();
            items.add("Sin expediente");
            for (Expediente exp : expedientes) {
                items.add(exp.getId() + " - " + exp.getNumero() + " - " + exp.getCaratula());
            }
            cmbExpediente.setItems(items);
            cmbExpediente.setValue("Sin expediente");
        } catch (SQLException e) {
            e.printStackTrace();
        }

        if (evento != null) {
            txtTitulo.setText(evento.getTitulo());
            txtDescripcion.setText(evento.getDescripcion());
            dpFecha.setValue(evento.getFechaHora().toLocalDate());
            spHora.getValueFactory().setValue(evento.getFechaHora().getHour());
            spMinuto.getValueFactory().setValue(evento.getFechaHora().getMinute());
            spDuracion.getValueFactory().setValue(evento.getDuracionMinutos());
            cmbTipo.setValue(evento.getTipo());
            txtUbicacion.setText(evento.getUbicacion());
            cmbRecordatorio.setValue(evento.getRecordatorioMinutos());
        } else {
            cmbRecordatorio.setValue(1440);
        }

        form.getChildren().addAll(
                new Label("Título *:"), txtTitulo,
                new Label("Descripción:"), txtDescripcion,
                new Label("Fecha *:"), dpFecha,
                horaBox,
                new Label("Duración (minutos):"), spDuracion,
                new Label("Tipo *:"), cmbTipo,
                new Label("Ubicación:"), txtUbicacion,
                new Label("Recordatorio:"), cmbRecordatorio,
                new Label("Expediente asociado:"), cmbExpediente
        );

        HBox botones = new HBox(10);
        botones.setAlignment(Pos.CENTER);
        botones.setPadding(new Insets(10, 0, 0, 0));

        Button btnGuardar = new Button("💾 Guardar");
        btnGuardar.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-font-weight: bold;");
        btnGuardar.setOnAction(e -> {
            try {
                EventoAgenda ev = evento != null ? evento : new EventoAgenda();
                ev.setTitulo(txtTitulo.getText());
                ev.setDescripcion(txtDescripcion.getText());

                // CORRECCIÓN: Crear LocalTime primero
                LocalDate fecha = dpFecha.getValue();
                LocalTime hora = LocalTime.of(spHora.getValue(), spMinuto.getValue());
                LocalDateTime fechaHora = LocalDateTime.of(fecha, hora);

                ev.setFechaHora(fechaHora);
                ev.setDuracionMinutos(spDuracion.getValue());
                ev.setTipo(cmbTipo.getValue());
                ev.setUbicacion(txtUbicacion.getText());
                ev.setRecordatorioMinutos(cmbRecordatorio.getValue());

                // Expediente asociado
                String expSeleccionado = cmbExpediente.getValue();
                if (expSeleccionado != null && !expSeleccionado.equals("Sin expediente")) {
                    Integer expId = Integer.parseInt(expSeleccionado.split(" - ")[0]);
                    ev.setExpedienteId(expId);
                }

                ev.setUsuarioId(SesionUsuario.getUsuarioActual().getId());

                if (evento == null) {
                    agendaService.crearEvento(ev);
                    mostrarInfo("Evento creado correctamente");
                } else {
                    agendaService.actualizarEvento(ev);
                    mostrarInfo("Evento actualizado correctamente");
                }

                cargarEventosAgenda();
                ventana.close();

            } catch (IllegalArgumentException ex) {
                mostrarAdvertencia(ex.getMessage());
            } catch (SQLException ex) {
                mostrarError("Error: " + ex.getMessage());
            }
        });

        Button btnCancelar = new Button("❌ Cancelar");
        btnCancelar.setOnAction(e -> ventana.close());

        botones.getChildren().addAll(btnGuardar, btnCancelar);
        form.getChildren().add(botones);

        ScrollPane scroll = new ScrollPane(form);
        scroll.setFitToWidth(true);

        Scene scene = new Scene(scroll, 550, 650);
        ventana.setScene(scene);
        ventana.showAndWait();
    }

// ==================== OPERACIONES CRUD EXPEDIENTES ====================

    private void guardarExpediente() {
        try {
            if (!validarCamposExpediente()) {
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

            limpiarFormularioExpediente();
            cargarExpedientes();
            cargarDashboard();

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
                limpiarFormularioExpediente();
                cargarExpedientes();
                cargarDashboard();
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

    private void limpiarFormularioExpediente() {
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

    private boolean validarCamposExpediente() {
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

    private HBox crearBarraEstado() {
        HBox barra = new HBox();
        barra.setPadding(new Insets(5));
        barra.setStyle("-fx-background-color: #ecf0f1;");

        Label lblEstado = new Label("✅ Sistema listo");
        barra.getChildren().add(lblEstado);

        return barra;
    }

    // ==================== PANEL DE CLIENTES ====================

    // ==================== PANEL DE CLIENTES (VERSIÓN SIMPLE) ====================

    private VBox crearPanelClientes() {
        VBox panel = new VBox(15);
        panel.setPadding(new Insets(15));

        Label titulo = new Label("👥 Gestión de Clientes");
        titulo.setStyle("-fx-font-size: 20px; -fx-font-weight: bold;");

        // Barra superior
        HBox barraControl = new HBox(10);
        barraControl.setAlignment(Pos.CENTER_LEFT);

        Label lblBuscar = new Label("🔍 Buscar:");
        txtBuscarCliente = new TextField();
        txtBuscarCliente.setPromptText("Nombre, DNI, email...");
        txtBuscarCliente.setPrefWidth(250);
        txtBuscarCliente.textProperty().addListener((obs, old, val) -> buscarClientes());

        Button btnNuevoCliente = new Button("➕ Nuevo Cliente");
        btnNuevoCliente.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-font-weight: bold;");
        btnNuevoCliente.setOnAction(e -> abrirFormularioCliente(null));

        Button btnActualizar = new Button("🔄 Actualizar");
        btnActualizar.setOnAction(e -> cargarClientes());

        barraControl.getChildren().addAll(lblBuscar, txtBuscarCliente, btnNuevoCliente, btnActualizar);

        // Tabla de clientes
        tablaClientes = new TableView<>();
        tablaClientes.setItems(listaClientes);

        TableColumn<Cliente, String> colNombre = new TableColumn<>("Nombre Completo");
        colNombre.setCellValueFactory(new PropertyValueFactory<>("nombreCompleto"));
        colNombre.setPrefWidth(250);

        TableColumn<Cliente, String> colDni = new TableColumn<>("DNI");
        colDni.setCellValueFactory(new PropertyValueFactory<>("dni"));
        colDni.setPrefWidth(100);

        TableColumn<Cliente, String> colTelefono = new TableColumn<>("Teléfono");
        colTelefono.setCellValueFactory(new PropertyValueFactory<>("telefono"));
        colTelefono.setPrefWidth(120);

        TableColumn<Cliente, String> colEmail = new TableColumn<>("Email");
        colEmail.setCellValueFactory(new PropertyValueFactory<>("email"));
        colEmail.setPrefWidth(200);

        tablaClientes.getColumns().addAll(colNombre, colDni, colTelefono, colEmail);

        // Doble clic para ver detalles
        tablaClientes.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2 && tablaClientes.getSelectionModel().getSelectedItem() != null) {
                abrirVistaDetalladaCliente(tablaClientes.getSelectionModel().getSelectedItem());
            }
        });

        panel.getChildren().addAll(titulo, barraControl, tablaClientes);
        VBox.setVgrow(tablaClientes, Priority.ALWAYS);

        // Cargar clientes
        cargarClientes();

        return panel;
    }

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
        stage.setTitle("Juridix - Sistema de Gestión Jurídica");
        stage.setMaximized(true);
        stage.show();
    }
    // ==================== OPERACIONES DE CLIENTES ====================

    private void cargarClientes() {
        try {
            List<Cliente> clientes = clienteService.listarActivos();
            listaClientes.clear();
            listaClientes.addAll(clientes);
        } catch (SQLException e) {
            mostrarError("Error al cargar clientes: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void buscarClientes() {
        try {
            String textoBusqueda = txtBuscarCliente.getText().trim();
            boolean soloActivos = true; // Puedes vincular esto con un CheckBox si quieres

            List<Cliente> resultados;

            if (textoBusqueda.isEmpty()) {
                resultados = soloActivos ? clienteService.listarActivos() : clienteService.listarTodos();
            } else {
                resultados = clienteService.buscarPorCriterios(textoBusqueda, soloActivos);
            }

            listaClientes.clear();
            listaClientes.addAll(resultados);

        } catch (SQLException e) {
            mostrarError("Error al buscar clientes: " + e.getMessage());
        }
    }

    private void abrirFormularioCliente(Cliente cliente) {
        Stage ventana = new Stage();
        ventana.initModality(Modality.APPLICATION_MODAL);
        ventana.setTitle(cliente == null ? "Nuevo Cliente" : "Editar Cliente - " + cliente.getNombreCompleto());

        VBox form = new VBox(10);
        form.setPadding(new Insets(20));

        // Campos del formulario
        TextField txtNombre = new TextField();
        TextField txtDni = new TextField();
        TextField txtCuit = new TextField();
        DatePicker dpFechaNac = new DatePicker();
        TextField txtTelefono = new TextField();
        TextField txtEmail = new TextField();
        TextField txtDomicilio = new TextField();
        TextField txtLocalidad = new TextField();

        ComboBox<String> cmbProvincia = new ComboBox<>();
        cmbProvincia.setItems(FXCollections.observableArrayList(
                "Buenos Aires", "CABA", "Catamarca", "Chaco", "Chubut", "Córdoba",
                "Corrientes", "Entre Ríos", "Formosa", "Jujuy", "La Pampa", "La Rioja",
                "Mendoza", "Misiones", "Neuquén", "Río Negro", "Salta", "San Juan",
                "San Luis", "Santa Cruz", "Santa Fe", "Santiago del Estero",
                "Tierra del Fuego", "Tucumán"
        ));
        cmbProvincia.setMaxWidth(Double.MAX_VALUE);

        TextField txtCP = new TextField();
        TextField txtProfesion = new TextField();

        ComboBox<String> cmbEstadoCivil = new ComboBox<>();
        cmbEstadoCivil.setItems(FXCollections.observableArrayList(
                "Soltero/a", "Casado/a", "Divorciado/a", "Viudo/a", "Unión de hecho"
        ));
        cmbEstadoCivil.setMaxWidth(Double.MAX_VALUE);

        TextArea txtObservaciones = new TextArea();
        txtObservaciones.setPrefRowCount(3);

        CheckBox chkActivo = new CheckBox("Cliente activo");
        chkActivo.setSelected(true);

        // Cargar datos si es edición
        if (cliente != null) {
            txtNombre.setText(cliente.getNombreCompleto());
            txtDni.setText(cliente.getDni());
            txtCuit.setText(cliente.getCuitCuil());
            dpFechaNac.setValue(cliente.getFechaNacimiento());
            txtTelefono.setText(cliente.getTelefono());
            txtEmail.setText(cliente.getEmail());
            txtDomicilio.setText(cliente.getDomicilio());
            txtLocalidad.setText(cliente.getLocalidad());
            cmbProvincia.setValue(cliente.getProvincia());
            txtCP.setText(cliente.getCodigoPostal());
            txtProfesion.setText(cliente.getProfesion());
            cmbEstadoCivil.setValue(cliente.getEstadoCivil());
            txtObservaciones.setText(cliente.getObservaciones());
            chkActivo.setSelected(cliente.isActivo());
        }


        // Layout del formulario en dos columnas
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);

        int row = 0;

        // Columna izquierda
        grid.add(new Label("Nombre Completo *:"), 0, row);
        grid.add(txtNombre, 1, row++);

        grid.add(new Label("DNI:"), 0, row);
        grid.add(txtDni, 1, row++);

        grid.add(new Label("CUIT/CUIL:"), 0, row);
        grid.add(txtCuit, 1, row++);

        grid.add(new Label("Fecha Nacimiento:"), 0, row);
        grid.add(dpFechaNac, 1, row++);

        grid.add(new Label("Teléfono:"), 0, row);
        grid.add(txtTelefono, 1, row++);

        grid.add(new Label("Email:"), 0, row);
        grid.add(txtEmail, 1, row++);

        grid.add(new Label("Domicilio:"), 0, row);
        grid.add(txtDomicilio, 1, row++);

        grid.add(new Label("Localidad:"), 0, row);
        grid.add(txtLocalidad, 1, row++);

        grid.add(new Label("Provincia:"), 0, row);
        grid.add(cmbProvincia, 1, row++);

        grid.add(new Label("Código Postal:"), 0, row);
        grid.add(txtCP, 1, row++);

        grid.add(new Label("Profesión:"), 0, row);
        grid.add(txtProfesion, 1, row++);

        grid.add(new Label("Estado Civil:"), 0, row);
        grid.add(cmbEstadoCivil, 1, row++);

        grid.add(new Label("Observaciones:"), 0, row);
        grid.add(txtObservaciones, 1, row++);

        grid.add(chkActivo, 1, row++);

        // Hacer que las columnas se expandan
        ColumnConstraints col1 = new ColumnConstraints();
        col1.setMinWidth(120);
        ColumnConstraints col2 = new ColumnConstraints();
        col2.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().addAll(col1, col2);

        // Botones
        HBox botones = new HBox(10);
        botones.setAlignment(Pos.CENTER);
        botones.setPadding(new Insets(15, 0, 0, 0));

        Button btnGuardar = new Button("💾 Guardar");
        btnGuardar.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-font-weight: bold;");
        btnGuardar.setOnAction(e -> {
            try {
                Cliente cli = cliente != null ? cliente : new Cliente();

                cli.setNombreCompleto(txtNombre.getText());
                cli.setDni(txtDni.getText());
                cli.setCuitCuil(txtCuit.getText());
                cli.setFechaNacimiento(dpFechaNac.getValue());
                cli.setTelefono(txtTelefono.getText());
                cli.setEmail(txtEmail.getText());
                cli.setDomicilio(txtDomicilio.getText());
                cli.setLocalidad(txtLocalidad.getText());
                cli.setProvincia(cmbProvincia.getValue());
                cli.setCodigoPostal(txtCP.getText());
                cli.setProfesion(txtProfesion.getText());
                cli.setEstadoCivil(cmbEstadoCivil.getValue());
                cli.setObservaciones(txtObservaciones.getText());
                cli.setActivo(chkActivo.isSelected());
                cli.setUsuarioCreadorId(SesionUsuario.getUsuarioActual().getId());

                if (cliente == null) {
                    clienteService.crearCliente(cli);
                    mostrarInfo("Cliente creado correctamente");
                } else {
                    clienteService.actualizarCliente(cli);
                    mostrarInfo("Cliente actualizado correctamente");
                }

                cargarClientes();
                ventana.close();

            } catch (IllegalArgumentException ex) {
                mostrarAdvertencia(ex.getMessage());
            } catch (SQLException ex) {
                mostrarError("Error: " + ex.getMessage());
            }
        });

        Button btnCancelar = new Button("❌ Cancelar");
        btnCancelar.setOnAction(e -> ventana.close());

        botones.getChildren().addAll(btnGuardar, btnCancelar);

        form.getChildren().addAll(grid, botones);

        ScrollPane scroll = new ScrollPane(form);
        scroll.setFitToWidth(true);

        Scene scene = new Scene(scroll, 600, 700);
        ventana.setScene(scene);
        ventana.showAndWait();
    }
    // ==================== VISTA DETALLADA DEL CLIENTE ====================

    private void abrirVistaDetalladaCliente(Cliente cliente) {
        Stage ventana = new Stage();
        ventana.initModality(Modality.APPLICATION_MODAL);
        ventana.setTitle("Cliente: " + cliente.getNombreCompleto());
        ventana.setMaximized(true);

        BorderPane root = new BorderPane();
        root.setPadding(new Insets(15));

        // ========== TOP: Header con datos básicos del cliente ==========
        VBox header = new VBox(10);
        header.setPadding(new Insets(15));
        header.setStyle("-fx-background-color: #3498db; -fx-background-radius: 5;");

        Label lblNombre = new Label(cliente.getNombreCompleto());
        lblNombre.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: white;");

        HBox infoDatos = new HBox(30);
        infoDatos.setAlignment(Pos.CENTER_LEFT);

        Label lblDni = new Label("📋 DNI: " + (cliente.getDni() != null ? cliente.getDni() : "N/A"));
        lblDni.setStyle("-fx-text-fill: white; -fx-font-size: 14px;");

        Label lblTelefono = new Label("📞 Tel: " + (cliente.getTelefono() != null ? cliente.getTelefono() : "N/A"));
        lblTelefono.setStyle("-fx-text-fill: white; -fx-font-size: 14px;");

        Label lblEmail = new Label("✉️ Email: " + (cliente.getEmail() != null ? cliente.getEmail() : "N/A"));
        lblEmail.setStyle("-fx-text-fill: white; -fx-font-size: 14px;");

        Label lblEstado = new Label(cliente.isActivo() ? "✅ ACTIVO" : "❌ INACTIVO");
        lblEstado.setStyle("-fx-text-fill: " + (cliente.isActivo() ? "#2ecc71" : "#e74c3c") + "; -fx-font-size: 14px; -fx-font-weight: bold;");

        infoDatos.getChildren().addAll(lblDni, lblTelefono, lblEmail, lblEstado);

        Button btnEditar = new Button("✏️ Editar Cliente");
        btnEditar.setStyle("-fx-background-color: white; -fx-text-fill: #3498db; -fx-font-weight: bold;");
        btnEditar.setOnAction(e -> {
            abrirFormularioCliente(cliente);
            ventana.close();
        });

        header.getChildren().addAll(lblNombre, infoDatos, btnEditar);
        root.setTop(header);

        // ========== CENTER: SplitPane con datos, expedientes y documentos ==========
        SplitPane splitPane = new SplitPane();
        splitPane.setOrientation(javafx.geometry.Orientation.HORIZONTAL);
        splitPane.setDividerPositions(0.3);

        // ========== PANEL IZQUIERDO: Datos completos del cliente ==========
        VBox panelDatos = crearPanelDatosCliente(cliente);

        // ========== PANEL DERECHO: Expedientes y Documentos ==========
        SplitPane splitDerecha = new SplitPane();
        splitDerecha.setOrientation(javafx.geometry.Orientation.VERTICAL);
        splitDerecha.setDividerPositions(0.5);

        VBox panelExpedientes = crearPanelExpedientesCliente(cliente);
        VBox panelDocumentos = crearPanelDocumentosCliente(cliente);

        splitDerecha.getItems().addAll(panelExpedientes, panelDocumentos);

        splitPane.getItems().addAll(panelDatos, splitDerecha);
        root.setCenter(splitPane);

        // ========== BOTTOM: Botón cerrar ==========
        HBox bottomBar = new HBox(10);
        bottomBar.setAlignment(Pos.CENTER_RIGHT);
        bottomBar.setPadding(new Insets(10));

        Button btnCerrar = new Button("❌ Cerrar");
        btnCerrar.setOnAction(e -> ventana.close());

        bottomBar.getChildren().add(btnCerrar);
        root.setBottom(bottomBar);

        Scene scene = new Scene(root, 1200, 800);
        ventana.setScene(scene);
        ventana.show();
    }

    // ========== Panel de datos completos del cliente ==========
    private VBox crearPanelDatosCliente(Cliente cliente) {
        VBox panel = new VBox(10);
        panel.setPadding(new Insets(15));
        panel.setStyle("-fx-background-color: #ecf0f1; -fx-border-color: #bdc3c7; -fx-border-width: 1;");

        Label titulo = new Label("📋 Información Completa");
        titulo.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

        ScrollPane scroll = new ScrollPane();
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: transparent;");

        VBox contenido = new VBox(8);
        contenido.setPadding(new Insets(10));

        // Crear campos de información
        contenido.getChildren().addAll(
                crearCampoInfo("Nombre Completo:", cliente.getNombreCompleto()),
                crearCampoInfo("DNI:", cliente.getDni()),
                crearCampoInfo("CUIT/CUIL:", cliente.getCuitCuil()),
                crearCampoInfo("Fecha Nacimiento:", cliente.getFechaNacimiento() != null ?
                        cliente.getFechaNacimiento().toString() + " (" + cliente.getEdad() + " años)" : "N/A"),
                new Separator(),
                crearCampoInfo("Teléfono:", cliente.getTelefono()),
                crearCampoInfo("Email:", cliente.getEmail()),
                new Separator(),
                crearCampoInfo("Domicilio:", cliente.getDomicilioCompleto()),
                new Separator(),
                crearCampoInfo("Profesión:", cliente.getProfesion()),
                crearCampoInfo("Estado Civil:", cliente.getEstadoCivil()),
                new Separator(),
                crearCampoInfo("Observaciones:", cliente.getObservaciones())
        );

        scroll.setContent(contenido);

        panel.getChildren().addAll(titulo, new Separator(), scroll);
        VBox.setVgrow(scroll, Priority.ALWAYS);

        return panel;
    }

    private VBox crearCampoInfo(String label, String valor) {
        VBox campo = new VBox(3);

        Label lblLabel = new Label(label);
        lblLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 12px; -fx-text-fill: #7f8c8d;");

        Label lblValor = new Label(valor != null && !valor.trim().isEmpty() ? valor : "N/A");
        lblValor.setStyle("-fx-font-size: 13px;");
        lblValor.setWrapText(true);

        campo.getChildren().addAll(lblLabel, lblValor);
        return campo;
    }

    // ========== Panel de expedientes del cliente ==========
    private VBox crearPanelExpedientesCliente(Cliente cliente) {
        VBox panel = new VBox(10);
        panel.setPadding(new Insets(15));

        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);

        Label titulo = new Label("📁 Expedientes del Cliente");
        titulo.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

        Button btnNuevoExpediente = new Button("➕ Nuevo Expediente");
        btnNuevoExpediente.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white;");
        btnNuevoExpediente.setOnAction(e -> {
            // Pre-cargar el cliente en el formulario de expediente
            limpiarFormularioExpediente();
            txtCliente.setText(cliente.getNombreCompleto());
            // Puedes cambiar a la pestaña de expedientes si quieres
            mostrarInfo("Complete los datos del expediente y guarde");
        });

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        header.getChildren().addAll(titulo, spacer, btnNuevoExpediente);

        // Tabla de expedientes
        TableView<Expediente> tablaExp = new TableView<>();
        ObservableList<Expediente> listaExp = FXCollections.observableArrayList();
        tablaExp.setItems(listaExp);

        TableColumn<Expediente, String> colNumero = new TableColumn<>("Número");
        colNumero.setCellValueFactory(new PropertyValueFactory<>("numero"));
        colNumero.setPrefWidth(120);

        TableColumn<Expediente, String> colCaratula = new TableColumn<>("Carátula");
        colCaratula.setCellValueFactory(new PropertyValueFactory<>("caratula"));
        colCaratula.setPrefWidth(350);

        TableColumn<Expediente, EstadoExpediente> colEstado = new TableColumn<>("Estado");
        colEstado.setCellValueFactory(new PropertyValueFactory<>("estado"));
        colEstado.setPrefWidth(100);

        TableColumn<Expediente, LocalDate> colFecha = new TableColumn<>("Fecha");
        colFecha.setCellValueFactory(new PropertyValueFactory<>("fechaInicio"));
        colFecha.setPrefWidth(100);

        tablaExp.getColumns().addAll(colNumero, colCaratula, colEstado, colFecha);

        // Doble clic para ver movimientos
        tablaExp.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2 && tablaExp.getSelectionModel().getSelectedItem() != null) {
                expedienteSeleccionado = tablaExp.getSelectionModel().getSelectedItem();
                abrirVentanaMovimientos();
            }
        });

        // Cargar expedientes del cliente
        try {
            List<Expediente> expedientes = new ExpedienteDAO().listarPorCliente(cliente.getId());
            listaExp.addAll(expedientes);
        } catch (SQLException e) {
            mostrarError("Error al cargar expedientes: " + e.getMessage());
        }

        panel.getChildren().addAll(header, tablaExp);
        VBox.setVgrow(tablaExp, Priority.ALWAYS);

        return panel;
    }

    // ========== Panel de documentos del cliente ==========
    private VBox crearPanelDocumentosCliente(Cliente cliente) {
        VBox panel = new VBox(10);
        panel.setPadding(new Insets(15));

        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);

        Label titulo = new Label("📎 Documentos del Cliente");
        titulo.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

        Button btnSubirDoc = new Button("➕ Subir Documento");
        btnSubirDoc.setStyle("-fx-background-color: #3498db; -fx-text-fill: white;");
        btnSubirDoc.setOnAction(e -> abrirDialogoSubirDocumento(cliente));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        header.getChildren().addAll(titulo, spacer, btnSubirDoc);

        // Tabla de documentos
        TableView<DocumentoCliente> tablaDocs = new TableView<>();
        ObservableList<DocumentoCliente> listaDocs = FXCollections.observableArrayList();
        tablaDocs.setItems(listaDocs);

        TableColumn<DocumentoCliente, String> colNombre = new TableColumn<>("Nombre");
        colNombre.setCellValueFactory(new PropertyValueFactory<>("nombreOriginal"));
        colNombre.setPrefWidth(250);

        TableColumn<DocumentoCliente, TipoDocumentoCliente> colTipo = new TableColumn<>("Tipo");
        colTipo.setCellValueFactory(new PropertyValueFactory<>("tipoDocumento"));
        colTipo.setPrefWidth(150);

        TableColumn<DocumentoCliente, String> colTamanio = new TableColumn<>("Tamaño");
        colTamanio.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(cellData.getValue().getTamanioFormateado()));
        colTamanio.setPrefWidth(100);

        TableColumn<DocumentoCliente, LocalDateTime> colFecha = new TableColumn<>("Fecha Subida");
        colFecha.setCellValueFactory(new PropertyValueFactory<>("fechaSubida"));
        colFecha.setPrefWidth(150);

        // Columna de acciones
        TableColumn<DocumentoCliente, Void> colAcciones = new TableColumn<>("Acciones");
        colAcciones.setPrefWidth(150);
        colAcciones.setCellFactory(param -> new TableCell<>() {
            private final Button btnAbrir = new Button("👁️ Ver");
            private final Button btnEliminar = new Button("🗑️");

            {
                btnAbrir.setOnAction(e -> {
                    DocumentoCliente doc = getTableView().getItems().get(getIndex());
                    try {
                        documentoClienteService.abrirDocumento(doc.getId());
                    } catch (Exception ex) {
                        mostrarError("Error al abrir documento: " + ex.getMessage());
                    }
                });

                btnEliminar.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white;");
                btnEliminar.setOnAction(e -> {
                    DocumentoCliente doc = getTableView().getItems().get(getIndex());
                    Alert confirmacion = new Alert(Alert.AlertType.CONFIRMATION);
                    confirmacion.setHeaderText("¿Eliminar este documento?");
                    confirmacion.setContentText(doc.getNombreOriginal());

                    if (confirmacion.showAndWait().get() == ButtonType.OK) {
                        try {
                            documentoClienteService.eliminarDocumento(doc.getId());
                            listaDocs.remove(doc);
                            mostrarInfo("Documento eliminado");
                        } catch (Exception ex) {
                            mostrarError("Error: " + ex.getMessage());
                        }
                    }
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    HBox botones = new HBox(5, btnAbrir, btnEliminar);
                    setGraphic(botones);
                }
            }
        });

        tablaDocs.getColumns().addAll(colNombre, colTipo, colTamanio, colFecha, colAcciones);

        // Cargar documentos del cliente
        try {
            List<DocumentoCliente> documentos = documentoClienteService.listarPorCliente(cliente.getId());
            listaDocs.addAll(documentos);
        } catch (SQLException e) {
            mostrarError("Error al cargar documentos: " + e.getMessage());
        }

        panel.getChildren().addAll(header, tablaDocs);
        VBox.setVgrow(tablaDocs, Priority.ALWAYS);

        return panel;
    }

    // ========== Diálogo para subir documento ==========
    private void abrirDialogoSubirDocumento(Cliente cliente) {
        Stage ventana = new Stage();
        ventana.initModality(Modality.APPLICATION_MODAL);
        ventana.setTitle("Subir Documento - " + cliente.getNombreCompleto());

        VBox form = new VBox(15);
        form.setPadding(new Insets(20));

        Label lblArchivo = new Label("Archivo seleccionado: Ninguno");
        lblArchivo.setStyle("-fx-font-weight: bold;");

        final File[] archivoSeleccionado = {null};

        Button btnSeleccionar = new Button("📁 Seleccionar Archivo");
        btnSeleccionar.setOnAction(e -> {
            javafx.stage.FileChooser fileChooser = new javafx.stage.FileChooser();
            fileChooser.setTitle("Seleccionar Documento");
            fileChooser.getExtensionFilters().addAll(
                    new javafx.stage.FileChooser.ExtensionFilter("Todos los archivos", "*.*"),
                    new javafx.stage.FileChooser.ExtensionFilter("PDF", "*.pdf"),
                    new javafx.stage.FileChooser.ExtensionFilter("Word", "*.doc", "*.docx"),
                    new javafx.stage.FileChooser.ExtensionFilter("Imágenes", "*.jpg", "*.jpeg", "*.png"),
                    new javafx.stage.FileChooser.ExtensionFilter("Excel", "*.xls", "*.xlsx")
            );

            File archivo = fileChooser.showOpenDialog(ventana);
            if (archivo != null) {
                archivoSeleccionado[0] = archivo;
                lblArchivo.setText("Archivo: " + archivo.getName() +
                        " (" + (archivo.length() / 1024) + " KB)");
            }
        });

        ComboBox<TipoDocumentoCliente> cmbTipo = new ComboBox<>();
        cmbTipo.setItems(FXCollections.observableArrayList(TipoDocumentoCliente.values()));
        cmbTipo.setPromptText("Seleccione tipo de documento");
        cmbTipo.setMaxWidth(Double.MAX_VALUE);

        TextArea txtDescripcion = new TextArea();
        txtDescripcion.setPromptText("Descripción opcional...");
        txtDescripcion.setPrefRowCount(3);

        HBox botones = new HBox(10);
        botones.setAlignment(Pos.CENTER);

        Button btnSubir = new Button("⬆️ Subir");
        btnSubir.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-font-weight: bold;");
        btnSubir.setOnAction(e -> {
            if (archivoSeleccionado[0] == null) {
                mostrarAdvertencia("Seleccione un archivo");
                return;
            }

            if (cmbTipo.getValue() == null) {
                mostrarAdvertencia("Seleccione el tipo de documento");
                return;
            }

            try {
                documentoClienteService.subirDocumento(
                        cliente.getId(),
                        archivoSeleccionado[0],
                        cmbTipo.getValue(),
                        txtDescripcion.getText(),
                        SesionUsuario.getUsuarioActual().getId()
                );

                mostrarInfo("Documento subido correctamente");
                ventana.close();

            } catch (Exception ex) {
                mostrarError("Error al subir documento: " + ex.getMessage());
            }
        });

        Button btnCancelar = new Button("❌ Cancelar");
        btnCancelar.setOnAction(e -> ventana.close());

        botones.getChildren().addAll(btnSubir, btnCancelar);

        form.getChildren().addAll(
                new Label("Seleccione el archivo a subir:"),
                btnSeleccionar,
                lblArchivo,
                new Separator(),
                new Label("Tipo de documento *:"),
                cmbTipo,
                new Label("Descripción:"),
                txtDescripcion,
                botones
        );

        Scene scene = new Scene(form, 500, 400);
        ventana.setScene(scene);
        ventana.showAndWait();
    }
}