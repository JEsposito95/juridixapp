package com.juridix.db;

import com.juridix.db.Database;
import com.juridix.model.Gasto;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class GastoDAO {

    // ==================== CREATE ====================

    public Gasto guardar(Gasto gasto) throws SQLException {
        String sql = """
            INSERT INTO gastos (
                expediente_id, concepto, monto, fecha, categoria,
                comprobante, observaciones, usuario_id
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
        """;

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setInt(1, gasto.getExpedienteId());
            ps.setString(2, gasto.getConcepto());
            ps.setDouble(3, gasto.getMonto());
            ps.setString(4, gasto.getFechaAsString());
            ps.setString(5, gasto.getCategoria());
            ps.setString(6, gasto.getComprobante());
            ps.setString(7, gasto.getObservaciones());

            if (gasto.getUsuarioId() != null) {
                ps.setInt(8, gasto.getUsuarioId());
            } else {
                ps.setNull(8, Types.INTEGER);
            }

            ps.executeUpdate();

            try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    gasto.setId(generatedKeys.getInt(1));
                }
            }

            return gasto;

        } catch (SQLException e) {
            System.err.println("❌ Error al guardar gasto: " + e.getMessage());
            throw e;
        }
    }

    // ==================== READ ====================

    public Optional<Gasto> buscarPorId(Integer id) throws SQLException {
        String sql = "SELECT * FROM gastos WHERE id = ?";

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapearGasto(rs));
                }
            }

            return Optional.empty();

        } catch (SQLException e) {
            System.err.println("❌ Error al buscar gasto: " + e.getMessage());
            throw e;
        }
    }

    public List<Gasto> listarPorExpediente(Integer expedienteId) throws SQLException {
        String sql = """
            SELECT * FROM gastos 
            WHERE expediente_id = ?
            ORDER BY fecha DESC
        """;

        List<Gasto> gastos = new ArrayList<>();

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, expedienteId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    gastos.add(mapearGasto(rs));
                }
            }

            return gastos;

        } catch (SQLException e) {
            System.err.println("❌ Error al listar gastos: " + e.getMessage());
            throw e;
        }
    }

    // ==================== UPDATE ====================

    public Gasto actualizar(Gasto gasto) throws SQLException {
        String sql = """
            UPDATE gastos SET
                concepto = ?,
                monto = ?,
                fecha = ?,
                categoria = ?,
                comprobante = ?,
                observaciones = ?
            WHERE id = ?
        """;

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, gasto.getConcepto());
            ps.setDouble(2, gasto.getMonto());
            ps.setString(3, gasto.getFechaAsString());
            ps.setString(4, gasto.getCategoria());
            ps.setString(5, gasto.getComprobante());
            ps.setString(6, gasto.getObservaciones());
            ps.setInt(7, gasto.getId());

            ps.executeUpdate();
            return gasto;

        } catch (SQLException e) {
            System.err.println("❌ Error al actualizar gasto: " + e.getMessage());
            throw e;
        }
    }

    // ==================== DELETE ====================

    public void eliminar(Integer id) throws SQLException {
        String sql = "DELETE FROM gastos WHERE id = ?";

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);
            ps.executeUpdate();

        } catch (SQLException e) {
            System.err.println("❌ Error al eliminar gasto: " + e.getMessage());
            throw e;
        }
    }

    // ==================== ESTADÍSTICAS ====================

    public Double calcularTotalPorExpediente(Integer expedienteId) throws SQLException {
        String sql = """
            SELECT COALESCE(SUM(monto), 0) 
            FROM gastos 
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

    private Gasto mapearGasto(ResultSet rs) throws SQLException {
        Gasto gasto = new Gasto();

        gasto.setId(rs.getInt("id"));
        gasto.setExpedienteId(rs.getInt("expediente_id"));
        gasto.setConcepto(rs.getString("concepto"));
        gasto.setMonto(rs.getDouble("monto"));
        gasto.setFechaFromString(rs.getString("fecha"));
        gasto.setCategoria(rs.getString("categoria"));
        gasto.setComprobante(rs.getString("comprobante"));
        gasto.setObservaciones(rs.getString("observaciones"));

        int usuarioId = rs.getInt("usuario_id");
        if (!rs.wasNull()) {
            gasto.setUsuarioId(usuarioId);
        }

        gasto.setFechaCreacionFromString(rs.getString("fecha_creacion"));

        return gasto;
    }
}