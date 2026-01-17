package com.juridix.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Cliente {
    private Integer id;
    private String nombreCompleto;
    private String dni;
    private String cuitCuil;
    private LocalDate fechaNacimiento;
    private String telefono;
    private String email;
    private String domicilio;
    private String localidad;
    private String provincia;
    private String codigoPostal;
    private String profesion;
    private String estadoCivil;
    private String observaciones;
    private boolean activo;
    private Integer usuarioCreadorId;
    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaModificacion;

    // Formateadores
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // Constructores
    public Cliente() {
        this.activo = true;
    }

    public Cliente(String nombreCompleto) {
        this();
        this.nombreCompleto = nombreCompleto;
    }

    // Getters y Setters
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getNombreCompleto() {
        return nombreCompleto;
    }

    public void setNombreCompleto(String nombreCompleto) {
        this.nombreCompleto = nombreCompleto != null ? nombreCompleto.trim() : null;
    }

    public String getDni() {
        return dni;
    }

    public void setDni(String dni) {
        this.dni = dni != null ? dni.trim() : null;
    }

    public String getCuitCuil() {
        return cuitCuil;
    }

    public void setCuitCuil(String cuitCuil) {
        this.cuitCuil = cuitCuil != null ? cuitCuil.trim() : null;
    }

    public LocalDate getFechaNacimiento() {
        return fechaNacimiento;
    }

    public void setFechaNacimiento(LocalDate fechaNacimiento) {
        this.fechaNacimiento = fechaNacimiento;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono != null ? telefono.trim() : null;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email != null ? email.trim().toLowerCase() : null;
    }

    public String getDomicilio() {
        return domicilio;
    }

    public void setDomicilio(String domicilio) {
        this.domicilio = domicilio != null ? domicilio.trim() : null;
    }

    public String getLocalidad() {
        return localidad;
    }

    public void setLocalidad(String localidad) {
        this.localidad = localidad != null ? localidad.trim() : null;
    }

    public String getProvincia() {
        return provincia;
    }

    public void setProvincia(String provincia) {
        this.provincia = provincia != null ? provincia.trim() : null;
    }

    public String getCodigoPostal() {
        return codigoPostal;
    }

    public void setCodigoPostal(String codigoPostal) {
        this.codigoPostal = codigoPostal != null ? codigoPostal.trim() : null;
    }

    public String getProfesion() {
        return profesion;
    }

    public void setProfesion(String profesion) {
        this.profesion = profesion != null ? profesion.trim() : null;
    }

    public String getEstadoCivil() {
        return estadoCivil;
    }

    public void setEstadoCivil(String estadoCivil) {
        this.estadoCivil = estadoCivil;
    }

    public String getObservaciones() {
        return observaciones;
    }

    public void setObservaciones(String observaciones) {
        this.observaciones = observaciones;
    }

    public boolean isActivo() {
        return activo;
    }

    public void setActivo(boolean activo) {
        this.activo = activo;
    }

    public Integer getUsuarioCreadorId() {
        return usuarioCreadorId;
    }

    public void setUsuarioCreadorId(Integer usuarioCreadorId) {
        this.usuarioCreadorId = usuarioCreadorId;
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

    // Métodos de conversión de fechas
    public String getFechaNacimientoAsString() {
        return fechaNacimiento != null ? fechaNacimiento.format(DATE_FORMATTER) : null;
    }

    public void setFechaNacimientoFromString(String fecha) {
        this.fechaNacimiento = fecha != null && !fecha.isEmpty() ?
                LocalDate.parse(fecha, DATE_FORMATTER) : null;
    }

    public String getFechaCreacionAsString() {
        return fechaCreacion != null ? fechaCreacion.format(DATETIME_FORMATTER) : null;
    }

    public void setFechaCreacionFromString(String fecha) {
        this.fechaCreacion = fecha != null && !fecha.isEmpty() ?
                LocalDateTime.parse(fecha, DATETIME_FORMATTER) : null;
    }

    public String getFechaModificacionAsString() {
        return fechaModificacion != null ? fechaModificacion.format(DATETIME_FORMATTER) : null;
    }

    public void setFechaModificacionFromString(String fecha) {
        this.fechaModificacion = fecha != null && !fecha.isEmpty() ?
                LocalDateTime.parse(fecha, DATETIME_FORMATTER) : null;
    }

    // Métodos de utilidad
    public String getDomicilioCompleto() {
        StringBuilder sb = new StringBuilder();
        if (domicilio != null && !domicilio.isEmpty()) {
            sb.append(domicilio);
        }
        if (localidad != null && !localidad.isEmpty()) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(localidad);
        }
        if (provincia != null && !provincia.isEmpty()) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(provincia);
        }
        if (codigoPostal != null && !codigoPostal.isEmpty()) {
            if (sb.length() > 0) sb.append(" - CP: ");
            sb.append(codigoPostal);
        }
        return sb.toString();
    }

    public int getEdad() {
        if (fechaNacimiento == null) return 0;
        return LocalDate.now().getYear() - fechaNacimiento.getYear();
    }

    // Validación
    public boolean isValid() {
        return nombreCompleto != null && !nombreCompleto.trim().isEmpty();
    }

    @Override
    public String toString() {
        return nombreCompleto + (dni != null ? " (DNI: " + dni + ")" : "");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Cliente cliente = (Cliente) o;
        return id != null && id.equals(cliente.id);
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }
}