package com.juridix.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Honorario {
    private Integer id;
    private Integer expedienteId;
    private TipoHonorario tipo;
    private Double porcentaje;
    private Double montoFijo;
    private Double montoCalculado;
    private String descripcion;
    private EstadoHonorario estado;
    private LocalDate fechaEstimada;
    private Integer usuarioId;
    private LocalDateTime fechaCreacion;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public Honorario() {
        this.estado = EstadoHonorario.PENDIENTE;
    }

    // Getters y Setters
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Integer getExpedienteId() { return expedienteId; }
    public void setExpedienteId(Integer expedienteId) { this.expedienteId = expedienteId; }

    public TipoHonorario getTipo() { return tipo; }
    public void setTipo(TipoHonorario tipo) { this.tipo = tipo; }

    public Double getPorcentaje() { return porcentaje; }
    public void setPorcentaje(Double porcentaje) { this.porcentaje = porcentaje; }

    public Double getMontoFijo() { return montoFijo; }
    public void setMontoFijo(Double montoFijo) { this.montoFijo = montoFijo; }

    public Double getMontoCalculado() { return montoCalculado; }
    public void setMontoCalculado(Double montoCalculado) { this.montoCalculado = montoCalculado; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public EstadoHonorario getEstado() { return estado; }
    public void setEstado(EstadoHonorario estado) { this.estado = estado; }

    public LocalDate getFechaEstimada() { return fechaEstimada; }
    public void setFechaEstimada(LocalDate fechaEstimada) { this.fechaEstimada = fechaEstimada; }

    public Integer getUsuarioId() { return usuarioId; }
    public void setUsuarioId(Integer usuarioId) { this.usuarioId = usuarioId; }

    public LocalDateTime getFechaCreacion() { return fechaCreacion; }
    public void setFechaCreacion(LocalDateTime fechaCreacion) { this.fechaCreacion = fechaCreacion; }

    // Conversión de fechas
    public String getFechaEstimadaAsString() {
        return fechaEstimada != null ? fechaEstimada.format(DATE_FORMATTER) : null;
    }

    public void setFechaEstimadaFromString(String fecha) {
        this.fechaEstimada = fecha != null && !fecha.isEmpty() ?
                LocalDate.parse(fecha, DATE_FORMATTER) : null;
    }

    public String getFechaCreacionAsString() {
        return fechaCreacion != null ? fechaCreacion.format(DATETIME_FORMATTER) : null;
    }

    public void setFechaCreacionFromString(String fecha) {
        this.fechaCreacion = fecha != null && !fecha.isEmpty() ?
                LocalDateTime.parse(fecha, DATETIME_FORMATTER) : null;
    }

    // Métodos de utilidad
    public String getMontoFormateado() {
        Double monto = montoCalculado != null ? montoCalculado :
                (montoFijo != null ? montoFijo : 0.0);
        return String.format("$%.2f", monto);
    }

    @Override
    public String toString() {
        return tipo + " - " + getMontoFormateado();
    }
}