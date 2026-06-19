package com.juridix.controller;

import com.juridix.db.Database;
import com.juridix.db.ExpedienteDAO;
import com.juridix.model.*;
import com.juridix.seguridad.PasswordUtil;
import com.juridix.service.*;
import com.juridix.seguridad.SesionUsuario;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.Modality;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.ColumnConstraints;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;



public class MainController {

    private final Stage stage;
    private Scene scene;

    private Usuario usuarioActual;
    // Servicios
    private ExpedienteService expedienteService;
    private MovimientoService movimientoService;
    private EventoAgendaService agendaService;
    private CuotaService cuotaService;

    // Servicios económicos
    private HonorarioService honorarioService;
    private GastoService gastoService;
    private PagoService pagoService;

    // Componentes del formulario de expedientes
    private TextField txtNumero;
    private TextField txtCaratula;
    private TextField txtCliente;
    private ComboBox<Cliente> cmbCliente;
    private TextField txtActor;
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
    private ListView<ItemProximoEvento> listProximosEventos;

    // Servicios (agregar junto a los otros servicios)
    private ClienteService clienteService;
    private DocumentoClienteService documentoClienteService;
    private DocumentoExpedienteService documentoExpedienteService;

    // Tabla de clientes
    private TableView<Cliente> tablaClientes;
    private ObservableList<Cliente> listaClientes;
    private TextField txtBuscarCliente;

    // Cliente seleccionado para vista detallada
    private Cliente clienteSeleccionado;

    // Agregar junto a los otros componentes
    private ListView<Notificacion> listNotificaciones;

    // Navegación principal
    private StackPane contentArea;
    private Button btnEconomiaNav;
    private VBox viewEconomia;
    private Button[] botonesNav;

    // ComboBox del panel global de Economía (para preseleccionar expediente)
    private ComboBox<Expediente> cmbExpedientesEconomia;

    private Label lblHonorariosPendientes;
    private Label lblTotalGastosEconomia;
    private Label lblPagosRecibidos;
    private Label lblSaldoPendienteEconomia;

    private VBox viewAgenda;
    private VBox viewExpedientes;
    private Button navAgenda;
    private Button navExpedientes;



    private TableView<EventoAgenda> tablaEventos;
    private final ObservableList<EventoAgenda> listaEventos = FXCollections.observableArrayList();
    private ComboBox<String> cmbVistaAgenda;


    public MainController(Stage stage) {
        this.stage = stage;
        this.expedienteService = new ExpedienteService();
        this.movimientoService = new MovimientoService();
        this.agendaService = new EventoAgendaService();
        this.cuotaService = new CuotaService();

        this.clienteService = new ClienteService();
        this.documentoClienteService = new DocumentoClienteService();
        this.documentoExpedienteService = new DocumentoExpedienteService();

        this.honorarioService = new HonorarioService();
        this.gastoService = new GastoService();
        this.pagoService = new PagoService();

        this.listaExpedientes = FXCollections.observableArrayList();
        this.listaClientes = FXCollections.observableArrayList();
        inicializarUI();
        cargarDashboard();
        cargarExpedientes();

        // ========== ASIGNAR USUARIO ACTUAL ==========
        this.usuarioActual = SesionUsuario.getUsuarioActual();

        if (this.usuarioActual == null) {
            throw new IllegalStateException("No hay usuario en sesión");
        }
    }

    private void inicializarUI() {
        BorderPane root = new BorderPane();

        // Topbar simplificada solo con título y cerrar sesión
        HBox topbar = new HBox();
        topbar.setStyle("-fx-background-color: #FFFFFF; -fx-border-color: rgba(0,0,0,0.06); -fx-border-width: 0 0 0.5 0; -fx-min-height: 48px; -fx-padding: 0 20 0 20;");
        topbar.setAlignment(Pos.CENTER_LEFT);
        Region spacerTop = new Region();
        HBox.setHgrow(spacerTop, Priority.ALWAYS);
        Label lblUserTop = new Label("👤 " + SesionUsuario.getUsuarioActual().getUsername());
        lblUserTop.setStyle("-fx-font-size: 13px; -fx-text-fill: #6B6B67;");
        Button btnCerrarTop = new Button("Cerrar sesión");
        btnCerrarTop.setStyle("-fx-background-color: transparent; -fx-text-fill: #6B6B67; -fx-border-color: rgba(0,0,0,0.10); -fx-border-width: 0.5px; -fx-border-radius: 6px; -fx-background-radius: 6px; -fx-padding: 5 12 5 12; -fx-cursor: hand;");
        btnCerrarTop.setOnAction(e -> cerrarSesion());
        topbar.getChildren().addAll(spacerTop, lblUserTop, btnCerrarTop);
        root.setTop(topbar);
        root.setStyle("-fx-background-color: #F4F4F2;");

        // Center: Panel principal con pestañas
        root.setCenter(crearPanelPrincipal());

        // Bottom: Barra de estado
        //root.setBottom(crearBarraEstado());

        scene = new Scene(root, 1200, 700);
        scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
    }

    private HBox crearBarraSuperior() {
        // Badge de notificaciones
        Label lblBadgeNotif = new Label();
        lblBadgeNotif.getStyleClass().add("badge");
        lblBadgeNotif.setVisible(false);

        HBox barra = new HBox(15);
        barra.setPadding(new Insets(0, 20, 0, 20));
        barra.setAlignment(Pos.CENTER_LEFT);
        barra.setMinHeight(48);
        barra.setStyle("-fx-background-color: #0C447C;");

        //nuevo estilo
        barra.getStyleClass().add("topbar");

        Label lblTitulo = new Label("⚖ Juridix");
        lblTitulo.getStyleClass().add("topbar-title");


        // ============ BÚSQUEDA GLOBAL (NUEVO) ============
        TextField txtBusquedaGlobal = new TextField();
        txtBusquedaGlobal.setPromptText("🔍 Buscar en todo...");
        txtBusquedaGlobal.setPrefWidth(300);
        txtBusquedaGlobal.getStyleClass().add("search-field");
        //txtBusquedaGlobal.setStyle("-fx-background-radius: 20; -fx-padding: 8;");

        txtBusquedaGlobal.setOnAction(e -> {
            String busqueda = txtBusquedaGlobal.getText().trim();
            if (!busqueda.isEmpty()) {
                mostrarResultadosBusquedaGlobal(busqueda);
            }
        });

        Button btnBuscar = new Button("🔍");
        btnBuscar.getStyleClass().addAll("button", "button-info");
        btnBuscar.setOnAction(e -> {
            String busqueda = txtBusquedaGlobal.getText().trim();
            if (!busqueda.isEmpty()) {
                mostrarResultadosBusquedaGlobal(busqueda);
            }
        });


        // ============ FIN BÚSQUEDA GLOBAL ============

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label lblUsuario = new Label("👤 " + SesionUsuario.getUsuarioActual().getUsername());
        lblUsuario.setStyle("-fx-text-fill: white; -fx-font-size: 14px;");

        Button btnCerrarSesion = new Button("Cerrar Sesión");
        btnCerrarSesion.setStyle("-fx-background-color: rgba(255,255,255,0.15); -fx-text-fill: white; -fx-border-color: rgba(255,255,255,0.3); -fx-border-width: 0.5px; -fx-border-radius: 6px; -fx-background-radius: 6px; -fx-cursor: hand; -fx-padding: 6 12 6 12;");
        btnCerrarSesion.setOnAction(e -> cerrarSesion());

        //barra.getChildren().addAll(lblTitulo, txtBusquedaGlobal, btnBuscar, spacer, lblUsuario, btnCerrarSesion);

        // Actualizar badge
        try {
            Integer usuarioId = SesionUsuario.getUsuarioActual().getId();
            List<EventoAgenda> eventosHoy = agendaService.listarHoy(usuarioId);
            if (!eventosHoy.isEmpty()) {
                lblBadgeNotif.setText("🔔 " + eventosHoy.size());
                lblBadgeNotif.setVisible(true);
            }
        } catch (SQLException e) {
            // Ignorar
        }

        // Agregar botón de backup
        Button btnBackup = new Button("💾 Backup");
        btnBackup.setStyle("-fx-background-color: #9b59b6; -fx-text-fill: white; -fx-font-weight: bold;");
        btnBackup.setOnAction(e -> crearBackupBaseDatos());

// Y agregarlo a la barra:
        barra.getChildren().addAll(lblTitulo, lblBadgeNotif, txtBusquedaGlobal, btnBuscar,
                btnBackup, spacer, lblUsuario, btnCerrarSesion);

        return barra;
    }

    private VBox crearPanelPrincipal() {
        VBox panel = new VBox(0);

        // Contenedor principal: sidebar + contenido
        HBox mainContainer = new HBox(0);
        VBox.setVgrow(mainContainer, Priority.ALWAYS);

        // ── SIDEBAR ──
        VBox sidebar = new VBox(1);
        sidebar.setStyle("-fx-background-color: #0C447C; -fx-min-width: 200px; -fx-pref-width: 200px; -fx-max-width: 200px; -fx-padding: 10 8 10 8;");
        VBox.setVgrow(sidebar, Priority.ALWAYS);

        // Logo area
        VBox logoArea = new VBox(2);
        logoArea.setStyle("-fx-padding: 8 8 14 8; -fx-border-color: rgba(255,255,255,0.10); -fx-border-width: 0 0 0.5 0;");
        Label lblAppName = new Label("⚖ Juridix");
        lblAppName.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: white;");
        Label lblAppSub = new Label("Estudio Jurídico");
        lblAppSub.setStyle("-fx-font-size: 11px; -fx-text-fill: #85B7EB;");
        logoArea.getChildren().addAll(lblAppName, lblAppSub);

        // Nav items
        Label lblPrincipal = new Label("PRINCIPAL");
        lblPrincipal.setStyle("-fx-font-size: 10px; -fx-text-fill: #378ADD; -fx-padding: 12 8 4 8; -fx-font-weight: bold;");

        Button btnDashboard   = crearNavItem("◼  Dashboard");
        navExpedientes = crearNavItem("▤  Expedientes");
        Button btnExpedientes = navExpedientes;
        Button btnClientes    = crearNavItem("◉  Clientes");

        Label lblGestion = new Label("GESTIÓN");
        lblGestion.setStyle("-fx-font-size: 10px; -fx-text-fill: #378ADD; -fx-padding: 12 8 4 8; -fx-font-weight: bold;");

        navAgenda = crearNavItem("▦  Agenda");
        Button btnAgenda = navAgenda;
        Button btnEconomia = crearNavItem("◈  Economía");

        // Área de contenido
        contentArea = new StackPane();
        contentArea.setStyle("-fx-background-color: #F4F4F2;");
        HBox.setHgrow(contentArea, Priority.ALWAYS);
        VBox.setVgrow(contentArea, Priority.ALWAYS);

        // Vistas
        VBox viewDashboard   = crearPanelDashboard();
        viewExpedientes = crearPanelExpedientes();
        VBox viewClientes    = crearPanelClientes();
        viewAgenda = crearPanelAgenda();
        this.viewEconomia    = crearPanelEconomia();

        contentArea.getChildren().add(viewDashboard);

        // Acción de navegación
        Runnable[] acciones = new Runnable[5];
        this.botonesNav = new Button[]{btnDashboard, btnExpedientes, btnClientes, btnAgenda, btnEconomia};
        Button[] botones = this.botonesNav;
        VBox[] vistas = {viewDashboard, viewExpedientes, viewClientes, viewAgenda, this.viewEconomia};

        for (int i = 0; i < botones.length; i++) {
            final int idx = i;
            botones[i].setOnAction(e -> {
                contentArea.getChildren().setAll(vistas[idx]);
                for (Button b : botones) {
                    b.setStyle(estiloNavItem(false));
                }
                botones[idx].setStyle(estiloNavItem(true));
            });
        }

        // Activo inicial
        btnDashboard.setStyle(estiloNavItem(true));

        // Panel admin usuarios
        if (SesionUsuario.getUsuarioActual().getRol() == RolUsuario.ADMIN) {
            Label lblSistema = new Label("SISTEMA");
            lblSistema.setStyle("-fx-font-size: 10px; -fx-text-fill: #378ADD; -fx-padding: 12 8 4 8; -fx-font-weight: bold;");
            Button btnUsuarios = crearNavItem("◎  Usuarios");
            VBox viewUsuarios = crearPanelUsuarios();
            btnUsuarios.setOnAction(e -> {
                contentArea.getChildren().setAll(viewUsuarios);
                for (Button b : botones) b.setStyle(estiloNavItem(false));
                btnUsuarios.setStyle(estiloNavItem(true));
            });
            sidebar.getChildren().addAll(lblSistema, btnUsuarios);
            // Agregar al sidebar antes del footer
        }

        sidebar.getChildren().addAll(logoArea, lblPrincipal, btnDashboard,
                lblGestion, btnExpedientes, btnClientes, btnAgenda, btnEconomia);

        // Footer usuario
        Region spacerSidebar = new Region();
        VBox.setVgrow(spacerSidebar, Priority.ALWAYS);
        HBox footerUser = new HBox(8);
        footerUser.setStyle("-fx-padding: 12 8 8 8; -fx-border-color: rgba(255,255,255,0.10); -fx-border-width: 0.5 0 0 0;");
        footerUser.setAlignment(Pos.CENTER_LEFT);
        Label lblUser = new Label("👤  " + SesionUsuario.getUsuarioActual().getUsername());
        lblUser.setStyle("-fx-font-size: 12px; -fx-text-fill: #B5D4F4;");
        footerUser.getChildren().add(lblUser);
        sidebar.getChildren().addAll(spacerSidebar, footerUser);

        mainContainer.getChildren().addAll(sidebar, contentArea);
        panel.getChildren().add(mainContainer);

        return panel;
    }

    private Button crearNavItem(String texto) {
        Button btn = new Button(texto);
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setAlignment(Pos.CENTER_LEFT);
        btn.setStyle(estiloNavItem(false));
        return btn;
    }

    private String estiloNavItem(boolean activo) {
        if (activo) {
            return "-fx-background-color: #185FA5; -fx-text-fill: white; -fx-font-size: 13px; " +
                    "-fx-padding: 8 10 8 10; -fx-background-radius: 6px; -fx-cursor: hand; " +
                    "-fx-alignment: CENTER_LEFT; -fx-border-width: 0;";
        } else {
            return "-fx-background-color: transparent; -fx-text-fill: #B5D4F4; -fx-font-size: 13px; " +
                    "-fx-padding: 8 10 8 10; -fx-background-radius: 6px; -fx-cursor: hand; " +
                    "-fx-alignment: CENTER_LEFT; -fx-border-width: 0;";
        }
    }


    // ==================== DASHBOARD ====================

    private VBox crearPanelDashboard() {
        VBox panel = new VBox(20);
        panel.setPadding(new Insets(24, 32, 24, 32));

        // ===== Encabezado con saludo =====
        VBox encabezado = new VBox(2);
        Label titulo = new Label("Dashboard");
        titulo.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #1A1A18;");
        Label subtitulo = new Label("Resumen general del estudio");
        subtitulo.getStyleClass().add("text-secondary");
        encabezado.getChildren().addAll(titulo, subtitulo);

        // ===== Tarjetas de estadísticas =====
        HBox tarjetas = new HBox(16);
        tarjetas.setAlignment(Pos.CENTER_LEFT);

        VBox tarjetaExpedientes = crearStatCard("Total Expedientes", "");
        lblTotalExpedientes = (Label) tarjetaExpedientes.getProperties().get("valueLabel");

        VBox tarjetaActivos = crearStatCard("Expedientes Activos", "");
        lblExpedientesActivos = (Label) tarjetaActivos.getProperties().get("valueLabel");

        VBox tarjetaEventosHoy = crearStatCard("Eventos Hoy", "");
        lblEventosHoy = (Label) tarjetaEventosHoy.getProperties().get("valueLabel");

        VBox tarjetaEventosSemana = crearStatCard("Esta Semana", "");
        lblEventosSemana = (Label) tarjetaEventosSemana.getProperties().get("valueLabel");

        HBox.setHgrow(tarjetaExpedientes, Priority.ALWAYS);
        HBox.setHgrow(tarjetaActivos, Priority.ALWAYS);
        HBox.setHgrow(tarjetaEventosHoy, Priority.ALWAYS);
        HBox.setHgrow(tarjetaEventosSemana, Priority.ALWAYS);

        tarjetas.getChildren().addAll(tarjetaExpedientes, tarjetaActivos, tarjetaEventosHoy, tarjetaEventosSemana);

        // ===== Panel inferior: próximos eventos + notificaciones =====
        HBox panelInferior = new HBox(16);
        panelInferior.setAlignment(Pos.TOP_CENTER);

        // --- Próximos eventos ---
        VBox panelEventos = new VBox();
        panelEventos.getStyleClass().add("card");
        HBox.setHgrow(panelEventos, Priority.ALWAYS);

        HBox headerEventos = new HBox();
        headerEventos.getStyleClass().add("card-header");
        headerEventos.setAlignment(Pos.CENTER_LEFT);
        Label lblProximos = new Label("Próximos Eventos");
        lblProximos.getStyleClass().add("card-title");
        Region spacerEv = new Region();
        HBox.setHgrow(spacerEv, Priority.ALWAYS);
        Button btnActualizarDashboard = new Button("Actualizar");
        btnActualizarDashboard.getStyleClass().add("btn-ghost");
        btnActualizarDashboard.setOnAction(e -> cargarDashboard());
        headerEventos.getChildren().addAll(lblProximos, spacerEv, btnActualizarDashboard);

        listProximosEventos = new ListView<>();
        listProximosEventos.getStyleClass().add("dashboard-list");
        listProximosEventos.setPrefHeight(220);
        listProximosEventos.setCellFactory(lv -> new ListCell<ItemProximoEvento>() {
            @Override
            protected void updateItem(ItemProximoEvento item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    return;
                }
                if (item.esEncabezado) {
                    Label lbl = new Label(item.textoEncabezado);
                    lbl.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #6B6B67;");
                    HBox box = new HBox(lbl);
                    box.setPadding(new Insets(6, 0, 2, 0));
                    setGraphic(box);
                    setText(null);
                } else {
                    Label lblHora = new Label(item.hora);
                    lblHora.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #185FA5; -fx-min-width: 90px;");

                    Label lblTitulo = new Label(item.titulo);
                    lblTitulo.setStyle("-fx-font-size: 14px; -fx-text-fill: #1A1A18;");

                    Region spacer = new Region();
                    HBox.setHgrow(spacer, Priority.ALWAYS);

                    Label pill = new Label(item.tipo);
                    pill.getStyleClass().addAll("pill", "pill-" + item.nivelPill);

                    HBox box = new HBox(10, lblHora, lblTitulo, spacer, pill);
                    box.setAlignment(Pos.CENTER_LEFT);
                    box.setPadding(new Insets(2, 6, 2, 12));
                    setGraphic(box);
                    setText(null);
                }
            }
        });
        VBox.setVgrow(listProximosEventos, Priority.ALWAYS);


        VBox contenidoEventos = new VBox(listProximosEventos);
        contenidoEventos.setPadding(new Insets(12, 14, 14, 14));
        VBox.setVgrow(contenidoEventos, Priority.ALWAYS);

        panelEventos.getChildren().addAll(headerEventos, contenidoEventos);

        // --- Notificaciones ---
        VBox panelNotificaciones = crearPanelNotificaciones();
        HBox.setHgrow(panelNotificaciones, Priority.ALWAYS);

        panelInferior.getChildren().addAll(panelEventos, panelNotificaciones);
        VBox.setVgrow(panelInferior, Priority.ALWAYS);

