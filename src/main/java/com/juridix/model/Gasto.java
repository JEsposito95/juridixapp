package com.juridix.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Gasto {
    private Integer id;
    private Integer expedienteId;
    private String concepto;
    private Double monto;
    private LocalDate fecha;
    private String categoria;
    private String comprobante;
    private String observaciones;
    private Integer usuarioId;
    private LocalDateTime fechaCreacion;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public Gasto() {
        this.fecha = LocalDate.now();
    }

    // Getters y Setters
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Integer getExpedienteId() { return expedienteId; }
    public void setExpedienteId(Integer expedienteId) { this.expedienteId = expedienteId; }

    public String getConcepto() { return concepto; }
    public void setConcepto(String concepto) { this.concepto = concepto; }

    public Double getMonto() { return monto; }
    public void setMonto(Double monto) { this.monto = monto; }

    public LocalDate getFecha() { return fecha; }
    public void setFecha(LocalDate fecha) { this.fecha = fecha; }

    public String getCategoria() { return categoria; }
    public void setCategoria(String categoria) { this.categoria = categoria; }

    public String getComprobante() { return comprobante; }
    public void setComprobante(String comprobante) { this.comprobante = comprobante; }

    public String getObservaciones() { return observaciones; }
    public void setObservaciones(String observaciones) { this.observaciones = observaciones; }

    public Integer getUsuarioId() { return usuarioId; }
    public void setUsuarioId(Integer usuarioId) { this.usuarioId = usuarioId; }

    public LocalDateTime getFechaCreacion() { return fechaCreacion; }
    public void setFechaCreacion(LocalDateTime fechaCreacion) { this.fechaCreacion = fechaCreacion; }

    // Conversión de fechas
    public String getFechaAsString() {
        return fecha != null ? fecha.format(DATE_FORMATTER) : null;
    }

    public void setFechaFromString(String fecha) {
        this.fecha = fecha != null && !fecha.isEmpty() ?
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
        return monto != null ? String.format("$%.2f", monto) : "$0.00";
    }

    @Override
    public String toString() {
        return concepto + " - " + getMontoFormateado();
    }
}