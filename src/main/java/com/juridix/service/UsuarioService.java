package com.juridix.service;

import com.juridix.db.Database;
import com.juridix.model.Usuario;
import com.juridix.seguridad.PasswordUtil;
import com.juridix.model.RolUsuario;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class UsuarioService {
    public Usuario login(String username, String password) {
        String sql = """
            SELECT * FROM usuarios
            WHERE username = ? AND activo = 1
        """;

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, username);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                String hash = rs.getString("password_hash");

                if (PasswordUtil.verificar(password, hash)) {
                    Usuario u = new Usuario();
                    u.setId(rs.getInt("id"));
                    u.setUsername(username);
                    String rolStr = rs.getString("rol");
                    u.setRol(RolUsuario.valueOf(rolStr));
                    u.setActivo(true);
                    return u;
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
