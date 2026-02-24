package com.juridix.db;

import com.juridix.model.Cuota;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class CuotaDAO {

    public Cuota guardar(Cuota cuota) throws SQLException {
        String sql = """
            INSERT INTO cuotas (
                expediente_id, monto_total_acordado, monto_pagado, 
                cantidad_cuotas_planificadas, monto_por_cuota, observaciones,
                fecha_acuerdo, estado, usuario_id
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setInt(1, cuota.getExpedienteId());
            ps.setDouble(2, cuota.getMontoTotalAcordado());
            ps.setDouble(3, cuota.getMontoPagado() != null ? cuota.getMontoPagado() : 0);

            if (cuota.getCantidadCuotasPlanificadas() != null) {
                ps.setInt(4, cuota.getCantidadCuotasPlanificadas());
            } else {
                ps.setNull(4, Types.INTEGER);
            }

            if (cuota.getMontoPorCuota() != null) {
                ps.setDouble(5, cuota.getMontoPorCuota());
            } else {
                ps.setNull(5, Types.DOUBLE);
            }

            ps.setString(6, cuota.getObservaciones());
            ps.setString(7, cuota.getFechaAcuerdo().toString());
            ps.setString(8, cuota.getEstado());

            if (cuota.getUsuarioId() != null) {
                ps.setInt(9, cuota.getUsuarioId());
            } else {
                ps.setNull(9, Types.INTEGER);
            }

            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    cuota.setId(rs.getInt(1));
                }
            }

            return cuota;
        }
    }

    public Optional<Cuota> buscarPorId(Integer id) throws SQLException {
        String sql = "SELECT * FROM cuotas WHERE id = ?";

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapearCuota(rs));
                }
            }
        }

        return Optional.empty();
    }

    public List<Cuota> listarPorExpediente(Integer expedienteId) throws SQLException {
        String sql = "SELECT * FROM cuotas WHERE expediente_id = ? ORDER BY fecha_acuerdo DESC";
        List<Cuota> cuotas = new ArrayList<>();

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, expedienteId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    cuotas.add(mapearCuota(rs));
                }
            }
        }

        return cuotas;
    }

    public Cuota actualizar(Cuota cuota) throws SQLException {
        String sql = """
            UPDATE cuotas SET
                monto_pagado = ?, estado = ?, observaciones = ?
            WHERE id = ?
        """;

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setDouble(1, cuota.getMontoPagado());
            ps.setString(2, cuota.getEstado());
            ps.setString(3, cuota.getObservaciones());
            ps.setInt(4, cuota.getId());

            ps.executeUpdate();
            return cuota;
        }
    }

    public void eliminar(Integer id) throws SQLException {
        String sql = "DELETE FROM cuotas WHERE id = ?";

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    private Cuota mapearCuota(ResultSet rs) throws SQLException {
        Cuota cuota = new Cuota();

        cuota.setId(rs.getInt("id"));
        cuota.setExpedienteId(rs.getInt("expediente_id"));
        cuota.setMontoTotalAcordado(rs.getDouble("monto_total_acordado"));
        cuota.setMontoPagado(rs.getDouble("monto_pagado"));

        Integer cantCuotas = (Integer) rs.getObject("cantidad_cuotas_planificadas");
        cuota.setCantidadCuotasPlanificadas(cantCuotas);

        Double montoCuota = (Double) rs.getObject("monto_por_cuota");
        cuota.setMontoPorCuota(montoCuota);

        cuota.setObservaciones(rs.getString("observaciones"));

        String fechaAcuerdo = rs.getString("fecha_acuerdo");
        if (fechaAcuerdo != null) {
            cuota.setFechaAcuerdo(java.time.LocalDate.parse(fechaAcuerdo));
        }

        cuota.setEstado(rs.getString("estado"));

        Integer usuarioId = (Integer) rs.getObject("usuario_id");
        cuota.setUsuarioId(usuarioId);

        return cuota;
    }
}