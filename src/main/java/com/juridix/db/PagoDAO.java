package com.juridix.db;

import com.juridix.db.Database;
import com.juridix.model.Pago;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class PagoDAO {

    // ==================== CREATE ====================

    public Pago guardar(Pago pago) throws SQLException {
        String sql = """
            INSERT INTO pagos (
                expediente_id, cliente_id, monto, fecha, forma_pago,
                referencia, concepto, observaciones, usuario_id
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setInt(1, pago.getExpedienteId());

            if (pago.getClienteId() != null) {
                ps.setInt(2, pago.getClienteId());
            } else {
                ps.setNull(2, Types.INTEGER);
            }

            ps.setDouble(3, pago.getMonto());
            ps.setString(4, pago.getFechaAsString());
            ps.setString(5, pago.getFormaPago());
            ps.setString(6, pago.getReferencia());
            ps.setString(7, pago.getConcepto());
            ps.setString(8, pago.getObservaciones());

            if (pago.getUsuarioId() != null) {
                ps.setInt(9, pago.getUsuarioId());
            } else {
                ps.setNull(9, Types.INTEGER);
            }

            ps.executeUpdate();

            try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    pago.setId(generatedKeys.getInt(1));
                }
            }

            return pago;

        } catch (SQLException e) {
            System.err.println("❌ Error al guardar pago: " + e.getMessage());
            throw e;
        }
    }

    // ==================== READ ====================

    public Optional<Pago> buscarPorId(Integer id) throws SQLException {
        String sql = "SELECT * FROM pagos WHERE id = ?";

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapearPago(rs));
                }
            }

            return Optional.empty();

        } catch (SQLException e) {
            System.err.println("❌ Error al buscar pago: " + e.getMessage());
            throw e;
        }
    }

    public List<Pago> listarPorExpediente(Integer expedienteId) throws SQLException {
        String sql = """
            SELECT * FROM pagos 
            WHERE expediente_id = ?
            ORDER BY fecha DESC
        """;

        List<Pago> pagos = new ArrayList<>();

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, expedienteId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    pagos.add(mapearPago(rs));
                }
            }

            return pagos;

        } catch (SQLException e) {
            System.err.println("❌ Error al listar pagos: " + e.getMessage());
            throw e;
        }
    }

    public List<Pago> listarPorCliente(Integer clienteId) throws SQLException {
        String sql = """
            SELECT * FROM pagos 
            WHERE cliente_id = ?
            ORDER BY fecha DESC
        """;

        List<Pago> pagos = new ArrayList<>();

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, clienteId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    pagos.add(mapearPago(rs));
                }
            }

            return pagos;

        } catch (SQLException e) {
            System.err.println("❌ Error al listar pagos por cliente: " + e.getMessage());
            throw e;
        }
    }

    // ==================== UPDATE ====================

    public Pago actualizar(Pago pago) throws SQLException {
        String sql = """
            UPDATE pagos SET
                monto = ?,
                fecha = ?,
                forma_pago = ?,
                referencia = ?,
                concepto = ?,
                observaciones = ?
            WHERE id = ?
        """;

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setDouble(1, pago.getMonto());
            ps.setString(2, pago.getFechaAsString());
            ps.setString(3, pago.getFormaPago());
            ps.setString(4, pago.getReferencia());
            ps.setString(5, pago.getConcepto());
            ps.setString(6, pago.getObservaciones());
            ps.setInt(7, pago.getId());

            ps.executeUpdate();
            return pago;

        } catch (SQLException e) {
            System.err.println("❌ Error al actualizar pago: " + e.getMessage());
            throw e;
        }
    }

    // ==================== DELETE ====================

    public void eliminar(Integer id) throws SQLException {
        String sql = "DELETE FROM pagos WHERE id = ?";

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);
            ps.executeUpdate();

        } catch (SQLException e) {
            System.err.println("❌ Error al eliminar pago: " + e.getMessage());
            throw e;
        }
    }

    // ==================== ESTADÍSTICAS ====================

    public Double calcularTotalPorExpediente(Integer expedienteId) throws SQLException {
        String sql = """
            SELECT COALESCE(SUM(monto), 0) 
            FROM pagos 
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

    public Double calcularTotalPorCliente(Integer clienteId) throws SQLException {
        String sql = """
            SELECT COALESCE(SUM(monto), 0) 
            FROM pagos 
            WHERE cliente_id = ?
        """;

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, clienteId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getDouble(1);
                }
            }

            return 0.0;

        } catch (SQLException e) {
            System.err.println("❌ Error al calcular total por cliente: " + e.getMessage());
            throw e;
        }
    }

    // ==================== MAPEO ====================

    private Pago mapearPago(ResultSet rs) throws SQLException {
        Pago pago = new Pago();

        pago.setId(rs.getInt("id"));
        pago.setExpedienteId(rs.getInt("expediente_id"));

        int clienteId = rs.getInt("cliente_id");
        if (!rs.wasNull()) {
            pago.setClienteId(clienteId);
        }

        pago.setMonto(rs.getDouble("monto"));
        pago.setFechaFromString(rs.getString("fecha"));
        pago.setFormaPago(rs.getString("forma_pago"));
        pago.setReferencia(rs.getString("referencia"));
        pago.setConcepto(rs.getString("concepto"));
        pago.setObservaciones(rs.getString("observaciones"));

        int usuarioId = rs.getInt("usuario_id");
        if (!rs.wasNull()) {
            pago.setUsuarioId(usuarioId);
        }

        pago.setFechaCreacionFromString(rs.getString("fecha_creacion"));

        return pago;
    }
}