package com.juridix.db;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.*;

public class Database {
    // ✅ Ruta en carpeta del usuario
    private static final String APP_DIR = System.getProperty("user.home") + File.separator + "Juridix";
    private static final String DB_FILE = "juridix.db";
    private static final String URL = "jdbc:sqlite:" + APP_DIR + File.separator + DB_FILE;

    static {
        // Crear directorio si no existe
        try {
            Path appPath = Paths.get(APP_DIR);
            if (!Files.exists(appPath)) {
                Files.createDirectories(appPath);
                System.out.println("✅ Directorio de aplicación creado: " + appPath.toAbsolutePath());
            }
        } catch (Exception e) {
            System.err.println("❌ Error al crear directorio de aplicación: " + e.getMessage());
        }
    }

    private Database() {
        // Constructor privado
    }

    /**
     * Obtiene una nueva conexión a la base de datos
     */
    public static Connection getConnection() throws SQLException {
        try {
            Class.forName("org.sqlite.JDBC");
            Connection conn = DriverManager.getConnection(URL);
            conn.setAutoCommit(true);
            return conn;
        } catch (ClassNotFoundException e) {
            throw new SQLException("Driver SQLite no encontrado", e);
        }
    }

    /**
     * Verifica si la base de datos es accesible
     */
    public static boolean testConnection() {
        try (Connection conn = getConnection()) {
            return conn != null && !conn.isClosed();
        } catch (SQLException e) {
            System.err.println("❌ Error al probar conexión: " + e.getMessage());
            return false;
        }
    }

    /**
     * Obtiene la ruta del directorio de la aplicación
     */
    public static String getAppDirectory() {
        return APP_DIR;
    }

    /**
     * Obtiene la ruta completa de la base de datos
     */
    public static String getDatabasePath() {
        return APP_DIR + File.separator + DB_FILE;
    }

    /**
     * Verifica y actualiza la estructura de la base de datos
     */
    public static void verificarYActualizarEstructura() {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {

            // 1. Verificar y agregar columna actor en expedientes
            ResultSet rs = stmt.executeQuery("PRAGMA table_info(expedientes)");
            boolean tieneActor = false;

            while (rs.next()) {
                if ("actor".equals(rs.getString("name"))) {
                    tieneActor = true;
                    break;
                }
            }

            if (!tieneActor) {
                stmt.execute("ALTER TABLE expedientes ADD COLUMN actor TEXT");
                System.out.println("✅ Columna 'actor' agregada a expedientes");
            }

            // 2. Verificar y crear tabla cuotas si no existe
            ResultSet rsTables = stmt.executeQuery(
                    "SELECT name FROM sqlite_master WHERE type='table' AND name='cuotas'"
            );

            if (!rsTables.next()) {
                // Crear tabla cuotas
                stmt.execute("""
                CREATE TABLE cuotas (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    expediente_id INTEGER NOT NULL,
                    monto_total_acordado REAL NOT NULL,
                    monto_pagado REAL DEFAULT 0,
                    cantidad_cuotas_planificadas INTEGER,
                    monto_por_cuota REAL,
                    observaciones TEXT,
                    fecha_acuerdo DATE NOT NULL,
                    estado TEXT DEFAULT 'ACTIVO',
                    usuario_id INTEGER NOT NULL,
                    fecha_creacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY (expediente_id) REFERENCES expedientes(id),
                    FOREIGN KEY (usuario_id) REFERENCES usuarios(id)
                )
            """);
                System.out.println("✅ Tabla 'cuotas' creada");

                // Crear tabla pagos_cuota
                stmt.execute("""
                CREATE TABLE pagos_cuota (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    cuota_id INTEGER NOT NULL,
                    monto REAL NOT NULL,
                    fecha_pago DATE NOT NULL,
                    forma_pago TEXT,
                    numero_cuota INTEGER,
                    referencia TEXT,
                    observaciones TEXT,
                    usuario_id INTEGER NOT NULL,
                    fecha_creacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY (cuota_id) REFERENCES cuotas(id),
                    FOREIGN KEY (usuario_id) REFERENCES usuarios(id)
                )
            """);
                System.out.println("✅ Tabla 'pagos_cuota' creada");
            }

            System.out.println("✅ Estructura de BD verificada - OK");

        } catch (SQLException e) {
            System.err.println("⚠️ Error al verificar estructura: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
