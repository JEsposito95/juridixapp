package com.juridix.service;

import com.juridix.db.MovimientoDAO;
import com.juridix.model.Movimiento;
import com.juridix.model.TipoMovimiento;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public class MovimientoService {

    private final MovimientoDAO movimientoDAO;

    public MovimientoService() {
        this.movimientoDAO = new MovimientoDAO();
    }

    public MovimientoService(MovimientoDAO movimientoDAO) {
        this.movimientoDAO = movimientoDAO;
    }

    // ==================== CREATE ====================

    public Movimiento crearMovimiento(Movimiento movimiento) throws SQLException {
        validarMovimiento(movimiento);
        return movimientoDAO.guardar(movimiento);
    }

    // ==================== READ ====================

    public Optional<Movimiento> buscarPorId(Integer id) throws SQLException {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("El ID debe ser un número positivo");
        }
        return movimientoDAO.buscarPorId(id);
    }

    public List<Movimiento> listarPorExpediente(Integer expedienteId) throws SQLException {
        if (expedienteId == null || expedienteId <= 0) {
            throw new IllegalArgumentException("El ID del expediente debe ser válido");
        }
        return movimientoDAO.listarPorExpediente(expedienteId);
    }

    public List<Movimiento> buscarPorTipo(Integer expedienteId, TipoMovimiento tipo) throws SQLException {
        if (expedienteId == null || expedienteId <= 0) {
            throw new IllegalArgumentException("El ID del expediente debe ser válido");
        }
        if (tipo == null) {
            throw new IllegalArgumentException("El tipo no puede ser nulo");
        }
        return movimientoDAO.buscarPorTipo(expedienteId, tipo);
    }

    public List<Movimiento> buscarPorRangoFechas(Integer expedienteId, LocalDate desde, LocalDate hasta)
            throws SQLException {
        if (expedienteId == null || expedienteId <= 0) {
            throw new IllegalArgumentException("El ID del expediente debe ser válido");
        }
        if (desde == null || hasta == null) {
            throw new IllegalArgumentException("Las fechas no pueden ser nulas");
        }
        if (desde.isAfter(hasta)) {
            throw new IllegalArgumentException("La fecha inicial no puede ser posterior a la final");
        }
        return movimientoDAO.buscarPorRangoFechas(expedienteId, desde, hasta);
    }

    public Movimiento obtenerUltimoMovimiento(Integer expedienteId) throws SQLException {
        if (expedienteId == null || expedienteId <= 0) {
            throw new IllegalArgumentException("El ID del expediente debe ser válido");
        }
        return movimientoDAO.obtenerUltimo(expedienteId);
    }

    // ==================== UPDATE ====================

    public Movimiento actualizarMovimiento(Movimiento movimiento) throws SQLException {
        validarMovimiento(movimiento);

        if (movimiento.getId() == null) {
            throw new IllegalArgumentException("El movimiento debe tener un ID para actualizarse");
        }

        Optional<Movimiento> existente = movimientoDAO.buscarPorId(movimiento.getId());
        if (existente.isEmpty()) {
            throw new IllegalArgumentException("No existe un movimiento con el ID: " + movimiento.getId());
        }

        return movimientoDAO.actualizar(movimiento);
    }

    // ==================== DELETE ====================

    public void eliminarMovimiento(Integer id) throws SQLException {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("El ID debe ser un número positivo");
        }

        Optional<Movimiento> movimiento = movimientoDAO.buscarPorId(id);
        if (movimiento.isEmpty()) {
            throw new IllegalArgumentException("No existe un movimiento con el ID: " + id);
        }

        movimientoDAO.eliminar(id);
    }

    // ==================== ESTADÍSTICAS ====================

    public int contarMovimientos(Integer expedienteId) throws SQLException {
        if (expedienteId == null || expedienteId <= 0) {
            throw new IllegalArgumentException("El ID del expediente debe ser válido");
        }
        return movimientoDAO.contarPorExpediente(expedienteId);
    }

    // ==================== VALIDACIONES ====================

    private void validarMovimiento(Movimiento movimiento) {
        if (movimiento == null) {
            throw new IllegalArgumentException("El movimiento no puede ser nulo");
        }

        if (movimiento.getExpedienteId() == null) {
            throw new IllegalArgumentException("El movimiento debe estar asociado a un expediente");
        }

        if (movimiento.getFecha() == null) {
            throw new IllegalArgumentException("La fecha es obligatoria");
        }

        if (movimiento.getFecha().isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("La fecha no puede ser futura");
        }

        if (movimiento.getTipo() == null) {
            throw new IllegalArgumentException("El tipo de movimiento es obligatorio");
        }

        if (movimiento.getDescripcion() == null || movimiento.getDescripcion().trim().isEmpty()) {
            throw new IllegalArgumentException("La descripción es obligatoria");
        }

        if (movimiento.getUsuarioId() == null) {
            throw new IllegalArgumentException("El movimiento debe tener un usuario asignado");
        }

        if (movimiento.getFoja() != null && movimiento.getFoja() < 0) {
            throw new IllegalArgumentException("El número de foja no puede ser negativo");
        }
    }
}