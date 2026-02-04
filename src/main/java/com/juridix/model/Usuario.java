package com.juridix.model;

import java.time.LocalDateTime;

public class Usuario {
    private Integer id;
    private String username;
    private String passwordHash;
    private RolUsuario rol;
    private String nombreCompleto;
    private String email;
    private boolean activo;
    private LocalDateTime fechaCreacion;
    private LocalDateTime ultimoAcceso;

    // ==================== CONSTRUCTORES ====================

    public Usuario() {
        this.activo = true; // Por defecto activo
    }

    public Usuario(String username, String passwordHash, RolUsuario rol) {
        this();
        this.username = username;
        this.passwordHash = passwordHash;
        this.rol = rol;
    }

    public Usuario(Integer id, String username, String passwordHash, RolUsuario rol, boolean activo) {
        this.id = id;
        this.username = username;
        this.passwordHash = passwordHash;
        this.rol = rol;
        this.activo = activo;
    }

    // ==================== GETTERS ====================

    public Integer getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public RolUsuario getRol() {
        return rol;
    }

    public String getNombreCompleto() {
        return nombreCompleto;
    }

    public String getEmail() {
        return email;
    }

    public boolean isActivo() {
        return activo;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public LocalDateTime getUltimoAcceso() {
        return ultimoAcceso;
    }

    // ==================== SETTERS ====================

    public void setId(Integer id) {
        this.id = id;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public void setRol(RolUsuario rol) {
        this.rol = rol;
    }

    public void setNombreCompleto(String nombreCompleto) {
        this.nombreCompleto = nombreCompleto;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setActivo(boolean activo) {
        this.activo = activo;
    }

    public void setFechaCreacion(LocalDateTime fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }

    public void setUltimoAcceso(LocalDateTime ultimoAcceso) {
        this.ultimoAcceso = ultimoAcceso;
    }

    // ==================== MÉTODOS ÚTILES ====================

    @Override
    public String toString() {
        return username + " (" + rol + ")";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Usuario usuario = (Usuario) o;
        return id != null && id.equals(usuario.id);
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }

    /**
     * Verifica si el usuario tiene un rol específico
     */
    public boolean tieneRol(RolUsuario rol) {
        return this.rol == rol;
    }

    /**
     * Verifica si el usuario es administrador
     */
    public boolean esAdmin() {
        return this.rol == RolUsuario.ADMIN;
    }

    /**
     * Obtiene el nombre para mostrar (nombre completo si existe, sino username)
     */
    public String getNombreParaMostrar() {
        if (nombreCompleto != null && !nombreCompleto.trim().isEmpty()) {
            return nombreCompleto;
        }
        return username;
    }
}