package com.juridix.service;

import com.juridix.db.CuotaDAO;
import com.juridix.db.PagoCuotaDAO;
import com.juridix.model.Cuota;
import com.juridix.model.PagoCuota;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public class CuotaService {

    private final CuotaDAO cuotaDAO;
    private final PagoCuotaDAO pagoCuotaDAO;

    public CuotaService() {
        this.cuotaDAO = new CuotaDAO();
        this.pagoCuotaDAO = new PagoCuotaDAO();
    }

    // ==================== CUOTAS ====================

    public Cuota crearCuota(Cuota cuota) throws SQLException {
        validarCuota(cuota);
        return cuotaDAO.guardar(cuota);
    }

    public Optional<Cuota> buscarCuotaPorId(Integer id) throws SQLException {
        return cuotaDAO.buscarPorId(id);
    }

    public List<Cuota> listarCuotasPorExpediente(Integer expedienteId) throws SQLException {
        return cuotaDAO.listarPorExpediente(expedienteId);
    }

    public Cuota actualizarCuota(Cuota cuota) throws SQLException {
        return cuotaDAO.actualizar(cuota);
    }

    public void eliminarCuota(Integer id) throws SQLException {
        cuotaDAO.eliminar(id);
    }

    // ==================== PAGOS DE CUOTA ====================

    public PagoCuota registrarPagoCuota(PagoCuota pago) throws SQLException {
        validarPagoCuota(pago);

        // Guardar el pago
        PagoCuota pagoGuardado = pagoCuotaDAO.guardar(pago);

        // Actualizar el monto pagado en la cuota
        Optional<Cuota> cuotaOpt = cuotaDAO.buscarPorId(pago.getCuotaId());
        if (cuotaOpt.isPresent()) {
            Cuota cuota = cuotaOpt.get();
            double nuevoMontoPagado = cuota.getMontoPagado() + pago.getMonto();
            cuota.setMontoPagado(nuevoMontoPagado);

            // Si se completó el pago, cambiar estado
            if (nuevoMontoPagado >= cuota.getMontoTotalAcordado()) {
                cuota.setEstado("COMPLETADO");
            }

            cuotaDAO.actualizar(cuota);
        }

        return pagoGuardado;
    }

    public List<PagoCuota> listarPagosDeCuota(Integer cuotaId) throws SQLException {
        return pagoCuotaDAO.listarPorCuota(cuotaId);
    }

    public void eliminarPagoCuota(Integer id, Integer cuotaId, Double monto) throws SQLException {
        // Eliminar el pago
        pagoCuotaDAO.eliminar(id);

        // Actualizar el monto pagado en la cuota (restar)
        Optional<Cuota> cuotaOpt = cuotaDAO.buscarPorId(cuotaId);
        if (cuotaOpt.isPresent()) {
            Cuota cuota = cuotaOpt.get();
            double nuevoMontoPagado = cuota.getMontoPagado() - monto;
            cuota.setMontoPagado(Math.max(0, nuevoMontoPagado));
            cuota.setEstado("ACTIVO"); // Volver a activo
            cuotaDAO.actualizar(cuota);
        }
    }

    // ==================== VALIDACIONES ====================

    private void validarCuota(Cuota cuota) {
        if (cuota.getExpedienteId() == null) {
            throw new IllegalArgumentException("El expediente es obligatorio");
        }
        if (cuota.getMontoTotalAcordado() == null || cuota.getMontoTotalAcordado() <= 0) {
            throw new IllegalArgumentException("El monto total debe ser mayor a cero");
        }
        if (cuota.getFechaAcuerdo() == null) {
            throw new IllegalArgumentException("La fecha de acuerdo es obligatoria");
        }
    }

    private void validarPagoCuota(PagoCuota pago) {
        if (pago.getCuotaId() == null) {
            throw new IllegalArgumentException("La cuota es obligatoria");
        }
        if (pago.getMonto() == null || pago.getMonto() <= 0) {
            throw new IllegalArgumentException("El monto debe ser mayor a cero");
        }
        if (pago.getFechaPago() == null) {
            throw new IllegalArgumentException("La fecha de pago es obligatoria");
        }
    }
}