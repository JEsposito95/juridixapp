package com.juridix.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class Database {
    private static final String URL = "jdbc:sqlite:juridix.db";

    private Database() {
        // Constructor privado
    }

    /**
     * Obtiene una nueva conexión a la base de datos
     * IMPORTANTE: Cada llamada retorna una NUEVA conexión que debe cerrarse después de usarse
     */
    public static Connection getConnection() throws SQLException {
        try {
            // Cargar el driver
            Class.forName("org.sqlite.JDBC");

            // Crear nueva conexión
            Connection conn = DriverManager.getConnection(URL);

            // Configurar para SQLite
            conn.setAutoCommit(true); // MUY IMPORTANTE

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
}