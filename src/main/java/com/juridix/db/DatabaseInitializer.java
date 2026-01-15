package com.juridix.db;

import com.juridix.seguridad.PasswordUtil;
import java.sql.Connection;
import java.sql.Statement;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class DatabaseInitializer {

    public static void init() {
        Connection conn = null;
        Statement stmt = null;
        PreparedStatement ps = null;

        try {
            // Obtener conexión
            conn = Database.getConnection();
            stmt = conn.createStatement();

            // IMPORTANTE: Configurar SQLite para evitar locks
            stmt.execute("PRAGMA journal_mode=WAL");
            stmt.execute("PRAGMA busy_timeout=5000");
            stmt.execute("PRAGMA foreign_keys=ON");

            System.out.println("🔧 Inicializando base de datos...");

            // Tabla de usuarios
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

            // Tabla de expedientes
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

            // Ejecutar creación de tablas
            stmt.execute(usuariosTable);
            System.out.println("✅ Tabla usuarios creada/verificada");

            stmt.execute(expedientesTable);
            System.out.println("✅ Tabla expedientes creada/verificada");

            // Crear índices
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_expedientes_estado ON expedientes(estado)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_expedientes_cliente ON expedientes(cliente)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_expedientes_numero ON expedientes(numero)");
            System.out.println("✅ Índices creados/verificados");

            // Cerrar Statement antes de usar PreparedStatement
            if (stmt != null) {
                stmt.close();
                stmt = null;
            }

            // Insertar usuario admin por defecto
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
            // CRÍTICO: Cerrar recursos en orden inverso
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