package com.juridix.model;

import java.time.LocalDate;

public class Expediente {

    private int id;
    private String numero;
    private String caratula;
    private String cliente;
    private String estado;
    private LocalDate fechaInicio;
    private Usuario creador;

    public Expediente() {
    }

    public Expediente(String numero, String caratula, String cliente,
                      String estado, Usuario creador) {
        this.numero = numero;
        this.caratula = caratula;
        this.cliente = cliente;
        this.estado = estado;
        this.fechaInicio = LocalDate.now();
        this.creador = creador;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getNumero() {
        return numero;
    }

    public String getCaratula() {
        return caratula;
    }

    public String getCliente() {
        return cliente;
    }

    public String getEstado() {
        return estado;
    }

    public LocalDate getFechaInicio() {
        return fechaInicio;
    }

    public Usuario getCreador() {
        return creador;
    }
}
