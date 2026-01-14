package com.juridix.db;

import com.juridix.seguridad.PasswordUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;

public class UserBootstrap {

    public static void crearAdminSiNoExiste() {

        String sql = """
            INSERT OR IGNORE INTO usuarios (username, password_hash, rol)
            VALUES (?, ?, ?)
        """;

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, "admin");
            ps.setString(2, PasswordUtil.hash("admin123"));
            ps.setString(3, "ABOGADO");

            ps.executeUpdate();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
