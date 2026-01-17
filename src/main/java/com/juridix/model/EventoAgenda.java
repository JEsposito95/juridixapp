package com.juridix.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class EventoAgenda {
    private Integer id;
    private String titulo;
    private String descripcion;
    private LocalDateTime fechaHora;
    private Integer duracionMinutos;
    private TipoEvento tipo;
    private Integer expedienteId;
    private String ubicacion;
    private EstadoEvento estado;
    private Integer recordatorioMinutos;
    private String color;
    private Integer usuarioId;
    private LocalDateTime fechaCreacion;

    // Formateador
    private static final DateTimeFormatter DATETIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // Constructores
    public EventoAgenda() {
        this.fechaHora = LocalDateTime.now();
        this.duracionMinutos = 60;
        this.estado = EstadoEvento.PENDIENTE;
        this.recordatorioMinutos = 1440; // 24 horas antes
        this.color = "#3498db";
    }

    public EventoAgenda(String titulo, LocalDateTime fechaHora, TipoEvento tipo, Integer usuarioId) {
        this();
        this.titulo = titulo;
        this.fechaHora = fechaHora;
        this.tipo = tipo;
        this.usuarioId = usuarioId;
    }

    // Getters y Setters
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public LocalDateTime getFechaHora() {
        return fechaHora;
    }

    public void setFechaHora(LocalDateTime fechaHora) {
        this.fechaHora = fechaHora;
    }

    public Integer getDuracionMinutos() {
        return duracionMinutos;
    }

    public void setDuracionMinutos(Integer duracionMinutos) {
        this.duracionMinutos = duracionMinutos;
    }

    public TipoEvento getTipo() {
        return tipo;
    }

    public void setTipo(TipoEvento tipo) {
        this.tipo = tipo;
    }

    public Integer getExpedienteId() {
        return expedienteId;
    }

    public void setExpedienteId(Integer expedienteId) {
        this.expedienteId = expedienteId;
    }

    public String getUbicacion() {
        return ubicacion;
    }

    public void setUbicacion(String ubicacion) {
        this.ubicacion = ubicacion;
    }

    public EstadoEvento getEstado() {
        return estado;
    }

    public void setEstado(EstadoEvento estado) {
        this.estado = estado;
    }

    public Integer getRecordatorioMinutos() {
        return recordatorioMinutos;
    }

    public void setRecordatorioMinutos(Integer recordatorioMinutos) {
        this.recordatorioMinutos = recordatorioMinutos;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public Integer getUsuarioId() {
        return usuarioId;
    }

    public void setUsuarioId(Integer usuarioId) {
        this.usuarioId = usuarioId;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(LocalDateTime fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }

    // Métodos de conversión
    public String getFechaHoraAsString() {
        return fechaHora != null ? fechaHora.format(DATETIME_FORMATTER) : null;
    }

    public void setFechaHoraFromString(String fecha) {
        this.fechaHora = fecha != null && !fecha.isEmpty() ?
                LocalDateTime.parse(fecha, DATETIME_FORMATTER) : null;
    }

    public String getFechaCreacionAsString() {
        return fechaCreacion != null ? fechaCreacion.format(DATETIME_FORMATTER) : null;
    }

    public void setFechaCreacionFromString(String fecha) {
        this.fechaCreacion = fecha != null && !fecha.isEmpty() ?
                LocalDateTime.parse(fecha, DATETIME_FORMATTER) : null;
    }

    // Métodos de utilidad
    public LocalDateTime getFechaHoraFin() {
        return fechaHora != null && duracionMinutos != null ?
                fechaHora.plusMinutes(duracionMinutos) : fechaHora;
    }

    public boolean isPendiente() {
        return EstadoEvento.PENDIENTE.equals(estado);
    }

    public boolean isCompletado() {
        return EstadoEvento.COMPLETADO.equals(estado);
    }

    public boolean isCancelado() {
        return EstadoEvento.CANCELADO.equals(estado);
    }

    public boolean esProximo() {
        if (fechaHora == null) return false;
        LocalDateTime ahora = LocalDateTime.now();
        LocalDateTime recordatorio = fechaHora.minusMinutes(recordatorioMinutos != null ? recordatorioMinutos : 0);
        return ahora.isAfter(recordatorio) && ahora.isBefore(fechaHora) && isPendiente();
    }

    // Validación
    public boolean isValid() {
        return titulo != null && !titulo.trim().isEmpty() &&
                fechaHora != null &&
                tipo != null &&
                usuarioId != null;
    }

    @Override
    public String toString() {
        return fechaHora + " - " + titulo;
    }
}