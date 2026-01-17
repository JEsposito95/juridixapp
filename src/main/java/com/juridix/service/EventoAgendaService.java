package com.juridix.service;

import com.juridix.db.EventoAgendaDAO;
import com.juridix.model.EventoAgenda;
import com.juridix.model.EstadoEvento;
import com.juridix.model.TipoEvento;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class EventoAgendaService {

    private final EventoAgendaDAO eventoDAO;

    public EventoAgendaService() {
        this.eventoDAO = new EventoAgendaDAO();
    }

    public EventoAgendaService(EventoAgendaDAO eventoDAO) {
        this.eventoDAO = eventoDAO;
    }

    // ==================== CREATE ====================

    public EventoAgenda crearEvento(EventoAgenda evento) throws SQLException {
        validarEvento(evento);

        // Establecer valores por defecto si no están
        if (evento.getEstado() == null) {
            evento.setEstado(EstadoEvento.PENDIENTE);
        }

        if (evento.getDuracionMinutos() == null) {
            evento.setDuracionMinutos(60);
        }

        if (evento.getRecordatorioMinutos() == null) {
            evento.setRecordatorioMinutos(1440); // 24 horas
        }

        if (evento.getColor() == null || evento.getColor().isEmpty()) {
            evento.setColor(evento.getTipo().getColorPredeterminado());
        }

        return eventoDAO.guardar(evento);
    }

    // ==================== READ ====================

    public Optional<EventoAgenda> buscarPorId(Integer id) throws SQLException {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("El ID debe ser un número positivo");
        }
        return eventoDAO.buscarPorId(id);
    }

    public List<EventoAgenda> listarTodos() throws SQLException {
        return eventoDAO.listarTodos();
    }

    public List<EventoAgenda> listarPorUsuario(Integer usuarioId) throws SQLException {
        if (usuarioId == null || usuarioId <= 0) {
            throw new IllegalArgumentException("El ID del usuario debe ser válido");
        }
        return eventoDAO.listarPorUsuario(usuarioId);
    }

    public List<EventoAgenda> listarPorFecha(LocalDate fecha) throws SQLException {
        if (fecha == null) {
            throw new IllegalArgumentException("La fecha no puede ser nula");
        }
        return eventoDAO.listarPorFecha(fecha);
    }

    public List<EventoAgenda> listarPorRangoFechas(LocalDate desde, LocalDate hasta) throws SQLException {
        if (desde == null || hasta == null) {
            throw new IllegalArgumentException("Las fechas no pueden ser nulas");
        }
        if (desde.isAfter(hasta)) {
            throw new IllegalArgumentException("La fecha inicial no puede ser posterior a la final");
        }
        return eventoDAO.listarPorRangoFechas(desde, hasta);
    }

    public List<EventoAgenda> listarPorExpediente(Integer expedienteId) throws SQLException {
        if (expedienteId == null || expedienteId <= 0) {
            throw new IllegalArgumentException("El ID del expediente debe ser válido");
        }
        return eventoDAO.listarPorExpediente(expedienteId);
    }

    public List<EventoAgenda> listarProximos(Integer usuarioId, int dias) throws SQLException {
        if (usuarioId == null || usuarioId <= 0) {
            throw new IllegalArgumentException("El ID del usuario debe ser válido");
        }
        if (dias <= 0) {
            throw new IllegalArgumentException("Los días deben ser mayores a 0");
        }
        return eventoDAO.listarProximos(usuarioId, dias);
    }

    public List<EventoAgenda> listarPendientes(Integer usuarioId) throws SQLException {
        if (usuarioId == null || usuarioId <= 0) {
            throw new IllegalArgumentException("El ID del usuario debe ser válido");
        }
        return eventoDAO.listarPendientes(usuarioId);
    }

    public List<EventoAgenda> listarHoy(Integer usuarioId) throws SQLException {
        return listarPorFecha(LocalDate.now()).stream()
                .filter(e -> e.getUsuarioId().equals(usuarioId))
                .collect(Collectors.toList());
    }

    public List<EventoAgenda> listarEstaSemana(Integer usuarioId) throws SQLException {
        LocalDate hoy = LocalDate.now();
        LocalDate finSemana = hoy.plusDays(7);
        return listarPorRangoFechas(hoy, finSemana).stream()
                .filter(e -> e.getUsuarioId().equals(usuarioId))
                .collect(Collectors.toList());
    }

    public List<EventoAgenda> listarEsteMes(Integer usuarioId) throws SQLException {
        LocalDate hoy = LocalDate.now();
        LocalDate primerDia = hoy.withDayOfMonth(1);
        LocalDate ultimoDia = hoy.withDayOfMonth(hoy.lengthOfMonth());
        return listarPorRangoFechas(primerDia, ultimoDia).stream()
                .filter(e -> e.getUsuarioId().equals(usuarioId))
                .collect(Collectors.toList());
    }

    // ==================== UPDATE ====================

    public EventoAgenda actualizarEvento(EventoAgenda evento) throws SQLException {
        validarEvento(evento);

        if (evento.getId() == null) {
            throw new IllegalArgumentException("El evento debe tener un ID para actualizarse");
        }

        Optional<EventoAgenda> existente = eventoDAO.buscarPorId(evento.getId());
        if (existente.isEmpty()) {
            throw new IllegalArgumentException("No existe un evento con el ID: " + evento.getId());
        }

        return eventoDAO.actualizar(evento);
    }

    public void completarEvento(Integer id) throws SQLException {
        cambiarEstado(id, EstadoEvento.COMPLETADO);
    }

    public void cancelarEvento(Integer id) throws SQLException {
        cambiarEstado(id, EstadoEvento.CANCELADO);
    }

    public void reactivarEvento(Integer id) throws SQLException {
        cambiarEstado(id, EstadoEvento.PENDIENTE);
    }

    private void cambiarEstado(Integer id, EstadoEvento nuevoEstado) throws SQLException {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("El ID debe ser un número positivo");
        }
        if (nuevoEstado == null) {
            throw new IllegalArgumentException("El nuevo estado no puede ser nulo");
        }

        Optional<EventoAgenda> evento = eventoDAO.buscarPorId(id);
        if (evento.isEmpty()) {
            throw new IllegalArgumentException("No existe un evento con el ID: " + id);
        }

        eventoDAO.cambiarEstado(id, nuevoEstado);
    }

    // ==================== DELETE ====================

    public void eliminarEvento(Integer id) throws SQLException {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("El ID debe ser un número positivo");
        }

        Optional<EventoAgenda> evento = eventoDAO.buscarPorId(id);
        if (evento.isEmpty()) {
            throw new IllegalArgumentException("No existe un evento con el ID: " + id);
        }

        eventoDAO.eliminar(id);
    }

    // ==================== ESTADÍSTICAS ====================

    public int contarPendientes(Integer usuarioId) throws SQLException {
        if (usuarioId == null || usuarioId <= 0) {
            throw new IllegalArgumentException("El ID del usuario debe ser válido");
        }
        return eventoDAO.contarPorEstado(usuarioId, EstadoEvento.PENDIENTE);
    }

    public int contarCompletados(Integer usuarioId) throws SQLException {
        if (usuarioId == null || usuarioId <= 0) {
            throw new IllegalArgumentException("El ID del usuario debe ser válido");
        }
        return eventoDAO.contarPorEstado(usuarioId, EstadoEvento.COMPLETADO);
    }

    public EstadisticasAgenda obtenerEstadisticas(Integer usuarioId) throws SQLException {
        return new EstadisticasAgenda(
                contarPendientes(usuarioId),
                contarCompletados(usuarioId),
                eventoDAO.contarPorEstado(usuarioId, EstadoEvento.CANCELADO),
                listarProximos(usuarioId, 7).size()
        );
    }

    // ==================== VALIDACIONES ====================

    private void validarEvento(EventoAgenda evento) {
        if (evento == null) {
            throw new IllegalArgumentException("El evento no puede ser nulo");
        }

        if (evento.getTitulo() == null || evento.getTitulo().trim().isEmpty()) {
            throw new IllegalArgumentException("El título es obligatorio");
        }

        if (evento.getFechaHora() == null) {
            throw new IllegalArgumentException("La fecha y hora son obligatorias");
        }

        if (evento.getTipo() == null) {
            throw new IllegalArgumentException("El tipo de evento es obligatorio");
        }

        if (evento.getUsuarioId() == null) {
            throw new IllegalArgumentException("El evento debe tener un usuario asignado");
        }

        if (evento.getDuracionMinutos() != null && evento.getDuracionMinutos() < 0) {
            throw new IllegalArgumentException("La duración no puede ser negativa");
        }

        if (evento.getRecordatorioMinutos() != null && evento.getRecordatorioMinutos() < 0) {
            throw new IllegalArgumentException("El tiempo de recordatorio no puede ser negativo");
        }
    }

    // ==================== CLASE INTERNA ====================

    public static class EstadisticasAgenda {
        private final int pendientes;
        private final int completados;
        private final int cancelados;
        private final int proximaSemana;

        public EstadisticasAgenda(int pendientes, int completados, int cancelados, int proximaSemana) {
            this.pendientes = pendientes;
            this.completados = completados;
            this.cancelados = cancelados;
            this.proximaSemana = proximaSemana;
        }

        public int getPendientes() { return pendientes; }
        public int getCompletados() { return completados; }
        public int getCancelados() { return cancelados; }
        public int getProximaSemana() { return proximaSemana; }

        @Override
        public String toString() {
            return String.format(
                    "Pendientes: %d | Completados: %d | Cancelados: %d | Próxima semana: %d",
                    pendientes, completados, cancelados, proximaSemana
            );
        }
    }
}