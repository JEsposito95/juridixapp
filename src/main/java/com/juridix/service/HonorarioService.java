package com.juridix.service;

import com.juridix.db.HonorarioDAO;
import com.juridix.model.Honorario;
import com.juridix.model.TipoHonorario;
import com.juridix.model.EstadoHonorario;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public class HonorarioService {

    private final HonorarioDAO honorarioDAO;

    public HonorarioService() {
        this.honorarioDAO = new HonorarioDAO();
    }

    // ==================== CREATE ====================

    public Honorario crearHonorario(Honorario honorario) throws SQLException {
        validarHonorario(honorario);

        // Calcular monto según tipo
        calcularMonto(honorario);

        return honorarioDAO.guardar(honorario);
    }

    // ==================== READ ====================

    public Optional<Honorario> buscarPorId(Integer id) throws SQLException {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("El ID debe ser válido");
        }
        return honorarioDAO.buscarPorId(id);
    }

    public List<Honorario> listarPorExpediente(Integer expedienteId) throws SQLException {
        if (expedienteId == null || expedienteId <= 0) {
            throw new IllegalArgumentException("El ID del expediente debe ser válido");
        }
        return honorarioDAO.listarPorExpediente(expedienteId);
    }

    public List<Honorario> listarPendientes() throws SQLException {
        return honorarioDAO.listarPorEstado(EstadoHonorario.PENDIENTE);
    }

    // ==================== UPDATE ====================

    public Honorario actualizarHonorario(Honorario honorario) throws SQLException {
        validarHonorario(honorario);

        if (honorario.getId() == null) {
            throw new IllegalArgumentException("El honorario debe tener un ID");
        }

        calcularMonto(honorario);

        return honorarioDAO.actualizar(honorario);
    }

    public void marcarComoCobrado(Integer id) throws SQLException {
        Optional<Honorario> honorario = honorarioDAO.buscarPorId(id);
        if (honorario.isPresent()) {
            Honorario h = honorario.get();
            h.setEstado(EstadoHonorario.COBRADO);
            honorarioDAO.actualizar(h);
        }
    }

    // ==================== DELETE ====================

    public void eliminarHonorario(Integer id) throws SQLException {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("El ID debe ser válido");
        }
        honorarioDAO.eliminar(id);
    }

    // ==================== CÁLCULOS ====================

    public Double calcularTotalPorExpediente(Integer expedienteId) throws SQLException {
        if (expedienteId == null || expedienteId <= 0) {
            throw new IllegalArgumentException("El ID del expediente debe ser válido");
        }
        return honorarioDAO.calcularTotalPorExpediente(expedienteId);
    }

    private void calcularMonto(Honorario honorario) {
        if (honorario.getTipo() == TipoHonorario.MONTO_FIJO && honorario.getMontoFijo() != null) {
            honorario.setMontoCalculado(honorario.getMontoFijo());
        }
        // Para PORCENTAJE y REGULACION_JUDICIAL, el monto se calcula manualmente o se ingresa
    }

    // ==================== VALIDACIONES ====================

    private void validarHonorario(Honorario honorario) {
        if (honorario == null) {
            throw new IllegalArgumentException("El honorario no puede ser nulo");
        }

        if (honorario.getExpedienteId() == null) {
            throw new IllegalArgumentException("Debe especificar un expediente");
        }

        if (honorario.getTipo() == null) {
            throw new IllegalArgumentException("Debe especificar el tipo de honorario");
        }

        if (honorario.getTipo() == TipoHonorario.PORCENTAJE && honorario.getPorcentaje() == null) {
            throw new IllegalArgumentException("Debe especificar el porcentaje");
        }

        if (honorario.getTipo() == TipoHonorario.MONTO_FIJO && honorario.getMontoFijo() == null) {
            throw new IllegalArgumentException("Debe especificar el monto fijo");
        }

        if (honorario.getPorcentaje() != null && (honorario.getPorcentaje() < 0 || honorario.getPorcentaje() > 100)) {
            throw new IllegalArgumentException("El porcentaje debe estar entre 0 y 100");
        }
    }
}