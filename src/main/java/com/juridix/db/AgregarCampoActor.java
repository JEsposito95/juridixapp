package com.juridix.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class AgregarCampoActor {

    public static void main(String[] args) {
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "ALTER TABLE expedientes ADD COLUMN actor TEXT")) {

            ps.executeUpdate();
            System.out.println("✅ Campo 'actor' agregado a expedientes");

        } catch (SQLException e) {
            if (e.getMessage().contains("duplicate column")) {
                System.out.println("ℹ️ El campo 'actor' ya existe");
            } else {
                System.err.println("❌ Error: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
}