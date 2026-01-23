package com.juridix.db;

import com.juridix.seguridad.PasswordUtil;
import java.sql.Connection;
import java.sql.Statement;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class DatabaseInitializer {

    public static void init() throws SQLException {
        Connection conn = null;
        Statement stmt = null;
        PreparedStatement ps = null;

        try {
            conn = Database.getConnection();
            stmt = conn.createStatement();

            stmt.execute("PRAGMA journal_mode=WAL");
            stmt.execute("PRAGMA busy_timeout=5000");
            stmt.execute("PRAGMA foreign_keys=ON");

            System.out.println("🔧 Inicializando base de datos...");

            // ========== TABLA USUARIOS ==========
            String usuariosTable = """
                CREATE TABLE IF NOT EXISTS usuarios (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    username TEXT UNIQUE NOT NULL,
                    password_hash TEXT NOT NULL,
                    rol TEXT NOT NULL CHECK(rol IN ('ADMIN', 'ABOGADO', 'SECRETARIO')),
                    nombre_completo TEXT,
                    email TEXT,
                    activo INTEGER NOT NULL DEFAULT 1 CHECK(activo IN (0, 1)),
                    fecha_creacion TEXT DEFAULT (datetime('now', 'localtime')),
                    ultimo_acceso TEXT
                );
            """;

            // ========== TABLA EXPEDIENTES ==========
            String expedientesTable = """
                CREATE TABLE IF NOT EXISTS expedientes (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    numero TEXT NOT NULL UNIQUE,
                    caratula TEXT NOT NULL,
                    cliente TEXT NOT NULL,
                    demandado TEXT,
                    fuero TEXT,
                    juzgado TEXT,
                    secretaria TEXT,
                    estado TEXT NOT NULL CHECK(estado IN ('ACTIVO', 'ARCHIVADO', 'SUSPENDIDO', 'FINALIZADO')) DEFAULT 'ACTIVO',
                    fecha_inicio TEXT NOT NULL,
                    fecha_finalizacion TEXT,
                    monto_estimado REAL,
                    observaciones TEXT,
                    creador_id INTEGER NOT NULL,
                    fecha_creacion TEXT DEFAULT (datetime('now', 'localtime')),
                    fecha_modificacion TEXT DEFAULT (datetime('now', 'localtime')),
                    FOREIGN KEY (creador_id) REFERENCES usuarios(id) ON DELETE RESTRICT
                );
            """;

            // ========== TABLA MOVIMIENTOS ==========
            String movimientosTable = """
                CREATE TABLE IF NOT EXISTS movimientos (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    expediente_id INTEGER NOT NULL,
                    fecha DATE NOT NULL,
                    tipo TEXT NOT NULL CHECK(tipo IN ('PRESENTACION', 'AUDIENCIA', 'RESOLUCION', 'NOTIFICACION', 'OTRO')),
                    descripcion TEXT NOT NULL,
                    cuaderno TEXT,
                    foja INTEGER,
                    observaciones TEXT,
                    usuario_id INTEGER NOT NULL,
                    fecha_creacion TEXT DEFAULT (datetime('now', 'localtime')),
                    FOREIGN KEY (expediente_id) REFERENCES expedientes(id) ON DELETE CASCADE,
                    FOREIGN KEY (usuario_id) REFERENCES usuarios(id)
                );
            """;

            // ========== TABLA EVENTOS AGENDA ==========
            String agendaTable = """
                CREATE TABLE IF NOT EXISTS eventos_agenda (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    titulo TEXT NOT NULL,
                    descripcion TEXT,
                    fecha_hora TEXT NOT NULL,
                    duracion_minutos INTEGER DEFAULT 60,
                    tipo TEXT NOT NULL CHECK(tipo IN ('AUDIENCIA', 'VENCIMIENTO', 'REUNION', 'PRESENTACION', 'OTRO')),
                    expediente_id INTEGER,
                    ubicacion TEXT,
                    estado TEXT DEFAULT 'PENDIENTE' CHECK(estado IN ('PENDIENTE', 'COMPLETADO', 'CANCELADO')),
                    recordatorio_minutos INTEGER DEFAULT 1440,
                    color TEXT DEFAULT '#3498db',
                    usuario_id INTEGER NOT NULL,
                    fecha_creacion TEXT DEFAULT (datetime('now', 'localtime')),
                    FOREIGN KEY (expediente_id) REFERENCES expedientes(id) ON DELETE SET NULL,
                    FOREIGN KEY (usuario_id) REFERENCES usuarios(id)
                );
            """;

            // ========== TABLA CLIENTES ==========
            String clientesTable = """
                CREATE TABLE IF NOT EXISTS clientes (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    nombre_completo TEXT NOT NULL,
                    dni TEXT,
                    cuit_cuil TEXT,
                    fecha_nacimiento TEXT,
                    telefono TEXT,
                    email TEXT,
                    domicilio TEXT,
                    localidad TEXT,
                    provincia TEXT,
                    codigo_postal TEXT,
                    profesion TEXT,
                    estado_civil TEXT,
                    observaciones TEXT,
                    activo INTEGER DEFAULT 1 CHECK(activo IN (0, 1)),
                    usuario_creador_id INTEGER,
                    fecha_creacion TEXT DEFAULT (datetime('now', 'localtime')),
                    fecha_modificacion TEXT DEFAULT (datetime('now', 'localtime')),
                    FOREIGN KEY (usuario_creador_id) REFERENCES usuarios(id)
                );
            """;

            // ========== TABLA DOCUMENTOS CLIENTE ==========
            String documentosClienteTable = """
                CREATE TABLE IF NOT EXISTS documentos_cliente (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    cliente_id INTEGER NOT NULL,
                    nombre_archivo TEXT NOT NULL,
                    nombre_original TEXT NOT NULL,
                    ruta_archivo TEXT NOT NULL,
                    tipo_documento TEXT,
                    descripcion TEXT,
                    tamanio_bytes INTEGER,
                    extension TEXT,
                    usuario_id INTEGER,
                    fecha_subida TEXT DEFAULT (datetime('now', 'localtime')),
                    FOREIGN KEY (cliente_id) REFERENCES clientes(id) ON DELETE CASCADE,
                    FOREIGN KEY (usuario_id) REFERENCES usuarios(id)
                );
            """;

            // ========== TABLA HONORARIOS ==========
            String honorariosTable = """
                CREATE TABLE IF NOT EXISTS honorarios (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    expediente_id INTEGER NOT NULL,
                    tipo TEXT NOT NULL CHECK(tipo IN ('PORCENTAJE', 'MONTO_FIJO', 'REGULACION_JUDICIAL')),
                    porcentaje REAL,
                    monto_fijo REAL,
                    monto_calculado REAL,
                    descripcion TEXT,
                    estado TEXT DEFAULT 'PENDIENTE' CHECK(estado IN ('PENDIENTE', 'PARCIAL', 'COBRADO')),
                    fecha_estimada TEXT,
                    usuario_id INTEGER,
                    fecha_creacion TEXT DEFAULT (datetime('now', 'localtime')),
                    FOREIGN KEY (expediente_id) REFERENCES expedientes(id) ON DELETE CASCADE,
                    FOREIGN KEY (usuario_id) REFERENCES usuarios(id)
                );
            """;

            // ========== TABLA GASTOS ==========
            String gastosTable = """
                CREATE TABLE IF NOT EXISTS gastos (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    expediente_id INTEGER NOT NULL,
                    concepto TEXT NOT NULL,
                    monto REAL NOT NULL,
                    fecha DATE NOT NULL,
                    categoria TEXT,
                    comprobante TEXT,
                    observaciones TEXT,
                    usuario_id INTEGER,
                    fecha_creacion TEXT DEFAULT (datetime('now', 'localtime')),
                    FOREIGN KEY (expediente_id) REFERENCES expedientes(id) ON DELETE CASCADE,
                    FOREIGN KEY (usuario_id) REFERENCES usuarios(id)
                );
            """;

            // ========== TABLA PAGOS ==========
            String pagosTable = """
                CREATE TABLE IF NOT EXISTS pagos (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    expediente_id INTEGER NOT NULL,
                    cliente_id INTEGER,
                    monto REAL NOT NULL,
                    fecha DATE NOT NULL,
                    forma_pago TEXT,
                    referencia TEXT,
                    concepto TEXT,
                    observaciones TEXT,
                    usuario_id INTEGER,
                    fecha_creacion TEXT DEFAULT (datetime('now', 'localtime')),
                    FOREIGN KEY (expediente_id) REFERENCES expedientes(id) ON DELETE CASCADE,
                    FOREIGN KEY (cliente_id) REFERENCES clientes(id) ON DELETE SET NULL,
                    FOREIGN KEY (usuario_id) REFERENCES usuarios(id)
                );
            """;

            // EJECUTAR CREACIÓN DE TABLAS
            stmt.execute(usuariosTable);
            System.out.println("✅ Tabla usuarios creada/verificada");

            stmt.execute(expedientesTable);
            System.out.println("✅ Tabla expedientes creada/verificada");

            stmt.execute(movimientosTable);
            System.out.println("✅ Tabla movimientos creada/verificada");

            stmt.execute(agendaTable);
            System.out.println("✅ Tabla eventos_agenda creada/verificada");

            stmt.execute(clientesTable);
            System.out.println("✅ Tabla clientes creada/verificada");

            stmt.execute(documentosClienteTable);
            System.out.println("✅ Tabla documentos_cliente creada/verificada");

            stmt.execute(honorariosTable);
            System.out.println("✅ Tabla honorarios creada/verificada");

            stmt.execute(gastosTable);
            System.out.println("✅ Tabla gastos creada/verificada");

            stmt.execute(pagosTable);
            System.out.println("✅ Tabla pagos creada/verificada");

            // Agregar cliente_id a expedientes (si no existe)
            try {
                stmt.execute("ALTER TABLE expedientes ADD COLUMN cliente_id INTEGER REFERENCES clientes(id)");
                System.out.println("✅ Campo cliente_id agregado a expedientes");
            } catch (SQLException e) {
                System.out.println("ℹ️ Campo cliente_id ya existe en expedientes");
            }

            // ========== ÍNDICES ==========
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_expedientes_estado ON expedientes(estado)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_expedientes_cliente ON expedientes(cliente)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_expedientes_numero ON expedientes(numero)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_movimientos_expediente ON movimientos(expediente_id)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_movimientos_fecha ON movimientos(fecha)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_agenda_fecha ON eventos_agenda(fecha_hora)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_agenda_usuario ON eventos_agenda(usuario_id)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_agenda_estado ON eventos_agenda(estado)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_clientes_nombre ON clientes(nombre_completo)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_clientes_dni ON clientes(dni)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_clientes_activo ON clientes(activo)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_documentos_cliente ON documentos_cliente(cliente_id)");

            // Índices económicos
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_honorarios_expediente ON honorarios(expediente_id)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_gastos_expediente ON gastos(expediente_id)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_pagos_expediente ON pagos(expediente_id)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_pagos_cliente ON pagos(cliente_id)");
            System.out.println("✅ Índices creados/verificados");

            // ========== CERRAR STATEMENT ANTES DE USAR PREPAREDSTATEMENT ==========
            stmt.close();
            stmt = null;

            // ========== USUARIO ADMIN ==========
            String adminInsert = """
                INSERT OR IGNORE INTO usuarios (username, password_hash, rol, nombre_completo, activo)
                VALUES (?, ?, 'ADMIN', 'Administrador', 1)
            """;

            ps = conn.prepareStatement(adminInsert);
            ps.setString(1, "admin");
            ps.setString(2, PasswordUtil.hash("admin123"));
            int inserted = ps.executeUpdate();

            if (inserted > 0) {
                System.out.println("✅ Usuario admin creado");
            } else {
                System.out.println("ℹ️ Usuario admin ya existe");
            }

            System.out.println("✅ Base de datos inicializada correctamente");

        } catch (SQLException e) {
            System.err.println("❌ Error al inicializar la base de datos: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Error crítico al inicializar la base de datos", e);

        } finally {
            try {
                if (ps != null) ps.close();
            } catch (SQLException e) {
                System.err.println("⚠️ Error al cerrar PreparedStatement: " + e.getMessage());
            }

            try {
                if (stmt != null) stmt.close();
            } catch (SQLException e) {
                System.err.println("⚠️ Error al cerrar Statement: " + e.getMessage());
            }

            try {
                if (conn != null) conn.close();
            } catch (SQLException e) {
                System.err.println("⚠️ Error al cerrar Connection: " + e.getMessage());
            }
        }
    }
}