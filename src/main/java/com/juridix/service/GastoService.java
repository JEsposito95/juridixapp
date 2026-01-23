package com.juridix.service;

import com.juridix.db.GastoDAO;
import com.juridix.model.Gasto;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public class GastoService {

    private final GastoDAO gastoDAO;

    public GastoService() {
        this.gastoDAO = new GastoDAO();
    }

    // ==================== CREATE ====================

    public Gasto crearGasto(Gasto gasto) throws SQLException {
        validarGasto(gasto);
        return gastoDAO.guardar(gasto);
    }

    // ==================== READ ====================

    public Optional<Gasto> buscarPorId(Integer id) throws SQLException {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("El ID debe ser válido");
        }
        return gastoDAO.buscarPorId(id);
    }

    public List<Gasto> listarPorExpediente(Integer expedienteId) throws SQLException {
        if (expedienteId == null || expedienteId <= 0) {
            throw new IllegalArgumentException("El ID del expediente debe ser válido");
        }
        return gastoDAO.listarPorExpediente(expedienteId);
    }

    // ==================== UPDATE ====================

    public Gasto actualizarGasto(Gasto gasto) throws SQLException {
        validarGasto(gasto);

        if (gasto.getId() == null) {
            throw new IllegalArgumentException("El gasto debe tener un ID");
        }

        return gastoDAO.actualizar(gasto);
    }

    // ==================== DELETE ====================

    public void eliminarGasto(Integer id) throws SQLException {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("El ID debe ser válido");
        }
        gastoDAO.eliminar(id);
    }

    // ==================== CÁLCULOS ====================

    public Double calcularTotalPorExpediente(Integer expedienteId) throws SQLException {
        if (expedienteId == null || expedienteId <= 0) {
            throw new IllegalArgumentException("El ID del expediente debe ser válido");
        }
        return gastoDAO.calcularTotalPorExpediente(expedienteId);
    }

    // ==================== VALIDACIONES ====================

    private void validarGasto(Gasto gasto) {
        if (gasto == null) {
            throw new IllegalArgumentException("El gasto no puede ser nulo");
        }

        if (gasto.getExpedienteId() == null) {
            throw new IllegalArgumentException("Debe especificar un expediente");
        }

        if (gasto.getConcepto() == null || gasto.getConcepto().trim().isEmpty()) {
            throw new IllegalArgumentException("El concepto es obligatorio");
        }

        if (gasto.getMonto() == null || gasto.getMonto() <= 0) {
            throw new IllegalArgumentException("El monto debe ser mayor a cero");
        }

        if (gasto.getFecha() == null) {
            throw new IllegalArgumentException("La fecha es obligatoria");
        }

        if (gasto.getFecha().isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("La fecha no puede ser futura");
        }
    }
}