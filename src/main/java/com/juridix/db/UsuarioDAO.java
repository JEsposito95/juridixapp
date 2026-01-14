package com.juridix.db;

import com.juridix.model.Usuario;
import com.juridix.model.RolUsuario;

import java.sql.Connection;

import java.sql.*;

public class UsuarioDAO {

    public UsuarioDAO() {
        crearTablaSiNoExiste();
    }

    private void crearTablaSiNoExiste() {
        String sql = """
            CREATE TABLE IF NOT EXISTS usuarios (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                username TEXT UNIQUE NOT NULL,
                password_hash TEXT NOT NULL,
                rol TEXT NOT NULL,
                activo INTEGER NOT NULL DEFAULT 1
            );
        """;

        try (Connection conn = Database.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        } catch (SQLException e) {
            System.out.println("Error creando tabla usuarios: " + e.getMessage());
        }
    }

    public Usuario buscarPorUsername(String username) {

        String sql = "SELECT * FROM usuarios WHERE username = ? AND activo = 1";

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, username);

            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                Usuario u = new Usuario();
                u.setId(rs.getInt("id"));
                u.setUsername(rs.getString("username"));
                u.setPasswordHash(rs.getString("password_hash"));
                RolUsuario.valueOf(rs.getString("rol"));
                u.setActivo(rs.getInt("activo") == 1);
                return u;
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }
}
