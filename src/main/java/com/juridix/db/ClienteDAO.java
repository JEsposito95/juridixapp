package com.juridix.db;

import com.juridix.db.Database;
import com.juridix.model.Cliente;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ClienteDAO {

    // ==================== CREATE ====================

    public Cliente guardar(Cliente cliente) throws SQLException {
        String sql = """
            INSERT INTO clientes (
                nombre_completo, dni, cuit_cuil, fecha_nacimiento, telefono,
                email, domicilio, localidad, provincia, codigo_postal,
                profesion, estado_civil, observaciones, activo, usuario_creador_id
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, cliente.getNombreCompleto());
            ps.setString(2, cliente.getDni());
            ps.setString(3, cliente.getCuitCuil());
            ps.setString(4, cliente.getFechaNacimientoAsString());
            ps.setString(5, cliente.getTelefono());
            ps.setString(6, cliente.getEmail());
            ps.setString(7, cliente.getDomicilio());
            ps.setString(8, cliente.getLocalidad());
            ps.setString(9, cliente.getProvincia());
            ps.setString(10, cliente.getCodigoPostal());
            ps.setString(11, cliente.getProfesion());
            ps.setString(12, cliente.getEstadoCivil());
            ps.setString(13, cliente.getObservaciones());
            ps.setInt(14, cliente.isActivo() ? 1 : 0);

            if (cliente.getUsuarioCreadorId() != null) {
                ps.setInt(15, cliente.getUsuarioCreadorId());
            } else {
                ps.setNull(15, Types.INTEGER);
            }

            int affectedRows = ps.executeUpdate();

            if (affectedRows == 0) {
                throw new SQLException("No se pudo guardar el cliente");
            }

            try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    cliente.setId(generatedKeys.getInt(1));
                }
            }

            return cliente;

        } catch (SQLException e) {
            System.err.println("❌ Error al guardar cliente: " + e.getMessage());
            throw e;
        }
    }

    // ==================== READ ====================

    public Optional<Cliente> buscarPorId(Integer id) throws SQLException {
        String sql = "SELECT * FROM clientes WHERE id = ?";

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapearCliente(rs));
                }
            }

            return Optional.empty();

        } catch (SQLException e) {
            System.err.println("❌ Error al buscar cliente: " + e.getMessage());
            throw e;
        }
    }

    public Optional<Cliente> buscarPorDni(String dni) throws SQLException {
        String sql = "SELECT * FROM clientes WHERE dni = ?";

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, dni);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapearCliente(rs));
                }
            }

            return Optional.empty();

        } catch (SQLException e) {
            System.err.println("❌ Error al buscar por DNI: " + e.getMessage());
            throw e;
        }
    }

    public List<Cliente> listarTodos() throws SQLException {
        String sql = """
            SELECT * FROM clientes 
            ORDER BY nombre_completo ASC
        """;

        List<Cliente> clientes = new ArrayList<>();

        try (Connection conn = Database.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                clientes.add(mapearCliente(rs));
            }

            return clientes;

        } catch (SQLException e) {
            System.err.println("❌ Error al listar clientes: " + e.getMessage());
            throw e;
        }
    }

    public List<Cliente> listarActivos() throws SQLException {
        String sql = """
            SELECT * FROM clientes 
            WHERE activo = 1
            ORDER BY nombre_completo ASC
        """;

        List<Cliente> clientes = new ArrayList<>();

        try (Connection conn = Database.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                clientes.add(mapearCliente(rs));
            }

            return clientes;

        } catch (SQLException e) {
            System.err.println("❌ Error al listar activos: " + e.getMessage());
            throw e;
        }
    }

    public List<Cliente> buscarPorNombre(String nombre) throws SQLException {
        String sql = """
            SELECT * FROM clientes 
            WHERE nombre_completo LIKE ?
            ORDER BY nombre_completo ASC
        """;

        List<Cliente> clientes = new ArrayList<>();

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, "%" + nombre + "%");

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    clientes.add(mapearCliente(rs));
                }
            }

            return clientes;

        } catch (SQLException e) {
            System.err.println("❌ Error al buscar por nombre: " + e.getMessage());
            throw e;
        }
    }

    public List<Cliente> buscarPorCriterios(String busqueda, Boolean soloActivos) throws SQLException {
        StringBuilder sql = new StringBuilder("SELECT * FROM clientes WHERE 1=1");
        List<Object> parametros = new ArrayList<>();

        if (busqueda != null && !busqueda.trim().isEmpty()) {
            sql.append(" AND (nombre_completo LIKE ? OR dni LIKE ? OR email LIKE ? OR telefono LIKE ?)");
            String patron = "%" + busqueda.trim() + "%";
            parametros.add(patron);
            parametros.add(patron);
            parametros.add(patron);
            parametros.add(patron);
        }

        if (soloActivos != null && soloActivos) {
            sql.append(" AND activo = 1");
        }

        sql.append(" ORDER BY nombre_completo ASC");

        List<Cliente> clientes = new ArrayList<>();

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {

            for (int i = 0; i < parametros.size(); i++) {
                ps.setObject(i + 1, parametros.get(i));
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    clientes.add(mapearCliente(rs));
                }
            }

            return clientes;

        } catch (SQLException e) {
            System.err.println("❌ Error al buscar por criterios: " + e.getMessage());
            throw e;
        }
    }

    // ==================== UPDATE ====================

    public Cliente actualizar(Cliente cliente) throws SQLException {
        String sql = """
            UPDATE clientes SET
                nombre_completo = ?,
                dni = ?,
                cuit_cuil = ?,
                fecha_nacimiento = ?,
                telefono = ?,
                email = ?,
                domicilio = ?,
                localidad = ?,
                provincia = ?,
                codigo_postal = ?,
                profesion = ?,
                estado_civil = ?,
                observaciones = ?,
                activo = ?,
                fecha_modificacion = datetime('now', 'localtime')
            WHERE id = ?
        """;

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, cliente.getNombreCompleto());
            ps.setString(2, cliente.getDni());
            ps.setString(3, cliente.getCuitCuil());
            ps.setString(4, cliente.getFechaNacimientoAsString());
            ps.setString(5, cliente.getTelefono());
            ps.setString(6, cliente.getEmail());
            ps.setString(7, cliente.getDomicilio());
            ps.setString(8, cliente.getLocalidad());
            ps.setString(9, cliente.getProvincia());
            ps.setString(10, cliente.getCodigoPostal());
            ps.setString(11, cliente.getProfesion());
            ps.setString(12, cliente.getEstadoCivil());
            ps.setString(13, cliente.getObservaciones());
            ps.setInt(14, cliente.isActivo() ? 1 : 0);
            ps.setInt(15, cliente.getId());

            int affectedRows = ps.executeUpdate();

            if (affectedRows == 0) {
                throw new SQLException("No se pudo actualizar el cliente");
            }

            return cliente;

        } catch (SQLException e) {
            System.err.println("❌ Error al actualizar cliente: " + e.getMessage());
            throw e;
        }
    }

    public void cambiarEstado(Integer id, boolean activo) throws SQLException {
        String sql = """
            UPDATE clientes 
            SET activo = ?,
                fecha_modificacion = datetime('now', 'localtime')
            WHERE id = ?
        """;

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, activo ? 1 : 0);
            ps.setInt(2, id);

            int affectedRows = ps.executeUpdate();

            if (affectedRows == 0) {
                throw new SQLException("No se pudo cambiar el estado");
            }

        } catch (SQLException e) {
            System.err.println("❌ Error al cambiar estado: " + e.getMessage());
            throw e;
        }
    }

    // ==================== DELETE ====================

    public void eliminar(Integer id) throws SQLException {
        String sql = "DELETE FROM clientes WHERE id = ?";

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);

            int affectedRows = ps.executeUpdate();

            if (affectedRows == 0) {
                throw new SQLException("No se pudo eliminar el cliente");
            }

        } catch (SQLException e) {
            System.err.println("❌ Error al eliminar cliente: " + e.getMessage());
            throw e;
        }
    }

    // ==================== ESTADÍSTICAS ====================

    public int contarTotal() throws SQLException {
        String sql = "SELECT COUNT(*) FROM clientes";

        try (Connection conn = Database.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            if (rs.next()) {
                return rs.getInt(1);
            }

            return 0;

        } catch (SQLException e) {
            System.err.println("❌ Error al contar clientes: " + e.getMessage());
            throw e;
        }
    }

    public int contarActivos() throws SQLException {
        String sql = "SELECT COUNT(*) FROM clientes WHERE activo = 1";

        try (Connection conn = Database.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            if (rs.next()) {
                return rs.getInt(1);
            }

            return 0;

        } catch (SQLException e) {
            System.err.println("❌ Error al contar activos: " + e.getMessage());
            throw e;
        }
    }

    // ==================== MAPEO ====================

    private Cliente mapearCliente(ResultSet rs) throws SQLException {
        Cliente cliente = new Cliente();

        cliente.setId(rs.getInt("id"));
        cliente.setNombreCompleto(rs.getString("nombre_completo"));
        cliente.setDni(rs.getString("dni"));
        cliente.setCuitCuil(rs.getString("cuit_cuil"));
        cliente.setFechaNacimientoFromString(rs.getString("fecha_nacimiento"));
        cliente.setTelefono(rs.getString("telefono"));
        cliente.setEmail(rs.getString("email"));
        cliente.setDomicilio(rs.getString("domicilio"));
        cliente.setLocalidad(rs.getString("localidad"));
        cliente.setProvincia(rs.getString("provincia"));
        cliente.setCodigoPostal(rs.getString("codigo_postal"));
        cliente.setProfesion(rs.getString("profesion"));
        cliente.setEstadoCivil(rs.getString("estado_civil"));
        cliente.setObservaciones(rs.getString("observaciones"));
        cliente.setActivo(rs.getInt("activo") == 1);

        int usuarioCreadorId = rs.getInt("usuario_creador_id");
        if (!rs.wasNull()) {
            cliente.setUsuarioCreadorId(usuarioCreadorId);
        }

        cliente.setFechaCreacionFromString(rs.getString("fecha_creacion"));
        cliente.setFechaModificacionFromString(rs.getString("fecha_modificacion"));

        return cliente;
    }
}