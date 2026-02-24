package com.juridix.db;

import com.juridix.model.PagoCuota;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class PagoCuotaDAO {

    public PagoCuota guardar(PagoCuota pago) throws SQLException {
        String sql = """
            INSERT INTO pagos_cuota (
                cuota_id, monto, fecha_pago, forma_pago, numero_cuota,
                referencia, observaciones, usuario_id
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
        """;

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setInt(1, pago.getCuotaId());
            ps.setDouble(2, pago.getMonto());
            ps.setString(3, pago.getFechaPago().toString());
            ps.setString(4, pago.getFormaPago());

            if (pago.getNumeroCuota() != null) {
                ps.setInt(5, pago.getNumeroCuota());
            } else {
                ps.setNull(5, Types.INTEGER);
            }

            ps.setString(6, pago.getReferencia());
            ps.setString(7, pago.getObservaciones());

            if (pago.getUsuarioId() != null) {
                ps.setInt(8, pago.getUsuarioId());
            } else {
                ps.setNull(8, Types.INTEGER);
            }

            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    pago.setId(rs.getInt(1));
                }
            }

            return pago;
        }
    }

    public List<PagoCuota> listarPorCuota(Integer cuotaId) throws SQLException {
        String sql = "SELECT * FROM pagos_cuota WHERE cuota_id = ? ORDER BY fecha_pago DESC";
        List<PagoCuota> pagos = new ArrayList<>();

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, cuotaId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    pagos.add(mapearPago(rs));
                }
            }
        }

        return pagos;
    }

    public void eliminar(Integer id) throws SQLException {
        String sql = "DELETE FROM pagos_cuota WHERE id = ?";

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    private PagoCuota mapearPago(ResultSet rs) throws SQLException {
        PagoCuota pago = new PagoCuota();

        pago.setId(rs.getInt("id"));
        pago.setCuotaId(rs.getInt("cuota_id"));
        pago.setMonto(rs.getDouble("monto"));

        String fecha = rs.getString("fecha_pago");
        if (fecha != null) {
            pago.setFechaPago(java.time.LocalDate.parse(fecha));
        }

        pago.setFormaPago(rs.getString("forma_pago"));

        Integer numCuota = (Integer) rs.getObject("numero_cuota");
        pago.setNumeroCuota(numCuota);

        pago.setReferencia(rs.getString("referencia"));
        pago.setObservaciones(rs.getString("observaciones"));

        Integer usuarioId = (Integer) rs.getObject("usuario_id");
        pago.setUsuarioId(usuarioId);

        return pago;
    }
}