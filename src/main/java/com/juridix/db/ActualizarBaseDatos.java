package com.juridix.db;

import java.sql.Connection;
import java.sql.Statement;
import java.sql.SQLException;

public class ActualizarBaseDatos {

    public static void main(String[] args) {
        try (Connection conn = Database.getConnection();
             Statement stmt = conn.createStatement()) {

            // 1. Agregar campo 'actor' a expedientes (ya lo tienes, pero por si acaso)
            try {
                stmt.execute("ALTER TABLE expedientes ADD COLUMN actor TEXT");
                System.out.println("✅ Campo 'actor' agregado a expedientes");
            } catch (SQLException e) {
                if (e.getMessage().contains("duplicate column")) {
                    System.out.println("ℹ️ El campo 'actor' ya existe");
                } else {
                    throw e;
                }
            }

            // 2. Crear tabla de documentos de expediente
            String documentosExpedienteTable = """
                CREATE TABLE IF NOT EXISTS documentos_expediente (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    expediente_id INTEGER NOT NULL,
                    nombre_archivo TEXT NOT NULL,
                    nombre_original TEXT NOT NULL,
                    ruta_archivo TEXT NOT NULL,
                    tipo_documento TEXT,
                    descripcion TEXT,
                    tamanio_bytes INTEGER,
                    extension TEXT,
                    usuario_id INTEGER,
                    fecha_subida TEXT DEFAULT (datetime('now', 'localtime')),
                    FOREIGN KEY (expediente_id) REFERENCES expedientes(id) ON DELETE CASCADE,
                    FOREIGN KEY (usuario_id) REFERENCES usuarios(id)
                );
            """;

            stmt.execute(documentosExpedienteTable);
            System.out.println("✅ Tabla documentos_expediente creada/verificada");

            // 3. Crear índice
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_documentos_expediente ON documentos_expediente(expediente_id)");
            System.out.println("✅ Índice creado");

            System.out.println("\n✅ Base de datos actualizada correctamente");

        } catch (SQLException e) {
            System.err.println("❌ Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}