        panel.getChildren().addAll(encabezado, tarjetas, panelInferior);
        return panel;
    }

    // Crea una tarjeta de estadística con valor accesible vía properties
    private VBox crearStatCard(String titulo, String valorInicial) {
        VBox card = new VBox(6);
        card.getStyleClass().add("stat-card");
        card.setPrefSize(200, 120);

        Label lblTitulo = new Label(titulo);
        lblTitulo.getStyleClass().add("stat-label");

        Label lblValor = new Label(valorInicial.isEmpty() ? "—" : valorInicial);
        lblValor.getStyleClass().add("stat-value");

        card.getChildren().addAll(lblTitulo, lblValor);
        card.getProperties().put("valueLabel", lblValor);
        return card;
    }

    private void migrarClienteIdEnExpedientes() {
        Alert confirmacion = new Alert(Alert.AlertType.CONFIRMATION);
        confirmacion.setTitle("Migrar datos");
        confirmacion.setHeaderText("¿Actualizar vínculos Cliente-Expediente?");
        confirmacion.setContentText(
                "Esto buscará el cliente por nombre y vinculará automáticamente los expedientes.\n" +
                        "Esta operación puede tardar unos segundos.\n\n" +
                        "¿Continuar?"
        );

        if (confirmacion.showAndWait().get() != ButtonType.OK) {
            return;
        }

        try {
            List<Expediente> expedientes = expedienteService.listarTodos();
            List<Cliente> clientes = clienteService.listarTodos();

            int actualizados = 0;
            int noEncontrados = 0;

            for (Expediente exp : expedientes) {
                // Si ya tiene cliente_id, saltar
                if (exp.getClienteId() != null) {
                    continue;
                }

                String nombreCliente = exp.getCliente();
                if (nombreCliente == null || nombreCliente.isEmpty()) {
                    continue;
                }

                // Buscar el cliente por nombre
                Cliente clienteEncontrado = clientes.stream()
                        .filter(c -> c.getNombreCompleto().equalsIgnoreCase(nombreCliente))
                        .findFirst()
                        .orElse(null);

                if (clienteEncontrado != null) {
                    exp.setClienteId(clienteEncontrado.getId());
                    expedienteService.actualizarExpediente(exp);
                    actualizados++;
                    System.out.println("✅ Vinculado: " + exp.getNumero() + " → " + clienteEncontrado.getNombreCompleto());
                } else {
                    noEncontrados++;
                    System.out.println("⚠️ No encontrado cliente: " + nombreCliente + " (Expediente: " + exp.getNumero() + ")");
                }
            }

            mostrarInfo(
                    "Migración completada:\n\n" +
                            "✅ Expedientes actualizados: " + actualizados + "\n" +
                            "⚠️ Clientes no encontrados: " + noEncontrados
            );

        } catch (Exception ex) {
            mostrarError("Error en migración: " + ex.getMessage());
            ex.printStackTrace();
        }
    }



    // ==================== BÚSQUEDA GLOBAL ====================

    private void mostrarResultadosBusquedaGlobal(String busqueda) {
        Stage ventana = new Stage();
        ventana.initModality(Modality.APPLICATION_MODAL);
        ventana.setTitle("Resultados de búsqueda: " + busqueda);

        VBox root = new VBox(15);
        root.setPadding(new Insets(20));

        Label lblTitulo = new Label("🔍 Resultados para: \"" + busqueda + "\"");
        lblTitulo.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        TabPane tabPane = new TabPane();

        // ========== TAB 1: Clientes ==========
        Tab tabClientes = new Tab("👥 Clientes");
        tabClientes.setClosable(false);

        ListView<String> listClientes = new ListView<>();
        ObservableList<String> resultadosClientes = FXCollections.observableArrayList();
        listClientes.setItems(resultadosClientes);

        try {
            List<Cliente> clientes = clienteService.buscarPorCriterios(busqueda, true);
            if (clientes.isEmpty()) {
                resultadosClientes.add("No se encontraron clientes");
            } else {
                for (Cliente c : clientes) {
                    resultadosClientes.add(c.getNombreCompleto() +
                            (c.getDni() != null ? " - DNI: " + c.getDni() : "") +
                            (c.getTelefono() != null ? " - Tel: " + c.getTelefono() : ""));
                }
            }
        } catch (SQLException e) {
            resultadosClientes.add("❌ Error al buscar clientes");
        }

        // Doble clic para ver cliente
        listClientes.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                String seleccionado = listClientes.getSelectionModel().getSelectedItem();
                if (seleccionado != null && !seleccionado.startsWith("No se") && !seleccionado.startsWith("❌")) {
                    try {
                        // Extraer nombre del cliente
                        String nombreCliente = seleccionado.split(" - ")[0];
                        List<Cliente> clientes = clienteService.buscarPorNombre(nombreCliente);
                        if (!clientes.isEmpty()) {
                            ventana.close();
                            abrirVistaDetalladaCliente(clientes.get(0));
                        }
                    } catch (SQLException e) {
                        mostrarError("Error: " + e.getMessage());
                    }
                }
            }
        });

        tabClientes.setContent(listClientes);

        // ========== TAB 2: Expedientes ==========
        Tab tabExpedientes = new Tab("📁 Expedientes");
        tabExpedientes.setClosable(false);

        ListView<String> listExpedientes = new ListView<>();
        ObservableList<String> resultadosExpedientes = FXCollections.observableArrayList();
        listExpedientes.setItems(resultadosExpedientes);

        try {
            List<Expediente> expedientes = expedienteService.buscarPorCriterios(busqueda, busqueda, null);
            if (expedientes.isEmpty()) {
                resultadosExpedientes.add("No se encontraron expedientes");
            } else {
                for (Expediente exp : expedientes) {
                    resultadosExpedientes.add(exp.getNumero() + " - " + exp.getCaratula() +
                            " (" + exp.getEstado() + ")");
                }
            }
        } catch (SQLException e) {
            resultadosExpedientes.add("❌ Error al buscar expedientes");
        }

        // Doble clic para cargar expediente
        listExpedientes.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                String seleccionado = listExpedientes.getSelectionModel().getSelectedItem();
                if (seleccionado != null && !seleccionado.startsWith("No se") && !seleccionado.startsWith("❌")) {
                    try {
                        String numero = seleccionado.split(" - ")[0];
                        List<Expediente> expedientes = expedienteService.buscarPorCriterios(numero, null, null);
                        if (!expedientes.isEmpty()) {
                            ventana.close();
                            cargarExpedienteEnFormulario(expedientes.get(0));
                            mostrarInfo("Expediente cargado en el formulario");
                        }
                    } catch (SQLException e) {
                        mostrarError("Error: " + e.getMessage());
                    }
                }
            }
        });

        tabExpedientes.setContent(listExpedientes);

        // ========== TAB 3: Agenda ==========
        Tab tabAgenda = new Tab("📅 Eventos");
        tabAgenda.setClosable(false);

        ListView<String> listEventos = new ListView<>();
        ObservableList<String> resultadosEventos = FXCollections.observableArrayList();
        listEventos.setItems(resultadosEventos);

        try {
            Integer usuarioId = SesionUsuario.getUsuarioActual().getId();
            List<EventoAgenda> eventos = agendaService.listarPorUsuario(usuarioId);

            List<EventoAgenda> eventosCoincidentes = eventos.stream()
                    .filter(e -> e.getTitulo().toLowerCase().contains(busqueda.toLowerCase()) ||
                            (e.getDescripcion() != null && e.getDescripcion().toLowerCase().contains(busqueda.toLowerCase())))
                    .toList();

            if (eventosCoincidentes.isEmpty()) {
                resultadosEventos.add("No se encontraron eventos");
            } else {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
                for (EventoAgenda evento : eventosCoincidentes) {
                    resultadosEventos.add(evento.getFechaHora().format(formatter) + " - " +
                            evento.getTitulo() + " (" + evento.getTipo() + ")");
                }
            }
        } catch (SQLException e) {
            resultadosEventos.add("❌ Error al buscar eventos");
        }

        tabAgenda.setContent(listEventos);

        tabPane.getTabs().addAll(tabClientes, tabExpedientes, tabAgenda);

        Button btnCerrar = new Button("❌ Cerrar");
        btnCerrar.setOnAction(e -> ventana.close());

        root.getChildren().addAll(lblTitulo, tabPane, btnCerrar);
        VBox.setVgrow(tabPane, Priority.ALWAYS);

        Scene scene = new Scene(root, 700, 500);
        ventana.setScene(scene);
        ventana.show();
    }

    private VBox crearTarjetaEstadistica(String titulo, String valorInicial, String color) {
        VBox contenedor = new VBox(5);
        contenedor.setAlignment(Pos.CENTER);
        contenedor.setPadding(new Insets(10));
        contenedor.setPrefSize(200, 120);
        //contenedor.setStyle("-fx-background-color: white; -fx-border-color: " + color + "; -fx-border-width: 3; -fx-border-radius: 10; -fx-background-radius: 10;");
        //nuevo estilo
        contenedor.getStyleClass().addAll("stat-card", "stat-card-blue");

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

    private VBox crearPanelNotificaciones() {
        VBox panel = new VBox(10);
        panel.setPadding(new Insets(15));
        panel.getStyleClass().add("card");

        Label lblTitulo = new Label("🔔 Notificaciones y Alertas");
        lblTitulo.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

        listNotificaciones = new ListView<>();
        listNotificaciones.getStyleClass().add("dashboard-list");
        listNotificaciones.setCellFactory(lv -> new ListCell<Notificacion>() {
            @Override
            protected void updateItem(Notificacion item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    return;
                }

                if (item.nivel.equals("header")) {
                    // Encabezado de sección: texto en negrita, sin pill
                    Label lbl = new Label(item.texto);
                    lbl.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #6B6B67;");
                    setGraphic(lbl);
                    setText(null);
                } else if (item.nivel.equals("muted")) {
                    Label lbl = new Label(item.texto);
                    lbl.getStyleClass().add("text-secondary");
                    lbl.setStyle("-fx-font-size: 14px;");
                    setGraphic(lbl);
                    setText(null);
                } else {
                    // Item con pill de color
                    Label pill = new Label();
                    pill.getStyleClass().addAll("pill", "pill-" + item.nivel);
                    pill.setMinWidth(10);
                    pill.setText(" ");

                    Label texto = new Label(item.texto);
                    texto.setStyle("-fx-font-size: 14px; -fx-text-fill: #1A1A18;");

                    HBox box = new HBox(8, pill, texto);
                    box.setAlignment(Pos.CENTER_LEFT);
                    setGraphic(box);
                    setText(null);
                }
            }
        });
        // Cargar notificaciones iniciales
        actualizarNotificaciones();

        Button btnActualizar = new Button("🔄 Actualizar");
        btnActualizar.setOnAction(e -> actualizarNotificaciones());

        panel.getChildren().addAll(lblTitulo, listNotificaciones, btnActualizar);
        return panel;
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

            // Próximos eventos agrupados por día
            List<EventoAgenda> proximos = agendaService.listarProximos(usuarioId, 7);
            ObservableList<ItemProximoEvento> items = FXCollections.observableArrayList();

            LocalDate hoy = LocalDate.now();
            LocalDate manana = hoy.plusDays(1);
            DateTimeFormatter horaFmt = DateTimeFormatter.ofPattern("HH:mm");
            DateTimeFormatter diaFmt = DateTimeFormatter.ofPattern("EEEE dd/MM", new java.util.Locale("es", "ES"));

            LocalDate diaActual = null;
            for (EventoAgenda evento : proximos) {
                LocalDate diaEvento = evento.getFechaHora().toLocalDate();

                // Insertar encabezado cuando cambia el día
                if (!diaEvento.equals(diaActual)) {
                    diaActual = diaEvento;
                    String etiqueta;
                    if (diaEvento.equals(hoy)) {
                        etiqueta = "HOY · " + diaEvento.format(diaFmt);
                    } else if (diaEvento.equals(manana)) {
                        etiqueta = "MAÑANA · " + diaEvento.format(diaFmt);
                    } else {
                        etiqueta = diaEvento.format(diaFmt).toUpperCase();
                    }
                    items.add(ItemProximoEvento.header(etiqueta));
                }

                // Color de pill según tipo
                String nivel = switch (evento.getTipo()) {
                    case AUDIENCIA -> "red";
                    case VENCIMIENTO -> "amber";
                    case REUNION -> "blue";
                    case PRESENTACION -> "green";
                    default -> "blue";
                };

                items.add(ItemProximoEvento.evento(
                        evento.getFechaHora().format(horaFmt),
                        evento.getTitulo(),
                        evento.getTipo().getDisplayName(),
                        nivel
                ));
            }

            if (items.isEmpty()) {
                items.add(ItemProximoEvento.header("No hay eventos próximos"));
            }

            listProximosEventos.setItems(items);

        } catch (SQLException e) {
            mostrarError("Error al cargar dashboard: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void actualizarNotificaciones() {
        ObservableList<Notificacion> notificaciones = FXCollections.observableArrayList();

        try {
            Integer usuarioId = SesionUsuario.getUsuarioActual().getId();
            LocalDate hoy = LocalDate.now();
            LocalDate manana = hoy.plusDays(1);
            DateTimeFormatter hm = DateTimeFormatter.ofPattern("HH:mm");

            // Eventos de hoy
            List<EventoAgenda> eventosHoy = agendaService.listarPorFecha(hoy).stream()
                    .filter(e -> e.getUsuarioId().equals(usuarioId) && e.isPendiente())
                    .toList();

            if (!eventosHoy.isEmpty()) {
                notificaciones.add(new Notificacion("HOY · " + eventosHoy.size() + " evento(s) pendiente(s)", "header"));
                for (EventoAgenda evento : eventosHoy) {
                    notificaciones.add(new Notificacion(
                            evento.getFechaHora().format(hm) + "  ·  " + evento.getTitulo(), "red"));
                }
            }

            // Eventos de mañana
            List<EventoAgenda> eventosManana = agendaService.listarPorFecha(manana).stream()
                    .filter(e -> e.getUsuarioId().equals(usuarioId) && e.isPendiente())
                    .toList();

            if (!eventosManana.isEmpty()) {
                notificaciones.add(new Notificacion("MAÑANA · " + eventosManana.size() + " evento(s)", "header"));
                for (EventoAgenda evento : eventosManana) {
                    notificaciones.add(new Notificacion(
                            evento.getFechaHora().format(hm) + "  ·  " + evento.getTitulo(), "amber"));
                }
            }

            // Próximos 7 días (sin hoy ni mañana)
            List<EventoAgenda> proximaSemana = agendaService.listarProximos(usuarioId, 7).stream()
                    .filter(e -> e.isPendiente()
                            && !e.getFechaHora().toLocalDate().equals(hoy)
                            && !e.getFechaHora().toLocalDate().equals(manana))
                    .toList();

            if (!proximaSemana.isEmpty()) {
                notificaciones.add(new Notificacion("PRÓXIMOS 7 DÍAS · " + proximaSemana.size() + " evento(s)", "header"));
                DateTimeFormatter dm = DateTimeFormatter.ofPattern("dd/MM HH:mm");
                for (EventoAgenda evento : proximaSemana) {
                    notificaciones.add(new Notificacion(
                            evento.getFechaHora().format(dm) + "  ·  " + evento.getTitulo(), "blue"));
                }
            }

            // Vencimientos próximos
            List<EventoAgenda> vencimientos = agendaService.listarProximos(usuarioId, 7).stream()
                    .filter(e -> e.getTipo() == TipoEvento.VENCIMIENTO && e.isPendiente())
                    .toList();

            if (!vencimientos.isEmpty()) {
                notificaciones.add(new Notificacion("VENCIMIENTOS PRÓXIMOS", "header"));
                DateTimeFormatter dmy = DateTimeFormatter.ofPattern("dd/MM/yyyy");
                for (EventoAgenda v : vencimientos) {
                    long dias = java.time.temporal.ChronoUnit.DAYS.between(hoy, v.getFechaHora().toLocalDate());
                    String nivel = dias <= 1 ? "red" : dias <= 3 ? "amber" : "green";
                    notificaciones.add(new Notificacion(
                            v.getFechaHora().toLocalDate().format(dmy) + "  ·  " + v.getTitulo(), nivel));
                }
            }

            // Sin notificaciones
            if (notificaciones.isEmpty()) {
                notificaciones.add(new Notificacion("No hay notificaciones pendientes", "muted"));
                notificaciones.add(new Notificacion("Todo al día", "muted"));
            }

        } catch (SQLException e) {
            notificaciones.add(new Notificacion("Error al cargar notificaciones", "red"));
            e.printStackTrace();
        }

        listNotificaciones.setItems(notificaciones);
    }

    // ==================== EXPEDIENTES ====================

    private VBox crearPanelExpedientes() {
        VBox panel = new VBox(16);
        panel.setPadding(new Insets(24, 32, 24, 32));

        // Header con botón de nuevo expediente
        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);

        VBox tituloBox = new VBox(2);
        Label titulo = new Label("Gestión de Expedientes");
        titulo.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #1A1A18;");
        Label subtitulo = new Label("Listado de causas del estudio");
        subtitulo.getStyleClass().add("text-secondary");
        tituloBox.getChildren().addAll(titulo, subtitulo);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button btnNuevo = new Button("+  Nuevo Expediente");
        btnNuevo.getStyleClass().add("btn-primary-lg");
        btnNuevo.setOnAction(e -> abrirFormularioNuevoExpediente(null));

        header.getChildren().addAll(tituloBox, spacer, btnNuevo);

        // Panel de búsqueda y tabla
        VBox panelTabla = crearPanelTablaExpedientes();

        panel.getChildren().addAll(header, panelTabla);
        VBox.setVgrow(panelTabla, Priority.ALWAYS);

        return panel;
    }

    private void abrirFormularioNuevoExpediente(Cliente clientePreseleccionado) {
        Stage ventana = new Stage();
        ventana.initModality(Modality.APPLICATION_MODAL);
        ventana.setTitle("Nuevo Expediente");

        VBox root = new VBox(15);
        root.setPadding(new Insets(20));
        root.setStyle("-fx-background-color: white;");

        Label titulo = new Label("📄 Crear Nuevo Expediente");
        titulo.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        // Grid con los campos
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(10));

        int row = 0;

        // Número
        grid.add(new Label("Número *:"), 0, row);
        TextField txtNumeroModal = new TextField();
        grid.add(txtNumeroModal, 1, row++);

        // Carátula
        grid.add(new Label("Carátula *:"), 0, row);
        TextField txtCaratulaModal = new TextField();
        txtCaratulaModal.setPrefWidth(300);
        grid.add(txtCaratulaModal, 1, row++);

        // Cliente
        grid.add(new Label("Cliente *:"), 0, row);
        ComboBox<Cliente> cmbClienteModal = new ComboBox<>();
        cmbClienteModal.setPrefWidth(300);
        try {
            List<Cliente> clientes = clienteService.listarActivos();
            cmbClienteModal.setItems(FXCollections.observableArrayList(clientes));
            cmbClienteModal.setConverter(new javafx.util.StringConverter<Cliente>() {
                @Override
                public String toString(Cliente cliente) {
                    return cliente != null ? cliente.getNombreCompleto() : "";
                }
                @Override
                public Cliente fromString(String string) {
                    return null;
                }
            });
        } catch (SQLException e) {
            mostrarError("Error al cargar clientes: " + e.getMessage());
        }
        // Preseleccionar cliente si viene de la ficha del cliente
        if (clientePreseleccionado != null) {
            cmbClienteModal.getItems().stream()
                    .filter(c -> c.getId().equals(clientePreseleccionado.getId()))
                    .findFirst()
                    .ifPresent(cmbClienteModal::setValue);
        }
        grid.add(cmbClienteModal, 1, row++);

        // Actor
        grid.add(new Label("Actor:"), 0, row);
        TextField txtActorModal = new TextField();
        grid.add(txtActorModal, 1, row++);

        // Demandado
        grid.add(new Label("Demandado:"), 0, row);
        TextField txtDemandadoModal = new TextField();
        grid.add(txtDemandadoModal, 1, row++);

        // Fuero
        grid.add(new Label("Fuero:"), 0, row);
        ComboBox<String> cmbFueroModal = new ComboBox<>();
        cmbFueroModal.setItems(FXCollections.observableArrayList(
                "Civil", "Penal", "Laboral", "Comercial", "Familia", "Contencioso Administrativo"
        ));
        cmbFueroModal.setPrefWidth(300);
        grid.add(cmbFueroModal, 1, row++);

        // Juzgado
        grid.add(new Label("Juzgado:"), 0, row);
        TextField txtJuzgadoModal = new TextField();
        grid.add(txtJuzgadoModal, 1, row++);

        // Secretaría
        grid.add(new Label("Secretaría:"), 0, row);
        TextField txtSecretariaModal = new TextField();
        grid.add(txtSecretariaModal, 1, row++);

        // Estado
        grid.add(new Label("Estado *:"), 0, row);
        ComboBox<EstadoExpediente> cmbEstadoModal = new ComboBox<>();
        cmbEstadoModal.setItems(FXCollections.observableArrayList(EstadoExpediente.values()));
        cmbEstadoModal.setValue(EstadoExpediente.ACTIVO);
        cmbEstadoModal.setPrefWidth(300);
        grid.add(cmbEstadoModal, 1, row++);

        // Fecha Inicio
        grid.add(new Label("Fecha Inicio *:"), 0, row);
        DatePicker dpFechaInicioModal = new DatePicker(LocalDate.now());
        dpFechaInicioModal.setPrefWidth(300);
        grid.add(dpFechaInicioModal, 1, row++);

        // Monto Estimado
        grid.add(new Label("Monto Estimado:"), 0, row);
        TextField txtMontoModal = new TextField();
        grid.add(txtMontoModal, 1, row++);

        // Observaciones
        grid.add(new Label("Observaciones:"), 0, row);
        TextArea txtObsModal = new TextArea();
        txtObsModal.setPrefRowCount(3);
        txtObsModal.setPrefWidth(300);
        grid.add(txtObsModal, 1, row++);

        // Botones
        HBox botones = new HBox(10);
        botones.setAlignment(Pos.CENTER);
        botones.setPadding(new Insets(15, 0, 0, 0));

        Button btnGuardar = new Button("💾 Guardar");
        btnGuardar.getStyleClass().addAll("button", "button-success");
        btnGuardar.setOnAction(e -> {
            try {
                // Validar campos obligatorios
                if (txtNumeroModal.getText().trim().isEmpty()) {
                    mostrarAdvertencia("El número es obligatorio");
                    return;
                }
                if (txtCaratulaModal.getText().trim().isEmpty()) {
                    mostrarAdvertencia("La carátula es obligatoria");
                    return;
                }
                if (cmbClienteModal.getValue() == null) {
                    mostrarAdvertencia("Debe seleccionar un cliente");
                    return;
                }

                // Crear expediente
                Expediente exp = new Expediente();
                exp.setNumero(txtNumeroModal.getText().trim().toUpperCase());
                exp.setCaratula(txtCaratulaModal.getText().trim());

                Cliente cliente = cmbClienteModal.getValue();
                exp.setCliente(cliente.getNombreCompleto());
                exp.setClienteId(cliente.getId());

                exp.setActor(txtActorModal.getText().trim());
                exp.setDemandado(txtDemandadoModal.getText().trim());
                exp.setFuero(cmbFueroModal.getValue());
                exp.setJuzgado(txtJuzgadoModal.getText().trim());
                exp.setSecretaria(txtSecretariaModal.getText().trim());
                exp.setEstado(cmbEstadoModal.getValue());
                exp.setFechaInicio(dpFechaInicioModal.getValue());
                exp.setObservaciones(txtObsModal.getText().trim());
                exp.setCreadorId(SesionUsuario.getUsuarioActual().getId());

                if (!txtMontoModal.getText().trim().isEmpty()) {
                    exp.setMontoEstimado(Double.parseDouble(txtMontoModal.getText().trim()));
                }

                expedienteService.crearExpediente(exp);
                mostrarInfo("Expediente creado correctamente");
                cargarExpedientes();
                cargarDashboard();
                ventana.close();

            } catch (NumberFormatException ex) {
                mostrarError("El monto debe ser un número válido");
            } catch (Exception ex) {
                mostrarError("Error al guardar: " + ex.getMessage());
                ex.printStackTrace();
            }
        });

        Button btnCancelar = new Button("❌ Cancelar");
        btnCancelar.setOnAction(e -> ventana.close());

        botones.getChildren().addAll(btnGuardar, btnCancelar);

        root.getChildren().addAll(titulo, new Separator(), grid, botones);

        ScrollPane scroll = new ScrollPane(root);
        scroll.setFitToWidth(true);

        Scene scene = new Scene(scroll, 500, 700);
        scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
        ventana.setScene(scene);
        ventana.showAndWait();
    }

    private void abrirVentanaDetalleExpediente() {
        if (expedienteSeleccionado == null) {
            mostrarAdvertencia("Seleccione un expediente");
            return;
        }

        Stage ventana = new Stage();
        ventana.initModality(Modality.APPLICATION_MODAL);
        ventana.setTitle("Expediente: " + expedienteSeleccionado.getNumero());

        BorderPane root = new BorderPane();

        // TabPane con las diferentes secciones
        TabPane tabPane = new TabPane();

        // Pestaña 1: Datos del Expediente
        Tab tabDatos = new Tab("📋 Datos");
        tabDatos.setClosable(false);
        tabDatos.setContent(crearPanelDatosExpediente());

        // Pestaña 2: Movimientos
        Tab tabMovimientos = new Tab("📝 Movimientos");
        tabMovimientos.setClosable(false);
        tabMovimientos.setContent(crearPanelMovimientosExpediente());

        // Pestaña 3: Documentos
        Tab tabDocumentos = new Tab("📎 Documentos");
        tabDocumentos.setClosable(false);
        tabDocumentos.setContent(crearPanelDocumentosExpediente());

        // Pestaña 4: Economía
        Tab tabEconomia = new Tab("💰 Economía");
        tabEconomia.setClosable(false);
        tabEconomia.setContent(crearPanelEconomiaExpediente());

        tabPane.getTabs().addAll(tabDatos, tabMovimientos, tabDocumentos, tabEconomia);

        root.setCenter(tabPane);

        // Botón cerrar
        HBox footer = new HBox();
        footer.setAlignment(Pos.CENTER);
        footer.setPadding(new Insets(10));

        Button btnCerrar = new Button("✅ Cerrar");
        btnCerrar.setOnAction(e -> ventana.close());

        footer.getChildren().add(btnCerrar);
        root.setBottom(footer);

        // ✅ CAMBIOS AQUÍ: Tamaño 1000x650 en lugar de 1000x1000
        Scene scene = new Scene(root, 1000, 650);
        scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());

        ventana.setScene(scene);
        ventana.setResizable(true);    // ✅ Permitir redimensionar
        ventana.setMaximized(false);   // ✅ NO maximizar automáticamente
        ventana.centerOnScreen();      // ✅ Centrar en pantalla

        ventana.showAndWait();
    }

    private VBox crearPanelDatosExpediente() {
        VBox panel = new VBox(15);
        panel.setPadding(new Insets(20));

        GridPane grid = new GridPane();
        grid.setHgap(15);
        grid.setVgap(12);

        int row = 0;

        // Estilo para labels
        String labelStyle = "-fx-font-weight: bold; -fx-text-fill: #34495e;";
        String valueStyle = "-fx-text-fill: #2c3e50;";

        // Número
        Label lblNum = new Label("Número:");
        lblNum.setStyle(labelStyle);
        Label valNum = new Label(expedienteSeleccionado.getNumero());
        valNum.setStyle(valueStyle);
        grid.add(lblNum, 0, row);
        grid.add(valNum, 1, row++);

        // Carátula
        Label lblCar = new Label("Carátula:");
        lblCar.setStyle(labelStyle);
        Label valCar = new Label(expedienteSeleccionado.getCaratula());
        valCar.setStyle(valueStyle);
        valCar.setWrapText(true);
        valCar.setMaxWidth(400);
        grid.add(lblCar, 0, row);
        grid.add(valCar, 1, row++);

        // Cliente
        Label lblCli = new Label("Cliente:");
        lblCli.setStyle(labelStyle);
        Label valCli = new Label(expedienteSeleccionado.getCliente());
        valCli.setStyle(valueStyle);
        grid.add(lblCli, 0, row);
        grid.add(valCli, 1, row++);

        // Actor
        if (expedienteSeleccionado.getActor() != null && !expedienteSeleccionado.getActor().isEmpty()) {
            Label lblAct = new Label("Actor:");
            lblAct.setStyle(labelStyle);
            Label valAct = new Label(expedienteSeleccionado.getActor());
            valAct.setStyle(valueStyle);
            grid.add(lblAct, 0, row);
            grid.add(valAct, 1, row++);
        }

        // Demandado
        if (expedienteSeleccionado.getDemandado() != null && !expedienteSeleccionado.getDemandado().isEmpty()) {
            Label lblDem = new Label("Demandado:");
            lblDem.setStyle(labelStyle);
            Label valDem = new Label(expedienteSeleccionado.getDemandado());
            valDem.setStyle(valueStyle);
            grid.add(lblDem, 0, row);
            grid.add(valDem, 1, row++);
        }

        // Fuero
        if (expedienteSeleccionado.getFuero() != null) {
            Label lblFue = new Label("Fuero:");
            lblFue.setStyle(labelStyle);
            Label valFue = new Label(expedienteSeleccionado.getFuero());
            valFue.setStyle(valueStyle);
            grid.add(lblFue, 0, row);
            grid.add(valFue, 1, row++);
        }

        // Juzgado
        if (expedienteSeleccionado.getJuzgado() != null && !expedienteSeleccionado.getJuzgado().isEmpty()) {
            Label lblJuz = new Label("Juzgado:");
            lblJuz.setStyle(labelStyle);
            Label valJuz = new Label(expedienteSeleccionado.getJuzgado());
            valJuz.setStyle(valueStyle);
            grid.add(lblJuz, 0, row);
            grid.add(valJuz, 1, row++);
        }

        // Secretaría
        if (expedienteSeleccionado.getSecretaria() != null && !expedienteSeleccionado.getSecretaria().isEmpty()) {
            Label lblSec = new Label("Secretaría:");
            lblSec.setStyle(labelStyle);
            Label valSec = new Label(expedienteSeleccionado.getSecretaria());
            valSec.setStyle(valueStyle);
            grid.add(lblSec, 0, row);
            grid.add(valSec, 1, row++);
        }

        // Estado
        Label lblEst = new Label("Estado:");
        lblEst.setStyle(labelStyle);
        Label valEst = new Label(expedienteSeleccionado.getEstado().toString());
        valEst.setStyle(valueStyle + " -fx-font-weight: bold;");
        grid.add(lblEst, 0, row);
        grid.add(valEst, 1, row++);

        // Fecha Inicio
        Label lblFec = new Label("Fecha Inicio:");
        lblFec.setStyle(labelStyle);
        Label valFec = new Label(expedienteSeleccionado.getFechaInicio().toString());
        valFec.setStyle(valueStyle);
        grid.add(lblFec, 0, row);
        grid.add(valFec, 1, row++);

        // Monto Estimado
        if (expedienteSeleccionado.getMontoEstimado() != null) {
            Label lblMon = new Label("Monto Estimado:");
            lblMon.setStyle(labelStyle);
            Label valMon = new Label(formatearMoneda(expedienteSeleccionado.getMontoEstimado()));
            valMon.setStyle(valueStyle);
            grid.add(lblMon, 0, row);
            grid.add(valMon, 1, row++);
        }

        // Observaciones
        if (expedienteSeleccionado.getObservaciones() != null && !expedienteSeleccionado.getObservaciones().isEmpty()) {
            Label lblObs = new Label("Observaciones:");
            lblObs.setStyle(labelStyle);
            TextArea valObs = new TextArea(expedienteSeleccionado.getObservaciones());
            valObs.setEditable(false);
            valObs.setPrefRowCount(3);
            valObs.setWrapText(true);
            grid.add(lblObs, 0, row);
            grid.add(valObs, 1, row++);
        }

        panel.getChildren().add(grid);

        ScrollPane scroll = new ScrollPane(panel);
        scroll.setFitToWidth(true);

        return panel;
    }

    private VBox crearPanelMovimientosExpediente() {
        VBox panel = new VBox(10);
        panel.setPadding(new Insets(15));

        // Botón nuevo movimiento
        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_RIGHT);

        Button btnNuevo = new Button("➕ Nuevo Movimiento");
        btnNuevo.getStyleClass().addAll("button", "button-success");

        header.getChildren().add(btnNuevo);

        // Tabla de movimientos
        TableView<Movimiento> tabla = new TableView<>();
        ObservableList<Movimiento> lista = FXCollections.observableArrayList();
        tabla.setItems(lista);

        TableColumn<Movimiento, LocalDate> colFecha = new TableColumn<>("Fecha");
        colFecha.setCellValueFactory(new PropertyValueFactory<>("fecha"));
        colFecha.setPrefWidth(100);

        TableColumn<Movimiento, TipoMovimiento> colTipo = new TableColumn<>("Tipo");
        colTipo.setCellValueFactory(new PropertyValueFactory<>("tipo"));
        colTipo.setPrefWidth(120);

        TableColumn<Movimiento, String> colDesc = new TableColumn<>("Descripción");
        colDesc.setCellValueFactory(new PropertyValueFactory<>("descripcion"));
        colDesc.setPrefWidth(400);

        TableColumn<Movimiento, String> colCuad = new TableColumn<>("Cuaderno");
        colCuad.setCellValueFactory(new PropertyValueFactory<>("cuaderno"));
        colCuad.setPrefWidth(100);

        tabla.getColumns().addAll(colFecha, colTipo, colDesc, colCuad);

        // Cargar movimientos
        try {
            List<Movimiento> movimientos = movimientoService.listarPorExpediente(expedienteSeleccionado.getId());
            lista.addAll(movimientos);
        } catch (SQLException e) {
            mostrarError("Error al cargar movimientos: " + e.getMessage());
        }

        // Evento del botón nuevo
        btnNuevo.setOnAction(e -> abrirFormularioMovimiento(null, lista));

        // Botones de acción
        HBox botones = new HBox(10);
        botones.setAlignment(Pos.CENTER);
        botones.setPadding(new Insets(10, 0, 0, 0));

        Button btnEditar = new Button("✏️ Editar");
        btnEditar.setOnAction(e -> {
            Movimiento sel = tabla.getSelectionModel().getSelectedItem();
            if (sel != null) {
                abrirFormularioMovimiento(sel, lista);
            } else {
                mostrarAdvertencia("Seleccione un movimiento");
            }
        });

        Button btnEliminar = new Button("🗑️ Eliminar");
        btnEliminar.getStyleClass().add("btn-danger");
        btnEliminar.setOnAction(e -> {
            Movimiento sel = tabla.getSelectionModel().getSelectedItem();
            if (sel != null) {
                Alert confirmacion = new Alert(Alert.AlertType.CONFIRMATION);
                confirmacion.setTitle("Confirmar");
                confirmacion.setHeaderText("¿Eliminar movimiento?");
                confirmacion.setContentText("Esta acción no se puede deshacer");

                if (confirmacion.showAndWait().get() == ButtonType.OK) {
                    try {
                        movimientoService.eliminarMovimiento(sel.getId());
                        lista.remove(sel);
                        mostrarInfo("Movimiento eliminado");
                    } catch (SQLException ex) {
                        mostrarError("Error al eliminar: " + ex.getMessage());
                    }
                }
            } else {
                mostrarAdvertencia("Seleccione un movimiento");
            }
        });

        botones.getChildren().addAll(btnEditar, btnEliminar);

        panel.getChildren().addAll(header, tabla, botones);
        VBox.setVgrow(tabla, Priority.ALWAYS);

        return panel;
    }

    private VBox crearPanelDocumentosExpediente() {
        VBox panel = new VBox(10);
        panel.setPadding(new Insets(15));

        // Botón subir documento
        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_RIGHT);

        Button btnSubir = new Button("📎 Subir Documento");
        btnSubir.getStyleClass().addAll("button", "button-success");

        header.getChildren().add(btnSubir);

        // Tabla de documentos
        TableView<DocumentoExpediente> tabla = new TableView<>();
        ObservableList<DocumentoExpediente> lista = FXCollections.observableArrayList();
        tabla.setItems(lista);

        TableColumn<DocumentoExpediente, String> colNombre = new TableColumn<>("Nombre");
        colNombre.setCellValueFactory(new PropertyValueFactory<>("nombreOriginal"));
        colNombre.setPrefWidth(300);

        TableColumn<DocumentoExpediente, String> colTipo = new TableColumn<>("Tipo");
        colTipo.setCellValueFactory(new PropertyValueFactory<>("tipoDocumento"));
        colTipo.setPrefWidth(150);

        TableColumn<DocumentoExpediente, String> colTamanio = new TableColumn<>("Tamaño");
        colTamanio.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getTamanioFormateado()));
        colTamanio.setPrefWidth(100);

        TableColumn<DocumentoExpediente, LocalDateTime> colFecha = new TableColumn<>("Fecha");
        colFecha.setCellValueFactory(new PropertyValueFactory<>("fechaSubida"));
        colFecha.setPrefWidth(150);

        tabla.getColumns().addAll(colNombre, colTipo, colTamanio, colFecha);

        // Cargar documentos
        try {
            List<DocumentoExpediente> docs = documentoExpedienteService.listarPorExpediente(expedienteSeleccionado.getId());
            lista.addAll(docs);
        } catch (SQLException e) {
            mostrarError("Error al cargar documentos: " + e.getMessage());
        }

        // Evento subir documento
        btnSubir.setOnAction(e -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Seleccionar Documento");
            fileChooser.getExtensionFilters().addAll(
                    new FileChooser.ExtensionFilter("Documentos", "*.pdf", "*.doc", "*.docx", "*.txt"),
                    new FileChooser.ExtensionFilter("Todos los archivos", "*.*")
            );

            File archivo = fileChooser.showOpenDialog(stage);
            if (archivo != null) {
                // Pedir tipo y descripción
                TextInputDialog dialogTipo = new TextInputDialog("General");
                dialogTipo.setTitle("Tipo de Documento");
                dialogTipo.setHeaderText("Ingrese el tipo de documento");
                dialogTipo.setContentText("Tipo:");

                Optional<String> resultTipo = dialogTipo.showAndWait();
                if (resultTipo.isPresent()) {
                    TextInputDialog dialogDesc = new TextInputDialog("");
                    dialogDesc.setTitle("Descripción");
                    dialogDesc.setHeaderText("Descripción del documento (opcional)");
                    dialogDesc.setContentText("Descripción:");

                    String descripcion = dialogDesc.showAndWait().orElse("");

                    try {
                        DocumentoExpediente doc = documentoExpedienteService.subirDocumento(
                                expedienteSeleccionado.getId(),
                                archivo,
                                resultTipo.get(),
                                descripcion,
                                SesionUsuario.getUsuarioActual().getId()
                        );

                        lista.add(doc);
                        mostrarInfo("Documento subido correctamente");

                    } catch (SQLException | IOException ex) {
                        mostrarError("Error al subir documento: " + ex.getMessage());
                    }
                }
            }
        });

        // Botones de acción
        HBox botones = new HBox(10);
        botones.setAlignment(Pos.CENTER);
        botones.setPadding(new Insets(10, 0, 0, 0));

        Button btnAbrir = new Button("👁️ Abrir");
        btnAbrir.setOnAction(e -> {
            DocumentoExpediente sel = tabla.getSelectionModel().getSelectedItem();
            if (sel != null) {
                try {
                    documentoExpedienteService.abrirDocumento(sel.getId());
                } catch (SQLException | IOException ex) {
                    mostrarError("Error al abrir documento: " + ex.getMessage());
                }
            } else {
                mostrarAdvertencia("Seleccione un documento");
            }
        });

        Button btnEliminar = new Button("🗑️ Eliminar");
        btnEliminar.getStyleClass().add("btn-danger");
        btnEliminar.setOnAction(e -> {
            DocumentoExpediente sel = tabla.getSelectionModel().getSelectedItem();
            if (sel != null) {
                Alert confirmacion = new Alert(Alert.AlertType.CONFIRMATION);
                confirmacion.setTitle("Confirmar");
                confirmacion.setHeaderText("¿Eliminar documento?");
                confirmacion.setContentText("El archivo se eliminará permanentemente");

                if (confirmacion.showAndWait().get() == ButtonType.OK) {
                    try {
                        documentoExpedienteService.eliminarDocumento(sel.getId());
                        lista.remove(sel);
                        mostrarInfo("Documento eliminado");
                    } catch (SQLException | IOException ex) {
                        mostrarError("Error al eliminar: " + ex.getMessage());
                    }
                }
            } else {
                mostrarAdvertencia("Seleccione un documento");
            }
        });

        botones.getChildren().addAll(btnAbrir, btnEliminar);

        panel.getChildren().addAll(header, tabla, botones);
        VBox.setVgrow(tabla, Priority.ALWAYS);

        return panel;
    }

    private VBox crearPanelEconomiaExpediente() {
        VBox panel = new VBox(20);
        panel.setPadding(new Insets(20));

        Label titulo = new Label("Economía del Expediente");
        titulo.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

        // ===== TARJETAS DE RESUMEN =====
        HBox tarjetas = new HBox(15);
        tarjetas.setAlignment(Pos.CENTER_LEFT);

        try {
            double totalHonorarios = honorarioService.calcularTotalPorExpediente(expedienteSeleccionado.getId());
            double totalGastos     = gastoService.calcularTotalPorExpediente(expedienteSeleccionado.getId());
            double totalPagos      = pagoService.calcularTotalPorExpediente(expedienteSeleccionado.getId());

            // Cuotas
            double totalCuotasAcordado = 0;
            double totalCuotasPagado   = 0;
            List<Cuota> cuotas = cuotaService.listarCuotasPorExpediente(expedienteSeleccionado.getId());
            for (Cuota c : cuotas) {
                totalCuotasAcordado += c.getMontoTotalAcordado();
                totalCuotasPagado   += c.getMontoPagado();
            }

            double totalDebido = totalHonorarios + totalGastos + totalCuotasAcordado;
            double totalAbonado = totalPagos + totalCuotasPagado;
            double saldo = totalDebido - totalAbonado;

            tarjetas.getChildren().addAll(
                    crearTarjetaFinanciera("Honorarios",    formatearMoneda(totalHonorarios),    "#3498db"),
                    crearTarjetaFinanciera("Gastos",        formatearMoneda(totalGastos),        "#e74c3c"),
                    crearTarjetaFinanciera("Pagos",         formatearMoneda(totalAbonado),       "#27ae60"),
                    crearTarjetaFinanciera("Saldo Pendiente", formatearMoneda(saldo),
                            saldo > 0 ? "#f39c12" : "#27ae60")
            );
        } catch (SQLException e) {
            Label lblError = new Label("Error al cargar resumen: " + e.getMessage());
            lblError.setStyle("-fx-text-fill: red;");
            tarjetas.getChildren().add(lblError);
        }

        // ===== PLAN DE CUOTAS (se mantiene acá) =====
        Label lblCuotas = new Label("Plan de Cuotas");
        lblCuotas.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-padding: 10 0 5 0;");

        ScrollPane scrollCuotas = new ScrollPane(crearPanelCuotasDetalle());
        scrollCuotas.setFitToWidth(true);
        scrollCuotas.setStyle("-fx-background-color: transparent;");
        scrollCuotas.setPrefHeight(320);

        // ===== BOTÓN IR A ECONOMÍA =====
        Separator sep = new Separator();

        Label lblInfo = new Label(
                "Para registrar o editar honorarios, gastos y pagos de este expediente usá el módulo de Economía. " +
                        "El expediente quedará preseleccionado automáticamente.");
        lblInfo.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 12px;");
        lblInfo.setWrapText(true);

        Button btnIrEconomia = new Button("📊 Ir a Gestión Económica");
        btnIrEconomia.setStyle(
                "-fx-background-color: #185FA5; -fx-text-fill: white; " +
                        "-fx-font-weight: bold; -fx-padding: 10 20; -fx-background-radius: 6; -fx-cursor: hand;");
        btnIrEconomia.setOnAction(e -> {
            // Preseleccionar el expediente en el combo del panel global
            if (cmbExpedientesEconomia != null && expedienteSeleccionado != null) {
                cmbExpedientesEconomia.setValue(expedienteSeleccionado);
            }
            // Navegar al panel de Economía
            if (contentArea != null && viewEconomia != null) {
                contentArea.getChildren().setAll(viewEconomia);
                if (botonesNav != null) {
                    for (Button b : botonesNav) b.setStyle(estiloNavItem(false));
                    botonesNav[4].setStyle(estiloNavItem(true)); // índice 4 = Economía
                }
            }
        });

        panel.getChildren().addAll(titulo, tarjetas, lblCuotas, scrollCuotas,
                sep, lblInfo, btnIrEconomia);
        return panel;
    }

    private VBox crearResumenEconomico() {
        VBox panel = new VBox(5);
        panel.setPadding(new Insets(10));
        panel.getStyleClass().add("card");

        Label titulo = new Label("💰 Resumen Económico");
        titulo.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");

        // ✅ GRID COMPACTO: 2 columnas x 4 filas
        GridPane grid = new GridPane();
        grid.setHgap(30); // Espacio horizontal entre columnas
        grid.setVgap(5);  // Espacio vertical entre filas
        grid.setPadding(new Insets(5, 0, 5, 0));

        int row = 0;

        // Calcular totales
        double totalCuotasAcordado = 0;
        double totalCuotasPagado = 0;
        double totalHonorarios = 0;
        double totalOtrosPagos = 0;
        double totalGastos = 0;

        try {
            List<Cuota> cuotas = cuotaService.listarCuotasPorExpediente(expedienteSeleccionado.getId());
            for (Cuota c : cuotas) {
                totalCuotasAcordado += c.getMontoTotalAcordado();
                totalCuotasPagado += c.getMontoPagado();
            }

            List<Honorario> honorarios = honorarioService.listarPorExpediente(expedienteSeleccionado.getId());
            for (Honorario h : honorarios) {
                totalHonorarios += h.getMontoCalculado();
            }

            List<Pago> pagos = pagoService.listarPorExpediente(expedienteSeleccionado.getId());
            for (Pago p : pagos) {
                totalOtrosPagos += p.getMonto();
            }

            List<Gasto> gastos = gastoService.listarPorExpediente(expedienteSeleccionado.getId());
            for (Gasto g : gastos) {
                totalGastos += g.getMonto();
            }
        } catch (SQLException e) {
            mostrarError("Error: " + e.getMessage());
        }

        double totalPagosRecibidos = totalCuotasPagado + totalOtrosPagos;
        double pendienteCobro = (totalCuotasAcordado + totalHonorarios) - totalPagosRecibidos;

        // COLUMNA 1
        grid.add(crearLabelInfo("Plan Cuotas:", formatearMoneda(totalCuotasAcordado), "#9b59b6"), 0, row++);
        grid.add(crearLabelInfo("Cuotas Pagadas:", formatearMoneda(totalCuotasPagado), "#27ae60"), 0, row++);
        grid.add(crearLabelInfo("Honorarios:", formatearMoneda(totalHonorarios), "#2980b9"), 0, row++);
        grid.add(crearLabelInfo("Otros Pagos:", formatearMoneda(totalOtrosPagos), "#16a085"), 0, row++);

        // COLUMNA 2
        row = 0;
        grid.add(crearLabelInfo("Gastos:", formatearMoneda(totalGastos), "#e67e22"), 1, row++);
        grid.add(crearLabelInfo("Total Recibido:", formatearMoneda(totalPagosRecibidos), "#27ae60"), 1, row++);
        grid.add(crearLabelInfo("Pendiente:", formatearMoneda(pendienteCobro), pendienteCobro > 0 ? "#e74c3c" : "#95a5a6"), 1, row++);
        // Fila vacía para igualar alturas
        grid.add(new Label(""), 1, row++);

        panel.getChildren().addAll(titulo, grid);

        return panel;
    }

    // ✅ MÉTODO AUXILIAR para crear labels compactos
    private HBox crearLabelInfo(String etiqueta, String valor, String color) {
        HBox box = new HBox(5);
        box.setAlignment(Pos.CENTER_LEFT);

        Label lbl = new Label(etiqueta);
        lbl.setStyle("-fx-font-size: 11px;");
        lbl.setPrefWidth(120);

        Label val = new Label(valor);
        val.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: " + color + ";");

        box.getChildren().addAll(lbl, val);
        return box;
    }

    private VBox crearPanelCuotasDetalle() {
        VBox panel = new VBox(10);
        panel.setPadding(new Insets(10));

        // Dos botones separados
        HBox botonesHeader = new HBox(10);
        botonesHeader.setAlignment(Pos.CENTER_LEFT);

        Button btnNuevoPlan = new Button("📋 Nuevo Plan de Cuotas");
        btnNuevoPlan.getStyleClass().addAll("button", "button-success");

        Button btnRegistrarCuota = new Button("💵 Registrar Cuota Individual");
        btnRegistrarCuota.getStyleClass().addAll("button", "button-info");

        botonesHeader.getChildren().addAll(btnNuevoPlan, btnRegistrarCuota);

        // Tabla de cuotas
        TableView<Cuota> tabla = new TableView<>();
        tabla.setPrefHeight(300); // ✅ AGREGAR ESTA LÍNEA
        tabla.setMaxHeight(400);  // ✅ AGREGAR ESTA LÍNEA
        ObservableList<Cuota> lista = FXCollections.observableArrayList();
        tabla.setItems(lista);

        TableColumn<Cuota, LocalDate> colFecha = new TableColumn<>("Fecha Acuerdo");
        colFecha.setCellValueFactory(new PropertyValueFactory<>("fechaAcuerdo"));
        colFecha.setPrefWidth(120);

        TableColumn<Cuota, Double> colTotal = new TableColumn<>("Monto Total");
        colTotal.setCellValueFactory(new PropertyValueFactory<>("montoTotalAcordado"));
        colTotal.setPrefWidth(120);

        TableColumn<Cuota, Double> colPagado = new TableColumn<>("Pagado");
        colPagado.setCellValueFactory(new PropertyValueFactory<>("montoPagado"));
        colPagado.setPrefWidth(120);

        TableColumn<Cuota, String> colPendiente = new TableColumn<>("Pendiente");
        colPendiente.setCellValueFactory(cellData ->
                new SimpleStringProperty(formatearMoneda(cellData.getValue().getSaldoPendiente())));
        colPendiente.setPrefWidth(120);

        TableColumn<Cuota, String> colProgreso = new TableColumn<>("Progreso");
        colProgreso.setCellValueFactory(cellData ->
                new SimpleStringProperty(String.format("%.1f%%", cellData.getValue().getPorcentajePagado())));
        colProgreso.setPrefWidth(100);

        TableColumn<Cuota, String> colEstado = new TableColumn<>("Estado");
        colEstado.setCellValueFactory(new PropertyValueFactory<>("estado"));
        colEstado.setPrefWidth(100);

        tabla.getColumns().addAll(colFecha, colTotal, colPagado, colPendiente, colProgreso, colEstado);

        // Cargar cuotas
        try {
            List<Cuota> cuotas = cuotaService.listarCuotasPorExpediente(expedienteSeleccionado.getId());
            lista.addAll(cuotas);
        } catch (SQLException e) {
            mostrarError("Error: " + e.getMessage());
        }

        // Evento botón nuevo plan de cuotas
        btnNuevoPlan.setOnAction(e -> {
            abrirFormularioNuevoPlanCuotas();
            try {
                List<Cuota> cuotas = cuotaService.listarCuotasPorExpediente(expedienteSeleccionado.getId());
                lista.clear();
                lista.addAll(cuotas);
            } catch (SQLException ex) {
                mostrarError("Error: " + ex.getMessage());
            }
        });

        // Evento botón registrar cuota individual
        btnRegistrarCuota.setOnAction(e -> {
            abrirFormularioCuotaIndividual();
            try {
                List<Cuota> cuotas = cuotaService.listarCuotasPorExpediente(expedienteSeleccionado.getId());
                lista.clear();
                lista.addAll(cuotas);
            } catch (SQLException ex) {
                mostrarError("Error: " + ex.getMessage());
            }
        });

        // Botones de acción
        HBox botones = new HBox(10);
        botones.setAlignment(Pos.CENTER);
        botones.setPadding(new Insets(10, 0, 0, 0));

        Button btnVerPagos = new Button("👁️ Ver Pagos");
        btnVerPagos.setOnAction(e -> {
            Cuota sel = tabla.getSelectionModel().getSelectedItem();
            if (sel != null) {
                abrirVentanaPagosCuota(sel, lista);
            } else {
                mostrarAdvertencia("Seleccione una cuota");
            }
        });

        Button btnEliminar = new Button("🗑️ Eliminar");
        btnEliminar.getStyleClass().add("btn-danger");
        btnEliminar.setOnAction(e -> {
            Cuota sel = tabla.getSelectionModel().getSelectedItem();
            if (sel != null) {
                Alert confirmacion = new Alert(Alert.AlertType.CONFIRMATION);
                confirmacion.setTitle("Confirmar");
                confirmacion.setHeaderText("¿Eliminar cuota?");
                confirmacion.setContentText("Se eliminarán también todos los pagos asociados");

                if (confirmacion.showAndWait().get() == ButtonType.OK) {
                    try {
                        cuotaService.eliminarCuota(sel.getId());
                        lista.remove(sel);
                        mostrarInfo("Cuota eliminada");
                    } catch (SQLException ex) {
                        mostrarError("Error: " + ex.getMessage());
                    }
                }
            } else {
                mostrarAdvertencia("Seleccione una cuota");
            }
        });

        botones.getChildren().addAll(btnVerPagos, btnEliminar);

        panel.getChildren().addAll(botonesHeader, tabla, botones);
        VBox.setVgrow(tabla, Priority.ALWAYS);

        return panel;
    }

    private void abrirFormularioNuevoPlanCuotas() {
        Stage ventana = new Stage();
        ventana.initModality(Modality.APPLICATION_MODAL);
        ventana.setTitle("Nuevo Plan de Cuotas");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));

        int row = 0;

        grid.add(new Label("Fecha de Acuerdo *:"), 0, row);
        DatePicker dpFecha = new DatePicker(LocalDate.now());
        grid.add(dpFecha, 1, row++);

        grid.add(new Label("Monto Total Acordado *:"), 0, row);
        TextField txtTotal = new TextField();
        txtTotal.setPromptText("Ej: 50000");
        grid.add(txtTotal, 1, row++);

        grid.add(new Label("Cantidad de Cuotas:"), 0, row);
        TextField txtCantidad = new TextField();
        txtCantidad.setPromptText("Ej: 10");
        grid.add(txtCantidad, 1, row++);

        grid.add(new Label("Monto por Cuota:"), 0, row);
        TextField txtMontoCuota = new TextField();
        txtMontoCuota.setPromptText("Ej: 5000");
        grid.add(txtMontoCuota, 1, row++);

        Label lblInfo = new Label("ℹ️ Plan de pagos con cuotas. Los pagos pueden variar en monto.");
        lblInfo.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 11px;");
        lblInfo.setWrapText(true);
        lblInfo.setMaxWidth(350);
        grid.add(lblInfo, 0, row++, 2, 1);

        grid.add(new Label("Observaciones:"), 0, row);
        TextArea txtObs = new TextArea();
        txtObs.setPrefRowCount(3);
        txtObs.setPromptText("Condiciones del acuerdo...");
        grid.add(txtObs, 1, row++);

        HBox botones = new HBox(10);
        botones.setAlignment(Pos.CENTER);
        botones.setPadding(new Insets(15, 0, 0, 0));

        Button btnGuardar = new Button("💾 Guardar");
        btnGuardar.getStyleClass().addAll("button", "button-success");
        btnGuardar.setOnAction(e -> {
            try {
                if (txtTotal.getText().trim().isEmpty()) {
                    mostrarAdvertencia("El monto total es obligatorio");
                    return;
                }

                Cuota cuota = new Cuota();
                cuota.setExpedienteId(expedienteSeleccionado.getId());
                cuota.setFechaAcuerdo(dpFecha.getValue());
                cuota.setMontoTotalAcordado(Double.parseDouble(txtTotal.getText().trim()));

                if (!txtCantidad.getText().trim().isEmpty()) {
                    cuota.setCantidadCuotasPlanificadas(Integer.parseInt(txtCantidad.getText().trim()));
                }

                if (!txtMontoCuota.getText().trim().isEmpty()) {
                    cuota.setMontoPorCuota(Double.parseDouble(txtMontoCuota.getText().trim()));
                }

                cuota.setObservaciones(txtObs.getText().trim());
                cuota.setUsuarioId(SesionUsuario.getUsuarioActual().getId());

                cuotaService.crearCuota(cuota);
                mostrarInfo("Plan de cuotas creado correctamente");
                ventana.close();

            } catch (NumberFormatException ex) {
                mostrarError("Los montos deben ser números válidos");
            } catch (Exception ex) {
                mostrarError("Error: " + ex.getMessage());
            }
        });

        Button btnCancelar = new Button("❌ Cancelar");
        btnCancelar.setOnAction(e -> ventana.close());

        botones.getChildren().addAll(btnGuardar, btnCancelar);

        VBox root = new VBox(15);
        root.getChildren().addAll(
                new Label("Crear Plan de Cuotas"),
                new Separator(),
                grid,
                botones
        );
        root.setPadding(new Insets(20));

        Scene scene = new Scene(root);
        scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
        ventana.setScene(scene);
        ventana.showAndWait();
    }

    private void abrirFormularioCuotaIndividual() {
        Stage ventana = new Stage();
        ventana.initModality(Modality.APPLICATION_MODAL);
        ventana.setTitle("Registrar Cuota Individual");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));

        int row = 0;

        grid.add(new Label("Fecha *:"), 0, row);
        DatePicker dpFecha = new DatePicker(LocalDate.now());
        grid.add(dpFecha, 1, row++);

        grid.add(new Label("Monto *:"), 0, row);
        TextField txtMonto = new TextField();
        txtMonto.setPromptText("Ej: 10000");
        grid.add(txtMonto, 1, row++);

        Label lblInfo = new Label("ℹ️ Registro de una única cuota sin plan de pagos");
        lblInfo.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 11px;");
        lblInfo.setWrapText(true);
        lblInfo.setMaxWidth(350);
        grid.add(lblInfo, 0, row++, 2, 1);

        grid.add(new Label("Observaciones:"), 0, row);
        TextArea txtObs = new TextArea();
        txtObs.setPrefRowCount(2);
        grid.add(txtObs, 1, row++);

        HBox botones = new HBox(10);
        botones.setAlignment(Pos.CENTER);
        botones.setPadding(new Insets(15, 0, 0, 0));

        Button btnGuardar = new Button("💾 Guardar");
        btnGuardar.getStyleClass().addAll("button", "button-success");
        btnGuardar.setOnAction(e -> {
            try {
                if (txtMonto.getText().trim().isEmpty()) {
                    mostrarAdvertencia("El monto es obligatorio");
                    return;
                }

                Cuota cuota = new Cuota();
                cuota.setExpedienteId(expedienteSeleccionado.getId());
                cuota.setFechaAcuerdo(dpFecha.getValue());
                cuota.setMontoTotalAcordado(Double.parseDouble(txtMonto.getText().trim()));
                cuota.setMontoPagado(Double.parseDouble(txtMonto.getText().trim())); // Ya pagada
                cuota.setEstado("COMPLETADO"); // Cuota individual ya está completa
                cuota.setObservaciones(txtObs.getText().trim());
                cuota.setUsuarioId(SesionUsuario.getUsuarioActual().getId());

                cuotaService.crearCuota(cuota);
                mostrarInfo("Cuota registrada correctamente");
                ventana.close();

            } catch (NumberFormatException ex) {
                mostrarError("El monto debe ser un número válido");
            } catch (Exception ex) {
                mostrarError("Error: " + ex.getMessage());
            }
        });

        Button btnCancelar = new Button("❌ Cancelar");
        btnCancelar.setOnAction(e -> ventana.close());

        botones.getChildren().addAll(btnGuardar, btnCancelar);

        VBox root = new VBox(15);
        root.getChildren().addAll(
                new Label("Registrar Cuota Individual"),
                new Separator(),
                grid,
                botones
        );
        root.setPadding(new Insets(20));

        Scene scene = new Scene(root);
        scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
        ventana.setScene(scene);
        ventana.showAndWait();
    }


    private void abrirFormularioNuevaCuota() {
        Stage ventana = new Stage();
        ventana.initModality(Modality.APPLICATION_MODAL);
        ventana.setTitle("Nueva Cuota / Plan de Pago");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));

        int row = 0;

        // Fecha Acuerdo
        grid.add(new Label("Fecha de Acuerdo *:"), 0, row);
        DatePicker dpFecha = new DatePicker(LocalDate.now());
        grid.add(dpFecha, 1, row++);

        // Monto Total
        grid.add(new Label("Monto Total Acordado *:"), 0, row);
        TextField txtTotal = new TextField();
        txtTotal.setPromptText("Ej: 50000");
        grid.add(txtTotal, 1, row++);

        // Cantidad de Cuotas (opcional)
        grid.add(new Label("Cantidad de Cuotas:"), 0, row);
        TextField txtCantidad = new TextField();
        txtCantidad.setPromptText("Opcional - Ej: 10");
        grid.add(txtCantidad, 1, row++);

        // Monto por Cuota (opcional)
        grid.add(new Label("Monto por Cuota:"), 0, row);
        TextField txtMontoCuota = new TextField();
        txtMontoCuota.setPromptText("Opcional - Ej: 5000");
        grid.add(txtMontoCuota, 1, row++);

        Label lblInfo = new Label("ℹ️ Los pagos pueden ser de montos variables, no necesariamente fijos");
        lblInfo.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 11px;");
        lblInfo.setWrapText(true);
        lblInfo.setMaxWidth(350);
        grid.add(lblInfo, 0, row++, 2, 1);

        // Observaciones
        grid.add(new Label("Observaciones:"), 0, row);
        TextArea txtObs = new TextArea();
        txtObs.setPrefRowCount(3);
        txtObs.setPromptText("Condiciones del acuerdo, forma de pago, etc.");
        grid.add(txtObs, 1, row++);

        // Botones
        HBox botones = new HBox(10);
        botones.setAlignment(Pos.CENTER);
        botones.setPadding(new Insets(15, 0, 0, 0));

        Button btnGuardar = new Button("💾 Guardar");
        btnGuardar.getStyleClass().addAll("button", "button-success");
        btnGuardar.setOnAction(e -> {
            try {
                if (txtTotal.getText().trim().isEmpty()) {
                    mostrarAdvertencia("El monto total es obligatorio");
                    return;
                }

                Cuota cuota = new Cuota();
                cuota.setExpedienteId(expedienteSeleccionado.getId());
                cuota.setFechaAcuerdo(dpFecha.getValue());
                cuota.setMontoTotalAcordado(Double.parseDouble(txtTotal.getText().trim()));

                if (!txtCantidad.getText().trim().isEmpty()) {
                    cuota.setCantidadCuotasPlanificadas(Integer.parseInt(txtCantidad.getText().trim()));
                }

                if (!txtMontoCuota.getText().trim().isEmpty()) {
                    cuota.setMontoPorCuota(Double.parseDouble(txtMontoCuota.getText().trim()));
                }

                cuota.setObservaciones(txtObs.getText().trim());
                cuota.setUsuarioId(SesionUsuario.getUsuarioActual().getId());

                cuotaService.crearCuota(cuota);
                mostrarInfo("Cuota / Plan de pago creado correctamente");
                ventana.close();

            } catch (NumberFormatException ex) {
                mostrarError("Los montos deben ser números válidos");
            } catch (Exception ex) {
                mostrarError("Error: " + ex.getMessage());
            }
        });

        Button btnCancelar = new Button("❌ Cancelar");
        btnCancelar.setOnAction(e -> ventana.close());

        botones.getChildren().addAll(btnGuardar, btnCancelar);

        VBox root = new VBox(15);
        root.getChildren().addAll(
                new Label("Crear Plan de Cuotas / Acuerdo de Pago"),
                new Separator(),
                grid,
                botones
        );
        root.setPadding(new Insets(20));

        Scene scene = new Scene(root);
        scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
        ventana.setScene(scene);
        ventana.showAndWait();
    }

    private void abrirVentanaPagosCuota(Cuota cuota, ObservableList<Cuota> listaCuotas) {
        Stage ventana = new Stage();
        ventana.initModality(Modality.APPLICATION_MODAL);
        ventana.setTitle("Pagos de Cuota");

        BorderPane root = new BorderPane();
        root.setPadding(new Insets(15));

        // Header con info de la cuota
        VBox header = new VBox(5);
        header.setPadding(new Insets(10));
        header.setStyle("-fx-background-color: #8e44ad;");

        Label lblInfo = new Label(String.format("Cuota: $%.2f | Pagado: $%.2f | Pendiente: $%.2f",
                cuota.getMontoTotalAcordado(), cuota.getMontoPagado(), cuota.getSaldoPendiente()));
        lblInfo.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: white;");

        Label lblProgreso = new Label(String.format("Progreso: %.1f%%", cuota.getPorcentajePagado()));
        lblProgreso.setStyle("-fx-text-fill: white;");

        header.getChildren().addAll(lblInfo, lblProgreso);
        root.setTop(header);

        // Tabla de pagos
        TableView<PagoCuota> tabla = new TableView<>();
        ObservableList<PagoCuota> listaPagos = FXCollections.observableArrayList();
        tabla.setItems(listaPagos);

        TableColumn<PagoCuota, LocalDate> colFecha = new TableColumn<>("Fecha");
        colFecha.setCellValueFactory(new PropertyValueFactory<>("fechaPago"));
        colFecha.setPrefWidth(100);

        TableColumn<PagoCuota, Double> colMonto = new TableColumn<>("Monto");
        colMonto.setCellValueFactory(new PropertyValueFactory<>("monto"));
        colMonto.setPrefWidth(100);

        TableColumn<PagoCuota, String> colForma = new TableColumn<>("Forma de Pago");
        colForma.setCellValueFactory(new PropertyValueFactory<>("formaPago"));
        colForma.setPrefWidth(120);

        TableColumn<PagoCuota, Integer> colNumero = new TableColumn<>("Nro. Cuota");
        colNumero.setCellValueFactory(new PropertyValueFactory<>("numeroCuota"));
        colNumero.setPrefWidth(80);

        TableColumn<PagoCuota, String> colRef = new TableColumn<>("Referencia");
        colRef.setCellValueFactory(new PropertyValueFactory<>("referencia"));
        colRef.setPrefWidth(150);

        tabla.getColumns().addAll(colFecha, colMonto, colForma, colNumero, colRef);

        // Cargar pagos
        try {
            List<PagoCuota> pagos = cuotaService.listarPagosDeCuota(cuota.getId());
            listaPagos.addAll(pagos);
        } catch (SQLException e) {
            mostrarError("Error: " + e.getMessage());
        }

        root.setCenter(tabla);

        // Botones
        HBox botones = new HBox(10);
        botones.setAlignment(Pos.CENTER);
        botones.setPadding(new Insets(10));

        Button btnNuevo = new Button("➕ Registrar Pago");
        btnNuevo.getStyleClass().addAll("button", "button-success");
        btnNuevo.setOnAction(e -> {
            registrarPagoCuota(cuota, listaPagos, listaCuotas, lblInfo, lblProgreso);
        });

        Button btnEliminar = new Button("🗑️ Eliminar");
        btnEliminar.getStyleClass().add("btn-danger");
        btnEliminar.setOnAction(e -> {
            PagoCuota sel = tabla.getSelectionModel().getSelectedItem();
            if (sel != null) {
                try {
                    cuotaService.eliminarPagoCuota(sel.getId(), sel.getCuotaId(), sel.getMonto());
                    listaPagos.remove(sel);

                    // Recargar cuota actualizada
                    Optional<Cuota> cuotaActualizada = cuotaService.buscarCuotaPorId(cuota.getId());
                    if (cuotaActualizada.isPresent()) {
                        Cuota c = cuotaActualizada.get();
                        lblInfo.setText(String.format("Cuota: $%.2f | Pagado: $%.2f | Pendiente: $%.2f",
                                c.getMontoTotalAcordado(), c.getMontoPagado(), c.getSaldoPendiente()));
                        lblProgreso.setText(String.format("Progreso: %.1f%%", c.getPorcentajePagado()));

                        // Actualizar en la lista principal
                        int idx = listaCuotas.indexOf(cuota);
                        if (idx >= 0) {
                            listaCuotas.set(idx, c);
                        }
                    }

                    mostrarInfo("Pago eliminado");
                } catch (SQLException ex) {
                    mostrarError("Error: " + ex.getMessage());
                }
            }
        });

        Button btnCerrar = new Button("✅ Cerrar");
        btnCerrar.setOnAction(e -> ventana.close());

        botones.getChildren().addAll(btnNuevo, btnEliminar, btnCerrar);
        root.setBottom(botones);

        Scene scene = new Scene(root, 700, 500);
        scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
        ventana.setScene(scene);
        ventana.showAndWait();
    }


    private void registrarPagoCuota(Cuota cuota, ObservableList<PagoCuota> listaPagos,
                                    ObservableList<Cuota> listaCuotas, Label lblInfo, Label lblProgreso) {
        Stage ventana = new Stage();
        ventana.initModality(Modality.APPLICATION_MODAL);
        ventana.setTitle("Registrar Pago de Cuota");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));

        int row = 0;

        grid.add(new Label("Fecha *:"), 0, row);
        DatePicker dpFecha = new DatePicker(LocalDate.now());
        grid.add(dpFecha, 1, row++);

        grid.add(new Label("Monto *:"), 0, row);
        TextField txtMonto = new TextField();
        txtMonto.setPromptText(String.format("Pendiente: $%.2f", cuota.getSaldoPendiente()));
        grid.add(txtMonto, 1, row++);

        grid.add(new Label("Forma de Pago:"), 0, row);
        ComboBox<String> cmbForma = new ComboBox<>();
        cmbForma.setItems(FXCollections.observableArrayList(
                "Efectivo", "Transferencia", "Cheque", "Tarjeta", "Otro"
        ));
        cmbForma.setValue("Efectivo");
        grid.add(cmbForma, 1, row++);

        grid.add(new Label("Nro. de Cuota:"), 0, row);
        TextField txtNumero = new TextField();
        txtNumero.setPromptText("Opcional - Ej: 1, 2, 3...");
        grid.add(txtNumero, 1, row++);

        grid.add(new Label("Referencia:"), 0, row);
        TextField txtRef = new TextField();
        txtRef.setPromptText("Nro. operación, cheque, etc.");
        grid.add(txtRef, 1, row++);

        grid.add(new Label("Observaciones:"), 0, row);
        TextArea txtObs = new TextArea();
        txtObs.setPrefRowCount(2);
        grid.add(txtObs, 1, row++);

        HBox botones = new HBox(10);
        botones.setAlignment(Pos.CENTER);
        botones.setPadding(new Insets(15, 0, 0, 0));

        Button btnGuardar = new Button("💾 Guardar");
        btnGuardar.getStyleClass().addAll("button", "button-success");
        btnGuardar.setOnAction(e -> {
            try {
                if (txtMonto.getText().trim().isEmpty()) {
                    mostrarAdvertencia("El monto es obligatorio");
                    return;
                }

                PagoCuota pago = new PagoCuota();
                pago.setCuotaId(cuota.getId());
                pago.setFechaPago(dpFecha.getValue());
                pago.setMonto(Double.parseDouble(txtMonto.getText().trim()));
                pago.setFormaPago(cmbForma.getValue());

                if (!txtNumero.getText().trim().isEmpty()) {
                    pago.setNumeroCuota(Integer.parseInt(txtNumero.getText().trim()));
                }

                pago.setReferencia(txtRef.getText().trim());
                pago.setObservaciones(txtObs.getText().trim());
                pago.setUsuarioId(SesionUsuario.getUsuarioActual().getId());

                cuotaService.registrarPagoCuota(pago);
                listaPagos.add(pago);

                // Recargar cuota actualizada
                Optional<Cuota> cuotaActualizada = cuotaService.buscarCuotaPorId(cuota.getId());
                if (cuotaActualizada.isPresent()) {
                    Cuota c = cuotaActualizada.get();
                    lblInfo.setText(String.format("Cuota: $%.2f | Pagado: $%.2f | Pendiente: $%.2f",
                            c.getMontoTotalAcordado(), c.getMontoPagado(), c.getSaldoPendiente()));
                    lblProgreso.setText(String.format("Progreso: %.1f%%", c.getPorcentajePagado()));

                    // Actualizar en la lista principal
                    int idx = listaCuotas.indexOf(cuota);
                    if (idx >= 0) {
                        listaCuotas.set(idx, c);
                    }
                }

                mostrarInfo("Pago registrado correctamente");
                ventana.close();

            } catch (NumberFormatException ex) {
                mostrarError("El monto debe ser un número válido");
            } catch (Exception ex) {
                mostrarError("Error: " + ex.getMessage());
            }
        });

        Button btnCancelar = new Button("❌ Cancelar");
        btnCancelar.setOnAction(e -> ventana.close());

        botones.getChildren().addAll(btnGuardar, btnCancelar);

        VBox root = new VBox(15);
        root.getChildren().addAll(
                new Label("Registrar Pago de Cuota"),
                new Separator(),
                grid,
                botones
        );
        root.setPadding(new Insets(20));

        Scene scene = new Scene(root);
        scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
        ventana.setScene(scene);
        ventana.showAndWait();
    }



    private VBox crearPanelHonorariosDetalle() {
        VBox panel = new VBox(10);
        panel.setPadding(new Insets(10));

        // Tabla de honorarios
        TableView<Honorario> tabla = new TableView<>();
        ObservableList<Honorario> lista = FXCollections.observableArrayList();
        tabla.setItems(lista);

        TableColumn<Honorario, String> colTipo = new TableColumn<>("Tipo");
        colTipo.setCellValueFactory(new PropertyValueFactory<>("tipo"));
        colTipo.setPrefWidth(150);

        TableColumn<Honorario, Double> colMonto = new TableColumn<>("Monto");
        colMonto.setCellValueFactory(new PropertyValueFactory<>("montoCalculado"));
        colMonto.setPrefWidth(120);

        TableColumn<Honorario, String> colDesc = new TableColumn<>("Descripción");
        colDesc.setCellValueFactory(new PropertyValueFactory<>("descripcion"));
        colDesc.setPrefWidth(300);

        TableColumn<Honorario, String> colEstado = new TableColumn<>("Estado");
        colEstado.setCellValueFactory(new PropertyValueFactory<>("estado"));
        colEstado.setPrefWidth(100);

        tabla.getColumns().addAll(colTipo, colMonto, colDesc, colEstado);

        // Cargar datos iniciales
        try {
            List<Honorario> hons = honorarioService.listarPorExpediente(expedienteSeleccionado.getId());
            lista.addAll(hons);
        } catch (SQLException e) {
            mostrarError("Error: " + e.getMessage());
        }

        // Botón nuevo honorario
        Button btnNuevo = new Button("➕ Nuevo Honorario");
        btnNuevo.getStyleClass().add("btn-primary");
        btnNuevo.setOnAction(e -> {
            abrirFormularioHonorario(null, expedienteSeleccionado.getId(), lista);
            // Recargar la lista
            try {
                List<Honorario> hons = honorarioService.listarPorExpediente(expedienteSeleccionado.getId());
                lista.clear();
                lista.addAll(hons);
            } catch (SQLException ex) {
                mostrarError("Error al recargar: " + ex.getMessage());
            }
        });

        // Botón editar
        Button btnEditar = new Button("✏️ Editar");
        btnEditar.setOnAction(e -> {
            Honorario sel = tabla.getSelectionModel().getSelectedItem();
            if (sel != null) {
                abrirFormularioHonorario(sel, expedienteSeleccionado.getId(),lista);
                // Recargar
                try {
                    List<Honorario> hons = honorarioService.listarPorExpediente(expedienteSeleccionado.getId());
                    lista.clear();
                    lista.addAll(hons);
                } catch (SQLException ex) {
                    mostrarError("Error: " + ex.getMessage());
                }
            } else {
                mostrarAdvertencia("Seleccione un honorario");
            }
        });

        // Botón eliminar
        Button btnEliminar = new Button("🗑️ Eliminar");
        btnEliminar.getStyleClass().add("btn-danger");
        btnEliminar.setOnAction(e -> {
            Honorario sel = tabla.getSelectionModel().getSelectedItem();
            if (sel != null) {
                Alert confirmacion = new Alert(Alert.AlertType.CONFIRMATION);
                confirmacion.setTitle("Confirmar");
                confirmacion.setHeaderText("¿Eliminar honorario?");

                if (confirmacion.showAndWait().get() == ButtonType.OK) {
                    try {
                        honorarioService.eliminarHonorario(sel.getId());
                        lista.remove(sel);
                        mostrarInfo("Honorario eliminado");
                    } catch (SQLException ex) {
                        mostrarError("Error: " + ex.getMessage());
                    }
                }
            } else {
                mostrarAdvertencia("Seleccione un honorario");
            }
        });

        HBox botones = new HBox(10);
        botones.setAlignment(Pos.CENTER);
        botones.setPadding(new Insets(10, 0, 0, 0));
        botones.getChildren().addAll(btnEditar, btnEliminar);

        panel.getChildren().addAll(btnNuevo, tabla, botones);
        VBox.setVgrow(tabla, Priority.ALWAYS);

        return panel;
    }

    private VBox crearPanelPagosDetalle() {
        VBox panel = new VBox(10);
        panel.setPadding(new Insets(10));

        // Tabla de pagos
        TableView<Pago> tabla = new TableView<>();
        ObservableList<Pago> lista = FXCollections.observableArrayList();
        tabla.setItems(lista);

        TableColumn<Pago, LocalDate> colFecha = new TableColumn<>("Fecha");
        colFecha.setCellValueFactory(new PropertyValueFactory<>("fecha"));
        colFecha.setPrefWidth(100);

        TableColumn<Pago, Double> colMonto = new TableColumn<>("Monto");
        colMonto.setCellValueFactory(new PropertyValueFactory<>("monto"));
        colMonto.setPrefWidth(120);

        TableColumn<Pago, String> colForma = new TableColumn<>("Forma de Pago");
        colForma.setCellValueFactory(new PropertyValueFactory<>("formaPago"));
        colForma.setPrefWidth(150);

        TableColumn<Pago, String> colConcepto = new TableColumn<>("Concepto");
        colConcepto.setCellValueFactory(new PropertyValueFactory<>("concepto"));
        colConcepto.setPrefWidth(300);

        TableColumn<Pago, Void> colAcciones = new TableColumn<>("Acciones");
        colAcciones.setPrefWidth(120);
        colAcciones.setCellFactory(param -> new TableCell<>() {
            private final Button btnEditar = new Button("✏️");
            private final Button btnEliminar = new Button("🗑️");

            {
                btnEditar.setOnAction(e -> {
                    Pago p = getTableView().getItems().get(getIndex());
                    abrirFormularioPago(p, expedienteSeleccionado, lista);
                });

                btnEliminar.getStyleClass().add("btn-danger");
                btnEliminar.setOnAction(e -> {
                    Pago p = getTableView().getItems().get(getIndex());
                    if (mostrarConfirmacion("¿Eliminar este pago?")) {
                        try {
                            pagoService.eliminarPago(p.getId());
                            lista.remove(p);
                            mostrarInfo("Pago eliminado");
                        } catch (SQLException ex) {
                            mostrarError("Error: " + ex.getMessage());
                        }
                    }
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : new HBox(5, btnEditar, btnEliminar));
            }
        });

        tabla.getColumns().addAll(colFecha, colMonto, colForma, colConcepto, colAcciones);


        // Cargar datos iniciales
        try {
            List<Pago> pagos = pagoService.listarPorExpediente(expedienteSeleccionado.getId());
            lista.addAll(pagos);
        } catch (SQLException e) {
            mostrarError("Error: " + e.getMessage());
        }

        // Botón nuevo pago
        Button btnNuevo = new Button("➕ Registrar Pago");
        btnNuevo.getStyleClass().addAll("button", "button-success");
        btnNuevo.setOnAction(e -> abrirFormularioPago(null, expedienteSeleccionado, lista));


        panel.getChildren().addAll(btnNuevo, tabla);
        VBox.setVgrow(tabla, Priority.ALWAYS);

        return panel;
    }

    private VBox crearPanelGastosDetalle() {
        VBox panel = new VBox(10);
        panel.setPadding(new Insets(10));

        // Tabla de gastos
        TableView<Gasto> tabla = new TableView<>();
        ObservableList<Gasto> lista = FXCollections.observableArrayList();
        tabla.setItems(lista);

        TableColumn<Gasto, LocalDate> colFecha = new TableColumn<>("Fecha");
        colFecha.setCellValueFactory(new PropertyValueFactory<>("fecha"));
        colFecha.setPrefWidth(100);

        TableColumn<Gasto, String> colConcepto = new TableColumn<>("Concepto");
        colConcepto.setCellValueFactory(new PropertyValueFactory<>("concepto"));
        colConcepto.setPrefWidth(250);

        TableColumn<Gasto, Double> colMonto = new TableColumn<>("Monto");
        colMonto.setCellValueFactory(new PropertyValueFactory<>("monto"));
        colMonto.setPrefWidth(120);

        TableColumn<Gasto, String> colCat = new TableColumn<>("Categoría");
        colCat.setCellValueFactory(new PropertyValueFactory<>("categoria"));
        colCat.setPrefWidth(150);

        tabla.getColumns().addAll(colFecha, colConcepto, colMonto, colCat);

        // Cargar datos iniciales
        try {
            List<Gasto> gastos = gastoService.listarPorExpediente(expedienteSeleccionado.getId());
            lista.addAll(gastos);
        } catch (SQLException e) {
            mostrarError("Error: " + e.getMessage());
        }

        // Botón nuevo gasto
        Button btnNuevo = new Button("➕ Nuevo Gasto");
        btnNuevo.getStyleClass().addAll("button", "button-success");
        btnNuevo.setOnAction(e -> {
            abrirFormularioGasto(null, expedienteSeleccionado.getId(), lista);
            // Recargar la lista
            try {
                List<Gasto> gastos = gastoService.listarPorExpediente(expedienteSeleccionado.getId());
                lista.clear();
                lista.addAll(gastos);
            } catch (SQLException ex) {
                mostrarError("Error al recargar: " + ex.getMessage());
            }
        });

        // Botón editar
        Button btnEditar = new Button("✏️ Editar");
        btnEditar.setOnAction(e -> {
            Gasto sel = tabla.getSelectionModel().getSelectedItem();
            if (sel != null) {
                abrirFormularioGasto(sel, expedienteSeleccionado.getId(), lista);
                // Recargar
                try {
                    List<Gasto> gastos = gastoService.listarPorExpediente(expedienteSeleccionado.getId());
                    lista.clear();
                    lista.addAll(gastos);
                } catch (SQLException ex) {
                    mostrarError("Error: " + ex.getMessage());
                }
            } else {
                mostrarAdvertencia("Seleccione un gasto");
            }
        });

        // Botón eliminar
        Button btnEliminar = new Button("🗑️ Eliminar");
        btnEliminar.getStyleClass().add("btn-danger");
        btnEliminar.setOnAction(e -> {
            Gasto sel = tabla.getSelectionModel().getSelectedItem();
            if (sel != null) {
                Alert confirmacion = new Alert(Alert.AlertType.CONFIRMATION);
                confirmacion.setTitle("Confirmar");
                confirmacion.setHeaderText("¿Eliminar gasto?");

                if (confirmacion.showAndWait().get() == ButtonType.OK) {
                    try {
                        gastoService.eliminarGasto(sel.getId());
                        lista.remove(sel);
                        mostrarInfo("Gasto eliminado");
                    } catch (SQLException ex) {
                        mostrarError("Error: " + ex.getMessage());
                    }
                }
            } else {
                mostrarAdvertencia("Seleccione un gasto");
            }
        });

        HBox botones = new HBox(10);
        botones.setAlignment(Pos.CENTER);
        botones.setPadding(new Insets(10, 0, 0, 0));
        botones.getChildren().addAll(btnEditar, btnEliminar);

        panel.getChildren().addAll(btnNuevo, tabla, botones);
        VBox.setVgrow(tabla, Priority.ALWAYS);

        return panel;
    }

    private void abrirFormularioPago(Pago pago, Expediente expediente, ObservableList<Pago> lista) {
        boolean esEdicion = (pago != null);

        Stage ventana = new Stage();
        ventana.initModality(Modality.APPLICATION_MODAL);
        ventana.setTitle(esEdicion ? "Editar Pago" : "Registrar Pago");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));

        int row = 0;

        // Fecha
        grid.add(new Label("Fecha *:"), 0, row);
        DatePicker dpFecha = new DatePicker(esEdicion ? pago.getFecha() : LocalDate.now());
        grid.add(dpFecha, 1, row++);

        // Monto
        grid.add(new Label("Monto *:"), 0, row);
        TextField txtMonto = new TextField();
        if (esEdicion) txtMonto.setText(String.valueOf(pago.getMonto()));
        grid.add(txtMonto, 1, row++);

        // Forma de Pago
        grid.add(new Label("Forma de Pago:"), 0, row);
        ComboBox<String> cmbFormaPago = new ComboBox<>();
        cmbFormaPago.setItems(FXCollections.observableArrayList(
                "Efectivo", "Transferencia", "Cheque", "Tarjeta", "Otro"
        ));
        cmbFormaPago.setValue(esEdicion ? pago.getFormaPago() : "Efectivo");
        grid.add(cmbFormaPago, 1, row++);

        // Referencia
        grid.add(new Label("Referencia:"), 0, row);
        TextField txtRef = new TextField();
        txtRef.setPromptText("Nro. de operación, cheque, etc.");
        if (esEdicion) txtRef.setText(pago.getReferencia());
        grid.add(txtRef, 1, row++);

        // Concepto
        grid.add(new Label("Concepto:"), 0, row);
        TextField txtConcepto = new TextField();
        txtConcepto.setPromptText("Concepto del pago");
        if (esEdicion) txtConcepto.setText(pago.getConcepto());
        grid.add(txtConcepto, 1, row++);

        // Observaciones
        grid.add(new Label("Observaciones:"), 0, row);
        TextArea txtObs = new TextArea();
        txtObs.setPrefRowCount(3);
        if (esEdicion) txtObs.setText(pago.getObservaciones());
        grid.add(txtObs, 1, row++);

        // Botones
        HBox botones = new HBox(10);
        botones.setAlignment(Pos.CENTER);
        botones.setPadding(new Insets(15, 0, 0, 0));

        Button btnGuardar = new Button("💾 Guardar");
        btnGuardar.getStyleClass().addAll("button", "button-success");
        btnGuardar.setOnAction(e -> {
            try {
                if (txtMonto.getText().trim().isEmpty()) {
                    mostrarAdvertencia("El monto es obligatorio");
                    return;
                }

                Pago p = esEdicion ? pago : new Pago();
                p.setExpedienteId(expediente.getId());
                p.setClienteId(expediente.getClienteId());
                if (!esEdicion) {
                    p.setUsuarioId(SesionUsuario.getUsuarioActual().getId());
                }
                p.setFecha(dpFecha.getValue());
                p.setMonto(Double.parseDouble(txtMonto.getText().trim()));
                p.setFormaPago(cmbFormaPago.getValue());
                p.setReferencia(txtRef.getText().trim());
                p.setConcepto(txtConcepto.getText().trim());
                p.setObservaciones(txtObs.getText().trim());

                if (esEdicion) {
                    pagoService.actualizarPago(p);
                    mostrarInfo("Pago actualizado correctamente");
                } else {
                    pagoService.crearPago(p);
                    mostrarInfo("Pago registrado correctamente");
                }

                // Recargar la lista
                if (lista != null) {
                    List<Pago> pagos = pagoService.listarPorExpediente(expediente.getId());
                    lista.clear();
                    lista.addAll(pagos);
                }
                actualizarResumenFinanciero();
                ventana.close();

            } catch (NumberFormatException ex) {
                mostrarError("El monto debe ser un número válido");
            } catch (Exception ex) {
                mostrarError("Error: " + ex.getMessage());
                ex.printStackTrace();
            }
        });

        Button btnCancelar = new Button("❌ Cancelar");
        btnCancelar.setOnAction(e -> ventana.close());

        botones.getChildren().addAll(btnGuardar, btnCancelar);

        VBox root = new VBox(15);
        root.getChildren().addAll(
                new Label(esEdicion ? "Editar Pago" : "Registrar Pago"),
                new Separator(),
                grid,
                botones
        );
        root.setPadding(new Insets(20));

        Scene scene = new Scene(root);
        scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
        ventana.setScene(scene);
        ventana.showAndWait();
    }

    private VBox crearFormularioExpediente() {
        VBox form = new VBox(10);
        form.setPadding(new Insets(10));
        form.getStyleClass().add("panel-formulario");

        Label lblTitulo = new Label("Datos del Expediente");
        lblTitulo.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

        txtNumero = new TextField();
        txtCaratula = new TextField();

        // ============ COMBO DE CLIENTES ============
        cmbCliente = new ComboBox<>(); // ✅ Usar la variable de instancia declarada en línea 60
        cmbCliente.setPromptText("Seleccione un cliente...");
        cmbCliente.setMaxWidth(Double.MAX_VALUE);

        // Mostrar nombre completo en el ComboBox
        cmbCliente.setConverter(new javafx.util.StringConverter<Cliente>() {
            @Override
            public String toString(Cliente cliente) {
                return cliente != null ? cliente.getNombreCompleto() : "";
            }

            @Override
            public Cliente fromString(String string) {
                return null;
            }
        });

        // Botón para crear cliente rápido
        Button btnNuevoClienteRapido = new Button("➕");
        btnNuevoClienteRapido.getStyleClass().addAll("button", "button-success");
        btnNuevoClienteRapido.setOnAction(e -> {
            abrirFormularioCliente(null);
            cargarComboClientes(); // Recargar después de crear
        });

        HBox clienteBox = new HBox(5, cmbCliente, btnNuevoClienteRapido);
        HBox.setHgrow(cmbCliente, Priority.ALWAYS);

        txtCliente = new TextField();
        txtCliente.setEditable(false);
        txtCliente.setStyle("-fx-background-color: #e8e8e8;");

        // Cuando selecciona un cliente del combo, actualizar el TextField
        cmbCliente.setOnAction(e -> {
            Cliente clienteSel = cmbCliente.getValue();
            if (clienteSel != null) {
                txtCliente.setText(clienteSel.getNombreCompleto());
            }
        });

        // Cargar clientes al inicio
        cargarComboClientes();
        // ============ FIN COMBO DE CLIENTES ============

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
                new Label("Seleccionar Cliente:"), clienteBox,
                new Label("Cliente *:"), txtCliente,
                new Label("Demandado:"), txtDemandado,
                new Label("Fuero:"), cmbFuero,
                new Label("Juzgado:"), txtJuzgado,
                new Label("Secretaría:"), txtSecretaria,
                new Label("Estado *:"), cmbEstado,
                new Label("Fecha Inicio *:"), dpFechaInicio,
                new Label("Monto Estimado:"), txtMontoEstimado,
                new Label("Observaciones:"), txtObservaciones,
                crearBotonesFormularioExpediente() // ✅ SIN parámetro
        );

        ScrollPane scroll = new ScrollPane(form);
        scroll.setFitToWidth(true);

        VBox container = new VBox(scroll);
        VBox.setVgrow(scroll, Priority.ALWAYS);

        return container;
    }

    // ✅ MÉTODO AUXILIAR PARA CARGAR CLIENTES EN EL COMBO
    private void cargarComboClientes() {
        try {
            List<Cliente> clientes = clienteService.listarActivos();
            cmbCliente.setItems(FXCollections.observableArrayList(clientes));
        } catch (SQLException e) {
            System.err.println("Error al cargar clientes: " + e.getMessage());
            mostrarError("Error al cargar clientes: " + e.getMessage());
        }
    }

    // Método auxiliar para cargar clientes en el combo
    private void cargarComboClientes(ComboBox<Cliente> combo) {
        try {
            List<Cliente> clientes = clienteService.listarActivos();
            combo.setItems(FXCollections.observableArrayList(clientes));
        } catch (SQLException e) {
            System.err.println("Error al cargar clientes: " + e.getMessage());
        }
    }

    private HBox crearBotonesFormularioExpediente() { // ✅ SIN parámetro
        HBox botones = new HBox(10);
        botones.setAlignment(Pos.CENTER);
        botones.setPadding(new Insets(10, 0, 0, 0));

        Button btnGuardar = new Button("💾 Guardar");
        btnGuardar.getStyleClass().addAll("button", "button-success");
        btnGuardar.setOnAction(e -> guardarExpediente()); // ✅ Usar el método modificado

        Button btnNuevo = new Button("📄 Nuevo");
        btnNuevo.getStyleClass().add("button");
        btnNuevo.setOnAction(e -> limpiarFormularioExpediente());

        Button btnEliminar = new Button("🗑️ Eliminar");
        btnEliminar.getStyleClass().add("btn-danger");
        btnEliminar.setOnAction(e -> eliminarExpediente());

        Button btnMovimientos = new Button("📋 Ver Movimientos");
        btnMovimientos.getStyleClass().addAll("button", "button-info");
        btnMovimientos.setOnAction(e -> abrirVentanaMovimientos());

        botones.getChildren().addAll(btnGuardar, btnNuevo, btnEliminar, btnMovimientos);
        return botones;
    }

    // Nuevo método para guardar con cliente vinculado
    private void guardarExpedienteConCliente(ComboBox<Cliente> cmbClientes) {
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

            // ============ VINCULAR CLIENTE ============
            Cliente clienteSeleccionado = cmbClientes.getValue();
            if (clienteSeleccionado != null) {
                expediente.setClienteId(clienteSeleccionado.getId());
            }
            // ============ FIN VINCULAR ============

            if (!txtMontoEstimado.getText().trim().isEmpty()) {
                try {
                    expediente.setMontoEstimado(Double.parseDouble(txtMontoEstimado.getText().trim()));
                } catch (NumberFormatException ex) {
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

        } catch (IllegalArgumentException ex) {
            mostrarAdvertencia(ex.getMessage());
        } catch (SQLException ex) {
            mostrarError("Error de base de datos: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    private VBox crearPanelTablaExpedientes() {
        VBox panel = new VBox(12);

        HBox panelBusqueda = new HBox(10);
        panelBusqueda.setAlignment(Pos.CENTER_LEFT);

        Label lblBuscar = new Label("Buscar:");
        lblBuscar.getStyleClass().add("text-secondary");
        txtBuscar = new TextField();
        txtBuscar.setPromptText("Número, cliente...");
        txtBuscar.getStyleClass().add("search-field");
        txtBuscar.textProperty().addListener((obs, old, val) -> buscarExpedientes());

        Label lblFiltro = new Label("Estado:");
        lblFiltro.getStyleClass().add("text-secondary");
        cmbFiltroEstado = new ComboBox<>();
        cmbFiltroEstado.setItems(FXCollections.observableArrayList(EstadoExpediente.values()));
        cmbFiltroEstado.setPromptText("Todos");
        cmbFiltroEstado.setOnAction(e -> buscarExpedientes());

        Button btnLimpiarFiltro = new Button("Limpiar");
        btnLimpiarFiltro.getStyleClass().add("btn-ghost");
        btnLimpiarFiltro.setOnAction(e -> {
            txtBuscar.clear();
            cmbFiltroEstado.setValue(null);
            cargarExpedientes();
        });

        Region spacerBusqueda = new Region();
        HBox.setHgrow(spacerBusqueda, Priority.ALWAYS);

        Button btnExportarExp = new Button("Exportar Excel");
        btnExportarExp.getStyleClass().add("button-info");
        btnExportarExp.setOnAction(e -> exportarExpedientesExcel());

        panelBusqueda.getChildren().addAll(lblBuscar, txtBuscar, lblFiltro, cmbFiltroEstado,
                btnLimpiarFiltro, spacerBusqueda, btnExportarExp);

        tablaExpedientes = new TableView<>();
        tablaExpedientes.setItems(listaExpedientes);
        tablaExpedientes.getStyleClass().add("table-view");
        // Reparte el ancho entre las columnas (elimina el espacio vacío a la derecha)
        tablaExpedientes.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<Expediente, String> colNumero = new TableColumn<>("Número");
        colNumero.setCellValueFactory(new PropertyValueFactory<>("numero"));
        colNumero.setMaxWidth(1f * Integer.MAX_VALUE * 12); // peso 12%

        TableColumn<Expediente, String> colCaratula = new TableColumn<>("Carátula");
        colCaratula.setCellValueFactory(new PropertyValueFactory<>("caratula"));
        colCaratula.setMaxWidth(1f * Integer.MAX_VALUE * 38); // peso 38%

        TableColumn<Expediente, String> colCliente = new TableColumn<>("Cliente");
        colCliente.setCellValueFactory(new PropertyValueFactory<>("cliente"));
        colCliente.setMaxWidth(1f * Integer.MAX_VALUE * 25); // peso 25%

        TableColumn<Expediente, EstadoExpediente> colEstado = new TableColumn<>("Estado");
        colEstado.setCellValueFactory(new PropertyValueFactory<>("estado"));
        colEstado.setMaxWidth(1f * Integer.MAX_VALUE * 12); // peso 12%
        // Renderiza el estado como pill de color
        colEstado.setCellFactory(col -> new TableCell<Expediente, EstadoExpediente>() {
            @Override
            protected void updateItem(EstadoExpediente estado, boolean empty) {
                super.updateItem(estado, empty);
                if (empty || estado == null) {
                    setGraphic(null);
                    setText(null);
                    return;
                }
                Label pill = new Label(estado.getDisplayName());
                pill.getStyleClass().add("pill");
                switch (estado) {
                    case ACTIVO -> pill.getStyleClass().add("pill-green");
                    case ARCHIVADO -> pill.getStyleClass().add("pill-blue");
                    case SUSPENDIDO -> pill.getStyleClass().add("pill-amber");
                    case FINALIZADO -> pill.getStyleClass().add("pill-red");
                }
                setGraphic(pill);
                setText(null);
            }
        });

        TableColumn<Expediente, LocalDate> colFecha = new TableColumn<>("Fecha Inicio");
        colFecha.setCellValueFactory(new PropertyValueFactory<>("fechaInicio"));
        colFecha.setMaxWidth(1f * Integer.MAX_VALUE * 13); // peso 13%

        tablaExpedientes.getColumns().addAll(colNumero, colCaratula, colCliente, colEstado, colFecha);

        tablaExpedientes.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldVal, newVal) -> {
                    if (newVal != null) {
                        cargarExpedienteEnFormulario(newVal);
                    }
                }
        );

        tablaExpedientes.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2 && tablaExpedientes.getSelectionModel().getSelectedItem() != null) {
                expedienteSeleccionado = tablaExpedientes.getSelectionModel().getSelectedItem();
                abrirVentanaDetalleExpediente();
            }
        });

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
        header.getStyleClass().addAll("button", "button-info");

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
        btnNuevo.getStyleClass().addAll("button", "button-success");
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
        btnEliminar.getStyleClass().add("btn-danger");
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
        btnGuardar.getStyleClass().addAll("button", "button-success");
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
        VBox panel = new VBox(16);
        panel.setPadding(new Insets(24, 32, 24, 32));

        // Header
        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);

        VBox tituloBox = new VBox(2);
        Label titulo = new Label("Agenda y Calendario");
        titulo.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #1A1A18;");
        Label subtitulo = new Label("Eventos, audiencias y vencimientos");
        subtitulo.getStyleClass().add("text-secondary");
        tituloBox.getChildren().addAll(titulo, subtitulo);

        Region spacerHeader = new Region();
        HBox.setHgrow(spacerHeader, Priority.ALWAYS);

        Button btnNuevoEvento = new Button("+  Nuevo Evento");
        btnNuevoEvento.getStyleClass().add("btn-primary-lg");
        btnNuevoEvento.setOnAction(e -> abrirFormularioEvento(null));

        header.getChildren().addAll(tituloBox, spacerHeader, btnNuevoEvento);

        // Barra de filtros
        HBox barraFiltros = new HBox(10);
        barraFiltros.setAlignment(Pos.CENTER_LEFT);

        Label lblVista = new Label("Vista:");
        lblVista.getStyleClass().add("text-secondary");

        cmbVistaAgenda = new ComboBox<>();
        cmbVistaAgenda.setItems(FXCollections.observableArrayList("Todos", "Hoy", "Esta Semana", "Este Mes", "Pendientes"));
        cmbVistaAgenda.setValue("Esta Semana");
        cmbVistaAgenda.setOnAction(e -> filtrarEventosAgenda(cmbVistaAgenda.getValue()));

        Button btnActualizar = new Button("Actualizar");
        btnActualizar.getStyleClass().add("btn-ghost");
        btnActualizar.setOnAction(e -> cargarEventosAgenda());

        barraFiltros.getChildren().addAll(lblVista, cmbVistaAgenda, btnActualizar);

        // Tabla
        tablaEventos = crearTablaEventos();

        panel.getChildren().addAll(header, barraFiltros, tablaEventos);
        VBox.setVgrow(tablaEventos, Priority.ALWAYS);

        return panel;
    }

    private TableView<EventoAgenda> crearTablaEventos() {
        TableView<EventoAgenda> tabla = new TableView<>();
        tabla.setItems(listaEventos); // usa el campo de instancia, no una lista local
        tabla.getStyleClass().add("table-view");
        tabla.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        // Fecha y hora
        TableColumn<EventoAgenda, LocalDateTime> colFechaHora = new TableColumn<>("Fecha y Hora");
        colFechaHora.setCellValueFactory(new PropertyValueFactory<>("fechaHora"));
        colFechaHora.setMaxWidth(1f * Integer.MAX_VALUE * 13);
        colFechaHora.setCellFactory(column -> new TableCell<EventoAgenda, LocalDateTime>() {
            @Override
            protected void updateItem(LocalDateTime item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
                }
            }
        });

        TableColumn<EventoAgenda, String> colTitulo = new TableColumn<>("Título");
        colTitulo.setCellValueFactory(new PropertyValueFactory<>("titulo"));
        colTitulo.setMaxWidth(1f * Integer.MAX_VALUE * 22);

        TableColumn<EventoAgenda, TipoEvento> colTipo = new TableColumn<>("Tipo");
        colTipo.setCellValueFactory(new PropertyValueFactory<>("tipo"));
        colTipo.setMaxWidth(1f * Integer.MAX_VALUE * 13);

        TableColumn<EventoAgenda, String> colUbicacion = new TableColumn<>("Ubicación");
        colUbicacion.setCellValueFactory(new PropertyValueFactory<>("ubicacion"));
        colUbicacion.setMaxWidth(1f * Integer.MAX_VALUE * 15);

        // Estado como pill
        TableColumn<EventoAgenda, EstadoEvento> colEstado = new TableColumn<>("Estado");
        colEstado.setCellValueFactory(new PropertyValueFactory<>("estado"));
        colEstado.setMaxWidth(1f * Integer.MAX_VALUE * 12);
        colEstado.setCellFactory(col -> new TableCell<EventoAgenda, EstadoEvento>() {
            @Override
            protected void updateItem(EstadoEvento estado, boolean empty) {
                super.updateItem(estado, empty);
                if (empty || estado == null) {
                    setGraphic(null);
                    setText(null);
                    return;
                }
                Label pill = new Label(estado.getDisplayName());
                pill.getStyleClass().add("pill");
                switch (estado) {
                    case PENDIENTE -> pill.getStyleClass().add("pill-amber");
                    case COMPLETADO -> pill.getStyleClass().add("pill-green");
                    case CANCELADO -> pill.getStyleClass().add("pill-red");
                }
                setGraphic(pill);
                setText(null);
            }
        });

        // Acciones
        TableColumn<EventoAgenda, Void> colAcciones = new TableColumn<>("Acciones");
        colAcciones.setMaxWidth(1f * Integer.MAX_VALUE * 25);
        colAcciones.setCellFactory(param -> new TableCell<>() {
            private final Button btnEditar = new Button("Editar");
            private final Button btnCompletar = new Button("Completar");
            private final Button btnEliminar = new Button("Eliminar");

            {
                btnEditar.getStyleClass().add("btn-ghost");
                btnEditar.setOnAction(e -> {
                    EventoAgenda evento = getTableView().getItems().get(getIndex());
                    abrirFormularioEvento(evento);
                });

                btnCompletar.getStyleClass().add("button-success");
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

                btnEliminar.getStyleClass().add("btn-danger");
                btnEliminar.setOnAction(e -> {
                    EventoAgenda evento = getTableView().getItems().get(getIndex());

                    String fecha = evento.getFechaHora() != null
                            ? evento.getFechaHora().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
                            : "—";

                    StringBuilder mensaje = new StringBuilder();
                    mensaje.append("¿Eliminar el evento?\n\n");
                    mensaje.append("Título: ").append(evento.getTitulo()).append("\n");
                    mensaje.append("Fecha: ").append(fecha).append("\n");
                    mensaje.append("Tipo: ").append(evento.getTipo().getDisplayName()).append("\n");

                    if (evento.getUbicacion() != null && !evento.getUbicacion().isEmpty()) {
                        mensaje.append("Ubicación: ").append(evento.getUbicacion()).append("\n");
                    }
                    if (evento.getExpedienteId() != null) {
                        mensaje.append("\nEste evento está asociado a un expediente.");
                    }

                    Alert confirmacion = new Alert(Alert.AlertType.CONFIRMATION);
                    confirmacion.setTitle("Confirmar eliminación");
                    confirmacion.setHeaderText("Eliminar evento");
                    confirmacion.setContentText(mensaje.toString());

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
                    HBox botones = new HBox(6, btnEditar, btnCompletar, btnEliminar);
                    botones.setAlignment(Pos.CENTER_LEFT);
                    setGraphic(botones);
                }
            }
        });

        tabla.getColumns().addAll(colFechaHora, colTitulo, colTipo, colUbicacion, colEstado, colAcciones);

        // Carga inicial respetando la vista por defecto ("Esta Semana")
        try {
            Integer usuarioId = SesionUsuario.getUsuarioActual().getId();
            listaEventos.setAll(agendaService.listarEstaSemana(usuarioId));
        } catch (SQLException e) {
            mostrarError("Error al cargar eventos: " + e.getMessage());
        }

        return tabla;
    }

    private void cargarEventosAgenda() {
        // Recarga respetando la vista seleccionada actual
        String vista = (cmbVistaAgenda != null && cmbVistaAgenda.getValue() != null)
                ? cmbVistaAgenda.getValue() : "Esta Semana";
        filtrarEventosAgenda(vista);

        cargarDashboard();
        actualizarNotificaciones();
    }

    private void filtrarEventosAgenda(String filtro) {
        if (tablaEventos == null) return;
        try {
            Integer usuarioId = SesionUsuario.getUsuarioActual().getId();
            List<EventoAgenda> eventos = switch (filtro) {
                case "Hoy" -> agendaService.listarHoy(usuarioId);
                case "Esta Semana" -> agendaService.listarEstaSemana(usuarioId);
                case "Este Mes" -> agendaService.listarEsteMes(usuarioId);
                case "Pendientes" -> agendaService.listarPendientes(usuarioId);
                default -> agendaService.listarPorUsuario(usuarioId);
            };
            listaEventos.setAll(eventos);
        } catch (SQLException e) {
            mostrarError("Error al filtrar eventos: " + e.getMessage());
        }
    }



    private void abrirFormularioEvento(EventoAgenda evento) {
        Stage ventana = new Stage();
        ventana.initModality(Modality.APPLICATION_MODAL);
        ventana.setTitle(evento == null ? "Nuevo Evento" : "Editar Evento");

        VBox form = new VBox(14);
        form.setPadding(new Insets(24));

        // Encabezado
        Label tituloVentana = new Label(evento == null ? "Nuevo Evento" : "Editar Evento");
        tituloVentana.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #1A1A18;");

        // Campos
        TextField txtTitulo = new TextField();
        txtTitulo.setPromptText("Ej: Audiencia preliminar");

        TextArea txtDescripcion = new TextArea();
        txtDescripcion.setPrefRowCount(3);

        DatePicker dpFecha = new DatePicker(LocalDate.now());
        dpFecha.setMaxWidth(Double.MAX_VALUE);

        // Combo de horarios cada 30 min
        ComboBox<LocalTime> cmbHora = new ComboBox<>();
        ObservableList<LocalTime> horarios = FXCollections.observableArrayList();
        for (int h = 0; h < 24; h++) {
            horarios.add(LocalTime.of(h, 0));
            horarios.add(LocalTime.of(h, 30));
        }
        cmbHora.setItems(horarios);
        cmbHora.setMaxWidth(Double.MAX_VALUE);
        DateTimeFormatter hhmm = DateTimeFormatter.ofPattern("HH:mm");
        // Mostrar como "HH:mm"
        cmbHora.setConverter(new javafx.util.StringConverter<LocalTime>() {
            @Override public String toString(LocalTime t) { return t != null ? t.format(hhmm) : ""; }
            @Override public LocalTime fromString(String s) { return (s == null || s.isBlank()) ? null : LocalTime.parse(s, hhmm); }
        });
        cmbHora.setValue(LocalTime.of(9, 0));

        Spinner<Integer> spDuracion = new Spinner<>(15, 480, 60, 15);
        spDuracion.setEditable(true);
        spDuracion.setMaxWidth(Double.MAX_VALUE);

        ComboBox<TipoEvento> cmbTipo = new ComboBox<>();
        cmbTipo.setItems(FXCollections.observableArrayList(TipoEvento.values()));
        cmbTipo.setMaxWidth(Double.MAX_VALUE);

        TextField txtUbicacion = new TextField();
        txtUbicacion.setPromptText("Ej: Juzgado Civil N° 3");

        ComboBox<RecordatorioOpcion> cmbRecordatorio = new ComboBox<>();
        cmbRecordatorio.setItems(FXCollections.observableArrayList(
                new RecordatorioOpcion(15, "15 minutos antes"),
                new RecordatorioOpcion(30, "30 minutos antes"),
                new RecordatorioOpcion(60, "1 hora antes"),
                new RecordatorioOpcion(120, "2 horas antes"),
                new RecordatorioOpcion(1440, "1 día antes"),
                new RecordatorioOpcion(2880, "2 días antes")
        ));
        cmbRecordatorio.setMaxWidth(Double.MAX_VALUE);

        ComboBox<String> cmbExpediente = new ComboBox<>();
        cmbExpediente.setMaxWidth(Double.MAX_VALUE);
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

        // Cargar datos (modo edición)
        if (evento != null) {
            txtTitulo.setText(evento.getTitulo());
            txtDescripcion.setText(evento.getDescripcion());
            dpFecha.setValue(evento.getFechaHora().toLocalDate());
            // Selecciona la opción de hora más cercana entre las del combo
            LocalTime horaEvento = evento.getFechaHora().toLocalTime();
            cmbHora.setValue(LocalTime.of(horaEvento.getHour(), horaEvento.getMinute() < 30 ? 0 : 30));
            spDuracion.getValueFactory().setValue(evento.getDuracionMinutos());
            cmbTipo.setValue(evento.getTipo());
            txtUbicacion.setText(evento.getUbicacion());
            Integer minutosEvento = evento.getRecordatorioMinutos();
            cmbRecordatorio.getItems().stream()
                    .filter(r -> r.getMinutos().equals(minutosEvento))
                    .findFirst()
                    .ifPresent(cmbRecordatorio::setValue);
        } else {
            cmbRecordatorio.setValue(cmbRecordatorio.getItems().get(4)); // 1 día por defecto
        }

        // Fila hora + duración (lado a lado)
        VBox boxHora = campoConLabel("Hora *", cmbHora);
        VBox boxDuracion = campoConLabel("Duración (min)", spDuracion);
        HBox.setHgrow(boxHora, Priority.ALWAYS);
        HBox.setHgrow(boxDuracion, Priority.ALWAYS);
        HBox filaHora = new HBox(12, boxHora, boxDuracion);

        form.getChildren().addAll(
                tituloVentana,
                new Separator(),
                campoConLabel("Título *", txtTitulo),
                campoConLabel("Descripción", txtDescripcion),
                campoConLabel("Fecha *", dpFecha),
                filaHora,
                campoConLabel("Tipo *", cmbTipo),
                campoConLabel("Ubicación", txtUbicacion),
                campoConLabel("Recordatorio", cmbRecordatorio),
                campoConLabel("Expediente asociado", cmbExpediente)
        );

        // Botones
        HBox botones = new HBox(10);
        botones.setAlignment(Pos.CENTER_RIGHT);
        botones.setPadding(new Insets(10, 0, 0, 0));

        Button btnGuardar = new Button("Guardar");
        btnGuardar.getStyleClass().add("btn-primary");
        btnGuardar.setOnAction(e -> {
            try {
                EventoAgenda ev = evento != null ? evento : new EventoAgenda();
                ev.setTitulo(txtTitulo.getText());
                ev.setDescripcion(txtDescripcion.getText());

                LocalDate fecha = dpFecha.getValue();
                LocalTime hora = cmbHora.getValue() != null ? cmbHora.getValue() : LocalTime.of(9, 0);
                ev.setFechaHora(LocalDateTime.of(fecha, hora));
                ev.setDuracionMinutos(spDuracion.getValue());
                ev.setTipo(cmbTipo.getValue());
                ev.setUbicacion(txtUbicacion.getText());

                RecordatorioOpcion recordatorio = cmbRecordatorio.getValue();
                ev.setRecordatorioMinutos(recordatorio != null ? recordatorio.getMinutos() : 1440);

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

        Button btnCancelar = new Button("Cancelar");
        btnCancelar.getStyleClass().add("btn-ghost");
        btnCancelar.setOnAction(e -> ventana.close());

        botones.getChildren().addAll(btnCancelar, btnGuardar);
        form.getChildren().add(botones);

        ScrollPane scroll = new ScrollPane(form);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("scroll-pane");

        Scene scene = new Scene(scroll, 480, 640);
        scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
        ventana.setScene(scene);
        ventana.showAndWait();
    }

    // Agrupa una etiqueta y su control con el estilo de la app
    private VBox campoConLabel(String etiqueta, javafx.scene.Node control) {
        VBox box = new VBox(4);
        Label lbl = new Label(etiqueta);
        lbl.getStyleClass().add("field-label");
        box.getChildren().addAll(lbl, control);
        return box;
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

            // ✅ OBTENER CLIENTE DEL COMBOBOX
            Cliente clienteSeleccionado = cmbCliente.getValue();
            if (clienteSeleccionado != null) {
                expediente.setCliente(clienteSeleccionado.getNombreCompleto());
                expediente.setClienteId(clienteSeleccionado.getId()); // ✅ ESTO ES LO IMPORTANTE
            } else {
                // Por compatibilidad, si no hay cliente seleccionado
                expediente.setCliente(txtCliente.getText().trim());
                expediente.setClienteId(null);
            }

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

    // ========== 1. AL ELIMINAR EXPEDIENTE ==========
    private void eliminarExpediente() {
        if (expedienteSeleccionado == null) {
            mostrarAdvertencia("Seleccione un expediente para eliminar");
            return;
        }
        try {
            // Contar elementos asociados
            int cantMovimientos = movimientoService.listarPorExpediente(expedienteSeleccionado.getId()).size();
            int cantHonorarios = honorarioService.listarPorExpediente(expedienteSeleccionado.getId()).size();
            int cantGastos = gastoService.listarPorExpediente(expedienteSeleccionado.getId()).size();
            int cantPagos = pagoService.listarPorExpediente(expedienteSeleccionado.getId()).size();

            // Construir mensaje detallado
            StringBuilder mensaje = new StringBuilder();
            mensaje.append("¿Está seguro de eliminar el expediente?\n\n");
            mensaje.append("📁 ").append(expedienteSeleccionado.getNumero()).append("\n");
            mensaje.append("    ").append(expedienteSeleccionado.getCaratula()).append("\n\n");

            boolean tieneAsociaciones = false;

            if (cantMovimientos > 0) {
                mensaje.append("⚠️ Se eliminarán ").append(cantMovimientos).append(" movimiento(s)\n");
                tieneAsociaciones = true;
            }

            if (cantHonorarios > 0) {
                mensaje.append("💵 Se eliminarán ").append(cantHonorarios).append(" honorario(s)\n");
                tieneAsociaciones = true;
            }

            if (cantGastos > 0) {
                mensaje.append("💸 Se eliminarán ").append(cantGastos).append(" gasto(s)\n");
                tieneAsociaciones = true;
            }

            if (cantPagos > 0) {
                mensaje.append("💳 Se eliminarán ").append(cantPagos).append(" pago(s)\n");
                tieneAsociaciones = true;
            }

            if (tieneAsociaciones) {
                mensaje.append("\n⚠️ ESTA ACCIÓN NO SE PUEDE DESHACER");
            } else {
                mensaje.append("Este expediente no tiene datos asociados.");
            }

            Alert confirmacion = new Alert(Alert.AlertType.WARNING);
            confirmacion.setTitle("Confirmar eliminación");
            confirmacion.setHeaderText("Eliminar expediente y todos sus datos");
            confirmacion.setContentText(mensaje.toString());

            // Agregar ícono grande de advertencia
            confirmacion.setGraphic(new Label("⚠️"));

            Optional<ButtonType> resultado = confirmacion.showAndWait();
            if (resultado.isPresent() && resultado.get() == ButtonType.OK) {
                expedienteService.eliminarExpediente(expedienteSeleccionado.getId());

                String mensajeExito = "Expediente eliminado correctamente";
                if (tieneAsociaciones) {
                    int totalEliminados = cantMovimientos + cantHonorarios + cantGastos + cantPagos;
                    mensajeExito += "\n(" + totalEliminados + " registros asociados eliminados)";
                }

                mostrarInfo(mensajeExito);
                limpiarFormularioExpediente();
                cargarExpedientes();
                cargarDashboard();
            }

        } catch (SQLException e) {
            mostrarError("No se pudo eliminar el expediente: " + e.getMessage());
            e.printStackTrace();
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

            System.out.println("========================================");
            System.out.println("🔍 DEBUG: Búsqueda de expedientes");
            System.out.println("Texto: '" + textoBusqueda + "' (length: " + textoBusqueda.length() + ")");
            System.out.println("Estado filtro: " + estadoFiltro);

            List<Expediente> resultados;

            // Si no hay filtros, listar todos
            if (textoBusqueda.isEmpty() && estadoFiltro == null) {
                System.out.println("→ Sin filtros, listando todos");
                resultados = expedienteService.listarTodos();
            } else {
                System.out.println("→ Buscando con criterios...");
                System.out.println("   Parámetro número: " + (textoBusqueda.isEmpty() ? "null" : textoBusqueda));
                System.out.println("   Parámetro cliente: " + (textoBusqueda.isEmpty() ? "null" : textoBusqueda));
                System.out.println("   Parámetro estado: " + estadoFiltro);

                resultados = expedienteService.buscarPorCriterios(
                        textoBusqueda.isEmpty() ? null : textoBusqueda,
                        textoBusqueda.isEmpty() ? null : textoBusqueda,
                        estadoFiltro
                );
            }

            System.out.println("📊 Resultados encontrados: " + resultados.size());

            if (!resultados.isEmpty()) {
                System.out.println("✅ Primeros resultados:");
                for (int i = 0; i < Math.min(3, resultados.size()); i++) {
                    Expediente exp = resultados.get(i);
                    System.out.println("  - " + exp.getNumero() + " | " + exp.getCliente());
                }
            }

            listaExpedientes.clear();
            listaExpedientes.addAll(resultados);

            System.out.println("📋 Items en tabla: " + listaExpedientes.size());
            System.out.println("========================================");

        } catch (SQLException e) {
            System.err.println("❌ ERROR en búsqueda: " + e.getMessage());
            e.printStackTrace();
            mostrarError("Error al buscar: " + e.getMessage());
        }
    }

    private void cargarExpedienteEnFormulario(Expediente exp) {
        expedienteSeleccionado = exp;
        txtNumero.setText(exp.getNumero());
        txtCaratula.setText(exp.getCaratula());
        txtCliente.setText(exp.getCliente());

        // ✅ BUSCAR Y SELECCIONAR EL CLIENTE EN EL COMBO
        if (exp.getClienteId() != null) {
            try {
                Optional<Cliente> clienteOpt = clienteService.buscarPorId(exp.getClienteId());
                if (clienteOpt.isPresent()) {
                    cmbCliente.setValue(clienteOpt.get());
                }
            } catch (SQLException e) {
                System.err.println("Error al cargar cliente: " + e.getMessage());
            }
        }

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
        cmbCliente.setValue(null);
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
        //barra.setStyle("-fx-background-color: #F8F8F6;");
        barra.getStyleClass().add("status-bar");

        Label lblEstado = new Label("✅ Sistema listo");
        barra.getChildren().add(lblEstado);

        return barra;
    }

    // ==================== PANEL DE CLIENTES ====================

    // ==================== PANEL DE CLIENTES (VERSIÓN SIMPLE) ====================

    private VBox crearPanelClientes() {
        VBox panel = new VBox(16);
        panel.setPadding(new Insets(24, 32, 24, 32));

        // Header con título y botón
        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);

        VBox tituloBox = new VBox(2);
        Label titulo = new Label("Gestión de Clientes");
        titulo.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #1A1A18;");
        Label subtitulo = new Label("Cartera de clientes del estudio");
        subtitulo.getStyleClass().add("text-secondary");
        tituloBox.getChildren().addAll(titulo, subtitulo);

        Region spacerHeader = new Region();
        HBox.setHgrow(spacerHeader, Priority.ALWAYS);

        Button btnNuevoCliente = new Button("+  Nuevo Cliente");
        btnNuevoCliente.getStyleClass().add("btn-primary-lg");
        btnNuevoCliente.setOnAction(e -> abrirFormularioCliente(null));

        header.getChildren().addAll(tituloBox, spacerHeader, btnNuevoCliente);

        // Barra de búsqueda
        HBox barraControl = new HBox(10);
        barraControl.setAlignment(Pos.CENTER_LEFT);

        Label lblBuscar = new Label("Buscar:");
        lblBuscar.getStyleClass().add("text-secondary");
        txtBuscarCliente = new TextField();
        txtBuscarCliente.setPromptText("Nombre, DNI, email...");
        txtBuscarCliente.getStyleClass().add("search-field");
        txtBuscarCliente.textProperty().addListener((obs, old, val) -> buscarClientes());

        Button btnActualizar = new Button("Actualizar");
        btnActualizar.getStyleClass().add("btn-ghost");
        btnActualizar.setOnAction(e -> cargarClientes());

        Region spacerBusqueda = new Region();
        HBox.setHgrow(spacerBusqueda, Priority.ALWAYS);

        Button btnExportar = new Button("Exportar Excel");
        btnExportar.getStyleClass().add("button-info");
        btnExportar.setOnAction(e -> exportarClientesExcel());

        barraControl.getChildren().addAll(lblBuscar, txtBuscarCliente, btnActualizar, spacerBusqueda, btnExportar);

        // Tabla de clientes
        tablaClientes = new TableView<>();
        tablaClientes.setItems(listaClientes);
        tablaClientes.getStyleClass().add("table-view");
        tablaClientes.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<Cliente, String> colNombre = new TableColumn<>("Nombre Completo");
        colNombre.setCellValueFactory(new PropertyValueFactory<>("nombreCompleto"));
        colNombre.setMaxWidth(1f * Integer.MAX_VALUE * 32);

        TableColumn<Cliente, String> colDni = new TableColumn<>("DNI");
        colDni.setCellValueFactory(new PropertyValueFactory<>("dni"));
        colDni.setMaxWidth(1f * Integer.MAX_VALUE * 18);

        TableColumn<Cliente, String> colTelefono = new TableColumn<>("Teléfono");
        colTelefono.setCellValueFactory(new PropertyValueFactory<>("telefono"));
        colTelefono.setMaxWidth(1f * Integer.MAX_VALUE * 20);

        TableColumn<Cliente, String> colEmail = new TableColumn<>("Email");
        colEmail.setCellValueFactory(new PropertyValueFactory<>("email"));
        colEmail.setMaxWidth(1f * Integer.MAX_VALUE * 30);

        tablaClientes.getColumns().addAll(colNombre, colDni, colTelefono, colEmail);

        // Doble clic para ver detalles
        tablaClientes.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2 && tablaClientes.getSelectionModel().getSelectedItem() != null) {
                abrirVistaDetalladaCliente(tablaClientes.getSelectionModel().getSelectedItem());
            }
        });

        panel.getChildren().addAll(header, barraControl, tablaClientes);
        VBox.setVgrow(tablaClientes, Priority.ALWAYS);

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
    private void mostrarExito(String mensaje) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Éxito");
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

        // ========== MOSTRAR POPUP DE EVENTOS DEL DÍA ==========
        // Usar Platform.runLater para que se muestre después de que cargue la ventana principal
        javafx.application.Platform.runLater(() -> {
            mostrarPopupEventosDelDia();
        });
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

        ColumnConstraints col1 = new ColumnConstraints();
        col1.setMinWidth(120);
        ColumnConstraints col2 = new ColumnConstraints();
        col2.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().addAll(col1, col2);

        // Botones principales
        HBox botones = new HBox(10);
        botones.setAlignment(Pos.CENTER);
        botones.setPadding(new Insets(15, 0, 0, 0));

        Button btnGuardar = new Button("Guardar");
        btnGuardar.getStyleClass().add("btn-primary");
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

        Button btnCancelar = new Button("Cancelar");
        btnCancelar.getStyleClass().add("btn-ghost");
        btnCancelar.setOnAction(e -> ventana.close());

        botones.getChildren().addAll(btnGuardar, btnCancelar);

        form.getChildren().addAll(grid, botones);

        // Zona de eliminar (solo en edición), separada y discreta al final
        if (cliente != null) {
            Separator sep = new Separator();
            sep.setPadding(new Insets(10, 0, 10, 0));

            VBox zonaPeligro = new VBox(6);
            Label lblZona = new Label("Zona de riesgo");
            lblZona.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #791F1F;");

            Button btnEliminar = new Button("Eliminar Cliente");
            btnEliminar.getStyleClass().add("btn-danger");
            btnEliminar.setOnAction(a -> eliminarClienteConValidacion(cliente, ventana));

            zonaPeligro.getChildren().addAll(lblZona, btnEliminar);
            form.getChildren().addAll(sep, zonaPeligro);
        }

        ScrollPane scroll = new ScrollPane(form);
        scroll.setFitToWidth(true);

        Scene scene = new Scene(scroll, 600, 720);
        scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
        ventana.setScene(scene);
        ventana.showAndWait();
    }

    private void eliminarClienteConValidacion(Cliente cliente, Stage ventanaFormulario) {
        try {
            ExpedienteDAO expedienteDAO = new ExpedienteDAO();
            List<Expediente> expedientes = expedienteDAO.listarPorClienteId(cliente.getId());
            int cantDocumentos = documentoClienteService.listarPorCliente(cliente.getId()).size();

            StringBuilder mensaje = new StringBuilder();
            mensaje.append("¿Está seguro de eliminar el cliente: ").append(cliente.getNombreCompleto()).append("?\n\n");

            boolean tieneAsociaciones = false;

            if (!expedientes.isEmpty()) {
                mensaje.append("ATENCIÓN: Este cliente tiene ").append(expedientes.size())
                        .append(" expediente(s) asociado(s):\n");
                for (Expediente exp : expedientes) {
                    mensaje.append("   - ").append(exp.getNumero()).append(" - ").append(exp.getCaratula()).append("\n");
                }
                mensaje.append("\n");
                tieneAsociaciones = true;
            }

            if (cantDocumentos > 0) {
                mensaje.append("Este cliente tiene ").append(cantDocumentos).append(" documento(s) cargado(s).\n\n");
                tieneAsociaciones = true;
            }

            if (tieneAsociaciones) {
                mensaje.append("No se puede eliminar.\n");
                mensaje.append("Primero debe eliminar o reasignar los expedientes y documentos asociados.");

                Alert alerta = new Alert(Alert.AlertType.ERROR);
                alerta.setTitle("No se puede eliminar");
                alerta.setHeaderText("Cliente con datos asociados");
                alerta.setContentText(mensaje.toString());
                alerta.showAndWait();
                return;
            }

            mensaje.append("Esta acción no se puede deshacer.");

            Alert confirmacion = new Alert(Alert.AlertType.CONFIRMATION);
            confirmacion.setTitle("Confirmar eliminación");
            confirmacion.setHeaderText("Eliminar cliente");
            confirmacion.setContentText(mensaje.toString());

            if (confirmacion.showAndWait().get() == ButtonType.OK) {
                clienteService.eliminarCliente(cliente.getId());
                mostrarInfo("Cliente eliminado correctamente");
                ventanaFormulario.close();
                cargarClientes();
            }

        } catch (IllegalStateException ex) {
            Alert alerta = new Alert(Alert.AlertType.ERROR);
            alerta.setTitle("No se puede eliminar");
            alerta.setHeaderText("Validación fallida");
            alerta.setContentText(ex.getMessage());
            alerta.showAndWait();
        } catch (SQLException ex) {
            mostrarError("Error al eliminar cliente: " + ex.getMessage());
        }
    }
    // ==================== VISTA DETALLADA DEL CLIENTE ====================

    private void abrirVistaDetalladaCliente(Cliente cliente) {
        Stage ventana = new Stage();
        ventana.initModality(Modality.APPLICATION_MODAL);
        ventana.setTitle("Cliente: " + cliente.getNombreCompleto());
        ventana.setMaximized(true);

        BorderPane root = new BorderPane();

        // ========== TOP: Header con datos básicos ==========
        VBox header = new VBox(12);
        header.setPadding(new Insets(20, 24, 20, 24));
        header.setStyle("-fx-background-color: #0C447C;");

        // Línea 1: nombre + estado (pill)
        HBox lineaNombre = new HBox(12);
        lineaNombre.setAlignment(Pos.CENTER_LEFT);

        Label lblNombre = new Label(cliente.getNombreCompleto());
        lblNombre.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: white;");

        Label lblEstado = new Label(cliente.isActivo() ? "ACTIVO" : "INACTIVO");
        lblEstado.getStyleClass().add("pill");
        lblEstado.getStyleClass().add(cliente.isActivo() ? "pill-green" : "pill-red");

        lineaNombre.getChildren().addAll(lblNombre, lblEstado);

        // Línea 2: datos de contacto
        HBox infoDatos = new HBox(28);
        infoDatos.setAlignment(Pos.CENTER_LEFT);

        Label lblDni = new Label("DNI: " + (cliente.getDni() != null ? cliente.getDni() : "—"));
        lblDni.setStyle("-fx-text-fill: #B5D4F4; -fx-font-size: 14px;");

        Label lblTelefono = new Label("Tel: " + (cliente.getTelefono() != null ? cliente.getTelefono() : "—"));
        lblTelefono.setStyle("-fx-text-fill: #B5D4F4; -fx-font-size: 14px;");

        Label lblEmail = new Label("Email: " + (cliente.getEmail() != null ? cliente.getEmail() : "—"));
        lblEmail.setStyle("-fx-text-fill: #B5D4F4; -fx-font-size: 14px;");

        infoDatos.getChildren().addAll(lblDni, lblTelefono, lblEmail);

        // Línea 3: botón editar
        Button btnEditar = new Button("Editar Cliente");
        btnEditar.setStyle("-fx-background-color: white; -fx-text-fill: #0C447C; -fx-font-weight: bold; " +
                "-fx-padding: 8 18 8 18; -fx-background-radius: 6px; -fx-cursor: hand;");
        btnEditar.setOnAction(e -> {
            abrirFormularioCliente(cliente);
            ventana.close();
        });

        header.getChildren().addAll(lineaNombre, infoDatos, btnEditar);
        root.setTop(header);

        // ========== CENTER ==========
        SplitPane splitPane = new SplitPane();
        splitPane.setOrientation(javafx.geometry.Orientation.HORIZONTAL);
        splitPane.setDividerPositions(0.3);
        splitPane.setPadding(new Insets(16));

        VBox panelDatos = crearPanelDatosCliente(cliente);

        SplitPane splitDerecha = new SplitPane();
        splitDerecha.setOrientation(javafx.geometry.Orientation.VERTICAL);
        splitDerecha.setDividerPositions(0.5);

        VBox panelExpedientes = crearPanelExpedientesCliente(cliente, ventana);
        VBox panelDocumentos = crearPanelDocumentosCliente(cliente);

        splitDerecha.getItems().addAll(panelExpedientes, panelDocumentos);
        splitPane.getItems().addAll(panelDatos, splitDerecha);
        root.setCenter(splitPane);

        // ========== BOTTOM: cerrar ==========
        HBox bottomBar = new HBox(10);
        bottomBar.setAlignment(Pos.CENTER_RIGHT);
        bottomBar.setPadding(new Insets(10, 16, 16, 16));

        Button btnCerrar = new Button("Cerrar");
        btnCerrar.getStyleClass().add("btn-ghost");
        btnCerrar.setOnAction(e -> ventana.close());

        bottomBar.getChildren().add(btnCerrar);
        root.setBottom(bottomBar);

        Scene scene = new Scene(root, 1200, 800);
        scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
        ventana.setScene(scene);
        ventana.show();
    }

    // ========== Panel de datos completos del cliente ==========
    private VBox crearPanelDatosCliente(Cliente cliente) {
        VBox panel = new VBox(10);
        panel.setPadding(new Insets(15));
        panel.setStyle("-fx-background-color: #F8F8F6; -fx-border-color: #bdc3c7; -fx-border-width: 1;");

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
    private VBox crearPanelExpedientesCliente(Cliente cliente, Stage ventanaCliente) {
        VBox panel = new VBox(10);
        panel.setPadding(new Insets(15));

        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);

        Label titulo = new Label("Expedientes del Cliente");
        titulo.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #1A1A18;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button btnNuevoExpediente = new Button("+  Nuevo Expediente");
        btnNuevoExpediente.getStyleClass().add("btn-primary");
        btnNuevoExpediente.setOnAction(e -> {
            ventanaCliente.close();
            abrirFormularioNuevoExpediente(cliente);



            limpiarFormularioExpediente();
            txtCliente.setText(cliente.getNombreCompleto());
            mostrarInfo("Complete los datos del nuevo expediente para: " + cliente.getNombreCompleto());
        });

        header.getChildren().addAll(titulo, spacer, btnNuevoExpediente);

        TableView<Expediente> tablaExp = new TableView<>();
        ObservableList<Expediente> listaExp = FXCollections.observableArrayList();
        tablaExp.setItems(listaExp);
        tablaExp.getStyleClass().add("table-view");
        tablaExp.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<Expediente, String> colNumero = new TableColumn<>("Número");
        colNumero.setCellValueFactory(new PropertyValueFactory<>("numero"));
        colNumero.setMaxWidth(1f * Integer.MAX_VALUE * 15);

        TableColumn<Expediente, String> colCaratula = new TableColumn<>("Carátula");
        colCaratula.setCellValueFactory(new PropertyValueFactory<>("caratula"));
        colCaratula.setMaxWidth(1f * Integer.MAX_VALUE * 45);

        TableColumn<Expediente, EstadoExpediente> colEstado = new TableColumn<>("Estado");
        colEstado.setCellValueFactory(new PropertyValueFactory<>("estado"));
        colEstado.setMaxWidth(1f * Integer.MAX_VALUE * 22);
        colEstado.setCellFactory(col -> new TableCell<Expediente, EstadoExpediente>() {
            @Override
            protected void updateItem(EstadoExpediente estado, boolean empty) {
                super.updateItem(estado, empty);
                if (empty || estado == null) {
                    setGraphic(null);
                    setText(null);
                    return;
                }
                Label pill = new Label(estado.getDisplayName());
                pill.getStyleClass().add("pill");
                switch (estado) {
                    case ACTIVO -> pill.getStyleClass().add("pill-green");
                    case ARCHIVADO -> pill.getStyleClass().add("pill-blue");
                    case SUSPENDIDO -> pill.getStyleClass().add("pill-amber");
                    case FINALIZADO -> pill.getStyleClass().add("pill-red");
                }
                setGraphic(pill);
                setText(null);
            }
        });

        TableColumn<Expediente, LocalDate> colFecha = new TableColumn<>("Fecha");
        colFecha.setCellValueFactory(new PropertyValueFactory<>("fechaInicio"));
        colFecha.setMaxWidth(1f * Integer.MAX_VALUE * 18);

        tablaExp.getColumns().addAll(colNumero, colCaratula, colEstado, colFecha);

        tablaExp.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2 && tablaExp.getSelectionModel().getSelectedItem() != null) {
                expedienteSeleccionado = tablaExp.getSelectionModel().getSelectedItem();
                abrirVentanaMovimientos();
            }
        });

        // Cargar expedientes del cliente
        try {
            ExpedienteDAO expedienteDAO = new ExpedienteDAO();
            List<Expediente> expedientes = expedienteDAO.listarPorClienteId(cliente.getId());
            listaExp.setAll(expedientes);
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

        Label titulo = new Label("Documentos del Cliente");
        titulo.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #1A1A18;");

        Button btnSubirDoc = new Button("+  Subir Documento");
        btnSubirDoc.getStyleClass().add("btn-primary");
        btnSubirDoc.setOnAction(e -> abrirDialogoSubirDocumento(cliente));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        header.getChildren().addAll(titulo, spacer, btnSubirDoc);

        TableView<DocumentoCliente> tablaDocs = new TableView<>();
        ObservableList<DocumentoCliente> listaDocs = FXCollections.observableArrayList();
        tablaDocs.setItems(listaDocs);
        tablaDocs.getStyleClass().add("table-view");
        tablaDocs.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<DocumentoCliente, String> colNombre = new TableColumn<>("Nombre");
        colNombre.setCellValueFactory(new PropertyValueFactory<>("nombreOriginal"));
        colNombre.setMaxWidth(1f * Integer.MAX_VALUE * 40);

        TableColumn<DocumentoCliente, TipoDocumentoCliente> colTipo = new TableColumn<>("Tipo");
        colTipo.setCellValueFactory(new PropertyValueFactory<>("tipoDocumento"));
        colTipo.setMaxWidth(1f * Integer.MAX_VALUE * 22);

        TableColumn<DocumentoCliente, String> colFecha = new TableColumn<>("Fecha Subida");
        colFecha.setCellValueFactory(cellData -> {
            LocalDateTime f = cellData.getValue().getFechaSubida();
            String texto = f != null ? f.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) : "";
            return new javafx.beans.property.SimpleStringProperty(texto);
        });
        colFecha.setMaxWidth(1f * Integer.MAX_VALUE * 20);

        TableColumn<DocumentoCliente, Void> colAcciones = new TableColumn<>("Acciones");
        colAcciones.setMaxWidth(1f * Integer.MAX_VALUE * 18);
        colAcciones.setCellFactory(param -> new TableCell<>() {
            private final Button btnAbrir = new Button("Ver");
            private final Button btnEliminar = new Button("Eliminar");

            {
                btnAbrir.getStyleClass().add("button-info");
                btnAbrir.setOnAction(e -> {
                    DocumentoCliente doc = getTableView().getItems().get(getIndex());
                    try {
                        documentoClienteService.abrirDocumento(doc.getId());
                    } catch (Exception ex) {
                        mostrarError("Error al abrir documento: " + ex.getMessage());
                    }
                });

                btnEliminar.getStyleClass().add("btn-danger");
                btnEliminar.setOnAction(e -> {
                    DocumentoCliente doc = getTableView().getItems().get(getIndex());

                    String fechaTexto = doc.getFechaSubida() != null
                            ? doc.getFechaSubida().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
                            : "—";

                    StringBuilder mensaje = new StringBuilder();
                    mensaje.append("¿Eliminar el documento?\n\n");
                    mensaje.append("Archivo: ").append(doc.getNombreOriginal()).append("\n");
                    mensaje.append("Subido: ").append(fechaTexto).append("\n\n");
                    mensaje.append("El archivo será eliminado del sistema de archivos.\n");
                    mensaje.append("Esta acción no se puede deshacer.");

                    Alert confirmacion = new Alert(Alert.AlertType.WARNING);
                    confirmacion.setTitle("Confirmar eliminación");
                    confirmacion.setHeaderText("Eliminar documento");
                    confirmacion.setContentText(mensaje.toString());

                    if (confirmacion.showAndWait().get() == ButtonType.OK) {
                        try {
                            documentoClienteService.eliminarDocumento(doc.getId());
                            listaDocs.remove(doc);
                            mostrarInfo("Documento eliminado del sistema");
                        } catch (Exception ex) {
                            mostrarError("Error al eliminar: " + ex.getMessage());
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
                    HBox botones = new HBox(6, btnAbrir, btnEliminar);
                    botones.setAlignment(Pos.CENTER_LEFT);
                    setGraphic(botones);
                }
            }
        });

        tablaDocs.getColumns().addAll(colNombre, colTipo, colFecha, colAcciones);

        try {
            List<DocumentoCliente> documentos = documentoClienteService.listarPorCliente(cliente.getId());
            listaDocs.setAll(documentos);
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

        TextField txtNombrePersonalizado = new TextField();
        txtNombrePersonalizado.setPromptText("Nombre personalizado (opcional)");
        txtNombrePersonalizado.setMaxWidth(Double.MAX_VALUE);

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
                // NUEVO: Pre-cargar el nombre sin extensión
                String nombreSinExt = archivo.getName().substring(0, archivo.getName().lastIndexOf('.'));
                txtNombrePersonalizado.setText(nombreSinExt);
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
        btnSubir.getStyleClass().addAll("button", "button-success");
        btnSubir.setOnAction(e -> {
            String nombreFinal = txtNombrePersonalizado.getText().trim().isEmpty() ?
                    archivoSeleccionado[0].getName() : txtNombrePersonalizado.getText().trim();
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
                new Label("Nombre del documento:"),
                txtNombrePersonalizado,
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
    // ==================== EXPORTACIÓN DE REPORTES ====================

    private void exportarClientesExcel() {
        try {
            List<Cliente> clientes = clienteService.listarTodos();

            javafx.stage.FileChooser fileChooser = new javafx.stage.FileChooser();
            fileChooser.setTitle("Guardar Excel");
            fileChooser.setInitialFileName("clientes_" + LocalDate.now() + ".csv");
            fileChooser.getExtensionFilters().add(
                    new javafx.stage.FileChooser.ExtensionFilter("CSV", "*.csv")
            );

            File file = fileChooser.showSaveDialog(stage);
            if (file != null) {
                StringBuilder csv = new StringBuilder();
                csv.append("Nombre,DNI,CUIT,Teléfono,Email,Domicilio,Activo\n");

                for (Cliente c : clientes) {
                    csv.append(escapeCsv(c.getNombreCompleto())).append(",");
                    csv.append(escapeCsv(c.getDni())).append(",");
                    csv.append(escapeCsv(c.getCuitCuil())).append(",");
                    csv.append(escapeCsv(c.getTelefono())).append(",");
                    csv.append(escapeCsv(c.getEmail())).append(",");
                    csv.append(escapeCsv(c.getDomicilioCompleto())).append(",");
                    csv.append(c.isActivo() ? "Sí" : "No").append("\n");
                }

                java.nio.file.Files.writeString(file.toPath(), csv.toString());
                mostrarInfo("Excel exportado correctamente");

                // Abrir el archivo
                if (java.awt.Desktop.isDesktopSupported()) {
                    java.awt.Desktop.getDesktop().open(file);
                }
            }

        } catch (Exception e) {
            mostrarError("Error al exportar: " + e.getMessage());
        }
    }

    private void exportarExpedientesExcel() {
        try {
            List<Expediente> expedientes = expedienteService.listarTodos();

            javafx.stage.FileChooser fileChooser = new javafx.stage.FileChooser();
            fileChooser.setTitle("Guardar Excel");
            fileChooser.setInitialFileName("expedientes_" + LocalDate.now() + ".csv");
            fileChooser.getExtensionFilters().add(
                    new javafx.stage.FileChooser.ExtensionFilter("CSV", "*.csv")
            );

            File file = fileChooser.showSaveDialog(stage);
            if (file != null) {
                StringBuilder csv = new StringBuilder();
                csv.append("Número,Carátula,Cliente,Demandado,Fuero,Juzgado,Estado,Fecha Inicio\n");

                for (Expediente exp : expedientes) {
                    csv.append(escapeCsv(exp.getNumero())).append(",");
                    csv.append(escapeCsv(exp.getCaratula())).append(",");
                    csv.append(escapeCsv(exp.getCliente())).append(",");
                    csv.append(escapeCsv(exp.getDemandado())).append(",");
                    csv.append(escapeCsv(exp.getFuero())).append(",");
                    csv.append(escapeCsv(exp.getJuzgado())).append(",");
                    csv.append(exp.getEstado()).append(",");
                    csv.append(exp.getFechaInicio()).append("\n");
                }

                java.nio.file.Files.writeString(file.toPath(), csv.toString());
                mostrarInfo("Excel exportado correctamente");

                if (java.awt.Desktop.isDesktopSupported()) {
                    java.awt.Desktop.getDesktop().open(file);
                }
            }

        } catch (Exception e) {
            mostrarError("Error al exportar: " + e.getMessage());
        }
    }

    private String escapeCsv(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    // ==================== PANEL DE HERRAMIENTAS ====================

    private VBox crearPanelHerramientas() {
        VBox panel = new VBox(20);
        panel.setPadding(new Insets(20));

        Label titulo = new Label("🛠️ Herramientas Jurídicas");
        titulo.setStyle("-fx-font-size: 20px; -fx-font-weight: bold;");

        // ========== CALCULADORA DE PLAZOS ==========
        VBox calculadoraPlazos = new VBox(15);
        calculadoraPlazos.setPadding(new Insets(20));
        calculadoraPlazos.setStyle("-fx-background-color: white; -fx-border-color: #3498db; -fx-border-width: 2; -fx-border-radius: 10;");

        Label lblTituloCalc = new Label("⚖️ Calculadora de Plazos Procesales");
        lblTituloCalc.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

        GridPane gridCalc = new GridPane();
        gridCalc.setHgap(15);
        gridCalc.setVgap(10);

        Label lblFechaInicio = new Label("Fecha de inicio:");
        DatePicker dpFechaInicioPlazo = new DatePicker(LocalDate.now());

        Label lblDias = new Label("Cantidad de días:");
        Spinner<Integer> spDias = new Spinner<>(1, 365, 10);
        spDias.setEditable(true);

        Label lblTipoDias = new Label("Tipo de días:");
        ComboBox<String> cmbTipoDias = new ComboBox<>();
        cmbTipoDias.setItems(FXCollections.observableArrayList("Días hábiles", "Días corridos"));
        cmbTipoDias.setValue("Días hábiles");
        cmbTipoDias.setMaxWidth(Double.MAX_VALUE);

        Label lblResultado = new Label("Resultado:");
        TextField txtResultadoPlazo = new TextField();
        txtResultadoPlazo.setEditable(false);
        txtResultadoPlazo.setStyle("-fx-background-color: #F8F8F6; -fx-font-weight: bold; -fx-font-size: 14px;");

        Button btnCalcular = new Button("🔢 Calcular Plazo");
        //btnCalcular.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-font-weight: bold;");
        btnCalcular.getStyleClass().addAll("button", "button-info");
        btnCalcular.setOnAction(e -> {
            LocalDate fechaInicio = dpFechaInicioPlazo.getValue();
            int dias = spDias.getValue();
            boolean esHabiles = cmbTipoDias.getValue().equals("Días hábiles");

            LocalDate fechaFin = calcularPlazo(fechaInicio, dias, esHabiles);
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy (EEEE)",
                    new java.util.Locale("es", "AR"));
            txtResultadoPlazo.setText(fechaFin.format(formatter));
        });

        Button btnAgregarAgenda = new Button("📅 Agregar a Agenda");
        btnAgregarAgenda.getStyleClass().addAll("button", "button-success");
        btnAgregarAgenda.setOnAction(e -> {
            String resultado = txtResultadoPlazo.getText();
            if (!resultado.isEmpty()) {
                // Pre-cargar evento en agenda
                EventoAgenda evento = new EventoAgenda();
                evento.setTitulo("Vencimiento de plazo");
                evento.setTipo(TipoEvento.VENCIMIENTO);

                // Parsear fecha del resultado
                String fechaStr = resultado.split(" ")[0]; // Obtener solo la fecha
                LocalDate fecha = LocalDate.parse(fechaStr, DateTimeFormatter.ofPattern("dd/MM/yyyy"));
                evento.setFechaHora(LocalDateTime.of(fecha, LocalTime.of(9, 0)));
                evento.setUsuarioId(SesionUsuario.getUsuarioActual().getId());

                try {
                    agendaService.crearEvento(evento);
                    mostrarInfo("Vencimiento agregado a la agenda");
                    actualizarNotificaciones(); // ← AGREGAR ESTA LÍNEA
                    cargarDashboard();
                } catch (SQLException ex) {
                    mostrarError("Error al agregar a agenda: " + ex.getMessage());
                }
            } else {
                mostrarAdvertencia("Primero calcule el plazo");
            }
        });

        gridCalc.add(lblFechaInicio, 0, 0);
        gridCalc.add(dpFechaInicioPlazo, 1, 0);
        gridCalc.add(lblDias, 0, 1);
        gridCalc.add(spDias, 1, 1);
        gridCalc.add(lblTipoDias, 0, 2);
        gridCalc.add(cmbTipoDias, 1, 2);
        gridCalc.add(lblResultado, 0, 3);
        gridCalc.add(txtResultadoPlazo, 1, 3);

        HBox botonesCalc = new HBox(10, btnCalcular, btnAgregarAgenda);
        botonesCalc.setAlignment(Pos.CENTER);

        calculadoraPlazos.getChildren().addAll(lblTituloCalc, gridCalc, botonesCalc);

        // ========== OTRAS HERRAMIENTAS ==========
        HBox herramientasRapidas = new HBox(15);
        herramientasRapidas.setAlignment(Pos.CENTER);

        Button btnExportarTodo = new Button("📊 Exportar Todo a Excel");
        btnExportarTodo.setStyle("-fx-background-color: #16a085; -fx-text-fill: white; -fx-font-size: 14px; -fx-padding: 15;");
        btnExportarTodo.setOnAction(e -> {
            exportarClientesExcel();
            exportarExpedientesExcel();
            mostrarInfo("Datos exportados correctamente");
        });

        Button btnBackup = new Button("💾 Crear Backup de BD");
        btnBackup.setStyle("-fx-background-color: #8e44ad; -fx-text-fill: white; -fx-font-size: 14px; -fx-padding: 15;");
        btnBackup.setOnAction(e -> crearBackupBaseDatos());

        herramientasRapidas.getChildren().addAll(btnExportarTodo, btnBackup);

        panel.getChildren().addAll(titulo, calculadoraPlazos, new Separator(), herramientasRapidas);
        return panel;
    }

    // Método para calcular plazos
    private LocalDate calcularPlazo(LocalDate fechaInicio, int dias, boolean esHabiles) {
        if (!esHabiles) {
            return fechaInicio.plusDays(dias);
        }

        // Calcular días hábiles (lunes a viernes)
        LocalDate fecha = fechaInicio;
        int diasContados = 0;

        while (diasContados < dias) {
            fecha = fecha.plusDays(1);

            // Si no es sábado ni domingo
            if (fecha.getDayOfWeek() != java.time.DayOfWeek.SATURDAY &&
                    fecha.getDayOfWeek() != java.time.DayOfWeek.SUNDAY) {
                diasContados++;
            }
        }

        return fecha;
    }

    // Método para crear backup
    private void crearBackupBaseDatos() {
        try {
            javafx.stage.FileChooser fileChooser = new javafx.stage.FileChooser();
            fileChooser.setTitle("Guardar Backup");
            fileChooser.setInitialFileName("juridix_backup_" + LocalDate.now() + ".db");
            fileChooser.getExtensionFilters().add(
                    new javafx.stage.FileChooser.ExtensionFilter("Base de Datos SQLite", "*.db")
            );

            File destino = fileChooser.showSaveDialog(stage);
            if (destino != null) {
                File origen = new File("juridix.db");
                java.nio.file.Files.copy(origen.toPath(), destino.toPath(),
                        java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                mostrarInfo("Backup creado correctamente en:\n" + destino.getAbsolutePath());
            }

        } catch (Exception e) {
            mostrarError("Error al crear backup: " + e.getMessage());
        }
    }

    // ==================== PANEL DE ECONOMÍA ====================

    private VBox crearPanelEconomia() {
        VBox panel = new VBox(20);
        panel.setPadding(new Insets(20));

        VBox encabezado = new VBox(2);
        Label titulo = new Label("Gestión Económica");
        titulo.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #1A1A18;");
        Label subtitulo = new Label("Honorarios, gastos y pagos del estudio");
        subtitulo.getStyleClass().add("text-secondary");
        encabezado.getChildren().addAll(titulo, subtitulo);

        // ========== RESUMEN FINANCIERO ==========
        HBox resumenFinanciero = new HBox(16);
        resumenFinanciero.setAlignment(Pos.CENTER_LEFT);

        VBox tarjetaHonorarios = crearTarjetaFinanciera("Honorarios Pendientes", "$0", "#3498db");
        VBox tarjetaGastos     = crearTarjetaFinanciera("Total Gastos", "$0", "#e74c3c");
        VBox tarjetaPagos      = crearTarjetaFinanciera("Pagos Recibidos", "$0", "#27ae60");
        VBox tarjetaSaldo      = crearTarjetaFinanciera("Saldo Pendiente", "$0", "#f39c12");

        this.lblHonorariosPendientes  = (Label) tarjetaHonorarios.getChildren().get(1);
        this.lblTotalGastosEconomia   = (Label) tarjetaGastos.getChildren().get(1);
        this.lblPagosRecibidos        = (Label) tarjetaPagos.getChildren().get(1);
        this.lblSaldoPendienteEconomia = (Label) tarjetaSaldo.getChildren().get(1);

        resumenFinanciero.getChildren().addAll(tarjetaHonorarios, tarjetaGastos, tarjetaPagos, tarjetaSaldo);

        actualizarResumenFinanciero();

        // ========== SELECTOR DE EXPEDIENTE ==========
        HBox selectorExpediente = new HBox(10);
        selectorExpediente.setAlignment(Pos.CENTER_LEFT);

        Label lblSeleccionar = new Label("Seleccione un expediente:");
        lblSeleccionar.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

        ComboBox<Expediente> cmbExpedientes = new ComboBox<>();
        cmbExpedientes.setPromptText("Seleccione expediente...");
        cmbExpedientes.setPrefWidth(400);
        this.cmbExpedientesEconomia = cmbExpedientes;

        try {
            List<Expediente> expedientes = expedienteService.listarActivos();
            cmbExpedientes.setItems(FXCollections.observableArrayList(expedientes));
        } catch (SQLException e) {
            mostrarError("Error al cargar expedientes: " + e.getMessage());
        }

        selectorExpediente.getChildren().addAll(lblSeleccionar, cmbExpedientes);

        // ========== PESTAÑAS DE GESTIÓN ==========
        TabPane tabPaneEconomia = new TabPane();

        Tab tabHonorarios = new Tab("💵 Honorarios");
        tabHonorarios.setClosable(false);
        tabHonorarios.setContent(crearPanelHonorarios(cmbExpedientes));

        Tab tabGastos = new Tab("💸 Gastos");
        tabGastos.setClosable(false);
        tabGastos.setContent(crearPanelGastos(cmbExpedientes));

        Tab tabPagos = new Tab("💳 Pagos");
        tabPagos.setClosable(false);
        tabPagos.setContent(crearPanelPagos(cmbExpedientes));

        Tab tabCuentaCorriente = new Tab("📊 Cuenta Corriente");
        tabCuentaCorriente.setClosable(false);
        tabCuentaCorriente.setContent(crearPanelCuentaCorriente(cmbExpedientes));

        tabPaneEconomia.getTabs().addAll(tabHonorarios, tabGastos, tabPagos, tabCuentaCorriente);

        panel.getChildren().addAll(encabezado, resumenFinanciero, new Separator(), selectorExpediente, tabPaneEconomia);
        VBox.setVgrow(tabPaneEconomia, Priority.ALWAYS);

        return panel;
    }

    private void actualizarResumenFinanciero() {
        if (lblHonorariosPendientes == null) return; // panel aún no creado
        try {
            double totalHonorariosPendientes = calcularTotalHonorariosPendientes();
            double totalGastos = calcularTotalGastos();
            double totalPagos = calcularTotalPagos();
            double saldoPendiente = totalHonorariosPendientes - totalPagos;

            lblHonorariosPendientes.setText(formatearMoneda(totalHonorariosPendientes));
            lblTotalGastosEconomia.setText(formatearMoneda(totalGastos));
            lblPagosRecibidos.setText(formatearMoneda(totalPagos));
            lblSaldoPendienteEconomia.setText(formatearMoneda(saldoPendiente));
        } catch (SQLException e) {
            mostrarError("Error al actualizar resumen financiero: " + e.getMessage());
        }
    }

    // Tarjeta financiera
    private VBox crearTarjetaFinanciera(String titulo, String valor, String color) {
        VBox tarjeta = new VBox(6);
        tarjeta.getStyleClass().add("stat-card");
        tarjeta.setPrefSize(200, 90);

        Label lblTitulo = new Label(titulo);
        lblTitulo.getStyleClass().add("stat-label");

        Label lblValor = new Label(valor);
        lblValor.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: " + color + ";");

        tarjeta.getChildren().addAll(lblTitulo, lblValor);
        return tarjeta;
    }

    private VBox crearPanelHonorarios(ComboBox<Expediente> cmbExpedientes) {
        VBox panel = new VBox(15);
        panel.setPadding(new Insets(15));
        ObservableList<Honorario> listaHonorarios = FXCollections.observableArrayList();
        // Botón nuevo honorario
        Button btnNuevo = new Button("+ Nuevo Honorario");
        btnNuevo.getStyleClass().add("btn-primary");

        btnNuevo.setOnAction(e -> {
            Expediente exp = cmbExpedientes.getValue();
            if (exp != null) {
                abrirFormularioHonorario(null, exp.getId(), listaHonorarios);
            } else {
                mostrarAdvertencia("Seleccione un expediente primero");
            }
        });

        // Listener para cambio de expediente
        cmbExpedientes.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                cargarHonorariosPorExpediente(newVal.getId(), listaHonorarios);
            }
        });

        // Tabla de honorarios
        TableView<Honorario> tablaHonorarios = new TableView<>();
        tablaHonorarios.setItems(listaHonorarios);
        tablaHonorarios.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        tablaHonorarios.getStyleClass().add("table-view");

        TableColumn<Honorario, TipoHonorario> colTipo = new TableColumn<>("Tipo");
        colTipo.setCellValueFactory(new PropertyValueFactory<>("tipo"));
        colTipo.setPrefWidth(200);

        TableColumn<Honorario, String> colMonto = new TableColumn<>("Monto");
        colMonto.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(cellData.getValue().getMontoFormateado()));
        colMonto.setPrefWidth(150);

        TableColumn<Honorario, EstadoHonorario> colEstado = new TableColumn<>("Estado");
        colEstado.setCellValueFactory(new PropertyValueFactory<>("estado"));
        colEstado.setPrefWidth(150);

        TableColumn<Honorario, LocalDate> colFechaEstimada = new TableColumn<>("Fecha Estimada");
        colFechaEstimada.setCellValueFactory(new PropertyValueFactory<>("fechaEstimada"));
        colFechaEstimada.setPrefWidth(130);
        colFechaEstimada.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(LocalDate fecha, boolean empty) {
                super.updateItem(fecha, empty);
                setText((empty || fecha == null) ? null : fecha.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
            }
        });

        TableColumn<Honorario, String> colDescripcion = new TableColumn<>("Descripción");
        colDescripcion.setCellValueFactory(new PropertyValueFactory<>("descripcion"));
        colDescripcion.setPrefWidth(200);

        // Columna acciones
        TableColumn<Honorario, Void> colAcciones = new TableColumn<>("Acciones");
        colAcciones.setPrefWidth(210);
        colAcciones.setCellFactory(param -> new TableCell<>() {
            private final Button btnEditar = new Button("Editar");
            private final Button btnCobrar = new Button("Cobrar");
            private final Button btnEliminar = new Button("Eliminar");

            {
                btnEditar.setOnAction(e -> {
                    Honorario h = getTableView().getItems().get(getIndex());
                    abrirFormularioHonorario(h, h.getExpedienteId(), listaHonorarios);
                });

                btnEditar.getStyleClass().add("btn-ghost");
                btnCobrar.getStyleClass().add("button-success");;
                btnCobrar.setOnAction(e -> {
                    Honorario h = getTableView().getItems().get(getIndex());
                    try {
                        honorarioService.marcarComoCobrado(h.getId());
                        mostrarInfo("Honorario marcado como cobrado");
                        cargarHonorariosPorExpediente(cmbExpedientes.getValue().getId(), listaHonorarios);
                        actualizarResumenFinanciero();
                    } catch (SQLException ex) {
                        mostrarError("Error: " + ex.getMessage());
                    }
                });

                btnEliminar.getStyleClass().add("btn-danger");
                btnEliminar.setOnAction(e -> {
                    Honorario h = getTableView().getItems().get(getIndex());
                    if (mostrarConfirmacion("¿Eliminar este honorario?")) {
                        try {
                            honorarioService.eliminarHonorario(h.getId());
                            listaHonorarios.remove(h);
                            actualizarResumenFinanciero();
                            mostrarInfo("Honorario eliminado");
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
                    HBox botones = new HBox(5, btnEditar, btnCobrar, btnEliminar);
                    setGraphic(botones);
                }
            }
        });

        tablaHonorarios.getColumns().addAll(colMonto, colEstado, colFechaEstimada, colDescripcion, colAcciones);

        // Listener para cambio de expediente
        cmbExpedientes.setOnAction(e -> {
            Expediente exp = cmbExpedientes.getValue();
            if (exp != null) {
                cargarHonorariosPorExpediente(exp.getId(), listaHonorarios);
            }
        });

        panel.getChildren().addAll(btnNuevo, tablaHonorarios);
        VBox.setVgrow(tablaHonorarios, Priority.ALWAYS);

        return panel;
    }

    private void cargarHonorariosPorExpediente(Integer expedienteId, ObservableList<Honorario> lista) {
        try {
            List<Honorario> honorarios = honorarioService.listarPorExpediente(expedienteId);
            lista.clear();
            lista.addAll(honorarios);
        } catch (SQLException e) {
            mostrarError("Error al cargar honorarios: " + e.getMessage());
        }
    }

    private void abrirFormularioHonorario(Honorario honorario, Integer expedienteId, ObservableList<Honorario> listaTabla) {

        // Recargar lista de expedientes por si hay nuevos
        ComboBox<Expediente> cmbExpedientesForm = new ComboBox<>();
        try {
            List<Expediente> expedientes = expedienteService.listarActivos();
            cmbExpedientesForm.setItems(FXCollections.observableArrayList(expedientes));
            // Pre-seleccionar el expediente actual
            if (expedienteId != null) {
                expedientes.stream()
                        .filter(e -> e.getId().equals(expedienteId))
                        .findFirst()
                        .ifPresent(cmbExpedientesForm::setValue);
            }
        } catch (SQLException e) {
            mostrarError("Error al cargar expedientes: " + e.getMessage());
        }

        Stage ventana = new Stage();
        ventana.initModality(Modality.APPLICATION_MODAL);
        ventana.setTitle(honorario == null ? "Nuevo Honorario" : "Editar Honorario");

        VBox form = new VBox(15);
        form.setPadding(new Insets(20));

        ComboBox<TipoHonorario> cmbTipo = new ComboBox<>();
        cmbTipo.setItems(FXCollections.observableArrayList(TipoHonorario.MONTO_FIJO));
        cmbTipo.setMaxWidth(Double.MAX_VALUE);

        TextField txtPorcentaje = new TextField();
        txtPorcentaje.setPromptText("Ej: 30");

        TextField txtMontoFijo = new TextField();
        txtMontoFijo.setPromptText("Ej: 150000");

        TextField txtMontoCalculado = new TextField();
        txtMontoCalculado.setPromptText("Para regulación judicial o cálculo manual");

        DatePicker dpFechaEstimada = new DatePicker();

        TextArea txtDescripcion = new TextArea();
        txtDescripcion.setPrefRowCount(3);

        ComboBox<EstadoHonorario> cmbEstado = new ComboBox<>();
        cmbEstado.setItems(FXCollections.observableArrayList(EstadoHonorario.values()));
        cmbEstado.setValue(EstadoHonorario.PENDIENTE);
        cmbEstado.setMaxWidth(Double.MAX_VALUE);

        // Cargar datos si es edición
        if (honorario != null) {
            cmbTipo.setValue(honorario.getTipo());
            if (honorario.getPorcentaje() != null) txtPorcentaje.setText(honorario.getPorcentaje().toString());
            if (honorario.getMontoFijo() != null) txtMontoFijo.setText(honorario.getMontoFijo().toString());
            if (honorario.getMontoCalculado() != null) txtMontoCalculado.setText(honorario.getMontoCalculado().toString());
            dpFechaEstimada.setValue(honorario.getFechaEstimada());
            txtDescripcion.setText(honorario.getDescripcion());
            cmbEstado.setValue(honorario.getEstado());
        }

        // Deshabilitar campos según tipo
        cmbTipo.setOnAction(e -> {
            TipoHonorario tipo = cmbTipo.getValue();
            txtPorcentaje.setDisable(tipo != TipoHonorario.PORCENTAJE);
            txtMontoFijo.setDisable(tipo != TipoHonorario.MONTO_FIJO);
            txtMontoCalculado.setDisable(tipo == TipoHonorario.MONTO_FIJO);
        });

        form.getChildren().addAll(
                new Label("Monto ($) *:"), txtMontoFijo,
                new Label("Fecha Estimada de Cobro:"), dpFechaEstimada,
                new Label("Descripción:"), txtDescripcion,
                new Label("Estado:"), cmbEstado
        );

        HBox botones = new HBox(10);
        botones.setAlignment(Pos.CENTER);

        Button btnGuardar = new Button("💾 Guardar");
        btnGuardar.getStyleClass().addAll("button", "button-success");
        btnGuardar.setOnAction(e -> {
            try {
                Honorario h = honorario != null ? honorario : new Honorario();
                h.setExpedienteId(expedienteId);
                h.setTipo(TipoHonorario.MONTO_FIJO);

                if (!txtPorcentaje.getText().isEmpty()) {
                    h.setPorcentaje(Double.parseDouble(txtPorcentaje.getText()));
                }

                if (!txtMontoFijo.getText().isEmpty()) {
                    double monto = Double.parseDouble(txtMontoFijo.getText());
                    h.setMontoFijo(monto);
                    h.setMontoCalculado(monto);
                }

                h.setFechaEstimada(dpFechaEstimada.getValue());
                h.setDescripcion(txtDescripcion.getText());
                h.setEstado(cmbEstado.getValue());
                h.setUsuarioId(SesionUsuario.getUsuarioActual().getId());

                if (honorario == null) {
                    honorarioService.crearHonorario(h);
                    mostrarInfo("Honorario creado correctamente");
                } else {
                    honorarioService.actualizarHonorario(h);
                    mostrarInfo("Honorario actualizado correctamente");
                }

                // Recargar la tabla
                if (listaTabla != null && expedienteId != null) {
                    cargarHonorariosPorExpediente(expedienteId, listaTabla);
                }
                actualizarResumenFinanciero();
                ventana.close();

            } catch (NumberFormatException ex) {
                mostrarError("Los montos deben ser números válidos");
            } catch (Exception ex) {
                mostrarError("Error: " + ex.getMessage());
            }
        });

        Button btnCancelar = new Button("❌ Cancelar");
        btnCancelar.setOnAction(e -> ventana.close());

        botones.getChildren().addAll(btnGuardar, btnCancelar);
        form.getChildren().add(botones);

        VBox root = new VBox(form);
        root.setPadding(new Insets(20));
        root.setStyle("-fx-background-color: white;");

        Scene scene = new Scene(root, 420, 480);
        scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
        ventana.setScene(scene);
        ventana.showAndWait();
    }

    private VBox crearPanelGastos(ComboBox<Expediente> cmbExpedientes) {
        VBox panel = new VBox(15);
        panel.setPadding(new Insets(15));
        ObservableList<Gasto> listaGastos = FXCollections.observableArrayList();

        Button btnNuevo = new Button("+ Nuevo Gasto");
        btnNuevo.getStyleClass().add("btn-primary");
        btnNuevo.setOnAction(e -> {
            Expediente exp = cmbExpedientes.getValue();
            if (exp != null) {
                abrirFormularioGasto(null, exp.getId(), listaGastos);
            } else {
                mostrarAdvertencia("Seleccione un expediente primero");
            }
        });

        TableView<Gasto> tablaGastos = new TableView<>();
        tablaGastos.setItems(listaGastos);
        tablaGastos.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        tablaGastos.getStyleClass().add("table-view");

        TableColumn<Gasto, LocalDate> colFecha = new TableColumn<>("Fecha");
        colFecha.setCellValueFactory(new PropertyValueFactory<>("fecha"));
        colFecha.setPrefWidth(120);
        colFecha.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(LocalDate fecha, boolean empty) {
                super.updateItem(fecha, empty);
                setText((empty || fecha == null) ? null : fecha.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
            }
        });

        TableColumn<Gasto, String> colConcepto = new TableColumn<>("Concepto");
        colConcepto.setCellValueFactory(new PropertyValueFactory<>("concepto"));
        colConcepto.setPrefWidth(250);

        TableColumn<Gasto, String> colCategoria = new TableColumn<>("Categoría");
        colCategoria.setCellValueFactory(new PropertyValueFactory<>("categoria"));
        colCategoria.setPrefWidth(150);

        TableColumn<Gasto, String> colMonto = new TableColumn<>("Monto");
        colMonto.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(cellData.getValue().getMontoFormateado()));
        colMonto.setPrefWidth(120);

        TableColumn<Gasto, Void> colAcciones = new TableColumn<>("Acciones");
        colAcciones.setPrefWidth(150);
        colAcciones.setCellFactory(param -> new TableCell<>() {
            private final Button btnEditar = new Button("Editar");
            private final Button btnEliminar = new Button("Eliminar");

            {
                btnEditar.getStyleClass().add("btn-ghost");
                btnEditar.setOnAction(e -> {
                    Gasto g = getTableView().getItems().get(getIndex());
                    abrirFormularioGasto(g, g.getExpedienteId(), listaGastos);
                });

                btnEliminar.getStyleClass().add("btn-danger");
                btnEliminar.setOnAction(e -> {
                    Gasto g = getTableView().getItems().get(getIndex());
                    if (mostrarConfirmacion("¿Eliminar este gasto?")) {
                        try {
                            gastoService.eliminarGasto(g.getId());
                            listaGastos.remove(g);
                            actualizarResumenFinanciero();
                            mostrarInfo("Gasto eliminado");
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
                    HBox botones = new HBox(5, btnEditar, btnEliminar);
                    setGraphic(botones);
                }
            }
        });

        tablaGastos.getColumns().addAll(colFecha, colConcepto, colCategoria, colMonto, colAcciones);

        cmbExpedientes.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                cargarGastosPorExpediente(newVal.getId(), listaGastos);
            }
        });

        panel.getChildren().addAll(btnNuevo, tablaGastos);
        VBox.setVgrow(tablaGastos, Priority.ALWAYS);

        return panel;
    }

    private void cargarGastosPorExpediente(Integer expedienteId, ObservableList<Gasto> lista) {
        try {
            List<Gasto> gastos = gastoService.listarPorExpediente(expedienteId);
            lista.clear();
            lista.addAll(gastos);
        } catch (SQLException e) {
            mostrarError("Error al cargar gastos: " + e.getMessage());
        }
    }

    private void abrirFormularioGasto(Gasto gasto, Integer expedienteId, ObservableList<Gasto> listaTabla) {
        Stage ventana = new Stage();
        ventana.initModality(Modality.APPLICATION_MODAL);
        ventana.setTitle(gasto == null ? "Nuevo Gasto" : "Editar Gasto");

        VBox form = new VBox(15);
        form.setPadding(new Insets(20));

        TextField txtConcepto = new TextField();
        txtConcepto.setPromptText("Ej: Tasa judicial");

        TextField txtMonto = new TextField();
        txtMonto.setPromptText("Ej: 5000");

        DatePicker dpFecha = new DatePicker(LocalDate.now());

        ComboBox<String> cmbCategoria = new ComboBox<>();
        cmbCategoria.setItems(FXCollections.observableArrayList(
                "Tasa judicial", "Pericia", "Traslado", "Fotocopias",
                "Notificación", "Publicación", "Otro"
        ));
        cmbCategoria.setMaxWidth(Double.MAX_VALUE);

        TextField txtComprobante = new TextField();
        txtComprobante.setPromptText("Número de comprobante");

        TextArea txtObservaciones = new TextArea();
        txtObservaciones.setPrefRowCount(3);

        if (gasto != null) {
            txtConcepto.setText(gasto.getConcepto());
            txtMonto.setText(gasto.getMonto().toString());
            dpFecha.setValue(gasto.getFecha());
            cmbCategoria.setValue(gasto.getCategoria());
            txtComprobante.setText(gasto.getComprobante());
            txtObservaciones.setText(gasto.getObservaciones());
        }

        form.getChildren().addAll(
                new Label("Concepto *:"), txtConcepto,
                new Label("Monto ($) *:"), txtMonto,
                new Label("Fecha *:"), dpFecha,
                new Label("Categoría:"), cmbCategoria,
                new Label("Comprobante:"), txtComprobante,
                new Label("Observaciones:"), txtObservaciones
        );

        HBox botones = new HBox(10);
        botones.setAlignment(Pos.CENTER);

        Button btnGuardar = new Button("💾 Guardar");
        btnGuardar.getStyleClass().addAll("button", "button-success");
        btnGuardar.setOnAction(e -> {
            try {
                Gasto g = gasto != null ? gasto : new Gasto();
                g.setExpedienteId(expedienteId);
                g.setConcepto(txtConcepto.getText());
                g.setMonto(Double.parseDouble(txtMonto.getText()));
                g.setFecha(dpFecha.getValue());
                g.setCategoria(cmbCategoria.getValue());
                g.setComprobante(txtComprobante.getText());
                g.setObservaciones(txtObservaciones.getText());
                g.setUsuarioId(SesionUsuario.getUsuarioActual().getId());

                if (gasto == null) {
                    gastoService.crearGasto(g);
                    mostrarInfo("Gasto registrado correctamente");
                } else {
                    gastoService.actualizarGasto(g);
                    mostrarInfo("Gasto actualizado correctamente");
                }
                // Recargar la tabla
                if (listaTabla != null && expedienteId != null) {
                    cargarGastosPorExpediente(expedienteId, listaTabla);
                }

                actualizarResumenFinanciero();
                ventana.close();

            } catch (NumberFormatException ex) {
                mostrarError("El monto debe ser un número válido");
            } catch (Exception ex) {
                mostrarError("Error: " + ex.getMessage());
            }
        });

        Button btnCancelar = new Button("❌ Cancelar");
        btnCancelar.setOnAction(e -> ventana.close());

        botones.getChildren().addAll(btnGuardar, btnCancelar);
        form.getChildren().add(botones);

        Scene scene = new Scene(form, 500, 550);
        ventana.setScene(scene);
        ventana.showAndWait();
    }

    // ==================== MÉTODOS DE CÁLCULO GLOBALES ====================

    private double calcularTotalHonorariosPendientes() throws SQLException {
        List<Honorario> pendientes = honorarioService.listarPendientes();
        return pendientes.stream()
                .mapToDouble(h -> h.getMontoCalculado() != null ? h.getMontoCalculado() : 0.0)
                .sum();
    }

    private double calcularTotalGastos() throws SQLException {
        // Sumar todos los gastos de todos los expedientes
        List<Expediente> expedientes = expedienteService.listarActivos();
        double total = 0.0;
        for (Expediente exp : expedientes) {
            Double totalExp = gastoService.calcularTotalPorExpediente(exp.getId());
            total += (totalExp != null ? totalExp : 0.0);
        }
        return total;
    }

    private double calcularTotalPagos() throws SQLException {
        // Sumar todos los pagos de todos los expedientes
        List<Expediente> expedientes = expedienteService.listarActivos();
        double total = 0.0;
        for (Expediente exp : expedientes) {
            Double totalExp = pagoService.calcularTotalPorExpediente(exp.getId());
            total += (totalExp != null ? totalExp : 0.0);
        }
        return total;
    }

    // ==================== PANEL DE PAGOS ====================

    private VBox crearPanelPagos(ComboBox<Expediente> cmbExpedientes) {
        VBox panel = new VBox(15);
        panel.setPadding(new Insets(15));

        Button btnNuevo = new Button("+ Registrar Pago");
        btnNuevo.getStyleClass().add("btn-primary");

        TableView<Pago> tablaPagos = new TableView<>();
        tablaPagos.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        tablaPagos.getStyleClass().add("table-view");

        TableColumn<Pago, LocalDate> colFecha = new TableColumn<>("Fecha");
        colFecha.setCellValueFactory(data -> new SimpleObjectProperty<>(data.getValue().getFecha()));
        colFecha.setPrefWidth(100);
        colFecha.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(LocalDate fecha, boolean empty) {
                super.updateItem(fecha, empty);
                setText((empty || fecha == null) ? null : fecha.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
            }
        });

        TableColumn<Pago, String> colMonto = new TableColumn<>("Monto");
        colMonto.setCellValueFactory(data ->
                new SimpleStringProperty(formatearMoneda(data.getValue().getMonto())));
        colMonto.setPrefWidth(100);

        TableColumn<Pago, String> colFormaPago = new TableColumn<>("Forma de Pago");
        colFormaPago.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getFormaPago()));
        colFormaPago.setPrefWidth(120);

        TableColumn<Pago, String> colReferencia = new TableColumn<>("Referencia");
        colReferencia.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getReferencia()));
        colReferencia.setPrefWidth(150);

        TableColumn<Pago, String> colConcepto = new TableColumn<>("Concepto");
        colConcepto.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getConcepto()));
        colConcepto.setPrefWidth(200);

        TableColumn<Pago, Void> colAcciones = new TableColumn<>("Acciones");
        colAcciones.setPrefWidth(150);
        colAcciones.setCellFactory(param -> new TableCell<>() {
            private final Button btnEditar = new Button("Editar");
            private final Button btnEliminar = new Button("Eliminar");

            {
                btnEditar.getStyleClass().add("btn-ghost");
                btnEditar.setOnAction(e -> {
                    Pago p = getTableView().getItems().get(getIndex());
                    Expediente exp = cmbExpedientes.getValue();
                    abrirFormularioPago(p, exp, null);
                    cargarTablaPagos(getTableView(), exp.getId());
                    actualizarResumenFinanciero();
                });

                btnEliminar.getStyleClass().add("btn-danger");
                btnEliminar.setOnAction(e -> {
                    Pago p = getTableView().getItems().get(getIndex());
                    if (mostrarConfirmacion("¿Eliminar este pago?")) {
                        try {
                            pagoService.eliminarPago(p.getId());
                            getTableView().getItems().remove(p);
                            mostrarInfo("Pago eliminado");
                            actualizarResumenFinanciero();
                        } catch (SQLException ex) {
                            mostrarError("Error: " + ex.getMessage());
                        }
                    }
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : new HBox(5, btnEditar, btnEliminar));
            }
        });

        tablaPagos.getColumns().addAll(colFecha, colMonto, colFormaPago, colReferencia, colConcepto, colAcciones);

        // Listener para cargar pagos cuando se selecciona expediente
        cmbExpedientes.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                cargarTablaPagos(tablaPagos, newVal.getId());
            }
        });

        // Wrapper para recargar la tabla actual desde el botón "Nuevo"
        Runnable recargar = () -> {
            Expediente exp = cmbExpedientes.getValue();
            if (exp != null) {
                cargarTablaPagos(tablaPagos, exp.getId());
                actualizarResumenFinanciero();
            }
        };
        btnNuevo.setOnAction(e -> {
            Expediente exp = cmbExpedientes.getValue();
            if (exp != null) {
                abrirFormularioPago(null, exp, null);
                recargar.run();
            } else {
                mostrarAdvertencia("Seleccione un expediente primero");
            }
        });

        panel.getChildren().addAll(btnNuevo, tablaPagos);
        VBox.setVgrow(tablaPagos, Priority.ALWAYS);

        return panel;
    }

    private void cargarTablaPagos(TableView<Pago> tabla, Integer expedienteId) {
        try {
            List<Pago> pagos = pagoService.listarPorExpediente(expedienteId);
            tabla.setItems(FXCollections.observableArrayList(pagos));
        } catch (SQLException ex) {
            mostrarError("Error al cargar pagos: " + ex.getMessage());
        }
    }

    // ==================== PANEL CUENTA CORRIENTE ====================

    private VBox crearPanelCuentaCorriente(ComboBox<Expediente> cmbExpedientes) {
        VBox panel = new VBox(15);
        panel.setPadding(new Insets(20));

        // Título
        Label titulo = new Label("Estado de Cuenta Corriente");
        titulo.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        // Resumen del expediente
        HBox resumen = new HBox(20);
        resumen.setAlignment(Pos.CENTER);

        VBox cardHonorarios = crearTarjetaFinanciera("Honorarios", "$0.00", "#3498db");
        VBox cardGastos = crearTarjetaFinanciera("Gastos", "$0.00", "#e74c3c");
        VBox cardPagos = crearTarjetaFinanciera("Pagos", "$0.00", "#27ae60");
        VBox cardSaldo = crearTarjetaFinanciera("Saldo", "$0.00", "#f39c12");

        resumen.getChildren().addAll(cardHonorarios, cardGastos, cardPagos, cardSaldo);

        // Tabla de movimientos
        TableView<MovimientoCuenta> tablaMovimientos = new TableView<>();
        tablaMovimientos.setPrefHeight(400);

        TableColumn<MovimientoCuenta, LocalDate> colFecha = new TableColumn<>("Fecha");
        colFecha.setCellValueFactory(data -> new SimpleObjectProperty<>(data.getValue().fecha));
        colFecha.setPrefWidth(100);

        TableColumn<MovimientoCuenta, String> colTipo = new TableColumn<>("Tipo");
        colTipo.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().tipo));
        colTipo.setPrefWidth(100);

        TableColumn<MovimientoCuenta, String> colDescripcion = new TableColumn<>("Descripción");
        colDescripcion.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().descripcion));
        colDescripcion.setPrefWidth(300);

        TableColumn<MovimientoCuenta, String> colDebe = new TableColumn<>("Debe");
        colDebe.setCellValueFactory(data -> {
            double debe = data.getValue().debe;
            return new SimpleStringProperty(debe > 0 ? formatearMoneda(debe) : "-");
        });
        colDebe.setPrefWidth(100);
        colDebe.setStyle("-fx-alignment: CENTER-RIGHT;");

        TableColumn<MovimientoCuenta, String> colHaber = new TableColumn<>("Haber");
        colHaber.setCellValueFactory(data -> {
            double haber = data.getValue().haber;
            return new SimpleStringProperty(haber > 0 ? formatearMoneda(haber) : "-");
        });
        colHaber.setPrefWidth(100);
        colHaber.setStyle("-fx-alignment: CENTER-RIGHT;");

        TableColumn<MovimientoCuenta, String> colSaldo = new TableColumn<>("Saldo");
        colSaldo.setCellValueFactory(data ->
                new SimpleStringProperty(formatearMoneda(data.getValue().saldo)));
        colSaldo.setPrefWidth(120);
        colSaldo.setStyle("-fx-alignment: CENTER-RIGHT;");

        tablaMovimientos.getColumns().addAll(colFecha, colTipo, colDescripcion, colDebe, colHaber, colSaldo);

        // Listener para actualizar cuando cambia el expediente
        cmbExpedientes.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                cargarCuentaCorriente(newVal.getId(), resumen, tablaMovimientos,
                        cardHonorarios, cardGastos, cardPagos, cardSaldo);
            }
        });

        panel.getChildren().addAll(titulo, resumen, new Separator(), tablaMovimientos);

        return panel;
    }

    private void cargarCuentaCorriente(Integer expedienteId, HBox resumen,
                                       TableView<MovimientoCuenta> tabla, VBox cardHonorarios, VBox cardGastos,
                                       VBox cardPagos, VBox cardSaldo) {
        try {
            // Obtener datos
            List<Honorario> honorarios = honorarioService.listarPorExpediente(expedienteId);
            List<Gasto> gastos = gastoService.listarPorExpediente(expedienteId);
            List<Pago> pagos = pagoService.listarPorExpediente(expedienteId);

            // Calcular totales
            double totalHonorarios = honorarios.stream()
                    .mapToDouble(h -> h.getMontoCalculado() != null ? h.getMontoCalculado() : 0.0)
                    .sum();

            double totalGastos = gastos.stream()
                    .mapToDouble(g -> g.getMonto() != null ? g.getMonto() : 0.0)
                    .sum();

            double totalPagos = pagos.stream()
                    .mapToDouble(p -> p.getMonto() != null ? p.getMonto() : 0.0)
                    .sum();

            double saldo = (totalHonorarios + totalGastos) - totalPagos;

            // Actualizar tarjetas
            actualizarTarjeta(cardHonorarios, formatearMoneda(totalHonorarios));
            actualizarTarjeta(cardGastos, formatearMoneda(totalGastos));
            actualizarTarjeta(cardPagos, formatearMoneda(totalPagos));
            actualizarTarjeta(cardSaldo, formatearMoneda(saldo));

            // Crear lista de movimientos
            List<MovimientoCuenta> movimientos = new ArrayList<>();
            double saldoAcumulado = 0.0;

            // Agregar honorarios
            for (Honorario h : honorarios) {
                saldoAcumulado += h.getMontoCalculado() != null ? h.getMontoCalculado() : 0.0;
                movimientos.add(new MovimientoCuenta(
                        h.getFechaCreacion().toLocalDate(),
                        "Honorario",
                        h.getDescripcion(),
                        h.getMontoCalculado() != null ? h.getMontoCalculado() : 0.0,
                        0.0,
                        saldoAcumulado
                ));
            }

            // Agregar gastos
            for (Gasto g : gastos) {
                saldoAcumulado += g.getMonto() != null ? g.getMonto() : 0.0;
                movimientos.add(new MovimientoCuenta(
                        g.getFecha(),
                        "Gasto",
                        g.getConcepto(),
                        g.getMonto() != null ? g.getMonto() : 0.0,
                        0.0,
                        saldoAcumulado
                ));
            }

            // Agregar pagos
            for (Pago p : pagos) {
                saldoAcumulado -= p.getMonto() != null ? p.getMonto() : 0.0;
                movimientos.add(new MovimientoCuenta(
                        p.getFecha(),
                        "Pago",
                        p.getConcepto() != null ? p.getConcepto() : "Pago recibido",
                        0.0,
                        p.getMonto() != null ? p.getMonto() : 0.0,
                        saldoAcumulado
                ));
            }

            // Ordenar por fecha
            movimientos.sort(Comparator.comparing(m -> m.fecha));

            // Recalcular saldo acumulado después de ordenar
            saldoAcumulado = 0.0;
            for (MovimientoCuenta m : movimientos) {
                saldoAcumulado += m.debe - m.haber;
                m.saldo = saldoAcumulado;
            }

            tabla.setItems(FXCollections.observableArrayList(movimientos));

        } catch (SQLException ex) {
            mostrarError("Error al cargar cuenta corriente: " + ex.getMessage());
        }
    }

    private void actualizarTarjeta(VBox tarjeta, String nuevoValor) {
        Label lblValor = (Label) tarjeta.getChildren().get(1);
        lblValor.setText(nuevoValor);
    }

    // Clase auxiliar para los movimientos de cuenta corriente
    private static class MovimientoCuenta {
        LocalDate fecha;
        String tipo;
        String descripcion;
        double debe;
        double haber;
        double saldo;

        public MovimientoCuenta(LocalDate fecha, String tipo, String descripcion,
                                double debe, double haber, double saldo) {
            this.fecha = fecha;
            this.tipo = tipo;
            this.descripcion = descripcion;
            this.debe = debe;
            this.haber = haber;
            this.saldo = saldo;
        }
    }
    private boolean mostrarConfirmacion(String mensaje) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmación");
        alert.setHeaderText(null);
        alert.setContentText(mensaje);

        Optional<ButtonType> resultado = alert.showAndWait();
        return resultado.isPresent() && resultado.get() == ButtonType.OK;
    }

    // ========== CREAR PANEL DE USUARIOS ==========
    private VBox crearPanelUsuarios() {
        VBox panel = new VBox(15);
        panel.setPadding(new Insets(15));

        Label titulo = new Label("👥 Gestión de Usuarios");
        titulo.setStyle("-fx-font-size: 20px; -fx-font-weight: bold;");

        // Barra de control
        HBox barraControl = new HBox(10);
        barraControl.setAlignment(Pos.CENTER_LEFT);

        Button btnNuevo = new Button("➕ Nuevo Usuario");
        btnNuevo.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-font-weight: bold;");
        btnNuevo.setOnAction(e -> abrirFormularioUsuario(null));

        Button btnActualizar = new Button("🔄 Actualizar");
        btnActualizar.setOnAction(e -> cargarUsuarios());

        barraControl.getChildren().addAll(btnNuevo, btnActualizar);

        // Tabla de usuarios
        TableView<Usuario> tablaUsuarios = new TableView<>();
        ObservableList<Usuario> listaUsuarios = FXCollections.observableArrayList();
        tablaUsuarios.setItems(listaUsuarios);

        TableColumn<Usuario, String> colUsername = new TableColumn<>("Usuario");
        colUsername.setCellValueFactory(new PropertyValueFactory<>("username"));
        colUsername.setPrefWidth(150);

        TableColumn<Usuario, String> colNombre = new TableColumn<>("Nombre Completo");
        colNombre.setCellValueFactory(new PropertyValueFactory<>("nombreCompleto"));
        colNombre.setPrefWidth(250);

        TableColumn<Usuario, RolUsuario> colRol = new TableColumn<>("Rol");
        colRol.setCellValueFactory(new PropertyValueFactory<>("rol"));
        colRol.setPrefWidth(150);

        TableColumn<Usuario, String> colEmail = new TableColumn<>("Email");
        colEmail.setCellValueFactory(new PropertyValueFactory<>("email"));
        colEmail.setPrefWidth(200);

        TableColumn<Usuario, Boolean> colActivo = new TableColumn<>("Estado");
        colActivo.setCellValueFactory(new PropertyValueFactory<>("activo"));
        colActivo.setPrefWidth(100);
        colActivo.setCellFactory(column -> new TableCell<Usuario, Boolean>() {
            @Override
            protected void updateItem(Boolean item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item ? "✅ Activo" : "❌ Inactivo");
                    setStyle(item ? "-fx-text-fill: #27ae60;" : "-fx-text-fill: #e74c3c;");
                }
            }
        });

        TableColumn<Usuario, LocalDateTime> colUltimoAcceso = new TableColumn<>("Último Acceso");
        colUltimoAcceso.setCellValueFactory(new PropertyValueFactory<>("ultimoAcceso"));
        colUltimoAcceso.setPrefWidth(150);
        colUltimoAcceso.setCellFactory(column -> new TableCell<Usuario, LocalDateTime>() {
            @Override
            protected void updateItem(LocalDateTime item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText("Nunca");
                } else {
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
                    setText(item.format(formatter));
                }
            }
        });

        // Columna acciones
        TableColumn<Usuario, Void> colAcciones = new TableColumn<>("Acciones");
        colAcciones.setPrefWidth(200);
        colAcciones.setCellFactory(param -> new TableCell<>() {
            private final Button btnEditar = new Button("✏️ Editar");
            private final Button btnEliminar = new Button("🗑️ Eliminar");

            {
                btnEditar.setOnAction(e -> {
                    Usuario usuario = getTableView().getItems().get(getIndex());
                    abrirFormularioUsuario(usuario);
                });

                btnEliminar.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white;");
                btnEliminar.setOnAction(e -> {
                    Usuario usuario = getTableView().getItems().get(getIndex());

                    // No permitir eliminar al usuario logueado
                    if (usuario.getId().equals(SesionUsuario.getUsuarioActual().getId())) {
                        mostrarError("No puedes eliminar tu propio usuario");
                        return;
                    }

                    try {
                        // Contar registros creados por este usuario
                        ExpedienteDAO expedienteDAO = new ExpedienteDAO();
                        int expedientesCreados = expedienteDAO.contarPorCreador(usuario.getId());

                        StringBuilder mensaje = new StringBuilder();
                        mensaje.append("¿Eliminar el usuario?\n\n");
                        mensaje.append("👤 ").append(usuario.getUsername()).append("\n");
                        mensaje.append("    ").append(usuario.getNombreCompleto()).append("\n");
                        mensaje.append("🎭 Rol: ").append(usuario.getRol()).append("\n\n");

                        if (expedientesCreados > 0) {
                            mensaje.append("⚠️ Este usuario ha creado ").append(expedientesCreados).append(" expediente(s).\n");
                            mensaje.append("Los expedientes NO serán eliminados, pero quedarán sin creador asociado.\n\n");
                        }

                        mensaje.append("Esta acción no se puede deshacer.");

                        Alert confirmacion = new Alert(Alert.AlertType.WARNING);
                        confirmacion.setTitle("Confirmar eliminación");
                        confirmacion.setHeaderText("Eliminar usuario");
                        confirmacion.setContentText(mensaje.toString());

                        if (confirmacion.showAndWait().get() == ButtonType.OK) {
                            UsuarioService usuarioService = new UsuarioService();
                            usuarioService.eliminarUsuario(usuario.getId());
                            listaUsuarios.remove(usuario);
                            mostrarInfo("Usuario eliminado correctamente");
                        }

                    } catch (SQLException ex) {
                        mostrarError("Error al eliminar: " + ex.getMessage());
                    }
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    HBox botones = new HBox(5, btnEditar, btnEliminar);
                    setGraphic(botones);
                }
            }
        });

        tablaUsuarios.getColumns().addAll(colUsername, colNombre, colRol, colEmail, colActivo, colUltimoAcceso, colAcciones);

        // Cargar usuarios
        cargarUsuarios(listaUsuarios);

        panel.getChildren().addAll(titulo, barraControl, tablaUsuarios);
        VBox.setVgrow(tablaUsuarios, Priority.ALWAYS);

        return panel;
    }

    // ========== FORMULARIO DE USUARIO ==========
    private void abrirFormularioUsuario(Usuario usuario) {
        Stage ventana = new Stage();
        ventana.initModality(Modality.APPLICATION_MODAL);
        ventana.setTitle(usuario == null ? "Nuevo Usuario" : "Editar Usuario - " + usuario.getUsername());

        VBox form = new VBox(15);
        form.setPadding(new Insets(20));

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);

        // Username
        Label lblUsername = new Label("Usuario *:");
        TextField txtUsername = new TextField();
        txtUsername.setPromptText("Nombre de usuario único");
        if (usuario != null) {
            txtUsername.setText(usuario.getUsername());
            txtUsername.setDisable(true); // No permitir cambiar username
        }

        // Password
        Label lblPassword = new Label(usuario == null ? "Contraseña *:" : "Nueva Contraseña:");
        PasswordField txtPassword = new PasswordField();
        txtPassword.setPromptText(usuario == null ? "Contraseña" : "Dejar vacío para mantener la actual");

        // Confirmar Password
        Label lblConfirmarPassword = new Label("Confirmar Contraseña:");
        PasswordField txtConfirmarPassword = new PasswordField();

        // Nombre completo
        Label lblNombre = new Label("Nombre Completo:");
        TextField txtNombre = new TextField();
        if (usuario != null) txtNombre.setText(usuario.getNombreCompleto());

        // Email
        Label lblEmail = new Label("Email:");
        TextField txtEmail = new TextField();
        if (usuario != null) txtEmail.setText(usuario.getEmail());

        // Rol
        Label lblRol = new Label("Rol *:");
        ComboBox<RolUsuario> cmbRol = new ComboBox<>();
        cmbRol.setItems(FXCollections.observableArrayList(RolUsuario.values()));
        cmbRol.setMaxWidth(Double.MAX_VALUE);
        if (usuario != null) {
            cmbRol.setValue(usuario.getRol());
        } else {
            cmbRol.setValue(RolUsuario.SECRETARIA);
        }

        // Activo
        CheckBox chkActivo = new CheckBox("Usuario activo");
        chkActivo.setSelected(usuario == null || usuario.isActivo());

        // Layout
        int row = 0;
        grid.add(lblUsername, 0, row);
        grid.add(txtUsername, 1, row++);

        grid.add(lblPassword, 0, row);
        grid.add(txtPassword, 1, row++);

        grid.add(lblConfirmarPassword, 0, row);
        grid.add(txtConfirmarPassword, 1, row++);

        grid.add(lblNombre, 0, row);
        grid.add(txtNombre, 1, row++);

        grid.add(lblEmail, 0, row);
        grid.add(txtEmail, 1, row++);

        grid.add(lblRol, 0, row);
        grid.add(cmbRol, 1, row++);

        grid.add(chkActivo, 1, row++);

        ColumnConstraints col1 = new ColumnConstraints();
        col1.setMinWidth(150);
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
                // Validaciones
                if (usuario == null && txtUsername.getText().trim().isEmpty()) {
                    mostrarError("El nombre de usuario es obligatorio");
                    return;
                }

                if (usuario == null && txtPassword.getText().isEmpty()) {
                    mostrarError("La contraseña es obligatoria");
                    return;
                }

                if (!txtPassword.getText().isEmpty() && !txtPassword.getText().equals(txtConfirmarPassword.getText())) {
                    mostrarError("Las contraseñas no coinciden");
                    return;
                }

                if (cmbRol.getValue() == null) {
                    mostrarError("Debe seleccionar un rol");
                    return;
                }

                UsuarioService usuarioService = new UsuarioService();
                Usuario u = usuario != null ? usuario : new Usuario();

                if (usuario == null) {
                    u.setUsername(txtUsername.getText().trim());
                }

                if (!txtPassword.getText().isEmpty()) {
                    u.setPasswordHash(PasswordUtil.hash(txtPassword.getText()));
                }

                u.setNombreCompleto(txtNombre.getText().trim());
                u.setEmail(txtEmail.getText().trim());
                u.setRol(cmbRol.getValue());
                u.setActivo(chkActivo.isSelected());

                if (usuario == null) {
                    usuarioService.crearUsuario(u);
                    mostrarInfo("Usuario creado correctamente");
                } else {
                    usuarioService.actualizarUsuario(u);
                    mostrarInfo("Usuario actualizado correctamente");
                }

                cargarUsuarios();
                ventana.close();

            } catch (Exception ex) {
                mostrarError("Error: " + ex.getMessage());
            }
        });

        Button btnCancelar = new Button("❌ Cancelar");
        btnCancelar.setOnAction(e -> ventana.close());

        botones.getChildren().addAll(btnGuardar, btnCancelar);
        form.getChildren().addAll(grid, botones);

        ScrollPane scroll = new ScrollPane(form);
        scroll.setFitToWidth(true);

        Scene scene = new Scene(scroll, 500, 550);
        ventana.setScene(scene);
        ventana.showAndWait();
    }

    // ========== CARGAR USUARIOS ==========
    private void cargarUsuarios() {
        // Buscar la tabla en el panel
        TabPane tabPane = (TabPane) scene.getRoot().lookup("TabPane");
        if (tabPane != null) {
            Tab tabUsuarios = tabPane.getTabs().stream().filter(t -> t.getText().contains("Usuarios")).findFirst().orElse(null);

            if (tabUsuarios != null) {
                VBox contenido = (VBox) tabUsuarios.getContent();
                @SuppressWarnings("unchecked") TableView<Usuario> tabla = (TableView<Usuario>) contenido.lookup("TableView");
                if (tabla != null) {
                    cargarUsuarios(tabla.getItems());
                }
            }
        }
    }

    private void cargarUsuarios(ObservableList<Usuario> lista) {
        try {
            UsuarioService usuarioService = new UsuarioService();
            List<Usuario> usuarios = usuarioService.listarTodos();
            lista.clear();
            lista.addAll(usuarios);
        } catch (SQLException e) {
            mostrarError("Error al cargar usuarios: " + e.getMessage());
        }
    }



