package com.juridix.service;

import com.juridix.db.PagoDAO;
import com.juridix.model.Pago;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public class PagoService {

    private final PagoDAO pagoDAO;

    public PagoService() {
        this.pagoDAO = new PagoDAO();
    }

    // ==================== CREATE ====================

    public Pago crearPago(Pago pago) throws SQLException {
        validarPago(pago);
        return pagoDAO.guardar(pago);
    }

    // ==================== READ ====================

    public Optional<Pago> buscarPorId(Integer id) throws SQLException {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("El ID debe ser válido");
        }
        return pagoDAO.buscarPorId(id);
    }

    public List<Pago> listarPorExpediente(Integer expedienteId) throws SQLException {
        if (expedienteId == null || expedienteId <= 0) {
            throw new IllegalArgumentException("El ID del expediente debe ser válido");
        }
        return pagoDAO.listarPorExpediente(expedienteId);
    }

    public List<Pago> listarPorCliente(Integer clienteId) throws SQLException {
        if (clienteId == null || clienteId <= 0) {
            throw new IllegalArgumentException("El ID del cliente debe ser válido");
        }
        return pagoDAO.listarPorCliente(clienteId);
    }

    // ==================== UPDATE ====================

    public Pago actualizarPago(Pago pago) throws SQLException {
        validarPago(pago);

        if (pago.getId() == null) {
            throw new IllegalArgumentException("El pago debe tener un ID");
        }

        return pagoDAO.actualizar(pago);
    }

    // ==================== DELETE ====================

    public void eliminarPago(Integer id) throws SQLException {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("El ID debe ser válido");
        }
        pagoDAO.eliminar(id);
    }

    // ==================== CÁLCULOS ====================

    public Double calcularTotalPorExpediente(Integer expedienteId) throws SQLException {
        if (expedienteId == null || expedienteId <= 0) {
            throw new IllegalArgumentException("El ID del expediente debe ser válido");
        }
        return pagoDAO.calcularTotalPorExpediente(expedienteId);
    }

    public Double calcularTotalPorCliente(Integer clienteId) throws SQLException {
        if (clienteId == null || clienteId <= 0) {
            throw new IllegalArgumentException("El ID del cliente debe ser válido");
        }
        return pagoDAO.calcularTotalPorCliente(clienteId);
    }

    // ==================== VALIDACIONES ====================

    private void validarPago(Pago pago) {
        if (pago == null) {
            throw new IllegalArgumentException("El pago no puede ser nulo");
        }

        if (pago.getExpedienteId() == null) {
            throw new IllegalArgumentException("Debe especificar un expediente");
        }

        if (pago.getMonto() == null || pago.getMonto() <= 0) {
            throw new IllegalArgumentException("El monto debe ser mayor a cero");
        }

        if (pago.getFecha() == null) {
            throw new IllegalArgumentException("La fecha es obligatoria");
        }

        if (pago.getFecha().isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("La fecha no puede ser futura");
        }
    }
}