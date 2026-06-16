package com.juridix.model;

import java.time.LocalDate;

public class MovimientoCuentaCorriente {

    private LocalDate fecha;
    private String tipo;       // "Honorario", "Gasto", "Pago"
    private String concepto;
    private double debe;       // honorarios y gastos
    private double haber;      // pagos
    private double saldo;      // saldo acumulado

    public LocalDate getFecha() { return fecha; }
    public void setFecha(LocalDate fecha) { this.fecha = fecha; }

    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }

    public String getConcepto() { return concepto; }
    public void setConcepto(String concepto) { this.concepto = concepto; }

    public double getDebe() { return debe; }
    public void setDebe(double debe) { this.debe = debe; }

    public double getHaber() { return haber; }
    public void setHaber(double haber) { this.haber = haber; }

    public double getSaldo() { return saldo; }
    public void setSaldo(double saldo) { this.saldo = saldo; }
}