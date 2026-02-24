package com.juridix.db;

import java.sql.Connection;
import java.sql.Statement;
import java.sql.SQLException;

public class CrearTablaCuotas {

    public static void main(String[] args) {
        try (Connection conn = Database.getConnection();
             Statement stmt = conn.createStatement()) {

            // Crear tabla de cuotas
            String tablaCuotas = """
                CREATE TABLE IF NOT EXISTS cuotas (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    expediente_id INTEGER NOT NULL,
                    monto_total_acordado REAL NOT NULL,
                    monto_pagado REAL DEFAULT 0,
                    cantidad_cuotas_planificadas INTEGER,
                    monto_por_cuota REAL,
                    observaciones TEXT,
                    fecha_acuerdo DATE NOT NULL,
                    estado TEXT DEFAULT 'ACTIVO' CHECK(estado IN ('ACTIVO', 'COMPLETADO', 'CANCELADO')),
                    usuario_id INTEGER,
                    fecha_creacion TEXT DEFAULT (datetime('now', 'localtime')),
                    FOREIGN KEY (expediente_id) REFERENCES expedientes(id) ON DELETE CASCADE,
                    FOREIGN KEY (usuario_id) REFERENCES usuarios(id)
                );
            """;

            stmt.execute(tablaCuotas);
            System.out.println("✅ Tabla cuotas creada");

            // Crear tabla de pagos de cuotas (entregas parciales)
            String tablaPagosCuotas = """
                CREATE TABLE IF NOT EXISTS pagos_cuota (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    cuota_id INTEGER NOT NULL,
                    monto REAL NOT NULL,
                    fecha_pago DATE NOT NULL,
                    forma_pago TEXT,
                    numero_cuota INTEGER,
                    referencia TEXT,
                    observaciones TEXT,
                    usuario_id INTEGER,
                    fecha_creacion TEXT DEFAULT (datetime('now', 'localtime')),
                    FOREIGN KEY (cuota_id) REFERENCES cuotas(id) ON DELETE CASCADE,
                    FOREIGN KEY (usuario_id) REFERENCES usuarios(id)
                );
            """;

            stmt.execute(tablaPagosCuotas);
            System.out.println("✅ Tabla pagos_cuota creada");

            // Índices
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_cuotas_expediente ON cuotas(expediente_id)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_pagos_cuota ON pagos_cuota(cuota_id)");

            System.out.println("✅ Índices creados");
            System.out.println("\n✅ Sistema de cuotas creado correctamente");

        } catch (SQLException e) {
            System.err.println("❌ Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}