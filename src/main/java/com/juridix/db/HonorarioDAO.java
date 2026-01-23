package com.juridix.db;

import com.juridix.db.Database;
import com.juridix.model.Honorario;
import com.juridix.model.TipoHonorario;
import com.juridix.model.EstadoHonorario;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class HonorarioDAO {

    // ==================== CREATE ====================

    public Honorario guardar(Honorario honorario) throws SQLException {
        String sql = """
            INSERT INTO honorarios (
                expediente_id, tipo, porcentaje, monto_fijo, monto_calculado,
                descripcion, estado, fecha_estimada, usuario_id
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setInt(1, honorario.getExpedienteId());
            ps.setString(2, honorario.getTipo().name());

            if (honorario.getPorcentaje() != null) {
                ps.setDouble(3, honorario.getPorcentaje());
            } else {
                ps.setNull(3, Types.DOUBLE);
            }

            if (honorario.getMontoFijo() != null) {
                ps.setDouble(4, honorario.getMontoFijo());
            } else {
                ps.setNull(4, Types.DOUBLE);
            }

            if (honorario.getMontoCalculado() != null) {
                ps.setDouble(5, honorario.getMontoCalculado());
            } else {
                ps.setNull(5, Types.DOUBLE);
            }

            ps.setString(6, honorario.getDescripcion());
            ps.setString(7, honorario.getEstado().name());
            ps.setString(8, honorario.getFechaEstimadaAsString());

            if (honorario.getUsuarioId() != null) {
                ps.setInt(9, honorario.getUsuarioId());
            } else {
                ps.setNull(9, Types.INTEGER);
            }

            ps.executeUpdate();

            try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    honorario.setId(generatedKeys.getInt(1));
                }
            }

            return honorario;

        } catch (SQLException e) {
            System.err.println("❌ Error al guardar honorario: " + e.getMessage());
            throw e;
        }
    }

    // ==================== READ ====================

    public Optional<Honorario> buscarPorId(Integer id) throws SQLException {
        String sql = "SELECT * FROM honorarios WHERE id = ?";

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapearHonorario(rs));
                }
            }

            return Optional.empty();

        } catch (SQLException e) {
            System.err.println("❌ Error al buscar honorario: " + e.getMessage());
            throw e;
        }
    }

    public List<Honorario> listarPorExpediente(Integer expedienteId) throws SQLException {
        String sql = """
            SELECT * FROM honorarios 
            WHERE expediente_id = ?
            ORDER BY fecha_creacion DESC
        """;

        List<Honorario> honorarios = new ArrayList<>();

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, expedienteId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    honorarios.add(mapearHonorario(rs));
                }
            }

            return honorarios;

        } catch (SQLException e) {
            System.err.println("❌ Error al listar honorarios: " + e.getMessage());
            throw e;
        }
    }

    public List<Honorario> listarPorEstado(EstadoHonorario estado) throws SQLException {
        String sql = """
            SELECT * FROM honorarios 
            WHERE estado = ?
            ORDER BY fecha_estimada ASC
        """;

        List<Honorario> honorarios = new ArrayList<>();

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, estado.name());

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    honorarios.add(mapearHonorario(rs));
                }
            }

            return honorarios;

        } catch (SQLException e) {
            System.err.println("❌ Error al listar por estado: " + e.getMessage());
            throw e;
        }
    }

    // ==================== UPDATE ====================

    public Honorario actualizar(Honorario honorario) throws SQLException {
        String sql = """
            UPDATE honorarios SET
                tipo = ?,
                porcentaje = ?,
                monto_fijo = ?,
                monto_calculado = ?,
                descripcion = ?,
                estado = ?,
                fecha_estimada = ?
            WHERE id = ?
        """;

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, honorario.getTipo().name());

            if (honorario.getPorcentaje() != null) {
                ps.setDouble(2, honorario.getPorcentaje());
            } else {
                ps.setNull(2, Types.DOUBLE);
            }

            if (honorario.getMontoFijo() != null) {
                ps.setDouble(3, honorario.getMontoFijo());
            } else {
                ps.setNull(3, Types.DOUBLE);
            }

            if (honorario.getMontoCalculado() != null) {
                ps.setDouble(4, honorario.getMontoCalculado());
            } else {
                ps.setNull(4, Types.DOUBLE);
            }

            ps.setString(5, honorario.getDescripcion());
            ps.setString(6, honorario.getEstado().name());
            ps.setString(7, honorario.getFechaEstimadaAsString());
            ps.setInt(8, honorario.getId());

            ps.executeUpdate();
            return honorario;

        } catch (SQLException e) {
            System.err.println("❌ Error al actualizar honorario: " + e.getMessage());
            throw e;
        }
    }

    // ==================== DELETE ====================

    public void eliminar(Integer id) throws SQLException {
        String sql = "DELETE FROM honorarios WHERE id = ?";

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);
            ps.executeUpdate();

        } catch (SQLException e) {
            System.err.println("❌ Error al eliminar honorario: " + e.getMessage());
            throw e;
        }
    }

    // ==================== ESTADÍSTICAS ====================

    public Double calcularTotalPorExpediente(Integer expedienteId) throws SQLException {
        String sql = """
            SELECT COALESCE(SUM(monto_calculado), 0) 
            FROM honorarios 
            WHERE expediente_id = ?
        """;

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, expedienteId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getDouble(1);
                }
            }

            return 0.0;

        } catch (SQLException e) {
            System.err.println("❌ Error al calcular total: " + e.getMessage());
            throw e;
        }
    }

    // ==================== MAPEO ====================

    private Honorario mapearHonorario(ResultSet rs) throws SQLException {
        Honorario honorario = new Honorario();

        honorario.setId(rs.getInt("id"));
        honorario.setExpedienteId(rs.getInt("expediente_id"));
        honorario.setTipo(TipoHonorario.fromString(rs.getString("tipo")));

        double porcentaje = rs.getDouble("porcentaje");
        if (!rs.wasNull()) {
            honorario.setPorcentaje(porcentaje);
        }

        double montoFijo = rs.getDouble("monto_fijo");
        if (!rs.wasNull()) {
            honorario.setMontoFijo(montoFijo);
        }

        double montoCalculado = rs.getDouble("monto_calculado");
        if (!rs.wasNull()) {
            honorario.setMontoCalculado(montoCalculado);
        }

        honorario.setDescripcion(rs.getString("descripcion"));
        honorario.setEstado(EstadoHonorario.fromString(rs.getString("estado")));
        honorario.setFechaEstimadaFromString(rs.getString("fecha_estimada"));

        int usuarioId = rs.getInt("usuario_id");
        if (!rs.wasNull()) {
            honorario.setUsuarioId(usuarioId);
        }

        honorario.setFechaCreacionFromString(rs.getString("fecha_creacion"));

        return honorario;
    }
}