// ========== MÉTODO PARA MOSTRAR POPUP ==========
private void mostrarPopupEventosDelDia() {
    try {
        Integer usuarioId = SesionUsuario.getUsuarioActual().getId();
        LocalDate hoy = LocalDate.now();

        List<EventoAgenda> eventosHoy = agendaService.listarPorFecha(hoy).stream()
                .filter(e -> e.getUsuarioId().equals(usuarioId) && e.isPendiente())
                .toList();

        if (eventosHoy.isEmpty()) {
            return;
        }

        Stage popup = new Stage();
        popup.initModality(Modality.NONE);
        popup.setTitle("Eventos de Hoy");
        popup.setAlwaysOnTop(true);

        VBox root = new VBox();
        root.setStyle("-fx-background-color: white;");

        // ===== Header sobrio =====
        VBox header = new VBox(3);
        header.setPadding(new Insets(18, 20, 18, 20));
        header.setStyle("-fx-background-color: #0C447C;");

        Label lblTitulo = new Label("Eventos pendientes para hoy");
        lblTitulo.setStyle("-fx-font-size: 17px; -fx-font-weight: bold; -fx-text-fill: white;");

        Label lblSubtitulo = new Label(hoy.format(
                DateTimeFormatter.ofPattern("EEEE, dd 'de' MMMM 'de' yyyy", new java.util.Locale("es", "ES"))));
        lblSubtitulo.setStyle("-fx-font-size: 13px; -fx-text-fill: #B5D4F4;");

        header.getChildren().addAll(lblTitulo, lblSubtitulo);

        // ===== Lista de eventos =====
        VBox listaEventos = new VBox(10);
        listaEventos.setPadding(new Insets(16));

        DateTimeFormatter horaFormatter = DateTimeFormatter.ofPattern("HH:mm");

        for (EventoAgenda evento : eventosHoy) {
            HBox itemEvento = new HBox(14);
            itemEvento.setPadding(new Insets(14));
            itemEvento.setAlignment(Pos.CENTER_LEFT);
            itemEvento.setStyle("-fx-background-color: #F8F8F6; " +
                    "-fx-border-color: rgba(0,0,0,0.08); " +
                    "-fx-border-width: 1; " +
                    "-fx-border-radius: 8; " +
                    "-fx-background-radius: 8;");

            // Hora destacada a la izquierda
            Label lblHora = new Label(evento.getFechaHora().format(horaFormatter));
            lblHora.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #185FA5; -fx-min-width: 56px;");

            // Detalles
            VBox detallesEvento = new VBox(4);

            Label lblTituloEvento = new Label(evento.getTitulo());
            lblTituloEvento.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #1A1A18;");

            // Pill del tipo
            String nivel = switch (evento.getTipo()) {
                case AUDIENCIA -> "red";
                case VENCIMIENTO -> "amber";
                case REUNION -> "blue";
                case PRESENTACION -> "green";
                default -> "blue";
            };
            Label pillTipo = new Label(evento.getTipo().getDisplayName());
            pillTipo.getStyleClass().addAll("pill", "pill-" + nivel);

            HBox lineaTipo = new HBox(8, pillTipo);
            lineaTipo.setAlignment(Pos.CENTER_LEFT);

            detallesEvento.getChildren().addAll(lblTituloEvento, lineaTipo);

            if (evento.getUbicacion() != null && !evento.getUbicacion().isEmpty()) {
                Label lblUbicacion = new Label(evento.getUbicacion());
                lblUbicacion.getStyleClass().add("text-secondary");
                lblUbicacion.setStyle("-fx-font-size: 12px;");
                detallesEvento.getChildren().add(lblUbicacion);
            }

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            // Tiempo restante
            VBox tiempoRestante = new VBox();
            tiempoRestante.setAlignment(Pos.CENTER_RIGHT);

            LocalDateTime ahora = LocalDateTime.now();
            long minutosRestantes = java.time.Duration.between(ahora, evento.getFechaHora()).toMinutes();

            if (minutosRestantes > 0) {
                String tiempoTexto;
                if (minutosRestantes < 60) {
                    tiempoTexto = "En " + minutosRestantes + " min";
                } else {
                    long horas = minutosRestantes / 60;
                    tiempoTexto = "En " + horas + "h " + (minutosRestantes % 60) + "m";
                }
                Label lblTiempo = new Label(tiempoTexto);
                lblTiempo.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: "
                        + (minutosRestantes < 60 ? "#791F1F" : "#633806") + ";");
                tiempoRestante.getChildren().add(lblTiempo);
            } else if (minutosRestantes > -60) {
                Label lblTiempo = new Label("Ahora");
                lblTiempo.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #791F1F;");
                tiempoRestante.getChildren().add(lblTiempo);
            }

            itemEvento.getChildren().addAll(lblHora, detallesEvento, spacer, tiempoRestante);
            listaEventos.getChildren().add(itemEvento);
        }

        ScrollPane scrollEventos = new ScrollPane(listaEventos);
        scrollEventos.setFitToWidth(true);
        scrollEventos.setPrefHeight(300);
        scrollEventos.setMaxHeight(400);
        scrollEventos.getStyleClass().add("scroll-pane");

        // ===== Botones =====
        HBox botones = new HBox(10);
        botones.setAlignment(Pos.CENTER_RIGHT);
        botones.setPadding(new Insets(12, 16, 16, 16));

        Button btnVerAgenda = new Button("Ver Agenda Completa");
        btnVerAgenda.getStyleClass().add("btn-ghost");
        btnVerAgenda.setOnAction(e -> {
            irASeccion(viewAgenda, navAgenda);
            popup.close();
        });
        Button btnCerrar = new Button("Entendido");
        btnCerrar.getStyleClass().add("btn-primary");
        btnCerrar.setOnAction(e -> popup.close());

        botones.getChildren().addAll(btnVerAgenda, btnCerrar);

        root.getChildren().addAll(header, scrollEventos, botones);

        Scene scenePopup = new Scene(root, 500, Math.min(480, 200 + (eventosHoy.size() * 100)));
        scenePopup.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
        popup.setScene(scenePopup);

        popup.setX(javafx.stage.Screen.getPrimary().getVisualBounds().getMaxX() - 520);
        popup.setY(javafx.stage.Screen.getPrimary().getVisualBounds().getMaxY() - scenePopup.getHeight() - 50);

        popup.show();

        // Auto-cerrar después de 30 segundos
        new Thread(() -> {
            try {
                Thread.sleep(30000);
                javafx.application.Platform.runLater(() -> {
                    if (popup.isShowing()) {
                        popup.close();
                    }
                });
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();

    } catch (SQLException e) {
        System.err.println("Error al cargar eventos del día: " + e.getMessage());
    }
}
    // ==================== CLASE AUXILIAR PARA RECORDATORIOS ====================
    private static class RecordatorioOpcion {
        private final Integer minutos;
        private final String descripcion;

        public RecordatorioOpcion(Integer minutos, String descripcion) {
            this.minutos = minutos;
            this.descripcion = descripcion;
        }

        public Integer getMinutos() {
            return minutos;
        }

        public String getDescripcion() {
            return descripcion;
        }

        @Override
        public String toString() {
            return descripcion;
        }
    }

// ==================== SISTEMA DE PERMISOS ====================

    private boolean esAdmin() {
        return usuarioActual.getRol() == RolUsuario.ADMIN;
    }

    private boolean esAbogado() {
        return usuarioActual.getRol() == RolUsuario.ABOGADO;
    }

    private boolean esSecretario() {
        return usuarioActual.getRol() == RolUsuario.SECRETARIA;
    }

    private boolean puedeEliminar() {
        // Solo Admin y Abogado pueden eliminar
        return esAdmin() || esAbogado();
    }

    private boolean puedeEditarUsuarios() {
        // Solo Admin puede gestionar usuarios
        return esAdmin();
    }

    private boolean puedeEditarEconomia() {
        // Admin y Abogado pueden editar economía
        return esAdmin() || esAbogado();
    }

    private String formatearMoneda(double monto) {
        return String.format("$%,.0f", monto).replace(",", ".");
    }

    // Representa una notificación del dashboard
    private static class Notificacion {
        final String texto;
        final String nivel; // "header", "red", "amber", "green", "blue", "muted"

        Notificacion(String texto, String nivel) {
            this.texto = texto;
            this.nivel = nivel;
        }
    }
    // Item de la lista de próximos eventos del dashboard
    private static class ItemProximoEvento {
        final boolean esEncabezado;
        final String textoEncabezado;
        final String hora;
        final String titulo;
        final String tipo;
        final String nivelPill; // green / amber / blue / red

        // Encabezado de día
        static ItemProximoEvento header(String texto) {
            return new ItemProximoEvento(true, texto, null, null, null, null);
        }
        // Evento
        static ItemProximoEvento evento(String hora, String titulo, String tipo, String nivelPill) {
            return new ItemProximoEvento(false, null, hora, titulo, tipo, nivelPill);
        }

        private ItemProximoEvento(boolean esEncabezado, String textoEncabezado,
                                  String hora, String titulo, String tipo, String nivelPill) {
            this.esEncabezado = esEncabezado;
            this.textoEncabezado = textoEncabezado;
            this.hora = hora;
            this.titulo = titulo;
            this.tipo = tipo;
            this.nivelPill = nivelPill;
        }
    }

    // Navega a una sección desde cualquier parte del código
    private void irASeccion(VBox vista, Button navBtn) {
        if (contentArea == null || vista == null) return;
        contentArea.getChildren().setAll(vista);
        // Resaltar el item del sidebar correspondiente (si se pasó)
        if (navBtn != null) {
            navBtn.setStyle(estiloNavItem(true));
        }
    }

}

