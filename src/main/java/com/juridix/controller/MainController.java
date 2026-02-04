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
import javafx.stage.Stage;
import javafx.stage.Modality;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.ColumnConstraints;

import java.io.File;
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

    // Servicios económicos
    private HonorarioService honorarioService;
    private GastoService gastoService;
    private PagoService pagoService;

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

    // Agregar junto a los otros componentes
    private ListView<String> listNotificaciones;


    public MainController(Stage stage) {
        this.stage = stage;
        this.expedienteService = new ExpedienteService();
        this.movimientoService = new MovimientoService();
        this.agendaService = new EventoAgendaService();

        this.clienteService = new ClienteService();
        this.documentoClienteService = new DocumentoClienteService();

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
        root.setPadding(new Insets(10));

        // Top: Barra superior
        root.setTop(crearBarraSuperior());

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
        barra.setPadding(new Insets(10));
        barra.setAlignment(Pos.CENTER_LEFT);
        //barra.setStyle("-fx-background-color: #2c3e50;");

        //nuevo estilo
        barra.getStyleClass().add("barra-superior");

        Label lblTitulo = new Label("JURIDIX - Gestión de Estudios Jurídicos");
        lblTitulo.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: white;");


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
        btnCerrarSesion.getStyleClass().addAll("button", "button-danger");
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

        panel.getChildren().add(tabPane);
        VBox.setVgrow(tabPane, Priority.ALWAYS);

        // Pestaña Economía (NUEVA)
        Tab tabEconomia = new Tab("💰 Economía");
        tabEconomia.setClosable(false);
        tabEconomia.setContent(crearPanelEconomia());

        // ========== PESTAÑA USUARIOS (SOLO ADMIN) ==========
        if (SesionUsuario.getUsuarioActual().getRol() == RolUsuario.ADMIN) {
            Tab tabUsuarios = new Tab("👥 Usuarios");
            tabUsuarios.setClosable(false);
            tabUsuarios.setContent(crearPanelUsuarios());

            tabPane.getTabs().add(tabUsuarios);
        }

        tabPane.getTabs().addAll(tabDashboard, tabExpedientes, tabClientes, tabAgenda, tabEconomia);

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

        // ========== AGREGAR BOTÓN AQUÍ ==========
        Button btnMigrarDatos = new Button("🔧 Actualizar vínculos Cliente-Expediente");
        btnMigrarDatos.setStyle("-fx-background-color: #f39c12; -fx-text-fill: white; " +
                "-fx-font-weight: bold; -fx-font-size: 14px; -fx-padding: 12 25;");
        btnMigrarDatos.setOnAction(e -> migrarClienteIdEnExpedientes());

        // Centrarlo
        HBox contenedorBoton = new HBox(btnMigrarDatos);
        contenedorBoton.setAlignment(Pos.CENTER);
        contenedorBoton.setPadding(new Insets(10));

        // Panel de próximos eventos Y notificaciones
        HBox panelInferior = new HBox(15);
        panelInferior.setAlignment(Pos.TOP_CENTER);

// Panel izquierdo: Próximos eventos
        VBox panelEventos = new VBox(10);
        panelEventos.setPadding(new Insets(15));
        panelEventos.setStyle("-fx-background-color: white; -fx-border-color: #bdc3c7; -fx-border-width: 1; -fx-border-radius: 5;");
        HBox.setHgrow(panelEventos, Priority.ALWAYS);

        Label lblProximos = new Label("📅 Próximos Eventos");
        lblProximos.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

        listProximosEventos = new ListView<>();
        listProximosEventos.setPrefHeight(200);

        Button btnActualizarDashboard = new Button("🔄 Actualizar");
        btnActualizarDashboard.setOnAction(e -> cargarDashboard());

        panelEventos.getChildren().addAll(lblProximos, listProximosEventos, btnActualizarDashboard);

// Panel derecho: Notificaciones y alertas
        VBox panelNotificaciones = crearPanelNotificaciones();
        HBox.setHgrow(panelNotificaciones, Priority.ALWAYS);

        panelInferior.getChildren().addAll(panelEventos, panelNotificaciones);

        panel.getChildren().addAll(titulo, tarjetas,contenedorBoton, panelInferior);
        return panel;
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
        panel.setStyle("-fx-background-color: white; -fx-border-color: #bdc3c7; -fx-border-width: 1; -fx-border-radius: 5;");

        Label lblTitulo = new Label("🔔 Notificaciones y Alertas");
        lblTitulo.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

        listNotificaciones = new ListView<>();
        listNotificaciones.setPrefHeight(200);
        listNotificaciones.setCellFactory(lv -> new ListCell<String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);

                    // Colorear según tipo
                    if (item.contains("⚠️") || item.contains("🔴")) {
                        setStyle("-fx-background-color: #ffe6e6; -fx-font-weight: bold;");
                    } else if (item.contains("📅") || item.contains("🟡")) {
                        setStyle("-fx-background-color: #fff9e6;");
                    } else if (item.contains("✅")) {
                        setStyle("-fx-background-color: #e6ffe6;");
                    } else if (item.startsWith("   ")) {
                        setStyle("-fx-padding: 2 2 2 20;"); // Indentar items
                    }
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

    private void actualizarNotificaciones() {
        ObservableList<String> notificaciones = FXCollections.observableArrayList();

        try {
            Integer usuarioId = SesionUsuario.getUsuarioActual().getId();
            LocalDate hoy = LocalDate.now();
            LocalDate manana = hoy.plusDays(1);

            // ========== EVENTOS DE HOY ==========
            List<EventoAgenda> eventosHoy = agendaService.listarPorFecha(hoy).stream()
                    .filter(e -> e.getUsuarioId().equals(usuarioId) && e.isPendiente())
                    .toList();

            if (!eventosHoy.isEmpty()) {
                notificaciones.add("⚠️ HOY - " + eventosHoy.size() + " evento(s) pendiente(s)");
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
                for (EventoAgenda evento : eventosHoy) {
                    String icono = switch (evento.getTipo()) {
                        case AUDIENCIA -> "⚖️";
                        case VENCIMIENTO -> "⏰";
                        case REUNION -> "👥";
                        case PRESENTACION -> "📝";
                        default -> "📌";
                    };
                    notificaciones.add("   " + icono + " " + evento.getFechaHora().format(formatter) + " - " + evento.getTitulo());
                }
                notificaciones.add(""); // Separador
            }

            // ========== EVENTOS DE MAÑANA ==========
            List<EventoAgenda> eventosManana = agendaService.listarPorFecha(manana).stream()
                    .filter(e -> e.getUsuarioId().equals(usuarioId) && e.isPendiente())
                    .toList();

            if (!eventosManana.isEmpty()) {
                notificaciones.add("📅 MAÑANA - " + eventosManana.size() + " evento(s)");
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
                for (EventoAgenda evento : eventosManana) {
                    String icono = switch (evento.getTipo()) {
                        case AUDIENCIA -> "⚖️";
                        case VENCIMIENTO -> "⏰";
                        case REUNION -> "👥";
                        case PRESENTACION -> "📝";
                        default -> "📌";
                    };
                    notificaciones.add("   " + icono + " " + evento.getFechaHora().format(formatter) + " - " + evento.getTitulo());
                }
                notificaciones.add(""); // Separador
            }

            // ========== PRÓXIMOS 7 DÍAS (sin contar hoy y mañana) ==========
            List<EventoAgenda> proximaSemana = agendaService.listarProximos(usuarioId, 7).stream()
                    .filter(e -> e.isPendiente() &&
                            !e.getFechaHora().toLocalDate().equals(hoy) &&
                            !e.getFechaHora().toLocalDate().equals(manana))
                    .toList();

            if (!proximaSemana.isEmpty()) {
                notificaciones.add("📆 PRÓXIMOS 7 DÍAS - " + proximaSemana.size() + " evento(s)");
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM HH:mm");
                for (EventoAgenda evento : proximaSemana) {
                    notificaciones.add("   📌 " + evento.getFechaHora().format(formatter) + " - " + evento.getTitulo());
                }
            }

            // ========== VENCIMIENTOS PRÓXIMOS ==========
            List<EventoAgenda> vencimientos = agendaService.listarProximos(usuarioId, 7).stream()
                    .filter(e -> e.getTipo() == TipoEvento.VENCIMIENTO && e.isPendiente())
                    .toList();

            if (!vencimientos.isEmpty()) {
                notificaciones.add(""); // Separador
                notificaciones.add("⏰ VENCIMIENTOS PRÓXIMOS");
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
                for (EventoAgenda v : vencimientos) {
                    long diasRestantes = java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), v.getFechaHora().toLocalDate());
                    String urgencia = diasRestantes <= 1 ? "🔴" : diasRestantes <= 3 ? "🟡" : "🟢";
                    notificaciones.add("   " + urgencia + " " + v.getFechaHora().toLocalDate().format(formatter) + " - " + v.getTitulo());
                }
            }

            // ========== SI NO HAY NADA ==========
            if (notificaciones.isEmpty()) {
                notificaciones.add("✅ No hay notificaciones pendientes");
                notificaciones.add("");
                notificaciones.add("¡Todo al día! 🎉");
            }

        } catch (SQLException e) {
            notificaciones.add("❌ Error al cargar notificaciones");
            notificaciones.add("Detalles: " + e.getMessage());
            e.printStackTrace();
        }

        listNotificaciones.setItems(notificaciones);
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
//        form.setStyle("-fx-background-color: #ecf0f1; -fx-border-color: #bdc3c7; -fx-border-width: 1;");
        form.getStyleClass().add("panel-formulario");

        Label lblTitulo = new Label("Datos del Expediente");
        lblTitulo.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

        txtNumero = new TextField();
        txtCaratula = new TextField();

        // ============ NUEVO: ComboBox de clientes ============
        ComboBox<Cliente> cmbClientes = new ComboBox<>();
        cmbClientes.setPromptText("Seleccione un cliente...");
        cmbClientes.setMaxWidth(Double.MAX_VALUE);

        // Botón para crear cliente rápido
        Button btnNuevoClienteRapido = new Button("➕");
        btnNuevoClienteRapido.getStyleClass().addAll("button", "button-success");
        btnNuevoClienteRapido.setOnAction(e -> {
            abrirFormularioCliente(null);
            // Recargar combo después de crear cliente
            cargarComboClientes(cmbClientes);
        });

        HBox clienteBox = new HBox(5, cmbClientes, btnNuevoClienteRapido);
        HBox.setHgrow(cmbClientes, Priority.ALWAYS);

        txtCliente = new TextField();
        txtCliente.setEditable(false);
        txtCliente.setStyle("-fx-background-color: #e8e8e8;");

        // Cuando selecciona un cliente del combo
        cmbClientes.setOnAction(e -> {
            Cliente clienteSel = cmbClientes.getValue();
            if (clienteSel != null) {
                txtCliente.setText(clienteSel.getNombreCompleto());
            }
        });

        // Cargar clientes en el combo
        cargarComboClientes(cmbClientes);
        // ============ FIN NUEVO ============

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
                crearBotonesFormularioExpediente(cmbClientes)
        );

        ScrollPane scroll = new ScrollPane(form);
        scroll.setFitToWidth(true);

        VBox container = new VBox(scroll);
        VBox.setVgrow(scroll, Priority.ALWAYS);

        return container;
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

    private HBox crearBotonesFormularioExpediente(ComboBox<Cliente> cmbClientes) {
        HBox botones = new HBox(10);
        botones.setAlignment(Pos.CENTER);
        botones.setPadding(new Insets(10, 0, 0, 0));

        Button btnGuardar = new Button("💾 Guardar");
        //btnGuardar.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-font-weight: bold;");
        btnGuardar.getStyleClass().addAll("button", "button-success");
        btnGuardar.setOnAction(e -> guardarExpedienteConCliente(cmbClientes));

        Button btnNuevo = new Button("📄 Nuevo");
        btnNuevo.getStyleClass().add("button");
        btnNuevo.setOnAction(e -> limpiarFormularioExpediente());

        Button btnEliminar = new Button("🗑️ Eliminar");
        //btnEliminar.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-font-weight: bold;");
        btnEliminar.getStyleClass().addAll("button", "button-danger");
        btnEliminar.setOnAction(e -> eliminarExpediente());
//        // ========== APLICAR PERMISOS ==========
//        if (!puedeEliminar()) {
//            btnEliminar.setDisable(true);
//            btnEliminar.setTooltip(new Tooltip("No tienes permisos para eliminar expedientes"));
//        }

        Button btnMovimientos = new Button("📋 Ver Movimientos");
        //btnMovimientos.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-font-weight: bold;");
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

        Button btnExportarExp = new Button("📊 Exportar Excel");
        btnExportarExp.setStyle("-fx-background-color: #16a085; -fx-text-fill: white;");
        btnExportarExp.setOnAction(e -> exportarExpedientesExcel());

        panelBusqueda.getChildren().addAll(lblBuscar, txtBuscar, lblFiltro, cmbFiltroEstado,
                btnLimpiarFiltro, btnExportarExp);

        //panelBusqueda.getChildren().addAll(lblBuscar, txtBuscar, lblFiltro, cmbFiltroEstado, btnLimpiarFiltro);

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
        btnEliminar.getStyleClass().addAll("button", "button-danger");
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
        VBox panel = new VBox(15);
        panel.setPadding(new Insets(15));

        Label titulo = new Label("📅 Agenda y Calendario");
        titulo.setStyle("-fx-font-size: 20px; -fx-font-weight: bold;");

        // Botones superiores
        HBox botonesSuperiores = new HBox(10);
        botonesSuperiores.setAlignment(Pos.CENTER_LEFT);

        Button btnNuevoEvento = new Button("➕ Nuevo Evento");
        btnNuevoEvento.getStyleClass().addAll("button", "button-success");
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

        // CELLFACTORY PARA FORMATEAR:
        colFechaHora.setCellFactory(column -> new TableCell<EventoAgenda, LocalDateTime>() {
            @Override
            protected void updateItem(LocalDateTime item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    // Formato: 26-Ene 14:30
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MMM HH:mm",
                            new java.util.Locale("es", "ES"));
                    setText(item.format(formatter));
                }
            }
        });

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
        colAcciones.setPrefWidth(480);
        colAcciones.setCellFactory(param -> new TableCell<>() {
            private final Button btnEditar = new Button("✏️ Editar");
            private final Button btnCompletar = new Button("✅ Completar");
            private final Button btnEliminar = new Button("🗑️ Eliminar");

            {

                btnEditar.setMinWidth(85);
                btnCompletar.setMinWidth(110);
                btnEliminar.setMinWidth(85);

                btnEditar.setOnAction(e -> {
                    EventoAgenda evento = getTableView().getItems().get(getIndex());
                    abrirFormularioEvento(evento);
                });
                //nuevo estilo botones
                btnCompletar.getStyleClass().addAll("button", "button-success");
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

                btnEliminar.getStyleClass().addAll("button", "button-danger");
                btnEliminar.setOnAction(e -> {
                    EventoAgenda evento = getTableView().getItems().get(getIndex());

                    StringBuilder mensaje = new StringBuilder();
                    mensaje.append("¿Eliminar el evento?\n\n");
                    mensaje.append("📅 ").append(evento.getTitulo()).append("\n");
                    mensaje.append("⏰ ").append(evento.getFechaHora().format(
                            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))).append("\n");
                    mensaje.append("📍 Tipo: ").append(evento.getTipo().getDisplayName()).append("\n");

                    if (evento.getUbicacion() != null && !evento.getUbicacion().isEmpty()) {
                        mensaje.append("📍 Ubicación: ").append(evento.getUbicacion()).append("\n");
                    }

                    if (evento.getExpedienteId() != null) {
                        mensaje.append("\n⚠️ Este evento está asociado a un expediente");
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
                            actualizarNotificaciones();
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
        actualizarNotificaciones();
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

        ComboBox<RecordatorioOpcion> cmbRecordatorio = new ComboBox<>();
        cmbRecordatorio.setItems(FXCollections.observableArrayList(
                new RecordatorioOpcion(15, "15 minutos antes"),
                new RecordatorioOpcion(30, "30 minutos antes"),
                new RecordatorioOpcion(60, "1 hora antes"),
                new RecordatorioOpcion(120, "2 horas antes"),
                new RecordatorioOpcion(1440, "1 día antes"),
                new RecordatorioOpcion(2880, "2 días antes")
        ));
        cmbRecordatorio.setPromptText("Seleccione recordatorio");
        cmbRecordatorio.setMaxWidth(Double.MAX_VALUE);

        if (evento != null) {
            // Buscar y seleccionar el recordatorio actual
            Integer minutosEvento = evento.getRecordatorioMinutos();
            cmbRecordatorio.getItems().stream()
                    .filter(r -> r.getMinutos().equals(minutosEvento))
                    .findFirst()
                    .ifPresent(cmbRecordatorio::setValue);
        } else {
            cmbRecordatorio.setValue(cmbRecordatorio.getItems().get(4)); // 1 día por defecto
        }

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
            cmbRecordatorio.setValue(cmbRecordatorio.getValue());
        } else {
            cmbRecordatorio.setValue(cmbRecordatorio.getValue());
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
        btnGuardar.getStyleClass().addAll("button", "button-success");
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
                RecordatorioOpcion recordatorio = cmbRecordatorio.getValue();
                if (recordatorio != null) {
                    ev.setRecordatorioMinutos(recordatorio.getMinutos());
                } else {
                    ev.setRecordatorioMinutos(1440); // 1 día por defecto
                }

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
        //barra.setStyle("-fx-background-color: #ecf0f1;");
        barra.getStyleClass().add("status-bar");

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
        btnNuevoCliente.getStyleClass().addAll("button", "button-success");
        btnNuevoCliente.setOnAction(e -> abrirFormularioCliente(null));

        Button btnActualizar = new Button("🔄 Actualizar");
        btnActualizar.setOnAction(e -> cargarClientes());

        Button btnExportar = new Button("📊 Exportar Excel");
        btnExportar.setStyle("-fx-background-color: #16a085; -fx-text-fill: white;");
        btnExportar.setOnAction(e -> exportarClientesExcel());

// Agregar a barraControl
        barraControl.getChildren().addAll(lblBuscar, txtBuscarCliente, btnNuevoCliente,
                btnActualizar, btnExportar);

        //barraControl.getChildren().addAll(lblBuscar, txtBuscarCliente, btnNuevoCliente, btnActualizar);

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
        btnGuardar.getStyleClass().addAll("button", "button-success");
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
        //header.setStyle("-fx-background-color: #3498db; -fx-background-radius: 5;");
        header.setStyle("-fx-background-color: linear-gradient(to right, #3498db, #2980b9); " +
                "-fx-background-radius: 5;");

        // Grid con información visible
        GridPane infoGrid = new GridPane();
        infoGrid.setHgap(30);
        infoGrid.setVgap(5);

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
        Button btnEliminarCliente = new Button("🗑️ Eliminar Cliente");
        btnEliminarCliente.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-font-weight: bold;");
        btnEliminarCliente.setOnAction(a -> {
            try {
                // Verificar expedientes y documentos ANTES de confirmar
                ExpedienteDAO expedienteDAO = new ExpedienteDAO();
                List<Expediente> expedientes = expedienteDAO.listarPorClienteId(cliente.getId());

                int cantDocumentos = documentoClienteService.listarPorCliente(cliente.getId()).size();

                // Construir mensaje de confirmación con detalles
                StringBuilder mensaje = new StringBuilder();
                mensaje.append("¿Está seguro de eliminar el cliente: ").append(cliente.getNombreCompleto()).append("?\n\n");

                boolean tieneAsociaciones = false;

                if (!expedientes.isEmpty()) {
                    mensaje.append("⚠️ ATENCIÓN: Este cliente tiene ").append(expedientes.size())
                            .append(" expediente(s) asociado(s):\n");
                    for (Expediente exp : expedientes) {
                        mensaje.append("   • ").append(exp.getNumero()).append(" - ").append(exp.getCaratula()).append("\n");
                    }
                    mensaje.append("\n");
                    tieneAsociaciones = true;
                }

                if (cantDocumentos > 0) {
                    mensaje.append("📎 Este cliente tiene ").append(cantDocumentos).append(" documento(s) cargado(s).\n\n");
                    tieneAsociaciones = true;
                }

                if (tieneAsociaciones) {
                    mensaje.append("❌ NO SE PUEDE ELIMINAR.\n");
                    mensaje.append("Primero debe eliminar o reasignar los expedientes y documentos asociados.");

                    Alert alerta = new Alert(Alert.AlertType.ERROR);
                    alerta.setTitle("No se puede eliminar");
                    alerta.setHeaderText("Cliente con datos asociados");
                    alerta.setContentText(mensaje.toString());
                    alerta.showAndWait();
                    return;
                }

                // Si no tiene asociaciones, pedir confirmación normal
                mensaje.append("Esta acción no se puede deshacer.");

                Alert confirmacion = new Alert(Alert.AlertType.CONFIRMATION);
                confirmacion.setTitle("Confirmar eliminación");
                confirmacion.setHeaderText("Eliminar cliente");
                confirmacion.setContentText(mensaje.toString());

                if (confirmacion.showAndWait().get() == ButtonType.OK) {
                    clienteService.eliminarCliente(cliente.getId());
                    mostrarInfo("Cliente eliminado correctamente");
                    ventana.close();
                    cargarClientes();
                }

            } catch (IllegalStateException ex) {
                // Error de validación de negocio
                Alert alerta = new Alert(Alert.AlertType.ERROR);
                alerta.setTitle("No se puede eliminar");
                alerta.setHeaderText("Validación fallida");
                alerta.setContentText(ex.getMessage());
                alerta.showAndWait();
            } catch (SQLException ex) {
                mostrarError("Error al eliminar cliente: " + ex.getMessage());
            }

        });


        Button btnEditar = new Button("✏️ Editar Cliente");
        btnEditar.setStyle("-fx-background-color: white; -fx-text-fill: #3498db; -fx-font-weight: bold;");
        btnEditar.setOnAction(e -> {
            abrirFormularioCliente(cliente);

            ventana.close();
        });

        header.getChildren().addAll(lblNombre, infoDatos, btnEditar, btnEliminarCliente);
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

        VBox panelExpedientes = crearPanelExpedientesCliente(cliente,ventana);
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
    private VBox crearPanelExpedientesCliente(Cliente cliente, Stage ventanaCliente) {
        VBox panel = new VBox(10);
        panel.setPadding(new Insets(15));

        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);

        Label titulo = new Label("📁 Expedientes del Cliente");
        titulo.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

        Button btnNuevoExpediente = new Button("➕ Nuevo Expediente");
        btnNuevoExpediente.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-font-weight: bold;");
        btnNuevoExpediente.setOnAction(e -> {
            // Cerrar la ventana del cliente
            ventanaCliente.close();

            // Cambiar a la pestaña de expedientes
            TabPane tabPane = (TabPane) scene.getRoot().lookup("TabPane");
            if (tabPane != null) {
                Tab tabExpedientes = tabPane.getTabs().stream()
                        .filter(t -> t.getText().contains("Expedientes"))
                        .findFirst()
                        .orElse(null);
                if (tabExpedientes != null) {
                    tabPane.getSelectionModel().select(tabExpedientes);
                }
            }

            // Pre-cargar el cliente
            limpiarFormularioExpediente();
            txtCliente.setText(cliente.getNombreCompleto());
            mostrarInfo("Complete los datos del nuevo expediente para: " + cliente.getNombreCompleto());
        });

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        header.getChildren().addAll(titulo, spacer, btnNuevoExpediente);

        // ========== TABLA DE EXPEDIENTES ==========
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

        // ========== CARGAR EXPEDIENTES DEL CLIENTE (AQUÍ ES DONDE DEBE ESTAR) ==========
        try {
            System.out.println("========================================");
            System.out.println("🔍 DEBUG: Cargando expedientes del cliente");
            System.out.println("Cliente ID: " + cliente.getId());
            System.out.println("Cliente Nombre: " + cliente.getNombreCompleto());

            ExpedienteDAO expedienteDAO = new ExpedienteDAO();
            List<Expediente> expedientes = expedienteDAO.listarPorClienteId(cliente.getId());

            System.out.println("📊 Cantidad de expedientes encontrados: " + expedientes.size());

            if (expedientes.isEmpty()) {
                System.out.println("⚠️ LISTA VACÍA - Verificando en BD...");

                // Verificar directamente en la BD
                try (Connection conn = Database.getConnection();
                     PreparedStatement ps = conn.prepareStatement(
                             "SELECT id, numero, caratula, cliente_id FROM expedientes WHERE cliente_id = ?")) {
                    ps.setInt(1, cliente.getId());
                    ResultSet rs = ps.executeQuery();

                    System.out.println("🔍 Consulta directa a BD:");
                    while (rs.next()) {
                        System.out.println("  - ID: " + rs.getInt("id") +
                                " | Número: " + rs.getString("numero") +
                                " | cliente_id: " + rs.getInt("cliente_id"));
                    }
                }
            } else {
                System.out.println("✅ Expedientes encontrados:");
                for (Expediente exp : expedientes) {
                    System.out.println("  - " + exp.getNumero() + " | " + exp.getCaratula() +
                            " | cliente_id: " + exp.getClienteId());
                }
            }

            listaExp.clear();
            listaExp.addAll(expedientes);
            System.out.println("📋 Items en ObservableList: " + listaExp.size());
            System.out.println("========================================");

        } catch (SQLException e) {
            System.err.println("❌ ERROR: " + e.getMessage());
            e.printStackTrace();
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
                //boton con nuevo estilo
                btnAbrir.getStyleClass().addAll("button", "button-info");
                btnAbrir.setOnAction(e -> {
                    DocumentoCliente doc = getTableView().getItems().get(getIndex());
                    try {
                        documentoClienteService.abrirDocumento(doc.getId());
                    } catch (Exception ex) {
                        mostrarError("Error al abrir documento: " + ex.getMessage());
                    }
                });

                btnEliminar.getStyleClass().addAll("button", "button-danger");
                btnEliminar.setOnAction(e -> {
                    DocumentoCliente doc = getTableView().getItems().get(getIndex());

                    StringBuilder mensaje = new StringBuilder();
                    mensaje.append("¿Eliminar el documento?\n\n");
                    mensaje.append("📄 ").append(doc.getNombreOriginal()).append("\n");
                    mensaje.append("📦 Tamaño: ").append(doc.getTamanioFormateado()).append("\n");
                    mensaje.append("📅 Subido: ").append(doc.getFechaSubida().format(
                            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))).append("\n\n");
                    mensaje.append("⚠️ El archivo será eliminado del sistema de archivos.\n");
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
        txtResultadoPlazo.setStyle("-fx-background-color: #ecf0f1; -fx-font-weight: bold; -fx-font-size: 14px;");

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

        Label titulo = new Label("💰 Gestión Económica");
        titulo.setStyle("-fx-font-size: 20px; -fx-font-weight: bold;");

        // ========== RESUMEN FINANCIERO ==========
        HBox resumenFinanciero = new HBox(20);
        resumenFinanciero.setAlignment(Pos.CENTER);

        try {
            // Calcular totales globales
            double totalHonorariosPendientes = calcularTotalHonorariosPendientes();
            double totalGastos = calcularTotalGastos();
            double totalPagos = calcularTotalPagos();
            double saldoPendiente = totalHonorariosPendientes - totalPagos;

            VBox tarjetaHonorarios = crearTarjetaFinanciera("Honorarios Pendientes",
                    String.format("$%.2f", totalHonorariosPendientes), "#3498db");

            VBox tarjetaGastos = crearTarjetaFinanciera("Total Gastos",
                    String.format("$%.2f", totalGastos), "#e74c3c");

            VBox tarjetaPagos = crearTarjetaFinanciera("Pagos Recibidos",
                    String.format("$%.2f", totalPagos), "#27ae60");

            VBox tarjetaSaldo = crearTarjetaFinanciera("Saldo Pendiente",
                    String.format("$%.2f", saldoPendiente), "#f39c12");

            resumenFinanciero.getChildren().addAll(tarjetaHonorarios, tarjetaGastos, tarjetaPagos, tarjetaSaldo);

        } catch (SQLException e) {
            Label lblError = new Label("Error al cargar resumen financiero: " + e.getMessage());
            lblError.setStyle("-fx-text-fill: red;");
            resumenFinanciero.getChildren().add(lblError);
        }

        // ========== SELECTOR DE EXPEDIENTE ==========
        HBox selectorExpediente = new HBox(10);
        selectorExpediente.setAlignment(Pos.CENTER_LEFT);

        Label lblSeleccionar = new Label("Seleccione un expediente:");
        lblSeleccionar.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

        ComboBox<Expediente> cmbExpedientes = new ComboBox<>();
        cmbExpedientes.setPromptText("Seleccione expediente...");
        cmbExpedientes.setPrefWidth(400);

        try {
            List<Expediente> expedientes = expedienteService.listarActivos();
            cmbExpedientes.setItems(FXCollections.observableArrayList(expedientes));
        } catch (SQLException e) {
            mostrarError("Error al cargar expedientes: " + e.getMessage());
        }

        selectorExpediente.getChildren().addAll(lblSeleccionar, cmbExpedientes);

        // ========== PESTAÑAS DE GESTIÓN ==========
        TabPane tabPaneEconomia = new TabPane();

        // Tab Honorarios
        Tab tabHonorarios = new Tab("💵 Honorarios");
        tabHonorarios.setClosable(false);
        VBox panelHonorarios = crearPanelHonorarios(cmbExpedientes);
        tabHonorarios.setContent(panelHonorarios);

        // Tab Gastos
        Tab tabGastos = new Tab("💸 Gastos");
        tabGastos.setClosable(false);
        VBox panelGastos = crearPanelGastos(cmbExpedientes);
        tabGastos.setContent(panelGastos);

        // Tab Pagos
        Tab tabPagos = new Tab("💳 Pagos");
        tabPagos.setClosable(false);
        VBox panelPagos = crearPanelPagos(cmbExpedientes);
        tabPagos.setContent(panelPagos);

        // Tab Cuenta Corriente
        Tab tabCuentaCorriente = new Tab("📊 Cuenta Corriente");
        tabCuentaCorriente.setClosable(false);
        VBox panelCuentaCorriente = crearPanelCuentaCorriente(cmbExpedientes);
        tabCuentaCorriente.setContent(panelCuentaCorriente);

        tabPaneEconomia.getTabs().addAll(tabHonorarios, tabGastos, tabPagos, tabCuentaCorriente);

        panel.getChildren().addAll(titulo, resumenFinanciero, new Separator(), selectorExpediente, tabPaneEconomia);
        VBox.setVgrow(tabPaneEconomia, Priority.ALWAYS);

        return panel;
    }

    // Tarjeta financiera
    private VBox crearTarjetaFinanciera(String titulo, String valor, String color) {
        VBox tarjeta = new VBox(10);
        tarjeta.setAlignment(Pos.CENTER);
        tarjeta.setPadding(new Insets(20));
        tarjeta.setStyle("-fx-background-color: white; -fx-border-color: " + color +
                "; -fx-border-width: 3; -fx-border-radius: 10; -fx-background-radius: 10;");
        tarjeta.setPrefSize(220, 120);

        Label lblTitulo = new Label(titulo);
        lblTitulo.setStyle("-fx-font-size: 14px; -fx-text-fill: #7f8c8d;");

        Label lblValor = new Label(valor);
        lblValor.setStyle("-fx-font-size: 28px; -fx-font-weight: bold; -fx-text-fill: " + color + ";");

        tarjeta.getChildren().addAll(lblTitulo, lblValor);
        return tarjeta;
    }

    private VBox crearPanelHonorarios(ComboBox<Expediente> cmbExpedientes) {
        VBox panel = new VBox(15);
        panel.setPadding(new Insets(15));

        // Botón nuevo honorario
        Button btnNuevo = new Button("➕ Nuevo Honorario");
        btnNuevo.getStyleClass().addAll("button", "button-success");

        btnNuevo.setOnAction(e -> {
            Expediente exp = cmbExpedientes.getValue();
            if (exp != null) {
                abrirFormularioHonorario(null, exp.getId());
            } else {
                mostrarAdvertencia("Seleccione un expediente primero");
            }
        });

        // Tabla de honorarios
        TableView<Honorario> tablaHonorarios = new TableView<>();
        ObservableList<Honorario> listaHonorarios = FXCollections.observableArrayList();
        tablaHonorarios.setItems(listaHonorarios);

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
        colFechaEstimada.setPrefWidth(150);

        TableColumn<Honorario, String> colDescripcion = new TableColumn<>("Descripción");
        colDescripcion.setCellValueFactory(new PropertyValueFactory<>("descripcion"));
        colDescripcion.setPrefWidth(200);

        // Columna acciones
        TableColumn<Honorario, Void> colAcciones = new TableColumn<>("Acciones");
        colAcciones.setPrefWidth(180);
        colAcciones.setCellFactory(param -> new TableCell<>() {
            private final Button btnEditar = new Button("✏️");
            private final Button btnCobrar = new Button("💰 Cobrar");
            private final Button btnEliminar = new Button("🗑️");

            {
                btnEditar.setOnAction(e -> {
                    Honorario h = getTableView().getItems().get(getIndex());
                    abrirFormularioHonorario(h, h.getExpedienteId());
                });

                btnCobrar.getStyleClass().addAll("button", "button-success");
                btnCobrar.setOnAction(e -> {
                    Honorario h = getTableView().getItems().get(getIndex());
                    try {
                        honorarioService.marcarComoCobrado(h.getId());
                        mostrarInfo("Honorario marcado como cobrado");
                        cargarHonorariosPorExpediente(cmbExpedientes.getValue().getId(), listaHonorarios);
                    } catch (SQLException ex) {
                        mostrarError("Error: " + ex.getMessage());
                    }
                });

                btnEliminar.getStyleClass().addAll("button", "button-danger");
                btnEliminar.setOnAction(e -> {
                    Honorario h = getTableView().getItems().get(getIndex());
                    if (mostrarConfirmacion("¿Eliminar este honorario?")) {
                        try {
                            honorarioService.eliminarHonorario(h.getId());
                            listaHonorarios.remove(h);
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

        tablaHonorarios.getColumns().addAll(colTipo, colMonto, colEstado, colFechaEstimada, colDescripcion, colAcciones);

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

    private void abrirFormularioHonorario(Honorario honorario, Integer expedienteId) {

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
        cmbTipo.setItems(FXCollections.observableArrayList(TipoHonorario.values()));
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
                new Label("Tipo de Honorario *:"), cmbTipo,
                new Label("Porcentaje (%) - Solo si es porcentaje:"), txtPorcentaje,
                new Label("Monto Fijo ($) - Solo si es monto fijo:"), txtMontoFijo,
                new Label("Monto Calculado ($):"), txtMontoCalculado,
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
                h.setTipo(cmbTipo.getValue());

                if (!txtPorcentaje.getText().isEmpty()) {
                    h.setPorcentaje(Double.parseDouble(txtPorcentaje.getText()));
                }

                if (!txtMontoFijo.getText().isEmpty()) {
                    h.setMontoFijo(Double.parseDouble(txtMontoFijo.getText()));
                }

                if (!txtMontoCalculado.getText().isEmpty()) {
                    h.setMontoCalculado(Double.parseDouble(txtMontoCalculado.getText()));
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

        Scene scene = new Scene(new ScrollPane(form), 500, 600);
        ventana.setScene(scene);
        ventana.showAndWait();
    }

    private VBox crearPanelGastos(ComboBox<Expediente> cmbExpedientes) {
        VBox panel = new VBox(15);
        panel.setPadding(new Insets(15));

        Button btnNuevo = new Button("➕ Nuevo Gasto");
        btnNuevo.getStyleClass().addAll("button", "button-danger");
        btnNuevo.setOnAction(e -> {
            Expediente exp = cmbExpedientes.getValue();
            if (exp != null) {
                abrirFormularioGasto(null, exp.getId());
            } else {
                mostrarAdvertencia("Seleccione un expediente primero");
            }
        });

        TableView<Gasto> tablaGastos = new TableView<>();
        ObservableList<Gasto> listaGastos = FXCollections.observableArrayList();
        tablaGastos.setItems(listaGastos);

        TableColumn<Gasto, LocalDate> colFecha = new TableColumn<>("Fecha");
        colFecha.setCellValueFactory(new PropertyValueFactory<>("fecha"));
        colFecha.setPrefWidth(120);

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
        colAcciones.setPrefWidth(120);
        colAcciones.setCellFactory(param -> new TableCell<>() {
            private final Button btnEditar = new Button("✏️");
            private final Button btnEliminar = new Button("🗑️");

            {
                btnEditar.setOnAction(e -> {
                    Gasto g = getTableView().getItems().get(getIndex());
                    abrirFormularioGasto(g, g.getExpedienteId());
                });

                btnEliminar.getStyleClass().addAll("button", "button-danger");
                btnEliminar.setOnAction(e -> {
                    Gasto g = getTableView().getItems().get(getIndex());
                    if (mostrarConfirmacion("¿Eliminar este gasto?")) {
                        try {
                            gastoService.eliminarGasto(g.getId());
                            listaGastos.remove(g);
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

        cmbExpedientes.setOnAction(e -> {
            Expediente exp = cmbExpedientes.getValue();
            if (exp != null) {
                cargarGastosPorExpediente(exp.getId(), listaGastos);
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

    private void abrirFormularioGasto(Gasto gasto, Integer expedienteId) {
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
        panel.setPadding(new Insets(20));

        // Título
        Label titulo = new Label("Registrar Pago");
        titulo.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        // Formulario de pago
        GridPane formPago = new GridPane();
        formPago.setHgap(10);
        formPago.setVgap(10);
        formPago.setPadding(new Insets(10));

        // Campo: Monto
        Label lblMonto = new Label("Monto:");
        TextField txtMonto = new TextField();
        txtMonto.setPromptText("Ingrese el monto");
        txtMonto.setPrefWidth(200);

        // Campo: Fecha
        Label lblFecha = new Label("Fecha:");
        DatePicker dpFecha = new DatePicker(LocalDate.now());
        dpFecha.setPrefWidth(200);

        // Campo: Forma de Pago
        Label lblFormaPago = new Label("Forma de Pago:");
        ComboBox<String> cmbFormaPago = new ComboBox<>();
        cmbFormaPago.getItems().addAll("Efectivo", "Transferencia", "Cheque", "Tarjeta", "Otro");
        cmbFormaPago.setValue("Efectivo");
        cmbFormaPago.setPrefWidth(200);

        // Campo: Referencia
        Label lblReferencia = new Label("Referencia:");
        TextField txtReferencia = new TextField();
        txtReferencia.setPromptText("N° de comprobante, transferencia, etc.");
        txtReferencia.setPrefWidth(200);

        // Campo: Concepto
        Label lblConcepto = new Label("Concepto:");
        TextField txtConcepto = new TextField();
        txtConcepto.setPromptText("Descripción del pago");
        txtConcepto.setPrefWidth(400);

        // Campo: Observaciones
        Label lblObservaciones = new Label("Observaciones:");
        TextArea txtObservaciones = new TextArea();
        txtObservaciones.setPromptText("Observaciones adicionales");
        txtObservaciones.setPrefRowCount(3);
        txtObservaciones.setPrefWidth(400);

        // Agregar al formulario
        formPago.add(lblMonto, 0, 0);
        formPago.add(txtMonto, 1, 0);
        formPago.add(lblFecha, 0, 1);
        formPago.add(dpFecha, 1, 1);
        formPago.add(lblFormaPago, 0, 2);
        formPago.add(cmbFormaPago, 1, 2);
        formPago.add(lblReferencia, 0, 3);
        formPago.add(txtReferencia, 1, 3);
        formPago.add(lblConcepto, 0, 4);
        formPago.add(txtConcepto, 1, 4);
        formPago.add(lblObservaciones, 0, 5);
        formPago.add(txtObservaciones, 1, 5);
// Tabla de pagos del expediente (DEBE IR ANTES del botón)
        TableView<Pago> tablaPagos = new TableView<>();
        tablaPagos.setPrefHeight(300);

        TableColumn<Pago, LocalDate> colFecha = new TableColumn<>("Fecha");
        colFecha.setCellValueFactory(data -> new SimpleObjectProperty<>(data.getValue().getFecha()));
        colFecha.setPrefWidth(100);

        TableColumn<Pago, String> colMonto = new TableColumn<>("Monto");
        colMonto.setCellValueFactory(data ->
                new SimpleStringProperty(String.format("$%.2f", data.getValue().getMonto())));
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

        tablaPagos.getColumns().addAll(colFecha, colMonto, colFormaPago, colReferencia, colConcepto);

        // Botón Registrar Pago
        Button btnRegistrar = new Button("💰 Registrar Pago");
        btnRegistrar.getStyleClass().addAll("button", "button-success");


        btnRegistrar.setOnAction(e -> {
            try {
                Expediente expediente = cmbExpedientes.getValue();
                if (expediente == null) {
                    mostrarError("Debe seleccionar un expediente");
                    return;
                }

                String montoStr = txtMonto.getText().trim();
                if (montoStr.isEmpty()) {
                    mostrarError("Debe ingresar el monto");
                    return;
                }

                double monto = Double.parseDouble(montoStr);
                if (monto <= 0) {
                    mostrarError("El monto debe ser mayor a cero");
                    return;
                }

                Pago pago = new Pago();
                pago.setExpedienteId(expediente.getId());
                pago.setClienteId(expediente.getClienteId());
                pago.setMonto(monto);
                pago.setFecha(dpFecha.getValue());
                pago.setFormaPago(cmbFormaPago.getValue());
                pago.setReferencia(txtReferencia.getText().trim());
                pago.setConcepto(txtConcepto.getText().trim());
                pago.setObservaciones(txtObservaciones.getText().trim());
                pago.setUsuarioId(SesionUsuario.getUsuarioActual().getId());


                pagoService.crearPago(pago);

                mostrarExito("Pago registrado exitosamente");

                // Limpiar formulario
                txtMonto.clear();
                dpFecha.setValue(LocalDate.now());
                cmbFormaPago.setValue("Efectivo");
                txtReferencia.clear();
                txtConcepto.clear();
                txtObservaciones.clear();

                // Recargar tabla de pagos
                cargarTablaPagos(tablaPagos, expediente.getId());

            } catch (NumberFormatException ex) {
                mostrarError("El monto debe ser un número válido");
            } catch (SQLException ex) {
                mostrarError("Error al registrar pago: " + ex.getMessage());
            }
        });


        // Listener para cargar pagos cuando se selecciona expediente
        cmbExpedientes.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                cargarTablaPagos(tablaPagos, newVal.getId());
            }
        });

        Label lblHistorial = new Label("Historial de Pagos");
        lblHistorial.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");

        panel.getChildren().addAll(titulo, formPago, btnRegistrar, new Separator(),
                lblHistorial, tablaPagos);

        ScrollPane scroll = new ScrollPane(panel);
        scroll.setFitToWidth(true);
        scroll.setFitToHeight(true);

        VBox container = new VBox(scroll);
        VBox.setVgrow(scroll, Priority.ALWAYS);
        return container;
        //return panel;
        //POSIBLE ERROR!!!!
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
            return new SimpleStringProperty(debe > 0 ? String.format("$%.2f", debe) : "-");
        });
        colDebe.setPrefWidth(100);
        colDebe.setStyle("-fx-alignment: CENTER-RIGHT;");

        TableColumn<MovimientoCuenta, String> colHaber = new TableColumn<>("Haber");
        colHaber.setCellValueFactory(data -> {
            double haber = data.getValue().haber;
            return new SimpleStringProperty(haber > 0 ? String.format("$%.2f", haber) : "-");
        });
        colHaber.setPrefWidth(100);
        colHaber.setStyle("-fx-alignment: CENTER-RIGHT;");

        TableColumn<MovimientoCuenta, String> colSaldo = new TableColumn<>("Saldo");
        colSaldo.setCellValueFactory(data ->
                new SimpleStringProperty(String.format("$%.2f", data.getValue().saldo)));
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
            actualizarTarjeta(cardHonorarios, String.format("$%.2f", totalHonorarios));
            actualizarTarjeta(cardGastos, String.format("$%.2f", totalGastos));
            actualizarTarjeta(cardPagos, String.format("$%.2f", totalPagos));
            actualizarTarjeta(cardSaldo, String.format("$%.2f", saldo));

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

            // Obtener eventos de HOY
            List<EventoAgenda> eventosHoy = agendaService.listarPorFecha(hoy).stream()
                    .filter(e -> e.getUsuarioId().equals(usuarioId) && e.isPendiente())
                    .toList();

            // Si no hay eventos, no mostrar nada
            if (eventosHoy.isEmpty()) {
                return;
            }

            // Crear ventana de popup
            Stage popup = new Stage();
            popup.initModality(Modality.NONE); // No modal para no bloquear
            popup.setTitle("🔔 Eventos de Hoy");
            popup.setAlwaysOnTop(true); // Mantener al frente

            VBox root = new VBox(15);
            root.setPadding(new Insets(20));
            root.setStyle("-fx-background-color: white;");

            // Header
            HBox header = new HBox(10);
            header.setAlignment(Pos.CENTER_LEFT);
            header.setPadding(new Insets(15));
            header.setStyle("-fx-background-color: linear-gradient(to right, #e74c3c, #c0392b); " +
                    "-fx-background-radius: 10 10 0 0;");

            Label lblIcono = new Label("⚠️");
            lblIcono.setStyle("-fx-font-size: 32px;");

            VBox textoHeader = new VBox(3);
            Label lblTitulo = new Label("¡Tienes eventos pendientes HOY!");
            lblTitulo.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: white;");

            Label lblSubtitulo = new Label(LocalDate.now().format(
                    DateTimeFormatter.ofPattern("EEEE, dd 'de' MMMM 'de' yyyy", new java.util.Locale("es", "ES"))));
            lblSubtitulo.setStyle("-fx-font-size: 13px; -fx-text-fill: white;");

            textoHeader.getChildren().addAll(lblTitulo, lblSubtitulo);
            header.getChildren().addAll(lblIcono, textoHeader);

            // Lista de eventos
            VBox listaEventos = new VBox(10);
            listaEventos.setPadding(new Insets(10));

            DateTimeFormatter horaFormatter = DateTimeFormatter.ofPattern("HH:mm");

            for (EventoAgenda evento : eventosHoy) {
                HBox itemEvento = new HBox(15);
                itemEvento.setPadding(new Insets(15));
                itemEvento.setAlignment(Pos.CENTER_LEFT);
                itemEvento.setStyle("-fx-background-color: #f8f9fa; " +
                        "-fx-border-color: #dee2e6; " +
                        "-fx-border-width: 1; " +
                        "-fx-border-radius: 8; " +
                        "-fx-background-radius: 8;");

                // Icono según tipo
                String icono = switch (evento.getTipo()) {
                    case AUDIENCIA -> "⚖️";
                    case VENCIMIENTO -> "⏰";
                    case REUNION -> "👥";
                    case PRESENTACION -> "📝";
                    default -> "📌";
                };

                Label lblIconoEvento = new Label(icono);
                lblIconoEvento.setStyle("-fx-font-size: 28px;");

                VBox detallesEvento = new VBox(5);

                Label lblHora = new Label(evento.getFechaHora().format(horaFormatter));
                lblHora.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #e74c3c;");

                Label lblTituloEvento = new Label(evento.getTitulo());
                lblTituloEvento.setStyle("-fx-font-size: 15px; -fx-font-weight: bold;");

                Label lblTipo = new Label(evento.getTipo().getDisplayName());
                lblTipo.setStyle("-fx-font-size: 12px; -fx-text-fill: #6c757d;");

                if (evento.getUbicacion() != null && !evento.getUbicacion().isEmpty()) {
                    Label lblUbicacion = new Label("📍 " + evento.getUbicacion());
                    lblUbicacion.setStyle("-fx-font-size: 12px; -fx-text-fill: #6c757d;");
                    detallesEvento.getChildren().add(lblUbicacion);
                }

                detallesEvento.getChildren().addAll(lblHora, lblTituloEvento, lblTipo);

                // Calcular tiempo restante
                LocalDateTime ahora = LocalDateTime.now();
                long minutosRestantes = java.time.Duration.between(ahora, evento.getFechaHora()).toMinutes();

                VBox tiempoRestante = new VBox(5);
                tiempoRestante.setAlignment(Pos.CENTER);

                if (minutosRestantes > 0) {
                    String tiempoTexto;
                    if (minutosRestantes < 60) {
                        tiempoTexto = "En " + minutosRestantes + " min";
                    } else {
                        long horas = minutosRestantes / 60;
                        tiempoTexto = "En " + horas + "h " + (minutosRestantes % 60) + "m";
                    }

                    Label lblTiempo = new Label(tiempoTexto);
                    lblTiempo.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; " +
                            "-fx-text-fill: " + (minutosRestantes < 60 ? "#e74c3c" : "#f39c12") + ";");
                    tiempoRestante.getChildren().add(lblTiempo);
                } else if (minutosRestantes > -60) {
                    Label lblTiempo = new Label("¡AHORA!");
                    lblTiempo.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #e74c3c;");
                    tiempoRestante.getChildren().add(lblTiempo);
                }

                Region spacer = new Region();
                HBox.setHgrow(spacer, Priority.ALWAYS);

                itemEvento.getChildren().addAll(lblIconoEvento, detallesEvento, spacer, tiempoRestante);
                listaEventos.getChildren().add(itemEvento);
            }

            ScrollPane scrollEventos = new ScrollPane(listaEventos);
            scrollEventos.setFitToWidth(true);
            scrollEventos.setPrefHeight(300);
            scrollEventos.setMaxHeight(400);
            scrollEventos.setStyle("-fx-background-color: transparent;");

            // Botones
            HBox botones = new HBox(10);
            botones.setAlignment(Pos.CENTER_RIGHT);
            botones.setPadding(new Insets(10, 0, 0, 0));

            Button btnVerAgenda = new Button("📅 Ver Agenda Completa");
            btnVerAgenda.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-font-weight: bold;");
            btnVerAgenda.setOnAction(e -> {
                // Cambiar a la pestaña de agenda
                TabPane tabPane = (TabPane) scene.getRoot().lookup("TabPane");
                if (tabPane != null) {
                    Tab tabAgenda = tabPane.getTabs().stream()
                            .filter(t -> t.getText().contains("Agenda"))
                            .findFirst()
                            .orElse(null);
                    if (tabAgenda != null) {
                        tabPane.getSelectionModel().select(tabAgenda);
                    }
                }
                popup.close();
            });

            Button btnCerrar = new Button("✅ Entendido");
            btnCerrar.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-font-weight: bold;");
            btnCerrar.setOnAction(e -> popup.close());

            botones.getChildren().addAll(btnVerAgenda, btnCerrar);

            root.getChildren().addAll(header, scrollEventos, botones);

            Scene scene = new Scene(root, 550, Math.min(500, 200 + (eventosHoy.size() * 110)));
            popup.setScene(scene);

            // Posicionar en la esquina inferior derecha
            popup.setX(javafx.stage.Screen.getPrimary().getVisualBounds().getMaxX() - 570);
            popup.setY(javafx.stage.Screen.getPrimary().getVisualBounds().getMaxY() - scene.getHeight() - 50);

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

}

