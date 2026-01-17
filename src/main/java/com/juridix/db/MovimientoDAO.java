package com.juridix.db;

import com.juridix.db.Database;
import com.juridix.model.Movimiento;
import com.juridix.model.TipoMovimiento;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class MovimientoDAO {

    // ==================== CREATE ====================

    public Movimiento guardar(Movimiento movimiento) throws SQLException {
        String sql = """
            INSERT INTO movimientos (
                expediente_id, fecha, tipo, descripcion, cuaderno, 
                foja, observaciones, usuario_id
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
        """;

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setInt(1, movimiento.getExpedienteId());
            ps.setString(2, movimiento.getFechaAsString());
            ps.setString(3, movimiento.getTipo().name());
            ps.setString(4, movimiento.getDescripcion());
            ps.setString(5, movimiento.getCuaderno());

            if (movimiento.getFoja() != null) {
                ps.setInt(6, movimiento.getFoja());
            } else {
                ps.setNull(6, Types.INTEGER);
            }

            ps.setString(7, movimiento.getObservaciones());
            ps.setInt(8, movimiento.getUsuarioId());

            int affectedRows = ps.executeUpdate();

            if (affectedRows == 0) {
                throw new SQLException("No se pudo guardar el movimiento");
            }

            try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    movimiento.setId(generatedKeys.getInt(1));
                }
            }

            return movimiento;

        } catch (SQLException e) {
            System.err.println("❌ Error al guardar movimiento: " + e.getMessage());
            throw e;
        }
    }

    // ==================== READ ====================

    public Optional<Movimiento> buscarPorId(Integer id) throws SQLException {
        String sql = "SELECT * FROM movimientos WHERE id = ?";

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapearMovimiento(rs));
                }
            }

            return Optional.empty();

        } catch (SQLException e) {
            System.err.println("❌ Error al buscar movimiento: " + e.getMessage());
            throw e;
        }
    }

    public List<Movimiento> listarPorExpediente(Integer expedienteId) throws SQLException {
        String sql = """
            SELECT * FROM movimientos 
            WHERE expediente_id = ?
            ORDER BY fecha DESC, fecha_creacion DESC
        """;

        List<Movimiento> movimientos = new ArrayList<>();

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, expedienteId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    movimientos.add(mapearMovimiento(rs));
                }
            }

            return movimientos;

        } catch (SQLException e) {
            System.err.println("❌ Error al listar movimientos: " + e.getMessage());
            throw e;
        }
    }

    public List<Movimiento> buscarPorTipo(Integer expedienteId, TipoMovimiento tipo) throws SQLException {
        String sql = """
            SELECT * FROM movimientos 
            WHERE expediente_id = ? AND tipo = ?
            ORDER BY fecha DESC
        """;

        List<Movimiento> movimientos = new ArrayList<>();

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, expedienteId);
            ps.setString(2, tipo.name());

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    movimientos.add(mapearMovimiento(rs));
                }
            }

            return movimientos;

        } catch (SQLException e) {
            System.err.println("❌ Error al buscar por tipo: " + e.getMessage());
            throw e;
        }
    }

    public List<Movimiento> buscarPorRangoFechas(Integer expedienteId, LocalDate desde, LocalDate hasta)
            throws SQLException {
        String sql = """
            SELECT * FROM movimientos 
            WHERE expediente_id = ? AND fecha BETWEEN ? AND ?
            ORDER BY fecha DESC
        """;

        List<Movimiento> movimientos = new ArrayList<>();

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, expedienteId);
            ps.setString(2, desde.toString());
            ps.setString(3, hasta.toString());

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    movimientos.add(mapearMovimiento(rs));
                }
            }

            return movimientos;

        } catch (SQLException e) {
            System.err.println("❌ Error al buscar por rango: " + e.getMessage());
            throw e;
        }
    }

    public Movimiento obtenerUltimo(Integer expedienteId) throws SQLException {
        String sql = """
            SELECT * FROM movimientos 
            WHERE expediente_id = ?
            ORDER BY fecha DESC, fecha_creacion DESC
            LIMIT 1
        """;

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, expedienteId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapearMovimiento(rs);
                }
            }

            return null;

        } catch (SQLException e) {
            System.err.println("❌ Error al obtener último movimiento: " + e.getMessage());
            throw e;
        }
    }

    // ==================== UPDATE ====================

    public Movimiento actualizar(Movimiento movimiento) throws SQLException {
        String sql = """
            UPDATE movimientos SET
                fecha = ?,
                tipo = ?,
                descripcion = ?,
                cuaderno = ?,
                foja = ?,
                observaciones = ?
            WHERE id = ?
        """;

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, movimiento.getFechaAsString());
            ps.setString(2, movimiento.getTipo().name());
            ps.setString(3, movimiento.getDescripcion());
            ps.setString(4, movimiento.getCuaderno());

            if (movimiento.getFoja() != null) {
                ps.setInt(5, movimiento.getFoja());
            } else {
                ps.setNull(5, Types.INTEGER);
            }

            ps.setString(6, movimiento.getObservaciones());
            ps.setInt(7, movimiento.getId());

            int affectedRows = ps.executeUpdate();

            if (affectedRows == 0) {
                throw new SQLException("No se pudo actualizar el movimiento");
            }

            return movimiento;

        } catch (SQLException e) {
            System.err.println("❌ Error al actualizar movimiento: " + e.getMessage());
            throw e;
        }
    }

    // ==================== DELETE ====================

    public void eliminar(Integer id) throws SQLException {
        String sql = "DELETE FROM movimientos WHERE id = ?";

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);

            int affectedRows = ps.executeUpdate();

            if (affectedRows == 0) {
                throw new SQLException("No se pudo eliminar el movimiento");
            }

        } catch (SQLException e) {
            System.err.println("❌ Error al eliminar movimiento: " + e.getMessage());
            throw e;
        }
    }

    // ==================== ESTADÍSTICAS ====================

    public int contarPorExpediente(Integer expedienteId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM movimientos WHERE expediente_id = ?";

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, expedienteId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }

            return 0;

        } catch (SQLException e) {
            System.err.println("❌ Error al contar movimientos: " + e.getMessage());
            throw e;
        }
    }

    // ==================== MAPEO ====================

    private Movimiento mapearMovimiento(ResultSet rs) throws SQLException {
        Movimiento mov = new Movimiento();

        mov.setId(rs.getInt("id"));
        mov.setExpedienteId(rs.getInt("expediente_id"));
        mov.setFechaFromString(rs.getString("fecha"));
        mov.setTipo(TipoMovimiento.fromString(rs.getString("tipo")));
        mov.setDescripcion(rs.getString("descripcion"));
        mov.setCuaderno(rs.getString("cuaderno"));

        int foja = rs.getInt("foja");
        if (!rs.wasNull()) {
            mov.setFoja(foja);
        }

        mov.setObservaciones(rs.getString("observaciones"));
        mov.setUsuarioId(rs.getInt("usuario_id"));
        mov.setFechaCreacionFromString(rs.getString("fecha_creacion"));

        return mov;
    }
}