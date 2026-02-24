package com.juridix.db;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

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
}