package com.juridix.service;

import com.juridix.db.Database;
import com.juridix.model.Usuario;
import com.juridix.seguridad.PasswordUtil;
import com.juridix.model.RolUsuario;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class UsuarioService {

    // ==================== LOGIN (ya lo tienes) ====================
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
                    Usuario u = mapearUsuario(rs);

                    // Actualizar último acceso
                    actualizarUltimoAcceso(u.getId());

                    return u;
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    // ==================== CREATE ====================
    public Usuario crearUsuario(Usuario usuario) throws SQLException {
        validarUsuario(usuario);

        // Verificar que el username no exista
        if (existeUsername(usuario.getUsername())) {
            throw new IllegalArgumentException("El nombre de usuario ya existe");
        }

        String sql = """
            INSERT INTO usuarios (username, password_hash, rol, nombre_completo, email, activo)
            VALUES (?, ?, ?, ?, ?, ?)
        """;

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, usuario.getUsername());
            ps.setString(2, usuario.getPasswordHash());
            ps.setString(3, usuario.getRol().name());
            ps.setString(4, usuario.getNombreCompleto());
            ps.setString(5, usuario.getEmail());
            ps.setInt(6, usuario.isActivo() ? 1 : 0);

            ps.executeUpdate();

            ResultSet rs = ps.getGeneratedKeys();
            if (rs.next()) {
                usuario.setId(rs.getInt(1));
            }

            return usuario;
        }
    }

    // ==================== READ ====================
    public Optional<Usuario> buscarPorId(Integer id) throws SQLException {
        String sql = "SELECT * FROM usuarios WHERE id = ?";

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return Optional.of(mapearUsuario(rs));
            }

            return Optional.empty();
        }
    }

    public Optional<Usuario> buscarPorUsername(String username) throws SQLException {
        String sql = "SELECT * FROM usuarios WHERE username = ?";

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, username);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return Optional.of(mapearUsuario(rs));
            }

            return Optional.empty();
        }
    }

    public List<Usuario> listarTodos() throws SQLException {
        String sql = "SELECT * FROM usuarios ORDER BY username";
        List<Usuario> usuarios = new ArrayList<>();

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                usuarios.add(mapearUsuario(rs));
            }
        }

        return usuarios;
    }

    public List<Usuario> listarActivos() throws SQLException {
        String sql = "SELECT * FROM usuarios WHERE activo = 1 ORDER BY username";
        List<Usuario> usuarios = new ArrayList<>();

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                usuarios.add(mapearUsuario(rs));
            }
        }

        return usuarios;
    }

    // ==================== UPDATE ====================
    public Usuario actualizarUsuario(Usuario usuario) throws SQLException {
        validarUsuario(usuario);

        if (usuario.getId() == null) {
            throw new IllegalArgumentException("El usuario debe tener un ID");
        }

        // Si se está actualizando la contraseña, debe venir en passwordHash
        // Si passwordHash es null, no se actualiza la contraseña

        String sql;
        if (usuario.getPasswordHash() != null && !usuario.getPasswordHash().isEmpty()) {
            sql = """
                UPDATE usuarios 
                SET password_hash = ?, rol = ?, nombre_completo = ?, 
                    email = ?, activo = ?, fecha_modificacion = datetime('now', 'localtime')
                WHERE id = ?
            """;
        } else {
            sql = """
                UPDATE usuarios 
                SET rol = ?, nombre_completo = ?, email = ?, activo = ?,
                    fecha_modificacion = datetime('now', 'localtime')
                WHERE id = ?
            """;
        }

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            int index = 1;

            if (usuario.getPasswordHash() != null && !usuario.getPasswordHash().isEmpty()) {
                ps.setString(index++, usuario.getPasswordHash());
            }

            ps.setString(index++, usuario.getRol().name());
            ps.setString(index++, usuario.getNombreCompleto());
            ps.setString(index++, usuario.getEmail());
            ps.setInt(index++, usuario.isActivo() ? 1 : 0);
            ps.setInt(index++, usuario.getId());

            ps.executeUpdate();

            return usuario;
        }
    }

    public void actualizarUltimoAcceso(Integer id) throws SQLException {
        String sql = "UPDATE usuarios SET ultimo_acceso = datetime('now', 'localtime') WHERE id = ?";

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    // ==================== DELETE ====================
    public void eliminarUsuario(Integer id) throws SQLException {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("El ID debe ser válido");
        }

        String sql = "DELETE FROM usuarios WHERE id = ?";

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    // ==================== VALIDACIONES ====================
    private void validarUsuario(Usuario usuario) {
        if (usuario == null) {
            throw new IllegalArgumentException("El usuario no puede ser nulo");
        }

        if (usuario.getUsername() == null || usuario.getUsername().trim().isEmpty()) {
            throw new IllegalArgumentException("El nombre de usuario es obligatorio");
        }

        if (usuario.getRol() == null) {
            throw new IllegalArgumentException("El rol es obligatorio");
        }

        // Validar username: solo letras, números y guiones bajos
        if (!usuario.getUsername().matches("^[a-zA-Z0-9_]{3,20}$")) {
            throw new IllegalArgumentException("El nombre de usuario debe tener entre 3 y 20 caracteres (solo letras, números y guiones bajos)");
        }
    }

    private boolean existeUsername(String username) throws SQLException {
        String sql = "SELECT COUNT(*) FROM usuarios WHERE username = ?";

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, username);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return rs.getInt(1) > 0;
            }

            return false;
        }
    }

    // ==================== MAPEO ====================
    private Usuario mapearUsuario(ResultSet rs) throws SQLException {
        Usuario u = new Usuario();
        u.setId(rs.getInt("id"));
        u.setUsername(rs.getString("username"));
        u.setPasswordHash(rs.getString("password_hash"));
        u.setRol(RolUsuario.valueOf(rs.getString("rol")));
        u.setNombreCompleto(rs.getString("nombre_completo"));
        u.setEmail(rs.getString("email"));
        u.setActivo(rs.getInt("activo") == 1);

        String fechaCreacion = rs.getString("fecha_creacion");
        if (fechaCreacion != null) {
            u.setFechaCreacion(LocalDateTime.parse(fechaCreacion.replace(" ", "T")));
        }

        String ultimoAcceso = rs.getString("ultimo_acceso");
        if (ultimoAcceso != null) {
            u.setUltimoAcceso(LocalDateTime.parse(ultimoAcceso.replace(" ", "T")));
        }

        return u;
    }
}