package com.juridix.model;

import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Movimiento {
    private Integer id;
    private Integer expedienteId;
    private LocalDate fecha;
    private TipoMovimiento tipo;
    private String descripcion;
    private String cuaderno;
    private Integer foja;
    private String observaciones;
    private Integer usuarioId;
    private LocalDateTime fechaCreacion;

    // Formateadores
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // Constructores
    public Movimiento() {
        this.fecha = LocalDate.now();
    }

    public Movimiento(Integer expedienteId, LocalDate fecha, TipoMovimiento tipo,
                      String descripcion, Integer usuarioId) {
        this.expedienteId = expedienteId;
        this.fecha = fecha;
        this.tipo = tipo;
        this.descripcion = descripcion;
        this.usuarioId = usuarioId;
    }

    // Getters y Setters
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getExpedienteId() {
        return expedienteId;
    }

    public void setExpedienteId(Integer expedienteId) {
        this.expedienteId = expedienteId;
    }

    public LocalDate getFecha() {
        return fecha;
    }

    public void setFecha(LocalDate fecha) {
        this.fecha = fecha;
    }

    public TipoMovimiento getTipo() {
        return tipo;
    }

    public void setTipo(TipoMovimiento tipo) {
        this.tipo = tipo;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getCuaderno() {
        return cuaderno;
    }

    public void setCuaderno(String cuaderno) {
        this.cuaderno = cuaderno;
    }

    public Integer getFoja() {
        return foja;
    }

    public void setFoja(Integer foja) {
        this.foja = foja;
    }

    public String getObservaciones() {
        return observaciones;
    }

    public void setObservaciones(String observaciones) {
        this.observaciones = observaciones;
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

    // Métodos de conversión de fechas
    public String getFechaAsString() {
        return fecha != null ? fecha.format(DATE_FORMATTER) : null;
    }

    public void setFechaFromString(String fecha) {
        this.fecha = fecha != null && !fecha.isEmpty() ? LocalDate.parse(fecha, DATE_FORMATTER) : null;
    }

    public String getFechaCreacionAsString() {
        return fechaCreacion != null ? fechaCreacion.format(DATETIME_FORMATTER) : null;
    }

    public void setFechaCreacionFromString(String fecha) {
        this.fechaCreacion = fecha != null && !fecha.isEmpty() ?
                LocalDateTime.parse(fecha, DATETIME_FORMATTER) : null;
    }

    // Validación
    public boolean isValid() {
        return expedienteId != null &&
                fecha != null &&
                tipo != null &&
                descripcion != null && !descripcion.trim().isEmpty() &&
                usuarioId != null;
    }

    @Override
    public String toString() {
        return fecha + " - " + tipo + ": " + descripcion;
    }
}
