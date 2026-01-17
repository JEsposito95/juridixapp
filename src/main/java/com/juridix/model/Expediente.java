package com.juridix.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Expediente {
    private Integer id;
    private String numero;
    private String caratula;
    private String cliente;
    private Integer clienteId;
    private String demandado;
    private String fuero;
    private String juzgado;
    private String secretaria;
    private EstadoExpediente estado;
    private LocalDate fechaInicio;
    private LocalDate fechaFinalizacion;
    private Double montoEstimado;
    private String observaciones;
    private Integer creadorId;
    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaModificacion;

    // Formateadores para conversión de fechas
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // Constructor vacío
    public Expediente() {
        this.estado = EstadoExpediente.ACTIVO;
        this.fechaInicio = LocalDate.now();
    }

    // Constructor para nuevo expediente
    public Expediente(String numero, String caratula, String cliente, Integer creadorId) {
        this();
        this.numero = numero;
        this.caratula = caratula;
        this.cliente = cliente;
        this.creadorId = creadorId;
    }

    // Getters y Setters
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getNumero() {
        return numero;
    }

    public void setNumero(String numero) {
        this.numero = numero != null ? numero.trim().toUpperCase() : null;
    }

    public String getCaratula() {
        return caratula;
    }

    public void setCaratula(String caratula) {
        this.caratula = caratula != null ? caratula.trim() : null;
    }

    public String getCliente() {
        return cliente;
    }

    public void setCliente(String cliente) {
        this.cliente = cliente != null ? cliente.trim() : null;
    }

    public String getDemandado() {
        return demandado;
    }

    public void setDemandado(String demandado) {
        this.demandado = demandado != null ? demandado.trim() : null;
    }

    public String getFuero() {
        return fuero;
    }

    public void setFuero(String fuero) {
        this.fuero = fuero;
    }

    public String getJuzgado() {
        return juzgado;
    }

    public void setJuzgado(String juzgado) {
        this.juzgado = juzgado;
    }

    public String getSecretaria() {
        return secretaria;
    }

    public void setSecretaria(String secretaria) {
        this.secretaria = secretaria;
    }

    public EstadoExpediente getEstado() {
        return estado;
    }

    public void setEstado(EstadoExpediente estado) {
        this.estado = estado;
    }

    public LocalDate getFechaInicio() {
        return fechaInicio;
    }

    public void setFechaInicio(LocalDate fechaInicio) {
        this.fechaInicio = fechaInicio;
    }

    public LocalDate getFechaFinalizacion() {
        return fechaFinalizacion;
    }

    public void setFechaFinalizacion(LocalDate fechaFinalizacion) {
        this.fechaFinalizacion = fechaFinalizacion;
    }

    public Double getMontoEstimado() {
        return montoEstimado;
    }

    public void setMontoEstimado(Double montoEstimado) {
        this.montoEstimado = montoEstimado;
    }

    public String getObservaciones() {
        return observaciones;
    }

    public void setObservaciones(String observaciones) {
        this.observaciones = observaciones;
    }

    public Integer getCreadorId() {
        return creadorId;
    }

    public void setCreadorId(Integer creadorId) {
        this.creadorId = creadorId;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(LocalDateTime fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }

    public LocalDateTime getFechaModificacion() {
        return fechaModificacion;
    }

    public void setFechaModificacion(LocalDateTime fechaModificacion) {
        this.fechaModificacion = fechaModificacion;
    }

    // Métodos de utilidad para conversión de fechas
    public String getFechaInicioAsString() {
        return fechaInicio != null ? fechaInicio.format(DATE_FORMATTER) : null;
    }

    public String getFechaFinalizacionAsString() {
        return fechaFinalizacion != null ? fechaFinalizacion.format(DATE_FORMATTER) : null;
    }

    public String getFechaCreacionAsString() {
        return fechaCreacion != null ? fechaCreacion.format(DATETIME_FORMATTER) : null;
    }

    public String getFechaModificacionAsString() {
        return fechaModificacion != null ? fechaModificacion.format(DATETIME_FORMATTER) : null;
    }

    // Para parsear desde la BD
    public void setFechaInicioFromString(String fecha) {
        this.fechaInicio = fecha != null && !fecha.isEmpty() ? LocalDate.parse(fecha, DATE_FORMATTER) : null;
    }

    public void setFechaFinalizacionFromString(String fecha) {
        this.fechaFinalizacion = fecha != null && !fecha.isEmpty() ? LocalDate.parse(fecha, DATE_FORMATTER) : null;
    }

    public void setFechaCreacionFromString(String fecha) {
        this.fechaCreacion = fecha != null && !fecha.isEmpty() ? LocalDateTime.parse(fecha, DATETIME_FORMATTER) : null;
    }

    public void setFechaModificacionFromString(String fecha) {
        this.fechaModificacion = fecha != null && !fecha.isEmpty() ? LocalDateTime.parse(fecha, DATETIME_FORMATTER) : null;
    }
    public Integer getClienteId() {
        return clienteId;
    }

    public void setClienteId(Integer clienteId) {
        this.clienteId = clienteId;
    }

    // Validación
    public boolean isValid() {
        return numero != null && !numero.isEmpty() &&
                caratula != null && !caratula.isEmpty() &&
                cliente != null && !cliente.isEmpty() &&
                fechaInicio != null &&
                creadorId != null;
    }

    @Override
    public String toString() {
        return numero + " - " + caratula;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Expediente that = (Expediente) o;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }
}