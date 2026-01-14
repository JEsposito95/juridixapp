package com.juridix.db;

import java.sql.Connection;
import java.sql.Statement;

public class DatabaseInitializer {

    public static void init() {
        try (Connection conn = Database.getConnection();
             Statement stmt = conn.createStatement()) {

            String usuariosTable = """
                CREATE TABLE IF NOT EXISTS usuarios (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    username TEXT UNIQUE NOT NULL,
                    password_hash TEXT NOT NULL,
                    rol TEXT NOT NULL,
                    activo INTEGER NOT NULL DEFAULT 1
                );
            """;
            String adminInsert = """
                        INSERT OR IGNORE INTO usuarios (username, password_hash, rol, activo)
                        VALUES ('admin', ?, 'ADMIN', 1);
                    """;

            String expedientesTable = """
                            CREATE TABLE IF NOT EXISTS expedientes (
                            id INTEGER PRIMARY KEY AUTOINCREMENT,
                            numero TEXT NOT NULL UNIQUE,
                            caratula TEXT NOT NULL,
                            cliente TEXT NOT NULL,
                            estado TEXT NOT NULL,
                            fecha_inicio TEXT NOT NULL,
                            creador_id INTEGER NOT NULL,
                            FOREIGN KEY (creador_id) REFERENCES usuarios(id)
                        );
""";

            try (var ps = conn.prepareStatement(adminInsert)) {
                ps.setString(1, com.juridix.seguridad.PasswordUtil.hash("admin123"));
                ps.executeUpdate();
            }

            stmt.execute(usuariosTable);
            stmt.execute(expedientesTable);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Error initializing database");
        }
    }
}
