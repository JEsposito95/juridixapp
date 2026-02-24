package com.juridix.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class FixDatabase {

    public static void main(String[] args) {
        try {
            // 1. Ver clientes
            System.out.println("========== CLIENTES ==========");
            verClientes();

            System.out.println("\n========== EXPEDIENTES ANTES ==========");
            verExpedientes();

            // 2. Actualizar expedientes
            System.out.println("\n========== ACTUALIZANDO... ==========");
            actualizarExpedientes();

            System.out.println("\n========== EXPEDIENTES DESPUÉS ==========");
            verExpedientes();

            System.out.println("\n✅ ¡LISTO! Actualización completada.");

        } catch (SQLException e) {
            System.err.println("❌ Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void verClientes() throws SQLException {
        String sql = "SELECT id, nombre_completo FROM clientes";

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                System.out.println("ID: " + rs.getInt("id") +
                        " | Nombre: " + rs.getString("nombre_completo"));
            }
        }
    }

    private static void verExpedientes() throws SQLException {
        String sql = "SELECT id, numero, caratula, cliente, cliente_id FROM expedientes";

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                System.out.println("ID: " + rs.getInt("id") +
                        " | Número: " + rs.getString("numero") +
                        " | Cliente: " + rs.getString("cliente") +
                        " | cliente_id: " + rs.getObject("cliente_id"));
            }
        }
    }

    private static void actualizarExpedientes() throws SQLException {
        // Actualización automática: busca el cliente_id basándose en el nombre
        String sql = """
            UPDATE expedientes 
            SET cliente_id = (
                SELECT c.id 
                FROM clientes c 
                WHERE c.nombre_completo = expedientes.cliente
                LIMIT 1
            )
            WHERE cliente_id IS NULL 
              AND cliente IS NOT NULL
        """;

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            int rowsAffected = ps.executeUpdate();
            System.out.println("📊 Expedientes actualizados: " + rowsAffected);
        }
    }
}
