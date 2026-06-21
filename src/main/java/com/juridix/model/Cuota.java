package com.juridix.model;

import java.time.LocalDate;

public class Cuota {

    private Integer id;
    private Integer expedienteId;
    private Double montoTotalAcordado;
    private Double montoPagado;
    private Integer cantidadCuotasPlanificadas;
    private Double montoPorCuota;
    private String observaciones;
    private LocalDate fechaAcuerdo;
    private String estado; // ACTIVO, COMPLETADO, CANCELADO
    private Integer usuarioId;

    public Cuota() {
        this.montoPagado = 0.0;
        this.estado = "ACTIVO";
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

    public Double getMontoTotalAcordado() {
        return montoTotalAcordado;
    }

    public void setMontoTotalAcordado(Double montoTotalAcordado) {
        this.montoTotalAcordado = montoTotalAcordado;
    }

    public Double getMontoPagado() {
        return montoPagado;
    }

    public void setMontoPagado(Double montoPagado) {
        this.montoPagado = montoPagado;
    }

    public Integer getCantidadCuotasPlanificadas() {
        return cantidadCuotasPlanificadas;
    }

    public void setCantidadCuotasPlanificadas(Integer cantidadCuotasPlanificadas) {
        this.cantidadCuotasPlanificadas = cantidadCuotasPlanificadas;
    }

    public Double getMontoPorCuota() {
        return montoPorCuota;
    }

    public void setMontoPorCuota(Double montoPorCuota) {
        this.montoPorCuota = montoPorCuota;
    }

    public String getObservaciones() {
        return observaciones;
    }

    public void setObservaciones(String observaciones) {
        this.observaciones = observaciones;
    }

    public LocalDate getFechaAcuerdo() {
        return fechaAcuerdo;
    }

    public void setFechaAcuerdo(LocalDate fechaAcuerdo) {
        this.fechaAcuerdo = fechaAcuerdo;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public Integer getUsuarioId() {
        return usuarioId;
    }

    public void setUsuarioId(Integer usuarioId) {
        this.usuarioId = usuarioId;
    }

    // Métodos de cálculo
    public Double getSaldoPendiente() {
        if (montoTotalAcordado == null || montoPagado == null) {
            return 0.0;
        }
        return montoTotalAcordado - montoPagado;
    }

    public Integer getCuotasPagadas() {
        if (montoPorCuota == null || montoPorCuota == 0 || montoPagado == null) {
            return 0;
        }
        return (int) Math.floor(montoPagado / montoPorCuota);
    }

    public Integer getCuotasAdeudadas() {
        if (cantidadCuotasPlanificadas == null) {
            return null; // no se definió un plan con cantidad fija de cuotas
        }
        int pagadas = getCuotasPagadas();
        int adeudadas = cantidadCuotasPlanificadas - pagadas;
        return Math.max(adeudadas, 0);
    }

    public Double getPorcentajePagado() {
        if (montoTotalAcordado == null || montoTotalAcordado == 0) {
            return 0.0;
        }
        return (montoPagado / montoTotalAcordado) * 100;
    }

    public boolean estaCompletado() {
        return montoPagado != null && montoTotalAcordado != null &&
                montoPagado >= montoTotalAcordado;
    }
}